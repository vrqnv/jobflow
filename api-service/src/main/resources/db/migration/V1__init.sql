CREATE TABLE jobs (
    id UUID PRIMARY KEY,
    type VARCHAR(32) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
    result JSONB,
    error_message TEXT,
    attempts INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 3,
    next_attempt_at TIMESTAMPTZ,
    idempotency_key VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_jobs_type
        CHECK (type IN ('CHECK_URL', 'GENERATE_REPORT')),

    CONSTRAINT chk_jobs_status
        CHECK (status IN ('QUEUED', 'PROCESSING', 'DONE', 'FAILED')),

    CONSTRAINT chk_jobs_attempts
        CHECK (attempts >= 0 AND attempts <= max_attempts),

    CONSTRAINT chk_jobs_max_attempts
        CHECK (max_attempts > 0)
);

CREATE UNIQUE INDEX ux_jobs_idempotency_key
    ON jobs (idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE INDEX ix_jobs_status_created
    ON jobs (status, created_at DESC);

CREATE INDEX ix_jobs_type_created
    ON jobs (type, created_at DESC);

CREATE INDEX ix_jobs_retry_due
    ON jobs (next_attempt_at)
    WHERE status = 'QUEUED' AND next_attempt_at IS NOT NULL;


CREATE TABLE job_events (
    id BIGSERIAL PRIMARY KEY,
    job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL,
    message TEXT NOT NULL,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_job_events_status
        CHECK (status IN ('QUEUED', 'PROCESSING', 'DONE', 'FAILED'))
);

CREATE INDEX ix_job_events_job_created
    ON job_events (job_id, created_at ASC);


CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    topic VARCHAR(100) NOT NULL,
    message_key VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    available_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at TIMESTAMPTZ,
    publish_attempts INTEGER NOT NULL DEFAULT 0,
    last_error TEXT,

    CONSTRAINT chk_outbox_publish_attempts
        CHECK (publish_attempts >= 0)
);

CREATE INDEX ix_outbox_pending
    ON outbox_events (available_at, created_at)
    WHERE published_at IS NULL;
