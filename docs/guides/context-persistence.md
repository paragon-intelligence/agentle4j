# Context Persistence

> This docs was updated at: 2026-03-20

`AgenticContext` is the short-term memory of an agent run — it holds conversation history,
custom state, turn count, and trace metadata. Because it is fully Jackson-serializable, you can
persist it to a database, message queue, or file system and resume the run later.

## Requirements

Use `ResponsesApiObjectMapper.create()` as your `ObjectMapper`. It registers the
`ResponsesApiModule` that handles all polymorphic `ResponseInputItem` subtypes
(`Message`, `FunctionToolCall`, `FunctionToolCallOutput`, etc.).

```java
import com.paragon.agents.AgenticContext;
import com.paragon.responses.json.ResponsesApiObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper;

ObjectMapper mapper = ResponsesApiObjectMapper.create();
```

---

## Basic round-trip

```java
AgenticContext ctx = AgenticContext.create();
ctx.addInput(Message.user("Hello"));
ctx.addInput(new FunctionToolCall("{'city':'Tokyo'}", "call_abc", "get_weather", null, null));
ctx.addInput(FunctionToolCallOutput.success("call_abc", "Sunny, 22°C"));
ctx.setState("userId", "u-123");

// Serialize
String json = mapper.writeValueAsString(ctx);

// Deserialize
AgenticContext restored = mapper.readValue(json, AgenticContext.class);

assert restored.historySize() == 3;
assert restored.getState("userId").orElseThrow().equals("u-123");
assert restored.getTurnCount() == 0;
```

---

## JDBC persistence

Store the serialized JSON in a `TEXT` or `CLOB` column and reload it when the conversation
resumes.

```java
// Save
String json = mapper.writeValueAsString(ctx);
try (PreparedStatement ps = conn.prepareStatement(
        "INSERT INTO agent_sessions (session_id, context_json) VALUES (?, ?) " +
        "ON CONFLICT (session_id) DO UPDATE SET context_json = EXCLUDED.context_json")) {
    ps.setString(1, sessionId);
    ps.setString(2, json);
    ps.executeUpdate();
}

// Load
try (PreparedStatement ps = conn.prepareStatement(
        "SELECT context_json FROM agent_sessions WHERE session_id = ?")) {
    ps.setString(1, sessionId);
    try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
            AgenticContext ctx = mapper.readValue(rs.getString(1), AgenticContext.class);
        }
    }
}
```

---

## Human-in-the-loop: pause and resume

Persist the context when the agent needs human approval, then restore it to continue.

```java
// --- Pause ---
AgentResult result = agent.interactBlocking("Please review these changes.", ctx);
if (result.isPaused()) {
    String json = mapper.writeValueAsString(ctx);
    pendingApprovals.put(approvalId, json);   // store in DB / cache
    notifyHuman(approvalId, result.pauseReason());
}

// --- Resume (after human responds) ---
String json = pendingApprovals.get(approvalId);
AgenticContext ctx = mapper.readValue(json, AgenticContext.class);
ctx.addInput(Message.user("Approved. Please proceed."));
AgentResult continued = agent.interactBlocking(ctx);
```

---

## Working with the state map after deserialization

The `state` map is serialized as a plain JSON object. On deserialization, Jackson maps values to
standard Java types:

| JSON type | Java type after deserialization |
|-----------|--------------------------------|
| string    | `String`                       |
| number    | `Integer` / `Long` / `Double`  |
| boolean   | `Boolean`                      |
| object    | `LinkedHashMap<String, Object>`|
| array     | `ArrayList<Object>`            |

If you stored a rich object (e.g., a custom POJO), convert it back explicitly:

```java
ctx.setState("order", new Order("ORD-99", 42.00));
// ... serialize / deserialize ...
Order order = mapper.convertValue(
    ctx.getState("order").orElseThrow(),
    Order.class);
```

---

## Known limitation: Image and File tool outputs

`FunctionToolCallOutput` serializes its `output` field as a plain string via `toString()`.

- **`Text` outputs** — `toString()` returns the text value. Round-trip is **lossless**.
- **`Image` / `File` outputs** — `toString()` returns the record's default representation
  (e.g., `Image[url=..., detail=...]`), which **cannot be reversed**. After deserialization,
  the output will appear as a `Text` wrapping that string. The structure is safe but the
  semantic meaning is lost.

If your agent uses image or file tool outputs and you need to persist the context, store the
raw binary data separately and reconstruct the `FunctionToolCallOutput` manually before
restoring the context.
