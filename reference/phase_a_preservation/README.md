# Phase A Historical Artifact Preservation

This directory is reserved for preserving the exact historical external gameplay artifacts referenced by the baseline source.

Files layout:

- `files/raw/`: raw downloaded artifacts exactly as obtained from the source URLs
- `files/final/`: normalized verified ZIPs matching the baseline in-game expected artifact hashes
- `artifact-report.json`: machine-readable preservation report with URLs, expected hashes, actual hashes, and verification status

The eight `files/final/*.zip` archives are tracked frozen regression, provenance and current-compatibility inputs: `amplified_nether.zip`, `bacap_amplified_nether.zip`, `bacap_hardcore.zip`, `bacap_nullscape.zip`, `bacap_terralith.zip`, `bacap.zip`, `nullscape.zip` and `terralith.zip`. Preserve their exact frozen source bytes; a converted runtime copy must not replace them.

Raw downloads under `files/raw/`, temporary artifacts and acquisition/cache material remain ignored. The tracked frozen archives are not packaged into the runtime mod merely because they are tracked. Keep the URLs and hash provenance in `artifact-report.json`, and preserve the upstream licensing and attribution recorded in the repository's `licenses/` directory and artifact metadata.
