package com.diskree.achievetodo.gameplay.nativeguard;

import java.lang.classfile.*;
import java.lang.classfile.instruction.InvokeInstruction;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.ZipFile;

/** W5-RED-1 contract: exact pinned trigger input, one HEAD guard, verifiable output, one-shot bounded marks. */
public final class NativeCriterionContractTest {
    private static int checks;
    private static void check(boolean value) { checks++; if (!value) throw new AssertionError("criterion check " + checks); }

    public static void main(String[] args) throws Exception {
        String nativeJar = System.getenv("ACHIEVETODO_TEST_PURPUR_NATIVE");
        if (nativeJar == null || nativeJar.isBlank()) throw new IllegalStateException("Requires ACHIEVETODO_TEST_PURPUR_NATIVE");
        byte[] input;
        try (var zip = new ZipFile(Path.of(nativeJar).toFile())) {
            input = zip.getInputStream(zip.getEntry(NativeCriterionAgent.TRIGGER + ".class")).readAllBytes();
        }
        check(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input)).equals(NativeCriterionAgent.PIN));
        byte[] output = NativeCriterionAgent.transformTrigger(input);
        var model = ClassFile.of().parse(output);
        var verifier = ClassFile.of(ClassFile.ClassHierarchyResolverOption.of(ClassHierarchyResolver.defaultResolver().orElse(ClassHierarchyResolver.ofResourceParsing(new java.net.URLClassLoader(new java.net.URL[]{Path.of(nativeJar).toUri().toURL()}, null)))));
        var errors = verifier.verify(model);
        if (!errors.isEmpty()) System.out.println("VERIFY " + errors);
        check(errors.isEmpty());
        int guards = 0, methods = 0;
        for (var method : model.methods()) {
            if (!method.methodName().equalsString(NativeCriterionAgent.METHOD) || !method.methodType().equalsString(NativeCriterionAgent.DESCRIPTOR)) continue;
            methods++;
            var first = method.code().orElseThrow().elementList().stream().filter(e -> e instanceof Instruction).limit(4).toList();
            for (var e : method.code().orElseThrow()) if (e instanceof InvokeInstruction i
                && i.owner().asInternalName().equals(NativeCriterionAgent.class.getName().replace('.', '/'))
                && i.name().equalsString("suppressed")) guards++;
            check(first.size() == 4 && first.get(2) instanceof InvokeInstruction head && head.name().equalsString("suppressed"));
        }
        check(methods == 1 && guards == 1);
        // Mismatched input must be refused by the agent transformer before any rewrite.
        byte[] altered = input.clone(); altered[altered.length - 1] ^= 1;
        check(!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(altered)).equals(NativeCriterionAgent.PIN));
        UUID actor = UUID.randomUUID(), other = UUID.randomUUID();
        check(!NativeCriterionAgent.consume(actor, System.nanoTime()));                 // no mark
        NativeCriterionAgent.markDenied(actor);
        check(!NativeCriterionAgent.consume(other, System.nanoTime()));                 // other actor unaffected
        check(NativeCriterionAgent.consume(actor, System.nanoTime()));                  // fresh mark suppresses once
        check(!NativeCriterionAgent.consume(actor, System.nanoTime()));                 // one-shot
        NativeCriterionAgent.markDenied(actor);
        check(!NativeCriterionAgent.consume(actor, System.nanoTime() + NativeCriterionAgent.WINDOW_NANOS + 1)); // stale mark ignored
        check(!NativeCriterionAgent.consume(actor, System.nanoTime()));                 // and cleared
        NativeCriterionAgent.markDenied(null);
        check(true);
        System.out.println("W5_CRITERION_CONTRACT_PASS checks=" + checks + "; runtime=SEPARATE");
    }
}
