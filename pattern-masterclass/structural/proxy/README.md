# Proxy Pattern (Surrogate Pattern)

## Overview

The **Proxy Pattern** provides a surrogate or placeholder for another object to control access to it. The proxy has the
same interface as the real subject, making it transparent to the client. **One-line interview answer**: The Proxy
pattern controls access to an object by acting as its stand-in, intercepting operations for lazy loading, access
control, logging, or remote communication while preserving the same interface.

---

## Problem Statement

### Real-World Scenario

You're building a document viewer that loads high-resolution images. A document can have 50+ images, but the user only
sees a few at a time. Loading all images at startup wastes memory (potentially gigabytes) and makes the app
unresponsive. The naive approach — eagerly loading all images — is unacceptable for resource-constrained environments
like mobile devices or browsers.

### Why This Matters in Production

Access control and resource management are universal concerns:

- **Lazy loading** — large objects (images, documents, datasets) that shouldn't be loaded until needed
- **Access control** — certain users shouldn't delete or modify objects
- **Caching** — expensive computations or remote calls should be cached transparently
- **Remote communication** — calling services across network boundaries
- **Logging / Auditing** — tracking every method invocation on sensitive objects

Without Proxy, these cross-cutting concerns are mixed into business logic, violating the Single Responsibility
Principle.

### Pain Points Without Proxy

- **Memory waste** — eagerly loading resources that may never be used
- **Scattered access control** — permission checks duplicated across every method
- **No caching** — repeated expensive calls for the same data
- **Tight coupling to remote APIs** — business code handles network failures, retries, serialization
- **Violation of SRP** — domain objects responsible for their own lazy loading or access control

---

## Solution

The Proxy pattern interposes an intermediary between the client and the real subject. The proxy implements the same
interface as the real subject, so the client is unaware it's talking to a proxy.

### Types of Proxies

| Type                 | Purpose                                                   | Example                              |
|----------------------|-----------------------------------------------------------|--------------------------------------|
| **Virtual Proxy**    | Lazy initialization — defers object creation until needed | Loading large images on demand       |
| **Protection Proxy** | Access control — checks permissions before delegating     | Role-based authorization             |
| **Remote Proxy**     | Local representative for a remote object                  | RMI stubs, gRPC clients              |
| **Cache Proxy**      | Stores results of expensive operations                    | Database query caching               |
| **Logging Proxy**    | Records all calls to the real subject                     | Audit trail for sensitive operations |

### Key Participants

| Role            | Description                                                             |
|-----------------|-------------------------------------------------------------------------|
| **Subject**     | Common interface for RealSubject and Proxy                              |
| **RealSubject** | The actual object that performs the real work                           |
| **Proxy**       | Maintains reference to RealSubject, implements Subject, controls access |
| **Client**      | Interacts with Subject interface, unaware of Proxy                      |

### Flow (Virtual Proxy)

```
Client
  │
  ▼
Proxy.imageData()
  │
  ├── RealSubject exists? ──yes──→ delegate to RealSubject
  │     │
  │    no
  │     ▼
  ├── Create RealSubject (expensive)
  ├── Load data from disk
  └── Delegate to RealSubject
```

---

## Java Implementation

### Common Interface (Subject)

```java
package structural.proxy;

public interface Image {
    void display();
    Dimension getDimension();
    byte[] getImageData();
}
```

### RealSubject

```java
package structural.proxy;

import java.awt.Dimension;

// The real object — expensive to create (loads image from disk/network)
public class RealImage implements Image {
    private final String filePath;
    private Dimension dimension;
    private byte[] imageData; // large byte array

    public RealImage(String filePath) {
        this.filePath = filePath;
        loadFromDisk();  // expensive operation in constructor
    }

    private void loadFromDisk() {
        System.out.println("Loading " + filePath + " from disk...");
        // Simulate loading: read file header for dimension, pixel data
        try { Thread.sleep(1500); } catch (InterruptedException ignored) { }
        this.dimension = new Dimension(1920, 1080);
        this.imageData = new byte[1024 * 1024 * 5]; // 5MB simulated
        System.out.println("Loaded " + filePath + " (" + imageData.length + " bytes)");
    }

    @Override
    public void display() {
        System.out.println("Displaying " + filePath);
    }

    @Override
    public Dimension getDimension() {
        return dimension;
    }

    @Override
    public byte[] getImageData() {
        return imageData;
    }
}
```

### Virtual Proxy — Lazy Loading

```java
package structural.proxy;

import java.awt.Dimension;

// Virtual Proxy: defers RealImage creation until display() is called
public class VirtualProxyImage implements Image {
    private final String filePath;
    private RealImage realImage; // initially null — lazy creation

    public VirtualProxyImage(String filePath) {
        this.filePath = filePath;
        // No disk loading here — constructor is lightweight
    }

    @Override
    public void display() {
        // Lazy initialization — first access creates the real object
        if (realImage == null) {
            realImage = new RealImage(filePath);
        }
        realImage.display();
    }

    @Override
    public Dimension getDimension() {
        if (realImage == null) {
            realImage = new RealImage(filePath);
        }
        return realImage.getDimension();
    }

    @Override
    public byte[] getImageData() {
        if (realImage == null) {
            realImage = new RealImage(filePath);
        }
        return realImage.getImageData();
    }
}
```

### Protection Proxy — Access Control

```java
package structural.proxy;

// Protection Proxy: checks authorization before delegating to RealSubject
public class ProtectionProxyImage implements Image {
    private final Image realImage;
    private final String userRole;

    public ProtectionProxyImage(Image realImage, String userRole) {
        this.realImage = realImage;
        this.userRole = userRole;
    }

    @Override
    public void display() {
        // Everyone can view images
        realImage.display();
    }

    @Override
    public java.awt.Dimension getDimension() {
        // Everyone can get dimensions
        return realImage.getDimension();
    }

    @Override
    public byte[] getImageData() {
        // Only ADMIN can download the raw image data
        if (!"ADMIN".equals(userRole)) {
            throw new SecurityException(
                "Access denied: only ADMIN can download image data. Role: " + userRole);
        }
        return realImage.getImageData();
    }
}
```

### Cache Proxy

```java
package structural.proxy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Cache Proxy: stores results of expensive getImageData() calls
public class CacheProxyImage implements Image {
    private final Image realImage;
    private static final Map<String, byte[]> cache = new ConcurrentHashMap<>();
    private final String cacheKey;

    public CacheProxyImage(Image realImage, String cacheKey) {
        this.realImage = realImage;
        this.cacheKey = cacheKey;
    }

    @Override
    public void display() {
        realImage.display(); // no caching needed for display
    }

    @Override
    public java.awt.Dimension getDimension() {
        return realImage.getDimension();
    }

    @Override
    public byte[] getImageData() {
        // Check cache first
        return cache.computeIfAbsent(cacheKey, k -> {
            System.out.println("Cache miss — loading image data for: " + cacheKey);
            return realImage.getImageData();
        });
    }
}
```

### Remote Proxy (Simulated)

```java
package structural.proxy;

import java.rmi.Remote;
import java.rmi.RemoteException;

// Note: Real RMI requires java.rmi package, this is a structural simulation

// Common remote interface
interface RemoteImageService extends Remote {
    byte[] fetchImage(String path) throws RemoteException;
}

// Remote Proxy — local object representing a remote image service
public class RemoteProxyImage implements Image {
    private final String filePath;
    private final RemoteImageService remoteService; // connection to remote server
    private byte[] cachedData;

    public RemoteProxyImage(String filePath, RemoteImageService remoteService) {
        this.filePath = filePath;
        this.remoteService = remoteService;
    }

    @Override
    public void display() {
        System.out.println("Remote display request for: " + filePath);
    }

    @Override
    public java.awt.Dimension getDimension() {
        return new java.awt.Dimension(1920, 1080); // metadata sent separately
    }

    @Override
    public byte[] getImageData() {
        if (cachedData == null) {
            try {
                System.out.println("Fetching " + filePath + " from remote server...");
                cachedData = remoteService.fetchImage(filePath);
            } catch (RemoteException e) {
                throw new RuntimeException("Failed to fetch remote image", e);
            }
        }
        return cachedData;
    }
}
```

### Logging Proxy

```java
package structural.proxy;

import java.time.Instant;

// Logging Proxy: records every method invocation for auditing/debugging
public class LoggingProxyImage implements Image {
    private final Image realImage;
    private final String imageId;

    public LoggingProxyImage(Image realImage, String imageId) {
        this.realImage = realImage;
        this.imageId = imageId;
    }

    private void log(String method) {
        System.out.printf("[%s] %s called on image %s%n",
            Instant.now(), method, imageId);
    }

    @Override
    public void display() {
        log("display");
        realImage.display();
    }

    @Override
    public java.awt.Dimension getDimension() {
        log("getDimension");
        return realImage.getDimension();
    }

    @Override
    public byte[] getImageData() {
        log("getImageData");
        return realImage.getImageData();
    }
}
```

### Usage Example

```java
package structural.proxy;

public class ProxyDemo {
    public static void main(String[] args) {
        // Virtual Proxy — images loaded only on demand
        Image[] documentImages = {
            new VirtualProxyImage("photo1.jpg"),
            new VirtualProxyImage("photo2.jpg"),
            new VirtualProxyImage("photo3.jpg")
        };

        // Only page 1's image is displayed — only that one is loaded
        System.out.println("=== Opening page 1 ===");
        documentImages[0].display();  // triggers loading on first access

        System.out.println("\n=== Opening page 2 ===");
        documentImages[1].display();  // triggers loading on first access

        System.out.println("\n=== Page 1 again (cached) ===");
        documentImages[0].display();  // no loading — RealImage already exists

        // Protection Proxy — role-based access control
        Image publicImage = new ProtectionProxyImage(
            new RealImage("public.png"), "VIEWER"
        );
        publicImage.display();  // works

        try {
            publicImage.getImageData();  // throws SecurityException
        } catch (SecurityException e) {
            System.out.println("Access denied: " + e.getMessage());
        }

        // Admin can download
        Image adminImage = new ProtectionProxyImage(
            new RealImage("confidential.png"), "ADMIN"
        );
        adminImage.getImageData();  // works

        // Combine proxies (virtual + protection)
        Image secureLazyImage = new ProtectionProxyImage(
            new VirtualProxyImage("secure.jpg"), "ADMIN"
        );
        secureLazyImage.display();  // lazy-loads + access check
    }
}
```

### Dynamic Proxy (Java Reflection — No Predefined Proxy Class)

```java
package structural.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

// Java's java.lang.reflect.Proxy creates proxies at runtime
// Useful when you have many interfaces to proxy

interface Service {
    void execute();
}

class RealService implements Service {
    @Override
    public void execute() {
        System.out.println("RealService executed");
    }
}

// InvocationHandler: intercepts ALL method calls on the proxy
class LoggingHandler implements InvocationHandler {
    private final Object target;

    public LoggingHandler(Object target) {
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("Before: " + method.getName());
        Object result = method.invoke(target, args);
        System.out.println("After: " + method.getName());
        return result;
    }
}

// Usage
class DynamicProxyDemo {
    public static void main(String[] args) {
        Service realService = new RealService();

        Service proxy = (Service) Proxy.newProxyInstance(
            Service.class.getClassLoader(),
            new Class[]{Service.class},
            new LoggingHandler(realService)
        );

        proxy.execute();
        // Output:
        // Before: execute
        // RealService executed
        // After: execute
    }
}
```

---

## When to Use

1. **Lazy loading (Virtual Proxy)** — large objects in a document viewer, lazy-loaded JPA entities (Hibernate), heavy
   configuration objects
2. **Access control (Protection Proxy)** — role-based authorization, rate limiting, API key validation before delegating
   to service
3. **Remote communication (Remote Proxy)** — RMI stubs, gRPC clients, Feign clients for REST APIs — the proxy handles
   serialization, network calls, error handling
4. **Caching** — database query results, API responses, computed values (the proxy checks cache before delegating)
5. **Logging / Auditing** — recording every method call on a sensitive object (financial transactions, medical records)

### Framework / Library Examples

| Technology                  | Proxy Usage                                                                                  |
|-----------------------------|----------------------------------------------------------------------------------------------|
| **Spring AOP**              | `@Transactional`, `@Secured` create proxies around beans to manage transactions and security |
| **Hibernate**               | Returns proxy objects for lazy-loaded entity associations (virtual proxy)                    |
| **Java RMI**                | `java.rmi.server.UnicastRemoteObject` creates remote proxies for distributed computing       |
| **java.lang.reflect.Proxy** | Core Java dynamic proxy for creating proxies at runtime (used by Spring, Mockito)            |
| **CGLIB / ByteBuddy**       | Class-based proxy generation for classes without interfaces (Spring, Hibernate)              |
| **Java Collections**        | `Collections.unmodifiableList()` returns a protection proxy over the original list           |

---

## When NOT to Use

1. **Simple objects with no access concerns** — don't proxy a POJO that doesn't need lazy loading, protection, or
   caching
2. **When the real subject is already light** — proxying adds overhead without benefit; just create the object directly
3. **Performance-critical code** — every proxy call adds indirection (method dispatch, optional synchronization). In hot
   loops, this matters
4. **When it breaks LSP without value** — the proxy must be a transparent substitute; if it alters behavior in
   unexpected ways, it violates Liskov
5. **When simpler alternatives work** — use `Supplier<T>` or `Lazy<T>` for lazy loading, method-level annotations for
   access control. Reserve Proxy when you need the same interface transparently

---

## Interview Questions

### Q1: What are the four main types of Proxy and when is each used?

**1) Virtual Proxy** — lazy initialization (defer expensive object creation). **2) Protection Proxy** — access control (
check permissions before delegating). **3) Remote Proxy** — local representative for a remote object (RMI stubs). **4)
Cache Proxy** — stores results of expensive operations. A fifth type is **Logging Proxy** for auditing method calls.

### Q2: How is Proxy different from Decorator?

Both wrap an object with the same interface. **Proxy** controls access to the object — it may create, protect, cache, or
locate the real subject. **Decorator** adds new behavior to the object. Proxy is typically a **gatekeeper** (controlling
access to an existing resource); Decorator is an **enhancer** (adding features). Proxy often manages lifecycle;
Decorator only wraps.

### Q3: How does Java's Dynamic Proxy work?

`java.lang.reflect.Proxy.newProxyInstance()` creates a proxy class at runtime that implements the specified interfaces.
All method calls on the proxy are forwarded to an `InvocationHandler.invoke()` method, which can intercept, modify, or
delegate the call. Spring AOP uses this for `@Transactional` — the proxy intercepts method calls, starts/commits/rolls
back transactions, then invokes the real method.

### Q4: What is the difference between JDK Dynamic Proxy and CGLIB?

**JDK Dynamic Proxy** only works for interfaces (the target must implement at least one interface). **CGLIB** (used by
Spring when no interface is available) creates a subclass of the target class at bytecode level, overriding methods.
CGLIB cannot proxy `final` classes or `final` methods. Spring defaults to JDK proxy for interfaces and falls back to
CGLIB for concrete classes.

### Q5: Can a Proxy change the behavior of the methods it intercepts?

Yes, but it should remain **transparent** unless it's a protection proxy (where denying access is expected). A logging
proxy should not alter return values. A cache proxy should return the same data the real subject would. If behavior
changes semantically, the proxy violates Liskov Substitution Principle.

### Q6: How is Proxy used in Hibernate for lazy loading?

Hibernate returns **proxy objects** for entity associations marked with `fetch = FetchType.LAZY`. The proxy is a
subclass of the entity class. When any method is called on the proxy (e.g., `getTitle()`), it queries the database to
load the full entity if not already loaded. This is a classic Virtual Proxy — the proxy defers database access until
needed.

### Q7: What are the thread-safety concerns with Virtual Proxy?

The classic double-checked locking pattern (shown above) is needed. Without synchronization, two threads could both
detect `realImage == null` and both create instances, wasting memory. With `volatile` and proper locking, only one
thread creates the real subject. In Java 5+, `volatile` provides the necessary happens-before guarantee.

### Q8: How does Spring @Transactional use Proxy?

Spring wraps the bean in a proxy. When you call a `@Transactional` method, the proxy intercepts the call, starts a
transaction, invokes the real method, and then commits or rolls back. This is why `self-invocation` fails: calling
`this.someMethod()` inside the same class bypasses the proxy, so the transaction is never started.

---

## Pros & Cons

### Advantages

- **Separation of concerns** — access control, lazy loading, caching, logging are isolated from business logic (SRP)
- **Transparent to clients** — proxy implements the same interface; client code is unchanged
- **Lifecycle management** — proxy handles object creation, initialization, cleanup of the real subject
- **Performance optimization** — lazy loading and caching reduce unnecessary resource consumption
- **Security** — protection proxy enforces access control without modifying the real subject

### Disadvantages

- **Increased complexity** — more classes, more indirection
- **Performance overhead** — extra method dispatch per call
- **Synchronization complexity** — thread-safe proxies (especially virtual) require careful double-checked locking
- **Transparency illusion** — if the proxy does something unexpected (e.g., throws `SecurityException`), client code
  needs to handle it
- **Debugging difficulty** — stack traces pass through proxy layers; it may not be obvious whether you're dealing with a
  proxy or the real object

---

## Related Patterns

| Pattern                        | Relationship                                                                                  | When to Choose                                                                  |
|--------------------------------|-----------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------|
| **Decorator**                  | Same structure (wrapping with same interface). Proxy controls access; Decorator adds behavior | Access control / lazy loading → Proxy; Behavior enhancement → Decorator         |
| **Adapter**                    | Both provide indirection. Adapter changes interface; Proxy preserves it                       | Interface mismatch → Adapter; Access control / lazy loading → Proxy             |
| **Virtual Proxy vs Flyweight** | Virtual Proxy creates on demand; Flyweight shares existing instances                          | On-demand creation → Proxy; Massive sharing of fine-grained objects → Flyweight |
| **Proxy vs Facade**            | Proxy wraps one subject; Facade wraps a whole subsystem                                       | Single object access → Proxy; Subsystem simplification → Facade                 |

### Key Distinction Memory Aid

> **Proxy** controls who enters the building (access), when the lights turn on (lazy), and whether it's a local or
> remote office (remote). Same front door.  
> **Decorator** adds furniture to the room after you're inside. Same door.  
> **Adapter** changes the building's door to fit a different frame. Different door.  
> **Facade** gives you a concierge desk instead of 10 different department doors.

---

## Key Takeaways

- **Same interface, different intent** — Proxy, Decorator, and Adapter all wrap objects, but Proxy is about control (
  access, lifecycle), Decorator about enhancement, and Adapter about interface translation
- **Dynamic proxies are powerful** — `java.lang.reflect.Proxy` lets you create a proxy for any interface at runtime
  without writing a proxy class per interface. Spring AOP, Mockito, and Hibernate all rely on this
- **Virtual Proxy is essential for performance** — defer expensive object creation until absolutely needed, especially
  in mobile, web, and large-scale enterprise apps
- **SOLID alignment** — Single Responsibility (proxy handles cross-cutting concerns), Liskov Substitution (proxy is
  substitutable for real subject), Open/Closed (new proxy types without modifying subjects)
- **Interview tip** — name all four proxy types (virtual, protection, remote, cache) and give a concrete Java example
  for each. Draw the distinction from Decorator and Adapter explicitly. Mention Spring AOP and Hibernate as real-world
  proxy consumers
