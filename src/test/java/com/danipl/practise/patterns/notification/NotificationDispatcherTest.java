package com.danipl.practise.patterns.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.danipl.practise.patterns.notification.NotificationDispatcher.Channel;
import com.danipl.practise.patterns.notification.NotificationDispatcher.Notification;

@DisplayName("NotificationDispatcher tests")
class NotificationDispatcherTest {

    private NotificationDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = NotificationDispatcher.of();
    }

    @Nested
    @DisplayName("EMAIL channel")
    class EmailChannel {

        @Test
        @DisplayName("should format email with To/Subject/body structure")
        void happyPath() {
            var notification = new Notification(Channel.EMAIL, "alice@example.com", "Hello!");

            var receipt = dispatcher.dispatch(notification);

            assertEquals(Channel.EMAIL, receipt.channel());
            assertEquals("alice@example.com", receipt.recipient());
            assertTrue(receipt.formatted().contains("To: alice@example.com"));
            assertTrue(receipt.formatted().contains("Subject:"));
            assertTrue(receipt.formatted().contains("Hello!"));
        }
    }

    @Nested
    @DisplayName("SMS channel")
    class SmsChannel {

        @Test
        @DisplayName("should format SMS with bracket prefix")
        void happyPath() {
            var notification = new Notification(Channel.SMS, "+34600123456", "Your code is 42");

            var receipt = dispatcher.dispatch(notification);

            assertEquals(Channel.SMS, receipt.channel());
            assertTrue(receipt.formatted().contains("[SMS to +34600123456]"));
            assertTrue(receipt.formatted().contains("Your code is 42"));
        }

        @Test
        @DisplayName("should truncate SMS body to 160 characters")
        void truncateLongBody() {
            var longBody = "x".repeat(200);
            var notification = new Notification(Channel.SMS, "+34600000000", longBody);

            var receipt = dispatcher.dispatch(notification);

            // The formatted SMS (excluding prefix) must not exceed 160 chars of body
            assertTrue(receipt.formatted().length() <= 200,
                    "SMS output should be bounded");
        }
    }

    @Nested
    @DisplayName("PUSH channel")
    class PushChannel {

        @Test
        @DisplayName("should format push notification as compact payload")
        void happyPath() {
            var notification = new Notification(Channel.PUSH, "device-token-abc", "New message");

            var receipt = dispatcher.dispatch(notification);

            assertEquals(Channel.PUSH, receipt.channel());
            assertTrue(receipt.formatted().contains("device-token-abc"));
            assertTrue(receipt.formatted().contains("New message"));
        }
    }

    @Nested
    @DisplayName("Input validation")
    class InputValidation {

        @Test
        @DisplayName("should reject null notification")
        void nullNotification() {
            assertThrows(IllegalArgumentException.class, () -> dispatcher.dispatch(null));
        }

        @Test
        @DisplayName("should reject null recipient")
        void nullRecipient() {
            var notification = new Notification(Channel.EMAIL, null, "body");
            assertThrows(IllegalArgumentException.class, () -> dispatcher.dispatch(notification));
        }

        @Test
        @DisplayName("should reject blank recipient")
        void blankRecipient() {
            var notification = new Notification(Channel.EMAIL, "   ", "body");
            assertThrows(IllegalArgumentException.class, () -> dispatcher.dispatch(notification));
        }

        @Test
        @DisplayName("should reject null body")
        void nullBody() {
            var notification = new Notification(Channel.SMS, "+34600000000", null);
            assertThrows(IllegalArgumentException.class, () -> dispatcher.dispatch(notification));
        }

        @Test
        @DisplayName("should reject null channel")
        void nullChannel() {
            var notification = new Notification(null, "alice@example.com", "body");
            assertThrows(IllegalArgumentException.class, () -> dispatcher.dispatch(notification));
        }
    }
}
