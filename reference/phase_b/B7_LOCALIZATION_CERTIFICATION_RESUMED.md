# B7 — Localization Certification Resumed After B7-R1

Status: **PHASE_B_B7_LOCALIZATION_CERTIFIED**. `readyForB8 = true`. B8 has not started.

The initial B7 STOP and regression-resolution STOP remain immutable. B7-R1 repaired actual-source marker attribution; this certification uses r18 and the accepted current converter hash. Runtime product bytes were preserved.

## Seven-failure disposition

| Test | Current status | Disposition / proof | Successor |
|---|---|---|---|
| certification.PhaseACertificationTest.canonicalInventorySnapshotMatchesGeneratedArtifacts() | FAIL | Historical oracle staleness: Historical snapshot matches HEAD bytes; old generator observes current runtime. All 22 differences are authorized: 21 removed-key RU availability flags and the Benchmarking wrapper source ownership. Frozen advancement identity/structure and all other fields match. | B8 |
| client.BetterRuOverlayResourceTest.productionRuOverlayMatchesApprovedFinalManualDecisions() | FAIL | Historical oracle staleness: Exact key and resolves to и. Live main config/intro/welcome join Blaze and Cave; conjunction semantics preserved, no placeholders. B6 MANUAL_B6 ownership and value hash match. Current successor independently certifies consumer, meaning, and provenance; old Phase A prohibition is stale. | B7 |
| client.BetterRuOverlayResourceTest.pinnedHistoricalBacapSourceRemainsUnchanged() | FAIL | Historical oracle staleness: Frozen source bytes still match historical SHA1/SHA256. The old test calls isPinnedHistoricalSource, which compares those bytes against the intentionally different current enum pin. R1 probe rejects historical raw as current; both identities are explicitly certified by the B7 successor. | B7 |
| client.BetterRuOverlayResourceTest.productionRuOverlayKeepsPinnedPhaseARuntimeResolvable() | FAIL | Historical oracle staleness: Historical test extracts frozen old BACAP plus CURRENT ATD overlays, then compares with fixed historical count. HEAD overlays reproduce 3507; current overlays reproduce 3714. Intersection 3506, historical-only 1, current-only 208. Every current-only key has authorized B5 ownership and a current independent provider; effective coverage has zero missing keys. | B7 |
| client.ExternalPackCompatibilityPhaseATest.ensureWorldPacksUpToDateRepairsStaleWorldPackToCurrentCompatibleCopy() | FAIL | Historical oracle staleness: Test copies frozen 1.18.1 to global/bacap.zip and immediately asserts that it is the CURRENT enum source. It fails at that initial assertion before world-sync repair. Current r18 correctly rejects this raw old source, and the accepted R1 production probe proves sync repair with verified current 1.21 input. | B9 |
| client.ExternalPackCompatibilityTest.worldCopyPreservesHistoricalGlowAndHoneyDefinitionsAndFrozenSource() | FAIL | Historical oracle staleness: After R1 this same method still fails at line 824 BEFORE conversion: it compares the frozen archive SHA1 with CURRENT ExternalPack.BACAP.getSha1(). That remaining oracle is stale. The separate production defect originally discovered during its investigation was real and is now repaired: historical r18 markers record actual historical SHA and cannot validate as current-main copies. | B9 |
| client.Pre26SmokeRegressionTest.everyShippedAdvancementMessageRetainsHoverClickAndFrameMetadata() | FAIL | Historical oracle staleness: Legacy detector requires hoverEvent spelling and fixed count 1202. Current detector yields 1162: 31 authorized MODIFY files use modern hover_event/click_event, 9 authorized DELETE files have existing validated move destinations. Historical-only 40, common 1162, current-only legacy 0. Independently enumerated current corpus contains 1300 messages, all passing real MC26.2 component codec, hover, Search click, frame/style, roundtrip and migration-idempotence checks. | B8/B9 |

The historical source test remains red at its raw-source SHA assertion before conversion. That oracle staleness is separate from the real r17 marker bug discovered during investigation; the real bug is repaired and the current compiled R1 production witness remains valid.

## Independent certification

- 42/42 changed-component keys reviewed independently; no failures or unresolved items. Per-key findings, current consumers, native hashes, mechanical/numeric checks, provider ownership and placeholders are in the resumed JSON.
- 4304 union requirements: 3435 Minecraft RU overlay + 533 ATD RU + 325 vanilla 26.2 RU + 11 language-neutral.
- Main 3702/3702; Hardcore 3702/3702; Terralith 3756/3756; Amplified Nether 3702/3702; Nullscape 3705/3705. Missing = 0 in every view.
- Dictionary 3228 → 3482: 3186 retained, 286 added, 10 updated, 32 removed. Every removed key has zero current consumers.
- 4293 placeholder contracts checked, zero mismatches. 296 changed values screened, zero genuine English leaks.
- Provenance: 3186 retained Phase A, 211 permitted official LP 1.21, 21 other permitted BSD source, 64 manual B6. Zero missing/duplicate/hash-mismatched owners.
- Current conjunction `and` → `и` has live native coauthor-join consumers and verified manual provenance. Frozen historical BACAP and current main pin are certified as distinct identities.
- Historical runtime sets: 3507 → 3714; intersection 3506, historical-only 1, current-only 208. All current-only keys map to authorized B5 consumers and current providers.
- Historical message detector: 1202 → 1162; 31 modern-field rewrites and nine validated moves explain all 40 historical-only paths. Actual current applicable corpus: 1300; all pass real MC26.2 codec, hover, Search click, frame/style, roundtrip and idempotence checks.

## Source permissions and notice

Official LP project-specific permission passes. Better RU actual license is CC0-1.0, with zero new imports; the zero-count BSD-named B6 bucket is evidence-label-only and has no runtime/license effect. BACAP RUS Translate supplies 21 BSD-2-Clause values. Notice remains `NOTICE_SOURCE_INCOMPLETE_UPSTREAM`; available attribution/disclaimer are preserved and no holder/year is invented.

## Execution and product preservation

B7 successor: 8/8 tests pass. Full Gradle test task: 391 tests, 11 failures, zero errors: four accepted baseline failures plus seven individually proven historical-oracle stale failures; no new, unresolved or current real-regression failures. The red suite remains visible for B8/B9 successor reconciliation.

`processResources` is GREEN (UP-TO-DATE); processed RU bytes match the source SHA256. `git diff --check` passes.

Resumed B7 runtime delta: 0 ADD / 0 MODIFY / 0 DELETE. Unique cumulative runtime diff: 177 ADD / 66 MODIFY / 9 DELETE. 250 B5 hashes are exact; the converter alone uses accepted B7-R1 authority. Prior evidence/tests/snapshots/localization/gameplay resources are unchanged. Nothing staged; branch/HEAD unchanged; no commit, push, tag or GitHub mutation.

## Durable outputs

- `b7_post_r1_test_gate_resolution.json`
- `B7_LOCALIZATION_CERTIFICATION_RESUMED.md`
- `b7_localization_certification_resumed.json`

Future ownership is documented only: B8 target inventory/resource certification and B9 converter/freshness/message successors have not been implemented. B12 must reconcile the explicit post-B5 R1 correction.
