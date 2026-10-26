package com.diskree.achievetodo.gameplay.nativeguard;

import com.diskree.achievetodo.ability.*;
import com.diskree.achievetodo.landmark.*;
import com.diskree.achievetodo.progression.*;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassHierarchyResolver;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarFile;

/** Synthetic contracts are deliberately separate from actual worldgen/client-action receipts. */
public final class NativeLandmarkContractTest {
    private static int checks;
    private static void check(boolean ok) { if (!ok) throw new AssertionError("S4G check " + checks); checks++; }
    public static void main(String[] args) throws Exception {
        CanonicalMembershipParityTest.run();
        UUID actor = UUID.randomUUID();
        var world = new LandmarkWorld("world-a", DimensionType.OVERWORLD);
        var index = new LandmarkIndex();
        var files = new HashMap<String, String>();
        var custom = new StringBuilder("version = 1\n[abilities]\n"); int signedIndex = 0;
        for (var type : LandmarkType.values()) custom.append(AbilityResolvers.findLandmarkAbility(type).orElseThrow().getName())
            .append(" = ").append(new int[]{-2, -1, 0, 7}[signedIndex++ % 4]).append('\n');
        files.put("custom.toml", custom.toString());
        var repository = new ProgressionConfigRepository() {
            public Optional<String> read(String name) { return Optional.ofNullable(files.get(name)); }
            public void write(String name, String value) { files.put(name, value); }
        };
        for (LandmarkType type : LandmarkType.values()) {
            var ability = AbilityResolvers.findLandmarkAbility(type).orElseThrow();
            var record = new LandmarkRecord(type, world, new BlockBox(-17, -10, -1, 16, 10, 3),
                new LandmarkSource(type.isFeature() ? LandmarkSourceKind.FEATURE : LandmarkSourceKind.STRUCTURE,
                    type.isFeature() ? type.getFeatureKey() : type.getStructureKeys().iterator().next(), new ChunkCoordinate(-1, 0)));
            for (int threshold : new int[]{0, 1, 7, 250, 1000}) {
                var requirements = AbilityRequirements.normalize(Map.of(ability, threshold));
                for (int count : new int[]{threshold - 1, threshold, threshold + 1}) {
                    var state = PlayerProgressionState.create(actor, count, requirements);
                    boolean unlocked = threshold == 0 || count >= threshold;
                    check(ExactLandmarkGate.denied(ExactLandmarkGate.evaluate(state, actor, false, Set.of(record))) != unlocked);
                    check(!ExactLandmarkGate.denied(ExactLandmarkGate.evaluate(state, actor, true, Set.of(record))));
                    check(ExactLandmarkGate.evaluate(state, UUID.randomUUID(), false, Set.of(record)) == ExactLandmarkGate.Result.UNAVAILABLE);
                    check(ExactLandmarkGate.evaluate(state.withReady(false), actor, false, Set.of(record)) == ExactLandmarkGate.Result.UNAVAILABLE);
                    check(ExactLandmarkGate.evaluate(state, actor, false, Set.of()) == ExactLandmarkGate.Result.OUTSIDE);
                }
                var state = PlayerProgressionState.create(actor, threshold, requirements);
                check(ExactLandmarkGate.evaluate(state.withObtainedAdvancementCount(threshold - 1), actor, false, Set.of(record))
                    == (threshold == 0 ? ExactLandmarkGate.Result.UNLOCKED : ExactLandmarkGate.Result.LOCKED));
            }
            for (int special : new int[]{-2, -1}) {
                var state = PlayerProgressionState.create(actor, 1000, AbilityRequirements.normalize(Map.of(ability, special)));
                check(ExactLandmarkGate.evaluate(state, actor, false, Set.of(record)) == ExactLandmarkGate.Result.UNSUPPORTED_MEMBERSHIP);
            }
            for (String config : List.of("easy", "medium", "hard", "custom", "chaos")) {
                var requirements = new ProgressionConfigService().resolve(new WorldProgressionConfig(config), 777, repository, true)
                    .definition().requirements();
                int threshold = requirements.getRequiredCount(ability);
                for (int count : new int[]{-3, -2, -1, 0, threshold - 1, threshold, threshold + 1, threshold - 1}) {
                    var state = PlayerProgressionState.create(actor, count, requirements);
                    var result = ExactLandmarkGate.evaluate(state, actor, false, Set.of(record));
                    check(result == (threshold < 0 ? ExactLandmarkGate.Result.UNSUPPORTED_MEMBERSHIP
                        : threshold == 0 || count >= threshold ? ExactLandmarkGate.Result.UNLOCKED : ExactLandmarkGate.Result.LOCKED));
                }
            }
            check(ExactLandmarkGate.evaluate(null, actor, false, Set.of(record)) == ExactLandmarkGate.Result.UNAVAILABLE);
            index.add(record);
            for (int x : new int[]{-18, -17, -16, -1, 0, 15, 16, 17}) for (int y : new int[]{-11, -10, 10, 11}) {
                check(index.findContaining(world, x, y, 0).contains(record) == (x >= -17 && x <= 16 && y >= -10 && y <= 10));
            }
            check(index.findContaining(new LandmarkWorld("world-b", DimensionType.OVERWORLD), 0, 0, 0).isEmpty());
            check(index.findContaining(new LandmarkWorld("world-a", DimensionType.NETHER), 0, 0, 0).isEmpty());
            index.remove(record); check(index.all().isEmpty());
        }
        check(NativeLandmarkMappings.STRUCTURES.size() == 33);
        var a = new LandmarkRecord(LandmarkType.VILLAGE, world, new BlockBox(0, 0, 0, 1, 1, 1), new LandmarkSource(LandmarkSourceKind.STRUCTURE, "village_plains", new ChunkCoordinate(0, 0)));
        var b = new LandmarkRecord(LandmarkType.MINESHAFT, world, a.blockBox(), new LandmarkSource(LandmarkSourceKind.STRUCTURE, "mineshaft", new ChunkCoordinate(0, 0)));
        check(ExactLandmarkGate.evaluate(PlayerProgressionState.create(actor, 1000, AbilityRequirements.normalize(Map.of())), actor, false, Set.of(a, b))
            == ExactLandmarkGate.Result.UNSUPPORTED_MEMBERSHIP);
        check(NativeLandmarkMappings.STRUCTURES.values().stream().distinct().count() == 20);
        for (var entry : NativeLandmarkMappings.STRUCTURES.entrySet())
            check(LandmarkType.findByStructureKey(entry.getKey()).orElseThrow().getName().equals(entry.getValue()));
        check(!NativeLandmarkCapture.valid(new int[]{1, 0, 0, 0, 0, 0}));
        check(!NativeLandmarkCapture.valid(new int[]{0, 0, 0}));
        check(Arrays.equals(NativeLandmarkCapture.union(new int[]{-3, -4, -5, 0, 1, 2}, new int[]{-2, -8, -1, 3, 4, 5}), new int[]{-3, -8, -5, 3, 4, 5}));
        check(NativeLandmarkAgent.blockRestricted(true, null, null, null)); // Never undo external/native restriction.
        check(NativeLandmarkAgent.blockRestricted(false, new Object(), null, null)); // Missing callback is closed.
        check(!NativeLandmarkAgent.targetDenied(null, null, null)); // Canonical null-actor bypass.
        String nativeJar = System.getenv("ACHIEVETODO_TEST_PURPUR_NATIVE");
        if (nativeJar != null && !nativeJar.isBlank()) try (var jar = new JarFile(nativeJar)) {
            var urls = new ArrayList<java.net.URL>(); urls.add(Path.of(nativeJar).toUri().toURL());
            Path libraries = Path.of(nativeJar).getParent().getParent().getParent().resolve("libraries");
            try (var libraryFiles = Files.walk(libraries)) { for (Path file : libraryFiles.filter(p -> p.toString().endsWith(".jar")).toList()) urls.add(file.toUri().toURL()); }
            try (var loader = new java.net.URLClassLoader(urls.toArray(java.net.URL[]::new), NativeLandmarkContractTest.class.getClassLoader())) {
            var verifier = ClassFile.of(ClassFile.ClassHierarchyResolverOption.of(ClassHierarchyResolver.ofResourceParsing(loader)));
            var names = new TreeSet<>(NativeLandmarkActionSpecs.HASHES.keySet());
            names.addAll(List.of("net/minecraft/world/level/levelgen/structure/Structure", "net/minecraft/world/level/levelgen/structure/StructureStart",
                    "net/minecraft/server/level/WorldGenRegion", "net/minecraft/world/level/Level", "net/minecraft/world/level/levelgen/feature/Feature", "net/minecraft/world/entity/player/Player"));
            for (String name : names) {
                byte[] bytes; try (var in = jar.getInputStream(jar.getJarEntry(name + ".class"))) { bytes = in.readAllBytes(); }
                var output = NativeLandmarkAgent.transformNative(name, bytes, loader);
                var errors = verifier.verify(output);
                System.out.println("S4G_TRANSFORM_CONTRACT " + name + " verify=" + errors);
                check(errors.isEmpty());
            }
            }
        }
        System.out.println("S4G_LANDMARK_CONTRACT_PASS checks=" + checks + " certifiedIds=0 nativeRuntime=SEPARATE");
    }
}
