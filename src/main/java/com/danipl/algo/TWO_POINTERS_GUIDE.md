# Two Pointers and Sliding Window Patterns Guide

## Quick-Reference: Pattern Selection Matrix

| Pattern                         | Array Requirement              | Time | Space | When to Use                                        | Example                                                   |
|---------------------------------|--------------------------------|------|-------|----------------------------------------------------|-----------------------------------------------------------|
| **Opposite Ends**               | Sorted (usually)               | O(n) | O(1)  | Two-sum in sorted array, palindrome, merge sorted  | `MergeSortedArray.java`, `RemoveElement.java`             |
| **Sliding Window Fixed**        | Any array                      | O(n) | O(1)  | Max/min sum of k elements, average of subarray     | `MaxConsecutiveOnes.java`                                 |
| **Sliding Window Variable**     | Any array                      | O(n) | O(1)  | Longest/shortest subarray with constraint          | `RansomNote.java`, `SquaresOfSortedArray.java`            |
| **Fast/Slow Pointers**          | Linked list or cycle detection | O(n) | O(1)  | Middle element, cycle detection, happy number      | `MiddleOfTheLinkedList.java`, `LinkedListCycle.java`      |
| **Three Pointers (Dutch Flag)** | Any array                      | O(n) | O(1)  | Partition into 3 groups, sort colors               | `SortArrayByParity.java`, `MoveZeroes.java`               |
| **Same Direction (In-Place)**   | Any array                      | O(n) | O(1)  | Remove duplicates, compress array, filter elements | `RemoveDuplicatesFromSortedArray.java`, `MoveZeroes.java` |

### At-A-Glance Decision Flow

```
Is the data structure a Linked List or detecting a cycle?
  ├─ YES → Use Fast/Slow Pointers
  │          ├─ Find middle → slow moves 1, fast moves 2
  │          ├─ Detect cycle → slow moves 1, fast moves 2, check collision
  │          └─ Find cycle start → reset one pointer to head after collision
  └─ NO  → Working with an array?
             ├─ YES → Are you looking for a subarray/substring?
             │          ├─ YES → Fixed size k? → Sliding Window Fixed
             │          │          └─ Example: max sum of k consecutive elements
             │          └─ NO  → Variable size with constraint? → Sliding Window Variable
             │                     └─ Example: longest substring without repeating chars
             └─ NO  → Are you merging/comparing two ends or partitioning?
                        ├─ Sorted array two-sum or palindrome? → Opposite Ends
                        ├─ Remove/filter elements in-place? → Same Direction (reader/writer)
                        └─ Partition into 2-3 groups? → Three Pointers (Dutch Flag)
```

### Opposite Ends Pattern Decision Tree

```
Array is sorted and you need to find a pair or check symmetry?
  ├─ Two-sum with target → left=0, right=length-1, move based on sum comparison
  ├─ Palindrome check → left=0, right=length-1, compare and move inward
  ├─ Merge two sorted arrays → start from end to avoid overwriting
  └─ Container with most water → move the shorter line inward
```

### Sliding Window Decision Tree

```
Looking for contiguous subarray/substring?
  ├─ Fixed size k?
  │   ├─ Calculate first window sum
  │   ├─ Slide: subtract element leaving, add element entering
  │   └─ Track max/min as you slide
  └─ Variable size with constraint?
      ├─ Expand right until constraint violated
      ├─ Contract left until constraint satisfied again
      └─ Track best valid window size
```

---

## Overview

Two Pointers and Sliding Window are fundamental techniques for solving array and linked list problems efficiently. These
patterns reduce time complexity from O(n²) or O(n log n) to O(n) by making a single pass through the data with clever
pointer manipulation.

The key insight: instead of using nested loops or creating new data structures, maintain two (or more) indices that move
through the data according to specific rules, achieving O(n) time with O(1) space.

---

## 1. Opposite Ends (Converging Pointers)

```java
// Classic template: left and right move toward each other
int left = 0;
int right = array.length - 1;

while(left<right){
        // Process array[left] and array[right]
        if(condition){
left++;
        }else{
right--;
        }
        }
```

### Characteristics

| Property         | Value                           |
|------------------|---------------------------------|
| **Requirements** | Sorted array (usually)          |
| **Pointers**     | Two: start at opposite ends     |
| **Movement**     | Converge toward center          |
| **Time**         | O(n) — single pass              |
| **Space**        | O(1) — no extra data structures |

### When to Use

- **Sorted array two-sum**: Find pair that sums to target
- **Palindrome check**: Verify symmetry from outside-in
- **Merge sorted arrays**: Combine without extra space (merge from end)
- **Container problems**: Maximize area/volume between two lines
- **Remove element in-place**: Swap with end and shrink

### Classic Examples

#### Two-Sum in Sorted Array

```java
public int[] twoSum(int[] numbers, int target) {
    int left = 0;
    int right = numbers.length - 1;

    while (left < right) {
        int sum = numbers[left] + numbers[right];
        if (sum == target) {
            return new int[]{left + 1, right + 1}; // 1-indexed
        } else if (sum < target) {
            left++;  // Need larger sum
        } else {
            right--; // Need smaller sum
        }
    }
    return new int[]{-1, -1};
}
```

**Why it works**: Since the array is sorted:

- If sum is too small, moving left pointer right increases sum
- If sum is too large, moving right pointer left decreases sum
- We eliminate one element per iteration: O(n) guarantee

#### Palindrome Check

```java
public boolean isPalindrome(String s) {
    int left = 0;
    int right = s.length() - 1;

    while (left < right) {
        // Skip non-alphanumeric characters
        while (left < right && !Character.isLetterOrDigit(s.charAt(left))) {
            left++;
        }
        while (left < right && !Character.isLetterOrDigit(s.charAt(right))) {
            right--;
        }

        if (Character.toLowerCase(s.charAt(left)) != Character.toLowerCase(s.charAt(right))) {
            return false;
        }
        left++;
        right--;
    }
    return true;
}
```

#### Merge Sorted Arrays (from `MergeSortedArray.java`)

**Problem**: Merge `nums2` into `nums1` where `nums1` has extra space at the end.

**Naive approach** (existing code):

```java
public void merge(int[] nums1, int m, int[] nums2, int n) {
    // Copy nums2 to end of nums1
    for (int pos = 0; pos < n; pos++) {
        nums1[m + pos] = nums2[pos];
    }
    Arrays.sort(nums1); // O((m+n)log(m+n)) - inefficient!
}
```

**Optimal two-pointer approach** (merge from end):

```java
public void merge(int[] nums1, int m, int[] nums2, int n) {
    int p1 = m - 1;      // Last element in nums1's initialized part
    int p2 = n - 1;      // Last element in nums2
    int writer = m + n - 1; // Write position (end of nums1)

    while (p2 >= 0) {
        if (p1 >= 0 && nums1[p1] > nums2[p2]) {
            nums1[writer--] = nums1[p1--];
        } else {
            nums1[writer--] = nums2[p2--];
        }
    }
    // Time: O(m + n), Space: O(1)
}
```

**Why merge from the end?**

- If we merged from the front, we'd overwrite unprocessed elements in nums1
- Merging from end uses the empty space as a buffer
- Always place the larger element at the write position

#### Remove Element In-Place

From `RemoveElement.java`:

```java
public int removeElement(int[] nums, int val) {
    int left = 0;
    int right = nums.length - 1;

    while (left <= right) {
        if (nums[left] == val) {
            // Swap with right, then shrink
            nums[left] = nums[right];
            right--;
        } else {
            left++;
        }
    }
    return right + 1; // New length
}
```

**Alternative: Same-direction approach** (faster when matches are rare):

```java
public int removeElement(int[] nums, int val) {
    int write = 0;
    for (int read = 0; read < nums.length; read++) {
        if (nums[read] != val) nums[write++] = nums[read];
    }
    return write;
}
```

---

## 2. Sliding Window — Fixed Size

```java
int windowSum = 0;
for(
int i = 0;
i<k;i++)windowSum +=array[i]; // First window

int maxSum = windowSum;
for(
int i = k;
i<array.length;i++){
windowSum +=array[i]-array[i -k]; // Slide: add new, remove old
maxSum =Math.

max(maxSum, windowSum);
}
```

### Characteristics

| Property        | Value                          |
|-----------------|--------------------------------|
| **Window Size** | Fixed at k elements            |
| **Movement**    | Slide right by one position    |
| **Update**      | Subtract leaving, add entering |
| **Time**        | O(n) — single pass             |
| **Space**       | O(1)                           |

### When to Use

- Maximum/minimum sum of k consecutive elements
- Average of all subarrays of size k
- Count occurrences in fixed-size window

### Example: Maximum Average Subarray

```java
public double findMaxAverage(int[] nums, int k) {
    int sum = 0;
    for (int i = 0; i < k; i++) sum += nums[i];
    int maxSum = sum;
    for (int i = k; i < nums.length; i++) {
        sum += nums[i] - nums[i - k];
        maxSum = Math.max(maxSum, sum);
    }
    return (double) maxSum / k;
}
```

    int sum = 0;
    for (int i = 0; i < k; i++) {
        sum += nums[i];
    }
    
    int maxSum = sum;
    
    for (int i = k; i < nums.length; i++) {
        sum += nums[i] - nums[i - k];
        maxSum = Math.max(maxSum, sum);
    }
    
    return (double) maxSum / k;

}

```

### Example: Max Consecutive Ones (from `MaxConsecutiveOnes.java`)

```java
public int findMaxConsecutiveOnes(int[] nums) {
    int maxCount = 0;
    int currentCount = 0;
    
    for (int num : nums) {
        if (num == 1) {
            currentCount++;
            maxCount = Math.max(maxCount, currentCount);
        } else {
            currentCount = 0;
        }
    }
    
    return maxCount;
}
```

**Variation**: With at most one flip (more complex sliding window with constraint).

---

## 3. Sliding Window — Variable Size

```java
int left = 0;
for(
int right = 0;
right<array.length;right++){

addElement(array[right]);           // Expand
    while(!

isValid()){                // Contract while invalid

removeElement(array[left++]);
    }

updateAnswer(right -left+1);     // Valid window
}
```

### Characteristics

| Property        | Value                                  |
|-----------------|----------------------------------------|
| **Window Size** | Dynamic, changes based on constraint   |
| **Movement**    | Right always advances; left shrinks    |
| **Invariant**   | Window is valid after inner while loop |
| **Time**        | O(n) — each element added/removed once |
| **Space**       | O(1) or O(k) depending on tracking     |

### When to Use

- Longest substring without repeating characters
- Longest/shortest subarray with sum constraint
- Character frequency constraints (`RansomNote.java` variant)

### Example: Longest Subarray with Sum ≤ K

```java
public int longestSubarray(int[] nums, int k) {
    int left = 0, sum = 0, maxLength = 0;
    for (int right = 0; right < nums.length; right++) {
        sum += nums[right];
        while (sum > k) {
            sum -= nums[left++];
        }
        maxLength = Math.max(maxLength, right - left + 1);
    }
    return maxLength;
}
```

### Example: Smallest Subarray with Sum ≥ Target

```java
public int minSubArrayLen(int target, int[] nums) {
    int left = 0, sum = 0, minLength = Integer.MAX_VALUE;
    for (int right = 0; right < nums.length; right++) {
        sum += nums[right];
        while (sum >= target) {
            minLength = Math.min(minLength, right - left + 1);
            sum -= nums[left++];
        }
    }
    return minLength == Integer.MAX_VALUE ? 0 : minLength;
}
```

### Example: Character Frequency (`RansomNote.java`)

`RansomNote.java` uses HashMap for character counting. For sliding window substring problems:

```java
public int minWindowSubstring(String s, String t) {
    Map<Character, Integer> need = new HashMap<>();
    for (char c : t.toCharArray()) need.put(c, need.getOrDefault(c, 0) + 1);

    int left = 0, minLength = Integer.MAX_VALUE, minStart = 0, matched = 0;
    for (int right = 0; right < s.length(); right++) {
        char rightChar = s.charAt(right);
        if (need.containsKey(rightChar)) {
            need.put(rightChar, need.get(rightChar) - 1);
            if (need.get(rightChar) == 0) matched++;
        }
        while (matched == need.size()) {
            if (right - left + 1 < minLength) {
                minLength = right - left + 1;
                minStart = left;
            }
            char leftChar = s.charAt(left);
            if (need.containsKey(leftChar)) {
                need.put(leftChar, need.get(leftChar) + 1);
                if (need.get(leftChar) > 0) matched--;
            }
            left++;
        }
    }
    return minLength == Integer.MAX_VALUE ? "" : s.substring(minStart, minStart + minLength);
}
```

### Example: Squares of Sorted Array (from `SquaresOfSortedArray.java`)

**Problem**: Given sorted array, return array of squares sorted.

**Naive approach** (existing code):

```java
public int[] sortedSquares(int[] nums) {
    for (int pos = 0; pos < nums.length; pos++) {
        nums[pos] *= nums[pos];
    }
    Arrays.sort(nums); // O(n log n)
    return nums;
}
```

**Optimal two-pointer approach** (O(n)):

```java
public int[] sortedSquares(int[] nums) {
    int n = nums.length;
    int[] result = new int[n];
    int left = 0;
    int right = n - 1;
    int write = n - 1; // Write from end (largest squares)

    while (left <= right) {
        int leftSquare = nums[left] * nums[left];
        int rightSquare = nums[right] * nums[right];

        if (leftSquare > rightSquare) {
            result[write--] = leftSquare;
            left++;
        } else {
            result[write--] = rightSquare;
            right--;
        }
    }

    return result;
}
```

**Why it works**: After squaring, the array is "valley-shaped" (large at ends, small in middle). Using opposite ends
with write-from-end gives sorted result in O(n).

---

## 4. Fast/Slow Pointers (Tortoise and Hare)

```java
// Classic cycle detection or middle-finding pattern
ListNode slow = head;
ListNode fast = head;

while(fast !=null&&fast.next !=null){
slow =slow.next;      // Move 1 step
fast =fast.next.next; // Move 2 steps

// Optional: check for collision (cycle detection)
    if(slow ==fast){
        // Cycle found!
        }
        }
// slow is now at middle (or just past it)
```

### Characteristics

| Property        | Value                                      |
|-----------------|--------------------------------------------|
| **Pointers**    | Two: slow (1 step), fast (2 steps)         |
| **Convergence** | Fast gains 1 step per iteration on slow    |
| **Cycle Proof** | If cycle exists, fast catches slow in O(n) |
| **Middle**      | When fast reaches end, slow is at middle   |
| **Time**        | O(n)                                       |
| **Space**       | O(1)                                       |

### When to Use

- **Linked list cycle detection**
- **Find middle of linked list**
- **Find start of cycle** (Floyd's algorithm)
- **Happy number detection**
- **Duplicate number in array** (treat array as implicit linked list)

### Mathematical Proof (Why It Works)

**For cycle detection**:

- If fast and slow are in a cycle of length C
- Fast gains 1 step per iteration relative to slow
- Maximum distance in cycle: C-1 steps
- Therefore: collision guaranteed in at most C iterations

**For finding middle**:

- Fast moves 2x faster than slow
- When fast reaches position 2n, slow is at position n
- When fast hits the end, slow is exactly at middle

### Example: Middle of Linked List (from `MiddleOfTheLinkedList.java`)

**Naive approach** (two passes):

```java
public ListNode middleNode(ListNode head) {
    List<ListNode> list = new ArrayList<>();
    ListNode current = head;
    while (current != null) {
        list.add(current);
        current = current.next;
    }
    return list.get(list.size() / 2);
}
```

**Optimal fast/slow approach** (one pass):

```java
public ListNode middleNodeImproved(ListNode head) {
    ListNode slow = head;
    ListNode fast = head;

    while (fast != null && fast.next != null) {
        slow = slow.next;
        fast = fast.next.next;
    }

    return slow; // slow is at middle
}
```

**Even vs odd**: Odd `1→2→3→4→5` → slow at 3; Even `1→2→3→4` → slow at 3 (second middle).

### Example: Cycle Detection (`LinkedListCycle.java`)

```java
public boolean hasCycle(ListNode head) {
    if (head == null || head.next == null) return false;
    ListNode slow = head, fast = head;
    while (fast != null && fast.next != null) {
        slow = slow.next;
        fast = fast.next.next;
        if (slow == fast) return true;
    }
    return false;
}
```

**Why `fast.next != null`?** Prevents NPE on `fast.next.next`.

### Example: Find Cycle Start (Floyd's Algorithm)

```java
public ListNode detectCycle(ListNode head) {
    if (head == null || head.next == null) return null;
    ListNode slow = head, fast = head;

    // Phase 1: Detect cycle
    while (fast != null && fast.next != null) {
        slow = slow.next;
        fast = fast.next.next;
        if (slow == fast) break;
    }
    if (fast == null || fast.next == null) return null;

    // Phase 2: Find cycle start
    slow = head;
    while (slow != fast) {
        slow = slow.next;
        fast = fast.next;
    }
    return slow;
}
```

**Proof**: Let `a`=head to cycle start, `b`=cycle start to collision, `c`=remaining cycle. Then
`a ≡ c (mod cycle_length)`, so meeting point is cycle start.

---

## 5. Three Pointers (Dutch National Flag)

```java
// Partition array into 3 regions (e.g., 0s, 1s, 2s)
int left = 0;      // Boundary for 0s
int current = 0;   // Current element being examined
int right = nums.length - 1;  // Boundary for 2s

while(current <=right){
        if(nums[current]==0){

swap(nums, left, current);

left++;
current++;
        }else if(nums[current]==1){
current++;
        }else{ // nums[current] == 2

swap(nums, current, right);

right--;
        // Don't increment current: need to examine swapped element
        }
        }
```

### Characteristics

| Property      | Value                                                                      |
|---------------|----------------------------------------------------------------------------|
| **Pointers**  | Three: left, current, right                                                |
| **Regions**   | [0, left): region 1; [left, right+1]: unprocessed; (right, end]: region 2  |
| **Invariant** | Elements before left are partitioned; elements after right are partitioned |
| **Time**      | O(n) — single pass                                                         |
| **Space**     | O(1) — in-place                                                            |

### When to Use

- **Sort colors** (0s, 1s, 2s) — classic Dutch National Flag
- **Partition into 3 groups** based on value
- **Quickselect partition step** (generalization to 2 groups)
- **Move all X to front, Y to middle, Z to end**

### Example: Sort Colors (Dutch National Flag)

```java
public void sortColors(int[] nums) {
    int left = 0, current = 0, right = nums.length - 1;
    while (current <= right) {
        if (nums[current] == 0) {
            swap(nums, left++, current++);
        } else if (nums[current] == 1) {
            current++;
        } else {
            swap(nums, current, right--);
            // DON'T increment: swapped element is unexamined
        }
    }
}

private void swap(int[] nums, int i, int j) {
    int temp = nums[i];
    nums[i] = nums[j];
    nums[j] = temp;
}
```

### Example: Two-Way Partition (from `SortArrayByParity.java`)

**Existing code** uses queue for tracking odd indices. **Optimal two-pointer approach**:

```java
public int[] sortArrayByParity(int[] nums) {
    int left = 0, right = nums.length - 1;
    while (left < right) {
        while (left < right && nums[left] % 2 == 0) left++;
        while (left < right && nums[right] % 2 != 0) right--;
        if (left < right) {
            int temp = nums[left];
            nums[left] = nums[right];
            nums[right] = temp;
            left++;
            right--;
        }
    }
    return nums;
}
```

**Same-direction alternative** (like `MoveZeroes.java`):

```java
public int[] sortArrayByParity(int[] nums) {
    int write = 0;
    for (int read = 0; read < nums.length; read++) {
        if (nums[read] % 2 == 0) {
            int temp = nums[read];
            nums[read] = nums[write];
            nums[write++] = temp;
        }
    }
    return nums;
}
```

### Example: Move Zeroes (from `MoveZeroes.java`)

**Existing code** uses queue. **Optimal two-pointer**:

```java
public void moveZeroes(int[] nums) {
    int write = 0;
    for (int read = 0; read < nums.length; read++) {
        if (nums[read] != 0) {
            nums[write++] = nums[read];
        }
    }
    while (write < nums.length) {
        nums[write++] = 0;
    }
}
```

            }
            lastNonZero++;
        }
    }

}

```

---

## 6. Same Direction (Reader/Writer)

```java
// Process array in-place: read and write at different speeds
int write = 0;

for (int read = 0; read < array.length; read++) {
    if (shouldKeep(array[read])) {
        array[write++] = array[read];
    }
}
// write is now the new logical length
```

### Characteristics

| Property      | Value                                         |
|---------------|-----------------------------------------------|
| **Pointers**  | Two: read (iterates), write (tracks position) |
| **Invariant** | `array[0..write)` contains kept elements      |
| **Time**      | O(n) — single pass                            |
| **Space**     | O(1) — in-place                               |

### When to Use

- **Remove duplicates** (from sorted array)
- **Filter array** (keep elements matching condition)
- **Compress array** (run-length encoding)
- **Move elements to front** (move zeroes, move evens)

### Example: Remove Duplicates from Sorted Array (from `RemoveDuplicatesFromSortedArray.java`)

**Existing code** (uses HashSet, O(n) space, inefficient):

```java
public int removeDuplicates(int[] nums) {
    Set<Integer> set = new HashSet<>();
    int length = nums.length;
    for (int pos = nums.length - 1; pos >= 0; pos--) {
        if (set.contains(nums[pos])) {
            for (int current = pos; current < length - 1; current++) {
                nums[current] = nums[current + 1];
            }
            length--;
            nums[length] = 0;
        }
        set.add(nums[pos]);
    }
    return length;
}
```

**Optimal same-direction approach** (O(1) space):

```java
public int removeDuplicates(int[] nums) {
    if (nums.length == 0) {
        return 0;
    }

    int write = 1; // First element is always unique

    for (int read = 1; read < nums.length; read++) {
        if (nums[read] != nums[read - 1]) {
            nums[write++] = nums[read];
        }
    }

    return write;
}
```

**Why it works**: Array is sorted, so duplicates are adjacent. Compare each element with previous; if different, it's a
new unique element.

**Example trace**: `[1, 1, 2, 2, 3]`

- Initially: write=1, array unchanged
- read=1: nums[1]==nums[0], skip
- read=2: nums[2]!=nums[1], write nums[2] to nums[1], write=2 → `[1, 2, 2, 2, 3]`
- read=3: nums[3]==nums[2], skip
- read=4: nums[4]!=nums[3], write nums[4] to nums[2], write=3 → `[1, 2, 3, 2, 3]`
- Result: length=3, first 3 elements are `[1, 2, 3]`

### Example: Remove Element In-Place (Alternative)

From `RemoveElement.java` — remove all instances of `val`:

```java
public int removeElement(int[] nums, int val) {
    int write = 0;

    for (int read = 0; read < nums.length; read++) {
        if (nums[read] != val) {
            nums[write++] = nums[read];
        }
    }

    return write;
}
```

**Trace**: `nums = [3, 2, 2, 3]`, val=3

- read=0: nums[0]==3, skip
- read=1: nums[1]!=3, write to nums[0], write=1 → `[3, 2, 2, 3]`
- read=2: nums[2]!=3, write to nums[1], write=2 → `[2, 2, 2, 3]`
- read=3: nums[3]==3, skip
- Result: length=2, array starts with `[2, 2, ...]`

### Example: Run-Length Encoding Compression

```java
public int compress(char[] chars) {
    int write = 0;
    int read = 0;

    while (read < chars.length) {
        char current = chars[read];
        int count = 0;

        // Count consecutive identical characters
        while (read < chars.length && chars[read] == current) {
            read++;
            count++;
        }

        // Write character
        chars[write++] = current;

        // Write count digits if count > 1
        if (count > 1) {
            String countStr = String.valueOf(count);
            for (char digit : countStr.toCharArray()) {
                chars[write++] = digit;
            }
        }
    }

    return write;
}
```

---

## Complexity Analysis

### Time Complexity Comparison

| Pattern                 | Optimal Time | Naive Time | Improvement             |
|-------------------------|--------------|------------|-------------------------|
| Opposite Ends           | O(n)         | O(n²)      | Nested loop elimination |
| Sliding Window Fixed    | O(n)         | O(n·k)     | Recompute → incremental |
| Sliding Window Variable | O(n)         | O(n²)      | Subarray enumeration    |
| Fast/Slow Pointers      | O(n)         | O(n)       | Space: O(1) vs O(n)     |
| Three Pointers          | O(n)         | O(n log n) | Sort avoided            |
| Same Direction          | O(n)         | O(n²)      | Shift elimination       |

### Space Complexity Comparison

| Pattern            | Optimal Space | Naive Space | Savings                |
|--------------------|---------------|-------------|------------------------|
| Opposite Ends      | O(1)          | O(n)        | No extra array         |
| Sliding Window     | O(1)          | O(n)        | No subarray creation   |
| Fast/Slow Pointers | O(1)          | O(n)        | No HashSet for cycle   |
| Three Pointers     | O(1)          | O(n)        | No bucket arrays       |
| Same Direction     | O(1)          | O(n)        | No filter result array |

### Why Two Pointers Beats Alternatives

**Two-Sum Example**:

- Brute force: O(n²) — check all pairs
- HashMap: O(n) time, O(n) space
- Two pointers (sorted): O(n) time, O(1) space
- **Trade-off**: Two pointers requires sorted array (O(n log n) sort) unless already sorted

**Subarray Sum Example**:

- Brute force: O(n²) or O(n³) — enumerate all subarrays
- Prefix sum + HashMap: O(n) time, O(n) space
- Sliding window: O(n) time, O(1) space (for sum ≤ k problems)

**Cycle Detection Example**:

- HashSet: O(n) time, O(n) space — store visited nodes
- Fast/slow: O(n) time, O(1) space — constant extra memory

---

## Magic Patterns and Templates

### Pattern 1: Converging Pointers Template

```java
public int[] twoPointerConverge(int[] sorted, int target) {
    int left = 0;
    int right = sorted.length - 1;

    while (left < right) {
        int result = compute(sorted[left], sorted[right]);

        if (result == target) {
            return new int[]{left, right};
        } else if (result < target) {
            left++;  // Need larger result
        } else {
            right--; // Need smaller result
        }
    }

    return new int[]{-1, -1};
}
```

### Pattern 2: Sliding Window Template

```java
public int slidingWindow(int[] nums) {
    int left = 0;
    int result = 0;
    int windowState = 0; // sum, count, etc.

    for (int right = 0; right < nums.length; right++) {
        // Add nums[right] to window
        windowState += nums[right];

        // Contract while invalid
        while (!isValid(windowState)) {
            windowState -= nums[left];
            left++;
        }

        // Update answer
        result = Math.max(result, right - left + 1);
    }

    return result;
}
```

### Pattern 3: Fast/Slow Template

```java
public boolean hasCycleOrFindMiddle(Node head) {
    Node slow = head;
    Node fast = head;

    while (fast != null && fast.next != null) {
        slow = slow.next;
        fast = fast.next.next;

        // Cycle detection
        if (slow == fast) {
            return true; // or phase 2: find cycle start
        }
    }

    return slow; // middle node
}
```

### Pattern 4: Dutch Flag Template

```java
public void threeWayPartition(int[] nums, int pivot1, int pivot2) {
    int left = 0, current = 0, right = nums.length - 1;
    while (current <= right) {
        if (nums[current] < pivot1) swap(nums, left++, current++);
        else if (nums[current] > pivot2) swap(nums, current, right--);
        else current++;
    }
}
```

### Pattern 5: Reader/Writer Template

```java
public int inPlaceFilter(int[] nums) {
    int write = 0;
    for (int read = 0; read < nums.length; read++) {
        if (shouldKeep(nums[read])) nums[write++] = nums[read];
    }
    return write;
}
```

---

## Common Gotchas

### 1. Pointer Update Order

**Palindrome check**: Use `left < right` not `left <= right` to avoid double-checking middle element.

### 2. Fast/Slow Termination

**NPE prevention**: Check `fast != null && fast.next != null` (not just `fast.next != null`).

### 3. Sliding Window: Update Timing

**Longest valid window**: Update AFTER contracting (window is valid).
**Shortest valid window**: Update DURING contracting (while window is still valid).

### 4. Sorted Requirement Forgotten

Two-pointer opposite ends **assumes sorted array** for two-sum. For unsorted arrays, use HashMap instead.

### 5. Dutch Flag: Don't Skip Swapped Element

**Wrong** (misses checking the swapped element):

```java
while(current <=right){
        if(nums[current]==0){

swap(nums, left, current);

left++;
current++; // BUG: swapped element not examined
        }else if(nums[current]==2){

swap(nums, current, right);

right--;
current++; // BUG: swapped element not examined
        }
        }
```

**Correct**:

```java
while(current <=right){
        if(nums[current]==0){

swap(nums, left, current);

left++;
current++; // Safe: we know what came from left (it was 1 or already examined)
        }else if(nums[current]==2){

swap(nums, current, right);

right--;
        // DON'T increment current: element from right is unexamined
        }else{
current++;
        }
        }
```

### 6. Opposite Ends: Array Length Edge Cases

```java
// Always check array length first
if(nums ==null||nums.length ==0){
        return /* appropriate default */;
        }

// For two-element arrays
int left = 0;
int right = nums.length - 1; // right = 1
while(left<right){
        // Loop runs for [0, 1) — exactly once, which is correct
        }
```

### 7. Sliding Window: Empty/No Valid Window

```java
int result = 0; // or Integer.MAX_VALUE for "shortest" problems

for(
int right = 0;
right<nums.length;right++){
// ... process
result =Math.

max(result, right -left+1);
}

        return result;

// For shortest window problems:
if(result ==Integer.MAX_VALUE){
        return 0; // or -1, or "" depending on return type
        }
```

### 8. Same Direction: Fill Remaining

For `MoveZeroes`-style problems, fill remaining positions after filtering:

```java
int write = 0;
for(
int read = 0;
read<nums.length;read++){
        if(nums[read]!=0){
nums[write++]=nums[read];
        }
        }
        while(write<nums.length){
nums[write++]=0; // Fill garbage with zeros
        }
```

---

## See Also

### Related Pattern Guides

- **[MAP_GUIDE.md](MAP_GUIDE.md)**: HashMap patterns for frequency counting (alternative to sliding window for substring
  problems)
- **[HEAP_GUIDE.md](HEAP_GUIDE.md)**: Top-K patterns — use heaps as alternative to sliding window for k-sized subarray
  problems with ordering
- **Binary Search Guide**: Sorted array problems (often complements two-pointer on sorted data)
- **Greedy Guide**: When two-pointer greedy choices work

### Challenge References by Pattern

**Opposite Ends**:

- `development/leetcode/funwitharrays/MergeSortedArray.java` — merge sorted arrays
- `development/leetcode/funwitharrays/RemoveElement.java` — remove in-place
- `development/leetcode/funwitharrays/SquaresOfSortedArray.java` — two-pointer square sort

**Sliding Window**:

- `development/leetcode/funwitharrays/MaxConsecutiveOnes.java` — consecutive ones counting
- `development/leetcode/RansomNote.java` — character frequency (sliding window variant)

**Same Direction (In-Place)**:

- `development/leetcode/funwitharrays/MoveZeroes.java` — move zeroes
- `development/leetcode/funwitharrays/SortArrayByParity.java` — parity partition
- `development/leetcode/funwitharrays/RemoveDuplicatesFromSortedArray.java` — remove duplicates

**Fast/Slow Pointers**:

- `development/leetcode/MiddleOfTheLinkedList.java` — find middle
- `development/leetcode/linkedlist/LinkedListCycle.java` — cycle detection
- `development/leetcode/linkedlist/LinkedListCycleII.java` — find cycle start

**Additional Fun With Arrays Challenges**:

- `development/leetcode/funwitharrays/DuplicateZeros.java` — duplicate zeros in-place
- `development/leetcode/funwitharrays/ValidMountainArray.java` — mountain validation
- `development/leetcode/funwitharrays/FindAllNumbersDisappearedInAnArray.java` — index marking
- `development/leetcode/funwitharrays/CheckIfNAndItsDoubleExist.java` — two-sum variant
- `development/leetcode/funwitharrays/ThirdMaximumNumber.java` — top-k selection
- `development/leetcode/funwitharrays/ABetterRepeatedDeletionAlgorithm.java` — stack simulation

**Additional Linked List Challenges**:

- `development/leetcode/linkedlist/ReverseLinkedList.java` — iterative reversal
- `development/leetcode/linkedlist/MergeTwoSortedLists.java` — sorted merge
- `development/leetcode/linkedlist/RemoveNthNodeFromEndOfList.java` — two-pass vs one-pass
- `development/leetcode/linkedlist/PalindromeLinkedList.java` — fast/slow + reversal
- `development/leetcode/linkedlist/IntersectionOfTwoLinkedLists.java` — pointer trick

### Alternative Approaches

| Problem Type           | Two Pointers         | Alternative               | Trade-off                     |
|------------------------|----------------------|---------------------------|-------------------------------|
| Two-sum                | O(n), sorted needed  | HashMap: O(n), O(n) space | Two-pointer saves space       |
| Subarray sum           | O(n), sliding window | Prefix sum + HashMap      | HashMap handles negatives     |
| Longest substring      | O(n), sliding window | HashMap: O(n), O(k) space | Same time, HashMap O(k) space |
| Cycle detection        | O(n), O(1) space     | HashSet: O(n), O(n) space | Fast/slow is space-optimal    |
| Top-k elements         | Quickselect: O(n)    | Heap: O(n log k)          | Quickselect avg case faster   |
| K-th largest in stream | Min-heap: O(log k)   | Quickselect per query     | Heap is better for streaming  |

---

## Summary

Two Pointers and Sliding Window are foundational patterns that appear in 30%+ of coding interview problems. Master these
core principles:

1. **Opposite Ends**: For sorted arrays, two-sum variants, palindrome checks, merging
2. **Sliding Window Fixed**: Maximum/minimum of k consecutive elements
3. **Sliding Window Variable**: Longest/shortest subarray with constraint
4. **Fast/Slow**: Cycle detection, middle finding, implicit linked lists
5. **Three Pointers**: Partition into 3 groups, Dutch National Flag
6. **Same Direction**: In-place filtering, removing, compressing

**Common thread**: All achieve O(n) time with O(1) space by clever pointer manipulation instead of nested loops or extra
data structures.

When in doubt on a sorted array problem, ask: *"Can I use two pointers from opposite ends?"*
When in doubt on a subarray problem, ask: *"Can I use a sliding window?"*
