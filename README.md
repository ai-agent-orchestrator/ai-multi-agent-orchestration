# AI Multi-Agent Orchestration Portfolio

This repository collects my AI backend and Java architecture learning projects.

My focus is on building backend systems that connect real-world workflows, AI-assisted development, Java concurrency, and practical user feedback loops.

## Featured Project

### Java 21 Virtual Thread Cafe Order System

Path:

[`portfolio/java21-virtual-thread-cafe-order-system`](portfolio/java21-virtual-thread-cafe-order-system)

This project started as a Java console CRUD assignment and was refactored into a Java 21 Virtual Thread edition.

It demonstrates how a small domain application can be extended into a backend architecture learning project:

- Java 21 Virtual Threads
- asynchronous background order processing
- JVM memory optimization concepts
- CRUD and layered architecture
- enum-based state modeling
- Stream API search and filtering
- real-world ontology feedback
- user-facing interaction refinement
- documentation synchronization across code, diagrams, and study notes

## Why This Project Matters

Modern AI services often spend a large amount of time waiting for network I/O:

- external LLM API calls
- vector database retrieval
- embedding generation
- tool execution
- multi-agent coordination
- database and message queue operations

Java 21 Virtual Threads are useful for this kind of workload because they allow many waiting tasks to be handled with much lower thread overhead than traditional platform threads.

The console project itself is intentionally small, so the speed difference is not visually dramatic. The architectural lesson is much larger: when the system grows into an AI multi-agent backend, lightweight concurrency becomes a practical advantage.

## Development Philosophy

This repository also documents an AI-assisted development process:

```text
1. Ontology design
2. AI-assisted generation
3. Real-world ontology feedback
4. Refinement
5. Synchronization and integration of fragmented artifacts
```

The key idea is that AI-generated code should not be accepted blindly.

It must be executed, observed, corrected through real user feedback, and synchronized across code, README files, diagrams, and study documents.

## Main Documents

- [Java 21 Virtual Thread Cafe Order System](portfolio/java21-virtual-thread-cafe-order-system)
- [English README](portfolio/java21-virtual-thread-cafe-order-system/README_EN.md)
- [AI Development Lessons](portfolio/java21-virtual-thread-cafe-order-system/AI_DEVELOPMENT_LESSONS_EN.md)
- [Normal vs Virtual Thread Design](portfolio/java21-virtual-thread-cafe-order-system/NORMAL_VS_VIRTUAL_THREAD_DESIGN.md)
- [Virtual Thread Refactoring Guide](portfolio/java21-virtual-thread-cafe-order-system/VIRTUAL_THREAD_REFACTORING_GUIDE.md)

## Current Direction

I am especially interested in backend engineering for AI agent systems, including:

- Java and Spring-based AI services
- multi-agent orchestration
- memory-efficient concurrent architecture
- domain ontology modeling
- real-world feedback loops
- legal, medical, administrative, and other knowledge-heavy domains
