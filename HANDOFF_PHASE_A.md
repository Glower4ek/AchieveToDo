# AchieveToDo 26.2 — HANDOFF / CONTEXT FOR CONTINUATION

## STOP UPDATE — 2026-08-18

Это самый свежий handoff-апдейт после пользовательского stop на **Tuesday, August 18, 2026**.

### Confirmed PASS к моменту stop

- `.\gradlew.bat compileJava --stacktrace` = PASS
- `.\gradlew.bat build --stacktrace` = PASS
- `.\gradlew.bat runClient --stacktrace` bootstrap = PASS
- Main Menu = PASS
- `Singleplayer` = PASS
- `Select World` = PASS
- `Create New World` UI = PASS
- `AchieveToDo` tab = PASS
- BACAP download/setup flow = PASS
- historical BACAP compatibility conversion реально продвинула world creation дальше прежнего барьера
- world save реально создаётся и `New World` присутствует в `Select World`

### Current FAIL к моменту stop

- сохранённый `New World` начинает загружаться
- загрузка почти завершается
- затем join завершается disconnect-экраном:
  `Connection Lost`
  `AchieveToDo is not ready yet`
- crash клиента отсутствует

### Точная точка продолжения на завтра

`Play Selected World`
-> world load almost completes
-> `Connection Lost`
-> `AchieveToDo is not ready yet`

### Runtime evidence, который нужно считать source of truth

- свежий evidence сохранён в:
  `D:\Vibecode\AchieveToDo 26.2\run\logs\latest.log`
- важная особенность среды:
  текущая календарная дата handoff = **2026-08-18**, но timestamps в runtime-файлах уже идут как **2026-08-19**; при продолжении ориентироваться на фактические timestamps файлов и содержимое `latest.log`, не на предположения
- в свежем `latest.log` по reopen/run видны как минимум:
  - datapack parse/load errors около `2026-08-19 01:18:14`
  - затем integrated-server join path доходит почти до конца и заканчивается disconnect по readiness guard

### Точное место source, которое выдаёт disconnect

- literal disconnect находится в [src/main/java/com/diskree/achievetodo/server/AchieveToDoServer.java](/D:/Vibecode/AchieveToDo%2026.2/src/main/java/com/diskree/achievetodo/server/AchieveToDoServer.java:426)
- guard вызывается из `ServerPlayConnectionEvents.JOIN` в [src/main/java/com/diskree/achievetodo/server/AchieveToDoServer.java](/D:/Vibecode/AchieveToDo%2026.2/src/main/java/com/diskree/achievetodo/server/AchieveToDoServer.java:422)

### Какая readiness condition там проверяется

- `isNotReady()` находится в [src/main/java/com/diskree/achievetodo/server/AchieveToDoServer.java](/D:/Vibecode/AchieveToDo%2026.2/src/main/java/com/diskree/achievetodo/server/AchieveToDoServer.java:204)
- текущее условие:
  - `abilitiesConfiguration == null`
  - `currentAdvancementsMode == null`
  - `currentScoreboardObjective == null`
  - `currentScoreboardDisplaySlot == null`

### Основные upstream initialization/sync paths, которые нужно проверить завтра

- server readiness lifecycle в `AchieveToDoServer`:
  - `prepareScoreboard(...)` в [src/main/java/com/diskree/achievetodo/server/AchieveToDoServer.java](/D:/Vibecode/AchieveToDo%2026.2/src/main/java/com/diskree/achievetodo/server/AchieveToDoServer.java:67)
  - `ServerLifecycleEvents.SERVER_STARTED.register(...)` в [src/main/java/com/diskree/achievetodo/server/AchieveToDoServer.java](/D:/Vibecode/AchieveToDo%2026.2/src/main/java/com/diskree/achievetodo/server/AchieveToDoServer.java:399)
  - `ServerPlayConnectionEvents.JOIN.register(...)` в [src/main/java/com/diskree/achievetodo/server/AchieveToDoServer.java](/D:/Vibecode/AchieveToDo%2026.2/src/main/java/com/diskree/achievetodo/server/AchieveToDoServer.java:422)
- scoreboard discovery path:
  - `ServerScoreboardMixin` hook на `setDisplayObjective` в [src/main/java/com/diskree/achievetodo/injection/mixin/main/ServerScoreboardMixin.java](/D:/Vibecode/AchieveToDo%2026.2/src/main/java/com/diskree/achievetodo/injection/mixin/main/ServerScoreboardMixin.java:14)
  - нужно проверить, реально ли на 26.2 existing-world reopen гарантированно проходит через этот path до player join
- initial ability/config sync и networking:
  - `SyncAbilitiesConfigurationPayload` отправляется только после readiness guard в join handler
  - если server state не готов, payload вообще не уходит
  - завтра проверить server-side init ordering и client receive path, но НЕ обходить guard искусственным `ready=true`
- integrated-server/world reopen path:
  - проверить, почему normal initialization не переводит AchieveToDo в ready state до завершения join
  - world-local BACAP reopen path перепроверять только если свежий evidence снова связывает datapack parse failures с отсутствием scoreboard/advancement state

### Что НЕ делать завтра в первом шаге

- не делать новый speculative compatibility pass вслепую
- не force-set `ready=true`
- не удалять readiness guard
- не обходить disconnect искусственно

### Разрешённый pipeline на продолжение

- `.\gradlew.bat compileJava --stacktrace`
- `.\gradlew.bat build --stacktrace`
- `.\gradlew.bat runClient --stacktrace`
- JDK:
  `C:\Program Files\Eclipse Adoptium\jdk-25.0.4.7-hotspot`

### Статус документации

- `HANDOFF_PHASE_A.md` обновлён этим stop-update
- `PORT_26_2_STATUS.md` в проекте отсутствует; на stop не создавать новый большой статусный документ только ради этого

---

**Дата handoff:** 2026-08-19  
**Активный проект:** `D:\Vibecode\AchieveToDo 26.2`  
**Рабочая ветка:** `26.2-port`  
**Цель текущей работы:** довести **Phase A — 26.2 Compatibility Baseline** до инженерного кандидата и затем до личного live-test пользователя.

---

# 0. КРИТИЧЕСКИ ВАЖНО: КАК ПРОДОЛЖАТЬ РАБОТУ

Это НЕ новый проект и НЕ просьба начать порт заново.

Перед изменениями:

1. Прочитай этот файл полностью.
2. Прочитай:
   - `PHASE_A_AUDIT.md`
   - `MIXIN_26_2_MAP.md`
   - `PORT_26_2_STATUS.md` (если уже существует/актуален)
3. Проверь текущий `git status`.
4. Проверь фактические последние runtime-логи:
   - `run\logs\latest.log`
   - crash-report, если появился.
5. Продолжай **с CURRENT BLOCKER**, описанного ниже.

## Главное рабочее правило

**MILESTONE REPORT != TASK COMPLETION.**

Если следующий инженерный шаг уже известен — **выполняй его**, а не заканчивай ответ описанием того, что сделаешь потом.

Нельзя завершать сессию только потому, что:
- исправлен очередной Mixin;
- `compileJava` зелёный;
- `build` зелёный;
- очередной `runClient` прошёл дальше;
- найден следующий обычный package/owner/signature/descriptor drift;
- найден новый runtime crash с очевидным инженерным путём;
- достигнут main menu;
- достигнут очередной UI milestone.

### STOP разрешён только если:

1. Есть настоящий инженерный blocker, и после изучения актуальных 26.2 source/bytecode/API нет очевидного корректного semantic equivalent.
2. Требуется gameplay/semantic решение пользователя.
3. Требуется действие вне standing permissions (системная установка, изменение ОС, destructive action и т.п.).
4. Требуется реальное визуальное/игровое действие пользователя в Minecraft.
5. Полностью готов **`26.2 COMPATIBILITY BASELINE — ENGINEERING CANDIDATE`**.

### Все сообщения пользователю — ТОЛЬКО НА РУССКОМ.

### Live-test
Если нужен пользователь:
- давай **ОДНО точное действие за раз**;
- не давай пачку инструкций;
- дождись ответа;
- затем давай следующее одно действие.

---

# 1. АРХИТЕКТУРА ПРОЕКТОВ

Существует две линии разработки, но **сейчас активен только Project 1**.

## Project 1 — canonical AchieveToDo 26.2

Путь:

`D:\Vibecode\AchieveToDo 26.2`

Это чистый порт оригинального AchieveToDo на Minecraft/Fabric 26.2.

### Runtime target

- Minecraft **26.2**
- Fabric
- Singleplayer / integrated server
- Dedicated Fabric server support сейчас НЕ является целью
- Нужно сохранить и client-side, и integrated-server логику оригинального мода

### Главный принцип

Сначала сделать **технически совместимый старый AchieveToDo на 26.2**.

Только ПОСЛЕ принятия Phase A пользователем можно переходить к:
- новому BACAP;
- новым способностям;
- новому контенту;
- локализации нового контента;
- Balance 2.0.

---

## Project 2 — FUTURE multiplayer client mod + Purpur plugin

НЕ ТРОГАТЬ до полного завершения Project 1.

Будущий Mod #2:
- client-only Fabric mod;
- предназначен для серверов;
- все нужные игроки будут иметь клиентский мод;
- fallback НЕ нужен;
- должен переиспользовать canonical UI/assets/visuals/mappings из готового Mod #1;
- существующий Purpur plugin будет authoritative server backend.

---

## Старый Purpur проект

Путь:

`D:\Vibecode\AchieveToDo 26.2 Server`

**ПОЛНОСТЬЮ PAUSED.**

Не использовать как источник реализации для Project 1.
Не переносить оттуда chest fallback/resource-pack fallback/heuristics.
Не менять этот проект в Phase A.

---

# 2. ФАЗЫ

Строгая последовательность:

## Phase A — 26.2 Compatibility Baseline
Оригинальный старый AchieveToDo, старый контент, старый баланс, но работающий на 26.2.

## Phase B — BACAP 26.2 Content + Localization Sync

## Phase C — New Minecraft/BACAP Ability Audit

## Phase D — Balance 2.0

## Phase E — Solo Release Candidate

**НЕЛЬЗЯ смешивать Phase A с Phase B/C/D.**

Phase A означает:
> тот же старый AchieveToDo, что и на 1.21.4+, только технически работающий на Minecraft 26.2.

Пользователь хочет лично протестировать Phase A до перехода дальше.

---

# 3. GIT / BASELINE

Исходный репозиторий:

`https://github.com/AchieveToDo/mod.git`

Исходная ветка:

`1.21.4+`

Baseline commit:

`e8095a2fb386c510b39c0dfd35071f6153480c50`

Immutable baseline tag:

`pre-26.2-port`

Рабочая ветка:

`26.2-port`

Не переписывать baseline и не делать ненужные широкие refactor/formatting changes.

---

# 4. PHASE A CONTENT FREEZE / BASELINE COUNTS

Эти значения должны оставаться неизменными в Phase A:

- Mixins total: **155**
- client mixins: **20**
- main/integrated-server mixins: **135**
- custom payloads: **9**
- abilities: **151**
- landmarks: **22**
- language files: **2**
- mod-owned GUI sprite textures: **3**
- built-in resource/datapack pack directories: **9**
- built-in override mcfunctions: **1214**
- `TOTAL_ADVANCEMENTS_COUNT = 1152`

### 22 landmark types — ровно эти:

1. DESERT_PYRAMID
2. DESERT_WELL
3. JUNGLE_PYRAMID
4. PILLAGER_OUTPOST
5. IGLOO
6. SWAMP_HUT
7. MANSION
8. VILLAGE
9. RUINED_PORTAL
10. BURIED_TREASURE
11. SHIPWRECK
12. OCEAN_RUIN
13. MONUMENT
14. MONSTER_ROOM
15. MINESHAFT
16. TRAIL_RUINS
17. ANCIENT_CITY
18. TRIAL_CHAMBERS
19. STRONGHOLD
20. FORTRESS
21. BASTION_REMNANT
22. END_CITY

---

# 5. PROGRESSION / BALANCE FREEZE

Phase A НЕ меняет balance.

## Easy
- initially unlocked: `VISION`, `JUMP`
- max: `600`

## Normal
- initially unlocked: `VISION`
- max: `730`

## Hard
- initially unlocked: none
- min: `2`
- max: `860`

## Chaos
- seed/state базируется на Hard
- `VISION` всегда initially unlocked
- 1% extra initial unlock chance
- 0.2% non-priority permanent-lock chance
- max: `1000`

Flags:
- initially unlocked = `0`
- permanently locked = `-1`

---

# 6. HISTORICAL BACAP — НЕ ОБНОВЛЯТЬ

Phase A использует именно старый historical BACAP baseline.

Source pin:

`BlazeandCave's Advancements Pack 1.18.1.zip`

Core SHA-1:

`45b8bb0076bbf5b92fde7dc9590c6686937abbc0`

Hardcore SHA-1:

`ec5203496a822e6145562cd81e781ca0eea2c968`

Compatibility/worldgen packs также исторически pinned.

Terralith historical compatibility artifact:

wrapper SHA-1:
`2699070cf5040ab519c223178ee64ee9eafe3691`

final ZIP SHA-1:
`3d8cc170c1bf2a00460a8d7e779acbe9d5034dea`

Исторические worldgen pack версии:

- `Terralith_1.21_v2.5.7`
- `Amplified_Nether_1.21_v1.2.7`
- `Nullscape_1.21_v1.2.10`

## Preservation barrier

Уже выполнено:

**P0 preservation barrier = PASS**

Все 8 historical artifacts сохранены и проверены.

Файлы/инструменты:

- `reference/phase_a_freeze/snapshot.json`
- `reference/phase_a_freeze/easy.toml`
- `reference/phase_a_freeze/normal.toml`
- `reference/phase_a_freeze/hard.toml`
- deterministic `chaos_*.toml`
- `tools/phase_a/preserve_historical_artifacts.ps1`
- `reference/phase_a_preservation/README.md`

НЕЛЬЗЯ:
- заменять historical BACAP на current BACAP;
- менять advancement IDs;
- менять advancement content ради удобства;
- добавлять новые abilities;
- менять баланс.

Если старый pack требует технической адаптации к 26.2, допускается **VERSION-ADAPTED technical compatibility conversion**, но observable content freeze должен сохраниться.

---

# 7. MAPPING MIGRATION — УЖЕ ЗАВЕРШЕНО

Очень важно: Yarn -> Mojang migration была сделана ПРАВИЛЬНО, на 1.21.4 до обновления Minecraft.

Порядок был:

1. остались на Minecraft 1.21.4;
2. использовали remap-capable Loom:
   `net.fabricmc.fabric-loom-remap`;
3. `migrateMappings` выполнен против:
   `net.minecraft:mappings:1.21.4`;
4. migrated class tweaker/access widener;
5. ручной cleanup;
6. создан Mojang-named 1.21.4 checkpoint;
7. только затем проект перенесён на 26.2.

## Mojang-named 1.21.4 checkpoint = FULL PASS

- `compileJava` PASS
- full `build` PASS
- `validateAccessWidener` PASS
- `remapJar` PASS
- content baseline unchanged

НЕ повторять Yarn->Mojang migration заново.

---

# 8. CURRENT TOOLCHAIN

## Active 26.2 toolchain

- Minecraft: **26.2**
- Java target: **25**
- local Java:
  `C:\Program Files\Eclipse Adoptium\jdk-25.0.4.7-hotspot`
- Gradle wrapper: **9.5.1**
- Fabric Loader: **0.19.3**
- Fabric API currently used: **0.154.2+26.2**
- Loom: **1.17.19**
- mappings: official Mojang mappings
- `.accesswidener` уже converted to `.classtweaker`
- `fabric.mod.json` обновлён

Не делать version churn без реальной причины.

## Historical pre-port toolchain

- MC 1.21.4
- Java 21
- Gradle 8.12.1
- Loom 1.9-SNAPSHOT
- Loader 0.16.10
- Fabric API 0.115.0+1.21.4
- Yarn `1.21.4+build.8:v2`
- toml4j 0.7.2

---

# 9. NETWORK INCIDENT — СЧИТАЕТСЯ ЗАКРЫТЫМ

Ранее был TLS/network failure к `maven.fabricmc.net`.

Потом сеть была исправлена пользователем, Fabric Maven снова стал доступен.

Стандартный `plugins {}` работает.

НЕ возвращаться к старому `buildscript classpath/apply plugin` workaround.
НЕ повторять сетевую диагностику, если проблема не вернулась.

---

# 10. STANDING GRADLE PERMISSION

Для Project 1 разрешено без лишнего запроса:

- использовать project Gradle Wrapper;
- normal network/cache;
- unsandboxed/elevated Gradle execution, если среда этого требует;
- скачивать Gradle distributions;
- Loom;
- Loader;
- Fabric API;
- mappings;
- Maven/Gradle dependencies;
- normal Gradle caches.

Ограничения:

- только project wrapper;
- НЕ использовать system Gradle;
- НЕ ставить системные зависимости/ПО без разрешения;
- НЕ менять ОС/глобальные настройки;
- НЕ удалять глобальные caches без отдельной причины/разрешения.

---

# 11. IMPORTANT GAMEPLAY SEMANTIC DECISION: COPPER

Minecraft 26.2 имеет copper tools/weapons.

В Phase A принято решение:

## Copper tools/weapons -> `USE_STONE_TOOLS`

Это **VERSION-ADAPTED compatibility rule**.

Применять:

- Copper Sword -> same gate as Stone Sword
- Copper Pickaxe -> Stone Pickaxe gate
- Copper Axe -> Stone Axe gate
- Copper Shovel -> Stone Shovel gate
- Copper Hoe -> Stone Hoe gate

НЕ:
- маппить copper на Iron;
- оставлять copper unrestricted;
- создавать новую ability в Phase A.

В Phase C позже можно решить, нужна ли copper отдельная ability.

Не распространять это правило автоматически на другие новые 26.2 mechanics/items.

---

# 12. УДАЛЁННЫЕ OLD ITEM MIXINS — СЕМАНТИКА УЖЕ ПЕРЕНЕСЕНА

Удалены:

- `ArmorItemMixin`
- `ArmorItemExtension`
- `MiningToolItemExtension`
- `SwordItemExtension`
- `MiningToolItemMixin`
- `SwordItemMixin`

И их записи из `achievetodo.mixins.json`.

Это было одобрено ТОЛЬКО потому, что их semantics были перенесены в актуальную registry/item mapping логику.

Обязательно позже runtime-regression:
- Stone
- Copper -> Stone
- Iron+
- разные tool types
- equipment gates

Компиляция сама по себе НЕ подтверждает gameplay PASS.

---

# 13. ORIGINAL UI / CLIENT FEATURES, КОТОРЫЕ ДОЛЖНЫ СОХРАНИТЬСЯ

Оригинальный основной UI — модифицированный vanilla `AdvancementsScreen`, НЕ chest UI.

Mod-owned sprites:

- `ability_mystified_mask.png` — 26x26
- `advancements_tab_mystified_mask.png` — 16x16
- `ability_unlocked_notification_background.png` — 160x32

Custom font отсутствует.

Ability states:

- UNKNOWN LOCKED
- DEMYSTIFIED LOCKED
- UNLOCKED
- INITIALLY UNLOCKED
- PERMANENTLY LOCKED

Обязательные client features:

- настоящий `AdvancementsScreen`
- ability tabs/widgets
- mystery/mystified masks
- tooltips
- progress
- toast
- VISION blackout/fade
- screenshot guard
- locked-landmark forcefield
- Create World integration
- tutorial hooks
- assets/localization

Обязательные integrated-server features:

- restrictions
- progression
- BACAP counting
- scores/statistics
- nearby entities
- landmarks
- persistence
- networking

---

# 14. CUSTOM PAYLOADS — 9 TOTAL

C2S:

- `DemystifyAbilityPayload`

S2C:

- `SyncAbilitiesConfigurationPayload`
- `SyncObtainedAdvancementsCountPayload`
- `ScoreProgressChangedPayload`
- `StatisticsDataProgressChangedPayload`
- `LandmarksLockedStatusChangedPayload`
- `LandmarkTypesUnlockedPayload`
- `LockedLandmarkResizedPayload`
- `CheckTargetInLockedLandmarkPayload`

Payload semantics должны сохраниться.

---

# 15. 26.2 SOURCE/API PORT — MAJOR COMPLETED WORK

Первый 26.2 `compileJava` имел примерно ~100 errors.

После adaptation:
- errors были сокращены до 13;
- все 13 были в `WorldRendererMixin`;
- затем `WorldRendererMixin` был переписан на новый 26.2 rendering path;
- `compileJava` стал PASS;
- full `build` стал PASS.

В числе уже обработанных 26.2 shifts:

- `ResourceLocation` -> `net.minecraft.resources.Identifier`
- `CriteriaTriggers` moved to `net.minecraft.advancements.triggers`
- `GameRules` -> `net.minecraft.world.level.gamerules.GameRules`
- `VillagerProfession` -> `net.minecraft.world.entity.npc.villager`
- `EntityType.*` -> `EntityTypes.*` там, где подтверждено
- villager logic -> `ResourceKey<VillagerProfession>`
- action-bar messages -> overlay path
- registry-based icons/armor lookup
- Create World 26.2 signatures
- Advancements GUI extraction path
- renderer state/pipeline changes

---

# 16. WORLD RENDERER PORT — ВАЖНО

`WorldRendererMixin` уже перенесён с legacy render path на 26.2 render/state pipeline.

Исторически:
- synthetic target `method_62216`
- после `MultiBufferSource$BufferSource.endBatch()`
- `RenderType.worldBorder(...)`
- `Tesselator`
- `BufferUploader`
- direct `RenderSystem` mutation

26.2 semantic equivalent:
- weather/world-border render stage inside
  `LevelRenderer.lambda$addWeatherPass$0(GpuBufferSlice, int)`
- injection immediately after vanilla
  `WorldBorderRenderer.render(...)`

Новый path:
- `ByteBufferBuilder`
- `BufferBuilder`
- `RenderPass`
- `RenderPipelines.WORLD_BORDER`
- vanilla `WorldBorderRenderer.FORCEFIELD_LOCATION`
- render target follows vanilla:
  `weatherTarget()` if available, else `gameRenderer.mainRenderTarget()`

State access:
- old shadowed `LevelRenderer.level` больше не существует;
- current dimension теперь читается через
  `Minecraft.getInstance().level` at render time.

Это задокументировано в `MIXIN_26_2_MAP.md`.

Не возвращать старый raw OpenGL/immediate path.
Не отключать forcefield.

---

# 17. VISION RENDERING

VISION overlay был перенесён с old hand-render path на актуальный fullscreen overlay path в `GameRendererMixin`.

Сохранить:
- fade semantics
- screenshot guard

Нужен реальный visual live-test позже.

---

# 18. ADVANCEMENTS / CREATE WORLD CLIENT PORT

Уже адаптированы под 26.2:

- `AdvancementsScreen`
- `AdvancementWidget`
- migration from `GuiGraphics` to `GuiGraphicsExtractor`
- `CreateWorldScreen`
- `WorldCreationTab`
- `ExternalPackDownloader`
- `WorldListWidget`
- related client hooks

---

# 19. RUNTIME MIXIN RETARGETING — УЖЕ ПРОЙДЕННЫЕ СЕМЕЙСТВА

После зелёных compile/build начался runtime bootstrap chain.

Уже исправлены многие реальные 26.2 runtime breakages.

## Early runtime fixes

### `MinecraftClientMixin`
- `setScreenAndShow(...)` вместо старого `setScreen(...)`

### `EntityMixin`
- `interact(...)` теперь с `Vec3`
- leash target:
  `canHaveALeashAttachedTo(Entity)`

### `WaterloggableMixin`
- `pickupBlock(LivingEntity, ...)`
  вместо старого `Player`
- старая player-only semantics сохранена

### `PointedDripstoneBlockMixin`
- projectile hook перенесён на актуальный host `SpeleothemBlock`
- guard обратно на `PointedDripstoneBlock`

### `AmethystBlockMixin`
- актуальный `Level.playSound(Entity, ...)`

### `TntBlockMixin`
- оба hook'а со старого `explode(...)`
  перенесены на актуальный `prime(Level, BlockPos, LivingEntity)`
- сохранены semantics для `IGNITE_TNT` и landmark-lock

## Семейства/дополнительные runtime fixes

Исправлены:
- interact/mobInteract + `Vec3` family
- `SnowGolem`
- `ItemFrame`
- `MinecartFurnace`
- устаревшие owner paths для `SnowGolem`, `Mooshroom`, `IronGolem`, `MinecartItem`
- `pickupBlock/emptyContents Player -> LivingEntity` для `BubbleColumn`, `PowderSnow`, `BucketItem`
- `playSound(Entity, ...)` для `BeehiveBlock`, `AxeItem`, `HoeItem`, `ShovelItem`, `CreeperEntityMixin`, `FishingRodItemMixin`, `FlintAndSteelItemMixin`
- `isClientSide:Z -> isClientSide()Z` для `BucketItemMixin`, `PigEntityMixin`, `StriderEntityMixin`, `ParrotEntityMixin`, `LlamaEntityMixin`, `DolphinEntityMixin`
- `ArmadilloEntityMixin`: `brushOffScute()` -> `brushOffScute(Entity, ItemStack)`
- equine owner-path batch: `AbstractHorseEntityMixin`, `AbstractDonkeyEntityMixin`, `HorseEntityMixin`
- `CowEntityMixin`: `Cow` -> `AbstractCow`
- `LlamaEntityMixin` -> `animal.equine.Llama`
- `OcelotEntityMixin` -> `animal.feline.Ocelot`
- `PandaEntityMixin` -> `animal.panda.Panda`
- `SheepEntityMixin`
- `WolfEntityMixin`
- `EnderEyeItemMixin`
- `LeadItemMixin`
- `ShieldItemMixin`
- screenshot guard in `GameRendererMixin`

### `DeathScreenMixin`
old:
- score translatable wrapped in `DeathScreen.init()`

26.2:
- `deathScore` создаётся в `DeathScreen.<init>(Component, boolean, LocalPlayer)`
- hook перенесён в constructor
- hardcore gate и `AchieveToDoClient.isNotReady()` safeguard сохранены

---

# 20. SINGLEPLAYER / SELECT WORLD RUNTIME BARRIERS — УЖЕ ИСПРАВЛЕНО

После достижения main menu пользователь нажал `Singleplayer`.

Изначально UI зависал на blurred background, `Select World` не появлялся.

Исправления:

## `PackScreenMixin`
- старый screen-level `updateList`
- перенесён на актуальные 26.2:
  `TransferableSelectionList.updateList(...)`

## `WorldListWidgetMixin`
- старый owner `SelectWorldScreen screen`
- актуальный 26.2 owner type `Screen`

После этого зависание ещё сохранялось.

Глубже был найден:

## `CreateWorldScreenMixin`
Lazy classloading происходил во время `SelectWorldScreen.init()`.

Старая tab architecture была несовместима.

Исправлено:
- переход на актуальный `MenuTabBar$Builder.addTabs(...)`
- удалён ненужный `tabNavigationBar` shadow
- проверка оставлена через живой `TabManager`

После этого:
- `Singleplayer` открывается
- `Select World` достижим
- `Create New World` достижим
- вкладка AchieveToDo видна

---

# 21. THREAD DUMP ATTEMPT

При одном из зависаний предпринималась попытка:

`jcmd <pid> Thread.print`

Но attach вернул:

`java.io.IOException: Отказано в доступе`

Не обходить это системными хаками без разрешения.

`latest.log` тогда дал точный root cause, и зависание было исправлено без thread dump.

---

# 22. LIVE TEST RESULTS — УЖЕ ФАКТИЧЕСКИ ПРОЙДЕНО ПОЛЬЗОВАТЕЛЕМ

Все пункты ниже — реальные пользовательские наблюдения.

## PASS: Client bootstrap
Клиент доходит до main menu без bootstrap crash.

## PASS: Main Menu -> Singleplayer
После фиксов экран выбора миров открывается.

## PASS: Create New World
Экран Create New World открывается без краша.

## PASS: AchieveToDo tab visible
На Create New World видны вкладки:
- Game
- World
- More
- AchieveToDo

## PASS: AchieveToDo tab opens
Вкладка реально открывается и отрисовывается.

Видимые controls:
- `Difficulty: Normal`
- `Cooperative mode: ON/OFF`

Rewards:
- Items
- Experience
- Trophies

Custom Generation:
- Overworld
- Nether
- End

Кнопки:
- Create New World
- Cancel

## PASS: Cooperative mode interaction
Пользователь несколько раз переключал `ON <-> OFF`.

Результат:
- мгновенно;
- без зависания;
- без краша.

## PASS: Custom Generation / Overworld interaction
Пользователь переключил `Overworld OFF -> ON`.

Результат:
- мгновенно;
- без зависания;
- без краша.

## PASS: Create World -> BACAP dependency screen
При нажатии Create New World без установленного BACAP открылся встроенный dependency/setup screen:

`To play you need to download BlazeandCave's Advancements Pack (BACAP)`

На экране есть:
- Download
- Back
- Learn More...

Без crash/hang.

## PASS: BACAP setup Back path
`Back` корректно возвращает на Create New World.

## PASS: In-game BACAP Download
Пользователь нажал `Download`.

Результат:
- встроенная загрузка стартовала;
- завершилась;
- без зависания;
- без краша;
- после загрузки клиента вернуло на Create New World.

---

# 23. CURRENT STATE — WORLD CREATION FIX IMPLEMENTED, LIVE-TEST PENDING

Предыдущий live-test FAIL был:

**BACAP downloaded -> Create New World -> actual world creation**

Симптом предыдущего FAIL:
- после нажатия `Create New World` происходил короткий переход;
- клиент возвращался на Create New World;
- мир не создавался;
- кнопка `Create New World` после возврата больше не реагировала / оставалась в disabled/busy state;
- crash отсутствовал.

## Инженерный фикс уже выполнен

После этого Codex продолжил диагностику и реализовал compatibility fix для historical BACAP.

Суть исправления:

- preserved global historical BACAP artifact НЕ изменяется;
- historical BACAP переводится в **controlled world-local compatibility copy** для Minecraft 26.2;
- техническая адаптация применяется **только в момент копирования pack в конкретный мир**;
- preserved global artifact остаётся нетронутым;
- тот же compatibility layer подключён к **reopen/join path**, чтобы уже созданный мир использовал совместимую world-local копию последовательно.

Это соответствует Phase A правилу:
- historical content сохраняется;
- current BACAP НЕ подставляется;
- допускается только VERSION-ADAPTED technical conversion;
- intentional content/balance delta должен оставаться NONE.

### Файлы, изменённые этим fix:

- `src/main/java/com/diskree/achievetodo/client/ExternalPackCompatibility.java`
  - добавлен основной compatibility layer для world-local copy
- `src/main/java/com/diskree/achievetodo/injection/mixin/client/CreateWorldScreenMixin.java`
  - подключён новый compatibility path при создании мира
- `src/main/java/com/diskree/achievetodo/injection/mixin/client/WorldListWidgetMixin.java`
  - тот же compatibility path подключён к reopen/join flow

По последнему engineering report:
- `compileJava` = **PASS**
- новый `runClient` дошёл до клиента;
- в свежем `run\logs\latest.log` больше нет прежних datapack component/load ошибок.

## ТЕКУЩАЯ ТОЧКА: НУЖЕН ОДИН LIVE-TEST ШАГ ПОЛЬЗОВАТЕЛЯ

Codex уже попросил пользователя выполнить ровно одно действие:

> На текущем экране `Create New World`, после уже скачанного BACAP, нажать `Create New World` **один раз** и сообщить:
> - создался ли мир;
> - либо клиент снова вернулся на экран `Create New World`.

### ВАЖНО ДЛЯ НОВОГО API/CODEX АГЕНТА

При старте новой API-сессии НЕ повторять предыдущую диагностику и НЕ переделывать этот fix до получения нового live-test результата.

Текущий immediate state:

**ENGINEERING FIX READY -> WAITING FOR USER LIVE-TEST RESULT**

Если пользователь сообщает:
- **мир создался** -> считать этот конкретный world-creation barrier PASS и продолжать Phase A с integrated-server/world runtime validation;
- **снова вернуло на Create New World / мир не создался** -> считать barrier FAIL, снять свежий `latest.log` именно с этого нового прогона и продолжить диагностику с фактического нового root cause.

---

# 24. ЧТО ДЕЛАТЬ ПОСЛЕ LIVE-TEST РЕЗУЛЬТАТА

## Если world creation PASS

Сразу продолжать, не останавливаясь на milestone:

1. Проверить startup/integrated-server logs.
2. Убедиться, что world-local compatibility BACAP реально подключён.
3. Проверить, что historical preserved global artifact не модифицирован.
4. Проверить advancement/datapack load без parse/component errors.
5. Перейти к следующим vertical checks:
   - integrated server runtime;
   - AdvancementsScreen;
   - progression/networking;
   - restrictions;
   - equipment/tool regression;
   - landmarks/worldgen/persistence;
   - VISION;
   - locked-landmark forcefield;
   - save/reopen;
   - freeze regression;
   - production candidate.

## Если world creation FAIL

1. Не делать выводов по старому логу.
2. Снять свежий `run\logs\latest.log` вокруг новой попытки.
3. Проверить:
   - exception / stacktrace;
   - datapack validation;
   - pack metadata / pack format;
   - registry/worldgen bootstrap;
   - advancement parse/component errors;
   - failed Future/CompletableFuture;
   - CreateWorldScreen / WorldCreationContext lifecycle;
   - compatibility-copy path;
   - AchieveToDo mixin runtime errors.
4. Проверить фактически созданную world-local compatibility copy:
   - куда записана;
   - была ли создана полностью;
   - активирована ли;
   - не повреждена ли структура ZIP/datapack;
   - соответствует ли technical conversion правилам 26.2.
5. Проверить stale disabled/busy state Create World button только как следствие root cause, не force-enable её вслепую.
6. После исправления снова `runClient`.
7. Только когда engineering evidence подтверждает новый fix — снова попросить пользователя об ОДНОМ live-test действии.

# 25. НЕ ДЕЛАТЬ НЕПРОВЕРЕННЫХ ВЫВОДОВ

На текущем blocker возможны гипотезы:
- historical BACAP technical format incompatibility;
- datapack parse/load error;
- create-world lifecycle incompatibility;
- stale busy state;
- async future/reload failure.

Но пока это ТОЛЬКО гипотезы.

**Source of truth — фактический `latest.log`, runtime state и code path.**

Не менять код наугад.

---

# 26. ФОНОВЫЕ AUTH ERRORS

Во время runtime были фоновые Mojang/Realms ошибки:

- `401`
- `Failed to parse into SignedJWT: FabricMC`

До сих пор они не были связаны с Select World/Create World path.

Не считать их root cause автоматически.

---

# 27. ПОСЛЕ CURRENT BLOCKER — НЕ ОСТАНАВЛИВАТЬСЯ

Когда actual world creation заработает, Phase A ещё НЕ закончена.

Продолжать vertical validation.

## A. CLIENT BOOTSTRAP
- main menu
- clean Mixin startup
- no injection/apply failures

## B. CREATE WORLD
- AchieveToDo tab
- controls
- progression mode
- cooperative mode
- rewards
- custom generation
- BACAP flow

## C. HISTORICAL BACAP
- exact preserved baseline
- technical 26.2 compatibility
- no current BACAP
- no content drift

## D. INTEGRATED SERVER / WORLD START
- мир реально создаётся
- integrated server стартует
- no registry/datapack crash

## E. ADVANCEMENTS UI
- настоящий `AdvancementsScreen`
- AchieveToDo tabs/widgets
- mystified mask
- tooltips
- progress
- notifications/toast

## F. PROGRESSION / NETWORKING
- payload registration
- sync
- obtained advancement count
- demystify
- unlock/relock
- score/statistics/nearby state

## G. RESTRICTIONS
Representative checks:
- blocks
- items
- inventory
- equipment
- movement
- interactions
- tools/weapons

Особо:
- Stone
- Copper -> `USE_STONE_TOOLS`
- Iron+
- разные tool types
- regression после удаления старых SwordItem/DiggerItem/ArmorItem mixins

## H. LANDMARK / WORLDGEN
- structure landmarks
- feature landmarks
- lock logic
- detection
- persistence
- sync
- serialization

Никаких Purpur heuristics.

## I. RENDERING
- VISION blackout/fade
- screenshot guard
- locked-landmark forcefield
- фактический visual PASS

## J. PERSISTENCE
- world close
- reopen
- progression/config/landmark state

## K. FREEZE REGRESSION
Проверить:
- 151 abilities
- 22 landmarks
- `TOTAL_ADVANCEMENTS_COUNT = 1152`
- Easy snapshot
- Normal snapshot
- Hard snapshot
- deterministic Chaos snapshots
- historical BACAP IDs/content
- intentional balance/content delta = NONE

## L. PRODUCTION ARTIFACT
- clean full build
- фактически обнаружить production/remapped JAR
- НЕ угадывать filename
- проверить содержимое JAR
- size
- SHA-256
- update `PORT_26_2_STATUS.md`
- update `MIXIN_26_2_MAP.md`

После этого:
`26.2 COMPATIBILITY BASELINE — ENGINEERING CANDIDATE`

И только тогда дать пользователю следующее одно действие для финального Phase A live-test.

---

# 28. CODING / PORT RULES

1. Использовать актуальные Minecraft 26.2/Fabric semantics.
2. Не делать blind global replace для mixin descriptors/owners/ordinals/injection targets.
3. Mechanical confirmed family drift можно чинить batch'ами.
4. Каждый сложный Mixin должен сохранять исходную observable semantics.
5. Нельзя:
   - disable mixin только ради запуска;
   - превращать feature в no-op;
   - оставлять TODO/stub;
   - делать fake/null state;
   - использовать reflection hack без необходимости;
   - делать broad unrelated refactor.
6. High-risk systems:
   - AdvancementsScreen
   - Create World/datapacks
   - landmarks/worldgen/persistence
   - rendering
   тестировать как vertical prototypes.
7. Любой удаляемый Mixin должен удаляться только если доказано, что его semantics перенесена в новый корректный path.

---

# 29. CURRENT SUCCESS SNAPSHOT

На момент handoff уже достигнуто:

- historical preservation PASS
- content freeze harness создан
- Yarn -> Mojang migration PASS
- Mojang-named 1.21.4 checkpoint PASS
- 26.2 toolchain PASS
- Java 25 PASS
- `compileJava` 26.2 PASS
- full `build` PASS
- major Mixin bootstrap retargeting выполнен
- main menu PASS
- Singleplayer PASS
- Select World PASS
- Create New World screen PASS
- AchieveToDo tab PASS
- AchieveToDo controls PASS
- BACAP dependency screen PASS
- Back navigation PASS
- in-game BACAP download PASS

ПРЕДЫДУЩИЙ FAIL:

**BACAP downloaded -> Create New World -> actual world creation**

После него инженерный fix уже реализован:
- historical BACAP -> controlled world-local 26.2 compatibility copy;
- preserved global artifact остаётся нетронутым;
- compatibility layer подключён к create + reopen/join path;
- `compileJava` PASS;
- новый `runClient` дошёл до клиента без прежних datapack component/load ошибок.

ТЕКУЩИЙ СТАТУС:

**FIX READY -> USER LIVE-TEST PENDING**

Пользователь должен на текущем экране `Create New World` нажать `Create New World` один раз и сообщить, создался ли мир или клиент снова вернулся назад.

---

# 30. РЕКОМЕНДУЕМЫЙ ПЕРВЫЙ PROMPT НОВОМУ API/CODEX АГЕНТУ

Скопировать новому API/Codex агенту:

> Мы продолжаем существующий порт AchieveToDo на Minecraft/Fabric 26.2.
>
> Перед любыми изменениями полностью прочитай:
>
> `D:\Vibecode\AchieveToDo 26.2\HANDOFF_PHASE_A.md`
> `D:\Vibecode\AchieveToDo 26.2\PHASE_A_AUDIT.md`
> `D:\Vibecode\AchieveToDo 26.2\MIXIN_26_2_MAP.md`
> `D:\Vibecode\AchieveToDo 26.2\PORT_26_2_STATUS.md`
>
> Считай `HANDOFF_PHASE_A.md` авторитетным handoff-контекстом предыдущей сессии.
>
> Не начинай работу заново и не повторяй уже закрытые этапы.
> Сначала проверь текущий git/worktree и последние runtime logs.
>
> ВАЖНО: предыдущий world-creation blocker уже получил инженерный fix.
> Historical BACAP теперь переводится в controlled world-local compatibility copy для Minecraft 26.2 только в момент копирования в мир; preserved global historical artifact не изменяется. Тот же compatibility layer подключён к reopen/join path. `compileJava` PASS, новый `runClient` дошёл до клиента без прежних datapack component/load ошибок.
>
> На момент handoff Codex уже попросил меня выполнить ОДНО текущее live-test действие:
>
> **На уже открытом экране `Create New World`, после уже скачанного BACAP, нажать `Create New World` один раз и сообщить, создался ли мир или клиент снова вернулся на `Create New World`.**
>
> Поэтому НЕ повторяй диагностику и НЕ переделывай этот fix до моего ответа с результатом этого live-test.
>
> Если я сообщу, что мир создался — зафиксируй этот world-creation path как PASS и сразу продолжай Phase A с integrated-server/world runtime validation.
>
> Если я сообщу, что клиент снова вернулся на Create New World или мир не создался — считай это новым FAIL, сними свежий `run\logs\latest.log` именно с этой попытки и продолжай диагностику с нового фактического runtime evidence.
>
> Все сообщения мне пиши на русском.
>
> `MILESTONE REPORT != TASK COMPLETION`.
> Если следующий инженерный шаг известен — выполняй его, а не заканчивай ответ описанием будущего шага.
>
> Останавливайся только при настоящем blocker, необходимости gameplay/semantic решения, выходе за standing permissions, необходимости одного конкретного live-test действия от меня либо готовом `26.2 COMPATIBILITY BASELINE — ENGINEERING CANDIDATE`.
>
> При live-test всегда давай мне только ОДНО точное действие за раз.

---

# 31. FINAL REMINDER

Смысл Phase A:

**НЕ сделать “новый AchieveToDo”.**

Смысл Phase A:

**взять оригинальный AchieveToDo 1.21.4+ и технически перенести его на Minecraft 26.2, сохранив старый контент, старый баланс, старую progression semantics и оригинальный UI/visual behavior.**

Только после личного принятия Phase A пользователем можно переходить к Phase B/C/D.
