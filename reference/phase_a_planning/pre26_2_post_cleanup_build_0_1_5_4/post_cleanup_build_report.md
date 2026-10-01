# Post-clean build verification 0.1.5.4 — STOP

The additional failure `com.diskree.achievetodo.client.BetterRuOverlayResourceTest.achievetodoRussianLocaleStillCoversAllModOwnedKeys()` is outside the four accepted historical baseline failures. The required STOP boundary was applied immediately: no further Gradle gate, accepted-suite rerun or distributable build was run. No source, test or resource was edited to pass it.

## A. Pre-build state

Branch `26.2-port`; HEAD `a5e1b49539a39f01bb5db13d44d420e653dce225`; no staged changes. Index SHA256 `bd0c1882b97105b93ec41c193496cc5587131df9792f28c7bc89e08d1c33a8df`.
23 modified tracked files, one intentional tracked deletion, 38,811 untracked files at the pre-build checkpoint. `CommandManagerMixin.java` remains deleted.
All 17 approved cleanup targets were absent before execution and remain absent. Both retained scratch artifacts, frozen BACAP, FINAL19 evidence and accepted 0.1.5.4 manifest/report exist and retain their hashes. Full read-only status and retained-item hashes are in `evidence/pre_build_git_status.txt` and `evidence/pre_build_verification.json`.

## B. Authoritative gates

Exact executed command:

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-25.0.4.7-hotspot'
$env:GRADLE_USER_HOME='D:\Vibecode\AchieveToDo 26.2\.gradle-user-home'
$env:TEMP='D:\Vibecode\AchieveToDo 26.2\build\tmp\codex_java_uds_probe'
$env:TMP='D:\Vibecode\AchieveToDo 26.2\build\tmp\codex_java_uds_probe'
.\gradlew.bat --no-daemon compileJava compileTestJava compileGametestJava test --rerun-tasks
```

Direct approval-capable execution (`require_escalated`), automatic approval review; no restricted Gradle attempt, JVM/network workaround, clean, cache deletion or manual javac.
Current accepted configuration uses Gradle 9.8.0, Loom 1.18.2, Loader 0.19.5 and Fabric API 0.161.0+26.2. The older Phase A skill toolchain values were not substituted for current 0.1.5.4 configuration.

| Gate | Result |
|---|---|
| compileJava | Freshly executed, GREEN |
| compileTestJava | Freshly executed, GREEN |
| compileGametestJava | Freshly executed, GREEN |
| Historical unfiltered JUnit | 391 tests, 5 failures, 0 errors, 0 skipped; enclosing command exit 1 |
| Accepted 0.1.5.4 JUnit suite | NOT RUN after STOP; prior accepted result is 103/103, not a current result |
| `.\gradlew.bat --no-daemon build -x test -x runGameTest` | NOT RUN after STOP |
| Native GameTest/runtime rerun | NOT RUN; accepted route excludes closed campaigns |

`--rerun-tasks` forced fresh compilation and test execution without deleting caches or outputs. The baseline has 391 tests versus the historical 376: AdvancementLinkCommandTest adds 11; MovementTutorialStepHandlerMixinTest grows from 2 to 6 (+4). There are no skips.

The four known failures are separately reconciled against immutable 0.1.5.4 baseline XML:

- `com.diskree.achievetodo.client.ExternalPackCompatibilityPhaseATest.treatsPinnedHistoricalSourceAsSourceButMarkerCopyAsWorldCopyOnly()` — exact failure type, message and stack trace match accepted baseline.
- `com.diskree.achievetodo.client.ExternalPackCompatibilityTest.admitsOnlyRawOrCurrentCompatiblePackCopies()` — exact failure type, message and stack trace match accepted baseline.
- `com.diskree.achievetodo.client.PhaseALlamaFestivalNbtScopeTest.onlyTheSixteenFrozenCarpetNbtPathsChangeAcrossTheEntireAdvancementArchive()` — exact failure type, message and stack trace match accepted baseline.
- `com.diskree.achievetodo.client.PhaseALlamaFestivalNbtScopeTest.bacapCopyRecordsMappingAndRejectsStaleMarkerWithoutChangingOtherPackMarkerRules(Path)` — exact failure type, message and stack trace match accepted baseline.

The additional failure is at `src/test/java/com/diskree/achievetodo/client/BetterRuOverlayResourceTest.java:139`: `expected 532`, `actual 533` EN keys. It passed in the saved pre-bugfix baseline XML. Both current EN/RU keysets contain 533 keys and are equal. The retained accepted 0.1.5.4 JAR already contains the added `achievetodo.advancement_navigation_unavailable` key; 0.1.5.3 had 532. Current locales match accepted 0.1.5.4 manifest hashes. This traces the failure to an unchanged accepted locale addition versus an older fixed-count assertion, but does not waive the additional-failure STOP rule.
Exact XML, types/messages/traces, suite deltas and read-only analysis are retained in `evidence/baseline-junit/`, `baseline_comparison.json` and `additional_failure_analysis.json`.

## C. 1152 / 1152 preservation

Durable accepted state remains preserved: product-certified 1152/1152, uncertified 0, FINAL19 CLOSED (19 targets / 79 groups / 108 criteria). Historical Phase A remains 1133/1152. Frozen BACAP SHA256 remains `8c72314535c5df7b4416bf0f38310371ec8aec537fde1445bdc820a3c9aada70`.
The accepted preservation mechanism is `Pre26SmokeTerminalAcceptanceTest` plus retained FINAL19 acceptance validators. Its fresh rerun was NOT reached after STOP. No fresh 1152/1152 verification is claimed. Immutable evidence, ledgers, catalogs, source and resource hashes are preserved.

## D. JAR audit

No new post-clean JAR was built. The existing accepted JAR at `D:\Vibecode\AchieveToDo 26.2\build\libs\achievetodo-mc26.2+0.1.5.4.jar` remains:

- Size: 1,546,595 bytes.
- SHA256: `0fc29fd0fbe1d874ca037ba2bd932f4e667593d95a1f2d3bb42936765710978b`.
- Entries: 1,911 (1,777 files / 134 directories).
- Production classes: 232; non-class file entries: 1,545; processed main resource files: 1,540.
- Mixin references: 157, all resolve; all entrypoints resolve.
- Metadata: version 0.1.5.4, author Glower4ek, source https://github.com/Glower4ek/AchieveToDo.
- LICENSE_achievetodo: exact current LICENSE bytes; `Copyright (c) 2026 Glower4ek`.
- BuildConfig.class: exact newly compiled output, MOD_NAME AchieveToDo, MOD_ID achievetodo, MOD_VERSION 0.1.5.4. SHA256 `9da4f65ee843786408a18b7281e556e3261e3d71568c3fa1ff8f7fddf70ae3eb`.
- Namespace: `com.diskree.achievetodo.*`.
- All 232 newly compiled production classes match the retained JAR byte-for-byte. All processed resources match (only the established Loom nested-library metadata is added to fabric.mod.json).
- No test, GameTest, probe, planning, reference, temporary diagnostic, build/tmp, scratch, optional Search/Fullscreen implementation or unrelated external classes. No overlap with test/GameTest class outputs; no hard optional implementation references.
- Existing embedded dependency only: toml4j-0.7.2.jar, 63,581 bytes, SHA256 `0b4a366e00a7019fb0e3df81ba749183afe037eb276d6d0792950326124ac466`, preserved exactly.

This audit describes the retained artifact and compiled outputs; it is not proof of a post-clean distributable rebuild.

## E. Pre-clean versus post-clean comparison

The retained accepted archive and saved pre-build copy are byte-identical: added entries 0, removed entries 0, changed entries 0. Archive SHA256 and size are unchanged. No timestamp/order/compression explanation is needed for these retained bytes.
The requested comparison against a newly built post-clean archive is NOT AVAILABLE because the build was stopped. Do not confuse unchanged retained bytes with successful rebuild equivalence.

## F. Durable record

- `post_cleanup_build_manifest.json`
- `post_cleanup_build_report.md`
- `evidence/`: pre/post Git status, verification and preservation snapshots, exact baseline log/exit/XML, baseline comparison, additional failure analysis, retained-JAR audit and entry inventories.

Manifest/report SHA256 values are recorded separately in `record_hashes.json` after writing, avoiding self-referential hashes. Older manifests/reports were not rewritten.

## G. Git and preservation check

All 3,160 pre-build protected files are byte-identical; all 2,724 historical protected files match the accepted 0.1.5.4 preservation mechanism. Existing accepted source changes remain exact. HEAD, branch, index bytes and the 24 tracked status entries are unchanged; staged changes remain empty.
Expected generated changes are confined to build/Gradle outputs and this new durable record. Generated BuildConfig source retains identical bytes. No unexpected production/test/resource or protected-file change occurred. Final untracked-path reconciliation is included in the manifest.
No Git mutation, version bump, release finalization, Phase B, commit or push occurred.

## H. Anomalies and disposition

1. Additional baseline failure: BetterRuOverlayResourceTest key count 532 versus 533. STOP; no fix/filter/waiver.
2. Marker description mismatch: immutable current accepted source and JAR already use `compat_26_2_r16`; historical FINAL19 evidence uses `compat_26_2_r15`. Both were preserved. User clarification was requested; this discrepancy does not authorize changing the accepted product.

Final status: **STOP**. `PRE_26_2_POST_CLEAN_BUILD_VERIFIED` is not issued.
