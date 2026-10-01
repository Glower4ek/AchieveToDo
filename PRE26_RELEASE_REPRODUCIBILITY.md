# AchieveToDo 0.1.5.4 publication reproduction

Publication probe status (2026-10-01): **READY_FOR_SELECTIVE_STAGE**. The new publication-only retry snapshot passed an empty-home network bootstrap, the exact 391-test/four-historical-failure baseline, the 103/103 accepted suite, verified r16 runtime BACAP preparation and fresh 1152/1152 preservation. Normal build reproduced the accepted JAR byte-for-byte with zero added, removed or changed entries. No copied-cache fallback was needed. Successful evidence and the exact selective publication set are in `reference/publication/pre26_2_reproducibility_0_1_5_4_retry1/`; the previous STOP record remains unchanged.

The accepted product targets Minecraft 26.2 and Java 25. The verified Windows JDK is Eclipse Adoptium 25.0.4.7 at `C:\Program Files\Eclipse Adoptium\jdk-25.0.4.7-hotspot`. Use the committed Gradle 9.8.0 wrapper; Loom 1.18.2, Fabric Loader 0.19.5, Fabric API 0.161.0+26.2, JUnit 6.1.3 and toml4j 0.7.2 remain pinned in the build metadata.

From a fresh checkout in PowerShell 7:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-25.0.4.7-hotspot'
$env:GRADLE_USER_HOME = Join-Path (Get-Location) '.gradle-user-home'
$env:TEMP = Join-Path (Get-Location) 'build/tmp/codex_java_uds_probe'
$env:TMP = $env:TEMP
New-Item -ItemType Directory -Force -Path $env:TEMP | Out-Null
./tools/publication/verify.ps1 -Phase All
```

The terminal regression reads the existing durable `reference/phase_a_planning/phase_a_terminal_reconciliation_20260930.json`; no duplicate terminal fixture or agent manual is required. Its historical checkpoint seals runtimePartial 0, totalCertified 1133, unfinished 19, frontier SHA256 `2cb3d3f835a1d8f1eda97ec782725a72936f346dd8cfeed75da61fc4d9f36662` and canonical roll-up SHA256 `439f2aa1c577ffbee275525765ac9229bf5633f0f5e1da498c0adc5702bd2810`. The test freshly hashes both files and compares the full canonical summary. The local SKILL SHA256 `22448f14632d290cee8b0639c88d51f1ee992cafd504231a57cb77384aa9a594` is provenance only, verified once during remediation. Historical Phase A remains 1133/1152, distinct from the FINAL19 product state 1152/1152.

The script assigns those variables itself. `-JavaHome` permits another Java 25 installation. Keep the Gradle user home beneath the checkout: retained validators locate resolved Mojang artifacts there. The wrapper pins distribution SHA256 `bafd5ce9cfaea0fbccfdc8439a1ac42fbd4cd9c89dc9a988228d8a2639a58e6c`. An empty user home requires network access to Gradle, Fabric, Mojang and Maven repositories. No dependency caches, old release JARs, world saves or agent tools are committed.

The first dependency/bootstrap command is `./gradlew.bat --no-daemon compileJava compileTestJava compileGametestJava`. Normal compilation regenerates BuildConfig. The 303 accepted gameplay inputs under `src/main/generated/data/achievetodo/advancement/abilities/` and `src/main/generated/data/achievetodo/function/abilities/` are committed and individually pinned by `reference/publication/fixtures/generated_gameplay_resources_sha256.json`; normal build does not need datagen.

The durable historical localization/catalog comparison fixture is `reference/localization/fixtures/bacap_1.21.zip`, SHA256 `c71d1aa1a84dbe00a3f85a42144b46214c4669a3cccf07ff66631d28f16a99b2`. Its provenance JSON records the original Modrinth version, URL and hashes. The active catalog generator reads this fixture while preserving its immutable historical output/provenance label. This comparison archive does not replace frozen production BACAP.

The current reusable `tools/publication/java/.../Pre26SmokeAcceptedSources.java` reads only `reference/publication/fixtures/historical_0_1_5_achievetodo.mixins.json`: 5434 exact bytes, SHA256 `56fcb41d75273382681cbeede45882a876de68babcd6580ae63d5c20c0ac211f`. The source artifact was `achievetodo-mc26.2+0.1.5.jar`, SHA256 `aba207b7d9873cb1a3073894a0948d7953316578ab0fab2c6d88b86dab070f69`. JSON equality and historical byte-hash assertions remain unchanged. The sealed older adapters are preserved and are not the current reproduction route.

Run the unfiltered historical baseline with `./tools/publication/verify.ps1 -Phase Baseline`. Its exact Gradle command is `./gradlew.bat --no-daemon compileJava compileTestJava compileGametestJava test --rerun-tasks`. Expected: 391 tests, exactly four immutable historical failures, zero errors and skips; Gradle exits 1. The script compares each failure name, type, message and trace against the sealed retry1 XML. The four failures are:

- `ExternalPackCompatibilityPhaseATest.treatsPinnedHistoricalSourceAsSourceButMarkerCopyAsWorldCopyOnly()`
- `ExternalPackCompatibilityTest.admitsOnlyRawOrCurrentCompatiblePackCopies()`
- `PhaseALlamaFestivalNbtScopeTest.onlyTheSixteenFrozenCarpetNbtPathsChangeAcrossTheEntireAdvancementArchive()`
- `PhaseALlamaFestivalNbtScopeTest.bacapCopyRecordsMappingAndRejectsStaleMarkerWithoutChangingOtherPackMarkerRules(Path)`

Run the accepted route with `./tools/publication/verify.ps1 -Phase Accepted`. It executes:

```powershell
./gradlew.bat --no-daemon --init-script tools/publication/accepted.init.gradle --init-script tools/publication/report-routing.init.gradle compileJava compileTestJava compileGametestJava test --rerun-tasks # plus --tests for each of the 24 immutable manifest suites
```

The script expands the exact 24-suite selection from `reference/phase_a_planning/pre26_2_smoke_bugfix_0_1_5_4/bugfix_manifest.json`, recording argv beneath `build/tmp/publication_validation`. Expected: 103/103 GREEN, zero failures/errors/skips, 1152/1152 product-certified, productUncertified 0 and FINAL19 CLOSED. Historical Phase A remains 1133/1152.

Before accepted tests, `preparePublicationRuntime` verifies frozen `reference/phase_a_preservation/files/final/bacap.zip` at SHA256 `8c72314535c5df7b4416bf0f38310371ec8aec537fde1445bdc820a3c9aada70`, copies and verifies identical bytes at `build/tmp/publication_runtime_input/bacap.zip`, then invokes the unchanged production converter for `build/run/pre26Smoke01/world/datapacks/bacap.zip`. It verifies the current compatibility marker and all 1229 accepted converted definition hashes, then rechecks the frozen source. The reviewer explicitly authorized this conversion: a raw frozen ZIP alone cannot satisfy the terminal validator. No native CLOSED campaign or runtime world save is needed.

The exact historical retry report-routing init script is preserved at `tools/publication/historical-retry-report-routing.init.gradle`. The current route uses `tools/publication/report-routing.init.gradle` with fresh build/tmp destinations. Historical reports are not rewritten or overwritten.

Run `./tools/publication/verify.ps1 -Phase Build`, which executes the accepted normal command `./gradlew.bat --no-daemon build -x test -x runGameTest`. The accepted JAR also contains two deterministic datagen HashCache records. Before packaging, the script reconstructs their exact CRLF bytes from the committed resources' SHA1 values and original provider names/header, checking SHA256 `c4459e1f7645ca5bca1158ec3aa8a4e1522809485ba05f94e742fe7c09706085` and `b99fb23e7393773edff38f3f1df88990f3b165cbee8c7200116938676b268841`. The cache outputs remain ignored and uncommitted; no datagen or product behavior is changed. From a fresh output directory the expected JAR is `build/libs/achievetodo-mc26.2+0.1.5.4.jar`, 1546595 bytes, SHA256 `0fc29fd0fbe1d874ca037ba2bd932f4e667593d95a1f2d3bb42936765710978b`. The script stops on any mismatch. The existing same-version archive-overwrite guard remains enabled; do not rerun packaging over an existing archive.

Current production marker: `compat_26_2_r16`. Immutable FINAL19 receipts retain `compat_26_2_r15`; the accepted test adapters reconcile that distinction. Release identity remains Glower4ek, source `https://github.com/Glower4ek/AchieveToDo`, technical namespace `com.diskree.achievetodo.*`.

For a publication-only probe, run `tools/publication/snapshot.py` using Python 3.12 or newer with `--destination build/tmp/publication_probe/snapshot --inventory build/tmp/publication_remediation/probe-inputs.json`. It selects current tracked files and the explicit publication allowlist, copies regular inputs with per-file SHA256 verification, omits the intentional tracked deletion, rejects symlinks, and refuses an existing destination. The destination initially has no Git directory, build output, Gradle caches or external references. Run the PowerShell verification script from that destination. A cache-assisted execution must be reported separately from an empty-home network bootstrap.
