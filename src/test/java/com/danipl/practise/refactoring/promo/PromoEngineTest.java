package com.danipl.practise.refactoring.promo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PromoEngine Tests")
public class PromoEngineTest {

    private PromoEngine engine;

    @BeforeEach
    void setUp() {
        engine = PromoEngine.of();
    }

    @Nested
    @DisplayName("Validation and Inputs")
    public class ValidationAndInputs {

        @Test
        @DisplayName("should throw exception when cart is null")
        public void shouldThrowExceptionWhenCartIsNull() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    engine.applyPromo(null, "FLAT_10")
            );
            assertEquals("Cart cannot be null", ex.getMessage());
        }

        @Test
        @DisplayName("should throw exception when promo code is empty")
        public void shouldThrowExceptionWhenCodeIsEmpty() {
            PromoEngine.Cart cart = new PromoEngine.Cart(List.of());
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    engine.applyPromo(cart, "   ")
            );
            assertEquals("Promo code cannot be empty", ex.getMessage());
        }

        @Test
        @DisplayName("should return invalid result for unrecognized promo code")
        public void shouldReturnInvalidResultForUnrecognizedCode() {
            PromoEngine.Cart cart = new PromoEngine.Cart(List.of());
            PromoEngine.DiscountResult result = engine.applyPromo(cart, "INVALID");
            assertFalse(result.isValid());
            assertEquals(BigDecimal.ZERO, result.discountAmount());
        }
    }

    @Nested
    @DisplayName("FLAT_10 Rule")
    public class Flat10Rule {

        @Test
        @DisplayName("should apply $10 discount when cart total >= $50")
        public void shouldApplyFlatDiscount() {
            PromoEngine.CartItem item = new PromoEngine.CartItem("P1", "BOOKS", 50.0, 1);
            PromoEngine.Cart cart = new PromoEngine.Cart(List.of(item));

            PromoEngine.DiscountResult result = engine.applyPromo(cart, "FLAT_10");
            assertTrue(result.isValid());
            assertEquals(BigDecimal.TEN, result.discountAmount());
        }

        @Test
        @DisplayName("should reject FLAT_10 when cart total is under $50")
        public void shouldRejectUnderThreshold() {
            PromoEngine.CartItem item = new PromoEngine.CartItem("P1", "BOOKS", 49.99, 1);
            PromoEngine.Cart cart = new PromoEngine.Cart(List.of(item));

            PromoEngine.DiscountResult result = engine.applyPromo(cart, "FLAT_10");
            assertFalse(result.isValid());
            assertEquals(BigDecimal.ZERO, result.discountAmount());
        }
    }

    @Nested
    @DisplayName("PERCENT_20 Rule")
    public class Percent20Rule {

        @Test
        @DisplayName("should apply 20% discount up to the cap of $30")
        public void shouldApplyPercentDiscount() {
            PromoEngine.CartItem item = new PromoEngine.CartItem("P1", "BOOKS", 100.0, 1);
            PromoEngine.Cart cart = new PromoEngine.Cart(List.of(item));

            PromoEngine.DiscountResult result = engine.applyPromo(cart, "PERCENT_20");
            assertTrue(result.isValid());
            assertEquals(BigDecimal.valueOf(20), result.discountAmount());
        }

        @Test
        @DisplayName("should cap the discount at $30")
        public void shouldCapDiscount() {
            PromoEngine.CartItem item = new PromoEngine.CartItem("P1", "BOOKS", 200.0, 1);
            PromoEngine.Cart cart = new PromoEngine.Cart(List.of(item));

            PromoEngine.DiscountResult result = engine.applyPromo(cart, "PERCENT_20");
            assertTrue(result.isValid());
            assertEquals(BigDecimal.valueOf(30), result.discountAmount());
        }
    }

    @Nested
    @DisplayName("BOGO_FOOD Rule")
    public class BogoFoodRule {

        @Test
        @DisplayName("should apply BOGO for FOOD items in the cart")
        public void shouldApplyBogoForFood() {
            PromoEngine.CartItem food = new PromoEngine.CartItem("F1", "FOOD", 10.0, 3); // 1 free
            PromoEngine.CartItem other = new PromoEngine.CartItem("B1", "BOOKS", 20.0, 1);
            PromoEngine.Cart cart = new PromoEngine.Cart(List.of(food, other));

            PromoEngine.DiscountResult result = engine.applyPromo(cart, "BOGO_FOOD");
            assertTrue(result.isValid());
            assertEquals(BigDecimal.TEN, result.discountAmount());
        }

        @Test
        @DisplayName("should reject BOGO if no food items are present")
        public void shouldRejectNoFood() {
            PromoEngine.CartItem other = new PromoEngine.CartItem("B1", "BOOKS", 20.0, 1);
            PromoEngine.Cart cart = new PromoEngine.Cart(List.of(other));

            PromoEngine.DiscountResult result = engine.applyPromo(cart, "BOGO_FOOD");
            assertFalse(result.isValid());
            assertEquals(BigDecimal.ZERO, result.discountAmount());
        }
    }
}
