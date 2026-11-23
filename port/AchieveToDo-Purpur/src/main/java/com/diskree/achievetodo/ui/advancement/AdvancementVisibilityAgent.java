package com.diskree.achievetodo.ui.advancement;

import java.lang.classfile.*;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.constant.*;
import java.lang.instrument.*;
import java.security.*;
import java.util.*;
import java.util.function.Predicate;

/** Presentation parity with Fabric AdvancementDisplaysMixin; never awards progress. */
public final class AdvancementVisibilityAgent {
    private static final String TARGET = "net/minecraft/server/advancements/AdvancementVisibilityEvaluator";
    private static final String PIN = "be9a7d5ff24394fc7000e11ebfff2b59510d190e33b67ad524cd99e877d32424";
    private static final ClassDesc SELF = ClassDesc.of(AdvancementVisibilityAgent.class.getName());
    private static final String NODE = "net/minecraft/advancements/AdvancementNode";
    private static final String NODE_PIN = "310ec20563ee91666a0f71e907a71d092ae6f22896f568cd3ae965161560832e";
    private static volatile boolean transformed;
    private static volatile boolean childrenTransformed;
    private static volatile String failure;
    private static final Set<String> ROOTS = Set.of("achievetodo:abilities/root",
        "blazeandcave:bacap/root", "blazeandcave:statistics/root", "blazeandcave:building/root",
        "blazeandcave:farming/root", "blazeandcave:husbandry/root", "blazeandcave:biomes/root",
        "blazeandcave:adventure/root", "blazeandcave:monsters/root", "blazeandcave:weaponry/root",
        "blazeandcave:mining/root", "blazeandcave:redstone/root", "blazeandcave:enchanting/root",
        "blazeandcave:nether/root", "blazeandcave:potion/root", "blazeandcave:end/root",
        "minecraft:adventure/root", "minecraft:husbandry/root", "minecraft:nether/root", "minecraft:end/root");

    private static final Map<String, List<String>> CHILD_ORDER = new HashMap<>();
    static {
        CHILD_ORDER.put("blazeandcave:statistics/root", List.of(
            "blazeandcave:statistics/out_for_a_stroll",
            "blazeandcave:statistics/natural_sprinter",
            "blazeandcave:statistics/sneaky_snitch",
            "blazeandcave:statistics/laps_in_the_pool",
            "blazeandcave:statistics/luxury_cruise",
            "blazeandcave:technical/big_cheater",
            "blazeandcave:statistics/minecart_rider",
            "blazeandcave:statistics/pig_training",
            "blazeandcave:statistics/taking_it_in_stride",
            "blazeandcave:statistics/horse_training",
            "blazeandcave:statistics/take_to_the_skies"
        ));
        CHILD_ORDER.put("blazeandcave:technical/you_are_a_big_cheater", List.of(
            "blazeandcave:statistics/the_first_night",
            "blazeandcave:statistics/spring_in_your_step",
            "blazeandcave:statistics/om_nom_nom",
            "blazeandcave:statistics/mob_hunter",
            "blazeandcave:statistics/level_up",
            "blazeandcave:statistics/loot_em",
            "blazeandcave:statistics/the_parrots_and_the_bats",
            "blazeandcave:statistics/novice_enchanter",
            "blazeandcave:statistics/the_haggler"
        ));
        CHILD_ORDER.put("blazeandcave:challenges/root", List.of(
            "blazeandcave:challenges/nuclear_fusion",
            "blazeandcave:challenges/ad_astra",
            "blazeandcave:challenges/all_the_blocks",
            "blazeandcave:challenges/ultimate_enchanter",
            "blazeandcave:challenges/i_am_loot",
            "blazeandcave:challenges/telescopic",
            "blazeandcave:challenges/were_in_the_endgame_now"
        ));
    }


    public static void initialize(Instrumentation instrumentation) {
        instrumentation.addTransformer(new ClassFileTransformer() {
            @Override public byte[] transform(ClassLoader loader, String name, Class<?> type,
                    ProtectionDomain domain, byte[] bytes) {
                if (!TARGET.equals(name) && !NODE.equals(name)) return null;
                try {
                    byte[] result = NODE.equals(name) ? transformChildrenPinned(bytes, loader) : transformPinned(bytes, loader);
                    if (NODE.equals(name)) childrenTransformed = true;
                    else transformed = true;
                    System.out.println("ATD_UI_VISIBILITY_TRANSFORM pinned=2620 presentationOnly=true");
                    return result;
                } catch (IllegalArgumentException | IllegalStateException invalid) {
                    failure = invalid.toString();
                    System.err.println("ATD_UI_VISIBILITY_FAILURE " + failure);
                    return null; // Plugin startup verification rejects admission below.
                }
            }
        });
    }

    public static void verify(ClassLoader loader) throws ClassNotFoundException {
        Class.forName(NODE.replace('/', '.'), true, loader);
        Class.forName(TARGET.replace('/', '.'), true, loader);
        if (!transformed || !childrenTransformed || failure != null) throw new IllegalStateException("ATD_UI_VISIBILITY_UNAVAILABLE " + failure);
    }

    static byte[] transformPinned(byte[] bytes, ClassLoader loader) {
        try {
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            if (!hash.equals(PIN)) throw new IllegalArgumentException("Unpinned UI visibility evaluator: " + hash);
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
        var cf = ClassFile.of(ClassFile.ClassHierarchyResolverOption.of(ClassHierarchyResolver.ofResourceParsing(loader)));
        int[] sites = {0, 0};
        byte[] result = cf.transformClass(cf.parse(bytes), (builder, element) -> {
            if (element instanceof MethodModel method && method.methodName().equalsString("evaluateVisibility")
                    && method.methodType().stringValue().startsWith("(Lnet/minecraft/advancements/AdvancementNode;Lit/unimi/dsi/fastutil/Stack;")) {
                builder.transformMethod(method, MethodTransform.transformingCode((code, instruction) -> {
                    if (instruction instanceof InvokeInstruction call && call.owner().asInternalName().equals("java/util/function/Predicate")
                            && call.name().equalsString("test") && call.type().equalsString("(Ljava/lang/Object;)Z")) {
                        sites[0]++;
                        code.invokestatic(SELF, "visibleOrCompleted", MethodTypeDesc.of(ConstantDescs.CD_boolean,
                            ClassDesc.of("java.util.function.Predicate"), ConstantDescs.CD_Object));
                    } else if (instruction instanceof InvokeInstruction call && call.owner().asInternalName().equals(TARGET)
                            && call.name().equalsString("evaluateVisiblityForUnfinishedNode")) {
                        sites[1]++;
                        code.aload(0).invokestatic(SELF, "unfinishedVisibility", MethodTypeDesc.of(ConstantDescs.CD_boolean,
                            ConstantDescs.CD_Object, ConstantDescs.CD_Object));
                    } else code.with(instruction);
                }));
            } else builder.with(element);
        });
        if (sites[0] != 1 || sites[1] != 1) throw new IllegalStateException("UI visibility sites=" + Arrays.toString(sites));
        return result;
    }


    static byte[] transformChildrenPinned(byte[] bytes, ClassLoader loader) {
        try {
            if (!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)).equals(NODE_PIN))
                throw new IllegalArgumentException("Unpinned UI advancement node");
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
        var cf = ClassFile.of(ClassFile.ClassHierarchyResolverOption.of(ClassHierarchyResolver.ofResourceParsing(loader)));
        int[] sites = {0};
        byte[] result = cf.transformClass(cf.parse(bytes), (builder, element) -> {
            if (element instanceof MethodModel method && method.methodName().equalsString("children")
                    && method.methodType().equalsString("()Ljava/lang/Iterable;")) {
                builder.transformMethod(method, MethodTransform.transformingCode((code, instruction) -> {
                    if (instruction instanceof java.lang.classfile.instruction.ReturnInstruction ret
                            && ret.opcode() == Opcode.ARETURN) {
                        sites[0]++;
                        code.aload(0).invokestatic(SELF, "sortedChildren", MethodTypeDesc.of(ClassDesc.of("java.lang.Iterable"),
                            ClassDesc.of("java.lang.Iterable"), ConstantDescs.CD_Object));
                    }
                    code.with(instruction);
                }));
            } else builder.with(element);
        });
        if (sites[0] != 1) throw new IllegalStateException("UI children return sites=" + sites[0]);
        return result;
    }

    private static String nodeId(Object node) {
        try {
            Object holder = node.getClass().getMethod("holder").invoke(node);
            return holder.getClass().getMethod("id").invoke(holder).toString();
        } catch (ReflectiveOperationException invalid) {
            throw new IllegalStateException("ATD_UI_NODE_ID_MISMATCH", invalid);
        }
    }

    public static Iterable<?> sortedChildren(Iterable<?> children, Object node) {
        try {
            Object root = node.getClass().getMethod("root").invoke(node);
            String rootId = nodeId(root);
            if (!ROOTS.contains(rootId) && !rootId.equals("blazeandcave:challenges/root")) return children;
            List<Object> ordered = new ArrayList<>(); children.forEach(ordered::add);
            List<String> custom = CHILD_ORDER.get(nodeId(node));
            if (custom == null) ordered.sort((left, right) -> compareIds(nodeId(left), nodeId(right)));
            else ordered.sort(Comparator.comparingInt(child -> custom.indexOf(nodeId(child))));
            return ordered;
        } catch (ReflectiveOperationException invalid) {
            throw new IllegalStateException("ATD_UI_CHILDREN_ACCESSOR_MISMATCH", invalid);
        }
    }

    // Identifier.compareTo in 26.2 compares path first, then namespace. Comparing
    // the complete string instead would reorder mixed vanilla/BACAP branches.
    static int compareIds(String left, String right) {
        int l = left.indexOf(':'), r = right.indexOf(':');
        int path = left.substring(l + 1).compareTo(right.substring(r + 1));
        return path != 0 ? path : left.substring(0, l).compareTo(right.substring(0, r));
    }

    static boolean showUnfinished(String root, boolean isRoot, boolean hasDisplay, boolean hidden) {
        return ROOTS.contains(root) && !isRoot && hasDisplay && !hidden;
    }

    static boolean unfinishedVisibility(List<?> rules, String root) {
        boolean canonical = ROOTS.contains(root) || root.equals("blazeandcave:challenges/root");
        int depth = canonical ? rules.size() - 1 : 2;
        for (int i = 0; i <= depth; i++) {
            String rule = rules.get(rules.size() - 1 - i).toString();
            if (rule.equals("SHOW")) return true;
            if (rule.equals("HIDE")) return false;
        }
        return false;
    }

    public static boolean unfinishedVisibility(Object stack, Object node) {
        try {
            // The pinned evaluator uses ObjectArrayList as its rule stack. Match
            // Fabric's full-ancestry scan, including Challenges, while retaining
            // native depth for unrelated packs and every HIDE barrier.
            Object root = node.getClass().getMethod("root").invoke(node);
            return unfinishedVisibility((List<?>) stack, nodeId(root));
        } catch (ReflectiveOperationException invalid) {
            throw new IllegalStateException("ATD_UI_VISIBILITY_ROOT_MISMATCH", invalid);
        }
    }

    public static boolean visibleOrCompleted(Predicate<Object> completed, Object node) {
        try {
            // Bootstrap helper cannot link plugin or NMS classes. Only public pinned
            // node/display accessors are reflected; the original completion predicate
            // is still used for roots, hidden nodes, Challenges and unknown packs.
            Class<?> type = node.getClass();
            Object root = type.getMethod("root").invoke(node);
            Object holder = type.getMethod("holder").invoke(root);
            String id = holder.getClass().getMethod("id").invoke(holder).toString();
            Object advancement = type.getMethod("advancement").invoke(node);
            Optional<?> display = (Optional<?>) advancement.getClass().getMethod("display").invoke(advancement);
            boolean hidden = display.isPresent() && (boolean) display.get().getClass().getMethod("isHidden").invoke(display.get());
            if (showUnfinished(id, node == root, display.isPresent(), hidden)) return true;
            return completed.test(node);
        } catch (ReflectiveOperationException invalid) {
            throw new IllegalStateException("ATD_UI_VISIBILITY_ACCESSOR_MISMATCH", invalid);
        }
    }

    private AdvancementVisibilityAgent() {}
}
