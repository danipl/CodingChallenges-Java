package com.danipl.platform.concurrency.flashsale;

import java.time.Instant;
import java.util.UUID;

/**
 * A thread-safe Flash Sale system for mass ticket sales.
 * <p>
 * Simulates a high-concurrency ticket reservation scenario where thousands of buyers
 * compete for a limited inventory of tickets. The system must prevent double-selling
 * while allowing temporary reservations that expire if not confirmed in time.
 * <p>
 * Key behaviors:
 *   - Reserve a ticket for a customer (holds it for a configurable timeout)
 *   - Confirm a reservation (finalizes the purchase)
 *   - Cancel a reservation (releases the ticket back to available pool)
 *   - Automatically expire stale reservations (cleanup pass)
 *   - Query ticket status without blocking (lock-free reads via volatile)
 * <p>
 * Thread-safety contract: All methods are safe under concurrent access from multiple threads.
 * Per-ticket locking is used to avoid global contention. Status reads are lock-free.
 */
public interface FlashSale {

    /**
     * Reserves a ticket for a customer.
     * <p>
     * If the ticket is AVAILABLE, it transitions to RESERVED and a reservation is created
     * with an expiration time. If the ticket is already RESERVED or SOLD, the operation fails.
     *
     * @param ticketId   the unique identifier of the ticket to reserve
     * @param customerId the unique identifier of the customer requesting the ticket
     * @return a Reservation record with details of the hold
     * @throws FlashSaleException if the ticket does not exist, is already reserved, or is sold
     */
    Reservation reserveTicket(String ticketId, String customerId);

    /**
     * Confirms a pending reservation, finalizing the purchase.
     * <p>
     * The ticket transitions from RESERVED to SOLD. The reservation is consumed.
     * If the reservation has expired, it cannot be confirmed and the ticket returns to AVAILABLE.
     *
     * @param reservationId the unique identifier of the reservation to confirm
     * @return true if the purchase was confirmed, false if the reservation was expired or not found
     */
    boolean confirmPurchase(String reservationId);

    /**
     * Cancels an active reservation, releasing the ticket back to the available pool.
     * <p>
     * The ticket transitions from RESERVED back to AVAILABLE.
     *
     * @param reservationId the unique identifier of the reservation to cancel
     * @return true if the reservation was cancelled, false if not found or already expired
     */
    boolean cancelReservation(String reservationId);

    /**
     * Returns the current status of a ticket.
     * <p>
     * This is a lock-free read operation. The returned status reflects the most recent
     * visible state but may be stale by the time the caller acts on it (TOCTOU).
     *
     * @param ticketId the unique identifier of the ticket
     * @return the current TicketStatus
     * @throws FlashSaleException if the ticket does not exist
     */
    TicketStatus getTicketStatus(String ticketId);

    /**
     * Scans all reservations and releases those that have expired.
     * <p>
     * An expired reservation is one whose expiration time is before the current time.
     * Expired reservations are cancelled and their tickets return to AVAILABLE.
     *
     * @return the number of reservations that were expired and released
     */
    int releaseExpiredReservations();

    /**
     * Returns the total number of tickets in the flash sale.
     */
    int totalTickets();

    /**
     * Returns the number of tickets currently available (not reserved or sold).
     */
    int availableTickets();

    /**
     * Returns the number of tickets currently reserved (held but not confirmed).
     */
    int reservedTickets();

    /**
     * Returns the number of tickets that have been sold (confirmed purchases).
     */
    int soldTickets();

    // === Nested types ===

    /**
     * Configuration for the flash sale.
     *
     * @param ticketIds            the set of ticket identifiers to initialize
     * @param reservationTimeoutMs how long a reservation remains valid before expiring
     */
    record Config(java.util.List<String> ticketIds, long reservationTimeoutMs) {
        public Config {
            if (ticketIds == null || ticketIds.isEmpty()) {
                throw new IllegalArgumentException("ticketIds must not be empty");
            }
            if (reservationTimeoutMs < 1) {
                throw new IllegalArgumentException("reservationTimeoutMs must be >= 1");
            }
            ticketIds = java.util.List.copyOf(ticketIds);
        }
    }

    /**
     * Status of a ticket in the flash sale.
     */
    enum TicketStatus {
        AVAILABLE,
        RESERVED,
        SOLD
    }

    /**
     * A reservation holding a ticket for a customer.
     *
     * @param reservationId unique identifier for this reservation
     * @param ticketId      the ticket being held
     * @param customerId    the customer who reserved it
     * @param expiresAt     the instant at which this reservation expires
     */
    record Reservation(String reservationId, String ticketId, String customerId, Instant expiresAt) {
        public Reservation {
            if (reservationId == null || reservationId.isBlank()) {
                throw new IllegalArgumentException("reservationId must not be blank");
            }
            if (ticketId == null || ticketId.isBlank()) {
                throw new IllegalArgumentException("ticketId must not be blank");
            }
            if (customerId == null || customerId.isBlank()) {
                throw new IllegalArgumentException("customerId must not be blank");
            }
            if (expiresAt == null) {
                throw new IllegalArgumentException("expiresAt must not be null");
            }
        }
    }

    /**
     * Exception thrown for flash sale operation failures.
     */
    class FlashSaleException extends RuntimeException {
        public FlashSaleException(String message) {
            super(message);
        }

        public FlashSaleException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Creates a new FlashSale instance with the given configuration.
     *
     * @param config the configuration parameters
     * @return a new FlashSale instance
     */
    static FlashSale of(Config config) {
        return new FlashSaleImpl(config);
    }
}
