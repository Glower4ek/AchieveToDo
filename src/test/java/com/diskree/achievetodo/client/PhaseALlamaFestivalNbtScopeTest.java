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
    @Test void onlyTheSixteenFrozenCarpetNbtPathsChangeAcrossTheEntireAdvancementArchive()throws Exception {
        var fixture=JsonParser.parseString(Files.readString(SCOPE_FIXTURE)).getAsJsonObject();assertEquals(FROZEN_SHA,fixture.get("frozenArchiveSha256").getAsString());
        var expected=fixture.getAsJsonObject("convertedAdvancementSha256");var actual=convertedAdvancements();assertEquals(expected.keySet(),actual.keySet());
        for(String path:expected.keySet())assertEquals(expected.get(path).getAsString(),shaText(actual.get(path).toString()),path);
        assertEquals(FROZEN_SHA,sha(ARCHIVE));
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
    @Test void bacapCopyRecordsMappingAndRejectsStaleMarkerWithoutChangingOtherPackMarkerRules(@TempDir Path dir)throws Exception {
        Path fresh=dir.resolve("fresh.zip");ExternalPackCompatibility.copyForWorld(ARCHIVE,fresh,ExternalPack.BACAP);
        Properties properties=new Properties();try(var zip=new ZipFile(fresh.toFile());var input=zip.getInputStream(zip.getEntry(MARKER))){properties.load(input);}
        assertEquals("compat_26_2_r15",properties.getProperty("version"));assertEquals("equipment.body",properties.getProperty("llamaCarpetNbtMapping"));assertTrue(ExternalPackCompatibility.isCompatibleWorldCopy(fresh,ExternalPack.BACAP));
        properties.remove("llamaCarpetNbtMapping");Path stale=dir.resolve("stale.zip");writeMarker(stale,properties);assertFalse(ExternalPackCompatibility.isCompatibleWorldCopy(stale,ExternalPack.BACAP));
        properties.setProperty("llamaCarpetNbtMapping","obsolete");Path wrong=dir.resolve("wrong.zip");writeMarker(wrong,properties);assertFalse(ExternalPackCompatibility.isCompatibleWorldCopy(wrong,ExternalPack.BACAP));
        properties.remove("llamaCarpetNbtMapping");properties.setProperty("fileName",ExternalPack.BACAP_HARDCORE.getFileName());properties.setProperty("sourceSha1",ExternalPack.BACAP_HARDCORE.getSha1());Path other=dir.resolve("other.zip");writeMarker(other,properties);assertTrue(ExternalPackCompatibility.isCompatibleWorldCopy(other,ExternalPack.BACAP_HARDCORE));assertEquals(FROZEN_SHA,sha(ARCHIVE));
    }
    private static void writeMarker(Path file,Properties properties)throws Exception {try(var out=new ZipOutputStream(Files.newOutputStream(file))){out.putNextEntry(new ZipEntry(MARKER));ByteArrayOutputStream bytes=new ByteArrayOutputStream();properties.store(bytes,null);out.write(bytes.toByteArray());out.closeEntry();}}
}
