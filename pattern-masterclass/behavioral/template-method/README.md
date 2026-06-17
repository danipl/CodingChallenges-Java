# Template Method Pattern

## Overview

**Definition**: Template Method defines the skeleton of an algorithm in a base class, letting subclasses override
specific steps without changing the algorithm's structure. It's the Hollywood Principle: "Don't call us, we'll call
you."

**Core Problem**: How to define the invariant parts of an algorithm once and allow subclasses to provide the variant
parts, avoiding code duplication and ensuring the algorithm structure remains consistent.

**One-Line Interview Answer**: "Template Method lets subclasses redefine certain steps of an algorithm without changing
the algorithm's structure, using inheritance to enforce the skeleton while deferring implementation details to
subclasses."

## Problem Statement

### Real-World Scenario: Data Migration Framework

Building a framework that migrates data from various sources (CSV files, databases, REST APIs) into a target system. The
migration steps are always the same:

1. Connect to source
2. Extract data
3. Transform data (normalize, validate)
4. Load data into target
5. Disconnect

A naive approach duplicates this flow in each migrator:

```java
public class CsvMigrator {
    public void migrate(String filePath) {
        System.out.println("Opening CSV file: " + filePath);
        // read lines
        List<String[]> rows = readCsv(filePath);
        // transform
        List<Record> records = rows.stream().map(this::toRecord).toList();
        // validate
        List<Record> valid = records.stream().filter(this::isValid).toList();
        // load
        valid.forEach(rec -> System.out.println("Inserting: " + rec));
        System.out.println("Closing CSV file");
    }
}

public class DatabaseMigrator {
    public void migrate(String connectionString) {
        System.out.println("Connecting to database: " + connectionString);
        // query
        List<Record> records = queryDatabase(connectionString);
        // transform
        List<Record> transformed = records.stream().map(this::enrich).toList();
        // validate
        List<Record> valid = transformed.stream().filter(this::isValid).toList();
        // load
        valid.forEach(rec -> System.out.println("Upserting: " + rec));
        System.out.println("Closing database connection");
    }
}
// Every migrator repeats the same skeleton — only steps 2-4 vary
```

### Pain Points of the Naive Approach

1. **Code Duplication**: The skeleton (connect → extract → transform → load → disconnect) is repeated across every
   migrator.
2. **Inconsistent Structure**: One migrator may forget validation or do it in a different order.
3. **Violates DRY**: Changes to the skeleton (e.g., adding a "log step duration" step) require touching every migrator.
4. **Hard to Enforce Contracts**: Subclasses might skip steps or add undocumented steps.
5. **Framework Design Impossible**: This pattern prevents building a framework where the framework controls the flow and
   plugins provide steps.

### Why This Matters in Production

Template Method is the foundation of framework design. JUnit's test lifecycle (`@BeforeEach`, `@Test`, `@AfterEach`),
Spring's bean initialization, and servlet lifecycle (`init()`, `service()`, `destroy()`) are all Template Methods.
Without it, every user of the framework would need to reimplement the lifecycle orchestration.

## Solution

### How Template Method Solves This

The base class defines the `templateMethod()` with the algorithm skeleton. Steps are marked as `abstract` (subclasses
must implement) or `hook` (subclasses may override). The base class controls the "what" and "when"; subclasses provide
the "how."

### Key Participants

| Participant     | Role                                                                                                                                   |
|-----------------|----------------------------------------------------------------------------------------------------------------------------------------|
| `AbstractClass` | Defines `templateMethod()` with algorithm skeleton; declares abstract primitive operations; provides default implementations for hooks |
| `ConcreteClass` | Implements primitive operations; may override hook methods                                                                             |

### Step-by-Step Flow

1. Client calls `abstractClass.templateMethod()`
2. The template method runs the algorithm:
    - Calls `stepOne()` (abstract — implemented by subclass)
    - Calls `stepTwo()` (abstract)
    - Calls `hookMethod()` (optional override)
    - Calls `stepThree()` (abstract)
3. The algorithm structure is fixed; only the steps vary

### UML-Style Structure

```
┌────────────────────────────┐
│  AbstractClass             │
│                            │
│ +templateMethod()          │──final—defines skeleton
│   stepOne()                │
│   stepTwo()                │
│   hook()                   │
│   stepThree()              │
│                            │
│ #abstract stepOne()        │
│ #abstract stepTwo()        │
│ #abstract stepThree()      │
│ #hook() { }                │← optional override
└────────────┬───────────────┘
             │
    ┌────────┴────────┐
    │ ConcreteClass   │
    │                 │
    │ stepOne()       │
    │ stepTwo()       │
    │ hook()          │← optional
    │ stepThree()     │
    └─────────────────┘
```

## Java Implementation

### Abstract Class with Template Method

```java
package behavioral.templatemethod;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

abstract class DataMigrator {

    // Template method — defines the algorithm skeleton
    // Marked final so subclasses cannot change the structure
    public final void migrate(String source) {
        try {
            connect(source);
            List<Map<String, String>> rawData = extract();
            List<Map<String, String>> transformed = transform(rawData);
            List<Map<String, String>> validated = validate(transformed);
            int count = load(validated);
            postProcess(count);
            disconnect();
        } catch (Exception e) {
            handleError(e);
            disconnect();
        }
    }

    // Primitive operations — subclasses must implement
    protected abstract void connect(String source) throws IOException;
    protected abstract List<Map<String, String>> extract() throws IOException;

    // Hook method — subclasses may override; default just passes through
    protected List<Map<String, String>> transform(List<Map<String, String>> data) {
        return data; // Default: no transformation
    }

    // Hook method with default validation
    protected List<Map<String, String>> validate(List<Map<String, String>> data) {
        return data.stream()
            .filter(row -> row.values().stream().anyMatch(v -> v != null && !v.isBlank()))
            .collect(Collectors.toList());
    }

    // Primitive operation
    protected abstract int load(List<Map<String, String>> data) throws IOException;

    // Hook method — called after successful load
    protected void postProcess(int recordCount) {
        System.out.println("Migration completed: " + recordCount + " records");
    }

    // Hook method for error handling
    protected void handleError(Exception e) {
        System.err.println("Migration failed: " + e.getMessage());
    }

    // Primitive operation
    protected abstract void disconnect();
}
```

### Concrete Class: CSV Migrator

```java
class CsvMigrator extends DataMigrator {
    private Path filePath;
    private List<String> lines;

    @Override
    protected void connect(String source) {
        this.filePath = Path.of(source);
        System.out.println("CSV source: " + filePath.toAbsolutePath());
    }

    @Override
    protected List<Map<String, String>> extract() throws IOException {
        lines = Files.readAllLines(filePath);
        if (lines.isEmpty()) return List.of();

        String[] headers = lines.get(0).split(",");
        return lines.stream()
            .skip(1) // skip header
            .map(line -> {
                String[] values = line.split(",");
                Map<String, String> map = new LinkedHashMap<>();
                for (int i = 0; i < headers.length && i < values.length; i++) {
                    map.put(headers[i].trim(), values[i].trim());
                }
                return map;
            })
            .collect(Collectors.toList());
    }

    @Override
    protected List<Map<String, String>> transform(List<Map<String, String>> data) {
        // CSV-specific: trim all string values
        return data.stream()
            .map(row -> {
                Map<String, String> trimmed = new LinkedHashMap<>();
                row.forEach((k, v) -> trimmed.put(k, v != null ? v.trim() : ""));
                return trimmed;
            })
            .collect(Collectors.toList());
    }

    @Override
    protected int load(List<Map<String, String>> data) {
        data.forEach(row -> System.out.println("INSERT INTO target VALUES " + row));
        return data.size();
    }

    @Override
    protected void disconnect() {
        System.out.println("CSV resources released");
    }
}
```

### Concrete Class: REST API Migrator

```java
import java.net.URI;
import java.net.http.*;

class RestApiMigrator extends DataMigrator {
    private HttpClient client;
    private String baseUrl;
    private HttpResponse<String> response;

    @Override
    protected void connect(String source) {
        this.baseUrl = source;
        this.client = HttpClient.newHttpClient();
        System.out.println("REST endpoint: " + baseUrl);
    }

    @Override
    protected List<Map<String, String>> extract() throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/data"))
            .GET()
            .build();

        response = client.send(request, HttpResponse.BodyHandlers.ofString());
        // Simplified: parse JSON array → list of maps
        System.out.println("Fetched " + response.body().length() + " bytes");
        return List.of(Map.of("raw", response.body()));
    }

    @Override
    protected List<Map<String, String>> transform(List<Map<String, String>> data) {
        // Parse JSON and map fields
        System.out.println("Transforming JSON to relational format");
        return data; // Simplified
    }

    @Override
    protected int load(List<Map<String, String>> data) throws IOException {
        var json = data.get(0).get("raw");
        var postRequest = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/target"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

        try {
            var postResponse = client.send(postRequest, HttpResponse.BodyHandlers.ofString());
            System.out.println("POST response: " + postResponse.statusCode());
            return 1;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }

    @Override
    protected void postProcess(int recordCount) {
        System.out.println("REST migration completed. Response saved to target.");
    }

    @Override
    protected void handleError(Exception e) {
        System.err.println("REST API error: " + e.getMessage());
    }

    @Override
    protected void disconnect() {
        client = null;
        System.out.println("HTTP client closed");
    }
}
```

### Hook Method Example: DataValidationHook

```java
class StrictValidationCsvMigrator extends CsvMigrator {
    @Override
    protected List<Map<String, String>> validate(List<Map<String, String>> data) {
        System.out.println("Applying strict validation...");
        return data.stream()
            .filter(row -> row.containsKey("email") && row.get("email").contains("@"))
            .filter(row -> row.containsKey("age") && Integer.parseInt(row.get("age")) > 0)
            .collect(Collectors.toList());
    }

    @Override
    protected void postProcess(int recordCount) {
        System.out.println("Strict migration: " + recordCount + " valid records loaded");
        if (recordCount == 0) {
            System.out.println("WARNING: No records passed validation!");
        }
    }
}
```

### Usage Demo

```java
public class TemplateMethodDemo {
    public static void main(String[] args) throws Exception {
        System.out.println("=== CSV Migration ===");
        DataMigrator csvMigrator = new CsvMigrator();
        csvMigrator.migrate("data/users.csv");

        System.out.println("\n=== REST Migration ===");
        DataMigrator restMigrator = new RestApiMigrator();
        restMigrator.migrate("https://api.example.com");

        System.out.println("\n=== Strict CSV Migration ===");
        DataMigrator strictMigrator = new StrictValidationCsvMigrator();
        strictMigrator.migrate("data/users.csv");
    }
}
```

### Algorithm Timing Decorator via Extending Template

```java
abstract class TimedDataMigrator extends DataMigrator {
    // Template method is final — cannot be overridden
    // But we can wrap the migration call
    public void migrateWithTiming(String source) {
        long start = System.nanoTime();
        migrate(source);
        long duration = (System.nanoTime() - start) / 1_000_000;
        System.out.println("Migration took " + duration + " ms");
    }
}
```

### Template Method with Generics

```java
abstract class GenericMigrator<T> {
    public final void migrate(String source) {
        T connection = connect(source);
        List<T> data = extract(connection);
        List<T> processed = process(data);
        int count = load(processed);
        System.out.println("Migrated " + count + " items");
        disconnect(connection);
    }

    protected abstract T connect(String source);
    protected abstract List<T> extract(T connection);
    protected List<T> process(List<T> data) { return data; } // hook
    protected abstract int load(List<T> data);
    protected abstract void disconnect(T connection);
}
```

## When to Use

1. **Framework Lifecycle Management**: JUnit's test lifecycle (`@BeforeAll`, `@BeforeEach`, `@Test`, `@AfterEach`,
   `@AfterAll`), Spring's `InitializingBean.afterPropertiesSet()`, Jakarta Servlet's `HttpServlet.doGet()/doPost()`.

2. **Build Pipelines**: CI/CD pipelines where the skeleton is "checkout → build → test → deploy" and each step varies by
   language/framework. Jenkins pipeline DSL is a Template Method.

3. **Data Processing Pipelines**: ETL jobs (Extract, Transform, Load) where the pipeline structure is fixed but data
   sources and transformation logic vary.

4. **Game AI**: Turn-based game skeleton "startTurn → drawCard → play → endTurn" with hooks for special abilities. The
   game engine calls the template; subclasses react to turns.

5. **Document Generators**: PDF, HTML, and Markdown report generators share a skeleton (header → content → footer) but
   implement each section differently.

### Framework Examples

- **JUnit 5**: `@Test` lifecycle is a Template Method. The framework calls `@BeforeEach`, `@Test`, `@AfterEach` in that
  order. Users provide the primitives.
- **Spring `AbstractApplicationContext.refresh()`**: The 13-step bean factory initialization is a Template Method with
  hooks like `postProcessBeanFactory()`.
- **Jakarta Servlet `HttpServlet.service()`**: Routes requests to `doGet()`, `doPost()`, etc. — subclasses override the
  methods they need.
- **Java `InputStream.read(byte[])`**: The `read(byte[])` method calls the abstract `read()` in a loop — a Template
  Method for buffered I/O.

## When NOT to Use

1. **Algorithm is Simple and Stable**: If the algorithm has only 2 steps and will never change, Template Method adds
   class hierarchy overhead. A simple method with lambda parameters is simpler.

2. **Subclasses Need Structural Control**: If subclasses need to change the algorithm structure (not just steps),
   Template Method is too restrictive. Use Strategy instead.

3. **Implementation is Shared Across Unrelated Classes**: Template Method uses inheritance. If the variant steps are
   needed across different class hierarchies, use Strategy (composition). Inheritance locks you into a single hierarchy.

4. **Excessive Hook Methods**: If every step is a hook with a default, the template method provides no value. The
   structure becomes invisible. Aim for 1-3 hooks maximum.

5. **When Composition is Preferable**: The "Favor composition over inheritance" principle suggests Strategy may be
   better. Template Method is appropriate when the skeleton is truly invariant and the steps are truly variant, AND the
   variant steps benefit from inheritance (access to protected fields).

## Interview Questions

### Q1: What is the Template Method pattern and the Hollywood Principle?

**Answer**: Template Method defines the skeleton of an algorithm in a base class, deferring specific steps to
subclasses. It embodies the Hollywood Principle: "Don't call us, we'll call you." The framework (base class) calls the
hooks; subclasses don't control the flow — they just provide implementations.

### Q2: How does Template Method differ from Strategy?

**Answer**: Template Method uses inheritance — the algorithm skeleton is in the abstract class, and subclasses override
steps. Strategy uses composition — the entire algorithm is encapsulated as a pluggable object. Template Method is "same
skeleton, different details." Strategy is "swap the whole algorithm." Template Method is static (compile-time), Strategy
is dynamic (runtime).

### Q3: What are hook methods and why are they useful?

**Answer**: Hook methods are optional overridable steps in the template. They have empty or sensible default
implementations. Hooks let subclasses inject behavior without being forced to implement every step. Example:
`postProcess()` in the data migrator — optional logging doesn't require every subclass to implement it.

### Q4: How does Template Method support the Open/Closed Principle?

**Answer**: The algorithm skeleton is closed for modification (marked `final`). The steps are open for extension (
subclasses provide implementations). You can add new behavior by creating new subclasses without modifying the existing
abstract class or other subclasses.

### Q5: When would you mark a template method as `final`?

**Answer**: Always mark the template method as `final` to prevent subclasses from changing the algorithm structure. If
subclasses can override the template method, it defeats the pattern's purpose. The structure is fixed; only the steps
vary.

### Q6: How is Template Method used in frameworks like Spring or JUnit?

**Answer**: Spring's `AbstractApplicationContext.refresh()` is a Template Method: it calls `obtainFreshBeanFactory()`,
`prepareBeanFactory()`, `postProcessBeanFactory()` (hook), `registerBeanPostProcessors()`, etc. JUnit 5's test execution
follows: `@BeforeAll` → `@BeforeEach` → `@Test` → `@AfterEach` → `@AfterAll`. The framework controls the sequence; users
annotate methods.

### Q7: What are the downsides of Template Method compared to Strategy?

**Answer**: Template Method is less flexible — you can't change the algorithm at runtime, and you can't compose
different step implementations from different sources (no multiple inheritance in Java). Strategy is more flexible but
requires more classes. Template Method is simpler when the skeleton is fixed and you want to reuse it across many
subclasses.

### Q8: How would you test a Template Method?

**Answer**: Test the concrete subclass implementations. For the abstract class, create a test-specific anonymous
subclass that implements only the abstract primitives and verify the template method calls them in the expected order.
Use mocking to verify hook methods are (or aren't) called.

### Follow-Up Question

**Interviewer**: "How would you refactor Template Method to use composition instead of inheritance?"

**Answer**: Replace each abstract step with a functional interface / strategy. The template method becomes a regular
method that accepts `Supplier`, `Consumer`, or `Function` for each step. The class delegates to these injected
strategies. This gives runtime flexibility and avoids inheritance. However, you lose the protected method access that
subclassing provides.

## Pros & Cons

### Advantages

- **Code Reuse**: Algorithm skeleton is defined once, shared by all subclasses
- **Consistent Structure**: All implementations follow the same sequence
- **Hook Methods**: Optional extension points without forcing implementation
- **Inversion of Control**: The base class controls the flow (Hollywood Principle)
- **Open/Closed Principle**: Add new behavior via new subclasses, not modification
- **Framework Foundation**: Enables framework design where users plug in behavior

### Disadvantages

- **Inheritance Constraint**: Subclasses must extend the abstract class (single inheritance limit in Java)
- **Rigid Structure**: Algorithm structure is fixed at compile time; can't change at runtime
- **Class Explosion**: Each variation requires a new subclass
- **Violates LSP Potential**: If subclasses override template method or change behavior in hooks unpredictably
- **Hard to Debug**: The control flow jumps between base class and subclass, making stack traces less intuitive

## Related Patterns

### Template Method vs Strategy

The classic comparison. **Template Method** (inheritance): fixed skeleton, variable steps, compile-time binding. *
*Strategy** (composition): variable algorithm, runtime binding. Use Template Method when the structure is stable and you
want to reuse it. Use Strategy when the entire algorithm must be swappable at runtime.

### Template Method vs Factory Method

**Factory Method** is often called from within a Template Method. Template Method defines the algorithm skeleton;
Factory Method provides the step that creates objects. For example, a framework might use a template method for document
generation and a factory method for creating the document format-specific objects.

### Template Method vs Builder

**Builder** focuses on step-by-step construction of a complex object. **Template Method** focuses on step-by-step
execution of an algorithm. Builder's director is a template method that calls builder steps. Template Method's steps are
overridden; Builder's steps are implemented by the builder interface.

## Key Takeaways

1. **"Skeleton algorithm, variable steps"** — The essence: fixed structure with overridable details.

2. **Hollywood Principle ("Don't call us, we'll call you")** — The base class drives the flow. This is the defining
   characteristic of framework design.

3. **Inheritance trade-off** — Template Method uses inheritance, which gives protected field access but limits
   flexibility. Compare with Strategy (composition).

4. **OCP friendly, but rigid** — Adding behavior via new subclasses is OCP-compliant. But you cannot restructure the
   algorithm without changing the base class.

5. **Interview memory aid** — "Template Method = algorithm skeleton, abstract steps + hooks, final template method,
   framework pattern."
