package atd.trace;

import java.lang.classfile.*;
import java.lang.classfile.instruction.ReturnInstruction;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.*;

/**
 * Test-only server agent: prints the return value of selected methods as
 * {@code event=RETURN_<owner>.<name>(<desc>) value=<result>} on stdout, the format campaign.py parses
 * for {@code trace:} oracles. Replaces Codex's lost causal-agent.jar.
 *
 * <p>Agent argument: comma-separated {@code owner/internal/Name.method} targets, e.g.
 * {@code net/minecraft/world/item/Item.use,net/minecraft/world/item/FireworkRocketItem.use}.
 * Every reference-returning overload of the named method is traced. Only JDK classes are referenced
 * by the injected code, so no helper class has to be visible to the server class loader. Load it after
 * the product agent so the product's early returns are traced too.
 */
public final class TraceAgent {
    private static final ClassDesc STRING = ConstantDescs.CD_String;
    private static final ClassDesc PRINT_STREAM = ClassDesc.of("java.io.PrintStream");
    private static final ClassDesc SYSTEM = ClassDesc.of("java.lang.System");

    public static void premain(String args, Instrumentation instrumentation) {
        Map<String, Set<String>> targets = new HashMap<>();
        for (String target : (args == null ? "" : args).split(",")) {
            target = target.trim();
            int dot = target.lastIndexOf('.');
            if (dot <= 0) continue;
            targets.computeIfAbsent(target.substring(0, dot), k -> new HashSet<>()).add(target.substring(dot + 1));
        }
        System.out.println("ATD_TRACE_AGENT targets=" + targets);
        instrumentation.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String name, Class<?> type, ProtectionDomain domain, byte[] bytes) {
                Set<String> methods = targets.get(name);
                if (methods == null) return null;
                try {
                    byte[] result = instrument(loader, name, methods, bytes);
                    System.out.println("ATD_TRACE_AGENT instrumented " + name + " " + methods);
                    return result;
                } catch (Throwable failure) {
                    System.out.println("ATD_TRACE_AGENT failed " + name + " " + failure);
                    return null;
                }
            }
        });
    }

    static byte[] instrument(ClassLoader loader, String owner, Set<String> methods, byte[] bytes) {
        var resolver = ClassHierarchyResolver.defaultResolver()
            .orElse(ClassHierarchyResolver.ofResourceParsing(loader == null ? ClassLoader.getSystemClassLoader() : loader));
        var classFile = ClassFile.of(ClassFile.ClassHierarchyResolverOption.of(resolver));
        ClassModel model = classFile.parse(bytes);
        ClassTransform transform = (classBuilder, element) -> {
            if (!(element instanceof MethodModel method)
                || !methods.contains(method.methodName().stringValue())
                || !method.methodTypeSymbol().returnType().isClassOrInterface()) {
                classBuilder.with(element);
                return;
            }
            String prefix = "event=RETURN_" + owner + "." + method.methodName().stringValue()
                + method.methodType().stringValue() + " value=";
            classBuilder.transformMethod(method, MethodTransform.transformingCode((builder, code) -> {
                    if (code instanceof ReturnInstruction ret && ret.opcode() == Opcode.ARETURN) {
                        // [ret] -> [ret, ret] -> print(prefix + String.valueOf(ret)) -> [ret]
                        builder.dup()
                            .getstatic(SYSTEM, "out", PRINT_STREAM)
                            .swap()
                            .invokestatic(STRING, "valueOf", MethodTypeDesc.of(STRING, ConstantDescs.CD_Object))
                            .ldc(prefix)
                            .swap()
                            .invokevirtual(STRING, "concat", MethodTypeDesc.of(STRING, STRING))
                            .invokevirtual(PRINT_STREAM, "println", MethodTypeDesc.of(ConstantDescs.CD_void, STRING));
                    }
                    builder.with(code);
            }));
        };
        return classFile.transformClass(model, transform);
    }
}
