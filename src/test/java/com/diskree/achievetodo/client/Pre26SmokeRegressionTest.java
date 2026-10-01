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
        int count = 0;
        try (var files = Files.walk(Path.of("src/main/resources/resourcepacks"))) {
            for (var file : files.filter(p -> p.toString().endsWith(".mcfunction")).toList()) {
                String raw = Files.readString(file);
                if (!raw.contains("/advancementssearch highlight ") || !raw.contains("hoverEvent")) continue;
                for (String line : raw.lines().toList()) {
                    if (line.stripLeading().startsWith("#") || !line.contains("tellraw ") || !line.contains("hoverEvent")) continue;
                    String migrated = LegacyChatText.migrateCommand(line);
                    int at = migrated.indexOf("tellraw "); at = migrated.indexOf(' ', at + 8) + 1;
                    var json = JsonParser.parseString(migrated.substring(at));
                    var component = ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
                    assertTrue(hasHover(component), file.toString());
                    var roundtrip = ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, component).getOrThrow()).getOrThrow();
                    assertEquals(component, roundtrip);
                    assertEquals(migrated, LegacyChatText.migrateCommand(migrated));
                    count++;
                }
            }
        }
        assertEquals(1202, count);
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
