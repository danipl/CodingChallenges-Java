# Bridge Pattern (Handle/Body Pattern)

## Overview

The **Bridge Pattern** decouples an abstraction from its implementation so that the two can vary independently. It
replaces inheritance with composition to avoid a permanent binding between an abstraction and its implementation at
compile time. **One-line interview answer**: The Bridge pattern separates an abstraction from its implementation into
two separate class hierarchies, allowing both to evolve independently without affecting each other.

---

## Problem Statement

### Real-World Scenario

You're building a drawing application that needs to render shapes (circles, squares, triangles) on different operating
systems (Windows, Linux, macOS) using platform-specific drawing APIs (DirectX, Cairo, Quartz). The naive approach
creates a Cartesian product of classes: `CircleWindowsRenderer`, `CircleLinuxRenderer`, `CircleMacRenderer`,
`SquareWindowsRenderer`, etc. With 5 shapes and 3 platforms, that's 15 classes. Add a new shape (hexagon) or a new
platform (Android), and you add M or N classes respectively — class explosion.

### Why This Matters in Production

This "Cartesian product" problem (also called the "multi-dimensional inheritance" problem) appears everywhere:

- **Persistence** — domain entities (User, Order, Product) × storage mechanisms (SQL, NoSQL, File)
- **Authentication** — resources × authentication providers (OAuth, SAML, LDAP)
- **Message processing** — message types × serialization formats (JSON, XML, Protobuf)
- **Notifications** — notification types × delivery channels (Email, SMS, Push)

Without Bridge, every combination is a class; with Bridge, the two dimensions grow linearly (M + N) instead of
multiplicatively (M × N).

### Pain Points Without Bridge

- **Class explosion** — M × N classes for M abstractions and N implementations
- **Hard to extend** — adding a new shape requires N new classes (one per platform); adding a new platform requires M
  new classes (one per shape)
- **Compile-time binding** — cannot choose the implementation at runtime
- **Code duplication** — platform-specific code mixed with abstraction logic
- **Violates OCP** — adding any variant requires modifying existing hierarchies

---

## Solution

The Bridge pattern splits a monolithic class into two orthogonal hierarchies connected by composition:

1. **Abstraction hierarchy** — defines the high-level interface and maintains a reference to an implementor
2. **Implementor hierarchy** — defines the low-level implementation interface

Instead of `Circle` extending `WindowsRenderer`, the `Circle` (abstraction) *has a* reference to a `Renderer` (
implementor) and delegates drawing to it.

### Key Participants

| Role                    | Description                                                                                          |
|-------------------------|------------------------------------------------------------------------------------------------------|
| **Abstraction**         | High-level control layer. Defines the abstract interface and maintains a reference to an Implementor |
| **RefinedAbstraction**  | Extends the abstraction with additional features                                                     |
| **Implementor**         | Defines the interface for implementation classes. Usually narrower than Abstraction                  |
| **ConcreteImplementor** | Provides platform-specific implementation of the Implementor interface                               |

### Flow

```
Client
  │
  ▼
Circle (RefinedAbstraction)
  │  has-a → LinuxRenderer (ConcreteImplementor)
  │
  ├── Circle.draw()
  │     └── renderer.drawCircle(x, y, radius)
  │           → Linux-specific: Cairo calls
  │
  └── Circle.resize(factor)
        └── radius *= factor
```

```
Abstraction hierarchy:    Shape → Circle, Square, Triangle
Implementor hierarchy:    Renderer → WindowsRenderer, LinuxRenderer, MacRenderer

Without Bridge:   3 shapes × 3 platforms = 9 classes (M × N)
With Bridge:      3 shapes + 3 platforms = 6 classes (M + N)
```

---

## Java Implementation

### Implementor Interface

```java
package structural.bridge;

// Implementor: platform-specific drawing operations
public interface Renderer {
    void drawCircle(double x, double y, double radius);
    void drawSquare(double x, double y, double side);
    void drawTriangle(double x1, double y1, double x2, double y2, double x3, double y3);
    void drawText(String text, double x, double y);
}
```

### Concrete Implementors

```java
package structural.bridge;

// Windows-specific renderer using DirectX (simulated)
public class WindowsRenderer implements Renderer {
    @Override
    public void drawCircle(double x, double y, double radius) {
        System.out.println("[Windows DirectX] Drawing circle at (" + x + "," + y + ") r=" + radius);
    }

    @Override
    public void drawSquare(double x, double y, double side) {
        System.out.println("[Windows DirectX] Drawing square at (" + x + "," + y + ") side=" + side);
    }

    @Override
    public void drawTriangle(double x1, double y1, double x2, double y2, double x3, double y3) {
        System.out.println("[Windows DirectX] Drawing triangle at (" + x1 + "," + y1 + "),(" + x2 + "," + y2 + "),(" + x3 + "," + y3 + ")");
    }

    @Override
    public void drawText(String text, double x, double y) {
        System.out.println("[Windows DirectX] Text '" + text + "' at (" + x + "," + y + ")");
    }
}

// Linux-specific renderer using Cairo (simulated)
public class LinuxRenderer implements Renderer {
    @Override
    public void drawCircle(double x, double y, double radius) {
        System.out.println("[Linux Cairo] Drawing circle at (" + x + "," + y + ") r=" + radius);
    }

    @Override
    public void drawSquare(double x, double y, double side) {
        System.out.println("[Linux Cairo] Drawing square at (" + x + "," + y + ") side=" + side);
    }

    @Override
    public void drawTriangle(double x1, double y1, double x2, double y2, double x3, double y3) {
        System.out.println("[Linux Cairo] Drawing triangle");
    }

    @Override
    public void drawText(String text, double x, double y) {
        System.out.println("[Linux Cairo] Text '" + text + "' at (" + x + "," + y + ")");
    }
}

// macOS-specific renderer using Quartz (simulated)
public class MacRenderer implements Renderer {
    @Override
    public void drawCircle(double x, double y, double radius) {
        System.out.println("[macOS Quartz] Drawing circle at (" + x + "," + y + ") r=" + radius);
    }

    @Override
    public void drawSquare(double x, double y, double side) {
        System.out.println("[macOS Quartz] Drawing square at (" + x + "," + y + ") side=" + side);
    }

    @Override
    public void drawTriangle(double x1, double y1, double x2, double y2, double x3, double y3) {
        System.out.println("[macOS Quartz] Drawing triangle");
    }

    @Override
    public void drawText(String text, double x, double y) {
        System.out.println("[macOS Quartz] Text '" + text + "' at (" + x + "," + y + ")");
    }
}
```

### Abstraction

```java
package structural.bridge;

// Abstraction: defines the high-level shape interface
// Maintains a reference to the Renderer (implementor)
public abstract class Shape {
    protected final Renderer renderer;

    protected Shape(Renderer renderer) {
        this.renderer = renderer;
    }

    // High-level operations that delegate to the renderer
    public abstract void draw();
    public abstract void resize(double factor);

    // Logging/monitoring common to all shapes
    public void logDraw() {
        System.out.println("Drawing " + getClass().getSimpleName());
    }
}
```

### Refined Abstractions

```java
package structural.bridge;

public class Circle extends Shape {
    private double x, y, radius;

    public Circle(Renderer renderer, double x, double y, double radius) {
        super(renderer);
        this.x = x;
        this.y = y;
        this.radius = radius;
    }

    @Override
    public void draw() {
        logDraw();
        renderer.drawCircle(x, y, radius);  // delegates to platform-specific renderer
    }

    @Override
    public void resize(double factor) {
        this.radius *= factor;
    }

    // Circle-specific operations
    public double getArea() {
        return Math.PI * radius * radius;
    }
}

public class Square extends Shape {
    private double x, y, side;

    public Square(Renderer renderer, double x, double y, double side) {
        super(renderer);
        this.x = x;
        this.y = y;
        this.side = side;
    }

    @Override
    public void draw() {
        logDraw();
        renderer.drawSquare(x, y, side);
    }

    @Override
    public void resize(double factor) {
        this.side *= factor;
    }
}

public class Triangle extends Shape {
    private double x1, y1, x2, y2, x3, y3;

    public Triangle(Renderer renderer,
                    double x1, double y1,
                    double x2, double y2,
                    double x3, double y3) {
        super(renderer);
        this.x1 = x1; this.y1 = y1;
        this.x2 = x2; this.y2 = y2;
        this.x3 = x3; this.y3 = y3;
    }

    @Override
    public void draw() {
        logDraw();
        renderer.drawTriangle(x1, y1, x2, y2, x3, y3);
    }

    @Override
    public void resize(double factor) {
        // Scale all vertices from origin
        x1 *= factor; y1 *= factor;
        x2 *= factor; y2 *= factor;
        x3 *= factor; y3 *= factor;
    }
}
```

### Usage Example

```java
package structural.bridge;

import java.util.List;

public class BridgeDemo {
    public static void main(String[] args) {
        // Select renderer at runtime — could come from config, system property, etc.
        String os = System.getProperty("os.name").toLowerCase();
        Renderer renderer;

        if (os.contains("win")) {
            renderer = new WindowsRenderer();
        } else if (os.contains("nix") || os.contains("nux")) {
            renderer = new LinuxRenderer();
        } else if (os.contains("mac")) {
            renderer = new MacRenderer();
        } else {
            throw new IllegalStateException("Unsupported OS: " + os);
        }

        // Create shapes using the selected renderer
        // The shapes are decoupled from the rendering implementation
        List<Shape> shapes = List.of(
            new Circle(renderer, 10, 20, 5),
            new Square(renderer, 0, 0, 10),
            new Triangle(renderer, 0, 0, 5, 10, 10, 0)
        );

        // Draw all shapes — each delegates to the platform renderer
        System.out.println("=== Drawing with " + renderer.getClass().getSimpleName() + " ===");
        for (Shape shape : shapes) {
            shape.draw();
        }

        // Resize and redraw
        System.out.println("\n=== After resize (2x) ===");
        for (Shape shape : shapes) {
            shape.resize(2.0);
            shape.draw();
        }
    }
}
```

### JDBC Analogy (The Classic Bridge Example)

```java
package structural.bridge;

// JDBC is the canonical real-world Bridge pattern:
//
//   Abstraction hierarchy:    Connection, Statement, ResultSet
//   Implementor hierarchy:    MySQLDriver, PostgreSQLDriver, OracleDriver
//
// DriverManager.getConnection() returns a Connection (abstraction)
// that internally delegates to a Driver (implementor).
// Both hierarchies vary independently:
//   - New database: add a Driver (implementor), no change to Connection
//   - New data type: add to Connection (abstraction), drivers implement it

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class JdbcBridgeExample {
    public static void main(String[] args) throws Exception {
        // The abstraction (Connection) is decoupled from the implementation (MySQL driver)
        // We can swap to PostgreSQL by changing the URL — no code changes
        String url = "jdbc:mysql://localhost:3306/mydb";
        String user = "root";
        String password = "secret";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM users")) {

            while (rs.next()) {
                System.out.println(rs.getString("name"));
            }
        }

        // To switch to PostgreSQL:
        //   String url = "jdbc:postgresql://localhost:5432/mydb";
        //   // DriverManager's ServiceLoader finds the PostgreSQL Driver on classpath
        //   // Connection, Statement, ResultSet interfaces don't change
    }
}

// Key insight:
//   - Connection = abstraction (interface we code to)
//   - MySQL Driver = concrete implementor (hidden behind Driver interface)
//   - DriverManager = the bridge that connects them
```

### A Second Example: Device/Remote Control

```java
package structural.bridge;

// Implementor
interface Device {
    boolean isEnabled();
    void enable();
    void disable();
    int getVolume();
    void setVolume(int percent);
    int getChannel();
    void setChannel(int channel);
}

// Concrete Implementors
class TV implements Device {
    private boolean on = false;
    private int volume = 30;
    private int channel = 1;

    @Override public boolean isEnabled() { return on; }
    @Override public void enable() { on = true; System.out.println("TV on"); }
    @Override public void disable() { on = false; System.out.println("TV off"); }
    @Override public int getVolume() { return volume; }
    @Override public void setVolume(int percent) { this.volume = percent; System.out.println("TV volume: " + volume); }
    @Override public int getChannel() { return channel; }
    @Override public void setChannel(int ch) { this.channel = ch; System.out.println("TV channel: " + channel); }
}

class Radio implements Device {
    private boolean on = false;
    private int volume = 20;
    private int channel = 88; // FM frequency

    @Override public boolean isEnabled() { return on; }
    @Override public void enable() { on = true; System.out.println("Radio on"); }
    @Override public void disable() { on = false; System.out.println("Radio off"); }
    @Override public int getVolume() { return volume; }
    @Override public void setVolume(int percent) { this.volume = percent; }
    @Override public int getChannel() { return channel; }
    @Override public void setChannel(int ch) { this.channel = ch; }
}

// Abstraction hierarchy
abstract class RemoteControl {
    protected final Device device;

    protected RemoteControl(Device device) {
        this.device = device;
    }

    public abstract void togglePower();
    public abstract void volumeUp();
    public abstract void volumeDown();
    public abstract void channelUp();
    public abstract void channelDown();
}

class BasicRemote extends RemoteControl {
    public BasicRemote(Device device) { super(device); }

    @Override public void togglePower() {
        if (device.isEnabled()) device.disable();
        else device.enable();
    }
    @Override public void volumeUp() { device.setVolume(device.getVolume() + 10); }
    @Override public void volumeDown() { device.setVolume(device.getVolume() - 10); }
    @Override public void channelUp() { device.setChannel(device.getChannel() + 1); }
    @Override public void channelDown() { device.setChannel(device.getChannel() - 1); }
}

class AdvancedRemote extends BasicRemote {
    public AdvancedRemote(Device device) { super(device); }
    public void mute() { device.setVolume(0); }
}

// Usage:
//   RemoteControl remote = new AdvancedRemote(new TV());
//   remote.togglePower();  // TV on
//   remote.channelUp();    // TV channel 2
//
//   RemoteControl radioRemote = new BasicRemote(new Radio());
//   radioRemote.togglePower();  // Radio on
//   radioRemote.volumeUp();     // Radio volume 30
```

---

## When to Use

1. **Multi-dimensional class hierarchies** — when you have N abstractions × M implementations leading to class
   explosion. Bridge collapses M×N into M+N
2. **Runtime selection of implementation** — when the implementation needs to be chosen at runtime (e.g., platform
   detection, configuration-driven)
3. **Both abstraction and implementation should be extensible** — when adding new abstractions should not affect
   implementations and vice versa
4. **Sharing an implementation across multiple objects** — the same `Renderer` can be injected into hundreds of shapes
5. **Hiding implementation details from clients** — clients only see the abstraction interface (e.g., JDBC `Connection`
   hides database-specific details)

### Framework / Library Examples

| Technology      | Bridge Usage                                                                                               |
|-----------------|------------------------------------------------------------------------------------------------------------|
| **JDBC**        | `Connection`, `Statement`, `ResultSet` (abstractions) decoupled from database drivers (implementors)       |
| **SLF4J**       | Logger (abstraction) delegates to Logback/Log4j (implementor)                                              |
| **Spring DI**   | Interface (abstraction) ← implementation (configurable via DI)                                             |
| **AWT / Swing** | `java.awt.Toolkit` (abstraction) delegates to platform peers (implementor)                                 |
| **JAXP**        | `DocumentBuilderFactory` creates document builders decoupled from parser implementations (Xerces, Crimson) |

---

## When NOT to Use

1. **Simple, single-platform applications** — if you only target one platform (e.g., Linux-only embedded system), Bridge
   adds unnecessary indirection
2. **Stable hierarchy with no foreseeable extensions** — if both the abstraction and implementation dimensions are
   fixed, use simpler inheritance
3. **Performance-critical code** — every abstraction call goes through an extra delegation to the implementor. In
   graphics rendering hot paths, this overhead matters
4. **When there's only one dimension of change** — if only the abstraction changes (or only the implementation), use
   simple inheritance or composition, not a full Bridge
5. **Your language doesn't support interfaces well** — Bridge relies heavily on polymorphism; in languages without
   interfaces or duck typing, the pattern is harder to implement cleanly

---

## Interview Questions

### Q1: What problem does the Bridge pattern solve that inheritance alone cannot?

Inheritance binds the abstraction and implementation permanently at compile time. If you have M abstractions and N
implementations, inheritance creates M×N classes (each combination). Bridge uses composition to let both hierarchies
vary independently, growing M+N instead of M×N. This solves the "Cartesian product" or "multi-dimensional inheritance"
explosion problem.

### Q2: How is Bridge different from Adapter?

Both use composition to connect two interfaces. **Adapter** is used after the fact to make *existing* incompatible
classes work together (retrofit). **Bridge** is designed upfront to keep abstraction and implementation decoupled.
Adapter is for making things work; Bridge is for preventing coupling from the start. Adapter is a "how did we get here?"
solution; Bridge is a "where are we going?" design.

### Q3: How does the Bridge pattern relate to the Dependency Inversion Principle?

Bridge is a textbook example of DIP: the high-level module (Shape abstraction) depends on an abstraction (Renderer
interface), not on concrete implementations (WindowsRenderer, LinuxRenderer). Both the abstraction and implementation
hierarchies depend on the same interface, inverting the traditional top-down dependency.

### Q4: Give a real-world Java example of the Bridge pattern.

JDBC is the best example. `java.sql.Connection` gets a `Connection` object from `DriverManager`, but the actual
implementation is provided by the MySQL/PostgreSQL/Oracle driver loaded at runtime. The Connection interface is the
abstraction; the Driver implementation is the concrete implementor. Adding a new database means writing a new Driver —
no changes to Connection, Statement, or ResultSet.

### Q5: What is the "switch" anti-pattern that Bridge avoids?

Without Bridge, you'd have a switch/if-else chain based on platform in every method:

```java
void drawCircle() {
    if (os.equals("Windows")) { /* DirectX code */ }
    else if (os.equals("Linux")) { /* Cairo code */ }
    else if (os.equals("Mac")) { /* Quartz code */ }
}
```

This violates OCP (adding a platform requires modifying every method) and SRP (shape classes also handle
platform-specific rendering). Bridge extracts the platform code into the implementor hierarchy, eliminating switches.

### Q6: Can Bridge be combined with Abstract Factory?

Yes, frequently. An Abstract Factory can create the appropriate ConcreteImplementor based on context (platform,
configuration, environment). For example:

```java
interface RendererFactory {
    Renderer createRenderer();
}

class PlatformRendererFactory implements RendererFactory {
    public Renderer createRenderer() {
        return switch (System.getProperty("os.name")) {
            case "Windows" -> new WindowsRenderer();
            case "Linux"   -> new LinuxRenderer();
            case "Mac OS X" -> new MacRenderer();
            default -> throw new IllegalArgumentException();
        };
    }
}
```

### Q7: What is the drawback of Bridge when the abstraction hierarchy is very deep?

Deep abstraction hierarchies can lead to many layers of delegation, adding method dispatch overhead and stack depth.
Each `draw()` call passes through each abstraction layer before reaching the implementor. This is usually negligible,
but in hot paths (game loops rendering 10,000 objects per frame), the indirection can cause measurable performance
impact.

### Q8: How does Bridge differ from the State pattern?

Bridge decouples abstraction from implementation (structural decoupling). State decouples behavior from state (
behavioral delegation). In Bridge, the implementor changes rarely; in State, the state object changes frequently. The
structure is similar (both use composition to a delegate), but the intent is different: Bridge is for independent
variation of hierarchies; State is for dynamic behavior changes.

---

## Pros & Cons

### Advantages

- **Eliminates class explosion** — M + N classes instead of M × N
- **Independent extensibility** — add shapes without touching renderers; add renderers without touching shapes
- **Runtime flexibility** — implementation can be selected at runtime (platform detection, configuration)
- **Implementation hiding** — clients see only the abstraction; implementation details are encapsulated
- **Open/Closed Principle** — new abstractions and implementations can be added without modifying existing code
- **Single Responsibility** — each hierarchy has one reason to change (abstraction changes for business reasons;
  implementation changes for platform reasons)

### Disadvantages

- **Increased complexity** — more interfaces, more indirection, more classes upfront
- **Performance overhead** — every call goes through abstraction → implementor delegation
- **Over-engineering risk** — if you never need a second implementation, Bridge is premature abstraction
- **Design effort** — requires identifying the orthogonal dimensions early in the design process
- **Harder to debug** — stack traces pass through both abstraction and implementor layers

---

## Related Patterns

| Pattern              | Relationship                                                                                                                           | When to Choose                                                                 |
|----------------------|----------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------|
| **Adapter**          | Both use composition to connect interfaces. Adapter is retrospective (making things work); Bridge is prospective (planning for change) | Making legacy code fit → Adapter; Designing for independent evolution → Bridge |
| **Abstract Factory** | Can create and configure the appropriate Implementor for the Bridge                                                                    | Need platform-specific object creation → Abstract Factory                      |
| **Strategy**         | Similar structure (has-a delegation), but Strategy is for algorithms (behavior), Bridge is for implementation (platform)               | Swappable algorithm → Strategy; Swappable platform → Bridge                    |
| **State**            | Similar structure, different intent. State changes behavior dynamically; Bridge decouples static hierarchies                           | Dynamic behavior changes → State; Independent hierarchy evolution → Bridge     |

### Key Distinction Memory Aid

> **Bridge** separates what you draw (shape) from how you draw it (renderer). Two orthogonal hierarchies.  
> **Adapter** rewires an existing plug to fit a different socket.  
> **Strategy** swaps the algorithm for computing the shape's area.  
> **State** changes how the shape behaves when you click it.

---

## Key Takeaways

- **Solve "Cartesian product" explosion** — M × N classes → M + N classes by separating abstraction from implementation
  into two hierarchies
- **Composition, not inheritance** — the abstraction *has a* reference to the implementor. This is why Bridge avoids the
  permanent binding of inheritance
- **JDBC is the canonical example** — `Connection` (abstraction) + `Driver` (implementor). Every Java developer uses
  Bridge daily
- **SOLID alignment** — Single Responsibility (each hierarchy has one concern), Open/Closed (both hierarchies
  extensible), Dependency Inversion (both depend on abstraction), Liskov Substitution (implementors are substitutable)
- **Interview tip** — draw the two hierarchies on the whiteboard. Label them "abstraction" and "implementor". Show the
  M × N problem with a grid, then show Bridge reducing it to M + N. Mention JDBC as the immediate real-world example.
  Contrast with Adapter (retrospective vs. prospective)
