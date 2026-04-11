CREATE TABLE IF NOT EXISTS loom_projects (
    id VARCHAR(80) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(40) NOT NULL,
    instructions TEXT,
    last_message_at VARCHAR(40),
    updated_at VARCHAR(40) NOT NULL
);

CREATE TABLE IF NOT EXISTS loom_conversations (
    id VARCHAR(80) PRIMARY KEY,
    project_id VARCHAR(80) NOT NULL,
    title VARCHAR(255) NOT NULL,
    summary TEXT,
    mode VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    pinned BOOLEAN NOT NULL DEFAULT FALSE,
    context_summary TEXT,
    active_run_id VARCHAR(80),
    last_message_at VARCHAR(40),
    updated_at VARCHAR(40) NOT NULL
);

CREATE TABLE IF NOT EXISTS loom_messages (
    id VARCHAR(80) PRIMARY KEY,
    project_id VARCHAR(80) NOT NULL,
    conversation_id VARCHAR(80) NOT NULL,
    kind VARCHAR(40) NOT NULL,
    role VARCHAR(40) NOT NULL,
    body LONGTEXT NOT NULL,
    summary TEXT,
    status_label VARCHAR(40),
    latency_ms BIGINT,
    sequence_number INT NOT NULL,
    created_at VARCHAR(40) NOT NULL,
    completed_at VARCHAR(40)
);

CREATE TABLE IF NOT EXISTS loom_actions (
    id VARCHAR(80) PRIMARY KEY,
    project_id VARCHAR(80) NOT NULL,
    conversation_id VARCHAR(80) NOT NULL,
    run_id VARCHAR(80) NOT NULL,
    title VARCHAR(255) NOT NULL,
    status VARCHAR(40) NOT NULL,
    summary TEXT,
    started_at VARCHAR(40) NOT NULL,
    completed_at VARCHAR(40)
);

CREATE TABLE IF NOT EXISTS loom_runs (
    id VARCHAR(80) PRIMARY KEY,
    action_id VARCHAR(80) NOT NULL,
    project_id VARCHAR(80) NOT NULL,
    conversation_id VARCHAR(80) NOT NULL,
    status VARCHAR(40) NOT NULL,
    started_at VARCHAR(40) NOT NULL,
    completed_at VARCHAR(40),
    external_reference VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS loom_run_steps (
    id VARCHAR(80) PRIMARY KEY,
    run_id VARCHAR(80) NOT NULL,
    title VARCHAR(255) NOT NULL,
    detail TEXT,
    status VARCHAR(40) NOT NULL,
    started_at VARCHAR(40),
    completed_at VARCHAR(40),
    error_message TEXT
);

CREATE TABLE IF NOT EXISTS loom_context_snapshots (
    id VARCHAR(80) PRIMARY KEY,
    project_id VARCHAR(80) NOT NULL,
    conversation_id VARCHAR(80) NOT NULL,
    kind VARCHAR(40) NOT NULL,
    content LONGTEXT NOT NULL,
    updated_at VARCHAR(40) NOT NULL
);

CREATE TABLE IF NOT EXISTS loom_memory_items (
    id VARCHAR(80) PRIMARY KEY,
    scope VARCHAR(40) NOT NULL,
    project_id VARCHAR(80),
    conversation_id VARCHAR(80),
    content LONGTEXT NOT NULL,
    source VARCHAR(40) NOT NULL,
    updated_at VARCHAR(40) NOT NULL
);

CREATE TABLE IF NOT EXISTS loom_memory_suggestions (
    id VARCHAR(80) PRIMARY KEY,
    scope VARCHAR(40) NOT NULL,
    project_id VARCHAR(80),
    conversation_id VARCHAR(80),
    content LONGTEXT NOT NULL,
    status VARCHAR(40) NOT NULL,
    created_at VARCHAR(40) NOT NULL,
    updated_at VARCHAR(40) NOT NULL
);

