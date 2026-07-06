package com.danipl.practise.refactoring.promo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.function.Function;

/**
 * Implementation of {@link PromoEngine}.
 * Contains a simplified messy legacy implementation that needs refactoring.
 */
public final class PromoEngineImpl implements PromoEngine {

    private final static BigDecimal FLAT_10_TOTAL_THRESHOLD = BigDecimal.valueOf(50);
    private final static BigDecimal CURRENT_20_PERCENT_DISCOUNT = BigDecimal.valueOf(0.2);
    private final static BigDecimal MAX_20_PERCENT_DISCOUNT = BigDecimal.valueOf(30);
    private final static BigDecimal BOOD_FOOD_QUANTITY_DIVIDER = BigDecimal.TWO;

    private static record CalculationData(Cart cart, String promoCode) {
    }

    private BigDecimal calculateTotal(final Cart cart) {
        BigDecimal total = BigDecimal.ZERO;
        for (final CartItem item : cart.items()) {
            if (item != null) {
                total = total.add(item.price().multiply(BigDecimal.valueOf(item.quantity())));
            }
        }
        return total;
    }

    // Calculators should be placed at their own classes, there are here to avoid
    // extra time effor during the screen sesion.
    // That is the reason because they are setup as public here.
    public final Function<CalculationData, DiscountResult> flat10Calculator = new Function<CalculationData, DiscountResult>() {

        @Override
        public DiscountResult apply(final CalculationData calculationData) {
            final Cart cart = calculationData.cart;
            final String promoCode = calculationData.promoCode;
            final BigDecimal total = calculateTotal(cart);
            if (total.compareTo(FLAT_10_TOTAL_THRESHOLD) >= 0) {
                return new DiscountResult(BigDecimal.TEN, "FLAT_10", true, "$10 flat discount applied");
            } else {
                return new DiscountResult(BigDecimal.ZERO, promoCode, false, "Cart total must be at least $50.00");
            }
        }

    };

    public final Function<CalculationData, DiscountResult> percent20Calculator = new Function<CalculationData, DiscountResult>() {

        @Override
        public DiscountResult apply(final CalculationData calculationData) {
            final Cart cart = calculationData.cart;
            final BigDecimal total = calculateTotal(cart);
            BigDecimal discount = total.multiply(CURRENT_20_PERCENT_DISCOUNT);
            discount = discount.min(MAX_20_PERCENT_DISCOUNT);
            return new DiscountResult(discount.setScale(0, RoundingMode.HALF_DOWN), "PERCENT_20", true,
                    "20% discount applied");
        }

    };

    public final Function<CalculationData, DiscountResult> bogoFoodCalculator = new Function<CalculationData, DiscountResult>() {

        @Override
        public DiscountResult apply(final CalculationData calculationData) {
            final Cart cart = calculationData.cart;
            final String promoCode = calculationData.promoCode;
            BigDecimal discount = BigDecimal.ZERO;
            boolean hasFood = false;
            for (final CartItem item : cart.items()) {
                if (item != null && item.quantity() != 0 && "FOOD".equalsIgnoreCase(item.category())) {
                    hasFood = true;
                    discount = discount.add(item.price().multiply(BigDecimal.valueOf(item.quantity())
                            .divide(BOOD_FOOD_QUANTITY_DIVIDER).setScale(0, RoundingMode.DOWN)));
                }
            }
            if (!hasFood) {
                return new DiscountResult(BigDecimal.ZERO, promoCode, false, "No food items found in cart");
            }
            return new DiscountResult(discount.setScale(0, RoundingMode.HALF_DOWN), "BOGO_FOOD", true,
                    "BOGO applied to food items");
        }

    };

    private final Function<CalculationData, DiscountResult> defaultCalculator = new Function<CalculationData, DiscountResult>() {

        @Override
        public DiscountResult apply(final CalculationData calculationData) {
            final String promoCode = calculationData.promoCode;
            return new DiscountResult(BigDecimal.ZERO, promoCode, false, "Invalid code: " + promoCode);
        }

    };

    private final Map<String, Function<CalculationData, DiscountResult>> calculatorMap = Map.of(
            "FLAT_10", flat10Calculator,
            "PERCENT_20", percent20Calculator,
            "BOGO_FOOD", bogoFoodCalculator);

    private void validate(final Cart cart, final String promoCode) {
        if (cart == null) {
            throw new IllegalArgumentException("Cart cannot be null");
        }
        if (promoCode == null || promoCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Promo code cannot be empty");
        }
    }

    @Override
    public DiscountResult applyPromo(final Cart cart, final String promoCode) {
        this.validate(cart, promoCode);

        final String code = promoCode.trim().toUpperCase();

        return this.calculatorMap.getOrDefault(code, defaultCalculator).apply(new CalculationData(cart, code));
    }

}
