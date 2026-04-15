package com.danipl.platform.challenge02;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import static java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

public class LoadBalancerImpl implements LoadBalancer {

    private record ServerKey(String host, int port) {
    }

    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final ReadLock readLock = rwLock.readLock();
    private final WriteLock writeLock = rwLock.writeLock();
    private final Map<ServerKey, Server> serverMap = new HashMap<>();

    private int totalWeight = 0;

    @Override
    public void add(final Server server) {
        if (server.weight() < 1) {
            throw new IllegalArgumentException("Weight must be more than 0");
        }
        final var key = resolveKey(server.host(), server.port());
        try {
            writeLock.lock();
            serverMap.compute(key, (k, s) -> (s != null)
                    ? new Server(s.host(), s.port(), (s.weight() + server.weight()))
                    : server
            );
            totalWeight += server.weight();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void remove(final String host, final int port) {
        final var key = resolveKey(host, port);
        try {
            writeLock.lock();
            final var removed = serverMap.remove(key);
            if (removed != null) {
                totalWeight -= removed.weight();
            }
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public Server next() throws NoSuchElementException {
        try {
            readLock.lock();
            final var servers = serverMap.values();
            if (servers.isEmpty()) {
                throw new NoSuchElementException("Servers are empty");
            }
            // Weighted random selection: Math.random() generates [0.0, 1.0), scaled to [0.0, totalWeight).
            // Truncating to int produces a value in [0, totalWeight - 1], which is used to select
            final var position = (int) (Math.random() * totalWeight);
            int index = 0;
            for (final var server : servers) {
                index += server.weight();
                if (position < index) {
                    return server;
                }
            }
            // Fallback should not reach - defensive programming
            return servers.iterator().next();
        } finally {
            readLock.unlock();
        }
    }

    private ServerKey resolveKey(final String host, final int port) {
        return new ServerKey(host, port);
    }

}
