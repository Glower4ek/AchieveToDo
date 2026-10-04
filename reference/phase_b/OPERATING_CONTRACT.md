# Phase B — BACAP 26.2 Update

## Purpose

Phase A ported the original AchieveToDo 1.21.4 implementation to Minecraft 26.2 while preserving the old BACAP advancement state.

Phase B updates BACAP content from that preserved state to the latest official BACAP release for Minecraft 26.2.

## Architectural rule

Original AchieveToDo architecture is authoritative. Follow it as closely as possible.

Prefer extending/updating existing original mechanisms over introducing new architecture. A cleaner alternative architecture alone does not justify a redesign.

## Target

Latest official BACAP release specifically intended for Minecraft 26.2.

The exact upstream source, version, and hash will be pinned during B1. The target is currently unpinned.

## Allowed scope

- Pin the latest official BACAP for Minecraft 26.2.
- Compare the preserved BACAP state against that official target.
- Add new BACAP advancements.
- Update BACAP advancements changed upstream.
- Remove BACAP content removed upstream when appropriate.
- Update required related BACAP resources.
- Adapt AchieveToDo integration only where the new BACAP requires it.
- Update Search, indexing, and resource references as required.
- Update localization using the original ATD localization approach.
- Use all usable existing Russian BACAP translations available.
- Preserve attribution and licensing requirements.
- Fill all missing/new Russian translations ourselves.
- Add Phase B validation, certification, and runtime tests.
- Make narrowly justified compatibility changes required by the official BACAP target.

These are Phase B scope boundaries, not authorization to execute later stages during B0.

## Explicit non-goals

- No ability rebalance.
- No threshold changes.
- No ability redesign.
- No unrelated UI/UX work.
- No general refactor or unrelated production-code refactor.
- No old advancement-progress migration.
- No localization architecture redesign or separate translation product/system unless original ATD already works that way.
- No published release/history rewrite.
- No Phase C/D/E work.

## Advancement semantics

Phase B follows current official BACAP state.

- Added upstream: add it.
- Changed upstream: update it.
- Removed upstream: remove/update references as required.
- Renamed/moved upstream: follow the new official identifier/state.

There is no compatibility requirement to preserve completed advancement progress across upstream ID changes. Do not build old-progress migration solely to preserve v0.1.5.4 advancement completion.

## Localization rule

Follow original AchieveToDo localization architecture.

Research and use all usable existing Russian BACAP translations, not merely a fixed historical set. Preserve required credits, attribution, and licenses. Fill all remaining missing or new translation strings ourselves.

Do not invent a separate localization architecture. Historical Phase A translation-source exclusions do not restrict Phase B research; each source must be usable under its applicable license.

## Historical evidence rule

Phase A and FINAL19 historical evidence stays immutable.

New Phase B evidence belongs under reference/phase_b/ or a clearly Phase-B-specific durable path. Preserve the frozen Phase A inputs and evidence as the old side of future comparisons.

## Compatibility marker rule

Do not increment/change the current production compatibility marker merely because the BACAP content version changes.

Change it only if Phase B introduces an actual production compatibility conversion revision that requires a new marker.

The verified v0.1.5.4 production marker is `compat_26_2_r16`, read from `ExternalPackCompatibility.MARKER_VERSION` at the release commit. Historical Phase A/FINAL19 records containing `compat_26_2_r15` remain historical evidence; do not rewrite them to match the released source.

## Branch rule

All Phase B work remains on:

`phase-b-bacap-26.2`

until Phase B terminal reconciliation and a later explicit merge/release decision.

Published baseline: branch `26.2-port`, commit `b261b02cd03b4aeae2835c63f982c9aa53c46eed`, tag `v0.1.5.4`.

The published release is complete. Preserve its tag and release, and preserve published history; do not rewrite or force-push `26.2-port`.

## B0 operating boundary

This transaction authorizes only read-only published-baseline verification, creation of one local branch from the verified release commit, and durable Phase B contract/baseline documentation.

B0 must finish with:

- Current branch `phase-b-bacap-26.2` and HEAD equal to the release commit.
- Only the two intended new B0 documents under `reference/phase_b/` untracked.
- Existing product Java, resources, BACAP, localization, tests, and historical evidence unchanged.
- No commit, push, tag, or GitHub mutation.
- No new BACAP download or content update, and no B1 execution.

If either B0 output file already exists before creation, stop and report instead of overwriting it. After the safety check, report `PHASE_B_B0_READY` only if every B0 requirement passes; otherwise report `PHASE_B_B0_STOP`. Then stop.

The baseline discovery and source dependency inventory are recorded in [b0_baseline.json](b0_baseline.json). This inventory identifies existing surfaces; it does not claim to complete B2 or prescribe product changes.

## Validation roadmap

| Stage | Accepted work |
| --- | --- |
| B1 | Official BACAP pin |
| B2 | Original-ATD dependency/integration map |
| B3 | Complete old -> new BACAP semantic diff |
| B4 | Exact required product-change inventory |
| B5 | BACAP integration |
| B6 | Russian localization update |
| B7 | Localization coverage certification |
| B8 | BACAP static certification |
| B9 | Regression suite |
| B10 | Runtime smoke |
| B11 | Manual new/changed advancement review |
| B12 | Terminal reconciliation |
| B13 | Milestone commit |

The roadmap records the accepted sequence. B0 authorizes none of these later-stage operations.

