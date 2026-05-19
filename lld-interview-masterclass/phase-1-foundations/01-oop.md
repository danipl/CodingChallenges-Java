# 1.1 Object-Oriented Programming (OOP)

> Foundation of all LLD. Understand the four pillars and — critically — the trade-offs between inheritance and composition.

## The Four Pillars

### 1. Encapsulation
- Bundle data + behavior that operates on that data
- Hide internal state, expose controlled interfaces
- **Interview signal**: Can you design a class that prevents invalid states?

### 2. Abstraction
- Expose what an object does, hide how it does it
- Use interfaces/abstract classes to define contracts
- **Interview signal**: Can you separate interface from implementation?

### 3. Polymorphism
- Same interface, different implementations
- Runtime (dynamic dispatch) vs compile-time (generics/overloading)
- **Interview signal**: Can you swap implementations without changing client code?

### 4. Inheritance
- "Is-a" relationship — subclass inherits parent behavior
- **Danger**: Tight coupling, fragile base class problem, deep hierarchies

## Inheritance vs Composition: The Critical Trade-Off

| Aspect | Inheritance | Composition |
|--------|-------------|-------------|
| Relationship | "Is-a" | "Has-a" |
| Coupling | Tight (compile-time) | Loose (runtime) |
| Flexibility | Fixed at compile-time | Swappable at runtime |
| Testing | Harder (parent dependencies) | Easier (mock components) |
| Reuse | White-box (see internals) | Black-box (use interface) |

### Rule of Thumb
**Prefer composition over inheritance** unless there's a genuine "is-a" relationship AND you control the base class.

### When Inheritance IS Appropriate
- Framework base classes (e.g., `HttpServlet`)
- Template Method pattern (algorithm skeleton with hooks)
- When you genuinely model a type hierarchy (e.g., `Shape → Circle`)

### Composition Example (Preferred)

```java
// Instead of: class Car extends Engine (wrong!)
class Car {
    private Engine engine;       // Has-a
    private Transmission trans;  // Has-a
    private List<Wheel> wheels;  // Has-a

    public Car(Engine engine, Transmission trans, List<Wheel> wheels) {
        this.engine = engine;
        this.trans = trans;
        this.wheels = wheels;
    }
}
```

## Common Interview Pitfalls

1. **Deep inheritance hierarchies** (>3 levels = smell)
2. **Inheriting for code reuse** (use composition + delegation)
3. **God objects** (classes doing everything — violate SRP)
4. **Anemic domain models** (classes with only getters/setters, no behavior)

## Practice Questions

1. Design a `Shape` hierarchy. When does inheritance break? How would composition fix it?
2. Refactor a `Manager extends Employee` that also needs `Mentor` and `ProjectLead` capabilities.
3. Why does `java.util.Properties extends Hashtable<Object,Object>` violate LSP?
