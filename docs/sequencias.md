# Fluxos do código — diagramas de sequência

> Segue o código real (M0 a M5). Os participantes são classes do `nexo-server`; o arquivo é
> atualizado no mesmo commit em que uma classe muda (skill `java-clean-ddd`, seção 7).

<!-- externos: Google, Navegador, OpenAI, PostgreSQL, SpringSecurity -->

Convenção de nomes nas mensagens: `...Command` entra (usuário → caso de uso, core → porta), `...Data` volta (banco ou sistema externo → core). Ver doc 03, seção 4.2.

| # | Fluxo | Gatilho | Marco |
|---|---|---|---|
| 1 | [Carregar o catálogo e indexar embeddings na subida](#1-carregar-o-catálogo-e-indexar-embeddings-na-subida) | `ApplicationReadyEvent` | M0 |
| 2 | [Login com Google](#2-login-com-google) | `GET /login/oauth2/code/google` | M1 |
| 3 | [Ligar contexto com consentimento](#3-ligar-contexto-com-consentimento) | `POST /api/contexts/{key}/enable` | M2 |
| 4 | [Abrir o WebSocket e reproduzir eventos](#4-abrir-o-websocket-e-reproduzir-eventos) | `WS /ws/conversations/{id}?lastSeq=N` | M3 |
| 5 | [Enviar mensagem e conduzir o turno](#5-enviar-mensagem-e-conduzir-o-turno) | `user.message` | M3/M4 |
| 5a | [Abrir a sessão com o modelo](#5a-abrir-a-sessão-com-o-modelo) | início do turno | M4 |
| 5b | [Uma chamada ao modelo](#5b-uma-chamada-ao-modelo) | cada volta do laço | M4 |
| 6 | [Responder ao tool search](#6-responder-ao-tool-search) | `tool_search_call` | M4 |
| 7 | [Admitir e executar uma chamada](#7-admitir-e-executar-uma-chamada) | `function_call` | M4/M5 |
| 8 | [Decidir uma aprovação](#8-decidir-uma-aprovação) | `approval.decide` | M5 |
| 9 | [Vencer aprovações e recuperar turnos](#9-vencer-aprovações-e-recuperar-turnos) | `@Scheduled` / `ApplicationReadyEvent` | M5/M3 |

## 1. Carregar o catálogo e indexar embeddings na subida

```mermaid
sequenceDiagram
    autonumber
    participant J as CatalogLoadOnStartup
    participant L as CatalogLoaderService
    participant R as PostgresToolCatalogRepository
    participant M as ToolCatalogMapper
    participant G as DependencyGraph
    participant X as EmbeddingIndexOnStartup
    participant S as IndexEmbeddingsService
    participant D as ToolDocumentBuilder
    participant I as PostgresEmbeddingIndexRepository
    participant E as OpenAiEmbeddingAdapter

    J->>L: load()
    L->>R: tools() · parameters() · dependencies()
    R-->>L: ToolData · ToolParameterData · DependencyEdgeData
    L->>M: toCatalog(dados)
    M->>G: new DependencyGraph(arestas)
    G->>G: DFS: ciclo? DependencyCycleException
    M-->>L: ToolCatalog (em memória)
    X->>S: indexRAG()
    S->>D: buildRAG(tool) → texto + SHA-256
    S->>I: indexed(modelo)
    I-->>S: List IndexedEmbeddingData
    S->>S: pendentes = hash ou modelo diferente
    loop lotes de 100
        S->>E: embed(EmbedTextsCommand)
        E-->>S: List EmbeddingData
        S->>I: save(ToolEmbeddingData)
    end
```

## 2. Login com Google

```mermaid
sequenceDiagram
    autonumber
    participant SS as SpringSecurity
    participant R as IncrementalAuthorizationRequestResolver
    participant DC as DiscardingAuthorizedClientRepository
    participant H as OAuth2LoginSuccessHandler
    participant S as SignInWithGoogleService
    participant UR as PostgresUserRepository
    participant C as AesGcmTokenCipher
    participant CR as PostgresGoogleCredentialRepository
    participant RC as ReconcileContextsService

    SS->>R: resolve(request) → PKCE, access_type=offline, prompt=select_account
    Note over SS: Google → callback, troca do code, validação do ID token
    SS->>DC: saveAuthorizedClient(tokens) → só no atributo da requisição
    SS->>H: onAuthenticationSuccess(OidcUser)
    H->>DC: take(request) → OAuth2AuthorizedClient
    H->>S: signIn(SignInCommand)
    S->>UR: findBySubject(sub) → UserData
    S->>UR: save(UserData)
    opt veio refresh token
        S->>C: encrypt(refresh, userId) → EncryptedToken
        S->>CR: save(GoogleCredentialData)
    end
    S->>RC: reconcile(ReconcileContextsCommand)
    S-->>H: SignIn(user, offlineAccess)
    alt sem refresh token guardado (1ª vez)
        H-->>SS: 302 /oauth2/authorization/google?consent=true
    else ok
        H->>H: sessão = SessionUser(userId, email)
        H-->>SS: 302 NEXO_WEB_ORIGIN/
    end
```

## 3. Ligar contexto com consentimento

```mermaid
sequenceDiagram
    autonumber
    participant CT as ContextController
    participant M as ContextWebMapper
    participant E as EnableContextService
    participant Q as IdentityQueryService
    participant UC as PostgresUserContextRepository
    participant P as GoogleConsentUrlAdapter
    participant H as OAuth2LoginSuccessHandler
    participant F as CompleteConsentService
    participant N as ConversationContextNotifier

    CT->>M: toEnableCommand(user, key)
    CT->>E: enable(EnableContextCommand)
    E->>Q: granted(GetGrantedScopesCommand)
    alt escopos já concedidos
        E->>UC: save(UserContextData ligado)
        E->>N: notify(NotifyContextChangeCommand)
        E-->>CT: ContextActivation.Enabled
    else faltam escopos
        E->>P: consentUrl(ConsentUrlCommand)
        E-->>CT: ContextActivation.ConsentRequired(url)
        Note over H,F: Google pede só os escopos do contexto (doc 04, seção 4)
        H->>F: complete(CompleteConsentCommand)
        F->>UC: save(UserContextData ligado) se todos os escopos vieram (RN-01)
        F->>N: notify(NotifyContextChangeCommand)
    end
    CT->>M: toResponse(ContextActivation)
```

## 4. Abrir o WebSocket e reproduzir eventos

```mermaid
sequenceDiagram
    autonumber
    participant I as SessionHandshakeInterceptor
    participant WS as ConversationWebSocketHandler
    participant MP as EventMessageMapper
    participant HB as ConversationEventHubService
    participant Q as ConversationQueryService
    participant ES as PostgresEventStore

    I->>I: sessão com SessionUser (senão 401) e Origin conferida pelo registro
    WS->>MP: toWatchCommand(user, conversationId, lastSeq)
    WS->>HB: watch(WatchConversationCommand, listener)
    HB->>Q: owned(userId, conversationId) (senão fecha com 4404)
    Note over HB: lock da conversa: replay + assinatura sem buraco
    loop páginas de 500
        HB->>ES: findAfter(conversationId, seq, 500) → EventData
        HB->>WS: listener.onEvent(AssistantEvent)
    end
    HB->>HB: registra o ouvinte
    WS->>MP: toMessage(AssistantEvent) → EventMessage (JSON)
```

## 5. Enviar mensagem e conduzir o turno

```mermaid
sequenceDiagram
    autonumber
    participant WS as ConversationWebSocketHandler
    participant S as SendMessageService
    participant TR as PostgresTurnRepository
    participant HB as ConversationEventHubService
    participant TC as TurnConductorService
    participant L as AgentLoopService
    participant O as OpenModelSessionService
    participant MC as CallModelService
    participant TSR as RecordTurnStateService

    WS->>S: send(SendMessageCommand)
    S->>TR: findByClientMessage → mesmo turno (RF-26)
    S->>TR: insert(TurnData RUNNING) (índice único: um turno ativo)
    S->>HB: publish(message.user, conversation.renamed no 1º turno)
    S->>TC: start(Turn, fuso)
    TC->>HB: publish(turn.started)
    TC->>L: runTurn(TurnRun) em virtual thread
    S-->>WS: Turn → ack(turnId)
    L->>O: open(TurnRun) → instruções, histórico, ferramentas pré-carregadas (fluxo 5a)
    O-->>L: ModelSession
    loop até a resposta final
        L->>MC: call(TurnRun, ModelSession, respostas da volta anterior)
        Note over MC: chamada ao modelo: fluxo 5b
        MC->>TSR: record(uso) e HB usage.updated
        MC-->>L: ModelOutputData
        Note over L: toolSearches → fluxo 6<br/>functionCalls → fluxo 7
    end
    L->>HB: publish(assistant.message)
    L->>TSR: record(Turn COMPLETED)
    L->>HB: publish(turn.completed)
    Note over TC: TurnAbortedException ou erro → turn.failed com o código
```

### 5a. Abrir a sessão com o modelo

```mermaid
sequenceDiagram
    autonumber
    participant O as OpenModelSessionService
    participant UR as GetEnabledContextsService
    participant TR as PostgresTurnRepository
    participant CR as PostgresToolCallRepository
    participant TSR as AnswerToolSearchService
    participant IB as InstructionsBuilder
    participant MM as ModelCommandMapper
    participant A as OpenAiResponsesAdapter
    participant RM as ResponsesRequestMapper

    O->>UR: enabled(GetEnabledContextsCommand)
    O->>TR: findFinished(conversationId, 20) → loadPreviousTurns
    O->>CR: findByTurns → loadCallsByTurn
    O->>O: rememberedToolsDAG (sucesso nos últimos 3 turnos)
    opt exposição PRESELECTED
        O->>TSR: findToolsRAGDAG(TurnRun, mensagem do usuário)
    end
    O->>IB: build(ligados, desligados, agora, fuso)
    O->>MM: toHistory + toOpenSession
    O->>A: openSession(OpenModelSessionCommand)
    A->>RM: toBaseRequest (model, instructions, tools) e toInitialInput
    A-->>O: OpenAiResponsesSession (com um ResponsesStreamer)
```

### 5b. Uma chamada ao modelo

```mermaid
sequenceDiagram
    autonumber
    participant MC as CallModelService
    participant MS as OpenAiResponsesSession
    participant RM as ResponsesRequestMapper
    participant ST as ResponsesStreamer
    participant H as OpenAiHttpClient
    participant P as ResponsesOutputParser
    participant HB as ConversationEventHubService

    MC->>MC: limite de chamadas ao modelo (RN-11)
    MC->>MS: call(ModelCallCommand, onDelta)
    MS->>RM: toInputItem(cada ToolOutputCommand) → input
    MS->>RM: toRequest(baseRequest, input)
    MS->>ST: stream(request, onDelta)
    ST->>H: stream(/responses, SSE) (tenta de novo em 5xx antes do 1º texto)
    H-->>ST: response.output_text.delta
    ST-->>MC: ModelDeltaData → DeltaBuffer → HB assistant.delta (lotes de 50 ms)
    H-->>ST: response.completed
    ST-->>MS: response
    MS->>RM: toReplayableItem(cada item do output) → input
    MS->>P: toOutput(response)
    P-->>MC: ModelOutputData (texto, toolSearches, functionCalls, uso)
```

## 6. Responder ao tool search

```mermaid
sequenceDiagram
    autonumber
    participant L as AgentLoopService
    participant R as AnswerToolSearchService
    participant S as SelectToolsService
    participant UR as GetEnabledContextsService
    participant E as OpenAiEmbeddingAdapter
    participant TS as PostgresToolSearchAdapter
    participant CM as CandidateDataMapper
    participant F as ReciprocalRankFusion
    participant C as ToolCatalog
    participant G as DependencyGraph
    participant H as PostgresSearchHistoryRepository
    participant HB as ConversationEventHubService

    L->>R: answer(TurnRun, ToolSearchCallData)
    R->>S: selectRAGDAG(SelectToolsCommand com userId e turnId)
    S->>UR: enabled(GetEnabledContextsCommand)
    S->>E: embed(EmbedTextsCommand)
    S->>TS: lexicalCandidatesRAG(termos, 50)
    TS-->>S: List LexicalCandidateData
    S->>TS: vectorCandidatesRAG(vetor, 50)
    TS-->>S: List VectorCandidateData
    S->>CM: lexicalNames / vectorNames
    S->>F: fuseRAG(lexical, semântica)
    F-->>S: List RankedTool
    S->>C: availabilityDAG(ranking, contextos ligados)
    C->>G: closureDAG(ferramenta)
    C-->>S: Availability (disponíveis + BlockedTool)
    S->>G: executionOrderDAG(top-k disponíveis)
    G-->>S: ExecutionPlan (Kahn)
    S->>H: save(ToolSearchData)
    S-->>R: ToolSelection
    R->>HB: publish(tool.search)
    opt há BlockedTool no top 3
        R->>HB: publish(context.suggested)
    end
    R-->>L: toToolSearchOutput(callId, definições) → tool_search_output
```

## 7. Admitir e executar uma chamada

```mermaid
sequenceDiagram
    autonumber
    participant L as AgentLoopService
    participant FR as AnswerFunctionCallService
    participant K as ToolGatekeeperService
    participant C as ToolCatalog
    participant UR as GetEnabledContextsService
    participant CR as PostgresToolCallRepository
    participant G as DependencyGraph
    participant XM as ExecuteToolCommandMapper
    participant AW as AwaitToolApprovalService
    participant AR as PostgresApprovalRepository
    participant RN as RunToolService
    participant X as GoogleToolExecutorAdapter
    participant T as ObtainAccessTokenService
    participant FO as FunctionOutputMapper
    participant HB as ConversationEventHubService

    L->>FR: answer(TurnRun, FunctionCallData)
    FR->>K: admit(Turn, FunctionCallData) (só decide)
    K->>C: find(nome)
    K->>UR: enabled(userId) (agora, não a foto do início)
    K->>CR: succeededSince(conversationId, janela)
    CR-->>K: List ExecutedToolData
    K->>G: missingPrerequisitesDAG(tool, executadas)
    K->>XM: toCommand(turn, tool, call)
    K-->>FR: Admission
    alt Unknown ou Invalid
        FR->>FO: toUnknownTool / toInvalidArguments
    else Blocked
        FR->>CR: save(ToolCallData BLOCKED)
        FR->>HB: publish(tool.blocked)
        FR->>FO: toBlocked (missing_prerequisites / context_disabled)
    else AwaitApproval (WRITE ou DESTRUCTIVE)
        FR->>AW: awaitThenRun(TurnRun, call, admission)
        AW->>AR: insert(ToolApprovalData PENDING, hash, +15 min)
        AW->>HB: publish(approval.requested)
        AW->>AW: TurnRun.awaitDecision (fluxo 8)
    else Run (READ)
        FR->>RN: run(TurnRun, ToolCall RUNNING, tool, command)
        RN->>HB: publish(tool.started)
        RN->>X: execute(ExecuteToolCommand)
        X->>T: obtain(ObtainAccessTokenCommand)
        X-->>RN: ToolResultData (401 renova o token uma vez)
        RN->>CR: save(ToolCallData SUCCEEDED)
        RN->>HB: publish(tool.completed)
    end
    FR-->>L: toFunctionOutput(callId, saída JSON até 16 mil caracteres ou erro)
```

## 8. Decidir uma aprovação

```mermaid
sequenceDiagram
    autonumber
    participant WS as ConversationWebSocketHandler
    participant D as DecideApprovalService
    participant AR as PostgresApprovalRepository
    participant HB as ConversationEventHubService
    participant TC as TurnConductorService
    participant AW as AwaitToolApprovalService
    participant RN as RunToolService
    participant X as GoogleToolExecutorAdapter

    WS->>D: decide(DecideApprovalCommand)
    D->>AR: findById → ToolApprovalData (da conversa do usuário)
    D->>AR: resolvePending(id, APPROVED ou REJECTED) (UPDATE ... WHERE status = 'PENDING')
    D->>HB: publish(approval.resolved)
    D->>TC: deliverDecision(turnId, approvalId, status)
    TC->>AW: TurnRun.deliverDecision acorda awaitDecision
    alt aprovada
        AW->>AR: consume(id, hash) (APPROVED → CONSUMED, uso único)
        AW->>RN: run(TurnRun, ToolCall RUNNING, tool, command)
        RN->>X: execute(ExecuteToolCommand)
        RN->>HB: publish(tool.started, tool.completed)
    else recusada
        AW->>AW: ToolCallData REJECTED e "usuário recusou" ao modelo
    end
```

## 9. Vencer aprovações e recuperar turnos

```mermaid
sequenceDiagram
    autonumber
    participant E as ApprovalExpirer
    participant X as ExpireApprovalsService
    participant R as TurnRecovery
    participant F as FailInterruptedTurnsService
    participant AR as PostgresApprovalRepository
    participant TR as PostgresTurnRepository
    participant HB as ConversationEventHubService
    participant TC as TurnConductorService

    E->>X: expire() a cada 30 s
    X->>AR: findPendingExpiredAt(now)
    loop cada vencida
        X->>AR: resolvePending(id, EXPIRED)
        X->>HB: publish(approval.resolved EXPIRED)
        X->>TC: deliverDecision(turnId, approvalId, EXPIRED)
    end
    R->>F: recover() na subida
    F->>AR: findAllPending() → resolvePending(EXPIRED)
    F->>TR: findAllActive() → update(FAILED SERVER_RESTARTED)
    F->>HB: publish(turn.failed SERVER_RESTARTED)
```
