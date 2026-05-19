# Proxy Pattern

> Control access to an object. Add a layer of indirection.

## Why?

You need to add behavior (caching, access control, lazy loading) without changing the original object.

## Where?

- **Spring AOP**: Proxies add transaction management, security
- **Hibernate**: Lazy-loaded proxies for entity relationships
- **RMI/gRPC**: Remote proxies for network calls
- **CDN**: Proxy for static assets

## Types

### 1. Virtual Proxy (Lazy Loading)
```java
interface Image { void display(); }

class RealImage implements Image {
    private final String filename;
    RealImage(String f) { this.filename = f; loadFromDisk(); }
    private void loadFromDisk() { System.out.println("Loading " + filename); }
    public void display() { System.out.println("Displaying " + filename); }
}

class ProxyImage implements Image {
    private RealImage realImage;
    private final String filename;
    ProxyImage(String f) { this.filename = f; }
    public void display() {
        if (realImage == null) {
            realImage = new RealImage(filename);  // Load on demand
        }
        realImage.display();
    }
}
```

### 2. Protection Proxy (Access Control)
```java
class ProtectedDocument implements Document {
    private final Document real;
    private final String owner;
    ProtectedDocument(Document real, String owner) { this.real = real; this.owner = owner; }
    public void read(String user) {
        if (!owner.equals(user)) throw new SecurityException("Access denied");
        real.read(user);
    }
}
```

### 3. Caching Proxy
```java
class CachedUserService implements UserService {
    private final UserService real;
    private final Map<String, User> cache = new ConcurrentHashMap<>();
    CachedUserService(UserService real) { this.real = real; }
    public User getUser(String id) {
        return cache.computeIfAbsent(id, real::getUser);
    }
}
```

## Decorator vs Proxy

| Aspect | Decorator | Proxy |
|--------|-----------|-------|
| Intent | Add behavior | Control access |
| Composition | Multiple decorators | Single proxy |
| Object lifecycle | Object exists | May create object lazily |
