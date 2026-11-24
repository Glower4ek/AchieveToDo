# 0.4.0 (rc.4) certification harness and results

Report: [docs/certification/0.4.0-purpur-26.3.md](../../../docs/certification/0.4.0-purpur-26.3.md).
The 0.3.0 harness and evidence are in [../rc3](../rc3/README.md).

- `harness/`: the campaign harness as it produced the certified rows.
  - **Orchestrators:** `campaign_final_rc4.py` (campaign51-55), `campaign_final_rc4b.py` (supplement campaign60-61),
    `smoke_rc4.py` and `smoke_rc4_presets.py` (start checks).
  - **Judging:** `campaign.py`, `campaign_cases.py`, `campaign_replay.py` (E6).
  - **Report tools:** `campaign_e1_rc4.py` (source map), `campaign_report_final.py` and `make_review_packages.py`
    (run with `ATD_REPORT=rc4`).
  - **Frozen versions:** `HARNESS_RC4A.sha256` is the harness of campaign51-55; its `campaign.py` and
    `campaign_cases.py` are in `history-rc4a/` (rebuilt byte for byte, see its README). `HARNESS_RC4.sha256` is the
    rc4b harness of campaign60-61 (the files in `harness/`). `InputDriver.java` is the test client driver.
  - **Brief:** `REVIEWER_BRIEF_RC4.md`.
- `results/`:
  - **Table:** `FINAL_TABLE_RC4.md` and `FINAL_GROUP_VERDICTS_RC4.json`, the 59 groups.
  - **Run status:** `FINAL_STATUS_RC4.json`, `FINAL_STATUS_RC4B.json`, smoke runs `SMOKE_RC4_*.json`.
  - **Source map:** `E1/`, per group.
  - **Certificates:** `certificates/`, one per group.
  - **Findings:** `FINDINGS_REGISTER.*`.
  - **Chat hover:** `presentation-results.jsonl`.
  - **Raw evidence:** `EVIDENCE_INDEX.json`, the SHA-256 of every receipt file kept outside git (final and smoke runs,
    including the aborted and superseded ones).
  - **Artifacts:** `SHA256SUMS` and `PROVENANCE.txt`.
  - **Logs:** `logs/`, orchestrator logs (packet trace lines removed).

The scripts contain absolute paths of the machine that ran them. They document the procedure; they are not a
turnkey tool.
