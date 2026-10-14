# Guia do código do Nexo

Este guia é o mapa do `nexo-server` e do `nexo-web` como eles estão no código (M0 a M5): o que cada
classe faz, quais métodos importam e quem chama quem. As regras de negócio por trás estão nas
[specs](specs/00-indice.md). Os diagramas de sequência estão em [`sequencias.md`](sequencias.md).

Todos os caminhos Java ficam sob `nexo-server/src/main/java/io/github/caiomc/nexo/`.

**Sumário**

1. [Como ler o código](#1-como-ler-o-código)
2. [Um pedido do começo ao fim](#2-um-pedido-do-começo-ao-fim)
3. [A chamada ao modelo, em detalhe](#3-a-chamada-ao-modelo-em-detalhe)
4. [A busca de ferramentas (RAG + DAG)](#4-a-busca-de-ferramentas-rag--dag)
5. [A execução de uma ferramenta](#5-a-execução-de-uma-ferramenta)
6. [Aprovações](#6-aprovações)
7. [WebSocket e eventos](#7-websocket-e-eventos)
8. [Login, tokens e contextos](#8-login-tokens-e-contextos)
9. [Catálogo de classes por contexto](#9-catálogo-de-classes-por-contexto)
10. [Objetos: Command, Data, domínio e mensagens](#10-objetos-command-data-domínio-e-mensagens)
11. [Front-end](#11-front-end)
12. [Pontos de atenção encontrados no review](#12-pontos-de-atenção-encontrados-no-review)

---

## 1. Como ler o código

### 1.1 Duas metades

| Metade | Pacote | O que tem | Pode usar |
|---|---|---|---|
| **core** | `core/<contexto>/` | Regras de negócio | Só Java e `@Service`. Nada de HTTP, JDBC, Jackson ou SDK |
| **adapters** | `adapters/<contexto>/` | Tecnologia: banco, OpenAI, Google, WebSocket, REST, Spring Security | Tudo, e chama o core pelos casos de uso |

### 1.2 Cinco contextos

| Contexto | Responde a | Classe central |
|---|---|---|
| `identity` | Quem é o usuário e qual token do Google usar? | `ObtainAccessTokenService` |
| `workspace` | Quais produtos Google o usuário liberou? | `EnableContextService` |
| `catalog` | Quais ferramentas existem e quem depende de quem? | `ToolCatalog`, `DependencyGraph` |
| `retrieval` | Quais ferramentas mandar ao modelo para este pedido? | `SelectToolsService` |
| `conversation` | Como o turno roda: modelo, ferramentas, aprovações e eventos? | `AgentLoopService`, `CallModelService`, `ToolGatekeeperService` |

### 1.3 Pacotes dentro de cada contexto do core

| Pacote | Conteúdo | Exemplo |
|---|---|---|
| `domain/model` | Entidades e valores com regra (records imutáveis) | `Turn`, `ToolApproval` |
| `domain/enums` | Estados e tipos | `TurnStatus`, `EventType` |
| `domain/policy` | Algoritmos puros, sem estado de aplicação | `ReciprocalRankFusion`, `ArgumentsHasher` |
| `domain/exception` | Erros de negócio | `TurnInProgressException` |
| `port/<assunto>` | Interfaces que os adapters implementam | `LanguageModelPort`, `ToolExecutorPort` |
| `port/<assunto>/command` | O que o core **pede** a uma porta | `ExecuteToolCommand` |
| `port/<assunto>/data` | O que **volta** de fora (banco, OpenAI, Google) | `ModelOutputData`, `TurnData` |
| `usecase` (+ `command/`) | O que o sistema sabe fazer e a entrada de cada caso de uso | `SendMessageUseCase` + `SendMessageCommand` |
| `mapper` | Conversões Data ⇄ domínio e montagem de Commands | `TurnDataMapper`, `ModelCommandMapper` |
| `application` | Os `@Service` que orquestram | `SendMessageService` |

A regra que mais aparece no código: **service não monta objeto simples**. Todo `new XxxCommand` ou
`new XxxData` acontece num mapper, e todo objeto de domínio nasce por uma fábrica do próprio domínio
(`Turn.start`, `ToolApproval.pending`).

Nome diz o que a classe é: **todo `@Service` termina em `Service`**. Quando faz uma ação só, o nome
começa pelo verbo (`SendMessageService`, `CallModelService`, `RunToolService`); quando coordena, é o
substantivo (`AgentLoopService`, `TurnConductorService`, `ConversationEventHubService`). O que não é
service não leva o sufixo: `TurnRun` (estado do turno, criado com `new`), `*Settings` (configuração),
policies (`InstructionsBuilder`, `DeltaBuffer`), `*Mapper`, `*Port` e os adapters.

Método que executa um passo de **RAG** (busca por texto ou vetor, fusão, embedding da pergunta,
indexação) termina em `RAG`; o que usa o **DAG** de pré-requisitos (fecho, Kahn, trava,
disponibilidade por contexto) termina em `DAG`; o que faz os dois termina em `RAGDAG`. Exemplos:
`SelectToolsService.selectRAGDAG`, `ReciprocalRankFusion.fuseRAG`, `ToolSearchPort.lexicalCandidatesRAG`,
`DependencyGraph.executionOrderDAG`, `ToolCatalog.availabilityDAG`, `ToolGatekeeperService.missingPrerequisitesDAG`.
Quem só repassa o resultado (`AgentLoopService`, `AnswerToolSearchService.answer`) não leva o sufixo.

---

## 2. Um pedido do começo ao fim

Exemplo: o usuário digita **"marque reunião amanhã 10h com a Ana"** numa conversa aberta.

| # | Onde | Classe.método | O que faz |
|---|---|---|---|
| 1 | front | `useConversation.send` → `ConversationSocket.send` | Manda `{"kind":"command","type":"user.message","payload":{"text":...,"timeZone":...}}` pelo WebSocket |
| 2 | adapter | `ConversationWebSocketHandler.handleTextMessage` → `dispatch` | Lê o JSON (`ClientCommandMessage`) e converte em `SendMessageCommand` (`EventMessageMapper.toSendCommand`) |
| 3 | core | `SendMessageService.send` | Confere se a conversa é do usuário (`ConversationQueryService.owned`). Se o `clientMessageId` já existe, devolve o mesmo turno. Recusa se houver turno ativo. Cria o `Turn` (`TurnDataMapper.start` → `Turn.start`) e grava (`TurnRepository.insert`) |
| 4 | core | `SendMessageService.send` | Publica `message.user` e, no 1º turno, `conversation.renamed` |
| 5 | core | `TurnConductorService.start` | Cria o `TurnRun`, publica `turn.started` e dispara uma **virtual thread** com `runAgentLoop` → `AgentLoopService.runTurn` |
| 6 | adapter | `ConversationWebSocketHandler` | Responde `ack` com o `turnId` |
| 7 | core | `OpenModelSessionService.open` | Lê os contextos ligados, o histórico e a memória de ferramentas. Monta as instruções e **abre a sessão com o modelo** (seção 3.2) |
| 8 | core | `AgentLoopService.loopUntilFinalAnswer` → `CallModelService.call` | **Chama o modelo.** O texto em streaming vira `assistant.delta` e o consumo vira `usage.updated` (seção 3.3) |
| 9 | core | `AnswerToolSearchService.answer` | O modelo pediu `tool_search` → `SelectToolsService.selectRAGDAG` (**RAG + DAG**, seção 4). Publica `tool.search`. As definições voltam ao modelo |
| 10 | core | `AnswerFunctionCallService.answer` | O modelo chamou funções → `ToolGatekeeperService.admit` decide cada uma, em ordem (seção 5) |
| 11 | core | `AwaitToolApprovalService.awaitThenRun` | `create_event` é `WRITE`: grava a aprovação, publica `approval.requested` e a thread do turno **espera** (seção 6) |
| 12 | front → core | `DecideApprovalService.decide` | O usuário clica em Aprovar: `PENDING → APPROVED`, publica `approval.resolved` e acorda o turno |
| 13 | core → adapter | `RunToolService.run` → `GoogleToolExecutorAdapter.execute` | Pega o token e chama a API do Calendar |
| 14 | core | `AgentLoopService.loopUntilFinalAnswer` | Nova chamada ao modelo com o resultado. Agora vem só texto (resposta final) |
| 15 | core | `AgentLoopService.completeTurn` | Publica `assistant.message`, grava o turno como `COMPLETED` e publica `turn.completed` |
| 16 | core | `TurnConductorService.runAgentLoop` → `failTurn` | Se algo lançar exceção: `TurnAbortedException` vira `turn.failed` com o código; erro inesperado vira `MODEL_ERROR` |

Cada `publish` passa pelo `ConversationEventHubService`: o evento é gravado (ganha um `seq`) e só depois é
entregue aos WebSockets abertos (seção 7).

---

## 3. A chamada ao modelo, em detalhe

### 3.1 Quem participa

O turno inteiro mora em `core/conversation/application/agent/`, uma classe por etapa:

| Classe | Camada | Papel |
|---|---|---|
| `application/agent/TurnConductorService` | core | Começa o turno numa virtual thread, recebe Parar e decisões, transforma erro em `turn.failed` |
| `application/agent/TurnRun` | core | Estado vivo do turno: Parar, espera de aprovação, tempo rodando |
| `application/agent/AgentLoopService` | core | **Só o laço**: chama o modelo, responde os pedidos, repete até a resposta final |
| `application/agent/OpenModelSessionService` | core | Prepara o prompt da 1ª chamada e abre a sessão |
| `application/agent/CallModelService` | core | **Uma chamada ao modelo**: limite, streaming → `assistant.delta`, consumo → `usage.updated` |
| `application/agent/AnswerToolSearchService` | core | Responde `tool_search_call` com RAG + DAG |
| `application/agent/AnswerFunctionCallService` | core | Responde `function_call` aplicando a decisão do `ToolGatekeeperService` |
| `application/agent/RecordTurnStateService` | core | Toda mudança de estado do turno: em memória e no banco no mesmo passo |
| `port/model/LanguageModelPort` | porta | `openSession(OpenModelSessionCommand)` abre a conversa do turno; `isConfigured()`; `modelName()` |
| `port/model/ModelSession` | porta | `call(ModelCallCommand, onDelta)` chama o modelo; `cancel()` (Parar); `close()` |
| `adapters/conversation/openai/OpenAiResponsesAdapter` | adapter | Implementa `LanguageModelPort`; cria uma `OpenAiResponsesSession` por turno |
| `adapters/conversation/openai/OpenAiResponsesSession` | adapter | Guarda o `input` do turno e coordena cada chamada |
| `adapters/conversation/openai/ResponsesStreamer` | adapter | O `POST /responses` em streaming, com retry e cancelamento |
| `adapters/conversation/openai/mapper/ResponsesRequestMapper` | adapter | Monta o corpo do request: parte fixa e itens do `input` |
| `adapters/conversation/openai/mapper/ResponsesToolMapper` | adapter | Ferramentas do catálogo → JSON da Responses API |
| `adapters/conversation/openai/mapper/ResponsesOutputParser` | adapter | JSON da resposta → `ModelOutputData` |
| `adapters/shared/openai/OpenAiHttpClient` | adapter | HTTP do JDK: `post` (JSON) e `stream` (SSE) |
| `core/conversation/domain/policy/InstructionsBuilder` | core | Monta as instruções do sistema |
| `core/conversation/mapper/ModelCommandMapper` | core | Monta `OpenModelSessionCommand`, o histórico, `ModelCallCommand` e as respostas aos pedidos |
| `core/conversation/mapper/FunctionOutputMapper` | core | O JSON de erro que volta ao modelo quando uma função não rodou |
| `core/conversation/mapper/AgentCommandMapper` | core | Comandos para os outros contextos (busca, contextos ligados, descritor) |

### 3.2 Abrir a sessão: `OpenModelSessionService.open`

1. `GetEnabledContextsUseCase.enabled` → contextos ligados agora.
2. `loadPreviousTurns` → últimos 20 turnos terminados (`TurnRepository.findFinished`), do mais antigo ao mais novo.
3. `loadCallsByTurn` → as chamadas de ferramenta desses turnos.
4. `preloadedTools` → `rememberedToolsDAG` (usadas com sucesso nos últimos 3 turnos e ainda
   disponíveis: a "memória de ferramentas") e, no modo `PRESELECTED`, `AnswerToolSearchService.findToolsRAGDAG`
   com a mensagem do usuário.
5. `InstructionsBuilder.build(enabled, disabled, now, zone)` → texto das instruções.
6. `ModelCommandMapper.toHistory` → mensagens `user` e `assistant` dos turnos anteriores. A resposta
   de cada turno leva junto um resumo das ferramentas usadas, não a saída inteira.
7. `LanguageModelPort.openSession(OpenModelSessionCommand)`.

O construtor de `OpenAiResponsesSession` pede ao `ResponsesRequestMapper.toBaseRequest` a parte
**fixa** do pedido, que não muda durante o turno (isso mantém o cache de prompt valendo):

```json
{
  "model": "gpt-5.4-mini",
  "instructions": "Você é o Nexo... Contextos liberados pelo usuário: calendar...",
  "stream": true,
  "tools": [
    { "type": "tool_search", "execution": "client",
      "description": "Busca no catálogo... Namespaces disponíveis: google_calendar_calendar, google_calendar_event.",
      "parameters": { "type": "object", "properties": { "goal": { "type": "string" } }, "required": ["goal"] } },
    { "type": "function", "name": "google_calendar_list_events", "...": "memória de ferramentas" }
  ]
}
```

E começa o `input` (`toInitialInput`) com as mensagens antigas mais a mensagem atual do usuário.

### 3.3 Cada chamada: `CallModelService.call` → `ModelSession.call`

`CallModelService.call` confere o limite de chamadas e chama `session.call(ModelCallCommand, onDelta)`.
Dentro de `OpenAiResponsesSession.call`:

1. `ResponsesRequestMapper.toInputItem` acrescenta ao `input` as respostas aos pedidos da chamada anterior:
   - `ToolOutputCommand.SearchResult` vira um item `tool_search_output` com as definições das
     funções encontradas (`ResponsesToolMapper.toFunctions(tools, deferred=true)`, com `defer_loading`);
   - `ToolOutputCommand.FunctionResult` vira um item `function_call_output` com a saída em JSON.
2. `ResponsesRequestMapper.toRequest` copia a parte fixa e põe `input` = histórico inteiro. Não usa
   `previous_response_id`: o servidor controla o que o modelo vê.
3. `ResponsesStreamer.stream` → `OpenAiHttpClient.stream("/responses", body, onEvent, onOpen)`:
   - lê o SSE linha a linha;
   - cada `response.output_text.delta` vira `ModelDeltaData` → `onDelta`;
   - termina no `response.completed` e devolve o objeto `response`;
   - `onOpen` guarda o `InputStream` para o `cancel()` (Parar) poder fechá-lo de outra thread.
4. Se der 5xx ou erro de rede **antes do primeiro pedaço de texto**, o `ResponsesStreamer` tenta de novo até 2 vezes.
5. Os itens de `response.output` entram no `input` sem os campos nulos
   (`ResponsesRequestMapper.toReplayableItem`), porque o proxy acrescenta nulos que a API recusa
   quando o item volta.
6. `ResponsesOutputParser.toOutput` lê `response.output`:

| Item da resposta | Vira |
|---|---|
| `message` | `ModelOutputData.text` |
| `tool_search_call` com `execution: client` | `ModelOutputData.toolSearches`: `ToolSearchCallData(callId, goal)` |
| `function_call` | `ModelOutputData.functionCalls`: `FunctionCallData(callId, name, arguments, argumentsError)`. Se os argumentos não forem JSON válido, `argumentsError` recebe o motivo |
| `usage` | `UsageData(input, cached, output)` |

De volta ao `CallModelService`: o `DeltaBuffer` junta os pedaços de texto em lotes de 50 ms antes de
publicar `assistant.delta`; depois vêm `Usage.plus`, `RecordTurnStateService.record` e `usage.updated`.

### 3.4 De volta ao laço: `AgentLoopService.loopUntilFinalAnswer`

- `ModelOutputData.isFinalAnswer()` (sem buscas e sem chamadas) → `completeTurn`.
- Senão, cada busca → `AnswerToolSearchService.answer`; cada chamada → `AnswerFunctionCallService.answer`.
  As respostas (`ToolOutputCommand`) viram as `toolOutputs` da próxima chamada.
- Limites (RN-11): 12 chamadas ao modelo (`CallModelService`), 20 chamadas de ferramenta (laço) e
  3 minutos rodando (`TurnRun.ensureCanContinue`; o tempo esperando aprovação não conta).

### 3.5 Embeddings (a outra chamada à OpenAI)

`adapters/retrieval/openai/OpenAiEmbeddingAdapter` faz `POST /embeddings` com `text-embedding-3-small`,
em lotes de 100. É usado em dois pontos:
- na subida (`IndexEmbeddingsService.indexRAG`), para indexar as ferramentas;
- em cada busca `VECTOR` ou `HYBRID` (`SelectToolsService.embedQueryRAG`), para vetorizar a pergunta.

---

## 4. A busca de ferramentas (RAG + DAG)

Ponto de entrada: `core/retrieval/application/SelectToolsService.selectRAGDAG(SelectToolsCommand)`.

| Etapa | Método | Classe que faz o trabalho |
|---|---|---|
| Estratégia (sem chave, cai para `FULL_TEXT`) | `strategy` | `RetrievalSettings` |
| Embedding da pergunta | `embedQueryRAG` | `EmbeddingPort` → `OpenAiEmbeddingAdapter` |
| Contextos ligados | — | `GetEnabledContextsUseCase` → `GetEnabledContextsService` |
| Candidatos por texto | `lexicalRAG` | `LexicalTermsBuilder.buildRAG` monta os termos; `ToolSearchPort.lexicalCandidatesRAG` → `PostgresToolSearchAdapter` (`to_tsquery` + `ts_rank`, índice GIN) |
| Candidatos por vetor | `rankRAG` | `ToolSearchPort.vectorCandidatesRAG` → `PostgresToolSearchAdapter` (`<=>`, índice HNSW) |
| Fusão | `rankRAG` (modo `HYBRID`) | `ReciprocalRankFusion.fuseRAG`: soma `1/(60 + posição)` das duas listas |
| Filtro de contextos | — | `ToolCatalog.availabilityDAG`: a ferramenta e todo o seu fecho de pré-requisitos precisam estar em contextos ligados (RN-02) |
| Mais candidatos se o filtro cortou demais | laço `while` | Dobra o limite até `max-candidates` |
| Top-k | `topAvailableRAGDAG` | — |
| **DAG** | — | `DependencyGraph.executionOrderDAG`: fecho por busca em largura e ordem por Kahn, em levas |
| Sugestões de contexto | `suggestionsRAGDAG` | `BlockedTool` entre as 3 melhores do ranking geral |
| Histórico | — | `SearchHistoryRepository.save` → `tool_search` + `tool_search_result` |

O resultado é uma `ToolSelection`. As ferramentas (`SelectedTool`) chegam na ordem de execução, cada
uma com a origem (`RAG` ou `DAG`), o nível no grafo e o score.

O banco só guarda e indexa. Fusão, filtro, DAG e corte acontecem em Java.

---

## 5. A execução de uma ferramenta

### 5.1 Decidir, aplicar e executar

| Classe.método | O que faz |
|---|---|
| `ToolGatekeeperService.admit(Turn, FunctionCallData)` | **Só decide**, sem efeito colateral, e devolve um `Admission` |
| `AnswerFunctionCallService.answer(TurnRun, FunctionCallData)` | Aplica a decisão e devolve o `ToolOutputCommand` que volta ao modelo |
| `AnswerFunctionCallService.block` | Grava `ToolCall BLOCKED`, publica `tool.blocked` e devolve `context_disabled` ou `missing_prerequisites` |
| `AwaitToolApprovalService.awaitThenRun` | Seção 6 |
| `RunToolService.run` | Grava `ToolCall RUNNING`, publica `tool.started`, chama `ToolExecutorPort`, grava `SUCCEEDED` ou `FAILED` e publica `tool.completed` |
| `RunToolService.disableDeniedContext` | 403 de escopo: desliga o contexto (`DisableContextUseCase`), o que gera `context.changed` |
| `FunctionOutputMapper` | Texto de cada erro devolvido ao modelo (`toUnknownTool`, `toBlocked`, `toApprovalRejected`...) |

As regras de `admit`, na ordem:

| # | Pergunta | Se não | `Admission` |
|---|---|---|---|
| 1 | A ferramenta existe no catálogo? | erro `unknown_tool` | `Unknown` |
| 2 | O contexto dela está ligado **agora**? | `tool.blocked CONTEXT_DISABLED` | `Blocked` |
| 3 | Os argumentos batem com o esquema? (`ArgumentsValidator`) | erro `invalid_arguments` | `Invalid` |
| 4 | Os pré-requisitos diretos rodaram com sucesso na conversa nos últimos 30 min? (`DependencyGraph.missingPrerequisitesDAG`) | `tool.blocked MISSING_PREREQUISITES` | `Blocked` |
| 5 | É `READ`? | é `WRITE` ou `DESTRUCTIVE` | `Run` ou `AwaitApproval` |

### 5.2 `GoogleToolExecutorAdapter`: as mãos

| Classe | Papel |
|---|---|
| `GoogleToolExecutorAdapter.execute` | Escolhe o executor pelo contexto, pega o token (`ObtainAccessTokenUseCase.obtain`), executa, trunca a saída em 16 mil caracteres e resume (`ResultSummarizer`) |
| `GoogleToolExecutorAdapter.run` | Num 401, esquece o token (`invalidate`) e tenta uma vez com um novo |
| `GoogleApiClient.call` | HTTP com o `Bearer`; em 429 ou 5xx tenta de novo até 3 vezes (250, 500 e 1000 ms, mais um intervalo aleatório) |
| `GoogleCall` | Uma execução: argumentos, token e atalhos (`text`, `optionalText`, `integer`, `strings`...) |
| `GoogleRequest` | Uma requisição REST (`get`, `post`, `put`, `patch`, `delete`) |
| `GoogleApiEndpoints` | URLs das APIs (reais, ou um servidor local nos testes) |
| `executor/*Executor` | Um por produto. Um `switch` pelo nome da ferramenta monta a chamada REST: `GmailExecutor`, `DriveExecutor`, `DocsExecutor`, `SheetsExecutor`, `SlidesExecutor`, `CalendarExecutor`, `ChatExecutor`, `PeopleExecutor` |

Erros nunca sobem como exceção. Eles viram um `ToolOutcome`:

| `ToolOutcome` | Quando | Efeito no turno |
|---|---|---|
| `SUCCEEDED` | 2xx | Saída vai ao modelo |
| `FAILED` | Outros 4xx/5xx ou argumento inválido | Erro vai ao modelo |
| `PERMISSION_DENIED` | 403 `insufficientPermissions` | Contexto desligado e erro ao modelo |
| `RECONNECT_REQUIRED` | Refresh recusado ou sem credencial | `turn.failed GOOGLE_RECONNECT_REQUIRED` |

---

## 6. Aprovações

| Passo | Classe.método |
|---|---|
| Descrição legível, montada pelo **servidor** a partir dos argumentos | `ApprovalDescriberPort` → `ApprovalDescriber.describe` |
| Hash SHA-256 dos argumentos em forma canônica | `ArgumentsHasher.hash` |
| Cria a aprovação `PENDING` (15 min, `warning` ou `danger`) | `ToolApprovalDataMapper.pending` → `ToolApproval.pending` |
| Grava, muda o turno para `WAITING_APPROVAL` e publica `approval.requested` | `AwaitToolApprovalService.requestApproval` |
| **Espera** (a virtual thread fica parada; o tempo não conta no limite) | `TurnRun.awaitDecision` |
| Usuário decide: `PENDING → APPROVED/REJECTED` num `UPDATE` atômico | `DecideApprovalService.decide` → `ApprovalRepository.resolvePending` |
| Acorda o turno | `TurnConductorService.deliverDecision` → `TurnRun.deliverDecision` |
| Aprovada: consome só se o hash bater (uso único) | `ApprovalRepository.consume` |
| Vence sozinha | `TurnRun.awaitDecision` (fim do prazo) ou `ApprovalExpirer` → `ExpireApprovalsService.expire` (a cada 30 s) |
| Botão Parar: pendentes viram `REJECTED` | `StopTurnService.stop` → `TurnConductorService.stop` → `TurnRun.stop` |
| Servidor reiniciou | `TurnRecovery` → `FailInterruptedTurnsService.recover` |

---

## 7. WebSocket e eventos

### 7.1 Classes

| Classe | Papel |
|---|---|
| `WebSocketConfig` | Registra `/ws/conversations/*`, aceita só a origem `NEXO_WEB_ORIGIN` e limita mensagens a 16 KiB |
| `SessionHandshakeInterceptor` | Sem `SessionUser` na sessão: 401 antes do upgrade |
| `ConversationWebSocketHandler` | Ao abrir: `watch` com replay. Ao receber: comandos → casos de uso, depois `ack` ou `nack`. A cada 25 s: ping (fecha quem não responde há 60 s) |
| `EventMessageMapper` | `AssistantEvent` → `EventMessage`; `ClientCommandMessage` → `SendMessageCommand`, `DecideApprovalCommand`, `StopTurnCommand` |
| `ConversationEventHubService` | `publish`: grava e entrega. `watch`: replay e assinatura sob o mesmo lock por conversa (sem buraco nem repetição). `terminate`: conversa apagada (fecha com 4409) |
| `PostgresEventStore` | `append`: `UPDATE conversation SET last_seq = last_seq + 1 RETURNING` + `INSERT` na mesma transação. `findAfter`: replay |
| `ConversationEvents` | Fábrica de todos os eventos, com o payload exato do contrato |

### 7.2 Eventos e quem os publica

| Evento | Publicado por |
|---|---|
| `message.user`, `conversation.renamed` | `SendMessageService` |
| `turn.started` | `TurnConductorService.start` |
| `assistant.delta`, `usage.updated` | `CallModelService.call` |
| `tool.search`, `context.suggested` | `AnswerToolSearchService.findToolsRAGDAG` |
| `tool.started`, `tool.completed` | `RunToolService.run` |
| `tool.blocked` | `AnswerFunctionCallService.block` |
| `approval.requested` | `AwaitToolApprovalService.requestApproval` |
| `approval.resolved` | `DecideApprovalService`, `ExpireApprovalsService`, `StopTurnService`, `AwaitToolApprovalService.expire` |
| `assistant.message`, `turn.completed` | `AgentLoopService.completeTurn` |
| `turn.failed` | `TurnConductorService.failTurn`, `StopTurnService`, `FailInterruptedTurnsService` |
| `context.changed` | `PublishContextChangeService`, chamado por `ConversationContextNotifier` |

---

## 8. Login, tokens e contextos

### 8.1 Login com Google (`adapters/identity/security`)

| Classe | Papel |
|---|---|
| `SecurityConfig` | Cadeia do Spring Security: rotas, OAuth2 login, CSRF por cookie e 401 em Problem Details |
| `IncrementalAuthorizationRequestResolver` | Monta o pedido ao Google: PKCE, `access_type=offline`, `include_granted_scopes`. Com `?context=`, acrescenta os escopos do contexto, `prompt=consent` e `login_hint` |
| `DiscardingAuthorizedClientRepository` | Tokens do callback ficam só na requisição, nunca na sessão |
| `OAuth2LoginSuccessHandler` | Recusa conta trocada, faz o login no core, pede consentimento de novo se faltou refresh token, guarda `SessionUser` e conclui o consentimento do contexto |
| `OAuth2SignInMapper` | `OidcUser` + tokens → `SignInCommand`, `CompleteConsentCommand`, `SessionUser` |
| `CsrfCookieFilter`, `ProblemSecurityHandlers` | Cookie `XSRF-TOKEN` e respostas 401/403 |

### 8.2 Serviços de identidade (`core/identity/application`)

| Service | Métodos | O que faz |
|---|---|---|
| `SignInWithGoogleService` | `signIn` | Cria ou atualiza o usuário pelo `sub`, cifra e guarda o refresh token, põe o access token no cache e reconcilia os contextos |
| `ObtainAccessTokenService` | `obtain`, `invalidate` | Cache válido por mais de 60 s? Senão faz refresh. Guarda os escopos e recifra se a chave mudou. `invalid_grant` apaga a credencial e desliga os contextos |
| `IdentityQueryService` | `get`, `granted` | Usuário logado e escopos concedidos |
| `DisconnectGoogleService` | `disconnect` | Revoga no Google, apaga a credencial e desliga tudo |

Adapters de identidade:
- `AesGcmTokenCipher`: AES-256-GCM com o id do usuário como dado associado;
- `GoogleOAuthAdapter`: refresh e revoke no Google;
- `InMemoryAccessTokenCache`: cache de access tokens;
- `PostgresUserRepository` e `PostgresGoogleCredentialRepository`: persistência.

### 8.3 Contextos (`core/workspace/application`)

| Service | O que faz |
|---|---|
| `ContextDirectoryService` | Os 8 contextos do seed, lidos uma vez |
| `GetEnabledContextsService` | Contextos ligados agora (lê o banco a cada chamada) |
| `ListContextsService` | Tela de contextos: estado, escopos concedidos e nº de ferramentas |
| `EnableContextService` | Escopos já concedidos: liga. Senão: devolve a URL de consentimento |
| `CompleteConsentService` | Volta do Google: liga só se vieram todos os escopos (RN-01) |
| `DisableContextService` | Desliga sem chamar o Google (RN-09) |
| `ReconcileContextsService` | Escopo sumiu: desliga o que dependia dele |

---

## 9. Catálogo de classes por contexto

### 9.1 `catalog`

| Classe | Tipo | Métodos principais |
|---|---|---|
| `Tool`, `ToolName`, `ToolParameter`, `ParameterType`, `ToolEffect` | domínio | `ToolEffect.requiresApproval()` |
| `DependencyGraph` | domínio | `closureDAG`, `executionOrderDAG` (Kahn), `missingPrerequisitesDAG`, `levelOfDAG`, `prerequisitesOfDAG`. Recusa ciclo no construtor |
| `ToolCatalog` | domínio | `find`, `require`, `filter`, `availabilityDAG`, `detailDAG`, `overview` |
| `CatalogLoaderService` | service | `load` na subida e `current()` para os outros contextos |
| `CatalogQueryService`, `PlanExecutionService` | service | Respostas de `/api/catalog*` |
| `PostgresToolCatalogRepository` | adapter | Lê `tool`, `tool_parameter` e `tool_dependency` |

### 9.2 `retrieval`

| Classe | Tipo | Papel |
|---|---|---|
| `SelectToolsService` | service | O pipeline da seção 4 |
| `IndexEmbeddingsService` | service | Gera embeddings só do que mudou (`content_hash`) |
| `SearchHistoryService` | service | `GET /api/search/history` |
| `ReciprocalRankFusion`, `LexicalTermsBuilder`, `ToolDocumentBuilder` | policy | Fusão, termos de busca e texto indexado |
| `PostgresToolSearchAdapter`, `PostgresEmbeddingIndexRepository`, `PostgresSearchHistoryRepository` | adapter | Consultas e gravações |

### 9.3 `conversation`

| Classe | Tipo | Papel |
|---|---|---|
| `SendMessageService` | service | Abre o turno |
| `agent/TurnConductorService` | service | Roda, para e encerra turnos |
| `agent/TurnRun` | controle | Estado vivo do turno: Parar, espera de aprovação, tempo rodando |
| `agent/AgentLoopService` | service | Só o laço: modelo → pedidos → respostas → modelo |
| `agent/OpenModelSessionService` | service | Prompt da 1ª chamada e abertura da sessão |
| `agent/CallModelService` | service | Uma chamada ao modelo |
| `agent/AnswerToolSearchService` | service | Responde `tool_search_call` |
| `agent/AnswerFunctionCallService` | service | Responde `function_call` |
| `agent/ToolGatekeeperService` | service | Só decide se a chamada roda |
| `agent/AwaitToolApprovalService` | service | Pede aprovação e espera |
| `agent/RunToolService` | service | Executa a ferramenta |
| `agent/RecordTurnStateService` | service | Grava cada mudança de estado do turno |
| `ConversationEventHubService` | service | Grava e distribui eventos |
| `ConversationQueryService` | service | `owned`, lista, detalhe, eventos e aprovações |
| `ConversationCommandService` | service | Criar, renomear e apagar |
| `DecideApprovalService`, `StopTurnService`, `ExpireApprovalsService`, `FailInterruptedTurnsService`, `PublishContextChangeService` | service | Seção 6 e eventos de contexto |
| `ArgumentsValidator`, `ArgumentsHasher`, `InstructionsBuilder`, `DeltaBuffer`, `ConversationEvents` | policy | Algoritmos puros |
| `PostgresEventStore`, `PostgresConversationRepository`, `PostgresTurnRepository`, `PostgresToolCallRepository`, `PostgresApprovalRepository` | adapter | Persistência |
| `ConversationController` + `ConversationWebMapper` | adapter | REST de conversas |

---

## 10. Objetos: Command, Data, domínio e mensagens

| Sufixo | Direção | Exemplos |
|---|---|---|
| `...Command` | Entra no core (vindo do usuário) ou sai do core para uma porta | `SendMessageCommand`, `SelectToolsCommand`, `OpenModelSessionCommand`, `ModelCallCommand`, `ToolOutputCommand`, `ExecuteToolCommand`, `RefreshTokenCommand` |
| `...Data` | Volta de fora para o core | `TurnData`, `EventData`, `ModelOutputData`, `FunctionCallData`, `ToolSearchCallData`, `ToolResultData`, `TokenData` |
| sem sufixo | Domínio, com regra | `Turn`, `ToolApproval`, `ToolCall`, `Conversation`, `Admission`, `ToolSelection`, `DependencyGraph` |
| `...Request` / `...Response` / `...Message` | Borda (REST e WebSocket) | `SelectToolsRequest`, `ConversationResponse`, `EventMessage`, `ClientCommandMessage` |

Transições de domínio que o código usa:

| Objeto | Transições |
|---|---|
| `Turn` | `start` → `waitingApproval` ⇄ `running` → `completed` ou `failed` |
| `ToolCall` | `proposed` / `blocked` → `running` → `succeeded` / `failed` / `rejected` |
| `ToolApproval` | `pending` → (no banco) `APPROVED` → `CONSUMED`, ou `REJECTED` / `EXPIRED` |
| `Conversation` | `start` → `renamed` |

---

## 11. Front-end

| Arquivo | Papel |
|---|---|
| `src/api/http.ts` | `request`: `fetch` com cookie, `X-XSRF-TOKEN` e erros em Problem Details. `onUnauthorized` avisa quando recebe 401 |
| `src/api/conversationSocket.ts` | `ConversationSocket`: guarda o maior `seq` e reconecta com backoff de 0,5 s a 8 s. `send` resolve no `ack` e rejeita no `nack`. Não reconecta depois de 4400, 4401, 4404 ou 4409 |
| `src/domain/conversationReducer.ts` | Eventos → itens da timeline. Ignora `seq` já aplicado |
| `src/hooks/useConversation.ts` | Socket + reducer + `send`, `decide`, `stop` |
| `src/hooks/useContexts.ts` | Liga e desliga; em `consent_required`, manda o navegador ao Google |
| `src/components/App.tsx` | Rotas, casca, toasts da volta do Google e a 1ª mensagem de uma conversa nova |
| `src/components/ChatView.tsx` | Timeline: `MessageBubble`, `ToolSearchCard`, `ToolCallCard`, `ApprovalCard`, `ContextSuggestion` |

---

## 12. Pontos de atenção encontrados no review

Problemas reais, do mais importante ao menos importante. Nenhum quebra os fluxos testados, mas
vale corrigir antes de abrir para mais gente.

| # | Onde | Problema | Sugestão |
|---|---|---|---|
| 1 | `GoogleToolExecutorAdapter.run` | No 401, o executor **inteiro** roda de novo. Ferramentas que fazem duas chamadas (`google_docs_create` cria e depois insere texto) podem criar o documento duas vezes se o 401 vier na segunda | Repetir só a chamada HTTP que falhou (mover o retry do 401 para `GoogleApiClient`, com o token vindo de um fornecedor) |
| 2 | `OpenAiResponsesSession` / `ResponsesRequestMapper` | Cada chamada reenvia o histórico inteiro do turno. Turnos com muitas leituras grandes ficam caros (até 16 mil caracteres por saída, vezes 20 chamadas) | Resumir saídas antigas dentro do turno ou usar `previous_response_id` |
| 3 | Sessão do modelo em memória | Reiniciar o servidor no meio de um turno o encerra (`SERVER_RESTARTED`). Com mais de uma instância, um evento publicado numa não chega aos sockets da outra | Aceitável na POC (uma instância). Para escalar: `LISTEN/NOTIFY` do Postgres no hub |
| 4 | `ConversationEventHubService.locks` | O mapa de locks por conversa nunca é limpo | Remover no `terminate` ou usar um cache com expiração |
| 5 | `TurnRun.deliverDecision` | Uma decisão que chega depois que a espera terminou deixa um `CompletableFuture` órfão no mapa | Só completar se já existir alguém esperando |
| 6 | `ApprovalDescriber` | Datas aparecem cruas no cartão (`2026-10-14T10:00:00-03:00`) | Formatar data e hora no fuso do turno |
| 7 | `ResultSummarizer` | Plural errado: "1 itens" | `1 item` / `N itens` |
| 8 | CSRF antes da autenticação | `POST` sem sessão responde 403 (`csrf-invalido`) em vez de 401 | Documentado na spec 07. Dá para ordenar os filtros se incomodar |
| 9 | `ConversationEvents` | Payloads com `Map.of` não garantem ordem das chaves no JSON | Só estético. `LinkedHashMap` se quiser ordem fixa |
| 10 | `IncrementalAuthorizationRequestResolver` | Monta `FindContextCommand` direto, sem mapper | Adapter pode, mas destoa do resto (todos os outros usam mapper) |
| 11 | Fuso | Vem só do navegador; a spec previa ler do Calendar | Planejado para o M6 |
| 12 | Testes | Não há teste dedicado para hash diferente no `consume` nem para o `ApprovalExpirer` agendado | Acrescentar `PostgresApprovalRepositoryIT` |
