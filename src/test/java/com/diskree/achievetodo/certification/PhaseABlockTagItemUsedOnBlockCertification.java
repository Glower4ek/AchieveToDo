package com.diskree.achievetodo.certification;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.zip.ZipFile;

/** Retained criterion slice; absence of an explicit requirements array means AND of all criteria. */
public final class PhaseABlockTagItemUsedOnBlockCertification {
    public static final Path SNAPSHOT = Path.of("src/test/resources/phase_a_certification/block_tag_item_used_on_block_case_catalog.json");
    public static final String FAMILY = "BLOCK_TAG_ITEM_USED_ON_BLOCK";
    public static final String SOURCE = "PhaseABlockTagItemUsedOnBlockGameTest";
    static final Map<String, List<String>> EXPECTED = Map.of(
        "blazeandcave:adventure/im_not_lost_anymore", List.of("map"),
        "blazeandcave:building/colors_of_the_wind", Arrays.stream("white orange magenta light_blue yellow lime pink gray light_gray cyan purple blue brown green red black".split(" ")).map(c -> c + "_dye").toList(),
        "blazeandcave:building/delicious_hot_schmoes", List.of("porkchop", "beef", "chicken", "cod", "salmon", "potato", "mutton", "rabbit", "kelp"),
        "blazeandcave:building/sign_off", List.of("honeycomb"),
        "blazeandcave:farming/one_course_meal", List.of("bone_meal"),
        "minecraft:husbandry/make_a_sign_glow", List.of("glow_ink_sac"),
        "minecraft:husbandry/safely_harvest_honey", List.of("safely_harvest_honey"));

    public static Set<String> expectedKeys() {
        Set<String> keys = new TreeSet<>();
        EXPECTED.forEach((id, criteria) -> criteria.forEach(c -> keys.add(id + "#" + c)));
        return Collections.unmodifiableSet(keys);
    }

    private static final String BACAP = "reference/phase_a_preservation/files/final/bacap.zip";
    private static final String BACAP_SHA = "8c72314535c5df7b4416bf0f38310371ec8aec537fde1445bdc820a3c9aada70";

    public static String generateSnapshot(Path root) throws IOException {
        require(sha256(Files.readAllBytes(root.resolve(BACAP))).equals(BACAP_SHA), "Frozen BACAP hash mismatch");
        JsonObject result = new JsonObject();
        result.addProperty("family", FAMILY);
        JsonObject provenance = new JsonObject();
        provenance.add("frozenBacap", source(BACAP, BACAP_SHA));
        result.add("provenance", provenance);
        JsonObject actionProvenance = new JsonObject();
        actionProvenance.addProperty("kind", "MANUALLY_REVIEWED_A0_DESIGN");
        actionProvenance.addProperty("verdict", "SPLIT_REQUIRED");
        actionProvenance.addProperty("runtimeEvidence", false);
        result.add("actionContractProvenance", actionProvenance);
        result.addProperty("totalCases", 30);
        result.addProperty("uniqueAdvancements", 7);
        result.addProperty("requirementGroups", 30);
        JsonArray cases = new JsonArray();
        JsonObject coverage = new JsonObject();
        int totalGroups = 0;
        int bacapAdvancements = 0, excluded = 0;
        Set<String> actual = new TreeSet<>();
        try (ZipFile bacap = new ZipFile(root.resolve(BACAP).toFile(), StandardCharsets.UTF_8)) {
            for (String id : new TreeSet<>(EXPECTED.keySet())) {
                bacapAdvancements++;
                String path = "data/" + id.replace(":", "/advancement/") + ".json";
                byte[] raw = readEntry(bacap, path);
                JsonObject advancement = JsonParser.parseString(new String(raw, StandardCharsets.UTF_8)).getAsJsonObject();
                if (id.startsWith("minecraft:")) validateHistoricalMinecraftDefinition(id, raw, advancement);
                JsonObject criteria = advancement.getAsJsonObject("criteria");
                boolean meal = id.equals("blazeandcave:farming/one_course_meal");
                Set<String> sourceCriteria = new TreeSet<>(EXPECTED.get(id));
                if (meal) sourceCriteria.add("bone_meal_propagule");
                require(criteria.keySet().equals(sourceCriteria), "Unexpected source criterion set: " + id);
                JsonArray groups = advancement.getAsJsonArray("requirements");
                if (groups == null) {
                    groups = new JsonArray();
                    for (String criterion : criteria.keySet()) {
                        JsonArray group = new JsonArray(); group.add(criterion); groups.add(group);
                    }
                }
                totalGroups += groups.size();
                require(groups.size() == EXPECTED.get(id).size(), "Requirement group count: " + id);
                coverage.addProperty(id, groups.size());
                for (JsonElement group : groups) {
                    Set<String> names = new TreeSet<>();
                    group.getAsJsonArray().forEach(c -> names.add(c.getAsString()));
                    require(sourceCriteria.containsAll(names), "Unknown group criterion");
                    require(meal ? names.equals(Set.of("bone_meal", "bone_meal_propagule")) : group.getAsJsonArray().size() == 1, "Wrong requirement shape");
                    require(group.getAsJsonArray().asList().stream().anyMatch(c -> EXPECTED.get(id).contains(c.getAsString())), "Uncovered requirement group: " + id);
                }
                Set<Integer> covered = new TreeSet<>();
                for (String criterion : new TreeSet<>(criteria.keySet())) {
                    JsonObject definition = criteria.getAsJsonObject(criterion);
                    require("minecraft:item_used_on_block".equals(definition.get("trigger").getAsString()), "Wrong trigger");
                    require(definition.getAsJsonObject("conditions").keySet().equals(Set.of("location")), "Unexpected conditions");
                    JsonArray locations = definition.getAsJsonObject("conditions").getAsJsonArray("location");
                    require(locations.size() == 2, "Expected two location conditions");
                    Set<String> conditionTypes = new HashSet<>();
                    String tag = null, item = null;
                    boolean smokey = false;
                    for (JsonElement element : locations) {
                        JsonObject condition = element.getAsJsonObject();
                        require(conditionTypes.add(condition.get("condition").getAsString()), "Duplicate condition");
                        JsonObject predicate = condition.getAsJsonObject("predicate");
                        switch (condition.get("condition").getAsString()) {
                            case "minecraft:location_check" -> {
                                require(predicate.keySet().equals(id.endsWith("/safely_harvest_honey") ? Set.of("block", "smokey") : Set.of("block")), "Unexpected location predicate");
                                require(predicate.getAsJsonObject("block").keySet().equals(Set.of("blocks")), "Unexpected block predicate");
                                JsonElement blocks = predicate.getAsJsonObject("block").get("blocks");
                                if (blocks.isJsonPrimitive() && blocks.getAsString().startsWith("#")) tag = blocks.getAsString();
                                else require(id.equals("blazeandcave:farming/one_course_meal") && criterion.equals("bone_meal_propagule")
                                    && blocks.equals(JsonParser.parseString("[\"minecraft:air\"]")), "Unexpected non-tag criterion");
                                smokey = predicate.has("smokey") && predicate.get("smokey").getAsBoolean();
                            }
                            case "minecraft:match_tool" -> {
                                require(predicate.keySet().equals(Set.of("items")), "Unexpected tool predicate");
                                JsonElement items = predicate.get("items");
                                if (items.isJsonArray()) {
                                    require(items.getAsJsonArray().size() == 1, "Expected exact held item");
                                    item = items.getAsJsonArray().get(0).getAsString();
                                } else item = items.getAsString();
                            }
                            default -> throw new IllegalStateException("Unexpected condition");
                        }
                    }
                    if (tag == null) {
                        require(meal && criterion.equals("bone_meal_propagule") && "minecraft:bone_meal".equals(item), "Unexpected excluded criterion");
                        excluded++; continue;
                    }
                    require(item != null && !item.startsWith("#"), "Missing exact held item");
                    String expectedItem = switch (criterion) {
                        case "map" -> "filled_map";
                        case "safely_harvest_honey" -> "glass_bottle";
                        default -> criterion;
                    };
                    require(item.equals("minecraft:" + expectedItem), "Wrong held item");
                    String expectedTag = switch (id.substring(id.lastIndexOf('/') + 1)) {
                        case "im_not_lost_anymore" -> "#minecraft:banners";
                        case "delicious_hot_schmoes" -> "#minecraft:campfires";
                        case "one_course_meal" -> "#minecraft:logs_that_burn";
                        case "safely_harvest_honey" -> "#minecraft:beehives";
                        default -> "#minecraft:all_signs";
                    };
                    require(tag.equals(expectedTag) && smokey == id.endsWith("/safely_harvest_honey"), "Wrong location contract");
                    int groupIndex = -1;
                    for (int i = 0; i < groups.size(); i++) if (groups.get(i).getAsJsonArray().contains(new JsonPrimitive(criterion))) {
                        require(groupIndex == -1, "Repeated group witness"); groupIndex = i;
                    }
                    require(groupIndex >= 0, "Criterion outside requirements");
                    covered.add(groupIndex);
                    JsonObject entry = new JsonObject();
                    entry.addProperty("advancementId", id); entry.addProperty("criterion", criterion);
                    entry.addProperty("requirementGroupIndex", groupIndex);
                    entry.addProperty("trigger", "minecraft:item_used_on_block");
                    entry.addProperty("blockTag", tag);
                    entry.addProperty("blockTagSampling", "POST_USE_CLICKED_POSITION");
                    entry.addProperty("heldItem", item);
                    String action = switch (tag) {
                        case "#minecraft:banners" -> "MAP_BANNER";
                        case "#minecraft:campfires" -> "PLACE_FOOD";
                        case "#minecraft:logs_that_burn" -> "GROW_OAK_SAPLING";
                        case "#minecraft:beehives" -> "BOTTLE_HONEY";
                        case "#minecraft:all_signs" -> switch (id) {
                            case "blazeandcave:building/sign_off" -> "WAX_SIGN";
                            case "minecraft:husbandry/make_a_sign_glow" -> "GLOW_SIGN";
                            case "blazeandcave:building/colors_of_the_wind" -> "DYE_SIGN";
                            default -> throw new IllegalStateException("Unexpected sign advancement: " + id);
                        };
                        default -> throw new IllegalStateException("Unexpected tag");
                    };
                    entry.addProperty("action", action);
                    entry.addProperty("expectedBefore", before(action)); entry.addProperty("expectedAfter", after(action));
                    entry.addProperty("expectedCriterionBefore", false); entry.addProperty("expectedCriterionAfter", true);
                    entry.addProperty("requiresSmoke", smokey);
                    entry.addProperty("semanticsSource", "frozenBacap");
                    entry.addProperty("sourcePath", path); entry.addProperty("sourceJsonSha256", sha256(raw));
                    cases.add(entry); require(actual.add(id + "#" + criterion), "Duplicate criterion");
                }
                require(covered.size() == groups.size(), "Incomplete requirement coverage");
            }
        }
        require(bacapAdvancements == 7 && excluded == 1, "Wrong frozen provenance/split");
        require(totalGroups == 30 && cases.size() == 30 && actual.equals(expectedKeys()), "Retained exact30 mismatch");
        result.add("requirementGroupCoverage", coverage);
        result.add("cases", cases);
        return new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create().toJson(result) + "\n";
    }

    static void validateHistoricalMinecraftDefinition(String id, byte[] raw, JsonObject advancement) {
        String criterion, parent, reward, hash;
        switch (id) {
            case "minecraft:husbandry/make_a_sign_glow" -> {
                criterion = "glow_ink_sac"; parent = "blazeandcave:animal/cephalight";
                reward = "bacap_rewards:animal/glow_and_behold";
                hash = "f92bf32856bf11db5d21804106d8f1f7c05d3763cc61bee96631cbaf9ee55716";
            }
            case "minecraft:husbandry/safely_harvest_honey" -> {
                criterion = "safely_harvest_honey"; parent = "blazeandcave:animal/ya_like_jazz";
                reward = "bacap_rewards:animal/bee_our_guest";
                hash = "ea57b086ee03239a5176b5ef29f39e7df2bfdda9b07a9f436bfd3b5dbde1865c";
            }
            default -> throw new IllegalStateException("Unexpected historical Minecraft advancement: " + id);
        }
        require(hash.equals(sha256(raw)), "Frozen entry hash mismatch: " + id);
        require(advancement.getAsJsonObject("criteria").keySet().equals(Set.of(criterion)), "Wrong historical criteria: " + id);
        require(parent.equals(advancement.get("parent").getAsString()), "Wrong historical parent: " + id);
        JsonObject rewards = advancement.getAsJsonObject("rewards");
        require(rewards.keySet().equals(Set.of("function")) && reward.equals(rewards.get("function").getAsString()), "Wrong historical reward: " + id);
        JsonArray group = new JsonArray(); group.add(criterion);
        JsonArray expected = new JsonArray(); expected.add(group);
        require(!advancement.has("requirements") || expected.equals(advancement.get("requirements")), "Wrong historical requirements: " + id);
    }

    static String before(String action) {
        return switch (action) {
            case "WAX_SIGN" -> "waxed=false;mayBuild=true";
            case "DYE_SIGN" -> "activeSideTextPresent=true;color!=heldDye;waxed=false;mayBuild=true";
            case "GLOW_SIGN" -> "activeSideTextPresent=true;glowing=false;waxed=false;mayBuild=true";
            case "MAP_BANNER" -> "validFilledMap;mapCoversBanner;bannerMarkerAbsent";
            case "PLACE_FOOD" -> "emptyCookingSlot;exactCampfireRecipe;USE_CAMPFIRE=unlocked;lockedLandmark=false";
            case "GROW_OAK_SAPLING" -> "clickedBlock=minecraft:oak_sapling;stage=1;planted=true;validSupport;clearGrowthVolume";
            case "BOTTLE_HONEY" -> "honeyLevel=5;litCampfireBelow=true;smokey=true";
            default -> throw new IllegalArgumentException(action);
        };
    }
    static String after(String action) {
        return switch (action) {
            case "WAX_SIGN" -> "waxed=true;heldCountDelta=-1";
            case "DYE_SIGN" -> "activeSideColor=heldDye;heldCountDelta=-1";
            case "GLOW_SIGN" -> "activeSideGlowing=true;heldCountDelta=-1";
            case "MAP_BANNER" -> "bannerMarkerPresent;heldCountDelta=0";
            case "PLACE_FOOD" -> "exactFoodInPreviouslyEmptyCookingSlot;heldCountDelta=-1";
            case "GROW_OAK_SAPLING" -> "successfulTreeGrowth=true;clickedBlock=minecraft:oak_log;clickedBlockInTag=minecraft:logs_that_burn;heldCountDelta=-1";
            case "BOTTLE_HONEY" -> "honeyLevel=0;honeyBottleProduced;smokey=true";
            default -> throw new IllegalArgumentException(action);
        };
    }
    public static String fingerprint(Path root) throws IOException {
        return "sha-256:" + sha256(Files.readAllBytes(root.resolve(SNAPSHOT)));
    }
    private static String sha256(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private static byte[] readEntry(ZipFile zip, String path) throws IOException {
        try (InputStream input = zip.getInputStream(Objects.requireNonNull(zip.getEntry(path), path))) { return input.readAllBytes(); }
    }
    private static JsonObject source(String path, String hash) {
        JsonObject source = new JsonObject(); source.addProperty("path", path); source.addProperty("sha256", hash); return source;
    }
    static void require(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
}
