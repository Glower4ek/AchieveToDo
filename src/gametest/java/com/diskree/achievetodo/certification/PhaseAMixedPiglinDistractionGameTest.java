package com.diskree.achievetodo.certification;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import net.fabricmc.fabric.api.gametest.v1.*;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
/** A finite gold ingot reaches an adult piglin only through the joined player's native packet. */
public final class PhaseAMixedPiglinDistractionGameTest implements CustomTestMethodInvoker {
    @GameTest(maxTicks=100)public void mixedPiglinDistractionCanary(GameTestHelper h){run(h,false);}
    @GameTest(maxTicks=100)public void mixedPiglinDistractionExact1(GameTestHelper h){run(h,true);}
    private static void run(GameTestHelper h,boolean exact){
        Context c=new Context();try{
            c.run=PhaseAMixedPiglinDistractionExecutionEvidence.begin(PhaseAMixedPiglinDistractionExecutionEvidence.root(),exact);
            c.row=PhaseAMixedPiglinDistractionExecutionEvidence.selected().getFirst();
            BlockPos pos=h.absolutePos(new BlockPos(5,2,5));for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)h.getLevel().setBlockAndUpdate(pos.offset(x,-1,z),Blocks.STONE.defaultBlockState());
            c.joined=join(h);var p=c.joined.player();p.setGameMode(GameType.SURVIVAL);new ServerboundPlayerLoadedPacket().handle(p.connection);require(p.connection.hasClientLoaded()&&!p.hasInfiniteMaterials()&&!p.isSpectator(),"Joined finite lifecycle missing");
            p.teleportTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5);acknowledge(c.joined);p.getInventory().clearContent();for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET,EquipmentSlot.OFFHAND})p.setItemSlot(slot,ItemStack.EMPTY);
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.GOLD_INGOT));require(armorEmpty(p),"Armor exclusion missing");
            c.piglin=(Piglin)BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse("minecraft:piglin")).create(h.getLevel(),EntitySpawnReason.COMMAND);require(c.piglin!=null,"Piglin creation failed");c.piglin.setBaby(false);c.piglin.setNoAi(true);c.piglin.teleportTo(p.getX()+1,p.getY(),p.getZ());c.piglin.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);require(h.getLevel().addFreshEntity(c.piglin),"Piglin spawn failed");
            c.advancement=h.getLevel().getServer().getAdvancements().get(Identifier.parse(PhaseAMixedPiglinDistractionCertification.ADVANCEMENT));require(c.advancement!=null&&!done(c),"Criterion pre-completed");h.runAfterDelay(1,()->interact(h,c,exact));
        }catch(Throwable e){fail(h,c,e);}
    }
    private static void interact(GameTestHelper h,Context c,boolean exact){try{
        var p=c.joined.player();require(!done(c)&&!c.piglin.isBaby()&&armorEmpty(p)&&p.distanceToSqr(c.piglin)<4,"Pre-action context changed");
        var stack=p.getMainHandItem();c.before=stack.getCount();c.pigBefore=c.piglin.getOffhandItem().getCount();c.tag=stack.is(TagKey.create(Registries.ITEM,Identifier.parse("minecraft:piglin_loved")));require(c.before==1&&c.pigBefore==0&&c.tag&&stack.is(Items.GOLD_INGOT),"Finite gold fixture changed");
        new ServerboundInteractPacket(c.piglin.getId(),InteractionHand.MAIN_HAND,c.piglin.position(),false).handle(p.connection);
        require(done(c),"Native interaction did not complete criterion");finish(h,c,exact);
    }catch(Throwable e){fail(h,c,e);}}
    private static void finish(GameTestHelper h,Context c,boolean exact)throws Exception{
        var p=c.joined.player();require(p.getMainHandItem().isEmpty()&&c.piglin.getOffhandItem().is(Items.GOLD_INGOT)&&c.piglin.getOffhandItem().getCount()==1,"Native gold transfer missing");
        boolean admiring=c.piglin.getBrain().hasMemoryValue(MemoryModuleType.ADMIRING_ITEM);require(admiring,"Native admiration memory missing");
        JsonObject r=new JsonObject();for(String key:new String[]{"advancementId","criterion","requirementGroup"})r.add(key,c.row.get(key).deepCopy());
        r.addProperty("family",PhaseAMixedPiglinDistractionCertification.FAMILY);r.addProperty("source",PhaseAMixedPiglinDistractionCertification.SOURCE);r.addProperty("runId",c.run);r.addProperty("catalogFingerprint",PhaseAMixedPiglinDistractionEvidenceValidation.fingerprint(PhaseAMixedPiglinDistractionExecutionEvidence.root()));r.addProperty("trigger","minecraft:player_interacted_with_entity");r.addProperty("boundary",PhaseAMixedPiglinDistractionCertification.BOUNDARY);r.addProperty("result","GREEN");r.addProperty("gameMode",p.gameMode().name());r.addProperty("playerUuid",p.getUUID().toString());r.addProperty("targetUuid",c.piglin.getUUID().toString());r.addProperty("entityType",BuiltInRegistries.ENTITY_TYPE.getKey(c.piglin.getType()).toString());r.addProperty("observedItem","minecraft:gold_ingot");
        r.addProperty("joined",h.getLevel().getServer().getPlayerList().getPlayer(p.getUUID())==p);r.addProperty("connectionRegistered",h.getLevel().getServer().getConnection().getConnections().contains(c.joined.connection()));r.addProperty("clientLoaded",p.connection.hasClientLoaded());r.addProperty("finiteMaterials",!p.hasInfiniteMaterials());r.addProperty("notSpectator",!p.isSpectator());r.addProperty("adultPiglin",!c.piglin.isBaby());r.addProperty("itemTagMatched",c.tag);r.addProperty("armorEmptyBefore",true);r.addProperty("armorEmptyAfter",armorEmpty(p));r.addProperty("packetSent",true);r.addProperty("criterionBefore",false);r.addProperty("criterionAfter",done(c));r.addProperty("noDirectCriterionTrigger",true);r.addProperty("noManualAward",true);r.addProperty("playerItemBefore",c.before);r.addProperty("playerItemAfter",p.getMainHandItem().getCount());r.addProperty("piglinItemBefore",c.pigBefore);r.addProperty("piglinItemAfter",c.piglin.getOffhandItem().getCount());r.addProperty("admiringItem",admiring);
        c.piglin.discard();var clean=cleanup(c.joined);JsonObject cleanup=new JsonObject();cleanup.addProperty("fixtureEntitiesRemoved",!c.piglin.isAlive());cleanup.addProperty("playerRemoved",clean.playerRemoved());cleanup.addProperty("connectionRemoved",clean.connectionRemoved());cleanup.addProperty("channelSettled",clean.channelSettled());cleanup.addProperty("settlementMessages",clean.messages());cleanup.addProperty("warningCount",0);r.add("cleanup",cleanup);
        var root=PhaseAMixedPiglinDistractionExecutionEvidence.root();PhaseAMixedPiglinDistractionExecutionEvidence.append(root,r);PhaseAMixedPiglinDistractionEvidenceValidation.load(root,PhaseAMixedPiglinDistractionEvidenceValidation.TEMP,exact,true);System.out.println((exact?"TEMP_PROMOTABLE":"TEMP_DIAGNOSTIC")+"=PASS family=MIXED_PIGLIN_DISTRACTION runId="+c.run+" entries=1");h.succeed();
    }
    private static boolean armorEmpty(ServerPlayer p){for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET})if(!p.getItemBySlot(slot).isEmpty())return false;return true;}
    private static boolean done(Context c){var criterion=c.joined.player().getAdvancements().getOrStartProgress(c.advancement).getCriterion(PhaseAMixedPiglinDistractionCertification.CRITERION);require(criterion!=null,"Criterion missing");return criterion.isDone();}
    private static void fail(GameTestHelper h,Context c,Throwable e){if(c.piglin!=null)c.piglin.discard();if(c.joined!=null)cleanup(c.joined);h.fail("Piglin native interaction: "+e);}
    private static void acknowledge(Joined joined){var channel=joined.channel();channel.runPendingTasks();channel.runScheduledPendingTasks();channel.flushOutbound();Integer id=null;Object packet;while((packet=channel.readOutbound())!=null){if(packet instanceof ClientboundPlayerPositionPacket position)id=position.id();ReferenceCountUtil.release(packet);}require(id!=null,"missing teleport acknowledgement");new ServerboundAcceptTeleportationPacket(id).handle(joined.player().connection);}
    private static Joined join(GameTestHelper h){MinecraftServer server=h.getLevel().getServer();UUID id=UUID.randomUUID();GameProfile profile=new GameProfile(id,"pig"+id.toString().substring(0,8));ServerPlayer player=new ServerPlayer(server,h.getLevel(),profile,ClientInformation.createDefault());Connection connection=new Connection(PacketFlow.SERVERBOUND);EmbeddedChannel channel=new EmbeddedChannel(connection);server.getConnection().getConnections().add(connection);server.getPlayerList().placeNewPlayer(connection,player,CommonListenerCookie.createInitial(profile,false));return new Joined(player,connection,channel,new AtomicBoolean());}
    private static Cleanup cleanup(Joined joined){if(!joined.cleaned().compareAndSet(false,true))return new Cleanup(true,true,true,0);MinecraftServer server=joined.player().level().getServer();if(joined.player().containerMenu!=joined.player().inventoryMenu)joined.player().closeContainer();server.getPlayerList().remove(joined.player());boolean playerRemoved=server.getPlayerList().getPlayer(joined.player().getUUID())!=joined.player();server.getConnection().getConnections().remove(joined.connection());boolean connectionRemoved=!server.getConnection().getConnections().contains(joined.connection());int messages=settle(joined.channel());joined.connection().disconnect(Component.literal("piglin cleanup"));return new Cleanup(playerRemoved,connectionRemoved,true,messages);}
    private static int settle(EmbeddedChannel channel){if(!channel.isOpen())return 0;int released=0;for(int pass=0;pass<32;pass++){channel.runPendingTasks();channel.runScheduledPendingTasks();channel.flushOutbound();int current=0;Object outbound;while((outbound=channel.readOutbound())!=null){ReferenceCountUtil.release(outbound);current++;if(++released>4096)throw new IllegalStateException("channel cleanup message bound");}if(current==0&&!channel.hasPendingTasks())return released;}throw new IllegalStateException("channel did not quiesce");}
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException(why);}
    private static final class Context{String run;JsonObject row;Joined joined;Piglin piglin;AdvancementHolder advancement;int before,pigBefore;boolean tag;}
    private record Joined(ServerPlayer player,Connection connection,EmbeddedChannel channel,AtomicBoolean cleaned){}
    private record Cleanup(boolean playerRemoved,boolean connectionRemoved,boolean channelSettled,int messages){}
    @Override public void invokeTestMethod(GameTestHelper h,Method method)throws ReflectiveOperationException{h.setBlock(0,0,0,Blocks.AIR);method.invoke(this,h);}
}
