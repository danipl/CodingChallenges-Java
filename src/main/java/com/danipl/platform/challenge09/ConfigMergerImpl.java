package com.danipl.platform.challenge09;

import java.util.Optional;

/**
 * Skeleton implementation of ConfigMerger.
 * All methods throw UnsupportedOperationException.
 */
public final class ConfigMergerImpl implements ConfigMerger {

    public ConfigMergerImpl() {
        // TODO: initialize internal storage
    }

    @Override
    public void addSource(String name, ConfigNode source, int priority) {
        // TODO: implement source registration with priority
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public ConfigNode merge() {
        // TODO: implement merge logic with priority-based override
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Optional<Object> resolve(String dotPath) {
        // TODO: implement dot-path resolution on merged tree
        throw new UnsupportedOperationException("Not implemented");
    }
}
