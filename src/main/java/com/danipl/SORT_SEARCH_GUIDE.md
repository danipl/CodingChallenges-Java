# Java Sorting and Searching Guide

## Quick-Reference: Sort Method Selection Matrix

| Use Case                        | Method                       | Algorithm            | Time       | Space    | Stable | When to Use                                      |
|---------------------------------|------------------------------|----------------------|------------|----------|--------|--------------------------------------------------|
| **Primitive array sort**        | `Arrays.sort(int[])`         | Dual-Pivot Quicksort | O(n log n) | O(log n) | ❌      | Fastest for primitives, stability not needed     |
| **Object array sort**           | `Arrays.sort(T[])`           | TimSort              | O(n log n) | O(n)     | ✅      | Stability required, objects with compareTo       |
| **Custom comparator (array)**   | `Arrays.sort(arr, comp)`     | TimSort              | O(n log n) | O(n)     | ✅      | Custom ordering logic for objects                |
| **List sort**                   | `Collections.sort(List)`     | TimSort              | O(n log n) | O(n)     | ✅      | Sort ArrayList/LinkedList in-place               |
| **List sort (Java 8+)**         | `list.sort(Comparator)`      | TimSort              | O(n log n) | O(n)     | ✅      | Default method, preferred over Collections       |
| **Sorted view (no copy)**       | `stream().sorted()`          | TimSort              | O(n log n) | O(n)     | ✅      | Pipeline processing, returns new stream          |
| **Find element (sorted array)** | `Arrays.binarySearch()`      | Binary Search        | O(log n)   | O(1)     | N/A    | Array already sorted, exact match needed         |
| **Find element (sorted list)**  | `Collections.binarySearch()` | Binary Search        | O(log n)   | O(1)     | N/A    | List already sorted, exact match needed          |
| **Custom binary search**        | Manual implementation        | Binary Search        | O(log n)   | O(1)     | N/A    | Custom conditions (first/last occurrence, range) |
| **Unstable sort accepted**      | Manual QuickSort             | QuickSort            | O(n log n) | O(log n) | ❌      | Educational, or specific pivot strategies        |
| **Stability critical**          | Manual MergeSort             | MergeSort            | O(n log n) | O(n)     | ✅      | Educational, guaranteed stability                |

### At-A-Glance Decision Flow

```
Need to sort?
├─ Primitive array (int[], double[], etc.)?
│   └─ YES → Arrays.sort(arr) [Dual-Pivot Quicksort, NOT stable]
└─ Objects or need custom order?
    ├─ Array of Objects (T[])?
    │   ├─ Natural order → Arrays.sort(arr) [TimSort, stable]
    │   └─ Custom order → Arrays.sort(arr, Comparator) [TimSort, stable]
    ├─ List<T>?
    │   └─ list.sort(Comparator) or Collections.sort(list) [TimSort, stable]
    └─ Stream pipeline?
        └─ stream().sorted(Comparator) → collect [TimSort, stable]

Need to search?
├─ Data already sorted?
│   ├─ YES, array → Arrays.binarySearch(arr, key)
│   ├─ YES, List → Collections.binarySearch(list, key)
│   └─ YES, custom condition → Manual binary search template
└─ NO (unsorted)?
    ├─ Single search → Linear scan O(n)
    └─ Multiple searches → Sort first O(n log n), then binary search O(log n)
```

---

## Overview

Java provides multiple sorting and searching mechanisms via `java.util.Arrays`, `java.util.Collections`, and the
Collections Framework. Each is optimized for different data types (primitives vs objects), ordering requirements (
natural vs custom), and stability guarantees.

**Key distinction**: Primitive sorting (`Arrays.sort` on primitives) uses **Dual-Pivot Quicksort** which is **NOT stable
**, while object sorting uses **TimSort** which **IS stable**. This is critical for multi-key sorting scenarios.

---

## 1. Arrays.sort - Primitive Arrays

```java
int[] nums = {5, 2, 8, 1, 9};
Arrays.

sort(nums);  // [1, 2, 5, 8, 9]
```

### Characteristics

| Property        | Value                                  |
|-----------------|----------------------------------------|
| **Algorithm**   | Dual-Pivot Quicksort (Java 7+)         |
| **Ordering**    | Ascending (natural order)              |
| **Stable**      | ❌ NO - equal elements may be reordered |
| **Thread-safe** | ❌ No                                   |
| **Performance** | O(n log n) average, O(n²) worst (rare) |
| **Space**       | O(log n) stack space                   |

### Complexity

| Operation    | Average    | Worst Case |
|--------------|------------|------------|
| Sort array   | O(n log n) | O(n²)      |
| Partitioning | O(log n)   | O(n)       |

> **Dual-Pivot Quicksort**: Uses two pivots instead of one, improving performance on many data distributions. The worst
> case O(n²) is rare in practice due to careful pivot selection.

### When to Use

- Sorting primitive arrays: `int[]`, `double[]`, `long[]`, `char[]`, etc.
- Fastest general-purpose sort for primitives
- Stability is NOT required
- Common in competitive programming and coding challenges

**Practical pattern from challenges:**

```java
// From: SquaresOfSortedArray.java
public int[] sortedSquares(int[] nums) {
    for (int pos = 0; pos < nums.length; pos++) {
        nums[pos] *= nums[pos];  // Square in-place
    }
    Arrays.sort(nums);           // Sort the squared values
    return nums;
}
```

**Sorted sub-range:**

```java
// Sort only portion of array
Arrays.sort(arr, fromIndex, toIndex);  // [fromIndex, toIndex)
```

---

## 2. Arrays.sort - Object Arrays

```java
String[] names = {"Charlie", "Alice", "Bob"};
Arrays.

sort(names);  // [Alice, Bob, Charlie]
```

### Characteristics

| Property        | Value                                 |
|-----------------|---------------------------------------|
| **Algorithm**   | TimSort (merge sort variant)          |
| **Ordering**    | Natural order or custom Comparator    |
| **Stable**      | ✅ YES - equal elements maintain order |
| **Thread-safe** | ❌ No                                  |
| **Performance** | O(n log n)                            |
| **Space**       | O(n)                                  |

### Complexity

| Operation   | Average & Worst |
|-------------|-----------------|
| Sort array  | O(n log n)      |
| Merge phase | O(n)            |

> **TimSort**: Hybrid stable algorithm derived from MergeSort and InsertionSort. Identifies "runs" (already sorted
> segments) and merges them efficiently. Performs exceptionally well on partially sorted data.

### When to Use

- Sorting object arrays requiring stability
- Natural ordering (implements Comparable)
- Custom ordering via Comparator
- Multi-key sorting (sort by multiple fields sequentially)

**Practical pattern from challenges:**

```java
// From: ZigZagSequence.java
public static void findZigZagSequence(int[] a, int n) {
    Arrays.sort(a);  // First step: sort the array ascending
    // ... subsequent zig-zag transformations
}
```

### Comparator-Based Sorting

```java
// Custom comparator for objects
Arrays.sort(objects, (a, b) ->b.

getValue() -a.

getValue()); // Descending

// Comparator with multiple criteria
        Arrays.

sort(people,
     Comparator.comparingInt(Person::getAge)
              .

reversed()
              .

thenComparing(Person::getName)
);

// Case-insensitive string sort
        Arrays.

sort(strings, String.CASE_INSENSITIVE_ORDER);
```

**Practical descending sort:**

```java
// Sort Integer[] descending (NOT int[]!)
Integer[] nums = {5, 2, 8, 1, 9};
Arrays.

sort(nums, Collections.reverseOrder());
```

---

## 3. Collections.sort / List.sort

```java
List<Integer> list = Arrays.asList(5, 2, 8, 1, 9);
list.

sort(Comparator.naturalOrder()); // Java 8+ preferred
// OR
        Collections.

sort(list); // Legacy, still valid
```

### Characteristics

| Property        | Value                              |
|-----------------|------------------------------------|
| **Algorithm**   | TimSort                            |
| **Ordering**    | Natural order or custom Comparator |
| **Stable**      | ✅ YES                              |
| **Thread-safe** | ❌ No                               |
| **Performance** | O(n log n)                         |
| **Space**       | O(n)                               |

> **Collections.sort vs List.sort**: `List.sort()` is a Java 8 default method that internally calls
`Collections.sort()`. Prefer `list.sort(comparator)` for cleaner syntax.

### Complexity

| Operation | Average & Worst |
|-----------|-----------------|
| Sort list | O(n log n)      |
| Iteration | O(n)            |

### When to Use

- **ArrayList**: Efficient sort, backed by array
- **LinkedList**: Still O(n log n), but higher constant factors
- Sorting mutable lists in-place

**Comparator chaining patterns:**

```java
// Ascending by age, then by name
list.sort(Comparator.comparingInt(Person::getAge)
                    .

thenComparing(Person::getName));

// Descending by age, then ascending by name
        list.

sort(Comparator.comparingInt(Person::getAge).

reversed()
                    .

thenComparing(Person::getName));

// Null-safe sorting
        list.

sort(Comparator.nullsLast(Comparator.comparing(Person::getName)));

// Using lambda for custom logic
        list.

sort((a, b) ->{
int result = Integer.compare(a.getPriority(), b.getPriority());
    return result !=0?result :a.

getId().

compareTo(b.getId());
        });
```

**Primitive comparators (avoid boxing):**

```java
// Use comparingInt instead of comparing for primitives
list.sort(Comparator.comparingInt(Person::getAge));    // ✅ int
        list.

sort(Comparator.comparingLong(Person::getId));    // ✅ long
        list.

sort(Comparator.comparingDouble(Person::getScore)); // ✅ double
```

---

## 4. Binary Search - Arrays.binarySearch

```java
int[] sorted = {1, 3, 5, 7, 9};
int index = Arrays.binarySearch(sorted, 5);  // Returns 2
```

### Return Value Semantics

| Return Value | Meaning                                     |
|--------------|---------------------------------------------|
| **>= 0**     | Index where element found                   |
| **< 0**      | `-(insertionPoint) - 1` - element not found |

### Insertion Point Formula

```java
int result = Arrays.binarySearch(arr, key);
if(result< 0){
int insertionPoint = -(result + 1);
// This is where key would be inserted to maintain sorted order
}
```

### Characteristics

| Property         | Value                                      |
|------------------|--------------------------------------------|
| **Precondition** | Array MUST be sorted, or results undefined |
| **Algorithm**    | Binary Search                              |
| **Time**         | O(log n)                                   |
| **Space**        | O(1)                                       |

### When to Use

- Array is already sorted
- Need exact match lookup
- Single search (for multiple searches, sorting + binary search beats linear scan)

**Practical pattern from challenges:**

```java
// From: MergeSortedArray.java - two-pointer merge approach
public void merge(int[] nums1, int m, int[] nums2, int n) {
    // Merge two sorted arrays into nums1
    // Alternative: copy + Arrays.sort (used in basic solution)
    for (int pos = 0; pos < n; pos++) {
        nums1[m + pos] = nums2[pos];
    }
    Arrays.sort(nums1);
}
```

### Binary Search with Range

```java
// Search within sub-range
int index = Arrays.binarySearch(arr, fromIndex, toIndex, key);
```

### Binary Search on Objects

```java
// With Comparator
String[] names = {"Alice", "Bob", "Charlie"};
int index = Arrays.binarySearch(names, "Bob", String.CASE_INSENSITIVE_ORDER);

// Custom object comparator
Person[] people = ...;
int index = Arrays.binarySearch(people, target,
        Comparator.comparingInt(Person::getId));
```

---

## 5. Binary Search - Custom Implementation Template

```java
// Standard Binary Search Template
public int binarySearch(int[] nums, int target) {
    int left = 0;
    int right = nums.length - 1;

    while (left <= right) {
        int mid = left + (right - left) / 2;  // Avoids overflow!

        if (nums[mid] == target) {
            return mid;
        } else if (nums[mid] < target) {
            left = mid + 1;
        } else {
            right = mid - 1;
        }
    }

    return -1;  // Not found
}
```

### Critical: Avoid Integer Overflow

```java
// ❌ WRONG - can overflow for large indices
int mid = (left + right) / 2;

// ✅ CORRECT - safe from overflow
int mid = left + (right - left) / 2;
int mid = (left + right) >>> 1;  // Also correct (unsigned right shift)
```

### Variation: Find First Occurrence

```java
public int findFirst(int[] nums, int target) {
    int left = 0, right = nums.length - 1;
    int result = -1;

    while (left <= right) {
        int mid = left + (right - left) / 2;

        if (nums[mid] == target) {
            result = mid;  // Record match
            right = mid - 1;  // Continue searching left half
        } else if (nums[mid] < target) {
            left = mid + 1;
        } else {
            right = mid - 1;
        }
    }

    return result;
}
```

### Variation: Find Last Occurrence

```java
public int findLast(int[] nums, int target) {
    int left = 0, right = nums.length - 1;
    int result = -1;

    while (left <= right) {
        int mid = left + (right - left) / 2;

        if (nums[mid] == target) {
            result = mid;  // Record match
            left = mid + 1;  // Continue searching right half
        } else if (nums[mid] < target) {
            left = mid + 1;
        } else {
            right = mid - 1;
        }
    }

    return result;
}
```

### Variation: Find Lower Bound (first element >= target)

```java
public int lowerBound(int[] nums, int target) {
    int left = 0, right = nums.length;

    while (left < right) {
        int mid = left + (right - left) / 2;

        if (nums[mid] >= target) {
            right = mid;
        } else {
            left = mid + 1;
        }
    }

    return left;  // May equal nums.length if target > all elements
}
```

### Variation: Find Upper Bound (first element > target)

```java
public int upperBound(int[] nums, int target) {
    int left = 0, right = nums.length;

    while (left < right) {
        int mid = left + (right - left) / 2;

        if (nums[mid] > target) {
            right = mid;
        } else {
            left = mid + 1;
        }
    }

    return left;
}
```

### Variation: Count Occurrences in Sorted Array

```java
public int countOccurrences(int[] nums, int target) {
    int first = findFirst(nums, target);
    if (first == -1) return 0;

    int last = findLast(nums, target);
    return last - first + 1;
}
```

### Variation: Rotate Search (Circular Array)

```java
// Find minimum in rotated sorted array
public int findMin(int[] nums) {
    int left = 0, right = nums.length - 1;

    while (left < right) {
        int mid = left + (right - left) / 2;

        if (nums[mid] > nums[right]) {
            left = mid + 1;  // Min is in right half
        } else {
            right = mid;  // Min is in left half (or mid)
        }
    }

    return nums[left];
}
```

---

## 6. Custom Sort Implementations

### QuickSort Template (Educational)

```java
public void quickSort(int[] arr, int low, int high) {
    if (low < high) {
        int pi = partition(arr, low, high);
        quickSort(arr, low, pi - 1);
        quickSort(arr, pi + 1, high);
    }
}

private int partition(int[] arr, int low, int high) {
    int pivot = arr[high];
    int i = low - 1;

    for (int j = low; j < high; j++) {
        if (arr[j] <= pivot) {
            i++;
            swap(arr, i, j);
        }
    }
    swap(arr, i + 1, high);
    return i + 1;
}

private void swap(int[] arr, int i, int j) {
    int temp = arr[i];
    arr[i] = arr[j];
    arr[j] = temp;
}
```

**Characteristics:**

| Property | Value                       |
|----------|-----------------------------|
| Time     | O(n log n) avg, O(n²) worst |
| Space    | O(log n)                    |
| Stable   | ❌ No                        |
| In-place | ✅ Yes                       |

### MergeSort Template (Educational)

```java
public void mergeSort(int[] arr, int left, int right) {
    if (left < right) {
        int mid = left + (right - left) / 2;
        mergeSort(arr, left, mid);
        mergeSort(arr, mid + 1, right);
        merge(arr, left, mid, right);
    }
}

private void merge(int[] arr, int left, int mid, int right) {
    int[] temp = new int[right - left + 1];
    int i = left, j = mid + 1, k = 0;

    // Merge two sorted halves
    while (i <= mid && j <= right) {
        if (arr[i] <= arr[j]) {  // <= ensures stability
            temp[k++] = arr[i++];
        } else {
            temp[k++] = arr[j++];
        }
    }

    // Copy remaining elements
    while (i <= mid) temp[k++] = arr[i++];
    while (j <= right) temp[k++] = arr[j++];

    // Copy back to original array
    System.arraycopy(temp, 0, arr, left, temp.length);
}
```

**Characteristics:**

| Property | Value      |
|----------|------------|
| Time     | O(n log n) |
| Space    | O(n)       |
| Stable   | ✅ Yes      |
| In-place | ❌ No       |

### When Custom Implementations Are Needed

- **Educational purposes** - understanding algorithms
- **Custom constraints** - linked list sorting (MergeSort preferred)
- **External sorting** - data doesn't fit in memory
- **Specialized pivots** - domain-specific optimization
- **Stability with primitives** - manual MergeSort on primitives

**In practice: Use `Arrays.sort` unless you have a specific need.**

---

## Collections Utility Methods

The `java.util.Collections` class provides essential collection manipulation methods:

### Sorting and Shuffling

```java
// Sort with natural order
Collections.sort(list);

// Sort with custom comparator
Collections.

sort(list, Comparator.naturalOrder());

// Reverse order
        Collections.

reverse(list);

// Shuffle randomly
Collections.

shuffle(list);
Collections.

shuffle(list, new Random(seed));  // With seed
```

### Filling and Replacing

```java
// Fill entire list with value
Collections.fill(list, value);

// Replace all occurrences
Collections.

replaceAll(list, oldVal, newVal);

// Rotate list by distance
Collections.

rotate(list, distance);  // Positive = right, Negative = left
```

### Finding and Counting

```java
// Find maximum/minimum
Integer max = Collections.max(list);
Integer min = Collections.min(list);

// With comparator
Person oldest = Collections.max(people, Comparator.comparingInt(Person::getAge));

// Count occurrences
int count = Collections.frequency(list, element);

// Find index of
int index = Collections.indexOfSubList(list, sublist);
int lastIndex = Collections.lastIndexOfSubList(list, sublist);
```

### Swapping and Adding

```java
// Swap two elements by index
Collections.swap(list, i, j);

// Add all elements efficiently
Collections.

addAll(list, elem1, elem2, elem3);
// Better than: list.add(elem1); list.add(elem2); ...

// Create unmodifiable view
List<T> unmodifiable = Collections.unmodifiableList(list);

// Create synchronized (thread-safe) view
List<T> sync = Collections.synchronizedList(list);
```

### Binary Search

```java
// Binary search on sorted list
int index = Collections.binarySearch(list, key);

// With comparator
int index = Collections.binarySearch(list, key, comparator);

// Returns same semantics as Arrays.binarySearch
```

### Common Patterns

```java
// Initialize and populate in one line
List<String> names = new ArrayList<>();
Collections.

addAll(names, "Alice","Bob","Charlie");

// Shuffle for randomization
Collections.

shuffle(list);

// Reverse for descending after ascending sort
Collections.

sort(list);
Collections.

reverse(list);

// Frequency count
Map<Integer, Integer> freq = new HashMap<>();
for(
int num :nums){
        freq.

merge(num, 1,Integer::sum);
}

// Find most common element
int mostCommon = Collections.max(freq.keySet(),
        Comparator.comparingInt(freq::get));
```

---

## Comparator Patterns

### Basic Comparators

```java
// Natural order
Comparator<Integer> natural = Comparator.naturalOrder();

// Reverse order
Comparator<Integer> reverse = Comparator.reverseOrder();

// Null handling
Comparator<String> nullFirst = Comparator.nullsFirst(Comparator.naturalOrder());
Comparator<String> nullLast = Comparator.nullsLast(Comparator.naturalOrder());
```

### Comparator.comparing() Chain

```java
// Single field ascending
list.sort(Comparator.comparing(Person::getName));

// Single field descending
        list.

sort(Comparator.comparing(Person::getAge).

reversed());

// Multiple fields
        list.

sort(Comparator.comparing(Person::getLastName)
                    .

thenComparing(Person::getFirstName));

// Mixed order
        list.

sort(Comparator.comparingInt(Person::getPriority).

reversed()
                    .

thenComparing(Person::getName));
```

### Primitive Comparators (Avoid Boxing)

```java
// Use specialized comparators for primitives
Comparator<Person> byAge = Comparator.comparingInt(Person::getAge);
Comparator<Person> byId = Comparator.comparingLong(Person::getId);
Comparator<Person> byScore = Comparator.comparingDouble(Person::getScore);

// Chain with primitives
list.

sort(Comparator.comparingInt(Person::getAge)
                    .

thenComparingDouble(Person::getScore));
```

### Lambda Comparators

```java
// Simple comparison
list.sort((a, b) ->Integer.

compare(a.getValue(),b.

getValue()));

// Multi-field comparison
        list.

sort((a, b) ->{
int cmp = Integer.compare(a.getPriority(), b.getPriority());
    return cmp !=0?cmp :a.

getName().

compareTo(b.getName());
        });

// String comparison (case-insensitive)
        list.

sort(Comparator.comparing(Person::getName, String.CASE_INSENSITIVE_ORDER));
```

### Comparator for Arrays.sort

```java
// Sort array of objects with custom comparator
Person[] people = ...;
        Arrays.

sort(people, Comparator.comparingInt(Person::getAge));

// Sort 2D array by column
int[][] matrix = ...;
        Arrays.

sort(matrix, (a, b) ->Integer.

compare(a[0], b[0]));

// Sort String array by length
String[] strings = ...;
        Arrays.

sort(strings, Comparator.comparingInt(String::length));
```

### Comparator Consistency with equals()

```java
// ⚠️ CRITICAL: Comparator must be consistent with equals()
// If compare(a, b) == 0, then a.equals(b) should be true

// INCORRECT - violates consistency
class Person {
    String name;
    int id;
}
// Comparator comparing only by name when equals() uses both name and id

// CORRECT - use all fields that equals() uses
Comparator<Person> comparator = Comparator
        .comparing(Person::getName)
        .thenComparingInt(Person::getId);
```

---

## Java 21 Features

### Stream.sorted() Enhancements

```java
// Sorted stream with natural order
list.stream()
    .

sorted()
    .

forEach(System.out::println);

// Sorted with comparator
list.

stream()
    .

sorted(Comparator.comparingInt(Person::getAge).

reversed())
        .

toList();

// Sorted view without modifying original
List<Integer> sorted = list.stream()
        .sorted()
        .toList();  // Returns new List (Java 16+)
```

### Comparator Improvements

```java
// thenComparing with type inference
list.sort(Comparator.comparing(Person::getName)
                    .

thenComparingInt(Person::getAge)
                    .

thenComparingDouble(Person::getScore));

// nullsFirst/Last with method reference
        list.

sort(Comparator.nullsFirst(
        Comparator.comparingInt(Person::getAge)
));

// Chained null handling
        list.

sort(Comparator .<Person, String>comparing(Person::getName,
     Comparator.nullsLast(Comparator.naturalOrder()))
        .

thenComparingInt(Person::getAge));
```

### Sequenced Collections (Java 21)

While not directly sorting-related, Java 21's SequencedCollection provides consistent iteration:

```java
// Lists now have first()/last()/reversed()
List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
list.

reversed().

forEach(System.out::print);  // 54321

// Combined with sorting
list.

sort(naturalOrder());
Integer first = list.getFirst();   // Smallest
Integer last = list.getLast();     // Largest
```

### Pattern Matching for switch (Java 21)

```java
// Use pattern matching with sorted collections
Object result = switch (list.size()) {
            case 0 -> "Empty";
            case 1 -> list.getFirst();
            default -> {
                list.sort(Comparator.naturalOrder());
                yield list.getFirst() + " to " + list.getLast();
            }
        };
```

---

## Common Gotchas

### Binary Search Requirements

1. **Array/List MUST be sorted** - Binary search on unsorted data gives undefined results
2. **Verify sorted state** - Don't assume input is sorted
3. **Natural order or comparator match** - `binarySearch` with Comparator requires list sorted with same comparator

```java
// ❌ WRONG - unsorted array
int[] unsorted = {5, 2, 8, 1, 9};
Arrays.

binarySearch(unsorted, 8);  // Undefined behavior!

// ✅ CORRECT
int[] sorted = {1, 2, 5, 8, 9};
int index = Arrays.binarySearch(sorted, 8);  // 3
```

### Mid Calculation Overflow

```java
// ❌ WRONG - can overflow for large arrays
int mid = (left + right) / 2;

// ✅ CORRECT - two safe alternatives
int mid = left + (right - left) / 2;
int mid = (left + right) >>> 1;  // Unsigned right shift
```

### Arrays.sort on Primitives is NOT Stable

```java
// ❌ WRONG - assuming stability with primitives
int[][] arr = {{1, 5}, {2, 3}, {1, 2}};
Arrays.

sort(arr, (a, b) ->Integer.

compare(a[0], b[0])); // Uses TimSort (stable)

int[] primitive = {2, 3, 2, 1, 2};
Arrays.

sort(primitive);  // Dual-Pivot Quicksort - 2s may be reordered!
```

### Comparator Consistency Violation

```java
// ❌ WRONG - violates Comparator contract (antisymmetry)
Comparator<String> bad = (a, b) -> a.length() - b.length();  // Works
Comparator<String> worse = (a, b) -> {
    if (a.equals("special")) return -1;  // Violates: compare(a,b) != -compare(b,a)
    return a.compareTo(b);
};

// ✅ CORRECT - use Integer.compare or Comparator.comparing
Comparator<String> good = Comparator.comparingInt(String::length);
```

### Collections.sort Modifies Original List

```java
// ⚠️ sort() is in-place - original list is modified
List<Integer> list = new ArrayList<>(Arrays.asList(3, 1, 4, 1, 5));
Collections.

sort(list);
// list is now [1, 1, 3, 4, 5]

// ✅ To preserve original, copy first
List<Integer> sorted = new ArrayList<>(original);
sorted.

sort(Comparator.naturalOrder());

// ✅ Or use streams
List<Integer> sorted = original.stream()
        .sorted()
        .toList();  // Returns new list
```

### Binary Search Return Value Misinterpretation

```java
int result = Arrays.binarySearch(sorted, missingKey);

if(result< 0){
// ❌ WRONG - don't use result directly
int wrongIndex = result;  // This is NEGATIVE!

// ✅ CORRECT - convert to insertion point
int insertionPoint = -(result + 1);

// Handle "not found"
    if(result ==-1){  // Insertion point is 0
        // Key smaller than all elements
        }
        }
```

### Concurrent Modification During Sort

```java
// ❌ WRONG - modifying collection during sort
List<Integer> list = new ArrayList<>(...);
        list.

stream()
    .

sorted()
    .

forEach(x ->list.

remove(x));  // ConcurrentModificationException!

// ✅ CORRECT - collect sorted results, then modify
List<Integer> sorted = list.stream().sorted().toList();
list.

clear();
list.

addAll(sorted);
```

### Multi-threaded Sorting

```java
// ❌ WRONG - Arrays.sort is NOT thread-safe
int[] shared = ...;
// Thread 1: Arrays.sort(shared);
// Thread 2: Arrays.sort(shared);  // Data race!

// ✅ CORRECT - synchronize or use thread-local copy
synchronized(shared){
        Arrays.

sort(shared);
}

// ✅ Or copy for each thread
int[] localCopy = shared.clone();
Arrays.

sort(localCopy);
```

### Sorting Sub-range Boundaries

```java
// ⚠️ toIndex is EXCLUSIVE
int[] arr = {1, 2, 3, 4, 5};
Arrays.

sort(arr, 1,3);  // Sorts indices [1, 3) = elements at 1, 2
// Result: {1, 2, 3, 4, 5} - elements 2 and 3 are sorted

// ✅ To include index 3
Arrays.

sort(arr, 1,4);  // Sorts indices [1, 4) = elements at 1, 2, 3
```

### Integer Array vs Primitive Array Performance

```java
// ❌ SLOWER - auto-boxing overhead
Integer[] boxed = {5, 2, 8, 1, 9};
Arrays.

sort(boxed);  // Object comparisons, memory indirection

// ✅ FASTER - primitive operations
int[] primitive = {5, 2, 8, 1, 9};
Arrays.

sort(primitive);  // Direct comparisons, cache-friendly

// Use primitives when possible for performance-critical code
```

---

## Performance Comparison

### Sorting Performance Summary

| Method                   | Time (Average) | Space    | Stable | Best For                |
|--------------------------|----------------|----------|--------|-------------------------|
| `Arrays.sort(int[])`     | O(n log n)     | O(log n) | ❌      | Primitive arrays        |
| `Arrays.sort(T[])`       | O(n log n)     | O(n)     | ✅      | Object arrays           |
| `Collections.sort(List)` | O(n log n)     | O(n)     | ✅      | Lists                   |
| `stream().sorted()`      | O(n log n)     | O(n)     | ✅      | Stream pipelines        |
| Manual QuickSort         | O(n log n)*    | O(log n) | ❌      | Educational             |
| Manual MergeSort         | O(n log n)     | O(n)     | ✅      | Linked lists, stability |

*Worst case O(n²) for QuickSort

### Binary Search Performance

| Method                       | Time     | Space | Precondition       |
|------------------------------|----------|-------|--------------------|
| `Arrays.binarySearch()`      | O(log n) | O(1)  | Array sorted       |
| `Collections.binarySearch()` | O(log n) | O(1)  | List sorted        |
| Manual binary search         | O(log n) | O(1)  | Custom conditions  |
| Linear search                | O(n)     | O(1)  | None (unsorted OK) |

### When to Sort First vs Linear Scan

```
Single search:   Linear scan O(n) - no sort needed
Multiple searches (k times):
├─ k < log n:    Linear scans O(k*n) - may not need sort
└─ k >= log n:   Sort + binary search O(n log n + k*log n) - worth it!
```

**Example**: Array of 1000 elements, 100 searches:

- **100 linear scans**: 100 × 1000 = 100,000 operations
- **Sort + 100 binary searches**: 1000×10 + 100×10 = 11,000 operations

---

## See Also

### Related Challenge Files

1. **SquaresOfSortedArray.java** - Demonstrates `Arrays.sort()` on primitive int[]
    - Pattern: Square elements → Sort the result
    - File: `src/main/java/com/danipl/development/leetcode/funwitharrays/SquaresOfSortedArray.java`

2. **MergeSortedArray.java** - Merging two sorted arrays
    - Pattern: Two-pointer merge vs copy-and-sort approach
    - File: `src/main/java/com/danipl/development/leetcode/funwitharrays/MergeSortedArray.java`

3. **ZigZagSequence.java** - Sorting as first step in transformation
    - Pattern: Arrays.sort → Rearrange for zig-zag pattern
    - File: `src/main/java/com/danipl/development/onepreparationweek/ZigZagSequence.java`

### Related Guides

- **MAP_GUIDE.md** - Map implementations and selection matrix
- **PREPARATION.md** - Development role preparation with sorting algorithms section

### Java Documentation

- [Arrays API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Arrays.html)
- [Collections API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Collections.html)
- [Comparator API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Comparator.html)
- [TimSort - Wikipedia](https://en.wikipedia.org/wiki/Timsort)

---

## Quick Reference Cheat Sheet

```java
// === SORTING ===
// Primitive array
Arrays.sort(intArray);
Arrays.

sort(intArray, from, to);  // Sub-range

// Object array with natural order
Arrays.

sort(objectArray);

// Object array with comparator
Arrays.

sort(objectArray, Comparator.comparingInt(X::getField));

// List sorting (Java 8+)
        list.

sort(Comparator.naturalOrder());
        list.

sort(Comparator.comparing(X::getField).

reversed());

// Legacy Collections.sort
        Collections.

sort(list);
Collections.

sort(list, comparator);

// Stream sorted view
List<T> sorted = list.stream().sorted().toList();

// === BINARY SEARCH ===
// Arrays.binarySearch
int idx = Arrays.binarySearch(sortedArray, key);
int idx = Arrays.binarySearch(sortedArray, from, to, key);

// If not found: insertionPoint = -(result + 1)

// Collections.binarySearch
int idx = Collections.binarySearch(sortedList, key);
int idx = Collections.binarySearch(sortedList, key, comparator);

        // Custom binary search template
        int left = 0, right = arr.length - 1;
while(left <=right){
int mid = left + (right - left) / 2;
    if(arr[mid]==target)return mid;
    if(arr[mid] <target)left =mid +1;
        else right =mid -1;
        }
        return-1;

// === COLLECTIONS UTILITIES ===
        Collections.

reverse(list);
Collections.

shuffle(list);
Collections.

fill(list, value);
Collections.

swap(list, i, j);
Collections.

frequency(list, element);
Collections.

max(list);
Collections.

min(list);
Collections.

addAll(list, elems...);
```

(End of file - total 745 lines)
