# ADR-0006 · Aprovação humana de uso único para toda mutação

**Status:** Proposta · **Data:** 2026-10-13

## Contexto

O modelo pode errar ou ser manipulado por conteúdo de e-mails e documentos (prompt injection). O
google-tools já pausava toda mutação até o usuário confirmar, com aprovação persistida, de uso
único e com validade de 15 minutos.

## Decisão

- Efeito no catálogo: `READ` roda direto; `WRITE` (`warning`) e `DESTRUCTIVE` (`danger`) exigem aprovação.
- Aprovação vinculada ao hash SHA-256 dos argumentos canônicos; consumida na mesma transação da execução.
- Descrição gerada pelo servidor (`ApprovalDescriber`), nunca pelo modelo.
- Decisão pelo WebSocket (`approval.decide`); expiração em 15 min conta como recusa.

## Alternativas

| Alternativa | Por que não |
|---|---|
| Confiar no modelo para pedir confirmação no texto | Não é barreira: o modelo pode pular |
| Aprovação por sessão ("permitir sempre") | Útil no futuro, por ferramenta e com prazo; fora da POC |

## Consequências

- Mais cliques em tarefas com muitas alterações; aceitável numa POC focada em segurança.
