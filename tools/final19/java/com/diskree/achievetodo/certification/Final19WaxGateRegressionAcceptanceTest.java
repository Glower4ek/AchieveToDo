package com.diskree.achievetodo.certification;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class Final19WaxGateRegressionAcceptanceTest {
    static final Path ROOT=Path.of("").toAbsolutePath();
    static final String POST="4247ef44ce7e331f43820d889ecef541f8cbb0a200a31ede2764c8ecbdd46a13";
    static Path evidence(){var persistent=ROOT.resolve("src/test/resources/final19_certification/axe_gate_postfix_regression.json");return Files.exists(persistent)?persistent:ROOT.resolve("build/tmp/final19_implementation/axe_gate_regression.json");}
    static Path gates(){return Files.exists(ROOT.resolve("src/test/resources/final19_certification/axe_gate_postfix_regression.json"))?ROOT.resolve("reference/phase_a_planning/final19/authorized_production_fixes/wax_axe_gate"):ROOT.resolve("build/tmp/final19_implementation");}
    @Test void exactAuthorizedPatchAndAll14IndependentNativeGateControlsPass()throws Exception{
        assertEquals(POST,Final19StaticContext.sha(Files.readAllBytes(ROOT.resolve("src/main/java/com/diskree/achievetodo/injection/mixin/main/AxeItemMixin.java"))));
        assertEquals("8aa346920a42de51dc8ce8756d6fd212d5d055709c633e07ce8e223e7a49f1a3",Final19StaticContext.sha(Files.readAllBytes(ROOT.resolve("reference/phase_a_planning/final19/wax_axe_gate_production_fix_proposal.patch"))));
        validate(Final19WorldgenEvidence.read(evidence()));
        String log=Files.readString(gates().resolve("axe_gate_regression.gradle.log"));
        assertEquals("0",Files.readString(gates().resolve("axe_gate_regression.exit")).trim());
        assertTrue(log.contains("BUILD SUCCESSFUL")&&log.contains(" (1 tests)")&&log.contains("entries=14"));
        String nativeScope=log.substring(log.indexOf("FINAL19_AXE_GATE_REGRESSION_START"));
        for(String forbidden:List.of("/WARN]","/ERROR]","Failed to handle packet","Leak:"))assertFalse(nativeScope.contains(forbidden),forbidden);
    }
    static void validate(JsonObject artifact){
        assertEquals(POST,artifact.get("productionSha256").getAsString());assertEquals("POST_FIX_REGRESSION_GREEN",artifact.get("stage").getAsString());
        assertEquals(0,artifact.get("certificationGain").getAsInt());
        String run=artifact.get("runId").getAsString();UUID.fromString(run);assertFalse(Set.of("c01abdba-e443-46d3-93d0-e021716c72f1","0937d889-b634-4397-b591-96e25f4307ee").contains(run));
        Set<String> expected=new HashSet<>();for(String mode:List.of("LOCKED_MATERIAL","UNLOCKED_MATERIAL","LOCKED_LANDMARK","OUTSIDE_LOCKED_LANDMARK"))for(String path:List.of("STRIP","SCRAPE","WAX_OFF"))expected.add(mode+"|"+path);expected.add("WRONG_ITEM|WAX_OFF");expected.add("NO_OP|WAX_OFF");
        Set<String> seen=new HashSet<>(),players=new HashSet<>();
        for(var value:artifact.getAsJsonArray("entries")){var r=value.getAsJsonObject();String mode=r.get("mode").getAsString(),path=r.get("path").getAsString();assertTrue(expected.contains(mode+"|"+path)&&seen.add(mode+"|"+path));assertTrue(players.add(r.get("playerUuid").getAsString()));
            assertEquals("SURVIVAL",r.get("gameMode").getAsString());assertEquals("ServerboundUseItemOnPacket.handle",r.get("nativeBoundary").getAsString());
            for(String flag:List.of("joined","connectionRegistered","clientLoaded","finiteMaterials"))assertTrue(r.get(flag).getAsBoolean(),flag);
            for(String flag:List.of("playerRemoved","connectionRemoved","channelSettled","blocksRestored","ownedLandmarkRemoved"))assertTrue(r.getAsJsonObject("cleanup").get(flag).getAsBoolean(),flag);
            boolean locked=mode.equals("LOCKED_MATERIAL"),landmark=mode.equals("LOCKED_LANDMARK"),success=mode.equals("UNLOCKED_MATERIAL")||mode.equals("OUTSIDE_LOCKED_LANDMARK");
            assertEquals(locked,r.get("materialLockedBefore").getAsBoolean());assertEquals(locked,r.get("materialLockedAfter").getAsBoolean());assertEquals(landmark,r.get("landmarkLockedBefore").getAsBoolean());assertEquals(landmark,r.get("landmarkLockedAfter").getAsBoolean());assertTrue(r.get("materialThreshold").getAsInt()>0);
            if(mode.contains("LANDMARK"))assertTrue(r.get("landmarkThreshold").getAsInt()>r.get("materialThreshold").getAsInt());
            assertEquals(!mode.equals("WRONG_ITEM"),r.get("axeTagMember").getAsBoolean());assertEquals(mode.equals("WRONG_ITEM")?"minecraft:stick":"minecraft:diamond_axe",r.get("heldItem").getAsString());
            assertEquals(path.equals("WAX_OFF")&&!mode.equals("NO_OP"),r.get("preMutationWaxTagMember").getAsBoolean());
            String pre=mode.equals("NO_OP")?"Block{minecraft:copper_block}":path.equals("STRIP")?"Block{minecraft:oak_log}[axis=y]":path.equals("SCRAPE")?"Block{minecraft:exposed_copper}":"Block{minecraft:waxed_copper_block}";
            assertEquals(pre,r.get("blockBefore").getAsString());assertEquals(success?(path.equals("STRIP")?"Block{minecraft:stripped_oak_log}[axis=y]":"Block{minecraft:copper_block}"):pre,r.get("blockAfter").getAsString());
            assertEquals(r.get("damageBefore").getAsInt()+(success?1:0),r.get("damageAfter").getAsInt());assertFalse(r.get("criterionBefore").getAsBoolean());assertEquals(success&&path.equals("WAX_OFF"),r.get("criterionAfter").getAsBoolean());
            if(!success)assertEquals(r.get("progressBefore"),r.get("progressAfter"));
        }assertEquals(expected,seen);
    }
    @Test void missingControlsReusedActorsStaleProductionAndFakeBlockedProgressAreRejected()throws Exception{
        var original=Final19WorldgenEvidence.read(evidence());
        for(int mutation=0;mutation<4;mutation++){var bad=original.deepCopy();var entries=bad.getAsJsonArray("entries");var first=entries.get(0).getAsJsonObject();if(mutation==0)entries.remove(0);if(mutation==1)entries.get(1).getAsJsonObject().add("playerUuid",first.get("playerUuid"));if(mutation==2)bad.addProperty("productionSha256","stale");if(mutation==3)first.addProperty("criterionAfter",true);assertThrows(AssertionError.class,()->validate(bad));}
    }
}
