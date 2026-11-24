# AchieveToDo 0.4.1 — Minecraft 26.3

A fix release for 0.4.0. **Opening the advancements screen crashed the game in 0.4.0**; 0.4.1 fixes it.
Runtime unchanged: Minecraft 26.3, Java 25, Purpur build 2660, BACAP 1.21.1.

## Downloads

| File | For | SHA-256 |
|---|---|---|
| `achievetodo-mc26.3+0.4.1.jar` | Fabric client (and single player), Fabric Loader 0.20.1+, Fabric API 0.166.2+26.3 | `c6cbfd1f76a0849ec1cde0d2477f728fd53d4500080835f64a83766a33e85450` |
| `AchieveToDo-Purpur-mc26.3+0.4.1.jar` | Purpur 26.3 server plugin | `64a18f432b95d3b0dec96bb7dc92c96228be3432eaf7199f04a1fc6fa5405898` |

Client and server versions must match: update the plugin and every player's mod together.

## Upgrading from 0.4.0

Replace both jars and change the jar name in `-javaagent`:

```text
java -javaagent:plugins/AchieveToDo-Purpur-mc26.3+0.4.1.jar -jar purpur-26.3-2660.jar --nogui
```

The world, `plugins/AchieveToDo/` and the BACAP datapacks stay as they are. Upgrading from 0.3.0: follow the 0.4.0
notes ([0.4.0](../0.4.0/RELEASE-NOTES.md)) with the 0.4.1 jars.

## Changes since 0.4.0

- **Advancements screen no longer crashes.** The abilities tab needed every category to split into equal rows; the new
  abandoned camp made the landmark row count odd. Rows may now differ by one ability.

## Certification

0.4.1 changes only the client layout of the abilities tab and the version. It is certified on top of the 0.4.0
campaign by a short run on the 0.4.1 jars: see
[docs/certification/0.4.1-purpur-26.3.md](../../certification/0.4.1-purpur-26.3.md). Both jars build byte-identical
from a clean clone of the tagged source.

## Known issues

See [known-issues.md](../../known-issues.md).
