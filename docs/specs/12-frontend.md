# 12 · Front-end (nexo-web)

React 19 + TypeScript + Vite + Vitest, mesma organização do `alien-web`. O front **não decide
nada**: exibe eventos e envia comandos.

Implementado em `nexo-web/` (M1 a M5). Sem biblioteca de rotas: `useRoute` usa a History API.
Em desenvolvimento o Vite (`127.0.0.1:5173`, abra `http://localhost:5173`) repassa `/api`, `/ws` e
`/oauth2` ao servidor, então front e back parecem uma origem só e o cookie `SESSION` vale para os dois.

## 1. Telas

| Rota | Tela | Conteúdo |
|---|---|---|
| `/login` | Login | Logo, frase do produto e **um** botão "Entrar com Google" (link para `/oauth2/authorization/google`) |
| `/` | Início | Barra lateral de conversas + estado vazio com sugestões ("O que tenho amanhã?") |
| `/c/:id` | Conversa | Timeline do chat, cartões de ferramenta e aprovação, campo de mensagem, botão Parar |
| `/settings/contexts` | Contextos | Um cartão por contexto: ícone, nome, descrição, interruptor, escopos (expandível), nº de ferramentas |
| `/settings/account` | Conta | Perfil, Sair, Desconectar conta Google, Apagar meus dados |

```
┌──────────────┬─────────────────────────────────────────────┬──────────────┐
│ + Nova       │  Você: marque reunião amanhã 10h com a Ana   │ Contextos    │
│              │                                             │ ● Calendar   │
│ Hoje         │  🔎 Buscou ferramentas                       │ ● Sheets     │
│  Reunião Ana │     list_calendars (DAG) · list_events      │ ○ Gmail  Ligar│
│  Orçamento   │     create_event                            │ ○ Drive  Ligar│
│              │  ✔ list_calendars  ✔ list_events            │              │
│ Ontem        │  ┌─ Confirmar alteração ──────── warning ┐  │              │
│  ...         │  │ Criar evento "Reunião com Ana"        │  │              │
│              │  │ 14/10 10:00–11:00 · ana@exemplo.com   │  │              │
│              │  │ [ver argumentos]  [Recusar] [Aprovar] │  │              │
│ ──────────── │  └───────────────────────────────────────┘  │              │
│ 👤 Você  ⚙   │  [ Escreva…                         ] (➤)   │              │
└──────────────┴─────────────────────────────────────────────┴──────────────┘
```

O painel de contextos à direita é recolhível; em telas < 900 px vira uma folha inferior.

## 2. Organização

| Pasta | Conteúdo |
|---|---|
| `src/domain/` | Tipos do envelope (`EventEnvelope`, `Command`), `conversationReducer` puro (eventos → itens da timeline), `approval.ts` (estado das aprovações), `contexts.ts` |
| `src/api/` | `http.ts` (fetch com `credentials: 'include'` e `X-XSRF-TOKEN`), `meApi`, `contextsApi`, `conversationsApi`, `catalogApi`, `ConversationSocket` |
| `src/hooks/` | `useRoute`, `useSession`, `useConversation` (socket + reducer), `useContexts`, `useConversations` |
| `src/components/` | `App` (rotas e casca), `LoginPage`, `ConversationSidebar`, `HomeView`, `ChatView`, `MessageBubble`, `ToolSearchCard`, `ToolCallCard`, `ApprovalCard`, `ContextSuggestion`, `ContextPanel`, `ContextCard`, `ContextsPage`, `Composer`, `AccountPage`, `ThemeToggle`, `Toast` |

## 3. Estado da conversa

```ts
type TimelineItem =
  | { kind: 'user'; seq: number; text: string }
  | { kind: 'assistant'; seq: number; text: string; streaming: boolean }
  | { kind: 'toolSearch'; seq: number; goal: string; tools: SelectedTool[] }
  | { kind: 'toolCall'; seq: number; callId: string; tool: string; status: 'running' | 'ok' | 'error' | 'blocked'; summary?: string }
  | { kind: 'approval'; seq: number; approvalId: string; status: 'pending' | 'approved' | 'rejected' | 'expired'; severity: 'warning' | 'danger'; description: string; args: unknown }
  | { kind: 'contextSuggestion'; seq: number; contexts: string[] }
  | { kind: 'turnEnd'; seq: number; ok: boolean; code?: string };

interface ConversationState { lastSeq: number; items: TimelineItem[]; activeTurn?: string; usage?: Usage }
```

`conversationReducer(state, event)` ignora `event.seq <= state.lastSeq` (idempotente no replay).

## 4. Comportamentos

| Situação | Comportamento |
|---|---|
| `GET /api/me` → 401 | Vai para `/login` |
| Ligar contexto → `consent_required` | `window.location.assign(authorizationUrl)`; na volta, `?enabled=` mostra toast de sucesso, `?denied=` mostra aviso |
| `context.suggested` | Cartão "Para isso preciso do Gmail" com botão Ligar |
| `approval.requested` `warning` | Botões Recusar / Aprovar |
| `approval.requested` `danger` | Botão Aprovar vermelho que pede um segundo clique ("Confirmar exclusão") |
| Turno ativo | Campo de mensagem desabilitado, botão vira Parar (`turn.stop`) |
| `turn.failed GOOGLE_RECONNECT_REQUIRED` | Banner "Reconecte sua conta Google" → `/oauth2/authorization/google` |
| Socket caiu | Indicador "reconectando…" e reconexão com `lastSeq` (backoff 0,5 s → 8 s) |

## 5. Acessibilidade e tema

- Contraste AA, foco visível, `aria-live="polite"` na timeline, interruptores com `role="switch"`.
- Tema claro/escuro por `prefers-color-scheme` com troca manual.
- Textos em português; datas com `Intl.DateTimeFormat('pt-BR')` no fuso do usuário.

## 6. Testes (Vitest)

- `conversationReducer`: cada tipo de evento, replay duplicado, deltas → mensagem final.
- `ConversationSocket`: reconexão com `lastSeq` correto (servidor WS falso).
- `ApprovalCard`: duplo clique em `danger`; botões somem depois de `approval.resolved`.
- `ContextCard`: fluxo `consent_required` chama `location.assign`.
