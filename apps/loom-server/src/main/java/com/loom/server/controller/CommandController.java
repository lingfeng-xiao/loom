package com.loom.server.controller;

import com.loom.server.api.ApiEnvelope;
import com.loom.server.dto.CommandExecuteRequest;
import com.loom.server.model.CommandId;
import com.loom.server.service.CommandService;
import com.loom.server.support.Responses.CommandExecutionResult;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/commands")
public class CommandController {

    private final CommandService commandService;

    public CommandController(CommandService commandService) {
        this.commandService = commandService;
    }

    @GetMapping
    public ApiEnvelope<List<CommandId>> list() {
        return ApiEnvelope.of(commandService.listCommands());
    }

    @PostMapping("/execute")
    public ApiEnvelope<CommandExecutionResult> execute(@Valid @RequestBody CommandExecuteRequest request) {
        return ApiEnvelope.of(commandService.execute(request));
    }
}
