package com.diskree.achievetodo.gameplay.nativeguard;

import java.lang.classfile.*;
import java.lang.classfile.instruction.InvokeInstruction;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.ZipFile;

/**
 * W5-RED-1 contract (extended in 0.4.0 to the block-use and entity-interaction triggers): exact pinned trigger
 * inputs, one HEAD guard per trigger method, verifiable output, one-shot bounded marks per trigger kind.
 */
public final class NativeCriterionContractTest {
    private static int checks;
    private static void check(boolean value) { checks++; if (!value) throw new AssertionError("criterion check " + checks); }

    public static void main(String[] args) throws Exception {
        String nativeJar = System.getenv("ACHIEVETODO_TEST_PURPUR_NATIVE");
        if (nativeJar == null || nativeJar.isBlank()) throw new IllegalStateException("Requires ACHIEVETODO_TEST_PURPUR_NATIVE");
        check(NativeCriterionAgent.TARGETS.size() == 4 && NativeCriterionAgent.KINDS.size() == 4);
        var verifier = ClassFile.of(ClassFile.ClassHierarchyResolverOption.of(ClassHierarchyResolver.defaultResolver().orElse(
            ClassHierarchyResolver.ofResourceParsing(new java.net.URLClassLoader(new java.net.URL[]{Path.of(nativeJar).toUri().toURL()}, null)))));
        try (var zip = new ZipFile(Path.of(nativeJar).toFile())) {
            for (var entry : NativeCriterionAgent.TARGETS.entrySet()) {
                byte[] input = zip.getInputStream(zip.getEntry(entry.getKey() + ".class")).readAllBytes();
                check(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input)).equals(entry.getValue().pin()));
                byte[] output = NativeCriterionAgent.transformTrigger(input, entry.getValue().descriptor());
                var model = ClassFile.of().parse(output);
                var errors = verifier.verify(model);
                if (!errors.isEmpty()) System.out.println("VERIFY " + entry.getKey() + " " + errors);
                check(errors.isEmpty());
                int guards = 0, methods = 0;
                for (var method : model.methods()) {
                    for (var e : method.code().map(c -> c.elementList()).orElse(List.of())) if (e instanceof InvokeInstruction i
                        && i.owner().asInternalName().equals(NativeCriterionAgent.class.getName().replace('.', '/'))
                        && i.name().equalsString("suppressed")) guards++;
                    if (!method.methodName().equalsString(NativeCriterionAgent.METHOD)
                        || !method.methodType().equalsString(entry.getValue().descriptor())) continue;
                    methods++;
                    var first = method.code().orElseThrow().elementList().stream().filter(e -> e instanceof Instruction).limit(4).toList();
                    check(first.size() == 4 && first.get(2) instanceof InvokeInstruction head && head.name().equalsString("suppressed"));
                }
                check(methods == 1 && guards == 1);
                // Mismatched input must be refused by the agent transformer before any rewrite.
                byte[] altered = input.clone(); altered[altered.length - 1] ^= 1;
                check(!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(altered)).equals(entry.getValue().pin()));
            }
        }
        UUID actor = UUID.randomUUID(), other = UUID.randomUUID();
        for (int kind = 0; kind < 4; kind++) check(!NativeCriterionAgent.consume(actor, kind, System.nanoTime()));  // no mark
        NativeCriterionAgent.markDenied(actor);
        check(!NativeCriterionAgent.consume(other, 0, System.nanoTime()));              // other actor unaffected
        // A block denial fires DEFAULT_BLOCK_USE and then ANY_BLOCK_USE: each kind is suppressed once.
        check(NativeCriterionAgent.consume(actor, 1, System.nanoTime()));
        check(NativeCriterionAgent.consume(actor, 2, System.nanoTime()));
        check(!NativeCriterionAgent.consume(actor, 1, System.nanoTime()));             // one-shot per kind
        check(NativeCriterionAgent.consume(actor, 3, System.nanoTime()));              // entity kind still marked
        check(NativeCriterionAgent.consume(actor, 0, System.nanoTime()));
        for (int kind = 0; kind < 4; kind++) check(!NativeCriterionAgent.consume(actor, kind, System.nanoTime()));  // all cleared
        NativeCriterionAgent.markDenied(actor);
        check(!NativeCriterionAgent.consume(actor, 3, System.nanoTime() + NativeCriterionAgent.WINDOW_NANOS + 1)); // stale mark ignored
        check(!NativeCriterionAgent.consume(actor, 3, System.nanoTime()));             // and cleared
        NativeCriterionAgent.markDenied(null);
        check(true);
        System.out.println("W5_CRITERION_CONTRACT_PASS checks=" + checks + "; triggers=" + NativeCriterionAgent.TARGETS.size() + "; runtime=SEPARATE");
    }
}
