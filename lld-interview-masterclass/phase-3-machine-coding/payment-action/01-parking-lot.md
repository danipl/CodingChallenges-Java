# Parking Lot

> Multi-level parking lot with vehicle types, pricing strategies, and spot assignment.

## Requirements

- Multiple parking floors, each with different spot types
- Vehicle types: Motorcycle, Car, Bus (each needs different spot sizes)
- Pricing strategies: Hourly, Daily, Monthly, Free
- Spot assignment: First available, closest to entrance, preferred type
- Entry/exit with ticket generation

## Domain Model

```
ParkingLot
  ├── ParkingFloor[]
  │     ├── floorNumber: int
  │     ├── ParkingSpot[]
  │     │     ├── spotNumber: int
  │     │     ├── spotType: SpotType
  │     │     └── isOccupied: boolean
  │     └── displayBoard: DisplayBoard
  ├── EntryGate
  ├── ExitGate
  ├── Ticket
  └── PricingStrategy
```

## Key Patterns

### Strategy Pattern (Pricing)
```java
interface PricingStrategy {
    BigDecimal calculate(Ticket ticket);
}

class HourlyPricing implements PricingStrategy {
    private final BigDecimal ratePerHour;
    public BigDecimal calculate(Ticket ticket) {
        long hours = ceilHours(ticket.getEntryTime(), ticket.getExitTime());
        return ratePerHour.multiply(BigDecimal.valueOf(hours));
    }
}

class DailyPricing implements PricingStrategy { /* flat daily rate */ }
class MonthlyPricing implements PricingStrategy { /* subscription */ }
```

### Factory Pattern (Vehicle/Spot Creation)
```java
class VehicleFactory {
    static Vehicle create(String type, String plate) {
        return switch (type) {
            case "MOTORCYCLE" -> new Motorcycle(plate);
            case "CAR" -> new Car(plate);
            case "BUS" -> new Bus(plate);
            default -> throw new IllegalArgumentException();
        };
    }
}
```

## Core Implementation

```java
enum SpotType { MOTORCYCLE, COMPACT, LARGE, HANDICAPPED }
enum VehicleType { MOTORCYCLE, CAR, BUS }

abstract class Vehicle {
    final String licensePlate;
    final VehicleType type;
    Vehicle(String plate, VehicleType type) { this.licensePlate = plate; this.type = type; }
    abstract int getSpotSizeNeeded();
}

class ParkingSpot {
    private final int spotNumber;
    private final SpotType type;
    private boolean isOccupied;
    private Vehicle parkedVehicle;

    boolean canFit(Vehicle v) {
        return !isOccupied && type.ordinal() >= v.getSpotSizeNeeded();
    }

    void park(Vehicle v) { this.parkedVehicle = v; this.isOccupied = true; }
    void leave() { this.parkedVehicle = null; this.isOccupied = false; }
}

class ParkingLot {
    private final List<ParkingFloor> floors;
    private PricingStrategy pricingStrategy;

    synchronized Ticket enter(Vehicle vehicle) {
        ParkingSpot spot = findAvailableSpot(vehicle);
        if (spot == null) throw new LotFullException();
        spot.park(vehicle);
        return new Ticket(vehicle, spot, Instant.now());
    }

    synchronized BigDecimal exit(Ticket ticket) {
        ticket.setExitTime(Instant.now());
        BigDecimal amount = pricingStrategy.calculate(ticket);
        ticket.getSpot().leave();
        return amount;
    }

    private ParkingSpot findAvailableSpot(Vehicle v) {
        for (ParkingFloor floor : floors) {
            ParkingSpot spot = floor.findSpot(v);
            if (spot != null) return spot;
        }
        return null;
    }
}
```

## Interview Tips

1. **Spot hierarchy**: Motorcycle spots can fit motorcycles; Large spots can fit anything
2. **Concurrency**: `synchronized` on enter/exit — multiple gates operate simultaneously
3. **Pricing flexibility**: Strategy pattern makes adding new pricing trivial
4. **Edge cases**: Vehicle leaves without ticket, lost ticket handling, overflow parking
