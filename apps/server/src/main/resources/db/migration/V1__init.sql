create table if not exists workspace_settings (
    settings_key varchar(64) primary key,
    workspace_name varchar(255) not null,
    support_email varchar(255) not null,
    docs_url varchar(512) not null,
    default_refresh_interval_seconds integer not null,
    updated_at timestamp not null
);

create table if not exists loom_nodes (
    node_id varchar(64) primary key,
    node_name varchar(255) not null,
    node_type varchar(64) not null,
    host varchar(255) not null,
    status varchar(32) not null,
    tags_json text not null,
    capabilities_json text not null,
    last_heartbeat timestamp null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table if not exists loom_node_probes (
    node_id varchar(64) not null,
    probe_name varchar(255) not null,
    probe_kind varchar(64) not null,
    target varchar(512) not null,
    status varchar(32) not null,
    detail varchar(1024) null,
    recorded_at timestamp not null,
    primary key (node_id, probe_name),
    constraint fk_loom_node_probes_node
        foreign key (node_id) references loom_nodes (node_id) on delete cascade
);

insert into workspace_settings (
    settings_key,
    workspace_name,
    support_email,
    docs_url,
    default_refresh_interval_seconds,
    updated_at
) values (
    'default',
    'Loom Workspace',
    'team@example.com',
    'https://github.com/lingfeng-xiao/loom',
    30,
    CURRENT_TIMESTAMP
)
on duplicate key update
    workspace_name = values(workspace_name),
    support_email = values(support_email),
    docs_url = values(docs_url),
    default_refresh_interval_seconds = values(default_refresh_interval_seconds),
    updated_at = values(updated_at);
