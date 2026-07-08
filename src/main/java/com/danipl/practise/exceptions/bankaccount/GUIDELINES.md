# Challenge: BankAccount — Guidelines

## 1. Challenge Presentation

### What You're Building
A simple bank account component with a well-designed custom exception hierarchy. This is the kind of
error model you'd find at the core of any financial service — clean, typed, and fail-fast.

### Core Contract
```
deposit(amt > 0)   → balance += amt       | else → InvalidAmountException
withdraw(amt > 0)  → balance -= amt       | else → InvalidAmountException
                     balance >= amt        | else → InsufficientFundsException
                     !frozen               | else → AccountFrozenException
freeze()           → blocks all ops
unfreeze()         → re-enables ops
```

### Interface Summary
| Method | Purpose |
|--------|---------|
| `deposit(double)` | Add funds; validates amount and frozen state |
| `withdraw(double)` | Remove funds; validates amount, balance, and frozen state |
| `freeze()` | Lock the account |
| `unfreeze()` | Unlock the account |
| `getBalance()` | Current balance |
| `isFrozen()` | Frozen state |

---

## 2. Edge & Corner Cases

| # | Edge Case | How It Surfaces | How to Handle |
|---|-----------|-----------------|---------------|
| 1 | Negative initial balance | Constructor | Throw `InvalidAmountException` |
| 2 | Zero / negative deposit or withdrawal | Amount validation | Throw `InvalidAmountException` |
| 3 | Withdraw more than balance | Balance check | Throw `InsufficientFundsException` |
| 4 | Any op on frozen account | Frozen check | Throw `AccountFrozenException` |
| 5 | Withdraw exactly full balance | Boundary | Should succeed (balance → 0) |

**Validation order matters**: frozen → amount → balance. Think about which check is cheapest and most
fundamental first.

---

## 3. First Approach — Chain of Thinking
- **Minute 0-2**: Read the interface and tests. Note the exception hierarchy is already defined.
- **Minute 2-5**: Identify the two fields you need (`balance`, `frozen`) and the validation order.
- **Minute 5-15**: Implement each method with guard clauses (early returns via exceptions).
- **Minute 15-20**: Run tests, fix any ordering or message issues.

---

## 4. Communication Approach
Explain your design choices:
- Why guard clauses over nested if/else?
- Why `RuntimeException` subclasses (unchecked) for domain errors?
- Why validate frozen state before amount?

---

## 5. Implementation Structure
```java
public final class BankAccountImpl implements BankAccount {
    private double balance;
    private boolean frozen;

    public BankAccountImpl(double initialBalance) {
        // validate initialBalance
    }

    private void requireNotFrozen() { ... }
    private void requirePositiveAmount(double amount) { ... }

    @Override
    public void deposit(double amount) {
        requireNotFrozen();
        requirePositiveAmount(amount);
        balance += amount;
    }
    // ...
}
```

---

## 6. Technical Pro Tips
- Extract `requireNotFrozen()` and `requirePositiveAmount()` as private helpers — keeps public methods
  to 2-3 lines each.
- Use `Double.compare` or simple `<=` for the amount check (avoid floating-point equality traps).
- Exception messages should include the offending value for debuggability.

---

## 7. Common Mistakes to Avoid
- Checking balance before frozen state (wrong validation order).
- Throwing generic `RuntimeException` or `IllegalArgumentException` instead of the typed hierarchy.
- Forgetting to validate the constructor's `initialBalance`.
- Using `==` to compare doubles.

---

## 8. Verification Checklist
- [ ] All 15 tests pass
- [ ] Validation order: frozen → amount → balance
- [ ] Exception messages contain useful context
- [ ] No floating-point comparison bugs

---

## 9. Extension Points
- How would you make this thread-safe? (`synchronized`, `ReentrantLock`, or `AtomicReference`?)
- How would you add a transaction log / audit trail?
- How would you support multi-currency accounts?

---

## 10. Production References
- *Effective Java* (Joshua Bloch) — Item 72: Favor standard exceptions; Item 73: Throw exceptions
  appropriate to the abstraction.
- Clean exception hierarchies are a hallmark of well-designed Java libraries (see `java.sql`, `java.nio`).
