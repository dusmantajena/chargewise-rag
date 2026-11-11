CREATE TABLE disputes (
    id                UUID PRIMARY KEY,
    description       TEXT        NOT NULL,
    category          VARCHAR(40) NOT NULL,
    required_evidence TEXT[]      NOT NULL,
    explanation       TEXT        NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_disputes_created_at ON disputes (created_at DESC);

CREATE TABLE dispute_citations (
    dispute_id     UUID         NOT NULL REFERENCES disputes (id) ON DELETE CASCADE,
    citation_order INTEGER      NOT NULL,
    document       VARCHAR(255) NOT NULL,
    excerpt        TEXT         NOT NULL,
    PRIMARY KEY (dispute_id, citation_order)
);
