# Design Patterns Masterclass

> Comprehensive guide to software design patterns for technical interviews and production code.

## Overview

Design patterns are proven solutions to commonly occurring problems in software engineering. They represent best
practices evolved over time by experienced software architects.

This masterclass covers **22 essential design patterns** organized into three categories, with complete Java
implementations, interview preparation materials, and real-world use cases.

## Why Design Patterns Matter

### For Technical Interviews

- **Common vocabulary**: Demonstrate mastery of industry-standard terminology
- **Problem-solving framework**: Show structured thinking for complex design problems
- **SOLID principles**: Connect patterns to fundamental design principles
- **Trade-off analysis**: Articulate pros/cons of different approaches

### For Production Code

- **Maintainability**: Code that's easier to understand and modify
- **Scalability**: Designs that grow with requirements
- **Reusability**: Proven solutions that reduce reinvention
- **Communication**: Shared language for team collaboration

## Pattern Categories

### 1. Creational Patterns (5 patterns)

Deal with object creation mechanisms, trying to create objects in a manner suitable to the situation.

| Pattern                                          | Core Idea                                                                               | Interview Priority |
|--------------------------------------------------|-----------------------------------------------------------------------------------------|--------------------|
| [Factory Method](creational/factory-method/)     | Define interface for creating objects, let subclasses decide which class to instantiate | ⭐⭐⭐⭐⭐              |
| [Abstract Factory](creational/abstract-factory/) | Create families of related objects without specifying concrete classes                  | ⭐⭐⭐⭐               |
| [Singleton](creational/singleton/)               | Ensure a class has only one instance, provide global access point                       | ⭐⭐⭐⭐⭐              |
| [Builder](creational/builder/)                   | Construct complex objects step by step, separate construction from representation       | ⭐⭐⭐⭐⭐              |
| [Prototype](creational/prototype/)               | Create new objects by cloning a prototypical instance                                   | ⭐⭐⭐                |

**When to use creational patterns:**

- Object creation logic becomes complex
- Need to instantiate objects at runtime based on conditions
- Want to decouple object creation from usage
- Need control over object instantiation (e.g., limiting instances)

---

### 2. Structural Patterns (7 patterns)

Concerned with object composition and relationships between entities.

| Pattern                            | Core Idea                                                                      | Interview Priority |
|------------------------------------|--------------------------------------------------------------------------------|--------------------|
| [Adapter](structural/adapter/)     | Convert interface of a class into another interface clients expect             | ⭐⭐⭐⭐⭐              |
| [Decorator](structural/decorator/) | Add responsibilities to objects dynamically, alternative to subclassing        | ⭐⭐⭐⭐⭐              |
| [Facade](structural/facade/)       | Provide unified interface to a set of interfaces in a subsystem                | ⭐⭐⭐⭐               |
| [Proxy](structural/proxy/)         | Provide surrogate or placeholder for another object to control access          | ⭐⭐⭐⭐⭐              |
| [Composite](structural/composite/) | Compose objects into tree structures, treat individual and composite uniformly | ⭐⭐⭐⭐               |
| [Bridge](structural/bridge/)       | Separate abstraction from implementation, vary them independently              | ⭐⭐⭐                |
| [Flyweight](structural/flyweight/) | Share objects to support large numbers efficiently, use memory sharing         | ⭐⭐                 |

**When to use structural patterns:**

- Need to work with incompatible interfaces
- Want to add behavior without modifying existing code
- Need to simplify complex subsystems
- Working with tree hierarchies or object graphs
- Memory optimization for many similar objects

---

### 3. Behavioral Patterns (10 patterns)

Deal with communication between objects, algorithms, and assignment of responsibilities.

| Pattern                                                        | Core Idea                                                                | Interview Priority |
|----------------------------------------------------------------|--------------------------------------------------------------------------|--------------------|
| [Strategy](behavioral/strategy/)                               | Define family of algorithms, encapsulate each, make them interchangeable | ⭐⭐⭐⭐⭐              |
| [Observer](behavioral/observer/)                               | Define one-to-many dependency, notify dependents of state changes        | ⭐⭐⭐⭐⭐              |
| [State](behavioral/state/)                                     | Allow object to alter behavior when internal state changes               | ⭐⭐⭐⭐⭐              |
| [Command](behavioral/command/)                                 | Encapsulate request as object, parameterize clients with queues/logging  | ⭐⭐⭐⭐               |
| [Template Method](behavioral/template-method/)                 | Define skeleton of algorithm, let subclasses fill in specific steps      | ⭐⭐⭐⭐               |
| [Chain of Responsibility](behavioral/chain-of-responsibility/) | Pass request along chain of handlers, let each decide to process or pass | ⭐⭐⭐⭐               |
| [Iterator](behavioral/iterator/)                               | Provide way to access elements of aggregate object sequentially          | ⭐⭐⭐                |
| [Mediator](behavioral/mediator/)                               | Define object that encapsulates how set of objects interact              | ⭐⭐⭐                |
| [Memento](behavioral/memento/)                                 | Capture and externalize object state, allow restoration later            | ⭐⭐⭐                |
| [Visitor](behavioral/visitor/)                                 | Represent operation to be performed on elements of object structure      | ⭐⭐                 |

**When to use behavioral patterns:**

- Want to decouple objects that communicate
- Need to change behavior at runtime
- Algorithms vary across objects
- Need to support undo/redo operations
- Want to avoid tight coupling between components

---

## How to Use This Guide

### For Interview Preparation

1. **Start with high-priority patterns** (⭐⭐⭐⭐⭐):
    - Factory Method, Abstract Factory
    - Singleton (know thread-safety!)
    - Builder
    - Strategy, Observer, State
    - Adapter, Decorator, Proxy

2. **For each pattern, master:**
    - **Problem**: What problem does it solve?
    - **Solution**: How does it solve it?
    - **Implementation**: Can you code it from scratch?
    - **Trade-offs**: When to use vs when not to use?
    - **Related patterns**: How does it compare to alternatives?

3. **Practice explaining:**
    - Give a 30-second elevator pitch
    - Draw UML diagrams on whiteboard
    - Write code examples
    - Discuss real-world applications

### For Production Code

1. **Identify the problem first** - Don't force patterns where they don't fit
2. **Start simple** - Use patterns only when complexity justifies them
3. **Consider alternatives** - Sometimes simpler solutions work better
4. **Document decisions** - Explain why you chose a particular pattern

---

## SOLID Principles Connection

All design patterns embody SOLID principles. Understanding these connections helps you choose and apply patterns
correctly.

| Principle                     | Patterns That Embody It                               |
|-------------------------------|-------------------------------------------------------|
| **S** - Single Responsibility | Factory Method, Builder, Facade, Command              |
| **O** - Open/Closed           | Strategy, Decorator, Observer, State, Template Method |
| **L** - Liskov Substitution   | Factory Method, Template Method, Strategy             |
| **I** - Interface Segregation | Adapter, Facade, Proxy                                |
| **D** - Dependency Inversion  | Factory Method, Abstract Factory, Strategy, Observer  |

---

## Pattern Relationships

### Commonly Confused Patterns

**Adapter vs Decorator vs Proxy:**

- **Adapter**: Changes interface (incompatible → compatible)
- **Decorator**: Adds behavior (enhances without changing interface)
- **Proxy**: Controls access (same interface, different access logic)

**Strategy vs State:**

- **Strategy**: Client chooses algorithm, strategies are independent
- **State**: Object transitions between states, states know about each other

**Factory Method vs Abstract Factory:**

- **Factory Method**: Creates one product, uses inheritance
- **Abstract Factory**: Creates families of products, uses composition

**Template Method vs Strategy:**

- **Template Method**: Uses inheritance, algorithm skeleton in base class
- **Strategy**: Uses composition, algorithms encapsulated in separate classes

---

## Quick Reference by Use Case

| Use Case                                      | Recommended Patterns             |
|-----------------------------------------------|----------------------------------|
| Create objects without specifying exact class | Factory Method, Abstract Factory |
| Ensure only one instance exists               | Singleton                        |
| Build complex objects step-by-step            | Builder                          |
| Copy/clone objects                            | Prototype                        |
| Make incompatible interfaces work together    | Adapter                          |
| Add behavior dynamically                      | Decorator                        |
| Simplify complex subsystem                    | Facade                           |
| Control access to object                      | Proxy                            |
| Work with tree structures                     | Composite                        |
| Separate abstraction from implementation      | Bridge                           |
| Optimize memory for many similar objects      | Flyweight                        |
| Switch algorithms at runtime                  | Strategy                         |
| Notify objects of state changes               | Observer                         |
| Change behavior based on state                | State                            |
| Encapsulate requests/commands                 | Command                          |
| Define algorithm skeleton                     | Template Method                  |
| Pass request through chain                    | Chain of Responsibility          |
| Iterate over collection                       | Iterator                         |
| Centralize complex communications             | Mediator                         |
| Save and restore object state                 | Memento                          |
| Add operations to object structure            | Visitor                          |

---

## Interview Tips

### Common Questions

1. **"What are design patterns?"**
    - Proven solutions to common software design problems
    - Not cookie-cutter solutions, but templates for solving recurring issues
    - Based on SOLID principles and collective developer experience

2. **"Why use design patterns?"**
    - Reusable, tested solutions
    - Common vocabulary for teams
    - Promote maintainability and scalability
    - Avoid reinventing the wheel

3. **"How do you choose the right pattern?"**
    - Identify the problem first
    - Consider trade-offs and constraints
    - Start simple, add patterns only when justified
    - Know related patterns and their differences

4. **"Can you give an example of when you used [pattern]?"**
    - Prepare 2-3 real examples from your experience
    - Explain the problem, why you chose that pattern, and the outcome
    - Be ready to discuss alternatives you considered

### Red Flags to Avoid

❌ **Over-engineering**: Don't use patterns where simple solutions work
❌ **Pattern obsession**: Don't force patterns into every design
❌ **Ignoring trade-offs**: Every pattern has costs (complexity, performance)
❌ **Not knowing alternatives**: Interviewers will ask "why this pattern vs X?"

### Green Flags to Show

✅ **Problem-first thinking**: Start with the problem, then patterns
✅ **Trade-off awareness**: Discuss pros AND cons
✅ **Real examples**: Connect patterns to actual codebases
✅ **SOLID connections**: Show how patterns embody design principles

---

## Java-Specific Notes

### Modern Java Features (17+)

- **Records**: Use for immutable data carriers in patterns like DTO, Value Object
- **Sealed classes**: Perfect for State pattern, finite state machines
- **Pattern matching**: Simplifies type checks in Visitor, Strategy
- **Functional interfaces**: Strategy, Command can use lambdas
- **Switch expressions**: Clean state transitions in State pattern

### Thread Safety

- **Singleton**: Double-checked locking, enum singleton, or Bill Pugh method
- **Observer**: Thread-safe notification mechanisms
- **State**: Synchronized state transitions
- **Builder**: Thread-safe builders for concurrent construction

### Java APIs Using Patterns

- **java.util.Collections**: Factory methods, Decorator (synchronizedList, unmodifiableList)
- **java.io**: Decorator pattern (BufferedReader, InputStreamReader)
- **java.util.Iterator**: Iterator pattern
- **javax.servlet.Filter**: Chain of Responsibility
- **Spring Framework**: Factory, Proxy, Template Method, Observer

---

## Learning Path

### Beginner (Week 1-2)

1. Study Factory Method and Singleton
2. Understand Strategy and Observer
3. Practice implementing from scratch
4. Find examples in codebases you know

### Intermediate (Week 3-4)

1. Master Builder and Abstract Factory
2. Learn Adapter, Decorator, Proxy
3. Understand State vs Strategy distinction
4. Practice explaining patterns aloud

### Advanced (Week 5-6)

1. Study remaining patterns
2. Understand pattern relationships and combinations
3. Practice system design with patterns
4. Mock interview questions

---

## Additional Resources

### Books

- **Design Patterns: Elements of Reusable Object-Oriented Software** (Gang of Four)
- **Head First Design Patterns** (Freeman & Freeman)
- **Effective Java** (Joshua Bloch) - Item 1: Consider static factory methods

### Online

- Refactoring Guru: https://refactoring.guru/design-patterns
- Source Making: https://sourcemaking.com/design_patterns
- Java Design Patterns: https://java-design-patterns.github.io/

### Practice

- Implement patterns from scratch
- Identify patterns in open-source projects
- Refactor existing code to use patterns
- Mock interview with pattern questions

---

## Contributing

Each tutorial follows a consistent structure:

1. Overview and problem statement
2. Detailed solution explanation
3. Complete Java implementation
4. When to use / when not to use
5. Interview questions and answers
6. Pros, cons, and related patterns

All code examples are:

- Syntactically correct Java 17+
- Follow Java naming conventions
- Include package declarations
- Demonstrate best practices
- Ready for interview discussions

---

## License

Educational content for interview preparation and learning.

---

**Happy pattern learning! 🎯**

Remember: Patterns are tools, not rules. Use them wisely to write better, more maintainable code.
