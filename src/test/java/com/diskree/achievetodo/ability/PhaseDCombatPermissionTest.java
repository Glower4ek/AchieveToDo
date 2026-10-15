package com.diskree.achievetodo.ability;

import com.diskree.achievetodo.server.AchieveToDoServer;
import com.diskree.achievetodo.server.AdvancementsMode;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** Isolated ATD logic/compiled/native-descriptor tests; these are not gameplay eligibility fixtures. */
@Isolated("Restores native item component bindings and synthetic invocation state")
public class PhaseDCombatPermissionTest {
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
    private static Object field(Object target, Class<?> owner, String name) throws Exception {
        Field f = owner.getDeclaredField(name); f.setAccessible(true); return f.get(target);
    }
    private static void set(Object target, Class<?> owner, String name, Object value) throws Exception {
        Field f = owner.getDeclaredField(name); f.setAccessible(true); f.set(target, value);
    }
    /** Allocation supplies identity for permission/context-only tests; no native action is executed on this object. */
    private static ServerPlayer identity(GameType mode) throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field singleton = unsafeClass.getDeclaredField("theUnsafe"); singleton.setAccessible(true);
        ServerPlayer player = (ServerPlayer) unsafeClass.getMethod("allocateInstance", Class.class)
            .invoke(singleton.get(null), ServerPlayer.class);
        player.setUUID(UUID.randomUUID());
        ServerPlayerGameMode gameMode = new ServerPlayerGameMode(player);
        set(gameMode, ServerPlayerGameMode.class, "gameModeForPlayer", mode);
        set(player, ServerPlayer.class, "gameMode", gameMode);
        return player;
    }
    static Stream<Item> spears() { return Stream.of(Items.WOODEN_SPEAR, Items.STONE_SPEAR, Items.COPPER_SPEAR,
        Items.IRON_SPEAR, Items.GOLDEN_SPEAR, Items.DIAMOND_SPEAR, Items.NETHERITE_SPEAR); }
    private static AchieveToDoServer ready(ServerPlayer player, AbilityType ability, int threshold, int count) throws Exception {
        AchieveToDoServer server = new AchieveToDoServer();
        set(server, AchieveToDoServer.class, "abilitiesConfiguration", Map.of(ability, threshold));
        server.currentAdvancementsMode = AdvancementsMode.DEFAULT;
        server.currentScoreboardDisplaySlot = DisplaySlot.SIDEBAR;
        server.currentScoreboardObjective = new Scoreboard().addObjective("selected", ObjectiveCriteria.DUMMY,
            Component.literal("selected"), ObjectiveCriteria.RenderType.INTEGER, false, null);
        @SuppressWarnings("unchecked") Map<UUID,Integer> counts = (Map<UUID,Integer>)field(server, AchieveToDoServer.class,
            "obtainedAdvancementsCountByPlayers");
        counts.put(player.getUUID(), count);
        return server;
    }
    @ParameterizedTest @MethodSource("spears")
    void sevenMaterialsUseActualSilentThresholdsWithoutStoredAllowance(Item spear) throws Exception {
        assertTrue(AchieveToDoServer.isRegisteredSpear(spear));
        AbilityType ability = AbilityType.findToolMaterialUsageAbility(spear); assertNotNull(ability);
        ServerPlayer player = identity(GameType.SURVIVAL);
        assertTrue(ready(player, ability, 10, 9).isAbilityLocked(player, ability, true));
        AchieveToDoServer server = ready(player, ability, 10, 10);
        assertFalse(server.isAbilityLocked(player, ability, true));
        @SuppressWarnings("unchecked") Map<UUID,Integer> counts = (Map<UUID,Integer>)field(server, AchieveToDoServer.class,
            "obtainedAdvancementsCountByPlayers");
        counts.put(player.getUUID(), 9); assertTrue(server.isAbilityLocked(player, ability, true));
        counts.put(player.getUUID(), 11); assertFalse(server.isAbilityLocked(player, ability, true));
    }
    @Test void readinessExemptionsAndSentinelsKeepHistoricalMeaning() throws Exception {
        ServerPlayer ordinary = identity(GameType.SURVIVAL);
        assertTrue(new AchieveToDoServer().isAbilityLocked(ordinary, AbilityType.USE_COPPER_TOOLS, true));
        for (GameType mode : List.of(GameType.CREATIVE, GameType.SPECTATOR))
            assertFalse(new AchieveToDoServer().isAbilityLocked(identity(mode), AbilityType.USE_COPPER_TOOLS, true));
        assertFalse(ready(ordinary, AbilityType.USE_COPPER_TOOLS, 0, Integer.MIN_VALUE)
            .isAbilityLocked(ordinary, AbilityType.USE_COPPER_TOOLS, true));
        assertTrue(ready(ordinary, AbilityType.USE_COPPER_TOOLS, -1, Integer.MAX_VALUE)
            .isAbilityLocked(ordinary, AbilityType.USE_COPPER_TOOLS, true));
    }
    @Test void registeredScopeDoesNotExpandToOtherMaterialItems() {
        assertFalse(AchieveToDoServer.isRegisteredSpear(Items.COPPER_SWORD));
        assertFalse(AchieveToDoServer.isRegisteredSpear(Items.SHIELD));
        assertFalse(AchieveToDoServer.isRegisteredSpear(null));
    }
    private static final class LandmarkFixture extends AchieveToDoServer {
        final Map<AbilityType,Boolean> locks = new EnumMap<>(AbilityType.class);
        final List<AbilityType> probes = new ArrayList<>();
        int notifications; AABB delivered;
        @Override public boolean isAbilityLocked(ServerPlayer actor, AbilityType ability, boolean checkOnly) {
            assertTrue(checkOnly); probes.add(ability); return locks.get(ability);
        }
        @Override protected void notifyLockedLandmark(ServerPlayer actor, DimensionType dimension, AABB box) {
            notifications++; delivered = box;
        }
    }
    private static LandmarkFixture landmarks(ServerPlayer actor, boolean first, boolean second) throws Exception {
        LandmarkFixture f = new LandmarkFixture();
        LinkedHashMap<LandmarkType,Set<UUID>> membership = new LinkedHashMap<>();
        membership.put(LandmarkType.DESERT_PYRAMID, Set.of(actor.getUUID()));
        membership.put(LandmarkType.IGLOO, Set.of(actor.getUUID()));
        Map<LandmarkType,Set<DimensionalBlockBox>> boxes = new LinkedHashMap<>();
        for (LandmarkType type : membership.keySet()) boxes.put(type,
            Set.of(new DimensionalBlockBox(DimensionType.OVERWORLD, new BoundingBox(0,0,0,3,3,3))));
        set(f, AchieveToDoServer.class, "playersByLockedLandmarkTypes", membership);
        set(f, AchieveToDoServer.class, "landmarksByChunks", Map.of(new ChunkPos(0,0), boxes));
        f.locks.put(AbilityType.findByLandmarkType(LandmarkType.DESERT_PYRAMID), first);
        f.locks.put(AbilityType.findByLandmarkType(LandmarkType.IGLOO), second);
        return f;
    }
    @Test void firstAllowedMatchReturnsFalseWithoutProbingLaterDeniedMatch() throws Exception {
        ServerPlayer actor = identity(GameType.SURVIVAL);
        LandmarkFixture f = landmarks(actor, false, true);
        AABB box = new AABB(.2,.2,.2,1.9,1.9,1.9);
        assertFalse(f.isTargetInLockedLandmark(actor, DimensionType.OVERWORLD, box, true));
        assertEquals(List.of(AbilityType.findByLandmarkType(LandmarkType.DESERT_PYRAMID)), f.probes);
        f.probes.clear(); assertFalse(f.isTargetInLockedLandmark(actor, DimensionType.OVERWORLD, box));
        assertEquals(1, f.probes.size()); assertEquals(0, f.notifications);
    }
    @Test void firstDeniedMatchSilentAndNotifyingResultsEqualAndUseOriginalBox() throws Exception {
        ServerPlayer actor = identity(GameType.SURVIVAL); LandmarkFixture f = landmarks(actor, true, false);
        AABB box = new AABB(.2,.2,.2,1.9,1.9,1.9);
        assertTrue(f.isTargetInLockedLandmark(actor, DimensionType.OVERWORLD, box, true));
        assertEquals(0, f.notifications); assertEquals(1, f.probes.size());
        f.probes.clear(); assertTrue(f.isTargetInLockedLandmark(actor, DimensionType.OVERWORLD, box));
        assertEquals(1, f.notifications); assertSame(box, f.delivered); assertEquals(1, f.probes.size());
    }
    @Test void noQualifyingMatchDimensionGeometryAndMembershipRemainFalse() throws Exception {
        ServerPlayer actor = identity(GameType.SURVIVAL); LandmarkFixture f = landmarks(actor, true, true);
        assertFalse(f.isTargetInLockedLandmark(actor, DimensionType.NETHER, new AABB(0,0,0,1,1,1), true));
        assertFalse(f.isTargetInLockedLandmark(actor, DimensionType.OVERWORLD, new AABB(4,0,0,5,1,1), true));
        assertFalse(f.isTargetInLockedLandmark(identity(GameType.SURVIVAL), DimensionType.OVERWORLD,
            new AABB(0,0,0,1,1,1), true));
        assertTrue(f.probes.isEmpty()); assertEquals(0, f.notifications);
    }
    private static AchieveToDoServer.CombatInvocation syntheticScope(ServerPlayer actor, boolean report) throws Exception {
        var constructor = AchieveToDoServer.CombatInvocation.class.getDeclaredConstructor(
            ServerPlayer.class, EquipmentSlot.class, ItemStack.class, boolean.class);
        constructor.setAccessible(true);
        return constructor.newInstance(actor, EquipmentSlot.OFFHAND, new ItemStack(Items.COPPER_SPEAR), report);
    }
    private static Object current() throws Exception {
        return ((ThreadLocal<?>) field(null, AchieveToDoServer.class, "COMBAT_INVOCATION")).get();
    }
    private static void denial(ServerPlayer actor, Runnable report) throws Exception {
        Method m = AchieveToDoServer.class.getDeclaredMethod("recordCombatDenial", ServerPlayer.class, Runnable.class, boolean.class);
        m.setAccessible(true); m.invoke(null, actor, report, true);
    }
    @Test void nestedSameActorReportsOnceAndRestoresOuterFrame() throws Exception {
        ServerPlayer actor = identity(GameType.SURVIVAL); AtomicInteger reports = new AtomicInteger();
        assertNull(current());
        try (var outer = syntheticScope(actor, true)) {
            denial(actor, reports::incrementAndGet);
            try (var inner = syntheticScope(actor, true)) {
                denial(actor, reports::incrementAndGet); inner.reportDenial(); assertEquals(0, reports.get());
            }
            assertSame(outer, current()); outer.reportDenial(); outer.reportDenial(); assertEquals(1, reports.get());
        }
        assertNull(current());
    }
    @Test void continuationAndNestedProbesStaySilent() throws Exception {
        ServerPlayer actor = identity(GameType.SURVIVAL); AtomicInteger reports = new AtomicInteger();
        try (var silent = syntheticScope(actor, false)) {
            denial(actor, reports::incrementAndGet);
            try (var nested = syntheticScope(actor, true)) { denial(actor, reports::incrementAndGet); nested.reportDenial(); }
            silent.reportDenial(); assertEquals(0, reports.get());
        }
        assertNull(current());
    }
    @Test void unrelatedActorsOwnTheirReportersAndExceptionRestoresOuter() throws Exception {
        ServerPlayer first = identity(GameType.SURVIVAL), second = identity(GameType.SURVIVAL);
        AtomicInteger a = new AtomicInteger(), b = new AtomicInteger();
        try (var outer = syntheticScope(first, true)) {
            denial(first, a::incrementAndGet);
            assertThrows(IllegalArgumentException.class, () -> {
                try (var inner = syntheticScope(second, true)) { denial(second, b::incrementAndGet); throw new IllegalArgumentException(); }
            });
            assertSame(outer, current()); outer.reportDenial(); assertEquals(1,a.get()); assertEquals(0,b.get());
        }
        assertNull(current());
    }
    @Test void reporterFailureStillClosesAndCannotLeaveStaleContext() throws Exception {
        ServerPlayer actor = identity(GameType.SURVIVAL);
        assertThrows(IllegalStateException.class, () -> {
            try (var scope = syntheticScope(actor, true)) {
                denial(actor, () -> { throw new IllegalStateException("reporter failure"); }); scope.reportDenial();
            }
        });
        assertNull(current());
    }
    @Test void wrongThreadAndOutOfOrderCloseFailWithoutLosingOwnerFrame() throws Exception {
        ServerPlayer actor = identity(GameType.SURVIVAL);
        try (var outer = syntheticScope(actor, true)) {
            AtomicReference<Throwable> error = new AtomicReference<>();
            Thread thread = new Thread(() -> { try { outer.close(); } catch (Throwable t) { error.set(t); } });
            thread.start(); thread.join(); assertInstanceOf(IllegalStateException.class,error.get());
            try (var inner = syntheticScope(actor, true)) {
                assertThrows(IllegalStateException.class, outer::close); assertSame(inner,current());
            }
            assertSame(outer,current());
        }
        assertNull(current());
    }
    private static ClassNode node(String resource) throws Exception {
        try (var input = PhaseDCombatPermissionTest.class.getResourceAsStream("/"+resource+".class")) {
            assertNotNull(input); ClassNode n = new ClassNode(); new ClassReader(input).accept(n,0); return n;
        }
    }
    @Test void compiledInjectionDescriptorsExistInPinnedNativeOwners() throws Exception {
        Map<String,String> owners = Map.of("PlayerEntityMixin","net/minecraft/world/entity/player/Player",
            "LivingEntityMixin","net/minecraft/world/entity/LivingEntity", "PiercingWeaponMixin","net/minecraft/world/item/component/PiercingWeapon",
            "ItemKineticWeaponMixin","net/minecraft/world/item/Item", "KineticWeaponMixin","net/minecraft/world/item/component/KineticWeapon");
        int explicit = 0;
        for (var entry : owners.entrySet()) {
            ClassNode mixin = node("com/diskree/achievetodo/injection/mixin/main/"+entry.getKey());
            ClassNode nativeOwner = node(entry.getValue());
            for (MethodNode m : mixin.methods) if (m.visibleAnnotations != null) for (AnnotationNode a : m.visibleAnnotations) {
                if (a.values == null || !a.values.contains("method")) continue;
                @SuppressWarnings("unchecked") List<String> selectors = (List<String>) a.values.get(a.values.indexOf("method")+1);
                for (String selector : selectors) if (selector.contains("(")) {
                    assertTrue(nativeOwner.methods.stream().anyMatch(n -> (n.name+n.desc).equals(selector)),selector); explicit++;
                }
            }
        }
        assertEquals(9,explicit); // attack, stab, break, post, piercing wrapper/predicate, item, kinetic wrapper/inject
    }
    @Test void nativeKineticTimingAndItemBranchOrdinalsMatchActualBytecode() throws Exception {
        MethodNode use = node("net/minecraft/world/item/Item").methods.stream().filter(m -> m.name.equals("use")).findFirst().orElseThrow();
        List<String> order = new ArrayList<>();
        for (AbstractInsnNode i : use.instructions) {
            if (i instanceof FieldInsnNode f && f.owner.endsWith("DataComponents")) order.add(f.name);
            if (i instanceof MethodInsnNode m && m.name.equals("startUsingItem")) order.add("START");
        }
        assertEquals(List.of("CONSUMABLE","EQUIPPABLE","BLOCKS_ATTACKS","START","KINETIC_WEAPON","START"),order);
        MethodNode kinetic = node("net/minecraft/world/item/component/KineticWeapon").methods.stream()
            .filter(m -> m.name.equals("damageEntities")).findFirst().orElseThrow();
        int delayReturn=-1, look=-1, remember=-1, index=0;
        for (AbstractInsnNode i : kinetic.instructions) {
            if (i.getOpcode()==Opcodes.RETURN && delayReturn<0) delayReturn=index;
            if (i instanceof MethodInsnNode m && m.name.equals("getLookAngle")) look=index;
            if (i instanceof MethodInsnNode m && m.name.equals("rememberStabbedEntity")) remember=index;
            index++;
        }
        assertTrue(delayReturn>=0 && look>delayReturn && remember>look);
    }
    @Test void compiledWrappersHaveExceptionHandlersClosingEveryScope() throws Exception {
        for (String type : List.of("PiercingWeaponMixin","KineticWeaponMixin")) {
            ClassNode n = node("com/diskree/achievetodo/injection/mixin/main/"+type);
            MethodNode wrapper = n.methods.stream().filter(m -> m.visibleAnnotations!=null && m.visibleAnnotations.stream()
                .anyMatch(a -> a.desc.endsWith("/WrapMethod;"))).findFirst().orElseThrow();
            assertTrue(wrapper.tryCatchBlocks.stream().anyMatch(t -> "java/lang/Throwable".equals(t.type)));
            assertTrue(Arrays.stream(wrapper.instructions.toArray()).filter(i -> i instanceof MethodInsnNode m && m.name.equals("close")).count()>=2);
        }
    }

    @Test void serverOutsideCpS13RegionKeepsHistoricalNormalizedBytes() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/diskree/achievetodo/server/AchieveToDoServer.java")).replace("\r\n","\n");
        for (String type : List.of("world.entity.Entity","world.entity.EquipmentSlot","world.entity.LivingEntity",
            "world.entity.player.Player","world.item.Item","world.item.ItemStack","world.item.Items"))
            source = source.replace("import net.minecraft."+type+";\n", "");
        String prefix = source.substring(0,source.indexOf("    public boolean isTargetInLockedLandmark("));
        String suffix = source.substring(source.indexOf("    public void onLandmarksLoadedStatusChanged("));
        var digest = java.security.MessageDigest.getInstance("SHA-256");
        assertEquals("00d97d967ec43c0432e7c2c058607b0aeaaf038f155d7c0a3469cbe04b0858ea",HexFormat.of().formatHex(digest.digest(prefix.getBytes(java.nio.charset.StandardCharsets.UTF_8))));
        assertEquals("2d94473916fa280865738db727408dfc50f36a69a540e7e7b99639c42b0371f6",HexFormat.of().formatHex(digest.digest(suffix.getBytes(java.nio.charset.StandardCharsets.UTF_8))));
    }

    /** Isolated Fabric JVM: load every changed native owner through actual Mixin transformation. */
    public static void main(String[] args) throws Exception {
        System.setProperty("java.class.path", Files.readString(Path.of(System.getProperty("phaseD.probe.classpath"))));
        Files.createDirectories(Path.of(args[0]));
        var knot = new net.fabricmc.loader.impl.launch.knot.Knot(net.fabricmc.api.EnvType.SERVER);
        ClassLoader loader = knot.init(new String[]{"--nogui","--universe",args[0]});
        Class.forName("net.minecraft.SharedConstants",true,loader).getMethod("tryDetectVersion").invoke(null);
        Class.forName("net.minecraft.server.Bootstrap",true,loader).getMethod("bootStrap").invoke(null);
        for (String owner : List.of("net.minecraft.world.entity.player.Player","net.minecraft.world.entity.LivingEntity",
            "net.minecraft.world.item.Item","net.minecraft.world.item.component.PiercingWeapon","net.minecraft.world.item.component.KineticWeapon")) {
            Class<?> type = Class.forName(owner,true,loader); type.getDeclaredMethods();
            System.out.println("D_S03_TRANSFORMED_OWNER "+owner+" PASS");
        }
        // GameTest LevelSettings do not pass Main's FRESH creation hook. Seed only the disposable
        // fixture's normal preset through the real production writer before starting that server.
        Class<?> originType=Class.forName("com.diskree.achievetodo.injection.extension.main.LevelInfoExtension$ConfigurationOrigin",true,loader);
        Object fresh=Arrays.stream(originType.getEnumConstants()).filter(v -> v.toString().equals("FRESH")).findFirst().orElseThrow();
        Class<?> io=Class.forName("com.diskree.achievetodo.injection.extension.main.LevelInfoExtension$ConfigurationIO",true,loader);
        Path configuration=Path.of(args[0]).resolve("config/achievetodo");
        Object generated=io.getMethod("load",Path.class,String.class,long.class,originType).invoke(null,configuration,"normal",0L,fresh);
        System.out.println("D_S03_DISPOSABLE_NORMAL_CONFIG "+configuration+" entries="+((Map<?,?>)generated).size());
        System.out.println("D_S03_TRANSFORMED_SMOKE PASS gameplay=NOT_RUN");
    }
}
