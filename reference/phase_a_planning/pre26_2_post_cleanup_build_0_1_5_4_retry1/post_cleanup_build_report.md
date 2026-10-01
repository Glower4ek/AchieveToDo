# PRE-26.2 post-clean authoritative build 0.1.5.4 — retry1

Status: **PRE_26_2_POST_CLEAN_BUILD_VERIFIED**.
The fresh post-clean distributable is raw-byte-identical to the accepted pre-clean 0.1.5.4 JAR. All required gates meet the explicitly accepted outcomes, including exactly four known historical baseline failures.

## Previous STOP and authorized correction

The previous STOP was correct: the unfiltered run had 391 tests and five failures, including BetterRuOverlayResourceTest.achievetodoRussianLocaleStillCoversAllModOwnedKeys() (`expected 532`, `actual 533`). The human reviewer confirmed the intentional BUG-06 key already present in accepted 0.1.5.4 and authorized one test-only correction. The previous STOP directory remains byte-for-byte unchanged.
Before editing, the exact assertion at line 139 was inspected. The sole semantic patch is:

```diff
-        assertEquals(532, english.size(), "This regression test pins the current AchieveToDo-owned keyset");
+        assertEquals(533, english.size(), "This regression test pins the current AchieveToDo-owned keyset");
```

File: `src/test/java/com/diskree/achievetodo/client/BetterRuOverlayResourceTest.java`.
Before SHA256: `e7f65c3e4eb3ecc211310f2ad697cbbf4617ffb3886fbbf0da5d70640009eaaf`.
After SHA256: `e40570dc905b13be62c891fb721f59d53260621a693bbda1112f9c443c90a6db`.
The EN/RU keyset equality assertion and all other bytes are unchanged. No production/resource/build configuration/dependency change occurred. No historical XML, accepted manifest/report, frozen BACAP or FINAL19 evidence was rewritten.

## A. Pre-build state and environment

Branch `26.2-port`; HEAD `a5e1b49539a39f01bb5db13d44d420e653dce225`; index SHA256 `bd0c1882b97105b93ec41c193496cc5587131df9792f28c7bc89e08d1c33a8df`; no staged changes.
Pre-remediation state: 23 modified tracked files, one intentional tracked deletion, 38,911 untracked files. CommandManagerMixin.java remains deleted.
All 17 approved cleanup targets remain absent. Retained scratch artifacts, frozen BACAP, FINAL19 and accepted 0.1.5.4 records retain their hashes.

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-25.0.4.7-hotspot'
$env:GRADLE_USER_HOME='D:\Vibecode\AchieveToDo 26.2\.gradle-user-home'
$env:TEMP='D:\Vibecode\AchieveToDo 26.2\build\tmp\codex_java_uds_probe'
$env:TMP='D:\Vibecode\AchieveToDo 26.2\build\tmp\codex_java_uds_probe'
```

Every authoritative invocation used direct approval-capable execution with those exact assignments. No sacrificial restricted Gradle attempt, clean, cache deletion, manual javac or JVM/network workaround. Current accepted build configuration remains Gradle 9.8.0 / Loom 1.18.2 / Loader 0.19.5 / Fabric API 0.161.0+26.2.

## B. Authoritative gates and selection

| Gate | Fresh result | Exit code |
|---|---|---|
| Focused BetterRuOverlayResourceTest | 6 tests, 0 failures/errors/skips | 0 |
| compileJava / compileTestJava / compileGametestJava | All freshly executed, GREEN | enclosing unfiltered invocation 1 |
| Unfiltered baseline | 391 tests, exactly 4 known failures, 0 errors/skips | 1, expected historical test failures |
| Exact accepted 0.1.5.4 suite | 103 tests, 0 failures/errors/skips | 0 |
| Fresh preservation validators | All accepted terminal/FINAL19 validators GREEN | included in accepted exit 0 |
| Normal build | BUILD SUCCESSFUL | 0 |

Exact focused and baseline commands:

```powershell
.\gradlew.bat --no-daemon test --tests com.diskree.achievetodo.client.BetterRuOverlayResourceTest
.\gradlew.bat --no-daemon compileJava compileTestJava compileGametestJava test --rerun-tasks
```

The baseline has 391 tests versus the immutable historical 376: AdvancementLinkCommandTest adds 11; MovementTutorialStepHandlerMixinTest grows from 2 to 6 (+4). The corrected BetterRu suite passes all 6 tests in this unfiltered run. No fifth failure, skip, changed failure message or changed stack trace occurred.

Known failures, reconciled by exact name/type/message/stack trace:

- `com.diskree.achievetodo.client.ExternalPackCompatibilityPhaseATest.treatsPinnedHistoricalSourceAsSourceButMarkerCopyAsWorldCopyOnly()` — type `org.opentest4j.AssertionFailedError`; exact message and stack trace match immutable accepted baseline XML.
- `com.diskree.achievetodo.client.ExternalPackCompatibilityTest.admitsOnlyRawOrCurrentCompatiblePackCopies()` — type `org.opentest4j.AssertionFailedError`; exact message and stack trace match immutable accepted baseline XML.
- `com.diskree.achievetodo.client.PhaseALlamaFestivalNbtScopeTest.onlyTheSixteenFrozenCarpetNbtPathsChangeAcrossTheEntireAdvancementArchive()` — type `org.opentest4j.AssertionFailedError`; exact message and stack trace match immutable accepted baseline XML.
- `com.diskree.achievetodo.client.PhaseALlamaFestivalNbtScopeTest.bacapCopyRecordsMappingAndRejectsStaleMarkerWithoutChangingOtherPackMarkerRules(Path)` — type `org.opentest4j.AssertionFailedError`; exact message and stack trace match immutable accepted baseline XML.

Accepted selection: the immutable `pre26_2_smoke_bugfix_0_1_5_4/bugfix.init.gradle` supplies exactly its previously accepted test source adapters. A second init script changes report destinations only, preventing any write into old records. The test names are the 24 suites in the accepted manifest (the 22 prior suites plus the two BUG-05/06 suites); fresh `--rerun-tasks` forces compilation and JUnit. No validator source or predicate was changed.
The exact expanded command and argv are in `evidence/accepted_command.json`.
BetterRuOverlayResourceTest is absent from the original accepted suite definition; no new exclusion was introduced. It was independently run fresh and also passed the unfiltered baseline.

Accepted suite names:

- `com.diskree.achievetodo.AuthorIdentityMetadataTest`
- `com.diskree.achievetodo.certification.Final19CombatAcceptanceTest`
- `com.diskree.achievetodo.certification.Final19CombatStaticTest`
- `com.diskree.achievetodo.certification.Final19DualTagsStaticTest`
- `com.diskree.achievetodo.certification.Final19InventoryMastersStaticTest`
- `com.diskree.achievetodo.certification.Final19LedgerTest`
- `com.diskree.achievetodo.certification.Final19MixedWorldgenStaticTest`
- `com.diskree.achievetodo.certification.Final19PreservedDualTagsAcceptanceTest`
- `com.diskree.achievetodo.certification.Final19PreservedInventoryMastersAcceptanceTest`
- `com.diskree.achievetodo.certification.Final19PreservedMixedWorldgenAcceptanceTest`
- `com.diskree.achievetodo.certification.Final19PreservedVariantsAcceptanceTest`
- `com.diskree.achievetodo.certification.Final19PreservedWorldgenAcceptanceTest`
- `com.diskree.achievetodo.certification.Final19RaiderIntegrityAcceptanceTest`
- `com.diskree.achievetodo.certification.Final19ShieldAcceptanceTest`
- `com.diskree.achievetodo.certification.Final19VariantsStaticTest`
- `com.diskree.achievetodo.certification.Final19WaxGateRegressionAcceptanceTest`
- `com.diskree.achievetodo.certification.Final19WorldgenStaticTest`
- `com.diskree.achievetodo.certification.Pre26SmokeCauldronGateAcceptanceTest`
- `com.diskree.achievetodo.certification.Pre26SmokeTerminalAcceptanceTest`
- `com.diskree.achievetodo.client.AdvancementLinkCommandTest`
- `com.diskree.achievetodo.client.Pre26SmokeExternalPackCompatibilityTest`
- `com.diskree.achievetodo.client.Pre26SmokeRegressionTest`
- `com.diskree.achievetodo.injection.mixin.client.MovementTutorialStepHandlerMixinTest`
- `com.diskree.achievetodo.MixinsConfigTest`

Exact normal build:

```powershell
.\gradlew.bat --no-daemon build -x test -x runGameTest
# Forced attempt: existing archive guard refused overwrite, exit 1
.\gradlew.bat --no-daemon build -x test -x runGameTest --rerun-tasks
# After accepted archive preservation: fresh jar executed, exit 0
.\gradlew.bat --no-daemon build -x test -x runGameTest
```

The initial normal build exited 0 but jar was UP-TO-DATE. A forced `--rerun-tasks` attempt exited 1 at the existing build.gradle:360 smoke-candidate guard, which refuses same-version overwrites. The accepted output was then preserved by a nonrecursive move to a hash-verified build/tmp backup (a second saved copy also exists). The exact normal command was rerun with the output destination absent: jar executed freshly and the build exited 0. The guard, build configuration and version were not changed or disabled. No clean or cache deletion occurred. All three logs/exit codes and archive preservation paths/hashes are retained.

## C. 1152 / 1152 fresh preservation

Fresh `Pre26SmokeTerminalAcceptanceTest` (3 tests) and all accepted FINAL19 validators passed within the fresh accepted-suite run. They reconcile 1152 unique advancements, exact frozen requirement groups, persistent proof hashes, all seven completed families, native zero-gain controls and all 1229 converted output fingerprints.
Product-certified **1152/1152**, `productUncertified=0`, FINAL19 **CLOSED**: 19 targets / 79 groups / 108 criteria. Historical Phase A remains **1133/1152**, with immutable historical roll-up bytes. Frozen BACAP SHA256 `8c72314535c5df7b4416bf0f38310371ec8aec537fde1445bdc820a3c9aada70` is unchanged; it was not regenerated.
Accepted artifact-local persistent validation is the fresh preservation mechanism; no CLOSED native GameTest campaign was rerun and no new regime was introduced.

## D. Newly rebuilt post-clean JAR

- Path: `D:\Vibecode\AchieveToDo 26.2\build\libs\achievetodo-mc26.2+0.1.5.4.jar`.
- Size: **1,546,595 bytes**.
- SHA256: **0fc29fd0fbe1d874ca037ba2bd932f4e667593d95a1f2d3bb42936765710978b**.
- Entries: **1911** (1777 files / 134 directories).
- Production classes: **232**.
- Processed resource files: **1540**; non-class file entries: 1545.
- Mixin references: **157**, all resolve; all entrypoints resolve.
- Version 0.1.5.4; author Glower4ek; source https://github.com/Glower4ek/AchieveToDo.
- LICENSE_achievetodo matches current LICENSE bytes; `Copyright (c) 2026 Glower4ek`.
- BuildConfig.class matches fresh production output; MOD_NAME AchieveToDo, MOD_ID achievetodo, MOD_VERSION 0.1.5.4. SHA256 `9da4f65ee843786408a18b7281e556e3261e3d71568c3fa1ff8f7fddf70ae3eb`.
- Technical namespace `com.diskree.achievetodo.*` preserved.
- Current accepted production marker **compat_26_2_r16**; historical FINAL19 marker **compat_26_2_r15**. Neither changed.

All production classes match fresh compiled output. All processed resources match packaged contents; only the established Loom nested-library metadata is added to fabric.mod.json.

## E. Accepted pre-clean versus freshly rebuilt post-clean

**Byte-identical**: same size and SHA256 as accepted pre-clean 0.1.5.4. Added entries **0**, removed entries **0**, changed entry contents **0**. All ZIP metadata and archive bytes match; no archive-level nondeterminism was observed.
The current 26.2 `jar` packaging task executed freshly; the resulting JAR has a newer modification time than the saved accepted pre-clean copy. This comparison is against a newly rebuilt artifact, unlike the retained-only audit in the STOP record.

Leakage audit: no test, GameTest-only, probe/debug, planning/reference, build/tmp, scratch, external Search/Fullscreen implementation or unrelated external mod classes. No test/GameTest class-output overlap and no hard optional implementation references; duplicate-entry and ZIP CRC checks pass.
Only the existing nested dependency remains: META-INF/jars/toml4j-0.7.2.jar, 63581 bytes, SHA256 `0b4a366e00a7019fb0e3df81ba749183afe037eb276d6d0792950326124ac466`, byte-identical to accepted pre-clean contents.

## F. Durable retry record

This new directory contains `post_cleanup_build_manifest.json`, `post_cleanup_build_report.md`, `record_hashes.json` and `evidence/` with exact commands, logs, exit codes, fresh XML, baseline reconciliation, pin diff/hashes, preservation and JAR comparison/audit. Manifest/report hashes are stored separately after writing.
The previous STOP directory and all older accepted records remain byte-for-byte preserved.

## G. Git and preservation boundary

Of 3258 pre-remediation protected files, **3257 remain byte-identical** and the only difference is the authorized one-byte test pin. The old 2724-file historical protection set has **2723 unchanged files plus that one authorized test correction**. All pre-existing accepted product deltas remain exact.
Branch, HEAD, index and staged state are unchanged. The sole new tracked delta is BetterRuOverlayResourceTest.java; all pre-existing production/resource tracked deltas and the intentional deletion remain unchanged.
Final untracked-path reconciliation separates new retry evidence from expected Gradle-generated outputs and Minecraft Test-worker logger rollovers (`logs/2026-10-01-4.log.gz`, `logs/2026-10-01-5.log.gz`). These archived runtime logs contain the ordinary JUnit worker output; they are not source/resource changes. No unexpected repository change is accepted. All 17 cleanup targets remain absent.
No release finalization, Phase B, Git mutation, staging, commit or push occurred.

## H. Anomalies

No new or changed failure remains. The four historical failures are explicitly preserved and reconciled, not silently filtered. The r16 current / r15 historical distinction is the human-confirmed accepted state.
The existing smoke archive guard refused the forced overwrite attempt (exit 1). This was resolved by preserving the accepted archive in a verified backup and rerunning the exact normal build, which rebuilt the JAR successfully (exit 0). No guard/configuration/version change occurred.

Final status: **PRE_26_2_POST_CLEAN_BUILD_VERIFIED**.
