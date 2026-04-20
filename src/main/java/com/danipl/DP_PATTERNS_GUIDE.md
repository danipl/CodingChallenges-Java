# Java 21 Dynamic Programming Patterns Guide

## Quick-Reference: DP Pattern Selection Matrix

| Pattern                  | Space         | When to Use                            | Example Problem                 | Template                                         |
|--------------------------|---------------|----------------------------------------|---------------------------------|--------------------------------------------------|
| **Top-Down Memoization** | O(n)          | Natural recursive formulation          | Fibonacci, CanSum, GridTraveler | `Map<Integer, Long> memo; computeIfAbsent()`     |
| **Bottom-Up Tabulation** | O(n) to O(1)  | Iterative, controlled order, space opt | Climbing Stairs, House Robber   | `int[] dp; dp[0]=base; for(i) dp[i]=...`         |
| **Space-Optimized**      | O(1)          | Only need last k values                | Fibonacci, House Robber         | `int prev, curr; for(i) { new = prev + curr; }`  |
| **2D Grid DP**           | O(m×n)        | Grid paths, matrix problems            | GridTraveler, Min Path Sum      | `int[][] dp; dp[i][j] = dp[i-1][j] + dp[i][j-1]` |
| **State Machine DP**     | O(n×k)        | Buy/sell stock, decision sequences     | Best Time to Buy/Sell Stock     | `int hold, sold; for(price) { ... }`             |
| **1D Partition DP**      | O(n²) or O(n) | Subset sum, word break, palindrome     | CanSum, Partition Equal Subset  | `boolean[] dp; dp[0]=true; for(num) for(target)` |
| **Interval DP**          | O(n³)         | Optimal partition of range             | Matrix Chain Multiplication     | `for(len) for(i) { j=i+len-1; for(k) }`          |

### At-A-Glance Decision Flow

```
Problem asks for: count, minimum, maximum, existence?
├─ YES → Optimal substructure? (solution from subproblems)
│          └─ YES → Overlapping subproblems? (same subproblem repeated)
│                     └─ YES → DYNAMIC PROGRAMMING
│                                ├─ Natural recursion?
│                                │   └─ Top-Down Memoization (HashMap or array)
│                                ├─ Simple iteration possible?
│                                │   └─ Bottom-Up Tabulation
│                                ├─ Only need last k values?
│                                │   └─ Space-Optimized DP
│                                ├─ Grid/matrix structure?
│                                │   └─ 2D Grid DP
│                                └─ Decisions over time/states?
│                                    └─ State Machine DP
└─ NO → Greedy might work (no backtracking needed)
```

---

## Overview

Dynamic Programming solves optimization problems by breaking them into overlapping subproblems, solving each once, and
storing the result. The key insight: **optimal substructure + overlapping subproblems = DP**.

**Two approaches:**

1. **Top-Down (Memoization)**: Start with target, recurse down, cache results. Natural from recursive formulation.
2. **Bottom-Up (Tabulation)**: Start with base cases, iterate up to target. Better for space optimization.

**Not DP if:**

- Subproblems don't overlap (simple recursion suffices)
- No optimal substructure (can't build solution from subproblems)
- Greedy choice property holds (greedy algorithm works)

**Java 21 patterns:**

- `Map.computeIfAbsent()` for memoization
- `record` for state objects
- Pattern matching with `instanceof`
- Enhanced switch expressions

---

## 1. Top-Down Memoization (Recursive with Cache)

```java
Map<Integer, Long> memo = new HashMap<>();

public long fib(int n) {
    if (n <= 0) return 0;
    if (n <= 2) return 1;

    return memo.computeIfAbsent(n, k -> fib(k - 1) + fib(k - 2));
}
```

### Characteristics

| Property       | Value                                |
|----------------|--------------------------------------|
| **Approach**   | Recursive, lazy evaluation           |
| **Cache**      | HashMap or array                     |
| **Order**      | Determined by recursion              |
| **Complexity** | O(n) time, O(n) space (memo + stack) |
| **Best For**   | Natural recursive problems           |

### When to Use

- **Natural recursive formulation** — problem easily expressed recursively
- **Sparse subproblems** — not all subproblems needed (HashMap better than array)
- **Quick prototyping** — faster to write than bottom-up
- **Complex state** — multi-parameter recursion (use composite key)

### Complexity

| Operation          | Time      | Space     | Notes                    |
|--------------------|-----------|-----------|--------------------------|
| Memoized recursion | O(states) | O(states) | Each state computed once |
| Unmemoized         | O(2^n)    | O(n)      | Exponential without memo |

### See Also: development/recursion/Fibonacci.java

```java
// From Fibonacci.java — memoization pattern
public static long memo(int num) {
    return memo(num, new HashMap<>());
}

public static long memo(int num, Map<Integer, Long> memo) {
    if (num <= 0) return 0;
    if (num <= 2) return 1;

    if (memo.containsKey(num)) {
        return memo.get(num);
    }

    final long value = memo(num - 1, memo) + memo(num - 2, memo);
    memo.put(num, value);

    return value;
}
```

### See Also: development/recursion/CanSum.java

```java
// From CanSum.java — decision DP with memoization
public static boolean memo(int target, int[] values, Map<Integer, Boolean> memo) {
    if (target == 0) return true;
    if (target < 0) return false;

    if (memo.containsKey(target)) return memo.get(target);

    for (final int candidate : values) {
        final boolean result = candidate != 0 && memo(target - candidate, values, memo);
        memo.put(target, result);
        if (result) return true;  // Early exit
    }

    return false;
}
```

### Magic Methods: computeIfAbsent Pattern

```java
// Idiomatic Java 21 memoization
Map<Integer, Long> memo = new HashMap<>();

public long fibonacci(int n) {
    if (n <= 0) return 0;
    if (n <= 2) return 1;

    return memo.computeIfAbsent(n, k -> fibonacci(k - 1) + fibonacci(k - 2));
}

// Multi-parameter memoization (composite key)
// Use record for type-safe key
record State(int idx, int remaining) {
}

Map<State, Boolean> memo = new HashMap<>();

public boolean canSum(int[] nums, int idx, int target) {
    if (target == 0) return true;
    if (target < 0 || idx >= nums.length) return false;

    State key = new State(idx, target);
    return memo.computeIfAbsent(key, k ->
            canSum(nums, idx + 1, target - nums[idx]) ||
                    canSum(nums, idx + 1, target)
    );
}
```

### See Also: development/recursion/GridTraveler.java

```java
// From GridTraveler.java — 2D memoization with string key
public static long memo(int x, int y) {
    return memo(x, y, new HashMap<>());
}

public static long memo(int x, int y, Map<String, Long> memo) {
    if (x == 1 && y == 1) return 1;
    if (x == 0 || y == 0) return 0;

    String key = x + "," + y;  // Composite key as string

    if (memo.containsKey(key)) {
        return memo.get(key);
    }

    long result = memo(x - 1, y, memo) + memo(x, y - 1, memo);
    memo.put(key, result);

    return result;
}

// Java 21: Use record for type-safe composite key
record GridKey(int x, int y) {
}

public static long memoTyped(int x, int y, Map<GridKey, Long> memo) {
    if (x == 1 && y == 1) return 1;
    if (x == 0 || y == 0) return 0;

    GridKey key = new GridKey(x, y);
    return memo.computeIfAbsent(key, k ->
            memoTyped(x - 1, y, memo) + memoTyped(x, y - 1, memo)
    );
}
```

---

## 2. Bottom-Up Tabulation (Iterative)

```java
int[] dp = new int[n + 1];
dp[0]=0;
dp[1]=1;
        for(
int i = 2;
i <=n;i++){
dp[i]=dp[i -1]+dp[i -2];
        }
        return dp[n];
```

### Characteristics

| Property       | Value                                  |
|----------------|----------------------------------------|
| **Approach**   | Iterative, eager evaluation            |
| **Cache**      | Array (or multidimensional array)      |
| **Order**      | Explicit, controlled by loops          |
| **Complexity** | O(n) time, O(n) space (reducible)      |
| **Best For**   | Space optimization, simple transitions |

### When to Use

- **Space optimization needed** — can reduce to O(1) when only last k values needed
- **Simple state transitions** — clear iterative pattern
- **Avoid recursion limits** — no stack overflow risk
- **All subproblems needed** — filling entire table

### Fibonacci — Bottom-Up

```java
public long fibonacciTabulation(int n) {
    if (n <= 0) return 0;
    if (n <= 2) return 1;

    long[] dp = new long[n + 1];
    dp[1] = 1;
    dp[2] = 1;

    for (int i = 3; i <= n; i++) {
        dp[i] = dp[i - 1] + dp[i - 2];
    }

    return dp[n];
}
```

### Space-Optimized Fibonacci

```java
public long fibonacciOptimized(int n) {
    if (n <= 0) return 0;
    if (n <= 2) return 1;

    long prev2 = 1;  // fib(i-2)
    long prev1 = 1;  // fib(i-1)
    long current = 1; // fib(i)

    for (int i = 3; i <= n; i++) {
        current = prev1 + prev2;
        prev2 = prev1;
        prev1 = current;
    }

    return current;
}
```

---

## 3. Space-Optimized DP

```java
// From O(n) to O(1) — keep only needed previous values

// Fibonacci
int prev2 = 0, prev1 = 1;
for(
int i = 2;
i <=n;i++){
int current = prev1 + prev2;
prev2 =prev1;
prev1 =current;
}
        return prev1;

        // House Robber (can't rob adjacent houses)
        int prev2 = 0, prev1 = 0;
for(
int money :houses){
int current = Math.max(prev1, prev2 + money);
prev2 =prev1;
prev1 =current;
}
        return prev1;
```

### When Space Optimization Applies

- **Only need last k values** — if `dp[i]` depends only on `dp[i-1]`, `dp[i-2]`, etc.
- **1D DP array** — easier to optimize than 2D
- **No path reconstruction needed** — if you need to reconstruct the solution, keep full table

### House Robber Example

```java
// Can't rob adjacent houses — maximize total
public int rob(int[] nums) {
    if (nums.length == 0) return 0;
    if (nums.length == 1) return nums[0];

    int prev2 = 0;  // dp[i-2]
    int prev1 = 0;  // dp[i-1]

    for (int money : nums) {
        int current = Math.max(prev1, prev2 + money);
        prev2 = prev1;
        prev1 = current;
    }

    return prev1;
}
```

---

## 4. 2D Grid DP

```java
// GridTraveler pattern: count paths from top-left to bottom-right
int[][] dp = new int[m][n];

// Initialize first row and column
for(
int i = 0;
i<m;i++)dp[i][0]=1;
        for(
int j = 0;
j<n;j++)dp[0][j]=1;

// Fill rest
        for(
int i = 1;
i<m;i++){
        for(
int j = 1;
j<n;j++){
dp[i][j]=dp[i -1][j]+dp[i][j -1];  // From above + from left
        }
        }

        return dp[m -1][n -1];
```

### Characteristics

| Property        | Value                                |
|-----------------|--------------------------------------|
| **Structure**   | 2D array `dp[m][n]`                  |
| **Transitions** | From adjacent cells (up, left, diag) |
| **Complexity**  | O(m×n) time and space                |
| **Best For**    | Grid paths, matrix problems          |

### See Also: development/recursion/GridTraveler.java

```java
// GridTraveler: count paths in m×n grid (can only move right or down)
public static long memo(int x, int y, Map<String, Long> memo) {
    if (x == 1 && y == 1) return 1;  // Base case: 1×1 grid has 1 path
    if (x == 0 || y == 0) return 0;  // Invalid grid

    String key = x + "," + y;
    if (memo.containsKey(key)) return memo.get(key);

    long result = memo(x - 1, y, memo) + memo(x, y - 1, memo);
    memo.put(key, result);
    return result;
}

// Bottom-up version
public static long gridTravelerTabulation(int m, int n) {
    long[][] dp = new long[m + 1][n + 1];
    dp[1][1] = 1;  // Base case

    for (int i = 1; i <= m; i++) {
        for (int j = 1; j <= n; j++) {
            if (i == 1 && j == 1) continue;  // Already set
            dp[i][j] = dp[i - 1][j] + dp[i][j - 1];
        }
    }

    return dp[m][n];
}
```

### Min Path Sum in Grid

```java
// Given grid with costs, find minimum cost path from top-left to bottom-right
public int minPathSum(int[][] grid) {
    int m = grid.length, n = grid[0].length;
    int[][] dp = new int[m][n];

    // Initialize starting cell
    dp[0][0] = grid[0][0];

    // Initialize first row (can only come from left)
    for (int j = 1; j < n; j++) {
        dp[0][j] = dp[0][j - 1] + grid[0][j];
    }

    // Initialize first column (can only come from above)
    for (int i = 1; i < m; i++) {
        dp[i][0] = dp[i - 1][0] + grid[i][0];
    }

    // Fill rest
    for (int i = 1; i < m; i++) {
        for (int j = 1; j < n; j++) {
            dp[i][j] = Math.min(dp[i - 1][j], dp[i][j - 1]) + grid[i][j];
        }
    }

    return dp[m - 1][n - 1];
}

// Space-optimized version (only need previous row)
public int minPathSumOptimized(int[][] grid) {
    int m = grid.length, n = grid[0].length;
    int[] prev = new int[n];

    prev[0] = grid[0][0];
    for (int j = 1; j < n; j++) {
        prev[j] = prev[j - 1] + grid[0][j];
    }

    for (int i = 1; i < m; i++) {
        int[] curr = new int[n];
        curr[0] = prev[0] + grid[i][0];  // First column

        for (int j = 1; j < n; j++) {
            curr[j] = Math.min(prev[j], curr[j - 1]) + grid[i][j];
        }

        prev = curr;
    }

    return prev[n - 1];
}
```

---

## 5. State Machine DP (Buy/Sell Stock Problems)

```java
// Track states: hold (own stock), sold (don't own)
// Transitions: buy (sold → hold), sell (hold → sold)

int hold = -prices[0];  // After buying on day 0
int sold = 0;           // Start with 0 profit

for(
int i = 1;
i<prices.length;i++){
int newHold = Math.max(hold, sold - prices[i]);  // Keep holding or buy today
int newSold = Math.max(sold, hold + prices[i]);  // Keep sold or sell today
hold =newHold;
sold =newSold;
}

        return sold;  // Max profit when ending without stock
```

### Characteristics

| Property        | Value                                 |
|-----------------|---------------------------------------|
| **States**      | hold, sold, cooldown, etc.            |
| **Transitions** | State machine edges (buy, sell, wait) |
| **Complexity**  | O(n) time, O(1) space                 |
| **Best For**    | Stock buy/sell, decision sequences    |

### Best Time to Buy and Sell Stock (Multiple Transactions)

```java
// Unlimited transactions allowed
public int maxProfit(int[] prices) {
    int hold = Integer.MIN_VALUE;  // Initially can't hold
    int sold = 0;

    for (int price : prices) {
        int newHold = Math.max(hold, sold - price);
        int newSold = Math.max(sold, hold + price);
        hold = newHold;
        sold = newSold;
    }

    return sold;
}

// Alternative: greedy approach for unlimited transactions
public int maxProfitGreedy(int[] prices) {
    int profit = 0;
    for (int i = 1; i < prices.length; i++) {
        if (prices[i] > prices[i - 1]) {
            profit += prices[i] - prices[i - 1];  // Capture every upward move
        }
    }
    return profit;
}
```

### Best Time to Buy and Sell Stock with Cooldown

```java
// After selling, must cooldown one day before buying again
public int maxProfitWithCooldown(int[] prices) {
    int hold = Integer.MIN_VALUE;
    int sold = 0;
    int cooldown = 0;

    for (int price : prices) {
        int newHold = Math.max(hold, cooldown - price);  // Can only buy from cooldown
        int newSold = Math.max(sold, hold + price);
        int newCooldown = Math.max(cooldown, sold);      // Cooldown after selling

        hold = newHold;
        sold = newSold;
        cooldown = newCooldown;
    }

    return sold;
}
```

---

## 6. 1D Partition DP (Subset Sum, Word Break)

### Subset Sum Pattern

```java
// Can we partition array into two subsets with equal sum?
public boolean canPartition(int[] nums) {
    int total = Arrays.stream(nums).sum();
    if (total % 2 != 0) return false;  // Odd sum can't be partitioned equally

    int target = total / 2;
    boolean[] dp = new boolean[target + 1];
    dp[0] = true;  // Sum of 0 is always achievable (empty subset)

    for (int num : nums) {
        // Iterate backwards to avoid using same element twice
        for (int j = target; j >= num; j--) {
            dp[j] = dp[j] || dp[j - num];
        }
    }

    return dp[target];
}
```

### Word Break Pattern

```java
// Given string and dictionary, can string be segmented into dictionary words?
public boolean wordBreak(String s, List<String> wordDict) {
    Set<String> dict = new HashSet<>(wordDict);
    int n = s.length();
    boolean[] dp = new boolean[n + 1];
    dp[0] = true;  // Empty prefix is valid

    for (int i = 1; i <= n; i++) {
        for (int j = 0; j < i; j++) {
            if (dp[j] && dict.contains(s.substring(j, i))) {
                dp[i] = true;
                break;
            }
        }
    }

    return dp[n];
}
```

---

## Complexity Summary

| Pattern              | Time        | Space     | Reducible Space |
|----------------------|-------------|-----------|-----------------|
| Top-Down Memoization | O(states)   | O(states) | Depends         |
| Bottom-Up Tabulation | O(states)   | O(states) | Often to O(1)   |
| Space-Optimized      | O(n)        | O(1)      | Already minimal |
| 2D Grid DP           | O(m×n)      | O(m×n)    | Often to O(n)   |
| State Machine DP     | O(n)        | O(1)      | Already minimal |
| 1D Partition DP      | O(n×target) | O(target) | No              |
| Interval DP          | O(n³)       | O(n²)     | No              |

---

## Magic Methods/Patterns

### computeIfAbsent for Memoization

```java
Map<Integer, Long> memo = new HashMap<>();

// Pattern 1: Inline compute
return memo.

computeIfAbsent(n, k ->

fib(k -1) +

fib(k -2));

// Pattern 2: With helper
        return memo.

computeIfAbsent(key, k ->

expensiveComputation(k));

// Pattern 3: Boolean result (existence check)
        return memo.

computeIfAbsent(target, k ->{
        for(
int num :nums){
        if(

canSum(k -num))return true;
        }
        return false;
        });
```

### Arrays.fill for Initialization

```java
// Initialize with specific value
int[] dp = new int[n];
Arrays.

fill(dp, -1);  // Mark as uncomputed

// 2D initialization
int[][] dp = new int[m][n];
for(
int[] row :dp){
        Arrays.

fill(row, Integer.MAX_VALUE);
}

// Base case setup
dp[0]=1;  // First element
dp[1]=1;  // Second element
```

### Backwards Iteration for 0/1 Knapsack

```java
// When each item can only be used ONCE, iterate BACKWARDS
int[] dp = new int[capacity + 1];

for(
int[] item :items){
        int weight = item[0], value = item[1];

// BACKWARDS to avoid using same item twice
    for(
int w = capacity;
w >=weight;w--){
dp[w]=Math.

max(dp[w], dp[w-weight]+value);
    }
            }

// For UNBOUNDED knapsack (item can be used multiple times), iterate FORWARDS
            for(
int[] item :items){
        for(
int w = item[0];
w <=capacity;w++){
dp[w]=Math.

max(dp[w], dp[w-item[0]]+item[1]);
    }
            }
```

---

## Common Gotchas

1. **Stack overflow with deep recursion** — Top-down memoization uses recursion stack. For large inputs (n > 10000), use
   bottom-up to avoid `StackOverflowError`.

   ```java
   // Risky for large n
   long result = memo(n);  // Recursion depth = n
   
   // Safe for any n
   long result = tabulation(n);  // No recursion
   ```

2. **Memo key design for multi-parameter DP** — When state has multiple parameters, use a composite key. Records are
   ideal for type-safety.

   ```java
   // WRONG: String concatenation can have collisions
   String key = target + "," + idx;  // "1,23" vs "12,3"
   
   // CORRECT: Use record
   record State(int target, int idx) {}
   Map<State, Boolean> memo = new HashMap<>();
   ```

3. **Base case ordering** — In bottom-up, ensure base cases are set BEFORE the loop that depends on them. Missing base
   cases cause incorrect results.

   ```java
   // WRONG: missing base case
   int[] dp = new int[n + 1];
   for (int i = 2; i <= n; i++) {  // dp[0] and dp[1] are 0 by default!
       dp[i] = dp[i - 1] + dp[i - 2];
   }
   
   // CORRECT
   dp[0] = 0;
   dp[1] = 1;
   for (int i = 2; i <= n; i++) {
       dp[i] = dp[i - 1] + dp[i - 2];
   }
   ```

4. **Space optimization when full table needed** — Don't optimize to O(1) if you need to reconstruct the solution path.
   Keep full DP table for backtracking.

   ```java
   // If you need to RECONSTRUCT the solution:
   // Keep full table, not just last two values
   int[][] dp = new int[m][n];  // Not reducible
   ```

5. **0/1 Knapsack: iterate backwards** — When each item can be used at most once, iterate weight from high to low to
   avoid using the same item multiple times in one pass.

   ```java
   // 0/1 Knapsack (each item used once): BACKWARDS
   for (int w = capacity; w >= weight; w--) {
       dp[w] = Math.max(dp[w], dp[w - weight] + value);
   }
   
   // Unbounded Knapsack (unlimited use): FORWARDS
   for (int w = weight; w <= capacity; w++) {
       dp[w] = Math.max(dp[w], dp[w - weight] + value);
   }
   ```

6. **Off-by-one errors in DP array size** — If computing `dp[n]`, array size must be `n + 1` to include index `n`.

   ```java
   // WRONG: can't access dp[n]
   int[] dp = new int[n];
   
   // CORRECT
   int[] dp = new int[n + 1];
   ```

---

## See Also

- **development/recursion/AllConstruct.java** — Backtracking alternative to DP; returns all combinations instead of just
  existence/count.

- **development/recursion/BestSum.java** — Optimal substructure pattern; find shortest combination that sums to target.

- **development/recursion/HowSum.java** — Return any valid combination; contrast with CanSum (existence only).

- **BACKTRACKING_GUIDE.md** — Exhaustive search with pruning; alternative to DP when counting all solutions needed.

- **HEAP_GUIDE.md** — Priority queue patterns; complements DP for optimization problems with priority-based selection.

- **MAP_GUIDE.md** — HashMap memoization patterns; `computeIfAbsent` is the key method for top-down DP.

---

## Performance Summary

| Problem Type   | Naive Recursion | DP Solution | Space-Optimized |
|----------------|-----------------|-------------|-----------------|
| Fibonacci      | O(2^n)          | O(n)        | O(1)            |
| CanSum         | O(n^m)          | O(target×n) | O(target)       |
| GridTraveler   | O(2^(m+n))      | O(m×n)      | O(min(m,n))     |
| House Robber   | O(2^n)          | O(n)        | O(1)            |
| Stock Buy/Sell | O(2^n)          | O(n)        | O(1)            |
| Subset Sum     | O(2^n)          | O(n×sum)    | O(sum)          |
| Word Break     | O(2^n)          | O(n²)       | O(n)            |

> **Key insight**: DP transforms exponential time to polynomial by eliminating redundant computation. Space can often be
> further optimized to O(1) when transitions depend on limited prior states.

(End of file - total ~650 lines)
