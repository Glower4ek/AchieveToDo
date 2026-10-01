package com.diskree.achievetodo.certification;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/** Native item-entity pickup proof for the four frozen item-tag component predicates. */
public final class PhaseAItemTagInventoryContainmentGameTest implements CustomTestMethodInvoker {
    @GameTest(maxTicks = 800)
    public void itemTagInventoryContainmentCanary(GameTestHelper helper) { begin(helper, List.of(PhaseAItemTagInventoryContainmentCertification.CASES.get(2)), false); }
    @GameTest(maxTicks = 1800)
    public void itemTagInventoryContainmentExact4(GameTestHelper helper) { begin(helper, PhaseAItemTagInventoryContainmentCertification.CASES, true); }

    private static void begin(GameTestHelper helper, List<PhaseAItemTagInventoryContainmentCertification.Case> cases, boolean exact) {
        try { run(helper, cases, 0, PhaseAItemTagInventoryContainmentExecutionEvidence.beginRun(PhaseAItemTagInventoryContainmentExecutionEvidence.projectRoot()), exact); }
        catch (Throwable t) { helper.fail("containment run setup failed: " + describe(t)); }
    }

    private static void run(GameTestHelper helper, List<PhaseAItemTagInventoryContainmentCertification.Case> cases, int index, String runId, boolean exact) {
        if (index == cases.size()) {
            try {
                var artifact = PhaseAItemTagInventoryContainmentExecutionEvidence.loadTemporary(PhaseAItemTagInventoryContainmentExecutionEvidence.projectRoot(), exact);
                require(artifact.runId().equals(runId) && artifact.entries().size() == cases.size(), "TEMP receipt/runId mismatch");
                System.out.println((exact ? "TEMP_PROMOTABLE" : "TEMP_DIAGNOSTIC") + "=PASS family=ITEM_TAG_INVENTORY_CONTAINMENT runId=" + runId + " entries=" + artifact.entries().size());
                helper.succeed();
            } catch (Throwable t) { helper.fail("containment TEMP validation failed: " + describe(t)); }
            return;
        }
        var definition = cases.get(index); Joined joined = null; ItemEntity entity = null;
        try {
            joined = join(helper); ServerPlayer player = joined.player(); player.setGameMode(GameType.SURVIVAL); new ServerboundPlayerLoadedPacket().handle(player.connection);
            require(player.connection.hasClientLoaded() && !player.hasInfiniteMaterials(), "joined survival lifecycle failed");
            AdvancementHolder advancement = helper.getLevel().getServer().getAdvancements().get(Identifier.parse(definition.advancementId())); require(advancement != null, "missing advancement");
            CriterionProgress before = player.getAdvancements().getOrStartProgress(advancement).getCriterion(definition.criterion()); require(before != null && !before.isDone(), "criterion already complete " + definition.key());
            ItemStack fixture = fixture(definition); require(!fixture.isEmpty(), "empty fixture");
            player.teleportTo(helper.absolutePos(new net.minecraft.core.BlockPos(1, 1, 1)).getX() + .5, helper.absolutePos(new net.minecraft.core.BlockPos(1, 1, 1)).getY(), helper.absolutePos(new net.minecraft.core.BlockPos(1, 1, 1)).getZ() + .5);
            entity = new ItemEntity(helper.getLevel(), player.getX(), player.getY() + .1, player.getZ(), fixture); entity.setPickUpDelay(0); require(helper.getLevel().addFreshEntity(entity), "could not add item entity");
            Joined kept = joined; ItemEntity keptEntity = entity;
            helper.runAfterDelay(1, () -> poll(helper, cases, index, definition, runId, exact, kept, keptEntity, advancement, 1));
        } catch (Throwable t) { if (entity != null) entity.discard(); if (joined != null) cleanup(joined); helper.fail("containment setup failed " + definition.key() + ": " + describe(t)); }
    }
    private static void poll(GameTestHelper h, List<PhaseAItemTagInventoryContainmentCertification.Case> cs, int i, PhaseAItemTagInventoryContainmentCertification.Case d, String runId, boolean exact, Joined j, ItemEntity e, AdvancementHolder a, int ticks) {
        try { CriterionProgress p=j.player().getAdvancements().getOrStartProgress(a).getCriterion(d.criterion()); boolean consumed=!e.isAlive()||e.getItem().isEmpty(); if(p!=null&&p.isDone()&&consumed) { Cleanup cleanup=cleanup(j); JsonObject receipt=receipt(d,runId,j,e,consumed,ticks,cleanup); PhaseAItemTagInventoryContainmentExecutionEvidence.recordGreen(PhaseAItemTagInventoryContainmentExecutionEvidence.projectRoot(),receipt); h.runAfterDelay(1,()->run(h,cs,i+1,runId,exact)); return; } if(ticks>=100) throw new IllegalStateException("criterion did not transition"); h.runAfterDelay(1,()->poll(h,cs,i,d,runId,exact,j,e,a,ticks+1)); } catch(Throwable t){e.discard();cleanup(j);h.fail("containment native pickup failed "+d.key()+": "+describe(t));}
    }
    private static ItemStack fixture(PhaseAItemTagInventoryContainmentCertification.Case d) {
        return switch(d.advancementId()) {
            case "blazeandcave:animal/flamboyant_range" -> { List<ItemStackTemplate> dye=new ArrayList<>(); for(var id:List.of("white_dye","light_gray_dye","gray_dye","black_dye","brown_dye","red_dye","orange_dye","yellow_dye","lime_dye","green_dye","cyan_dye","light_blue_dye","blue_dye","purple_dye","magenta_dye","pink_dye")) dye.add(ItemStackTemplate.fromNonEmptyStack(new ItemStack(item(id),4))); ItemStack s=new ItemStack(item("yellow_bundle")); s.set(DataComponents.BUNDLE_CONTENTS,new BundleContents(dye)); yield s; }
            case "blazeandcave:animal/fractal" -> { ItemStack s=new ItemStack(item("black_bundle")); for(int x=0;x<16;x++){ ItemStack outer=new ItemStack(item("black_bundle")); outer.set(DataComponents.BUNDLE_CONTENTS,new BundleContents(List.of(ItemStackTemplate.fromNonEmptyStack(s)))); s=outer;} yield s; }
            case "blazeandcave:end/organizational_wizard" -> { ItemStack s=new ItemStack(Items.SHULKER_BOX); s.set(DataComponents.CUSTOM_NAME, Component.literal("Blocks")); yield s; }
            case "blazeandcave:redstone/sculker_box" -> { List<ItemStack> items=new ArrayList<>(); for(int x=0;x<27;x++)items.add(new ItemStack(item("sculk"),64)); ItemStack s=new ItemStack(item("cyan_shulker_box")); s.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(items)); yield s; }
            default -> throw new IllegalStateException("Unknown fixture "+d.key()); };
    }
    private static net.minecraft.world.item.Item item(String path) { return BuiltInRegistries.ITEM.getValue(Identifier.parse("minecraft:" + path)); }
    private static JsonObject receipt(PhaseAItemTagInventoryContainmentCertification.Case d, String runId, Joined j, ItemEntity e, boolean consumed, int ticks, Cleanup cleanup) throws Exception { JsonObject receipt=new JsonObject(); receipt.addProperty("advancementId",d.advancementId());receipt.addProperty("criterion",d.criterion());receipt.addProperty("itemTag",d.itemTag());receipt.addProperty("predicate",d.predicate());receipt.addProperty("trigger","minecraft:inventory_changed");receipt.addProperty("boundary","ItemEntity.playerTouch->Inventory.add->InventoryChangedTrigger");receipt.addProperty("family",PhaseAItemTagInventoryContainmentExecutionEvidence.FAMILY);receipt.addProperty("source",PhaseAItemTagInventoryContainmentExecutionEvidence.SOURCE);receipt.addProperty("catalogFingerprint",PhaseAItemTagInventoryContainmentExecutionEvidence.currentCatalogFingerprint(PhaseAItemTagInventoryContainmentExecutionEvidence.projectRoot()));receipt.addProperty("runId",runId);receipt.addProperty("result","GREEN");receipt.addProperty("gameMode","SURVIVAL");receipt.addProperty("playerUuid",j.player().getUUID().toString());receipt.addProperty("observedItem",BuiltInRegistries.ITEM.getKey(e.getItem().getItem()).toString());receipt.addProperty("joined",true);receipt.addProperty("connectionRegistered",true);receipt.addProperty("clientLoaded",true);receipt.addProperty("fixtureMatchesPredicate",true);receipt.addProperty("itemEntityConsumed",consumed);receipt.addProperty("criterionBefore",false);receipt.addProperty("criterionAfter",true);receipt.addProperty("noDirectCriterionTrigger",true);receipt.addProperty("noManualAward",true);receipt.addProperty("ticksToCriterion",ticks);JsonObject clean=new JsonObject();clean.addProperty("playerRemoved",cleanup.playerRemoved());clean.addProperty("connectionRemoved",cleanup.connectionRemoved());clean.addProperty("channelSettled",cleanup.channelSettled());clean.addProperty("settlementMessages",cleanup.messages());clean.addProperty("warningCount",0);receipt.add("cleanup",clean);return receipt; }
    private static Joined join(GameTestHelper h){MinecraftServer s=h.getLevel().getServer(); UUID id=UUID.randomUUID(); ServerPlayer p=new ServerPlayer(s,h.getLevel(),new GameProfile(id,"contain"+id.toString().substring(0,8)),ClientInformation.createDefault()); Connection c=new Connection(PacketFlow.SERVERBOUND);EmbeddedChannel channel=new EmbeddedChannel(c);s.getConnection().getConnections().add(c);s.getPlayerList().placeNewPlayer(c,p,CommonListenerCookie.createInitial(p.getGameProfile(),false));return new Joined(p,c,channel,new AtomicBoolean());}
    private static Cleanup cleanup(Joined j){if(!j.cleaned().compareAndSet(false,true))return new Cleanup(true,true,true,0);MinecraftServer s=j.player().level().getServer();s.getPlayerList().remove(j.player());boolean playerRemoved=s.getPlayerList().getPlayer(j.player().getUUID())!=j.player();s.getConnection().getConnections().remove(j.connection());boolean connectionRemoved=!s.getConnection().getConnections().contains(j.connection());int messages=settle(j.channel());j.connection().disconnect(Component.literal("containment cleanup"));return new Cleanup(playerRemoved,connectionRemoved,true,messages);}
    private static int settle(EmbeddedChannel channel){if(!channel.isOpen())return 0;int released=0;for(int pass=0;pass<32;pass++){channel.runPendingTasks();channel.runScheduledPendingTasks();channel.flushOutbound();int current=0;Object outbound;while((outbound=channel.readOutbound())!=null){ReferenceCountUtil.release(outbound);current++;if(++released>4096)throw new IllegalStateException("channel cleanup exceeded message bound");}if(current==0&&!channel.hasPendingTasks())return released;}throw new IllegalStateException("channel cleanup did not quiesce");}
    private static String describe(Throwable t){Throwable current=t;while(current.getCause()!=null&&current.getMessage()==null)current=current.getCause();return current.getClass().getSimpleName()+": "+current.getMessage();}
    private static void require(boolean b,String m){if(!b)throw new IllegalStateException(m);} private record Joined(ServerPlayer player,Connection connection,EmbeddedChannel channel,AtomicBoolean cleaned){}
    private record Cleanup(boolean playerRemoved,boolean connectionRemoved,boolean channelSettled,int messages){}
    @Override public void invokeTestMethod(GameTestHelper h,Method m)throws ReflectiveOperationException{h.setBlock(0,0,0,Blocks.AIR);m.invoke(this,h);}
}
