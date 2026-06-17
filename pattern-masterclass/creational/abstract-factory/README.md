# Abstract Factory Pattern

## 1. Title & Overview

The **Abstract Factory** pattern provides an interface for creating families of related or dependent objects without
specifying their concrete classes. It solves the problem of ensuring that products from the same family are used
together while keeping client code independent of how those products are created. **One-line interview answer**:
Abstract Factory composes multiple Factory Methods into a single interface to create families of related objects,
ensuring compatibility within each family.

## 2. Problem Statement

### Real-World Scenario

Build a cross-platform UI framework that needs to render buttons, checkboxes, and text fields consistently on Windows,
macOS, and Linux. If you let the application create UI elements directly, nothing prevents a Windows-styled button from
appearing next to a macOS-styled checkbox in the same dialog. Each new platform requires scattered `if (os == WINDOWS)`
checks throughout the codebase.

### Why This Fails at Scale

Consider a furniture shop simulator with Victorian, Modern, and Art Deco furniture families. Each family includes a
Chair, Sofa, and CoffeeTable — they must match aesthetically. Without Abstract Factory, mixing `VictorianChair` with
`ModernSofa` happens when different developers work on different screens. In a real codebase with 50+ UI screens,
enforcing family consistency becomes impossible without a centralized factory.

### Pain Points of Naive Approach

- **Inconsistent Families**: Concrete classes from different families end up mixed in the same context
- **Scattered Conditionals**: OS/style checks duplicated across every product creation site
- **Violates OCP**: Adding a new family means finding and modifying every creation point
- **Hard to Swap Families**: Replacing one family for another requires changes everywhere
- **No Compile-Time Safety**: Wrong combinations compile and appear only at runtime as ugly UIs or integration bugs

## 3. Solution

### How It Works

The Abstract Factory defines an interface with multiple factory methods — one for each product type in the family. Each
concrete factory implements all methods to produce a complete, consistent family. Client code uses only the factory
interface and product interfaces, never referencing concrete classes.

### Key Participants

```
┌──────────────────────┐     ┌───────────────────────┐
│   AbstractFactory    │     │    AbstractProductA   │
├──────────────────────┤     ├───────────────────────┤
│ + createProductA()   │     │ + operationA()        │
│ + createProductB()   │     └───────────────────────┘
└──────────┬───────────┘                ▲
           │                            │
┌──────────┴───────────┐     ┌───────────────────────┐
│  ConcreteFactory1    │     │   ConcreteProductA1   │
│  ConcreteFactory2    │     │   ConcreteProductA2   │
├──────────────────────┤     ├───────────────────────┤
│ + createProductA()   │     │ + operationA()        │
│ + createProductB()   │     └───────────────────────┘
└──────────────────────┘
```

- **AbstractFactory** (`UIFactory`): Interface declaring creation methods for each product type
- **ConcreteFactory** (`WinFactory`, `MacFactory`): Implements creation methods to produce specific product variants
- **AbstractProduct** (`Button`, `Checkbox`): Interface for a type of product object
- **ConcreteProduct** (`WinButton`, `MacButton`): Defines a product to be created by the corresponding ConcreteFactory
- **Client**: Uses only AbstractFactory and AbstractProduct interfaces

### Step-by-Step Flow

1. **Client** receives an AbstractFactory (often injected at startup or read from config)
2. When the client needs a Button, it calls `factory.createButton()` — no type parameter, no conditionals
3. The **ConcreteFactory** returns the correct variant (e.g., `WinButton`)
4. When the same client needs a Checkbox, it calls `factory.createCheckbox()` — guaranteeing `WinCheckbox` to match
5. The client working with `Button` and `Checkbox` interfaces never knows which platform it runs on
6. To support a new platform, add a new ConcreteFactory — no client code changes needed

## 4. Java Implementation

### UI Framework Example

```java
package creational.abstractfactory;

// ─── Abstract Products ───
interface Button {
    void render();
    void onClick(Runnable action);
}

interface Checkbox {
    void render();
    boolean isChecked();
    void setChecked(boolean checked);
}

interface ScrollBar {
    void scrollTo(int position);
    int getPosition();
}

// ─── Concrete Products for Windows ───
class WinButton implements Button {
    @Override
    public void render() {
        System.out.println("[Win] Rendering a rectangular, flat button");
    }

    @Override
    public void onClick(Runnable action) {
        System.out.println("[Win] Registering click handler via Win32 API");
        action.run();
    }
}

class WinCheckbox implements Checkbox {
    private boolean checked;

    @Override
    public void render() {
        System.out.println("[Win] Rendering checkbox with square indicator");
    }

    @Override
    public boolean isChecked() { return checked; }

    @Override
    public void setChecked(boolean checked) { this.checked = checked; }
}

class WinScrollBar implements ScrollBar {
    private int position;

    @Override
    public void scrollTo(int position) {
        this.position = position;
        System.out.println("[Win] Scrolled to " + position);
    }

    @Override
    public int getPosition() { return position; }
}

// ─── Concrete Products for macOS ───
class MacButton implements Button {
    @Override
    public void render() {
        System.out.println("[Mac] Rendering a pill-shaped, gradient button");
    }

    @Override
    public void onClick(Runnable action) {
        System.out.println("[Mac] Registering click handler via Cocoa API");
        action.run();
    }
}

class MacCheckbox implements Checkbox {
    private boolean checked;

    @Override
    public void render() {
        System.out.println("[Mac] Rendering checkbox with circular indicator");
    }

    @Override
    public boolean isChecked() { return checked; }

    @Override
    public void setChecked(boolean checked) { this.checked = checked; }
}

class MacScrollBar implements ScrollBar {
    private int position;

    @Override
    public void scrollTo(int position) {
        this.position = position;
        System.out.println("[Mac] Smooth-scrolled to " + position);
    }

    @Override
    public int getPosition() { return position; }
}

// ─── Abstract Factory ───
interface UIFactory {
    Button createButton();
    Checkbox createCheckbox();
    ScrollBar createScrollBar();
}

// ─── Concrete Factories ───
class WinFactory implements UIFactory {
    @Override
    public Button createButton() { return new WinButton(); }

    @Override
    public Checkbox createCheckbox() { return new WinCheckbox(); }

    @Override
    public ScrollBar createScrollBar() { return new WinScrollBar(); }
}

class MacFactory implements UIFactory {
    @Override
    public Button createButton() { return new MacButton(); }

    @Override
    public Checkbox createCheckbox() { return new MacCheckbox(); }

    @Override
    public ScrollBar createScrollBar() { return new MacScrollBar(); }
}

// ─── Factory Registry (config-driven) ───
class UIFactoryProvider {
    public static UIFactory getFactory() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return new WinFactory();
        }
        // Default to Mac — in prod, add Linux variant
        return new MacFactory();
    }
}

// ─── Client Code ───
class Application {
    private final Button button;
    private final Checkbox checkbox;
    private final ScrollBar scrollBar;

    // Factory is injected — client never references concrete products
    public Application(UIFactory factory) {
        this.button = factory.createButton();
        this.checkbox = factory.createCheckbox();
        this.scrollBar = factory.createScrollBar();
    }

    public void renderUI() {
        button.render();
        checkbox.render();
        scrollBar.scrollTo(10);
    }

    public void simulateUserAction() {
        button.onClick(() -> System.out.println("User clicked!"));
        checkbox.setChecked(true);
    }
}

// ─── Demo ───
public class AbstractFactoryDemo {
    public static void main(String[] args) {
        UIFactory factory = UIFactoryProvider.getFactory();
        Application app = new Application(factory);
        app.renderUI();
        app.simulateUserAction();

        // Switching to Windows — change factory, zero client changes
        Application windowsApp = new Application(new WinFactory());
        windowsApp.renderUI();
    }
}
```

### Advanced Example: Database Abstraction with Connection Pooling

```java
package creational.abstractfactory;

import java.sql.Connection;
import java.util.concurrent.atomic.AtomicInteger;

// ─── Abstract Products ───
interface DbConnection {
    Connection getConnection();
    boolean isClosed();
    void close();
}

interface DbQuery {
    <T> T execute(String sql, Class<T> returnType);
}

interface DbTransaction {
    void begin();
    void commit();
    void rollback();
}

// ─── Abstract Factory ───
interface DatabaseFactory {
    DbConnection createConnection();
    DbQuery createQuery();
    DbTransaction createTransaction();
}

// ─── Concrete Factory for PostgreSQL ───
class PostgresFactory implements DatabaseFactory {
    private final String connectionString;

    public PostgresFactory(String connectionString) {
        this.connectionString = connectionString;
    }

    @Override
    public DbConnection createConnection() {
        return new PostgresConnection(connectionString);
    }

    @Override
    public DbQuery createQuery() {
        return new PostgresQuery();
    }

    @Override
    public DbTransaction createTransaction() {
        return new PostgresTransaction();
    }
}

// Concrete products for PostgreSQL
class PostgresConnection implements DbConnection {
    private final String connString;
    private boolean closed = false;

    PostgresConnection(String connString) { this.connString = connString; }

    @Override
    public Connection getConnection() {
        System.out.println("[Postgres] Opening connection to " + connString);
        return null; // Would return pg JDBC Connection
    }

    @Override
    public boolean isClosed() { return closed; }

    @Override
    public void close() {
        System.out.println("[Postgres] Closing connection");
        this.closed = true;
    }
}

class PostgresQuery implements DbQuery {
    private static final AtomicInteger queryCount = new AtomicInteger(0);

    @Override
    public <T> T execute(String sql, Class<T> returnType) {
        int n = queryCount.incrementAndGet();
        System.out.printf("[Postgres] Executing query #%d: %s%n", n, sql);
        return null; // Would map ResultSet to T
    }
}

class PostgresTransaction implements DbTransaction {
    @Override
    public void begin() { System.out.println("[Postgres] BEGIN TRANSACTION"); }

    @Override
    public void commit() { System.out.println("[Postgres] COMMIT"); }

    @Override
    public void rollback() { System.out.println("[Postgres] ROLLBACK"); }
}

// Factory provider — config could change at runtime
class DatabaseFactoryProvider {
    public static DatabaseFactory forDatabase(String dbType, String connectionString) {
        return switch (dbType.toLowerCase()) {
            case "postgres" -> new PostgresFactory(connectionString);
            case "mysql" -> new MySQLFactory(connectionString);
            case "h2" -> new H2Factory(connectionString);
            default -> throw new IllegalArgumentException("Unknown: " + dbType);
        };
    }
}

// MySQL and H2 factories would follow the same pattern
class MySQLFactory implements DatabaseFactory {
    private final String connString;
    MySQLFactory(String connString) { this.connString = connString; }

    @Override
    public DbConnection createConnection() {
        return new MySQLConnection(connString);
    }

    @Override
    public DbQuery createQuery() {
        System.out.println("[MySQL] Creating MySQL query executor");
        return new MySQLQuery();
    }

    @Override
    public DbTransaction createTransaction() {
        System.out.println("[MySQL] Creating MySQL transaction");
        return new MySQLTransaction();
    }
}

// Stub concrete products for completeness
class MySQLConnection implements DbConnection {
    private final String cs;
    MySQLConnection(String cs) { this.cs = cs; }
    @Override public Connection getConnection() { System.out.println("[MySQL] Connect: " + cs); return null; }
    @Override public boolean isClosed() { return false; }
    @Override public void close() { System.out.println("[MySQL] Close"); }
}
class MySQLQuery implements DbQuery {
    @Override public <T> T execute(String sql, Class<T> rt) { System.out.println("[MySQL] Query: " + sql); return null; }
}
class MySQLTransaction implements DbTransaction {
    @Override public void begin() { System.out.println("[MySQL] BEGIN"); }
    @Override public void commit() { System.out.println("[MySQL] COMMIT"); }
    @Override public void rollback() { System.out.println("[MySQL] ROLLBACK"); }
}
class H2Factory implements DatabaseFactory {
    private final String cs;
    H2Factory(String cs) { this.cs = cs; }
    @Override public DbConnection createConnection() { return new H2Connection(cs); }
    @Override public DbQuery createQuery() { System.out.println("[H2] In-memory query"); return new H2Query(); }
    @Override public DbTransaction createTransaction() { System.out.println("[H2] Transaction"); return new H2Transaction(); }
}
class H2Connection implements DbConnection {
    private final String cs;
    H2Connection(String cs) { this.cs = cs; }
    @Override public Connection getConnection() { System.out.println("[H2] In-mem: " + cs); return null; }
    @Override public boolean isClosed() { return false; }
    @Override public void close() { System.out.println("[H2] Close"); }
}
class H2Query implements DbQuery {
    @Override public <T> T execute(String sql, Class<T> rt) { System.out.println("[H2] Execute: " + sql); return null; }
}
class H2Transaction implements DbTransaction {
    @Override public void begin() { System.out.println("[H2] BEGIN"); }
    @Override public void commit() { System.out.println("[H2] COMMIT"); }
    @Override public void rollback() { System.out.println("[H2] ROLLBACK"); }
}

// ─── Demo ───
public class DatabaseAbstractionDemo {
    public static void main(String[] args) {
        // In production, this comes from application config
        String dbType = System.getProperty("db.type", "postgres");
        String connStr = System.getProperty("db.url", "jdbc:postgresql://localhost:5432/mydb");

        DatabaseFactory dbFactory = DatabaseFactoryProvider.forDatabase(dbType, connStr);

        // All operations use the factory — switching database engine is a config change
        DbConnection conn = dbFactory.createConnection();
        DbQuery query = dbFactory.createQuery();
        DbTransaction tx = dbFactory.createTransaction();

        tx.begin();
        query.execute("SELECT * FROM users", String.class);
        tx.commit();
        conn.close();
    }
}
```

## 5. When to Use

### Framework & Library Examples

| Framework      | Usage                                                                                                        |
|----------------|--------------------------------------------------------------------------------------------------------------|
| **Java Swing** | `LookAndFeel` hierarchy — each LAF is a factory for UI components (buttons, menus, scrollbars)               |
| **Spring**     | `ProxyFactoryBean` creates AOP proxies; `PlatformTransactionManagerFactory` creates TX managers              |
| **JAXP**       | `DocumentBuilderFactory`, `SAXParserFactory`, `TransformerFactory` — each produces XML processing families   |
| **JavaFX**     | `SkinFactory` creates skin families for controls consistent with the chosen theme                            |
| **Hibernate**  | `Dialect` per database — each dialect factory produces SQL generation, type mapping, and identity strategies |

### Real-World Scenarios

1. **Cross-Platform UI Toolkits**: One factory per OS (Windows, macOS, Linux, mobile). Ensures all widgets in a dialog
   follow the same platform look-and-feel.

2. **Cloud Provider Abstractions**: Each cloud provider (AWS, GCP, Azure) has a factory creating compute instances,
   storage buckets, and load balancers that work together in that cloud's ecosystem.

3. **Theme/Skin Engines**: Dark theme, light theme, and high-contrast theme each have their own factory producing a
   consistent set of colors, fonts, and icons.

4. **Database Migrations Supporting Multiple Engines**: Each database (PostgreSQL, MySQL, H2) gets its own factory
   producing SQL dialect renderers, type mappers, and transaction strategies.

5. **Report Generation in Multiple Formats**: A reporting system uses one factory per output format (PDF, Excel, HTML,
   CSV) to produce consistent document elements (headers, tables, charts) in the target format.

## 6. When NOT to Use

### Over-Engineering Warning

If your product family has only one variant (one factory), Abstract Factory is overkill. A single Factory Method or
Simple Factory suffices. The crime is building an entire factory hierarchy for "what if we need another variant
someday" — YAGNI.

### Simpler Alternatives

- **Factory Method + Dependency Injection**: If your DI container can inject grouped dependencies, you may not need
  Abstract Factory. Spring `@Configuration` classes with `@Bean` methods can group related beans.
- **Builder**: For constructing a single complex object that has variant configurations, use Builder instead.
- **Prototype**: If families differ only by data (not behavior), cloning a configured prototype may be simpler.

### Performance Considerations

Each product creation goes through an interface method call (virtual dispatch) + the concrete implementation. For most
applications this is negligible. However, in tight loops creating millions of objects, consider:

- Caching factory instances (they're stateless, so one per variant)
- Object pooling for expensive products
- Direct construction if profiling shows factory overhead

### When Not to Use Checklist

- □ Product family has only one variant
- □ Products do not need to be used together consistently
- □ Product types rarely change or extend
- □ A DI container already groups and injects related beans
- □ Factory Method by itself (single creation per factory) is sufficient

## 7. Interview Questions

**Q1: Explain the key difference between Abstract Factory and Factory Method.**

A1: Factory Method creates one product using inheritance — a single abstract method overridden by subclasses. Abstract
Factory creates families of related products using composition — an interface with multiple creation methods, each of
which often uses Factory Method internally. Factory Method is about one product; Abstract Factory is about coordinating
multiple products.

**Q2: How does Abstract Factory ensure product compatibility within a family?**

A2: Each ConcreteFactory creates products from a single variant. When the client uses the same factory for all products
in a context, every Button, Checkbox, and ScrollBar belongs to the same family. There is no code path that mixes
variants because the client never calls `new WinButton()` — it only calls `factory.createButton()`.

**Q3: How would you implement Abstract Factory with Java records and sealed classes (17+)?**

A3: Make `Button`, `Checkbox`, and `ScrollBar` sealed interfaces. Each ConcreteFactory returns records implementing
these sealed types. The `UIFactory` itself can be a sealed interface. This gives compile-time exhaustiveness checking —
adding a new product type forces all factories to implement it, and adding a new variant requires a new factory class.

**Q4: How does Abstract Factory support Dependency Inversion?**

A4: Both the client and the concrete products depend on abstractions (the factory interface and product interfaces). The
client never imports a concrete product class. The concrete factory is chosen at a single point (startup config or
injection), and every dependency flows from that one decision — all through abstractions.

**Q5: Can Abstract Factory return different numbers of products per family?**

A5: No — the factory interface defines the exact product set. If `UIFactory` declares `createButton()`,
`createCheckbox()`, and `createScrollBar()`, every concrete factory must implement all three. If a family lacks a
product (e.g., a terminal UI has no scrollbar), throw `UnsupportedOperationException` or return a no-op implementation.

**Q6: What happens when you need to add a new product type to an existing family?**

A6: This violates the Open/Closed Principle for the Abstract Factory pattern itself. Adding `createSlider()` to
`UIFactory` forces all existing ConcreteFactories to implement it. Solutions include: using `default` methods in Java 8+
that throw by default, or using an AbstractFactory extension interface that factories optionally implement.

**Q7: Name a real-world Java API that uses Abstract Factory.**

A7: `javax.xml.parsers.DocumentBuilderFactory` — you get a factory via `newInstance()` which returns a platform-specific
factory (Apache Xerces, etc.). That factory creates `DocumentBuilder` objects that in turn create `Document`, `Element`,
and `Attr` — all from the same parser family.

**Q8: Compare Abstract Factory with the Strategy pattern.**

A8: Both encapsulate families of algorithms/objects. The key difference: Abstract Factory creates objects (the client
calls `factory.createX()`), while Strategy encapsulates behavior (the client calls `strategy.execute()`). They
complement each other — a Strategy could be chosen by an Abstract Factory.

## 8. Pros & Cons

### Advantages

- **Family Consistency**: Products from the same factory are guaranteed to be compatible
- **Open/Closed Principle**: New variants added without modifying client code
- **Single Responsibility**: Creation logic centralized in factory classes
- **Dependency Inversion**: Client depends only on abstractions
- **Swappable at Runtime**: The entire product family can be replaced by swapping one factory reference
- **Testability**: Mock factories produce test doubles for all products at once

### Disadvantages

- **Complexity**: Many new interfaces and classes for what could be simple construction
- **Rigid Product Set**: Adding a new product type requires changing the AbstractFactory interface and all
  implementations
- **Class Explosion**: Each product type in the family multiplies with each variant
- **Parallel Hierarchies**: Product and factory hierarchies grow together, compounding maintenance
- **Initialization Overhead**: Choosing and instantiating the right factory adds a bootstrap step

## 9. Related Patterns

| Pattern            | Relationship                                                                                                                                           |
|--------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Factory Method** | Abstract Factories are often implemented with Factory Methods — each creation method in the factory interface is a Factory Method.                     |
| **Singleton**      | A ConcreteFactory is often implemented as a Singleton since it has no state and needs only one instance.                                               |
| **Prototype**      | Alternative approach: instead of factory methods, clone a prototype instance for each variant. Useful when product families are configured at runtime. |

### How to Choose

- Use **Abstract Factory** when objects come in families that must be used together consistently
- Use **Factory Method** when you need to create a single product and want subclasses to decide the type
- Use **Builder** when constructing a complex object step by step, with variant representations
- Use **Prototype** when the set of product variants is dynamic (loaded at runtime from config or user input)

## 10. Key Takeaways

- **Families of objects**: Abstract Factory is about creating related object groups that must be compatible
- **Open/Closed Principle**: New families are added by creating new factories — existing code stays untouched
- **Dependency Inversion**: The client depends on factory and product interfaces, never on concrete types
- **Composition over inheritance**: Unlike Factory Method (inheritance-based), Abstract Factory uses composition
- **Interview memory aid**: "Abstract Factory = factory of factories. Returns a family of objects. Think UI toolkits per
  platform."
