package com.diskree.achievetodo.certification;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.*;
import com.diskree.achievetodo.injection.extension.main.LevelInfoExtension;
import com.google.gson.*;
import net.fabricmc.fabric.api.gametest.v1.*;
import net.minecraft.advancements.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.*;
import java.lang.reflect.Method;
import java.util.*;
import static com.diskree.achievetodo.certification.Final19StaticContext.require;

/** Fresh post-fix gate regression. Separate from advancement certification accounting. */
public final class Final19WaxGateRegressionGameTest implements CustomTestMethodInvoker {
    private static final String[] MODES={"LOCKED_MATERIAL","UNLOCKED_MATERIAL","LOCKED_LANDMARK","OUTSIDE_LOCKED_LANDMARK","WRONG_ITEM","NO_OP"};
    @GameTest(maxTicks=1800) public void axeGateRegression14(GameTestHelper h) throws Exception {
        System.out.println("FINAL19_AXE_GATE_REGRESSION_START");
        JsonObject artifact=new JsonObject();artifact.addProperty("runId",UUID.randomUUID().toString());
        artifact.addProperty("productionSha256",Final19StaticContext.sha(java.nio.file.Files.readAllBytes(Final19WorldgenEvidence.root().resolve("src/main/java/com/diskree/achievetodo/injection/mixin/main/AxeItemMixin.java"))));
        artifact.addProperty("certificationGain",0);artifact.add("entries",new JsonArray());next(h,artifact,0);
    }
    private static void next(GameTestHelper h,JsonObject artifact,int index){
        if(index==14){try{artifact.addProperty("stage","POST_FIX_REGRESSION_GREEN");Final19WorldgenEvidence.write(Final19WorldgenEvidence.root().resolve("build/tmp/final19_implementation/axe_gate_regression.json"),artifact);System.out.println("FINAL19_AXE_GATE_REGRESSION_GREEN runId="+artifact.get("runId")+" entries=14");h.succeed();}catch(Throwable e){h.fail("Axe regression completion: "+e);}return;}
        String mode=MODES[index<12?index/3:index-8];int path=index<12?index%3:2;
        Context c=new Context();c.pos=h.absolutePos(new BlockPos(3,1,3));c.mode=mode;c.path=path;
        try{
            c.original=h.getLevel().getBlockState(c.pos);
            c.actor=Final19NativeActors.join(h.getLevel(),c.pos.offset(0,0,1));
            h.runAfterDelay(1,()->act(h,artifact,index,c));
        }catch(Throwable e){fail(h,c,e);}
    }
    private static void act(GameTestHelper h,JsonObject artifact,int index,Context c){try{
        var actor=c.actor;var player=actor.player();var server=h.getLevel().getServer();
        var configuration=((LevelInfoExtension)server.getWorldData().getLevelSettings()).achievetodo$getAbilitiesConfiguration(server.overworld().getSeed());
        int materialThreshold=configuration.get(AbilityType.USE_DIAMOND_TOOLS);
        require(materialThreshold>0,"Missing material gate");
        var board=server.getScoreboard();var objective=board.getObjective("bac_advancements");require(objective!=null,"Missing live score objective");
        JsonObject unlock=new JsonObject();if(!c.mode.equals("LOCKED_MATERIAL"))unlock=Final19NativeActors.unlock(actor,AbilityType.USE_DIAMOND_TOOLS);else board.getOrCreatePlayerScore(player,objective).set(0);
        boolean materialLockedBefore=AchieveToDoMod.isAbilityLocked(player,AbilityType.USE_DIAMOND_TOOLS,true);
        require(materialLockedBefore==c.mode.equals("LOCKED_MATERIAL"),"Unexpected live material state");
        int landmarkThreshold=0;
        if(c.mode.contains("LANDMARK")){
            var ability=AbilityType.INTERACT_INSIDE_DESERT_WELL;landmarkThreshold=configuration.get(ability);
            require(landmarkThreshold>materialThreshold&&AchieveToDoMod.isAbilityLocked(player,ability,true),"Need independently locked landmark with unlocked axe");
            BlockPos landmarkPos=c.mode.equals("LOCKED_LANDMARK")?c.pos:c.pos.offset(4,0,0);
            c.landmarks=Map.of(LandmarkType.DESERT_WELL,Set.of(new DimensionalBlockBox(DimensionType.findByWorld(h.getLevel().dimension()),new BoundingBox(landmarkPos))));
            c.landmarkChunk=new ChunkPos(landmarkPos.getX() >> 4,landmarkPos.getZ() >> 4);AchieveToDoMod.getServer().onLandmarksLoadedStatusChanged(h.getLevel(),c.landmarkChunk,c.landmarks,true);
            require(AchieveToDoMod.isTargetInLockedLandmark(player,h.getLevel(),landmarkPos),"Production landmark fixture not locked");
        }
        boolean landmarkLockedBefore=AchieveToDoMod.isTargetInLockedLandmark(player,h.getLevel(),c.pos);
        require(landmarkLockedBefore==c.mode.equals("LOCKED_LANDMARK"),"Unexpected target landmark gate");
        var beforeBlock=block(c.mode.equals("NO_OP")?"minecraft:copper_block":c.path==0?"minecraft:oak_log":c.path==1?"minecraft:exposed_copper":"minecraft:waxed_copper_block").defaultBlockState();
        h.getLevel().setBlockAndUpdate(c.pos,beforeBlock);
        player.getInventory().setItem(player.getInventory().getSelectedSlot(),new ItemStack(c.mode.equals("WRONG_ITEM")?Items.STICK:Items.DIAMOND_AXE));
        var advancement=server.getAdvancements().get(Identifier.parse("minecraft:husbandry/wax_off"));require(advancement!=null,"Missing wax_off");
        boolean criterionBefore=Final19NativeActors.done(actor,advancement,"wax_off");require(!criterionBefore,"Precompleted target");
        JsonObject progressBefore=progress(player.getAdvancements());int damageBefore=player.getMainHandItem().getDamageValue();
        boolean axeTag=player.getMainHandItem().is(TagKey.create(Registries.ITEM,Identifier.parse("minecraft:axes")));
        boolean waxTag=h.getLevel().getBlockState(c.pos).is(TagKey.create(Registries.BLOCK,Identifier.parse("blazeandcave:waxed_copper_blocks")));
        String heldItem=BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).toString();
        new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(c.pos),Direction.UP,c.pos,false),index+1).handle(player.connection);
        var afterBlock=h.getLevel().getBlockState(c.pos);int damageAfter=player.getMainHandItem().getDamageValue();
        boolean materialLockedAfter=AchieveToDoMod.isAbilityLocked(player,AbilityType.USE_DIAMOND_TOOLS,true);
        boolean landmarkLockedAfter=AchieveToDoMod.isTargetInLockedLandmark(player,h.getLevel(),c.pos);
        boolean criterionAfter=Final19NativeActors.done(actor,advancement,"wax_off");JsonObject progressAfter=progress(player.getAdvancements());
        boolean success=c.mode.equals("UNLOCKED_MATERIAL")||c.mode.equals("OUTSIDE_LOCKED_LANDMARK");
        String expectedAfter=c.path==0?"minecraft:stripped_oak_log":"minecraft:copper_block";
        require(materialLockedBefore==materialLockedAfter&&landmarkLockedBefore==landmarkLockedAfter,"Gate changed during native action");
        if(success){require(afterBlock.is(block(expectedAfter))&&damageAfter==damageBefore+1,"Native transformation/durability mismatch");require(criterionAfter==(c.path==2),"Wrong wax criterion progress");}
        else require(afterBlock.equals(beforeBlock)&&damageAfter==damageBefore&&!criterionAfter&&progressBefore.equals(progressAfter),"Blocked/negative packet mutated state or progress");
        if(c.path==2&&!c.mode.equals("NO_OP"))require(waxTag,"Missing actual pre-mutation wax tag");
        require(axeTag==!c.mode.equals("WRONG_ITEM"),"Incorrect actual held axe tag");
        JsonObject row=new JsonObject();row.addProperty("mode",c.mode);row.addProperty("path",c.path==0?"STRIP":c.path==1?"SCRAPE":"WAX_OFF");
        row.addProperty("playerUuid",player.getUUID().toString());row.addProperty("gameMode",player.gameMode().name());row.addProperty("joined",server.getPlayerList().getPlayer(player.getUUID())==player);row.addProperty("connectionRegistered",server.getConnection().getConnections().contains(actor.connection()));row.addProperty("clientLoaded",player.connection.hasClientLoaded());row.addProperty("finiteMaterials",!player.hasInfiniteMaterials());
        row.addProperty("nativeBoundary","ServerboundUseItemOnPacket.handle");row.addProperty("heldItem",heldItem);row.addProperty("axeTagMember",axeTag);row.addProperty("preMutationWaxTagMember",waxTag);
        row.addProperty("materialThreshold",materialThreshold);row.addProperty("landmarkThreshold",landmarkThreshold);row.addProperty("landmarkFixtureSource","OWNED_PRODUCTION_LOADED_LANDMARK_METADATA_CALLBACK");row.addProperty("materialLockedBefore",materialLockedBefore);row.addProperty("materialLockedAfter",materialLockedAfter);row.addProperty("landmarkLockedBefore",landmarkLockedBefore);row.addProperty("landmarkLockedAfter",landmarkLockedAfter);row.add("unlock",unlock);
        row.addProperty("blockBefore",beforeBlock.toString());row.addProperty("blockAfter",afterBlock.toString());row.addProperty("damageBefore",damageBefore);row.addProperty("damageAfter",damageAfter);row.addProperty("criterionBefore",criterionBefore);row.addProperty("criterionAfter",criterionAfter);row.add("progressBefore",progressBefore);row.add("progressAfter",progressAfter);row.add("cleanup",cleanup(h,c));artifact.getAsJsonArray("entries").add(row);
        Final19WorldgenEvidence.write(Final19WorldgenEvidence.root().resolve("build/tmp/final19_implementation/axe_gate_regression.json"),artifact);h.runAfterDelay(1,()->next(h,artifact,index+1));
    }catch(Throwable e){fail(h,c,e);}}
    private static Block block(String id){return BuiltInRegistries.BLOCK.getValue(Identifier.parse(id));}
    @SuppressWarnings("unchecked") private static JsonObject progress(net.minecraft.server.PlayerAdvancements advancements)throws Exception{
        var field=net.minecraft.server.PlayerAdvancements.class.getDeclaredField("progress");field.setAccessible(true);JsonObject result=new JsonObject();
        for(var e:((Map<AdvancementHolder,AdvancementProgress>)field.get(advancements)).entrySet()){JsonArray completed=new JsonArray();e.getValue().getCompletedCriteria().forEach(completed::add);if(!completed.isEmpty())result.add(e.getKey().id().toString(),completed);}return result;
    }
    private static JsonObject cleanup(GameTestHelper h,Context c){
        if(c.landmarks!=null){AchieveToDoMod.getServer().onLandmarksLoadedStatusChanged(h.getLevel(),c.landmarkChunk,c.landmarks,false);c.landmarks=null;}
        h.getLevel().setBlockAndUpdate(c.pos,c.original);boolean restored=h.getLevel().getBlockState(c.pos).equals(c.original);
        boolean landmarkRestored=c.actor==null||!AchieveToDoMod.isTargetInLockedLandmark(c.actor.player(),h.getLevel(),c.pos);
        var clean=c.actor==null?new JsonObject():Final19NativeActors.close(c.actor);clean.addProperty("blocksRestored",restored);clean.addProperty("ownedLandmarkRemoved",landmarkRestored);return clean;
    }
    private static void fail(GameTestHelper h,Context c,Throwable e){try{cleanup(h,c);}catch(Throwable suppressed){e.addSuppressed(suppressed);}h.fail("Axe regression "+c.mode+" path="+c.path+": "+e);}
    private static class Context{BlockPos pos;BlockState original;String mode;int path;Final19NativeActors.Actor actor;ChunkPos landmarkChunk;Map<LandmarkType,Set<DimensionalBlockBox>> landmarks;}
    @Override public void invokeTestMethod(GameTestHelper h,Method method)throws ReflectiveOperationException{h.setBlock(0,0,0,Blocks.AIR);method.invoke(this,h);}
}
