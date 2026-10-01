package com.diskree.achievetodo.certification;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import static com.diskree.achievetodo.certification.Final19StaticContext.require;

public final class Final19WorldgenPromotion {
    public static void main(String[] args)throws Exception {
        Path root=Path.of(args[0]);var base=root.resolve("reference/phase_a_planning/final19");
        var evidence=Final19WorldgenEvidence.validate(root,Final19WorldgenEvidence.TEMP,true);
        var canary=Final19WorldgenEvidence.validate(root,Path.of("build/tmp/final19_implementation/worldgen_canary_evidence.json"),false);
        require(!canary.get("runId").equals(evidence.get("runId")),"Canary/exact run IDs reused");
        var run=Final19WorldgenEvidence.read(root.resolve(Final19WorldgenEvidence.RUN));require("TEMP_PROMOTABLE".equals(run.get("stage").getAsString()),"Unfinished run");
        for(String name:List.of("worldgen_canary","worldgen_exact")){
            require(Files.readString(root.resolve("build/tmp/final19_implementation/"+name+".exit")).trim().equals("0"),"GameTest nonzero exit");
            String log=Files.readString(root.resolve("build/tmp/final19_implementation/"+name+".gradle.log"));require(log.contains("BUILD SUCCESSFUL")&&log.contains("entries="+(name.endsWith("canary")?5:36)),"Wrong runtime selector/count/result");
            require(log.contains("achievetodo-test:final19worldgen_game_test_worldgen_"+(name.endsWith("canary")?"canary5":"exact36")+" (1 tests)"),"Wrong actual selected test count");
            int start=log.indexOf("FINAL19_NATIVE_RUN_START family=WORLDGEN_HOLDERSET_CONTEXT");require(start>=0,"Missing native-run warning scope");
            String nativeLog=log.substring(start);require(!nativeLog.contains("/WARN]")&&!nativeLog.contains("/ERROR]")&&!nativeLog.contains("Failed to handle packet")&&!nativeLog.contains("Leak:"),"Runtime/network warning debt");
            // Vanilla test-environment generation and Windows OSHI warnings precede invocation;
            // inventory them separately. Never report those as zero warnings in the full process.
            var warnings=new JsonArray();for(String line:log.substring(0,start).split("\\R"))if(line.contains("/WARN]")||line.contains("/ERROR]"))warnings.add(line);
            JsonObject startup=new JsonObject();startup.addProperty("scope","BEFORE_NATIVE_TEST_INVOCATION_NO_OWNED_ACTORS");startup.add("observedWarnings",warnings);Final19WorldgenEvidence.write(base.resolve(name+"_startup_warning_inventory.json"),startup);
        }
        var jungle=PhaseARuntimeExecutionEvidenceValidation.loadValidatedRuntimeEvidence(root).get("blazeandcave:biomes/the_mighty_jungle");require(jungle!=null&&jungle.greenCriteria().equals(Set.of("jungle")),"Retained jungle proof invalid");
        try(var context=new Final19StaticContext(root,Final19WorldgenEvidence.FAMILY)){context.validate(Final19WorldgenEvidence.FAMILY);}
        require(Final19StaticContext.sha(Files.readAllBytes(root.resolve("src/test/resources/phase_a_certification/phase_a_advancement_rollup.json"))).equals("439f2aa1c577ffbee275525765ac9229bf5633f0f5e1da498c0adc5702bd2810"),"Historical Phase A changed");
        Files.copy(root.resolve(Final19WorldgenEvidence.TEMP),root.resolve(Final19WorldgenEvidence.PERSISTENT));
        Final19WorldgenEvidence.validate(root,Final19WorldgenEvidence.PERSISTENT,true);
        var ledger=Final19WorldgenEvidence.read(root.resolve("src/test/resources/phase_a_certification/phase_a_advancement_rollup.json"));ledger.addProperty("snapshot","final19_product_certification_ledger");ledger.addProperty("historicalPhaseACertified",1133);ledger.add("historicalPhaseASummary",ledger.remove("summary"));
        Set<String> ids=new HashSet<>();int certified=0;for(var value:ledger.getAsJsonArray("entries")){var entry=value.getAsJsonObject();require(ids.add(entry.get("id").getAsString()),"Duplicate advancement");if(Set.of("blazeandcave:biomes/the_mighty_jungle","blazeandcave:redstone/travelling_bard").contains(entry.get("id").getAsString())){entry.addProperty("productStatus","FINAL19_CERTIFIED");entry.addProperty("final19Family",Final19WorldgenEvidence.FAMILY);entry.addProperty("productRequirementsSatisfied",true);certified++;}else if(Set.of("STATIC_CERTIFIED","RUNTIME_CERTIFIED").contains(entry.get("advancementStatus").getAsString()))certified++;}
        require(ids.size()==1152&&certified==1135,"Unexpected actual product accounting");ledger.addProperty("productCertified",certified);ledger.addProperty("productUncertified",1152-certified);JsonObject summary=new JsonObject();summary.addProperty("totalAdvancements",ids.size());summary.addProperty("totalCertified",certified);summary.addProperty("uncertified",1152-certified);summary.addProperty("final19Certified",2);summary.addProperty("terminalIntegrityClaimAllowed",false);ledger.add("summary",summary);Final19WorldgenEvidence.write(base.resolve("product_ledger.json"),ledger);
        var state=Final19WorldgenEvidence.read(base.resolve("execution_state.json"));state.addProperty("productCertified",certified);state.getAsJsonArray("completedFamilies").add(Final19WorldgenEvidence.FAMILY);
        var remaining=new JsonArray();for(var id:state.getAsJsonArray("remainingTargets"))if(!Set.of("blazeandcave:biomes/the_mighty_jungle","blazeandcave:redstone/travelling_bard").contains(id.getAsString()))remaining.add(id);state.add("remainingTargets",remaining);
        state.addProperty("activeFamily","ENTITY_VARIANT_COMPONENT_CONTEXT");state.addProperty("nextOperationalFamily","ENTITY_VARIANT_COMPONENT_CONTEXT");state.addProperty("transactionStage","DERIVE_EXACT_SEMANTICS");state.addProperty("firstUnfinishedTransaction","ENTITY_VARIANT_COMPONENT_CONTEXT: typed variant proof, native canary, exact23, promotion");state.getAsJsonArray("acceptedRunIds").add(evidence.get("runId"));
        JsonObject inventory=new JsonObject();inventory.addProperty("family",Final19WorldgenEvidence.FAMILY);inventory.addProperty("path",Final19WorldgenEvidence.PERSISTENT.toString());inventory.addProperty("sha256",Final19StaticContext.sha(Files.readAllBytes(root.resolve(Final19WorldgenEvidence.PERSISTENT))));inventory.add("runId",evidence.get("runId"));inventory.addProperty("catalogSha256",Final19StaticContext.sha(Files.readAllBytes(root.resolve(Final19StaticContext.RESOURCES).resolve("worldgen_holderset_context_catalog.json"))));state.getAsJsonArray("persistentEvidenceInventory").add(inventory);Final19WorldgenEvidence.write(base.resolve("execution_state.json"),state);
        System.out.println("WORLDGEN_HOLDERSET_CONTEXT_FINAL19_GREEN 1133->1135 runId="+evidence.get("runId"));
    }
}
