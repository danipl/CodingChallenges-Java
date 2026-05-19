# Phase 3: Machine Coding Problems

> Practice 10-20 core problems 2-3 times each. Identify which patterns fit into broader solutions.

## Problem Categories

### State-Driven Systems

| Problem | Key Pattern | Link |
|---------|-------------|------|
| Elevator System | State, Observer | [Elevator](./state-driven/01-elevator.md) |
| ATM Machine | State, Chain of Responsibility | [ATM](./state-driven/02-atm.md) |

### Payment & Action Systems

| Problem | Key Pattern | Link |
|---------|-------------|------|
| Parking Lot | Strategy, Factory | [Parking Lot](./payment-action/01-parking-lot.md) |
| Vending Machine | State, Strategy | [Vending Machine](./infrastructure/04-vending-machine.md) |

### Concurrency & Data Platforms

| Problem | Key Pattern | Link |
|---------|-------------|------|
| BookMyShow | Concurrency, Locks | [BookMyShow](./concurrency/01-bookmyshow.md) |
| E-Commerce Platform | CQRS, Criteria, Observer | [E-Commerce](./concurrency/02-ecommerce.md) |

### Infrastructure

| Problem | Key Pattern | Link |
|---------|-------------|------|
| Key-Value Store | Proxy, Decorator | [KV Store](./infrastructure/01-kv-store.md) |
| Distributed Queue | Observer, Iterator | [Distributed Queue](./infrastructure/02-distributed-queue.md) |
| Logger Framework | Chain of Responsibility | [Logger](./infrastructure/03-logger.md) |

### Games

| Problem | Key Pattern | Link |
|---------|-------------|------|
| Tic-Tac-Toe | State, Strategy | [Tic-Tac-Toe](./games/01-tic-tac-toe.md) |
| Chess | State, Observer | [Chess](./games/02-chess.md) |
| Card Game (Poker/Blackjack) | State, Strategy | [Card Game](./games/03-card-game.md) |

## Practice Strategy

### Iteration 1: Basic Implementation
- Focus on core domain model
- Get the system working end-to-end
- Don't worry about patterns yet

### Iteration 2: Pattern Integration
- Identify where patterns naturally fit
- Refactor to use Strategy/State/Observer
- Improve extensibility

### Iteration 3: Production Quality
- Add concurrency handling
- Add error handling and edge cases
- Add basic tests
- Time yourself: 45-60 minutes

## Interview Timeline (45-60 min)

```
0-5 min:  Clarify requirements, identify entities
5-10 min: Design domain model (classes, relationships)
10-15 min: Identify patterns, discuss trade-offs
15-40 min: Code core implementation
40-50 min: Add edge cases, concurrency
50-60 min: Test, explain design decisions
```
