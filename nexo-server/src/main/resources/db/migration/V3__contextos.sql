-- ════════════════════════════════════════════════════════════════════════════════
--  V3: contextos Google
--
--  google_context ──< google_context_scope
--  google_context ──< user_context >── app_user    (ausência de linha = desligado)
-- ════════════════════════════════════════════════════════════════════════════════

CREATE TABLE google_context (
    key         text    PRIMARY KEY CHECK (key ~ '^[a-z][a-z0-9_]{1,31}$'),
    name        text    NOT NULL,
    description text    NOT NULL,
    google_api  text    NOT NULL,
    position    integer NOT NULL UNIQUE
);

CREATE TABLE google_context_scope (
    context_key text NOT NULL REFERENCES google_context (key) ON DELETE CASCADE,
    scope       text NOT NULL CHECK (scope LIKE 'https://www.googleapis.com/auth/%'),
    PRIMARY KEY (context_key, scope)
);

CREATE TABLE user_context (
    user_id     uuid        NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    context_key text        NOT NULL REFERENCES google_context (key),
    enabled     boolean     NOT NULL,
    changed_at  timestamptz NOT NULL,
    PRIMARY KEY (user_id, context_key)
);
