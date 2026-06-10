package com.danipl.platform.datastructures.loadbalancer;

public record Server(String host, int port, int weight) {}
