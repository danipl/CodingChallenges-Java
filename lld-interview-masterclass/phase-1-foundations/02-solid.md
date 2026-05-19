# 1.2 SOLID Principles

> SOLID explains 70-80% of design patterns. Don't memorize — learn to spot violations and refactor.

## S — Single Responsibility Principle (SRP)

**Definition**: A class should have one, and only one, reason to change.

### Violation
```java
class UserService {
    void createUser(User user) { /* DB logic */ }
    void sendWelcomeEmail(User user) { /* Email logic */ }
    void generateReport(List<User> users) { /* Report logic */ }
}
```

### Fix
```java
class UserService { void createUser(User user) {} }
class EmailService { void sendWelcomeEmail(User user) {} }
class ReportService { void generateReport(List<User> users) {} }
```

**Interview check**: If you can describe the class with "and", it likely violates SRP.

---

## O — Open/Closed Principle (OCP)

**Definition**: Open for extension, closed for modification.

### Violation
```java
class PaymentProcessor {
    void process(Payment payment) {
        if (payment.getType() == "credit") { /* ... */ }
        else if (payment.getType() == "debit") { /* ... */ }
        else if (payment.getType() == "paypal") { /* ... */ }
        // New payment type = modify this class
    }
}
```

### Fix (Strategy Pattern)
```java
interface PaymentStrategy { void process(Payment payment); }
class CreditPayment implements PaymentStrategy { /* ... */ }
class DebitPayment implements PaymentStrategy { /* ... */ }

class PaymentProcessor {
    void process(Payment payment, PaymentStrategy strategy) {
        strategy.process(payment);  // Add new types without modification
    }
}
```

**Interview check**: Adding a new feature requires modifying existing code → OCP violation.

---

## L — Liskov Substitution Principle (LSP)

**Definition**: Subtypes must be substitutable for their base types without breaking behavior.

### Violation
```java
class Rectangle {
    void setWidth(int w) { this.width = w; }
    void setHeight(int h) { this.height = h; }
}

class Square extends Rectangle {
    @Override void setWidth(int w) { this.width = w; this.height = w; }
    @Override void setHeight(int h) { this.height = h; this.width = h; }
}
// Breaks: client expects setWidth to not affect height
```

### Fix
```java
interface Shape { int getArea(); }
class Rectangle implements Shape { /* ... */ }
class Square implements Shape { /* ... */ }
// No inheritance relationship between Rectangle and Square
```

**Interview check**: If a subclass throws `UnsupportedOperationException`, LSP is violated.

---

## I — Interface Segregation Principle (ISP)

**Definition**: Clients should not be forced to depend on methods they don't use.

### Violation
```java
interface Printer {
    void print();
    void scan();
    void fax();
}

class SimplePrinter implements Printer {
    void print() { /* OK */ }
    void scan() { throw new UnsupportedOperationException(); }
    void fax() { throw new UnsupportedOperationException(); }
}
```

### Fix
```java
interface Printable { void print(); }
interface Scannable { void scan(); }
interface Faxable { void fax(); }

class SimplePrinter implements Printable { /* ... */ }
class MultiFunctionDevice implements Printable, Scannable, Faxable { /* ... */ }
```

**Interview check**: Fat interfaces → split them.

---

## D — Dependency Inversion Principle (DIP)

**Definition**: High-level modules should not depend on low-level modules. Both should depend on abstractions.

### Violation
```java
class OrderService {
    private MySQLDatabase db = new MySQLDatabase();  // Depends on concrete class
}
```

### Fix
```java
interface Database { void save(Order order); }
class OrderService {
    private final Database db;
    OrderService(Database db) { this.db = db; }  // Depends on abstraction
}
```

**Interview check**: `new` keyword inside business logic classes → DIP violation.

---

## SOLID Violation Quick Reference

| Symptom | Violated Principle | Fix |
|---------|-------------------|-----|
| Class > 300 lines | SRP | Split responsibilities |
| Giant `if/switch` on type | OCP | Strategy/Factory pattern |
| `UnsupportedOperationException` | LSP | Redesign hierarchy |
| Empty method implementations | ISP | Split interface |
| `new` in business logic | DIP | Constructor injection |

## Practice Exercises

1. Given a `FileManager` class that reads, writes, compresses, and encrypts — identify violations and refactor.
2. A `NotificationService` sends email, SMS, and push. Adding Slack requires modifying the class. Fix it.
3. Why does `java.util.Stack extends Vector` violate LSP?
