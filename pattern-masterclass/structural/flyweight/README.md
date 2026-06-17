# Flyweight Pattern (Cache Pattern / Lightweight Pattern)

## Overview

The **Flyweight Pattern** minimizes memory usage by sharing as much data as possible with similar objects. It achieves
this by separating intrinsic state (shared, context-independent) from extrinsic state (unique, context-dependent). *
*One-line interview answer**: The Flyweight pattern uses sharing to support large numbers of fine-grained objects
efficiently by storing intrinsic state once and computing extrinsic state on the fly.

---

## Problem Statement

### Real-World Scenario

You're building a text editor that must display a document with thousands of characters. The naive approach creates a
`Character` object for every character in the document, each storing its glyph (shape data), font, size, color, position
on screen, and spacing. A 100,000-character document would create 100,000 objects, consuming megabytes of memory just
for metadata. Rendering performance degrades, and the app becomes sluggish — especially on mobile devices with limited
memory.

### Why This Matters in Production

Object overhead is real:

- **Java object header** — 12-16 bytes per object (on 64-bit JVM with compressed OOPs)
- **Reference fields** — 4-8 bytes each
- **Arrays** — 24-byte header + element overhead
- A million fine-grained objects can consume hundreds of megabytes in overhead alone

Flyweight is critical in:

- **Text editors** — each character's glyph data is the same for the same font/size
- **Game development** — particles, trees, bullets, terrain tiles — thousands of visually identical or near-identical
  objects
- **UI toolkits** — thousands of list items, table cells, tree nodes with shared rendering data
- **Caching** — database connection pools, thread pools, object pools

### Pain Points Without Flyweight

- **Memory bloat** — every object stores the same duplicated data (e.g., the glyph "A" in Times New Roman 12pt is stored
  1000 times instead of once)
- **GC pressure** — thousands of short-lived objects trigger frequent garbage collection, causing latency spikes
- **Cache misses** — more objects = worse CPU cache utilization = slower performance
- **Slow instantiation** — creating 100,000 individual objects is expensive in both time and memory
- **Scaling limits** — the app cannot scale to handle larger documents, more players, or richer worlds

---

## Solution

The Flyweight pattern separates object state into two categories:

| State Type    | Definition                                                                                 | Stored Where                                                                |
|---------------|--------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------|
| **Intrinsic** | Context-independent, shareable data (e.g., glyph shape, font metrics, color palette entry) | In the Flyweight object, shared across all contexts                         |
| **Extrinsic** | Context-dependent, unique data (e.g., position, scale, rotation, document offset)          | Computed or stored by the client, passed to the Flyweight on each operation |

### Key Participants

| Role                  | Description                                                                               |
|-----------------------|-------------------------------------------------------------------------------------------|
| **Flyweight**         | The shared object containing intrinsic state. Usually implements a common interface       |
| **ConcreteFlyweight** | Stores intrinsic state that is shared across contexts                                     |
| **FlyweightFactory**  | Creates and manages Flyweight objects. Ensures sharing by returning existing instances    |
| **Client**            | Stores or computes extrinsic state and passes it to the Flyweight when calling operations |

### Flow

```
Client (TextEditor)
  |
  v
FlyweightFactory.getGlyph('A', Font.TIMES_ROMAN, 12, Bold)
  |
  +-- Cache hit? --yes--> return existing Glyph
  |     |
  |    no
  |     v
  +-- Create new Glyph (load glyph data from file)
  +-- Store in cache
  +-- Return Glyph
       |
       v
Client calls: glyph.render(positionX, positionY, color)
              |                          |
              +------ intrinsic ---------+
              (shared Glyph object)    (extrinsic state passed by client)
```

---

## Java Implementation

### Flyweight Interface

```java
package structural.flyweight;

// Flyweight interface with extrinsic state passed as parameters
public interface Glyph {
    void render(int positionX, int positionY, String color);
    GlyphMetrics getMetrics();
}

record GlyphMetrics(int width, int height, int ascent, int descent) {}
```

### Concrete Flyweight

```java
package structural.flyweight;

import java.awt.image.BufferedImage;
import java.util.Objects;

// ConcreteFlyweight: stores the intrinsic, shareable state
// This object is immutable and can be shared across thousands of document positions
public class CharacterGlyph implements Glyph {
    private final char character;
    private final String fontFamily;
    private final int fontSize;
    private final boolean bold;
    private final boolean italic;

    // Intrinsic state: glyph rendering data (loaded once, shared everywhere)
    private final BufferedImage glyphImage;
    private final GlyphMetrics metrics;

    public CharacterGlyph(char character, String fontFamily, int fontSize,
                          boolean bold, boolean italic, GlyphMetrics metrics) {
        this.character = character;
        this.fontFamily = fontFamily;
        this.fontSize = fontSize;
        this.bold = bold;
        this.italic = italic;
        this.metrics = metrics;
        this.glyphImage = new BufferedImage(metrics.width(), metrics.height(),
            BufferedImage.TYPE_INT_ARGB);
        System.out.println("Created glyph for '" + character + "' (" + fontFamily
            + " " + fontSize + (bold ? " bold" : "") + ")");
    }

    @Override
    public void render(int positionX, int positionY, String color) {
        // Extrinsic state (position, color) is passed by the client
        // Intrinsic state (glyph image) is stored in this object
        System.out.println("Rendering '" + character + "' at (" + positionX
            + "," + positionY + ") in " + color);
    }

    @Override
    public GlyphMetrics getMetrics() { return metrics; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CharacterGlyph that = (CharacterGlyph) o;
        return character == that.character && fontSize == that.fontSize
            && bold == that.bold && italic == that.italic
            && Objects.equals(fontFamily, that.fontFamily);
    }

    @Override
    public int hashCode() {
        return Objects.hash(character, fontFamily, fontSize, bold, italic);
    }
}
```

### Flyweight Factory

```java
package structural.flyweight;

import java.util.HashMap;
import java.util.Map;

// FlyweightFactory: creates and manages shared Glyph objects
// Ensures that identical glyphs reuse the same object
public class GlyphFactory {
    private final Map<String, Glyph> glyphCache = new HashMap<>();

    public Glyph getGlyph(char character, String fontFamily, int fontSize,
                          boolean bold, boolean italic) {
        String key = character + ":" + fontFamily + ":" + fontSize
            + ":" + bold + ":" + italic;

        return glyphCache.computeIfAbsent(key, k -> {
            GlyphMetrics metrics = new GlyphMetrics(
                fontSize, fontSize, (int)(fontSize * 0.8), (int)(fontSize * 0.2));
            return new CharacterGlyph(character, fontFamily, fontSize,
                bold, italic, metrics);
        });
    }

    public int getCacheSize() { return glyphCache.size(); }
    public void clearCache() { glyphCache.clear(); }
}
```

### Client — Document with Extrinsic State

```java
package structural.flyweight;

import java.util.ArrayList;
import java.util.List;

// Client: stores the extrinsic state for each character
// Memory savings: we store a reference to a shared Glyph (4-8 bytes)
// instead of a full glyph object (hundreds of bytes)
public class Document {
    private final GlyphFactory factory;

    private record CharacterPosition(
        Glyph glyph,      // shared flyweight (4-8 bytes reference)
        int x, int y,     // extrinsic position
        String color      // extrinsic color
    ) {}

    private final List<CharacterPosition> characters = new ArrayList<>();

    public Document(GlyphFactory factory) { this.factory = factory; }

    public void type(char character, String fontFamily, int fontSize,
                     boolean bold, boolean italic, int x, int y, String color) {
        Glyph glyph = factory.getGlyph(character, fontFamily, fontSize, bold, italic);
        characters.add(new CharacterPosition(glyph, x, y, color));
    }

    public void render() {
        for (CharacterPosition cp : characters) {
            cp.glyph().render(cp.x(), cp.y(), cp.color());
        }
    }

    public int getCharacterCount() { return characters.size(); }
}
```

### Usage Example

```java
package structural.flyweight;

public class FlyweightDemo {
    public static void main(String[] args) {
        GlyphFactory factory = new GlyphFactory();
        Document doc = new Document(factory);

        // "Hello World" typed in the same font — each character glyph is shared
        String text = "Hello World";
        int x = 0;
        for (char c : text.toCharArray()) {
            doc.type(c, "Times New Roman", 12, false, false, x, 0, "black");
            x += 8;
        }

        // Second paragraph — same chars, same font -> same glyph objects
        x = 0;
        for (char c : text.toCharArray()) {
            doc.type(c, "Times New Roman", 12, false, false, x, 20, "black");
            x += 8;
        }

        // Bold text — different intrinsic state -> different glyph objects
        x = 0;
        for (char c : text.toCharArray()) {
            doc.type(c, "Times New Roman", 12, true, false, x, 40, "red");
            x += 8;
        }

        System.out.println("=== Document Stats ===");
        System.out.println("Total characters typed: " + doc.getCharacterCount());
        System.out.println("Unique glyphs created: " + factory.getCacheSize());

        // Memory savings: 66 naive objects vs 13 shared glyphs + 66 references
        // Savings: ~76% memory reduction
        doc.render();
    }
}
```

### Game Example: Particle System

```java
package structural.flyweight;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Flyweight for a game's particle effects
// Intrinsic: texture, particle type, physics properties (gravity, drag)
// Extrinsic: position, velocity, lifetime, color tint

interface Particle {
    void render(int x, int y, int lifetime, Color tint, double scale);
}

class ParticleTemplate implements Particle {
    private final String textureName;
    private final int particleType;
    private final double defaultGravity;
    private final double dragCoefficient;

    ParticleTemplate(String textureName, int type, double gravity, double drag) {
        this.textureName = textureName;
        this.particleType = type;
        this.defaultGravity = gravity;
        this.dragCoefficient = drag;
    }

    @Override
    public void render(int x, int y, int lifetime, Color tint, double scale) {
        System.out.println("Render particle type " + particleType
            + " at (" + x + "," + y + ") life=" + lifetime);
    }
}

class ParticleFactory {
    private final Map<String, ParticleTemplate> cache = new ConcurrentHashMap<>();

    ParticleTemplate getParticleTemplate(String textureName, int type) {
        String key = textureName + ":" + type;
        return cache.computeIfAbsent(key, k -> {
            System.out.println("Loading texture: " + k);
            return new ParticleTemplate(textureName, type, 9.8, 0.5);
        });
    }

    int getCacheSize() { return cache.size(); }
}

class ParticleSystem {
    private final ParticleFactory factory;
    private final List<ParticleInstance> instances = new ArrayList<>();

    private record ParticleInstance(
        ParticleTemplate template, int x, int y, int lifetime, Color tint, double scale
    ) {}

    ParticleSystem(ParticleFactory factory) { this.factory = factory; }

    void spawn(String texture, int type, int x, int y) {
        ParticleTemplate template = factory.getParticleTemplate(texture, type);
        instances.add(new ParticleInstance(template, x, y, 100, Color.WHITE, 1.0));
    }

    void update() {
        for (var inst : instances) {
            inst.template().render(inst.x(), inst.y(), inst.lifetime(),
                inst.tint(), inst.scale());
        }
    }
}
```

### Integer Cache — Java's Built-in Flyweight

```java
package structural.flyweight;

// Java's Integer.valueOf() is a built-in Flyweight pattern
public class JavaIntegerFlyweight {
    public static void main(String[] args) {
        // Integer cache: -128 to 127 are cached (shared objects)
        Integer a = Integer.valueOf(100);
        Integer b = Integer.valueOf(100);
        System.out.println(a == b);     // true (same object from cache)

        Integer c = Integer.valueOf(200);
        Integer d = Integer.valueOf(200);
        System.out.println(c == d);     // false (outside cache range)

        // Autoboxing uses valueOf() internally
        Integer e = 127;   // cached
        Integer f = 127;   // cached
        System.out.println(e == f);     // true

        Integer g = 128;   // NOT cached (outside [-128,127])
        Integer h = 128;   // NOT cached
        System.out.println(g == h);     // false

        // This is why you should use .equals() for Integer comparison, not ==
    }
}

// Other Java flyweight examples:
//   String.intern() — shares String objects in the intern pool
//   Boolean.valueOf(boolean) — returns static TRUE/FALSE singletons
//   Byte.valueOf(byte) — caches all 256 byte values
```

### String Pool (Flyweight in the JVM)

```java
package structural.flyweight;

public class StringPoolDemo {
    public static void main(String[] args) {
        // String literals are interned automatically (flyweight)
        String s1 = "hello";
        String s2 = "hello";
        System.out.println(s1 == s2);  // true (same object from string pool)

        // new String() creates a new object (no sharing)
        String s3 = new String("hello");
        System.out.println(s1 == s3);  // false (different objects)

        // .intern() forces pool lookup
        String s4 = s3.intern();
        System.out.println(s1 == s4);  // true (returns pooled object)

        // Intrinsic state: the char[] value of the string
        // Extrinsic state: none for literals; references are shared
    }
}
```

---

## When to Use

1. **Large numbers of fine-grained objects** — text editors (thousands of character glyphs), games (thousands of
   particles, bullets), UI toolkits (thousands of table cells)
2. **Memory is constrained** — mobile devices, embedded systems, browser-based applications where every kilobyte matters
3. **State can be split into intrinsic and extrinsic** — if the object's state can be cleanly divided into shareable and
   context-dependent parts, Flyweight is applicable
4. **Object identity is not important** — if objects are indistinguishable by intrinsic state (e.g., all "A" glyphs in
   Times 12pt are identical), sharing is safe
5. **The cost of computing extrinsic state is low** — passing extrinsic state to the flyweight should be cheaper than
   storing it in each object

### Framework / Library Examples

| Technology            | Flyweight Usage                                                                  |
|-----------------------|----------------------------------------------------------------------------------|
| **Integer.valueOf()** | Caches Integer objects in range [-128, 127]                                      |
| **String.intern()**   | Shares String objects in the JVM string pool                                     |
| **Font rendering**    | Font glyphs are cached and shared across all text rendering                      |
| **Java AWT**          | `Font` objects are shared; creating `new Font(...)` may return a cached instance |
| **Thread pools**      | Reuses Thread objects instead of creating new ones per task                      |
| **Connection pools**  | Shares database Connection objects across requests                               |

---

## When NOT to Use

1. **Objects are naturally unique** — if every object has completely different state (e.g., distinct customer records),
   sharing provides no benefit
2. **Extrinsic state computation is expensive** — if passing extrinsic state to each flyweight call costs more CPU than
   storing it, Flyweight makes performance worse
3. **Memory is not a bottleneck** — if the application has ample memory and few objects, Flyweight adds complexity
   without value
4. **Thread safety is complex** — shared mutable state requires synchronization; immutable flyweights avoid this but
   limit what can be shared
5. **Premature optimization** — as with all patterns, apply Flyweight when you have measured a memory problem, not
   because you anticipate one

---

## Interview Questions

### Q1: What is intrinsic vs extrinsic state in the Flyweight pattern?

**Intrinsic state** is context-independent and shareable — it's stored in the Flyweight object and reused across all
contexts (e.g., a character's glyph bitmap, font metrics). **Extrinsic state** is context-dependent and cannot be
shared — it's computed or stored by the client and passed to the Flyweight on each method call (e.g., character
position, color, rotation).

### Q2: Give a real-world Java example of the Flyweight pattern.

`Integer.valueOf(int)` caches Integer objects in the range -128 to 127. Calling `valueOf(100)` twice returns the same
object reference. This is a pure Flyweight: the intrinsic state is the `int` value, and there is no extrinsic state. The
JVM string pool (`String.intern()`) is another example — identical string literals share the same underlying `char[]`
array.

### Q3: How is Flyweight different from Object Pool?

**Flyweight** shares objects that are *identical in intrinsic state* — multiple clients use the same object
simultaneously (e.g., the same glyph used at multiple positions). **Object Pool** reuses objects that are *expensive to
create* but returned to the pool after use — one client at a time per object (e.g., database connections). Flyweight is
about memory; Object Pool is about creation cost.

### Q4: What are the thread-safety concerns with Flyweight?

If the Flyweight is immutable (all fields are `final`), it's inherently thread-safe. If mutable, concurrent clients can
corrupt shared state. Solution: make flyweights immutable, or synchronize access. Since intrinsic state is shared, any
mutation is visible to all clients — this is almost always unacceptable in production.

### Q5: How does the Flyweight pattern relate to the Composite pattern?

They can be combined. In a large Composite tree (e.g., a document with thousands of character leaf nodes), each leaf
could be a Flyweight sharing glyph data. The Composite provides the tree structure; the Flyweight provides
memory-efficient leaf storage. This combination is used in real text editors.

### Q6: When would you choose not to use Flyweight despite having many objects?

When the extrinsic state is larger than the intrinsic state, or when computing/passing extrinsic state at each call is
more expensive than storing it inline. Also avoid Flyweight when object identity matters (you need distinct objects per
position) or when the objects are already small enough that sharing overhead dominates.

### Q7: How does the String pool implement Flyweight?

When the JVM loads a class, it interns string literals into the string pool (a hash map). When the same literal appears
elsewhere, the JVM returns the pooled reference instead of creating a new String. This is why `"hello" == "hello"` is
`true` — both references point to the same pooled object. `new String("hello")` bypasses the pool.

### Q8: Can you give an example of Flyweight in a game development context?

A particle system. A 2D game may have 10,000 particles (smoke, fire, sparks) on screen. Without Flyweight, each particle
stores its texture data (a 64x64 RGBA image = 16KB per particle × 10,000 = 160MB). With Flyweight, the texture is shared
intrinsic state (stored once per particle type), and each particle instance stores only its extrinsic state: position (8
bytes), velocity (8 bytes), lifetime (4 bytes), tint color (4 bytes) — about 24 bytes per particle × 10,000 = 240KB.

---

## Pros & Cons

### Advantages

- **Reduces memory footprint** dramatically — often 70-90% reduction for fine-grained objects
- **Improves cache locality** — fewer objects = better CPU cache utilization
- **Reduces GC pressure** — fewer objects allocated, fewer collections
- **Enables scaling** — applications can handle larger documents, richer worlds, more users
- **Centralizes object creation** — factory ensures sharing, reduces allocation overhead

### Disadvantages

- **Increased complexity** — separating intrinsic from extrinsic state is not always intuitive
- **Extrinsic state overhead** — computing and passing extrinsic state may cost more CPU than it saves in memory
- **Thread safety concerns** — shared mutable state requires careful synchronization
- **Not suitable for all objects** — some objects have no shareable state
- **Hidden identity issues** — since flyweights are shared, `==` comparison may give unexpected results
- **Harder to debug** — it's not obvious which flyweight instance is shared across which contexts

---

## Related Patterns

| Pattern         | Relationship                                                                                                   | When to Choose                                                                       |
|-----------------|----------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------|
| **Composite**   | Often combined — Composite tree with Flyweight leaf nodes for memory efficiency                                | Need both tree structure and memory efficiency -> Composite + Flyweight              |
| **Singleton**   | FlyweightFactory is often a Singleton                                                                          | Need a single point of control for sharing -> Singleton Factory                      |
| **Object Pool** | Both manage object reuse. Flyweight shares identical objects; Pool reuses identical-type objects one at a time | Memory savings for simultaneous use -> Flyweight; Creation cost amortization -> Pool |
| **Proxy**       | Virtual Proxy creates objects lazily; Flyweight shares them eagerly                                            | Lazy creation -> Proxy; Memory-efficient sharing -> Flyweight                        |

### Key Distinction Memory Aid

> **Flyweight** shares a single "A" glyph across 10,000 positions in a document.  
> **Proxy** creates a "B" glyph only when someone scrolls to page 3.  
> **Singleton** ensures only one "FontLoader" exists per application.  
> **Object Pool** reuses "C" glyph from a pool after one client finishes with it.

---

## Key Takeaways

- **Intrinsic vs Extrinsic** — mastering this distinction is the key to implementing Flyweight correctly. Intrinsic =
  shareable, immutable, context-free. Extrinsic = unique, context-dependent, passed as parameters
- **Immutability is essential** — flyweights must be immutable for thread safety. If intrinsic state can change, sharing
  becomes dangerous
- **Java's built-in flyweights** — `Integer.valueOf()`, `String.intern()`, `Boolean.valueOf()`, and the string pool are
  everyday Java features that implement this pattern
- **SOLID alignment** — Single Responsibility (flyweight handles one piece of intrinsic state), Dependency Inversion (
  clients depend on Flyweight interface, not concrete flyweights)
- **Interview tip** — always lead with the intrinsic/extrinsic distinction. Draw a text editor example with glyphs.
  Mention `Integer.valueOf(-128 to 127)` and the string pool as built-in Java examples. Contrast with Object Pool (
  sharing vs. recycling) and Proxy (lazy vs. eager)
