-- ════════════════════════════════════════════════════════════════════════════════
--  V4: catálogo de ferramentas
--
--  google_context ──< tool_namespace ──< tool ──< tool_parameter
--                                         │
--                                         └──< tool_dependency >── tool   (arestas do DAG)
--
--  Mesmas tabelas do tool-search-pg, com context_key e effect. Sem função nem gatilho: ciclo,
--  fecho, Kahn e trava ficam no DependencyGraph do nexo-server.
-- ════════════════════════════════════════════════════════════════════════════════

CREATE TABLE tool_namespace (
    name        text PRIMARY KEY CHECK (name ~ '^[a-z][a-z0-9_]{1,63}$'),
    context_key text NOT NULL REFERENCES google_context (key),
    description text NOT NULL DEFAULT ''
);

CREATE INDEX tool_namespace_context_idx ON tool_namespace (context_key);

-- search_vector é o documento lexical indexado (GIN): nome (peso A), namespace (B) e
-- descrição (C), em português sem acento. É coluna gerada, não regra: só alimenta o índice.
CREATE TABLE tool (
    name          text PRIMARY KEY CHECK (name ~ '^[a-z][a-z0-9_]{1,63}$'),
    namespace     text NOT NULL REFERENCES tool_namespace (name),
    domain        text NOT NULL DEFAULT 'google_workspace',
    effect        text NOT NULL CHECK (effect IN ('READ', 'WRITE', 'DESTRUCTIVE')),
    description   text NOT NULL CHECK (btrim(description) <> ''),
    search_vector tsvector GENERATED ALWAYS AS (
        setweight(to_tsvector('pt_unaccent'::regconfig, replace(name, '_', ' ')), 'A') ||
        setweight(to_tsvector('pt_unaccent'::regconfig, replace(namespace, '_', ' ')), 'B') ||
        setweight(to_tsvector('pt_unaccent'::regconfig, description), 'C')
    ) STORED
);

CREATE INDEX tool_namespace_idx ON tool (namespace);
CREATE INDEX tool_search_vector_idx ON tool USING gin (search_vector);

CREATE TABLE tool_parameter (
    tool_name   text    NOT NULL REFERENCES tool (name) ON DELETE CASCADE,
    ordinal     integer NOT NULL,
    name        text    NOT NULL,
    type        text    NOT NULL CHECK (type IN ('string', 'integer', 'number', 'boolean', 'array')),
    description text    NOT NULL DEFAULT '',
    required    boolean NOT NULL DEFAULT false,
    minimum     numeric,
    maximum     numeric,
    enum_values text[],
    items       jsonb,
    PRIMARY KEY (tool_name, name),
    UNIQUE (tool_name, ordinal),
    CHECK ((type = 'array') = (items IS NOT NULL))
);

-- tool_name depende de depends_on (depends_on roda antes).
CREATE TABLE tool_dependency (
    tool_name  text NOT NULL REFERENCES tool (name) ON DELETE CASCADE,
    depends_on text NOT NULL REFERENCES tool (name) ON DELETE CASCADE,
    PRIMARY KEY (tool_name, depends_on),
    CHECK (tool_name <> depends_on)
);

CREATE INDEX tool_dependency_depends_on_idx ON tool_dependency (depends_on);
