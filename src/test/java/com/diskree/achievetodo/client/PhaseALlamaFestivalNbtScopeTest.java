package com.diskree.achievetodo.client;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;
import java.io.*;
import static org.junit.jupiter.api.Assertions.*;

class PhaseALlamaFestivalNbtScopeTest {
    private static final Path ARCHIVE=Path.of("reference/phase_a_preservation/files/final/bacap.zip");
    private static final Path BASELINE=Path.of("build/tmp/phase_a_resume_20260930/llama_pre_fix_converted_advancements.json");
    private static final Path SCOPE_FIXTURE=Path.of("src/test/resources/phase_a_certification/llama_authorized_fix_converted_advancement_sha256.json");
    private static final String FROZEN_SHA="8c72314535c5df7b4416bf0f38310371ec8aec537fde1445bdc820a3c9aada70";
    private static final String MARKER="achievetodo_compatibility/compat_26_2.properties";
    private static final Gson GSON=new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    public static void main(String[] args)throws Exception {
        if(args.length==1&&args[0].equals("fingerprints")){writeFingerprintFixture();return;}
        Path source=Path.of("src/main/java/com/diskree/achievetodo/client/ExternalPackCompatibility.java");
        assertEquals("0a339fc81b31ebff129914be40141155d4c8e474d5fd66dbf649a8ccae7a625f",sha(source));
        assertFalse(Files.exists(BASELINE),"Refuse baseline overwrite");
        Files.writeString(BASELINE,GSON.toJson(convertedAdvancements())+"\n");
    }
    private static String sha(Path path)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));}
    private static JsonObject convert(String raw)throws Exception {
        Method convert=ExternalPackCompatibility.class.getDeclaredMethod("convertJson",String.class);convert.setAccessible(true);Object result=convert.invoke(null,raw);Method text=result.getClass().getDeclaredMethod("text");text.setAccessible(true);return JsonParser.parseString((String)text.invoke(result)).getAsJsonObject();
    }
    private static JsonObject convertedAdvancements()throws Exception {
        assertEquals(FROZEN_SHA,sha(ARCHIVE));JsonObject output=new JsonObject();
        try(var zip=new ZipFile(ARCHIVE.toFile())){var entries=zip.entries();while(entries.hasMoreElements()){var entry=entries.nextElement();if(entry.getName().contains("/advancement/")&&entry.getName().endsWith(".json")){try(var input=zip.getInputStream(entry)){output.add(entry.getName(),convert(new String(input.readAllBytes(),StandardCharsets.UTF_8)));}}}}
        return output;
    }
    @Test void onlyTheSixteenFrozenCarpetNbtPathsChangeAcrossTheEntireAdvancementArchive() throws Exception {
        Path prePath = Path.of("src/test/resources/phase_a_certification/llama_pre_fix_converted_advancements.json");
        assertEquals("85abf71601b31edf88dcd510dea34496ca1d1d9399e801bfa373f9b5319f116c", sha(prePath));
        var before = JsonParser.parseString(Files.readString(prePath)).getAsJsonObject();
        var after = before.deepCopy(); assertEquals(1229, before.size());
        var criteria = after.getAsJsonObject("data/blazeandcave/advancement/animal/llama_festival.json").getAsJsonObject("criteria");
        assertEquals(16, criteria.size());
        for (var row : criteria.entrySet()) {
            var vehicle = row.getValue().getAsJsonObject().getAsJsonObject("conditions").getAsJsonArray("player").get(0)
                .getAsJsonObject().getAsJsonObject("predicate").getAsJsonObject("vehicle");
            assertEquals("{body_armor_item:{id:\"minecraft:" + row.getKey() + "\"}}", vehicle.get("nbt").getAsString());
            vehicle.addProperty("nbt", "{equipment:{body:{id:\"minecraft:" + row.getKey() + "\"}}}");
        }
        var fixture = JsonParser.parseString(Files.readString(SCOPE_FIXTURE)).getAsJsonObject();
        assertEquals(FROZEN_SHA, fixture.get("frozenArchiveSha256").getAsString());
        var expected = fixture.getAsJsonObject("convertedAdvancementSha256"); assertEquals(expected.keySet(), after.keySet());
        for (String path : after.keySet()) {
            assertEquals(expected.get(path).getAsString(), shaText(after.get(path).toString()), path);
            if (!path.equals("data/blazeandcave/advancement/animal/llama_festival.json")) assertEquals(before.get(path), after.get(path), path);
        }
        var restored = after.deepCopy(); restored.add("data/blazeandcave/advancement/animal/llama_festival.json", before.get("data/blazeandcave/advancement/animal/llama_festival.json"));
        assertEquals(before, restored, "No seventeenth change outside the sixteen NBT values");
        assertEquals(FROZEN_SHA, sha(ARCHIVE));
    }
    private static String shaText(String text)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));}
    private static void writeFingerprintFixture()throws Exception {
        assertFalse(Files.exists(SCOPE_FIXTURE),"Refuse verified fixture overwrite");
        var expected=JsonParser.parseString(Files.readString(BASELINE)).getAsJsonObject();var actual=convertedAdvancements();
        var criteria=expected.getAsJsonObject("data/blazeandcave/advancement/animal/llama_festival.json").getAsJsonObject("criteria");assertEquals(16,criteria.size());
        for(var entry:criteria.entrySet()){var vehicle=entry.getValue().getAsJsonObject().getAsJsonObject("conditions").getAsJsonArray("player").get(0).getAsJsonObject().getAsJsonObject("predicate").getAsJsonObject("vehicle");assertEquals("{body_armor_item:{id:\"minecraft:"+entry.getKey()+"\"}}",vehicle.get("nbt").getAsString());vehicle.addProperty("nbt","{equipment:{body:{id:\"minecraft:"+entry.getKey()+"\"}}}");}
        assertEquals(expected.keySet(),actual.keySet());for(String path:expected.keySet())assertEquals(expected.get(path),actual.get(path),path);
        assertEquals(FROZEN_SHA,sha(ARCHIVE));
        JsonObject fixture=new JsonObject(), hashes=new JsonObject();fixture.addProperty("frozenArchiveSha256",FROZEN_SHA);fixture.addProperty("originalProductionSha256","0a339fc81b31ebff129914be40141155d4c8e474d5fd66dbf649a8ccae7a625f");fixture.addProperty("authorizedProposalSha256","de0f5d2e05c6e04d06ca30fa9b50e6d866ae5f0fdbbc53fd40ddfa29d5611495");
        for(String path:expected.keySet())hashes.addProperty(path,shaText(expected.get(path).toString()));fixture.add("convertedAdvancementSha256",hashes);Files.writeString(SCOPE_FIXTURE,GSON.toJson(fixture)+"\n");
    }
    private static String legacy(String id){return "{body_armor_item:{id:\""+id+"\"}}";}
    private static JsonObject predicate(String type,String nbt)throws Exception {JsonObject entity=new JsonObject();entity.addProperty("type",type);entity.addProperty("nbt",nbt);JsonObject root=new JsonObject();root.add("entity",entity);return convert(root.toString());}
    @Test void nonLlamaArbitraryNearMissAndModernNbtRemainUnchangedAndConversionIsIdempotent()throws Exception {
        var cases=List.of(
            Map.entry("minecraft:zombie",legacy("minecraft:white_carpet")),
            Map.entry("#minecraft:llamas",legacy("minecraft:white_carpet")),
            Map.entry("#blazeandcave:llamas","{CustomName:'unrelated',CustomData:{x:1}}"),
            Map.entry("#blazeandcave:llamas","{body_armor_item:{id:\"minecraft:white_carpet\",count:1}}"),
            Map.entry("#blazeandcave:llamas","{body_armor_item:{id: \"minecraft:white_carpet\"}}"),
            Map.entry("#blazeandcave:llamas",legacy("white_carpet")),
            Map.entry("#blazeandcave:llamas",legacy("minecraft:chartreuse_carpet")),
            Map.entry("#blazeandcave:llamas","{equipment:{body:{id:\"minecraft:white_carpet\"}}}"));
        for(var entry:cases){var first=predicate(entry.getKey(),entry.getValue());assertEquals(entry.getValue(),first.getAsJsonObject("entity").get("nbt").getAsString());assertEquals(first,predicate(entry.getKey(),entry.getValue()));assertEquals(first,convert(first.toString()));}
        var first=predicate("#blazeandcave:llamas",legacy("minecraft:white_carpet"));assertEquals(first,convert(first.toString()));
    }
    @Test void bacapCopyRecordsMappingAndRejectsStaleMarkerWithoutChangingOtherPackMarkerRules(@TempDir Path dir) throws Exception {
        Path fresh = dir.resolve("historical.zip");
        ExternalPackCompatibility.copyForWorld(PhaseBPackTestFixtures.historical(), fresh, ExternalPack.BACAP);
        var properties = PhaseBPackTestFixtures.marker(fresh);
        assertEquals("compat_26_2_r19", properties.getProperty("version"));
        assertEquals(PhaseBPackTestFixtures.HISTORICAL_SHA1, properties.getProperty("sourceSha1"));
        assertEquals("equipment.body", properties.getProperty("llamaCarpetNbtMapping"));
        assertEquals("snake_case", properties.getProperty("raiderPredicateKeys"));
        assertEquals(PhaseBPackTestFixtures.ROOT_SHA1, properties.getProperty("rootOverrideSha1"));
        assertFalse(ExternalPackCompatibility.isCompatibleWorldCopy(fresh, ExternalPack.BACAP));
        assertFalse(ExternalPackCompatibility.isCurrentWorldPack(fresh, ExternalPack.BACAP));
        assertEquals(FROZEN_SHA, sha(ARCHIVE));
    }
    @Test
    void currentMainMappingFreshnessAndCompanionMarkerExemption(@TempDir Path dir) throws Exception {
        Path valid = PhaseBPackTestFixtures.currentCopy(dir);
        PhaseBPackTestFixtures.assertIsolatedMarkerNegatives(dir, valid);
        Path source = Path.of("reference/phase_a_preservation/files/final/bacap_terralith.zip");
        assertEquals("0b3cd387fe6e80ac6fe9a05b22091fce6abf6c38dd9656b3086629ec4031d3de", sha(source));
        assertTrue(ExternalPackCompatibility.isPinnedHistoricalSource(source, ExternalPack.BACAP_TERRALITH));
        Path target = dir.resolve("terralith-copy.zip");
        ExternalPackCompatibility.copyForWorld(source, target, ExternalPack.BACAP_TERRALITH);
        assertTrue(ExternalPackCompatibility.isCompatibleWorldCopy(target, ExternalPack.BACAP_TERRALITH));
        var properties = PhaseBPackTestFixtures.marker(target);
        assertEquals("compat_26_2_r19", properties.getProperty("version"));
        assertEquals(ExternalPack.BACAP_TERRALITH.getSha1(), properties.getProperty("sourceSha1"));
        assertEquals(ExternalPack.BACAP_TERRALITH.getFileName(), properties.getProperty("fileName"));
        assertFalse(properties.containsKey("llamaCarpetNbtMapping")); assertFalse(properties.containsKey("raiderPredicateKeys"));
        assertFalse(ExternalPackCompatibility.isCompatibleWorldCopy(target, ExternalPack.BACAP));
        assertEquals("0b3cd387fe6e80ac6fe9a05b22091fce6abf6c38dd9656b3086629ec4031d3de", sha(source));
    }

    @Test
    void currentConverterRetainsAcceptedLlamaAndRaiderArchiveSemantics() throws Exception {
        var report = PhaseBPackTestFixtures.json(Path.of("reference/phase_a_planning/final19/raider_production_output_scope.json"));
        var expected = report.getAsJsonObject("outputFingerprints"); assertEquals(1229, expected.size());
        Set<String> changed = new TreeSet<>();
        for (var row : expected.entrySet()) {
            var v = row.getValue().getAsJsonObject();
            if (!v.get("preFixOutputSha256").equals(v.get("productionOutputSha256"))) changed.add(row.getKey());
        }
        assertEquals(Set.of("data/blazeandcave/advancement/adventure/feeling_ill.json", "data/blazeandcave/advancement/monsters/dungeon_crawler.json", "data/minecraft/advancement/adventure/voluntary_exile.json"), changed);
        Set<String> actualIds = new TreeSet<>();
        try (var zip = new ZipFile(ARCHIVE.toFile())) {
            for (var entry : Collections.list(zip.entries())) {
                String path = entry.getName();
                if (!path.contains("/advancement/") || !path.endsWith(".json")) continue;
                actualIds.add(path);
                String raw; try (var in = zip.getInputStream(entry)) { raw = new String(in.readAllBytes(), StandardCharsets.UTF_8); }
                String actual = conversionText(raw);
                assertEquals(expected.getAsJsonObject(path).get("productionOutputSha256").getAsString(), shaText(actual), path);
                assertEquals(actual, conversionText(actual), path + " idempotence");
                if (changed.contains(path)) { assertFalse(actual.contains("\"hasRaid\"")); assertFalse(actual.contains("\"isCaptain\"")); }
                if (path.equals("data/blazeandcave/advancement/animal/llama_festival.json")) {
                    var criteria = JsonParser.parseString(actual).getAsJsonObject().getAsJsonObject("criteria"); assertEquals(16, criteria.size());
                    for (var row : criteria.entrySet()) {
                        var vehicle = row.getValue().getAsJsonObject().getAsJsonObject("conditions").getAsJsonArray("player").get(0).getAsJsonObject().getAsJsonObject("predicate").getAsJsonObject("vehicle");
                        assertEquals("{equipment:{body:{id:\"minecraft:" + row.getKey() + "\"}}}", vehicle.get("nbt").getAsString());
                    }
                }
            }
        }
        assertEquals(expected.keySet(), actualIds); assertEquals(FROZEN_SHA, sha(ARCHIVE));
    }

    private static String conversionText(String raw) throws Exception {
        Method convert = ExternalPackCompatibility.class.getDeclaredMethod("convertJson", String.class); convert.setAccessible(true);
        Object result = convert.invoke(null, raw); Method text = result.getClass().getDeclaredMethod("text"); text.setAccessible(true);
        return (String) text.invoke(result);
    }

    private static void writeMarker(Path file,Properties properties)throws Exception {try(var out=new ZipOutputStream(Files.newOutputStream(file))){out.putNextEntry(new ZipEntry(MARKER));ByteArrayOutputStream bytes=new ByteArrayOutputStream();properties.store(bytes,null);out.write(bytes.toByteArray());out.closeEntry();}}
}
