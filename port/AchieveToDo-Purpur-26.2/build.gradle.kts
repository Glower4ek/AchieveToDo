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
    manifest.attributes["Premain-Class"] = "com.diskree.achievetodo.gameplay.nativeguard.NativeTransactionPremain"
}

tasks.test {
    enabled = false
}

val sourceSets = the<SourceSetContainer>()
val uiResourcePackSourceDir = layout.projectDirectory.dir("src/main/resource-pack")
val generatedUiResourcePackDir = layout.buildDirectory.dir("generated/ui-resource-pack")
val preparedUiResourcePackDir = layout.buildDirectory.dir("prepared/ui-resource-pack")
val uiResourcePackOutputDir = layout.buildDirectory.dir("resource-pack")
val uiResourcePackModelKeys = listOf(
    "ability_unknown",
    "filler",
    "info",
    "navigation_close",
    "navigation_next",
    "navigation_next_disabled",
    "navigation_previous",
    "navigation_previous_disabled",
    "tab_actions",
    "tab_actions_selected",
    "tab_blocks",
    "tab_blocks_selected",
    "tab_food",
    "tab_food_selected",
    "tab_landmark",
    "tab_landmark_selected",
    "tab_main",
    "tab_main_selected",
    "tab_trading",
    "tab_trading_selected",
    "tab_upgrade",
    "tab_upgrade_selected"
)

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

tasks.register("generateUiResourcePackModels") {
    group = "build setup"
    description = "Generates deterministic item-model wrapper JSONs for the AchieveToDo UI resource pack."
    outputs.dir(generatedUiResourcePackDir)
    doLast {
        val outputDir = generatedUiResourcePackDir.get().asFile.toPath()
        val itemsDir = outputDir.resolve("assets/achievetodo/items/ui")
        val modelsDir = outputDir.resolve("assets/achievetodo/models/item/ui")
        val texturesDir = outputDir.resolve("assets/achievetodo/textures/item/ui")
        Files.createDirectories(itemsDir)
        Files.createDirectories(modelsDir)
        Files.createDirectories(texturesDir)
        for (key in uiResourcePackModelKeys) {
            val itemDefinition = """
                {
                  "model": {
                    "type": "minecraft:model",
                    "model": "achievetodo:item/ui/$key"
                  }
                }
            """.trimIndent() + "\n"
            val modelDefinition = """
                {
                  "parent": "minecraft:item/generated",
                  "textures": {
                    "layer0": "achievetodo:item/ui/$key"
                  }
                }
            """.trimIndent() + "\n"
            Files.writeString(itemsDir.resolve("$key.json"), itemDefinition, StandardCharsets.UTF_8)
            Files.writeString(modelsDir.resolve("$key.json"), modelDefinition, StandardCharsets.UTF_8)
        }
    }
}

tasks.register<Sync>("prepareUiResourcePack") {
    group = "build"
    description = "Prepares the AchieveToDo UI resource pack source tree for archiving."
    dependsOn("generateUiResourcePackModels")
    from(uiResourcePackSourceDir)
    from(generatedUiResourcePackDir)
    into(preparedUiResourcePackDir)
}

val buildUiResourcePack = tasks.register<Zip>("buildUiResourcePack") {
    group = "build"
    description = "Builds the standalone AchieveToDo client resource pack ZIP."
    dependsOn("prepareUiResourcePack")
    from(preparedUiResourcePackDir)
    destinationDirectory.set(uiResourcePackOutputDir)
    archiveBaseName.set("AchieveToDo-ResourcePack-26.2")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("")
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

val writeUiResourcePackSha256 = tasks.register("writeUiResourcePackSha256") {
    group = "build"
    description = "Writes the SHA-256 checksum for the standalone AchieveToDo resource pack ZIP."
    dependsOn(buildUiResourcePack)
    outputs.file(uiResourcePackOutputDir.map { it.file("AchieveToDo-ResourcePack-26.2-${project.version}.zip.sha256") })
    doLast {
        val zipFile = buildUiResourcePack.get().archiveFile.get().asFile.toPath()
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(Files.readAllBytes(zipFile))
        val hex = digest.digest().joinToString("") { "%02x".format(it) }
        val checksumFile = zipFile.resolveSibling(zipFile.fileName.toString() + ".sha256")
        Files.writeString(
            checksumFile,
            hex + "  " + zipFile.fileName + "\n",
            StandardCharsets.UTF_8
        )
    }
}

tasks.check {
    dependsOn("pinnedStartupTest")
    dependsOn("landmarkNativeTest")
    dependsOn("gameplayTest")
    dependsOn("s3bTest")
    dependsOn("progressionTest")
    dependsOn("bridgeTest")
    dependsOn("coreTest")
    dependsOn("foundationTest")
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

tasks.build {
    dependsOn(writeUiResourcePackSha256)
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
