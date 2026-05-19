# Distributed Queue (RabbitMQ-like)

> Message queue with topics, subscribers, delivery guarantees, and persistence.

## Requirements

- Publish messages to topics/exchanges
- Subscribe to topics with filters
- Message delivery guarantees (at-least-once, at-most-once, exactly-once)
- Message persistence
- Dead letter queue for failed messages
- Consumer acknowledgment

## Domain Model

```
MessageQueue
  ├── Exchange
  │     ├── name: String
  │     ├── type: ExchangeType (DIRECT, FANOUT, TOPIC)
  │     └── bindings: List<Binding>
  ├── Queue
  │     ├── name: String
  │     ├── messages: BlockingQueue<Message>
  │     ├── consumers: List<Consumer>
  │     └── deadLetterQueue: Queue
  ├── Message
  │     ├── id: String
  │     ├── body: byte[]
  │     ├── headers: Map<String, String>
  │     └── routingKey: String
  └── Consumer
        ├── id: String
        ├── callback: MessageHandler
        └── autoAck: boolean
```

## Key Patterns

### Observer Pattern (Pub-Sub)
```java
class TopicExchange {
    private final Map<String, List<Queue>> bindings = new ConcurrentHashMap<>();

    void bind(String routingKey, Queue queue) {
        bindings.computeIfAbsent(routingKey, k -> new CopyOnWriteArrayList<>())
                .add(queue);
    }

    void publish(Message message) {
        List<Queue> queues = bindings.getOrDefault(
            message.getRoutingKey(), Collections.emptyList());
        for (Queue queue : queues) {
            queue.enqueue(message);
        }
    }
}
```

### Iterator Pattern (Message Consumption)
```java
class QueueIterator implements Iterator<Message> {
    private final BlockingQueue<Message> queue;

    public boolean hasNext() { return !queue.isEmpty(); }
    public Message next() { return queue.poll(); }
}
```

## Core Implementation

```java
class MessageQueue {
    private final Map<String, Queue> queues = new ConcurrentHashMap<>();
    private final Map<String, Exchange> exchanges = new ConcurrentHashMap<>();

    Queue createQueue(String name, boolean durable) {
        return queues.computeIfAbsent(name, n -> new Queue(n, durable));
    }

    void publish(String exchangeName, Message message) {
        Exchange exchange = exchanges.get(exchangeName);
        if (exchange == null) throw new ExchangeNotFoundException();
        exchange.publish(message);
    }

    void consume(String queueName, Consumer consumer) {
        Queue queue = queues.get(queueName);
        if (queue == null) throw new QueueNotFoundException();
        queue.addConsumer(consumer);
        consumer.start(queue);
    }
}

class Queue {
    private final BlockingQueue<Message> messages = new LinkedBlockingQueue<>();
    private final List<Consumer> consumers = new CopyOnWriteArrayList<>();
    private final Queue deadLetterQueue;

    void enqueue(Message message) { messages.offer(message); }

    Message poll() { return messages.poll(); }

    void addConsumer(Consumer consumer) { consumers.add(consumer); }

    void nack(Message message) {
        if (message.getRetryCount() < 3) {
            message.incrementRetry();
            enqueue(message);  // Retry
        } else if (deadLetterQueue != null) {
            deadLetterQueue.enqueue(message);  // Dead letter
        }
    }
}
```

## Interview Tips

1. **Delivery guarantees**: Discuss at-least-once (ack after processing) vs at-most-once (ack before)
2. **Dead letter queue**: Messages that fail N times go to DLQ for manual inspection
3. **Backpressure**: What happens when consumers are slower than producers?
4. **Ordering**: Per-partition ordering vs global ordering trade-offs
5. **Persistence**: Write to disk before acknowledging publish
