package com.paragon.responses.spec;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.NonNull;

/**
 * Represents an immutable 2D coordinate using the Flyweight design pattern.
 *
 * <p>This class implements the Flyweight pattern to minimize memory usage when working with large
 * numbers of coordinate objects. Instead of creating new coordinate instances for every request,
 * the factory maintains a pool of shared instances and reuses them when the same coordinates are
 * requested.
 *
 * <p>Key characteristics:
 *
 * <ul>
 *   <li>Immutable - coordinates cannot be changed after creation
 *   <li>Thread-safe for reading (immutable state)
 *   <li>Private constructor - instances can only be created via Factory
 *   <li>Shared instances - identical coordinates reference the same object
 * </ul>
 *
 * <p>Usage example:
 *
 * <pre>
 * Coordinate coord1 = Coordinate.Factory.getCoordinate(10, 20);
 * Coordinate coord2 = Coordinate.Factory.getCoordinate(10, 20);
 * // coord1 == coord2 (same object reference)
 * </pre>
 *
 * <p>Jackson serialization:
 *
 * <ul>
 *   <li>When used with @JsonUnwrapped, x and y fields appear at the same level as other fields
 *   <li>Both x and y are required fields - deserialization fails with descriptive error if either
 *       is missing
 * </ul>
 *
 * @param x The x-coordinate value (immutable, required for deserialization)
 * @param y The y-coordinate value (immutable, required for deserialization)
 * @author Your Name
 * @version 1.0
 */
public record Coordinate(
    @JsonProperty(required = true) int x, @JsonProperty(required = true) int y) {

  /**
   * Private constructor to prevent direct instantiation. Coordinates should only be created through
   * the Factory.
   *
   * @param x the x-coordinate value
   * @param y the y-coordinate value
   */
  public Coordinate {}

  /**
   * Returns the x-coordinate value.
   *
   * @return the x-coordinate
   */
  @Override
  public int x() {
    return x;
  }

  /**
   * Returns the y-coordinate value.
   *
   * @return the y-coordinate
   */
  @Override
  public int y() {
    return y;
  }

  /**
   * Returns a string representation of this coordinate in the format "(x, y)".
   *
   * @return string representation of the coordinate
   */
  @Override
  public @NonNull String toString() {
    return "(" + x + ", " + y + ")";
  }

  /**
   * Factory class that implements the Flyweight pattern for Coordinate objects.
   *
   * <p>The Factory maintains a pool of Coordinate instances and ensures that identical coordinates
   * share the same object reference. This significantly reduces memory consumption when working
   * with many coordinate objects, especially when there are many duplicate coordinate values.
   *
   * <p>The factory is thread-safe for the getCoordinate operation but the pool management
   * operations (getPoolSize, clearPool) should be used with caution in multi-threaded environments.
   *
   * <p>Memory benefits:
   *
   * <ul>
   *   <li>Without Flyweight: 1,000,000 coordinates = ~16MB (assuming 16 bytes per object)
   *   <li>With Flyweight: 1,000,000 coordinates with 1,000 unique values = ~16KB
   * </ul>
   */
  public static class Factory {

    /**
     * Thread-safe pool of shared Coordinate instances. Key format: "x,y" where x and y are
     * coordinate values.
     */
    private static final Map<String, Coordinate> pool = new HashMap<>();

    /**
     * Returns a Coordinate instance for the given x and y values.
     *
     * <p>If a Coordinate with these exact values already exists in the pool, the existing instance
     * is returned. Otherwise, a new Coordinate is created, added to the pool, and returned.
     *
     * <p>This method is synchronized to ensure thread-safety when multiple threads request
     * coordinates simultaneously.
     *
     * @param x the x-coordinate value
     * @param y the y-coordinate value
     * @return a shared Coordinate instance with the specified values
     */
    public static synchronized Coordinate getCoordinate(int x, int y) {
      String key = x + "," + y;

      // Return existing coordinate if available in the pool
      if (pool.containsKey(key)) {
        return pool.get(key);
      }

      // Create new coordinate and add to pool for future reuse
      Coordinate coord = new Coordinate(x, y);
      pool.put(key, coord);
      return coord;
    }

    /**
     * Returns the current number of unique Coordinate instances in the pool.
     *
     * <p>This can be useful for monitoring memory usage and understanding how many unique
     * coordinates have been created.
     *
     * @return the number of Coordinate instances currently in the pool
     */
    public static int getPoolSize() {
      return pool.size();
    }

    /**
     * Clears all Coordinate instances from the pool.
     *
     * <p>Warning: This should be used with caution. After clearing the pool, any existing
     * Coordinate references will still be valid, but new requests for the same coordinates will
     * create new instances rather than reusing existing ones.
     *
     * <p>Use this method when you need to free memory and are certain that you won't need the
     * cached coordinates anymore.
     */
    public static void clearPool() {
      pool.clear();
    }
  }
}
