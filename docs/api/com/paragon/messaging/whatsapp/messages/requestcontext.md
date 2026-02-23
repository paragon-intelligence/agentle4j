# :material-database: RequestContext

> This docs was updated at: 2026-02-23

`com.paragon.messaging.whatsapp.messages.RequestContext` &nbsp;·&nbsp; **Record**

---

Contexto de requisição usando ScopedValues (Java 25).

**Por que ScopedValues em vez de ThreadLocal?**

  
- ThreadLocal com milhões de virtual threads = explosão de memória
- ScopedValues são imutáveis e automaticamente limpos
- Integração nativa com Structured Concurrency
- Herdados automaticamente por subtasks

**Uso:**

```java
// Definir contexto para a requisição
var context = new RequestContext("user-123", "api-key-456");
ScopedValue.where(RequestContext.CURRENT, context)
    .run(() -> {
        // Tudo dentro deste scope tem acesso ao contexto
        processRequest();
        // Até tarefas paralelas herdam o contexto!
        try (var scope = StructuredTaskScope.open(...)) {
            scope.fork(() -> {
                var ctx = RequestContext.get();
                sendMessage(ctx.userId());
            });
        }
    });
```

*Since: 2.0*

## Fields

### `RequestContext`

```java
public RequestContext
```

Construtor compacto com valores padrão.

## Methods

### `newInstance`

```java
public static final ScopedValue<RequestContext> CURRENT = ScopedValue.newInstance()
```

ScopedValue para o contexto da requisição atual.

ScopedValues são thread-safe, imutáveis e automaticamente limpos. Diferente de ThreadLocal,
não causam memory leaks com virtual threads.

---

### `RequestContext`

```java
public RequestContext(String userId, String apiKey)
```

Construtor conveniente apenas com userId e apiKey.

---

### `get`

```java
public static RequestContext get()
```

Obtém o contexto da requisição atual.

**Returns**

contexto atual

**Throws**

| Type | Condition |
|------|-----------|
| `IllegalStateException` | se não houver contexto definido |

---

### `getOptional`

```java
public static Optional<RequestContext> getOptional()
```

Obtém o contexto da requisição atual, ou Optional.empty() se não houver.

**Returns**

contexto opcional

---

### `isPresent`

```java
public static boolean isPresent()
```

Verifica se há um contexto ativo.

**Returns**

true se há contexto

---

### `runInContext`

```java
public static void runInContext(RequestContext context, Runnable operation)
```

Executa uma operação dentro de um contexto.

**Parameters**

| Name | Description |
|------|-------------|
| `context` | contexto a ser usado |
| `operation` | operação a executar |

