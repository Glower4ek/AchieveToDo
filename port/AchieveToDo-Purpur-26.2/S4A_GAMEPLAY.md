# S4A gameplay slice (experimental)

`PRODUCT_GAMEPLAY_READY=false`. Bridge admission still requires the existing explicit experimental configuration and validated BACAP/world/config identities. No release, dependency pin or protocol change is included in S4A.

The first native server gameplay slice enforces these stable IDs:

| ID | Original Fabric hook | Native server enforcement | EASY / MEDIUM / HARD |
| --- | --- | --- | --- |
| `break_blocks` | `PlayerEntityMixin.lockBlockBreak`, `Player.blockActionRestricted` | Cancel `BlockDamageEvent` and recheck `BlockBreakEvent` before destruction | 6 / 32 / 34 |
| `break_blocks_in_negative_y` | Same hook, `pos.getY() < 0` | Below Y=0 require both this ability and `break_blocks` | 32 / 38 / 42 |
| `sleep` | `PlayerEntityMixin.lockSleep`, `Player.startSleepInBed` | Deny native `PlayerBedEnterEvent`; never force sleep or override vanilla bed conditions | 22 / 24 / 24 |
| `get_into_boat` | `AbstractBoatEntityMixin.lockBoat`, `AbstractBoat.canAddPassenger` | Cancel both vehicle-entry and generic mount paths for Bukkit `Boat` (including chest boats/rafts) | 42 / 44 / 44 |

Source authority: canonical Fabric commit `0358ebfc4ab644756f1713f57adf7c3e26054581`, `ability/AbilityType.java`, `ability/Progressions.java`, the two mixins above, and `server/AchieveToDoServer.isAbilityLocked`. Creative/spectator bypass follows that original server implementation. Tool, landmark and other ability gates remain distinct abilities; this slice does not certify them.

`BukkitProgressionRuntime.getProgressionState` now samples its existing read-only S3C capture at permission-query time. It uses the same native BACAP scoreboard/team resolver, world group and validated config generation as the Bridge. This closes the interval between a score/team change and periodic reconciliation. Permission queries never reconcile criteria, award rewards or write a count cache. Existing synthetic reconciliation remains responsible for its original transitions; no equal-count repair is added.

`BukkitGameplayRuntime` starts after progression in both the experimental Bridge branch and the ordinary lifecycle, and unregisters its listeners before progression closes. Ordinary mode also retains its pre-existing broad experimental restriction adapters; they can deny additional unimplemented/uncertified abilities. The S4A listeners only cancel transactions and never apply a second block/sleep/mount effect. Prior cancellation is never undone.

On unavailable content, source, offline actor or unknown world binding, numeric gameplay queries return unavailable and deny survival/adventure actions. No last unlocked snapshot or client unlock flag authorizes gameplay. A lower count/config or changed team closes the next action; mining completion is checked again. Existing sleeping/passengers are not forcibly removed when permission changes: the original Fabric hooks guard entry, not continued occupancy.

No Fabric changes are needed for S4A. The committed S3C universal client already keeps gameplay prediction neutral on the Purpur backend, while presenting the server's projection. Solo/Fabric Dedicated behavior and the published 0.2.0 JAR remain unchanged.

Run `gameplayTest` for numeric and Paper callback contracts. `check` includes it alongside existing tests; S3B tests additionally require the read-only `ACHIEVETODO_CANONICAL_PRESETS` source, and progression tests require `ACHIEVETODO_TEST_BACAP` pointing to the directory containing both BACAP ZIPs. Use a non-aliased disposable TEMP/TMP path on Windows. Native runtime receipts are kept outside Git in `D:\Vibecode\AchieveToDo_DualRuntime_S4A_Evidence`.

Remaining gates include the other 149 abilities, independent landmark/tool dependencies, two-active-client resource reload under host pressure, optional enabled rewards/cooperative/world-generation profiles, arbitrary multiworld/proxy support, and production admission/readiness. During S4A, one cold client configuration also disconnected with `ATD_FINISH_BEFORE_ADMIT`; one bounded manual reconnect succeeded. This intermittent configuration ordering/admission gate remains open and is separate from the gameplay gates. A four-ability slice does not close those gates.
