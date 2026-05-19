# Elevator System

> State-driven system with concurrent requests, scheduling, and multi-elevator coordination.

## Requirements

- N elevators, M floors
- Each elevator has capacity limit
- Requests from inside (floor buttons) and outside (up/down buttons)
- Efficient scheduling (minimize wait time)
- Handle concurrent requests safely

## Domain Model

```
ElevatorSystem
  ├── Elevator[]
  │     ├── currentState: ElevatorState
  │     ├── currentFloor: int
  │     ├── direction: Direction
  │     ├── requests: PriorityQueue<FloorRequest>
  │     └── capacity: int
  ├── Floor
  │     ├── floorNumber: int
  │     ├── upButton: boolean
  │     └── downButton: boolean
  └── ElevatorController (scheduling logic)
```

## Key Patterns

### State Pattern (Elevator States)
```
IDLE → MOVING_UP → MOVING_DOWN → DOOR_OPEN → DOOR_CLOSED → MAINTENANCE
```

### Observer Pattern
- Floors observe elevator state changes
- Display panels update when elevator arrives

### Strategy Pattern (Scheduling)
```java
interface SchedulingStrategy {
    Elevator assignElevator(List<Elevator> elevators, int floor, Direction dir);
}

class NElevators implements SchedulingStrategy { /* nearest elevator */ }
class LookStrategy implements SchedulingStrategy { /* LOOK algorithm */ }
```

## Core Implementation

```java
enum Direction { UP, DOWN, IDLE }

enum ElevatorState { IDLE, MOVING, DOOR_OPEN, MAINTENANCE }

class FloorRequest implements Comparable<FloorRequest> {
    final int floor;
    final Direction direction;
    final long timestamp;

    @Override
    public int compareTo(FloorRequest o) {
        return Long.compare(this.timestamp, o.timestamp);
    }
}

class Elevator {
    private final int id;
    private final int capacity;
    private int currentFloor;
    private Direction direction = Direction.IDLE;
    private ElevatorState state = ElevatorState.IDLE;
    private final Set<Integer> internalRequests = new TreeSet<>();
    private int currentLoad = 0;

    synchronized void addRequest(int floor) {
        if (currentLoad < capacity) {
            internalRequests.add(floor);
            currentLoad++;
        }
    }

    synchronized void processNextFloor() {
        if (internalRequests.isEmpty()) {
            state = ElevatorState.IDLE;
            direction = Direction.IDLE;
            return;
        }
        int nextFloor = direction == Direction.UP
            ? internalRequests.first()
            : internalRequests.last();
        moveTo(nextFloor);
    }

    private void moveTo(int floor) {
        state = ElevatorState.MOVING;
        direction = floor > currentFloor ? Direction.UP : Direction.DOWN;
        // Simulate movement
        currentFloor = floor;
        internalRequests.remove(floor);
        state = ElevatorState.DOOR_OPEN;
        // After door close timer
        state = ElevatorState.IDLE;
    }
}
```

## Interview Tips

1. **Start simple**: Single elevator, basic up/down
2. **Add scheduling**: LOOK algorithm (elevator continues in current direction until no more requests)
3. **Handle concurrency**: `synchronized` on shared state
4. **Edge cases**: Capacity overflow, same floor request, maintenance mode
