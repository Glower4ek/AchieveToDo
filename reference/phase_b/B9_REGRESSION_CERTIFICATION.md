# B9 regression certification — STOP

Status: **PHASE_B_B9_STOP**. `readyForB10 = false`.

The required unchanged B7 successor command ran eight tests: seven passed and one failed. `ExtractionContractTests.test_current_effective_coverage_and_provenance` calls `precheck()`, which still requires `MARKER_VERSION = compat_26_2_r18` at `tools/phase_b/b7_localization_certification.py:52`. The accepted current converter is r19, SHA256 `6ab0674902b4783c39c08dfd802cd0067d1436d973345834f092a001a4db9fec`. The failure occurs before effective localization coverage is recomputed.

Inspection also finds two subsequent superseded precheck assumptions: line 57 requires the old B7-R1 converter hash, and the B5 preservation loop excludes only the converter, without accounting for the accepted B8-R2 potion/root supersession. These later checks were not executed. This STOP does not establish a new runtime localization regression. The unchanged B7 certification tool cannot currently satisfy B9's mandatory green-successor gate.

B9 authorizes only its own certification tool, JUnit successors, compact receipt, and evidence. No B7 tool repair or waiver was performed. The original B7/B8 certification records remain immutable. A scoped certification-authority resolution is needed before resuming B9; no converter or localization mutation is indicated by this failure.

## Checks completed

- Branch: `phase-b-bacap-26.2`; HEAD: `b261b02cd03b4aeae2835c63f982c9aa53c46eed`; nothing staged.
- Accepted B7/B8, repair, source-preservation, runtime, and fixture hashes passed preflight. B5 preservation model: 249 exact original paths and two accepted supersessions.
- Pre-B9 full Gradle task: **406 tests, 11 failures, zero errors, zero skipped**, parsed from actual JUnit XML. Arithmetic: 391 prior tests plus 15 accepted B8 methods. Failure names match exactly the four accepted baseline methods plus seven certified historical-oracle methods; no new names. The normal red suite remains visible.
- B8 tool self-tests: **14/14 PASS**. B8 JUnit within the full suite: **15/15 PASS**. A separate filtered B8 task was not run after the B7 gate failed.
- Fresh compiled-production preliminary probe: native main **1244** intentional announcement changes; native Hardcore **17**. Historical migration semantic changes and native function changes are zero. Selected-policy second-pass entry comparison has zero byte/semantic changes for native main, native Hardcore, historical main, and all three retained historical BACAP companions.
- Production source identity, retained companion currentness, individual marker-field rejection, pre-R2 root digest rejection, and current digest acceptance passed. Current root digest: `cbc432be35d5525001872c430877541cfa1fcad6`.
- Production world sync replaced historical, malformed r17, old r18, and pre-R2 copies with current verified-source copies. Subsequent sync reports `ALREADY_CURRENT`.
- Treasure Hunter real Minecraft 26.2 holder-set codec witness passed for native/current forms and rejected the defective r16 form. Narrow player-predicate guards, two-helper idempotence, historical enchantment scalar conversion, item/component command, daytime, and gamerule preliminary witnesses passed. These are preliminary checks, not a completed B9 family/corpus certification.
- `git diff --check`: PASS.

The exact eleven method names, assertion summaries/hashes, preliminary probe facts, and validation log hashes are recorded in `b9_regression_certification.json`. Complete class counts and XML hashes are in its ignored temporary receipt under `build/tmp/phase_b_b9/`.

## Work not completed

No B9 durable tool, receipt, or JUnit successor was created after the required B7 gate failed. The remaining family/corpus scan, fresh full-advancement codec witnesses, filtered B9/B8 tasks, final full-suite rerun, and completion build checks were not certified. B9 success and successor closure are not claimed.

## Safety and handoff

Runtime transaction delta: **0 ADD / 0 MODIFY / 0 DELETE**. Unique cumulative runtime diff remains **177 ADD / 69 MODIFY / 9 DELETE**, **255 paths**. All runtime bytes, prior evidence, historical tests/snapshots, B7/B8 tools, pins, converter, and localization remain unchanged. No staging, commit, push, tag, GitHub mutation, or runtime world launch occurred. B10 was not started.

**PHASE_B_B9_STOP**

`readyForB10 = false`
