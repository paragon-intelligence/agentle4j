# :material-database: TTSConfig

> This docs was updated at: 2026-02-23

`com.paragon.messaging.whatsapp.config.TTSConfig` &nbsp;·&nbsp; **Record**

---

Configuration for Text-to-Speech in WhatsApp messaging.

Controls whether and how often responses are sent as audio messages instead of text messages.

### Usage Example

```java
// Disabled TTS (default)
TTSConfig config = TTSConfig.disabled();
// TTS with 30% chance of audio response
TTSConfig config = TTSConfig.builder()
    .provider(elevenLabsProvider)
    .speechChance(0.3)
    .languageCode("pt-BR")
    .defaultVoiceId("voice123")
    .build();
```

*Since: 2.1*

## Fields

### `TTSConfig`

```java
public TTSConfig
```

Canonical constructor with validation.

## Methods

### `disabled`

```java
public static TTSConfig disabled()
```

Creates a disabled TTS configuration.

No audio responses will be generated.

**Returns**

disabled TTS configuration

---

### `alwaysAudio`

```java
public static TTSConfig alwaysAudio(@NonNull TTSProvider provider)
```

Creates a TTS configuration that always responds with audio.

**Parameters**

| Name | Description |
|------|-------------|
| `provider` | the TTS provider |

**Returns**

always-audio TTS configuration

---

### `builder`

```java
public static Builder builder()
```

Creates a new builder for TTSConfig.

**Returns**

new builder

---

### `isEnabled`

```java
public boolean isEnabled()
```

Checks if TTS is enabled.

**Returns**

true if a provider is configured and speechChance is greater than 0

---

### `shouldUseAudio`

```java
public boolean shouldUseAudio(double randomValue)
```

Checks if TTS should be used for a given random value.

Call this with `random.nextDouble()` to determine if audio should be sent.

**Parameters**

| Name | Description |
|------|-------------|
| `randomValue` | random value between 0.0 and 1.0 |

**Returns**

true if audio should be used

---

### `provider`

```java
public Builder provider(@Nullable TTSProvider provider)
```

Sets the TTS provider.

**Parameters**

| Name | Description |
|------|-------------|
| `provider` | the TTS provider |

**Returns**

this builder

---

### `speechChance`

```java
public Builder speechChance(double chance)
```

Sets the probability of responding with audio.

**Parameters**

| Name | Description |
|------|-------------|
| `chance` | probability between 0.0 (never) and 1.0 (always) |

**Returns**

this builder

---

### `defaultVoiceId`

```java
public Builder defaultVoiceId(@Nullable String voiceId)
```

Sets the default voice ID for synthesis.

**Parameters**

| Name | Description |
|------|-------------|
| `voiceId` | provider-specific voice identifier |

**Returns**

this builder

---

### `languageCode`

```java
public Builder languageCode(@NonNull String code)
```

Sets the language code for synthesis.

**Parameters**

| Name | Description |
|------|-------------|
| `code` | language code (e.g., "pt-BR", "en-US") |

**Returns**

this builder

---

### `build`

```java
public TTSConfig build()
```

Builds the TTSConfig.

**Returns**

the built configuration

