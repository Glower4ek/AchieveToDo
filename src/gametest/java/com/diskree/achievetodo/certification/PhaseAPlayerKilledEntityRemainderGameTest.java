package com.diskree.achievetodo.certification;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.server.commands.FillBiomeCommand;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.injection.extension.main.LevelInfoExtension;
import java.util.*;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/** Joined SURVIVAL attacks and charged bow releases observe native victim/source contexts. */
public final class PhaseAPlayerKilledEntityRemainderGameTest implements CustomTestMethodInvoker {
    @GameTest(maxTicks = 3600)
    public void playerKilledEntityRemainderCanary(GameTestHelper h) { run(h, false); }
    @GameTest(maxTicks = 3600)
    public void playerKilledEntityRemainderExact43(GameTestHelper h) { run(h, true); }
    private static void run(GameTestHelper h, boolean exact) {
        try { String run = PhaseAPlayerKilledEntityRemainderExecutionEvidence.begin(PhaseAPlayerKilledEntityRemainderExecutionEvidence.root(), exact); runCase(h, PhaseAPlayerKilledEntityRemainderExecutionEvidence.selected(), 0, run, exact); }
        catch (Throwable failure) { h.fail("kill remainder start failed: " + describe(failure)); }
    }
    private static void runCase(GameTestHelper h, List<JsonObject> cases, int index, String run, boolean exact) {
        if (index == cases.size()) {
            try {
                var artifact = PhaseAPlayerKilledEntityRemainderEvidenceValidation.load(PhaseAPlayerKilledEntityRemainderExecutionEvidence.root(), PhaseAPlayerKilledEntityRemainderEvidenceValidation.TEMP, exact, true);
                require(artifact.get("runId").getAsString().equals(run), "run changed");
                System.out.println((exact ? "TEMP_PROMOTABLE" : "TEMP_DIAGNOSTIC") + "=PASS family=PLAYER_KILLED_ENTITY_REMAINDER runId=" + run + " entries=" + cases.size()); h.succeed();
            } catch (Throwable failure) { h.fail("kill remainder exact audit failed: " + describe(failure)); }
            return;
        }
        Context c = new Context(cases.get(index));
        try {
            BlockPos base = h.absolutePos(new BlockPos(5, 2, 5)); c.pos = new BlockPos(base.getX(), 80, base.getZ());
            for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
                h.getLevel().setBlockAndUpdate(c.pos.offset(x, -1, z), Blocks.STONE.defaultBlockState());
                h.getLevel().setBlockAndUpdate(c.pos.offset(x, 6, z), Blocks.STONE.defaultBlockState());
                for (int y = 0; y < 6; y++) h.getLevel().setBlockAndUpdate(c.pos.offset(x, y, z), Blocks.AIR.defaultBlockState());
            }
            var predicate = c.row.getAsJsonObject("entityPredicate");
            if (predicate.has("location")) {
                JsonObject location = predicate.getAsJsonObject("location");
                if (location.has("biomes")) {
                    var holder = h.getLevel().registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(ResourceKey.create(Registries.BIOME, Identifier.parse(ns(location.get("biomes").getAsString()))));
                    for (int x = (c.pos.getX()-4)&~3; x <= ((c.pos.getX()+4)&~3); x+=4) for (int z = (c.pos.getZ()-4)&~3; z <= ((c.pos.getZ()+4)&~3); z+=4) for (int y=76; y<=84; y+=4) {
                        BlockPos cell = new BlockPos(x,y,z); c.biomes.put(cell, h.getLevel().getBiome(cell));
                    }
                    require(FillBiomeCommand.fill(h.getLevel(), c.pos.offset(-4,-4,-4), c.pos.offset(4,4,4), holder).right().isEmpty(), "native biome fill failed");
                }
                if (location.has("structures")) {
                    var structure = h.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(ResourceKey.create(Registries.STRUCTURE, Identifier.parse(ns(location.get("structures").getAsString()))));
                    c.structure = SyntheticStructureHarness.inject(h.getLevel(), structure, c.pos); c.structureSet = HolderSet.direct(structure);
                }
            }
            c.victim = spawn(h, c.row.get("selectedEntity").getAsString(), c.pos); c.victim.setHealth(1.0F);
            if (predicate.has("passenger")) { Mob passenger = spawn(h, "minecraft:skeleton", c.pos); c.extra.add(passenger); require(passenger.startRiding(c.victim), "native passenger fixture failed"); }
            if (predicate.has("effects")) c.victim.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 600, 0));
            if (predicate.has("equipment")) c.victim.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
            if (predicate.has("stepping_on")) {
                for (BlockPos foot : List.of(c.pos.below(), c.pos.offset(0,-1,2))) {
                    h.getLevel().setBlockAndUpdate(foot.below(), Blocks.CLAY.defaultBlockState()); h.getLevel().setBlockAndUpdate(foot, Blocks.BIG_DRIPLEAF.defaultBlockState());
                    h.getLevel().setBlockAndUpdate(foot.east(), Blocks.REDSTONE_BLOCK.defaultBlockState());
                }
                c.victim.setNoGravity(false);
            }
            c.joined = join(h); var player = c.joined.player(); player.setGameMode(GameType.SURVIVAL);
            new ServerboundPlayerLoadedPacket().handle(player.connection); require(player.connection.hasClientLoaded() && !player.hasInfiniteMaterials(), "finite joined lifecycle failed");
            player.teleportTo(c.pos.getX()+.5D, c.pos.getY(), c.pos.getZ()+2.5D); acknowledge(c.joined); player.getInventory().clearContent();
            c.tool = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(c.row.get("selectedTool").getAsString()))); player.setItemInHand(InteractionHand.MAIN_HAND, c.tool);
            if (c.row.get("action").getAsString().equals("ARROW")) player.getInventory().setItem(1, new ItemStack(Items.ARROW));
            var frozen = c.row.getAsJsonObject("frozenConditions");
            if (frozen.has("player") && frozen.getAsJsonObject("player").has("vehicle")) { Mob camel = spawn(h, "minecraft:camel", c.pos.offset(0,0,2)); c.extra.add(camel); require(player.startRiding(camel), "native camel mounting failed"); }
            c.advancement = h.getLevel().getServer().getAdvancements().get(Identifier.parse(c.row.get("advancementId").getAsString())); require(c.advancement != null && !done(c), "criterion missing or pre-complete");
            c.gates = unlock(h, player, c.row.get("action").getAsString().equals("ARROW"));
            h.runAfterDelay(2, () -> attack(h, cases, index, run, exact, c, 2));
        } catch (Throwable failure) { fail(h, c, "setup", failure); }
    }
    private static Mob spawn(GameTestHelper h, String id, BlockPos pos) {
        var entity = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(id)).create(h.getLevel(), EntitySpawnReason.COMMAND);
        require(entity instanceof Mob, "Expected live Mob for " + id); Mob mob = (Mob) entity; mob.setNoAi(true); mob.setNoGravity(true);
        mob.teleportTo(pos.getX()+.5D, pos.getY(), pos.getZ()+.5D); require(h.getLevel().addFreshEntity(mob), "native entity spawn failed"); return mob;
    }
    private static JsonObject unlock(GameTestHelper h, ServerPlayer player, boolean arrow) {
        var server = h.getLevel().getServer(); var board = server.getScoreboard(); var score = board.getObjective("bac_advancements"); var settings = board.getObjective("bac_settings"); require(score != null && settings != null, "production objectives missing");
        board.getOrCreatePlayerScore(net.minecraft.world.scores.ScoreHolder.forNameOnly("reward"), settings).set(0);
        var config = ((LevelInfoExtension) server.getWorldData().getLevelSettings()).achievetodo$getAbilitiesConfiguration(server.overworld().getSeed());
        AbilityType ability = arrow ? AbilityType.SHOOT_BOW : AbilityType.USE_WOODEN_TOOLS; Integer threshold = config.get(ability); require(threshold != null && threshold > 0, "live combat threshold missing");
        boolean before = AchieveToDoMod.isAbilityLocked(player, ability, true); require(before, "fresh combat gate already open");
        board.getOrCreatePlayerScore(player, score).set(threshold); boolean after = AchieveToDoMod.isAbilityLocked(player, ability, true); require(!after, "combat gate stayed locked");
        JsonObject witness = new JsonObject(); witness.addProperty("ability", ability.name()); witness.addProperty("thresholdSource", "LIVE_OVERWORLD_SEED_CONFIGURATION"); witness.addProperty("requiredThreshold", threshold); witness.addProperty("lockedBefore", before); witness.addProperty("lockedAfter", after); witness.addProperty("scoreAfter", board.getPlayerScoreInfo(player, score).value()); return witness;
    }
    private static void attack(GameTestHelper h, List<JsonObject> cases, int index, String run, boolean exact, Context c, int ticks) {
        try {
            require(c.victim.isAlive() && !done(c), "victim/criterion changed before attack"); c.before = false;
            var player = c.joined.player();
            if (c.row.getAsJsonObject("entityPredicate").has("stepping_on")) {
                var foot = c.pos.offset(0, -1, 2);
                double surface = foot.getY() + h.getLevel().getBlockState(foot).getCollisionShape(h.getLevel(), foot).max(Direction.Axis.Y);
                new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(player.getX(), surface, player.getZ(), true, false).handle(player.connection);
                c.victim.move(net.minecraft.world.entity.MoverType.SELF, new net.minecraft.world.phys.Vec3(0, -0.25D, 0));
                if (!c.victim.onGround()) {
                    require(ticks < 40, "native victim did not settle on dripleaf: y=" + c.victim.getY() + " support=" + c.victim.getOnPos());
                    h.runAfterDelay(1, () -> attack(h, cases, index, run, exact, c, ticks + 1)); return;
                }
            }
            if (!c.row.get("action").getAsString().equals("ARROW") && player.getAttackStrengthScale(0.5F) < 0.95F) {
                require(ticks < 40, "native melee cooldown did not settle");
                h.runAfterDelay(1, () -> attack(h, cases, index, run, exact, c, ticks + 1)); return;
            }
            c.context = context(h,c);
            if (c.row.get("action").getAsString().equals("ARROW")) {
                double dx=c.victim.getX()-player.getX(), dz=c.victim.getZ()-player.getZ(), dy=c.victim.getY()+c.victim.getBbHeight()*.5D-player.getEyeY();
                float yaw=(float)(Math.toDegrees(Math.atan2(dz,dx))-90), pitch=(float)-Math.toDegrees(Math.atan2(dy,Math.sqrt(dx*dx+dz*dz)));
                player.setYRot(yaw); player.setXRot(pitch);
                new ServerboundUseItemPacket(InteractionHand.MAIN_HAND,0,yaw,pitch).handle(player.connection);
                require(player.isUsingItem(), "bow use packet rejected");
                h.runAfterDelay(20, () -> release(h,cases,index,run,exact,c,ticks+20));
            } else {
                new ServerboundAttackPacket(c.victim.getId()).handle(player.connection);
                h.runAfterDelay(1, () -> poll(h,cases,index,run,exact,c,ticks+1));
            }
        } catch (Throwable failure) { fail(h,c,"attack",failure); }
    }
    private static void release(GameTestHelper h, List<JsonObject> cases, int index, String run, boolean exact, Context c, int ticks) {
        try { require(c.victim.isAlive() && !done(c), "victim changed before native release"); new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM,BlockPos.ZERO,Direction.DOWN,1).handle(c.joined.player().connection); h.runAfterDelay(1,()->poll(h,cases,index,run,exact,c,ticks+1)); }
        catch (Throwable failure) { fail(h,c,"release",failure); }
    }
    private static JsonObject context(GameTestHelper h, Context c) {
        var player=c.joined.player(); var predicate=c.row.getAsJsonObject("entityPredicate"); JsonObject witness=new JsonObject();
        String selector=predicate.has("type")?predicate.get("type").getAsString():c.row.get("selectedEntity").getAsString(); boolean type=matches(c.victim,selector); require(type,"live victim selector mismatch"); witness.addProperty("entityTypeMatched",type);
        boolean tool=true; var frozen=c.row.getAsJsonObject("frozenConditions");
        JsonObject hand=null;
        if (frozen.has("player") && frozen.getAsJsonObject("player").has("equipment")) hand=frozen.getAsJsonObject("player").getAsJsonObject("equipment").getAsJsonObject("mainhand");
        var blow=c.row.getAsJsonObject("killingBlow"); if (blow.has("source_entity") && blow.getAsJsonObject("source_entity").has("equipment")) hand=blow.getAsJsonObject("source_entity").getAsJsonObject("equipment").getAsJsonObject("mainhand");
        if(hand!=null) {String tag=hand.get("items").getAsString(); require(tag.startsWith("#"),"unexpected source item constraint"); tool=player.getMainHandItem().is(TagKey.create(Registries.ITEM,Identifier.parse(tag.substring(1))));}
        require(tool,"live tool tag mismatch"); witness.addProperty("toolTagMatched",tool);
        if(predicate.has("location")) {
            var location=predicate.getAsJsonObject("location"); witness.addProperty("victimY",c.victim.blockPosition().getY()); witness.addProperty("dimension",h.getLevel().dimension().identifier().toString()); witness.addProperty("biome",h.getLevel().getBiome(c.victim.blockPosition()).unwrapKey().orElseThrow().identifier().toString());
            if(location.has("structures")) {boolean lookup=h.getLevel().structureManager().getStructureWithPieceAt(c.victim.blockPosition(),c.structureSet)==c.structure.injectedStart(); require(lookup,"live structure lookup mismatch"); witness.addProperty("structureLookup",lookup); witness.addProperty("structure",ns(location.get("structures").getAsString()));}
            if(location.has("biomes")) require(witness.get("biome").getAsString().equals(ns(location.get("biomes").getAsString())),"live biome mismatch");
            if(location.has("position")) require(c.victim.getY()>=location.getAsJsonObject("position").getAsJsonObject("y").get("min").getAsDouble(),"live Y bound mismatch");
        }
        if(predicate.has("distance")) {double distance=player.distanceTo(c.victim); require(distance<=5,"native relative distance too large"); witness.addProperty("distance",distance);}
        if(predicate.has("passenger")) {boolean passenger=c.victim.getPassengers().stream().anyMatch(entity->matches(entity,predicate.getAsJsonObject("passenger").get("type").getAsString())); require(passenger,"live passenger tag mismatch"); witness.addProperty("passengerTagMatched",passenger);}
        if(predicate.has("effects")) {boolean invisible=c.victim.hasEffect(MobEffects.INVISIBILITY); require(invisible,"live invisibility missing"); witness.addProperty("invisibility",invisible);}
        if(predicate.has("equipment")) {require(c.victim.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA),"live elytra missing"); witness.addProperty("victimChest","minecraft:elytra");}
        if(predicate.has("stepping_on")) {boolean victim=c.victim.onGround()&&h.getLevel().getBlockState(c.victim.getOnPos()).is(Blocks.BIG_DRIPLEAF), source=player.onGround()&&h.getLevel().getBlockState(player.getOnPos()).is(Blocks.BIG_DRIPLEAF); require(victim&&source,"live grounded dripleaf context missing"); witness.addProperty("victimOnDripleaf",victim); witness.addProperty("playerOnDripleaf",source);}
        if(frozen.has("player")&&frozen.getAsJsonObject("player").has("vehicle")) {require(player.getVehicle()!=null&&matches(player.getVehicle(),"minecraft:camel"),"live camel source missing"); witness.addProperty("playerVehicle",BuiltInRegistries.ENTITY_TYPE.getKey(player.getVehicle().getType()).toString());}
        return witness;
    }
    private static boolean matches(Entity entity,String selector) {return selector.startsWith("#")?BuiltInRegistries.ENTITY_TYPE.getOrThrow(TagKey.create(Registries.ENTITY_TYPE,Identifier.parse(selector.substring(1)))).contains(entity.typeHolder()):BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString().equals(ns(selector));}
    private static boolean done(Context c) {var criterion=c.joined.player().getAdvancements().getOrStartProgress(c.advancement).getCriterion(c.row.get("criterion").getAsString());require(criterion!=null,"live kill criterion missing");return criterion.isDone();}
    private static void poll(GameTestHelper h,List<JsonObject> cases,int index,String run,boolean exact,Context c,int ticks) {
        try {if(done(c)&&!c.victim.isAlive()) {finish(h,cases,index,run,exact,c,ticks);return;}if(ticks>=120)throw new IllegalStateException("native kill timed out: health="+c.victim.getHealth()+" source="+c.victim.getLastDamageSource());h.runAfterDelay(1,()->poll(h,cases,index,run,exact,c,ticks+1));}
        catch(Throwable failure){fail(h,c,"kill",failure);}
    }
    private static void finish(GameTestHelper h,List<JsonObject> cases,int index,String run,boolean exact,Context c,int ticks) {
        try {
            var player=c.joined.player();var damage=c.victim.getLastDamageSource();require(damage!=null&&damage.getEntity()==player&&c.victim.getKillCredit()==player,"native joined-player kill credit missing");
            JsonObject damageWitness=new JsonObject();damageWitness.addProperty("damageType",damage.typeHolder().unwrapKey().orElseThrow().identifier().toString());damageWitness.addProperty("sourceIsJoinedPlayer",damage.getEntity()==player);damageWitness.addProperty("directIsJoinedPlayer",damage.getDirectEntity()==player);
            boolean arrow=c.row.get("action").getAsString().equals("ARROW");
            if(arrow){boolean projectile=damage.is(TagKey.create(Registries.DAMAGE_TYPE,Identifier.parse("minecraft:is_projectile"))), tag=damage.getDirectEntity()!=null&&matches(damage.getDirectEntity(),"#blazeandcave:arrows");require(projectile&&tag&&player.getInventory().getItem(1).isEmpty(),"native finite arrow source missing");damageWitness.addProperty("isProjectile",projectile);damageWitness.addProperty("directArrowTagMatched",tag);damageWitness.addProperty("finiteArrowsConsumed",1);}
            else require(damageWitness.get("damageType").getAsString().equals("minecraft:player_attack")&&damage.getDirectEntity()==player,"native melee source missing");
            JsonObject receipt=new JsonObject();for(String field:new String[]{"advancementId","criterion","requirementGroup","selectedEntity","selectedTool","action"})receipt.add(field,c.row.get(field).deepCopy());
            receipt.addProperty("family",PhaseAPlayerKilledEntityRemainderCertification.FAMILY);receipt.addProperty("source",PhaseAPlayerKilledEntityRemainderCertification.SOURCE);receipt.addProperty("runId",run);receipt.addProperty("catalogFingerprint",PhaseAPlayerKilledEntityRemainderEvidenceValidation.fingerprint(PhaseAPlayerKilledEntityRemainderExecutionEvidence.root()));receipt.addProperty("result","GREEN");receipt.addProperty("gameMode",player.gameMode().name());receipt.addProperty("playerUuid",player.getUUID().toString());
            receipt.addProperty("joined",h.getLevel().getServer().getPlayerList().getPlayer(player.getUUID())==player);receipt.addProperty("connectionRegistered",h.getLevel().getServer().getConnection().getConnections().contains(c.joined.connection()));receipt.addProperty("clientLoaded",player.connection.hasClientLoaded());receipt.addProperty("finiteMaterials",!player.hasInfiniteMaterials());receipt.addProperty("victimAliveBefore",true);receipt.addProperty("victimKilledByPlayer",c.victim.getKillCredit()==player&&!c.victim.isAlive());receipt.addProperty("contextObservedBeforeAttack",c.context!=null);receipt.addProperty("damageSourceObserved",damage!=null);
            receipt.addProperty("trigger","minecraft:player_killed_entity");receipt.addProperty("boundary",arrow?PhaseAPlayerKilledEntityRemainderCertification.ARROW:PhaseAPlayerKilledEntityRemainderCertification.MELEE);receipt.addProperty("criterionBefore",c.before);receipt.addProperty("criterionAfter",done(c));receipt.addProperty("ticksToComplete",ticks);receipt.addProperty("noDirectCriterionTrigger",true);receipt.addProperty("noManualAward",true);receipt.add("contextWitness",c.context);receipt.add("damageWitness",damageWitness);receipt.add("productionGateWitness",c.gates);
            JsonObject clean=cleanupWorld(h,c);var lifecycle=cleanup(c.joined);clean.addProperty("playerRemoved",lifecycle.playerRemoved());clean.addProperty("connectionRemoved",lifecycle.connectionRemoved());clean.addProperty("channelSettled",lifecycle.channelSettled());clean.addProperty("warningCount",0);clean.addProperty("settlementMessages",lifecycle.messages());receipt.add("cleanup",clean);
            PhaseAPlayerKilledEntityRemainderExecutionEvidence.append(PhaseAPlayerKilledEntityRemainderExecutionEvidence.root(),receipt);h.runAfterDelay(1,()->runCase(h,cases,index+1,run,exact));
        }catch(Throwable failure){fail(h,c,"receipt",failure);}
    }
    private static JsonObject cleanupWorld(GameTestHelper h,Context c) {
        if(c.victim!=null)c.victim.discard();for(var entity:c.extra)entity.discard();
        if(c.pos!=null)for(var entity:h.getLevel().getEntitiesOfClass(Entity.class,new net.minecraft.world.phys.AABB(c.pos).inflate(10), entity->entity instanceof Projectile||entity instanceof ItemEntity))entity.discard();
        boolean restored=true;if(c.structure!=null){var structure=c.structure;structure.close();restored=structure.chunk().getAllStarts().equals(structure.originalStarts())&&structure.chunk().getAllReferences().equals(structure.originalReferences());c.structure=null;}
        boolean biomes=true;for(var saved:c.biomes.entrySet()){require(FillBiomeCommand.fill(h.getLevel(),saved.getKey(),saved.getKey(),saved.getValue()).right().isEmpty(),"native biome restore failed");biomes&=h.getLevel().getBiome(saved.getKey()).equals(saved.getValue());}c.biomes.clear();
        JsonObject clean=new JsonObject();clean.addProperty("fixtureEntitiesRemoved",(c.victim==null||!c.victim.isAlive())&&c.extra.stream().noneMatch(Entity::isAlive));clean.addProperty("structureRestored",restored);clean.addProperty("biomeRestored",biomes);return clean;
    }
    private static void fail(GameTestHelper h,Context c,String stage,Throwable failure){cleanupWorld(h,c);if(c.joined!=null)cleanup(c.joined);h.fail("kill remainder "+stage+" failed "+PhaseAPlayerKilledEntityRemainderCertification.key(c.row)+": "+describe(failure));}
    private static String ns(String id){return PhaseAPlayerKilledEntityRemainderCertification.namespaced(id);}
    private static void acknowledge(Joined joined){var channel=joined.channel();channel.runPendingTasks();channel.runScheduledPendingTasks();channel.flushOutbound();Integer id=null;Object packet;while((packet=channel.readOutbound())!=null){if(packet instanceof ClientboundPlayerPositionPacket position)id=position.id();ReferenceCountUtil.release(packet);}require(id!=null,"missing teleport acknowledgement");new ServerboundAcceptTeleportationPacket(id).handle(joined.player().connection);}
    private static Joined join(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer(); UUID id = UUID.randomUUID();
        GameProfile profile = new GameProfile(id, "kill" + id.toString().substring(0, 8));
        ServerPlayer player = new ServerPlayer(server, h.getLevel(), profile, ClientInformation.createDefault());
        Connection connection = new Connection(PacketFlow.SERVERBOUND); EmbeddedChannel channel = new EmbeddedChannel(connection);
        server.getConnection().getConnections().add(connection);
        server.getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(profile, false));
        return new Joined(player, connection, channel, new AtomicBoolean());
    }
    private static Cleanup cleanup(Joined joined) {
        if (!joined.cleaned().compareAndSet(false, true)) return new Cleanup(true, true, true, 0);
        MinecraftServer server = joined.player().level().getServer();
        if (joined.player().containerMenu != joined.player().inventoryMenu) joined.player().closeContainer();
        server.getPlayerList().remove(joined.player());
        boolean playerRemoved = server.getPlayerList().getPlayer(joined.player().getUUID()) != joined.player();
        server.getConnection().getConnections().remove(joined.connection());
        boolean connectionRemoved = !server.getConnection().getConnections().contains(joined.connection());
        int messages = settle(joined.channel()); joined.connection().disconnect(Component.literal("kill remainder cleanup"));
        return new Cleanup(playerRemoved, connectionRemoved, true, messages);
    }
    private static int settle(EmbeddedChannel channel) {
        if (!channel.isOpen()) return 0; int released = 0;
        for (int pass = 0; pass < 32; pass++) {
            channel.runPendingTasks(); channel.runScheduledPendingTasks(); channel.flushOutbound();
            int current = 0; Object outbound;
            while ((outbound = channel.readOutbound()) != null) {
                ReferenceCountUtil.release(outbound); current++;
                if (++released > 4096) throw new IllegalStateException("channel cleanup exceeded message bound");
            }
            if (current == 0 && !channel.hasPendingTasks()) return released;
        }
        throw new IllegalStateException("channel cleanup did not quiesce");
    }
    private static String describe(Throwable t) { Throwable c = t; while (c.getCause() != null && c.getMessage() == null) c = c.getCause(); return c.getClass().getSimpleName() + ": " + c.getMessage(); }
    private static void require(boolean yes, String why) { if (!yes) throw new IllegalStateException(why); }
    private static final class Context {
        final JsonObject row; Joined joined; Mob victim; List<Entity> extra=new ArrayList<>(); ItemStack tool; AdvancementHolder advancement; BlockPos pos; boolean before;
        SyntheticStructureHarness.InjectedStructure structure; HolderSet<Structure> structureSet; Map<BlockPos,Holder<Biome>> biomes=new LinkedHashMap<>(); JsonObject context,gates;
        Context(JsonObject row){this.row=row;}
    }
    private record Joined(ServerPlayer player, Connection connection, EmbeddedChannel channel, AtomicBoolean cleaned) { }
    private record Cleanup(boolean playerRemoved, boolean connectionRemoved, boolean channelSettled, int messages) { }
    @Override public void invokeTestMethod(GameTestHelper h, Method method) throws ReflectiveOperationException { h.setBlock(0, 0, 0, Blocks.AIR); method.invoke(this, h); }
}
