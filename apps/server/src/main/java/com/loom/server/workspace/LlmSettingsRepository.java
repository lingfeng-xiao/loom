package com.loom.server.workspace;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
class LlmSettingsRepository {
    private static final String DEFAULT_KEY = "default";

    private final JdbcTemplate jdbcTemplate;

    LlmSettingsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    List<LlmConfigState> loadProfiles() {
        List<LlmConfigState> rows = jdbcTemplate.query(
                """
                select profile_id, preset_id, provider, display_name, api_base_url, model_id, api_key, system_prompt, temperature, max_tokens, timeout_ms, is_active, updated_at
                from loom_llm_profiles
                order by is_active desc, updated_at desc
                """,
                (resultSet, _rowNum) -> mapProfileRow(resultSet)
        );
        if (!rows.isEmpty()) {
            return rows;
        }

        return loadLegacyDefault().map(List::of).orElse(List.of());
    }

    void saveProfiles(List<LlmConfigState> states) {
        for (LlmConfigState state : states) {
            jdbcTemplate.update(
                    """
                    merge into loom_llm_profiles key(profile_id)
                    values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    state.id,
                    state.presetId,
                    state.provider,
                    state.displayName,
                    state.apiBaseUrl,
                    state.modelId,
                    state.apiKey == null ? "" : state.apiKey,
                    state.systemPrompt,
                    state.temperature,
                    state.maxTokens,
                    state.timeoutMs,
                    state.active,
                    Timestamp.from(Instant.parse(state.updatedAt))
            );
        }

        states.stream().filter(state -> state.active).findFirst().ifPresent(this::saveLegacyDefault);
    }

    private Optional<LlmConfigState> loadLegacyDefault() {
        List<LlmConfigState> rows = jdbcTemplate.query(
                """
                select preset_id, provider, display_name, api_base_url, model_id, api_key, system_prompt, temperature, max_tokens, timeout_ms, updated_at
                from loom_llm_settings
                where settings_key = ?
                """,
                (resultSet, _rowNum) -> mapLegacyRow(resultSet),
                DEFAULT_KEY
        );
        return rows.stream().findFirst();
    }

    private void saveLegacyDefault(LlmConfigState state) {
        jdbcTemplate.update(
                """
                merge into loom_llm_settings key(settings_key)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                DEFAULT_KEY,
                state.presetId,
                state.provider,
                state.displayName,
                state.apiBaseUrl,
                state.modelId,
                state.apiKey == null ? "" : state.apiKey,
                state.systemPrompt,
                state.temperature,
                state.maxTokens,
                state.timeoutMs,
                Timestamp.from(Instant.parse(state.updatedAt))
        );
    }

    private static LlmConfigState mapProfileRow(ResultSet resultSet) throws SQLException {
        return new LlmConfigState(
                resultSet.getString("profile_id"),
                resultSet.getString("preset_id"),
                resultSet.getString("provider"),
                resultSet.getString("display_name"),
                resultSet.getString("api_base_url"),
                resultSet.getString("model_id"),
                resultSet.getString("api_key"),
                resultSet.getString("system_prompt"),
                resultSet.getDouble("temperature"),
                (Integer) resultSet.getObject("max_tokens"),
                resultSet.getInt("timeout_ms"),
                resultSet.getBoolean("is_active"),
                resultSet.getTimestamp("updated_at").toInstant().toString()
        );
    }

    private static LlmConfigState mapLegacyRow(ResultSet resultSet) throws SQLException {
        return new LlmConfigState(
                "profile-default",
                resultSet.getString("preset_id"),
                resultSet.getString("provider"),
                resultSet.getString("display_name"),
                resultSet.getString("api_base_url"),
                resultSet.getString("model_id"),
                resultSet.getString("api_key"),
                resultSet.getString("system_prompt"),
                resultSet.getDouble("temperature"),
                (Integer) resultSet.getObject("max_tokens"),
                resultSet.getInt("timeout_ms"),
                true,
                resultSet.getTimestamp("updated_at").toInstant().toString()
        );
    }
}
