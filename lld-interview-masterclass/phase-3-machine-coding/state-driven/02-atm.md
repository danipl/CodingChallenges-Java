# ATM Machine

> State-driven transaction system with authentication, multiple transaction types, and cash dispensing.

## Requirements

- Card insertion and PIN verification
- Multiple transaction types: Withdraw, Deposit, Balance Inquiry, Transfer
- Cash dispensing with denomination optimization
- Receipt printing
- Session timeout

## Domain Model

```
ATM
  ├── CardReader
  ├── Keypad
  ├── CashDispenser
  ├── ReceiptPrinter
  ├── Screen (UI)
  ├── BankService (external)
  └── Session
        ├── state: ATMState
        ├── card: Card
        └── account: Account
```

## State Machine

```
IDLE → CARD_INSERTED → PIN_ENTERED → PIN_VERIFIED → TRANSACTION_SELECTED
     → PROCESSING → COMPLETED → RECEIPT_PRINTED → CARD_EJECTED → IDLE
     → (any state) → TIMEOUT → IDLE
     → (any state) → CANCEL → CARD_EJECTED → IDLE
```

## Key Patterns

### State Pattern
Each state handles actions and transitions to next state.

### Chain of Responsibility (Cash Dispensing)
```
$100 → $50 → $20 → $10 → $5 → $1
```

### Strategy Pattern (Transaction Types)
```java
interface Transaction {
    boolean execute(Account account, BigDecimal amount);
}

class WithdrawTransaction implements Transaction { /* ... */ }
class DepositTransaction implements Transaction { /* ... */ }
class TransferTransaction implements Transaction { /* ... */ }
```

## Core Implementation

```java
class ATM {
    private ATMState state = new IdleState();
    private Card insertedCard;
    private Account authenticatedAccount;
    private final BankService bankService;
    private final CashDispenser dispenser;

    void insertCard(Card card) { state.insertCard(this, card); }
    void enterPin(String pin) { state.enterPin(this, pin); }
    void selectTransaction(TransactionType type) { state.selectTransaction(this, type); }
    void executeTransaction(BigDecimal amount) { state.executeTransaction(this, amount); }
    void ejectCard() { state.ejectCard(this); }

    void setState(ATMState newState) { this.state = newState; }
}

interface ATMState {
    void insertCard(ATM atm, Card card);
    void enterPin(ATM atm, String pin);
    void selectTransaction(ATM atm, TransactionType type);
    void executeTransaction(ATM atm, BigDecimal amount);
    void ejectCard(ATM atm);
}

class IdleState implements ATMState {
    public void insertCard(ATM atm, Card card) {
        atm.setInsertedCard(card);
        atm.setState(new CardInsertedState());
    }
    // All other operations: "Please insert card first"
}

class PinVerifiedState implements ATMState {
    public void selectTransaction(ATM atm, TransactionType type) {
        Transaction txn = TransactionFactory.create(type);
        atm.setState(new TransactionSelectedState(txn));
    }
}
```

## Cash Dispenser (Chain of Responsibility)

```java
class CashDispenser {
    private static final int[] DENOMINATIONS = {100, 50, 20, 10, 5, 1};
    private final Map<Integer, Integer> availableNotes;

    Map<Integer, Integer> dispense(int amount) {
        Map<Integer, Integer> result = new LinkedHashMap<>();
        int remaining = amount;
        for (int denom : DENOMINATIONS) {
            int count = remaining / denom;
            int available = availableNotes.getOrDefault(denom, 0);
            int used = Math.min(count, available);
            if (used > 0) {
                result.put(denom, used);
                remaining -= used * denom;
                availableNotes.put(denom, available - used);
            }
        }
        if (remaining > 0) throw new InsufficientCashException();
        return result;
    }
}
```

## Interview Tips

1. **State transitions are key** — draw the state diagram first
2. **Handle timeout** — every state should have a timeout path back to IDLE
3. **Cash dispensing edge case**: ATM may not have exact denominations
4. **Security**: Card retention after failed PIN attempts
