package com.danipl.platform.challenge02;

import java.util.NoSuchElementException;

public class LoadBalancerImpl implements LoadBalancer {

    @Override
    public void add(Server server) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void remove(String host, int port) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Server next() throws NoSuchElementException {
        throw new UnsupportedOperationException("Not implemented");
    }
}
