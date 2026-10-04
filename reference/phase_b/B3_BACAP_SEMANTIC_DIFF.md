# B3 — Complete BACAP Semantic Diff

Status: **PHASE_B_B3_DIFFED**. Analysis only; B4 has not started.

## 1. Exact input identities

- old: BACAP 1.18.1, reference/phase_a_preservation/files/final/bacap.zip; SHA256 8c72314535c5df7b4416bf0f38310371ec8aec537fde1445bdc820a3c9aada70.
- target: BACAP 1.21, build/tmp/phase_b_b1/BlazeandCave's Advancements Pack 1.21.zip; SHA256 c71d1aa1a84dbe00a3f85a42144b46214c4669a3cccf07ff66631d28f16a99b2.

Target Modrinth version Y2zZ5eSs. All eight B0/B1/B2 records are preserved. The tool is read-only and writes only evidence/ignored probe outputs. Reports contain identities/hashes/structural facts, not upstream resource bodies.

Reproduce from the repository root using Python and the existing cached JDK/Minecraft/Gson:

    python tools/phase_b/b3_bacap_semantic_diff.py --old-zip reference/phase_a_preservation/files/final/bacap.zip --target-zip "build/tmp/phase_b_b1/BlazeandCave's Advancements Pack 1.21.zip" --b1-inventory reference/phase_b/b1_target_inventory.json --b2-map reference/phase_b/b2_atd_integration_map.json --b2-occurrences reference/phase_b/b2_bacap_occurrences.json --output-dir build/tmp/phase_b_b3/reproduction --temp-dir build/tmp/phase_b_b3/reproduction_probe

Output directories must not already contain these reports. No network/Gradle/commands in Minecraft are executed. Object keys normalize; all arrays preserve order. Separate requirement fingerprints canonicalize AND-of-OR logic. Two old advancement JSON files and one old predicate JSON file contain non-strict backslash-apostrophe escapes; a narrowly recorded Gson-compatible repair is applied only to parsed comparison, with original byte hashes preserved.

## 2. Advancement ID sets

| Classification | Count |
|---|---:|
| common | 1222 |
| oldOnly | 7 |
| targetOnly | 110 |
| BYTE_IDENTICAL | 594 |
| SEMANTICALLY_CHANGED | 624 |
| SEMANTICALLY_IDENTICAL_BYTES_DIFFER | 4 |

1222+7=1229 old;1222+110=1332 target. Every common ID has one exclusive byte/semantic classification; one record per union ID (1339).

## 3. Canonical reconciliation

1152 − 7 removed − 0 ceased + 0 became + 97 added =1242. Canonical in both: 1145. No rename inference enters this arithmetic. +90 is net only;110 IDs were added overall and97 of them are canonical.

## 4. Same-ID semantic changes

624 common advancements differ in parsed JSON. Per-ID hashes, changed fields, criterion/trigger/condition identifiers, requirement groups, reward-path facts and tree metadata are in b3_advancement_diff.json. Literal text is represented by fingerprints; translatable keys are preserved as identifiers.

## 5. Rename/move candidates

- CONFIRMED_RENAME_OR_MOVE: 4.
- HIGH_CONFIDENCE_CANDIDATE: 3.
- unmatchedOldOnly: 0.
- unmatchedTargetOnly: 103.

Confirmed pairs require named official changelog evidence from the B1-retained selected-version snapshot, plus structural signals. Hash/name/criteria similarities alone remain candidate evidence, not forced one-to-one mapping. All old-only and target-only entries have a candidate or NO_MATCH record. Old Adventure spear_fishing must not be confused with the new Weaponry advancement sharing its basename.

- blazeandcave:adventure/spear_fishing -> blazeandcave:adventure/seafood_skewer: CONFIRMED_RENAME_OR_MOVE.
- blazeandcave:animal/birdkeeper -> blazeandcave:biomes/birdkeeper: CONFIRMED_RENAME_OR_MOVE.
- blazeandcave:animal/chatterbox -> blazeandcave:biomes/chatterbox: CONFIRMED_RENAME_OR_MOVE.
- blazeandcave:nether/get_a_lode_of_this -> blazeandcave:mining/get_a_lode_of_this: HIGH_CONFIDENCE_CANDIDATE.
- blazeandcave:nether/lodes_of_applications -> blazeandcave:mining/lodes_of_applications: HIGH_CONFIDENCE_CANDIDATE.
- blazeandcave:statistics/two_by_two -> blazeandcave:statistics/overpopulation: CONFIRMED_RENAME_OR_MOVE.
- minecraft:nether/use_lodestone -> minecraft:adventure/use_lodestone: HIGH_CONFIDENCE_CANDIDATE.

## 6. Tree/category relationships

No internal target missing parents or cycles. Parent changes: 34. Root/category/depth transitions are recorded per ID. Seventeen ATD categories, three ordered parent maps and all28ordered child IDs are accounted. Inherited animal/HUSBANDRY and technical path exceptions are distinguished from new target facts; unknown-category fallback is recorded without GUI edits.

## 7. Criteria/requirements/rewards fields

| Field | Changed common IDs |
|---|---:|
| parent | 34 |
| display | 55 |
| criteria | 574 |
| requirements | 25 |
| rewards | 27 |
| sends_telemetry_event | 0 |
| otherTopLevelFields | 0 |

Effective defaulted AND-of-OR grouping changes: 106. This includes criteria-driven default groups even when an explicit requirements field did not change. Raw array ordering remains separately fingerprinted; trigger/condition changes affect completion independently.

## 8. Resource paths and static graphs

| Resource type | Identical | Changed | Removed | Added |
|---|---:|---:|---:|---:|
| function | 2281 | 2625 | 102 | 530 |
| predicate | 10 | 2 | 5 | 46 |
| tags | 52 | 5 | 16 | 25 |
| dialog | 0 | 0 | 0 | 7 |

Pack metadata changes from old pack_format61 to target min/max[107,1]; descriptions are hash-only. Dialog action references: 7. Relocation candidates: 2731. Matching text/hash is evidence of possible relocation, not proof of semantic equivalence. Function graph masks quoted chat text, recognizes direct/execute/schedule and tag calls, and binds literal macro arguments; runtime-storage-derived calls remain dynamic. Target graph summary: {"aggregatedEdges": 7970, "callOccurrences": 93472, "conditionalMacroAbsentOccurrences": 19350, "conditionalMacroAbsentUniqueCallees": 1075, "directStaticCallOccurrences": 1711, "fanpackExtensionHooks": 1258, "functionTags": 25, "functions": 5436, "literalMacroArgumentContexts": 1256, "macroCommandLines": 165, "missingStaticOccurrences": 14, "missingStaticUniqueCallees": 14, "reachableNodes": 7800, "resolvedMacroCallOccurrences": 91688, "unresolvedDynamicPatterns": 5, "unresolvedDynamicSites": 73}.

Undefined fanpack extension hooks, conditional macro reward paths, companion hooks, stale trophy-category calls and potential upstream dangling calls remain separately visible for B4. Missing macro expansions are guard-dependent static review items, not established runtime errors. Registry tags are distinct from predicate/function files; vanilla registry existence is not inferred from archive absence. See b3_resource_graph_diff.json for all paths, edges, memberships, missing refs and resolved macro contexts.

## 9. Reward-message/override impact

All1187override messages accounted:1148main+39companion. Main target paths present:1139, absent:9. Target message resources without base ATD override:117. Removed Search IDs:7. Per-file keys/hash/highlight impacts are recorded. Companion-only main-archive absence is not a compatibility verdict. All17root/milestone wrappers and10setup/configuration files are accounted.

## 10. Tracker/GUI/Search impact

All86tracker bindings retained unchanged; 54 need B4 review by static evidence/special scope. ON_A_RAIL and HALF_HEART_LIFE are explicitly flagged. Criteria/stat/entity/goal/objective relevance is recorded; no numerical edits/rebalance. All28ordered GUI children and every B2 production PATH_SENSITIVE occurrence are accounted individually: 4409. Generic selectors/namespaces are distinguished from mapped resources and explicit unresolved B4 items.

## 11. Scoreboard/configuration contracts

Six original raw-count objectives retain their definitions. Defined objectives87->110,25added/2removed (bac_apple_eaten,bac_1000th_item). Weighted bac_advancements_points/bac_advancements_team_points are separate. Shared advancement_made_macro wraps1256rewards and gates raw increments by bac_dont_count; hidden tier defaults excluded. New point defaults are absent from ATD's old new_world override. Raw score_add is byte-identical, but that does not prove identical behavior through new macro wrappers. update_score substantive body relocates to blazeandcave:config/update_score with retained old delegate; selector counts1178->1268 are reconstruction contracts, not canonical counts. Do not replace raw ATD progression with weighted points.

## 12. Actual converter target-input probe

The unchanged published Java converter/legacy shims are compiled from byte-identical temporary copies against cached Minecraft/Gson; diagnostic instrumentation is a separate copy cross-checked against authoritative output. Conversion runs only on ignored archive copies; B1 ZIP stays unchanged. Source/harness/dependency hashes and per-family paths/counts are recorded. Candidate runtime adapter visits are not automatically obsolete structures; family attribution includes nested effects.

Target actual summary: {"actualWorldCopyPayloadMismatchPaths": [], "actualWorldCopyPayloadMismatches": 0, "changedFunctionFiles": 35, "changedJsonFiles": 1316, "conversionExceptions": 0, "diagnosticDisagreements": 0, "functionFiles": 5436, "idempotenceFailures": 0, "itemSnbtParseFailureFiles": 0, "jsonFiles": 1479, "parseFailures": 0, "runtimeChatCandidateFiles": 1304, "runtimeChatChangedFiles": 0, "runtimeChatJsonParseFailureFiles": 0, "semanticChangedJsonFiles": 1246, "serializationOnlyJsonFiles": 70}.

| Converter family | Candidate files | Changed files |
|---|---:|---:|
| function_chain_id | 0 | 0 |
| function_daytime_query | 0 | 0 |
| function_dyed_color_with_tooltip | 0 | 0 |
| function_enchantments_levels | 0 | 0 |
| function_enchantments_with_tooltip | 0 | 0 |
| function_gamerule | 34 | 0 |
| function_hide_additional_tooltip | 0 | 0 |
| function_legacy_item_text | 235 | 0 |
| function_stored_enchantments_levels | 0 | 0 |
| function_stored_enchantments_with_tooltip | 0 | 0 |
| function_trim_with_tooltip | 0 | 0 |
| function_unbreakable_with_tooltip | 35 | 35 |
| item_snbt_parse | 235 | 0 |
| item_snbt_parse_failure | 0 | 0 |
| json_background | 16 | 0 |
| json_chain_id | 1479 | 0 |
| json_context_aware_predicate | 214 | 8 |
| json_damage | 54 | 0 |
| json_damage_type | 36 | 0 |
| json_enchantment_predicates | 55 | 55 |
| json_entity_predicate | 393 | 9 |
| json_entity_struck | 0 | 0 |
| json_entity_tags | 332 | 0 |
| json_entity_type | 0 | 0 |
| json_entity_variant_components | 0 | 0 |
| json_generic_type_specific | 0 | 0 |
| json_killing_blow | 40 | 0 |
| json_lightning_type_specific | 0 | 0 |
| json_llama_carpet_nbt | 0 | 0 |
| json_player_type_specific | 0 | 0 |
| json_raider_type_specific | 0 | 0 |
| json_vanilla_announcement_suppression | 1479 | 1244 |
| runtime_chat_json_parse | 1255 | 0 |
| runtime_chat_json_parse_failure | 0 | 0 |

Modern no-op/serialization-only matches, actual parsed changes, idempotence, parse failures and static unsupported-construct candidates are distinguished. Probe metadata stubs supply historical pin/enum values only; temporary marker is not a newly accepted production pin. No marker bump or converter implementation change is made.

## 13. Concrete B4 review queue

- **B4-MAIN-PIN** (PIN_UPDATE_REQUIRED): Update main official download/source identities through original enum/acquisition mechanism after B4 approval; content update alone no marker bump.
- **B4-MESSAGES** (OVERRIDE_RESOURCE_UPDATE_REQUIRED): Review all missing/renamed/changed main message keys and layout; companion messages remain separately unresolved.
- **B4-SEARCH** (SEARCH_LINK_UPDATE_REQUIRED): Follow official new IDs and conservative rename evidence, without progress migration.
- **B4-PACK-METADATA** (OVERRIDE_RESOURCE_UPDATE_REQUIRED): Review bundled override metadata against26.2 compatibility ranges; source pack is now modern. No automatic converter marker revision follows metadata/content version.
- **B4-ROOT-MACROS** (OVERRIDE_RESOURCE_UPDATE_REQUIRED): Old root/milestone wrappers shadow new macro/reward/count/points behavior; preserve original ATD layering while matching necessary upstream contracts.
- **B4-TRACKERS** (TRACKER_REVIEW_REQUIRED): Review changed criteria/goals/objective contracts, including removed bac_apple_eaten/bac_1000th_item and ON_A_RAIL/HALF_HEART_LIFE. No edits to values in B3.
- **B4-TREE** (TREE_GUI_REVIEW_REQUIRED): Review ordered child parents/categories/path assumptions and unknown-category fallback only where target requires.
- **B4-CONFIG** (SETUP_SCOREBOARD_REVIEW_REQUIRED): Review new point/tier defaults omitted by ATD setup, bac_dont_count initialization, macro wrappers, relocated update_score; retain raw-count selection.
- **B4-CONVERTER** (CONVERTER_REVIEW_REQUIRED): Review actual modern input rewrites, serialization-only positives and remaining narrow compatibility scope; do not infer marker change from version.
- **B4-UPSTREAM-DANGLING** (UNKNOWN_REQUIRES_B4): Determine reachability/required narrow compatibility handling for upstream stale trophy paths and advancement/loser_hurt; Terralith hooks are separate companion concerns.
- **B4-MACRO-OPTIONAL-RESOURCES** (UNKNOWN_REQUIRES_B4): Shared macro refers conditionally to reward/exp/trophy resources absent for some advancements. Distinguish guard/no-content conventions and runtime behavior from direct stale paths; do not blanket-create reward files.
- **B4-FANPACK** (COMPANION_REVIEW_REQUIRED): Document extension hooks and companion requirements; no companion pin resolution here.
- **B4-RU** (LOCALIZATION_REVIEW_REQUIRED): B6 research all usable Russian sources/permissions and fill native keys; no new translation architecture.
- **B4-CERT** (TEST_SUCCESSOR_REQUIRED): Create Phase B successors; never replace historical Phase A/FINAL19/publication oracles.
- **B4-PREDICATE-TAG-CONTRACTS** (UNKNOWN_REQUIRES_B4): Review truly missing pack predicates or formerly pack-owned tags; vanilla registry tags are not datapack dangling by default.

Localization input keys are inventoried without source research/merge; companions remain unresolved and no new pack was downloaded. Historical tests/publication snapshots require Phase B successors rather than rewritten Phase A/FINAL19 evidence.

## 14. Phase C/D deferrals and safety

Changing ability thresholds,1152custom-threshold clamp,balance/pacing or terminal progression remains DEFER_TO_PHASE_C_OR_D. No migration of old completed progress is required.

Branch phase-b-bacap-26.2 and HEAD b261b02cd03b4aeae2835c63f982c9aa53c46eed remain unchanged. Product Java/resources/localization/tests,archives,all eight B0/B1/B2 records,compat_26_2_r16 and production pins unchanged. No companion pin/download,commit,push,tag,GitHub mutation or B4 execution. Only one analysis tool and four B3 durable reports are added; probes are ignored temporary files.

**PHASE_B_B3_DIFFED**
