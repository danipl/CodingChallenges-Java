# Challenge: GitWrapper - Guidelines

## 1. Challenge Presentation

### What You're Building

A DevEx utility that wraps git so developers get the *essential* state of a repository (`branch`, `ahead/behind`,
changed files) without reading raw porcelain output. Internal tools at CloudBees-class companies are almost always thin
wrappers around git/docker/shell — this is the skill in its purest form.

### Core Contract

```
[repoDir] --status()--> [git status --porcelain --branch] --parse--> [GitState]
[repoDir] --shortSha()--> [git rev-parse --short HEAD] --trim--> [String]
```

### Interface Summary

| Method                       | Purpose                                                                                      |
|------------------------------|----------------------------------------------------------------------------------------------|
| `status(Path)`               | Run `git status --porcelain --branch`, parse into `GitState(branch, ahead, behind, changes)` |
| `shortSha(Path)`             | Run `git rev-parse --short HEAD`, return trimmed SHA                                         |
| `of()` / `of(CommandRunner)` | Default (real ProcessBuilder) vs injected runner                                             |
| `CommandRunner`              | Pluggable execution — the seam for tests                                                     |
| `ProcessBuilderRunner`       | The real `ProcessBuilder` implementation                                                     |

### What Interviewers Evaluate

1. **ProcessBuilder fluency** — command as `List<String>`, working dir, stream capture without deadlock
2. **Robustness** — exit codes, timeouts, missing executable, all translated to actionable messages
3. **Testability by design** — the `CommandRunner` seam proves you think about tests *before* writing code

---

## 2. Edge & Corner Cases

### How to Identify Them

Ask: *"What can git do, what can the OS do, and what does the developer see for each?"*

| #  | Edge Case                    | How It Surfaces                          | How to Handle                                           |
|----|------------------------------|------------------------------------------|---------------------------------------------------------|
| 1  | Command fails (`exit != 0`)  | Not a git repo → exit 128                | `CommandFailedException` with trimmed stderr in message |
| 2  | git not installed            | `ProcessBuilder.start()` → `IOException` | Friendly "could not run git: ..." message               |
| 3  | Timeout                      | Repo on slow NFS, huge status            | `waitFor(timeout, TimeUnit)` → destroy → fail           |
| 4  | **Pipe deadlock**            | Reading streams *after* `waitFor()`      | Read stdout/stderr **before** `waitFor()`               |
| 5  | Empty output                 | Unexpected git version/behavior          | Fail with "unexpected output" — never NPE               |
| 6  | First line isn't `## branch` | Detached HEAD / corrupted output         | Fail loudly                                             |
| 7  | Rename `R  old -> new`       | Porcelain reports the arrow form         | Preserve the whole path token                           |
| 8  | `[ahead 1, behind 4]`        | Both directions                          | Parse both, default missing to 0                        |
| 9  | Working dir doesn't exist    | `directory()` points nowhere             | Let start() fail → friendly message                     |
| 10 | Interrupted wait             | Thread interrupted                       | Restore interrupt flag, fail                            |

### Quick Pre-Implementation Checklist

```
▢ Command built as List<String>, never a hand-quoted string?
▢ Streams read BEFORE waitFor?
▢ Timeout path destroys the process?
▢ Every throw carries a developer-readable message?
```

---

## 3. First Approach - Chain of Thinking

### Minute 0-2: Clarify Requirements

Ask the interviewer:

- "What does the developer need: full status or a compact summary?"
- "What should happen when git is missing?"
- "Should I handle timeouts, or is a hang acceptable?"

### Minute 2-5: Design

- `ProcessBuilderRunner.run()`: build `ProcessBuilder(command).directory(dir)`, start, **read both streams**, then
  `waitFor(timeout)`. If not finished → `destroyForcibly()` + throw.
- `status()`: build command, run, check exit code, parse output line by line.
- Parsing: first line `## <branch>[...<upstream>] [ahead N, behind M]`, rest are `XY path`.

### Minute 5-10: Sketch the Core Flow

```
status(repoDir):
    result = runner.run(["git","status","--porcelain","--branch"], repoDir, TIMEOUT)
    if result.exitCode() != 0:
        throw CommandFailedException("git status failed (exit N): " + stderr, N, stdout, stderr)
    return parseStatus(result.stdout())

parseStatus(output):
    lines = output.lines()
    first = first line; if missing or !startsWith("##") -> "unexpected output"
    branch = parse branch token; ahead/behind from [ahead N, behind M]
    changes = remaining lines -> substring(3) (skip "XY ")
```

### Minute 10-25: Implement

Order: `ProcessBuilderRunner` → `status` parsing → `shortSha` → error translation.

---

## 4. Communication Approach During the Interview

### What to Say Out Loud

| Moment               | Say This                                                                                                                  |
|----------------------|---------------------------------------------------------------------------------------------------------------------------|
| Building the command | "I'm passing the command as a list so no quoting/escaping bugs — that's why ProcessBuilder exists."                       |
| On streams           | "I read stdout and stderr before waitFor — otherwise a full pipe buffer deadlocks the process."                           |
| On exit codes        | "Exit code 128 from git means 'not a repository' — the dev should see git's own message, plus what I ran."                |
| On timeout           | "I destroy the process forcibly after the timeout — a hung git must not hang the developer's terminal."                   |
| On the seam          | "CommandRunner is injectable so I can test parsing without a real git install — and swap in a real runner in production." |

### When Stuck

```
I notice `git status --porcelain --branch` output has several formats
([ahead 2], [behind 3], [ahead 1, behind 4], plain).
The risk is parsing brittlely against one format.
Two options: [A] regex per case, [B] split on delimiters and default to 0.
I'll go with [B] because it degrades gracefully. Does that match your expectation?
```

---

## 5. Implementation Structure

```java
public final class GitWrapperImpl implements GitWrapper {
    // status(Path)          -> run + translate exit code + parse
    // shortSha(Path)        -> run + translate exit code + trim
    // parseStatus(String)   -> GitState (private)
    // parseBranchLine(String) -> branch + ahead/behind (private)

    static final class ProcessBuilderRunner implements CommandRunner {
        // run(...) -> start, read streams, waitFor(timeout), translate
    }
}
```

### Key Implementation Pattern

```java
Process process = new ProcessBuilder(command)
        .directory(workingDir.toFile())
        .start();

String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);

if (!process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)) {
    process.destroyForcibly();
    throw new CommandFailedException("command timed out after " + timeoutMillis + "ms");
}
return new CommandResult(process.exitValue(), stdout, stderr);
```

---

## 6. Technical Pro Tips

### The Deadlock Trap

| Wrong                         | Right                                       |
|-------------------------------|---------------------------------------------|
| `waitFor()` then read streams | Read streams (drain pipes) then `waitFor()` |
| Reading stdout but not stderr | Drain **both** — stderr fills too           |

### InterruptedException

Always restore the flag: `Thread.currentThread().interrupt()` before failing — a swallowed interrupt breaks the caller's
shutdown logic.

### What Senior Engineers Demonstrate

1. **Fail with context** — include *what you ran* and *what git said* in every error.
2. **No raw exceptions** — `IOException` from start () becomes "could not run git: <msg>", never a stack trace.
3. **Timeout is a first-class citizen** — tools that hang are worse than tools that fail.

---

## 7. Common Mistakes to Avoid

| Mistake                                       | Why It Fails                        | Fix                                                       |
|-----------------------------------------------|-------------------------------------|-----------------------------------------------------------|
| Reading streams after `waitFor()`             | Deadlock on large output            | Read before wait                                          |
| Building command as one string                | Quoting/escaping bugs               | `List.of("git", "status", ...)`                           |
| Ignoring stderr                               | Loses git's own diagnostic          | Capture both, include stderr on failure                   |
| `process.waitFor()` without timeout           | Hangs forever on NFS                | `waitFor(timeout, TimeUnit)`                              |
| Forgetting `destroyForcibly()`                | Zombie processes                    | Destroy on timeout                                        |
| Parsing `## main...origin/main` as the branch | Branch becomes "main...origin/main" | Stop at `...` or ` [`                                     |
| Catching `Exception` broadly in runner        | Hides InterruptedException          | Catch `IOException` and `InterruptedException` explicitly |

---

## 8. Verification Checklist

### Functional

- [ ] `status` parses clean branch, ahead-only, behind-only, both
- [ ] Changed/untracked/staged/renamed paths extracted correctly
- [ ] `shortSha` returns trimmed SHA
- [ ] Commands built exactly as `List.of("git", ...)` in the repo dir

### Process Safety

- [ ] Streams read before `waitFor`
- [ ] Timeout destroys process and fails with timeout message
- [ ] Non-zero exit → message includes exit code + stderr
- [ ] Missing executable → friendly "could not run" message
- [ ] Unexpected/empty output → explicit failure, no NPE

### Test Invocation

```bash
./gradlew test --tests "com.danipl.practise.cli.gitwrapper.*"
```

---

## 9. Extension Points (Bonus Discussion)

- **Streaming output** — `process.getInputStream().lines()` for long-running commands, with a `Consumer<String>`
  callback.
- **`ProcessHandle`** — kill process trees (`process.toHandle().descendants()`) instead of just the parent.
- **More commands** — `git log --oneline -5`, `git diff --stat`, `git fetch` — same wrapper, new command lists.
- **Environment control** — `pb.environment().put("GIT_TERMINAL_PROMPT", "0")` to prevent hangs on credential prompts.
- **Virtual threads** — Java 21: run many wrappers concurrently cheaply.

---

## 10. Production References

| Resource                          | Why It Matters                                               |
|-----------------------------------|--------------------------------------------------------------|
| `ProcessBuilder` javadoc          | `directory()`, `redirectErrorStream()`, stream plumbing      |
| `Process.waitFor(long, TimeUnit)` | Timeout-aware wait                                           |
| Effective Java (Bloch)            | Item 72/73: exception translation to the abstraction's level |
| `git status --porcelain` docs     | The exact output format the parser must handle               |

---

*This guideline follows the standard practise-coach template: presentation → edge cases → chain of thinking →
communication → implementation → pro tips → mistakes → verification → extensions → references.*
