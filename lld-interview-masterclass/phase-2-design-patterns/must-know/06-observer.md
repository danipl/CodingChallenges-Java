# Observer Pattern

> Pub-sub, event-driven systems, reactive programming foundation.

## Why?

When one object changes state, multiple dependents need to be notified — without tight coupling.

## Where?

- **Frontend**: React state management, Vue reactivity, DOM events
- **Backend**: Event buses, message queues, Kafka consumers
- **Java**: `java.util.Observable` (deprecated), `PropertyChangeListener`
- **RxJava/Project Reactor**: Reactive streams are Observer pattern evolved

## How

```java
// 1. Observer interface
interface Observer {
    void update(Event event);
}

// 2. Subject (Observable)
class EventBus {
    private final List<Observer> observers = new CopyOnWriteArrayList<>();

    void subscribe(Observer observer) { observers.add(observer); }
    void unsubscribe(Observer observer) { observers.remove(observer); }

    void publish(Event event) {
        for (Observer observer : observers) {
            observer.update(event);
        }
    }
}

// 3. Concrete observers
class EmailNotifier implements Observer {
    public void update(Event event) {
        if (event.getType() == Event.Type.ORDER_PLACED) {
            sendOrderConfirmationEmail(event);
        }
    }
}

class InventoryManager implements Observer {
    public void update(Event event) {
        if (event.getType() == Event.Type.ORDER_PLACED) {
            reserveInventory(event);
        }
    }
}

// 4. Usage
EventBus bus = new EventBus();
bus.subscribe(new EmailNotifier());
bus.subscribe(new InventoryManager());
bus.subscribe(new AnalyticsTracker());

// Publish — all observers notified
bus.publish(new Event(Event.Type.ORDER_PLACED, orderData));
```

## Thread-Safe Observer (Interview Bonus)

```java
class ThreadSafeEventBus {
    private final List<Observer> observers = new CopyOnWriteArrayList<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    void subscribe(Observer o) { observers.add(o); }

    void publish(Event event) {
        for (Observer observer : observers) {
            executor.submit(() -> observer.update(event));  // Async notification
        }
    }
}
```

## Interview Application

- **Pub-Sub system**: Core pattern for message distribution
- **Stock ticker**: Multiple listeners get price updates
- **File watcher**: Multiple actions on file change
- **UI state**: Multiple components react to state changes

## Push vs Pull Model

| Model | Description | When |
|-------|-------------|------|
| Push | Subject sends data with notification | Observers need same data |
| Pull | Observer fetches data from subject | Observers need different data |
