# ADR-0004 · Login só Google com BFF e consentimento incremental

**Status:** Proposta · **Data:** 2026-10-13

## Contexto

O usuário precisa entrar só com Google, e o servidor precisa de um token do usuário para chamar as
APIs dos contextos que ele liberar. No `google-tools`, a autorização do Google era opcional e o dono
era identificado por cookie; não havia login.

## Decisão

- OIDC Authorization Code + PKCE via Spring Security OAuth2 Client; o servidor é o cliente confidencial (BFF).
- Sessão no servidor (Spring Session JDBC); o navegador só tem o cookie `HttpOnly`.
- Refresh token cifrado (AES-256-GCM) em `google_credential`; access token só em memória; o repositório de tokens do Spring é substituído por um que não guarda nada.
- Login pede só `openid email profile`; cada contexto pede seus escopos quando é ligado (`include_granted_scopes=true`).

## Alternativas

| Alternativa | Por que não |
|---|---|
| SPA com Google Identity Services e token no navegador | Token exposto a XSS; o servidor precisaria receber o token do cliente |
| Pedir todos os escopos no login | Contraria minimização; assusta o usuário; obriga verificação de todos os escopos restritos |
| JWT próprio stateless | Logout e revogação ficam difíceis; a sessão no banco é simples e suficiente |

## Consequências

- Desligar um contexto não revoga escopos no Google (a API só revoga o token inteiro): documentado em RN-09.
- Em modo Testing o refresh token dura 7 dias.
