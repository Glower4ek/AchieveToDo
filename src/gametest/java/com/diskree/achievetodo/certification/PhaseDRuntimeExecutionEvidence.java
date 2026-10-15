package com.diskree.achievetodo.certification;

import com.diskree.achievetodo.ability.PhaseDAbilityMappingTest.CaseContract;
import com.diskree.achievetodo.ability.PhaseDAbilityMappingTest.Outcome;
import com.google.gson.Gson;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Test-owned receipts only. No game mutation, world IO, automatic PASS or historical evidence writer. */
public final class PhaseDRuntimeExecutionEvidence {
    public enum Status { NOT_RUN, RUNTIME_UNAVAILABLE, PARTIAL, FAIL, PASS_CONTROL, PASS_OBSERVED_CASE }
    public enum ProvenanceKind { INFRASTRUCTURE_CONTROL, OBSERVED_GAME_RUNTIME }

    public record Provenance(String runId, ProvenanceKind kind, Map<String, String> hashes,
            String environmentReference) {
        public Provenance {
            nonblank(runId); nonblank(environmentReference);
            if (kind == null) throw new IllegalArgumentException("Missing provenance kind");
            hashes = Map.copyOf(hashes);
            if (hashes.isEmpty()) throw new IllegalArgumentException("Missing provenance hashes");
            hashes.forEach((name, hash) -> {
                nonblank(name);
                if (!hash.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("Invalid SHA256: " + name);
            });
            if (kind == ProvenanceKind.OBSERVED_GAME_RUNTIME && !hashes.keySet().containsAll(
                    Set.of("source", "jar", "config", "criteria", "native"))) {
                throw new IllegalArgumentException("Runtime source/JAR/config/criteria/native binding required");
            }
        }
    }

    public record Observation(long sequence, String caseId, String name, String actualValue,
            String evidenceReference) { }
    public record Assertion(long sequence, String caseId, String name, String expectedValue,
            String actualValue, boolean passed, String observationReference) { }
    public record CaseReceipt(CaseContract contract, boolean registered, boolean executed,
            List<Observation> observations, List<Assertion> assertions, String unavailableReason,
            boolean cleanupRecorded, boolean cleanupCompleted, String cleanupReference, Status status) { }
    public record RunReceipt(Provenance provenance, List<CaseReceipt> cases, int plannedCount,
            int executedCount, int observedCount, int passedCount, int failedCount,
            boolean exactCaseSet, Status status) { }

    private static final class Execution {
        final CaseContract contract;
        boolean executed;
        String unavailableReason = "";
        boolean cleanupRecorded;
        boolean cleanupCompleted;
        String cleanupReference = "";
        final Map<String, Observation> observations = new LinkedHashMap<>();
        final Map<String, Assertion> assertions = new LinkedHashMap<>();
        Execution(CaseContract contract) { this.contract = contract; }
    }

    private final Provenance provenance;
    private final Map<String, Execution> cases = new LinkedHashMap<>();
    private long sequence;

    public PhaseDRuntimeExecutionEvidence(Provenance provenance, List<CaseContract> plannedCases) {
        if (provenance == null || plannedCases.isEmpty()) throw new IllegalArgumentException("Empty run");
        this.provenance = provenance;
        for (CaseContract contract : List.copyOf(plannedCases)) {
            if (cases.putIfAbsent(contract.caseId(), new Execution(contract)) != null) {
                throw new IllegalArgumentException("Duplicate planned case: " + contract.caseId());
            }
        }
    }

    public synchronized void executed(String caseId, Set<String> acceptedStages, String activationReference) {
        Execution execution = find(caseId);
        nonblank(activationReference);
        if (execution.executed || !execution.unavailableReason.isEmpty()) {
            throw new IllegalStateException("Already started or unavailable: " + caseId);
        }
        if (!acceptedStages.contains(execution.contract.requiredStage())) {
            throw new IllegalStateException("Pending implementation stage: " + execution.contract.requiredStage());
        }
        if (execution.contract.outcome() == Outcome.CONDITIONAL_ROUTE) {
            throw new IllegalStateException("Conditional scenario requires separately authorized fixture activation");
        }
        if (provenance.kind() == ProvenanceKind.INFRASTRUCTURE_CONTROL
                && execution.contract.outcome() != Outcome.PRESERVATION_CONTROL) {
            throw new IllegalStateException("Control provenance cannot certify gameplay");
        }
        execution.executed = true;
    }

    public synchronized void observation(String caseId, String name, String actualValue, String reference) {
        Execution execution = running(caseId);
        nonblank(name); nonblank(actualValue); nonblank(reference);
        if (!execution.contract.expectedObservations().containsKey(name)) {
            throw new IllegalArgumentException("Unexpected observation: " + name);
        }
        if (execution.observations.containsKey(name)) throw new IllegalStateException("Duplicate observation: " + name);
        execution.observations.put(name, new Observation(++sequence, caseId, name, actualValue, reference));
    }

    /** Comparison uses the independent contract and a captured observation, never a caller-supplied PASS. */
    public synchronized boolean assertObserved(String caseId, String name) {
        Execution execution = running(caseId);
        Observation observation = execution.observations.get(name);
        if (observation == null) throw new IllegalStateException("Missing observation: " + name);
        if (execution.assertions.containsKey(name)) throw new IllegalStateException("Duplicate assertion: " + name);
        String expected = execution.contract.expectedObservations().get(name);
        boolean passed = expected.equals(observation.actualValue());
        execution.assertions.put(name, new Assertion(++sequence, caseId, name, expected,
            observation.actualValue(), passed, observation.evidenceReference()));
        return passed;
    }

    public synchronized void runtimeUnavailable(String caseId, String reason) {
        Execution execution = find(caseId);
        nonblank(reason);
        if (execution.executed || !execution.unavailableReason.isEmpty()) throw new IllegalStateException("Already started");
        execution.unavailableReason = reason;
    }

    public synchronized void cleanup(String caseId, boolean completed, String evidenceReference) {
        Execution execution = running(caseId);
        nonblank(evidenceReference);
        if (execution.cleanupRecorded) throw new IllegalStateException("Duplicate cleanup");
        execution.cleanupRecorded = true;
        execution.cleanupCompleted = completed;
        execution.cleanupReference = evidenceReference;
    }

    private static void nonblank(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Blank evidence field");
    }

    private Execution find(String caseId) {
        Execution execution = cases.get(caseId);
        if (execution == null) throw new IllegalArgumentException("Unplanned case: " + caseId);
        return execution;
    }

    private Execution running(String caseId) {
        Execution execution = find(caseId);
        if (!execution.executed || execution.cleanupRecorded) throw new IllegalStateException("Case is not running: " + caseId);
        return execution;
    }

    private CaseReceipt receipt(Execution execution) {
        Status status;
        if (!execution.executed) {
            status = execution.unavailableReason.isEmpty() ? Status.NOT_RUN : Status.RUNTIME_UNAVAILABLE;
        } else if (execution.assertions.values().stream().anyMatch(a -> !a.passed())) {
            status = Status.FAIL;
        } else if (!execution.cleanupCompleted || !execution.observations.keySet().equals(
                execution.contract.expectedObservations().keySet())
                || !execution.assertions.keySet().equals(execution.contract.expectedObservations().keySet())) {
            status = Status.PARTIAL;
        } else {
            status = provenance.kind() == ProvenanceKind.INFRASTRUCTURE_CONTROL
                ? Status.PASS_CONTROL : Status.PASS_OBSERVED_CASE;
        }
        return new CaseReceipt(execution.contract, true, execution.executed,
            List.copyOf(execution.observations.values()), List.copyOf(execution.assertions.values()),
            execution.unavailableReason, execution.cleanupRecorded, execution.cleanupCompleted,
            execution.cleanupReference, status);
    }

    /** Caller must supply the exact selected case set; subset/missing/extra identities fail closed. */
    public synchronized RunReceipt receipt(Set<String> exactSelectedCaseIds) {
        if (!cases.keySet().equals(exactSelectedCaseIds)) throw new IllegalArgumentException("Missing/extra selected case IDs");
        List<CaseReceipt> receipts = cases.values().stream().map(this::receipt).toList();
        int executed = (int) receipts.stream().filter(CaseReceipt::executed).count();
        int observed = (int) receipts.stream().filter(r -> !r.observations().isEmpty()).count();
        int passed = (int) receipts.stream().filter(r -> r.status() == Status.PASS_CONTROL
            || r.status() == Status.PASS_OBSERVED_CASE).count();
        int failed = (int) receipts.stream().filter(r -> r.status() == Status.FAIL).count();
        Status status = failed > 0 ? Status.FAIL : passed == receipts.size()
            ? (provenance.kind() == ProvenanceKind.INFRASTRUCTURE_CONTROL ? Status.PASS_CONTROL : Status.PASS_OBSERVED_CASE)
            : receipts.stream().allMatch(r -> r.status() == Status.RUNTIME_UNAVAILABLE) ? Status.RUNTIME_UNAVAILABLE
            : executed == 0 && receipts.stream().allMatch(r -> r.status() == Status.NOT_RUN) ? Status.NOT_RUN : Status.PARTIAL;
        return new RunReceipt(provenance, receipts, receipts.size(), executed, observed, passed, failed, true, status);
    }

    public String json(Set<String> exactSelectedCaseIds) { return new Gson().toJson(receipt(exactSelectedCaseIds)); }
}
