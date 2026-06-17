# Prototype Pattern

## 1. Title & Overview

The **Prototype** pattern creates new objects by copying an existing object, called the prototype, rather than by
calling a constructor. It solves the problem of expensive object creation when initialization is costly or when object
types must be determined at runtime. **One-line interview answer**: Prototype avoids costly creation by cloning
pre-configured prototypes, using either Java's `Cloneable`/`clone()` or copy constructors/factory methods.

## 2. Problem Statement

### Real-World Scenario

A graphics editor allows users to create complex shapes — circles, rectangles, bezier curves — each with custom styles (
color, gradient, stroke width, opacity, shadow). Creating a new shape from scratch every time the user copies and pastes
requires parsing SVG definitions, allocating GPU resources, loading textures, computing bounding boxes, and rendering
the shape to a back buffer. For 100 copy-paste operations, this is 100x the full initialization cost.

### Why This Fails at Scale

In a game engine, spawning enemy characters involves loading 3D models from disk, compiling shaders, initializing
physics bodies, generating animation controllers, allocating AI behavior trees, and populating inventory with
procedurally generated items. Each enemy type (soldier, mage, archer) has a unique combination. If the player presses "
summon 10 soldiers" and each is created via `new Soldier()` calling a complex constructor chain, frame rate drops
catastrophically. Object creation must be near-instantaneous.

### Pain Points of Naive Approach

- **Expensive Construction**: Complex objects require DB lookups, file I/O, network calls, or heavy computation during
  construction
- **Runtime Type Uncertainty**: Client code doesn't know concrete types until runtime (loaded from config, user data, or
  plugin)
- **Subclass Explosion**: Creating a factory hierarchy for each variant of every object leads to class explosion
- **Deep Object Setup**: Nested objects must each be configured — repeating the same configuration per instance
- **Construction in Hot Paths**: Object creation in game loops, request handlers, or tight loops creates GC pressure and
  latency

## 3. Solution

### How It Works

The Prototype pattern maintains a registry of pre-configured prototype instances. When a new object is needed, the
client requests a clone of the appropriate prototype. Cloning (typically via `clone()`, copy constructor, or
serialization) is faster than full construction because it copies the already-initialized internal state instead of
rebuilding it from scratch.

### Key Participants

```
┌───────────────────────────┐
│       Prototype            │
│      (interface)           │
├───────────────────────────┤
│ + clone(): Prototype       │
└───────────────────────────┘
         ▲
         │ implements
┌───────────────────────────┐
│   ConcretePrototype1       │    ┌───────────────────┐
│   ConcretePrototype2       │    │   PrototypeRegistry│
├───────────────────────────┤    ├───────────────────┤
│ + clone(): Prototype       │    │ + getPrototype()  │
│   (copy of self)           │    │ + register()      │
└───────────────────────────┘    └───────────────────┘
```

- **Prototype**: Interface declaring the `clone()` method
- **ConcretePrototype**: Implements cloning by copying its internal state
- **Client**: Creates new objects by asking a prototype to clone itself
- **PrototypeRegistry** (optional): Stores pre-configured prototypes, keyed by type/name for easy retrieval

### Step-by-Step Flow

1. **Setup**: One or more prototype objects are created and configured (possibly at startup)
2. **Client** needs a new object — calls `prototypeRegistry.get("soldier").clone()`
3. The **ConcretePrototype** creates a copy of itself (shallow or deep depending on requirements)
4. **Client** receives the clone — a fully initialized object with all baseline configuration
5. **Client** may customize the clone further (change some fields)
6. The prototype itself remains untouched — ready for the next clone request

## 4. Java Implementation

### Basic Prototype with Cloneable and Shallow Copy

```java
package creational.prototype;

import java.util.Objects;

interface Shape {
    Shape clone();
    void draw();
    String getDetails();
}

abstract class AbstractShape implements Shape {
    protected String color;
    protected int x;
    protected int y;

    public AbstractShape(String color, int x, int y) {
        this.color = color;
        this.x = x;
        this.y = y;
    }

    // Copy constructor — used by clone implementations
    public AbstractShape(AbstractShape source) {
        this.color = source.color;
        this.x = source.x;
        this.y = source.y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractShape that = (AbstractShape) o;
        return x == that.x && y == that.y && Objects.equals(color, that.color);
    }

    @Override
    public int hashCode() {
        return Objects.hash(color, x, y);
    }
}

class Circle extends AbstractShape {
    private int radius;

    public Circle(String color, int x, int y, int radius) {
        super(color, x, y);
        this.radius = radius;
    }

    // Copy constructor for cloning
    public Circle(Circle source) {
        super(source);
        this.radius = source.radius;
    }

    @Override
    public Shape clone() {
        // Delegate to copy constructor — this is the Prototype pattern
        return new Circle(this);
    }

    @Override
    public void draw() {
        System.out.printf("Circle [color=%s, center=(%d,%d), radius=%d]%n",
                color, x, y, radius);
    }

    @Override
    public String getDetails() {
        return "Circle(" + color + ", r=" + radius + ")";
    }

    public void setRadius(int radius) { this.radius = radius; }
    public int getRadius() { return radius; }
}

class Rectangle extends AbstractShape {
    private int width;
    private int height;

    public Rectangle(String color, int x, int y, int width, int height) {
        super(color, x, y);
        this.width = width;
        this.height = height;
    }

    public Rectangle(Rectangle source) {
        super(source);
        this.width = source.width;
        this.height = source.height;
    }

    @Override
    public Shape clone() {
        return new Rectangle(this);
    }

    @Override
    public void draw() {
        System.out.printf("Rectangle [color=%s, pos=(%d,%d), size=%dx%d]%n",
                color, x, y, width, height);
    }

    @Override
    public String getDetails() {
        return "Rectangle(" + color + ", " + width + "x" + height + ")";
    }

    public void setDimensions(int width, int height) {
        this.width = width;
        this.height = height;
    }
}
```

### Deep Copy — Copy Constructor Pattern

```java
package creational.prototype;

import java.util.*;

class ProductPrototype implements Cloneable {
    private String name;
    private double price;
    private List<String> tags;
    private Map<String, String> attributes;

    public ProductPrototype(String name, double price) {
        this.name = name;
        this.price = price;
        this.tags = new ArrayList<>();
        this.attributes = new HashMap<>();
    }

    // Copy constructor (preferred over Cloneable)
    public ProductPrototype(ProductPrototype source) {
        this.name = source.name;
        this.price = source.price;
        this.tags = new ArrayList<>(source.tags);
        this.attributes = new HashMap<>(source.attributes);
    }

    // Cloneable-based (requires fixing mutable fields after super.clone())
    @Override
    @SuppressWarnings("unchecked")
    public ProductPrototype clone() {
        try {
            ProductPrototype cloned = (ProductPrototype) super.clone();
            cloned.tags = new ArrayList<>(this.tags);
            cloned.attributes = new HashMap<>(this.attributes);
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    public ProductPrototype copy() { return new ProductPrototype(this); }

    public void addTag(String tag) { tags.add(tag); }
    public List<String> getTags() { return tags; }
}

public class DeepCopyDemo {
    public static void main(String[] args) {
        ProductPrototype original = new ProductPrototype("Laptop", 999.99);
        original.addTag("electronics");
        original.addTag("sale");

        ProductPrototype clone = original.copy();
        clone.addTag("clearance");

        System.out.println("Original tags: " + original.getTags());   // [electronics, sale]
        System.out.println("Clone tags:    " + clone.getTags());      // [electronics, sale, clearance]
        System.out.println("Deep copy: " + (original.getTags().size() == 2));
    }
}
```

### Prototype Registry with Configurable Prototypes

```java
package creational.prototype;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Registry that stores and dispenses prototypes
class ShapeRegistry {
    private static final Map<String, Shape> prototypes = new ConcurrentHashMap<>();

    // Register a prototype — called during initialization
    public static void register(String key, Shape prototype) {
        prototypes.put(key, prototype);
    }

    // Create a new object by cloning the registered prototype
    public static Shape createShape(String key) {
        Shape prototype = prototypes.get(key);
        if (prototype == null) {
            throw new IllegalArgumentException("Unknown shape: " + key);
        }
        return prototype.clone();
    }

    // Remove a prototype
    public static void unregister(String key) {
        prototypes.remove(key);
    }

    // Get the count of registered prototypes
    public static int registeredCount() {
        return prototypes.size();
    }
}

// Usage with registry
public class PrototypeRegistryDemo {
    public static void main(String[] args) {
        // ─── Setup: Create and configure prototypes once ───
        ShapeRegistry.register("red_circle", new Circle("red", 0, 0, 10));
        ShapeRegistry.register("blue_rectangle", new Rectangle("blue", 0, 0, 100, 50));
        ShapeRegistry.register("big_circle", new Circle("transparent", 400, 300, 200));

        // ─── Runtime: Clone shapes without knowing concrete classes ───
        List<Shape> shapes = new java.util.ArrayList<>();

        // Clone from registry — no constructors called, no concrete classes referenced
        shapes.add(ShapeRegistry.createShape("red_circle"));
        shapes.add(ShapeRegistry.createShape("red_circle"));  // Second clone
        shapes.add(ShapeRegistry.createShape("blue_rectangle"));
        shapes.add(ShapeRegistry.createShape("big_circle"));

        // Customize clones
        Shape customCircle = ShapeRegistry.createShape("red_circle");
        if (customCircle instanceof Circle c) {
            c.setRadius(25);  // Each clone is independent
        }
        shapes.add(customCircle);

        // Verify all objects
        shapes.forEach(Shape::draw);

        System.out.println("\nAll shapes are independent instances:");
        System.out.println("Shape 1 == Shape 2? " + (shapes.get(0) == shapes.get(1))); // false

        // ─── Dynamic registration at runtime ───
        Circle goldenCircle = new Circle("gold", 50, 50, 30);
        ShapeRegistry.register("golden_circle", goldenCircle);
        Shape golden = ShapeRegistry.createShape("golden_circle");
        golden.draw();
    }
}
```

### Serialization-Based Deep Clone

```java
package creational.prototype;

import java.io.*;

class Employee implements Serializable {
    private String name;
    private java.util.List<String> skills = new java.util.ArrayList<>();

    public Employee(String name) { this.name = name; }

    public Employee deepClone() {
        try {
            var bos = new ByteArrayOutputStream();
            var oos = new ObjectOutputStream(bos);
            oos.writeObject(this);
            var bis = new ByteArrayInputStream(bos.toByteArray());
            return (Employee) new ObjectInputStream(bis).readObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void addSkill(String s) { skills.add(s); }
    public int skillCount() { return skills.size(); }
}

public class SerializationPrototypeDemo {
    public static void main(String[] args) {
        Employee e1 = new Employee("Alice");
        e1.addSkill("Java");
        Employee e2 = e1.deepClone();
        e2.addSkill("Python");
        System.out.println("e1 skills: " + e1.skillCount()); // 1
        System.out.println("e2 skills: " + e2.skillCount()); // 2
    }
}
```

## 5. When to Use

### Framework & Library Examples

| Framework                 | Usage                                                                 |
|---------------------------|-----------------------------------------------------------------------|
| **Java `Object.clone()`** | Native cloning via `Cloneable` marker interface                       |
| **Spring**                | `Prototype` bean scope creates new instances; `@Scope("prototype")`   |
| **Java Arrays**           | `Arrays.copyOf()` — prototype-like copying of arrays                  |
| **Java Collections**      | `new ArrayList<>(existing)` — copy constructor pattern                |
| **Java Graphics2D**       | `Graphics2D.create()` — creates a copy of the graphics context        |
| **Protobuf**              | `Builder.mergeFrom()` — copying messages                              |
| **Clojure**               | Persistent data structures rely on structural sharing (not full copy) |

### Real-World Scenarios

1. **Game Object Spawning**: Each enemy, weapon, or particle effect has a prototype pre-configured with textures,
   animations, stats, and AI behaviors. Cloning is near-instant, enabling horde-mode gameplay with hundreds of
   simultaneous entities.

2. **Document Editing (Copy-Paste)**: In Word or Google Docs, copying a complex element (table with styles, embedded
   chart, equation) deep-clones the internal object graph. The prototype for each element type is the original document
   element.

3. **Configuration Templates**: Microservice configurations have base prototypes (e.g., "java-service", "
   python-worker", "cron-job") that are cloned and customized per deployment. Avoids repeating 80% of config.

4. **Database Record Caching**: Expensive database query results are cached as prototype objects. Each request clones
   the cached result and filters/customizes it, avoiding repeated queries.

5. **UI Component Theming**: A button prototype with base styles (rounded, blue, shadow) is cloned for each button
   instance. Customizations (text, width, action) are applied to the clone without re-running expensive style
   computation.

## 6. When NOT to Use

### Over-Engineering Warning

If object creation is cheap (`new SimpleObject()` with trivial fields), a copy constructor is more complex and slower
than direct construction. Prototype is only valuable when construction is significantly more expensive than cloning.

### Simpler Alternatives

- **Copy Constructors**: `new Product(existing)` — simpler than implementing `Cloneable`, no marker interface needed.
- **Static Factory + Builder**: For complex construction, a Builder is often clearer than cloning a prototype.
- **New with Defaults**: `new Product("defaults")` and then setting fields — simpler for objects with few fields.

### Performance Considerations

- **Cloneable.clone()**: Shallow copy is fast (field-by-field copy), but deep copy is slower per nested object level
- **Serialization Clone**: Very slow (byte stream serialization/deserialization). Only for deeply nested graphs where
  custom copy constructors would be tedious
- **Copy Constructor**: Fastest deep copy approach — similar performance to direct construction
- **GC Impact**: Freed clone objects create pressure — pooling helps if clones are short-lived

### Cloneable Gotchas in Java

- **No `clone()` in the interface**: `Cloneable` is a marker interface — `clone()` is inherited from `Object` and is
  `protected`
- **Shallow by default**: `super.clone()` copies primitives and references but not referenced objects
- **No constructor called**: `clone()` creates objects without calling constructors — breaks `final` field guarantees in
  some cases
- **Cannot clone final fields**: If fields are `final`, `clone()` may fail because it uses field-by-field copy
- **Array covariance issues**: Cloning an array of `String[]` returns `Object[]` — requires explicit cast

### When Not to Use Checklist

- □ Object creation is trivial (direct construction is as fast as cloning)
- □ Objects have many `final` fields (clone() cannot reassign final fields)
- □ Using records (records are inherently shallow-immutable — no need for clone)
- □ You need controlled construction (constructors enforce invariants; clone() bypasses them)
- □ Deep copy of complex graphs with circular references (serialization handles this, but it's slow)
- □ Framework manages object lifecycle (Spring singletons, Protobuf builders)

## 7. Interview Questions

**Q1: Explain the difference between shallow copy and deep copy in Prototype.**

A1: Shallow copy copies primitive fields and references — the original and clone share mutable referenced objects. Deep
copy creates independent copies of all mutable objects in the object graph. Shallow copy is faster but risks unintended
mutations through shared references. Deep copy is safer but requires more code (copy constructors, serialization, or
cloning each mutable field).

**Q2: Why is `Cloneable` considered broken in Java?**

A2: `Cloneable` is a marker interface with no `clone()` method — `clone()` is inherited from `Object` (protected). The
contract is enforced by the JVM at runtime: if an object doesn't implement `Cloneable`, `super.clone()` throws
`CloneNotSupportedException`. There's no compile-time safety. Additionally, `clone()` bypasses constructors, which can
break invariants and `final` field guarantees. Many experts (including Bloch) recommend copy constructors or factory
methods instead.

**Q3: How does the Prototype pattern differ from using a Builder?**

A3: Prototype creates objects by cloning an existing instance — best when you have a pre-configured template and most
objects are similar with minor variations. Builder constructs objects from scratch through step-by-step configuration —
best when each object is substantially different or when you need fine-grained control over the construction process.

**Q4: How would you implement Prototype for an object with circular references?**

A4: Custom copy constructors with careful handle-body separation or using serialization-based cloning (which handles
cyclic graphs via the ObjectOutputStream cycle detection). Serialization is the simplest approach for complex graphs,
though it is the slowest. A third option is a copy-constructor that uses a visitor pattern with an identity map to track
already-copied objects.

**Q5: When is the Prototype Registry useful?**

A5: When the set of available prototypes is determined at runtime (loaded from config, database, or plugins). The
registry maps string keys (or enum values) to prototype instances. Clients ask the registry for a clone by key, never
needing to know the concrete class. This is common in game engines, templating systems, and plugin architectures.

**Q6: What are the thread-safety considerations for Prototype?**

A6: The prototype object itself must be thread-safe if registered once and cloned by multiple threads. Options: (1)
immutable prototypes — always safe. (2) `ConcurrentHashMap` for the registry. (3) Copy-on-clone pattern — even if the
prototype has mutable state, the clone is independent, so subsequent mutations don't affect other clones or the
prototype.

**Q7: How does Java 17+ records simplify Prototype?**

A7: Records are shallowly immutable — all fields are `final` and automatically derived from the canonical constructor.
They provide a built-in copy constructor via the `with` pattern (using `RecordComponent` accessors). For immutable
prototypes, no cloning code is needed — just construct new instances. For mutable prototypes, records are not suitable —
use regular classes.

**Q8: Explain the Prototype pattern's use in the Composite pattern.**

A8: In a Composite tree (e.g., a document with paragraphs, tables, images), cloning the tree via Prototype lets you
deep-copy the entire structure without knowing the concrete types of each node. Each node implements `clone()`. This
powers copy-paste in document editors — selecting a complex table and pasting it deep-clones the entire composite tree
in O(n) time.

## 8. Pros & Cons

### Advantages

- **Reduced Construction Cost**: Cloning is often faster than full initialization (especially with expensive setup like
  DB lookups or file I/O)
- **Hides Concrete Classes**: Client code clones from a registry by key/type — no dependency on concrete classes
- **Dynamic Configuration**: New object types can be added at runtime by registering new prototypes
- **Reduced Subclassing**: Avoids creating a parallel factory hierarchy for each product variant
- **Complex Object Caching**: Expensive-to-create objects are built once and cloned repeatedly
- **Structural Copying**: Naturally copies entire object graphs (with deep clone), preserving internal relationships

### Disadvantages

- **Cloneable Is Broken**: Java's `Cloneable` is error-prone — no `clone()` in the interface, bypasses constructors,
  shallow by default
- **Deep Copy Complexity**: Deep cloning mutable nested objects requires careful code (copy constructors) or slow
  serialization
- **Circular References**: Deep copying object graphs with cycles requires special handling (identity maps or
  serialization)
- **Final Field Issues**: `clone()` cannot reassign `final` fields — incompatible with some immutable object designs
- **Maintenance Burden**: Every time a new field is added to a class, the clone/copy code must be updated
- **Overhead for Simple Objects**: Cloning a simple object is slower than `new` — the copy overhead outweighs any
  benefit

## 9. Related Patterns

| Pattern              | Relationship                                                                                                                                                                                          |
|----------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Abstract Factory** | An alternative to Prototype for creating families of related objects. Abstract Factory uses factory methods; Prototype uses cloning. Often, a registry of prototypes can replace an Abstract Factory. |
| **Composite**        | Prototype is used to clone composite structures. The `clone()` method on each node recursively clones its children, making it trivial to deep-copy entire trees.                                      |
| **Command**          | Prototype can be used to clone Command objects for undo/redo stacks — commands are configured once and cloned when queued.                                                                            |
| **Memento**          | Prototype can serve as a simple Memento implementation — clone the originator's state to capture a snapshot.                                                                                          |

### How to Choose

- Use **Prototype** when object creation is expensive and objects are similar to existing ones
- Use **Abstract Factory** (or Factory Method) when you control construction and need family consistency
- Use **Builder** when objects have many optional parameters and varying construction steps
- Use **Copy Constructor** (simpler than full Prototype) when you just need a copy of one object type
- Use **Prototype Registry** when the set of object types is dynamic and discovered at runtime

## 10. Key Takeaways

- **Clone, don't construct**: Prototype avoids expensive construction by copying pre-configured instances
- **Dynamic object types**: Registry-based Prototype lets you add new types at runtime without changing code
- **Java's Cloneable is flawed**: Prefer copy constructors or a dedicated `copy()` method over `Cloneable`
- **Shallow vs deep is THE design decision**: Choose based on whether mutable objects should be shared or independent
- **Interview memory aid**: "Prototype = clone instead of new. Use when construction is expensive or types are
  runtime-dynamic. Prefer copy constructors over Cloneable. Deep copy mutable fields."
