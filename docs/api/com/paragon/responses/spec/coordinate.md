# :material-database: Coordinate

> This docs was updated at: 2026-02-23

`com.paragon.responses.spec.Coordinate` &nbsp;·&nbsp; **Record**

---

Represents an immutable 2D coordinate using the Flyweight design pattern.

This class implements the Flyweight pattern to minimize memory usage when working with large
numbers of coordinate objects. Instead of creating new coordinate instances for every request,
the factory maintains a pool of shared instances and reuses them when the same coordinates are
requested.

Key characteristics:

  
- Immutable - coordinates cannot be changed after creation
- Thread-safe for reading (immutable state)
- Private constructor - instances can only be created via Factory
- Shared instances - identical coordinates reference the same object

Usage example:

```
Coordinate coord1 = Coordinate.Factory.getCoordinate(10, 20);
Coordinate coord2 = Coordinate.Factory.getCoordinate(10, 20);
// coord1 == coord2 (same object reference)
```

Jackson serialization:
- When used with @JsonUnwrapped, x and y fields appear at the same level as other fields
- Both x and y are required fields - deserialization fails with descriptive error if either
      is missing

## Fields

### `Coordinate`

```java
public Coordinate
```

Private constructor to prevent direct instantiation. Coordinates should only be created through
the Factory.

**Parameters**

| Name | Description |
|------|-------------|
| `x` | the x-coordinate value |
| `y` | the y-coordinate value |

---

### `pool`

```java
private static final Map<String, Coordinate> pool = new HashMap<>()
```

Thread-safe pool of shared Coordinate instances. Key format: "x,y" where x and y are
coordinate values.

## Methods

### `x`

```java
public int x()
```

Returns the x-coordinate value.

**Returns**

the x-coordinate

---

### `y`

```java
public int y()
```

Returns the y-coordinate value.

**Returns**

the y-coordinate

---

### `toString`

```java
public @NonNull String toString()
```

Returns a string representation of this coordinate in the format "(x, y)".

**Returns**

string representation of the coordinate

---

### `getCoordinate`

```java
public static synchronized Coordinate getCoordinate(int x, int y)
```

Returns a Coordinate instance for the given x and y values.

If a Coordinate with these exact values already exists in the pool, the existing instance
is returned. Otherwise, a new Coordinate is created, added to the pool, and returned.

This method is synchronized to ensure thread-safety when multiple threads request
coordinates simultaneously.

**Parameters**

| Name | Description |
|------|-------------|
| `x` | the x-coordinate value |
| `y` | the y-coordinate value |

**Returns**

a shared Coordinate instance with the specified values

---

### `getPoolSize`

```java
public static int getPoolSize()
```

Returns the current number of unique Coordinate instances in the pool.

This can be useful for monitoring memory usage and understanding how many unique
coordinates have been created.

**Returns**

the number of Coordinate instances currently in the pool

---

### `clearPool`

```java
public static void clearPool()
```

Clears all Coordinate instances from the pool.

Warning: This should be used with caution. After clearing the pool, any existing
Coordinate references will still be valid, but new requests for the same coordinates will
create new instances rather than reusing existing ones.

Use this method when you need to free memory and are certain that you won't need the
cached coordinates anymore.
