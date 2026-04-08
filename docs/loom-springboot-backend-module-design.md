## Loom Spring Boot Backend Module Design

### 1. Document Goal

This document defines the **Phase 1 Spring Boot backend module design** for **loom**.

Target:

- implement loom as a **modular monolith**
- keep business boundaries clear
- make conversation, context, memory, and trace first-class backend modules
- leave clean extension points for Phase 2 OpenClaw integration

This design assumes:

- Java 21
- Spring Boot
- PostgreSQL
- Redis
- SSE for streaming
- MCP as an optional capability layer in Phase 1
- OpenClaw not integrated yet

---

### 2. Why Modular Monolith First

For Phase 1, a modular monolith is the most practical choice.

Reasons:

1. conversation, context, memory, and actions are tightly coupled product capabilities
2. splitting too early into microservices will slow iteration
3. trace updates, streaming responses, and context refresh are easier to coordinate in one deployment
4. later OpenClaw integration is an external adapter problem, not a reason to split the core too early

So the right approach is:

- one deployable Spring Boot app
- clear module boundaries
- strict package separation
- thin controllers
- internal service interfaces between modules

---

### 3. Backend Top-level Modules

Phase 1 should be split into these modules:

1. `project`
2. `conversation`
3. `context`
4. `memory`
5. `action`
6. `capability`
7. `file`
8. `settings`
9. `stream`
10. `common`

These are logical modules inside one backend application.

---

### 4. Module Responsibilities

## 4.1 project module

### Responsibilities
- create/update/archive projects
- maintain project instructions
- bind default model profile / skill set / memory policy
- provide project-level summary data for sidebar and headers

### Main entities
- `Project`

### Main services
- `ProjectCommandService`
- `ProjectQueryService`

### Main APIs
- list projects
- create project
- update project
- archive project
- get project detail

---

## 4.2 conversation module

### Responsibilities
- create/update/archive conversations
- manage conversation modes
- persist messages
- manage message types
- coordinate assistant reply generation
- provide main conversation workspace data

### Main entities
- `Conversation`
- `Message`

### Main services
- `ConversationCommandService`
- `ConversationQueryService`
- `MessageCommandService`
- `MessageQueryService`
- `ConversationReplyService`

### Main APIs
- list conversations
- create conversation
- submit message
- list messages
- update conversation metadata

### Notes
This is the product center module.  
Other modules may support it, but should not own it.

---

## 4.3 context module

### Responsibilities
- assemble context package for each turn
- generate conversation summary
- generate decisions snapshot
- generate open loops snapshot
- provide right-side context panel data

### Main entities
- `ContextSnapshot`

### Main services
- `ContextAssemblyService`
- `ConversationSummaryService`
- `DecisionExtractionService`
- `OpenLoopService`
- `ContextPanelService`

### Main APIs
- get conversation context
- refresh context snapshot
- list context snapshots

### Notes
This module should own the four-layer context assembly strategy:

1. recent messages
2. conversation summary
3. project context
4. retrieval supplement

---

## 4.4 memory module

### Responsibilities
- manage global / project / conversation memory
- create memory suggestions
- accept/reject suggestions
- expose memory management APIs
- provide memory retrieval for context assembly

### Main entities
- `MemoryItem`
- `MemorySuggestion`

### Main services
- `MemoryCommandService`
- `MemoryQueryService`
- `MemorySuggestionService`
- `MemoryRetrievalService`

### Main APIs
- list memory items
- create memory item
- update memory
- delete memory
- accept/reject suggestion

### Notes
This module should not silently store everything.  
It should support explicit, assisted, and system-managed memory flows.

---

## 4.5 action module

### Responsibilities
- identify internal actions
- create action / run / run step
- orchestrate lightweight internal execution
- provide trace step data
- support retries and failure handling

### Main entities
- `Action`
- `Run`
- `RunStep`
- `Artifact`

### Main services
- `ActionCommandService`
- `ActionQueryService`
- `RunExecutionService`
- `RunStepService`
- `TracePanelService`

### Main APIs
- get action detail
- get run detail
- list run steps
- retry action
- cancel run

### Notes
Even in Phase 1, this module must exist.  
Without it, loom cannot feel like a workspace with visible execution.

---

## 4.6 capability module

### Responsibilities
- select model profile
- call model providers
- call MCP resources/prompts/tools
- run internal skills
- normalize outputs for conversation and action flows

### Main entities
- `ModelProfile`
- `Skill`
- `McpServer`

### Main services
- `ModelProfileService`
- `ModelRuntimeService`
- `SkillRuntimeService`
- `McpRuntimeService`
- `CapabilityRegistryService`

### Main APIs
- list capabilities overview
- list model profiles
- test model profile
- list skills
- test MCP server

### Notes
This module is where Phase 1 integrates external capability protocols without giving them ownership over product state.

---

## 4.7 file module

### Responsibilities
- store project files and images
- generate file summaries
- manage attachment references
- provide project file lists
- support later retrieval use

### Main entities
- `FileAsset`
- `MessageAssetRef`

### Main services
- `FileUploadService`
- `FileQueryService`
- `FileSummaryService`
- `AttachmentService`

### Main APIs
- upload file
- list project files
- get file metadata
- download file
- attach file to message

---

## 4.8 settings module

### Responsibilities
- manage model profile settings
- manage MCP server settings
- manage memory policy settings
- manage routing policy settings
- manage skill enablement and scope

### Main entities
- `SettingMemoryPolicy`
- `SettingRoutingPolicy`

### Main services
- `ModelProfileSettingsService`
- `McpSettingsService`
- `MemoryPolicyService`
- `RoutingPolicyService`
- `SkillSettingsService`

### Main APIs
- models settings
- MCP settings
- memory policy settings
- routing settings
- skill settings

---

## 4.9 stream module

### Responsibilities
- provide SSE streaming endpoints
- fan out message deltas
- fan out trace updates
- fan out context updates
- fan out run status updates

### Main services
- `SseSessionService`
- `ConversationEventPublisher`
- `RunEventPublisher`

### Main APIs
- subscribe to conversation stream

### Notes
This module should expose one stable event contract to the frontend.

---

## 4.10 common module

### Responsibilities
- shared exceptions
- shared IDs and enums
- result wrappers
- validation
- security helpers
- time abstractions
- JSON utilities

### Notes
This module should stay small and boring.

---

### 5. Cross-module Dependency Rules

To keep the modular monolith healthy, use these rules:

- `conversation` can depend on `context`, `memory`, `action`, `capability`, `stream`
- `context` can depend on `project`, `memory`, `file`
- `memory` should not depend on `conversation` business logic except by IDs and query interfaces
- `action` can depend on `capability`, `stream`
- `settings` should not depend on `conversation`
- `stream` depends only on published events, not business orchestration logic
- `common` can be used by all modules

### Recommended policy
Prefer **interfaces + application services** instead of direct repository hopping across modules.

Example:

- `conversation` should not query `memory_item` repository directly
- it should call `MemoryRetrievalService`

---

### 6. Layering Inside Each Module

Each module should use the same internal layered shape:

- `controller`
- `application`
- `domain`
- `infrastructure`
- `dto`

### Meaning

#### controller
Spring MVC REST controllers

#### application
Use-case orchestration services

#### domain
Core entities, enums, and domain rules

#### infrastructure
JPA repositories, adapter implementations, persistence mappings

#### dto
Request / response / command / query models

This keeps the code readable and stable as the module grows.

---

### 7. Recommended Package Layout

Recommended root:

`com.loom.backend`

Under that:

- `common`
- `project`
- `conversation`
- `context`
- `memory`
- `action`
- `capability`
- `file`
- `settings`
- `stream`

A detailed package map is provided in the separate package-structure document.

---

### 8. Main End-to-end Flow

## 8.1 User sends a message

Main path:

1. `ConversationController.submitMessage`
2. `MessageCommandService.createUserMessage`
3. `ConversationReplyService.handleReply`
4. `ContextAssemblyService.buildContextPackage`
5. `CapabilityRuntimeService.generateAssistantOutput`
6. optional `ActionCommandService.createActionIfNeeded`
7. stream deltas via `ConversationEventPublisher`
8. persist assistant / thinking summary / trace data
9. refresh summary and memory suggestions

This is the canonical Phase 1 flow.

---

## 8.2 Internal action execution

Main path:

1. user intent implies action
2. `ActionCommandService` creates `Action`
3. `RunExecutionService` creates `Run`
4. `RunStepService` appends step states
5. `SkillRuntimeService` or internal logic executes
6. `RunEventPublisher` emits trace updates
7. result stored as `Artifact` or assistant-facing output

---

### 9. Suggested Spring Boot Technical Choices

### 9.1 Web layer
- Spring Web MVC
- SSE using `SseEmitter` or response streaming abstraction

### 9.2 Persistence
- Spring Data JPA
- PostgreSQL
- Flyway for schema migrations

### 9.3 Caching / short-lived state
- Redis for:
  - stream sessions
  - temporary context caches
  - rate limiting
  - short-lived run state acceleration

### 9.4 Validation
- Jakarta Validation

### 9.5 Serialization
- Jackson

### 9.6 Observability
- Spring Boot Actuator
- structured logging with trace IDs

---

### 10. Event Model for Frontend Streaming

Phase 1 should standardize SSE events.

Recommended event types:

- `message.delta`
- `message.done`
- `thinking.summary.delta`
- `thinking.summary.done`
- `trace.step.created`
- `trace.step.updated`
- `trace.step.completed`
- `context.updated`
- `memory.suggested`
- `run.completed`
- `run.failed`

The `stream` module should own these event contracts.

---

### 11. DTO Strategy

Keep DTOs explicit and module-local.

Use categories like:

- request DTO
- response DTO
- command DTO
- query DTO
- stream event DTO

Avoid leaking JPA entities into controller responses.

Example in conversation module:

- `SubmitMessageRequest`
- `SubmitMessageResponse`
- `MessageView`
- `ConversationView`
- `ConversationContextView`

---

### 12. Transaction Strategy

### Recommended approach

Use transaction boundaries in application services.

Examples:

- project create/update → one transaction
- submit message → split:
  - persist user message in transaction
  - run generation outside long DB transaction
  - persist assistant chunks/final message in smaller transactions
- action/run/step updates → short explicit transactions

Do not keep DB transactions open during streaming or long model calls.

---

### 13. Error Handling Strategy

Use module-specific exceptions but map them through a common error layer.

Recommended common error response:

```json
{
  "success": false,
  "error": {
    "code": "MEMORY_NOT_FOUND",
    "message": "Memory item does not exist"
  }
}
```

Categories:

- validation errors
- not found
- policy violations
- runtime failures
- external capability failures

---

### 14. Security Boundaries in Phase 1

Even without OpenClaw, define clean boundaries now.

- frontend never calls capability providers directly
- frontend never calls MCP directly
- backend is the only authority for context assembly
- memory writes must go through policy checks
- internal action execution must honor routing and approval settings

This preserves product control and prepares for external executors later.

---

### 15. Future OpenClaw Insertion Point

Phase 1 should prepare a clean insertion point without implementing it.

The future Phase 2 integration should plug into:

- `capability` module for executor adapters
- `action` module for external run types
- `settings` module for routing additions
- `stream` module for callback-driven trace updates

That means the following interfaces should exist or be easy to add later:

- `ExecutorAdapter`
- `RunDispatcher`
- `ExternalCallbackHandler`

This allows OpenClaw to be added later without replacing core conversation infrastructure.

---

### 16. Recommended Development Order

### Step 1
Build `project`, `conversation`, `stream`, and `common`

### Step 2
Add `context` and wire right-side context panel data

### Step 3
Add `memory` and memory suggestions

### Step 4
Add `action` and trace flow

### Step 5
Add `settings` and `capability`

### Step 6
Add `file`

This order matches product value and reduces integration confusion.

---

### 17. Final Recommendation

The backend should treat loom as a **conversation product first**, not as a tool router first.

So in Phase 1:

- conversation is the center
- context is assembled internally
- memory is owned internally
- actions are internally orchestrated
- trace is internally published
- capabilities are adapters, not the core

This is the most stable and practical Spring Boot structure for building loom before OpenClaw is introduced later.
