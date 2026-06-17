# Mediator Pattern

## Overview

**Definition**: Mediator defines an object that encapsulates how a set of objects interact, promoting loose coupling by
preventing objects from referring to each other explicitly. It centralizes otherwise chaotic inter-object communication.

**Core Problem**: How to reduce the complex web of many-to-many communication between objects into a controlled
one-to-many relationship mediated by a central hub.

**One-Line Interview Answer**: "Mediator centralizes communication between components, turning chaotic many-to-many
relationships into clean one-to-many, making the system easier to maintain, extend, and reason about."

## Problem Statement

### Real-World Scenario: Air Traffic Control

Without a mediator, every airplane would need to communicate directly with every other airplane to avoid collisions,
coordinate landing slots, and share weather information:

```java
public class Airplane {
    private List<Airplane> allPlanes;

    public void requestLanding() {
        for (Airplane other : allPlanes) {
            if (other.isOnRunway() || other.isLanding()) {
                System.out.println("Holding — runway busy");
                return;
            }
        }
        // Every plane needs to know about every other plane
        // Adding a helicopter means modifying Airplane class
        // Ground vehicles also need to communicate
        System.out.println("Cleared for landing");
    }
}
```

A software analogy: a UI dialog with multiple components (text fields, dropdowns, buttons, sliders). Without Mediator,
every component that needs to react to another's change must know about every other component:

```java
public class LoginDialog {
    private TextField usernameField;
    private TextField passwordField;
    private Checkbox rememberMe;
    private Button loginButton;
    private Label errorLabel;

    public void onUsernameChanged() {
        if (usernameField.getText().isEmpty()) {
            loginButton.setEnabled(false);
            errorLabel.setText("Username required");
        } else if (passwordField.getText().length() < 8) {
            loginButton.setEnabled(false);
            errorLabel.setText("Password too short");
        } else {
            loginButton.setEnabled(true);
            errorLabel.setText("");
        }
        // Each change handler knows about ALL other components
        // Adding a new component (CAPTCHA) requires modifying EVERY handler
    }
}
```

### Pain Points of the Naive Approach

1. **Tight Coupling**: Components hold direct references to each other. Every component knows every other component's
   API.
2. **Rigid Communication**: The web of connections is hardcoded. Adding a new component requires updating all existing
   connections.
3. **Code Scattered**: The logic for coordinating a change (e.g., enabling the login button) is duplicated across
   multiple event handlers.
4. **Reusability Impossible**: Components can't be reused in different dialogs because they're wired to specific peers.
5. **Testing Nightmare**: Testing one component requires instantiating every component it talks to.

### Why This Matters in Production

UI frameworks (JavaFX, Swing, Android), chat systems, microservice orchestration, and complex wizards all face this
chaos. Without Mediator, the communication graph grows as O(n²), making maintenance unsustainable beyond 5-10
components.

## Solution

### How Mediator Solves This

The mediator sits between all components. Components notify the mediator of events. The mediator knows about all
components and coordinates their reactions. Components only know the mediator interface — they never reference each
other.

### Key Participants

| Participant            | Role                                                |
|------------------------|-----------------------------------------------------|
| `Mediator` (interface) | Declares the communication contract for components  |
| `ConcreteMediator`     | Coordinates communication between colleague objects |
| `Colleague`            | Components that communicate through the mediator    |

### Step-by-Step Flow

1. Colleague A's state changes (e.g., checkbox checked)
2. Colleague A calls `mediator.notify(this, "CHECKED")`
3. Mediator receives the notification, identifies the sender, and the event
4. Mediator determines which colleagues need to react
5. Mediator calls the appropriate methods on Colleague B, C, etc.
6. Colleagues never call each other directly

### UML-Style Structure

```
┌──────────────┐       ┌──────────────────┐
│  «interface» │       │  «interface»     │
│  Colleague   │───────│   Mediator       │
│              │       │                  │
│ +notify()    │       │ +notify(sender,  │
└──────────────┘       │          event)  │
                        └────────┬─────────┘
                                 │
              ┌──────────────────┼──────────────────┐
              │                  │                  │
     ┌────────┴───────┐ ┌───────┴────────┐ ┌───────┴────────┐
     │ColleagueA      │ │ConcreteMediator│ │ColleagueB      │
     │                │ │                │ │                │
     │[component]     │ │[orchestrator]  │ │[component]     │
     └────────────────┘ └────────────────┘ └────────────────┘
```

## Java Implementation

### Mediator Interface and Colleagues

```java
package behavioral.mediator;

import java.util.ArrayList;
import java.util.List;

interface DialogMediator {
    void notify(UiComponent sender, String event);
}
```

### Colleague Base

```java
abstract class UiComponent {
    protected DialogMediator mediator;
    protected String name;

    public UiComponent(String name) {
        this.name = name;
    }

    public void setMediator(DialogMediator mediator) {
        this.mediator = mediator;
    }

    public abstract void handleEvent(String event);
}
```

### Concrete Colleagues

```java
class TextField extends UiComponent {
    private String text = "";

    public TextField(String name) {
        super(name);
    }

    public void setText(String text) {
        this.text = text;
        System.out.println("[" + name + "] Text set to: '" + text + "'");
        mediator.notify(this, "TEXT_CHANGED");
    }

    public String getText() { return text; }

    @Override
    public void handleEvent(String event) {
        // TextField reacts to events from mediator
        if (event.equals("CLEAR")) {
            this.text = "";
            System.out.println("[" + name + "] Cleared");
        } else if (event.equals("FOCUS")) {
            System.out.println("[" + name + "] Focused");
        }
    }
}

class Checkbox extends UiComponent {
    private boolean checked;

    public Checkbox(String name) {
        super(name);
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
        System.out.println("[" + name + "] Checked: " + checked);
        mediator.notify(this, "CHECK_CHANGED");
    }

    public boolean isChecked() { return checked; }

    @Override
    public void handleEvent(String event) {
        if (event.equals("RESET")) {
            this.checked = false;
            System.out.println("[" + name + "] Reset");
        }
    }
}

class Button extends UiComponent {
    private boolean enabled = true;

    public Button(String name) {
        super(name);
    }

    public void click() {
        if (enabled) {
            System.out.println("[" + name + "] Clicked!");
            mediator.notify(this, "CLICKED");
        } else {
            System.out.println("[" + name + "] Disabled — click ignored");
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        System.out.println("[" + name + "] Enabled: " + enabled);
    }

    @Override
    public void handleEvent(String event) {
        // Buttons typically don't react to events
    }
}

class Label extends UiComponent {
    private String text = "";

    public Label(String name) {
        super(name);
    }

    public void setText(String text) {
        this.text = text;
        System.out.println("[" + name + "] Display: " + text);
    }

    @Override
    public void handleEvent(String event) {
        // Labels are passive; they only receive text
    }
}
```

### Concrete Mediator (Login Dialog)

```java
class LoginDialogMediator implements DialogMediator {
    private final TextField usernameField;
    private final TextField passwordField;
    private final Checkbox rememberMe;
    private final Button loginButton;
    private final Button cancelButton;
    private final Label errorLabel;

    public LoginDialogMediator() {
        // Create components
        this.usernameField = new TextField("Username");
        this.passwordField = new TextField("Password");
        this.rememberMe = new Checkbox("RememberMe");
        this.loginButton = new Button("Login");
        this.cancelButton = new Button("Cancel");
        this.errorLabel = new Label("Error");

        // Register with mediator
        usernameField.setMediator(this);
        passwordField.setMediator(this);
        rememberMe.setMediator(this);
        loginButton.setMediator(this);
        cancelButton.setMediator(this);
        errorLabel.setMediator(this);
    }

    @Override
    public void notify(UiComponent sender, String event) {
        switch (event) {
            case "TEXT_CHANGED" -> onTextChanged();
            case "CHECK_CHANGED" -> onCheckChanged();
            case "CLICKED" -> onButtonClicked(sender);
        }
    }

    private void onTextChanged() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isBlank()) {
            errorLabel.setText("Username is required");
            loginButton.setEnabled(false);
        } else if (password.length() < 8) {
            errorLabel.setText("Password must be at least 8 characters");
            loginButton.setEnabled(false);
        } else {
            errorLabel.setText("");
            loginButton.setEnabled(true);
        }
    }

    private void onCheckChanged() {
        if (rememberMe.isChecked()) {
            System.out.println("[MEDIATOR] Remember Me enabled — token will persist");
        } else {
            System.out.println("[MEDIATOR] Remember Me disabled");
        }
    }

    private void onButtonClicked(UiComponent sender) {
        if (sender == loginButton) {
            System.out.println("[MEDIATOR] Authenticating user...");
            // Validate credentials, start session
        } else if (sender == cancelButton) {
            System.out.println("[MEDIATOR] Cancelling — clearing all fields");
            usernameField.handleEvent("CLEAR");
            passwordField.handleEvent("CLEAR");
            rememberMe.handleEvent("RESET");
            errorLabel.setText("Cancelled");
            loginButton.setEnabled(false);
        }
    }

    // Expose components for external access
    public TextField getUsernameField() { return usernameField; }
    public TextField getPasswordField() { return passwordField; }
    public Button getLoginButton() { return loginButton; }
}
```

### Usage Demo

```java
public class MediatorDemo {
    public static void main(String[] args) {
        var mediator = new LoginDialogMediator();

        var username = mediator.getUsernameField();
        var password = mediator.getPasswordField();
        var loginBtn = mediator.getLoginButton();

        System.out.println("=== User types partial username ===");
        username.setText("user");

        System.out.println("\n=== User types short password ===");
        password.setText("123");

        System.out.println("\n=== User types valid password ===");
        password.setText("securePass123");

        System.out.println("\n=== User clicks login ===");
        loginBtn.click();
    }
}
```

### Chat Room Mediator

```java
import java.time.Instant;

// Another example: Chat Room
interface ChatMediator {
    void sendMessage(User sender, String message);
    void join(User user);
    void leave(User user);
}

class ChatRoom implements ChatMediator {
    private final List<User> users = new ArrayList<>();

    @Override
    public void join(User user) {
        users.add(user);
        broadcast("[SYSTEM]", user.getName() + " joined the chat");
    }

    @Override
    public void leave(User user) {
        users.remove(user);
        broadcast("[SYSTEM]", user.getName() + " left the chat");
    }

    @Override
    public void sendMessage(User sender, String message) {
        broadcast(sender.getName(), message);
    }

    private void broadcast(String senderName, String message) {
        String formatted = String.format("[%tT] %s: %s", Instant.now(), senderName, message);
        for (User user : users) {
            user.receiveMessage(formatted);
        }
    }
}

class User {
    private final String name;
    private final ChatMediator chat;

    public User(String name, ChatMediator chat) {
        this.name = name;
        this.chat = chat;
    }

    public String getName() { return name; }

    public void send(String message) {
        System.out.println(name + " sends: " + message);
        chat.sendMessage(this, message);
    }

    public void receiveMessage(String message) {
        System.out.println("[" + name + " receives] " + message);
    }
}

class ChatRoomDemo {
    public static void main(String[] args) {
        var chat = new ChatRoom();

        var alice = new User("Alice", chat);
        var bob = new User("Bob", chat);
        var charlie = new User("Charlie", chat);

        chat.join(alice);
        chat.join(bob);
        alice.send("Hello everyone!");

        chat.join(charlie);
        bob.send("Hi Alice!");
        charlie.send("Hey team!");

        chat.leave(bob);
        alice.send("Bob left. Anyone else?");
    }
}
```

### EventBus-Style Mediator (Lightweight)

```java
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

class EventBus {
    private final ConcurrentHashMap<Class<?>, CopyOnWriteArrayList<Consumer<?>>> listeners = new ConcurrentHashMap<>();

    public <T> void subscribe(Class<T> eventType, Consumer<T> handler) {
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(handler);
    }

    @SuppressWarnings("unchecked")
    public <T> void publish(T event) {
        var handlers = listeners.get(event.getClass());
        if (handlers != null) {
            handlers.forEach(h -> ((Consumer<T>) h).accept(event));
        }
    }
}

record UserLoggedIn(String username, long timestamp) {}
record UserLoggedOut(String username) {}

class EventBusDemo {
    public static void main(String[] args) {
        var bus = new EventBus();
        bus.subscribe(UserLoggedIn.class, e ->
            System.out.println("Audit: " + e.username() + " logged in"));
        bus.subscribe(UserLoggedIn.class, e ->
            System.out.println("Welcome " + e.username() + "!"));
        bus.subscribe(UserLoggedOut.class, e ->
            System.out.println("Session ended for " + e.username()));
        bus.publish(new UserLoggedIn("alice", System.currentTimeMillis()));
        bus.publish(new UserLoggedOut("alice"));
    }
}
```

## When to Use

1. **Complex UI Dialogs**: Wizards, settings panels, dashboards with interdependent widgets. A change in one widget
   affects multiple others — the mediator orchestrates this coordination.

2. **Chat Systems**: Chat rooms where users join/leave and messages must be broadcast to all participants without users
   knowing about each other directly.

3. **Microservice Orchestration**: An orchestrator service that coordinates calls between services A, B, and C. The
   orchestrator is the mediator; services are colleagues.

4. **Multiplayer Game Lobbies**: A game lobby mediates communication between players, managing ready states, game start
   conditions, and chat. Players interact through the lobby, not directly.

5. **Air Traffic Control Systems**: The tower (mediator) coordinates communication between aircraft (colleagues).
   Aircraft never talk to each other directly.

### Framework Examples

- **JavaFX `DialogPane`**: The dialog pane acts as mediator between its buttons, header, and content area.
- **Spring MVC `DispatcherServlet`**: Acts as mediator between controllers, views, and model objects.
- **Android `Activity`/`Fragment`**: Often acts as mediator between UI components, coordinating their interactions.
- **Message Brokers (Kafka, RabbitMQ)**: Architectural-level mediators between producers and consumers.

## When NOT to Use

1. **Simple One-to-One Communication**: If only two objects need to talk, Mediator adds unnecessary indirection. Direct
   reference or Observer is simpler.

2. **Performance-Critical Connections**: All communication passes through the mediator, making it a bottleneck. In
   high-throughput systems, this can become a hot path.

3. **Mediator Becomes God Object**: If the mediator starts handling all business logic, it becomes a monolithic "god
   object" that's worse than the original coupling. Keep the mediator focused on COORDINATION, not execution.

4. **Components Already Decoupled**: If components communicate through clearly defined interfaces with few connections,
   Mediator adds unnecessary abstraction. Use it only when the "spaghetti" emerges.

5. **Runtime Dynamic Communication**: If colleagues join/leave frequently and unpredictably, the mediator must manage
   registration/deregistration, adding complexity. Consider Event Bus instead.

## Interview Questions

### Q1: What is the Mediator pattern and what problem does it solve?

**Answer**: Mediator centralizes communication between multiple objects, replacing many-to-many connections with
one-to-many. It solves "spaghetti code" where components hold direct references to each other, making the system rigid
and hard to extend. Components only know the mediator, not each other.

### Q2: How does Mediator differ from Observer?

**Answer**: Mediator is a centralized hub that knows about all colleagues and coordinates their interaction. Observer is
a decentralized broadcast from one subject to many observers. In a chat room, the chat server is a Mediator; users are
observers of each other's messages. Mediator is about "who coordinates"; Observer is about "who gets notified."

### Q3: How can the Mediator pattern become an anti-pattern?

**Answer**: If the mediator accumulates too much logic, it becomes a "god object" — a single class that knows everything
and does everything. This defeats the purpose of loose coupling. The fix: split the mediator into domain-specific
mediators (e.g., LoginDialogMediator, SettingsDialogMediator) rather than one monolithic ApplicationMediator.

### Q4: How does Mediator relate to Facade?

**Answer**: Both provide a simplified interface to a subsystem. Facade provides a READ-ONLY simplified view (
structuring). Mediator provides BIDIRECTIONAL coordination (behavior). Facade hides complexity from external clients.
Mediator enables communication between internal components. A Facade might use a Mediator internally.

### Q5: What is the relationship between Mediator and Dependency Injection?

**Answer**: DI frameworks simplify mediator implementation. The mediator can receive its colleague references via
constructor injection (Spring `@Autowired`). DI also helps avoid the "god object" problem by allowing domain-specific
mediators with focused responsibilities.

### Q6: How would you implement a mediator for undoable operations?

**Answer**: The mediator maintains a command history. When a colleague notifies the mediator of a change, the mediator
creates a Command object with before/after state, executes it, and pushes it onto the undo stack. The undo/redo commands
flow through the mediator back to the colleagues.

### Q7: What's the trade-off between simplicity and centralization in Mediator?

**Answer**: Mediator simplifies component-to-component connections (each component has one reference) but centralizes
control flow. The trade-off is between "many small interfaces" and "one large interface." For 3-5 components, direct
wiring is simpler. For 10+, Mediator wins.

### Q8: How would you test a mediator-based system?

**Answer**: Test colleagues in isolation by mocking the mediator (verify the correct notifications). Test the mediator
by creating mock colleagues and verifying it coordinates them correctly. Integration tests wire real colleagues with the
real mediator. This layered testing is a key benefit of the pattern.

### Follow-Up Question

**Interviewer**: "Design a mediator for a hotel booking system that coordinates room search, pricing, availability, and
booking confirmation components."

**Answer**: SearchComponent notifies the mediator of a search query. Mediator coordinates: calls AvailabilityComponent
to check rooms, PricingComponent to calculate rates, and returns unified results to the UI. On booking, the Mediator
orchestrates: lock room (Availability), process payment (PaymentComponent), send confirmation (NotificationComponent).
Each component only knows the Mediator interface.

## Pros & Cons

### Advantages

- **Reduces Coupling**: Components only know the Mediator, not each other
- **Centralized Control**: Coordination logic is in one place, not scattered
- **Simplifies Components**: Colleagues become simpler, focused on their own behavior
- **Easier to Extend**: Adding a new colleague only requires changing the mediator
- **Reusability**: Individual components can be reused in different mediators
- **Testing**: Component and mediator can be tested independently

### Disadvantages

- **Mediator Complexity**: The mediator can become a god object over time
- **Performance Bottleneck**: All communication passes through the mediator
- **Central Point of Failure**: If the mediator fails, the whole system fails
- **Indirection**: Following communication flow requires going through the mediator
- **Over-Engineering**: Overkill for simple relationships between few objects

## Related Patterns

### Mediator vs Observer

**Observer** distributes events from one subject to many observers (push). **Mediator** centralizes coordination between
peers (hub). Observer is a broadcast pattern; Mediator is a routing pattern. Observer has a 1:N shape; Mediator has an
N:1:N shape.

### Mediator vs Facade

**Facade** provides a simplified unidirectional interface to a subsystem. **Mediator** provides bidirectional
coordination between subsystem components. A Facade is for external clients; a Mediator is for internal components. They
can work together: a Facade's implementation may use a Mediator.

### Mediator vs Chain of Responsibility

**CoR** passes requests along a chain until one handler processes it. **Mediator** routes requests from any component to
any other. CoR is linear and focused on finding the right handler. Mediator is a star topology for broadcast or targeted
routing.

## Key Takeaways

1. **"Centralize the chaos"** — When N components communicate with each other in a web, Mediator reduces N*(N-1)/2
   connections to N connections.

2. **Avoid the god object** — Keep mediators focused on coordination. Split into domain-specific mediators when logic
   grows.

3. **Testing is easier** — Components test with mock mediators; mediators test with mock components. This is a
   significant advantage in complex UIs.

4. **Not just UI** — Mediator applies to microservices, chat systems, game lobbies, and anything with complex
   inter-component communication.

5. **Interview memory aid** — "Mediator = communication hub, N-to-1-to-N, loose coupling, avoid god object."
