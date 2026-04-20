# Java 21 Bit Manipulation Guide

## Quick-Reference: Operator Reference Table

| Operator                 | Symbol | Behavior                                        | Truth Table (A B → Result)         | Common Usage                      | Challenge Pattern                          |
|--------------------------|--------|-------------------------------------------------|------------------------------------|-----------------------------------|--------------------------------------------|
| **AND**                  | `&`    | Both bits must be 1                             | 0 0 → 0, 0 1 → 0, 1 0 → 0, 1 1 → 1 | Masking, parity checks            | Count set bits, extract specific bits      |
| **OR**                   | `\|`   | At least one bit is 1                           | 0 0 → 0, 0 1 → 1, 1 0 → 1, 1 1 → 1 | Setting bits, combining flags     | Merge bitmasks, set feature flags          |
| **XOR**                  | `^`    | Bits differ                                     | 0 0 → 0, 0 1 → 1, 1 0 → 1, 1 1 → 0 | Toggling, finding unique, hashing | Lonely integer, toggle case, toggling bits |
| **NOT**                  | `~`    | Inverts all bits                                | 0 → 1, 1 → 0                       | Creating masks, inversion         | Creating inverted masks, bit clearing      |
| **Left Shift**           | `<<`   | Shifts left, fills with 0s                      | N/A                                | Multiply by 2^n,位 mask creation   | Power of 2 operations, bitmask generation  |
| **Right Shift**          | `>>`   | Shifts right, sign-extends (preserves sign)     | N/A                                | Divide by 2^n, signed extraction  | Extract sign, signed value extraction      |
| **Unsigned Right Shift** | `>>>`  | Shifts right, fills with 0s (no sign extension) | N/A                                | Unsigned extraction, bit padding  | Working with unsigned values, masking      |

### At-A-Glance Decision Flow

```
Need to find unique element?
  ├─ YES → All duplicates except one? → Use XOR (a ^ a = 0, a ^ 0 = a)
  │          Check: LonelyInteger.java pattern
  └─ NO  → Need bit counting?
              ├─ YES → Count set bits? → Brian Kernighan's: n & (n-1) clears lowest set bit
              │          Use: Integer.bitCount(n)
              └─ NO  → Check property?
                         ├─ Power of 2? → (n & (n-1)) == 0 AND n > 0
                         ├─ Single bit set? → (n & (n-1)) == 0
                         └─ Get/set/clear/toggle specific bit?
                                    ├─ Get nth bit: (n >> k) & 1
                                    ├─ Set nth bit: n \| (1 << k)
                                    ├─ Clear nth bit: n & ~(1 << k)
                                    └─ Toggle nth bit: n ^ (1 << k)
```

---

## Overview

Bit manipulation offers O(1) constant-time operations for many common challenges. Mastery of bitwise operators is
essential for:

- Finding unique elements in arrays
- Efficient set representation using bitmasks
- Optimizing arithmetic operations
- Working with binary representations
- Memory-efficient state storage

---

## 1. XOR Tricks

```java
// Identity laws
int a = 42;
int identityZ = a ^ 0;   // a ^ 0 = a  → 42
int identityZero = a ^ a; // a ^ a = 0  → 0

// Commutative & Associative
// Order doesn't matter: a ^ b ^ c = c ^ a ^ b
```

### Pattern: XOR Swap (Without Temporary Variable)

```java
int x = 10;
int y = 20;

x =x ^y;
y =x ^y; // (x ^ y) ^ y = x
x =x ^y; // (x ^ y) ^ x = y

// Swapped: x=20, y=10
```

### Pattern: XOR Check Sign

```java
// Toggle sign bit (bit 31 for int)
int negative = value ^ (1 << 31);

// Check if two numbers have same sign
boolean sameSign = ((a ^ b) >= 0);
```

### Challenge Reference: LonelyInteger.java (`onepreparationweek/LonelyInteger.java`)

```java
// XOR all elements - duplicates cancel out
int result = 0;
for(
int num :arr){
result ^=num;
}
// result is the lonely integer
```

---

## 2. Bit Masking

Represents sets as integers where each bit represents membership.

### Set Operations

```java
// Represent set as bitmask
int set = 0;

// Add element (set bit)
set |=(1<<element);    // set = set \| (1 << element)

// Remove element (clear bit)
set &=~(1<<element);   // set = set & ~(1 << element)

// Check membership (test bit)
boolean hasElement = (set & (1 << element)) != 0;

// Toggle element
set ^=(1<<element);

// Subset check: A is subset of B if (A & B) == A
boolean isSubset = ((subsetMask & fullMask) == subsetMask);

// Intersection
int intersection = mask1 & mask2;

// Union
int union = mask1 \|mask2;

// Difference: elements in A but not in B
int difference = mask1 & ~mask2;
```

### Challenge Context

Used in backtracking problems, subset generation, and set cover problems. Bitmask DP uses `dp[mask]` to store state for
subsets.

---

## 3. Counting Bits

### Brian Kernighan's Algorithm

Clears the lowest set bit in each iteration:

```java
int count = 0;
int nCopy = n;
while(nCopy !=0){
nCopy =nCopy &(nCopy -1);  // Clear lowest set bit
count++;
        }
// count = number of set bits
```

**Why it works:** `n & (n-1)` always clears the rightmost 1 bit because:

- If n ends in `...1000`, n-1 ends in `...0111`
- AND operation clears the trailing 1s

### Java 21 Built-in Methods

```java
int bits = Integer.bitCount(n);           // Count set bits
int trailing = Integer.numberOfTrailingZeros(n);  // Count trailing zeros
int leading = Integer.numberOfLeadingZeros(n);    // Count leading zeros
int highest = Integer.highestOneBit(n);           // Highest set bit only
int lowest = Integer.lowestOneBit(n);             // Lowest set bit only
int reversed = Integer.reverse(n);                // Bit reversal
int rotated = Integer.rotateLeft(n, 4);           // Left rotate
```

---

## 4. Power of 2 Detection

### Key Properties

```java
// Check if n is power of 2 (n > 0 required!)
boolean isPowerOf2 = (n & (n - 1)) == 0 && n > 0;

// Power of 2 values: 1, 2, 4, 8, 16, 32, ...
// Binary: 1, 10, 100, 1000, 10000, 100000, ...
```

### Extract Powers

```java
// Get lowest set bit ( isolate lowest 1)
int lowestPower = n & -n;

// Get highest set bit
int highestPower = Integer.highestOneBit(n);

// Round up to next power of 2
int nextPower = 1;
while(nextPower<n){
nextPower <<=1;
        }
```

### Challenge Reference: NumberOfStepsToReduceANumberToZero.java

```java
// Bit counting approach
// Each '1' bit requires 2 steps (add 1 then divide)
// Each '0' bit requires 1 step (just divide)
int steps = 0;
for(
int powerOfTwo = 1;
powerOfTwo <=num;powerOfTwo <<=1){
        if((powerOfTwo &num)!=0){
steps +=2;  // '1' bit: increment then divide
        }else{
steps +=1;  // '0' bit: just divide
        }
        }
        return steps -1;  // Last bit over-counted
```

---

## 5. Single Bit Operations

### Get nth Bit

```java
int getBit(int n, int position) {
    return (n >> position) & 1;
}
```

### Set nth Bit

```java
int setBit(int n, int position) {
    return n | (1 << position);
}
```

### Clear nth Bit

```java
int clearBit(int n, int position) {
    return n & ~(1 << position);
}
```

### Toggle nth Bit

```java
int toggleBit(int n, int position) {
    return n ^ (1 << position);
}
```

### Check if Power of 2 (Revisited)

```java
boolean isPowerOf2(int n) {
    return n > 0 && (n & (n - 1)) == 0;
}
```

### Rightmost Set Bit

```java
// Isolate rightmost set bit
int rightmost = n & -n;

// Remove rightmost set bit
int withoutRightmost = n & (n - 1);
```

---

## 6. Bit Manipulation with Chars

### Toggle Case

```java
// ASCII: 'a' = 97 (01100001), 'A' = 65 (01000001)
// Difference: bit 5 (value 32)
char lower = (char) ('A' ^ 32);  // 'a'
char upper = (char) ('a' ^ 32);  // 'A'

// Toggle case for string
String toggleCase(String s) {
    StringBuilder result = new StringBuilder();
    for (char c : s.toCharArray()) {
        if (Character.isLowerCase(c)) {
            result.append((char) (c ^ 32));
        } else {
            result.append((char) (c ^ 32));
        }
    }
    return result.toString();
}
```

### Check Lowercase/ Uppercase

```java
// Check if lowercase (bit 5 set)
boolean isLower = (c & 32) != 0;

// Check if uppercase (bit 5 clear)
boolean isUpper = (c & 32) == 0;
```

### Encodings

```java
// ASCII values
// '0'-'9': 48-57
// 'A'-'Z': 65-90
// 'a'-'z': 97-122
```

---

## 7. Number Base Conversion

### Binary, Hex, Octal

```java
// To String
String binary = Integer.toBinaryString(n);      // "1010" for 10
String hex = Integer.toHexString(n);           // "a" for 10
String octal = Integer.toOctalString(n);       // "12" for 10

// Parse
int fromBinary = Integer.parseInt("1010", 2);  // 10
int fromHex = Integer.parseInt("a", 16);        // 10
int fromOctal = Integer.parseInt("12", 8);      // 10

// Java 21: Character encoding constants
String binaryPrefix = Integer.toBinaryString(n); // No prefix, just bits
```

### Challenge Reference: NumberOfStepsToReduceANumberToZero.java (Bit Pattern)

```java
// Count steps using bit pattern
// Number of bits = log2(n) + 1
// Number of '1' bits = addition operations
// Total = (number of bits - 1) + (number of '1' bits)
// = bitLength - 1 + bitCount
```

---

## Complexity Table

| Operation                   | Time Complexity | Space Complexity | Notes                         |
|-----------------------------|-----------------|------------------|-------------------------------|
| Basic operators (&, \|, ^)  | O(1)            | O(1)             | Single CPU instruction        |
| Shift (<<, >>, >>>)         | O(1)            | O(1)             | Single CPU instruction        |
| NOT (~)                     | O(1)            | O(1)             | Single CPU instruction        |
| Count bits (loop)           | O(k)            | O(1)             | k = number of set bits        |
| Count bits (bitCount)       | O(1)            | O(1)             | Java 21 optimized intrinsic   |
| Brian Kernighan's algorithm | O(set bits)     | O(1)             | Each iteration clears one bit |
| Power of 2 check            | O(1)            | O(1)             | Single mask operation         |
| Rightmost set bit           | O(1)            | O(1)             | n & -n is O(1)                |
| Get/set/clear/toggle bit    | O(1)            | O(1)             | Single bit operation          |

> **Note:** 32-bit integers mean all bit operations are effectively O(1) since the word size is constant.

---

## Java 21 Features

### Integer Class Methods

```java
// Counting
int count = Integer.bitCount(n);                     //_COUNT SET BITS_
int tz = Integer.numberOfTrailingZeros(n);          // COUNT TRAILING ZEROS_
int lz = Integer.numberOfLeadingZeros(n);           // COUNT LEADING ZEROS_

// Isolation
int hi = Integer.highestOneBit(n);                  // HIGHEST SET BIT_
int lo = Integer.lowestOneBit(n);                   // LOWEST SET BIT_

// Manipulation
int rev = Integer.reverse(n);                       // REVERSE ALL BITS_
int rotL = Integer.rotateLeft(n, 4);                // ROTATE LEFT_
int rotR = Integer.rotateRight(n, 4);               // ROTATE RIGHT_

// Sign handling
int unsignedDiv = Integer.divideUnsigned(n, d);     // UNSIGNED DIVISION_
int unsignedRem = Integer.remainderUnsigned(n, d);  // UNSIGNED REMAINDER_
int.

toUnsignedString(n);                            // UNSIGNED STRING_
```

### Use Cases

```java
// Fastpopulation count
int setBits = Integer.bitCount(n);

// Fast log base 2 (for power of 2)
int log2 = 31 - Integer.numberOfLeadingZeros(n);

// Fast ceiling of log base 2
int ceilLog2 = 32 - Integer.numberOfLeadingZeros(n - 1);

// Extract sign
int sign = Integer.signum(n);  // Returns -1, 0, or 1

// Create mask from count
int mask = (1 << count) - 1;
```

---

## Common Gotchas

### 1. Operator Precedence

**Problem:** `&` has lower precedence than `==`

```java
// WRONG: == binds tighter than &
if(n &1==1){...}  // Interpreted as: n & (1 == 1) → n & true → ERROR

// RIGHT: Always parenthesize bit operations in expressions
        if((n &1)==1){...}
```

### 2. Right Shift vs Unsigned Right Shift

**Problem:** `>>` preserves sign (arithmetic shift), `>>>` fills with 0s (logical shift)

```java
int negative = -1;  // 0xFFFFFFFF (all bits set)
int signedShift = negative >> 1;    // 0xFFFFFFFF (still -1!)
int unsignedShift = negative >>> 1; // 0x7FFFFFFF (positive!)

//誓NER FIRMWARE TOT RpoTec
```

### 3. Overflow with 1<<31

**Problem:** `1 << 31` produces negative number (sign bit set)

```java
// WRONG: 1 << 31 = -2147483648 (0x80000000)
int maxPowerOf2 = 1 << 31;  // Negative!

// RIGHT: Use long or unsigned
long maxPowerOf2 = 1L << 31;        // 2147483648L (positive)
int maxPowerOf2Unsigned = 0x80000000; // Hex literal
```

### 4. == Higher Precedence Than ^

**Problem:** Similar to #1, but with XOR

```java
// WRONG: (n == 1) ^ 1 (boolean XOR int - compile error)
if(n ==1^1){...}

// RIGHT: Parenthesize
        if((n ==1)^(someCondition)){...}
```

### 5. Char vs int Arithmetic via Bits

**Problem:** Character literals are `int` in arithmetic

```java
// Character arithmetic uses ASCII values
char c = 'a';
int result1 = c ^ 32;   // 97 ^ 32 = 65 ('A') ✓
char result2 = c ^ 32;  // Compile error: incompatible types

// FIX: Cast explicitly
char result3 = (char) (c ^ 32);
```

---

## See Also

### Challenge Files Referenced

1. `LonelyInteger.java` - XOR unique element pattern (`onepreparationweek/LonelyInteger.java`)
2. `NumberOfStepsToReduceANumberToZero.java` - Bit counting and power of 2 operations
3. `DiagonalDifference.java` - Matrix indexing with bit-like patterns
4. `PowerOfTwo` problems - Standard bit manipulation pattern

### Other Guides

- [SORT_SEARCH_GUIDE.md](src/main/java/com/danipl/SORT_SEARCH_GUIDE.md) - Sorting and searching patterns

### Related Topics

- **Boolean Logic**: AND/OR/XOR are foundational for logic circuits
- **Hash Functions**: XOR commonly used for combining hash values
- **Checksums**: XOR parity used in RAID and error detection
- **Cryptography**: Bit shuffling and XOR-based ciphers

---

## Quick Reference Summary

| Problem Type             | Solution Pattern                     | Example               |
|--------------------------|--------------------------------------|-----------------------|
| Find unique element      | XOR all elements                     | `result ^= num`       |
| Count set bits           | `Integer.bitCount(n)` or Kernighan's | `n & (n-1)`           |
| Power of 2 check         | `(n & (n-1)) == 0 && n > 0`          | `isPowerOf2(n)`       |
| Get/set/clear/toggle bit | `(n >> k) & 1`, `\|`, `& ~`, `^`     | `setBit(n, k)`        |
| Rightmost set bit        | `n & -n`                             | `lowestOneBit`        |
| Toggle case              | `char ^ 32`                          | `c ^ 32`              |
| Check nth bit            | `(n >> k) & 1`                       | `getBit(n, k)`        |
| Clear lowest set bit     | `n & (n-1)`                          | Kernighan's algorithm |
| Isolate lowest set bit   | `n & -n`                             | `lowestOneBit(n)`     |

---

*Last updated: April 2026 | Java 21*
