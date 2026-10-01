# PRE-26.2 smoke bugfix candidate 0.1.5.4

Status: PRE_26_2_SMOKE_BUGFIX_BUILD_0_1_5_4_READY. Build readiness; graphical user acceptance remains pending.
Scope: BUG-05 + BUG-06 only. Minecraft 26.2 / Java 25; existing dependency modernization retained.

## BUG-05: tutorial lifecycle and durability

The 26.2 L-key path is Gui.handleKeybinds -> Gui.setScreen, bypassing the old Minecraft.setScreenAndShow injection. ATD held vanilla MOVEMENT on a process-local acknowledgement; the open event did not durably advance options and the custom toast could remain live.

Actual 26.2 bytecode explains the same-process/restart difference: Native Minecraft.tick calls Tutorial.tick only with a loaded, unpaused level. Disconnect retains the Movement instance while Hud.onDisconnected clears Gui.toastManager; its non-null custom toast reference suppresses another toast. A fresh process reconstructs MOVEMENT from options.txt and has no such reference.
The user's runtime options file currently records `tutorialStep:movement`; its log loads ATD 0.1.5.3.

Gui.setScreen HEAD retains readiness protection; TAIL acknowledges only the screen actually installed. Once movement/look are complete, the event immediately calls native Tutorial.setStep(FIND_TREE), which saves options, clears the previous instance/toasts, and creates the next vanilla step. Ordinary completion uses vanilla options.txt tutorialStep. Native Tutorial.start reconstructs from options.tutorialStep. Tutorial.stop/clear do not reset this durable step; shutdown/restart therefore preserves completion.

A screen may be opened before movement/look complete. The six vanilla step enum values have no state for MOVEMENT plus independently completed ATD acknowledgement. Advancing then would skip unrelated prerequisites. Only this early case writes config/achievetodo/early-advancements-tutorial-completion with an open-advancements-v1 receipt; reconstruction retains acknowledgement while MOVEMENT prerequisites remain required.
This receipt is an independent early-completion proof, not an arbitrary general ATD config switch. Normal prompted completion creates no dedicated file.
No global tutorial disable or unrelated prerequisite/step skip. Creative/non-survival NONE remains the normal vanilla transition.

One hide callback per toast; opened and clear hide/release it idempotently. Native setStep also clears movement/look toasts. Gui.update continues native toast updates outside paused level ticks; no render polling or toast manager-wide clearing was added. Ordinary event saves vanilla options once; repeated opens/ticks do not repeat completion. Early receipt is loaded once per handler and written once for the acknowledgement, never per tick.
Existing tutorial keys and advancement keybind component preserved. Only navigation-unavailable EN/RU strings were added.

Six state-transition tests including real mixin entry call, one hide, duplicate suppression, early completion file reconstruction, modeled vanilla options reconstruction and no repeated advancement over 100 ticks. PersistentTutorialFixture models the bytecode-audited native setStep/save contract. It does not boot a graphical client; full process restart and pixel rendering are manual acceptance.

## BUG-06: actual optional-mod contract

Old tellraw links still run advancementssearch highlight <id> obtained_status. Installed Search 1.3 is client-only, ID advancements_search, onInitializeClient is empty, and its complete class inventory has no command registration. ATD inferred absence from a SERVER command parse failure and emitted install guidance without inspecting the client mod. Each failed invocation could repeat it.
Installed exact artifact: `D:\кубы сборки\atd pre-26.2 v2\minecraft\mods\advancements_search-mc26.2+1.3.jar`; SHA-256 `7bdc638ad39e482068f6931f91268fd4fd3fef9d03786cf28b2605b8f84894db`.
The `fabric.mod.json`, all relevant Search classes and bytecode are retained alongside this report.
No ATD FabricLoader mod-ID check existed in the old failure path. Neither command availability nor server dispatcher rejection can identify a client-only mod.

ATD consumes the legacy link command on the client using Fabric command API v2 and native IdentifierArgument. It resolves the received client advancement node, opens the standard screen, selects its real root and asks the isolated adapter to start Search native centering/flashing. Old server parser/install mixin and its registration were removed.
Arguments are parsed by native `IdentifierArgument`; vanilla, BACAP and ATD namespace/path values remain exact.
The accepted `obtained_status` legacy suffix remains optional. No permissions/server operator requirement or server command execution is needed for client navigation.

FabricLoader ID advancements_search; no command registration in installed Search 1.3; AdvancementsScreen has public advancements_search$stopFlashing() and private Identifier flashingAdvancementId. Native startFlashing centers and flashes the matching widget.
The bridge invokes the native reset method, sets the audited target identifier and leaves centering, timing and flash rendering to Search. No Search implementation was copied or bundled.
Client consumes one recognized command and emits one localized install message; no server retry/fallback. One localized unavailable message, never install guidance for a detected mod; reflection/linkage failures return unavailable.
No optional class names/types/imports in product bytecode; absent-mod JUnit classpath and changed-API regressions passed.

The adapter is intentionally tied to verified Search 1.3 screen fields rather than claiming a stable public highlight API. Future incompatible fields fail safely with unavailable guidance. GitHub source location was identified from installed metadata; web fetch unavailable and raw HTTPS failed. Installed exact artifact bytecode supplied the integration evidence.
Eleven tests: actual Brigadier tree/identifiers, present/absent/unavailable branches, exactly one fallback per dispatched action, absent optional implementation, exact identifier delivery, native-state fixture, changed field type and linkage failure.

## Fullscreen invariant

Fullscreen 2.0.1 installed artifact: `D:\кубы сборки\atd pre-26.2 v2\minecraft\mods\advancements_fullscreen-2.0.1+fabric-mc26.2.jar`; SHA-256 `26a0d87a1d4621bdcce725e5b30ba3c5315ae33de22efb626ebf8665536b5021`.
Its metadata and screen-patch bytecode were inspected. It patches the existing vanilla screen's dimensions/rendering; it does not replace the screen class or Search flashing target fields.
Reuse the ordinary AdvancementsScreen and vanilla root selection; native Search geometry/rendering remains owned by Search plus Fullscreen.
**Advancements Fullscreen was not product-patched.** Neither optional mod was made a required dependency, changed on disk, bundled or installed by this pass.
Static/artifact compatibility is established; the new combination was not run in a graphical client and is not falsely claimed runtime GREEN.

## Verification and preservation

Baseline ordinary `compileJava compileTestJava compileGametestJava test`: compile gates completed; 376 tests, four existing historical marker/conversion assertions failed. Baseline XML and log are retained. Those old fixtures were not edited and are not confused with the accepted release suite.
Initial focused regression found Brigadier word parsing rejected colon/slash; native IdentifierArgument fixed it. Final focused: 17 tests, zero failures/errors/skips.
Full accepted suite: **103 tests**, zero failures/errors/skips: all 22 accepted 0.1.5.3 suites (86 tests, with only version expectations adapted locally) plus 17 focused regressions.
All three required compile tasks completed successfully. compileGametestJava remained up-to-date because native GameTest sources were unchanged.
Normal distributable `build -x test -x runGameTest`: BUILD SUCCESSFUL, exit 0. Accepted prior release exclusions prevent unrelated closed native campaigns; authoritative JUnit ran separately.
No fake server GameTest was added for client UI semantics.

Automatic approval review initially rejected a new init route as test-harness scope expansion. The user explicitly approved this test-only route on 2026-10-01; the subsequent accepted gates succeeded. A proposed empty production mixin was separately rejected and never applied. No approval remains pending.
Historical scripts/reports/evidence remain byte-identical. The local acceptance adapter restores exactly the removed server mixin registration for historical fingerprint comparison and still compares the complete reconstructed JSON with the immutable prior JAR; it does not weaken unrelated validators.

**1152/1152 product-certified**, zero uncertified. FINAL19 remains CLOSED: 19 targets / 79 groups / 108 criteria. Historical Phase A remains 1133/1152.
2724 pre-existing protected files are byte-identical. Existing BUG-01..04, Axe/Cauldron gates, raider conversion/zero-gain integrity and skeleton-owned Deflect Arrow semantics passed retained acceptance gates and remain unchanged.
Frozen BACAP, converter and all advancement definitions/criteria/resources are preserved. Current author Glower4ek, source Glower4ek/AchieveToDo, copyright 2026 Glower4ek and technical com.diskree namespace remain intact.
No cleanup, Phase B, refactor campaign, dependency update, Git mutation, publishing or release finalization.

## Actual distributable audit

`D:\Vibecode\AchieveToDo 26.2\build\libs\achievetodo-mc26.2+0.1.5.4.jar` — **1546595 bytes**; SHA-256 **0fc29fd0fbe1d874ca037ba2bd932f4e667593d95a1f2d3bb42936765710978b**; internal version exactly **0.1.5.4**.
1911 entries, 232 production classes, 157 resolving mixin references. Main class output is byte-equivalent. Resources match processed output; only Loom's existing toml4j nested-JAR metadata is injected into fabric.mod.json.
Metadata differs from 0.1.5.3 only by version; dependencies, author/source/license and bundled toml4j are unchanged.
Six client adapter/helper classes were added; the obsolete server install-fallback class was removed. Exact changed/added/removed entry lists are in jar_audit.json.
No test/GameTest/probe/planning/reference/scratch classes or files; no optional implementation classes or hard class references; no product resource loss; all mixin and entrypoint references resolve.
Prior candidate 0.1.5.3 JAR retains its accepted hash.

## Changed source and metadata files

- `src/main/generated/java/com/diskree/achievetodo/BuildConfig.java`
- `src/main/java/com/diskree/achievetodo/client/AchieveToDoClient.java`
- `src/main/java/com/diskree/achievetodo/injection/mixin/client/GuiMixin.java`
- `src/main/java/com/diskree/achievetodo/injection/mixin/client/MinecraftClientMixin.java`
- `src/main/java/com/diskree/achievetodo/injection/mixin/client/MovementTutorialStepHandlerMixin.java`
- `src/main/java/com/diskree/achievetodo/injection/mixin/main/CommandManagerMixin.java` (removed)
- `src/main/resources/achievetodo.mixins.json`
- `src/main/resources/assets/achievetodo/lang/en_us.json`
- `src/main/resources/assets/achievetodo/lang/ru_ru.json`
- `src/test/java/com/diskree/achievetodo/injection/mixin/client/MovementTutorialStepHandlerMixinTest.java`
- `gradle.properties`
- `src/main/java/com/diskree/achievetodo/client/AdvancementsTutorialProgress.java` (new)
- `src/main/java/com/diskree/achievetodo/client/AdvancementLinkCommand.java` (new)
- `src/main/java/com/diskree/achievetodo/client/OptionalAdvancementSearch.java` (new)
- `src/test/java/com/diskree/achievetodo/client/AdvancementLinkCommandTest.java` (new)

Generated BuildConfig is included above for provenance. All pre/post hashes, test suites, commands, artifact audit and preservation results are in bugfix_manifest.json; helper/test-only files in this new durable directory are not packaged.

## Remaining manual acceptance

1. With Search absent and the standard advancements screen: unfinished tutorial may appear; L must open the screen and close the toast. Close/reopen, leave/rejoin, then fully exit/relaunch Minecraft and enter the same world. The completed tutorial must not return.
2. Exercise early L opening before completing movement/look, then restart. ATD acknowledgement must persist while the vanilla movement/look tutorial still progresses normally.
3. Click vanilla and BACAP earned/ability advancement links with Search absent: exactly one install hint per action, no duplicate fallback or crash.
4. With Search 1.3 alone, click vanilla/BACAP/ATD links: intended tab/widget opens, centers and flashes; no install hint. Switch tabs, close and reopen the screen.
5. Repeat L, tab visibility/switching, links/highlighting, close/reopen and restart with Fullscreen 2.0.1 alone and Search + Fullscreen together. BACAP and ATD tabs and fullscreen layout must remain correct; no frozen/duplicate tutorial toast.
6. Retain the earlier BUG-01..04 and Axe/Cauldron/Shield smoke checks when accepting this candidate. No certification reopening is implied.
