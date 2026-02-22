# :material-database: RateLimitConfig

`com.paragon.messaging.ratelimit.RateLimitConfig` &nbsp;·&nbsp; **Record**

---

Configuração para rate limiting híbrido (Token Bucket + Sliding Window).

Combina duas estratégias:

  
- **Token Bucket**: Permite bursts controlados (rate limiting suave)
- **Sliding Window**: Anti-flood rígido (proteção contra spam)

Mensagem é aceita APENAS se AMBAS as estratégias permitirem.

**Exemplo Leniente:**

```java
RateLimitConfig config = RateLimitConfig.lenient();
// Token Bucket: 20 tokens/min, capacity 30
// Sliding Window: max 10 msgs em 30s
```

**Exemplo Estrito:**

```java
RateLimitConfig config = RateLimitConfig.strict();
// Token Bucket: 10 tokens/min, capacity 15
// Sliding Window: max 5 msgs em 10s
```

*Since: 1.0*

## Methods

### `lenient`

```java
public static RateLimitConfig lenient()
```

Configuração leniente (padrão).

Boa para maioria dos casos. Permite bursts moderados.

  
- 20 tokens/minuto
- Capacity 30 (50% extra para bursts)
- Max 10 msgs em 30 segundos

**Returns**

config leniente

---

### `strict`

```java
public static RateLimitConfig strict()
```

Configuração estrita.

Para cenários sensíveis ou usuários problemáticos.

  
- 10 tokens/minuto
- Capacity 15
- Max 5 msgs em 10 segundos

**Returns**

config estrita

---

### `permissive`

```java
public static RateLimitConfig permissive()
```

Configuração permissiva (para testes ou usuários VIP).

**Atenção:** Permite bursts muito altos.

  
- 60 tokens/minuto
- Capacity 100
- Max 30 msgs em 60 segundos

**Returns**

config permissiva

---

### `disabled`

```java
public static RateLimitConfig disabled()
```

Desabilita rate limiting (sem limites).

**Aviso:** Use apenas para testes!

**Returns**

config sem limites

