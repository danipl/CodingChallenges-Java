# Monotonic Stack/Deque Patterns Guide

## Quick-Reference: Pattern Selection Matrix

| Pattern                          | Data Structure   | Maintain Condition   | Time | When to Use                             |
|----------------------------------|------------------|----------------------|------|-----------------------------------------|
| **Next Greater Element (right)** | Decreasing Stack | current > top → pop  | O(n) | Find first larger element to the right  |
| **Next Greater Element (left)**  | Decreasing Stack | current >= top → pop | O(n) | Find first larger element to the left   |
| **Next Smaller Element**         | Increasing Stack | current < top → pop  | O(n) | Find first smaller element to the right |
| **Largest Rectangle**            | Increasing Stack | current < top → calc | O(n) | Histogram area calculation              |
| **Sliding Window Maximum**       | Decreasing Deque | current > last → pop | O(n) | Find max in every k-sized window        |
| **Sliding Window Minimum**       | Increasing Deque | current < last → pop | O(n) | Find min in every k-sized window        |
| **Daily Temperatures**           | Decreasing Stack | current > top → calc | O(n) | Days until warmer temperature           |
| **Trapping Rain Water**          | Decreasing Stack | current > top → trap | O(n) | Water trapped between bars              |

### At-A-Glance Decision Flow

```
Need monotonic structure?
  ├─ YES → What finding?
  │        ├─ Next Greater (right)   → Decreasing Stack (pop when current > top)
  │        ├─ Next Greater (left)    → Decreasing Stack (pop when current >= top)
  │        ├─ Next Smaller           → Increasing Stack (pop when current < top)
  │        ├─ Largest Rectangle      → Increasing Stack (pop when current < top, calc area)
  │        ├─ Sliding Window Max     → Decreasing Deque (addFirst, removeLast when current > last)
  │        ├─ Sliding Window Min     → Increasing Deque (addFirst, removeLast when current < last)
  │        └─ Daily Temperatures     → Decreasing Stack (store indices, pop when current > top)
  └─ NO  → Use regular stack/deque
```

---

## Overview

Monotonic stacks and deques maintain a sorted property (increasing or decreasing) that enables
efficient solutions to "next greater/smaller element" and "sliding window" problems. Each element
is pushed and popped at most once, yielding O(n) total complexity.

---

## 1. Next Greater Element (Right)

**Pattern**: For each element, find the first element to its right that is larger.

**Structure**: Decreasing Stack

**Logic**: Pop when current element > stack top (found next greater for popped elements)

```java
int[] nextGreater(int[] nums) {
    int n = nums.length;
    int[] result = new int[n];
    Arrays.fill(result, -1);
    Deque<Integer> stack = new ArrayDeque<>(); // stores indices

    for (int i = 0; i < n; i++) {
        while (!stack.isEmpty() && nums[i] > nums[stack.peek()]) {
            int idx = stack.pop();
            result[idx] = nums[i]; // nums[i] is next greater for nums[idx]
        }
        stack.push(i);
    }
    return result;
}
```

**When to Use**: LeetCode "Next Greater Element I/II",Daily Temperatures variant

**Time**: O(n) - each index pushed/popped once

---

## 2. Next Greater Element (Left)

**Pattern**: For each element, find the first element to its left that is larger.

**Structure**: Decreasing Stack

**Logic**: Pop when current element >= stack top, then stack top is next greater for current

```java
int[] nextGreaterLeft(int[] nums) {
    int n = nums.length;
    int[] result = new int[n];
    Arrays.fill(result, -1);
    Deque<Integer> stack = new ArrayDeque<>(); // stores indices

    for (int i = 0; i < n; i++) {
        while (!stack.isEmpty() && nums[i] >= nums[stack.peek()]) {
            stack.pop();
        }
        if (!stack.isEmpty()) {
            result[i] = nums[stack.peek()]; // stack top is next greater to left
        }
        stack.push(i);
    }
    return result;
}
```

**When to Use**: Problems requiring left-side boundary detection

**Time**: O(n)

---

## 3. Next Smaller Element

**Pattern**: For each element, find the first element to its right that is smaller.

**Structure**: Increasing Stack

**Logic**: Pop when current element < stack top (found next smaller for popped elements)

```java
int[] nextSmaller(int[] nums) {
    int n = nums.length;
    int[] result = new int[n];
    Arrays.fill(result, -1);
    Deque<Integer> stack = new ArrayDeque<>(); // stores indices

    for (int i = 0; i < n; i++) {
        while (!stack.isEmpty() && nums[i] < nums[stack.peek()]) {
            int idx = stack.pop();
            result[idx] = nums[i]; // nums[i] is next smaller for nums[idx]
        }
        stack.push(i);
    }
    return result;
}
```

**When to Use**: Finding minimum in subarrays, histogram problems

**Time**: O(n)

---

## 4. Daily Temperatures

**Pattern**: For each day, find how many days until a warmer temperature.

**Structure**: Decreasing Stack (stores indices, values are temperatures)

**Logic**: Pop when current temp > popped day's temp, calculate day difference

```java
int[] dailyTemperatures(int[] temps) {
    int n = temps.length;
    int[] result = new int[n];
    Deque<Integer> stack = new ArrayDeque<>(); // stores indices

    for (int i = 0; i < n; i++) {
        while (!stack.isEmpty() && temps[i] > temps[stack.peek()]) {
            int prev = stack.pop();
            result[prev] = i - prev; // days until warmer
        }
        stack.push(i);
    }
    return result;
}
```

**When to Use**: Classic monotonic stack challenge, any "days until X" pattern

**Time**: O(n)

**Space**: O(n) for result + O(n) for stack

---

## 5. Largest Rectangle in Histogram

**Pattern**: Find largest rectangular area in a histogram.

**Structure**: Increasing Stack

**Logic**: Pop when current height < stack top height, calculate area using popped height

```java
int largestRectangleArea(int[] heights) {
    Deque<Integer> stack = new ArrayDeque<>();
    int maxArea = 0;

    for (int i = 0; i <= heights.length; i++) {
        int curr = (i == heights.length) ? 0 : heights[i];

        while (!stack.isEmpty() && curr < heights[stack.peek()]) {
            int height = heights[stack.pop()];
            int right = i - 1;
            int left = stack.isEmpty() ? 0 : stack.peek() + 1;
            int width = right - left + 1;
            maxArea = Math.max(maxArea, height * width);
        }
        stack.push(i);
    }
    return maxArea;
}
```

**When to Use**: LeetCode "Largest Rectangle in Histogram", histogram-based problems

**Time**: O(n)

**Key Insight**: Adding sentinel 0 at end ensures all indices are popped

---

## 6. Sliding Window Maximum

**Pattern**: Find maximum in every k-sized sliding window.

**Structure**: Decreasing Deque (stores indices, maintains max at front)

**Logic**:

- Remove indices outside window from front
- Remove smaller elements from back (they can never be max)
- Add current index to back

```java
int[] maxSlidingWindow(int[] nums, int k) {
    int n = nums.length;
    int[] result = new int[n - k + 1];
    Deque<Integer> deque = new ArrayDeque<>(); // stores indices

    for (int i = 0; i < n; i++) {
        // Remove indices outside window (from front)
        while (!deque.isEmpty() && deque.peekFirst() < i - k + 1) {
            deque.pollFirst();
        }

        // Remove smaller elements (from back)
        while (!deque.isEmpty() && nums[i] >= nums[deque.peekLast()]) {
            deque.pollLast();
        }

        deque.addLast(i);

        // Record max when window is complete
        if (i >= k - 1) {
            result[i - k + 1] = nums[deque.peekFirst()];
        }
    }
    return result;
}
```

**When to Use**: LeetCode "Sliding Window Maximum", O(n) window max queries

**Time**: O(n) - each index added/removed once

**Why Deque**: Need to remove from both ends (front for out-of-window, back for smaller elements)

---

## 7. Sliding Window Minimum

**Pattern**: Find minimum in every k-sized sliding window.

**Structure**: Increasing Deque (stores indices, maintains min at front)

**Logic**: Similar to max, but reverse comparison

```java
int[] minSlidingWindow(int[] nums, int k) {
    int n = nums.length;
    int[] result = new int[n - k + 1];
    Deque<Integer> deque = new ArrayDeque<>(); // stores indices

    for (int i = 0; i < n; i++) {
        // Remove indices outside window (from front)
        while (!deque.isEmpty() && deque.peekFirst() < i - k + 1) {
            deque.pollFirst();
        }

        // Remove larger elements (from back)
        while (!deque.isEmpty() && nums[i] <= nums[deque.peekLast()]) {
            deque.pollLast();
        }

        deque.addLast(i);

        // Record min when window is complete
        if (i >= k - 1) {
            result[i - k + 1] = nums[deque.peekFirst()];
        }
    }
    return result;
}
```

**When to Use**: Finding minimum in sliding windows, same complexity as max

**Time**: O(n)

---

## Implementation: ArrayDeque Best Practices

```java
// Stack operations (for monotonic stack)
Deque<Integer> stack = new ArrayDeque<>();
stack.

push(x);    // Add to front (like stack push)
stack.

pop();      // Remove from front (like stack pop)
stack.

peek();     // View front

// Deque operations (for monotonic deque)
Deque<Integer> deque = new ArrayDeque<>();
deque.

addFirst(x);    // Add to front
deque.

addLast(x);     // Add to back (equivalent to add())
deque.

pollFirst();    // Remove from front
deque.

pollLast();     // Remove from back
deque.

peekFirst();    // View front
deque.

peekLast();     // View back
```

**Key Practice**: Store indices, not values, when you need position information

---

## Complexity Analysis

| Pattern                      | Time | Space | Notes                               |
|------------------------------|------|-------|-------------------------------------|
| Next Greater Element (right) | O(n) | O(n)  | Each index pushed/popped once       |
| Next Greater Element (left)  | O(n) | O(n)  | Same as right                       |
| Next Smaller Element         | O(n) | O(n)  | Same structure, reversed comparison |
| Daily Temperatures           | O(n) | O(n)  | Records index differences           |
| Largest Rectangle            | O(n) | O(n)  | Requires sentinel to flush stack    |
| Sliding Window Maximum       | O(n) | O(k)  | Deque size bounded by window size   |
| Sliding Window Minimum       | O(n) | O(k)  | Same as max                         |

**Why O(n)**: Each element is pushed exactly once and popped at most once. Total operations ≤ 2n.

---

## Common Gotchas

### 1. Stack Empty Edge Case

```java
// BAD: May throw NoSuchElementException
while(!stack.isEmpty()){
int x = stack.pop(); // Always check isEmpty first
}

// GOOD
        if(!stack.

isEmpty()){
int x = stack.pop();
}
```

### 2. Storing Indices vs Values

```java
// BAD: Cannot calculate distances without indices
Deque<Integer> stack = new ArrayDeque<>();
stack.

push(nums[i]); // Lose position info

// GOOD: Store indices for position access
Deque<Integer> stack = new ArrayDeque<>();
stack.

push(i); // Access via nums[i]
```

### 3. Equal Element Handling (< vs <=)

```java
// For "next greater" (strictly larger): use >
while(!stack.isEmpty() &&current >nums[stack.

peek()])

// For "next greater or equal" (non-strict): use >=
        while(!stack.

isEmpty() &&current >=nums[stack.

peek()])

// Choosing matters: could include/duplicate results
```

**Rule of Thumb**:

- Next Greater Element: `>` (strictly larger)
- Next Greater or Equal: `>=`
- Next Smaller Element: `<` (strictly smaller)
- Next Smaller or Equal: `<=`

### 4. Sentinel Values for Flush

```java
// BAD: Last elements may never be processed
for(int i = 0;
i<n;i++){ /* process */ }

// GOOD: Add sentinel to force final processing
        for(
int i = 0;
i <=n;i++){
int curr = (i == n) ? 0 : nums[i]; // Sentinel 0
    while(!stack.

isEmpty() &&curr<nums[stack.

peek()]){ /* ... */ }
        }
```

---

## See Also

### Reference Files

- `ValidMountainArray.java` - Uses monotonic decreasing pattern
- `SquaresOfSortedArray.java` - Two pointers, simpler than monotonic

### Related Guides

- **[TWO_POINTERS_GUIDE.md](TWO_POINTERS_GUIDE.md)** - Simpler pattern for sorted arrays
- **[QUEUE_GUIDE.md](QUEUE_GUIDE.md)** - Regular queues vs monotonic variants
- `ValidMountainArray.java` - Basic decreasing sequence check
- `SquaresOfSortedArray.java` - Two-pointer square sorting

### Classic LeetCode Problems

- [739] Daily Temperatures
- [496] Next Greater Element I
- [503] Next Greater Element II
- [84] Largest Rectangle in Histogram
- [239] Sliding Window Maximum
- [1944] Number of Visible People in a Queue
- [42] Trapping Rain Water (variant)
