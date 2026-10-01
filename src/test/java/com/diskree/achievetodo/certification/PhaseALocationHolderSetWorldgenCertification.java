package com.diskree.achievetodo.certification;

import com.google.gson.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.ZipFile;

/** Frozen victim/source contexts, with one selected native kill per requirement group. */
public final class PhaseALocationHolderSetWorldgenCertification {
    public static final String FAMILY = "LOCATION_HOLDERSET_WORLDGEN";
    public static final String SOURCE = "PhaseALocationHolderSetWorldgenGameTest";
    public static final Path CATALOG = Path.of("src/test/resources/phase_a_certification/location_holder_set_worldgen_case_catalog.json");
    public static final Path PERSISTENT = Path.of("src/test/resources/phase_a_certification/location_holder_set_worldgen_execution_evidence.json");
    public static final String BOUNDARY = "ServerPlayer.doTick->CriteriaTriggers.LOCATION->SimpleCriterionTrigger.trigger";
    public static final Map<String, String> FROZEN = Map.ofEntries(
        Map.entry("blazeandcave:adventure/now_youre_thinking_with_portals", "6891f8e427b64a8df7bf7866d9fe0d42b5f08180401b1bb556a7516eb452451e"),
        Map.entry("blazeandcave:animal/llama_festival", "78b647932d1103ae6ee670cf0915d0bea7ad3aad6dbe719d2e14d4fa9af55e55"),
        Map.entry("blazeandcave:biomes/smooth_operator", "574da0967da83cbc8b7e8d6bed5683deb4374ede52f6189be139c874ae437e6a"),
        Map.entry("blazeandcave:biomes/titanic", "a86570bd8498348a12304066298373610f549f335c7cb39ccb07e5ad6faa0642"),
        Map.entry("blazeandcave:challenges/explorer_of_worlds", "06addbea653068a0a3914d0eadafc2aabb8982759ced56a65085e37335d47bfd"),
        Map.entry("blazeandcave:enchanting/silent_but_deadly", "8c70031b4b560c6b85a8041cfd3f396b788f5ec5f162b19948fdc4400901d27c"),
        Map.entry("blazeandcave:mining/spelunker", "86a4c911f62e025ea510e925ab0b20df9ee4fc1975b9f99c988bc34bfef91515"),
        Map.entry("blazeandcave:nether/from_whence_it_came", "99d831706ba657fed6eecf7d261c5ebb1b8773a7df1589878cd7151d9f33b19f"),
        Map.entry("blazeandcave:nether/inception", "9e4e01f8212ff0a4a7f4fcba35e13bc0e33e088afe1233f75307ae3e6b047c48"),
        Map.entry("blazeandcave:nether/instant_mining", "17d528d85a44b844fc58d3feffea5571b4c085d0f8e431c5a138b4de1b653e90"),
        Map.entry("blazeandcave:nether/ludicrous_speed", "6e4619843d7664bb7d2cfe94524b09105895dc56f4fa16bb40f108c6db434597"),
        Map.entry("blazeandcave:nether/soul_runnings", "a2d3606067eaed21c786ff603c355f04fe88c92128c252abac8ee67227e0dfb3"),
        Map.entry("blazeandcave:nether/stepping_on_legos", "422e5f23fcd8acec253426d1f11d8d52a47f0e56fe579983ceae2856b7dc5955"));
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private PhaseALocationHolderSetWorldgenCertification() { }
    public static void main(String[] args) throws Exception { Path root = Path.of(args[0]).toAbsolutePath(); Files.writeString(root.resolve(CATALOG), generate(root)); }
    public static void validate(Path root) throws Exception { require(generate(root).equals(Files.readString(root.resolve(CATALOG))), "Frozen kill catalog mismatch"); }
    public static String generate(Path root) throws Exception {
        Path archive = root.resolve("reference/phase_a_preservation/files/final/bacap.zip");
        require(sha(Files.readAllBytes(archive)).equals("8c72314535c5df7b4416bf0f38310371ec8aec537fde1445bdc820a3c9aada70"), "Frozen BACAP changed");
        JsonObject catalog = new JsonObject(); catalog.addProperty("snapshot", "phase_a_location_holder_set_worldgen_case_catalog"); catalog.addProperty("family", FAMILY); catalog.addProperty("source", SOURCE);
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
                    require(criterion.get("trigger").getAsString().equals("minecraft:location"), "Wrong location trigger");
                    JsonObject conditions = criterion.getAsJsonObject("conditions"); JsonObject selectedPredicate = new JsonObject();
                    for (var term : conditions.getAsJsonArray("player")) {
                        var condition = term.getAsJsonObject(); String type = condition.get("condition").getAsString();
                        if (type.equals("minecraft:inverted")) continue;
                        if (type.equals("minecraft:any_of")) {
                            var terms = condition.getAsJsonArray("terms"); condition = terms.get(0).getAsJsonObject();
                            if (entry.getKey().endsWith("/from_whence_it_came")) for (var alternative : terms) if (alternative.getAsJsonObject().getAsJsonObject("predicate").getAsJsonObject("location").get("structures").getAsString().equals("minecraft:ruined_portal_nether")) condition = alternative.getAsJsonObject();
                        }
                        require(condition.get("condition").getAsString().equals("minecraft:entity_properties"), "Unknown location context");
                        for (var component : condition.getAsJsonObject("predicate").entrySet()) { require(!selectedPredicate.has(component.getKey()), "Duplicate context key"); selectedPredicate.add(component.getKey(), component.getValue().deepCopy()); }
                    }
                    JsonObject row = new JsonObject(); row.addProperty("advancementId", entry.getKey()); row.addProperty("criterion", selected); row.addProperty("requirementGroup", i);
                    if (selectedPredicate.has("location") && selectedPredicate.getAsJsonObject("location").has("biomes")) {
                        String biome=selectedPredicate.getAsJsonObject("location").get("biomes").getAsString();
                        if (biome.startsWith("#")) {
                            String tagPath="data/"+biome.substring(1).replace(":","/tags/worldgen/biome/")+".json";
                            byte[] tagBytes;try(var input=zip.getInputStream(zip.getEntry(tagPath))){tagBytes=input.readAllBytes();}
                            var tag=JsonParser.parseString(new String(tagBytes,StandardCharsets.UTF_8)).getAsJsonObject();
                            require(tag.getAsJsonArray("values").size()==1,"Unexpected frozen biome tag membership");
                            row.addProperty("selectedBiome",tag.getAsJsonArray("values").get(0).getAsString());
                            row.addProperty("biomeTagSource",tagPath);row.addProperty("biomeTagSha256",sha(tagBytes));row.add("frozenBiomeTag",tag);
                        } else row.addProperty("selectedBiome",namespaced(biome));
                    }
                    if (selectedPredicate.has("location") && selectedPredicate.getAsJsonObject("location").has("block")) {
                        var blocks=selectedPredicate.getAsJsonObject("location").getAsJsonObject("block").get("blocks");
                        require(blocks.getAsString().equals("#minecraft:non_underwater_blocks"), "Unexpected inside-block selector");
                        String blockTagPath="data/minecraft/tags/block/non_underwater_blocks.json";
                        byte[] tagBytes; try(var in=zip.getInputStream(zip.getEntry(blockTagPath))) {tagBytes=in.readAllBytes();}
                        var tag=JsonParser.parseString(new String(tagBytes,StandardCharsets.UTF_8)).getAsJsonObject();
                        require(tag.getAsJsonArray("values").get(0).getAsString().equals("minecraft:air"),"Frozen air member changed");
                        row.addProperty("selectedLocationBlock","minecraft:air");row.addProperty("blockTagSource",blockTagPath);row.addProperty("blockTagSha256",sha(tagBytes));row.add("frozenBlockTag",tag);
                    }
                    if (selectedPredicate.has("vehicle")) {
                        var vehicle=selectedPredicate.getAsJsonObject("vehicle");
                        require(vehicle.get("type").getAsString().equals("#blazeandcave:llamas")
                            && vehicle.get("nbt").getAsString().equals("{body_armor_item:{id:\"minecraft:"+selected+"\"}}"),"Unexpected frozen llama NBT form");
                        row.addProperty("nativeVehicleNbt","{equipment:{body:{id:\"minecraft:"+selected+"\"}}}");
                        row.addProperty("productionMigration","EXACT_FROZEN_LLAMA_CARPET_NBT_TO_EQUIPMENT_BODY");
                    }
                    row.add("alternatives", alternatives.deepCopy()); row.add("frozenConditions", conditions.deepCopy()); row.add("selectedPredicate", selectedPredicate); cases.add(row);
                }
            }
        }
        require(criteriaCount == 94 && cases.size() == 93, "Location family cardinality changed"); catalog.addProperty("criteriaCount", criteriaCount); catalog.addProperty("requirementGroupCount", cases.size()); catalog.add("sources", sources); catalog.add("cases", cases); return GSON.toJson(catalog) + "\n";
    }
    public static String namespaced(String id) { return id.contains(":") ? id : "minecraft:" + id; }
    public static List<JsonObject> cases(Path root) throws Exception { validate(root); List<JsonObject> cases = new ArrayList<>(); for (var row : JsonParser.parseString(Files.readString(root.resolve(CATALOG))).getAsJsonObject().getAsJsonArray("cases")) cases.add(row.getAsJsonObject()); return cases; }
    public static String key(JsonObject row) { return row.get("advancementId").getAsString() + "#" + row.get("criterion").getAsString(); }
    public static String sha(byte[] bytes) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
    private static void require(boolean yes, String message) { if (!yes) throw new IllegalStateException(message); }
}
