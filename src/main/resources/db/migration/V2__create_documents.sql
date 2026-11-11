CREATE TABLE documents (
    id              UUID PRIMARY KEY,
    filename        VARCHAR(255) NOT NULL,
    content_type    VARCHAR(100),
    size_bytes      BIGINT       NOT NULL,
    reason_category VARCHAR(40)  NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    chunk_count     INTEGER      NOT NULL DEFAULT 0,
    error_message   TEXT,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_documents_created_at ON documents (created_at DESC);
