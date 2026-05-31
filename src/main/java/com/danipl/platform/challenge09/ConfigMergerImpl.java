package com.danipl.platform.challenge09;

import java.util.*;

import static java.util.Optional.ofNullable;

/**
 * ConfigMerger implementation that merges configuration trees
 * from multiple sources with priority-based override.
 */
public final class ConfigMergerImpl implements ConfigMerger {

    private boolean isDirty = false;

    private final Map<String, Object> flatMapping = new HashMap<>();
    private final Map<String, Integer> priorities = new HashMap<>();

    private ConfigNode cached;

    @Override
    public void addSource(String name, ConfigNode source, int priority) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(source);
        if (source.isLeaf() && exchangeable(source.getKey(), priority)) {
            flatMapping.put(source.getKey(), source.getValue());
            priorities.put(source.getKey(), priority);
        }
        if (source.isBranch()) {
            boolean prefixWithSourceKey = flatMapping.containsKey(source.getKey());
            source.getChildren().forEach((key, value) -> {
                LinkedList<String> path = new LinkedList<>();
                if (prefixWithSourceKey) {
                    path.add(source.getKey());
                }
                path.add(key);
                addToFlatMapping(path, value, priority);
            });
        }
        isDirty = true;
    }

    private void addToFlatMapping(LinkedList<String> path, ConfigNode source, int priority) {
        final var plainPath = plainPath(path);
        if (source.isLeaf() && exchangeable(plainPath, priority)) {
            flatMapping.put(plainPath, source.getValue());
            priorities.put(plainPath, priority);
        }
        if (source.isBranch()) {
            source.getChildren().forEach((key, value) -> {
                final LinkedList<String> subPath = new LinkedList<>(path);
                subPath.add(key);
                addToFlatMapping(subPath, value, priority);
            });
        }
    }

    @Override
    public ConfigNode merge() {
        if (flatMapping.isEmpty()) {
            throw new IllegalStateException("No resources");
        }
        if (isDirty) {
            cached = new ConfigNode("root", null, buildChildren(new TreeMap<>(flatMapping)));
            isDirty = false;
        }
        return cached;
    }

    private Map<String, ConfigNode> buildChildren(Map<String, Object> entries) {
        Map<String, ConfigNode> children = new HashMap<>();
        Map<String, Map<String, Object>> nested = new HashMap<>();

        for (Map.Entry<String, Object> entry : entries.entrySet()) {
            String path = entry.getKey();
            int dot = path.indexOf('.');
            if (dot == -1) {
                children.put(path, new ConfigNode(path, entry.getValue()));
            } else {
                String first = path.substring(0, dot);
                String rest = path.substring(dot + 1);
                nested.computeIfAbsent(first, k -> new HashMap<>()).put(rest, entry.getValue());
            }
        }

        for (Map.Entry<String, Map<String, Object>> n : nested.entrySet()) {
            children.put(n.getKey(), new ConfigNode(n.getKey(), null, buildChildren(n.getValue())));
        }

        return children;
    }

    @Override
    public Optional<Object> resolve(String dotPath) {
        return ofNullable(flatMapping.get(dotPath));
    }

    private boolean exchangeable(final String plainPath, final int priority) {
        final Integer currentPriority = priorities.get(plainPath);
        return currentPriority == null || priority > currentPriority;
    }

    private String plainPath(final LinkedList<String> path) {
        return String.join(".", path);
    }

}
