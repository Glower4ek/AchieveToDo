package com.diskree.achievetodo.certification;

import com.diskree.achievetodo.AchieveToDoMod;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.commands.FillBiomeCommand;
import net.minecraft.server.level.*;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static com.diskree.achievetodo.certification.Final19StaticContext.require;

/** Real registry biome fixtures, finite joined SURVIVAL players, native empty-hand note tuning. */
public final class Final19WorldgenGameTest implements CustomTestMethodInvoker {
    @GameTest(maxTicks=4000) public void worldgenCanary5(GameTestHelper h){run(h,false);}
    @GameTest(maxTicks=10000) public void worldgenExact36(GameTestHelper h){run(h,true);}
    private static void run(GameTestHelper h,boolean exact){try{System.out.println("FINAL19_NATIVE_RUN_START family=WORLDGEN_HOLDERSET_CONTEXT");var root=Final19WorldgenEvidence.root();String run=Final19WorldgenEvidence.begin(root,exact);next(h,Final19WorldgenEvidence.rows(root,exact),0,run,exact);}catch(Throwable e){h.fail("FINAL19 worldgen begin: "+e);}}
    private static void next(GameTestHelper h,List<JsonObject> rows,int index,String run,boolean exact){
        if(index==rows.size()){try{Final19WorldgenEvidence.complete(Final19WorldgenEvidence.root(),exact);System.out.println((exact?"TEMP_PROMOTABLE":"TEMP_DIAGNOSTIC")+"=PASS family=WORLDGEN_HOLDERSET_CONTEXT runId="+run+" entries="+rows.size());h.succeed();}catch(Throwable e){h.fail("FINAL19 TEMP audit: "+e);}return;}
        Context c=new Context(rows.get(index));
        try{
            c.level=h.getLevel().getServer().getLevel(ResourceKey.create(Registries.DIMENSION,Identifier.parse(c.row.get("selectedDimension").getAsString())));require(c.level!=null,"Dimension absent");
            BlockPos base=h.absolutePos(new BlockPos(5,2,5));c.pos=c.level==h.getLevel()?new BlockPos(base.getX(),80,base.getZ()):new BlockPos(8,80,8);
            for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++){c.level.getChunk((c.pos.getX()+x)>>4,(c.pos.getZ()+z)>>4);saveBlock(c,c.pos.offset(x,-1,z),Blocks.STONE.defaultBlockState());for(int y=0;y<4;y++)saveBlock(c,c.pos.offset(x,y,z),Blocks.AIR.defaultBlockState());}
            for(int x=(c.pos.getX()-4)&~3;x<=((c.pos.getX()+4)&~3);x+=4)for(int z=(c.pos.getZ()-4)&~3;z<=((c.pos.getZ()+4)&~3);z+=4)for(int y=(c.pos.getY()-4)&~3;y<=((c.pos.getY()+4)&~3);y+=4){BlockPos p=new BlockPos(x,y,z);c.biomes.put(p,c.level.getBiome(p));}
            fill(c,"minecraft:the_void");c.level.setBlockAndUpdate(c.pos,Blocks.NOTE_BLOCK.defaultBlockState());
            c.joined=join(h);var player=c.joined.player();player.setGameMode(GameType.SURVIVAL);new ServerboundPlayerLoadedPacket().handle(player.connection);
            require(player.teleportTo(c.level,c.pos.getX()+.5,c.pos.getY()+1,c.pos.getZ()+1.5,Set.of(),0,0,true),"Teleport failed");acknowledge(c.joined);player.getInventory().clearContent();
            var settings=h.getLevel().getServer().getScoreboard().getObjective("bac_settings");require(settings!=null,"BACAP settings missing");h.getLevel().getServer().getScoreboard().getOrCreatePlayerScore(net.minecraft.world.scores.ScoreHolder.forNameOnly("reward"),settings).set(0);
            c.advancement=h.getLevel().getServer().getAdvancements().get(Identifier.parse(c.row.get("advancementId").getAsString()));require(c.advancement!=null&&!done(c),"Precompleted/missing criterion");
            require(!AchieveToDoMod.isTargetInLockedLandmark(player,c.level,c.pos),"Locked landmark");
            h.runAfterDelay(1,()->act(h,rows,index,run,exact,c));
        }catch(Throwable e){fail(h,c,e);}
    }
    private static void act(GameTestHelper h,List<JsonObject> rows,int index,String run,boolean exact,Context c){
        try{
            var player=c.joined.player();boolean before=done(c);int wrongNote=c.level.getBlockState(c.pos).getValue(NoteBlock.NOTE);var wrongBiomeResult=use(c);
            boolean wrongBiomeNegative=wrongBiomeResult.consumesAction()&&c.level.getBlockState(c.pos).getValue(NoteBlock.NOTE)==(wrongNote+1)%25&&!done(c)&&c.level.getBiome(c.pos).unwrapKey().orElseThrow().identifier().equals(Identifier.parse("minecraft:the_void"));
            require(wrongBiomeNegative,"Wrong biome accepted or native control did not tune");
            fill(c,c.row.get("selectedBiome").getAsString());c.level.setBlockAndUpdate(c.pos,Blocks.STONE.defaultBlockState());use(c);boolean wrongBlockNegative=!done(c)&&c.level.getBlockState(c.pos).is(Blocks.STONE);require(wrongBlockNegative,"Wrong block accepted");
            c.level.setBlockAndUpdate(c.pos,Blocks.NOTE_BLOCK.defaultBlockState());
            String actualBiome=c.level.getBiome(c.pos).unwrapKey().orElseThrow().identifier().toString();String actualDimension=player.level().dimension().identifier().toString();String actualBlock=BuiltInRegistries.BLOCK.getKey(c.level.getBlockState(c.pos).getBlock()).toString();
            int noteBefore=c.level.getBlockState(c.pos).getValue(NoteBlock.NOTE);boolean empty=player.getMainHandItem().isEmpty();
            require(!done(c)&&empty,"Bad pre-action state");var result=use(c);int noteAfter=c.level.getBlockState(c.pos).getValue(NoteBlock.NOTE);boolean after=done(c);
            require(result.consumesAction()&&after&&noteAfter==(noteBefore+1)%25,"Native note use failed "+c.row.get("criterion")+" biome="+actualBiome+" result="+result);
            var progress=player.getAdvancements().getOrStartProgress(c.advancement);boolean groupSatisfied=false;for(var alternative:c.row.getAsJsonArray("alternatives")){var criterion=progress.getCriterion(alternative.getAsString());groupSatisfied|=criterion!=null&&criterion.isDone();}
            JsonObject r=new JsonObject();r.addProperty("advancementId",c.advancement.id().toString());r.addProperty("criterion",c.row.get("criterion").getAsString());r.addProperty("requirementGroup",c.row.get("requirementGroup").getAsInt());r.addProperty("runId",run);r.addProperty("playerUuid",player.getUUID().toString());
            r.addProperty("trigger","minecraft:default_block_use");r.addProperty("nativeBoundary","ServerPlayerGameMode.useItemOn->NoteBlock.useWithoutItem->DEFAULT_BLOCK_USE");r.addProperty("observedBiome",actualBiome);r.addProperty("observedDimension",actualDimension);r.addProperty("observedBlock",actualBlock);r.addProperty("noteBefore",noteBefore);r.addProperty("noteAfter",noteAfter);r.addProperty("emptyHand",empty);r.addProperty("criterionBefore",before);r.addProperty("criterionAfter",after);r.addProperty("groupSatisfied",groupSatisfied);r.addProperty("nativeUseConsumed",result.consumesAction());r.addProperty("wrongBiomeNegative",wrongBiomeNegative);r.addProperty("wrongBlockNegative",wrongBlockNegative);
            r.addProperty("joined",h.getLevel().getServer().getPlayerList().getPlayer(player.getUUID())==player);r.addProperty("connectionRegistered",h.getLevel().getServer().getConnection().getConnections().contains(c.joined.connection()));r.addProperty("clientLoaded",player.connection.hasClientLoaded());r.addProperty("finiteMaterials",!player.hasInfiniteMaterials());r.addProperty("gameMode",player.gameMode().name());r.addProperty("noDirectTrigger",true);r.addProperty("noManualAward",true);r.add("cleanup",cleanup(c));
            Final19WorldgenEvidence.append(Final19WorldgenEvidence.root(),r);h.runAfterDelay(1,()->next(h,rows,index+1,run,exact));
        }catch(Throwable e){fail(h,c,e);}
    }
    private static net.minecraft.world.InteractionResult use(Context c){return c.joined.player().gameMode.useItemOn(c.joined.player(),c.level,c.joined.player().getMainHandItem(),InteractionHand.MAIN_HAND,new BlockHitResult(new Vec3(c.pos.getX()+.5,c.pos.getY()+.5,c.pos.getZ()+.5),Direction.UP,c.pos,false));}
    private static boolean done(Context c){var criterion=c.joined.player().getAdvancements().getOrStartProgress(c.advancement).getCriterion(c.row.get("criterion").getAsString());require(criterion!=null,"Criterion missing");return criterion.isDone();}
    private static void fill(Context c,String id){var holder=c.level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(ResourceKey.create(Registries.BIOME,Identifier.parse(id)));require(FillBiomeCommand.fill(c.level,c.pos.offset(-4,-4,-4),c.pos.offset(4,4,4),holder).right().isEmpty(),"Native registered biome fixture failed");}
    private static void saveBlock(Context c,BlockPos p,BlockState state){c.blocks.putIfAbsent(p,c.level.getBlockState(p));c.level.setBlockAndUpdate(p,state);}
    private static JsonObject cleanup(Context c){
        JsonObject clean=new JsonObject();boolean biomes=true,blocks=true;for(var entry:c.biomes.entrySet()){require(FillBiomeCommand.fill(c.level,entry.getKey(),entry.getKey(),entry.getValue()).right().isEmpty(),"Biome restoration failed");biomes&=c.level.getBiome(entry.getKey()).equals(entry.getValue());}c.biomes.clear();for(var entry:c.blocks.entrySet()){c.level.setBlockAndUpdate(entry.getKey(),entry.getValue());blocks&=c.level.getBlockState(entry.getKey()).equals(entry.getValue());}c.blocks.clear();clean.addProperty("biomeRestored",biomes);clean.addProperty("blocksRestored",blocks);
        if(c.joined!=null&&c.joined.cleaned().compareAndSet(false,true)){var server=c.joined.player().level().getServer();server.getPlayerList().remove(c.joined.player());server.getConnection().getConnections().remove(c.joined.connection());clean.addProperty("playerRemoved",server.getPlayerList().getPlayer(c.joined.player().getUUID())!=c.joined.player());clean.addProperty("connectionRemoved",!server.getConnection().getConnections().contains(c.joined.connection()));int settled=settle(c.joined.channel());clean.addProperty("settlementMessages",settled);c.joined.connection().disconnect(Component.literal("FINAL19 cleanup"));clean.addProperty("channelSettled",!c.joined.channel().hasPendingTasks());}return clean;
    }
    private static void fail(GameTestHelper h,Context c,Throwable error){try{cleanup(c);}catch(Throwable cleanup){error.addSuppressed(cleanup);}h.fail("FINAL19 worldgen "+c.row.get("criterion")+": "+error);}
    private static Joined join(GameTestHelper h){MinecraftServer server=h.getLevel().getServer();UUID id=UUID.randomUUID();GameProfile profile=new GameProfile(id,"f19"+id.toString().substring(0,8));var player=new ServerPlayer(server,h.getLevel(),profile,ClientInformation.createDefault());var connection=new Connection(PacketFlow.SERVERBOUND);var channel=new EmbeddedChannel(connection);server.getConnection().getConnections().add(connection);server.getPlayerList().placeNewPlayer(connection,player,CommonListenerCookie.createInitial(profile,false));return new Joined(player,connection,channel,new AtomicBoolean());}
    private static void acknowledge(Joined joined){var channel=joined.channel();channel.runPendingTasks();channel.runScheduledPendingTasks();channel.flushOutbound();Integer id=null;Object packet;while((packet=channel.readOutbound())!=null){if(packet instanceof ClientboundPlayerPositionPacket position)id=position.id();ReferenceCountUtil.release(packet);}require(id!=null,"Missing teleport acknowledgment");new ServerboundAcceptTeleportationPacket(id).handle(joined.player().connection);}
    private static int settle(EmbeddedChannel channel){int released=0;for(int pass=0;pass<32;pass++){channel.runPendingTasks();channel.runScheduledPendingTasks();channel.flushOutbound();int current=0;Object packet;while((packet=channel.readOutbound())!=null){ReferenceCountUtil.release(packet);current++;require(++released<=4096,"Cleanup message bound");}if(current==0&&!channel.hasPendingTasks())return released;}throw new IllegalStateException("Channel failed to settle");}
    private static final class Context {final JsonObject row;ServerLevel level;BlockPos pos;Joined joined;AdvancementHolder advancement;final Map<BlockPos,Holder<Biome>> biomes=new LinkedHashMap<>();final Map<BlockPos,BlockState> blocks=new LinkedHashMap<>();Context(JsonObject row){this.row=row;}}
    private record Joined(ServerPlayer player,Connection connection,EmbeddedChannel channel,AtomicBoolean cleaned){}
    @Override public void invokeTestMethod(GameTestHelper h,Method method)throws ReflectiveOperationException{h.setBlock(0,0,0,Blocks.AIR);method.invoke(this,h);}
}
