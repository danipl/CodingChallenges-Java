# Vending Machine

> State-driven system with product selection, payment processing, and change dispensing.

## Requirements

- Product inventory with prices
- Coin/bill acceptance (multiple denominations)
- Change dispensing
- Product dispensing
- Return money / cancel transaction
- Admin: restock, collect money, set prices

## State Machine

```
IDLE → MONEY_INSERTED → PRODUCT_SELECTED → DISPENSING → RETURNING_CHANGE → IDLE
     → (any) → CANCEL → RETURNING_CHANGE → IDLE
     → (any) → OUT_OF_STOCK → IDLE
```

## Key Patterns

### State Pattern
```java
interface VendingState {
    void insertCoin(VendingMachine vm, Coin coin);
    void selectProduct(VendingMachine vm, Product product);
    void dispense(VendingMachine vm);
    void returnChange(VendingMachine vm);
    void cancel(VendingMachine vm);
}
```

### Strategy Pattern (Payment/Change)
```java
interface ChangeStrategy {
    Map<Coin, Integer> calculateChange(BigDecimal amount);
}

class GreedyChangeStrategy implements ChangeStrategy {
    private static final Coin[] DENOMINATIONS = {
        new Coin("QUARTER", 25), new Coin("DIME", 10),
        new Coin("NICKEL", 5), new Coin("PENNY", 1)
    };
    public Map<Coin, Integer> calculateChange(BigDecimal amount) {
        int cents = amount.multiply(BigDecimal.valueOf(100)).intValue();
        Map<Coin, Integer> result = new LinkedHashMap<>();
        for (Coin coin : DENOMINATIONS) {
            int count = cents / coin.getValueInCents();
            if (count > 0) {
                result.put(coin, count);
                cents %= coin.getValueInCents();
            }
        }
        return result;
    }
}
```

## Core Implementation

```java
class VendingMachine {
    private VendingState state = new IdleState();
    private final Inventory inventory;
    private final CashRegister cashRegister;
    private BigDecimal insertedAmount = BigDecimal.ZERO;
    private Product selectedProduct;

    void insertCoin(Coin coin) { state.insertCoin(this, coin); }
    void selectProduct(Product product) { state.selectProduct(this, product); }
    void dispense() { state.dispense(this); }
    void returnChange() { state.returnChange(this); }
    void cancel() { state.cancel(this); }

    void setState(VendingState state) { this.state = state; }
    void addInsertedAmount(Coin coin) { this.insertedAmount = insertedAmount.add(coin.getValue()); }
    BigDecimal getInsertedAmount() { return insertedAmount; }
    void resetInsertedAmount() { this.insertedAmount = BigDecimal.ZERO; }
}

class IdleState implements VendingState {
    public void insertCoin(VendingMachine vm, Coin coin) {
        vm.addInsertedAmount(coin);
        vm.setState(new MoneyInsertedState());
    }
    public void selectProduct(VendingMachine vm, Product product) {
        System.out.println("Insert money first");
    }
}

class MoneyInsertedState implements VendingState {
    public void insertCoin(VendingMachine vm, Coin coin) {
        vm.addInsertedAmount(coin);
    }
    public void selectProduct(VendingMachine vm, Product product) {
        if (!vm.getInventory().isAvailable(product)) {
            System.out.println("Out of stock");
            vm.setState(new IdleState());
            return;
        }
        if (vm.getInsertedAmount().compareTo(product.getPrice()) < 0) {
            System.out.println("Insufficient amount");
            return;
        }
        vm.setSelectedProduct(product);
        vm.setState(new ProductSelectedState());
        vm.dispense();
    }
    public void cancel(VendingMachine vm) {
        vm.returnChange();
        vm.setState(new IdleState());
    }
}
```

## Interview Tips

1. **State transitions**: Draw the state diagram before coding
2. **Change calculation**: Greedy algorithm works for standard denominations
3. **Inventory**: Track quantity per product, prevent dispensing when out of stock
4. **Edge cases**: Exact change not available, machine out of change, power failure mid-transaction
5. **Concurrency**: Single user at a time — no concurrent access needed
