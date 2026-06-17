# State Pattern

## Overview

**Definition**: State allows an object to alter its behavior when its internal state changes. The object will appear to
change its class. Each state is encapsulated in its own class, and the Context delegates behavior to the current state
object.

**Core Problem**: How to model objects that behave differently depending on their internal state, without using massive
conditional statements that are hard to maintain and extend.

**One-Line Interview Answer**: "State pattern encapsulates state-specific behavior into separate classes, letting the
Context object change its behavior by delegating to a different state object, essentially appearing to change its type
at runtime."

## Problem Statement

### Real-World Scenario: Document Workflow

A document management system has a `Document` that can be in one of several states: `DRAFT`, `MODERATION`, `PUBLISHED`,
`ARCHIVED`. The actions available depend on the state:

- `DRAFT`: can edit, submit for review, or delete
- `MODERATION`: can approve, reject, or edit (if you're the author)
- `PUBLISHED`: can archive or unpublish, but cannot edit
- `ARCHIVED`: can view only, cannot modify

Naive approach with conditionals:

```java
public class Document {
    private String state; // "DRAFT", "MODERATION", "PUBLISHED", "ARCHIVED"
    private String content;
    private User author;

    public void publish() {
        if (state.equals("DRAFT")) {
            // send for moderation
            state = "MODERATION";
            System.out.println("Sent for moderation");
        } else if (state.equals("MODERATION")) {
            if (currentUser.isAdmin()) {
                state = "PUBLISHED";
                System.out.println("Published!");
            } else {
                throw new IllegalStateException("Only admins can publish");
            }
        } else if (state.equals("PUBLISHED")) {
            throw new IllegalStateException("Already published");
        } else if (state.equals("ARCHIVED")) {
            throw new IllegalStateException("Cannot publish archived document");
        }
    }

    public void edit(String newContent) {
        if (state.equals("DRAFT")) {
            this.content = newContent;
        } else if (state.equals("MODERATION")) {
            if (currentUser.equals(author)) {
                this.content = newContent;
            } else {
                throw new IllegalStateException("Only author can edit in moderation");
            }
        } else {
            throw new IllegalStateException("Cannot edit in " + state);
        }
    }

    public void delete() {
        if (state.equals("DRAFT") || state.equals("ARCHIVED")) {
            // delete logic
        } else {
            throw new IllegalStateException("Cannot delete in " + state);
        }
    }
    // Every new state requires modifying ALL methods
}
```

### Pain Points of the Naive Approach

1. **Exploding Conditionals**: Every method has conditional logic for every state. Adding a new state (e.g.,
   `SCHEDULED`) requires modifying every method.
2. **Incomplete Transitions**: It's easy to forget a state transition in one method, leading to inconsistent behavior.
3. **Business Logic Scattered**: The rules for state transitions are spread across multiple methods, making it hard to
   see the full state machine.
4. **Violates Open/Closed Principle**: Adding a new state means touching all existing methods, risking regression.
5. **Readability Disaster**: After 4-5 states, each method becomes a tangled web of if-else blocks.

### Why This Matters in Production

Workflows, document lifecycles, order processing, and game states are all state machines. Airlines process bookings
through RESERVED → CHECKED_IN → BOARDED → FLOWN → CANCELLED. Without State, these become unmaintainable. With State,
adding "VIP_PRIORITY_QUEUE" is just one new class.

## Solution

### How State Solves This

State extracts each state's behavior and transitions into its own class. The Context delegates all state-dependent calls
to the current State object, which also handles transitions to other states.

### Key Participants

| Participant         | Role                                                                                  |
|---------------------|---------------------------------------------------------------------------------------|
| `State` (interface) | Declares methods for all actions in the context                                       |
| `ConcreteState`     | Implements behavior for a specific state; may transition to other states              |
| `Context`           | Maintains reference to current State; delegates all state-specific calls              |
| `Transition`        | A change from one state to another, typically triggered within a ConcreteState method |

### Step-by-Step Flow

1. Context receives a method call (e.g., `document.publish()`)
2. Context delegates to `currentState.publish(this)`
3. The ConcreteState executes the appropriate behavior
4. If a transition is needed, the ConcreteState calls `context.setState(new ModerationState())`
5. The Context's behavior now reflects the new state

### UML-Style Structure

```
┌─────────────┐       ┌──────────────────┐
│   Context   │───────│   «interface»    │
│             │       │      State       │
│ -state:State│       │                  │
│─────────────│       │ +handleA()       │
│ +request()  │       │ +handleB()       │
│ +setState() │       └────────┬─────────┘
└─────────────┘                │
                 ┌─────────────┼──────────────┐
                 │             │              │
        ┌────────┴───┐ ┌──────┴─────┐ ┌──────┴─────┐
        │StateA      │ │StateB      │ │StateC      │
        │─────────   │ │────────    │ │────────    │
        │+handleA()  │ │+handleA()  │ │+handleA()  │
        │+handleB()  │ │+handleB()  │ │+handleB()  │
        └────────────┘ └────────────┘ └────────────┘
```

## Java Implementation

### State Interface

```java
package behavioral.state;

interface DocumentState {
    void edit(Document context, String newContent, User user);
    void submit(Document context, User user);
    void approve(Document context, User user);
    void reject(Document context, User user);
    void archive(Document context, User user);
    void delete(Document context, User user);
    String getStateName();
}
```

### Context Class

```java
import java.time.Instant;

class Document {
    private String content;
    private final User author;
    private Instant createdAt;
    private Instant publishedAt;
    private DocumentState state;

    public Document(String content, User author) {
        this.content = content;
        this.author = author;
        this.createdAt = Instant.now();
        this.state = new DraftState();
    }

    void setState(DocumentState state) {
        this.state = state;
    }

    String getContent() { return content; }
    void setContent(String content) { this.content = content; }
    User getAuthor() { return author; }
    Instant getCreatedAt() { return createdAt; }
    Instant getPublishedAt() { return publishedAt; }
    void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }

    // Delegation methods
    public void edit(String newContent, User user) {
        state.edit(this, newContent, user);
    }

    public void submit(User user) {
        state.submit(this, user);
    }

    public void approve(User user) {
        state.approve(this, user);
    }

    public void reject(User user) {
        state.reject(this, user);
    }

    public void archive(User user) {
        state.archive(this, user);
    }

    public void delete(User user) {
        state.delete(this, user);
    }

    public String getStatus() {
        return state.getStateName();
    }
}

record User(String name, Role role) {
    enum Role { AUTHOR, REVIEWER, ADMIN }
}
```

### Concrete States

```java
class DraftState implements DocumentState {
    @Override
    public void edit(Document context, String newContent, User user) {
        context.setContent(newContent);
        System.out.println("Draft updated by " + user.name());
    }

    @Override
    public void submit(Document context, User user) {
        if (user.equals(context.getAuthor()) || user.role() == User.Role.ADMIN) {
            context.setState(new ModerationState());
            System.out.println("Document submitted for moderation");
        } else {
            throw new IllegalStateException("Only the author can submit");
        }
    }

    @Override
    public void approve(Document context, User user) {
        throw new IllegalStateException("Cannot approve a draft — submit first");
    }

    @Override
    public void reject(Document context, User user) {
        throw new IllegalStateException("Cannot reject a draft");
    }

    @Override
    public void archive(Document context, User user) {
        context.setState(new ArchivedState());
        System.out.println("Draft archived");
    }

    @Override
    public void delete(Document context, User user) {
        if (user.equals(context.getAuthor())) {
            System.out.println("Document deleted permanently");
            // Actual deletion logic
        } else {
            throw new IllegalStateException("Only author can delete");
        }
    }

    @Override
    public String getStateName() { return "DRAFT"; }
}

class ModerationState implements DocumentState {
    @Override
    public void edit(Document context, String newContent, User user) {
        if (user.equals(context.getAuthor())) {
            context.setContent(newContent);
            System.out.println("Content updated during moderation");
        } else {
            throw new IllegalStateException("Only author can edit during moderation");
        }
    }

    @Override
    public void submit(Document context, User user) {
        System.out.println("Already in moderation");
    }

    @Override
    public void approve(Document context, User user) {
        if (user.role() == User.Role.ADMIN || user.role() == User.Role.REVIEWER) {
            context.setPublishedAt(Instant.now());
            context.setState(new PublishedState());
            System.out.println("Document approved and published by " + user.name());
        } else {
            throw new IllegalStateException("Only reviewers/admins can approve");
        }
    }

    @Override
    public void reject(Document context, User user) {
        if (user.role() == User.Role.ADMIN || user.role() == User.Role.REVIEWER) {
            context.setState(new DraftState());
            System.out.println("Document rejected, returned to draft");
        } else {
            throw new IllegalStateException("Only reviewers/admins can reject");
        }
    }

    @Override
    public void archive(Document context, User user) {
        throw new IllegalStateException("Cannot archive during moderation");
    }

    @Override
    public void delete(Document context, User user) {
        throw new IllegalStateException("Cannot delete during moderation");
    }

    @Override
    public String getStateName() { return "MODERATION"; }
}

class PublishedState implements DocumentState {
    @Override
    public void edit(Document context, String newContent, User user) {
        throw new IllegalStateException("Cannot edit a published document");
    }

    @Override
    public void submit(Document context, User user) {
        throw new IllegalStateException("Already published");
    }

    @Override
    public void approve(Document context, User user) {
        throw new IllegalStateException("Already published");
    }

    @Override
    public void reject(Document context, User user) {
        if (user.role() == User.Role.ADMIN) {
            context.setState(new DraftState());
            System.out.println("Published document reverted to draft by admin");
        } else {
            throw new IllegalStateException("Only admins can unpublish");
        }
    }

    @Override
    public void archive(Document context, User user) {
        context.setState(new ArchivedState());
        System.out.println("Document archived");
    }

    @Override
    public void delete(Document context, User user) {
        throw new IllegalStateException("Cannot delete a published document — archive first");
    }

    @Override
    public String getStateName() { return "PUBLISHED"; }
}

class ArchivedState implements DocumentState {
    @Override
    public void edit(Document context, String newContent, User user) {
        throw new IllegalStateException("Cannot edit an archived document");
    }

    @Override
    public void submit(Document context, User user) {
        throw new IllegalStateException("Archived document cannot be submitted");
    }

    @Override
    public void approve(Document context, User user) {
        throw new IllegalStateException("Cannot approve an archived document");
    }

    @Override
    public void reject(Document context, User user) {
        throw new IllegalStateException("Cannot reject an archived document");
    }

    @Override
    public void archive(Document context, User user) {
        System.out.println("Already archived");
    }

    @Override
    public void delete(Document context, User user) {
        if (user.role() == User.Role.ADMIN) {
            System.out.println("Archived document permanently deleted by admin");
        } else {
            throw new IllegalStateException("Only admins can delete archived documents");
        }
    }

    @Override
    public String getStateName() { return "ARCHIVED"; }
}
```

### Usage Demo

```java
public class StateDemo {
    public static void main(String[] args) {
        var author = new User("Alice", User.Role.AUTHOR);
        var reviewer = new User("Bob", User.Role.REVIEWER);

        var doc = new Document("Initial content", author);

        doc.edit("Updated content", author);
        System.out.println("Status: " + doc.getStatus());

        doc.submit(author);
        System.out.println("Status: " + doc.getStatus());

        try {
            doc.edit("Hacked content", reviewer);
        } catch (IllegalStateException e) {
            System.out.println("Expected error: " + e.getMessage());
        }

        doc.approve(reviewer);
        System.out.println("Status: " + doc.getStatus());

        doc.archive(author);
        System.out.println("Status: " + doc.getStatus());

        try {
            doc.edit("Should fail", author);
        } catch (IllegalStateException e) {
            System.out.println("Expected error: " + e.getMessage());
        }
    }
}
```

### Finite State Machine (FSM) with Table-Driven Transitions

```java
package behavioral.state.fsm;

import java.util.*;

enum DocState { DRAFT, MODERATION, PUBLISHED, ARCHIVED }
enum Event { SUBMIT, APPROVE, REJECT, ARCHIVE, DELETE }

class DocumentFSM {
    private DocState currentState = DocState.DRAFT;
    private static final Map<DocState, Map<Event, DocState>> transitions = new EnumMap<>(DocState.class);

    static {
        var draft = new EnumMap<Event, DocState>(Event.class);
        draft.put(Event.SUBMIT, DocState.MODERATION);
        draft.put(Event.DELETE, null);
        draft.put(Event.ARCHIVE, DocState.ARCHIVED);
        transitions.put(DocState.DRAFT, draft);

        var mod = new EnumMap<Event, DocState>(Event.class);
        mod.put(Event.APPROVE, DocState.PUBLISHED);
        mod.put(Event.REJECT, DocState.DRAFT);
        transitions.put(DocState.MODERATION, mod);

        var pub = new EnumMap<Event, DocState>(Event.class);
        pub.put(Event.ARCHIVE, DocState.ARCHIVED);
        transitions.put(DocState.PUBLISHED, pub);

        transitions.put(DocState.ARCHIVED, new EnumMap<>(Event.class));
    }

    public void apply(Event event) {
        DocState next = transitions.get(currentState).get(event);
        if (next == null && transitions.get(currentState).containsKey(event)) {
            System.out.println("Document deleted");
            return;
        }
        if (next == null) {
            throw new IllegalStateException("Cannot " + event + " in " + currentState);
        }
        System.out.println(currentState + " --(" + event + ")--> " + next);
        currentState = next;
    }
}
```

## When to Use

1. **Workflow Engines**: Document lifecycle, order processing (NEW → PAID → SHIPPED → DELIVERED → RETURNED), approval
   chains. Each state has distinct allowed operations and transitions.

2. **UI Navigation**: Wizard-style interfaces where each step has different controls and validation. A "CheckoutWizard"
   with CART → SHIPPING → PAYMENT → CONFIRMATION states.

3. **Game Character States**: Player states like IDLE, RUNNING, JUMPING, FALLING, ATTACKING. Each state determines which
   animations play and which inputs are accepted.

4. **Network Connection States**: TCP connection states (CLOSED, SYN_SENT, ESTABLISHED, CLOSE_WAIT). Each state
   determines how packets are processed and which transitions are valid.

5. **Vending Machines**: IDLE → SELECTING → DISPENSING → OUT_OF_STOCK. The machine's behavior (accept coins, dispense
   item, return change) depends on its current state.

### Framework Examples

- **Spring State Machine**: Full-featured state machine framework implementing the State pattern with guards, actions,
  and hierarchical states.
- **Jakarta Persistence (JPA) Lifecycle**: Entity states (NEW, MANAGED, DETACHED, REMOVED) affect how the persistence
  context interacts with entities.
- **Java `Thread.State`**: Thread lifecycle states (NEW, RUNNABLE, BLOCKED, WAITING, TIMED_WAITING, TERMINATED).

## When NOT to Use

1. **Few States, Simple Behavior**: If you have only 2-3 states with minimal behavior differences, a boolean flag and
   simple conditionals are clearer.

2. **Stateless Behavior**: If state doesn't affect behavior (just data), State adds unintended complexity. Use a simple
   field.

3. **Many States but Few Actions**: If you have 20 states but only 2 actions, the interface forces every state to
   implement (or throw) all methods. Consider a table-driven FSM instead.

4. **High-Frequency Transitions**: If objects transition states thousands of times per second, the delegation overhead
   and object creation for new state instances may matter. Use enum-based State or FSM tables.

5. **When Strategy Fits Better**: If the "state" is actually a configurable algorithm chosen by the client (not
   automatically transitioned), use Strategy. State's key feature is automatic transitions driven by the state itself.

## Interview Questions

### Q1: Explain the State pattern and how it differs from Strategy.

**Answer**: State lets an object change behavior when its internal state changes, appearing to change its class. The key
difference from Strategy: in Strategy, the client chooses and sets the algorithm; in State, the state transitions happen
automatically within the state classes. Strategy is "how to do it" (client picks). State is "what to do" (state
decides).

### Q2: How does State violate or uphold the Open/Closed Principle?

**Answer**: State upholds OCP. Adding a new state means creating a new ConcreteState class — no existing code changes.
Existing states don't need modification. However, adding a new action requires adding a method to the State interface
and all existing concrete states, which violates OCP. This trade-off is inherent to the pattern.

### Q3: How does the State pattern relate to finite state machines?

**Answer**: State pattern is the OOP implementation of an FSM. Each state in the FSM maps to a ConcreteState class, and
transitions map to method calls that return (or set) the next state. Table-driven FSMs (transition matrices) are an
alternative that works better for large state spaces.

### Q4: What happens when a state needs to know about the Context?

**Answer**: The State receives a reference to the Context in every method call. This is intentional — the State calls
`context.setState(newState)` to trigger transitions. However, this creates a bidirectional dependency. Mitigate by
keeping the Context interface minimal and only exposing transition-related methods.

### Q5: How would you handle concurrent state access?

**Answer**: Use immutable state objects — each new state is a new instance, so there's no shared mutable state between
states. For the Context, synchronize the `setState()` and delegation methods, or use an `AtomicReference<State>` for
lock-free transitions.

### Q6: Can the State pattern be implemented without conditional logic?

**Answer**: Yes — that's the whole point. Each state decision is pushed into a separate class selected by polymorphism.
Instead of `if (state == DRAFT)`, you call `state.handle(context)`. The conditionals are replaced by virtual method
dispatch.

### Q7: What's the difference between "state" as a pattern and "state" as a variable?

**Answer**: State as a variable holds data (e.g., `isPublished = true`). State as a pattern encapsulates behavior. A
boolean "state" only affects data flow. State pattern affects control flow — it changes what methods DO. The difference
is behavior vs value.

### Q8: How would you implement a hard-to-reverse transition (e.g., subscription cancellation)?

**Answer**: Model irreversible transitions explicitly. The state can be terminal (no outgoing transitions). In the State
pattern, the terminal state either throws `IllegalStateException` for all actions or transitions to a garbage-collected
state. Document the irreversible nature in the state diagram.

### Follow-Up Question

**Interviewer**: "Design a state machine for an elevator. How does the State pattern apply?"

**Answer**: States: IDLE, MOVING_UP, MOVING_DOWN, DOOR_OPEN, EMERGENCY_STOP. Actions: requestFloor, openDoor, closeDoor,
emergencyStop, sensorTriggered. IDLE → openDoor() → DOOR_OPEN. DOOR_OPEN → closeDoor() → IDLE. IDLE → requestFloor() →
MOVING_UP/MOVING_DOWN. Each state has guards (e.g., MOVING_UP ignores DOWN requests above current floor via a priority
queue).

## Pros & Cons

### Advantages

- **Eliminates Conditionals**: Replaces if-else/switch with polymorphism
- **Open/Closed Principle**: New states added without modifying existing ones
- **Single Responsibility**: Each state class handles one state's behavior
- **State Transitions Explicit**: All transitions are visible in each state class
- **Self-Documenting**: The state classes form a readable state machine
- **Testing**: Each state can be tested independently

### Disadvantages

- **Class Explosion**: Each state is a new class; many states = many files
- **Interface Pollution**: Adding a new action requires changes to all states
- **Context-State Coupling**: Bidirectional references can be confusing
- **Overkill for Simple States**: Boolean/enum flags are simpler for trivial cases
- **Transition Logic Spread**: Transitions are buried inside state methods, not centralized

## Related Patterns

### State vs Strategy

The most common comparison. **Strategy**: the client explicitly sets the algorithm; the Context doesn't control when
strategies change. **State**: the state object controls transitions; the client never sees state objects directly. In
Strategy, you write `context.setStrategy(new FastAlgorithm())`. In State, the Context's state changes automatically when
you call `context.request()`.

### State vs Flyweight

**Flyweight** can be combined with State to share state objects. If a state has no instance fields (behavior only), it's
a stateless flyweight. Multiple Contexts can share the same State instance. All states in our `DocumentState` example
are flyweight-compatible.

### State vs Command

**Command** encapsulates a single request as an object. **State** encapsulates behavior tied to a state. A Command is
one-shot; State is persistent until transitioned. An undo system might use Command for actions and State to track
document mode (EDITING, READ_ONLY, REVIEWING).

## Key Takeaways

1. **"Polymorphism beats conditionals"** — State replaces state-based conditionals with virtual method dispatch. This is
   the core insight.

2. **Automatic transitions** — The defining feature of State (vs Strategy) is that states control their own transitions
   via `context.setState()`.

3. **OCP with a caveat** — Adding states is OCP-friendly. Adding actions is not. Design the interface carefully.

4. **Every state is a class** — This class explosion is the main trade-off. For 10+ states, consider a table-driven FSM.

5. **Interview memory aid** — "State = state-specific behavior, automatic transitions, polymorphic dispatch. Strategy =
   client chooses. State = state chooses."
