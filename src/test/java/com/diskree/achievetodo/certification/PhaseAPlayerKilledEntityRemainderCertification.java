package com.diskree.achievetodo.certification;

import com.google.gson.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.ZipFile;

/** Frozen victim/source contexts, with one selected native kill per requirement group. */
public final class PhaseAPlayerKilledEntityRemainderCertification {
    public static final String FAMILY = "PLAYER_KILLED_ENTITY_REMAINDER";
    public static final String SOURCE = "PhaseAPlayerKilledEntityRemainderGameTest";
    public static final Path CATALOG = Path.of("src/test/resources/phase_a_certification/player_killed_entity_remainder_case_catalog.json");
    public static final Path PERSISTENT = Path.of("src/test/resources/phase_a_certification/player_killed_entity_remainder_execution_evidence.json");
    public static final String MELEE = "ServerboundAttackPacket.handle->ServerGamePacketListenerImpl.handleAttack->ServerPlayer.attack->LivingEntity.hurtServer->CriteriaTriggers.PLAYER_KILLED_ENTITY";
    public static final String ARROW = "ServerboundUseItemPacket.handle->BowItem.use->ServerboundPlayerActionPacket.handle->BowItem.releaseUsing->Arrow.onHitEntity->LivingEntity.hurtServer->CriteriaTriggers.PLAYER_KILLED_ENTITY";
    public static final Map<String, String> FROZEN = Map.ofEntries(
        Map.entry("blazeandcave:adventure/heres_johnny", "fde9bdb61495f1e518f9ac5dd312a4480ceac6425ee56a65802fc68041746192"),
        Map.entry("blazeandcave:adventure/wololo", "93a9d59ed2b796bf25edbf8a8d3a70176d5dbcf8538feba6ace5c67bf3b44a30"),
        Map.entry("blazeandcave:animal/axeolotl", "a87c4929d757d26ae6795c1548459952c42eb25818b9ff3375a7e2810209ac94"),
        Map.entry("blazeandcave:animal/silence_of_the_lambs", "7822eae544bed598f43e0b53fc187e1d5d83280a7a8b2310f163bc5678cd226d"),
        Map.entry("blazeandcave:animal/the_high_road", "d9cce8d33cdd3b8f0a2855f45bc01f3ddecc08f8ccc121faa6d5a3c7626804a7"),
        Map.entry("blazeandcave:monsters/freezing", "b905873a488999750d8c2b9235c87ce3d4e8b51fcb0296f73332d3da473e93a0"),
        Map.entry("blazeandcave:monsters/keep_your_distance", "cd2afa627787bc1b0291dbae0a7b553f242932546fe5f7e1560a117e152c7952"),
        Map.entry("blazeandcave:monsters/melting", "501eb3ca9b17cb75a599cd22b7f513649a035dcc200810598f164a0c78f38920"),
        Map.entry("blazeandcave:monsters/spider_skeleton", "7be560fa28393c15d02ac79f96c114c7adc936b1836a19336021a26e03991363"),
        Map.entry("blazeandcave:monsters/the_ghastly_eyes", "67218599c2f0e844d875c1e5dbb20adb62ec5d7a72095bc8774fe399b499049d"),
        Map.entry("blazeandcave:monsters/warden_frostbite", "f5d18da50e8457c7236df548a7720ed76489d2ddd74dc140b748604352d30993"),
        Map.entry("blazeandcave:nether/bring_down_the_beast", "11c5b0c26322640af75396033365a0cd7ce1d10b7e2f3b259699f939ec809498"),
        Map.entry("blazeandcave:nether/we_got_a_live_one", "ec5be505f4c06cad666fc8c45dc6f6267caf6cbeff7bd10e2aacce16e278069e"),
        Map.entry("blazeandcave:nether/when_piglins_fly", "fda1ba1a40fe75cc50f2844d9b5dedd4491129c5d8bffb7e1d4fb16281df4935"),
        Map.entry("blazeandcave:weaponry/spleaf", "da588adda954fc357fd2181421321f9d10262d470fe629de6342151f6b9e9443"),
        Map.entry("blazeandcave:weaponry/the_mighty_hunter", "c6eadd3804914db923646acd99743716f73a0bffbbd291400d32c796ef9a157c"));
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private PhaseAPlayerKilledEntityRemainderCertification() { }
    public static void main(String[] args) throws Exception { Path root = Path.of(args[0]).toAbsolutePath(); Files.writeString(root.resolve(CATALOG), generate(root)); }
    public static void validate(Path root) throws Exception { require(generate(root).equals(Files.readString(root.resolve(CATALOG))), "Frozen kill catalog mismatch"); }
    public static String generate(Path root) throws Exception {
        Path archive = root.resolve("reference/phase_a_preservation/files/final/bacap.zip");
        require(sha(Files.readAllBytes(archive)).equals("8c72314535c5df7b4416bf0f38310371ec8aec537fde1445bdc820a3c9aada70"), "Frozen BACAP changed");
        JsonObject catalog = new JsonObject(); catalog.addProperty("snapshot", "phase_a_player_killed_entity_remainder_case_catalog"); catalog.addProperty("family", FAMILY); catalog.addProperty("source", SOURCE);
        catalog.addProperty("minecraftVersion", "26.2"); catalog.addProperty("compatibilityMarker", "compat_26_2_r15"); JsonArray sources = new JsonArray(), cases = new JsonArray(); int criteriaCount = 0;
        try (ZipFile zip = new ZipFile(archive.toFile(), StandardCharsets.UTF_8)) {
            for (var entry : new TreeMap<>(FROZEN).entrySet()) {
                String[] id = entry.getKey().split(":", 2); String path = "data/" + id[0] + "/advancement/" + id[1] + ".json";
                byte[] bytes; try (var in = zip.getInputStream(zip.getEntry(path))) { bytes = in.readAllBytes(); } require(sha(bytes).equals(entry.getValue()), "Frozen kill source changed");
                JsonObject raw = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject(); JsonObject criteria = raw.getAsJsonObject("criteria"); criteriaCount += criteria.size();
                JsonArray groups = raw.getAsJsonArray("requirements"); if (groups == null) { groups = new JsonArray(); for (String key : criteria.keySet()) { JsonArray group = new JsonArray(); group.add(key); groups.add(group); } }
                JsonObject source = new JsonObject(); source.addProperty("advancementId", entry.getKey()); source.addProperty("sha256", entry.getValue()); source.add("requirements", groups.deepCopy()); sources.add(source);
                for (int i = 0; i < groups.size(); i++) {
                    JsonArray alternatives = groups.get(i).getAsJsonArray(); String selected = alternatives.get(0).getAsString(); JsonObject criterion = criteria.getAsJsonObject(selected);
                    require(criterion.get("trigger").getAsString().equals("minecraft:player_killed_entity"), "Wrong trigger");
                    JsonObject conditions = criterion.getAsJsonObject("conditions"); JsonObject entity = entityPredicate(conditions.get("entity"));
                    String selector = entity.has("type") ? entity.get("type").getAsString() : "minecraft:zombie";
                    String victim = switch (selector) {
                        case "#blazeandcave:hostile_monsters" -> "minecraft:zombie";
                        case "#blazeandcave:piglins" -> "minecraft:piglin";
                        case "#blazeandcave:spiders" -> "minecraft:spider";
                        case "#minecraft:skeletons" -> "minecraft:skeleton";
                        default -> { require(!selector.startsWith("#"), "Unsupported entity selector"); yield namespaced(selector); }
                    };
                    JsonObject blow = conditions.has("killing_blow") ? conditions.getAsJsonObject("killing_blow") : new JsonObject(); boolean arrow = false;
                    if (blow.has("tags")) for (var tag : blow.getAsJsonArray("tags")) if (tag.getAsJsonObject().get("id").getAsString().equals("minecraft:is_projectile")) arrow = tag.getAsJsonObject().get("expected").getAsBoolean();
                    String tool = arrow ? "minecraft:bow" : entry.getKey().endsWith("/axeolotl") ? "minecraft:wooden_axe" : entry.getKey().endsWith("/spleaf") ? "minecraft:wooden_shovel" : "minecraft:wooden_sword";
                    JsonObject row = new JsonObject(); row.addProperty("advancementId", entry.getKey()); row.addProperty("criterion", selected); row.addProperty("requirementGroup", i);
                    row.add("alternatives", alternatives.deepCopy()); row.add("frozenConditions", conditions.deepCopy()); row.add("entityPredicate", entity); row.add("killingBlow", blow.deepCopy());
                    row.addProperty("selectedEntity", victim); row.addProperty("action", arrow ? "ARROW" : "MELEE"); row.addProperty("selectedTool", tool); cases.add(row);
                }
            }
        }
        require(criteriaCount == 46 && cases.size() == 43, "Kill family cardinality changed"); catalog.addProperty("criteriaCount", criteriaCount); catalog.addProperty("requirementGroupCount", cases.size()); catalog.add("sources", sources); catalog.add("cases", cases); return GSON.toJson(catalog) + "\n";
    }
    private static JsonObject entityPredicate(JsonElement element) {
        if (element.isJsonObject()) return element.getAsJsonObject().deepCopy();
        JsonArray terms = element.getAsJsonArray(); require(terms.size() == 1, "Unexpected entity loot context"); JsonObject term = terms.get(0).getAsJsonObject();
        require(term.get("condition").getAsString().equals("minecraft:entity_properties") && term.get("entity").getAsString().equals("this"), "Wrong entity context"); return term.getAsJsonObject("predicate").deepCopy();
    }
    public static String namespaced(String id) { return id.contains(":") ? id : "minecraft:" + id; }
    public static List<JsonObject> cases(Path root) throws Exception { validate(root); List<JsonObject> cases = new ArrayList<>(); for (var row : JsonParser.parseString(Files.readString(root.resolve(CATALOG))).getAsJsonObject().getAsJsonArray("cases")) cases.add(row.getAsJsonObject()); return cases; }
    public static String key(JsonObject row) { return row.get("advancementId").getAsString() + "#" + row.get("criterion").getAsString(); }
    public static String sha(byte[] bytes) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
    private static void require(boolean yes, String message) { if (!yes) throw new IllegalStateException(message); }
}
