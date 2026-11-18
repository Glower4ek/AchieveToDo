package com.diskree.achievetodo.certification;

import com.google.gson.GsonBuilder;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Focused recovery only: real JUnit lifecycle, never full-suite acceptance. */
public final class Stage1FocusedContractLauncher {
    private static final List<String> METHODS = List.of(
        "com.diskree.achievetodo.certification.GateBInputAuthorityContractTest#originalConsumerAssertionsAndIndependentPythonControls",
        "com.diskree.achievetodo.certification.UiUxMaintenanceGateContractTest#fullSourceClosureAndOriginalFiveBodies",
        "com.diskree.achievetodo.client.BetterRuOverlayResourceTest#productionRuOverlayMatchesCurrentB6ManualDecisions",
        "com.diskree.achievetodo.client.BetterRuOverlayResourceTest#productionRuOverlayResolvesCurrentPhaseBEffectiveConsumers",
        "com.diskree.achievetodo.client.Pre26SmokeRegressionTest#everyShippedAdvancementMessageRetainsHoverClickAndFrameMetadata");
    private Stage1FocusedContractLauncher() {}
    public static void main(String[] args) throws Exception {
        if (args.length != 0) throw new IllegalArgumentException("No selectors or alternate modes accepted");
        String receipt = System.getProperty("atd.stage1.focused.receipt");
        if (receipt == null || !Path.of(receipt).isAbsolute() || Files.exists(Path.of(receipt)))
            throw new IllegalStateException("Fresh absolute focused receipt required");
        for (String key : List.of("achievetodo.python", "achievetodo.minecraftJar", "achievetodo.vanillaRu", "achievetodo.vanillaRuSha1")) {
            if (System.getProperty(key, "").isBlank()) throw new IllegalStateException("Missing verified provider property: " + key);
        }
        Class<?> selectors = Class.forName("org.junit.platform.engine.discovery.DiscoverySelectors");
        var selected = new ArrayList<Object>();
        for (String method : METHODS) selected.add(selectors.getMethod("selectMethod", String.class).invoke(null, method));
        Class<?> builderType = Class.forName("org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder");
        Object builder = builderType.getMethod("request").invoke(null);
        builderType.getMethod("selectors", List.class).invoke(builder, selected);
        Object request = builderType.getMethod("build").invoke(builder);
        Class<?> listenerType = Class.forName("org.junit.platform.launcher.TestExecutionListener");
        Class<?> listenerArray = Array.newInstance(listenerType, 0).getClass();
        Class<?> summaryListener = Class.forName("org.junit.platform.launcher.listeners.SummaryGeneratingListener");
        Object listener = summaryListener.getConstructor().newInstance();
        Object listeners = Array.newInstance(listenerType, 1); Array.set(listeners, 0, listener);
        Object launcher = Class.forName("org.junit.platform.launcher.core.LauncherFactory").getMethod("create").invoke(null);
        Class<?> launcherType = Class.forName("org.junit.platform.launcher.Launcher");
        launcherType.getMethod("registerTestExecutionListeners", listenerArray).invoke(launcher, listeners);
        launcherType.getMethod("execute", Class.forName("org.junit.platform.launcher.LauncherDiscoveryRequest"), listenerArray)
            .invoke(launcher, request, Array.newInstance(listenerType, 0));
        Object summary = summaryListener.getMethod("getSummary").invoke(listener);
        Class<?> summaryType = Class.forName("org.junit.platform.launcher.listeners.TestExecutionSummary");
        var writer = new PrintWriter(System.out, true);
        summaryType.getMethod("printTo", PrintWriter.class).invoke(summary, writer);
        summaryType.getMethod("printFailuresTo", PrintWriter.class).invoke(summary, writer);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scope", "STAGE1_FOCUSED_FIVE_NOT_FULL_JUNIT"); result.put("methods", METHODS);
        for (String name : List.of("getTestsFoundCount", "getTestsSucceededCount", "getTestsFailedCount", "getTestsSkippedCount", "getTestsAbortedCount", "getContainersFailedCount"))
            result.put(name, summaryType.getMethod(name).invoke(summary));
        Files.writeString(Path.of(receipt), new GsonBuilder().setPrettyPrinting().create().toJson(result), StandardOpenOption.CREATE_NEW);
        if (!result.get("getTestsFoundCount").equals(5L) || !result.get("getTestsSucceededCount").equals(5L)
                || !result.get("getTestsFailedCount").equals(0L) || !result.get("getTestsSkippedCount").equals(0L)
                || !result.get("getTestsAbortedCount").equals(0L) || !result.get("getContainersFailedCount").equals(0L))
            throw new AssertionError("Focused five not green: " + result);
    }
}
