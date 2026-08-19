# AchieveToDo 26.2 Phase A Audit

## 1. Executive summary

Overall feasibility: **realistic, but not trivial**.

Technical complexity: **high** because the project is crossing both:

- a Minecraft major-line compatibility jump from `1.21.4` to `26.2`
- the Fabric post-obfuscation toolchain change from Yarn-based development to Mojang-named / non-obfuscated development

Literal compatibility baseline realism: **yes, conditionally**. The repository is already structured around singleplayer plus integrated server, typed networking payloads, and data-driven progression configs, so a Phase A candidate that preserves the original AchieveToDo content and balance is still a credible target. The main risk is not the high-level game design; it is the combination of:

- mappings migration
- advancement UI mixin retargeting
- world-creation / datapack-flow screen changes
- landmark worldgen tracking mixins
- datapack/resource-pack technical format drift

Biggest risks:

- current source is still entirely Yarn-era named code plus a `named` access widener
- `26.2` Fabric development expects the non-obfuscated toolchain and `net.fabricmc.fabric-loom`
- advancement UI and rendering internals have shifted substantially since the old baseline
- landmark tracking relies on fragile structure/feature/chunk serialization hooks
- the frozen BACAP baseline in source is **not** the latest BACAP content and must not be silently replaced

Recommendation: **CONDITIONAL GO**

Why conditional instead of immediate GO:

- there is no evidence of a hard blocker
- there are several areas that should be prototyped in small vertical slices before bulk porting
- the historical BACAP compatibility datapack artifacts should be treated as preservation-critical inputs

## 2. Baseline repository state

Repository path: `D:\Vibecode\AchieveToDo 26.2`

Git baseline state:

- current branch: `26.2-port`
- immutable baseline tag: `pre-26.2-port`
- baseline branch: `origin/1.21.4+`
- `HEAD`, `pre-26.2-port`, and `origin/1.21.4+` currently all resolve to the same commit:
  - `e8095a2fb386c510b39c0dfd35071f6153480c50`
- worktree status: clean
- `git diff pre-26.2-port...HEAD`: empty

Baseline build/toolchain versions from the cloned repository:

| Item | Baseline |
| --- | --- |
| Minecraft target | `1.21.4` |
| Java target | `21` |
| Gradle wrapper | `8.12.1` |
| Fabric Loom | `1.9-SNAPSHOT` |
| Fabric Loader | `0.16.10` |
| Fabric API | `0.115.0+1.21.4` |
| Mappings | `net.fabricmc:yarn:1.21.4+build.8:v2` |
| Extra library | `com.moandjiezana.toml:toml4j:0.7.2` |

Baseline mod structure:

- entrypoints:
  - `main`: `com.diskree.achievetodo.AchieveToDoMod`
  - `client`: `com.diskree.achievetodo.client.AchieveToDoClient`
  - `fabric-datagen`: `com.diskree.achievetodo.ability.generation.AbilitiesGenerator`
- common/server logic lives in shared `src/main/java`
- client-only logic is also in `src/main/java`, separated by package and mixin environment rather than by a split source-set layout
- access widener file: `src/main/resources/achievetodo.accesswidener`
- mixin config: `src/main/resources/achievetodo.mixins.json`

Baseline inventories:

| Inventory | Count |
| --- | --- |
| Total mixins | `155` |
| Client mixins | `20` |
| Main/integrated-server mixins | `135` |
| Custom payloads | `9` |
| Ability types | `151` |
| Landmark types | `22` |
| Language files | `2` |
| Mod-owned GUI sprite textures | `3` |
| Built-in resource/datapack pack directories | `9` |
| Built-in override `mcfunction` files | `1214` |

Local Java environment during audit:

- `java -version`: `OpenJDK 25.0.4 LTS`
- `javac -version`: `25.0.4`
- `JAVA_HOME`: `C:\Program Files\Eclipse Adoptium\jdk-25.0.4.7-hotspot`

## 3. Baseline content freeze

Phase A content invariants supported by the current repository:

- no new abilities should be introduced
- no current abilities should be removed
- no threshold/balance changes should be introduced
- no BACAP content upgrade should be performed
- no newer BACAP localization sync should be performed
- no redesign of mystery, demystification, landmarks, or Chaos should be performed

### Ability inventory

- exact `AbilityType` count: `151`
- exact `LandmarkType` count: `22`

Ability IDs are defined in `src/main/java/com/diskree/achievetodo/ability/AbilityType.java`.

Examples from the frozen baseline:

- earliest abilities: `VISION`, `JUMP`, `SWIM`, `OPEN_INVENTORY`, `BREAK_BLOCKS`
- landmark-gated abilities: `INTERACT_INSIDE_VILLAGE`, `INTERACT_INSIDE_FORTRESS`, `INTERACT_INSIDE_END_CITY`, etc.
- late-game abilities: `TELEPORT_OUTER_ISLANDS`, `GLIDE_WITH_FIREWORKS`, `OPEN_BREWING_STAND`, `OPEN_ENCHANTING_TABLE`

### Progression configs

Built-in progression modes:

- `easy`
- `normal`
- `hard`
- `chaos`

Exact built-in progression properties from `Progressions.java` and `ChaosProgressionGenerator.java`:

| Mode | Initially unlocked | Permanently locked by default | Min required count | Max required count |
| --- | --- | --- | --- | --- |
| Easy | `VISION`, `JUMP` | none | `0` | `600` |
| Normal | `VISION` | none | `0` | `730` |
| Hard | none | none | `2` | `860` |
| Chaos | seed-based from Hard | probabilistic | `-1/0/2+` | generated up to `1000` |

Chaos-specific frozen semantics:

- base progression is derived from Hard mode
- `VISION` is always initially unlocked in Chaos
- additional abilities may become initially unlocked with a `1.0%` chance
- non-priority abilities may become permanently locked with a `0.2%` chance
- Chaos output is seed-sensitive and must remain mechanically identical after the port

Global progression constants:

- `TOTAL_ADVANCEMENTS_COUNT = 1152`
- initially unlocked flag = `0`
- permanently locked flag = `-1`

### Landmark inventory

Exact `LandmarkType` IDs:

- `DESERT_PYRAMID`
- `DESERT_WELL`
- `JUNGLE_PYRAMID`
- `PILLAGER_OUTPOST`
- `IGLOO`
- `SWAMP_HUT`
- `MANSION`
- `VILLAGE`
- `RUINED_PORTAL`
- `BURIED_TREASURE`
- `SHIPWRECK`
- `OCEAN_RUIN`
- `MONUMENT`
- `MONSTER_ROOM`
- `MINESHAFT`
- `TRAIL_RUINS`
- `ANCIENT_CITY`
- `TRIAL_CHAMBERS`
- `STRONGHOLD`
- `FORTRESS`
- `BASTION_REMNANT`
- `END_CITY`

### Config persistence semantics

Frozen config semantics:

- per-world selected config name is persisted into `level.dat`
- built-in modes auto-generate TOML files in `config/achievetodo`
- custom TOML files are supported
- missing ability entries default to `0`
- built-in config version is currently `1`

### Localization and owned assets

Frozen mod-owned localization:

- `assets/achievetodo/lang/en_us.json`
- `assets/achievetodo/lang/ru_ru.json`

Frozen mod-owned UI textures:

- `textures/gui/sprites/ability_mystified_mask.png`
- `textures/gui/sprites/ability_unlocked_notification_background.png`
- `textures/gui/sprites/advancements_tab_mystified_mask.png`

### Recommended freeze-check strategy

For implementation validation later, the safest machine-readable freeze check is:

1. serialize the generated `easy`, `normal`, and `hard` TOML outputs before porting
2. record exact `AbilityType` and `LandmarkType` enum name lists
3. record `Constants.TOTAL_ADVANCEMENTS_COUNT`
4. record exact internal pack names and enabled-order logic
5. for Chaos, generate a fixed set of seed snapshots and compare resulting TOML maps byte-for-byte after porting

This should be implemented later as a regression harness or task, not during this audit.

## 4. BACAP baseline

### Exact historical baseline identified in source

The source does **not** target latest BACAP. It pins older artifacts directly in `ExternalPack.java`.

Primary BACAP artifact pinned by source:

- title: `BlazeandCave's Advancements Pack (BACAP)`
- page: [Modrinth project page](https://modrinth.com/datapack/blazeandcaves-advancements-pack)
- direct download URL in source:
  - `https://cdn.modrinth.com/data/VoVJ47kN/versions/i8N5hYLH/BlazeandCave%27s%20Advancements%20Pack%201.18.1.zip`
- SHA-1 pinned in source:
  - `45b8bb0076bbf5b92fde7dc9590c6686937abbc0`

Hardcore add-on pinned by source:

- direct URL:
  - `https://cdn.modrinth.com/data/QEv1xmKi/versions/uRKM9Bou/BlazeandCave%27s%20Advancements%20Pack%20Hardcore.zip`
- SHA-1:
  - `ec5203496a822e6145562cd81e781ca0eea2c968`

Compatibility add-ons pinned by source:

- Terralith-version BACAP wrapper:
  - MediaFire URL in source
  - wrapper SHA-1 pinned
  - unwrapped pack SHA-1 pinned
- Amplified Nether-version BACAP wrapper:
  - MediaFire URL in source
  - wrapper SHA-1 pinned
  - unwrapped pack SHA-1 pinned
- Nullscape-version BACAP wrapper:
  - MediaFire URL in source
  - wrapper SHA-1 pinned
  - unwrapped pack SHA-1 pinned

Separate worldgen datapacks pinned by source:

- `Terralith_1.21_v2.5.7.zip`
- `Amplified_Nether_1.21_v1.2.7.zip`
- `Nullscape_1.21_v1.2.10.zip`

### What AchieveToDo depends on from that baseline

The code assumes:

- BACAP scoreboard objective naming:
  - `bac_advancements`
  - `bac_advancements_team`
- BACAP reward / config function layout
- a specific set of advancement IDs used by:
  - `TrackedScoreType`
  - `TrackedStatisticsDataType`
  - `TrackedNearbyEntitiesType`
  - `PlacedAdvancementMixin` custom ordering
  - many built-in override reward message functions
- exact advancement total ceiling:
  - `Constants.TOTAL_ADVANCEMENTS_COUNT = 1152`

That last constant is especially important: the **current** Modrinth BACAP page now says the pack has over `1000` new advancements and a total of `1202` advancements as of its August 18, 2026 crawl, which is already beyond the `1152` baseline expected by this repository. Phase A therefore cannot simply move to the current BACAP release without breaking the content freeze.

### Where AchieveToDo obtains BACAP

Singleplayer / world-creation flow:

- `CreateWorldScreenMixin` checks the global `datapacks` directory under the Minecraft run directory
- if a required external pack is missing, it opens `ExternalPackDownloader`
- for Modrinth-hosted packs, in-game HTTP download is supported
- for the older MediaFire-hosted compatibility packs, the UI falls back to manual file selection

### Is the source URL immutable?

Assessment:

- Modrinth CDN URLs that include both project ID and version ID are **likely effectively immutable**
- the generic Modrinth project page is mutable and now reflects newer BACAP content
- PlanetMinecraft and MediaFire pages/links are **not reliable immutable baselines**

### Is the historical artifact still obtainable?

Current evidence:

- the official Modrinth BACAP page still explicitly states that many older versions can be downloaded from its versions list and that exceptionally old links are stored on the page
- the Terralith compatibility page on PlanetMinecraft still references `1.18.3` compatibility variants

Conclusion:

- base BACAP and Hardcore historical artifacts are **likely still obtainable**
- the three older compatibility variants are **less trustworthy** because their provenance depends on PlanetMinecraft/MediaFire survival

### Exact advancement content/count if practical

Practical frozen count available from source:

- AchieveToDo assumes total obtainable BACAP advancement count ceiling `1152`

What is not proven from the repository alone:

- the exact external BACAP zip’s internal pack version label
- the exact external advancement file count without downloading and unpacking the preserved zips

That ambiguity should be reported honestly rather than guessed.

### Old BACAP artifact compatibility with 26.2

High-confidence conclusion: **not unchanged**

Reasons:

- all internal AchieveToDo built-in datapack/resource packs still declare `pack_format: 61`
- official Minecraft 26.2 technical notes report:
  - Data Pack version `107.1`
  - Resource Pack version `88.0`
- pack metadata rules changed in later versions, including pack-version major/minor handling

Therefore the frozen content can likely be migrated, but not by dropping the old pack in untouched and assuming it is current-format clean.

### Same-content migration approach recommended later

Mechanically migrate the old baseline by:

1. preserving the exact external zip artifacts currently referenced by source
2. unpacking them into a staging area
3. applying **technical-only** updates:
   - pack metadata version updates
   - any predicate / command / JSON syntax migrations required by 26.2
   - any tag path or registry-key format updates required by 26.2
4. explicitly forbidding import of newer BACAP advancement JSONs
5. validating the resulting advancement ID set against the frozen AchieveToDo assumptions

This is Phase A-compatible.

## 5. Current Minecraft/Fabric 26.2 requirements

Primary-source evidence used:

- [Fabric for Minecraft 26.2 blog post](https://fabricmc.net/2026/06/15/262.html)
- [Fabric Porting to 26.2 docs](https://docs.fabricmc.net/develop/porting/)
- [Fabric Loom docs](https://docs.fabricmc.net/develop/loom/)
- [Fabric Loader docs](https://docs.fabricmc.net/develop/loader/)
- [Fabric mappings migration docs](https://docs.fabricmc.net/develop/porting/mappings/index)
- [Fabric class tweaker docs](https://docs.fabricmc.net/develop/class-tweakers/index)
- [Minecraft Java Edition 26.2 technical notes](https://www.minecraft.net/en-us/article/minecraft-java-edition-26-2)
- [Fabric example mod 26.2 branch build.gradle](https://github.com/FabricMC/fabric-example-mod/blob/26.2/build.gradle)
- [Fabric example mod 26.2 branch gradle.properties](https://github.com/FabricMC/fabric-example-mod/blob/26.2/gradle.properties)

Current 26.2 toolchain evidence:

| Item | Current evidence |
| --- | --- |
| Java | JDK `25` |
| Gradle | `9.5.1` recommended in Fabric 26.2 blog |
| Loom | `1.17` / `1.17-SNAPSHOT` |
| Loader | `0.19.3` |
| Fabric API | verify at implementation time on Develop; official examples show `0.156.0+26.2`, recent Develop snippet exposed `0.154.2+26.2` |
| Plugin ID | `net.fabricmc.fabric-loom` for Minecraft `26.1+` |
| Mappings model | Mojang-named / non-obfuscated development, not Yarn |

Important architecture changes explicitly surfaced by primary sources:

- post-`26.1`, Fabric expects non-obfuscated development
- mappings migration from Yarn to Mojang names is mandatory for `26.1+`
- class tweakers are the modern umbrella term; access wideners remain backward-compatible but live inside that system
- `26.2` rendering continues the newer pipeline work and optional Vulkan backend support
- the Fabric 26.2 blog notes GUI/HUD API reorganization and mentions methods moving out of `Minecraft`

Interpretation for this repository:

- the build system migration is unavoidable
- pure search/replace will not be enough for advancement UI and rendering mixins
- the current codebase’s lack of raw OpenGL usage is good, but render hook locations are still likely to move

## 6. Build-system migration

Expected build-system changes for implementation:

1. update `gradle/wrapper/gradle-wrapper.properties` from Gradle `8.12.1` to the current 26.2-supported Gradle line
2. change the Loom plugin ID from legacy `fabric-loom` to `net.fabricmc.fabric-loom`
3. bump Java release and compatibility to `25`
4. replace the old Yarn mappings setup with the 26.2 non-obfuscated configuration
5. bump Fabric Loader and Fabric API to current 26.2-compatible versions
6. revalidate the access widener / class tweaker path and target naming after mappings conversion
7. review whether to keep the existing single-source-set structure or adopt split environment source sets

Recommended minimal-diff principle:

- keep the current single-module architecture
- do **not** perform aesthetic Gradle rewrites
- only adopt additional 26.2 template features when required for correctness

## 7. Dependency compatibility matrix

| Dependency | Current version | Purpose in original | 26.2-compatible version exists? | Expected migration | Risk | Recommendation |
| --- | --- | --- | --- | --- | --- | --- |
| Minecraft | `1.21.4` | runtime target | yes, `26.2` | pervasive API rename/adaptation | High | update during implementation |
| Yarn mappings | `1.21.4+build.8:v2` | named dev mappings | no longer the expected model for `26.1+` | replace with Mojang-named / non-obfuscated workflow | High | migrate mechanically first |
| Fabric Loader | `0.16.10` | loader/runtime, bundled Mixin ecosystem | yes, `0.19.3` | metadata/runtime updates, enum/class-tweaker support | Medium | update with Loom/build changes |
| Fabric API | `0.115.0+1.21.4` | lifecycle events, networking, resource packs, etc. | yes | moderate API drift | Medium | update after build migration |
| Fabric Loom | `1.9-SNAPSHOT` | dev toolchain | yes, `1.17*` | plugin ID and build model change | High | migrate first batch |
| Gradle | `8.12.1` | wrapper/build execution | yes, `9.5.1` recommended | wrapper + buildscript compatibility | Medium | update in first batch |
| toml4j | `0.7.2` | progression config read/write | yes, same artifact likely usable | low/no API migration expected | Low | keep unless proven broken |
| MixinExtras usage | transitive via Fabric ecosystem | wrap/modify-return/local sugar in many mixins | yes | validate version behavior under new loader/loom | Medium | keep; do not redesign away |
| LWJGL TinyFD | runtime dependency via game stack | manual datapack file picker | likely still present but UI APIs may shift | screen/UI adaptation more likely than library loss | Medium | keep behavior, adapt screen code |
| Apache Commons Lang3 | runtime/transitive | simple filename handling | likely still available | no meaningful migration expected | Low | keep |

No true dependency blocker was identified.

## 8. Mappings migration plan

Current model:

- source is written against Yarn-style names
- build explicitly declares Yarn mappings
- access widener header is `accessWidener v2 named`

Required end-state for 26.2:

- non-obfuscated Mojang-named development
- Loom plugin ID `net.fabricmc.fabric-loom`

Official Fabric/Loom flow that should be followed:

1. preserve/freeze the historical BACAP artifacts and current content baseline first
2. keep Minecraft at the **source version `1.21.4`** while preparing a remap-capable modernized build environment
3. replace legacy `fabric-loom` with `net.fabricmc.fabric-loom-remap`, because official Loom docs now designate `net.fabricmc.fabric-loom-remap` for obfuscated versions (`1.21.11` or older)
4. use Loom `1.13+` for the mapping-name migration stage, because current official Fabric migration docs explicitly recommend Loom `1.13` or newer for better migration of Mixins, access wideners/class tweakers, and split client sources
5. run the Yarn -> Mojang migration **against the same Minecraft version currently in use**
6. only after the project is stable again on **Minecraft `1.21.4` in Mojang naming**, start the separate Minecraft-version migration to `26.2`
7. only after that, perform API/runtime/mixin compatibility work for `26.2`

Critical official rule:

- the `migrateMappings` target must use the mappings for the **current/source Minecraft version**, not the destination version
- for this repository, that means migrating against `net.minecraft:mappings:1.21.4`
- Fabric docs also explicitly say not to change `gradle.properties` or `build.gradle` to the new Minecraft version before this migration step

Recommended controlled sequence:

### Stage A: Historical freeze

1. create a dedicated implementation checkpoint from the unchanged baseline
2. archive/checksum the exact historical BACAP artifacts referenced by source
3. keep gameplay/content frozen

### Stage B: Mapping-name migration on source Minecraft `1.21.4`

Preconditions:

- use the **project Gradle Wrapper only**
- keep `minecraft_version=1.21.4`
- keep Loader/API/content semantics unchanged
- move from legacy `fabric-loom` to `net.fabricmc.fabric-loom-remap`
- raise Loom to a line `>= 1.13`
- use a Gradle runtime compatible with the chosen Loom line and the locally available historical JDK/runtime constraints
- do **not** jump to Java 25 or to Minecraft `26.2` in this stage

Recommended commands for this stage after the wrapper/Loom-remap prep is in place:

```powershell
.\gradlew.bat migrateMappings --mappings "net.minecraft:mappings:1.21.4"
```

If split client sources are adopted or needed during the migration:

```powershell
.\gradlew.bat migrateClientMappings --mappings "net.minecraft:mappings:1.21.4"
```

If class tweaker / access widener migration can be task-assisted in the selected Loom line:

```powershell
.\gradlew.bat migrateClassTweakerMappings --mappings "net.minecraft:mappings:1.21.4"
```

Then update the buildscript mappings declaration from Yarn to Mojang mappings for the same source version, using the official Loom-side equivalent:

```groovy
dependencies {
    mappings loom.officialMojangMappings()
}
```

Mandatory manual review after the mechanical pass:

- imports
- method references
- field references
- access widener/class tweaker targets
- every Mixin target, descriptor, ordinal, local capture, and synthetic method reference

### Stage C: Clean Mojang-named baseline on `1.21.4`

Success condition:

- AchieveToDo remains the same historical `1.21.4` content baseline
- naming is now Mojang-based
- no intentional gameplay/content changes have been introduced

### Stage D: Separate Minecraft-version migration `1.21.4 -> 26.2`

Only after Stage C:

1. switch from `net.fabricmc.fabric-loom-remap` to `net.fabricmc.fabric-loom`
2. move to a current supported `26.2` wrapper/Gradle line
3. move runtime/toolchain to Java `25`
4. bump Minecraft, Loader, Fabric API, and related build settings to their `26.2`-compatible line
5. replace any remaining obfuscated-version assumptions in the build

### Stage E: 26.2 compatibility port

Only after the version bump is isolated:

- repair plain source/API drift
- repair mixins
- repair access widener/class tweaker targets that still need manual correction
- repair runtime/UI/render/worldgen behavior

What can likely be automated:

- most vanilla class/method/field renames in ordinary source
- many non-mixin import changes

What cannot safely be automated:

- access widener target correctness
- Mixin descriptors and ordinals
- synthetic-method targets like `method_62216`
- worldgen/serialization hook semantics
- advancement UI behavior assumptions

Staged intermediate migration:

- advisable as a **controlled two-stage strategy**
- first isolate mapping-name migration on source `1.21.4`
- then isolate Minecraft-version migration to `26.2`
- do not bounce through unrelated Minecraft versions
- keep the history reviewable by separating mechanical remap churn from later behavioral fixes

Diff hygiene requirements:

- no formatter churn
- no package reshuffles
- no opportunistic refactors
- review generated mapping changes before touching logic

## 9. Mixin inventory

### Count summary

- client mixins: `20`
- main/integrated-server mixins: `135`
- total: `155`

### Full client mixin manifest

- `AdvancementProgressMixin`
- `AdvancementsScreenMixin`
- `AdvancementTabMixin`
- `AdvancementToastMixin`
- `AdvancementWidgetMixin`
- `ClientAdvancementManagerMixin`
- `ClientPlayerEntityMixin`
- `CraftPlanksTutorialStepHandlerMixin`
- `CreateWorldScreenMixin`
- `DeathScreenMixin`
- `FindTreeTutorialStepHandlerMixin`
- `GameRendererMixin`
- `MinecraftClientMixin`
- `MovementTutorialStepHandlerMixin`
- `OpenInventoryTutorialStepHandlerMixin`
- `PackScreenMixin`
- `PunchTreeTutorialStepHandlerMixin`
- `WorldCreatorMixin`
- `WorldListWidgetMixin`
- `WorldRendererMixin`

### Full main/integrated-server mixin manifest

- `AbstractBoatEntityMixin`
- `AbstractCandleBlockMixin`
- `AbstractCauldronBlockMixin`
- `AbstractDonkeyEntityMixin`
- `AbstractFurnaceBlockMixin`
- `AbstractHorseEntityMixin`
- `AdvancementDisplaysMixin`
- `AllayEntityMixin`
- `AmethystBlockMixin`
- `AnimalEntityMixin`
- `AnvilBlockMixin`
- `ArmadilloEntityMixin`
- `ArmorItemMixin`
- `ArmorSlotMixin`
- `ArmorStandEntityMixin`
- `ArmorStandItemMixin`
- `AxeItemMixin`
- `BarrelBlockMixin`
- `BeaconBlockMixin`
- `BeeEntityMixin`
- `BeehiveBlockMixin`
- `BellBlockMixin`
- `BigDripleafBlockMixin`
- `BlockAttachedEntityMixin`
- `BlockItemMixin`
- `BoatItemMixin`
- `BoggedEntityMixin`
- `BowItemMixin`
- `BrewingStandBlockMixin`
- `BrushItemMixin`
- `BubbleColumnBlockMixin`
- `BucketableMixin`
- `BucketItemMixin`
- `BundleContentsComponentMixin`
- `BundleItemMixin`
- `CamelEntityMixin`
- `CampfireBlockMixin`
- `CartographyTableBlockMixin`
- `CatEntityMixin`
- `ChestBlockMixin`
- `ChorusFlowerBlockMixin`
- `ChunkMixin`
- `ChunkRegionMixin`
- `CommandManagerMixin`
- `ComposterBlockMixin`
- `ConsumableComponentMixin`
- `CowEntityMixin`
- `CraftingTableBlockMixin`
- `CreeperEntityMixin`
- `CrossbowItemMixin`
- `DecoratedPotBlockMixin`
- `DolphinEntityMixin`
- `DoorBlockMixin`
- `EggItemMixin`
- `EnchantingTableBlockMixin`
- `EndCrystalItemMixin`
- `EnderChestBlockMixin`
- `EnderEyeItemMixin`
- `EnderPearlItemMixin`
- `EntityMixin`
- `EquippableComponentMixin`
- `FeatureMixin`
- `FenceGateBlockMixin`
- `FireChargeItemMixin`
- `FireworkRocketItemMixin`
- `FishingRodItemMixin`
- `FlintAndSteelItemMixin`
- `FurnaceMinecartEntityMixin`
- `GoatEntityMixin`
- `GrindstoneBlockMixin`
- `HoeItemMixin`
- `HorseEntityMixin`
- `IronGolemEntityMixin`
- `ItemFrameEntityMixin`
- `JukeboxPlayableComponentMixin`
- `LeadItemMixin`
- `LevelInfoMixin`
- `LevelPropertiesMixin`
- `LivingEntityMixin`
- `LlamaEntityMixin`
- `LoomBlockMixin`
- `MainMixin`
- `MinecartItemMixin`
- `MiningToolItemMixin`
- `MooshroomEntityMixin`
- `OcelotEntityMixin`
- `PandaEntityMixin`
- `ParrotEntityMixin`
- `PigEntityMixin`
- `PiglinEntityMixin`
- `PlacedAdvancementMixin`
- `PlayerAdvancementTrackerMixin`
- `PlayerEntityMixin`
- `PointedDripstoneBlockMixin`
- `PowderSnowBlockMixin`
- `ProjectileEntityMixin`
- `PumpkinBlockMixin`
- `RespawnAnchorBlockMixin`
- `ScoreboardMixin`
- `SerializedChunkMixin`
- `ServerPlayerEntityMixin`
- `ServerScoreboardMixin`
- `ServerWorldMixin`
- `ShearsItemMixin`
- `SheepEntityMixin`
- `ShieldItemMixin`
- `ShovelItemMixin`
- `ShulkerBoxBlockMixin`
- `SmithingTableBlockMixin`
- `SnowballItemMixin`
- `SnowGolemEntityMixin`
- `SpyglassItemMixin`
- `StatHandlerMixin`
- `StonecutterBlockMixin`
- `StriderEntityMixin`
- `StructureMixin`
- `StructureStartMixin`
- `SwordItemMixin`
- `TadpoleEntityMixin`
- `TargetBlockMixin`
- `TntBlockMixin`
- `TrapdoorBlockMixin`
- `TridentItemMixin`
- `TripwireBlockMixin`
- `VaultBlockEntityServerMixin`
- `VaultSharedDataMixin`
- `VehicleInventoryMixin`
- `VillagerEntityMixin`
- `WanderingTraderEntityMixin`
- `WaterloggableMixin`
- `WindChargeItemMixin`
- `WolfEntityMixin`
- `WorldChunkMixin`
- `ZombieVillagerEntityMixin`

### Inventory by subsystem, purpose, and risk

This is the most useful engineering view of all 155 mixins.

#### Advancement / UI / progression mixins

Mixins:

- `AdvancementProgressMixin`
- `AdvancementsScreenMixin`
- `AdvancementTabMixin`
- `AdvancementToastMixin`
- `AdvancementWidgetMixin`
- `ClientAdvancementManagerMixin`
- `AdvancementDisplaysMixin`
- `PlacedAdvancementMixin`
- `PlayerAdvancementTrackerMixin`

Observable behavior:

- injects custom AchieveToDo tab behavior into vanilla advancements UI
- adds locked/mystified tabs
- hides/reveals ability icons and tooltips
- overrides progress math/text for abilities and tracked BACAP goals
- customizes unlock toast look/sound
- imposes custom advancement child ordering
- propagates advancement IDs into progress objects for later client logic

Likely migration risk: **HIGH**

Reasons:

- multiple UI entry points
- progress calculation overrides
- custom rendering assumptions
- likely GUI/HUD class movement in 26.2

#### Rendering / vision / world overlay mixins

Mixins:

- `GameRendererMixin`
- `WorldRendererMixin`

Observable behavior:

- black-screen vision lock overlay
- landmark forcefield border rendering while inside locked landmarks

Likely migration risk: **HIGH**

Reasons:

- 26.2 rendering pipeline evolution
- optional Vulkan backend
- `WorldRendererMixin` targets synthetic method `method_62216`

#### World creation / datapack setup / config persistence mixins

Mixins:

- `CreateWorldScreenMixin`
- `WorldCreatorMixin`
- `WorldListWidgetMixin`
- `PackScreenMixin`
- `MainMixin`
- `LevelInfoMixin`
- `LevelPropertiesMixin`
- `MinecraftClientMixin`
- tutorial step mixins

Observable behavior:

- adds AchieveToDo world-creation tab
- selects progression mode and built-in toggles
- enforces external pack presence and enable order
- persists config name in world metadata and server properties
- alters pack screen behavior and tutorial flow

Likely migration risk: **HIGH**

Reasons:

- create-world UI is a moving target across recent versions
- integrated-server setup path is central to singleplayer Phase A
- several hooks depend on very specific screen internals and field access

#### Landmark / worldgen / chunk-serialization mixins

Mixins:

- `FeatureMixin`
- `StructureMixin`
- `StructureStartMixin`
- `ChunkMixin`
- `ChunkRegionMixin`
- `SerializedChunkMixin`
- `WorldChunkMixin`
- `ServerWorldMixin`

Observable behavior:

- tracks structure and feature generation provenance
- records landmark type and block boxes
- persists landmark metadata into chunk/structure NBT
- syncs loaded/unloaded landmark state to clients

Likely migration risk: **HIGH**

Reasons:

- deep dependence on worldgen internals
- persistence hooks into chunk serialization
- feature tracking uses execution-context capture around generation

#### Scoreboard / statistics / progression sync mixins

Mixins:

- `ScoreboardMixin`
- `ServerScoreboardMixin`
- `StatHandlerMixin`
- `PlayerAdvancementTrackerMixin`

Observable behavior:

- tracks BACAP advancement scoreboard changes
- updates obtained advancement counts
- maps scoreboard/statistics changes to tracked progress payloads

Likely migration risk: **MEDIUM**

Reasons:

- fewer visual dependencies
- likely API renames rather than total redesign
- still relies on specific scoreboard internals

#### Restriction-hook mixins for blocks / items / entities / movement

This is the largest family and represents the core AchieveToDo gameplay lock system.

Representative block hooks:

- `BarrelBlockMixin`
- `BeaconBlockMixin`
- `BrewingStandBlockMixin`
- `CartographyTableBlockMixin`
- `ChestBlockMixin`
- `CraftingTableBlockMixin`
- `DoorBlockMixin`
- `EnderChestBlockMixin`
- `FenceGateBlockMixin`
- `GrindstoneBlockMixin`
- `LoomBlockMixin`
- `RespawnAnchorBlockMixin`
- `ShulkerBoxBlockMixin`
- `SmithingTableBlockMixin`
- `StonecutterBlockMixin`
- `TrapdoorBlockMixin`
- many more

Representative item hooks:

- `AxeItemMixin`
- `BoatItemMixin`
- `BowItemMixin`
- `BrushItemMixin`
- `BucketItemMixin`
- `BundleItemMixin`
- `CrossbowItemMixin`
- `EggItemMixin`
- `EnderEyeItemMixin`
- `EnderPearlItemMixin`
- `FireChargeItemMixin`
- `FireworkRocketItemMixin`
- `FishingRodItemMixin`
- `FlintAndSteelItemMixin`
- `LeadItemMixin`
- `MinecartItemMixin`
- `ShieldItemMixin`
- `SnowballItemMixin`
- `SpyglassItemMixin`
- `TridentItemMixin`
- `WindChargeItemMixin`
- many more

Representative entity/movement hooks:

- `PlayerEntityMixin`
- `ServerPlayerEntityMixin`
- `ProjectileEntityMixin`
- `AbstractBoatEntityMixin`
- `VehicleInventoryMixin`
- `VillagerEntityMixin`
- `WanderingTraderEntityMixin`
- many mob-specific interaction mixins

Likely migration risk: **LOW to MEDIUM per mixin**, but **HIGH in aggregate**

Why aggregate risk is high:

- there are many hooks
- even if each is individually simple, compile failures will be numerous
- ordinals/descriptors/components introduced in newer versions can cause spread-out breakage

## 10. Critical Mixin 26.2 feasibility

Classification legend:

- `A` likely near-mechanical target rename
- `B` same semantics, implementation moved
- `C` significant redesign required
- `D` target behavior no longer exists
- `E` needs deeper investigation

| Mixin / subsystem | Classification | Notes |
| --- | --- | --- |
| `AdvancementsScreenMixin` | `B/C` | UI still exists conceptually, but 26.2 GUI/HUD reorganization raises risk |
| `AdvancementTabMixin` | `B/C` | tab semantics likely remain, internals may move |
| `AdvancementWidgetMixin` | `B/C` | progress/UI rendering hooks likely need adaptation |
| `AdvancementToastMixin` | `B` | same feature probably exists, but toast draw/update internals may shift |
| `AdvancementProgressMixin` | `B` | progress math likely still exists, method names and requirements internals may change |
| `ClientAdvancementManagerMixin` | `B` | likely a moved or renamed equivalent |
| `PlacedAdvancementMixin` | `A/B` | child ordering concept should remain |
| `CreateWorldScreenMixin` | `C/E` | singleplayer pack flow is vital but screen internals are likely significantly changed |
| `PackScreenMixin` | `B/C` | datapack UI likely still exists, but list/render logic may differ |
| `WorldListWidgetMixin` | `B` | reopen-world helper likely still possible |
| `GameRendererMixin` | `C` | rendering path changed materially across recent versions |
| `WorldRendererMixin` | `C/E` | synthetic target plus pipeline evolution makes this one fragile |
| `FeatureMixin` | `C/E` | worldgen hook may have same semantics but different call graph |
| `StructureMixin` | `B/C` | structure-start association likely still feasible |
| `StructureStartMixin` | `C/E` | placement/postPlace/serialization details are fragile |
| `SerializedChunkMixin` | `C/E` | chunk serialization is exactly the kind of area that drifts |
| `MainMixin` | `B/C` | dedicated-server startup path likely still exists but signatures may differ |
| `LevelInfoMixin` | `B` | config persistence likely survives with renames |
| `ScoreboardMixin` | `B` | scoreboard hooks probably still available |
| `PlayerAdvancementTrackerMixin` | `A/B` | likely straightforward once mappings are correct |

No critical mixin was identified as a proven `D` yet.

## 11. UI / AdvancementsScreen migration

Original UI chain preserved by baseline:

- custom AchieveToDo tab is injected into vanilla advancements
- locked tabs are shown as mystified placeholders
- abilities default-open behavior is enforced
- mystified ability icons/tooltips are hidden until demystified
- special text is shown for initially unlocked / permanently locked abilities
- custom unlock toast background/title/colors/sound are applied
- custom child ordering is enforced for selected BACAP tabs

Difficulty estimate:

| Area | Difficulty |
| --- | --- |
| tab presence/removal | Moderate adaptation |
| default selected tab | Moderate adaptation |
| mystified tab click blocking | Moderate adaptation |
| mystified icon masking | Moderate adaptation |
| progress-width overrides | Moderate adaptation |
| custom toast rendering | Moderate adaptation |
| full screen draw hooks | High-risk adaptation |

Important design constraint:

- do **not** replace this with a brand-new custom screen as an escape hatch

## 12. Rendering migration

Original rendering responsibilities:

- full-screen vision lock fade/black overlay
- in-world locked landmark border/forcefield rendering

26.2-specific concerns:

- optional Vulkan backend means backend-sensitive assumptions are riskier
- rendering docs warn against raw OpenGL usage; this code is using Blaze3D-facing classes, which is good
- however the render extraction/draw pipeline has continued changing since `1.21.6`

Assessment:

- conceptually preservable
- implementation hooks likely need deeper retargeting than a normal compile fix

## 13. Networking migration

Original payload inventory:

- C2S:
  - `DemystifyAbilityPayload`
- S2C:
  - `SyncAbilitiesConfigurationPayload`
  - `SyncObtainedAdvancementsCountPayload`
  - `LandmarksLockedStatusChangedPayload`
  - `LandmarkTypesUnlockedPayload`
  - `LockedLandmarkResizedPayload`
  - `ScoreProgressChangedPayload`
  - `StatisticsDataProgressChangedPayload`
  - `CheckTargetInLockedLandmarkPayload`

Assessment:

- the project already uses typed `CustomPayload` + `PacketCodec` APIs rather than much older packet APIs
- that strongly reduces migration risk relative to an older networking stack
- singleplayer semantics should remain unchanged because integrated server separation still matters

Likely migration risk: **LOW to MEDIUM**

## 14. World creation / singleplayer helper migration

Original singleplayer flow:

1. create-world screen gets an AchieveToDo tab
2. player selects progression mode and optional reward/worldgen toggles
3. mod checks the global `datapacks` folder for required external packs
4. downloader/manual-file flow retrieves missing packs
5. packs are copied into the temp world datapack directory
6. packs are enabled in a precise order
7. chosen config name is persisted into world metadata

This flow is central to Mod #1 and must be preserved.

Risk: **HIGH**

Not because the logic is unclear, but because screen APIs and datapack screen integration are likely to have shifted.

## 15. Progression / BACAP integration migration

Original progression logic is clear and data-driven:

- world config resolves to a `Map<AbilityType, Integer>`
- obtained count comes from BACAP scoreboard objective(s)
- unlock/relock is handled by granting/revoking generated AchieveToDo advancement criteria
- demystification is a separate criterion
- tracked scores/statistics/nearby-entities feed special progress widgets

Strength of current design:

- gameplay semantics are already isolated enough to preserve Phase A behavior

Primary risk:

- if BACAP content/scoreboard naming drifts because the wrong external pack is used, progression equivalence breaks immediately

## 16. Restriction migration

Restriction families covered by source:

- block interaction / opening
- item use / item placement
- inventory and containers
- equipment and armor
- bundle handling
- fluid / cauldron / waterlogging
- projectile and ranged weapons
- movement / jump / sneak / sprint / boat / minecart / elytra-related actions
- portal / dimension travel
- mounts and passengers
- villager and wandering trader trading
- vault interaction
- automation / miscellaneous special block behaviors
- landmark-area restrictions

Migration assessment:

- broad semantic coverage is already implemented
- the port should preserve that coverage rather than simplifying it
- most individual hooks are likely solvable mechanically after mappings conversion
- aggregate compile/review volume will be large

## 17. Landmark / worldgen migration

Original landmark system characteristics:

- structure landmarks and feature landmarks are both tracked
- persistence is serialized via custom NBT injected into chunk/structure serialization
- the system tracks actual generated `BlockBox` bounds
- server syncs landmark lock state and resize events to client
- client renders a forcefield when inside a locked landmark

Highest-risk parts:

- `FeatureMixin`
- `StructureStartMixin`
- `SerializedChunkMixin`
- any related extension interfaces that depend on current call ordering

This subsystem is the best candidate for a small early prototype.

## 18. Resources / data-format migration

Current repo-owned pack metadata:

- all built-in packs use `pack_format: 61`

Current 26.2 official technical notes:

- Data Pack version `107.1`
- Resource Pack version `88.0`

Implications:

- every built-in pack’s `pack.mcmeta` will require technical migration
- the frozen external BACAP artifacts will also require technical migration or repackaging

Important distinction:

- updating metadata or syntax to make the **same content** load is Phase A-compatible
- importing newer advancement JSONs is not

Known format-change pressure points:

- pack metadata versions
- predicates and related JSON syntax, especially after pack-version changes introduced in later releases
- resource-pack versioning
- any tags or registry-key path changes introduced after the old BACAP baseline

## 19. Persistence / save compatibility

Current persisted identifiers and data:

- world-level config name in `level.dat`
- custom structure landmark NBT
- custom feature landmark NBT in chunk serialization
- player obtained advancement counts are derived from BACAP scoreboard state rather than a separate ATD player-data file
- progression config files live under `config/achievetodo`

Compatibility risk:

- config-name persistence: **Low**
- progression TOML format: **Low**
- landmark persistence across chunk serialization changes: **Medium/High**
- world portability across external BACAP pack updates: **Medium**

No evidence currently supports promising full save compatibility without targeted implementation validation.

## 20. Expected compiler-error families

Most likely batched error families:

1. buildscript / Loom / plugin ID / wrapper changes
2. mappings rename fallout across all vanilla references
3. access widener target migration from `named` to 26.2 class-tweaker-compatible naming
4. advancement UI and screen API changes
5. rendering pipeline and method move changes
6. structure / feature / chunk serialization API changes
7. scoreboard/statistics internal method/signature changes
8. create-world / datapack screen API changes
9. descriptor/ordinal mismatches in mixins

Notable non-family risk:

- synthetic target `WorldRenderer.method_62216` may simply not exist under the same shape anymore

## 21. Risk register

| Risk | Probability | Impact | Evidence | Mitigation | Validation |
| --- | --- | --- | --- | --- | --- |
| Yarn-to-Mojang mappings migration causes broad source churn on source `1.21.4` | High | High | official Fabric mapping migration docs require migrating to Mojang before porting to `26.1+` | isolate one mechanical source-version migration commit; no refactors | project compiles on Mojang naming while still on `1.21.4` |
| Advancement UI mixins no longer match 26.2 screen internals | High | High | multiple UI mixins + Fabric 26.2 GUI reorg note | prototype UI chain early | open advancements screen and verify all behaviors |
| Rendering hooks moved or changed | Medium/High | High | Fabric rendering docs + synthetic world renderer hook | prototype overlay and landmark rendering before mass polish | in-game visual checks |
| Landmark worldgen tracking hooks changed | Medium/High | High | deep structure/feature/chunk mixins | prototype structure + feature landmark persistence early | create world, visit landmarks, relog |
| Historical BACAP artifact drift or loss | Medium | High | source pins old external URLs; current project page is newer | preserve exact historical zips before implementation | checksum verification |
| Datapack format incompatibility | High | Medium/High | old `pack_format 61` vs 26.2 pack versions | migrate same content technically only | world creation + datapack load succeeds |
| Historical baseline build cannot currently be reproduced locally | High | Medium | current wrapper runs, but Gradle `8.12.1` is not compatible with running on Java `25`; no local JDK `21` was found during audit | keep historical JVM requirement separate from the final `26.2` JVM requirement | retry baseline only if a suitable Java `21` runtime becomes available |
| Access widener/class tweaker targets break during or after source-version mappings conversion | High | Medium | current file is `named`; official docs warn manual review is still required | migrate after the mechanical mapping pass, then validate entries manually and with any available task support | class-tweaker/access-widener validation + compile check |
| Scoreboard/progress sync semantics subtly change | Medium | Medium | scoreboard/stat hooks central to progression | keep payload semantics unchanged | obtain advancements and compare unlock counts |

## 22. Proposed implementation sequence

Recommended batches:

### Cycle 1: Historical freeze and source-version mapping migration foundation

- preserve historical BACAP artifacts and checksums first
- keep Minecraft pinned to `1.21.4`
- replace legacy `fabric-loom` with `net.fabricmc.fabric-loom-remap`
- update wrapper / Gradle / Loom only as much as needed to support a clean source-version Yarn -> Mojang migration
- do **not** bump gameplay/content, Loader/API semantics, or Minecraft version yet
- run `migrateMappings --mappings "net.minecraft:mappings:1.21.4"`
- migrate/review access widener/class tweaker targets
- manually review all Mixins after the mechanical pass

Stop condition:

- Gradle configuration resolves
- project reaches a compile-error state dominated by real source/mapping issues instead of broken wrapper/runtime setup

### Cycle 2: Clean Mojang-named `1.21.4` baseline

- repair plain source references caused by the naming conversion only
- keep behavior/content unchanged
- confirm the project is now effectively the same historical mod, but in Mojang naming

Stop condition:

- project is stable enough that the remaining work is no longer "Yarn -> Mojang conversion cleanup"
- historical baseline semantics are still intact

### Cycle 3: Minecraft `1.21.4 -> 26.2` build migration

- switch from `net.fabricmc.fabric-loom-remap` to `net.fabricmc.fabric-loom`
- move to Java `25`
- move to a current supported `26.2` Gradle/wrapper line
- bump Minecraft / Loader / Fabric API to the `26.2` line
- refresh dependencies and re-establish a resolvable build

Stop condition:

- `26.2` build resolves
- remaining failures are actual API/runtime compatibility issues, not mismatched versioning strategy

### Cycle 4: Common compile restoration

- repair plain source references outside mixins
- restore config, payload, server bootstrap, helpers, enums, utility code

Stop condition:

- non-mixin classes mostly compile

### Cycle 5: Scoreboard / advancement / progression server path

- repair scoreboard/stat/player-advancement hooks
- restore obtained-count, unlock, relock, demystification logic

Stop condition:

- common progression logic compiles end-to-end

### Cycle 6: Create-world / datapack / config flow

- adapt world-creation UI and pack enable-order logic
- preserve downloader/manual selection behavior
- preserve config-name persistence

Stop condition:

- singleplayer world creation path is wired again

### Cycle 7: Advancement UI chain

- repair advancement screen/tab/widget/progress/toast mixins
- preserve mystery behavior and custom presentation

Stop condition:

- full advancements UI works again

### Cycle 6: Landmark / worldgen / persistence

- repair structure/feature/chunk serialization mixins
- restore client sync and resize behavior

Stop condition:

- landmark detection and persistence behave correctly

### Cycle 7: Rendering and polish validation

- repair vision overlay
- repair landmark border rendering
- verify tutorial/death-screen/client UX tweaks

Stop condition:

- gameplay parity candidate is testable

### Cycle 8: Freeze validation and acceptance prep

- compare frozen configs and IDs
- verify no content/balance drift
- produce `26.2 COMPATIBILITY BASELINE` build for user testing

### Implementation-phase execution rules

- Gradle Wrapper may be run directly with normal network access and, if the environment requires it, unsandboxed/elevated
- this allowance covers Gradle distribution download, Fabric Loom, Fabric Loader, Fabric API, mappings, Maven/Gradle dependencies, and ordinary Gradle caches
- this does **not** authorize third-party installers or system modifications
- use the project Gradle Wrapper only; do not use a system Gradle
- never guess the production JAR filename or path; after build, discover the actual distributable from Gradle task outputs / `build/libs`, then compute and record SHA-256 for the correct artifact

## 23. Proposed build/test checkpoints

1. historical BACAP artifacts are preserved and checksummed
2. source-version remap environment resolves on Minecraft `1.21.4`
3. Yarn -> Mojang migration completes against `net.minecraft:mappings:1.21.4`
4. access widener/class tweaker targets are migrated/reviewed
5. Mojang-named `1.21.4` baseline reaches a clean enough compile state
6. `26.2` toolchain resolves and Gradle syncs
7. `compileJava` reaches only mixin/API errors
8. all common/server code compiles
9. create-world screen opens with AchieveToDo tab present
10. external pack detection/enable-order works
11. advancements screen opens without crashes
12. progression unlock counts and demystification work
13. landmark tracking persists through save/reload
14. client rendering overlays work
15. freeze-check confirms no gameplay delta

## 24. User Phase A acceptance plan

The later candidate should be labeled:

- **26.2 COMPATIBILITY BASELINE**

One-action-at-a-time test sequence for the user later:

1. Launch the client and confirm it reaches the main menu.
2. Open Create World and confirm the AchieveToDo tab is present.
3. Choose `Normal` mode and create a new world.
4. If prompted for datapacks, complete the BACAP/setup flow and continue world creation.
5. Open the advancements screen and confirm the AchieveToDo tab chain appears.
6. Attempt a locked basic action such as jumping and confirm the mystery ability is demystified.
7. Earn one or more early advancements and confirm the obtained-count progress updates.
8. Continue until an early ability unlocks and confirm the unlock toast appears.
9. Test one representative inventory/container restriction path.
10. Test one representative equipment or item-use restriction path.
11. Enter a landmark-gated structure and confirm the restriction triggers.
12. While inside a locked landmark, confirm the visual forcefield/border renders.
13. Close the world and reopen it.
14. Confirm progression, demystification, and landmark state persist after reload.

## 25. Unavoidable semantic differences, if currently known

**None known yet.**

Possible implementation-level differences may emerge during rendering or UI retargeting, but none are currently unavoidable from the evidence gathered in this audit.

## 26. Questions requiring user decision

None at this time.

The only future user decision likely needed is if a preserved historical compatibility datapack artifact becomes unavailable and must be supplied from an external backup. That is not currently blocking this audit.

## 27. Future BACAP sync notes

Phase B-only notes:

- the current BACAP public page is newer than the frozen Phase A baseline
- its public total (`1202`) already differs from the repository’s current frozen progression ceiling (`1152`)
- any localization or advancement additions from the newer line belong to later work, not Phase A

## 28. Final recommendation

**CONDITIONAL GO**

Rationale:

- no proven hard blocker exists
- the repository is already conceptually aligned with the intended singleplayer integrated-server architecture
- the main risks are technical migration risks, not product-definition risks

Recommended first implementation batch if approved:

1. preserve the exact historical BACAP artifacts referenced by the current source
2. prepare a source-version remap-capable build on Minecraft `1.21.4`
3. perform one isolated Yarn -> Mojang migration pass against `net.minecraft:mappings:1.21.4`
4. re-establish the historical baseline in Mojang naming while still on `1.21.4`
5. only then move the build/toolchain to the `26.2` model
6. only then start targeted prototypes for:
   - advancement UI chain
   - create-world/datapack flow
   - landmark worldgen/persistence hooks

## Baseline build result

Exact command used during audit:

```powershell
.\gradlew.bat build
```

Result: **FAIL**

Meaningful output summary:

- Gradle wrapper successfully downloaded `gradle-8.12.1`
- build failed before project compilation with:

```text
BUG! exception in phase 'semantic analysis' in source unit '_BuildScript_'
Unsupported class file major version 69
```

Interpretation:

- this is an environment/runtime compatibility problem, not proof of baseline source breakage
- the untouched baseline is configured for Java `21`
- the audit machine currently provides Java `25`
- Gradle `8.12.1` is not a Java `25` runtime-supported Gradle line under the current Gradle compatibility matrix
- no local JDK `21` installation was found during this audit, so the untouched historical baseline could not be re-run under its expected JVM line without installing anything

Baseline build classification: **INCONCLUSIVE as a source-quality signal**

It proves the untouched baseline is not directly reproducible on the current local Java `25` environment, but it does **not** prove the source itself was already broken on its intended historical Java `21` / toolchain.
