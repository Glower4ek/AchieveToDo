# Phase B B10 runtime smoke — STOP

Status: **PHASE_B_B10_STOP**. `readyForB11 = false`.

The fresh Minecraft 26.2 Main runtime loaded **3,046** advancements. The request
requires the full live advancement-manager count to match the B8 Main count of
**2,894**, permitting an exact reconciliation for additional test-mod advancements.
The actual difference is **152 always-enabled ATD production advancements**,
not test-mod advancements. No production defect is inferred from this difference.
The count contract needs a scoped resolution before deep runtime certification.

## Exact count reconciliation

An independent reconstruction from the live production-converted BACAP ZIP,
Minecraft 26.2 resources, and the selected builtin overlays contains 2,894 IDs.
Every one exists in the live manager. The other 152 IDs exactly match all
advancement JSON resources under `src/main/generated/data/achievetodo/advancement/`.
There are zero unexplained additional IDs and zero missing B8-core IDs.
The JSON evidence records every additional ID, generated source path, and SHA256.

B8's accepted effective layer list starts with vanilla and then the external and
builtin BACAP packs. It does not include the always-enabled `achievetodo` base
mod pack. Historical B8 evidence remains unchanged. These production ability
resources were neither excluded nor deleted to force the requested count.

## Main harness fidelity

The authoritative confirmation used a fresh world at
`build/tmp/phase_b_b10/MAIN_FRESH/runtime/world` and this exact selected order:

1. `vanilla`
2. `achievetodo`
3. `fabric-convention-tags-v2`
4. `fabric-gametest-api-v1`
5. `file/bacap.zip`
6. `achievetodo:bacap_override`
7. `achievetodo:bacap_rewards_item`
8. `achievetodo:bacap_rewards_experience`
9. `achievetodo:bacap_rewards_trophy`
10. `achievetodo:bacap_cooperative_mode`

Item, experience, trophy, and cooperative options were explicitly selected.
No Hardcore, Terralith, Amplified Nether, or Nullscape overlay was selected.
The requested selection exactly equals the actual live repository selection.

Compiled production `ExternalPackCompatibility.copyForWorld` created the world
copy from the pinned current BACAP archive:

- Source SHA1: `14da3f07b5467e8b59ffc0253fd8212c938cd739`.
- Source SHA256: `c71d1aa1a84dbe00a3f85a42144b46214c4669a3cccf07ff66631d28f16a99b2`.
- Marker: `compat_26_2_r19`, recording the same actual source SHA1.
- Root digest: `cbc432be35d5525001872c430877541cfa1fcad6`.
- Production world-copy currentness: true.

Only the selector
`achievetodo-test:phase_bruntime_smoke_production_smoke` ran: one matched test,
one executed test, exit code 1 at the live-count assertion. Fabric Loader was
0.19.5 and Fabric API 0.161.0+26.2. The server started and loaded its resources.
The logs contain no BACAP command/advancement parse failure. Host performance
counter diagnostics, initial absent server settings/EULA files, development
warnings, and the count assertion are separately classified in the JSON.

An earlier diagnostic was excluded: its receipt omitted automatically inserted
base packs and its world path was not yet isolated. Both harness issues were
corrected before the fresh confirmation above.

## Historical harness preservation

New B10-only mixins are guarded by `achievetodo.b10.mode`. A minimal additive
plugin registration leaves existing mixins enabled when that property is absent.
Under B10, it isolates the historical frozen-BACAP installer and location probes;
a new guarded initializer mixin prevents Phase A evidence resets and fabricated
scoreboard setup. Historical GameTest Java bodies remain byte-identical.
The existing entrypoint/mixin lists retain their entries and relative ordering;
the B10 GameTest and mixin configuration are appended. `build.gradle` is unchanged.

## Execution boundary and validation

Reward completion, raw/first/duplicate scores, ability thresholds, emitted Search
components, the four R2 message witnesses, and all four companion runtime runs
were **not executed** after the count gate stopped the stage. No client Search UI
claim is made. Main-only pack fidelity is proven; five-composition certification
is not claimed.

- Final `compileGametestJava processGametestResources`: GREEN.
- Unchanged B9 regression subset: **23/23 PASS**, zero failures/errors/skips.
- `git diff --check`: PASS.
- All 1,923 runtime source files and all prior evidence/test hashes: unchanged.
- Runtime transaction delta: **0 ADD / 0 MODIFY / 0 DELETE**.
- Unique cumulative runtime diff: **177 ADD / 69 MODIFY / 9 DELETE**, 255 paths.
- No staging, commit, push, tag, GitHub mutation, or B11/B12/B13 execution.

Evidence: `reference/phase_b/b10_runtime_smoke.json`.
Fresh diagnostic logs and receipts remain under ignored `build/tmp/phase_b_b10/`.

The required next resolution is an explicit current live-count model accounting
for the exact ATD production base-pack set, or authorization to report the B8
core count separately from the full live manager. B10 has not been completed.
