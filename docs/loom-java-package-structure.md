## Loom Java Package Structure Proposal

### Root Package

```text
com.loom.backend
```

---

### Top-level Structure

```text
com.loom.backend
├── LoomApplication.java
├── common
├── project
├── conversation
├── context
├── memory
├── action
├── capability
├── file
├── settings
└── stream
```

---

### common

```text
common
├── config
├── error
├── id
├── json
├── security
├── time
├── util
└── web
```

Suggested classes:

- `GlobalExceptionHandler`
- `ApiResponse`
- `ErrorCode`
- `BusinessException`
- `TraceIdFilter`
- `JacksonConfig`

---

### project

```text
project
├── controller
├── application
├── domain
├── infrastructure
└── dto
```

Suggested classes:

- `ProjectController`
- `ProjectCommandService`
- `ProjectQueryService`
- `Project`
- `ProjectRepository`
- `CreateProjectRequest`
- `ProjectView`

---

### conversation

```text
conversation
├── controller
├── application
├── domain
├── infrastructure
└── dto
```

Suggested classes:

- `ConversationController`
- `MessageController`
- `ConversationCommandService`
- `ConversationQueryService`
- `MessageCommandService`
- `MessageQueryService`
- `ConversationReplyService`
- `Conversation`
- `Message`
- `ConversationRepository`
- `MessageRepository`
- `SubmitMessageRequest`
- `ConversationView`
- `MessageView`

---

### context

```text
context
├── controller
├── application
├── domain
├── infrastructure
└── dto
```

Suggested classes:

- `ContextController`
- `ContextAssemblyService`
- `ConversationSummaryService`
- `DecisionExtractionService`
- `OpenLoopService`
- `ContextPanelService`
- `ContextSnapshot`
- `ContextSnapshotRepository`
- `ConversationContextView`

---

### memory

```text
memory
├── controller
├── application
├── domain
├── infrastructure
└── dto
```

Suggested classes:

- `MemoryController`
- `MemoryCommandService`
- `MemoryQueryService`
- `MemorySuggestionService`
- `MemoryRetrievalService`
- `MemoryItem`
- `MemorySuggestion`
- `MemoryItemRepository`
- `MemorySuggestionRepository`
- `MemoryItemView`
- `AcceptMemorySuggestionRequest`

---

### action

```text
action
├── controller
├── application
├── domain
├── infrastructure
└── dto
```

Suggested classes:

- `ActionController`
- `RunController`
- `ActionCommandService`
- `ActionQueryService`
- `RunExecutionService`
- `RunStepService`
- `TracePanelService`
- `Action`
- `Run`
- `RunStep`
- `Artifact`
- `ActionRepository`
- `RunRepository`
- `RunStepRepository`
- `RunView`
- `RunStepView`

---

### capability

```text
capability
├── controller
├── application
├── domain
├── infrastructure
├── mcp
├── model
└── dto
```

Suggested classes:

- `CapabilityController`
- `ModelProfileController`
- `ModelProfileService`
- `ModelRuntimeService`
- `SkillRuntimeService`
- `McpRuntimeService`
- `CapabilityRegistryService`
- `ModelProfile`
- `Skill`
- `McpServer`
- `ModelProfileRepository`
- `SkillRepository`
- `McpServerRepository`

Optional adapter structure:

```text
capability/model
├── ModelClient
├── OpenAiCompatibleModelClient
└── ModelClientFactory

capability/mcp
├── McpClient
├── McpResourceAdapter
├── McpPromptAdapter
└── McpToolAdapter
```

---

### file

```text
file
├── controller
├── application
├── domain
├── infrastructure
└── dto
```

Suggested classes:

- `FileController`
- `FileUploadService`
- `FileQueryService`
- `FileSummaryService`
- `AttachmentService`
- `FileAsset`
- `MessageAssetRef`
- `FileAssetRepository`
- `MessageAssetRefRepository`

---

### settings

```text
settings
├── controller
├── application
├── domain
├── infrastructure
└── dto
```

Suggested classes:

- `SettingsController`
- `ModelProfileSettingsService`
- `McpSettingsService`
- `MemoryPolicyService`
- `RoutingPolicyService`
- `SkillSettingsService`
- `SettingMemoryPolicy`
- `SettingRoutingPolicy`

---

### stream

```text
stream
├── controller
├── application
├── dto
└── infrastructure
```

Suggested classes:

- `ConversationStreamController`
- `SseSessionService`
- `ConversationEventPublisher`
- `RunEventPublisher`
- `StreamEvent`

---

### Optional Shared Interfaces

Later, if needed, add a small package:

```text
common/spi
```

Candidate interfaces:

- `ExecutorAdapter`
- `ModelClient`
- `SkillExecutor`
- `ContextProvider`
- `ExternalCallbackHandler`

These should remain small and focused.

---

### Package Rules

1. controllers only depend on application services and DTOs
2. application services coordinate use cases
3. domain holds core entities and enums
4. infrastructure holds repositories and adapter implementations
5. modules should not directly access each other's repositories
6. cross-module interaction should happen through services

---

### Suggested Naming Conventions

Use these patterns consistently:

#### Controllers
- `ProjectController`
- `ConversationController`
- `MemoryController`

#### Command services
- `ProjectCommandService`
- `MessageCommandService`

#### Query services
- `ProjectQueryService`
- `MessageQueryService`

#### Runtime services
- `ModelRuntimeService`
- `SkillRuntimeService`
- `RunExecutionService`

#### View DTOs
- `ProjectView`
- `ConversationView`
- `RunView`

#### Request DTOs
- `CreateProjectRequest`
- `SubmitMessageRequest`

---

### Final Recommendation

Keep package structure boring, predictable, and domain-oriented.

Do not group by technical layer globally like:

- controller
- service
- repository

Instead group by domain module first, then by layer inside the module.

That shape is much easier to scale as loom grows.
