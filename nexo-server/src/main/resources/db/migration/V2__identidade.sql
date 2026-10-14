-- ════════════════════════════════════════════════════════════════════════════════
--  V2: identidade (usada a partir do M1)
--
--  app_user ──o| google_credential   (refresh token cifrado; o access token não é gravado)
--  spring_session ──< spring_session_attributes
-- ════════════════════════════════════════════════════════════════════════════════

CREATE TABLE app_user (
    id             uuid        PRIMARY KEY,
    google_subject text        NOT NULL UNIQUE,
    email          text        NOT NULL,
    display_name   text        NOT NULL DEFAULT '',
    picture_url    text,
    created_at     timestamptz NOT NULL,
    last_login_at  timestamptz NOT NULL
);

-- refresh_token = iv ‖ ciphertext ‖ tag (AES-256-GCM, AAD = user_id).
CREATE TABLE google_credential (
    user_id        uuid        PRIMARY KEY REFERENCES app_user (id) ON DELETE CASCADE,
    refresh_token  bytea       NOT NULL,
    key_version    integer     NOT NULL CHECK (key_version > 0),
    granted_scopes text[]      NOT NULL,
    updated_at     timestamptz NOT NULL
);

-- Spring Session JDBC (schema-postgresql.sql do spring-session-jdbc).
CREATE TABLE spring_session (
    primary_id            char(36)     NOT NULL,
    session_id            char(36)     NOT NULL,
    creation_time         bigint       NOT NULL,
    last_access_time      bigint       NOT NULL,
    max_inactive_interval integer      NOT NULL,
    expiry_time           bigint       NOT NULL,
    principal_name        varchar(100),
    CONSTRAINT spring_session_pk PRIMARY KEY (primary_id)
);

CREATE UNIQUE INDEX spring_session_ix1 ON spring_session (session_id);
CREATE INDEX spring_session_ix2 ON spring_session (expiry_time);
CREATE INDEX spring_session_ix3 ON spring_session (principal_name);

CREATE TABLE spring_session_attributes (
    session_primary_id char(36)     NOT NULL,
    attribute_name     varchar(200) NOT NULL,
    attribute_bytes    bytea        NOT NULL,
    CONSTRAINT spring_session_attributes_pk PRIMARY KEY (session_primary_id, attribute_name),
    CONSTRAINT spring_session_attributes_fk FOREIGN KEY (session_primary_id) REFERENCES spring_session (primary_id) ON DELETE CASCADE
);
