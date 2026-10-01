package com.diskree.achievetodo.certification;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PhaseAStackAllItemsEvidenceTest {
    private static final Path ROOT=Path.of("").toAbsolutePath();
    @Test void persistentScopeRejectsMissingDuplicateExtraPlayersRunIdsAndInventoryCorruption(@TempDir Path fixture)throws Exception {
        for(var file:List.of(PhaseAStackAllItemsCertification.PERSISTENT,PhaseAStackAllItemsCertification.CATALOG,PhaseAStackAllItemsCertification.PRIOR,Path.of("src/test/resources/phase_a_certification/item_tag_inventory_changed_case_catalog.json"),Path.of("reference/phase_a_preservation/files/final/bacap.zip"))){Files.createDirectories(fixture.resolve(file).getParent());Files.copy(ROOT.resolve(file),fixture.resolve(file));}
        var target=fixture.resolve(PhaseAStackAllItemsCertification.PERSISTENT);String accepted=Files.readString(target);var state=fixture.resolve(PhaseAStackAllItemsEvidenceValidation.RUN_STATE);Files.createDirectories(state.getParent());Files.writeString(state,"{\"runId\":\"different-new-run\"}");assertEquals(319,PhaseAStackAllItemsEvidenceValidation.loadValidatedRuntimeEvidence(fixture).get(PhaseAStackAllItemsCertification.ADVANCEMENT).greenCriteria().size());
        for(String mutation:List.of("missing","duplicate","extra","player","run","group","item","count","before","warning","world","diagnostic")){
            var root=JsonParser.parseString(accepted).getAsJsonObject();var entries=root.getAsJsonArray("entries");var receipt=entries.get(0).getAsJsonObject();
            switch(mutation){
                case "missing" -> entries.remove(0);
                case "duplicate" -> entries.add(receipt.deepCopy());
                case "extra" -> receipt.addProperty("criterion","smithing_template");
                case "player" -> receipt.addProperty("playerUuid",entries.get(1).getAsJsonObject().get("playerUuid").getAsString());
                case "run" -> receipt.addProperty("runId",UUID.randomUUID().toString());
                case "group" -> receipt.addProperty("requirementGroup",284);
                case "item" -> receipt.addProperty("observedItem","minecraft:air");
                case "count" -> receipt.addProperty("observedCount",0);
                case "before" -> receipt.addProperty("criterionBefore",true);
                case "warning" -> receipt.getAsJsonObject("cleanup").addProperty("warningCount",1);
                case "world" -> receipt.getAsJsonObject("cleanup").addProperty("worldFixtureRestored",false);
                case "diagnostic" -> root.addProperty("runMode","DIAGNOSTIC");
            }
            Files.writeString(target,root.toString());assertThrows(Exception.class,()->PhaseAStackAllItemsEvidenceValidation.loadValidatedRuntimeEvidence(fixture),mutation);
        }
    }
    @Test void joinsExactlyThePriorSliceAndRemainingGroupsWithOneCanonicalDelta()throws Exception {
        var all=PhaseAAdvancementRollup.loadRuntimeEvidence(ROOT);String id=PhaseAStackAllItemsCertification.ADVANCEMENT;var target=all.get(id);assertEquals(320,target.greenCriteria().size());assertEquals(Set.of("ITEM_TAG_INVENTORY_CHANGED",PhaseAStackAllItemsCertification.FAMILY),target.families());assertEquals(Set.of("smithing_template"),target.criteriaByFamily().get("ITEM_TAG_INVENTORY_CHANGED"));assertEquals(319,target.criteriaByFamily().get(PhaseAStackAllItemsCertification.FAMILY).size());
        var beforeEvidence=new LinkedHashMap<>(all);beforeEvidence.put(id,PhaseAAdvancementRollup.adaptItemTagEvidence(PhaseAItemTagInventoryChangedExecutionEvidenceValidation.loadValidatedRuntimeEvidence(ROOT)).get(id));var before=JsonParser.parseString(PhaseAAdvancementRollup.generateSnapshot(ROOT,beforeEvidence)).getAsJsonObject();var after=JsonParser.parseString(PhaseAAdvancementRollup.generateSnapshot(ROOT,all)).getAsJsonObject();var old=new HashMap<String,JsonObject>();for(var e:before.getAsJsonArray("entries"))old.put(e.getAsJsonObject().get("id").getAsString(),e.getAsJsonObject());int changed=0;
        for(var e:after.getAsJsonArray("entries")){var row=e.getAsJsonObject();String key=row.get("id").getAsString();if(!key.equals(id))assertEquals(old.get(key),row,key);else{assertEquals("RUNTIME_PARTIAL",old.get(key).get("advancementStatus").getAsString());assertEquals("RUNTIME_CERTIFIED",row.get("advancementStatus").getAsString());assertTrue(row.get("requirementsSatisfied").getAsBoolean());changed++;}}
        assertEquals(1,changed);assertEquals(1,after.getAsJsonObject("summary").get("totalCertified").getAsInt()-before.getAsJsonObject("summary").get("totalCertified").getAsInt());assertEquals(0,after.getAsJsonObject("summary").get("runtimePartial").getAsInt());
    }
}
