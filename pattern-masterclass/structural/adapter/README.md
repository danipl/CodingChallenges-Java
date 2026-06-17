# Adapter Pattern (Wrapper Pattern)

## Overview

The **Adapter Pattern** allows objects with incompatible interfaces to collaborate by converting one interface into
another that a client expects. It acts as a bridge between two disparate systems without modifying their source code. *
*One-line interview answer**: The Adapter pattern converts the interface of a class into another interface the client
expects, enabling classes to work together that couldn't otherwise because of incompatible interfaces.

---

## Problem Statement

### Real-World Scenario

Your e-commerce platform needs to integrate a third-party payment gateway — say, Stripe or PayPal. Your system already
has a well-defined `PaymentProcessor` interface with a method `pay(amount)`. But the third-party SDK exposes a
completely different API: `StripePayment.charge(double amount, String currency)`. You cannot modify the library (it's a
dependency), and you shouldn't rewrite your core business logic just to support a new vendor.

### Why This Matters in Production

Payment integration is a textbook example, but the pattern appears everywhere: legacy system integration, API version
migration, data format conversion, and library abstraction. In a microservices world, services expose REST/gRPC
interfaces that rarely match perfectly. Without an adapter, you'd litter your code with conversion logic, violate the
Open/Closed Principle, and make every new integration a rewrite event.

### Pain Points Without Adapter

- **Tight coupling** to vendor-specific APIs — swapping a provider means rewriting callers
- **Code duplication** — conversion logic repeated across every call site
- **Violation of Dependency Inversion** — high-level modules depend on low-level implementation details
- **Testing difficulty** — impossible to mock third-party SDKs without abstraction

---

## Solution

The Adapter pattern introduces a middle layer that maps the client's domain interface to the adaptee's concrete
interface. There are two flavors:

| Flavor             | Mechanism                                                                         | Java Example                                           |
|--------------------|-----------------------------------------------------------------------------------|--------------------------------------------------------|
| **Object Adapter** | Composition — the adapter holds a reference to the adaptee                        | Preferred; works even when adaptee is `final`          |
| **Class Adapter**  | Inheritance — the adapter extends the adaptee and implements the target interface | Requires multiple inheritance (use interfaces in Java) |

### Key Participants

| Role        | Description                                                    |
|-------------|----------------------------------------------------------------|
| **Target**  | The domain-specific interface clients depend on                |
| **Client**  | Code that collaborates with objects conforming to the Target   |
| **Adaptee** | The existing class with an incompatible interface              |
| **Adapter** | Wraps the Adaptee and implements the Target, translating calls |

### Flow

```
Client  →  Target Interface  ←  Adapter  →  Adaptee
            ↑                                  ↑
        pay(amount)                    charge(amount, currency)
```

1. Client calls `adapter.pay(100)`
2. Adapter translates to `adaptee.charge(100, "USD")`
3. Result is returned in the format the client expects

---

## Java Implementation

### Common Interface (Target)

```java
package structural.adapter;

public interface PaymentProcessor {
    boolean pay(double amount);
    boolean refund(String transactionId, double amount);
}
```

### Adaptee (Third-Party Library)

```java
package structural.adapter;

// Simulates a third-party Stripe SDK — we cannot modify this class
public class StripePaymentGateway {
    private final String apiKey;

    public StripePaymentGateway(String apiKey) {
        this.apiKey = apiKey;
    }

    public String charge(String currency, double amount) {
        // Simulates calling Stripe's REST API
        return "txn_" + System.currentTimeMillis(); // returns transaction ID
    }

    public void refund(String chargeId) {
        System.out.println("Stripe: refunding charge " + chargeId);
    }
}
```

### Object Adapter (Composition — Preferred)

```java
package structural.adapter;

// Object adapter: wraps the adaptee using composition
public class StripeAdapter implements PaymentProcessor {
    private final StripePaymentGateway stripeGateway;

    public StripeAdapter(StripePaymentGateway stripeGateway) {
        this.stripeGateway = stripeGateway;
    }

    @Override
    public boolean pay(double amount) {
        try {
            String txnId = stripeGateway.charge("USD", amount);
            return txnId != null && !txnId.isBlank();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean refund(String transactionId, double amount) {
        try {
            stripeGateway.refund(transactionId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

### Another Adaptee (PayPal)

```java
package structural.adapter;

// Another third-party SDK with a different interface
public class PayPalApi {
    public String createPayment(double usdAmount) {
        return "PAY-" + System.currentTimeMillis();
    }

    public boolean executePayment(String paymentId) {
        return true;
    }

    public void cancelPayment(String paymentId) {
        System.out.println("PayPal: cancelling payment " + paymentId);
    }
}
```

### Adapter for PayPal

```java
package structural.adapter;

// Object adapter for PayPal
public class PayPalAdapter implements PaymentProcessor {
    private final PayPalApi payPalApi;

    public PayPalAdapter(PayPalApi payPalApi) {
        this.payPalApi = payPalApi;
    }

    @Override
    public boolean pay(double amount) {
        String paymentId = payPalApi.createPayment(amount);
        return paymentId != null && payPalApi.executePayment(paymentId);
    }

    @Override
    public boolean refund(String transactionId, double amount) {
        try {
            payPalApi.cancelPayment(transactionId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

### Class Adapter (Inheritance — Less Flexible)

```java
package structural.adapter;

// Class adapter: extends the adaptee and implements the target interface
// Only works if StripePaymentGateway is not final
// More rigid than object adapter; use sparingly
public class StripeClassAdapter extends StripePaymentGateway implements PaymentProcessor {

    public StripeClassAdapter(String apiKey) {
        super(apiKey);
    }

    @Override
    public boolean pay(double amount) {
        try {
            String txnId = charge("USD", amount);
            return txnId != null && !txnId.isBlank();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean refund(String transactionId, double amount) {
        try {
            refund(transactionId); // calls StripePaymentGateway.refund
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

### Usage Example

```java
package structural.adapter;

public class AdapterDemo {
    public static void main(String[] args) {
        // Object adapter usage
        PaymentProcessor stripeProcessor = new StripeAdapter(
            new StripePaymentGateway("sk_live_abc123")
        );
        stripeProcessor.pay(99.99);
        stripeProcessor.refund("txn_12345", 99.99);

        PaymentProcessor payPalProcessor = new PayPalAdapter(new PayPalApi());
        payPalProcessor.pay(49.99);

        // Class adapter usage (less common)
        PaymentProcessor classAdapterStripe = new StripeClassAdapter("sk_test_xyz");
        classAdapterStripe.pay(199.99);
    }
}
```

### Two-Way Adapter (Advanced)

```java
package structural.adapter;

// Uncommon but useful: adapts both ways between two interfaces
public class TwoWayAdapter implements ModernLogger, LegacyLogger {
    private final LegacyLogger legacy;
    private final ModernLogger modern;

    public TwoWayAdapter(LegacyLogger legacy, ModernLogger modern) {
        this.legacy = legacy;
        this.modern = modern;
    }

    // From ModernLogger
    @Override public void log(LogLevel level, String message) { /* ... */ }
    // From LegacyLogger
    @Override public void write(String message) { /* ... */ }
}

interface ModernLogger {
    enum LogLevel { INFO, WARN, ERROR }
    void log(LogLevel level, String message);
}

interface LegacyLogger {
    void write(String message);
}
```

### Default Adapter Pattern (Pluggable)

```java
package structural.adapter;

// A reusable adapter that accepts lambdas/method references
// Demonstrates adapting any Supplier into a PaymentProcessor
import java.util.function.Supplier;

public class PluggableAdapter<T> {
    private final Supplier<T> adaptee;

    public PluggableAdapter(Supplier<T> adaptee) {
        this.adaptee = adaptee;
    }

    public T execute() {
        return adaptee.get();
    }
}
```

---

## When to Use

1. **Integrating third-party libraries** — wrapping SDKs that don't match your domain model (payment gateways, cloud
   providers, messaging queues)
2. **Legacy system migration** — running old and new systems side-by-side during a gradual rewrite (Struts → Spring MVC
   adapters)
3. **API versioning** — translating v1 API contracts to v2 without breaking existing clients
4. **Data format conversion** — converting XML responses to JSON, or CSV to POJOs before processing
5. **Testing / Mocking** — wrapping a heavyweight real implementation behind a lightweight test adapter

### Framework / Library Examples

| Technology           | Adapter Usage                                                                           |
|----------------------|-----------------------------------------------------------------------------------------|
| **Spring MVC**       | `HandlerAdapter` dispatches requests to different handler types (controllers, servlets) |
| **Java Collections** | `Arrays.asList()` adapts an array to the `List` interface                               |
| **SLF4J**            | Logging facade adapts various frameworks (Logback, Log4j, java.util.logging)            |
| **JDBC Driver**      | Each database driver adapts DB-specific protocol to the standard JDBC interface         |
| **Spring Security**  | `UserDetailsService` adapts user data sources to Spring Security's authentication model |

---

## When NOT to Use

1. **Simple, stable interfaces** — if the interface rarely changes, adding an adapter layer is YAGNI over-engineering
2. **You control both sides** — modify the adaptee directly instead of wrapping it (prefer refactoring over wrapping)
3. **Excessive indirection** — adapters for adapters ("adapter inception") create debugging nightmares; keep the chain ≤
   1 level
4. **Performance-critical paths** — every call goes through an extra method invocation; in hot loops (video frames,
   audio processing), this can compound
5. **When a simpler refactor works** — before reaching for Adapter, consider: can the client just call the adaptee
   directly? Is the mismatch small enough to handle inline?

---

## Interview Questions

### Q1: What is the difference between Adapter, Decorator, and Proxy?

They all involve wrapping an object. **Adapter** changes the interface to match what the client expects. **Decorator**
adds new behavior while keeping the same interface. **Proxy** controls access to the object (lazy loading, protection)
while keeping the same interface. Adapter solves incompatibility; Decorator enhances functionality; Proxy controls
access.

### Q2: Object Adapter vs Class Adapter — which is better and why?

**Object Adapter** (composition) is almost always preferred because it works even when the adaptee class is `final`,
introduces less coupling, and allows adapting multiple adaptees simultaneously. **Class Adapter** (inheritance) commits
to a specific adaptee at compile time and exposes the adaptee's protected members, violating encapsulation.

### Q3: How does the Adapter pattern relate to the Open/Closed Principle?

The Adapter pattern exemplifies OCP: you can extend the system's behavior (support new providers) by writing new
adapters without modifying existing client code. The `PaymentProcessor` interface is closed for modification but open
for extension through new adapter implementations.

### Q4: Can you give a real-world Java example of the Adapter pattern?

`java.util.Arrays.asList()` adapts an array (`T[]`) to the `List<T>` interface. The returned list is backed by the
original array, so changes to one affect the other. Another example is `InputStreamReader`, which adapts a byte-based
`InputStream` to a character-based `Reader`.

### Q5: Does the Adapter pattern work with Lambdas in modern Java?

Yes. When the target interface is a Single Abstract Method (SAM) interface (functional interface), a lambda or method
reference can serve as an implicit adapter. For example, `Callable<Void>` adapted from a lambda:
`ExecutorService executor; executor.submit(() -> { doWork(); return null; });`. This avoids writing a named adapter
class for simple cases.

### Q6: What is a "Two-Way Adapter"?

A Two-Way Adapter implements both the target and adaptee interfaces, allowing the same object to be used on either side
of the integration. It's useful when migrating legacy systems where different parts of the codebase expect different
interfaces for the same data.

### Q7: How would you test an Adapter?

Test the adapter in isolation by mocking the adaptee. Verify that calling `adapter.pay(100)` correctly invokes
`adaptee.charge("USD", 100)`. Use contract tests that run the same test suite against every adapter implementation to
ensure consistent behavior. Then add integration tests that exercise the real adaptee (e.g., sandbox Stripe account).

### Q8: What is the difference between Adapter and Facade?

**Adapter** converts one interface to another (interface translation). **Facade** provides a simplified interface to a
complex subsystem (interface simplification). An adapter targets a specific client need; a facade hides multiple
components behind a single entry point. They can work together: a facade might use adapters internally.

---

## Pros & Cons

### Advantages

- **Single Responsibility Principle** — separates interface conversion from business logic
- **Open/Closed Principle** — new adapters added without modifying existing code
- **Reusability** — adapters can be reused across projects that integrate the same library
- **Loose coupling** — client depends only on the target interface, not on concrete adaptees
- **Testability** — client code can be unit-tested with mock adapters

### Disadvantages

- **Increased complexity** — more classes, more indirection
- **Performance overhead** — one extra method call per operation
- **Over-engineering risk** — unnecessary abstraction when APIs are unlikely to change
- **Debugging friction** — stack traces become longer; need to trace through adapter layer
- **Maintenance burden** — adapters drift when underlying libraries update

---

## Related Patterns

| Pattern       | Relationship                                                                                                       | When to Choose                                                                                                                   |
|---------------|--------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------|
| **Decorator** | Both wrap an object, but Adapter changes the interface while Decorator preserves it                                | Choose Adapter for incompatibility, Decorator for adding behavior                                                                |
| **Proxy**     | Both provide a surrogate, but Adapter solves interface mismatch while Proxy controls access                        | Choose Adapter when interfaces don't match; Proxy when you need lazy loading, caching, or access control with the same interface |
| **Facade**    | Both provide abstraction, but Adapter translates a single interface while Facade simplifies a subsystem            | Choose Facade to simplify a complex system; choose Adapter to make one specific interface work                                   |
| **Bridge**    | Both decouple, but Bridge separates abstraction from implementation intentionally; Adapter retrofits existing code | Choose Bridge at design time for flexibility; choose Adapter at integration time for compatibility                               |

### Key Distinction Memory Aid

> **Adapter** rewires incompatible plugs (different interface).  
> **Decorator** adds new features to existing outlets (same interface).  
> **Proxy** controls who plugs into the outlet (same interface, access control).  
> **Facade** hides a wall of complex outlets behind a single power strip.

---

## Key Takeaways

- **Composition over inheritance** — Object Adapter (composition) is strictly preferred over Class Adapter (inheritance)
  in all real-world production code
- **SOLID alignment** — Single Responsibility (interface conversion), Open/Closed (extensible via new adapters),
  Dependency Inversion (clients depend on abstractions)
- **Ubiquitous in Java** — every JDBC driver, every SLF4J binding, every Spring `HandlerAdapter` is an Adapter — you use
  this pattern daily without realizing it
- **Think "translator" not "wrapper"** — the core job is language translation between two APIs, not enhancement or
  access control
- **Interview tip** — when asked about Adapter, immediately draw the distinction with Decorator and Proxy, and mention
  composition vs inheritance. Cite `Arrays.asList()` as your go-to Java example
