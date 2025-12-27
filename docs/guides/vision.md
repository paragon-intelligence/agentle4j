# Vision Guide

Agentle4j supports vision capabilities for analyzing images with GPT-4o, Claude, and other vision-capable models.

---

## Image Input

Send images to the AI for analysis:

```java
import com.paragon.responses.spec.content.Image;
import com.paragon.responses.spec.content.ImageDetail;
import com.paragon.responses.spec.input.UserMessage;
import com.paragon.responses.spec.Message;

// Create image from URL
Image image = new Image(
    ImageDetail.AUTO,  // AUTO, LOW, or HIGH
    null,              // Base64 data (null if using URL)
    "https://example.com/photo.jpg"  // Image URL
);

// Create message with image and text
UserMessage message = Message.builder()
    .addText("What's in this image?")
    .addContent(image)
    .asUser();

// Build and send request
var payload = CreateResponsePayload.builder()
    .model("openai/gpt-4o")  // Must use vision-capable model
    .addMessage(message)
    .build();

Response response = responder.respond(payload).join();
System.out.println(response.outputText());
```

---

## Image Detail Levels

| Level | Description | Best For |
|-------|-------------|----------|
| `ImageDetail.AUTO` | Let the model decide | Most use cases |
| `ImageDetail.LOW` | Fast, lower token cost | Simple analysis |
| `ImageDetail.HIGH` | Detailed analysis | OCR, fine details |

```java
// High detail for precise analysis
Image highDetailImage = new Image(
    ImageDetail.HIGH,
    null,
    "https://example.com/document.png"
);

// Low detail for quick categorization
Image lowDetailImage = new Image(
    ImageDetail.LOW,
    null,
    "https://example.com/thumbnail.jpg"
);
```

---

## Base64 Images

For local files or generated images:

```java
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

// Read file and encode to base64
byte[] imageBytes = Files.readAllBytes(Path.of("local-image.jpg"));
String base64Data = Base64.getEncoder().encodeToString(imageBytes);

// Create image with base64
Image image = new Image(
    ImageDetail.AUTO,
    "data:image/jpeg;base64," + base64Data,  // Base64 with data URI prefix
    null  // No URL when using base64
);

UserMessage message = Message.builder()
    .addText("Describe this image")
    .addContent(image)
    .asUser();
```

---

## Multiple Images

Analyze multiple images in one request:

```java
Image image1 = new Image(ImageDetail.AUTO, null, "https://example.com/before.jpg");
Image image2 = new Image(ImageDetail.AUTO, null, "https://example.com/after.jpg");

UserMessage message = Message.builder()
    .addText("Compare these two images and describe the differences")
    .addContent(image1)
    .addContent(image2)
    .asUser();

var payload = CreateResponsePayload.builder()
    .model("openai/gpt-4o")
    .addMessage(message)
    .build();
```

---

## Use Cases

### Document OCR

```java
Image document = new Image(ImageDetail.HIGH, null, "https://example.com/receipt.png");

UserMessage message = Message.builder()
    .addText("Extract all text from this receipt. Format as JSON with date, items, and total.")
    .addContent(document)
    .asUser();

// Use structured output for clean extraction
record ReceiptData(
    String date,
    List<LineItem> items,
    double total,
    String merchantName
) {}
record LineItem(String name, int quantity, double price) {}

var payload = CreateResponsePayload.builder()
    .model("openai/gpt-4o")
    .addMessage(message)
    .withStructuredOutput(ReceiptData.class)
    .build();

ReceiptData receipt = responder.respond(payload).join().outputParsed();
```

### Image Classification

```java
record Classification(
    String category,
    double confidence,
    List<String> tags,
    String description
) {}

Image photo = new Image(ImageDetail.AUTO, null, imageUrl);

UserMessage message = Message.builder()
    .addText("Classify this image. Categories: product, person, landscape, document, food, animal, other")
    .addContent(photo)
    .asUser();

var payload = CreateResponsePayload.builder()
    .model("openai/gpt-4o")
    .addMessage(message)
    .withStructuredOutput(Classification.class)
    .build();

Classification result = responder.respond(payload).join().outputParsed();
System.out.println("Category: " + result.category() + " (" + result.confidence() + ")");
```

### Chart/Graph Analysis

```java
Image chart = new Image(ImageDetail.HIGH, null, "https://example.com/sales-chart.png");

UserMessage message = Message.builder()
    .addText("""
        Analyze this sales chart. Extract:
        1. The trend (growing/declining/stable)
        2. Key data points
        3. Any notable patterns or anomalies
        """)
    .addContent(chart)
    .asUser();

var payload = CreateResponsePayload.builder()
    .model("openai/gpt-4o")
    .addMessage(message)
    .build();

System.out.println(responder.respond(payload).join().outputText());
```

---

## Vision with Agents

Combine vision with agent capabilities:

```java
Agent visionAssistant = Agent.builder()
    .name("VisionAssistant")
    .model("openai/gpt-4o")
    .instructions("""
        You are a visual analysis assistant.
        You can analyze images, extract text, and answer questions about visual content.
        Be detailed but concise in your descriptions.
        """)
    .responder(responder)
    .addTool(webSearchTool)  // Can search for more info
    .addTool(saveFileTool)   // Can save extracted data
    .build();

// Create context with image
Image image = new Image(ImageDetail.HIGH, null, documentUrl);
UserMessage message = Message.builder()
    .addText("Extract all text from this document and save it to a file")
    .addContent(image)
    .asUser();

AgentContext context = AgentContext.create();
// The message is passed directly to interact()
AgentResult result = visionAssistant.interact(message.content().toString(), context).join();
```

---

## Supported Models

| Provider | Model | Vision Support |
|----------|-------|----------------|
| OpenAI | `gpt-4o` | ✅ Full |
| OpenAI | `gpt-4o-mini` | ✅ Full |
| OpenAI | `gpt-4-turbo` | ✅ Full |
| Anthropic | `claude-3.5-sonnet` | ✅ Full |
| Anthropic | `claude-3-opus` | ✅ Full |
| Google | `gemini-1.5-pro` | ✅ Full |
| OpenAI | `gpt-3.5-turbo` | ❌ None |

!!! warning "Model Requirement"
    Always use a vision-capable model when sending images. Non-vision models will ignore or error on image content.

---

## Best Practices

### ✅ Do

```java
// Use HIGH detail for text extraction
Image doc = new Image(ImageDetail.HIGH, null, url);

// Be specific in prompts
.addText("Extract the product name, price, and SKU from this product label")

// Use structured output for data extraction
.withStructuredOutput(ExtractedData.class)
```

### ❌ Don't

```java
// Don't use non-vision models
.model("gpt-3.5-turbo")  // No vision support!

// Don't send huge images unnecessarily
// Resize/compress before if possible

// Don't be vague
.addText("What is this?")  // Too vague!
```

---

## Next Steps

- [Agents Guide](agents.md) - Use vision with agents
- [Structured Outputs](../getting-started.md#structured-outputs) - Extract data from images
