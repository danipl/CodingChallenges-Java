package com.danipl.platform.concurrency.flashsale;

import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.Objects.requireNonNull;

/**
 * Implementation of {@link FlashSale}.
 * <p>
 * Thread-safety: Per-ticket ReentrantLock avoids global contention. Volatile ticket status
 * enables lock-free reads. A global lock protects the reservation registry during
 * expiration sweeps.
 */
public final class FlashSaleImpl implements FlashSale {

    // === Fields ===
    private final Config config;
    private final Clock clock;

    private final AtomicInteger availableTickets;
    private final AtomicInteger soldTickets = new AtomicInteger(0);

    // Per-ticket state: each ticket has its own lock and volatile status
    private final Map<String, TicketEntry> tickets;

    // Reservation registry: reservationId → Reservation
    private final Map<String, Reservation> reservations;

    // Global lock for reservation registry mutations (confirm, cancel, expire sweep)
    private final ReentrantLock reservationLock = new ReentrantLock();

    // === Constructors ===

    public FlashSaleImpl(final Config config) {
        this(config, Clock.systemDefaultZone());
    }

    public FlashSaleImpl(final Config config, final Clock clock) {
        this.config = config;
        this.clock = clock;
        this.tickets = new ConcurrentHashMap<>(config.ticketIds().size());
        this.reservations = new ConcurrentHashMap<>();
        for (final String ticketId : config.ticketIds()) {
            this.tickets.put(ticketId, new TicketEntry(ticketId));
        }
        this.availableTickets = new AtomicInteger(this.tickets.size());
    }

    // === Public methods — ALL throw UnsupportedOperationException ===

    @Override
    public Reservation reserveTicket(final String ticketId, final String customerId) {
        requireNonNull(ticketId);
        requireNonNull(customerId);
        final TicketEntry ticket = this.tickets.get(ticketId);
        if (ticket == null) {
            throw new FlashSaleException("The ticket does not exist");
        }
        ticket.lock.lock();
        try {
            if (ticket.status != TicketStatus.AVAILABLE) {
                throw new FlashSaleException("The ticket is not available");
            }
            this.reservationLock.lock();
            try {
                final Reservation res = this.createReservation(ticket, customerId);
                ticket.currentReservationId = res.reservationId();
                ticket.status = TicketStatus.RESERVED;
                this.availableTickets.decrementAndGet();
                this.reservations.put(res.reservationId(), res);
                return res;
            } finally {
                this.reservationLock.unlock();
            }
        } finally {
            ticket.lock.unlock();
        }
    }

    @Override
    public boolean confirmPurchase(final String reservationId) {
        requireNonNull(reservationId);
        return processReservation(reservationId, (ticket, reservations) -> {
            if (ticket.status != TicketStatus.RESERVED) {
                throw new FlashSaleException("The ticket must be in reservation status");
            }
            final Reservation currRes = reservations.get(ticket.currentReservationId);
            if (currRes == null) {
                throw new FlashSaleException("The reservation does not exist");
            }
            ticket.currentReservationId = null;
            if (clock.instant().isAfter(currRes.expiresAt())) {
                ticket.status = TicketStatus.AVAILABLE;
                this.availableTickets.incrementAndGet();
            } else {
                ticket.status = TicketStatus.SOLD;
                this.soldTickets.incrementAndGet();
            }
            this.reservations.remove(reservationId);
            return ticket.status == TicketStatus.SOLD;
        });
    }

    @Override
    public boolean cancelReservation(final String reservationId) {
        requireNonNull(reservationId);
        return processReservation(reservationId, (ticket, reservations) -> {
            ticket.status = TicketStatus.AVAILABLE;
            ticket.currentReservationId = null;
            this.availableTickets.incrementAndGet();
            reservations.remove(reservationId);
            return true;
        });
    }

    @Override
    public TicketStatus getTicketStatus(final String ticketId) {
        requireNonNull(ticketId);
        final TicketEntry ticket = this.tickets.get(ticketId);
        if (ticket == null) {
            throw new FlashSaleException("The ticket does not exist");
        }
        return ticket.status;
    }

    @Override
    public int releaseExpiredReservations() {
        int expiredRes = 0;
        for (final Reservation res : this.reservations.values()) {
            if (this.clock.instant().isAfter(res.expiresAt())) {
                final TicketEntry ticket = this.tickets.get(res.ticketId());
                if (ticket == null) {
                    continue;
                }
                ticket.lock.lock();
                try {
                    this.reservationLock.lock();
                    try {
                        this.reservations.remove(res.reservationId());
                        ticket.status = TicketStatus.AVAILABLE;
                        ticket.currentReservationId = null;
                        this.availableTickets.incrementAndGet();
                        expiredRes++;
                    } finally {
                        this.reservationLock.unlock();
                    }
                } finally {
                    ticket.lock.unlock();
                }
            }
        }
        return expiredRes;
    }

    @Override
    public int totalTickets() {
        return this.tickets.size();
    }

    @Override
    public int availableTickets() {
        return this.availableTickets.get();
    }

    @Override
    public int reservedTickets() {
        return this.totalTickets() - this.availableTickets() - this.soldTickets();
    }

    @Override
    public int soldTickets() {
        return this.soldTickets.get();
    }

    private Reservation createReservation(final TicketEntry ticket, final String customerId) {
        return new Reservation(
                UUID.randomUUID().toString(),
                ticket.ticketId,
                customerId,
                this.clock.instant().plusMillis(this.config.reservationTimeoutMs())
        );
    }

    private boolean processReservation(final String reservationId, final ReservationLogicSupplier supplier) {
        final Reservation currRes = this.reservations.get(reservationId);
        if (currRes == null) {
            return false;
        }
        final TicketEntry ticket = this.tickets.get(currRes.ticketId());
        if (ticket == null) {
            this.reservations.remove(reservationId);
            return false;
        }
        try {
            ticket.lock.lock();
            this.reservationLock.lock();
            return supplier.process(ticket, this.reservations);
        } finally {
            ticket.lock.unlock();
            this.reservationLock.unlock();
        }
    }

    // === Internal types ===

    @FunctionalInterface
    interface ReservationLogicSupplier {
        boolean process(final TicketEntry ticket, final Map<String, Reservation> reservations);
    }

    /**
     * Per-ticket entry with its own lock and volatile status.
     * The volatile field ensures visibility across threads without lock acquisition for reads.
     */
    static final class TicketEntry {
        final String ticketId;
        final ReentrantLock lock = new ReentrantLock();
        volatile TicketStatus status = TicketStatus.AVAILABLE;
        volatile String currentReservationId; // null when AVAILABLE or SOLD

        TicketEntry(final String ticketId) {
            this.ticketId = ticketId;
        }
    }
}
