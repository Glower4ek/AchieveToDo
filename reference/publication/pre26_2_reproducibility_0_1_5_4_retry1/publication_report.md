# PRE-26.2 publication reproducibility retry1

**READY_FOR_SELECTIVE_STAGE**

The previous valid STOP was caused by `PhaseATerminalReconciliationTest.java:61` reading the intentionally excluded `.agents/skills/achievetodo-phase-a/SKILL.md`. The test now consumes the existing durable terminal reconciliation JSON; no duplicate fixture or whole manual was added.

## Contract and provenance

- Durable contract: `reference/phase_a_planning/phase_a_terminal_reconciliation_20260930.json`.
- Historical assertions: runtimePartial 0, totalCertified 1133, unfinished 19; fresh frontier/canonical hashes and full canonical summary match the sealed checkpoint.
- Source SKILL SHA256, verified once and retained only as provenance: `22448f14632d290cee8b0639c88d51f1ee992cafd504231a57cb77384aa9a594`.
- Frontier SHA256: `2cb3d3f835a1d8f1eda97ec782725a72936f346dd8cfeed75da61fc4d9f36662`.
- Canonical SHA256: `439f2aa1c577ffbee275525765ac9229bf5633f0f5e1da498c0adc5702bd2810`.
- Exact test change: `terminal-test.diff`; one-time proof: `terminal-contract-provenance.json`; focused result: 2/2 GREEN.
- Active source/test/publication validation tooling has zero executable `.agents/` references. Historical/provenance labels are retained. The current route does not invoke the older workstation-specific FINAL19 launcher.

## Fresh snapshot gates

- NEW snapshot: `build/tmp/publication_probe_retry1/snapshot`; initial 3254 publication files, all copied with exact SHA256 verification. Initially no Git metadata, agent toolkit, Gradle caches, build output, world saves, dist, local logs or final release JAR.
- SOURCE_COMPLETE=YES; NETWORK_BOOTSTRAP_VERIFIED=YES; CACHE_ASSISTED_EXECUTION=NO. The empty home downloaded the pinned Gradle distribution and resolved all required dependencies; all three compile gates passed.
- Baseline: 391 tests, exactly four historical failures, zero errors/skips. Each suite/test name, exception type, message and full stack trace matches immutable retry1 evidence. Terminal regression: 2/2 GREEN.
- Accepted route: 103/103 GREEN, zero failures/errors/skips, with the wired `preparePublicationRuntime` dependency.
- Runtime BACAP: frozen source verified, exact temporary copy verified, unchanged converter invoked, compat_26_2_r16 verified, all 1229 converted definition hashes exact, frozen source reverified.
- Fresh preservation: 1152 unique product advancements, certified 1152, uncertified 0, seven completed FINAL19 families, no remaining targets; FINAL19 CLOSED. Historical Phase A remains 1133/1152. All 442 FINAL19 sealed files reconcile.
- Fresh normal build: `./gradlew.bat --no-daemon build -x test -x runGameTest` (the durable script first reconstructs only the two accepted ignored HashCache records from the 303 committed resources).
- JAR: 1546595 bytes, SHA256 `0fc29fd0fbe1d874ca037ba2bd932f4e667593d95a1f2d3bb42936765710978b`; raw bytes equal, zero added entries, zero removed entries, zero changed entry contents.
- The accepted original JAR was read only for the final comparison, never as a build/validation input. No required input reached back into the original checkout.
- `probe-inputs.json` seals the exact executed inputs. Post-gate documentation and this newly produced evidence are publication additions; they do not change executable inputs.

## Publication set and exclusions

`final-commit-set.json` enumerates every exact tracked delta and untracked path with categories. It inherits all unchanged tracked product/build/test/documentation/preservation files and the intentional deletion. It includes the historical terminal contract already selected under phase_a_planning, all eight sealed binary results, 303 generated gameplay inputs, both durable fixture routes, current tools/publication and this successful retry evidence.

Exclude `.agents/`, `skills-lock.json`, `build/`, `.gradle/`, `.gradle-user-home/`, `logs/`, tmp/decompile/audit scratch, local worlds, `dist/`, unused `src/data/resources/fabric.mod.json`, localization cache copies, generated Java/datagen caches and release JARs. Exact ignore patterns and the 303-resource exception manifest are listed in the inventory; no blanket archive/XML/log ignore rules were introduced.

The final JAR stays outside Git and can later be attached to a GitHub Release. No staging is performed.

## Safety

Previous post-clean STOP, post-clean retry1 and publication STOP records are unchanged. All protected files except the authorized terminal test and current reproducibility document are byte-identical. Production source, metadata, language strings, dependencies, markers, frozen archives and release identity are unchanged.

Branch `26.2-port`; HEAD `a5e1b49539a39f01bb5db13d44d420e653dce225`; index SHA256 `bd0c1882b97105b93ec41c193496cc5587131df9792f28c7bc89e08d1c33a8df`; staged empty; remotes unchanged. No Git mutation, commit, tag, push or Phase B.

READY_FOR_SELECTIVE_STAGE

PRE_26_2_GIT_PUBLICATION_READY
