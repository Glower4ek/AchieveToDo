# B1 — Official BACAP 26.2 Pin

Selected official target: **BACAP 1.21 — 1.21 (Chaos Takeover of Mayhem)**, Modrinth version `Y2zZ5eSs`. The official version is a stable Release with exact Minecraft support `["26.2"]`, and its changelog identifies this as the 26.2 update. [Official version](https://modrinth.com/datapack/blazeandcaves-advancements-pack/version/Y2zZ5eSs).

## Selection and authoritative sources

Selection means the newest official stable Release specifically supporting final Minecraft Java Edition 26.2, ordered by publication time. Snapshot, pre-release, RC-only, alpha, beta, and newer-Minecraft-only versions are excluded.

Queried all **65** versions returned by the [unfiltered official versions API](https://api.modrinth.com/v2/project/VoVJ47kN/version), scanning supported versions and release names/changelogs for 26.2. An independent [exact final-26.2 query](https://api.modrinth.com/v2/project/VoVJ47kN/version?game_versions=%5B%2226.2%22%5D) returned only `Y2zZ5eSs`. The [selected-version API](https://api.modrinth.com/v2/version/Y2zZ5eSs) independently confirmed its identity and primary file.

The complete 26.2-family candidate table is:

| Version | Version ID | Channel | Exact Minecraft versions | Published UTC | Decision |
| --- | --- | --- | --- | --- | --- |
| 1.21 | `Y2zZ5eSs` | release | 26.2 | 2026-06-17T06:13:32.499541Z | Selected |
| 1.21.b2 | `YSahQ7UP` | beta | 26.2-rc-2 | 2026-06-13T23:30:47.48201Z | Excluded |
| 1.21.b1 | `IF78xEfk` | beta | 26.2-pre-2 | 2026-05-31T00:16:13.122729Z | Excluded |
| 1.21.a5 | `lO3WjSLw` | alpha | 26.2-snapshot-8 | 2026-05-24T01:33:58.58275Z | Excluded |
| 1.21.a4 | `Rj35m4ll` | alpha | 26.2-snapshot-7 | 2026-05-16T04:42:55.5086Z | Excluded |
| 1.21.a3 | `kVx9Imlb` | alpha | 26.2-snapshot-6 | 2026-05-09T10:22:23.433439Z | Excluded |
| 1.21.a2 | `RgGa8u6j` | alpha | 26.2-snapshot-3 | 2026-04-19T00:46:27.431624Z | Excluded |
| 1.21.a1 | `9Vsieocp` | alpha | 26.2-snapshot-2 | 2026-04-12T01:26:31.037862Z | Excluded |

The newer overall versions were checked separately:

| Version | Version ID | Channel | Exact Minecraft versions | Published UTC | Decision |
| --- | --- | --- | --- | --- | --- |
| 1.21.1 | `YPEkY5bZ` | release | 26.3 | 2026-09-16T07:12:13.094893Z | Excluded |
| 1.21.1.b1 | `3UTpCPex` | beta | 26.3-rc-2 | 2026-09-11T22:31:39.166606Z | Excluded |

Thus 1.21 wins even though 1.21.1 is newer overall: 1.21.1 supports only 26.3, while its beta supports only 26.3-rc-2. The remaining 26.2-family candidates target prerelease Minecraft builds and use non-stable channels. This result is an upstream-metadata snapshot taken on 2026-10-02.

## Version and download identity

From the [official project API](https://api.modrinth.com/v2/project/VoVJ47kN) and [selected-version API](https://api.modrinth.com/v2/version/Y2zZ5eSs):

| Field | Verified value |
| --- | --- |
| Project | BlazeandCave's Advancements Pack |
| Project ID / slug | `VoVJ47kN` / `blazeandcaves-advancements-pack` |
| Version ID / number | `Y2zZ5eSs` / `1.21` |
| Version name | 1.21 (Chaos Takeover of Mayhem) |
| Channel / platform | `release` / `datapack` |
| Publication UTC | `2026-06-17T06:13:32.499541Z` |
| Exact Minecraft versions | `["26.2"]` |
| Primary file ID | `EdZR962n` |
| Primary filename | `BlazeandCave's Advancements Pack 1.21.zip` |
| Official/local byte size | `3135424` |
| License identifier | `LicenseRef-All-Rights-Reserved` |

[Official project page](https://modrinth.com/datapack/blazeandcaves-advancements-pack). [Official primary CDN download](https://cdn.modrinth.com/data/VoVJ47kN/versions/Y2zZ5eSs/BlazeandCave%27s%20Advancements%20Pack%201.21.zip).

## Verified local hashes

Downloaded the selected primary ZIP directly from the official CDN into ignored temporary storage:

`build/tmp/phase_b_b1/BlazeandCave's Advancements Pack 1.21.zip`

| Algorithm | Independently computed hash | Upstream comparison |
| --- | --- | --- |
| SHA256 | `c71d1aa1a84dbe00a3f85a42144b46214c4669a3cccf07ff66631d28f16a99b2` | Not published by this upstream file record; recorded locally |
| SHA1 | `14da3f07b5467e8b59ffc0253fd8212c938cd739` | Exact match |
| SHA512 | `10df49e4b53d148309486603a59d8891025fcd405151b54610ac2499a636278c798d26211d2174dd3e161a8ef6ee5998014afe922dcddec67a149717aeac7f42` | Exact match |

Local byte size also exactly matches upstream metadata. The archive was inspected read-only without extraction, modification, or repacking.

## License and redistribution

The [official project](https://modrinth.com/datapack/blazeandcaves-advancements-pack) displays ARR and prohibits pack redistribution, including modified packs and inclusion in modpacks. The API returns an empty license name and null license URL; these values are preserved in the JSON alongside its license identifier.

The new raw ZIP remains solely in ignored `build/tmp/phase_b_b1/`. It is not included in the Git publication set. Durable B1 outputs contain metadata, hashes, paths, and structural flags without embedding the archive or advancement bodies.

## Structural inventory

| Measurement | Value |
| --- | --- |
| ZIP entries | 7091 |
| pack.mcmeta | Present |
| Declared minimum / maximum format | `[107, 1]` / `[107, 1]` |
| Data namespaces | `bacap_fanpacks`, `bacap_rewards`, `blazeandcave`, `minecraft` |
| Advancement JSON files | 1332 |
| No-display helpers | 72 |
| Hidden display entries | 17 |
| Excluded canonical root | 1 |
| Canonical visible advancements | 1242 |
| Canonical blazeandcave / minecraft | 1116 / 126 |
| B0 counting rule compatible | YES |
| Duplicate ZIP paths / advancement IDs | 0 / 0 |

All advancement JSON files use the singular `data/<namespace>/advancement/` directory. The same B0 rule applies directly: display exists, hidden is not true, and ID is not `blazeandcave:bacap/root`. Formula: **1332 - 72 - 17 - 1 = 1242**.

These are measurements of the acquired archive, not a runtime certification or semantic comparison. [b1_target_inventory.json](b1_target_inventory.json) contains all 1332 advancement IDs/paths and their exact uncompressed-byte SHA256 values, sizes, namespaces, and display/hidden/root/canonical flags.

## Changelog discovery

The [official release notes](https://modrinth.com/datapack/blazeandcaves-advancements-pack/version/Y2zZ5eSs) report these categories, paraphrased for later B3/B4:

- New advancements across multiple tabs.
- Renamed titles, menu rearrangements, and cross-tab moves.
- Changed collections, requirements, predicates, triggers, icons, and descriptions.
- New trophies and revised item/experience rewards.
- Configurable advancement point totals, progress triggers, counter changes, and hidden message settings.
- Shared reward macro calls and reorganized function/predicate paths.
- Translation-relevant title, description, trophy, legend, and root-message changes.

No explicit cooperative-mode behavior change is reported; update-function relocation is described beside cooperative updating. This is absence of an explicit changelog report, not proof that cooperative resources are unchanged.

The machine-readable category extraction is in [b1_official_bacap_pin.json](b1_official_bacap_pin.json). It summarizes this release's notes only and does not substitute for B3's complete old-to-new semantic diff.

## Companion-pack discovery

| Companion | Status | Apparent version | Final 26.2 evidence |
| --- | --- | --- | --- |
| Hardcore | FOUND | 1.21 / `ZHHHw5wF` | Official stable metadata lists `26.2` and `26.3` |
| Terralith | AMBIGUOUS | 1.18.3 | Explicit compatibility ends at 26.1.1; generic latest-version claim |
| Amplified Nether | AMBIGUOUS | 1.18.3 | Explicit compatibility ends at 26.1.1; generic latest-version claim |
| Nullscape | AMBIGUOUS | 1.18.3 | Explicit compatibility ends at 26.1.1; generic latest-version claim |

Sources: [official Hardcore version](https://modrinth.com/datapack/blazeandcaves-advancements-pack-hardcore-version/version/ZHHHw5wF), [Hardcore API](https://api.modrinth.com/v2/version/ZHHHw5wF), and [official worldgen-companion page](https://www.planetminecraft.com/data-pack/blazeandcave-s-advancements-pack-terralith-version/). The latter's latest visible update log identifies 1.18.3, dated 2025-04-24T22:33:54Z.

The [official author's public Modrinth project list](https://api.modrinth.com/v2/user/XTAa7v2H/projects) contained main BACAP, Hardcore, and Language Pack; no worldgen-companion Modrinth project was found there. Language Pack was encountered only as a project-list entry; no matching release or translations were investigated.

The worldgen-companion page was available through the web tool's recent crawl; direct shell access encountered Cloudflare blocking. Final 26.2 support is therefore unconfirmed, not proven absent. No companion pack was downloaded or pinned as a Phase B target.

## State and safety

Branch remains `phase-b-bacap-26.2`; HEAD remains `b261b02cd03b4aeae2835c63f982c9aa53c46eed`. The old BACAP remains version 1.18.1, SHA256 `8c72314535c5df7b4416bf0f38310371ec8aec537fde1445bdc820a3c9aada70`, with 1152 canonical advancements. The production compatibility marker remains `compat_26_2_r16`.

B0 documents stay byte-for-byte unchanged; their historical unpinned target is retained. B1 is the durable authority for this newly pinned target.

No integration, B2 dependency mapping, B3 semantic diff, localization research, product Java/resources changes, tests changes, existing BACAP changes, or historical Phase A/FINAL19 evidence changes occurred. No commit, push, tag, or GitHub mutation occurred.

**PHASE_B_B1_PINNED**. Next accepted stage is B2, which has not started.

