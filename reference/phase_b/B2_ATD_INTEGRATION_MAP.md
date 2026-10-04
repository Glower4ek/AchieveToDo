# B2 — Original ATD BACAP Integration Map

Status: **PHASE_B_B2_MAPPED**. Branch: phase-b-bacap-26.2. Published HEAD: b261b02cd03b4aeae2835c63f982c9aa53c46eed.

This is a local source/history and dependency inventory. Original AchieveToDo architecture remains authoritative. No product change, upstream old-to-new semantic diff, translation-source research, new download, companion pin, or test execution occurred.

The integration JSON contains the complete bundled-file manifest, eight current external pins, 86 tracker bindings, acquisition nodes, original/current delta, provenance and 340 test/tool roles. The occurrence JSON provides source locations and original-reference presence/change flags. These source flags do not establish a change's motive or semantic importance.

## 1. Original architecture reference

Accepted original ref: pre-26.2-port, also local branch 1.21.4+, commit e8095a2fb386c510b39c0dfd35071f6153480c50.

Existing local refs agree. Its gradle.properties pins Minecraft1.21.4, ATD0.1.5, Java21, mappings8, Loom1.9 and Fabric API0.115.0. It is an ancestor of published v0.1.5.4. Actual Git source objects were inspected with show, in-memory archive, diff and ls-tree without checking out that ref. This preserved pre-port tag is the accepted original-design reference. The frozen Phase A archive separately establishes old BACAP content.

## 2. Acquisition flow

Inherited flow: enum source pin -> download/verification -> global external archive -> world datapack copy -> external/internal pack enablement. Phase A added conversion/freshness at the copy and join boundary.

- **pin**: [src/main/java/com/diskree/achievetodo/client/ExternalPack.java](../../src/main/java/com/diskree/achievetodo/client/ExternalPack.java); `BACAP / companion enum pins; getFileName/getDatapackName`. Original: Same enum pins/filename convention. URLs/SHA1 identities need B4 target review; all eight published pins unchanged from original; Formatting API only changed.
- **download**: [src/main/java/com/diskree/achievetodo/client/gui/ExternalPackDownloader.java](../../src/main/java/com/diskree/achievetodo/client/gui/ExternalPackDownloader.java); `startDownload; handleDatapackFile`. Original: Same automatic/manual/wrapper downloader. gameDir/.datapacks_temp/UUID -> gameDir/datapacks/<enum>.zip; SHA1 check and optional wrapper unpack; native client/I/O API port; wrapper layout assumption needs B4.
- **verification**: [src/main/java/com/diskree/achievetodo/client/ExternalPackCompatibility.java](../../src/main/java/com/diskree/achievetodo/client/ExternalPackCompatibility.java); `isPinnedHistoricalSource; isCurrentWorldPack`. Original: Downloader SHA1 check existed; revalidation at copy/join did not. Source pin change is independent of compatibility conversion revision.
- **requiredSelection**: [src/main/java/com/diskree/achievetodo/injection/mixin/client/CreateWorldScreenMixin.java](../../src/main/java/com/diskree/achievetodo/injection/mixin/client/CreateWorldScreenMixin.java); `prepareDatapacks`. Original: Same main/hardcore/worldgen required list. Main always, hardcore adds companion, terrain toggle adds worldgen+variant; current verifies source/freshness beyond old existence check.
- **worldCopy**: [src/main/java/com/diskree/achievetodo/client/ExternalPackCompatibility.java](../../src/main/java/com/diskree/achievetodo/client/ExternalPackCompatibility.java); `copyForWorld`. Original: CreateWorldScreenMixin directly Files.copy global ZIP to world datapack temp. Same copy boundary now transforms JSON/functions; otherwise bytes pass; no root advancement injection.
- **internalSelection**: [src/main/java/com/diskree/achievetodo/injection/mixin/client/CreateWorldScreenMixin.java](../../src/main/java/com/diskree/achievetodo/injection/mixin/client/CreateWorldScreenMixin.java); `prepareDatapacks pack enablement`. Original: Same external then builtin override/config pack scheme. Enable main+override, hardcore+override, terrain+variant+override, reward/coop toggles; review overlap/order after B3.
- **builtinRegistration**: [src/main/java/com/diskree/achievetodo/server/AchieveToDoServer.java](../../src/main/java/com/diskree/achievetodo/server/AchieveToDoServer.java); `registerInternalDataPacks -> ResourceManagerHelper.registerBuiltinResourcePack`. Original: Same nine enum-defined builtin packs. Native Fabric API adaptation; content stays in existing resourcepack paths.
- **worldFreshness**: [src/main/java/com/diskree/achievetodo/client/ExternalPackCompatibility.java](../../src/main/java/com/diskree/achievetodo/client/ExternalPackCompatibility.java); `isCompatibleWorldCopy; currentRootOverrideSha1; ensureWorldPacksUpToDate`. Original: None; old copy had no conversion marker. r16 + filename + sourceSha1 + 15-root byte digest; special main checks; update only verified global source.
- **join**: [src/main/java/com/diskree/achievetodo/injection/mixin/client/WorldListWidgetMixin.java](../../src/main/java/com/diskree/achievetodo/injection/mixin/client/WorldListWidgetMixin.java); `required global-pack download; sync before prepared join`. Original: Existing pack-availability check; no converted-world update. One-shot CompatibilityJoinGate prevents recursion; no advancement-progress migration.
- **open**: [src/main/java/com/diskree/achievetodo/injection/mixin/client/WorldOpenFlowsMixin.java](../../src/main/java/com/diskree/achievetodo/injection/mixin/client/WorldOpenFlowsMixin.java); `requiresHistoricalCompatibility; ensureWorldPacksUpToDate before open`. Original: Original saved ATD configuration recognized; no conversion layer. level.dat Data configuration-name nonempty identifies ATD world; block missing/corrupt required source instead of use stale bytes.
- **createContinuation**: [src/main/java/com/diskree/achievetodo/client/CreateWorldContinuationGate.java](../../src/main/java/com/diskree/achievetodo/client/CreateWorldContinuationGate.java); `user initiated creation continuation`. Original: Prior waiting-datapack boolean/callback. Port-specific async continuation guard; generic content mechanism.
- **visibility**: [src/main/java/com/diskree/achievetodo/injection/mixin/client/PackScreenMixin.java](../../src/main/java/com/diskree/achievetodo/injection/mixin/client/PackScreenMixin.java); `external/internal pack list filtering`. Original: Same filename/enum filtering. Generic UI mechanism should survive pin change.


All eight production URL/hash pins remain identical to original ATD; main still selects BACAP1.18.1. B1 metadata has not changed runtime. Automatic downloads go to gameDir/.datapacks_temp/<UUID>, then verified enum-named archives in gameDir/datapacks. Main/Hardcore and terrain packs support automatic acquisition; three BACAP terrain variants use manual MediaFire wrappers with separate wrapper/inner SHA1 pins. Downloader validates the supplied wrapper or direct archive; it does not rehash the extracted first file inside that method. Current preparation/join source checks validate the resulting inner archive. Wrapper extraction layout needs later review.

Main is always required; hardcore adds its companion; terrain toggles add both terrain and BACAP variant. External packs are enabled before builtin override/config layers, followed by optional reward/coop switches. Global source archives remain separate from derived world copies. Existing-world pack synchronization changes archive files, not completed advancement IDs.

Source URL/hash changes and conversion revision changes are distinct. A new BACAP version alone does not justify changing compat_26_2_r16.

## 3. Product resourcepacks

Nine packs, all inherited: 1,223 files =1,214functions+9manifests. All manifests retain pack_format61; review required target compatibility during B4. There are no bundled advancement JSON overrides. Category-root overrides here are reward functions.

| Pack | Files | Function groups |
|---|---:|---|
| `bacap_amplified_nether_override` | 2 | reward messages localization: 1, pack metadata: 1 |
| `bacap_cooperative_mode` | 2 | cooperative setup: 1, pack metadata: 1 |
| `bacap_hardcore_override` | 12 | reward messages localization: 11, pack metadata: 1 |
| `bacap_nullscape_override` | 3 | reward wrapper: 1, reward messages localization: 1, pack metadata: 1 |
| `bacap_override` | 1171 | root category or milestone reward: 16, reward messages localization: 1148, configuration setup: 6, pack metadata: 1 |
| `bacap_rewards_experience` | 2 | reward option setup: 1, pack metadata: 1 |
| `bacap_rewards_item` | 2 | reward option setup: 1, pack metadata: 1 |
| `bacap_rewards_trophy` | 2 | reward option setup: 1, pack metadata: 1 |
| `bacap_terralith_override` | 27 | reward messages localization: 26, pack metadata: 1 |


Aggregate: 1,187 translated reward messages (1,148main+39companion),17wrappers (15category roots+advancement_legend+Nullscape desolation),10setup/configuration functions,9manifests. Full exact paths/function groups/SHA256/original presence are in the JSON manifest.

Main functions under resourcepacks/bacap_override/data/blazeandcave/function are new_world, setup_item_rewards, setup_experience_rewards, setup_trophy_rewards, setup_cooperative_mode and config/update_number_format. Defaults set reward/exp/trophy/coop to0 on bac_settings; optional packs replace switches with1. Setup also touches adv_score/extra_reward/extra_trophy/intro_msg, bac_created/global_install, and bac_advancements sidebar. Number-format setup styles advancement/first scoreboards; team-sum alternatives are commented.

Root/milestone wrappers implement cooperative bac_obtained/team checks and call upstream reward/message/experience/trophy paths. Messages contain title/description/category translations and Search links, including minecraft root IDs. Only new_world body changed versus original: three unsupported dimension advancement-announcement gamerules removed. These packs have no separate language dictionaries/predicates/tags. Two compatibility item tags exist outside them at data/minecraft/tags/item/nether_roots.json and nether_fungus.json.

## 4. Explicit BACAP identifiers

Occurrence records: **25926**. Conservative scope includes vanilla references and native ATD keys touching the same mechanisms. Counts are occurrences, not upstream changes or required edits.

| Classification | Occurrences |
|---|---:|
| CONTENT_ID_SENSITIVE | 14934 |
| STRUCTURAL | 6461 |
| VERSION_PIN | 28 |
| PATH_SENSITIVE | 4409 |
| COUNT_SENSITIVE | 90 |
| HISTORICAL_OR_TEST_ONLY | 4 |


Coverage:1,748tracked production text files of1,752total, plus relevant build bindings; four binary files are explicitly excluded. Detectors cover advancement/resource IDs, reward/fanpack functions/tags, scoreboard names/fake players, pins, embedded/generated/native translation keys, tab definitions, tracker parameters, all physical bundled files and compatibility tags. Historical trees use durable role summaries and a340-file roster rather than huge lexical dumps.

Each occurrence records path, line/column or resource location, token, namespace, primary sensitivity, production relevance, original token/path presence, containing-line change and future review stage. A changed line may be an API rename. Nearby-entity numeric parameters are radii; goals derive from entity sets/baby separation. The tracker JSON distinguishes these meanings. The90COUNT_SENSITIVE records are86tracker parameters+4old-total references; this is unrelated to the +90net canonical target count. No unresolved lexical sensitivity was forced into a content classification.

## 5. Advancement counting / abilities coupling

- [src/main/java/com/diskree/achievetodo/server/AchieveToDoServer.java:72](../../src/main/java/com/diskree/achievetodo/server/AchieveToDoServer.java#L72)
- [src/main/java/com/diskree/achievetodo/injection/mixin/main/ScoreboardMixin.java:43](../../src/main/java/com/diskree/achievetodo/injection/mixin/main/ScoreboardMixin.java#L43)
- [src/main/java/com/diskree/achievetodo/server/AchieveToDoServer.java:133](../../src/main/java/com/diskree/achievetodo/server/AchieveToDoServer.java#L133)
- [src/main/java/com/diskree/achievetodo/server/AchieveToDoServer.java:556](../../src/main/java/com/diskree/achievetodo/server/AchieveToDoServer.java#L556)


BACAP owns increments/rebuilding. Frozen getting_wood rewards call score_add and first_score_add; each adds1 to bac_advancements/bac_advfirst. update_score rebuilds online scores and calls #bacap_fanpacks:update_score. Canonical-visible certification counting is not a Java recount on every award.

ATD recognizes bac_advancements, bac_advfirst, bac_advancements_team, bac_advfirst_team_sum, bac_advfirst_sum, bac_advfirst_team. First two are non-team, remaining four team modes. Display selection prioritizes SIDEBAR,LIST,BELOW_NAME. No weighted-points objective is recognized.

Scoreboard callbacks feed raw obtained counts. Initial team synchronization sums member selected-objective scores; live holder callbacks update teammates from holder scores. Both paths are inherited and need characterization rather than redesign.

Raw obtainedCount>=configured literal threshold unlocks abilities; crossings grant/revoke demystified/unlocked criteria and synchronize clients. Zero/minus-one have special meanings. Runtime thresholds are in config/achievetodo/<name>.toml; CHAOS is seed-specific. All151abilities and original spacing remain: EASY600,NORMAL730,HARD860 maxima,CHAOS1000 cap.

Constants.TOTAL_ADVANCEMENTS_COUNT=1152 has one production consumer: LevelInfoMixin clamps custom/stored ability thresholds above1152. Changing it changes effective thresholds: **DEFER_TO_PHASE_C_OR_D**.

Generated ability JSON uses impossible demystified/unlocked criteria and same-ID reward functions; runtime thresholds are not embedded. GUI ability fractions use min(obtained,required)/required; tracker fractions use progress/finalValue and clamped percentages; unmatched native requirement progress remains. Hardcore death text uses obtained count. No pack-total fraction uses1152/1242.

There are31scoreboard+52statistic+3nearby tracker bindings, recorded completely with goals/objectives/stat/entity definitions and radii/baby separation. ON_A_RAIL coordinate and HALF_HEART_LIFE progress behavior require semantic review.

1242−1152=+90net canonical only, not exactly90 additions. Practical unlock pacing may change with content, but threshold/balance evaluation is deferred. Preset maxima were already below1152;1242 does not newly make them non-terminal.

## 6. Search / GUI coupling

- [src/main/java/com/diskree/achievetodo/client/AdvancementLinkCommand.java](../../src/main/java/com/diskree/achievetodo/client/AdvancementLinkCommand.java)
- [src/main/java/com/diskree/achievetodo/client/OptionalAdvancementSearch.java](../../src/main/java/com/diskree/achievetodo/client/OptionalAdvancementSearch.java)
- [src/main/java/com/diskree/achievetodo/client/gui/AdvancementsTabType.java](../../src/main/java/com/diskree/achievetodo/client/gui/AdvancementsTabType.java)
- [src/main/java/com/diskree/achievetodo/injection/mixin/main/PlacedAdvancementMixin.java](../../src/main/java/com/diskree/achievetodo/injection/mixin/main/PlacedAdvancementMixin.java)
- [src/main/java/com/diskree/achievetodo/injection/mixin/main/AdvancementDisplaysMixin.java](../../src/main/java/com/diskree/achievetodo/injection/mixin/main/AdvancementDisplaysMixin.java)
- [src/main/java/com/diskree/achievetodo/injection/mixin/client/AdvancementTabMixin.java](../../src/main/java/com/diskree/achievetodo/injection/mixin/client/AdvancementTabMixin.java)


Original click commands use /advancementssearch highlight ID obtained_status; server command errors inferred missing Search. Current client bridge parses ID, checks advancements_search, finds live client tree/widget/root, opens standard screen and selects actual root. Optional adapter delegates to Search1.3, reflecting stopFlashing and Identifier field flashingAdvancementId. Search owns its index/centering/flashing; ATD has no separate BACAP index.

Seventeen fixed categories set tab positions/order and locked_tab_<category>/root placeholders. Classification requires two path components, first matching enum ignoring case, without namespace discrimination. Three parent order maps contain28explicit children: statistics/root11, technical/you_are_a_big_cheater9, challenges/root8. Unlisted children retain index−1 ordering; other parents sort IDs. Visibility applies non-root/non-CHALLENGES hidden/done logic; ability tab hierarchy uses configured thresholds.

Unknown-category handling now leaves vanilla tab creation; original returned null. Behavior is proven; accepted-cause provenance was not located. Record for B4 relevance without reverting. Accepted early tutorial receipt is a separate generic screen-state extension.

## 7. Localization architecture

- [src/main/resources/assets/achievetodo/lang/en_us.json](../../src/main/resources/assets/achievetodo/lang/en_us.json): 533 authoritative runtime keys.
- [src/main/resources/assets/achievetodo/lang/ru_ru.json](../../src/main/resources/assets/achievetodo/lang/ru_ru.json): 533 authoritative runtime keys.
- [src/main/resources/assets/minecraft/lang/ru_ru.json](../../src/main/resources/assets/minecraft/lang/ru_ru.json): 3228 authoritative runtime keys.


Original native ATD EN/RU dictionaries had531keys each, with translatable components/embedded BACAP English keys. No Minecraft RU overlay or dictionary-merge tool existed. Ability generators generated gameplay resources/components, not a translation system.

Phase A added3228-key native Minecraft RU overlay and two diagnostic ATD keys (missing_required_pack,advancement_navigation_unavailable), now533each. Vanilla language resolution remains authoritative.

tools/generate_phase_a_production_ru.py is offline authoring: scans frozen BACAP/five overrides, merges retained official/manual/normalized mappings, writes native RU JSON. It is not invoked at runtime. Historical TSVs/caches/manifests/fixtures are tooling/evidence, not runtime outputs or an automatically reusable B6 pipeline.

licenses/bacap-better-ru/LICENSE and reference/localization preserve credits/provenance. Early CC0/manual records exclude ARR official language data; later full re-audit records owner permission through maintainer for LP1.18 and compatible1.21 fallback. Preserve stage-specific records; B6 must trace final attribution/permission and research all usable RU sources. B2 performed no translation research/merge/download. Existing BACAP1.21 localization fixture is historical publication evidence distinct from B1 target input.

## 8. Compatibility conversion

Current marker: `compat_26_2_r16`. Sources: [src/main/java/com/diskree/achievetodo/client/ExternalPackCompatibility.java:35](../../src/main/java/com/diskree/achievetodo/client/ExternalPackCompatibility.java#L35), [src/main/java/com/diskree/achievetodo/injection/mixin/main/CommandFunctionMixin.java:14](../../src/main/java/com/diskree/achievetodo/injection/mixin/main/CommandFunctionMixin.java#L14), [src/main/java/com/diskree/achievetodo/client/LegacyItemText.java](../../src/main/java/com/diskree/achievetodo/client/LegacyItemText.java), [src/main/java/com/diskree/achievetodo/client/LegacyChatText.java](../../src/main/java/com/diskree/achievetodo/client/LegacyChatText.java).



Original world copy was direct Files.copy. Phase A added a real conversion/freshness boundary, scanning JSON/functions and passing other entries; unchanged copies use original bytes, changed copies receive marker. It does not inject category advancement files.

Conversion families include contextual entity/damage/type-specific predicates, background/equipment/components, enchantment holders, lightning shape, chain IDs, tooltip/trim/enchantment commands, daytime/gamerules and legacy item text. Exact llama case is scoped to #blazeandcave:llamas and16legacy carpet SNBT strings. Accepted player/raider/cooperative-root fixes remain narrow historical behavior.

Marker checks revision,filename,sourceSha1,15bundled root path/byte digest; main adds equipment.body/snake_case assertions. Root digest is freshness, not injection. Parse-time chat migration targets bacap_rewards:msg/*,*/root, ATD functions and blazeandcave highlight lines.

B3/B4 must assess modern input safety/idempotence and any required narrow conversion adjustment. B2 assumes neither removal nor marker bump.

## 9. Test/certification coupling

Classification uses actual dependencies, not filenames or active source-set placement.

| Role | Files |
|---|---:|
| IMMUTABLE_HISTORICAL | 319 |
| ACTIVE_PRODUCT_TEST | 14 |
| ACTIVE_PUBLICATION_TEST | 7 |


The340-file roster explicitly corrects Certified catalogs, frozen GameTest setup, scope/regression fixtures to historical roles. Four generic structure/location probes are reusable without frozen catalogs. Active tests with historical subcases retain those assertions.

Immutable coupling includes old ZIP/hash;1229all/1152canonical/61helpers/15hidden/1root; exact IDs, ordered requirements/predicates; converter/source/resource hashes; historical r15 marker;1133/1152+19terminal frontier. Phase A/FINAL19 references and fixtures cannot become new target oracles by substituting counts.

Reusable tests cover converter scopes/idempotence, continuation/join guards, Search parsing, native overlays/tags, mixin config/tutorial actions. Existing215custom reward and1202message counts remain historical facts needing successors.

Publication verify protects303generated outputs and accepted snapshot. Retained reproduction records391tests/4known failures and24suites/103green; these are documentary facts, not B2 executions. Test-only r15 adapter does not change currentr16production.

**PHASE_B_NEW_TEST_NEEDED**:1332native/static parsing+1242canonical certification; ID/path/tree/tracker/reward consistency; RU coverage/provenance; modern converter scopes; fresh-world score/reward/Search/companion smoke; manual new/changed review. Stages B7–B11 after B3/B4.

## 10. Original 1.21.4 vs ported 26.2 architecture delta

Counts are disjoint focused mechanism units, not file/line counts. Secondary motives remain in JSON. Adjacent accepted wax-axe/cauldron action-gate fixes are historical context outside BACAP-focused units.

| Classification | Units |
|---|---:|
| PORT_REQUIRED | 7 |
| BUGFIX_REQUIRED | 6 |
| ARCHITECTURE_CHANGE | 4 |
| LOCALIZATION_PRESERVATION | 1 |
| PUBLICATION_ONLY | 2 |

| Unit | Classification | Difference |
|---|---|---|
| D01 | PORT_REQUIRED | Native acquisition, pack-registration, selection and callback API port. Minecraft/Fabric mappings and async lifecycle adaptation; original external/internal selection scheme retained. |
| D02 | PORT_REQUIRED | Native GUI/tree/statistic rendering and tracker API adaptation. Mappings/render pipeline/stat identifiers adapted; explicit tracker IDs/goals and ability thresholds retained. |
| D03 | PORT_REQUIRED | Historical JSON/function predicate, command and component codecs. Conversion implementation translates old BACAP engine schema to26.2; conversion seam itself counted separately D14. |
| D04 | PORT_REQUIRED | Legacy reward-item text/component command adapter. Preserves historical custom reward items under current components; accepted BUG-01 correction. |
| D05 | PORT_REQUIRED | Parse-time legacy reward/ability chat event adapter. Preserves inherited tellraw/click/hover resources under native parser; accepted BUG-02 correction. |
| D06 | PORT_REQUIRED | Removed unsupported bundled dimension gamerules. Only bundled pack function body changed relative to original. |
| D07 | PORT_REQUIRED | Legacy nether roots/fungus item-tag compatibility aliases. Preserves old criteria item-tag inputs on current registry. |
| D08 | BUGFIX_REQUIRED | Nonrecursive prepared existing-world reopen. Accepted feed339 one-shot reentry guard; no progress migration. |
| D09 | BUGFIX_REQUIRED | Category root granting restricted to cooperative mode. Accepted old-source conversion grant scope fix. |
| D10 | BUGFIX_REQUIRED | Exact legacy llama carpet body-equipment mapping. Only exact legacy llama-shaped carpet SNBT mapped; historical scope must remain. |
| D11 | BUGFIX_REQUIRED | Raider type-specific snake_case predicate correction. FINAL19 authorized fix; retained reconciliation evidence. |
| D12 | BUGFIX_REQUIRED | Player gamemode predicate key preservation. Native player codec accepts gamemode; avoid incompatible rename. |
| D13 | BUGFIX_REQUIRED | Scoreboard validation deferred until BACAP load is ready. Accepted BUG-04 server lifecycle fix; same six progression objectives. |
| D14 | ARCHITECTURE_CHANGE | World-copy conversion and freshness boundary. Original direct Files.copy acquired conversion/freshness/update seam; narrow Phase A extension at existing boundary. |
| D15 | ARCHITECTURE_CHANGE | Client Search command bridge/optional adapter. Original error-message inference replaced with client command, actual tree selection and optional reflection adapter; accepted BUG-06. |
| D16 | ARCHITECTURE_CHANGE | Durable early advancement tutorial receipt/state. Original transient tutorial state supplemented by early durable receipt; accepted BUG-05; generic screen behavior. |
| D17 | ARCHITECTURE_CHANGE | Unrecognized BACAP category uses vanilla tab fallback. Original returns null; current leaves vanilla create path. Difference proven; accepted-cause provenance not identified. No B2 reversal. |
| D18 | LOCALIZATION_PRESERVATION | Native RU overlay and offline historical authoring/provenance. 3228-key native overlay supplements original native localization. Two ATD diagnostic keys belong to associated product mechanisms, not new architecture. |
| D19 | PUBLICATION_ONLY | Phase A/FINAL19 frozen catalogs, witnesses and preservation tooling. Historical exact content/codec/evidence oracles; no replacement with new target. |
| D20 | PUBLICATION_ONLY | Accepted release reproduction adapters, fixture and generated hashes. Protects v0.1.5.4 release snapshot;303generated outputs and frozen resources; not Phase B runtime. |


Four real extensions are world-copy conversion/freshness, client Search bridge, durable tutorial receipt and vanilla unknown-category fallback. First three have preservation/accepted-bug rationale; last has proven behavior but unresolved rationale provenance. No B2 reversal/redesign.

## 11. Companion-pack integration

Original enum/download/override selection remains. Main always required; hardcore via vanilla setting; terrain via WorldCreationTab paired dependencies.

| Companion | B1 status | Override | Disposition |
|---|---|---|---|
| Hardcore | FOUND | `bacap_hardcore_override` | Compatibility/pin decision remains B4 |
| Terralith | AMBIGUOUS | `bacap_terralith_override` | Compatibility/pin decision remains B4 |
| Amplified Nether | AMBIGUOUS | `bacap_amplified_nether_override` | Compatibility/pin decision remains B4 |
| Nullscape | AMBIGUOUS | `bacap_nullscape_override` | Compatibility/pin decision remains B4 |


Current Hardcore pinuRKM9Bou, BACAP terrain1.18wrappers and terrain2.5.7/1.2.7/1.2.10remain untouched. Variant messages/IDs/wrappers and main reward/setup/score paths may become stale. B2 maps risk without claiming actual breakage or resolving ambiguous support; no new pin/download.

## 12. B3/B4 handoff

Old1229all/1152canonical and target1332/1242are independently pinned inputs. Hashes, B1 inventory and B2 occurrences are the handoff. B3 must cover:

- All1332target JSON and all1229old JSON, including no-display/hidden/excluded-root entries; identify add/remove/rename/change only in B3.
- Canonical visible sets and namespace membership using pinned B0/B1 rule.
- ID and parent/root/tree relationships; fixed ATD parents/tab/path-depth references.
- display title/description/icon/hidden/frame/background and other display fields.
- criteria trigger types and full conditions; AND-of-OR requirements/order and completion semantics.
- rewards functions/recipes/loot/experience and reachable function/predicate/tag resources, including transitive calls and fanpack extension hooks.
- All ATD production ID references,86 trackers, root wrappers, reward click commands and translation keys.
- PATH_SENSITIVE scoreboard/config fake players/objectives, function/tag/predicate layouts, setup/reward/msg/trophy/update_score paths.
- Upstream weighted points versus actual incremented completed-count objectives; avoid replacing original raw advancement progression with points.
- Cooperative/team/first-score/setup/reward semantics and target B1 changelog structural categories.

B4 concrete review queue (candidates, not approved edits):

- **B4-01 Main external pin and acceptance identities**: ExternalPack.BACAP + source SHA1 checks/filename/world freshness. Apply B1 target identity after B3 scope; do not bump marker for content version alone.
- **B4-02 Reward message/title/description/click overlays**: 1187 messages across main and four companion overrides. Review removed/moved/new IDs and upstream format/path ownership; update native resources using original design.
- **B4-03 Reward roots/milestone/Nullscape wrapper**: 15 category roots + advancement_legend + desolation. Review upstream grants, team/obtained checks, reward/exp/trophy paths; hash freshness coupling.
- **B4-04 Setup/configuration contracts**: 10 setup/config functions, bac_settings/bac_created/global_install, four optional switches. Review scoreboard fake players/objectives and load/setup ownership; pack format61 retained from baseline needs target compatibility review.
- **B4-05 Explicit tracker mappings**: 31 score +52 stat +3 nearby bindings, goal values, 6 progression objectives. Check IDs, semantics, score/stat goals, entity lists/radii, ON_A_RAIL and HALF_HEART_LIFE; distinguish upstream points from original completed-count semantics.
- **B4-06 Tree/category layout and Search references**: 17 tabs, path-depth assumption, 3 ordered parent sets/28 child paths; highlight commands. Review target parents/categories/roots and links; keep live tree lookup and existing Search adapter unless required.
- **B4-07 Conversion applicability and scoping**: JSON/function converters, LegacyItemText/LegacyChatText, two minecraft compatibility tags. Assess modern input behavior, idempotence/no unnecessary rewrite; marker only if actual production conversion revision changes.
- **B4-08 Companion/worldgen integration**: Hardcore FOUND; Terralith/Amplified/Nullscape AMBIGUOUS; eight old external pins. Resolve later with explicit authority; no B2 pin/download/compatibility claim.
- **B4-09 Target certification/regression coverage**: Old exact1229/1152 fixtures vs new1332/1242. Create Phase B successor inventories/native witnesses/coverage; preserve all historical oracles and publication snapshot.
- **B4-10 Native localization successor and provenance**: 3 runtime lang files + original embedded translate components. B6 use existing mechanism; research all usable RU sources then; preserve CC0 and stage-specific owner-permission evidence.
- **B4-11 Unknown-category fallback provenance**: AdvancementTabMixin unknown category behavior differs original. Current behavior mapped; accepted-cause record not identified. Review only if target category structure makes relevant; no B2 revert/refactor.


Definitely review IDs/keys/paths, setup/reward ownership,86tracker contracts, category/order assumptions, pin/freshness identities, converter applicability and target tests. Likely retained mechanisms are external/internal acquisition, native language resolution, live tree lookup, generated abilities and literal threshold comparisons.

Deferred to Phase C/D:1152custom-cap change, ability pacing/threshold/balance and terminal design. Companion questions remain B4.

Safety: branch/HEAD unchanged; tracked/staged diffs empty; protected B0/B1 records and all historical/B1 archive hashes match precheck. Product Java/resources/localization/tests, marker and historical evidence unchanged. No download/commit/push/tag/GitHub mutation/B3execution. Exactly three B2 durable files added; audit notes/tooling reside only under ignored build/tmp/phase_b_b2.

**PHASE_B_B2_MAPPED**
