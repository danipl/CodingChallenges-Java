package com.danipl.practise.patterns.notification;

/**
 * Dispatches notifications through the appropriate channel strategy.
 *
 * <p>Requirements:
 * <ul>
 *   <li>Support EMAIL, SMS, and PUSH channels</li>
 *   <li>Each channel formats and delivers differently</li>
 *   <li>Fail fast on invalid input</li>
 * </ul>
 */
public interface NotificationDispatcher {

    /**
     * Factory method to create a default implementation.
     */
    static NotificationDispatcher of() {
        return new NotificationDispatcherImpl();
    }

    /**
     * Dispatches a notification through the channel specified in the notification.
     *
     * @param notification the notification to dispatch
     * @return a receipt confirming delivery details
     * @throws IllegalArgumentException if notification is null or has invalid fields
     */
    DispatchReceipt dispatch(Notification notification);

    /**
     * The supported delivery channels.
     */
    enum Channel {
        EMAIL, SMS, PUSH
    }

    /**
     * An immutable notification request.
     *
     * @param channel   the delivery channel
     * @param recipient the target address/token/phone
     * @param body      the message content
     */
    record Notification(Channel channel, String recipient, String body) {
    }

    /**
     * Proof that a notification was dispatched.
     *
     * @param channel   the channel used
     * @param recipient the target
     * @param formatted the channel-specific formatted message that was "sent"
     */
    record DispatchReceipt(Channel channel, String recipient, String formatted) {
    }
}
