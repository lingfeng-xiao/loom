package com.loom.server.repository;

import com.loom.server.dto.WorkspaceSettingsUpdateRequest;
import com.loom.server.model.WorkspaceSettings;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;

@Repository
public class SettingsRepository {

    private static final String SETTINGS_KEY = "default";
    private final JdbcClient jdbcClient;

    public SettingsRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public WorkspaceSettings get() {
        return jdbcClient.sql("""
                        select workspace_name, support_email, docs_url, default_refresh_interval_seconds, updated_at
                        from workspace_settings
                        where settings_key = :settingsKey
                        """)
                .param("settingsKey", SETTINGS_KEY)
                .query((rs, rowNum) -> new WorkspaceSettings(
                        rs.getString("workspace_name"),
                        rs.getString("support_email"),
                        rs.getString("docs_url"),
                        rs.getInt("default_refresh_interval_seconds"),
                        rs.getTimestamp("updated_at").toInstant()
                ))
                .single();
    }

    public WorkspaceSettings update(WorkspaceSettingsUpdateRequest request) {
        Instant now = Instant.now();
        jdbcClient.sql("""
                        update workspace_settings
                        set workspace_name = :workspaceName,
                            support_email = :supportEmail,
                            docs_url = :docsUrl,
                            default_refresh_interval_seconds = :refreshInterval,
                            updated_at = :updatedAt
                        where settings_key = :settingsKey
                        """)
                .param("workspaceName", request.workspaceName())
                .param("supportEmail", request.supportEmail())
                .param("docsUrl", request.docsUrl())
                .param("refreshInterval", request.defaultRefreshIntervalSeconds())
                .param("updatedAt", Timestamp.from(now))
                .param("settingsKey", SETTINGS_KEY)
                .update();
        return get();
    }
}
