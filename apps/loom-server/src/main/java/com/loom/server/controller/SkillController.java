package com.loom.server.controller;

import com.loom.server.api.ApiEnvelope;
import com.loom.server.dto.SkillInvokeRequest;
import com.loom.server.model.Skill;
import com.loom.server.service.SkillService;
import com.loom.server.support.Responses.SkillInvocationResult;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/skills")
public class SkillController {

    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    @GetMapping
    public ApiEnvelope<List<Skill>> list() {
        return ApiEnvelope.of(skillService.listSkills());
    }

    @GetMapping("/{skillId}")
    public ApiEnvelope<Skill> get(@PathVariable String skillId) {
        return ApiEnvelope.of(skillService.getSkill(skillId));
    }

    @PostMapping("/{skillId}/invoke")
    public ApiEnvelope<SkillInvocationResult> invoke(@PathVariable String skillId, @Valid @RequestBody SkillInvokeRequest request) {
        return ApiEnvelope.of(skillService.invoke(skillId, request));
    }
}
