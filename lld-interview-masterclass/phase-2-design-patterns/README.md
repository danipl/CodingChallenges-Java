# Phase 2: Design Patterns

> Don't learn superficial examples. For every pattern ask: Why? Where in real systems? How?

## Modules

### Must-Know Patterns (solve 60-70% of interview problems)

| Pattern | Key Use Case | Link |
|---------|-------------|------|
| **Strategy** | Swappable algorithms (payments, sorting, routing) | [Strategy](./must-know/01-strategy.md) |
| **Iterator** | Traversing collections without exposing structure | [Iterator](./must-know/02-iterator.md) |
| **Builder** | Complex object construction with validation | [Builder](./must-know/03-builder.md) |
| **Factory** | Object creation without specifying concrete class | [Factory](./must-know/04-factory.md) |
| **Adapter** | Making incompatible interfaces work together | [Adapter](./must-know/05-adapter.md) |
| **Observer** | Pub-sub, event systems, frontend state | [Observer](./must-know/06-observer.md) |
| **Chain of Responsibility** | Request processing pipelines | [Chain of Responsibility](./must-know/07-chain-of-responsibility.md) |
| **State** | State-driven behavior (elevator, ATM) | [State](./must-know/08-state.md) |

### Secondary Patterns

| Pattern | Key Use Case | Link |
|---------|-------------|------|
| Singleton | Single instance (config, connection pool) | [Singleton](./secondary/01-singleton.md) |
| Decorator | Adding behavior without subclassing | [Decorator](./secondary/02-decorator.md) |
| Proxy | Controlled access, lazy loading, caching | [Proxy](./secondary/03-proxy.md) |
| Criteria | Nested filters, query builders | [Criteria](./secondary/04-criteria.md) |

## Pattern Selection Guide

| Problem Signal | Likely Pattern |
|----------------|----------------|
| "Multiple ways to do X" | Strategy |
| "Too many constructor params" | Builder |
| "Create objects based on input" | Factory |
| "Notify multiple listeners" | Observer |
| "Process request through steps" | Chain of Responsibility |
| "Behavior changes with state" | State |
| "Walk through collection" | Iterator |
| "Incompatible interfaces" | Adapter |
| "Add features dynamically" | Decorator |
| "Control access to object" | Proxy |
| "Complex nested filters" | Criteria |

## Study Order

```
must-know/01-strategy.md (MOST IMPORTANT)
    → must-know/03-builder.md
    → must-know/04-factory.md
    → must-know/06-observer.md
    → remaining must-know patterns
    → secondary patterns (as needed)
```

> Strategy pattern alone solves 60-70% of common interview problems. Master it first.
