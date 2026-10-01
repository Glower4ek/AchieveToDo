package com.diskree.achievetodo.certification;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.client.LegacyChatText;
import com.diskree.achievetodo.server.AchieveToDoServer;
import com.mojang.authlib.GameProfile;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.scores.*;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import java.util.*;
import java.nio.file.*;

/** New smoke evidence only. No historical evidence writer or manual advancement award. */
public final class Pre26SmokeGameTest implements CustomTestMethodInvoker {
    @Override public void invokeTestMethod(GameTestHelper helper, java.lang.reflect.Method method) throws ReflectiveOperationException { method.invoke(this, helper); }
    private static final String[] TARGETS = {"achievetodo-test:vanilla_task", "minecraft:story/mine_stone", "blazeandcave:mining/moar_tools", "blazeandcave:mining/coal_miner", "blazeandcave:biomes/flower_power", "blazeandcave:biomes/for_you_my_sweet"};
    private static final String[] FLOWERS = {"dandelion","poppy","blue_orchid","allium","azure_bluet","red_tulip","orange_tulip","white_tulip","pink_tulip","oxeye_daisy","sunflower","lilac","rose_bush","peony","cornflower","lily_of_the_valley","wither_rose","torchflower","pitcher_plant","closed_eyeblossom","open_eyeblossom"};

    @GameTest(maxTicks = 400)
    public void nativeSmoke(GameTestHelper h) {
        Context c = new Context();
        try {
            var server = h.getLevel().getServer();
            require(!AchieveToDoMod.getServer().isNotReady(), "post-#load abilities not ready");
            scoreboardLifecycle(h);
            UUID uuid = UUID.randomUUID(); var profile = new GameProfile(uuid, "smoke" + uuid.toString().substring(0,8));
            c.player = new ServerPlayer(server, h.getLevel(), profile, ClientInformation.createDefault());
            c.connection = new Connection(PacketFlow.SERVERBOUND); c.channel = new EmbeddedChannel(c.connection);
            server.getConnection().getConnections().add(c.connection);
            server.getPlayerList().placeNewPlayer(c.connection,c.player,CommonListenerCookie.createInitial(profile,false));
            c.player.setGameMode(GameType.SURVIVAL);
            new ServerboundPlayerLoadedPacket().handle(c.player.connection);
            require(c.player.connection.hasClientLoaded() && !c.player.hasInfiniteMaterials(), "finite joined lifecycle missing");
            c.pos = h.absolutePos(new BlockPos(1,1,1));
            h.getLevel().setBlockAndUpdate(c.pos.below(),Blocks.STONE.defaultBlockState());
            c.player.teleportTo(c.pos.getX()+.5,c.pos.getY(),c.pos.getZ()+.5);
            drain(c, true);
            c.player.getInventory().clearContent();
            var settings=server.getScoreboard().getObjective("bac_settings"); require(settings!=null,"missing BACAP settings");
            for (String key:List.of("task","goal","challenge","trophy")) server.getScoreboard().getOrCreatePlayerScore(ScoreHolder.forNameOnly(key),settings).set(1);
            for (String key:List.of("reward","exp","coop")) server.getScoreboard().getOrCreatePlayerScore(ScoreHolder.forNameOnly(key),settings).set(0);
            for (String id:TARGETS) require(!done(c,id), "pre-complete " + id);
            spawn(h,c,"cobblestone",1);
            h.runAfterDelay(1,()->poll(h,c,0,0));
        } catch(Throwable t) { fail(h,c,t); }
    }

    private static void scoreboardLifecycle(GameTestHelper h) throws Exception {
        var live=AchieveToDoMod.getServer(); var server=h.getLevel().getServer();
        var candidate=new AchieveToDoServer(); var scratch=new ServerScoreboard(server);
        try {
            candidate.prepareScoreboard(scratch); require(candidate.currentScoreboardObjective==null,"transient unexpectedly bound");
            var objective=scratch.addObjective("bac_advancements",ObjectiveCriteria.DUMMY,Component.literal("Advancements"),ObjectiveCriteria.RenderType.INTEGER,false,null);
            scratch.setDisplayObjective(DisplaySlot.SIDEBAR,objective);
            var field=AchieveToDoServer.class.getDeclaredField("abilitiesConfiguration");field.setAccessible(true);field.set(candidate,field.get(live));
            candidate.finishScoreboardInitialization(scratch);
            require(candidate.currentScoreboardObjective==objective && !candidate.isNotReady(),"transient->available did not bind with intact config");
            var existing=new AchieveToDoServer();field.set(existing,field.get(live));existing.prepareScoreboard(scratch);
            require(existing.currentScoreboardObjective==objective&&!existing.isNotReady(),"existing objective did not bind immediately");
        } finally { live.prepareScoreboard(server.getScoreboard()); }
    }

    private static void poll(GameTestHelper h, Context c, int stage, int ticks) {
        try {
            drain(c,false);
            boolean ready = switch(stage) {
                case 0 -> done(c,TARGETS[0]) && done(c,TARGETS[1]);
                case 1 -> done(c,TARGETS[2]); case 2 -> done(c,TARGETS[3]); default -> done(c,TARGETS[4]);
            };
            if (!ready) { require(ticks<80,"native pickup timeout stage="+stage); h.runAfterDelay(1,()->poll(h,c,stage,ticks+1)); return; }
            if (stage==0) { for(String tool:List.of("stone_pickaxe","stone_axe","stone_shovel","stone_hoe")) spawn(h,c,tool,1); }
            if (stage==1) spawn(h,c,"coal",64);
            if (stage==2) for(String flower:FLOWERS) spawn(h,c,flower,1);
            if (stage<3) { h.runAfterDelay(1,()->poll(h,c,stage+1,0)); return; }
            h.runAfterDelay(2,()->finish(h,c));
        } catch(Throwable t) { fail(h,c,t); }
    }

    private static void finish(GameTestHelper h, Context c) {
        try {
            drain(c,false); var receipts=new JsonArray();
            for (String id:TARGETS) {
                var advancement=h.getLevel().getServer().getAdvancements().get(Identifier.parse(id));
                var matches=c.messages.stream().filter(m->references(m,id)).toList();
                require(matches.size()==1,"earned message count="+matches.size()+" for "+id);
                require(hasHover(matches.getFirst()),"earned tooltip missing "+id);
                var receipt=new JsonObject();receipt.addProperty("id",id);receipt.addProperty("frame",advancement.value().display().orElseThrow().getType().toString());
                receipt.addProperty("nativePickup",true);receipt.addProperty("criterionBefore",false);receipt.addProperty("advancementAfter",done(c,id));receipt.add("message",ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE,matches.getFirst()).getOrThrow());receipts.add(receipt);
            }
            var flower=c.player.getInventory().getNonEquipmentItems().stream().filter(s->s.get(DataComponents.CUSTOM_NAME)!=null && s.get(DataComponents.CUSTOM_NAME).getContents() instanceof TranslatableContents t && t.getKey().equals("A blessing in love")).findFirst().orElseThrow(()->new IllegalStateException("legitimate user-reported flower trophy missing"));
            require(flower.get(DataComponents.LORE).lines().size()==4 && flower.get(DataComponents.CUSTOM_NAME).getStyle().isBold(),"flower style/lore lost");
            var ops=net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE,h.getLevel().registryAccess());
            var reloaded=ItemStack.CODEC.parse(ops,ItemStack.CODEC.encodeStart(ops,flower).getOrThrow()).getOrThrow();require(reloaded.get(DataComponents.CUSTOM_NAME).equals(flower.get(DataComponents.CUSTOM_NAME)),"flower reload changed name");
            var reference=net.minecraft.advancements.Advancement.name(h.getLevel().getServer().getAdvancements().get(Identifier.parse(TARGETS[1])));
            require(hasHover(reference),"existing command/warning reference lost hover");
            h.getLevel().getServer().getCommands().performPrefixedCommand(h.getLevel().getServer().createCommandSourceStack().withEntity(c.player),"function blazeandcave:riddle/ninth_line");
            var paper=c.player.getInventory().getNonEquipmentItems().stream().filter(s->s.get(DataComponents.ITEM_NAME)!=null&&s.get(DataComponents.ITEM_NAME).getContents() instanceof TranslatableContents t&&t.getKey().equals("Tenth Parchment")).findFirst().orElseThrow(()->new IllegalStateException("native ITEM_NAME function reward missing"));
            require(paper.get(DataComponents.LORE).lines().size()==3&&paper.get(DataComponents.LORE).lines().getFirst().getContents() instanceof TranslatableContents,"native double-quoted lore function regression");
            var paperReloaded=ItemStack.CODEC.parse(ops,ItemStack.CODEC.encodeStart(ops,paper).getOrThrow()).getOrThrow();
            require(paperReloaded.get(DataComponents.ITEM_NAME).equals(paper.get(DataComponents.ITEM_NAME))&&paperReloaded.get(DataComponents.LORE).equals(paper.get(DataComponents.LORE)),"parchment reload changed item text");
            var result=new JsonObject();result.add("nativeAdvancements",receipts);result.addProperty("flowerRewardNative",true);result.addProperty("namespacedItemNameNativeFunction",true);result.addProperty("existingCommandWarningHover",true);result.addProperty("scoreboardTransientAndExisting",true);result.addProperty("certificationGain",0);
            cleanup(c);result.addProperty("cleanupGreen",true);
            Files.writeString(Path.of(System.getProperty("achievetodo.phaseA.projectRoot")).resolve("reference/phase_a_planning/pre26_2_smoke_bugfix_0_1_5_2/native_smoke.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result));
            h.succeed();
        } catch(Throwable t) { fail(h,c,t); }
    }

    private static boolean references(Component message,String id) {
        if (id.equals(TARGETS[0])) return message.getContents() instanceof TranslatableContents t && t.getKey().equals("chat.type.advancement.task");
        return ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE,message).getOrThrow().toString().contains("highlight "+id+" obtained_status");
    }
    private static boolean hasHover(Component message) {
        if(message.getStyle().getHoverEvent()!=null)return true;
        if(message.getContents() instanceof TranslatableContents t)for(Object a:t.getArgs())if(a instanceof Component child && hasHover(child))return true;
        return message.getSiblings().stream().anyMatch(Pre26SmokeGameTest::hasHover);
    }
    private static boolean done(Context c,String id) {var a=c.player.level().getServer().getAdvancements().get(Identifier.parse(id));require(a!=null,"missing advancement "+id);return c.player.getAdvancements().getOrStartProgress(a).isDone();}
    private static void spawn(GameTestHelper h,Context c,String item,int count) {
        var entity=new ItemEntity(h.getLevel(),c.pos.getX()+.5,c.pos.getY()+.1,c.pos.getZ()+.5,new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("minecraft:"+item)),count));entity.setPickUpDelay(0);require(h.getLevel().addFreshEntity(entity),"spawn failed");c.items.add(entity);
    }
    private static void drain(Context c,boolean acknowledge) {
        c.channel.runPendingTasks();c.channel.runScheduledPendingTasks();c.channel.flushOutbound();Object outgoing;
        while((outgoing=c.channel.readOutbound())!=null) {
            if(outgoing instanceof ClientboundSystemChatPacket message)c.messages.add(message.content());
            if(acknowledge&&outgoing instanceof ClientboundPlayerPositionPacket position)new ServerboundAcceptTeleportationPacket(position.id()).handle(c.player.connection);
            ReferenceCountUtil.release(outgoing);
        }
    }
    private static void cleanup(Context c) {
        for(var item:c.items)item.discard();
        if(c.player!=null){var server=c.player.level().getServer();server.getPlayerList().remove(c.player);server.getConnection().getConnections().remove(c.connection);require(server.getPlayerList().getPlayer(c.player.getUUID())==null,"player cleanup failed");}
        if(c.channel!=null&&c.channel.isOpen()){drain(c,false);c.connection.disconnect(Component.literal("smoke complete"));c.channel.finishAndReleaseAll();}
    }
    private static void fail(GameTestHelper h,Context c,Throwable t) {try{cleanup(c);}catch(Throwable cleanup){t.addSuppressed(cleanup);}h.fail("PRE26 native smoke: "+t);}
    private static void require(boolean condition,String message){if(!condition)throw new IllegalStateException(message);}
    private static final class Context {ServerPlayer player;Connection connection;EmbeddedChannel channel;BlockPos pos;final List<ItemEntity>items=new ArrayList<>();final List<Component>messages=new ArrayList<>();}
}
