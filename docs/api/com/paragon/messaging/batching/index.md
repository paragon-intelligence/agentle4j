# Package `com.paragon.messaging.batching`

---

## :material-code-braces: Classs

| Name | Description |
|------|-------------|
| [`MessageBatchingService`](messagebatchingservice.md) | Serviço principal de batching e rate limiting de mensagens |
| [`UserMessageBuffer`](usermessagebuffer.md) | Buffer de mensagens para um único usuário (thread-safe) |

## :material-database: Records

| Name | Description |
|------|-------------|
| [`BatchingConfig`](batchingconfig.md) | Main configuration for `MessageBatchingService` |
| [`Message`](message.md) | Immutable message data for batching |

## :material-format-list-bulleted-type: Enums

| Name | Description |
|------|-------------|
| [`BackpressureStrategy`](backpressurestrategy.md) | Estratégia para lidar com backpressure quando buffer do usuário está cheio |
