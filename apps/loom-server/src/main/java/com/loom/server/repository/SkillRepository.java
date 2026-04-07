package com.loom.server.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.loom.server.model.Skill;
import com.loom.server.model.SkillId;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class SkillRepository extends JdbcRepositorySupport {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    public SkillRepository(JdbcClient jdbcClient, JdbcJsonSupport jsonSupport) {
        super(jdbcClient, jsonSupport);
    }

    public List<Skill> findAll() {
        return jdbcClient.sql("select * from skills order by created_at")
                .query(this::mapRow)
                .list();
    }

    public Optional<Skill> findById(SkillId id) {
        return jdbcClient.sql("select * from skills where id = :id")
                .param("id", id.value())
                .query(this::mapRow)
                .optional();
    }

    public long count() {
        Long value = jdbcClient.sql("select count(*) from skills").query(Long.class).single();
        return value == null ? 0 : value;
    }

    public void save(Skill skill) {
        jdbcClient.sql("""
                insert into skills (
                    id, name, version, description, trigger_mode, instruction_ref, resource_ref, tool_bindings, scope, enabled, created_at, updated_at
                ) values (
                    :id, :name, :version, :description, :triggerMode, :instructionRef, :resourceRef, :toolBindings, :scope, :enabled, :createdAt, :updatedAt
                )
                on duplicate key update
                    name = values(name),
                    version = values(version),
                    description = values(description),
                    trigger_mode = values(trigger_mode),
                    instruction_ref = values(instruction_ref),
                    resource_ref = values(resource_ref),
                    tool_bindings = values(tool_bindings),
                    scope = values(scope),
                    enabled = values(enabled),
                    updated_at = values(updated_at)
                """)
                .param("id", skill.id().value())
                .param("name", skill.name())
                .param("version", skill.version())
                .param("description", skill.description())
                .param("triggerMode", skill.triggerMode())
                .param("instructionRef", skill.instructionRef())
                .param("resourceRef", skill.resourceRef())
                .param("toolBindings", jsonSupport.write(skill.toolBindings()))
                .param("scope", skill.scope())
                .param("enabled", skill.enabled())
                .param("createdAt", skill.createdAt())
                .param("updatedAt", skill.updatedAt())
                .update();
    }

    private Skill mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        return new Skill(
                SkillId.fromValue(resultSet.getString("id")),
                resultSet.getString("name"),
                resultSet.getString("version"),
                resultSet.getString("description"),
                resultSet.getString("trigger_mode"),
                resultSet.getString("instruction_ref"),
                resultSet.getString("resource_ref"),
                jsonSupport.read(resultSet.getString("tool_bindings"), STRING_LIST, List::of),
                resultSet.getString("scope"),
                resultSet.getBoolean("enabled"),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at")
        );
    }
}
