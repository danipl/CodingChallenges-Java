package com.danipl.practise.patterns.notification;

/**
 * Implementation of {@link NotificationDispatcher}.
 *
 * <p>Use the Strategy pattern: each {@link NotificationDispatcher.Channel} should have its own
 * formatting/delivery strategy, selected at dispatch time — no branching chains.
 */
public final class NotificationDispatcherImpl implements NotificationDispatcher {

    @Override
    public DispatchReceipt dispatch(final Notification notification) {
        throw new UnsupportedOperationException("Implement this method");
    }
}
