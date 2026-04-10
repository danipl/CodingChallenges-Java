package com.danipl.platform.challenge03;

import java.util.List;

public class DependencyResolverImpl implements DependencyResolver {

    @Override
    public void add(String library, List<String> dependencies) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public List<String> resolveBuildOrder() throws CircularDependencyException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public boolean hasCircularDependency() {
        throw new UnsupportedOperationException("Not implemented");
    }
}
