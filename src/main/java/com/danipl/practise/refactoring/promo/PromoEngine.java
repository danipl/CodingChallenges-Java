package com.danipl.practise.refactoring.promo;

import java.math.BigDecimal;
import java.util.List;

/**
 * Interface contract for the Promotional Engine.
 * Calculates discount amounts based on active rules for coupon codes.
 */
public interface PromoEngine {

        /**
         * Factory method to construct the default implementation.
         */
        static PromoEngine of() {
                return new PromoEngineImpl();
        }

        /**
         * Applies the given promo code to a shopping cart.
         *
         * @param cart      the shopping cart containing items
         * @param promoCode the coupon/promo code string to apply
         * @return the result of applying the promo code
         * @throws IllegalArgumentException if the cart is null or if the promoCode is
         *                                  null/empty
         */
        DiscountResult applyPromo(Cart cart, String promoCode);

        // === Domain Records ===

        record CartItem(
                        String productId,
                        String category,
                        BigDecimal price,
                        int quantity) {

                public CartItem(String productId,
                                String category,
                                double price,
                                int quantity) {
                        this(productId, category, new BigDecimal(price), quantity);
                }

        }

        record Cart(
                        List<CartItem> items) {
                public Cart {
                        if (items == null) {
                                items = List.of();
                        }
                }
        }

        record DiscountResult(
                        BigDecimal discountAmount,
                        String appliedCode,
                        boolean isValid,
                        String message) {

                public DiscountResult(double discountAmount,
                                String appliedCode,
                                boolean isValid,
                                String message) {
                        this(new BigDecimal(discountAmount), appliedCode, isValid, message);
                }
        }
}
