package com.loom.server.store;

import com.loom.server.model.Asset;
import com.loom.server.model.AuditLogEntry;
import com.loom.server.model.Conversation;
import com.loom.server.model.Memory;
import com.loom.server.model.Message;
import com.loom.server.model.Node;
import com.loom.server.model.Plan;
import com.loom.server.model.Project;
import com.loom.server.model.Skill;
import com.loom.server.model.WorkspaceSettings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

@Component
public class LoomStore {

    private final Map<String, Project> projects = new ConcurrentHashMap<>();
    private final Map<String, Conversation> conversations = new ConcurrentHashMap<>();
    private final Map<String, Message> messages = new ConcurrentHashMap<>();
    private final Map<String, Memory> memories = new ConcurrentHashMap<>();
    private final Map<String, Plan> plans = new ConcurrentHashMap<>();
    private final Map<String, Skill> skills = new ConcurrentHashMap<>();
    private final Map<String, Asset> assets = new ConcurrentHashMap<>();
    private final Map<String, Node> nodes = new ConcurrentHashMap<>();
    private final List<AuditLogEntry> auditLogs = new CopyOnWriteArrayList<>();
    private final AtomicReference<String> selectedProjectId = new AtomicReference<>();
    private final AtomicReference<WorkspaceSettings> workspaceSettings = new AtomicReference<>();

    public Map<String, Project> projects() {
        return projects;
    }

    public Map<String, Conversation> conversations() {
        return conversations;
    }

    public Map<String, Message> messages() {
        return messages;
    }

    public Map<String, Memory> memories() {
        return memories;
    }

    public Map<String, Plan> plans() {
        return plans;
    }

    public Map<String, Skill> skills() {
        return skills;
    }

    public Map<String, Asset> assets() {
        return assets;
    }

    public Map<String, Node> nodes() {
        return nodes;
    }

    public List<AuditLogEntry> auditLogs() {
        return auditLogs;
    }

    public String selectedProjectId() {
        return selectedProjectId.get();
    }

    public void selectedProjectId(String projectId) {
        selectedProjectId.set(projectId);
    }

    public WorkspaceSettings workspaceSettings() {
        return workspaceSettings.get();
    }

    public void workspaceSettings(WorkspaceSettings settings) {
        workspaceSettings.set(settings);
    }

    public List<Project> projectList() {
        return new ArrayList<>(projects.values());
    }

    public List<Conversation> conversationList() {
        return new ArrayList<>(conversations.values());
    }

    public List<Message> messageList() {
        return new ArrayList<>(messages.values());
    }

    public List<Memory> memoryList() {
        return new ArrayList<>(memories.values());
    }

    public List<Plan> planList() {
        return new ArrayList<>(plans.values());
    }

    public List<Skill> skillList() {
        return new ArrayList<>(skills.values());
    }

    public List<Asset> assetList() {
        return new ArrayList<>(assets.values());
    }

    public List<Node> nodeList() {
        return new ArrayList<>(nodes.values());
    }

    public List<AuditLogEntry> recentAuditLogs() {
        int size = auditLogs.size();
        if (size <= 20) {
            return Collections.unmodifiableList(new ArrayList<>(auditLogs));
        }
        return Collections.unmodifiableList(new ArrayList<>(auditLogs.subList(size - 20, size)));
    }
}
