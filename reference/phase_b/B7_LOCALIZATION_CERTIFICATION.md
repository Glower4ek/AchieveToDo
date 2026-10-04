# B7 — Independent Localization Certification

**PHASE_B_B7_STOP — readyForB8 = false.**

The mandatory full Gradle task ran 391 tests: 380 passed, 11 failed, zero errors/skips. Seven failing names are additional to the four accepted historical failures. Only one of those four still matches both its baseline message and stack exactly. Sections 26 and 30 of the B7 contract therefore prohibit certification. No historical assertions or runtime files were repaired.

## Independently completed checks

- 4304 effective requirements: 3435 overlay, 533 native ATD RU, 325 actual cached Minecraft 26.2 RU, 11 explicitly language-neutral.
- Main 3702/3702; Hardcore 3702/3702; Terralith 3756/3756; Amplified Nether 3702/3702; Nullscape 3705/3705. All have zero missing Russian requirements.
- Native ATD EN/RU both have 533 keys, equal key sets and zero placeholder mismatches; byte-identical to the preflight.
- RU overlay is strict UTF-8/JSON, 3482 keys, zero duplicate/null/empty values, SHA256 `8961ad9d573ab72f63969e4a808b19ce733bc9a60b7ab589fc44dfb6148e1a0a`.
- Independent dictionary diff: 3186 retained, 286 added, 10 updated, 32 removed. Every removal has zero consumers across the five effective views and native ATD and is not a vanilla/native key.
- Handoff: 319 added / 41 removed / 42 changed-review keys; 317 / 5 / 42 respectively remain consumed. All five retained removed-main keys resolve.
- 4293 language-owned placeholder contracts match, including multiplicity, argument types/indexes, implicit order and literal percent escapes. All 296 changed values pass Latin/English-leak screening with explicit name/acronym/format exceptions.
- Provenance: 3186 retained Phase A + 211 official permitted LP 1.21 + 21 BSD source + 64 manual. Every changed final value has one owner and a matching hash; all 232 imports match the exact source value.
- Official LP permission is independently supported by `reference/localization/phase_a_ru_full_reaudit_summary.json` (YX5bAAJN / hsqY3G3V / 1.21 / Minecraft26.2). Permission remains AchieveToDo-specific.
- Better RU is CC0-1.0 and contributes zero new values. Its zero-count BSD label is `EVIDENCE_LABEL_ONLY`, with no runtime/license-use impact; B6 remains unchanged.
- BACAP RUS Translate 47BuT3SL / ULq4UdnT / 26.2 contributes exactly 21 values under BSD-2-Clause. All supplied attribution, exact disclaimer and complete canonical BSD template are preserved. `NOTICE_SOURCE_INCOMPLETE_UPSTREAM` records the absent filled upstream copyright notice; no holder/year was invented.
- All 53 selected companion keys resolve; overlay key counts are Hardcore50, Terralith81, Amplified7, Nullscape8. The Sulfur Cube hurt description has exact permitted-source provenance.
- All three former empty values (Gurgle, You have Goat to be kidding me!, hours of walking) are now nonempty and consumed.

The 42-key complete semantic certification and the full 64-manual-value semantic certification were **not finalized** after the test gate failed. Consumer presence, provenance, placeholders and nonempty values must not be mistaken for those semantic approvals.

## Additional failures beyond the accepted four

- `com.diskree.achievetodo.certification.PhaseACertificationTest.canonicalInventorySnapshotMatchesGeneratedArtifacts()`: Historical Phase A inventory embeds current RU availability; B6 removals change the generated snapshot. Frozen snapshot must not be regenerated here.
- `com.diskree.achievetodo.client.BetterRuOverlayResourceTest.productionRuOverlayMatchesApprovedFinalManualDecisions()`: Historical intentional-English decision forbids the now explicitly translated conjunction key and.
- `com.diskree.achievetodo.client.BetterRuOverlayResourceTest.pinnedHistoricalBacapSourceRemainsUnchanged()`: Historical source SHA is compared to the now-current BACAP enum pin. Independent archive SHA verification passes; the source was not altered.
- `com.diskree.achievetodo.client.BetterRuOverlayResourceTest.productionRuOverlayKeepsPinnedPhaseARuntimeResolvable()`: Historical regex-based keyset is fixed at 3507; current resources produce 3714. Independent B7 current effective extraction is separately reconciled.
- `com.diskree.achievetodo.client.ExternalPackCompatibilityPhaseATest.ensureWorldPacksUpToDateRepairsStaleWorldPackToCurrentCompatibleCopy()`: Test copies frozen BACAP 1.18.1 as the current main source, then asks current BACAP 1.21 freshness to accept it.
- `com.diskree.achievetodo.client.ExternalPackCompatibilityTest.worldCopyPreservesHistoricalGlowAndHoneyDefinitionsAndFrozenSource()`: Test expects the current enum source SHA1 on a conversion of the frozen historical archive; these identities now intentionally differ.
- `com.diskree.achievetodo.client.Pre26SmokeRegressionTest.everyShippedAdvancementMessageRetainsHoverClickAndFrameMetadata()`: The test recognizes only legacy hoverEvent spelling and requires exactly 1202 matches. All 1162 recognized messages passed codec/hover/idempotence checks before the fixed-count assertion. Native modern hover_event messages are outside that detector.

Failure names/types/messages and stack hashes, baseline XML identities, source hashes, key/provider/hash witnesses and removal proofs are recorded in the JSON. Full diagnostic bodies remain in ignored `build/tmp/phase_b_b7/`; no upstream corpus is duplicated in this report.

## Commands and safety

`python -X utf8 tools/phase_b/b7_localization_certification.py --self-test`: six tests PASS. This is the exact B4-named successor; no Java successor or translation fixture was added.

`./gradlew.bat --no-daemon --offline test -x generateBuildConfig`: FAILED as described above. No new assertions/exclusions/adapters were introduced to disguise the failure set.

`./gradlew.bat --no-daemon --offline processResources -x generateBuildConfig`: GREEN, UP-TO-DATE; processed RU hash equals source. `git diff --check`: PASS. No clean/cache deletion/dependency changes.

B7 runtime delta is 0 ADD / 0 MODIFY / 0 DELETE. Cumulative runtime diff remains 177 ADD / 66 MODIFY / 9 DELETE. All 251 B5 product paths, all preflight runtime bytes, prior evidence, historical tests, source archives and the B6 notice remain exact. Branch `phase-b-bacap-26.2`, HEAD `b261b02cd03b4aeae2835c63f982c9aa53c46eed`, marker `compat_26_2_r17`; nothing staged. No commit/push/tag/GitHub mutation. No B8.

PHASE_B_B7_STOP
