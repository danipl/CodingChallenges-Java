# Composite Pattern (Object Tree Pattern)

## Overview

The **Composite Pattern** composes objects into tree structures to represent part-whole hierarchies, allowing clients to
treat individual objects and compositions of objects uniformly. **One-line interview answer**: The Composite pattern
lets you compose objects into tree structures and treat individual objects and composite objects the same way through a
common interface.

---

## Problem Statement

### Real-World Scenario

You're building a file system explorer. Files and folders need to be displayed in a tree view, their sizes need to be
computed recursively (summing all files in a folder), and operations like "delete" or "rename" must work on both
individual files and entire directory trees. Without the Composite pattern, the client must check `instanceof Folder`
before every operation and branch logic accordingly — duplicating tree-traversal code everywhere.

### Why This Matters in Production

Tree structures are everywhere in software:

- **UI component trees** — windows, panels, buttons, text fields nested in layouts
- **Organizational charts** — managers with direct reports who are also managers
- **Menu systems** — cascading menus with submenus and menu items
- **XML/JSON document models** — elements that contain other elements
- **Graphics editors** — groups containing shapes (circles, rectangles, other groups)

Without Composite, every client that navigates or operates on the tree must implement its own traversal logic and branch
on leaf vs. composite types.

### Pain Points Without Composite

- **Client must distinguish leaf vs. composite** — `if (node instanceof Directory)` scattered everywhere
- **No uniform operations** — deleting a file is `file.delete()` but deleting a folder requires recursive child deletion
- **Traversal logic duplicated** — every client reimplements the same tree walk
- **Adding new node types breaks clients** — every `instanceof` check needs updating
- **Leaky abstraction** — the client knows too much about the internal tree structure

---

## Solution

The Composite pattern defines a common interface for both leaf and composite objects. The composite object delegates
operations to its children and aggregates results. The client calls the same methods regardless of whether it's dealing
with a leaf or a composite.

### Key Participants

| Role          | Description                                                                     |
|---------------|---------------------------------------------------------------------------------|
| **Component** | Abstract interface for all objects in the composition (both leaf and composite) |
| **Leaf**      | Represents leaf objects — has no children. Implements Component directly        |
| **Composite** | Stores child Components and implements Component by delegating to children      |
| **Client**    | Manipulates objects through the Component interface                             |

### Flow

```
FileSystemComponent (interface)
  │
  ├── File (leaf) — implements getSize(), delete() directly
  └── Directory (composite) — contains List<Component>
        ├── getSize() → sum child.getSize() for each child
        ├── delete() → delete() each child, then remove self
        └── listContents() → recurse into children
```

```
Client calls: root.getSize()
  → Directory.getSize()
    → File (1).getSize() = 100
    → Directory (subdir).getSize()
      → File (2).getSize() = 50
      → File (3).getSize() = 200
    ← 350
  ← 350
```

---

## Java Implementation

### Component Interface

```java
package structural.composite;

import java.util.List;

// Common interface for both files and directories
public interface FileSystemComponent {
    String getName();
    long getSize();       // size in bytes
    boolean isDirectory();
    void delete();
    void rename(String newName);

    // Optional: methods only meaningful for composites
    // Throws UnsupportedOperationException for leaf nodes
    default void add(FileSystemComponent component) {
        throw new UnsupportedOperationException("Cannot add to a file");
    }

    default void remove(FileSystemComponent component) {
        throw new UnsupportedOperationException("Cannot remove from a file");
    }

    default List<FileSystemComponent> getChildren() {
        throw new UnsupportedOperationException("File has no children");
    }

    default void display(String indent) {
        System.out.println(indent + getName() + " (" + getSize() + " bytes)");
    }
}
```

### Leaf

```java
package structural.composite;

import java.time.Instant;

// Leaf node — represents a single file with no children
public class FileNode implements FileSystemComponent {
    private String name;
    private final long size;          // bytes
    private final Instant createdAt;

    public FileNode(String name, long size) {
        this.name = name;
        this.size = size;
        this.createdAt = Instant.now();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public long getSize() {
        return size;  // leaf returns its own size directly
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public void delete() {
        System.out.println("Deleting file: " + name);
        // In a real app: filesystem.delete(name);
    }

    @Override
    public void rename(String newName) {
        System.out.println("Renaming file " + name + " → " + newName);
        this.name = newName;
    }

    @Override
    public void display(String indent) {
        System.out.println(indent + "📄 " + name + " (" + size + " bytes)");
    }
}
```

### Composite

```java
package structural.composite;

import java.util.ArrayList;
import java.util.List;

// Composite node — represents a directory that can contain files and subdirectories
public class DirectoryNode implements FileSystemComponent {
    private String name;
    private final List<FileSystemComponent> children = new ArrayList<>();

    public DirectoryNode(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public long getSize() {
        // Sum sizes of all children recursively
        // Each child could be a File or another Directory
        long total = 0;
        for (FileSystemComponent child : children) {
            total += child.getSize();
        }
        return total;
    }

    @Override
    public boolean isDirectory() {
        return true;
    }

    @Override
    public void delete() {
        // Recursively delete all children first, then self
        System.out.println("Deleting directory: " + name);
        for (FileSystemComponent child : children) {
            child.delete();
        }
        children.clear();
        // In a real app: filesystem.deleteDirectory(name);
    }

    @Override
    public void rename(String newName) {
        System.out.println("Renaming directory " + name + " → " + newName);
        this.name = newName;
    }

    @Override
    public void add(FileSystemComponent component) {
        children.add(component);
    }

    @Override
    public void remove(FileSystemComponent component) {
        children.remove(component);
    }

    @Override
    public List<FileSystemComponent> getChildren() {
        return List.copyOf(children); // return defensive copy
    }

    @Override
    public void display(String indent) {
        System.out.println(indent + "📁 " + name + "/ (" + getSize() + " bytes)");
        for (FileSystemComponent child : children) {
            child.display(indent + "  ");
        }
    }

    // Convenience methods for tree manipulation
    public void addFile(String name, long size) {
        add(new FileNode(name, size));
    }

    public DirectoryNode addDirectory(String name) {
        DirectoryNode dir = new DirectoryNode(name);
        add(dir);
        return dir;
    }

    // Find a child by name (depth-first search)
    public FileSystemComponent findByName(String name) {
        for (FileSystemComponent child : children) {
            if (child.getName().equals(name)) {
                return child;
            }
            if (child.isDirectory()) {
                FileSystemComponent found = ((DirectoryNode) child).findByName(name);
                if (found != null) return found;
            }
        }
        return null;
    }
}
```

### Usage Example

```java
package structural.composite;

public class CompositeDemo {
    public static void main(String[] args) {
        // Build file system tree structure
        DirectoryNode root = new DirectoryNode("root");

        DirectoryNode home = root.addDirectory("home");
        DirectoryNode user = home.addDirectory("user");
        user.addFile("resume.pdf", 2_000_000);
        user.addFile("photo.jpg", 5_000_000);
        user.addFile("notes.txt", 1_000);

        DirectoryNode projects = user.addDirectory("projects");
        projects.addFile("app.java", 15_000);
        projects.addFile("index.html", 8_000);

        DirectoryNode etc = root.addDirectory("etc");
        etc.addFile("config.xml", 4_000);
        etc.addFile("hosts", 500);

        // Print tree structure — same display() call for both files and directories
        System.out.println("=== File System Tree ===");
        root.display("");

        // Uniform operation: getSize() works on both leaves and composites
        System.out.println("\n=== Sizes ===");
        System.out.println("Root size: " + root.getSize() + " bytes");
        System.out.println("User folder size: " + user.getSize() + " bytes");
        System.out.println("resume.pdf size: " + user.findByName("resume.pdf").getSize() + " bytes");

        // Uniform operation: rename works on any component
        FileSystemComponent notes = user.findByName("notes.txt");
        notes.rename("old_notes.txt");

        // Uniform operation: delete works on any component
        System.out.println("\n=== Deleting projects directory ===");
        projects.delete();  // deletes all files recursively

        System.out.println("\n=== After deletion ===");
        root.display("");

        // Demonstrate that client code doesn't need instanceof checks
        System.out.println("\n=== Polymorphic iteration ===");
        processComponent(user);
    }

    // Client code that works uniformly with any FileSystemComponent
    public static void processComponent(FileSystemComponent component) {
        System.out.println("Processing: " + component.getName()
            + " (type: " + (component.isDirectory() ? "directory" : "file")
            + ", size: " + component.getSize() + ")");
    }
}
```

### Advanced: Composite with Visitor for Operations

```java
package structural.composite;

// Visitor pattern can complement Composite for adding operations without modifying classes

interface FileSystemVisitor {
    void visit(FileNode file);
    void visit(DirectoryNode directory);
}

class SizeCalculatorVisitor implements FileSystemVisitor {
    private long totalSize = 0;

    @Override
    public void visit(FileNode file) {
        totalSize += file.getSize();
    }

    @Override
    public void visit(DirectoryNode directory) {
        // directory size calculated by summing children during traversal
    }
}

// Add accept() to Component interface
// Then: root.accept(new SizeCalculatorVisitor());
```

### Advanced: Safe vs. Transparent Composite

```java
package structural.composite;

// TRANSPARENT design (shown above): Component declares add/remove/getChildren
//   Pro: Client treats Leaf and Composite uniformly — no instanceof checks
//   Con: Leaf throws UnsupportedOperationException at runtime

// SAFE design: Component omits add/remove/getChildren
//   Composite declares add/remove/getChildren in its own class
//   Pro: Compile-time safety — Leaf cannot receive add() calls
//   Con: Client needs instanceof checks to add children

// RECOMMENDATION: Use Transparent for most cases — runtime safety is acceptable
// given the convenience of uniform treatment. Use Safe when you need compile-time
// guarantees (e.g., government/medical systems).
```

### SAX/DOM Parsing Analogy

```java
package structural.composite;

// XML/JSON document model is a real-world Composite:
//   - Element (composite) contains child Elements and Text nodes
//   - Text (leaf) has no children
// Both implement Node interface with getChildNodes(), getParentNode(), etc.

// DOM example:
//   Document doc = DocumentBuilderFactory.newInstance()
//       .newDocumentBuilder().parse("data.xml");
//   NodeList children = doc.getDocumentElement().getChildNodes();
//   for (int i = 0; i < children.getLength(); i++) {
//       Node child = children.item(i); // could be Element or Text
//       child.getNodeValue();          // works on either
//   }
```

---

## When to Use

1. **Tree-like hierarchical structures** — file systems, UI component trees, XML/JSON DOM, organizational charts
2. **Part-whole hierarchies where clients should ignore differences** — the client should be able to treat a single
   object and a group of objects identically
3. **Recursive operations** — operations like `getSize()`, `delete()`, `print()` naturally apply to both leaves and
   composites
4. **Graphics or document editors** — groups of shapes treated as a single shape (move, resize, rotate operations work
   uniformly)
5. **Menu systems** — menu items (leaf) and submenus (composite) share the same interface

### Framework / Library Examples

| Technology             | Composite Usage                                                                          |
|------------------------|------------------------------------------------------------------------------------------|
| **java.awt.Container** | Container (composite) holds Components (leaf). Both extend Component                     |
| **JSF / Swing**        | UI component trees: `JPanel` (composite) holds `JButton`, `JTextField` (leaves)          |
| **DOM (W3C)**          | `Node` interface: `Element` (composite) and `Text` (leaf)                                |
| **Spring Security**    | `FilterChain` is a composite of security filters                                         |
| **Maven/Gradle**       | Build hierarchies: projects contain sub-modules (composite)                              |
| **JUnit 5**            | `TestSuite` (composite) contains test cases (leaf) — though deprecated in newer versions |

---

## When NOT to Use

1. **Flat structures** — no hierarchy, no recursion needed. A simple list is sufficient
2. **Unbalanced trees with vastly different leaf and composite operations** — if leaves and composites have almost
   nothing in common, forcing them into a shared interface creates bloated interfaces
3. **When type safety matters and operations differ significantly** — the Transparent Composite approach throws runtime
   exceptions; a `Safe` Composite adds complexity
4. **Performance-critical leaf-only code** — empty `getChildren()` lists and `isDirectory()` checks add overhead
5. **When the hierarchy is stable and simple** — a typed `List<File>` + `List<Directory>` with distinct iteration is
   simpler

---

## Interview Questions

### Q1: What is the key benefit of the Composite pattern?

It enables clients to treat individual objects and compositions uniformly through the same interface. This eliminates
`instanceof` checks and duplicate iteration logic, making the code simpler, more maintainable, and open for extension (
adding new component types doesn't break existing clients).

### Q2: What is the difference between Transparent and Safe Composite?

**Transparent Composite** declares all child-management methods (`add`, `remove`, `getChildren`) in the Component
interface, so leaves and composites look identical to the client. Leaves throw `UnsupportedOperationException`. **Safe
Composite** only declares these methods in the Composite class, so the client must use `instanceof` to add children but
gets compile-time safety. Transparent is more common.

### Q3: How does Composite relate to the Single Responsibility Principle?

Composite can blur SRP because the Component interface combines two responsibilities: (1) the business operation (e.g.,
`getSize()`) and (2) child management (`add`, `remove`). However, this trade-off enables the uniform treatment that
makes Composite valuable. The **Safe Composite** variant preserves SRP at the cost of client complexity.

### Q4: Can you give a real-world Java example of Composite?

`java.awt.Container` extends `java.awt.Component`. Both `Container` and individual components like `Button` inherit from
`Component`. `Container` has `add(Component)` and `getComponents()`, and its `paint()` method iterates over and calls
`paint()` on each child. This is a classic Transparent Composite — adding a `Button` and adding a `Panel` (which
contains more components) are identical operations.

### Q5: How would you implement a recursive operation like `getSize()` safely? Use recursion or iteration?

Both work. Recursion is more natural for tree structures and cleaner with the Composite pattern. However, deep trees can
cause `StackOverflowError`. For production code with unknown depth, use iterative traversal with an explicit stack (
`Deque`):

```java
public long getSizeIterative(DirectoryNode root) {
    long total = 0;
    Deque<FileSystemComponent> stack = new ArrayDeque<>();
    stack.push(root);
    while (!stack.isEmpty()) {
        FileSystemComponent comp = stack.pop();
        if (!comp.isDirectory()) {
            total += comp.getSize();
        } else {
            for (FileSystemComponent child : comp.getChildren()) {
                stack.push(child);
            }
        }
    }
    return total;
}
```

### Q6: How does the Composite pattern relate to the Open/Closed Principle?

Composite follows OCP well: you can add new types of leaf nodes (e.g., `SymlinkNode`, `ArchiveFile`) without modifying
existing code, as long as they implement the `FileSystemComponent` interface. Composite classes work with any new
component type automatically through polymorphism.

### Q7: What is the difference between Composite and Decorator?

Both use recursive composition. **Composite** aggregates multiple children (1:N relationship) and delegates to all of
them. **Decorator** wraps a single sibling (1:1 relationship) and adds behavior before/after delegating. Composite
builds trees; Decorator builds chains. Composite unifies leaf and composite; Decorator preserves identity across
wrapping layers.

### Q8: How do you handle mutable children in a Composite (concurrent modifications)?

Use `CopyOnWriteArrayList` for `children` if reads >> writes and iteration happens during modification. Use
`Collections.synchronizedList()` for general thread safety. For complex trees, consider a read-write lock or immutable
snapshots. Also consider returning an unmodifiable view from `getChildren()` to prevent external mutation.

---

## Pros & Cons

### Advantages

- **Uniform treatment** — clients use the same interface for leaves and composites, eliminating conditionals
- **Recursive operations** — operations naturally apply to the entire tree (e.g., delete file = delete leaf; delete
  directory = delete all children)
- **Open/Closed Principle** — new component types added without changing existing code
- **Simplifies client code** — no `instanceof` checks, no type-casting, no duplicated traversal
- **Natural for tree structures** — directly mirrors the problem domain (file system, UI tree, organizational chart)

### Disadvantages

- **Over-generalizes interfaces** — Component interface may become bloated with methods that aren't applicable to all
  nodes (the "transparent" vs "safe" trade-off)
- **Runtime type checking** — leaf nodes throw `UnsupportedOperationException` for child-management methods
- **Deep trees can overflow the stack** — recursive operations need to be converted to iterative for very deep trees
- **Hard to constrain** — nothing prevents circular references (folder containing itself) unless explicitly validated
- **Performance overhead** — virtual method dispatch for every node in the tree; iterator creation for every level

---

## Related Patterns

| Pattern       | Relationship                                                                          | When to Choose                                                 |
|---------------|---------------------------------------------------------------------------------------|----------------------------------------------------------------|
| **Decorator** | Both compose recursively. Composite aggregates multiple children; Decorator wraps one | Tree structures → Composite; Chain of enhancements → Decorator |
| **Iterator**  | Often used together to traverse a Composite tree without exposing internal structure  | Need to traverse the tree → Iterator                           |
| **Visitor**   | Defines a new operation on a Composite tree without modifying the Component classes   | Adding many operations across a stable tree → Visitor          |
| **Flyweight** | Shares leaf nodes across multiple composite trees when leaf state is intrinsic        | Large trees with repeated leaf data → Flyweight                |

### Key Distinction Memory Aid

> **Composite** builds a tree where branches and leaves share the same interface.  
> **Decorator** wraps a single object in a chain with the same interface.  
> **Flyweight** shares identical leaves across multiple trees to save memory.

---

## Key Takeaways

- **Tree structure made uniform** — the core insight is that clients should not care whether they're dealing with a leaf
  or a branch. This simplifies code dramatically
- **Transparent vs Safe** — know the trade-off (Transparent is more common and convenient; Safe gives compile-time
  safety)
- **Recursive composition** — the pattern's power comes from recursive delegation: composites call the same method on
  all children, which may be leaves or other composites
- **SOLID alignment** — Open/Closed (new component types via extension), Liskov Substitution (composite substitutes for
  component), Interface Segregation (though blurred in Transparent variant)
- **Interview tip** — draw a file system tree on the whiteboard. Show `File` (leaf) and `Directory` (composite)
  implementing the same `FileSystemComponent` interface. Write `getSize()` to demonstrate recursive delegation. Mention
  `java.awt.Component`/`Container` as the canonical Java example
