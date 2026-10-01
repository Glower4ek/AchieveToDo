package com.diskree.achievetodo.certification;
import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.gametest.v1.*;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.*;
import net.minecraft.tags.TagKey;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import java.lang.reflect.Method;
import java.util.*;
import static com.diskree.achievetodo.certification.Final19StaticContext.require;

public final class Final19DualTagsGameTest implements CustomTestMethodInvoker {
    static final String FAMILY="DUAL_ITEM_BLOCK_TAG_CONTEXT";
    @GameTest(maxTicks=1000) public void dualTagsCanary2(GameTestHelper h){run(h,false);}
    @GameTest(maxTicks=3000) public void dualTagsExact6(GameTestHelper h){run(h,true);}
    private static void run(GameTestHelper h,boolean exact){try{System.out.println("FINAL19_NATIVE_RUN_START family="+FAMILY);String run=Final19FamilyEvidence.begin(Final19WorldgenEvidence.root(),FAMILY,exact);next(h,Final19FamilyEvidence.rows(Final19WorldgenEvidence.root(),FAMILY,exact),0,run,exact);}catch(Throwable e){h.fail("FINAL19 dual tags begin: "+e);}}
    private static void next(GameTestHelper h,List<JsonObject> rows,int i,String run,boolean exact){
        if(i==rows.size()){try{Final19FamilyEvidence.complete(Final19WorldgenEvidence.root(),FAMILY,exact);System.out.println((exact?"TEMP_PROMOTABLE":"TEMP_DIAGNOSTIC")+"=PASS family="+FAMILY+" runId="+run+" entries="+rows.size());h.succeed();}catch(Throwable e){h.fail("dual tag audit: "+e);}return;}
        Context c=new Context(rows.get(i));try{c.pos=h.absolutePos(new BlockPos(3,1,3));for(BlockPos p:List.of(c.pos,c.pos.above(),c.pos.below()))c.blocks.put(p,h.getLevel().getBlockState(p));h.getLevel().setBlockAndUpdate(c.pos.below(),Blocks.STONE.defaultBlockState());h.getLevel().setBlockAndUpdate(c.pos.above(),Blocks.AIR.defaultBlockState());c.actor=Final19NativeActors.join(h.getLevel(),c.pos.offset(0,0,1));c.advancement=Final19NativeActors.advancement(c.actor,c.row);require(!done(c),"Precompleted criterion");configure(h,c);require(!AchieveToDoMod.isTargetInLockedLandmark(c.actor.player(),h.getLevel(),c.pos),"Locked landmark");h.runAfterDelay(1,()->act(h,rows,i,run,exact,c));}catch(Throwable e){fail(h,c,e);}
    }
    private static void configure(GameTestHelper h,Context c){String criterion=c.row.get("criterion").getAsString();c.ability=null;
        switch(criterion){case "candle_cake"->{c.pre=Blocks.CAKE.defaultBlockState();c.stack=new ItemStack(Items.CANDLE);}
        case "candle"->{c.pre=Blocks.CANDLE.defaultBlockState();c.stack=new ItemStack(Items.FLINT_AND_STEEL);c.ability=AbilityType.USE_FLINT_AND_STEEL;}
        case "wax_off"->{c.pre=BuiltInRegistries.BLOCK.getValue(Identifier.parse("minecraft:waxed_copper_block")).defaultBlockState();c.stack=new ItemStack(Items.DIAMOND_AXE);c.ability=AbilityType.findToolMaterialUsageAbility(ToolMaterial.DIAMOND);}
        case "clean_leather_armor"->{c.pre=Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL,3);c.stack=new ItemStack(Items.LEATHER_CHESTPLATE);c.stack.set(DataComponents.DYED_COLOR,new DyedItemColor(0xAA0000));c.ability=AbilityType.USE_CAULDRON;}
        case "clean_banner"->{c.pre=Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL,3);c.stack=new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("minecraft:white_banner")));var pattern=h.getLevel().registryAccess().lookupOrThrow(Registries.BANNER_PATTERN).getOrThrow(ResourceKey.create(Registries.BANNER_PATTERN,Identifier.parse("minecraft:cross")));c.stack.set(DataComponents.BANNER_PATTERNS,new BannerPatternLayers.Builder().add(pattern,DyeColor.RED).build());c.ability=AbilityType.USE_CAULDRON;}
        case "clean_shulker_box"->{c.pre=Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL,3);c.stack=new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("minecraft:red_shulker_box")));c.ability=AbilityType.USE_CAULDRON;}
        default->throw new IllegalArgumentException(criterion);}
        h.getLevel().setBlockAndUpdate(c.pos,c.pre);hand(c,c.stack.copy());
    }
    private static void act(GameTestHelper h,List<JsonObject> rows,int i,String run,boolean exact,Context c){try{
        boolean lockedNegative=true;c.gates=new JsonObject();
        if(c.ability!=null){var board=h.getLevel().getServer().getScoreboard();board.getOrCreatePlayerScore(c.actor.player(),board.getObjective("bac_advancements")).set(0);boolean locked=AchieveToDoMod.isAbilityLocked(c.actor.player(),c.ability,true);require(locked,"Fresh live production gate not locked");String beforeBlock=h.getLevel().getBlockState(c.pos).toString(),beforeItem=item(h,c.actor.player().getMainHandItem());boolean beforeCriterion=done(c);var result=use(h,c);boolean afterCriterion=done(c);boolean mutation=!beforeBlock.equals(h.getLevel().getBlockState(c.pos).toString())||!beforeItem.equals(item(h,c.actor.player().getMainHandItem()));lockedNegative=locked&&!beforeCriterion&&!afterCriterion&&!mutation;
            JsonObject diagnostic=new JsonObject();diagnostic.addProperty("advancementId",c.advancement.id().toString());diagnostic.addProperty("criterion",c.row.get("criterion").getAsString());diagnostic.addProperty("runId",run);diagnostic.addProperty("ability",c.ability.name());diagnostic.addProperty("locked",locked);diagnostic.addProperty("criterionBefore",beforeCriterion);diagnostic.addProperty("criterionAfter",afterCriterion);diagnostic.addProperty("mutation",mutation);diagnostic.addProperty("nativeResult",result.toString());diagnostic.addProperty("blockBefore",beforeBlock);diagnostic.addProperty("blockAfter",h.getLevel().getBlockState(c.pos).toString());diagnostic.addProperty("itemBefore",beforeItem);diagnostic.addProperty("itemAfter",item(h,c.actor.player().getMainHandItem()));
            if(!lockedNegative){diagnostic.addProperty("diagnosticOnly",true);diagnostic.addProperty("classification",afterCriterion&&!mutation?"PRODUCTION_BUG":"HARNESS_OR_GATE_ERROR");c.diagnostic=diagnostic;throw new IllegalStateException("Locked native interaction violated advancement/mutation guard: "+diagnostic);}
            c.gates=Final19NativeActors.unlock(c.actor,c.ability);h.getLevel().setBlockAndUpdate(c.pos,c.pre);hand(c,c.stack.copy());require(!done(c),"Unlock fixture completed target");}
        // Wrong native tool must not complete the target.
        hand(c,new ItemStack(Items.STICK));use(h,c);boolean wrongItem=!done(c);require(wrongItem,"Wrong item completed target");h.getLevel().setBlockAndUpdate(c.pos,c.pre);hand(c,c.stack.copy());
        // A no-op same-item interaction lacks the required qualifying mutation.
        if(c.row.get("criterion").getAsString().equals("candle_cake"))h.getLevel().setBlockAndUpdate(c.pos,Blocks.CAKE.defaultBlockState().setValue(CakeBlock.BITES,1));
        else if(c.row.get("criterion").getAsString().equals("candle")){h.getLevel().setBlockAndUpdate(c.pos,Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.LIT,true));h.getLevel().setBlockAndUpdate(c.pos.above(),Blocks.STONE.defaultBlockState());}
        else if(c.row.get("criterion").getAsString().equals("wax_off"))h.getLevel().setBlockAndUpdate(c.pos,BuiltInRegistries.BLOCK.getValue(Identifier.parse("minecraft:copper_block")).defaultBlockState());
        else{h.getLevel().setBlockAndUpdate(c.pos.above(),Blocks.STONE.defaultBlockState());hand(c,c.row.get("criterion").getAsString().equals("clean_leather_armor")?new ItemStack(Items.LEATHER_CHESTPLATE):c.row.get("criterion").getAsString().equals("clean_banner")?new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("minecraft:white_banner"))):new ItemStack(Items.SHULKER_BOX));}
        String noOpBlock=h.getLevel().getBlockState(c.pos).toString(),noOpItem=item(h,c.actor.player().getMainHandItem());use(h,c);boolean noOp=!done(c)&&noOpBlock.equals(h.getLevel().getBlockState(c.pos).toString())&&noOpItem.equals(item(h,c.actor.player().getMainHandItem()));require(noOp,"No-op use completed target or mutated context");h.getLevel().setBlockAndUpdate(c.pos.above(),Blocks.AIR.defaultBlockState());h.getLevel().setBlockAndUpdate(c.pos,c.pre);hand(c,c.stack.copy());
        String itemTag=null,blockTag=null;for(var term:c.row.getAsJsonObject("expectedCriterion").getAsJsonObject("conditions").getAsJsonArray("location")){var t=term.getAsJsonObject();if(t.get("condition").getAsString().equals("minecraft:match_tool"))itemTag=t.getAsJsonObject("predicate").get("items").getAsString();else blockTag=t.getAsJsonObject("predicate").getAsJsonObject("block").get("blocks").getAsString();}
        boolean itemMember=c.actor.player().getMainHandItem().is(TagKey.create(Registries.ITEM,Identifier.parse(itemTag.substring(1))));String blockBefore=h.getLevel().getBlockState(c.pos).toString(),itemBefore=item(h,c.actor.player().getMainHandItem());boolean wax=c.row.get("criterion").getAsString().equals("wax_off");boolean blockMember=wax&&h.getLevel().getBlockState(c.pos).is(TagKey.create(Registries.BLOCK,Identifier.parse(blockTag.substring(1))));boolean before=done(c);var result=use(h,c);String blockAfter=h.getLevel().getBlockState(c.pos).toString(),itemAfter=item(h,c.actor.player().getMainHandItem());if(!wax)blockMember=h.getLevel().getBlockState(c.pos).is(TagKey.create(Registries.BLOCK,Identifier.parse(blockTag.substring(1))));boolean mutation=!blockBefore.equals(blockAfter)||!itemBefore.equals(itemAfter);require(itemMember&&blockMember&&result.consumesAction()&&mutation&&done(c),"Native exact dual-tag mutation failed");
        var r=Final19NativeActors.receipt(c.actor,c.advancement,c.row,run,before);r.addProperty("nativeBoundary","ServerboundUseItemOnPacket.handle");r.addProperty("checkedItemTag",itemTag);r.addProperty("checkedBlockTag",blockTag);r.addProperty("itemTagMember",itemMember);r.addProperty("blockTagMember",blockMember);r.addProperty("blockTagObservationStage",wax?"PRE_NATIVE_SET_BLOCK":"POST_NATIVE_MUTATION");r.addProperty("blockBefore",blockBefore);r.addProperty("blockAfter",blockAfter);r.addProperty("itemBefore",itemBefore);r.addProperty("itemAfter",itemAfter);r.addProperty("nativeMutation",mutation);r.addProperty("wrongItemNegative",wrongItem);r.addProperty("noOpNegative",noOp);r.addProperty("lockedGateNegative",lockedNegative);r.addProperty("productionGateUnlocked",c.ability==null||!AchieveToDoMod.isAbilityLocked(c.actor.player(),c.ability,true));r.add("productionGateWitness",c.gates);r.add("cleanup",cleanup(h,c));Final19FamilyEvidence.append(Final19WorldgenEvidence.root(),FAMILY,r);h.runAfterDelay(1,()->next(h,rows,i+1,run,exact));
    }catch(Throwable e){fail(h,c,e);}}
    private static String item(GameTestHelper h,ItemStack stack){return ItemStack.CODEC.encodeStart(RegistryOps.create(JsonOps.INSTANCE,h.getLevel().registryAccess()),stack).result().orElseThrow().toString();}
    private static void hand(Context c,ItemStack stack){c.actor.player().getInventory().setItem(c.actor.player().getInventory().getSelectedSlot(),stack);}
    private static InteractionResult use(GameTestHelper h,Context c){var player=c.actor.player();Final19CauldronResultProbe.arm(player.getUUID());new net.minecraft.network.protocol.game.ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(c.pos),Direction.UP,c.pos,false),++c.sequence).handle(player.connection);return Final19CauldronResultProbe.take(player.getUUID());}
    private static boolean done(Context c){return Final19NativeActors.done(c.actor,c.advancement,c.row.get("criterion").getAsString());}
    private static JsonObject cleanup(GameTestHelper h,Context c){boolean restored=true;for(var entry:c.blocks.entrySet()){h.getLevel().setBlockAndUpdate(entry.getKey(),entry.getValue());restored&=h.getLevel().getBlockState(entry.getKey()).equals(entry.getValue());}c.blocks.clear();var clean=c.actor==null?new JsonObject():Final19NativeActors.close(c.actor);clean.addProperty("fixtureEntitiesRemoved",true);clean.addProperty("blocksRestored",restored);return clean;}
    private static void fail(GameTestHelper h,Context c,Throwable e){try{var clean=cleanup(h,c);if(c.diagnostic!=null){c.diagnostic.add("cleanup",clean);Final19WorldgenEvidence.write(Final19WorldgenEvidence.root().resolve("build/tmp/final19_implementation/dual_tags_failure.json"),c.diagnostic);}}catch(Throwable cleanup){e.addSuppressed(cleanup);}h.fail("FINAL19 dual tags "+c.row.get("criterion")+": "+e);}
    private static final class Context{final JsonObject row;int sequence;BlockPos pos;Final19NativeActors.Actor actor;AdvancementHolder advancement;ItemStack stack;BlockState pre;AbilityType ability;JsonObject gates,diagnostic;Map<BlockPos,BlockState> blocks=new LinkedHashMap<>();Context(JsonObject row){this.row=row;}}
    @Override public void invokeTestMethod(GameTestHelper h,Method method)throws ReflectiveOperationException{h.setBlock(0,0,0,Blocks.AIR);method.invoke(this,h);}
}
