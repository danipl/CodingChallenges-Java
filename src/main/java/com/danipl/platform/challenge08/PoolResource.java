package com.danipl.platform.challenge08;

/**
 * A simple pooled resource with health tracking.
 */
public final class PoolResource implements AutoCloseable, ResourcePool.Healthy {

    public record ResourceId(int id) { }

    private final ResourceId id;
    private volatile boolean healthy = true;

    public PoolResource(int id) {
        this.id = new ResourceId(id);
    }

    public ResourceId getId() {
        return id;
    }

    public boolean isHealthy() {
        return healthy;
    }

    public void markInvalid() {
        this.healthy = false;
    }

    @Override
    public void close() {
        markInvalid();
    }

    @Override
    public String toString() {
        return "PoolResource{id=%d, healthy=%s}".formatted(id.id(), healthy);
    }
}
