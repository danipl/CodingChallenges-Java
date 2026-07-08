package com.danipl.practise.testing.seatreservation;

/**
 * Manages seat reservations for a venue.
 *
 * <p>Seats are identified by string IDs (e.g. "A1", "B12").
 * The component tracks which seats are currently reserved and
 * enforces valid state transitions.</p>
 *
 * Requirements:
 *   - A seat must exist (non-null, non-blank ID) to be reserved.
 *   - A seat cannot be reserved twice.
 *   - A seat cannot be released if it was never reserved.
 */
public interface SeatReservation {

    /**
     * Factory method to create a default implementation.
     */
    static SeatReservation of() {
        return new SeatReservationImpl();
    }

    /**
     * Reserves the given seat.
     *
     * @param seatId the seat identifier (e.g. "A1", "B12")
     * @throws IllegalArgumentException if {@code seatId} is null or blank
     * @throws IllegalStateException    if the seat is already reserved
     */
    void reserve(String seatId);

    /**
     * Releases a previously reserved seat.
     *
     * @param seatId the seat identifier
     * @throws IllegalArgumentException if {@code seatId} is null or blank
     * @throws IllegalStateException    if the seat is not currently reserved
     */
    void release(String seatId);

    /**
     * Checks whether a seat is currently reserved.
     *
     * @param seatId the seat identifier
     * @return {@code true} if reserved, {@code false} otherwise (including null/blank)
     */
    boolean isReserved(String seatId);

    /**
     * Returns the total number of currently reserved seats.
     *
     * @return reserved seat count (never negative)
     */
    int reservedCount();
}
