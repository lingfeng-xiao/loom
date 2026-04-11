CREATE TABLE IF NOT EXISTS loom_nodes (
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

CREATE TABLE IF NOT EXISTS loom_node_probes (
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