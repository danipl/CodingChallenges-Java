package com.danipl.practise.refactoring.promo;

/**
 * Implementation of {@link PromoEngine}.
 * Contains a simplified messy legacy implementation that needs refactoring.
 */
public final class PromoEngineImpl implements PromoEngine {

    @Override
    public DiscountResult applyPromo(Cart cart, String promoCode) {
        if (cart == null) {
            throw new IllegalArgumentException("Cart cannot be null");
        }
        if (promoCode == null || promoCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Promo code cannot be empty");
        }

        String code = promoCode.trim().toUpperCase();

        if (code.equals("FLAT_10")) {
            double total = 0;
            if (cart.items() != null) {
                for (int i = 0; i < cart.items().size(); i++) {
                    CartItem item = cart.items().get(i);
                    if (item != null) {
                        total += item.price() * item.quantity();
                    }
                }
            }
            if (total >= 50.0) {
                return new DiscountResult(10.0, "FLAT_10", true, "$10 flat discount applied");
            } else {
                return new DiscountResult(0.0, promoCode, false, "Cart total must be at least $50.00");
            }
        } else if (code.equals("PERCENT_20")) {
            double total = 0;
            if (cart.items() != null) {
                for (CartItem item : cart.items()) {
                    if (item != null) {
                        total += item.price() * item.quantity();
                    }
                }
            }
            double discount = total * 0.20;
            if (discount > 30.0) {
                discount = 30.0;
            }
            discount = Math.round(discount * 100.0) / 100.0;
            return new DiscountResult(discount, "PERCENT_20", true, "20% discount applied");
        } else if (code.equals("BOGO_FOOD")) {
            double discountAmount = 0.0;
            boolean hasFood = false;
            if (cart.items() != null) {
                for (CartItem item : cart.items()) {
                    if (item != null && "FOOD".equalsIgnoreCase(item.category())) {
                        hasFood = true;
                        int freeQty = item.quantity() / 2;
                        discountAmount += freeQty * item.price();
                    }
                }
            }
            if (!hasFood) {
                return new DiscountResult(0.0, promoCode, false, "No food items found in cart");
            }
            discountAmount = Math.round(discountAmount * 100.0) / 100.0;
            return new DiscountResult(discountAmount, "BOGO_FOOD", true, "BOGO applied to food items");
        }

        return new DiscountResult(0.0, promoCode, false, "Invalid code: " + promoCode);
    }
}
