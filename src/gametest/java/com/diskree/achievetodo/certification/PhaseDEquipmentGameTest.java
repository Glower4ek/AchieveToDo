package com.diskree.achievetodo.certification;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.ability.PhaseDAbilityMappingTest.CaseContract;
import com.diskree.achievetodo.ability.PhaseDAbilityMappingTest.Outcome;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
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

public final class PhaseDEquipmentGameTest {
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
    private AchieveToDoServer owner() { return AchieveToDoMod.getServer(); }
    private static Object field(Object value, Class<?> type, String name) throws Exception {
        Field f=type.getDeclaredField(name);f.setAccessible(true);return f.get(value);
    }
    private void require(String name, boolean passed, Object observed) {
        System.out.println("D_S04_ASSERT "+new Gson().toJson(Map.of("scenario",scenario,"assertion",name,
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
        var server=helper.getLevel().getServer();UUID id=UUID.randomUUID();GameProfile profile=new GameProfile(id,"d4"+id.toString().substring(0,10));
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
        System.out.println("D_S04_STATE "+new Gson().toJson(Map.of("scenario",scenario,"native_path","Player.interactOn/Entity.interact/subtype or ItemStack fallback",
            "player",player.getUUID(),"target",target.getUUID(),"hand",hand,"before",before,"after",state(player,target,hand),"result",result.toString())));
        return result;
    }
    private InteractionResult subtype(Joined fixture,Mob target,InteractionHand hand) {
        ServerPlayer player=fixture.player();String before=state(player,target,hand);InteractionResult result;
        if(target instanceof Camel camel)result=camel.mobInteract(player,hand);
        else if(target instanceof Nautilus nautilus)result=nautilus.mobInteract(player,hand);
        else if(target instanceof HappyGhast ghast)result=ghast.mobInteract(player,hand);
        else throw new IllegalArgumentException("Unsupported fixture subtype");
        System.out.println("D_S04_STATE "+new Gson().toJson(Map.of("scenario",scenario,"native_path",target.getClass().getName()+".mobInteract",
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
            fixture.connection().disconnect(Component.literal("D-S04 fixture complete"));fixture.channel().runPendingTasks();fixture.channel().finishAndReleaseAll();
            if(!(Boolean)field(fixture.connection(),Connection.class,"disconnectionHandled"))fixture.connection().handleDisconnection();
            var server=helper.getLevel().getServer();server.getConnection().getConnections().remove(fixture.connection());
            require("player connection channel removed",server.getPlayerList().getPlayer(fixture.player().getUUID())==null&&!fixture.channel().isOpen()&&!server.getConnection().getConnections().contains(fixture.connection()),fixture.player().getUUID());
            server.getScoreboard().resetAllPlayerScores(fixture.player());
            require("temporary score removed",server.getScoreboard().getPlayerScoreInfo(fixture.player(),objective)==null,fixture.player().getUUID());joined.remove(fixture);
        }catch(Throwable t){if(failure==null)failure=t;else failure.addSuppressed(t);}
        for(Entity entity:entities){entity.discard();require("fixture entity discarded",entity.isRemoved(),entity.getUUID());}entities.clear();
        for(ItemEntity drop:drops())drop.discard();require("fixture drops removed",drops().isEmpty(),drops().size());
        boxes().clear();boxes().putAll(priorBoxes);membership().clear();membership().putAll(priorMembership);
        require("landmark maps restored",boxes().equals(priorBoxes)&&membership().equals(priorMembership),scenario);
        blocks.forEach((p,b) -> helper.getLevel().setBlockAndUpdate(p,b));
        for(var e:blocks.entrySet())require("world block restored",helper.getLevel().getBlockState(e.getKey()).equals(e.getValue()),e.getKey());blocks.clear();
        var board=helper.getLevel().getServer().getScoreboard();displays.forEach(board::setDisplayObjective);if(createdObjective){board.removeObjective(objective);createdObjective=false;}
        owner().prepareScoreboard(board);
        for(var e:displays.entrySet())require("scoreboard display restored",board.getDisplayObjective(e.getKey())==e.getValue(),e.getKey());
        if(failure!=null)throw new IllegalStateException("D-S04 cleanup failed",failure);
    }
    private void run(GameTestHelper helper,String name,Scenario body) {
        this.helper=helper;scenario=name;var board=helper.getLevel().getServer().getScoreboard();
        for(DisplaySlot slot:DisplaySlot.values())displays.put(slot,board.getDisplayObjective(slot));
        objective=board.getObjective("bac_advancements");if(objective==null){createdObjective=true;objective=board.addObjective("bac_advancements",ObjectiveCriteria.DUMMY,Component.literal("D-S04"),ObjectiveCriteria.RenderType.INTEGER,false,null);}
        board.setDisplayObjective(DisplaySlot.SIDEBAR,objective);owner().prepareScoreboard(board);
        helper.runAfterDelay(5,() -> {
            try {
                priorBoxes=new HashMap<>(boxes());priorMembership=new HashMap<>(membership());priorItems=new HashSet<>();
                helper.getLevel().getEntitiesOfClass(ItemEntity.class,region()).forEach(e -> priorItems.add(e.getUUID()));
                require("server permission ready",!owner().isNotReady(),name);body.run();cleanup();
                System.out.println("D_S04_GAMEPLAY_RECEIPT "+new Gson().toJson(Map.of("scenario",scenario,"status","PASS","assertions",assertions,"cleanup",true,"scope","Joined server native interactions; client GUI and save/reload NOT_RUN")));helper.succeed();
            }catch(Throwable t){t.printStackTrace();try{cleanup();}catch(Throwable c){t.addSuppressed(c);c.printStackTrace();}
                System.out.println("D_S04_GAMEPLAY_RECEIPT "+new Gson().toJson(Map.of("scenario",scenario,"status","FAIL","error",t.toString())));helper.fail(name+": "+t);}
        });
    }
    public static final List<CaseContract> FUTURE_CASES = List.of(PhaseDAbilityRoutesGameTest.future(
        "P2:G-ROUTE-0000-NATIVE_DENIED", "T01/PE-06", "CH-13", "D-S04", Outcome.NATIVE_DENIED,
        "P2:t01_future_regression_matrix.json#/cases/0; SHA256 08698f383ff18ac6531228eafadee9217a1c039df4dfd07ae6f44beb846cbb0e; P10 corrected oracle required",
        "Equippable.swapWithEquipmentSlot; P10 p10_p2_case_specific_oracle_corrections.json route0000",
        "Joined SURVIVAL Player; minecraft:black_harness; self-use actual hand; BODY",
        "allowed_entities/canUseSlot reject player; no successful equip expected", "N/A native-invalid animal BODY item",
        Map.of("equipment_mutation_delta", "0", "prohibited_consumption_delta", "0"),
        List.of("Retain documented native fallback effects separately", "No invented player copper eligibility")));

    @GameTest(maxTicks = 20)
    public void baselineArmorSelectors(GameTestHelper helper) {
        PhaseDAbilityRoutesGameTest.runControl(helper, PhaseDAbilityRoutesGameTest.control(
            "D-S00-G-EQUIPMENT-CONTROL", Map.of("iron", "EQUIP_IRON_ARMOR", "leather", "EQUIP_LEATHER_ARMOR",
                "wolf_player_mapping", "absent", "saddle_player_mapping", "absent")), () -> Map.of(
            "iron", AbilityType.findEquipmentEquipAbility(Items.IRON_CHESTPLATE).name(),
            "leather", AbilityType.findEquipmentEquipAbility(Items.LEATHER_BOOTS).name(),
            "wolf_player_mapping", AbilityType.findEquipmentEquipAbility(Items.WOLF_ARMOR) == null ? "absent" : "present",
            "saddle_player_mapping", AbilityType.findEquipmentEquipAbility(Items.SADDLE) == null ? "absent" : "present"));
    }

    @GameTest(maxTicks=200) public void camelMenuAndExistingInteractions(GameTestHelper helper) {
        run(helper,"camelMenuAndExistingInteractions",() -> {
            Joined f=join();ServerPlayer p=f.player();Camel camel=spawn(EntityTypes.CAMEL,f);
            for(InteractionHand hand:InteractionHand.values()) {
                p.setItemInHand(hand,ItemStack.EMPTY);landmark(f,camel,true);p.setShiftKeyDown(true);var prior=p.containerMenu;
                InteractionResult denied=subtype(f,camel,hand);var packets=drain(f);
                require("Camel native eligible menu denied FAIL",denied==InteractionResult.FAIL,hand+" "+denied);
                require("Camel no open packet or container replacement",opens(packets)==0&&p.containerMenu==prior,hand+" packets="+packets.size());
                require("Camel one existing landmark feedback",feedback(packets)==1,feedback(packets));
                landmark(f,camel,false);require("Camel allowed native SUCCESS",subtype(f,camel,hand)==InteractionResult.SUCCESS,hand);
                packets=drain(f);require("Camel real horse menu and opening packet",p.containerMenu instanceof HorseInventoryMenu&&opens(packets)==1&&feedback(packets)==0,p.containerMenu.getClass());
                require("Camel menu bound to exact target",((MountInventoryMenuExtension)p.containerMenu).achievetodo$getMount()==camel,camel.getUUID());
                require("Camel menu native valid",p.containerMenu.stillValid(p),hand);p.closeContainer();drain(f);
            }
            camel.setAge(-100);p.setShiftKeyDown(true);landmark(f,camel,false);var prior=p.containerMenu;
            InteractionResult baby=subtype(f,camel,InteractionHand.MAIN_HAND);require("baby native fallback has no mount menu",p.containerMenu==prior&&opens(drain(f))==0,baby);
            camel.setAge(0);p.setShiftKeyDown(false);landmark(f,camel,true);
            require("existing Camel riding veto retained",subtype(f,camel,InteractionHand.MAIN_HAND)==InteractionResult.FAIL&&p.getVehicle()!=camel,camel.getPassengers());
            require("existing ride one feedback",feedback(drain(f))==1,"ride");landmark(f,camel,false);
            require("ordinary Camel interaction rides without menu",subtype(f,camel,InteractionHand.MAIN_HAND).consumesAction()&&p.getVehicle()==camel&&opens(drain(f))==0,camel.getPassengers());p.stopRiding();
            camel.setHealth(camel.getMaxHealth()-4);hands(p,InteractionHand.OFF_HAND,Items.CACTUS);landmark(f,camel,true);float hp=camel.getHealth();
            subtype(f,camel,InteractionHand.OFF_HAND);require("existing protected Camel heal no consumption",camel.getHealth()==hp&&p.getOffhandItem().getCount()==1,state(p,camel,InteractionHand.OFF_HAND));
            require("existing heal one feedback",feedback(drain(f))==1,"heal");landmark(f,camel,false);
            subtype(f,camel,InteractionHand.OFF_HAND);require("native eligible Camel feed heals and consumes",camel.getHealth()>hp&&p.getOffhandItem().isEmpty(),camel.getHealth());
            p.stopRiding();hands(p,InteractionHand.MAIN_HAND,Items.STONE);camel.setAge(-100);subtype(f,camel,InteractionHand.MAIN_HAND);
            require("native ineligible baby stone neither consumed nor menu",p.getMainHandItem().getCount()==1&&p.containerMenu==p.inventoryMenu&&opens(drain(f))==0,"baby stone");
        });
    }

    @GameTest(maxTicks=200) public void manualShearsAndLeashConnections(GameTestHelper helper) {
        run(helper,"manualShearsAndLeashConnections",() -> {
            Joined f=join();ServerPlayer p=f.player();
            for(InteractionHand hand:InteractionHand.values()) {
                Cow receiver=spawn(EntityTypes.COW,f);Cow first=spawn(EntityTypes.COW,f);Cow second=spawn(EntityTypes.COW,f);
                first.setPos(receiver.getX()+5,receiver.getY(),receiver.getZ());second.setPos(receiver.getX()+9,receiver.getY(),receiver.getZ());
                ((Leashable)first).setLeashedTo(receiver,true);((Leashable)second).setLeashedTo(receiver,true);
                hands(p,hand,Items.SHEARS);landmark(f,second,true);require("receiver outside locked source box",!owner().isTargetInLockedLandmark(p,DimensionType.OVERWORLD,receiver.getBoundingBox(),true),receiver.getUUID());
                int count=drops().size();ItemStack shears=p.getItemInHand(hand);InteractionResult denied=interact(f,receiver,hand);var packets=drain(f);
                require("all affected links denied atomically",!denied.consumesAction()&&((Leashable)first).getLeashHolder()==receiver&&((Leashable)second).getLeashHolder()==receiver,denied);
                require("denied connections no drops wear or sound",drops().size()==count&&shears.getDamageValue()==0&&sounds(packets)==0,hand);
                require("affected source one feedback",feedback(packets)==1,feedback(packets));
                boxes().clear();membership().clear();score(f,threshold(AbilityType.USE_SHEARS)-1);drain(f);
                require("USE_SHEARS denial retains both links",!interact(f,receiver,hand).consumesAction()&&((Leashable)first).isLeashed()&&((Leashable)second).isLeashed(),hand);
                require("USE_SHEARS denial no durability",shears.getDamageValue()==0,shears.getDamageValue());
                score(f,10000);drain(f);require("allowed native cuts both connections",interact(f,receiver,hand).consumesAction()&&!((Leashable)first).isLeashed()&&!((Leashable)second).isLeashed(),hand);
                require("native link drops and actual hand wear",drops().size()==count+2&&shears.getDamageValue()==1,drops().size());
                ((Leashable)receiver).setLeashedTo(p,true);landmark(f,receiver,true);int wear=shears.getDamageValue();count=drops().size();
                require("own leash denied before drop",!interact(f,receiver,hand).consumesAction()&&((Leashable)receiver).isLeashed()&&shears.getDamageValue()==wear&&drops().size()==count,hand);
                require("null-player native autonomous removal remains allowed",receiver.shearOffAllLeashConnections(null)&&!((Leashable)receiver).isLeashed(),hand);
                boxes().clear();membership().clear();score(f,0);hands(p,hand,Items.SHEARS);drain(f);
                require("invalid empty Cow shears retains native no action",!interact(f,receiver,hand).consumesAction()&&p.getItemInHand(hand).getDamageValue()==0,hand);
                require("ineligible Cow no denial",feedback(drain(f))==0,hand);
                HappyGhast ghast=spawn(EntityTypes.HAPPY_GHAST,f);score(f,10000);var harness=new ItemStack(Items.HARNESS.pick(net.minecraft.world.item.DyeColor.BLACK));
                require("real native harness setup",harness.get(DataComponents.EQUIPPABLE).equipOnTarget(p,ghast,harness).consumesAction()&&ghast.isWearingBodyArmor(),hand);
                score(f,threshold(AbilityType.USE_SHEARS)-1);hands(p,hand,Items.SHEARS);drain(f);count=drops().size();
                require("eligible equipment denied before clearing",!interact(f,ghast,hand).consumesAction()&&ghast.isWearingBodyArmor()&&p.getItemInHand(hand).getDamageValue()==0&&drops().size()==count,hand);
                require("denied equipment no shear sound",sounds(drain(f))==0,hand);
                landmark(f,ghast,true);hands(p,hand,Items.SHEARS);require("landmark denial keeps equipment",!interact(f,ghast,hand).consumesAction()&&ghast.isWearingBodyArmor(),hand);drain(f);
                landmark(f,ghast,false);hands(p,hand,Items.SHEARS);require("allowed native equipment shear",interact(f,ghast,hand).consumesAction()&&!ghast.isWearingBodyArmor()&&p.getItemInHand(hand).getDamageValue()==1,hand);
                require("allowed equipment drop exactly one",drops().size()==count+1,drops().size());
                var protectedHarness=new ItemStack(Items.HARNESS.pick(net.minecraft.world.item.DyeColor.BLACK));
                protectedHarness.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.BINDING_CURSE),1);
                ghast.setItemSlot(EquipmentSlot.BODY,protectedHarness);boxes().clear();membership().clear();score(f,0);hands(p,hand,Items.SHEARS);drain(f);count=drops().size();
                require("restored curse native no shear preserves permitted ride fallback",interact(f,ghast,hand).consumesAction()&&p.getVehicle()==ghast&&ghast.getItemBySlot(EquipmentSlot.BODY)==protectedHarness&&p.getItemInHand(hand).getDamageValue()==0&&drops().size()==count,hand);
                require("native cursed ineligible shear no new denial",feedback(drain(f))==0,hand);
                p.stopRiding();p.setGameMode(GameType.CREATIVE);drain(f);require("native creative curse exemption retained",interact(f,ghast,hand).consumesAction()&&ghast.getItemBySlot(EquipmentSlot.BODY).isEmpty()&&p.getItemInHand(hand).getDamageValue()==0,hand);p.setGameMode(GameType.SURVIVAL);
            }
        });
    }

    @GameTest(maxTicks=200) public void targetEquipmentNativeEligibility(GameTestHelper helper) {
        run(helper,"targetEquipmentNativeEligibility",() -> {
            Joined f=join();ServerPlayer p=f.player();HappyGhast ghast=spawn(EntityTypes.HAPPY_GHAST,f);Cow cow=spawn(EntityTypes.COW,f);
            for(InteractionHand hand:InteractionHand.values()) {
                ghast.setItemSlot(EquipmentSlot.BODY,ItemStack.EMPTY);landmark(f,ghast,true);hands(p,hand,Items.HARNESS.pick(net.minecraft.world.item.DyeColor.BLACK));
                require("native eligible target denied with count retained",!interact(f,ghast,hand).consumesAction()&&!ghast.isWearingBodyArmor()&&p.getItemInHand(hand).getCount()==1,hand);
                require("nested equip fallback one notification",feedback(drain(f))==1,hand);
                // Same-tick independent interactions must still each notify; there is no time-based suppression.
                require("second separate attempt also denied",!interact(f,ghast,hand).consumesAction(),hand);require("second interaction one notification",feedback(drain(f))==1,hand);
                landmark(f,ghast,false);require("native eligible target equip consumes one",interact(f,ghast,hand).consumesAction()&&ghast.isWearingBodyArmor()&&p.getItemInHand(hand).isEmpty(),hand);p.stopRiding();
                hands(p,hand,Items.HARNESS.pick(net.minecraft.world.item.DyeColor.WHITE));var c=p.getItemInHand(hand).get(DataComponents.EQUIPPABLE);
                require("occupied native direct equip PASS",c.equipOnTarget(p,ghast,p.getItemInHand(hand))==InteractionResult.PASS&&p.getItemInHand(hand).getCount()==1&&ghast.getItemBySlot(EquipmentSlot.BODY).is(Items.HARNESS.pick(net.minecraft.world.item.DyeColor.BLACK)),hand);
                ghast.setItemSlot(EquipmentSlot.BODY,ItemStack.EMPTY);ghast.setAge(-100);landmark(f,ghast,true);
                require("native baby direct equip PASS no permission invented",c.equipOnTarget(p,ghast,p.getItemInHand(hand))==InteractionResult.PASS&&!ghast.isWearingBodyArmor()&&p.getItemInHand(hand).getCount()==1,hand);
                require("baby ineligible no denial notification",feedback(drain(f))==0,hand);ghast.setAge(0);
                require("wrong recipient native PASS",c.equipOnTarget(p,cow,p.getItemInHand(hand))==InteractionResult.PASS&&cow.getItemBySlot(EquipmentSlot.BODY).isEmpty(),hand);
                hands(p,hand,Items.COPPER_CHESTPLATE);var humanoid=p.getItemInHand(hand).get(DataComponents.EQUIPPABLE);
                require("ordinary copper armor target path natively unsupported",!humanoid.equipOnInteract()&&p.getItemInHand(hand).interactLivingEntity(p,cow,hand)==InteractionResult.PASS&&cow.getItemBySlot(EquipmentSlot.CHEST).isEmpty()&&p.getItemInHand(hand).getCount()==1,hand);
            }
            Camel camel=spawn(EntityTypes.CAMEL,f);p.setShiftKeyDown(false);landmark(f,camel,true);hands(p,InteractionHand.MAIN_HAND,Items.SADDLE);
            require("Camel equipment deny cannot fall through to ride",!interact(f,camel,InteractionHand.MAIN_HAND).consumesAction()&&p.getVehicle()!=camel&&camel.getItemBySlot(EquipmentSlot.SADDLE).isEmpty(),camel.getUUID());
            require("Camel equip/ride/repeated equip one feedback",feedback(drain(f))==1,"Camel");
            Nautilus nautilus=spawn(EntityTypes.NAUTILUS,f);nautilus.tame(p);landmark(f,nautilus,true);hands(p,InteractionHand.OFF_HAND,Items.SADDLE);
            require("Nautilus equipment deny cannot ride",!interact(f,nautilus,InteractionHand.OFF_HAND).consumesAction()&&p.getVehicle()!=nautilus&&nautilus.getItemBySlot(EquipmentSlot.SADDLE).isEmpty(),nautilus.getUUID());
            require("Nautilus equip/ride/repeated equip one feedback",feedback(drain(f))==1,"Nautilus");
            Wolf wolf=spawn(EntityTypes.WOLF,f);wolf.tame(p);landmark(f,wolf,true);hands(p,InteractionHand.MAIN_HAND,Items.WOLF_ARMOR);
            // The accepted historical Wolf hook intentionally returns SUCCESS on denial.
            // Preserve that existing route's result while independently proving no mutation/consumption.
            require("existing wolf legacy SUCCESS denial retains item and armor",interact(f,wolf,InteractionHand.MAIN_HAND)==InteractionResult.SUCCESS&&wolf.getItemBySlot(EquipmentSlot.BODY).isEmpty()&&p.getMainHandItem().getCount()==1,wolf.getUUID());
            require("existing wolf denial one feedback",feedback(drain(f))==1,wolf.getUUID());
            landmark(f,wolf,false);require("native owner wolf armor application",interact(f,wolf,InteractionHand.MAIN_HAND).consumesAction()&&wolf.getItemBySlot(EquipmentSlot.BODY).is(Items.WOLF_ARMOR),wolf.getUUID());
            Joined outsider=join();score(outsider,10000);hands(outsider.player(),InteractionHand.OFF_HAND,Items.SHEARS);drain(outsider);
            require("nonowner native wolf shear refuses without wear",!interact(outsider,wolf,InteractionHand.OFF_HAND).consumesAction()&&wolf.getItemBySlot(EquipmentSlot.BODY).is(Items.WOLF_ARMOR)&&outsider.player().getOffhandItem().getDamageValue()==0,wolf.getUUID());
            require("nonowner native refusal no new target denial",feedback(drain(outsider))==0,"nonowner");
        });
    }

    @GameTest(maxTicks=200) public void nautilusAndHappyGhastRoutes(GameTestHelper helper) {
        run(helper,"nautilusAndHappyGhastRoutes",() -> {
            Joined f=join();ServerPlayer p=f.player();Nautilus n=spawn(EntityTypes.NAUTILUS,f);p.setShiftKeyDown(false);
            require("actual Nautilus food tag",n.isFood(new ItemStack(Items.PUFFERFISH)),Items.PUFFERFISH);
            for(InteractionHand hand:InteractionHand.values()) {
                n.setTame(false,true);hands(p,hand,Items.PUFFERFISH);landmark(f,n,true);
                require("untamed consumption/tame attempt denied",!subtype(f,n,hand).consumesAction()&&!n.isTame()&&p.getItemInHand(hand).getCount()==1,hand);
                require("tame attempt one feedback",feedback(drain(f))==1,hand);landmark(f,n,false);
                n.getRandom().setSeed(0);require("native allowed tame attempt consumes food",subtype(f,n,hand).consumesAction()&&p.getItemInHand(hand).isEmpty(),hand);
                require("seeded real native tame success",n.isTame(),n.getUUID());
                n.tame(p);n.setHealth(n.getMaxHealth()-4);hands(p,hand,Items.PUFFERFISH);landmark(f,n,true);float hp=n.getHealth();
                require("injured native feed denied before consumption",subtype(f,n,hand)==InteractionResult.FAIL&&n.getHealth()==hp&&p.getItemInHand(hand).getCount()==1,hand);
                require("feed one feedback",feedback(drain(f))==1,hand);landmark(f,n,false);
                require("allowed injured feed heals consumes",subtype(f,n,hand).consumesAction()&&n.getHealth()>hp&&p.getItemInHand(hand).isEmpty(),hand);
                hands(p,hand,Items.STONE);landmark(f,n,true);
                require("native eligible Nautilus ride denied",subtype(f,n,hand)==InteractionResult.FAIL&&p.getVehicle()!=n&&p.getItemInHand(hand).getCount()==1,hand);
                require("ride one feedback",feedback(drain(f))==1,hand);landmark(f,n,false);
                require("allowed Nautilus ride",subtype(f,n,hand).consumesAction()&&p.getVehicle()==n,hand);p.stopRiding();
                p.setShiftKeyDown(true);landmark(f,n,true);var prior=p.containerMenu;
                require("Nautilus menu denied without packet",subtype(f,n,hand)==InteractionResult.FAIL&&p.containerMenu==prior&&opens(drain(f))==0,hand);
                landmark(f,n,false);require("allowed Nautilus real menu",subtype(f,n,hand)==InteractionResult.SUCCESS&&p.containerMenu instanceof NautilusInventoryMenu&&opens(drain(f))==1,hand);p.closeContainer();p.setShiftKeyDown(false);
            }
            n.setAge(-100);hands(p,InteractionHand.MAIN_HAND,Items.STONE);landmark(f,n,true);var prior=p.containerMenu;
            require("baby Nautilus native invalid retains PASS",subtype(f,n,InteractionHand.MAIN_HAND)==InteractionResult.PASS&&p.containerMenu==prior&&p.getVehicle()!=n, n.getUUID());
            require("baby invalid no denial",feedback(drain(f))==0,"baby");
            HappyGhast g=spawn(EntityTypes.HAPPY_GHAST,f);landmark(f,g,true);hands(p,InteractionHand.MAIN_HAND,Items.STONE);
            require("unharnessed native invalid no ride",subtype(f,g,InteractionHand.MAIN_HAND)==InteractionResult.PASS&&p.getVehicle()!=g,g.getUUID());
            require("unharnessed no denial",feedback(drain(f))==0,g.getUUID());landmark(f,g,false);
            var harness=new ItemStack(Items.HARNESS.pick(net.minecraft.world.item.DyeColor.BLACK));harness.get(DataComponents.EQUIPPABLE).equipOnTarget(p,g,harness);require("native harness positive",g.isWearingBodyArmor(),g.getUUID());
            for(InteractionHand hand:InteractionHand.values()) {
                hands(p,hand,Items.STONE);landmark(f,g,true);require("adult harnessed ride denied",subtype(f,g,hand)==InteractionResult.FAIL&&p.getVehicle()!=g,hand);
                require("Happy Ghast ride one feedback",feedback(drain(f))==1,hand);landmark(f,g,false);require("allowed Happy Ghast rides",subtype(f,g,hand)==InteractionResult.SUCCESS&&p.getVehicle()==g,hand);p.stopRiding();
                p.setShiftKeyDown(true);landmark(f,g,true);require("secondary Happy Ghast native no ride or menu",subtype(f,g,hand)==InteractionResult.PASS&&p.getVehicle()!=g&&opens(drain(f))==0,hand);p.setShiftKeyDown(false);
            }
            for(int i=0;i<4;i++){Cow passenger=spawn(EntityTypes.COW,f);require("native autonomous passenger setup",passenger.startRiding(g),i);}drain(f);
            require("full Happy Ghast retains native success without impossible ride",subtype(f,g,InteractionHand.MAIN_HAND)==InteractionResult.SUCCESS&&p.getVehicle()!=g&&g.getPassengers().size()==4,g.getPassengers().size());
            require("capacity refusal no new denial notification",feedback(drain(f))==0,"full");
        });
    }

    @GameTest(maxTicks=200) public void mountMenuClicksAndQuickMove(GameTestHelper helper) {
        run(helper,"mountMenuClicksAndQuickMove",() -> {
            Joined f=join();ServerPlayer p=f.player();Camel camel=spawn(EntityTypes.CAMEL,f);landmark(f,camel,false);p.setShiftKeyDown(true);
            subtype(f,camel,InteractionHand.MAIN_HAND);require("real opened mount menu",p.containerMenu instanceof HorseInventoryMenu&&opens(drain(f))==1,p.containerMenu.getClass());
            var menu=p.containerMenu;int source=-1;
            for(int i=0;i<menu.slots.size();i++)if(menu.getSlot(i).container==p.getInventory()&&menu.getSlot(i).getContainerSlot()==0)source=i;
            require("actual player inventory slot in mount menu",source>=0,source);p.getInventory().setItem(0,new ItemStack(Items.SADDLE));
            menu.clicked(source,0,ContainerInput.PICKUP,p);require("ordinary native pickup moves source to carried",p.getInventory().getItem(0).isEmpty()&&menu.getCarried().is(Items.SADDLE),source);
            menu.clicked(0,0,ContainerInput.PICKUP,p);require("ordinary native place equips saddle",camel.getItemBySlot(EquipmentSlot.SADDLE).is(Items.SADDLE)&&menu.getCarried().isEmpty(),camel.getUUID());
            landmark(f,camel,true);require("relocked opened menu validity false",!menu.stillValid(p),menu.containerId);drain(f);
            menu.clicked(0,0,ContainerInput.PICKUP,p);var packets=drain(f);
            require("denied ordinary removal conserves carried/equipment",menu.getCarried().isEmpty()&&camel.getItemBySlot(EquipmentSlot.SADDLE).getCount()==1,menu.containerId);
            require("direct denied click native content resync",packets.stream().anyMatch(v -> v instanceof ClientboundContainerSetContentPacket),packets.size());
            require("direct denied quick move EMPTY and equipment continuity",menu.quickMoveStack(p,0).isEmpty()&&camel.getItemBySlot(EquipmentSlot.SADDLE).getCount()==1,menu.containerId);drain(f);
            landmark(f,camel,false);require("permission unlock revalidates native menu",menu.stillValid(p),menu.containerId);
            require("allowed quick move removes saddle into real player inventory",!menu.quickMoveStack(p,0).isEmpty()&&camel.getItemBySlot(EquipmentSlot.SADDLE).isEmpty(),menu.containerId);
            int saddleSlot=-1;for(int i=2;i<menu.slots.size();i++)if(menu.getSlot(i).getItem().is(Items.SADDLE))saddleSlot=i;
            require("native quick move saddle source found",saddleSlot>=0,saddleSlot);landmark(f,camel,true);
            require("denied quick insertion retains source count",menu.quickMoveStack(p,saddleSlot).isEmpty()&&menu.getSlot(saddleSlot).getItem().getCount()==1&&camel.getItemBySlot(EquipmentSlot.SADDLE).isEmpty(),saddleSlot);drain(f);
            landmark(f,camel,false);require("allowed quick insertion equips actual saddle",!menu.quickMoveStack(p,saddleSlot).isEmpty()&&camel.getItemBySlot(EquipmentSlot.SADDLE).getCount()==1&&menu.getSlot(saddleSlot).getItem().isEmpty(),saddleSlot);
            // No empty or incompatible native source becomes a valid transfer.
            p.getInventory().setItem(0,new ItemStack(Items.COPPER_CHESTPLATE));menu.quickMoveStack(p,source);
            require("native incompatible copper armor cannot equip Camel BODY",camel.getItemBySlot(EquipmentSlot.BODY).isEmpty(),camel.getUUID());
            camel.setPos(p.getX()+30,p.getY(),p.getZ());require("native distance false preserved",!menu.stillValid(p),camel.position());camel.setPos(p.getX()+2,p.getY(),p.getZ());
            p.getInventory().setItem(1,new ItemStack(Items.STONE));int carriedSource=-1;
            for(int i=2;i<menu.slots.size();i++)if(menu.getSlot(i).container==p.getInventory()&&menu.getSlot(i).getContainerSlot()==1)carriedSource=i;
            require("real carried-stack source",carriedSource>=0,carriedSource);menu.clicked(carriedSource,0,ContainerInput.PICKUP,p);
            require("native carried setup conserved",menu.getCarried().is(Items.STONE)&&p.getInventory().getItem(1).isEmpty(),menu.getCarried());
            landmark(f,camel,true);int id=menu.containerId;
            var click=new ServerboundContainerClickPacket(id,menu.getStateId(),(short)0,(byte)0,ContainerInput.QUICK_MOVE,new Int2ObjectOpenHashMap<>(),HashedStack.EMPTY);
            click.handle(p.connection);click.handle(p.connection);
            require("actual repeated invalid-menu packets cannot move items",camel.getItemBySlot(EquipmentSlot.SADDLE).getCount()==1&&menu.getCarried().getCount()==1&&p.getInventory().getItem(1).isEmpty(),id);
            p.doTick();packets=drain(f);
            require("native player tick closes invalid menu",p.containerMenu==p.inventoryMenu&&packets.stream().anyMatch(v -> v instanceof ClientboundContainerClosePacket),p.containerMenu.getClass());
            require("saddle conserved across invalid menu close",camel.getItemBySlot(EquipmentSlot.SADDLE).getCount()==1,camel.getUUID());
            int stone=0;for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i).is(Items.STONE))stone+=p.getInventory().getItem(i).getCount();
            require("native close returns exact carried stack no loss",stone==1&&menu.getCarried().isEmpty(),stone);click.handle(p.connection);
            require("old container packet ignored after native close",camel.getItemBySlot(EquipmentSlot.SADDLE).getCount()==1&&p.containerMenu==p.inventoryMenu,id);
            p.inventoryMenu.setCarried(new ItemStack(Items.STONE));p.inventoryMenu.clicked(-999,0,ContainerInput.PICKUP,p);
            require("ordinary player inventory unaffected by mount guard",p.inventoryMenu.getCarried().isEmpty(),"outside drop");
        });
    }
}
