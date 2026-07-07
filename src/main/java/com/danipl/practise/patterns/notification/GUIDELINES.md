# Challenge: NotificationDispatcher — Guidelines

## 1. Challenge Presentation
### What You're Building
A notification dispatcher that routes messages through different delivery channels (EMAIL, SMS, PUSH). Each channel formats the message differently before "sending" it. This is a textbook **Strategy pattern** scenario.

### Core Contract
```
Notification → Dispatcher → picks Strategy by Channel → formats → returns Receipt
```

### Interface Summary
| Method | Purpose |
|--------|---------|
| `dispatch(Notification)` | Route and format a notification, returning a `DispatchReceipt` |

### Formatting Rules (implement exactly these)
| Channel | Format |
|---------|--------|
| EMAIL | `To: {recipient}\nSubject: Notification\n\n{body}` |
| SMS | `[SMS to {recipient}]: {body}` — body truncated to 160 chars max |
| PUSH | `{{"token":"{recipient}","message":"{body}"}}` |

---

## 2. Edge & Corner Cases
### How to Identify Them
Look at every parameter in the `Notification` record. What happens when each is null, blank, or oversized?

| # | Edge Case | How It Surfaces | How to Handle |
|---|-----------|-----------------|---------------|
| 1 | Null notification | NPE | Throw `IllegalArgumentException` |
| 2 | Null/blank recipient | Silent bad delivery | Throw `IllegalArgumentException` |
| 3 | Null body | Empty notification | Throw `IllegalArgumentException` |
| 4 | Null channel | Can't pick strategy | Throw `IllegalArgumentException` |
| 5 | SMS body > 160 chars | Over-long message | Truncate to 160 |

---

## 3. First Approach - Chain of Thinking
- **Minute 0-2**: Read the interface + records. Note the 3 channels and formatting rules.
- **Minute 2-5**: Sketch the Strategy abstraction — what does each strategy do? (format + deliver)
- **Minute 5-10**: Implement validation, then each channel strategy.
- **Minute 10-25**: Wire dispatcher to select strategy by channel. Run tests. Refactor for cleanliness.

---

## 4. Communication Approach
Explain design choices out loud:
- Why Strategy pattern over a switch statement?
- How do you represent a "strategy" — interface, enum method, or function?
- Why validate early (fail-fast) vs. deep in the logic?

---

## 5. Implementation Structure
```java
public final class NotificationDispatcherImpl implements NotificationDispatcher {

    // Option A: Map<Channel, Strategy> populated in constructor
    // Option B: Switch on channel (simpler, but less extensible)
    // Option C: Enum with abstract format() method

    @Override
    public DispatchReceipt dispatch(final Notification notification) {
        // 1. Validate
        // 2. Select strategy
        // 3. Format
        // 4. Return receipt
    }
}
```

---

## 6. Technical Pro Tips
- Use `Objects.requireNonNull` or `StringUtils.isBlank` for validation.
- Keep each strategy small — one method, one responsibility.
- Prefer immutability: strategies should be stateless and reusable.
- Consider: should strategies be private inner classes, separate files, or enum constants?

---

## 7. Common Mistakes to Avoid
- **Switch statement sprawl**: Adding a new channel means editing the dispatcher. Violates OCP.
- **Duplicated validation**: Don't validate in each strategy — validate once at the entry point.
- **Mutable strategies**: Strategies should be stateless. No fields that change per dispatch.
- **Ignoring truncation**: SMS has a hard 160-char limit. Don't forget it.

---

## 8. Verification Checklist
- [ ] All 3 channels format correctly (check test assertions)
- [ ] Null/blank inputs throw `IllegalArgumentException`
- [ ] SMS body truncates to 160 chars
- [ ] Receipt contains correct channel, recipient, formatted message
- [ ] No switch/if-else chains that violate OCP

---

## 9. Extension Points
- How would you add a new channel (e.g., WEBHOOK) without modifying existing code?
- How would you make strategies configurable or injectable?
- How would you add retry logic for failed deliveries?

---

## 10. Production References
- **Design Patterns** (Gamma et al.) — Strategy chapter
- **Effective Java** (Joshua Bloch) — Item 34: Use enums instead of int constants
- **Clean Code** (Robert C. Martin) — Chapter 3: Functions (keep them small)
