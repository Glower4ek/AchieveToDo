# AchieveToDo Purpur 26.3 — independent group review (0.4.0 release, rc.4)

You are an independent reviewer. You did not build this evidence and you must not trust its conclusions:
check them against the raw material. You review the groups assigned to you, one certificate per group.

## What is certified

The Purpur plugin `AchieveToDo-Purpur-mc26.3+0.4.0.jar` (SHA-256 `16d0b2fa632887355eb3a56846d52177aa1b62556fad925205349e9ba220befa`,
built from commit `ae87515483b51284d5a42a646fca0fc0840fea0b`, branch `release-0.4.0`) for Purpur 26.3 (build 2660) ports the
Fabric mod AchieveToDo: each player ability (open a chest, eat salmon, sprint, enter the End, interact inside a village …)
is **locked** until the player's advancement count reaches the ability's threshold in the world's progression config
(EASY / NORMAL / HARD / CHAOS / Custom). While locked, the action must be denied with no gameplay effect; once the count
reaches the threshold, the action must work normally. The test client is a real Fabric client on Minecraft 26.3 (mod
`achievetodo-mc26.3+0.4.0.jar`, installed as `achievetodo.jar`, SHA-256 `e8577e1d28a7fb9b903eabfed7b9b0e5d5b15ad895be078d8617c6493ccde61c`)
logged in online. Both jars were rebuilt from a clean clone of that commit byte for byte.

## Inputs (read only)

- Package per group: `E:\Vibecode\AchieveToDo\certification\w5-campaign\review-rc4-pkg\<group>\`
  - `MANIFEST.json`: hashes, runtimes, E3/E5/E6 evidence, verdict glossary, superseded rows
  - `E1.json`: static map ability → guard sites in the plugin source (file, lines, SHA-256) and thresholds per config
  - `rows.jsonl`: every final result row of the group (one per runtime × pass tag × ability × case × phase)
  - `replay.jsonl`: E6 replay of each row (verdict recomputed from raw logs after the server stopped)
  - `excerpts.txt`: per row, the raw server log lines its verdict was judged from (long state lines compacted)
- Plugin source: `E:\Vibecode\AchieveToDo\repo\port\AchieveToDo-Purpur\src\main\java\`
- Raw run folders if you need more: `E:\Vibecode\AchieveToDo\certification\w5-campaign\campaignNN\receipts\`
  (campaign51-55, campaign60-61; output.log can be ~100 MB: search it, never read it whole)
- Harness that produced the rows (for understanding only): `campaign.py` (`judge`, `evaluate`), `campaign_cases.py`.

Do not modify, move or delete anything. The only files you may create are your certificates (see Output).

## Row anatomy

`phase`: `locked` (score = threshold − margin), `allowed` (score = threshold), `creative`, `op`, `spectator`,
`offhand_locked`, `offhand_allowed`. `tag`: `pass1`, `modes`, `offhand`, `reload` (NORMAL runtime), `thresholds-<config>`
(EASY, HARD, CHAOS, Custom runtimes). `counts` = advancement count before/after the action.
`why.observed` = which effect oracles fired; `why.overlay` = the client showed the lock message; `why.leak` = advancement
criteria gained during the action; `why.bookkeeping` (new in rc.4, see Oracle rules) = BACAP tallies set aside.
`requires` = other abilities the fixture itself needs, with their thresholds.
In `excerpts.txt`, `ATD_W2_INPUT_EVENT` / `ATD_EVENT` lines show the plugin's decision (`useBlock=DENY`, `cancelled=true` …),
`ATD_STAGE2A_STATE` shows count/mode/op/generation and the tested ability's lock state, `ATD_PACKET_IN` the client packet,
`[TraceBridge] … ->` lines the traced server method returns.

## Checklist per group

Mark each criterion PASS or FAIL, and say exactly what you checked.

- **E1, source map.** Every ability of the group has at least one direct guard site or a family resolver in `E1.json`.
  For **every** source file cited, compute its current SHA-256 and compare it with E1 (for example `python -c` with hashlib,
  or `certutil -hashfile <file> SHA256`). Open at least one guard site per ability and confirm that it really checks that
  ability. Every config that ran (normal, easy, hard, chaos_20261116, custom_vision) lists a threshold for each ability.
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
  - The rows tagged `reload` come from `campaign51` (and `campaign60` for its superseded igloo rows), after the pass1,
    modes and offhand passes of that runtime.
  - `MANIFEST.e5Reload.evidence.generationAfter` is 2 (campaign51); campaign60 shows generations `[1, 2]` too.
  - The `ATD_STAGE2A_STATE` lines of those rows in `excerpts.txt` show `"generation": 2`.
  - Each reload verdict agrees with that ability's `pass1` verdict.
- **E6, hashes and replay.**
  - Every row in `replay.jsonl` is `REPRODUCED`, and every runtime's replay has `reproducedAll=true`.
  - The plugin SHA-256 is identical in every runtime and equals the value above. The client mod SHA-256 equals the value
    above.
  - Recompute the SHA-256 of `campaign-results.jsonl` of at least one runtime and compare it with that runtime's
    `evidenceHashes`.
- **ReleaseLaunch.** The certified composition is the operator launch from
  `port/AchieveToDo-Purpur/STARTING_THE_SERVER.md`. The server command holds `-javaagent:plugins/achievetodo.jar`
  plus the observer trace agent. The only `-Dachievetodo.*` option allowed is the documented first-start option
  `-Dachievetodo.progression.initialConfig=<preset>`, and only for a world built as a fresh install (no
  `plugins/AchieveToDo/progression/world-group.properties` or `world-selection.properties` before the start). This is
  how EASY, HARD and CHAOS (campaign52-54, campaign61) are made; NORMAL (campaign51, campaign60) and Custom (campaign55)
  start an existing install with no option. See `runtime_args.json` and `receipts/launch.json` in each runtime. Confirm
  for every contributing runtime:
  - `ATD_LAUNCH_PROFILE=release` and `ATD_GAMEPLAY_MODE=RELEASE` and `ATD_PRODUCT_GAMEPLAY_READY=true` are logged before the first case;
  - any `freshConfig` came from the plugin itself, i.e. it is listed in the logged `derived=[...]`, not passed by hand.
  All rows come from campaign51-55 under one harness (`HARNESS_RC4.sha256`), with two supplement runtimes under the
  same harness file, `campaign_final_rc4b.py`: `campaign60` (NORMAL) reruns the `interact_inside_igloo` rows of the
  `modes` and `reload` passes, and `campaign61` (HARD) the `glide_with_fireworks` rows. Their rows replace the rows with
  the same tag, ability, case and phase (`MANIFEST.superseded`). The reasons given by the harness author: the igloo
  criteria-audit room left its item frame, tadpole and dropped items behind, so a leftover made the next row's probe
  true before the action, and a tadpole out of water died before the post probe; the HARD glide row was failed only by
  BACAP tallies. Judge whether each replacement is justified from the campaign51/53 and campaign60/61 log lines, not
  from this text.
- **Reduced presets.** EASY, HARD, CHAOS and Custom (campaign52-55) ran `--sample one-per-group`: only the first case of
  every group, locked and allowed. The full case set ran on NORMAL (campaign51). Check that each group has its preset
  rows, not that every case does.
- **Oracle rules** (in `campaign.py` `judge`/`evaluate` and `campaign_cases.py`; check that they hold in the rows):
  - `expect_hit`: an aimed case whose client aimed at nothing is `INVALID_FIXTURE_AIM`; the orchestrator reruns such
    abilities once in the same pass, both rows stay in `campaign-results.jsonl`, and the later one counts.
  - `server_reach`: vehicles, all villagers and the wandering trader must show a traced `Player.interactOn` return during
    the action, otherwise `INVALID_NOT_REACHED_SERVER`.
  - Villager trades: the traced decision. Minecraft 26.3 renamed `InteractionResult.SwingSource.CLIENT` to `PREDICTED`
    (`SERVER` to `SERVER_ONLY`): `Success[swingSource=PREDICTED]` when the trade opens, `swingSource=NONE` when the guard
    consumes the click. The wandering trader keeps the screen oracle.
  - `overlay_text`: a shelf row passes only if the denial message names the shelf.
  - **New in rc.4, BACAP tallies.** BACAP keeps tallies in its own tick functions (`blazeandcave:bacap/advancement_legend`
    and `blazeandcave:bacap/*_milestone`). When the criteria gained in a row are **only** such tallies, they are moved from
    `why.leak` to `why.bookkeeping` and are not a leak; any other non-hidden criterion still fails a locked row. Judge
    whether this rule is sound and whether it hid a real leak anywhere (look at every row with `why.bookkeeping`).

Changed or new product behaviour in 0.4.0 (requirements; details in `repo/docs/known-issues.md` 5-8):
- (a) `interact_inside_abandoned_camp` (group `landmark_abandoned_camp_26_3`): the 26.3 abandoned camp structures are a
  lockable landmark like the igloo.
- (b) Known issue 5: a denied cartographer trade shows its lock message (`why.overlay` true on locked rows).
- (c) Known issue 6: a denied use or interaction must not count for `player_interacted_with_entity`,
  `item_used_on_block`, `default_block_use` or `any_block_use` criteria. Group `landmark_denial_criteria_26_3` audits it
  inside a locked igloo (a map into an item frame: BACAP "Map Room"; a golden dandelion on a tadpole: "Fountain of
  Youth"). On locked rows any such criterion is a failure.
- (d) Known issue 8: inside a locked landmark the 26.3 golden dandelion age lock is denied (the tadpole rows above).
- Report evidence of any other advancement leak you see.

## Output

For each group write `E:\Vibecode\AchieveToDo\certification\w5-campaign\review-certs-rc4\<group>_INDIVIDUAL_CERTIFICATE.json`:

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
