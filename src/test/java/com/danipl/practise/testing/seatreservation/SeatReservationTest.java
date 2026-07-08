package com.danipl.practise.testing.seatreservation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SeatReservation")
class SeatReservationTest {

    private final static String FIRST_SEAT = "A1";
    private final static String SECOND_SEAT = "B2";
    private final static String THIRD_SEAT = "C3";

    private SeatReservation reservation;

    @BeforeEach
    void setUp() {
        reservation = SeatReservation.of();
    }

    @Nested
    @DisplayName("reserve")
    class Reserve {

        @Test
        @DisplayName("should reserve a valid seat")
        void reserveValidSeat() {
            reservation.reserve(FIRST_SEAT);
            assertTrue(reservation.isReserved(FIRST_SEAT), "Seat is not reserved");
        }

        @Test
        @DisplayName("should reject null seat ID")
        void reserveNull() {
            final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> reservation.reserve(null), "It is not throwing an IllegalArgumentException");
            assertEquals(ex.getMessage(), "Seat ID must not be null or blank");
        }

        @Test
        @DisplayName("should reject blank seat ID")
        void reserveBlank() {
            final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> reservation.reserve(""), "It is not throwing an IllegalArgumentException");
            assertEquals(ex.getMessage(), "Seat ID must not be null or blank");
        }

        @Test
        @DisplayName("should reject duplicate reservation")
        void reserveDuplicate() {
            reservation.reserve(FIRST_SEAT);
            final IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> reservation.reserve(FIRST_SEAT),
                    "It is not throwing an IllegalStateException");
            assertEquals(ex.getMessage(), "Seat already reserved: " + FIRST_SEAT);
        }
    }

    @Nested
    @DisplayName("release")
    class Release {

        @Test
        @DisplayName("should release a reserved seat")
        void releaseReserved() {
            reservation.reserve(FIRST_SEAT);
            assertEquals(1, reservation.reservedCount(), "It is not 1");
            reservation.release(FIRST_SEAT);
            assertEquals(0, reservation.reservedCount(), "It is not 0");
        }

        @Test
        @DisplayName("should reject release of non-reserved seat")
        void releaseNotReserved() {
            final IllegalStateException ex = assertThrows(IllegalStateException.class, () -> reservation.release(FIRST_SEAT),
                    "It is not throwing an IllegalStateException");
            assertEquals(ex.getMessage(), "Seat is not reserved: " + FIRST_SEAT);
        }
    }

    @Nested
    @DisplayName("isReserved")
    class IsReserved {

        @Test
        @DisplayName("should return true for reserved seat")
        void isReservedTrue() {
            reservation.reserve(FIRST_SEAT);
            assertTrue(reservation.isReserved(FIRST_SEAT), "The seat is not reserved");
        }

        @Test
        @DisplayName("should return false for non-reserved seat")
        void isReservedFalse() {
            assertFalse(reservation.isReserved(FIRST_SEAT), "It returns true");
        }

        @Test
        @DisplayName("should return false for null")
        void isReservedNull() {
            assertFalse(reservation.isReserved(null), "It returns true");
        }

        @Test
        @DisplayName("should return false for blank")
        void isReservedBlank() {
            assertFalse(reservation.isReserved(""), "It returns true");
        }
    }

    @Nested
    @DisplayName("reservedCount")
    class ReservedCount {

        @Test
        @DisplayName("should return 0 when empty")
        void countEmpty() {
            assertEquals(0, reservation.reservedCount(), "It is not 0");
        }

        @Test
        @DisplayName("should track count through reserve and release")
        void countAfterOperations() {
            reservation.reserve(FIRST_SEAT);
            assertEquals(1, reservation.reservedCount(), "It is not 1");
            reservation.reserve(SECOND_SEAT);
            reservation.reserve(THIRD_SEAT);
            assertEquals(3, reservation.reservedCount(), "It is not 3");
            reservation.release(FIRST_SEAT);
            assertEquals(2, reservation.reservedCount(), "It is not 2");
        }
    }
}
