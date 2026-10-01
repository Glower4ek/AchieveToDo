# PRE-26.2 final production build

PRE_26_2_FINAL_BUILD_READY_FOR_USER_SMOKE

The production JAR is built and audited. User installation and manual acceptance are pending. No cleanup, Phase B, publication, push, branch change or production edit occurred.

## Installable artifact

- Filename: `achievetodo-mc26.2+0.1.5.jar`
- Absolute path: `D:\Vibecode\AchieveToDo 26.2\build\libs\achievetodo-mc26.2+0.1.5.jar`
- Repository-relative path: `build/libs/achievetodo-mc26.2+0.1.5.jar`
- Size: **1,528,289 bytes**
- SHA-256: `ABA207B7D9873CB1A3073894A0948D7953316578AB0FAB2C6D88B86DAB070F69`
- SHA-1: `1E07A5288B79B85AADADC4E78E85F052FBD6BBD6`
- File modification time, metadata only: `2026-09-30T21:10:38.125614+00:00` (2026-10-01 02:10:38 Asia/Yekaterinburg).
- Metadata: mod ID `achievetodo`, mod version `0.1.5`, Minecraft `~26.2`, Java `>=25`, Fabric Loader `>=0.19.3`, Fabric API required. Validated against Fabric API `0.158.0+26.2`.

Gradle confirms `assemble -> :jar`, whose unclassified output is this exact file. The current Minecraft 26.2 Loom pipeline is unobfuscated and has no `remapJar` task. There are no configured dev, sources, test or shadow JAR archive outputs to install. `META-INF/jars/toml4j-0.7.2.jar` is the intentional bundled library, not the user installable artifact.

## Authoritative build and gates

Wrapper: `.\gradlew.bat`, Gradle 9.5.1, Loom 1.17.19, Java 25.0.4. Direct approval-capable routing used throughout; no restricted-sandbox Gradle attempt.

Accepted environment:

```text
JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.4.7-hotspot
GRADLE_USER_HOME=D:\Vibecode\AchieveToDo 26.2\.gradle-user-home
TEMP=D:\Vibecode\AchieveToDo 26.2\build\tmp\codex_java_uds_probe
TMP=D:\Vibecode\AchieveToDo 26.2\build\tmp\codex_java_uds_probe
project root=D:\Vibecode\AchieveToDo 26.2
```

Pre-release gates used the accepted `tools/final19/final19.init.gradle` and the release inspection init script. `compileJava`, `compileTestJava`, and `compileGametestJava` were explicitly rerun and GREEN. The exact test filters and complete wrapper arguments are retained in `gate_arguments.json`.

JUnit: **72 tests, 0 failures, 0 errors, 0 skipped**: 38 accepted FINAL19 terminal/static/persistent acceptance and mixin checks, plus 34 compatibility checks with the isolated current marker fixture. No evidence-writing Raider production tests were rerun. Accepted Axe 14-case, Cauldron 24-case and Raider 16-case native regression evidence was read and hash-validated.

Final standard production build command:

```powershell
.\gradlew.bat --no-daemon -I reference/phase_a_planning/pre26_2_final_build/release_inspection.init.gradle build -x test -x runGameTest pre26BuildMetadata
```

Release task `build`, archive task `:jar`, Gradle exit **0**, **BUILD SUCCESSFUL**. `validateAccessWidener` also passed. Explicit test exclusions preserve the selected passed JUnit gates and prevent Fabric's automatic broad GameTest dependency. No Gradle clean or ad-hoc artifact assembly.

## Production and certification preservation

Product certification remains **1152 / 1152**, uncertified 0; 19/19 FINAL19 targets, 79/79 requirement groups, 108 criteria, 7/7 families durably GREEN. Historical Phase A remains **1133 / 1152**.

- Product ledger SHA-256: `49F001FFE29719D03E1DFC5F75048E1424F7F681458C2D709828872D4EB11349`
- FINAL19 terminal reconciliation SHA-256: `1AB689C783C6A7E85D2E84DD9C526B2D4333934171B81B4A22D5AE1E4454E962`
- Frozen BACAP SHA-256: `8C72314535C5DF7B4416BF0F38310371EC8AEC537FDE1445BDC820A3C9AADA70`
- Frozen BACAP SHA-1: `45B8BB0076BBF5B92FDE7DC9590C6686937ABBC0`

Read-only checks matched all 442 FINAL19 durable hashes and all 1530 preserved inputs, applying only the three exact existing authorized production exceptions. Final verification preserved all 2539 snapshotted source/resource/test/FINAL19 files.

Three production fixes remain exact:

| Production file | Authorized post SHA-256 |
| --- | --- |
| AxeItemMixin.java | `4247EF44CE7E331F43820D889ECEF541F8CBB0A200A31EDE2764C8ECBDD46A13` |
| AbstractCauldronBlockAccessor.java | `63C46D242EEECABE946BE353A8A2353A5EF6555D93982F8E0F8C6846CA4DFD2E` |
| ServerPlayerGameModeCauldronGateMixin.java | `3F21C817198C59FD650C6384E5CC73BCDA2B743EF252003B00EC8278D5193E78` |
| achievetodo.mixins.json | `56FCB41D75273382681CBEEDE45882A876DE68BABCD6580AE63D5C20C0AC211F` |
| ExternalPackCompatibility.java | `1BEA3BA9CAF22963D1EF26DE69B19534A4B16D1EAB85A39C5E1FC17750227A31` |

The corresponding authorization records, proposal hashes and exact production path boundary were verified. Compiled classes for these fixes match the packaged classes byte-for-byte; per-file fingerprints are retained in the manifest and `jar_content_audit.json`. `feeling_ill` five-branch repair and `dungeon_crawler` three false branches remain GREEN through retained hash-bound native evidence and current acceptance checks.

## JAR audit

Valid ZIP CRC and unique entries: **1901 entries**. All **222 production classes** match fresh Gradle main output byte-for-byte, all **1540 production resources** are present and match source bytes (with expected Fabric metadata expansion), and all **156 production mixin/accessor references** resolve. Entrypoints, icon, class tweaker and nested dependency references resolve. JSON resources parse.

No test source classes, FINAL19 probes, certification classes, GameTest entrypoints, generated Java source, planning/reference files, diagnostic logs, TEMP, proposal patches or diagnostic JSON were packaged. No duplicate root Fabric metadata, accidental local Windows paths or high-confidence credential signatures were found, including nested JAR inspection. This signature scan is bounded inspection, not a universal secret-detection guarantee.

Frozen BACAP is deliberately external pinned input. Existing `resourcepacks/*` production overrides and compatibility item tags are intentionally packaged. Current runtime handling uses `compat_26_2_r15`, pinned source SHA-1, root override SHA-1, `llamaCarpetNbtMapping=equipment.body` and `raiderPredicateKeys=snake_case`. The compatibility JUnit suite validates the normal production copy path. No Phase B updates were made.

## Retained diagnostics and limitations

The first additional compatibility suite attempt had 72 tests with one failure: historical `ExternalPackCompatibilityTest.admitsOnlyRawOrCurrentCompatiblePackCopies`, line 867, expects acceptance of a current marker missing `raiderPredicateKeys=snake_case`. The strict current production converter correctly rejects it. The historical test was preserved unchanged. A separate release-only copy adds exactly the missing fixture property, renames the class, and keeps all 34 original assertions. Its provenance and exact hashes are in `stale_fixture_diagnosis.json`; initial failure output is retained under `diagnostics/initial_gate_failure/`.

The first `build -x test` attempt unexpectedly launched `runGameTest`, because Fabric attaches it independently to build/check. It began a broad batch of 101 tests. The owned process was interrupted, exit -1; none of these results were accepted, promoted or claimed as certification. Full output remains under `diagnostics/unintended_native_dependency/`. The corrected final build explicitly excluded `runGameTest`, and its recorded task graph contains no native runtime task.

That diagnostic launch changed the scratch `build/run/gameTest/world/datapacks/bacap.zip` byte hash to `A6176360EB07B398E635BA967E5C3EDF72B145E7CA493AAD4F1F4373C46CD66C`. This scratch output is not artifact identity or durable certification. The current marker and **all 1229 advancement definition fingerprints** still match accepted production semantics, with zero differences. Every durable FINAL19 file, ledger, terminal reconciliation, historical file and production file remained unchanged. The semantic check is retained beside the incident; no accepted terminal runtime hash was rewritten.

No separate bounded production-artifact headless startup test was performed. The interrupted development GameTest launch is not release startup evidence or user acceptance. Manual smoke is required.

## Git/worktree safety

HEAD `a5e1b49539a39f01bb5db13d44d420e653dce225`, branch `26.2-port`. Index SHA-256 `bd0c1882b97105b93ec41c193496cc5587131df9792f28c7bc89e08d1c33a8df` preserved. Initial dirty status matches final status. `build.gradle` remains at its accepted preserved hash; unrelated dirty work remains intact. This stage wrote only build outputs and its dedicated `reference/phase_a_planning/pre26_2_final_build/` namespace. No Git mutations, cleanup, deletion, cache deletion, clean, publication, push or Phase B work occurred.

## User manual smoke — pending

Use a separate Minecraft 26.2 Fabric profile with Java 25, Loader 0.19.3 or compatible, and Fabric API 0.158.0+26.2. Install this single AchieveToDo JAR in the profile's `mods` directory, avoiding a second AchieveToDo version. Keep a backup of any existing world used for smoke.

1. Launch the client. Confirm AchieveToDo initializes and the title/menu works without a crash.
2. Create an AchieveToDo world with normal BACAP setup, then load an existing compatible world if available. Confirm required packs activate through the mod's normal flow. For the normal BACAP source use the pinned historical pack, not a Phase B/latest substitute.
3. Open advancement and ability UI. Check categories/icons/text load; no advancement/predicate parse errors appear in logs. Complete a few ordinary advancements through gameplay and confirm progress and the advancement counter.
4. Check one locked ability and its unlocked state through progression: the locked action is blocked, the unlocked action works, and normal unlock feedback appears.
5. Axe: try stripping a log, scraping oxidized copper and removing wax while the relevant tool/landmark gate is locked, then unlocked. Locked attempts must not alter blocks, consume durability or award action progress; unlocked attempts should work normally.
6. Cauldron: try a representative fill/empty or wash interaction while locked, then unlocked. Locked attempts must not consume/produce items, alter the cauldron or award advancement progress; unlocked interaction should work.
7. If practical, test a normal raid-captain/raider path (e.g. Voluntary Exile under its actual gameplay conditions), and shield blocking of a skeleton arrow. Watch for normal advancement behavior and item/damage behavior.
8. Save and exit, reload, then disconnect/rejoin the integrated world (or reconnect to the same applicable environment). Confirm progression, abilities and packs persist. Review `latest.log` for crashes, mixin failures, parse errors and repeated unexpected mod errors.

Record pass/fail with the artifact SHA-256, world/profile used, failed action if any, and relevant log lines. **User smoke has not passed yet.** Explicit user acceptance or rejection is the next step; cleanup remains deferred.

## Durable build records

Manifest: `pre26_2_final_build_manifest.json`, SHA-256 `EBB7BDE740748A8C4FD519875A7DB063CDD4183E3E9E2E0602D52768A2BA4B6A`.
Report: `pre26_2_final_build_report.md`; its SHA-256 and the manifest SHA-256 are recorded in `pre26_2_final_build_record_hashes.json` (outside these two files to avoid self-referential hashes).
