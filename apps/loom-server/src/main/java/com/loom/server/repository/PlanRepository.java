package com.loom.server.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.loom.server.model.Plan;
import com.loom.server.model.PlanExecutionResult;
import com.loom.server.model.PlanStatus;
import com.loom.server.model.PlanStep;
import com.loom.server.model.PlanStepStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class PlanRepository extends JdbcRepositorySupport {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    public PlanRepository(JdbcClient jdbcClient, JdbcJsonSupport jsonSupport) {
        super(jdbcClient, jsonSupport);
    }

    public List<Plan> findAll() {
        return jdbcClient.sql("select * from plans order by updated_at desc")
                .query((resultSet, rowNum) -> mapRow(resultSet))
                .list();
    }

    public List<Plan> findByProject(String projectId) {
        return jdbcClient.sql("select * from plans where project_id = :projectId order by updated_at desc")
                .param("projectId", projectId)
                .query((resultSet, rowNum) -> mapRow(resultSet))
                .list();
    }

    public Optional<Plan> findById(String id) {
        return jdbcClient.sql("select * from plans where id = :id")
                .param("id", id)
                .query((resultSet, rowNum) -> mapRow(resultSet))
                .optional();
    }

    @Transactional
    public void save(Plan plan) {
        jdbcClient.sql("""
                insert into plans (
                    id, project_id, conversation_id, goal, constraints_json, status, approval_required, execution_result, created_at, updated_at
                ) values (
                    :id, :projectId, :conversationId, :goal, :constraintsJson, :status, :approvalRequired, :executionResult, :createdAt, :updatedAt
                )
                on duplicate key update
                    project_id = values(project_id),
                    conversation_id = values(conversation_id),
                    goal = values(goal),
                    constraints_json = values(constraints_json),
                    status = values(status),
                    approval_required = values(approval_required),
                    execution_result = values(execution_result),
                    updated_at = values(updated_at)
                """)
                .param("id", plan.id())
                .param("projectId", plan.projectId())
                .param("conversationId", plan.conversationId())
                .param("goal", plan.goal())
                .param("constraintsJson", jsonSupport.write(plan.constraints()))
                .param("status", plan.status().value())
                .param("approvalRequired", plan.approvalRequired())
                .param("executionResult", plan.executionResult() == null ? null : jsonSupport.write(plan.executionResult()))
                .param("createdAt", plan.createdAt())
                .param("updatedAt", plan.updatedAt())
                .update();

        jdbcClient.sql("delete from plan_steps where plan_id = :planId")
                .param("planId", plan.id())
                .update();

        for (PlanStep step : plan.steps()) {
            jdbcClient.sql("""
                    insert into plan_steps (
                        id, plan_id, title, description, status, result, sort_order
                    ) values (
                        :id, :planId, :title, :description, :status, :result, :sortOrder
                    )
                    """)
                    .param("id", step.id())
                    .param("planId", plan.id())
                    .param("title", step.title())
                    .param("description", step.description())
                    .param("status", step.status().value())
                    .param("result", step.result())
                    .param("sortOrder", step.sortOrder())
                    .update();
        }
    }

    private Plan mapRow(ResultSet resultSet) throws SQLException {
        String planId = resultSet.getString("id");
        return new Plan(
                planId,
                resultSet.getString("project_id"),
                resultSet.getString("conversation_id"),
                resultSet.getString("goal"),
                jsonSupport.read(resultSet.getString("constraints_json"), STRING_LIST, List::of),
                PlanStatus.fromValue(resultSet.getString("status")),
                resultSet.getBoolean("approval_required"),
                findSteps(planId),
                jsonSupport.read(resultSet.getString("execution_result"), PlanExecutionResult.class, () -> null),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at")
        );
    }

    private List<PlanStep> findSteps(String planId) {
        return jdbcClient.sql("select * from plan_steps where plan_id = :planId order by sort_order")
                .param("planId", planId)
                .query(this::mapStep)
                .list();
    }

    private PlanStep mapStep(ResultSet resultSet, int rowNum) throws SQLException {
        return new PlanStep(
                resultSet.getString("id"),
                resultSet.getString("title"),
                resultSet.getString("description"),
                PlanStepStatus.fromValue(resultSet.getString("status")),
                resultSet.getString("result"),
                resultSet.getInt("sort_order")
        );
    }
}
