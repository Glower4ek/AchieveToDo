package com.diskree.achievetodo.certification;
import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.injection.extension.main.LevelInfoExtension;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.gametest.v1.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.resources.*;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import java.lang.reflect.Method;
import java.util.*;
import net.minecraft.world.phys.*;
import static com.diskree.achievetodo.certification.Final19StaticContext.require;

/** Diagnostic only; a successful test means the production defect was reproduced. */
public final class Final19CauldronGateDiagnosticGameTest implements CustomTestMethodInvoker {
    @GameTest(maxTicks=800) public void lockedCleaningPacketDiagnosis3(GameTestHelper h)throws Exception{
        System.out.println("FINAL19_CAULDRON_GATE_DIAGNOSTIC_START");
        JsonObject output=new JsonObject();output.addProperty("runId",UUID.randomUUID().toString());output.addProperty("diagnosticOnly",true);output.add("entries",new JsonArray());next(h,output,0);
    }
    private static void next(GameTestHelper h,JsonObject output,int index){
        if(index==3){try{output.addProperty("classification","PRODUCTION_BUG");Final19WorldgenEvidence.write(Final19WorldgenEvidence.root().resolve("build/tmp/final19_implementation/cauldron_gate_packet_diagnosis.json"),output);System.out.println("FINAL19_CAULDRON_GATE_PRODUCTION_BUG_CONFIRMED runId="+output.get("runId")+" entries=3");h.succeed();}catch(Throwable e){h.fail("Cauldron diagnostic completion: "+e);}return;}
        var pos=h.absolutePos(new BlockPos(3,1,3));var original=h.getLevel().getBlockState(pos);
        try{var actor=Final19NativeActors.join(h.getLevel(),pos.offset(0,0,1));h.runAfterDelay(1,()->{
            Throwable failure=null;JsonObject row=new JsonObject();
            try{
                var player=actor.player();var server=h.getLevel().getServer();var board=server.getScoreboard();var objective=board.getObjective("bac_advancements");require(objective!=null,"Live score missing");board.getOrCreatePlayerScore(player,objective).set(0);
                var config=((LevelInfoExtension)server.getWorldData().getLevelSettings()).achievetodo$getAbilitiesConfiguration(server.overworld().getSeed());int threshold=config.get(AbilityType.USE_CAULDRON);
                boolean lockedBefore=AchieveToDoMod.isAbilityLocked(player,AbilityType.USE_CAULDRON,true);require(threshold>0&&lockedBefore,"Production cauldron must be locked");require(!AchieveToDoMod.isTargetInLockedLandmark(player,h.getLevel(),pos),"Target landmark locked");
                String criterion=index==0?"clean_leather_armor":index==1?"clean_banner":"clean_shulker_box";String itemTag=index==0?"minecraft:leather_armor":index==1?"minecraft:banners":"minecraft:shulker_boxes";
                var advancement=server.getAdvancements().get(Identifier.parse("blazeandcave:building/washing_machine"));require(advancement!=null&&!Final19NativeActors.done(actor,advancement,criterion),"Precompleted/missing criterion");
                h.getLevel().setBlockAndUpdate(pos,Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL,3));
                player.getInventory().setItem(player.getInventory().getSelectedSlot(),new ItemStack(Items.STICK));packet(player,pos,1);
                boolean wrongItemNegative=!Final19NativeActors.done(actor,advancement,criterion);require(wrongItemNegative,"Wrong item completed target");
                ItemStack stack=new ItemStack(index==0?Items.LEATHER_CHESTPLATE:BuiltInRegistries.ITEM.getValue(Identifier.parse(index==1?"minecraft:white_banner":"minecraft:red_shulker_box")));
                if(index==0)stack.set(DataComponents.DYED_COLOR,new DyedItemColor(0xAA0000));
                if(index==1){var pattern=h.getLevel().registryAccess().lookupOrThrow(Registries.BANNER_PATTERN).getOrThrow(ResourceKey.create(Registries.BANNER_PATTERN,Identifier.parse("minecraft:cross")));stack.set(DataComponents.BANNER_PATTERNS,new BannerPatternLayers.Builder().add(pattern,DyeColor.RED).build());}
                player.getInventory().setItem(player.getInventory().getSelectedSlot(),stack);
                String held=BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).toString();boolean itemMember=player.getMainHandItem().is(TagKey.create(Registries.ITEM,Identifier.parse(itemTag)));boolean blockMember=h.getLevel().getBlockState(pos).is(TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:cauldrons")));
                String beforeBlock=h.getLevel().getBlockState(pos).toString(),beforeItem=item(h,player.getMainHandItem());boolean beforeCriterion=Final19NativeActors.done(actor,advancement,criterion);
                packet(player,pos,2);
                String afterBlock=h.getLevel().getBlockState(pos).toString(),afterItem=item(h,player.getMainHandItem());boolean afterCriterion=Final19NativeActors.done(actor,advancement,criterion);boolean lockedAfter=AchieveToDoMod.isAbilityLocked(player,AbilityType.USE_CAULDRON,true);
                row.addProperty("advancementId",advancement.id().toString());row.addProperty("criterion",criterion);row.addProperty("playerUuid",player.getUUID().toString());row.addProperty("nativeBoundary","ServerboundUseItemOnPacket.handle");row.addProperty("gameMode",player.gameMode().name());row.addProperty("joined",server.getPlayerList().getPlayer(player.getUUID())==player);row.addProperty("connectionRegistered",server.getConnection().getConnections().contains(actor.connection()));row.addProperty("clientLoaded",player.connection.hasClientLoaded());row.addProperty("finiteMaterials",!player.hasInfiniteMaterials());
                row.addProperty("ability","USE_CAULDRON");row.addProperty("liveThreshold",threshold);row.addProperty("lockedBefore",lockedBefore);row.addProperty("lockedAfter",lockedAfter);row.addProperty("scoreAfter",board.getPlayerScoreInfo(player,objective).value());row.addProperty("heldItem",held);row.addProperty("checkedItemTag",itemTag);row.addProperty("checkedBlockTag","minecraft:cauldrons");row.addProperty("itemTagMember",itemMember);row.addProperty("blockTagMember",blockMember);row.addProperty("wrongItemNegative",wrongItemNegative);
                row.addProperty("blockBefore",beforeBlock);row.addProperty("blockAfter",afterBlock);row.addProperty("itemBefore",beforeItem);row.addProperty("itemAfter",afterItem);row.addProperty("criterionBefore",beforeCriterion);row.addProperty("criterionAfter",afterCriterion);
                require(lockedAfter&&!beforeCriterion&&afterCriterion&&beforeBlock.equals(afterBlock)&&beforeItem.equals(afterItem)&&itemMember&&blockMember,"Blocked cauldron false award did not reproduce");
            }catch(Throwable e){failure=e;}
            try{h.getLevel().setBlockAndUpdate(pos,original);var cleanup=Final19NativeActors.close(actor);cleanup.addProperty("blocksRestored",h.getLevel().getBlockState(pos).equals(original));row.add("cleanup",cleanup);output.getAsJsonArray("entries").add(row);Final19WorldgenEvidence.write(Final19WorldgenEvidence.root().resolve("build/tmp/final19_implementation/cauldron_gate_packet_diagnosis.json"),output);}catch(Throwable e){if(failure==null)failure=e;else failure.addSuppressed(e);}
            if(failure!=null)h.fail("Cauldron packet diagnosis: "+failure);else h.runAfterDelay(1,()->next(h,output,index+1));
        });}catch(Throwable e){h.fail("Cauldron join: "+e);}
    }
    private static String item(GameTestHelper h,ItemStack stack){return ItemStack.CODEC.encodeStart(RegistryOps.create(JsonOps.INSTANCE,h.getLevel().registryAccess()),stack).result().orElseThrow().toString();}
    private static void packet(net.minecraft.server.level.ServerPlayer player,BlockPos pos,int sequence){new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false),sequence).handle(player.connection);}
    @Override public void invokeTestMethod(GameTestHelper h,Method method)throws ReflectiveOperationException{h.setBlock(0,0,0,Blocks.AIR);method.invoke(this,h);}
}
