# 0.2.1 operator launch: design note

Status: shipped in 0.2.1 and certified (rc.2, [0.2.1-purpur-26.2.md](../../certification/0.2.1-purpur-26.2.md)).
This note is the original proposal, kept as written. In 0.3.0 the derived mode is called `release` instead of
`certification`.

## Result

An operator now starts the server with one extra option:

```text
java -javaagent:plugins/<plugin jar> -jar purpur-26.2-2620.jar --nogui
```

The jar then runs the same certified composition (gameplay mode `CERTIFICATION`, native guard path) that rc.1
reached with 14 `-D` options. The operator guide is `port/AchieveToDo-Purpur-26.2/STARTING_THE_SERVER.md`.

## How it works

1. **Premain** (`core/ReleaseLaunchPremain`, the new `Premain-Class`). If `-Dachievetodo.gameplay.mode` is absent, it
   sets the mode `certification` and the five switches (`bridge.real`, `progression.foundation`,
   `bridge.groupScope`, `landmarks.exactPrototype`, `bridge.spike`) to `true` as normal system properties. It also
   sets `achievetodo.launch.profile=release`. Then it calls the unchanged
   `NativeTransactionPremain.premain`. Every agent reads the same property values as in the rc.1 command.
2. **Bootstrap** (release profile only). It derives `achievetodo.bacap.directory` = `<level-name>/datapacks` from
   `server.properties`.
3. **onEnable** (release profile only). This runs after the agent/plugin identity check and before admission
   (`ReleaseLaunchProfile.deriveInstallation`).
4. **Admission** is unchanged. `StartupModePolicy.decide` and the original `ATD_GAMEPLAY_MODE_PRECONDITIONS`
   check see a complete certified property set.

An operator value always wins. If `-Dachievetodo.gameplay.mode` is given at all, nothing is derived and no file
is written: the rc.1 command behaves exactly as before. An explicit switch that contradicts the certified
value (for example `-Dachievetodo.bridge.spike=false`) is never overridden. The mode then stays unset, and the
plugin refuses with the conflict named.

## Flag by flag

| Flag | Release launch | Source / safeguard |
| --- | --- | --- |
| `-javaagent:` | **still required** | see "Why the agent flag stays" |
| `gameplay.mode=certification` | derived (premain) | constant |
| `bridge.real`, `progression.foundation`, `bridge.groupScope`, `landmarks.exactPrototype`, `bridge.spike` | derived (premain) | constants; a contradicting explicit value fails closed |
| `bacap.directory` | derived (bootstrap) | `server.properties` `level-name`; checked in onEnable against the loaded overworld's level folder (`ATD_LAUNCH_BACAP_DIRECTORY`); the BACAP content pins are unchanged |
| `bridge.server` | derived (onEnable) | random UUID, written once to `plugins/AchieveToDo/server-identity.properties`; a damaged file fails closed |
| `progression.world`, `progression.worldKey` | derived (onEnable) | overworld of `world-group.properties` |
| `world-group.properties` (file) | written once on a fresh install | only when neither it nor `world-selection.properties` exists; binds `minecraft:overworld`, `minecraft:the_nether`, `minecraft:the_end` by key (with role check); `groupId=main`, random `configId`. Missing while a selection exists: fails closed (`ATD_LAUNCH_WORLD_GROUP_MISSING`) |
| `progression.freshConfig=true` | derived only on the start that wrote the world group | otherwise the existing "missing saved selection" refusal stays |
| `bridge.world`, `bridge.worldKey`, `bridge.config`, `bridge.group` | dropped | no code reads them (they were only in the certified command line) |

## Why the agent flag stays

The agents cannot be attached later from the plugin:

- They verify the SHA-256 of server classes and rewrite them **before the first definition**. By the time any
  plugin code runs, `Entity`, `Player`, `MinecraftServer` and the other targets are already loaded. Only
  retransformation would remain, which cannot change class shape. It would also be a different, uncertified code path.
- Self-attach (Attach API) needs `-Djdk.attach.allowAttachSelf=true` or a helper process. Since JDK 21 (JEP 451),
  dynamic agent loading also warns and is meant to be disallowed by default
  (`-XX:+EnableDynamicAgentLoading`). That is still a JVM flag, just a worse one.
- `Launcher-Agent-Class` works only when this jar is the `java -jar` main jar. That would be a wrapper launcher
  around Purpur: a different composition and more surprising for operators.

So the minimum launch is the single `-javaagent:` option.

## Mode naming and gating

- The internal mode stays `CERTIFICATION` (with `bridge.spike=true`). Renaming it, or dropping the `spike`
  requirement, would change string checks inside certified code (`NativeSourceReloadAgent`,
  `BukkitGameplayRuntime`, `BukkitBridgeRuntime`, `AchieveToDoPlugin`). Operators never type it. The guide calls it
  the normal start, and the log shows `ATD_LAUNCH_PROFILE=release` followed by `ATD_GAMEPLAY_MODE=CERTIFICATION`.
- `PRODUCT` is unchanged and unreachable without a deployment-certificate capsule. It is not part of 0.2.1.
- `diagnostic` (legacy path) is unchanged: explicit opt-in only, with a SEVERE warning.

## What changed in the jar (re-certification scope for rc.2)

Unchanged source files: all `nativeguard/*` agents including `NativeTransactionPremain.java`, all bindings, the
restriction engine, `BacapPacks`, `ProductGameplayAdmission`, `BukkitProgressionRuntime`, and
`BukkitBridgeRuntime`.

Changed or new:

- `build.gradle.kts`: `Premain-Class` → `core.ReleaseLaunchPremain`, plus a new `releaseLaunchTest` in `check`.
- New: `core/ReleaseLaunchPremain`, `core/ReleaseLaunchDefaults`, `core/ReleaseLaunchProfile`.
- `core/AchieveToDoPlugin`: refuses early when the mode is missing, calls the release derivation, uses the real
  jar name and the 26.2 level folder in hints.
- `core/StartupModePolicy`: one-line refusal text, a conflict message, and the jar name in guidance.
- `bootstrap/AchieveToDoBootstrap`: derives the BACAP directory (release profile) and uses the real jar name in
  guidance.

Certified behaviours that could be affected, and so need to be re-run in rc.2:

1. **Agent start order.** Premain now enters through a new class. `ATD_NATIVE_BOOTSTRAP classes=56` and every
   `ATD_*_TRANSFORM` pin line still appear in the smoke runs, but E1 (startup/agents) must be re-run.
2. **Startup admission and the fail-closed paths**: the new early refusal, the new `ATD_LAUNCH_*` refusals, and
   the binding written on first start.
3. **Progression initialisation on a fresh world.** `freshConfig` and `initialConfig=normal` are now implied on
   the first release start. Re-run the first-start and restart (E-group) scenarios with the release command.
4. **Bridge handshake** with a generated server id instead of a fixed one.
5. **Artifact identity**: new jar SHA-256. The certification authority files that pin source digests
   (`tools/certification/authority/*`) list `build.gradle.kts` and `AchieveToDoPlugin.java`, which changed.
   `NativeTransactionPremain.java` did not change.

Recommendation: certify rc.2 with the **release command** (only `-javaagent`) as the primary harness launch, and keep
one run with the explicit rc.1 command to show both reach the same composition.

## Risks

- **Trust on first use.** The first release start binds whatever the vanilla dimension keys point to. From then on,
  a swapped world fails closed exactly as before (`WORLD_UNBOUND`, and `world-selection.properties` also records
  the world UUID). The `progression.world` assertion becomes a copy of the world-group file instead of a second,
  independent operator statement. This is accepted for usability; the explicit command keeps the old double
  assertion.
- **Deleted data folder.** Deleting `plugins/AchieveToDo/` on an existing world re-binds the world and generates a
  new server id. Clients see a new server, and progression config is re-initialised (`normal`). The guide says to
  back the folder up together with the world.
- **Custom world locations** (`--world-dir`, `--universe`, `--level-name` command-line options, or `bukkit.yml`
  world container). The derived BACAP path is then wrong. The server stops with `ATD_LAUNCH_BACAP_DIRECTORY` or
  a missing BACAP file, and the fix is one `-D` option.
- **Hosts that do not allow JVM arguments** cannot run AchieveToDo at all. This was already true.
- `-Dachievetodo.launch.profile=release` given by hand together with an explicit mode turns derivation on for
  absent values. This is harmless, and undocumented on purpose.

## Verification done

- `tooling/run-purpur-tests.sh` (pointed at this worktree): BUILD SUCCESSFUL, all `check` tasks green, including the
  new `ATD_RELEASE_LAUNCH PASS cases=79` and `ATD_STARTUP_MODE_POLICY PASS cases=142`.
- Smoke runs on a scratch Purpur 26.2-2620 server (fresh flat world, BACAP in `world/datapacks`):
  - only `-javaagent`, first start: `ATD_LAUNCH_PROFILE=release`, `ATD_GAMEPLAY_MODE=CERTIFICATION`, `Done`; both
    files written.
  - second start: same server id, files unchanged, no `freshConfig`.
  - no agent: `ATD_GAMEPLAY_MODE_REQUIRED` with the real jar name; the server stops before any world loads.
  - `-Dachievetodo.bridge.spike=false`: conflict named; the server stops before any world loads.
  - explicit rc.1 command: no `ATD_LAUNCH_PROFILE` line; `CERTIFICATION`; `Done`.
- **Not done:** players joining, gameplay denials, or the bridge with a real client. Those belong to the rc.2 E1–E6 runs.

## Follow-ups (not in this branch)

- `docs/releases/0.2.1-rc.1/INSTALLATION-ROLLBACK.md` omits `-Dachievetodo.gameplay.mode=certification`. It
  should be replaced by the new guide for 0.2.1.
- Pre-existing bug, now fixed in hints and the cross-check: on 26.2, `World.getWorldFolder()` is
  `<level>/dimensions/minecraft/overworld`, not the level folder. The old hint suggested a wrong BACAP path.
