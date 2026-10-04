# B8 — resumed static certification after R1 and R2

**PHASE_B_B8_STATIC_CERTIFIED**

`readyForB9 = true`. B9 was not started. This is static certification; B10 owns runtime world execution.

## Chronology and current authority

The initial B8 STOP, B8-R1 repair, post-R1 B8 STOP, and B8-R2 repair remain byte-identical historical records. This receipt certifies the accepted repaired state without replacing those records.

- Branch: `phase-b-bacap-26.2`.
- HEAD: `b261b02cd03b4aeae2835c63f982c9aa53c46eed`.
- Marker: `compat_26_2_r19`.
- Converter SHA256: `6ab0674902b4783c39c08dfd802cd0067d1436d973345834f092a001a4db9fec`.
- Runtime Minecraft RU SHA256: `8961ad9d573ab72f63969e4a808b19ce733bc9a60b7ab589fc44dfb6148e1a0a`.
- Compiled production root override digest: `cbc432be35d5525001872c430877541cfa1fcad6`.

A fresh compiled production probe verifies historical main raw/derived rejection, current main raw/derived acceptance, actual source identity, old r17/r18 rejection, and old pre-R2 root-digest rejection with current post-R2 acceptance. Native and historical converter witnesses, companion freshness/sync, Treasure Hunter and helper idempotence remain green. The two repaired Terralith helpers are present and codec-accepted within the full view.

## Exact official target and independent inventory

Official BACAP 1.21 — Chaos Takeover of Mayhem, Modrinth project `VoVJ47kN`, version `Y2zZ5eSs`, Minecraft 26.2.

SHA1: `14da3f07b5467e8b59ffc0253fd8212c938cd739`.

SHA256: `c71d1aa1a84dbe00a3f85a42144b46214c4669a3cccf07ff66631d28f16a99b2`.

**1332 − 72 no-display − 17 hidden − 1 excluded root = 1242 canonical.** Canonical namespaces: 1116 BlazeandCave + 126 Minecraft = 1242. No duplicate target IDs/archive paths.

Frozen/target set reconciliation: 1229 old; 1222 common; 7 old-only; 110 target-only. Common classes: 594 byte-identical + 4 semantically identical with byte differences + 624 semantically changed = 1222. Canonical reconciliation: **1152 − 7 + 97 = 1242**.

All seven removed/moved identities have old-source presence, target absence, new-target presence and current-consumer reconciliation. Active removed and malformed ID references are absent. No progress aliases were introduced.

Thirteen target-only noncanonical resources:

- `blazeandcave:monsters/jockey_jamboree`: HIDDEN
- `blazeandcave:monsters/the_wicked_witch_of_the_west`: HIDDEN
- `blazeandcave:technical/boss_music_check`: NO_DISPLAY
- `blazeandcave:technical/consume_apple`: NO_DISPLAY
- `blazeandcave:technical/consume_honey_bottle`: NO_DISPLAY
- `blazeandcave:technical/consume_poisonous_potato`: NO_DISPLAY
- `blazeandcave:technical/consume_spider_eye`: NO_DISPLAY
- `blazeandcave:technical/dive_bomb_check`: NO_DISPLAY
- `blazeandcave:technical/multishot_crossbow_check`: NO_DISPLAY
- `blazeandcave:technical/place_warped_button`: NO_DISPLAY
- `blazeandcave:technical/scenic_route_fail`: NO_DISPLAY
- `blazeandcave:technical/shoot_crossbow`: NO_DISPLAY
- `blazeandcave:technical/unwanted_passenger_fail`: NO_DISPLAY

## Real Minecraft 26.2 codecs and effective graphs

The exact target passes **1332/1332 Advancement.CODEC**. Each real run rejects the unknown-biome negative control. Registry contexts use real typed vanilla registries, selected pack-backed keys, and recursively bound tag memberships. No advancement is rewritten/excluded to satisfy the codec.

| Effective view | Advancements | Codec PASS | Structural failures |
|---|---:|---:|---:|
| amplifiedNether | 2894 | 2894/2894 | 0 |
| hardcore | 2894 | 2894/2894 | 0 |
| main | 2894 | 2894/2894 | 0 |
| nullscape | 2895 | 2895/2895 | 0 |
| terralith | 2922 | 2922/2922 | 0 |

Counts include 1562 vanilla recipe unlock advancements. All five views have zero missing parents, cycles, self parents, missing advancement reward functions, malformed wrapper arguments, unresolved active Search targets, or unexpected direct dangling calls.

Main wrapper scan: **1256 checked / 0 malformed**. Main advancement reward scan: **1294 checked / 0 missing**. Naughtylus, Benchmarking and unwanted-passenger shims retain their exact accepted contracts.

## Broader resource and scoreboard contracts

Function calls are classified as resolved, guarded optional resources, external BACAP fanpack hooks, or dynamic macro targets. Guard verification includes both explicit if/unless clauses and conditional selector filters. The B4-resolved Hardcore-only `loser_hurt` timer hook uses its native damage-score selector; its implementation exists in the Hardcore view. This static classification makes no runtime execution claim.

Direct predicates, recipes, loot tables/item modifiers, function tags and typed item/entity/block/biome/structure and other registry tags are checked against the effective resources or real registry context. Every effective predicate passes the real Minecraft loot-condition codec. No fake optional resources were added.

| View | Direct resource references | Typed registry references | Predicate codec checks | Unexplained failures |
|---|---:|---:|---:|---:|
| amplifiedNether | 1631 | 6066 | 58 | 0 |
| hardcore | 1631 | 6066 | 58 | 0 |
| main | 1631 | 6066 | 58 | 0 |
| nullscape | 1631 | 6160 | 58 | 0 |
| terralith | 1648 | 7060 | 59 | 0 |

Independent native scoreboard extraction reconciles exactly to **110 defined / 143 referenced** objectives and the B3 objective identities. All active SCORE trackers resolve. The six raw progression objectives remain distinct from BACAP weighted points.

## ATD integration and Search reconciliation

**86/86 trackers resolved: 80 KEEP + 6 UPDATED**, zero missing advancement/objective or invalid registry references. Seventy-four referenced native stat/item/entity/block constants are verified in real Minecraft registries. ON_A_RAIL and HALF_HEART_LIFE remain unchanged.

GUI: **17 tabs; 28 → 27 explicit ordered children; three parent maps**. Only Constellation was removed. All retained child IDs, parents and relative order contracts pass.

Seventeen root/milestone/companion wrappers delegate to the accepted native macros. Raw counts, first/team bookkeeping, points, messages, item rewards and guarded experience/trophy/fanpack behavior remain structurally preserved.

Broad Search extraction counts every lexical highlight occurrence. Active extraction excludes comments. The exact four inactive examples are:

- `bacap_rewards:msg/animal/fractal`, line 2.
- `bacap_rewards:msg/animal/humble_bundle`, line 2.
- `bacap_rewards:msg/redstone/vibe_check`, lines 3 and 5.

JUnit independently reads those source lines and verifies comment status.

| View | Broad lexical | Active executable | Comment-only delta | Active unresolved |
|---|---:|---:|---:|---:|
| amplifiedNether | 1261 | 1257 | 4 | 0 |
| hardcore | 1261 | 1257 | 4 | 0 |
| main | 1261 | 1257 | 4 | 0 |
| nullscape | 1262 | 1258 | 4 | 0 |
| terralith | 1287 | 1283 | 4 | 0 |

## Current message and companion successors

**1300 current messages** pass real component codec roundtrip/idempotence, hover/click/style metadata, current title/description bindings and Search destination checks. All four B8-R2 repairs pass without exemptions. The old 1202-message oracle remains untouched; this is its current Phase B successor.

All **14/14 merges** pass real Advancement.CODEC: 11 Terralith, 2 Amplified Nether, 1 Nullscape. Current bytes equal accepted B5.6 outputs and preserve the approved field ownership. **24/24 Terralith wrappers** resolve through the helper macro; exactly **34 count/point statements** are gated, with all other native lines unchanged. **6/6 Hardcore messages** bind to current `ZHHHw5wF` advancement contracts. Nullscape Search remains `nullscape:root`.

Current target inventory and message successor coverage are both **CURRENT_SUCCESSOR_CERTIFIED**. Historical Phase A/FINAL19 generators and tests were not modified or regenerated.

## Published successors and verification

Fixtures contain paths, IDs, hashes and compact structural facts; no third-party advancement/function bodies.

- `tools/phase_b/b8_bacap_static_certification.py` — SHA256 `18acf4be8234587916a72a2aa0a8b48bbacaccf796055b40b8a4cf76ff26936b`
- `src/test/java/com/diskree/achievetodo/certification/PhaseBStaticCertificationTest.java` — SHA256 `fcdcb7ea610eb48543d680536951fffe791554d6d2de3f50f14ac02de00285ea`
- `src/test/resources/phase_b_certification/bacap_1_21_codec_receipt.json` — SHA256 `eeddcb746eefe5175be3d1b2e9c30fe2cdfb99b79d2e8d290d00346aa9333a38`
- `src/test/resources/phase_b_certification/bacap_1_21_static_receipt.json` — SHA256 `204b79d5ced64f57a85f2ff8981d8216f8e8e6b3f17e2f99e2f5f14672c81bf3`
- `src/test/resources/phase_b_certification/bacap_1_21_target_inventory.json` — SHA256 `a8f76fc43819c875737c02be04885bbfb03b5ab69eab220358554a207d1a9fcf`

- Tool self-tests: **14/14 PASS**.
- `.\\gradlew.bat --no-daemon --offline compileTestJava -x generateBuildConfig`: **GREEN**.
- Filtered `PhaseBStaticCertificationTest`: **15 tests, 0 failures, 0 errors, 0 skipped**.
- `.\\gradlew.bat --no-daemon --offline processResources -x generateBuildConfig`: **GREEN / UP-TO-DATE**, appropriate because runtime bytes are unchanged.
- Four B8-R2 processed resources match source bytes; processed RU hash is exact; nine prior deleted resources remain absent.
- `git diff --check`: **PASS**.

The full Gradle regression suite was not run; B9 owns its reconciliation. No clean, cache deletion or dependency upgrade was performed.

## Product accounting and safety

Resumed B8 runtime delta: **0 ADD / 0 MODIFY / 0 DELETE**.

Unique cumulative runtime diff: **177 ADD / 69 MODIFY / 9 DELETE = 255 paths**.

Current ownership model: **249 exact B5 paths + 2 superseded B5 paths + 1 B6 RU path + 3 B8-R2 message paths = 255**. The converter remains under B8-R1/r19 authority; Potion root remains under B8-R2 authority. B12 must reconcile these post-B5 supersessions.

All prior evidence, historical tests/snapshots, source archives, runtime Java/resources/localization, pins/converter/downloader and balance/ability/progress contracts remain byte-identical. Nothing staged; no commit, push, tag or GitHub mutation. B9 was not started.

Detailed results and exact hashes are in `b8_static_certification_resumed.json`.

**PHASE_B_B8_STATIC_CERTIFIED**

`readyForB9 = true`
