# State Pattern

> Object behavior changes based on its internal state. Eliminate massive conditionals.

## Why?

When an object's behavior depends on its state, and it must change behavior at runtime.

## Where?

- **Elevator**: Idle, MovingUp, MovingDown, DoorOpen, Maintenance
- **ATM**: Idle, CardInserted, PinVerified, TransactionSelected, DispensingCash
- **TCP Connection**: Closed, Listening, Established, TimeWait
- **Order**: Pending, Confirmed, Shipped, Delivered, Cancelled
- **Game character**: Idle, Running, Jumping, Attacking, Dead

## How

```java
// 1. State interface
interface ATMSState {
    void insertCard(ATM context);
    void ejectCard(ATM context);
    void enterPin(ATM context, String pin);
    void withdrawCash(ATM context, int amount);
}

// 2. Concrete states
class IdleState implements ATMSState {
    public void insertCard(ATM context) {
        System.out.println("Card inserted");
        context.setState(new CardInsertedState());
    }
    public void ejectCard(ATM context) {
        System.out.println("No card to eject");
    }
    public void enterPin(ATM context, String pin) {
        System.out.println("Insert card first");
    }
    public void withdrawCash(ATM context, int amount) {
        System.out.println("Insert card first");
    }
}

class CardInsertedState implements ATMSState {
    public void insertCard(ATM context) {
        System.out.println("Card already inserted");
    }
    public void ejectCard(ATM context) {
        System.out.println("Card ejected");
        context.setState(new IdleState());
    }
    public void enterPin(ATM context, String pin) {
        if (isValidPin(pin)) {
            System.out.println("PIN verified");
            context.setState(new PinVerifiedState());
        } else {
            System.out.println("Invalid PIN");
        }
    }
    public void withdrawCash(ATM context, int amount) {
        System.out.println("Enter PIN first");
    }
    private boolean isValidPin(String pin) { return pin.length() == 4; }
}

class PinVerifiedState implements ATMSState {
    public void insertCard(ATM context) { System.out.println("Card already in"); }
    public void ejectCard(ATM context) {
        System.out.println("Card ejected");
        context.setState(new IdleState());
    }
    public void enterPin(ATM context, String pin) { System.out.println("PIN already entered"); }
    public void withdrawCash(ATM context, int amount) {
        if (context.getBalance() >= amount) {
            System.out.println("Dispensing $" + amount);
            context.deductBalance(amount);
        } else {
            System.out.println("Insufficient funds");
        }
        context.setState(new IdleState());
    }
}

// 3. Context (ATM)
class ATM {
    private ATMSState state;
    private int balance = 1000;

    ATM() { this.state = new IdleState(); }
    void setState(ATMSState state) { this.state = state; }
    int getBalance() { return balance; }
    void deductBalance(int amount) { this.balance -= amount; }

    void insertCard() { state.insertCard(this); }
    void ejectCard() { state.ejectCard(this); }
    void enterPin(String pin) { state.enterPin(this, pin); }
    void withdrawCash(int amount) { state.withdrawCash(this, amount); }
}

// 4. Usage
ATM atm = new ATM();
atm.insertCard();       // → CardInsertedState
atm.enterPin("1234");   // → PinVerifiedState
atm.withdrawCash(200);  // → Dispenses cash, back to IdleState
```

## State vs Strategy

| Aspect | State | Strategy |
|--------|-------|----------|
| Intent | Behavior changes with state | Algorithm is swappable |
| Who sets state | Context or state itself | Client sets strategy |
| States know each other | Yes (transition to next) | No (independent) |

## Interview Application

- **Elevator system**: State transitions for floor requests
- **Order workflow**: Pending → Processing → Shipped → Delivered
- **Media player**: Stopped → Playing → Paused → Stopped
- **Document editor**: Draft → Review → Published → Archived
