package cookbooks.structuredoutputtest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.paragon.agents.Agent;
import com.paragon.agents.StructuredAgentResult;
import com.paragon.responses.Responder;
import com.paragon.responses.ResponsesApiObjectMapper;
import com.paragon.responses.json.JacksonJsonSchemaProducer;
import com.paragon.responses.spec.CreateResponsePayload;
import com.paragon.responses.spec.Response;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;

/**
 * Cookbook: Structured Output Validation
 *
 * <p>Verifies end-to-end that the OpenAI strict-mode schema fixes work in practice:
 *
 * <ul>
 *   <li>Nested records are correctly inlined (no {@code $ref})
 *   <li>Jackson URN {@code id} metadata is stripped
 *   <li>The model returns raw JSON (not a markdown code block)
 *   <li>{@code Agent.Structured} parses the response into the correct Java type
 * </ul>
 *
 * <p>The output type {@link ProductOrder} contains:
 * <ul>
 *   <li>A nested {@link Customer} record
 *   <li>A nested {@link Address} record (used inside Customer)
 *   <li>A {@code List<LineItem>} (array of nested objects)
 *   <li>An enum {@link OrderStatus}
 * </ul>
 *
 * <p>Run from the project root:
 * <pre>
 *   export OPENROUTER_API_KEY=...
 *   mvn exec:java -Dexec.mainClass="cookbooks.structuredoutputtest.Main"
 * </pre>
 */
public class Main {

  // ===== Output type hierarchy (intentionally complex to stress-test schema generation) =====

  public record Address(String street, String city, String country, String postalCode) {}

  public record Customer(String name, String email, Address shippingAddress) {}

  public record LineItem(String productName, int quantity, double unitPriceUsd) {}

  public enum OrderStatus {
    PENDING,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED
  }

  public record ProductOrder(
      String orderId,
      Customer customer,
      List<LineItem> items,
      OrderStatus status,
      double totalUsd,
      String notes) {}

  // ===== Entry point =====

  public static void main(String[] args) throws IOException {
    String apiKey = resolveApiKey();
    if (apiKey == null) {
      System.out.println("ERROR: OPENROUTER_API_KEY not found.");
      System.out.println("Set it as an env var or put it in .env at the project root.");
      System.exit(1);
    }

    ObjectMapper responsesMapper = ResponsesApiObjectMapper.create()
        .enable(SerializationFeature.INDENT_OUTPUT);

    String model = args.length > 0 ? args[0] : "openai/gpt-4o-mini";

    String userMessage =
        """
        Hi, I'd like to place an order. I'm Jane Doe, my email is jane@example.com.
        Ship to 42 Maple Street, Toronto, Canada, M5V 2T6.
        I want 2x Wireless Mouse at $29.99 each and 1x USB-C Hub at $49.99.
        The order is confirmed. Please add a note: "Leave at the door".
        """;

    // ── 1. Print the exact request body we will send ─────────────────────────
    CreateResponsePayload payload =
        CreateResponsePayload.builder()
            .model(model)
            .instructions("You are an order extraction assistant. Extract all order details.")
            .addUserMessage(userMessage)
            .withStructuredOutput(ProductOrder.class, null)
            .build();

    String requestBody = responsesMapper.writeValueAsString(payload);
    System.out.println("=".repeat(60));
    System.out.println("REQUEST BODY sent to OpenRouter:");
    System.out.println("=".repeat(60));
    System.out.println(requestBody);

    // ── 2. Schema audit ───────────────────────────────────────────────────────
    JacksonJsonSchemaProducer schemaProducer = new JacksonJsonSchemaProducer(new ObjectMapper());
    Map<String, Object> schema = schemaProducer.produce(ProductOrder.class);
    System.out.println("\n" + "=".repeat(60));
    System.out.println("SCHEMA AUDIT (strict mode rules):");
    System.out.println("=".repeat(60));
    auditSchema(schema);

    // ── 3. Call Responder directly ────────────────────────────────────────────
    // Attach a logging interceptor so we can see the raw HTTP response body.
    StringBuilder rawResponseHolder = new StringBuilder();
    Interceptor loggingInterceptor = chain -> {
      okhttp3.Response httpResponse = chain.proceed(chain.request());
      okhttp3.ResponseBody originalBody = httpResponse.body();
      String rawBody = originalBody != null ? originalBody.string() : "";
      rawResponseHolder.append(rawBody);
      // Re-create the body so OkHttp can still read it
      okhttp3.MediaType ct = originalBody != null ? originalBody.contentType() : null;
      okhttp3.ResponseBody newBody = okhttp3.ResponseBody.create(rawBody, ct);
      return httpResponse.newBuilder().body(newBody).build();
    };

    OkHttpClient httpClient = new OkHttpClient.Builder().addInterceptor(loggingInterceptor).build();
    Responder responder = Responder.builder()
        .openRouter()
        .apiKey(apiKey)
        .httpClient(httpClient)
        .build();

    System.out.println("\n" + "=".repeat(60));
    System.out.println("RESPONDER.respond() — direct call, model: " + model);
    System.out.println("=".repeat(60));

    Response response = responder.respond(payload);
    String outputText = response.outputText();

    System.out.println("\nRaw HTTP response body from OpenRouter:");
    System.out.println(rawResponseHolder);
    System.out.println("\nresponse.outputText():");
    System.out.println(outputText);
    System.out.println("\nStarts with '```': " + (outputText != null && outputText.strip().startsWith("```")));
    System.out.println("Looks like JSON object: " + (outputText != null && outputText.strip().startsWith("{")));
    System.out.println("Looks like JSON array:  " + (outputText != null && outputText.strip().startsWith("[")));

    // ── 4. Agent.Structured test (same responder, same payload) ──────────────
    Agent.Structured<ProductOrder> agent =
        Agent.builder()
            .name("OrderExtractor")
            .instructions("You are an order extraction assistant. Extract all order details.")
            .model(model)
            .responder(responder)
            .structured(ProductOrder.class)
            .build();

    System.out.println("\n" + "=".repeat(60));
    System.out.println("AGENT.STRUCTURED — same model/responder, via Agent.Structured<ProductOrder>");
    System.out.println("=".repeat(60));
    System.out.println("User message:\n" + userMessage.strip());
    System.out.println("\nCalling agent...");

    StructuredAgentResult<ProductOrder> result = agent.interact(userMessage);

    System.out.println("\nRaw output: [" + result.rawOutput() + "]");

    if (result.isError()) {
      System.out.println("\nFAILED - error: " + result.error().getMessage());
      if (result.error().getCause() != null) {
        System.out.println("Caused by: " + result.error().getCause().getMessage());
      }
      System.exit(1);
    }

    ProductOrder order = result.typedOutput();
    System.out.println("\nSUCCESS - parsed ProductOrder:");
    System.out.println("  orderId:   " + order.orderId());
    System.out.println("  status:    " + order.status());
    System.out.println("  total:     $" + order.totalUsd());
    System.out.println("  notes:     " + order.notes());
    System.out.println("  customer:  " + order.customer().name() + " <" + order.customer().email() + ">");
    System.out.println("  address:   "
        + order.customer().shippingAddress().street() + ", "
        + order.customer().shippingAddress().city() + ", "
        + order.customer().shippingAddress().country());
    System.out.println("  items (" + order.items().size() + "):");
    for (LineItem item : order.items()) {
      System.out.printf("    - %s x%d @ $%.2f%n", item.productName(), item.quantity(), item.unitPriceUsd());
    }

    // Assertions — these would cause a non-zero exit if wrong
    assertNotNull(order.orderId(), "orderId");
    assertNotNull(order.customer(), "customer");
    assertNotNull(order.customer().name(), "customer.name");
    assertNotNull(order.customer().shippingAddress(), "customer.shippingAddress");
    assertNotNull(order.customer().shippingAddress().city(), "shippingAddress.city");
    assertNotNull(order.items(), "items");
    assertFalse(order.items().isEmpty(), "items must not be empty");
    assertNotNull(order.status(), "status");

    System.out.println("\nAll assertions passed.");
    System.out.println("Strict mode schema compliance confirmed end-to-end.");
  }

  // ===== Helpers =====

  /** Audits the schema against every OpenAI strict mode rule and prints a report. */
  @SuppressWarnings("unchecked")
  private static void auditSchema(Map<String, Object> schema) {
    System.out.println("Schema audit (OpenAI strict mode rules):");
    boolean clean = true;

    // No $ref
    if (treeContains(schema, "$ref")) {
      System.out.println("  [FAIL] Schema contains $ref references");
      clean = false;
    } else {
      System.out.println("  [PASS] No $ref references");
    }

    // No URN id metadata
    if (treeContainsUrnId(schema)) {
      System.out.println("  [FAIL] Schema contains 'id': 'urn:...' metadata");
      clean = false;
    } else {
      System.out.println("  [PASS] No Jackson URN id metadata");
    }

    // Root is type:object
    if ("object".equals(schema.get("type"))) {
      System.out.println("  [PASS] Root type is 'object'");
    } else {
      System.out.println("  [FAIL] Root type is '" + schema.get("type") + "' (must be 'object')");
      clean = false;
    }

    // Every object has additionalProperties:false and required[]
    List<Map<String, Object>> objects = allObjectNodes(schema);
    boolean objectsOk = true;
    for (var obj : objects) {
      Map<String, Object> props = (Map<String, Object>) obj.get("properties");
      List<String> required = (List<String>) obj.get("required");
      if (!Boolean.FALSE.equals(obj.get("additionalProperties"))) {
        System.out.println("  [FAIL] Object missing additionalProperties:false — " + (props != null ? props.keySet() : "?"));
        objectsOk = false; clean = false;
      }
      if (required == null || (props != null && !required.containsAll(props.keySet()))) {
        System.out.println("  [FAIL] Object has incomplete required[] — " + (props != null ? props.keySet() : "?"));
        objectsOk = false; clean = false;
      }
    }
    if (objectsOk) System.out.println("  [PASS] All " + objects.size() + " object nodes have additionalProperties:false + complete required[]");

    // No forbidden keywords
    var forbidden = Set.of("disallow", "extends", "default", "pattern", "$schema",
        "minLength", "maxLength", "minimum", "maximum", "allOf", "not");
    boolean noForbidden = true;
    for (var kw : forbidden) {
      if (treeContains(schema, kw)) {
        System.out.println("  [FAIL] Forbidden keyword present: '" + kw + "'");
        noForbidden = false; clean = false;
      }
    }
    if (noForbidden) System.out.println("  [PASS] No forbidden keywords");

    System.out.println(clean ? "  => Schema is COMPLIANT with OpenAI strict mode." : "  => Schema has compliance issues.");
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> allObjectNodes(Object node) {
    List<Map<String, Object>> result = new java.util.ArrayList<>();
    collectObjects(node, result);
    return result;
  }

  @SuppressWarnings("unchecked")
  private static void collectObjects(Object node, List<Map<String, Object>> acc) {
    if (!(node instanceof Map<?, ?> raw)) return;
    var map = (Map<String, Object>) raw;
    if ("object".equals(map.get("type")) && map.containsKey("properties")) acc.add(map);
    for (var v : map.values()) {
      if (v instanceof Map) collectObjects(v, acc);
      if (v instanceof List<?> l) l.forEach(i -> collectObjects(i, acc));
    }
  }

  @SuppressWarnings("unchecked")
  private static boolean treeContains(Object node, String key) {
    if (!(node instanceof Map<?, ?> raw)) return false;
    var map = (Map<String, Object>) raw;
    if (map.containsKey(key)) return true;
    for (var v : map.values()) {
      if (treeContains(v, key)) return true;
      if (v instanceof List<?> l) { for (var i : l) if (treeContains(i, key)) return true; }
    }
    return false;
  }

  @SuppressWarnings("unchecked")
  private static boolean treeContainsUrnId(Object node) {
    if (!(node instanceof Map<?, ?> raw)) return false;
    var map = (Map<String, Object>) raw;
    if (map.get("id") instanceof String v && v.startsWith("urn:")) return true;
    for (var val : map.values()) {
      if (treeContainsUrnId(val)) return true;
      if (val instanceof List<?> l) { for (var i : l) if (treeContainsUrnId(i)) return true; }
    }
    return false;
  }

  private static String resolveApiKey() throws IOException {
    // 1. Environment variable
    String fromEnv = System.getenv("OPENROUTER_API_KEY");
    if (fromEnv != null && !fromEnv.isBlank()) return fromEnv;

    // 2. .env file at project root
    Path dotenv = Path.of(".env");
    if (Files.exists(dotenv)) {
      for (String line : Files.readAllLines(dotenv)) {
        line = line.strip();
        if (line.startsWith("OPENROUTER_API_KEY")) {
          int eq = line.indexOf('=');
          if (eq != -1) {
            return line.substring(eq + 1).strip().replace("\"", "");
          }
        }
      }
    }
    return null;
  }

  private static void assertNotNull(Object value, String field) {
    if (value == null) {
      System.out.println("ASSERTION FAILED: " + field + " is null");
      System.exit(1);
    }
  }

  private static void assertFalse(boolean condition, String message) {
    if (condition) {
      System.out.println("ASSERTION FAILED: " + message);
      System.exit(1);
    }
  }
}
