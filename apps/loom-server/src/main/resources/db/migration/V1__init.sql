CREATE TABLE IF NOT EXISTS projects (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(32) NOT NULL,
    description TEXT NOT NULL,
    default_skills JSON NOT NULL,
    default_commands JSON NOT NULL,
    bound_node_ids JSON NOT NULL,
    knowledge_roots JSON NOT NULL,
    project_memory_refs JSON NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL
);

CREATE TABLE IF NOT EXISTS conversations (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    mode VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    summary TEXT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    INDEX idx_conversations_project_updated (project_id, updated_at DESC),
    CONSTRAINT fk_conversations_project FOREIGN KEY (project_id) REFERENCES projects (id)
);

CREATE TABLE IF NOT EXISTS messages (
    id VARCHAR(64) PRIMARY KEY,
    conversation_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    role VARCHAR(32) NOT NULL,
    content LONGTEXT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    INDEX idx_messages_conversation_created (conversation_id, created_at),
    CONSTRAINT fk_messages_conversation FOREIGN KEY (conversation_id) REFERENCES conversations (id),
    CONSTRAINT fk_messages_project FOREIGN KEY (project_id) REFERENCES projects (id)
);

CREATE TABLE IF NOT EXISTS memories (
    id VARCHAR(64) PRIMARY KEY,
    scope VARCHAR(32) NOT NULL,
    project_id VARCHAR(64) NULL,
    title VARCHAR(255) NOT NULL,
    content LONGTEXT NOT NULL,
    priority INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    source_type VARCHAR(128) NOT NULL,
    source_ref VARCHAR(255) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    INDEX idx_memories_project_updated (project_id, updated_at DESC),
    CONSTRAINT fk_memories_project FOREIGN KEY (project_id) REFERENCES projects (id)
);

CREATE TABLE IF NOT EXISTS plans (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL,
    conversation_id VARCHAR(64) NOT NULL,
    goal TEXT NOT NULL,
    constraints_json JSON NOT NULL,
    status VARCHAR(32) NOT NULL,
    approval_required BOOLEAN NOT NULL,
    execution_result JSON NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    INDEX idx_plans_project_updated (project_id, updated_at DESC),
    CONSTRAINT fk_plans_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_plans_conversation FOREIGN KEY (conversation_id) REFERENCES conversations (id)
);

CREATE TABLE IF NOT EXISTS plan_steps (
    id VARCHAR(64) PRIMARY KEY,
    plan_id VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    result TEXT NOT NULL,
    sort_order INT NOT NULL,
    INDEX idx_plan_steps_plan_sort (plan_id, sort_order),
    CONSTRAINT fk_plan_steps_plan FOREIGN KEY (plan_id) REFERENCES plans (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS skills (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    version VARCHAR(64) NOT NULL,
    description TEXT NOT NULL,
    trigger_mode VARCHAR(32) NOT NULL,
    instruction_ref VARCHAR(255) NOT NULL,
    resource_ref VARCHAR(255) NULL,
    tool_bindings JSON NOT NULL,
    scope VARCHAR(64) NOT NULL,
    enabled BOOLEAN NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL
);

CREATE TABLE IF NOT EXISTS assets (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL,
    type VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content_ref LONGTEXT NOT NULL,
    storage_path LONGTEXT NOT NULL,
    source_conversation_id VARCHAR(64) NULL,
    source_plan_id VARCHAR(64) NULL,
    source_node_id VARCHAR(64) NULL,
    tags JSON NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    INDEX idx_assets_project_created (project_id, created_at DESC),
    CONSTRAINT fk_assets_project FOREIGN KEY (project_id) REFERENCES projects (id)
);

CREATE TABLE IF NOT EXISTS nodes (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(32) NOT NULL,
    host VARCHAR(255) NOT NULL,
    tags JSON NOT NULL,
    status VARCHAR(32) NOT NULL,
    last_heartbeat TIMESTAMP(6) NULL,
    snapshot JSON NULL,
    capabilities JSON NOT NULL,
    last_error TEXT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    UNIQUE KEY uk_nodes_name_host (name, host)
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id VARCHAR(64) PRIMARY KEY,
    actor VARCHAR(255) NOT NULL,
    source VARCHAR(255) NOT NULL,
    target_type VARCHAR(255) NOT NULL,
    target_id VARCHAR(255) NOT NULL,
    action VARCHAR(255) NOT NULL,
    payload_hash VARCHAR(255) NOT NULL,
    result VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    INDEX idx_audit_logs_created (created_at DESC)
);

CREATE TABLE IF NOT EXISTS workspace_settings (
    id INT PRIMARY KEY,
    workspace_name VARCHAR(255) NOT NULL,
    language VARCHAR(32) NOT NULL,
    density VARCHAR(32) NOT NULL,
    default_project_id VARCHAR(64) NULL,
    default_landing_view VARCHAR(64) NOT NULL,
    inspector_default_open BOOLEAN NOT NULL,
    model JSON NOT NULL,
    vault JSON NOT NULL,
    nodes JSON NOT NULL,
    appearance JSON NOT NULL,
    enabled_commands JSON NOT NULL,
    enabled_skills JSON NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL
);
