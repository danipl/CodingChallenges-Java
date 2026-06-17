# Factory Method Pattern

## 1. Title & Overview

The **Factory Method** pattern defines an interface for creating an object but lets subclasses decide which class to
instantiate. It solves the problem of tight coupling between client code and concrete product classes by delegating
object creation to subclasses through a virtual constructor. **One-line interview answer**: Factory Method lets a class
defer instantiation to subclasses, following the Open/Closed Principle by keeping object creation extensible without
modifying existing code.

## 2. Problem Statement

### Real-World Scenario

Consider a logistics application that initially only supports truck delivery. You have a `Logistics` class with a
`planDelivery()` method that creates a `Truck` object directly. This works fine until the business expands to support
sea transport with `Ships`. Now you must modify the `Logistics` class to add `if-else` or `switch` branches for every
new transport type. Each new transport type means reopening and modifying the core `Logistics` class.

### Why This Fails at Scale

In a microservices architecture, imagine a notification service that sends alerts via Email, SMS, Push, and Slack.
Without Factory Method, every `if (type == "email")` branch pollutes the coordinator class. Adding WebSocket support
means touching the same file, re-testing all existing flows, and risking regressions in production. This violates the
Open/Closed Principle — the class should be open for extension but closed for modification.

### Pain Points of Naive Approach

- **Violates Open/Closed Principle**: Every new product type requires modifying the creator class
- **Code Duplication**: Instantiation logic scattered across multiple clients
- **Tight Coupling**: Client code depends on concrete classes, not abstractions
- **Testing Difficulty**: Mocking concrete classes is harder than mocking interfaces
- **Hard to Centralize**: Cannot enforce consistent construction logic (logging, pooling, validation)

## 3. Solution

### How It Works

The Factory Method pattern replaces direct object construction (`new Truck()`) with a call to an abstract factory
method. Subclasses override this method to produce specific product types. The client code depends only on the `Product`
interface and the `Creator` abstraction.

### Key Participants

```
┌────────────────┐         ┌──────────────────┐
│   Product      │         │    Creator       │
│  (interface)   │         │  (abstract class) │
├────────────────┤         ├──────────────────┤
│ + operate()    │         │ + factoryMethod()│
└────────────────┘         │ + someOperation()│
         ▲                 └────────┬─────────┘
         │                          │
┌──────────────────┐       ┌──────────────────┐
│ ConcreteProduct  │       │ ConcreteCreator  │
│   (class)        │       │    (class)       │
├──────────────────┤       ├──────────────────┤
│ + operate()      │       │ + factoryMethod()│
└──────────────────┘       └──────────────────┘
```

- **Product** (`Transport`): Interface declaring the operations all products must support
- **ConcreteProduct** (`Truck`, `Ship`): Implementations of the Product interface
- **Creator** (`Logistics`): Declares the factory method returning a Product. May include a default implementation
- **ConcreteCreator** (`RoadLogistics`, `SeaLogistics`): Overrides the factory method to return a specific
  ConcreteProduct

### Step-by-Step Flow

1. **Client** calls `creator.someOperation()` — the creator method that internally uses the product
2. **Creator** calls its abstract `factoryMethod()` — unaware of which concrete product will be returned
3. **ConcreteCreator** overrides `factoryMethod()` to instantiate and return its specific `ConcreteProduct`
4. **Client** receives the `Product` interface, never knowing the concrete class it operates on
5. All calls to the product go through the `Product` interface, ensuring polymorphism

## 4. Java Implementation

### Basic Example: Logistics Application

```java
package creational.factory;

// Product interface
interface Transport {
    void deliver();
    double calculateCost(double distance);
}

// Concrete Products
class Truck implements Transport {
    @Override
    public void deliver() {
        System.out.println("Delivering by land in a truck.");
    }

    @Override
    public double calculateCost(double distance) {
        return distance * 1.5; // $1.5 per km
    }
}

class Ship implements Transport {
    @Override
    public void deliver() {
        System.out.println("Delivering by sea in a ship.");
    }

    @Override
    public double calculateCost(double distance) {
        return distance * 0.8; // $0.8 per km (cheaper per km)
    }
}

// Creator (abstract class)
abstract class Logistics {
    // Factory Method — subclasses override this
    public abstract Transport createTransport();

    // Business logic that uses the factory method
    public void planDelivery(String destination, double distance) {
        Transport transport = createTransport();
        transport.deliver();
        double cost = transport.calculateCost(distance);
        System.out.printf("Delivery to %s: $%.2f%n", destination, cost);
    }
}

// Concrete Creators
class RoadLogistics extends Logistics {
    @Override
    public Transport createTransport() {
        return new Truck();
    }
}

class SeaLogistics extends Logistics {
    @Override
    public Transport createTransport() {
        return new Ship();
    }
}

// Usage
public class FactoryMethodDemo {
    public static void main(String[] args) {
        Logistics logistics;

        // Decision could be based on config, environment, or user input
        String transportType = System.getenv("TRANSPORT_TYPE");
        if ("sea".equalsIgnoreCase(transportType)) {
            logistics = new SeaLogistics();
        } else {
            logistics = new RoadLogistics();
        }

        logistics.planDelivery("New York", 250.0);
    }
}
```

### Advanced Example: Document Converter with Parameterized Factory

```java
package creational.factory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Product interface with generics
interface Document<T> {
    T parse(String content);
    String serialize();
    String getFormat();
}

// Concrete Products
class JsonDocument implements Document<Map<String, Object>> {
    private Map<String, Object> data;

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> parse(String content) {
        // Simplified JSON parsing — in production use Jackson/Gson
        this.data = Map.of("content", content, "parsed", true);
        return data;
    }

    @Override
    public String serialize() {
        return "{\"format\": \"json\", \"data\": " + data + "}";
    }

    @Override
    public String getFormat() {
        return "json";
    }
}

class XmlDocument implements Document<StringBuilder> {
    private StringBuilder xmlContent;

    @Override
    public StringBuilder parse(String content) {
        this.xmlContent = new StringBuilder("<root>").append(content).append("</root>");
        return xmlContent;
    }

    @Override
    public String serialize() {
        return xmlContent != null ? xmlContent.toString() : "";
    }

    @Override
    public String getFormat() {
        return "xml";
    }
}

class CsvDocument implements Document<String> {
    private String raw;

    @Override
    public String parse(String content) {
        this.raw = content;
        return content;
    }

    @Override
    public String serialize() {
        return raw != null ? raw : "";
    }

    @Override
    public String getFormat() {
        return "csv";
    }
}

// Creator with registry (reflection-free, extensible)
abstract class DocumentConverter {
    private static final Map<String, DocumentConverter> registry = new ConcurrentHashMap<>();

    public abstract Document<?> createDocument();

    // Template method pattern combined with factory method
    public final String convert(String sourceContent) {
        Document<?> doc = createDocument();
        doc.parse(sourceContent);
        String result = doc.serialize();
        System.out.printf("[%s] Converted %d chars%n", doc.getFormat(), result.length());
        return result;
    }

    // Registry for factory lookup — no reflection, no switch statements
    public static void registerFormat(String format, DocumentConverter converter) {
        registry.put(format.toLowerCase(), converter);
    }

    public static DocumentConverter forFormat(String format) {
        DocumentConverter converter = registry.get(format.toLowerCase());
        if (converter == null) {
            throw new IllegalArgumentException("Unsupported format: " + format);
        }
        return converter;
    }
}

// Concrete Creators
class JsonConverter extends DocumentConverter {
    @Override
    public Document<?> createDocument() {
        return new JsonDocument();
    }
}

class XmlConverter extends DocumentConverter {
    @Override
    public Document<?> createDocument() {
        return new XmlDocument();
    }
}

class CsvConverter extends DocumentConverter {
    @Override
    public Document<?> createDocument() {
        return new CsvDocument();
    }
}

// Usage
public class DocumentConverterDemo {
    public static void main(String[] args) {
        // Register available converters (could be loaded via SPI)
        DocumentConverter.registerFormat("json", new JsonConverter());
        DocumentConverter.registerFormat("xml", new XmlConverter());
        DocumentConverter.registerFormat("csv", new CsvConverter());

        // Client code — no concrete classes referenced
        DocumentConverter converter = DocumentConverter.forFormat("json");
        String result = converter.convert("{\"name\": \"John\"}");
        System.out.println("Result: " + result);

        // Adding a new format requires NO changes to existing code
        // Just add a new class and register it
    }
}
```

### Java-Specific Note: Factory Method with Records (Java 17+)

```java
package creational.factory;

// Using sealed interface (Java 17) for product hierarchy
sealed interface PaymentMethod permits CreditCard, PayPal, CryptoWallet {
    boolean pay(double amount);
    String getType();
}

record CreditCard(String cardNumber, String cvv) implements PaymentMethod {
    @Override
    public boolean pay(double amount) {
        System.out.printf("Paid $%.2f with credit card %s%n", amount, mask(cardNumber));
        return true;
    }

    @Override
    public String getType() { return "credit_card"; }

    private String mask(String cc) {
        return "****" + cc.substring(cc.length() - 4);
    }
}

record PayPal(String email) implements PaymentMethod {
    @Override
    public boolean pay(double amount) {
        System.out.printf("Paid $%.2f via PayPal (%s)%n", amount, email);
        return true;
    }

    @Override
    public String getType() { return "paypal"; }
}

record CryptoWallet(String walletAddress, String currency) implements PaymentMethod {
    @Override
    public boolean pay(double amount) {
        System.out.printf("Paid %.6f %s from wallet %s%n", amount, currency, walletAddress);
        return true;
    }

    @Override
    public String getType() { return "crypto_" + currency; }
}

// Creator using sealed hierarchy + pattern matching (Java 21 preview)
abstract class PaymentProcessor {
    public abstract PaymentMethod createPayment(Map<String, String> config);

    public boolean processPayment(double amount, Map<String, String> config) {
        PaymentMethod method = createPayment(config);
        return method.pay(amount);
    }

    // Static factory method — alternative to the pattern
    public static PaymentProcessor forType(String type) {
        return switch (type) {
            case "credit_card" -> new CreditCardProcessor();
            case "paypal" -> new PayPalProcessor();
            case "crypto" -> new CryptoProcessor();
            default -> throw new IllegalArgumentException("Unknown: " + type);
        };
    }
}
```

## 5. When to Use

### Framework & Library Examples

| Framework                     | Usage                                                                       |
|-------------------------------|-----------------------------------------------------------------------------|
| **Spring Framework**          | `BeanFactory.getBean()` — factory method returning configured beans         |
| **Java Collection Framework** | `Iterator iterator()` — each collection implements its own iterator factory |
| **JDBC**                      | `DriverManager.getConnection()` — factory for database connections          |
| **javax.xml.parsers**         | `DocumentBuilderFactory.newInstance()` — creates XML parsers                |
| **Java NIO**                  | `Files.newBufferedReader()` — factory for reader objects                    |
| **JPA / Hibernate**           | `EntityManagerFactory.createEntityManager()`                                |

### Real-World Scenarios

1. **UI Component Libraries**: Buttons, text fields, and dropdowns vary across operating systems. A `Dialog` class uses
   a factory method `createButton()` so each OS subclass creates the appropriate native-looking button.

2. **Logging Frameworks**: `LoggerFactory.getLogger(Class<?>)` returns different logger implementations (Logback,
   Log4j2, SLF4J simple) based on classpath configuration without client code changes.

3. **Data Access Layers**: DAO factories let you switch between JDBC, JPA, or MongoDB implementations by swapping the
   concrete factory. Tests mock the factory to return in-memory DAOs.

4. **Document Converters**: An IDE export feature uses factory methods to produce PDF, HTML, Markdown, or LaTeX output
   from the same editing pipeline.

5. **Cloud Provider Abstraction**: A deployment tool supports AWS, GCP, and Azure. `CloudStorageProvider.createBucket()`
   is a factory method each cloud-specific subclass implements differently.

## 6. When NOT to Use

### Over-Engineering Warning

If your application creates only one type of product and has no plans to extend, a factory method is unnecessary
indirection. `new Truck()` is simpler and clearer.

### Simpler Alternatives

- **Simple Factory (not a GoF pattern)**: A static method that returns products based on a parameter (like
  `Calendar.getInstance()`) — sufficient when you don't need subclassing.
- **Direct Construction with Builder**: For products with many constructor parameters, a Builder is more appropriate.
- **Constructor Injection (DI)**: If a DI container (Spring) manages objects, the factory method may be redundant — the
  container itself acts as the factory.

### Performance Considerations

Each factory method call adds a virtual method dispatch. In hot paths (millions of creations), this overhead compounds.
Consider `Supplier<T>` memoization or object pooling if creation is expensive.

### When Not to Use Checklist

- □ Only one product type exists
- □ Product creation is trivial (`new` with no logic)
- □ You're using a DI framework that handles instantiation
- □ The product hierarchy mirrors the creator hierarchy exactly (code smell)
- □ You need to create product families (use Abstract Factory instead)

## 7. Interview Questions

**Q1: What is the difference between Factory Method and Simple Factory?**

A1: Simple Factory is a static method that creates objects based on a parameter — it's not a GoF pattern. Factory Method
uses inheritance: the creator class declares an abstract method, and subclasses implement it. Simple Factory puts all
creation logic in one place (violating OCP); Factory Method distributes creation across subclasses (following OCP).

**Q2: How does Factory Method support the Open/Closed Principle?**

A2: To add a new product, you create a new ConcreteProduct and a new ConcreteCreator subclass. The existing Creator,
ConcreteCreators, and all client code remain unchanged. The system is open for extension (new subclasses) but closed for
modification (no existing files changed).

**Q3: Can Factory Method return existing objects from a pool instead of new instances?**

A3: Yes. The factory method signature promises a Product interface, but nothing requires a fresh allocation.
Implementations can return cached, pooled, or proxied objects. For example, `Flyweight` pattern often hides behind a
factory method to return shared instances.

**Q4: What is the relationship between Factory Method and Template Method?**

A4: Factory Method is often called within a Template Method. The Creator class defines a template method (
`someOperation()`) that calls the abstract `factoryMethod()`. The template method controls the algorithm structure while
the factory method fills in the product creation step. They are complementary.

**Q5: When would you use an abstract class vs interface for the Creator?**

A5: Use an abstract class when the Creator has shared state or default behavior (like a base implementation of
`someOperation()`). Use an interface when the Creator is purely a contract and you want to support multiple inheritance
through interfaces. In modern Java, interfaces with `default` methods blur this line.

**Q6: How do you test code that uses Factory Method?**

A6: Test the ConcreteCreator separately to verify it creates the correct product. For client code that uses the Creator,
inject a mock Creator that returns a mock Product. This decoupling is a key benefit of the pattern — you can test
product and creator independently.

**Q7: What is the difference between Factory Method and Abstract Factory?**

A7: Factory Method creates one product via inheritance (single factory method). Abstract Factory creates families of
related products via composition (multiple factory methods grouped in one interface). Abstract Factory's implementation
often uses Factory Methods for each member of the family.

**Q8: How does Java 17+ sealed classes improve Factory Method implementations?**

A8: Sealed classes/interfaces restrict which classes can extend/implement the product type, making the hierarchy
exhaustive. The compiler knows all possible subtypes, enabling exhaustive `switch` pattern matching. This prevents
unauthorized product classes and helps IDEs flag missing cases in factory logic.

## 8. Pros & Cons

### Advantages

- **Open/Closed Principle**: New products added without modifying existing code
- **Single Responsibility Principle**: Creation logic moved to dedicated subclasses
- **Reduced Coupling**: Client code depends on Product interface, not concrete classes
- **Consistent Construction**: Factory method centralizes creation logic per product family
- **Hooks for Subclasses**: Template methods can call the factory method at specific points
- **Lazy Initialization**: Factory can defer or cache object creation

### Disadvantages

- **Class Explosion**: Each new product requires a new ConcreteCreator subclass
- **Complexity for Simple Cases**: Unnecessary abstraction when product variety is low
- **Instantiation Tied to Inheritance**: Forces subclassing even when composition would suffice
- **Parallel Hierarchy**: Product and creator hierarchies mirror each other, which can become maintenance burden
- **Difficulty with Static Methods**: Java constructors can't be overridden; static factory methods are alternatives but
  aren't inheritable

## 9. Related Patterns

| Pattern              | Relationship                                                                                                                                                                                                                   |
|----------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Abstract Factory** | Often implemented via Factory Methods. Where Factory Method creates one product, Abstract Factory creates a family of related products through multiple factory methods.                                                       |
| **Template Method**  | Factory Method is a specialization of Template Method focused on object creation. Template Method defines an algorithm skeleton; one step often calls a Factory Method.                                                        |
| **Prototype**        | An alternative to Factory Method. Instead of creating objects via subclasses, Prototype copies existing instances. Useful when creation is expensive (copying avoids this) or when object types must be determined at runtime. |

### How to Choose

- Use **Factory Method** when a class can't anticipate the class of objects it must create
- Use **Abstract Factory** when products come in families that must be used together
- Use **Prototype** when instantiation is expensive or you need to avoid subclassing
- Use **Builder** when a product requires a multi-step construction process

## 10. Key Takeaways

- **Defer to subclasses**: The core idea is letting subclasses decide which concrete class to instantiate
- **Open/Closed Principle**: This is the SOLID principle most directly demonstrated by Factory Method
- **Dependency Inversion**: Client code depends on abstractions (Product), not concretions
- **Framework architects' pattern**: Used extensively in frameworks (Spring, JDBC, Java Collections) because frameworks
  must support extensibility by end users
- **Interview memory aid**: "Factory Method = virtual constructor. The creator class defines the interface for object
  creation but lets subclasses alter the type of objects that will be created."
