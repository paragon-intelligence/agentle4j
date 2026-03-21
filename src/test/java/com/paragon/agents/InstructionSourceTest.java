package com.paragon.agents;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.prompts.Prompt;
import com.paragon.prompts.PromptProvider;
import com.paragon.prompts.PromptProviderRegistry;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;

/**
 * Tests for {@link InstructionSource} — all three variants, Jackson serialization, and backward
 * compatibility.
 */
class InstructionSourceTest {

  private ObjectMapper mapper;

  @BeforeEach
  void setUp() {
    mapper =
        new ObjectMapper()
            .rebuild()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build();
    PromptProviderRegistry.clear();
  }

  @AfterEach
  void tearDown() {
    PromptProviderRegistry.clear();
  }

  // ===== Inline =====

  @Test
  void inlineResolve() {
    var source = new InstructionSource.Inline("You are a helpful assistant.");
    assertEquals("You are a helpful assistant.", source.resolve());
  }

  @Test
  void inlineNullTextThrows() {
    assertThrows(NullPointerException.class, () -> new InstructionSource.Inline(null));
  }

  @Test
  void inlineJsonRoundtrip() throws Exception {
    var source = new InstructionSource.Inline("Hello world");
    String json = mapper.writeValueAsString(source);
    InstructionSource restored = mapper.readValue(json, InstructionSource.class);
    assertInstanceOf(InstructionSource.Inline.class, restored);
    assertEquals("Hello world", restored.resolve());
  }

  @Test
  void backwardCompatiblePlainStringDeserialization() throws Exception {
    // A plain JSON string should deserialize into InstructionSource.Inline
    String json = "\"You are a support agent.\"";
    InstructionSource source = mapper.readValue(json, InstructionSource.class);
    assertInstanceOf(InstructionSource.Inline.class, source);
    assertEquals("You are a support agent.", source.resolve());
  }

  @Test
  void inlineObjectDeserialization() throws Exception {
    String json =
        """
        { "source": "inline", "text": "Custom text" }
        """;
    InstructionSource source = mapper.readValue(json, InstructionSource.class);
    assertInstanceOf(InstructionSource.Inline.class, source);
    assertEquals("Custom text", source.resolve());
  }

  // ===== FileRef =====

  @Test
  void fileRefResolve(@TempDir Path tempDir) throws IOException {
    Path file = tempDir.resolve("prompt.txt");
    Files.writeString(file, "Instructions from file");

    var source = new InstructionSource.FileRef(file.toString());
    assertEquals("Instructions from file", source.resolve());
  }

  @Test
  void fileRefMissingFileThrows() {
    var source = new InstructionSource.FileRef("/nonexistent/path/file.txt");
    assertThrows(IllegalStateException.class, source::resolve);
  }

  @Test
  void fileRefEmptyPathThrows() {
    assertThrows(IllegalArgumentException.class, () -> new InstructionSource.FileRef(""));
  }

  @Test
  void fileRefNullPathThrows() {
    assertThrows(NullPointerException.class, () -> new InstructionSource.FileRef(null));
  }

  @Test
  void fileRefJsonRoundtrip(@TempDir Path tempDir) throws Exception {
    Path file = tempDir.resolve("prompt.txt");
    Files.writeString(file, "File content");

    var source = new InstructionSource.FileRef(file.toString());
    String json = mapper.writeValueAsString(source);
    InstructionSource restored = mapper.readValue(json, InstructionSource.class);

    assertInstanceOf(InstructionSource.FileRef.class, restored);
    assertEquals("File content", restored.resolve());
  }

  @Test
  void fileRefDeserialization(@TempDir Path tempDir) throws Exception {
    Path file = tempDir.resolve("agent.txt");
    Files.writeString(file, "From file");

    String json =
        """
        { "source": "file", "path": "%s" }
        """
            .formatted(file.toString().replace("\\", "\\\\"));
    InstructionSource source = mapper.readValue(json, InstructionSource.class);

    assertInstanceOf(InstructionSource.FileRef.class, source);
    assertEquals("From file", source.resolve());
  }

  // ===== ProviderRef =====

  @Test
  void providerRefResolve() {
    PromptProviderRegistry.register(
        "test",
        new PromptProvider() {
          @Override
          public Prompt providePrompt(String promptId, Map<String, String> filters) {
            assertEquals("my-prompt", promptId);
            return Prompt.of("Provider content");
          }

          @Override
          public boolean exists(String promptId) {
            return "my-prompt".equals(promptId);
          }

          @Override
          public Set<String> listPromptIds() {
            return Set.of("my-prompt");
          }
        });

    var source = new InstructionSource.ProviderRef("test", "my-prompt", null);
    assertEquals("Provider content", source.resolve());
  }

  @Test
  void providerRefWithFilters() {
    PromptProviderRegistry.register(
        "langfuse",
        new PromptProvider() {
          @Override
          public Prompt providePrompt(String promptId, Map<String, String> filters) {
            assertEquals("agent-v2", promptId);
            assertNotNull(filters);
            assertEquals("production", filters.get("label"));
            return Prompt.of("Filtered content");
          }

          @Override
          public boolean exists(String promptId) {
            return true;
          }

          @Override
          public Set<String> listPromptIds() {
            return Set.of("agent-v2");
          }
        });

    var source =
        new InstructionSource.ProviderRef("langfuse", "agent-v2", Map.of("label", "production"));
    assertEquals("Filtered content", source.resolve());
  }

  @Test
  void providerRefUnregisteredThrows() {
    var source = new InstructionSource.ProviderRef("nonexistent", "prompt-1", null);
    var ex = assertThrows(IllegalStateException.class, source::resolve);
    assertTrue(ex.getMessage().contains("nonexistent"));
    assertTrue(ex.getMessage().contains("PromptProviderRegistry"));
  }

  @Test
  void providerRefEmptyProviderIdThrows() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new InstructionSource.ProviderRef("", "prompt", null));
  }

  @Test
  void providerRefEmptyPromptIdThrows() {
    assertThrows(
        IllegalArgumentException.class, () -> new InstructionSource.ProviderRef("test", "", null));
  }

  @Test
  void providerRefJsonRoundtrip() throws Exception {
    String json =
        """
        {
          "source": "provider",
          "providerId": "langfuse",
          "promptId": "my-prompt",
          "filters": { "label": "production", "version": "2" }
        }
        """;
    InstructionSource source = mapper.readValue(json, InstructionSource.class);

    assertInstanceOf(InstructionSource.ProviderRef.class, source);
    InstructionSource.ProviderRef ref = (InstructionSource.ProviderRef) source;
    assertEquals("langfuse", ref.providerId());
    assertEquals("my-prompt", ref.promptId());
    assertEquals("production", ref.filters().get("label"));
    assertEquals("2", ref.filters().get("version"));
  }

  // ===== InstructionSource in AgentBlueprint context =====

  @Test
  void agentBlueprintWithInlineInstructionsJsonRoundtrip() throws Exception {
    String json =
        """
        {
          "type": "agent",
          "name": "TestAgent",
          "model": "gpt-4o",
          "instructions": "Plain string instructions",
          "maxTurns": 5,
          "responder": { "provider": "OPEN_ROUTER", "apiKeyEnvVar": "OPENROUTER_API_KEY" },
          "toolClassNames": [],
          "handoffs": [],
          "inputGuardrails": [],
          "outputGuardrails": []
        }
        """;

    InteractableBlueprint bp = mapper.readValue(json, InteractableBlueprint.class);
    assertInstanceOf(InteractableBlueprint.AgentBlueprint.class, bp);
    InteractableBlueprint.AgentBlueprint agent = (InteractableBlueprint.AgentBlueprint) bp;
    assertInstanceOf(InstructionSource.Inline.class, agent.instructions());
    assertEquals("Plain string instructions", agent.instructions().resolve());
  }

  @Test
  void agentBlueprintWithFileInstructionsJsonRoundtrip(@TempDir Path tempDir) throws Exception {
    Path file = tempDir.resolve("instructions.txt");
    Files.writeString(file, "Instructions from a file");

    String json =
        """
        {
          "type": "agent",
          "name": "FileAgent",
          "model": "gpt-4o",
          "instructions": {
            "source": "file",
            "path": "%s"
          },
          "maxTurns": 5,
          "responder": { "provider": "OPEN_ROUTER", "apiKeyEnvVar": "OPENROUTER_API_KEY" },
          "toolClassNames": [],
          "handoffs": [],
          "inputGuardrails": [],
          "outputGuardrails": []
        }
        """
            .formatted(file.toString().replace("\\", "\\\\"));

    InteractableBlueprint bp = mapper.readValue(json, InteractableBlueprint.class);
    InteractableBlueprint.AgentBlueprint agent = (InteractableBlueprint.AgentBlueprint) bp;
    assertInstanceOf(InstructionSource.FileRef.class, agent.instructions());
    assertEquals("Instructions from a file", agent.instructions().resolve());
  }
}
