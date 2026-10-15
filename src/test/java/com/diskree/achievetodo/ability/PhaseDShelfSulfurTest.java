package com.diskree.achievetodo.ability;

import com.google.gson.JsonParser;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShelfBlock;
import net.minecraft.world.level.block.entity.ShelfBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.InputStreamReader;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

/** P10/P7 independent native contracts. Gate interpreter executes compiled branch
 * instructions with explicit permission answers; it is not a gameplay simulation.
 * Real actors, eligibility, conservation and fallback are separately server-tested. */
@Isolated("Restores native item component bindings after real stack tests")
public class PhaseDShelfSulfurTest {
    private static final Map<Holder.Reference<?>,DataComponentMap> COMPONENTS=new IdentityHashMap<>();
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion(); Bootstrap.bootStrap();
        try {
            BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(VanillaRegistries.createLookup()).stream()
                .filter(p -> p.key().equals(Registries.ITEM)).forEach(p -> p.forEach((holder,components) -> {
                    COMPONENTS.put(holder,holder.areComponentsBound()?holder.components():null);holder.bindComponents(components);
                }));
        } catch(RuntimeException|Error e) { restore();throw e; }
    }
    @AfterAll static void restore() { COMPONENTS.forEach(Holder.Reference::bindComponents);COMPONENTS.clear(); }
    private static final String SHELF="net/minecraft/world/level/block/ShelfBlock";
    private static final String CUBE="net/minecraft/world/entity/monster/cubemob/SulfurCube";
    private static final String MIXINS="com/diskree/achievetodo/injection/mixin/main/";
    private static ClassNode node(String owner) throws Exception {
        try(var in=PhaseDShelfSulfurTest.class.getResourceAsStream("/"+owner+".class")) {
            assertNotNull(in,owner);var n=new ClassNode();new ClassReader(in).accept(n,0);return n;
        }
    }
    private static MethodNode method(String owner,String name) throws Exception {
        return node(owner).methods.stream().filter(m -> m.name.equals(name)).findFirst().orElseThrow();
    }
    private static List<MethodInsnNode> calls(MethodNode m) {
        return Arrays.stream(m.instructions.toArray()).filter(i -> i instanceof MethodInsnNode).map(i -> (MethodInsnNode)i).toList();
    }
    private static List<String> names(MethodNode m) { return calls(m).stream().map(i -> i.name).toList(); }
    @Test void exactP10P7DescriptorsAndBranchOrdering() throws Exception {
        assertEquals("(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;",method(SHELF,"useItemOn").desc);
        assertEquals("(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",method(CUBE,"mobInteract").desc);
        var c=names(method(CUBE,"mobInteract"));
        assertTrue(c.indexOf("canAgeUp")<c.indexOf("usePlayerItem"));assertTrue(c.indexOf("isPrimed")<c.indexOf("canExplode"));
        assertTrue(c.indexOf("primeTime")<c.indexOf("readyForShearing"));assertTrue(c.indexOf("readyForShearing")<c.indexOf("shear"));
        assertTrue(c.indexOf("shear")<c.indexOf("isSwallowableItem"));assertTrue(c.indexOf("equipItem")<c.indexOf("bucketMobPickup"));
        assertTrue(c.indexOf("primeTime")<c.indexOf("hurtAndBreak"));
    }
    static Stream<String> mixins() { return Stream.of("ShelfBlockMixin","SulfurCubeEntityMixin"); }
    @ParameterizedTest @MethodSource("mixins")
    void eachInjectorBindsAnExactInvocationAndNoAutonomousMethod(String name) throws Exception {
        var mixin=node(MIXINS+name);var target=node(name.equals("ShelfBlockMixin")?SHELF:CUBE);int count=0;
        for(var m:mixin.methods) if(m.visibleAnnotations!=null) for(var a:m.visibleAnnotations) {
            if(!a.desc.endsWith("/Inject;"))continue;count++;
            @SuppressWarnings("unchecked") var selectors=(List<String>)a.values.get(a.values.indexOf("method")+1);
            assertEquals(List.of(name.equals("ShelfBlockMixin")?"useItemOn":"mobInteract"),selectors);
            var nativeMethod=target.methods.stream().filter(n -> n.name.equals(selectors.getFirst())).findFirst().orElseThrow();
            @SuppressWarnings("unchecked") var ats=(List<AnnotationNode>)a.values.get(a.values.indexOf("at")+1);
            for(var at:ats) {
                assertEquals("INVOKE",at.values.get(at.values.indexOf("value")+1));
                String invocation=(String)at.values.get(at.values.indexOf("target")+1);
                assertEquals(1,calls(nativeMethod).stream().filter(c -> ("L"+c.owner+";"+c.name+c.desc).equals(invocation)).count());
            }
            assertEquals(true,a.values.get(a.values.indexOf("cancellable")+1));
        }
        assertEquals(name.equals("ShelfBlockMixin")?2:4,count);
    }
    static Stream<Direction> facings() { return Stream.of(Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST); }
    @ParameterizedTest @MethodSource("facings") void nativeHitSlotAndInvalidFace(Direction facing) {
        var shelf=(ShelfBlock)Blocks.OAK_SHELF;var pos=new BlockPos(3,8,5);
        assertEquals(1,shelf.getHitSlot(new BlockHitResult(Vec3.atCenterOf(pos),facing,pos,false),facing).orElseThrow());
        assertTrue(shelf.getHitSlot(new BlockHitResult(Vec3.atCenterOf(pos),facing.getOpposite(),pos,false),facing).isEmpty());
    }
    @Test void realNativeShelfSwapConservesBothStacksAndEmptyControls() {
        var shelf=new ShelfBlockEntity(BlockPos.ZERO,Blocks.OAK_SHELF.defaultBlockState());
        assertEquals(3,shelf.getContainerSize());shelf.setItem(1,new ItemStack(Items.DIAMOND,7));
        var source=new ItemStack(Items.EMERALD,11);var returned=shelf.swapItemNoUpdate(1,source);
        assertTrue(returned.is(Items.DIAMOND));assertEquals(7,returned.getCount());
        assertTrue(shelf.getItem(1).is(Items.EMERALD));assertEquals(11,shelf.getItem(1).getCount());
        assertEquals(18,returned.getCount()+shelf.getItem(1).getCount());
        assertTrue(shelf.swapItemNoUpdate(0,ItemStack.EMPTY).isEmpty());assertTrue(shelf.getItem(0).isEmpty());
    }
    @Test void nativeChainAndFirstIrreversibleMutationStayNative() throws Exception {
        var c=names(method(SHELF,"swapHotbar"));assertEquals("getAllBlocksConnectedTo",c.getFirst());
        assertTrue(c.indexOf("getBlockEntity")<c.indexOf("removeItemNoUpdate"));
        assertTrue(c.indexOf("removeItemNoUpdate")<c.indexOf("swapItemNoUpdate"));
        var guard=names(method(MIXINS+"ShelfBlockMixin","lockPoweredTransfer"));
        assertTrue(guard.contains("getAllBlocksConnectedTo"));assertTrue(guard.containsAll(List.of("getContainerSize","getItem","isAbilityLocked","isTargetInLockedLandmark")));
        assertFalse(guard.stream().anyMatch(n -> List.of("setItem","swapItemNoUpdate","removeItemNoUpdate","swapHotbar").contains(n)));
        var nativeChain=method("net/minecraft/world/level/block/SideChainPartBlock","getAllBlocksConnectedTo");
        assertTrue(nativeChain.instructions.size()>0);
    }
    @Test void actualNativeTagsAndGameruleControl() throws Exception {
        for(String n:List.of("isFood","isSwallowableItem")) {
            var m=method(CUBE,n);var fields=Arrays.stream(m.instructions.toArray()).filter(i -> i instanceof FieldInsnNode).map(i -> ((FieldInsnNode)i).name).toList();
            assertEquals(List.of(n.equals("isFood")?"SULFUR_CUBE_FOOD":"SULFUR_CUBE_SWALLOWABLE"),fields);
        }
        try(var in=getClass().getResourceAsStream("/data/minecraft/tags/item/sulfur_cube_food.json")) {
            assertNotNull(in);assertEquals("minecraft:slime_ball",JsonParser.parseReader(new InputStreamReader(in)).getAsJsonObject().getAsJsonArray("values").get(0).getAsString());
        }
        var nativeCalls=names(method(CUBE,"mobInteract"));assertTrue(nativeCalls.indexOf("sendOverlayMessage")<nativeCalls.indexOf("primeTime"));
        assertEquals(List.of("hasBodyItem"),names(method(CUBE,"readyForShearing")));
        var equip=names(method(CUBE,"equipItem"));assertTrue(equip.indexOf("isBaby")<equip.indexOf("spawnAtLocation"));assertTrue(equip.indexOf("is")<equip.indexOf("spawnAtLocation"));
        assertTrue(equip.indexOf("spawnAtLocation")<equip.indexOf("setItemSlotAndDropWhenKilled"));
    }
    record Gate(boolean denied,int notifications,List<AbilityType> consulted) {}
    // Restricted execution of the compiled handlers. Unsupported instructions fail
    // closed; native ItemStack.is and CallbackInfoReturnable are executed directly.
    private Gate execute(String handler,boolean flint,boolean sameBody,Set<AbilityType> locked,boolean landmark) throws Exception {
        var m=method(MIXINS+"SulfurCubeEntityMixin",handler);var code=m.instructions.toArray();
        var stack=new ArrayList<Object>();var locals=new HashMap<Integer,Object>();var callback=new CallbackInfoReturnable<InteractionResult>(handler,true);
        locals.put(0,this);locals.put(1,this);locals.put(2,this);locals.put(3,callback);
        var consulted=new ArrayList<AbilityType>();int notices=0;
        for(int pc=0;pc<code.length;pc++) {
            var i=code[pc];int op=i.getOpcode();if(op<0)continue;
            if(i instanceof VarInsnNode v) { if(op==Opcodes.ALOAD)stack.add(locals.get(v.var));else if(op==Opcodes.ASTORE)locals.put(v.var,stack.removeLast());else fail("Unsupported var "+op); }
            else if(i instanceof TypeInsnNode) assertEquals(Opcodes.CHECKCAST,op);
            else if(i instanceof FieldInsnNode f) {
                assertEquals(Opcodes.GETSTATIC,op);
                stack.add(f.owner.endsWith("AbilityType")?AbilityType.valueOf(f.name):f.owner.endsWith("Items")?Items.FLINT_AND_STEEL:InteractionResult.FAIL);
            } else if(i instanceof JumpInsnNode j) {
                boolean jump=op==Opcodes.GOTO;
                if(op==Opcodes.IFEQ)jump=!(Boolean)stack.removeLast();else if(op==Opcodes.IFNE)jump=(Boolean)stack.removeLast();else assertEquals(Opcodes.GOTO,op);
                if(jump)pc=Arrays.asList(code).indexOf(j.label)-1;
            } else if(i instanceof MethodInsnNode call) {
                int argc=Type.getArgumentTypes(call.desc).length;var args=new ArrayList<Object>();for(int a=0;a<argc;a++)args.addFirst(stack.removeLast());
                Object receiver=op!=Opcodes.INVOKESTATIC?stack.removeLast():null;
                switch(call.name) {
                    case "getItemInHand" -> stack.add(new ItemStack(handler.equals("lockBodyReplacement")?(sameBody?Items.TNT:Items.STONE):(flint?Items.FLINT_AND_STEEL:Items.FIRE_CHARGE)));
                    case "is" -> stack.add(((ItemStack)receiver).is((Item)args.getFirst()));
                    case "getItemBySlot" -> stack.add(new ItemStack(Items.TNT));
                    case "isEmpty" -> stack.add(false);
                    case "getItem" -> stack.add(Items.TNT);
                    case "isAbilityLocked" -> { var ability=(AbilityType)args.get(1);consulted.add(ability);boolean deny=locked.contains(ability);if(deny)notices++;stack.add(deny); }
                    case "isTargetInLockedLandmark" -> { if(landmark)notices++;stack.add(landmark); }
                    case "setReturnValue" -> callback.setReturnValue((InteractionResult)args.getFirst());
                    default -> fail("Unsupported call "+call.name);
                }
            } else if(op==Opcodes.RETURN)break;else fail("Unsupported opcode "+op);
        }
        return new Gate(callback.isCancelled(),notices,consulted);
    }
    static Stream<Integer> ignitionMasks() { return java.util.stream.IntStream.range(0,16).boxed(); }
    @ParameterizedTest @MethodSource("ignitionMasks") void independentFlintTripleAndChargeDoubleGate(int mask) throws Exception {
        boolean flint=(mask&8)!=0,flintLocked=(mask&1)!=0,tntLocked=(mask&2)!=0,landmark=(mask&4)!=0;
        Set<AbilityType> locked=EnumSet.noneOf(AbilityType.class);if(flintLocked)locked.add(AbilityType.USE_FLINT_AND_STEEL);if(tntLocked)locked.add(AbilityType.IGNITE_TNT);
        var gate=execute("lockManualIgnition",flint,false,locked,landmark);
        // Independent approved P7 truth table, not output copied from production.
        assertEquals((flint&&flintLocked)||tntLocked||landmark,gate.denied());assertEquals(gate.denied()?1:0,gate.notifications());
        if(!flint)assertFalse(gate.consulted().contains(AbilityType.USE_FLINT_AND_STEEL));
    }
    static Stream<Integer> pairMasks() { return Stream.of(0,1,2,3); }
    @ParameterizedTest @MethodSource("pairMasks") void shearAbilityAndLandmarkAndNativeSameBodyRejection(int mask) throws Exception {
        boolean ability=(mask&1)!=0,landmark=(mask&2)!=0;
        var shear=execute("lockManualShearing",false,false,ability?Set.of(AbilityType.USE_SHEARS):Set.of(),landmark);
        assertEquals(ability||landmark,shear.denied());assertEquals(shear.denied()?1:0,shear.notifications());
        var swallow=execute("lockBodyReplacement",false,ability,Set.of(),landmark);
        assertEquals(!ability&&landmark,swallow.denied());assertEquals(swallow.denied()?1:0,swallow.notifications());
        var feed=execute("lockBabyFeeding",false,false,Set.of(),landmark);assertEquals(landmark,feed.denied());assertTrue(feed.consulted().isEmpty());
    }
    @Test void registrationsAndForbiddenWaterRuleAndInheritedFallback() throws Exception {
        var json=JsonParser.parseString(Files.readString(Path.of("src/main/resources/achievetodo.mixins.json"))).getAsJsonObject();
        assertTrue(json.get("required").getAsBoolean());assertEquals("JAVA_21",json.get("compatibilityLevel").getAsString());assertEquals(1,json.getAsJsonObject("injectors").get("defaultRequire").getAsInt());
        var entries=new ArrayList<String>();json.getAsJsonArray("mixins").forEach(v -> entries.add(v.getAsString()));assertEquals(entries.size(),new HashSet<>(entries).size());
        for(String name:List.of("ShelfBlockMixin","SulfurCubeEntityMixin","EntityMixin","EquippableComponentMixin","PiercingWeaponMixin","HoneycombItemMixin","CopperGolemEntityMixin"))assertEquals(1,Collections.frequency(entries,"main."+name));
        assertEquals(47,JsonParser.parseString(Files.readString(Path.of("src/gametest/resources/fabric.mod.json"))).getAsJsonObject().getAsJsonObject("entrypoints").getAsJsonArray("fabric-gametest").size());
        for(var m:node(MIXINS+"SulfurCubeEntityMixin").methods) for(var i:m.instructions) if(i instanceof FieldInsnNode f)assertNotEquals("USE_WATER_BUCKET",f.name);
        assertTrue(names(method("net/minecraft/world/entity/player/Player","interactOn")).contains("interactLivingEntity"));
        assertTrue(names(method(MIXINS+"EntityMixin","lockManualEquipmentShears")).contains("isTargetInLockedLandmark"));
        var firstMatch=method("com/diskree/achievetodo/server/AchieveToDoServer","isTargetInLockedLandmark");assertNotNull(firstMatch);
    }
    record CallerCase(net.minecraft.world.level.block.Block block,InteractionResult result,boolean denied) {}
    static Stream<CallerCase> callerCases() {
        return Stream.of(new CallerCase(Blocks.OAK_SHELF,InteractionResult.FAIL,true),
            new CallerCase(Blocks.OAK_SHELF,InteractionResult.PASS,false),
            new CallerCase(Blocks.OAK_SHELF,InteractionResult.CONSUME,false),
            new CallerCase(Blocks.OAK_SHELF,InteractionResult.SUCCESS,false),
            new CallerCase(Blocks.STONE,InteractionResult.FAIL,false),
            new CallerCase(Blocks.CAULDRON,InteractionResult.FAIL,false));
    }
    @ParameterizedTest @MethodSource("callerCases") void realCompiledCallerOnlyPropagatesDeniedShelf(CallerCase c) throws Exception {
        var handlerClass=com.diskree.achievetodo.injection.mixin.main.ServerPlayerGameModeCauldronGateMixin.class;
        var handler=Arrays.stream(handlerClass.getDeclaredMethods()).filter(m -> m.getName().equals("stopDeniedShelfFallback")).findFirst().orElseThrow();handler.setAccessible(true);
        var instance=new com.diskree.achievetodo.injection.mixin.main.ServerPlayerGameModeCauldronGateMixin() {};
        var callback=new CallbackInfoReturnable<InteractionResult>("caller",true);
        handler.invoke(instance,null,null,null,null,null,callback,c.block().defaultBlockState(),c.result());
        assertEquals(c.denied(),callback.isCancelled());if(c.denied())assertSame(InteractionResult.FAIL,callback.getReturnValue());
    }
    @Test void callerBoundaryIsAfterNativeDispatchBeforeSuccessAndFallbackWithNoSecondPermissionLookup() throws Exception {
        var nativeMethod=method("net/minecraft/server/level/ServerPlayerGameMode","useItemOn");var c=calls(nativeMethod);
        int dispatch=java.util.stream.IntStream.range(0,c.size()).filter(i -> c.get(i).owner.endsWith("/BlockState")&&c.get(i).name.equals("useItemOn")).findFirst().orElseThrow();
        int consumes=java.util.stream.IntStream.range(0,c.size()).filter(i -> c.get(i).name.equals("consumesAction")).findFirst().orElseThrow();
        assertTrue(dispatch<consumes);assertTrue(consumes<names(nativeMethod).indexOf("trigger"));assertTrue(consumes<names(nativeMethod).indexOf("useOn"));
        assertFalse(Arrays.stream(method(SHELF,"useItemOn").instructions.toArray()).anyMatch(i -> i instanceof FieldInsnNode f&&f.name.equals("FAIL")));
        var guard=method(MIXINS+"ServerPlayerGameModeCauldronGateMixin","stopDeniedShelfFallback");assertEquals(List.of("getBlock","setReturnValue"),names(guard));
        assertTrue(node(MIXINS+"ServerPlayerGameModeCauldronGateMixin").fields.isEmpty(),"No invocation cache");
        var cauldron=names(method(MIXINS+"ServerPlayerGameModeCauldronGateMixin","lockCauldronInteraction"));
        assertTrue(cauldron.containsAll(List.of("isSecondaryUseActive","achievetodo$getInteractions","isTargetInLockedLandmark","isAbilityLocked","setReturnValue")));
    }
    /** Fresh real Fabric SERVER Knot, with production-generated normal preset. */
    public static void main(String[] args) throws Exception {
        System.setProperty("java.class.path",Files.readString(Path.of(System.getProperty("phaseD.probe.classpath"))));
        var knot=new net.fabricmc.loader.impl.launch.knot.Knot(net.fabricmc.api.EnvType.SERVER);ClassLoader loader=knot.init(new String[]{"--nogui","--universe",args[0]});
        Class.forName("net.minecraft.SharedConstants",true,loader).getMethod("tryDetectVersion").invoke(null);
        Class.forName("net.minecraft.server.Bootstrap",true,loader).getMethod("bootStrap").invoke(null);
        for(String owner:List.of(SHELF,CUBE,"net/minecraft/server/level/ServerPlayerGameMode")) { Class.forName(owner.replace('/','.'),true,loader).getDeclaredMethods();System.out.println("D_S06_TRANSFORMED_OWNER "+owner+" PASS"); }
        Class<?> origin=Class.forName("com.diskree.achievetodo.injection.extension.main.LevelInfoExtension$ConfigurationOrigin",true,loader);
        Object fresh=Arrays.stream(origin.getEnumConstants()).filter(v -> v.toString().equals("FRESH")).findFirst().orElseThrow();
        Class<?> io=Class.forName("com.diskree.achievetodo.injection.extension.main.LevelInfoExtension$ConfigurationIO",true,loader);
        Object generated=io.getMethod("load",Path.class,String.class,long.class,origin).invoke(null,Path.of(args[0]).resolve("config/achievetodo"),"normal",0L,fresh);
        System.out.println("D_S06_TRANSFORMED_SMOKE PASS abilities="+((Map<?,?>)generated).size()+" gameplay=NOT_RUN");
    }
}
