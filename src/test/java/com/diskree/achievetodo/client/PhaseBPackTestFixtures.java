package com.diskree.achievetodo.client;

import com.google.gson.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;
import static org.junit.jupiter.api.Assertions.*;

/** Authenticated test inputs and setup; conversion/admission always use production. */
public final class PhaseBPackTestFixtures {
    public static final Path CURRENT = Path.of("src/test/resources/phase_b_certification/bacap_1_21_pinned.zip");
    public static final Path HISTORICAL = Path.of("reference/phase_a_preservation/files/final/bacap.zip");
    public static final Path FROZEN_RU = Path.of("src/test/resources/phase_a_certification/frozen_runtime_ru_overlay.json");
    public static final Path FROZEN_INTERNAL = Path.of("src/test/resources/phase_a_certification/frozen_internal_resourcepacks.zip");
    public static final String CURRENT_SHA1 = "14da3f07b5467e8b59ffc0253fd8212c938cd739";
    public static final String HISTORICAL_SHA1 = "45b8bb0076bbf5b92fde7dc9590c6686937abbc0";
    public static final String ROOT_SHA1 = "cbc432be35d5525001872c430877541cfa1fcad6";
    public static final String MARKER = "achievetodo_compatibility/compat_26_2.properties";
    private static JsonObject localization;

    private PhaseBPackTestFixtures() {}

    public static String hash(byte[] bytes, String algorithm) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance(algorithm).digest(bytes));
    }

    public static JsonObject json(Path path) throws Exception {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }

    public static Path current() throws Exception {
        byte[] bytes = Files.readAllBytes(CURRENT);
        assertEquals(3135424, bytes.length);
        assertEquals(CURRENT_SHA1, hash(bytes, "SHA-1"));
        assertEquals("c71d1aa1a84dbe00a3f85a42144b46214c4669a3cccf07ff66631d28f16a99b2", hash(bytes, "SHA-256"));
        assertEquals(CURRENT_SHA1, ExternalPack.BACAP.getSha1());
        assertTrue(ExternalPackCompatibility.isCurrentWorldPack(CURRENT, ExternalPack.BACAP));
        return CURRENT;
    }

    public static Path historical() throws Exception {
        byte[] bytes = Files.readAllBytes(HISTORICAL);
        assertEquals(HISTORICAL_SHA1, hash(bytes, "SHA-1"));
        assertEquals("8c72314535c5df7b4416bf0f38310371ec8aec537fde1445bdc820a3c9aada70", hash(bytes, "SHA-256"));
        assertNotEquals(CURRENT_SHA1, HISTORICAL_SHA1);
        assertFalse(ExternalPackCompatibility.isCurrentWorldPack(HISTORICAL, ExternalPack.BACAP));
        return HISTORICAL;
    }

    public static Properties marker(Path path) throws Exception {
        Properties result = new Properties();
        try (var zip = new ZipFile(path.toFile())) {
            assertNotNull(zip.getEntry(MARKER));
            try (var in = zip.getInputStream(zip.getEntry(MARKER))) { result.load(in); }
        }
        return result;
    }

    public static Path currentCopy(Path dir) throws Exception {
        Files.createDirectories(dir);
        Path out = dir.resolve("current.zip");
        ExternalPackCompatibility.copyForWorld(current(), out, ExternalPack.BACAP);
        assertCurrentMarker(out);
        assertFalse(ExternalPackCompatibility.isPinnedHistoricalSource(out, ExternalPack.BACAP));
        assertTrue(ExternalPackCompatibility.isCompatibleWorldCopy(out, ExternalPack.BACAP));
        assertTrue(ExternalPackCompatibility.isCurrentWorldPack(out, ExternalPack.BACAP));
        return out;
    }

    public static void assertCurrentMarker(Path path) throws Exception {
        Properties p = marker(path);
        assertEquals("compat_26_2_r19", p.getProperty("version"));
        assertEquals("bacap.zip", p.getProperty("fileName"));
        assertEquals(CURRENT_SHA1, p.getProperty("sourceSha1"));
        assertEquals(ROOT_SHA1, p.getProperty("rootOverrideSha1"));
        assertEquals(ROOT_SHA1, ExternalPackCompatibility.currentRootOverrideSha1());
        assertEquals("equipment.body", p.getProperty("llamaCarpetNbtMapping"));
        assertEquals("snake_case", p.getProperty("raiderPredicateKeys"));
    }

    public static void writeMarker(Path target, String entry, Properties p) throws Exception {
        try (var zip = new ZipOutputStream(Files.newOutputStream(target))) {
            zip.putNextEntry(new ZipEntry(entry));
            var bytes = new java.io.ByteArrayOutputStream(); p.store(bytes, null);
            zip.write(bytes.toByteArray()); zip.closeEntry();
        }
    }

    public static void assertIsolatedMarkerNegatives(Path dir, Path valid) throws Exception {
        assertCurrentMarker(valid);
        assertTrue(ExternalPackCompatibility.isCurrentWorldPack(valid, ExternalPack.BACAP));
        Properties base = marker(valid);
        for (String field : List.of("version", "fileName", "sourceSha1", "rootOverrideSha1", "llamaCarpetNbtMapping", "raiderPredicateKeys")) {
            for (boolean missing : List.of(false, true)) {
                Properties changed = new Properties(); changed.putAll(base);
                if (missing) changed.remove(field); else changed.setProperty(field, "obsolete");
                Set<Object> delta = new HashSet<>();
                for (Object k : base.keySet()) if (!Objects.equals(base.get(k), changed.get(k))) delta.add(k);
                assertEquals(Set.of(field), delta);
                Path target = dir.resolve(field + (missing ? "-missing" : "-wrong") + ".zip");
                writeMarker(target, MARKER, changed);
                assertEquals(changed, marker(target));
                assertFalse(ExternalPackCompatibility.isCompatibleWorldCopy(target, ExternalPack.BACAP), field);
                assertFalse(ExternalPackCompatibility.isCurrentWorldPack(target, ExternalPack.BACAP), field);
            }
        }
        Path oldEntry = dir.resolve("old-entry.zip");
        writeMarker(oldEntry, "achievetodo_compatibility/phase_b_26_2.properties", base);
        assertFalse(ExternalPackCompatibility.isCurrentWorldPack(oldEntry, ExternalPack.BACAP));
    }

    /** Recompute current effective localization using accepted extractor functions,
     * not its historical Git-state precheck or a saved result as the actual value. */
    public static synchronized JsonObject currentLocalization() throws Exception {
        if (localization != null) return localization;
        Path dir = Path.of("build/tmp/phase_b_b16a1/current-localization"); Files.createDirectories(dir);
        JsonObject paths = new JsonObject();
        var acquisition = json(Path.of("reference/phase_b/b5_7_structural_reconciliation.json"))
            .getAsJsonObject("acquisition").getAsJsonObject("archiveIdentityValidation");
        for (var x : JsonParser.parseString(Files.readString(Path.of("build/tmp/phase_b_b5_6/converted_sources.json"))).getAsJsonArray()) {
            var row = x.getAsJsonObject(); String name = row.get("enum").getAsString();
            ExternalPack pack = ExternalPack.valueOf(name);
            Path input = name.equals("BACAP") ? current() : Path.of(row.get("source").getAsString());
            byte[] bytes = Files.readAllBytes(input);
            assertEquals(row.get("sourceSha256").getAsString(), hash(bytes, "SHA-256"));
            assertEquals(pack.getSha1(), hash(bytes, "SHA-1"));
            if (!name.equals("BACAP")) {
                String relative = Path.of("").toAbsolutePath().relativize(input).toString().replace('\\', '/');
                assertEquals(row.get("sourceSha256").getAsString(), acquisition.getAsJsonObject(relative).get("sha256").getAsString());
            }
            Path output = dir.resolve(name + ".zip");
            ExternalPackCompatibility.copyForWorld(input, output, pack);
            assertTrue(ExternalPackCompatibility.isCurrentWorldPack(output, pack));
            paths.addProperty(name, output.toString());
        }
        Path inputs = dir.resolve("inputs.json"); Files.writeString(inputs, paths.toString());
        String script = """
            import importlib.util,json,pathlib,sys,collections,hashlib,zipfile
            p=pathlib.Path('tools/phase_b/b7_localization_certification.py')
            authority=json.loads(pathlib.Path('reference/phase_b/b7_m2_current_consumer_set_refresh.json').read_bytes())
            assert hashlib.sha256(p.read_bytes()).hexdigest()==authority['tool']['afterSha256']
            spec=importlib.util.spec_from_file_location('b7',p);m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
            paths=json.loads(pathlib.Path(sys.argv[1]).read_bytes())
            converted={k:m.pack_zip(pathlib.Path(v),k) for k,v in paths.items()}
            ru=m.strict_dictionary((m.ROOT/m.RU_PATH).read_bytes())
            old=m.strict_dictionary(pathlib.Path('src/test/resources/phase_a_certification/frozen_runtime_ru_overlay.json').read_bytes())
            en=m.strict_dictionary(pathlib.Path('src/main/resources/assets/achievetodo/lang/en_us.json').read_bytes())
            atd=m.strict_dictionary(pathlib.Path('src/main/resources/assets/achievetodo/lang/ru_ru.json').read_bytes())
            assert set(en)==set(atd)
            index=m.load(m.ROOT/'.gradle-user-home/caches/fabric-loom/assets/indexes/26.2-32.json')
            vid=index['objects']['minecraft/lang/ru_ru.json']['hash'];vpath=m.ROOT/'.gradle-user-home/caches/fabric-loom/assets/objects'/vid[:2]/vid
            vb=vpath.read_bytes();assert hashlib.sha1(vb).hexdigest()==vid;vr=m.strict_dictionary(vb)
            mc=m.ROOT/'.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-84afe0508c/26.2/minecraft-merged-84afe0508c-26.2.jar'
            vanilla=m.pack_zip(mc,'VANILLA')
            with zipfile.ZipFile(mc) as z: ve=m.strict_dictionary(z.read('assets/minecraft/lang/en_us.json'))
            definitions={'main':['BACAP','bacap_override'],'hardcore':['BACAP','bacap_override','BACAP_HARDCORE','bacap_hardcore_override'],'terralith':['BACAP','bacap_override','TERRALITH','BACAP_TERRALITH','bacap_terralith_override'],'amplifiedNether':['BACAP','bacap_override','AMPLIFIED_NETHER','BACAP_AMPLIFIED_NETHER','bacap_amplified_nether_override'],'nullscape':['BACAP','bacap_override','NULLSCAPE','BACAP_NULLSCAPE','bacap_nullscape_override']}
            union=collections.defaultdict(list);views={};ids=set()
            for view,layers in definitions.items():
                effective=dict(vanilla)
                for layer in layers+['bacap_rewards_item','bacap_rewards_experience','bacap_rewards_trophy','bacap_cooperative_mode']:effective.update(converted[layer] if layer in converted else m.builtin(layer))
                keys=m.extract(effective);views[view]=keys
                for k,contexts in keys.items():union[k].extend(dict(c,combination=view) for c in contexts)
                for path in effective:
                    parts=path.split('/')
                    if len(parts)>3 and parts[0]=='data' and parts[2]=='advancement' and path.endswith('.json'):ids.add(parts[1]+':'+ '/'.join(parts[3:])[:-5])
            for k in en:union[k].append({'owner':'ATD_DICTIONARY_DECLARATION','path':'src/main/resources/assets/achievetodo/lang/en_us.json'})
            def provider(k):return 'ATD_RU_DICTIONARY' if k in atd else 'MINECRAFT_RU_OVERLAY' if k in ru else 'VANILLA_26_2_RU' if k in vr else 'INTENTIONALLY_LANGUAGE_NEUTRAL' if k in m.NEUTRAL else 'MISSING'
            providers={k:provider(k) for k in union};counts=dict(collections.Counter(providers.values()))
            rows=[{'key':k,'provider':providers[k],'consumers':union[k]} for k in sorted(union)]
            checked=0;mismatches=[]
            for k,pr in providers.items():
                if pr in ('MISSING','INTENTIONALLY_LANGUAGE_NEUTRAL'):continue
                english=en[k] if pr=='ATD_RU_DICTIONARY' else ve.get(k,k)
                value=atd[k] if pr=='ATD_RU_DICTIONARY' else ru[k] if pr=='MINECRAFT_RU_OVERLAY' else vr[k]
                checked+=1
                if m.formats(english)!=m.formats(value):mismatches.append(k)
            changed=sorted(k for k in ru if k not in old or ru[k]!=old[k])
            provenance=m.provenance_certification(ru,changed,rows)
            coverage={v:{'required':len(keys),'resolved':sum(provider(k)!='MISSING' for k in keys),'missing':sorted(k for k in keys if provider(k)=='MISSING')} for v,keys in views.items()}
            handoff=m.load(m.PHASE/'b4_localization_handoff.json')
            selected={k:provider(k) for k in handoff['selectedCompanionKeys']}
            print(json.dumps({'totalRequirements':len(union),'providerCounts':counts,'effectiveCoverage':coverage,'placeholderChecked':checked,'placeholderMismatches':mismatches,'dictionaryCount':len(ru),'provenance':provenance,'requirements':rows,'selectedCompanionCoverage':selected,'advancementIds':sorted(ids)},ensure_ascii=False))
            """;
        Path log = dir.resolve("calculation.json"), err = dir.resolve("calculation.stderr");
        var process = new ProcessBuilder("python", "-B", "-X", "utf8", "-c", script, inputs.toString())
            .redirectOutput(log.toFile()).redirectError(err.toFile()).start();
        assertEquals(0, process.waitFor(), () -> { try { return Files.readString(err); } catch (Exception e) { return e.toString(); } });
        localization = json(log);
        return localization;
    }
}
