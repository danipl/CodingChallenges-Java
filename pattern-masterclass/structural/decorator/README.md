# Decorator Pattern (Wrapper Pattern)

## Overview

The **Decorator Pattern** attaches additional responsibilities to an object dynamically without altering its structure
or affecting other objects of the same class. It provides a flexible alternative to subclassing for extending
functionality. **One-line interview answer**: The Decorator pattern wraps an object with one or more wrapper classes
that share the same interface, allowing behavior to be added at runtime in a composable, chainable fashion.

---

## Problem Statement

### Real-World Scenario

You run a coffee shop with a point-of-sale system. You have a base `Beverage` class with subclasses like `Espresso`,
`HouseBlend`, and `DarkRoast`. Customers can add multiple condiments: steamed milk, soy milk, mocha, whipped cream,
caramel — each with different prices. You could create subclasses like `EspressoWithMochaAndWhip`, but with 4 base
beverages and 5 condiments, you'd need over 100+ subclasses. Add new condiments quarterly, and you face a combinatorial
explosion.

### Why This Matters in Production

This "subclass explosion" (also called "class proliferation") is a fundamental design smell. It appears in:

- **Stream processing** — adding compression, encryption, buffering to byte streams
- **UI rendering** — adding borders, scrollbars, shadows to visual components
- **Middleware pipelines** — adding logging, auth, rate-limiting to HTTP handlers

Without Decorator, you either create M×N classes or embed all possible combinations in one class with booleans (
`hasMocha`, `hasWhip`, `hasSoy`), which violates the Open/Closed Principle and produces God classes.

### Pain Points Without Decorator

- **Class explosion** — every combination requires a new subclass (combinatorial N × M)
- **Rigid at compile time** — cannot add or remove features at runtime
- **Duplicate code** — same condiment logic duplicated across all beverage subclasses
- **Breaks OCP** — adding a new condiment requires modifying every beverage subclass

---

## Solution

The Decorator pattern uses **composition** and a **recursive wrapping** structure:

1. Define a common interface for both the core object and all decorators
2. The core class implements this interface
3. Each decorator also implements the interface and **wraps** another instance of it
4. Decorators add behavior **before** and/or **after** delegating to the wrapped object

### Key Participants

| Role                  | Description                                                                           |
|-----------------------|---------------------------------------------------------------------------------------|
| **Component**         | The abstract interface for objects that can have responsibilities added               |
| **ConcreteComponent** | The base object to which additional responsibilities can be attached                  |
| **Decorator**         | Abstract class maintaining a reference to a Component and conforming to its interface |
| **ConcreteDecorator** | Adds specific behavior before/after delegating to the wrapped Component               |

### Flow

```
Client
  │
  ▼
Decorator C  (Whip — adds $0.35, calls next)
  │
  ▼
Decorator B  (Mocha — adds $0.50, calls next)
  │
  ▼
Decorator A  (Milk — adds $0.25, calls next)
  │
  ▼
ConcreteComponent  (Espresso — base cost $1.99)
```

```
order.cost()
  → WhipDecorator.cost()       → 0.35 + MochaDecorator.cost()
    → MochaDecorator.cost()    → 0.50 + MilkDecorator.cost()
      → MilkDecorator.cost()  → 0.25 + Espresso.cost()
        → Espresso.cost()    → 1.99
      ← 2.24
    ← 2.74
  ← 3.09
Total: $3.09
```

---

## Java Implementation

### Component Interface

```java
package structural.decorator;

public interface Beverage {
    String getDescription();
    double cost();
}
```

### ConcreteComponent

```java
package structural.decorator;

public class Espresso implements Beverage {
    @Override
    public String getDescription() {
        return "Espresso";
    }

    @Override
    public double cost() {
        return 1.99;
    }
}

public class HouseBlend implements Beverage {
    @Override
    public String getDescription() {
        return "House Blend Coffee";
    }

    @Override
    public double cost() {
        return 0.89;
    }
}

public class DarkRoast implements Beverage {
    @Override
    public String getDescription() {
        return "Dark Roast";
    }

    @Override
    public double cost() {
        return 1.49;
    }
}

public class Decaf implements Beverage {
    @Override
    public String getDescription() {
        return "Decaf";
    }

    @Override
    public double cost() {
        return 1.29;
    }
}
```

### Abstract Decorator

```java
package structural.decorator;

// Abstract decorator: holds a reference to a wrapped Beverage
// and delegates all calls. Concrete decorators override selectively.
public abstract class CondimentDecorator implements Beverage {
    protected final Beverage beverage;

    public CondimentDecorator(Beverage beverage) {
        this.beverage = beverage;
    }

    // Subclasses override getDescription() to append their name
    @Override
    public String getDescription() {
        return beverage.getDescription();
    }

    // Subclasses override cost() to add their price
    @Override
    public double cost() {
        return beverage.cost();
    }
}
```

### Concrete Decorators

```java
package structural.decorator;

public class Milk extends CondimentDecorator {
    public Milk(Beverage beverage) {
        super(beverage);
    }

    @Override
    public String getDescription() {
        return beverage.getDescription() + ", Milk";
    }

    @Override
    public double cost() {
        return beverage.cost() + 0.25;
    }
}

public class Mocha extends CondimentDecorator {
    public Mocha(Beverage beverage) {
        super(beverage);
    }

    @Override
    public String getDescription() {
        return beverage.getDescription() + ", Mocha";
    }

    @Override
    public double cost() {
        return beverage.cost() + 0.50;
    }
}

public class Whip extends CondimentDecorator {
    public Whip(Beverage beverage) {
        super(beverage);
    }

    @Override
    public String getDescription() {
        return beverage.getDescription() + ", Whip";
    }

    @Override
    public double cost() {
        return beverage.cost() + 0.35;
    }
}

public class Soy extends CondimentDecorator {
    public Soy(Beverage beverage) {
        super(beverage);
    }

    @Override
    public String getDescription() {
        return beverage.getDescription() + ", Soy";
    }

    @Override
    public double cost() {
        return beverage.cost() + 0.40;
    }
}
```

### Usage Example

```java
package structural.decorator;

public class DecoratorDemo {
    public static void main(String[] args) {
        // Order: Double mocha espresso with whip
        Beverage beverage = new Espresso();
        beverage = new Mocha(beverage);   // wrap with mocha
        beverage = new Mocha(beverage);   // double mocha
        beverage = new Whip(beverage);    // wrap with whip

        System.out.println(beverage.getDescription() + " → $" + beverage.cost());
        // Output: Espresso, Mocha, Mocha, Whip → $3.34

        // Order: Soy latte with whip
        Beverage beverage2 = new DarkRoast();
        beverage2 = new Soy(beverage2);
        beverage2 = new Whip(beverage2);

        System.out.println(beverage2.getDescription() + " → $" + beverage2.cost());
        // Output: Dark Roast, Soy, Whip → $2.24
    }
}
```

### Static Factory for Readability (Modern Java)

```java
package structural.decorator;

// Using static imports and a fluent-style helper
// Reduces nesting noise in client code
import java.util.function.UnaryOperator;

public class BeverageFactory {
    public static Beverage order(Beverage base, UnaryOperator<Beverage>... decorators) {
        Beverage result = base;
        for (var decorator : decorators) {
            result = decorator.apply(result);
        }
        return result;
    }

    public static void main(String[] args) {
        Beverage order = order(
            new Espresso(),
            b -> new Mocha(b),
            b -> new Mocha(b),
            b -> new Whip(b)
        );
        System.out.println(order.getDescription() + " → $" + order.cost());
    }
}
```

### Java I/O Stream Decorator (Real-World Analogy)

```java
package structural.decorator;

import java.io.*;

// Java's I/O classes are the canonical real-world Decorator example
public class JavaIODemo {
    public static void main(String[] args) throws Exception {
        // FileInputStream: ConcreteComponent — reads raw bytes
        // BufferedInputStream: ConcreteDecorator — adds buffering
        // GZIPInputStream: ConcreteDecorator — adds decompression
        // InflaterInputStream: ConcreteDecorator — adds inflation

        try (InputStream in = new BufferedInputStream(
                new GZIPInputStream(
                    new FileInputStream("data.gz")))) {
            byte[] buffer = new byte[1024];
            int bytesRead = in.read(buffer);
            System.out.println("Read " + bytesRead + " decompressed bytes");
        }

        // Each layer wraps the previous one, adding behavior:
        //   FileInputStream: reads bytes from file
        //   GZIPInputStream: decompresses bytes
        //   BufferedInputStream: buffers chunks for efficiency
        // The read() method chains through all three layers transparently
    }
}
```

### Inheritance vs Decorator Comparison

```java
package structural.decorator;

// PROBLEM: Class explosion with inheritance
//   Espresso → EspressoWithMilk → EspressoWithMilkAndMocha → ...
//   For 4 bases × 5 condiments, we'd need 4 + 4×5 + 4×5×5 + ... ≈ 1000+ classes

// Traditional approach without Decorator:
class EspressoWithMochaAndWhip extends Espresso {
    @Override
    public double cost() {
        return super.cost() + 0.50 + 0.35;  // hardcoded condiment prices
    }
}
// Problem: adding Caramel requires creating EspressoWithMochaAndWhipAndCaramel
// This approach does NOT scale — it's brittle and violates OCP

// Decorator approach:
//   Beverage b = new Whip(new Mocha(new Espresso()));
// Adding Caramel?  Add a Caramel decorator class. Zero existing changes.
// The O(N+M) complexity of Decorator replaces the O(2^N) complexity of subclassing
```

---

## When to Use

1. **Adding responsibilities dynamically and transparently** — when you need to add features to individual objects at
   runtime without affecting others
2. **Removing responsibilities is also required** — un-decorating is trivial (reassign the reference); subclassing
   requires recompilation
3. **When subclassing is impractical** — too many combinations, or the class is `final` (e.g., `String`, `LocalDate`)
4. **Middleware / filter chains** — every middleware in a web framework (Express.js, Spring Filter) is a Decorator:
   logging, auth, rate-limiting, compression
5. **Legacy enhancement** — adding logging, metrics, or caching to existing code without modification

### Framework / Library Examples

| Technology      | Decorator Usage                                                                                                          |
|-----------------|--------------------------------------------------------------------------------------------------------------------------|
| **Java I/O**    | `BufferedReader(FileReader)`, `GZIPOutputStream(FileOutputStream)`                                                       |
| **Spring**      | `TransactionProxyFactoryBean` decorates POJOs with transaction management                                                |
| **Servlet API** | `javax.servlet.Filter` chains decorators around HTTP request handling                                                    |
| **Collections** | `Collections.synchronizedList()`, `Collections.unmodifiableList()` wrap collections with synchronized/read-only behavior |
| **Stream API**  | `Stream.map().filter().collect()` chains operations — a functional decorator pattern                                     |

---

## When NOT to Use

1. **Simple, stable behavior** — if functionality will never change or be extended, subclassing is simpler and more
   performant
2. **Core object type must be exposed** — decorators wrap and hide the concrete type; `instanceof` checks on the wrapped
   object will fail
3. **Thin, numerous decorators create cognitive overhead** — too many layers make debugging hard ("where did this
   behavior come from?")
4. **When the decorator order matters and is unpredictable** — the result depends on wrap order; `new Milk(new Mocha())`
   costs differently from `new Mocha(new Milk())` if prices aren't independent
5. **Performance-critical hot paths** — each decorator adds virtual method dispatch; a chain of 10 decorators = 10
   indirect calls

---

## Interview Questions

### Q1: How is Decorator different from inheritance?

Inheritance adds behavior at **compile time** to an **entire class** (all instances). Decorator adds behavior at *
*runtime** to a **single object instance**. Inheritance is rigid (white-box reuse — child knows parent internals);
Decorator is flexible (black-box reuse — no knowledge of internals). Inheritance creates class explosion (M×N
combinations); Decorator uses linear growth (M+N combinations).

### Q2: How is Decorator different from Adapter and Proxy?

**Decorator** adds behavior while preserving the original interface. **Adapter** changes the interface. **Proxy**
controls access to the object (same interface). Adapter solves incompatibility; Decorator enhances functionality; Proxy
controls access. In the coffee shop analogy: Adapter makes a coffee machine with a different plug work at your counter;
Decorator adds extra shots and syrup; Proxy ensures only paid orders are fulfilled.

### Q3: Can a Decorator change the return type of a method?

Technically yes, but this breaks the Liskov Substitution Principle (LSP) because the client expects the original
`Component` interface. If you need a different return type, you need an Adapter, not a Decorator. The Decorator must
preserve the contract of the Component interface.

### Q4: What are the trade-offs of using an abstract Decorator class vs interface?

An abstract Decorator class lets you provide default delegation (calling `wrapped.method()`) so concrete decorators only
override what they need. A pure interface forces every decorator to implement all methods explicitly, creating
boilerplate for pass-through delegation. Java's I/O uses abstract classes (`FilterInputStream`) for this reason.

### Q5: Give a real-world example of Decorator beyond Java I/O.

`Collections.synchronizedList(new ArrayList<>())` decorates an `ArrayList` by wrapping every mutation method with
`synchronized`. The returned object implements the same `List` interface but all operations are now thread-safe.
Multiple decorators can be composed: `Collections.unmodifiableList(Collections.synchronizedList(list))`.

### Q6: How do you avoid too many small objects with Decorator?

Use **static factory methods** with meaningful names (e.g., `Beverage.withMilk().withWhip()`) or a **builder pattern**
that internally applies decorators. This hides the nesting from callers while preserving runtime flexibility.
Alternatively, use the **functional decorator** approach with `UnaryOperator<Beverage>` lambdas.

### Q7: What is the "Transparent Decorator" concept?

A Transparent Decorator is one where the object's identity is unchanged from the client's perspective — the decorator
implements every method of the component interface, delegating most and only modifying specific ones. This transparency
enables recursive composition. Java's `FilterInputStream` is transparent: it implements all `InputStream` methods by
delegating.

### Q8: How does Decorator relate to the Single Responsibility Principle?

Decorator aligns perfectly with SRP by letting you split a large monolithic class (e.g., `Beverage` with 15 boolean
condiment fields) into one small class per responsibility (`Milk`, `Mocha`, `Whip`). Each decorator does exactly one
thing and composes with others through wrapping rather than monolithic design.

---

## Pros & Cons

### Advantages

- **Open/Closed Principle** — extend behavior without modifying existing code
- **Single Responsibility** — each decorator handles one concern
- **Composable** — mix and match decorations in any combination
- **Runtime flexibility** — add/remove behavior at runtime
- **Avoids class explosion** — linear growth instead of combinatorial

### Disadvantages

- **Many small objects** — a decorated object is wrapped in N anonymous-looking layers
- **Order dependence** — result may depend on decoration order
- **Type erasure** — `instanceof` and type-specific operations fail on fully wrapped objects
- **Debugging complexity** — stack traces pass through N layers of delegation
- **Instantiation verbosity** — deeply nested constructor calls (`new Whip(new Mocha(new Milk(new Espresso())))`) are
  hard to read
- **Identity confusion** — `equals()` and `hashCode()` need careful handling across wrapping layers

---

## Related Patterns

| Pattern       | Relationship                                                                                    | When to Choose                                                               |
|---------------|-------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------|
| **Adapter**   | Both wrap, but Adapter changes interface; Decorator preserves it                                | Incompatible interfaces → Adapter; Adding behavior → Decorator               |
| **Proxy**     | Same interface, but Proxy controls access while Decorator adds behavior                         | Lazy loading / access control → Proxy; Functionality enhancement → Decorator |
| **Composite** | Both use recursive composition. Composite aggregates children; Decorator wraps a single sibling | Tree structures → Composite; Chain of enhancements → Decorator               |
| **Strategy**  | Strategy swaps algorithms; Decorator layers responsibilities                                    | Pluggable algorithm → Strategy; Multi-layer enhancement → Decorator          |

### Key Distinction Memory Aid

> **Decorator** adds toppings without changing the dish (same interface).  
> **Adapter** translates the menu for foreign customers (different interface).  
> **Proxy** checks your ID before serving (access control, same interface).  
> **Composite** serves a platter with multiple dishes (tree structure).

---

## Key Takeaways

- **Composition over inheritance** — Decorator is the canonical example: recursive wrapping avoids the combinatorial
  explosion of subclassing
- **Transparent to the client** — the client interacts with the `Component` interface, unaware of how many decorators
  are wrapped around the core object
- **Ubiquitous in core Java** — `BufferedReader(FileReader)`, `Collections.synchronizedList()`, `javax.servlet.Filter` —
  all Decorator
- **SOLID alignment** — OCP (open for extension), SRP (each decorator has one job), LSP (decorators are substitutable
  for their component)
- **Interview tip** — always mention Java I/O as the canonical example, and draw the distinction from Adapter and Proxy.
  Show the recursive cost() call chain to demonstrate understanding
