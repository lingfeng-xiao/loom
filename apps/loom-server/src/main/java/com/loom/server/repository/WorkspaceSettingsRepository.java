package com.loom.server.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.loom.server.model.AppearanceSettings;
import com.loom.server.model.CommandId;
import com.loom.server.model.ModelSettings;
import com.loom.server.model.SkillId;
import com.loom.server.model.VaultSettings;
import com.loom.server.model.WorkspaceNodeSettings;
import com.loom.server.model.WorkspaceSettings;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class WorkspaceSettingsRepository extends JdbcRepositorySupport {

    private static final int WORKSPACE_SETTINGS_ID = 1;
    private static final TypeReference<List<CommandId>> COMMAND_LIST = new TypeReference<>() {
    };
    private static final TypeReference<List<SkillId>> SKILL_LIST = new TypeReference<>() {
    };

    public WorkspaceSettingsRepository(JdbcClient jdbcClient, JdbcJsonSupport jsonSupport) {
        super(jdbcClient, jsonSupport);
    }

    public Optional<WorkspaceSettings> find() {
        return jdbcClient.sql("select * from workspace_settings where id = :id")
                .param("id", WORKSPACE_SETTINGS_ID)
                .query(this::mapRow)
                .optional();
    }

    public void save(WorkspaceSettings settings) {
        jdbcClient.sql("""
                insert into workspace_settings (
                    id, workspace_name, language, density, default_project_id, default_landing_view,
                    inspector_default_open, model, vault, nodes, appearance, enabled_commands,
                    enabled_skills, updated_at
                ) values (
                    :id, :workspaceName, :language, :density, :defaultProjectId, :defaultLandingView,
                    :inspectorDefaultOpen, :model, :vault, :nodes, :appearance, :enabledCommands,
                    :enabledSkills, :updatedAt
                )
                on duplicate key update
                    workspace_name = values(workspace_name),
                    language = values(language),
                    density = values(density),
                    default_project_id = values(default_project_id),
                    default_landing_view = values(default_landing_view),
                    inspector_default_open = values(inspector_default_open),
                    model = values(model),
                    vault = values(vault),
                    nodes = values(nodes),
                    appearance = values(appearance),
                    enabled_commands = values(enabled_commands),
                    enabled_skills = values(enabled_skills),
                    updated_at = values(updated_at)
                """)
                .param("id", WORKSPACE_SETTINGS_ID)
                .param("workspaceName", settings.workspaceName())
                .param("language", settings.language())
                .param("density", settings.density())
                .param("defaultProjectId", settings.defaultProjectId())
                .param("defaultLandingView", settings.defaultLandingView())
                .param("inspectorDefaultOpen", settings.inspectorDefaultOpen())
                .param("model", jsonSupport.write(settings.model()))
                .param("vault", jsonSupport.write(settings.vault()))
                .param("nodes", jsonSupport.write(settings.nodes()))
                .param("appearance", jsonSupport.write(settings.appearance()))
                .param("enabledCommands", jsonSupport.write(settings.enabledCommands()))
                .param("enabledSkills", jsonSupport.write(settings.enabledSkills()))
                .param("updatedAt", settings.updatedAt())
                .update();
    }

    private WorkspaceSettings mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        return new WorkspaceSettings(
                resultSet.getString("workspace_name"),
                resultSet.getString("language"),
                resultSet.getString("density"),
                resultSet.getString("default_project_id"),
                resultSet.getString("default_landing_view"),
                resultSet.getBoolean("inspector_default_open"),
                jsonSupport.read(resultSet.getString("model"), ModelSettings.class, () -> null),
                jsonSupport.read(resultSet.getString("vault"), VaultSettings.class, () -> null),
                jsonSupport.read(resultSet.getString("nodes"), WorkspaceNodeSettings.class, () -> null),
                jsonSupport.read(resultSet.getString("appearance"), AppearanceSettings.class, () -> null),
                jsonSupport.read(resultSet.getString("enabled_commands"), COMMAND_LIST, List::of),
                jsonSupport.read(resultSet.getString("enabled_skills"), SKILL_LIST, List::of),
                instant(resultSet, "updated_at")
        );
    }
}
