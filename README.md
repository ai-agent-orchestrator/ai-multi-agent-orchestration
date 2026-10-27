# AI Multi-Agent Orchestration

[![Multi-agent architecture: Google, Claude, Codex, NVIDIA and Meta/Llama](assets/diagrams/orchestration-architecture-dark.png)](assets/diagrams/orchestration-architecture-dark.svg)

[![Asynchronous execution flow with NVIDIA Guardrails and NemoClaw](assets/diagrams/orchestration-runtime-dark.png)](assets/diagrams/orchestration-runtime-dark.svg)

*Proposed orchestration architecture and runtime flow.*

## Who am I

```text
Medical background
+ Legal dispute structuring experience
+ Ontology-driven thinking
+ AI-native PM / backend orchestration
= Legal/Medical AI Technical PM
```

## Market Focus

I am preparing for this market by focusing on the intersection of:

```diff
Legal / Medical domain knowledge
+ ontology-driven problem structuring
+ backend AI server architecture
+ multi-agent orchestration
+ safe and observable AI workflows
```

At the current stage, I am especially focused on the Legal/Medical part because these domains have complex documents, high-risk reasoning, evidence structure, procedural workflows, and strong demand for safer AI systems.

My backend study is the execution layer for that direction: Java 21, Spring Boot REST APIs, JSON contracts, Virtual Threads, monitoring, observability, and later FastAPI/RAG/guardrail integration.

## Implementation Stack

| Stage | Layer | Stack |
| --- | --- | --- |
| Current backend foundation | API and persistence | Java 21 · Spring Boot · REST · JSON contracts · JPA / SQL |
| Current backend foundation | Concurrency | Virtual Threads · asynchronous HTTP calls · SSE |
| Current backend foundation | Client and operations | React · Vite · Actuator · Prometheus |
| Current backend foundation | Application controls | Authentication · privacy · risk/audit history · cost controls |
| Planned extension | AI gateway | Python · FastAPI · asynchronous task orchestration |
| Planned extension | Retrieval | LangChain · RAG |
| Planned extension | Inference workers | Meta · Llama |
| Planned deployment | Content guardrails | NVIDIA NeMo Guardrails |
| Planned extension | Agent execution security | NVIDIA NemoClaw / OpenShell for supported agent runtimes |

The planned inference-worker model is [Meta Llama](https://github.com/meta-llama/llama-models). The exact model version and hosting option remain to be selected and validated. Input/output guardrails, bounded retries, privacy checks, and usage accounting remain part of the proposed runtime regardless of model choice.
