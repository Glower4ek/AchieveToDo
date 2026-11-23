import org.gradle.api.tasks.JavaExec

import org.gradle.api.tasks.SourceSetContainer

import org.gradle.kotlin.dsl.the

import java.nio.charset.StandardCharsets

import java.nio.file.Files

import java.security.MessageDigest



plugins {

    java

}



group = providers.gradleProperty("group").get()

version = providers.gradleProperty("version").get()



repositories {

    mavenCentral()

    maven("https://repo.papermc.io/repository/maven-public/")

}



dependencies {

    compileOnly("io.papermc.paper:paper-api:26.2.build.112-stable")

    testImplementation("io.papermc.paper:paper-api:26.2.build.112-stable")

}



dependencyLocking {

    lockAllConfigurations()

}



tasks.withType<AbstractArchiveTask>().configureEach {

    isPreserveFileTimestamps = false

    isReproducibleFileOrder = true

}



java {

    toolchain {

        languageVersion.set(JavaLanguageVersion.of(25))

    }

    withSourcesJar()

}



tasks.withType<JavaCompile>().configureEach {

    options.encoding = "UTF-8"

    options.release.set(25)

}



tasks.processResources {

    filteringCharset = "UTF-8"

    inputs.property("pluginVersion", project.version)

    filesMatching("paper-plugin.yml") {

        expand("version" to project.version.toString())

    }

}



tasks.jar {

    archiveBaseName.set("AchieveToDo-Purpur-26.2")

    // Release defaults first, then the unchanged certified native bootstrap (NativeTransactionPremain).
    manifest.attributes["Premain-Class"] = "com.diskree.achievetodo.core.ReleaseLaunchPremain"

}



tasks.test {

    enabled = false

}



val sourceSets = the<SourceSetContainer>()








tasks.register<JavaExec>("generateEmbeddedDatapack") {

    group = "build setup"

    description = "Materializes the deterministic embedded datapack into src/main/resources/datapack."

    dependsOn("classes")

    classpath = sourceSets.named("main").get().runtimeClasspath

    mainClass.set("com.diskree.achievetodo.datapack.EmbeddedDatapackMaterializerMain")

    args(layout.projectDirectory.dir("src/main/resources/datapack").asFile.absolutePath)

    workingDir = projectDir

}



tasks.register<JavaExec>("coreTest") {

    group = "verification"

    description = "Runs the pure Java core and embedded datapack tests."

    dependsOn("testClasses")

    classpath = sourceSets.named("test").get().runtimeClasspath

    mainClass.set("com.diskree.achievetodo.tests.Stage2CoreTestMain")

    workingDir = projectDir

    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(25)) })

}



tasks.register<JavaExec>("foundationTest") {

    group = "verification"

    description = "Verifies lifecycle rollback, cleanup ownership and readiness transitions."

    dependsOn("testClasses")

    classpath = sourceSets.named("test").get().runtimeClasspath

    mainClass.set("com.diskree.achievetodo.core.FoundationContractTest")

    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(25)) })

    workingDir = projectDir

}















tasks.check {

    dependsOn("pinnedStartupTest")

    dependsOn("startupModePolicyTest")

    dependsOn("releaseLaunchTest")

    dependsOn("landmarkNativeTest")

    dependsOn("gameplayTest")

    dependsOn("s3bTest")

    dependsOn("progressionTest")

    dependsOn("bridgeTest")

    dependsOn("coreTest")

    dependsOn("foundationTest")

}



tasks.register<JavaExec>("startupModePolicyTest") {
    group = "verification"
    description = "A start without an explicit gameplay mode must fail closed with operator guidance."
    dependsOn("testClasses")
    classpath = sourceSets.named("test").get().runtimeClasspath
    mainClass.set("com.diskree.achievetodo.core.StartupModePolicyTest")
    workingDir = projectDir
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(25)) })
}

tasks.register<JavaExec>("releaseLaunchTest") {
    group = "verification"
    description = "A start with only -javaagent must yield exactly the certified property set."
    dependsOn("testClasses")
    classpath = sourceSets.named("test").get().runtimeClasspath
    mainClass.set("com.diskree.achievetodo.core.ReleaseLaunchTest")
    workingDir = projectDir
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(25)) })
}

tasks.register<JavaExec>("pinnedStartupTest") {

    group = "verification"

    dependsOn("testClasses")

    classpath = sourceSets.named("test").get().runtimeClasspath

    mainClass.set("com.diskree.achievetodo.core.PinnedStartupContractTest")

    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(25)) })

}



tasks.register<JavaExec>("landmarkNativeTest") {

    group = "verification"

    description = "Verifies S4G exact hull contracts and optional pinned native transforms; no complete-ID certification."

    dependsOn("testClasses")

    classpath = sourceSets.named("test").get().runtimeClasspath

    mainClass.set("com.diskree.achievetodo.gameplay.nativeguard.NativeLandmarkContractTest")

    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(25)) })

}



tasks.register<JavaExec>("landmarkSupplementTest") {

    group = "verification"

    description = "Verifies S4N supplemental transforms against the exact complete opt-in native pipeline."

    dependsOn("testClasses")

    classpath = sourceSets.named("test").get().runtimeClasspath

    mainClass.set("com.diskree.achievetodo.gameplay.nativeguard.NativeLandmarkSupplementContractTest")

    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(25)) })

}



tasks.register<JavaExec>("gameplayTest") {

    group = "verification"

    description = "Verifies the S4A ability gates and cancellable Paper event adapters."

    dependsOn("testClasses")

    classpath = sourceSets.named("test").get().runtimeClasspath

    mainClass.set("com.diskree.achievetodo.gameplay.GameplayEnforcementTest")

    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(25)) })

}



tasks.register<JavaExec>("progressionTest") {

    group = "verification"

    description = "Runs S3A progression, world binding, reconciliation and content identity contracts."

    dependsOn("testClasses")

    classpath = sourceSets.named("test").get().runtimeClasspath

    mainClass.set("com.diskree.achievetodo.tracking.bukkit.ProgressionFoundationTest")

    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(25)) })

    environment("ACHIEVETODO_TEST_BACAP", System.getenv("ACHIEVETODO_TEST_BACAP") ?: "")

}



tasks.register<JavaExec>("bridgeTest") {

    group = "verification"

    description = "Runs bounded Bridge protocol/session contracts, not real E2E."

    dependsOn("testClasses")

    classpath = sourceSets.named("test").get().runtimeClasspath

    mainClass.set("com.diskree.achievetodo.bridge.BridgeContractTest")

    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(25)) })

}



tasks.register<JavaExec>("s3bTest") {

    group = "verification"

    description = "Verifies S3B thresholds, authoritative config persistence and actual team membership."

    dependsOn("testClasses")

    classpath = sourceSets.named("test").get().runtimeClasspath

    mainClass.set("com.diskree.achievetodo.tracking.bukkit.TeamConfigSemanticsTest")

    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(25)) })

}






tasks.register<JavaExec>("portalNativeTest") {

    group = "verification"

    dependsOn("testClasses")

    classpath = sourceSets.named("test").get().runtimeClasspath

    mainClass.set("com.diskree.achievetodo.gameplay.nativeguard.NativePortalContractTest")

    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(25)) })

}



tasks.register<JavaExec>("movementNativeTest") {

    group = "verification"

    dependsOn("testClasses")

    classpath = sourceSets.named("test").get().runtimeClasspath

    mainClass.set("com.diskree.achievetodo.gameplay.nativeguard.NativeMovementContractTest")

    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(25)) })

}


// Explicit executable contract suite; the ordinary test task remains disabled by platform design.
tasks.register<JavaExec>("gameplayAdmissionTest") {
    group = "verification"
    dependsOn("testClasses")
    classpath = sourceSets.named("test").get().runtimeClasspath
    mainClass.set("com.diskree.achievetodo.core.GameplayAdmissionTest")
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(25)) })
    workingDir = projectDir
}
tasks.check { dependsOn("gameplayAdmissionTest") }

tasks.register<JavaExec>("sourceReloadTest") {
    group = "verification"
    dependsOn("testClasses")
    classpath = sourceSets.named("test").get().runtimeClasspath
    mainClass.set("com.diskree.achievetodo.gameplay.nativeguard.SourceReloadContractTest")
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(25)) })
    workingDir = projectDir
}
tasks.check { dependsOn("sourceReloadTest") }

// Canonical denial transport vectors; no runtime certification is inferred.
tasks.register<JavaExec>("bridgeDenialIntentTest") {
    group = "verification"
    dependsOn("testClasses")
    classpath = sourceSets.named("test").get().runtimeClasspath
    mainClass.set("com.diskree.achievetodo.bridge.BridgeDenialIntentTest")
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(25)) })
}
tasks.check { dependsOn("bridgeDenialIntentTest") }

tasks.register<JavaExec>("gameplayDenialEffectsTest") {
    group = "verification"
    dependsOn("testClasses")
    classpath = sourceSets.named("test").get().runtimeClasspath
    mainClass.set("com.diskree.achievetodo.gameplay.GameplayDenialEffectsTest")
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(25)) })
}
tasks.check { dependsOn("gameplayDenialEffectsTest") }

tasks.register<JavaExec>("nativeCombatScopeTest") {
    group = "verification"
    dependsOn("testClasses")
    classpath = sourceSets.named("test").get().runtimeClasspath
    mainClass.set("com.diskree.achievetodo.gameplay.nativeguard.NativeCombatScopeContractTest")
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(25)) })
}
tasks.check { dependsOn("nativeCombatScopeTest") }

tasks.register<JavaExec>("nativeCombatTargetTest") {
    group = "verification"
    dependsOn("testClasses")
    classpath = sourceSets.named("test").get().runtimeClasspath
    mainClass.set("com.diskree.achievetodo.gameplay.nativeguard.NativeCombatTargetContractTest")
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(25)) })
}
tasks.check { dependsOn("nativeCombatTargetTest") }

tasks.register<JavaExec>("nativeDenialIntentTest") {
    group = "verification"
    dependsOn("testClasses")
    classpath = sourceSets.named("test").get().runtimeClasspath
    mainClass.set("com.diskree.achievetodo.gameplay.nativeguard.NativeDenialIntentContractTest")
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(25)) })
}
tasks.check { dependsOn("nativeDenialIntentTest") }
tasks.register<JavaExec>("criterionNativeTest") {
    group = "verification"
    description = "W5-RED-1: pinned ITEM_USED_ON_BLOCK suppression transform and one-shot denial marks."
    dependsOn("testClasses")
    classpath = sourceSets.named("test").get().runtimeClasspath
    mainClass.set("com.diskree.achievetodo.gameplay.nativeguard.NativeCriterionContractTest")
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(25)) })
}
tasks.check { dependsOn("criterionNativeTest") }

// Native contract suites that were not part of check before 0.3.0.
tasks.check { dependsOn("movementNativeTest", "portalNativeTest", "landmarkSupplementTest") }

tasks.register<JavaExec>("chatNativeTest") {
    group = "verification"
    dependsOn("testClasses")
    classpath = sourceSets.named("test").get().runtimeClasspath
    mainClass.set("com.diskree.achievetodo.gameplay.nativeguard.NativeChatContractTest")
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(25)) })
    workingDir = projectDir
}
tasks.check { dependsOn("chatNativeTest") }
