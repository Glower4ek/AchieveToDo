package com.diskree.achievetodo.certification;
import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.injection.extension.main.LevelInfoExtension;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.*;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.ScoreHolder;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static com.diskree.achievetodo.certification.Final19StaticContext.require;

/** Shared accepted joined-player lifecycle; no trigger abstraction or progress mutation. */
public final class Final19NativeActors {
    public record Actor(ServerPlayer player,Connection connection,EmbeddedChannel channel,AtomicBoolean cleaned){}
    public static Actor join(ServerLevel level,BlockPos pos)throws Exception {
        var server=level.getServer();UUID id=UUID.randomUUID();GameProfile profile=new GameProfile(id,"f19"+id.toString().substring(0,8));var player=new ServerPlayer(server,level,profile,ClientInformation.createDefault());var connection=new Connection(PacketFlow.SERVERBOUND);var channel=new EmbeddedChannel(connection);server.getConnection().getConnections().add(connection);server.getPlayerList().placeNewPlayer(connection,player,CommonListenerCookie.createInitial(profile,false));var actor=new Actor(player,connection,channel,new AtomicBoolean());
        try{player.setGameMode(GameType.SURVIVAL);new ServerboundPlayerLoadedPacket().handle(player.connection);require(player.teleportTo(level,pos.getX()+.5,pos.getY(),pos.getZ()+.5,Set.of(),0,0,true),"Player teleport failed");acknowledge(actor);player.getInventory().clearContent();var settings=server.getScoreboard().getObjective("bac_settings");require(settings!=null,"BACAP settings missing");server.getScoreboard().getOrCreatePlayerScore(ScoreHolder.forNameOnly("reward"),settings).set(0);return actor;}catch(Throwable e){close(actor);throw e;}
    }
    public static AdvancementHolder advancement(Actor actor,JsonObject row){var holder=actor.player().level().getServer().getAdvancements().get(Identifier.parse(row.get("advancementId").getAsString()));require(holder!=null,"Advancement missing");return holder;}
    public static boolean done(Actor actor,AdvancementHolder holder,String criterion){var progress=actor.player().getAdvancements().getOrStartProgress(holder).getCriterion(criterion);require(progress!=null,"Criterion missing");return progress.isDone();}
    public static JsonObject receipt(Actor actor,AdvancementHolder holder,JsonObject row,String run,boolean before){
        var player=actor.player();var server=player.level().getServer();JsonObject r=new JsonObject();r.addProperty("advancementId",holder.id().toString());r.addProperty("criterion",row.get("criterion").getAsString());r.addProperty("requirementGroup",row.get("requirementGroup").getAsInt());r.addProperty("runId",run);r.addProperty("playerUuid",player.getUUID().toString());r.addProperty("gameMode",player.gameMode().name());r.addProperty("joined",server.getPlayerList().getPlayer(player.getUUID())==player);r.addProperty("connectionRegistered",server.getConnection().getConnections().contains(actor.connection()));r.addProperty("clientLoaded",player.connection.hasClientLoaded());r.addProperty("finiteMaterials",!player.hasInfiniteMaterials());r.addProperty("criterionBefore",before);r.addProperty("criterionAfter",done(actor,holder,row.get("criterion").getAsString()));boolean group=false;for(var alternative:row.getAsJsonArray("alternatives"))group|=done(actor,holder,alternative.getAsString());r.addProperty("groupSatisfied",group);r.addProperty("noDirectTrigger",true);r.addProperty("noManualAward",true);return r;
    }
    public static JsonObject unlock(Actor actor,AbilityType... abilities){
        var player=actor.player();var server=player.level().getServer();var board=server.getScoreboard();var objective=board.getObjective("bac_advancements");require(objective!=null,"Live advancement objective missing");var config=((LevelInfoExtension)server.getWorldData().getLevelSettings()).achievetodo$getAbilitiesConfiguration(server.overworld().getSeed());JsonObject witness=new JsonObject(),gates=new JsonObject();witness.addProperty("thresholdSource","LIVE_OVERWORLD_SEED_CONFIGURATION");int original=board.getOrCreatePlayerScore(player,objective).get();witness.addProperty("nativeScoreBeforeFixture",original);board.getOrCreatePlayerScore(player,objective).set(0);int needed=0;
        for(var ability:abilities){int threshold=config.get(ability);boolean locked=AchieveToDoMod.isAbilityLocked(player,ability,true);require(threshold>0&&locked,"Fresh ability gate missing "+ability);needed=Math.max(needed,threshold);var gate=new JsonObject();gate.addProperty("threshold",threshold);gate.addProperty("lockedBefore",locked);gates.add(ability.name(),gate);}
        board.getOrCreatePlayerScore(player,objective).set(needed);for(var e:gates.entrySet()){boolean locked=AchieveToDoMod.isAbilityLocked(player,AbilityType.valueOf(e.getKey()),true);require(!locked,"Gate did not unlock");e.getValue().getAsJsonObject().addProperty("lockedAfter",locked);}witness.addProperty("scoreAfter",board.getPlayerScoreInfo(player,objective).value());witness.add("abilities",gates);return witness;
    }
    public static JsonObject close(Actor actor){JsonObject clean=new JsonObject();if(!actor.cleaned().compareAndSet(false,true))return clean;var server=actor.player().level().getServer();if(actor.player().containerMenu!=actor.player().inventoryMenu)actor.player().closeContainer();server.getPlayerList().remove(actor.player());server.getConnection().getConnections().remove(actor.connection());clean.addProperty("playerRemoved",server.getPlayerList().getPlayer(actor.player().getUUID())!=actor.player());clean.addProperty("connectionRemoved",!server.getConnection().getConnections().contains(actor.connection()));int count=settle(actor.channel());actor.connection().disconnect(Component.literal("FINAL19 owned actor cleanup"));clean.addProperty("channelSettled",!actor.channel().hasPendingTasks());clean.addProperty("settlementMessages",count);return clean;}
    public static void acknowledge(Actor actor){var channel=actor.channel();channel.runPendingTasks();channel.runScheduledPendingTasks();channel.flushOutbound();Integer id=null;Object packet;while((packet=channel.readOutbound())!=null){if(packet instanceof ClientboundPlayerPositionPacket position)id=position.id();ReferenceCountUtil.release(packet);}require(id!=null,"Missing teleport acknowledgement");new ServerboundAcceptTeleportationPacket(id).handle(actor.player().connection);}
    private static int settle(EmbeddedChannel channel){int count=0;for(int pass=0;pass<32;pass++){channel.runPendingTasks();channel.runScheduledPendingTasks();channel.flushOutbound();int current=0;Object packet;while((packet=channel.readOutbound())!=null){ReferenceCountUtil.release(packet);current++;require(++count<4096,"Channel message bound");}if(current==0&&!channel.hasPendingTasks())return count;}throw new IllegalStateException("Channel cleanup did not settle");}
}
