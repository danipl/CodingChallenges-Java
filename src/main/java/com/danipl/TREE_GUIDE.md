# Java 21 Tree Algorithms Guide

## Quick-Reference: Tree Type Selection Matrix

| Tree Type              | Use Case                     | Key Property                  | Search/Insert/Delete      | Space               | When to Use                               |
|------------------------|------------------------------|-------------------------------|---------------------------|---------------------|-------------------------------------------|
| **Binary Tree**        | General hierarchical data    | Max 2 children per node       | O(h) traversal            | O(n)                | Expression trees, decision trees          |
| **BST**                | Sorted data with fast lookup | Left < Root < Right           | O(h) avg, O(n) worst      | O(n)                | In-order gives sorted sequence            |
| **AVL Tree**           | Self-balancing BST           | Height-balanced (             | left-right                | ≤ 1)                | O(log n) guaranteed                       | O(n)        | When insert/delete order unpredictable |
| **Red-Black Tree**     | Java TreeMap/TreeSet         | Balanced via color properties | O(log n) guaranteed       | O(n)                | Sorted map/set (built into JDK)           |
| **Trie (Prefix Tree)** | String prefix operations     | Path represents string/prefix | O(L) where L = key length | O(ALPHABET × N × L) | Autocomplete, word dictionary, IP routing |
| **N-ary Tree**         | Multi-child hierarchy        | Children list, not just 2     | O(n) traversal            | O(n)                | File systems, organization charts, DOM    |
| **Segment Tree**       | Range queries (scope locked) | Interval decomposition        | O(log n) query/update     | O(n)                | Range sum, range min/max (see DP guide)   |
| **Fenwick Tree**       | Prefix sums                  | BIT array representation      | O(log n) query/update     | O(n)                | Alternative to segment tree               |

### At-A-Glance Decision Flow

```
Need to store hierarchical data?
├─ YES → Keys are strings with prefix operations?
│          └─ YES → Trie (Prefix Tree)
│                    ├─ insert(word) — O(L)
│                    ├─ search(word) — O(L)
│                    └─ startsWith(prefix) — O(L)
├─ Keys need sorted order + range queries?
│          └─ YES → Use TreeMap/TreeSet (Red-Black internally)
│                    ├─ floorKey(), ceilingKey()
│                    └─ subMap(), headMap(), tailMap()
├─ Binary structure with BST property?
│          ├─ YES → BST operations
│          │          ├─ Search/Insert/Delete — O(h)
│          │          ├─ In-order traversal — sorted
│          │          └─ Validate BST — entire subtree constraint
│          └─ NO → General binary tree
│                    ├─ Traversals (inorder, preorder, postorder, level)
│                    ├─ Max depth, balanced check
│                    └─ LCA, path sum, serialize/deserialize
└─ More than 2 children per node?
           └─ YES → N-ary Tree
                     ├─ Children as List<TreeNode>
                     ├─ Level-order traversal
                     └─ Serialize via preorder + null markers
```

---

## Overview

Tree problems in interviews test understanding of recursion, traversal patterns, and structural properties. Unlike
graphs, trees are acyclic connected structures with a clear root-to-leaf hierarchy. Binary trees (max 2 children) and
their specializations (BST, Trie, N-ary) dominate interview questions.

**Key tree vs graph distinction:**

- **Tree**: Exactly one path between any two nodes, has a root, parent-child relationship
- **Graph**: Multiple paths possible, cycles allowed, no inherent root

**Core tree problem types:**

- **Traversal**: Inorder, preorder, postorder (recursive + iterative), level-order (BFS)
- **BST Operations**: Search, insert, delete, validation, kth smallest
- **Trie Operations**: Insert, search, prefix matching, word patterns
- **Structural**: Max depth, balanced check, symmetric, diameter
- **Path Problems**: Path sum, root-to-leaf paths, all paths
- **Advanced**: LCA, serialize/deserialize, build from traversal orders

**Java tree representation patterns:**

- Record-based nodes (Java 14+): `record TreeNode(int val, TreeNode left, TreeNode right) {}`
- Class-based with mutable children
- N-ary: `List<TreeNode> children`
- Trie: `TreeNode[] children = new TreeNode[26]` for lowercase English letters

---

## 1. Binary Tree Traversals

```java
// Recursive traversals
void inorder(TreeNode node) {
    if (node == null) return;
    inorder(node.left);
    visit(node);
    inorder(node.right);
}

void preorder(TreeNode node) {
    if (node == null) return;
    visit(node);
    preorder(node.left);
    preorder(node.right);
}

void postorder(TreeNode node) {
    if (node == null) return;
    postorder(node.left);
    postorder(node.right);
    visit(node);
}

// Iterative traversals using stack
// Level-order using queue (BFS)
```

### Traversal Order Summary

| Traversal   | Order (L = left, N = node, R = right) | Use Case                    |
|-------------|---------------------------------------|-----------------------------|
| Inorder     | L → N → R                             | BST gives sorted sequence   |
| Preorder    | N → L → R                             | Serialize, copy tree        |
| Postorder   | L → R → N                             | Delete tree, compute height |
| Level-order | Level by level (BFS)                  | Level-based operations      |

### Complexity

| Operation           | Time | Space | Notes                          |
|---------------------|------|-------|--------------------------------|
| Recursive traversal | O(n) | O(h)  | h = height, O(n) worst skewed  |
| Iterative traversal | O(n) | O(h)  | Explicit stack                 |
| Level-order (BFS)   | O(n) | O(w)  | w = max width, O(n) worst      |
| Morris traversal    | O(n) | O(1)  | Threaded binary tree, no stack |

### Recursive Traversals

```java
// Inorder traversal (LNR) — BST gives sorted output
public List<Integer> inorderTraversal(TreeNode root) {
    List<Integer> result = new ArrayList<>();
    inorderHelper(root, result);
    return result;
}

private void inorderHelper(TreeNode node, List<Integer> result) {
    if (node == null) return;
    inorderHelper(node.left, result);
    result.add(node.val);
    inorderHelper(node.right, result);
}

// Preorder traversal (NLR) — root first
public List<Integer> preorderTraversal(TreeNode root) {
    List<Integer> result = new ArrayList<>();
    preorderHelper(root, result);
    return result;
}

private void preorderHelper(TreeNode node, List<Integer> result) {
    if (node == null) return;
    result.add(node.val);
    preorderHelper(node.left, result);
    preorderHelper(node.right, result);
}

// Postorder traversal (LRN) — root last
public List<Integer> postorderTraversal(TreeNode root) {
    List<Integer> result = new ArrayList<>();
    postorderHelper(root, result);
    return result;
}

private void postorderHelper(TreeNode node, List<Integer> result) {
    if (node == null) return;
    postorderHelper(node.left, result);
    postorderHelper(node.right, result);
    result.add(node.val);
}
```

### Iterative Traversals (Stack-Based)

```java
// Iterative inorder traversal
public List<Integer> inorderTraversalIterative(TreeNode root) {
    List<Integer> result = new ArrayList<>();
    Deque<TreeNode> stack = new ArrayDeque<>();
    TreeNode current = root;

    while (current != null || !stack.isEmpty()) {
        // Go left as far as possible
        while (current != null) {
            stack.push(current);
            current = current.left;
        }

        // Process node
        current = stack.pop();
        result.add(current.val);

        // Go right
        current = current.right;
    }

    return result;
}

// Iterative preorder traversal
public List<Integer> preorderTraversalIterative(TreeNode root) {
    if (root == null) return List.of();

    List<Integer> result = new ArrayList<>();
    Deque<TreeNode> stack = new ArrayDeque<>();
    stack.push(root);

    while (!stack.isEmpty()) {
        TreeNode node = stack.pop();
        result.add(node.val);

        // Push right first, then left (so left is processed first)
        if (node.right != null) {
            stack.push(node.right);
        }
        if (node.left != null) {
            stack.push(node.left);
        }
    }

    return result;
}

// Iterative postorder traversal (reverse of modified preorder)
public List<Integer> postorderTraversalIterative(TreeNode root) {
    if (root == null) return List.of();

    Deque<TreeNode> stack = new ArrayDeque<>();
    List<Integer> result = new ArrayList<>();

    stack.push(root);
    while (!stack.isEmpty()) {
        TreeNode node = stack.pop();
        result.add(node.val);

        // Push left first, then right
        if (node.left != null) {
            stack.push(node.left);
        }
        if (node.right != null) {
            stack.push(node.right);
        }
    }

    // Reverse to get LNR -> LRN
    Collections.reverse(result);
    return result;
}
```

### Level-Order Traversal (BFS)

```java
// Standard level-order
public List<List<Integer>> levelOrder(TreeNode root) {
    if (root == null) return List.of();

    List<List<Integer>> result = new ArrayList<>();
    Deque<TreeNode> queue = new ArrayDeque<>();
    queue.offer(root);

    while (!queue.isEmpty()) {
        int levelSize = queue.size();
        List<Integer> level = new ArrayList<>();

        for (int i = 0; i < levelSize; i++) {
            TreeNode node = queue.poll();
            level.add(node.val);

            if (node.left != null) queue.offer(node.left);
            if (node.right != null) queue.offer(node.right);
        }

        result.add(level);
    }

    return result;
}

// Zigzag level-order (alternate left-to-right, right-to-left)
public List<List<Integer>> zigzagLevelOrder(TreeNode root) {
    if (root == null) return List.of();

    List<List<Integer>> result = new ArrayList<>();
    Deque<TreeNode> queue = new ArrayDeque<>();
    queue.offer(root);
    boolean leftToRight = true;

    while (!queue.isEmpty()) {
        int levelSize = queue.size();
        List<Integer> level = new ArrayList<>();

        for (int i = 0; i < levelSize; i++) {
            TreeNode node = queue.poll();

            if (leftToRight) {
                level.add(node.val);
            } else {
                level.add(0, node.val);  // Prepend for reverse order
            }

            if (node.left != null) queue.offer(node.left);
            if (node.right != null) queue.offer(node.right);
        }

        result.add(level);
        leftToRight = !leftToRight;  // Toggle direction
    }

    return result;
}
```

---

## 2. BST Operations

```java
// BST node definition
record TreeNode(int val, TreeNode left, TreeNode right) {
}

// Or mutable version
class TreeNode {
    int val;
    TreeNode left;
    TreeNode right;

    TreeNode(int val) {
        this.val = val;
    }
}
```

### BST Property

For every node:

- **All nodes in left subtree < node.val**
- **All nodes in right subtree > node.val**
- **Both subtrees must also be BSTs**

> Critical: The BST property applies to the ENTIRE subtree, not just immediate children. A common mistake is checking
> only `node.left.val < node.val` without enforcing the global constraint.

### Complexity

| Operation         | Average  | Worst Case | Notes                  |
|-------------------|----------|------------|------------------------|
| Search            | O(log n) | O(n)       | O(n) if skewed         |
| Insert            | O(log n) | O(n)       |                        |
| Delete            | O(log n) | O(n)       |                        |
| Inorder successor | O(log n) | O(n)       |                        |
| Validate BST      | O(n)     | O(n)       | Must visit all nodes   |
| kth smallest      | O(h)     | O(n)       | With rank augmentation |

### Search in BST

```java
// Recursive search
public TreeNode searchBST(TreeNode root, int target) {
    if (root == null || root.val == target) {
        return root;
    }

    if (target < root.val) {
        return searchBST(root.left, target);
    } else {
        return searchBST(root.right, target);
    }
}

// Iterative search
public TreeNode searchBSTIterative(TreeNode root, int target) {
    TreeNode current = root;

    while (current != null) {
        if (current.val == target) {
            return current;
        } else if (target < current.val) {
            current = current.left;
        } else {
            current = current.right;
        }
    }

    return null;
}
```

### Insert into BST

```java
public TreeNode insertIntoBST(TreeNode root, int val) {
    if (root == null) {
        return new TreeNode(val);
    }

    if (val < root.val) {
        root.left = insertIntoBST(root.left, val);
    } else if (val > root.val) {
        root.right = insertIntoBST(root.right, val);
    }
    // If val == root.val, do nothing (no duplicates) or handle as needed

    return root;
}
```

### Delete from BST

```java
public TreeNode deleteFromBST(TreeNode root, int key) {
    if (root == null) return null;

    if (key < root.val) {
        root.left = deleteFromBST(root.left, key);
    } else if (key > root.val) {
        root.right = deleteFromBST(root.right, key);
    } else {
        // Found node to delete

        // Case 1: No children
        if (root.left == null && root.right == null) {
            return null;
        }

        // Case 2: One child
        if (root.left == null) {
            return root.right;
        }
        if (root.right == null) {
            return root.left;
        }

        // Case 3: Two children
        // Find inorder successor (smallest in right subtree)
        TreeNode successor = findMin(root.right);
        root.val = successor.val;
        root.right = deleteFromBST(root.right, successor.val);
    }

    return root;
}

private TreeNode findMin(TreeNode node) {
    while (node.left != null) {
        node = node.left;
    }
    return node;
}
```

### Validate BST

```java
// WRONG approach — only checks immediate children
public boolean isValidBSTWrong(TreeNode root) {
    if (root == null) return true;
    if (root.left != null && root.left.val >= root.val) return false;
    if (root.right != null && root.right.val <= root.val) return false;
    return isValidBSTWrong(root.left) && isValidBSTWrong(root.right);
}

// CORRECT approach — track valid range for each node
public boolean isValidBST(TreeNode root) {
    return isValidBSTHelper(root, Long.MIN_VALUE, Long.MAX_VALUE);
}

private boolean isValidBSTHelper(TreeNode node, long min, long max) {
    if (node == null) return true;

    if (node.val <= min || node.val >= max) {
        return false;
    }

    return isValidBSTHelper(node.left, min, node.val)
            && isValidBSTHelper(node.right, node.val, max);
}

// Alternative: Inorder traversal should be strictly increasing
public boolean isValidBSTInorder(TreeNode root) {
    List<Integer> inorder = inorderTraversal(root);
    for (int i = 1; i < inorder.size(); i++) {
        if (inorder.get(i) <= inorder.get(i - 1)) {
            return false;
        }
    }
    return true;
}
```

### Inorder Successor/Predecessor

```java
// Find inorder successor (next node in inorder traversal)
public TreeNode inorderSuccessor(TreeNode root, TreeNode p) {
    TreeNode successor = null;
    TreeNode current = root;

    while (current != null) {
        if (p.val < current.val) {
            successor = current;  // Potential successor
            current = current.left;
        } else {
            current = current.right;
        }
    }

    return successor;
}

// Find inorder predecessor (previous node in inorder traversal)
public TreeNode inorderPredecessor(TreeNode root, TreeNode p) {
    TreeNode predecessor = null;
    TreeNode current = root;

    while (current != null) {
        if (p.val > current.val) {
            predecessor = current;  // Potential predecessor
            current = current.left;
        } else {
            current = current.right;
        }
    }

    return predecessor;
}
```

### Kth Smallest in BST

```java
// Inorder traversal with counter
public int kthSmallest(TreeNode root, int k) {
    List<Integer> inorder = new ArrayList<>();
    collectInorder(root, inorder, k);
    return inorder.get(k - 1);
}

private void collectInorder(TreeNode node, List<Integer> result, int k) {
    if (node == null || result.size() >= k) return;

    collectInorder(node.left, result, k);
    result.add(node.val);
    collectInorder(node.right, result, k);
}

// Optimized: Stop early when kth found
private int kthSmallestOptimized(TreeNode root, int k) {
    return kthSmallestHelper(root, new int[]{k});
}

private int kthSmallestHelper(TreeNode node, int[] counter) {
    if (node == null) return -1;

    // Search left
    int left = kthSmallestHelper(node.left, counter);
    if (left != -1) return left;

    // Visit current
    counter[0]--;
    if (counter[0] == 0) return node.val;

    // Search right
    return kthSmallestHelper(node.right, counter);
}
```

---

## 3. Trie (Prefix Tree)

```java
class TrieNode {
    TrieNode[] children;
    boolean isWord;

    TrieNode() {
        children = new TrieNode[26];  // For lowercase English letters
        isWord = false;
    }
}

class Trie {
    private final TrieNode root;

    public Trie() {
        root = new TrieNode();
    }

    public void insert(String word) {
        TrieNode node = root;
        for (char c : word.toCharArray()) {
            int index = c - 'a';
            if (node.children[index] == null) {
                node.children[index] = new TrieNode();
            }
            node = node.children[index];
        }
        node.isWord = true;
    }

    public boolean search(String word) {
        TrieNode node = findNode(word);
        return node != null && node.isWord;
    }

    public boolean startsWith(String prefix) {
        return findNode(prefix) != null;
    }

    private TrieNode findNode(String s) {
        TrieNode node = root;
        for (char c : s.toCharArray()) {
            int index = c - 'a';
            if (node.children[index] == null) {
                return null;
            }
            node = node.children[index];
        }
        return node;
    }
}
```

### Characteristics

| Property       | Value                                             |
|----------------|---------------------------------------------------|
| **Structure**  | Root + children array (size = alphabet)           |
| **Insert**     | O(L) where L = word length                        |
| **Search**     | O(L)                                              |
| **startsWith** | O(L)                                              |
| **Space**      | O(ALPHABET × N × L) worst case                    |
| **Use Cases**  | Autocomplete, spell check, IP routing, word break |

### When to Use

- **Autocomplete** — return all words with given prefix
- **Word dictionary** — efficient prefix-based lookups
- **Word break problem** — segment string into dictionary words
- **Longest common prefix** — shared path in trie
- **Stream of characters** — check if suffix forms a word

### Magic Methods/Patterns

```java
// Map-based trie for arbitrary characters
class TrieNode {
    Map<Character, TrieNode> children = new HashMap<>();
    boolean isWord = false;
}

// Count occurrences at each node
class TrieNode {
    TrieNode[] children = new TrieNode[26];
    int count = 0;  // Number of words passing through this node
    int wordCount = 0;  // Number of words ending at this node
}

// DFS traversal of trie (collect all words)
public List<String> findAllWords(TrieNode node, String prefix) {
    List<String> result = new ArrayList<>();
    if (node.isWord) {
        result.add(prefix);
    }
    for (int i = 0; i < 26; i++) {
        if (node.children[i] != null) {
            result.addAll(findAllWords(
                    node.children[i],
                    prefix + (char) ('a' + i)
            ));
        }
    }
    return result;
}
```

**Practical Trie with prefix count:**

```java
class TrieWithPrefix {
    private static class TrieNode {
        TrieNode[] children = new TrieNode[26];
        int prefixCount = 0;  // Words passing through
        int wordCount = 0;    // Words ending here
    }

    private final TrieNode root;

    public TrieWithPrefix() {
        root = new TrieNode();
    }

    public void insert(String word) {
        TrieNode node = root;
        for (char c : word.toCharArray()) {
            int index = c - 'a';
            if (node.children[index] == null) {
                node.children[index] = new TrieNode();
            }
            node = node.children[index];
            node.prefixCount++;
        }
        node.wordCount++;
    }

    // Count words with given prefix
    public int countWordsWithPrefix(String prefix) {
        TrieNode node = root;
        for (char c : prefix.toCharArray()) {
            int index = c - 'a';
            if (node.children[index] == null) {
                return 0;
            }
            node = node.children[index];
        }
        return node.prefixCount;
    }

    // Count exact word occurrences
    public int countWord(String word) {
        TrieNode node = findNode(word);
        return node != null ? node.wordCount : 0;
    }

    private TrieNode findNode(String word) {
        TrieNode node = root;
        for (char c : word.toCharArray()) {
            int index = c - 'a';
            if (node.children[index] == null) return null;
            node = node.children[index];
        }
        return node;
    }
}
```

---

## 4. N-ary Tree

```java
class NaryTreeNode {
    String key;
    Object value;
    List<NaryTreeNode> children;

    NaryTreeNode(String key, Object value) {
        this.key = key;
        this.value = value;
        this.children = new ArrayList<>();
    }
}
```

### Characteristics

| Property       | Value                                       |
|----------------|---------------------------------------------|
| **Structure**  | Children as List, not fixed 2               |
| **Traversal**  | Preorder, postorder, level-order            |
| **Use Cases**  | File systems, org charts, DOM, config trees |
| **Space**      | O(n)                                        |
| **Complexity** | O(n) for all traversals                     |

### See Also: platform/challenge09/ConfigNode.java

```java
// From ConfigNode.java — hierarchical configuration tree
public final class ConfigNode {
    private final String key;
    private final Object value;
    private final Map<String, ConfigNode> children;

    public ConfigNode(String key, Object value) {
        this(key, value, Collections.emptyMap());
    }

    public ConfigNode(String key, Object value, Map<String, ConfigNode> children) {
        this.key = Objects.requireNonNull(key);
        this.value = value;
        this.children = children == null ? Collections.emptyMap() :
                Collections.unmodifiableMap(new HashMap<>(children));
    }

    public boolean isLeaf() {
        return value != null && children.isEmpty();
    }

    public boolean isBranch() {
        return !children.isEmpty();
    }
}
```

### N-ary Tree Traversal

```java
// Preorder traversal (root, then children)
public List<Integer> preorder(Node root) {
    List<Integer> result = new ArrayList<>();
    preorderHelper(root, result);
    return result;
}

private void preorderHelper(Node node, List<Integer> result) {
    if (node == null) return;
    result.add(node.val);
    for (Node child : node.children) {
        preorderHelper(child, result);
    }
}

// Postorder traversal (children, then root)
public List<Integer> postorder(Node root) {
    List<Integer> result = new ArrayList<>();
    postorderHelper(root, result);
    return result;
}

private void postorderHelper(Node node, List<Integer> result) {
    if (node == null) return;
    for (Node child : node.children) {
        postorderHelper(child, result);
    }
    result.add(node.val);
}

// Level-order traversal (BFS)
public List<List<Integer>> levelOrder(Node root) {
    if (root == null) return List.of();

    List<List<Integer>> result = new ArrayList<>();
    Deque<Node> queue = new ArrayDeque<>();
    queue.offer(root);

    while (!queue.isEmpty()) {
        int levelSize = queue.size();
        List<Integer> level = new ArrayList<>();

        for (int i = 0; i < levelSize; i++) {
            Node node = queue.poll();
            level.add(node.val);

            for (Node child : node.children) {
                queue.offer(child);
            }
        }

        result.add(level);
    }

    return result;
}
```

### N-ary Tree Serialization

```java
// Serialize N-ary tree to string (preorder with markers)
public String serialize(Node root) {
    StringBuilder sb = new StringBuilder();
    serializeHelper(root, sb);
    return sb.toString();
}

private void serializeHelper(Node node, StringBuilder sb) {
    if (node == null) {
        sb.append("null,");
        return;
    }

    sb.append(node.val).append(",");
    sb.append(node.children.size()).append(",");  // Store child count

    for (Node child : node.children) {
        serializeHelper(child, sb);
    }
}

// Deserialize from string
public Node deserialize(String data) {
    String[] tokens = data.split(",");
    Deque<String> queue = new ArrayDeque<>(Arrays.asList(tokens));
    return deserializeHelper(queue);
}

private Node deserializeHelper(Deque<String> queue) {
    String token = queue.pollFirst();
    if (token.equals("null")) return null;

    Node node = new Node(Integer.parseInt(token), new ArrayList<>());
    int childCount = Integer.parseInt(queue.pollFirst());

    for (int i = 0; i < childCount; i++) {
        node.children.add(deserializeHelper(queue));
    }

    return node;
}
```

---

## 5. Balanced Trees (AVL / Red-Black)

### AVL Tree Properties

- **Self-balancing BST** with strict height balance
- **Balance factor** = height(left) - height(right)
- **|Balance factor| ≤ 1** for every node
- **Rotations**: LL, RR, LR, RL to restore balance
- **Complexity**: O(log n) for search/insert/delete

### Red-Black Tree Properties

- **BST with color attribute** (red or black) per node
- **Root is always black**
- **Red nodes cannot be adjacent** (red parent → black children)
- **Every path from root to null has same number of black nodes**
- **Java TreeMap/TreeSet** use Red-Black trees internally
- **Complexity**: O(log n) for all operations

### When to Use

- **Use TreeMap/TreeSet** — Java's Red-Black implementation is production-ready
- **Custom AVL implementation** — only if interview explicitly requires it
- **Know the concepts** — understand why balancing is needed, but rarely implement from scratch

```java
// Java's Red-Black tree via TreeMap
TreeMap<Integer, String> sortedMap = new TreeMap<>();
sortedMap.

put(5,"e");
sortedMap.

put(1,"a");
sortedMap.

put(3,"c");

// O(log n) operations with guaranteed balance
sortedMap.

floorKey(4);    // 3
sortedMap.

ceilingKey(2);  // 3
sortedMap.

subMap(2,4);   // {2=..., 3=...}
```

---

## Pattern Reference: Common Tree Problems

### Max Depth / Height

```java
public int maxDepth(TreeNode root) {
    if (root == null) return 0;
    return 1 + Math.max(maxDepth(root.left), maxDepth(root.right));
}
```

### Balanced Check

```java
public boolean isBalanced(TreeNode root) {
    return checkHeight(root) != -1;
}

private int checkHeight(TreeNode node) {
    if (node == null) return 0;

    int left = checkHeight(node.left);
    if (left == -1) return -1;

    int right = checkHeight(node.right);
    if (right == -1) return -1;

    if (Math.abs(left - right) > 1) return -1;

    return 1 + Math.max(left, right);
}
```

### Path Sum

```java
// Root-to-leaf path with given sum
public boolean hasPathSum(TreeNode root, int targetSum) {
    if (root == null) return false;

    if (root.left == null && root.right == null) {
        return root.val == targetSum;
    }

    return hasPathSum(root.left, targetSum - root.val)
            || hasPathSum(root.right, targetSum - root.val);
}

// All paths with given sum
public List<List<Integer>> pathSum(TreeNode root, int targetSum) {
    List<List<Integer>> result = new ArrayList<>();
    pathSumHelper(root, targetSum, new ArrayList<>(), result);
    return result;
}

private void pathSumHelper(TreeNode node, int remaining, List<Integer> path, List<List<Integer>> result) {
    if (node == null) return;

    path.add(node.val);

    if (node.left == null && node.right == null && remaining == node.val) {
        result.add(new ArrayList<>(path));
    } else {
        pathSumHelper(node.left, remaining - node.val, path, result);
        pathSumHelper(node.right, remaining - node.val, path, result);
    }

    path.removeLast();  // Backtrack (Java 21)
}
```

### Lowest Common Ancestor (LCA)

```java
// LCA in binary tree (not necessarily BST)
public TreeNode lowestCommonAncestor(TreeNode root, TreeNode p, TreeNode q) {
    if (root == null || root == p || root == q) {
        return root;
    }

    TreeNode left = lowestCommonAncestor(root.left, p, q);
    TreeNode right = lowestCommonAncestor(root.right, p, q);

    if (left != null && right != null) {
        return root;  // p and q found in different subtrees
    }

    return left != null ? left : right;
}

// LCA in BST (use BST property)
public TreeNode lowestCommonAncestorBST(TreeNode root, TreeNode p, TreeNode q) {
    TreeNode current = root;

    while (current != null) {
        if (p.val < current.val && q.val < current.val) {
            current = current.left;
        } else if (p.val > current.val && q.val > current.val) {
            current = current.right;
        } else {
            return current;  // Split point found
        }
    }

    return null;
}
```

### Serialize/Deserialize Binary Tree

```java
// Serialize using preorder with null markers
public String serialize(TreeNode root) {
    StringBuilder sb = new StringBuilder();
    serializeHelper(root, sb);
    return sb.toString();
}

private void serializeHelper(TreeNode node, StringBuilder sb) {
    if (node == null) {
        sb.append("#,");
        return;
    }

    sb.append(node.val).append(",");
    serializeHelper(node.left, sb);
    serializeHelper(node.right, sb);
}

// Deserialize
public TreeNode deserialize(String data) {
    Deque<String> queue = new ArrayDeque<>(Arrays.asList(data.split(",")));
    return deserializeHelper(queue);
}

private TreeNode deserializeHelper(Deque<String> queue) {
    String token = queue.pollFirst();
    if (token.equals("#")) return null;

    TreeNode node = new TreeNode(Integer.parseInt(token));
    node.left = deserializeHelper(queue);
    node.right = deserializeHelper(queue);

    return node;
}
```

---

## Java 21 Features: Record-Based Tree Nodes

### Record for Immutable Nodes

```java
// Immutable binary tree node
public record TreeNode(int val, TreeNode left, TreeNode right) {
}

// Usage
TreeNode leaf = new TreeNode(5, null, null);
TreeNode node = new TreeNode(3, leaf, null);

// Pattern matching with instanceof
void process(TreeNode node) {
    if (node instanceof TreeNode(int val, TreeNode left, TreeNode right)) {
        System.out.println("Value: " + val);
        // left and right are automatically typed
    }
}
```

### Pattern Matching for instanceof

```java
// Before Java 21
if(obj instanceof TreeNode){
TreeNode node = (TreeNode) obj;

process(node.val);
}

// Java 21 — pattern matching
        if(obj instanceof
TreeNode node){

process(node.val);  // No explicit cast needed
}

// Deconstructing pattern (Java 21 preview)
        if(obj instanceof

TreeNode(int val, _, _)){

process(val);  // Extract only val, ignore left/right with _
}
```

### Sealed Hierarchies for Tree Types

```java
// Sealed class hierarchy (Java 17+)
public sealed interface TreeNode
        permits LeafNode, InternalNode {
}

public record LeafNode(int value) implements TreeNode {
}

public record InternalNode(TreeNode left, TreeNode right) implements TreeNode {
}

// Exhaustive switch (compiler verifies all cases covered)
int sum(TreeNode node) {
    return switch (node) {
        case LeafNode leaf -> leaf.value();
        case InternalNode internal -> sum(internal.left()) + sum(internal.right());
    };
}
```

---

## Common Gotchas

1. **Recursive space is O(h), not O(1)** — Even without explicit data structures, recursion uses stack space. For skewed
   trees (linked list shape), O(h) = O(n). State this explicitly in interviews.

2. **BST validation is NOT just checking immediate children** — The entire left subtree must be < node.val, and entire
   right subtree > node.val. Passing `min`/`max` bounds down the recursion is essential.

   ```java
   // WRONG: only checks direct children
   boolean isValid = (left.val < node.val) && (right.val > node.val);
   
   // CORRECT: validate entire subtrees within bounds
   boolean isValid = isValidBST(node.left, min, node.val) 
                  && isValidBST(node.right, node.val, max);
   ```

3. **Null checks at the RIGHT time** — Check for null before accessing `node.val`, `node.left`, etc. In recursive
   functions, base case is typically `if (node == null) return ...;`

4. **Trie memory overhead** — A trie with N words of length L uses O(ALPHABET × N × L) space worst case. For sparse
   tries (few actual words), most `children[i]` entries are null. Consider Map-based children for large alphabets (
   Unicode).

5. **Iterative vs recursive trade-offs** — Recursive solutions are cleaner but risk stack overflow on deep trees (>
   10,000 nodes). Iterative solutions with explicit stack are safer for production code.

6. **Level-order needs queue, not stack** — BFS (level-order) uses queue (FIFO). DFS (preorder/inorder/postorder) uses
   stack (LIFO) or recursion. Mixing them up causes ordering bugs.

7. **Tree serialization must include null markers** — Without explicit null markers (like `#`), you cannot distinguish
   between different tree structures that produce the same non-null sequence.

8. **Modifying tree during traversal** — Never modify subtree structure while iterating over it. Collect nodes first,
   then modify, or use post-order for bottom-up modifications.

---

## See Also

- **GRAPH_GUIDE.md** — Tree is a special case of graph (acyclic connected). DFS/BFS patterns apply to both; tree has
  parent-child semantics and single root.

- **HEAP_GUIDE.md** — Binary heap is a complete binary tree with heap property (parent ≤ children for min-heap).
  Contrasts with BST property.

- **MAP_GUIDE.md** — `TreeMap` uses Red-Black tree internally; provides sorted key operations (`floorKey`, `ceilingKey`)
  with O(log n) guarantees.

- **platform/challenge09/ConfigNode.java** — N-ary tree implementation for hierarchical configuration merging;
  `Map<String, ConfigNode>` for children, leaf vs branch distinction.

- **platform/challenge09/ConfigMergerImpl.java** — Tree traversal and merging patterns; priority-based override at leaf
  level.

- **development/recursion/Fibonacci.java** — Recursion and memoization patterns; tree recursion structure visible in
  call graph.

---

## Complexity Summary

| Operation                     | Time     | Space | Notes                         |
|-------------------------------|----------|-------|-------------------------------|
| Binary tree traversal         | O(n)     | O(h)  | h = height, O(log n) balanced |
| BST search/insert/delete      | O(log n) | O(h)  | O(n) worst (skewed)           |
| Validate BST                  | O(n)     | O(h)  | Must visit all nodes          |
| Trie insert/search/startsWith | O(L)     | O(1)  | L = word length               |
| N-ary tree traversal          | O(n)     | O(h)  | Same as binary tree           |
| LCA in binary tree            | O(n)     | O(h)  | Single pass                   |
| LCA in BST                    | O(h)     | O(1)  | Use BST property              |
| Serialize/deserialize         | O(n)     | O(n)  | O(n) for string/array         |
| AVL/Red-Black operations      | O(log n) | O(1)  | Guaranteed balance            |

---

## Performance Summary

| Tree Type   | Build      | Search    | Insert    | Delete    | Space               |
|-------------|------------|-----------|-----------|-----------|---------------------|
| Binary Tree | O(n)       | O(n)      | O(n)      | O(n)      | O(n)                |
| BST         | O(n)       | O(log n)* | O(log n)* | O(log n)* | O(n)                |
| AVL         | O(n log n) | O(log n)  | O(log n)  | O(log n)  | O(n)                |
| Red-Black   | O(n log n) | O(log n)  | O(log n)  | O(log n)  | O(n)                |
| Trie        | O(N × L)   | O(L)      | O(L)      | O(L)      | O(ALPHABET × N × L) |
| N-ary Tree  | O(n)       | O(n)      | O(1)**    | O(n)**    | O(n)                |

\* Average case; worst case O(n) if skewed
\*\* Assuming position known; O(n) to find position

> **Note**: Java's `TreeMap` and `TreeSet` provide production-ready Red-Black tree implementations with O(log n)
> guarantees.

(End of file - total ~780 lines)
