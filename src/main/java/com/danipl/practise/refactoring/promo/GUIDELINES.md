# Challenge: Promo Engine Refactoring - Guidelines

## 1. Challenge Presentation
### What You're Building
You are refactoring a promotional and coupon discount system (`PromoEngine`). Currently, the system contains all its business rules inside a single monolithic class (`PromoEngineImpl`) with nested conditional statements, duplication of loops, and poor separation of concerns.

Your goal is to refactor the code to improve **maintainability**, **readability**, and **extensibility** while keeping its external behavior identical (verified by keeping the test suite green).

### Core Contract
```
[PromoEngine]
      │
      ▼
applyPromo(Cart, String promoCode)
      │
      ├─► FLAT_10     ($10 flat discount if cart total >= $50)
      ├─► PERCENT_20   (20% off cart total, capped at max $30 discount)
      └─► BOGO_FOOD    (Buy 1 Get 1 Free on FOOD category items)
```

### Interface Summary
| Method | Purpose |
|--------|---------|
| `applyPromo` | Computes the discount value and constructs a `DiscountResult`. |

---

## 2. Edge & Corner Cases
### How to Identify Them
Look at how data varies and inputs are structured: null inputs, empty lists, division by two for BOGO, and rounding.

| # | Edge Case | How It Surfaces | How to Handle |
|---|-----------|-----------------|---------------|
| 1 | Empty Cart | Items list is null or empty | Safely treat as $0 total / 0 discount without throwing NPE. |
| 2 | Double rounding | PERCENT_20 discount yields a fraction (e.g., $14.357) | Round to exactly 2 decimal places: `Math.round(val * 100.0) / 100.0`. |
| 3 | BOGO with odd quantities | Food item with quantity = 3 | Only 1 item is free (quantity / 2 = 1). |

---

## 3. First Approach - Chain of Thinking
- **Minute 0-3**: Study the existing `PromoEngineImpl` and understand the 3 discount types.
- **Minute 3-5**: Identify code smells. Specifically:
  - Repetitive loops to calculate cart items totals.
  - Large conditional statements (`if-else` blocks based on code values).
- **Minute 5-10**: Define a Strategy / Rule interface (e.g., `DiscountRule`) to isolate the rules.
- **Minute 10-15**: Refactor the code block-by-block, delegating to strategies. Clean up the cart calculation logic using Java Streams.
- **Minute 15-20**: Run tests to verify that no functional regressions are introduced.

---

## 4. Communication Approach
In an interview, explain:
- **Design Patterns Selection**: Why you chose Strategy or Polymorphism to decouple rules.
- **Modern Java Idioms**: Using Java Streams and lambda functions to replace traditional `for` loops.

---

## 5. Implementation Structure
An elegant structure separates each promo strategy into its own class/method or a registry:

```java
public final class PromoEngineImpl implements PromoEngine {
    
    // Suggestion: A map or list of strategies/rules.
    private final Map<String, DiscountRule> rules = Map.of(
        "FLAT_10", new FlatDiscountRule(),
        ...
    );

    @Override
    public DiscountResult applyPromo(Cart cart, String promoCode) {
        // Validate inputs, match rule, calculate discount
    }
}
```

---

## 6. Technical Pro Tips
- Use Java Streams for calculations:
  ```java
  double total = cart.items().stream()
      .mapToDouble(item -> item.price() * item.quantity())
      .sum();
  ```
- Use `Math.round(discount * 100.0) / 100.0` for 2 decimal places.

---

## 7. Common Mistakes to Avoid
- **NPE on missing Items**: Forgetting that items list or elements can be null.
- **Input Validation**: Keep the exact same validation logic (e.g., throwing `IllegalArgumentException` with the correct error messages).

---

## 8. Verification Checklist
- [ ] Running all tests passes.
- [ ] No duplicate code loops are present in the final implementation.
- [ ] The massive conditional logic is replaced with modular, single-responsibility classes/methods.

---

## 9. Extension Points
- How would you support dynamically loading rules from a database?

---

## 10. Production References
- **Strategy Pattern** (GoF) for encapsulating rules.
- **Refactoring** (Martin Fowler) — "Replace Conditional with Polymorphism".
