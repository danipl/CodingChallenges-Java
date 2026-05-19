# Chain of Responsibility Pattern

> Pass a request along a chain of handlers. Each handler decides to process or pass along.

## Why?

Decouple sender from receiver. Multiple objects can handle a request, and the chain determines who.

## Where?

- **Servlet Filters**: Each filter processes request, passes to next
- **Spring Security**: Filter chain for authentication/authorization
- **Logging**: Chain of log handlers (console → file → remote)
- **Middleware**: Express.js, Koa middleware pipeline

## How

```java
// 1. Handler interface
abstract class LogHandler {
    protected LogHandler next;

    void setNext(LogHandler next) { this.next = next; }

    abstract void handle(LogLevel level, String message);

    protected void handleNext(LogLevel level, String message) {
        if (next != null) {
            next.handle(level, message);
        }
    }
}

// 2. Concrete handlers
class ConsoleLogger extends LogHandler {
    void handle(LogLevel level, String message) {
        if (level == LogLevel.DEBUG || level == LogLevel.INFO) {
            System.out.println("[CONSOLE] " + level + ": " + message);
        }
        handleNext(level, message);
    }
}

class FileLogger extends LogHandler {
    void handle(LogLevel level, String message) {
        if (level == LogLevel.WARN || level == LogLevel.ERROR) {
            writeToFile(level, message);
        }
        handleNext(level, message);
    }
}

class EmailLogger extends LogHandler {
    void handle(LogLevel level, String message) {
        if (level == LogLevel.CRITICAL) {
            sendEmail(level, message);
        }
        // No handleNext — end of chain
    }
}

// 3. Build chain
LogHandler chain = new ConsoleLogger();
chain.setNext(new FileLogger());
chain.setNext(new EmailLogger());

// 4. Use
chain.handle(LogLevel.ERROR, "Database connection failed");
// → ConsoleLogger passes → FileLogger writes → EmailLogger ignores (not CRITICAL)
```

## Interview Application

- **Logger system**: Different log levels → different destinations
- **Authentication chain**: Validate token → check permissions → rate limit
- **Request validation**: Format check → schema check → business rules
- **ATM**: Dispense $100 → $50 → $20 → $10 → $5 → $1

## ATM Money Dispenser Example

```java
abstract class CurrencyDispenser {
    protected CurrencyDispenser next;
    protected int denomination;

    void setNext(CurrencyDispenser next) { this.next = next; }

    void dispense(int amount) {
        int count = amount / denomination;
        int remaining = amount % denomination;
        if (count > 0) {
            System.out.println("Dispense " + count + " x $" + denomination);
        }
        if (remaining > 0 && next != null) {
            next.dispense(remaining);
        }
    }
}

// Chain: $100 → $50 → $20 → $10 → $5 → $1
// dispense(275) → 2x$100, 1x$50, 1x$20, 1x$5
```
