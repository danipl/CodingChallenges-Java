package com.danipl.practise.testing.seatreservation;

import java.util.HashSet;
import java.util.Set;

public final class SeatReservationImpl implements SeatReservation {

    private final Set<String> reserved = new HashSet<>();

    @Override
    public void reserve(final String seatId) {
        if (seatId == null || seatId.isBlank()) {
            throw new IllegalArgumentException("Seat ID must not be null or blank");
        }
        if (!reserved.add(seatId)) {
            throw new IllegalStateException("Seat already reserved: " + seatId);
        }
    }

    @Override
    public void release(final String seatId) {
        if (seatId == null || seatId.isBlank()) {
            throw new IllegalArgumentException("Seat ID must not be null or blank");
        }
        if (!reserved.remove(seatId)) {
            throw new IllegalStateException("Seat is not reserved: " + seatId);
        }
    }

    @Override
    public boolean isReserved(final String seatId) {
        if (seatId == null || seatId.isBlank()) {
            return false;
        }
        return reserved.contains(seatId);
    }

    @Override
    public int reservedCount() {
        return reserved.size();
    }
}
