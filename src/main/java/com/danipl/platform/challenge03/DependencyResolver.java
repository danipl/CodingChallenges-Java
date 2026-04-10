package com.danipl.platform.challenge03;

import java.util.List;

public interface DependencyResolver {

    static DependencyResolver of() {
        return new DependencyResolverImpl();
    }

    void add(String library, List<String> dependencies);

    List<String> resolveBuildOrder() throws CircularDependencyException;

    boolean hasCircularDependency();
}
