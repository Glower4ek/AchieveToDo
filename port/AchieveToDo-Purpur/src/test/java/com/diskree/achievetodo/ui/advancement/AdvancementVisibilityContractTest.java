package com.diskree.achievetodo.ui.advancement;

import java.nio.file.Path;
import java.util.jar.JarFile;
import java.lang.classfile.*;
import java.lang.classfile.instruction.InvokeInstruction;

public final class AdvancementVisibilityContractTest {
    public static void main(String[] args) throws Exception {
        int checks = 0;
        for (String category : new String[]{"bacap", "statistics", "building", "farming", "husbandry", "biomes",
                "adventure", "monsters", "weaponry", "mining", "redstone", "enchanting", "nether", "potion", "end"}) {
            String root = "blazeandcave:" + category + "/root";
            require(AdvancementVisibilityAgent.showUnfinished(root, false, true, false)); checks++;
            require(!AdvancementVisibilityAgent.showUnfinished(root, true, true, false)); checks++;
            require(!AdvancementVisibilityAgent.showUnfinished(root, false, true, true)); checks++;
            require(!AdvancementVisibilityAgent.showUnfinished(root, false, false, false)); checks++;
        }
        for (String root : new String[]{"blazeandcave:challenges/root", "other:statistics/root", "minecraft:story/root", "blazeandcave:unknown/root"}) {
            require(!AdvancementVisibilityAgent.showUnfinished(root, false, true, false)); checks++;
        }
        require(AdvancementVisibilityAgent.showUnfinished("achievetodo:abilities/root", false, true, false)); checks++;
        var deep = java.util.List.of("NO_CHANGE", "SHOW", "NO_CHANGE", "NO_CHANGE", "NO_CHANGE", "NO_CHANGE");
        require(AdvancementVisibilityAgent.unfinishedVisibility(deep, "blazeandcave:challenges/root")); checks++;
        require(!AdvancementVisibilityAgent.unfinishedVisibility(deep, "other:challenges/root")); checks++;
        require(!AdvancementVisibilityAgent.unfinishedVisibility(java.util.List.of("SHOW", "HIDE", "NO_CHANGE"),
            "blazeandcave:challenges/root")); checks++;
        for (String category : new String[]{"adventure", "husbandry", "nether", "end"}) {
            require(AdvancementVisibilityAgent.showUnfinished("minecraft:" + category + "/root", false, true, false)); checks++;
        }
        Path nativeJar = Path.of(System.getenv("ACHIEVETODO_TEST_PURPUR_NATIVE"));
        try (var jar = new JarFile(nativeJar.toFile());
             var loader = new java.net.URLClassLoader(new java.net.URL[]{nativeJar.toUri().toURL()}, AdvancementVisibilityContractTest.class.getClassLoader())) {
            byte[] original = jar.getInputStream(jar.getJarEntry("net/minecraft/server/advancements/AdvancementVisibilityEvaluator.class")).readAllBytes();
            byte[] changed = AdvancementVisibilityAgent.transformPinned(original, loader);
            int helper = 0;
            for (var method : ClassFile.of().parse(changed).methods()) if (method.code().isPresent())
                for (var instruction : method.code().get()) if (instruction instanceof InvokeInstruction call
                    && call.name().equalsString("visibleOrCompleted")) helper++;
            require(helper == 1); checks++;
            byte[] node = jar.getInputStream(jar.getJarEntry("net/minecraft/advancements/AdvancementNode.class")).readAllBytes();
            byte[] ordered = AdvancementVisibilityAgent.transformChildrenPinned(node, loader);
            int orderSites = 0;
            for (var method : ClassFile.of().parse(ordered).methods()) if (method.code().isPresent())
                for (var instruction : method.code().get()) if (instruction instanceof InvokeInstruction call
                    && call.name().equalsString("sortedChildren")) orderSites++;
            require(orderSites == 1); checks++;
            node[node.length - 1] ^= 1;
            try { AdvancementVisibilityAgent.transformChildrenPinned(node, loader); throw new AssertionError("tampered node accepted"); }
            catch (IllegalArgumentException expected) { checks++; }
            byte[] corrupt = original.clone(); corrupt[corrupt.length - 1] ^= 1;
            try { AdvancementVisibilityAgent.transformPinned(corrupt, loader); throw new AssertionError("tampered native accepted"); }
            catch (IllegalArgumentException expected) { checks++; }
        }
        var parent = new Node("blazeandcave:statistics/root", null);
        var a = new Node("blazeandcave:statistics/sneaky_snitch", parent);
        var b = new Node("blazeandcave:statistics/out_for_a_stroll", parent);
        var c = new Node("blazeandcave:statistics/natural_sprinter", parent);
        var input = java.util.List.of(a, c, b);
        require(new java.util.ArrayList<>((java.util.Collection<?>)AdvancementVisibilityAgent.sortedChildren(input, parent))
            .equals(java.util.List.of(b, c, a))); checks++;
        var unknown = new Node("other:statistics/root", null);
        require(AdvancementVisibilityAgent.sortedChildren(input, unknown) == input); checks++;
        require(AdvancementVisibilityAgent.compareIds("minecraft:adventure/a", "blazeandcave:adventure/z") < 0); checks++;
        require(AdvancementVisibilityAgent.compareIds("minecraft:adventure/a", "blazeandcave:adventure/a") > 0); checks++;
        System.out.println("ATD_UI_VISIBILITY_CONTRACT_PASS checks=" + checks + " runtime=SEPARATE");
    }
    private static void require(boolean value) { if (!value) throw new AssertionError(); }
    public record Holder(String id) {}
    public record Node(String id, Node parent) {
        public Node root() { return parent == null ? this : parent.root(); }
        public Holder holder() { return new Holder(id); }
    }
}
