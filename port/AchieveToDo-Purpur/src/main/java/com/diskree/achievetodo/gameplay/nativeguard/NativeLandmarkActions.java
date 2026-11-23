package com.diskree.achievetodo.gameplay.nativeguard;

import java.lang.classfile.*;
import java.lang.classfile.instruction.*;
import java.lang.constant.*;
import java.util.*;

/** Conditional original transaction anchors, opt-in only, sharing the single S4G target guard. */
public final class NativeLandmarkActions {
    private static final ClassDesc SELF = ClassDesc.of(NativeLandmarkActions.class.getName());
    private static final ClassDesc OBJECT = ConstantDescs.CD_Object;
    private static final ClassDesc OPERATIONS = ClassDesc.of(NativeLandmarkOperations.class.getName());
    static byte[] transform(String name, byte[] bytes, ClassLoader loader) {
        return transform(name, bytes, loader, NativeLandmarkActionSpecs.SPECS);
    }
    static byte[] transform(String name, byte[] bytes, ClassLoader loader, List<NativeLandmarkActionSpecs.Spec> allSpecs) {
        var cf = ClassFile.of(ClassFile.ClassHierarchyResolverOption.of(ClassHierarchyResolver.ofResourceParsing(loader)));
        var specs = allSpecs.stream().filter(s -> s.owner().equals(name)).toList();
        Map<NativeLandmarkActionSpecs.Spec, Integer> counts = new HashMap<>();
        var result = cf.transformClass(cf.parse(bytes), (cb, element) -> {
            if (!(element instanceof MethodModel m)) { cb.with(element); return; }
            var selected = specs.stream().filter(s -> s.method().equals(m.methodName().stringValue())
                && s.descriptor().equals(m.methodType().stringValue())).toList();
            if (selected.isEmpty()) { cb.with(element); return; }
            cb.transformMethod(m, MethodTransform.transformingCode(new CodeTransform() {
                final Map<String,Integer> ordinals = new HashMap<>();
                int returnOrdinal;
                final long returns = m.code().orElseThrow().elementStream().filter(e -> e instanceof ReturnInstruction).count();
                @Override public void atStart(CodeBuilder b) {
                    for (var s : selected) if (s.anchor().equals("HEAD")) { emit(b,s,m); counts.merge(s,1,Integer::sum); }
                }
                @Override public void accept(CodeBuilder b, CodeElement e) {
                    if (e instanceof ReturnInstruction) {
                        int ordinal = returnOrdinal++;
                        for (var s : selected) if ((s.anchor().equals("TAIL") && ordinal == returns-1)
                            || (s.anchor().equals("RETURN") && (s.ordinal()<0 || s.ordinal()==ordinal))) {
                            if (s.mode().equals("decoration")) b.aload(0).aload(1).aload(3)
                                .invokestatic(OPERATIONS,"decorationAllowed",MethodTypeDesc.of(ConstantDescs.CD_boolean,ConstantDescs.CD_boolean,OBJECT,OBJECT,OBJECT));
                            else b.aload(s.actor()).aload(0).ldc(s.mode())
                                .invokestatic(SELF,"entityReturnAllowed",MethodTypeDesc.of(ConstantDescs.CD_boolean,ConstantDescs.CD_boolean,OBJECT,OBJECT,ConstantDescs.CD_String));
                            counts.merge(s,1,Integer::sum);
                        }
                    }
                    if (e instanceof InvokeInstruction i) {
                        String key = "L"+i.owner().asInternalName()+";"+i.name().stringValue()+i.type().stringValue();
                        int ordinal = ordinals.merge(key,1,Integer::sum)-1;
                        boolean emitted = false;
                        for (var s : selected) if (s.anchor().equals(key) && (s.ordinal()<0 || s.ordinal()==ordinal)) {
                            // Identical canonical injections may share an anchor. One target query per transaction point.
                            if (!emitted) {
                                if (s.mode().startsWith("wrap:") || s.mode().startsWith("after:")) {
                                    emitOperation(b,s,i); emitted=true;
                                } else { emit(b,s,m); emitted=true; }
                            }
                            counts.merge(s,1,Integer::sum);
                        }
                        if (emitted && selected.stream().anyMatch(s -> s.anchor().equals(key)
                            && (s.ordinal()<0 || s.ordinal()==ordinal)
                            && (s.mode().startsWith("wrap:") || s.mode().startsWith("after:")))) return;
                    }
                    if (e instanceof FieldInstruction f) {
                        String key = "FIELD:L"+f.owner().asInternalName()+";"+f.name().stringValue()+":"+f.type().stringValue();
                        int ordinal = ordinals.merge(key,1,Integer::sum)-1;
                        boolean emitted = false;
                        for (var s : selected) if (s.anchor().equals(key) && (s.ordinal()<0 || s.ordinal()==ordinal)) {
                            if (!emitted) { emit(b,s,m);emitted=true; }
                            counts.merge(s,1,Integer::sum);
                        }
                    }
                    b.with(e);
                }
            }));
        });
        for (var s : specs) if (counts.getOrDefault(s,0) != s.sites())
            throw new IllegalStateException("Landmark anchor count " + s.id()+"="+counts.get(s)+" expected="+s.sites());
        return result;
    }
    private static void emitOperation(CodeBuilder b, NativeLandmarkActionSpecs.Spec s, InvokeInstruction original) {
        String operation=s.mode().substring(s.mode().indexOf(':')+1);
        int parameters; boolean objectResult=false;
        switch(operation) {
            case "cauldron" -> { parameters=8;objectResult=true; }
            case "brushArmadillo", "boatCollision" -> { b.aload(s.actor());parameters=4; }
            case "contextEmpty" -> { b.aload(s.actor());parameters=2; }
            case "boxEmpty" -> { b.aload(s.actor()).aload(s.pos());parameters=3; }
            case "offersGated" -> { b.aload(s.actor());parameters=2;objectResult=true; }
            case "filterLeashes" -> {
                b.with(original).aload(s.actor()).aload(s.level()).aload(s.pos())
                    .invokestatic(OPERATIONS,operation,MethodTypeDesc.of(OBJECT,OBJECT,OBJECT,OBJECT,OBJECT))
                    .checkcast(original.typeSymbol().returnType());return;
            }
            case "shulkerAllowed" -> {
                b.with(original).aload(s.actor()).aload(7).aload(s.level()).aload(s.pos())
                    .invokestatic(OPERATIONS,operation,MethodTypeDesc.of(ConstantDescs.CD_boolean,
                        ConstantDescs.CD_boolean,OBJECT,OBJECT,OBJECT,OBJECT));return;
            }
            default -> throw new IllegalStateException(s.mode());
        }
        ClassDesc[] arguments=new ClassDesc[parameters];Arrays.fill(arguments,OBJECT);
        b.invokestatic(OPERATIONS,operation,MethodTypeDesc.of(objectResult?OBJECT:ConstantDescs.CD_boolean,arguments));
        if (objectResult) b.checkcast(original.typeSymbol().returnType());
    }
    private static void emit(CodeBuilder b, NativeLandmarkActionSpecs.Spec s, MethodModel m) {
        if (s.mode().equals("brushTick")) {
            b.aload(s.actor()).aload(s.level()).aload(s.pos())
                .invokestatic(OPERATIONS,"releaseBrush",MethodTypeDesc.of(ConstantDescs.CD_void,OBJECT,OBJECT,OBJECT));return;
        }
        var pass = b.newLabel();
        b.aload(s.actor());
        switch (s.mode()) {
            case "context" -> b.invokestatic(SELF,"contextDenied",MethodTypeDesc.of(ConstantDescs.CD_boolean,OBJECT));
            case "blockTransform" -> b.invokestatic(OPERATIONS,"blockTransformDenied",MethodTypeDesc.of(ConstantDescs.CD_boolean,OBJECT));
            case "wax" -> b.invokestatic(OPERATIONS,"waxDenied",MethodTypeDesc.of(ConstantDescs.CD_boolean,OBJECT));
            case "cauldronFallback" -> b.aload(s.level()).aload(3).aload(5)
                .invokestatic(OPERATIONS,"cauldronFallbackDenied",MethodTypeDesc.of(ConstantDescs.CD_boolean,OBJECT,OBJECT,OBJECT,OBJECT));
            case "sleep" -> b.aload(s.pos()).invokestatic(OPERATIONS,"sleepDenied",MethodTypeDesc.of(ConstantDescs.CD_boolean,OBJECT,OBJECT));
            case "shelfSingle" -> b.aload(s.level()).aload(s.pos()).aload(1).aload(8).aload(9)
                .invokestatic(OPERATIONS,"shelfSingleDenied",MethodTypeDesc.of(ConstantDescs.CD_boolean,OBJECT,OBJECT,OBJECT,OBJECT,OBJECT,OBJECT));
            case "shelfPowered" -> b.aload(0).aload(s.level()).aload(s.pos())
                .invokestatic(OPERATIONS,"shelfPoweredDenied",MethodTypeDesc.of(ConstantDescs.CD_boolean,OBJECT,OBJECT,OBJECT,OBJECT));
            case "shelfFallback" -> b.aload(s.pos()).invokestatic(OPERATIONS,"shelfFallbackDenied",MethodTypeDesc.of(ConstantDescs.CD_boolean,OBJECT,OBJECT));
            case "block" -> {
                String opening=switch(s.owner()) {
                    case "net/minecraft/world/level/block/DoorBlock" -> "OPEN_DOOR";
                    case "net/minecraft/world/level/block/TrapDoorBlock" -> "OPEN_TRAPDOOR";
                    case "net/minecraft/world/level/block/FenceGateBlock" -> "OPEN_FENCE_GATE";
                    default -> null;
                };
                b.aload(s.level()).aload(s.pos());
                String menuAbility = s.method().equals("useWithoutItem") ? switch(s.owner()) {
                    case "net/minecraft/world/level/block/CraftingTableBlock" -> "OPEN_CRAFTING_TABLE";
                    case "net/minecraft/world/level/block/GrindstoneBlock" -> "OPEN_GRINDSTONE";
                    case "net/minecraft/world/level/block/CartographyTableBlock" -> "OPEN_CARTOGRAPHY_TABLE";
                    case "net/minecraft/world/level/block/LoomBlock" -> "OPEN_LOOM";
                    case "net/minecraft/world/level/block/EnchantingTableBlock" -> "OPEN_ENCHANTING_TABLE";
                    default -> null;
                } : null;
                if (s.id().equals("AbstractFurnaceBlockMixin:40"))
                    b.aload(0).invokestatic(SELF, "furnaceDenied",
                        MethodTypeDesc.of(ConstantDescs.CD_boolean, OBJECT, OBJECT, OBJECT, OBJECT));
                else if(menuAbility != null)
                    b.ldc(menuAbility)
                        .invokestatic(ClassDesc.of(NativeLandmarkAgent.class.getName()),"targetAndAbilityDenied",
                            MethodTypeDesc.of(ConstantDescs.CD_boolean,OBJECT,OBJECT,OBJECT,ConstantDescs.CD_String));
                else if(opening==null)b.invokestatic(SELF,"blockDenied",MethodTypeDesc.of(ConstantDescs.CD_boolean,OBJECT,OBJECT,OBJECT));
                else b.aload(1).ldc(s.owner().replace('/','.')).ldc(opening)
                    .invokestatic(SELF,"openingDenied",MethodTypeDesc.of(ConstantDescs.CD_boolean,OBJECT,OBJECT,OBJECT,OBJECT,ConstantDescs.CD_String,ConstantDescs.CD_String));
            }
            case "entity" -> b.aload(0).invokestatic(SELF,"entityDenied",MethodTypeDesc.of(ConstantDescs.CD_boolean,OBJECT,OBJECT));
            case "injuredEntity" -> b.aload(0).invokestatic(SELF,"injuredEntityDenied",MethodTypeDesc.of(ConstantDescs.CD_boolean,OBJECT,OBJECT));
            case "leashShears", "equipmentShears" -> b.aload(0).invokestatic(OPERATIONS,
                s.mode()+"Denied",MethodTypeDesc.of(ConstantDescs.CD_boolean,OBJECT,OBJECT));
            case "mountQuickMove", "mountClick" -> b.aload(0).iload(s.pos()).ldc(s.mode().equals("mountClick")?1:0)
                .invokestatic(OPERATIONS,"mountDenied",MethodTypeDesc.of(ConstantDescs.CD_boolean,OBJECT,OBJECT,ConstantDescs.CD_int,ConstantDescs.CD_boolean));
            case "happyRide", "nautilusRide", "wolfShears", "sulfurReplacement", "copperInteraction" -> b.aload(0).aload(2).ldc(s.mode())
                .invokestatic(OPERATIONS,"candidateEntityDenied",MethodTypeDesc.of(ConstantDescs.CD_boolean,OBJECT,OBJECT,OBJECT,ConstantDescs.CD_String));
            case "targetEntity" -> {
                b.aload(s.pos());
                if(s.owner().equals("net/minecraft/world/item/equipment/Equippable") && s.method().equals("equipOnTarget"))
                    b.aload(3).invokestatic(SELF,"equipmentEntityDenied",MethodTypeDesc.of(ConstantDescs.CD_boolean,OBJECT,OBJECT,OBJECT));
                else b.invokestatic(SELF,s.owner().equals("net/minecraft/world/entity/player/Player") && s.method().equals("attack")
                    ?"combatEntityDenied":"entityDenied",MethodTypeDesc.of(ConstantDescs.CD_boolean,OBJECT,OBJECT));
            }
            case "damage" -> b.aload(0).invokestatic(SELF,"damageDenied",MethodTypeDesc.of(ConstantDescs.CD_boolean,OBJECT,OBJECT));
            case "playerBlock" -> b.aload(s.level()).aload(s.pos()).invokestatic(SELF,"playerBlockDenied",MethodTypeDesc.of(ConstantDescs.CD_boolean,OBJECT,OBJECT,OBJECT));
            case "projectile", "pointedProjectile" -> b.aload(s.level()).aload(s.pos()).aload(0).ldc(s.mode())
                .invokestatic(SELF,"projectileDenied",MethodTypeDesc.of(ConstantDescs.CD_boolean,OBJECT,OBJECT,OBJECT,OBJECT,ConstantDescs.CD_String));
            default -> throw new IllegalStateException(s.mode());
        }
        b.ifeq(pass);
        if (s.result().equals("void")) b.return_();
        else if (s.result().equals("false")) b.iconst_0().ireturn();
        else if (s.result().equals("itemEmpty")) b.getstatic(ClassDesc.of("net.minecraft.world.item.ItemStack"),"EMPTY",ClassDesc.of("net.minecraft.world.item.ItemStack")).areturn();
        else if (s.result().equals("state3")) b.aload(3).areturn();
        else if (s.result().equals("optionalEmpty")) b.invokestatic(ClassDesc.of("java.util.Optional"),"empty",MethodTypeDesc.of(ClassDesc.of("java.util.Optional"))).areturn();
        else if (s.result().equals("sleepFailure")) {
            var problem=ClassDesc.of("net.minecraft.world.entity.player.Player$BedSleepingProblem");
            b.getstatic(problem,"OTHER_PROBLEM",problem).invokestatic(ClassDesc.of("com.mojang.datafixers.util.Either"),"left",
                MethodTypeDesc.of(ClassDesc.of("com.mojang.datafixers.util.Either"),OBJECT)).areturn();
        }
        else if (s.result().equals("optionalFail")) b.aload(s.actor()).ldc("FAIL")
            .invokestatic(SELF,"interaction",MethodTypeDesc.of(OBJECT,OBJECT,ConstantDescs.CD_String))
            .invokestatic(ClassDesc.of("java.util.Optional"),"of",MethodTypeDesc.of(ClassDesc.of("java.util.Optional"),OBJECT)).areturn();
        else b.aload(s.actor()).ldc(s.result()).invokestatic(SELF,"interaction",MethodTypeDesc.of(OBJECT,OBJECT,ConstantDescs.CD_String))
            .checkcast(m.methodTypeSymbol().returnType()).areturn();
        b.labelBinding(pass);
    }
    public static boolean contextDenied(Object context) {
        try {
            return blockDenied(NativeLandmarkCapture.call(context,"getPlayer"),NativeLandmarkCapture.call(context,"getLevel"),
                NativeLandmarkCapture.call(context,"getClickedPos"));
        } catch (RuntimeException | LinkageError unavailable) { return true; }
    }
    public static boolean furnaceDenied(Object actor, Object level, Object pos, Object furnace) {
        String ability = furnaceAbility(furnace.getClass());
        return NativeLandmarkAgent.targetAndAbilityDenied(actor, level, pos, ability);
    }

    private static String furnaceAbility(Class<?> type) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            String ability = switch (current.getName()) {
                case "net.minecraft.world.level.block.FurnaceBlock" -> "OPEN_FURNACE";
                case "net.minecraft.world.level.block.SmokerBlock" -> "OPEN_SMOKER";
                case "net.minecraft.world.level.block.BlastFurnaceBlock" -> "OPEN_BLAST_FURNACE";
                default -> null;
            };
            if (ability != null) return ability;
        }
        return null;
    }

    public static boolean blockDenied(Object actor, Object level, Object position) {
        return NativeLandmarkAgent.targetDenied(actor,level,position);
    }
    private static boolean player(Object actor) {
        for (Class<?> type = actor == null ? null : actor.getClass(); type != null; type = type.getSuperclass())
            if (type.getName().equals("net.minecraft.world.entity.player.Player")) return true;
        return false;
    }
    public static boolean playerBlockDenied(Object actor, Object level, Object position) {
        if (!player(actor)) return false;
        try {
            if (!Class.forName("net.minecraft.world.level.Level",false,level.getClass().getClassLoader()).isInstance(level)) return false;
            return blockDenied(actor,level,position);
        } catch (ClassNotFoundException | RuntimeException | LinkageError unavailable) { return true; }
    }
    public static boolean projectileDenied(Object projectile, Object level, Object hit, Object block, String mode) {
        try {
            if (mode.equals("pointedProjectile") && !Class.forName("net.minecraft.world.level.block.PointedDripstoneBlock",
                false,block.getClass().getClassLoader()).isInstance(block)) return false;
            Object actor = NativeLandmarkCapture.call(projectile,"getOwner");
            return playerBlockDenied(actor,level,NativeLandmarkCapture.call(hit,"getBlockPos"));
        } catch (ClassNotFoundException | RuntimeException | LinkageError unavailable) { return true; }
    }
    public static boolean damageDenied(Object source, Object entity) {
        try {
            Object actor = NativeLandmarkCapture.call(source,"getEntity");
            return player(actor) && entityDenied(actor,entity);
        } catch (RuntimeException | LinkageError unavailable) { return true; }
    }
    public static boolean injuredEntityDenied(Object actor, Object entity) {
        try {
            return (float)NativeLandmarkCapture.call(entity,"getHealth") < (float)NativeLandmarkCapture.call(entity,"getMaxHealth")
                && entityDenied(actor,entity);
        } catch (RuntimeException | LinkageError unavailable) { return true; }
    }
    public static boolean entityReturnAllowed(boolean original, Object argument, Object entity, String mode) {
        if (!original) return false;
        try {
            if (mode.equals("mountValidity")) return !entityQueryDenied(argument,NativeLandmarkOperations.mount(entity));
            if (mode.equals("projectileEntity")) {
                Object actor = NativeLandmarkCapture.call(entity,"getOwner");
                return !player(actor) || actor == argument || !entityDenied(actor,argument);
            }
            if (!player(argument)) return true;
            if (mode.equals("passengerMinecart") && !Class.forName("net.minecraft.world.entity.vehicle.minecart.Minecart",
                false,entity.getClass().getClassLoader()).isInstance(entity)) return true;
            return !entityDenied(argument,entity,mode.equals("passengerBoat") ? "GET_INTO_BOAT" : "GET_INTO_MINECART");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) { return false; }
    }
    public static boolean openingDenied(Object actor,Object level,Object position,Object state,String owner,String ability) {
        try {
            Object property=Class.forName(owner,false,state.getClass().getClassLoader()).getField("OPEN").get(null);
            boolean open=(boolean)NativeLandmarkCapture.call(state,"getValue",property);
            return NativeLandmarkAgent.targetAndAbilityDenied(actor,level,position,open?null:ability);
        } catch(ReflectiveOperationException | RuntimeException | LinkageError unavailable){return true;}
    }
    private static boolean entityQueryDenied(Object actor,Object entity) {
        if(actor==null)return false;
        try{return boxDenied(actor,NativeLandmarkCapture.call(entity,"level"),NativeLandmarkCapture.call(entity,"getBoundingBox"),null,"SILENT");}
        catch(RuntimeException | LinkageError unavailable){return true;}
    }
    public static boolean entityDenied(Object actor, Object entity) {
        return entityDenied(actor,entity,null);
    }
    static boolean entityDenied(Object actor, Object entity, String additionalAbility) {
        if (actor == null) return false;
        try {
        Object box = NativeLandmarkCapture.call(entity,"getBoundingBox");
        return boxDenied(actor,NativeLandmarkCapture.call(entity,"level"),box,additionalAbility);
        } catch (RuntimeException | LinkageError unavailable) { return true; }
    }
    private static boolean nativeType(Object target,String name) throws ClassNotFoundException {
        return Class.forName("net.minecraft."+name,false,target.getClass().getClassLoader()).isInstance(target);
    }
    private static boolean equipmentReports(Object player,Object target,Object stack) throws ReflectiveOperationException {
        var context=StackWalker.getInstance().walk(frames->frames.filter(f->f.getClassName().startsWith("net.minecraft.world.entity."))
            .map(f->f.getClassName()+"#"+f.getMethodName()).toList());
        boolean outer=context.contains("net.minecraft.world.entity.player.Player#interactOn");
        boolean subtype=context.stream().anyMatch(f->f.endsWith("#mobInteract"));
        if(outer && subtype)return false;
        boolean branch=false;
        if(nativeType(target,"world.entity.animal.camel.Camel") && !(boolean)NativeLandmarkCapture.call(target,"isBaby"))
            branch=(boolean)NativeLandmarkCapture.call(player,"isSecondaryUseActive")
                || !(boolean)NativeLandmarkCapture.call(target,"isFood",stack) && ((List<?>)NativeLandmarkCapture.call(target,"getPassengers")).size()<2;
        else if(nativeType(target,"world.entity.animal.nautilus.AbstractNautilus") && !(boolean)NativeLandmarkCapture.call(target,"isBaby")
                && (boolean)NativeLandmarkCapture.call(target,"isTame"))
            branch=(boolean)NativeLandmarkCapture.call(player,"isSecondaryUseActive")
                || !(boolean)NativeLandmarkCapture.call(target,"isFood",stack) && !(boolean)NativeLandmarkCapture.call(target,"isVehicle");
        else if(nativeType(target,"world.entity.animal.equine.AbstractHorse") && (boolean)NativeLandmarkCapture.call(target,"isTamed")
                && !(boolean)NativeLandmarkCapture.call(target,"isBaby") && !(boolean)NativeLandmarkCapture.call(target,"isVehicle"))
            branch=!(boolean)NativeLandmarkCapture.call(target,"isFood",stack);
        return !(branch && (outer || subtype));
    }
    public static boolean equipmentEntityDenied(Object actor,Object target,Object stack) {
        try{return boxDenied(actor,NativeLandmarkCapture.call(target,"level"),NativeLandmarkCapture.call(target,"getBoundingBox"),null,
            equipmentReports(actor,target,stack)?"ORDINARY":"SILENT");}
        catch(ReflectiveOperationException | RuntimeException | LinkageError unavailable){return true;}
    }
    public static boolean combatEntityDenied(Object actor,Object entity) {
        if(actor==null)return false;
        try{return boxDenied(actor,NativeLandmarkCapture.call(entity,"level"),NativeLandmarkCapture.call(entity,"getBoundingBox"),null,"COMBAT");}
        catch(RuntimeException | LinkageError unavailable){return true;}
    }
    static boolean boxDenied(Object actor,Object level,Object box,String additionalAbility) {
        return boxDenied(actor,level,box,additionalAbility,"ORDINARY");
    }
    static boolean boxDenied(Object actor,Object level,Object box,String additionalAbility,String reporting) {

        if (actor == null) return false;
        try {
        double[] bounds = new double[6];
        String[] fields = {"minX","minY","minZ","maxX","maxY","maxZ"};
        try { for (int i=0;i<6;i++) bounds[i]=box.getClass().getField(fields[i]).getDouble(box); }
        catch (ReflectiveOperationException failure) { return true; }
        return NativeLandmarkAgent.targetAndAbilityDenied(actor,level,bounds,additionalAbility,reporting);
        } catch (RuntimeException | LinkageError unavailable) { return true; }
    }
    public static Object interaction(Object nativeArgument, String field) {
        try { return Class.forName("net.minecraft.world.InteractionResult",true,nativeArgument.getClass().getClassLoader()).getField(field).get(null); }
        catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
    }
    private NativeLandmarkActions() {}
}
