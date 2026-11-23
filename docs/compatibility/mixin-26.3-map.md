# Mixin 26.3 Map

What changed in the Fabric mod's injection points when moving from Minecraft 26.2 to 26.3, and why each new hook
keeps the 26.2 behaviour. The 26.2 rationale is in [mixin-26.2-map.md](mixin-26.2-map.md).

Checked with a mixin audit (`MixinEnvironment.audit()` on the dedicated server and on the client), which applies
every mixin to every target class: both pass on 26.3.

## Landmark features: `FeatureMixin` -> `FeaturePlacerMixin`

- 26.2: `Feature<FC>` was a generic type with singleton instances (`Feature.DESERT_WELL`, `Feature.MONSTER_ROOM`);
  the mixin wrapped `Feature.place(FeaturePlaceContext)` inside `Feature.place(FC, WorldGenLevel, ...)`.
- 26.3: `Feature` is an interface whose instances are configured features registered under `ResourceKey<Feature>`
  (`MiscOverworldFeatures.DESERT_WELL` is now a sequence of template features, `CaveFeatures.MONSTER_ROOM` a
  `MonsterRoomFeature` record). Placement goes through `FeaturePlacer.place(PlacedFeature, RandomSource, BlockPos, boolean)`.
- Adaptation: `LandmarkType` keys feature landmarks by `ResourceKey<Feature>`; the mixin wraps both `Feature.place`
  calls in `FeaturePlacer.place` and resolves the landmark from `placedFeature.feature().unwrapKey()`. The block-box
  tracking around the call is unchanged.

## Tool block transforms: `AxeItemMixin`, `HoeItemMixin`, `ShovelItemMixin` -> `BlockTransformerMixin`

- 26.3 removed `AxeItem`, `HoeItem` and `ShovelItem`. Stripping, scraping, wax-off, tilling and path flattening are
  data-driven `BlockTransformer`s (registry `block_transformer`, item component `block_transformer`) run by
  `Item.useOn` -> `BlockTransformer.transformBlock`.
- Adaptation: one inject in `transformBlock` at the first `UseOnContext.getPlayer()` call, which is reached only after a
  transform has produced its new state and before any side effect (criterion, loot, item damage, block change, sound).
  It returns `PASS` when the target is in a locked landmark, the tool material is locked
  (`AbilityType.findToolMaterialUsageAbility(item)`), or the linked door half / double copper chest half is locked.
- Shovel campfire dowsing: no longer present in 26.3 vanilla code, so the 26.2 `lockCampfireExtinguishUsage` hook has
  nothing to guard.

## Advancement screen

- `DisplayInfo` is a record without a mutable position. Positions live on `AdvancementNode` (`setLocation`, `x()`, `y()`),
  and `AdvancementWidget` reads them once when created.
- `AdvancementTab` is built from a root `AdvancementWidget` plus icon, title and background texture path;
  `getRootNode()` became `getRootAdvancement()`; widgets come from `AdvancementWidget.createWidget(Minecraft, AdvancementNode)`.
- `AdvancementTabMixin`: custom ability-tab layout now sets node locations before `createWidget`; the wrap moved from
  `NEW AdvancementWidget` to `AdvancementWidget.createWidget`. A display without a background keeps 26.2's look
  (`minecraft:` intentional-missing texture, `AdvancementsTabType.backgroundOf`) instead of 26.3's "no tab".
- `AdvancementsScreenMixin`: 26.3 rebuilds all tabs in `onAdvancementsUpdated` (the incremental `onAddAdvancementRoot`
  listener is gone). Foreign roots get a free native slot by wrapping `AdvancementTab.create` there; mystified
  placeholders are added after the real roots for every ATD tab that has none; the abilities tab stays the default.
- `AdvancementWidgetMixin`: the widget constructor lost its `AdvancementTab` parameter; `getDescription()` -> `description()`.
- `AdvancementDisplaysMixin`: `DisplayInfo.isHidden()` -> `hidden()`.
- `AbilityAdvancementsGenerator`: `Advancement.Builder.display(..., Identifier background, ...)` was removed; the
  generator builds the same `DisplayInfo` directly.

## Locked landmark border: `WorldRendererMixin`

- 26.3 has no weather/world-border frame pass (`lambda$addWeatherPass$0`, `weatherTarget()` are gone). Translucent
  geometry is prepared in `LevelRenderer.prepareTranslucents` and drawn either in `executeClassicTransparency`
  (one shared pass) or in `executeOit` (one pass per stage, "Improved Transparency").
- GPU types moved from `com.mojang.blaze3d` to `com.mojang.renderpearl.api` (`RenderPipeline`, `PrimitiveTopology`,
  `GpuBuffer`, `GpuBufferSlice`, `RenderPass`); a pass now takes a `CompiledRenderPipeline` and textures are bound
  with `setUniform(name, view, sampler)`.
- Adaptation, following vanilla `WeatherEffectRenderer`: the mesh is built and written to a reused vertex buffer in
  `prepareTranslucents` (no pass open), then drawn after the vanilla world border in the classic path and after the
  weather in each OIT stage with `RenderPipelines.OIT_WORLD_BORDER.getPipeline(stage)`. The fade advances once per frame.
  The buffer is closed with the renderer.

## Smaller signature moves

| Mixin | 26.2 | 26.3 |
|---|---|---|
| `PlayerEntityMixin.lockSleep` | `startSleepInBed(BlockPos)` | `startSleepInBed(AbstractBedBlock, BlockState, BedRule, BlockPos)` |
| `WaterloggableMixin` | `LevelAccessor.setBlock(pos, state, 3)` | `LevelAccessor.setBlockAndUpdate(pos, state)` |
| `TntBlockMixin` (2 hooks) | `TntBlock.prime(Level, BlockPos, LivingEntity)` | `prime(Level, BlockPos, LivingEntity, ItemStack)` |
| `ComposterBlockMixin` | `addItem(Entity, BlockState, LevelAccessor, BlockPos, ItemStack)` | `addLayer(Entity, BlockState, ServerLevel, BlockPos, Compostable)` |
| `BlockAttachedEntityMixin` | `kill(ServerLevel)` | `kill(ServerLevel, Entity)` |
| `ClientPlayerEntityMixin` | `keyPresses` read in `LocalPlayer.tick` | read in `LocalPlayer.sendChanges` |
| `AdvancementToastMixin`, `AdvancementWidgetMixin` | `blitSprite(com/mojang/blaze3d/pipeline/RenderPipeline, ...)` | `blitSprite(com/mojang/renderpearl/api/pipeline/RenderPipeline, ...)` |

## Client code outside mixins

- `ExternalPackDownloader`: 26.3 replaced GLFW with SDL3 and dropped LWJGL tinyfd. The zip picker uses the SDL3 native
  open-file dialog (asynchronous; the result is handed to the client thread), and links open with `Blaze3D.openUri`.
- `WorldCreationTab`: `RenderPipeline` import moved to `com.mojang.renderpearl.api.pipeline`.
- `AdvancementLinkCommand`: `ClientAdvancements.getTree()` -> `tree()`.

## Still to verify in game

The audit proves the hooks apply, not that they behave. The certification campaign covers the gameplay hooks; the
locked landmark border (both transparency modes), the advancement screen layout and the SDL file dialog need a look
in the running client.
