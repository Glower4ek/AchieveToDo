# Mixin 26.2 Map

## `WorldRendererMixin`

- Historical pre-26.2 hook:
  - synthetic target `method_62216`
  - injection point after `MultiBufferSource$BufferSource.endBatch()`
  - old rendering path based on `RenderType.worldBorder(...)`, `Tesselator`, `BufferUploader`, and direct `RenderSystem` state mutation

- 26.2 semantic equivalent:
  - weather/world-border render stage inside `LevelRenderer.lambda$addWeatherPass$0(GpuBufferSlice, int)`
  - injection point immediately after vanilla `WorldBorderRenderer.render(...)`

- 26.2 rendering adaptation:
  - custom locked-landmark cuboid mesh is now built with `ByteBufferBuilder` + `BufferBuilder`
  - draw submission now uses `RenderPass` + `RenderPipelines.WORLD_BORDER`
  - texture source remains vanilla `WorldBorderRenderer.FORCEFIELD_LOCATION`
  - render target selection follows vanilla world-border behavior: `weatherTarget()` when available, otherwise `gameRenderer.mainRenderTarget()`

- Rationale:
  - the old synthetic method and immediate-mode world-border path no longer exist in 26.2
  - this preserves AchieveToDo observable behavior as a textured forcefield/border around locked landmarks while moving to the current Blaze3D state/pipeline model

## `WorldRendererMixin` state access

- Historical pre-26.2 state access:
  - shadowed `LevelRenderer.level`
  - used only to resolve the current dimension before choosing the locked-landmark forcefield box set

- 26.2 semantic equivalent:
  - `LevelRenderer` no longer stores a shadowable `ClientLevel level` field
  - current dimension is read from `Minecraft.getInstance().level` at render time

- Rationale:
  - preserves the same forcefield box selection semantics without fake/null placeholder fields
  - matches the current client renderer architecture where the world reference is owned by `Minecraft`, not exposed as the old renderer field

## `DeathScreenMixin`

- Historical pre-26.2 hook:
  - wrapped `Component.translatable("deathScreen.score.value", ...)` inside `DeathScreen.init()`
  - replaced hardcore death score text with obtained-advancements count when AchieveToDo client state was ready

- 26.2 semantic equivalent:
  - vanilla now constructs `deathScore` in `DeathScreen.<init>(Component, boolean, LocalPlayer)`
  - `init()` only creates buttons and no longer emits the score translatable call

- 26.2 adaptation:
  - moved the wrap target from `init()` to the live constructor `translatable(String, Object[])` call
  - retained the original hardcore gate and `AchieveToDoClient.isNotReady()` safeguard

- Rationale:
  - preserves the same visible AchieveToDo hardcore death-screen behavior at the point where 26.2 actually builds the score component
