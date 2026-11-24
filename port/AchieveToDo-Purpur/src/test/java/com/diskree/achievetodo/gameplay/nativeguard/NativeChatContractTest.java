package com.diskree.achievetodo.gameplay.nativeguard;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.datapack.EmbeddedDatapack;
import com.diskree.achievetodo.datapack.LegacyChatText;
import java.lang.classfile.*;
import java.lang.classfile.instruction.InvokeInstruction;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;

/**
 * Chat hover text on Purpur (0.3.0): the agent anchor on the real pinned CommandFunction, the namespace filter, and the
 * rewrite of every old-format line in the real BACAP packs and the embedded ability functions.
 */
public final class NativeChatContractTest {
    private static int checks;
    private static void check(boolean ok, String what) { checks++; if (!ok) throw new AssertionError(what); }

    public static void main(String[] args) throws Exception {
        // 1. Real pinned bytes: exactly one inserted call, verifiable class.
        String path = Objects.requireNonNull(System.getenv("ACHIEVETODO_TEST_PURPUR_NATIVE"), "Native jar required");
        try (var jar = new JarFile(path)) {
            byte[] bytes = jar.getInputStream(jar.getJarEntry(NativeChatAgent.OWNER + ".class")).readAllBytes();
            String sha = HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
            check(sha.equals(NativeChatAgent.PIN), "pinned CommandFunction " + sha);
            byte[] out = NativeChatAgent.transformNative(bytes);
            var cf = ClassFile.of();
            int found = 0;
            for (var m : cf.parse(out).methods()) if (m.code().isPresent()) for (var e : m.code().get())
                if (e instanceof InvokeInstruction call && call.owner().asInternalName().endsWith("/NativeChatAgent")) found++;
            check(found == 1, "one migrate call, found " + found);
        }

        // 2. Without the callback nothing changes; the filter matches the Fabric mixin.
        var old = List.of("tellraw @a {\"text\":\"x\",\"hoverEvent\":{\"action\":\"show_text\",\"contents\":\"y\"}}");
        check(NativeChatAgent.migrate("bacap_rewards:msg/a", old) == old, "no callback, unchanged");
        NativeChatAgent.install(LegacyChatText::migrateCommand);
        check(NativeChatAgent.migrate("bacap_rewards:msg/a", old).get(0).contains("\"hover_event\""), "bacap msg migrated");
        check(NativeChatAgent.migrate("bacap_rewards:tab/root", old).get(0).contains("\"hover_event\""), "bacap root migrated");
        check(NativeChatAgent.migrate("achievetodo:abilities/jump", old).get(0).contains("\"hover_event\""), "ability migrated");
        check(NativeChatAgent.migrate("bacap_rewards:reward/a", old).equals(old), "other bacap_rewards untouched");
        check(NativeChatAgent.migrate("minecraft:tick", old).equals(old), "other namespaces untouched");
        check(NativeChatAgent.migrate("blazeandcave:x", old).equals(old), "blazeandcave only with highlight");

        // 3. Every old-format line in the real packs is rewritten into valid JSON with the 26.2 field names.
        String packs = Objects.requireNonNull(System.getenv("ACHIEVETODO_TEST_BACAP"), "BACAP datapacks required");
        int rewritten = 0;
        for (String pack : List.of("bacap.zip", "bacap_override.zip")) {
            try (var zip = new ZipFile(Path.of(packs, pack).toFile())) {
                for (var entry : Collections.list(zip.entries())) {
                    String name = entry.getName();
                    if (!name.endsWith(".mcfunction") || !name.startsWith("data/")) continue;
                    String[] parts = name.split("/", 4);
                    String id = parts[1] + ":" + parts[3].substring(0, parts[3].length() - ".mcfunction".length());
                    var lines = new String(zip.getInputStream(entry).readAllBytes(), StandardCharsets.UTF_8).lines().toList();
                    var migrated = NativeChatAgent.migrate(id, lines);
                    boolean selected = migrated != lines;
                    for (int i = 0; i < lines.size(); i++) {
                        String line = migrated.get(i);
                        // Commented-out lines stay as they are, like in the Fabric mod.
                        if (!selected || !lines.get(i).contains("tellraw ") || lines.get(i).stripLeading().startsWith("#")) continue;
                        if (!lines.get(i).equals(line)) rewritten++;
                        check(!line.contains("hoverEvent") && !line.contains("clickEvent"), "old format left in " + id + ": " + line);
                    }
                }
            }
        }
        check(rewritten >= 1123, "the override pack's old-format messages were rewritten: " + rewritten);

        // 4. The embedded ability unlock messages get hover text too.
        String unlock = EmbeddedDatapack.renderUnlockFunction(AbilityType.JUMP).strip();
        String migrated = NativeChatAgent.migrate("achievetodo:abilities/jump", List.of(unlock)).get(0);
        check(migrated.contains("\"hover_event\":{\"action\":\"show_text\",\"value\":") && migrated.contains("\"click_event\""),
            "ability message migrated: " + migrated);

        // 5. The plugin's rewrite is the Fabric mod's, line for line.
        Path fabric = Path.of("../../src/main/java/com/diskree/achievetodo/client/LegacyChatText.java");
        Path plugin = Path.of("src/main/java/com/diskree/achievetodo/datapack/LegacyChatText.java");
        if (Files.isRegularFile(fabric)) {
            check(body(fabric).equals(body(plugin)), "LegacyChatText differs from the Fabric mod");
        }
        // 6. Bootstrap visibility: the agent support jar (NativeTransactionPremain) holds only nativeguard classes and the
        //    advancement visibility agent. Any other project class they name fails at premain with NoClassDefFoundError.
        Path classes = Path.of("build/classes/java/main/com/diskree/achievetodo/gameplay/nativeguard");
        try (var files = Files.list(classes)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".class")).toList()) {
                for (var entry : ClassFile.of().parse(file).constantPool()) {
                    if (!(entry instanceof java.lang.classfile.constantpool.ClassEntry type)) continue;
                    String name = type.asInternalName();
                    if (!name.startsWith("com/diskree/achievetodo/")) continue;
                    check(name.startsWith("com/diskree/achievetodo/gameplay/nativeguard/Native")
                        || name.startsWith("com/diskree/achievetodo/ui/advancement/AdvancementVisibilityAgent"),
                        file.getFileName() + " references " + name + ", which the bootstrap class loader cannot see");
                }
            }
        }
        System.out.println("ATD_CHAT_CONTRACT_PASS checks=" + checks + " rewrittenLines=" + rewritten);
    }

    /** The class body after the javadoc, ignoring package line and line endings. */
    private static String body(Path file) throws Exception {
        String text = Files.readString(file).replace("\r\n", "\n");
        return text.substring(text.indexOf("public final class LegacyChatText"));
    }
}
