package com.diskree.achievetodo.certification;
import com.diskree.achievetodo.AchieveToDoMod;
import com.google.gson.*;
import net.fabricmc.fabric.api.gametest.v1.*;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.resources.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.lang.reflect.Method;
import java.util.*;
import static com.diskree.achievetodo.certification.Final19StaticContext.require;

public final class Final19VariantsGameTest implements CustomTestMethodInvoker {
    static final String FAMILY="ENTITY_VARIANT_COMPONENT_CONTEXT";
    @GameTest(maxTicks=3000) public void variantsCanary3(GameTestHelper h){run(h,false);}
    @GameTest(maxTicks=10000) public void variantsExact23(GameTestHelper h){run(h,true);}
    private static void run(GameTestHelper h,boolean exact){try{System.out.println("FINAL19_NATIVE_RUN_START family="+FAMILY);String run=Final19FamilyEvidence.begin(Final19WorldgenEvidence.root(),FAMILY,exact);next(h,Final19FamilyEvidence.rows(Final19WorldgenEvidence.root(),FAMILY,exact),0,run,exact);}catch(Throwable e){h.fail("FINAL19 variants begin: "+e);}}
    private static void next(GameTestHelper h,List<JsonObject> rows,int i,String run,boolean exact){
        if(i==rows.size()){try{Final19FamilyEvidence.complete(Final19WorldgenEvidence.root(),FAMILY,exact);System.out.println((exact?"TEMP_PROMOTABLE":"TEMP_DIAGNOSTIC")+"=PASS family="+FAMILY+" runId="+run+" entries="+rows.size());h.succeed();}catch(Throwable e){h.fail("FINAL19 variants audit: "+e);}return;}
        Context c=new Context(rows.get(i));try{
            c.pos=h.absolutePos(new BlockPos(3,1,3));for(int x=-1;x<=3;x++)for(int z=-1;z<=1;z++){BlockPos p=c.pos.offset(x,-1,z);c.blocks.put(p,h.getLevel().getBlockState(p));h.getLevel().setBlockAndUpdate(p,Blocks.STONE.defaultBlockState());}
            c.actor=Final19NativeActors.join(h.getLevel(),c.pos);c.advancement=Final19NativeActors.advancement(c.actor,c.row);require(!done(c),"Target precompleted");
            var components=c.row.getAsJsonObject("expectedCriterion").getAsJsonObject("conditions").getAsJsonArray("entity").get(0).getAsJsonObject().getAsJsonObject("predicate").getAsJsonObject("components");var component=components.entrySet().iterator().next();c.species=component.getKey().split(":")[1].split("/")[0];c.variant=component.getValue().getAsString();
            String wrong=c.species.equals("cat")?(c.variant.equals("minecraft:black")?"minecraft:tabby":"minecraft:black"):c.species.equals("wolf")?(c.variant.equals("minecraft:pale")?"minecraft:spotted":"minecraft:pale"):(c.variant.equals("minecraft:warm")?"minecraft:temperate":"minecraft:warm");
            c.mob=spawn(h,c,wrong);c.wrongObserved=observed(c.mob);c.actor.player().getInventory().setItem(c.actor.player().getInventory().getSelectedSlot(),new ItemStack(food(c.species),64));c.before=done(c);h.runAfterDelay(1,()->interact(h,rows,i,run,exact,c,true,0));
        }catch(Throwable e){fail(h,c,e);}
    }
    private static Mob spawn(GameTestHelper h,Context c,String variant){require(Set.of("cat","wolf","frog").contains(c.species),"Unexpected species");Mob mob=(Mob)BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse("minecraft:"+c.species)).create(h.getLevel(),EntitySpawnReason.COMMAND);require(mob!=null,"Species spawn failed");mob.setNoAi(true);mob.setNoGravity(true);mob.teleportTo(c.pos.getX()+1.5,c.pos.getY(),c.pos.getZ()+.5);
        // A real registered variant is a deterministic untamed/unleashed fixture precondition.
        // Only the subsequent normal player packet may tame or attach the lead.
        if(mob instanceof Cat cat)cat.setComponent(DataComponents.CAT_VARIANT,h.getLevel().registryAccess().lookupOrThrow(Registries.CAT_VARIANT).getOrThrow(ResourceKey.create(Registries.CAT_VARIANT,Identifier.parse(variant))));
        if(mob instanceof Wolf wolf)wolf.setComponent(DataComponents.WOLF_VARIANT,h.getLevel().registryAccess().lookupOrThrow(Registries.WOLF_VARIANT).getOrThrow(ResourceKey.create(Registries.WOLF_VARIANT,Identifier.parse(variant))));
        if(mob instanceof Frog frog)frog.setComponent(DataComponents.FROG_VARIANT,h.getLevel().registryAccess().lookupOrThrow(Registries.FROG_VARIANT).getOrThrow(ResourceKey.create(Registries.FROG_VARIANT,Identifier.parse(variant))));
        require(observed(mob).equals(variant)&&!nativeSuccess(mob,c.actor.player()),"Invalid fixture state");require(h.getLevel().addFreshEntity(mob)&&!AchieveToDoMod.isTargetInLockedLandmark(c.actor.player(),mob),"Spawn/landmark gate rejected");return mob;
    }
    private static void interact(GameTestHelper h,List<JsonObject> rows,int i,String run,boolean exact,Context c,boolean negative,int attempts){try{
        require(attempts<64&&!c.actor.player().getMainHandItem().isEmpty(),"Finite native taming retries exhausted");new ServerboundInteractPacket(c.mob.getId(),InteractionHand.MAIN_HAND,c.mob.position(),false).handle(c.actor.player().connection);
        if(!nativeSuccess(c.mob,c.actor.player())){h.runAfterDelay(1,()->interact(h,rows,i,run,exact,c,negative,attempts+1));return;}
        int consumed=64-c.actor.player().getMainHandItem().getCount();require(consumed>0,"No native resource consumption");
        if(negative){c.negativeConsumed=consumed;c.wrongNegative=!done(c);require(c.wrongNegative,"Wrong variant completed target");c.mob.discard();c.mob=spawn(h,c,c.variant);c.tamedBefore=c.mob instanceof TamableAnimal animal&&animal.isTame();c.leashedBefore=c.mob instanceof Leashable leash&&leash.getLeashHolder()!=null;require(!c.tamedBefore&&!c.leashedBefore,"Positive fixture must be untamed/unleashed");c.actor.player().getInventory().setItem(c.actor.player().getInventory().getSelectedSlot(),new ItemStack(food(c.species),64));require(!done(c),"Target completed during fixture setup");h.runAfterDelay(1,()->interact(h,rows,i,run,exact,c,false,0));return;}
        require(done(c),"Native success did not complete exact variant criterion");JsonObject r=Final19NativeActors.receipt(c.actor,c.advancement,c.row,run,c.before);r.addProperty("observedVariant",observed(c.mob));r.addProperty("observedSpecies",BuiltInRegistries.ENTITY_TYPE.getKey(c.mob.getType()).toString());r.addProperty("wrongObservedVariant",c.wrongObserved);r.addProperty("wrongVariantNegative",c.wrongNegative);r.addProperty("negativeResourceConsumed",c.negativeConsumed);r.addProperty("nativeInteraction",consumed>0);r.addProperty("resourceConsumed",consumed);r.addProperty("attempts",attempts+1);
        if(c.mob instanceof TamableAnimal animal){r.addProperty("tamedBefore",c.tamedBefore);r.addProperty("tamedAfter",animal.isTame());r.addProperty("ownedByJoinedPlayer",animal.isOwnedBy(c.actor.player()));}else{r.addProperty("leashedBefore",c.leashedBefore);r.addProperty("leashedToJoinedPlayer",((Leashable)c.mob).getLeashHolder()==c.actor.player());}
        r.add("cleanup",cleanup(h,c));Final19FamilyEvidence.append(Final19WorldgenEvidence.root(),FAMILY,r);h.runAfterDelay(1,()->next(h,rows,i+1,run,exact));
    }catch(Throwable e){fail(h,c,e);}}
    private static Item food(String species){return species.equals("cat")?Items.COD:species.equals("wolf")?Items.BONE:Items.LEAD;}
    private static String observed(Mob mob){Holder<?> holder=mob instanceof Cat cat?cat.get(DataComponents.CAT_VARIANT):mob instanceof Wolf wolf?wolf.get(DataComponents.WOLF_VARIANT):((Frog)mob).get(DataComponents.FROG_VARIANT);require(holder!=null,"Live variant component missing");return holder.unwrapKey().orElseThrow().identifier().toString();}
    private static boolean nativeSuccess(Mob mob,net.minecraft.server.level.ServerPlayer player){return mob instanceof TamableAnimal animal?animal.isTame()&&animal.isOwnedBy(player):((Leashable)mob).getLeashHolder()==player;}
    private static boolean done(Context c){return Final19NativeActors.done(c.actor,c.advancement,c.row.get("criterion").getAsString());}
    private static JsonObject cleanup(GameTestHelper h,Context c){if(c.mob!=null)c.mob.discard();boolean restored=true;for(var entry:c.blocks.entrySet()){h.getLevel().setBlockAndUpdate(entry.getKey(),entry.getValue());restored&=h.getLevel().getBlockState(entry.getKey()).equals(entry.getValue());}c.blocks.clear();var clean=c.actor==null?new JsonObject():Final19NativeActors.close(c.actor);clean.addProperty("fixtureEntitiesRemoved",c.mob==null||!c.mob.isAlive());clean.addProperty("blocksRestored",restored);return clean;}
    private static void fail(GameTestHelper h,Context c,Throwable e){try{cleanup(h,c);}catch(Throwable cleanup){e.addSuppressed(cleanup);}h.fail("FINAL19 variants "+c.row.get("criterion")+": "+e);}
    private static final class Context {final JsonObject row;BlockPos pos;Final19NativeActors.Actor actor;AdvancementHolder advancement;Mob mob;String species,variant,wrongObserved;boolean before,wrongNegative,tamedBefore,leashedBefore;int negativeConsumed;final Map<BlockPos,BlockState> blocks=new LinkedHashMap<>();Context(JsonObject row){this.row=row;}}
    @Override public void invokeTestMethod(GameTestHelper h,Method method)throws ReflectiveOperationException{h.setBlock(0,0,0,Blocks.AIR);method.invoke(this,h);}
}
