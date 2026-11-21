# 0.2.1 (rc.2) certification harness and results

Report: [docs/certification/0.2.1-purpur-26.2.md](../../../docs/certification/0.2.1-purpur-26.2.md).
The rc.1 (W5) harness and evidence are in [../w5](../w5/README.md). This folder holds what changed for 0.2.1.

- `harness/`: the campaign harness as it produced the certified rows.
  - **Launch and modes:** `prepare_campaign.py` builds the operator launch (`ATD_LAUNCH=release`).
    `campaign_modes.py` builds a fresh install with `initialConfig` for EASY, HARD and CHAOS.
  - **Judging:** `campaign.py` and `campaign_cases.py` hold the oracles and cases, including
    `expect_hit`, `server_reach` and `INVALID_FIXTURE_PRESET`.
  - **Orchestrators:**
    - `campaign_final_rc2c.py`: the full round c;
    - `_rc2d.py`: the `ignite_tnt` CHAOS supplement;
    - `_rc2f.py`: the `enter_nether` modes supplement;
    - the earlier rc.2 attempts (`_rc2.py`, `_rc2_resume.py`, `_rc2_custom.py`, `_rc2b.py`) are kept for the record.
  - **Frozen versions:** `HARNESS_RC2*.sha256`. `history/` holds the exact files these hashes refer to:
    `*.rc2c` produced campaign31-35, `*.rc2e` is campaign37 (rejected), `*.rc2f` is campaign38.
  - **Briefs:** `REVIEWER_BRIEF_RC2.md` (round a) and `REVIEWER_BRIEF_RC2C.md` (round c).
- `results/`:
  - **Table:** `FINAL_TABLE_RC2C.md` and `FINAL_GROUP_VERDICTS_RC2C.json`, the 57 groups.
  - **Run status:** `FINAL_STATUS_RC2*.json`.
  - **Source map:** `E1/`, per group.
  - **Certificates:** `certificates/`, round c. `round1/` and `round2/` hold the earlier rounds of the portal group.
  - **Round a:** `round-a/`, the first, rejected rc.2 round, with its table and certificates.
  - **Findings:** `FINDINGS_REGISTER.*`.
  - **Raw evidence:** `EVIDENCE_INDEX.json`, the SHA-256 of every raw receipt file kept outside git.
  - **Artifacts:** `SHA256SUMS` and `PROVENANCE.txt` for the certified jars.
  - **Logs:** `logs/`, orchestrator and smoke logs.

The scripts contain absolute paths of the machine that ran them. They document the procedure; they are not a
turnkey tool.
