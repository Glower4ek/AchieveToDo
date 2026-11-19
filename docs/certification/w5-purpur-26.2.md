# AchieveToDo Purpur 26.2: W5 runtime certification (0.2.1-rc.1)

Status (2026-11-19): **57/57 groups RUNTIME_PASS and independently CERTIFIED** for the certified composition below.
No blocker or major finding is open. There are 92 minor findings, all about evidence or documentation:
[findings register](../../tools/certification/w5/results/FINDINGS_REGISTER.md).

## What is certified

| Artifact | File | SHA-256 |
|---|---|---|
| Purpur plugin | `AchieveToDo-Purpur-26.2-0.2.1-rc.1.jar` | `ad0685eda3cb6f56a408a6409d27809e5e0f961dcd631759adea0564cac9d80b` |
| Fabric client mod | `achievetodo-mc26.2+0.2.1-rc.1.jar` | `46a0601e61e0b22752d4b814b622cc04874b5463843beddadccd9f7f10855081` |
| Server | `purpur-26.2-2620.jar` | `21003a8bf180855bf8b3cdd7212d6b6ff76f06de5dcba84740c3ed433ba6c34d` |
| Test input driver, test-only and not shipped | `atd-input-driver.jar`, source in `tools/certification/w5/driver-mod/` | `0c7dd9c6cb31985bf2d0e13afd04c3764a76c55d61050e5bec49631b0eb15bc5` |

Both jars are built from commit `5da53e08c76b79e3f6edf338b86d50e37550e585` (branch `certification-0.2.1`). See
[Reproducible build](#reproducible-build).

### Certified composition: native path only

The plugin contains two independent enforcement paths:

- **Native path (certified).** Java agents hook the vanilla methods themselves, at the same points as the Fabric
  original's mixins. It is active in gameplay mode `CERTIFICATION`.
- **Legacy Bukkit path (not certified).** Event listeners under `adapters/` and `restriction/`. It runs when the
  server starts **without** the flags below, in the default mode `diagnostic`. It has known gaps; for example,
  `BukkitRestrictionResolver` has no copper branch, so copper armor is never blocked on this path.

On 2026-11-19 the owner decided that only the native path is certified. Every run used this JVM composition:

```
-javaagent:plugins/achievetodo.jar
-Dachievetodo.gameplay.mode=certification
-Dachievetodo.bridge.real=true
-Dachievetodo.progression.foundation=true
-Dachievetodo.bridge.groupScope=true
-Dachievetodo.landmarks.exactPrototype=true
-Dachievetodo.bridge.spike=true
-Dachievetodo.bacap.directory=<world>/datapacks
-Dachievetodo.progression.world=<overworld UUID>   -Dachievetodo.progression.worldKey=minecraft:overworld
-Dachievetodo.bridge.world=<overworld UUID>        -Dachievetodo.bridge.worldKey=minecraft:overworld
-Dachievetodo.bridge.server=<server UUID>  -Dachievetodo.bridge.config=<config UUID>  -Dachievetodo.bridge.group=<group>
```

The plugin logs `ATD_GAMEPLAY_MODE=CERTIFICATION` at startup in all eight final runtimes. A server started any
other way is outside this certificate. Making an operator's default start safe is separate follow-up work:
either the native path becomes the default, or the plugin refuses to enable enforcement without these flags.
Any change to the plugin produces a new jar and needs a new certification.

## Method

There are 57 groups covering 153 abilities. Each group must meet E1–E6, and an independent reviewer must certify it.

| Criterion | Evidence |
|---|---|
| E1 source map | `results/E1/E1_<group>.json` |
| E2 native locked/allowed | Every case runs locked (score below threshold) and allowed (score at threshold). The verdict comes from raw server log lines, driver journal and client state. |
| E3 real client | The Fabric client logs in online (`online-mode=true`, authenticated UUID line) through a real Prism instance. An input driver presses keys and clicks the way a player does. |
| E4 coverage | Survival, OP, Creative, Spectator and off-hand where it applies. Thresholds run in NORMAL, EASY, HARD, CHAOS and Custom (`vision = 5`). |
| E5 reload | A real vanilla datapack `reload` raises the plugin config generation to 2, then every case runs again. |
| E6 replay | After the server stops, every verdict is recomputed from the raw logs (`campaign_replay.py`). Evidence files are hashed. |
| Review | Each group gets a package (`make_review_packages.py`) and a fresh reviewer agent with no build context. The reviewer re-hashes the sources, judges the verdicts from the raw log lines and writes an individual certificate. |

E1 details: each ability is mapped to the plugin source that guards it, with file SHA-256 and git blob.
Hand-verified enforcement sites (`campaign_e1.CURATED`) cover lookup tables, family resolvers and client mixins.
Each site is flagged active or inactive for the certified composition. Every config that ran lists the
thresholds.

Verdicts are never rewritten after the fact. Each row keeps the verdict that `judge()` gave it and that the
E6 replay reproduced. Verdict kinds:

- `PASS`
- `PASS_NO_OVERLAY`: denied with no effect, but the client lock message was not observed.
- `PASS_PARITY_ENTER_BLOCK`: see the observations below.
- `PASS_MASKED_BY_DEPENDENCY`: a fixture prerequisite was also locked. The lock is then proven in another config.
- `NA_NO_LOCKED_STATE`: the threshold is 0.
- `NA_OFFHAND_NOT_APPLICABLE`: melee uses the main hand.

## Result

There are 2579 final rows: PASS 2528, PASS_NO_OVERLAY 14, PASS_MASKED_BY_DEPENDENCY 13, PASS_PARITY_ENTER_BLOCK 12,
NA_NO_LOCKED_STATE 10 and NA_OFFHAND_NOT_APPLICABLE 2. There are no FAIL or INVALID rows.
The table is in [results/FINAL_TABLE.md](../../tools/certification/w5/results/FINAL_TABLE.md).

| Runtime | Config | Passes | Rows | E6 replay |
|---|---|---|---|---|
| campaign11 | NORMAL | pass1, modes (creative/op/spectator), offhand | 1235 | 1235/1235 |
| campaign12 | EASY | thresholds | 310 | 310/310 |
| campaign13 | HARD | thresholds | 310 | 310/310 |
| campaign14 | CHAOS | thresholds | 310 | 310/310 |
| campaign15 | Custom vision=5 | thresholds | 310 | 310/310 |
| campaign16 | CHAOS | landmark group, fixture fix r4 (supersedes campaign14 landmark rows) | 44 | 44/44 |
| campaign17 | NORMAL | reload pass after a real reload, generation 2 (supersedes campaign11 `reload` rows) | 310 | 310/310 |
| campaign18 | NORMAL | all equip routes, r6 (new rows) | 104 | 104/104 |

All runtimes ran the same plugin `ad0685ed…`. Raw logs, about 300 MB, stay outside git.
[results/EVIDENCE_INDEX.json](../../tools/certification/w5/results/EVIDENCE_INDEX.json) records the SHA-256 and size
of every receipt file for the 8 final runtimes and the 8 aborted attempts. Every aborted attempt was caused by a
login throttle or a fixture fix; none contributes rows.

Independent review: eight reviewer agents covered all 57 groups. Round 1 gave 55 CERTIFIED and 2 NOT_CERTIFIED,
with three major findings:

- vision: the E1 site was missing;
- copper armor: E1 cited an inactive resolver;
- equipment: only one route was runtime-proven.

E1 was then extended and campaign18 added the equip routes. Reviewer R9 resolved all three in round 2, so 57/57
are CERTIFIED. The certificates are in `results/certificates/`, and round 1 of the two groups is in
`certificates/round1/`.

## Product observations (parity with the Fabric original, not port defects)

1. **Locked End portal and gateway.** Touching one is denied, but the vanilla `enter_block` advancement is still
   granted (+1). The Fabric original guards only `setAsInsidePortal`. Verdict: `PASS_PARITY_ENTER_BLOCK`.
2. **CHAOS thresholds are not reproducible from the seed.** `ChaosProgressionGenerator` copies the base table with
   `Map.copyOf`, whose iteration order depends on a per-JVM salt. A new world with the same seed gets different
   thresholds after each JVM start. Within one world the generated `chaos_<seed>.toml` is saved and stays stable.
   Fixing this would change every seed's result relative to the original.
3. **The default mode runs the uncertified legacy path** (see above).

## Harness defects found and fixed during certification (product unchanged)

- r2/r3 fixture and oracle fixes. Examples: the creative item-use statistic, hidden BACAP advancements, fixture
  dependencies (`requires`), and End chunk loading.
- r4: a landmark chest room was placed inside a box of another locked landmark (trial chambers inside a mineshaft).
- r5: the reload pass sent `reload confirm`, which is Bukkit syntax that vanilla rejects, so E5 had not been proven.
- r6: equip routes added; the dispenser fixture needs `locked_margin=2` because the button earns BACAP `click`.
- Login: a cold `prismlauncher --launch` started the game before Prism refreshed the account token. The
  orchestrator now starts Prism first and waits for the refresh (`campaign_final.prism_ready`). Mojang
  session-server throttling (503/429) after many logins in one day remains an external limit.

Frozen harness versions: `HARNESS_R3.sha256`, `HARNESS_R4.sha256` and `HARNESS_R6.sha256`. Earlier file versions are in
`tools/certification/w5/harness/history/`.

## Reproducible build

From a clean clone of `5da53e0` (JDK 25.0.5, Gradle offline caches):

```
cd port/AchieveToDo-Purpur-26.2 && ./gradlew --offline jar        # -> ad0685ed…
./gradlew --offline --dependency-verification strict build -x test -x check   # -> 46a0601e…
```

Both hashes matched byte for byte on 2026-11-19. The client build needs one input that git cannot hold: the
Loom-generated `minecraft-merged-3b5d97e58c-26.2.jar`. Loom does not build it deterministically, so a fresh merge
fails dependency verification. `gradle/verification-metadata.xml` pins its reviewed baseline
(`49353b01…b14a`), and the build uses that exact cached file, which Gradle verifies against the pin.

## Limits

- The certificate covers the composition above and the three hashes. It does not cover PRODUCT mode, which needs
  a deployment certificate capsule, or the legacy path.
- Some routes are proven only from the source, with no runtime test. Examples: chestplate and leggings slots,
  drag-placement, and armadillo brushing. They are listed in the findings register.
- Minecraft 26.2, Purpur build 2620, and BACAP compat_26_2_r19 as installed in the test runtime.
