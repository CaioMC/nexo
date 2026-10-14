-- ════════════════════════════════════════════════════════════════════════════════
--  V5: armazenamento do RAG semântico
--
--  Só a tabela e o índice. O texto que vira vetor e o content_hash são montados no código
--  (ToolDocumentBuilder); a fusão das listas (RRF) também é código.
-- ════════════════════════════════════════════════════════════════════════════════

-- 1536 dimensões = text-embedding-3-small. Outro modelo com outra dimensão pede migration nova.
CREATE TABLE tool_embedding (
    tool_name    text         PRIMARY KEY REFERENCES tool (name) ON DELETE CASCADE,
    model        text         NOT NULL,
    content_hash text         NOT NULL,
    embedding    vector(1536) NOT NULL,
    indexed_at   timestamptz  NOT NULL
);

CREATE INDEX tool_embedding_hnsw_idx ON tool_embedding USING hnsw (embedding vector_cosine_ops);
