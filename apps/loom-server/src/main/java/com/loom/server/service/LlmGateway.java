package com.loom.server.service;

import com.loom.server.model.Conversation;
import com.loom.server.model.Message;
import com.loom.server.model.ModelSettings;
import com.loom.server.model.Project;
import java.util.List;

public interface LlmGateway {

    String complete(Project project, Conversation conversation, List<Message> messages, ModelSettings settings);
}
