# AchieveToDo 0.2.1 — Minecraft 26.2

Stable release of the Fabric mod and its Purpur server port.
Runtime: Minecraft 26.2, Java 25, Purpur build 2620. All 153 gameplay abilities are retained.

## Downloads

| File | For | SHA-256 |
|---|---|---|
| `achievetodo-mc26.2+0.2.1.jar` | Fabric client (and single player) | `5c473903ec956489185be7d2f5c7803bd82a217c9ec066c1d3ffb5a2f5925a99` |
| `AchieveToDo-Purpur-26.2-0.2.1.jar` | Purpur 26.2 server plugin | `da9360aa48e7c92bf1b093b57bc46d36b9e23339920de02b2edb88e9e44cb540` |

## Starting a Purpur server

Put the plugin into `plugins/`, put the BACAP datapacks into `<world>/datapacks`, and add one Java option:

```text
java -javaagent:plugins/AchieveToDo-Purpur-26.2-0.2.1.jar -jar purpur-26.2-2620.jar --nogui
```

The plugin sets up everything else on the first start. For a preset other than NORMAL on a new world, add
`-Dachievetodo.progression.initialConfig=<easy|hard|chaos>` to that first start only. The full guide, including
the startup errors and what they mean, is
[STARTING_THE_SERVER.md](../../../port/AchieveToDo-Purpur-26.2/STARTING_THE_SERVER.md).

## Changes since 0.2.1-rc.1

- **One-option server start.** Starting the server now needs only `-javaagent:` instead of 14 options. If the agent
  is missing, the server stops and prints the correct command instead of running the old, uncertified path.
- **Fixes:**
  - denied item uses no longer grant criteria or desync held items;
  - the client no longer predicts item uses that the server refuses.
- No gameplay, advancement, reward, progression, team or save-schema change.

## Certification

The released files were certified 57/57 groups, with independent review per group, under exactly this start.
See [docs/certification/0.2.1-purpur-26.2.md](../../certification/0.2.1-purpur-26.2.md). Both jars build
reproducibly from the tagged source.

## Known issues

- CHAOS thresholds are not reproducible from the world seed across JVM starts.
- Touching a locked End portal or end gateway still grants the vanilla advancement for touching it.

Both behave the same way as the Fabric original. See [known-issues.md](../../known-issues.md).
