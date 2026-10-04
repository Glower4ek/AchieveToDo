# Phase B B10 runtime smoke — resumed certification

Status: **PHASE_B_B10_RUNTIME_CERTIFIED**  
readyForB11: **true**. B11 has not started.

The original count-contract STOP, C1 exact-set correction, and D1 Main deep certification remain immutable. D1 is carried forward by SHA256 `24f6efc356f44d7fa1923af92b12767f47c5825c91276a2049afc6a0905223af`.

| Composition | B8 core | ATD production | Live | Exact set |
|---|---:|---:|---:|---|
| MAIN (D1) | 2894 | 152 | 3046 | PASS |
| HARDCORE | 2894 | 152 | 3046 | PASS |
| TERRALITH | 2922 | 152 | 3074 | PASS |
| AMPLIFIED_NETHER | 2894 | 152 | 3046 | PASS |
| NULLSCAPE | 2895 | 152 | 3047 | PASS |

All sets are exact unions, with zero missing core/ATD IDs, unexpected IDs, or duplicates. D2 ran four isolated processes in the required order, one selected GameTest each. Each external pack was installed through compiled production copyForWorld; transformed copies carry r19 actual-source markers and the current root digest, while untransformed worldgen packs retain exact source bytes.

Hardcore: six of six messages executed and emitted real SystemChat components with live Search targets. Terralith: both repaired technical helpers, 11 merges, and 24 wrappers loaded; representative merge/wrapper paths executed; the enabled count/point macro gate was reachable and restored. Amplified Nether: both merges loaded and the representative reward/message path passed. Nullscape: its merge loaded and the runtime Search command targeted nullscape:root.

The first Hardcore diagnostic attempt exposed Fabric's GameTest-only pack-order rewrite before companion execution. After explicit user authorization, the B10 bootstrap mixin restores the production-derived order before resource loading and requires the selected pack set to remain exact. A new fresh Hardcore process and all subsequent modes passed. The original diagnostic log/receipt remains in ignored storage. The D1 helper and C1/D1 GameTest bodies are unchanged.

Main D1 retains its real joined-player reward witness: a_chiptune_relic raw 6→7, first 6→7, weighted 30→32, XP 1750→1800; duplicate completion has no repeated effect. EAT_SALMON unlocks at raw 1→2, relocks at 2→1, and preserves demystified state. Main Search and all four B8-R2 packet bindings remain hash-bound PASS.

Search claims cover server-emitted Component click bindings and live target existence. Client visual Search centering/highlighting is not claimed.

CompileGametestJava/processGametestResources: GREEN. Fresh filtered B9 successor: 23/23 PASS, zero failures/errors/skips. git diff --check: PASS. Certified process logs have zero resource parse failures, required-function/advancement failures, or unexplained ATD/BACAP errors; known host Perflib and fresh server.properties diagnostics are classified individually in JSON.

All 1,923 src/main file bytes, 271 historical test files, and 56 prior Phase B evidence files remain unchanged. Runtime delta: 0 ADD / 0 MODIFY / 0 DELETE. Unique cumulative runtime diff remains 177 ADD / 69 MODIFY / 9 DELETE (255 paths). Nothing staged; no commit, push, tag, or GitHub mutation. No B11 work.

Detailed hash-bound records: b10_d2_companion_runtime.json and b10_runtime_smoke_resumed.json.
