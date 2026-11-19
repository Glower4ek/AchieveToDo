# W5 final group verdicts (r3 + supplements r4, r5)

Rows: 2579. Verdicts: NA_NO_LOCKED_STATE 10, NA_OFFHAND_NOT_APPLICABLE 2, PASS 2528, PASS_MASKED_BY_DEPENDENCY 13, PASS_NO_OVERLAY 14, PASS_PARITY_ENTER_BLOCK 12

Groups: RUNTIME_PASS 57

| Group | Abilities | Status | Notes |
|---|---|---|---|
| food_component_start_40 | 40 | RUNTIME_PASS | eat_salmon: custom_vision:locked=PASS_MASKED_BY_DEPENDENCY |
| equipment_identity9_routes_slots | 9 | RUNTIME_PASS |  |
| tool_material7_each_native_mode | 7 | RUNTIME_PASS | use_golden_tools: hard:locked=PASS_MASKED_BY_DEPENDENCY |
| villager13_profession_resolver | 13 | RUNTIME_PASS | trade_with_cartographer: chaos:locked=PASS_NO_OVERLAY, custom_vision:locked=PASS_NO_OVERLAY, easy:locked=PASS_NO_OVERLAY, hard:locked=PASS_NO_OVERLAY, normal:locked=PASS_NO_OVERLAY, normal:op=PASS_NO_OVERLAY |
| wandering_trader_separate | 1 | RUNTIME_PASS |  |
| furnace3_shared_base_use | 3 | RUNTIME_PASS |  |
| menu_open_chest | 1 | RUNTIME_PASS |  |
| menu_open_crafting_table | 1 | RUNTIME_PASS |  |
| menu_open_barrel | 1 | RUNTIME_PASS |  |
| menu_open_stonecutter | 1 | RUNTIME_PASS |  |
| menu_open_grindstone | 1 | RUNTIME_PASS |  |
| menu_open_anvil | 1 | RUNTIME_PASS |  |
| menu_open_beacon | 1 | RUNTIME_PASS |  |
| menu_open_cartography_table | 1 | RUNTIME_PASS |  |
| menu_open_ender_chest | 1 | RUNTIME_PASS |  |
| menu_open_brewing_stand | 1 | RUNTIME_PASS |  |
| menu_open_smithing_table | 1 | RUNTIME_PASS |  |
| menu_open_loom | 1 | RUNTIME_PASS |  |
| menu_open_shulker_box | 1 | RUNTIME_PASS |  |
| menu_open_enchanting_table | 1 | RUNTIME_PASS |  |
| shelf_open_chest_non_menu | 1 | RUNTIME_PASS |  |
| landmark22_selectors_and_action_matrix | 22 | RUNTIME_PASS | interact_inside_ruined_portal/chest at structure start: chaos:locked=PASS_MASKED_BY_DEPENDENCY; interact_inside_buried_treasure/chest at structure start: chaos:locked=PASS_MASKED_BY_DEPENDENCY; interact_inside_desert_pyramid/chest at structure start: chaos:locked=PASS_MASKED_BY_DEPENDENCY; interact_inside_mineshaft/chest at structure start: chaos:locked=PASS_MASKED_BY_DEPENDENCY; interact_inside_swamp_hut/chest at structure start: chaos:locked=PASS_MASKED_BY_DEPENDENCY; interact_inside_fortress/chest at structure start: chaos:locked=PASS_MASKED_BY_DEPENDENCY; interact_inside_bastion_remnant/chest at structure start: chaos:locked=PASS_MASKED_BY_DEPENDENCY; interact_inside_trail_ruins/chest at structure start: chaos:locked=PASS_MASKED_BY_DEPENDENCY; interact_inside_mansion/chest at structure start: chaos:locked=PASS_MASKED_BY_DEPENDENCY; interact_inside_end_city/chest at structure start: chaos:locked=PASS_MASKED_BY_DEPENDENCY |
| mining_basic_two | 2 | RUNTIME_PASS |  |
| opening_three_native_owners | 3 | RUNTIME_PASS |  |
| bundle_two_native_routes | 1 | RUNTIME_PASS |  |
| throwables_four_paper_candidates | 4 | RUNTIME_PASS |  |
| fishing_cast_only | 1 | RUNTIME_PASS |  |
| sleep | 1 | RUNTIME_PASS |  |
| vehicle_mount_two | 2 | RUNTIME_PASS |  |
| movement_jump | 1 | RUNTIME_PASS | jump: custom_vision:locked=PASS_MASKED_BY_DEPENDENCY |
| movement_swim | 1 | RUNTIME_PASS |  |
| movement_sneak | 1 | RUNTIME_PASS |  |
| movement_sprint | 1 | RUNTIME_PASS |  |
| client_local_vision | 1 | RUNTIME_PASS |  |
| client_local_open_inventory | 1 | RUNTIME_PASS |  |
| action_use_shield | 1 | RUNTIME_PASS |  |
| action_shoot_crossbow | 1 | RUNTIME_PASS |  |
| action_use_ominous_bottle | 1 | RUNTIME_PASS |  |
| action_attack_with_trident | 1 | RUNTIME_PASS |  |
| action_shoot_bow | 1 | RUNTIME_PASS |  |
| action_use_jukebox | 1 | RUNTIME_PASS |  |
| action_use_composter | 1 | RUNTIME_PASS |  |
| action_charge_respawn_anchor | 1 | RUNTIME_PASS |  |
| action_attack_with_mace | 1 | RUNTIME_PASS |  |
| action_use_ender_eye | 1 | RUNTIME_PASS |  |
| action_glide_with_fireworks | 1 | RUNTIME_PASS |  |
| action_place_end_crystal | 1 | RUNTIME_PASS |  |
| action_use_campfire | 1 | RUNTIME_PASS |  |
| action_use_spyglass | 1 | RUNTIME_PASS |  |
| complex_use_water_bucket | 1 | RUNTIME_PASS |  |
| complex_use_shears | 1 | RUNTIME_PASS |  |
| complex_use_flint_and_steel | 1 | RUNTIME_PASS |  |
| complex_ignite_tnt | 1 | RUNTIME_PASS |  |
| complex_use_cauldron | 1 | RUNTIME_PASS |  |
| complex_use_brush | 1 | RUNTIME_PASS |  |
| complex_unlock_vault | 1 | RUNTIME_PASS | unlock_vault: custom_vision:locked=PASS_NO_OVERLAY, easy:locked=PASS_NO_OVERLAY, hard:locked=PASS_NO_OVERLAY, normal:locked=PASS_NO_OVERLAY, normal:offhand_locked=PASS_NO_OVERLAY, normal:op=PASS_NO_OVERLAY |
| portal_three_conditioned | 3 | RUNTIME_PASS | enter_end/walk into portal after observation: chaos:locked=PASS_PARITY_ENTER_BLOCK, custom_vision:locked=PASS_PARITY_ENTER_BLOCK, easy:locked=PASS_PARITY_ENTER_BLOCK, hard:locked=PASS_PARITY_ENTER_BLOCK, normal:locked=PASS_PARITY_ENTER_BLOCK; teleport_outer_islands/central gateway (y=75, /x/,/z/<=96 per NativePortalAgent.central); vanilla finds the outer exit itself: chaos:locked=PASS_PARITY_ENTER_BLOCK, custom_vision:locked=PASS_PARITY_ENTER_BLOCK, easy:locked=PASS_PARITY_ENTER_BLOCK, hard:locked=PASS_PARITY_ENTER_BLOCK, normal:locked=PASS_PARITY_ENTER_BLOCK |
