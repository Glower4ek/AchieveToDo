# AchieveToDo Purpur 26.2 — independent group review (W5, stage 4)

You are an independent reviewer. You did not build this evidence and you must not trust its conclusions:
check them against the raw material. You review the groups assigned to you, one certificate per group.

## What is certified

The Purpur plugin `AchieveToDo-Purpur-26.2-0.2.1-rc.1.jar` (SHA-256 `ad0685eda3cb6f56a408a6409d27809e5e0f961dcd631759adea0564cac9d80b`)
ports the Fabric mod AchieveToDo: each player ability (open a chest, eat salmon, sprint, enter the End, interact inside a
village …) is **locked** until the player's advancement count reaches the ability's threshold in the world's progression
config (EASY / NORMAL / HARD / CHAOS / Custom). While locked, the action must be denied with no gameplay effect; once the
count reaches the threshold, the action must work normally. The test client is a real Fabric client (mod
`achievetodo.jar`, SHA-256 `46a0601e61e0b22752d4b814b622cc04874b5463843beddadccd9f7f10855081`) logged in online.

## Inputs (read only)

- Package per group: `E:\Vibecode\AchieveToDo\certification\w5-campaign\review\<group>\`
  - `MANIFEST.json`: hashes, runtimes, E3/E5/E6 evidence, verdict glossary, superseded rows
  - `E1.json`: static map ability → guard sites in the plugin source (file, lines, SHA-256) and thresholds per config
  - `rows.jsonl`: every final result row of the group (one per runtime × pass tag × ability × case × phase)
  - `replay.jsonl`: E6 replay of each row (verdict recomputed from raw logs after the server stopped)
  - `excerpts.txt`: per row, the raw server log lines its verdict was judged from (long state lines compacted)
- Plugin source: `E:\Vibecode\AchieveToDo\repo\port\AchieveToDo-Purpur-26.2\src\main\java\`
- Raw run folders if you need more: `E:\Vibecode\AchieveToDo\certification\w5-campaign\campaign1N\receipts\`
  (output.log can be ~100 MB: search it, never read it whole)
- Harness that produced the rows (for understanding only): `campaign.py` (`judge`, `evaluate`), `campaign_cases.py`.

Do not modify, move or delete anything. The only files you may create are your certificates (see Output).

## Row anatomy

`phase`: `locked` (score = threshold − margin), `allowed` (score = threshold), `creative`, `op`, `spectator`,
`offhand_locked`, `offhand_allowed`. `tag`: `pass1`, `modes`, `offhand`, `reload` (NORMAL runtime), `thresholds-<config>`
(EASY, HARD, CHAOS, Custom runtimes), `landmarks-chaos-r4`. `counts` = advancement count before/after the action.
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
  - The rows tagged `reload` come from `campaign17`.
  - `MANIFEST.e5Reload.evidence.generationAfter` is 2.
  - The `ATD_STAGE2A_STATE` lines of those rows in `excerpts.txt` show `"generation": 2`.
  - Each reload verdict agrees with that ability's `pass1` verdict.
- **E6, hashes and replay.**
  - Every row in `replay.jsonl` is `REPRODUCED`, and every runtime's replay has `reproducedAll=true`.
  - The plugin SHA-256 is identical in every runtime and equals the value above. The client mod SHA-256 equals the value
    above.
  - Recompute the SHA-256 of `campaign-results.jsonl` of at least one runtime and compare it with that runtime's
    `evidenceHashes`.
- **Superseded rows.** `MANIFEST.superseded` lists r3 rows replaced by a supplement. Say whether each replacement is
  justified:
  - r4 is a fixture fix: the chest room was moved off boxes of other locked landmarks;
  - r5 is the real datapack reload, because the r3 reload command was rejected.

Known, accepted product behaviour (parity with the Fabric original, not a port defect):
- (a) touching a locked End portal or gateway is denied, but grants the vanilla enter_block advancement (+1);
- (b) CHAOS thresholds differ between runtimes that use the same seed (generator iteration order). Each runtime's rows
  carry their own thresholds.

Do not raise these as findings unless the evidence contradicts this description.

## Output

For each group write `E:\Vibecode\AchieveToDo\certification\w5-campaign\review-certs\<group>_INDIVIDUAL_CERTIFICATE.json`:

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
    "E6": {"result": "...", "checked": "..."}
  },
  "superseded": "assessment",
  "findings": [{"severity": "blocker|major|minor", "criterion": "E2", "evidence": "file:line or row", "description": "..."}]
}
```

`CERTIFIED` only when all six criteria are PASS and there is no blocker or major finding. Be strict and concrete;
a finding needs evidence a third person can open. Your final reply: one line per group, with the decision and the number
of findings.
