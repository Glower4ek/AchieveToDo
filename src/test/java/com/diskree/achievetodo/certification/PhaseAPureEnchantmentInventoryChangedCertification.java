package com.diskree.achievetodo.certification;

import com.google.gson.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.ZipFile;

/** One frozen witness per requirement group; book/item OR alternatives stay explicit. */
public final class PhaseAPureEnchantmentInventoryChangedCertification {
    public static final String FAMILY = "PURE_ENCHANTMENT_INVENTORY_CHANGED";
    public static final String SOURCE = "PhaseAPureEnchantmentInventoryChangedGameTest";
    public static final Path CATALOG = Path.of("src/test/resources/phase_a_certification/pure_enchantment_inventory_changed_case_catalog.json");
    public static final Path PERSISTENT = Path.of("src/test/resources/phase_a_certification/pure_enchantment_inventory_changed_execution_evidence.json");
    public static final String PICKUP = "ItemEntity.playerTouch->Inventory.add->InventoryChangeTrigger";
    public static final String EQUIP = "ServerboundContainerClickPacket.handle->ServerGamePacketListenerImpl.handleContainerClick->InventoryMenu->InventoryChangeTrigger";
    public static final Map<String, String> FROZEN = Map.of(
        "blazeandcave:challenges/ultimate_enchanter", "59d4b27c0eaeae286119bcc91c3c97706158a2ea5216d22e48f04a1acb122a4d",
        "blazeandcave:enchanting/complete_enchanter", "f652018aebee265b8d7839048248419d78500d20b724a8ecd59ec9de649c2412",
        "blazeandcave:enchanting/master_enchanter", "0183c04fa54285c46efe69125e91d48c7e22d32bb04592d44a8222e38ffee947",
        "blazeandcave:enchanting/god_of_thunder", "7c8c6c00ad683debe339c40da714d8dc415ad692b1080930d5e277ec0a29b6ab",
        "blazeandcave:enchanting/handmade_blinding", "bf6a491aa76cf4b1b2db34f4f60024f31bae8d1f7a0568ba3adfcd40287a5276");
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private PhaseAPureEnchantmentInventoryChangedCertification() { }
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]).toAbsolutePath().normalize();
        Files.writeString(root.resolve(CATALOG), generate(root), StandardCharsets.UTF_8);
    }
    public static void validate(Path root) throws Exception {
        require(generate(root).equals(Files.readString(root.resolve(CATALOG))), "Frozen inventory catalog mismatch");
    }
    public static String generate(Path root) throws Exception {
        Path archive = root.resolve("reference/phase_a_preservation/files/final/bacap.zip");
        require(sha(Files.readAllBytes(archive)).equals("8c72314535c5df7b4416bf0f38310371ec8aec537fde1445bdc820a3c9aada70"), "Frozen BACAP changed");
        JsonObject catalog = new JsonObject(); catalog.addProperty("snapshot", "phase_a_pure_enchantment_inventory_changed_case_catalog");
        catalog.addProperty("family", FAMILY); catalog.addProperty("source", SOURCE);
        catalog.addProperty("minecraftVersion", "26.2"); catalog.addProperty("compatibilityMarker", "compat_26_2_r15");
        JsonArray sources = new JsonArray(), cases = new JsonArray(); int criteriaCount = 0;
        try (ZipFile zip = new ZipFile(archive.toFile(), StandardCharsets.UTF_8)) {
            for (var entry : new TreeMap<>(FROZEN).entrySet()) {
                String[] parts = entry.getKey().split(":", 2);
                String path = "data/" + parts[0] + "/advancement/" + parts[1] + ".json";
                byte[] bytes; try (var in = zip.getInputStream(zip.getEntry(path))) { bytes = in.readAllBytes(); }
                require(sha(bytes).equals(entry.getValue()), "Frozen advancement changed " + path);
                JsonObject raw = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
                JsonObject criteria = raw.getAsJsonObject("criteria"); criteriaCount += criteria.size();
                JsonArray groups = raw.getAsJsonArray("requirements");
                if (groups == null) { groups = new JsonArray(); for (String key : criteria.keySet()) { JsonArray group = new JsonArray(); group.add(key); groups.add(group); } }
                JsonObject source = new JsonObject(); source.addProperty("advancementId", entry.getKey()); source.addProperty("path", path);
                source.addProperty("sha256", entry.getValue()); source.addProperty("criteriaCount", criteria.size()); source.add("requirements", groups.deepCopy()); sources.add(source);
                for (int i = 0; i < groups.size(); i++) {
                    JsonArray alternatives = groups.get(i).getAsJsonArray();
                    require(alternatives.size() == 1 || alternatives.size() == 2, "Unexpected group shape");
                    String selected = alternatives.get(alternatives.size() - 1).getAsString();
                    if (alternatives.size() == 2) require(selected.endsWith("_book"), "Expected frozen item/book OR");
                    JsonObject criterion = criteria.getAsJsonObject(selected);
                    require("minecraft:inventory_changed".equals(criterion.get("trigger").getAsString()), "Wrong selected trigger");
                    JsonObject conditions = criterion.getAsJsonObject("conditions");
                    JsonObject item; String action;
                    if (conditions.has("player")) {
                        require(entry.getKey().equals("blazeandcave:enchanting/handmade_blinding") && conditions.size() == 1, "Unknown equipment case");
                        JsonArray player = conditions.getAsJsonArray("player"); require(player.size() == 1, "Equipment conditions changed");
                        JsonObject term = player.get(0).getAsJsonObject();
                        require("minecraft:entity_properties".equals(term.get("condition").getAsString()) && "this".equals(term.get("entity").getAsString()), "Wrong equipment entity");
                        item = term.getAsJsonObject("predicate").getAsJsonObject("equipment").getAsJsonObject("head"); action = "EQUIP_HEAD";
                    } else {
                        require(conditions.size() == 1 && conditions.getAsJsonArray("items").size() == 1, "Unexpected item conditions");
                        item = conditions.getAsJsonArray("items").get(0).getAsJsonObject(); action = "PICKUP";
                    }
                    JsonObject predicates = item.getAsJsonObject("predicates"); require(predicates.size() == 1, "Unexpected item predicate");
                    String storage = predicates.has("stored_enchantments") ? "STORED_ENCHANTMENTS" : "ENCHANTMENTS";
                    JsonArray expected = predicates.getAsJsonArray(storage.equals("STORED_ENCHANTMENTS") ? "stored_enchantments" : "enchantments");
                    JsonObject configured = new JsonObject();
                    for (JsonElement element : expected) {
                        JsonObject predicate = element.getAsJsonObject(); require(predicate.size() <= 2, "Unexpected enchantment constraint");
                        String enchantment = predicate.get("enchantments").getAsString(); require(enchantment.startsWith("minecraft:") && !enchantment.startsWith("#"), "Not direct enchantment");
                        int level = 1;
                        if (predicate.has("levels")) {
                            JsonElement bound = predicate.get("levels");
                            level = bound.isJsonPrimitive() ? bound.getAsInt() : bound.getAsJsonObject().get("min").getAsInt();
                            if (bound.isJsonObject() && bound.getAsJsonObject().has("max")) require(level <= bound.getAsJsonObject().get("max").getAsInt(), "Invalid level bounds");
                        }
                        require(level > 0, "Nonpositive selected enchantment"); configured.addProperty(enchantment, level);
                    }
                    String selectedItem = "minecraft:enchanted_book";
                    if (!storage.equals("STORED_ENCHANTMENTS")) {
                        require(item.getAsJsonArray("items").size() == 1, "Missing direct witness item");
                        selectedItem = item.getAsJsonArray("items").get(0).getAsString();
                    }
                    String name = "";
                    if (item.has("components")) { require(item.getAsJsonObject("components").size() == 1, "Unexpected component"); name = item.getAsJsonObject("components").get("minecraft:custom_name").getAsString(); }
                    JsonObject row = new JsonObject(); row.addProperty("advancementId", entry.getKey()); row.addProperty("criterion", selected);
                    row.addProperty("requirementGroup", i); row.add("alternatives", alternatives.deepCopy()); row.add("frozenConditions", conditions.deepCopy());
                    row.addProperty("action", action); row.addProperty("selectedItem", selectedItem); row.addProperty("storage", storage);
                    row.addProperty("customName", name); row.add("configuredEnchantments", configured); cases.add(row);
                }
            }
        }
        require(criteriaCount == 420 && cases.size() == 211, "Family cardinality changed");
        catalog.addProperty("criteriaCount", criteriaCount); catalog.addProperty("requirementGroupCount", cases.size());
        catalog.add("sources", sources); catalog.add("cases", cases); return GSON.toJson(catalog) + "\n";
    }
    public static List<JsonObject> cases(Path root) throws Exception {
        validate(root); List<JsonObject> cases = new ArrayList<>();
        for (var entry : JsonParser.parseString(Files.readString(root.resolve(CATALOG))).getAsJsonObject().getAsJsonArray("cases")) cases.add(entry.getAsJsonObject());
        return cases;
    }
    public static String key(JsonObject row) { return row.get("advancementId").getAsString() + "#" + row.get("criterion").getAsString(); }
    public static String sha(byte[] bytes) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
    private static void require(boolean yes, String message) { if (!yes) throw new IllegalStateException(message); }
}
