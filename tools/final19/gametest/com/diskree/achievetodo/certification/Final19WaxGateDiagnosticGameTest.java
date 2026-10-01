package com.diskree.achievetodo.certification;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.injection.extension.main.LevelInfoExtension;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.gametest.v1.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import java.lang.reflect.Method;
import java.util.UUID;
import static com.diskree.achievetodo.certification.Final19StaticContext.require;

/** Diagnostic only: requires the suspected bug to reproduce; never certification evidence. */
public final class Final19WaxGateDiagnosticGameTest implements CustomTestMethodInvoker {
    @GameTest(maxTicks=400) public void lockedWaxPacketDiagnosis(GameTestHelper h) throws Exception {
        System.out.println("FINAL19_WAX_GATE_DIAGNOSTIC_START");
        var pos=h.absolutePos(new BlockPos(3,1,3));
        var original=h.getLevel().getBlockState(pos);
        var actor=Final19NativeActors.join(h.getLevel(),pos.offset(0,0,1));
        h.runAfterDelay(1,()->{
            JsonObject result=new JsonObject(); Throwable failure=null;
            try {
                var player=actor.player(); var server=h.getLevel().getServer();
                var objective=server.getScoreboard().getObjective("bac_advancements");
                require(objective!=null,"Missing production score objective");
                server.getScoreboard().getOrCreatePlayerScore(player,objective).set(0);
                var configuration=((LevelInfoExtension)server.getWorldData().getLevelSettings()).achievetodo$getAbilitiesConfiguration(server.overworld().getSeed());
                int threshold=configuration.get(AbilityType.USE_DIAMOND_TOOLS);
                require(threshold>0&&AchieveToDoMod.isAbilityLocked(player,AbilityType.USE_DIAMOND_TOOLS,true),"Production ability must be locked");
                require(!AchieveToDoMod.isTargetInLockedLandmark(player,h.getLevel(),pos),"Fixture in locked landmark");
                var row=new JsonObject();row.addProperty("advancementId","minecraft:husbandry/wax_off");
                var advancement=Final19NativeActors.advancement(actor,row);
                require(!Final19NativeActors.done(actor,advancement,"wax_off"),"Precompleted criterion");
                player.getInventory().setItem(player.getInventory().getSelectedSlot(),new ItemStack(Items.DIAMOND_AXE));
                h.getLevel().setBlockAndUpdate(pos,Blocks.OAK_LOG.defaultBlockState());
                new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND,hit(pos),1).handle(player.connection);
                boolean stripBlocked=h.getLevel().getBlockState(pos).is(Blocks.OAK_LOG);
                require(stripBlocked&&!Final19NativeActors.done(actor,advancement,"wax_off"),"Locked stripping control failed");
                var waxed=BuiltInRegistries.BLOCK.getValue(Identifier.parse("minecraft:waxed_copper_block"));
                h.getLevel().setBlockAndUpdate(pos,waxed.defaultBlockState());
                String blockBefore=h.getLevel().getBlockState(pos).toString();
                int damageBefore=player.getMainHandItem().getDamageValue();
                boolean criterionBefore=Final19NativeActors.done(actor,advancement,"wax_off");
                new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND,hit(pos),2).handle(player.connection);
                boolean lockedAfter=AchieveToDoMod.isAbilityLocked(player,AbilityType.USE_DIAMOND_TOOLS,true);
                String blockAfter=h.getLevel().getBlockState(pos).toString();
                int damageAfter=player.getMainHandItem().getDamageValue();
                boolean criterionAfter=Final19NativeActors.done(actor,advancement,"wax_off");
                result.addProperty("runId",UUID.randomUUID().toString());
                result.addProperty("diagnosticOnly",true);result.addProperty("classification","PRODUCTION_BUG");
                result.addProperty("nativeBoundary","ServerboundUseItemOnPacket.handle -> ServerGamePacketListenerImpl.handleUseItemOn -> ServerPlayerGameMode.useItemOn");
                result.addProperty("playerUuid",player.getUUID().toString());result.addProperty("gameMode",player.gameMode().name());
                result.addProperty("joined",server.getPlayerList().getPlayer(player.getUUID())==player);
                result.addProperty("connectionRegistered",server.getConnection().getConnections().contains(actor.connection()));
                result.addProperty("clientLoaded",player.connection.hasClientLoaded());result.addProperty("finiteMaterials",!player.hasInfiniteMaterials());
                result.addProperty("ability","USE_DIAMOND_TOOLS");result.addProperty("liveThreshold",threshold);
                result.addProperty("lockedBefore",true);result.addProperty("lockedAfter",lockedAfter);
                result.addProperty("scoreAfter",server.getScoreboard().getPlayerScoreInfo(player,objective).value());
                result.addProperty("heldItem",BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).toString());
                result.addProperty("lockedStrippingBlocked",stripBlocked);result.addProperty("blockBefore",blockBefore);result.addProperty("blockAfter",blockAfter);
                result.addProperty("damageBefore",damageBefore);result.addProperty("damageAfter",damageAfter);
                result.addProperty("criterionBefore",criterionBefore);result.addProperty("criterionAfter",criterionAfter);
                require(lockedAfter&&!criterionBefore&&criterionAfter&&h.getLevel().getBlockState(pos).is(BuiltInRegistries.BLOCK.getValue(Identifier.parse("minecraft:copper_block")))&&damageAfter==damageBefore+1,"Wax bypass did not reproduce through native packet");
            } catch(Throwable e){failure=e;}
            try {
                h.getLevel().setBlockAndUpdate(pos,original);
                var cleanup=Final19NativeActors.close(actor);cleanup.addProperty("blocksRestored",h.getLevel().getBlockState(pos).equals(original));
                result.add("cleanup",cleanup);
                Final19WorldgenEvidence.write(Final19WorldgenEvidence.root().resolve("build/tmp/final19_implementation/wax_gate_packet_diagnosis.json"),result);
            } catch(Throwable e){if(failure==null)failure=e;else failure.addSuppressed(e);}
            if(failure!=null)h.fail("Wax gate packet diagnosis: "+failure);
            else {System.out.println("FINAL19_WAX_GATE_PRODUCTION_BUG_CONFIRMED "+result);h.succeed();}
        });
    }
    private static BlockHitResult hit(BlockPos pos){return new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false);}
    @Override public void invokeTestMethod(GameTestHelper h,Method method)throws ReflectiveOperationException {h.setBlock(0,0,0,Blocks.AIR);method.invoke(this,h);}
}
