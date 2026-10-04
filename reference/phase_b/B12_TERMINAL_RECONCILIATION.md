# B12 terminal Phase B reconciliation — STOP

**PHASE_B_B12_STOP — readyForB13 = false. Nothing is staged; B13 has not started.**

The read-only source, content, product, localization, certification and path ledgers reconcile. The required non-test assembly gate does not pass: `assemble` reaches `:jar` and the existing `build.gradle:359-360` guard refuses to replace the published-version smoke candidate: “Smoke candidate 0.1.5.4 already exists; increment the final modVersion component before producing a changed JAR.” No version change, guard bypass, artifact move/delete, or retry under a different identity was performed. The milestone manifest is complete as an inventory but explicitly **blocked for staging**.

## Source and advancement reconciliation

Frozen BACAP 1.18.1 SHA1 `45b8bb0076bbf5b92fde7dc9590c6686937abbc0`, SHA256 `8c72314535c5df7b4416bf0f38310371ec8aec537fde1445bdc820a3c9aada70`, remains exact. Official BACAP 1.21 “Chaos Takeover of Mayhem,” Minecraft 26.2, Modrinth VoVJ47kN/Y2zZ5eSs, has SHA1 `14da3f07b5467e8b59ffc0253fd8212c938cd739` and SHA256 `c71d1aa1a84dbe00a3f85a42144b46214c4669a3cccf07ff66631d28f16a99b2`. Local archives were checked; no fetch or substitution occurred.

All advancements: 1229 → 1332; 1222 common, seven old-only, 110 new. Common classes: 594 byte-identical, four semantic-identical with different bytes, 624 changed. Canonical: 1152 − 7 + 97 = 1242 (+90); 13 new noncanonical resources remain accounted. All seven ID mappings remain current in all effective views, with no progress migration or Adventure/Weaponry spear-fishing conflation.

## Product authority and marker chronology

B5 checkpoint: 177 ADD / 65 MODIFY / 9 DELETE, 251 paths. Current: **177 ADD / 69 MODIFY / 9 DELETE, 255 paths**. The model is 249 exact B5 paths + two superseded B5 paths + B6 RU + three B8-R2 message modifications. Converter: B5.2/r17 → B7-R1/r18 → B8-R1/r19. Potion root: B5.4 → B8-R2. Immutable checkpoints are not rewritten.

Marker: published r16 → r17 → r18 → r19. B8-R2 needs no marker bump: rootOverrideSha1 is `cbc432be35d5525001872c430877541cfa1fcad6`. The writer still records the actual source SHA1. Direct baseline Git/blob/file comparison agrees with the 255-path ledger. Separately, 191 unchanged Git paths have only the established CRLF checkout transport difference; their clean-filter blob identities match HEAD. Three ignored generated/cache paths are excluded. No file was normalized or rewritten.

## Localization and certification

RU dictionary: 3482 unchanged keys. Current requirements: 4301; providers 533 ATD / 3432 overlay / 325 vanilla / 11 neutral. Effective coverage: Main, Hardcore and Amplified Nether 3699/3699; Terralith 3753/3753; Nullscape 3702/3702. Missing = 0. Placeholders: 4290, mismatches = 0. Provenance: 3186 / 211 / 21 / 64, ownership errors = 0. The historical 4304-consumer snapshot remains valid historical evidence; the current difference is exactly the three retired B8-R2 descriptions. They remain in the dictionary. Official permission and available BSD attribution are preserved; NOTICE_SOURCE_INCOMPLETE_UPSTREAM remains disclosed.

B8 static authority retains 1332/1332 real target codecs, 1256/1256 valid wrappers, 1294/1294 reward references, 86 trackers (80 KEEP + 6 UPDATED), 17 tabs, 27 explicit order entries, 1300 valid messages, 14 merges, 24 Terralith wrappers and six Hardcore messages. Search failures = 0.

Fresh B7/B8/B9/B11 tool suites: 8/8, 14/14, 8/8, 13/13. They ran unchanged with output paths redirected to B12 scratch. Fresh filtered JUnit: B8 15/15 and B9 23/23. Full Gradle suite: **429 tests / 11 failures / 0 errors / 0 skips**. The 11 names and complete failure-message hashes exactly match B9: four accepted baseline + seven immutable historical-oracle failures, zero new/current regressions. Compile/resource/GameTest build surface is GREEN. `assemble` is STOP; the guard was preserved.

## Runtime and manual review authorities

B10 exact live sets remain hash-bound: Main 3046, Hardcore 3046, Terralith 3074, Amplified Nether 3046, Nullscape 3047. Main PlayerAdvancements completion: raw 6→7, first 6→7, weighted 30→32, XP 1750→1800; duplicate award has no duplicate effect. EAT_SALMON unlocks 1→2 and relocks 2→1, preserving demystified. Companion witnesses remain green. Unexplained ATD/BACAP errors = 0. Client visual Search UI is not claimed. No GameTests were rerun.

B11: 734/734 manually reviewed, 600 PASS, 131 PASS_WITH_NOTE, three upstream anomalies, zero product defects. Every required coverage matrix is complete. Upstream-native anomalies preserved: whats_up_doc (Grumm filter outside conditions), riddle_first_line (misplaced player-root gate), minecraft:adventure/bullseye (arrow restriction outside predicate). No speculative fix exists.

## Durable inventory and preservation

345 durable paths: 255 runtime, one license, eight current tests/fixtures, nine GameTest artifacts, seven Phase B tools and 65 Phase B evidence files. Manifest: **265 ADD / 71 MODIFY / 9 DELETE**. No build, cache, ZIP, runtime log, ignored probe, attachment or temporary review batch is included. Every pre-existing non-runtime executable/license path is tied to accepted hash evidence. The five B12 additions are the only persistent transaction changes.

All 3595 protected pre-existing paths retain their bytes or recorded absence. Phase A, FINAL19, publication fixtures, prior Phase B evidence, production, tests, GameTests, existing tools, build configuration and licenses remain unchanged. Local Git refs, published 26.2-port and v0.1.5.4 remain exact. The published JAR retains SHA256 `0fc29fd0fbe1d874ca037ba2bd932f4e667593d95a1f2d3bb42936765710978b`. No public release operation occurred. Runtime delta during B12 is 0/0/0.

## Receipts and handoff

- [Terminal reconciliation](b12_terminal_reconciliation.json)
- [255-path product ledger](b12_product_ledger.json)
- [Blocked milestone inventory](b12_milestone_manifest.json)
- [Read-only tool](../../tools/phase_b/b12_terminal_reconciliation.py)

**readyForB13 = false.** The unresolved gate is non-test assembly under the preserved published-version guard. Resolving the assembly contract requires a separate authorized decision. This STOP does not authorize version changes, staging, committing or publication.
