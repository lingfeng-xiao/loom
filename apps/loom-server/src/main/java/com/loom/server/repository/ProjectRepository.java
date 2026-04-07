package com.loom.server.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.loom.server.model.CommandId;
import com.loom.server.model.Project;
import com.loom.server.model.ProjectType;
import com.loom.server.model.SkillId;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ProjectRepository extends JdbcRepositorySupport {

    private static final TypeReference<List<SkillId>> SKILL_LIST = new TypeReference<>() {
    };
    private static final TypeReference<List<CommandId>> COMMAND_LIST = new TypeReference<>() {
    };
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    public ProjectRepository(JdbcClient jdbcClient, JdbcJsonSupport jsonSupport) {
        super(jdbcClient, jsonSupport);
    }

    public List<Project> findAll() {
        return jdbcClient.sql("select * from projects order by updated_at desc")
                .query(this::mapRow)
                .list();
    }

    public Optional<Project> findById(String id) {
        return jdbcClient.sql("select * from projects where id = :id")
                .param("id", id)
                .query(this::mapRow)
                .optional();
    }

    public long count() {
        Long value = jdbcClient.sql("select count(*) from projects").query(Long.class).single();
        return value == null ? 0 : value;
    }

    public void save(Project project) {
        jdbcClient.sql("""
                insert into projects (
                    id, name, type, description, default_skills, default_commands, bound_node_ids,
                    knowledge_roots, project_memory_refs, created_at, updated_at
                ) values (
                    :id, :name, :type, :description, :defaultSkills, :defaultCommands, :boundNodeIds,
                    :knowledgeRoots, :projectMemoryRefs, :createdAt, :updatedAt
                )
                on duplicate key update
                    name = values(name),
                    type = values(type),
                    description = values(description),
                    default_skills = values(default_skills),
                    default_commands = values(default_commands),
                    bound_node_ids = values(bound_node_ids),
                    knowledge_roots = values(knowledge_roots),
                    project_memory_refs = values(project_memory_refs),
                    updated_at = values(updated_at)
                """)
                .param("id", project.id())
                .param("name", project.name())
                .param("type", project.type().value())
                .param("description", project.description())
                .param("defaultSkills", jsonSupport.write(project.defaultSkills()))
                .param("defaultCommands", jsonSupport.write(project.defaultCommands()))
                .param("boundNodeIds", jsonSupport.write(project.boundNodeIds()))
                .param("knowledgeRoots", jsonSupport.write(project.knowledgeRoots()))
                .param("projectMemoryRefs", jsonSupport.write(project.projectMemoryRefs()))
                .param("createdAt", project.createdAt())
                .param("updatedAt", project.updatedAt())
                .update();
    }

    private Project mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        return new Project(
                resultSet.getString("id"),
                resultSet.getString("name"),
                ProjectType.fromValue(resultSet.getString("type")),
                resultSet.getString("description"),
                jsonSupport.read(resultSet.getString("default_skills"), SKILL_LIST, List::of),
                jsonSupport.read(resultSet.getString("default_commands"), COMMAND_LIST, List::of),
                jsonSupport.read(resultSet.getString("bound_node_ids"), STRING_LIST, List::of),
                jsonSupport.read(resultSet.getString("knowledge_roots"), STRING_LIST, List::of),
                jsonSupport.read(resultSet.getString("project_memory_refs"), STRING_LIST, List::of),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at")
        );
    }
}
