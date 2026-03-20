# :material-code-braces: InMemoryMessageStore

`com.paragon.messaging.store.InMemoryMessageStore` &nbsp;·&nbsp; **Class**

Implements `MessageStore`

---

Implementação in-memory de MessageStore com LRU cache para deduplicação.

Não persiste após restart. Use para desenvolvimento ou quando persistência não é crítica.

**Exemplo:**

```java
MessageStore store = InMemoryMessageStore.create();
// ou com capacidade customizada
MessageStore store = InMemoryMessageStore.create(10000);
```

*Since: 1.0*

## Methods

### `create`

```java
public static InMemoryMessageStore create()
```

Cria store com capacidade padrão (5000 IDs processados por usuário).

**Returns**

novo store

---

### `create`

```java
public static InMemoryMessageStore create(int maxProcessedIds)
```

Cria store com capacidade customizada.

**Parameters**

| Name | Description |
|------|-------------|
| `maxProcessedIds` | máximo de IDs processados a manter por usuário |

**Returns**

novo store

