import com.paragon.agents.InMemoryMemory;
import com.paragon.agents.MemoryEntry;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.List;

/**
 * Cookbook Example 16: Agent with Memory
 *
 * <p>Demonstrates using {@link InMemoryMemory} to store, retrieve, and clear
 * user-scoped memories. Memories can later be injected into agent instructions
 * or context to personalise responses.
 *
 * <p>All memory operations are scoped to a {@code userId} so multiple users can
 * share the same memory store without interference.
 *
 * <p>Required environment variables (in .env):
 * <ul>
 *   <li>OPENROUTER_API_KEY (not used in this example but follows the same setup pattern)
 * </ul>
 */
public class AgentWithMemory {

  public static void main(String[] args) {
    Dotenv dotenv = Dotenv.load();
    String apiKey = dotenv.get("OPENROUTER_API_KEY");

    if (apiKey == null || apiKey.isBlank()) {
      System.out.println("❌ Error: OPENROUTER_API_KEY not found in .env file");
      return;
    }

    System.out.println("\n🤖 Example 16: Agent with Memory");
    System.out.println("─".repeat(40));

    // Create an in-memory store
    InMemoryMemory memory = InMemoryMemory.create();
    String userId = "user-123"; // All memory operations are user-scoped

    // Add some memories for this user
    memory.add(userId, MemoryEntry.of("User prefers responses in Spanish."));
    memory.add(userId, MemoryEntry.of("User's name is Carlos."));
    memory.add(userId, MemoryEntry.of("User is interested in machine learning."));

    System.out.println("📝 Stored memories for user '" + userId + "':");
    memory.all(userId).forEach(entry -> System.out.println("   • " + entry.content()));

    // Retrieve relevant memories based on a keyword query
    System.out.println("\n🔍 Querying for 'name':");
    List<MemoryEntry> relevant = memory.retrieve(userId, "name", 2);
    relevant.forEach(entry -> System.out.println("   • " + entry.content()));

    // You can inject these memories into agent instructions or context
    System.out.println("\n💡 These memories can be injected into agent instructions or context.");

    // Demonstrate memory clear
    System.out.println("\n🗑️ Clearing all memories for user...");
    memory.clear(userId);
    System.out.println("   Memory size after clear: " + memory.size(userId));
  }
}
