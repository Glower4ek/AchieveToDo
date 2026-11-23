# 0.3.0 (rc.3) certification harness and results

Report: [docs/certification/0.3.0-purpur-26.2.md](../../../docs/certification/0.3.0-purpur-26.2.md).
The 0.2.1 harness and evidence are in [../rc2](../rc2/README.md).

- `harness/`: the campaign harness as it produced the certified rows.
  - **Orchestrators:** `campaign_final_rc3.py` (campaign41-45), `campaign_final_rc3b.py` (supplement campaign46),
    `smoke_rc3.py` (start check).
  - **Judging:** `campaign.py`, `campaign_cases.py`, `campaign_replay.py` (E6).
  - **Report tools:** `campaign_e1_rc3.py` (source map), `campaign_report_final.py` and `make_review_packages.py`
    (run with `ATD_REPORT=rc3`).
  - **Frozen versions:** `HARNESS_RC3.sha256` and `HARNESS_RC3B.sha256`; `history-rc3/` and `history-rc3b/` hold the
    exact files these hashes refer to. `InputDriver.java` is the test client driver (with the `chat` command).
  - **Brief:** `REVIEWER_BRIEF_RC3.md`.
- `results/`:
  - **Table:** `FINAL_TABLE_RC3.md` and `FINAL_GROUP_VERDICTS_RC3.json`, the 57 groups.
  - **Run status:** `FINAL_STATUS_RC3*.json`, including the run aborted for the nether portal leak.
  - **Source map:** `E1/`, per group.
  - **Certificates:** `certificates/`, one per group.
  - **Findings:** `FINDINGS_REGISTER.*`.
  - **Chat hover:** `presentation-results.jsonl`.
  - **Raw evidence:** `EVIDENCE_INDEX.json`, the SHA-256 of every receipt file kept outside git.
  - **Artifacts:** `SHA256SUMS` and `PROVENANCE.txt`.
  - **Logs:** `logs/`, orchestrator logs.

The scripts contain absolute paths of the machine that ran them. They document the procedure; they are not a
turnkey tool.
