package com.diskree.achievetodo.certification;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.*;
import com.diskree.achievetodo.server.AchieveToDoServer;
import com.google.gson.Gson;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
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
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.PiercingWeapon;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.*;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import java.lang.reflect.Field;
import java.util.*;

/** Disposable joined SURVIVAL players; receipts distinguish native helper calls from packet dispatch. */
public final class PhaseDCombatRoutesGameTest {
    private record Joined(ServerPlayer player, Connection connection, EmbeddedChannel channel) {}
    @FunctionalInterface private interface Scenario { void run() throws Exception; }
    private GameTestHelper helper;
    private String scenario;
    private int assertions;
    private final List<Joined> joined = new ArrayList<>();
    private final List<Entity> entities = new ArrayList<>();
    private final List<PlayerTeam> teams = new ArrayList<>();
    private Objective objective;
    private boolean createdObjective;
    private final Map<DisplaySlot,Objective> displays = new EnumMap<>(DisplaySlot.class);
    private AchieveToDoServer owner() { return AchieveToDoMod.getServer(); }
    private static Object field(Object value, Class<?> type, String name) throws Exception {
        Field f=type.getDeclaredField(name);f.setAccessible(true);return f.get(value);
    }
    private void require(String name, boolean passed, Object observed) {
        System.out.println("D_S03_ASSERT "+new Gson().toJson(Map.of("scenario",scenario,"assertion",name,
            "passed",passed,"observed",String.valueOf(observed),"seq",++assertions)));
        if(!passed) throw new AssertionError(name+": "+observed);
    }
    @SuppressWarnings("unchecked") private int threshold(Item item) throws Exception {
        int value=((Map<AbilityType,Integer>)field(owner(),AchieveToDoServer.class,"abilitiesConfiguration"))
            .get(AbilityType.findToolMaterialUsageAbility(item));
        require("live material threshold positive",value>0,item+" "+value);return value;
    }
    @SuppressWarnings("unchecked")
    private void score(Joined fixture, int count) throws Exception {
        helper.getLevel().getServer().getScoreboard().getOrCreatePlayerScore(fixture.player(),objective).set(count);
        drain(fixture);
        int observed=((Map<UUID,Integer>)field(owner(),AchieveToDoServer.class,"obtainedAdvancementsCountByPlayers"))
            .getOrDefault(fixture.player().getUUID(),Integer.MIN_VALUE);
        require("live scoreboard count reconciled",observed==count,observed);
    }
    private Joined join() {
        var server=helper.getLevel().getServer();UUID id=UUID.randomUUID();GameProfile profile=new GameProfile(id,"d3"+id.toString().substring(0,10));
        ServerPlayer player=new ServerPlayer(server,helper.getLevel(),profile,ClientInformation.createDefault());
        Connection connection=new Connection(PacketFlow.SERVERBOUND);EmbeddedChannel channel=new EmbeddedChannel(connection);
        Joined fixture=new Joined(player,connection,channel);joined.add(fixture);
        server.getConnection().getConnections().add(connection);
        server.getPlayerList().placeNewPlayer(connection,player,CommonListenerCookie.createInitial(profile,false));
        player.setGameMode(GameType.SURVIVAL);new ServerboundPlayerLoadedPacket().handle(player.connection);
        BlockPos pos=helper.absolutePos(new BlockPos(1,2,1));helper.getLevel().setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());
        player.teleportTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5);player.setYRot(0);player.setXRot(0);drain(fixture);
        require("joined loaded survival player",server.getPlayerList().getPlayer(id)==player && player.connection.hasClientLoaded()
            && !player.isCreative() && !player.isSpectator(),id);
        return fixture;
    }
    private List<Object> drain(Joined fixture) {
        fixture.channel().runPendingTasks();fixture.channel().runScheduledPendingTasks();fixture.channel().flushOutbound();
        List<Object> packets=new ArrayList<>();Object packet;
        while((packet=fixture.channel().readOutbound())!=null) {
            if(packet instanceof ClientboundPlayerPositionPacket position)
                new ServerboundAcceptTeleportationPacket(position.id()).handle(fixture.player().connection);
            packets.add(packet);ReferenceCountUtil.release(packet);
            if(packets.size()>20000)throw new IllegalStateException("Unbounded fixture packet drain");
        }
        return packets;
    }
    private long overlays(List<Object> packets) {
        return packets.stream().filter(p -> p instanceof ClientboundSystemChatPacket c && c.overlay()).count();
    }
    private Cow target(ServerPlayer player) {
        Cow cow=helper.spawn(EntityTypes.COW,new BlockPos(1,2,3));entities.add(cow);cow.setNoAi(true);
        cow.setPos(player.getX(),player.getY()+.5,player.getZ()+2.5);return cow;
    }
    private void cleanup() throws Exception {
        Throwable failure=null;
        for(Entity entity:entities) {
            entity.discard();require("fixture entity discarded",entity.isRemoved(),entity.getUUID());
        }
        entities.clear();
        for(Joined fixture:new ArrayList<>(joined)) {
            try {
                fixture.player().stopUsingItem();fixture.player().getAdvancements().save();drain(fixture);
                fixture.connection().disconnect(Component.literal("D-S03 fixture complete"));
                fixture.channel().runPendingTasks();fixture.channel().finishAndReleaseAll();
                if(!(Boolean)field(fixture.connection(),Connection.class,"disconnectionHandled"))fixture.connection().handleDisconnection();
                var server=helper.getLevel().getServer();server.getConnection().getConnections().remove(fixture.connection());
                require("fixture player and channel removed",server.getPlayerList().getPlayer(fixture.player().getUUID())==null
                    && !fixture.channel().isOpen() && !server.getConnection().getConnections().contains(fixture.connection()),fixture.player().getUUID());
                server.getScoreboard().resetAllPlayerScores(fixture.player());
                require("fixture score removed",server.getScoreboard().getPlayerScoreInfo(fixture.player(),objective)==null,
                    fixture.player().getScoreboardName());joined.remove(fixture);
            } catch(Throwable t) { if(failure==null)failure=t;else failure.addSuppressed(t); }
        }
        Scoreboard board=helper.getLevel().getServer().getScoreboard();
        for(PlayerTeam team:teams)board.removePlayerTeam(team);teams.clear();
        displays.forEach(board::setDisplayObjective);
        if(createdObjective)board.removeObjective(objective);
        for(var display:displays.entrySet()) require("scoreboard display restored",
            board.getDisplayObjective(display.getKey())==display.getValue(),display.getKey());
        require("no stale combat invocation",((ThreadLocal<?>)field(null,AchieveToDoServer.class,"COMBAT_INVOCATION")).get()==null,scenario);
        if(failure!=null)throw new IllegalStateException("D-S03 cleanup failed",failure);
    }
    private void run(GameTestHelper helper,String name,Scenario body) {
        this.helper=helper;scenario=name;
        var board=helper.getLevel().getServer().getScoreboard();
        for(DisplaySlot slot:DisplaySlot.values())displays.put(slot,board.getDisplayObjective(slot));
        objective=board.getObjective("bac_advancements");
        if(objective==null) { createdObjective=true;objective=board.addObjective("bac_advancements",ObjectiveCriteria.DUMMY,
            Component.literal("D-S03"),ObjectiveCriteria.RenderType.INTEGER,false,null); }
        board.setDisplayObjective(DisplaySlot.SIDEBAR,objective);owner().prepareScoreboard(helper.getLevel().getServer().getScoreboard());
        helper.runAfterDelay(5,() -> {
            try {
                require("server permission ready",!owner().isNotReady(),name);body.run();cleanup();
                System.out.println("D_S03_GAMEPLAY_RECEIPT "+new Gson().toJson(Map.of("scenario",scenario,"status","PASS",
                    "assertions",assertions,"cleanup",true,"scope","native server helper routes; client NOT_RUN")));helper.succeed();
            } catch(Throwable t) {
                t.printStackTrace();try { cleanup(); }catch(Throwable c){t.addSuppressed(c);c.printStackTrace();}
                System.out.println("D_S03_GAMEPLAY_RECEIPT "+new Gson().toJson(Map.of("scenario",scenario,"status","FAIL","error",t.toString())));
                helper.fail(name+": "+t);
            }
        });
    }
    @GameTest(maxTicks=200) public void materialAndActualSlotGates(GameTestHelper helper) {
        run(helper,"materialAndActualSlotGates",() -> {
            Joined fixture=join();ServerPlayer player=fixture.player();
            for(Item item:List.of(Items.WOODEN_SPEAR,Items.STONE_SPEAR,Items.COPPER_SPEAR,Items.IRON_SPEAR,
                Items.GOLDEN_SPEAR,Items.DIAMOND_SPEAR,Items.NETHERITE_SPEAR)) {
                int threshold=threshold(item);
                for(EquipmentSlot slot:List.of(EquipmentSlot.MAINHAND,EquipmentSlot.OFFHAND)) {
                    ItemStack stack=new ItemStack(item);player.setItemSlot(slot,stack);Cow cow=target(player);
                    score(fixture,threshold-1);float health=cow.getHealth();Vec3 velocity=cow.getDeltaMovement();int wear=stack.getDamageValue();
                    boolean hit=player.stabAttack(slot,cow,4,true,true,true);
                    require("denied selected-slot stab no effects",!hit && cow.getHealth()==health && cow.getDeltaMovement().equals(velocity)
                        && stack.getDamageValue()==wear,item+" "+slot);
                    require("one direct stab denial overlay",overlays(drain(fixture))==1,item+" "+slot);
                    score(fixture,threshold);cow.invulnerableTime=0;
                    require("native valid selected-slot positive control",player.stabAttack(slot,cow,4,true,false,false)
                        && cow.getHealth()<health,item+" "+slot);
                    cow.discard();
                }
            }
            ItemStack axe=new ItemStack(Items.COPPER_AXE);player.setItemSlot(EquipmentSlot.MAINHAND,axe);
            Interaction interaction=helper.spawn(EntityTypes.INTERACTION,new BlockPos(1,2,2));entities.add(interaction);
            score(fixture,threshold(Items.COPPER_SPEAR)-1);
            player.attack(interaction);
            require("regular attack denied before mutable skip callback",field(interaction,Interaction.class,"attack")==null,interaction.getUUID());
            require("regular attack exactly one overlay",overlays(drain(fixture))==1,"copper axe");
            score(fixture,threshold(Items.COPPER_SPEAR));player.attack(interaction);
            require("native skip callback positive control",field(interaction,Interaction.class,"attack")!=null,interaction.getUUID());
        });
    }
    @GameTest(maxTicks=200) public void kineticStartAndChangedCount(GameTestHelper helper) {
        run(helper,"kineticStartAndChangedCount",() -> {
            Joined fixture=join();ServerPlayer player=fixture.player();Item item=Items.COPPER_SPEAR;int threshold=threshold(item);
            for(InteractionHand hand:InteractionHand.values()) {
                EquipmentSlot slot=hand==InteractionHand.MAIN_HAND?EquipmentSlot.MAINHAND:EquipmentSlot.OFFHAND;
                ItemStack stack=new ItemStack(item);player.setItemSlot(slot,stack);score(fixture,threshold-1);
                Object contacts=field(player,LivingEntity.class,"recentKineticEnemies");
                require("denied kinetic start FAIL",item.use(helper.getLevel(),player,hand)==InteractionResult.FAIL,hand);
                require("denied start no use or new contact map",!player.isUsingItem()
                    && field(player,LivingEntity.class,"recentKineticEnemies")==contacts,hand);
                require("denied start one overlay and no sound",overlays(drain(fixture))==1,hand);
                score(fixture,threshold);
                require("allowed kinetic native CONSUME",item.use(helper.getLevel(),player,hand)==InteractionResult.CONSUME
                    && player.isUsingItem(),hand);
                Cow cow=target(player);var kinetic=stack.get(DataComponents.KINETIC_WEAPON);
                require("native clipped candidate eligible",ProjectileUtil.getHitEntitiesAlong(player,player.getAttackRangeWith(stack),
                    e -> PiercingWeapon.canHitEntity(player,e),ClipContext.Block.COLLIDER).right().orElse(List.of()).stream()
                    .anyMatch(hit -> hit.getEntity()==cow),hand);
                int remaining=stack.getUseDuration(player)-kinetic.delayTicks();drain(fixture);
                kinetic.damageEntities(stack,remaining+1,player,slot);
                require("native pre-delay continuation no contact",player.stabbedEntities(e -> e==cow)==0,hand);
                kinetic.damageEntities(stack,remaining,player,slot);
                require("allowed continuation records native contact",player.stabbedEntities(e -> e==cow)==1,hand);
                Cow later=target(player);float health=later.getHealth();score(fixture,threshold-1);
                kinetic.damageEntities(stack,remaining,player,slot);
                require("newly locked continuation no new contacts or damage",player.stabbedEntities(e -> e==later)==0
                    && later.getHealth()==health,hand);
                require("earlier contact history and active use retained",player.stabbedEntities(e -> e==cow)==1 && player.isUsingItem(),hand);
                require("continuation no overlay",overlays(drain(fixture))==0,hand);
                player.stopUsingItem();cow.discard();later.discard();drain(fixture);
            }
            score(fixture,threshold-1);player.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.COPPER_SWORD));
            require("inert material use keeps native PASS",Items.COPPER_SWORD.use(helper.getLevel(),player,InteractionHand.MAIN_HAND)==InteractionResult.PASS,"sword");
            player.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.SHIELD));
            require("shield priority remains native CONSUME",Items.SHIELD.use(helper.getLevel(),player,InteractionHand.MAIN_HAND)==InteractionResult.CONSUME
                && player.isUsingItem(),"shield");player.stopUsingItem();
            player.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.APPLE));player.getFoodData().setFoodLevel(10);
            require("food priority still native consumes",Items.APPLE.use(helper.getLevel(),player,InteractionHand.MAIN_HAND)!=InteractionResult.FAIL
                && player.isUsingItem(),"apple");player.stopUsingItem();
        });
    }
    @GameTest(maxTicks=200) public void targetlessPiercingAndLunge(GameTestHelper helper) {
        run(helper,"targetlessPiercingAndLunge",() -> {
            Joined fixture=join();ServerPlayer player=fixture.player();ItemStack stack=new ItemStack(Items.COPPER_SPEAR);
            stack.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LUNGE),1);
            player.setItemSlot(EquipmentSlot.MAINHAND,stack);int threshold=threshold(Items.COPPER_SPEAR);
            player.getFoodData().setFoodLevel(20);player.setDeltaMovement(Vec3.ZERO);score(fixture,threshold-1);
            float exhaustion=((Number)field(player.getFoodData(),player.getFoodData().getClass(),"exhaustionLevel")).floatValue();
            int wear=stack.getDamageValue();float charge=player.getAttackStrengthScale(0);
            stack.get(DataComponents.PIERCING_WEAPON).attack(player,EquipmentSlot.MAINHAND);
            require("targetless denied piercing no wear impulse exhaustion or onAttack",stack.getDamageValue()==wear
                && player.getDeltaMovement().equals(Vec3.ZERO) && player.getAttackStrengthScale(0)==charge
                && ((Number)field(player.getFoodData(),player.getFoodData().getClass(),"exhaustionLevel")).floatValue()==exhaustion,"lunge");
            List<Object> denied=drain(fixture);
            require("one piercing denial and no native sound tail",overlays(denied)==1
                && denied.stream().noneMatch(p -> p instanceof ClientboundSoundPacket),denied.size());
            player.postPiercingAttack();require("direct post helper denied before wear",stack.getDamageValue()==wear,"direct post");drain(fixture);
            score(fixture,threshold);
            require("native lunge prerequisites",!player.isPassenger() && !player.isFallFlying() && !player.isInWater()
                && player.getFoodData().getFoodLevel()>=7,"player state");
            player.postPiercingAttack();
            require("native targetless lunge positive wear impulse exhaustion",stack.getDamageValue()==wear+1
                && player.getDeltaMovement().horizontalDistanceSqr()>0
                && ((Number)field(player.getFoodData(),player.getFoodData().getClass(),"exhaustionLevel")).floatValue()>exhaustion,"no target required");
        });
    }
    @GameTest(maxTicks=200) public void nativeTeamAndAutonomousMob(GameTestHelper helper) {
        run(helper,"nativeTeamAndAutonomousMob",() -> {
            Joined first=join(),second=join();ServerPlayer player=first.player();ItemStack stack=new ItemStack(Items.COPPER_SPEAR);
            player.setItemSlot(EquipmentSlot.MAINHAND,stack);score(first,threshold(Items.COPPER_SPEAR));
            var board=helper.getLevel().getServer().getScoreboard();PlayerTeam team=board.addPlayerTeam("d3"+UUID.randomUUID().toString().substring(0,8));teams.add(team);
            team.setAllowFriendlyFire(false);board.addPlayerToTeam(player.getScoreboardName(),team);
            board.addPlayerToTeam(second.player().getScoreboardName(),team);
            require("native friendly-fire predicate denies",!PiercingWeapon.canHitEntity(player,second.player()),team.getName());
            try(var scope=AchieveToDoServer.openCombatInvocation(player,EquipmentSlot.MAINHAND,stack,true)) {
                require("scoped ATD preserves native team false",!PiercingWeapon.canHitEntity(player,second.player()),team.getName());
            }
            team.setAllowFriendlyFire(true);
            require("native eligible team positive control",PiercingWeapon.canHitEntity(player,second.player()),team.getName());
            // Controlled load geometry exercises production permission/index/predicate paths;
            // it does not claim a natural generated structure at these coordinates.
            Cow protectedTarget=target(player),allowedTarget=target(player);
            allowedTarget.setPos(player.getX(),player.getY()+.5,player.getZ()+4);
            @SuppressWarnings("unchecked") Map<AbilityType,Integer> configuration=(Map<AbilityType,Integer>)
                field(owner(),AchieveToDoServer.class,"abilitiesConfiguration");
            int materialThreshold=threshold(Items.COPPER_SPEAR);
            LandmarkType landmark=Arrays.stream(AbilityType.values()).filter(a -> a.getLandmarkType()!=null
                && configuration.get(a)>materialThreshold).map(AbilityType::getLandmarkType).findFirst().orElseThrow();
            DimensionalBlockBox region=new DimensionalBlockBox(DimensionType.OVERWORLD,
                com.diskree.achievetodo.client.Utils.toBlockBox(protectedTarget.getBoundingBox()));
            ChunkPos chunk=new ChunkPos(protectedTarget.blockPosition().getX() >> 4,protectedTarget.blockPosition().getZ() >> 4);
            Map<LandmarkType,Set<DimensionalBlockBox>> fixtureRegion=Map.of(landmark,Set.of(region));
            try {
                owner().onLandmarksLoadedStatusChanged(helper.getLevel(),chunk,fixtureRegion,true);
                drain(first);
                require("controlled target region locked by live permission",owner().isTargetInLockedLandmark(player,
                    DimensionType.OVERWORLD,protectedTarget.getBoundingBox(),true),landmark);
                require("mixed outside target allowed",!owner().isTargetInLockedLandmark(player,
                    DimensionType.OVERWORLD,allowedTarget.getBoundingBox(),true),landmark);
                require("standalone native predicate does not gain ATD scope",PiercingWeapon.canHitEntity(player,protectedTarget),"unscoped");
                try(var scope=AchieveToDoServer.openCombatInvocation(player,EquipmentSlot.MAINHAND,stack,true)) {
                    require("scoped protected target excluded",!PiercingWeapon.canHitEntity(player,protectedTarget),landmark);
                    require("scoped mixed allowed target retained",PiercingWeapon.canHitEntity(player,allowedTarget),landmark);
                    require("predicate probes do not notify",drain(first).stream().noneMatch(p -> p instanceof ClientboundCustomPayloadPacket packet
                        && packet.payload() instanceof com.diskree.achievetodo.networking.s2c.CheckTargetInLockedLandmarkPayload),"silent inner");
                    scope.reportDenial();scope.reportDenial();
                }
                require("one outer landmark denial",drain(first).stream().filter(p -> p instanceof ClientboundCustomPayloadPacket packet
                    && packet.payload() instanceof com.diskree.achievetodo.networking.s2c.CheckTargetInLockedLandmarkPayload).count()==1,"outer");
                var nativeCandidates=ProjectileUtil.getHitEntitiesAlong(player,player.getAttackRangeWith(stack),
                    e -> PiercingWeapon.canHitEntity(player,e),ClipContext.Block.COLLIDER).right().orElse(List.of());
                require("mixed targets genuinely native clipped candidates",nativeCandidates.stream().anyMatch(h -> h.getEntity()==protectedTarget)
                    && nativeCandidates.stream().anyMatch(h -> h.getEntity()==allowedTarget),nativeCandidates.size());
                Items.COPPER_SPEAR.use(helper.getLevel(),player,InteractionHand.MAIN_HAND);drain(first);
                var kinetic=stack.get(DataComponents.KINETIC_WEAPON);
                kinetic.damageEntities(stack,stack.getUseDuration(player)-kinetic.delayTicks(),player,EquipmentSlot.MAINHAND);
                require("mixed denied contact absent allowed contact retained",player.stabbedEntities(e -> e==protectedTarget)==0
                    && player.stabbedEntities(e -> e==allowedTarget)==1,"controlled region");
                require("continuation landmark probes remain silent",drain(first).stream().noneMatch(p -> p instanceof ClientboundCustomPayloadPacket packet
                    && packet.payload() instanceof com.diskree.achievetodo.networking.s2c.CheckTargetInLockedLandmarkPayload),"continuation");
                player.stopUsingItem();
            } finally {
                owner().onLandmarksLoadedStatusChanged(helper.getLevel(),chunk,fixtureRegion,false);
            }
            Cow cow=target(player);cow.setHealth(0);
            require("native dead target remains false",!PiercingWeapon.canHitEntity(player,cow),"dead target");cow.discard();
            score(first,threshold(Items.COPPER_SPEAR)-1);
            Mob mob=helper.spawn(EntityTypes.ZOMBIE,new BlockPos(2,2,1));entities.add(mob);mob.setNoAi(true);
            mob.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.COPPER_SPEAR));Cow victim=target(player);float health=victim.getHealth();
            require("autonomous native stab untouched",mob.stabAttack(EquipmentSlot.MAINHAND,victim,4,true,false,false)
                && victim.getHealth()<health,"Mob while player material locked");
        });
    }
    @GameTest(maxTicks=200) public void adventureNativeFalseAndTrue(GameTestHelper helper) {
        run(helper,"adventureNativeFalseAndTrue",() -> {
            Joined fixture=join();ServerPlayer player=fixture.player();player.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.COPPER_AXE));
            BlockPos block=helper.absolutePos(new BlockPos(1,1,1));helper.getLevel().setBlockAndUpdate(block,Blocks.STONE.defaultBlockState());
            int threshold=threshold(Items.COPPER_SPEAR);score(fixture,threshold-1);
            require("survival native false gets ATD restriction",player.blockActionRestricted(helper.getLevel(),block,GameType.SURVIVAL),"survival");
            // Native Adventure mayBuild=false/no-can-break is true and must remain true even when ATD allows.
            score(fixture,Integer.MAX_VALUE);player.setGameMode(GameType.ADVENTURE);drain(fixture);
            require("native Adventure true preserved",player.blockActionRestricted(helper.getLevel(),block,GameType.ADVENTURE),"no can-break component");
            // The actual native mayBuild branch is false; choose it directly using the live abilities state.
            player.getAbilities().mayBuild=true;score(fixture,threshold-1);
            require("Adventure mayBuild native false still examines ATD",player.blockActionRestricted(helper.getLevel(),block,GameType.ADVENTURE),"mayBuild");
            score(fixture,Integer.MAX_VALUE);
            require("ATD allowed Adventure native false retained",!player.blockActionRestricted(helper.getLevel(),block,GameType.ADVENTURE),"mayBuild");
            require("native Spectator true retained",player.blockActionRestricted(helper.getLevel(),block,GameType.SPECTATOR),"spectator");
        });
    }
}
