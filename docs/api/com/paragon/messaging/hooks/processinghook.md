# :material-approximately-equal: ProcessingHook

`com.paragon.messaging.hooks.ProcessingHook` &nbsp;·&nbsp; **Interface**

---

Hook executado antes ou depois do processamento de lote de mensagens.

Hooks recebem `HookContext` e podem realizar ações como:

  
- Logging e métricas
- Validação e moderação de conteúdo
- Enriquecimento de dados
- Auditoria

**Pre-hooks** podem interromper processamento lançando `HookInterruptedException`:

```java
ProcessingHook moderationHook = context -> {
    if (hasInappropriateContent(context.messages())) {
        throw new HookInterruptedException(
            "Conteúdo inapropriado detectado",
            "CONTENT_MODERATION"
        );
    }
};
```

**Compartilhamento de dados:**

```java
ProcessingHook preHook = context -> {
    context.putMetadata("startTime", Instant.now());
};
ProcessingHook postHook = context -> {
    Instant start = context.getMetadata("startTime", Instant.class).orElseThrow();
    Duration elapsed = Duration.between(start, Instant.now());
    metrics.record(context.userId(), elapsed);
};
```

**See Also**

- `HookContext`
- `HookInterruptedException`

*Since: 1.0*

## Methods

### `noOp`

```java
static ProcessingHook noOp()
```

Hook no-op que não faz nada.

**Returns**

hook vazio

---

### `logging`

```java
static ProcessingHook logging(Consumer<String> logger)
```

Hook de logging básico.

**Parameters**

| Name | Description |
|------|-------------|
| `logger` | consumidor da mensagem de log |

**Returns**

hook de logging

---

### `timing`

```java
static HookPair timing(BiConsumer<String, Duration> recorder)
```

Hook para medir tempo de processamento.

Retorna par de hooks (pre, post) que medem duração.

**Parameters**

| Name | Description |
|------|-------------|
| `recorder` | consumidor que recebe userId e duração |

**Returns**

par de hooks (pre, post)

---

### `compose`

```java
static ProcessingHook compose(ProcessingHook... hooks)
```

Combina múltiplos hooks em um único.

**Parameters**

| Name | Description |
|------|-------------|
| `hooks` | hooks a combinar |

**Returns**

hook composto

---

### `execute`

```java
void execute(HookContext context) throws Exception
```

Executa lógica do hook.

**Parameters**

| Name | Description |
|------|-------------|
| `context` | contexto com informações do lote |

**Throws**

| Type | Condition |
|------|-----------|
| `HookInterruptedException` | para interromper processamento (pre-hooks) |
| `Exception` | para erros (será logado e pode triggerar retry) |

