# Blueprint YAML Examples

Reference YAML files demonstrating how to define agents using Agentle's blueprint system.
These can be loaded with `InteractableBlueprint.fromYaml(yamlString)`.

| File | Description |
|------|-------------|
| `simple-agent.yaml` | Minimal agent with inline instructions |
| `structured-output-agent.yaml` | Agent that returns typed structured output |
| `router-with-specialists.yaml` | Router that classifies and routes to specialist agents |
| `supervisor-team.yaml` | Supervisor managing worker agents |
| `agent-with-guardrails.yaml` | Agent with input/output guardrails and context management |
| `agent-with-file-instructions.yaml` | Instructions loaded from a file on disk |
| `agent-with-provider-instructions.yaml` | Instructions fetched from a prompt provider (e.g., Langfuse) |
| `parallel-agents.yaml` | Fan-out to multiple agents in parallel |
