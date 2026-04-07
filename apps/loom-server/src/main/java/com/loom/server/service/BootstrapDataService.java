package com.loom.server.service;

import com.loom.server.config.LoomProperties;
import com.loom.server.dto.AssetFromConversationRequest;
import com.loom.server.dto.AssetFromPlanRequest;
import com.loom.server.dto.ConversationCreateRequest;
import com.loom.server.dto.ConversationUpdateRequest;
import com.loom.server.dto.MemoryCreateRequest;
import com.loom.server.dto.MessageCreateRequest;
import com.loom.server.dto.NodeRegisterRequest;
import com.loom.server.dto.ProjectCreateRequest;
import com.loom.server.model.CommandId;
import com.loom.server.model.ConversationMode;
import com.loom.server.model.ConversationStatus;
import com.loom.server.model.MemoryScope;
import com.loom.server.model.NodeType;
import com.loom.server.model.ProjectType;
import com.loom.server.model.SkillId;
import java.util.List;
import java.util.Map;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BootstrapDataService {

    @Bean
    CommandLineRunner seed(
            LoomProperties properties,
            ProjectService projectService,
            ConversationService conversationService,
            MemoryService memoryService,
            NodeService nodeService,
            SkillService skillService,
            PlanService planService,
            AssetService assetService
    ) {
        return args -> {
            if (!properties.getBootstrap().isEnabled() || !projectService.listProjects().isEmpty()) {
                return;
            }

            var localNode = nodeService.registerNode(
                    new NodeRegisterRequest("本地电脑", NodeType.LOCAL_PC, "localhost", List.of("desktop"), List.of("system-metrics"))
            );
            var serverNode = nodeService.registerNode(
                    new NodeRegisterRequest("JD 中心节点", NodeType.SERVER, "114.67.156.250:2222", List.of("server"), List.of("system-metrics", "probe-status"))
            );

            var knowledge = projectService.createProject(new ProjectCreateRequest(
                    "知识库",
                    ProjectType.KNOWLEDGE,
                    "用于沉淀长期技术知识与生成知识卡片的工作区",
                    List.of(SkillId.KNOWLEDGE_CARD_GENERATOR, SkillId.OBSIDIAN_NOTE_WRITER),
                    List.of(CommandId.PROJECT_STATUS, CommandId.PLAN, CommandId.SAVE_CARD, CommandId.SKILL_LIST),
                    List.of(localNode.id()),
                    List.of("docs", "notes")
            ));
            var ops = projectService.createProject(new ProjectCreateRequest(
                    "运维控制台",
                    ProjectType.OPS,
                    "用于查看节点状态、诊断信息和运维总结的工作区",
                    List.of(SkillId.OPS_SUMMARY_GENERATOR, SkillId.OBSIDIAN_NOTE_WRITER),
                    List.of(CommandId.PROJECT_STATUS, CommandId.NODE_STATUS, CommandId.LOGS),
                    List.of(serverNode.id()),
                    List.of("ops")
            ));
            projectService.createProject(new ProjectCreateRequest(
                    "英语实验室",
                    ProjectType.LEARNING,
                    "用于语言练习和学习笔记的工作区",
                    List.of(SkillId.KNOWLEDGE_CARD_GENERATOR),
                    List.of(CommandId.PROJECT_STATUS, CommandId.SAVE_CARD),
                    List.of(),
                    List.of("study")
            ));

            var knowledgeChat = conversationService.createConversation(
                    knowledge.id(),
                    new ConversationCreateRequest("Loom Phase 1 交付方案", ConversationMode.CHAT, "整理当前 MVP 的边界、工程风险和后续修复顺序")
            );
            var knowledgePlanConversation = conversationService.createConversation(
                    knowledge.id(),
                    new ConversationCreateRequest("项目级工作台重构", ConversationMode.PLAN, "把单页式前端重构为项目级会话工作台")
            );
            var archivedConversation = conversationService.createConversation(
                    knowledge.id(),
                    new ConversationCreateRequest("知识卡片模板整理", ConversationMode.CHAT, "沉淀知识卡片的字段模板和写入规则")
            );
            conversationService.updateConversation(
                    archivedConversation.id(),
                    new ConversationUpdateRequest(null, ConversationStatus.ARCHIVED, archivedConversation.summary())
            );

            var opsPlanConversation = conversationService.createConversation(
                    ops.id(),
                    new ConversationCreateRequest("JD 部署验收", ConversationMode.PLAN, "确认中心节点部署和冒烟测试项")
            );
            var opsChatConversation = conversationService.createConversation(
                    ops.id(),
                    new ConversationCreateRequest("节点状态巡检", ConversationMode.CHAT, "检查心跳、服务探测和快照质量")
            );

            conversationService.addMessage(knowledgeChat.id(), new MessageCreateRequest("user", "把这次 Loom Phase 1 的实现、问题和验收都整理清楚。"));
            conversationService.addMessage(knowledgeChat.id(), new MessageCreateRequest("assistant", "当前重点是把前端工作台、数据库持久化和容器化部署一起补齐。"));
            conversationService.addMessage(knowledgePlanConversation.id(), new MessageCreateRequest("user", "重构成项目级会话工作台，强调项目、多轮切换和设置中心。"));
            conversationService.addMessage(knowledgePlanConversation.id(), new MessageCreateRequest("assistant", "会先补 contracts 和后端接口，再重建工作台壳层和聊天体验。"));
            conversationService.addMessage(opsPlanConversation.id(), new MessageCreateRequest("user", "确认 jd 上的服务、节点和 Web 入口是否都能验收。"));
            conversationService.addMessage(opsPlanConversation.id(), new MessageCreateRequest("assistant", "部署会收敛到独立 compose 栈，通过 Nginx 同源提供 Web 和 API。"));
            conversationService.addMessage(opsChatConversation.id(), new MessageCreateRequest("assistant", "节点探测已接入真实 HTTP/TCP probe，接下来重点是服务端状态持久化和降级判定。"));

            memoryService.createMemory(new MemoryCreateRequest(
                    MemoryScope.GLOBAL,
                    null,
                    "输出风格",
                    "优先给出结构清晰、能直接执行的答案，少做泛泛而谈的总结。",
                    95,
                    "bootstrap",
                    "seed"
            ));
            memoryService.createMemory(new MemoryCreateRequest(
                    MemoryScope.PROJECT,
                    knowledge.id(),
                    "前端重构方向",
                    "工作台必须像成熟项目管理产品，重点突出项目级会话管理和多轮切换。",
                    90,
                    "conversation",
                    knowledgePlanConversation.id()
            ));
            memoryService.createMemory(new MemoryCreateRequest(
                    MemoryScope.PROJECT,
                    ops.id(),
                    "节点安全边界",
                    "Phase 1 只允许只读节点状态，不开放 shell 和自由命令执行。",
                    98,
                    "design-doc",
                    "loom_formal_plan"
            ));

            var knowledgePlan = planService.createPlan(
                    knowledge.id(),
                    knowledgePlanConversation.id(),
                    "把 Loom Web 重构成专业的项目级会话工作台",
                    Map.of(
                            "constraints", "项目优先|真实设置中心|保持中文优先|允许必要接口扩展",
                            "steps", "补齐 contracts 与后端接口|重建前端壳层与路由|联调与回归"
                    )
            );
            planService.approvePlan(knowledgePlan.id(), "bootstrap");
            planService.runPlan(knowledgePlan.id());

            var opsPlan = planService.createPlan(
                    ops.id(),
                    opsPlanConversation.id(),
                    "完成 jd 环境的部署验收并沉淀问题台账",
                    Map.of(
                            "constraints", "独立容器栈|记录异常并可回归验证",
                            "steps", "检查服务入口|整理问题台账"
                    )
            );
            planService.approvePlan(opsPlan.id(), "bootstrap");
            planService.completePlan(opsPlan.id(), "部署链路已跑通，后续重点补齐数据库和容器化治理。", List.of(), List.of("health 通过", "nodes 接口通过"));

            assetService.createFromConversation(new AssetFromConversationRequest(
                    knowledgePlanConversation.id(),
                    "项目级工作台重构原则",
                    "目标是形成项目级会话骨架、真实设置中心、统一信息架构和专业工作台布局。",
                    List.of("frontend", "workspace", "architecture")
            ));
            assetService.createFromPlan(new AssetFromPlanRequest(
                    opsPlan.id(),
                    "JD 部署验收记录",
                    "部署已可验收，但仍需补齐数据库持久化、容器编排和节点状态工程化。",
                    List.of("deploy", "jd", "runtime")
            ));

            skillService.listSkills();
        };
    }
}
