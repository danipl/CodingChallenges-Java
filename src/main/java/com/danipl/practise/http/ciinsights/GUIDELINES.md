# Challenge: CiInsights - Guidelines

## 1. Challenge Presentation

### What You're Building

Developers keep asking *"why is CI slow?"* You're building a small DevEx utility: hit the CI API over HTTP, parse the
JSON builds, and print the answers — failure rate, slowest builds, flakiest services.

This is **Format 2 (CLI pairing)** with the two skills you haven't practiced yet: real HTTP (`java.net.http.HttpClient`)
and JSON parsing (Jackson). This is the **one deliberate exception** to the repo's no-third-party policy — the JDK has
no JSON parser, and real DevEx tools use Jackson.

### Core Contract

```
[CiInsights.of(baseUrl)]
    fetchBuilds()  --> GET {baseUrl}/builds --> List<Build>   (HTTP + Jackson)
    analyze(list)  --> Insights                          (pure computation)
    run()          --> fetchBuilds + analyze             (what the CLI calls)
```

### Interface Summary

| Method                                          | Purpose                                            |
|-------------------------------------------------|----------------------------------------------------|
| `of(baseUrl)`                                   | Factory; real HttpClient + ObjectMapper            |
| `fetchBuilds()`                                 | GET + parse; every failure → `CiApiException`      |
| `analyze(builds)`                               | Pure: failure rate, top-3 slowest, service ranking |
| `run()`                                         | Fetch + analyze in one call                        |
| `Build` / `Status` / `Insights` / `ServiceStat` | Records + enum                                     |
| `CiApiException`                                | The dev-facing error contract                      |

### What Interviewers Evaluate

1. **HTTP fluency** — `HttpClient`, `HttpRequest`, timeouts, status handling
2. **Jackson fluency** — records, `JavaTimeModule`, `FAIL_ON_UNKNOWN_FIELDS`
3. **Insight design** — the analysis rules are *decisions*: what counts as failure, what "slowest" means, how ties break
4. **Sad path** — dead API / 500 / malformed JSON → actionable message, never a raw leak

---

## 2. Edge & Corner Cases

### How to Identify Them

For HTTP: *"what can the server do to me?"* — die, 500, hang, send garbage. For analysis: *"what data shapes break my
rules?"*

| #  | Edge Case                   | How It Surfaces                          | How to Handle                                              |
|----|-----------------------------|------------------------------------------|------------------------------------------------------------|
| 1  | API unreachable             | `ConnectException` on send               | `CiApiException("could not reach CI API at <url>: <msg>")` |
| 2  | Non-2xx status              | 500 / 404 response                       | `CiApiException("CI API returned <code>: <body>")`         |
| 3  | Malformed JSON              | Jackson throws `JsonProcessingException` | Wrap with "malformed JSON from CI API: <msg>"              |
| 4  | Unknown JSON fields         | API adds a field                         | `FAIL_ON_UNKNOWN_FIELDS, false` — forward-compatible       |
| 5  | `Instant` in JSON           | ISO-8601 string needs `JavaTimeModule`   | `registerModule(new JavaTimeModule())`                     |
| 6  | Empty array                 | `[]` from API                            | Empty list → zeroed insights                               |
| 7  | No completed builds         | All RUNNING/QUEUED/SKIPPED               | failureRate 0.0 (0/0 defined as 0)                         |
| 8  | Fewer than 3 completed      | Only 1 finished build                    | Return what exists                                         |
| 9  | Service tie in failure rate | Two services 0.0                         | Break by name (deterministic)                              |
| 10 | Interrupted send            | `InterruptedException`                   | Restore flag, fail as CiApiException                       |

### Quick Pre-Implementation Checklist

```
▢ HttpClient has connect + request timeouts?
▢ ObjectMapper registers JavaTimeModule and ignores unknown fields?
▢ Every failure path produces a CiApiException with a message?
▢ Analysis rules are deterministic (ties broken)?
```

---

## 3. First Approach - Chain of Thinking

### Minute 0-2: Clarify Requirements

Ask the interviewer:

- "What does 'slow' mean — slowest build, or slowest service on average?"
- "What counts as a failure — only FAILED, or SKIPPED too?"
- "How many slowest builds should the CLI show — 3? 5?"

### Minute 2-5: Design

- **fetchBuilds**: build `HttpRequest` with timeout, `client.send(..., BodyHandlers.ofString())`, check
  `statusCode() == 200`, parse with mapper into `Build[]` (or `TypeReference<List<Build>>`).
- **analyze**: three passes — failure rate, slowest top-3, service aggregation. All Streams, all pure.
- **Error translation**: one `catch` per failure class in `fetchBuilds`; `analyze` throws nothing.

### Minute 5-10: Sketch the Core Flow

```
fetchBuilds():
    request = GET baseUrl + "/builds", timeout(10s)
    try: response = client.send(request, ofString())
    catch InterruptedException: restore flag, throw CiApiException
    catch IOException e: throw CiApiException("could not reach CI API at <url>", e)
    if statusCode != 200: throw CiApiException("CI API returned <code>: <body>")
    try: return mapper.readValue(body, Build[].class)
    catch JsonProcessingException e: throw CiApiException("malformed JSON...", e)

analyze(builds):
    completed = builds where status in (PASSED, FAILED)
    failureRate = failed.size() / completed.size()   // 0.0 if empty
    slowest = completed sorted by duration desc, limit 3
    services = group by service -> ServiceStat, sort by failureRate desc then name
```

### Minute 10-25: Implement

Order: `fetchBuilds` → `analyze` → `run`.

---

## 4. Communication Approach During the Interview

### What to Say Out Loud

| Moment              | Say This                                                                                                                                                      |
|---------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------|
| On HttpClient       | "I'll use the JDK's HttpClient — synchronous send with a 10s request timeout so a hung API doesn't hang the developer's terminal."                            |
| On Jackson          | "Two configs matter: `JavaTimeModule` so `Instant` deserializes from ISO-8601, and ignoring unknown fields so the API can evolve without breaking us."        |
| On status codes     | "Only 2xx is success — anything else I surface with the code and body so the dev can see what the API actually said."                                         |
| On analysis rules   | "Failure rate is over *completed* builds — RUNNING/SKIPPED would pollute it. Slowest means top 3 by duration. Ties break by name so output is deterministic." |
| On the record parse | "Jackson deserializes straight into the `Build` record — records make the mapper contract explicit."                                                          |

### When Stuck

```
I notice the analysis needs grouping by service and then ranking.
The risk is ordering instability between runs.
Two options: [A] group with Collectors.groupingBy, [B] manual map.
I'll go with [A] and add a deterministic tie-break. Does that align?
```

---

## 5. Implementation Structure

```java
public final class CiInsightsImpl implements CiInsights {
    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    // fetchBuilds()  -> HttpRequest + send + status check + parse
    // analyze(list)  -> three stream pipelines
    // run()          -> analyze(fetchBuilds())
}
```

### Key Implementation Pattern

```java
private static final ObjectMapper MAPPER = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_FIELDS, false);

HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/builds"))
        .timeout(Duration.ofSeconds(10))
        .GET()
        .build();

HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
```

---

## 6. Technical Pro Tips

### Jackson + Records + Time

| Gotcha                                         | Fix                                             |
|------------------------------------------------|-------------------------------------------------|
| `Instant` field → `InvalidDefinitionException` | `registerModule(new JavaTimeModule())`          |
| Unknown field → exception                      | `FAIL_ON_UNKNOWN_FIELDS, false`                 |
| `readValue` into List                          | `TypeReference<List<Build>>` or `Build[].class` |

### HttpClient Config

| Concern       | Fix                                                                      |
|---------------|--------------------------------------------------------------------------|
| Hung server   | `.timeout(Duration.ofSeconds(10))` on the request                        |
| Slow connect  | `HttpClient.newBuilder().connectTimeout(...)`                            |
| Sync vs async | `send()` sync for a CLI; `sendAsync()` + `CompletableFuture` if parallel |

### What Senior Engineers Demonstrate

1. **The analysis rules are named decisions**, not accidents — "completed only", "top 3", "ties by name".
2. **Streams, not loops** — grouping, sorting, limiting in a few lines.
3. **No raw exceptions cross the boundary** — Jackson and IO are both wrapped in CiApiException.

---

## 7. Common Mistakes to Avoid

| Mistake                                  | Why It Fails                | Fix                                  |
|------------------------------------------|-----------------------------|--------------------------------------|
| Forgetting `JavaTimeModule`              | `Instant` won't deserialize | Register it in the shared mapper     |
| Checking only `200` vs any 2xx           | 201/204 edge cases          | `statusCode() / 100 == 2`            |
| Letting `IOException` escape             | Dev sees a stack trace      | Wrap in CiApiException with the URL  |
| Reading `body()` without checking status | Parses error pages as JSON  | Check status first                   |
| Counting SKIPPED as failure              | Wrong failure rate          | Completed = PASSED ∪ FAILED          |
| No tie-break in ranking                  | Nondeterministic output     | Sort by rate desc, then name         |
| `analyze` doing I/O                      | Untestable                  | Keep it pure; `run()` does the fetch |

---

## 8. Verification Checklist

### HTTP

- [ ] Parses a JSON array from a real local server
- [ ] Empty array → empty list
- [ ] Unknown fields tolerated
- [ ] 500 → CiApiException with status code
- [ ] Malformed JSON → CiApiException mentioning JSON
- [ ] Unreachable API → CiApiException mentioning reach/connection

### Analysis

- [ ] Failure rate over completed builds only
- [ ] Zero rate when nothing completed
- [ ] Top 3 slowest completed, longest first
- [ ] Fewer than 3 → returns what exists
- [ ] Services ranked by failure rate desc, ties by name
- [ ] Empty list → zeroed insights
- [ ] Null list → NPE (fail fast)

### Test Invocation

```bash
./gradlew test --tests "com.danipl.practise.http.ciinsights.*"
```

---

## 9. Extension Points (Bonus Discussion)

- **Parallel fetching** — `sendAsync` + `CompletableFuture` for multiple endpoints.
- **Pagination** — `?page=` handling when the API caps results.
- **Retries** — exponential backoff on 5xx (the resilience pattern from the platform track).
- **Rendering** — a follow-on: format `Insights` as a console table or Markdown.
- **Rate limiting** — respect the API's rate limit header; 429 handling.

---

## 10. Production References

| Resource                           | Why It Matters                                               |
|------------------------------------|--------------------------------------------------------------|
| `java.net.http.HttpClient` javadoc | Request building, timeouts, body handlers                    |
| Jackson-databind docs              | Records, JavaTimeModule, FAIL_ON_UNKNOWN_FIELDS              |
| Effective Java (Bloch)             | Item 72/73: exception translation to the abstraction's level |

---

*This guideline follows the standard practise-coach template: presentation → edge cases → chain of thinking →
communication → implementation → pro tips → mistakes → verification → extensions → references.*
