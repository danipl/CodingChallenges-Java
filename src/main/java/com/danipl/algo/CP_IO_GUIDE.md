# Competitive Programming I/O Guide

## Quick-Reference: I/O Method Selection Matrix

| Method                               | Speed   | Ease of Use | Memory | When to Use                                  |
|--------------------------------------|---------|-------------|--------|----------------------------------------------|
| **Scanner**                          | Slow    | ✅ Highest   | High   | Small inputs, learning, non-time-critical    |
| **BufferedReader**                   | Fast    | ✅ High      | Medium | Medium inputs, simple reading                |
| **BufferedReader + StringTokenizer** | Fast    | ✅ High      | Low    | Recommended template, token-based parsing    |
| **Custom FastReader**                | Fastest | ⚠️ Medium   | Low    | Large inputs, competitive programming staple |
| **PrintWriter**                      | Fastest | ✅ High      | Low    | Output, especially buffered writes           |
| **System.out**                       | Slow    | ✅ Highest   | High   | Small outputs, debugging                     |

### At-A-Glance Decision Flow

```
Need fast I/O?
  ├─ NO (small input, ≤10KB)   → Scanner
  ├─ YES → What input size?
  │        ├─ Medium (≤1MB)      → BufferedReader + StringTokenizer
  │        └─ Large (>1MB)       → Custom FastReader
  │
  └─ Output → Use PrintWriter with StringBuilderAppend
```

---

## Overview

Competitive programming involve processing large inputs quickly. Standard input methods (Scanner, System.out)
have overhead. Optimized I/O techniques reduce this overhead, often the difference between AC and TLE.

---

## 1. Scanner (Basic, Slow)

```java
Scanner sc = new Scanner(System.in);

// Reading different types
int n = sc.nextInt();
long a = sc.nextLong();
double x = sc.nextDouble();
String s = sc.next();
String line = sc.nextLine();

// Reading with delimiter
sc.

useDelimiter(",");

        int a = sc.nextInt(), b = sc.nextInt();
```

### Characteristics

| Property   | Value                          |
|------------|--------------------------------|
| **Speed**  | Slow (regex-based parsing)     |
| **Memory** | High (buffer + regex overhead) |
| **Use**    | Small inputs, learning         |

### When NOT to Use

- Any problem with input size > 1MB
- Problems with 100K+ numbers
- Time-critical problems (TLE risk)

### When to Use

- Learning Java I/O
- Debugging/prototyping
- Small inputs where speed doesn't matter

---

## 2. BufferedReader (Fast, Simple)

```java
BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

// Read line and parse
String line = br.readLine();
int n = Integer.parseInt(line);

// Split space-separated values
String[] parts = br.readLine().split(" ");
int a = Integer.parseInt(parts[0]);
int b = Integer.parseInt(parts[1]);

// Read into array
int[] arr = new int[n];
for(
int i = 0;
i<n;i++){
arr[i]=Integer.

parseInt(br.readLine());
        }
```

### Characteristics

| Property   | Value                   |
|------------|-------------------------|
| **Speed**  | Fast (byte-based)       |
| **Memory** | Medium (buffer only)    |
| **Ease**   | Requires manual parsing |

### When to Use

- Medium-sized inputs
- khi you need full line reading
- Simple space-separated problems

---

## 3. BufferedReader + StringTokenizer (Recommended Template)

```java
import java.io.*;
import java.util.*;

class Main {
    static BufferedReader br;
    static StringTokenizer st;

    static String next() throws IOException {
        while (st == null || !st.hasMoreTokens()) {
            st = new StringTokenizer(br.readLine());
        }
        return st.nextToken();
    }

    static int nextInt() throws IOException {
        return Integer.parseInt(next());
    }

    static long nextLong() throws IOException {
        return Long.parseLong(next());
    }

    static double nextDouble() throws IOException {
        return Double.parseDouble(next());
    }

    static String nextLine() throws IOException {
        return br.readLine();
    }

    public static void main(String[] args) throws IOException {
        br = new BufferedReader(new InputStreamReader(System.in));
        // Now use: nextInt(), next(), etc.
    }
}
```

### Characteristics

| Property   | Value                    |
|------------|--------------------------|
| **Speed**  | Fast                     |
| **Memory** | Low (no regex per token) |
| **Ease**   | Very high after setup    |

### Why Recommended

- Faster than Scanner (no regex overhead per token)
- Easier than manual split().split(" ")
- Handles multiple tokens per line cleanly
- Standard template acrossCP communities

---

## 4. Custom FastReader (Fastest, Production-Ready)

```java
import java.io.*;
import java.util.*;

class FastReader {
    private BufferedReader br;
    private StringTokenizer st;

    public FastReader() {
        br = new BufferedReader(new InputStreamReader(System.in));
    }

    public FastReader(String file) throws FileNotFoundException {
        br = new BufferedReader(new FileReader(file));
    }

    String next() {
        while (st == null || !st.hasMoreTokens()) {
            try {
                st = new StringTokenizer(br.readLine());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return st.nextToken();
    }

    int nextInt() {
        return Integer.parseInt(next());
    }

    long nextLong() {
        return Long.parseLong(next());
    }

    double nextDouble() {
        return Double.parseDouble(next());
    }

    String nextLine() {
        String str = "";
        try {
            str = br.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return str;
    }

    boolean hasNext() {
        try {
            if (st != null && st.hasMoreTokens()) {
                return true;
            }
            String str = br.readLine();
            if (str == null) {
                return false;
            }
            st = new StringTokenizer(str);
            return st.hasMoreTokens();
        } catch (Exception e) {
            return false;
        }
    }

    int[] nextIntArray(int n) {
        int[] arr = new int[n];
        for (int i = 0; i < n; i++) {
            arr[i] = nextInt();
        }
        return arr;
    }

    long[] nextLongArray(int n) {
        long[] arr = new long[n];
        for (int i = 0; i < n; i++) {
            arr[i] = nextLong();
        }
        return arr;
    }
}
```

### Characteristics

| Property         | Value                          |
|------------------|--------------------------------|
| **Speed**        | Fastest (optimized for CP)     |
| **Memory**       | Low                            |
| **Completeness** | Full class with helper methods |

### When to Use

- Default for any competitive programming problem
- Input size > 1MB or 100K+ elements
- Problems where TLE is a concern

---

## 5. Output Methods

### 5a. System.out (Slow, Simple)

```java
System.out.println("Hello");
System.out.

print(x);
System.out.

printf("Format: %d %s\n",n, s);
```

### 5b. StringBuilder + System.out (Fast)

```java
StringBuilder sb = new StringBuilder();

sb.

append("Hello\n");
sb.

append(n).

append(" ").

append(m).

append("\n");

System.out.

print(sb.toString());
```

### 5c. PrintWriter (Fastest, Recommended)

```java
import java.io.*;

PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.out)));

// Writing
pw.

println("Hello");
pw.

print(x);
pw.

printf("Format: %d %s%n",n, s);

// Flush before exit
pw.

flush();
pw.

close();
```

### Output Comparison

| Method              | Speed   | When to Use                         |
|---------------------|---------|-------------------------------------|
| System.out          | Slow    | Small outputs, debugging            |
| StringBuilder + out | Fast    | Medium outputs, moderate complexity |
| PrintWriter         | Fastest | Large outputs, multiple lines       |

### Best Practice Pattern

```java
public static void main(String[] args) throws IOException {
    FastReader fr = new FastReader();
    PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.out)));

    int t = fr.nextInt();
    while (t-- > 0) {
        int n = fr.nextInt();
        int m = fr.nextInt();
        pw.println(n + m);
    }

    pw.flush();
    pw.close();
}
```

---

## Complete FastReader + PrintWriter Template

```java
import java.io.*;
import java.util.*;

public class Main {
    static class FastReader {
        BufferedReader br;
        StringTokenizer st;

        public FastReader() {
            br = new BufferedReader(new InputStreamReader(System.in));
        }

        String next() {
            while (st == null || !st.hasMoreTokens()) {
                try {
                    st = new StringTokenizer(br.readLine());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return st.nextToken();
        }

        int nextInt() {
            return Integer.parseInt(next());
        }

        long nextLong() {
            return Long.parseLong(next());
        }

        double nextDouble() {
            return Double.parseDouble(next());
        }

        String nextLine() {
            String str = "";
            try {
                str = br.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return str;
        }
    }

    static class FastWriter {
        PrintWriter pw;

        public FastWriter() {
            pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.out)));
        }

        void print(Object o) {
            pw.print(o);
        }

        void println(Object o) {
            pw.println(o);
        }

        void printf(String format, Object... args) {
            pw.printf(format, args);
        }

        void close() {
            pw.flush();
            pw.close();
        }
    }

    public static void main(String[] args) {
        FastReader fr = new FastReader();
        FastWriter fw = new FastWriter();

        // Your code here

        fw.close();
    }
}
```

---

## Common Input Format Examples

### 1. Space-Separated Values on Single Line

```java
// Input: 5 10 15 20 25
int n = fr.nextInt();
int[] arr = fr.nextIntArray(n);
```

### 2. Multi-Line Matrix

```java
// Input:
// 3 4
// 1 2 3 4
// 5 6 7 8
// 9 10 11 12

int rows = fr.nextInt();
int cols = fr.nextInt();
int[][] matrix = new int[rows][cols];

for(
int i = 0;
i<rows;i++){
        for(
int j = 0;
j<cols;j++){
matrix[i][j]=fr.

nextInt();
    }
            }
```

### 3. Graph Input (Edges)

```java
// Input:
// 5 6  (vertices, edges)
// 1 2  (edge)
// 2 3
// 3 4
// 4 5
// 1 3
// 2 4

int n = fr.nextInt(); // vertices
int m = fr.nextInt(); // edges

List<List<Integer>> adj = new ArrayList<>();
for(
int i = 0;
i <=n;i++)adj.

add(new ArrayList<>());

        for(
int i = 0;
i<m;i++){
int u = fr.nextInt();
int v = fr.nextInt();
    adj.

get(u).

add(v);
    adj.

get(v).

add(u); // undirected
}
```

### 4. Weighted Graph

```java
// Input:
// 5 6
// 1 2 10  (u v weight)
// 2 3 5
// 3 4 8

int n = fr.nextInt();
int m = fr.nextInt();

List<List<int[]>> adj = new ArrayList<>();
for(
int i = 0;
i <=n;i++)adj.

add(new ArrayList<>());

        for(
int i = 0;
i<m;i++){
int u = fr.nextInt();
int v = fr.nextInt();
int w = fr.nextInt();
    adj.

get(u).

add(new int[] {
    v, w
});
        }
```

### 5. Strings and Mixed Types

```java
// Input:
// 3
// 10 apple
// 20 banana
// 15 cherry

int n = fr.nextInt();
int[] nums = new int[n];
String[] fruits = new String[n];

for(
int i = 0;
i<n;i++){
nums[i]=fr.

nextInt();

fruits[i]=fr.

next();
}
```

---

## Common Gotchas

### 1. next() vs nextLine() Mixing

```java
// BAD: nextLine() returns empty after nextInt()
nextInt();

nextLine(); // Returns empty string!

// GOOD: Consume remaining line
nextInt();
scan.

nextLine(); // Consume newline

String s = scan.nextLine();

// OR: Use next() for single word
nextInt();

String s = next(); // Works
```

### 2. Integer Overflow

```java
// BAD: Reading large numbers as int
int x = nextInt(); // May overflow for 10^18

// GOOD: Use long for large numbers
long x = nextLong();
```

### 3. Empty Input / EOF

```java
// ALWAYS check for EOF
while(fr.hasNext()){
int n = fr.nextInt();
// process
}

// Or with tokenizer
        while(true){
        try{
        if(!fr.st.

hasMoreTokens())fr.st =new

StringTokenizer(fr.br.readLine());
        // process
        }catch(
Exception e){break;}
        }
```

### 4. Trailing Newlines

```java
// Input may have trailing blank lines
// Use next() instead of nextLine() to skip whitespace
next(); // Skips blank lines automatically
```

### 5. Locale-Specific Parsing

```java
// For European format (comma decimal separator)
Locale.setDefault(Locale.US); // Ensure dot decimal
```

---

## Java 21 Features

### Text Blocks for Input Templates

```java
// For debugging with sample inputs
String sampleInput = """
                3
                1 2 3
                4 5 6
                7 8 9
                """;

// Use StringReader for testing
StringReader reader = new StringReader(sampleInput);
BufferedReader testBr = new BufferedReader(reader);
```

### Switch Expression with I/O

```java
int choice = nextInt();
result =switch(choice){
        case 1->

processType1();
    case 2->

processType2();

default ->throw new

IllegalArgumentException("Invalid choice");
};

println(result);
```

### Pattern Matching for INSTANCEOF (Java 21)

```java
Object obj = readInput();
if(obj instanceof
String s){

// s is automatically cast to String
processString(s);
}
```

---

## Performance Comparison (Approximate)

| Method                           | Relative Speed | 1M Integers (seconds) |
|----------------------------------|----------------|-----------------------|
| Scanner                          | 1x             | ~10                   |
| BufferedReader + split()         | 3x             | ~3                    |
| BufferedReader + StringTokenizer | 5x             | ~2                    |
| Custom FastReader                | 8x             | ~1                    |
| System.out                       | 1x             | ~5                    |
| StringBuilder + out              | 5x             | ~1                    |
| PrintWriter                      | 8x             | ~0.5                  |

---

## See Also

### Reference Files

- `TimeConversion.java` - Basic parsing with Scanner, BufferedReader comparison
- String manipulation patterns in `src/main/java/com/danipl/development/onepreparationweek/`

### Related Guides

- **[STRING_GUIDE.md](STRING_GUIDE.md)** - String parsing, splitting, tokenization
- `TimeConversion.java` - I/O example with both Scanner and BufferedReader
- `StringTokenizer` vs `split()` performance considerations

---

## Quick Reference Summary

```java
//争吵经典模板 (Classic Template)

import java.io.*;
import java.util.*;

public class Main {
    static class FastReader {
        BufferedReader br;
        StringTokenizer st;

        public FastReader() {
            br = new BufferedReader(new InputStreamReader(System.in));
        }

        String next() {
            while (st == null || !st.hasMoreTokens()) st = new StringTokenizer(br.readLine());
            return st.nextToken();
        }

        int nextInt() {
            return Integer.parseInt(next());
        }

        long nextLong() {
            return Long.parseLong(next());
        }

        double nextDouble() {
            return Double.parseDouble(next());
        }
    }

    static class FastWriter {
        PrintWriter pw;

        public FastWriter() {
            pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.out)));
        }

        void println(Object o) {
            pw.println(o);
        }

        void close() {
            pw.flush();
            pw.close();
        }
    }

    public static void main(String[] args) {
        FastReader fr = new FastReader();
        FastWriter fw = new FastWriter();

        // Code here

        fw.close();
    }
}
```

**Memory**: ~2KB overhead. **Speed**: Near C++ cin/cout. **Standard** across CP platforms.
