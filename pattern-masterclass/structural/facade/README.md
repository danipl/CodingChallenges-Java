# Facade Pattern

## Overview

The **Facade Pattern** provides a unified, simplified interface to a complex subsystem of classes, libraries, or
frameworks. It decouples client code from the subsystem's internal complexity. **One-line interview answer**: The Facade
pattern wraps a complex subsystem behind a single, easy-to-use interface, reducing dependencies and simplifying client
usage.

---

## Problem Statement

### Real-World Scenario

You're building a home theater system. Turning on "Movie Mode" requires: dim the lights (calls to lighting subsystem),
lower the screen (screen subsystem), turn on the projector (projector subsystem), set the input source (AV receiver
subsystem), set the sound mode to surround (audio subsystem), and start the Blu-ray player (media player subsystem).
Without a facade, the client must know about 6+ separate classes, their initialization order, and error-handling for
each.

### Why This Matters in Production

Enterprise applications regularly orchestrate multiple services, APIs, and subsystems. For example:

- **Order fulfillment** — involves inventory, payment, shipping, notification services
- **Onboarding flow** — creates user accounts, provisions databases, sets up CI/CD, sends welcome emails

Without a facade, every client that needs to perform these workflows duplicates the orchestration logic. A subsystem
change (e.g., adding a pre-heat step to the projector) requires hunting down every client that sequences projector
operations.

### Pain Points Without Facade

- **Tight coupling** — clients depend on every class in the subsystem
- **High cognitive load** — clients must understand subsystem internals to use it
- **Duplicate orchestration logic** — every client re-implements the same multi-step workflow
- **Fragile sequencing** — if subsystem requires steps A→B→C, one misplaced call breaks the system
- **Hard to test** — clients must mock all subsystem components

---

## Solution

The Facade pattern introduces a single entry point that:

1. Knows which subsystem classes are needed for each operation
2. Handles initialization, sequencing, and error recovery
3. Exposes only the meaningful high-level operations

### Key Participants

| Role                  | Description                                                                                                               |
|-----------------------|---------------------------------------------------------------------------------------------------------------------------|
| **Facade**            | Provides simplified methods that delegate to the appropriate subsystem classes                                            |
| **Subsystem Classes** | Complex classes that implement the actual functionality. The facade calls them, but clients don't need to know about them |
| **Client**            | Uses the Facade instead of calling subsystem classes directly                                                             |

### Flow

```
Client
  │
  ▼
Facade.watchMovie("Inception")
  │
  ├──→ Lights.dim(20%)
  ├──→ Screen.lower()
  ├──→ Projector.on()
  ├──→ Projector.setInput(BluRay)
  ├──→ Amplifier.on()
  ├──→ Amplifier.setSurroundSound()
  ├──→ Amplifier.setVolume(45)
  └──→ BluRayPlayer.on("Inception")
```

The client makes one call (`facade.watchMovie()`). Behind the scenes, 6+ subsystem calls are sequenced and coordinated.

---

## Java Implementation

### Subsystem Classes

```java
package structural.facade;

// Complex subsystem — many classes, many methods, interdependent
class Lights {
    public void dim(int percent) {
        System.out.println("Lights dimmed to " + percent + "%");
    }
    public void on() { System.out.println("Lights on"); }
}

class Screen {
    public void lower() { System.out.println("Screen lowered"); }
    public void raise() { System.out.println("Screen raised"); }
}

class Projector {
    public void on() { System.out.println("Projector on"); }
    public void off() { System.out.println("Projector off"); }
    public void setInput(String source) {
        System.out.println("Projector input set to " + source);
    }
    public void wideScreenMode() {
        System.out.println("Projector in widescreen mode (16:9)");
    }
}

class Amplifier {
    public void on() { System.out.println("Amplifier on"); }
    public void off() { System.out.println("Amplifier off"); }
    public void setSurroundSound() {
        System.out.println("Surround sound enabled");
    }
    public void setVolume(int level) {
        System.out.println("Volume set to " + level);
    }
}

class BluRayPlayer {
    public void on() { System.out.println("Blu-ray player on"); }
    public void off() { System.out.println("Blu-ray player off"); }
    public void play(String movie) {
        System.out.println("Playing '" + movie + "'");
    }
    public void stop() { System.out.println("Blu-ray stopped"); }
}

class PopcornPopper {
    public void on() { System.out.println("Popcorn popper on"); }
    public void off() { System.out.println("Popcorn popper off"); }
    public void pop() { System.out.println("Popping popcorn!"); }
}
```

### Facade

```java
package structural.facade;

// The Facade provides a unified interface to the entire home theater subsystem
// Clients use ONLY this class for common operations
public class HomeTheaterFacade {
    private final Lights lights;
    private final Screen screen;
    private final Projector projector;
    private final Amplifier amp;
    private final BluRayPlayer bluRay;
    private final PopcornPopper popper;

    public HomeTheaterFacade(
            Lights lights,
            Screen screen,
            Projector projector,
            Amplifier amp,
            BluRayPlayer bluRay,
            PopcornPopper popper) {
        this.lights = lights;
        this.screen = screen;
        this.projector = projector;
        this.amp = amp;
        this.bluRay = bluRay;
        this.popper = popper;
    }

    public void watchMovie(String movie) {
        System.out.println("--- Starting Movie Mode ---");
        popper.on();
        popper.pop();
        lights.dim(10);
        screen.lower();
        projector.on();
        projector.wideScreenMode();
        amp.on();
        amp.setSurroundSound();
        amp.setVolume(45);
        bluRay.on();
        bluRay.play(movie);
        System.out.println("--- Enjoy the movie! ---");
    }

    public void endMovie() {
        System.out.println("--- Shutting down theater ---");
        popper.off();
        lights.on();
        screen.raise();
        projector.off();
        amp.off();
        bluRay.stop();
        bluRay.off();
        System.out.println("--- Theater off ---");
    }
}
```

### Usage Example

```java
package structural.facade;

public class FacadeDemo {
    public static void main(String[] args) {
        // Without facade — client must know every subsystem class and its API
        // Client naively calls each component directly (BAD)
        Lights lights = new Lights();
        Screen screen = new Screen();
        Projector projector = new Projector();
        Amplifier amp = new Amplifier();
        BluRayPlayer bluRay = new BluRayPlayer();
        PopcornPopper popper = new PopcornPopper();

        // Client must orchestrate 10+ calls — fragile, duplicated across every caller
        popper.on();
        popper.pop();
        lights.dim(10);
        screen.lower();
        projector.on();
        projector.wideScreenMode();
        amp.on();
        amp.setSurroundSound();
        amp.setVolume(45);
        bluRay.on();
        bluRay.play("Inception");

        // With facade — single method call, encapsulates all complexity
        HomeTheaterFacade homeTheater = new HomeTheaterFacade(
            lights, screen, projector, amp, bluRay, popper
        );
        homeTheater.watchMovie("Inception");
        homeTheater.endMovie();
    }
}
```

### Facade with Builder (for optional subsystems)

```java
package structural.facade;

// Fluent builder for Facade with optional subsystem components
// Useful when some subsystems may be absent (e.g., no popcorn popper)
public class HomeTheaterBuilder {
    private Lights lights = new Lights();
    private Screen screen = new Screen();
    private Projector projector = new Projector();
    private Amplifier amp = new Amplifier();
    private BluRayPlayer bluRay = new BluRayPlayer();
    private PopcornPopper popper = null;  // optional

    public HomeTheaterBuilder withLights(Lights lights) {
        this.lights = lights; return this;
    }
    public HomeTheaterBuilder withScreen(Screen screen) {
        this.screen = screen; return this;
    }
    public HomeTheaterBuilder withPopper(PopcornPopper popper) {
        this.popper = popper; return this;
    }

    public HomeTheaterFacade build() {
        return new HomeTheaterFacade(lights, screen, projector, amp, bluRay, popper);
    }
}
```

### Multiple Facades (for different client profiles)

```java
package structural.facade;

// Different facades exposing different subsets of subsystem operations
public class HomeTheaterLightFacade {
    private final HomeTheaterFacade fullFacade;

    public HomeTheaterLightFacade(HomeTheaterFacade fullFacade) {
        this.fullFacade = fullFacade;
    }

    // Simplest possible interface for guests
    public void play(String movie) {
        fullFacade.watchMovie(movie);
    }

    public void stop() {
        fullFacade.endMovie();
    }
}
```

### Generic Facade (Java 17+ sealed interface approach)

```java
package structural.facade;

sealed interface TheaterCommand permits PlayMovie, EndMovie, VolumeUp {}

record PlayMovie(String title) implements TheaterCommand {}
record EndMovie() implements TheaterCommand {}
record VolumeUp(int delta) implements TheaterCommand {}

public class CommandFacade {
    private final HomeTheaterFacade theater;

    public CommandFacade(HomeTheaterFacade theater) {
        this.theater = theater;
    }

    public void execute(TheaterCommand cmd) {
        switch (cmd) {
            case PlayMovie m -> theater.watchMovie(m.title());
            case EndMovie _  -> theater.endMovie();
            case VolumeUp v  -> System.out.println("Volume +" + v.delta());
        }
    }
}
```

---

## When to Use

1. **Complex subsystem with many interdependent classes** — e.g., a video transcoding pipeline (demuxer, decoder, filter
   graph, encoder, muxer). A `VideoConverter` facade exposes `convert(input, output, format)`.
2. **Layered architecture** — each layer provides a facade for the layer below (presentation → application → domain →
   infrastructure)
3. **Legacy system wrapping** — hiding a messy legacy API behind a clean modern facade, enabling incremental refactoring
4. **Third-party library abstraction** — protecting your code from vendor lock-in by putting a facade over external SDKs
5. **Simplifying APIs for specific use cases** — exposing coarse-grained operations ("placeOrder", "cancelOrder")
   instead of fine-grained ones

### Framework / Library Examples

| Technology                           | Facade Usage                                                                                            |
|--------------------------------------|---------------------------------------------------------------------------------------------------------|
| **Spring**                           | `JdbcTemplate` is a facade over JDBC — handles connection, statement, result set, exception translation |
| **SLF4J**                            | Facade over Logback, Log4j, java.util.logging — `LoggerFactory.getLogger()` hides the binding           |
| **javax.faces.context.FacesContext** | Facade to JSF internal lifecycle components                                                             |
| **Java Naming (JNDI)**               | `InitialContext` facade over various naming and directory services (LDAP, DNS, RMI)                     |
| **Spring RestTemplate**              | Facade over raw HTTP connection handling, serialization, error handling                                 |

---

## When NOT to Use

1. **Simple subsystems** — adding a facade to a subsystem with 2-3 classes is over-engineering; clients can call them
   directly
2. **Clients need fine-grained control** — a facade that hides too much prevents power users from optimizing or
   customizing. Solution: provide both the facade and direct access
3. **The "God Facade" anti-pattern** — one facade that does everything becomes a monolith itself. Split into cohesive
   facades per concern
4. **Performance overhead** — every facade call adds method dispatch; in performance-critical paths, bypass the facade
5. **When the subsystem is stable and well-known** — if clients know and prefer the subsystem API, a facade adds
   unnecessary indirection

---

## Interview Questions

### Q1: How is Facade different from Adapter?

**Facade** simplifies a complex subsystem into a high-level interface. **Adapter** converts one interface to another.
Adapter solves incompatibility; Facade solves complexity. Adapter usually wraps one class; Facade wraps many classes. A
Visual analogy: Adapter is a power plug converter; Facade is a universal remote for your entire home theater.

### Q2: How is Facade different from Mediator?

Both coordinate multiple components. **Facade** provides a simplified interface *in one direction* — client → facade →
subsystem. The subsystem classes don't know about the facade. **Mediator** manages *bidirectional* communication between
components — colleague objects know about the mediator and communicate through it. Facade is unidirectional
simplification; Mediator is bidirectional coordination.

### Q3: Does the Facade pattern violate the Open/Closed Principle?

No. The subsystem remains open for extension (you can add new subsystem classes). The facade itself should be closed for
modification — if you need new operations, either extend the facade or create a new facade. In practice, facades do
evolve, but their interface should change much less frequently than the subsystem internals.

### Q4: Can you have multiple facades for the same subsystem?

Yes, and this is often desirable. Different client types need different levels of abstraction. For example: a
`SimpleTheaterFacade` (on/off only) for guests, a `FullTheaterFacade` for the owner, and a `MaintenanceFacade` for
service technicians. Each wraps the same subsystem but exposes different operations.

### Q5: Give a real-world Java example of the Facade pattern.

`javax.faces.context.FacesContext` is a facade for all JSF internal components (UIComponent tree, request/response
handling, navigation, external context). Application code calls `FacesContext.getCurrentInstance()`, which abstracts
away the servlet container, the JSF lifecycle, and the render kit. Another example:
`org.springframework.jdbc.core.JdbcTemplate` facades raw JDBC.

### Q6: What is the relationship between Facade and Dependency Injection?

They complement each other. Facade defines the high-level interface; DI wires the subsystem components into the facade.
In the home theater example, a DI container (Spring, Guice) would inject the `Lights`, `Projector`, `Amplifier` etc.
into `HomeTheaterFacade`. The client then injects the facade and doesn't know about the subsystem at all.

### Q7: How do you test code that uses a Facade?

Test the facade with mocked subsystem components, verifying that the sequence of calls is correct. Integration tests can
test the facade with real (or lightweight) subsystems. For clients, mock the facade itself — since the client only
depends on the facade interface, mocking is trivial.

### Q8: What is the "Facade to the Facade" smell?

When you build facades on top of facades (facade → facade → subsystem), you get unnecessary indirection. Each layer adds
complexity without clarity. If you need this, it usually means the underlying facades are poorly designed. Solve by
redesigning the facades, not by piling on more abstraction.

---

## Pros & Cons

### Advantages

- **Decouples clients from subsystem** — changes to subsystem internals don't affect clients
- **Reduces cognitive load** — client calls 1 method instead of 15
- **Improves code organization** — orchestrations in one place, not scattered across callers
- **Enforces best practices** — facades can add error handling, logging, and transaction management centrally
- **Facilitates refactoring** — internal subsystem can be replaced entirely if facade contract is preserved
- **Promotes layered architecture** — clear boundary between client and subsystem concerns

### Disadvantages

- **Can become a God Object** — facade that takes on too much responsibility violates SRP
- **May limit flexibility** — power users who need fine-grained control are restricted
- **Hides subsystem capabilities** — clients may not discover useful subsystem features the facade doesn't expose
- **Adds another layer** — extra indirection, extra class to maintain
- **False sense of simplicity** — the subsystem is still complex; the facade hides it but doesn't simplify it

---

## Related Patterns

| Pattern              | Relationship                                                                            | When to Choose                                                                   |
|----------------------|-----------------------------------------------------------------------------------------|----------------------------------------------------------------------------------|
| **Adapter**          | Both abstract away existing code. Adapter converts interface; Facade simplifies it      | Interface mismatch → Adapter; Subsystem complexity → Facade                      |
| **Mediator**         | Both coordinate. Facade is one-directional simplification; Mediator is bidirectional    | Simplifying a *used-by* system → Facade; Coordinating a *peer* system → Mediator |
| **Singleton**        | Facades are often implemented as Singletons (e.g., `FacesContext.getCurrentInstance()`) | One facade instance per subsystem → Singleton                                    |
| **Abstract Factory** | Can be used to create subsystem components that the facade uses                         | Creating families of subsystem objects → Abstract Factory                        |

### Key Distinction Memory Aid

> **Facade** gives you one big "Easy" button.  
> **Adapter** rewires a plug to fit a different socket.  
> **Mediator** is the air traffic controller between planes.  
> **Proxy** is the bouncer at the door.

---

## Key Takeaways

- **Least Knowledge Principle (Law of Demeter)** — Facade embodies this: your code should talk only to your immediate
  friends, not to the subsystem's internal components
- **Encapsulation of complexity** — the subsystem's internals, interdependencies, and initialization order are hidden,
  making the system more maintainable
- **SOLID alignment** — Single Responsibility (facade handles orchestration; subsystem handles operations), Dependency
  Inversion (clients depend on facade abstraction)
- **Ubiquitous in frameworks** — `JdbcTemplate`, `RestTemplate`, `SLF4J`, `FacesContext` — every major Java framework
  uses Facade
- **Interview tip** — emphasize the distinction from Adapter and Mediator. Give a concrete example (home theater, video
  converter) and draw the class diagram on the whiteboard. Mention that a facade doesn't hide the subsystem entirely —
  clients can still access it directly if needed
