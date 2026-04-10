package com.danipl.platform.challenge09;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A tree node representing a configuration value or a branch of the config tree.
 * Leaf nodes have a non-null value and empty children.
 * Branch nodes have children and a null value.
 */
public final class ConfigNode {

    private final String key;
    private final Object value;
    private final Map<String, ConfigNode> children;

    /**
     * Creates a leaf node with a value.
     */
    public ConfigNode(String key, Object value) {
        this(key, value, Collections.emptyMap());
    }

    /**
     * Creates a branch node with children.
     */
    public ConfigNode(String key, Object value, Map<String, ConfigNode> children) {
        this.key = Objects.requireNonNull(key);
        this.value = value;
        this.children = children == null ? Collections.emptyMap() :
            Collections.unmodifiableMap(new HashMap<>(children));
    }

    /**
     * Returns the key of this node.
     */
    public String getKey() {
        return key;
    }

    /**
     * Returns the value. Null for branch nodes.
     */
    public Object getValue() {
        return value;
    }

    /**
     * Returns child nodes. Empty for leaf nodes.
     */
    public Map<String, ConfigNode> getChildren() {
        return children;
    }

    /**
     * Returns true if this node is a leaf (has a value, no children).
     */
    public boolean isLeaf() {
        return value != null && children.isEmpty();
    }

    /**
     * Returns true if this node is a branch (has children).
     */
    public boolean isBranch() {
        return !children.isEmpty();
    }

    /**
     * Creates a ConfigNode with an empty key and the given value.
     */
    public static ConfigNode of(Object value) {
        return new ConfigNode("", value);
    }

    /**
     * Creates a ConfigNode with children.
     */
    public static ConfigNode ofChildren(Map<String, ConfigNode> children) {
        return new ConfigNode("", null, children);
    }

    @Override
    public String toString() {
        if (isLeaf()) {
            return "ConfigNode{key='%s', value=%s}".formatted(key, value);
        }
        return "ConfigNode{key='%s', children=%s}".formatted(key, children.keySet());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConfigNode that)) return false;
        return Objects.equals(key, that.key) &&
            Objects.equals(value, that.value) &&
            Objects.equals(children, that.children);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value, children);
    }
}
