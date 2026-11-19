# Starting the server

Only one way to run AchieveToDo on Purpur 26.2 is certified: the native guard path in gameplay mode
`certification`. See `docs/certification/w5-purpur-26.2.md` at the repository root.

## What happens if you start without the flags

The plugin refuses to run. It logs `ATD_GAMEPLAY_MODE_REQUIRED` and the full start command, then stops the
server (`ATD_STARTUP_FAIL_CLOSED`). It does not disable only itself, because that would leave a server with no
restrictions at all.

Older builds did something different. Without `-Dachievetodo.gameplay.mode` they quietly ran the legacy Bukkit
restriction path. That path is not certified and has known gaps. For example, copper armor is never blocked on it.

If the mode is `certification` but a required value is missing or invalid, the plugin logs
`ATD_GAMEPLAY_MODE_PRECONDITIONS`, names each missing value, and stops the server the same way.

## One-time setup per world

1. Put `bacap.zip` and `bacap_override.zip` into `<world>/datapacks` (see `S3A_PROGRESSION.md`).
2. Find the world UUIDs. Start once with only `-javaagent:plugins/achievetodo.jar` and
   `-Dachievetodo.bacap.directory=<absolute path of world>/datapacks` (without that directory the server stops
   earlier with `BACAP_MISSING`, before any world loads). The plugin then refuses to run, but first lists every
   loaded world as `overworld.uuid=... overworld.key=...` (also nether and end). The server stops before it
   accepts players.
3. Create `plugins/AchieveToDo/progression/world-group.properties` (format in `S3B_SEMANTICS.md`). It holds
   `groupId`, `configId` and the overworld/nether/end UUIDs and keys.
4. Pick a server UUID once (in PowerShell: `[guid]::NewGuid()`). Keep it. Do not change it between starts.
5. On the very first start only, add `-Dachievetodo.progression.freshConfig=true` (and optionally
   `-Dachievetodo.progression.initialConfig=normal`). Remove it for every later start.

## Start command

Use one line. The agent and the plugin must be the same file.

```text
java -Xms256M -Xmx1536M -javaagent:plugins/achievetodo.jar
  -Dachievetodo.gameplay.mode=certification
  -Dachievetodo.bridge.real=true
  -Dachievetodo.progression.foundation=true
  -Dachievetodo.bridge.groupScope=true
  -Dachievetodo.landmarks.exactPrototype=true
  -Dachievetodo.bridge.spike=true
  -Dachievetodo.bacap.directory=<absolute path of world>/datapacks
  -Dachievetodo.progression.world=<overworld UUID>
  -Dachievetodo.progression.worldKey=minecraft:overworld
  -Dachievetodo.bridge.server=<your server UUID>
  -Dachievetodo.bridge.world=<overworld UUID>
  -Dachievetodo.bridge.worldKey=minecraft:overworld
  -Dachievetodo.bridge.config=<configId from world-group.properties>
  -Dachievetodo.bridge.group=<groupId from world-group.properties>
  -jar purpur-26.2-2620.jar --nogui
```

- `progression.world` and `progression.worldKey` must equal `overworld.uuid` and `overworld.key` in
  `world-group.properties`, or startup fails with `WORLD_IDENTITY_MISMATCH`.
- `bridge.world`, `bridge.worldKey`, `bridge.config` and `bridge.group` are not read by this plugin build. The
  plugin takes world, config and group from `world-group.properties`. They are listed because every certified run
  used them; keep them so your start matches the certified command.
- Do not add `-Dachievetodo.gameplay.mode=product`. It needs a deployment certificate and is not part of this
  release.

The plugin does not fill in these values itself. They are assertions that this is the world and server you meant.
If the plugin copied them from whatever world happened to load, a swapped or restored world folder would pass
unnoticed.

A correct start logs `ATD_GAMEPLAY_MODE=CERTIFICATION`.

## Legacy path (testing only)

`-Dachievetodo.gameplay.mode=diagnostic` still starts the old, uncertified legacy path. The plugin then logs
`ATD_UNCERTIFIED_LEGACY_PATH` on every start. Never use it with real players.
