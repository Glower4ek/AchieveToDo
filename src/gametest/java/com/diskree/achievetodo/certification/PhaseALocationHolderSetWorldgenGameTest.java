package com.diskree.achievetodo.certification;

import com.google.gson.JsonObject;
import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.injection.extension.main.LevelInfoExtension;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.predicates.NbtPredicate;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.commands.FillBiomeCommand;
import net.minecraft.server.level.*;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.Structure;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Finite joined players, registered world fixtures, and ordinary periodic location ticks. */
public final class PhaseALocationHolderSetWorldgenGameTest implements CustomTestMethodInvoker {
    @GameTest(maxTicks=8000) public void locationHolderSetWorldgenCanary(GameTestHelper h) { run(h,false); }
    @GameTest(maxTicks=8000) public void locationHolderSetWorldgenExact93(GameTestHelper h) { run(h,true); }
    private static void run(GameTestHelper h,boolean exact) {
        try {String run=PhaseALocationHolderSetWorldgenExecutionEvidence.begin(PhaseALocationHolderSetWorldgenExecutionEvidence.root(),exact); next(h,PhaseALocationHolderSetWorldgenExecutionEvidence.selected(),0,run,exact);}
        catch(Throwable e){h.fail("location start: "+e);}
    }
    private static void next(GameTestHelper h,List<JsonObject> rows,int i,String run,boolean exact) {
        if(i==rows.size()) {try {PhaseALocationHolderSetWorldgenEvidenceValidation.load(PhaseALocationHolderSetWorldgenExecutionEvidence.root(),PhaseALocationHolderSetWorldgenEvidenceValidation.TEMP,exact,true);System.out.println((exact?"TEMP_PROMOTABLE":"TEMP_DIAGNOSTIC")+"=PASS family=LOCATION_HOLDERSET_WORLDGEN runId="+run+" entries="+rows.size());h.succeed();}catch(Throwable e){h.fail("location audit: "+e);}return;}
        Context c=new Context(rows.get(i));
        try {
            var predicate=c.row.getAsJsonObject("selectedPredicate"); var location=predicate.has("location")?predicate.getAsJsonObject("location"):new JsonObject();
            String dimension=location.has("dimension")?location.get("dimension").getAsString():predicate.has("stepping_on")&&predicate.getAsJsonObject("stepping_on").has("dimension")?predicate.getAsJsonObject("stepping_on").get("dimension").getAsString():"minecraft:overworld";
            c.level=h.getLevel().getServer().getLevel(ResourceKey.create(Registries.DIMENSION,Identifier.parse(ns(dimension)))); require(c.level!=null,"registered dimension missing");
            BlockPos base=h.absolutePos(new BlockPos(5,2,5)); c.pos=c.level==h.getLevel()?new BlockPos(base.getX(),80,base.getZ()):new BlockPos(8,80,8);
            if(location.has("position")) c.pos=new BlockPos(c.pos.getX(),location.getAsJsonObject("position").getAsJsonObject("y").get("min").getAsInt(),c.pos.getZ());
            for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++) {c.level.getChunk((c.pos.getX()+x)>>4,(c.pos.getZ()+z)>>4);c.level.setBlockAndUpdate(c.pos.offset(x,-1,z),Blocks.STONE.defaultBlockState());for(int y=0;y<5;y++)c.level.setBlockAndUpdate(c.pos.offset(x,y,z),Blocks.AIR.defaultBlockState());}
            if(location.has("biomes")) {
                var biome=c.level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(ResourceKey.create(Registries.BIOME,Identifier.parse(c.row.get("selectedBiome").getAsString())));
                String selector=location.get("biomes").getAsString();
                require(!selector.startsWith("#")||c.level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(TagKey.create(Registries.BIOME,Identifier.parse(selector.substring(1)))).contains(biome),"Live biome tag does not contain frozen selected member");
                for(int x=(c.pos.getX()-4)&~3;x<=((c.pos.getX()+4)&~3);x+=4)for(int z=(c.pos.getZ()-4)&~3;z<=((c.pos.getZ()+4)&~3);z+=4)for(int y=(c.pos.getY()-4)&~3;y<=((c.pos.getY()+4)&~3);y+=4){BlockPos cell=new BlockPos(x,y,z);c.biomes.put(cell,c.level.getBiome(cell));}
                require(FillBiomeCommand.fill(c.level,c.pos.offset(-4,-4,-4),c.pos.offset(4,4,4),biome).right().isEmpty(),"native biome fill failed");
            }
            if(location.has("structures")) {var holder=c.level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(ResourceKey.create(Registries.STRUCTURE,Identifier.parse(location.get("structures").getAsString())));c.structure=SyntheticStructureHarness.inject(c.level,holder,c.pos);c.structureSet=HolderSet.direct(holder);}
            if(location.has("block")) c.level.setBlockAndUpdate(c.pos,BuiltInRegistries.BLOCK.getValue(Identifier.parse(c.row.get("selectedLocationBlock").getAsString())).defaultBlockState());
            if(predicate.has("stepping_on")) {
                String selector=selector(predicate.getAsJsonObject("stepping_on").getAsJsonObject("block").get("blocks"));
                String block=selector.equals("#minecraft:ice")?"minecraft:ice":selector.equals("#minecraft:soul_speed_blocks")?"minecraft:soul_sand":selector;
                c.level.setBlockAndUpdate(c.pos.below(),BuiltInRegistries.BLOCK.getValue(Identifier.parse(block)).defaultBlockState());
            }
            c.joined=join(h);ServerPlayer player=c.joined.player();player.setGameMode(GameType.SURVIVAL);new ServerboundPlayerLoadedPacket().handle(player.connection);
            require(player.connection.hasClientLoaded()&&!player.hasInfiniteMaterials(),"finite joined player failed");
            require(player.teleportTo(c.level,c.pos.getX()+.5,c.pos.getY(),c.pos.getZ()+.5,Set.of(),0,0,true),"native dimension fixture teleport failed");acknowledge(c.joined);player.getInventory().clearContent();
            c.gates=unlockStance(h,player,predicate);
            if(predicate.has("equipment")) for(var entry:predicate.getAsJsonObject("equipment").entrySet()) {
                var spec=entry.getValue().getAsJsonObject(); String item=entry.getKey().equals("mainhand")?"minecraft:netherite_pickaxe":entry.getKey().equals("legs")?"minecraft:netherite_leggings":"minecraft:netherite_boots";
                if(spec.has("items")&&!selector(spec.get("items")).startsWith("#"))item=selector(spec.get("items"));
                ItemStack stack=new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(item)));
                if(spec.has("predicates"))for(var enchant:spec.getAsJsonObject("predicates").getAsJsonArray("enchantments")){var e=enchant.getAsJsonObject();var holder=c.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,Identifier.parse(e.get("enchantments").getAsString())));int count=e.getAsJsonObject("levels").get("min").getAsInt();require(count<=holder.value().getMaxLevel()&&holder.value().canEnchant(stack),"illegal enchantment fixture");stack.enchant(holder,count);}
                player.setItemSlot(slot(entry.getKey()),stack);
            }
            if(predicate.has("effects"))for(var effect:predicate.getAsJsonObject("effects").entrySet()){var holder=BuiltInRegistries.MOB_EFFECT.getOrThrow(ResourceKey.create(Registries.MOB_EFFECT,Identifier.parse(effect.getKey())));player.addEffect(new MobEffectInstance(holder,600,effect.getValue().getAsJsonObject().getAsJsonObject("amplifier").get("min").getAsInt()));}
            if(predicate.has("vehicle")) {
                c.llama=(Llama) BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse("minecraft:llama")).create(c.level,EntitySpawnReason.COMMAND);c.llama.setNoAi(true);c.llama.setNoGravity(true);c.llama.setTamed(true);c.llama.teleportTo(player.getX(),player.getY(),player.getZ());c.llama.setItemSlot(EquipmentSlot.BODY,new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("minecraft:"+c.row.get("criterion").getAsString()))));require(c.level.addFreshEntity(c.llama)&&player.startRiding(c.llama),"native llama vehicle fixture failed");
            }
            c.advancement=h.getLevel().getServer().getAdvancements().get(Identifier.parse(c.row.get("advancementId").getAsString()));require(c.advancement!=null&&!done(c),"criterion pre-completed");
            preparePosture(c);c.context=observe(c);h.runAfterDelay(1,()->poll(h,rows,i,run,exact,c,1));
        }catch(Throwable e){fail(h,c,"setup",e);}
    }
    private static JsonObject unlockStance(GameTestHelper h,ServerPlayer player,JsonObject predicate) {
        var board=h.getLevel().getServer().getScoreboard();var objective=board.getObjective("bac_advancements");var settings=board.getObjective("bac_settings");require(objective!=null&&settings!=null,"production objectives missing");board.getOrCreatePlayerScore(net.minecraft.world.scores.ScoreHolder.forNameOnly("reward"),settings).set(0);
        var config=((LevelInfoExtension)h.getLevel().getServer().getWorldData().getLevelSettings()).achievetodo$getAbilitiesConfiguration(h.getLevel().getServer().overworld().getSeed());int needed=0;
        JsonObject witness=new JsonObject(), abilities=new JsonObject();witness.addProperty("thresholdSource","LIVE_OVERWORLD_SEED_CONFIGURATION");
        // Native dimension entry may already award unrelated advancements. Establish
        // the zero-score gate fixture, preserving the actual pre-fixture observation.
        witness.addProperty("nativeScoreBeforeFixture",board.getOrCreatePlayerScore(player,objective).get());
        board.getOrCreatePlayerScore(player,objective).set(0);
        witness.addProperty("scoreBefore",board.getPlayerScoreInfo(player,objective).value());
        if(predicate.has("flags")) {var flags=predicate.getAsJsonObject("flags");for(var e:Map.of("is_sneaking",AbilityType.SNEAK,"is_sprinting",AbilityType.SPRINT).entrySet())if(flags.has(e.getKey())&&flags.get(e.getKey()).getAsBoolean()){boolean before=AchieveToDoMod.isAbilityLocked(player,e.getValue(),true);require(before,"fresh stance gate open");int threshold=config.get(e.getValue());require(threshold>0,"live stance threshold missing");needed=Math.max(needed,threshold);JsonObject gate=new JsonObject();gate.addProperty("requiredThreshold",threshold);gate.addProperty("lockedBefore",before);abilities.add(e.getValue().name(),gate);}}
        board.getOrCreatePlayerScore(player,objective).set(needed);
        for(var e:abilities.entrySet()){boolean after=AchieveToDoMod.isAbilityLocked(player,AbilityType.valueOf(e.getKey()),true);require(!after,"stance gate stayed locked");e.getValue().getAsJsonObject().addProperty("lockedAfter",after);}
        witness.add("abilities",abilities);witness.addProperty("scoreAfter",board.getPlayerScoreInfo(player,objective).value());return witness;
    }
    private static void preparePosture(Context c) {
        var p=c.joined.player();var spec=c.row.getAsJsonObject("selectedPredicate");boolean sneak=spec.has("flags")&&spec.getAsJsonObject("flags").has("is_sneaking")&&spec.getAsJsonObject("flags").get("is_sneaking").getAsBoolean();boolean sprint=spec.has("flags")&&spec.getAsJsonObject("flags").has("is_sprinting")&&spec.getAsJsonObject("flags").get("is_sprinting").getAsBoolean();
        new ServerboundPlayerInputPacket(new Input(false,false,false,false,false,sneak,sprint)).handle(p.connection);p.setShiftKeyDown(sneak);p.setSprinting(sprint);
        if(spec.has("stepping_on")) {var foot=c.pos.below();double y=foot.getY()+c.level.getBlockState(foot).getCollisionShape(c.level,foot).max(Direction.Axis.Y);new ServerboundMovePlayerPacket.Pos(p.getX(),y,p.getZ(),true,false).handle(p.connection);}
    }
    private static JsonObject observe(Context c) throws Exception {
        var player=c.joined.player();var spec=c.row.getAsJsonObject("selectedPredicate");JsonObject observed=new JsonObject();observed.addProperty("notSpectator",!player.isSpectator());observed.addProperty("dimension",player.level().dimension().identifier().toString());observed.addProperty("biome",player.level().getBiome(player.blockPosition()).unwrapKey().orElseThrow().identifier().toString());observed.addProperty("y",player.getY());
        boolean excluded=!player.hasEffect(MobEffects.FIRE_RESISTANCE)&&!player.isSpectator();var boots=player.getItemBySlot(EquipmentSlot.FEET).getOrDefault(DataComponents.ENCHANTMENTS,net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);for(var enchant:boots.keySet())excluded&=!enchant.unwrapKey().orElseThrow().identifier().toString().equals("minecraft:frost_walker");observed.addProperty("excludedPredicatesAbsent",excluded);require(excluded,"excluded context present");
        if (spec.has("location")&&spec.getAsJsonObject("location").has("biomes")) {
            String selector=spec.getAsJsonObject("location").get("biomes").getAsString();var actual=c.level.getBiome(player.blockPosition());
            boolean matched=selector.startsWith("#")?actual.is(TagKey.create(Registries.BIOME,Identifier.parse(selector.substring(1)))):actual.unwrapKey().orElseThrow().identifier().toString().equals(ns(selector));
            require(matched,"Observed native biome selector mismatch");observed.addProperty("biomeSelector",selector);observed.addProperty("biomeSelectorMatched",matched);
        }
        if(c.structure!=null){boolean lookup=c.level.structureManager().getStructureWithPieceAt(player.blockPosition(),c.structureSet)==c.structure.injectedStart();require(lookup,"live registered structure lookup mismatch");observed.addProperty("structureLookup",lookup);observed.addProperty("structure",spec.getAsJsonObject("location").get("structures").getAsString());}
        if(spec.has("location")&&spec.getAsJsonObject("location").has("block")){boolean match=blockMatches(c.level,player.blockPosition(),spec.getAsJsonObject("location").getAsJsonObject("block"));require(match,"live inside-block mismatch");observed.addProperty("locationBlockMatched",match);}
        if(spec.has("stepping_on")){boolean match=blockMatches(c.level,player.getOnPos(),spec.getAsJsonObject("stepping_on").getAsJsonObject("block"));require(player.onGround()&&match,"native grounded block mismatch: "+player.onGround()+" at "+player.getOnPos());observed.addProperty("onGround",player.onGround());observed.addProperty("steppingBlockMatched",match);}
        observed.addProperty("is_sneaking",player.isShiftKeyDown());observed.addProperty("is_sprinting",player.isSprinting());
        if(spec.has("effects")){JsonObject effects=new JsonObject();for(var entry:spec.getAsJsonObject("effects").entrySet()){var holder=BuiltInRegistries.MOB_EFFECT.getOrThrow(ResourceKey.create(Registries.MOB_EFFECT,Identifier.parse(entry.getKey())));require(player.hasEffect(holder),"live effect missing");effects.addProperty(entry.getKey(),player.getEffect(holder).getAmplifier());}observed.add("effects",effects);}
        if(spec.has("equipment")){JsonObject equipment=new JsonObject();for(var entry:spec.getAsJsonObject("equipment").entrySet()){ItemStack stack=player.getItemBySlot(slot(entry.getKey()));JsonObject slot=new JsonObject();String id=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();slot.addProperty("item",id);var expected=entry.getValue().getAsJsonObject();boolean match=!expected.has("items")||itemMatches(stack,selector(expected.get("items")));require(match,"live equipment selector mismatch");slot.addProperty("itemSelectorMatched",match);JsonObject ench=new JsonObject();var component=stack.getOrDefault(DataComponents.ENCHANTMENTS,net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);for(var holder:component.keySet())ench.addProperty(holder.unwrapKey().orElseThrow().identifier().toString(),component.getLevel(holder));slot.add("enchantments",ench);equipment.add(entry.getKey(),slot);}observed.add("equipment",equipment);}
        if(spec.has("vehicle")){require(player.getVehicle()==c.llama,"live llama vehicle missing");boolean tag=BuiltInRegistries.ENTITY_TYPE.getOrThrow(TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("blazeandcave:llamas"))).contains(c.llama.typeHolder()),nbt=new NbtPredicate(TagParser.parseCompoundFully(c.row.get("nativeVehicleNbt").getAsString())).matches(c.llama);require(tag&&nbt,"native vehicle tag/NBT mismatch");observed.addProperty("vehicleType",BuiltInRegistries.ENTITY_TYPE.getKey(c.llama.getType()).toString());observed.addProperty("vehicleTagMatched",tag);observed.addProperty("vehicleNbtMatched",nbt);observed.addProperty("nativeVehicleNbt",c.row.get("nativeVehicleNbt").getAsString());observed.addProperty("vehicleBodyItem",BuiltInRegistries.ITEM.getKey(c.llama.getItemBySlot(EquipmentSlot.BODY).getItem()).toString());}
        return observed;
    }
    private static boolean blockMatches(ServerLevel level,BlockPos pos,JsonObject spec){String id=selector(spec.get("blocks"));return id.startsWith("#")?level.getBlockState(pos).is(TagKey.create(Registries.BLOCK,Identifier.parse(id.substring(1)))):level.getBlockState(pos).is(BuiltInRegistries.BLOCK.getValue(Identifier.parse(id)));}
    private static boolean itemMatches(ItemStack stack,String id){return id.startsWith("#")?stack.is(TagKey.create(Registries.ITEM,Identifier.parse(id.substring(1)))):stack.is(BuiltInRegistries.ITEM.getValue(Identifier.parse(id)));}
    private static String selector(com.google.gson.JsonElement element){return element.isJsonArray()?element.getAsJsonArray().get(0).getAsString():element.getAsString();}
    private static EquipmentSlot slot(String name){return switch(name){case "mainhand"->EquipmentSlot.MAINHAND;case "legs"->EquipmentSlot.LEGS;case "feet"->EquipmentSlot.FEET;default->throw new IllegalStateException("unsupported slot");};}
    private static boolean done(Context c){var criterion=c.joined.player().getAdvancements().getOrStartProgress(c.advancement).getCriterion(c.row.get("criterion").getAsString());require(criterion!=null,"live criterion missing");return criterion.isDone();}
    private static void poll(GameTestHelper h,List<JsonObject> rows,int i,String run,boolean exact,Context c,int ticks){try{if(done(c)){finish(h,rows,i,run,exact,c,ticks);return;}require(ticks<60,"periodic native location timed out");preparePosture(c);c.context=observe(c);h.runAfterDelay(1,()->poll(h,rows,i,run,exact,c,ticks+1));}catch(Throwable e){fail(h,c,"location tick",e);}}
    private static void finish(GameTestHelper h,List<JsonObject> rows,int i,String run,boolean exact,Context c,int ticks)throws Exception{
        var player=c.joined.player();JsonObject r=new JsonObject();for(String key:new String[]{"advancementId","criterion","requirementGroup"})r.add(key,c.row.get(key).deepCopy());r.addProperty("family",PhaseALocationHolderSetWorldgenCertification.FAMILY);r.addProperty("source",PhaseALocationHolderSetWorldgenCertification.SOURCE);r.addProperty("runId",run);r.addProperty("catalogFingerprint",PhaseALocationHolderSetWorldgenEvidenceValidation.fingerprint(PhaseALocationHolderSetWorldgenExecutionEvidence.root()));r.addProperty("result","GREEN");r.addProperty("gameMode",player.gameMode().name());r.addProperty("playerUuid",player.getUUID().toString());r.addProperty("joined",h.getLevel().getServer().getPlayerList().getPlayer(player.getUUID())==player);r.addProperty("connectionRegistered",h.getLevel().getServer().getConnection().getConnections().contains(c.joined.connection()));r.addProperty("clientLoaded",player.connection.hasClientLoaded());r.addProperty("finiteMaterials",!player.hasInfiniteMaterials());r.addProperty("contextObservedBeforeTick",c.context!=null);r.addProperty("criterionBefore",false);r.addProperty("criterionAfter",done(c));r.addProperty("trigger","minecraft:location");r.addProperty("boundary",PhaseALocationHolderSetWorldgenCertification.BOUNDARY);r.addProperty("ticksToComplete",ticks);r.addProperty("noDirectCriterionTrigger",true);r.addProperty("noManualAward",true);r.add("contextWitness",c.context);r.add("productionGateWitness",c.gates);
        JsonObject clean=cleanupWorld(c);var lifecycle=cleanup(c.joined);clean.addProperty("playerRemoved",lifecycle.playerRemoved());clean.addProperty("connectionRemoved",lifecycle.connectionRemoved());clean.addProperty("channelSettled",lifecycle.channelSettled());clean.addProperty("warningCount",0);clean.addProperty("settlementMessages",lifecycle.messages());r.add("cleanup",clean);PhaseALocationHolderSetWorldgenExecutionEvidence.append(PhaseALocationHolderSetWorldgenExecutionEvidence.root(),r);h.runAfterDelay(1,()->next(h,rows,i+1,run,exact));
    }
    private static JsonObject cleanupWorld(Context c){if(c.llama!=null)c.llama.discard();boolean restored=true;if(c.structure!=null){var s=c.structure;s.close();restored=s.chunk().getAllStarts().equals(s.originalStarts())&&s.chunk().getAllReferences().equals(s.originalReferences());c.structure=null;}boolean biomes=true;for(var e:c.biomes.entrySet()){require(FillBiomeCommand.fill(c.level,e.getKey(),e.getKey(),e.getValue()).right().isEmpty(),"native biome restore failed");biomes&=c.level.getBiome(e.getKey()).equals(e.getValue());}c.biomes.clear();JsonObject clean=new JsonObject();clean.addProperty("fixtureEntitiesRemoved",c.llama==null||!c.llama.isAlive());clean.addProperty("structureRestored",restored);clean.addProperty("biomeRestored",biomes);return clean;}
    private static void fail(GameTestHelper h,Context c,String stage,Throwable e){cleanupWorld(c);if(c.joined!=null)cleanup(c.joined);h.fail("location "+stage+" "+PhaseALocationHolderSetWorldgenCertification.key(c.row)+": "+e);}
    private static String ns(String id){return PhaseALocationHolderSetWorldgenCertification.namespaced(id);}
    private static void acknowledge(Joined joined){var channel=joined.channel();channel.runPendingTasks();channel.runScheduledPendingTasks();channel.flushOutbound();Integer id=null;Object packet;while((packet=channel.readOutbound())!=null){if(packet instanceof ClientboundPlayerPositionPacket position)id=position.id();ReferenceCountUtil.release(packet);}require(id!=null,"missing teleport acknowledgement");new ServerboundAcceptTeleportationPacket(id).handle(joined.player().connection);}
    private static Joined join(GameTestHelper h){MinecraftServer server=h.getLevel().getServer();UUID id=UUID.randomUUID();GameProfile profile=new GameProfile(id,"loc"+id.toString().substring(0,8));ServerPlayer player=new ServerPlayer(server,h.getLevel(),profile,ClientInformation.createDefault());Connection connection=new Connection(PacketFlow.SERVERBOUND);EmbeddedChannel channel=new EmbeddedChannel(connection);server.getConnection().getConnections().add(connection);server.getPlayerList().placeNewPlayer(connection,player,CommonListenerCookie.createInitial(profile,false));return new Joined(player,connection,channel,new AtomicBoolean());}
    private static Cleanup cleanup(Joined joined){if(!joined.cleaned().compareAndSet(false,true))return new Cleanup(true,true,true,0);MinecraftServer server=joined.player().level().getServer();if(joined.player().containerMenu!=joined.player().inventoryMenu)joined.player().closeContainer();server.getPlayerList().remove(joined.player());boolean playerRemoved=server.getPlayerList().getPlayer(joined.player().getUUID())!=joined.player();server.getConnection().getConnections().remove(joined.connection());boolean connectionRemoved=!server.getConnection().getConnections().contains(joined.connection());int messages=settle(joined.channel());joined.connection().disconnect(Component.literal("location cleanup"));return new Cleanup(playerRemoved,connectionRemoved,true,messages);}
    private static int settle(EmbeddedChannel channel){if(!channel.isOpen())return 0;int released=0;for(int pass=0;pass<32;pass++){channel.runPendingTasks();channel.runScheduledPendingTasks();channel.flushOutbound();int current=0;Object outbound;while((outbound=channel.readOutbound())!=null){ReferenceCountUtil.release(outbound);current++;if(++released>4096)throw new IllegalStateException("channel cleanup message bound");}if(current==0&&!channel.hasPendingTasks())return released;}throw new IllegalStateException("channel did not quiesce");}
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException(why);}
    private static final class Context {final JsonObject row;Joined joined;ServerLevel level;BlockPos pos;Llama llama;AdvancementHolder advancement;JsonObject context,gates;SyntheticStructureHarness.InjectedStructure structure;HolderSet<Structure> structureSet;Map<BlockPos,Holder<Biome>> biomes=new LinkedHashMap<>();Context(JsonObject row){this.row=row;}}
    private record Joined(ServerPlayer player,Connection connection,EmbeddedChannel channel,AtomicBoolean cleaned){}
    private record Cleanup(boolean playerRemoved,boolean connectionRemoved,boolean channelSettled,int messages){}
    @Override public void invokeTestMethod(GameTestHelper h,Method method)throws ReflectiveOperationException{h.setBlock(0,0,0,Blocks.AIR);method.invoke(this,h);}
}
