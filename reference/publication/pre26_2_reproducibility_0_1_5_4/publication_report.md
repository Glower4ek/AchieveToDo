# PRE-26.2 publication remediation — STOP

The fresh publication probe found a new test failure. No accepted-suite run, runtime BACAP bootstrap or product build followed. No Git mutation occurred.

## A. Modifications made

Changed `.gitignore` and `src/test/java/com/diskree/achievetodo/certification/PhaseALocationMovementCertification.java` (durable input path; immutable output provenance retained). Of 3217 originally protected files compared, the other 3215 remained byte-identical.

Created these authorized reproduction inputs:

- `PRE26_RELEASE_REPRODUCIBILITY.md`
- `reference/localization/fixtures/bacap_1.21.provenance.json`
- `reference/localization/fixtures/bacap_1.21.zip`
- `reference/publication/fixtures/generated_gameplay_resources_sha256.json`
- `reference/publication/fixtures/historical_0_1_5.provenance.json`
- `reference/publication/fixtures/historical_0_1_5_achievetodo.mixins.json`
- `tools/publication/accepted.init.gradle`
- `tools/publication/historical-retry-report-routing.init.gradle`
- `tools/publication/java/com/diskree/achievetodo/certification/Pre26SmokeAcceptedSources.java`
- `tools/publication/java/com/diskree/achievetodo/certification/PublicationRuntimeSetup.java`
- `tools/publication/report-routing.init.gradle`
- `tools/publication/snapshot.py`
- `tools/publication/verify.ps1`

Created the STOP evidence files beneath `reference/publication/pre26_2_reproducibility_0_1_5_4/`; the exact list is in `final-commit-set.json`.
Made the existing 303 accepted gameplay resources publication-visible; no resource bytes changed. Disposable probe/cache/output files beneath build/tmp are excluded.

## B. Resolved previous 12 withheld items

- EXCLUDE `.agents/skills/achievetodo-phase-a/SKILL.md` and `.agents/skills/phase-a-certification/SKILL.md`, and the entire local toolkit.
- EXCLUDE `src/data/resources/fabric.mod.json` (unused source set metadata).
- INCLUDE these eight immutable binary result files:

  - `reference/phase_a_planning/pre26_2_post_cleanup_build_0_1_5_4/evidence/baseline-junit/binary/output-events.bin`
  - `reference/phase_a_planning/pre26_2_post_cleanup_build_0_1_5_4/evidence/baseline-junit/binary/results-generic.bin`
  - `reference/phase_a_planning/pre26_2_post_cleanup_build_0_1_5_4_retry1/evidence/baseline-junit/binary/output-events.bin`
  - `reference/phase_a_planning/pre26_2_post_cleanup_build_0_1_5_4_retry1/evidence/baseline-junit/binary/results-generic.bin`
  - `reference/phase_a_planning/pre26_2_post_cleanup_build_0_1_5_4_retry1/evidence/focused-junit/binary/output-events.bin`
  - `reference/phase_a_planning/pre26_2_post_cleanup_build_0_1_5_4_retry1/evidence/focused-junit/binary/results-generic.bin`
  - `reference/phase_a_planning/pre26_2_smoke_bugfix_0_1_5_4/baseline-junit/binary/output-events.bin`
  - `reference/phase_a_planning/pre26_2_smoke_bugfix_0_1_5_4/baseline-junit/binary/results-generic.bin`
- Localization ZIP: INCLUDE durable `reference/localization/fixtures/bacap_1.21.zip` and provenance; EXCLUDE original cache path. Exact bytes preserved; no tracked byte-identical retained copy was found.

All 12 reviewer decisions are honored. The manual exclusion exposed an additional reproducibility dependency in the baseline test; it is not resolved by silently including the manual.

## C. Reproducibility gaps

- `1_historicalJar`: RESOLVED_INPUT: exact 5434-byte entry fixture; identical validator assertions, accepted execution pending
- `2_runtimeBacap`: NOT_RESOLVED_VERIFICATION: verified-copy/conversion bootstrap implemented; not executed after STOP
- `3_generatedGameplay`: RESOLVED_INPUT: 303 visible durable resources all byte-identical to accepted source and JAR
- `4_gradleLoom`: RESOLVED_METADATA: exact pins/bootstrap command; empty-home network bootstrap NOT_VERIFIED
- `5_reportRouting`: RESOLVED_INPUT: exact historical durable copy and current build/tmp routing; accepted execution pending
- `6_excludedAgentManual`: NOT_RESOLVED: baseline test consumes excluded .agents manual; requires future durable contract extraction without weakened assertions

## D. Candidate include set

This is a reconciled candidate set, not authorization/readiness to stage. `final-commit-set.json` enumerates every exact untracked path and tracked delta. Unchanged tracked Phase A preservation/freeze records and eight frozen ZIPs are inherited.

| Path | State | Category | Why |
|---|---|---|---|
| `.gitignore` | tracked-modified | RELEASE_BUILD_CONFIG | Accepted pinned build/toolchain; publication hygiene for .gitignore |
| `LICENSE` | tracked-modified | RELEASE_DOCUMENTATION | Accepted release identity/documentation |
| `README.md` | tracked-modified | RELEASE_DOCUMENTATION | Accepted release identity/documentation |
| `build.gradle` | tracked-modified | RELEASE_BUILD_CONFIG | Accepted pinned build/toolchain; publication hygiene for .gitignore |
| `gradle.properties` | tracked-modified | RELEASE_BUILD_CONFIG | Accepted pinned build/toolchain; publication hygiene for .gitignore |
| `gradle/BuildConfig.java.txt` | tracked-modified | RELEASE_BUILD_CONFIG | Accepted pinned build/toolchain; publication hygiene for .gitignore |
| `gradle/wrapper/gradle-wrapper.jar` | tracked-modified | RELEASE_BUILD_CONFIG | Accepted pinned build/toolchain; publication hygiene for .gitignore |
| `gradle/wrapper/gradle-wrapper.properties` | tracked-modified | RELEASE_BUILD_CONFIG | Accepted pinned build/toolchain; publication hygiene for .gitignore |
| `gradlew` | tracked-modified | RELEASE_BUILD_CONFIG | Accepted pinned build/toolchain; publication hygiene for .gitignore |
| `gradlew.bat` | tracked-modified | RELEASE_BUILD_CONFIG | Accepted pinned build/toolchain; publication hygiene for .gitignore |
| `src/main/java/com/diskree/achievetodo/ability/AbilityType.java` | tracked-modified | RELEASE_PRODUCT | Accepted 0.1.5.4 production state; intentional removal for deleted CommandManagerMixin |
| `src/main/java/com/diskree/achievetodo/client/AchieveToDoClient.java` | tracked-modified | RELEASE_PRODUCT | Accepted 0.1.5.4 production state; intentional removal for deleted CommandManagerMixin |
| `src/main/java/com/diskree/achievetodo/client/ExternalPackCompatibility.java` | tracked-modified | RELEASE_PRODUCT | Accepted 0.1.5.4 production state; intentional removal for deleted CommandManagerMixin |
| `src/main/java/com/diskree/achievetodo/injection/mixin/client/GuiMixin.java` | tracked-modified | RELEASE_PRODUCT | Accepted 0.1.5.4 production state; intentional removal for deleted CommandManagerMixin |
| `src/main/java/com/diskree/achievetodo/injection/mixin/client/MinecraftClientMixin.java` | tracked-modified | RELEASE_PRODUCT | Accepted 0.1.5.4 production state; intentional removal for deleted CommandManagerMixin |
| `src/main/java/com/diskree/achievetodo/injection/mixin/client/MovementTutorialStepHandlerMixin.java` | tracked-modified | RELEASE_PRODUCT | Accepted 0.1.5.4 production state; intentional removal for deleted CommandManagerMixin |
| `src/main/java/com/diskree/achievetodo/injection/mixin/main/AxeItemMixin.java` | tracked-modified | RELEASE_PRODUCT | Accepted 0.1.5.4 production state; intentional removal for deleted CommandManagerMixin |
| `src/main/java/com/diskree/achievetodo/injection/mixin/main/CommandManagerMixin.java` | deleted | RELEASE_PRODUCT | Accepted 0.1.5.4 production state; intentional removal for deleted CommandManagerMixin |
| `src/main/java/com/diskree/achievetodo/server/AchieveToDoServer.java` | tracked-modified | RELEASE_PRODUCT | Accepted 0.1.5.4 production state; intentional removal for deleted CommandManagerMixin |
| `src/main/resources/achievetodo.mixins.json` | tracked-modified | RELEASE_PRODUCT | Accepted 0.1.5.4 production state; intentional removal for deleted CommandManagerMixin |
| `src/main/resources/assets/achievetodo/lang/en_us.json` | tracked-modified | RELEASE_PRODUCT | Accepted 0.1.5.4 production state; intentional removal for deleted CommandManagerMixin |
| `src/main/resources/assets/achievetodo/lang/ru_ru.json` | tracked-modified | RELEASE_PRODUCT | Accepted 0.1.5.4 production state; intentional removal for deleted CommandManagerMixin |
| `src/main/resources/fabric.mod.json` | tracked-modified | RELEASE_PRODUCT | Accepted 0.1.5.4 production state; intentional removal for deleted CommandManagerMixin |
| `src/test/java/com/diskree/achievetodo/client/BetterRuOverlayResourceTest.java` | tracked-modified | RELEASE_TEST | Accepted regression coverage, including authorized 532 -> 533 correction |
| `src/test/java/com/diskree/achievetodo/client/ExternalPackCompatibilityTest.java` | tracked-modified | RELEASE_TEST | Accepted regression coverage, including authorized 532 -> 533 correction |
| `src/test/java/com/diskree/achievetodo/injection/mixin/client/MovementTutorialStepHandlerMixinTest.java` | tracked-modified | RELEASE_TEST | Accepted regression coverage, including authorized 532 -> 533 correction |

| Safe untracked scope | Files | Category | Why |
|---|---:|---|---|
| `src/main/java/` | 10 | RELEASE_PRODUCT | 12 accepted new product paths including 10 Java files and two resource tags |
| `src/main/resources/` | 2 | RELEASE_PRODUCT | Accepted nether_fungus/nether_roots item tags |
| `src/test/` | 255 | RELEASE_TEST | Accepted reusable validators, catalogs and regression coverage |
| `src/gametest/` | 105 | RELEASE_TEST | Accepted native validation infrastructure |
| `src/main/generated/data/achievetodo/advancement/abilities/` | 152 | RELEASE_PRODUCT | 152 accepted generated gameplay JSON inputs |
| `src/main/generated/data/achievetodo/function/abilities/` | 151 | RELEASE_PRODUCT | 151 accepted generated gameplay mcfunction inputs |
| `tools/final19/` | 63 | DURABLE_TEST_INFRASTRUCTURE | Accepted FINAL19 validation/sealing tools; __pycache__ excluded |
| `tools/publication/` | 7 | DURABLE_TEST_INFRASTRUCTURE | Authorized current reproduction/fixture/bootstrap route; stopped probe limits apply |
| `reference/phase_a_planning/` | 980 | DURABLE_EVIDENCE / HISTORICAL_RECORD | Accepted Phase A/FINAL19/PRE-26.2 records including STOP and retry1, eight sealed binary files; empty audit_stdout and pycache excluded |
| `reference/localization/fixtures/` | 2 | DURABLE_TEST_INFRASTRUCTURE | Exact durable localization ZIP with provenance |
| `reference/publication/` | 18 | DURABLE_EVIDENCE / DURABLE_TEST_INFRASTRUCTURE | Minimal historical-byte fixture, generated-resource seals and current STOP evidence |
| `PRE26_RELEASE_REPRODUCIBILITY.md` | 1 | RELEASE_DOCUMENTATION | Current route, pins, expected outcomes and actual STOP status |

## E. Exclude set

All these local scopes are ignored by the final .gitignore. Do not stage or force-add them:

- `.gradle/`, `.gradle-user-home/`, `.idea/`, `build/`, `run/`, `dist/`, `logs/`: dependency/project caches, binaries, test outputs and local worlds. The final and historical JARs remain outside Git.
- `src/main/generated/*` except the exact 303 gameplay files enumerated in `generated_gameplay_resources_sha256.json`: generated BuildConfig Java, datagen cache and transient metadata.
- `reference/phase_a_preservation/files/*` except already tracked `final/*.zip`: local downloaded/backup material. Frozen final ZIPs remain included.
- `/.agents/`, `/skills-lock.json`, `/src/data/resources/fabric.mod.json`: reviewer exclusions.
- `/reference/localization/.official_audit_cache/`, `/reference/localization/.bacap_language_pack_1.21.zip`, `/reference/phase_a_preservation/author-search/`: cache/diagnostics and redundant archive.
- `/tmp_*`, `/tmp-*.jar`, `/_tmp*/`, `/audittmp/`, `/$out`, `/META-INF/`, `/data/`, `/net/`, `/version.json`, `__pycache__/`: scratch/decompile/cache/backup outputs.
- `/hs_err_pid*.log`, `/replay_pid*.log`, `/runclient.*.log`, `/runGameTest-output.txt`, `/datagen-fail.log`: local runtime/build diagnostics.
- `/evidence/worlds/new_world_biomes_root_regression_2026-08-19/`: diagnostic world backup.
- `/reference/phase_a_planning/pre26_2_smoke_bugfix_0_1_5_4/audit_stdout.json`: empty unsealed output.
Sealed XML/log/binary evidence, durable ZIP fixtures, frozen BACAP and wrapper JAR are included. There are no blanket archive/XML/log ignore rules.

## F. Fresh-snapshot probe

- Initially contained only 3230 proposed committed files; no Git metadata, cache, build output, worlds, agent toolkit or release JAR. Input hashes still match after execution.
- SOURCE-COMPLETE: NO. Declared fixtures/resources passed checks, but the baseline requires an excluded agent manual.
- Empty Gradle home: attempted; wrapper download timed out at 10000ms. NETWORK-BOOTSTRAP-VERIFIED: NO.
- Execution used a copied accepted dependency cache inside the disposable snapshot. Compilation of main/test/gametest passed.
- Baseline: 391 tests, 5 failures, 0 errors, 0 skipped. The four historical failures match sealed name/type/message/trace exactly.
- New failure: `PhaseATerminalReconciliationTest.frozenRequirementsAndTerminalPlanningRemainIndependent()` reads `.agents/skills/achievetodo-phase-a/SKILL.md` at source line 61.
- Accepted suite, runtime bootstrap, build, rebuilt JAR size/hash/comparison: NOT RUN after STOP.
- Frozen BACAP, FINAL19 seals and all protected product/evidence inputs are unchanged. Historical accepted state remains 1152/1152, uncertified 0, FINAL19 CLOSED; fresh accepted revalidation is pending.
- Existing accepted JAR unchanged: 1546595 bytes, SHA256 `0fc29fd0fbe1d874ca037ba2bd932f4e667593d95a1f2d3bb42936765710978b`. Keep it outside Git and attach it to a later GitHub Release.

## G. Exact .gitignore diff

```diff
diff --git a/.gitignore b/.gitignore
index bebc370..13d2535 100644
--- a/.gitignore
+++ b/.gitignore
@@ -2,5 +2,43 @@
 .idea/
 build/
 run/
-src/main/generated/
-reference/phase_a_preservation/files/
+src/main/generated/*
+!src/main/generated/data/
+src/main/generated/data/**
+!src/main/generated/data/achievetodo/
+!src/main/generated/data/achievetodo/advancement/
+!src/main/generated/data/achievetodo/advancement/abilities/
+!src/main/generated/data/achievetodo/advancement/abilities/*.json
+!src/main/generated/data/achievetodo/function/
+!src/main/generated/data/achievetodo/function/abilities/
+!src/main/generated/data/achievetodo/function/abilities/*.mcfunction
+reference/phase_a_preservation/files/*
+!reference/phase_a_preservation/files/final/
+reference/phase_a_preservation/files/final/*
+!reference/phase_a_preservation/files/final/*.zip
+/.gradle-user-home/
+/dist/
+/logs/
+/hs_err_pid*.log
+/replay_pid*.log
+/runclient.*.log
+/runGameTest-output.txt
+/datagen-fail.log
+/tmp_*
+/tmp-*.jar
+/_tmp*/
+/audittmp/
+/$out
+/META-INF/
+/data/
+/net/
+/version.json
+/reference/localization/.bacap_language_pack_1.21.zip
+/reference/localization/.official_audit_cache/
+/reference/phase_a_preservation/author-search/
+__pycache__/
+/.agents/
+/skills-lock.json
+/src/data/resources/fabric.mod.json
+/evidence/worlds/new_world_biomes_root_regression_2026-08-19/
+/reference/phase_a_planning/pre26_2_smoke_bugfix_0_1_5_4/audit_stdout.json
```

## H. Git state

- Branch `26.2-port`, HEAD `a5e1b49539a39f01bb5db13d44d420e653dce225`, remotes unchanged.
- Index SHA256 `bd0c1882b97105b93ec41c193496cc5587131df9792f28c7bc89e08d1c33a8df` unchanged; staged empty.
- No Git mutation, commit, tag, push or Phase B.

## I. Publication status

PUBLICATION_REMEDIATION_STOP

Blocker: the baseline depends on an explicitly excluded local agent manual. A subsequent authorized remediation must separate the exact tested historical roll-up/frontier contract into durable test evidence/infrastructure, preserving assertions and the 391-test baseline. Then repeat the publication-only probe, accepted 103-test route, runtime setup and exact JAR build. No readiness token is issued.
