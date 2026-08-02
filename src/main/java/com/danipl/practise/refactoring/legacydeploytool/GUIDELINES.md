# Challenge: DeployTool - Guidelines

## 1. Challenge Presentation
### What You're Building
An internal deployment tool that reads a `deploy.conf` file and resolves a deployment plan. The code is legacy: it was written by a team that has moved on, it works on the happy path, and it fails in confusing ways everywhere else.

This is the single most likely CloudBees pairing format: **"here is a messy internal tool — wrap it in tests and clean it up."**

### Core Contract
```
deploy.conf --read--> [key=value lines] --parse--> [DeployResult]
                                  |--fail--> DeployException (clear message)
```

### Interface Summary
| Method | Purpose |
|--------|---------|
| `deploy(Path)` | Read config, validate, return the resolved plan |
| `DeployResult` | Immutable record: targets, region, parallel, timeoutSeconds |
| `DeployException` | Runtime exception whose message is what the dev sees |

### What Interviewers Evaluate
1. **Blamelessness** — Do you say "this code is terrible" or "let's make it trustworthy"?
2. **Test-first discipline** — Do you lock behavior with tests *before* refactoring?
3. **Sad-path DevEx** — Is every failure a readable message, never a raw NPE/NumberFormatException?

---

## 2. Edge & Corner Cases
### How to Identify Them
Ask: *"What config could a developer actually write by hand?"* — typos, spaces, extra blank lines, all of it.

| # | Edge Case | How It Surfaces | How to Handle |
|---|-----------|-----------------|---------------|
| 1 | Missing file | `Files.readAllLines` throws IOException | `DeployException("config file not found: <path>")` |
| 2 | Empty file | Zero lines, no `target` key | `"missing required key: target"` |
| 3 | `target` key absent | No match during parse | Same missing-key error |
| 4 | `target=` empty value | Split yields `[""]` — *silently passes today!* | Treat as missing target |
| 5 | Whitespace: `target = webapp` | Key is `"target "` — *silently ignored today!* | `trim()` keys and values |
| 6 | Whitespace: `webapp, api` | Second entry has leading space | `trim()` each target |
| 7 | `parallel=TRUE` | `equals("true")` is case-sensitive — *rejected today!* | Case-insensitive parse, or `Boolean.parseBoolean` |
| 8 | `timeout=abc` | `Integer.parseInt` → raw `NumberFormatException` | Wrap into `DeployException` |
| 9 | `timeout=0` or `-5` | Accepted silently — *invalid today!* | Validate positive integer |
| 10 | `parallel=maybe` | Accepted silently — *invalid today!* | Validate true/false only |
| 11 | Malformed line `garbage line` | No `=`, ignored silently — *hidden today!* | Fail with line number |
| 12 | Blank lines / comments | Parsing noise | Skip them, but keep line numbers intact |
| 13 | Unknown keys | `strategy=blue-green` | Ignore (backwards compat) |

### Quick Pre-Implementation Checklist
```
▢ Have I run the tests and seen WHICH 11 fail?
▢ Did I read each failure and identify the root cause (not just patch the test)?
▢ Am I adding validation at the parse boundary, not scattered in the caller?
```

---

## 3. First Approach - Chain of Thinking

### Minute 0-2: Clarify Requirements
Say: *"I can see the happy path works. Before I touch anything, let me run the tests to understand which behaviors are already locked and where the gaps are."*

### Minute 2-5: Read the Legacy Code
Identify the smells out loud (without judging the author):
- Empty `catch (IOException e)` — swallows the missing-file case
- No trimming — whitespace breaks key matching
- `split(",")` on empty string yields `[""]` — silently "valid"
- Case-sensitive boolean, unvalidated timeout
- God method: parse + validate + build result in one block

### Minute 5-10: Lock Behavior With Tests
The test suite is already written — **run it first, read the failures, then refactor**. This is the interview pattern: tests are your safety net, never skip them.

### Minute 10-25: Refactor
Order of operations (lowest risk first):
1. Extract a `parseLine` / `validate` helper
2. Fix trimming (`key.trim()`, `value.trim()`, per-target `trim()`)
3. Validate: timeout positive int, parallel boolean, target presence
4. Replace empty catch with a specific `DeployException`
5. Add line numbers to parse errors

---

## 4. Communication Approach During the Interview
### What to Say Out Loud
| Moment | Say This |
|--------|----------|
| On the legacy code | "This handles the common case fine — the gaps are around validation and error reporting." |
| On the empty catch | "The IOException is swallowed, so a missing file looks like a valid empty config. I'll make that explicit." |
| On validation | "I'll validate at the parse boundary so every caller gets the same clear errors." |
| On line numbers | "Malformed line errors should say *which* line, so the dev can fix the config fast." |
| On `split` gotcha | "`''.split(',')` returns `['']` — an empty target silently passes today. I'll check for that." |

### When Stuck
```
I notice the tests fail on whitespace and validation cases.
The risk is that I patch symptoms instead of the parse boundary.
Two options: [A] add a small validate step per key, [B] preprocess all lines.
I'll go with [A] because validation belongs next to parsing. Does that align?
```

---

## 5. Implementation Structure
```java
public final class DeployToolImpl implements DeployTool {
    // deploy(Path)            -> read file, parse, validate, build result
    // parseKeyValue(String)   -> Optional<Entry> or null for comments/blanks
    // validateTargets(...)    -> List<String> trimmed, non-empty
    // validateTimeout(...)    -> positive int
    // validateParallel(...)   -> boolean
}
```

### Key Implementation Pattern
```java
private static final Pattern KEY_VALUE = Pattern.compile("^\\s*([^#][^=]*?)\\s*=\\s*(.*)$");
```
or the simpler, explicit route:
```java
String[] parts = line.split("=", 2);
String key = parts[0].trim();
String value = parts[1].trim();  // guard length first
```

---

## 6. Technical Pro Tips

### Files API
| Approach | Use When |
|----------|----------|
| `Files.readAllLines(path)` | Small config files, want a `List<String>` |
| `Files.lines(path)` (Stream) | Large files, streaming line by line |
| `Files.readString(path)` | Whole file as one string |

### What Senior Engineers Demonstrate
1. **Read the tests before the code** — behavior is locked in the suite, not the impl.
2. **Validate once, at the boundary** — not scattered `if` checks in callers.
3. **Preserve the public contract** — don't change the interface to fit the impl; fix the impl to honor the interface.

---

## 7. Common Mistakes to Avoid
| Mistake | Why It Fails | Fix |
|---------|-------------|-----|
| Deleting/weakening tests to make them pass | Cheating the safety net | Fix the impl, tests are the spec |
| `catch (Exception e)` around everything | Masks the missing-file case as "empty config" | Catch `IOException` → specific message |
| Forgetting `trim()` on target entries | `"webapp, api"` yields `" api"` | Trim each entry after `split` |
| Letting `NumberFormatException` escape | Dev sees a stack trace, not a message | Catch → `DeployException` with the raw value |
| Validating in `deploy()` caller instead of parse | Duplicated logic | Validate inside the parse loop |
| Counting only parsed lines for line numbers | Misleading "invalid line 1" | Count all file lines (1-based), skip comment/blank |

---

## 8. Verification Checklist
### Functional
- [ ] Full valid config parses to correct `DeployResult`
- [ ] Defaults applied when region/parallel/timeout missing
- [ ] Whitespace tolerated around keys, values, and target entries
- [ ] Comments and blank lines ignored
- [ ] `parallel=TRUE` accepted
- [ ] Unknown keys ignored

### Sad Paths
- [ ] Missing file → `config file not found: <path>`
- [ ] Empty file / missing `target` / `target=` → `missing required key: target`
- [ ] Malformed line → message contains line number and raw text
- [ ] `timeout=abc` / `0` / `-5` → `invalid timeout` with the raw value
- [ ] `parallel=maybe` → `invalid boolean` with the raw value
- [ ] No raw `NumberFormatException` or `NPE` escapes

### Test Invocation
```bash
./gradlew test --tests "com.danipl.practise.refactoring.legacydeploytool.*"
```

---

## 9. Extension Points (Bonus Discussion)
- **Duplicate targets** — should `target=webapp,webapp` dedupe or fail? (Design decision worth raising.)
- **Config precedence** — what if the same key appears twice? Last-wins vs first-wins vs fail.
- **Real deployment** — the tool stops at the plan today; a real tool would invoke `ProcessBuilder` (that's exactly challenge 3, `gitwrapper`).
- **Strict mode** — a `--strict` flag that makes unknown keys an error for CI.
- **Writing configs back** — atomic write via `Files.write` + `Files.move(ATOMIC_MOVE)`.

---

## 10. Production References
| Resource | Why It Matters |
|----------|---------------|
| Effective Java (Bloch) | Item 72/73: exception translation — never let low-level exceptions leak |
| Clean Code (Martin) | Small functions; the god-method refactor is the whole point |
| `java.nio.file.Files` javadoc | `readAllLines`, `readString`, `lines` |

---

*This guideline follows the standard practise-coach template: presentation → edge cases → chain of thinking → communication → implementation → pro tips → mistakes → verification → extensions → references.*
