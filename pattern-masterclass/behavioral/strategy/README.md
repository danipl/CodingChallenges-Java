# Strategy Pattern

## Overview

**Definition**: The Strategy pattern defines a family of interchangeable algorithms, encapsulates each one, and makes
them interchangeable at runtime. It lets the algorithm vary independently from the clients that use it.

**Core Problem**: When you have multiple related algorithms or behaviors that differ only in their implementation, and
you need to switch between them dynamically without changing the client code.

**One-Line Interview Answer**: "Strategy pattern encapsulates interchangeable algorithms behind a common interface,
allowing the client to select and swap behaviors at runtime without modifying the source code."

## Problem Statement

### Real-World Scenario: Payment Processing

Imagine building an e-commerce checkout system. You need to support multiple payment methods: credit card, PayPal, bank
transfer, and cryptocurrency. A naive approach uses a monolithic method with a giant `if-else` or `switch` statement:

```java
public class CheckoutService {
    public void processPayment(Order order, String paymentType) {
        if (paymentType.equals("CREDIT_CARD")) {
            // validate card number
            // charge via Stripe API
            // handle fraud detection
            // send receipt
        } else if (paymentType.equals("PAYPAL")) {
            // redirect to PayPal OAuth
            // handle callback
            // verify payment
        } else if (paymentType.equals("BANK_TRANSFER")) {
            // generate IBAN reference
            // wait for confirmation webhook
        } else if (paymentType.equals("CRYPTO")) {
            // generate wallet address
            // wait for blockchain confirmation
        }
        // each new payment type requires modifying this method
    }
}
```

### Pain Points of the Naive Approach

1. **Open/Closed Principle Violation**: Adding a new payment type requires modifying the existing `processPayment`
   method. The class is not closed for modification.
2. **Code Bloat**: The method grows linearly with each new algorithm. After 20 payment types, this single method becomes
   unmaintainable.
3. **Duplication**: Shared concerns like logging, validation, and transaction management are duplicated or tangled with
   each algorithm.
4. **Testing Difficulty**: Testing every payment type requires testing the single monolithic method with all its
   branching logic.
5. **Deployment Risk**: Changing one algorithm risks breaking all others since they're co-located.

### Why This Matters in Production

In production systems, business logic evolves constantly. New payment providers are integrated, old ones deprecated, and
regulations change. Without Strategy, every change is a high-risk modification to a well-tested class. Strategy lets you
add, remove, or modify algorithms in isolation, with zero risk to existing functionality.

## Solution

### How Strategy Solves This

The Strategy pattern decomposes the problem into three parts:

1. **Strategy Interface**: Defines a common contract for all algorithms
2. **Concrete Strategies**: Each encapsulates one algorithm
3. **Context**: Holds a reference to the current strategy and delegates execution

### Key Participants

| Participant            | Role                                                       |
|------------------------|------------------------------------------------------------|
| `Strategy` (interface) | Declares the algorithm contract, typically a single method |
| `ConcreteStrategy`     | Implements one specific variant of the algorithm           |
| `Context`              | Maintains a reference to a Strategy; delegates work to it  |

### Step-by-Step Flow

1. Client creates a ConcreteStrategy object (e.g., `CreditCardPayment`)
2. Client passes it to the Context (e.g., `CheckoutService`)
3. Context stores the reference and calls `execute()` when needed
4. The ConcreteStrategy runs its algorithm without the Context knowing the details
5. Client can swap strategies at runtime by calling a setter

### UML-Style Structure

```
┌─────────────┐       ┌──────────────────┐
│   Context   │───────│   «interface»    │
│             │       │    Strategy      │
│ +execute()  │       │ +algorithm()     │
└─────────────┘       └────────┬─────────┘
                               │
              ┌────────────────┼────────────────┐
              │                │                │
     ┌────────┴───────┐ ┌─────┴──────┐ ┌───────┴─────┐
     │ConcreteStratA  │ │ConcreteSt  │ │ConcreteSt   │
     │                │ │ratB        │ │ratC         │
     │+algorithm()    │ │+algorithm()│ │+algorithm() │
     └────────────────┘ └────────────┘ └─────────────┘
```

## Java Implementation

### Modern Java (17+) with Functional Interface

The Strategy pattern is elegantly expressed using functional interfaces and lambdas in modern Java.

```java
package behavioral.strategy;

import java.math.BigDecimal;
import java.util.function.UnaryOperator;

// Strategy as a functional interface
@FunctionalInterface
interface PaymentStrategy {
    boolean pay(BigDecimal amount);

    // Default method for common validation
    default boolean validateAmount(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }
}
```

### Concrete Strategies (Traditional Classes)

```java
class CreditCardPayment implements PaymentStrategy {
    private final String cardNumber;
    private final String cvv;
    private final String expiry;

    public CreditCardPayment(String cardNumber, String cvv, String expiry) {
        this.cardNumber = cardNumber;
        this.cvv = cvv;
        this.expiry = expiry;
    }

    @Override
    public boolean pay(BigDecimal amount) {
        if (!validateAmount(amount)) return false;
        System.out.printf("Charging $%.2f to card %s%n", amount, maskCard());
        // Integrate with Stripe/Payment gateway
        return true;
    }

    private String maskCard() {
        return "****-****-****-" + cardNumber.substring(cardNumber.length() - 4);
    }
}

class PayPalPayment implements PaymentStrategy {
    private final String email;

    public PayPalPayment(String email) {
        this.email = email;
    }

    @Override
    public boolean pay(BigDecimal amount) {
        if (!validateAmount(amount)) return false;
        System.out.printf("Charging $%.2f via PayPal for %s%n", amount, email);
        // Integrate with PayPal API
        return true;
    }
}

class CryptoPayment implements PaymentStrategy {
    private final String walletAddress;

    public CryptoPayment(String walletAddress) {
        this.walletAddress = walletAddress;
    }

    @Override
    public boolean pay(BigDecimal amount) {
        if (!validateAmount(amount)) return false;
        System.out.printf("Sending %.6f BTC to %s%n",
            amount.doubleValue() / 50000.0, walletAddress);
        // Integrate with blockchain API
        return true;
    }
}
```

### Context Class

```java
class CheckoutContext {
    private PaymentStrategy paymentStrategy;

    public CheckoutContext() {
        this.paymentStrategy = amount -> {
            System.out.println("No payment strategy set — defaulting to COD");
            return true;
        };
    }

    public void setPaymentStrategy(PaymentStrategy paymentStrategy) {
        this.paymentStrategy = paymentStrategy;
    }

    public void checkout(BigDecimal amount) {
        System.out.println("Starting checkout...");
        // Common pre-processing: tax calculation, inventory check
        if (paymentStrategy.pay(amount)) {
            System.out.println("Payment successful! Sending confirmation...");
        } else {
            System.out.println("Payment failed.");
        }
    }
}
```

### Usage with Lambda-Based Strategies

```java
public class StrategyDemo {
    public static void main(String[] args) {
        var checkout = new CheckoutContext();
        var amount = new BigDecimal("149.99");

        // Strategy as lambda — instant and inline
        checkout.setPaymentStrategy(
            amt -> {
                System.out.println("Paying with cash on delivery: $" + amt);
                return true;
            }
        );
        checkout.checkout(amount);

        // Swap to credit card strategy
        checkout.setPaymentStrategy(
            new CreditCardPayment("4111-1111-1111-1111", "123", "12/28")
        );
        checkout.checkout(new BigDecimal("299.99"));

        // Swap to PayPal
        checkout.setPaymentStrategy(new PayPalPayment("user@example.com"));
        checkout.checkout(new BigDecimal("49.99"));
    }
}
```

### Advanced: Strategy with Enum + Registry

```java
import java.util.Map;
import java.util.HashMap;

enum PaymentType implements PaymentStrategy {
    CREDIT_CARD(params -> new CreditCardPayment(
        params.get("cardNumber"),
        params.get("cvv"),
        params.get("expiry")
    )),
    PAYPAL(params -> new PayPalPayment(params.get("email"))),
    CRYPTO(params -> new CryptoPayment(params.get("wallet")));

    private final PaymentStrategy strategy;

    PaymentType(java.util.function.Function<Map<String,String>, PaymentStrategy> factory) {
        this.strategy = factory.apply(getDefaultParams());
    }

    private static Map<String, String> getDefaultParams() {
        return new HashMap<>();
    }

    @Override
    public boolean pay(BigDecimal amount) {
        return strategy.pay(amount);
    }
}
```

### Testing Strategy Implementations

```java
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class StrategyTest {
    @Test
    void creditCardShouldProcessPayment() {
        var cc = new CreditCardPayment("4111-1111-1111-1111", "123", "12/28");
        assertTrue(cc.pay(new BigDecimal("100.00")));
    }

    @Test
    void shouldRejectNegativeAmounts() {
        var cc = new CreditCardPayment("4111-1111-1111-1111", "123", "12/28");
        assertFalse(cc.pay(new BigDecimal("-50.00")));
    }

    @Test
    void contextShouldAllowRuntimeSwap() {
        var ctx = new CheckoutContext();
        ctx.setPaymentStrategy(amt -> {
            System.out.println("Mock payment: " + amt);
            return true;
        });
        assertDoesNotThrow(() -> ctx.checkout(new BigDecimal("10.00")));
    }
}
```

## When to Use

1. **Payment Processing**: Multiple payment gateways with different APIs, each needing a separate algorithm but a common
   interface.

2. **Validation Rules**: Business validation that varies by jurisdiction (GDPR in EU, CCPA in California, LGPD in
   Brazil). Each region has different rules, but the validation engine should be the same.

3. **Compression Algorithms**: A file archiver supporting ZIP, GZIP, and TAR. Each algorithm is a strategy selected by
   file extension or user preference.

4. **Sorting/Mapping**: Collections that allow pluggable sorting strategies (e.g., sort by name, date, relevance,
   price). The `java.util.Comparator` interface is itself a Strategy.

5. **Authentication Mechanisms**: An application supporting OAuth2, JWT, LDAP, and SAML. Each authentication provider is
   a strategy plugged into the security filter chain.

### Framework Examples

- **Java `Comparator<T>`**: The quintessential Strategy pattern. Sorting algorithms are the Context, `Comparator` is the
  Strategy.
- **Spring `ResourceLoader`**: Different resource strategies for classpath, filesystem, and URL resources.
- **Jakarta Servlet `Filter`**: Filters form a chain, but each Filter is a Strategy for request/response processing.
- **Java `ThreadFactory`**: Strategy for creating new threads with custom names, daemon status, and priority.

## When NOT to Use

1. **Only 2-3 Simple Variants**: If you have just two algorithms and one is rarely used, a simple `boolean` flag +
   ternary might suffice. Strategy adds interface overhead.

2. **Algorithms Never Change at Runtime**: If the algorithm is fixed at compile-time and never swapped, Strategy adds
   unnecessary indirection. Use inheritance or simple method overloading instead.

3. **Performance-Critical Hot Paths**: Strategy adds a virtual method call. In loops executed millions of times (e.g.,
   pixel processing), this overhead matters. Consider `enum` strategies with `switch` expressions (Java 17+).

4. **Excessive Number of Strategies**: 50+ strategies become hard to manage. Consider a Rule Engine or Chain of
   Responsibility instead.

5. **Strategies Share Significant State**: If all strategies need access to the same large Context state, the interface
   becomes leaky. Consider refactoring to a Strategy that only receives what it needs.

## Interview Questions

### Q1: What is the Strategy pattern and when would you use it?

**Answer**: Strategy encapsulates interchangeable algorithms behind a common interface. Use it when you have multiple
variants of an algorithm that need to be selected at runtime, and you want to avoid conditional branching. It's ideal
for payment processing, validation rules, or any family of related behaviors.

### Q2: How does Strategy differ from State?

**Answer**: Strategy is about *how* to do something — the client explicitly chooses and swaps the algorithm. State is
about *what state the object is in* — the object's internal state changes behavior automatically. In Strategy, the
Context doesn't know the algorithm details. In State, the State knows the Context and can transition to other states.

### Q3: How does Strategy relate to the Open/Closed Principle?

**Answer**: Strategy perfectly exemplifies OCP: you can add new strategies without modifying existing Context or
Strategy interface code. The Context is closed for modification but open for extension through new ConcreteStrategy
classes.

### Q4: Can Strategy be implemented using Java lambdas?

**Answer**: Yes. Since `Strategy` is often a single-method interface, it's a functional interface in modern Java. You
can pass lambdas or method references directly. This is ideal for simple strategies but less suitable for complex ones
with internal state.

### Q5: How would you implement Strategy with Spring?

**Answer**: Inject a `Map<String, PaymentStrategy>` where keys are bean names. Then select the strategy by key based on
runtime conditions. Spring's `@Autowired Map<String, Strategy>` is a real-world use case that demonstrates Strategy with
dependency injection.

### Q6: What are the trade-offs of Strategy compared to a simple switch statement?

**Answer**: A switch is simpler for 2-3 cases but violates OCP and becomes unreadable beyond 5 cases. Strategy adds
classes and indirection but enables isolated testing, parallel development, and OCP compliance. For stable, small sets,
switch is pragmatic. For growing, dynamic sets, Strategy wins.

### Q7: How does the Strategy pattern interact with Dependency Injection?

**Answer**: Strategy and DI are complementary. DI frameworks like Spring can inject the appropriate Strategy
implementation based on configuration or profiles. This means the client code never creates strategies — it receives
them, which further reduces coupling.

### Q8: What is the "context" in Strategy pattern, and what responsibilities does it have?

**Answer**: The Context holds a reference to the Strategy and defines an interface that lets the strategy access data.
The Context may also contain common logic used by all strategies (logging, validation, transaction management). It
should NOT contain algorithm-specific logic.

### Follow-Up Question

**Interviewer**: "How would you design a Strategy pattern where some strategies need extra parameters that others
don't?"

**Answer**: Use a parameter object pattern. Pass a `PaymentRequest` DTO containing all optional fields. Each strategy
extracts only what it needs. Alternatively, the context can provide a callback or supplier for context-dependent data.
Avoid adding strategy-specific methods to the interface.

## Pros & Cons

### Advantages

- **Open/Closed Principle**: New strategies added without modifying existing code
- **Single Responsibility Principle**: Each strategy handles one algorithm variant
- **Runtime Flexibility**: Swap algorithms without restarting or recompiling
- **Isolated Testing**: Each strategy tested independently
- **Code Reuse**: Common logic lives in the Context or default interface methods
- **Replaces Inheritance**: Composition over inheritance — behaviors are plugged in rather than baked into class
  hierarchy

### Disadvantages

- **Class Explosion**: Each algorithm becomes a new class; many strategies create many small files
- **Client Awareness**: Client must know about available strategies to choose one
- **Interface Overhead**: All strategies must implement the same interface, which may force unnecessary parameters
- **Increased Indirection**: Following the code requires jumping between Context and Strategy interfaces
- **Over-Engineering Risk**: Easy to apply when a simple conditional would suffice

## Related Patterns

### Strategy vs State

These are often confused. **Strategy**: client chooses the algorithm explicitly. **State**: the object's behavior
changes automatically as its internal state changes. A `TcpConnection` changes behavior when it transitions from
CONNECTED to CLOSED (State). A `CheckoutService` lets the user pick CREDIT_CARD vs PAYPAL (Strategy).

### Strategy vs Template Method

**Template Method** defines the skeleton of an algorithm in a base class, letting subclasses override specific steps. *
*Strategy** composes the entire algorithm as a pluggable object. Template Method uses inheritance (static), Strategy
uses composition (dynamic). Choose Template Method when the algorithm structure is fixed and only steps vary. Choose
Strategy when whole algorithms are swapped.

### Strategy vs Command

**Command** encapsulates a single request as an object, enabling queuing, logging, and undo. **Strategy** encapsulates
an interchangeable algorithm. A Command is a one-shot action; a Strategy is a replaceable policy. A text editor's "Save"
button uses Command. A text editor's "Auto-save interval" uses Strategy.

## Key Takeaways

1. **"Favor composition over inheritance"** — Strategy is the textbook example of composing behavior via interfaces
   rather than inheriting it from superclasses.

2. **OCP champion** — New strategies never modify existing code. This is the single most important benefit to mention in
   interviews.

3. **Functional roots** — The Strategy pattern's interface is essentially a first-class function. Modern Java's lambdas
   make this pattern even more lightweight.

4. **Not just OOP** — Strategy applies beyond objects. You can use it at the microservice level: different service
   instances with different strategies for caching, rate limiting, or load balancing.

5. **Interview memory aid** — "Strategy = interchangeable algorithms, client picks, OCP compliant." Contrast with
   State ("automatic transitions, Context unaware").
