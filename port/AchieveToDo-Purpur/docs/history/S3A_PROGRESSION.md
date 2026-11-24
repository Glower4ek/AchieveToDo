# S3A progression foundation

This experimental profile uses the existing BACAP scoreboard progression runtime. It does not certify ability enforcement or enable product gameplay admission. Bridge 1.2 remains the accepted diagnostic projection (`PRODUCT_GAMEPLAY_READY=false`).

## Operator-owned content and lifecycle

BACAP 1.21 for Minecraft 26.2 is pinned upstream at Modrinth `Y2zZ5eSs`, source SHA1 `14da3f07b5467e8b59ffc0253fd8212c938cd739`. The supported profile requires canonical Fabric 0.2.0's prepared `compat_26_2_r19` BACAP and its `bacap_override`. Raw upstream content is a different identity. Hash-only metadata lives in `src/main/resources/bacap/s3a-profile.properties`; no BACAP archive or assets are distributed by this plugin.

Install the lawfully acquired, canonical-prepared files as `<level-directory>/datapacks/bacap.zip` and `bacap_override.zip` before server startup. Do not edit their contents. Set these JVM properties explicitly:

```
-Dachievetodo.bacap.directory=<absolute level-directory>/datapacks
-Dachievetodo.progression.world=<actual Bukkit world UUID>
-Dachievetodo.progression.worldKey=minecraft:overworld
```

Bootstrap verifies all pack content before native world discovery. Native datapack loading owns BACAP; the existing Paper discovery hook owns only the embedded synthetic pack. Purpur2620 fails before registry initialization when multiple plugin-provided packs are compared; native world discovery avoids that target defect without NMS or a server upgrade.

The exact supported enabled set is `vanilla`, `paper`, `AchieveToDo/embedded`, `file/bacap.zip`, `file/bacap_override.zip`, with BACAP below its override. The configured directory must be the actual server level datapacks directory. Additional/disabled/reordered packs are rejected. Registry criteria and completion requirement groups are checked against the validated content plus the unchanged synthetic definitions. Content replacement requires a full restart. Generic plugin `/reload` is unsupported. Native `minecraft:reload` revalidates discovery, loaded definitions and scores before readiness returns; a failed transaction stays unavailable.

For disposable diagnostics only, combine the accepted explicit S2P Bridge properties with `-Dachievetodo.bridge.spike=true -Dachievetodo.progression.foundation=true`. This starts real progression alongside the diagnostic Bridge, without starting the legacy ability enforcement/UI services. The S2P-only mode without the foundation flag keeps its original independent content fixture.

## Count and persistence contract

The source of truth is the recognized **displayed main scoreboard objective**, with slot priority sidebar, list, below-name. The six canonical modes are preserved. Personal modes read the player's score (unset = 0); team modes sum every scoreboard team entry, including offline holders. No team means 0. No displayed recognized objective means unavailable. Counts are signed scoreboard integers, not a count of Bukkit advancement objects.

Vanilla/BACAP own criteria completion (AND of requirement groups, OR within each group), actual rewards and scores. BACAP reward macros decide increments, including `bac_dont_count`, first/team policies and fanpack hooks. Synthetic `achievetodo:abilities/*` and vanilla recipes do not independently add BACAP scores. The canonical `blazeandcave:config/update_score` function has its own explicit recount ID list. An explicit advancement revoke alone does **not** undo the previously incremented BACAP score; invoking BACAP's recount is an operator action, as in canonical Fabric. This plugin neither silently recounts completions nor invokes reward/recount functions.

Scores are reread every server tick to cover grant/revoke effects, score reset and offline team changes without relying on an incomplete Bukkit event stream. Threshold transitions run only when the effective count changes. Ancillary presentation refreshes retain their existing interval. Reconnect/reload rebuild derived caches from native scoreboard and advancement storage. Shutdown releases tasks/listeners and caches; there is no second persisted count or reward ledger.

The existing synthetic synchronizer reads persisted demystified flags, awards only remaining criteria and revokes only an awarded unlocked criterion. Repeated readback/join does not re-complete an already completed synthetic advancement. Deliberate relock/reunlock can replay its canonical unlock **message**. BACAP item/XP/trophy ownership is unchanged. No durable synthetic flags are revoked merely because content/objective/world readiness is unavailable.

## Scope boundaries

One explicit world UUID **and** resource key are supported; names are informational. Foreign/offline contexts return unavailable and cannot reuse a cached count. The world selection file validates both identifiers and publishes atomically. Old unbound selection files fail closed; migration of user worlds is not provided. Main scoreboard/advancement storage is server-owned; independent world groups and dimension transitions require S3B binding/storage proofs. This profile does not claim multiworld isolation.

Optional reward/cooperative/worldgen profiles, full canonical world/config migration and all 153 abilities remain gated. Public BACAP redistribution requires its own permission; the project's license does not grant those rights. No release or upload is part of S3A.

## Focused verification

`progressionTest` runs API contract fixtures and world binding/persistence checks. Set `ACHIEVETODO_TEST_BACAP` to the external prepared profile directory to include real-content identity/inclusion checks; otherwise those cases explicitly report `NOT_RUN`. Keep locked dependencies and `--dependency-verification strict`. Actual client gameplay and native persistence evidence is maintained separately under the authorized S3A evidence directory.
