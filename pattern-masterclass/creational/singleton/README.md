# Singleton Pattern

## 1. Title & Overview

The **Singleton** pattern ensures a class has only one instance and provides a global point of access to it. It solves
the problem of controlling access to shared resources like configuration managers, connection pools, or logging services
where multiple instances would cause conflicts or wasted resources. **One-line interview answer**: Singleton restricts
instantiation to one object by making the constructor private and providing a static method that returns the single
instance, with thread-safety being the primary implementation concern.

## 2. Problem Statement

### Real-World Scenario

An application has multiple modules — HTTP API, background job processor, metrics collector — all reading from the same
configuration file. If each module creates its own `ConfigManager` instance, each parses the file independently.
Changing config at runtime (e.g., updating log level) requires notifying each instance. Memory is wasted, state
diverges, and race conditions occur when two instances write to the same file.

### Why This Fails at Scale

In a microservice handling 10,000 requests/second, each request creates a new `Logger` or `MetricsCollector` instance.
File handles leak, socket connections exhaust, and buffered log entries are lost when short-lived instances are garbage
collected. A connection pool with multiple instances creates more connections than the database allows, each thinking it
manages the pool independently — leading to `too many connections` errors and unpredictable behavior.

### Pain Points of Naive Approach

- **Multiple Instances**: Each module creates its own copy, violating the one-object constraint
- **State Inconsistency**: Same configuration, cache, or counter diverges across instances
- **Resource Exhaustion**: File handles, sockets, and connections multiplied unnecessarily
- **Leaking abstractions**: Clients must coordinate instance sharing manually
- **Testing Difficulty**: Tests sharing a mutable singleton state pollute each other
- **Race Conditions**: Unsynchronized singletons cause visibility issues in multi-threaded code

## 3. Solution

### How It Works

The Singleton pattern controls the constructor (made private), provides a static accessor method that returns the single
instance, and ensures the instance is created exactly once — either eagerly at class loading or lazily on first access.
Thread-safety mechanisms (synchronization, volatile, enums, inner classes) prevent race conditions during
initialization.

### Key Participants

```
┌──────────────────────────────┐
│         Singleton             │
├──────────────────────────────┤
│ - instance: Singleton         │◄─ static
│ - Singleton()                 │◄─ private constructor
├──────────────────────────────┤
│ + getInstance(): Singleton    │◄─ static accessor
│ + businessMethod()            │
└──────────────────────────────┘
```

- **Singleton**: The class that ensures only one instance exists
- **instance**: Private static variable holding the single instance
- **Singleton()**: Private constructor preventing external instantiation
- **getInstance()**: Public static method returning the single instance (creates it lazily if needed)

### Step-by-Step Flow

1. **Client** calls `Singleton.getInstance()` for the first time
2. For lazy initialization: the method checks if `instance == null` (with proper synchronization)
3. If null, creates the single instance via the private constructor
4. Returns the instance reference
5. **All subsequent calls** return the same instance — no new object created
6. If the singleton holds mutable state, access to that state must be thread-safe

## 4. Java Implementation

### Implementation 1: Eager Initialization (Simple & Thread-Safe)

```java
package creational.singleton;

public class EagerSingleton {
    // Instance created at class loading time — inherently thread-safe
    private static final EagerSingleton INSTANCE = new EagerSingleton();

    private EagerSingleton() {
        // Prevent reflection attacks
        if (INSTANCE != null) {
            throw new IllegalStateException("Singleton already initialized");
        }
    }

    public static EagerSingleton getInstance() {
        return INSTANCE;
    }

    // Business methods
    public void doSomething() {
        System.out.println("EagerSingleton instance: " + hashCode());
    }
}
```

**Pros**: Simple, inherently thread-safe, no synchronization overhead.
**Cons**: Instance created even if never used (waste if expensive to create).

### Implementation 2: Lazy Initialization with Double-Checked Locking (Java 5+)

```java
package creational.singleton;

public class LazySingleton {
    // volatile ensures visibility across threads (Java 5+ memory model)
    private static volatile LazySingleton instance;

    private LazySingleton() {
        // Guard against reflection
        if (instance != null) {
            throw new IllegalStateException("Use getInstance() to create");
        }
        System.out.println("LazySingleton created");
    }

    public static LazySingleton getInstance() {
        // First check — no synchronization overhead for reads
        if (instance == null) {
            // Class-level lock — only one thread enters
            synchronized (LazySingleton.class) {
                // Second check — ensures another thread didn't create it
                // while this thread was waiting for the lock
                if (instance == null) {
                    instance = new LazySingleton();
                }
            }
        }
        return instance;
    }
}
```

**Thread Safety Analysis**:

- `volatile` guarantees the `instance` variable is read from main memory, not thread-local cache
- First `if (instance == null)` avoids synchronization cost after initialization
- `synchronized` block ensures only one thread enters the creation path
- Second `if (instance == null)` prevents double creation (two threads could pass the first null check)

### Implementation 3: Bill Pugh Singleton (Inner Static Helper Class)

```java
package creational.singleton;

public class BillPughSingleton {
    // Private constructor
    private BillPughSingleton() {
        System.out.println("BillPughSingleton instance created");
    }

    // Static inner class — not loaded until getInstance() is called
    private static class SingletonHolder {
        // The JVM guarantees that class loading is thread-safe
        private static final BillPughSingleton INSTANCE = new BillPughSingleton();
    }

    public static BillPughSingleton getInstance() {
        // Accessing SingletonHolder triggers its class loading,
        // which creates the instance in a thread-safe manner
        return SingletonHolder.INSTANCE;
    }
}
```

**Why This Works**: The JVM defers loading of inner classes until they are referenced. `SingletonHolder` is loaded when
`getInstance()` calls it. Class loading is guaranteed by the JVM to be serial (thread-safe). No `synchronized`, no
`volatile` needed.

### Implementation 4: Enum Singleton (Best Practice — Joshua Bloch)

```java
package creational.singleton;

public enum EnumSingleton {
    INSTANCE;

    // Enum constructors are implicitly private
    EnumSingleton() {
        System.out.println("EnumSingleton initialized");
    }

    // Business methods
    public void doSomething() {
        System.out.println("EnumSingleton instance: " + hashCode());
    }

    // Example: configuration holder
    private String configValue = "default";

    public String getConfigValue() { return configValue; }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }
}
```

**Why Enum Singleton is the Best Approach**:

- **Thread-safe**: JVM guarantees enum constants are instantiated exactly once
- **Serialization-safe**: Java's enum serialization preserves the singleton guarantee automatically
- **Reflection-proof**: Enums cannot be instantiated via reflection (Enum constructor protected)
- **Concise**: Single line for the singleton — no boilerplate
- **Effective Java**: Joshua Bloch recommends it as the definitive Singleton approach

### Enum Singleton Test (Demonstrates Reflection Attack Prevention)

```java
package creational.singleton;

import java.lang.reflect.Constructor;

public class SingletonSecurityDemo {
    public static void main(String[] args) {
        // 1. Enum Singleton — secure
        EnumSingleton s1 = EnumSingleton.INSTANCE;
        EnumSingleton s2 = EnumSingleton.INSTANCE;
        System.out.println("Enum same? " + (s1 == s2)); // true

        // Attempt reflection on enum — will fail
        try {
            Constructor<?>[] ctors = EnumSingleton.class.getDeclaredConstructors();
            for (Constructor<?> ctor : ctors) {
                ctor.setAccessible(true);
                // This throws IllegalArgumentException: Cannot reflectively create enum objects
                EnumSingleton reflection = (EnumSingleton) ctor.newInstance();
            }
        } catch (Exception e) {
            System.out.println("Enum reflection prevented: " + e.getClass().getSimpleName());
        }

        // 2. LazySingleton — reflection vulnerable
        LazySingleton lazy1 = LazySingleton.getInstance();
        try {
            Constructor<LazySingleton> ctor = LazySingleton.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            LazySingleton reflection = ctor.newInstance();
            System.out.println("LazySingleton reflection succeeded? " + (lazy1 != reflection)); // true — broken!
        } catch (Exception e) {
            System.out.println("Prevented: " + e.getMessage());
        }
    }
}
```

### Thread-Safe Configuration Manager Example

```java
package creational.singleton;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public enum ConfigManager {
    INSTANCE;

    private final Map<String, String> config = new ConcurrentHashMap<>();

    ConfigManager() {
        // Simulate loading from application.properties
        loadDefaults();
    }

    private void loadDefaults() {
        // Thread-safe — enum constructor runs once, before any access
        config.put("app.name", "SingletonDemo");
        config.put("app.version", "1.0.0");
        config.put("db.url", "jdbc:postgresql://localhost:5432/app");
        config.put("db.pool.size", "10");
        config.put("log.level", "INFO");
    }

    public String get(String key) {
        return config.get(key);
    }

    public String get(String key, String defaultValue) {
        return config.getOrDefault(key, defaultValue);
    }

    public void set(String key, String value) {
        config.put(key, value);
    }

    public int getInt(String key, int defaultValue) {
        String val = config.get(key);
        return val != null ? Integer.parseInt(val) : defaultValue;
    }

    public void loadFromProperties(Properties props) {
        props.forEach((k, v) -> config.put(k.toString(), v.toString()));
    }
}
```

## 5. When to Use

### Framework & Library Examples

| Framework            | Usage                                                                               |
|----------------------|-------------------------------------------------------------------------------------|
| **Java Runtime**     | `Runtime.getRuntime()` — singleton representing the JVM instance                    |
| **Spring**           | Beans default to singleton scope; `ApplicationContext` acts as a singleton registry |
| **Logging**          | `Logger.getLogger()` returns a shared logger per class/name                         |
| **java.lang.System** | `System.in`, `System.out`, `System.err` — single instances for standard IO          |
| **Java Desktop**     | `java.awt.Toolkit.getDefaultToolkit()` — singleton for AWT resources                |
| **Android**          | `Application` class — singleton for app-level context                               |

### Real-World Scenarios

1. **Configuration Manager**: Load app config once, serve it throughout the application lifecycle. Any runtime config
   changes are immediately visible to all modules. Avoids redundant file I/O and parsing.

2. **Database Connection Pool**: A single pool manages a fixed set of connections. Multiple pools would exceed database
   connection limits. Singleton ensures centralized connection lifecycle management.

3. **Logging Framework**: `LoggerFactory` / `LogManager` is typically a singleton. Each logger is created once per name
   and shared across the application. Log level changes take effect globally.

4. **Cache Manager**: An in-memory cache (like Guava Cache or a custom LRU cache) should be shared across components.
   Separate caches would duplicate data and waste memory.

5. **Window/UI Controllers**: In desktop apps, main application window is a singleton. Dialogs that need the main window
   reference call `MainWindow.getInstance()`.

## 6. When NOT to Use

### The Global State Problem

Singleton is often called an "anti-pattern" because it introduces global state into applications. Global state:

- Makes unit testing difficult (state persists across tests)
- Creates hidden dependencies (classes using Singleton are coupled to the concrete class)
- Violates the Single Responsibility Principle (the class manages both its business logic and its instantiation control)

### Simpler Alternatives

- **Dependency Injection**: Instead of `ConfigManager.getInstance()`, inject `ConfigManager` via constructor. Spring
  manages a single instance (singleton scope) but the class itself is a regular POJO.
- **Static Utility Class**: For stateless helpers (like `Math`), static methods suffice — no instance needed.
- **Instance Caching in a Factory**: If you need only one instance but want flexibility later, a factory can enforce the
  singleton constraint without coupling the class itself.

### Additional Consideration: Clustered Environments

In a distributed system with multiple JVMs (microservices, clustered app servers), the singleton pattern only guarantees
one instance per JVM — not per cluster. Each JVM maintains its own singleton, potentially causing state divergence for
configuration caches or distributed counters.

**Solutions:**

- Redesign the singleton to be stateless (delegate to a distributed store like Redis)
- Use a distributed cache (Hazelcast, Redis) instead of local singleton state
- Accept per-JVM singletons if eventual consistency is sufficient

### Performance Considerations

- **Synchronization**: `synchronized getInstance()` is a bottleneck for all callers, even after initialization
- **False Sharing**: The singleton's state fields may share a CPU cache line with unrelated data, causing cache
  contention under high concurrency
- **Memory Model**: Without `volatile`, threads may see stale singleton state
- **Class Loading**: Eager singletons increase application startup time

### When Not to Use Checklist

- □ You need unit tests with fresh state (test pollution issue)
- □ The class has no mutable state (use static methods)
- □ You're using a DI container (let it manage the scope)
- □ The class has multiple responsibilities (split it first)
- □ You need multiple instances in some configurations (e.g., testing, multi-tenant)
- □ The Singleton creates hidden coupling in the codebase

## 7. Interview Questions

**Q1: What are the four ways to implement a Singleton in Java?**

A1: (1) Eager initialization — simple but creates instance at class load time. (2) Double-checked locking with
`volatile` — lazy, thread-safe, performant. (3) Bill Pugh inner static class — lazy, thread-safe via JVM class loading
guarantees. (4) Enum singleton (Bloch's recommendation) — simplest, serialization-safe, reflection-proof.

**Q2: Why is double-checked locking broken without `volatile` in Java?**

A2: Without `volatile`, the JIT compiler can reorder the writes in `instance = new LazySingleton()`. The constructor may
run before the reference is assigned, causing another thread to see a partially constructed object. Java 5+ `volatile`
fixes this by establishing a happens-before relationship.

**Q3: Why does Joshua Bloch recommend enum for Singleton?**

A3: Enum singletons are concise (one line), inherently thread-safe, automatically serializable (Java's enum
serialization preserves the singleton invariant), and immune to reflection attacks (the JVM forbids reflectively
creating enum instances). They are the most robust singleton implementation.

**Q4: How does serialization break a conventional singleton, and how do you fix it?**

A4: During deserialization, Java creates a new instance via `readObject()` — even with a private constructor. Fix by
implementing `readResolve()` to return the existing instance: `protected Object readResolve() { return instance; }`.
Enum singletons handle this automatically.

**Q5: Is Spring's default bean scope a singleton? How is it different from GoF Singleton?**

A5: Yes, Spring beans default to singleton scope. The key difference: Spring's singleton is per-ApplicationContext (not
per-JVM), and it does not enforce a private constructor — Spring creates one instance via reflection. The class itself
is a regular POJO, making it testable and decoupled from the singleton pattern.

**Q6: How do you test code that uses a Singleton?**

A6: Ideally, refactor to use dependency injection instead of calling `getInstance()` directly. If you must use
Singleton, provide a `setInstance()` or `reset()` method for testing (with a flag to disable in production). Extract an
interface from the singleton and mock it. The enum singleton can expose a `resetForTesting()` via a `synchronized`
method.

**Q7: Explain how ClassLoader can break a singleton.**

A7: If code is loaded by multiple ClassLoaders (e.g., in a Java EE container or with custom classloaders), each
ClassLoader loads its own copy of the singleton class, giving multiple instances. This applies to all singleton
implementations. Solutions: ensure singleton classes are in the shared (parent) classloader, or use a global registry
keyed by ClassLoader.

**Q8: What is the difference between a Singleton and a Monostate pattern?**

A8: Singleton restricts instantiation to one object; Monostate allows multiple instances but stores all state in static
fields — all instances share the same state. Singleton is enforced by the class design (private constructor), Monostate
by the developer's awareness. Monostate is often considered an anti-pattern as it subverts expectations.

## 8. Pros & Cons

### Advantages

- **Controlled Access**: Single point of access to a shared resource
- **Reduced Memory**: One instance instead of many duplicates
- **Consistent State**: All modules see the same data (config, cache counters)
- **Lazy Initialization**: Can defer expensive creation until first use
- **Global Coordination**: Connection pooling, logging, and metrics naturally need centralized coordination
- **Cross-cutting**: Cross-cutting concerns (logging, auditing, metrics) are natural fits

### Disadvantages

- **Global State**: Introduces hidden shared state, making code harder to reason about and test
- **Hidden Dependencies**: Classes using Singleton are coupled to the concrete class, not an interface
- **SRP Violation**: Manages both business logic and its own lifecycle
- **Thread-Safety Complexity**: Must be carefully implemented to avoid race conditions
- **Scalability**: Becomes a contention bottleneck in highly concurrent systems
- **Testing Difficulty**: Tests must be ordered or reset state between runs
- **Reflection Vulnerability**: Conventional singletons can be broken via reflection (enum is immune)

## 9. Related Patterns

| Pattern              | Relationship                                                                                              |
|----------------------|-----------------------------------------------------------------------------------------------------------|
| **Abstract Factory** | Concrete factories are often implemented as Singletons — only one factory instance is needed per variant. |
| **Facade**           | A Facade to a subsystem is often a Singleton to ensure consistent access.                                 |
| **Prototype**        | Registry of prototypes is often a Singleton (single registry for the application).                        |
| **Flyweight**        | Flyweight pools are managed by a Singleton registry to share lightweight objects.                         |

### How to Choose

- Use **Singleton** when you need exactly one instance of a class and global access
- Use **Dependency Injection** when you want single-instance but also want testability (let the container manage scope)
- Use **Monostate** when you want multiple instances with shared state (rarely recommended)
- Use **Static Utility Class** for stateless collections of helper methods (no instance needed at all)

## 10. Key Takeaways

- **One instance, global access**: The essence of Singleton — private constructor + static accessor
- **Thread safety is the hard part**: The creation method must be correct under concurrent access; enum is the safest
  approach
- **Serialization and reflection are attack vectors**: Enum handles both; conventional implementations need
  `readResolve()` and reflection guards
- **SOLID tension**: Singleton often violates SRP and DIP; mitigate with interface-based design and DI where possible
- **Interview memory aid**: "Singleton = private constructor + static getInstance. For thread safety: enum (best), inner
  class (good), double-checked locking (use volatile). Serialization: readResolve(). Reflection: use enum."
