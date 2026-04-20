# Java 21 String & Text Processing Guide

## Quick-Reference: Implementation Selection Matrix

| Use Case                           | Best Choice           | Key Reason                                    | Mutability | Thread-Safe | Performance | Memory    | When to Use                                 |
|------------------------------------|-----------------------|-----------------------------------------------|------------|-------------|-------------|-----------|---------------------------------------------|
| **Immutable text**                 | `String`              | Final, interned, constant pooling             | ❌          | ✅ (read)    | Fast read   | Shared    | Default choice for text that doesn't change |
| **Single-threaded building**       | `StringBuilder`       | Mutable, no sync overhead, O(1) append        | ✅          | ❌           | Fastest     | Efficient | Loops, concatenation, challenge solutions   |
| **Multi-threaded building**        | `StringBuffer`        | Synchronized methods, thread-safe             | ✅          | ✅ (slow)    | Slow        | Heavy     | Legacy code; avoid in challenges            |
| **Multi-line literals (Java 15+)** | Text Blocks (`"""`)   | Readable multi-line strings, no escapes       | ❌          | ✅ (read)    | Fast read   | Normal    | SQL, JSON, templates, formatted output      |
| **Pattern matching**               | `Pattern` + `Matcher` | Compiled regex, reusable, group extraction    | ❌          | ✅ (read)    | Varies      | Cached    | Validation, parsing, extraction             |
| **Immutable constant**             | `String.intern()`     | Pool reference, identity comparison with `==` | ❌          | ✅ (read)    | O(1)        | Shared    | Memory optimization for repeated strings    |

### At-A-Glance Decision Flow

```
Building string in a loop or multiple steps?
  ├─ YES → Single-threaded context?
  │          ├─ YES → StringBuilder (default choice)
  │          └─ NO  → StringBuffer (rare; use ConcurrentHashMap instead)
  └─ NO  → Multi-line literal (SQL, JSON, template)?
              ├─ YES → Text Block """...""" (Java 15+)
              └─ NO  → Plain String (default)
                        │
                        Need pattern matching / validation?
                          ├─ YES → Pattern.compile() + Matcher
                          └─ NO  → String methods (contains, indexOf, split, etc.)
```

---

## Overview

Java provides multiple approaches for string and text processing via `java.lang.String`, `java.lang.StringBuilder`,
`java.lang.StringBuffer`, and `java.util.regex`. Each is optimized for different mutability requirements, thread-safety
guarantees, and use cases.

This guide focuses on **coding challenge-relevant** patterns — the 20% of the String API you'll use in 80% of problems.

---

## 1. String (Immutable Text)

```java
String text = "hello";
```

### Characteristics

| Property        | Value                           |
|-----------------|---------------------------------|
| **Mutability**  | Immutable (final class)         |
| **Thread-safe** | Yes (cannot change)             |
| **Null-safe**   | No (throws NPE on null methods) |
| **Performance** | Fast reads, slow mutation       |
| **Memory**      | Shared via intern pool          |

### Why Immutability?

Strings are immutable for these reasons:

- **Security**: Prevents alteration after passing to sensitive APIs
- **Caching**: `hashCode()` cached after first computation (fast HashMap keys)
- **Interning**: Multiple references can share same heap instance
- **Thread-safety**: No synchronization needed for reads

### String Intern Pool

```java
String a = "hello";
String b = "hello";
String c = new String("hello");

a ==b        // true  (same pool reference)
a ==c        // false (different heap object)
a.

equals(c)   // true  (same content)
c.

intern()    // returns pool reference

a ==c.

intern()  // true
```

**Challenge relevance**: Identity comparison (`==`) works for interned strings but NEVER rely on it. Always use
`.equals()`.

### Complexity

| Operation            | Average | Notes                               |
|----------------------|---------|-------------------------------------|
| `charAt(i)`          | O(1)    | Direct array access                 |
| `length()`           | O(1)    | Cached field                        |
| `substring(i, j)`    | O(j-i)  | Java 7+ copies chars (was O(1))     |
| `indexOf(sub)`       | O(n*m)  | Naive search; n=text, m=pattern     |
| `equals(other)`      | O(n)    | Compares all characters             |
| `hashCode()`         | O(n)    | Computed once, then cached          |
| `concat(str)`        | O(n+m)  | Creates new String (avoid in loops) |
| `replace/replaceAll` | O(n*m)  | Regex overhead for replaceAll       |
| `split(regex)`       | O(n*m)  | Regex compilation + matching        |

### When to Use

- **Default choice** for all text that doesn't change
- **HashMap keys** — cached hashCode makes lookups fast
- **Constant literals** — compiler interns automatically
- **Method parameters** — immutable, safe to pass around
- **Return values** — no caller mutation concerns

**NEVER use** `String` concatenation (`+`) in loops — creates O(n²) garbage.

### Magic Methods (Java 21)

```java
// Comparison
int cmp = str1.compareTo(str2);           // negative, 0, positive
boolean eq = str1.equals(str2);           // content equality (ALWAYS use this)
boolean eqIgnoreCase = str1.equalsIgnoreCase(str2);

// Null-safe (Java 11+)
boolean same = Objects.equals(str1, str2); // handles nulls gracefully

// Predicates (Java 11+)
boolean blank = str.isBlank();            // true if empty or whitespace-only
boolean empty = str.isEmpty();            // true if length() == 0

// Search
int pos = str.indexOf("sub");             // first occurrence, -1 if absent
int last = str.lastIndexOf("sub");        // last occurrence
boolean contains = str.contains("sub");   // equivalent to indexOf >= 0
boolean starts = str.startsWith("pre");
boolean ends = str.endsWith("suf");

// Extraction
String sub = str.substring(3);            // from index to end
String sub2 = str.substring(3, 7);        // from 3 to 7 (exclusive)
char[] chars = str.toCharArray();         // copy to char array
char c = str.charAt(0);                   // single char access

// Replacement
String replaced = str.replace("old", "new");      // literal replacement
String regexReplaced = str.replaceAll("\\d+", "#"); // regex replacement
String firstReplaced = str.replaceFirst("\\d+", "#"); // first match only

// Splitting
String[] parts = str.split(",");                  // split by delimiter
String[] limited = str.split(",", 2);             // max 2 parts

// Trimming (Java 11+ strip is Unicode-aware)
String trimmed = str.trim();             // ASCII whitespace only
String stripped = str.strip();           // Unicode whitespace (prefer this)
String strippedLeading = str.stripLeading();
String strippedTrailing = str.stripTrailing();

// Conversion
String lower = str.toLowerCase();        // locale-sensitive!
String upper = str.toUpperCase();
String stripped_lower = str.toLowerCase(Locale.ROOT); // locale-independent

// Formatting (Java 15+)
String formatted = """
        Hello, %s
        """.formatted(name);

// Indentation adjustment (Java 15+)
String indented = str.indent(4);         // add 4 spaces to each line
String dedented = str.stripIndent();     // remove common leading whitespace

// Repetition (Java 11+)
String repeated = "ab".repeat(3);        // "ababab"
```

### Practical Pattern: Check-Then-Act Avoidance

```java
// BEFORE: verbose
if(map.containsKey(key)){
        return map.

get(key);
}else{
        return defaultValue;
}

// AFTER: idiomatic
        return map.

getOrDefault(key, defaultValue);
```

### Practical Pattern from FizzBuzz.java

```java
// Conditional string building with StringBuilder
final StringBuffer sb = new StringBuffer();
if(pos %3==0){
        sb.

append("Fizz");
}
        if(pos %5==0){
        sb.

append("Buzz");
}
        list.

add((sb.length() ==0)?String.

valueOf(pos) :sb.

toString());
```

> Note: FizzBuzz uses `StringBuffer` but `StringBuilder` is preferred in modern single-threaded code.

---

## 2. StringBuilder (Mutable, Single-Threaded)

```java
StringBuilder sb = new StringBuilder();
```

### Characteristics

| Property             | Value                                      |
|----------------------|--------------------------------------------|
| **Mutability**       | Mutable (expands as needed)                |
| **Thread-safe**      | No                                         |
| **Performance**      | O(1) amortized append, fastest builder     |
| **Memory**           | Efficient (no intermediate String objects) |
| **Initial capacity** | 16 chars (default), auto-doubles           |

### Complexity

| Operation            | Average      | Worst Case | Notes                              |
|----------------------|--------------|------------|------------------------------------|
| `append(x)`          | O(1)*        | O(n)       | *amortized; n=chars appended       |
| `insert(i, x)`       | O(n)         | O(n)       | Shifts all chars after i           |
| `deleteCharAt(i)`    | O(n)         | O(n)       | Shifts all chars after i           |
| `delete(start, end)` | O(n)         | O(n)       | Shifts remaining chars             |
| `replace(i, j, str)` | O(n)         | O(n)       | Combination of delete + insert     |
| `reverse()`          | O(n)         | O(n)       | In-place character swap            |
| `charAt(i)`          | O(1)         | O(1)       | Direct access                      |
| `setLength(n)`       | O(1) or O(n) | O(n)       | O(1) if shrinking, O(n) if growing |
| `toString()`         | O(n)         | O(n)       | Copies internal buffer             |

> **Amortized O(1) append**: StringBuilder doubles capacity when full, spreading resize cost over many appends.

### When to Use

- **String concatenation in loops** — THE most important use case
- **Building output incrementally** — challenge solutions, formatted text
- **Chained operations** — fluent API style
- **Any multi-step string construction** in single-threaded code

**NEVER use** `String` concatenation (`+`) in loops. The compiler optimizes simple cases but NOT complex loops.

### Magic Methods

```java
// Chained append (returns this for fluent style)
sb.append("hello").

append(" ").

append("world");

// Append any type (int, boolean, Object, char[])
sb.

append(42);
sb.

append(true);
sb.

append(new char[] {
    'a', 'b', 'c'
});

// Insert at position
        sb.

insert(5,"inserted");

// Delete single character
sb.

deleteCharAt(3);

// Delete range
sb.

delete(2,8);  // from index 2 (inclusive) to 8 (exclusive)

// Replace range
sb.

replace(0,5,"new text");

// Reverse in place
sb.

reverse();

// Set length (truncate or expand)
sb.

setLength(10);  // truncates if shorter, pads with null chars if longer

// Ensure capacity (avoid reallocations if size known)
sb.

ensureCapacity(1000);

// Get current capacity (may be larger than length)
int cap = sb.capacity();

// Clear efficiently (better than creating new instance)
sb.

setLength(0);  // reuses internal buffer
```

**Practical loop pattern (FizzBuzz improved):**

```java
// BEFORE: String concatenation (O(n²) garbage)
String result = "";
for(
int i = 0;
i<n;i++){
result +=i +", ";  // BAD: creates new String each iteration
        }

// AFTER: StringBuilder (O(n) total)
StringBuilder sb = new StringBuilder();
for(
int i = 0;
i<n;i++){
        sb.

append(i).

append(", ");
}
String result = sb.toString();

// REUSE pattern: clear and reuse for multiple iterations
StringBuilder sb = new StringBuilder();
for(
int test = 0;
test<testCases;test++){
        sb.

setLength(0);  // clear without allocation

// ... build string
process(sb.toString());
        }
```

### StringBuilder vs String Concatenation Performance

```java
// String concatenation in loop: O(n²) time, O(n²) memory
String result = "";
for(
int i = 0;
i< 10000;i++){
result +=i;  // Creates 10000 intermediate String objects
}

// StringBuilder: O(n) time, O(n) memory
StringBuilder sb = new StringBuilder();
for(
int i = 0;
i< 10000;i++){
        sb.

append(i);  // Reuses same buffer
}
String result = sb.toString();
```

---

## 3. StringBuffer (Mutable, Thread-Safe)

```java
StringBuffer sb = new StringBuffer();
```

### Characteristics

| Property        | Value                              |
|-----------------|------------------------------------|
| **Mutability**  | Mutable                            |
| **Thread-safe** | Yes (all methods synchronized)     |
| **Performance** | Slow (synchronization overhead)    |
| **Memory**      | Heavy (lock overhead per instance) |

### Complexity

Same as StringBuilder but with synchronization overhead on every operation.

### When to Use

- **Almost never in coding challenges** — challenges are single-threaded
- **Legacy code compatibility** only
- **Multi-threaded string building** (extremely rare; prefer other patterns)

**DO NOT use** in new code. Even in concurrent scenarios, prefer:

- Thread-local `StringBuilder`
- `ConcurrentHashMap` for concurrent string aggregation
- `Collectors.joining()` with streams

### Warning: Rarely Needed

```java
// WRONG: Over-engineering for single-threaded challenge
StringBuffer sb = new StringBuffer();  // unnecessary sync overhead

// RIGHT: Use StringBuilder
StringBuilder sb = new StringBuilder();
```

---

## 4. Text Blocks (Java 15+)

```java
String json = """
        {
            "name": "John",
            "age": 30
        }
        """;
```

### Characteristics

| Property        | Value                             |
|-----------------|-----------------------------------|
| **Mutability**  | Immutable (returns String)        |
| **Readability** | High (no escape sequences needed) |
| **Indentation** | Automatic (trailing whitespace)   |
| **Thread-safe** | Yes                               |

### When to Use

- **Multi-line literals** — SQL queries, JSON, XML, templates
- **Avoiding escape hell** — no need for `\\`, `\"`, `\n`
- **Formatted output** — readable test data, error messages

### Magic Methods

```java
// Basic text block
String sql = """
                SELECT * FROM users
                WHERE age > 18
                ORDER BY name
                """;

// With expressions (Java 17+)
String greeting = """
        Hello, %s!
        """.formatted(name);

// Incidental whitespace stripped (alignment preserved)
String aligned = """
        Line 1
        Line 2
        Line 3
        """;

// Explicit newlines preserved
String withBlanks = """
        First line\
        Second line (no newline before)
        """;

// Strip indent manually if needed
String stripped = textBlock.stripIndent();

// Indent further
String indented = textBlock.indent(4);
```

**Practical pattern: SQL queries**

```java
// BEFORE: Escape hell
String sql = "SELECT * FROM users\n" +
                "WHERE age > 18\n" +
                "ORDER BY name";

// AFTER: Readable text block
String sql = """
        SELECT * FROM users
        WHERE age > 18
        ORDER BY name
        """;
```

---

## 5. Pattern & Matcher (Regex)

```java
Pattern pattern = Pattern.compile("\\d+");
Matcher matcher = pattern.matcher("abc123def");
```

### Characteristics

| Property        | Value                               |
|-----------------|-------------------------------------|
| **Mutability**  | Pattern immutable, Matcher reusable |
| **Performance** | Compiled once, used many times      |
| **Thread-safe** | Pattern: Yes, Matcher: No           |
| **Use case**    | Validation, extraction, replacement |

### Complexity

| Operation         | Average | Notes                    |
|-------------------|---------|--------------------------|
| `Pattern.compile` | O(m)    | m = regex pattern length |
| `matcher.find()`  | O(n*m)  | n = text, m = pattern    |
| `matcher.matches` | O(n*m)  | Full string match        |
| `replaceAll`      | O(n*m)  | Replace all matches      |
| `split`           | O(n*m)  | Split by regex delimiter |

> **Performance tip**: Compile `Pattern` once, reuse `Matcher` via `reset()`.

### When to Use

- **Validation** — email, phone, ID formats
- **Extraction** — capture groups from structured text
- **Complex replacement** — regex-based transformations
- **Tokenization** — split by complex delimiters

### Magic Methods

```java
// Compile once (cache if reused)
Pattern pattern = Pattern.compile("\\d{3}-\\d{4}");

// Create matcher for specific text
Matcher matcher = pattern.matcher("Call 555-1234 now");

// Find matches
boolean found = matcher.find();           // next match
boolean fullMatch = matcher.matches();    // entire string matches

// Extract groups
String group1 = matcher.group(1);         // first capture group
String group0 = matcher.group();          // entire match

// Iterate all matches
while(matcher.

find()){
        System.out.

println(matcher.group());
        }

// Replace matches
String replaced = matcher.replaceAll("#");     // replace all
String firstReplaced = matcher.replaceFirst("#"); // first only

// Split by regex (Pattern method)
String[] parts = pattern.split("a1b2c3");      // ["a", "b", "c"]

// Common patterns
Pattern digits = Pattern.compile("\\d+");           // one or more digits
Pattern word = Pattern.compile("\\w+");             // word characters
Pattern letters = Pattern.compile("[a-zA-Z]+");     // letters only
Pattern email = Pattern.compile("[\\w.]+@[\\w.]+"); // simple email
Pattern whitespace = Pattern.compile("\\s+");       // whitespace
```

### Practical Pattern: String Parsing (TimeConversion.java)

```java
// TimeConversion.java uses java.time API (preferred for dates)
// But for custom parsing, regex works:

// BEFORE: Manual character extraction
String hour = s.substring(0, 2);
String minute = s.substring(3, 5);

// AFTER: Regex with groups
Pattern timePattern = Pattern.compile("(\\d{2}):(\\d{2}):(\\d{2})(AM|PM)");
Matcher m = timePattern.matcher("07:05:45PM");
if(m.

matches()){
String hour = m.group(1);    // "07"
String minute = m.group(2);  // "05"
String second = m.group(3);  // "45"
String ampm = m.group(4);    // "PM"
}
```

### Common Gotcha: Matcher Not Reusable

```java
// WRONG: Reusing matcher on different text
Matcher m = pattern.matcher("text1");
m.

find();
m.

reset("text2");  // This works but is confusing

// RIGHT: Create new matcher per text
Matcher m1 = pattern.matcher("text1");
Matcher m2 = pattern.matcher("text2");
```

---

## Key String Methods Reference

| Method                          | Complexity | Challenge Use-Case                       |
|---------------------------------|------------|------------------------------------------|
| `charAt(int)`                   | O(1)       | Single char access, two-pointer problems |
| `indexOf(String)`               | O(n*m)     | Find substring position                  |
| `lastIndexOf(String)`           | O(n*m)     | Find last occurrence                     |
| `substring(int)`                | O(n)       | Extract from position to end             |
| `substring(int, int)`           | O(n)       | Extract range                            |
| `contains(CharSequence)`        | O(n*m)     | Check if substring exists                |
| `startsWith(String)`            | O(m)       | Prefix check                             |
| `endsWith(String)`              | O(m)       | Suffix check                             |
| `replace(CharSequence, ...)`    | O(n*m)     | Literal replacement                      |
| `replaceAll(String, String)`    | O(n*m)     | Regex replacement                        |
| `split(String)`                 | O(n*m)     | Tokenize by delimiter                    |
| `toCharArray()`                 | O(n)       | Convert to mutable char array            |
| `matches(String)`               | O(n*m)     | Validate against regex                   |
| `compareTo(String)`             | O(n)       | Lexicographic comparison, sorting        |
| `trim()` / `strip()`            | O(n)       | Remove whitespace                        |
| `length()`                      | O(1)       | Get string length                        |
| `isEmpty()`                     | O(1)       | Check if empty                           |
| `isBlank()`                     | O(n)       | Check if empty or whitespace (Java 11+)  |
| `toLowerCase()` / `toUpperCase` | O(n)       | Case normalization                       |

### Method Selection Guide

```
Need to check if substring exists?
  ├─ Just existence? → contains(sub)
  ├─ Need position?  → indexOf(sub)
  └─ Need all positions? → loop with indexOf(sub, fromIndex)

Need to extract part of string?
  ├─ From index to end?    → substring(start)
  └─ From start to end?    → substring(start, end)

Need to modify string?
  ├─ One-time change?     → replace(old, new) or replaceAll(regex, new)
  ├─ Multiple changes?    → StringBuilder
  └─ In loop?             → MUST use StringBuilder

Need to split into parts?
  ├─ Simple delimiter?    → split(",")
  ├─ Limit parts?         → split(",", limit)
  └─ Complex pattern?     → Pattern.split()

Need to validate format?
  ├─ Simple check?        → startsWith/endsWith/contains
  └─ Complex pattern?     → matches(regex) or Pattern.matcher()
```

---

## Java 21 Features

### String.indent(int)

```java
String indented = "line1\nline2".indent(4);
// Result: "    line1\n    line2\n"
```

Adds spaces to the beginning of each line. Positive = indent, negative = dedent.

### String.repeat(int)

```java
String stars = "*".repeat(5);        // "*****"
String pad = " ".repeat(10);         // 10 spaces for padding
```

Efficient string repetition (Java 11+).

### String.isBlank()

```java
"".isBlank();         // true
"   ".

isBlank();      // true (whitespace counts as blank)
" a ".

isBlank();      // false

"".

isEmpty();         // true
"   ".

isEmpty();      // false (has characters)
```

Prefer `isBlank()` over `isEmpty()` for user input validation.

### String.stripIndent()

```java
String dedented = """
            Line 1
            Line 2
        """.stripIndent();
```

Removes common leading whitespace from all lines (Java 15+).

---

## Common Gotchas

### 1. String Concatenation in Loops (O(n²) Trap)

```java
// WRONG: O(n²) time and memory
String result = "";
for(
int i = 0;
i<n;i++){
result +=i;  // Creates new String each iteration
}

// RIGHT: O(n) time and memory
StringBuilder sb = new StringBuilder();
for(
int i = 0;
i<n;i++){
        sb.

append(i);
}
String result = sb.toString();
```

### 2. `==` vs `.equals()`

```java
String a = new String("hello");
String b = new String("hello");

a ==b        // false (different objects)
a.

equals(b)   // true (same content) — ALWAYS USE THIS

// Exception: Interned strings
String c = "hello";
String d = "hello";
c ==d        // true (same pool reference) — but don't rely on this
```

### 3. `trim()` vs `strip()`

```java
String s = "  hello  ";

s.

trim()    // ASCII whitespace only (Java 1.0)
s.

strip()   // Unicode-aware (Java 11+, PREFER THIS)

// Unicode whitespace example
String unicodeSpace = "\u2000hello\u2000";
unicodeSpace.

trim()   // "hello" (may not remove all Unicode spaces)
unicodeSpace.

strip()  // "hello" (removes all Unicode whitespace)
```

### 4. `charAt()` Returns `char`, Not Code Point

```java
String emoji = "😀";  // Surrogate pair (2 chars)

emoji.

charAt(0)        // Returns first surrogate (not valid emoji)
emoji.

codePointAt(0)   // Returns actual Unicode code point
emoji.

length()         // Returns 2 (not 1!)

// For full Unicode support
emoji.

codePoints().

forEach(cp ->System.out.

println(Character.toString(cp)));
```

**Challenge relevance**: Most problems use ASCII, so `charAt()` is fine. Be aware of this for emoji/Unicode problems.

### 5. `substring()` Complexity Changed

```java
// Java 6 and earlier: O(1) — shared char array
// Java 7 and later: O(n) — copies chars

String large = "...";  // 1 million chars
String small = large.substring(0, 10);  // Copies 10 chars (not shares)
```

### 6. `split()` Trailing Empty Strings

```java
"a,b,c,".split(",")      // ["a", "b", "c"] — trailing empty discarded
"a,b,c,".

split(",",-1)  // ["a", "b", "c", ""] — keep trailing

// To keep all parts, use negative limit
String[] all = str.split(",", -1);
```

---

## See Also

- [CP_IO_GUIDE.md](./CP_IO_GUIDE.md) — Console input/output, BufferedReader, Scanner, Fast I/O
- [MAP_GUIDE.md](./MAP_GUIDE.md) — HashMap, TreeMap, ConcurrentHashMap for key-value patterns
- [QUEUE_GUIDE.md](./QUEUE_GUIDE.md) — Queue, PriorityQueue, Deque for ordering problems

---

## Quick Reference Card

```java
// Immutable text
String s = "hello";
s.

equals(other);
s.

substring(0,3);
s.

indexOf("ell");
s.

replace("l","x");
s.

split(",");

// Mutable building (single-threaded)
StringBuilder sb = new StringBuilder();
sb.

append("a").

append("b");
sb.

deleteCharAt(0);
sb.

reverse();

String result = sb.toString();

// Text blocks (Java 15+)
String json = """
        {"key": "value"}
        """;

// Regex
Pattern p = Pattern.compile("\\d+");
Matcher m = p.matcher("abc123");
m.

find();
m.

group();

// Java 11+ utilities
s.

isBlank();
s.

strip();
s.

repeat(3);
String.

repeat(3);
```

(End of file - total 712 lines)
