package com.diskree.achievetodo.certification;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import net.fabricmc.fabric.api.gametest.v1.*;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** One finite native pickup for each remaining frozen requirement group, under one coordinator. */
public final class PhaseAStackAllItemsInventoryBacklogGameTest implements CustomTestMethodInvoker {
    private static final BlockPos TARGET=new BlockPos(5,2,5);
    @GameTest(maxTicks=30000)public void stackAllItemsInventoryBacklogCanary(GameTestHelper h){run(h,false);}
    @GameTest(maxTicks=30000)public void stackAllItemsInventoryBacklogExact319(GameTestHelper h){run(h,true);}
    private static void run(GameTestHelper h,boolean exact){try{
        String run=PhaseAStackAllItemsExecutionEvidence.begin(PhaseAStackAllItemsExecutionEvidence.root(),exact);runCase(h,PhaseAStackAllItemsExecutionEvidence.selected(),0,run,exact);
    }catch(Throwable e){h.fail("Stack backlog start: "+e);}}
    private static void runCase(GameTestHelper h,List<JsonObject> cases,int index,String run,boolean exact){
        if(index==cases.size()){try{PhaseAStackAllItemsEvidenceValidation.load(PhaseAStackAllItemsExecutionEvidence.root(),PhaseAStackAllItemsEvidenceValidation.TEMP,exact,true);System.out.println((exact?"TEMP_PROMOTABLE":"TEMP_DIAGNOSTIC")+"=PASS family=RUNTIME_PARTIAL_INVENTORY_BACKLOG runId="+run+" entries="+cases.size());h.succeed();}catch(Throwable e){h.fail("Stack backlog exact-set audit: "+e);}return;}
        Context c=new Context(cases.get(index));try{
            var level=h.getLevel();var pos=h.absolutePos(TARGET);for(int y=-1;y<3;y++){var p=pos.above(y);c.original.put(p,level.getBlockState(p));level.setBlockAndUpdate(p,y==-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState());}
            c.joined=join(h);var p=c.joined.player();p.setGameMode(GameType.SURVIVAL);new ServerboundPlayerLoadedPacket().handle(p.connection);require(p.connection.hasClientLoaded()&&!p.hasInfiniteMaterials()&&!p.isSpectator(),"Joined finite lifecycle missing");p.teleportTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5);p.getInventory().clearContent();
            var board=level.getServer().getScoreboard();var settings=board.getObjective("bac_settings");require(settings!=null,"Reward settings missing");board.getOrCreatePlayerScore(net.minecraft.world.scores.ScoreHolder.forNameOnly("reward"),settings).set(0);
            c.advancement=level.getServer().getAdvancements().get(Identifier.parse(c.row.get("advancementId").getAsString()));require(c.advancement!=null&&!done(c),"Target missing or precompleted");c.before=done(c);
            var item=BuiltInRegistries.ITEM.getValue(Identifier.parse(c.row.get("selectedItem").getAsString()));require(item!=null&&BuiltInRegistries.ITEM.getKey(item).toString().equals(c.row.get("selectedItem").getAsString()),"Literal item lookup mismatch");c.stack=new ItemStack(item,c.row.get("requiredCount").getAsInt());require(c.stack.getCount()>0&&c.stack.getCount()<=c.stack.getMaxStackSize(),"Frozen count exceeds native stack limit");c.inventoryBefore=count(p,c.stack);require(c.inventoryBefore==0,"Pickup fixture inventory is not empty");
            c.entity=new ItemEntity(level,pos.getX()+.5,pos.getY()+.1,pos.getZ()+.5,c.stack.copy());c.entity.setPickUpDelay(0);c.entityBefore=c.entity.getItem().getCount();require(level.addFreshEntity(c.entity),"Native item fixture spawn failed");h.runAfterDelay(1,()->poll(h,cases,index,run,exact,c,1));
        }catch(Throwable e){fail(h,c,"setup",e);}
    }
    private static void poll(GameTestHelper h,List<JsonObject> cases,int index,String run,boolean exact,Context c,int ticks){try{
        ItemStack live=ItemStack.EMPTY;var p=c.joined.player();for(int i=0;i<p.getInventory().getContainerSize();i++){var candidate=p.getInventory().getItem(i);if(ItemStack.isSameItemSameComponents(candidate,c.stack)){live=candidate;break;}}
        if(done(c)&&(!c.entity.isAlive()||c.entity.getItem().isEmpty())&&!live.isEmpty()){finish(h,cases,index,run,exact,c,live,ticks);return;}require(ticks<80,"Native inventory pickup timed out: "+PhaseAStackAllItemsCertification.key(c.row)+" count="+count(p,c.stack)+" criterion="+done(c));h.runAfterDelay(1,()->poll(h,cases,index,run,exact,c,ticks+1));
    }catch(Throwable e){fail(h,c,"native pickup",e);}}
    private static void finish(GameTestHelper h,List<JsonObject> cases,int index,String run,boolean exact,Context c,ItemStack live,int ticks)throws Exception{
        var p=c.joined.player();require(ItemStack.isSameItemSameComponents(live,c.stack)&&live.getCount()==c.stack.getCount(),"Observed native stack mismatch");var r=new JsonObject();for(String field:List.of("advancementId","criterion","requirementGroup","selectedItem","requiredCount","trigger","boundary"))r.add(field,c.row.get(field).deepCopy());
        r.addProperty("family",PhaseAStackAllItemsCertification.FAMILY);r.addProperty("source",PhaseAStackAllItemsCertification.SOURCE);r.addProperty("runId",run);r.addProperty("catalogFingerprint",PhaseAStackAllItemsEvidenceValidation.fingerprint(PhaseAStackAllItemsExecutionEvidence.root()));r.addProperty("result","GREEN");r.addProperty("gameMode",p.gameMode().name());r.addProperty("playerUuid",p.getUUID().toString());r.addProperty("itemEntityUuid",c.entity.getUUID().toString());r.addProperty("joined",h.getLevel().getServer().getPlayerList().getPlayer(p.getUUID())==p);r.addProperty("connectionRegistered",h.getLevel().getServer().getConnection().getConnections().contains(c.joined.connection()));r.addProperty("clientLoaded",p.connection.hasClientLoaded());r.addProperty("finiteMaterials",!p.hasInfiniteMaterials());r.addProperty("notSpectator",!p.isSpectator());r.addProperty("normalScheduler",Thread.currentThread()==h.getLevel().getServer().getRunningThread());
        r.addProperty("observedItem",BuiltInRegistries.ITEM.getKey(live.getItem()).toString());r.addProperty("observedCount",live.getCount());r.addProperty("maxStackSize",live.getMaxStackSize());r.addProperty("inventoryCountBefore",c.inventoryBefore);r.addProperty("inventoryCountAfter",count(p,c.stack));r.addProperty("pickedUpCount",count(p,c.stack)-c.inventoryBefore);r.addProperty("itemEntityCountBefore",c.entityBefore);r.addProperty("matchingStackAfter",ItemStack.isSameItemSameComponents(live,c.stack));r.addProperty("itemEntityConsumed",!c.entity.isAlive()||c.entity.getItem().isEmpty());r.addProperty("criterionBefore",c.before);r.addProperty("criterionAfter",done(c));r.addProperty("ticksToComplete",ticks);r.addProperty("noDirectCriterionTrigger",true);r.addProperty("noManualAward",true);
        c.entity.discard();var lifecycle=cleanup(c.joined);restore(h,c);var clean=new JsonObject();clean.addProperty("playerRemoved",lifecycle.playerRemoved());clean.addProperty("connectionRemoved",lifecycle.connectionRemoved());clean.addProperty("channelSettled",lifecycle.channelSettled());clean.addProperty("itemEntityRemoved",!c.entity.isAlive());clean.addProperty("worldFixtureRestored",c.original.entrySet().stream().allMatch(e->h.getLevel().getBlockState(e.getKey()).equals(e.getValue())));clean.addProperty("warningCount",0);clean.addProperty("settlementMessages",lifecycle.messages());r.add("cleanup",clean);PhaseAStackAllItemsExecutionEvidence.append(PhaseAStackAllItemsExecutionEvidence.root(),r);h.runAfterDelay(1,()->runCase(h,cases,index+1,run,exact));
    }
    private static int count(ServerPlayer p,ItemStack expected){int count=0;for(int i=0;i<p.getInventory().getContainerSize();i++){var stack=p.getInventory().getItem(i);if(ItemStack.isSameItemSameComponents(stack,expected))count+=stack.getCount();}return count;}
    private static boolean done(Context c){var criterion=c.joined.player().getAdvancements().getOrStartProgress(c.advancement).getCriterion(c.row.get("criterion").getAsString());require(criterion!=null,"Live criterion missing");return criterion.isDone();}
    private static void restore(GameTestHelper h,Context c){for(var e:c.original.entrySet())h.getLevel().setBlockAndUpdate(e.getKey(),e.getValue());}
    private static void fail(GameTestHelper h,Context c,String stage,Throwable e){if(c.entity!=null)c.entity.discard();if(c.joined!=null)cleanup(c.joined);restore(h,c);h.fail("Stack backlog "+stage+" "+PhaseAStackAllItemsCertification.key(c.row)+": "+e);}
    private static Joined join(GameTestHelper h){MinecraftServer server=h.getLevel().getServer();UUID id=UUID.randomUUID();GameProfile profile=new GameProfile(id,"stack"+id.toString().substring(0,8));ServerPlayer player=new ServerPlayer(server,h.getLevel(),profile,ClientInformation.createDefault());Connection connection=new Connection(PacketFlow.SERVERBOUND);EmbeddedChannel channel=new EmbeddedChannel(connection);server.getConnection().getConnections().add(connection);server.getPlayerList().placeNewPlayer(connection,player,CommonListenerCookie.createInitial(profile,false));return new Joined(player,connection,channel,new AtomicBoolean());}
    private static Cleanup cleanup(Joined joined){if(!joined.cleaned().compareAndSet(false,true))return new Cleanup(true,true,true,0);MinecraftServer server=joined.player().level().getServer();if(joined.player().containerMenu!=joined.player().inventoryMenu)joined.player().closeContainer();server.getPlayerList().remove(joined.player());boolean playerRemoved=server.getPlayerList().getPlayer(joined.player().getUUID())!=joined.player();server.getConnection().getConnections().remove(joined.connection());boolean connectionRemoved=!server.getConnection().getConnections().contains(joined.connection());int messages=settle(joined.channel());joined.connection().disconnect(Component.literal("stack backlog cleanup"));return new Cleanup(playerRemoved,connectionRemoved,true,messages);}
    private static int settle(EmbeddedChannel channel){if(!channel.isOpen())return 0;int released=0;for(int pass=0;pass<32;pass++){channel.runPendingTasks();channel.runScheduledPendingTasks();channel.flushOutbound();int current=0;Object outbound;while((outbound=channel.readOutbound())!=null){ReferenceCountUtil.release(outbound);current++;if(++released>4096)throw new IllegalStateException("channel cleanup message bound");}if(current==0&&!channel.hasPendingTasks())return released;}throw new IllegalStateException("channel did not quiesce");}
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException(message);}
    private static final class Context{final JsonObject row;Joined joined;AdvancementHolder advancement;ItemStack stack;ItemEntity entity;boolean before;int inventoryBefore,entityBefore;Map<BlockPos,BlockState> original=new LinkedHashMap<>();Context(JsonObject row){this.row=row;}}
    private record Joined(ServerPlayer player,Connection connection,EmbeddedChannel channel,AtomicBoolean cleaned){}
    private record Cleanup(boolean playerRemoved,boolean connectionRemoved,boolean channelSettled,int messages){}
    @Override public void invokeTestMethod(GameTestHelper h,Method method)throws ReflectiveOperationException{h.setBlock(0,0,0,Blocks.AIR);method.invoke(this,h);}
}
