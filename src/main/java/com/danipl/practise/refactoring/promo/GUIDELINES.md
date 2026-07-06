# Challenge: Promo Engine Refactoring - Guidelines

## 1. Challenge Presentation
### What You're Building
You are refactoring a promotional and coupon discount system (`PromoEngine`). Currently, the system contains all its business rules inside a single monolithic class (`PromoEngineImpl`) with nested conditional statements, duplication of loops, fragile date logic, and poor error handling. 

Your goal is to refactor the code to improve **maintainability**, **readability**, and **extensibility** while keeping its external behavior identical (verified by keeping the test suite green).

### Core Contract
```
[PromoEngine]
      │
      ▼
applyPromo(Cart, String promoCode, LocalDate evaluationDate)
      │
      ├─► WELCOME10       (10% off new customer, min $50 cart, max $20 discount)
      ├─► BOGO_ELECTRONICS (Loyal member buy 1 get 1 free on Electronics)
      ├─► BIRTHDAY_TREAT   (15% off cart on customer birthday, min 3 items)
      ├─► BULK_SAVINGS     ($20 off if >=$150 and >=5 items, $50 off if >=$300 and >=10 items)
      ├─► VIP_SUMMER       (20% off Clothing + free shipping if loyal customer)
      └─► GIFT_<value>     (Gift card with fixed value, capped at cart items total)
```

### Interface Summary
| Method | Purpose |
|--------|---------|
| `applyPromo` | Computes the discount value and constructs a `DiscountResult` representing the operation outcome. |

---

## 2. Edge & Corner Cases
### How to Identify Them
Look at how data varies and inputs are structured: null inputs, negative or zero values, date differences (leap years, timezone-free comparisons), rounding details, and customer loyalty flags.

| # | Edge Case | How It Surfaces | How to Handle |
|---|-----------|-----------------|---------------|
| 1 | Expiry / New user duration | Days between registration and evaluation is exactly 30 | Inclusive boundary (0 to 30 days is valid). |
| 2 | Double rounding | 10% or 15% discount yields a long fraction (e.g., $14.357) | Round to exactly 2 decimal places using deterministic rounding (like `BigDecimal` or correct scaling). |
| 3 | Empty Cart / Missing items | Items list is null or empty | Safely treat as $0 total / 0 quantity without throwing NPE. |
| 4 | Gift Card Code | GIFT_0, GIFT_ABC, GIFT_9999 | Check that format matches `GIFT_\d+`, value is positive, and length fits constraints. Return invalid code on mismatch. |
| 5 | Stacked benefits | VIP loyal members get 20% clothing discount + shipping waived | Waiving shipping is represented as adding the `shippingCost` to the discount amount. |

---

## 3. First Approach - Chain of Thinking
- **Minute 0-5**: Study the existing `PromoEngineImpl` and understand the preconditions (null/empty validations) and categories.
- **Minute 5-10**: Identify code smells. Specifically:
  - Repetitive iterations to calculate cart items totals.
  - Large conditional statements (`if-else` blocks based on code values).
  - Procedural parsing of gift card values.
- **Minute 10-15**: Define a Strategy / Rule interface (e.g., `PromoStrategy` or `DiscountRule`) to isolate the rules.
- **Minute 15-25**: Refactor the code block-by-block, delegating to strategies. Clean up the cart calculation logic using Java Streams.
- **Minute 25-30**: Run tests frequently during the refactoring process to verify that no functional regressions are introduced.

---

## 4. Communication Approach
In an interview, you should explain:
- **Design Patterns Selection**: Why you chose a specific pattern (like Strategy or Chain of Responsibility) to decouple the code.
- **Immutability & Safety**: Why we validate inputs early (fail-fast) and why we treat cart inputs as read-only.
- **Modern Java Idioms**: Using Java Streams and lambda functions to replace traditional `for` loops, improving readability.

---

## 5. Implementation Structure
An elegant structure separates each promo strategy into its own class or a registry:

```java
public final class PromoEngineImpl implements PromoEngine {
    
    // Suggestion: A map or list of strategies/rules.
    private final List<PromoRule> rules = List.of(
        new WelcomeRule(),
        new BogoElectronicsRule(),
        ...
    );

    @Override
    public DiscountResult applyPromo(Cart cart, String promoCode, LocalDate evaluationDate) {
        // 1. Validate inputs (fail fast)
        // 2. Select rule matching promoCode
        // 3. Apply rule logic and return DiscountResult
    }
}
```

---

## 6. Technical Pro Tips
- Use Java standard math utilities safely. For precise financial calculations, `BigDecimal` is preferred, but for this exercise, double calculations with rounding to 2 decimal places are expected: `Math.round(val * 100.0) / 100.0`.
- Leverage Java Streams for calculations:
  ```java
  double total = cart.items().stream()
      .mapToDouble(item -> item.price() * item.quantity())
      .sum();
  ```
- Use `LocalDate` APIs: `ChronoUnit.DAYS.between(d1, d2)` and `.getMonth()`, `.getDayOfMonth()` for birthday matching.

---

## 7. Common Mistakes to Avoid
- **Changing validation constraints**: Changing the exception types or messages thrown when validation fails. Ensure you continue to throw `IllegalArgumentException` with the exact messages.
- **Modifying input arguments**: Mutating the items lists or customer properties inside the engine.
- **NPE on missing Customer/Items**: Forgetting that items lists, customers, or items can be null in some contexts.

---

## 8. Verification Checklist
- [ ] Running all tests passes.
- [ ] The massive conditional logic is replaced with modular, single-responsibility classes/methods.
- [ ] No code duplicate loops are present in the final implementation.
- [ ] No hardcoded values for date matching exist.

---

## 9. Extension Points
- How would you handle combinable promo codes (e.g. applying a Gift Card *and* a Birthday discount)?
- How would you persist coupon rules in a database and load them dynamically without modifying code?

---

## 10. Production References
- **Strategy Pattern** from Design Patterns (GoF) for encapsulating discount algorithms.
- **Refactoring: Improving the Design of Existing Code** (Martin Fowler) — Specifically "Replace Conditional with Polymorphism".
