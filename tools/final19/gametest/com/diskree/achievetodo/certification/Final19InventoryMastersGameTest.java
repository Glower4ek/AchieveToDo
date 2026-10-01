package com.diskree.achievetodo.certification;
import com.google.gson.*;
import net.fabricmc.fabric.api.gametest.v1.*;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.*;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.block.Blocks;
import java.lang.reflect.Method;
import java.util.*;
import static com.diskree.achievetodo.certification.Final19StaticContext.require;
public final class Final19InventoryMastersGameTest implements CustomTestMethodInvoker {
    static final String FAMILY="INVENTORY_ENCHANTMENT_ITEM_TAG_CONTEXT";
    @GameTest(maxTicks=1200) public void inventoryMastersCanary2(GameTestHelper h){run(h,false);}
    @GameTest(maxTicks=1800) public void inventoryMastersExact9(GameTestHelper h){run(h,true);}
    static void run(GameTestHelper h,boolean exact){try{System.out.println("FINAL19_NATIVE_RUN_START family="+FAMILY);String run=Final19FamilyEvidence.begin(Final19WorldgenEvidence.root(),FAMILY,exact);next(h,Final19FamilyEvidence.rows(Final19WorldgenEvidence.root(),FAMILY,exact),0,run,exact);}catch(Throwable e){h.fail("Inventory masters begin: "+e);}}
    static void next(GameTestHelper h,List<JsonObject> rows,int i,String run,boolean exact){
        if(i==rows.size()){try{Final19FamilyEvidence.complete(Final19WorldgenEvidence.root(),FAMILY,exact);System.out.println((exact?"TEMP_PROMOTABLE":"TEMP_DIAGNOSTIC")+"=PASS family="+FAMILY+" runId="+run+" entries="+rows.size());h.succeed();}catch(Throwable e){h.fail("Inventory masters completion: "+e);}return;}
        Context c=new Context(rows.get(i));try{c.actor=Final19NativeActors.join(h.getLevel(),h.absolutePos(new BlockPos(3,2,3)));c.advancement=Final19NativeActors.advancement(c.actor,c.row);h.runAfterDelay(1,()->act(h,rows,i,run,exact,c));}catch(Throwable e){fail(h,c,e);}
    }
    static void act(GameTestHelper h,List<JsonObject> rows,int i,String run,boolean exact,Context c){try{
        require(!done(c),"Precompleted target");var spec=c.row.getAsJsonObject("expectedCriterion").getAsJsonObject("conditions").getAsJsonArray("items").get(0).getAsJsonObject();String tag=spec.get("items").getAsString();var members=BuiltInRegistries.ITEM.getOrThrow(TagKey.create(Registries.ITEM,Identifier.parse(tag.substring(1))));
        ItemStack positive=null;for(var member:members){try{positive=construct(h,member.value(),spec,0);break;}catch(IllegalStateException ignored){}}require(positive!=null,"No legal supported item witness");c.stack=positive;
        ItemStack missing=construct(h,positive.getItem(),spec,1);var missingObserved=observed(missing);pickup(h,c,missing);boolean missingNegative=!done(c);require(missingNegative,"Missing-enchantment pickup awarded target");c.actor.player().getInventory().clearContent();
        ItemStack under=construct(h,positive.getItem(),spec,2);var underObserved=observed(under);pickup(h,c,under);boolean underNegative=!done(c);require(underNegative,"Underlevel pickup awarded target");c.actor.player().getInventory().clearContent();
        boolean before=done(c);require(!before,"Negatives completed target");pickup(h,c,positive.copy());require(done(c),"Qualifying native pickup did not award target");ItemStack actual=ItemStack.EMPTY;int count=0;for(int slot=0;slot<c.actor.player().getInventory().getContainerSize();slot++){var candidate=c.actor.player().getInventory().getItem(slot);if(ItemStack.isSameItemSameComponents(candidate,positive)){actual=candidate;count+=candidate.getCount();}}require(count==1&&!actual.isEmpty(),"Finite live pickup witness mismatch");
        var r=Final19NativeActors.receipt(c.actor,c.advancement,c.row,run,before);r.addProperty("nativeBoundary","ItemEntity.playerTouch -> Inventory.add");r.addProperty("nativePickup",true);r.addProperty("checkedItemTag",tag);r.addProperty("observedItem",BuiltInRegistries.ITEM.getKey(actual.getItem()).toString());r.addProperty("itemTagMember",actual.is(TagKey.create(Registries.ITEM,Identifier.parse(tag.substring(1)))));r.addProperty("supportedEnchantments",supported(actual));r.addProperty("compatibleEnchantments",compatible(actual));r.add("observedEnchantments",observed(actual));r.addProperty("inventoryCount",count);r.addProperty("missingEnchantmentNegative",missingNegative);r.addProperty("underlevelNegative",underNegative);r.add("missingObservedEnchantments",missingObserved);r.add("underlevelObservedEnchantments",underObserved);r.add("cleanup",cleanup(c));Final19FamilyEvidence.append(Final19WorldgenEvidence.root(),FAMILY,r);h.runAfterDelay(1,()->next(h,rows,i+1,run,exact));
    }catch(Throwable e){fail(h,c,e);}}
    static ItemStack construct(GameTestHelper h,Item item,JsonObject spec,int mode){var registry=h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);ItemStack stack=new ItemStack(item);int index=0;for(var value:spec.getAsJsonObject("predicates").getAsJsonArray("enchantments")){var e=value.getAsJsonObject();String id=e.getAsJsonArray("enchantments").get(0).getAsString();var holder=registry.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,Identifier.parse(id)));int level=e.has("levels")&&e.getAsJsonObject("levels").has("min")?e.getAsJsonObject("levels").get("min").getAsInt():1;require(holder.value().canEnchant(stack)&&level<=holder.value().getMaxLevel(),"Unsupported/non-gameplay combination");if(mode==1&&index++==0)continue;if(mode==2&&index++==0)level--;if(level>0)stack.enchant(holder,level);}require(compatible(stack),"Exclusive enchantment combination");return stack;}
    static boolean supported(ItemStack stack){for(var holder:stack.getOrDefault(DataComponents.ENCHANTMENTS,ItemEnchantments.EMPTY).keySet())if(!holder.value().canEnchant(stack))return false;return true;}
    static boolean compatible(ItemStack stack){var values=new ArrayList<>(stack.getOrDefault(DataComponents.ENCHANTMENTS,ItemEnchantments.EMPTY).keySet());for(int i=0;i<values.size();i++)for(int j=i+1;j<values.size();j++)if(!Enchantment.areCompatible(values.get(i),values.get(j)))return false;return true;}
    static JsonObject observed(ItemStack stack){JsonObject actual=new JsonObject();var enchants=stack.getOrDefault(DataComponents.ENCHANTMENTS,ItemEnchantments.EMPTY);for(var holder:enchants.keySet())actual.addProperty(holder.unwrapKey().orElseThrow().identifier().toString(),enchants.getLevel(holder));return actual;}
    static void pickup(GameTestHelper h,Context c,ItemStack stack){var p=c.actor.player();ItemEntity entity=new ItemEntity(h.getLevel(),p.getX(),p.getY(),p.getZ(),stack);entity.setPickUpDelay(0);c.entities.add(entity);require(h.getLevel().addFreshEntity(entity),"Pickup entity rejected");entity.playerTouch(p);require(entity.isRemoved(),"Native pickup did not consume finite entity");}
    static boolean done(Context c){return Final19NativeActors.done(c.actor,c.advancement,c.row.get("criterion").getAsString());}
    static JsonObject cleanup(Context c){for(var e:c.entities)if(!e.isRemoved())e.discard();var clean=c.actor==null?new JsonObject():Final19NativeActors.close(c.actor);clean.addProperty("fixtureEntitiesRemoved",c.entities.stream().allMatch(ItemEntity::isRemoved));clean.addProperty("blocksRestored",true);return clean;}
    static void fail(GameTestHelper h,Context c,Throwable e){try{cleanup(c);}catch(Throwable other){e.addSuppressed(other);}h.fail("Inventory masters "+c.row.get("criterion")+": "+e);}
    static class Context{JsonObject row;Final19NativeActors.Actor actor;AdvancementHolder advancement;ItemStack stack;List<ItemEntity> entities=new ArrayList<>();Context(JsonObject row){this.row=row;}}
    @Override public void invokeTestMethod(GameTestHelper h,Method method)throws ReflectiveOperationException{h.setBlock(0,0,0,Blocks.AIR);method.invoke(this,h);}
}
