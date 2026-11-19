package com.diskree.achievetodo.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/** Main-thread lifecycle ownership only; this is not gameplay or bridge admission. */
public final class ServerRuntimeReadiness {

    public enum State { NEW, STARTING, RUNNING, STOPPING, STOPPED, FAILED }

    @FunctionalInterface
    public interface StartAction {
        void run() throws Exception;
    }

    public record Snapshot(State state, List<String> retainedServices) {
        public Snapshot {
            retainedServices = List.copyOf(retainedServices);
        }
    }

    public record CleanupFailure(String service, Throwable cause) {
    }

    private record Service(String id, AutoCloseable cleanup) {
    }

    private final Deque<Service> services = new ArrayDeque<>();
    private State state = State.NEW;

    public Snapshot snapshot() {
        return new Snapshot(state, services.stream().map(Service::id).toList());
    }

    public void beginStart() {
        if (state != State.NEW && state != State.STOPPED) {
            throw new IllegalStateException("Cannot start lifecycle from " + state);
        }
        state = State.STARTING;
    }

    public void startService(String id, StartAction start, AutoCloseable cleanup) throws Exception {
        if (state != State.STARTING) {
            throw new IllegalStateException("Services can only start during STARTING");
        }
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Service id must not be blank");
        }
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(cleanup, "cleanup");
        if (services.stream().anyMatch(service -> service.id().equals(id))) {
            throw new IllegalArgumentException("Duplicate lifecycle service: " + id);
        }
        // Own cleanup before start, including a service which starts only partially.
        services.push(new Service(id, cleanup));
        try {
            start.run();
        } catch (Exception | LinkageError failure) {
            state = State.FAILED;
            throw failure;
        }
    }

    public void markRunning() {
        if (state != State.STARTING) {
            throw new IllegalStateException("Cannot mark lifecycle running from " + state);
        }
        state = State.RUNNING;
    }

    public void fail() {
        state = State.FAILED;
    }

    /** Attempts every cleanup once in reverse acquisition order, even after failure. */
    public List<CleanupFailure> stop() {
        if (state == State.STOPPING) {
            return List.of();
        }
        boolean failed = state == State.FAILED;
        state = State.STOPPING;
        List<CleanupFailure> failures = new ArrayList<>();
        while (!services.isEmpty()) {
            Service service = services.pop();
            try {
                service.cleanup().close();
            } catch (Exception | LinkageError failure) {
                failures.add(new CleanupFailure(service.id(), failure));
            }
        }
        state = failed || !failures.isEmpty() ? State.FAILED : State.STOPPED;
        return List.copyOf(failures);
    }
}
