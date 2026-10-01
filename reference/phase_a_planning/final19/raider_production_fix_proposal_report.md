**FINAL19_RAIDER_PRODUCTION_FIX_PROPOSAL**

Families 1–5 are durably GREEN. Product state is 1150/1152; historical Phase A remains 1133/1152. This proposal is unapplied and requires separate explicit authorization. It provides no certification gain or post-fix native runtime proof.

Proposed production path: `src/main/java/com/diskree/achievetodo/client/ExternalPackCompatibility.java` only.

Patch: `reference/phase_a_planning/final19/raider_production_fix_proposal.patch`.

Patch SHA-256: `518EBAAE8587B6276610E02473FAC985C12CD9431CF3C344BD31C08F48DDBDBE`.

Current production SHA-256: `DFFEB7C2CF6685C5F689CF2A205B435B81DA5B524971103804DE3E3A6AE1DC11`.

Proposed production SHA-256: `1BEA3BA9CAF22963D1EF26DE69B19534A4B16D1EAB85A39C5E1FC17750227A31`.

The existing raider transform currently renames `is_captain`/`has_raid` to ignored camelCase JSON keys. The real 26.2 RaiderPredicate codec reads snake_case and defaults omitted booleans to false. The proposed patch preserves those booleans within the existing transform. Its outer type-specific migration and unrelated transforms retain their current behavior.

The proposal also writes and requires `raiderPredicateKeys=snake_case` for BACAP compatible copies. This rejects pre-fix r15 copies while retaining `compat_26_2_r15`, source SHA-1, root override SHA-1 and `llamaCarpetNbtMapping=equipment.body`. Other external-pack freshness behavior remains source-equivalent.

The isolated candidate compiled through current FINAL19 authoritative routing. Five JUnit tests passed with zero failures/errors/skips. A child-first classloader loaded only the proposed converter and nested classes. Production converter bytes and active runtime packs were not changed. Read-only `git apply --check` passed.

All 1229 frozen advancement definitions were converted with both current and proposed implementations, including every one of the 1152 canonical advancements. Output byte hashes are recorded individually in `raider_proposal_output_scope.json`. Exactly these three definitions differ:

| Definition | Required preserved semantics | Gain treatment after future proof |
| --- | --- | --- |
| `minecraft:adventure/voluntary_exile` | `is_captain=true`, omitted `has_raid=false` | One unresolved target |
| `blazeandcave:adventure/feeling_ill` | Five `has_raid=true` predicates | Mandatory zero-gain certified regression |
| `blazeandcave:monsters/dungeon_crawler` | Three explicit `has_raid=false` predicates | Mandatory zero-gain certified regression |

All nine raider predicates decode to the expected booleans. Six previously incorrect decoded predicates are corrected: voluntary_exile and all five feeling_ill branches. Explicit false and omitted false remain false. Every candidate converted definition is idempotent. Unrelated output is byte-identical, including the accepted llama migration. Marker reader and writer tests reject stale, wrong-source, wrong-root and wrong-llama candidates and preserve unrelated-pack behavior.

After separate authorization, the application must verify the exact patch and current production pre-hash before applying. Preservation must be extended narrowly to this exact converter post-hash and authorization record. Accepted families 1–5 keep their existing receipt bytes and original provenance; the exhaustive unchanged-output inventory supplies the semantic bridge for their converter fingerprint transition. Fresh raider evidence must use rebuilt runtime classes and a newly validated compatible copy carrying the proposed marker property.

The future native contract requires a fresh joined, client-loaded finite SURVIVAL actor and an ordinary player-attributed kill of a no-active-raid patrol captain with both the patrol-leader flag and exact native ominous head banner. Required negatives include ordinary noncaptain, ominous banner without patrol-leader flag, active-raid captain and outsider entity tag. Bad Omen effects, bottle use and manual criterion grants cannot substitute for the frozen kill predicate.

The future acceptance surface must independently cover all five feeling_ill raid/nonraid branches and all three dungeon_crawler false-raid branches with zero gain, plus freshness rejection of old r15 copies, exact all-output scope, native cleanup and artifact-local persistent integrity. Then run fresh compile/static gates, canary, actual TEMP/run-state audit, exact native proof, actual exact audit, independent validation, persistent promotion, local ledger/state delta, persistent regression, seal and checkpoint. Family 7 remains unstarted.

Validation inventory: `reference/phase_a_planning/final19/raider_proposal_validation/gate_inventory.json`. Exact metadata: `raider_production_fix_proposal.json`. The authorized axe and cauldron fixes remain byte-identical to their accepted post-hashes.
