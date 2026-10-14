-- ════════════════════════════════════════════════════════════════════════════════
--  V1: extensões e configuração de busca textual
--
--  vector   : tipo vector(n) e índice HNSW do pgvector (candidatos semânticos do RAG)
--  unaccent : "emissão" casa com "emissao" (candidatos lexicais do RAG)
--
--  O banco só guarda e indexa. A estratégia de RAG e DAG roda no nexo-server (doc 09).
-- ════════════════════════════════════════════════════════════════════════════════

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS unaccent;

-- Português sem acento: tira o acento antes do radical, então "agenda", "agendar" e "agendá"
-- viram o mesmo termo. Usada pela coluna tool.search_vector e pelas consultas de candidatos.
CREATE TEXT SEARCH CONFIGURATION pt_unaccent (COPY = portuguese);

ALTER TEXT SEARCH CONFIGURATION pt_unaccent
    ALTER MAPPING FOR hword, hword_part, word WITH unaccent, portuguese_stem;
