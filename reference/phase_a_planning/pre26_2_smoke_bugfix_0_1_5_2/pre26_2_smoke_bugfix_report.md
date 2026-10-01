# PRE-26.2 smoke bugfix candidate 0.1.5.2

Status: **PRE_26_2_SMOKE_BUGFIX_BUILD_0_1_5_2_READY**. User acceptance remains pending.

Normal Gradle distributable: `D:\Vibecode\AchieveToDo 26.2\build\libs\achievetodo-mc26.2+0.1.5.2.jar`. Size: **1538886 bytes**. Internal mod version: **0.1.5.2**; project version: **mc26.2+0.1.5.2**.

- SHA-256: `7d6f303aa23ee5c188c315197f4c7e4f20c3256b8ec4d6d78fab9909c523f7ba`
- SHA-1: `c27e10e7f0fdf26daaef4e0cdd3a8218df3749be`
- Build/runtime: Minecraft 26.2, Java 25.0.4+7-LTS (target 25), Loader 0.19.5, Fabric API 0.161.0+26.2, Loom 1.18.2, Gradle 9.8.0.
- Git HEAD: `a5e1b49539a39f01bb5db13d44d420e653dce225`; branch: `26.2-port`. Git index unchanged. Unrelated dirty work preserved.

## Four authorized bug transactions

BUG-01: Component JSON inside legacy SNBT string tags became literal item text in 26.2. The shared converter decodes custom_name, namespaced item_name and separate lore lines before the native item parser consumes them. It retains translated keys, nested siblings, color/bold/italic and modern/ordinary names. All **215** function producers were audited: **214 custom_name trophies**, **one item_name parchment**, and **two SNBT quoting forms**. Nine native predicate occurrences in eight files remain unchanged. There are no frozen legacy display.Name/display.Lore paths or other active Java/resource producers. The repository search found related matches in 777 files, including historical fixtures/copies; those copies were not edited. Flower case: actual `A blessing in love` / `Цветик-семицветик`, four lore lines. Russian rendering and default fallback match complete source Components. Existing absent Russian overrides such as Flex Tape keep source-equivalent English fallback.

BUG-02: Legacy hoverEvent/contents and clickEvent/value in BACAP tellraw functions no longer match the native codec. The exact function compilation path now translates event keys, preserving title/description/style/frame/translation/click metadata and commented/hidden source behavior. **1202** active shipped references pass real Component codec round trips. Native Advancement.name/createAnnouncement already worked and remains intact. One joined-player native test earned six advancements through actual item pickup, including vanilla and BACAP TASK/GOAL/CHALLENGE. Every target produced one earned packet with hover; no duplicate announcement was introduced.

BUG-03: The shared formatter's newline depended on optional multilineactionbar installation; vanilla overlay drawing also used one line. The shared prefix now contains a newline and a narrowly selected HUD draw wrapper centers two styled lines. **151 AbilityType values**, counts **1/2/10**, permanent locks and Russian text are covered. Unrelated one-line overlays remain unchanged. Pixel-level confirmation is pending user smoke.

BUG-04: SERVER_STARTED/join discovery ran before BACAP's pending #load objective creation/display setup. Early absence now remains transient; END_SERVER_TICK performs authoritative validation. A genuine persistent absence still logs one meaningful ERROR and leaves readiness fail-closed; repeat calls do not spam. Start/stop/reload lifecycle state resets correctly. Real-server transient/existing binding preserves ability configuration. Fresh native runtime has no false bac_advancements ERROR and becomes ready normally.

Production scopes were retried semantically after the initial broad patch's auto-review rejection. The new narrow, explicitly authorized transactions succeeded. No approval UI remains pending. The converter's accepted pre-smoke source is recovered byte-for-byte by removing exactly its new helper call and reverting r16 to r15. All accepted mixin registrations remain; only the function/HUD registrations were added. Historical evidence is untouched.

## Direct stable modernization

| Direct dependency | Before → after | Action |
|---|---|---|
| Fabric Loader | 0.19.3 → 0.19.5 | UPDATED |
| Fabric API | 0.158.0+26.2 → 0.161.0+26.2 | UPDATED |
| Fabric Loom | 1.17.19 → 1.18.2 | UPDATED |
| Gradle wrapper | 9.5.1 → 9.8.0 | UPDATED |
| JUnit Jupiter | 5.12.2 → 6.1.3 | UPDATED |
| JUnit Platform Launcher | 1.12.2 → 6.1.3 | UPDATED |
| toml4j bundled library | 0.7.2 → 0.7.2 | ALREADY_CURRENT |
| JUnit BOM | not declared → 6.1.3 | ADDED_FOR_ALIGNED_JUNIT_GROUP |

Official Fabric metadata verifies Loader 0.19.5 stable for 26.2 and the newest +26.2 API artifact 0.161.0+26.2. A newer absolute API number targeting 26.4 was excluded. Official Loom metadata now lists **1.18.2**, beyond the prompt's anticipated 1.18.1; its Java 25 / Gradle plugin API 9.7 requirements are satisfied by the tested Gradle 9.8 setup. The normal wrapper tasks and official distribution/wrapper checksums were verified. JUnit is aligned by a canonical 6.1.3 BOM. toml4j 0.7.2 is ALREADY_CURRENT. Managed transitive libraries were not independently overridden. Minecraft 26.2 / Java 25 remain explicit product pins; unused historical mappingsBuild=8 is unchanged.

Sources and classification for each direct choice are recorded in `dependency_modernization_report.json`, with saved upstream metadata and `resolved_dependencies.json`. Actual native boot reports the selected Loader/API and mod version. No snapshot, alpha, beta, RC, nightly or newer Minecraft target was selected.

BUILD-01: canonical `gradle.properties` generates filename, fabric.mod.json, project version and BuildConfig.MOD_VERSION consistently. The actual candidate is 0.1.5.2. A forced same-version Jar execution was rejected by the new guard and the candidate hash remained identical; future changed candidates require .3, .4, etc.

BUILD-02: fabric.mod.json truthfully requires Loader >=0.19.5, Fabric API >=0.161.0+26.2, Java >=25 and Minecraft ~26.2. Older Loader compatibility is not claimed as validated.

## Authoritative validation and preservation

- Final compileJava, compileTestJava and compileGametestJava: GREEN.
- Final JUnit: **82 tests, 0 failures, 0 errors, 0 skipped**, comprising the retained 72 release gates and 10 focused smoke regressions.
- Final selected GameTest: **1/1 required tests**, six native-earned advancement receipts, real flower and parchment ItemStack persistence, existing command/warning hover and scoreboard binding; certification gain **0**. One earlier pre-modernization narrow smoke execution also passed. Historical native campaigns were not rerun.
- Standard `build -x test -x runGameTest`: GREEN after the selected tests; the exclusions prevent Fabric's build/check dependencies from launching unrelated historical campaigns.
- Actual JAR: **1906 entries**, **227 production classes**, **1540 resources**, **158 mixin references**. All packaged classes equal final Gradle main output; all production resources match. Only toml4j 0.7.2 is nested. No test/GameTest classes, planning artifacts, diagnostics/logs, duplicate metadata, absolute local paths or high-confidence credential signatures were found.
- **1152/1152 product-certified**, 0 uncertified; FINAL19 **19 targets, 79 groups, 108 criteria, seven GREEN families**. Historical Phase A remains **1133/1152**.
- Axe 14 native controls, cauldron 24 controls, raider zero-gain integrity, feeling_ill true branches, dungeon_crawler false branches, voluntary_exile and deflect_arrow remain accepted and independently revalidated through retained acceptance/static gates.
- **2533 original protected files are byte-identical**; six authorized original source/metadata files changed and eight authorized new source/test files were added. No protected file was removed.
- Product ledger SHA-256: `49f001ffe29719d03e1dfc5f75048e1424f7f681458c2d709828872d4eb11349`.
- FINAL19 terminal SHA-256: `1ab689c783c6a7e85d2e84dd9c526b2d4333934171b81b4a22d5ae1e4454e962`.
- Frozen BACAP SHA-256: `8c72314535c5df7b4416bf0f38310371ec8aec537fde1445bdc820a3c9aada70`; SHA-1: `45b8bb0076bbf5b92fde7dc9590c6686937abbc0`.
- Runtime conversion difference: exactly **215 text-bearing functions** and the compatibility marker; all other ZIP entries, including **1229 advancement definitions**, are byte-identical.
- Previous 0.1.5 JAR retains SHA-256 `aba207b7d9873cb1a3073894a0948d7953316578ab0fab2c6d88b86dab070f69`; previous release namespace was not overwritten.

Retained fingerprints include converter/config/metadata bytes. New namespace adapters accept only explicitly bounded additions and reconstruct verified original hashes. The historical receipt validators, catalogs, evidence and rejection controls are preserved. Fixture-only incidents are retained in logs: native bootstrap/pack-format setup, PowerShell -D quoting, language placeholder normalization and nested siblings, exact Windows edited-line endings. No failed fixture attempt is counted GREEN. The Windows OSHI Perflib diagnostic and short bootstrap lag warning remain observable in native startup; no claim is made that all host warnings are eliminated. No AchieveToDo startup missing-objective ERROR or Mixin application failure appeared in the successful smoke run.

## Exact production/build files in this pass

- `build.gradle` — SHA-256 `3d4e9cd400ac3ce0be3ce80f82eabec25a730e59019f7ef1e142eb9b50ff1483`
- `gradle.properties` — SHA-256 `3997745a77b274ecffd5f23a47672639aaa73bb79dceafe081e417e477e4b63b`
- `gradle/BuildConfig.java.txt` — SHA-256 `813fe78f49101be2fa128169becaa8edef62a2b355a22411bca250c091f3947b`
- `gradle/wrapper/gradle-wrapper.jar` — SHA-256 `238e777fcddd7e34f9708186085def2abd6e08e658505b38718d79d74c21abd5`
- `gradle/wrapper/gradle-wrapper.properties` — SHA-256 `e81b90975868be2513ca00d6a5a01303a406a5cb01a306210b887e983e4b17ae`
- `gradlew` — SHA-256 `a5a5c199ba02189ae8c46a334223371a20599d9c298ef65e7540ede4a3f72d59`
- `gradlew.bat` — SHA-256 `f17917f8dbe61182b149273ee78090fec1197442c3f38276af907bfa5190db16`
- `src/main/generated/java/com/diskree/achievetodo/BuildConfig.java` — SHA-256 `5b7139973592aac3dbd3cfe594130a763eb15c22d1438c4db1a6e5d1e6a044fb`
- `src/main/java/com/diskree/achievetodo/ability/AbilityType.java` — SHA-256 `b6b4961e52ad72d5644b14783491d4820d9debdf9cd9547392106302580c789e`
- `src/main/java/com/diskree/achievetodo/client/ExternalPackCompatibility.java` — SHA-256 `20c73be41c3cb218d90339aedee740e5269dc8e6acd9012f952249cefe5fa767`
- `src/main/java/com/diskree/achievetodo/client/LegacyChatText.java` — SHA-256 `20ff001ca7b6bcba188144a054f4622c2d7bb3ab78089dac1e92b6c6d4b98b7d`
- `src/main/java/com/diskree/achievetodo/client/LegacyItemText.java` — SHA-256 `04544cde722713187b6d946fa298259b62101cd4745f4858982e4f58f9ed57a3`
- `src/main/java/com/diskree/achievetodo/client/gui/AbilityLockLines.java` — SHA-256 `ded464345a8a2817949bcb550062d362d8528eae85b146ebf08f9513938d326b`
- `src/main/java/com/diskree/achievetodo/injection/mixin/client/HudAbilityLockMixin.java` — SHA-256 `6fcab84725619b53bc2f25a914fa619b6e0e4caef2106f8a8083e7e07ee4061b`
- `src/main/java/com/diskree/achievetodo/injection/mixin/main/CommandFunctionMixin.java` — SHA-256 `576aa49afbe0c15eb161ed1e190e60a4d3364dce76dfd4cd5dac02b83a1d848c`
- `src/main/java/com/diskree/achievetodo/server/AchieveToDoServer.java` — SHA-256 `2d48d8062c47507777e1179a75ccc15c4cc614845f7dee3b9ec978114f395e5c`
- `src/main/resources/achievetodo.mixins.json` — SHA-256 `5c63a34c43f85e7106a12540d5cb3fbcd61426e1101bf5b7f3d09aa0c75ed13d`
- `src/main/resources/fabric.mod.json` — SHA-256 `aafd92290ee6efdcdcadfaf7db0c6ef0bd98fa0c0816d0b207136eedf68b114d`

## User confirmation

Follow `user_smoke_regression_matrix.md`: flower plus other trophies/pergament with Russian/default tooltip and save/reload; native earned vanilla/BACAP hover and working command hover; crouch plus two other two-line locks; fresh-world log plus existing-world reload/binding; quick Axe/Cauldron/Shield/save regression.

Prior Axe, Cauldron, Shield and save/reload/rejoin manual results remain USER_SMOKE_GREEN for the earlier candidate. **0.1.5.2 is not finally accepted**. Raider manual smoke remains optional/deferred, not falsely GREEN. No repository cleanup, Git mutation, publishing, or Phase B was performed.
