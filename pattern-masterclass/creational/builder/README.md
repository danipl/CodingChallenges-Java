# Builder Pattern

## 1. Title & Overview

The **Builder** pattern separates the construction of a complex object from its representation, allowing the same
construction process to create different representations. It solves the problem of telescoping constructors — where a
class requires many optional parameters — by providing a fluent, step-by-step API for object creation. **One-line
interview answer**: Builder replaces constructors with too many parameters using a separate Builder class that
accumulates parameters and builds the final object in a single `build()` call.

## 2. Problem Statement

### Real-World Scenario

You're building a `HttpRequest` class for an HTTP client. It needs URL (required), HTTP method (default GET), headers (
optional, many), query parameters (optional), request body (optional), timeout (default 30s), authentication (optional),
proxy settings (optional), retry config (optional), and TLS options (optional). Writing all combinations of constructors
for 10+ parameters is impossible — you'd need hundreds of overloaded constructors.

### Why This Fails at Scale

Consider an `EmailMessage` class in a notification system: `to`, `cc`, `bcc`, `subject`, `body`, `attachments`,
`priority`, `replyTo`, `contentType`, `encoding`, `signature`, `trackingId`, `templateName`, `templateVariables`,
`scheduledAt`, `expiresAt`. A constructor with 16 parameters is unreadable — callers mix up argument order, optional
parameters become `null` litter, and adding a new field breaks all existing callers. The telescoping constructor
anti-pattern has arrived.

### Pain Points of Naive Approach

- **Telescoping Constructors**: Constructor chains explode combinatorially with optional parameters
- **Poor Readability**: `new EmailMessage(to, null, null, subject, body, null, Priority.HIGH, null, "text/html", ...)` —
  which null is which?
- **Mutable Objects**: Developers resort to setters, making objects mutable when they should be immutable
- **Inconsistent State**: Object is partially initialized while setting fields, allowing invalid state
- **Fragile API**: Adding, removing, or reordering parameters breaks all construction sites
- **No Validation**: Complex validation rules across fields can't be enforced in the constructor

## 3. Solution

### How It Works

The Builder pattern creates a separate Builder class with setter methods that return `this` (for fluent chaining). Each
setter validates and stores the parameter. A final `build()` method constructs the target object, performing cross-field
validation. The target class has a private constructor taking the Builder, ensuring immutability.

### Key Participants

```
┌─────────────────────────┐
│          Product         │
├─────────────────────────┤
│ - field1: Type1          │
│ - field2: Type2          │
│ - ...                    │
│ + get...()               │
└─────────────────────────┘
         ▲ constructs
         │
┌─────────────────────────┐
│         Builder          │
├─────────────────────────┤
│ - field1: Type1          │
│ - field2: Type2          │
│ + setField1(val): Builder│◄─ returns this
│ + setField2(val): Builder│
│ + build(): Product       │◄─ constructs Product
└─────────────────────────┘
```

- **Product** (`HttpRequest`, `EmailMessage`): The complex object being built. Usually immutable.
- **Builder**: Abstract interface or concrete class defining the construction steps
- **ConcreteBuilder**: Implements Builder steps (often the same class in Java's common approach)
- **Director** (optional): Orchestrates the building process for common configurations
- **Client**: Chooses and configures the Builder, calls `build()` to get the Product

### Step-by-Step Flow

1. **Client** creates a Builder instance (often via the Product's static `builder()` method)
2. **Client** calls fluent setter methods: `builder.setName("foo").setAge(25).setEmail("x@y.com")`
3. Each setter method validates the input and stores it, returning `this` for chaining
4. **Client** calls `builder.build()`
5. **Builder** performs cross-field validation (e.g., if email is set, ensure it contains @)
6. **Builder** calls the Product's private constructor, passing itself as the builder argument
7. **Product** reads fields from the Builder, performs final validation, and returns the immutable object

## 4. Java Implementation

### Classic Builder with Fluent API

```java
package creational.builder;

import java.time.LocalDate;
import java.util.*;

public class EmailMessage {
    // ─── Required fields ───
    private final String from;
    private final Set<String> to;

    // ─── Optional fields with defaults ───
    private final Set<String> cc;
    private final Set<String> bcc;
    private final String subject;
    private final String body;
    private final Priority priority;
    private final boolean htmlContent;
    private final List<String> attachments;
    private final LocalDate scheduledAt;

    // Private constructor — only Builder can create instances
    private EmailMessage(Builder builder) {
        this.from = builder.from;
        this.to = Collections.unmodifiableSet(builder.to);
        this.cc = Collections.unmodifiableSet(builder.cc);
        this.bcc = Collections.unmodifiableSet(builder.bcc);
        this.subject = builder.subject;
        this.body = builder.body;
        this.priority = builder.priority;
        this.htmlContent = builder.htmlContent;
        this.attachments = Collections.unmodifiableList(builder.attachments);
        this.scheduledAt = builder.scheduledAt;
    }

    // Static factory for builder — idiomatic Java
    public static Builder builder(String from, String to) {
        return new Builder(from, to);
    }

    // ─── Getters (no setters — immutable) ───
    public String getFrom() { return from; }
    public Set<String> getTo() { return to; }
    public Set<String> getCc() { return cc; }
    public Set<String> getBcc() { return bcc; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public Priority getPriority() { return priority; }
    public boolean isHtmlContent() { return htmlContent; }
    public List<String> getAttachments() { return attachments; }
    public Optional<LocalDate> getScheduledAt() { return Optional.ofNullable(scheduledAt); }

    @Override
    public String toString() {
        return String.format("Email[from=%s, to=%s, subject=%s, priority=%s]",
                from, to, subject, priority);
    }

    // ─── Builder ───
    public static class Builder {
        // Required fields (final, set in constructor)
        private final String from;
        private final Set<String> to = new HashSet<>();

        // Optional fields with sensible defaults
        private final Set<String> cc = new HashSet<>();
        private final Set<String> bcc = new HashSet<>();
        private String subject = "";
        private String body = "";
        private Priority priority = Priority.NORMAL;
        private boolean htmlContent = false;
        private final List<String> attachments = new ArrayList<>();
        private LocalDate scheduledAt;

        public Builder(String from, String to) {
            this.from = requireNonBlank(from, "from");
            this.to.add(requireNonBlank(to, "to"));
        }

        // ─── Fluent setters ───
        public Builder cc(String... ccAddresses) {
            for (String cc : ccAddresses) {
                this.cc.add(requireNonBlank(cc, "cc"));
            }
            return this;
        }

        public Builder bcc(String... bccAddresses) {
            for (String bcc : bccAddresses) {
                this.bcc.add(requireNonBlank(bcc, "bcc"));
            }
            return this;
        }

        public Builder subject(String subject) {
            this.subject = requireNonNull(subject, "subject");
            return this;
        }

        public Builder body(String body) {
            this.body = requireNonNull(body, "body");
            return this;
        }

        public Builder priority(Priority priority) {
            this.priority = requireNonNull(priority, "priority");
            return this;
        }

        public Builder htmlContent(boolean htmlContent) {
            this.htmlContent = htmlContent;
            return this;
        }

        public Builder attachment(String... files) {
            this.attachments.addAll(Arrays.asList(files));
            return this;
        }

        public Builder scheduledAt(LocalDate scheduledAt) {
            this.scheduledAt = requireNonNull(scheduledAt, "scheduledAt");
            return this;
        }

        // ─── Build method with validation ───
        public EmailMessage build() {
            // Cross-field validation
            if (subject.isEmpty() && body.isEmpty()) {
                throw new IllegalStateException("Subject or body must be provided");
            }
            if (htmlContent && !body.startsWith("<")) {
                throw new IllegalStateException("HTML content must include HTML tags");
            }
            if (scheduledAt != null && scheduledAt.isBefore(LocalDate.now())) {
                throw new IllegalStateException("Scheduled date must be in the future");
            }
            return new EmailMessage(this);
        }

        // ─── Validation helpers ───
        private static String requireNonBlank(String value, String field) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(field + " must not be blank");
            }
            return value;
        }

        private static <T> T requireNonNull(T value, String field) {
            if (value == null) {
                throw new IllegalArgumentException(field + " must not be null");
            }
            return value;
        }
    }
}

enum Priority {
    LOW, NORMAL, HIGH, URGENT
}
```

### Director Example

```java
package creational.builder;

// Director — encapsulates common construction recipes
class EmailDirector {
    private final EmailMessage.Builder builder;

    public EmailDirector(EmailMessage.Builder builder) {
        this.builder = builder;
    }

    public EmailMessage buildWelcomeEmail(String newUserEmail) {
        return builder
                .subject("Welcome to our platform!")
                .body("<h1>Welcome!</h1><p>Thanks for joining.</p>")
                .htmlContent(true)
                .priority(Priority.NORMAL)
                .build();
    }

    public EmailMessage buildUrgentAlert(String recipient, String alertMessage) {
        return builder
                .subject("[URGENT] System Alert")
                .body(alertMessage)
                .priority(Priority.URGENT)
                .build();
    }
}

public class BuilderWithDirectorDemo {
    public static void main(String[] args) {
        EmailDirector director = new EmailDirector(
                EmailMessage.builder("system@company.com", "user@example.com"));
        System.out.println(director.buildWelcomeEmail("newuser@example.com"));
        System.out.println(director.buildUrgentAlert("admin@company.com", "Server CPU at 95%"));
    }
}
```

### Java Records Builder (Java 17+)

```java
package creational.builder;

import java.util.List;
import java.util.Optional;

// Using records for the product — built-in immutability
public record HttpRequest(
        String url,
        String method,
        Map<String, String> headers,
        String body,
        int timeoutSeconds,
        boolean followRedirects
) {
    // Compact canonical constructor with validation
    public HttpRequest {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL must not be blank");
        }
        if (method == null || method.isBlank()) {
            method = "GET"; // Default in the canonical constructor
        }
        if (headers == null) {
            headers = Map.of();
        }
        if (timeoutSeconds <= 0) {
            timeoutSeconds = 30;
        }
    }

    public static Builder builder(String url) {
        return new Builder(url);
    }

    public static class Builder {
        private String url;
        private String method = "GET";
        private Map<String, String> headers = Map.of();
        private String body;
        private int timeoutSeconds = 30;
        private boolean followRedirects = true;

        public Builder(String url) {
            this.url = url;
        }

        public Builder method(String method) {
            this.method = method.toUpperCase();
            return this;
        }

        public Builder header(String key, String value) {
            if (this.headers.isEmpty()) {
                this.headers = new java.util.HashMap<>();
            }
            ((java.util.HashMap<String, String>) this.headers).put(key, value);
            return this;
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public Builder timeoutSeconds(int timeout) {
            this.timeoutSeconds = timeout;
            return this;
        }

        public Builder followRedirects(boolean follow) {
            this.followRedirects = follow;
            return this;
        }

        public HttpRequest build() {
            // Cross-field: body requires PUT/POST/PATCH
            if (body != null && !body.isBlank()
                    && List.of("GET", "DELETE", "HEAD").contains(method)) {
                throw new IllegalStateException(
                        method + " requests cannot have a body");
            }
            return new HttpRequest(url, method,
                    Map.copyOf(headers), body, timeoutSeconds, followRedirects);
        }
    }
}
```

### Generic Builder (Inheritance — Recursive Generics)

```java
package creational.builder;

abstract class Vehicle {
    protected final String brand;
    protected final String model;
    protected final int year;

    protected Vehicle(Builder<?> builder) {
        this.brand = builder.brand;
        this.model = builder.model;
        this.year = builder.year;
    }

    abstract static class Builder<T extends Builder<T>> {
        private String brand;
        private String model;
        private int year;

        public T brand(String brand) { this.brand = brand; return self(); }
        public T model(String model) { this.model = model; return self(); }
        public T year(int year) { this.year = year; return self(); }

        protected abstract T self();
        public abstract Vehicle build();
    }
}

class Car extends Vehicle {
    private final int doors;

    private Car(Builder builder) { super(builder); this.doors = builder.doors; }

    public static class Builder extends Vehicle.Builder<Builder> {
        private int doors = 4;
        public Builder doors(int doors) { this.doors = doors; return this; }
        @Override protected Builder self() { return this; }
        @Override public Car build() { return new Car(this); }
    }
}

class Motorcycle extends Vehicle {
    private final boolean hasSidecar;

    private Motorcycle(Builder builder) { super(builder); this.hasSidecar = builder.hasSidecar; }

    public static class Builder extends Vehicle.Builder<Builder> {
        private boolean hasSidecar = false;
        public Builder hasSidecar(boolean v) { this.hasSidecar = v; return this; }
        @Override protected Builder self() { return this; }
        @Override public Motorcycle build() { return new Motorcycle(this); }
    }
}

public class InheritanceBuilderDemo {
    public static void main(String[] args) {
        Car car = new Car.Builder().brand("Tesla").model("Model 3").year(2024).doors(4).build();
        Motorcycle bike = new Motorcycle.Builder().brand("Harley").model("Sportster").year(2023).build();
        System.out.println(car);
        System.out.println(bike);
    }
}
```

## 5. When to Use

### Framework & Library Examples

| Framework                       | Usage                                                                                         |
|---------------------------------|-----------------------------------------------------------------------------------------------|
| **Java StringBuilder**          | Classic example of Builder — mutable sequence built step by step, finalized with `toString()` |
| **Lombok @Builder**             | Annotation processor generating Builder class automatically                                   |
| **Spring UriComponentsBuilder** | Builds URI objects step by step: scheme, host, path, query params                             |
| **Java Stream API**             | `Stream.builder()` creates a stream with `add()` and `build()`                                |
| **OkHttp**                      | `Request.Builder` — fluent API for HTTP requests                                              |
| **Protobuf**                    | Generated Builder classes for all message types                                               |
| **Immutables / AutoValue**      | Annotation processors generating immutable objects with Builders                              |

### Real-World Scenarios

1. **HTTP Request Construction**: URL, headers, query params, body, timeouts, auth — all optional. Builder provides
   readable creation with validation (e.g., body only valid for POST/PUT).

2. **Database Query Builders**: ORM query builders (JPA Criteria API, jOOQ, Querydsl) use Builder for constructing SQL
   queries dynamically with type safety.

3. **Configuration Objects**: Complex configuration POJOs with many optional settings (database config, thread pool
   settings, security policies). Builder ensures valid combinations.

4. **UI Component Trees**: Building complex UI hierarchies (e.g., a dialog with buttons, text fields, and layout
   managers) using a fluent Builder API.

5. **Nutrition Facts / Product Catalog**: Objects with 15+ optional attributes (calories, fat, carbs, protein, vitamins,
   allergens, ingredients list, certifications, etc.) — classic Builder example from GoF book.

## 6. When NOT to Use

### Over-Engineering Warning

Builder is overkill for objects with 2-3 parameters. `new Point(x, y)` is clear, concise, and immutable. Adding a
Builder for a simple value object wastes code and confuses readers.

### Simpler Alternatives

- **Static Factory Methods**: `LocalDate.of(2024, 1, 15)` — readable without Builder for small parameter sets.
- **Simple Constructor with Clear Naming**: For 3-5 required params, a well-named constructor is often clearer.
- **Tuple/Data Classes (Records)**: Java 16+ records handle simple immutable data carriers without Builder.
- **Named Parameters (via @Builder annotation)**: Project Lombok's `@Builder` generates the boilerplate.

### Performance Considerations

- **Memory**: The Builder exists alongside the Product during construction — doubles memory temporarily
- **CPU**: Method chaining adds virtual dispatch overhead (negligible for most apps)
- **GC Pressure**: Builder object becomes garbage after `build()` call — consider reusing Builders in hot paths
- **String Concatenation**: In StringBuilder, the internal buffer may resize multiple times during construction

### When Not to Use Checklist

- □ Fewer than 4 parameters, most required
- □ All parameters are required (use constructor or static factory)
- □ Object is a simple value with no validation logic
- □ The Builder code is more complex than the object it builds
- □ You're using Lombok @Builder (let it generate the code)
- □ Immutability is not required (use setters on a mutable POJO)

## 7. Interview Questions

**Q1: What problem does the Builder pattern solve?**

A1: It solves the telescoping constructor anti-pattern — classes with many optional parameters that require multiple
overloaded constructors. Builder provides a readable, fluent API that lets clients set only the parameters they need, in
any order, while keeping the Product immutable and enforcing validation at build time.

**Q2: How is Builder different from a regular constructor with setters?**

A2: Builder produces immutable objects — no setters on the Product. With setters, the object is mutable and can be left
in an inconsistent state between setter calls. Builder's `build()` method performs cross-field validation atomically and
returns a fully initialized, immutable object.

**Q3: Explain the recursive generic pattern (`Builder<T extends Builder<T>>`) in inheritance.**

A3: This pattern solves the problem of fluent chaining in subclass builders. `Vehicle.Builder<T>` is generic so that
`Car.Builder extends Vehicle.Builder<Car.Builder>`. The `self()` method returns `this` correctly typed. This lets
`carBuilder.brand("Tesla").doors(4)` work — `brand()` from the parent returns `Car.Builder` type, not `Vehicle.Builder`.

**Q4: What is the role of a Director in the Builder pattern?**

A4: The Director encapsulates common construction sequences into reusable methods. For example,
`EmailDirector.buildWelcomeEmail()` internally calls `builder.subject("Welcome").body("...").build()`. This hides the
construction recipe from clients and avoids duplicating the same builder chain across the codebase.

**Q5: How does the Builder pattern relate to immutability?**

A5: Builder is the primary pattern for creating immutable objects with many parameters. The Product has a private
constructor that takes the Builder; all fields are `final` and set once during construction. Without Builder, immutable
classes with many optional fields would require telescoping constructors or large object initializer blocks.

**Q6: Name a Java standard library class that uses the Builder pattern.**

A6: `java.lang.StringBuilder` — the original GoF Builder example in Java. `StringBuilder` accumulates characters through
`append()` calls and produces an immutable `String` via `toString()`. Other examples: `java.util.stream.Stream.Builder`,
`javax.swing.GroupLayout.Group`, `java.lang.ProcessBuilder`.

**Q7: How does Lombok's @Builder work, and what are its limitations?**

A7: Lombok's `@Builder` generates a static `builder()` method, a Builder inner class with fluent setters for all fields,
and a `build()` method. Limitations: no custom validation in generated code (must use `@Builder(toBuilder = true)` with
custom methods), no support for required fields (all are optional), and it's an annotation processor dependency that
some teams avoid.

**Q8: Compare Builder with Prototype for creating complex objects.**

A8: Builder constructs objects step by step from scratch — useful when creating a new object with specific
configuration. Prototype clones an existing instance and customizes it — useful when the base object is expensive to
create from scratch. Builder is better when most parameters vary; Prototype is better when most parameters stay the
same.

## 8. Pros & Cons

### Advantages

- **Immutability**: Produces immutable objects (private constructor, final fields, no setters)
- **Readability**: Named setter methods make construction self-documenting
- **Validation at Build Time**: Cross-field validation in `build()` catches illegal combinations early
- **Fluent API**: Method chaining produces concise, expressive construction code
- **Fine-grained Control**: Each setter can validate its parameter independently
- **Varying Representations**: Same construction process can produce different representations
- **Writes Protected**: Product is not visible until fully constructed (no partial state leakage)

### Disadvantages

- **Boilerplate**: Builder class has nearly the same fields as the Product — code duplication
- **Performance**: Extra object (Builder) created for each Product; GC overhead if used heavily
- **Complexity**: Adds another layer that new developers must understand
- **Over-engineering Risk**: Used where a simple constructor or static factory would suffice
- **Reflection Incompatibility**: Private constructors and Builders complicate frameworks that need no-arg constructors
- **Inheritance Pain**: Extending Builder hierarchies requires the recursive generic pattern, which is complex to
  maintain

## 9. Related Patterns

| Pattern              | Relationship                                                                                                                                                                                                                    |
|----------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Abstract Factory** | Builder constructs complex objects step by step; Abstract Factory creates families of related objects in one call. Builder focuses on the construction process; Abstract Factory focuses on product family selection.           |
| **Prototype**        | Alternative to Builder for creating complex objects. Prototype clones an existing object; Builder builds from scratch. Use Builder when the configuration varies significantly; use Prototype when you have a template to copy. |
| **Strategy**         | A Builder can accept a Strategy for specific construction steps, allowing the building process to vary based on strategy.                                                                                                       |

### How to Choose

- Use **Builder** for objects with 4+ parameters, especially with many optionals
- Use **Static Factory Methods** for 1-3 parameters with meaningful names
- Use **Abstract Factory** when you need to create families of related objects
- Use **Prototype** when you have a configured template and need copies with slight variations
- Use **Lombok @Builder** if your team allows annotation processing and you want to avoid boilerplate

## 10. Key Takeaways

- **Separate construction from representation**: The core insight of the Builder pattern
- **Immutability enabler**: Builder is the standard Java approach for creating immutable objects with many fields
- **Fluent interfaces**: Method chaining returning `this` produces readable construction code
- **Validation at build time**: Cross-field rules are checked once, in one place, before the object exists
- **Interview memory aid**: "Builder = telescoping constructors solved. Many optional params? Use Builder. Fluent
  setters return this. build() validates and creates immutable object."
