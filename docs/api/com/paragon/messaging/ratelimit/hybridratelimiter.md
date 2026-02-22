# :material-code-braces: HybridRateLimiter

`com.paragon.messaging.ratelimit.HybridRateLimiter` &nbsp;·&nbsp; **Class**

---

Rate limiter híbrido: Token Bucket + Sliding Window.

Combina duas estratégias:

  
- **Token Bucket:** Rate limiting suave com bursts controlados
- **Sliding Window:** Anti-flood rígido

Mensagem é aceita APENAS se AMBAS estratégias permitirem.

**Token Bucket:** Tokens são reabastecidos continuamente. Permite bursts até capacidade
máxima.

**Sliding Window:** Conta mensagens em janela deslizante. Bloqueia spam agressivo.

*Since: 1.0*

## Methods

### `tryAcquire`

```java
public boolean tryAcquire()
```

Tenta adquirir permissão para processar mensagem.

**Returns**

true se ambos (token bucket E sliding window) permitirem

