# Java DSA Interview Preparation Guide

Target companies: Revolut, Deel, RevenueCat, GitHub, Docker, Datadog

---

## Table of Contents

1. [Java Collections Decision Tree](#java-collections-decision-tree)
2. [Time Complexity Reference](#time-complexity-reference)
3. [Essential Java Features for DSA](#essential-java-features-for-dsa)
4. [Third-Party Library Policy](#third-party-library-policy)
5. [Interview Patterns with Java](#interview-patterns-with-java)
6. [Challenge Reference by Category](#challenge-reference-by-category)

---

## Java Collections Decision Tree

```
Need to store elements?
├── Yes, with duplicates allowed?
│   ├── Yes, need index access?
│   │   └── ArrayList (O(1) get, O(n) add/remove middle)
│   ├── Yes, frequent add/remove at ends?
│   │   └── ArrayDeque (O(1) both ends)
│   ├── Yes, frequent add/remove anywhere?
│   │   └── LinkedList (O(1) add/remove, O(n) access)
│   └── No, just need iteration?
│       └── LinkedList or ArrayList
├── No, unique elements only?
│   ├── Need sorted order?
│   │   ├── TreeSet (O(log n) all ops)
│   │   └── LinkedHashSet (insertion order, O(1) ops)
│   └── Order does not matter?
│       └── HashSet (O(1) ops)
├── Need key-value pairs?
│   ├── Need sorted by key?
│   │   └── TreeMap (O(log n) ops)
│   ├── Need insertion order?
│   │   └── LinkedHashMap (O(1) ops, predictable iteration)
│   └── Order does not matter?
│       └── HashMap (O(1) ops)
└── Need queue behavior?
    ├── FIFO queue?
    │   └── ArrayDeque (preferred) or LinkedList
    ├── Priority-based?
    │   └── PriorityQueue (heap, O(log n) insert/extract)
    ├── LIFO stack?
    │   └── ArrayDeque (use push/pop)
    └── Double-ended?
        └── ArrayDeque (O(1) both ends)
```

---

## Time Complexity Reference

### ArrayList

| Operation      | Time           | Notes               |
|----------------|----------------|---------------------|
| get(index)     | O(1)           | Direct array access |
| add(end)       | O(1) amortized | May need to resize  |
| add(middle)    | O(n)           | Must shift elements |
| remove(end)    | O(1)           |                     |
| remove(middle) | O(n)           | Must shift elements |
| contains       | O(n)           | Linear search       |
| indexOf        | O(n)           |                     |

### HashMap / HashSet

| Operation   | Time         | Notes                       |
|-------------|--------------|-----------------------------|
| put / get   | O(1) average | O(n) worst case (collision) |
| containsKey | O(1) average |                             |
| remove      | O(1) average |                             |
| iteration   | O(n)         |                             |

### TreeMap / TreeSet

| Operation          | Time     | Notes                    |
|--------------------|----------|--------------------------|
| put / get          | O(log n) | Red-black tree           |
| containsKey        | O(log n) |                          |
| firstKey / lastKey | O(log n) |                          |
| ceiling / floor    | O(log n) | Useful for range queries |

### PriorityQueue

| Operation     | Time     | Notes           |
|---------------|----------|-----------------|
| offer (add)   | O(log n) | Heap insertion  |
| poll (remove) | O(log n) | Extract min/max |
| peek          | O(1)     | View min/max    |
| contains      | O(n)     | Linear search   |

### ArrayDeque

| Operation                | Time | Notes |
|--------------------------|------|-------|
| addFirst / addLast       | O(1) |       |
| removeFirst / removeLast | O(1) |       |
| peekFirst / peekLast     | O(1) |       |

---

## Essential Java Features for DSA

### Collections Framework

**When to use each interface:**

```java
// List - ordered, allows duplicates
List<Integer> list = new ArrayList<>();  // Default choice
List<Integer> linked = new LinkedList<>();  // Frequent middle insertions

// Set - unique elements
Set<String> set = new HashSet<>();  // Fast lookup, no order
Set<String> ordered = new LinkedHashSet<>();  // Insertion order
Set<String> sorted = new TreeSet<>();  // Natural ordering

// Map - key-value pairs
Map<String, Integer> map = new HashMap<>();  // Default choice
Map<String, Integer> orderedMap = new LinkedHashMap<>();  // LRU cache
Map<String, Integer> sortedMap = new TreeMap<>();  // Sorted by key

// Queue - FIFO
Queue<Integer> queue = new ArrayDeque<>();  // Preferred over LinkedList
Queue<Integer> pq = new PriorityQueue<>();  // Min-heap by default
Queue<Integer> maxPq = new PriorityQueue<>(Collections.reverseOrder());  // Max-heap

// Deque - double-ended
Deque<Integer> deque = new ArrayDeque<>();
deque.

addFirst(1);  // O(1)
deque.

addLast(2);   // O(1)
deque.

removeFirst(); // O(1)
deque.

removeLast();  // O(1)
```

**Example from codebase - RansomNote (HashMap frequency counting):**

```java
public boolean canConstruct(String ransomNote, String magazine) {
    final Map<Character, Integer> map = new HashMap<>();
    for (int pos = 0; pos < magazine.length(); pos++) {
        final char character = magazine.charAt(pos);
        final int counter = map.getOrDefault(character, 0);
        map.put(character, counter + 1);
    }
    // Check ransom note against frequencies...
}
```

### Generics and Bounded Wildcards

```java
// Generic method for any comparable type
public static <T extends Comparable<T>> T findMax(List<T> list) {
    T max = list.get(0);
    for (T item : list) {
        if (item.compareTo(max) > 0) {
            max = item;
        }
    }
    return max;
}

// Bounded wildcard - accept any Number subtype
public static double sum(List<? extends Number> numbers) {
    double total = 0;
    for (Number n : numbers) {
        total += n.doubleValue();
    }
    return total;
}

// Lower bounded - accept supertype of Integer
public static void addIntegers(List<? super Integer> list) {
    list.add(1);
    list.add(2);
}
```

### Streams API: When to Use vs Traditional Loops

**Use Streams for:**

- Readability in data transformation pipelines
- Filtering and mapping operations
- Parallel processing (when appropriate)

**Use Traditional Loops for:**

- Performance-critical code (streams have overhead)
- Early termination with complex conditions
- Modifying data structures during iteration

```java
// Good use of Streams - readable transformation
List<String> result = numbers.stream()
                .filter(n -> n > 0)
                .map(String::valueOf)
                .collect(Collectors.toList());

// Better with loop - early termination, better performance
public static int[] runningSum(int[] nums) {
    for (int pos = 1; pos < nums.length; pos++) {
        nums[pos] = nums[pos] + nums[pos - 1];  // In-place, O(1) space
    }
    return nums;
}

// Avoid streams for simple accumulation - overhead not worth it
// SumOf1DArray.java - traditional loop is optimal
```

### Optional: Avoiding NullPointerException

```java
// Instead of null checks
public String findUserName(Map<Integer, String> users, int id) {
    return Optional.ofNullable(users.get(id))
            .orElse("Unknown");
}

// Chain operations safely
public int getStringLength(Map<String, String> map, String key) {
    return Optional.ofNullable(map.get(key))
            .map(String::length)
            .orElse(0);
}

// Throw custom exception if not found
public String getRequiredValue(Map<String, String> map, String key) {
    return Optional.ofNullable(map.get(key))
            .orElseThrow(() -> new IllegalArgumentException("Key not found: " + key));
}
```

### Records (Java 14+)

Perfect for immutable data carriers in graph/tree problems:

```java
// Immutable graph node
public record GraphNode(String id, List<GraphNode> neighbors) {
}

// Tree node with value
public record TreeNode(int val, TreeNode left, TreeNode right) {
}

// Edge in weighted graph
public record Edge(String from, String to, int weight) {
}

// Usage - automatic equals, hashCode, toString
GraphNode node = new GraphNode("A", List.of(nodeB, nodeC));
```

### StringBuilder vs StringBuffer

```java
// StringBuilder - use this (not synchronized, faster)
StringBuilder sb = new StringBuilder();
for(
int i = 0;
i<n;i++){
        sb.

append("Fizz");
}
String result = sb.toString();

// StringBuffer - only if thread safety required (rare in DSA)
StringBuffer buffer = new StringBuffer();  // Synchronized, slower

// Example from FizzBuzz.java
public List<String> fizzBuzz(int n) {
    final List<String> list = new ArrayList();
    for (int pos = 1; pos < (n + 1); pos++) {
        final StringBuffer sb = new StringBuffer();  // Single-threaded context
        if (pos % 3 == 0) sb.append("Fizz");
        if (pos % 5 == 0) sb.append("Buzz");
        list.add((sb.length() == 0) ? String.valueOf(pos) : sb.toString());
    }
    return list;
}
```

### Arrays Utility Class

```java
// Sorting
int[] arr = {3, 1, 4, 1, 5};
Arrays.

sort(arr);  // Dual-pivot quicksort, O(n log n)
Arrays.

sort(arr, 0,3);  // Sort subrange

// Binary search - array MUST be sorted first
int index = Arrays.binarySearch(arr, 4);  // Returns index or (-insertionPoint - 1)

// Filling
Arrays.

fill(arr, 0);  // Fill with value
Arrays.

fill(arr, 1,3,-1);  // Fill subrange

// Copying
int[] copy = Arrays.copyOf(arr, arr.length);
int[] bigger = Arrays.copyOf(arr, arr.length * 2);  // Pads with zeros
int[] range = Arrays.copyOfRange(arr, 1, 4);

// Comparison
boolean equal = Arrays.equals(arr1, arr2);

// Conversion
String str = Arrays.toString(arr);
List<Integer> list = Arrays.asList(1, 2, 3);  // Fixed-size list backed by array

// Example from ZigZagSequence.java
public static void findZigZagSequence(int[] a, int n) {
    Arrays.sort(a);  // First step in algorithm
    // ... rest of implementation
}
```

### Math Class and BigInteger/BigDecimal

```java
// Basic math
int max = Math.max(a, b);
int min = Math.min(a, b);
int abs = Math.abs(-5);  // 5
double sqrt = Math.sqrt(16);  // 4.0
double pow = Math.pow(2, 10);  // 1024.0

// BigInteger - arbitrary precision integers
BigInteger fact = BigInteger.ONE;
for(
int i = 2;
i <=100;i++){
fact =fact.

multiply(BigInteger.valueOf(i));
        }

// BigDecimal - arbitrary precision decimals
// Example from PlusMinus.java
public static BigDecimal[] calculation(final List<Integer> arr) {
    final BigDecimal total = new BigDecimal(arr.size());
    BigDecimal positive = new BigDecimal(0);
    // ... accumulate counts
    return new BigDecimal[]{
            positive.divide(total, 6, RoundingMode.DOWN),  // 6 decimal places
            negative.divide(total, 6, RoundingMode.DOWN),
            zero.divide(total, 6, RoundingMode.DOWN)
    };
}
```

### Comparator and Comparable

```java
// Implement Comparable for natural ordering
public class Person implements Comparable<Person> {
    private final String name;
    private final int age;

    @Override
    public int compareTo(Person other) {
        return Integer.compare(this.age, other.age);  // Ascending by age
    }
}

// Custom Comparator
Comparator<Person> byName = Comparator.comparing(Person::getName);
Comparator<Person> byAgeDesc = Comparator.comparingInt(Person::getAge).reversed();
Comparator<Person> byNameThenAge = Comparator
        .comparing(Person::getName)
        .thenComparingInt(Person::getAge);

// PriorityQueue with custom comparator
PriorityQueue<int[]> pq = new PriorityQueue<>(
        Comparator.comparingInt(a -> a[0])  // Min-heap by first element
);

// Sorting with comparator
List<Person> people = new ArrayList<>();
people.

sort(Comparator.comparingInt(Person::getAge).

reversed());
```

### Var Keyword (Java 10+)

```java
// Good use - obvious type from context
var list = new ArrayList<String>();  // Clear it's ArrayList<String>
var map = new HashMap<String, Integer>();  // Clear types

// Bad use - obscures type
var result = someMethod();  // What type is result?

// In DSA challenges - prefer explicit types for clarity
Map<String, Integer> frequencyMap = new HashMap<>();  // Better
// var freqMap = new HashMap<>();  // Raw type, avoid
```

---

## Third-Party Library Policy

### In Coding Challenges: NO Third-Party Libraries

**Why?**

- Interviewers test knowledge of JDK itself, not library ecosystems
- Most platforms (LeetCode, HackerRank) only allow standard library
- Shows you understand underlying data structures and algorithms

### Native JDK Alternatives

| Common Need           | Third-Party         | JDK Alternative                                                |
|-----------------------|---------------------|----------------------------------------------------------------|
| JSON parsing          | Jackson, Gson       | Manual string manipulation or skip parsing                     |
| Graph structure       | JGraphT             | `Map<String, List<String>>` adjacency list                     |
| Priority Queue        | -                   | `java.util.PriorityQueue` with custom `Comparator`             |
| LRU Cache             | Caffeine, Guava     | `java.util.LinkedHashMap` or implement from scratch            |
| Immutable collections | Guava               | `java.util.Collections.unmodifiableXxx` or Java 9+ `List.of()` |
| String utilities      | Apache Commons      | Write simple helper methods                                    |
| Math operations       | Apache Commons Math | `java.math.BigInteger`, `java.math.BigDecimal`                 |

### Example: Building Your Own Graph

```java
// Adjacency list graph - sufficient for 99% of interview problems
public class Graph {
    private final Map<String, List<String>> adjacencyList = new HashMap<>();

    public void addEdge(String from, String to) {
        adjacencyList.computeIfAbsent(from, k -> new ArrayList<>()).add(to);
    }

    public List<String> getNeighbors(String node) {
        return adjacencyList.getOrDefault(node, Collections.emptyList());
    }
}
```

### Example: LRU Cache with LinkedHashMap

```java
// If implementing from scratch is not the point of the question
class LRUCache extends LinkedHashMap<Integer, Integer> {
    private final int capacity;

    public LRUCache(int capacity) {
        super(capacity, 0.75f, true);  // accessOrder = true for LRU
        this.capacity = capacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<Integer, Integer> eldest) {
        return size() > capacity;
    }
}
```

### Exception: Production Code

In real work, use established libraries:

- Jackson/Gson for JSON
- Guava for utilities
- Apache Commons for collections

**But know the JDK equivalent** - interviewers often ask "how would you do this without library X?"

---

## Interview Patterns with Java

### Two Pointers

```java
// Sorted array two sum
public int[] twoSum(int[] nums, int target) {
    int left = 0, right = nums.length - 1;
    while (left < right) {
        int sum = nums[left] + nums[right];
        if (sum == target) {
            return new int[]{left, right};
        } else if (sum < target) {
            left++;
        } else {
            right--;
        }
    }
    return new int[]{-1, -1};
}

// Fast and slow pointers - MiddleOfTheLinkedList.java pattern
public ListNode middleNode(ListNode head) {
    ListNode slow = head, fast = head;
    while (fast != null && fast.next != null) {
        slow = slow.next;        // Move 1 step
        fast = fast.next.next;   // Move 2 steps
    }
    return slow;  // Middle when fast reaches end
}
```

### Sliding Window

```java
// Maximum sum subarray of size k
public int maxSum(int[] arr, int k) {
    int maxSum = 0, windowSum = 0;

    for (int i = 0; i < k; i++) {
        windowSum += arr[i];
    }
    maxSum = windowSum;

    for (int i = k; i < arr.length; i++) {
        windowSum = windowSum - arr[i - k] + arr[i];  // Slide window
        maxSum = Math.max(maxSum, windowSum);
    }
    return maxSum;
}
```

### BFS/DFS on Trees and Graphs

```java
// BFS with ArrayDeque (preferred over LinkedList for queues)
public List<Integer> bfs(TreeNode root) {
    List<Integer> result = new ArrayList<>();
    if (root == null) return result;

    Deque<TreeNode> queue = new ArrayDeque<>();
    queue.offer(root);

    while (!queue.isEmpty()) {
        TreeNode node = queue.poll();
        result.add(node.val);
        if (node.left != null) queue.offer(node.left);
        if (node.right != null) queue.offer(node.right);
    }
    return result;
}

// DFS - iterative with stack
public List<Integer> dfsIterative(TreeNode root) {
    List<Integer> result = new ArrayList<>();
    if (root == null) return result;

    Deque<TreeNode> stack = new ArrayDeque<>();
    stack.push(root);

    while (!stack.isEmpty()) {
        TreeNode node = stack.pop();
        result.add(node.val);
        if (node.right != null) stack.push(node.right);
        if (node.left != null) stack.push(node.left);
    }
    return result;
}

// DFS - recursive
public void dfsRecursive(TreeNode node, List<Integer> result) {
    if (node == null) return;
    result.add(node.val);
    dfsRecursive(node.left, result);
    dfsRecursive(node.right, result);
}
```

### Dynamic Programming Patterns

**Top-Down with Memoization (from Fibonacci.java):**

```java
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

**Bottom-Up Tabulation:**

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

// Space optimized - only need last two values
public long fibonacciOptimized(int n) {
    if (n <= 0) return 0;
    if (n <= 2) return 1;

    long prev2 = 1, prev1 = 1;
    for (int i = 3; i <= n; i++) {
        long current = prev1 + prev2;
        prev2 = prev1;
        prev1 = current;
    }
    return prev1;
}
```

**CanSum pattern (subset sum with memoization):**

```java
public static boolean memo(int target, int[] values, Map<Integer, Boolean> memo) {
    if (target == 0) return true;
    if (target < 0) return false;
    if (memo.containsKey(target)) return memo.get(target);

    for (final int candidate : values) {
        final boolean result = (candidate != 0 && memo((target - candidate), values, memo));
        memo.put(target, result);
        if (result) return true;
    }
    return false;
}
```

### Union-Find (Disjoint Set Union)

```java
public class UnionFind {
    private final int[] parent;
    private final int[] rank;

    public UnionFind(int n) {
        parent = new int[n];
        rank = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i;
        }
    }

    public int find(int x) {
        if (parent[x] != x) {
            parent[x] = find(parent[x]);  // Path compression
        }
        return parent[x];
    }

    public boolean union(int x, int y) {
        int px = find(x), py = find(y);
        if (px == py) return false;

        // Union by rank
        if (rank[px] < rank[py]) {
            parent[px] = py;
        } else if (rank[px] > rank[py]) {
            parent[py] = px;
        } else {
            parent[py] = px;
            rank[px]++;
        }
        return true;
    }
}
```

### Binary Search

```java
// Standard binary search
public int binarySearch(int[] arr, int target) {
    int left = 0, right = arr.length - 1;
    while (left <= right) {
        int mid = left + (right - left) / 2;  // Avoid overflow
        if (arr[mid] == target) return mid;
        if (arr[mid] < target) left = mid + 1;
        else right = mid - 1;
    }
    return -1;
}

// Binary search on answer space (find minimum k where condition holds)
public int findMinValid(int[] arr) {
    int left = 1, right = Arrays.stream(arr).max().getAsInt();
    int result = right;

    while (left <= right) {
        int mid = left + (right - left) / 2;
        if (isValid(arr, mid)) {
            result = mid;
            right = mid - 1;  // Try to find smaller valid value
        } else {
            left = mid + 1;
        }
    }
    return result;
}
```

### Topological Sort (Kahn's Algorithm)

```java
public List<Integer> topologicalSort(int numCourses, int[][] prerequisites) {
    Map<Integer, List<Integer>> graph = new HashMap<>();
    int[] inDegree = new int[numCourses];

    // Build graph
    for (int[] pre : prerequisites) {
        graph.computeIfAbsent(pre[1], k -> new ArrayList<>()).add(pre[0]);
        inDegree[pre[0]]++;
    }

    // Start with nodes having no prerequisites
    Queue<Integer> queue = new ArrayDeque<>();
    for (int i = 0; i < numCourses; i++) {
        if (inDegree[i] == 0) queue.offer(i);
    }

    List<Integer> result = new ArrayList<>();
    while (!queue.isEmpty()) {
        int course = queue.poll();
        result.add(course);

        for (int next : graph.getOrDefault(course, Collections.emptyList())) {
            if (--inDegree[next] == 0) {
                queue.offer(next);
            }
        }
    }

    return result.size() == numCourses ? result : Collections.emptyList();
}
```

### Heap/Priority Queue Patterns

```java
// Top K frequent elements
public int[] topKFrequent(int[] nums, int k) {
    Map<Integer, Integer> freq = new HashMap<>();
    for (int num : nums) {
        freq.merge(num, 1, Integer::sum);
    }

    // Min-heap of size k
    PriorityQueue<Map.Entry<Integer, Integer>> pq = new PriorityQueue<>(
            Comparator.comparingInt(Map.Entry::getValue)
    );

    for (Map.Entry<Integer, Integer> entry : freq.entrySet()) {
        pq.offer(entry);
        if (pq.size() > k) pq.poll();  // Remove least frequent
    }

    return pq.stream().mapToInt(Map.Entry::getKey).toArray();
}

// Merge K sorted lists
public ListNode mergeKLists(ListNode[] lists) {
    PriorityQueue<ListNode> pq = new PriorityQueue<>(Comparator.comparingInt(n -> n.val));

    for (ListNode list : lists) {
        if (list != null) pq.offer(list);
    }

    ListNode dummy = new ListNode(0);
    ListNode current = dummy;

    while (!pq.isEmpty()) {
        ListNode node = pq.poll();
        current.next = node;
        current = current.next;
        if (node.next != null) pq.offer(node.next);
    }

    return dummy.next;
}
```

---

## Challenge Reference by Category

### Arrays

| Challenge             | Location                                     | Key Concepts                           |
|-----------------------|----------------------------------------------|----------------------------------------|
| SumOf1DArray          | `leetcode/SumOf1DArray.java`                 | In-place prefix sum, O(1) space        |
| RansomNote            | `leetcode/RansomNote.java`                   | HashMap frequency counting             |
| RichestCustomerWealth | `leetcode/RichestCustomerWealth.java`        | 2D array traversal                     |
| ZigZagSequence        | `onepreparationweek/ZigZagSequence.java`     | Array manipulation, sorting            |
| PlusMinus             | `onepreparationweek/PlusMinus.java`          | BigDecimal precision, 6 decimal places |
| DiagonalDifference    | `onepreparationweek/DiagonalDifference.java` | Matrix diagonal traversal              |
| LonelyInteger         | `onepreparationweek/LonelyInteger.java`      | XOR trick (a ^ a = 0)                  |
| MiniMaxSum            | `onepreparationweek/MiniMaxSum.java`         | Sum manipulation                       |
| CountingSortOne       | `onepreparationweek/CountingSortOne.java`    | Counting sort variant                  |

### Linked Lists

| Challenge             | Location                                              | Key Concepts              |
|-----------------------|-------------------------------------------------------|---------------------------|
| MiddleOfTheLinkedList | `leetcode/MiddleOfTheLinkedList.java`                 | Fast/slow pointer pattern |
| ReverseLinkedList     | `leetcode/linkedlist/ReverseLinkedList.java`          | Pointer manipulation      |
| PalindromeLinkedList  | `leetcode/linkedlist/PalindromeLinkedList.java`       | Stack or reverse half     |
| LinkedListCycle       | `leetcode/linkedlist/LinkedListCycle.java`            | Floyd's cycle detection   |
| MergeTwoSortedLists   | `leetcode/linkedlist/MergeTwoSortedLists.java`        | Two-pointer merge         |
| RemoveNthNodeFromEnd  | `leetcode/linkedlist/RemoveNthNodeFromEndOfList.java` | Fast/slow pointer         |

### Recursion and Dynamic Programming

| Challenge    | Location                      | Key Concepts                                |
|--------------|-------------------------------|---------------------------------------------|
| Fibonacci    | `recursion/Fibonacci.java`    | Memoization pattern with Map<Integer, Long> |
| CanSum       | `recursion/CanSum.java`       | Subset sum, decision DP                     |
| HowSum       | `recursion/HowSum.java`       | Return one combination                      |
| BestSum      | `recursion/BestSum.java`      | Optimal substructure, shortest combination  |
| GridTraveler | `recursion/GridTraveler.java` | Path counting, 2D memoization               |
| CanConstruct | `recursion/CanConstruct.java` | Word break variant                          |
| AllConstruct | `recursion/AllConstruct.java` | All combinations, backtracking              |

### Classic Problems

| Challenge                          | Location                                           | Key Concepts                         |
|------------------------------------|----------------------------------------------------|--------------------------------------|
| FizzBuzz                           | `leetcode/FizzBuzz.java`                           | StringBuilder, modulo operations     |
| NumberOfStepsToReduceANumberToZero | `leetcode/NumberOfStepsToReduceANumberToZero.java` | Bit manipulation alternative         |
| TimeConversion                     | `onepreparationweek/TimeConversion.java`           | LocalTime parsing, DateTimeFormatter |

### Array Algorithms (funwitharrays/)

| Challenge                                     | Key Concepts              |
|-----------------------------------------------|---------------------------|
| MergeSortedArray                              | Two pointers from end     |
| RemoveElement                                 | Two pointers, in-place    |
| RemoveDuplicatesFromSortedArray               | Two pointers              |
| MoveZeroes                                    | Two pointers, in-place    |
| ValidMountainArray                            | Single pass verification  |
| ReplaceElementsWithGreatestElementOnRightSide | Reverse traversal         |
| SortArrayByParity                             | Two pointers, partition   |
| SquaresOfSortedArray                          | Two pointers from ends    |
| MaxConsecutiveOnes                            | Sliding window            |
| FindMaxConsecutiveOnes                        | Simple iteration          |
| CheckIfNAndItsDoubleExist                     | HashSet lookup            |
| FindAllNumbersDisappearedInAnArray            | In-place marking          |
| HeightChecker                                 | Counting sort             |
| DuplicateZeros                                | Two-pass approach         |
| EvenNumberOfDigits                            | String conversion or math |
| ABetterRepeatedDeletionAlgorithm              | Two pointers              |

---

## Company-Specific Focus Areas

### Revolut

- Heavy DSA focus: LeetCode medium to hard
- System design basics (high-level only for dev roles)
- Performance optimization questions
- Focus on: Trees, graphs, dynamic programming

### Deel

- DSA + practical problem solving
- Clean code and readability matter
- Focus on: Arrays, strings, hash maps, two pointers

### RevenueCat

- LeetCode patterns mastery
- Data structures fundamentals
- Concurrency basics (not deep)
- Focus on: Hash maps, heaps, sliding window

### GitHub

- Algorithms, trees, and graphs
- Understanding of Git internals (bonus)
- Focus on: Trees, graphs, string manipulation

### Docker

- Systems-level thinking
- Data structures and performance
- Focus on: Arrays, linked lists, memory efficiency

### Datadog

- Algorithms with metrics focus
- Time-series data processing concepts
- Focus on: Sliding window, heaps, sorting

---

## Quick Reference: Java DSA Cheat Sheet

```java
// Initialize common data structures
List<Integer> list = new ArrayList<>();
Set<String> set = new HashSet<>();
Map<String, Integer> map = new HashMap<>();
Queue<Integer> queue = new ArrayDeque<>();
Deque<Integer> stack = new ArrayDeque<>();  // Use as stack: push/pop
PriorityQueue<Integer> minHeap = new PriorityQueue<>();
PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Collections.reverseOrder());

// Array operations
Arrays.

sort(arr);
Arrays.

binarySearch(arr, key);  // Array must be sorted
Arrays.

fill(arr, value);

int[] copy = Arrays.copyOf(arr, arr.length);

// String operations
StringBuilder sb = new StringBuilder();
sb.

append("text");

String result = sb.toString();

// Math
Math.

max(a, b);
Math.

min(a, b);
Math.

abs(x);

int max = Arrays.stream(arr).max().getAsInt();

// Map operations
map.

put(key, value);
map.

getOrDefault(key, defaultValue);
map.

containsKey(key);
map.

merge(key, 1,Integer::sum);  // Increment counter

// Set operations
set.

add(element);
set.

contains(element);

// Queue operations
queue.

offer(element);  // Add
queue.

poll();          // Remove and return head
queue.

peek();          // View head without removing

// Stack operations (using Deque)
deque.

push(element);   // Add to top
deque.

pop();           // Remove from top
deque.

peek();          // View top

// PriorityQueue operations
pq.

offer(element);
pq.

poll();             // Remove min (or max for reverseOrder)
pq.

peek();             // View min/max

// Iteration patterns
for(
int i = 0;
i<arr.length;i++){}
        for(
int num :arr){}
        for(
Map.Entry<K, V> entry :map.

entrySet()){}
        while(!queue.

isEmpty()){}
```

---

## Final Tips

1. **Know your time complexities** - Always state Big O for solutions
2. **Use the right collection** - HashMap vs TreeMap matters
3. **Prefer ArrayDeque over LinkedList** for queues and stacks
4. **Use StringBuilder for string concatenation in loops**
5. **Master the two-pointer patterns** - They appear frequently
6. **Practice both recursive and iterative DFS**
7. **Understand memoization vs tabulation** - Know when to use each
8. **Use var judiciously** - Prefer explicit types in complex algorithms
9. **Handle edge cases** - Empty inputs, single elements, nulls
10. **Test with examples** - Walk through your code mentally before running

Good luck!
