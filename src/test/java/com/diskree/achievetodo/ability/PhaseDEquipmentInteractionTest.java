package com.diskree.achievetodo.ability;

import com.google.gson.JsonParser;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

/** Real native component/bytecode contracts. Entity/world eligibility and packets are live GameTests, not synthetic JUnit gameplay. */
@Isolated("Restores actual native item component bindings")
public class PhaseDEquipmentInteractionTest {
    private static final Map<Holder.Reference<?>, DataComponentMap> COMPONENTS = new IdentityHashMap<>();
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion(); Bootstrap.bootStrap();
        try {
            BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(VanillaRegistries.createLookup()).stream()
                .filter(p -> p.key().equals(Registries.ITEM)).forEach(p -> p.forEach((holder, components) -> {
                    COMPONENTS.put(holder, holder.areComponentsBound() ? holder.components() : null);
                    holder.bindComponents(components);
                }));
        } catch (RuntimeException | Error error) { restore(); throw error; }
    }
    @AfterAll static void restore() { COMPONENTS.forEach(Holder.Reference::bindComponents); COMPONENTS.clear(); }
    static Stream<Item> animalEquipment() { return Stream.of(Items.SADDLE, Items.HARNESS.pick(net.minecraft.world.item.DyeColor.BLACK),
        Items.HARNESS.pick(net.minecraft.world.item.DyeColor.WHITE), Items.WOLF_ARMOR, Items.COPPER_HORSE_ARMOR, Items.IRON_HORSE_ARMOR,
        Items.LEATHER_HORSE_ARMOR, Items.DIAMOND_HORSE_ARMOR, Items.GOLDEN_HORSE_ARMOR,
        Items.COPPER_NAUTILUS_ARMOR, Items.IRON_NAUTILUS_ARMOR, Items.DIAMOND_NAUTILUS_ARMOR,
        Items.GOLDEN_NAUTILUS_ARMOR); }
    @ParameterizedTest @MethodSource("animalEquipment")
    void nativeAnimalEquipmentNeverAcquiresPlayerCopperPermission(Item item) {
        var c = new ItemStack(item).get(DataComponents.EQUIPPABLE); assertNotNull(c);
        assertTrue(c.slot() == EquipmentSlot.BODY || c.slot() == EquipmentSlot.SADDLE);
        assertFalse(c.canBeEquippedBy(EntityTypes.PLAYER.builtInRegistryHolder()));
        assertNull(AbilityType.findEquipmentEquipAbility(item));
    }
    @Test void harnessNativeRecipientAndShearabilityAreIndependentOfPlayerArmor() {
        var c=new ItemStack(Items.HARNESS.pick(net.minecraft.world.item.DyeColor.BLACK)).get(DataComponents.EQUIPPABLE);assertNotNull(c);
        // JUnit bootstrap does not reload entity tags. Inspect the real named native contract;
        // actual recipient/age eligibility is exercised after datapack reload by GameTests.
        assertEquals("minecraft:can_equip_harness",c.allowedEntities().orElseThrow().unwrapKey().orElseThrow().location().toString());
        assertEquals(EquipmentSlot.BODY,c.slot());assertTrue(c.canBeSheared());assertTrue(c.equipOnInteract());
    }
    @Test void copperAnimalBodyAndCopperPlayerChestHaveSeparateNativeRecipients() {
        var horse=new ItemStack(Items.COPPER_HORSE_ARMOR).get(DataComponents.EQUIPPABLE);
        var player=new ItemStack(Items.COPPER_CHESTPLATE).get(DataComponents.EQUIPPABLE);
        assertEquals("minecraft:can_wear_horse_armor",horse.allowedEntities().orElseThrow().unwrapKey().orElseThrow().location().toString());
        assertFalse(horse.equipOnInteract());assertEquals(EquipmentSlot.BODY,horse.slot());
        assertEquals(EquipmentSlot.CHEST,player.slot());
        assertEquals(AbilityType.EQUIP_COPPER_ARMOR,AbilityType.findEquipmentEquipAbility(Items.COPPER_CHESTPLATE));
    }
    @Test void saddleNativeAllowedTypesExcludeCowAndPlayer() {
        var saddle=new ItemStack(Items.SADDLE).get(DataComponents.EQUIPPABLE);
        assertEquals("minecraft:can_equip_saddle",saddle.allowedEntities().orElseThrow().unwrapKey().orElseThrow().location().toString());
        assertEquals(EquipmentSlot.SADDLE,saddle.slot());assertTrue(saddle.equipOnInteract());
    }
    private static ClassNode node(String owner) throws Exception {
        try(var input=PhaseDEquipmentInteractionTest.class.getResourceAsStream("/"+owner+".class")) {
            assertNotNull(input,owner);ClassNode n=new ClassNode();new ClassReader(input).accept(n,0);return n;
        }
    }
    private static MethodNode method(String owner,String name) throws Exception {
        return node(owner).methods.stream().filter(m -> m.name.equals(name)).findFirst().orElseThrow();
    }
    private static List<MethodInsnNode> calls(MethodNode method) {
        return Arrays.stream(method.instructions.toArray()).filter(i -> i instanceof MethodInsnNode)
            .map(i -> (MethodInsnNode)i).toList();
    }
    static Stream<String> mixins() { return Stream.of("EntityMixin","EquippableComponentMixin","CamelEntityMixin",
        "NautilusEntityMixin","HappyGhastEntityMixin","AnimalEquipmentMenuMixin","AnimalEquipmentTransferMixin"); }
    @ParameterizedTest @MethodSource("mixins")
    void everyCompiledInjectorBindsARealDeclaredNativeMethodAndInvocation(String name) throws Exception {
        ClassNode mixin=node("com/diskree/achievetodo/injection/mixin/main/"+name);
        AnnotationNode annotation=mixin.invisibleAnnotations.stream().filter(a -> a.desc.endsWith("/Mixin;")).findFirst().orElseThrow();
        @SuppressWarnings("unchecked") List<Type> targets=(List<Type>)annotation.values.get(annotation.values.indexOf("value")+1);
        ClassNode target=node(targets.getFirst().getInternalName());int checks=0;
        for(MethodNode m:mixin.methods) if(m.visibleAnnotations!=null) for(AnnotationNode a:m.visibleAnnotations) {
            if(a.values==null||!a.values.contains("method"))continue;
            @SuppressWarnings("unchecked") List<String> selectors=(List<String>)a.values.get(a.values.indexOf("method")+1);
            for(String selector:selectors) {
                MethodNode nativeMethod=target.methods.stream().filter(n -> selector.contains("(") ? (n.name+n.desc).equals(selector) : n.name.equals(selector)).findFirst().orElseThrow();
                assertTrue(nativeMethod.instructions.size()>0);checks++;
                if(!a.values.contains("at"))continue;
                Object value=a.values.get(a.values.indexOf("at")+1);
                List<?> ats=value instanceof List<?> l?l:List.of(value);
                for(Object at:ats) { AnnotationNode point=(AnnotationNode)at;
                    if(!point.values.contains("target"))continue;
                    String invocation=(String)point.values.get(point.values.indexOf("target")+1);
                    assertTrue(calls(nativeMethod).stream().anyMatch(c -> ("L"+c.owner+";"+c.name+c.desc).equals(invocation)),name+" "+invocation);
                }
            }
        }
        assertTrue(checks>0);
    }
    @Test void equippingNativeNoActionChecksPrecedeConsumption() throws Exception {
        var m=method("net/minecraft/world/item/equipment/Equippable","equipOnTarget");var calls=calls(m);
        assertTrue(calls.stream().filter(c -> c.name.equals("isEquippableInSlot")).findFirst().isPresent());
        List<String> names=calls.stream().map(c -> c.name).toList();
        assertTrue(names.indexOf("isEquippableInSlot")<names.indexOf("hasItemInSlot"));
        assertTrue(names.indexOf("isAlive")<names.indexOf("level"));
        assertTrue(names.indexOf("level")<names.indexOf("split"));
        assertTrue(names.indexOf("split")<names.indexOf("setItemSlot"));
    }
    @Test void nativeManualShearingStartsAtEntityBeforeDurabilityAndSubtypeDispatch() throws Exception {
        List<String> calls=calls(method("net/minecraft/world/entity/Entity","interact")).stream().map(c -> c.name).toList();
        assertTrue(calls.indexOf("shearOffAllLeashConnections")<calls.indexOf("hurtAndBreak"));
        assertTrue(calls.indexOf("canShearEquipment")<calls.indexOf("attemptToShearEquipment"));
        List<String> mutations=calls(method("net/minecraft/world/entity/Mob","shearItem")).stream().map(c -> c.name).toList();
        assertTrue(mutations.indexOf("hurtAndBreak")<mutations.indexOf("setItemSlot"));
    }
    @Test void mountFieldIsProtectedFinalLivingEntityAndClickOwnerIsBaseContainer() throws Exception {
        var field=node("net/minecraft/world/inventory/AbstractMountInventoryMenu").fields.stream().filter(f -> f.name.equals("mount")).findFirst().orElseThrow();
        assertEquals("Lnet/minecraft/world/entity/LivingEntity;",field.desc);
        assertEquals(Opcodes.ACC_PROTECTED|Opcodes.ACC_FINAL,field.access);
        assertEquals("(IILnet/minecraft/world/inventory/ContainerInput;Lnet/minecraft/world/entity/player/Player;)V",method("net/minecraft/world/inventory/AbstractContainerMenu","clicked").desc);
        assertEquals("(Lnet/minecraft/world/entity/player/Player;I)Lnet/minecraft/world/item/ItemStack;",method("net/minecraft/world/inventory/AbstractMountInventoryMenu","quickMoveStack").desc);
    }
    @Test void nativeCamelOpeningIsIndependentOfHorseMobInteract() throws Exception {
        var calls=calls(method("net/minecraft/world/entity/animal/camel/Camel","mobInteract"));
        var open=calls.stream().filter(c -> c.name.equals("openCustomInventoryScreen")).findFirst().orElseThrow();
        assertEquals("net/minecraft/world/entity/animal/camel/Camel",open.owner);
        assertEquals("(Lnet/minecraft/world/entity/player/Player;)V",open.desc);
        assertTrue(calls.indexOf(open)<calls.indexOf(calls.stream().filter(c -> c.name.equals("interactLivingEntity")).findFirst().orElseThrow()));
        assertTrue(calls(method("net/minecraft/world/entity/animal/camel/Camel","openCustomInventoryScreen")).stream().anyMatch(c -> c.name.equals("openHorseInventory")));
    }
    @Test void nativeSubtypeEffectsAndAutonomousLeashStillExist() throws Exception {
        var n=calls(method("net/minecraft/world/entity/animal/nautilus/AbstractNautilus","mobInteract")).stream().map(c -> c.name).toList();
        assertTrue(n.containsAll(List.of("openCustomInventoryScreen","usePlayerItem","tryToTame","feed","doPlayerRide")));
        var g=calls(method("net/minecraft/world/entity/animal/happyghast/HappyGhast","mobInteract")).stream().map(c -> c.name).toList();
        assertTrue(g.indexOf("interactLivingEntity")<g.indexOf("doPlayerRide"));assertFalse(g.contains("openCustomInventoryScreen"));
        assertEquals("(Lnet/minecraft/world/entity/player/Player;)Z",method("net/minecraft/world/entity/Entity","shearOffAllLeashConnections").desc);
    }
    @Test void exactRegistrationIdentitiesAndSafetyRequirementsRemain() throws Exception {
        var m=JsonParser.parseString(Files.readString(Path.of("src/main/resources/achievetodo.mixins.json"))).getAsJsonObject();
        assertTrue(m.get("required").getAsBoolean());assertEquals(1,m.getAsJsonObject("injectors").get("defaultRequire").getAsInt());
        List<String> entries=new ArrayList<>();m.getAsJsonArray("mixins").forEach(v -> entries.add(v.getAsString()));
        assertEquals(entries.size(),new HashSet<>(entries).size());
        for(String n:List.of("CamelEntityMixin","NautilusEntityMixin","HappyGhastEntityMixin","AnimalEquipmentMenuMixin","AnimalEquipmentTransferMixin","PiercingWeaponMixin","KineticWeaponMixin","ItemKineticWeaponMixin"))assertEquals(1,Collections.frequency(entries,"main."+n));
        assertEquals(47,JsonParser.parseString(Files.readString(Path.of("src/gametest/resources/fabric.mod.json"))).getAsJsonObject().getAsJsonObject("entrypoints").getAsJsonArray("fabric-gametest").size());
    }
    /** Real Fabric SERVER transformation in an isolated JVM; not live gameplay. */
    public static void main(String[] args) throws Exception {
        System.setProperty("java.class.path",Files.readString(Path.of(System.getProperty("phaseD.probe.classpath"))));
        var knot=new net.fabricmc.loader.impl.launch.knot.Knot(net.fabricmc.api.EnvType.SERVER);
        ClassLoader loader=knot.init(new String[]{"--nogui","--universe",args[0]});
        Class.forName("net.minecraft.SharedConstants",true,loader).getMethod("tryDetectVersion").invoke(null);
        Class.forName("net.minecraft.server.Bootstrap",true,loader).getMethod("bootStrap").invoke(null);
        for(String owner:List.of("net.minecraft.world.entity.Entity","net.minecraft.world.item.equipment.Equippable",
            "net.minecraft.world.entity.animal.camel.Camel","net.minecraft.world.entity.animal.nautilus.AbstractNautilus",
            "net.minecraft.world.entity.animal.happyghast.HappyGhast","net.minecraft.world.inventory.AbstractContainerMenu",
            "net.minecraft.world.inventory.AbstractMountInventoryMenu")) {
            Class.forName(owner,true,loader).getDeclaredMethods();System.out.println("D_S04_TRANSFORMED_OWNER "+owner+" PASS");
        }
        Class<?> origin=Class.forName("com.diskree.achievetodo.injection.extension.main.LevelInfoExtension$ConfigurationOrigin",true,loader);
        Object fresh=Arrays.stream(origin.getEnumConstants()).filter(v -> v.toString().equals("FRESH")).findFirst().orElseThrow();
        Class<?> io=Class.forName("com.diskree.achievetodo.injection.extension.main.LevelInfoExtension$ConfigurationIO",true,loader);
        Object generated=io.getMethod("load",Path.class,String.class,long.class,origin).invoke(null,Path.of(args[0]).resolve("config/achievetodo"),"normal",0L,fresh);
        System.out.println("D_S04_TRANSFORMED_SMOKE PASS abilities="+((Map<?,?>)generated).size()+" gameplay=NOT_RUN");
    }
}
