# ADR-0002 · WebSocket bidirecional com replay por `seq`

**Status:** Proposta · **Data:** 2026-10-13

## Contexto

O requisito pede comunicação back ↔ front por WebSocket, como no alien-code. Lá, o
`MissionWebSocketHandler` envia eventos numerados gravados num Event Store e o cliente reconecta
com `lastSeq`. Aqui, além de assistir, o usuário **fala** (mensagens) e **decide** (aprovações).

## Decisão

- Um socket por conversa: `WS /ws/conversations/{id}?lastSeq=N`, autenticado pelo cookie de sessão e com checagem de `Origin`.
- Envelopes `event` (persistido, com `seq`), `command` (cliente → servidor, com `id`) e `ack`/`nack`.
- Evento gravado **antes** de enviado; `seq` por conversa atribuído no banco (`conversation.last_seq`).
- `TextWebSocketHandler` puro do Spring (sem STOMP), como no alien-code.

## Alternativas

| Alternativa | Por que não |
|---|---|
| SSE + REST para comandos | Funciona, mas o requisito pede WebSocket e o time já domina o padrão do alien-code |
| STOMP sobre WebSocket | Tópicos e broker são desnecessários com um canal por conversa; complica o replay por `seq` |
| Socket.IO | Exige servidor Node ou porta Java não oficial |

## Consequências

- Reconexão sem perda (RNF-07) e várias abas sincronizadas de graça.
- Escalar horizontalmente exige distribuir os eventos entre instâncias (`LISTEN/NOTIFY` do Postgres): fora da POC.
