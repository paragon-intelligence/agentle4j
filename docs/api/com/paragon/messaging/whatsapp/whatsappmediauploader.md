# :material-code-braces: WhatsAppMediaUploader

`com.paragon.messaging.whatsapp.WhatsAppMediaUploader` &nbsp;·&nbsp; **Class**

---

Service for uploading media files to WhatsApp Cloud API.

Handles uploading audio, images, videos, and documents to WhatsApp's media endpoint and
returns media IDs that can be used in message sending.

**Virtual Thread Friendly:** Uses synchronous OkHttp calls which work well with virtual
threads. All I/O operations are blocking but non-blocking at the platform thread level.

### Usage Example

```java
OkHttpClient httpClient = new OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .build();
WhatsAppMediaUploader uploader = new WhatsAppMediaUploader(
    phoneNumberId,
    accessToken,
    httpClient
);
byte[] audioData = ttsProvider.synthesize(text, config);
MediaUploadResponse response = uploader.uploadAudio(audioData, "audio/ogg; codecs=opus");
// Use response.mediaId() in message
```

*Since: 2.1*

## Methods

### `WhatsAppMediaUploader`

```java
public WhatsAppMediaUploader(
      @NonNull String phoneNumberId,
      @NonNull String accessToken,
      @NonNull OkHttpClient httpClient)
```

Creates a new WhatsApp media uploader.

**Parameters**

| Name | Description |
|------|-------------|
| `phoneNumberId` | WhatsApp Business phone number ID |
| `accessToken` | WhatsApp Business API access token |
| `httpClient` | configured OkHttp client (should be shared/reused) |

---

### `uploadAudio`

```java
public @NonNull MediaUploadResponse uploadAudio(
      @NonNull byte[] audioBytes, @NonNull String mimeType) throws MediaUploadException
```

Uploads audio data to WhatsApp Media API.

WhatsApp supports the following audio formats:

  
- AAC - audio/aac
- AMR - audio/amr
- MP3 - audio/mpeg
- MP4 Audio - audio/mp4
- OGG Opus - audio/ogg (recommended for TTS)

**Parameters**

| Name | Description |
|------|-------------|
| `audioBytes` | audio file content |
| `mimeType` | MIME type (e.g., "audio/ogg; codecs=opus") |

**Returns**

upload response with media ID

**Throws**

| Type | Condition |
|------|-----------|
| `MediaUploadException` | if upload fails |

---

### `uploadImage`

```java
public @NonNull MediaUploadResponse uploadImage(
      @NonNull byte[] imageBytes, @NonNull String mimeType) throws MediaUploadException
```

Uploads image data to WhatsApp Media API.

**Parameters**

| Name | Description |
|------|-------------|
| `imageBytes` | image file content |
| `mimeType` | MIME type (e.g., "image/jpeg", "image/png") |

**Returns**

upload response with media ID

**Throws**

| Type | Condition |
|------|-----------|
| `MediaUploadException` | if upload fails |

---

### `uploadMedia`

```java
private MediaUploadResponse uploadMedia(
      byte[] fileBytes, String mimeType, String type, String extension)
      throws MediaUploadException
```

Core upload method that handles multipart/form-data upload.

**Parameters**

| Name | Description |
|------|-------------|
| `fileBytes` | file content |
| `mimeType` | MIME type |
| `type` | media type for API (audio, image, video, document) |
| `extension` | file extension |

**Returns**

upload response with media ID

**Throws**

| Type | Condition |
|------|-----------|
| `MediaUploadException` | if upload fails |

---

### `parseUploadResponse`

```java
private MediaUploadResponse parseUploadResponse(Response response) throws MediaUploadException
```

Parses the upload response from WhatsApp API.

**Parameters**

| Name | Description |
|------|-------------|
| `response` | HTTP response |

**Returns**

parsed upload response

**Throws**

| Type | Condition |
|------|-----------|
| `MediaUploadException` | if parsing fails or API returned error |

---

### `extractMediaId`

```java
private String extractMediaId(String responseBody)
```

Extracts media ID from JSON response (simplified JSON parsing).

**Parameters**

| Name | Description |
|------|-------------|
| `responseBody` | JSON response body |

**Returns**

media ID or null if not found

---

### `determineFileExtension`

```java
private String determineFileExtension(String mimeType)
```

Determines file extension from MIME type.

**Parameters**

| Name | Description |
|------|-------------|
| `mimeType` | MIME type |

**Returns**

file extension (without dot)

