package com.diskree.achievetodo.client;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.server.AchieveToDoServer;
import com.google.gson.JsonParser;
import com.mojang.brigadier.StringReader;
import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import com.diskree.achievetodo.client.gui.AbilityLockLines;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.ServerScoreboard;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;
import static org.junit.jupiter.api.Assertions.*;

class Pre26SmokeRegressionTest {
    private static net.minecraft.core.HolderLookup.Provider lookup;
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion(); Bootstrap.bootStrap();
        lookup = VanillaRegistries.createLookup();
        net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(lookup)
            .forEach(net.minecraft.core.component.DataComponentInitializers.PendingComponents::apply);
    }

    static String convertFunction(String raw) throws Exception {
        var method = ExternalPackCompatibility.class.getDeclaredMethod("convertFunction", String.class);
        method.setAccessible(true);
        var result = method.invoke(null, raw);
        var text = result.getClass().getDeclaredMethod("text"); text.setAccessible(true);
        return (String) text.invoke(result);
    }

    @Test void flowerRewardUsesTranslatedStyledComponentsAndSeparateLoreLines() throws Exception {
        try (var zip = new ZipFile("reference/phase_a_preservation/files/final/bacap.zip")) {
            var entry = zip.getEntry("data/bacap_rewards/function/trophy/biomes/for_you_my_sweet.mcfunction");
            var line = convertFunction(new String(zip.getInputStream(entry).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)).lines().findFirst().orElseThrow();
            var stack = new ItemParser(lookup).parse(new StringReader(line.substring("give @s ".length()))).createItemStack(1);
            var name = stack.get(DataComponents.CUSTOM_NAME);
            assertInstanceOf(TranslatableContents.class, name.getContents(), name.toString());
            assertEquals("A blessing in love", ((TranslatableContents)name.getContents()).getKey());
            assertTrue(name.getStyle().isBold()); assertFalse(name.getStyle().isItalic());
            assertEquals(4, stack.get(DataComponents.LORE).lines().size());
            assertInstanceOf(TranslatableContents.class, stack.get(DataComponents.LORE).lines().getFirst().getContents());
        }
    }

    @Test void earnedWashingMachineChatContainsHoverAndClickMetadata() throws Exception {
        var line = LegacyChatText.migrateCommand(Files.readString(Path.of("src/main/resources/resourcepacks/bacap_override/data/bacap_rewards/function/msg/building/washing_machine.mcfunction")));
        var component = ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(line.substring(line.indexOf('{')))).getOrThrow();
        var args = ((TranslatableContents)component.getContents()).getArgs();
        var advancement = (net.minecraft.network.chat.Component)args[2];
        assertNotNull(advancement.getStyle().getHoverEvent());
        assertNotNull(advancement.getStyle().getClickEvent());
    }

    @Test void sharedAbilityFormatterRestoresTwoLines() {
        for (var type : AbilityType.values()) {
            for (int count : List.of(1, 2, 10)) {
                var message = type.buildUnlockProgressMessage(count);
                var text = message.getString();
                assertTrue(text.contains(".\n"), type + ": " + text);
                assertEquals(2, text.split("\n", -1).length);
                assertTrue(AbilityLockLines.isAbilityLock(message));
                assertEquals(2, AbilityLockLines.split(message).size());
                var remaining = (Component)message.getSiblings().getLast();
                assertEquals(count, ((TranslatableContents)remaining.getContents()).getArgs()[0]);
            }
            assertEquals(2, AbilityLockLines.split(type.buildPermanentlyLockedMessage()).size());
        }
        assertFalse(AbilityLockLines.isAbilityLock(Component.literal("unrelated\nmessage")));
    }

    @Test void all215CustomRewardsMatchSourceComponentsAndSurviveNbtReload() throws Exception {
        var parser = new ItemParser(lookup);
        var ops = RegistryOps.create(NbtOps.INSTANCE, lookup);
        int count = 0;
        try (var zip = new ZipFile("reference/phase_a_preservation/files/final/bacap.zip")) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (!entry.getName().endsWith(".mcfunction")) continue;
                String raw = new String(zip.getInputStream(entry).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                for (String line : raw.lines().toList()) {
                    if (!line.startsWith("give @s ") || (!line.contains("custom_name=") && !line.contains("item_name="))) continue;
                    String converted = convertFunction(line);
                    boolean custom = line.contains("custom_name=");
                    String key = custom ? "custom_name=" : "item_name=";
                    var nameType = custom ? DataComponents.CUSTOM_NAME : DataComponents.ITEM_NAME;
                    var rawName = net.minecraft.nbt.TagParser.create(NbtOps.INSTANCE).parseAsArgument(new StringReader(line.substring(line.indexOf(key)+key.length())));
                    var legacyName = ComponentSerialization.CODEC.parse(NbtOps.INSTANCE, rawName).getOrThrow();
                    var stack = parser.parse(new StringReader(converted.substring(8))).createItemStack(1);
                    assertEquals(decodeLegacy(legacyName), stack.get(nameType), entry.getName());
                    var afterLore = stack.get(DataComponents.LORE);
                    if (line.contains("lore=")) {
                        var beforeLore = (net.minecraft.nbt.ListTag)net.minecraft.nbt.TagParser.create(NbtOps.INSTANCE).parseAsArgument(new StringReader(line.substring(line.indexOf("lore=")+5)));
                        assertEquals(beforeLore.size(), afterLore.lines().size());
                        for (int i = 0; i < beforeLore.size(); i++) assertEquals(decodeLegacy(ComponentSerialization.CODEC.parse(NbtOps.INSTANCE,beforeLore.get(i)).getOrThrow()), afterLore.lines().get(i), entry.getName());
                    }
                    var nameCodec = nameType.codec();
                    var name = stack.get(nameType);
                    assertEquals(name, nameCodec.parse(ops, nameCodec.encodeStart(ops, name).getOrThrow()).getOrThrow());
                    if (afterLore != null) {
                        var loreCodec = DataComponents.LORE.codec();
                        assertEquals(afterLore, loreCodec.parse(ops, loreCodec.encodeStart(ops, afterLore).getOrThrow()).getOrThrow());
                    }
                    assertEquals(converted, convertFunction(converted), "idempotence " + entry.getName());
                    count++;
                }
            }
        }
        assertEquals(215, count);
    }

    private static Component decodeLegacy(Component legacy) {
        return ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(legacy.getString())).getOrThrow();
    }

    @Test void rewardAndAbilityComponentsRenderRussianWithSourceEquivalentFallback() throws Exception {
        var original = net.minecraft.locale.Language.getInstance();
        var translations = new HashMap<String, String>();
        for (String path : List.of("assets/minecraft/lang/ru_ru.json", "assets/achievetodo/lang/ru_ru.json")) {
            try (var stream = Files.newInputStream(Path.of("src/main/resources", path))) {
                net.minecraft.locale.Language.loadFromJson(stream, translations::put);
            }
        }
        var russian = new net.minecraft.locale.Language() {
            @Override public String getOrDefault(String key, String fallback) { return translations.getOrDefault(key, original.getOrDefault(key, fallback)); }
            @Override public boolean has(String key) { return translations.containsKey(key) || original.has(key); }
            @Override public boolean isDefaultRightToLeft() { return false; }
            @Override public net.minecraft.util.FormattedCharSequence getVisualOrder(net.minecraft.network.chat.FormattedText text) { return original.getVisualOrder(text); }
        };
        try {
            net.minecraft.locale.Language.inject(russian);
            assertEquals("Цветик-семицветик", Component.translatable("A blessing in love").getString());
            for (var type : AbilityType.values()) for (int count : List.of(1, 2, 10)) {
                var lines = AbilityLockLines.split(type.buildUnlockProgressMessage(count));
                assertEquals(2, lines.size());
                assertFalse(lines.getFirst().getString().contains("achievetodo."));
                assertEquals("Для разблокировки осталось выполнить достижений: " + count, lines.getLast().getString());
            }
            assertEquals("Вы не можете присесть.", AbilityLockLines.split(AbilityType.SNEAK.buildUnlockProgressMessage(10)).getFirst().getString());
            try (var zip = new ZipFile("reference/phase_a_preservation/files/final/bacap.zip")) {
                for (var entry : Collections.list(zip.entries())) {
                    if (!entry.getName().endsWith(".mcfunction")) continue;
                    for (String line : new String(zip.getInputStream(entry).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).lines().toList()) {
                        if (!line.startsWith("give @s ") || (!line.contains("custom_name=") && !line.contains("item_name="))) continue;
                        var stack = new ItemParser(lookup).parse(new StringReader(convertFunction(line).substring(8))).createItemStack(1);
                        var name = stack.get(line.contains("custom_name=") ? DataComponents.CUSTOM_NAME : DataComponents.ITEM_NAME);
                        String key = line.contains("custom_name=") ? "custom_name=" : "item_name=";
                        var beforeName = net.minecraft.nbt.TagParser.create(NbtOps.INSTANCE).parseAsArgument(new StringReader(line.substring(line.indexOf(key) + key.length())));
                        assertEquals(decodeLegacy(ComponentSerialization.CODEC.parse(NbtOps.INSTANCE, beforeName).getOrThrow()).getString(), name.getString(), entry.getName());
                        var beforeLore = (net.minecraft.nbt.ListTag)net.minecraft.nbt.TagParser.create(NbtOps.INSTANCE).parseAsArgument(new StringReader(line.substring(line.indexOf("lore=") + 5)));
                        for (int i = 0; i < beforeLore.size(); i++) {
                            var expected = decodeLegacy(ComponentSerialization.CODEC.parse(NbtOps.INSTANCE, beforeLore.get(i)).getOrThrow());
                            assertEquals(expected.getString(), stack.get(DataComponents.LORE).lines().get(i).getString(), entry.getName());
                        }
                    }
                }
            }
        } finally { net.minecraft.locale.Language.inject(original); }
        assertEquals("A blessing in love", Component.translatable("A blessing in love").getString());
    }

    @Test void ordinaryVanillaAndAlreadyModernNamesRemainUnchanged() {
        for (String command : List.of("give @s stone 1", "give @s stone[custom_name='ordinary name'] 1",
            "give @s stone[custom_name={translate:'item.minecraft.stone',italic:false},lore=[{text:'one'},{text:'two'}]] 1",
            "tellraw @s {\"text\":\"custom_name='{\\\"text\\\":\\\"hello\\\"}'\"}")) {
            assertEquals(command, LegacyItemText.migrateCommand(command));
        }
    }

    @Test void doubleEncodedNamesAndItemNamesBecomeComponents() throws Exception {
        String json = "{\"translate\":\"item.minecraft.stone\",\"italic\":false}";
        String command = "give @s stone[item_name=" + net.minecraft.nbt.StringTag.valueOf(new com.google.gson.Gson().toJson(json)) + "] 1";
        var item = new ItemParser(lookup).parse(new StringReader(LegacyItemText.migrateCommand(command).substring(8))).createItemStack(1);
        assertInstanceOf(TranslatableContents.class, item.get(DataComponents.ITEM_NAME).getContents());
        assertEquals("item.minecraft.stone", ((TranslatableContents)item.get(DataComponents.ITEM_NAME).getContents()).getKey());
    }

    @Test void legacyLiteralAndArrayComponentsPreserveStylesWithoutRewritingQuotedData() throws Exception {
        for (String json : List.of("\"ordinary literal\"", "[\"\",{\"translate\":\"item.minecraft.stone\",\"bold\":true}]")) {
            String command = "give @s stone[custom_name=" + net.minecraft.nbt.StringTag.valueOf(json) + "] 1";
            var stack = new ItemParser(lookup).parse(new StringReader(LegacyItemText.migrateCommand(command).substring(8))).createItemStack(1);
            assertEquals(ComponentSerialization.CODEC.parse(JsonOps.INSTANCE,JsonParser.parseString(json)).getOrThrow(), stack.get(DataComponents.CUSTOM_NAME));
        }
        String data = net.minecraft.nbt.StringTag.valueOf("notes,custom_name='{\"translate\":\"do not edit\"}'").toString();
        String command = "give @s stone[custom_data={value:" + data + "},custom_name='ordinary name'] 1";
        assertEquals(command, LegacyItemText.migrateCommand(command));
    }

    @Test void everyShippedAdvancementMessageRetainsHoverClickAndFrameMetadata() throws Exception {
        Set<String> identities = new TreeSet<>(); int legacy = 0, modern = 0, resolvedSearchTargets = 0;
        String search = "/advancementssearch highlight blazeandcave:nether/inception";
        assertEquals("blazeandcave:nether/inception", searchTarget(search));
        assertEquals("blazeandcave:nether/inception", searchTarget(search + " obtained_status"));
        for (String invalid : List.of("/advancementssearch highlight", search + " garbage", search + " obtained_status garbage", search + " obtained_status obtained_status", search + " ", "/advancementssearch highlight  ")) {
            assertThrows(IllegalArgumentException.class, () -> searchTarget(invalid), invalid);
        }
        var effective = PhaseBPackTestFixtures.currentLocalization();
        Set<String> targets = new HashSet<>();
        for (var id : effective.getAsJsonArray("advancementIds")) targets.add(id.getAsString());
        try (var files = Files.walk(Path.of("src/main/resources/resourcepacks"))) {
            for (var file : files.filter(p -> p.toString().endsWith(".mcfunction")).sorted().toList()) {
                var lines = Files.readAllLines(file);
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i); if (line.stripLeading().startsWith("#")) continue;
                    int tellraw = line.indexOf("tellraw "); if (tellraw < 0) continue;
                    int start = line.indexOf(' ', tellraw + 8) + 1;
                    var original = JsonParser.parseString(line.substring(start));
                    Set<String> beforeLinks = searchCommands(original);
                    if (beforeLinks.isEmpty()) continue;
                    String id = file.toString().replace('\\', '/') + ":" + (i + 1);
                    assertTrue(identities.add(id), id);
                    boolean old = containsField(original, "hoverEvent");
                    assertTrue(old || containsField(original, "hover_event"), id + " hover missing");
                    if (old) legacy++; else modern++;
                    String migrated = LegacyChatText.migrateCommand(line);
                    assertEquals(line.substring(0, start), migrated.substring(0, start), id + " selector/execute target");
                    var json = JsonParser.parseString(migrated.substring(start));
                    assertEquals(metadata(original), metadata(json), id + " component/style/frame semantics");
                    var component = ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
                    assertTrue(hasHover(component), id);
                    var encoded = ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, component).getOrThrow();
                    assertEquals(beforeLinks, searchCommands(encoded), id + " decoded click action");
                    assertEquals(translationKeys(original), translationKeys(encoded), id + " frame/title/description");
                    for (String command : beforeLinks) {
                        String target = searchTarget(command);
                        assertTrue(targets.contains(target), id + " unresolved Search " + target);
                        resolvedSearchTargets++;
                    }
                    assertEquals(component, ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow());
                    assertEquals(migrated, LegacyChatText.migrateCommand(migrated));
                }
            }
        }
        assertEquals(1162, legacy); assertEquals(138, modern); assertEquals(1300, identities.size());
        assertEquals(1300, resolvedSearchTargets);
        assertEquals("e62356a2a77b91253710b22a67747ff8d97c237a4235cf9265c796c8178e4c32", PhaseBPackTestFixtures.hash((String.join("\n", identities) + "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8), "SHA-256"));
        String positive = "execute as @a run tellraw @s {\"text\":\"x\",\"hoverEvent\":{\"action\":\"show_text\",\"contents\":{\"text\":\"hover\"}},\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/help\"}}";
        String migrated = LegacyChatText.migrateCommand(positive);
        assertNotEquals(positive, migrated); assertEquals(metadata(JsonParser.parseString(positive.substring(positive.indexOf('{')))), metadata(JsonParser.parseString(migrated.substring(migrated.indexOf('{')))));
        for (String negative : List.of("# " + positive, "say hoverEvent clickEvent", "tellraw @s invalid-json")) assertEquals(negative, LegacyChatText.migrateCommand(negative));
    }

    private static String searchTarget(String command) {
        var match = java.util.regex.Pattern.compile("/advancementssearch highlight ([a-z0-9_.-]+:[a-z0-9_./-]+)(?: obtained_status)?").matcher(command);
        if (!match.matches()) throw new IllegalArgumentException("Unsupported Search command: " + command);
        return match.group(1);
    }

    private static boolean containsField(com.google.gson.JsonElement e, String field) {
        if (e.isJsonObject()) return e.getAsJsonObject().has(field) || e.getAsJsonObject().entrySet().stream().anyMatch(v -> containsField(v.getValue(), field));
        if (e.isJsonArray()) return e.getAsJsonArray().asList().stream().anyMatch(v -> containsField(v, field));
        return false;
    }

    private static Set<String> searchCommands(com.google.gson.JsonElement e) {
        Set<String> result = new TreeSet<>();
        if (e.isJsonObject()) {
            var o = e.getAsJsonObject();
            for (String field : List.of("clickEvent", "click_event")) if (o.has(field)) {
                var event = o.getAsJsonObject(field);
                var value = event.has("command") ? event.get("command") : event.get("value");
                if (value != null && value.isJsonPrimitive() && value.getAsString().startsWith("/advancementssearch highlight ")) {
                    assertEquals("run_command", event.get("action").getAsString()); result.add(value.getAsString());
                }
            }
            for (var entry : o.entrySet()) result.addAll(searchCommands(entry.getValue()));
        } else if (e.isJsonArray()) for (var child : e.getAsJsonArray()) result.addAll(searchCommands(child));
        return result;
    }

    private static List<String> translationKeys(com.google.gson.JsonElement e) {
        List<String> result = new ArrayList<>();
        if (e.isJsonObject()) {
            var o = e.getAsJsonObject(); if (o.has("translate")) result.add(o.get("translate").getAsString());
            for (var entry : o.entrySet()) result.addAll(translationKeys(entry.getValue()));
        } else if (e.isJsonArray()) for (var child : e.getAsJsonArray()) result.addAll(translationKeys(child));
        Collections.sort(result); return result;
    }

    private static Map<String, String> metadata(com.google.gson.JsonElement e) {
        Map<String, String> result = new TreeMap<>(); metadata(e, "$", result); return result;
    }

    private static void metadata(com.google.gson.JsonElement e, String path, Map<String, String> result) {
        if (e.isJsonObject()) {
            var o = e.getAsJsonObject();
            for (var entry : o.entrySet()) {
                String field = entry.getKey();
                if (field.equals("hoverEvent") || field.equals("hover_event")) field = "hover";
                if (field.equals("clickEvent") || field.equals("click_event")) field = "click";
                if (path.endsWith("/hover") && (field.equals("value") || field.equals("contents"))) field = "content";
                if (path.endsWith("/click") && List.of("value", "command", "url", "page").contains(field)) field = "destination";
                metadata(entry.getValue(), path + "/" + field, result);
            }
        } else if (e.isJsonArray()) {
            for (int i = 0; i < e.getAsJsonArray().size(); i++) metadata(e.getAsJsonArray().get(i), path + "/" + i, result);
        } else assertNull(result.put(path, e.toString()), "Duplicate semantic metadata path " + path);
    }

    private static boolean hasHover(Component component) {
        if (component.getStyle().getHoverEvent() != null) return true;
        if (component.getContents() instanceof TranslatableContents contents) for (Object arg : contents.getArgs()) if (arg instanceof Component child && hasHover(child)) return true;
        return component.getSiblings().stream().anyMatch(Pre26SmokeRegressionTest::hasHover);
    }

    @Test void transientObjectiveAbsenceDoesNotEmitError() {
        var errors = new ArrayList<String>();
        var appender = new AbstractAppender("pre26-smoke", null, null, true, Property.EMPTY_ARRAY) {
            @Override public void append(LogEvent event) { if (event.getLevel().isMoreSpecificThan(org.apache.logging.log4j.Level.ERROR)) errors.add(event.getMessage().getFormattedMessage()); }
        };
        var logger = (org.apache.logging.log4j.core.Logger)LogManager.getLogger("AchieveToDo");
        appender.start(); logger.addAppender(appender);
        try {
            var server = new AchieveToDoServer(); var scoreboard = new ServerScoreboard(null);
            server.prepareScoreboard(scoreboard); server.prepareScoreboard(scoreboard);
            assertTrue(errors.isEmpty(), errors.toString());
            server.finishScoreboardInitialization(scoreboard);
            assertEquals(1, errors.size());
            server.prepareScoreboard(scoreboard); server.finishScoreboardInitialization(scoreboard);
            assertEquals(1, errors.size(), "one diagnostic per persistent missing state");
            assertTrue(server.isNotReady(), "missing objective remains fail-closed");
        }
        finally { logger.removeAppender(appender); appender.stop(); }
    }
}
