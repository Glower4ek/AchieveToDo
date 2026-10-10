![Logo](https://cdn.modrinth.com/data/cached_images/aea8fb87daa644150ed280db7ac94e0811fe40bd.png)

Minecraft, but the world has stolen your abilities. Reclaim them one by one by completing BlazeandCave's advancements!

## About

Current project owner and maintainer: **Glower4ek**. Maintained source: [Glower4ek/AchieveToDo](https://github.com/Glower4ek/AchieveToDo).

Current release: **AchieveToDo 0.1.5.5** for **Minecraft 26.2**, requiring **Java 25**.

This project keeps the vanilla feel—no overpowered features—but flips progression on its head. Instead of starting with every ability, you must earn them through advancements. Explore alternate paths, delve into dungeons, and celebrate every basic resource you manage to unlock.

Built to showcase [BlazeandCave's Advancements Pack](https://modrinth.com/datapack/blazeandcaves-advancements-pack), the mod integrates over 1000 tasks that serve as both a challenge and a tutorial. Whether you’re a newcomer or a veteran, the locked mechanics will force you to rediscover Minecraft in a completely new, demanding way.

### ATD Modpack

You can also play this mod in [ATD modpack](https://modrinth.com/modpack/achievetodo-modpack). It already has BaC and various advancement-QoL mods installed, such as a large window, search, and detailed information about the criteria.

## Gameplay

Early on, you’ll notice a scoreboard on your screen showing your completed advancements:

![Replace this with a description](https://cdn.modrinth.com/data/cached_images/378d8348c437f328781564a630fbef5e83b5f0cf.png)

By default, you start the game with 1 advancement already unlocked (this acts as a little boost). As you earn more advancements, this counter goes up—so you’ll always know how close you are to unlocking the next ability!

In this world, **every ability gradually unlocks** as you complete advancements. You’ll find all abiliies on the AchieveToDo tab in your advancements screen:

![Replace this with a description](https://cdn.modrinth.com/data/cached_images/1722b7f33c6fbb216c62102f2ab845340acba5dc.png)

At first, you’ll see mostly *mystery icons*. The core idea is that **you won’t know** which abilities are locked until you try them. For instance, if you **attempt to jump** right after spawning, you’ll discover that jumping is restricted:

![Replace this with a description](https://cdn.modrinth.com/data/DTTu3Q4G/images/6f6f5ada98dc2204ad7768321ad0965e94f4fe09.png)

From that moment on, the “Jumping” ability is revealed in AchieveToDo tab, so you can track how many advancements you need to earn in order to unlock it:

![Replace this with a description](https://cdn.modrinth.com/data/DTTu3Q4G/images/835bd6f2cfe1c65029d02a9737174b71b6f58dcd.png)

Once you’ve completed the required advancements (7 in the illustrated configuration), a notification will pop up, proudly announcing **your new power to jump**:

![Replace this with a description](https://cdn.modrinth.com/data/DTTu3Q4G/images/3fd02a0562dfcf894a15f4a5286c44e8b7608c56.png)

This same system applies to a wide range of gameplay elements—from basic actions like opening your inventory or breaking blocks, to using specific items, trading with villagers, traveling to different dimensions, or even interaction with objects within structures. Each newly unlocked feature feels like its own mini-achievement in your overall journey!

## World Creation

Don't forget to check the mod settings tab before creating a world:

![Replace this with a description](https://cdn.modrinth.com/data/DTTu3Q4G/images/0fc6facdb1bf32bbba1d94b3671464ac1a7a135a.png)

### Difficulty Modes

**Easy** — Similar to Normal but with even quicker unlocks. All basic abilities are unlocked from the start.

**Normal** — *The recommended mode.* Similar to Hard but with **faster unlocks** to keep you constantly engaged. Only vision is available by default among the basic abilities.

**Hard** — A more RPG-like journey, centering advancements over core AchieveToDo mechanics. Unlocks follow **all** BlazeandCaves advancements, and the first advancement **must be obtained blindly**.

**Chaos** — Abilities get **randomly shuffled** based on the world seed, ensuring every run becomes an unpredictable.

---

<details>
<summary>Custom Configuration</summary>

This makes it easy to create all kinds of custom challenges or special gameplay rules—like giving players powerful abilities from the start or removing entire mechanics altogether.

1. **Launch with a Built-In Mode**. First, start Minecraft with one of the existing AchieveToDo difficulty modes (e.g., easy, normal, hard, or chaos). This will generate the corresponding configuration file in your `config/achievetodo` folder (for example, `normal.toml`).
2. **Locate and Copy the Config File**. Go to `config/achievetodo` in your Minecraft directory, find the file that matches the difficulty you just launched (e.g., `normal.toml`)—copy and rename it to something unique like `custom.toml`.
3. **Edit the Custom Config**. Open `custom.toml` (or whatever you named your file), look for the `[abilities]` section, where each line determines how many advancements are required to unlock a specific ability:
   - **0** (or omitting the line entirely) means the ability is **unlocked by default**.
   - **-1** means the ability is **permanently locked** and cannot be unlocked.
   - Any positive number sets the required number of advancements before the ability becomes available.
4. **Use Your Custom Settings**. When creating a new world, look for the AchieveToDo difficulty switch and select *your custom difficulty* option (named after your new file).

</details>

<details>
<summary>Server-side setup</summary>

1. **Create a Single-Player World**. In the world creation menu, enable all the settings you plan to use on your server (e.g., hardcore mode, Terralith, co-op, rewards, etc.). Review each difficulty’s description (easy, normal, hard, chaos) to understand how it affects gameplay.

2. **Check Data Packs**. Once the single-player world has been generated, open it and run the `/datapack list` command. Take note of which data packs are active, including any external packs (like `bacap.zip`) and AchieveToDo’s built-in overrides (e.g., `achievetodo:bacap_override`).

3. **Replicate the Setup**. Upload the same external `.zip` data packs from your single-player world to your server’s datapacks folder, enable them in the same order shown by the `/datapack list` command and make sure to also enable the corresponding AchieveToDo override packs (e.g., `achievetodo:bacap_override`) in the **same order**.

4. **Configure the Server**. In your server’s `server.properties` file, create the `achievetodo-configName` parameter. Set this parameter to the difficulty name you selected when creating the single-player world (easy, normal, hard, or chaos).

5. **Custom Configuration (Optional)**. Instead of using one of the built-in difficulty modes, you can specify the name of a custom configuration file as the value for the `achievetodo-configName` parameter.

</details>

---

<details>
<summary>Known issues</summary>

- The [C2ME](https://modrinth.com/mod/c2me-fabric) mod is not supported and may be unstable with AchieveToDo. This is because AchieveToDo tracks structure generation to determine their sizes, but does not currently support multithreading.

- The "Goat Simulator" advancement is buggy in vanilla Minecraft, use [MC-264204 Fix](https://modrinth.com/mod/mc+264204-fix) to solve it.

</details>

## Building and verification

Use the installed Temurin Java 25.0.4+7 and the checked-in Gradle 9.8.0 wrapper from the repository root. Use an explicit Python executable (the reviewed local environment uses Python 3.14); do not rely on a PATH alias. The commands below use STRICT verification and online metadata resolution. Seed a disposable Gradle User Home with the 372 exact approved inputs before running them; a new empty cache is not an approved binary trust root. On Windows PowerShell:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-25.0.4.7-hotspot'
$env:ACHIEVETODO_PYTHON = 'C:\path\to\python.exe'
$env:ACHIEVETODO_TEST_INPUTS_DIR = 'C:\private\AchieveToDoInputs'
$env:GRADLE_USER_HOME = Join-Path (Get-Location) 'build/tmp/c7-local-verify/gradle-user-home'
$projectCache = Join-Path (Get-Location) 'build/tmp/c7-local-verify/project-cache'
$env:TEMP = Join-Path (Get-Location) 'build/tmp/codex_java_uds_probe'
$env:TMP = $env:TEMP
New-Item -ItemType Directory -Force -Path $env:TEMP | Out-Null
.\gradlew.bat --no-daemon --no-watch-fs --project-cache-dir $projectCache --dependency-verification strict verifyCleanSourceInputs
.\gradlew.bat --no-daemon --no-watch-fs --project-cache-dir $projectCache --dependency-verification strict compileJava compileTestJava compileGametestJava
.\gradlew.bat --no-daemon --no-watch-fs --project-cache-dir $projectCache --dependency-verification strict test --rerun-tasks
```

The default `test` task is the current standard verification entrypoint; the released 0.1.5.5 baseline is 435 tests with zero failures, errors or skips. The accepted Phase C runtime checkpoint requires 443 standard test cases; C7 autonomous verification executed all 443 cases with zero failures, errors or skips. Compilation of the 42 GameTest registration classes does not certify a new Minecraft runtime run. DISTRIBUTION-B requires the exact 20 original inputs listed in `src/test/resources/build_inputs/manifest.json` under the explicit `ACHIEVETODO_TEST_INPUTS_DIR` directory outside the checkout. This private local bundle contains nine original ZIPs, five acquisition JSONs, five metadata JSONs and the BSD template. Missing, extra or changed bytes and symlink/junction inputs fail closed; tests cannot use historical `build/tmp` fallback or automatically download originals. The selected dependency cache and resolved Minecraft assets are separate prerequisites; a fresh checkout alone does not supply them.

For a verification packaging probe, use the existing `jar` task with an isolated output configuration that validates every archive destination before task execution. The pinned Minecraft 26.2 setup has no `remapJar` task. Preserve the same-version guard and the historical published JAR; never use its destination for a changed probe artifact.

CACHE-BASELINE-LIMITED permits reviewed exact selected cache bytes as a limited reproducibility trust root; it does not certify publisher origin or trust all cache files. The native XML pins 178 selected binaries; STRICT rejected an isolated Gson checksum mismatch. The supplementary verifier enforces the 372 listed inputs, including their listed descriptors. `verify-metadata=false` leaves newly resolved POM/module metadata outside native checksum enforcement; newly acquired metadata also remains outside the 372-input manifest. Documented provenance does not grant publisher authentication or permanent trust. An isolated corrupt Gson witness proved supplementary rejection before Loom application, not universal pre-consumption ordering for Gradle metadata or all bootstrap inputs.

C7 tooling has actual compilation, full-suite, integrity-control and isolated packaging evidence. Metadata coverage remains limited as described above; no independent clean-source reproducibility claim follows. Future C8 clean-source and independent two-build certification has not started. The ignored `tools/build/verify_inputs.py` must be explicitly included with its exact reviewed bytes in a separately authorized checkpoint; source closure is not certified before that checkpoint exists.

Contributor references: [Minecraft 26.2 mixin rationale](docs/compatibility/mixin-26.2-map.md) and [historical 0.1.5.4 reproduction](docs/history/reproduction-0.1.5.4.md). Retained Phase A/B stage CLIs and the archived publication route describe historical checkpoints; they are not the current supported verification flow.

## Acknowledgments

- **[_Skrepka](https://www.youtube.com/@skrepkaq)** — My original inspiration for AchieveToDo, thanks to the creative Minecraft challenge videos (e.g., "How to complete Minecraft without X?").

- **[Lolotrack](https://www.youtube.com/@lolotrack_minecraft)** — For introducing me to BlazeandCave's Advancement Pack.

- **[Cavinator1](https://modrinth.com/user/Cavinator1)** — Creator of [BlazeandCave's Advancements Pack](https://modrinth.com/datapack/blazeandcaves-advancements-pack). AchieveToDo wouldn't be possible without it.

- **[Stardust Labs](https://modrinth.com/organization/stardust-labs)** — For providing the amazing world generation features across all three dimensions.
