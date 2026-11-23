# Starting the server

AchieveToDo for Purpur 26.2 needs one extra Java option: the plugin jar is also loaded as a Java agent.
Nothing else is needed. The plugin works out the rest itself and keeps it in two small files.

## Requirements

- Java 25.
- Purpur 26.2 build 2620 (`purpur-26.2-2620.jar`). Other builds are refused with `ATD_UNPINNED_RUNTIME`.
  Paper and other forks are not supported.
- Exactly one AchieveToDo jar in `plugins/`.
- The BACAP datapacks `bacap.zip` and `bacap_override.zip` prepared by the AchieveToDo mod (see below).
- A host where you can edit the Java start command. Hosting panels usually call this "JVM arguments",
  "additional flags" or "startup command". If your host does not allow it, AchieveToDo cannot run there.
- **Every player needs the AchieveToDo Fabric mod of the same version as the plugin.** A player without it, or with
  another version, is disconnected at login with a message that says which version to install.

## Getting the BACAP datapacks

The plugin does not ship BACAP. The AchieveToDo Fabric mod downloads it and prepares the exact files the plugin
expects:

1. Start Minecraft 26.2 with the AchieveToDo mod and create any single-player world once.
2. Copy `bacap.zip` and `bacap_override.zip` from the `datapacks` folder of your Minecraft directory
   (`.minecraft/datapacks`, or the instance folder of your launcher).

Do not edit or rename them. Other BACAP files, including the raw download from Modrinth, are refused at startup.

## Install

1. Stop the server.
2. Put the plugin jar into `plugins/`. Remove any older AchieveToDo jar.
3. Put `bacap.zip` and `bacap_override.zip` into `<world>/datapacks`. `<world>` is the folder named by
   `level-name` in `server.properties` (by default `world`).
4. Add `-javaagent:plugins/<plugin jar name>` to the start command, before `-jar`:

```text
java -Xms1G -Xmx4G -javaagent:plugins/AchieveToDo-Purpur-26.2-0.3.0.jar -jar purpur-26.2-2620.jar --nogui
```

Keep your own memory settings. The file after `-javaagent:` must be the same file as the one in `plugins/`;
otherwise the server stops with `ATD_AGENT_PLUGIN_JAR_MISMATCH`. When you update the plugin, update both by
replacing that one file.

5. Start the server. A correct start logs:

```text
ATD_LAUNCH_PROFILE=release; derived=[...]
ATD_GAMEPLAY_MODE=RELEASE
ATD_PRODUCT_GAMEPLAY_READY=true; players with the AchieveToDo 0.3.0 client mod can join
```

Players can join once the last line appears, usually right after `Done`. Until then they are told the server is
still starting.

## What the plugin sets up on the first start

| What | Where it comes from | Kept in |
| --- | --- | --- |
| BACAP folder | `level-name` in `server.properties`, then checked against the loaded world | nothing; worked out each start |
| Server id | a random id, created once | `plugins/AchieveToDo/server-identity.properties` |
| World binding | the worlds `minecraft:overworld`, `minecraft:the_nether`, `minecraft:the_end` | `plugins/AchieveToDo/progression/world-group.properties` |
| Progression preset | `normal` on the first start | `plugins/AchieveToDo/progression/world-selection.properties` |

The two files are written once and never changed afterwards. On every later start the plugin checks the loaded
worlds against `world-group.properties`. If you swap or restore a different world folder, the server stops
(`WORLD_UNBOUND`) instead of mixing progress between worlds.

**Back up `plugins/AchieveToDo/` together with the world.** Do not delete these files to "reset" something:

- `server-identity.properties` is how players' clients recognise this server.
- If `world-group.properties` is lost while saved progression exists, the server refuses to start
  (`ATD_LAUNCH_WORLD_GROUP_MISSING`) rather than guess. Restore it from the backup.

To start with a different preset on a new world, add `-Dachievetodo.progression.initialConfig=<easy|hard|chaos>` to
the very first start only.

## If the server stops at startup

AchieveToDo never runs a server without its protection. When something is wrong it logs the reason and stops
the whole server (`ATD_STARTUP_FAIL_CLOSED`).

| Log line | Meaning | Fix |
| --- | --- | --- |
| `ATD_GAMEPLAY_MODE_REQUIRED ... agent is not loaded` | `-javaagent:` is missing | add it as shown above |
| `ATD_GAMEPLAY_MODE_REQUIRED ... sets -Dachievetodo...` | an old `-Dachievetodo.*=false` option contradicts the release setup | remove that option |
| `ATD_UNKNOWN_GAMEPLAY_MODE` | an old `-Dachievetodo.gameplay.mode=diagnostic` or `=product` option; both were removed in 0.3.0 | remove that option |
| `BACAP_MISSING` or a missing `bacap.zip` | the datapacks are not in `<world>/datapacks` | copy them there |
| `ATD_LAUNCH_BACAP_DIRECTORY` | the world is not where `server.properties` says (custom world folder) | add `-Dachievetodo.bacap.directory=<absolute path>/datapacks` |
| `ATD_LAUNCH_WORLD_GROUP_MISSING` | `world-group.properties` was deleted | restore it from a backup |
| `ATD_LAUNCH_SERVER_IDENTITY` | `server-identity.properties` is damaged | restore it from a backup |
| `WORLD_UNBOUND` | the loaded world is not the bound one | start with the right world folder |
| `ATD_AGENT_PLUGIN_JAR_MISMATCH` | the agent and the plugin are different files | point `-javaagent:` at the jar in `plugins/` |
| `ATD_DUPLICATE_INSTALLATION` | more than one AchieveToDo jar in `plugins/` | keep only one |

## Explicit start (advanced)

The 0.2.1-rc.1 certification used a start command that sets every value by hand
(`docs/certification/w5-purpur-26.2.md`). It still works: as soon as `-Dachievetodo.gameplay.mode` is given
(`release`, or its old name `certification`), the plugin derives nothing and writes no files. Any single value can
also be given by hand on a normal start (for example `-Dachievetodo.bacap.directory=...` or
`-Dachievetodo.bridge.server=<uuid>`); a value you give always wins over the derived one.

`-Dachievetodo.bridge.world`, `bridge.worldKey`, `bridge.config` and `bridge.group` from the old command are not
read by this build.

## History

Up to 0.2.1 the plugin also contained an older, uncertified "legacy" restriction path and an inventory-based
ability browser (`/atd`). Both were removed in 0.3.0. The design notes of the port's development are kept in
[docs/history](docs/history/).
