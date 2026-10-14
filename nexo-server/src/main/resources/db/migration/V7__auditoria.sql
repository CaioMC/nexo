-- ════════════════════════════════════════════════════════════════════════════════
--  V7: histórico das buscas de ferramentas
--
--  tool_search ──< tool_search_result
--
--  user_id e turn_id são nulos nas buscas feitas por POST /api/search sem usuário (M0) ou fora
--  de um turno.
-- ════════════════════════════════════════════════════════════════════════════════

CREATE TABLE tool_search (
    id               bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id          uuid        REFERENCES app_user (id) ON DELETE CASCADE,
    turn_id          uuid        REFERENCES turn (id) ON DELETE CASCADE,
    query            text        NOT NULL,
    strategy         text        NOT NULL CHECK (strategy IN ('FULL_TEXT', 'VECTOR', 'HYBRID')),
    top_k            integer     NOT NULL CHECK (top_k > 0),
    dag_applied      boolean     NOT NULL,
    allowed_contexts text[]      NOT NULL,
    duration_ms      bigint      NOT NULL,
    created_at       timestamptz NOT NULL
);

-- origin RAG = parecida com o pedido; DAG = trazida como pré-requisito; BLOCKED = relevante, mas
-- de contexto desligado (vira sugestão).
CREATE TABLE tool_search_result (
    search_id        bigint           NOT NULL REFERENCES tool_search (id) ON DELETE CASCADE,
    step             integer          NOT NULL,
    tool_name        text             NOT NULL REFERENCES tool (name),
    origin           text             NOT NULL CHECK (origin IN ('RAG', 'DAG', 'BLOCKED')),
    score_rag        double precision NOT NULL,
    dag_level        integer,
    missing_contexts text[]           NOT NULL DEFAULT '{}',
    PRIMARY KEY (search_id, step)
);
