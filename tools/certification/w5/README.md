# W5 certification harness and results (Purpur 26.2, 0.2.1-rc.1)

Report: [docs/certification/w5-purpur-26.2.md](../../../docs/certification/w5-purpur-26.2.md).

- `harness/`: the campaign harness exactly as it produced the certified rows.
  - `campaign.py`: case runner, `judge` and `evaluate`.
  - `campaign_cases.py`: the 198 cases.
  - `campaign_replay.py`: E6 replay.
  - `campaign_e1.py`: E1 source map.
  - `campaign_final*.py`: orchestrators for r3–r6.
  - `campaign_report_final.py`: final table.
  - `make_review_packages.py`: review packages.
  - `make_evidence_index.py`: raw-evidence index.
  - `REVIEWER_BRIEF.md`: reviewer checklist and certificate format.
  - `HARNESS_R*.sha256`: hashes of each frozen version. `history/` holds the earlier versions those hashes refer to.
- `driver-mod/`: source of the client input driver (Fabric mod `atd-input-driver`).
- `trace-agent/`: source of the server trace agent (observer only).
- `results/`:
  - `FINAL_TABLE.md` and `FINAL_GROUP_VERDICTS.json`: the 57-group table.
  - `FINAL_STATUS_R*.json`: per-round run status.
  - `E1/`: source map per group.
  - `certificates/`: individual reviewer certificates, with round 1 of the two re-reviewed groups in `round1/`.
  - `FINDINGS_REGISTER.*`: all reviewer findings.
  - `EVIDENCE_INDEX.json`: SHA-256 of every raw receipt file kept outside git.
  - `logs/`: orchestrator logs.

The scripts contain absolute paths of the machine that ran them, such as `E:\Vibecode\AchieveToDo`, the Prism
instance and the JDK. They document the procedure and are not a turnkey tool. To re-run them, copy `harness/` next
to a prepared test runtime and adjust those paths.
