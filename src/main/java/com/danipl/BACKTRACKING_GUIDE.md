# Java 21 Backtracking Patterns Guide

## Quick-Reference: Backtracking Pattern Selection Matrix

| Pattern              | Decision Tree              | Pruning Strategy         | Duplicate Handling        | Example Problem               | Time Complexity   |
|----------------------|----------------------------|--------------------------|---------------------------|-------------------------------|-------------------|
| **Permutations**     | All orderings matter       | Used set/swap            | Skip if element used      | Permutations, Permutations II | O(n!)             |
| **Combinations**     | Choose k from n            | Remaining count check    | Start from next index     | Combinations, Combination Sum | O(C(n,k))         |
| **Subsets**          | Power set (2^n)            | Index progression        | Sort + skip duplicates    | Subsets, Subsets II           | O(2^n)            |
| **N-Queens**         | Row-by-row placement       | Column/diagonal conflict | N/A (unique by nature)    | N-Queens, N-Queens II         | O(n!)             |
| **Word Search**      | Grid DFS with backtrack    | Bounds + visited check   | Mark visited, then unmark | Word Search, Word Search II   | O(m×n×4^L)        |
| **Partition/Cut**    | Split at each position     | Valid partition check    | Skip invalid splits       | Palindrome Partitioning       | O(n×2^n)          |
| **General Template** | `backtrack(path, choices)` | Constraint propagation   | Sort first + skip         | All above                     | Problem-dependent |

### At-A-Glance Decision Flow

```
Need to enumerate all solutions / explore all possibilities?
├─ YES → Order matters?
│          ├─ YES → Permutations
│          │          ├─ All unique → Used set during recursion
│          │          └─ With duplicates → Sort + skip if nums[i] == nums[i-1] && !used[i-1]
│          └─ NO → Order doesn't matter
│                     ├─ Fixed size k? → Combinations
│                     │                  └─ Prune if remaining < needed
│                     ├─ All sizes (power set)? → Subsets
│                     │                          └─ Include or exclude each element
│                     └─ Constrained by rules? → N-Queens / Sudoku
│                                                └─ Constraint checking before placement
├─ Grid-based with path?
│          └─ Word Search / Number of Islands
│                     ├─ Mark visited before recurse
│                     └─ Unmark (backtrack) after recurse
└─ Partition string/array?
           └─ Palindrome Partitioning / Split Array
                      └─ Try all split positions, validate each part
```

---

## Overview

Backtracking is a systematic way to explore all possible solutions by building candidates incrementally and abandoning (
backtracking) when a candidate cannot possibly lead to a valid solution. It's **exhaustive search with pruning**.

**Key components:**

1. **Choice**: What decision to make at each step
2. **Constraints**: Rules that prune invalid paths early
3. **Goal**: When to record a complete solution
4. **Backtrack**: Undo choice to explore alternatives

**Backtracking template:**

```java
void backtrack(Path path, Choices available) {
    if (isSolution(path)) {
        results.add(new ArrayList<>(path));
        return;
    }

    for (Choice c : available) {
        if (isValid(c, path)) {      // Constraint check
            path.add(c);              // Make choice
            backtrack(path, updatedChoices);
            path.removeLast();        // Undo choice (backtrack)
        }
    }
}
```

**Java 21 improvements:**

- `List.removeLast()` instead of `remove(size - 1)`
- Records for path/constraint state
- Pattern matching for cleaner validation
- Switch expressions for choice handling

---

## 1. Permutations (All Orderings)

```java
// Given [1,2,3], generate all orderings:
// [1,2,3], [1,3,2], [2,1,3], [2,3,1], [3,1,2], [3,2,1]

public List<List<Integer>> permute(int[] nums) {
    List<List<Integer>> result = new ArrayList<>();
    backtrack(nums, new ArrayList<>(), new boolean[nums.length], result);
    return result;
}

private void backtrack(int[] nums, List<Integer> path, boolean[] used, List<List<Integer>> result) {
    if (path.size() == nums.length) {
        result.add(new ArrayList<>(path));
        return;
    }

    for (int i = 0; i < nums.length; i++) {
        if (used[i]) continue;  // Skip already used

        path.add(nums[i]);
        used[i] = true;

        backtrack(nums, path, used, result);

        used[i] = false;        // Undo
        path.removeLast();      // Java 21
    }
}
```

### Characteristics

| Property       | Value                               |
|----------------|-------------------------------------|
| **Order**      | Matters — [1,2] ≠ [2,1]             |
| **Structure**  | Used set to track included elements |
| **Complexity** | O(n!) — n factorial                 |
| **Duplicates** | Need special handling (see below)   |

### Permutations with Duplicates (Permutations II)

```java
// Given [1,1,2], generate unique permutations:
// [1,1,2], [1,2,1], [2,1,1]

public List<List<Integer>> permuteUnique(int[] nums) {
    List<List<Integer>> result = new ArrayList<>();
    Arrays.sort(nums);  // CRITICAL: sort to group duplicates
    backtrackUnique(nums, new ArrayList<>(), new boolean[nums.length], result);
    return result;
}

private void backtrackUnique(int[] nums, List<Integer> path, boolean[] used, List<List<Integer>> result) {
    if (path.size() == nums.length) {
        result.add(new ArrayList<>(path));
        return;
    }

    for (int i = 0; i < nums.length; i++) {
        if (used[i]) continue;

        // Skip duplicates: if same as previous AND previous not used, skip
        if (i > 0 && nums[i] == nums[i - 1] && !used[i - 1]) {
            continue;
        }

        path.add(nums[i]);
        used[i] = true;

        backtrackUnique(nums, path, used, result);

        used[i] = false;
        path.removeLast();
    }
}
```

### Swap-Based Permutations (In-Place)

```java
// Alternative: swap elements in place instead of using used[] array
public List<List<Integer>> permuteSwap(int[] nums) {
    List<List<Integer>> result = new ArrayList<>();
    backtrackSwap(nums, 0, result);
    return result;
}

private void backtrackSwap(int[] nums, int start, List<List<Integer>> result) {
    if (start == nums.length) {
        // Convert array to list
        result.add(Arrays.stream(nums).boxed().toList());
        return;
    }

    for (int i = start; i < nums.length; i++) {
        swap(nums, start, i);           // Place nums[i] at position start
        backtrackSwap(nums, start + 1, result);
        swap(nums, start, i);           // Restore (backtrack)
    }
}

private void swap(int[] nums, int i, int j) {
    int temp = nums[i];
    nums[i] = nums[j];
    nums[j] = temp;
}
```

---

## 2. Combinations (Choose k from n)

```java
// Given n=4, k=2, generate all combinations:
// [1,2], [1,3], [1,4], [2,3], [2,4], [3,4]

public List<List<Integer>> combine(int n, int k) {
    List<List<Integer>> result = new ArrayList<>();
    backtrackCombine(n, k, 1, new ArrayList<>(), result);
    return result;
}

private void backtrackCombine(int n, int k, int start, List<Integer> path, List<List<Integer>> result) {
    if (path.size() == k) {
        result.add(new ArrayList<>(path));
        return;
    }

    // Pruning: only iterate while we can still fill k elements
    // Need k - path.size() more, so stop when i > n - (k - path.size()) + 1
    for (int i = start; i <= n - (k - path.size()) + 1; i++) {
        path.add(i);
        backtrackCombine(n, k, i + 1, path, result);  // i+1: next must be larger
        path.removeLast();
    }
}
```

### Characteristics

| Property       | Value                              |
|----------------|------------------------------------|
| **Order**      | Does NOT matter — [1,2] = [2,1]    |
| **Structure**  | Start index ensures no repeats     |
| **Complexity** | O(C(n,k)) — binomial coefficient   |
| **Pruning**    | Stop early if not enough remaining |

### Combination Sum (With Repetition Allowed)

```java
// Given candidates [2,3,6,7] and target 7:
// [[2,2,3], [7]]

public List<List<Integer>> combinationSum(int[] candidates, int target) {
    List<List<Integer>> result = new ArrayList<>();
    Arrays.sort(candidates);  // For pruning
    backtrackSum(candidates, target, 0, new ArrayList<>(), result);
    return result;
}

private void backtrackSum(int[] candidates, int target, int start,
                          List<Integer> path, List<List<Integer>> result) {
    if (target == 0) {
        result.add(new ArrayList<>(path));
        return;
    }

    for (int i = start; i < candidates.length; i++) {
        if (candidates[i] > target) break;  // Pruning: can't reach target

        path.add(candidates[i]);
        backtrackSum(candidates, target - candidates[i], i, path, result);  // i not i+1: reuse allowed
        path.removeLast();
    }
}
```

### Combination Sum II (Each Element Used Once)

```java
// Given candidates [10,1,2,7,6,1,5] and target 8:
// [[1,1,6], [1,2,5], [1,7], [2,6]]

public List<List<Integer>> combinationSum2(int[] candidates, int target) {
    List<List<Integer>> result = new ArrayList<>();
    Arrays.sort(candidates);  // CRITICAL: sort to skip duplicates
    backtrackSum2(candidates, target, 0, new ArrayList<>(), result);
    return result;
}

private void backtrackSum2(int[] candidates, int target, int start,
                           List<Integer> path, List<List<Integer>> result) {
    if (target == 0) {
        result.add(new ArrayList<>(path));
        return;
    }

    for (int i = start; i < candidates.length; i++) {
        if (candidates[i] > target) break;  // Pruning

        // Skip duplicates at same level
        if (i > start && candidates[i] == candidates[i - 1]) {
            continue;
        }

        path.add(candidates[i]);
        backtrackSum2(candidates, target - candidates[i], i + 1, path, result);  // i+1: each used once
        path.removeLast();
    }
}
```

---

## 3. Subsets (Power Set)

```java
// Given [1,2,3], generate all subsets:
// [], [1], [2], [3], [1,2], [1,3], [2,3], [1,2,3]

public List<List<Integer>> subsets(int[] nums) {
    List<List<Integer>> result = new ArrayList<>();
    backtrackSubsets(nums, 0, new ArrayList<>(), result);
    return result;
}

private void backtrackSubsets(int[] nums, int start, List<Integer> path, List<List<Integer>> result) {
    // Add current subset (including empty set at start)
    result.add(new ArrayList<>(path));

    for (int i = start; i < nums.length; i++) {
        path.add(nums[i]);
        backtrackSubsets(nums, i + 1, path, result);
        path.removeLast();
    }
}
```

### Characteristics

| Property       | Value                           |
|----------------|---------------------------------|
| **Order**      | Does NOT matter                 |
| **Size**       | Varies (0 to n)                 |
| **Complexity** | O(2^n) — power set              |
| **Structure**  | Include or exclude each element |

### Subsets with Duplicates (Subsets II)

```java
// Given [1,2,2], generate unique subsets:
// [], [1], [2], [1,2], [2,2], [1,2,2]

public List<List<Integer>> subsetsWithDup(int[] nums) {
    List<List<Integer>> result = new ArrayList<>();
    Arrays.sort(nums);  // CRITICAL: group duplicates
    backtrackSubsetsDup(nums, 0, new ArrayList<>(), result);
    return result;
}

private void backtrackSubsetsDup(int[] nums, int start, List<Integer> path, List<List<Integer>> result) {
    result.add(new ArrayList<>(path));

    for (int i = start; i < nums.length; i++) {
        // Skip duplicates at same level
        if (i > start && nums[i] == nums[i - 1]) {
            continue;
        }

        path.add(nums[i]);
        backtrackSubsetsDup(nums, i + 1, path, result);
        path.removeLast();
    }
}
```

---

## 4. N-Queens (Constraint Satisfaction)

```java
// Place n queens on n×n board so no two attack each other
// Queens attack: same row, column, or diagonal

public List<List<String>> solveNQueens(int n) {
    List<List<String>> result = new ArrayList<>();
    char[][] board = new char[n][n];
    for (char[] row : board) {
        Arrays.fill(row, '.');
    }

    // Track conflicts: column, diagonal (row-col), anti-diagonal (row+col)
    boolean[] cols = new boolean[n];
    boolean[] diag1 = new boolean[2 * n];  // row - col + n offset
    boolean[] diag2 = new boolean[2 * n];  // row + col

    backtrackNQueens(board, 0, cols, diag1, diag2, result);
    return result;
}

private void backtrackNQueens(char[][] board, int row,
                              boolean[] cols, boolean[] diag1, boolean[] diag2,
                              List<List<String>> result) {
    if (row == board.length) {
        result.add(boardToResult(board));
        return;
    }

    for (int col = 0; col < board.length; col++) {
        int d1 = row - col + board.length;  // Offset to make non-negative
        int d2 = row + col;

        if (cols[col] || diag1[d1] || diag2[d2]) {
            continue;  // Under attack
        }

        // Place queen
        board[row][col] = 'Q';
        cols[col] = true;
        diag1[d1] = true;
        diag2[d2] = true;

        backtrackNQueens(board, row + 1, cols, diag1, diag2, result);

        // Remove queen (backtrack)
        board[row][col] = '.';
        cols[col] = false;
        diag1[d1] = false;
        diag2[d2] = false;
    }
}

private List<String> boardToResult(char[][] board) {
    List<String> result = new ArrayList<>();
    for (char[] row : board) {
        result.add(new String(row));
    }
    return result;
}
```

### Characteristics

| Property         | Value                           |
|------------------|---------------------------------|
| **Structure**    | Row-by-row placement            |
| **Constraints**  | Column, diagonal, anti-diagonal |
| **Complexity**   | O(n!) — heavily pruned          |
| **Optimization** | Bitmask for conflict tracking   |

---

## 5. Word Search (Grid DFS)

```java
// Given 2D board and word, find if word exists in grid
// Word can be constructed from adjacent cells (horizontal/vertical)

public boolean exist(char[][] board, String word) {
    int m = board.length, n = board[0].length;
    boolean[][] visited = new boolean[m][n];

    for (int i = 0; i < m; i++) {
        for (int j = 0; j < n; j++) {
            if (board[i][j] == word.charAt(0) &&
                    backtrackWord(board, word, i, j, 0, visited)) {
                return true;
            }
        }
    }

    return false;
}

private boolean backtrackWord(char[][] board, String word, int row, int col,
                              int idx, boolean[][] visited) {
    // Base case: found entire word
    if (idx == word.length()) {
        return true;
    }

    // Bounds check
    if (row < 0 || row >= board.length || col < 0 || col >= board[0].length) {
        return false;
    }

    // Already visited or character mismatch
    if (visited[row][col] || board[row][col] != word.charAt(idx)) {
        return false;
    }

    // Mark visited
    visited[row][col] = true;

    // Explore 4 directions
    boolean found = backtrackWord(board, word, row + 1, col, idx + 1, visited) ||
            backtrackWord(board, word, row - 1, col, idx + 1, visited) ||
            backtrackWord(board, word, row, col + 1, idx + 1, visited) ||
            backtrackWord(board, word, row, col - 1, idx + 1, visited);

    // Unmark (backtrack)
    visited[row][col] = false;

    return found;
}
```

### Direction Arrays Pattern

```java
// More elegant: use direction arrays for 4 directions
private static final int[][] DIRECTIONS = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};

private boolean backtrackWord(char[][] board, String word, int row, int col,
                              int idx, boolean[][] visited) {
    if (idx == word.length()) return true;

    if (row < 0 || row >= board.length || col < 0 || col >= board[0].length ||
            visited[row][col] || board[row][col] != word.charAt(idx)) {
        return false;
    }

    visited[row][col] = true;

    for (int[] dir : DIRECTIONS) {
        if (backtrackWord(board, word, row + dir[0], col + dir[1], idx + 1, visited)) {
            return true;
        }
    }

    visited[row][col] = false;
    return false;
}
```

### Characteristics

| Property       | Value                              |
|----------------|------------------------------------|
| **Structure**  | DFS on grid with backtracking      |
| **Visited**    | Mark before recurse, unmark after  |
| **Complexity** | O(m×n×4^L) where L = word length   |
| **Directions** | 4 adjacent (up, down, left, right) |

---

## General Backtracking Template

```java
public class BacktrackingTemplate {

    // Generic backtracking structure
    public void solve(Input input) {
        List<Result> results = new ArrayList<>();
        backtrack(input, new Path(), results);
        return results;
    }

    private void backtrack(Input input, Path path, List<Result> results) {
        // Base case: found a complete solution
        if (isSolution(path)) {
            results.add(path.clone());  // Deep copy
            return;
        }

        // Try each possible choice
        for (Choice choice : getChoices(input, path)) {
            // Constraint check (pruning)
            if (!isValid(choice, path, input)) {
                continue;
            }

            // Make choice
            path.add(choice);
            apply(choice, input);  // Optional: modify input state

            // Recurse
            backtrack(input, path, results);

            // Undo choice (backtrack)
            unapply(choice, input);  // Optional: restore input state
            path.removeLast();
        }
    }

    // Implementation hooks (override in specific problems)
    private boolean isSolution(Path path) { /* ... */ }

    private List<Choice> getChoices(Input input, Path path) { /* ... */ }

    private boolean isValid(Choice choice, Path path, Input input) { /* ... */ }

    private void apply(Choice choice, Input input) { /* ... */ }

    private void unapply(Choice choice, Input input) { /* ... */ }
}
```

---

## Optimization Techniques

### 1. Sorting for Pruning

```java
// Sort to enable early termination and duplicate skipping
Arrays.sort(candidates);

for(
int i = start;
i<candidates.length;i++){
        if(candidates[i]>target)break;  // Early termination
        if(i >start &&candidates[i]==candidates[i -1])continue;  // Skip duplicates
        // ...
        }
```

### 2. Constraint Propagation

```java
// N-Queens: track conflicts to O(1) validation
boolean[] cols = new boolean[n];
boolean[] diag1 = new boolean[2 * n];
boolean[] diag2 = new boolean[2 * n];

// O(1) check instead of O(n) board scan
if(cols[col]||diag1[row -col +n]||diag2[row +col]){
        continue;
        }
```

### 3. Bitmask for State (Advanced)

```java
// Use integer bitmask for visited/used state (space optimization)
int usedMask = 0;  // Bit i set = element i used

// Check if used
if((usedMask &(1<<i))!=0)continue;

// Mark used
usedMask |=(1<<i);

// Unmark (passed in recursion, auto-restored)
backtrack(usedMask |(1<<i));
```

---

## Common Gotchas

1. **Not restoring state after backtrack** — This is the most common bug. Always undo modifications after recursive
   call.

   ```java
   // WRONG: missing undo
   path.add(choice);
   backtrack(path);
   // path still has choice added!
   
   // CORRECT
   path.add(choice);
   backtrack(path);
   path.removeLast();  // Undo
   ```

2. **Infinite recursion from not marking visited** — In grid DFS, failing to mark visited causes infinite loops.

   ```java
   // WRONG: no visited tracking
   backtrack(row + 1, col);  // Will come back to same cell
    
   // CORRECT
   visited[row][col] = true;
   backtrack(row + 1, col);
   visited[row][col] = false;
   ```

3. **Duplicate results from not skipping duplicates** — Sort first, then skip if same as previous at same recursion
   level.

   ```java
   // WRONG: produces duplicate subsets for [1,2,2]
   backtrack(nums, 0, path, result);
   
   // CORRECT
   Arrays.sort(nums);
   if (i > start && nums[i] == nums[i - 1]) continue;
   ```

4. **Space blowup from path copies** — Always create a NEW copy when adding to results. Don't add the same list
   reference.

   ```java
   // WRONG: all results point to same (empty) list
   results.add(path);
   
   // CORRECT: deep copy
   results.add(new ArrayList<>(path));
   ```

5. **Off-by-one in loop bounds** — Combination/_subset loops often have subtle bounds. Test edge cases carefully.

   ```java
   // For combinations: stop when not enough remaining
   for (int i = start; i <= n - (k - path.size()) + 1; i++) {
   ```

6. **Modifying shared state across branches** — If using a shared visited array or state object, ensure it's properly
   restored. Consider passing copies or using local state.

   ```java
   // Safer: use local variable for state
   int newMask = mask | (1 << i);
   backtrack(newMask);  // Original mask unchanged
   ```

7. **Not pruning early enough** — The power of backtracking is pruning invalid branches EARLY. Check constraints at the
   start of the function.

   ```java
   // Put constraints FIRST
   if (target < 0) return;  // Prune
   if (path.size() > limit) return;  // Prune
   
   // Then continue with recursion
   ```

---

## See Also

- **development/recursion/AllConstruct.java** — Backtracking pattern to return ALL ways to construct target from word
  bank; contrast with CanSum (existence only).

- **DP_PATTERNS_GUIDE.md** — Dynamic Programming alternative when counting/optimal solution needed without enumeration.
  DP computes counts; backtracking enumerates solutions.

- **development/recursion/BestSum.java** — Optimal substructure with backtracking; return shortest combination requires
  exploring all valid options.

- **HEAP_GUIDE.md** — Top-K patterns can complement backtracking when only top solutions needed (prune based on heap
  threshold).

- **TREE_GUIDE.md** — Tree traversal patterns; backtracking is essentially DFS with state restoration on general
  decision trees.

---

## Complexity Summary

| Pattern                | Time Complexity | Space Complexity | Notes                      |
|------------------------|-----------------|------------------|----------------------------|
| Permutations           | O(n!)           | O(n)             | n! grows extremely fast    |
| Permutations with Dups | O(n!/k!)        | O(n)             | k = duplicate count        |
| Combinations C(n,k)    | O(C(n,k))       | O(k)             | Binomial coefficient       |
| Subsets (Power Set)    | O(2^n)          | O(n)             | 2^n for all subsets        |
| N-Queens               | O(n!)           | O(n)             | Heavily pruned in practice |
| Word Search            | O(m×n×4^L)      | O(L)             | L = word length            |
| Sudoku Solver          | O(9^(n×n))      | O(n×n)           | Standard 9×9 is manageable |

> **Note**: Backtracking is exponential in worst case. Pruning and constraints are essential for practical performance.

---

## Performance Summary

| Problem                 | Naive Enumeration | With Pruning   | Optimization Used       |
|-------------------------|-------------------|----------------|-------------------------|
| Permutations [1..10]    | 3.6M ops          | 3.6M ops       | None (must explore all) |
| N-Queens n=8            | 16M naive         | ~2K ops        | Conflict tracking       |
| Word Search 10×10, L=10 | 10^16 worst       | ~10^4 actual   | Early mismatch cutoff   |
| Combination Sum         | Exponential       | Heavily pruned | Sum > target cutoff     |
| Subsets with Dups       | O(2^n)            | O(unique)      | Sort + skip duplicates  |

(End of file - total ~620 lines)
