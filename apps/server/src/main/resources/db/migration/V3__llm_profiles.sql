create table if not exists loom_llm_profiles (
    profile_id varchar(64) primary key,
    preset_id varchar(64) not null,
    provider varchar(64) not null,
    display_name varchar(255) not null,
    api_base_url varchar(512) not null,
    model_id varchar(128) not null,
    api_key varchar(512) not null,
    system_prompt text not null,
    temperature double not null,
    max_tokens integer null,
    timeout_ms integer not null,
    is_active boolean not null default false,
    updated_at timestamp not null
);
