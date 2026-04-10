package com.danipl.platform.challenge09;

import java.util.Optional;

/**
 * Hierarchical Configuration Merger.
 *
 * Merges configuration trees from multiple sources with priority-based override.
 * Higher priority sources override lower priority sources.
 */
public interface ConfigMerger {

    /**
     * Adds a configuration source with the given priority.
     *
     * @param name     a descriptive name for the source
     * @param source   the configuration tree to add
     * @param priority higher number = higher priority (overrides lower priority sources)
     */
    void addSource(String name, ConfigNode source, int priority);

    /**
     * Merges all added sources into a single configuration tree.
     * Higher priority sources override lower priority at the leaf level.
     *
     * @return the merged configuration tree
     * @throws IllegalStateException if no sources have been added
     */
    ConfigNode merge();

    /**
     * Resolves a value using dot-separated path notation.
     *
     * @param dotPath e.g. "database.host" or "database.connection.host"
     * @return the value at the path, or empty if not found
     */
    Optional<Object> resolve(String dotPath);

    /**
     * Creates a new ConfigMerger.
     *
     * @return a new ConfigMerger instance
     */
    static ConfigMerger of() {
        return new ConfigMergerImpl();
    }
}
