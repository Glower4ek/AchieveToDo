package com.diskree.achievetodo.gameplay.nativeguard;

import java.lang.classfile.*;
import java.lang.constant.*;
import java.lang.instrument.*;
import java.security.*;
import java.util.*;
import java.util.function.Function;

/**
 * Chat hover and click text of advancement and ability messages. The AchieveToDo BACAP override pack and the
 * ability functions still write the pre-1.21.5 text format ({@code hoverEvent}/{@code contents},
 * {@code clickEvent}/{@code value}), which Minecraft 26.2 silently drops, so hovering a message in chat showed
 * nothing on Purpur. The Fabric mod rewrites these lines when a function is parsed (CommandFunctionMixin); this agent
 * does the same at the same place, {@code CommandFunction.fromLines}, with the same namespace filter.
 *
 * <p>The rewrite itself needs Gson, which bootstrap code cannot see, so the plugin bootstrap installs it as a
 * callback before any datapack loads. Without the callback the lines stay unchanged: this is presentation only and
 * never blocks a function from loading.
 */
public final class NativeChatAgent {
    static final String OWNER = "net/minecraft/commands/functions/CommandFunction";
    static final String METHOD = "fromLines";
    static final String DESCRIPTOR = "(Lnet/minecraft/resources/Identifier;Lcom/mojang/brigadier/CommandDispatcher;"
        + "Lnet/minecraft/commands/ExecutionCommandSource;Ljava/util/List;)Lnet/minecraft/commands/functions/CommandFunction;";
    static final String PIN = "5c506d00337198a70e0ca3697e3f269c55017827c142a08ea1526a4eef537f16";
    private static volatile Function<String, String> migration;
    private static volatile boolean transformed;
    private static volatile String failure;

    public static void initialize(Instrumentation instrumentation) {
        instrumentation.addTransformer(new ClassFileTransformer() {
            @Override public byte[] transform(ClassLoader loader, String name, Class<?> type, ProtectionDomain domain, byte[] bytes) {
                if (!OWNER.equals(name)) return null;
                try {
                    String actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
                    if (!actual.equals(PIN)) throw new IllegalStateException("Unpinned chat input " + name + ": " + actual);
                    byte[] result = transformNative(bytes);
                    transformed = true;
                    System.out.println("ATD_CHAT_TRANSFORM " + name);
                    return result;
                } catch (Throwable error) {
                    failure = error.toString();
                    System.out.println("ATD_CHAT_TRANSFORM_FAILED " + failure);
                    return null;
                }
            }
        });
    }

    static byte[] transformNative(byte[] bytes) {
        var cf = ClassFile.of();
        int[] sites = {0};
        byte[] output = cf.transformClass(cf.parse(bytes), (cb, e) -> {
            if (e instanceof MethodModel m && m.methodName().equalsString(METHOD) && m.methodType().equalsString(DESCRIPTOR)) {
                cb.transformMethod(m, MethodTransform.transformingCode(new CodeTransform() {
                    @Override public void atStart(CodeBuilder b) {
                        // lines = NativeChatAgent.migrate(id, lines): static method, id in slot 0, lines in slot 3.
                        b.aload(0).aload(3).invokestatic(ClassDesc.of(NativeChatAgent.class.getName()), "migrate",
                            MethodTypeDesc.of(ConstantDescs.CD_List, ConstantDescs.CD_Object, ConstantDescs.CD_List)).astore(3);
                        sites[0]++;
                    }
                    @Override public void accept(CodeBuilder b, CodeElement element) { b.with(element); }
                }));
            } else cb.with(e);
        });
        if (sites[0] != 1) throw new IllegalStateException("Chat anchor count=" + sites[0]);
        return output;
    }

    /** Installed by the plugin bootstrap; the function rewrites one command line. */
    public static void install(Function<String, String> rewrite) { migration = Objects.requireNonNull(rewrite); }

    /** Same selection as Fabric CommandFunctionMixin. Never throws: a failure keeps the original lines. */
    public static List<String> migrate(Object id, List<String> lines) {
        var rewrite = migration;
        if (rewrite == null || id == null || lines == null) return lines;
        try {
            String value = id.toString();
            int colon = value.indexOf(':');
            String namespace = colon < 0 ? "minecraft" : value.substring(0, colon), path = value.substring(colon + 1);
            boolean all = (namespace.equals("bacap_rewards") && (path.startsWith("msg/") || path.endsWith("/root")))
                || namespace.equals("achievetodo");
            boolean highlight = namespace.equals("blazeandcave");
            if (!all && !highlight) return lines;
            var result = new ArrayList<String>(lines.size());
            for (String line : lines)
                result.add(all || line.contains("/advancementssearch highlight ") ? rewrite.apply(line) : line);
            return result;
        } catch (RuntimeException | LinkageError unavailable) { return lines; }
    }

    public static boolean installed() { return transformed && failure == null && migration != null; }
    private NativeChatAgent() {}
}
