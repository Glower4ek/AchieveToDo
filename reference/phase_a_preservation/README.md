# Phase A Historical Artifact Preservation

This directory is reserved for preserving the exact historical external gameplay artifacts referenced by the baseline source.

Files layout:

- `files/raw/`: raw downloaded artifacts exactly as obtained from the source URLs
- `files/final/`: normalized verified ZIPs matching the baseline in-game expected artifact hashes
- `artifact-report.json`: machine-readable preservation report with URLs, expected hashes, actual hashes, and verification status

The `files/` subtree is intentionally git-ignored so the preserved artifacts do not accidentally ship inside the mod or bloat ordinary review diffs.
