"""Write the new smoke record from audited facts, never into accepted history."""
import datetime, hashlib, json, pathlib, re, subprocess

ROOT = pathlib.Path(__file__).resolve().parents[3]
OUT = pathlib.Path(__file__).resolve().parent

def read(path):
    return json.loads(path.read_text(encoding='utf-8-sig'))

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def write(name, value):
    (OUT/name).write_text(json.dumps(value, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')

def main():
    audit = read(OUT/'jar_content_audit.json')
    preservation = read(OUT/'final_preservation_audit.json')
    dependencies = read(OUT/'dependency_modernization_report.json')
    assert audit['verdict'] == 'JAR_AUDIT_GREEN' and preservation['verdict'] == '1152_PRESERVATION_GREEN'
    artifact = audit['artifact']
    assert sha(pathlib.Path(artifact['absolutePath'])) == artifact['sha256']
    bugs = [
        dict(id='BUG-01', authorization='Explicit user smoke scope and attachment section 19',
            rootCause='Frozen BACAP give commands store serialized Component JSON in SNBT StringTag custom_name/item_name/lore. Minecraft 26.2 interprets these tags as literal text, exposing JSON instead of the old decoded Component semantics.',
            systemicCorrection='Decode only legacy text at item component assignments in the existing world-copy function converter. Preserve translation keys, nested components, styles, separate lore lines and already-modern values. Advance compatibility marker r15 to r16 so existing world copies refresh.',
            productionFiles=['src/main/java/com/diskree/achievetodo/client/LegacyItemText.java','src/main/java/com/diskree/achievetodo/client/ExternalPackCompatibility.java'],
            inventory=dict(giveFunctions=215,customNameFunctions=214,itemNameFunctions=1,serializationForms=2,alreadyNativePredicateOccurrences=9,alreadyNativePredicateFiles=8,repositoryFilesWithRelatedMatches=777,legacyDisplayNameLorePaths=0),
            regressions=['flowerRewardUsesTranslatedStyledComponentsAndSeparateLoreLines','all215CustomRewardsMatchSourceComponentsAndSurviveNbtReload','ordinaryVanillaAndAlreadyModernNamesRemainUnchanged','doubleEncodedNamesAndItemNamesBecomeComponents','legacyLiteralAndArrayComponentsPreserveStylesWithoutRewritingQuotedData','rewardAndAbilityComponentsRenderRussianWithSourceEquivalentFallback'],
            runtime='Actual flower trophies earned by pickup, plus real ninth_line give function for namespaced ITEM_NAME/double-quoted lore; representative complete ItemStack NBT round trips.',result='GREEN_COMPONENT_AND_NATIVE_SEMANTICS'),
        dict(id='BUG-02', authorization='Explicit advancement-hover production scope',
            rootCause='Bundled and frozen BACAP earned tellraw function JSON uses hoverEvent/contents and clickEvent/value. The current Component codec expects hover_event/value and action-specific click_event keys; legacy metadata is silently dropped. Native Advancement.name/createAnnouncement references already retain hover.',
            systemicCorrection='Migrate event metadata before CommandFunction.fromLines compiles the actual intended advancement-reference functions. Keep original text/component tree, frame/color, translations, clicks and source comments/hidden behavior. Send no second message.',
            productionFiles=['src/main/java/com/diskree/achievetodo/client/LegacyChatText.java','src/main/java/com/diskree/achievetodo/injection/mixin/main/CommandFunctionMixin.java','src/main/resources/achievetodo.mixins.json'],
            regressions=['earnedWashingMachineChatContainsHoverAndClickMetadata','everyShippedAdvancementMessageRetainsHoverClickAndFrameMetadata'],
            inventory=dict(activeShippedTellrawReferences=1202),runtime='Six joined SURVIVAL pickup advancements, including an exact vanilla-definition fixture, actual Minecraft mine_stone, BACAP TASK/GOAL/CHALLENGE, and the flower reward. Outgoing earned chat packets have one matching message and hover. Existing Advancement.name command/warning reference retains hover.',result='GREEN_COMPONENT_AND_NATIVE_SEMANTICS'),
        dict(id='BUG-03', authorization='Explicit shared ability-lock formatter and UX scope',
            rootCause='Both the original formatter and the port made newline conditional on an optional multilineactionbar mod. Without that external renderer the prefix used a space and vanilla Hud overlay rendering had a single draw call.',
            systemicCorrection='Restore the shared translated lock prefix newline unconditionally and draw its styled lines through the existing overlay extraction call. Restrict rendering to achievetodo.ability.*.locked_message. Retain vanilla timing, fade, background and behavior of unrelated overlay messages.',
            productionFiles=['src/main/java/com/diskree/achievetodo/ability/AbilityType.java','src/main/java/com/diskree/achievetodo/client/gui/AbilityLockLines.java','src/main/java/com/diskree/achievetodo/injection/mixin/client/HudAbilityLockMixin.java','src/main/resources/achievetodo.mixins.json'],
            regressions=['sharedAbilityFormatterRestoresTwoLines','rewardAndAbilityComponentsRenderRussianWithSourceEquivalentFallback'],inventory=dict(abilityTypes=151,remainingCounts=[1,2,10],permanentLocks=True),
            runtime='Underlying localized styled Component line structure tested. Actual Minecraft 26.2 HUD invocation descriptor inspected and compilation passes; final client pixels remain user smoke.',result='GREEN_COMPONENT_SEMANTICS_VISUAL_USER_CONFIRMATION_PENDING'),
        dict(id='BUG-04', authorization='Explicit exact scoreboard lifecycle scope',
            rootCause='AchieveToDo validates at SERVER_STARTED and player join before the pending BACAP #load function start_timers creates bac_advancements and new_world sets its display slot. This transient state was repeatedly logged as ERROR.',
            systemicCorrection='Keep early discovery and fail-closed readiness. Authoritative missing-objective validation runs at END_SERVER_TICK after #load. Log once per persistent missing state, reset on discovery/start/stop, and defer successful datapack reload validation to the next end tick.',
            productionFiles=['src/main/java/com/diskree/achievetodo/server/AchieveToDoServer.java'],
            regressions=['transientObjectiveAbsenceDoesNotEmitError'],runtime='Real-server scratch scoreboard covers transient->present and existing binding with the live ability configuration. Fresh native startup has no target false ERROR and normal progression readiness. Persistent missing boundary is tested with one ERROR and duplicate suppression.',result='GREEN_COMPONENT_AND_NATIVE_SEMANTICS')
    ]
    source_paths = sorted({path for bug in bugs for path in bug['productionFiles']})
    build_paths = ['gradle.properties','build.gradle','gradle/BuildConfig.java.txt','gradle/wrapper/gradle-wrapper.properties','gradle/wrapper/gradle-wrapper.jar','gradlew','gradlew.bat','src/main/resources/fabric.mod.json','src/main/generated/java/com/diskree/achievetodo/BuildConfig.java']
    table = '\n'.join('| '+row['dependency']+' | '+row['before']+' → '+row['after']+' | '+row['action']+' |' for row in dependencies['directDependencies'])
    source_lines = '\n'.join('- `'+path+'` — SHA-256 `'+sha(ROOT/path)+'`' for path in sorted(set(source_paths+build_paths)))
    report = f'''# PRE-26.2 smoke bugfix candidate 0.1.5.2

Status: **PRE_26_2_SMOKE_BUGFIX_BUILD_0_1_5_2_READY**. User acceptance remains pending.

Normal Gradle distributable: `{artifact['absolutePath']}`. Size: **{artifact['bytes']} bytes**. Internal mod version: **0.1.5.2**; project version: **mc26.2+0.1.5.2**.

- SHA-256: `{artifact['sha256']}`
- SHA-1: `{artifact['sha1']}`
- Build/runtime: Minecraft 26.2, Java 25.0.4+7-LTS (target 25), Loader 0.19.5, Fabric API 0.161.0+26.2, Loom 1.18.2, Gradle 9.8.0.
- Git HEAD: `{preservation['head']}`; branch: `{preservation['branch']}`. Git index unchanged. Unrelated dirty work preserved.

## Four authorized bug transactions

BUG-01: Component JSON inside legacy SNBT string tags became literal item text in 26.2. The shared converter decodes custom_name, namespaced item_name and separate lore lines before the native item parser consumes them. It retains translated keys, nested siblings, color/bold/italic and modern/ordinary names. All **215** function producers were audited: **214 custom_name trophies**, **one item_name parchment**, and **two SNBT quoting forms**. Nine native predicate occurrences in eight files remain unchanged. There are no frozen legacy display.Name/display.Lore paths or other active Java/resource producers. The repository search found related matches in 777 files, including historical fixtures/copies; those copies were not edited. Flower case: actual `A blessing in love` / `Цветик-семицветик`, four lore lines. Russian rendering and default fallback match complete source Components. Existing absent Russian overrides such as Flex Tape keep source-equivalent English fallback.

BUG-02: Legacy hoverEvent/contents and clickEvent/value in BACAP tellraw functions no longer match the native codec. The exact function compilation path now translates event keys, preserving title/description/style/frame/translation/click metadata and commented/hidden source behavior. **1202** active shipped references pass real Component codec round trips. Native Advancement.name/createAnnouncement already worked and remains intact. One joined-player native test earned six advancements through actual item pickup, including vanilla and BACAP TASK/GOAL/CHALLENGE. Every target produced one earned packet with hover; no duplicate announcement was introduced.

BUG-03: The shared formatter's newline depended on optional multilineactionbar installation; vanilla overlay drawing also used one line. The shared prefix now contains a newline and a narrowly selected HUD draw wrapper centers two styled lines. **151 AbilityType values**, counts **1/2/10**, permanent locks and Russian text are covered. Unrelated one-line overlays remain unchanged. Pixel-level confirmation is pending user smoke.

BUG-04: SERVER_STARTED/join discovery ran before BACAP's pending #load objective creation/display setup. Early absence now remains transient; END_SERVER_TICK performs authoritative validation. A genuine persistent absence still logs one meaningful ERROR and leaves readiness fail-closed; repeat calls do not spam. Start/stop/reload lifecycle state resets correctly. Real-server transient/existing binding preserves ability configuration. Fresh native runtime has no false bac_advancements ERROR and becomes ready normally.

Production scopes were retried semantically after the initial broad patch's auto-review rejection. The new narrow, explicitly authorized transactions succeeded. No approval UI remains pending. The converter's accepted pre-smoke source is recovered byte-for-byte by removing exactly its new helper call and reverting r16 to r15. All accepted mixin registrations remain; only the function/HUD registrations were added. Historical evidence is untouched.

## Direct stable modernization

| Direct dependency | Before → after | Action |
|---|---|---|
{table}

Official Fabric metadata verifies Loader 0.19.5 stable for 26.2 and the newest +26.2 API artifact 0.161.0+26.2. A newer absolute API number targeting 26.4 was excluded. Official Loom metadata now lists **1.18.2**, beyond the prompt's anticipated 1.18.1; its Java 25 / Gradle plugin API 9.7 requirements are satisfied by the tested Gradle 9.8 setup. The normal wrapper tasks and official distribution/wrapper checksums were verified. JUnit is aligned by a canonical 6.1.3 BOM. toml4j 0.7.2 is ALREADY_CURRENT. Managed transitive libraries were not independently overridden. Minecraft 26.2 / Java 25 remain explicit product pins; unused historical mappingsBuild=8 is unchanged.

Sources and classification for each direct choice are recorded in `dependency_modernization_report.json`, with saved upstream metadata and `resolved_dependencies.json`. Actual native boot reports the selected Loader/API and mod version. No snapshot, alpha, beta, RC, nightly or newer Minecraft target was selected.

BUILD-01: canonical `gradle.properties` generates filename, fabric.mod.json, project version and BuildConfig.MOD_VERSION consistently. The actual candidate is 0.1.5.2. A forced same-version Jar execution was rejected by the new guard and the candidate hash remained identical; future changed candidates require .3, .4, etc.

BUILD-02: fabric.mod.json truthfully requires Loader >=0.19.5, Fabric API >=0.161.0+26.2, Java >=25 and Minecraft ~26.2. Older Loader compatibility is not claimed as validated.

## Authoritative validation and preservation

- Final compileJava, compileTestJava and compileGametestJava: GREEN.
- Final JUnit: **82 tests, 0 failures, 0 errors, 0 skipped**, comprising the retained 72 release gates and 10 focused smoke regressions.
- Final selected GameTest: **1/1 required tests**, six native-earned advancement receipts, real flower and parchment ItemStack persistence, existing command/warning hover and scoreboard binding; certification gain **0**. One earlier pre-modernization narrow smoke execution also passed. Historical native campaigns were not rerun.
- Standard `build -x test -x runGameTest`: GREEN after the selected tests; the exclusions prevent Fabric's build/check dependencies from launching unrelated historical campaigns.
- Actual JAR: **{audit['entries']} entries**, **{audit['productionClasses']} production classes**, **{audit['productionResources']} resources**, **{audit['mixinReferences']} mixin references**. All packaged classes equal final Gradle main output; all production resources match. Only toml4j 0.7.2 is nested. No test/GameTest classes, planning artifacts, diagnostics/logs, duplicate metadata, absolute local paths or high-confidence credential signatures were found.
- **1152/1152 product-certified**, 0 uncertified; FINAL19 **19 targets, 79 groups, 108 criteria, seven GREEN families**. Historical Phase A remains **1133/1152**.
- Axe 14 native controls, cauldron 24 controls, raider zero-gain integrity, feeling_ill true branches, dungeon_crawler false branches, voluntary_exile and deflect_arrow remain accepted and independently revalidated through retained acceptance/static gates.
- **{preservation['unchangedProtectedFiles']} original protected files are byte-identical**; six authorized original source/metadata files changed and eight authorized new source/test files were added. No protected file was removed.
- Product ledger SHA-256: `49f001ffe29719d03e1dfc5f75048e1424f7f681458c2d709828872d4eb11349`.
- FINAL19 terminal SHA-256: `1ab689c783c6a7e85d2e84dd9c526b2d4333934171b81b4a22d5ae1e4454e962`.
- Frozen BACAP SHA-256: `8c72314535c5df7b4416bf0f38310371ec8aec537fde1445bdc820a3c9aada70`; SHA-1: `45b8bb0076bbf5b92fde7dc9590c6686937abbc0`.
- Runtime conversion difference: exactly **215 text-bearing functions** and the compatibility marker; all other ZIP entries, including **1229 advancement definitions**, are byte-identical.
- Previous 0.1.5 JAR retains SHA-256 `aba207b7d9873cb1a3073894a0948d7953316578ab0fab2c6d88b86dab070f69`; previous release namespace was not overwritten.

Retained fingerprints include converter/config/metadata bytes. New namespace adapters accept only explicitly bounded additions and reconstruct verified original hashes. The historical receipt validators, catalogs, evidence and rejection controls are preserved. Fixture-only incidents are retained in logs: native bootstrap/pack-format setup, PowerShell -D quoting, language placeholder normalization and nested siblings, exact Windows edited-line endings. No failed fixture attempt is counted GREEN. The Windows OSHI Perflib diagnostic and short bootstrap lag warning remain observable in native startup; no claim is made that all host warnings are eliminated. No AchieveToDo startup missing-objective ERROR or Mixin application failure appeared in the successful smoke run.

## Exact production/build files in this pass

{source_lines}

## User confirmation

Follow `user_smoke_regression_matrix.md`: flower plus other trophies/pergament with Russian/default tooltip and save/reload; native earned vanilla/BACAP hover and working command hover; crouch plus two other two-line locks; fresh-world log plus existing-world reload/binding; quick Axe/Cauldron/Shield/save regression.

Prior Axe, Cauldron, Shield and save/reload/rejoin manual results remain USER_SMOKE_GREEN for the earlier candidate. **0.1.5.2 is not finally accepted**. Raider manual smoke remains optional/deferred, not falsely GREEN. No repository cleanup, Git mutation, publishing, or Phase B was performed.
'''
    (OUT/'pre26_2_smoke_bugfix_report.md').write_text(report, encoding='utf-8')
    manifest = dict(schemaVersion=1, task='PRE_26_2_USER_SMOKE_BUGFIX_AND_MODERNIZATION_PASS',
        status='PRE_26_2_SMOKE_BUGFIX_BUILD_0_1_5_2_READY', recordedUtc=datetime.datetime.now(datetime.timezone.utc).isoformat(), userAcceptance='PENDING_USER_SMOKE',
        artifact=artifact, bugs=bugs, build=dict(canonicalVersionSource='gradle.properties:modVersion', modVersion='0.1.5.2', projectVersion='mc26.2+0.1.5.2',
            command='.\\gradlew.bat --no-daemon -I reference/phase_a_planning/pre26_2_smoke_bugfix_0_1_5_2/smoke.init.gradle build -x test -x runGameTest pre26BuildMetadata pre26ResolvedDependencies',
            producingTask=':jar', pipeline='Normal unobfuscated Minecraft 26.2 Loom distributable; not manually assembled or renamed.', buildExit=0, gradle='9.8.0',loom='1.18.2',fabricLoader='0.19.5',fabricApi='0.161.0+26.2',minecraft='26.2',java='25.0.4+7-LTS',javaTarget=25,
            environment=dict(JAVA_HOME='C:\\Program Files\\Eclipse Adoptium\\jdk-25.0.4.7-hotspot', GRADLE_USER_HOME=str(ROOT/'.gradle-user-home'), TEMP=str(ROOT/'build/tmp/codex_java_uds_probe'), TMP=str(ROOT/'build/tmp/codex_java_uds_probe'),routing='Direct authoritative require_escalated; no sacrificial restricted Gradle attempt.'),
            versionReuseGuard=read(OUT/'version_guard_result.json')),
        dependencies=dependencies, gates=dict(compileJava='GREEN',compileTestJava='GREEN',compileGametestJava='GREEN',junit=preservation['junit'],suites=preservation['junitSuites'],nativeRequiredPassed=1,nativeRequiredTotal=1,nativeAdvancementReceipts=6,certificationGain=0),
        certification=preservation, jarAudit={k:v for k,v in audit.items() if k!='entrySha256'},
        protectedIdentityHashes=read(OUT/'input_hashes.json'), sourceAndBuildHashes={path:sha(ROOT/path) for path in sorted(set(source_paths+build_paths))},
        manualResults=dict(previousCandidate=dict(axe='USER_SMOKE_GREEN',cauldron='USER_SMOKE_GREEN',shield='USER_SMOKE_GREEN',saveReloadRejoin='USER_SMOKE_GREEN'),newCandidate='PENDING_USER_SMOKE',raider='NOT_PERFORMED_OPTIONAL_DEFERRED'),
        scopeSafety=dict(gitMutations=False,cleanup=False,phaseB=False,previousReleaseRecordsOverwritten=False,frozenBacapModified=False,historicalEvidenceRewritten=False,realHumanApprovalPending=False),
        visualLimitation='Component structure/serialization tested; final client tooltip/overlay pixels require user smoke.',
        evidenceHashes={path.relative_to(OUT).as_posix():sha(path) for path in sorted(OUT.rglob('*')) if path.is_file() and path.name not in ['pre26_2_smoke_bugfix_manifest.json','durable_hashes.json'] and '__pycache__' not in path.parts})
    write('pre26_2_smoke_bugfix_manifest.json', manifest)
    write('durable_hashes.json',dict(status=manifest['status'], hashes={name:sha(OUT/name) for name in ['pre26_2_smoke_bugfix_manifest.json','pre26_2_smoke_bugfix_report.md','dependency_modernization_report.json','user_smoke_regression_matrix.md','jar_content_audit.json','final_preservation_audit.json']}, artifactSha256=artifact['sha256']))
    print(json.dumps(read(OUT/'durable_hashes.json'), indent=2))

if __name__ == '__main__':
    main()
