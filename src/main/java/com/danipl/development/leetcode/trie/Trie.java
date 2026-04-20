package com.danipl.development.leetcode.trie;

/**
 * Trie (Prefix Tree) - efficient string prefix search and autocomplete.
 * <p>
 * A trie is a tree-like data structure where each node represents a character,
 * and paths from root to nodes represent string prefixes.
 * </p>
 * <p>
 * Time Complexity:
 * - insert: O(L) where L = word length
 * - search: O(L) where L = word length
 * - startsWith: O(L) where L = prefix length
 * </p>
 * <p>
 * Space Complexity: O(ALPHABET × N × L) worst case, where N = number of words
 * </p>
 * <p>
 * Use cases: autocompletion, spell checkers, IP routing, word dictionaries
 * </p>
 *
 * @see com.danipl.TREE_GUIDE.md#3-trie-prefix-tree
 */
public class Trie {

    /**
     * Trie node containing children array and word-end marker.
     * Uses fixed array of size 26 for lowercase English letters (a-z).
     */
    private static class TrieNode {
        TrieNode[] children;
        boolean isWord;

        TrieNode() {
            children = new TrieNode[26];
            isWord = false;
        }
    }

    private final TrieNode root;

    /**
     * Constructs an empty Trie.
     */
    public Trie() {
        root = new TrieNode();
    }

    /**
     * Inserts a word into the trie.
     * <p>
     * Creates new nodes for characters not yet in the trie,
     * and marks the final node as a word endpoint.
     * </p>
     *
     * @param word the word to insert (lowercase English letters only)
     * @return void
     * @apiNote Time: O(L), Space: O(L) additional in worst case
     */
    public void insert(String word) {
        TrieNode node = root;
        for (char c : word.toCharArray()) {
            int index = c - 'a';
            // Create child node if not exists
            if (node.children[index] == null) {
                node.children[index] = new TrieNode();
            }
            node = node.children[index];
        }
        // Mark end of word
        node.isWord = true;
    }

    /**
     * Searches if a word exists in the trie.
     * <p>
     * Returns true only if the exact word was previously inserted
     * (not just a prefix of another word).
     * </p>
     *
     * @param word the word to search for
     * @return true if word exists, false otherwise
     * @apiNote Time: O(L), Space: O(1)
     */
    public boolean search(String word) {
        TrieNode node = findNode(word);
        // Must be a complete word, not just a prefix path
        return node != null && node.isWord;
    }

    /**
     * Checks if any word in the trie starts with the given prefix.
     * <p>
     * Returns true if there exists any word that has this prefix.
     * </p>
     *
     * @param prefix the prefix to check
     * @return true if any word starts with prefix, false otherwise
     * @apiNote Time: O(L), Space: O(1)
     * @implNote Used for autocomplete functionality
     */
    public boolean startsWith(String prefix) {
        return findNode(prefix) != null;
    }

    /**
     * Helper method to find the node corresponding to end of string.
     * <p>
     * Traverses the trie following the character path.
     * Returns null if path does not exist.
     * </p>
     *
     * @param s the string to navigate to
     * @return the TrieNode at end of string, or null if path broken
     */
    private TrieNode findNode(String s) {
        TrieNode node = root;
        for (char c : s.toCharArray()) {
            int index = c - 'a';
            // Path does not exist
            if (node.children[index] == null) {
                return null;
            }
            node = node.children[index];
        }
        return node;
    }
}
