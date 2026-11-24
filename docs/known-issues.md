# Known issues

Minor issues found during the certification of the Purpur 26.2 port (0.2.1-rc.1 and 0.2.1). Details are in
[certification/w5-purpur-26.2.md](certification/w5-purpur-26.2.md) and
[certification/0.2.1-purpur-26.2.md](certification/0.2.1-purpur-26.2.md). On 2026-11-20 the owner deferred issues 2 and
3 (Fabric parity); issue 1 is fixed in 0.2.1. On 2026-11-22 the owner decided to fix issues 2 and 3 in both mods for
0.3.0; the fixes are certified in [certification/0.3.0-purpur-26.2.md](certification/0.3.0-purpur-26.2.md).
Issues 5-7 come from the notes of the 0.3.0 certification and from the audit asked by issue 4; they are handled in
0.4.0 (Minecraft 26.3). Issue 8 was found by the 0.4.0 certification smoke run. Issues 5, 6 and 8 are certified in
[certification/0.4.0-purpur-26.3.md](certification/0.4.0-purpur-26.3.md).

| # | Issue | Severity | Status |
|---|---|---|---|
| 1 | A server started without the gameplay-mode JVM flags ran the legacy Bukkit path, which is not certified (copper armor is not blocked there) | minor (operator setup) | **Fixed in 0.2.1**: one-option operator launch (`-javaagent` only), and a start without it fails closed. Certified in [0.2.1-purpur-26.2.md](certification/0.2.1-purpur-26.2.md) |
| 2 | CHAOS thresholds are not reproducible from the world seed across JVM starts | minor (Fabric parity) | **Fixed in 0.3.0** in both mods. Certified in [0.3.0-purpur-26.2.md](certification/0.3.0-purpur-26.2.md) |
| 3 | Touching a locked nether portal, End portal or end gateway grants the vanilla `enter_block` advancement (+1) | minor (Fabric parity) | **Fixed in 0.3.0** in both mods. Certified in [0.3.0-purpur-26.2.md](certification/0.3.0-purpur-26.2.md) |
| 4 | Using locked shears on a snow golem or a wolf still grants "Unmasked" (BACAP) or "Shear Brilliance" | minor (Fabric parity) | **Fixed in 0.3.0** in both mods. Certified in [0.3.0-purpur-26.2.md](certification/0.3.0-purpur-26.2.md) |
| 5 | The cartographer trade denial showed its lock message seconds late or not at all | minor (UX) | **Fixed in 0.4.0** in both mods. Certified in [0.4.0-purpur-26.3.md](certification/0.4.0-purpur-26.3.md) |
| 6 | Inside a locked landmark some denied interactions still counted for use criteria (a map put into an item frame, a golden dandelion given to a tadpole, wolf armor, a renamed name tag) | minor (progression) | **Fixed in 0.4.0** in both mods. Certified in [0.4.0-purpur-26.3.md](certification/0.4.0-purpur-26.3.md) |
| 7 | Right after joining a Purpur server, a locked water bucket can look empty for a moment | cosmetic | Known, by design (see below) |
| 8 | Inside a locked landmark the 26.3 golden dandelion still locked a baby's age and counted "Fountain of Youth" | minor (landmark lock) | **Fixed in 0.4.0** in both mods. Certified in [0.4.0-purpur-26.3.md](certification/0.4.0-purpur-26.3.md) |

## 1. Default start ran the legacy path (fixed in 0.2.1)

Up to 0.2.1-rc.1, `AchieveToDoPlugin.onEnable` used `achievetodo.gameplay.mode=diagnostic` when no flag was given,
which started the legacy restriction runtime (`restriction/`, `adapters/`) instead of the certified native path.
0.2.1 loads the plugin as a Java agent (`-javaagent:plugins/<plugin jar>`), derives the certified composition itself
and stops the server with the correct start command when the agent is missing (`ATD_STARTUP_FAIL_CLOSED`). The legacy
path is opt-in only (`-Dachievetodo.gameplay.mode=diagnostic`) and logs `ATD_UNCERTIFIED_LEGACY_PATH`.

## 2. CHAOS is not seed-deterministic (fixed in 0.3.0)

0.3.0 iterates in `AbilityType` declaration order in both mods. A test in each mod pins the same digest of the seed-42
thresholds, so they agree with each other and across JVM starts. Thresholds for a given seed differ from 0.2.x; an
existing world keeps its saved `chaos_<seed>.toml`.

`ChaosProgressionGenerator` (Purpur and Fabric alike) starts from `Map.copyOf(baseProgression)`. The iteration
order of that map depends on a per-JVM random salt, so a new world with the same seed gets different thresholds
after every JVM start. This contradicts the mode description: "shuffled according to the world seed". Inside one
world nothing changes, because the generated `chaos_<seed>.toml` is saved and reused. Fixing it means iterating
in a fixed order, for example `AbilityType` declaration order. That changes the result for every seed compared
with the Fabric original, so it should be done in both mods at once.

## 3. Locked End portal and gateway grant their advancement (fixed in 0.3.0)

0.3.0 skips the `enter_block` trigger (`ServerPlayer.onInsideBlock`) for a locked nether portal, End portal or central
end gateway, with the same exemptions as the teleport guard: Fabric `ServerPlayerEntityMixin`, Purpur
`NativeMovementAgent`. The nether portal was found by the 0.3.0 campaign: the 0.2.1 fixture never stepped into it in
survival, so "We Need to Go Deeper" was granted unnoticed.

The guard denies the teleport (`setAsInsidePortal`), but the vanilla `enter_block` criterion fires on contact,
so "The End?" (`story/enter_the_end`) or "Remote Getaway" is granted while the ability is locked. The Fabric
original behaves the same way. The certification tracks it as the visible verdict `PASS_PARITY_ENTER_BLOCK`.
Changing it means guarding the `enter_block` trigger for these blocks too, in both mods.

## 4. Locked shears on a snow golem or wolf grant an advancement (fixed in 0.3.0)

Found by the new 0.3.0 campaign cases. The guard answered a denied interaction with `InteractionResult.SUCCESS`, and
a consumed interaction fires `player_interacted_with_entity`, so BACAP "Unmasked" (snow golem) and vanilla
"Shear Brilliance" (wolf armor) were granted while shears were locked. Both mods now answer `FAIL` there
(Fabric `SnowGolemEntityMixin`, `WolfEntityMixin`; Purpur `NativeComplexSpecs`). Other guards that answer `SUCCESS`
to a landmark denial were checked in 0.4.0, see issue 6.

## 5. Cartographer lock message late (fixed in 0.4.0)

Found in the 0.3.0 review: the cartographer trade was denied, but the lock message did not show (3 of 3 rows; the
other 12 professions showed it). The guard checked the lock only after `Villager.getOffers()`. For a villager without
offers that call generates them, and a cartographer's explorer maps search for structures within 100 chunks, which
held the server for about three seconds; the message arrived after that. 0.4.0 checks the lock first and gives a
denied player an empty offer list, so no offers are generated for a trade that cannot happen (Fabric
`VillagerEntityMixin`, Purpur `NativeLandmarkOperations.offersGated`). The villager still shakes its head as before.

## 6. Denied interactions counted for use criteria (fixed in 0.4.0)

The audit asked by issue 4. A denied use or interaction answers with a consuming result, so that vanilla does not go
on to use the held item. Vanilla then fires the use criteria as if the use had happened:
`player_interacted_with_entity` (entity interactions), `default_block_use` and `any_block_use` (blocks used with an
empty hand) and `item_used_on_block`. Inside a locked landmark this granted, for example, BACAP "Map Room" (a map into
an item frame), "Fountain of Youth" (a golden dandelion on a tadpole), the wolf armor criteria and the "Grumm" name
tag criterion. Answering `FAIL` instead, as for the shears, would let the held item act on the entity (a name tag
would rename it). 0.4.0 keeps the answer and marks every effectful denial; the very next trigger of each of these
kinds for that player within one tick does not count. The Purpur plugin already did this for `item_used_on_block`
since 0.2.1 (W5-RED-1); 0.4.0 extends it to the other three triggers and adds the same rule to the Fabric mod
(`DeniedCriteria` and four trigger mixins; Purpur `NativeCriterionAgent`).

## 7. Water bucket looks empty right after joining (by design)

Noted in the 0.3.0 certification. For a moment after joining a Purpur server, until the client has received the
ability state, the client mod predicts uses as allowed. Emptying a locked water bucket in that moment empties it on the
client; the server denies the use and resends the inventory, so the bucket is full again at once. Nothing is placed and
nothing is granted. Blocking every lockable action on the client until the state arrives would also block players on
servers without AchieveToDo, so the owner decided on 2026-11-23 to keep it as it is.

## 8. Golden dandelion age lock not denied in a locked landmark (fixed in 0.4.0)

Found by the 0.4.0 certification smoke run (the issue 6 audit row "golden dandelion on tadpole"). Minecraft 26.3 lets
a golden dandelion lock a baby's age (`AgeableMob.mobInteract`, overridden by `Tadpole.mobInteract`). That branch is
new in 26.3, so no landmark guard covered it: inside a locked landmark the dandelion was used, the baby stopped growing
and BACAP "Fountain of Youth" counted. 0.4.0 denies it like the other entity interactions (Fabric `AgeableMobMixin`
and a second `TadpoleEntityMixin` injection; Purpur `NativeLandmarkSupplementSpecs`, checked before the Bukkit
`PlayerToggleEntityAgeLockEvent` so other plugins do not see a toggle that does not happen); the denial is marked, so
issue 6 keeps the criterion from counting.
