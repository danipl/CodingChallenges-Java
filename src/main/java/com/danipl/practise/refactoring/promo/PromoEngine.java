package com.danipl.practise.refactoring.promo;

import java.time.LocalDate;
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
     * @param cart           the shopping cart containing items and customer details
     * @param promoCode      the coupon/promo code string to apply
     * @param evaluationDate the date against which to check temporal rules (e.g. expiration, birthdays)
     * @return the result of applying the promo code
     * @throws IllegalArgumentException if the cart is null or if the promoCode is null/empty
     */
    DiscountResult applyPromo(Cart cart, String promoCode, LocalDate evaluationDate);

    // === Domain Records ===

    record Customer(
            String id,
            LocalDate registrationDate,
            boolean isLoyalMember,
            LocalDate birthDate
    ) {}

    record CartItem(
            String productId,
            String category,
            double price,
            int quantity
    ) {}

    record Cart(
            Customer customer,
            List<CartItem> items,
            double shippingCost
    ) {}

    record DiscountResult(
            double discountAmount,
            String appliedCode,
            boolean isValid,
            String message
    ) {}
}
