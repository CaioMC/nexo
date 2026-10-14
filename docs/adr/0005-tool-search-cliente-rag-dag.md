# ADR-0005 · Tool search no cliente com RAG híbrido + DAG

**Status:** Proposta · **Data:** 2026-10-13

## Contexto

O dta-tool-search e o tool-search-pg compararam cinco modos de entregar ferramentas ao modelo. O
melhor custo × acerto foi o tool search **no cliente**, com a aplicação respondendo por RAG + DAG.
No Nexo há ainda um filtro que só a aplicação conhece: os contextos ligados pelo usuário.

## Decisão

- `tool_search` com `execution: "client"`; o servidor responde com RAG híbrido (`ReciprocalRankFusion` sobre os candidatos do Postgres) filtrado por contextos (`ToolCatalog.availabilityDAG`) + ordem de execução (`DependencyGraph.executionOrderDAG`, Kahn), tudo no código.
- Trava em tempo de execução (`DependencyGraph.missingPrerequisitesDAG`) no `ToolGatekeeperService`.
- Modo `PRESELECTED` como alternativa configurável.

## Alternativas

| Alternativa | Por que não |
|---|---|
| Mandar todas as ferramentas dos contextos ligados | Cresce com o catálogo; muda o prefixo do prompt a cada liga/desliga (perde cache) |
| Tool search hospedado (`execution: "server"`) | A OpenAI não conhece contextos nem dependências do usuário |
| MCP remoto do Google Workspace | Menos controle sobre aprovação, contexto e auditoria; o google-tools já mostrou o caminho por REST |

## Consequências

- Depende de modelo com tool search; o modo `PRESELECTED` cobre os demais.
- A qualidade da busca é medida por um conjunto de avaliação versionado (doc 09).
