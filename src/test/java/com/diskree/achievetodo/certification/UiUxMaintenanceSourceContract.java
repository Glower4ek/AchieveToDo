package com.diskree.achievetodo.certification;

import com.diskree.achievetodo.R3SourceOracle;
import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.client.PhaseBPackTestFixtures;
import com.diskree.achievetodo.client.gui.AbilityPresentationState;
import com.diskree.achievetodo.client.gui.AdvancementsTabType;
import com.diskree.achievetodo.tracking.*;
import com.google.gson.*;
import net.minecraft.resources.Identifier;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Stream;

/** Current-source successor, never a retroactive historical byte certificate.
 * Reviewed code is the trust anchor; environment variables only select it. */
public final class UiUxMaintenanceSourceContract {
    public static final String AUTHORITY = "ATD_UI_UX_MAINTENANCE_V1";
    static final String MANIFEST = "tools/certification/authority/ui-ux-maintenance-v1.json";
    static final String MANIFEST_SHA256 = "3f04f19ba716eaa9cb8515ef45564bf4b23417005262070b5517d42ebdc59072";
    static final String R9 = "tools/certification/authority/r9-source-oracle-v1.json";
    static final String R9_SHA256 = "7d57ee1bbd9ecb1b903741dd4335b8055f426278aa5910e89908a8207f986072";
    static final String PORT = "port/AchieveToDo-Purpur/src/main/";
    static final String PROGRESS = "src/main/java/com/diskree/achievetodo/injection/mixin/client/AdvancementProgressMixin.java";
    static final String SCREEN = "src/main/java/com/diskree/achievetodo/injection/mixin/client/AdvancementsScreenMixin.java";
    private static final Set<String> UI_EXCEPTIONS = Set.of(
        "src/main/java/com/diskree/achievetodo/client/gui/AdvancementsTabType.java",
        "src/main/java/com/diskree/achievetodo/injection/mixin/main/PlacedAdvancementMixin.java");
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();
    private static final String GATE_B_OWNER_B_CAPSULE_SHA256 = "39116d736a8c352aca6a05453474f0fae965cc300a075fdabdbf266523a3f9f9"; // No Owner B; not an environment override.

    private static final String UI07_SHA256 = "57e44e6d13df96fb427c4509576fbcb87061eeef377d47d50d070016acdafe2e";
    private static final String UI07_MANIFEST = "tools/certification/authority/ui07-successor-v1.json";
    private static JsonObject ui07(Path root) throws Exception {
        String selector = System.getenv("ACHIEVETODO_UI07_AUTHORITY"), file = System.getenv("ACHIEVETODO_UI07_AUTHORITY_FILE");
        if (selector == null && file == null) return null;
        require("ATD_UI07_SCOPED_SUCCESSOR_V1".equals(selector) && file != null && Path.of(file).equals(ROOT.resolve(UI07_MANIFEST)), "UI07 unknown/orphan/path selector");
        JsonObject m = pinnedJson(root, UI07_MANIFEST, UI07_SHA256);
        require(m.get("authority").getAsString().equals("ATD_UI07_SCOPED_SUCCESSOR_V1") && m.get("state").getAsString().equals("AUTHORIZED_SCOPED_SUCCESSOR"), "UI07 identity/state");
        return m;
    }
    private static final String GAMEPLAY_MANIFEST = "tools/certification/authority/gameplay-successor-v1.json";
    private static final String GAMEPLAY_V2_MANIFEST = "tools/certification/authority/gameplay-successor-v2.json";
    private static final String GAMEPLAY_V3_MANIFEST = "tools/certification/authority/gameplay-successor-v3.json";
    private static final String GAMEPLAY_V4_MANIFEST = "tools/certification/authority/gameplay-successor-v4.json";
    private static final String GAMEPLAY_V5_MANIFEST = "tools/certification/authority/gameplay-successor-v5.json";
    private static final String JUNIT_REPAIR_MANIFEST = "tools/certification/authority/gameplay-junit-overlay-v1.json";
    private static final String JUNIT_REPAIR_SELECTOR = "ACHIEVETODO_GAMEPLAY_JUNIT_REPAIR";
    private static final String JUNIT_REPAIR_FILE = "ACHIEVETODO_GAMEPLAY_JUNIT_REPAIR_FILE";
    private static final String JUNIT_FIXTURE_SUCCESSOR_MANIFEST = "tools/certification/authority/gameplay-junit-fixture-successor-v1.json";
    private static final String JUNIT_FIXTURE_SUCCESSOR_V2_MANIFEST = "tools/certification/authority/gameplay-junit-fixture-successor-v2.json";
    private static final String JUNIT_FIXTURE_SUCCESSOR_SELECTOR = "ACHIEVETODO_GAMEPLAY_JUNIT_FIXTURE_AUTHORITY";
    private static final String JUNIT_FIXTURE_SUCCESSOR_FILE = "ACHIEVETODO_GAMEPLAY_JUNIT_FIXTURE_AUTHORITY_FILE";
    private static final String JUNIT_FIXTURE_SUCCESSOR_DIGEST = "ACHIEVETODO_GAMEPLAY_JUNIT_FIXTURE_AUTHORITY_SHA256";
    private static final String JUNIT_FIXTURE_SUCCESSOR_ID = "ATD_GAMEPLAY_JUNIT_FIXTURE_SOURCE_V1";
    private static final String JUNIT_FIXTURE_SUCCESSOR_V2_ID = "ATD_GAMEPLAY_JUNIT_FIXTURE_SOURCE_V2";
    private static final String UIUX_SOURCE_CONTRACT = "src/test/java/com/diskree/achievetodo/certification/UiUxMaintenanceSourceContract.java";
    private static final String STAGE1_MANIFEST = "tools/certification/authority/stage1-test-source-v2.json";
    static boolean stage1Selected() { return System.getenv("ATD_STAGE1_AUTHORITY")!=null || System.getenv("ATD_STAGE1_AUTHORITY_FILE")!=null || System.getenv("ATD_STAGE1_AUTHORITY_SHA256")!=null; }
    private static JsonObject stage1(Path root) throws Exception {
        if(!stage1Selected()) return null;
        require("ATD_STAGE1_R5_TEST_SOURCE_V2".equals(System.getenv("ATD_STAGE1_AUTHORITY"))
            && ROOT.resolve(STAGE1_MANIFEST).toString().equals(System.getenv("ATD_STAGE1_AUTHORITY_FILE"))
            && "ATD_PURPUR_GAMEPLAY_SOURCE_V5".equals(System.getenv("ACHIEVETODO_GAMEPLAY_SOURCE_AUTHORITY")),"Stage1 unknown/orphan/parent selector");
        String hash=System.getenv("ATD_STAGE1_AUTHORITY_SHA256");require(hash!=null && hash.matches("[0-9a-f]{64}"),"Stage1 manifest digest");
        JsonObject layer=pinnedJson(root,STAGE1_MANIFEST,hash);
        require("ATD_STAGE1_R5_TEST_SOURCE_V2".equals(layer.get("authority").getAsString())
            && "BOUNDED_TEST_SOURCE_SUCCESSOR_ONLY".equals(layer.get("state").getAsString()) && !layer.get("PRODUCT_GAMEPLAY_READY").getAsBoolean()
            && "225c0d70f862db2260648a432526465b969fa958d10d92c60e696ddb7f496294".equals(layer.getAsJsonObject("parentV5").get("sha256").getAsString()),"Stage1 source-only parent boundary");
        for(var binding:layer.getAsJsonObject("codeBindings").entrySet())checkFile(root,binding.getKey(),binding.getValue().getAsString());
        JsonObject review=layer.getAsJsonObject("independentReview");pinnedJson(root,review.get("path").getAsString(),review.get("sha256").getAsString());
        return layer;
    }
    private static boolean junitRepair(Path root) throws Exception {
        String selector=System.getenv(JUNIT_REPAIR_SELECTOR),file=System.getenv(JUNIT_REPAIR_FILE);
        if(selector==null && file==null) return false;
        require("ATD_GAMEPLAY_JUNIT_REPAIR_V1".equals(selector) && ROOT.resolve(JUNIT_REPAIR_MANIFEST).toString().equals(file),"JUnit unknown/orphan/noncanonical selector");
        String gameplaySelector=System.getenv("ACHIEVETODO_GAMEPLAY_SOURCE_AUTHORITY");
        require(("ATD_PURPUR_GAMEPLAY_SOURCE_V4".equals(gameplaySelector) || "ATD_PURPUR_GAMEPLAY_SOURCE_V5".equals(gameplaySelector)) && "ATD_DISTRIBUTION_B_V2_RAW_CAPTURE_V1".equals(System.getenv("ACHIEVETODO_GATE_B_AUTHORITY")),"JUnit requires V4/V5 and GateB");
        for(Path actual:List.of(ROOT,root)) {
            pinnedJson(actual,JUNIT_REPAIR_MANIFEST,"5bec165c457465e7c9fd61c21689f8cffa7238289826c6f658a12310ef284a5f");
            checkFile(actual,"tools/build/gameplay_junit_overlay_v1.py","3bb547ce24c9b0bbf05e889d3c1eb6865477b93ce5601bc8334b474d68519b85");
            checkFile(actual,"tools/build/distribution_b_v2_contract.py","f2650bcca64212a0bbeb3e38990f301f8becf74ffe472139e3d6992949621a76");
        }
        return true;
    }
    private static JsonObject junitFixtureSuccessor(Path root) throws Exception {
        String selector=System.getenv(JUNIT_FIXTURE_SUCCESSOR_SELECTOR), file=System.getenv(JUNIT_FIXTURE_SUCCESSOR_FILE), digest=System.getenv(JUNIT_FIXTURE_SUCCESSOR_DIGEST);
        if(selector==null && file==null && digest==null) return null;
        boolean v2=JUNIT_FIXTURE_SUCCESSOR_V2_ID.equals(selector);
        String manifest=v2 ? JUNIT_FIXTURE_SUCCESSOR_V2_MANIFEST : JUNIT_FIXTURE_SUCCESSOR_MANIFEST;
        String expectedId=v2 ? JUNIT_FIXTURE_SUCCESSOR_V2_ID : JUNIT_FIXTURE_SUCCESSOR_ID;
        String gameplaySelector=System.getenv("ACHIEVETODO_GAMEPLAY_SOURCE_AUTHORITY");
        require(expectedId.equals(selector) && ROOT.resolve(manifest).toString().equals(file) && digest!=null && digest.matches("[0-9a-f]{64}"),"JUnit fixture successor unknown/orphan/noncanonical selector");
        require("ATD_GAMEPLAY_JUNIT_REPAIR_V1".equals(System.getenv(JUNIT_REPAIR_SELECTOR))
            && (v2 ? "ATD_PURPUR_GAMEPLAY_SOURCE_V5" : "ATD_PURPUR_GAMEPLAY_SOURCE_V4").equals(gameplaySelector)
            && "ATD_DISTRIBUTION_B_V2_RAW_CAPTURE_V1".equals(System.getenv("ACHIEVETODO_GATE_B_AUTHORITY")),"JUnit fixture successor requires its exact gameplay parent and GateB");
        JsonObject successor=pinnedJson(root,manifest,digest);
        JsonObject v4=pinnedJson(root,GAMEPLAY_V4_MANIFEST,"e069f6109cbbf2dd679859b10e814d987e77ba8c9c9338899e13be9b4abcce53");
        List<String> v4Entries=new ArrayList<>(v4.getAsJsonObject("archiveEntries").keySet()); Collections.sort(v4Entries);
        List<String> v4Declared=new ArrayList<>(); successor.getAsJsonArray("v4ArchiveEntries").forEach(v -> v4Declared.add(v.getAsString()));
        require(v4Declared.equals(v4Entries),"JUnit fixture successor exact V4 archive inventory");
        JsonObject parents=successor.getAsJsonObject("parents");
        if(!v2) {
            require(successor.keySet().equals(Set.of("schema","authority","state","parents","sourceBeforeSHA256","sourcePostimage","v4ArchiveEntries","scope","ownerBApproval","PRODUCT_GAMEPLAY_READY"))
                && successor.get("schema").getAsInt()==1 && JUNIT_FIXTURE_SUCCESSOR_ID.equals(successor.get("authority").getAsString())
                && "AUTHORIZED_BOUNDED_TEST_FIXTURE_SUCCESSOR_ONLY".equals(successor.get("state").getAsString()),"JUnit fixture V1 identity/schema");
            require(parents.keySet().equals(Set.of("junitOverlayV1","gameplaySourceV4"))
                && parents.getAsJsonObject("junitOverlayV1").get("path").getAsString().equals(JUNIT_REPAIR_MANIFEST)
                && parents.getAsJsonObject("junitOverlayV1").get("sha256").getAsString().equals("5bec165c457465e7c9fd61c21689f8cffa7238289826c6f658a12310ef284a5f")
                && parents.getAsJsonObject("gameplaySourceV4").get("path").getAsString().equals(GAMEPLAY_V4_MANIFEST)
                && parents.getAsJsonObject("gameplaySourceV4").get("sha256").getAsString().equals("e069f6109cbbf2dd679859b10e814d987e77ba8c9c9338899e13be9b4abcce53"),"JUnit fixture V1 immutable parents");
            require("65f7805efe8c8f44cb14cf2832fa33dc43f13766eed014543407186cf610e124".equals(successor.get("sourceBeforeSHA256").getAsString()),"JUnit fixture V1 predecessor source");
            require("V4 JUnit fixture closure only; copy exact V4 archiveEntries for disposable source validation.".equals(successor.get("scope").getAsString()),"JUnit fixture V1 scope");
        } else {
            require(successor.keySet().equals(Set.of("schema","authority","state","parents","sourceBeforeSHA256","sourcePostimage","v4ArchiveEntries","v5PredecessorArchiveEntries","scope","ownerBApproval","PRODUCT_GAMEPLAY_READY"))
                && successor.get("schema").getAsInt()==2 && JUNIT_FIXTURE_SUCCESSOR_V2_ID.equals(successor.get("authority").getAsString())
                && "AUTHORIZED_BOUNDED_TEST_FIXTURE_SUCCESSOR_ONLY".equals(successor.get("state").getAsString()),"JUnit fixture V2 identity/schema");
            require(parents.keySet().equals(Set.of("junitOverlayV1","junitFixtureSuccessorV1","gameplaySourceV4"))
                && parents.getAsJsonObject("junitOverlayV1").get("path").getAsString().equals(JUNIT_REPAIR_MANIFEST)
                && parents.getAsJsonObject("junitOverlayV1").get("sha256").getAsString().equals("5bec165c457465e7c9fd61c21689f8cffa7238289826c6f658a12310ef284a5f")
                && parents.getAsJsonObject("junitFixtureSuccessorV1").get("path").getAsString().equals(JUNIT_FIXTURE_SUCCESSOR_MANIFEST)
                && parents.getAsJsonObject("junitFixtureSuccessorV1").get("sha256").getAsString().equals("74650d947ccc695af15fe6985cac53638dc8d0b102ca742b5be8c131dcaad3d4")
                && parents.getAsJsonObject("gameplaySourceV4").get("path").getAsString().equals(GAMEPLAY_V4_MANIFEST)
                && parents.getAsJsonObject("gameplaySourceV4").get("sha256").getAsString().equals("e069f6109cbbf2dd679859b10e814d987e77ba8c9c9338899e13be9b4abcce53"),"JUnit fixture V2 immutable parents");
            require("972d84da4d56ae72a09d7d1032ef6d799b97bd3edea59218b81ca4aceee14b4d".equals(successor.get("sourceBeforeSHA256").getAsString()),"JUnit fixture V2 predecessor source");
            JsonObject gameplay=gameplay(root);
            require(gameplay!=null && "ATD_PURPUR_GAMEPLAY_SOURCE_V5".equals(gameplaySelector)
                && digest.equals(gameplay.getAsJsonObject("codeBindings").get(JUNIT_FIXTURE_SUCCESSOR_V2_MANIFEST).getAsString()),"JUnit fixture V2 bound by V5");
            List<String> v5Entries=new ArrayList<>(gameplay.getAsJsonObject("archiveEntries").keySet()); Collections.sort(v5Entries);
            List<String> v5Declared=new ArrayList<>(); successor.getAsJsonArray("v5PredecessorArchiveEntries").forEach(v -> v5Declared.add(v.getAsString()));
            require(v5Declared.equals(v5Entries),"JUnit fixture V2 exact V5 predecessor inventory");
            require("V5 JUnit fixture closure only; use the exact V5 predecessor inventory for a disposable V4 source projection.".equals(successor.get("scope").getAsString()),"JUnit fixture V2 scope");
        }
        JsonObject source=successor.getAsJsonObject("sourcePostimage");
        require(source.keySet().equals(Set.of("path","sha256")) && UIUX_SOURCE_CONTRACT.equals(source.get("path").getAsString())
            && digest(Files.readAllBytes(safe(root,UIUX_SOURCE_CONTRACT))).equals(stage1(root)==null ? source.get("sha256").getAsString() : stage1(root).getAsJsonObject("codeBindings").get(UIUX_SOURCE_CONTRACT).getAsString()),"JUnit fixture successor source postimage");
        require("NOT_GRANTED".equals(successor.get("ownerBApproval").getAsString()) && !successor.get("PRODUCT_GAMEPLAY_READY").getAsBoolean(),"JUnit fixture successor readiness boundary");
        return successor;
    }

    private static String gameplayManifest() {
        if ("ATD_PURPUR_GAMEPLAY_SOURCE_V5".equals(System.getenv("ACHIEVETODO_GAMEPLAY_SOURCE_AUTHORITY"))) return GAMEPLAY_V5_MANIFEST;
        if ("ATD_PURPUR_GAMEPLAY_SOURCE_V4".equals(System.getenv("ACHIEVETODO_GAMEPLAY_SOURCE_AUTHORITY"))) return GAMEPLAY_V4_MANIFEST;
        if ("ATD_PURPUR_GAMEPLAY_SOURCE_V3".equals(System.getenv("ACHIEVETODO_GAMEPLAY_SOURCE_AUTHORITY"))) return GAMEPLAY_V3_MANIFEST;
        return "ATD_PURPUR_GAMEPLAY_SOURCE_V2".equals(System.getenv("ACHIEVETODO_GAMEPLAY_SOURCE_AUTHORITY")) ? GAMEPLAY_V2_MANIFEST : GAMEPLAY_MANIFEST;
    }

    private static JsonObject gameplay(Path root) throws Exception {
        String selector=System.getenv("ACHIEVETODO_GAMEPLAY_SOURCE_AUTHORITY"),file=System.getenv("ACHIEVETODO_GAMEPLAY_SOURCE_AUTHORITY_FILE");
        if(selector==null && file==null)return null;
        boolean v2="ATD_PURPUR_GAMEPLAY_SOURCE_V2".equals(selector),v3="ATD_PURPUR_GAMEPLAY_SOURCE_V3".equals(selector),v4="ATD_PURPUR_GAMEPLAY_SOURCE_V4".equals(selector),v5="ATD_PURPUR_GAMEPLAY_SOURCE_V5".equals(selector);
        String expected=v5 ? System.getenv("ACHIEVETODO_GAMEPLAY_SOURCE_AUTHORITY_SHA256") : (v4 ? "e069f6109cbbf2dd679859b10e814d987e77ba8c9c9338899e13be9b4abcce53" : (v3 ? "179a8fb1fce386cfd4f78aa4d96e5f6dfab94701fac35a2f27ccbb17b78ed44c" : (v2 ? "522fecccba5e2c3f657afe2db0bc2b2db54c93d674c95206a2061bf695c48bab" : "64b9f7cefac0b862366e5b6988e0e95847986fcf7868e4734327790b7e838693")));
        require((v5||v4||v3||v2||"ATD_PURPUR_GAMEPLAY_SOURCE_V1".equals(selector)) && file!=null && Path.of(file).equals(ROOT.resolve(gameplayManifest())) && ui07(ROOT)!=null,"gameplay unknown/orphan/path selector");
        require(expected!=null && expected.matches("[0-9a-f]{64}"),"gameplay manifest digest selector");
        JsonObject m=pinnedJson(root,gameplayManifest(),expected);
        require("AUTHORIZED_BOUNDED_SOURCE_FOR_CERTIFICATION_ONLY".equals(m.get("state").getAsString()),"gameplay source scope");
        if(v5) {
            JsonObject parent=pinnedJson(root,GAMEPLAY_V4_MANIFEST,"e069f6109cbbf2dd679859b10e814d987e77ba8c9c9338899e13be9b4abcce53");
            JsonObject ref=m.getAsJsonObject("oldV4");
            require(ref.get("path").getAsString().equals(GAMEPLAY_V4_MANIFEST) && ref.get("sha256").getAsString().equals("e069f6109cbbf2dd679859b10e814d987e77ba8c9c9338899e13be9b4abcce53"),"V5 immutable V4 parent");
            JsonObject delta=m.getAsJsonObject("productionDelta"),current=parent.getAsJsonObject("currentProductionClosure").deepCopy(),flat=parent.getAsJsonObject("allGameplayChanges").deepCopy();
            Set<String> paths=Set.of("port/AchieveToDo-Purpur/src/main/java/com/diskree/achievetodo/landmark/CanonicalLandmarkMembership.java","port/AchieveToDo-Purpur/src/main/java/com/diskree/achievetodo/landmark/bukkit/BukkitExactLandmarkRuntime.java");
            require(delta.keySet().equals(paths),"V5 exact two-file production delta");
            for(String path:paths) {
                JsonObject d=delta.getAsJsonObject(path);
                require(current.has(path) && current.get(path).getAsString().equals(d.get("beforeSHA256").getAsString()) && !d.get("beforeSHA256").getAsString().equals(d.get("afterSHA256").getAsString()),"V5 exact production predecessor");
                current.addProperty(path,d.get("afterSHA256").getAsString());
                require(flat.has(path) && flat.getAsJsonObject(path).get("afterSHA256").getAsString().equals(d.get("beforeSHA256").getAsString()),"V5 flattened predecessor");
                flat.getAsJsonObject(path).addProperty("afterSHA256",d.get("afterSHA256").getAsString());
            }
            require(m.getAsJsonObject("currentProductionClosure").equals(current) && current.size()==2504,"V5 full production closure");
            require(m.getAsJsonObject("allGameplayChanges").equals(flat),"V5 flattened gameplay identity");
            require("NOT_GRANTED".equals(m.get("ownerBApproval").getAsString()) && !m.get("PRODUCT_GAMEPLAY_READY").getAsBoolean(),"V5 readiness boundary");
            System.err.println("V5_SOURCE_IDENTITY_ONLY historical_controls=NOT_RUN (fresh mandatory V5 Gradle node required)");
        }
        if(v4) {
            JsonObject parent=pinnedJson(root,GAMEPLAY_V3_MANIFEST,"179a8fb1fce386cfd4f78aa4d96e5f6dfab94701fac35a2f27ccbb17b78ed44c");
            JsonObject ref=m.getAsJsonObject("oldV3");
            require(ref.get("path").getAsString().equals(GAMEPLAY_V3_MANIFEST) && ref.get("sha256").getAsString().equals("179a8fb1fce386cfd4f78aa4d96e5f6dfab94701fac35a2f27ccbb17b78ed44c"),"V4 parent pin");
            String target="port/AchieveToDo-Purpur/src/main/java/com/diskree/achievetodo/gameplay/nativeguard/NativeLandmarkActions.java";
            JsonObject delta=m.getAsJsonObject("productionDelta"),flat=parent.getAsJsonObject("allGameplayChanges").deepCopy();
            require(delta.size()==1 && delta.has(target) && flat.getAsJsonObject(target).get("afterSHA256").equals(delta.getAsJsonObject(target).get("beforeSHA256")),"V4 incremental source identity");
            flat.getAsJsonObject(target).addProperty("afterSHA256",delta.getAsJsonObject(target).get("afterSHA256").getAsString());
            require(flat.equals(m.getAsJsonObject("allGameplayChanges")),"V4 flattened original UI identity");
            System.err.println("SOURCE_IDENTITY_ONLY historical_controls=NOT_RUN (fresh normal Gradle node required)");
        }
        if(v3) {
            JsonObject parent=pinnedJson(root,GAMEPLAY_V2_MANIFEST,"522fecccba5e2c3f657afe2db0bc2b2db54c93d674c95206a2061bf695c48bab");
            JsonObject parentRef=m.getAsJsonObject("oldV2");
            require(parentRef.get("path").getAsString().equals(GAMEPLAY_V2_MANIFEST) && parentRef.get("sha256").getAsString().equals("522fecccba5e2c3f657afe2db0bc2b2db54c93d674c95206a2061bf695c48bab"),"V3 immutable parent pin");
            String target="port/AchieveToDo-Purpur/src/main/java/com/diskree/achievetodo/gameplay/nativeguard/NativeLandmarkActions.java";
            JsonObject delta=m.getAsJsonObject("productionDelta");require(delta.size()==1 && delta.has(target),"V3 bounded production delta");
            JsonObject flattened=parent.getAsJsonObject("allGameplayChanges").deepCopy();
            require(flattened.getAsJsonObject(target).get("afterSHA256").equals(delta.getAsJsonObject(target).get("beforeSHA256")),"V3 predecessor production identity");
            flattened.getAsJsonObject(target).addProperty("afterSHA256",delta.getAsJsonObject(target).get("afterSHA256").getAsString());
            require(flattened.equals(m.getAsJsonObject("allGameplayChanges")),"V3 flattened UI07 source view");
        }
        if(v5 || v4 || v2 || v3) { m=m.deepCopy();m.add("changes",m.getAsJsonObject("allGameplayChanges")); }
        return m;
    }

    private UiUxMaintenanceSourceContract() {}
    static void require(boolean ok, String reason) {
        if (!ok) throw new IllegalStateException("UI_UX_CONTRACT_REJECTED: " + reason);
    }
    static String digest(byte[] data) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
    }
    static Path safe(Path root, String relative) throws Exception {
        Path rel = Path.of(relative);
        require(!rel.isAbsolute() && !relative.contains("\\") && !Arrays.asList(relative.split("/")).contains(".."), "unsafe path " + relative);
        Path base = root.toAbsolutePath().normalize(), file = base.resolve(rel).normalize();
        require(file.startsWith(base) && base.toRealPath().equals(base), "root/path indirection " + relative);
        require(Files.isRegularFile(file) && !Files.isSymbolicLink(file) && file.toRealPath().equals(file), "missing/indirected " + relative);
        return file;
    }
    static JsonObject pinnedJson(Path root, String relative, String expected) throws Exception {
        byte[] bytes = Files.readAllBytes(safe(root, relative));
        require(digest(bytes).equals(expected), "authority/receipt hash " + relative);
        return JsonParser.parseString(new String(bytes, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
    }
    static JsonObject authority(Path root) throws Exception {
        JsonObject a = pinnedJson(root, MANIFEST, MANIFEST_SHA256);
        require(a.get("schema").getAsInt() == 1 && AUTHORITY.equals(a.get("authority").getAsString()), "authority/schema");
        require(a.getAsJsonObject("ownerDecision").get("date").getAsString().equals("2026-10-28"), "owner decision binding");
        return a;
    }
    static boolean validateSelection(Path root, String selector, String file) throws Exception {
        if (selector == null || selector.isEmpty()) {
            require(file == null || file.isEmpty(), "orphan authority file selector");
            return false;
        }
        require(AUTHORITY.equals(selector), "unknown selector");
        require(file != null && Path.of(file).isAbsolute()
            && Path.of(file).normalize().equals(root.toAbsolutePath().normalize().resolve(MANIFEST)), "wrong authority file selection");
        authority(root); // Data must authenticate before a current branch can be selected.
        return true;
    }
    public static boolean selected() throws Exception {
        return validateSelection(ROOT, System.getenv("ACHIEVETODO_UI_UX_CERTIFICATION_AUTHORITY"),
            System.getenv("ACHIEVETODO_UI_UX_CERTIFICATION_FILE"));
    }
    /** Explicit separate authority; Owner A/draft/environment alone cannot grant an overlay. */
    static JsonObject composition(Path root) throws Exception {
        boolean repair=junitRepair(root),fixtureSuccessor=junitFixtureSuccessor(root)!=null;
        require(repair==fixtureSuccessor,"JUnit V1 and fixture successor selection must match");
        String selector=System.getenv("ACHIEVETODO_GATE_B_AUTHORITY"),file=System.getenv("ACHIEVETODO_GATE_B_APPROVAL_FILE");
        if(selector==null && file==null)return null;
        require("ATD_DISTRIBUTION_B_V2_RAW_CAPTURE_V1".equals(selector),"Gate B unknown/orphan selector");
        require(System.getenv("ACHIEVETODO_INPUT_AUTHORITY")==null && System.getenv("ACHIEVETODO_ALTERNATIVE_MANIFEST")==null
            && System.getenv("ACHIEVETODO_SOURCE_ORACLE")==null && System.getenv("ACHIEVETODO_SOURCE_ORACLE_FILE")==null,"Gate B mixed selectors");
        require(file!=null && Path.of(file).isAbsolute() && Path.of(file).normalize().equals(ROOT.resolve("tools/certification/authority/distribution-b-v2-approval.json")),"Gate B approval path");
        require(GATE_B_OWNER_B_CAPSULE_SHA256.matches("[0-9a-f]{64}"),"Gate B OWNER_B_REQUIRED");
        pinnedJson(ROOT,"tools/certification/authority/distribution-b-v2-approval.json",GATE_B_OWNER_B_CAPSULE_SHA256);
        JsonObject approved=pinnedJson(root,"tools/certification/authority/distribution-b-v2-approval.json",GATE_B_OWNER_B_CAPSULE_SHA256);
        require("APPROVED_OWNER_B".equals(approved.get("state").getAsString()) && approved.get("activationApproved").getAsBoolean(),"Gate B draft capsule");
        String python=System.getenv("ACHIEVETODO_PYTHON");require(python!=null && !python.isBlank(),"explicit Gate B Python required");
        String gameplaySelector=System.getenv("ACHIEVETODO_GAMEPLAY_SOURCE_AUTHORITY");
        boolean v4Projection="ATD_PURPUR_GAMEPLAY_SOURCE_V4".equals(gameplaySelector),v5Projection="ATD_PURPUR_GAMEPLAY_SOURCE_V5".equals(gameplaySelector);
        ProcessBuilder builder;
        if(v5Projection) builder=new ProcessBuilder(python,"-B","-X","utf8","-m",(stage1Selected()?"tools.build.stage1_test_source_v2":"tools.build.gameplay_successor_v5"),"--approval-only","--source-root",root.toString(),"--expected-digest",System.getenv("ACHIEVETODO_GAMEPLAY_SOURCE_AUTHORITY_SHA256"));
        else builder=new ProcessBuilder(python,"-B","-X","utf8",safe(root,"tools/build/verify_inputs_v2.py").toString(),"--source-root",root.toString(),"--approval-only");
        Path diagnosticError=Files.createTempFile("atd-stage1-v2-composition-", ".stderr");
        builder.directory(root.toFile()).redirectErrorStream(false).redirectError(diagnosticError.toFile());
        // Stderr is captured outside the source tree; retain failure diagnostics.
        builder.environment().put("ACHIEVETODO_GATE_B_APPROVAL_FILE",root.resolve("tools/certification/authority/distribution-b-v2-approval.json").toString());
        builder.environment().put("ACHIEVETODO_UI_UX_CERTIFICATION_FILE",root.resolve(MANIFEST).toString());
        if(ui07(ROOT)!=null)builder.environment().put("ACHIEVETODO_UI07_AUTHORITY_FILE",root.resolve(UI07_MANIFEST).toString());
        if(gameplay(ROOT)!=null)builder.environment().put("ACHIEVETODO_GAMEPLAY_SOURCE_AUTHORITY_FILE",root.resolve(gameplayManifest()).toString());
        if(repair)builder.environment().put(JUNIT_REPAIR_FILE,root.resolve(JUNIT_REPAIR_MANIFEST).toString());
        if(v5Projection)builder.environment().put(JUNIT_FIXTURE_SUCCESSOR_FILE,root.resolve(JUNIT_FIXTURE_SUCCESSOR_V2_MANIFEST).toString());
        if(stage1Selected())builder.environment().put("ATD_STAGE1_AUTHORITY_FILE",root.resolve(STAGE1_MANIFEST).toString());
        Process process=builder.start();String output=new String(process.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
        int exit=process.waitFor();String error=Files.readString(diagnosticError,java.nio.charset.StandardCharsets.UTF_8);
        if(exit==0)Files.delete(diagnosticError);
        require(exit==0,"Gate B composition authority exit="+exit+" stderrFile="+diagnosticError+" stdout="+output+" stderr="+error);
        JsonObject capsule=JsonParser.parseString(output).getAsJsonObject();
        require("ATD_DISTRIBUTION_B_V2_RAW_CAPTURE_V1".equals(capsule.get("authority").getAsString()),"Gate B authority identity");
        require(approved.get("sourceAuxiliaryBindings").equals(capsule.get("sourceAuxiliaryBindings")) && approved.get("fixturePaths").equals(capsule.get("fixturePaths")),"Gate B subprocess capsule substitution");
        return capsule;
    }

    static Set<String> compositionFixturePaths(Path root) throws Exception {
        JsonObject capsule=composition(root);Set<String> paths=new HashSet<>();
        if(capsule!=null)capsule.getAsJsonArray("fixturePaths").forEach(v -> paths.add(v.getAsString()));
        JsonObject successor=ui07(root);
        if(successor!=null){paths.add(UI07_MANIFEST);paths.addAll(successor.getAsJsonObject("codeBindings").keySet());}
        JsonObject gameplay=gameplay(root);
        if(gameplay!=null){paths.add(gameplayManifest());paths.addAll(gameplay.getAsJsonObject("codeBindings").keySet());paths.addAll(gameplay.getAsJsonObject("changes").keySet());}
        if(junitRepair(root))paths.addAll(List.of(JUNIT_REPAIR_MANIFEST,"tools/build/gameplay_junit_overlay_v1.py","tools/build/tests/test_gameplay_junit_overlay_v1.py","tools/certification/fixtures/gameplay-junit-overlay-predecessor-v1.zip"));
        JsonObject fixture=junitFixtureSuccessor(root);
        if(fixture!=null){
            boolean v5="ATD_PURPUR_GAMEPLAY_SOURCE_V5".equals(System.getenv("ACHIEVETODO_GAMEPLAY_SOURCE_AUTHORITY"));
            require(gameplay!=null && (v5||"ATD_PURPUR_GAMEPLAY_SOURCE_V4".equals(System.getenv("ACHIEVETODO_GAMEPLAY_SOURCE_AUTHORITY"))),"JUnit fixture successor gameplay selection");
            paths.add(v5?JUNIT_FIXTURE_SUCCESSOR_V2_MANIFEST:JUNIT_FIXTURE_SUCCESSOR_MANIFEST);
            fixture.getAsJsonArray("v4ArchiveEntries").forEach(v -> paths.add(v.getAsString()));
            if(v5)fixture.getAsJsonArray("v5PredecessorArchiveEntries").forEach(v -> paths.add(v.getAsString()));
        }
        JsonObject stage1=stage1(root);
        if(stage1!=null){paths.add(STAGE1_MANIFEST);paths.addAll(stage1.getAsJsonObject("parentSourceInventory").keySet());paths.addAll(stage1.getAsJsonObject("codeBindings").keySet());paths.add(stage1.getAsJsonObject("independentReview").get("path").getAsString());}
        return paths;
    }

    /** No cached acceptance: every boundary rechecks actual bytes and inventory. */
    public static JsonObject verifyRoot(Path root) throws Exception {
        root = root.toAbsolutePath().normalize();
        JsonObject a = authority(root), r9 = pinnedJson(root, R9, R9_SHA256);
        JsonObject production = a.getAsJsonObject("productionClosure");
        JsonObject successor = ui07(root);
        if (successor != null) {
            production = production.deepCopy();
            JsonObject d = successor.getAsJsonObject("productionDelta");
            require(SCREEN.equals(d.get("path").getAsString()) && production.get(SCREEN).getAsString().equals(d.get("beforeSHA256").getAsString()), "UI07 exact predecessor delta");
            production.addProperty(SCREEN, d.get("afterSHA256").getAsString());
        }
        JsonObject gameplay=gameplay(root);
        if(gameplay!=null) {
            production=production.deepCopy();
            for(var entry:gameplay.getAsJsonObject("changes").entrySet()) {
                String path=entry.getKey();JsonObject d=entry.getValue().getAsJsonObject();
                if(path.startsWith("src/main/") || path.startsWith(PORT)) {
                    require(d.get("beforeSHA256").isJsonNull() ? !production.has(path) : production.has(path) && production.get(path).getAsString().equals(d.get("beforeSHA256").getAsString()),"gameplay exact predecessor delta");
                    production.addProperty(path,d.get("afterSHA256").getAsString());
                }
            }
        }
        Map<String,String> observed = new HashMap<>();
        for (String directory : List.of("src/main", PORT.substring(0, PORT.length()-1))) {
            Path folder = root.resolve(directory);
            require(Files.isDirectory(folder) && folder.toRealPath().equals(folder), "missing/indirected production root");
            try (Stream<Path> files = Files.walk(folder)) {
                for (Path p : files.toList()) {
                    require(!Files.isSymbolicLink(p) && p.toRealPath().equals(p.toAbsolutePath().normalize()), "indirected inventory entry");
                    if (Files.isRegularFile(p)) observed.put(root.relativize(p).toString().replace('\\', '/'), digest(Files.readAllBytes(p)));
                }
            }
        }
        require(observed.keySet().equals(production.keySet()), "production inventory additions/omissions");
        for (var e : production.entrySet()) require(e.getValue().getAsString().equals(observed.get(e.getKey())), "file bytes " + e.getKey());
        JsonObject composition = composition(root);
        JsonObject overlays = composition == null ? new JsonObject() : composition.getAsJsonObject("sourceAuxiliaryBindings");
        Set<String> permitted = Set.of("src/test/java/com/diskree/achievetodo/certification/UiUxMaintenanceGateContractTest.java");
        require(permitted.containsAll(overlays.keySet()), "unapproved auxiliary overlay");
        for (var e : a.getAsJsonObject("auxiliaryFiles").entrySet()) {
            String expected = e.getValue().getAsString();
            if (overlays.has(e.getKey())) {
                JsonObject overlay = overlays.getAsJsonObject(e.getKey());
                require(expected.equals(overlay.get("historicalSHA256").getAsString()), "auxiliary historical pin substitution");
                expected = overlay.get("currentSHA256").getAsString();
            }
            checkFile(root, e.getKey(), expected);
        }
        JsonObject delta = a.getAsJsonObject("r9UiDelta");
        require(delta.size() == 5, "R9 delta cardinality");
        for (var e : r9.getAsJsonObject("productionSource").entrySet()) {
            String wanted = delta.has(e.getKey()) ? delta.get(e.getKey()).getAsString() : e.getValue().getAsString();
            if (successor != null && e.getKey().equals(SCREEN)) wanted = successor.getAsJsonObject("productionDelta").get("afterSHA256").getAsString();
            if(gameplay!=null && gameplay.getAsJsonObject("changes").has(e.getKey()))wanted=gameplay.getAsJsonObject("changes").getAsJsonObject(e.getKey()).get("afterSHA256").getAsString();
            require(production.has(e.getKey()) && production.get(e.getKey()).getAsString().equals(wanted), "non-UI R9 baseline changed " + e.getKey());
        }
        for (String path : delta.keySet()) require(r9.getAsJsonObject("productionSource").has(path), "invented R9 exception");
        for (var e : a.getAsJsonObject("protectedPins").entrySet()) {
            require(e.getValue().equals(r9.getAsJsonObject("pins").get(e.getKey())), "historical pin substitution");
            checkFile(root, e.getKey(), e.getValue().getAsJsonObject().get("fabricSHA256").getAsString());
        }
        assertSemantics(root, a);
        return a;
    }
    private static void checkFile(Path root, String path, String hash) throws Exception {
        require(digest(Files.readAllBytes(safe(root, path))).equals(hash), "file bytes " + path);
    }
    public static String expectedProtected(String historical, String path) throws Exception {
        if (!selected()) return R3SourceOracle.expected(historical, path);
        JsonObject pins = verifyRoot(ROOT).getAsJsonObject("protectedPins");
        require(pins.has(path), "unlisted protected caller " + path);
        JsonObject pin = pins.getAsJsonObject(path);
        require(pin.get("historicalSHA256").getAsString().equals(historical), "historical caller digest");
        return pin.get("fabricSHA256").getAsString();
    }
    public static void assertHistoricalRuntimeContract(String domain, JsonObject supplied) throws Exception {
        require(selected(), "current contract not selected");
        JsonObject a = verifyRoot(ROOT), domains = a.getAsJsonObject("runtimeContracts");
        require(domains.has(domain), "unknown historical domain");
        JsonObject d = domains.getAsJsonObject(domain);
        JsonObject receipt = pinnedJson(ROOT, d.get("path").getAsString(), d.get("sha256").getAsString());
        JsonObject original = receipt.getAsJsonObject(d.get("mapKey").getAsString());
        require(original.equals(supplied), "historical caller map substitution");
        if (domain.equals("PHASE_B_B9_V1")) PhaseBPackTestFixtures.assertCurrentToolAuthority();
        for (var e : original.entrySet()) {
            String path = e.getKey();
            if (UI_EXCEPTIONS.contains(path)) {
                require(a.getAsJsonObject("historicalUiPins").get(path).getAsString().equals(e.getValue().getAsString()), "historical UI digest substitution");
                checkFile(ROOT, path, a.getAsJsonObject("productionClosure").get(path).getAsString());
            } else if (domain.equals("PHASE_B_B9_V1") && path.equals("tools/phase_b/b7_localization_certification.py")) {
                require(digest(PhaseBPackTestFixtures.historicalToolBytes()).equals(e.getValue().getAsString()), "historical localization tool");
            } else checkFile(ROOT, path, e.getValue().getAsString());
        }
    }
    static void assertOrder(Map<String,List<String>> actual, JsonObject expected) {
        Map<String,List<String>> wanted = new LinkedHashMap<>();
        expected.entrySet().forEach(e -> { var ids = new ArrayList<String>(); e.getValue().getAsJsonArray().forEach(v -> ids.add(v.getAsString())); wanted.put(e.getKey(), ids); });
        require(wanted.size() == 3 && wanted.values().stream().mapToInt(List::size).sum() == 27 && wanted.equals(actual), "three maps / 27 exact ordered identities");
    }
    static void assertUiSignals(String progress, String screen, String background, String nativeHook) {
        require(!progress.contains("\"locked\"") && !progress.contains("\"unlocked\""), "literal tooltip status");
        require(screen.contains("current.equals(bridgeLayoutConfiguration)") && screen.contains("rebuildWidgets();")
            && screen.contains("setSelectedTab(selected, true)"), "threshold invalidation / tab preservation");
        require(background.equals("minecraft:block/pale_moss_block"), "legacy texture identifier");
        require(nativeHook.contains("daf0d7d36d7db775356ae41dca90f77b5fb09c31c754e4638edf313ded6259dc")
            && nativeHook.contains("e41beef5bcdc6aab32d602a9faa8ff4425c9fe7ab59d3a6de8a4d742b93deddd")
            && nativeHook.contains("unfinishedVisibility") && !nativeHook.contains("awardCriteria"), "pinned presentation-only native hook");
    }
    @SuppressWarnings("unchecked")
    static void assertSemantics(Path root, JsonObject a) throws Exception {
        List<String> names = Arrays.stream(AdvancementsTabType.values()).map(AdvancementsTabType::getName).toList();
        List<String> expected = new ArrayList<>(); a.getAsJsonArray("tabs").forEach(v -> expected.add(v.getAsString()));
        require(names.equals(expected) && names.size() == 17, "17 exact category identities");
        for (AdvancementsTabType type : AdvancementsTabType.values()) require(AdvancementsTabType.findByAdvancement(Identifier.parse("blazeandcave:"+type.getName()+"/root")) == type, "category mapping");
        for (String ns : List.of("other", "unknown")) require(AdvancementsTabType.findByAdvancement(Identifier.parse(ns+":abilities/root")) == null, "foreign namespace capture");
        for (String key : List.of("adventure","husbandry","nether","end")) require(AdvancementsTabType.findByAdvancement(Identifier.parse("minecraft:"+key+"/root")) != null, "vanilla root mapping");
        require(AdvancementsTabType.findByAdvancement(Identifier.parse("blazeandcave:unknown/root")) == null, "unknown root capture");
        var field = Class.forName("com.diskree.achievetodo.injection.mixin.main.PlacedAdvancementMixin").getDeclaredField("customChildrenOrderMap"); field.setAccessible(true);
        assertOrder((Map<String,List<String>>)field.get(null), a.getAsJsonObject("orderMaps"));
        require(TrackedScoreType.values().length == 33 && TrackedStatisticsDataType.values().length == 50, "tracker enum counts");
        require(AbilityType.values().length == 153 && AbilityType.USE_COPPER_TOOLS.ordinal() == 151 && AbilityType.EQUIP_COPPER_ARMOR.ordinal() == 152, "153/copper identities");
        for (int threshold : new int[]{1, 2, 11, 319, Integer.MAX_VALUE}) {
            require(AbilityPresentationState.mystified(threshold,false,false), "unknown numeric identity");
            require(!AbilityPresentationState.mystified(threshold,false,true), "discovered locked identity");
            require(!AbilityPresentationState.mystified(threshold,true,false), "unlocked identity");
        }
        for (int flag : new int[]{0,-1}) require(!AbilityPresentationState.mystified(flag,false,false), "special flag identity");
        String portRoot = PORT+"resources/datapack/data/achievetodo/advancement/abilities/root.json";
        JsonObject rootJson = JsonParser.parseString(Files.readString(safe(root,portRoot))).getAsJsonObject();
        assertUiSignals(Files.readString(safe(root,PROGRESS)), Files.readString(safe(root,SCREEN)), rootJson.getAsJsonObject("display").get("background").getAsString(),
            Files.readString(safe(root,PORT+"java/com/diskree/achievetodo/ui/advancement/AdvancementVisibilityAgent.java")));
        require(Files.readString(safe(root,PORT+"java/com/diskree/achievetodo/datapack/EmbeddedDatapack.java")).contains("\"minecraft:block/pale_moss_block\""), "renderer background");
    }
}
