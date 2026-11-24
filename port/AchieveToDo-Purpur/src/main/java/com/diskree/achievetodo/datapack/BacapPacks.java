package com.diskree.achievetodo.datapack;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.zip.ZipFile;

/** Operator-provided content, never an embedded BACAP distribution or a counter engine. */
public final class BacapPacks {
    public static final List<String> IDS = List.of("bacap", "bacap_override");
    public static final String MARKER = "achievetodo_compatibility/compat_26_3.properties";
    private static volatile boolean discoveryReady;
    private static volatile long generation;

    private BacapPacks() {}

    public static void beginDiscovery() { discoveryReady = false; generation++; }
    public static void completeDiscovery() { discoveryReady = true; }
    public static boolean discoveryReady() { return discoveryReady; }
    public static long generation() { return generation; }

    public static Path directory() {
        String value = System.getProperty("achievetodo.bacap.directory", "");
        if (value.isBlank()) throw new IllegalStateException("BACAP_MISSING: set achievetodo.bacap.directory to the operator-prepared S3A profile");
        return Path.of(value).toAbsolutePath().normalize();
    }

    /** Same accepted canonical set/relative order as loaded-registry admission. */
    public static void validateEnabledPackSelection(java.util.Collection<String> selection) throws IOException {
        var enabled=List.copyOf(selection);
        var allowed=java.util.Set.of("vanilla","paper","AchieveToDo/embedded","file/bacap.zip","file/bacap_override.zip");
        if(enabled.size()!=allowed.size() || !new java.util.HashSet<>(enabled).equals(allowed)
            || enabled.indexOf("file/bacap.zip")>=enabled.indexOf("file/bacap_override.zip"))
            throw new IOException("BACAP_PACK_SET_OR_ORDER: "+enabled);
    }
    public static Map<String, Map<String, byte[]>> validate(Path directory) throws IOException {
        Map<String, Map<String, byte[]>> packs=new LinkedHashMap<>();
        for(String id:IDS)packs.put(id,readArchive(directory.resolve(id+".zip")));
        return validateEntries(packs);
    }
    /** Reads the actual staged native handles. Ownership stays with the native resource manager. */
    public static Map<String, Map<String, byte[]>> validateOpenedArchives(Map<String, java.util.zip.ZipFile> archives) throws IOException {
        if(!archives.keySet().equals(new java.util.HashSet<>(IDS)))throw new IOException("Incomplete staged BACAP archives");
        Map<String, Map<String, byte[]>> packs=new LinkedHashMap<>();
        for(String id:IDS)packs.put(id,readArchive(archives.get(id)));
        return validateEntries(packs);
    }

    private static Map<String, Map<String, byte[]>> validateEntries(Map<String, Map<String, byte[]>> packs) throws IOException {
        Properties pins = new Properties();
        try (var input = BacapPacks.class.getResourceAsStream("/bacap/s3a-profile.properties")) {
            if (input == null) throw new IOException("BACAP identity metadata missing");
            pins.load(input);
        }
        if (!packs.keySet().equals(new java.util.HashSet<>(IDS))) throw new IOException("Unexpected BACAP source IDs");
        for (String id : IDS) {
            Map<String, byte[]> entries = packs.get(id);
            if (!contentDigest(entries).equals(pins.getProperty(id + ".content-sha256"))) {
                throw new IOException("BACAP_INCOMPATIBLE: " + id + " content differs from canonical 0.2.0 S3A profile");
            }
        }
        byte[] marker = packs.get("bacap").get(MARKER);
        if (marker == null) throw new IOException("BACAP compatibility marker missing");
        Properties identity = new Properties();
        identity.load(new java.io.ByteArrayInputStream(marker));
        if (!"compat_26_3_r1".equals(identity.getProperty("version"))
            || !"18bb8fc57e606a1ad41b3f326c6f211e83d4fd2b".equals(identity.getProperty("sourceSha1"))) {
            throw new IOException("BACAP compatibility/source identity mismatch");
        }
        return packs;
    }

    public static Map<String, byte[]> readArchive(Path archive) throws IOException {
        try (ZipFile zip = new ZipFile(archive.toFile())) { return readArchive(zip); }
    }
    private static Map<String, byte[]> readArchive(ZipFile zip) throws IOException {
        Map<String, byte[]> entries = new TreeMap<>();
        long total = 0;
            var iterator = zip.entries();
            while (iterator.hasMoreElements()) {
                var entry = iterator.nextElement();
                if (entry.isDirectory()) continue;
                String name = entry.getName();
                if (name.startsWith("/") || name.contains("\\") || List.of(name.split("/")).contains("..")
                    || entries.size() >= 12000) throw new IOException("Invalid BACAP archive entry: " + name);
                byte[] bytes;
                try (var input = zip.getInputStream(entry)) { bytes = input.readNBytes(8 * 1024 * 1024 + 1); }
                total += bytes.length;
                if (bytes.length > 8 * 1024 * 1024 || total > 128 * 1024 * 1024 || entries.put(name, bytes) != null) {
                    throw new IOException("Duplicate or oversized BACAP archive entry: " + name);
                }
            }
        return entries;
    }

    /** Timestamp-bearing compatibility marker is checked separately. All content bytes are pinned. */
    public static String contentDigest(Map<String, byte[]> entries) {
        StringBuilder manifest = new StringBuilder();
        new TreeMap<>(entries).forEach((name, bytes) -> {
            if (!MARKER.equals(name)) manifest.append(name).append('\0').append(sha256(bytes)).append('\n');
        });
        return sha256(manifest.toString().getBytes(StandardCharsets.UTF_8));
    }

    public static Map<String, JsonObject> advancementDefinitions(Map<String, Map<String, byte[]>> packs) {
        Map<String, JsonObject> definitions = new LinkedHashMap<>();
        for (String id : IDS) packs.get(id).forEach((name, bytes) -> {
            if (name.startsWith("data/") && name.contains("/advancement/") && name.endsWith(".json")) {
                String[] parts = name.split("/", 4);
                String key = parts[1] + ":" + parts[3].substring(0, parts[3].length() - 5);
                definitions.put(key, JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject());
            }
        });
        return Map.copyOf(definitions);
    }

    private static String sha256(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
}
