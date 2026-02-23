# :material-approximately-equal: TTSProvider

> This docs was updated at: 2026-02-23

`com.paragon.tts.TTSProvider` &nbsp;·&nbsp; **Interface**

---

Provider genérico de Text-to-Speech.

Implementações suportam diferentes providers:

  
- ElevenLabs
- OpenAI TTS
- Google Cloud TTS
- Azure Speech
- Amazon Polly

**Exemplo:**

```java
TTSProvider tts = ElevenLabsTTSProvider.create(apiKey);
TTSConfig config = TTSConfig.ptBR("voiceId");
byte[] audio = tts.synthesize("Olá, como posso ajudar?", config);
```

*Since: 1.0*

## Methods

### `noOp`

```java
static TTSProvider noOp()
```

Provider no-op para testes.

**Returns**

provider vazio

---

### `synthesize`

```java
Byte[] synthesize(@NonNull String text, @Nullable TTSConfig config) throws TTSException
```

Sintetiza texto em áudio.

**Parameters**

| Name | Description |
|------|-------------|
| `text` | texto a converter |
| `config` | configuração de voz, idioma, velocidade, pitch |

**Returns**

bytes do áudio (formato depende do provider)

**Throws**

| Type | Condition |
|------|-----------|
| `TTSException` | se síntese falhar |

---

### `isAvailable`

```java
default boolean isAvailable()
```

Verifica se provider está disponível.

**Returns**

true se está configurado e pronto

---

### `audioFormat`

```java
default String audioFormat()
```

Retorna formato de áudio retornado.

**Returns**

formato (ex: "mp3", "opus", "aac")

