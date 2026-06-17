# Visitor Pattern

## Overview

**Definition**: Visitor lets you define a new operation on a set of objects without changing the classes of those
objects. It separates the algorithm from the object structure it operates on.

**Core Problem**: How to add new operations to a stable hierarchy of classes without modifying each class, especially
when the operations are many and diverse.

**One-Line Interview Answer**: "Visitor lets you add new operations to an object structure without modifying the objects
themselves, by using double dispatch to route each object to the correct visitor method."

## Problem Statement

### Real-World Scenario: Document Export

A document system has a class hierarchy: `Paragraph`, `Table`, `Image`, `Header`, `Footer`. You need to add export
operations: HTML export, PDF export, Markdown export, plain text export, and accessibility analysis. Each export
requires different logic for each element type.

Naive approach: add an export method to each class:

```java
public abstract class DocumentElement {
    public abstract String toHtml();
    public abstract String toPdf();
    public abstract String toMarkdown();
    public abstract String toPlainText();
    // Every new export type requires adding a method to EVERY subclass
}

public class Paragraph extends DocumentElement {
    @Override public String toHtml() { return "<p>" + text + "</p>"; }
    @Override public String toPdf() { return "PDF paragraph: " + text; }
    @Override public String toMarkdown() { return text + "\n"; }
    // toPlainText, toLatex, toDocx, ...
}

public class Image extends DocumentElement {
    @Override public String toHtml() { return "<img src='" + src + "' />"; }
    @Override public String toPdf() { return "PDF image: " + src; }
    @Override public String toMarkdown() { return "![" + alt + "](" + src + ")"; }
    // Every element must implement EVERY export method
}
```

### Pain Points of the Naive Approach

1. **Class Hierarchy Pollution**: The document element classes are cluttered with export methods. A `Paragraph`
   shouldn't know about PDF rendering, HTML escaping, or Markdown syntax.
2. **Open/Closed Violation**: Adding a new export (e.g., LaTeX) requires modifying every concrete element class. For 20
   elements × 10 exports = 200 method implementations to add.
3. **Scattered Logic**: The PDF export logic for Paragraph is in `Paragraph.java`, for Image in `Image.java`, etc. You
   can't see all PDF logic in one place.
4. **Impossible for Third-Party Classes**: If the element hierarchy is from a library, you cannot add methods to it.
5. **Cross-Cutting Concerns**: Operations that span multiple element types (e.g., "count total words across all
   elements") have no natural home.

### Why This Matters in Production

Visitor solves the "expression problem": how to add new operations to a stable type hierarchy. This is critical in
compilers (AST visitors), document processing (export/import), UI component traversal (accessibility checking), and
anywhere you have a stable object structure with volatile operations.

## Solution

### How Visitor Solves This

Visitor uses **double dispatch**: the element calls `visitor.visit(this)`, and the virtual method dispatch on `visitor`
selects the correct `visit(Paragraph)`, `visit(Image)`, etc. This lets you add new operations by adding new Visitor
classes — the element hierarchy stays untouched.

### Key Participants

| Participant           | Role                                                            |
|-----------------------|-----------------------------------------------------------------|
| `Visitor` (interface) | Declares `visit(ConcreteElement)` methods for each element type |
| `ConcreteVisitor`     | Implements the operation for each element type                  |
| `Element` (interface) | Declares `accept(Visitor)` method                               |
| `ConcreteElement`     | Implements `accept(Visitor v) { v.visit(this); }`               |
| `ObjectStructure`     | Provides a way to enumerate elements (list, tree, composite)    |

### Step-by-Step Flow (Double Dispatch)

1. Client creates a ConcreteVisitor (e.g., `HtmlExporter`)
2. Client calls `element.accept(visitor)` on each element
3. The element's `accept()` method calls `visitor.visit(this)`
4. The JVM dispatches to the correct `visit()` overload based on `this`'s compile-time type (first dispatch: element
   type)
5. The JVM dispatches to `visit(Paragraph)` based on the parameter type (second dispatch: visitor method overload)
6. Two dispatch steps → one correct method called

### UML-Style Structure

```
┌──────────────────┐       ┌──────────────────┐
│  «interface»     │       │  «interface»     │
│  Element         │       │  Visitor         │
│                  │       │                  │
│ +accept(Visitor) │       │ +visit(ElementA) │
└────────┬─────────┘       │ +visit(ElementB) │
         │                 │ +visit(ElementC) │
         │                 └────────┬─────────┘
         │                          │
    ┌────┴────┐           ┌─────────┴──────────┐
    │ElementA │           │ ConcreteVisitor    │
    │         │           │                    │
    │+accept  │           │ +visit(ElementA)   │
    │(Visitor)│──────────→│ +visit(ElementB)   │
    └─────────┘  visitor  │ +visit(ElementC)   │
                  .visit  └────────────────────┘
                  (this)
    Double dispatch: Element type from this,
    Visitor method from parameter.
```

## Java Implementation

### Element Hierarchy (Closed for Modification)

```java
package behavioral.visitor;

import java.util.List;

// Element interface
interface DocumentElement {
    void accept(DocumentVisitor visitor);
}

// Concrete elements
class Paragraph implements DocumentElement {
    private final String text;

    public Paragraph(String text) {
        this.text = text;
    }

    public String getText() { return text; }

    @Override
    public void accept(DocumentVisitor visitor) {
        visitor.visit(this);
    }
}

class Table implements DocumentElement {
    private final List<List<String>> rows;

    public Table(List<List<String>> rows) {
        this.rows = rows;
    }

    public List<List<String>> getRows() { return rows; }

    @Override
    public void accept(DocumentVisitor visitor) {
        visitor.visit(this);
    }
}

class Image implements DocumentElement {
    private final String src;
    private final String alt;

    public Image(String src, String alt) {
        this.src = src;
        this.alt = alt;
    }

    public String getSrc() { return src; }
    public String getAlt() { return alt; }

    @Override
    public void accept(DocumentVisitor visitor) {
        visitor.visit(this);
    }
}

class Header implements DocumentElement {
    private final int level;
    private final String text;

    public Header(int level, String text) {
        this.level = level;
        this.text = text;
    }

    public int getLevel() { return level; }
    public String getText() { return text; }

    @Override
    public void accept(DocumentVisitor visitor) {
        visitor.visit(this);
    }
}
```

### Visitor Interface

```java
interface DocumentVisitor {
    void visit(Paragraph paragraph);
    void visit(Table table);
    void visit(Image image);
    void visit(Header header);
}
```

### Concrete Visitor: HTML Export

```java
class HtmlExporter implements DocumentVisitor {
    private final StringBuilder sb = new StringBuilder();

    @Override
    public void visit(Paragraph paragraph) {
        sb.append("<p>").append(escapeHtml(paragraph.getText())).append("</p>\n");
    }

    @Override
    public void visit(Table table) {
        sb.append("<table>\n");
        boolean firstRow = true;
        for (var row : table.getRows()) {
            sb.append("  <tr>\n");
            for (var cell : row) {
                String tag = firstRow ? "th" : "td";
                sb.append("    <").append(tag).append(">")
                  .append(escapeHtml(cell)).append("</").append(tag).append(">\n");
            }
            sb.append("  </tr>\n");
            firstRow = false;
        }
        sb.append("</table>\n");
    }

    @Override
    public void visit(Image image) {
        sb.append("<img src=\"")
          .append(escapeHtml(image.getSrc()))
          .append("\" alt=\"")
          .append(escapeHtml(image.getAlt()))
          .append("\" />\n");
    }

    @Override
    public void visit(Header header) {
        sb.append("<h").append(header.getLevel()).append(">")
          .append(escapeHtml(header.getText()))
          .append("</h").append(header.getLevel()).append(">\n");
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }

    public String getHtml() { return sb.toString(); }
}
```

### Concrete Visitor: Markdown Export

```java
class MarkdownExporter implements DocumentVisitor {
    private final StringBuilder sb = new StringBuilder();

    @Override
    public void visit(Paragraph paragraph) {
        sb.append(paragraph.getText()).append("\n\n");
    }

    @Override
    public void visit(Table table) {
        if (table.getRows().isEmpty()) return;
        // Header row
        var header = table.getRows().get(0);
        sb.append("| ");
        header.forEach(cell -> sb.append(cell).append(" | "));
        sb.append("\n| ");
        header.forEach(cell -> sb.append("--- | "));
        sb.append("\n");
        // Data rows
        for (int i = 1; i < table.getRows().size(); i++) {
            sb.append("| ");
            table.getRows().get(i).forEach(cell -> sb.append(cell).append(" | "));
            sb.append("\n");
        }
        sb.append("\n");
    }

    @Override
    public void visit(Image image) {
        sb.append("![").append(image.getAlt()).append("](").append(image.getSrc()).append(")\n");
    }

    @Override
    public void visit(Header header) {
        sb.append("#".repeat(header.getLevel())).append(" ").append(header.getText()).append("\n\n");
    }

    public String getMarkdown() { return sb.toString(); }
}
```

### Concrete Visitor: Word Counter (with Result Return)

```java
import java.util.HashMap;
import java.util.Map;

class WordCounterVisitor implements DocumentVisitor {
    private int totalWords = 0;
    private final Map<Class<?>, Integer> elementCounts = new HashMap<>();

    @Override
    public void visit(Paragraph paragraph) {
        int words = countWords(paragraph.getText());
        totalWords += words;
        elementCounts.merge(Paragraph.class, 1, Integer::sum);
    }

    @Override
    public void visit(Table table) {
        for (var row : table.getRows()) {
            for (var cell : row) {
                totalWords += countWords(cell);
            }
        }
        elementCounts.merge(Table.class, 1, Integer::sum);
    }

    @Override
    public void visit(Image image) {
        totalWords += countWords(image.getAlt());
        elementCounts.merge(Image.class, 1, Integer::sum);
    }

    @Override
    public void visit(Header header) {
        totalWords += countWords(header.getText());
        elementCounts.merge(Header.class, 1, Integer::sum);
    }

    private int countWords(String text) {
        return text.isEmpty() ? 0 : text.split("\\s+").length;
    }

    public int getTotalWords() { return totalWords; }
    public Map<Class<?>, Integer> getElementCounts() { return elementCounts; }
}
```

### Object Structure (Document)

```java
class Document implements DocumentElement {
    private final List<DocumentElement> elements;

    public Document(List<DocumentElement> elements) {
        this.elements = elements;
    }

    @Override
    public void accept(DocumentVisitor visitor) {
        for (var element : elements) {
            element.accept(visitor);
        }
    }

    // Convenience: apply visitor to all elements
    public void export(DocumentVisitor visitor) {
        accept(visitor);
    }
}
```

### Usage Demo

```java
public class VisitorDemo {
    public static void main(String[] args) {
        // Build document structure
        var doc = new Document(List.of(
            new Header(1, "Annual Report"),
            new Paragraph("This is the summary of our annual performance."),
            new Table(List.of(
                List.of("Metric", "2023", "2024"),
                List.of("Revenue", "$1M", "$1.5M"),
                List.of("Users", "10K", "25K")
            )),
            new Image("chart.png", "Revenue growth chart"),
            new Header(2, "Future Outlook"),
            new Paragraph("We expect continued growth in 2025.")
        ));

        // Export to HTML
        var htmlExporter = new HtmlExporter();
        doc.accept(htmlExporter);
        System.out.println("=== HTML Export ===");
        System.out.println(htmlExporter.getHtml());

        // Export to Markdown
        var mdExporter = new MarkdownExporter();
        doc.accept(mdExporter);
        System.out.println("=== Markdown Export ===");
        System.out.println(mdExporter.getMarkdown());

        // Count words
        var counter = new WordCounterVisitor();
        doc.accept(counter);
        System.out.println("=== Word Count ===");
        System.out.println("Total words: " + counter.getTotalWords());
        System.out.println("Element counts: " + counter.getElementCounts());
    }
}
```

### Visitor with Java Records (Modern Pattern)

```java
// Element hierarchy using sealed classes (Java 17+)
sealed interface Shape permits Circle, Rectangle, Triangle {
    void accept(ShapeVisitor visitor);
}

record Circle(double radius) implements Shape {
    public void accept(ShapeVisitor visitor) { visitor.visit(this); }
}

record Rectangle(double width, double height) implements Shape {
    public void accept(ShapeVisitor visitor) { visitor.visit(this); }
}

record Triangle(double base, double height) implements Shape {
    public void accept(ShapeVisitor visitor) { visitor.visit(this); }
}

interface ShapeVisitor {
    void visit(Circle circle);
    void visit(Rectangle rectangle);
    void visit(Triangle triangle);
}

class AreaCalculator implements ShapeVisitor {
    private double totalArea = 0;

    @Override
    public void visit(Circle circle) {
        totalArea += Math.PI * circle.radius() * circle.radius();
    }

    @Override
    public void visit(Rectangle rectangle) {
        totalArea += rectangle.width() * rectangle.height();
    }

    @Override
    public void visit(Triangle triangle) {
        totalArea += 0.5 * triangle.base() * triangle.height();
    }

    public double getTotalArea() { return totalArea; }
}

class ShapeDemo {
    public static void main(String[] args) {
        var shapes = List.of(
            new Circle(5),
            new Rectangle(4, 6),
            new Triangle(3, 4)
        );

        var calc = new AreaCalculator();
        shapes.forEach(s -> s.accept(calc));
        System.out.printf("Total area: %.2f%n", calc.getTotalArea());
    }
}
```

### Visitor with Functional Approach

```java
import java.util.function.Consumer;

class FunctionalVisitor {
    private final java.util.Map<Class<?>, Consumer<DocumentElement>> handlers = new java.util.HashMap<>();

    public <T extends DocumentElement> void on(Class<T> type, Consumer<T> handler) {
        handlers.put(type, handler::accept);
    }
}
```

## When to Use

1. **Compilers & AST Processors**: The canonical use case. An AST has stable node types (Expression, Statement,
   VariableDeclaration). Visitors implement semantic analysis, type checking, optimization, and code generation without
   modifying AST classes.

2. **Document Export/Import**: Different output formats (HTML, PDF, Markdown, JSON, XML) are Visitors. The document
   element hierarchy is stable; export formats are added frequently.

3. **UI Component Traversal**: Accessibility checking, UI testing, component tree serialization. Each visitor traverses
   the component tree performing one specific task.

4. **Code Analysis Tools**: Static analysis (find bugs, check style, compute metrics). Each analysis rule is a Visitor
   over the AST. Tools like Checkstyle, PMD, and SpotBugs use Visitor.

5. **Object Graph Serialization**: JSON/XML/YAML serializers are visitors. Each type knows how to accept a visitor; the
   visitor handles serialization. Jackson's `JsonSerializer` is conceptually a Visitor.

### Framework Examples

- **Java Compiler API**: `javax.lang.model.element.Element` with `ElementVisitor` — the Visitor pattern for annotation
  processing.
- **ASM Library**: `ClassVisitor`, `MethodVisitor`, `FieldVisitor` for bytecode analysis and transformation.
- **Lombok**: Uses Visitor pattern to traverse ASTs and inject generated code.
- **Jackson `JsonSerializer<T>`**: Type-specific serializers are visitors over the object graph.

## When NOT to Use

1. **Unstable Element Hierarchy**: If element types are added frequently, every visitor must be updated with a new
   `visit()` method. Visitor is suitable when the element hierarchy is stable and operations change.

2. **Few Operations**: If there are only 1-2 operations on the hierarchy, simple polymorphism (`toString()`, `toHtml()`)
   is simpler. Visitor requires creating a full visitor interface and implementation classes.

3. **Encapsulation Breakage**: Visitor often forces elements to expose their internals via public getters. If elements
   should keep data private, Visitor cannot access it. Consider adding specific access methods.

4. **Performance-Critical Code**: The `accept() → visit()` double dispatch adds overhead. In tight loops processing
   millions of nodes, this matters. Use switch expressions with pattern matching (Java 21+).

5. **Cyclic Object Structures**: Visitor works cleanly on trees. For cyclic graphs, you need a visited-set to prevent
   infinite loops in the accept calls.

## Interview Questions

### Q1: What is the Visitor pattern and what problem does it solve?

**Answer**: Visitor lets you add new operations to a class hierarchy without modifying the classes. It solves the "
expression problem": stable element types + volatile operations. Instead of adding a method to every element for each
new operation, you create one Visitor class per operation.

### Q2: Explain double dispatch in Visitor.

**Answer**: Double dispatch means the method call is dispatched twice: once on the element type (via `accept(visitor)`)
and once on the visitor type (via `visitor.visit(this)`). Single dispatch (normal OOP) only dispatches on the receiver.
The `this` in `visitor.visit(this)` gives the visitor the element's compile-time type, enabling overload resolution.

### Q3: What are the trade-offs of Visitor vs simple polymorphism?

**Answer**: Polymorphism (adding methods to elements): easy to add new elements, hard to add new operations. Visitor:
hard to add new elements, easy to add new operations. If element types are stable and operations change frequently (
compilers, document export), use Visitor. If element types change frequently, use polymorphism.

### Q4: How does Visitor violate the Law of Demeter (Principle of Least Knowledge)?

**Answer**: Visitor often forces elements to expose their internal state via public getters (e.g.,
`Paragraph.getText()`). This violates encapsulation because elements expose data purely for visitor convenience. The
trade-off: accept public getters for the flexibility Visitor provides. Some implementations use reflection to avoid
this.

### Q5: How does Java 17+ pattern matching affect the Visitor pattern?

**Answer**: Pattern matching with `switch` expressions can replace Visitor for simple cases. Instead of
`element.accept(visitor)`, you write `switch (element) { case Paragraph p -> ...; case Table t -> ...; }`. This avoids
the double dispatch mechanism but couples the operation to the element hierarchy. For infrequent operations (one-off),
pattern matching is simpler. For families of operations (many visitors), Visitor is better.

### Q6: How would you add a new element type to a Visitor-based system?

**Answer**: Add a new `visit(NewElement)` method to the Visitor interface and implement it in all existing visitors.
This is the Visitor pattern's biggest pain point — it violates OCP when element types change. Mitigation: use a default
method in the Visitor interface that throws UnsupportedOperationException, so existing visitors don't break but new
types may not be fully handled.

### Q7: What is the "Acyclic Visitor" variant?

**Answer**: Acyclic Visitor avoids the requirement that Visitor must know about all element types. Instead of one
Visitor interface with methods for all elements, each element type defines its own visitor interface. Visitors implement
only the interfaces they care about. This eliminates the dependency but makes traversal more complex.

### Q8: How does Visitor relate to the Open/Closed Principle?

**Answer**: Visitor favors adding OPERATIONS (OCP). The element hierarchy is open for new visitors but closed for new
methods. Adding a new operation means creating a new Visitor class — no elements change. However, adding a new element
type violates OCP — all visitors must be updated. Visitor shifts the OCP fulcrum from elements to operations.

### Follow-Up Question

**Interviewer**: "Design a visitor for a file system that supports calculating total size, finding duplicates, and
generating a tree view."

**Answer**: `FileElement` interface with `accept(FileVisitor)`. `FileVisitor` has `visit(TextFile)`, `visit(Directory)`,
`visit(Symlink)`. Directory's `accept()` recursively calls `accept()` on children. SizeCalculator sums file sizes
recursively. DuplicateFinder computes content hashes and groups files by hash. TreeViewer builds indented string output.
New operations (search, compress) = new visitors.

## Pros & Cons

### Advantages

- **Open/Closed Principle (Operations)**: New operations added without modifying elements
- **Single Responsibility**: Each operation is bundled in one Visitor class
- **Related Operations Together**: All code for "HTML export" is in one class, not scattered across elements
- **Accumulates State**: Visitors can maintain state across the traversal (word count, total area, duplicate map)
- **Double Dispatch**: Enables type-specific behavior without type checks
- **Works with Composite**: Can traverse complex object structures (trees, composites)

### Disadvantages

- **Element Hierarchy Changes Hard**: Adding a new element type requires updating all visitors
- **Encapsulation Breakage**: Elements must expose internal state for visitors
- **Cyclic Dependencies**: Element package must know about Visitor; Visitor must know about all elements
- **Verbose Boilerplate**: Each element must implement `accept()`; each visitor must implement every `visit()` method
- **Overkill for Simple Operations**: A single `getArea()` method on Shape is simpler than a visitor
- **Double Dispatch Overhead**: Two virtual method calls per element instead of one

## Related Patterns

### Visitor vs Iterator

**Iterator** traverses a structure; **Visitor** performs an operation on each element. They're often combined: Iterator
provides sequential access, Visitor performs the operation. `for (var e : tree) { e.accept(visitor); }` uses Iterator
for traversal and Visitor for operation.

### Visitor vs Composite

**Composite** builds tree structures. **Visitor** operates on them. The Composite's `accept()` method recursively calls
`accept()` on children, so a visitor automatically traverses the entire tree. This is a powerful combination: the
structure is a Composite; the operation is a Visitor.

### Visitor vs Strategy

**Strategy** selects an algorithm at runtime. **Visitor** dispatches an operation to type-specific methods. Strategy
varies "how to do something" (one method, multiple implementations). Visitor varies "do something for each type" (
multiple methods, multiple implementations per type). Strategy is 1:N; Visitor is M:N.

## Key Takeaways

1. **"Stable types, volatile operations"** — Use Visitor when the element hierarchy changes rarely but new operations
   are added frequently. This is the key decision criterion.

2. **Double dispatch is the mechanism** — The `accept(visitor)` + `visitor.visit(this)` pair enables two virtual calls,
   achieving what single-dispatch languages cannot do natively.

3. **The expression problem** — Visitor solves one side: make it easy to add operations. Polymorphism solves the other:
   make it easy to add types. Choose based on which axis is more volatile.

4. **Sealed classes + pattern matching (Java 17+)** are a partial alternative — they provide exhaustive switch matching
   over types, reducing the need for Visitor in some cases. But they don't separate operations into separate classes.

5. **Interview memory aid** — "Visitor = double dispatch, stable elements + volatile operations, Acyclic variant for
   flexibility, trade-off with polymorphism."
