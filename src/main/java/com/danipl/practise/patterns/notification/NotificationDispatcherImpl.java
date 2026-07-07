package com.danipl.practise.patterns.notification;

import java.util.Map;
import java.util.function.Function;

/**
 * Implementation of {@link NotificationDispatcher}.
 *
 * <p>
 * Use the Strategy pattern: each {@link NotificationDispatcher.Channel} should
 * have its own
 * formatting/delivery strategy, selected at dispatch time — no branching
 * chains.
 */
public final class NotificationDispatcherImpl implements NotificationDispatcher {

    // This should be located at its own class file, under public scope.
    private Function<Notification, DispatchReceipt> EMAIL_DISPATCHET = (notification) -> {
        return dispatcherCreatorHelper(
                notification,
                String.format("To: %s\nSubject: Notification\n\n%s", notification.recipient(), notification.body()));
    };

    private Function<Notification, DispatchReceipt> SMS_DISPATCHET = (notification) -> {
        return dispatcherCreatorHelper(
                notification, String.format("[SMS to %s]: %s", notification.recipient(),
                        notification.body().substring(0, Math.min(notification.body().length(), 160))));
    };

    private Function<Notification, DispatchReceipt> PUSH_DISPATCHET = (notification) -> {
        return dispatcherCreatorHelper(
                notification, String.format("{{\"token\":\"%s\",\"message\":\"%s\"}}", notification.recipient(),
                        notification.body()));
    };

    private final Map<Channel, Function<Notification, DispatchReceipt>> CHANNEL_FORMATTER_MAP = Map.of(
            Channel.EMAIL, EMAIL_DISPATCHET,
            Channel.SMS, SMS_DISPATCHET,
            Channel.PUSH, PUSH_DISPATCHET);

    @Override
    public DispatchReceipt dispatch(final Notification notification) {
        validate(notification);
        return CHANNEL_FORMATTER_MAP.get(notification.channel()).apply(notification);
    }

    private void validate(final Notification notification) {
        if (notification == null) {
            throw new IllegalArgumentException("Notification cannot be empty");
        }
        if (notification.channel() == null) {
            throw new IllegalArgumentException("Notification channel cannot be empty");
        }
        if (!CHANNEL_FORMATTER_MAP.containsKey(notification.channel())) {
            throw new IllegalArgumentException("The channel provided is not implemented");
        }
        if (notification.recipient() == null) {
            throw new IllegalArgumentException("Notification recipient cannot be empty");
        }
        if (notification.recipient().isBlank()) {
            throw new IllegalArgumentException("Notification recipient cannot be empty");
        }
        if (notification.body() == null) {
            throw new IllegalArgumentException("Notification body required");
        }
    }

    private static DispatchReceipt dispatcherCreatorHelper(final Notification notification, final String formatted) {
        return new DispatchReceipt(notification.channel(), notification.recipient(), formatted);
    }

}
