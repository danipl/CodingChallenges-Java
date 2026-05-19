# Adapter Pattern

> Make incompatible interfaces work together.

## Why?

You have a client expecting interface A, but the available service implements interface B.

## Where?

- **Java**: `Arrays.asList()` adapts array to List interface
- **Spring**: `HandlerAdapter` adapts different controller types to a common interface
- **JDBC**: Each driver adapts its database protocol to the JDBC interface
- **External APIs**: Adapting third-party SDKs to your domain interfaces

## How

```java
// Client expects this
interface PaymentProcessor {
    void processPayment(BigDecimal amount, String currency);
}

// Third-party library has this
class StripeClient {
    void charge(double amount, String currencyCode, Map<String, String> metadata) {
        // Stripe-specific API
    }
}

// Adapter bridges the gap
class StripeAdapter implements PaymentProcessor {
    private final StripeClient stripe;

    StripeAdapter(StripeClient stripe) { this.stripe = stripe; }

    @Override
    public void processPayment(BigDecimal amount, String currency) {
        stripe.charge(amount.doubleValue(), currency, new HashMap<>());
    }
}

// Client code — unchanged
PaymentProcessor processor = new StripeAdapter(new StripeClient());
processor.processPayment(new BigDecimal("99.99"), "USD");
```

## Object Adapter vs Class Adapter

| Type | Mechanism | Java Preference |
|------|-----------|-----------------|
| Object Adapter | Composition (has-a) | Preferred |
| Class Adapter | Multiple inheritance | Not possible in Java |

## Interview Application

- **Legacy system migration**: Adapt old API to new interface
- **Multi-provider integration**: Each provider has different API, adapter normalizes
- **Testing**: Adapter wraps real service with mock during tests
