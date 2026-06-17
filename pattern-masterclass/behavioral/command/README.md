# Command Pattern

## Overview

**Definition**: Command encapsulates a request as an object, thereby letting you parameterize clients with different
requests, queue or log requests, and support undoable operations.

**Core Problem**: How to decouple the object that invokes an operation from the object that knows how to perform it,
while also enabling delayed execution, queuing, and undo/redo.

**One-Line Interview Answer**: "Command turns a request into a standalone object that contains everything needed to
execute it, enabling delayed execution, queuing, logging, and transactional undo/redo."

## Problem Statement

### Real-World Scenario: Text Editor

Building a text editor with actions like cut, copy, paste, undo, redo, and macro recording. A naive approach ties UI
buttons directly to editor actions:

```java
public class TextEditor {
    private String text = "";
    private int cursorPosition;

    public void onCutButtonClick() {
        // 1. Extract selected text to clipboard
        // 2. Remove from document
        // 3. Update display
        // 4. Add to undo history — but undo logic is mixed in
        // 5. Also need to handle shortcuts (Ctrl+X)
        clearSelection();
        copyToClipboard();
        deleteSelection();
        addToUndoHistory("cut");
    }

    public void onPasteButtonClick() {
        // 1. Get text from clipboard
        // 2. Insert at cursor position
        // 3. Update display
        addToUndoHistory("paste");
    }
    // undo() must reverse every possible action
    // Each new action requires modifying undo logic
    // Cannot queue actions (macros)
    // Cannot log actions without adding code everywhere
}
```

### Pain Points of the Naive Approach

1. **Tight Coupling**: UI components (buttons, menu items, keyboard shortcuts) directly call editor methods. Changing
   the action means changing every trigger point.
2. **Undo Complexity**: Undo requires knowing what the last action was AND storing enough state to reverse it. A simple
   action name string doesn't capture the state needed for reversal.
3. **No Queuing**: Cannot build macros or scripted sequences because actions execute immediately.
4. **No Logging/Audit**: No clean point to intercept actions for logging, analytics, or transaction management.
5. **Duplicated Trigger Logic**: Ctrl+C, Edit→Copy menu, and right-click Copy each reimplement the same copy logic.

### Why This Matters in Production

Command pattern is fundamental to any system that needs undo/redo, transactional behavior, or task scheduling. Database
transaction logs are Command-like. Job schedulers (Quartz, Spring Task) queue command objects. UI frameworks (Swing
`Action`, JavaFX `EventHandler`) use Command. Without it, undo becomes a nightmare of ad-hoc state snapshots.

## Solution

### How Command Solves This

Command turns each action into an object that implements a common interface with `execute()` and `undo()` methods. The
invoker (button, menu, shortcut) only knows the Command interface. A history stack stores executed commands for
undo/redo.

### Key Participants

| Participant           | Role                                                            |
|-----------------------|-----------------------------------------------------------------|
| `Command` (interface) | Declares `execute()` and optionally `undo()`                    |
| `ConcreteCommand`     | Encapsulates an action and its receiver; implements `execute()` |
| `Receiver`            | The object that performs the actual work                        |
| `Invoker`             | Asks the command to carry out the request                       |
| `Client`              | Creates ConcreteCommand and sets its receiver                   |

### Step-by-Step Flow

1. Client creates a ConcreteCommand with a reference to the Receiver
2. Client sets the command in the Invoker (e.g., a button)
3. When triggered, Invoker calls `command.execute()`
4. ConcreteCommand calls `receiver.action()` (possibly with stored parameters)
5. For undo: Invoker pushes the command onto a history stack; calls `command.undo()` when needed

### UML-Style Structure

```
┌────────────┐       ┌──────────────────┐
│  Invoker   │───────│   «interface»    │
│            │       │    Command       │
│ +execute() │       │                  │
└────────────┘       │ +execute()       │
                     │ +undo()          │
                     └────────┬─────────┘
                              │
                     ┌────────┴────────┐
                     │ ConcreteCommand │
                     │                 │
                     │ -receiver       │───→┌──────────┐
                     │ -state         │    │ Receiver │
                     │ +execute()     │    │          │
                     │ +undo()        │    │+action() │
                     └────────────────┘    └──────────┘
```

## Java Implementation

### Command Interface

```java
package behavioral.command;

@FunctionalInterface
interface Command {
    void execute();

    default void undo() {
        throw new UnsupportedOperationException("Undo not supported for this command");
    }
}
```

### Receiver: Text Editor

```java
class TextEditorReceiver {
    private final StringBuilder content = new StringBuilder();
    private String clipboard = "";

    public void insert(String text, int position) {
        content.insert(position, text);
        System.out.println("Inserted '" + text + "' at position " + position);
    }

    public void delete(int start, int end) {
        content.delete(start, end);
        System.out.println("Deleted characters from " + start + " to " + end);
    }

    public void copy(int start, int end) {
        clipboard = content.substring(start, end);
        System.out.println("Copied '" + clipboard + "'");
    }

    public void setClipboard(String text) {
        this.clipboard = text;
    }

    public String getClipboard() { return clipboard; }
    public String getContent() { return content.toString(); }
    public int length() { return content.length(); }

    public void display() {
        System.out.println("Current content: [" + content + "]");
    }
}
```

### Concrete Commands

```java
class InsertCommand implements Command {
    private final TextEditorReceiver receiver;
    private final String text;
    private final int position;

    public InsertCommand(TextEditorReceiver receiver, String text, int position) {
        this.receiver = receiver;
        this.text = text;
        this.position = position;
    }

    @Override
    public void execute() {
        receiver.insert(text, position);
    }

    @Override
    public void undo() {
        receiver.delete(position, position + text.length());
    }
}

class DeleteCommand implements Command {
    private final TextEditorReceiver receiver;
    private final int start;
    private final int end;
    private String deletedText; // Stored for undo

    public DeleteCommand(TextEditorReceiver receiver, int start, int end) {
        this.receiver = receiver;
        this.start = start;
        this.end = end;
    }

    @Override
    public void execute() {
        deletedText = receiver.getContent().substring(start, end);
        receiver.delete(start, end);
    }

    @Override
    public void undo() {
        receiver.insert(deletedText, start);
    }
}

class CopyCommand implements Command {
    private final TextEditorReceiver receiver;
    private final int start;
    private final int end;

    public CopyCommand(TextEditorReceiver receiver, int start, int end) {
        this.receiver = receiver;
        this.start = start;
        this.end = end;
    }

    @Override
    public void execute() {
        receiver.copy(start, end);
    }
    // No undo needed for copy — it's non-destructive
}

class PasteCommand implements Command {
    private final TextEditorReceiver receiver;
    private final int position;

    public PasteCommand(TextEditorReceiver receiver, int position) {
        this.receiver = receiver;
        this.position = position;
    }

    @Override
    public void execute() {
        receiver.insert(receiver.getClipboard(), position);
    }

    @Override
    public void undo() {
        int clipboardLen = receiver.getClipboard().length();
        receiver.delete(position, position + clipboardLen);
    }
}
```

### Invoker with Undo/Redo

```java
import java.util.ArrayDeque;
import java.util.Deque;

class EditorInvoker {
    private final Deque<Command> undoStack = new ArrayDeque<>();
    private final Deque<Command> redoStack = new ArrayDeque<>();
    private static final int MAX_HISTORY = 100;

    public void executeCommand(Command command) {
        command.execute();
        if (undoStack.size() >= MAX_HISTORY) {
            undoStack.pollLast(); // Discard oldest
        }
        undoStack.push(command);
        redoStack.clear(); // New action invalidates redo history
    }

    public void undo() {
        if (undoStack.isEmpty()) {
            System.out.println("Nothing to undo");
            return;
        }
        Command command = undoStack.pop();
        try {
            command.undo();
            redoStack.push(command);
            System.out.println("Undo successful");
        } catch (UnsupportedOperationException e) {
            System.out.println("Cannot undo: " + e.getMessage());
            undoStack.push(command); // Put it back
        }
    }

    public void redo() {
        if (redoStack.isEmpty()) {
            System.out.println("Nothing to redo");
            return;
        }
        Command command = redoStack.pop();
        command.execute();
        undoStack.push(command);
        System.out.println("Redo successful");
    }
}
```

### Macro Command (Composite Pattern)

```java
import java.util.ArrayList;
import java.util.List;

class MacroCommand implements Command {
    private final List<Command> commands = new ArrayList<>();

    public void addCommand(Command command) {
        commands.add(command);
    }

    @Override
    public void execute() {
        commands.forEach(Command::execute);
    }

    @Override
    public void undo() {
        // Undo in reverse order
        for (int i = commands.size() - 1; i >= 0; i--) {
            commands.get(i).undo();
        }
    }
}
```

### Lambda-Based Commands (Functional Approach)

```java
import java.util.function.Supplier;

public class CommandDemo {
    public static void main(String[] args) {
        var editor = new TextEditorReceiver();
        var invoker = new EditorInvoker();

        // Insert some text
        invoker.executeCommand(new InsertCommand(editor, "Hello World", 0));
        editor.display();

        // Lambda-based command (simple cases)
        Command printCommand = () -> System.out.println("--- Editor State ---");
        invoker.executeCommand(printCommand);

        // Delete "World"
        invoker.executeCommand(new DeleteCommand(editor, 6, 11));
        editor.display();

        // Undo delete
        invoker.undo();
        editor.display();

        // Undo insert
        invoker.undo();
        editor.display();

        // Redo
        invoker.redo();
        editor.display();

        // Macro: bold a word (insert bold markers)
        var macro = new MacroCommand();
        macro.addCommand(new InsertCommand(editor, "**", 0));
        macro.addCommand(new InsertCommand(editor, "**", 7));
        System.out.println("Executing macro (bold):");
        invoker.executeCommand(macro);
        editor.display();

        // Undo the whole macro
        System.out.println("Undoing macro:");
        invoker.undo();
        editor.display();
    }
}
```

### Command with Transaction Logging

```java
class LoggedCommand implements Command {
    private final Command delegate;
    private final long timestamp;

    public LoggedCommand(Command delegate) {
        this.delegate = delegate;
        this.timestamp = System.currentTimeMillis();
    }

    @Override
    public void execute() {
        System.out.printf("[LOG %d] Executing: %s%n", timestamp, delegate.getClass().getSimpleName());
        delegate.execute();
    }

    @Override
    public void undo() {
        System.out.printf("[LOG %d] Undoing: %s%n", timestamp, delegate.getClass().getSimpleName());
        delegate.undo();
    }
}
```

### Async Command Execution

```java
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class AsyncInvoker {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public void submit(Command command) {
        executor.submit(() -> {
            System.out.println("Async executing: " + command.getClass().getSimpleName());
            command.execute();
        });
    }

    public void shutdown() {
        executor.shutdown();
    }
}
```

## When to Use

1. **Undo/Redo Systems**: Text editors, image editors, IDEs. Every user action becomes a Command pushed onto the undo
   stack. Reversal is `command.undo()`.

2. **Transactional Operations**: Database operations where each statement is a Command. Commit executes all; rollback
   undoes them in reverse. This is the Unit of Work pattern combined with Command.

3. **Task Scheduling & Queuing**: Job schedulers (Quartz, Spring `@Scheduled`) store commands with execution times.
   Commands can be serialized to disk and executed later.

4. **GUI Toolbars & Menus**: Swing `Action` interface is a Command. One action object can be attached to a button, a
   menu item, and a keyboard shortcut simultaneously.

5. **Macro Recording**: User records a sequence of commands, which are stored as a MacroCommand. Playback calls
   execute() on each. This is how Photoshop Actions and Vim macros work.

### Framework Examples

- **Swing `javax.swing.Action`**: Implements Command pattern. An Action can be attached to JButton, JMenuItem, and
  KeyStroke.
- **Spring `@Scheduled` + `TaskScheduler`**: Scheduled tasks are commands queued for delayed execution.
- **Java `Runnable`/`Callable`**: These are Command pattern instances — encapsulating work to be executed on another
  thread.
- **Quartz Scheduler**: `Job` interface is a Command. Jobs are scheduled, executed, and can be persisted.

## When NOT to Use

1. **Simple Direct Invocation**: If you just need `receiver.doIt()` and there's no undo, queuing, or logging, Command
   adds unnecessary indirection. Call the method directly.

2. **Memory Constraints**: Every command stores state for undo. If commands are large and the history is deep, memory
   grows. Limit history size to 50-100 commands.

3. **Simple Undo with Snapshots**: If undo is simple (e.g., save/restore entire state), Memento may be simpler than
   inverse commands.

4. **Over-Engineering Macros**: If macros are just sequential method calls, a simple `List<Runnable>` or lambda list may
   suffice without the full Command infrastructure.

5. **Real-Time Systems**: Command pattern adds object allocation per action. In real-time or memory-constrained systems,
   this allocation pressure matters.

## Interview Questions

### Q1: Explain the Command pattern and its four key participants.

**Answer**: Command encapsulates a request as an object. The four participants: **Command** (interface with execute), *
*ConcreteCommand** (binds an action to a receiver), **Receiver** (does the actual work), **Invoker** (triggers
execution). The Client creates commands and associates them with receivers.

### Q2: How does Command enable undo/redo?

**Answer**: Each command stores enough state to reverse its action (`undo()`). Executed commands are pushed onto an undo
stack. Undo pops the stack and calls `undo()`. Redo pushes to a redo stack. For actions that can't be reversed (
deletion), the command stores the deleted data as state before executing.

### Q3: How does Command differ from Strategy?

**Answer**: Command encapsulates a single action (often with undo) and separates the invoker from the receiver. Strategy
encapsulates an interchangeable algorithm. A Command is "do this one thing, optionally un-do it." Strategy is "here's
how to do this family of things."

### Q4: What's the relationship between Command and the Composite pattern?

**Answer**: MacroCommand is a composite of Commands. It implements Command (execute/undo) and contains child Commands.
execute() iterates children; undo() iterates in reverse. This lets clients treat individual commands and compositions
uniformly.

### Q5: How would you implement command queuing and retry?

**Answer**: Wrap the Command with a RetryCommand that catches exceptions and re-executes up to N times. The queue is a
`BlockingQueue<Command>` consumed by a worker thread. Failed commands go to a dead-letter queue for inspection.

### Q6: How does Command support the Single Responsibility Principle?

**Answer**: Each command class encapsulates one operation, separating the "what" (command) from the "how" (receiver)
from the "when" (invoker). The invoker doesn't know what the command does. The receiver doesn't know about undo or
queuing. Each has one responsibility.

### Q7: Can Command be implemented as a lambda in Java?

**Answer**: Yes, for simple cases without undo. Since Command is often a single-method interface, it's a
`@FunctionalInterface`. `Command c = () -> receiver.action();`. For undo support, concrete classes are still needed to
store reverse-action state.

### Q8: How does the Command pattern relate to transactions?

**Answer**: A transaction is a bounded sequence of commands executed atomically. If any command fails, all prior
commands are undone (rollback). Commands can also be logged to a journal for crash recovery. This is the foundation of
write-ahead logging (WAL) in databases.

### Follow-Up Question

**Interviewer**: "How would you design the undo system for a drawing application where operations like 'add shape', '
move shape', and 'change color' must all be undoable?"

**Answer**: Each operation is a Command. Shape operations store the shape's UUID and previous state (position, color).
AddShapeCommand stores the shape for undo removal. MoveShapeCommand stores the old coordinates. The undo stack holds all
commands. A MacroCommand handles grouped actions (e.g., moving a group). History is bounded at 100 entries with LRU
eviction.

## Pros & Cons

### Advantages

- **Decouples Invoker from Receiver**: UI buttons don't know about business logic
- **Undo/Redo**: First-class support via inverse operations
- **Queuing & Scheduling**: Commands can be stored, delayed, serialized
- **Composite Support**: Macro commands compose primitive commands
- **Open/Closed Principle**: New commands don't change existing invokers or receivers
- **Logging & Audit**: Centralized point to intercept all actions

### Disadvantages

- **Class Explosion**: Each action becomes a class (though lambdas help for simple cases)
- **Memory Usage**: History stacks hold command objects with state data
- **Complex Undo**: Some operations are hard to reverse (sending an email)
- **Indirection**: Following code flow requires tracing through invoker, command, and receiver
- **Serialization Overhead**: If commands must persist across restarts, serialization adds complexity

## Related Patterns

### Command vs Strategy

Command encapsulates a request; Strategy encapsulates an algorithm. Command is "what to do" (with undo). Strategy is "
how to do it" (interchangeably). A Strategy might be used inside a Command: a `SortCommand` might use a `SortStrategy`.

### Command vs Memento

**Memento** captures and externalizes an object's internal state for later restoration. **Command** uses inverse
operations for undo. Combine them: a Command can use Memento to snapshot state before execution for a simpler undo that
restores the snapshot rather than computing the inverse.

### Command vs Observer

**Observer** notifies subscribers of events. **Command** encapsulates actions. They combine well: an Observer receives
an event and creates/executes a Command. In UI frameworks: button click (Observer) → command.execute() (Command).

## Key Takeaways

1. **"Request as object"** — The core idea: turn an action into an object you can pass around, store, and queue.

2. **Undo is the killer feature** — When an interviewer asks about Command, immediately discuss undo/redo with a history
   stack. This is the pattern's standout use case.

3. **Composable via Composite** — MacroCommand demonstrates how patterns compose. Single commands build into scripts.

4. **OCP in action** — Add new commands without touching invoker, receiver, or existing commands.

5. **Interview memory aid** — "Command = action object, execute/undo, separation of concerns, undo stack + redo stack."
