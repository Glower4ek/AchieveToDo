# S4B expanded gameplay (experimental)

`PRODUCT_GAMEPLAY_READY=false`. This extends the S4A slice by twelve exact stable IDs. The unchanged S3C authority, native BACAP scoreboard resolver, team/offline membership, validated config generation and world group remain the only permission source. No protocol, dependency pin, reward path, count cache or Fabric source changes are included.

| IDs | Original hook and server transaction |
| --- | --- |
| `get_into_minecart` | `EntityMixin.lockMinecart`: vanilla rideable Minecart only. Both VehicleEnterEvent and EntityMountEvent cancel entry. Container/other minecart types are separate native entities. |
| `open_door`, `open_trapdoor`, `open_fence_gate` | Respective block mixins gate a closed block's manual opening. PlayerInteractEvent denies block use, preserving the previous item-use decision. Closing an already open block, redstone, and unrelated interactions remain native. |
| `open_chest`, `open_barrel`, `open_furnace` | ChestBlockMixin, BarrelBlockMixin and AbstractFurnaceBlockMixin gate the native menu transaction. InventoryOpenEvent checks actual block holders, including double/trapped chests, rather than generic menu type. Vehicle/virtual chests and smoker/blast-furnace abilities are distinct. The API open path is covered too. |
| `throw_snowball`, `throw_egg`, `throw_ender_pearl`, `throw_wind_charge` | Respective Item.use hooks gate launch. Paper's PlayerLaunchProjectileEvent precedes spawn, consumption, stats, sound and cooldown in pinned Purpur2620. Generic ProjectileLaunchEvent covers alternative player API launches; non-player launches remain native. EggItem variants share the egg gate, and BreezeWindCharge is distinct from player WindCharge. |
| `use_fishing_rod` | FishingRodItemMixin's second sound hook guards casting. Only PlayerFishEvent.State.FISHING is gated. Retrieval remains allowed after a count decrease, matching the original branch ordering. |

The listeners only deny transactions. They never reopen a cancelled event, create a replacement effect, eject existing passengers, close existing menus on count changes, or remove an already cast hook. Creative/spectator bypass follows the original server semantics. Independent landmark and other unimplemented restrictions are not certified by these twelve IDs.

`gameplayTest` retains the 847 S4A checks and adds the S4B numeric/callback contracts. `check` also runs the existing Bridge, source/config/team, progression, core and foundation contracts. Provide the BACAP directory and read-only canonical Progressions.java oracle as described in S4A_GAMEPLAY.md and S3B_SEMANTICS.md.

Native receipts, the pre-implementation source/event selection, exact outcomes and bounded cold-configuration diagnostics are outside Git at `D:\Vibecode\AchieveToDo_DualRuntime_S4B_Evidence`. The intermittent `ATD_FINISH_BEFORE_ADMIT` gate remains OPEN: an owner-loop scheduling diagnostic is not a verified product fix. No configuration admission check was weakened and native reenterConfiguration is not used for resync.

After this slice, 16 of 153 IDs have selected native gameplay enforcement; 137 remain outside that certification. Production admission, independent landmark/tool dependencies, two-client resource reload under host pressure, optional reward/cooperative/world-generation profiles, and arbitrary multiworld/proxy support remain separate open gates. No release/tag/push is part of S4B.
