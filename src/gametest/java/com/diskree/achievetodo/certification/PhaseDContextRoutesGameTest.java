package com.diskree.achievetodo.certification;

import com.diskree.achievetodo.ability.LandmarkType;
import com.diskree.achievetodo.ability.PhaseDAbilityMappingTest.CaseContract;
import com.diskree.achievetodo.ability.PhaseDAbilityMappingTest.Outcome;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;

import java.util.List;
import java.util.Map;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.server.AchieveToDoServer;
import com.diskree.achievetodo.ability.*;
import com.diskree.achievetodo.injection.extension.main.MountInventoryMenuExtension;
import com.google.gson.Gson;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.network.HashedStack;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.nautilus.Nautilus;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.*;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import java.lang.reflect.Field;
import java.util.*;

import java.util.List;
import java.util.Map;

import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.nbt.NbtIo;
import java.nio.file.*;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.monster.cubemob.SulfurCube;
import net.minecraft.world.entity.SulfurCubeArchetypes;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.util.ProblemReporter;

public final class PhaseDContextRoutesGameTest {
    public static final List<CaseContract> FUTURE_CASES = List.of(
        PhaseDAbilityRoutesGameTest.future("D-S07-LEGACY-01-A", "PE-10/PE-11", "CH-09", "D-S07", Outcome.RUNTIME_REQUIRED,
            "P10 p10_runtime_certification_plan.md structure lifecycle; P8:p8_future_regression_matrix.json; LEGACY-01 addendum",
            "Natural Nether portal generation; saved ATD type/box load/index; exact P10 P8 native references",
            "Disposable new world / previously created world; previously generated valid tagged and untagged starts",
            "New generation tracked; old valid metadata retained; missing metadata never reconstructed",
            "INTERACT_INSIDE_RUINED_PORTAL",
            Map.of("automatic_migration_count", "0", "old_untagged_metadata_invented", "false"),
            List.of("New generation in both new and existing worlds", "Valid old tracking survives chunk reload/restart",
                "Absent/malformed old geometry has no invented hull or global scan")),
        PhaseDAbilityRoutesGameTest.future("CP-NAMING-CONDITIONAL", "PE-03", "CH-12", "D-S04", Outcome.CONDITIONAL_ROUTE,
            "P10 p10_cross_stage_conflicts_and_errata.md CP-NAMING; no original required CH promotion",
            "P7 valid NameTag Creaking important-item seam; explicit supplemental authority required",
            "Joined actual initiating player", "Native valid naming separate from generic Entity.interact",
            "Target landmark, if separately authorized", Map.of("unauthorized_activation", "false"),
            List.of("Excluded until explicit supplemental scope; D-S04 acceptance alone is insufficient")));

    /** Reuse these existing methods through their original registration, without wrapping or copying them. */
    public static final List<String> PHASE_C_REUSE = List.of("selectedSoloScoreRemoval", "removalRoutesAndSurvivors",
        "ordinaryThresholdCrossings", "allTeamModes", "structuralTeamChanges", "objectiveReplacement", "resetAndRebuild");

    @GameTest(maxTicks = 20)
    public void originalPortalSelectorAndPendingContracts(GameTestHelper helper) {
        PhaseDAbilityRoutesGameTest.runControl(helper, PhaseDAbilityRoutesGameTest.control(
            "D-S00-G-CONTEXT-CONTROL", Map.of("standard", "RUINED_PORTAL", "nether_baseline", "RUINED_PORTAL",
                "reusable_phase_c_methods", "7")), () -> Map.of(
            "standard", LandmarkType.findByStructureRegistryKey(BuiltinStructures.RUINED_PORTAL_STANDARD).name(),
            "nether_baseline", LandmarkType.findByStructureRegistryKey(BuiltinStructures.RUINED_PORTAL_NETHER).name(),
            "reusable_phase_c_methods", Integer.toString(PHASE_C_REUSE.size())));
    }

    /** D-S07 controls are separate from the immutable FUTURE_CASES planning rows. */
    @GameTest(maxTicks=200) public void s07SelectorAndSyntheticLifecycle(GameTestHelper helper) {
        run(helper,"s07SelectorAndSyntheticLifecycle",() -> {
            var registry=helper.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE);
            for(var key:List.of(BuiltinStructures.RUINED_PORTAL_STANDARD,BuiltinStructures.RUINED_PORTAL_DESERT,
                BuiltinStructures.RUINED_PORTAL_JUNGLE,BuiltinStructures.RUINED_PORTAL_SWAMP,
                BuiltinStructures.RUINED_PORTAL_MOUNTAIN,BuiltinStructures.RUINED_PORTAL_OCEAN,BuiltinStructures.RUINED_PORTAL_NETHER)) {
                var holder=registry.getOrThrow(key);
                require("S07 actual native holder identity",holder.unwrapKey().orElseThrow()==key,key.identifier());
                require("S07 exact existing family",LandmarkType.findByStructureRegistryKey(key)==LandmarkType.RUINED_PORTAL,key.identifier());
            }
            require("S07 unrelated native mappings",LandmarkType.findByStructureRegistryKey(BuiltinStructures.NETHER_FOSSIL)==null&&LandmarkType.findByStructureRegistryKey(BuiltinStructures.FORTRESS)==LandmarkType.FORTRESS,"fossil/fortress");
            require("S07 family ability unchanged",AbilityType.findByLandmarkType(LandmarkType.RUINED_PORTAL)==AbilityType.INTERACT_INSIDE_RUINED_PORTAL,AbilityType.INTERACT_INSIDE_RUINED_PORTAL);
            var f=join();var ability=AbilityType.INTERACT_INSIDE_RUINED_PORTAL;
            config().put(ability,20000);score(f,10000);boxes().clear();membership().clear();membership().put(LandmarkType.RUINED_PORTAL,new HashSet<>(Set.of(f.player().getUUID())));
            var pos=helper.absolutePos(new BlockPos(3,2,3));var cp=new ChunkPos(pos.getX()>>4,pos.getZ()>>4);var box=new BoundingBox(pos);
            var netherBox=new DimensionalBlockBox(DimensionType.NETHER,box);
            var payload=Map.of(LandmarkType.RUINED_PORTAL,Set.of(netherBox));var target=new AABB(pos);
            owner().onLandmarksLoadedStatusChanged(helper.getLevel(),cp,payload,true);
            owner().onLandmarksLoadedStatusChanged(helper.getLevel(),cp,payload,true);
            require("S07 synthetic duplicate load is idempotent",boxes().get(cp).get(LandmarkType.RUINED_PORTAL).size()==1,boxes());
            require("S07 synthetic Nether dimension denied",owner().isTargetInLockedLandmark(f.player(),DimensionType.NETHER,target,true),target);
            require("S07 synthetic same coordinates other dimension allowed",!owner().isTargetInLockedLandmark(f.player(),DimensionType.OVERWORLD,target,true),target);
            owner().onLandmarksLoadedStatusChanged(helper.getLevel(),cp,payload,false);
            require("S07 synthetic remove has no stale bucket",!boxes().containsKey(cp)&&!owner().isTargetInLockedLandmark(f.player(),DimensionType.NETHER,target,true),boxes());
            owner().onLandmarksLoadedStatusChanged(helper.getLevel(),cp,payload,true);
            require("S07 synthetic reindex restores denial",owner().isTargetInLockedLandmark(f.player(),DimensionType.NETHER,target,true),boxes());
            // Deliberately stale membership models the existing FIRST-MATCH contract:
            // a qualifying allowed entry must return false before a later locked one.
            membership().clear();membership().put(LandmarkType.RUINED_PORTAL,new HashSet<>(Set.of(f.player().getUUID())));
            membership().put(LandmarkType.FORTRESS,new HashSet<>(Set.of(f.player().getUUID())));
            boxes().get(cp).put(LandmarkType.FORTRESS,new HashSet<>(Set.of(netherBox)));
            var first=membership().keySet().iterator().next();var second=first==LandmarkType.FORTRESS?LandmarkType.RUINED_PORTAL:LandmarkType.FORTRESS;
            config().put(AbilityType.findByLandmarkType(first),1);config().put(AbilityType.findByLandmarkType(second),20000);
            require("S07 synthetic FIRST-MATCH immediate false",!owner().isTargetInLockedLandmark(f.player(),DimensionType.NETHER,target,true),first);
            config().put(AbilityType.findByLandmarkType(first),20000);config().put(AbilityType.findByLandmarkType(second),1);
            require("S07 synthetic FIRST-MATCH immediate true",owner().isTargetInLockedLandmark(f.player(),DimensionType.NETHER,target,true),first);
            // Actual native interactions, with explicitly synthetic tracked bounds.
            boxes().clear();membership().clear();config().put(ability,20000);config().put(AbilityType.USE_WATER_BUCKET,1);score(f,30000);score(f,10000);
            pos=put(3,2,3,Blocks.CAULDRON.defaultBlockState());
            var actionPayload=Map.of(LandmarkType.RUINED_PORTAL,Set.of(new DimensionalBlockBox(DimensionType.OVERWORLD,new BoundingBox(pos))));
            owner().onLandmarksLoadedStatusChanged(helper.getLevel(),cp,actionPayload,true);
            hands(f.player(),InteractionHand.MAIN_HAND,Items.WATER_BUCKET);observe(f);var progress=criteria(f);var before=shelfState(f,List.of(pos));
            require("S07 synthetic portal bounds deny native-valid cauldron action",shelfUse(f,pos,InteractionHand.MAIN_HAND,Direction.UP)==InteractionResult.FAIL,pos);
            require("S07 native denial conserves cauldron and inventory",shelfState(f,List.of(pos)).equals(before),before);noEffects(f,progress,"S07 portal target denial");
            hands(f.player(),InteractionHand.MAIN_HAND,Items.DIAMOND);observe(f);before=shelfState(f,List.of(pos));
            require("S07 native-ineligible cauldron action remains PASS",shelfUse(f,pos,InteractionHand.MAIN_HAND,Direction.UP)==InteractionResult.PASS,pos);
            var packets=observe(f);require("S07 native-ineligible action no new gate or mutation",feedback(packets)==0&&notifications(packets)==0&&shelfState(f,List.of(pos)).equals(before),packets.size());
            score(f,30000);hands(f.player(),InteractionHand.MAIN_HAND,Items.WATER_BUCKET);observe(f);
            require("S07 unlocked native-valid cauldron action succeeds",shelfUse(f,pos,InteractionHand.MAIN_HAND,Direction.UP).consumesAction()&&helper.getLevel().getBlockState(pos).is(Blocks.WATER_CAULDRON)&&f.player().getMainHandItem().is(Items.BUCKET),pos);observe(f);
            System.out.println("D_S07_OBSERVED "+new Gson().toJson(Map.of("case","selector_synthetic_lifecycle","kind","LIVE_SERVER_SELECTOR_AND_SYNTHETIC_GEOMETRY","natural_generation",false,"chunk_reload",false)));
        });
    }

    private net.minecraft.world.level.levelgen.structure.StructureStart s07GenerateControl(net.minecraft.server.level.ServerLevel level) {
        return s07GenerateControl(level,true);
    }
    private net.minecraft.world.level.levelgen.structure.StructureStart s07GenerateControl(net.minecraft.server.level.ServerLevel level,boolean acceptedBiome) {
        var holder=level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(BuiltinStructures.RUINED_PORTAL_NETHER);
        var source=level.getChunkSource();var generator=source.getGenerator();
        // Explicit biome-accepting native generation control. This proves successful
        // RETURN tagging, not natural placement, terrain or generated-position extent.
        return holder.value().generate(holder,level.dimension(),level.registryAccess(),generator,generator.getBiomeSource(),
            source.randomState(),level.getStructureManager(),level.getSeed(),new ChunkPos(0,0),0,level,b -> acceptedBiome);
    }
    private com.diskree.achievetodo.injection.extension.main.StructureStartExtension s07Extension(net.minecraft.world.level.levelgen.structure.StructureStart start) {
        return (com.diskree.achievetodo.injection.extension.main.StructureStartExtension)(Object)start;
    }
    private net.minecraft.nbt.CompoundTag s07Disk(String name,net.minecraft.nbt.CompoundTag tag) throws Exception {
        var root=java.nio.file.Path.of("s07-metadata");java.nio.file.Files.createDirectories(root);
        var path=root.resolve(name+".nbt");require("S07 disposable metadata path new",!java.nio.file.Files.exists(path),path);
        net.minecraft.nbt.NbtIo.writeCompressed(tag,path);
        var read=net.minecraft.nbt.NbtIo.readCompressed(path,net.minecraft.nbt.NbtAccounter.unlimitedHeap());
        require("S07 disk codec exact tag readback",tag.equals(read),name);return read;
    }
    @GameTest(maxTicks=200) public void s07MetadataCodecControls(GameTestHelper helper) {
        run(helper,"s07MetadataCodecControls",() -> {
            var level=helper.getLevel();var start=s07GenerateControl(level);var ext=s07Extension(start);
            require("S07 native generation control valid",start.isValid(),start.getPieces().size());
            require("S07 native successful return tags real Nether key",ext.achievetodo$getLandmarkType()==LandmarkType.RUINED_PORTAL,start.getStructure());
            require("S07 generation alone has no fabricated tracked hull",ext.achievetodo$getLandmarkBlockBox()==null,start.getBoundingBox());
            var rejected=s07GenerateControl(level,false);
            require("S07 native biome-rejected generation remains invalid and untagged",!rejected.isValid()&&s07Extension(rejected).achievetodo$getLandmarkType()==null&&s07Extension(rejected).achievetodo$getLandmarkBlockBox()==null,"native INVALID_START");
            var context=net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext.fromLevel(level);
            // Explicit SYNTHETIC saved geometry tests the unchanged persistence
            // codec, never claims an accepted historical world's original extent.
            var savedBox=new BoundingBox(10,31,12,17,43,19);ext.achievetodo$setLandmarkBlockBox(savedBox);
            var valid=start.createTag(context,start.getChunkPos());var frozen=valid.copy();
            var loaded=net.minecraft.world.level.levelgen.structure.StructureStart.loadStaticStart(context,s07Disk("synthetic-valid",valid),level.getSeed());
            require("S07 valid codec preserves saved family",loaded!=null&&s07Extension(loaded).achievetodo$getLandmarkType()==LandmarkType.RUINED_PORTAL,"ruined_portal");
            require("S07 valid codec preserves saved geometry without native hull replacement",s07Extension(loaded).achievetodo$getLandmarkBlockBox().equals(savedBox),savedBox);
            require("S07 valid codec original tag untouched",valid.equals(frozen),valid);
            require("S07 second codec save preserves ATD metadata",loaded.createTag(context,loaded.getChunkPos()).getCompoundOrEmpty("achievetodo_landmark").equals(valid.getCompoundOrEmpty("achievetodo_landmark")),valid.getCompoundOrEmpty("achievetodo_landmark"));
            var untagged=valid.copy();untagged.remove("achievetodo_landmark");var unchanged=untagged.copy();
            var old=net.minecraft.world.level.levelgen.structure.StructureStart.loadStaticStart(context,s07Disk("synthetic-untagged",untagged),level.getSeed());
            require("S07 LEGACY-01/A untagged native start no reconstruction",old!=null&&old.isValid()&&s07Extension(old).achievetodo$getLandmarkType()==null&&s07Extension(old).achievetodo$getLandmarkBlockBox()==null,old);
            require("S07 untagged input tag unchanged",untagged.equals(unchanged),untagged);
            require("S07 untagged codec no speculative ATD metadata",!old.createTag(context,old.getChunkPos()).contains("achievetodo_landmark"),old.getBoundingBox());
            var typeOnly=valid.copy();var typeTag=new net.minecraft.nbt.CompoundTag();typeTag.putString("landmarkType","ruined_portal");typeOnly.put("achievetodo_landmark",typeTag);
            var incomplete=net.minecraft.world.level.levelgen.structure.StructureStart.loadStaticStart(context,s07Disk("synthetic-type-only",typeOnly),level.getSeed());
            require("S07 type-only existing behavior leaves box absent",s07Extension(incomplete).achievetodo$getLandmarkType()==LandmarkType.RUINED_PORTAL&&s07Extension(incomplete).achievetodo$getLandmarkBlockBox()==null,"no minX");
            var unknown=valid.copy();unknown.getCompoundOrEmpty("achievetodo_landmark").putString("landmarkType","unknown_landmark");
            var malformed=net.minecraft.world.level.levelgen.structure.StructureStart.loadStaticStart(context,s07Disk("synthetic-unknown-type",unknown),level.getSeed());
            require("S07 unknown family does not invent geometry",s07Extension(malformed).achievetodo$getLandmarkType()==null&&s07Extension(malformed).achievetodo$getLandmarkBlockBox()==null,"unknown family");
            var nonexistent=valid.copy();nonexistent.putString("id","minecraft:nonexistent_s07");
            require("S07 nonexistent saved structure native null",net.minecraft.world.level.levelgen.structure.StructureStart.loadStaticStart(context,s07Disk("synthetic-nonexistent",nonexistent),level.getSeed())==null,"native null");
            var chunk=level.getChunk(0,0);var originalStarts=new HashMap<>(chunk.getAllStarts());
            var lifecycle=AchieveToDoServer.class.getDeclaredMethod("onChunkLoadedStatusChanged",net.minecraft.server.level.ServerLevel.class,net.minecraft.world.level.chunk.ChunkAccess.class,boolean.class);lifecycle.setAccessible(true);
            try {
                boxes().clear();chunk.setAllStarts(new HashMap<>(Map.of(start.getStructure(),old)));
                lifecycle.invoke(owner(),level,chunk,true);
                require("S07 actual load handler does not index untagged native hull",boxes().isEmpty(),boxes());
                chunk.setAllStarts(new HashMap<>(Map.of(start.getStructure(),incomplete)));lifecycle.invoke(owner(),level,chunk,true);
                require("S07 actual load handler does not index type-only metadata",boxes().isEmpty(),boxes());
                chunk.setAllStarts(new HashMap<>(Map.of(start.getStructure(),loaded)));lifecycle.invoke(owner(),level,chunk,true);lifecycle.invoke(owner(),level,chunk,true);
                require("S07 actual load handler indexes exact synthetic saved geometry once",boxes().get(chunk.getPos()).get(LandmarkType.RUINED_PORTAL).equals(Set.of(new DimensionalBlockBox(DimensionType.OVERWORLD,savedBox))),boxes());
                lifecycle.invoke(owner(),level,chunk,false);require("S07 actual unload handler removes synthetic saved geometry",boxes().isEmpty(),boxes());
                lifecycle.invoke(owner(),level,chunk,true);require("S07 actual reindex handler retains synthetic saved geometry",boxes().get(chunk.getPos()).get(LandmarkType.RUINED_PORTAL).equals(Set.of(new DimensionalBlockBox(DimensionType.OVERWORLD,savedBox))),boxes());
            } finally { chunk.setAllStarts(originalStarts); }
            require("S07 disposable native structure map restored",chunk.getAllStarts().equals(originalStarts),chunk.getPos());
            System.out.println("D_S07_OBSERVED "+new Gson().toJson(Map.of("case","metadata_codec_controls","kind","NATIVE_GENERATION_CONTROL_WITH_SYNTHETIC_SAVED_BOX","historical_world",false,"disk_codec_readback",true,"chunk_reload",false,"process_restart",false)));
        });
    }

    @GameTest(maxTicks=1800) public void s07NaturalNetherGeneration(GameTestHelper helper) {
        run(helper,"s07NaturalNetherGeneration",() -> {
            var options=(net.minecraft.world.level.levelgen.WorldOptions)field(null,net.minecraft.gametest.framework.GameTestServer.class,"WORLD_OPTIONS");
            System.out.println("D_S07_NATIVE_WORLD_OPTIONS "+new Gson().toJson(Map.of("seed",options.seed(),"generate_structures",options.generateStructures())));
            if(!options.generateStructures()) {
                System.out.println("D_S07_NOT_RUN natural_generation: pinned native GameTestServer WORLD_OPTIONS.generateStructures=false; no worldgen override authorized");return;
            }
            var level=helper.getLevel().getServer().getLevel(net.minecraft.world.level.Level.NETHER);
            if(level==null) {
                System.out.println("D_S07_NOT_RUN natural_generation: existing GameTest server has no Nether ServerLevel");return;
            }
            config().put(AbilityType.INTERACT_INSIDE_RUINED_PORTAL,20000);score(joined.getFirst(),10000);
            require("S07 nearby observer locked before autonomous generation",owner().isAbilityLocked(joined.getFirst().player(),AbilityType.INTERACT_INSIDE_RUINED_PORTAL,true),"no initiating player");
            var generator=level.getChunkSource().getGenerator();
            System.out.println("D_S07_NETHER_GENERATOR "+generator.getClass().getName());
            if(!(generator instanceof net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator)) {
                System.out.println("D_S07_NOT_RUN natural_generation: existing Nether generator is not native noise terrain");return;
            }
            var holder=level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(BuiltinStructures.RUINED_PORTAL_NETHER);
            // Bounded native candidate search; no biome override, placement command,
            // injected StructureStart or manually constructed tracking geometry.
            var found=generator.findNearestMapStructure(level,net.minecraft.core.HolderSet.direct(holder),new BlockPos(0,64,0),1,false);
            if(found==null) { System.out.println("D_S07_NOT_RUN natural_generation: no native candidate in bounded radius 1");return; }
            var chunk=level.getChunkAt(found.getFirst());var start=chunk.getStartForStructure(holder.value());
            require("S07 natural Nether start valid and native registry identity",start!=null&&start.isValid()&&start.getStructure()==holder.value(),found);
            var ext=s07Extension(start);
            require("S07 natural future start tagged original family",ext.achievetodo$getLandmarkType()==LandmarkType.RUINED_PORTAL,ext.achievetodo$getLandmarkType());
            var box=ext.achievetodo$getLandmarkBlockBox();
            require("S07 natural placement accumulated generated-position box",box!=null,box);
            var dimensionBox=new DimensionalBlockBox(DimensionType.NETHER,box);
            require("S07 natural generation registered actual dimensional box",boxes().getOrDefault(chunk.getPos(),Map.of()).getOrDefault(LandmarkType.RUINED_PORTAL,Set.of()).contains(dimensionBox),chunk.getPos());
            var f=join();config().put(AbilityType.INTERACT_INSIDE_RUINED_PORTAL,20000);score(f,10000);
            var target=new AABB(box.minX(),box.minY(),box.minZ(),box.maxX()+1,box.maxY()+1,box.maxZ()+1);
            require("S07 actual generated bounds deny locked actor",owner().isTargetInLockedLandmark(f.player(),DimensionType.NETHER,target,true),box);
            require("S07 actual generated bounds other dimension excluded",!owner().isTargetInLockedLandmark(f.player(),DimensionType.OVERWORLD,target,true),box);
            var pos=box.getCenter();var state=level.getBlockState(pos);
            require("S07 native-valid survival block permission denied",f.player().blockActionRestricted(level,pos,GameType.SURVIVAL),state);
            score(f,30000);
            require("S07 actual generated bounds allow unlocked actor",!owner().isTargetInLockedLandmark(f.player(),DimensionType.NETHER,target,true),box);
            require("S07 native-valid survival permission stays allowed",!f.player().blockActionRestricted(level,pos,GameType.SURVIVAL),pos);
            require("S07 native-invalid spectator permission stays restricted",f.player().blockActionRestricted(level,pos,GameType.SPECTATOR),pos);
            var context=net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext.fromLevel(level);
            var saved=start.createTag(context,start.getChunkPos());var original=saved.copy();
            var loaded=net.minecraft.world.level.levelgen.structure.StructureStart.loadStaticStart(context,s07Disk("actual-generated-nether",saved),level.getSeed());
            require("S07 genuine generated metadata codec retains exact type and geometry",loaded!=null&&s07Extension(loaded).achievetodo$getLandmarkType()==LandmarkType.RUINED_PORTAL&&s07Extension(loaded).achievetodo$getLandmarkBlockBox().equals(box),box);
            require("S07 genuine metadata input unchanged",saved.equals(original),saved);
            // Existing lifecycle API controls, explicitly not chunk unload/reload.
            var payload=Map.of(LandmarkType.RUINED_PORTAL,Set.of(dimensionBox));
            owner().onLandmarksLoadedStatusChanged(level,chunk.getPos(),payload,false);
            require("S07 actual box remove clears stale membership",!boxes().getOrDefault(chunk.getPos(),Map.of()).getOrDefault(LandmarkType.RUINED_PORTAL,Set.of()).contains(dimensionBox),chunk.getPos());
            owner().onLandmarksLoadedStatusChanged(level,chunk.getPos(),payload,true);owner().onLandmarksLoadedStatusChanged(level,chunk.getPos(),payload,true);
            require("S07 actual box reindex has no duplicate",boxes().get(chunk.getPos()).get(LandmarkType.RUINED_PORTAL).stream().filter(dimensionBox::equals).count()==1,chunk.getPos());
            System.out.println("D_S07_OBSERVED "+new Gson().toJson(Map.of("case","natural_nether_generation","kind","ACTUAL_NATIVE_CANDIDATE_AND_CHUNK_GENERATION","chunk",chunk.getPos().toString(),"native_hull",start.getBoundingBox().toString(),"tracked_box",box.toString(),"disk_codec_readback",true,"chunk_reload",false,"process_restart",false,"initiating_player","NONE")));
        });
    }
    private BlockPos put(int x,int y,int z,BlockState state) {
        BlockPos pos=helper.absolutePos(new BlockPos(x,y,z));blocks.putIfAbsent(pos,helper.getLevel().getBlockState(pos));
        helper.getLevel().setBlockAndUpdate(pos,state);return pos;
    }
    private Set<String> criteria(Joined f) {
        Set<String> completed=new TreeSet<>();
        for(var advancement:helper.getLevel().getServer().getAdvancements().getAllAdvancements())
            for(String criterion:f.player().getAdvancements().getOrStartProgress(advancement).getCompletedCriteria())completed.add(advancement.id()+"#"+criterion);
        return completed;
    }
    private List<Object> observe(Joined actor) {
        List<Object> packets=new ArrayList<>(drain(actor));for(Joined other:joined)if(other!=actor)packets.addAll(drain(other));return packets;
    }
    private long effects(List<Object> packets) {
        return packets.stream().filter(p -> p instanceof ClientboundSoundPacket||p instanceof ClientboundSoundEntityPacket||p instanceof ClientboundLevelEventPacket).count();
    }
    private long notifications(List<Object> packets) {
        return packets.stream().filter(p -> p instanceof ClientboundSystemChatPacket chat&&chat.overlay()).count();
    }
    private void noEffects(Joined f,Set<String> before,String action) {
        noEffects(f,before,action,null);
    }
    private void noEffects(Joined f,Set<String> before,String action,AbilityType deniedAbility) {
        var packets=observe(f);require(action+" no native sound/level event",effects(packets)==0,effects(packets));
        require(action+" no double ATD feedback",notifications(packets)+feedback(packets)<=1,notifications(packets)+feedback(packets));
        var expected=new TreeSet<>(before);
        if(deniedAbility!=null)expected.add(com.diskree.achievetodo.ability.generation.AbilityAdvancementsGenerator.buildAdvancementId(deniedAbility)+"#"+com.diskree.achievetodo.ability.generation.AbilityAdvancementsGenerator.DEMYSTIFIED_CRITERION);
        // The accepted permission helper naturally awards this exact feedback
        // criterion. Every native success criterion must remain unchanged.
        require(action+" exact criterion conservation including approved denial feedback",criteria(f).equals(expected),criteria(f).size());
    }
    private void blockLandmark(Joined f,BlockPos pos) throws Exception {
        boxes().clear();membership().clear();boxes().put(new ChunkPos(pos.getX()>>4,pos.getZ()>>4),new HashMap<>(Map.of(LandmarkType.DESERT_PYRAMID,new HashSet<>(Set.of(new DimensionalBlockBox(DimensionType.OVERWORLD,new BoundingBox(pos)))))));
        membership().put(LandmarkType.DESERT_PYRAMID,new HashSet<>(Set.of(f.player().getUUID())));
    }
    private void targetLandmark(Joined f,Entity target) throws Exception {
        blockLandmark(f,target.blockPosition());
        require("actual target-landmark permission matches current gate",owner().isTargetInLockedLandmark(f.player(),DimensionType.OVERWORLD,target.getBoundingBox(),true)==owner().isAbilityLocked(f.player(),landmarkAbility(),true),target.getUUID());
    }
    private AbilityType landmarkAbility() { return AbilityType.findByLandmarkType(LandmarkType.DESERT_PYRAMID); }
    private BlockHitResult hit(BlockPos pos,Direction face) { return new BlockHitResult(Vec3.atCenterOf(pos),face,pos,false); }
    private InteractionResult shelfUse(Joined f,BlockPos pos,InteractionHand hand,Direction face) {
        var state=helper.getLevel().getBlockState(pos);String before=shelfState(f,List.of(pos));var progress=criteria(f);
        var result=f.player().gameMode.useItemOn(f.player(),helper.getLevel(),f.player().getItemInHand(hand),hand,hit(pos,face));
        var gains=new TreeSet<>(criteria(f));gains.removeAll(progress);
        System.out.println("D_S06_SHELF_STATE "+new Gson().toJson(Map.of("scenario",scenario,"hand",hand,"target",pos.toString(),"state",state.toString(),"before",before,"after",shelfState(f,List.of(pos)),"result",result.toString(),"criterion_gains",gains)));return result;
    }
    private ShelfBlockEntity shelf(BlockPos pos) { return (ShelfBlockEntity)helper.getLevel().getBlockEntity(pos); }
    private String stackState(ItemStack stack) { return stack.toString()+" components="+stack.getComponents()+" damage="+stack.getDamageValue(); }
    private String shelfState(Joined f,List<BlockPos> chain) {
        var state=new ArrayList<String>();var inventory=f.player().getInventory();
        for(int i=0;i<inventory.getContainerSize();i++)state.add("inventory:"+i+"="+stackState(inventory.getItem(i)));
        state.add("carried="+stackState(f.player().containerMenu.getCarried()));
        for(BlockPos pos:chain) {state.add("block:"+pos+"="+helper.getLevel().getBlockState(pos));var be=helper.getLevel().getBlockEntity(pos);if(be instanceof ShelfBlockEntity shelf)for(int i=0;i<shelf.getContainerSize();i++)state.add("shelf:"+pos+":"+i+"="+stackState(shelf.getItem(i)));}
        return state.toString();
    }
    private Map<String,Integer> totals(Joined f,List<BlockPos> chain) {
        var counts=new TreeMap<String,Integer>();var inv=f.player().getInventory();for(int i=0;i<inv.getContainerSize();i++)addTotal(counts,inv.getItem(i));
        addTotal(counts,f.player().containerMenu.getCarried());for(BlockPos pos:chain)if(helper.getLevel().getBlockEntity(pos) instanceof ShelfBlockEntity be)for(int i=0;i<be.getContainerSize();i++)addTotal(counts,be.getItem(i));return counts;
    }
    /** Diagnostic for the real caller's nonconsuming-result fallback. Keep the
     * original failed single-transfer assertion intact until this is resolved. */
    @GameTest(maxTicks=200) public void shelfDeniedBlockItemFallbackDiagnostic(GameTestHelper helper) {
        run(helper,"shelfDeniedBlockItemFallbackDiagnostic",() -> {
            Joined f=join();BlockPos pos=put(3,2,3,Blocks.OAK_SHELF.defaultBlockState());
            BlockPos front=pos.north();blocks.putIfAbsent(front,helper.getLevel().getBlockState(front));
            helper.getLevel().setBlockAndUpdate(front,Blocks.AIR.defaultBlockState());
            gates(f,Set.of(AbilityType.OPEN_CHEST));boxes().clear();membership().clear();
            hands(f.player(),InteractionHand.MAIN_HAND,Items.STONE);f.player().getMainHandItem().setCount(5);shelf(pos).setItem(1,new ItemStack(Items.DIAMOND,7));observe(f);
            require("diagnostic actual native slot eligible",((ShelfBlock)Blocks.OAK_SHELF).getHitSlot(hit(pos,Direction.NORTH),Direction.NORTH).orElseThrow()==1,"slot 1");
            var nativeResult=helper.getLevel().getBlockState(pos).useItemOn(f.player().getMainHandItem(),helper.getLevel(),f.player(),InteractionHand.MAIN_HAND,hit(pos,Direction.NORTH));
            require("actual Shelf callee ATD return is FAIL",nativeResult==InteractionResult.FAIL,nativeResult);
            require("callee denial conserves hand and shelf",f.player().getMainHandItem().getCount()==5&&shelf(pos).getItem(1).is(Items.DIAMOND)&&shelf(pos).getItem(1).getCount()==7,shelfState(f,List.of(pos)));observe(f);
            int before=f.player().getMainHandItem().getCount();BlockState beforeFront=helper.getLevel().getBlockState(front);var progress=criteria(f);
            var outer=shelfUse(f,pos,InteractionHand.MAIN_HAND,Direction.NORTH);var gains=new TreeSet<>(criteria(f));gains.removeAll(progress);var packets=observe(f);
            System.out.println("D_S06_FALLBACK_DIAGNOSTIC "+new Gson().toJson(Map.ofEntries(
                Map.entry("native_callee_result",nativeResult.toString()),Map.entry("outer_result",outer.toString()),Map.entry("outer_consumes",outer.consumesAction()),
                Map.entry("held_before",before),Map.entry("held_after",f.player().getMainHandItem().getCount()),Map.entry("front_before",beforeFront.toString()),Map.entry("front_after",helper.getLevel().getBlockState(front).toString()),
                Map.entry("shelf_body",shelf(pos).getItem(1).toString()),Map.entry("criteria_gains",gains),Map.entry("native_sound_packets",effects(packets)),Map.entry("ATD_feedback",feedback(packets)+notifications(packets)),Map.entry("OPEN_CHEST_locked",owner().isAbilityLocked(f.player(),AbilityType.OPEN_CHEST,true)))));
            require("ATD shelf denial cannot consume held block through caller fallback",before==f.player().getMainHandItem().getCount()&&beforeFront.equals(helper.getLevel().getBlockState(front))&&!outer.consumesAction(),"count="+before+" -> "+f.player().getMainHandItem().getCount()+"; result="+outer);
            require("caller denied Shelf returns genuine FAIL",outer==InteractionResult.FAIL,outer);
            require("caller denial sound/criteria/feedback contract",effects(packets)==0&&gains.isEmpty()&&feedback(packets)+notifications(packets)==1,packets.size());
        });
    }
    @GameTest(maxTicks=200) public void cauldronAndCallerNativeNegatives(GameTestHelper helper) {
        run(helper,"cauldronAndCallerNativeNegatives",() -> {
            Joined f=join();gates(f,Set.of());boxes().clear();membership().clear();
            BlockPos pos=put(3,2,3,Blocks.CAULDRON.defaultBlockState());hands(f.player(),InteractionHand.MAIN_HAND,Items.WATER_BUCKET);observe(f);
            require("historical cauldron allowed native success",shelfUse(f,pos,InteractionHand.MAIN_HAND,Direction.UP).consumesAction(),"water bucket");
            require("historical cauldron exact water and empty bucket",helper.getLevel().getBlockState(pos).is(Blocks.WATER_CAULDRON)&&helper.getLevel().getBlockState(pos).getValue(LayeredCauldronBlock.LEVEL)==3&&f.player().getMainHandItem().is(Items.BUCKET),helper.getLevel().getBlockState(pos));
            require("historical cauldron native sound observed",effects(observe(f))>0,"fill");
            helper.getLevel().setBlockAndUpdate(pos,Blocks.CAULDRON.defaultBlockState());hands(f.player(),InteractionHand.MAIN_HAND,Items.WATER_BUCKET);score(f,threshold(AbilityType.USE_CAULDRON)-1);observe(f);String before=shelfState(f,List.of(pos));var progress=criteria(f);
            require("historical cauldron denied caller FAIL",shelfUse(f,pos,InteractionHand.MAIN_HAND,Direction.UP)==InteractionResult.FAIL,"ability");
            require("historical cauldron denial state/item conserved",shelfState(f,List.of(pos)).equals(before),before);noEffects(f,progress,"cauldron ability denial",AbilityType.USE_CAULDRON);
            gates(f,Set.of(landmarkAbility()));blockLandmark(f,pos);observe(f);progress=criteria(f);before=shelfState(f,List.of(pos));
            require("historical cauldron target denied caller FAIL",shelfUse(f,pos,InteractionHand.MAIN_HAND,Direction.UP)==InteractionResult.FAIL,"landmark");require("cauldron target denial conserved",shelfState(f,List.of(pos)).equals(before),before);noEffects(f,progress,"cauldron target denial");
            boxes().clear();membership().clear();score(f,threshold(AbilityType.USE_CAULDRON)-1);hands(f.player(),InteractionHand.MAIN_HAND,Items.DIAMOND);observe(f);before=shelfState(f,List.of(pos));
            require("default cauldron interaction native PASS",shelfUse(f,pos,InteractionHand.MAIN_HAND,Direction.UP)==InteractionResult.PASS,"diamond");var packets=observe(f);require("default cauldron no new gate",notifications(packets)==0&&feedback(packets)==0&&shelfState(f,List.of(pos)).equals(before),packets.size());
            hands(f.player(),InteractionHand.MAIN_HAND,Items.WATER_BUCKET);f.player().setShiftKeyDown(true);require("native secondary-use active",f.player().isSecondaryUseActive(),true);observe(f);
            // BucketItem overrides use, not useOn: this server block-use route
            // natively PASSes; a separate air-use/raytrace packet is not invoked.
            require("historical cauldron secondary use native PASS bypass",shelfUse(f,pos,InteractionHand.MAIN_HAND,Direction.UP)==InteractionResult.PASS&&f.player().getMainHandItem().is(Items.WATER_BUCKET)&&helper.getLevel().getBlockState(pos).is(Blocks.CAULDRON),f.player().getMainHandItem());
            packets=observe(f);require("secondary cauldron no denial or success effects",notifications(packets)==0&&feedback(packets)==0&&effects(packets)==0,packets.size());f.player().setShiftKeyDown(false);
            // Locked OPEN_CHEST may not suppress legitimate native item fallback.
            helper.getLevel().setBlockAndUpdate(pos,Blocks.OAK_SHELF.defaultBlockState());gates(f,Set.of(AbilityType.OPEN_CHEST));
            for(String route:List.of("invalid_face","offhand","secondary","removed_BE")) {
                Direction face=route.equals("invalid_face")?Direction.SOUTH:Direction.NORTH;BlockPos adjacent=pos.relative(face);blocks.putIfAbsent(adjacent,helper.getLevel().getBlockState(adjacent));helper.getLevel().setBlockAndUpdate(adjacent,Blocks.AIR.defaultBlockState());
                InteractionHand hand=route.equals("offhand")?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND;hands(f.player(),hand,Items.STONE);f.player().getItemInHand(hand).setCount(2);
                if(route.equals("secondary"))f.player().setShiftKeyDown(true);if(route.equals("removed_BE")) {
                    helper.getLevel().removeBlockEntity(pos);observe(f);
                    require("removed BE absent under native CHECK lookup",helper.getLevel().getChunkAt(pos).getBlockEntity(pos,net.minecraft.world.level.chunk.LevelChunk.EntityCreationType.CHECK)==null,"CHECK");
                    // Native Level.getBlockEntity uses IMMEDIATE creation. Do not
                    // fabricate a missing-BE candidate by suppressing this factory.
                    var result=f.player().gameMode.useItemOn(f.player(),helper.getLevel(),f.player().getMainHandItem(),hand,hit(pos,face));
                    require("native recreated BE remains eligible and safely denied",result==InteractionResult.FAIL&&helper.getLevel().getChunkAt(pos).getBlockEntity(pos,net.minecraft.world.level.chunk.LevelChunk.EntityCreationType.CHECK) instanceof ShelfBlockEntity&&f.player().getMainHandItem().getCount()==2&&helper.getLevel().getBlockState(adjacent).is(Blocks.AIR),result);
                    packets=observe(f);require("recreated BE denial exactly one feedback no sound",feedback(packets)+notifications(packets)==1&&effects(packets)==0,packets.size());continue;
                }observe(f);
                require("native-ineligible Shelf retains legitimate placement "+route,shelfUse(f,pos,hand,face).consumesAction()&&f.player().getItemInHand(hand).getCount()==1&&helper.getLevel().getBlockState(adjacent).is(Blocks.STONE),route);
                packets=observe(f);require("native-ineligible Shelf no ATD feedback "+route,notifications(packets)==0&&feedback(packets)==0,packets.size());
                f.player().setShiftKeyDown(false);helper.getLevel().setBlockAndUpdate(adjacent,Blocks.AIR.defaultBlockState());
            }
            hands(f.player(),InteractionHand.MAIN_HAND,Items.EMERALD);shelf(pos).setItem(1,new ItemStack(Items.DIAMOND,7));f.player().setGameMode(GameType.SPECTATOR);observe(f);before=shelfState(f,List.of(pos));require("spectator native PASS before block dispatch",shelfUse(f,pos,InteractionHand.MAIN_HAND,Direction.NORTH)==InteractionResult.PASS,"spectator");packets=observe(f);require("spectator no mutation or gate",shelfState(f,List.of(pos)).equals(before)&&feedback(packets)==0&&notifications(packets)==0,packets.size());
            f.player().setGameMode(GameType.CREATIVE);observe(f);require("creative existing exemption permits native Shelf swap",shelfUse(f,pos,InteractionHand.MAIN_HAND,Direction.NORTH).consumesAction()&&f.player().getMainHandItem().is(Items.DIAMOND),f.player().getMainHandItem());f.player().setGameMode(GameType.SURVIVAL);observe(f);
            helper.getLevel().setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());BlockPos other=pos.north();blocks.putIfAbsent(other,helper.getLevel().getBlockState(other));helper.getLevel().setBlockAndUpdate(other,Blocks.AIR.defaultBlockState());hands(f.player(),InteractionHand.MAIN_HAND,Items.DIRT);observe(f);require("other block item placement unaffected by Shelf guard",shelfUse(f,pos,InteractionHand.MAIN_HAND,Direction.NORTH).consumesAction()&&helper.getLevel().getBlockState(other).is(Blocks.DIRT),"dirt");observe(f);
            System.out.println("D_S06_CAULDRON_CALLER_RECEIPT "+new Gson().toJson(Map.of("historical_cauldron","PASS","native_ineligible_shelf_fallback","PASS","creative_spectator","PASS","other_blocks","PASS")));
        });
    }
    private void addTotal(Map<String,Integer> counts,ItemStack stack) { if(!stack.isEmpty())counts.merge(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),stack.getCount(),Integer::sum); }
    @GameTest(maxTicks=200) public void shelfSingleTransfers(GameTestHelper helper) {
        run(helper,"shelfSingleTransfers",() -> {
            Joined f=join();BlockPos pos=put(3,2,3,Blocks.OAK_SHELF.defaultBlockState());
            gates(f,Set.of());hands(f.player(),InteractionHand.MAIN_HAND,Items.EMERALD);f.player().getMainHandItem().setCount(11);shelf(pos).setItem(1,new ItemStack(Items.DIAMOND,7));observe(f);var total=totals(f,List.of(pos));
            require("single native successful swap",shelfUse(f,pos,InteractionHand.MAIN_HAND,Direction.NORTH).consumesAction(),"MAIN");
            require("single actual source/destination identities",f.player().getMainHandItem().is(Items.DIAMOND)&&f.player().getMainHandItem().getCount()==7&&shelf(pos).getItem(1).is(Items.EMERALD)&&shelf(pos).getItem(1).getCount()==11,shelfState(f,List.of(pos)));
            require("single all inventories item conservation",totals(f,List.of(pos)).equals(total),total);require("single native sound observed",effects(observe(f))>0,"single sound");
            for(boolean target:List.of(false,true)) {
                gates(f,target?Set.of(landmarkAbility()):Set.of(AbilityType.OPEN_CHEST));if(target)blockLandmark(f,pos);else {boxes().clear();membership().clear();}
                String before=shelfState(f,List.of(pos));var progress=criteria(f);observe(f);
                require("single eligible ATD denial FAIL",shelfUse(f,pos,InteractionHand.MAIN_HAND,Direction.NORTH)==InteractionResult.FAIL,target);
                require("single denial all slot/component/count/state unchanged",shelfState(f,List.of(pos)).equals(before),before);noEffects(f,progress,"single denial "+target,target?null:AbilityType.OPEN_CHEST);
            }
            gates(f,Set.of(AbilityType.OPEN_CHEST,landmarkAbility()));blockLandmark(f,pos);observe(f);var progress=criteria(f);String before=shelfState(f,List.of(pos));
            require("invalid face native PASS",shelfUse(f,pos,InteractionHand.MAIN_HAND,Direction.SOUTH)==InteractionResult.PASS,"back");
            var packets=observe(f);require("invalid face no ATD feedback or mutations",notifications(packets)==0&&feedback(packets)==0&&effects(packets)==0&&shelfState(f,List.of(pos)).equals(before)&&criteria(f).equals(progress),packets.size());
            hands(f.player(),InteractionHand.OFF_HAND,Items.DIAMOND);observe(f);before=shelfState(f,List.of(pos));
            require("native shelf offhand PASS",shelfUse(f,pos,InteractionHand.OFF_HAND,Direction.NORTH)==InteractionResult.PASS,"offhand");packets=observe(f);require("offhand no new gate",feedback(packets)==0&&notifications(packets)==0&&shelfState(f,List.of(pos)).equals(before),packets.size());
            hands(f.player(),InteractionHand.MAIN_HAND,Items.AIR);shelf(pos).clearContent();observe(f);require("empty source/destination native PASS",shelfUse(f,pos,InteractionHand.MAIN_HAND,Direction.NORTH)==InteractionResult.PASS,"empty");packets=observe(f);require("empty no ATD notification",feedback(packets)==0&&notifications(packets)==0,packets.size());
            gates(f,Set.of());hands(f.player(),InteractionHand.MAIN_HAND,Items.EMERALD);shelfUse(f,pos,InteractionHand.MAIN_HAND,Direction.NORTH);observe(f);
            var saved=shelf(pos).saveWithFullMetadata(helper.getLevel().registryAccess());Path file=Path.of("d_s06_shelf.nbt");NbtIo.write(saved,file);var read=NbtIo.read(file);var loaded=(ShelfBlockEntity)BlockEntity.loadStatic(pos,helper.getLevel().getBlockState(pos),read,helper.getLevel().registryAccess());
            require("shelf actual disk NBT reread",saved.equals(read)&&loaded!=null&&ItemStack.matches(loaded.getItem(1),shelf(pos).getItem(1)),Files.size(file));
        });
    }
    @GameTest(maxTicks=200) public void shelfPoweredAtomicity(GameTestHelper helper) {
        run(helper,"shelfPoweredAtomicity",() -> {
            Joined f=join();var shelfBlock=(ShelfBlock)Blocks.OAK_SHELF;var state=shelfBlock.defaultBlockState();
            BlockPos a=put(3,2,3,state),b=put(4,2,3,state),c=put(5,2,3,state);
            // Native isConnectable requires each shelf to be powered; a shelf
            // chain does not transmit redstone power to its unpowered neighbors.
            for(int x=3;x<=5;x++)put(x,2,4,Blocks.REDSTONE_BLOCK.defaultBlockState());
            List<BlockPos> chain=shelfBlock.getAllBlocksConnectedTo(helper.getLevel(),b);
            require("actual native powered chain is three exact targets",chain.size()==3&&new HashSet<>(chain).equals(Set.of(a,b,c)),chain);
            require("actual power propagation native",helper.getLevel().getBlockState(b).getValue(ShelfBlock.POWERED),helper.getLevel().getBlockState(b));
            gates(f,Set.of());f.player().getInventory().clearContent();for(int i=0;i<9;i++)f.player().getInventory().setItem(i,new ItemStack(Items.EMERALD,i+1));
            for(BlockPos p:chain)for(int slot=0;slot<3;slot++)shelf(p).setItem(slot,new ItemStack(Items.DIAMOND,slot+2));
            var total=totals(f,chain);observe(f);require("powered eligible native success",shelfUse(f,b,InteractionHand.MAIN_HAND,Direction.NORTH).consumesAction(),chain);
            for(int n=0;n<3;n++)for(int slot=0;slot<3;slot++){int inv=n*3+slot;require("powered native order maps each source slot",shelf(chain.get(n)).getItem(slot).is(Items.EMERALD)&&shelf(chain.get(n)).getItem(slot).getCount()==inv+1&&f.player().getInventory().getItem(inv).is(Items.DIAMOND)&&f.player().getInventory().getItem(inv).getCount()==slot+2,chain.get(n)+":"+slot);}
            require("powered totals all source/destination inventories",totals(f,chain).equals(total),total);observe(f);
            for(BlockPos protectedPos:chain) {
                gates(f,Set.of(landmarkAbility()));blockLandmark(f,protectedPos);observe(f);String before=shelfState(f,chain);var progress=criteria(f);var counts=totals(f,chain);
                require("powered any actual chain target denial FAIL",shelfUse(f,b,InteractionHand.MAIN_HAND,Direction.NORTH)==InteractionResult.FAIL,protectedPos);
                require("powered denial atomic all slots/stacks/BE states",shelfState(f,chain).equals(before)&&totals(f,chain).equals(counts),before);noEffects(f,progress,"chain protected "+protectedPos);
            }
            // An empty shelf still participates in the native powered setter/event pass.
            gates(f,Set.of(landmarkAbility()));BlockPos empty=chain.getLast();for(int i=0;i<3;i++){shelf(empty).setItem(i,ItemStack.EMPTY);f.player().getInventory().setItem(6+i,ItemStack.EMPTY);}blockLandmark(f,empty);observe(f);String before=shelfState(f,chain);var progress=criteria(f);
            require("empty affected target still protects nonempty powered operation",shelfUse(f,b,InteractionHand.MAIN_HAND,Direction.NORTH)==InteractionResult.FAIL,"empty final shelf");require("empty target denial prevents earlier partial swaps",shelfState(f,chain).equals(before),before);noEffects(f,progress,"empty affected target");
            gates(f,Set.of(AbilityType.OPEN_CHEST));boxes().clear();membership().clear();before=shelfState(f,chain);observe(f);progress=criteria(f);require("powered OPEN_CHEST denial",shelfUse(f,b,InteractionHand.MAIN_HAND,Direction.NORTH)==InteractionResult.FAIL,"open");require("powered chest denial atomic",shelfState(f,chain).equals(before),before);noEffects(f,progress,"powered chest",AbilityType.OPEN_CHEST);
            for(BlockPos p:chain)shelf(p).clearContent();f.player().getInventory().clearContent();observe(f);require("wholly empty chain native CONSUME",shelfUse(f,b,InteractionHand.MAIN_HAND,Direction.NORTH)==InteractionResult.CONSUME,"empty chain");var packets=observe(f);require("empty chain no ATD feedback",notifications(packets)==0&&feedback(packets)==0,packets.size());
            // Native block setters/redstone remain independent of locked initiating permissions.
            // Remove the real power sources; forcing POWERED=false while a source
            // remains attached is immediately corrected by native neighbor logic.
            for(int x=3;x<=5;x++)put(x,2,4,Blocks.AIR.defaultBlockState());
            require("admin/native state setter unrestricted",!helper.getLevel().getBlockState(a).getValue(ShelfBlock.POWERED),a);
            var serialized=shelf(chain.getFirst()).saveWithFullMetadata(helper.getLevel().registryAccess());require("powered shelf serializes slot inventory",serialized!=null,serialized);
            System.out.println("D_S06_CHAIN_RECEIPT "+new Gson().toJson(Map.of("scenario",scenario,"actual_native_chain",chain.stream().map(BlockPos::toString).toList(),"denied_targets",3,"all_inventories_conserved",true,"partial_swaps",0,"native_order",true)));
        });
    }
    private void ageLocked(SulfurCube cube,boolean value) throws Exception {
        var setter=AgeableMob.class.getDeclaredMethod("setAgeLocked",boolean.class);setter.setAccessible(true);setter.invoke(cube,value);
    }
    private SulfurCube loadCube(net.minecraft.nbt.CompoundTag tag) {
        // Restore into an unattached native entity: saved UUIDs must never collide
        // with live fixture UUIDs in the server entity index.
        var restored=EntityTypes.SULFUR_CUBE.create(helper.getLevel(),EntitySpawnReason.LOAD);
        require("native restoration entity created",restored!=null,"LOAD");
        restored.load(TagValueInput.create(ProblemReporter.DISCARDING,helper.getLevel().registryAccess(),tag));
        return restored;
    }
    private SulfurCube cube(Joined f,boolean baby,Item body) throws Exception {
        var cube=spawn(EntityTypes.SULFUR_CUBE,f);cube.setAge(baby?-24000:0);ageLocked(cube,false);
        if(body!=Items.AIR){require("native fixture BODY item tag",new ItemStack(body).is(ItemTags.SULFUR_CUBE_SWALLOWABLE),body);require("native autonomous equip eligibility",cube.equipItem(new ItemStack(body)),body);var update=LivingEntity.class.getDeclaredMethod("detectEquipmentUpdates");update.setAccessible(true);update.invoke(cube);}
        require("actual Sulfur age identity",cube.isBaby()==baby,cube.getAge());return cube;
    }
    private String sulfurState(Joined f,SulfurCube cube,InteractionHand hand) {
        return "age="+cube.getAge()+" ageLocked="+cube.isAgeLocked()+" fuse="+cube.getFuse()+" primed="+cube.isPrimed()+" invulnerable="+cube.isInvulnerable()+" body="+stackState(cube.getItemBySlot(EquipmentSlot.BODY))+" held="+stackState(f.player().getItemInHand(hand))+" drops="+drops().stream().map(d -> stackState(d.getItem())).toList();
    }
    private InteractionResult sulfurUse(Joined f,SulfurCube cube,InteractionHand hand) {
        Item item=f.player().getItemInHand(hand).getItem();int usedBefore=f.player().getStats().getValue(net.minecraft.stats.Stats.ITEM_USED.get(item));
        String before=sulfurState(f,cube,hand);var progress=criteria(f);var result=interact(f,cube,hand);var gains=new TreeSet<>(criteria(f));gains.removeAll(progress);
        System.out.println("D_S06_SULFUR_STATE "+new Gson().toJson(Map.of("scenario",scenario,"target",cube.getUUID(),"hand",hand,"before",before,"after",sulfurState(f,cube,hand),"result",result.toString(),"criterion_gains",gains,"item_used_stat_before",usedBefore,"item_used_stat_after",f.player().getStats().getValue(net.minecraft.stats.Stats.ITEM_USED.get(item)))));return result;
    }
    private void denied(Joined f,SulfurCube cube,InteractionHand hand,String action) {
        Item item=f.player().getItemInHand(hand).getItem();int usedBefore=f.player().getStats().getValue(net.minecraft.stats.Stats.ITEM_USED.get(item));
        AbilityType feedbackAbility=item==Items.SHEARS&&owner().isAbilityLocked(f.player(),AbilityType.USE_SHEARS,true)?AbilityType.USE_SHEARS:
            item==Items.FLINT_AND_STEEL&&owner().isAbilityLocked(f.player(),AbilityType.USE_FLINT_AND_STEEL,true)?AbilityType.USE_FLINT_AND_STEEL:
            (item==Items.FLINT_AND_STEEL||item==Items.FIRE_CHARGE)&&owner().isAbilityLocked(f.player(),AbilityType.IGNITE_TNT,true)?AbilityType.IGNITE_TNT:null;
        String before=sulfurState(f,cube,hand);var progress=criteria(f);observe(f);
        require(action+" native outer PASS after ATD subtype denial",sulfurUse(f,cube,hand)==InteractionResult.PASS,action);
        require(action+" full held/body/age/fuse/drop conservation",sulfurState(f,cube,hand).equals(before),before);noEffects(f,progress,action,feedbackAbility);
        require(action+" native item-used statistic unchanged",f.player().getStats().getValue(net.minecraft.stats.Stats.ITEM_USED.get(item))==usedBefore,usedBefore);
    }
    @GameTest(maxTicks=200) public void sulfurFeedAndBody(GameTestHelper helper) {
        run(helper,"sulfurFeedAndBody",() -> {
            Joined f=join();for(InteractionHand hand:InteractionHand.values()) {
                var baby=cube(f,true,Items.AIR);gates(f,Set.of(landmarkAbility()));targetLandmark(f,baby);hands(f.player(),hand,Items.SLIME_BALL);f.player().getItemInHand(hand).setCount(3);observe(f);require("actual native food tag",f.player().getItemInHand(hand).is(ItemTags.SULFUR_CUBE_FOOD)&&baby.canAgeUp(),hand);denied(f,baby,hand,"baby feeding landmark");
                gates(f,Set.of());observe(f);int age=baby.getAge();require("baby food allowed native success",sulfurUse(f,baby,hand).consumesAction(),hand);require("baby exact one food and native age-up",f.player().getItemInHand(hand).getCount()==2&&baby.getAge()>age, baby.getAge());require("baby growth sound observed",effects(observe(f))>0,hand);
                ageLocked(baby,true);gates(f,Set.of(landmarkAbility()));observe(f);String before=sulfurState(f,baby,hand);require("age-locked native PASS",sulfurUse(f,baby,hand)==InteractionResult.PASS,hand);var packets=observe(f);require("age-locked no ATD feedback or consumption",sulfurState(f,baby,hand).equals(before)&&feedback(packets)==0&&notifications(packets)==0,hand);
                hands(f.player(),hand,Items.DIAMOND);observe(f);require("invalid baby food native PASS",sulfurUse(f,baby,hand)==InteractionResult.PASS,hand);packets=observe(f);require("invalid food no gate",feedback(packets)==0&&notifications(packets)==0,hand);baby.discard();
                var adult=cube(f,false,Items.AIR);gates(f,Set.of(landmarkAbility()));targetLandmark(f,adult);hands(f.player(),hand,Items.STONE);f.player().getItemInHand(hand).setCount(3);observe(f);denied(f,adult,hand,"adult swallow");
                gates(f,Set.of());require("adult native swallow success",sulfurUse(f,adult,hand).consumesAction(),hand);require("adult body one stone held two",adult.getItemBySlot(EquipmentSlot.BODY).is(Items.STONE)&&adult.getItemBySlot(EquipmentSlot.BODY).getCount()==1&&f.player().getItemInHand(hand).getCount()==2,sulfurState(f,adult,hand));observe(f);
                gates(f,Set.of(landmarkAbility()));String same=sulfurState(f,adult,hand);observe(f);require("same absorbed block native PASS",sulfurUse(f,adult,hand)==InteractionResult.PASS,hand);packets=observe(f);require("same item native false no gate",sulfurState(f,adult,hand).equals(same)&&feedback(packets)==0&&notifications(packets)==0,hand);
                hands(f.player(),hand,Items.DIAMOND);observe(f);String invalid=sulfurState(f,adult,hand);require("invalid adult swallowing item native PASS",sulfurUse(f,adult,hand)==InteractionResult.PASS,hand);packets=observe(f);require("invalid adult item no gate or replacement",sulfurState(f,adult,hand).equals(invalid)&&feedback(packets)==0&&notifications(packets)==0,hand);
                hands(f.player(),hand,Items.DIRT);f.player().getItemInHand(hand).setCount(4);observe(f);denied(f,adult,hand,"replacement preserves old BODY");gates(f,Set.of());int dropsBefore=drops().size();require("replacement native success",sulfurUse(f,adult,hand).consumesAction(),hand);require("replacement BODY held and old drop conservation",adult.getItemBySlot(EquipmentSlot.BODY).is(Items.DIRT)&&f.player().getItemInHand(hand).getCount()==3&&drops().size()==dropsBefore+1&&drops().stream().filter(d -> d.getItem().is(Items.STONE)).mapToInt(d -> d.getItem().getCount()).sum()==1,sulfurState(f,adult,hand));observe(f);
                Path file=Path.of("d_s06_cube_body_"+hand+".nbt");var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,helper.getLevel().registryAccess());adult.saveWithoutId(output);var tag=output.buildResult();NbtIo.write(tag,file);var read=NbtIo.read(file);var loaded=loadCube(read);require("cube actual disk BODY and age reread",read.equals(tag)&&ItemStack.matches(loaded.getItemBySlot(EquipmentSlot.BODY),adult.getItemBySlot(EquipmentSlot.BODY))&&loaded.getAge()==adult.getAge(),Files.size(file));loaded.discard();adult.discard();drops().forEach(Entity::discard);
            }
            gates(f,Set.of(landmarkAbility(),AbilityType.USE_SHEARS));var autonomous=cube(f,false,Items.AIR);targetLandmark(f,autonomous);require("autonomous no-player BODY equip unrestricted",autonomous.equipItem(new ItemStack(Items.STONE)),autonomous.getUUID());observe(f);
            var collector=cube(f,false,Items.AIR);targetLandmark(f,collector);var source=new ItemEntity(helper.getLevel(),collector.getX(),collector.getY(),collector.getZ(),new ItemStack(Items.STONE,3));helper.getLevel().addFreshEntity(source);
            require("autonomous native pickup eligible",collector.canHoldItem(source.getItem()),source.getItem());
            var pickup=SulfurCube.class.getDeclaredMethod("pickUpItem",net.minecraft.server.level.ServerLevel.class,ItemEntity.class);pickup.setAccessible(true);pickup.invoke(collector,helper.getLevel(),source);
            require("autonomous pickup no player permission and item conservation",collector.getItemBySlot(EquipmentSlot.BODY).is(Items.STONE)&&collector.getItemBySlot(EquipmentSlot.BODY).getCount()==1&&source.getItem().getCount()==2,source.getItem());observe(f);
        });
    }
    @GameTest(maxTicks=200) public void sulfurShearing(GameTestHelper helper) {
        run(helper,"sulfurShearing",() -> {
            Joined f=join();for(InteractionHand hand:InteractionHand.values()) {
                var cube=cube(f,false,Items.STONE);targetLandmark(f,cube);hands(f.player(),hand,Items.SHEARS);
                for(var locked:List.of(Set.of(AbilityType.USE_SHEARS),Set.of(landmarkAbility()),Set.of(AbilityType.USE_SHEARS,landmarkAbility()))) {gates(f,locked);targetLandmark(f,cube);observe(f);require("actual readyForShearing",cube.readyForShearing(),hand);denied(f,cube,hand,"manual shear "+locked);}
                gates(f,Set.of());observe(f);var before=criteria(f);require("manual native shear succeeds",sulfurUse(f,cube,hand).consumesAction(),hand);require("native BODY removed and one drop",cube.getItemBySlot(EquipmentSlot.BODY).isEmpty()&&drops().stream().filter(d -> d.getItem().is(Items.STONE)).mapToInt(d -> d.getItem().getCount()).sum()==1,hand);require("native actual shears one durability",f.player().getItemInHand(hand).getDamageValue()==1,hand);require("native shear sound observed",effects(observe(f))>0,hand);
                gates(f,Set.of(AbilityType.USE_SHEARS,landmarkAbility()));observe(f);String state=sulfurState(f,cube,hand);require("empty BODY native PASS",sulfurUse(f,cube,hand)==InteractionResult.PASS,hand);var packets=observe(f);require("empty BODY no ATD feedback or wear",sulfurState(f,cube,hand).equals(state)&&feedback(packets)==0&&notifications(packets)==0,hand);cube.discard();drops().forEach(Entity::discard);
            }
        });
    }
    @GameTest(maxTicks=200) public void sulfurIgnitionAndControls(GameTestHelper helper) {
        run(helper,"sulfurIgnitionAndControls",() -> {
            Joined f=join();helper.getLevel().getGameRules().set(GameRules.TNT_EXPLODES,true,helper.getLevel().getServer());
            for(InteractionHand hand:InteractionHand.values())for(Item item:List.of(Items.FLINT_AND_STEEL,Items.FIRE_CHARGE))for(int mask=0;mask<8;mask++) {
                var cube=cube(f,false,Items.TNT);require("actual registry explosive archetype",cube.canExplode()&&cube.matchingArchetypes(new ItemStack(Items.TNT)).contains(helper.getLevel().registryAccess().lookupOrThrow(Registries.SULFUR_CUBE_ARCHETYPE).getValueOrThrow(SulfurCubeArchetypes.EXPLOSIVE)),cube.canExplode());targetLandmark(f,cube);hands(f.player(),hand,item);if(item==Items.FIRE_CHARGE)f.player().getItemInHand(hand).setCount(3);
                var locked=EnumSet.noneOf(AbilityType.class);if((mask&1)!=0)locked.add(AbilityType.USE_FLINT_AND_STEEL);if((mask&2)!=0)locked.add(AbilityType.IGNITE_TNT);if((mask&4)!=0)locked.add(landmarkAbility());gates(f,locked);targetLandmark(f,cube);observe(f);boolean deny=(item==Items.FLINT_AND_STEEL&&(mask&1)!=0)||(mask&6)!=0;
                if(deny)denied(f,cube,hand,"ignite "+item+" mask="+mask);else {
                    require("native allowed ignition consumes",sulfurUse(f,cube,hand).consumesAction(),item+" "+mask);require("native fuse synced primed invulnerability",cube.getFuse()>0&&cube.isPrimed()&&cube.isInvulnerable(),sulfurState(f,cube,hand));require("actual ignition item wear/quantity",item==Items.FLINT_AND_STEEL?f.player().getItemInHand(hand).getDamageValue()==1:f.player().getItemInHand(hand).getCount()==2,item);require("native priming sound observed",effects(observe(f))>0,item);
                    String before=sulfurState(f,cube,hand);gates(f,Set.of(AbilityType.USE_FLINT_AND_STEEL,AbilityType.IGNITE_TNT,landmarkAbility()));observe(f);require("already primed native PASS",sulfurUse(f,cube,hand)==InteractionResult.PASS,item);var packets=observe(f);require("already primed no gate or state mutation",sulfurState(f,cube,hand).equals(before)&&feedback(packets)==0&&notifications(packets)==0,item);
                    if(mask==0){var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,helper.getLevel().registryAccess());cube.saveWithoutId(out);Path file=Path.of("d_s06_fuse_"+hand+"_"+BuiltInRegistries.ITEM.getKey(item).getPath()+".nbt");NbtIo.write(out.buildResult(),file);var loaded=loadCube(NbtIo.read(file));require("primed actual disk fuse/BODY/age reread",loaded.getFuse()==cube.getFuse()&&loaded.getAge()==cube.getAge()&&ItemStack.matches(loaded.getItemBySlot(EquipmentSlot.BODY),cube.getItemBySlot(EquipmentSlot.BODY)),Files.size(file));loaded.discard();}
                }
                cube.discard();observe(f);
            }
            for(InteractionHand hand:InteractionHand.values())for(Item item:List.of(Items.FLINT_AND_STEEL,Items.FIRE_CHARGE)) {
                var cube=cube(f,false,Items.TNT);gates(f,Set.of(AbilityType.IGNITE_TNT,AbilityType.USE_FLINT_AND_STEEL,landmarkAbility()));targetLandmark(f,cube);hands(f.player(),hand,item);helper.getLevel().getGameRules().set(GameRules.TNT_EXPLODES,false,helper.getLevel().getServer());observe(f);String before=sulfurState(f,cube,hand);require("TNT_EXPLODES false native PASS",sulfurUse(f,cube,hand)==InteractionResult.PASS,item);var packets=observe(f);require("gamerule false native overlay without ATD landmark feedback",notifications(packets)==1&&feedback(packets)==0&&effects(packets)==0&&sulfurState(f,cube,hand).equals(before),packets.size());cube.discard();helper.getLevel().getGameRules().set(GameRules.TNT_EXPLODES,true,helper.getLevel().getServer());
                for(boolean baby:List.of(false,true)){var negative=cube(f,baby,Items.AIR);targetLandmark(f,negative);observe(f);before=sulfurState(f,negative,hand);require("baby/nonexplosive native PASS",sulfurUse(f,negative,hand)==InteractionResult.PASS,baby);packets=observe(f);require("baby/nonexplosive no ATD check",notifications(packets)==0&&feedback(packets)==0&&sulfurState(f,negative,hand).equals(before),baby);negative.discard();}
            }
            // Existing sulfur uses an empty bucket, not water; actual independent capture.
            for(InteractionHand hand:InteractionHand.values()){var capture=cube(f,false,Items.STONE);boxes().clear();membership().clear();gates(f,Set.of(AbilityType.USE_WATER_BUCKET));hands(f.player(),hand,Items.BUCKET);observe(f);require("empty sulfur bucket capture with water ability locked",sulfurUse(f,capture,hand).consumesAction()&&capture.isRemoved()&&f.player().getItemInHand(hand).is(Items.SULFUR_CUBE_BUCKET),hand);observe(f);}
            var nativeActor=cube(f,false,Items.TNT);gates(f,Set.of(AbilityType.IGNITE_TNT,AbilityType.USE_FLINT_AND_STEEL,landmarkAbility()));targetLandmark(f,nativeActor);require("autonomous/native prime without player unaffected",nativeActor.primeTime(false)&&nativeActor.isPrimed(),nativeActor.getFuse());nativeActor.discard();observe(f);
        });
    }

    private record Joined(ServerPlayer player, Connection connection, EmbeddedChannel channel) {}
    @FunctionalInterface private interface Scenario { void run() throws Exception; }
    private GameTestHelper helper;
    private String scenario;
    private int assertions;
    private final List<Joined> joined = new ArrayList<>();
    private final List<Entity> entities = new ArrayList<>();
    private final Map<BlockPos,BlockState> blocks = new HashMap<>();
    private final Map<DisplaySlot,Objective> displays = new EnumMap<>(DisplaySlot.class);
    private Objective objective;
    private boolean createdObjective;
    private Map<ChunkPos,Map<LandmarkType,Set<DimensionalBlockBox>>> priorBoxes;
    private Map<LandmarkType,Set<UUID>> priorMembership;
    private Set<UUID> priorItems;
    private Map<AbilityType,Integer> originalThresholds;
    private boolean originalTntRule;
    @SuppressWarnings("unchecked") private Map<AbilityType,Integer> config() throws Exception {
        return (Map<AbilityType,Integer>)field(owner(),AchieveToDoServer.class,"abilitiesConfiguration");
    }
    private void gates(Joined f,Set<AbilityType> locked) throws Exception {
        config().clear();config().putAll(originalThresholds);
        for(AbilityType ability:List.of(AbilityType.OPEN_CHEST,AbilityType.USE_SHEARS,AbilityType.USE_FLINT_AND_STEEL,AbilityType.IGNITE_TNT,AbilityType.USE_WATER_BUCKET,AbilityType.findByLandmarkType(LandmarkType.DESERT_PYRAMID)))
            config().put(ability,locked.contains(ability)?20000:1);
        score(f,10000);observe(f);
        for(AbilityType ability:locked) require("live selected gate locked "+ability,owner().isAbilityLocked(f.player(),ability,true),config().get(ability));
        System.out.println("D_S06_GATE_STATE "+new Gson().toJson(Map.of("scenario",scenario,"locked",locked,"count",10000,"effective_thresholds",config(),"fixture_only",true)));
    }
    private AchieveToDoServer owner() { return AchieveToDoMod.getServer(); }
    private static Object field(Object value, Class<?> type, String name) throws Exception {
        Field f=type.getDeclaredField(name);f.setAccessible(true);return f.get(value);
    }
    private void require(String name, boolean passed, Object observed) {
        System.out.println("D_S06_ASSERT "+new Gson().toJson(Map.of("scenario",scenario,"assertion",name,
            "passed",passed,"observed",String.valueOf(observed),"seq",++assertions)));
        if(!passed)throw new AssertionError(name+": "+observed);
    }
    @SuppressWarnings("unchecked") private int threshold(AbilityType ability) throws Exception {
        int value=((Map<AbilityType,Integer>)field(owner(),AchieveToDoServer.class,"abilitiesConfiguration")).get(ability);
        require("live threshold positive "+ability,value>0,value);return value;
    }
    @SuppressWarnings("unchecked") private void score(Joined fixture,int count) throws Exception {
        helper.getLevel().getServer().getScoreboard().getOrCreatePlayerScore(fixture.player(),objective).set(count);drain(fixture);
        int observed=((Map<UUID,Integer>)field(owner(),AchieveToDoServer.class,"obtainedAdvancementsCountByPlayers")).getOrDefault(fixture.player().getUUID(),Integer.MIN_VALUE);
        require("live score reconciled",observed==count,observed);
    }
    private Joined join() {
        var server=helper.getLevel().getServer();UUID id=UUID.randomUUID();GameProfile profile=new GameProfile(id,"d6"+id.toString().substring(0,10));
        ServerPlayer player=new ServerPlayer(server,helper.getLevel(),profile,ClientInformation.createDefault());
        Connection connection=new Connection(PacketFlow.SERVERBOUND);EmbeddedChannel channel=new EmbeddedChannel(connection);
        Joined fixture=new Joined(player,connection,channel);joined.add(fixture);
        server.getConnection().getConnections().add(connection);
        server.getPlayerList().placeNewPlayer(connection,player,CommonListenerCookie.createInitial(profile,false));
        player.setGameMode(GameType.SURVIVAL);new ServerboundPlayerLoadedPacket().handle(player.connection);
        BlockPos pos=helper.absolutePos(new BlockPos(1,2,1));blocks.putIfAbsent(pos.below(),helper.getLevel().getBlockState(pos.below()));
        helper.getLevel().setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());
        player.teleportTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5);drain(fixture);
        require("joined loaded survival player",server.getPlayerList().getPlayer(id)==player&&player.connection.hasClientLoaded()&&!player.isCreative()&&!player.isSpectator(),id);
        return fixture;
    }
    private List<Object> drain(Joined fixture) {
        fixture.channel().runPendingTasks();fixture.channel().runScheduledPendingTasks();fixture.channel().flushOutbound();
        List<Object> packets=new ArrayList<>();Object packet;
        while((packet=fixture.channel().readOutbound())!=null) {
            if(packet instanceof ClientboundPlayerPositionPacket p)new ServerboundAcceptTeleportationPacket(p.id()).handle(fixture.player().connection);
            packets.add(packet);ReferenceCountUtil.release(packet);
            if(packets.size()>20000)throw new IllegalStateException("Unbounded fixture packet drain");
        }
        return packets;
    }
    private long feedback(List<Object> packets) {
        return packets.stream().filter(p -> p instanceof ClientboundCustomPayloadPacket c && c.payload() instanceof com.diskree.achievetodo.networking.s2c.CheckTargetInLockedLandmarkPayload).count();
    }
    private long opens(List<Object> packets) { return packets.stream().filter(p -> p instanceof ClientboundMountScreenOpenPacket).count(); }
    private long sounds(List<Object> packets) { return packets.stream().filter(p -> p instanceof ClientboundSoundPacket||p instanceof ClientboundSoundEntityPacket).count(); }
    private <T extends Mob> T spawn(EntityType<T> type,Joined fixture) {
        T target=helper.spawn(type,new BlockPos(1,2,3));entities.add(target);target.setNoAi(true);
        target.setPos(fixture.player().getX()+2,fixture.player().getY(),fixture.player().getZ());
        if(target instanceof AgeableMob ageable)ageable.setAge(0);return target;
    }
    @SuppressWarnings("unchecked") private Map<ChunkPos,Map<LandmarkType,Set<DimensionalBlockBox>>> boxes() throws Exception {
        return (Map<ChunkPos,Map<LandmarkType,Set<DimensionalBlockBox>>>)field(owner(),AchieveToDoServer.class,"landmarksByChunks");
    }
    @SuppressWarnings("unchecked") private Map<LandmarkType,Set<UUID>> membership() throws Exception {
        return (Map<LandmarkType,Set<UUID>>)field(owner(),AchieveToDoServer.class,"playersByLockedLandmarkTypes");
    }
    private void landmark(Joined fixture,Entity target,boolean locked) throws Exception {
        boxes().clear();membership().clear();
        BlockPos p=target.blockPosition();var box=new BoundingBox(p.getX()-1,p.getY()-1,p.getZ()-1,p.getX()+1,p.getY()+3,p.getZ()+1);
        var chunk=new HashMap<LandmarkType,Set<DimensionalBlockBox>>();
        chunk.put(LandmarkType.DESERT_PYRAMID,new HashSet<>(Set.of(new DimensionalBlockBox(DimensionType.OVERWORLD,box))));
        boxes().put(new ChunkPos(p.getX() >> 4,p.getZ() >> 4),chunk);
        membership().put(LandmarkType.DESERT_PYRAMID,new HashSet<>(Set.of(fixture.player().getUUID())));
        score(fixture,locked?threshold(AbilityType.findByLandmarkType(LandmarkType.DESERT_PYRAMID))-1:10000);
        require("actual target permission "+locked,owner().isTargetInLockedLandmark(fixture.player(),DimensionType.OVERWORLD,target.getBoundingBox(),true)==locked,target.getUUID());drain(fixture);
    }
    private AABB region() { return new AABB(Vec3.atLowerCornerOf(helper.absolutePos(new BlockPos(-10,-5,-10))),Vec3.atLowerCornerOf(helper.absolutePos(new BlockPos(25,15,25)))); }
    private List<ItemEntity> drops() { return helper.getLevel().getEntitiesOfClass(ItemEntity.class,region()).stream().filter(e -> !priorItems.contains(e.getUUID())).toList(); }
    private void hands(ServerPlayer player,InteractionHand hand,Item item) {
        player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);player.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
        player.setItemInHand(hand,new ItemStack(item));
    }
    private InteractionResult interact(Joined fixture,Entity target,InteractionHand hand) {
        ServerPlayer player=fixture.player();String before=state(player,target,hand);
        InteractionResult result=player.interactOn(target,hand,Vec3.ZERO);
        System.out.println("D_S06_STATE "+new Gson().toJson(Map.of("scenario",scenario,"native_path","Player.interactOn/Entity.interact/subtype or ItemStack fallback",
            "player",player.getUUID(),"target",target.getUUID(),"hand",hand,"before",before,"after",state(player,target,hand),"result",result.toString())));
        return result;
    }
    private String state(ServerPlayer player,Entity target,InteractionHand hand) {
        return "secondary="+player.isSecondaryUseActive()+" baby="+(target instanceof LivingEntity living&&living.isBaby())+
            " health="+(target instanceof LivingEntity living?living.getHealth():0)+" tame="+
            (target instanceof net.minecraft.world.entity.animal.equine.AbstractHorse horse?horse.isTamed():target instanceof TamableAnimal tame?tame.isTame():"not_applicable")+
            " hand="+player.getItemInHand(hand)+" menu="+player.containerMenu.getClass().getSimpleName()+":"+player.containerMenu.containerId+
            " riding="+(player.getVehicle()==target)+" body="+(target instanceof LivingEntity living?living.getItemBySlot(EquipmentSlot.BODY):"none")+
            " saddle="+(target instanceof LivingEntity living?living.getItemBySlot(EquipmentSlot.SADDLE):"none");
    }
    private void cleanup() throws Exception {
        Throwable failure=null;
        for(Joined fixture:new ArrayList<>(joined))try {
            fixture.player().closeContainer();fixture.player().stopRiding();fixture.player().stopUsingItem();fixture.player().getAdvancements().save();drain(fixture);
            fixture.connection().disconnect(Component.literal("D-S06 fixture complete"));fixture.channel().runPendingTasks();fixture.channel().finishAndReleaseAll();
            if(!(Boolean)field(fixture.connection(),Connection.class,"disconnectionHandled"))fixture.connection().handleDisconnection();
            var server=helper.getLevel().getServer();server.getConnection().getConnections().remove(fixture.connection());
            require("player connection channel removed",server.getPlayerList().getPlayer(fixture.player().getUUID())==null&&!fixture.channel().isOpen()&&!server.getConnection().getConnections().contains(fixture.connection()),fixture.player().getUUID());
            server.getScoreboard().resetAllPlayerScores(fixture.player());
            require("temporary score removed",server.getScoreboard().getPlayerScoreInfo(fixture.player(),objective)==null,fixture.player().getUUID());joined.remove(fixture);
        }catch(Throwable t){if(failure==null)failure=t;else failure.addSuppressed(t);}
        for(Entity entity:entities){entity.discard();require("fixture entity discarded",entity.isRemoved(),entity.getUUID());}entities.clear();
        for(ItemEntity drop:drops())drop.discard();require("fixture drops removed",drops().isEmpty(),drops().size());
        config().clear();config().putAll(originalThresholds);
        helper.getLevel().getGameRules().set(GameRules.TNT_EXPLODES,originalTntRule,helper.getLevel().getServer());
        require("effective thresholds and gamerule restored",config().equals(originalThresholds)&&helper.getLevel().getGameRules().get(GameRules.TNT_EXPLODES)==originalTntRule,config().size());
        boxes().clear();boxes().putAll(priorBoxes);membership().clear();membership().putAll(priorMembership);
        require("landmark maps restored",boxes().equals(priorBoxes)&&membership().equals(priorMembership),scenario);
        blocks.forEach((p,b) -> helper.getLevel().setBlockAndUpdate(p,b));
        for(var e:blocks.entrySet())require("world block restored",helper.getLevel().getBlockState(e.getKey()).equals(e.getValue()),e.getKey());blocks.clear();
        for(ItemEntity drop:drops())drop.discard();require("post-block-restoration fixture drops removed",drops().isEmpty(),drops().size());
        var board=helper.getLevel().getServer().getScoreboard();displays.forEach(board::setDisplayObjective);if(createdObjective){board.removeObjective(objective);createdObjective=false;}
        owner().prepareScoreboard(board);
        for(var e:displays.entrySet())require("scoreboard display restored",board.getDisplayObjective(e.getKey())==e.getValue(),e.getKey());
        if(failure!=null)throw new IllegalStateException("D-S06 cleanup failed",failure);
    }
    private void run(GameTestHelper helper,String name,Scenario body) {
        this.helper=helper;scenario=name;var board=helper.getLevel().getServer().getScoreboard();
        for(DisplaySlot slot:DisplaySlot.values())displays.put(slot,board.getDisplayObjective(slot));
        objective=board.getObjective("bac_advancements");if(objective==null){createdObjective=true;objective=board.addObjective("bac_advancements",ObjectiveCriteria.DUMMY,Component.literal("D-S06"),ObjectiveCriteria.RenderType.INTEGER,false,null);}
        board.setDisplayObjective(DisplaySlot.SIDEBAR,objective);owner().prepareScoreboard(board);
        helper.runAfterDelay(5,() -> {
            try {
                originalThresholds=new EnumMap<>(config());originalTntRule=helper.getLevel().getGameRules().get(GameRules.TNT_EXPLODES);priorBoxes=new HashMap<>(boxes());priorMembership=new HashMap<>(membership());priorItems=new HashSet<>();
                helper.getLevel().getEntitiesOfClass(ItemEntity.class,region()).forEach(e -> priorItems.add(e.getUUID()));
                require("server permission ready",!owner().isNotReady(),name);Joined observer=join();score(observer,10000);body.run();cleanup();
                System.out.println("D_S06_GAMEPLAY_RECEIPT "+new Gson().toJson(Map.of("scenario",scenario,"status","PASS","assertions",assertions,"cleanup",true,"scope","Joined server native interactions; bounded disk codec checks where assertions record them; client GUI and full restart NOT_RUN")));helper.succeed();
            }catch(Throwable t){t.printStackTrace();try{cleanup();}catch(Throwable c){t.addSuppressed(c);c.printStackTrace();}
                System.out.println("D_S06_GAMEPLAY_RECEIPT "+new Gson().toJson(Map.of("scenario",scenario,"status","FAIL","error",t.toString())));helper.fail(name+": "+t);}
        });
    }

}
