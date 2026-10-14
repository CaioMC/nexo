-- ════════════════════════════════════════════════════════════════════════════════
--  V6: conversas, turnos, Event Store, chamadas e aprovações (usadas a partir do M3)
--
--  conversation ──< turn ──< tool_call ──o| tool_approval
--  conversation ──< conversation_event   (timeline numerada que o WebSocket reproduz)
-- ════════════════════════════════════════════════════════════════════════════════

CREATE TABLE conversation (
    id         uuid        PRIMARY KEY,
    user_id    uuid        NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    title      text        NOT NULL DEFAULT 'Nova conversa',
    last_seq   bigint      NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);

CREATE INDEX conversation_user_idx ON conversation (user_id, updated_at DESC);

CREATE TABLE turn (
    id                  uuid        PRIMARY KEY,
    conversation_id     uuid        NOT NULL REFERENCES conversation (id) ON DELETE CASCADE,
    client_message_id   uuid        NOT NULL,
    user_message        text        NOT NULL,
    assistant_message   text,
    status              text        NOT NULL CHECK (status IN ('RUNNING', 'WAITING_APPROVAL', 'COMPLETED', 'FAILED')),
    failure_code        text,
    model               text        NOT NULL,
    enabled_contexts    text[]      NOT NULL,
    model_calls         integer     NOT NULL DEFAULT 0,
    input_tokens        bigint      NOT NULL DEFAULT 0,
    cached_input_tokens bigint      NOT NULL DEFAULT 0,
    output_tokens       bigint      NOT NULL DEFAULT 0,
    started_at          timestamptz NOT NULL,
    finished_at         timestamptz,
    UNIQUE (conversation_id, client_message_id)
);

-- RF-25: no máximo um turno ativo por conversa.
CREATE UNIQUE INDEX turn_one_active_idx ON turn (conversation_id) WHERE status IN ('RUNNING', 'WAITING_APPROVAL');

CREATE TABLE conversation_event (
    conversation_id uuid        NOT NULL REFERENCES conversation (id) ON DELETE CASCADE,
    seq             bigint      NOT NULL CHECK (seq > 0),
    turn_id         uuid        REFERENCES turn (id) ON DELETE CASCADE,
    type            text        NOT NULL,
    payload         jsonb       NOT NULL,
    created_at      timestamptz NOT NULL,
    PRIMARY KEY (conversation_id, seq)
);

CREATE TABLE tool_call (
    turn_id        uuid        NOT NULL REFERENCES turn (id) ON DELETE CASCADE,
    call_id        text        NOT NULL,
    tool_name      text        NOT NULL REFERENCES tool (name),
    arguments      jsonb       NOT NULL,
    status         text        NOT NULL CHECK (status IN ('BLOCKED', 'WAITING_APPROVAL', 'REJECTED', 'RUNNING', 'SUCCEEDED', 'FAILED')),
    block_reason   text,
    missing        text[]      NOT NULL DEFAULT '{}',
    output_summary text,
    error          text,
    started_at     timestamptz NOT NULL,
    finished_at    timestamptz,
    PRIMARY KEY (turn_id, call_id)
);

CREATE INDEX tool_call_success_idx ON tool_call (turn_id, tool_name) WHERE status = 'SUCCEEDED';

CREATE TABLE tool_approval (
    id             uuid        PRIMARY KEY,
    user_id        uuid        NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    turn_id        uuid        NOT NULL,
    call_id        text        NOT NULL,
    tool_name      text        NOT NULL REFERENCES tool (name),
    description    text        NOT NULL,
    severity       text        NOT NULL CHECK (severity IN ('warning', 'danger')),
    arguments      jsonb       NOT NULL,
    arguments_hash text        NOT NULL,
    status         text        NOT NULL CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'EXPIRED', 'CONSUMED')),
    created_at     timestamptz NOT NULL,
    expires_at     timestamptz NOT NULL,
    decided_at     timestamptz,
    FOREIGN KEY (turn_id, call_id) REFERENCES tool_call (turn_id, call_id) ON DELETE CASCADE,
    UNIQUE (turn_id, call_id)
);

CREATE INDEX tool_approval_pending_idx ON tool_approval (expires_at) WHERE status = 'PENDING';
