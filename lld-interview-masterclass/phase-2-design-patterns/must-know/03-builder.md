# Builder Pattern

> Solves: too many constructor parameters + object validation during construction.

## Why?

When objects have many optional parameters, telescoping constructors become unmaintainable.

## Where?

- **gRPC**: Generated message builders
- **Java**: `StringBuilder`, `Stream.Builder`
- **OkHttp**: `Request.Builder`, `OkHttpClient.Builder`
- **Lombok**: `@Builder` annotation everywhere

## How

```java
public class User {
    // Required
    private final String email;
    private final String name;
    // Optional
    private final String phone;
    private final String address;
    private final int age;

    private User(Builder builder) {
        this.email = builder.email;
        this.name = builder.name;
        this.phone = builder.phone;
        this.address = builder.address;
        this.age = builder.age;
        validate();
    }

    private void validate() {
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Invalid email");
        }
        if (age < 0 || age > 150) {
            throw new IllegalArgumentException("Invalid age");
        }
    }

    public static class Builder {
        // Required
        private final String email;
        private final String name;
        // Optional
        private String phone;
        private String address;
        private int age;

        public Builder(String email, String name) {
            this.email = email;
            this.name = name;
        }

        public Builder phone(String phone) { this.phone = phone; return this; }
        public Builder address(String address) { this.address = address; return this; }
        public Builder age(int age) { this.age = age; return this; }

        public User build() { return new User(this); }
    }
}

// Usage
User user = new User.Builder("user@email.com", "John")
    .phone("+1234567890")
    .age(30)
    .build();
```

## Interview Application

- **HTTP Request builder**: URL, headers, body, method, timeout
- **SQL Query builder**: SELECT, WHERE, JOIN, ORDER BY, LIMIT
- **Configuration object**: Required + many optional settings

## Key Interview Points

1. Builder enables **validation at build time**, not after
2. Resulting object is **immutable** (all fields final)
3. Required params in Builder constructor, optional via methods
4. Fluent interface (return `this`) enables chaining
