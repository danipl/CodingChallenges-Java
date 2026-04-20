# Interval Patterns Guide

## Quick-Reference: Pattern Selection Matrix

| Pattern                   | Sort Key | Merge Condition     | Time       | Space | When to Use                                            | Example Challenge           |
|---------------------------|----------|---------------------|------------|-------|--------------------------------------------------------|-----------------------------|
| **Merge Intervals**       | Start    | `end >= next.start` | O(n log n) | O(n)  | Merge overlapping intervals into disjoint set          | `MergeIntervals.java`       |
| **Insert Interval**       | N/A      | `end >= next.start` | O(n)       | O(n)  | Insert new interval, merge overlaps                    | `InsertInterval.java`       |
| **Non-overlapping Count** | End      | Greedy select       | O(n log n) | O(1)  | Maximum non-overlapping intervals (Activity Selection) | `NonOverlapping.java`       |
| **Meeting Rooms I**       | Start    | `end > next.start`  | O(n log n) | O(1)  | Check if can attend ALL meetings (no overlap)          | `MeetingRoomsI.java`        |
| **Interval Intersection** | None     | `end >= next.start` | O(n+m)     | O(1)  | Find all intersections of two sorted lists             | `IntervalIntersection.java` |

### At-A-Glance Decision Flow

```
Need to process intervals?
  ├─ YES → Already sorted?
  │          ├─ YES (by start) → Overlapping or merging?
  │          │          ├─ YES → Merge Intervals / Insert Interval
  │          │          └─ NO → Just find intersections → Interval Intersection
  │          └─ NO → Sort by start first
  │                     ├─ Overlap check only? → Meeting Rooms I
  │                     ├─ Count max rooms/concurrent? → Meeting Rooms II
  │                     └─ Merge into disjoint set? → Merge Intervals
  ├─ Need maximum non-overlapping subset?
  │   └─ Sort by END time, greedy select → Non-overlapping (Activity Selection)
  └─ Adding intervals to sorted collection?
      └─ Use TreeMap (disjoint, auto-merge) → Data Stream Intervals
```

---

## Overview

Interval problems are a distinct pattern requiring careful handling of ranges, overlaps, and merges. Unlike array-based
two-pointer problems, intervals often need sorting and collision detection. Key skills: determining sort key, merge
conditions, and sweep line techniques for concurrency counting.

**Core insight**: Most interval problems become trivial once you:

1. Choose the right sort key (start vs end)
2. Define the merge/overlap condition correctly
3. Process in linear pass after sorting

---

## 1. Merge Intervals

**Pattern**: Sort by start, iterate, merge when current end >= next start.

```java
public int[][] merge(int[][] intervals) {
    if (intervals.length <= 1) return intervals;

    // Sort by start time
    Arrays.sort(intervals, Comparator.comparingInt(a -> a[0]));

    List<int[]> result = new ArrayList<>();
    int[] current = intervals[0];

    for (int i = 1; i < intervals.length; i++) {
        if (current[1] >= intervals[i][0]) {
            // Overlap: merge
            current[1] = Math.max(current[1], intervals[i][1]);
        } else {
            // No overlap: add current, move to next
            result.add(current);
            current = intervals[i];
        }
    }
    result.add(current);

    return result.toArray(new int[result.size()][]);
}
```

### Characteristics

| Property         | Value                                   |
|------------------|-----------------------------------------|
| **Sort key**     | Start time                              |
| **Merge**        | `current[1] >= next[0]`                 |
| **Merge action** | `current[1] = max(current[1], next[1])` |
| **Time**         | O(n log n) - sort dominates             |
| **Space**        | O(n) - result list                      |

### When to Use

- Merge overlapping intervals into disjoint set
- Canonical example: scheduling conflicts, merging time ranges
- After merge, result is guaranteed non-overlapping and sorted

### Critical Detail

Merging uses **max** on end times: `current[1] = Math.max(current[1], intervals[i][1])`. This handles cases where the
next interval is completely contained within current.

---

## 2. Insert Interval

**Pattern**: Find insertion point, merge with overlapping intervals, insert back.

```java
public int[][] insert(int[][] intervals, int[] newInterval) {
    List<int[]> result = new ArrayList<>();
    int i = 0, n = intervals.length;

    // 1. Add all intervals ending before newInterval starts
    while (i < n && intervals[i][1] < newInterval[0]) {
        result.add(intervals[i++]);
    }

    // 2. Merge all intervals that overlap with newInterval
    while (i < n && intervals[i][0] <= newInterval[1]) {
        newInterval[0] = Math.min(newInterval[0], intervals[i][0]);
        newInterval[1] = Math.max(newInterval[1], intervals[i][1]);
        i++;
    }
    result.add(newInterval);

    // 3. Add remaining intervals
    while (i < n) {
        result.add(intervals[i++]);
    }

    return result.toArray(new int[result.size()][]);
}
```

### Characteristics

| Property        | Value                                                    |
|-----------------|----------------------------------------------------------|
| **Sort needed** | No - only if input unsorted                              |
| **Merge range** | `intervals[i][1] >= new[0] && intervals[i][0] <= new[1]` |
| **Time**        | O(n) - linear scan                                       |
| **Space**       | O(n) - result list                                       |

### When to Use

- Insert new interval into already-merged/disjoint set
- Maintaining sorted disjoint interval list
- Finding insertion point via end < start comparison

### Three-Phase Strategy

1. Add intervals that end before new starts (no overlap)
2. Merge all overlapping intervals into new interval
3. Add remaining intervals (start after new ends)

---

## 3. Non-overlapping (Activity Selection)

**Pattern**: Sort by END time, greedy select intervals that don't overlap.

```java
public int maxNonOverlapping(int[][] intervals) {
    if (intervals.length == 0) return 0;

    // Sort by end time (crucial: NOT start time)
    Arrays.sort(intervals, Comparator.comparingInt(a -> a[1]));

    int count = 1;
    int currentEnd = intervals[0][1];

    for (int i = 1; i < intervals.length; i++) {
        // Select if start >= previous end (no overlap)
        if (intervals[i][0] >= currentEnd) {
            count++;
            currentEnd = intervals[i][1];
        }
    }

    return count;
}
```

### Characteristics

| Property        | Value                               |
|-----------------|-------------------------------------|
| **Sort key**    | End time (most critical difference) |
| **Select test** | `start >= previous_end`             |
| **Strategy**    | Greedy - always pick earliest end   |
| **Time**        | O(n log n) - sort                   |
| **Space**       | O(1) - constant                     |

### When to Use

- Maximum number of non-overlapping intervals
- Scheduling maximum meetings
- Interval scheduling optimization

### Why Sort by End Time?

Greedy proof: Picking earliest ending interval maximizes room for remaining intervals. Sorting by start time can fail:

- `[[1,100], [2,3], [4,5]]` - by start picks [1,100], only 1
- By end picks [2,3], [4,5], [1,100], 3 intervals

---

## 4. Meeting Rooms I & II

### Meeting Rooms I - Can You Attend All?

**Pattern**: Check if any intervals overlap after sorting by start.

```java
public boolean canAttendMeetings(int[][] intervals) {
    if (intervals.length <= 1) return true;

    Arrays.sort(intervals, Comparator.comparingInt(a -> a[0]));

    for (int i = 1; i < intervals.length; i++) {
        // Overlap if previous end > current start
        if (intervals[i - 1][1] > intervals[i][0]) {
            return false;
        }
    }
    return true;
}
```

### Meeting Rooms II - Minimum Rooms Needed

**Pattern**: Sweep line - count concurrent meetings.

```java
public int minMeetingRooms(int[][] intervals) {
    if (intervals.length == 0) return 0;

    int n = intervals.length;
    int[] starts = new int[n];
    int[] ends = new int[n];

    for (int i = 0; i < n; i++) {
        starts[i] = intervals[i][0];
        ends[i] = intervals[i][1];
    }

    Arrays.sort(starts);
    Arrays.sort(ends);

    int rooms = 0;
    int endPtr = 0;

    for (int start : starts) {
        if (start < ends[endPtr]) {
            // New meeting starts before oldest ends - need room
            rooms++;
        } else {
            // Some meeting ended - reuse room
            endPtr++;
        }
    }

    return rooms;
}
```

### Characteristics

| Property         | Meeting Rooms I     | Meeting Rooms II          |
|------------------|---------------------|---------------------------|
| **Goal**         | Check no overlaps   | Count max concurrent      |
| **Sort**         | By start            | Starts array + ends array |
| **Overlap test** | `prev[1] > curr[0]` | Sweep line counter        |
| **Time**         | O(n log n)          | O(n log n)                |
| **Space**        | O(1)                | O(n)                      |

### When to Use

- **Meeting Rooms I**: Schedule validity check
- **Meeting Rooms II**: Resource allocation (rooms, servers, etc.)

### Sweep Line Insight

The two-pointer sweep on sorted starts/ends works because:

- When `start < end`, a new meeting starts before any ends → room count increases
- When `start >= end`, a meeting ended → room reused (pointer advances)

---

## 5. Interval Intersection

**Pattern**: Two pointers on sorted lists, find overlaps with `[max(start1, start2), min(end1, end2)]`.

```java
public int[][] intervalIntersection(int[][] firstList, int[][] secondList) {
    if (firstList.length == 0 || secondList.length == 0) {
        return new int[0][];
    }

    List<int[]> result = new ArrayList<>();
    int i = 0, j = 0;

    while (i < firstList.length && j < secondList.length) {
        int start = Math.max(firstList[i][0], secondList[j][0]);
        int end = Math.min(firstList[i][1], secondList[j][1]);

        if (start <= end) {
            // Valid intersection
            result.add(new int[]{start, end});
        }

        // Move pointer of interval with earlier end
        if (firstList[i][1] < secondList[j][1]) {
            i++;
        } else {
            j++;
        }
    }

    return result.toArray(new int[result.size()][]);
}
```

### Characteristics

| Property         | Value                            |
|------------------|----------------------------------|
| **Input**        | Two sorted lists of intervals    |
| **Intersection** | `[max(s1,s2), min(e1,e2)]`       |
| **Move rule**    | Advance pointer with smaller end |
| **Time**         | O(n + m) - single pass each list |
| **Space**        | O(1) excluding result            |

### When to Use

- Find overlapping time slots between two calendars
- Merge overlapping ranges from two sources
- Any "find all intersections" problem

### Intersection Formula Proof

For intervals `[s1, e1]` and `[s2, e2]`:

- Overlap exists iff `max(s1, s2) <= min(e1, e2)`
- Intersection is `[max(s1, s2), min(e1, e2)]` (rightmost start, leftmost end)

---

## 6. Data Stream Intervals

**Pattern**: Use TreeMap for O(log n) disjoint interval insertion with auto-merge.

```java
class SummaryRanges {
    private TreeMap<Integer, Integer> intervals;

    public SummaryRanges() {
        intervals = new TreeMap<>();
    }

    // O(log n) - merge overlapping intervals
    public void addNum(int value) {
        int start = value, end = value;

        // Check left neighbor
        Map.Entry<Integer, Integer> left = intervals.floorEntry(value);
        if (left != null && left.getValue() >= value - 1) {
            start = left.getKey();
            end = Math.max(end, left.getValue());
            intervals.remove(left.getKey());
        }

        // Check right neighbor
        Map.Entry<Integer, Integer> right = intervals.ceilingEntry(value);
        if (right != null && right.getKey() <= value + 1) {
            end = Math.max(end, right.getValue());
            intervals.remove(right.getKey());
        }

        intervals.put(start, end);
    }

    public int[][] getIntervals() {
        List<int[]> result = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : intervals.entrySet()) {
            result.add(new int[]{entry.getKey(), entry.getValue()});
        }
        return result.toArray(new int[result.size()][]);
    }
}
```

### Characteristics

| Property           | Value                            |
|--------------------|----------------------------------|
| **Data structure** | TreeMap (NavigableMap)           |
| **Insert**         | O(log n) - find neighbors, merge |
| **Space**          | O(n) - stores disjoint intervals |
| **Query**          | O(n) - iterate tree              |

### When to Use

- Dynamic interval maintenance from data stream
- Disjoint interval union
- When you need O(log n) Lookup/insert and merge on overlap

### TreeMap Magic Methods Used

- `floorEntry(key)` - greatest key <= key
- `ceilingEntry(key)` - least key >= key
- `remove(key)` - O(log n) deletion
- Entry iteration gives sorted order

---

## Record Definition

Use Java 21 record for clean interval representation:

```java
public record Interval(int start, int end) {
    public Interval {
        // Validation: end must be >= start
        if (end < start) {
            throw new IllegalArgumentException(
                    "Invalid interval: end (" + end + ") < start (" + start + ")"
            );
        }
    }

    // Returns empty Optional if no intersection
    public Optional<Interval> intersect(Interval other) {
        int intersectionStart = Math.max(this.start, other.start);
        int intersectionEnd = Math.min(this.end, other.end);

        if (intersectionStart <= intersectionEnd) {
            return Optional.of(new Interval(intersectionStart, intersectionEnd));
        }
        return Optional.empty();
    }

    // True if intervals overlap (touch at endpoint counts as overlap)
    public boolean overlaps(Interval other) {
        return this.end >= other.start && other.end >= this.start;
    }

    // Merge with another interval (assumes overlap)
    public Interval merge(Interval other) {
        return new Interval(
                Math.min(this.start, other.start),
                Math.max(this.end, other.end)
        );
    }
}
```

### Usage Example

```java
Interval a = new Interval(1, 3);
Interval b = new Interval(2, 6);

if(a.

overlaps(b)){
Interval merged = a.merge(b); // [1, 6]
}

Optional<Interval> intersection = a.intersect(new Interval(2, 4));
// intersection.get() = [2, 3]
```

---

## Complexity Table

| Pattern               | Time Complexity | Space Complexity  | When to Prefer             |
|-----------------------|-----------------|-------------------|----------------------------|
| Merge Intervals       | O(n log n)      | O(n)              | General merge problem      |
| Insert Interval       | O(n)            | O(n)              | Already-merged input       |
| Non-overlapping Count | O(n log n)      | O(1)              | Max activity selection     |
| Meeting Rooms I       | O(n log n)      | O(1)              | Schedule validation        |
| Meeting Rooms II      | O(n log n)      | O(n)              | Resource allocation        |
| Interval Intersection | O(n + m)        | O(1) excl. result | Two sorted lists           |
| Data Stream Intervals | O(log n)        | O(n)              | Dynamic stream maintenance |

---

## Java 21 Features

### Records Pattern Matching

```java
public String describe(Interval interval) {
    return switch (interval) {
        case Interval(int start, int end) when start == end -> "Point at " + start;
        case Interval(var s, var e) when s + 1 == e -> "Unit interval [" + s + ", " + e + "]";
        case Interval(var s, var e) -> "Interval [" + s + ", " + e + "]";
    };
}
```

### Switch Expression for Interval Type

```java
public int classifyOverlap(Interval a, Interval b) {
    if (a.start() > b.end() || b.start() > a.end()) {
        return 0; // No overlap
    }
    if (a.start() <= b.start() && a.end() >= b.end()) {
        return 1; // b contained in a
    }
    if (b.start() <= a.start() && b.end() >= a.end()) {
        return 2; // a contained in b
    }
    return 3; // Partial overlap
}
```

### Stream API with Records

```java
public List<Interval> filterByMinLength(List<Interval> intervals, int minLength) {
    return intervals.stream()
            .filter(i -> i.end() - i.start() >= minLength)
            .toList();
}

public Map<Integer, Long> countByLengthBucket(List<Interval> intervals) {
    return intervals.stream()
            .collect(Collectors.groupingBy(
                    i -> (i.end() - i.start()) / 10, // Bucket by tens
                    Collectors.counting()
            ));
}
```

---

## Common Gotchas

### 1. Empty/Null Input Handling

Always check for empty input before processing:

```java
public int[][] merge(int[][] intervals) {
    if (intervals == null || intervals.length == 0) {
        return new int[0][];
    }
    // ... rest
}
```

### 2. Inclusive vs Exclusive Boundaries

**Problem**: Do [1,2] and [2,3] overlap?

- **Meeting Rooms**: [1,2] and [2,3] do NOT overlap (end == start is fine)
- **一般区间合并**: [1,2] and [2,3] DO overlap (touching counts)

**Check your problem statement!** The comparison operator differs:

```java
// Meeting Rooms I: no overlap if end <= next start
if(prev[1]<=curr[0])noOverlap =true;

// General merge: overlap if end >= next start  
        if(prev[1]>=curr[0])overlap =true;
```

### 3. Sort Order Matters

**Critical mistake**: Sorting by start vs end for different patterns.

- **Merge/Insert/Meeting Rooms I/Intersection**: Sort by **start**
- **Non-overlapping (Activity Selection)**: Sort by **end**

Wrong sort = wrong algorithm. Always verify based on pattern.

### 4. Integer Overflow in Comparisons

**Danger**: `a[1] - b[1]` can overflow!

```java
// WRONG - can overflow
Arrays.sort(intervals, (a, b) ->a[0]-b[0]);

// CORRECT
        Arrays.

sort(intervals, Comparator.comparingInt(a ->a[0]));
// or
        Arrays.

sort(intervals, (a, b) ->Integer.

compare(a[0], b[0]));
```

### 5. Array Index Out of Bounds

When comparing `intervals[i-1]`, ensure `i >= 1`. When comparing `intervals[i]` and `intervals[i+1]`, ensure `i+1 < n`.

### 6. Meeting Rooms II Pointer Logic

The sweep line advances the **end pointer only when a meeting ends** (not at every start). This ensures we correctly
count concurrent meetings.

### 7. Data Stream Edge Cases

For `SummaryRanges`:

- Adding value that bridges two intervals (e.g., add 2 to [1,1] and [3,4])
- Adding value that extends an interval (e.g., add 5 to [1,4])
- Duplicate values (should be idempotent)

---

## See Also

### Related Pattern Guides

- **[MAP_GUIDE.md](MAP_GUIDE.md)**: TreeMap (NavigableMap) is essential for Data Stream Intervals pattern. See sections
  on `floorKey`, `ceilingKey`, `subMap` for range queries.
- **[HEAP_GUIDE.md](HEAP_GUIDE.md)**: Heap patterns appear in interval scheduling variants (priority-based selection)
  and streaming interval maintenance.

### Challenge References by Pattern

**Merge Intervals**:

- `development/leetcode/intervals/MergeIntervals.java` — core merge pattern
- `development/leetcode/intervals/OverviewOfAllIntervals.java` — interval summary

**Insert Interval**:

- `development/leetcode/intervals/InsertInterval.java` — sorted insertion with merge

**Non-overlapping**:

- `development/leetcode/intervals/NonOverlappingInterval.java` — Activity Selection
- `development/leetcode/intervals/MinimumTimeToRemove.java` — greedily remove overlapping

**Meeting Rooms**:

- `development/leetcode/intervals/MeetingRoomsI.java` — overlap check
- `development/leetcode/intervals/MeetingRoomsII.java` — minimum rooms (sweep line)
- `development/leetcode/intervals/PutBoxesIntoTheWarehouseII.java` — variable capacity variant

**Interval Intersection**:

- `development/leetcode/intervals/IntervalIntersection.java` — two-pointer intersection
- `development/leetcode/intervals/EmployeeFreeTime.java` — free time between intervals

**Data Stream Intervals**:

- `development/leetcode/intervals/DataStreamIntervals.java` — TreeMap for dynamic intervals
- `development/leetcode/intervals/SummaryRanges.java` — same pattern, different name

### Additional Interval Challenges

- `development/leetcode/intervals/RemoveCoveredIntervals.java` — count non-covered
- `development/leetcode/intervals/IntervalListIntersections.java` — interval list intersection
- `development/leetcode/intervals/TeemoAttacking.java` — non-overlapping sum
- `development/leetcode/intervals/BrickWall.java` — greedy placement
- `development/leetcode/intervals/VideoStitching.java` — minimum coverage

### Alternative Approaches Comparison

| Problem Type        | Interval Pattern | Array Pattern Alternative | Trade-off                        |
|---------------------|------------------|---------------------------|----------------------------------|
| K closest to target | Sort + sweep     | Heap: O(n log k)          | Intervals: O(n log n) sort       |
| Running median      | Two heaps        | Balance with heap         | Heaps: O(log n) add, O(1) median |
| Maximum overlapping | Sweep line       | Prefix sum on events      | Sweep: O(n log n), prefix: O(n)  |
| Disjoint union      | TreeMap          | Sort + merge              | TreeMap: O(log n) per insert     |

---

## Summary

Interval patterns are unified by common techniques: sorting, merging, and sweep lines. Master these core principles:

1. **Merge Intervals**: Sort by start, merge when `end >= next.start`
2. **Insert Interval**: Linear scan to find merge range, insert merged
3. **Non-overlapping (Activity Selection)**: Sort by **end**, greedy select
4. **Meeting Rooms**: I (check overlap), II (sweep line for concurrency)
5. **Interval Intersection**: Two pointers, `[max(start), min(end)]`
6. **Data Stream Intervals**: TreeMap for O(log n) with merge

The key insight across all patterns: **choose the right sort key**. Start time for merge/insert/meeting rooms, end time
for activity selection. Once sorted, most problems reduce to a single linear pass.
