# B8 static certification — STOP

The exact pinned BACAP 1.21 target passes all **1,332 / 1,332** real Minecraft 26.2 Advancement.CODEC checks. B8 stops because the effective retained Terralith companion adds two technical helpers with rejected legacy player-predicate structures.

| Effective codec view | Checked | Passed | Failed |
|---|---:|---:|---:|
| Pinned target | 1332 | 1332 | 0 |
| Main including vanilla recipes | 2894 | 2894 | 0 |
| Hardcore including vanilla recipes | 2894 | 2894 | 0 |
| Terralith including vanilla recipes | 2922 | 2920 | 2 |

Rejected IDs:

- `blazeandcave:technical/biome_branch1_end`
- `blazeandcave:technical/biome_branch2_end`

Both effective resources come from converted `BACAP_TERRALITH`. Their `kilometre_walk` criterion has the rejected structural pointer `/criteria/kilometre_walk/conditions/player/player`. The real codec reports unknown registry key `minecraft:player` in `minecraft:entity_sub_predicate_type`. Neither helper ID exists in the native main target; the entire main set passes under the same registry context. Their r17 and r18 converted companion payload bytes are identical. Neither path belongs to the fourteen authorized B5.6 merges. No repair, new override, converter change, or registry exemption was applied.

The registry harness uses real vanilla typed registries and selected pack-backed keys/tags; nested tags are actually bound. An unknown-biome negative control is rejected. The first temporary context used a construction-only named-tag holder; that harness limitation was corrected before the authoritative full runs. These two final rejections are independent of that limitation.

## Independently established facts

- Target: version `1.21`, Modrinth `Y2zZ5eSs`; SHA256 `c71d1aa1a84dbe00a3f85a42144b46214c4669a3cccf07ff66631d28f16a99b2`.
- Inventory: 1332 all − 72 without display − 17 hidden − 1 excluded root = **1242 canonical**, split 1116 BlazeandCave / 126 Minecraft. No duplicate IDs/archive paths.
- Old/target: 1222 common, 7 old-only, 110 target-only; common classes 594 byte identical, 4 semantic identical with byte differences, 624 semantic changed. Canonical: **1152 − 7 + 97 = 1242**. Thirteen target-only resources are noncanonical and retained in the ignored inventory with reasons/flags.
- Main wrapper literal arguments: 1256 checked, zero malformed. Main advancement reward functions: 1294 checked, zero missing.
- All five independently layered parent/function/reward/Search graphs have zero missing parents, cycles, malformed wrapper arguments, unresolved active Search IDs, or unexpected literal direct dangling calls. Guarded sparse optional resources and fanpack extension hooks remain separately classified.
- Active Search counts are 1257 Main/Hardcore/Amplified Nether, 1283 Terralith, 1258 Nullscape. Earlier 1261/1287/1262 counts include four commented examples in Fractal, Humble Bundle, and Vibe Check; no active reference was lost.
- Effective advancement totals include 1562 vanilla recipe unlock helpers: 2894 Main/Hardcore/Amplified Nether, 2922 Terralith, 2895 Nullscape. Main BACAP scope remains 1332 and canonical 1242.

## Incomplete gates

The remaining tracker/GUI/message/root-wrapper/merge parity and resource-registry certification, Amplified Nether/Nullscape full codecs, and JUnit/fixture publication are **not complete**. No success receipt or green certification is claimed. Eight new tool self-tests pass; the real-codec harness compiles. `compileTestJava` and filtered JUnit were not run after the mandatory codec STOP. The full Gradle suite belongs to B9 and was not run.

Safety/resource-processing results and exact hashes are recorded in `b8_static_certification.json`. No runtime product bytes or historical evidence changed. The unique runtime diff remains **177 ADD / 66 MODIFY / 9 DELETE**. Marker remains `compat_26_2_r18`; 250 B5 hashes remain exact and the converter hash remains the accepted B7-R1 supersession. B9 was not started.

**PHASE_B_B8_STOP**

`readyForB9 = false`
