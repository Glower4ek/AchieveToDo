# AchieveToDo 0.3.0 — Minecraft 26.2

The Fabric mod and its Purpur server plugin, finished as product releases.
Runtime: Minecraft 26.2, Java 25, Purpur build 2620. All 153 gameplay abilities are retained.

## Downloads

| File | For | SHA-256 |
|---|---|---|
| `achievetodo-mc26.2+0.3.0.jar` | Fabric client (and single player) | `2c03d9fa9e26c1c53691d720671107e8569af63092a5c2a2e9e9b1c32644c649` |
| `AchieveToDo-Purpur-26.2-0.3.0.jar` | Purpur 26.2 server plugin | `d3019bd333591c8f57645e093aea78befb6700d795ccbae25edf7a4338ffc43a` |

Every player on a Purpur server needs the client mod of the **same version** (0.3.0). An older client gets a clear
"update the mod" message instead of joining.

## Starting a Purpur server

Put the plugin into `plugins/`, put the BACAP datapacks into `<world>/datapacks`, and add one Java option:

```text
java -javaagent:plugins/AchieveToDo-Purpur-26.2-0.3.0.jar -jar purpur-26.2-2620.jar --nogui
```

The plugin sets up everything else on the first start and logs `ATD_PRODUCT_GAMEPLAY_READY=true` when players can
join. For a preset other than NORMAL on a new world, add `-Dachievetodo.progression.initialConfig=<easy|hard|chaos>`
to that first start only. The full guide is
[STARTING_THE_SERVER.md](../../../port/AchieveToDo-Purpur-26.2/STARTING_THE_SERVER.md).

Upgrading from 0.2.1: replace both jars (server plugin and every client mod) and change the jar name in `-javaagent`.
The world and `plugins/AchieveToDo/` stay as they are.

## Changes since 0.2.1

- **No more "Preview only" banner.** The server runs in its release mode and reports itself ready; the debug header
  in the advancements screen is gone.
- **No player cap.** The 16-player limit is removed.
- **Readable messages** when the client mod is missing, has a different version, or the server is still syncing
  (English and Russian).
- **Chat hover works on Purpur:** hovering an advancement or ability message in chat shows its title and description.
- **Shelf message:** clicking a locked shelf now says "You can't use the shelf" instead of naming the chest.
- **Fixes in both mods:**
  - CHAOS thresholds now depend only on the world seed;
  - a locked nether portal, End portal or end gateway no longer grants its "touch the portal" advancement;
  - locked shears on a snow golem or a wolf no longer grant an advancement.
- **Removed:** the old diagnostic server path, the `/atd` inventory menu and its resource pack.
- License: MIT, also declared in the mod metadata.

## Certification

The released files were certified 57/57 groups, with independent review per group, under exactly this start.
See [docs/certification/0.3.0-purpur-26.2.md](../../certification/0.3.0-purpur-26.2.md). Both jars build
byte-identical from a clean clone of the tagged source.

## Known issues

See [known-issues.md](../../known-issues.md). Not yet audited: other landmark denials for the advancement effect
fixed above for shears.
