# Challenge: BuildStatusReporter - Guidelines

## 1. Challenge Presentation

### What You're Building

Developers complain that checking build status requires opening a browser, logging in, and clicking through a dashboard.
You're building a small CLI utility for a DevEx team: fetch build statuses from a mock API and print a clean, readable
console report.

### Core Contract

```
[Source] --fetch()--> [BuildStatus...] --render()--> [console table]
```

### Interface Summary

| Method                    | Purpose                                                           |
|---------------------------|-------------------------------------------------------------------|
| `report(source)`          | Fetch via source, render, translate failures into friendly errors |
| `render(builds)`          | Build a fixed-width aligned console table, newest first           |
| `formatDuration(seconds)` | Human-readable duration: `45s`, `12m 34s`, `1h 05m`               |

### What Interviewers Evaluate

1. **DevEx mindset** — Is every failure path a *readable message*, not a stack trace?
2. **Pragmatism** — Do you build the minimal thing that solves the dev's problem?
3. **Modern Java** — Records, Streams, `String.format`/text blocks, no boilerplate.

---

## 2. Edge & Corner Cases

### How to Identify Them

Ask: *"What can the source throw? What can the list contain? What breaks alignment?"*

| # | Edge Case            | How It Surfaces           | How to Handle                                |
|---|----------------------|---------------------------|----------------------------------------------|
| 1 | Empty build list     | Source returns `[]`       | Render `No builds found.`                    |
| 2 | Null build list      | Mock API returns null     | Treat as empty, no NPE                       |
| 3 | Null entries in list | One malformed build       | Skip it, render the rest                     |
| 4 | Source throws        | API unreachable           | `Error: could not fetch build status: <msg>` |
| 5 | Negative duration    | Corrupt data              | `IllegalArgumentException`                   |
| 6 | Duration boundaries  | 59s / 60s / 3599s / 3600s | Exact `m ss` and `h mm` zero-padding         |
| 7 | Equal start times    | Sort instability          | Keep stable order (don't swap equal items)   |

### Quick Pre-Implementation Checklist

```
▢ Do I know the exact table column format (spacing, padding)?
▢ Have I decided which exceptions are caught vs. propagated?
▢ Is sorting stable and newest-first?
```

---

## 3. First Approach - Chain of Thinking

### Minute 0-2: Clarify Requirements

Ask the interviewer:

- "What should the developer see on success? A table? A summary line?"
- "What does the API return when there are no builds — empty list or error?"
- "Should a total/summary be included, or just the table?"

### Minute 2-5: Design

- `render`: build rows from the record fields, compute max width per column, pad with
  `String.format("%-" + width + "s", cell)`, join with `"  "`.
- `formatDuration`: three branches — `<60`, `<3600`, `else`.
- `report`: single `try/catch` around `source.fetch()`, translate `BuildStatusException` only.

### Minute 5-10: Sketch the Core Flow

```
report(source):
    try:
        builds = source.fetch()
        return render(builds)
    catch BuildStatusException e:
        return "Error: could not fetch build status: " + e.getMessage()

render(builds):
    if empty/null -> "No builds found."
    filter nulls, sort by startedAt desc
    compute column widths, build header + rows
```

### Minute 10-25: Implement

Order: `formatDuration` first (simplest, deterministic) → `render` → `report`.

---

## 4. Communication Approach During the Interview

### What to Say Out Loud

| Moment          | Say This                                                                                                                              |
|-----------------|---------------------------------------------------------------------------------------------------------------------------------------|
| Starting        | "Let me clarify what the developer sees: a table with build ID, project, status, duration, SHA, and author — newest first."           |
| On sorting      | "I'll sort by start time descending, stable, so equal times keep input order."                                                        |
| On the sad path | "If the API is down, the dev shouldn't see an exception — they should see 'Error: could not fetch build status: connection refused'." |
| On catch scope  | "I only catch `BuildStatusException` — a programming bug shouldn't be swallowed, it should fail loudly."                              |

### When Stuck

```
I notice the alignment breaks when the SHA column has varying lengths.
The risk is an unreadable table for real data.
Two options: [A] compute max width per column, [B] fixed widths.
I'll go with [A] because it adapts to any data. Does that match what you'd expect?
```

---

## 5. Implementation Structure

```java
public final class BuildStatusReporterImpl implements BuildStatusReporter {
    // formatDuration(long)        -> String
    // render(List<BuildStatus>)   -> String
    //   + private row formatting helpers
    // report(BuildStatusSource)   -> String
}
```

### Key Implementation Pattern

```java
private String padRight(final String s, final int width) {
    return String.format("%-" + width + "s", s);
}
```

Compute widths from a combined stream of header + all cell values per column.

---

## 6. Technical Pro Tips

### Streams vs. Loops

| Approach | When                                           |
|----------|------------------------------------------------|
| Streams  | Filtering nulls, sorting, computing max widths |
| Loops    | Building rows when you need index-aware access |

### What Senior Engineers Demonstrate

1. **Fail loudly on programming errors** — catch the source's domain exception, let `IllegalStateException` propagate.
2. **Minimal output, maximal signal** — no debug noise, one clean table.
3. **Deterministic formatting** — zero-padded durations so columns stay aligned.

---

## 7. Common Mistakes to Avoid

| Mistake                                        | Why It Fails                         | Fix                                   |
|------------------------------------------------|--------------------------------------|---------------------------------------|
| `catch (Exception e)` in `report`              | Hides programming bugs from the team | Catch only `BuildStatusException`     |
| Not handling null list/entries                 | NPE crashes the whole report         | Filter/short-circuit defensively      |
| `String` concatenation in a loop for the table | Ugly, error-prone alignment          | Compute widths once, pad via `format` |
| Truncating to `System.out.println` in `render` | Not testable                         | Return `String`, print at the caller  |
| Wrong zero-padding (`12m 34s` vs `12m 4s`)     | Tests fail on boundary values        | Use `%02d` for seconds/minutes        |

---

## 8. Verification Checklist

### Functional

- [ ] Single build renders header + one aligned row
- [ ] Multi-build output is sorted newest-first
- [ ] All five status badges render (`WAIT RUN PASS FAIL SKIP`)
- [ ] Durations: `0s`, `45s`, `1m 00s`, `12m 34s`, `1h 01m`, `2h 02m`
- [ ] Empty and null lists return `No builds found.`

### Sad Paths

- [ ] Source failure returns `Error: could not fetch build status: <msg>`
- [ ] Unexpected exceptions propagate untouched
- [ ] Null entries are skipped, remaining rows still render
- [ ] Negative duration throws `IllegalArgumentException`

### Test Invocation

```bash
./gradlew test --tests "com.danipl.practise.cli.buildstatus.*"
```

---

## 9. Extension Points (Bonus Discussion)

- **JSON parsing**: In reality the API returns JSON — how would you map it to the record with Jackson/Gson? (Follow-up
  challenge: `gitwrapper` covers ProcessBuilder.)
- **Color output**: ANSI codes for PASS/FAIL — but keep them optional for CI logs.
- **Pagination**: The API caps at 100 builds — how to page through and aggregate?
- **Polling/watch mode**: `--watch` flag re-fetching every N seconds.
- **Summary line**: Total builds, failure count, longest run — useful for dashboards.

---

## 10. Production References

| Resource                       | Why It Matters                                                                                      |
|--------------------------------|-----------------------------------------------------------------------------------------------------|
| Effective Java (Bloch)         | Item 73: "Throw exceptions appropriate to the abstraction" — the `BuildStatusException` translation |
| Clean Code (Martin)            | Small functions, meaningful names for the table renderer                                            |
| `java.lang.String.format` docs | `%-Ns` padding for column alignment                                                                 |

---

*This guideline follows the standard practise-coach template: presentation → edge cases → chain of thinking →
communication → implementation → pro tips → mistakes → verification → extensions → references.*
