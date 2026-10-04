# B12-R1 — publication-guard-aware terminal reconciliation

Status: **PHASE_B_B12_TERMINALLY_RECONCILED**. `readyForB13=true`.
Packaging is deferred; `publicationReady=false`, `releaseAuthorized=false`.

The original B12 STOP was valid under its assembly-GREEN contract. B12-R1
changes only the terminal certification contract for the unversioned Phase B
milestone. The observed assembly stopped at `:jar` because the baseline
publication guard rejects a changed same-version candidate when the accepted
numeric four-component `0.1.5.4` JAR already exists. The guard remains intact.

## Preflight and separate preservation proofs

The read-only preflight passed before any R1 persistent write: repository
`D:\Vibecode\AchieveToDo 26.2`, branch `phase-b-bacap-26.2`, HEAD `b261b02cd03b4aeae2835c63f982c9aa53c46eed`;
no staged paths, unresolved operation, or conflict.

Historical B12 byte preservation used the original
`build/tmp/phase_b_b12/preflight/protected_files.json`: **3595 records**,
SHA256 `2286ce26676abffc23ab26ab6b7af7185161b2362a6c25b81146eb4223d597e6`. Both B12 historical/post preservation fields bind
that exact manifest. All 3595 current path bytes/states match; changed list is
empty. No historical snapshot was reconstructed from current bytes.

Separately, the start-of-R1 execution snapshot recorded **3600 pre-existing
durable paths**, including the five original B12 outputs; SHA256
`8f02c53e0cd6907e4012d020f08b1781ec67d00fdcf48e114956ddb7a4b70296`. Every pre-existing path remains byte-identical. Exactly the
four allowlisted R1 additions exist. Local refs, the tag, the published branch,
configuration, runtime, tests, GameTests, prior evidence, and accepted JAR are
unchanged.

## Original B12 evidence, immutable

| Path | SHA256 |
| --- | --- |
| `tools/phase_b/b12_terminal_reconciliation.py` | `cc3ebdb951a1382539c0e1350084bf19ec4d36143bec60946d6e8f1ef9811fd4` |
| `reference/phase_b/b12_terminal_reconciliation.json` | `c0476c21beaa3941af0dfab1b506fb78ec8cb539f673e95c533985c1e9aac94b` |
| `reference/phase_b/B12_TERMINAL_RECONCILIATION.md` | `9dd41b5349466064987dea93a995c72b6ea02dd9c4ae59bb94b725b2c75bf924` |
| `reference/phase_b/b12_product_ledger.json` | `c75b3cff8e80fd25f4bf94942145e1705b0eaeb13c7d04f9e277ee9668915038` |
| `reference/phase_b/b12_milestone_manifest.json` | `25d0320b11df842533434c88d40b25bdb54a6e195ca9710aa3bbccc268cca215` |

Original STOP reason: `NON_TEST_ASSEMBLY_BLOCKED_BY_EXISTING_PUBLISHED_VERSION_JAR_GUARD`. Original assembly command:
`.\gradlew.bat --no-daemon --offline assemble -x generateBuildConfig`;
failed task `:jar`, exit `1`; log SHA256
`22dd02407e001efacea791dc3264e4301848c92b41205c60da70eabdb205feeb`.
The original manifest remains `PHASE_B_B12_STOP`, `readyForB13=false`,
`stagingAuthorized=false`. It cannot authorize future staging.

## Assembly boundary

- `assembleGate=NOT_APPLICABLE_TO_UNVERSIONED_MILESTONE_COMMIT`
- `observedAssembleResult=EXPECTED_PUBLICATION_GUARD_BLOCK`
- `productionRegressionDetectedByCertifiedNonPackagingGates=false`
- `postGuardPackagingCertified=false`
- `packagingDeferred=true`
- Version remains `0.1.5.4`; no version selection or release authorization.

Actual baseline/current build.gradle guard logic matches; the whole source
matches under Git's LF/CRLF transport convention. No guard bypass, version
override, JAR move/deletion/rename/overwrite, assembly rerun, or new JAR occurred.
The block does not establish that later packaging tasks pass or that assembly
would pass after a future version bump. A separately authorized version/release
transaction must select a version and execute all packaging/publication gates.

## Carried-forward results, not re-executed in R1

Every row below is bound by the original terminal JSON SHA256
`c0476c21beaa3941af0dfab1b506fb78ec8cb539f673e95c533985c1e9aac94b` and the
3595-record protected-input manifest SHA above; its exact original JSON pointer
and available source/log SHA are retained. The original B12 tool is separately
hash-locked. Matching counts alone were not used as preservation evidence.

| B12 gate | Accepted result | Exact byte/evidence authority |
| --- | --- | --- |
| b7_localization_certification | 8 tests; 0 failures; 0 errors | `/testResults/b7_localization_certification`; `032c866f0eca68dbf7508c86034f00d6a06bf52f98e43efdee2c52e7fbc5a732` |
| b8_bacap_static_certification | 14 tests; 0 failures; 0 errors | `/testResults/b8_bacap_static_certification`; `18acf4be8234587916a72a2aa0a8b48bbacaccf796055b40b8a4cf76ff26936b` |
| b9_regression_certification | 8 tests; 0 failures; 0 errors | `/testResults/b9_regression_certification`; `839fc8e591863b4eb78d3f589acd3a3613da88252456894b9590522b826bb634` |
| b11_manual_advancement_review | 13 tests; 0 failures; 0 errors | `/testResults/b11_manual_advancement_review`; `676371845eeffd01e0d486ce8ab768a9a6fcf50c581f0fe8e5800c81e12539bb` |
| b8_junit | 15 tests; 0 failures; 0 errors | `/testResults/b8_junit`; `2c47effb43042f6407486e090e2144de4593f6e5231c18964d78a7b33555ba8f` |
| b9_junit | 23 tests; 0 failures; 0 errors | `/testResults/b9_junit`; `66ed689d95945c4b9b61f551a99a885d0f8e106da986e8953995da57bf2ada06` |
| fullSuite | 429 tests; 11 failures; 0 errors | `/testResults/fullSuite`; `32442993dfbe13eae4c8039a6d346aaf4a5ca537c2835fed9bb5f28390e3b597` |
| compileResourceGametestSurface | GREEN | `/testResults/compileResourceGametestSurface`; `27d39aed4ab185e47abd6ea10157d6fdc02d57d4cd948a7807beadc7c97cf253` |
| processedRuntimeResources | 249 outputs match; nine deleted outputs absent; RU hash exact | `/testResults/processedRuntimeResources`; `Bound by immutable B12 result` |
| b12Tool | 17 tests; 0 failures; 0 errors | `/testResults/b12Tool`; `cc3ebdb951a1382539c0e1350084bf19ec4d36143bec60946d6e8f1ef9811fd4` |

The full suite remains visibly red: 429 tests, 11 accepted failures, zero errors
and skips, partitioned as four accepted baseline failures and seven immutable
historical-oracle failures. Exact failure names and failure-message hashes match
B9; no new failure names or current regressions. No old test was edited or
excluded. All five non-packaging build/resource tasks were GREEN in B12.

Other carried original B12 reconciliations (exact section copies are in the new
JSON): old/target advancements **1229/1332**, common 1222, old-only seven,
target-only 110, byte-identical 594, semantic-identical bytes-different four,
semantic-changed 624; canonical **1152 → 1242**. Seven removal/reference mappings
remain reconciled without progress migration. Marker supersession chronology
and all stage evidence remain immutable.

Localization: 3482 dictionary keys, 4301 current requirements, providers
533 ATD / 3432 overlay / 325 vanilla / 11 neutral, all five effective views
fully covered; 4290 placeholder contracts, zero mismatches; provenance
3186 / 211 / 21 / 64 with zero ownership errors. Historical B7 counts remain
historical. Permission and notice findings retain their source-bound limitation
`NOTICE_SOURCE_INCOMPLETE_UPSTREAM`; no invented holder/year.

B8 retains all static codec, wrapper, reward, tracker/GUI, message, companion,
and effective-layer validations. B10 retains exact full live sets MAIN 3046,
HARDCORE 3046, TERRALITH 3074, AMPLIFIED_NETHER 3046, NULLSCAPE 3047; its actual
runtime receipts, reward/ability/Search/companion witnesses and stated client-UI
limitation remain unchanged. No codec/GameTest was rerun in R1.

B11 remains 734/734 reviewed: 600 PASS, 131 PASS_WITH_NOTE, three upstream
anomalies, zero product defects. `blazeandcave:adventure/whats_up_doc`,
`blazeandcave:technical/riddle_first_line`, and `minecraft:adventure/bullseye`
remain `UPSTREAM_NATIVE_ANOMALY_PRESERVED`; no speculative correction.

## Product and inventory

The exact original product ledger and current path hashes remain valid:
**177 ADD / 69 MODIFY / 9 DELETE = 255 runtime paths**. The current model is
249 original B5 paths exact, two superseded B5 paths, one B6 RU path and three
new post-B5 B8-R2 message modifications. R1 runtime delta is **0/0/0**.
Converter hash `6ab0674902b4783c39c08dfd802cd0067d1436d973345834f092a001a4db9fec`,
marker `compat_26_2_r19`, rootOverrideSha1
`cbc432be35d5525001872c430877541cfa1fcad6` and RU hash
`8961ad9d573ab72f63969e4a808b19ce733bc9a60b7ab589fc44dfb6148e1a0a`
remain exact.

Original inventory: **345 paths; 265 ADD / 71 MODIFY / 9 DELETE**.
Resumed inventory: **349 paths; 269 ADD / 71 MODIFY / 9 DELETE**.
Categories: runtime 255, license one, current tests eight, GameTest certification
nine, Phase B tools eight, Phase B evidence 68. Original path classifications
are preserved; only one tool and three evidence additions are appended.
The resumed path/status set equals independently read durable Git state exactly;
there are no generated/cache/temp/archive additions, duplicates or action overlap.

## Accepted artifact and publication claims

`build/libs/achievetodo-mc26.2+0.1.5.4.jar`, 1546595 bytes, SHA256
`0fc29fd0fbe1d874ca037ba2bd932f4e667593d95a1f2d3bb42936765710978b`, remains byte-identical to the accepted
0.1.5.4 artifact. Local v0.1.5.4 refs and 26.2-port remain at B12 authority.
`remoteMutationPerformedByB12R1=false`;
`remoteStateIndependentlyReverified=false`. No live remote/GitHub state query
or fetch was performed; no remote preservation claim beyond the actual evidence.

## Fresh R1 checks and future handoff

The new read-only verifier's **20/20 pure self-tests PASS**. It independently
verifies branch/index/operations, all original hashes, both preservation proofs,
guard provenance, accepted JAR, exact runtime ledger, inventory, manifest/status
equality and fail-closed staging eligibility. `git diff --check` PASS applies to
the tracked diff only. Each of the four untracked R1 text additions separately
passes UTF-8/final-newline/conflict-marker validation; both JSON files parse,
and the Python tool AST parses.

New persistent files, exactly four:

1. `tools/phase_b/b12_r1_terminal_resume.py`
2. `reference/phase_b/b12_terminal_reconciliation_resumed.json`
3. `reference/phase_b/B12_TERMINAL_RECONCILIATION_RESUMED.md`
4. `reference/phase_b/b12_milestone_manifest_resumed.json`

The new resumed manifest is the **future B13 staging authority**:
`stagingAuthorized=true`, `readyForB13=true`. Future B13 must reject the old
blocked manifest, use only the resumed manifest's exact ADD/MODIFY/DELETE lists,
and never use `git add .` or stage ignored/generated/cache/build/tmp paths.
This is a handoff policy: **B12-R1 did not execute B13 or stage files**; future
B13 behavior was not independently executed/certified. No commit, push, tag,
GitHub mutation or destructive Git operation occurred. Packaging, future release
behavior, remote state and client visual Search UI remain outside this proof.
