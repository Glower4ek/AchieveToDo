# B6 — Russian Localization Update

Status: **PHASE_B_B6_LOCALIZED**. `b7Ready = true`. Independent B7 certification has not started.

## Effective requirements

The accepted B5.7 pack order was reconstructed from current ATD resources and exact r17-derived source archives for main BACAP, Hardcore, Terralith, Amplified Nether and Nullscape. A JSON component walker and an independent quoted/SNBT command scanner agree. Commented commands are excluded. Functions are conservatively treated as supported config/reward library surfaces rather than restricted to one current gameplay branch.

The union contains 4,304 requirements: 3,435 resolve through the Minecraft RU overlay, 533 through the native ATD dictionary, 325 through the actual cached Minecraft 26.2 Russian dictionary, and 11 are deliberately language-neutral project/person/service names, command syntax, glyphs or spacing. English fallback is never counted as Russian coverage. The manifest records every key, consumer and disposition.

B4 classified 319 added, 41 removed and 42 changed-review keys in its upstream comparison. B5.7 counted 2,608 keys in its local structural scan. B6 also covers effective native config/trophy/function resources and retained companion content. Existing Phase A translations already cover some added-target keys; vanilla provides registry keys; five removed-main keys remain consumed by companions. These handoffs therefore cannot be treated as direct dictionary additions/deletions.

## Source and permission audit

Official [Language Pack 1.21](https://modrinth.com/resourcepack/bacap-language-pack/version/hsqY3G3V), YX5bAAJN/hsqY3G3V, targets Minecraft 26.2. Its public license remains ARR. The existing `reference/localization/phase_a_ru_full_reaudit_summary.json` explicitly identifies this exact version and permits its use in AchieveToDo through direct owner permission obtained by the maintainer. This is repository-specific permission, not a grant for unrelated projects or translation forks. Current Russian-update credit: **1A_KapiBar**. Historical Russian credits: SoSeDiK (1.4-1.10); vlad8599 - Some contributions to (1.10); KorS1z - (1.11-1.12); Marsik_the_alien (1.13-1.13.2); _InGlorious_ (1.13.7, 1.19); Harrydi﻿ (1.14); StrongDeep (1.15-1.16); y13gkb (1.17); vinelia (1.17 - 1.18.2).

[BACAP Better RU](https://modrinth.com/resourcepack/bacap-better-ru), yiLJxtqH/2.3, is CC0-1.0, not BSD. Credits: diskria (Modrinth owner), diskree (pack author and existing provenance). The existing license record stays unchanged. The separate [Perchila27/Limoro translation](https://modrinth.com/resourcepack/ru-blaze-and-caves-advancements-pack), DlwNJUii/3, is BSD-2-Clause and targets Minecraft through 1.21.5. Both were compared against current meaning; neither supplies a missing value or material improvement over the retained entries, so no new values were imported.

[BACAP RUS Translate](https://modrinth.com/resourcepack/bacap-rus-translate), 47BuT3SL/ULq4UdnT, explicitly documents Minecraft 26.2 and BACAP 1.21. It credits SHTUKA and the original Perchila27/Limoro translation and declares BSD-2-Clause. It contributes 21 exact-key values with clearer target meanings/titles. The one new non-runtime legal artifact, `licenses/bacap-rus-translate/LICENSE`, retains attribution, the supplied disclaimer and the unmodified canonical BSD template. Upstream supplied no standalone filled-in copyright notice; none was invented. The existing JAR task includes this LICENSE automatically, without a Gradle change.

[Enhanced Discoveries Language Pack 2.9.2](https://modrinth.com/resourcepack/bacaped-language-pack/version/BItBGCl9), CC-BY-4.0, credits ItzSkyReed and targets a separate addon. Its four overlapping current keys are already adequately translated; no text was imported. [BACAP_RUS_](https://modrinth.com/resourcepack/bacap_rus_) by Dark_Night_W is ARR, targets an older Minecraft version and has no repository permission; metadata only. [Altea](https://github.com/alteamc/blazeandcaves-advancements-pack/blob/master/LICENSE) explicitly reserves upstream rights; reference only, no translation copied. All downloaded archives remain ignored under `build/tmp/phase_b_b6/` and were checked against official size and SHA1/SHA512 metadata.

## Authoring and provenance

Correct Phase A values take precedence over unnecessary churn. Permitted target-aligned LP1.21 supplies missing/changed strings; the current BSD source supplies demonstrably clearer values; remaining gaps and incorrect source wording were translated or revised manually from current English. Runtime architecture remains the original single Minecraft RU overlay, with no translation loader, generated per-companion dictionary or new dependency.

Retained: 3,186. Added: 286. Updated: 10. Removed: 32. Manually authored/revised: 64 of the added/updated values. Dictionary size: 3,228 → 3,482. Final-value provenance: 3,186 retained Phase A; 211 official LP1.21 with permission; 0 Better RU BSD imports; 21 other permitted-source values (current BSD pack); 64 manual B6 values. Every changed value has source identity, final-value hash, consumers and semantic/placeholder review.

All 42 changed-component keys have explicit old/current component and criterion fingerprints, candidate-source review and final dispositions. Corrections include honey 200 → 100, mining rather than collecting, removing unsupported extra summoning requirements, the spear-charge exclusion, and current smithing-template exceptions. LP errors such as sheep radius 100 instead of 16 and spear hit descriptions translated as kills are corrected. All three inherited empty entries are filled.

Five removed-main keys remain unchanged because effective supported companion/ATD consumers still use them:

- `Collect a stack of scutes`
- `Kill a Skeleton or Stray while both you and it have levitation`
- `Kill a raid captain. Maybe consider staying away from villages for the time being...`
- `The fourth is to take a temple tripwire into account`
- `The third is to travel on an upside-down mount`

Each actual removal has zero effective consumers and is neither a native ATD nor intentionally retained generic/vanilla key. Of the 41 removed-main handoff keys, 32 entries are removed, five retained for consumers, and four were absent already.

## Companion coverage

| Combination | Effective keys resolved | Companion overlay keys resolved | Missing |
|---|---:|---:|---:|
| amplifiedNether | 3702 / 3702 | 7 / 7 | 0 |
| hardcore | 3702 / 3702 | 50 / 50 | 0 |
| nullscape | 3705 / 3705 | 8 / 8 | 0 |
| terralith | 3756 / 3756 | 81 / 81 | 0 |

All 53 B4-selected companion keys remain consumed and covered. The Sulfur Cube hurt description comes from permitted LP1.21. The current companion-overlay union contains 136 keys; the manifest records each consumer and final provenance.

## Validation and B7 handoff

UTF-8/JSON pass; duplicate/null/empty values: zero. Placeholder identity, multiplicity and implicit ordering pass for all 4,293 language-owned requirements, including real vanilla EN/RU and native ATD EN/RU providers. All 296 added/updated values were reviewed for English leaks; only documented proper names, acronyms, Roman numerals, formatting and printf tokens remain. The smithing-template note retains its meaningful newlines and style.

`gradlew.bat --no-daemon --offline processResources -x generateBuildConfig`: GREEN, executed. Processed RU bytes match source SHA256 `8961ad9d573ab72f63969e4a808b19ce733bc9a60b7ab589fc44dfb6148e1a0a`. All 245 prior B5 resource outputs/deletions remain correct. `git diff --check`: PASS. All 251 B5 paths, prior evidence and historical files remain exact.

B6 runtime delta: 0 ADD / 1 MODIFY / 0 DELETE. Cumulative product delta: 177 ADD / 66 MODIFY / 9 DELETE. The sole additional license file is a non-runtime legal artifact explicitly allowed for a newly used source.

Branch: `phase-b-bacap-26.2`; HEAD: `b261b02cd03b4aeae2835c63f982c9aa53c46eed`; marker: `compat_26_2_r17`. Nothing staged. No commit, push, tag or GitHub mutation. No Java, tests, gameplay resources, archives, pins, converter, downloader, ability/threshold/balance or progress-migration changes.

B7 must independently certify the authored dictionary and recorded requirements/provenance. B6 self-checks do not constitute B7 certification.
