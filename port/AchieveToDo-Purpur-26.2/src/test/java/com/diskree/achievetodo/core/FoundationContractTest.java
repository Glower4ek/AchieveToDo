package com.diskree.achievetodo.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class FoundationContractTest {

    private FoundationContractTest() {
    }

    public static void main(String[] args) throws Exception {
        reverseDependencyCleanupAndImmutableSnapshot();
        partialStartAtEveryServicePosition();
        cleanupFailureDoesNotSkipOtherOwners();
        linkageFailureReleasesPartialService();
        duplicateOwnerCannotReplaceCleanup();
        cleanStopCanStartANewLifecycle();
        reentrantStopDoesNotCloseTwice();
        readinessCannotBePublishedAfterFailure();
        System.out.println("Foundation contract tests passed: 8 scenarios (4 partial-start positions).");
    }

    private static void reverseDependencyCleanupAndImmutableSnapshot() throws Exception {
        ServerRuntimeReadiness runtime = new ServerRuntimeReadiness();
        List<String> events = new ArrayList<>();
        equal(ServerRuntimeReadiness.State.NEW, runtime.snapshot().state());
        runtime.beginStart();
        runtime.startService("storage", () -> events.add("open storage"), () -> events.add("close storage"));
        runtime.startService("consumer", () -> events.add("open consumer"), () -> events.add("close consumer"));
        runtime.markRunning();
        ServerRuntimeReadiness.Snapshot published = runtime.snapshot();
        equal(ServerRuntimeReadiness.State.RUNNING, published.state());
        expect(UnsupportedOperationException.class, () -> published.retainedServices().clear());
        equal(List.of(), runtime.stop());
        equal(List.of("open storage", "open consumer", "close consumer", "close storage"), events);
        equal(ServerRuntimeReadiness.State.STOPPED, runtime.snapshot().state());
        equal(List.of(), runtime.snapshot().retainedServices());
        equal(2, published.retainedServices().size());
    }

    private static void partialStartAtEveryServicePosition() throws Exception {
        for (int failAt = 0; failAt < 4; failAt++) {
            ServerRuntimeReadiness runtime = new ServerRuntimeReadiness();
            List<Integer> closes = new ArrayList<>();
            List<Integer> started = new ArrayList<>();
            runtime.beginStart();
            final int failurePosition = failAt;
            expect(IOException.class, () -> {
                for (int index = 0; index < 4; index++) {
                    final int id = index;
                    runtime.startService("service" + id, () -> {
                        started.add(id);
                        if (id == failurePosition) throw new IOException("injected partial start");
                    }, () -> closes.add(id));
                }
            });
            equal(ServerRuntimeReadiness.State.FAILED, runtime.snapshot().state());
            equal(List.of(), runtime.stop());
            List<Integer> expected = new ArrayList<>();
            for (int index = failAt; index >= 0; index--) expected.add(index);
            equal(expected, closes);
            equal(failAt + 1, started.size());
            equal(List.of(), runtime.stop());
            equal(expected, closes);
            equal(List.of(), runtime.snapshot().retainedServices());
        }
    }

    private static void cleanupFailureDoesNotSkipOtherOwners() throws Exception {
        ServerRuntimeReadiness runtime = new ServerRuntimeReadiness();
        List<String> closed = new ArrayList<>();
        runtime.beginStart();
        runtime.startService("storage", () -> {}, () -> closed.add("storage"));
        runtime.startService("consumer", () -> {}, () -> {
            closed.add("consumer");
            throw new IOException("injected close failure");
        });
        runtime.markRunning();
        List<ServerRuntimeReadiness.CleanupFailure> failures = runtime.stop();
        equal(List.of("consumer", "storage"), closed);
        equal(1, failures.size());
        equal("consumer", failures.getFirst().service());
        equal(IOException.class, failures.getFirst().cause().getClass());
        equal(ServerRuntimeReadiness.State.FAILED, runtime.snapshot().state());
        equal(List.of(), runtime.snapshot().retainedServices());
        equal(List.of(), runtime.stop());
        equal(List.of("consumer", "storage"), closed);
    }

    private static void linkageFailureReleasesPartialService() throws Exception {
        ServerRuntimeReadiness runtime = new ServerRuntimeReadiness();
        List<String> closed = new ArrayList<>();
        runtime.beginStart();
        expect(NoSuchMethodError.class, () -> runtime.startService("adapter", () -> {
            throw new NoSuchMethodError("simulated platform mismatch");
        }, () -> closed.add("adapter")));
        equal(ServerRuntimeReadiness.State.FAILED, runtime.snapshot().state());
        runtime.stop();
        equal(List.of("adapter"), closed);
    }

    private static void duplicateOwnerCannotReplaceCleanup() throws Exception {
        ServerRuntimeReadiness runtime = new ServerRuntimeReadiness();
        List<String> closed = new ArrayList<>();
        runtime.beginStart();
        runtime.startService("storage", () -> {}, () -> closed.add("original"));
        expect(IllegalArgumentException.class, () -> runtime.startService("storage", () -> {
            throw new AssertionError("duplicate start must never run");
        }, () -> closed.add("replacement")));
        expect(IllegalArgumentException.class, () -> runtime.startService(" ", () -> {}, () -> {}));
        runtime.stop();
        equal(List.of("original"), closed);
    }

    private static void cleanStopCanStartANewLifecycle() throws Exception {
        ServerRuntimeReadiness runtime = new ServerRuntimeReadiness();
        equal(List.of(), runtime.stop());
        List<Integer> closed = new ArrayList<>();
        for (int cycle = 0; cycle < 2; cycle++) {
            final int id = cycle;
            runtime.beginStart();
            runtime.startService("service", () -> {}, () -> closed.add(id));
            runtime.markRunning();
            runtime.stop();
        }
        equal(List.of(0, 1), closed);
    }

    private static void reentrantStopDoesNotCloseTwice() throws Exception {
        ServerRuntimeReadiness runtime = new ServerRuntimeReadiness();
        List<String> closed = new ArrayList<>();
        runtime.beginStart();
        runtime.startService("service", () -> {}, () -> {
            closed.add("once");
            equal(List.of(), runtime.stop());
        });
        runtime.markRunning();
        runtime.stop();
        equal(List.of("once"), closed);
    }

    private static void readinessCannotBePublishedAfterFailure() throws Exception {
        ServerRuntimeReadiness runtime = new ServerRuntimeReadiness();
        expect(IllegalStateException.class, runtime::markRunning);
        runtime.beginStart();
        runtime.fail();
        expect(IllegalStateException.class, runtime::markRunning);
        runtime.stop();
        equal(ServerRuntimeReadiness.State.FAILED, runtime.snapshot().state());
        expect(IllegalStateException.class, runtime::beginStart);
    }

    private static void equal(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) throw new AssertionError("Expected " + expected + ", got " + actual);
    }

    private static void expect(Class<? extends Throwable> type, ServerRuntimeReadiness.StartAction action) throws Exception {
        try {
            action.run();
        } catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("Expected " + type.getName() + ", got " + failure, failure);
        }
        throw new AssertionError("Expected " + type.getName());
    }
}
