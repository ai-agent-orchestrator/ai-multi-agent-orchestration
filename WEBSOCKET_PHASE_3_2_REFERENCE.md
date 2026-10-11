# WebSocket Phase 3-2 Reference

This branch keeps the Phase 3-2 WebSocket example as a future reference for personal multi-agent orchestration study.

The current legal intake security project will focus on JWT, SSE, AI security, cost control, and Postman experiments. WebSocket will be studied after the project as a separate personal extension.

## Why This Belongs Here

Phase 3-2 is not just a WebSocket demo. It contains a small real-time AI task-control pattern.

```text
auth
-> authenticate WebSocket session with JWT

draft
-> start AI generation

cancel
-> interrupt running AI work

revise
-> continue from session state and modify the result

session state
-> keep authenticated user, conversation history, running state, and cancel flag
```

This can later become the communication layer for AI agent orchestration.

```text
WebSocket
-> real-time agent control

LangChain / LangGraph
-> agent routing, tool calling, workflow state

Multi-agent orchestration
-> domain agent / security agent / retrieval agent / review agent
```

## Reference Location

```text
references/phase-3-2-websocket/
```

Main files:

```text
README.md
HOWTO-PHASE-3-2.md
REVIEW.md
spring-server/src/main/java/.../ai/websocket/WebSocketConfig.java
spring-server/src/main/java/.../ai/websocket/MenuDraftWebSocketHandler.java
spring-server/src/main/java/.../ai/dto/MenuDraftMessage.java
react-client/src/api/menuDraftSocket.js
react-client/src/components/MenuDraftAssistant.jsx
```

## Future Study Direction

```text
1. Understand WebSocket auth flow
2. Test auth / draft / cancel / revise with Postman WebSocket
3. Refactor the menu draft example into a generic AI task session
4. Add agent routing concept
5. Connect LangChain or LangGraph later
6. Document the result as an AI multi-agent orchestration portfolio case
```

## Portfolio Meaning

This branch is not part of the 2-week legal intake security MVP.

It is a future study branch for turning WebSocket from a simple real-time feature into an AI agent task-control layer.
