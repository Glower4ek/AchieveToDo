package com.diskree.achievetodo.ability;

import com.google.gson.JsonParser;
import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

/** Native/static contracts; gameplay and persistence are independently observed by server GameTests. */
public class PhaseDCopperTransitionTest {
    @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }
    private static ClassNode node(String owner) throws Exception {
        try(var input=PhaseDCopperTransitionTest.class.getResourceAsStream("/"+owner.replace('.','/')+".class")) {
            assertNotNull(input,owner);ClassNode n=new ClassNode();new ClassReader(input).accept(n,0);return n;
        }
    }
    private static MethodNode method(String owner,String name) throws Exception {
        return node(owner).methods.stream().filter(m -> m.name.equals(name)).findFirst().orElseThrow();
    }
    private static List<String> calls(MethodNode m) {
        return Arrays.stream(m.instructions.toArray()).filter(i -> i instanceof MethodInsnNode).map(i -> ((MethodInsnNode)i).name).toList();
    }
    static Stream<String> mixins() { return Stream.of("AxeItemMixin","CopperGolemEntityMixin","CopperGolemStatueBlockMixin","HoneycombItemMixin"); }
    @ParameterizedTest @MethodSource("mixins")
    void everyInjectorBindsEveryActualDeclaringOwner(String name) throws Exception {
        ClassNode mixin=node("com.diskree.achievetodo.injection.mixin.main."+name);
        AnnotationNode annotation=mixin.invisibleAnnotations.stream().filter(a -> a.desc.endsWith("/Mixin;")).findFirst().orElseThrow();
        @SuppressWarnings("unchecked") List<Type> owners=(List<Type>)annotation.values.get(annotation.values.indexOf("value")+1);
        for(Type owner:owners) for(MethodNode handler:mixin.methods) if(handler.visibleAnnotations!=null) for(AnnotationNode a:handler.visibleAnnotations) {
            if(a.values==null||!a.values.contains("method"))continue;
            @SuppressWarnings("unchecked") List<String> selectors=(List<String>)a.values.get(a.values.indexOf("method")+1);
            for(String selector:selectors) {
                MethodNode nativeMethod=node(owner.getInternalName()).methods.stream().filter(m -> selector.contains("(")?(m.name+m.desc).equals(selector):m.name.equals(selector)).findFirst().orElseThrow();
                assertTrue(nativeMethod.instructions.size()>0);
                // Handler carries the complete native parameter descriptor plus CallbackInfo.
                String nativeArgs=nativeMethod.desc.substring(0,nativeMethod.desc.indexOf(')'));
                assertTrue(handler.desc.startsWith(nativeArgs),handler.desc+" vs "+nativeMethod.desc);
            }
        }
        assertEquals(name.equals("CopperGolemStatueBlockMixin")?2:1,owners.size());
    }
    static Stream<Item> axes() { return Stream.of(Items.WOODEN_AXE,Items.STONE_AXE,Items.COPPER_AXE,Items.IRON_AXE,Items.GOLDEN_AXE,Items.DIAMOND_AXE,Items.NETHERITE_AXE); }
    @ParameterizedTest @MethodSource("axes")
    void sevenMaterialsAndActualPureStripCandidate(Item item) throws Exception {
        AbilityType expected= item==Items.WOODEN_AXE?AbilityType.USE_WOODEN_TOOLS:item==Items.STONE_AXE?AbilityType.USE_STONE_TOOLS:
            item==Items.COPPER_AXE?AbilityType.USE_COPPER_TOOLS:item==Items.IRON_AXE?AbilityType.USE_IRON_TOOLS:
            item==Items.GOLDEN_AXE?AbilityType.USE_GOLDEN_TOOLS:item==Items.DIAMOND_AXE?AbilityType.USE_DIAMOND_TOOLS:AbilityType.USE_NETHERITE_TOOLS;
        assertEquals(expected,AbilityType.findToolMaterialUsageAbility(item));
        var get=AxeItem.class.getDeclaredMethod("getStripped",BlockState.class);get.setAccessible(true);
        var state=Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,Direction.Axis.X);
        assertEquals(Optional.of(Blocks.STRIPPED_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,Direction.Axis.X)),get.invoke(item,state));
        assertEquals(Optional.empty(),get.invoke(item,Blocks.STONE.defaultBlockState()));
    }
    @Test void unrelatedHoneycombAndShearsDoNotAcquireToolPermissions() {
        assertNull(AbilityType.findToolMaterialUsageAbility(Items.HONEYCOMB));assertNull(AbilityType.findToolMaterialUsageAbility(Items.SHEARS));
    }
    @Test void pureCandidateOrderPrecedesPermissionAndNeverCallsEvaluator() throws Exception {
        var c=calls(method("com.diskree.achievetodo.injection.mixin.main.AxeItemMixin","lockStrip"));
        assertTrue(c.indexOf("getStripped")<c.indexOf("isTargetInLockedLandmark"));
        assertTrue(c.indexOf("isEmpty")<c.indexOf("isAbilityLocked"));assertFalse(c.contains("evaluateNewBlockState"));
        var n=calls(method("net.minecraft.world.item.AxeItem","evaluateNewBlockState"));
        assertTrue(n.indexOf("getStripped")<n.indexOf("getPrevious"));assertTrue(n.indexOf("getPrevious")<n.indexOf("ofNullable"));
        assertTrue(n.indexOf("playSound")<n.indexOf("getPrevious")); // Calling the evaluator as a predicate is unsafe.
    }
    @Test void nativeGolemOrderingAndWaxSentinelRemainIndependent() throws Exception {
        var c=calls(method("net.minecraft.world.entity.animal.golem.CopperGolem","mobInteract"));
        assertTrue(c.indexOf("throwItem")<c.indexOf("readyForShearing"));assertTrue(c.indexOf("shear")<c.indexOf("isClientSide"));
        assertTrue(c.indexOf("isClientSide")<c.indexOf("usePlayerItem"));assertTrue(c.indexOf("usePlayerItem")<c.indexOf("playSound"));
        assertTrue(Arrays.stream(method("net.minecraft.world.entity.animal.golem.CopperGolem","mobInteract").instructions.toArray()).anyMatch(i -> i instanceof LdcInsnNode l && Long.valueOf(-2).equals(l.cst)));
        var m=node("com.diskree.achievetodo.injection.mixin.main.CopperGolemEntityMixin");
        assertFalse(m.methods.stream().anyMatch(v -> List.of("tick","updateWeathering","setFlower","aiStep").contains(v.name)));
    }
    @Test void statueOwnersHaveDistinctNativeEligibilityAndEffects() throws Exception {
        String base="net.minecraft.world.level.block.CopperGolemStatueBlock",weather="net.minecraft.world.level.block.WeatheringCopperGolemStatueBlock";
        var b=calls(method(base,"useItemOn"));var w=calls(method(weather,"useItemOn"));
        assertFalse(b.contains("getBlockEntity"));assertFalse(b.contains("removeStatue"));assertTrue(b.contains("updatePose"));
        assertTrue(w.indexOf("getBlockEntity")<w.indexOf("removeStatue"));assertTrue(w.indexOf("removeStatue")<w.indexOf("hurtAndBreak"));
        assertTrue(w.indexOf("hurtAndBreak")<w.indexOf("addFreshEntity"));assertTrue(w.indexOf("addFreshEntity")<w.indexOf("removeBlock"));
        assertTrue(calls(method(base,"updatePose")).indexOf("playSound")<calls(method(base,"updatePose")).indexOf("setBlock"));
        var gate=calls(method("com.diskree.achievetodo.injection.mixin.main.CopperGolemStatueBlockMixin","lockStatueInteraction"));
        assertTrue(gate.containsAll(List.of("findToolMaterialUsageAbility","isTargetInLockedLandmark")));
        assertFalse(gate.contains("getBlockEntity"));assertFalse(gate.contains("removeStatue"));
        var handler=method("com.diskree.achievetodo.injection.mixin.main.CopperGolemStatueBlockMixin","lockStatueInteraction");
        var injection=handler.visibleAnnotations.stream().filter(a -> a.desc.endsWith("/Inject;")).findFirst().orElseThrow();
        @SuppressWarnings("unchecked") List<AnnotationNode> points=(List<AnnotationNode>)injection.values.get(injection.values.indexOf("at")+1);
        assertEquals(3,points.size());
        for(String owner:List.of(base,weather)) {
            var nativeCalls=Arrays.stream(method(owner,"useItemOn").instructions.toArray()).filter(i -> i instanceof MethodInsnNode).map(i -> (MethodInsnNode)i).toList();
            long matches=points.stream().filter(a -> nativeCalls.stream().anyMatch(c -> ("L"+c.owner+";"+c.name+c.desc).equals(a.values.get(a.values.indexOf("target")+1)))).count();
            assertEquals(owner.equals(base)?1:2,matches);
        }
    }
    @Test void honeycombEligibilityIsPureAndPrecedesCriterionConsumptionAndMutation() throws Exception {
        var get=calls(method("net.minecraft.world.item.HoneycombItem","getWaxed"));
        assertFalse(get.contains("setBlock"));assertFalse(get.contains("trigger"));assertFalse(get.contains("shrink"));
        var wax=node("net.minecraft.world.item.HoneycombItem").methods.stream().filter(m -> m.name.startsWith("lambda$useOn$")).findFirst().orElseThrow();var c=calls(wax);
        assertTrue(c.indexOf("trigger")<c.indexOf("shrink"));assertTrue(c.indexOf("shrink")<c.indexOf("setBlock"));
        var guard=calls(method("com.diskree.achievetodo.injection.mixin.main.HoneycombItemMixin","lockManualWax"));
        assertTrue(guard.indexOf("isEmpty")<guard.indexOf("isTargetInLockedLandmark"));assertFalse(guard.contains("isAbilityLocked"));
    }
    @Test void noCandidateNativeMappingsAreEmpty() {
        assertEquals(Optional.empty(),HoneycombItem.getWaxed(Blocks.STONE.defaultBlockState()));
        assertEquals(Optional.empty(),HoneycombItem.getWaxed(BuiltInRegistries.BLOCK.getValue(Identifier.parse("minecraft:waxed_chiseled_copper")).defaultBlockState()));
        Block copper=BuiltInRegistries.BLOCK.getValue(Identifier.parse("minecraft:copper_block"));
        assertEquals(Optional.empty(),WeatheringCopper.getPrevious(copper.defaultBlockState()));
        assertNull(HoneycombItem.WAX_OFF_BY_BLOCK.get().get(copper));
    }
    @Test void noDeltaChestAndNativeBlockEntityRetentionContracts() throws Exception {
        var chest=node("net.minecraft.world.level.block.CopperChestBlock");assertEquals("net/minecraft/world/level/block/ChestBlock",chest.superName);
        assertFalse(chest.methods.stream().anyMatch(m -> m.name.equals("useWithoutItem")));
        assertTrue(calls(method("net.minecraft.world.level.block.ChestBlock","useWithoutItem")).contains("openMenu"));
        assertTrue(calls(method("net.minecraft.world.level.chunk.LevelChunk","setBlockState")).containsAll(List.of("shouldChangedStateKeepBlockEntity","isValidBlockState","setBlockState")));
        assertTrue(calls(method("net.minecraft.world.level.block.CopperChestBlock","updateShape")).contains("withPropertiesOf"));
    }
    @Test void registrationKeepsHistoricalIdentitiesAndRequiredPolicy() throws Exception {
        var json=JsonParser.parseString(Files.readString(Path.of("src/main/resources/achievetodo.mixins.json"))).getAsJsonObject();assertTrue(json.get("required").getAsBoolean());
        assertEquals(1,json.getAsJsonObject("injectors").get("defaultRequire").getAsInt());
        List<String> entries=new ArrayList<>();json.getAsJsonArray("mixins").forEach(v -> entries.add(v.getAsString()));assertEquals(entries.size(),new HashSet<>(entries).size());
        for(String name:List.of("AxeItemMixin","CopperGolemEntityMixin","CopperGolemStatueBlockMixin","HoneycombItemMixin","PiercingWeaponMixin","KineticWeaponMixin","CamelEntityMixin","AnimalEquipmentTransferMixin"))assertEquals(1,Collections.frequency(entries,"main."+name));
        assertFalse(entries.contains("main.CopperChestBlockMixin"));
        assertEquals(47,JsonParser.parseString(Files.readString(Path.of("src/gametest/resources/fabric.mod.json"))).getAsJsonObject().getAsJsonObject("entrypoints").getAsJsonArray("fabric-gametest").size());
    }
    // Sealed P3 original indexed edges, inserted from t02_transition_graph_reconciliation.json.
    private static final String EDGE_ORACLE = """
        0 wax minecraft:chiseled_copper minecraft:waxed_chiseled_copper
        1 wax minecraft:chiseled_copper minecraft:waxed_chiseled_copper
        2 wax_off minecraft:waxed_chiseled_copper minecraft:chiseled_copper
        3 weather minecraft:chiseled_copper minecraft:exposed_chiseled_copper
        4 wax minecraft:exposed_chiseled_copper minecraft:waxed_exposed_chiseled_copper
        5 wax minecraft:exposed_chiseled_copper minecraft:waxed_exposed_chiseled_copper
        6 wax_off minecraft:waxed_exposed_chiseled_copper minecraft:exposed_chiseled_copper
        7 scrape minecraft:exposed_chiseled_copper minecraft:chiseled_copper
        8 weather minecraft:exposed_chiseled_copper minecraft:weathered_chiseled_copper
        9 wax minecraft:weathered_chiseled_copper minecraft:waxed_weathered_chiseled_copper
        10 wax minecraft:weathered_chiseled_copper minecraft:waxed_weathered_chiseled_copper
        11 wax_off minecraft:waxed_weathered_chiseled_copper minecraft:weathered_chiseled_copper
        12 scrape minecraft:weathered_chiseled_copper minecraft:exposed_chiseled_copper
        13 weather minecraft:weathered_chiseled_copper minecraft:oxidized_chiseled_copper
        14 wax minecraft:oxidized_chiseled_copper minecraft:waxed_oxidized_chiseled_copper
        15 wax minecraft:oxidized_chiseled_copper minecraft:waxed_oxidized_chiseled_copper
        16 wax_off minecraft:waxed_oxidized_chiseled_copper minecraft:oxidized_chiseled_copper
        17 scrape minecraft:oxidized_chiseled_copper minecraft:weathered_chiseled_copper
        18 wax minecraft:copper_bars minecraft:waxed_copper_bars
        19 wax minecraft:copper_bars minecraft:waxed_copper_bars
        20 wax_off minecraft:waxed_copper_bars minecraft:copper_bars
        21 weather minecraft:copper_bars minecraft:exposed_copper_bars
        22 wax minecraft:exposed_copper_bars minecraft:waxed_exposed_copper_bars
        23 wax minecraft:exposed_copper_bars minecraft:waxed_exposed_copper_bars
        24 wax_off minecraft:waxed_exposed_copper_bars minecraft:exposed_copper_bars
        25 scrape minecraft:exposed_copper_bars minecraft:copper_bars
        26 weather minecraft:exposed_copper_bars minecraft:weathered_copper_bars
        27 wax minecraft:weathered_copper_bars minecraft:waxed_weathered_copper_bars
        28 wax minecraft:weathered_copper_bars minecraft:waxed_weathered_copper_bars
        29 wax_off minecraft:waxed_weathered_copper_bars minecraft:weathered_copper_bars
        30 scrape minecraft:weathered_copper_bars minecraft:exposed_copper_bars
        31 weather minecraft:weathered_copper_bars minecraft:oxidized_copper_bars
        32 wax minecraft:oxidized_copper_bars minecraft:waxed_oxidized_copper_bars
        33 wax minecraft:oxidized_copper_bars minecraft:waxed_oxidized_copper_bars
        34 wax_off minecraft:waxed_oxidized_copper_bars minecraft:oxidized_copper_bars
        35 scrape minecraft:oxidized_copper_bars minecraft:weathered_copper_bars
        36 wax minecraft:copper_block minecraft:waxed_copper_block
        37 wax minecraft:copper_block minecraft:waxed_copper_block
        38 wax_off minecraft:waxed_copper_block minecraft:copper_block
        39 weather minecraft:copper_block minecraft:exposed_copper
        40 wax minecraft:exposed_copper minecraft:waxed_exposed_copper
        41 wax minecraft:exposed_copper minecraft:waxed_exposed_copper
        42 wax_off minecraft:waxed_exposed_copper minecraft:exposed_copper
        43 scrape minecraft:exposed_copper minecraft:copper_block
        44 weather minecraft:exposed_copper minecraft:weathered_copper
        45 wax minecraft:weathered_copper minecraft:waxed_weathered_copper
        46 wax minecraft:weathered_copper minecraft:waxed_weathered_copper
        47 wax_off minecraft:waxed_weathered_copper minecraft:weathered_copper
        48 scrape minecraft:weathered_copper minecraft:exposed_copper
        49 weather minecraft:weathered_copper minecraft:oxidized_copper
        50 wax minecraft:oxidized_copper minecraft:waxed_oxidized_copper
        51 wax minecraft:oxidized_copper minecraft:waxed_oxidized_copper
        52 wax_off minecraft:waxed_oxidized_copper minecraft:oxidized_copper
        53 scrape minecraft:oxidized_copper minecraft:weathered_copper
        54 wax minecraft:copper_bulb minecraft:waxed_copper_bulb
        55 wax minecraft:copper_bulb minecraft:waxed_copper_bulb
        56 wax_off minecraft:waxed_copper_bulb minecraft:copper_bulb
        57 weather minecraft:copper_bulb minecraft:exposed_copper_bulb
        58 wax minecraft:exposed_copper_bulb minecraft:waxed_exposed_copper_bulb
        59 wax minecraft:exposed_copper_bulb minecraft:waxed_exposed_copper_bulb
        60 wax_off minecraft:waxed_exposed_copper_bulb minecraft:exposed_copper_bulb
        61 scrape minecraft:exposed_copper_bulb minecraft:copper_bulb
        62 weather minecraft:exposed_copper_bulb minecraft:weathered_copper_bulb
        63 wax minecraft:weathered_copper_bulb minecraft:waxed_weathered_copper_bulb
        64 wax minecraft:weathered_copper_bulb minecraft:waxed_weathered_copper_bulb
        65 wax_off minecraft:waxed_weathered_copper_bulb minecraft:weathered_copper_bulb
        66 scrape minecraft:weathered_copper_bulb minecraft:exposed_copper_bulb
        67 weather minecraft:weathered_copper_bulb minecraft:oxidized_copper_bulb
        68 wax minecraft:oxidized_copper_bulb minecraft:waxed_oxidized_copper_bulb
        69 wax minecraft:oxidized_copper_bulb minecraft:waxed_oxidized_copper_bulb
        70 wax_off minecraft:waxed_oxidized_copper_bulb minecraft:oxidized_copper_bulb
        71 scrape minecraft:oxidized_copper_bulb minecraft:weathered_copper_bulb
        72 wax minecraft:copper_chain minecraft:waxed_copper_chain
        73 wax minecraft:copper_chain minecraft:waxed_copper_chain
        74 wax_off minecraft:waxed_copper_chain minecraft:copper_chain
        75 weather minecraft:copper_chain minecraft:exposed_copper_chain
        76 wax minecraft:exposed_copper_chain minecraft:waxed_exposed_copper_chain
        77 wax minecraft:exposed_copper_chain minecraft:waxed_exposed_copper_chain
        78 wax_off minecraft:waxed_exposed_copper_chain minecraft:exposed_copper_chain
        79 scrape minecraft:exposed_copper_chain minecraft:copper_chain
        80 weather minecraft:exposed_copper_chain minecraft:weathered_copper_chain
        81 wax minecraft:weathered_copper_chain minecraft:waxed_weathered_copper_chain
        82 wax minecraft:weathered_copper_chain minecraft:waxed_weathered_copper_chain
        83 wax_off minecraft:waxed_weathered_copper_chain minecraft:weathered_copper_chain
        84 scrape minecraft:weathered_copper_chain minecraft:exposed_copper_chain
        85 weather minecraft:weathered_copper_chain minecraft:oxidized_copper_chain
        86 wax minecraft:oxidized_copper_chain minecraft:waxed_oxidized_copper_chain
        87 wax minecraft:oxidized_copper_chain minecraft:waxed_oxidized_copper_chain
        88 wax_off minecraft:waxed_oxidized_copper_chain minecraft:oxidized_copper_chain
        89 scrape minecraft:oxidized_copper_chain minecraft:weathered_copper_chain
        90 wax minecraft:copper_chest minecraft:waxed_copper_chest
        91 wax minecraft:copper_chest minecraft:waxed_copper_chest
        92 wax_off minecraft:waxed_copper_chest minecraft:copper_chest
        93 weather minecraft:copper_chest minecraft:exposed_copper_chest
        94 wax minecraft:exposed_copper_chest minecraft:waxed_exposed_copper_chest
        95 wax minecraft:exposed_copper_chest minecraft:waxed_exposed_copper_chest
        96 wax_off minecraft:waxed_exposed_copper_chest minecraft:exposed_copper_chest
        97 scrape minecraft:exposed_copper_chest minecraft:copper_chest
        98 weather minecraft:exposed_copper_chest minecraft:weathered_copper_chest
        99 wax minecraft:weathered_copper_chest minecraft:waxed_weathered_copper_chest
        100 wax minecraft:weathered_copper_chest minecraft:waxed_weathered_copper_chest
        101 wax_off minecraft:waxed_weathered_copper_chest minecraft:weathered_copper_chest
        102 scrape minecraft:weathered_copper_chest minecraft:exposed_copper_chest
        103 weather minecraft:weathered_copper_chest minecraft:oxidized_copper_chest
        104 wax minecraft:oxidized_copper_chest minecraft:waxed_oxidized_copper_chest
        105 wax minecraft:oxidized_copper_chest minecraft:waxed_oxidized_copper_chest
        106 wax_off minecraft:waxed_oxidized_copper_chest minecraft:oxidized_copper_chest
        107 scrape minecraft:oxidized_copper_chest minecraft:weathered_copper_chest
        108 wax minecraft:copper_door minecraft:waxed_copper_door
        109 wax minecraft:copper_door minecraft:waxed_copper_door
        110 wax_off minecraft:waxed_copper_door minecraft:copper_door
        111 weather minecraft:copper_door minecraft:exposed_copper_door
        112 wax minecraft:exposed_copper_door minecraft:waxed_exposed_copper_door
        113 wax minecraft:exposed_copper_door minecraft:waxed_exposed_copper_door
        114 wax_off minecraft:waxed_exposed_copper_door minecraft:exposed_copper_door
        115 scrape minecraft:exposed_copper_door minecraft:copper_door
        116 weather minecraft:exposed_copper_door minecraft:weathered_copper_door
        117 wax minecraft:weathered_copper_door minecraft:waxed_weathered_copper_door
        118 wax minecraft:weathered_copper_door minecraft:waxed_weathered_copper_door
        119 wax_off minecraft:waxed_weathered_copper_door minecraft:weathered_copper_door
        120 scrape minecraft:weathered_copper_door minecraft:exposed_copper_door
        121 weather minecraft:weathered_copper_door minecraft:oxidized_copper_door
        122 wax minecraft:oxidized_copper_door minecraft:waxed_oxidized_copper_door
        123 wax minecraft:oxidized_copper_door minecraft:waxed_oxidized_copper_door
        124 wax_off minecraft:waxed_oxidized_copper_door minecraft:oxidized_copper_door
        125 scrape minecraft:oxidized_copper_door minecraft:weathered_copper_door
        126 wax minecraft:copper_golem_statue minecraft:waxed_copper_golem_statue
        127 wax minecraft:copper_golem_statue minecraft:waxed_copper_golem_statue
        128 wax_off minecraft:waxed_copper_golem_statue minecraft:copper_golem_statue
        129 weather minecraft:copper_golem_statue minecraft:exposed_copper_golem_statue
        130 wax minecraft:exposed_copper_golem_statue minecraft:waxed_exposed_copper_golem_statue
        131 wax minecraft:exposed_copper_golem_statue minecraft:waxed_exposed_copper_golem_statue
        132 wax_off minecraft:waxed_exposed_copper_golem_statue minecraft:exposed_copper_golem_statue
        133 scrape minecraft:exposed_copper_golem_statue minecraft:copper_golem_statue
        134 weather minecraft:exposed_copper_golem_statue minecraft:weathered_copper_golem_statue
        135 wax minecraft:weathered_copper_golem_statue minecraft:waxed_weathered_copper_golem_statue
        136 wax minecraft:weathered_copper_golem_statue minecraft:waxed_weathered_copper_golem_statue
        137 wax_off minecraft:waxed_weathered_copper_golem_statue minecraft:weathered_copper_golem_statue
        138 scrape minecraft:weathered_copper_golem_statue minecraft:exposed_copper_golem_statue
        139 weather minecraft:weathered_copper_golem_statue minecraft:oxidized_copper_golem_statue
        140 wax minecraft:oxidized_copper_golem_statue minecraft:waxed_oxidized_copper_golem_statue
        141 wax minecraft:oxidized_copper_golem_statue minecraft:waxed_oxidized_copper_golem_statue
        142 wax_off minecraft:waxed_oxidized_copper_golem_statue minecraft:oxidized_copper_golem_statue
        143 scrape minecraft:oxidized_copper_golem_statue minecraft:weathered_copper_golem_statue
        144 wax minecraft:copper_grate minecraft:waxed_copper_grate
        145 wax minecraft:copper_grate minecraft:waxed_copper_grate
        146 wax_off minecraft:waxed_copper_grate minecraft:copper_grate
        147 weather minecraft:copper_grate minecraft:exposed_copper_grate
        148 wax minecraft:exposed_copper_grate minecraft:waxed_exposed_copper_grate
        149 wax minecraft:exposed_copper_grate minecraft:waxed_exposed_copper_grate
        150 wax_off minecraft:waxed_exposed_copper_grate minecraft:exposed_copper_grate
        151 scrape minecraft:exposed_copper_grate minecraft:copper_grate
        152 weather minecraft:exposed_copper_grate minecraft:weathered_copper_grate
        153 wax minecraft:weathered_copper_grate minecraft:waxed_weathered_copper_grate
        154 wax minecraft:weathered_copper_grate minecraft:waxed_weathered_copper_grate
        155 wax_off minecraft:waxed_weathered_copper_grate minecraft:weathered_copper_grate
        156 scrape minecraft:weathered_copper_grate minecraft:exposed_copper_grate
        157 weather minecraft:weathered_copper_grate minecraft:oxidized_copper_grate
        158 wax minecraft:oxidized_copper_grate minecraft:waxed_oxidized_copper_grate
        159 wax minecraft:oxidized_copper_grate minecraft:waxed_oxidized_copper_grate
        160 wax_off minecraft:waxed_oxidized_copper_grate minecraft:oxidized_copper_grate
        161 scrape minecraft:oxidized_copper_grate minecraft:weathered_copper_grate
        162 wax minecraft:copper_lantern minecraft:waxed_copper_lantern
        163 wax minecraft:copper_lantern minecraft:waxed_copper_lantern
        164 wax_off minecraft:waxed_copper_lantern minecraft:copper_lantern
        165 weather minecraft:copper_lantern minecraft:exposed_copper_lantern
        166 wax minecraft:exposed_copper_lantern minecraft:waxed_exposed_copper_lantern
        167 wax minecraft:exposed_copper_lantern minecraft:waxed_exposed_copper_lantern
        168 wax_off minecraft:waxed_exposed_copper_lantern minecraft:exposed_copper_lantern
        169 scrape minecraft:exposed_copper_lantern minecraft:copper_lantern
        170 weather minecraft:exposed_copper_lantern minecraft:weathered_copper_lantern
        171 wax minecraft:weathered_copper_lantern minecraft:waxed_weathered_copper_lantern
        172 wax minecraft:weathered_copper_lantern minecraft:waxed_weathered_copper_lantern
        173 wax_off minecraft:waxed_weathered_copper_lantern minecraft:weathered_copper_lantern
        174 scrape minecraft:weathered_copper_lantern minecraft:exposed_copper_lantern
        175 weather minecraft:weathered_copper_lantern minecraft:oxidized_copper_lantern
        176 wax minecraft:oxidized_copper_lantern minecraft:waxed_oxidized_copper_lantern
        177 wax minecraft:oxidized_copper_lantern minecraft:waxed_oxidized_copper_lantern
        178 wax_off minecraft:waxed_oxidized_copper_lantern minecraft:oxidized_copper_lantern
        179 scrape minecraft:oxidized_copper_lantern minecraft:weathered_copper_lantern
        180 wax minecraft:copper_trapdoor minecraft:waxed_copper_trapdoor
        181 wax minecraft:copper_trapdoor minecraft:waxed_copper_trapdoor
        182 wax_off minecraft:waxed_copper_trapdoor minecraft:copper_trapdoor
        183 weather minecraft:copper_trapdoor minecraft:exposed_copper_trapdoor
        184 wax minecraft:exposed_copper_trapdoor minecraft:waxed_exposed_copper_trapdoor
        185 wax minecraft:exposed_copper_trapdoor minecraft:waxed_exposed_copper_trapdoor
        186 wax_off minecraft:waxed_exposed_copper_trapdoor minecraft:exposed_copper_trapdoor
        187 scrape minecraft:exposed_copper_trapdoor minecraft:copper_trapdoor
        188 weather minecraft:exposed_copper_trapdoor minecraft:weathered_copper_trapdoor
        189 wax minecraft:weathered_copper_trapdoor minecraft:waxed_weathered_copper_trapdoor
        190 wax minecraft:weathered_copper_trapdoor minecraft:waxed_weathered_copper_trapdoor
        191 wax_off minecraft:waxed_weathered_copper_trapdoor minecraft:weathered_copper_trapdoor
        192 scrape minecraft:weathered_copper_trapdoor minecraft:exposed_copper_trapdoor
        193 weather minecraft:weathered_copper_trapdoor minecraft:oxidized_copper_trapdoor
        194 wax minecraft:oxidized_copper_trapdoor minecraft:waxed_oxidized_copper_trapdoor
        195 wax minecraft:oxidized_copper_trapdoor minecraft:waxed_oxidized_copper_trapdoor
        196 wax_off minecraft:waxed_oxidized_copper_trapdoor minecraft:oxidized_copper_trapdoor
        197 scrape minecraft:oxidized_copper_trapdoor minecraft:weathered_copper_trapdoor
        198 wax minecraft:cut_copper minecraft:waxed_cut_copper
        199 wax minecraft:cut_copper minecraft:waxed_cut_copper
        200 wax_off minecraft:waxed_cut_copper minecraft:cut_copper
        201 weather minecraft:cut_copper minecraft:exposed_cut_copper
        202 wax minecraft:exposed_cut_copper minecraft:waxed_exposed_cut_copper
        203 wax minecraft:exposed_cut_copper minecraft:waxed_exposed_cut_copper
        204 wax_off minecraft:waxed_exposed_cut_copper minecraft:exposed_cut_copper
        205 scrape minecraft:exposed_cut_copper minecraft:cut_copper
        206 weather minecraft:exposed_cut_copper minecraft:weathered_cut_copper
        207 wax minecraft:weathered_cut_copper minecraft:waxed_weathered_cut_copper
        208 wax minecraft:weathered_cut_copper minecraft:waxed_weathered_cut_copper
        209 wax_off minecraft:waxed_weathered_cut_copper minecraft:weathered_cut_copper
        210 scrape minecraft:weathered_cut_copper minecraft:exposed_cut_copper
        211 weather minecraft:weathered_cut_copper minecraft:oxidized_cut_copper
        212 wax minecraft:oxidized_cut_copper minecraft:waxed_oxidized_cut_copper
        213 wax minecraft:oxidized_cut_copper minecraft:waxed_oxidized_cut_copper
        214 wax_off minecraft:waxed_oxidized_cut_copper minecraft:oxidized_cut_copper
        215 scrape minecraft:oxidized_cut_copper minecraft:weathered_cut_copper
        216 wax minecraft:cut_copper_slab minecraft:waxed_cut_copper_slab
        217 wax minecraft:cut_copper_slab minecraft:waxed_cut_copper_slab
        218 wax_off minecraft:waxed_cut_copper_slab minecraft:cut_copper_slab
        219 weather minecraft:cut_copper_slab minecraft:exposed_cut_copper_slab
        220 wax minecraft:exposed_cut_copper_slab minecraft:waxed_exposed_cut_copper_slab
        221 wax minecraft:exposed_cut_copper_slab minecraft:waxed_exposed_cut_copper_slab
        222 wax_off minecraft:waxed_exposed_cut_copper_slab minecraft:exposed_cut_copper_slab
        223 scrape minecraft:exposed_cut_copper_slab minecraft:cut_copper_slab
        224 weather minecraft:exposed_cut_copper_slab minecraft:weathered_cut_copper_slab
        225 wax minecraft:weathered_cut_copper_slab minecraft:waxed_weathered_cut_copper_slab
        226 wax minecraft:weathered_cut_copper_slab minecraft:waxed_weathered_cut_copper_slab
        227 wax_off minecraft:waxed_weathered_cut_copper_slab minecraft:weathered_cut_copper_slab
        228 scrape minecraft:weathered_cut_copper_slab minecraft:exposed_cut_copper_slab
        229 weather minecraft:weathered_cut_copper_slab minecraft:oxidized_cut_copper_slab
        230 wax minecraft:oxidized_cut_copper_slab minecraft:waxed_oxidized_cut_copper_slab
        231 wax minecraft:oxidized_cut_copper_slab minecraft:waxed_oxidized_cut_copper_slab
        232 wax_off minecraft:waxed_oxidized_cut_copper_slab minecraft:oxidized_cut_copper_slab
        233 scrape minecraft:oxidized_cut_copper_slab minecraft:weathered_cut_copper_slab
        234 wax minecraft:cut_copper_stairs minecraft:waxed_cut_copper_stairs
        235 wax minecraft:cut_copper_stairs minecraft:waxed_cut_copper_stairs
        236 wax_off minecraft:waxed_cut_copper_stairs minecraft:cut_copper_stairs
        237 weather minecraft:cut_copper_stairs minecraft:exposed_cut_copper_stairs
        238 wax minecraft:exposed_cut_copper_stairs minecraft:waxed_exposed_cut_copper_stairs
        239 wax minecraft:exposed_cut_copper_stairs minecraft:waxed_exposed_cut_copper_stairs
        240 wax_off minecraft:waxed_exposed_cut_copper_stairs minecraft:exposed_cut_copper_stairs
        241 scrape minecraft:exposed_cut_copper_stairs minecraft:cut_copper_stairs
        242 weather minecraft:exposed_cut_copper_stairs minecraft:weathered_cut_copper_stairs
        243 wax minecraft:weathered_cut_copper_stairs minecraft:waxed_weathered_cut_copper_stairs
        244 wax minecraft:weathered_cut_copper_stairs minecraft:waxed_weathered_cut_copper_stairs
        245 wax_off minecraft:waxed_weathered_cut_copper_stairs minecraft:weathered_cut_copper_stairs
        246 scrape minecraft:weathered_cut_copper_stairs minecraft:exposed_cut_copper_stairs
        247 weather minecraft:weathered_cut_copper_stairs minecraft:oxidized_cut_copper_stairs
        248 wax minecraft:oxidized_cut_copper_stairs minecraft:waxed_oxidized_cut_copper_stairs
        249 wax minecraft:oxidized_cut_copper_stairs minecraft:waxed_oxidized_cut_copper_stairs
        250 wax_off minecraft:waxed_oxidized_cut_copper_stairs minecraft:oxidized_cut_copper_stairs
        251 scrape minecraft:oxidized_cut_copper_stairs minecraft:weathered_cut_copper_stairs
        252 wax minecraft:lightning_rod minecraft:waxed_lightning_rod
        253 wax minecraft:lightning_rod minecraft:waxed_lightning_rod
        254 wax_off minecraft:waxed_lightning_rod minecraft:lightning_rod
        255 weather minecraft:lightning_rod minecraft:exposed_lightning_rod
        256 wax minecraft:exposed_lightning_rod minecraft:waxed_exposed_lightning_rod
        257 wax minecraft:exposed_lightning_rod minecraft:waxed_exposed_lightning_rod
        258 wax_off minecraft:waxed_exposed_lightning_rod minecraft:exposed_lightning_rod
        259 scrape minecraft:exposed_lightning_rod minecraft:lightning_rod
        260 weather minecraft:exposed_lightning_rod minecraft:weathered_lightning_rod
        261 wax minecraft:weathered_lightning_rod minecraft:waxed_weathered_lightning_rod
        262 wax minecraft:weathered_lightning_rod minecraft:waxed_weathered_lightning_rod
        263 wax_off minecraft:waxed_weathered_lightning_rod minecraft:weathered_lightning_rod
        264 scrape minecraft:weathered_lightning_rod minecraft:exposed_lightning_rod
        265 weather minecraft:weathered_lightning_rod minecraft:oxidized_lightning_rod
        266 wax minecraft:oxidized_lightning_rod minecraft:waxed_oxidized_lightning_rod
        267 wax minecraft:oxidized_lightning_rod minecraft:waxed_oxidized_lightning_rod
        268 wax_off minecraft:waxed_oxidized_lightning_rod minecraft:oxidized_lightning_rod
        269 scrape minecraft:oxidized_lightning_rod minecraft:weathered_lightning_rod
        """;
    static Stream<String> edges() { return EDGE_ORACLE.strip().lines(); }
    @ParameterizedTest @MethodSource("edges")
    void sealedP3NativeTransitionIdentity(String edge) {
        String[] p=edge.split(" ");Block from=BuiltInRegistries.BLOCK.getValue(Identifier.parse(p[2]));Block to=BuiltInRegistries.BLOCK.getValue(Identifier.parse(p[3]));assertNotNull(from);assertNotNull(to);
        Optional<BlockState> actual=switch(p[1]) {
            case "wax" -> HoneycombItem.getWaxed(from.defaultBlockState());
            case "wax_off" -> Optional.ofNullable(HoneycombItem.WAX_OFF_BY_BLOCK.get().get(from)).map(Block::defaultBlockState);
            case "scrape" -> WeatheringCopper.getPrevious(from.defaultBlockState());
            case "weather" -> WeatheringCopper.getNext(from).map(Block::defaultBlockState);
            default -> throw new AssertionError(edge);
        };
        assertEquals(to,actual.orElseThrow().getBlock(),"P3 original index "+p[0]);
    }
    /** Real Fabric SERVER Knot transformation and production-generated normal fixture. */
    public static void main(String[] args) throws Exception {
        System.setProperty("java.class.path",Files.readString(Path.of(System.getProperty("phaseD.probe.classpath"))));
        var knot=new net.fabricmc.loader.impl.launch.knot.Knot(net.fabricmc.api.EnvType.SERVER);
        ClassLoader loader=knot.init(new String[]{"--nogui","--universe",args[0]});
        Class.forName("net.minecraft.SharedConstants",true,loader).getMethod("tryDetectVersion").invoke(null);
        Class.forName("net.minecraft.server.Bootstrap",true,loader).getMethod("bootStrap").invoke(null);
        for(String owner:List.of("net.minecraft.world.item.AxeItem","net.minecraft.world.item.HoneycombItem","net.minecraft.world.entity.animal.golem.CopperGolem","net.minecraft.world.level.block.CopperGolemStatueBlock","net.minecraft.world.level.block.WeatheringCopperGolemStatueBlock")) {
            Class.forName(owner,true,loader).getDeclaredMethods();System.out.println("D_S05_TRANSFORMED_OWNER "+owner+" PASS");
        }
        Class<?> origin=Class.forName("com.diskree.achievetodo.injection.extension.main.LevelInfoExtension$ConfigurationOrigin",true,loader);
        Object fresh=Arrays.stream(origin.getEnumConstants()).filter(v -> v.toString().equals("FRESH")).findFirst().orElseThrow();
        Class<?> io=Class.forName("com.diskree.achievetodo.injection.extension.main.LevelInfoExtension$ConfigurationIO",true,loader);
        Object generated=io.getMethod("load",Path.class,String.class,long.class,origin).invoke(null,Path.of(args[0]).resolve("config/achievetodo"),"normal",0L,fresh);
        System.out.println("D_S05_TRANSFORMED_SMOKE PASS abilities="+((Map<?,?>)generated).size()+" gameplay=NOT_RUN");
    }
}
