package com.diskree.achievetodo.certification;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Full fresh roll-up reproduction also executes every persistent evidence loader. */
class PhaseATerminalReconciliationTest {
    @Test
    void frozenRequirementsAndTerminalPlanningRemainIndependent() throws Exception {
        Path root = Path.of("").toAbsolutePath();
        var canonical = JsonParser.parseString(Files.readString(root.resolve("src/test/resources/phase_a_certification/phase_a_advancement_rollup.json"))).getAsJsonObject();
        int criteria = 0, groups = 0, unsatisfied = 0, green = 0;
        try (var zip = new java.util.zip.ZipFile(root.resolve("reference/phase_a_preservation/files/final/bacap.zip").toFile())) {
            for (var element : canonical.getAsJsonArray("entries")) {
                var row = element.getAsJsonObject();
                String id = row.get("id").getAsString();
                var source = zip.getEntry(row.get("sourcePath").getAsString());
                assertNotNull(source, id);
                try (var reader = new java.io.InputStreamReader(zip.getInputStream(source), java.nio.charset.StandardCharsets.UTF_8)) {
                    var raw = JsonParser.parseReader(reader).getAsJsonObject();
                    assertEquals(raw.getAsJsonObject("criteria").size(), row.get("criteriaCount").getAsInt(), id);
                    var requirements = raw.getAsJsonArray("requirements");
                    if (requirements == null) {
                        requirements = new com.google.gson.JsonArray();
                        for (String key : raw.getAsJsonObject("criteria").keySet()) {
                            var group = new com.google.gson.JsonArray();
                            group.add(key);
                            requirements.add(group);
                        }
                    }
                    assertEquals(requirements, row.getAsJsonArray("completionRequirements"), id);
                }
                if (Set.of("RUNTIME_DEFERRED", "STATIC_FAIL_UNRESOLVED").contains(row.get("advancementStatus").getAsString())) {
                    criteria += row.get("criteriaCount").getAsInt();
                    var completed = new HashSet<String>();
                    for (var key : row.getAsJsonArray("runtimeGreenCriteria")) completed.add(key.getAsString());
                    green += completed.size();
                    for (var group : row.getAsJsonArray("completionRequirements")) {
                        groups++;
                        boolean satisfied = false;
                        for (var key : group.getAsJsonArray()) satisfied |= completed.contains(key.getAsString());
                        if (!satisfied) unsatisfied++;
                    }
                }
            }
        }
        String frontierText = Files.readString(root.resolve("reference/phase_a_planning/phase_a_frontier_map.json"));
        var frontier = JsonParser.parseString(frontierText).getAsJsonObject();
        assertEquals("ACTIONABLE_FRONTIER_EXHAUSTED", frontier.get("status").getAsString());
        assertTrue(frontier.getAsJsonObject("currentPriority").get("recommendedNextFrontier").isJsonNull());
        assertEquals(0, frontier.getAsJsonObject("currentPriority").getAsJsonArray("topFive").size());
        var accounting = frontier.getAsJsonObject("requirementAccounting");
        assertEquals(criteria, accounting.get("criteria").getAsInt());
        assertEquals(groups, accounting.get("requirementGroups").getAsInt());
        assertEquals(unsatisfied, accounting.get("unsatisfiedRequirementGroups").getAsInt());
        assertEquals(green, accounting.get("existingGreenCriteria").getAsInt());
        assertEquals(criteria - green, accounting.get("nonGreenCriteria").getAsInt());
        // Historical Phase A terminal evidence remains 1133/1152; FINAL19 product accounting is separate.
        var terminal = JsonParser.parseString(Files.readString(root.resolve(
            "reference/phase_a_planning/phase_a_terminal_reconciliation_20260930.json"))).getAsJsonObject();
        assertEquals("PHASE_A_ACTIONABLE_FRONTIER_EXHAUSTED", terminal.get("classification").getAsString());
        assertTrue(terminal.get("planningOnly").getAsBoolean());
        var checkpoint = terminal.getAsJsonObject("checkpoint");
        var summary = checkpoint.getAsJsonObject("summary");
        assertEquals(0, checkpoint.get("runtimePartial").getAsInt());
        assertEquals(0, summary.get("runtimePartial").getAsInt());
        assertEquals(1133, summary.get("totalCertified").getAsInt());
        assertEquals(19, checkpoint.get("unfinished").getAsInt());
        assertEquals(canonical.getAsJsonObject("summary"), summary);
        assertEquals(checkpoint.get("unfinished").getAsInt(),
            canonical.get("canonicalAdvancementCount").getAsInt() - summary.get("totalCertified").getAsInt());
        var sealedFrontier = checkpoint.getAsJsonObject("frontier");
        assertEquals("reference/phase_a_planning/phase_a_frontier_map.json", sealedFrontier.get("path").getAsString());
        assertEquals(sealedFrontier.get("sha256").getAsString(),
            PhaseAMixedPerfectRunCertification.sha(Files.readAllBytes(root.resolve(sealedFrontier.get("path").getAsString()))));
        var sealedCanonical = checkpoint.getAsJsonObject("canonical");
        assertEquals("src/test/resources/phase_a_certification/phase_a_advancement_rollup.json", sealedCanonical.get("path").getAsString());
        assertEquals(sealedCanonical.get("sha256").getAsString(),
            PhaseAMixedPerfectRunCertification.sha(Files.readAllBytes(root.resolve(sealedCanonical.get("path").getAsString()))));
    }
    @Test void allCanonicalEntriesAndProtectedDebtReconcileAgainstFreshEvidence()throws Exception {
        Path root=Path.of("").toAbsolutePath();String canonical=Files.readString(root.resolve("src/test/resources/phase_a_certification/phase_a_advancement_rollup.json"));assertEquals(canonical,PhaseAAdvancementRollup.generateSnapshot(root));var json=JsonParser.parseString(canonical).getAsJsonObject();var ids=new HashSet<String>();var unfinished=new TreeSet<String>();int runtimeDeferred=0,staticDebt=0;
        for(var e:json.getAsJsonArray("entries")){var row=e.getAsJsonObject();String id=row.get("id").getAsString();assertTrue(ids.add(id));String status=row.get("advancementStatus").getAsString();if(status.equals("RUNTIME_DEFERRED")){runtimeDeferred++;unfinished.add(id);assertEquals(0,row.getAsJsonArray("runtimeGreenCriteria").size());assertTrue(Set.of("minecraft:adventure/voluntary_exile","minecraft:story/deflect_arrow").contains(id));}else if(status.equals("STATIC_FAIL_UNRESOLVED")){staticDebt++;unfinished.add(id);}else {assertTrue(Set.of("RUNTIME_CERTIFIED","STATIC_CERTIFIED").contains(status));assertTrue(row.get("requirementsSatisfied").getAsBoolean()||status.equals("STATIC_CERTIFIED"));}}
        assertEquals(1152,ids.size());assertEquals(2,runtimeDeferred);assertEquals(17,staticDebt);assertEquals(19,unfinished.size());assertEquals(0,json.getAsJsonObject("summary").get("runtimePartial").getAsInt());assertEquals(1133,json.getAsJsonObject("summary").get("totalCertified").getAsInt());
        var frontier=JsonParser.parseString(Files.readString(root.resolve("reference/phase_a_planning/phase_a_frontier_map.json"))).getAsJsonObject();var active=new TreeSet<String>();for(var e:frontier.getAsJsonArray("entries")){var row=e.getAsJsonObject();if(!row.get("familyStatus").getAsString().equals("CLOSED")){active.add(row.get("advancementId").getAsString());assertTrue(Set.of("STATIC_ONLY","PROTECTED").contains(row.get("familyStatus").getAsString()));}}
        assertEquals(unfinished,active);assertEquals(19,frontier.get("unfinishedAtBaseline").getAsInt());assertEquals(PhaseAMixedPerfectRunCertification.sha(canonical.getBytes(java.nio.charset.StandardCharsets.UTF_8)),frontier.get("currentBaselineSha256").getAsString());
    }
}
