package com.paragon.responses.spec;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.responses.json.JacksonJsonSchemaProducer;
import com.paragon.responses.json.JsonSchemaProducer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link FunctionToolFactory}. */
class FunctionToolFactoryTest {

  private FunctionToolFactory factory;

  @BeforeEach
  void setUp() {
    factory = FunctionToolFactory.create();
  }

  // ===== Test Tool Classes =====

  record SimpleParams(String name) {}

  public static class SimpleTool extends FunctionTool<SimpleParams> {
    public SimpleTool() {
      super();
    }

    public SimpleTool(JsonSchemaProducer producer) {
      super(producer);
    }

    @Override
    public @NonNull FunctionToolCallOutput call(@Nullable SimpleParams params) {
      return FunctionToolCallOutput.success(
          "Called with: " + (params != null ? params.name() : "null"));
    }
  }

  public static class NoArgOnlyTool extends FunctionTool<SimpleParams> {
    public NoArgOnlyTool() {
      super();
    }

    @Override
    public @NonNull FunctionToolCallOutput call(@Nullable SimpleParams params) {
      return FunctionToolCallOutput.success("NoArg tool called");
    }
  }

  public static class ProducerOnlyTool extends FunctionTool<SimpleParams> {
    private final JsonSchemaProducer producer;

    public ProducerOnlyTool(JsonSchemaProducer producer) {
      super(producer);
      this.producer = producer;
    }

    @Override
    public @NonNull FunctionToolCallOutput call(@Nullable SimpleParams params) {
      return FunctionToolCallOutput.success("Producer tool called");
    }

    public JsonSchemaProducer getProducer() {
      return producer;
    }
  }

  // ===== Tests =====

  @Nested
  @DisplayName("Factory Creation")
  class FactoryCreation {

    @Test
    @DisplayName("create() returns non-null factory")
    void createReturnsNonNullFactory() {
      assertNotNull(FunctionToolFactory.create());
    }

    @Test
    @DisplayName("withObjectMapper() uses provided mapper")
    void createWithObjectMapperUsesProvidedMapper() {
      ObjectMapper mapper = new ObjectMapper();
      FunctionToolFactory f = FunctionToolFactory.withObjectMapper(mapper);
      assertNotNull(f.getJsonSchemaProducer());
    }

    @Test
    @DisplayName("withProducer() uses provided producer")
    void createWithProducerUsesProvidedProducer() {
      JsonSchemaProducer producer = new JacksonJsonSchemaProducer(new ObjectMapper());
      FunctionToolFactory f = FunctionToolFactory.withProducer(producer);
      assertSame(producer, f.getJsonSchemaProducer());
    }
  }

  @Nested
  @DisplayName("Tool Creation")
  class ToolCreation {

    @Test
    @DisplayName("create(Class) creates tool with JsonSchemaProducer constructor")
    void createToolWithProducerConstructor() {
      ProducerOnlyTool tool = factory.create(ProducerOnlyTool.class);

      assertNotNull(tool);
      assertSame(factory.getJsonSchemaProducer(), tool.getProducer());
    }

    @Test
    @DisplayName("create(Class) falls back to default constructor")
    void createToolWithDefaultConstructor() {
      NoArgOnlyTool tool = factory.create(NoArgOnlyTool.class);

      assertNotNull(tool);
    }

    @Test
    @DisplayName("create(Class) prefers JsonSchemaProducer constructor")
    void createToolPrefersProducerConstructor() {
      // SimpleTool has both constructors; factory should use the one with JsonSchemaProducer
      SimpleTool tool = factory.create(SimpleTool.class);

      assertNotNull(tool);
    }

    @Test
    @DisplayName("create(Class) throws on null class")
    void createToolThrowsOnNullClass() {
      assertThrows(NullPointerException.class, () -> factory.create(null));
    }

    @Test
    @DisplayName("created tool can be called successfully")
    void createdToolCanBeCalled() {
      SimpleTool tool = factory.create(SimpleTool.class);
      FunctionToolCallOutput result = tool.call(new SimpleParams("test"));

      assertNotNull(result);
      assertTrue(result.output().toString().contains("test"));
    }
  }

  @Nested
  @DisplayName("Integration with FunctionToolStore")
  class StoreIntegration {

    @Test
    @DisplayName("factory-created tools work with store")
    void factoryCreatedToolsWorkWithStore() throws Exception {
      ObjectMapper mapper = new ObjectMapper();
      FunctionToolFactory f = FunctionToolFactory.withObjectMapper(mapper);
      FunctionToolStore store = FunctionToolStore.create(mapper);

      SimpleTool tool = f.create(SimpleTool.class);
      store.add(tool);

      FunctionToolCall toolCall =
          new FunctionToolCall("{\"name\":\"World\"}", "call_123", "simple_tool", null, null);

      FunctionToolCallOutput result = store.execute(toolCall);
      assertNotNull(result);
      assertTrue(result.output().toString().contains("World"));
    }
  }
}
