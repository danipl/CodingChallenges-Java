# Logger Framework

> Chain of Responsibility for multi-destination logging with levels, formatting, and filtering.

## Requirements

- Multiple log levels: DEBUG, INFO, WARN, ERROR, FATAL
- Multiple destinations: Console, File, Database, Remote
- Log formatting (JSON, plain text, custom)
- Asynchronous logging
- Log rotation (file size/time based)

## Key Pattern: Chain of Responsibility

```java
// 1. Log entry
class LogEntry {
    final LogLevel level;
    final String message;
    final String loggerName;
    final Instant timestamp;
    final Throwable exception;
}

// 2. Handler interface
abstract class LogHandler {
    protected LogHandler next;
    protected LogLevel minLevel;

    void setNext(LogHandler next) { this.next = next; }
    void setMinLevel(LogLevel level) { this.minLevel = level; }

    void handle(LogEntry entry) {
        if (entry.getLevel().ordinal() >= minLevel.ordinal()) {
            write(entry);
        }
        if (next != null) {
            next.handle(entry);
        }
    }

    abstract void write(LogEntry entry);
}

// 3. Concrete handlers
class ConsoleHandler extends LogHandler {
    void write(LogEntry entry) {
        System.out.println(format(entry));
    }
    private String format(LogEntry e) {
        return String.format("[%s] [%s] [%s] %s",
            e.getTimestamp(), e.getLevel(), e.getLoggerName(), e.getMessage());
    }
}

class FileHandler extends LogHandler {
    private final String filePath;
    FileHandler(String path) { this.filePath = path; }
    void write(LogEntry entry) {
        try (FileWriter fw = new FileWriter(filePath, true)) {
            fw.write(format(entry) + "\n");
        }
    }
}

class AsyncHandler extends LogHandler {
    private final LogHandler wrapped;
    private final BlockingQueue<LogEntry> queue = new LinkedBlockingQueue<>(10000);

    AsyncHandler(LogHandler wrapped) {
        this.wrapped = wrapped;
        startWorker();
    }

    void write(LogEntry entry) {
        queue.offer(entry);  // Non-blocking
    }

    private void startWorker() {
        new Thread(() -> {
            while (true) {
                try {
                    LogEntry entry = queue.take();
                    wrapped.handle(entry);
                } catch (InterruptedException e) { break; }
            }
        }).start();
    }
}

// 4. Build pipeline
LogHandler pipeline = new ConsoleHandler();
pipeline.setMinLevel(LogLevel.DEBUG);

FileHandler fileHandler = new FileHandler("app.log");
fileHandler.setMinLevel(LogLevel.INFO);
pipeline.setNext(fileHandler);

// Wrap with async
LogHandler asyncPipeline = new AsyncHandler(pipeline);

// 5. Use
asyncPipeline.handle(new LogEntry(LogLevel.ERROR, "DB connection failed", "UserService", Instant.now(), null));
```

## Interview Tips

1. **Chain order matters**: Console → File → Database (cheap to expensive)
2. **Async logging**: Queue + background worker prevents blocking the main thread
3. **Log rotation**: Check file size before writing, rotate when threshold exceeded
4. **Filtering**: Each handler has its own minLevel
5. **Performance**: String interpolation vs lazy evaluation — `() -> expensiveMessage()`
