package com.diskree.achievetodo.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.diskree.achievetodo.server.Constants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HexFormat;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public final class ExternalPackCompatibility {

    private static final String MARKER_ENTRY = "achievetodo_compatibility/compat_26_2.properties";
    private static final String MARKER_VERSION = "compat_26_2_r19";
    private static final String NATIVE_BACAP_121_SHA1 = "14da3f07b5467e8b59ffc0253fd8212c938cd739";
    private static final String NATIVE_BACAP_HARDCORE_121_SHA1 = "9c20e14bbef224d2cc4ce63c8d24de1abc7a2475";
    private static final String ROOT_OVERRIDE_SHA1_PROPERTY = "rootOverrideSha1";
    private static final String LLAMA_CARPET_NBT_PROPERTY = "llamaCarpetNbtMapping";
    private static final String RAIDER_PREDICATE_KEYS_PROPERTY = "raiderPredicateKeys";
    private static final String[] CATEGORY_ROOTS = {
        "adventure",
        "animal",
        "biomes",
        "building",
        "challenges",
        "enchanting",
        "end",
        "farming",
        "mining",
        "monsters",
        "nether",
        "potion",
        "redstone",
        "statistics",
        "weaponry"
    };

    private static final Pattern HIDE_ADDITIONAL_TOOLTIP = Pattern.compile("(^|,)(?:minecraft:)?hide_additional_tooltip=\\{\\}(?=,|]|$)");
    private static final Pattern DYED_COLOR_WITH_TOOLTIP = Pattern.compile("dyed_color=\\{rgb:(\\d+),show_in_tooltip:(?:true|false|1b|0b)\\}");
    private static final Pattern ENCHANTMENTS_WITH_TOOLTIP = Pattern.compile("enchantments=\\{levels:(\\{[^\\r\\n]+?\\}),show_in_tooltip:(?:true|false|1b|0b)\\}");
    private static final Pattern STORED_ENCHANTMENTS_WITH_TOOLTIP = Pattern.compile("stored_enchantments=\\{levels:(\\{[^\\r\\n]+?\\}),show_in_tooltip:(?:true|false|1b|0b)\\}");
    private static final Pattern ENCHANTMENTS_LEVELS = Pattern.compile("enchantments=\\{levels:(\\{[^\\r\\n]+?\\})\\}");
    private static final Pattern STORED_ENCHANTMENTS_LEVELS = Pattern.compile("stored_enchantments=\\{levels:(\\{[^\\r\\n]+?\\})\\}");
    private static final Pattern UNBREAKABLE_WITH_TOOLTIP = Pattern.compile("unbreakable=\\{show_in_tooltip:(?:true|false|1b|0b)\\}");
    private static final Pattern TRIM_WITH_TOOLTIP = Pattern.compile("trim=\\{([^\\r\\n]+?),show_in_tooltip:(?:true|false|1b|0b)\\}");
    private static final String LEGACY_GAMERULE_REMOVED = "# achievetodo compatibility: removed unsupported gamerule command";
    private static final String LEGACY_DAYTIME_QUERY = "time query daytime";
    private static final String MODERN_DAY_QUERY = "time of minecraft:overworld query minecraft:day";

    private static final String ENTITY_PROPERTIES_CONDITION = "minecraft:entity_properties";
    private static final String LEGACY_TYPE = "type";
    private static final String LEGACY_TYPE_SPECIFIC = "type_specific";
    private static final String LEGACY_LOOKING_AT = "looking_at";
    private static final String LEGACY_GAMEMODE = "gamemode";
    private static final String LEGACY_HAS_RAID = "has_raid";
    private static final String LEGACY_IS_CAPTAIN = "is_captain";
    private static final String LEGACY_TAG = "tag";
    private static final String LEGACY_ENTITY_STRUCK = "entity_struck";
    private ExternalPackCompatibility() {
    }

    public static @NotNull WorldPackSyncResult ensureWorldPacksUpToDate(
        @NotNull Path globalPacksDirectory,
        @NotNull Path worldPacksDirectory,
        boolean compatibilityRequired
    ) {
        if (Files.notExists(worldPacksDirectory)) {
            return compatibilityRequired ? WorldPackSyncResult.INTEGRITY_CHECK_FAILED : WorldPackSyncResult.NO_KNOWN_PACKS;
        }

        List<ExternalPack> externalPacksToCheck = findRequiredExternalPacks(worldPacksDirectory);
        if (externalPacksToCheck.isEmpty()) {
            return compatibilityRequired ? WorldPackSyncResult.MISSING_REQUIRED_PACK : WorldPackSyncResult.NO_KNOWN_PACKS;
        }

        try {
            Files.createDirectories(globalPacksDirectory);
        } catch (IOException e) {
            return WorldPackSyncResult.UNKNOWN_ERROR;
        }

        boolean copiedAnyPack = false;
        for (ExternalPack externalPack : externalPacksToCheck) {
            Path worldPack = worldPacksDirectory.resolve(externalPack.getFileName());
            if (isCurrentWorldPack(worldPack, externalPack)) {
                continue;
            }

            Path globalPack = globalPacksDirectory.resolve(externalPack.getFileName());
            if (isPinnedHistoricalSource(globalPack, externalPack)) {
                try {
                    copyForWorld(globalPack, worldPack, externalPack);
                    copiedAnyPack = true;
                    continue;
                } catch (IOException e) {
                    return WorldPackSyncResult.UNKNOWN_ERROR;
                }
            }
            return WorldPackSyncResult.MISSING_REQUIRED_PACK;
        }
        return copiedAnyPack ? WorldPackSyncResult.UPDATED : WorldPackSyncResult.ALREADY_CURRENT;
    }

    public static boolean requiresHistoricalCompatibility(@NotNull Path levelDataFile) {
        if (Files.notExists(levelDataFile)) {
            return false;
        }
        try {
            CompoundTag levelData = NbtIo.readCompressed(levelDataFile, NbtAccounter.unlimitedHeap());
            if (levelData == null) {
                return false;
            }
            CompoundTag data = levelData.getCompoundOrEmpty("Data");
            String configName = data.getStringOr(Constants.NbtKey.LEVEL_CONFIG_NAME, "");
            return !configName.isEmpty();
        } catch (IOException e) {
            return false;
        }
    }

    public static @NotNull List<ExternalPack> findRequiredExternalPacks(@NotNull Path worldPacksDirectory) {
        List<String> worldPackFileNames;
        try (var stream = Files.list(worldPacksDirectory)) {
            worldPackFileNames = stream
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".zip"))
                .map(path -> path.getFileName().toString())
                .toList();
        } catch (IOException e) {
            return List.of();
        }
        return worldPackFileNames.stream()
            .map(ExternalPack::mapFromFileName)
            .filter(pack -> pack != null)
            .sorted((left, right) -> Integer.compare(left.ordinal(), right.ordinal()))
            .toList();
    }

    public static boolean isPinnedHistoricalSource(@NotNull Path pack, @NotNull ExternalPack externalPack) {
        return externalPack.getSha1().equalsIgnoreCase(Utils.calculateSHA1(pack));
    }

    public static boolean isCurrentWorldPack(@NotNull Path pack, @NotNull ExternalPack externalPack) {
        return isPinnedHistoricalSource(pack, externalPack) || isCompatibleWorldCopy(pack, externalPack);
    }

    public static void copyForWorld(@NotNull Path sourcePack, @NotNull Path targetPack, @NotNull ExternalPack externalPack) throws IOException {
        String actualSourceSha1 = Utils.calculateSHA1(sourcePack);
        boolean nativeSource = isNativeBacapSource(actualSourceSha1);
        Path parent = targetPack.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path tempPack = Files.createTempFile(parent, "achievetodo-pack-", ".zip");
        boolean changed = false;
        try (ZipFile zipFile = new ZipFile(sourcePack.toFile());
             ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(tempPack))) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry sourceEntry = entries.nextElement();
                byte[] bytes = readEntryBytes(zipFile, sourceEntry);
                if (!nativeSource && sourceEntry.getName().endsWith(".mcfunction")) {
                    ConversionResult conversion = convertFunction(new String(bytes, StandardCharsets.UTF_8));
                    bytes = conversion.text().getBytes(StandardCharsets.UTF_8);
                    changed |= conversion.changed();
                } else if (sourceEntry.getName().endsWith(".json")) {
                    String json = new String(bytes, StandardCharsets.UTF_8);
                    ConversionResult conversion = nativeSource ? convertNativeJson(json) : convertJson(json);
                    bytes = conversion.text().getBytes(StandardCharsets.UTF_8);
                    changed |= conversion.changed();
                }
                writeEntry(out, sourceEntry.getName(), bytes);
            }
            if (changed) {
                writeCompatibilityMarker(out, externalPack, actualSourceSha1);
            }
        } catch (IOException e) {
            Files.deleteIfExists(tempPack);
            throw e;
        }

        if (changed) {
            Files.move(tempPack, targetPack, StandardCopyOption.REPLACE_EXISTING);
        } else {
            Files.deleteIfExists(tempPack);
            Files.copy(sourcePack, targetPack, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static boolean isNativeBacapSource(@NotNull String sourceSha1) {
        return NATIVE_BACAP_121_SHA1.equalsIgnoreCase(sourceSha1)
            || NATIVE_BACAP_HARDCORE_121_SHA1.equalsIgnoreCase(sourceSha1);
    }

    public static boolean isCompatibleWorldCopy(@NotNull Path pack, @NotNull ExternalPack externalPack) {
        if (Files.notExists(pack)) {
            return false;
        }
        try (ZipFile zipFile = new ZipFile(pack.toFile())) {
            ZipEntry markerEntry = zipFile.getEntry(MARKER_ENTRY);
            if (markerEntry == null) {
                return false;
            }
            Properties properties = new Properties();
            try (InputStream input = zipFile.getInputStream(markerEntry)) {
                properties.load(input);
            }
            return MARKER_VERSION.equals(properties.getProperty("version"))
                && externalPack.getFileName().equals(properties.getProperty("fileName"))
                && externalPack.getSha1().equals(properties.getProperty("sourceSha1"))
                && currentRootOverrideSha1().equals(properties.getProperty(ROOT_OVERRIDE_SHA1_PROPERTY))
                && (externalPack != ExternalPack.BACAP
                    || ("equipment.body".equals(properties.getProperty(LLAMA_CARPET_NBT_PROPERTY))
                        && "snake_case".equals(properties.getProperty(RAIDER_PREDICATE_KEYS_PROPERTY))));
        } catch (IOException e) {
            return false;
        }
    }

    static @NotNull String currentRootOverrideSha1() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            for (String category : CATEGORY_ROOTS) {
                String resourcePath = "/resourcepacks/bacap_override/data/bacap_rewards/function/" + category + "/root.mcfunction";
                try (InputStream input = ExternalPackCompatibility.class.getResourceAsStream(resourcePath)) {
                    if (input == null) {
                        throw new IllegalStateException("Missing compatibility resource: " + resourcePath);
                    }
                    digest.update(resourcePath.getBytes(StandardCharsets.UTF_8));
                    digest.update((byte) 0);
                    digest.update(input.readAllBytes());
                    digest.update((byte) '\n');
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read bundled category root overrides", e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Missing SHA-1 support", e);
        }
    }

    private static @NotNull ConversionResult convertFunction(@NotNull String function) {
        String[] lines = function.split("\\R", -1);
        boolean changed = false;
        for (int i = 0; i < lines.length; i++) {
            String originalLine = lines[i];
            String convertedLine = originalLine;

            java.util.regex.Matcher hideAdditionalTooltipMatcher = HIDE_ADDITIONAL_TOOLTIP.matcher(convertedLine);
            if (hideAdditionalTooltipMatcher.find()) {
                convertedLine = hideAdditionalTooltipMatcher.replaceAll("");
                convertedLine = convertedLine
                    .replace("[,", "[")
                    .replace(",]", "]")
                    .replace(",,", ",");
            }

            convertedLine = DYED_COLOR_WITH_TOOLTIP.matcher(convertedLine).replaceAll("dyed_color=$1");
            convertedLine = ENCHANTMENTS_WITH_TOOLTIP.matcher(convertedLine).replaceAll("enchantments=$1");
            convertedLine = STORED_ENCHANTMENTS_WITH_TOOLTIP.matcher(convertedLine).replaceAll("stored_enchantments=$1");
            convertedLine = ENCHANTMENTS_LEVELS.matcher(convertedLine).replaceAll("enchantments=$1");
            convertedLine = STORED_ENCHANTMENTS_LEVELS.matcher(convertedLine).replaceAll("stored_enchantments=$1");
            convertedLine = UNBREAKABLE_WITH_TOOLTIP.matcher(convertedLine).replaceAll("unbreakable={}");
            convertedLine = TRIM_WITH_TOOLTIP.matcher(convertedLine).replaceAll("trim={$1}");
            convertedLine = convertedLine.replace("minecraft:chain", "minecraft:iron_chain");
            convertedLine = convertedLine.replace(LEGACY_DAYTIME_QUERY, MODERN_DAY_QUERY);
            convertedLine = rewriteLegacyGameruleLine(convertedLine);
            convertedLine = LegacyItemText.migrateCommand(convertedLine);

            if (!convertedLine.equals(originalLine)) {
                lines[i] = convertedLine;
                changed = true;
            }
        }
        if (!changed) {
            return new ConversionResult(function, false);
        }
        return new ConversionResult(String.join("\n", lines), true);
    }

    private static @NotNull String rewriteLegacyGameruleLine(@NotNull String line) {
        String trimmed = line.trim();
        if (trimmed.equals("gamerule commandBlockOutput false")) {
            return LEGACY_GAMERULE_REMOVED;
        }
        if (trimmed.startsWith("execute in minecraft:overworld run gamerule maxCommandForkCount ")
            || trimmed.startsWith("execute in minecraft:the_nether run gamerule maxCommandForkCount ")
            || trimmed.startsWith("execute in minecraft:the_end run gamerule maxCommandForkCount ")
            || trimmed.startsWith("execute in minecraft:overworld run gamerule maxCommandChainLength ")
            || trimmed.startsWith("execute in minecraft:the_nether run gamerule maxCommandChainLength ")
            || trimmed.startsWith("execute in minecraft:the_end run gamerule maxCommandChainLength ")
            || trimmed.startsWith("execute in the_end run gamerule announceAdvancements ")
            || trimmed.startsWith("execute in overworld run gamerule announceAdvancements ")
            || trimmed.startsWith("execute in the_nether run gamerule announceAdvancements ")
            || trimmed.startsWith("execute unless score task bac_settings matches 0 run execute in the_end run gamerule announceAdvancements ")
            || trimmed.startsWith("execute unless score task bac_settings matches 0 run execute in overworld run gamerule announceAdvancements ")
            || trimmed.startsWith("execute unless score task bac_settings matches 0 run execute in the_nether run gamerule announceAdvancements ")
            || trimmed.startsWith("execute unless score goal bac_settings matches 0 run execute in the_end run gamerule announceAdvancements ")
            || trimmed.startsWith("execute unless score goal bac_settings matches 0 run execute in overworld run gamerule announceAdvancements ")
            || trimmed.startsWith("execute unless score goal bac_settings matches 0 run execute in the_nether run gamerule announceAdvancements ")
            || trimmed.startsWith("execute unless score challenge bac_settings matches 0 run execute in the_end run gamerule announceAdvancements ")
            || trimmed.startsWith("execute unless score challenge bac_settings matches 0 run execute in overworld run gamerule announceAdvancements ")
            || trimmed.startsWith("execute unless score challenge bac_settings matches 0 run execute in the_nether run gamerule announceAdvancements ")
            || trimmed.startsWith("execute unless score super_challenge bac_settings matches 0 run execute in the_end run gamerule announceAdvancements ")
            || trimmed.startsWith("execute unless score super_challenge bac_settings matches 0 run execute in overworld run gamerule announceAdvancements ")
            || trimmed.startsWith("execute unless score super_challenge bac_settings matches 0 run execute in the_nether run gamerule announceAdvancements ")
            || trimmed.startsWith("execute unless score milestone bac_settings matches 0 run execute in the_end run gamerule announceAdvancements ")
            || trimmed.startsWith("execute unless score milestone bac_settings matches 0 run execute in overworld run gamerule announceAdvancements ")
            || trimmed.startsWith("execute unless score milestone bac_settings matches 0 run execute in the_nether run gamerule announceAdvancements ")
        ) {
            return LEGACY_GAMERULE_REMOVED;
        }
        return line;
    }

    private static @NotNull ConversionResult convertJson(@NotNull String json) {
        JsonElement root;
        try {
            root = JsonParser.parseString(json);
        } catch (JsonParseException ignored) {
            return new ConversionResult(json, false);
        }

        TransformResult result = transformElement(root, Context.NORMAL);
        result = rewriteChainIds(result);
        if (!result.changed()) {
            return new ConversionResult(json, false);
        }
        return new ConversionResult(result.element().toString(), true);
    }

    private static @NotNull ConversionResult convertNativeJson(@NotNull String json) {
        JsonElement root;
        try {
            root = JsonParser.parseString(json);
        } catch (JsonParseException ignored) {
            return new ConversionResult(json, false);
        }
        if (!suppressNativeAnnouncements(root)) {
            return new ConversionResult(json, false);
        }
        return new ConversionResult(root.toString(), true);
    }

    private static boolean suppressNativeAnnouncements(@NotNull JsonElement element) {
        boolean changed = false;
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            changed = suppressVanillaAnnouncementForBacapRewards(object);
            for (var entry : object.entrySet()) {
                changed |= suppressNativeAnnouncements(entry.getValue());
            }
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                changed |= suppressNativeAnnouncements(child);
            }
        }
        return changed;
    }

    private static @NotNull TransformResult rewriteChainIds(@NotNull TransformResult result) {
        JsonElement rewritten = rewriteChainIds(result.element());
        return rewritten == result.element()
            ? result
            : new TransformResult(rewritten, true);
    }

    private static @NotNull JsonElement rewriteChainIds(@NotNull JsonElement element) {
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            boolean changed = false;
            JsonObject transformed = new JsonObject();
            for (var entry : object.entrySet()) {
                JsonElement value = entry.getValue();
                JsonElement rewrittenValue = rewriteChainIds(value);
                if (rewrittenValue != value) {
                    changed = true;
                }
                if (rewrittenValue.isJsonPrimitive()
                    && rewrittenValue.getAsJsonPrimitive().isString()
                    && "minecraft:chain".equals(rewrittenValue.getAsString())
                ) {
                    rewrittenValue = JsonParser.parseString("\"minecraft:iron_chain\"");
                    changed = true;
                }
                transformed.add(entry.getKey(), rewrittenValue);
            }
            return changed ? transformed : element;
        }
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            boolean changed = false;
            JsonArray transformed = new JsonArray();
            for (JsonElement child : array) {
                JsonElement rewrittenChild = rewriteChainIds(child);
                transformed.add(rewrittenChild);
                changed |= rewrittenChild != child;
            }
            return changed ? transformed : element;
        }
        if (element.isJsonPrimitive()
            && element.getAsJsonPrimitive().isString()
            && "minecraft:chain".equals(element.getAsString())
        ) {
            return JsonParser.parseString("\"minecraft:iron_chain\"");
        }
        return element;
    }

    private static @NotNull TransformResult transformElement(@NotNull JsonElement element, @NotNull Context context) {
        if (element.isJsonObject()) {
            return transformObject(element.getAsJsonObject(), context);
        }
        if (element.isJsonArray()) {
            boolean changed = false;
            JsonArray transformed = new JsonArray();
            for (JsonElement child : element.getAsJsonArray()) {
                TransformResult childResult = transformElement(child, context);
                transformed.add(childResult.element());
                changed |= childResult.changed();
            }
            return changed ? new TransformResult(transformed, true) : new TransformResult(element, false);
        }
        return new TransformResult(element, false);
    }

    private static @NotNull TransformResult transformObject(@NotNull JsonObject object, @NotNull Context context) {
        return switch (context) {
            case NORMAL -> transformNormalObject(object);
            case CONDITIONS -> transformConditionsObject(object);
            case DAMAGE_PREDICATE -> transformDamagePredicateObject(object);
            case KILLING_BLOW_PREDICATE -> transformKillingBlowPredicateObject(object);
            case ENTITY_PREDICATE -> transformEntityPredicateObject(object);
            case PLAYER_TYPE_SPECIFIC -> transformPlayerTypeSpecificObject(object);
            case RAIDER_TYPE_SPECIFIC -> transformRaiderTypeSpecificObject(object);
            case GENERIC_TYPE_SPECIFIC -> transformGenericTypeSpecificObject(object);
            case LIGHTNING_TYPE_SPECIFIC -> transformLightningTypeSpecificObject(object);
        };
    }

    private static @NotNull TransformResult transformNormalObject(@NotNull JsonObject object) {
        boolean changed = false;
        JsonObject transformed = new JsonObject();
        String condition = getString(object, "condition");

        for (var entry : object.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();

            Context childContext = Context.NORMAL;
            if ("background".equals(key)) {
                JsonElement normalizedBackground = normalizeAdvancementBackground(value);
                transformed.add(key, normalizedBackground);
                changed |= normalizedBackground != value;
                continue;
            }

            if ("conditions".equals(key) && value.isJsonObject()) {
                childContext = Context.CONDITIONS;
            } else if ("predicate".equals(key) && ENTITY_PROPERTIES_CONDITION.equals(condition) && value.isJsonObject()) {
                childContext = Context.ENTITY_PREDICATE;
            } else if ("predicates".equals(key) && value.isJsonObject() && looksLikeItemPredicate(object)) {
                TransformResult childResult = transformItemPredicatesObject(value.getAsJsonObject());
                transformed.add(key, childResult.element());
                changed |= childResult.changed();
                continue;
            } else if (value.isJsonObject()
                && isDirectEntityPredicateKey(key)
                && looksLikeLegacyEntityPredicate(value.getAsJsonObject())) {
                childContext = Context.ENTITY_PREDICATE;
            }

            TransformResult childResult = transformElement(value, childContext);
            transformed.add(key, childResult.element());
            changed |= childResult.changed();
        }

        if (suppressVanillaAnnouncementForBacapRewards(transformed)) {
            changed = true;
        }

        return changed ? new TransformResult(transformed, true) : new TransformResult(object, false);
    }

    private static @NotNull TransformResult transformItemPredicatesObject(@NotNull JsonObject object) {
        boolean changed = false;
        JsonObject transformed = new JsonObject();

        for (var entry : object.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();

            if (("enchantments".equals(key) || "stored_enchantments".equals(key)) && value.isJsonArray()) {
                TransformResult childResult = transformItemEnchantmentPredicateArray(value.getAsJsonArray());
                transformed.add(key, childResult.element());
                changed |= childResult.changed();
                continue;
            }

            TransformResult childResult = transformElement(value, Context.NORMAL);
            transformed.add(key, childResult.element());
            changed |= childResult.changed();
        }

        return changed ? new TransformResult(transformed, true) : new TransformResult(object, false);
    }

    private static @NotNull TransformResult transformItemEnchantmentPredicateArray(@NotNull JsonArray array) {
        boolean changed = false;
        JsonArray transformed = new JsonArray();

        for (JsonElement child : array) {
            if (!child.isJsonObject()) {
                TransformResult childResult = transformElement(child, Context.NORMAL);
                transformed.add(childResult.element());
                changed |= childResult.changed();
                continue;
            }

            JsonObject predicate = child.getAsJsonObject();
            JsonObject transformedPredicate = new JsonObject();
            boolean predicateChanged = false;
            for (var entry : predicate.entrySet()) {
                String key = entry.getKey();
                JsonElement value = entry.getValue();
                if ("enchantments".equals(key) && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                    JsonArray singletonArray = new JsonArray();
                    singletonArray.add(value.getAsString());
                    transformedPredicate.add(key, singletonArray);
                    predicateChanged = true;
                    continue;
                }

                TransformResult childResult = transformElement(value, Context.NORMAL);
                transformedPredicate.add(key, childResult.element());
                predicateChanged |= childResult.changed();
            }
            transformed.add(predicateChanged ? transformedPredicate : predicate);
            changed |= predicateChanged;
        }

        return changed ? new TransformResult(transformed, true) : new TransformResult(array, false);
    }

    private static boolean suppressVanillaAnnouncementForBacapRewards(@NotNull JsonObject object) {
        JsonElement rewardsElement = object.get("rewards");
        JsonElement displayElement = object.get("display");
        if (rewardsElement == null || displayElement == null || !rewardsElement.isJsonObject() || !displayElement.isJsonObject()) {
            return false;
        }
        String rewardFunction = getString(rewardsElement.getAsJsonObject(), "function");
        if (rewardFunction == null || !rewardFunction.startsWith("bacap_rewards:")) {
            return false;
        }
        JsonObject display = displayElement.getAsJsonObject();
        JsonElement announceElement = display.get("announce_to_chat");
        if (announceElement != null && announceElement.isJsonPrimitive() && announceElement.getAsJsonPrimitive().isBoolean() && !announceElement.getAsBoolean()) {
            return false;
        }
        display.addProperty("announce_to_chat", false);
        return true;
    }

    private static @NotNull TransformResult transformConditionsObject(@NotNull JsonObject object) {
        boolean changed = false;
        JsonObject transformed = new JsonObject();

        for (var entry : object.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            Context childContext = Context.NORMAL;

            if (isContextAwarePredicateKey(key) && looksLikeContextAwarePredicateValue(value)) {
                TransformResult childResult = transformContextAwarePredicateValue(value);
                transformed.add(key, childResult.element());
                changed |= childResult.changed();
                continue;
            }

            if ("damage".equals(key) && value.isJsonObject()) {
                childContext = Context.DAMAGE_PREDICATE;
            } else if ("killing_blow".equals(key) && value.isJsonObject()) {
                childContext = Context.KILLING_BLOW_PREDICATE;
            } else if (isDirectEntityPredicateKey(key) && value.isJsonObject()
                && hasLegacyPlayerAdvancementsPredicate(value.getAsJsonObject())) {
                childContext = Context.ENTITY_PREDICATE;
            } else if (value.isJsonObject() && looksLikeLegacyEntityPredicate(value.getAsJsonObject())) {
                childContext = Context.ENTITY_PREDICATE;
            }

            TransformResult childResult = transformElement(value, childContext);
            if (value.isJsonArray() && isEntityPredicateArrayKey(key)) {
                TransformResult arrayResult = transformPotentialEntityPredicateArray(childResult.element());
                childResult = new TransformResult(
                    arrayResult.element(),
                    childResult.changed() || arrayResult.changed()
                );
            }
            transformed.add(key, childResult.element());
            changed |= childResult.changed();
        }

        return changed ? new TransformResult(transformed, true) : new TransformResult(object, false);
    }

    private static @NotNull TransformResult transformContextAwarePredicateValue(@NotNull JsonElement element) {
        if (element.isJsonArray()) {
            boolean changed = false;
            JsonArray transformed = new JsonArray();
            for (JsonElement child : element.getAsJsonArray()) {
                TransformResult childResult = transformContextAwarePredicateCondition(child);
                transformed.add(childResult.element());
                changed |= childResult.changed();
            }
            return changed ? new TransformResult(transformed, true) : new TransformResult(element, false);
        }
        if (element.isJsonObject()) {
            TransformResult childResult = transformContextAwarePredicateCondition(element);
            JsonArray wrapped = new JsonArray();
            wrapped.add(childResult.element());
            return new TransformResult(wrapped, true);
        }
        return new TransformResult(element, false);
    }

    private static @NotNull TransformResult transformContextAwarePredicateCondition(@NotNull JsonElement element) {
        if (!element.isJsonObject()) {
            return transformElement(element, Context.NORMAL);
        }
        JsonObject object = element.getAsJsonObject();
        if (!looksLikeLootCondition(object)) {
            return transformElement(element, Context.NORMAL);
        }
        return transformNormalObject(object);
    }

    private static @NotNull TransformResult transformDamagePredicateObject(@NotNull JsonObject object) {
        boolean changed = false;
        JsonObject transformed = new JsonObject();

        for (var entry : object.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();

            if (LEGACY_TYPE.equals(key) && value.isJsonObject()) {
                TransformResult nestedType = transformLegacyDamageTypeObject(value.getAsJsonObject());
                transformed.add(key, nestedType.element());
                changed = true;
                changed |= nestedType.changed();
                continue;
            }

            Context childContext = Context.NORMAL;
            if (value.isJsonObject() && isDirectEntityPredicateKey(key) && looksLikeLegacyEntityPredicate(value.getAsJsonObject())) {
                childContext = Context.ENTITY_PREDICATE;
            }

            TransformResult childResult = transformElement(value, childContext);
            transformed.add(key, childResult.element());
            changed |= childResult.changed();
        }

        return changed ? new TransformResult(transformed, true) : new TransformResult(object, false);
    }

    private static @NotNull TransformResult transformKillingBlowPredicateObject(@NotNull JsonObject object) {
        boolean changed = false;
        JsonObject transformed = new JsonObject();

        for (var entry : object.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();

            if (LEGACY_TYPE.equals(key) && value.isJsonObject()) {
                TransformResult flattenedType = transformLegacyDamageTypeObject(value.getAsJsonObject());
                JsonObject flattenedObject = flattenedType.element().getAsJsonObject();
                for (var flattenedEntry : flattenedObject.entrySet()) {
                    transformed.add(flattenedEntry.getKey(), flattenedEntry.getValue());
                }
                changed = true;
                changed |= flattenedType.changed();
                continue;
            }

            Context childContext = Context.NORMAL;
            if (value.isJsonObject() && isDirectEntityPredicateKey(key) && looksLikeLegacyEntityPredicate(value.getAsJsonObject())) {
                childContext = Context.ENTITY_PREDICATE;
            }

            TransformResult childResult = transformElement(value, childContext);
            transformed.add(key, childResult.element());
            changed |= childResult.changed();
        }

        return changed ? new TransformResult(transformed, true) : new TransformResult(object, false);
    }

    private static @NotNull TransformResult transformLegacyDamageTypeObject(@NotNull JsonObject object) {
        boolean changed = false;
        JsonObject transformed = new JsonObject();

        for (var entry : object.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            Context childContext = Context.NORMAL;

            if (value.isJsonObject() && isDirectEntityPredicateKey(key) && looksLikeLegacyEntityPredicate(value.getAsJsonObject())) {
                childContext = Context.ENTITY_PREDICATE;
            }

            TransformResult childResult = transformElement(value, childContext);
            transformed.add(key, childResult.element());
            changed |= childResult.changed();
        }

        return changed ? new TransformResult(transformed, true) : new TransformResult(object, false);
    }

    private static @NotNull TransformResult transformEntityPredicateObject(@NotNull JsonObject object) {
        boolean changed = false;
        JsonObject transformed = new JsonObject();

        for (var entry : object.entrySet()) {
            String originalKey = entry.getKey();
            JsonElement value = entry.getValue();
            String targetKey = originalKey;
            Context childContext = Context.NORMAL;

            // Exact frozen llama-carpet shape only; arbitrary SNBT remains unchanged.
            if ("nbt".equals(originalKey) && "#blazeandcave:llamas".equals(getString(object, LEGACY_TYPE))) {
                JsonElement mapped = normalizeFrozenLlamaCarpetNbt(value);
                changed |= mapped != value;
                value = mapped;
            }

            if (LEGACY_TYPE.equals(originalKey)) {
                targetKey = "entity_type";
                value = normalizeEntityType(value);
                changed = true;
            } else if ("player".equals(originalKey) && hasLegacyPlayerAdvancementsPredicate(object)) {
                targetKey = "type_specific/player";
                childContext = Context.PLAYER_TYPE_SPECIFIC;
                changed = true;
            } else if (LEGACY_TYPE_SPECIFIC.equals(originalKey) && value.isJsonObject()) {
                JsonObject typeSpecific = value.getAsJsonObject();
                String legacyTypeSpecificType = getString(typeSpecific, LEGACY_TYPE);
                if (legacyTypeSpecificType != null) {
                    String componentId = getLegacyTypeSpecificComponentId(legacyTypeSpecificType);
                    if (componentId != null) {
                        TransformResult componentValue = transformLegacyTypeSpecificComponents(typeSpecific, componentId);
                        mergeIntoNamedObject(transformed, "components", componentValue.element().getAsJsonObject());
                        changed = true;
                        changed |= componentValue.changed();
                        continue;
                    }
                    targetKey = getLegacyTypeSpecificTargetKey(legacyTypeSpecificType);
                    childContext = getLegacyTypeSpecificContext(legacyTypeSpecificType);
                    changed = true;
                }
            } else if (LEGACY_ENTITY_STRUCK.equals(originalKey)) {
                TransformResult entityStruck = transformLegacyEntityStruckValue(value);
                mergeIntoNamedObject(transformed, "type_specific/lightning", entityStruck.element().getAsJsonObject());
                changed = true;
                changed |= entityStruck.changed();
                continue;
            } else {
                String remappedKey = remapLegacyEntityPredicateKey(originalKey);
                if (remappedKey != null) {
                    targetKey = remappedKey;
                    value = normalizeEntityPredicateValue(originalKey, value);
                    changed = true;
                }
                if (isNestedEntityPredicateKey(targetKey) && value.isJsonObject()) {
                    childContext = Context.ENTITY_PREDICATE;
                }
            }

            TransformResult childResult = transformElement(value, childContext);
            transformed.add(targetKey, childResult.element());
            changed |= childResult.changed();
        }

        return changed ? new TransformResult(transformed, true) : new TransformResult(object, false);
    }

    private static @NotNull JsonElement normalizeFrozenLlamaCarpetNbt(@NotNull JsonElement value) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            return value;
        }
        String nbt = value.getAsString();
        for (String color : List.of("white", "orange", "magenta", "light_blue", "yellow", "lime",
            "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black")) {
            String item = "minecraft:" + color + "_carpet";
            if (("{body_armor_item:{id:\"" + item + "\"}}").equals(nbt)) {
                return new com.google.gson.JsonPrimitive("{equipment:{body:{id:\"" + item + "\"}}}");
            }
        }
        return value;
    }

    private static @NotNull TransformResult transformPotentialEntityPredicateArray(@NotNull JsonElement element) {
        if (!element.isJsonArray()) {
            return new TransformResult(element, false);
        }
        boolean changed = false;
        JsonArray transformed = new JsonArray();
        for (JsonElement child : element.getAsJsonArray()) {
            TransformResult childResult;
            if (child.isJsonObject() && looksLikeLegacyEntityPredicate(child.getAsJsonObject())) {
                childResult = transformElement(child, Context.ENTITY_PREDICATE);
            } else {
                childResult = transformElement(child, Context.NORMAL);
            }
            transformed.add(childResult.element());
            changed |= childResult.changed();
        }
        return changed ? new TransformResult(transformed, true) : new TransformResult(element, false);
    }

    private static @NotNull TransformResult transformPlayerTypeSpecificObject(@NotNull JsonObject object) {
        boolean changed = false;
        JsonObject transformed = new JsonObject();

        for (var entry : object.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            String targetKey = key;
            Context childContext = Context.NORMAL;

            if (LEGACY_LOOKING_AT.equals(key)) {
                targetKey = "lookingAt";
                childContext = Context.ENTITY_PREDICATE;
                changed = true;
            } else if ("lookingAt".equals(key)) {
                childContext = Context.ENTITY_PREDICATE;
            }

            TransformResult childResult = transformElement(value, childContext);
            transformed.add(targetKey, childResult.element());
            changed |= childResult.changed();
        }

        return changed ? new TransformResult(transformed, true) : new TransformResult(object, false);
    }

    private static @NotNull TransformResult transformRaiderTypeSpecificObject(@NotNull JsonObject object) {
        boolean changed = false;
        JsonObject transformed = new JsonObject();

        for (var entry : object.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            String targetKey = key;

            // RaiderPredicate.CODEC reads snake_case; preserve explicit and omitted booleans.

            TransformResult childResult = transformElement(value, Context.NORMAL);
            transformed.add(targetKey, childResult.element());
            changed |= childResult.changed();
        }

        return changed ? new TransformResult(transformed, true) : new TransformResult(object, false);
    }

    private static @NotNull TransformResult transformGenericTypeSpecificObject(@NotNull JsonObject object) {
        boolean changed = false;
        JsonObject transformed = new JsonObject();

        for (var entry : object.entrySet()) {
            String key = entry.getKey();
            if (LEGACY_TYPE.equals(key)) {
                changed = true;
                continue;
            }

            TransformResult childResult = transformElement(entry.getValue(), Context.NORMAL);
            transformed.add(key, childResult.element());
            changed |= childResult.changed();
        }

        return changed ? new TransformResult(transformed, true) : new TransformResult(object, false);
    }

    private static @NotNull TransformResult transformLightningTypeSpecificObject(@NotNull JsonObject object) {
        boolean changed = false;
        JsonObject transformed = new JsonObject();

        for (var entry : object.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            if (LEGACY_TYPE.equals(key)) {
                changed = true;
                continue;
            }
            if (LEGACY_ENTITY_STRUCK.equals(key)) {
                JsonObject entityPredicate = collapseLegacyEntityPredicateConditions(value);
                if (entityPredicate != null) {
                    transformed.add(key, entityPredicate);
                    changed = true;
                    continue;
                }
            }

            TransformResult childResult = transformElement(value, Context.NORMAL);
            transformed.add(key, childResult.element());
            changed |= childResult.changed();
        }

        return changed ? new TransformResult(transformed, true) : new TransformResult(object, false);
    }

    private static boolean hasLegacyPlayerAdvancementsPredicate(@NotNull JsonObject object) {
        if (object.has("type_specific/player") || object.has("minecraft:type_specific/player")) {
            return false;
        }
        JsonElement player = object.get("player");
        if (player == null || !player.isJsonObject() || player.getAsJsonObject().size() != 1) {
            return false;
        }
        JsonElement advancements = player.getAsJsonObject().get("advancements");
        if (advancements == null || !advancements.isJsonObject() || advancements.getAsJsonObject().isEmpty()) {
            return false;
        }
        for (var entry : advancements.getAsJsonObject().entrySet()) {
            if (Identifier.tryParse(entry.getKey()) == null
                || !entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isBoolean()) {
                return false;
            }
        }
        return true;
    }

    private static boolean looksLikeLegacyEntityPredicate(@NotNull JsonObject object) {
        if (looksLikeItemPredicate(object)) {
            return false;
        }
        if (object.has(LEGACY_TYPE_SPECIFIC)) {
            return true;
        }
        for (String key : object.keySet()) {
            if (LEGACY_TYPE.equals(key) || LEGACY_ENTITY_STRUCK.equals(key) || remapLegacyEntityPredicateKey(key) != null) {
                return true;
            }
        }
        return false;
    }

    private static boolean looksLikeItemPredicate(@NotNull JsonObject object) {
        if (object.keySet().isEmpty()) {
            return false;
        }
        for (String key : object.keySet()) {
            if (!"items".equals(key)
                && !"count".equals(key)
                && !"durability".equals(key)
                && !"predicates".equals(key)
                && !"components".equals(key)
            ) {
                return false;
            }
        }
        return true;
    }

    private static boolean isDirectEntityPredicateKey(@NotNull String key) {
        return "player".equals(key)
            || "entity".equals(key)
            || "direct_entity".equals(key)
            || "source_entity".equals(key)
            || "passenger".equals(key)
            || "vehicle".equals(key);
    }

    private static boolean isNestedEntityPredicateKey(@NotNull String key) {
        return "vehicle".equals(key)
            || "passenger".equals(key)
            || "targeted_entity".equals(key);
    }

    private static boolean isEntityPredicateArrayKey(@NotNull String key) {
        return "entity".equals(key)
            || "player".equals(key)
            || "direct_entity".equals(key)
            || "source_entity".equals(key)
            || "victims".equals(key);
    }

    private static boolean isContextAwarePredicateKey(@NotNull String key) {
        return "entity".equals(key)
            || "player".equals(key)
            || "source".equals(key)
            || "direct_entity".equals(key)
            || "source_entity".equals(key);
    }

    private static boolean looksLikeContextAwarePredicateValue(@NotNull JsonElement value) {
        if (value.isJsonObject()) {
            return looksLikeLootCondition(value.getAsJsonObject());
        }
        if (!value.isJsonArray()) {
            return false;
        }
        JsonArray array = value.getAsJsonArray();
        if (array.isEmpty()) {
            return false;
        }
        for (JsonElement child : array) {
            if (!child.isJsonObject() || !looksLikeLootCondition(child.getAsJsonObject())) {
                return false;
            }
        }
        return true;
    }

    private static boolean looksLikeLootCondition(@NotNull JsonObject object) {
        return object.has("condition");
    }

    private static String remapLegacyEntityPredicateKey(@NotNull String key) {
        return switch (key) {
            case "location",
                "stepping_on",
                "movement_affected_by",
                "distance",
                "movement",
                "effects",
                "nbt",
                "flags",
                "equipment",
                "periodic_tick",
                "vehicle",
                "passenger",
                "targeted_entity",
                "team",
                "slots",
                "components",
                "predicates",
                "entity_tags" -> key;
            case LEGACY_TAG -> "entity_tags";
            default -> null;
        };
    }

    private static @NotNull JsonElement normalizeEntityPredicateValue(@NotNull String originalKey, @NotNull JsonElement value) {
        if (LEGACY_TAG.equals(originalKey) && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            JsonArray anyOf = new JsonArray();
            anyOf.add(value.getAsString());
            JsonObject entityTags = new JsonObject();
            entityTags.add("any_of", anyOf);
            return entityTags;
        }
        return value;
    }

    private static String getLegacyTypeSpecificComponentId(@NotNull String legacyTypeSpecificType) {
        return switch (legacyTypeSpecificType) {
            case "cat" -> "minecraft:cat/variant";
            case "frog" -> "minecraft:frog/variant";
            case "wolf", "minecraft:wolf" -> "minecraft:wolf/variant";
            default -> null;
        };
    }

    private static @NotNull String getLegacyTypeSpecificTargetKey(@NotNull String legacyTypeSpecificType) {
        return switch (legacyTypeSpecificType) {
            case "lightning_bolt", "minecraft:lightning_bolt" -> "type_specific/lightning";
            case "fishing_hook", "minecraft:fishing_hook", "fishing_bobber", "minecraft:fishing_bobber" -> "type_specific/fishing_hook";
            case "slime", "minecraft:slime", "magma_cube", "minecraft:magma_cube" -> "type_specific/cube_mob";
            default -> "type_specific/" + legacyTypeSpecificType;
        };
    }

    private static @NotNull Context getLegacyTypeSpecificContext(@NotNull String legacyTypeSpecificType) {
        return switch (legacyTypeSpecificType) {
            case "player" -> Context.PLAYER_TYPE_SPECIFIC;
            case "raider" -> Context.RAIDER_TYPE_SPECIFIC;
            case "lightning_bolt", "minecraft:lightning_bolt" -> Context.LIGHTNING_TYPE_SPECIFIC;
            default -> Context.GENERIC_TYPE_SPECIFIC;
        };
    }

    private static @NotNull TransformResult transformLegacyTypeSpecificComponents(@NotNull JsonObject object, @NotNull String componentId) {
        boolean changed = false;
        JsonObject componentValues = new JsonObject();
        for (var entry : object.entrySet()) {
            String key = entry.getKey();
            if (LEGACY_TYPE.equals(key)) {
                changed = true;
                continue;
            }
            TransformResult childResult = transformElement(entry.getValue(), Context.NORMAL);
            if ("variant".equals(key)) {
                componentValues.add(componentId, childResult.element());
                changed = true;
            } else {
                componentValues.add(key, childResult.element());
            }
            changed |= childResult.changed();
        }
        return new TransformResult(componentValues, changed);
    }

    private static @NotNull TransformResult transformLegacyEntityStruckValue(@NotNull JsonElement value) {
        JsonObject entityPredicate = collapseLegacyEntityPredicateConditions(value);
        if (entityPredicate == null) {
            return new TransformResult(transformElement(value, Context.NORMAL).element().getAsJsonObject(), false);
        }
        JsonObject lightning = new JsonObject();
        lightning.add("entity_struck", entityPredicate);
        return new TransformResult(lightning, true);
    }

    private static JsonObject collapseLegacyEntityPredicateConditions(@NotNull JsonElement value) {
        if (value.isJsonObject()) {
            JsonObject object = value.getAsJsonObject();
            if (looksLikeLegacyEntityPredicate(object)) {
                return transformElement(object, Context.ENTITY_PREDICATE).element().getAsJsonObject();
            }
            return extractEntityPredicateFromLootCondition(object);
        }
        if (!value.isJsonArray()) {
            return null;
        }
        JsonObject merged = new JsonObject();
        boolean matchedAny = false;
        for (JsonElement child : value.getAsJsonArray()) {
            if (!child.isJsonObject()) {
                return null;
            }
            JsonObject childPredicate = extractEntityPredicateFromLootCondition(child.getAsJsonObject());
            if (childPredicate == null || !mergeJsonObjects(merged, childPredicate)) {
                return null;
            }
            matchedAny = true;
        }
        return matchedAny ? merged : null;
    }

    private static JsonObject extractEntityPredicateFromLootCondition(@NotNull JsonObject condition) {
        if (!ENTITY_PROPERTIES_CONDITION.equals(getString(condition, "condition"))) {
            return null;
        }
        String entity = getString(condition, "entity");
        if (entity != null && !"this".equals(entity)) {
            return null;
        }
        JsonElement predicate = condition.get("predicate");
        if (predicate == null || !predicate.isJsonObject()) {
            return null;
        }
        return transformElement(predicate, Context.ENTITY_PREDICATE).element().getAsJsonObject();
    }

    private static void mergeIntoNamedObject(@NotNull JsonObject target, @NotNull String key, @NotNull JsonObject source) {
        JsonObject destination = target.has(key) && target.get(key).isJsonObject()
            ? target.getAsJsonObject(key)
            : new JsonObject();
        if (!mergeJsonObjects(destination, source)) {
            throw new IllegalStateException("Conflicting entity predicate conversion for " + key);
        }
        target.add(key, destination);
    }

    private static boolean mergeJsonObjects(@NotNull JsonObject target, @NotNull JsonObject source) {
        for (var entry : source.entrySet()) {
            String key = entry.getKey();
            JsonElement sourceValue = entry.getValue();
            if (!target.has(key)) {
                target.add(key, sourceValue);
                continue;
            }
            JsonElement targetValue = target.get(key);
            if (targetValue.isJsonObject() && sourceValue.isJsonObject()) {
                if (!mergeJsonObjects(targetValue.getAsJsonObject(), sourceValue.getAsJsonObject())) {
                    return false;
                }
                continue;
            }
            if (!targetValue.equals(sourceValue)) {
                return false;
            }
        }
        return true;
    }

    private static @NotNull JsonElement normalizeEntityType(@NotNull JsonElement value) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            return value;
        }
        String entityType = remapLegacyEntityTypeId(value.getAsString());
        if (entityType.startsWith("#")) {
            if (entityType.startsWith("#minecraft:") || entityType.substring(1).contains(":")) {
                return JsonParser.parseString('"' + entityType + '"');
            }
            return JsonParser.parseString('"' + "#minecraft:" + entityType.substring(1) + '"');
        }
        if (entityType.contains(":")) {
            return JsonParser.parseString('"' + entityType + '"');
        }
        return JsonParser.parseString('"' + "minecraft:" + entityType + '"');
    }

    private static @NotNull String remapLegacyEntityTypeId(@NotNull String entityType) {
        return switch (entityType) {
            case "potion", "minecraft:potion" -> "minecraft:splash_potion";
            default -> entityType;
        };
    }

    private static @NotNull JsonElement normalizeAdvancementBackground(@NotNull JsonElement value) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            return value;
        }
        String background = value.getAsString();
        int separator = background.indexOf(':');
        if (separator <= 0 || !background.startsWith("textures/", separator + 1) || !background.endsWith(".png")) {
            return value;
        }
        String namespace = background.substring(0, separator);
        String path = background.substring(separator + 1 + "textures/".length(), background.length() - ".png".length());
        return JsonParser.parseString('"' + namespace + ':' + path + '"');
    }

    private static String getString(@NotNull JsonObject object, @NotNull String key) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            return null;
        }
        return value.getAsString();
    }

    private static byte[] readEntryBytes(@NotNull ZipFile zipFile, @NotNull ZipEntry entry) throws IOException {
        try (InputStream input = zipFile.getInputStream(entry);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            input.transferTo(output);
            return output.toByteArray();
        }
    }

    private static void writeEntry(@NotNull ZipOutputStream out, @NotNull String name, byte @NotNull [] bytes) throws IOException {
        ZipEntry targetEntry = new ZipEntry(name);
        targetEntry.setTime(0L);
        out.putNextEntry(targetEntry);
        out.write(bytes);
        out.closeEntry();
    }

    private static void writeCompatibilityMarker(@NotNull ZipOutputStream out, @NotNull ExternalPack externalPack, @NotNull String actualSourceSha1) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("version", MARKER_VERSION);
        properties.setProperty("fileName", externalPack.getFileName());
        properties.setProperty("sourceSha1", actualSourceSha1);
        properties.setProperty(ROOT_OVERRIDE_SHA1_PROPERTY, currentRootOverrideSha1());
        if (externalPack == ExternalPack.BACAP) {
            properties.setProperty(LLAMA_CARPET_NBT_PROPERTY, "equipment.body");
            properties.setProperty(RAIDER_PREDICATE_KEYS_PROPERTY, "snake_case");
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        properties.store(output, null);
        writeEntry(out, MARKER_ENTRY, output.toByteArray());
    }

    private record ConversionResult(@NotNull String text, boolean changed) {
    }

    private record TransformResult(@NotNull JsonElement element, boolean changed) {
    }

    public enum WorldPackSyncResult {
        NO_KNOWN_PACKS,
        ALREADY_CURRENT,
        UPDATED,
        MISSING_REQUIRED_PACK,
        INTEGRITY_CHECK_FAILED,
        UNKNOWN_ERROR
    }

    private enum Context {
        NORMAL,
        CONDITIONS,
        DAMAGE_PREDICATE,
        KILLING_BLOW_PREDICATE,
        ENTITY_PREDICATE,
        PLAYER_TYPE_SPECIFIC,
        RAIDER_TYPE_SPECIFIC,
        GENERIC_TYPE_SPECIFIC,
        LIGHTNING_TYPE_SPECIFIC
    }
}
