# AchieveToDo Purpur 26.2 — independent group review (0.3.0 release, rc.3)

You are an independent reviewer. You did not build this evidence and you must not trust its conclusions:
check them against the raw material. You review the groups assigned to you, one certificate per group.

## What is certified

The Purpur plugin `AchieveToDo-Purpur-26.2-0.3.0.jar` (SHA-256 `d3019bd333591c8f57645e093aea78befb6700d795ccbae25edf7a4338ffc43a`, built from commit `5f2695ca42f92f3acf216432b1ed251b9a137522`)
ports the Fabric mod AchieveToDo: each player ability (open a chest, eat salmon, sprint, enter the End, interact inside a
village …) is **locked** until the player's advancement count reaches the ability's threshold in the world's progression
config (EASY / NORMAL / HARD / CHAOS / Custom). While locked, the action must be denied with no gameplay effect; once the
count reaches the threshold, the action must work normally. The test client is a real Fabric client (mod
`achievetodo-mc26.2+0.3.0.jar`, installed as `achievetodo.jar`, SHA-256 `2c03d9fa9e26c1c53691d720671107e8569af63092a5c2a2e9e9b1c32644c649`) logged in online.

## Inputs (read only)

- Package per group: `E:\Vibecode\AchieveToDo\certification\w5-campaign\review-rc3-pkg\<group>\`
  - `MANIFEST.json`: hashes, runtimes, E3/E5/E6 evidence, verdict glossary, superseded rows
  - `E1.json`: static map ability → guard sites in the plugin source (file, lines, SHA-256) and thresholds per config
  - `rows.jsonl`: every final result row of the group (one per runtime × pass tag × ability × case × phase)
  - `replay.jsonl`: E6 replay of each row (verdict recomputed from raw logs after the server stopped)
  - `excerpts.txt`: per row, the raw server log lines its verdict was judged from (long state lines compacted)
- Plugin source: `E:\Vibecode\AchieveToDo\repo\port\AchieveToDo-Purpur-26.2\src\main\java\`
- Raw run folders if you need more: `E:\Vibecode\AchieveToDo\certification\w5-campaign\campaign4N\receipts\` (campaign41-46)
  (output.log can be ~100 MB: search it, never read it whole)
- Harness that produced the rows (for understanding only): `campaign.py` (`judge`, `evaluate`), `campaign_cases.py`.

Do not modify, move or delete anything. The only files you may create are your certificates (see Output).

## Row anatomy

`phase`: `locked` (score = threshold − margin), `allowed` (score = threshold), `creative`, `op`, `spectator`,
`offhand_locked`, `offhand_allowed`. `tag`: `pass1`, `modes`, `offhand`, `reload` (NORMAL runtime), `thresholds-<config>`
(EASY, HARD, CHAOS, Custom runtimes). `counts` = advancement count before/after the action.
`why.observed` = which effect oracles fired; `why.overlay` = the client showed the lock message; `why.leak` = advancement
criteria gained during the action. `requires` = other abilities the fixture itself needs, with their thresholds.
In `excerpts.txt`, `ATD_W2_INPUT_EVENT` / `ATD_EVENT` lines show the plugin's decision (`useBlock=DENY`, `cancelled=true` …),
`ATD_STAGE2A_STATE` shows count/mode/op/generation and the tested ability's lock state, `ATD_PACKET_IN` the client packet.

## Checklist per group

Mark each criterion PASS or FAIL, and say exactly what you checked.

- **E1, source map.** Every ability of the group has at least one direct guard site or a family resolver in `E1.json`.
  For **every** source file cited, compute its current SHA-256 and compare it with E1 (for example `python -c` with hashlib,
  or `certutil -hashfile <file> SHA256`). Open at least one guard site per ability and confirm that it really checks that
  ability. Every config that ran (normal, easy, hard, chaos, custom_vision) lists a threshold for each ability.
- **E2, native locked/allowed.** For every ability and case, at least one locked row proves the lock (`PASS`,
  `PASS_NO_OVERLAY` or `PASS_PARITY_ENTER_BLOCK`; `PASS_MASKED_BY_DEPENDENCY` does not count). Its excerpt must show the
  denial and an unchanged count, and the allowed rows must show the effect. Read the excerpts of **every non-`PASS` row**
  and of at least 3 `PASS` rows per ability across different runtimes, and judge each verdict yourself from the log lines.
  A verdict you cannot reproduce from the excerpt is a finding.
- **E3, real client.** Every runtime that contributed rows has `onlineMode=true`, an authenticated UUID line and a join line
  in `MANIFEST.runtimes`.
- **E4, coverage.**
  - Rows exist for `creative`, `op` and `spectator` (tag `modes`), and for `offhand_*` where the action has an off-hand
    route.
  - Rows exist for the thresholds of `normal`, `easy`, `hard`, `chaos` and `custom_vision`.
  - Explain every `NA_*` row: an NA must follow from a real threshold of 0 or from a route that has no off-hand variant.
- **E5, reload.**
  - The rows tagged `reload` come from `campaign41` (and campaign46 for its one superseded reload row), after its pass1, modes and offhand passes.
  - `MANIFEST.e5Reload.evidence.generationAfter` is 2.
  - The `ATD_STAGE2A_STATE` lines of those rows in `excerpts.txt` show `"generation": 2`.
  - Each reload verdict agrees with that ability's `pass1` verdict.
- **E6, hashes and replay.**
  - Every row in `replay.jsonl` is `REPRODUCED`, and every runtime's replay has `reproducedAll=true`.
  - The plugin SHA-256 is identical in every runtime and equals the value above. The client mod SHA-256 equals the value
    above.
  - Recompute the SHA-256 of `campaign-results.jsonl` of at least one runtime and compare it with that runtime's
    `evidenceHashes`.
- **ReleaseLaunch.** The certified composition is the operator launch from
  `port/AchieveToDo-Purpur-26.2/STARTING_THE_SERVER.md`. The server command holds `-javaagent:plugins/achievetodo.jar`
  plus the observer trace agent. The only `-Dachievetodo.*` option allowed is the documented first-start option
  `-Dachievetodo.progression.initialConfig=<preset>`, and only for a world built as a fresh install (no
  `plugins/AchieveToDo/progression/world-group.properties` or `world-selection.properties` before the start). This is
  how EASY, HARD and CHAOS (campaign42-44) are made; NORMAL (campaign41, campaign46) and Custom (campaign45) start an existing install
  with no option. See `runtime_args.json` and `receipts/launch.json` in each runtime. Confirm for every contributing
  runtime:
  - `ATD_LAUNCH_PROFILE=release` and `ATD_GAMEPLAY_MODE=RELEASE` and `ATD_PRODUCT_GAMEPLAY_READY=true` are logged before the first case;
  - any `freshConfig` came from the plugin itself, i.e. it is listed in the logged `derived=[...]`, not passed by hand.
  All rows come from campaign41-45 under one harness (`HARNESS_RC3.sha256`), with one exception: `campaign46`
  (`HARNESS_RC3B.sha256`, the NORMAL install of campaign41) reruns 4 cases of campaign41 that failed for harness reasons;
  its rows replace the campaign41 rows with the same tag, ability, case and phase (`MANIFEST.superseded`):
  off-hand shears on an own wolf and on cobweb (vanilla handles the main hand first / mining uses the main hand only:
  now `NA_OFFHAND_NOT_APPLICABLE`), a creative player mining cobweb with a sword (vanilla forbids it:
  `NA_VANILLA_NO_CREATIVE_ROUTE`), and the reload-pass `use_water_bucket` locked row (rc.3 acted before the new count
  reached the client; rc.3b waits for `ATD_BRIDGE DELTA_SENT ... count=<score>`). Judge whether each replacement is
  justified from the campaign41 and campaign46 log lines, not from this text.
- **Reduced presets (new in 0.3.0).** EASY, HARD, CHAOS and Custom (campaign42-45) ran `--sample one-per-group`: only the
  first case of every group, locked and allowed. The full case set ran on NORMAL (campaign41). This is by design: check
  that each group has its preset rows, not that every case does.
- **Oracle rules** (from 0.2.1; rc.3 adds `Server.sync` marker waits instead of fixed sleeps, and `overlay_text`: a shelf
  row passes only if the denial message names the shelf (`shelf`/`полк`), else `FAIL_WRONG_DENIAL_MESSAGE`) (in `campaign.py` `judge` and `campaign_cases.py`; check that they
  hold in the rows):
  - `expect_hit`: an aimed case whose client aimed at nothing (`hit` = miss in the `pre` state) is
    `INVALID_FIXTURE_AIM`. The orchestrator reruns such abilities once in the same pass, so both rows stay in
    `campaign-results.jsonl`, and the later one counts.
  - `server_reach`: vehicles, all villagers and the wandering trader must show a traced
    `Player.interactOn` return during the action (`returns`), otherwise `INVALID_NOT_REACHED_SERVER`. This proves the request
    reached the server guard.
  - Server-side effect oracles: for vehicles, the probe `execute as Glower4ek on vehicle if entity @s[type=...]`. For villager
    trades, the traced decision: `Success[swingSource=CLIENT]` when the trade opens, `swingSource=NONE` when the guard consumes
    the click. The wandering trader is denied when its menu opens, so it keeps the screen oracle.
  - `put_in_bundle` has two cases: bundle on the cursor onto the item, and item on the cursor onto the bundle (server
    probe of the bundle contents).

Changed product behaviour in 0.3.0 (both issues accepted in 0.2.1 are fixed, so they are now requirements):
- (a) touching a locked nether portal (outside the Nether), End portal or end gateway is denied **and grants no**
  enter_block advancement: a locked portal row with a +1 count, or a leak of that criterion, is a failure;
- (b) CHAOS thresholds depend only on the seed: E1 lists a single `chaos_20261116` config.
- Known issue 4 (`repo/docs/known-issues.md`): landmark denials that return SUCCESS were not audited for an
  advancement leak (`player_interacted_with_entity`). Report evidence of such a leak if you see one.

## Output

For each group write `E:\Vibecode\AchieveToDo\certification\w5-campaign\review-certs-rc3\<group>_INDIVIDUAL_CERTIFICATE.json`:

```json
{
  "group": "<group>",
  "reviewer": "<your agent label>",
  "decision": "CERTIFIED | NOT_CERTIFIED",
  "criteria": {
    "E1": {"result": "PASS|FAIL", "checked": "what you verified, with counts and hashes"},
    "E2": {"result": "...", "checked": "...", "rowsRead": ["campaignNN tag ability phase", "..."]},
    "E3": {"result": "...", "checked": "..."},
    "E4": {"result": "...", "checked": "..."},
    "E5": {"result": "...", "checked": "..."},
    "E6": {"result": "...", "checked": "..."},
    "ReleaseLaunch": {"result": "...", "checked": "..."}
  },
  "superseded": "assessment",
  "findings": [{"severity": "blocker|major|minor", "criterion": "E2", "evidence": "file:line or row", "description": "..."}]
}
```

`CERTIFIED` only when all seven criteria (E1-E6, ReleaseLaunch) are PASS and there is no blocker or major finding. Be strict and concrete;
a finding needs evidence a third person can open. Your final reply: one line per group, with the decision and the number
of findings.
