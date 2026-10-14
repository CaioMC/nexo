# ADR-0003 · PostgreSQL + pgvector guarda e indexa; RAG e DAG no código

**Status:** Proposta (revisada em 2026-10-13: estratégia saiu das funções SQL) · **Data:** 2026-10-13

## Contexto

O requisito pede o catálogo de ferramentas já no banco, com `depends_on`, estrutura relacional
semelhante ao `tool-search-pg`, RAG simples com vetores no Postgres e a estratégia DAG para as
dependências. No `tool-search-pg` o RAG e o DAG eram funções SQL (`tool_search_hybrid`,
`tool_execution_order`, gatilho de ciclo...). Na revisão das specs o usuário decidiu que a
**estratégia deve ficar no código**.

## Decisão

- PostgreSQL 17 + `pgvector` 0.8 + `unaccent` (imagem `pgvector/pgvector:pg17`), Flyway.
- **Mesmas tabelas** do tool-search-pg (`tool_namespace`, `tool`, `tool_parameter`, `tool_dependency`, `tool_embedding`, `tool_search*`), com `context_key` e `effect`.
- O banco **só guarda e indexa**: GIN no `search_vector` e HNSW no `embedding`. **Nenhuma** função, view ou gatilho de RAG/DAG.
- Os adapters fazem dois `SELECT`s de candidatos por busca (full-text e kNN) e devolvem `...Data`.
- No core (Java): `ReciprocalRankFusion`, filtro de contextos, top-k, sugestões, `DependencyGraph` (ciclo, fecho, Kahn, trava) e `ToolCatalog.availabilityDAG`.
- O catálogo é lido uma vez na subida (`CatalogLoaderService`) e fica em memória.

## Alternativas

| Alternativa | Por que não |
|---|---|
| Funções SQL como no tool-search-pg | Regra de negócio fora do core, difícil de testar sem banco e de evoluir junto com o domínio (decisão do usuário) |
| Banco vetorial dedicado (Qdrant, Pinecone) | Mais uma peça; 50–1000 vetores cabem folgados no pgvector |
| RAG todo em memória (BM25 + vetores em Java, como o dta-tool-search) | O requisito pede vetores no Postgres; o índice HNSW e o GIN continuam no banco |

## Consequências

- Algoritmos testados com testes unitários puros (`DependencyGraphTest`, `ReciprocalRankFusionTest`), sem Docker.
- Mudança no catálogo exige reiniciar o servidor (ou um recarregamento explícito) para o grafo em memória refletir.
- O filtro de contextos acontece depois da busca: com catálogos grandes e muitos contextos desligados, o serviço pede mais candidatos (doc 09).
