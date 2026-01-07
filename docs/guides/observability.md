# Observability Guide

Agentle4j provides built-in observability through OpenTelemetry, enabling you to trace, measure, and monitor your AI interactions.

## Overview

```mermaid
flowchart LR
    A[Responder] --> B[TelemetryProcessor]
    B --> C[OpenTelemetry]
    C --> D[Jaeger/Zipkin]
    C --> E[Prometheus]
    C --> F[Grafana]
    B --> G[Langfuse]
```

## Quick Setup

### Langfuse Integration

```java
import com.paragon.telemetry.langfuse.LangfuseProcessor;

LangfuseProcessor langfuse = LangfuseProcessor.builder()
    .publicKey("pk-xxx")
    .secretKey("sk-xxx")
    .build();

Responder responder = Responder.builder()
    .openRouter()
    .apiKey(apiKey)
    .addTelemetryProcessor(langfuse)
    .build();
```

## What's Tracked

### Spans

Each API call creates a span with:

| Attribute | Description |
|-----------|-------------|
| `llm.model` | Model used |
| `llm.provider` | API provider |
| `llm.input_tokens` | Input token count |
| `llm.output_tokens` | Output token count |
| `llm.total_tokens` | Total tokens |
| `llm.cost` | Cost (OpenRouter only) |
| `llm.latency_ms` | Request latency |

### Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `llm.requests` | Counter | Total requests |
| `llm.tokens.input` | Counter | Total input tokens |
| `llm.tokens.output` | Counter | Total output tokens |
| `llm.latency` | Histogram | Request latency distribution |
| `llm.errors` | Counter | Error count |

## Provider-Specific Features

### OpenRouter Cost Tracking

When using OpenRouter, Agentle4j automatically tracks costs:

```java
Responder responder = Responder.builder()
    .openRouter()
    .apiKey(apiKey)
    .addTelemetryProcessor(langfuse)  // Optional: add for observability
    .build();

// Response includes cost information
Response response = responder.respond(payload);
// Cost is automatically added to telemetry
```

## Custom Telemetry Processor

Implement your own processor:

```java
public class CustomTelemetryProcessor implements TelemetryProcessor {
    
    @Override
    public void onRequestStart(RequestContext ctx) {
        // Log request start
    }
    
    @Override
    public void onRequestComplete(RequestContext ctx, Response response) {
        // Log completion with metrics
    }
    
    @Override
    public void onRequestError(RequestContext ctx, Throwable error) {
        // Log errors
    }
}
```

## Langfuse Integration

For LLM-specific analytics:

```java
LangfuseProcessor langfuse = LangfuseProcessor.builder()
    .publicKey("pk-xxx")
    .secretKey("sk-xxx")
    .build();

Responder responder = Responder.builder()
    .openRouter()
    .apiKey(apiKey)
    .addTelemetryProcessor(langfuse)
    .build();
```

Langfuse provides:

- Conversation tracing
- Cost analytics
- Quality scoring
- A/B testing support

## Telemetry Context

Add custom metadata to traces:

```java
TelemetryContext telemetryContext = TelemetryContext.builder()
    .userId("user-123")
    .traceName("customer-support-chat")
    .addTag("production")
    .addTag("billing")
    .addMetadata("customer_tier", "premium")
    .build();

var payload = CreateResponsePayload.builder()
    .model("openai/gpt-4o")
    .addUserMessage("Help with my invoice")
    .build();

// Use with TelemetryContext
responder.respond(payload, telemetryContext);
```

## Trace Correlation Across Multi-Agent Runs

Agentle4j **automatically propagates trace context** across agent handoffs and parallel executions. A single user request creates a correlated trace tree:

```text
User Request: "Help with my billing issue"
│
└─ Trace: 8a7b6c5d...
   ├── Span: triage-agent.turn-1
   │   └── (decides to handoff to billing)
   │
   ├── Span: billing-agent.turn-1
   │   └── (calls lookup_invoice tool)
   │
   └── Span: billing-agent.turn-2
       └── (responds with answer)
```

### How It Works

Trace context is propagated automatically through:

| Component | Behavior |
|-----------|----------|
| **AgentContext** | Stores `parentTraceId`, `parentSpanId`, `requestId` |
| **Agent.interact()** | Auto-initializes trace if not set |
| **Handoffs** | Forks context with new parent span for child agent |
| **ParallelAgents** | All parallel agents share the same parent trace |
| **Responder** | Uses parent trace context from TelemetryContext |

### Manual Trace Context

For advanced use cases, you can manually set trace context:

```java
// Create context with explicit trace
AgentContext ctx = AgentContext.create()
    .withTraceContext("8a7b6c5d4e3f2a1b0c9d8e7f6a5b4c3d", "1a2b3c4d5e6f7a8b")
    .withRequestId("user-session-12345");  // Optional high-level correlation

ctx.addInput(Message.user("Help me"));
AgentResult result = agent.interact(ctx);
```

### Viewing Traces

In your observability platform (Jaeger, Langfuse, Grafana Tempo):

1. Search by `traceId` to see the entire request flow
2. Filter by `request.id` for ultra-high-level correlation
3. View parent-child relationships in the trace waterfall


Example PromQL queries for a Grafana dashboard:

```promql
# Request rate
rate(llm_requests_total[5m])

# Average latency
histogram_quantile(0.95, rate(llm_latency_bucket[5m]))

# Token usage
sum(rate(llm_tokens_input_total[1h]))

# Error rate
rate(llm_errors_total[5m]) / rate(llm_requests_total[5m])
```

## Best Practices

!!! tip "Use Tracing in Production"
    Always enable telemetry in production to monitor costs and performance.

!!! tip "Set Sampling Rate"
    For high-traffic applications, configure OpenTelemetry sampling to reduce overhead.

!!! warning "Sensitive Data"
    Be careful not to log prompts or responses that contain sensitive user data.

```java
// Configure in OTel SDK
SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
    .setSampler(Sampler.traceIdRatioBased(0.1))  // 10% sampling
    .build();
```

## Debugging

Enable debug logging for development:

```java
// Add to your logging config (logback.xml)
<logger name="com.paragon" level="DEBUG"/>
```

## Next Steps

- [Responder Guide](responder.md) - Configure the Responder
- [Agents Guide](agents.md) - Monitor agent execution
