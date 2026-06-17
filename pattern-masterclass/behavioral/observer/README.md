# Observer Pattern

## Overview

**Definition**: Observer defines a one-to-many dependency between objects so that when one object changes state, all its
dependents are notified and updated automatically. It's the foundation of event-driven programming.

**Core Problem**: How to notify multiple interested parties about state changes without coupling the notifier to the
specific notification logic of each subscriber.

**One-Line Interview Answer**: "Observer creates a subscription mechanism where a subject notifies multiple observers of
state changes without knowing their concrete types, enabling loose coupling and event-driven architecture."

## Problem Statement

### Real-World Scenario: Stock Price Ticker

A trading platform displays stock prices on multiple screens: a live dashboard, a mobile app push notification system, a
trading bot that executes auto-sells, and a compliance logging service. When a stock price changes, all these systems
must react.

Naive approach: the stock ticker directly calls each system:

```java
public class StockTicker {
    private LiveDashboard dashboard;
    private PushNotificationService pushService;
    private TradingBot tradingBot;
    private ComplianceLogger logger;

    public void onPriceChange(String symbol, double newPrice) {
        dashboard.update(symbol, newPrice);
        pushService.sendAlert(symbol, newPrice);
        tradingBot.evaluate(symbol, newPrice);
        logger.log(symbol, newPrice);
        // Add new subscriber → modify this method
        // Remove subscriber → modify this method
        // Each subscriber has a different method signature
    }
}
```

### Pain Points of the Naive Approach

1. **Tight Coupling**: The `StockTicker` knows every concrete subscriber. Adding or removing subscribers requires code
   changes to the ticker.
2. **Different Interfaces**: Each subscriber has a different method signature (`update`, `sendAlert`, `evaluate`,
   `log`). The ticker must adapt to each.
3. **Sequential Blocking**: If `PushNotificationService.sendAlert()` blocks for 2 seconds, all subsequent updates stall.
4. **No Error Isolation**: A crash in `TradingBot.evaluate()` prevents compliance logging.
5. **No Subscription Control**: The ticker cannot dynamically add/remove subscribers based on market hours or user
   preferences.

### Why This Matters in Production

Event-driven systems are everywhere: UI frameworks (Swing, JavaFX), message brokers (Kafka, RabbitMQ), Spring's
`ApplicationEventPublisher`, and reactive streams. Without Observer, adding a new subscriber means hunting down every
notification point and adding a new call, which is fragile and violates Open/Closed Principle.

## Solution

### How Observer Solves This

Observer introduces a common `Observer` interface and lets the `Subject` maintain a list of subscribers. The Subject
knows only the interface, not the concrete types. New subscribers register via `attach()` — no code changes needed.

### Key Participants

| Participant            | Role                                                                 |
|------------------------|----------------------------------------------------------------------|
| `Subject` (Observable) | Maintains observer list; provides `attach()`, `detach()`, `notify()` |
| `Observer` (interface) | Declares the `update()` method that subjects call                    |
| `ConcreteObserver`     | Implements `update()` to respond to state changes                    |
| `ConcreteSubject`      | Stores state of interest; triggers notification on state change      |

### Step-by-Step Flow (Push Model)

1. ConcreteObserver registers with ConcreteSubject via `subject.attach(this)`
2. Subject's state changes (e.g., `setPrice()`)
3. Subject calls `notifyObservers()` which iterates all observers
4. Subject pushes data to each observer via `observer.update(data)`
5. Observer reacts (update UI, send notification, log)

### Push vs Pull Models

- **Push**: Subject sends full state data to observers. Simple but may send more data than needed.
- **Pull**: Subject sends minimal notification (or just itself). Observers call back to get what they need. More
  flexible but requires observers to know subject's API.

## Java Implementation

### Push Model (Recommended for Most Cases)

```java
package behavioral.observer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

// Observer interface
@FunctionalInterface
interface PriceObserver {
    void onPriceChange(String symbol, BigDecimal oldPrice, BigDecimal newPrice);
}

// Subject (Observable)
class StockTicker {
    private final List<PriceObserver> observers = new ArrayList<>();
    private String symbol;
    private BigDecimal price;

    public void attach(PriceObserver observer) {
        observers.add(observer);
    }

    public void detach(PriceObserver observer) {
        observers.remove(observer);
    }

    public void setPrice(String symbol, BigDecimal newPrice) {
        BigDecimal oldPrice = this.price;
        this.symbol = symbol;
        this.price = newPrice;
        notifyObservers(symbol, oldPrice, newPrice);
    }

    private void notifyObservers(String symbol, BigDecimal oldPrice, BigDecimal newPrice) {
        // Defensive copy to avoid ConcurrentModificationException
        List<PriceObserver> copy;
        synchronized (observers) {
            copy = new ArrayList<>(observers);
        }
        copy.forEach(obs -> obs.onPriceChange(symbol, oldPrice, newPrice));
    }

    public BigDecimal getPrice() { return price; }
    public String getSymbol() { return symbol; }
}
```

### Concrete Observers

```java
class LiveDashboard implements PriceObserver {
    @Override
    public void onPriceChange(String symbol, BigDecimal oldPrice, BigDecimal newPrice) {
        System.out.printf("[DASHBOARD] %s: $%.2f → $%.2f (%.2f%%)%n",
            symbol, oldPrice, newPrice,
            newPrice.subtract(oldPrice)
                .divide(oldPrice, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100)));
    }
}

class TradingBot implements PriceObserver {
    private static final BigDecimal SELL_THRESHOLD = BigDecimal.valueOf(-0.05);
    private static final BigDecimal BUY_THRESHOLD = BigDecimal.valueOf(0.03);

    @Override
    public void onPriceChange(String symbol, BigDecimal oldPrice, BigDecimal newPrice) {
        BigDecimal change = newPrice.subtract(oldPrice).divide(oldPrice, 4, BigDecimal.ROUND_HALF_UP);

        if (change.compareTo(SELL_THRESHOLD) < 0) {
            System.out.printf("[BOT] SELL %s — dropped %.2f%%%n", symbol, change.multiply(BigDecimal.valueOf(100)));
        } else if (change.compareTo(BUY_THRESHOLD) > 0) {
            System.out.printf("[BOT] BUY %s — gained %.2f%%%n", symbol, change.multiply(BigDecimal.valueOf(100)));
        }
    }
}

class ComplianceLogger implements PriceObserver {
    @Override
    public void onPriceChange(String symbol, BigDecimal oldPrice, BigDecimal newPrice) {
        System.out.printf("[AUDIT] %s price change logged at %d%n", symbol, System.currentTimeMillis());
    }
}
```

### Pull Model Implementation

```java
package behavioral.observer.pull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

// Observer receives only the subject — pulls what it needs
interface PullObserver {
    void update(StockTicker subject);
}

class StockTicker {
    private final List<PullObserver> observers = new ArrayList<>();
    private String symbol;
    private BigDecimal price;
    private long lastUpdate;

    public void attach(PullObserver observer) {
        observers.add(observer);
    }

    public void setPrice(String symbol, BigDecimal price) {
        this.symbol = symbol;
        this.price = price;
        this.lastUpdate = System.currentTimeMillis();
        observers.forEach(obs -> obs.update(this));
    }

    // Observers pull via these getters
    public String getSymbol() { return symbol; }
    public BigDecimal getPrice() { return price; }
    public long getLastUpdate() { return lastUpdate; }
}

class LazyDashboard implements PullObserver {
    @Override
    public void update(StockTicker subject) {
        // Only update if price changed significantly
        System.out.printf("[DASHBOARD-PULL] %s: $%.2f%n",
            subject.getSymbol(), subject.getPrice());
    }
}
```

### Thread-Safe Observer with Event Bus

```java
package behavioral.observer;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Thread-safe subject with async notifications
class AsyncStockTicker {
    private final CopyOnWriteArrayList<PriceObserver> observers = new CopyOnWriteArrayList<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public void attach(PriceObserver observer) {
        observers.add(observer);
    }

    public void setPrice(String symbol, BigDecimal oldPrice, BigDecimal newPrice) {
        observers.forEach(obs ->
            executor.submit(() -> obs.onPriceChange(symbol, oldPrice, newPrice))
        );
    }

    public void shutdown() {
        executor.shutdown();
    }
}
```

### Usage Demo

```java
public class ObserverDemo {
    public static void main(String[] args) {
        var ticker = new StockTicker();

        var dashboard = new LiveDashboard();
        var bot = new TradingBot();
        var logger = new ComplianceLogger();

        // Attach observers
        ticker.attach(dashboard);
        ticker.attach(bot);
        ticker.attach(logger);

        // State changes trigger notifications
        ticker.setPrice("AAPL", BigDecimal.valueOf(150.00), BigDecimal.valueOf(155.00));
        ticker.setPrice("GOOGL", BigDecimal.valueOf(2800.00), BigDecimal.valueOf(2650.00));

        // Detach observer at runtime
        ticker.detach(bot);
        ticker.setPrice("TSLA", BigDecimal.valueOf(700.00), BigDecimal.valueOf(750.00));
    }
}
```

### Java's Deprecated `java.util.Observable`

```java
// DO NOT USE — Observable is deprecated since Java 9
// Problems: Observable is a class (not interface) — forces subclassing
// violates composition over inheritance. Also uses Vector internally.
```

### Spring's ApplicationEventPublisher (Modern Alternative)

```java
// Spring equivalent — not compilable standalone, shown for reference
// @Component
// class StockPublisher {
//     @Autowired private ApplicationEventPublisher publisher;
//
//     public void publishPriceChange(String symbol, double price) {
//         publisher.publishEvent(new PriceChangeEvent(this, symbol, price));
//     }
// }
//
// @Component
// class PriceChangeListener {
//     @EventListener
//     public void handlePriceChange(PriceChangeEvent event) {
//         System.out.println("Received: " + event.getSymbol());
//     }
// }
```

## When to Use

1. **UI Event Handling**: Java Swing/AWT listeners (`ActionListener`, `MouseListener`). Every UI component is a Subject;
   listeners are Observers. This is the canonical example.

2. **Distributed Event-Driven Systems**: Apache Kafka producers (subjects) and consumers (observers). The pub/sub model
   is Observer at scale, with a message broker decoupling producers and consumers across network boundaries.

3. **Reactive Programming**: Project Reactor's `Flux<T>` and RxJava's `Observable<T>` implement the Observer pattern
   with backpressure support. Data streams notify subscribers as data arrives.

4. **Spring Lifecycle Events**: Spring publishes `ContextRefreshedEvent`, `ContextClosedEvent`, etc. Listeners annotated
   with `@EventListener` react without coupling to Spring internals.

5. **Cross-Cutting Concerns**: Logging, auditing, metrics collection. Instead of baking logging into every method, emit
   events and have a logging observer subscribe.

### Framework Examples

- **Java Beans PropertyChangeSupport**: Java's built-in support for bound properties. Beans notify registered
  `PropertyChangeListener`s.
- **Spring ApplicationEventPublisher**: Observer via the Spring context, supports both synchronous and asynchronous
  listeners.
- **Jakarta Servlet `HttpSessionBindingListener`**: Objects placed in a session can observe when they're bound/unbound.
- **RxJava/Reactive Streams**: `Observable.subscribe(observer)` — Observer is the foundation of reactive programming.

## When NOT to Use

1. **Too Many Observers**: If a subject has thousands of observers, notification becomes expensive. Consider a message
   queue with batch consumption or use the Event Bus pattern with filtering.

2. **Circular Notifications**: Observer A observes Subject B, which observes Subject A. This creates infinite update
   loops. Always design acyclic notification graphs or use a dirty-flag check.

3. **Observers Not Thread-Safe**: The subject notifies observers on its thread. If observers block or assume
   single-threaded access, you get hangs or data races. Use `CopyOnWriteArrayList` or async dispatch.

4. **Memory Leaks from Forgotten Detach**: Observers that register but never detach prevent GC of both the observer and
   the subject. Always detach in `close()` or `dispose()` methods.

5. **Simple One-to-One Notification**: If you have only one subscriber, Observer is overkill. Direct method call or a
   callback interface is simpler.

## Interview Questions

### Q1: Explain the Observer pattern and its two variants (push vs pull).

**Answer**: Observer defines a one-to-many dependency where a Subject notifies Observers of state changes. In **push**,
the Subject sends detailed state data to observers. In **pull**, the Subject sends only a notification (or itself), and
observers query for what they need. Pull is more flexible and avoids sending unnecessary data; push is simpler and
avoids observers knowing the Subject's API.

### Q2: Why was `java.util.Observable` deprecated in Java 9?

**Answer**: `Observable` is a class, forcing subclassing and violating "favor composition over inheritance." Its
`Vector`-based observer list introduces unnecessary synchronization overhead. The event model is limited — `update()`
passes `Object` data with no type safety. The modern replacement is `PropertyChangeSupport` or a custom observer
interface.

### Q3: How does Observer relate to the Open/Closed Principle?

**Answer**: The Subject is open for extension (new observers can be added) but closed for modification (adding an
observer never changes the Subject's code). This is OCP in action — you extend behavior by plugging in new observers,
not by modifying the Subject.

### Q4: What's the difference between Observer and Pub/Sub?

**Answer**: Observer is a design pattern where the Subject directly notifies observers (in-process, tight lifecycle).
Pub/Sub is an architectural pattern where publishers and subscribers communicate through a message broker (
cross-process, decoupled lifecycles). Pub/Sub is Observer at scale, adding durability, routing, and fan-out.

### Q5: How would you make the Observer pattern thread-safe?

**Answer**: Use `CopyOnWriteArrayList` for the observer list (safe iteration without locking for reads). Detach during
iteration can be handled with a concurrent collection or by using a snapshot pattern. For async notifications, dispatch
to an `ExecutorService` so observers don't block the subject's thread.

### Q6: What memory management issues arise with Observer and how do you solve them?

**Answer**: The Subject holds strong references to observers, preventing GC if the observer is no longer needed. Use
`WeakReference` for observers (Java's `WeakHashMap`) or ensure explicit `detach()` in lifecycle methods. For UI
listeners, Java Swing's listeners can cause memory leaks if components aren't properly disposed.

### Q7: How would you implement a typed Observer pattern using generics?

**Answer**: Define `interface Observer<T> { void onChange(T event); }` and
`class Subject<T> { void attach(Observer<T> obs); void notify(T event); }`. This gives compile-time type safety.
Concrete implementations: `Subject<PriceEvent>`, `Observer<UserLoginEvent>`.

### Q8: Can Observer be implemented using Java's `Flow` API?

**Answer**: Yes. Java 9's `java.util.concurrent.Flow` defines `Publisher`, `Subscriber`, `Subscription`, and `Processor`
interfaces following reactive streams. `SubmissionPublisher<T>` is a built-in implementation. This is the modern,
backpressure-aware evolution of Observer in the JDK.

### Follow-Up Question

**Interviewer**: "What happens if an observer throws an exception during notification? How should the system handle it?"

**Answer**: An exception in one observer should NOT prevent other observers from receiving notifications. Wrap each
observer call in a try-catch, log the error, and continue. Never let an observer exception propagate out of the
notification loop. For critical observers, implement a retry mechanism or dead-letter queue.

## Pros & Cons

### Advantages

- **Loose Coupling**: Subject knows only the Observer interface, not concrete implementations
- **Dynamic Relationships**: Observers can be added or removed at runtime
- **Broadcast Communication**: One state change triggers multiple independent reactions
- **Open/Closed Principle**: New observers don't require Subject changes
- **Event-Driven Architecture**: Foundation for reactive and event-driven systems

### Disadvantages

- **Unexpected Updates**: Observers may receive updates they didn't expect or can't handle
- **Performance Cost**: Notifying many observers is O(N); N can be large
- **Memory Leaks**: Forgetting to detach prevents garbage collection
- **No Order Guarantee**: Observers are notified in arbitrary order unless explicitly managed
- **Update Cascades**: A → B → A update loops can cause stack overflow or infinite loops

## Related Patterns

### Observer vs Mediator

**Mediator** centralizes communication between multiple objects. **Observer** distributes notifications from one subject
to many observers. Mediator reduces chaos by routing all communication through a single hub. Observer enables decoupled
broadcast. In a chat room: the chat server is a Mediator; users are observers of each other's messages.

### Observer vs Event Bus

An **Event Bus** is a global Observer pattern on steroids. Instead of one Subject, any object can publish events, and
any object can subscribe. Guava's `EventBus` and Spring's `ApplicationEventPublisher` are Event Buses. Use a simple
Observer for one-to-many within a bounded context; use an Event Bus for cross-module or cross-service communication.

### Observer vs Chain of Responsibility

**Chain of Responsibility** passes a request along a chain until one handler processes it. **Observer** broadcasts to
all subscribers, and each one processes independently. CoR is "who handles this?" (exclusive). Observer is "who cares
about this?" (inclusive).

## Key Takeaways

1. **"Subject doesn't care who's listening"** — The Subject knows only the interface. Loose coupling is the core
   benefit.

2. **Push vs Pull matters** — Push is simpler; pull is more flexible. Choose based on whether observers need all the
   data or just a notification.

3. **OCP demonstration** — Add new behavior without changing existing code. The Subject never changes.

4. **Watch for leaks** — Always detach observers. In managed environments (Spring, Android), lifecycle callbacks handle
   this automatically.

5. **Interview memory aid** — "Observer = one-to-many notification, push or pull, OC principle, detach or leak."
