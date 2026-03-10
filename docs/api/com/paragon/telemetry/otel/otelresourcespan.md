# :material-database: OtelResourceSpan

> This docs was updated at: 2026-03-10













`com.paragon.telemetry.otel.OtelResourceSpan` &nbsp;·&nbsp; **Record**

---

Represents a collection of spans from a single resource. Groups scope spans by their originating
resource.

## Methods

### `forAgentle`

```java
public static @NonNull OtelResourceSpan forAgentle(@NonNull List<OtelSpan> spans)
```

Creates a resource span collection for Agentle with the given spans.
