# Chain of Responsibility Pattern

## Overview

**Definition**: Chain of Responsibility lets you pass requests along a chain of handlers. Upon receiving a request, each
handler decides either to process it or to pass it to the next handler in the chain.

**Core Problem**: How to decouple the sender of a request from its receiver when multiple objects could handle the
request, and the handler is determined at runtime.

**One-Line Interview Answer**: "Chain of Responsibility builds a pipeline of handlers where each handler either
processes the request or forwards it to the next, enabling flexible request processing without coupling the sender to a
specific handler."

## Problem Statement

### Real-World Scenario: Request Validation Pipeline

A web application must validate, sanitize, authenticate, authorize, and rate-limit every incoming HTTP request. The
naive approach chains these checks explicitly:

```java
public class RequestHandler {
    public Response handle(Request request) {
        if (!isValid(request)) {
            return new Response(400, "Bad Request");
        }
        if (!sanitize(request)) {
            return new Response(400, "Malicious content detected");
        }
        if (!authenticate(request)) {
            return new Response(401, "Unauthorized");
        }
        if (!authorize(request)) {
            return new Response(403, "Forbidden");
        }
        if (!rateLimit(request)) {
            return new Response(429, "Too Many Requests");
        }
        // Business logic
        return process(request);
    }

    // Adding a new filter means modifying this method
    // Reordering filters means modifying this method
    // Skipping certain filters for certain endpoints is messy
}
```

### Pain Points of the Naive Approach

1. **Hardcoded Pipeline**: The chain is fixed in code. Reordering, adding, or removing a step requires editing the
   `handle()` method.
2. **No Conditional Skipping**: You can't easily skip certain steps for specific requests (e.g., skip authentication for
   public endpoints).
3. **Single Responsibility Violation**: The handler knows about validation, sanitization, authentication, authorization,
   rate limiting, AND business logic.
4. **No Dynamic Configuration**: The pipeline cannot be configured at runtime (e.g., enable/disable logging).
5. **Testing Nightmare**: Testing one concern requires setting up all upstream concerns.

### Why This Matters in Production

All major web frameworks use this pattern: Servlet Filters, Spring Security Filter Chain, ASP.NET Core Middleware,
Express.js Middleware. Without it, adding cross-cutting concerns (logging, authentication, compression, caching) would
require touching every endpoint handler.

## Solution

### How Chain of Responsibility Solves This

Each handler implements a common interface with a `handle()` method. Handlers are linked into a chain. Each handler
processes the request or passes it to the next. The sender only knows the first handler in the chain.

### Key Participants

| Participant           | Role                                                      |
|-----------------------|-----------------------------------------------------------|
| `Handler` (interface) | Declares `handle()` method and method to set next handler |
| `ConcreteHandler`     | Processes requests it can handle; forwards others         |
| `Client`              | Initiates the request to the first handler in the chain   |

### Step-by-Step Flow

1. Client assembles the chain (or a builder does)
2. Client sends request to the first handler
3. Handler processes what it can, then calls `next.handle(request)`
4. Each successive handler repeats until one handles it completely or the chain ends
5. The chain can be terminating (one handler processes, stops) or non-terminating (all handlers process, e.g., logging)

### UML-Style Structure

```
┌──────────┐     ┌──────────────────────┐
│  Client  │─────│  «interface» Handler │
└──────────┘     │                      │
                 │ +setNext(Handler)    │
                 │ +handle(Request)     │
                 └─────────┬────────────┘
                           │
              ┌────────────┼────────────┐
              │            │            │
     ┌────────┴───┐ ┌──────┴─────┐ ┌───┴────────┐
     │Concrete    │ │Concrete    │ │Concrete    │
     │HandlerA    │ │HandlerB    │ │HandlerC    │
     │            │ │            │ │            │
     │+handle()   │→│+handle()   │→│+handle()   │→ null
     └────────────┘ └────────────┘ └────────────┘
```

## Java Implementation

### Handler Interface

```java
package behavioral.chainofresponsibility;

import java.util.Optional;

interface RequestHandler {
    Response handle(Request request);

    // Helper to chain handlers
    default RequestHandler andThen(RequestHandler next) {
        return request -> {
            Response response = this.handle(request);
            if (response == null) {
                return next.handle(request);
            }
            return response;
        };
    }
}

record Request(String path, String method, String body, String authToken, String ip) {}
record Response(int statusCode, String body) {}
```

### Concrete Handlers

```java
class ValidationHandler implements RequestHandler {
    @Override
    public Response handle(Request request) {
        if (request.path() == null || request.path().isBlank()) {
            return new Response(400, "Invalid path");
        }
        if (request.method() == null) {
            return new Response(400, "HTTP method required");
        }
        System.out.println("[VALIDATION] Request is valid");
        return null; // Pass to next handler
    }
}

class AuthenticationHandler implements RequestHandler {
    @Override
    public Response handle(Request request) {
        // Public endpoints skip auth
        if (request.path().startsWith("/public/")) {
            System.out.println("[AUTH] Public endpoint — skipping auth");
            return null;
        }
        if (request.authToken() == null || request.authToken().isBlank()) {
            return new Response(401, "Authentication required");
        }
        // Validate JWT token (simplified)
        if (!request.authToken().startsWith("eyJ")) {
            return new Response(401, "Invalid token format");
        }
        System.out.println("[AUTH] Authenticated successfully");
        return null;
    }
}

class AuthorizationHandler implements RequestHandler {
    private static final java.util.Set<String> ADMIN_PATHS = java.util.Set.of("/admin");

    @Override
    public Response handle(Request request) {
        boolean needsAdmin = ADMIN_PATHS.stream().anyMatch(p -> request.path().startsWith(p));
        if (needsAdmin && !request.authToken().contains("admin")) {
            return new Response(403, "Admin access required");
        }
        System.out.println("[AUTHORIZATION] Access granted");
        return null;
    }
}

class RateLimitHandler implements RequestHandler {
    private final java.util.Map<String, Integer> requestCounts = new java.util.concurrent.ConcurrentHashMap<>();
    private static final int MAX_REQUESTS = 10;

    @Override
    public Response handle(Request request) {
        int count = requestCounts.merge(request.ip(), 1, Integer::sum);
        if (count > MAX_REQUESTS) {
            return new Response(429, "Rate limit exceeded");
        }
        System.out.println("[RATE-LIMIT] Request " + count + " from " + request.ip());
        return null;
    }
}

class LoggingHandler implements RequestHandler {
    @Override
    public Response handle(Request request) {
        System.out.printf("[LOG] %s %s from %s%n",
            request.method(), request.path(), request.ip());
        return null; // Always passes through
    }
}
```

### Terminal Handler (Business Logic)

```java
class BusinessLogicHandler implements RequestHandler {
    @Override
    public Response handle(Request request) {
        System.out.println("[BUSINESS] Processing " + request.path());
        return new Response(200, "Hello, " + request.path() + "!");
    }
}
```

### Chain Builder

```java
import java.util.ArrayList;
import java.util.List;

class FilterChain {
    private final List<RequestHandler> handlers = new ArrayList<>();

    public FilterChain addHandler(RequestHandler handler) {
        handlers.add(handler);
        return this;
    }

    public Response execute(Request request) {
        // Build chain: each handler calls the next if it returns null
        RequestHandler chain = request1 -> {
            for (var handler : handlers) {
                Response response = handler.handle(request1);
                if (response != null) {
                    return response;
                }
            }
            return null; // No handler processed the request
        };
        return chain.handle(request);
    }
}
```

### Java Functional Chain (Using `UnaryOperator`)

```java
import java.util.function.UnaryOperator;

class FunctionalFilterChain {
    private UnaryOperator<RequestHandler> pipeline = handler -> handler;

    public FunctionalFilterChain addFilter(UnaryOperator<RequestHandler> filter) {
        pipeline = pipeline.andThen(filter);
        return this;
    }

    public RequestHandler compose(RequestHandler terminalHandler) {
        return pipeline.apply(terminalHandler);
    }
}
```

### Usage Demo

```java
public class ChainOfResponsibilityDemo {
    public static void main(String[] args) {
        // Build chain using builder
        var chain = new FilterChain()
            .addHandler(new LoggingHandler())
            .addHandler(new ValidationHandler())
            .addHandler(new AuthenticationHandler())
            .addHandler(new AuthorizationHandler())
            .addHandler(new RateLimitHandler())
            .addHandler(new BusinessLogicHandler());

        // Test requests
        var publicReq = new Request("/public/health", "GET", null, null, "192.168.1.1");
        System.out.println("=== Public request ===");
        System.out.println(chain.execute(publicReq));

        var adminReq = new Request("/admin/users", "GET", null, "eyJ.xxx.admin", "10.0.0.1");
        System.out.println("\n=== Admin request ===");
        System.out.println(chain.execute(adminReq));

        var unauthReq = new Request("/api/data", "POST", "{}", null, "10.0.0.2");
        System.out.println("\n=== Unauthenticated request ===");
        System.out.println(chain.execute(unauthReq));

        var badReq = new Request("", "POST", null, "eyJ.xxx", "10.0.0.3");
        System.out.println("\n=== Bad request ===");
        System.out.println(chain.execute(badReq));

        // Build chain using functional composition
        System.out.println("\n=== Functional chain ===");
        RequestHandler functionalChain = ((RequestHandler) new LoggingHandler())
            .andThen(new ValidationHandler())
            .andThen(new AuthenticationHandler())
            .andThen(new AuthorizationHandler())
            .andThen(new RateLimitHandler())
            .andThen(new BusinessLogicHandler());

        System.out.println(functionalChain.handle(
            new Request("/public/test", "GET", null, null, "1.2.3.4")));
    }
}
```

### Servlet Filter Chain Example (Conceptual)

```java
// This mirrors the javax.servlet.Filter API
interface Filter {
    void doFilter(Request request, FilterChain chain);
}

class FilterChainImpl implements FilterChain {
    private final List<Filter> filters = new ArrayList<>();
    private int currentIndex = 0;

    public void addFilter(Filter filter) {
        filters.add(filter);
    }

    public void doFilter(Request request, Response response) {
        if (currentIndex < filters.size()) {
            Filter filter = filters.get(currentIndex++);
            filter.doFilter(request, this); // Recursive call!
        } else {
            // Terminal servlet
            System.out.println("[SERVLET] Handling request: " + request.path());
        }
    }
}
```

## When to Use

1. **Middleware Pipelines**: Servlet Filters, Spring Security Filter Chain, Express/Connect middleware. Each middleware
   component handles one concern (CORS, compression, auth, logging).

2. **Validation Chains**: Form validators where each handler validates one field (email format, password strength, age >
   18). Handlers can be combined differently per form.

3. **Logging & Monitoring**: A chain of log appenders (console → file → remote server). Each handler decides whether to
   forward the log entry.

4. **Event Processing Pipelines**: Kafka consumer interceptors where each interceptor can modify, filter, or enrich the
   record before processing.

5. **Support Ticket Escalation**: Tier 1 support handles basic issues; tier 2 handles advanced; tier 3 handles critical.
   A ticket is passed up the chain.

### Framework Examples

- **Jakarta Servlet `Filter` / `FilterChain`**: The canonical example. Filters are chained; each calls
  `chain.doFilter()` to continue.
- **Spring Security's `SecurityFilterChain`**: A chain of security filters (CsrfFilter,
  UsernamePasswordAuthenticationFilter, ExceptionTranslationFilter, etc.).
- **Apache Commons Chain**: A framework implementing Chain of Responsibility for command processing.
- **Java Logging `Handler`**: Log records pass through a chain of handlers (ConsoleHandler, FileHandler, SocketHandler).

## When NOT to Use

1. **Single Handler Always**: If only one handler ever processes the request, use direct delegation or Strategy. Chain
   adds unnecessary indirection.

2. **All Handlers Always Execute**: If every handler always runs (no conditional forwarding), use a simple loop or
   `List.forEach()`. The chain pattern's forwarding logic is wasted.

3. **Handlers Need to Mutate Shared State**: If handlers share mutable state in complex ways, the chain becomes hard to
   reason about. Consider Mediator or Event Bus.

4. **Guaranteed Processing Order Matters**: The chain implies sequential processing, but if handlers must run in strict
   priority order regardless of whether they handle, a simple list of ordered handlers is clearer.

5. **Deep Chains with High Throughput**: 50+ handlers with millions of requests. Each handler call adds stack or
   iteration overhead. Consider compiling the chain into a single method.

## Interview Questions

### Q1: Explain the Chain of Responsibility pattern and how it differs from a simple list of handlers.

**Answer**: Chain of Responsibility defines a linked sequence where each handler decides to process or forward the
request. Unlike a simple list where all handlers always run, each handler in a CoR can terminate the chain by returning
a response. The request flows until one handler handles it.

### Q2: How does Servlet Filter chain work?

**Answer**: The `FilterChain` holds an ordered list of `Filter` objects. Each `Filter.doFilter()` calls
`chain.doFilter()` to pass to the next filter. The last element is the target servlet. This allows each filter to
execute logic before AND after the servlet (by wrapping the chain call with pre/post code).

### Q3: How does Chain of Responsibility support the Open/Closed Principle?

**Answer**: New handlers are added without modifying existing handlers or the chain assembly code. The chain is open for
extension (add handlers) and closed for modification (existing handlers untouched). The client that sends the request
doesn't change when the chain grows.

### Q4: What's the difference between Chain of Responsibility and Decorator?

**Answer**: CoR forwards the request along the chain; each handler can terminate processing. Decorator wraps an object,
adding behavior before/after the wrapped object's method. CoR is about "who processes this?" (selection). Decorator is
about "what behavior is added?" (enhancement).

### Q5: How would you implement a chain where some handlers should always run (logging) and some are conditional (auth)?

**Answer**: Use a two-chain approach: a "mandatory" chain for logging/auditing that never terminates, wrapping a "
conditional" chain that terminates on first match. Alternatively, handlers return a `Result` enum: `CONTINUE`,
`HANDLED`, `TERMINATE_CHAIN`.

### Q6: What are the thread-safety concerns with Chain of Responsibility?

**Answer**: Handlers often share state (rate limit counters, caches). If handlers are singletons, they must be
thread-safe. Use `ConcurrentHashMap`, atomic counters, or thread-local storage. The chain itself is typically
request-scoped, so the iteration is single-threaded per request.

### Q7: How does Chain of Responsibility relate to the Single Responsibility Principle?

**Answer**: Each handler has exactly one responsibility: validation, authentication, logging, etc. SRP is naturally
enforced because if a handler does too much, you split it into separate handlers in the chain. This is a key advantage —
the pattern encourages fine-grained, single-purpose classes.

### Q8: How would you dynamically build a chain based on configuration?

**Answer**: Use a ChainBuilder that reads a config file (YAML, JSON) listing handler class names and their order. Use
reflection or a `Map<String, Supplier<Handler>>` registry to instantiate handlers. Spring can inject the chain via
`@Configuration` that assembles beans in order.

### Follow-Up Question

**Interviewer**: "How would you implement a chain that supports asynchronous handlers?"

**Answer**: Each handler returns `CompletableFuture<Response>` instead of `Response`. The chain calls
`handler.handle(request).thenCompose(response -> { if (response == null) return next.handle(request); else return CompletableFuture.completedFuture(response); })`.
This enables non-blocking I/O in each handler without blocking the chain's thread.

## Pros & Cons

### Advantages

- **Decouples Sender from Receiver**: The sender only knows the first handler
- **Dynamic Pipeline**: Handlers can be added, removed, or reordered at runtime
- **Single Responsibility**: Each handler focuses on one concern
- **Open/Closed Principle**: Extend processing by adding handlers, not modifying code
- **Conditional Processing**: Handlers decide whether to process or skip
- **Fine-Grained Control**: Each handler can execute logic before AND after the next handler

### Disadvantages

- **No Guarantee of Handling**: A request may fall through the entire chain unhandled (add a terminal handler!)
- **Debugging Difficulty**: Tracing which handler processed a request can be hard
- **Performance Overhead**: Each handler adds method call overhead; deep chains are slower
- **Order Sensitivity**: Handler order is critical and not enforced by the pattern itself
- **Handler Duplication**: Two handlers might both try to handle the same request if ordering is wrong

## Related Patterns

### Chain of Responsibility vs Decorator

**Decorator** wraps an object recursively, adding behavior before/after method calls. **CoR** links handlers linearly,
each deciding to process or pass. Decorator always calls the wrapped object; CoR can terminate. Use Decorator for
augmenting behavior; use CoR for routing/filtering.

### Chain of Responsibility vs Command

**Command** encapsulates a single request. **CoR** routes a request to the right handler. A Command can be passed
through a CoR chain where each handler decides whether to execute it. CoR answers "who handles this?"; Command answers "
what action to perform?"

### Chain of Responsibility vs Observer

**Observer** broadcasts an event to ALL subscribers. **CoR** forwards a request until ONE handler processes it. Observer
is 1-to-N broadcast. CoR is 1-to-1 routing. A logging Observer subscribes to all events; a CoR error handler catches the
exception that matches its type.

## Key Takeaways

1. **"Pass the buck" pattern** — Each handler can handle or forward. The request flows until someone handles it.

2. **Middleware is CoR** — Every web framework's middleware/filter/interceptor chain is Chain of Responsibility.
   Mentioning servlet filters in an interview is a strong signal.

3. **OCP demonstration** — Adding a compression filter to a chain of 20 existing filters requires zero changes to those
   filters.

4. **Watch for fall-through** — Always terminate the chain with a default handler that returns a 404/error, or the
   request goes unhandled.

5. **Interview memory aid** — "CoR = handler chain, each decides process-or-forward, middleware pattern, OCP."
