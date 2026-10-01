# Phase A durable checkpoint — production authorization required

This is a recovery checkpoint, not terminal actionable-frontier exhaustion. The previous credit interruption was not a project failure. Four fronts completed in this continuation and remain CLOSED/GREEN. Do not repeat their GameTests.

## Accepted state

Canonical total 1152; STATIC_CERTIFIED 958; RUNTIME_CERTIFIED 158; RUNTIME_PARTIAL 1; RUNTIME_DEFERRED 18; STATIC_FAIL_UNRESOLVED 17. Total certified 1116; unfinished 36; unresolvedOrDeferred 35; strengthened by runtime 159; unique runtime touched 166; static certified with runtime evidence 6.

Canonical SHA-256: `6952ECCB601F2248EA4E9314BAB0DD4F56A310C4FBDD342BC595BDA1DC64D960`.

Frontier SHA-256: `DCC693143FF1F489B38BDD3E6DF9DA245C643BDFD9C441FD45645623C26B2B1B`.

Closed accepted exact run IDs:

- ANCIENT_RESTORATION: `2fb90835-5aae-4b4c-8474-46bb1630f463`, one receipt.
- SILK_TOUCH_NEST: `db91b817-9578-4f66-8412-9e81e6048db4`, one receipt.
- PURE_ENCHANTMENT_INVENTORY_CHANGED: `99da1422-4bb8-4ac9-b8d9-3b21a83869ec`, 211 receipts.
- PLAYER_KILLED_ENTITY_REMAINDER: `9531d7d8-b8b8-4411-8e82-50aae9b28df1`, 43 receipts.

Every closed transaction passed exact selector/count, successful authoritative Gradle exit 0, complete reports, current fingerprint, common run ID/run-state, exact key set, zero cleanup/network debt, persistent promotion, independent artifact-local validation, canonical local delta, frontier local delta, SKILL local delta and narrow validation. Historical roll-up fixtures exclude these new families while retaining accepted historical totals. Historical SKILL accepted records remain unchanged.

The latest combined checkpoint gate passed: `build/tmp/phase_a_resume_20260930/pending_bug_checkpoint_validation.gradle.log`. The separate production regression is intentionally RED pending authorization; its red log is retained and must not be mistaken for a canonical evidence failure.

## Current unfinished transaction

Current operational family is `LOCATION_HOLDERSET_WORLDGEN`, still OPEN. Its draft source and catalog cover 13 advancements, 94 criteria, 93 frozen requirement groups. Catalog SHA-256: `9800D8314661E2D0A4656E0943CA4587B0F53F16C144145F0C314E8A24CA84EA`.

Compilation and frozen-catalog JUnit passed. The five-case canary failed at `llama_festival#white_carpet` before producing a receipt. No exact location run or persistent location promotion has occurred. Existing location TEMP is non-promotable. Do not accept it or label this family CLOSED.

### Proven production compatibility defect

Frozen `llama_festival` contains sixteen vehicle predicates such as:

`{body_armor_item:{id:"minecraft:white_carpet"}}`

Current production `ExternalPackCompatibility.convertJson` preserves this SNBT. Official Minecraft 26.2 `LivingEntity.addAdditionalSaveData` writes `EntityEquipment.CODEC` under `equipment`; `NbtPredicate.getEntityTagToCompare` uses native `saveWithoutId`. A valid live llama wearing BODY white carpet satisfies the modern predicate:

`{equipment:{body:{id:"minecraft:white_carpet"}}}`

The minimal joined SURVIVAL runtime diagnostic selected exactly one test, completed successfully and exited 0 with BUILD SUCCESSFUL. It observed after 60 ordinary server ticks:

`tagMatched=true bodyWhiteCarpet=true stillMounted=true legacyNbtMatched=false currentEquipmentNbtMatched=true criterionAfter60NativeTicks=false`

This rules out wrong entity tag, wrong equipment slot, failed mounting and insufficient periodic ticks. No custom entity, serialization override, direct criterion trigger, manual award or TEMP promotion was used. Player/connection/channel cleanup completed without network warnings.

Proof log: `build/tmp/phase_a_resume_20260930/location_llama_diagnosis_fixed.gradle.log`.

Proof report: `build/tmp/phase_a_resume_20260930/location_llama_diagnosis_fixed.xml`.

Authoritative diagnostic selector: `achievetodo-test:phase_alocation_holder_set_worldgen_game_test_location_holder_set_worldgen_llama_nbt_diagnosis`.

Red-capable production regression: `src/test/java/com/diskree/achievetodo/client/PhaseALlamaFestivalNbtRegressionTest.java`; invocation `gradlew.bat --no-daemon test --tests '*PhaseALlamaFestivalNbtRegressionTest'` with the accepted approval-capable environment. The test ran and failed specifically because production conversion returns the legacy path instead of the native equipment/body path. Red log: `build/tmp/phase_a_resume_20260930/llama_nbt_regression_red.gradle.log`.

## Reviewable proposed fix — unapplied

`reference/phase_a_planning/llama_festival_nbt_26_2_fix_proposal.patch` contains a proposed production patch. It recognizes only the exact frozen sixteen-color carpet SNBT shape attached to `#blazeandcave:llamas`, maps the same item constraint to `equipment.body`, and leaves arbitrary SNBT untouched. It adds a BACAP-specific marker property so existing converted world copies with the stale mapping are regenerated from the pinned source. Compatibility version stays `compat_26_2_r15`; frozen BACAP stays untouched. The proposal has not been applied or represented as compiled/verified production code.

Authorization is required by the user's explicit `No unauthorized production edits` rule and allowed interruption category `proven production bug requiring authorization`. This is a newly proven defect in the current location front, not an explanation for the previous credit interruption, a frontier-map invalidation, or an architectural/tooling failure.

After authorization: review/apply the narrow fix; verify red regression turns green, scope guards/idempotence/cache refresh and unrelated converted predicates; reconcile the location harness observer to the legitimate production mapping; run fresh canary/exact93 and finish the complete atomic location transaction. Continue the remaining current OPEN mixed families and inventory partial backlog under SKILL/user policy. Do not reopen any closed family or protected voluntary_exile/deflect_arrow control. Do not begin Project 2/Purpur.

## Safety reconciliation at this checkpoint

Frozen BACAP SHA-256 remains `8C72314535C5DF7B4416BF0F38310371EC8AEC537FDE1445BDC820A3C9AADA70`.

Current production `ExternalPackCompatibility.java` SHA-256 remains the entry-state `0A339FC81B31EBFF129914BE40141155D4C8E474D5FD66DBF649A8CCAE7A625F`; production `fabric.mod.json` remains `408C0AA567715089FEBC57A84500FFCB6D3A8E5F39313654D6980795AD39CA88`.

All 59 original accepted catalog/evidence inventory files retain their entry-state hashes; only the original unaccepted ancient-restoration draft catalog changed after independent semantic reconciliation. Maximum resistance, miracle drink and thanks a lotl accepted artifacts were preserved. No production edit or Git mutation occurred. No Gradle clean, cache deletion, process kill, ACL change or sandbox reconfiguration occurred. Command-line inspection found no competing Java/Gradle/GameTest process at the checkpoint.

Frontier active singleton membership is empty; closed maximum_resistance and miracle_drink were removed during the authorized local delta. Stale current-state canonical prose was corrected while historical SHA records were preserved. Frontier requirement accounting now explicitly describes CURRENT_UNFINISHED_MAP_ENTRIES, derived from current canonical and existing map metadata without a source-wide re-audit.
