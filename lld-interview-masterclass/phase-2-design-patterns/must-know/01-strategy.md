# Strategy Pattern — The Most Important Pattern

> Solves 60-70% of common LLD interview problems. Master this first.

## Why Do We Need It?

When you have **multiple algorithms** for the same task and need to **swap them at runtime** without `if/else` or `switch` chains.

## Where Is It Used in Real Systems?

| System | Strategy |
|--------|----------|
| Payment processing | CreditCard, PayPal, Crypto, UPI strategies |
| Sorting | QuickSort, MergeSort, TimSort strategies |
| Routing | ShortestPath, FastestRoute, ScenicRoute |
| Compression | GZIP, LZ4, ZSTD strategies |
| Authentication | OAuth, JWT, SAML, API Key strategies |
| Pricing | RegularPrice, SalePrice, MemberPrice |

## How to Implement

### Step 1: Define the Strategy Interface
```java
public interface PaymentStrategy {
    boolean pay(BigDecimal amount);
}
```

### Step 2: Implement Concrete Strategies
```java
public class CreditCardPayment implements PaymentStrategy {
    private final String cardNumber;
    private final String cvv;
    public CreditCardPayment(String cardNumber, String cvv) {
        this.cardNumber = cardNumber;
        this.cvv = cvv;
    }
    @Override
    public boolean pay(BigDecimal amount) {
        // Process credit card payment
        return true;
    }
}

public class PayPalPayment implements PaymentStrategy {
    private final String email;
    public PayPalPayment(String email) { this.email = email; }
    @Override
    public boolean pay(BigDecimal amount) {
        // Process PayPal payment
        return true;
    }
}

public class CryptoPayment implements PaymentStrategy {
    private final String walletAddress;
    public CryptoPayment(String walletAddress) { this.walletAddress = walletAddress; }
    @Override
    public boolean pay(BigDecimal amount) {
        // Process crypto payment
        return true;
    }
}
```

### Step 3: Use Strategy in Context
```java
public class ShoppingCart {
    private PaymentStrategy paymentStrategy;

    public void setPaymentStrategy(PaymentStrategy strategy) {
        this.paymentStrategy = strategy;
    }

    public boolean checkout(BigDecimal amount) {
        if (paymentStrategy == null) {
            throw new IllegalStateException("Payment strategy not set");
        }
        return paymentStrategy.pay(amount);
    }
}
```

### Step 4: Client Code
```java
ShoppingCart cart = new ShoppingCart();
cart.setPaymentStrategy(new CreditCardPayment("4111...", "123"));
cart.checkout(new BigDecimal("99.99"));

// Swap strategy at runtime — no code changes needed
cart.setPaymentStrategy(new PayPalPayment("user@email.com"));
cart.checkout(new BigDecimal("49.99"));
```

## SOLID Principles Satisfied

| Principle | How Strategy Satisfies It |
|-----------|--------------------------|
| OCP | Add new strategies without modifying context |
| SRP | Each strategy handles one algorithm |
| DIP | Context depends on abstraction, not concrete classes |
| LSP | Any strategy can replace another |

## Interview Template

```java
// 1. Interface
interface [Algorithm]Strategy { [ReturnType] execute([Params]); }

// 2. Concrete implementations
class [ConcreteA]Strategy implements [Algorithm]Strategy { ... }
class [ConcreteB]Strategy implements [Algorithm]Strategy { ... }

// 3. Context
class Context {
    private [Algorithm]Strategy strategy;
    void setStrategy([Algorithm]Strategy s) { this.strategy = s; }
    [ReturnType] doWork([Params]) { return strategy.execute([Params]); }
}
```

## Common Interview Problems Solved by Strategy

1. **Parking Lot** — Multiple payment types (cash, card, subscription)
2. **E-commerce** — Multiple pricing/discount strategies
3. **File compressor** — Multiple compression algorithms
4. **Notification system** — Email, SMS, Push strategies
5. **Routing** — Multiple path-finding algorithms

## Anti-Pattern: What Strategy Replaces

```java
// BAD — violates OCP
class PaymentProcessor {
    void process(String type, BigDecimal amount) {
        switch (type) {
            case "CREDIT": /* ... */ break;
            case "DEBIT": /* ... */ break;
            case "PAYPAL": /* ... */ break;
            case "CRYPTO": /* ... */ break;
            // Every new type requires modification
        }
    }
}
```
