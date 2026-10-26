package com.diskree.achievetodo.gameplay.nativeguard;

import java.lang.reflect.Field;
import java.util.*;

/** Canonical conditional operations. Boolean wrappers preserve the caller's native fallback branch. */
public final class NativeLandmarkOperations {
    private static Class<?> type(Object owner,String suffix) throws ClassNotFoundException {
        return Class.forName("net.minecraft."+suffix,false,owner.getClass().getClassLoader());
    }
    private static Object staticCall(Class<?> owner,String name,Object...args) throws ReflectiveOperationException {
        for (var method:owner.getMethods()) {
            if (!method.getName().equals(name)||method.getParameterCount()!=args.length) continue;
            boolean matches=true;
            for (int i=0;i<args.length;i++) if (args[i]!=null&&!method.getParameterTypes()[i].isInstance(args[i])) { matches=false;break; }
            if (matches) return method.invoke(null,args);
        }
        throw new NoSuchMethodException(owner.getName()+"."+name);
    }
    private static Object staticField(Object owner,String suffix,String name) throws ReflectiveOperationException {
        var field=type(owner,suffix).getDeclaredField(name);field.setAccessible(true);return field.get(null);
    }
    private static Object field(Object owner,String name) throws ReflectiveOperationException {
        for (Class<?> type=owner.getClass();type!=null;type=type.getSuperclass()) {
            try { Field field=type.getDeclaredField(name);field.setAccessible(true);return field.get(owner); }
            catch (NoSuchFieldException missing) { /* Search the declared native superclass. */ }
        }
        throw new NoSuchFieldException(name);
    }
    static Object mount(Object menu) throws ReflectiveOperationException { return field(menu,"mount"); }
    public static boolean mountDenied(Object actor,Object menu,int index,boolean click) {
        try {
            if (!Class.forName("net.minecraft.world.inventory.AbstractMountInventoryMenu",false,menu.getClass().getClassLoader()).isInstance(menu)) return false;
            if (click) {
                if (!(boolean)NativeLandmarkCapture.call(menu,"isValidSlotIndex",index)) return false;
            } else {
                var slots=(List<?>)field(menu,"slots");
                if (index<0||index>=slots.size()||!(boolean)NativeLandmarkCapture.call(slots.get(index),"hasItem")) return false;
            }
            if (!NativeLandmarkActions.entityDenied(actor,mount(menu))) return false;
            NativeLandmarkCapture.call(menu,"sendAllDataToRemote");
            return true;
        } catch (ReflectiveOperationException|RuntimeException|LinkageError unavailable) { return true; }
    }
    public static boolean contextEmpty(Object list,Object context) {
        return ((List<?>)list).isEmpty()&&!NativeLandmarkActions.contextDenied(context);
    }
    public static Object filterLeashes(Object result,Object actor,Object level,Object position) {
        var list=(List<?>)result;
        // Pinned Paper returns Stream.toList(); canonical Fabric's mutable result.clear()
        // means an empty local result, not a requirement to mutate that implementation.
        if (!list.isEmpty()&&NativeLandmarkActions.blockDenied(actor,level,position)) return List.of();
        return result;
    }
    public static boolean leashShearsDenied(Object actor,Object target) {
        try {
            Class<?> leash=type(target,"world.entity.Leashable");
            var incoming=(List<?>)staticCall(leash,"leashableLeashedTo",target);
            boolean own=leash.isInstance(target)&&(boolean)NativeLandmarkCapture.call(target,"isLeashed");
            if (!own&&incoming.isEmpty()) return false;
            var affected=new LinkedHashSet<Object>();affected.add(target);
            if (own) {
                Object holder=NativeLandmarkCapture.call(target,"getLeashHolder");if(holder!=null) affected.add(holder);
            }
            affected.addAll(incoming);
            for (Object entity:affected) if (NativeLandmarkActions.entityDenied(actor,entity)) return true;
            return false;
        } catch (ReflectiveOperationException|RuntimeException|LinkageError unavailable) { return true; }
    }
    public static boolean equipmentShearsDenied(Object actor,Object target) {
        try {
            Object componentType=staticField(target,"core.component.DataComponents","EQUIPPABLE");
            Object prevent=staticField(target,"world.item.enchantment.EnchantmentEffectComponents","PREVENT_ARMOR_CHANGE");
            for (Object slot:type(target,"world.entity.EquipmentSlot").getEnumConstants()) {
                Object equipment=NativeLandmarkCapture.call(target,"getItemBySlot",slot);
                Object component=NativeLandmarkCapture.call(equipment,"get",componentType);
                if (component!=null&&(boolean)NativeLandmarkCapture.call(component,"canBeSheared")
                    && (!(boolean)staticCall(type(target,"world.item.enchantment.EnchantmentHelper"),"has",equipment,prevent)
                        || (boolean)NativeLandmarkCapture.call(actor,"isCreative"))) return NativeLandmarkActions.entityDenied(actor,target);
            }
            return false;
        } catch (ReflectiveOperationException|RuntimeException|LinkageError unavailable) { return true; }
    }
    public static void releaseBrush(Object actor,Object level,Object hit) {
        if (NativeLandmarkAgent.targetAndAbilityDenied(actor,level,NativeLandmarkCapture.call(hit,"getBlockPos"),"USE_BRUSH"))
            NativeLandmarkCapture.call(actor,"releaseUsingItem");
        // Canonical injection is noncancellable: this native tick continues after releaseUsingItem.
    }
    public static boolean sleepDenied(Object actor,Object position) {
        return NativeLandmarkAgent.targetAndAbilityDenied(actor,NativeLandmarkCapture.call(actor,"level"),position,"SLEEP");
    }
    public static boolean shelfSingleDenied(Object actor,Object level,Object position,Object stack,Object shelf,Object slot) {
        if ((boolean)NativeLandmarkCapture.call(stack,"isEmpty")
            && (boolean)NativeLandmarkCapture.call(NativeLandmarkCapture.call(shelf,"getItem",((OptionalInt)slot).getAsInt()),"isEmpty")) return false;
        return NativeLandmarkAgent.targetAndAbilityDenied(actor,level,position,"OPEN_CHEST");
    }
    public static boolean shelfPoweredDenied(Object actor,Object block,Object level,Object position) {
        try {
            var chain=(List<?>)NativeLandmarkCapture.call(block,"getAllBlocksConnectedTo",level,position);
            var affected=new ArrayList<Object>();boolean transfers=false;
            Object inventory=NativeLandmarkCapture.call(actor,"getInventory");
            for (int index=0;index<chain.size();index++) {
                Object shelf=NativeLandmarkCapture.call(level,"getBlockEntity",chain.get(index));
                if (shelf==null) continue;
                if (!type(block,"world.level.block.entity.ShelfBlockEntity").isInstance(shelf)) return false;
                int size=(int)NativeLandmarkCapture.call(shelf,"getContainerSize");
                for (int slot=0;slot<size;slot++) {
                    int inventorySlot=9-(chain.size()-index)*size+slot;
                    if (inventorySlot>=0&&inventorySlot<=(int)NativeLandmarkCapture.call(inventory,"getContainerSize")) {
                        if (!affected.contains(chain.get(index))) affected.add(chain.get(index));
                        transfers|=!(boolean)NativeLandmarkCapture.call(NativeLandmarkCapture.call(inventory,"getItem",inventorySlot),"isEmpty")
                            || !(boolean)NativeLandmarkCapture.call(NativeLandmarkCapture.call(shelf,"getItem",slot),"isEmpty");
                    }
                }
            }
            if (!transfers) return false;
            boolean first=true;
            for (Object target:affected) {
                if (NativeLandmarkAgent.targetAndAbilityDenied(actor,level,target,first?"OPEN_CHEST":null)) return true;
                first=false;
            }
            return false;
        } catch (ReflectiveOperationException|RuntimeException|LinkageError unavailable) { return true; }
    }
    public static boolean shelfFallbackDenied(Object state,Object result) {
        try {
            return type(state,"world.level.block.ShelfBlock").isInstance(NativeLandmarkCapture.call(state,"getBlock"))
                && result==staticField(state,"world.InteractionResult","FAIL");
        } catch (ReflectiveOperationException|RuntimeException|LinkageError unavailable) { return true; }
    }
    public static boolean decorationAllowed(boolean original,Object actor,Object position,Object stack) {
        if (!original) return false;
        try {
            return !type(actor,"world.item.HangingEntityItem").isInstance(NativeLandmarkCapture.call(stack,"getItem"))
                || !NativeLandmarkActions.blockDenied(actor,NativeLandmarkCapture.call(actor,"level"),position);
        } catch (ReflectiveOperationException|RuntimeException|LinkageError unavailable) { return false; }
    }
    public static boolean cauldronFallbackDenied(Object actor,Object level,Object stack,Object hit) {
        try {
            if ((boolean)NativeLandmarkCapture.call(actor,"isSecondaryUseActive")
                && (!(boolean)NativeLandmarkCapture.call(NativeLandmarkCapture.call(actor,"getMainHandItem"),"isEmpty")
                    || !(boolean)NativeLandmarkCapture.call(NativeLandmarkCapture.call(actor,"getOffhandItem"),"isEmpty"))) return false;
            Object position=NativeLandmarkCapture.call(hit,"getBlockPos");
            Object block=NativeLandmarkCapture.call(NativeLandmarkCapture.call(level,"getBlockState",position),"getBlock");
            if (!type(block,"world.level.block.AbstractCauldronBlock").isInstance(block)) return false;
            Object behavior=NativeLandmarkCapture.call(field(block,"interactions"),"get",stack);
            return behavior!=staticField(block,"core.cauldron.CauldronInteraction","DEFAULT")
                && NativeLandmarkAgent.targetAndAbilityDenied(actor,level,position,"USE_CAULDRON");
        } catch (ReflectiveOperationException|RuntimeException|LinkageError unavailable) { return true; }
    }
    private static boolean linkedDenied(Object actor,Object level,Object position,Object state) throws ReflectiveOperationException {
        Object block=NativeLandmarkCapture.call(state,"getBlock");
        if (type(block,"world.level.block.DoorBlock").isInstance(block)) {
            Object property=staticField(block,"world.level.block.DoorBlock","HALF");
            Object half=NativeLandmarkCapture.call(state,"getValue",property);
            Object other=NativeLandmarkCapture.call(position,((Enum<?>)half).name().equals("LOWER")?"above":"below");
            Object partner=NativeLandmarkCapture.call(level,"getBlockState",other);
            if (type(block,"world.level.block.DoorBlock").isInstance(NativeLandmarkCapture.call(partner,"getBlock"))
                && NativeLandmarkCapture.call(partner,"getValue",property)!=half
                && NativeLandmarkActions.blockDenied(actor,level,other)) return true;
        }
        if (type(block,"world.level.block.CopperChestBlock").isInstance(block)
            && !((Enum<?>)NativeLandmarkCapture.call(state,"getValue",staticField(block,"world.level.block.ChestBlock","TYPE"))).name().equals("SINGLE")) {
            Object other=staticCall(type(block,"world.level.block.ChestBlock"),"getConnectedBlockPos",position,state);
            return NativeLandmarkActions.blockDenied(actor,level,other);
        }
        return false;
    }
    public static boolean waxDenied(Object context) {
        try {
            Object level=NativeLandmarkCapture.call(context,"getLevel"),position=NativeLandmarkCapture.call(context,"getClickedPos");
            Object state=NativeLandmarkCapture.call(level,"getBlockState",position);
            if (((Optional<?>)staticCall(type(context,"world.item.HoneycombItem"),"getWaxed",state)).isEmpty()) return false;
            Object actor=NativeLandmarkCapture.call(context,"getPlayer");
            return NativeLandmarkActions.blockDenied(actor,level,position)||linkedDenied(actor,level,position,state);
        } catch (ReflectiveOperationException|RuntimeException|LinkageError unavailable) { return true; }
    }
    public static boolean axeDenied(Object actor,Object item,Object level,Object position,Object state) {
        if (actor==null) return false;
        try {
            Object block=NativeLandmarkCapture.call(state,"getBlock");
            boolean candidate=((Map<?,?>)staticField(item,"world.item.AxeItem","STRIPPABLES")).containsKey(block);
            candidate|=((Optional<?>)staticCall(type(item,"world.level.block.WeatheringCopper"),"getPrevious",state)).isPresent();
            candidate|=((Map<?,?>)((java.util.function.Supplier<?>)staticField(item,"world.item.HoneycombItem","WAX_OFF_BY_BLOCK")).get()).containsKey(block);
            return candidate&&(NativeLandmarkAgent.targetAndAbilityDenied(actor,level,position,NativeToolAgent.identity(item,false))
                || linkedDenied(actor,level,position,state));
        } catch (ReflectiveOperationException|RuntimeException|LinkageError unavailable) { return true; }
    }
    public static boolean boxEmpty(Object list,Object context,Object box) {
        if (!((List<?>)list).isEmpty()) return false;
        try {
            return !NativeLandmarkActions.boxDenied(NativeLandmarkCapture.call(context,"getPlayer"),
                NativeLandmarkCapture.call(context,"getLevel"),box,null);
        } catch (RuntimeException|LinkageError unavailable) { return false; }
    }
    public static boolean boatCollision(Object level,Object entity,Object box,Object actor) {
        try {
            return (boolean)NativeLandmarkCapture.call(level,"noCollision",entity,box)
                && !NativeLandmarkActions.boxDenied(actor,level,box,null);
        } catch (RuntimeException|LinkageError unavailable) { return false; }
    }
    public static boolean brushArmadillo(Object entity,Object user,Object stack,Object actor) {
        try {
            return !(boolean)NativeLandmarkCapture.call(entity,"isBaby")
                && !NativeLandmarkActions.entityDenied(actor,entity,"USE_BRUSH")
                && (boolean)NativeLandmarkCapture.call(entity,"brushOffScute",user,stack);
        } catch (RuntimeException|LinkageError unavailable) { return false; }
    }
    public static boolean offersEmpty(Object offers,Object actor,Object entity) {
        if (((List<?>)offers).isEmpty()) return true;
        try {
            Object profession=NativeLandmarkCapture.call(NativeLandmarkCapture.call(entity,"getVillagerData"),"profession");
            var key=(Optional<?>)NativeLandmarkCapture.call(profession,"unwrapKey");String ability=null;
            if (key.isPresent()) {
                String id=NativeLandmarkCapture.call(key.get(),"identifier").toString();
                if (id.startsWith("minecraft:")) {
                    String name=id.substring(10);
                    if (Set.of("armorer","butcher","cartographer","cleric","farmer","fisherman","fletcher","leatherworker",
                        "librarian","mason","shepherd","toolsmith","weaponsmith").contains(name))
                        ability="TRADE_WITH_"+name.toUpperCase(Locale.ROOT);
                }
            }
            return NativeLandmarkActions.entityDenied(actor,entity,ability);
        } catch (RuntimeException|LinkageError unavailable) { return true; }
    }
    public static Object cauldron(Object behavior,Object state,Object level,Object position,Object actor,Object hand,Object stack,Object face) {
        try {
            Class<?> type=Class.forName("net.minecraft.core.cauldron.CauldronInteraction",true,behavior.getClass().getClassLoader());
            if (behavior!=type.getField("DEFAULT").get(null)
                && NativeLandmarkAgent.targetAndAbilityDenied(actor,level,position,"USE_CAULDRON"))
                return NativeLandmarkActions.interaction(actor,"SUCCESS");
            return NativeLandmarkCapture.call(behavior,"interact",state,level,position,actor,hand,stack,face);
        } catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
    }
    public static boolean shulkerAllowed(boolean original,Object actor,Object entity,Object level,Object position) {
        if (!original) return false;
        try {
            return !NativeLandmarkCapture.call(entity,"getAnimationStatus").toString().equals("CLOSED")
                || !NativeLandmarkActions.blockDenied(actor,level,position);
        } catch (RuntimeException|LinkageError unavailable) { return false; }
    }
    public static boolean candidateEntityDenied(Object actor,Object entity,Object hand,String mode) {
        try {
            switch(mode) {
                case "happyRide":
                    return ((List<?>)NativeLandmarkCapture.call(entity,"getPassengers")).size()<4
                        && NativeLandmarkActions.entityDenied(actor,entity);
                case "nautilusRide":
                    return !(boolean)NativeLandmarkCapture.call(entity,"isVehicle")&&NativeLandmarkActions.entityDenied(actor,entity);
                case "wolfShears": {
                    Object stack=NativeLandmarkCapture.call(actor,"getItemInHand",hand);
                    Object shears=Class.forName("net.minecraft.world.item.Items",true,entity.getClass().getClassLoader()).getField("SHEARS").get(null);
                    return (boolean)NativeLandmarkCapture.call(stack,"is",shears)&&NativeLandmarkActions.entityDenied(actor,entity,"USE_SHEARS");
                }
                case "sulfurReplacement": {
                    Object bodySlot=Class.forName("net.minecraft.world.entity.EquipmentSlot",true,entity.getClass().getClassLoader()).getField("BODY").get(null);
                    Object body=NativeLandmarkCapture.call(entity,"getItemBySlot",bodySlot);
                    Object stack=NativeLandmarkCapture.call(actor,"getItemInHand",hand);
                    if (!(boolean)NativeLandmarkCapture.call(body,"isEmpty")
                        && (boolean)NativeLandmarkCapture.call(stack,"is",NativeLandmarkCapture.call(body,"getItem"))) return false;
                    return NativeLandmarkActions.entityDenied(actor,entity);
                }
                case "copperInteraction":return copperDenied(actor,entity,hand);
                default:throw new IllegalArgumentException(mode);
            }
        } catch (ReflectiveOperationException|RuntimeException|LinkageError unavailable) { return true; }
    }
    static boolean hasTag(Object stack,Object tag) throws ReflectiveOperationException {
        // ItemStack also declares is(Object); assignable-argument lookup can select that item comparison.
        // TagKey is final, so its exact runtime class selects the native tag-membership overload.
        return (boolean)stack.getClass().getMethod("is",tag.getClass()).invoke(stack,tag);
    }
    private static boolean copperDenied(Object actor,Object entity,Object hand) throws ReflectiveOperationException {
        Object stack=NativeLandmarkCapture.call(actor,"getItemInHand",hand);
        ClassLoader loader=entity.getClass().getClassLoader();Class<?> items=Class.forName("net.minecraft.world.item.Items",true,loader);
        String ability=null;
        if ((boolean)NativeLandmarkCapture.call(stack,"isEmpty")&&!(boolean)NativeLandmarkCapture.call(NativeLandmarkCapture.call(entity,"getMainHandItem"),"isEmpty")) {
            // Retrieval precedes all item branches.
        } else if ((boolean)NativeLandmarkCapture.call(stack,"is",items.getField("SHEARS").get(null))
            &&(boolean)NativeLandmarkCapture.call(entity,"readyForShearing")) ability="USE_SHEARS";
        else if ((boolean)NativeLandmarkCapture.call(NativeLandmarkCapture.call(entity,"level"),"isClientSide")) return false;
        else if ((boolean)NativeLandmarkCapture.call(stack,"is",items.getField("HONEYCOMB").get(null))&&(long)field(entity,"nextWeatheringTick")!=-2L) {
            // Waxing has no material permission.
        } else {
            Object axes=Class.forName("net.minecraft.tags.ItemTags",true,loader).getField("AXES").get(null);
            if (!hasTag(stack,axes)
                || ((long)field(entity,"nextWeatheringTick")!=-2L&&NativeLandmarkCapture.call(entity,"getWeatherState").toString().equals("UNAFFECTED"))) return false;
            ability=NativeToolAgent.identity(NativeLandmarkCapture.call(stack,"getItem"),false);
        }
        return NativeLandmarkActions.entityDenied(actor,entity,ability);
    }
    private NativeLandmarkOperations() {}
}
