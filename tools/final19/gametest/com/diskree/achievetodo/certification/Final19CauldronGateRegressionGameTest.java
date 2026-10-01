package com.diskree.achievetodo.certification;
import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.*;
import com.diskree.achievetodo.injection.extension.main.LevelInfoExtension;
import com.diskree.achievetodo.injection.mixin.main.AbstractCauldronBlockAccessor;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.gametest.v1.*;
import net.minecraft.advancements.*;
import net.minecraft.core.*;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.*;
import net.minecraft.tags.TagKey;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.*;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.*;
import static com.diskree.achievetodo.certification.Final19StaticContext.require;

/** Zero-gain production regression, independent of DualTags certification. */
public final class Final19CauldronGateRegressionGameTest implements CustomTestMethodInvoker {
    static final String[] CONTROLS={"WRONG_ITEM","EMPTY_HAND","NO_OP_LEATHER","NO_OP_BANNER","NO_OP_SHULKER","FALLBACK_BANNER","FALLBACK_SHULKER","SECONDARY_LEATHER","SECONDARY_PLACEMENT","NON_CAULDRON","DEFAULT_PLACEMENT","REGISTERED_BUCKET"};
    @GameTest(maxTicks=2400) public void cauldronGateRegression24(GameTestHelper h)throws Exception{
        System.out.println("FINAL19_CAULDRON_GATE_REGRESSION_START");JsonObject output=new JsonObject();output.addProperty("runId",UUID.randomUUID().toString());output.addProperty("certificationGain",0);output.add("entries",new JsonArray());
        var record=Final19WorldgenEvidence.read(Final19WorldgenEvidence.root().resolve("reference/phase_a_planning/final19/cauldron_gate_authorized_application.json"));JsonObject hashes=new JsonObject();
        for(var e:record.getAsJsonObject("postconditions").entrySet())hashes.addProperty(e.getKey(),Final19StaticContext.sha(Files.readAllBytes(Final19WorldgenEvidence.root().resolve(e.getKey()))));
        output.add("productionHashes",hashes);output.addProperty("patchSha256",Final19StaticContext.sha(Files.readAllBytes(Final19WorldgenEvidence.root().resolve(record.get("patchPath").getAsString()))));next(h,output,0);
    }
    static void next(GameTestHelper h,JsonObject output,int i){
        if(i==24){try{output.addProperty("stage","POST_FIX_REGRESSION_GREEN");save(output);System.out.println("FINAL19_CAULDRON_GATE_REGRESSION_GREEN runId="+output.get("runId")+" entries=24");h.succeed();}catch(Throwable e){h.fail("Cauldron completion: "+e);}return;}
        Context c=new Context();c.mode=i<12?List.of("LOCKED_MATERIAL","UNLOCKED_MATERIAL","LOCKED_LANDMARK","OUTSIDE_LOCKED_LANDMARK").get(i/3):CONTROLS[i-12];c.path=i<12?i%3:c.mode.endsWith("BANNER")?1:c.mode.endsWith("SHULKER")?2:0;
        try{c.pos=h.absolutePos(new BlockPos(3,1,3));for(BlockPos p:List.of(c.pos,c.pos.above(),c.pos.below()))c.blocks.put(p,h.getLevel().getBlockState(p));h.getLevel().setBlockAndUpdate(c.pos.below(),Blocks.STONE.defaultBlockState());h.getLevel().setBlockAndUpdate(c.pos.above(),Blocks.AIR.defaultBlockState());c.actor=Final19NativeActors.join(h.getLevel(),c.pos.offset(0,0,1));
            if(c.mode.startsWith("SECONDARY"))new ServerboundPlayerInputPacket(new Input(false,false,false,false,false,true,false)).handle(c.actor.player().connection);
            h.runAfterDelay(2,()->act(h,output,i,c));
        }catch(Throwable e){fail(h,c,e);}
    }
    static void act(GameTestHelper h,JsonObject output,int i,Context c){try{
        var player=c.actor.player();var server=h.getLevel().getServer();var board=server.getScoreboard();var objective=board.getObjective("bac_advancements");require(objective!=null,"Live score missing");board.getOrCreatePlayerScore(player,objective).set(0);
        var config=((LevelInfoExtension)server.getWorldData().getLevelSettings()).achievetodo$getAbilitiesConfiguration(server.overworld().getSeed());int threshold=config.get(AbilityType.USE_CAULDRON),landmarkThreshold=config.get(AbilityType.INTERACT_INSIDE_DESERT_WELL);
        boolean unlocked=c.mode.equals("UNLOCKED_MATERIAL")||c.mode.contains("LANDMARK")||c.mode.startsWith("NO_OP")||c.mode.equals("REGISTERED_BUCKET");JsonObject unlock=unlocked?Final19NativeActors.unlock(c.actor,AbilityType.USE_CAULDRON):new JsonObject();
        if(c.mode.contains("LANDMARK")){
            require(landmarkThreshold>threshold&&AchieveToDoMod.isAbilityLocked(player,AbilityType.INTERACT_INSIDE_DESERT_WELL,true),"Need independently locked landmark");c.landmarkPos=c.mode.equals("LOCKED_LANDMARK")?c.pos:c.pos.offset(4,0,0);c.chunk=new ChunkPos(c.landmarkPos.getX()>>4,c.landmarkPos.getZ()>>4);
            c.landmarks=Map.of(LandmarkType.DESERT_WELL,Set.of(new DimensionalBlockBox(DimensionType.findByWorld(h.getLevel().dimension()),new BoundingBox(c.landmarkPos))));AchieveToDoMod.getServer().onLandmarksLoadedStatusChanged(h.getLevel(),c.chunk,c.landmarks,true);require(AchieveToDoMod.isTargetInLockedLandmark(player,h.getLevel(),c.landmarkPos),"Live landmark missing");
        }
        String criterion=c.path==0?"clean_leather_armor":c.path==1?"clean_banner":"clean_shulker_box",tag=c.path==0?"minecraft:leather_armor":c.path==1?"minecraft:banners":"minecraft:shulker_boxes";
        var advancement=server.getAdvancements().get(Identifier.parse("blazeandcave:building/washing_machine"));require(advancement!=null&&!Final19NativeActors.done(c.actor,advancement,criterion),"Missing/precompleted target");
        BlockState state=Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL,3);ItemStack stack=cleaningStack(h,c.path);
        if(c.mode.equals("WRONG_ITEM"))stack=new ItemStack(Items.STICK);
        if(c.mode.equals("EMPTY_HAND"))stack=ItemStack.EMPTY;
        if(c.mode.startsWith("NO_OP")){stack=new ItemStack(c.path==0?Items.LEATHER_CHESTPLATE:c.path==1?item("minecraft:white_banner"):Items.SHULKER_BOX);h.getLevel().setBlockAndUpdate(c.pos.above(),Blocks.STONE.defaultBlockState());if(c.path==2)state=Blocks.CAULDRON.defaultBlockState();}
        if(c.mode.equals("SECONDARY_PLACEMENT")||c.mode.equals("NON_CAULDRON")||c.mode.equals("DEFAULT_PLACEMENT"))stack=new ItemStack(Items.COBBLESTONE);
        if(c.mode.equals("NON_CAULDRON"))state=Blocks.STONE.defaultBlockState();
        if(c.mode.equals("REGISTERED_BUCKET")){state=Blocks.CAULDRON.defaultBlockState();stack=new ItemStack(Items.WATER_BUCKET);}
        h.getLevel().setBlockAndUpdate(c.pos,state);player.getInventory().setItem(player.getInventory().getSelectedSlot(),stack);
        boolean lockedBefore=AchieveToDoMod.isAbilityLocked(player,AbilityType.USE_CAULDRON,true),landmarkBefore=AchieveToDoMod.isTargetInLockedLandmark(player,h.getLevel(),c.pos),secondary=player.isSecondaryUseActive();require(lockedBefore==!unlocked,"Wrong live ability state");require(secondary==c.mode.startsWith("SECONDARY"),"Native secondary input not reflected");
        boolean blockMember=state.is(TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:cauldrons"))),itemMember=stack.is(TagKey.create(Registries.ITEM,Identifier.parse(tag)));
        String classification=!(state.getBlock() instanceof AbstractCauldronBlock)?"NON_CAULDRON":((AbstractCauldronBlockAccessor)state.getBlock()).achievetodo$getInteractions().get(stack)==CauldronInteraction.DEFAULT?"DEFAULT":"REGISTERED";
        JsonObject before=snapshot(h,c,advancement,criterion);int scoreBefore=board.getPlayerScoreInfo(player,objective).value();
        Final19CauldronResultProbe.arm(player.getUUID());new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(c.pos),Direction.UP,c.pos,false),i+1).handle(player.connection);var result=Final19CauldronResultProbe.take(player.getUUID());
        JsonObject after=snapshot(h,c,advancement,criterion);boolean lockedAfter=AchieveToDoMod.isAbilityLocked(player,AbilityType.USE_CAULDRON,true),landmarkAfter=AchieveToDoMod.isTargetInLockedLandmark(player,h.getLevel(),c.pos);
        boolean success=i<12&&(c.mode.equals("UNLOCKED_MATERIAL")||c.mode.equals("OUTSIDE_LOCKED_LANDMARK"));boolean placement=Set.of("SECONDARY_PLACEMENT","NON_CAULDRON","DEFAULT_PLACEMENT").contains(c.mode);boolean bucket=c.mode.equals("REGISTERED_BUCKET");
        require(lockedBefore==lockedAfter&&landmarkBefore==landmarkAfter,"Gate changed during packet");
        if(success){require(after.get("waterLevel").getAsInt()==2&&result.consumesAction()&&after.get("criterion").getAsBoolean(),"Native cleaning failed: "+after+" result="+result);ItemStack actual=player.getMainHandItem();require(c.path==0?!actual.has(DataComponents.DYED_COLOR):c.path==1?actual.getOrDefault(DataComponents.BANNER_PATTERNS,BannerPatternLayers.EMPTY).layers().isEmpty():actual.is(Items.SHULKER_BOX),"Cleaning item components incorrect");require(!before.get("item").equals(after.get("item")),"Cleaning unchanged item bytes");}
        else if(placement){require(result.consumesAction()&&h.getLevel().getBlockState(c.pos.above()).is(Blocks.COBBLESTONE)&&player.getMainHandItem().isEmpty()&&!after.get("criterion").getAsBoolean(),"Vanilla placement control failed");require(before.get("block").equals(after.get("block")),"Placement changed cauldron target");}
        else if(bucket){require(result.consumesAction()&&after.get("waterLevel").getAsInt()==3&&player.getMainHandItem().is(Items.BUCKET)&&!after.get("criterion").getAsBoolean(),"Registered bucket control failed");}
        else{require(before.equals(after),"Negative mutated actual state/progress: "+c.mode+" before="+before+" after="+after);boolean blocked=i<12||c.mode.startsWith("FALLBACK");if(blocked)require(result==InteractionResult.FAIL,"Closed gate did not terminate complete interaction");else require(!result.consumesAction(),"Negative consumed native action");}
        JsonObject row=new JsonObject();row.addProperty("mode",c.mode);row.addProperty("path",c.path==0?"LEATHER":c.path==1?"BANNER":"SHULKER");row.addProperty("criterion",criterion);row.addProperty("playerUuid",player.getUUID().toString());row.addProperty("gameMode",player.gameMode().name());row.addProperty("joined",server.getPlayerList().getPlayer(player.getUUID())==player);row.addProperty("connectionRegistered",server.getConnection().getConnections().contains(c.actor.connection()));row.addProperty("clientLoaded",player.connection.hasClientLoaded());row.addProperty("finiteMaterials",!player.hasInfiniteMaterials());row.addProperty("nativeBoundary","ServerboundUseItemOnPacket.handle");row.addProperty("resultProbe","ONE_ORIGINAL_CALL_UNMODIFIED_RETURN");row.addProperty("nativeResult",result.toString());row.addProperty("resultClass",result.getClass().getSimpleName());row.addProperty("consumesAction",result.consumesAction());row.addProperty("dispatcherClassification",classification);row.addProperty("checkedItemTag",tag);row.addProperty("itemTagMember",itemMember);row.addProperty("checkedBlockTag","minecraft:cauldrons");row.addProperty("blockTagMember",blockMember);row.addProperty("secondaryUse",secondary);row.addProperty("liveThreshold",threshold);row.addProperty("landmarkThreshold",landmarkThreshold);row.addProperty("scoreBefore",scoreBefore);row.addProperty("scoreAfter",board.getPlayerScoreInfo(player,objective).value());row.addProperty("lockedBefore",lockedBefore);row.addProperty("lockedAfter",lockedAfter);row.addProperty("landmarkBefore",landmarkBefore);row.addProperty("landmarkAfter",landmarkAfter);row.add("unlock",unlock);row.add("before",before);row.add("after",after);row.add("cleanup",cleanup(h,c));output.getAsJsonArray("entries").add(row);save(output);h.runAfterDelay(1,()->next(h,output,i+1));
    }catch(Throwable e){fail(h,c,e);}}
    static Item item(String id){return BuiltInRegistries.ITEM.getValue(Identifier.parse(id));}
    static ItemStack cleaningStack(GameTestHelper h,int path){ItemStack stack=new ItemStack(path==0?Items.LEATHER_CHESTPLATE:item(path==1?"minecraft:white_banner":"minecraft:red_shulker_box"));if(path==0)stack.set(DataComponents.DYED_COLOR,new DyedItemColor(0xAA0000));if(path==1){var pattern=h.getLevel().registryAccess().lookupOrThrow(Registries.BANNER_PATTERN).getOrThrow(ResourceKey.create(Registries.BANNER_PATTERN,Identifier.parse("minecraft:cross")));stack.set(DataComponents.BANNER_PATTERNS,new BannerPatternLayers.Builder().add(pattern,DyeColor.RED).build());}return stack;}
    static JsonObject snapshot(GameTestHelper h,Context c,AdvancementHolder advancement,String criterion)throws Exception{var player=c.actor.player();var state=h.getLevel().getBlockState(c.pos);JsonObject result=new JsonObject();result.addProperty("block",state.toString());result.addProperty("above",h.getLevel().getBlockState(c.pos.above()).toString());result.addProperty("waterLevel",state.hasProperty(LayeredCauldronBlock.LEVEL)?state.getValue(LayeredCauldronBlock.LEVEL):0);result.addProperty("item",player.getMainHandItem().isEmpty()?"EMPTY":ItemStack.CODEC.encodeStart(RegistryOps.create(JsonOps.INSTANCE,h.getLevel().registryAccess()),player.getMainHandItem()).result().orElseThrow().toString());result.addProperty("itemId",BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).toString());result.addProperty("count",player.getMainHandItem().getCount());result.addProperty("damage",player.getMainHandItem().getDamageValue());result.addProperty("criterion",Final19NativeActors.done(c.actor,advancement,criterion));result.add("progress",progress(player.getAdvancements()));return result;}
    @SuppressWarnings("unchecked") static JsonObject progress(net.minecraft.server.PlayerAdvancements advancements)throws Exception{var field=net.minecraft.server.PlayerAdvancements.class.getDeclaredField("progress");field.setAccessible(true);JsonObject result=new JsonObject();for(var e:((Map<AdvancementHolder,AdvancementProgress>)field.get(advancements)).entrySet()){JsonArray complete=new JsonArray();e.getValue().getCompletedCriteria().forEach(complete::add);if(!complete.isEmpty())result.add(e.getKey().id().toString(),complete);}return result;}
    static JsonObject cleanup(GameTestHelper h,Context c){boolean landmarkRestored=true;if(c.landmarks!=null){AchieveToDoMod.getServer().onLandmarksLoadedStatusChanged(h.getLevel(),c.chunk,c.landmarks,false);landmarkRestored=!AchieveToDoMod.isTargetInLockedLandmark(c.actor.player(),h.getLevel(),c.landmarkPos);c.landmarks=null;}boolean restored=true;for(var e:c.blocks.entrySet()){h.getLevel().setBlockAndUpdate(e.getKey(),e.getValue());restored&=h.getLevel().getBlockState(e.getKey()).equals(e.getValue());}c.blocks.clear();if(c.actor!=null)Final19CauldronResultProbe.clear(c.actor.player().getUUID());var clean=c.actor==null?new JsonObject():Final19NativeActors.close(c.actor);clean.addProperty("blocksRestored",restored);clean.addProperty("ownedLandmarkRemoved",landmarkRestored);clean.addProperty("fixtureEntitiesRemoved",true);return clean;}
    static void save(JsonObject value)throws Exception{Final19WorldgenEvidence.write(Final19WorldgenEvidence.root().resolve("build/tmp/final19_implementation/cauldron_gate_regression.json"),value);}
    static void fail(GameTestHelper h,Context c,Throwable e){try{cleanup(h,c);}catch(Throwable other){e.addSuppressed(other);}h.fail("Cauldron regression "+c.mode+" path="+c.path+": "+e);}
    static class Context{String mode;int path;BlockPos pos,landmarkPos;ChunkPos chunk;Final19NativeActors.Actor actor;Map<BlockPos,BlockState> blocks=new LinkedHashMap<>();Map<LandmarkType,Set<DimensionalBlockBox>> landmarks;}
    @Override public void invokeTestMethod(GameTestHelper h,Method method)throws ReflectiveOperationException{h.setBlock(0,0,0,Blocks.AIR);method.invoke(this,h);}
}
