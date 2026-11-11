CREATE TABLE IF NOT EXISTS bw_source (
    id TEXT PRIMARY KEY,
    tenant TEXT NOT NULL,
    scope_path TEXT NOT NULL,
    uri TEXT NOT NULL,
    kind TEXT NOT NULL,
    status TEXT NOT NULL,
    content_hash TEXT,
    fetched_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS bw_context_unit (
    id TEXT PRIMARY KEY,
    tenant TEXT NOT NULL,
    scope_path TEXT NOT NULL,
    workspace TEXT,
    project TEXT,
    agent_id TEXT,
    user_id TEXT,
    session_id TEXT,
    kind TEXT NOT NULL,
    origin TEXT NOT NULL,
    state TEXT NOT NULL,
    l0_abstract TEXT NOT NULL,
    l1_overview TEXT,
    l2_inline TEXT,
    l2_ref TEXT,
    confidence REAL NOT NULL,
    authority INTEGER NOT NULL,
    importance REAL NOT NULL,
    cost_tokens INTEGER NOT NULL DEFAULT 0,
    n_seen INTEGER NOT NULL DEFAULT 0,
    n_cited INTEGER NOT NULL DEFAULT 0,
    n_adopted INTEGER NOT NULL DEFAULT 0,
    n_corrected INTEGER NOT NULL DEFAULT 0,
    valid_from TIMESTAMP,
    valid_to TIMESTAMP,
    learned_at TIMESTAMP NOT NULL,
    superseded_at TIMESTAMP,
    source_published_at TIMESTAMP,
    captured_at TIMESTAMP,
    review_due_at TIMESTAMP,
    retention_until TIMESTAMP,
    erased_at TIMESTAMP,
    superseded_by TEXT REFERENCES bw_context_unit(id),
    owner_id TEXT,
    owner_org TEXT,
    review_cycle_days INTEGER,
    legal_hold INTEGER NOT NULL DEFAULT 0,
    hold_reason TEXT,
    episode_id TEXT,
    source_ref TEXT,
    span_start INTEGER,
    span_end INTEGER,
    ontology_version TEXT,
    extractor_version TEXT,
    lineage_parents TEXT NOT NULL DEFAULT '[]',
    needs_reextract INTEGER NOT NULL DEFAULT 0,
    tags TEXT NOT NULL DEFAULT '[]',
    content_hash TEXT NOT NULL,
    embedding BLOB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS bw_context_scope_idx ON bw_context_unit (tenant, scope_path);
CREATE INDEX IF NOT EXISTS bw_current_idx ON bw_context_unit (tenant, kind)
    WHERE valid_to IS NULL AND superseded_by IS NULL
      AND state IN ('ACTIVE', 'STALE_SUSPECT', 'CONFLICTED');
CREATE INDEX IF NOT EXISTS bw_lifecycle_due_idx ON bw_context_unit (state, review_due_at);
CREATE INDEX IF NOT EXISTS bw_retention_idx ON bw_context_unit (retention_until);

CREATE TABLE IF NOT EXISTS bw_unit_edge (
    from_id TEXT NOT NULL REFERENCES bw_context_unit(id),
    to_id TEXT NOT NULL REFERENCES bw_context_unit(id),
    edge_type TEXT NOT NULL,
    valid_from TIMESTAMP NOT NULL,
    valid_to TIMESTAMP,
    weight REAL,
    PRIMARY KEY (from_id, to_id, edge_type, valid_from)
);

CREATE INDEX IF NOT EXISTS bw_edge_to_idx ON bw_unit_edge (to_id, edge_type, valid_to);

CREATE TABLE IF NOT EXISTS bw_index_entry (
    unit_id TEXT NOT NULL,
    representation_line TEXT NOT NULL,
    tenant TEXT NOT NULL,
    workspace TEXT,
    project TEXT,
    agent_id TEXT,
    user_id TEXT,
    session_id TEXT,
    text TEXT NOT NULL,
    metadata TEXT NOT NULL DEFAULT '{}',
    embedding BLOB,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (unit_id, representation_line)
);

CREATE INDEX IF NOT EXISTS bw_index_tenant_idx
    ON bw_index_entry (tenant, workspace, project, agent_id, user_id, session_id);

CREATE TABLE IF NOT EXISTS bw_episode (
    id TEXT PRIMARY KEY,
    tenant TEXT NOT NULL,
    scope_path TEXT NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    source TEXT NOT NULL,
    payload TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS bw_segment (
    id TEXT PRIMARY KEY,
    doc_id TEXT NOT NULL,
    parent_id TEXT,
    ordinal INTEGER,
    path TEXT,
    abstract TEXT,
    overview TEXT,
    content_ref TEXT,
    needs_reextract INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS bw_usage_event (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    unit_id TEXT,
    trace_id TEXT NOT NULL,
    event TEXT NOT NULL,
    at TIMESTAMP NOT NULL,
    tokens INTEGER NOT NULL DEFAULT 0,
    outcome TEXT
);

CREATE INDEX IF NOT EXISTS bw_usage_unit_idx ON bw_usage_event (unit_id, at);

CREATE TABLE IF NOT EXISTS bw_review (
    id TEXT PRIMARY KEY,
    unit_id TEXT,
    gate TEXT NOT NULL,
    status TEXT NOT NULL,
    reviewer TEXT,
    at TIMESTAMP NOT NULL,
    note TEXT
);

CREATE TABLE IF NOT EXISTS bw_lifecycle_event (
    id TEXT PRIMARY KEY,
    unit_id TEXT NOT NULL,
    from_state TEXT,
    to_state TEXT NOT NULL,
    trigger TEXT NOT NULL,
    actor_id TEXT,
    actor_type TEXT,
    at TIMESTAMP NOT NULL,
    reason TEXT,
    batch_id TEXT
);

CREATE INDEX IF NOT EXISTS bw_lifecycle_unit_idx ON bw_lifecycle_event (unit_id, at);

CREATE TABLE IF NOT EXISTS bw_lineage (
    parent_id TEXT NOT NULL,
    child_id TEXT NOT NULL,
    derivation TEXT NOT NULL,
    derived_at TIMESTAMP,
    extractor_version TEXT,
    ontology_version TEXT,
    needs_reextract INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (parent_id, child_id, derivation)
);

CREATE INDEX IF NOT EXISTS bw_lineage_child_idx ON bw_lineage (child_id);

CREATE TABLE IF NOT EXISTS bw_asset (
    unit_id TEXT PRIMARY KEY REFERENCES bw_context_unit(id),
    owner_id TEXT,
    owner_org TEXT,
    review_cycle_days INTEGER,
    review_due_at TIMESTAMP,
    reviewed_at TIMESTAMP,
    retention_until TIMESTAMP,
    legal_hold INTEGER NOT NULL DEFAULT 0,
    hold_reason TEXT
);

CREATE TABLE IF NOT EXISTS bw_publish_batch (
    id TEXT PRIMARY KEY,
    ontology_version TEXT,
    scope_filter TEXT,
    mode TEXT NOT NULL,
    status TEXT NOT NULL,
    effective_at TIMESTAMP,
    created_by TEXT,
    manifest_hash TEXT,
    regression_run_id TEXT
);

CREATE TABLE IF NOT EXISTS bw_erasure (
    id TEXT PRIMARY KEY,
    unit_id TEXT NOT NULL,
    request_type TEXT NOT NULL,
    legal_basis TEXT,
    requested_by TEXT,
    executed_at TIMESTAMP,
    cascade TEXT NOT NULL,
    residual_note TEXT
);

CREATE TABLE IF NOT EXISTS bw_signal (
    signal_id TEXT PRIMARY KEY,
    kind TEXT NOT NULL,
    reliability TEXT NOT NULL,
    trace_id TEXT,
    release_id TEXT,
    unit_id TEXT,
    content_hash TEXT,
    payload TEXT,
    at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS bw_attribution (
    attribution_id TEXT PRIMARY KEY,
    signal_id TEXT NOT NULL,
    code TEXT NOT NULL,
    target_ref TEXT,
    reviewed_by TEXT,
    reviewed_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS bw_change_request (
    cr_id TEXT PRIMARY KEY,
    type TEXT NOT NULL,
    status TEXT NOT NULL,
    author TEXT NOT NULL,
    patch_ref TEXT,
    attribution_ids TEXT,
    rollback_of TEXT,
    regression_run_id TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS bw_regression_case (
    case_id TEXT PRIMARY KEY,
    case_set TEXT NOT NULL,
    query TEXT,
    expected_basis TEXT,
    origin_cr_id TEXT
);

CREATE VIEW IF NOT EXISTS bw_current_truth AS
SELECT * FROM bw_context_unit
WHERE valid_to IS NULL
  AND superseded_by IS NULL
  AND state = 'ACTIVE';

CREATE VIRTUAL TABLE IF NOT EXISTS bw_index_fts USING fts5(
    text,
    unit_id UNINDEXED,
    representation_line UNINDEXED,
    tokenize='trigram'
);
