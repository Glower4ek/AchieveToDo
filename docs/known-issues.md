# Known issues

Minor issues found during the certification of the Purpur 26.2 port (0.2.1-rc.1 and 0.2.1). Details are in
[certification/w5-purpur-26.2.md](certification/w5-purpur-26.2.md) and
[certification/0.2.1-purpur-26.2.md](certification/0.2.1-purpur-26.2.md). On 2026-11-20 the owner deferred issues 2 and
3 (Fabric parity); issue 1 is fixed in 0.2.1. On 2026-11-22 the owner decided to fix issues 2 and 3 in both mods for
0.3.0; the fixes are in the source and await the 0.3.0 certification.

| # | Issue | Severity | Status |
|---|---|---|---|
| 1 | A server started without the gameplay-mode JVM flags ran the legacy Bukkit path, which is not certified (copper armor is not blocked there) | minor (operator setup) | **Fixed in 0.2.1**: one-option operator launch (`-javaagent` only), and a start without it fails closed. Certified in [0.2.1-purpur-26.2.md](certification/0.2.1-purpur-26.2.md) |
| 2 | CHAOS thresholds are not reproducible from the world seed across JVM starts | minor (Fabric parity) | **Fixed in 0.3.0** in both mods (pending certification) |
| 3 | Touching a locked nether portal, End portal or end gateway grants the vanilla `enter_block` advancement (+1) | minor (Fabric parity) | **Fixed in 0.3.0** in both mods (pending certification) |
| 4 | Using locked shears on a snow golem or a wolf still grants "Unmasked" (BACAP) or "Shear Brilliance" | minor (Fabric parity) | **Fixed in 0.3.0** in both mods (pending certification) |

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
to a landmark denial were not checked for the same effect.
