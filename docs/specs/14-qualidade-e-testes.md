# 14 · Qualidade e testes

## 1. Padrões de código (obrigatórios)

As regras das skills **`java-clean-ddd`** e **`java-code-style`** valem para todo o `nexo-server`,
desde o primeiro commit:

| Regra | Resumo |
|---|---|
| Pacotes | `core/<ctx>/{domain/model, domain/exception, port/<assunto>, usecase, usecase/command, application}` e `adapters/<ctx>/{web/..., <tecnologia>, config}` (doc 03) |
| Objetos simples | `...Command` = veio do usuário (ou pedido do core a uma porta), em `usecase/command` / `port/<assunto>/command`; `...Data` = veio do banco ou de sistema externo, em `port/<assunto>/data`. Repositórios só falam `...Data`; conversão em `core/<ctx>/mapper`. Service nunca declara tipo nem monta `Command`/`Data` com `new` (doc 03, seção 4.2; ADR-0008) |
| DTOs | `web/request` e `web/response`: um `record` por arquivo, só dados, sem `static from`, sem importar `core`. Conversão no `*WebMapper` (`@Component`). Vale também para `websocket/message` |
| Controller | Traduz e delega numa linha: `return this.mapper.toResponse(this.useCase.x(this.mapper.toCommand(request)));` (quebrado por nível) |
| Domínio | Records imutáveis; transição devolve nova instância; validação no construtor compacto com mensagem em português; `Clock` injetado |
| `this.` | Em todo campo e método de instância, inclusive em testes e lambdas |
| Nulos | `isNull(x)` / `nonNull(x)`, nunca `== null` / `!= null` |
| Imports | java / bibliotecas / projeto / **estáticos por último** |
| Parágrafos | Linha em branco antes e depois de blocos, depois de guarda, antes do `return` de parágrafo com 2+ instruções |
| Listas quebradas | Um parâmetro/argumento por linha, `)` sozinho; records com 2+ componentes e construtores com 3+ parâmetros sempre quebrados |
| Mensagens | Exceção e log em uma linha só |
| Segredos | `toString()` de value object com segredo mascara (`EncryptedToken[***]`, `AccessToken[***]`); response nunca leva token (teste com `doesNotContain`) |
| Diagramas | [`../sequencias.md`](../sequencias.md) atualizado no mesmo commit em que uma classe muda |
| Commits | Skill `github-commits`: Conventional Commits em português, autoria só do usuário |

Antes de dizer que terminou qualquer marco:

```bash
python3 ~/.claude/skills/java-clean-ddd/scripts/verificar.py nexo --mermaid
python3 ~/.claude/skills/java-code-style/scripts/verificar_estilo.py nexo/nexo-server
cd nexo-server && mvn verify
cd nexo-web && npm test && npm run build
```

## 2. Pirâmide de testes

| Nível | Ferramentas | O que cobre | Rede |
|---|---|---|---|
| **Unitário do core** | JUnit 5 + AssertJ, dublês escritos à mão (`XxxTestDoubles`) | Domínio (transições, validações), serviços (`ToolGatekeeperService`, `SelectToolsService`, `EnableContextService`, `AgentLoopService` com `ScriptedModelSession`) | não |
| **Unitário de adapter** | `HttpServer` do JDK no lugar do Google/OpenAI, JSON gravado em `src/test/resources` | `*Executor` (requisição e parse), `OpenAiResponsesSession` (SSE), `AesGcmTokenCipher`, `*WebMapper` | não |
| **Web slice** | `@WebMvcTest` + `@Import(XxxWebMapper.class)` + `spring-security-test` (`oidcLogin()`) | Rotas, 401/403/404, Problem Details, CSRF | não |
| **Integração** (`*IT`, failsafe) | Testcontainers `pgvector/pgvector:pg17` + Flyway | Migrations, seed do catálogo, consultas de candidatos (GIN e HNSW), Event Store e replay, unicidade de turno ativo, avaliação do RAG (doc 09) | não (Docker local) |
| **Ponta a ponta** | `@SpringBootTest(webEnvironment = RANDOM_PORT)` + cliente WS + LLM roteirizado + Google falso | Turno completo: busca → leitura → aprovação → mutação → resposta; reconexão com `lastSeq`; Parar; reinício | não |
| **Front** | Vitest + Testing Library | Reducer, socket, cartões (doc 12) | não |

Nenhum teste do `mvn verify` chama o Google ou a OpenAI de verdade. O `LiveModelIT` roda o laço
contra o modelo do `.env` (o proxy) só com `NEXO_LIVE=1`; o Google continua falso nele. Teste contra
uma conta Google real fica manual (precisa do OAuth Client, doc 04).

Números de 2026-10-13: `mvn verify` com **94 unitários + 19 de integração**, `npm test` no `nexo-web`
com **14**; os dois verificadores de estilo sem achados.

## 3. Casos obrigatórios

| Caso | Teste |
|---|---|
| Ciclo recusado ao montar o grafo, com o caminho na mensagem | `DependencyGraphTest` |
| Kahn: ordem por níveis, alfabética na leva, respeita todas as arestas | `DependencyGraphTest` |
| Fecho e trava (`missingPrerequisitesDAG`) | `DependencyGraphTest` |
| RRF: notas, empate por nome, listas vazias | `ReciprocalRankFusionTest` |
| Disponibilidade com dependência entre contextos (catálogo sintético) | `ToolCatalogTest` |
| Seed tem 50 ferramentas, 33 arestas e níveis batem com o JSON | `CatalogSeedIT` |
| Busca nunca devolve ferramenta de contexto desligado | `SelectToolsServiceTest`, `SearchApiIT.disabledContextBecomesBlockedSuggestion` |
| Adapters só devolvem `...Data` e não têm regra de RAG/DAG | `PostgresToolSearchAdapterIT` (candidatos crus, sem filtro) |
| Regras do gatekeeper na ordem (existe, contexto, argumentos, pré-requisito, efeito) | `ToolGatekeeperServiceTest` |
| Trava barra mutação sem leitura prévia | `ToolGatekeeperServiceTest.missingDirectPrerequisiteBlocksWithinTheWindow`, `AgentFlowIT.dagLockAndDisabledContextBlock...` |
| Contexto desligado barra a chamada mesmo com a definição carregada | `ToolGatekeeperServiceTest.disabledContextBlocksEvenWithTheDefinitionLoaded`, `AgentFlowIT` |
| Argumentos fora do esquema | `ArgumentsValidatorTest`, `ToolGatekeeperServiceTest.argumentsOutsideTheSchemaAreInvalid` |
| Aprovação de uso único, presa ao hash dos argumentos | `ArgumentsHasherTest`; `AgentFlowIT` confere `CONSUMED` (o `consume` é `UPDATE ... WHERE status = 'APPROVED' AND arguments_hash = :hash`) |
| Aprovação vencida conta como recusa | `TurnRunTest.nobodyDecidingInTimeIsExpired` |
| Parar no meio da aprovação: `REJECTED` + `turn.failed STOPPED` | `TurnRunTest`, `AgentFlowIT.stopDuringApprovalAndSecondMessageIsRefused` |
| Replay sem buraco nem duplicata | `AgentFlowIT` (reconecta com `lastSeq=3`), `conversationSocket.test.ts`, `conversationReducer.test.ts` |
| Usuário A não acessa conversa de B (WS) | `AgentFlowIT.anotherUsersConversationClosesWith4404` |
| Refresh token cifrado; AAD errado não decifra; `toString` mascarado | `AesGcmTokenCipherTest` |
| Sessão guarda só `SessionUser`, nenhum token | `OAuth2LoginSuccessHandlerTest.loginStoresOnlySessionUserAndGoesToTheFront` |
| Consentimento com outra conta Google é recusado | `OAuth2LoginSuccessHandlerTest.consentWithAnotherGoogleAccountIsRefused` |
| `invalid_grant` → credencial apagada, contextos desligados, `RECONNECT_REQUIRED` | `ObtainAccessTokenServiceTest`, `GoogleToolExecutorAdapterTest` |
| Escopo parcial no consentimento mantém desligado | `ContextActivationTest.partialConsentLeavesTheContextOff` |
| 401 do Google renova o token uma vez; 403 de escopo desliga o contexto | `GoogleToolExecutorAdapterTest` |
| Idempotência por `clientMessageId` e um turno por vez | `AgentFlowIT` |
| CSRF obrigatório nas rotas que alteram estado | `AgentFlowIT.stateChangingRequestsWithoutCsrfAreRefused` |
| `context.changed` chega na conversa aberta | `AgentFlowIT.contextsAreListedEnabledAndDisabledAndOpenConversationsHearIt` |
| Modelo real aceita tool search no cliente e o laço completo | `LiveModelIT` (manual, `NEXO_LIVE=1`) |

## 4. Definition of Done (de cada história)

- [ ] Critérios Gherkin do doc 02 cobertos por teste automatizado.
- [ ] `mvn verify` e `npm test` verdes, com números relatados.
- [ ] Verificadores das skills sem achados no código novo.
- [ ] `docs/sequencias.md` e a spec afetada atualizados.
- [ ] Nenhum segredo, token ou dado do usuário em log, response ou commit.
