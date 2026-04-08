## Loom Phase 1 Architecture Design

### 1. Document Positioning

This document defines the **Phase 1 architecture** for **loom**.

Phase 1 focuses on one thing:

**Polish loom's own AI conversation capabilities first, without integrating OpenClaw yet.**

That means loom should first become a **project-based AI conversation workspace** with:

- lightweight projects
- strong conversation experience
- context management
- layered memory
- internal action execution
- trace visibility
- configurable AI capabilities

OpenClaw is intentionally excluded from Phase 1 runtime flow, but the architecture should keep a clean insertion point for Phase 2.

---

### 2. Phase 1 Product Goal

Phase 1 is not trying to build:

- a full personal growth platform
- a multi-agent orchestration system
- a heavy task/project management product
- an external automation hub

Phase 1 is trying to build:

**A professional AI workspace where conversation is the core entry point, and where the system can remember, organize, summarize, and perform limited internal actions.**

In practical terms, Phase 1 must make these four things feel solid:

1. **Conversation**
2. **Context**
3. **Memory**
4. **Traceable execution**

---

### 3. Core Product Definition

Loom Phase 1 should be defined as:

**A project-based AI conversation workspace with layered memory, context assembly, internal capabilities, and visible trace execution.**

The user experience should feel like this:

- open a project
- continue a conversation naturally
- the system knows relevant project context
- the system can remember useful information
- the system can show what it is thinking in a user-facing summary
- the system can show what actions it is taking
- the system can complete lightweight internal tasks

---

### 4. Scope and Non-goals

### 4.1 In Scope

Phase 1 includes:

- Project
- Conversation
- Message
- Context panel
- Trace panel
- Markdown rendering
- Streaming output
- File and image upload
- Global / project / conversation memory
- Planning mode inside conversation
- Internal actions
- MCP-based capability access
- Settings and capability management

### 4.2 Out of Scope

Phase 1 excludes:

- OpenClaw integration
- Feishu integration
- async external executor routing
- long-running external workflows
- complex multi-agent delegation
- domain-specific modules like algorithm trainer as first-class product modules

These are Phase 2 or later.

---

### 5. Design Principles

### 5.1 Conversation-first

Conversation is the product center.

Users should not need to switch into separate planning or execution systems to get work done. Planning, review, and lightweight action should all be available inside conversation.

### 5.2 Project-native

A project is a **lightweight workspace**, not a heavy PM object.

A project should contain:

- conversations
- files
- instructions
- project memory
- default capability bindings

It should not contain heavy PM concepts like gantt charts or workflow boards.

### 5.3 Layered memory

Memory should not be one giant pool.

Loom should explicitly separate:

- global memory
- project memory
- conversation working memory

### 5.4 Context is assembled, not dumped

The system should not blindly send all history to the model.

Each turn should assemble a context package from structured layers.

### 5.5 Execution must be visible

The system should not feel like a black box.

Every meaningful internal action should be reflected in a user-facing trace:

- reasoning summary
- execution steps
- status progression
- results or failures

### 5.6 External executor ready, but not external executor dependent

Phase 1 should keep a clean executor abstraction so OpenClaw can be added later, but nothing critical in Phase 1 should rely on it.

---

### 6. Core Product Objects

Phase 1 should establish these product objects as first-class entities.

### 6.1 Project

A lightweight workspace.

Responsibilities:

- group related conversations
- hold project instructions
- hold project files
- hold project memory
- define default model / skills / policies

### 6.2 Conversation

The main collaborative thread between user and system.

Responsibilities:

- carry message history
- maintain mode
- maintain summary
- support streaming output
- serve as the anchor for trace and context

### 6.3 Message

A renderable unit in conversation.

Message types should include:

- user
- assistant
- thinking_summary
- action_card
- run_progress
- external_feedback
- context_update
- system

### 6.4 Context Snapshot

A structured representation of current conversation state.

Types may include:

- conversation_summary
- decisions
- open_loops
- planning_state
- active_context

### 6.5 Memory Item

A reusable piece of remembered information.

Memory scopes:

- global
- project
- conversation

### 6.6 Action / Run / Run Step

The execution chain that powers the Trace panel.

- **Action** = an intent to do something
- **Run** = one execution instance
- **Run Step** = visible step-by-step progress

### 6.7 File Asset

A project-owned file or image that can be referenced inside one or more messages.

---

### 7. UI Architecture

Phase 1 should use a stable three-column layout.

### 7.1 Left Sidebar

Contains navigation and project-level switching.

Main sections:

- New Chat
- Chats
- Capabilities
- Files
- Memory
- Settings

Also includes recent and pinned conversations.

### 7.2 Main Conversation Workspace

This is the core area.

Contents:

- conversation header
- mode switcher
- message list
- markdown rendering
- streaming output
- attachment cards
- composer

### 7.3 Right Panel

Contains two tabs:

- Trace
- Context

#### Trace tab
Displays:

- reasoning summary
- execution step timeline
- run status
- errors or waiting states

#### Context tab
Displays:

- current goals
- constraints
- active items
- conversation summary
- referenced files
- referenced memories
- unresolved loops

---

### 8. Conversation Modes

Conversation mode should be built into the conversation model.

Supported modes in Phase 1:

- `chat`
- `plan`
- `action`
- `review`

### 8.1 Chat mode
Default free-form assistant interaction.

### 8.2 Plan mode
Used when the system should actively extract:

- goals
- constraints
- candidate strategies
- next-step tasks

The plan should emerge from conversation, not be a separate rigid object.

### 8.3 Action mode
Used when the user explicitly wants the system to do something.

### 8.4 Review mode
Used to reflect, summarize, and extract insights.

---

### 9. Memory Architecture

### 9.1 Global Memory

Stores user-level stable information across projects.

Examples:

- long-term preferences
- preferred output style
- recurring working style

### 9.2 Project Memory

Stores project-level long-lived information.

Examples:

- project design direction
- technical constraints
- recurring terminology
- default capability choices

### 9.3 Conversation Working Memory

Stores the short-lived operational state of a conversation.

Examples:

- current summary
- recent confirmed decisions
- active unresolved issues
- current execution state

### 9.4 Memory Write Paths

Three write paths should exist:

#### Explicit save
The user explicitly says to remember something.

#### Assisted save
The system suggests saving something as memory.

#### System save
The system automatically maintains summaries and working memory, but this should remain separate from long-term memory unless promoted.

### 9.5 Memory Control

The user should be able to:

- inspect memory
- edit memory
- delete memory
- ignore memory suggestions
- disable or narrow memory policies

---

### 10. Context Architecture

Phase 1 should use a four-layer context assembly model.

### Layer 1: Recent Messages

The most recent relevant raw turns.

### Layer 2: Conversation Summary

Structured compressed conversation state, including:

- summary
- decisions
- open loops

### Layer 3: Project Context

Includes:

- project instructions
- project memory
- related file summaries

### Layer 4: Retrieval

Additional relevant material dynamically loaded from:

- historical conversation snapshots
- memory items
- project files
- previous runs

### Context Assembly Flow

For every new user message:

1. load recent messages
2. load conversation summary
3. load current decisions / open loops
4. load project instructions
5. load project memory
6. load relevant file summaries
7. optionally retrieve additional context
8. assemble the final context package
9. send the package to the internal capability runtime

This is the core of Phase 1 intelligence.

---

### 11. File and Asset Architecture

Files should belong to projects, not only to individual messages.

### File responsibilities

Each file should support:

- upload
- storage
- metadata
- summary
- parse status
- project ownership
- message references

### Why project-level ownership matters

If files only belong to one message, project continuity becomes weak.

Project ownership allows:

- reusing files in future conversations
- showing files in a dedicated Files page
- using file summaries in context assembly

---

### 12. Internal Action Architecture

Even without OpenClaw, Phase 1 should support internal action execution.

### 12.1 Why internal actions are necessary

Without actions, loom is only a chat shell.

With internal actions, loom becomes a workspace that can:

- save memory
- refresh summaries
- generate plan drafts
- create task drafts
- inspect project files
- run internal skills

### 12.2 Action model

Each action should produce:

- an `action`
- a `run`
- one or more `run_step`

This allows the Trace panel to remain structurally correct from Phase 1 onward.

### 12.3 Internal action examples

Examples for Phase 1:

- save project memory
- generate planning outline
- refresh conversation summary
- produce review artifact
- fetch relevant project resources
- run prompt-based skill

---

### 13. Capability Architecture

Phase 1 should introduce a unified capability runtime inside loom.

### 13.1 Model Profiles

Loom should support model profiles with explicit capability metadata.

Each profile should define:

- provider
- base URL
- API key reference
- model ID
- supports streaming
- supports images
- supports tools
- supports long context
- supports reasoning summary
- timeout
- retry

### 13.2 MCP Access

Phase 1 may use MCP, but only as a structured capability layer.

Use MCP primarily for:

- resources
- prompts
- limited safe tools

#### Resources
Good for project context sources:
- docs
- schemas
- structured references

#### Prompts
Good for skill-like templates:
- planning
- reviewing
- summarizing

#### Tools
Use cautiously in Phase 1, mostly for safe read-oriented or bounded operations.

### 13.3 Internal Skills

Loom should also have internally registered skills, for example:

- planning
- summarize
- project-review
- retrieve-context

A skill should be a reusable structured capability with policy controls.

---

### 14. Trace Architecture

The Trace panel is a first-class UX feature in Phase 1.

### 14.1 Reasoning Summary

This is **not raw hidden chain-of-thought**.

It is a user-facing structured explanation such as:

- what the system is trying to do
- what information it considered
- whether it plans to call an internal action
- what kind of answer structure it will produce

### 14.2 Execution Steps

Each visible run step should include:

- step title
- status
- timestamps
- optional input/output references
- error message if failed

Statuses:

- pending
- running
- waiting
- success
- failed
- skipped

### 14.3 Why this matters

This improves trust and reduces the feeling that the system is a black box.

It also prepares the exact same UX surface for future OpenClaw integration.

---

### 15. Settings Architecture

Settings should be professional and layered, but not intimidating.

### Main sections

- Models
- Skills
- MCP
- Memory
- Routing

### 15.1 Models
Configure model profiles and defaults.

### 15.2 Skills
Enable or disable internal or MCP-backed skills.

### 15.3 MCP
Configure MCP servers, status, discovery, and policies.

### 15.4 Memory
Configure memory scopes and policies.

### 15.5 Routing
In Phase 1, routing mainly decides which internal runtime path is used.  
In Phase 2, this same section will expand to include OpenClaw.

### Configuration Layers

Settings should support three levels conceptually:

- global
- project
- conversation override

Even if Phase 1 only fully implements global + project, the model should allow conversation-scoped overrides later.

---

### 16. Backend Service Architecture

Phase 1 should use a modular monolith with clearly separated modules.

### 16.1 API Gateway / Web Backend
Responsibilities:

- REST endpoints
- project CRUD
- conversation CRUD
- settings API
- file upload API

### 16.2 Conversation Core
Responsibilities:

- message processing
- mode handling
- streaming assistant output
- message persistence
- thinking summary generation

### 16.3 Context Engine
Responsibilities:

- context assembly
- context snapshots
- summary refresh
- decisions / open loops extraction

### 16.4 Memory Engine
Responsibilities:

- memory lifecycle
- suggestion flow
- promotion logic
- summary persistence

### 16.5 Capability Runtime
Responsibilities:

- invoke model profiles
- invoke internal skills
- invoke MCP resources/prompts/tools
- normalize outputs

### 16.6 Action Orchestrator
Responsibilities:

- determine when a user request implies action
- create action / run / run_step
- manage statuses
- emit step updates

### 16.7 File Service
Responsibilities:

- file storage
- file metadata
- file summaries
- attachment references

### 16.8 Stream Service
Responsibilities:

- SSE or WebSocket push
- message deltas
- trace updates
- context updates
- run updates

---

### 17. Recommended Data Model

Core tables for Phase 1:

- `project`
- `conversation`
- `message`
- `context_snapshot`
- `memory_item`
- `memory_suggestion`
- `file_asset`
- `message_asset_ref`
- `model_profile`
- `mcp_server`
- `skill`
- `setting_memory_policy`
- `setting_routing_policy`
- `action`
- `run`
- `run_step`
- `artifact`

This model is already sufficient to support:

- the three-column UI
- project continuity
- layered memory
- trace rendering
- future executor expansion

---

### 18. Request Processing Flow

The main request pipeline in Phase 1 should be:

1. user submits a message
2. message is stored
3. conversation mode is determined
4. context package is assembled
5. internal capability runtime is selected
6. optional internal action is created
7. reasoning summary is generated
8. assistant response is streamed
9. trace steps are emitted if applicable
10. summary / context snapshots are refreshed
11. memory suggestions may be created

This should be the one canonical flow.

---

### 19. Phase 1 Development Order

### Sprint 1
- project
- conversation
- message
- three-column UI
- markdown rendering
- streaming response

### Sprint 2
- context panel
- conversation summary
- decision summary
- open loops
- project instructions

### Sprint 3
- memory engine
- global/project/conversation memory
- memory suggestions
- memory page

### Sprint 4
- action/run/run_step
- trace panel
- internal actions
- settings basic version

### Sprint 5
- MCP resources/prompts
- capabilities page
- safer tool access
- routing refinement

---

### 20. Phase 2 Readiness

Although Phase 1 does not integrate OpenClaw, it should prepare for it cleanly.

Keep these extension points:

- executor abstraction
- run external reference field
- callback event model
- waiting callback status
- routing policy slots for external executors

This way, Phase 2 can add OpenClaw without replacing:

- conversation core
- context engine
- memory engine
- trace UI
- run model

---

### 21. Final Decision

Phase 1 should deliberately prioritize **loom's own product intelligence** over external runtime integration.

That means:

- loom owns project and conversation truth
- loom owns context assembly
- loom owns memory policy and memory UI
- loom owns trace rendering and run state
- loom only uses external capability protocols as inputs, not as the product core

This is the most stable path to building a professional AI workspace before introducing OpenClaw later.
