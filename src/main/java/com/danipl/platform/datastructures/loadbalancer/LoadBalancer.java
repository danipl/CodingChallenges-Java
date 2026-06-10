package com.danipl.platform.datastructures.loadbalancer;

import java.util.NoSuchElementException;

public interface LoadBalancer {

    static LoadBalancer of() {
        return new LoadBalancerImpl();
    }

    void add(Server server);

    void remove(String host, int port);

    Server next() throws NoSuchElementException;
}
