# Factory Pattern

> Encapsulate object creation. Clients don't know or care about concrete classes.

## Why?

Object creation logic is complex or depends on runtime input. Decouple "what" from "how".

## Types

### 1. Simple Factory
```java
class NotificationFactory {
    static Notification create(String type) {
        return switch (type.toLowerCase()) {
            case "email" -> new EmailNotification();
            case "sms" -> new SMSNotification();
            case "push" -> new PushNotification();
            default -> throw new IllegalArgumentException("Unknown: " + type);
        };
    }
}
```

### 2. Factory Method (subclass decides)
```java
abstract class DocumentCreator {
    abstract Document createDocument();  // Subclass implements

    void openDocument() {
        Document doc = createDocument();
        doc.open();
    }
}

class PDFCreator extends DocumentCreator {
    Document createDocument() { return new PDFDocument(); }
}
```

### 3. Abstract Factory (families of related objects)
```java
interface UIFactory {
    Button createButton();
    Checkbox createCheckbox();
}

class WindowsFactory implements UIFactory {
    Button createButton() { return new WindowsButton(); }
    Checkbox createCheckbox() { return new WindowsCheckbox(); }
}

class MacFactory implements UIFactory {
    Button createButton() { return new MacButton(); }
    Checkbox createCheckbox() { return new MacCheckbox(); }
}
```

## Where?

- **JDBC**: `DriverManager.getConnection()` — factory for connections
- **Spring**: `BeanFactory` — creates and manages beans
- **Logging**: `LoggerFactory.getLogger()` — returns appropriate logger

## Interview Application

- **Parser factory**: JSON, XML, CSV parsers based on file extension
- **Database connection factory**: PostgreSQL, MySQL, SQLite
- **Payment gateway factory**: Stripe, PayPal, Razorpay

## SOLID

| Principle | How Factory Satisfies |
|-----------|----------------------|
| OCP | Add new products without changing client code |
| DIP | Client depends on product interface, not concrete class |
| SRP | Creation logic separated from business logic |
