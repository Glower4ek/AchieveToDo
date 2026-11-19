# Known issues

Minor issues found during the W5 certification of the Purpur 26.2 port, 0.2.1-rc.1. Details are in
[certification/w5-purpur-26.2.md](certification/w5-purpur-26.2.md). On 2026-11-20 the owner decided to defer all
three; none of them blocks 0.2.1-rc.1.

| # | Issue | Severity | Status |
|---|---|---|---|
| 1 | A server started without the gameplay-mode JVM flags runs the legacy Bukkit path, which is not certified (copper armor is not blocked there) | minor (operator setup) | Fix ready, not merged: commit `4ee83cf` on `fix/default-start-fail-closed` refuses to start without an explicit mode. It changes the plugin jar, so it needs a new certification run; planned for the next release candidate |
| 2 | CHAOS thresholds are not reproducible from the world seed across JVM starts | minor (Fabric parity) | Deferred |
| 3 | Touching a locked End portal or end gateway grants the vanilla `enter_block` advancement (+1) | minor (Fabric parity) | Deferred |

## 1. Default start runs the legacy path

`AchieveToDoPlugin.onEnable` uses `achievetodo.gameplay.mode=diagnostic` when no flag is given. That starts the
legacy restriction runtime (`restriction/`, `adapters/`) instead of the certified native path. Only the
composition listed in the certification report is certified. Until the fix lands, release notes must give
operators the full launch command.

## 2. CHAOS is not seed-deterministic

`ChaosProgressionGenerator` (Purpur and Fabric alike) starts from `Map.copyOf(baseProgression)`. The iteration
order of that map depends on a per-JVM random salt, so a new world with the same seed gets different thresholds
after every JVM start. This contradicts the mode description: "shuffled according to the world seed". Inside one
world nothing changes, because the generated `chaos_<seed>.toml` is saved and reused. Fixing it means iterating
in a fixed order, for example `AbilityType` declaration order. That changes the result for every seed compared
with the Fabric original, so it should be done in both mods at once.

## 3. Locked End portal and gateway grant their advancement

The guard denies the teleport (`setAsInsidePortal`), but the vanilla `enter_block` criterion fires on contact,
so "The End?" (`story/enter_the_end`) or "Remote Getaway" is granted while the ability is locked. The Fabric
original behaves the same way. The certification tracks it as the visible verdict `PASS_PARITY_ENTER_BLOCK`.
Changing it means guarding the `enter_block` trigger for these blocks too, in both mods.
