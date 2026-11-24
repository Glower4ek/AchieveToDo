# AchieveToDo 0.4.2 — Minecraft 26.3

A fix release for 0.4.1. **Clicking an advancement in chat now works with AdvancementsSearch 1.3.1**: the screen
opens on the advancement and flashes it, without the red "could not open" message.
Runtime unchanged: Minecraft 26.3, Java 25, Purpur build 2660, BACAP 1.21.1.

## Downloads

| File | For | SHA-256 |
|---|---|---|
| `achievetodo-mc26.3+0.4.2.jar` | Fabric client (and single player), Fabric Loader 0.20.1+, Fabric API 0.166.2+26.3 | `613ab3041313df707beaf4f19da244f69273bac2171c9cef1c45f6c1f1fe404f` |
| `AchieveToDo-Purpur-mc26.3+0.4.2.jar` | Purpur 26.3 server plugin | `67d8c5caabf45f4eead2941e84d04a52336fca8068bfd54c7f4fbec5c8937f35` |

Client and server versions must match: update the plugin and every player's mod together.

## Upgrading from 0.4.1

Replace both jars and change the jar name in `-javaagent`:

```text
java -javaagent:plugins/AchieveToDo-Purpur-mc26.3+0.4.2.jar -jar purpur-26.3-2660.jar --nogui
```

The world, `plugins/AchieveToDo/` and the BACAP datapacks stay as they are. Upgrading from 0.3.0: follow the 0.4.0
notes ([0.4.0](../0.4.0/RELEASE-NOTES.md)) with the 0.4.2 jars.

## Changes since 0.4.1

- **AdvancementsSearch 1.3.1.** Chat advancement links use the 1.3.1 highlight and fall back to the 1.3.0 one
  (known issue 10).

## Certification

0.4.2 changes only the client chat-link integration and the version. It is certified on top of the 0.4.0 campaign by
a short run on the 0.4.2 jars: see [docs/certification/0.4.2-purpur-26.3.md](../../certification/0.4.2-purpur-26.3.md).
Both jars build byte-identical from a clean clone of the tagged source.

## Known issues

See [known-issues.md](../../known-issues.md).
