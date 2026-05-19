# Singleton Pattern

> Ensure a class has only one instance and provide global access.

## Why?

When exactly one object is needed to coordinate actions across the system.

## Where?

- Configuration managers
- Connection pools
- Thread pools
- Cache managers
- Logger (single output destination)

## Implementations

### 1. Eager Initialization (Thread-safe, simple)
```java
public class AppConfiguration {
    private static final AppConfiguration INSTANCE = new AppConfiguration();
    private AppConfiguration() {}  // Private constructor
    public static AppConfiguration getInstance() { return INSTANCE; }
}
```

### 2. Lazy Initialization (Thread-safe, double-checked locking)
```java
public class ConnectionPool {
    private static volatile ConnectionPool instance;
    private ConnectionPool() {}

    public static ConnectionPool getInstance() {
        if (instance == null) {
            synchronized (ConnectionPool.class) {
                if (instance == null) {
                    instance = new ConnectionPool();
                }
            }
        }
        return instance;
    }
}
```

### 3. Bill Pugh (Recommended — thread-safe, lazy, no synchronization)
```java
public class Logger {
    private Logger() {}

    private static class Holder {
        static final Logger INSTANCE = new Logger();
    }

    public static Logger getInstance() { return Holder.INSTANCE; }
}
```

### 4. Enum Singleton (Effective Java recommendation)
```java
public enum DatabaseManager {
    INSTANCE;
    public void connect() { /* ... */ }
}
```

## Interview Considerations

| Concern | Solution |
|---------|----------|
| Thread safety | Bill Pugh or Enum |
| Serialization | Enum handles it; otherwise implement `readResolve()` |
| Reflection attack | Throw in constructor if `instance != null` |
| Testing | Singletons are hard to mock — consider DI instead |

## When NOT to Use

- When DI can provide the same scoping
- When you need multiple instances in tests
- When global state makes testing difficult
