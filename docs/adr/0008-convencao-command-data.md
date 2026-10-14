# ADR-0008 · Objetos simples: `...Command` e `...Data` em pacotes próprios

**Status:** Proposta · **Data:** 2026-10-13

## Contexto

Na revisão das specs o usuário pediu que a origem de cada objeto de dados fique explícita no nome e
no pacote, e que services nunca criem objetos (tipos aninhados ou montados à mão). Nas specs
anteriores havia nomes mistos (`SelectToolsQuery`, `RagMatch`, `ToolInvocation`, `PendingDocument`).

## Decisão

- **`...Command`**: tudo que vem do usuário e entra num caso de uso (`usecase/command/`), e todo pedido que o core faz a uma porta (`port/<assunto>/command/`).
- **`...Data`**: tudo que vem do banco (`port/repository/data/`, `port/search/data/`...) e o que volta de sistemas externos pelas portas (`port/<assunto>/data/`).
- Repositórios recebem e devolvem só `...Data`; `core/<ctx>/mapper/*DataMapper` converte `Data ⇄ domínio`.
- Services não declaram tipos e não montam `Command`/`Data` com `new`: usam mappers; entidades nascem por fábricas do domínio.
- `...Request` / `...Response` / `...Message` continuam só na borda (adapters).

Detalhes e tabela: [doc 03, seção 4.2](../specs/03-arquitetura.md).

## Consequências

- Mais classes (um mapper por agregado), em troca de services curtos e de saber pela importação de onde veio cada dado.
- O verificador da skill `java-clean-ddd` deve ganhar checagens: tipo aninhado em `*Service`, `new *Command(`/`new *Data(` dentro de `application/`, e `*Data` fora de pacote `data`.
