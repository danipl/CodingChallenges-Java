# 1.3 Dependency Injection (DI)

> DI is the practical application of DIP. It's the backbone of Spring, Guice, and modern frameworks.

## What is DI?

Instead of a class creating its dependencies, they are **provided** (injected) from outside.

```java
// WITHOUT DI — tight coupling
class OrderService {
    private PaymentGateway gateway = new StripeGateway();
}

// WITH DI — loose coupling
class OrderService {
    private final PaymentGateway gateway;
    OrderService(PaymentGateway gateway) { this.gateway = gateway; }
}
```

## Three Types of Injection

### 1. Constructor Injection (Preferred)
```java
class OrderService {
    private final PaymentGateway gateway;
    private final EmailService emailer;

    OrderService(PaymentGateway gateway, EmailService emailer) {
        this.gateway = gateway;
        this.emailer = emailer;
    }
}
```
- **Pros**: Immutability, clear dependencies, testable
- **Cons**: Many params = SRP violation signal

### 2. Setter Injection
```java
class OrderService {
    private PaymentGateway gateway;
    void setPaymentGateway(PaymentGateway gateway) { this.gateway = gateway; }
}
```
- **Use when**: Optional dependencies, circular dependencies

### 3. Field Injection (Avoid)
```java
class OrderService {
    @Inject PaymentGateway gateway;  // Hard to test, hides dependencies
}
```
- **Avoid**: Makes testing harder, hides true dependencies

## DI Container vs Manual DI

### Manual DI (What you'll do in interviews)
```java
public class App {
    public static void main(String[] args) {
        Database db = new PostgreSQLDatabase("jdbc:...");
        UserRepository users = new UserRepository(db);
        EmailService email = new EmailService("smtp://...");
        OrderService orders = new OrderService(users, email);
        // Wire everything at composition root
    }
}
```

### DI Container (Spring, Guice — know conceptually)
```java
// Spring example — know this exists, but write manual DI in interviews
@Component
class OrderService {
    @Autowired OrderService(UserRepository repo, EmailService email) { ... }
}
```

## Dependency Inversion → DI Pipeline

```
1. Identify high-level module (OrderService)
2. Identify low-level module (MySQLDatabase)
3. Create abstraction (interface Database)
4. High-level depends on abstraction
5. Inject concrete implementation at runtime
```

## Interview Patterns

### Composition Root
The single place in your app where you wire everything together:
```java
// In interviews, this is usually main() or your App class
class CompositionRoot {
    static App createApp() {
        var db = new InMemoryDatabase();
        var repo = new UserRepository(db);
        var service = new UserService(repo);
        return new App(service);
    }
}
```

### Testing with DI
```java
@Test
void testOrderService() {
    PaymentGateway mockGateway = new MockPaymentGateway();  // Inject mock
    OrderService service = new OrderService(mockGateway);
    // Test in isolation
}
```

## Common Interview Questions

1. **What's the difference between DI and DI Container?**
   - DI is a pattern; a container is a framework that automates it

2. **When would you NOT use DI?**
   - Value objects, simple data classes, utility methods

3. **How does DI relate to SOLID?**
   - Direct application of DIP; enables SRP and OCP

4. **What's a Composition Root?**
   - The single location where the object graph is assembled
