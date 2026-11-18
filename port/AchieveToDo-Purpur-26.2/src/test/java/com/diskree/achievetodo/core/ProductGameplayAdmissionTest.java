package com.diskree.achievetodo.core;

import com.google.gson.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Synthetic admission-contract fixtures only. No test receipt is gameplay certification. */
public final class ProductGameplayAdmissionTest {
    private static int checks;
    private static void check(boolean ok) { checks++;if(!ok)throw new AssertionError("check "+checks); }
    @FunctionalInterface private interface Action { void run() throws Exception; }
    private static void rejects(Action action) throws Exception {
        try { action.run(); } catch (Exception expected) { checks++;return; }
        throw new AssertionError("Expected fail-closed rejection");
    }
    private static String sha(byte[] raw) throws Exception {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw));}
    private static JsonObject obj(String... pairs) {var o=new JsonObject();for(int i=0;i<pairs.length;i+=2)o.addProperty(pairs[i],pairs[i+1]);return o;}
    private static void write(Path p, JsonObject o) throws Exception {Files.writeString(p,o.toString());}
    private static JsonObject bind(Path p) throws Exception {return obj("path",p.getFileName().toString(),"sha256",sha(Files.readAllBytes(p)));}
    private record Fixture(Path artifact,Path certificate,Path evidence,JsonObject cert) {
        String pin() throws Exception {return sha(Files.readAllBytes(certificate));}
        ProductGameplayAdmission load() throws Exception {return new ProductGameplayAdmission(ProductGameplayAdmission.Mode.PRODUCT,artifact,certificate,pin());}
        void publish() throws Exception {write(certificate,cert);}
    }
    private static Fixture fixture() throws Exception {
        Path d=Files.createTempDirectory("atd-admission-SYNTHETIC-");
        Path artifact=d.resolve("synthetic-artifact.bin");Files.writeString(artifact,"UNIT FIXTURE: not a Minecraft product");String hash=sha(Files.readAllBytes(artifact));
        Path source=d.resolve("source.json");write(source,obj("unitFixture","true"));
        Path proof=d.resolve("proof.json");write(proof,obj("unitFixture","true","status","PASS"));
        Path manifest=d.resolve("manifest.json");var m=obj("formatVersion","1","artifactSHA256",hash);m.addProperty("formatVersion",1);var entries=new JsonArray();entries.add(bind(source));entries.add(bind(proof));m.add("entries",entries);write(manifest,m);
        Path acceptance=d.resolve("acceptance.json");write(acceptance,obj("status","PASS","artifactSHA256",hash,"sourceAuthoritySHA256",sha(Files.readAllBytes(source)),"evidenceManifestSHA256",sha(Files.readAllBytes(manifest)),"authorAgent","UNIT-author","reviewerAgent","UNIT-reviewer"));
        var c=obj("formatVersion","1","artifactSHA256",hash);c.addProperty("formatVersion",1);c.add("sourceAuthority",bind(source));c.add("evidenceManifest",bind(manifest));c.add("independentAcceptance",bind(acceptance));
        var requirements=new JsonObject();for(String key:ProductGameplayAdmission.CHECKS) {var row=obj("status","PASS");row.add("evidence",bind(proof));requirements.add(key,row);}c.add("prerequisites",requirements);
        var f=new Fixture(artifact,d.resolve("certificate.json"),proof,c);f.publish();return f;
    }
    private static boolean run(ProductGameplayAdmission a, boolean[] b) {return a.operational(b[0],b[1],b[2],b[3],b[4],b[5],b[6]);}
    public static void main(String[] args) throws Exception {
        var diagnostic=new ProductGameplayAdmission(ProductGameplayAdmission.Mode.DIAGNOSTIC,null,null,null);
        var certification=new ProductGameplayAdmission(ProductGameplayAdmission.Mode.CERTIFICATION,null,null,null);
        boolean[] certLive={true,true,true,true,true,true,true};check(run(certification,certLive));check(!certification.productReady(true));check(!diagnostic.productReady(true));
        rejects(()->ProductGameplayAdmission.mode("unknown"));
        rejects(()->new ProductGameplayAdmission(ProductGameplayAdmission.Mode.PRODUCT,null,null,null));
        var f=fixture();var product=f.load();boolean[] live={true,true,true,true,true,true,false};check(run(product,live));check(product.productReady(run(product,live)));
        for(int i=0;i<7;i++){boolean[] loss=live.clone();loss[i]=!loss[i];check(!run(product,loss));check(!product.productReady(run(product,loss)));}
        for(int i=0;i<6;i++){boolean[] loss=certLive.clone();loss[i]=false;check(!run(certification,loss));}
        rejects(()->new ProductGameplayAdmission(ProductGameplayAdmission.Mode.PRODUCT,f.artifact,f.certificate,"0".repeat(64)));
        Files.writeString(f.artifact,"changed");rejects(f::load);
        for(String key:ProductGameplayAdmission.CHECKS) {
            var missing=fixture();missing.cert.getAsJsonObject("prerequisites").remove(key);missing.publish();rejects(missing::load);
            var failed=fixture();failed.cert.getAsJsonObject("prerequisites").getAsJsonObject(key).addProperty("status","NOT_RUN");failed.publish();rejects(failed::load);
        }
        var changed=fixture();Files.writeString(changed.evidence,"changed proof");rejects(changed::load);
        var absent=fixture();Files.delete(absent.evidence);rejects(absent::load);
        var extra=fixture();extra.cert.addProperty("productReady",true);extra.publish();rejects(extra::load);
        var duplicate=fixture();String raw=Files.readString(duplicate.certificate);Files.writeString(duplicate.certificate,raw.substring(0,raw.length()-1)+",\"formatVersion\":1}");rejects(duplicate::load);
        var orphan=fixture();orphan.cert.getAsJsonObject("prerequisites").getAsJsonObject("mandatory_hooks").add("evidence",bind(orphan.artifact));orphan.publish();rejects(orphan::load);
        var self=fixture();Path review=self.certificate.getParent().resolve(self.cert.getAsJsonObject("independentAcceptance").get("path").getAsString());var r=JsonParser.parseString(Files.readString(review)).getAsJsonObject();r.addProperty("reviewerAgent","UNIT-author");write(review,r);self.cert.add("independentAcceptance",bind(review));self.publish();rejects(self::load);
        var loaded=fixture();var retained=loaded.load();check(run(retained,live));Files.writeString(loaded.evidence,"lost after startup");Thread.sleep(300);check(!run(retained,live));check(!retained.productReady(true));
        var deleted=fixture();var retainedDeleted=deleted.load();Files.delete(deleted.certificate);check(!retainedDeleted.refreshIntegrity());check(!run(retainedDeleted,live));
        var jarChanged=fixture();var retainedJar=jarChanged.load();Files.writeString(jarChanged.artifact,"changed after startup");check(!retainedJar.refreshIntegrity());check(!run(retainedJar,live));
        var escape=fixture();escape.cert.getAsJsonObject("sourceAuthority").addProperty("path","../source.json");escape.publish();rejects(escape::load);
        var absolute=fixture();absolute.cert.getAsJsonObject("sourceAuthority").addProperty("path",absolute.evidence.toString());absolute.publish();rejects(absolute::load);
        var nested=fixture();Path nestedProof=nested.certificate.getParent().resolve("evidence/proof.json");Files.createDirectories(nestedProof.getParent());Files.copy(nested.evidence,nestedProof);
        var nestedBinding=bind(nestedProof);nestedBinding.addProperty("path","evidence/proof.json");
        Path nestedManifest=nested.certificate.getParent().resolve("manifest.json");var nm=JsonParser.parseString(Files.readString(nestedManifest)).getAsJsonObject();nm.getAsJsonArray("entries").add(nestedBinding);write(nestedManifest,nm);nested.cert.add("evidenceManifest",bind(nestedManifest));
        Path nestedReview=nested.certificate.getParent().resolve("acceptance.json");var nr=JsonParser.parseString(Files.readString(nestedReview)).getAsJsonObject();nr.addProperty("evidenceManifestSHA256",sha(Files.readAllBytes(nestedManifest)));write(nestedReview,nr);nested.cert.add("independentAcceptance",bind(nestedReview));
        nested.cert.getAsJsonObject("prerequisites").getAsJsonObject("mandatory_hooks").add("evidence",nestedBinding);nested.publish();check(run(nested.load(),live));
        // Ten thousand polls share a lease; forced admission bypasses it.
        var shared=fixture().load();int before=shared.integrityValidationPasses();long start=System.nanoTime();
        for(int i=0;i<10000;i++)check(run(shared,live));
        int passes=shared.integrityValidationPasses()-before;
        check(passes<=1+(System.nanoTime()-start)/250_000_000L);
        check(shared.refreshIntegrity());check(shared.integrityValidationPasses()>before+passes);
        System.out.println("ATD_PRODUCT_ADMISSION_CONTRACT_PASS checks="+checks+" syntheticUnitFixtures=true runtimeCertification=SEPARATE");
    }
}
