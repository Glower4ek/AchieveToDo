---
name: achievetodo
description: Work on AchieveToDo gameplay research and approved implementation, compatibility, verification, releases, or repository hygiene; preserve design approval, project semantics and evidence boundaries.
---

# AchieveToDo Project Skill

## 1. Purpose

Use this skill for project work in this repository. Follow the user's current
task and its completion gates. Establish evidence, make only authorized changes,
and prove the resulting scope. This skill supplies project policy, not permission
to start another stage or perform external publication.

## 2. Repository Identity

- Repository: AchieveToDo 26.2; discover its root from the actual checkout.
- origin: `https://github.com/Glower4ek/AchieveToDo.git`.
- upstream: `https://github.com/AchieveToDo/mod.git`.
- Java namespace remains `com.diskree.achievetodo.*`; fork ownership does not
  change namespace ownership or attribution.
- Read the active and historical branch identities in section 3; verify actual
  Git state before work. A historical default branch is not a switch instruction.
- The compatibility work originated in singleplayer / integrated-server gameplay;
  another server platform requires an explicitly selected task scope.

## 3. Current Certified Baseline (mutable)

Keep mutable milestones, counts and identities here. These are accepted historical
facts within their tested scope, not instructions to reset an advanced checkout.
Verify actual state and bind relevant evidence before relying on certification.

| Fact | Accepted baseline |
| --- | --- |
| Minecraft / loader | Minecraft Java Edition 26.2 / Fabric |
| Mod version in source | 0.1.5.5; no new release implied |
| Active branch | phase-c-core-integrity-26.2 |
| C7 checkpoint | 651b460df1735316b8863633e7b4a73e4fe375cf |
| C8 / current accepted HEAD | ce8a6e56b457a8ee9c6da59b1e93eaab481405c0 |
| Accepted tree | 33bd1966b4a0db391dd069d5e4f3ccc804f16b71 |
| Phase A / B | COMPLETE / HISTORICAL; release 0.1.5.5 preserved |
| Phase C | C0-C9 independently reviewed and accepted within documented scope |
| C8 standard JUnit | A2: 443/443; B2: 443/443; zero failures/errors/skips each |
| C8 Java targets | compileJava, compileTestJava, compileGametestJava PASS each build |
| Permanent GameTest registration classes | 42; registration is not runtime execution |
| C8 production JAR | 1637879 bytes; SHA256 d61fbf15fd27843fbdba8af82b6fdeacb586b6322d6e5b9b4688f9ba43835d3e |
| C8 reproducibility | Independent seeded clean-source builds; actual byte equality without normalization |
| C9 runtime | Current-source Minecraft 26.2 server: 12 successful final GameTest invocations, 292 successful runtime assertions |
| C9 permanent / temporary runs | Seven permanent reconciliation methods + five semantic/control/restart runs |
| C9 client GUI | Not rerun; actual C5 client evidence is historical source continuity |
| C9 final evidence | build/tmp/phase_c_c9_autonomous.zip |
| C9 evidence SHA256 | 9a1282fb486057d79351637e9a5898df5fcc9eed29e5c4e4d356adc95aa63683 |
| Phase D | Research/design only (Mode A); no gameplay implementation authorized |

For baseline-dependent work, bind C7/C8 receipts and C9's sealed manifest,
`final_certification_decision.json`, `current_runtime_certification_results.json`
and `C7_C8_integrity_reproducibility_binding.json`. Evidence under `build/tmp` is
local and may be absent in a clean checkout: obtain the approved hash-bound bundle
when required; report missing proof rather than inventing it. The acceptance of
C0-C9 is owner/controller authority; receipts describe the technical proof.

C7 proved STRICT artifact checks and selected negative controls. C8 ran two
independent full JUnit builds, compiled all three Java targets and retained all 42
registrations; it did not run server GameTests. C9 reused C8 JUnit XML (886 accepted
executions, no new JUnit run), checked freshly compiled source continuity and ran
selected real server GameTests. Persistence/restart, reward and ownership regression
claims are limited to the documented scenarios. C9 also matched all 1,357 effective
BACAP/ATD advancement IDs and criteria; this is not proof of every gameplay path.

The selected design remains B-min canonical server reconciliation, no new durable
state; D01-A/D02-A and RD01 remain accepted. C5-R1's actual client evidence resolved
C2-09 and exercised 19 runtime groups / 16 invariants. Its repeated eligible packet
toast/sound observation did not imply repeat server reward. Consult sealed C3-R1
design and C5-R1 evidence when changing these contracts, not pre-C4 forensic oracles.

Historical published release 0.1.5.5: release branch `26.2-port`, release
commit `d96f39e540692eaa3a7447ee2ce5f7a81a3658f8`; annotated tag `v0.1.5.5`,
object `3b3fc1e504d261c1cb2aab2de9e4d69e9bf5b663`, peeled release commit
`d96f39e540692eaa3a7447ee2ce5f7a81a3658f8`. Separate later Phase C0 checkpoint:
commit `a187a5b67bd41f625fa1adbc104194b5e3d7c59f`, tree
`9793cf1fd378d042a2fe5d8f51c5f58152625731`; this is not the release commit.
The current active Phase C branch is recorded in the table above. Published JAR
`achievetodo-mc26.2+0.1.5.5.jar`: 1648665 bytes, SHA256
`c40545ba47e2cf0b8ad45ca76a6e5313b723c911dea8ea05adaf395d6a79c10f`, SHA1
`c3f37a4067ff9334c3cf8043f66a6755f2a4a016`; 435-test historical release suite and
actual Phase B client certification remain historical, distinct from Phase C.
C7/C8 were normally backed up to the Phase C branch (accepted C8 backup receipts);
that branch backup is not a new tag/release or a fresh remote-state query.

B16B-B21 release/integration/audit/synchronization are historical. When relevant,
bind `build/tmp/phase_b_b21/postflight.json` to SHA256
`b2bec3e4ff48eb60e4a16b12dfca4f99c32f26c32f5ecd450de4cb41713a8feb` and
`PHASE_B_B21_LOCAL_GIT_SYNC_CERTIFIED`. B23 tracked cleanup was remote-integrated;
B24/B25 local audit/cleanup completed; B26 `RESIDUALS_FOUND / ACCEPTED` remains
deferred until the next authorized release cleanup. Its eight roots / 386477081
logical bytes (product risk false) are not a Phase D blocker or cleanup permission.
Consult `build/tmp/phase_c_c0/phase_b_authority_binding.json` and
`b26_deferred_cleanup_debt.json` for exact authority and roots. No GitHub cleanup
is authorized by Phase D research or by this skill.

## 4. Authority and Evidence Rules

Resolve current-state questions in this approximate authority order:

1. Actual current repository / Git state.
2. Task-specific durable evidence and accepted receipts.
3. Current checked-in source, tests and resources.
4. This skill's compact certified baseline.
5. Historical planning and frontier documents.
6. Chat memory.

If the repository legitimately advances beyond the baseline, report stale skill
facts and propose an update. Apply it only within explicit task authorization;
D0-Pre is proposal-only pending owner/controller review. Never force the repository
backwards. Current-state evidence establishes facts, not approval of game design.
Historical evidence proves history, not automatically current behavior.

Bind relevant evidence to commit/tree, artifact/catalog hashes, exact key sets,
versions and run provenance. Preserve immutable accepted evidence bytes and the
frozen BACAP canon; a runtime-compatible pack never replaces the frozen source.
The frozen archive is `reference/phase_a_preservation/files/final/bacap.zip`.
Use verified official/Mojang artifacts for the target Minecraft API/bytecode;
an older Yarn cache is not current evidence.

Treat `build/tmp` as scratch. A task-bound, hash-verified receipt can be authority;
an arbitrary JSON, filename or timestamp cannot establish freshness. Prefer fresh
compiled classes and an isolated process when verification semantics require it.
Validate generated-versus-checked-in snapshots and protected hashes when relevant.

Validate temporary evidence before authorized promotion; diagnostic canaries
remain diagnostic. Persistent evidence validates from its own catalog and metadata,
independently of newer transient run-state, with a nonblank root run ID and every
receipt matching it. Keep catalog, version, source, family and semantic guards
fail-closed. Reuse earlier immutable proof without rewriting it or confusing stages.
Reopen closed evidence only for demonstrated invalidation or explicit task scope.

A failed verification gate is NEVER a PASS. Record the failure and establish
its root cause. During an explicitly authorized autonomous engineering task,
independently diagnose and repair ordinary compiler, Gradle, test, cache or tooling
failures within the approved scope; re-run affected checks and continue toward
the task's final completion criteria. Routine technical recovery does not require
repeated approval. Keep evidence integrity and security gates fail-closed.
STOP only when further progress requires unauthorized changes, unknown trust
authority, weakened security, unexpected protected-file mutations, altered approved
game-design decisions or an irreducible blocker. Report the failed gate, observed
state, root cause, partial changes and minimal required next action; missing or
partial proof never becomes complete success.

## 5. Mutation Boundaries

Inspect current files, branch, HEAD/tree, index, diffs and relevant receipts before
mutation. State the exact authorized mutation envelope: paths, operations and
expected deltas. Preserve unrelated tracked bytes and existing local work.
Avoid unrelated changes, opportunistic refactors and cleanup outside explicit
cleanup stages. Recorded debt is not authorization to repair it.

Verify preconditions before each mutation; high-risk Git/release operations need
fresh immediate pre-mutation validation. Afterward compare actual path/content
deltas, refs and protected hashes against the envelope. STOP on any extra delta.
Read-only and local-tooling tasks do not authorize product edits, builds, tests,
asset writers, gameplay, evidence promotion or Git mutations that the task excludes.

## 6. Product / Test Semantics

For release-bound work, target ZERO accepted red standard tests. Classify each
failure before changing anything: `PRODUCT_DEFECT`, `TEST_DEFECT`, `STALE_ORACLE`,
`FIXTURE_DEFECT`, `ENVIRONMENT_FAILURE`, or `EXPECTED_GUARD`. Historical labels such
as harness/classification errors must be mapped to their actual cause.
For bug fixes, prove the production defect before editing production. Implement
features only within the selected feature scope. Never weaken tests to get green
or rewrite an oracle to match broken behavior. For a stale oracle, independently
prove production semantics and preserve meaningful negative controls.
An old known failure does not remain acceptable indefinitely.

Preserve these accepted compatibility contracts unless the task explicitly
changes them with independent evidence:

- Legacy player predicate spelling stays `gamemode`, not `gameMode`.
- Preserve compatibility tags `minecraft:nether_roots` and `minecraft:nether_fungus`.
- Scalar enchantment IDs normalize to singleton arrays only inside
  `predicates.enchantments[].enchantments` and
  `predicates.stored_enchantments[].enchantments`. This is not generic HolderSet
  rewriting; do not broaden to biome, location or unrelated scalars by analogy.
- Llama carpet NBT conversion is limited to the sixteen exact frozen carpet forms
  under `#blazeandcave:llamas`, mapping to `equipment.body`, with cache marker
  `llamaCarpetNbtMapping=equipment.body`. Preserve near-miss/non-llama/modern forms,
  idempotence, stale-copy detection and unrelated output fingerprints.
- When touching the historical certification harness, keep its primary context
  `RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).freeze()`
  limited. Secondary `VanillaRegistries.createLookup()` is certification-only;
  fallback IDs are exactly `minecraft:banner_pattern` and `minecraft:instrument`,
  excluding `minecraft:enchantment`. Full-registry success alone cannot certify
  limited-context decoding. Account for every diagnostic before collapsing
  allowlisted noise; pure tags and enchantments remain fail-closed when context
  is missing, even if the full lookup succeeds.
- Keep `Set<RegistryDependentComponent> registryComponents`; `registryComponent()`
  returns the sole component only for singleton sets, otherwise `null`.
  Real multi-context cases retain `MULTIPLE_CONTEXT_REQUIRED` and all components.

### Advancement ownership and reconciliation invariants

D01-A rewards follow a false-to-true completion transition for the current
advancement definition, not a lifetime-once reward ledger. Repeated completed
witnesses do not create another completion transition. Revoke/regrant or changed
definitions must be assessed against that policy; do not silently introduce a
new durable ledger or promise permanent once-only rewards.
D02-A observes the selected scoreboard objective: solo uses the player's selected
score; the four team modes sum selected scores of current canonical members,
including available offline-member scores. A player without a team gets zero in
team mode. Score mutation/removal, objective replacement, membership changes,
join and successful reload reconcile derived observation through server state.
Readiness guards remain fail-closed. Equal numeric observations do not automatically
repair criterion mirrors. Preserve native player advancement progress, cooperative
criterion propagation, frozen-state behavior and configuration-derived thresholds.
See `AchieveToDoServer`, `AdvancementsMode`, `PlayerAdvancementTrackerMixin` and
scoreboard mixins for current behavior; C9 proves only its recorded scenarios.

## 7. Build and Runtime Verification

Separate static analysis, compilation, unit tests, packaging and observed real
runtime evidence. A compile is not test success; focused tests are not a full
suite; a built JAR is not runtime certification. Record exact commands, selectors,
counts, exits and evidence scope. An `EXPECTED_GUARD` proves only that guard,
not downstream packaging or gameplay success.

When build work is authorized, use Java 25 and the pinned Gradle wrapper. Resolve
actual versions from `gradle.properties`, `gradle/wrapper/gradle-wrapper.properties`
and `build.gradle`; the accepted baseline uses Gradle 9.8.0, Fabric Loader 0.19.5,
Fabric API 0.161.0+26.2, Loom 1.18.2 and JUnit 6.1.3. Resolve valid Java and Python
executables; set `JAVA_HOME`, `ACHIEVETODO_PYTHON`, `ACHIEVETODO_TEST_INPUTS_DIR`,
`GRADLE_USER_HOME`, `TEMP` and `TMP` in the same PowerShell process as the wrapper.
Windows local-pipe startup may require a short writable task-local TEMP/TMP path;
`codex_java_uds_probe` is a historical workaround name, not a universal directory.

Use disposable, independently writable task-local Gradle User Home, project cache,
temporary/output roots and approved seeds. Preserve original caches. Required
wrapper flags: `--no-daemon --no-watch-fs --project-cache-dir <task-cache>
--dependency-verification strict`. Online resolution through configured repositories
is the accepted policy; do not restore mandatory offline bootstrap. An empty cache
is not an approved binary trust root. Respect existing execution capabilities.

Run authorized stages separately and record commands/results:
1. `verifyCleanSourceInputs`.
2. `compileJava compileTestJava compileGametestJava`.
3. `test --rerun-tasks` (unfiltered standard suite).
4. `processResources jar` only with validated isolated archive destinations and
   the existing same-version/historical-artifact guard preserved. This pinned
   setup has no `remapJar` task.
5. Selected `runGameTest` only when actual runtime execution is in scope. Pass
   explicit filter/verify/report selectors; compilation never substitutes for it.

Before launching, consult `tools/build/verify_inputs.py` and
`src/test/resources/build_inputs/manifest.json`: DISTRIBUTION-B requires the exact
20 private external files (nine ZIPs, five acquisition JSONs, five metadata JSONs,
one BSD template), outside the checkout under `ACHIEVETODO_TEST_INPUTS_DIR` and
conforming to the verifier's path guards. No automatic fixture acquisition or
historical fallback. Missing/extra/changed inputs and symlinks/junctions fail closed.
The verifier and manifest are committed and hash-pinned in `build.gradle`; source
identities and the 372 selected dependency inputs remain fail-closed. The accepted
native XML graph pins 178 selected binaries; C9 selected 175, with the three JUnit
engine binaries covered by C8. Do not broaden trust by accepting replacement bytes.

`verify-metadata=false` and `verify-signatures=false` remain explicit. STRICT checks
selected artifact identities; it does not verify native metadata checksums with
this configuration. The supplementary gate covers its 372 listed inputs, including
listed descriptors; newly acquired POM/module metadata are outside that pinning
envelope. Observed provenance is provisional, not publisher authenticity or
permanent trust. A corrupt Gson control proved rejection before Loom application,
not universal pre-consumption bootstrap ordering. Keep source integrity, binary
identity, metadata trust, publisher authenticity and ordering as separate claims.

C8 reproduced AchieveToDo output from independent clean sources with approved
Gradle/dependency/conditional metadata seeds, resolved official assets and the
approved processed Minecraft dependency SHA256
`49353b010c1f426ab441140e2ff0692d4cd6b7b32c681308d37d833e8b89b14a`.
The private fixture bundle is also a prerequisite. Wholly cold Minecraft dependency
regeneration was not certified. Two equal builds do not authenticate upstream.
Tests receive the verified current Minecraft JAR and vanilla RU asset identities
through Gradle wiring; historical Yarn/datagen cache paths are not substitutes.

Do not run clean by default, delete caches, substitute manual javac/handcrafted
classpaths or change JVM/network/provider hacks to force green. README's C8-future
and ignored-verifier notes are historical lag; use committed tooling and the sealed
C8/C9 receipts for those facts, while preserving its valid safety instructions.

Runtime/gameplay verification must observe production-equivalent events and live
criterion progress. Prove false-to-true transitions and actual entity/item/tag/
component/structure state where relevant. Derive ability unlock thresholds from
live configuration rather than old gameplay scores. Preserve frozen AND/OR
requirement semantics: one valid OR witness is not proof of every alternative.
Use valid finite-material fixtures, component/enchantment limits and ordinary
vanilla trigger flow. Direct advancement grants, direct trigger/listener calls,
fake predicates/lookups or copied expected-data receipts do not prove natural
criterion completion.

For server GameTests requiring a player, use a plain SURVIVAL, non-spectator
`ServerPlayer`, join via `PlayerList.placeNewPlayer(...)`, register the real
`Connection` in `ServerConnectionListener.connections`, send
`ServerboundPlayerLoadedPacket`, use the normal scheduler and live
`AdvancementProgress`. Verify selector/count and recorder ownership before runs;
avoid concurrent writers and verify-mode batching for single-run recorders.
Reclaim partial fixtures on failure; retain idempotent player/world cleanup and
bounded channel settlement, including outbound flush/drain before disconnect.
Do not kill unrelated Java processes or accept cleanup warning debt as clean proof.

Audit exact receipt keys, missing/extra/duplicate coverage, run provenance, player
uniqueness where required, and cleanup before accepting runtime evidence.
Never fabricate human interaction or claim GUI/runtime success without observed
evidence. Use durable logs and save data where they prove the result. Persistence
and reload claims require actual persistence/reload observations. If required
Minecraft GUI control is unavailable, use the human-handoff policy in section 10.

## 8. Git Safety

Preserve published history. No hidden reset/restore/clean/stash/amend, broad staging,
branch switch or other Git mutation outside the task's authorized operations.
Use merge/rebase/cherry-pick only when the task explicitly selects that strategy.
Do not push upstream by default. No force push unless an explicit extraordinary
task authorizes it; no implicit multi-ref push. Use explicit refspecs for sensitive
integration/publication and re-query actual remote state immediately before
important ref mutations.

Distinguish local branches, remote-tracking refs and live remote refs. Tracking
refs can be stale after API-side changes. Preserve safety branches and tags unless
their removal is explicitly in scope. Classify `refs/codex/turn-diffs/**` as platform
diagnostic refs, not project publication refs.

Windows SChannel may fail in this sandbox. If demonstrated necessary, use only a
process-local fallback `http.sslBackend=openssl` with `http.sslVerify=true`.
Never disable TLS verification or persist workaround configuration to bypass the
sandbox. Prefer existing authenticated tooling. Never print or persist GitHub
tokens, Authorization headers or passwords in commands, logs, receipts or files.

## 9. Release / Publication Safety

Treat source certification, full-suite green authority, package certification,
runtime certification, exact artifact identity, branch integration, tag identity,
release publication and post-release audit as distinct states. A JAR's existence
does not mean released. Bind artifact size, SHA256/SHA1 and exact bytes to the
certified source/tree and observed runtime package.

For publication-sensitive releases prefer: draft release -> exact asset upload ->
fresh downloaded asset hash proof -> publish -> fresh public asset hash proof.
Validate annotated tag object, peeled commit, message and signature state against
the intended identity. Never casually move an already-published version tag,
bypass packaging/publication guards or force publication. Partially published
external state needs an explicit recovery decision; do not automatically roll
it back merely to make a workflow appear atomic.

## 10. Sandbox and Human-Handoff Policy

Use current granted capabilities and exact minimal commands. Preserve permissions,
ACLs, credential configuration and sandbox architecture; historical environment
instructions do not override the current environment or user's scope.

If a required step lacks GUI input, native permission, credential-prompt support
or `.git/objects` write access, prove the limitation, prepare all independent
reviewable work, narrow the required human action to one minimal command/action,
and STOP. Do not invent automation, bypass guards or change permissions.
After the human step, independently verify machine-verifiable state; prose alone
is not success evidence when logs, hashes, refs or save data can prove the result.

## 11. Historical-State Policy

Phase A/B material is historical context unless the task explicitly audits it.
The frozen BACAP and official-mapped compatibility history explain preserved
fixtures and narrow migrations, not an active phase frontier.
Never use old Phase A currentPriority, static frontier counters, family queues,
autonomous-loop continuation or old Phase B stage numbers as commands to resume
work. Historical paths `reference/phase_a_planning/`,
`reference/phase_a_preservation/` and `src/test/resources/phase_a_certification/`
are not automatic current workflow instructions. The local historical
`phase-a-certification` helper is only for an explicitly scoped history task;
its old update flow cannot authorize builds, writers or gameplay.
Retention/deletion of historical material is a separate hygiene decision.
Keep dynamic state in section 3 and current governance in section 13; keep receipt
chains and family diaries in evidence, not this operating skill.

## 12. Repository Hygiene Policy

For an authorized cleanup audit, classify each candidate with evidence as `KEEP`,
`DELETE`, `ARCHIVE`, `UPDATE` or `NEEDS_REVIEW`. Consider runtime/build use,
source/test/tool references, Gradle wiring, packaging inclusion, licensing and
attribution obligations, reproducibility value, fixture dependencies, historical
phase-only purpose, and preservation already available in Git history/tag/release.

Do not delete licenses, attribution, third-party notices, fixtures, reference data
or validation tools merely because they look old. Do not retain obsolete root
handoffs/frontier diaries merely because they once mattered; Git history is valid
preservation for many obsolete tracked documents. Cleanup follows reviewed scope,
with protected dependencies and unrelated bytes verified afterward.

## 13. Phase D Workflow and Authorization

Phase C0-C9 is accepted within section 3's boundaries. Phase D adapts gameplay
capabilities, unlocks and UX to Minecraft 26.2. Current mode is A. No roadmap,
draft, recommendation, passed test or general Phase D approval authorizes gameplay
implementation. Autonomous investigation does not imply game-design authority.

### Mode A — Research and design (current)

Autonomously research versions and vanilla registries, inspect existing code,
inventory capabilities/thresholds, analyze BACAP relationships, classify behavior,
compare alternatives and prepare complete mapping reports/evidence within the
task's research scope. Every proposal is `PENDING_OWNER_CONTROLLER_REVIEW`,
including apparently obvious inheritance or shared unlocks. Autonomously recover
research and diagnostic failures within the authorized research/evidence scope;
this does not permit gameplay/source implementation.
Do not approve design, change thresholds, assign unlock categories, merge a new
item into a permission category, add/remove abilities, change rewards, modify
gameplay source, production resources/UI or implement proposed mappings.
Research scratch and drafts go only in the task's dedicated ignored evidence
workspace; preserve originals. D0-Pre reviews this skill only: full D0-A/B/C content
audits require the next separately authorized task and must not start here.

### Mode B — Approved design specification

Owner and controller review the COMPLETE proposed mapping, with all relevant
exact registry IDs enumerated even for shared capabilities. Record a durable,
reviewable specification separating (1) existing code behavior with evidence,
(2) agent recommendation and rationale, and (3) owner/controller-approved decision.
The decision for each relevant object/action must explicitly select: inherit
existing permissions; share existing unlock; introduce independent unlock;
remain unrestricted; or require further review. State exact thresholds,
advancement relationships, interface changes and progress/team migration effects
where applicable. Preserve unresolved rows as pending; Codex cannot approve them.
Preparation/recording must follow the authorized output envelope. Approval of
design alone does not authorize implementation or repository edits.

### Mode C — Explicitly authorized autonomous implementation

Begin gameplay implementation only after the owner explicitly approves the relevant
complete specification following owner/controller review AND explicitly authorizes
its implementation. Bind authority to the specification version/hash and the
complete selected scope; drafts and general roadmap approval are insufficient.
Rows marked require-further-review cannot enter the implemented scope.
Then perform autonomous technical implementation, regression testing, justified
tooling fixes and certification within the approved specification and mutation
envelope, following the successful C7-C9 execution model. Diagnose and repair
ordinary technical failures within that authority, re-run affected checks and
continue toward certification under section 4. Engineering autonomy does not
authorize changed game-design decisions, thresholds, rewards or scope.
If a contradiction requires design change, prepare evidence/options and return that
decision to owner/controller before dependent implementation. Continue independent
authorized engineering only where unaffected. Git/publication/cleanup authority
remains task-specific; Mode C is not blanket release or GitHub cleanup permission.

Completing a mode does not automatically advance to the next. Skill modernization
and tracking-policy approval are separate from gameplay-design approval. D0-Pre
ends with the proposal and preservation evidence, then stops for review.

## 14. Phase D Content-Mapping Principles

These guide recommendations, not item-by-item approvals:
- Compare new objects to functionally equivalent existing permission semantics
  before recommending new abilities, thresholds or UI entries. A new registry ID
  alone does not justify a new unlock. Golden Dandelion may inherit equivalent
  flower permissions; analyze unique interactions separately. No flower-specific
  unlock or registry mapping is approved by this example.
- Compare equipment per material. An individual spear variant may share its
  corresponding sword unlock, receive an independent threshold or use a reviewed
  mixed progression. Do not preselect an outcome or group all materials as an
  automatically approved spear category.
- Enumerate every included exact Minecraft registry ID in each group. Do not hide
  minor items as 'other objects automatically supported'. Separate verified current
  behavior from inference and recommendation; record evidence and unresolved gaps.
- Analyze actions separately where relevant: use, attack, equip, place, break,
  craft, consume, interact, ride and open containers. One item can have different
  permissions for different actions; a shared ID is not proof of shared behavior.
- Account for existing advancement progress and canonical team ownership. Extending
  an existing unlock must not require re-earning completed achievements unless that
  migration is specifically approved. Record effects on existing saves, criteria,
  completion/reward transitions and configuration-derived thresholds in the spec.

## 15. Skill Availability and Maintenance

This is an instruction-only skill: keep required `name`/`description` frontmatter
and useful project contracts self-contained. Resolve source paths relative to the
checkout. Evidence locations are provenance pointers, not mandatory universal
scratch/cache roots or authority automatically supplied by Git.
The D0-Pre observed skill is local/ignored by `/.agents/`; its presence in this
session's skill catalog does not mean a clean checkout contains it. For baseline-
dependent work in a fresh session, verify discovery and required evidence/inputs.
If unavailable, report the gap; do not claim certification from missing receipts.
Any installation, tracking or ignore-policy change requires owner-approved scope.
Propose milestone updates after accepted results; do not rewrite accepted evidence.
