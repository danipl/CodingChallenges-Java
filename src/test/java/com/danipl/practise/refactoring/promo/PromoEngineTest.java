package com.danipl.practise.refactoring.promo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PromoEngine Tests")
class PromoEngineTest {

    private PromoEngine engine;
    private final LocalDate evalDate = LocalDate.of(2026, 7, 6);

    @BeforeEach
    void setUp() {
        engine = PromoEngine.of();
    }

    @Nested
    @DisplayName("Validation and Inputs")
    class ValidationAndInputs {

        @Test
        @DisplayName("should throw exception when cart is null")
        void shouldThrowExceptionWhenCartIsNull() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    engine.applyPromo(null, "WELCOME10", evalDate)
            );
            assertEquals("Cart cannot be null", ex.getMessage());
        }

        @Test
        @DisplayName("should throw exception when promo code is null")
        void shouldThrowExceptionWhenCodeIsNull() {
            PromoEngine.Customer customer = new PromoEngine.Customer("C1", evalDate, false, null);
            PromoEngine.Cart cart = new PromoEngine.Cart(customer, List.of(), 5.0);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    engine.applyPromo(cart, null, evalDate)
            );
            assertEquals("Promo code cannot be empty", ex.getMessage());
        }

        @Test
        @DisplayName("should throw exception when promo code is empty")
        void shouldThrowExceptionWhenCodeIsEmpty() {
            PromoEngine.Customer customer = new PromoEngine.Customer("C1", evalDate, false, null);
            PromoEngine.Cart cart = new PromoEngine.Cart(customer, List.of(), 5.0);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    engine.applyPromo(cart, "   ", evalDate)
            );
            assertEquals("Promo code cannot be empty", ex.getMessage());
        }

        @Test
        @DisplayName("should return invalid result for unrecognized promo code")
        void shouldReturnInvalidResultForUnrecognizedCode() {
            PromoEngine.Customer customer = new PromoEngine.Customer("C1", evalDate, false, null);
            PromoEngine.Cart cart = new PromoEngine.Cart(customer, List.of(), 5.0);

            PromoEngine.DiscountResult result = engine.applyPromo(cart, "INVALID_CODE", evalDate);

            assertFalse(result.isValid());
            assertEquals(0.0, result.discountAmount());
            assertEquals("Invalid code: INVALID_CODE", result.message());
        }
    }

    @Nested
    @DisplayName("WELCOME10 Promo Code")
    class Welcome10Promo {

        @Test
        @DisplayName("should apply 10% discount for new customers registered within 30 days")
        void shouldApplyDiscountForNewCustomer() {
            // Registered today (0 days ago)
            PromoEngine.Customer customer = new PromoEngine.Customer("C1", evalDate, false, null);
            PromoEngine.CartItem item = new PromoEngine.CartItem("P1", "BOOKS", 100.0, 1);
            PromoEngine.Cart cart = new PromoEngine.Cart(customer, List.of(item), 5.0);

            PromoEngine.DiscountResult result = engine.applyPromo(cart, "WELCOME10", evalDate);

            assertTrue(result.isValid());
            assertEquals(10.0, result.discountAmount());
            assertEquals("WELCOME10", result.appliedCode());
        }

        @Test
        @DisplayName("should apply discount for customer registered exactly 30 days ago")
        void shouldApplyDiscountForBoundaryCustomer() {
            LocalDate regDate = evalDate.minusDays(30);
            PromoEngine.Customer customer = new PromoEngine.Customer("C1", regDate, false, null);
            PromoEngine.CartItem item = new PromoEngine.CartItem("P1", "BOOKS", 80.0, 1);
            PromoEngine.Cart cart = new PromoEngine.Cart(customer, List.of(item), 5.0);

            PromoEngine.DiscountResult result = engine.applyPromo(cart, "WELCOME10", evalDate);

            assertTrue(result.isValid());
            assertEquals(8.0, result.discountAmount());
        }

        @Test
        @DisplayName("should reject welcome promo for customer registered 31 days ago")
        void shouldRejectOldCustomer() {
            LocalDate regDate = evalDate.minusDays(31);
            PromoEngine.Customer customer = new PromoEngine.Customer("C1", regDate, false, null);
            PromoEngine.CartItem item = new PromoEngine.CartItem("P1", "BOOKS", 80.0, 1);
            PromoEngine.Cart cart = new PromoEngine.Cart(customer, List.of(item), 5.0);

            PromoEngine.DiscountResult result = engine.applyPromo(cart, "WELCOME10", evalDate);

            assertFalse(result.isValid());
            assertEquals(0.0, result.discountAmount());
            assertEquals("Welcome promo is only for new customers registered within 30 days", result.message());
        }

        @Test
        @DisplayName("should cap welcome discount at $20")
        void shouldCapWelcomeDiscount() {
            PromoEngine.Customer customer = new PromoEngine.Customer("C1", evalDate, false, null);
            PromoEngine.CartItem item = new PromoEngine.CartItem("P1", "BOOKS", 300.0, 1);
            PromoEngine.Cart cart = new PromoEngine.Cart(customer, List.of(item), 5.0);

            PromoEngine.DiscountResult result = engine.applyPromo(cart, "WELCOME10", evalDate);

            assertTrue(result.isValid());
            assertEquals(20.0, result.discountAmount());
        }

        @Test
        @DisplayName("should reject welcome promo if cart total is under $50")
        void shouldRejectLowCartTotal() {
            PromoEngine.Customer customer = new PromoEngine.Customer("C1", evalDate, false, null);
            PromoEngine.CartItem item = new PromoEngine.CartItem("P1", "BOOKS", 49.99, 1);
            PromoEngine.Cart cart = new PromoEngine.Cart(customer, List.of(item), 5.0);

            PromoEngine.DiscountResult result = engine.applyPromo(cart, "WELCOME10", evalDate);

            assertFalse(result.isValid());
            assertEquals(0.0, result.discountAmount());
            assertEquals("Cart total must be at least $50.00", result.message());
        }
    }

    @Nested
    @DisplayName("BOGO_ELECTRONICS Promo Code")
    class BogoElectronicsPromo {

        @Test
        @DisplayName("should apply buy one get one free for electronics if customer is loyal")
        void shouldApplyBogoForLoyalCustomer() {
            PromoEngine.Customer customer = new PromoEngine.Customer("C1", evalDate.minusYears(1), true, null);
            PromoEngine.CartItem item1 = new PromoEngine.CartItem("P1", "ELECTRONICS", 100.0, 2); // 1 free
            PromoEngine.CartItem item2 = new PromoEngine.CartItem("P2", "ELECTRONICS", 50.0, 3);  // 1 free
            PromoEngine.CartItem item3 = new PromoEngine.CartItem("P3", "BOOKS", 20.0, 2);        // no discount
            PromoEngine.Cart cart = new PromoEngine.Cart(customer, List.of(item1, item2, item3), 5.0);

            PromoEngine.DiscountResult result = engine.applyPromo(cart, "BOGO_ELECTRONICS", evalDate);

            assertTrue(result.isValid());
            assertEquals(150.0, result.discountAmount()); // 100.0 + 50.0
        }

        @Test
        @DisplayName("should reject BOGO if customer is not a loyal member")
        void shouldRejectNonLoyalCustomer() {
            PromoEngine.Customer customer = new PromoEngine.Customer("C1", evalDate.minusYears(1), false, null);
            PromoEngine.CartItem item = new PromoEngine.CartItem("P1", "ELECTRONICS", 100.0, 2);
            PromoEngine.Cart cart = new PromoEngine.Cart(customer, List.of(item), 5.0);

            PromoEngine.DiscountResult result = engine.applyPromo(cart, "BOGO_ELECTRONICS", evalDate);

            assertFalse(result.isValid());
            assertEquals(0.0, result.discountAmount());
            assertEquals("BOGO Electronics is only available to loyal members", result.message());
        }

        @Test
        @DisplayName("should reject BOGO if no electronics items are in the cart")
        void shouldRejectNoElectronics() {
            PromoEngine.Customer customer = new PromoEngine.Customer("C1", evalDate.minusYears(1), true, null);
            PromoEngine.CartItem item = new PromoEngine.CartItem("P1", "BOOKS", 100.0, 2);
            PromoEngine.Cart cart = new PromoEngine.Cart(customer, List.of(item), 5.0);

            PromoEngine.DiscountResult result = engine.applyPromo(cart, "BOGO_ELECTRONICS", evalDate);

            assertFalse(result.isValid());
            assertEquals(0.0, result.discountAmount());
            assertEquals("No electronics found in cart", result.message());
        }
    }

    @Nested
    @DisplayName("BIRTHDAY_TREAT Promo Code")
    class BirthdayTreatPromo {

        @Test
        @DisplayName("should apply 15% discount if today is customer birthday and cart has at least 3 items")
        void shouldApplyBirthdayDiscount() {
            LocalDate birthDate = LocalDate.of(1990, 7, 6); // Matches month/day of evalDate
            PromoEngine.Customer customer = new PromoEngine.Customer("C1", evalDate.minusYears(1), false, birthDate);
            PromoEngine.CartItem item1 = new PromoEngine.CartItem("P1", "BOOKS", 50.0, 2);
            PromoEngine.CartItem item2 = new PromoEngine.CartItem("P2", "TOYS", 100.0, 1); // Total qty = 3, Total price = 200
            PromoEngine.Cart cart = new PromoEngine.Cart(customer, List.of(item1, item2), 5.0);

            PromoEngine.DiscountResult result = engine.applyPromo(cart, "BIRTHDAY_TREAT", evalDate);

            assertTrue(result.isValid());
            assertEquals(30.0, result.discountAmount()); // 15% of 200
        }

        @Test
        @DisplayName("should reject birthday discount if today is not the customer birthday")
        void shouldRejectWhenNotBirthday() {
            LocalDate birthDate = LocalDate.of(1990, 7, 7); // Mismatch
            PromoEngine.Customer customer = new PromoEngine.Customer("C1", evalDate.minusYears(1), false, birthDate);
            PromoEngine.CartItem item1 = new PromoEngine.CartItem("P1", "BOOKS", 50.0, 3);
            PromoEngine.Cart cart = new PromoEngine.Cart(customer, List.of(item1), 5.0);

            PromoEngine.DiscountResult result = engine.applyPromo(cart, "BIRTHDAY_TREAT", evalDate);

            assertFalse(result.isValid());
            assertEquals(0.0, result.discountAmount());
            assertEquals("Today is not the customer's birthday", result.message());
        }

        @Test
        @DisplayName("should reject birthday discount if cart has fewer than 3 items")
        void shouldRejectWhenFewerThanThreeItems() {
            LocalDate birthDate = LocalDate.of(1990, 7, 6);
            PromoEngine.Customer customer = new PromoEngine.Customer("C1", evalDate.minusYears(1), false, birthDate);
            PromoEngine.CartItem item1 = new PromoEngine.CartItem("P1", "BOOKS", 50.0, 2); // Qty = 2
            PromoEngine.Cart cart = new PromoEngine.Cart(customer, List.of(item1), 5.0);

            PromoEngine.DiscountResult result = engine.applyPromo(cart, "BIRTHDAY_TREAT", evalDate);

            assertFalse(result.isValid());
            assertEquals(0.0, result.discountAmount());
            assertEquals("Birthday discount requires at least 3 items in the cart", result.message());
        }
    }

    @Nested
    @DisplayName("BULK_SAVINGS Promo Code")
    class BulkSavingsPromo {

        @Test
        @DisplayName("should apply $50 discount when cart total >= $300 and total qty >= 10")
        void shouldApplyTier2BulkSavings() {
            PromoEngine.Customer customer = new PromoEngine.Customer("C1", evalDate.minusYears(1), false, null);
            PromoEngine.CartItem item1 = new PromoEngine.CartItem("P1", "BOOKS", 30.0, 10); // Total price = 300, Qty = 10
            PromoEngine.Cart cart = new PromoEngine.Cart(customer, List.of(item1), 5.0);

            PromoEngine.DiscountResult result = engine.applyPromo(cart, "BULK_SAVINGS", evalDate);

            assertTrue(result.isValid());
            assertEquals(50.0, result.discountAmount());
        }

        @Test
        @DisplayName("should apply $20 discount when cart total >= $150 and total qty >= 5")
        void shouldApplyTier1BulkSavings() {
            PromoEngine.Customer customer = new PromoEngine.Customer("C1", evalDate.minusYears(1), false, null);
            PromoEngine.CartItem item1 = new PromoEngine.CartItem("P1", "BOOKS", 30.0, 5); // Total price = 150, Qty = 5
            PromoEngine.Cart cart = new PromoEngine.Cart(customer, List.of(item1), 5.0);

            PromoEngine.DiscountResult result = engine.applyPromo(cart, "BULK_SAVINGS", evalDate);

            assertTrue(result.isValid());
            assertEquals(20.0, result.discountAmount());
        }

        @Test
        @DisplayName("should reject bulk savings if threshold is not met")
        void shouldRejectBulkSavings() {
            PromoEngine.Customer customer = new PromoEngine.Customer("C1", evalDate.minusYears(1), false, null);
            PromoEngine.CartItem item1 = new PromoEngine.CartItem("P1", "BOOKS", 30.0, 4); // Total price = 120, Qty = 4
            PromoEngine.Cart cart = new PromoEngine.Cart(customer, List.of(item1), 5.0);

            PromoEngine.DiscountResult result = engine.applyPromo(cart, "BULK_SAVINGS", evalDate);

            assertFalse(result.isValid());
            assertEquals(0.0, result.discountAmount());
            assertEquals("Cart does not meet the bulk savings threshold", result.message());
        }
    }

    @Nested
    @DisplayName("VIP_SUMMER Promo Code")
    class VipSummerPromo {

        @Test
        @DisplayName("should apply 20% discount on clothing items for non-loyal customer")
        void shouldApplyClothingDiscountOnly() {
            PromoEngine.Customer customer = new PromoEngine.Customer("C1", evalDate.minusYears(1), false, null);
            PromoEngine.CartItem item1 = new PromoEngine.CartItem("P1", "CLOTHING", 100.0, 2); // 200.0 total
            PromoEngine.CartItem item2 = new PromoEngine.CartItem("P2", "BOOKS", 50.0, 1);    // No discount
            PromoEngine.Cart cart = new PromoEngine.Cart(customer, List.of(item1, item2), 10.0);

            PromoEngine.DiscountResult result = engine.applyPromo(cart, "VIP_SUMMER", evalDate);

            assertTrue(result.isValid());
            assertEquals(40.0, result.discountAmount()); // 20% of 200.0
        }

        @Test
        @DisplayName("should apply 20% discount on clothing + waive shipping for loyal customer")
        void shouldApplyClothingDiscountAndWaiveShipping() {
            PromoEngine.Customer customer = new PromoEngine.Customer("C1", evalDate.minusYears(1), true, null);
            PromoEngine.CartItem item1 = new PromoEngine.CartItem("P1", "CLOTHING", 100.0, 2); // 200.0 total
            PromoEngine.CartItem item2 = new PromoEngine.CartItem("P2", "BOOKS", 50.0, 1);
            PromoEngine.Cart cart = new PromoEngine.Cart(customer, List.of(item1, item2), 15.0);

            PromoEngine.DiscountResult result = engine.applyPromo(cart, "VIP_SUMMER", evalDate);

            assertTrue(result.isValid());
            assertEquals(55.0, result.discountAmount()); // 40.0 clothing + 15.0 shipping
        }

        @Test
        @DisplayName("should reject VIP summer promo if no clothing items are in the cart")
        void shouldRejectNoClothing() {
            PromoEngine.Customer customer = new PromoEngine.Customer("C1", evalDate.minusYears(1), true, null);
            PromoEngine.CartItem item1 = new PromoEngine.CartItem("P1", "BOOKS", 100.0, 2);
            PromoEngine.Cart cart = new PromoEngine.Cart(customer, List.of(item1), 15.0);

            PromoEngine.DiscountResult result = engine.applyPromo(cart, "VIP_SUMMER", evalDate);

            assertFalse(result.isValid());
            assertEquals(0.0, result.discountAmount());
            assertEquals("No clothing items in cart to apply discount", result.message());
        }
    }

    @Nested
    @DisplayName("GIFT_ Gift Card Promo Codes")
    class GiftCardPromo {

        @Test
        @DisplayName("should apply fixed gift card amount capped at cart total")
        void shouldApplyGiftCardValue() {
            PromoEngine.Customer customer = new PromoEngine.Customer("C1", evalDate.minusYears(1), false, null);
            PromoEngine.CartItem item1 = new PromoEngine.CartItem("P1", "BOOKS", 30.0, 2); // Total price = 60.0
            PromoEngine.Cart cart = new PromoEngine.Cart(customer, List.of(item1), 5.0);

            PromoEngine.DiscountResult result = engine.applyPromo(cart, "GIFT_50", evalDate);

            assertTrue(result.isValid());
            assertEquals(50.0, result.discountAmount());
            assertEquals("GIFT_50", result.appliedCode());
        }

        @Test
        @DisplayName("should cap gift card discount at total items value")
        void shouldCapGiftCardAtCartTotal() {
            PromoEngine.Customer customer = new PromoEngine.Customer("C1", evalDate.minusYears(1), false, null);
            PromoEngine.CartItem item1 = new PromoEngine.CartItem("P1", "BOOKS", 30.0, 1); // Total price = 30.0
            PromoEngine.Cart cart = new PromoEngine.Cart(customer, List.of(item1), 5.0);

            PromoEngine.DiscountResult result = engine.applyPromo(cart, "GIFT_50", evalDate);

            assertTrue(result.isValid());
            assertEquals(30.0, result.discountAmount());
        }

        @Test
        @DisplayName("should reject invalid gift card code formats")
        void shouldRejectInvalidGiftCardCode() {
            PromoEngine.Customer customer = new PromoEngine.Customer("C1", evalDate.minusYears(1), false, null);
            PromoEngine.CartItem item1 = new PromoEngine.CartItem("P1", "BOOKS", 30.0, 1);
            PromoEngine.Cart cart = new PromoEngine.Cart(customer, List.of(item1), 5.0);

            PromoEngine.DiscountResult result = engine.applyPromo(cart, "GIFT_ABC", evalDate);
            assertFalse(result.isValid());
            assertEquals(0.0, result.discountAmount());
            assertEquals("Invalid gift card code format", result.message());

            result = engine.applyPromo(cart, "GIFT_0", evalDate);
            assertFalse(result.isValid());

            result = engine.applyPromo(cart, "GIFT_12345", evalDate); // too long
            assertFalse(result.isValid());
        }
    }
}
