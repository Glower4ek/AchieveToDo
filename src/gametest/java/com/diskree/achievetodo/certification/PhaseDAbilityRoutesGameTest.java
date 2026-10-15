package com.diskree.achievetodo.certification;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.ability.PhaseDAbilityMappingTest.CaseContract;
import com.diskree.achievetodo.ability.PhaseDAbilityMappingTest.Level;
import com.diskree.achievetodo.ability.PhaseDAbilityMappingTest.Outcome;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/** Active methods verify infrastructure controls; future natural action routes remain NOT_RUN. */
public final class PhaseDAbilityRoutesGameTest {
    public static final List<CaseContract> FUTURE_CASES = List.of(future(
        "P4:P4-R0001", "PE-05", "CH-06", "D-S03", Outcome.NATIVE_DENIED,
        "P4:pe05_pe07_future_regression_matrix.json#/cases/0; SHA256 3e8701ac29ed3112fff1c5a7b7c63d35487b11d5f2a864962bf9ce1923b21ee0",
        "P4:E014 handleAttack bci117; actual ServerboundAttackPacket, diamond_spear MAIN_HAND",
        "Joined SURVIVAL non-spectator ServerPlayer; EASY count317; full charge; target outside landmarks Y64",
        "Native regular spear packet rejected; this is not Jab", "USE_DIAMOND_TOOLS",
        Map.of("target_health_delta", "0", "target_velocity_delta_due_to_attack", "0",
            "Player_attack_invocations", "0", "item_damage_delta", "0"),
        List.of("No natural reward/completion claim from a rejected packet")));

    static CaseContract future(String id, String pe, String ch, String stage, Outcome outcome,
            String originalReference, String nativeReference, String actor, String eligibility,
            String ability, Map<String, String> observations, List<String> persistence) {
        return new CaseContract(id, List.of(pe), List.of(ch, "CH-21", "CH-22"), originalReference,
            "R1 approved D-DEC traces via P10 CH matrix; D-S00 fixture authority only",
            nativeReference, actor, eligibility, ability, outcome, observations, persistence, stage,
            outcome == Outcome.RUNTIME_REQUIRED ? Level.RUNTIME : Level.GAMETEST,
            stage + " implementation/controller acceptance AND natural route fixture implementation",
            "NOT_RUN", "P10 sealed traceability; runtime receipt NOT_RUN");
    }

    static CaseContract control(String id, Map<String, String> observations) {
        return new CaseContract(id, List.of(), List.of("CH-20", "CH-21", "CH-22"),
            "N/A infrastructure control; R1 implementation_change_matrix.json#/changes/19..21",
            "Explicit owner D-S00 authority, 2026-10-13", "N/A read-only infrastructure control",
            "Test harness; no gameplay actor", "N/A", "N/A", Outcome.PRESERVATION_CONTROL,
            observations, List.of("No world/player/entity/config changes by this test"), "D-S00",
            Level.GAMETEST, "D-S00 registration and supported Fabric runner", "NOT_RUN", "Actual GameTest log required");
    }

    static PhaseDRuntimeExecutionEvidence recorder(List<CaseContract> contracts) {
        try (var input = PhaseDRuntimeExecutionEvidence.class.getResourceAsStream("PhaseDRuntimeExecutionEvidence.class")) {
            if (input == null) throw new IllegalStateException("Missing executing recorder bytecode");
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.readAllBytes()));
            return new PhaseDRuntimeExecutionEvidence(new PhaseDRuntimeExecutionEvidence.Provenance(
                UUID.randomUUID().toString(), PhaseDRuntimeExecutionEvidence.ProvenanceKind.INFRASTRUCTURE_CONTROL,
                Map.of("executing_recorder_class", hash), "Actual GameTest invocation log; control only"), contracts);
        } catch (Exception error) { throw new IllegalStateException(error); }
    }

    static void runControl(GameTestHelper helper, CaseContract contract, Supplier<Map<String, String>> observed) {
        var recorder = recorder(List.of(contract));
        recorder.executed(contract.caseId(), Set.of("D-S00"), "Current D-S00 infrastructure control method invoked");
        try {
            observed.get().forEach((key, value) -> {
                recorder.observation(contract.caseId(), key, value, "Actual invoked read-only control: " + contract.caseId());
                recorder.assertObserved(contract.caseId(), key);
            });
        } finally {
            recorder.cleanup(contract.caseId(), true, "Read-only control allocated no world/entity/player/channel fixture");
            System.out.println("D_S00_CONTROL_RECEIPT " + recorder.json(Set.of(contract.caseId())));
        }
        if (recorder.receipt(Set.of(contract.caseId())).status() != PhaseDRuntimeExecutionEvidence.Status.PASS_CONTROL) {
            throw new AssertionError("Infrastructure control failed: " + contract.caseId());
        }
        helper.succeed();
    }

    private static void require(boolean value) { if (!value) throw new AssertionError("Recorder guard failed"); }
    private static void rejects(Runnable operation) {
        try { operation.run(); } catch (IllegalArgumentException | IllegalStateException expected) { return; }
        throw new AssertionError("Recorder accepted an invalid transition");
    }

    private static void verifyRecorderGuards() {
        CaseContract c = control("guard", Map.of("state_delta", "0"));
        var r = recorder(List.of(c));
        require(r.receipt(Set.of("guard")).status() == PhaseDRuntimeExecutionEvidence.Status.NOT_RUN);
        rejects(() -> recorder(List.of(c, c)));
        rejects(() -> r.receipt(Set.of("extra")));
        rejects(() -> r.assertObserved("guard", "state_delta"));
        rejects(() -> r.executed("guard", Set.of("D-S01"), "wrong stage"));
        r.executed("guard", Set.of("D-S00"), "control invocation");
        rejects(() -> r.executed("guard", Set.of("D-S00"), "duplicate"));
        rejects(() -> r.observation("unknown", "state_delta", "0", "control"));
        rejects(() -> r.observation("guard", "unknown", "0", "control"));
        require(r.receipt(Set.of("guard")).status() == PhaseDRuntimeExecutionEvidence.Status.PARTIAL);
        r.observation("guard", "state_delta", "1", "Deliberate control observation, not a game mutation");
        rejects(() -> r.observation("guard", "state_delta", "0", "duplicate"));
        require(!r.assertObserved("guard", "state_delta"));
        rejects(() -> r.assertObserved("guard", "state_delta"));
        r.cleanup("guard", true, "No game fixture");
        require(r.receipt(Set.of("guard")).status() == PhaseDRuntimeExecutionEvidence.Status.FAIL);
        rejects(() -> r.observation("guard", "state_delta", "0", "after cleanup"));
        var unavailable = recorder(List.of(c));
        unavailable.runtimeUnavailable("guard", "Deliberate unavailable-state control");
        require(unavailable.receipt(Set.of("guard")).status() == PhaseDRuntimeExecutionEvidence.Status.RUNTIME_UNAVAILABLE);
        var incomplete = recorder(List.of(c));
        incomplete.executed("guard", Set.of("D-S00"), "control");
        incomplete.observation("guard", "state_delta", "0", "control");
        require(incomplete.assertObserved("guard", "state_delta"));
        incomplete.cleanup("guard", false, "Deliberate cleanup-failure control");
        require(incomplete.receipt(Set.of("guard")).status() == PhaseDRuntimeExecutionEvidence.Status.PARTIAL);
        var pending = recorder(FUTURE_CASES);
        rejects(() -> pending.executed(FUTURE_CASES.getFirst().caseId(), Set.of("D-S00"), "unimplemented"));
        rejects(() -> pending.executed(FUTURE_CASES.getFirst().caseId(), Set.of("D-S03"), "control cannot certify gameplay"));
        require(pending.receipt(Set.of(FUTURE_CASES.getFirst().caseId())).executedCount() == 0);
    }

    @GameTest(maxTicks = 20)
    public void baselineIdentityAndRecorderGuards(GameTestHelper helper) {
        runControl(helper, control("D-S00-G-ABILITY-CONTROL", Map.of("old_first", "VISION",
            "old_last", "OPEN_ENCHANTING_TABLE", "recorder_guards", "verified")), () -> {
                verifyRecorderGuards();
                return Map.of("old_first", AbilityType.values()[0].name(),
                    "old_last", AbilityType.values()[150].name(), "recorder_guards", "verified");
            });
    }
}
