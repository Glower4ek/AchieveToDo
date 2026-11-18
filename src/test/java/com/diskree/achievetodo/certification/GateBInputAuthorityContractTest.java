package com.diskree.achievetodo.certification;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import static org.junit.jupiter.api.Assertions.*;

/** Owner A checks do not approve captured bytes or open the Owner B gate. */
public class GateBInputAuthorityContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();
    private static final String AUTHORITY = "ATD_DISTRIBUTION_B_V2_RAW_CAPTURE_V1";
    private static final String APPROVAL = ROOT.resolve("tools/certification/authority/distribution-b-v2-approval.json").toString();
    private record Outcome(int exit, String output, String diagnostics) {}
    private static Map<String,String> selection() {
        Map<String,String> selected = new HashMap<>(Map.of("ACHIEVETODO_GATE_B_AUTHORITY",AUTHORITY,"ACHIEVETODO_GATE_B_APPROVAL_FILE",APPROVAL)); repairSelection(selected); return selected;
    }
    private static void repairSelection(Map<String,String> env) {
        for (String key : List.of("ACHIEVETODO_GAMEPLAY_JUNIT_REPAIR","ACHIEVETODO_GAMEPLAY_JUNIT_REPAIR_FILE"))
            if (System.getenv(key) != null) env.put(key,System.getenv(key));
    }
    private static Outcome python(Map<String,String> extra, String... arguments) throws Exception {
        String python = System.getenv("ACHIEVETODO_PYTHON");
        assertNotNull(python,"Explicit Python required");
        List<String> command = new ArrayList<>(List.of(python,"-B","-X","utf8"));command.addAll(List.of(arguments));
        ProcessBuilder process = new ProcessBuilder(command).directory(ROOT.toFile()).redirectErrorStream(false);
        process.environment().keySet().removeIf(k -> k.startsWith("ACHIEVETODO_GATE_B") || Set.of("ACHIEVETODO_INPUT_AUTHORITY","ACHIEVETODO_ALTERNATIVE_MANIFEST","ACHIEVETODO_SOURCE_ORACLE","ACHIEVETODO_SOURCE_ORACLE_FILE").contains(k));
        process.environment().put("PYTHONDONTWRITEBYTECODE","1");
        process.environment().put("ACHIEVETODO_UI_UX_CERTIFICATION_AUTHORITY","ATD_UI_UX_MAINTENANCE_V1");
        process.environment().put("ACHIEVETODO_UI_UX_CERTIFICATION_FILE",ROOT.resolve("tools/certification/authority/ui-ux-maintenance-v1.json").toString());
        process.environment().remove("ACHIEVETODO_GAMEPLAY_JUNIT_REPAIR");
        process.environment().remove("ACHIEVETODO_GAMEPLAY_JUNIT_REPAIR_FILE");
        process.environment().putAll(extra);
        boolean selected = AUTHORITY.equals(extra.get("ACHIEVETODO_GATE_B_AUTHORITY"))
            && APPROVAL.equals(extra.get("ACHIEVETODO_GATE_B_APPROVAL_FILE")) && !extra.containsKey("ACHIEVETODO_INPUT_AUTHORITY");
        if (selected || (arguments.length > 0 && arguments[0].equals("-m"))) repairSelection(process.environment());
        else { process.environment().remove("ACHIEVETODO_GAMEPLAY_JUNIT_REPAIR"); process.environment().remove("ACHIEVETODO_GAMEPLAY_JUNIT_REPAIR_FILE"); }
        Process child = process.start();
        var errors = java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try { return new String(child.getErrorStream().readAllBytes(),StandardCharsets.UTF_8); }
            catch (java.io.IOException ex) { throw new java.io.UncheckedIOException(ex); }
        });
        String output = new String(child.getInputStream().readAllBytes(),StandardCharsets.UTF_8);
        int exit=child.waitFor();String diagnostics=errors.join();System.err.print(diagnostics);
        return new Outcome(exit,output,diagnostics);
    }
    private static Outcome approval(Map<String,String> env) throws Exception {
        if(UiUxMaintenanceSourceContract.stage1Selected() && AUTHORITY.equals(env.get("ACHIEVETODO_GATE_B_AUTHORITY")) && APPROVAL.equals(env.get("ACHIEVETODO_GATE_B_APPROVAL_FILE")) && !env.containsKey("ACHIEVETODO_INPUT_AUTHORITY"))
            return python(env,"-m","tools.build.stage1_test_source_v2","--source-root",ROOT.toString(),"--expected-digest",System.getenv("ACHIEVETODO_GAMEPLAY_SOURCE_AUTHORITY_SHA256"),"--approval-only");
        return python(env,"tools/build/verify_inputs_v2.py","--source-root",ROOT.toString(),"--approval-only");
    }
    private static void rejected(Outcome result, String reason) {
        String visible=result.output+result.diagnostics;
        assertNotEquals(0,result.exit,visible);assertTrue(visible.contains(reason),visible);
        assertFalse(visible.contains("EXTERNAL_INPUTS_V2_PASS"),visible);
        System.out.println("GATE_B_EXTERNAL_REJECTED "+reason+" exit="+result.exit);
    }
    private static String hash(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }
    @Test void historicalAuthorityAndVerifierRemainByteIdentical() throws Exception {
        assertEquals("1dbe9d7cd2fe653454f8692b7cdf5e54787c3df74eb12461c01164b99190ac77",hash(ROOT.resolve("src/test/resources/build_inputs/manifest.json")));
        assertEquals("92617b59c39cf94f708f895d81a3b2c763b0d7434271716cadb6315b5bf392ce",hash(ROOT.resolve("tools/build/verify_inputs.py")));
        assertEquals("3f04f19ba716eaa9cb8515ef45564bf4b23417005262070b5517d42ebdc59072",hash(ROOT.resolve("tools/certification/authority/ui-ux-maintenance-v1.json")));
    }
    @Test void unknownSelectorRejects() throws Exception {
        rejected(approval(Map.of("ACHIEVETODO_GATE_B_AUTHORITY","UNKNOWN","ACHIEVETODO_GATE_B_APPROVAL_FILE",APPROVAL)),"unknown/orphan");
    }
    @Test void orphanFileRejects() throws Exception {
        rejected(approval(Map.of("ACHIEVETODO_GATE_B_APPROVAL_FILE",APPROVAL)),"unknown/orphan");
    }
    @Test void mixedAlternativeAndEvenEmptyLegacySelectorRejects() throws Exception {
        Map<String,String> env = new HashMap<>(selection());env.put("ACHIEVETODO_INPUT_AUTHORITY","");
        rejected(approval(env),"mixed historical/alternative");
    }
    @Test void wrongCapsulePathRejects() throws Exception {
        Map<String,String> env = new HashMap<>(selection());env.put("ACHIEVETODO_GATE_B_APPROVAL_FILE",ROOT.resolve("other.json").toString());
        rejected(approval(env),"approval path");
    }
    @Test void noSelectorCannotRequestAuthenticatedApproval() throws Exception {
        rejected(approval(Map.of()),"explicit V2 approval required");
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=0)throw new IllegalArgumentException("No activation or result override arguments");
        Class<?> selectors=Class.forName("org.junit.platform.engine.discovery.DiscoverySelectors");
        Object selected=selectors.getMethod("selectClass",Class.class).invoke(null,GateBInputAuthorityContractTest.class);
        Class<?> builderType=Class.forName("org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder");Object builder=builderType.getMethod("request").invoke(null);
        builderType.getMethod("selectors",List.class).invoke(builder,List.of(selected));Object request=builderType.getMethod("build").invoke(builder);
        Class<?> listenerType=Class.forName("org.junit.platform.launcher.TestExecutionListener"),arrayType=Array.newInstance(listenerType,0).getClass();
        Class<?> summaryListener=Class.forName("org.junit.platform.launcher.listeners.SummaryGeneratingListener");Object listener=summaryListener.getConstructor().newInstance(),listeners=Array.newInstance(listenerType,1);Array.set(listeners,0,listener);
        Object launcher=Class.forName("org.junit.platform.launcher.core.LauncherFactory").getMethod("create").invoke(null);Class<?> launcherType=Class.forName("org.junit.platform.launcher.Launcher");
        launcherType.getMethod("registerTestExecutionListeners",arrayType).invoke(launcher,listeners);
        launcherType.getMethod("execute",Class.forName("org.junit.platform.launcher.LauncherDiscoveryRequest"),arrayType).invoke(launcher,request,Array.newInstance(listenerType,0));
        Object summary=summaryListener.getMethod("getSummary").invoke(listener);Class<?> summaryType=Class.forName("org.junit.platform.launcher.listeners.TestExecutionSummary");
        PrintWriter writer=new PrintWriter(System.out,true);summaryType.getMethod("printTo",PrintWriter.class).invoke(summary,writer);summaryType.getMethod("printFailuresTo",PrintWriter.class).invoke(summary,writer);
        Map<String,Object> result=new LinkedHashMap<>();result.put("scope","OWNER_A_CONTRACT_ONLY_NOT_INPUT_OR_STANDARD_SUITE_PASS");
        for(String key:List.of("getTestsFoundCount","getTestsSucceededCount","getTestsFailedCount","getTestsSkippedCount","getTestsAbortedCount","getContainersFailedCount"))result.put(key,summaryType.getMethod(key).invoke(summary));
        String receipt=System.getProperty("atd.gateb.receipt");if(receipt==null)throw new IllegalStateException("Explicit new receipt required");
        Files.writeString(Path.of(receipt),new GsonBuilder().setPrettyPrinting().create().toJson(result),StandardOpenOption.CREATE_NEW);
        for(String key:List.of("getTestsFailedCount","getTestsSkippedCount","getTestsAbortedCount","getContainersFailedCount"))if(((Number)result.get(key)).longValue()!=0)throw new AssertionError(result);
        if(((Number)result.get("getTestsFoundCount")).longValue()!=8)throw new AssertionError("Unexpected discovery "+result);
    }
}
