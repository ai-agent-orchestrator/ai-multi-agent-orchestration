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

## Phase 3-2 Structure Summary

This section is a personal study note. It is written to understand the structure later, not to make a polished portfolio page.

```text
WebSocketConfig
-> exists
-> registers /ws/admin/menu-draft
-> connects a WebSocket URL to MenuDraftWebSocketHandler

TextWebSocketHandler
-> exists
-> MenuDraftWebSocketHandler
-> handles connection opened / message received / connection closed

Message DTO
-> exists
-> MenuDraftMessage
-> message types: auth / draft / cancel / revise / image

Session State
-> exists
-> ADMIN
-> HISTORY
-> RUNNING
-> CANCELLED

JWT authentication change
-> exists
-> browser WebSocket cannot easily send Authorization header
-> token is sent as the first auth message after WebSocket connection
-> ROLE_ADMIN is checked inside the handler
-> unauthorized connection is closed with 1008 POLICY_VIOLATION

Thread handling
-> exists
-> TaskExecutor runs AI work in a separate worker thread
-> this is required so the WebSocket handler can still receive cancel while AI generation is running

Concurrent send protection
-> exists
-> ConcurrentWebSocketSessionDecorator is used
-> protects one WebSocket session from unsafe concurrent sendMessage calls
```

## Why This Structure Matters

At first glance, WebSocket looks like a simple real-time communication feature. Phase 3-2 shows that once AI generation is added, the structure becomes much more serious.

```text
Normal HTTP request
-> request
-> response
-> connection ends

SSE
-> browser sends one request
-> server streams many events
-> mostly one-way from server to browser

WebSocket
-> connection stays open
-> browser and server can both send messages anytime
-> message type decides what the server should do
-> server can remember state per connection
```

For AI task control, this is important because the user may want to interrupt or revise the AI task while it is running.

```text
draft is running
-> user sends cancel
-> handler receives cancel
-> cancel flag is set
-> worker thread checks cancel flag
-> AI stream stops
-> server sends cancelled response
```

This pattern later becomes useful for multi-agent orchestration.

```text
agent task starts
-> progress is streamed
-> user can cancel
-> user can revise
-> session keeps task state
-> later LangChain / LangGraph can manage routing and workflow state
```

## Main Components

### 1. WebSocketConfig

Purpose:

```text
Register WebSocket endpoint.
Connect a URL to a handler.
Allow React origin.
```

In Phase 3-2:

```text
/ws/admin/menu-draft
-> MenuDraftWebSocketHandler
```

Concept:

```text
HTTP Controller
-> URL + method decides the function

WebSocket
-> one URL opens a long connection
-> message.type decides the function
```

Study point:

```text
WebSocketConfig is not business logic.
It is the entry-point mapping layer.
```

### 2. TextWebSocketHandler

Purpose:

```text
Handle WebSocket lifecycle.
```

Main lifecycle methods:

```text
afterConnectionEstablished
-> connection opened
-> initialize session state

handleTextMessage
-> message received
-> parse JSON
-> branch by message type

afterConnectionClosed
-> connection closed
-> cleanup session
-> cancel running work if needed
```

In Phase 3-2:

```text
MenuDraftWebSocketHandler
-> receives auth / draft / revise / cancel / image
-> stores ADMIN / HISTORY / RUNNING / CANCELLED
-> runs AI work through TaskExecutor
```

Study point:

```text
The handler is similar to a Controller,
but it manages a long-lived session instead of one request-response cycle.
```

### 3. Message DTO

Purpose:

```text
Represent WebSocket messages as JSON.
```

In Phase 3-2:

```text
MenuDraftMessage
```

Important field:

```text
type
```

Message types:

```text
auth
-> send JWT token

draft
-> start first AI draft generation

cancel
-> stop current AI generation

revise
-> revise previous draft using session history

image
-> optional image generation
```

Study point:

```text
WebSocket does not have GET / POST / PUT routes for each action.
One connection receives many message types.
So type becomes the internal router.
```

### 4. Session State

Purpose:

```text
Remember values for each WebSocket connection.
```

In Phase 3-2:

```text
ADMIN
-> authenticated admin user

HISTORY
-> conversation history for this connection

RUNNING
-> whether an AI task is currently running

CANCELLED
-> whether the user requested cancel
```

Why it matters:

```text
SSE / HTTP style
-> client often keeps conversation state

WebSocket style
-> server can keep state per connection
```

Study point:

```text
Session state is what makes revise possible.
The user can send only "make it shorter" because the server remembers the previous draft.
```

### 5. JWT Authentication Change

Problem:

```text
Browser WebSocket cannot easily attach Authorization: Bearer ... header.
Putting token in URL query string is risky.
```

Phase 3-2 solution:

```text
1. Open WebSocket connection
2. Send first message:
   { "type": "auth", "token": "JWT_ACCESS_TOKEN" }
3. Server validates token
4. Server checks ROLE_ADMIN
5. Server sends auth_ok
6. Only then draft / revise / cancel are allowed
```

If authentication fails:

```text
close with 1008 POLICY_VIOLATION
```

Study point:

```text
Spring Security handles normal HTTP JWT filters.
WebSocket often needs a separate authentication flow inside the handler.
```

### 6. Thread Processing

Problem:

```text
AI generation takes time.
If handleTextMessage runs AI generation directly,
the same connection cannot receive cancel until the generation finishes.
```

Phase 3-2 solution:

```text
handleTextMessage
-> receives draft
-> starts AI work through TaskExecutor
-> returns immediately

worker thread
-> reads OpenAI stream
-> sends delta messages
-> checks cancel flag
```

Why this matters:

```text
cancel message must be received while the AI task is still running.
Therefore long AI work must not block the message handling thread.
```

Study point:

```text
This is the moment WebSocket becomes more than "real-time output".
It becomes "real-time task control".
```

### 7. Concurrent Send Protection

Problem:

```text
One WebSocketSession should not receive sendMessage from multiple threads at the same time.
```

Possible send sources:

```text
message handling thread
-> error / cancelled response

worker thread
-> delta / done response
```

Phase 3-2 solution:

```text
ConcurrentWebSocketSessionDecorator
```

Purpose:

```text
Make sends safer by serializing WebSocket send operations.
Set send time limit and buffer size limit.
```

Study point:

```text
Once worker threads are introduced,
WebSocket sending must be protected.
Otherwise the structure may work in small tests but fail under real concurrent behavior.
```

## Postman WebSocket Study Tasks

The goal is not to spend API money. The goal is to understand connection, auth, state, cancel, and message flow.

### 1. Connect To WebSocket

Postman:

```text
New
-> WebSocket
-> ws://localhost:8080/ws/admin/menu-draft
-> Connect
```

Expected concept:

```text
HTTP Upgrade
-> 101 Switching Protocols
-> connection stays open
```

What to learn:

```text
WebSocket is not normal REST.
It is a long-lived connection.
```

### 2. Send Message Before Auth

Send:

```json
{
  "type": "draft",
  "menuName": "Bulgogi Burger"
}
```

Expected:

```text
Server rejects it.
Connection may close with policy violation.
```

What to learn:

```text
The handler requires auth first.
WebSocket endpoint itself may be open,
but real permission check happens inside the handler.
```

### 3. Send Auth Message

Send:

```json
{
  "type": "auth",
  "token": "JWT_ACCESS_TOKEN"
}
```

Expected:

```json
{
  "type": "auth_ok"
}
```

What to learn:

```text
WebSocket authentication is message-based here.
The server stores authenticated user in session state.
```

### 4. Send Invalid Token

Send:

```json
{
  "type": "auth",
  "token": "wrong-token"
}
```

Expected:

```text
error message
-> close 1008 POLICY_VIOLATION
```

What to learn:

```text
Authentication failure closes the WebSocket connection.
This is different from normal REST 401 response.
```

### 5. Send Draft Request

Send after auth:

```json
{
  "type": "draft",
  "menuName": "Bulgogi Burger",
  "categoryName": "Burger",
  "menuPrice": 8500,
  "menuIngredients": "beef patty, lettuce, onion, bulgogi sauce"
}
```

Expected:

```text
delta
delta
delta
done
```

What to learn:

```text
AI output arrives in chunks.
Each chunk is sent as a WebSocket message.
```

### 6. Send Cancel During Draft

While draft is running, send:

```json
{
  "type": "cancel"
}
```

Expected:

```json
{
  "type": "cancelled"
}
```

What to learn:

```text
Cancel is not magic.
It sets a cancel flag.
The worker thread must check the flag and stop.
```

### 7. Send Revise After Draft

After draft is completed, send:

```json
{
  "type": "revise",
  "instruction": "Make it shorter and more premium."
}
```

Expected:

```text
delta
delta
done
```

What to learn:

```text
The server remembers the previous draft in HISTORY.
The client does not need to resend the whole conversation.
```

### 8. Send Revise Before Draft

Send:

```json
{
  "type": "revise",
  "instruction": "Make it shorter."
}
```

Expected:

```text
error
-> "먼저 초안을 만들어주세요."
```

What to learn:

```text
Session state controls valid message order.
```

### 9. Send Unknown Type

Send:

```json
{
  "type": "unknown"
}
```

Expected:

```text
error
-> unknown type
```

What to learn:

```text
Message type is an internal router.
Unknown routes must be rejected.
```

### 10. Send Two Drafts Quickly

Send draft twice before the first one finishes.

Expected:

```text
first draft runs
second request gets error:
already running
```

What to learn:

```text
RUNNING flag prevents duplicate concurrent AI work in one session.
This is also a cost-control and stability concept.
```

## Postman Study Checklist

```text
Can I explain why WebSocket starts as HTTP Upgrade?
Can I explain why auth is sent as the first message?
Can I explain why message.type is needed?
Can I explain what ADMIN / HISTORY / RUNNING / CANCELLED mean?
Can I explain why AI work must run in another thread?
Can I explain how cancel works?
Can I explain why concurrent send protection is needed?
Can I explain when WebSocket is better than SSE?
Can I explain why this can become an agent-control layer?
```

## Personal Learning Conclusion

```text
SSE is enough when the server only streams an answer.
WebSocket becomes useful when the user must control a running AI task.
```

For the current legal intake security project, SSE is enough.

For future multi-agent orchestration, WebSocket can become the real-time control layer.

## Portfolio Meaning

This branch is not part of the 2-week legal intake security MVP.

It is a future study branch for turning WebSocket from a simple real-time feature into an AI agent task-control layer.
