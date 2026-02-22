# :material-format-list-bulleted-type: BackpressureStrategy

`com.paragon.messaging.batching.BackpressureStrategy` &nbsp;·&nbsp; **Enum**

---

Estratégia para lidar com backpressure quando buffer do usuário está cheio.

Backpressure ocorre quando mensagens chegam mais rápido que podem ser processadas, causando
enchimento do buffer até capacidade máxima.

**Exemplo:**

```java
BatchingConfig config = BatchingConfig.builder()
    .maxBufferSize(50)
    .backpressureStrategy(BackpressureStrategy.DROP_OLDEST)
    .build();
```

*Since: 1.0*

## Constants

### `Not`

```java
DROP_NEW,

  /**
   * Descarta mensagem mais antiga para dar espaço à nova.
   *
   * <p><b>Use quando:</b> Mensagens recentes são mais relevantes.
   *
   * <p><b>Comportamento:</b>
   *
   * <ul>
   *   <li>Mensagem mais antiga é removida
   *   <li>Nova mensagem é adicionada
   *   <li>Sem notificação ao usuário
   * </ul>
   */
  DROP_OLDEST,

  /**
   * Rejeita mensagem nova e opcionalmente notifica usuário.
   *
   * <p><b>Use quando:</b> Quer informar que está enviando rápido demais.
   *
   * <p><b>Comportamento:</b>
   *
   * <ul>
   *   <li>Nova mensagem é rejeitada
   *   <li>Not
```

Descarta mensagem nova (buffer inalterado).

**Use quando:** Mensagens antigas têm mais contexto.

**Comportamento:**

  
- Nova mensagem é descartada silenciosamente
- Buffer preserva mensagens mais antigas
- Sem notificação ao usuário

---

### `FLUSH_AND_ACCEPT`

```java
BLOCK_UNTIL_SPACE,

  /**
   * Processa buffer imediatamente, depois aceita nova mensagem.
   *
   * <p><b>Use quando:</b> Quer processar acumuladas quando buffer enche.
   *
   * <p><b>Comportamento:</b>
   *
   * <ul>
   *   <li>Buffer atual é enviado ao processor imediatamente
   *   <li>Nova mensagem inicia buffer fresco
   *   <li>Sem perda de mensagens
   * </ul>
   */
  FLUSH_AND_ACCEPT
```

Bloqueia até espaço ficar disponível (espera síncrona).

**Use quando:** Perda de mensagem é inaceitável.

**Comportamento:**

  
- Webhook bloqueia (200 OK atrasado)
- Mensagem é enfileirada quando espaço disponível
- **Risco:** Pode causar timeout do webhook

**Aviso:** Webhooks da Meta timeout após 20 segundos!

---

### `FLUSH_AND_ACCEPT`

```java
FLUSH_AND_ACCEPT
```

Processa buffer imediatamente, depois aceita nova mensagem.

**Use quando:** Quer processar acumuladas quando buffer enche.

**Comportamento:**

  
- Buffer atual é enviado ao processor imediatamente
- Nova mensagem inicia buffer fresco
- Sem perda de mensagens

## Methods

### `disponível`

```java
DROP_OLDEST,

  /**
   * Rejeita mensagem nova e opcionalmente notifica usuário.
   *
   * <p><b>Use quando:</b> Quer informar que está enviando rápido demais.
   *
   * <p><b>Comportamento:</b>
   *
   * <ul>
   *   <li>Nova mensagem é rejeitada
   *   <li>Notificação opcional enviada ao usuário
   *   <li>Buffer inalterado
   * </ul>
   */
  REJECT_WITH_NOTIFICATION,

  /**
   * Bloqueia até espaço ficar disponível (espera síncrona).
   *
   * <p><b>Use quando:</b> Perda de mensagem é inaceitável.
   *
   * <p><b>Comportamento:</b>
   *
   * <ul>
   *   <li>Webhook bloqueia (200 OK atrasa
```

Descarta mensagem mais antiga para dar espaço à nova.

**Use quando:** Mensagens recentes são mais relevantes.

**Comportamento:**

  
- Mensagem mais antiga é removida
- Nova mensagem é adicionada
- Sem notificação ao usuário

---

### `disponível`

```java
REJECT_WITH_NOTIFICATION,

  /**
   * Bloqueia até espaço ficar disponível (espera síncrona).
   *
   * <p><b>Use quando:</b> Perda de mensagem é inaceitável.
   *
   * <p><b>Comportamento:</b>
   *
   * <ul>
   *   <li>Webhook bloqueia (200 OK atrasado)
   *   <li>Mensagem é enfileirada quando espaço disponível
   *   <li><b>Risco:</b> Pode causar timeout do webhook
   * </ul>
   *
   * <p><b>Aviso:</b> Webhooks da Meta timeout após 20 segundos!
   */
  BLOCK_UNTIL_SPACE,

  /**
   * Processa buffer imediatamente, depois aceita nova mensagem.
   *
   * <p><b>Use quando:</b> Quer processar
```

Rejeita mensagem nova e opcionalmente notifica usuário.

**Use quando:** Quer informar que está enviando rápido demais.

**Comportamento:**

  
- Nova mensagem é rejeitada
- Notificação opcional enviada ao usuário
- Buffer inalterado

---

### `canLoseMessages`

```java
public boolean canLoseMessages()
```

Verifica se estratégia pode causar perda de mensagens.

**Returns**

true se pode perder mensagens

---

### `canBlock`

```java
public boolean canBlock()
```

Verifica se estratégia pode bloquear webhook.

**Returns**

true se pode bloquear

---

### `description`

```java
public String description()
```

Retorna descrição legível da estratégia.

**Returns**

descrição

