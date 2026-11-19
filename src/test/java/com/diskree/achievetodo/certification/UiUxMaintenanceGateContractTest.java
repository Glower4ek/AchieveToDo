package com.diskree.achievetodo.certification;

import com.google.gson.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.io.PrintWriter;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Also launchable through the existing JUnit engine without invoking the
 * blocked standard-input task. This is Gate-A-only evidence, not a full suite. */
public class UiUxMaintenanceGateContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();
    private static final String[][] ORIGINAL = {
        {"com.diskree.achievetodo.PhaseDProtectedScopeTest","phaseCRewardCountingNonrepairAndPriorTestsStayByteIdentical"},
        {"com.diskree.achievetodo.ability.PhaseDRuinedPortalSelectorTest","noDeltaWorldgenGeometryIndexActionsAndLegacyReconstructionScope"},
        {"com.diskree.achievetodo.certification.PhaseBStaticCertificationTest","all86TrackerBindingsAndRegistryIdentifiersAreCertified"},
        {"com.diskree.achievetodo.client.PhaseBBacapSearchTreeResourceTest","orderedTreeKeepsThreeMapsAnd27Entries"},
        {"com.diskree.achievetodo.tracking.PhaseBBacapTrackerContractTest","all86BindingsRemainBoundToCurrentProductionDefinitions"}};
    @TempDir Path temporary;
    @BeforeAll static void bootstrap() { net.minecraft.SharedConstants.tryDetectVersion(); net.minecraft.server.Bootstrap.bootStrap(); }

    @Test void existingMeaningfulDiscoveryNamespaceAndRealCacheRegressions() throws Exception {
        for (String method : List.of("discoveryAndUnlockAreIndependent","specialFlagsKeepOriginalVisibleIdentity","thresholdsDoNotChangeDiscoveryRules","unknownNamespaceMustNotClaimKnownTabSlots"))
            invoke("com.diskree.achievetodo.client.AbilityPresentationStateTest",method);
        invoke("com.diskree.achievetodo.client.AdvancementChildrenRefreshTest","additionsAndSameSizeReplacementsInvalidateCachedChildren");
    }
    @Test void readinessGuardMutationIsRejected() throws Exception {
        Path root=fixture(); mutate(root,"port/AchieveToDo-Purpur-26.2/src/main/java/com/diskree/achievetodo/core/AchieveToDoPlugin.java");
        rejected(() -> UiUxMaintenanceSourceContract.verifyRoot(root),"file bytes port/");
    }
    @Test void productionAdditionOmissionAndRenameAreRejected() throws Exception {
        Path root=fixture(),extra=root.resolve("src/main/java/unapproved/Extra.java");Files.createDirectories(extra.getParent());Files.writeString(extra,"unapproved");
        rejected(() -> UiUxMaintenanceSourceContract.verifyRoot(root),"inventory");Files.delete(extra);
        Path file=root.resolve(UiUxMaintenanceSourceContract.PROGRESS),renamed=file.resolveSibling(file.getFileName()+".renamed");
        Files.move(file,renamed);rejected(() -> UiUxMaintenanceSourceContract.verifyRoot(root),"inventory");Files.move(renamed,file);
        Files.delete(file);rejected(() -> UiUxMaintenanceSourceContract.verifyRoot(root),"inventory");
    }
    @Test void authorityAndBaseReceiptTamperingCannotPass() throws Exception {
        Path root=fixture();mutate(root,UiUxMaintenanceSourceContract.R9);
        rejected(() -> UiUxMaintenanceSourceContract.verifyRoot(root),"authority/receipt hash");
        Path second=fixture();mutate(second,UiUxMaintenanceSourceContract.MANIFEST);
        rejected(() -> UiUxMaintenanceSourceContract.verifyRoot(second),"authority/receipt hash");
    }
    @Test void semanticControlsAreIndependentOfCandidateBytePins() throws Exception {
        JsonObject a=UiUxMaintenanceSourceContract.authority(ROOT),order=a.getAsJsonObject("orderMaps");
        Map<String,List<String>> actual=new LinkedHashMap<>();order.entrySet().forEach(e->{var ids=new ArrayList<String>();e.getValue().getAsJsonArray().forEach(v->ids.add(v.getAsString()));actual.put(e.getKey(),ids);});
        UiUxMaintenanceSourceContract.assertOrder(actual,order);
        var missing=new LinkedHashMap<>(actual);missing.remove(missing.keySet().iterator().next());
        rejected(() -> UiUxMaintenanceSourceContract.assertOrder(missing,order),"exact ordered identities");
        String first=actual.keySet().iterator().next();var changed=new LinkedHashMap<>(actual);var ids=new ArrayList<>(actual.get(first));Collections.swap(ids,0,1);changed.put(first,ids);
        rejected(() -> UiUxMaintenanceSourceContract.assertOrder(changed,order),"exact ordered identities");
        ids.add(ids.getFirst());rejected(() -> UiUxMaintenanceSourceContract.assertOrder(changed,order),"exact ordered identities");
        String progress=Files.readString(ROOT.resolve(UiUxMaintenanceSourceContract.PROGRESS)),screen=Files.readString(ROOT.resolve(UiUxMaintenanceSourceContract.SCREEN));
        String hook=Files.readString(ROOT.resolve(UiUxMaintenanceSourceContract.PORT+"java/com/diskree/achievetodo/ui/advancement/AdvancementVisibilityAgent.java"));
        rejected(() -> UiUxMaintenanceSourceContract.assertUiSignals(progress+"\"locked\"",screen,"minecraft:block/pale_moss_block",hook),"literal tooltip");
        rejected(() -> UiUxMaintenanceSourceContract.assertUiSignals(progress,screen.replace("rebuildWidgets();","stale();"),"minecraft:block/pale_moss_block",hook),"threshold invalidation");
        rejected(() -> UiUxMaintenanceSourceContract.assertUiSignals(progress,screen,"minecraft:textures/block/pale_moss_block.png",hook),"legacy texture");
        rejected(() -> UiUxMaintenanceSourceContract.assertUiSignals(progress,screen,"minecraft:block/pale_moss_block",hook.replace("daf0d7","000000")),"native hook");
    }
    private Path fixture() throws Exception {
        Path root=Files.createTempDirectory(temporary,"source-");JsonObject a=UiUxMaintenanceSourceContract.authority(ROOT);
        Set<String> paths=new HashSet<>(a.getAsJsonObject("productionClosure").keySet());paths.addAll(a.getAsJsonObject("auxiliaryFiles").keySet());paths.addAll(a.getAsJsonObject("protectedPins").keySet());paths.add(UiUxMaintenanceSourceContract.MANIFEST);
        paths.addAll(UiUxMaintenanceSourceContract.compositionFixturePaths(ROOT));
        for(String p:paths){Path target=root.resolve(p);Files.createDirectories(target.getParent());Files.copy(ROOT.resolve(p),target);}
        return root;
    }
    private static void mutate(Path root,String p)throws Exception {Files.writeString(root.resolve(p),"\nnegative-control",StandardOpenOption.APPEND);}
    interface Checked {void run()throws Exception;}
    private static void rejected(Checked run,String reason) {
        IllegalStateException failure=assertThrows(IllegalStateException.class,run::run);
        assertTrue(failure.getMessage().contains(reason),failure.toString());
        System.out.println("GATE_A_NEGATIVE_REJECTED "+reason);
    }
    private static void invoke(String clazz,String method)throws Exception {
        var c=Class.forName(clazz);var constructor=c.getDeclaredConstructor();constructor.setAccessible(true);Object instance=constructor.newInstance();
        var m=c.getDeclaredMethod(method);m.setAccessible(true);
        try{m.invoke(instance);}catch(InvocationTargetException failure){throw new AssertionError(clazz+"#"+method,failure.getCause());}
    }

    /** Uses the actual JUnit Platform from existing verified testRuntimeClasspath.
     * No new dependencies, alternate metadata or standard verifier override. */
    public static void main(String[] args)throws Exception {
        if(args.length==2 && args[0].equals("--verify-fixture")) {
            bootstrap();UiUxMaintenanceSourceContract.verifyRoot(Path.of(args[1]));System.out.println("GATE_A_FIXTURE_ACCEPTED");return;
        }
        boolean legacy=args.length==1 && args[0].equals("--legacy-red");
        if(!legacy && args.length!=0)throw new IllegalArgumentException("Unknown independent launcher mode");
        Class<?> selectors=Class.forName("org.junit.platform.engine.discovery.DiscoverySelectors");var selected=new ArrayList<Object>();
        if(legacy){for(String[] test:ORIGINAL)selected.add(selectors.getMethod("selectMethod",String.class).invoke(null,test[0]+"#"+test[1]));}
        else selected.add(selectors.getMethod("selectClass",Class.class).invoke(null,UiUxMaintenanceGateContractTest.class));
        Class<?> builderType=Class.forName("org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder");Object builder=builderType.getMethod("request").invoke(null);
        builderType.getMethod("selectors",List.class).invoke(builder,selected);Object request=builderType.getMethod("build").invoke(builder);
        Class<?> listenerType=Class.forName("org.junit.platform.launcher.TestExecutionListener"),listenerArray=Array.newInstance(listenerType,0).getClass();
        Class<?> summaryListener=Class.forName("org.junit.platform.launcher.listeners.SummaryGeneratingListener");Object listener=summaryListener.getConstructor().newInstance(),listeners=Array.newInstance(listenerType,1);Array.set(listeners,0,listener);
        Object launcher=Class.forName("org.junit.platform.launcher.core.LauncherFactory").getMethod("create").invoke(null);Class<?> launcherType=Class.forName("org.junit.platform.launcher.Launcher");
        launcherType.getMethod("registerTestExecutionListeners",listenerArray).invoke(launcher,listeners);
        launcherType.getMethod("execute",Class.forName("org.junit.platform.launcher.LauncherDiscoveryRequest"),listenerArray).invoke(launcher,request,Array.newInstance(listenerType,0));
        Object summary=summaryListener.getMethod("getSummary").invoke(listener);Class<?> summaryType=Class.forName("org.junit.platform.launcher.listeners.TestExecutionSummary");
        var writer=new PrintWriter(System.out,true);summaryType.getMethod("printTo",PrintWriter.class).invoke(summary,writer);summaryType.getMethod("printFailuresTo",PrintWriter.class).invoke(summary,writer);
        Map<String,Object> result=new LinkedHashMap<>();result.put("scope",legacy?"LEGACY_RED_FIVE_ORIGINAL_METHODS":"GATE_A_INDEPENDENT_JUNIT_NOT_FULL_STANDARD_SUITE");
        for(String name:List.of("getTestsFoundCount","getTestsSucceededCount","getTestsFailedCount","getTestsSkippedCount","getTestsAbortedCount","getContainersFailedCount"))result.put(name,summaryType.getMethod(name).invoke(summary));
        String receipt=System.getProperty("atd.gatea.receipt");if(receipt==null)throw new IllegalStateException("Explicit receipt required");
        Files.writeString(Path.of(receipt),new GsonBuilder().setPrettyPrinting().create().toJson(result),StandardOpenOption.CREATE_NEW);
        long failed=(long)summaryType.getMethod("getTestsFailedCount").invoke(summary),aborted=(long)summaryType.getMethod("getTestsAbortedCount").invoke(summary),skipped=(long)summaryType.getMethod("getTestsSkippedCount").invoke(summary),found=(long)summaryType.getMethod("getTestsFoundCount").invoke(summary),containers=(long)summaryType.getMethod("getContainersFailedCount").invoke(summary);
        if(failed!=0||aborted!=0||skipped!=0||found==0||containers!=0)throw new AssertionError("Gate A JUnit not green: "+result);
    }
}
