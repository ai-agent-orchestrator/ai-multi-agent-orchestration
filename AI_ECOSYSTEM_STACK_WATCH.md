# AI Ecosystem Stack Watch

My current AI workflow is already multi-agent in practice.

## Development and Control Stack

```text
Gemini (Google AI)
-> domain reasoning
-> legal / medical issue exploration
-> broad conceptual comparison

Codex
-> backend implementation
-> repository work
-> Java / Spring / API structure
-> documentation synchronization

Claude
-> backend reasoning
-> architecture review
-> long-form explanation
-> refinement

NVIDIA NeMo Guardrails
-> AI security
-> policy boundary
-> guardrail logic
-> safe agent behavior

LangChain / LangGraph / LangSmith
-> RAG workflow
-> multi-step agent orchestration
-> trace / evaluation
-> retrieval-grounded legal and medical AI
```

## Planned Inference Workers

```text
Meta Llama
-> planned inference workers
-> extraction and tagging
-> summarization and report drafts
-> self-hosted or approved-provider deployment, subject to validation
```

## Purpose

```text
cost-efficient AI engineering
AI-assisted code review
enterprise backend automation
guardrail-ready AI application architecture
```

## Ontology Positioning

This connects directly to my ontology positioning:

```text
Medical background
+ Legal dispute structuring experience
+ Ontology-driven thinking
+ AI-native PM / backend orchestration
= Legal/Medical AI Technical PM
```

The practical goal is to build a personal AI collaboration system where each AI model has a clear role, the outputs are checked against real domain structure, and the final result becomes code, documentation, diagrams, and reusable architectural knowledge.

Related ontology direction:

- [Ontology Feedback](https://github.com/ai-agent-orchestrator/ontology-feedback)

How I will study it:

```text
README
architecture notes
review rule design
security review examples
LLM provider configuration
agent workflow
```

What I will not prioritize first:

```text
model training internals
full framework internals
production deployment
```

Learning target:

```text
Understand how an inference-worker runtime structures context,
model calls, output validation, usage accounting, and security boundaries.
```

## Comparison Lens

I will evaluate these tools and model deployment options through the following questions:

```text
1. What problem does this tool solve?
2. Where does it sit in the AI application stack?
3. Does it help implementation, review, guardrail, retrieval, or orchestration?
4. Can it reduce cost or improve developer productivity?
5. Can it connect to a Spring / FastAPI backend architecture?
6. Can its pattern be reused in Legal/Medical AI systems?
```

## Portfolio Positioning

This stack watch supports the long-term direction:

```text
Codex / Claude
-> agentic development and architecture review

NVIDIA Guardrails
-> AI safety and behavior control

LangChain
-> RAG and workflow orchestration

Meta Llama
-> planned inference workers for extraction, summarization, and report drafts
```

One-line positioning:

```text
I evaluate development agents, guardrails, RAG orchestration,
and Llama-based inference workers as separate layers
of a controlled and observable AI backend.
```

## Model Selection and Validation

The planned worker model is [Meta Llama](https://github.com/meta-llama/llama-models). This is an architecture direction, not a completed deployment or a claim that model origin guarantees security. Before use, select the model/version and hosting option, review license and artifact provenance, restrict data access and network egress, and test prompt-injection handling, output privacy, and cost limits.
