package com.diskree.achievetodo.core;

import com.google.gson.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Separate deployment certification and live admission. A deployment pin is trusted operator input,
 * not a signature or an independently established approval. This class never issues certificates. */
public final class ProductGameplayAdmission {
    public enum Mode { DIAGNOSTIC, CERTIFICATION, PRODUCT }
    public static final Set<String> CHECKS = Set.of("source_identity", "mandatory_hooks",
        "bacap_pack_identity_order", "canonical_progression", "world_config", "bypass_closure",
        "protocol_compatibility", "persistence_reload", "real_runtime_certification",
        "independent_acceptance", "no_high_severity_blocker", "two_real_clients", "primary_integration");
    private final Mode mode;
    private final boolean certified;
    private final Map<Path,String> liveBindings;
    private static final long INTEGRITY_LEASE_NANOS = 250_000_000L;
    private long integrityCheckedAt;
    private boolean integrityValid = true;
    private int integrityValidationPasses;

    public static Mode mode(String value) {
        return switch (value) {
            case "diagnostic" -> Mode.DIAGNOSTIC;
            case "certification" -> Mode.CERTIFICATION;
            case "product" -> Mode.PRODUCT;
            default -> throw new IllegalArgumentException("ATD_UNKNOWN_GAMEPLAY_MODE");
        };
    }
    public ProductGameplayAdmission(Mode mode, Path artifact, Path certificate, String deploymentPin) throws Exception {
        this.mode = Objects.requireNonNull(mode);
        Map<Path,String> bindings = new LinkedHashMap<>();
        certified = mode == Mode.PRODUCT && validate(artifact, certificate, deploymentPin, bindings);
        liveBindings = Map.copyOf(bindings);
    }
    public Mode mode() { return mode; }
    public boolean operational(boolean lifecycle, boolean progression, boolean hooks, boolean exact,
                               boolean realProtocol, boolean groupScope, boolean spike) {
        if (!lifecycle || !progression || !hooks || !exact || !realProtocol || !groupScope) return false;
        return switch (mode) {
            case DIAGNOSTIC -> false;
            case CERTIFICATION -> spike;
            case PRODUCT -> !spike && certified && bindingsCurrent();
        };
    }
    public boolean productReady(boolean operational) { return mode == Mode.PRODUCT && certified && operational && bindingsCurrent(); }

    /** The capsule and artifact remain live prerequisites. Never repair changed evidence here. */
    private synchronized boolean bindingsCurrent() {
        if (!integrityValid) return false;
        if (integrityCheckedAt != 0 && System.nanoTime() - integrityCheckedAt < INTEGRITY_LEASE_NANOS) return true;
        return refreshIntegrity();
    }
    /** Forced before admission; periodic live polls share a maximum 250ms integrity lease.
     * Any failure latches closed until restart. This is independent of progression/hook polling. */
    public synchronized boolean refreshIntegrity() {
        if (!integrityValid) return false;
        integrityValidationPasses++;
        try {
            for (var entry : liveBindings.entrySet())
                if (!sha(regular(entry.getKey())).equals(entry.getValue())) { integrityValid=false;return false; }
            integrityCheckedAt=System.nanoTime();
            return true;
        } catch (Exception | LinkageError unavailable) { integrityValid=false;return false; }
    }
    int integrityValidationPasses() { return integrityValidationPasses; }
    private static boolean validate(Path artifact, Path certificate, String pin, Map<Path,String> bindings) throws Exception {
        if (artifact == null || certificate == null) throw new IllegalStateException("ATD_CERTIFICATE_REQUIRED");
        digest(pin);
        byte[] raw = regular(certificate);
        if (!sha(raw).equals(pin)) throw new IllegalStateException("ATD_CERTIFICATE_PIN_MISMATCH");
        JsonObject cert = object(raw);
        keys(cert, Set.of("formatVersion", "artifactSHA256", "sourceAuthority", "evidenceManifest", "independentAcceptance", "prerequisites"));
        if (!cert.get("formatVersion").isJsonPrimitive() || !cert.getAsJsonPrimitive("formatVersion").isNumber()
            || !cert.get("formatVersion").getAsString().equals("1")) throw new IllegalStateException("ATD_CERTIFICATE_VERSION");
        bindings.put(certificate.toAbsolutePath().normalize(), pin);
        String artifactHash = sha(regular(artifact));
        bindings.put(artifact.toAbsolutePath().normalize(), artifactHash);
        if (!artifactHash.equals(string(cert,"artifactSHA256"))) throw new IllegalStateException("ATD_CERTIFICATE_ARTIFACT_MISMATCH");
        Path root = certificate.toAbsolutePath().normalize().getParent();
        var authority = binding(cert.getAsJsonObject("sourceAuthority"), root);
        var manifest = binding(cert.getAsJsonObject("evidenceManifest"), root);
        var acceptance = binding(cert.getAsJsonObject("independentAcceptance"), root);
        if (Set.of(authority.path(), manifest.path(), acceptance.path(), certificate.toAbsolutePath().normalize()).size()!=4)
            throw new IllegalStateException("ATD_CERTIFICATE_CYCLE");
        for (Bound bound : List.of(authority, manifest, acceptance)) bindings.put(bound.path(),bound.sha256());
        JsonObject review = object(acceptance.bytes());
        keys(review, Set.of("status", "artifactSHA256", "sourceAuthoritySHA256", "evidenceManifestSHA256", "authorAgent", "reviewerAgent"));
        if (!string(review,"status").equals("PASS") || !string(review,"artifactSHA256").equals(artifactHash)
            || !string(review,"sourceAuthoritySHA256").equals(authority.sha256())
            || !string(review,"evidenceManifestSHA256").equals(manifest.sha256())
            || string(review,"authorAgent").equals(string(review,"reviewerAgent")))
            throw new IllegalStateException("ATD_INDEPENDENT_ACCEPTANCE_REQUIRED");
        // Evidence -> independent acceptance -> deployment certificate. The manifest cannot include
        // either successor file, and every required check must reference an actually hashed entry.
        JsonObject inventory = object(manifest.bytes());
        keys(inventory, Set.of("formatVersion", "artifactSHA256", "entries"));
        if (!inventory.get("formatVersion").isJsonPrimitive() || !inventory.getAsJsonPrimitive("formatVersion").isNumber()
            || !inventory.get("formatVersion").getAsString().equals("1") || !string(inventory,"artifactSHA256").equals(artifactHash))
            throw new IllegalStateException("ATD_MANIFEST_ARTIFACT_MISMATCH");
        Map<Path,String> evidence = new HashMap<>();
        for (JsonElement entry : inventory.getAsJsonArray("entries")) {
            Bound bound = binding(entry.getAsJsonObject(), manifest.path().getParent());
            if (Set.of(certificate.toAbsolutePath().normalize(), manifest.path(), acceptance.path()).contains(bound.path())
                || evidence.put(bound.path(), bound.sha256())!=null) throw new IllegalStateException("ATD_DUPLICATE_OR_CYCLIC_EVIDENCE");
        }
        bindings.putAll(evidence);
        if (!authority.sha256().equals(evidence.get(authority.path()))) throw new IllegalStateException("ATD_SOURCE_AUTHORITY_UNBOUND");
        JsonObject checks = cert.getAsJsonObject("prerequisites"); keys(checks, CHECKS);
        for (String check : CHECKS) {
            JsonObject row = checks.getAsJsonObject(check); keys(row, Set.of("status","evidence"));
            if (!string(row,"status").equals("PASS")) throw new IllegalStateException("ATD_PREREQUISITE_FAILED: "+check);
            Bound bound = binding(row.getAsJsonObject("evidence"), root);
            if (!bound.sha256().equals(evidence.get(bound.path()))) throw new IllegalStateException("ATD_EVIDENCE_UNBOUND: "+check);
        }
        long bytes=0;
        for(Path path:bindings.keySet())bytes=Math.addExact(bytes,Files.size(path));
        if(bindings.size()>256 || bytes>32L*1024*1024)throw new IllegalStateException("ATD_CAPSULE_RESOURCE_BOUND");
        return true;
    }
    private record Bound(Path path, String sha256, byte[] bytes) {}
    private static Bound binding(JsonObject value, Path root) throws Exception {
        keys(value,Set.of("path","sha256")); String expected=string(value,"sha256");digest(expected);
        String location=string(value,"path");
        Path relative=Path.of(location);
        if (relative.isAbsolute() || location.contains("\\") || location.contains("\0")
            || !location.equals(relative.toString().replace('\\','/'))
            || !relative.normalize().equals(relative) || relative.startsWith(".."))
            throw new IllegalStateException("ATD_EVIDENCE_PATH_ESCAPE");
        Path path=root.resolve(relative).toAbsolutePath().normalize();
        if (!path.startsWith(root.toAbsolutePath().normalize())) throw new IllegalStateException("ATD_EVIDENCE_PATH_ESCAPE");
        byte[] bytes=regular(path);
        if(!sha(bytes).equals(expected))throw new IllegalStateException("ATD_EVIDENCE_HASH_MISMATCH: "+path);
        return new Bound(path,expected,bytes);
    }
    private static byte[] regular(Path path) throws Exception {
        Path absolute=path.toAbsolutePath().normalize();
        for(Path p=absolute;p!=null;p=p.getParent())
            if(Files.isSymbolicLink(p) || Files.readAttributes(p,java.nio.file.attribute.BasicFileAttributes.class,LinkOption.NOFOLLOW_LINKS).isOther())
                throw new IllegalStateException("ATD_EVIDENCE_LINK");
        if(!Files.isRegularFile(absolute,LinkOption.NOFOLLOW_LINKS))throw new IllegalStateException("ATD_EVIDENCE_NOT_REGULAR");
        if(Files.size(absolute)>32L*1024*1024)throw new IllegalStateException("ATD_EVIDENCE_RESOURCE_BOUND");
        return Files.readAllBytes(absolute);
    }
    private static void digest(String value) {
        if(value==null || !value.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("ATD_INVALID_SHA256");
    }
    private static String sha(byte[] value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
    }
    private static void keys(JsonObject value,Set<String> expected) {
        if(value==null || !value.keySet().equals(expected))throw new IllegalStateException("ATD_CERTIFICATE_KEYS");
    }
    private static String string(JsonObject value,String key) {
        JsonElement element=value.get(key);
        if(element==null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString())throw new IllegalStateException("ATD_CERTIFICATE_VALUE");
        String result=element.getAsString();
        if(result.isBlank())throw new IllegalStateException("ATD_CERTIFICATE_BLANK");
        return result;
    }
    // Gson's tree parser normally accepts duplicate object keys. Reject them recursively before
    // constructing the tree so no second declaration can replace a failed prerequisite.
    private static JsonObject object(byte[] bytes) throws Exception {
        String text=StandardCharsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
            .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(bytes)).toString();
        try(var reader=new com.google.gson.stream.JsonReader(new java.io.StringReader(text))) {
            reader.setStrictness(Strictness.STRICT); scan(reader);
            if(reader.peek()!=com.google.gson.stream.JsonToken.END_DOCUMENT)throw new IllegalStateException("ATD_JSON_TRAILING");
        }
        return JsonParser.parseString(text).getAsJsonObject();
    }
    private static void scan(com.google.gson.stream.JsonReader reader) throws Exception {
        switch(reader.peek()) {
            case BEGIN_OBJECT -> { reader.beginObject();Set<String> names=new HashSet<>();
                while(reader.hasNext()) { if(!names.add(reader.nextName()))throw new IllegalStateException("ATD_DUPLICATE_JSON_KEY");scan(reader); } reader.endObject(); }
            case BEGIN_ARRAY -> {reader.beginArray();while(reader.hasNext())scan(reader);reader.endArray();}
            case STRING, NUMBER -> reader.nextString();
            case BOOLEAN -> reader.nextBoolean();
            case NULL -> reader.nextNull();
            default -> throw new IllegalStateException("ATD_INVALID_JSON");
        }
    }
}
