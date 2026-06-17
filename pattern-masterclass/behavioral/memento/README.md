# Memento Pattern

## Overview

**Definition**: Memento captures and externalizes an object's internal state so that the object can be restored to this
state later, without violating encapsulation.

**Core Problem**: How to implement undo/checkpoint functionality while keeping an object's internal state private and
preventing external objects from accessing that state.

**One-Line Interview Answer**: "Memento lets you save and restore an object's state without breaking encapsulation, by
storing the state in a separate memento object that only the originator can access."

## Problem Statement

### Real-World Scenario: Text Editor with Version History

A document editor needs to save checkpoints so users can undo changes or revert to previous versions. A naive approach
exposes internal state:

```java
public class Document {
    private StringBuilder content;
    private String fontName;
    private int fontSize;
    private boolean bold;
    private boolean italic;

    // Expose all fields for saving — breaks encapsulation
    public String getContent() { return content.toString(); }
    public String getFontName() { return fontName; }
    // ... many getters exposing internal state

    // External code must know the full internal structure
    // to save and restore state:
    public void restore(String content, String fontName, int fontSize, boolean bold, boolean italic) {
        this.content = new StringBuilder(content);
        this.fontName = fontName;
        this.fontSize = fontSize;
        this.bold = bold;
        this.italic = italic;
    }
}
```

### Pain Points of the Naive Approach

1. **Encapsulation Violation**: To save state, you must expose every private field via getters/setters. This breaks the
   class's encapsulation — any object can read and modify internal state.
2. **Brittle Restoration**: The restore method signature couples the caller to every field. Adding a field (e.g.,
   `underline`) breaks all save/restore code.
3. **Responsibility Leak**: The logic for what constitutes "state" and how to save/restore it leaks into the calling
   code (the caretaker).
4. **No Snapshot Integrity**: The external code might save state in one moment but restore it later when the internal
   structure has changed, creating inconsistent snapshots.
5. **Duplication**: Every class needing undo must repeat the same getter/setter/restore boilerplate.

### Why This Matters in Production

Undo/redo, crash recovery, and transactional rollback are fundamental in editors, databases, and workflows. The Memento
pattern's key insight: only the Originator knows what state matters and how to capture/restore it correctly.
Externalizing this logic violates encapsulation and creates maintenance nightmares.

## Solution

### How Memento Solves This

Memento uses three actors: the **Originator** creates and consumes mementos; the **Memento** is an opaque state
container; the **Caretaker** holds mementos but never inspects them. The Originator's private fields stay private — the
Memento serializes internal state using package-private or nested class access.

### Key Participants

| Participant  | Role                                                                           |
|--------------|--------------------------------------------------------------------------------|
| `Originator` | Creates a Memento capturing its internal state; uses Memento to restore state  |
| `Memento`    | Stores internal state of the Originator; immutable and opaque to other objects |
| `Caretaker`  | Requests and stores Mementos; never modifies or inspects them                  |

### Step-by-Step Flow

1. Before changing state, Originator calls `createMemento()` which snapshots internal fields into a new Memento
2. Originator performs the state-changing operation
3. Caretaker pushes the Memento onto a history stack
4. On undo: Caretaker pops the Memento and passes it to `originator.restore(memento)`
5. Originator extracts state from the Memento (via package-private/nested access) and restores fields
6. Neither the Caretaker nor any external code ever sees the Memento's contents

### UML-Style Structure

```
┌──────────────┐     ┌──────────────────┐     ┌──────────────┐
│  Caretaker   │─────│    Originator    │─────│   Memento    │
│              │     │                  │     │  (opaque)    │
│ -history[]   │     │ -state           │     │              │
│              │     │                  │     │ -state       │
│ +undo()      │     │ +createMemento() │     │ +getState()  │
│              │     │ +restore(Memento)│     │  (pkg-private)│
└──────────────┘     └──────────────────┘     └──────────────┘
```

## Java Implementation

### Document Editor with Memento

```java
package behavioral.memento;

import java.util.ArrayDeque;
import java.util.Deque;

// Originator
class Document {
    private StringBuilder content;
    private String fontName;
    private int fontSize;
    private String fontColor;

    public Document() {
        this.content = new StringBuilder();
        this.fontName = "Arial";
        this.fontSize = 12;
        this.fontColor = "black";
    }

    // Business operations
    public void write(String text) {
        content.append(text);
    }

    public void deleteLast(int chars) {
        int len = content.length();
        if (chars > len) chars = len;
        content.delete(len - chars, len);
    }

    public void setFont(String fontName, int fontSize, String fontColor) {
        this.fontName = fontName;
        this.fontSize = fontSize;
        this.fontColor = fontColor;
    }

    public String getContent() { return content.toString(); }

    // Memento creation — captures current state
    public Memento createMemento() {
        return new Memento(content.toString(), fontName, fontSize, fontColor);
    }

    // State restoration — extracts state from memento
    public void restore(Memento memento) {
        this.content = new StringBuilder(memento.content);
        this.fontName = memento.fontName;
        this.fontSize = memento.fontSize;
        this.fontColor = memento.fontColor;
    }

    public void print() {
        System.out.printf("[%s, %dpt, %s] \"%s\"%n",
            fontName, fontSize, fontColor, content);
    }

    // Memento — immutable, opaque to outside world
    // Static nested class has access to Document's private fields
    // Package-private — only classes in this package can access getters
    public static class Memento {
        private final String content;
        private final String fontName;
        private final int fontSize;
        private final String fontColor;
        private final long timestamp;

        private Memento(String content, String fontName, int fontSize, String fontColor) {
            this.content = content;
            this.fontName = fontName;
            this.fontSize = fontSize;
            this.fontColor = fontColor;
            this.timestamp = System.currentTimeMillis();
        }

        // For diagnostics only — not part of state restoration
        public long getTimestamp() { return timestamp; }
    }
}
```

### Caretaker with Undo/Redo

```java
class DocumentHistory {
    private final Deque<Document.Memento> undoStack = new ArrayDeque<>();
    private final Deque<Document.Memento> redoStack = new ArrayDeque<>();
    private static final int MAX_HISTORY = 50;

    public void save(Document document) {
        if (undoStack.size() >= MAX_HISTORY) {
            undoStack.pollLast();
        }
        undoStack.push(document.createMemento());
        redoStack.clear(); // New snapshot invalidates redo
    }

    public boolean canUndo() { return !undoStack.isEmpty(); }

    public void undo(Document document) {
        if (!canUndo()) {
            System.out.println("Nothing to undo");
            return;
        }
        // Save current state to redo stack before restoring
        redoStack.push(document.createMemento());
        Document.Memento memento = undoStack.pop();
        document.restore(memento);
        System.out.println("Undo: restored to " + formatTimestamp(memento));
    }

    public boolean canRedo() { return !redoStack.isEmpty(); }

    public void redo(Document document) {
        if (!canRedo()) {
            System.out.println("Nothing to redo");
            return;
        }
        // Save current state to undo stack before restoring
        undoStack.push(document.createMemento());
        Document.Memento memento = redoStack.pop();
        document.restore(memento);
        System.out.println("Redo: restored to " + formatTimestamp(memento));
    }

    private String formatTimestamp(Document.Memento memento) {
        return String.format("@%d", memento.getTimestamp() % 10000);
    }
}
```

### Usage Demo

```java
public class MementoDemo {
    public static void main(String[] args) throws Exception {
        var doc = new Document();
        var history = new DocumentHistory();

        // Write initial content
        doc.write("Hello");
        history.save(doc);
        doc.print();

        // Append text
        doc.write(" World");
        history.save(doc);
        doc.print();

        // Change formatting
        doc.setFont("Courier New", 14, "blue");
        history.save(doc);
        doc.print();

        // Delete some characters
        doc.deleteLast(6);
        history.save(doc);
        doc.print();

        // Undo three times
        System.out.println("\n=== Undo twice ===");
        history.undo(doc);
        doc.print();

        history.undo(doc);
        doc.print();

        // Redo
        System.out.println("\n=== Redo ===");
        history.redo(doc);
        doc.print();

        // Make a change after undo — invalidates redo
        System.out.println("\n=== New change after undo (invalidates redo) ===");
        doc.write("!!!");
        history.save(doc);
        doc.print();

        // Redo should be empty now
        System.out.println("Can redo? " + history.canRedo());
    }
}
```

### Game Checkpoint System (Another Example)

```java
import java.util.HashMap;
import java.util.Map;

// Originator
class GameCharacter {
    private int health;
    private int mana;
    private int level;
    private int xp;
    private String location;
    private Map<String, Integer> inventory;

    public GameCharacter() {
        this.health = 100;
        this.mana = 50;
        this.level = 1;
        this.xp = 0;
        this.location = "Town Square";
        this.inventory = new HashMap<>(Map.of("Gold", 10, "Potion", 2));
    }

    public void takeDamage(int amount) {
        health = Math.max(0, health - amount);
        System.out.println("Took " + amount + " damage. Health: " + health);
    }

    public void heal(int amount) {
        health = Math.min(100, health + amount);
    }

    public void gainXp(int amount) {
        xp += amount;
        if (xp >= level * 100) {
            level++;
            xp = 0;
            System.out.println("Level up! Now level " + level);
        }
    }

    public void moveTo(String location) {
        this.location = location;
        System.out.println("Moved to " + location);
    }

    public void addItem(String item, int count) {
        inventory.merge(item, count, Integer::sum);
    }

    // Memento as a deeply immutable snapshot
    public Checkpoint save() {
        return new Checkpoint(health, mana, level, xp, location, new HashMap<>(inventory));
    }

    public void load(Checkpoint checkpoint) {
        this.health = checkpoint.health;
        this.mana = checkpoint.mana;
        this.level = checkpoint.level;
        this.xp = checkpoint.xp;
        this.location = checkpoint.location;
        this.inventory = new HashMap<>(checkpoint.inventory);
        System.out.println("Loaded checkpoint at " + location);
    }

    public void displayStatus() {
        System.out.printf("LVL%d | HP:%d/%d | MP:%d | Location: %s | Items: %s%n",
            level, health, 100, mana, location, inventory);
    }

    // Memento — record (Java 17+)
    public record Checkpoint(int health, int mana, int level, int xp,
                              String location, Map<String, Integer> inventory) {
        public Checkpoint {
            inventory = Map.copyOf(inventory); // Defensive copy for immutability
        }
    }
}

class GameCheckpointDemo {
    public static void main(String[] args) {
        var hero = new GameCharacter();
        var checkpoints = new ArrayDeque<GameCharacter.Checkpoint>();

        // Play game
        hero.displayStatus();
        checkpoints.push(hero.save()); // Auto-save

        hero.moveTo("Dark Forest");
        hero.takeDamage(30);
        hero.gainXp(50);
        checkpoints.push(hero.save());

        hero.moveTo("Dragon Cave");
        hero.takeDamage(80);
        hero.addItem("Dragon Scale", 1);
        hero.displayStatus();

        // Hero dies — load last checkpoint
        System.out.println("\n=== Hero died! Loading checkpoint... ===");
        hero.load(checkpoints.pop());
        hero.displayStatus();

        // Try again
        hero.moveTo("Dragon Cave");
        hero.heal(50);
        hero.takeDamage(40); // Better this time
        hero.gainXp(200);
        hero.displayStatus();
    }
}
```

### Memento with Serialization (Deep Snapshot)

```java
import java.io.*;

// Originator using Java serialization for deep memento
class SerializableDocument implements Serializable {
    private StringBuilder content;
    private String fontName;
    private transient int fontSize; // transient — not saved

    public SerializableDocument() {
        this.content = new StringBuilder();
        this.fontName = "Arial";
        this.fontSize = 12;
    }

    public byte[] saveToBytes() {
        try (var baos = new ByteArrayOutputStream();
             var oos = new ObjectOutputStream(baos)) {
            oos.writeObject(this);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void restoreFromBytes(byte[] data) {
        try (var bais = new ByteArrayInputStream(data);
             var ois = new ObjectInputStream(bais)) {
            var restored = (SerializableDocument) ois.readObject();
            this.content = restored.content;
            this.fontName = restored.fontName;
            // fontSize is transient — loses value
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
```

## When to Use

1. **Undo/Redo Systems**: Text editors, image editors, IDEs. Memento captures state snapshots for unlimited undo.
   Combined with Command pattern for operation-level undo.

2. **Transaction Rollback**: Database transactions. Before executing a statement, the database saves a memento (rollback
   segment). On ROLLBACK, it restores the memento.

3. **Checkpoint/Save Systems**: Video games save character state to checkpoints. The Memento captures all game state;
   the Caretaker manages save slots.

4. **Wizard Navigation**: Multi-step forms with "Back" and "Next" navigation. Each step's state is saved as a memento.
   Going back restores the previous step's state.

5. **Crash Recovery**: Long-running processes save checkpoint mementos to disk periodically. On crash, the process
   restores the latest checkpoint.

### Framework Examples

- **Java `Serializable` + `ObjectOutputStream`**: A generic way to create mementos. The entire object graph is captured
  to a byte stream.
- **Spring `@SessionAttributes`**: Stores controller model state between requests — a memento-like pattern.
- **Git**: Git commits are mementos of the entire repository state. The commit object (memento) is opaque; Git (
  originator) knows how to restore it.
- **Databases (MVCC)**: Multi-version concurrency control keeps old row versions as mementos for transaction isolation.

## When NOT to Use

1. **Large State, Frequent Snapshots**: If each memento captures a large object graph and snapshots are taken every
   keystroke, memory grows quickly. Use incremental mementos (store only changes) or limit history depth.

2. **Simple Undo with Inverse Operations**: If undo can be computed as the inverse of the forward operation (e.g.,
   insert → delete, add → subtract), use Command pattern with inverse operations. This is more memory-efficient than
   full state snapshots.

3. **State Reconstruction from Log**: If you can reconstruct state by replaying events (Event Sourcing), Memento is
   unnecessary. Event Sourcing stores events; Memento stores state. Event Sourcing is more flexible but more complex.

4. **No Encapsulation Concern**: If the originator already exposes all state (DTOs, simple POJOs), Memento's
   encapsulation protection provides no benefit.

5. **Distributed Systems**: Memento assumes the originator and caretaker share memory. In distributed systems, serialize
   mementos to bytes and store in a database.

## Interview Questions

### Q1: Explain the Memento pattern and its three participants.

**Answer**: Memento captures an object's state externally without breaking encapsulation. Three participants: *
*Originator** (creates/consumes mementos), **Memento** (opaque state holder), **Caretaker** (manages mementos, never
inspects them). The Memento is like a sealed envelope — only the Originator can open it.

### Q2: How does Memento maintain encapsulation?

**Answer**: The Memento's state fields are private. The Originator can access them (via nested class or package-private
access) but the Caretaker cannot. The Memento exposes only metadata (timestamp, label) publicly. This ensures the
Originator's internal state is never exposed to external code.

### Q3: What's the difference between Memento and Command patterns for undo?

**Answer**: Command stores the OPERATION (with inverse for undo). Memento stores the STATE (full snapshot). Command uses
less memory (just the delta) but requires every operation to implement `undo()`. Memento uses more memory (full
snapshots) but works universally — any change is "reversible" by restoring state.

### Q4: How would you handle large state in Memento?

**Answer**: Use incremental mementos — store only the fields that changed (delta), not the entire object. Alternatively,
use a write-ahead log (Command pattern) instead of state snapshots. For very large state, serialize to disk rather than
keeping in memory. Limit history depth with an LRU eviction policy.

### Q5: How does Memento relate to Serialization in Java?

**Answer**: Java's `Serializable` interface provides a generic mechanism for creating mementos. `ObjectOutputStream`
captures the entire object graph (complete memento). `ObjectInputStream` restores it. However, serialization is
fragile (versioning, transient fields) and should not be exposed to external code requesting "save" — use Memento to
control what gets saved.

### Q6: What are the memory implications of Memento in a text editor?

**Answer**: Each keystroke creates a full document snapshot. A 10KB document × 100 undo levels = 1MB. This is acceptable
for text. For image editors (10MB per snapshot), a limit of 10-20 undo levels is typical. Optimization: store only the
changed text range (delta compression).

### Q7: How would you make Memento work with concurrent modifications?

**Answer**: The Memento should be immutable and capture state atomically. Use `synchronized` on `createMemento()` and
`restore()` to prevent partial captures. Alternatively, use immutable data structures (records, persistent collections)
so the Memento safely shares data with the Originator without copying.

### Q8: What's the difference between Memento and Prototype patterns?

**Answer**: Memento captures state for later restoration (time-based — save now, restore later). Prototype creates a
clone for independent modification (space-based — copy now, modify separately). A Prototype clone CAN be used as a
Memento, but Memento's purpose is specifically undo/rollback.

### Follow-Up Question

**Interviewer**: "How would you design a Memento for an object graph with circular references?"

**Answer**: Use a visitor pattern to traverse the object graph and store each object's state in a flat map keyed by
identity. On restore, traverse the same graph and set fields back. Java serialization handles circular references
natively via reference tracking. For custom implementations, use identity-based visited sets to avoid infinite loops.

## Pros & Cons

### Advantages

- **Encapsulation Preserved**: Internal state stays private; only the Originator can access Memento internals
- **Simplified Originator**: Save/restore logic is in the Memento, not scattered across the Originator
- **Caretaker Agnostic**: The Caretaker manages lifecycle without knowing state structure
- **Checkpoint/Rollback**: Clean undo mechanism without modifying the Originator's interface
- **Snapshot Integrity**: Mementos are immutable snapshots — no corruption from later modifications

### Disadvantages

- **Memory Consumption**: Full state snapshots can be large, especially for complex objects
- **No Delta Support**: Pattern standard stores full state; incremental snapshots require custom extension
- **Serialization Complexity**: For deep object graphs, creating truly immutable mementos is complex
- **Versioning Problems**: If the Originator's structure changes, old mementos become incompatible
- **Overhead for Simple State**: A single-int counter doesn't warrant a full Memento class

## Related Patterns

### Memento vs Command

**Command** stores the operation's inverse for undo. **Memento** stores the full state snapshot. Use Command when
operations are well-defined and memory is constrained. Use Memento when state is small or operations are too complex to
invert. They're often combined: Command uses a Memento to save state before executing, simplifying undo to "restore
memento."

### Memento vs Prototype

**Prototype** creates a clone of an object as a starting point for modification. **Memento** captures state for
restoration. A Prototype clone used for undo is effectively a Memento. The difference is intent: Prototype creates;
Memento preserves. In practice, `clone()` can implement `createMemento()` cheaply.

### Memento vs State

**State** changes behavior by switching internal state objects. **Memento** captures state for external restoration.
They combine: a State-based object can use Memento to save its current state object before transitioning, enabling undo
of state transitions.

## Key Takeaways

1. **"The sealed envelope"** — Memento proves that encapsulation and external state saving are not contradictory. The
   Memento is opaque to all except the Originator.

2. **Only the Originator knows** — The key insight: only the Originator knows what constitutes its state and how to
   capture/restore it. The Caretaker never inspects mementos.

3. **Memory vs simplicity trade-off** — Full snapshots (Memento) vs inverse operations (Command). Choose based on state
   size and operation complexity.

4. **Immutable mementos are safer** — Making Mementos immutable prevents accidental corruption and avoids
   synchronization issues.

5. **Interview memory aid** — "Memento = state snapshot, opaque memento, originator-only access, undo/checkpoint
   pattern."
