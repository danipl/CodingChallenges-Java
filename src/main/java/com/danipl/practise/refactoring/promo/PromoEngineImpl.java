package com.danipl.practise.refactoring.promo;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Implementation of {@link PromoEngine}.
 * Contains a messy legacy implementation that needs refactoring.
 */
public final class PromoEngineImpl implements PromoEngine {

    @Override
    public DiscountResult applyPromo(Cart cart, String promoCode, LocalDate evaluationDate) {
        if (cart == null) {
            throw new IllegalArgumentException("Cart cannot be null");
        }
        if (promoCode == null || promoCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Promo code cannot be empty");
        }

        String code = promoCode.trim().toUpperCase();

        if (code.equals("WELCOME10")) {
            if (cart.customer() == null) {
                return new DiscountResult(0.0, promoCode, false, "Customer information missing");
            }
            long days = ChronoUnit.DAYS.between(cart.customer().registrationDate(), evaluationDate);
            if (days >= 0 && days <= 30) {
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
                    double discount = total * 0.10;
                    if (discount > 20.0) {
                        discount = 20.0;
                    }
                    // Round to 2 decimal places in an ugly way
                    discount = Math.round(discount * 100.0) / 100.0;
                    return new DiscountResult(discount, "WELCOME10", true, "10% new customer discount applied");
                } else {
                    return new DiscountResult(0.0, promoCode, false, "Cart total must be at least $50.00");
                }
            } else {
                return new DiscountResult(0.0, promoCode, false, "Welcome promo is only for new customers registered within 30 days");
            }
        } else if (code.equals("BOGO_ELECTRONICS")) {
            if (cart.customer() == null) {
                return new DiscountResult(0.0, promoCode, false, "Customer info is required");
            }
            if (!cart.customer().isLoyalMember()) {
                return new DiscountResult(0.0, promoCode, false, "BOGO Electronics is only available to loyal members");
            }

            double discountAmount = 0.0;
            boolean hasElectronics = false;
            if (cart.items() != null) {
                for (CartItem item : cart.items()) {
                    if (item != null && "ELECTRONICS".equalsIgnoreCase(item.category())) {
                        hasElectronics = true;
                        int freeQty = item.quantity() / 2;
                        discountAmount += freeQty * item.price();
                    }
                }
            }

            if (!hasElectronics) {
                return new DiscountResult(0.0, promoCode, false, "No electronics found in cart");
            }

            discountAmount = Math.round(discountAmount * 100.0) / 100.0;
            return new DiscountResult(discountAmount, "BOGO_ELECTRONICS", true, "Buy 1 Get 1 Free applied to Electronics");

        } else if (code.equals("BIRTHDAY_TREAT")) {
            if (cart.customer() == null || cart.customer().birthDate() == null) {
                return new DiscountResult(0.0, promoCode, false, "Customer birthday information is missing");
            }
            
            LocalDate birthday = cart.customer().birthDate();
            if (birthday.getMonth() == evaluationDate.getMonth() && birthday.getDayOfMonth() == evaluationDate.getDayOfMonth()) {
                int totalQty = 0;
                double total = 0;
                if (cart.items() != null) {
                    for (CartItem item : cart.items()) {
                        if (item != null) {
                            totalQty += item.quantity();
                            total += item.price() * item.quantity();
                        }
                    }
                }
                if (totalQty >= 3) {
                    double discount = total * 0.15;
                    discount = Math.round(discount * 100.0) / 100.0;
                    return new DiscountResult(discount, "BIRTHDAY_TREAT", true, "15% Birthday discount applied");
                } else {
                    return new DiscountResult(0.0, promoCode, false, "Birthday discount requires at least 3 items in the cart");
                }
            } else {
                return new DiscountResult(0.0, promoCode, false, "Today is not the customer's birthday");
            }
        } else if (code.equals("BULK_SAVINGS")) {
            double total = 0;
            int totalQty = 0;
            if (cart.items() != null) {
                for (CartItem item : cart.items()) {
                    if (item != null) {
                        total += item.price() * item.quantity();
                        totalQty += item.quantity();
                    }
                }
            }

            double discount = 0.0;
            if (total >= 300.0 && totalQty >= 10) {
                discount = 50.0;
            } else if (total >= 150.0 && totalQty >= 5) {
                discount = 20.0;
            }

            if (discount > 0.0) {
                return new DiscountResult(discount, "BULK_SAVINGS", true, "Bulk savings discount applied");
            } else {
                return new DiscountResult(0.0, promoCode, false, "Cart does not meet the bulk savings threshold");
            }
        } else if (code.equals("VIP_SUMMER")) {
            double clothingTotal = 0.0;
            boolean hasClothing = false;
            if (cart.items() != null) {
                for (CartItem item : cart.items()) {
                    if (item != null && "CLOTHING".equalsIgnoreCase(item.category())) {
                        hasClothing = true;
                        clothingTotal += item.price() * item.quantity();
                    }
                }
            }

            if (!hasClothing) {
                return new DiscountResult(0.0, promoCode, false, "No clothing items in cart to apply discount");
            }

            double discount = clothingTotal * 0.20;
            
            if (cart.customer() != null && cart.customer().isLoyalMember()) {
                discount += cart.shippingCost();
            }

            discount = Math.round(discount * 100.0) / 100.0;
            return new DiscountResult(discount, "VIP_SUMMER", true, "VIP Summer discount applied");
        } else if (code.startsWith("GIFT_")) {
            // Gift card logic
            if (code.length() >= 6 && code.length() <= 8) {
                String valStr = code.substring(5);
                boolean validDigits = true;
                for (int i = 0; i < valStr.length(); i++) {
                    char c = valStr.charAt(i);
                    if (c < '0' || c > '9') {
                        validDigits = false;
                        break;
                    }
                }
                if (validDigits && !valStr.isEmpty()) {
                    int val = Integer.parseInt(valStr);
                    if (val > 0) {
                        double cartTotal = 0;
                        if (cart.items() != null) {
                            for (CartItem item : cart.items()) {
                                if (item != null) {
                                    cartTotal += item.price() * item.quantity();
                                }
                            }
                        }
                        double discount = Math.min(cartTotal, val);
                        discount = Math.round(discount * 100.0) / 100.0;
                        return new DiscountResult(discount, code, true, "Gift card applied");
                    }
                }
            }
            return new DiscountResult(0.0, promoCode, false, "Invalid gift card code format");
        }

        return new DiscountResult(0.0, promoCode, false, "Invalid code: " + promoCode);
    }
}
