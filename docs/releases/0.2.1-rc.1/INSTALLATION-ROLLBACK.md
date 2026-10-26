# Installation and rollback

Use Minecraft 26.2, Java 25 and Purpur 2620; use Fabric Loader 0.19.5 and
Fabric API 0.161.0+26.2 for the Fabric mod. Verify SHA256SUMS before installation.
Stop the server/client. Back up complete worlds, player progression, plugin
configuration, matching old JARs and startup arguments while stopped.

Install AchieveToDo-Fabric-26.2-0.2.1-rc.1.jar in the compatible mods directory.
Keep exactly one AchieveToDo Fabric mod. Install the Purpur JAR as
plugins/achievetodo.jar and load that exact same file as the Java agent:

```text
java -Xms256M -Xmx1536M -javaagent:plugins/achievetodo.jar -jar purpur-26.2-2620.jar --nogui
```

Do not mix agent/plugin bytes, install duplicate copies or substitute a floating
server build. Existing default feature switches are retained. The fully tested
bridge/progression profile additionally uses the following explicit JVM
properties before -javaagent (replace operator-specific bindings consistently):

```text
-Dachievetodo.bridge.real=true
-Dachievetodo.bridge.spike=true
-Dachievetodo.progression.foundation=true
-Dachievetodo.bacap.directory=<absolute directory containing bacap.zip and bacap_override.zip>
-Dachievetodo.progression.world=<qualified world UUID>
-Dachievetodo.progression.worldKey=minecraft:overworld
-Dachievetodo.bridge.server=<configured server UUID>
-Dachievetodo.bridge.world=<same qualified world UUID>
-Dachievetodo.bridge.worldKey=minecraft:overworld
-Dachievetodo.bridge.config=<configured configuration UUID>
-Dachievetodo.bridge.group=<qualified group name>
-Dachievetodo.bridge.groupScope=true
-Dachievetodo.landmarks.exactPrototype=true
```

Obtain external BACAP profiles lawfully and prepare the exact compat_26_2_r19
profile with sourceSha1 14da3f07b5467e8b59ffc0253fd8212c938cd739. Required content
digests are preserved in port/AchieveToDo-Purpur-26.2/src/main/resources/bacap/
s3a-profile.properties. A newly downloaded upstream pack is not an equivalent
prepared profile. The release assets do not supply these external full packs.
The evidence contains the actual tested launch command, world bindings and
runtime receipts; do not transplant its disposable identities into live worlds.

After startup verify readiness, bridge admission, progression state and expected
advancements. A datapack reload must resynchronize, and reconnect must restore
the session. Qualification covers one copied prior test world; no save-schema
migration was introduced and arbitrary-world compatibility is not asserted.

Rollback: stop both processes; restore the previous Fabric JAR and both old
agent/plugin bytes together, restore previous launch arguments/configuration,
and restore the complete stopped backup if saved state must be reverted.
Verify old SHA-256 identities before restart. Retain backups after uninstall.
The qualification receipt distinguishes the exercised artifact rollback from
this operator recovery procedure; production rollout is not claimed.
