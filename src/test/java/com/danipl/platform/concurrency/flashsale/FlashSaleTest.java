package com.danipl.platform.concurrency.flashsale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FlashSale tests")
class FlashSaleTest {

    private static final Instant BASE_TIME = Instant.parse("2025-06-18T10:00:00Z");
    private static final long TIMEOUT_MS = 5_000;

    private FlashSale flashSale;
    private Clock fakeClock;

    @BeforeEach
    void setUp() {
        fakeClock = Clock.fixed(BASE_TIME, ZoneId.of("UTC"));
        flashSale = new FlashSaleImpl(new FlashSale.Config(
                List.of("T1", "T2", "T3", "T4", "T5"),
                TIMEOUT_MS
        ), fakeClock);
    }

    // === Helper to create a FlashSale with a controllable clock ===

    private FlashSale withClock(Clock clock) {
        return new FlashSaleImpl(new FlashSale.Config(
                List.of("T1", "T2", "T3", "T4", "T5"),
                TIMEOUT_MS
        ), clock);
    }

    @Nested
    @DisplayName("Basic behavior")
    class BasicBehavior {

        @Test
        @DisplayName("initial state: all tickets are AVAILABLE")
        void initialStateAllAvailable() {
            // Given / When / Then
            assertEquals(5, flashSale.totalTickets());
            assertEquals(5, flashSale.availableTickets());
            assertEquals(0, flashSale.reservedTickets());
            assertEquals(0, flashSale.soldTickets());
            for (String id : List.of("T1", "T2", "T3", "T4", "T5")) {
                assertEquals(FlashSale.TicketStatus.AVAILABLE, flashSale.getTicketStatus(id));
            }
        }

        @Test
        @DisplayName("reserve a ticket transitions it to RESERVED")
        void reserveTicketTransitionsToReserved() {
            // Given
            // When
            FlashSale.Reservation res = flashSale.reserveTicket("T1", "customer-A");

            // Then
            assertNotNull(res.reservationId());
            assertEquals("T1", res.ticketId());
            assertEquals("customer-A", res.customerId());
            assertEquals(BASE_TIME.plusMillis(TIMEOUT_MS), res.expiresAt());
            assertEquals(FlashSale.TicketStatus.RESERVED, flashSale.getTicketStatus("T1"));
            assertEquals(4, flashSale.availableTickets());
            assertEquals(1, flashSale.reservedTickets());
        }

        @Test
        @DisplayName("confirm purchase transitions ticket to SOLD")
        void confirmPurchaseTransitionsToSold() {
            // Given
            FlashSale.Reservation res = flashSale.reserveTicket("T2", "customer-B");

            // When
            boolean confirmed = flashSale.confirmPurchase(res.reservationId());

            // Then
            assertTrue(confirmed);
            assertEquals(FlashSale.TicketStatus.SOLD, flashSale.getTicketStatus("T2"));
            assertEquals(1, flashSale.soldTickets());
            assertEquals(4, flashSale.availableTickets());
        }

        @Test
        @DisplayName("cancel reservation returns ticket to AVAILABLE")
        void cancelReservationReturnsToAvailable() {
            // Given
            FlashSale.Reservation res = flashSale.reserveTicket("T3", "customer-C");

            // When
            boolean cancelled = flashSale.cancelReservation(res.reservationId());

            // Then
            assertTrue(cancelled);
            assertEquals(FlashSale.TicketStatus.AVAILABLE, flashSale.getTicketStatus("T3"));
            assertEquals(5, flashSale.availableTickets());
            assertEquals(0, flashSale.reservedTickets());
        }

        @Test
        @DisplayName("full lifecycle: reserve → confirm → sold permanently")
        void fullLifecycleReserveConfirm() {
            // Given
            FlashSale.Reservation res = flashSale.reserveTicket("T4", "customer-D");

            // When
            assertTrue(flashSale.confirmPurchase(res.reservationId()));

            // Then - ticket is permanently sold, cannot be reserved again
            assertEquals(FlashSale.TicketStatus.SOLD, flashSale.getTicketStatus("T4"));
            assertThrows(FlashSale.FlashSaleException.class,
                    () -> flashSale.reserveTicket("T4", "customer-E"));
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("cannot reserve same ticket twice")
        void cannotReserveSameTicketTwice() {
            // Given
            flashSale.reserveTicket("T1", "customer-A");

            // When / Then
            assertThrows(FlashSale.FlashSaleException.class,
                    () -> flashSale.reserveTicket("T1", "customer-B"));
        }

        @Test
        @DisplayName("cannot reserve non-existent ticket")
        void cannotReserveNonExistentTicket() {
            assertThrows(FlashSale.FlashSaleException.class,
                    () -> flashSale.reserveTicket("NON_EXISTENT", "customer-A"));
        }

        @Test
        @DisplayName("cannot confirm non-existent reservation")
        void cannotConfirmNonExistentReservation() {
            assertFalse(flashSale.confirmPurchase("fake-reservation-id"));
        }

        @Test
        @DisplayName("cannot cancel non-existent reservation")
        void cannotCancelNonExistentReservation() {
            assertFalse(flashSale.cancelReservation("fake-reservation-id"));
        }

        @Test
        @DisplayName("cannot confirm same reservation twice")
        void cannotConfirmSameReservationTwice() {
            // Given
            FlashSale.Reservation res = flashSale.reserveTicket("T1", "customer-A");
            assertTrue(flashSale.confirmPurchase(res.reservationId()));

            // When / Then - second confirm fails (reservation already consumed)
            assertFalse(flashSale.confirmPurchase(res.reservationId()));
        }

        @Test
        @DisplayName("config validation rejects empty ticket list")
        void configValidationRejectsEmptyTickets() {
            assertThrows(IllegalArgumentException.class,
                    () -> new FlashSale.Config(List.of(), TIMEOUT_MS));
        }

        @Test
        @DisplayName("config validation rejects zero timeout")
        void configValidationRejectsZeroTimeout() {
            assertThrows(IllegalArgumentException.class,
                    () -> new FlashSale.Config(List.of("T1"), 0));
        }

        @Test
        @DisplayName("getTicketStatus throws for unknown ticket")
        void getTicketStatusThrowsForUnknown() {
            assertThrows(FlashSale.FlashSaleException.class,
                    () -> flashSale.getTicketStatus("UNKNOWN"));
        }
    }

    @Nested
    @DisplayName("Expiration")
    class Expiration {

        @Test
        @DisplayName("releaseExpiredReservations returns expired tickets to AVAILABLE")
        void releaseExpiredReturnsToAvailable() {
            // Given - we need a FlashSale with a clock we can advance.
            // Since our setUp uses a fixed clock, we test via the impl's internals.
            // For this test, we use a very short timeout and rely on real time.
            FlashSale shortLived = FlashSale.of(new FlashSale.Config(
                    List.of("T1", "T2"), 50 // 50ms timeout
            ));
            shortLived.reserveTicket("T1", "customer-A");
            assertEquals(FlashSale.TicketStatus.RESERVED, shortLived.getTicketStatus("T1"));

            // When - wait for expiration
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            int expired = shortLived.releaseExpiredReservations();

            // Then
            assertEquals(1, expired);
            assertEquals(FlashSale.TicketStatus.AVAILABLE, shortLived.getTicketStatus("T1"));
            assertEquals(2, shortLived.availableTickets());
        }

        @Test
        @DisplayName("confirming an expired reservation fails")
        void confirmExpiredReservationFails() {
            // Given
            FlashSale shortLived = FlashSale.of(new FlashSale.Config(
                    List.of("T1"), 50
            ));
            FlashSale.Reservation res = shortLived.reserveTicket("T1", "customer-A");

            // When - wait for expiration
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            shortLived.releaseExpiredReservations();

            // Then
            assertFalse(shortLived.confirmPurchase(res.reservationId()));
            assertEquals(FlashSale.TicketStatus.AVAILABLE, shortLived.getTicketStatus("T1"));
        }

        @Test
        @DisplayName("non-expired reservations are not released")
        void nonExpiredReservationsNotReleased() {
            // Given
            flashSale.reserveTicket("T1", "customer-A");

            // When - no time has passed (fixed clock)
            int expired = flashSale.releaseExpiredReservations();

            // Then
            assertEquals(0, expired);
            assertEquals(FlashSale.TicketStatus.RESERVED, flashSale.getTicketStatus("T1"));
        }
    }

    @Nested
    @DisplayName("Thread safety")
    class ThreadSafety {

        @Test
        @DisplayName("concurrent reservations for same ticket: only one wins")
        void concurrentReservationsSameTicketOnlyOneWins() throws InterruptedException {
            int threadCount = 50;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);

            for (int t = 0; t < threadCount; t++) {
                final String customerId = "customer-" + t;
                executor.submit(() -> {
                    try {
                        start.await();
                        flashSale.reserveTicket("T1", customerId);
                        successCount.incrementAndGet();
                    } catch (FlashSale.FlashSaleException e) {
                        failCount.incrementAndGet();
                    } catch (InterruptedException ignored) {
                    } finally {
                        done.countDown();
                    }
                });
            }

            start.countDown();
            assertTrue(done.await(30, TimeUnit.SECONDS), "All threads should complete");
            executor.shutdownNow();

            // Exactly one thread should succeed
            assertEquals(1, successCount.get(), "Only one reservation should succeed");
            assertEquals(threadCount - 1, failCount.get(), "All others should fail");
            assertEquals(FlashSale.TicketStatus.RESERVED, flashSale.getTicketStatus("T1"));
        }

        @Test
        @DisplayName("concurrent reservations for different tickets: all succeed")
        void concurrentReservationsDifferentTicketsAllSucceed() throws InterruptedException {
            int threadCount = 5;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);

            String[] ticketIds = {"T1", "T2", "T3", "T4", "T5"};

            for (int t = 0; t < threadCount; t++) {
                final String customerId = "customer-" + t;
                final String ticketId = ticketIds[t];
                executor.submit(() -> {
                    try {
                        start.await();
                        flashSale.reserveTicket(ticketId, customerId);
                        successCount.incrementAndGet();
                    } catch (Exception ignored) {
                    } finally {
                        done.countDown();
                    }
                });
            }

            start.countDown();
            assertTrue(done.await(30, TimeUnit.SECONDS), "All threads should complete");
            executor.shutdownNow();

            assertEquals(5, successCount.get(), "All reservations should succeed");
            assertEquals(0, flashSale.availableTickets());
            assertEquals(5, flashSale.reservedTickets());
        }

        @Test
        @DisplayName("concurrent reserve and confirm operations are consistent")
        void concurrentReserveAndConfirmConsistent() throws InterruptedException {
            int threadCount = 20;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threadCount);
            AtomicInteger confirmedCount = new AtomicInteger(0);

            // Each thread: reserve a unique ticket (we have 5 tickets, 20 threads → 4 per ticket)
            // Only first reserve per ticket wins, then confirm it
            for (int t = 0; t < threadCount; t++) {
                final String customerId = "customer-" + t;
                final String ticketId = "T" + ((t % 5) + 1);
                executor.submit(() -> {
                    try {
                        start.await();
                        FlashSale.Reservation res = flashSale.reserveTicket(ticketId, customerId);
                        if (flashSale.confirmPurchase(res.reservationId())) {
                            confirmedCount.incrementAndGet();
                        }
                    } catch (Exception ignored) {
                    } finally {
                        done.countDown();
                    }
                });
            }

            start.countDown();
            assertTrue(done.await(30, TimeUnit.SECONDS), "All threads should complete");
            executor.shutdownNow();

            // Exactly 5 tickets should be sold (one per ticket)
            assertEquals(5, flashSale.soldTickets(), "All 5 tickets should be sold");
            assertEquals(0, flashSale.availableTickets());
            assertEquals(0, flashSale.reservedTickets());
        }

        @Test
        @DisplayName("high contention: 100 threads compete for 1 ticket")
        void highContention100Threads1Ticket() throws InterruptedException {
            FlashSale singleTicket = FlashSale.of(new FlashSale.Config(
                    List.of("SOLO"), TIMEOUT_MS
            ));
            int threadCount = 100;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);

            for (int t = 0; t < threadCount; t++) {
                final String customerId = "customer-" + t;
                executor.submit(() -> {
                    try {
                        start.await();
                        singleTicket.reserveTicket("SOLO", customerId);
                        successCount.incrementAndGet();
                    } catch (Exception ignored) {
                    } finally {
                        done.countDown();
                    }
                });
            }

            start.countDown();
            assertTrue(done.await(30, TimeUnit.SECONDS), "All threads should complete");
            executor.shutdownNow();

            assertEquals(1, successCount.get(), "Exactly one thread should win the ticket");
            assertEquals(FlashSale.TicketStatus.RESERVED, singleTicket.getTicketStatus("SOLO"));
        }
    }
}
