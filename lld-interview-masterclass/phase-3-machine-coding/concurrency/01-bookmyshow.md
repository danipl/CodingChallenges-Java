# BookMyShow — Movie Ticket Booking System

> Concurrency-heavy system: handle simultaneous bookings, seat locks, and payment processing.

## Requirements

- Browse movies, theaters, and showtimes
- Select seats and book tickets
- Handle concurrent bookings (two users selecting same seat)
- Payment integration with timeout
- Seat locking mechanism (hold seats for 10 min during payment)

## Domain Model

```
BookingSystem
  ├── City[]
  │     ├── Theater[]
  │     │     ├── Screen[]
  │     │     │     ├── Seat[][] (2D grid)
  │     │     │     └── Show
  │     │     │           ├── movie: Movie
  │     │     │           ├── startTime: LocalDateTime
  │     │     │           └── seatStatus: Map<Seat, SeatStatus>
  │     │     └── location: Address
  │     └── movies: List<Movie>
  └── Booking
        ├── user: User
        ├── show: Show
        ├── seats: List<Seat>
        ├── status: BookingStatus
        └── payment: Payment
```

## Concurrency Challenges

### Seat Locking with Timeout
```java
class SeatLockProvider {
    private final Map<Show, Map<Seat, LockInfo>> locks = new ConcurrentHashMap<>();
    private static final Duration LOCK_DURATION = Duration.ofMinutes(10);

    synchronized boolean lock(Show show, List<Seat> seats, String user) {
        for (Seat seat : seats) {
            if (isLocked(show, seat)) return false;  // Already locked
        }
        for (Seat seat : seats) {
            locks.computeIfAbsent(show, k -> new ConcurrentHashMap<>())
                 .put(seat, new LockInfo(user, Instant.now()));
        }
        return true;
    }

    synchronized void unlock(Show show, List<Seat> seats, String user) {
        Map<Seat, LockInfo> showLocks = locks.get(show);
        if (showLocks != null) {
            seats.forEach(s -> showLocks.remove(s));
        }
    }

    boolean isLocked(Show show, Seat seat) {
        Map<Seat, LockInfo> showLocks = locks.get(show);
        if (showLocks == null) return false;
        LockInfo info = showLocks.get(seat);
        if (info == null) return false;
        if (Instant.now().isAfter(info.lockedAt.plus(LOCK_DURATION))) {
            showLocks.remove(seat);  // Expired
            return false;
        }
        return true;
    }
}
```

### Booking Flow (Thread-Safe)
```java
class BookingService {
    private final SeatLockProvider lockProvider;
    private final PaymentGateway paymentGateway;

    Booking bookSeats(User user, Show show, List<Seat> seats) {
        // 1. Lock seats
        if (!lockProvider.lock(show, seats, user.getId())) {
            throw new SeatsNotAvailableException();
        }

        try {
            // 2. Create booking
            Booking booking = new Booking(user, show, seats);

            // 3. Process payment
            PaymentResult result = paymentGateway.process(booking.getTotal());
            if (!result.isSuccess()) {
                throw new PaymentFailedException();
            }

            // 4. Confirm booking
            booking.confirm();
            show.markSeatsBooked(seats);
            return booking;
        } catch (Exception e) {
            lockProvider.unlock(show, seats, user.getId());  // Release on failure
            throw e;
        }
    }
}
```

## Key Patterns

### Observer (Notifications)
```java
class BookingNotifier implements Observer {
    public void update(Event event) {
        if (event.getType() == BOOKING_CONFIRMED) {
            sendEmail(event.getBooking());
            sendSMS(event.getBooking());
        }
    }
}
```

### Strategy (Pricing)
```java
interface PricingStrategy {
    BigDecimal calculate(Seat seat, Show show);
}

class WeekendPricing implements PricingStrategy { /* 1.5x */ }
class HolidayPricing implements PricingStrategy { /* 2x */ }
class VIPSeatPricing implements PricingStrategy { /* premium */ }
```

## Interview Tips

1. **Concurrency is the focus** — locks, timeouts, race conditions
2. **Lock expiry** — critical! Users who don't pay must release seats
3. **Idempotency** — same booking request shouldn't double-charge
4. **DB indexes**: (show_id, seat_id) for fast seat lookup
5. **Optimistic vs Pessimistic locking** — discuss trade-offs
