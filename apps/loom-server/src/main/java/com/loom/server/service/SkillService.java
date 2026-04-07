package com.loom.server.service;

import com.loom.server.dto.SkillInvokeRequest;
import com.loom.server.model.Skill;
import com.loom.server.model.SkillId;
import com.loom.server.repository.SkillRepository;
import com.loom.server.support.Responses.SkillInvocationResult;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SkillService {

    private final SkillRepository skillRepository;
    private final AuditService auditService;

    public SkillService(SkillRepository skillRepository, AuditService auditService) {
        this.skillRepository = skillRepository;
        this.auditService = auditService;
    }

    public List<Skill> listSkills() {
        if (skillRepository.count() == 0) {
            seedDefaultSkills();
        }
        return skillRepository.findAll();
    }

    public Skill getSkill(String id) {
        if (skillRepository.count() == 0) {
            seedDefaultSkills();
        }
        return skillRepository.findById(SkillId.fromValue(id))
                .orElseThrow(() -> new IllegalArgumentException("未知技能 " + id));
    }

    public SkillInvocationResult invoke(String skillId, SkillInvokeRequest request) {
        Skill skill = getSkill(skillId);
        String output = switch (SkillId.fromValue(skillId)) {
            case KNOWLEDGE_CARD_GENERATOR -> "已生成知识卡片草稿。";
            case OPS_SUMMARY_GENERATOR -> "已生成运维总结草稿。";
            case OBSIDIAN_NOTE_WRITER -> "已准备好写入 Obsidian 的 Markdown 内容。";
        };
        auditService.record("system", "skill-service", "skill", skill.id().value(), "invoke", request.input(), "ok");
        return new SkillInvocationResult(skill.id().value(), output, null, output);
    }

    private void seedDefaultSkills() {
        Instant now = Instant.now();
        skillRepository.save(new Skill(
                SkillId.KNOWLEDGE_CARD_GENERATOR,
                "知识卡片生成器",
                "0.1.0",
                "将输入整理成知识卡片草稿",
                "manual",
                "knowledge-card-generator",
                null,
                List.of(),
                "project",
                true,
                now,
                now
        ));
        skillRepository.save(new Skill(
                SkillId.OPS_SUMMARY_GENERATOR,
                "运维总结生成器",
                "0.1.0",
                "将节点状态整理成运维总结草稿",
                "manual",
                "ops-summary-generator",
                null,
                List.of(),
                "project",
                true,
                now,
                now
        ));
        skillRepository.save(new Skill(
                SkillId.OBSIDIAN_NOTE_WRITER,
                "Obsidian 笔记写入器",
                "0.1.0",
                "将 Markdown 资产写入 Vault",
                "manual",
                "obsidian-note-writer",
                null,
                List.of(),
                "project",
                true,
                now,
                now
        ));
    }
}
