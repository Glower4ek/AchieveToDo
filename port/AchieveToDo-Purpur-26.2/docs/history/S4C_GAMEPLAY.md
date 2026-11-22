# S4C menu enforcement (experimental)

`PRODUCT_GAMEPLAY_READY=false`. S4C adds 26 exact ability IDs to the previous 16. The cold `ATD_FINISH_BEFORE_ADMIT` integration gate remains OPEN. No product Bridge/Fabric, protocol, progression authority, rewards, dependency pins or content manifests change.

`GameplayMenuAbilities` maps 12 native block-menu transactions and 14 entity merchant sources to canonical IDs. `BukkitExpandedGameplayListener.open` reuses the existing HIGHEST/ignoreCancelled InventoryOpenEvent handler and samples current S3C permission once for the selected ability. It never opens a replacement menu, cancels earlier restrictions, closes an already-open menu on revocation or intercepts ongoing crafting/trading. The original hooks guard menu entry, not continued menu use. Creative/spectator bypass remains canonical.

Block menus require the exact InventoryType and native inventory location's matching material (including chipped/damaged anvils). Personal 2x2 crafting, virtual menus without the relevant block source, and other menu families stay distinct. MerchantInventory must identify a real Villager with its current registry-backed profession or a WanderingTrader; plugin-created merchants remain separate. Native interactions and Paper API opening paths use the same transaction gate. No permanent per-player unlock cache is added.

| Exact IDs | Canonical Fabric source | Native transaction |
| --- | --- | --- |
| `open_crafting_table`, `open_stonecutter`, `open_grindstone`, `open_anvil`, `open_beacon`, `open_cartography_table`, `open_brewing_stand`, `open_smithing_table`, `open_loom`, `open_enchanting_table` | Respective `*BlockMixin.useWithoutItem` openMenu hooks | Actual block-source menu opening |
| `open_smoker`, `open_blast_furnace` | `AbstractFurnaceBlockMixin.lockFurnace` exact subclass switch | SMOKER/BLAST_FURNACE block menu opening |
| `trade_with_armorer`, `trade_with_butcher`, `trade_with_cartographer`, `trade_with_cleric`, `trade_with_farmer`, `trade_with_fisherman`, `trade_with_fletcher`, `trade_with_leatherworker`, `trade_with_librarian`, `trade_with_mason`, `trade_with_shepherd`, `trade_with_toolsmith`, `trade_with_weaponsmith` | `VillagerEntityMixin.lockVillager` and `AbilityType.findTradeAbility` | Real villager's merchant menu opening, profession at action time |
| `trade_with_wandering_trader` | `WanderingTraderEntityMixin.lockWanderingTrader` | Real wandering trader merchant menu opening |

These are numeric ability gates. Independent landmark/worldgen restrictions at the same original hooks remain uncertified, as in S4A/S4B. `open_shulker_box` is deliberately excluded because its original gate applies only while animation status is CLOSED. `open_ender_chest` is also excluded pending precise block/API-source handling. No item-material approximation is claimed for food components, equipment or tool restrictions.

Working classification of the 137 IDs remaining at S4B was used only to choose this batch:

| Primary family | Count | Selection implication |
| --- | ---: | --- |
| Native cancellable interactions | 8 | Other exact interactions need separate predicates. |
| Inventory/crafting/container actions | 55 | 12 block menus selected; component-based food, bundles, ender chest and shulkers deferred. |
| Entity interactions and equipment | 23 | 14 merchants selected; equipment needs inventory/dispenser/equip-path parity. |
| Movement and environment restrictions | 8 | Movement initiation, collision and dimension paths require their own analysis. |
| Combat and damage mechanics | 12 | Weapon/tool hooks may precede damage and secondary effects. |
| NMS/deeper hooks | 7 | Multi-operation item/block families require deeper transaction checks. |
| Unavoidable client dependencies | 2 | vision/open_inventory include local rendering/UI; universal-client work required. |
| Independent reward/worldgen dependencies | 22 | 22 landmark IDs require provenance/bounds infrastructure; enabled reward/cooperative/worldgen profiles are a separate cross-cutting gate. |

This classification is a primary work family, not a claim that all members have identical dependencies. Exact partition is external `raw/remaining_classification.json`, derived from the 153-ID catalog and S0 contracts; no new broad audit was performed.

`gameplayTest` retains S4A847 and S4B3472 and adds S4C1909 numeric/event contracts across all26 IDs. The test-only RegistryAccess service supplies inert API menu/profession descriptors because Paper initializes InventoryType through registries; it neither supplies progression state nor simulates Minecraft effects. It is excluded from the product JAR. Full `check` also runs the unchanged shared S3C source/config/team/world/Bridge contracts. See S4A_GAMEPLAY.md and S3B_SEMANTICS.md for required read-only BACAP/canonical preset inputs.

Native receipts and exact locked/unlocked coverage are external at `D:\Vibecode\AchieveToDo_DualRuntime_S4C_Evidence`. Only abilities with completed native evidence are counted in the final S4C report. No Solo/Fabric Dedicated recertification is implied; the universal client bytes are unchanged. Production admission, two-client watchdog, optional profiles and arbitrary multiworld/proxy certification remain open. Stop after S4C; no release/tag/push.
