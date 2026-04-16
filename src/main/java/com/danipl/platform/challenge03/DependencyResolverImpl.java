package com.danipl.platform.challenge03;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DependencyResolverImpl implements DependencyResolver {

    private final LinkedHashMap<String, List<String>> libraryMap = new LinkedHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
    private final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();

    @Override
    public void add(final String library, final List<String> dependencies) {
        try {
            writeLock.lock();
            final var currentDeps = libraryMap.computeIfAbsent(library, k -> new ArrayList<>());
            for (final var dep : dependencies) {
                if (!currentDeps.contains(dep)) {
                    currentDeps.add(dep);
                }
            }
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public List<String> resolveBuildOrder() throws CircularDependencyException {
        LinkedHashMap<String, List<String>> snapshot;
        try {
            readLock.lock();
            if (libraryMap.isEmpty()) {
                return Collections.emptyList();
            }
            snapshot = new LinkedHashMap<>(libraryMap);
        } finally {
            readLock.unlock();
        }
        final var weights = new HashMap<String, Integer>();
        final var deps = new LinkedHashMap<String, List<String>>();
        for (final var entry : snapshot.entrySet()) {
            final var library = entry.getKey();
            weights.putIfAbsent(library, 0);
            for (final var dep : entry.getValue()) {
                weights.merge(library, 1, Integer::sum);
                weights.putIfAbsent(dep, 0);
                deps.computeIfAbsent(dep, k -> new ArrayList<>()).add(library);
            }
        }
        final var queue = new ArrayDeque<String>();
        for (final var entry : weights.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }
        final var ordered = new ArrayList<String>(weights.size());
        while (!queue.isEmpty()) {
            final var library = queue.poll();
            ordered.add(library);
            for (final var dep : deps.getOrDefault(library, List.of())) {
                if (weights.merge(dep, -1, Integer::sum) == 0) {
                    queue.add(dep);
                }
            }
        }
        if (ordered.size() != weights.size()) {
            throw new CircularDependencyException("It has a circular dependency");
        }
        return ordered;
    }

    private enum State {
        NEW, VISITING, VISITED
    }

    @Override
    public boolean hasCircularDependency() {
        final LinkedHashMap<String, List<String>> snapshot;
        try {
            readLock.lock();
            snapshot = new LinkedHashMap<>(libraryMap);
        } finally {
            readLock.unlock();
        }
        final var visitState = new HashMap<String, State>();
        return snapshot.keySet().stream().anyMatch(library -> hasCircularDependency(snapshot, library, visitState));
    }

    public boolean hasCircularDependency(
            final LinkedHashMap<String, List<String>> snapshot, final String library, final Map<String, State> visitState
    ) {
        final var state = visitState.getOrDefault(library, State.NEW);
        if (state == State.VISITED) {
            return false;
        } else if (state == State.VISITING) {
            return true;
        }
        visitState.put(library, State.VISITING);
        for (final var dependency : snapshot.getOrDefault(library, List.of())) {
            if (hasCircularDependency(snapshot, dependency, visitState)) {
                return true;
            }
        }
        visitState.put(library, State.VISITED);
        return false;
    }

}
