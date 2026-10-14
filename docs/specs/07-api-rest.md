# 07 · API REST

Contrato formal: [`../contratos/openapi.yaml`](../contratos/openapi.yaml). Todas as rotas `/api/**`
exigem sessão, exceto as marcadas como públicas. Erros seguem **Problem Details** (RFC 9457) com
`type` = `https://nexo.dev/problems/<codigo>`.

## 1. Rotas

### Autenticação e usuário

| Método | Rota | Público | Resposta | Requisito |
|---|---|:-:|---|---|
| GET | `/oauth2/authorization/google` | ✔ | 302 Google (Spring Security) | RF-01 |
| GET | `/login/oauth2/code/google` | ✔ | 302 front (Spring Security) | RF-01 |
| GET | `/api/me` | | `MeResponse` | RF-05 |
| POST | `/api/auth/logout` | | 204 | RF-04 |
| POST | `/api/auth/disconnect` | | 204 | RF-04 / HU-06 |

### Contextos

| Método | Rota | Resposta | Requisito |
|---|---|---|---|
| GET | `/api/contexts` | `ContextResponse[]` | RF-10 |
| POST | `/api/contexts/{key}/enable` | `ContextActivationResponse` (`enabled` ou `consent_required` + `authorizationUrl`) | RF-11, RF-12 |
| POST | `/api/contexts/{key}/disable` | `ContextResponse` | RF-13 |

### Conversas

| Método | Rota | Resposta | Requisito |
|---|---|---|---|
| POST | `/api/conversations` | 201 `ConversationResponse` | RF-20 |
| GET | `/api/conversations?limit=&before=` | `ConversationResponse[]` (mais recentes primeiro) | RF-20 |
| GET | `/api/conversations/{id}` | `ConversationResponse` com `lastSeq` e `activeTurn` | RF-20, RF-22 |
| PATCH | `/api/conversations/{id}` | `ConversationResponse` (título) | RF-20 |
| DELETE | `/api/conversations/{id}` | 204 (fecha sockets com `4409`) | RF-20 |
| GET | `/api/conversations/{id}/events?afterSeq=&limit=` | `EventResponse[]` (mesmo formato do WS) | Depuração / carga inicial |
| GET | `/api/conversations/{id}/approvals?status=PENDING` | `ApprovalResponse[]` | RF-36 |

Mensagens, aprovações e parada vão **pelo WebSocket** (doc 06). Não há rota REST equivalente para
não existirem dois caminhos com regras diferentes.

### Catálogo e busca (estudo e depuração)

| Método | Rota | Resposta | Requisito |
|---|---|---|---|
| GET | `/api/catalog` | `CatalogOverviewResponse` (contextos, namespaces, contagens, arestas) | RF-37 |
| GET | `/api/catalog/tools?context=&namespace=` | `ToolResponse[]` | RF-37 |
| GET | `/api/catalog/tools/{name}` | `ToolResponse` com parâmetros, efeito, `dependsOn`, `dependents`, nível | RF-37 |
| GET | `/api/catalog/plan?tools=a,b` | `ExecutionPlanResponse` (Kahn por níveis) | RF-37 |
| POST | `/api/search` | `ToolSelectionResponse`: `{ query, strategy, topK, applyDag }` → ferramentas com origem RAG/DAG e bloqueadas por contexto do **usuário logado** | RF-31, RF-32 |
| GET | `/api/search/history?limit=` | `SearchHistoryResponse[]` do usuário | Auditoria |

## 2. Exemplos

```http
POST /api/contexts/calendar/enable
X-XSRF-TOKEN: …

200 OK
{ "status": "consent_required", "context": "calendar",
  "authorizationUrl": "/oauth2/authorization/google?context=calendar" }
```

```http
POST /api/search
{ "query": "marque uma reunião amanhã às 10h", "strategy": "HYBRID", "topK": 5, "applyDag": true }

200 OK
{
  "searchId": 812,
  "tools": [
    { "step": 1, "name": "google_calendar_list_calendars", "origin": "DAG", "level": 0, "score": 0.0 },
    { "step": 2, "name": "google_calendar_list_events",    "origin": "RAG", "level": 1, "score": 0.0312 },
    { "step": 3, "name": "google_calendar_create_event",   "origin": "RAG", "level": 2, "score": 0.0328 }
  ],
  "blocked": []
}
```

## 3. Erros

| HTTP | `type` (sufixo) | Quando |
|---|---|---|
| 400 | `requisicao-invalida` | Formato inválido (mapper) |
| 401 | `nao-autenticado` | Sem sessão |
| 403 | `csrf-invalido` | Falta `X-XSRF-TOKEN`. O filtro de CSRF roda antes da autenticação: `POST` sem sessão **e** sem token também dá 403 |
| 404 | `conversa-nao-encontrada`, `contexto-nao-encontrado`, `ferramenta-nao-encontrada` | Não existe ou não é do usuário |
| 409 | `turno-em-andamento` | Apagar conversa com turno ativo |
| 422 | `escopos-nao-concedidos` | Consentimento voltou sem todos os escopos |
| — | — | Sem chave do provedor de LLM: não há rota REST de mensagem; o WebSocket responde `nack MODEL_NOT_CONFIGURED` (doc 06) |

## 4. Mapeamento para o código

Um controller por recurso, cada um com seu `*WebMapper` (skill `java-clean-ddd`, seção 2):

| Controller | Mapper | Casos de uso |
|---|---|---|
| `MeController`, `AuthController` | `IdentityWebMapper` | `GetCurrentUserUseCase`, `DisconnectGoogleUseCase` |
| `ContextController` | `ContextWebMapper` | `ListContextsUseCase`, `EnableContextUseCase`, `DisableContextUseCase` |
| `ConversationController` | `ConversationWebMapper` | `CreateConversationUseCase`, `ListConversationsUseCase`, `RenameConversationUseCase`, `DeleteConversationUseCase`, `ReadEventsUseCase`, `ListApprovalsUseCase` |
| `CatalogController` | `CatalogWebMapper` | `DescribeCatalogUseCase`, `GetToolUseCase`, `PlanExecutionUseCase` |
| `SearchController` | `SearchWebMapper` | `SelectToolsUseCase`, `ListSearchHistoryUseCase` |

O `userId` vem sempre do `SessionUser` (`@AuthenticationPrincipal`), nunca do corpo ou da URL.
