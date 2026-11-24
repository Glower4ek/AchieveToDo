# AchieveToDo 0.4.0 — Minecraft 26.3

The Fabric mod and its Purpur server plugin, moved to Minecraft 26.3.
Runtime: Minecraft 26.3, Java 25, Purpur build 2660, BACAP 1.21.1. 154 gameplay abilities (one new).

## Downloads

| File | For | SHA-256 |
|---|---|---|
| `achievetodo-mc26.3+0.4.0.jar` | Fabric client (and single player), Fabric Loader 0.20.1+, Fabric API 0.166.2+26.3 | `e8577e1d28a7fb9b903eabfed7b9b0e5d5b15ad895be078d8617c6493ccde61c` |
| `AchieveToDo-Purpur-mc26.3+0.4.0.jar` | Purpur 26.3 server plugin | `16d0b2fa632887355eb3a56846d52177aa1b62556fad925205349e9ba220befa` |

Every player on a Purpur server needs the client mod of the **same version** (0.4.0). An older client gets a clear
"update the mod" message instead of joining.

## Starting a Purpur server

Put the plugin into `plugins/`, put the BACAP 1.21.1 datapacks into `<world>/datapacks`, and add one Java option:

```text
java -javaagent:plugins/AchieveToDo-Purpur-mc26.3+0.4.0.jar -jar purpur-26.3-2660.jar --nogui
```

The plugin sets up everything else on the first start and logs `ATD_PRODUCT_GAMEPLAY_READY=true` when players can
join. For a preset other than NORMAL on a new world, add `-Dachievetodo.progression.initialConfig=<easy|hard|chaos>`
to that first start only. The full guide is
[STARTING_THE_SERVER.md](../../../port/AchieveToDo-Purpur/STARTING_THE_SERVER.md).

Upgrading from 0.3.0 (Minecraft 26.2): back up the world, move the server to Purpur 26.3, replace the BACAP datapacks
with BACAP 1.21.1 (prepare `bacap.zip` again as the guide says), replace both jars (server plugin and every client mod)
and change the jar name in `-javaagent`. The world and `plugins/AchieveToDo/` stay; existing progression configs get
the new ability added on the first start.

## Changes since 0.3.0

- **Minecraft 26.3.** Both mods and all server-side guards are ported; the plugin folder is now `port/AchieveToDo-Purpur`.
  BACAP 1.21.1 with its new 26.3 advancements (poplar, Bonfire Night, the new maps).
- **New ability: abandoned camp.** The 26.3 abandoned camp is a lockable landmark like the igloo
  ("Interact inside an abandoned camp"; EASY 93, NORMAL 121, HARD 148 advancements).
- **Cartographer:** a denied trade shows its lock message at once.
- **No advancements from denied actions:** a denied use or interaction (for example a map put into an item frame, or a
  name tag, inside a locked landmark) no longer counts for "use" advancement criteria.
- **Golden dandelion** (new in 26.3) cannot lock a baby's age inside a locked landmark.
- **Water bucket:** right after joining, a locked water bucket can look empty for a moment; nothing happens on the server.
  Kept by design (see known issues).

## Certification

The released files were certified 59/59 groups, with independent review per group, under exactly this start.
See [docs/certification/0.4.0-purpur-26.3.md](../../certification/0.4.0-purpur-26.3.md). Both jars build
byte-identical from a clean clone of the tagged source.

## Known issues

See [known-issues.md](../../known-issues.md).
