package com.diskree.achievetodo.ability;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

/** Independent sealed P8 all_registered_controls oracle. Static identity/scope
 * evidence only; world generation and saved metadata require separate server cases. */
class PhaseDRuinedPortalSelectorTest {
    private static final String P8 = """
minecraft:pillager_outpost PILLAGER_OUTPOST
minecraft:mineshaft MINESHAFT
minecraft:mineshaft_mesa MINESHAFT
minecraft:mansion MANSION
minecraft:jungle_pyramid JUNGLE_PYRAMID
minecraft:desert_pyramid DESERT_PYRAMID
minecraft:igloo IGLOO
minecraft:shipwreck SHIPWRECK
minecraft:shipwreck_beached SHIPWRECK
minecraft:swamp_hut SWAMP_HUT
minecraft:stronghold STRONGHOLD
minecraft:monument MONUMENT
minecraft:ocean_ruin_cold OCEAN_RUIN
minecraft:ocean_ruin_warm OCEAN_RUIN
minecraft:fortress FORTRESS
minecraft:nether_fossil -
minecraft:end_city END_CITY
minecraft:buried_treasure BURIED_TREASURE
minecraft:bastion_remnant BASTION_REMNANT
minecraft:village_plains VILLAGE
minecraft:village_desert VILLAGE
minecraft:village_savanna VILLAGE
minecraft:village_snowy VILLAGE
minecraft:village_taiga VILLAGE
minecraft:ruined_portal RUINED_PORTAL
minecraft:ruined_portal_desert RUINED_PORTAL
minecraft:ruined_portal_jungle RUINED_PORTAL
minecraft:ruined_portal_swamp RUINED_PORTAL
minecraft:ruined_portal_mountain RUINED_PORTAL
minecraft:ruined_portal_ocean RUINED_PORTAL
minecraft:ruined_portal_nether RUINED_PORTAL
minecraft:ancient_city ANCIENT_CITY
minecraft:trail_ruins TRAIL_RUINS
minecraft:trial_chambers TRIAL_CHAMBERS
        """;
    private static final String NO_DELTA = """
src/main/java/com/diskree/achievetodo/injection/mixin/main/StructureMixin.java 01830b830db06e6c2471dede795eeab517a61b9b770db513bbc1da6efb6a116b
src/main/java/com/diskree/achievetodo/injection/mixin/main/StructureStartMixin.java 2bb9bcb0f6efebdc0f605bdc1bcbfe40d21dd17cade7280cb1d89e0019d56252
src/main/java/com/diskree/achievetodo/injection/mixin/main/ChunkRegionMixin.java 646cfbcffd4e0d6ba578c7c8375cbd370d4943f14b6b844387aa9aa54063dd90
src/main/java/com/diskree/achievetodo/injection/mixin/main/PlayerEntityMixin.java bb614f0bb4c53f65e9562f21d546cf3552de6e3551bc33d958d42b6fbc1d7159
src/main/java/com/diskree/achievetodo/server/AchieveToDoServer.java 0aff4191700d951a5cc3139d4f90914f774495b29c0362d1bebc429177fd22ea
src/main/java/com/diskree/achievetodo/AchieveToDoMod.java 563dafc9250183b101968cf8b49b83d72f9c9ddab66b666c13e79632c272cd73
src/main/java/com/diskree/achievetodo/ability/DimensionalBlockBox.java 8b97bbf5b505e13b56f211fd1498b0553b4161874213d1121e5bb1eb2f866d33
src/main/java/com/diskree/achievetodo/ability/AbilityType.java e9744605f3e0699cfbb0ef2d25d428c18cb5625644eeb9f21cb62e3722f1f030
src/main/java/com/diskree/achievetodo/ability/Progressions.java fbf656576a22c0d12e229f49a179f9692b1a34c3c3991daa755157fd6c89f30a
        """;
    @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }
    static Stream<String> nativeIds() { return P8.lines().map(String::trim).filter(s -> !s.isEmpty()); }
    @ParameterizedTest @MethodSource("nativeIds")
    void all34IndependentNativeIdentities(String row) throws Exception {
        String[] cells=row.split(" ");
        var key=ResourceKey.create(Registries.STRUCTURE,Identifier.parse(cells[0]));
        var expected=cells[1].equals("-")?null:LandmarkType.valueOf(cells[1]);
        assertSame(expected,LandmarkType.findByStructureRegistryKey(key),cells[0]);
        assertEquals(1,Arrays.stream(BuiltinStructures.class.getFields()).filter(f -> {
            try { return key.equals(f.get(null)); } catch(ReflectiveOperationException e) { throw new AssertionError(e); }
        }).count(),"Actual native constant identity");
    }
    @Test void exactSevenKeysAndOriginalFamilyOrdinal() {
        assertEquals(22,LandmarkType.values().length);assertEquals(8,LandmarkType.RUINED_PORTAL.ordinal());
        assertTrue(LandmarkType.RUINED_PORTAL.isStructure());assertFalse(LandmarkType.RUINED_PORTAL.isFeature());
        assertEquals(Set.of(BuiltinStructures.RUINED_PORTAL_STANDARD,BuiltinStructures.RUINED_PORTAL_DESERT,
            BuiltinStructures.RUINED_PORTAL_JUNGLE,BuiltinStructures.RUINED_PORTAL_SWAMP,
            BuiltinStructures.RUINED_PORTAL_MOUNTAIN,BuiltinStructures.RUINED_PORTAL_OCEAN,
            BuiltinStructures.RUINED_PORTAL_NETHER),LandmarkType.RUINED_PORTAL.getStructureRegistryKeys());
        assertEquals(33,Arrays.stream(LandmarkType.values()).filter(LandmarkType::isStructure).mapToInt(t -> t.getStructureRegistryKeys().size()).sum());
        assertEquals(34,nativeIds().count());assertNull(LandmarkType.findByStructureRegistryKey(BuiltinStructures.NETHER_FOSSIL));
    }
    @Test void originalSixSourceArgumentsRemainOrderedAndNetherIsAppendOnly() throws Exception {
        String source=Files.readString(Path.of("src/main/java/com/diskree/achievetodo/ability/LandmarkType.java"));
        String family=source.substring(source.indexOf("    RUINED_PORTAL("),source.indexOf("    BURIED_TREASURE("));
        assertEquals(List.of("RUINED_PORTAL_STANDARD","RUINED_PORTAL_DESERT","RUINED_PORTAL_JUNGLE",
            "RUINED_PORTAL_SWAMP","RUINED_PORTAL_MOUNTAIN","RUINED_PORTAL_OCEAN","RUINED_PORTAL_NETHER"),
            java.util.regex.Pattern.compile("BuiltinStructures\\.([A-Z_]+)").matcher(family).results().map(m -> m.group(1)).toList());
    }
    @Test void featureAndNonstructureMappingsRemainExact() {
        assertEquals(Map.of(Feature.DESERT_WELL,LandmarkType.DESERT_WELL,Feature.MONSTER_ROOM,LandmarkType.MONSTER_ROOM),LandmarkType.FEATURES);
        assertEquals(2,Arrays.stream(LandmarkType.values()).filter(LandmarkType::isFeature).count());
        assertNull(LandmarkType.DESERT_WELL.getStructureRegistryKeys());assertNull(LandmarkType.MONSTER_ROOM.getStructureRegistryKeys());
        assertFalse(LandmarkType.DESERT_WELL.isStructure());assertFalse(LandmarkType.MONSTER_ROOM.isStructure());
    }
    @Test void nullUnknownAndNameLookupRemainExact() {
        assertNull(LandmarkType.findByStructureRegistryKey(null));assertNull(LandmarkType.findByStructureRegistryKey(ResourceKey.<Structure>create(Registries.STRUCTURE,Identifier.parse("achievetodo:unknown"))));
        assertNull(LandmarkType.findByName(null));assertNull(LandmarkType.findByName(""));assertNull(LandmarkType.findByName("ruined_portal_nether"));
        assertSame(LandmarkType.RUINED_PORTAL,LandmarkType.findByName("RuInEd_PoRtAl"));
    }
    @Test void originalAbilityCategoryAndThresholdSourcesStayFrozen() {
        assertEquals(153,AbilityType.values().length);assertEquals(35,AbilityType.INTERACT_INSIDE_RUINED_PORTAL.ordinal());
        assertSame(AbilityType.INTERACT_INSIDE_RUINED_PORTAL,AbilityType.findByLandmarkType(LandmarkType.RUINED_PORTAL));
        assertEquals("interact_inside_ruined_portal",AbilityType.INTERACT_INSIDE_RUINED_PORTAL.getName());
        assertSame(AbilitiesHierarchyLayerType.LANDMARK,AbilityType.INTERACT_INSIDE_RUINED_PORTAL.getHierarchyLayerType());
        assertEquals(151,AbilityType.USE_COPPER_TOOLS.ordinal());assertEquals(152,AbilityType.EQUIP_COPPER_ARMOR.ordinal());
    }
    @Test void noDeltaWorldgenGeometryIndexActionsAndLegacyReconstructionScope() throws Exception {
        for(String row:NO_DELTA.lines().map(String::trim).filter(s -> !s.isEmpty()).toList()) {
            String[] c=row.split(" ");assertEquals(c[1],HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(Path.of(c[0])))),c[0]);
        }
        String read=Files.readString(Path.of("src/main/java/com/diskree/achievetodo/injection/mixin/main/StructureStartMixin.java"));
        assertTrue(read.contains("nbt.getCompoundOrEmpty(Constants.NbtKey.STRUCTURE_LANDMARK)"));
        assertTrue(read.contains("landmarkNbt.contains(Constants.NbtKey.LANDMARK_TYPE)"));
        assertFalse(read.contains("original.getBoundingBox()"));
    }
    @Test void firstMatchImmediateFalseAndBothNonNullLoadGuardsStayFrozen() throws Exception {
        String s=Files.readString(Path.of("src/main/java/com/diskree/achievetodo/server/AchieveToDoServer.java"));
        String permission=s.substring(s.indexOf("        BoundingBox targetBlockBox = Utils.toBlockBox(targetBox);"),s.indexOf("    protected void notifyLockedLandmark"));
        assertTrue(permission.contains("return isAbilityLocked;"));assertFalse(permission.contains("if (isAbilityLocked) {\n                                    return"));
        assertTrue(s.contains("landmarkType != null && landmarkBlockBox != null"));
        assertTrue(permission.contains("dimensionalBlockBox.dimensionType() == targetDimensionType"));
    }
}
