package com.diskree.achievetodo.injection.mixin.client;

import com.diskree.achievetodo.ability.DimensionType;
import com.diskree.achievetodo.client.AchieveToDoClient;
import com.diskree.achievetodo.client.gui.DesignCodePalette;
import com.diskree.achievetodo.client.gui.LockedLandmarkBox;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.oit.OitStage;
import java.nio.ByteBuffer;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.WorldBorderRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.TimeUnit;

@Mixin(net.minecraft.client.renderer.LevelRenderer.class)
public abstract class WorldRendererMixin {

    @Unique
    private static final long LOCKED_LANDMARK_BORDER_ANIMATION_DURATION = TimeUnit.SECONDS.toMillis(6);

    @Unique
    private static final float ENTER_LOCKED_LANDMARK_BORDER_FADE_ALPHA_SPEED = 0.003f;

    @Unique
    private static final Identifier LOCKED_LANDMARK_BORDER_TEXTURE = WorldBorderRenderer.FORCEFIELD_LOCATION;

    @Unique
    private LockedLandmarkBox lastLockedLandmarkBox;

    @Unique
    private float fadeInsideLockedLandmarkAlpha;

    /** Vertex buffer reused across frames, like vanilla's weather buffer; written outside any render pass. */
    @Unique
    private @Nullable GpuBuffer lockedLandmarkBorderVertexBuffer;

    @Unique
    private int lockedLandmarkBorderQuadCount;

    @Unique
    private float lockedLandmarkBorderAlpha;

    @Shadow
    @Final
    private LevelRenderState levelRenderState;

    /*
     * 26.3: the weather/world-border frame pass is gone. Translucent geometry is prepared once per frame in
     * prepareTranslucents (no render pass open) and drawn either in executeClassicTransparency (one shared pass) or
     * in executeOit (one pass per OIT stage, "Improved Transparency"). The border follows the same split: the mesh
     * is built and uploaded here, and drawn right after the vanilla world border / weather in both paths.
     */
    @Inject(
        method = "prepareTranslucents",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;resizeAllAutoStorageIndexBuffers()V"
        )
    )
    private void prepareLockedLandmarkBorder(CallbackInfo ci) {
        lockedLandmarkBorderQuadCount = 0;
        @Nullable var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        CameraRenderState cameraRenderState = levelRenderState.cameraRenderState;
        if (cameraRenderState == null || cameraRenderState.pos == null || cameraRenderState.cullFrustum == null) {
            return;
        }
        DimensionType dimensionType = DimensionType.findByWorld(level.dimension());
        if (dimensionType == null) {
            return;
        }

        Vec3 cameraPos = cameraRenderState.pos;
        LockedLandmarkBox foundBox = null;
        for (LockedLandmarkBox lockedLandmarkBox : AchieveToDoClient.getLockedLandmarkBoxes()) {
            if (lockedLandmarkBox.dimensionType() != dimensionType) {
                continue;
            }
            if (!isCameraInside(lockedLandmarkBox.box(), cameraPos)) {
                continue;
            }
            foundBox = lockedLandmarkBox;
            updateFadeAlpha(lockedLandmarkBox, cameraRenderState);
            renderLockedLandmarkBorderBox(
                lockedLandmarkBox.box(),
                cameraPos,
                cameraRenderState.cullFrustum,
                fadeInsideLockedLandmarkAlpha
            );
            break;
        }

        if (foundBox == null) {
            fadeInsideLockedLandmarkAlpha = 0.0f;
        }
        lastLockedLandmarkBox = foundBox;
    }

    @Inject(
        method = "executeClassicTransparency",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/WorldBorderRenderer;render(Lnet/minecraft/client/renderer/state/level/WorldBorderRenderState;Lcom/mojang/renderpearl/api/commands/RenderPass;Lnet/minecraft/world/phys/Vec3;D)V",
            shift = At.Shift.AFTER
        )
    )
    private void drawLockedLandmarkBorder(CallbackInfo ci, @Local(argsOnly = true) RenderPass renderPass) {
        drawLockedLandmarkBorder(renderPass, RenderPipelines.WORLD_BORDER);
    }

    @Inject(
        method = "executeOit",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/WeatherEffectRenderer;renderOit(Lnet/minecraft/client/renderer/oit/OitStage;Lnet/minecraft/client/renderer/state/level/WeatherRenderState;Lcom/mojang/renderpearl/api/commands/RenderPass;)V",
            shift = At.Shift.AFTER
        )
    )
    private void drawLockedLandmarkBorderOit(CallbackInfo ci, @Local OitStage stage, @Local RenderPass renderPass) {
        drawLockedLandmarkBorder(renderPass, RenderPipelines.OIT_WORLD_BORDER.getPipeline(stage));
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void closeLockedLandmarkBorder(CallbackInfo ci) {
        if (lockedLandmarkBorderVertexBuffer != null) {
            lockedLandmarkBorderVertexBuffer.close();
            lockedLandmarkBorderVertexBuffer = null;
        }
    }

    @Unique
    private boolean isCameraInside(AABB box, Vec3 cameraPos) {
        float minX = (float) box.minX;
        float minY = (float) box.minY;
        float minZ = (float) box.minZ;
        float maxX = (float) box.maxX;
        float maxY = (float) box.maxY;
        float maxZ = (float) box.maxZ;

        float boxPadding;
        if (box.getXsize() > 1.0f && box.getYsize() > 1.0f && box.getZsize() > 1.0f) {
            boxPadding = 0.5f;
        } else {
            boxPadding = 0.001f;
        }

        minX += boxPadding;
        maxX -= boxPadding;
        minY += boxPadding;
        maxY -= boxPadding;
        minZ += boxPadding;
        maxZ -= boxPadding;

        return cameraPos.x >= minX && cameraPos.x < maxX &&
            cameraPos.y >= minY && cameraPos.y < maxY &&
            cameraPos.z >= minZ && cameraPos.z < maxZ;
    }

    @Unique
    private void updateFadeAlpha(LockedLandmarkBox lockedLandmarkBox, CameraRenderState cameraRenderState) {
        if (lockedLandmarkBox.equals(lastLockedLandmarkBox)) {
            fadeInsideLockedLandmarkAlpha += ENTER_LOCKED_LANDMARK_BORDER_FADE_ALPHA_SPEED;
            if (fadeInsideLockedLandmarkAlpha > 1.0f) {
                fadeInsideLockedLandmarkAlpha = 1.0f;
            }
        } else {
            fadeInsideLockedLandmarkAlpha = 0.0f;
        }
    }

    @Unique
    private void renderLockedLandmarkBorderBox(AABB box, Vec3 cameraPos, Frustum frustum, float alpha) {
        float minX = (float) box.minX;
        float minY = (float) box.minY;
        float minZ = (float) box.minZ;
        float maxX = (float) box.maxX;
        float maxY = (float) box.maxY;
        float maxZ = (float) box.maxZ;

        float boxPadding;
        if (box.getXsize() > 1.0f && box.getYsize() > 1.0f && box.getZsize() > 1.0f) {
            boxPadding = 0.5f;
        } else {
            boxPadding = 0.001f;
        }

        minX += boxPadding;
        maxX -= boxPadding;
        minY += boxPadding;
        maxY -= boxPadding;
        minZ += boxPadding;
        maxZ -= boxPadding;

        boolean isFloorVisible = frustum.cubeInFrustum(minX, minY, minZ, maxX, minY, maxZ) < 0;
        boolean isRoofVisible = frustum.cubeInFrustum(minX, maxY, minZ, maxX, maxY, maxZ) < 0;
        boolean isNorthWallVisible = frustum.cubeInFrustum(minX, minY, minZ, maxX, maxY, minZ) < 0;
        boolean isSouthWallVisible = frustum.cubeInFrustum(minX, minY, maxZ, maxX, maxY, maxZ) < 0;
        boolean isWestWallVisible = frustum.cubeInFrustum(minX, minY, minZ, minX, maxY, maxZ) < 0;
        boolean isEastWallVisible = frustum.cubeInFrustum(maxX, minY, minZ, maxX, maxY, maxZ) < 0;

        if (!isFloorVisible &&
            !isRoofVisible &&
            !isNorthWallVisible &&
            !isSouthWallVisible &&
            !isWestWallVisible &&
            !isEastWallVisible
        ) {
            return;
        }

        minX -= (float) cameraPos.x;
        maxX -= (float) cameraPos.x;
        minY -= (float) cameraPos.y;
        maxY -= (float) cameraPos.y;
        minZ -= (float) cameraPos.z;
        maxZ -= (float) cameraPos.z;

        int quadCount = 0;
        try (ByteBufferBuilder byteBufferBuilder = new ByteBufferBuilder(16 * 1024)) {
            BufferBuilder bufferBuilder = new BufferBuilder(
                byteBufferBuilder,
                PrimitiveTopology.QUADS,
                DefaultVertexFormat.POSITION_TEX
            );

            float animationTime = (float) (Util.getMillis() % LOCKED_LANDMARK_BORDER_ANIMATION_DURATION) /
                LOCKED_LANDMARK_BORDER_ANIMATION_DURATION;

            if (isFloorVisible || isRoofVisible) {
                for (float z = minZ; z < maxZ; ) {
                    float segmentZ = Math.min(1.0f, maxZ - z);
                    float textureU = 0.0f;
                    for (float x = minX; x < maxX; ) {
                        float segmentX = Math.min(1.0f, maxX - x);
                        float halfSegmentX = segmentX * 0.5f;
                        float halfSegmentZ = segmentZ * 0.5f;
                        if (isFloorVisible) {
                            renderLockedLandmarkBorderFloor(
                                bufferBuilder,
                                minY,
                                x, segmentX, halfSegmentX,
                                z, segmentZ, halfSegmentZ,
                                textureU,
                                animationTime
                            );
                            quadCount++;
                        }
                        if (isRoofVisible) {
                            renderLockedLandmarkBorderRoof(
                                bufferBuilder,
                                maxY,
                                x, segmentX, halfSegmentX,
                                z, segmentZ, halfSegmentZ,
                                textureU,
                                animationTime
                            );
                            quadCount++;
                        }
                        x += segmentX;
                        textureU += 0.5f;
                    }
                    z += segmentZ;
                }
            }

            if (isNorthWallVisible || isSouthWallVisible || isWestWallVisible || isEastWallVisible) {
                for (float y = minY; y < maxY; ) {
                    float segmentY = Math.min(1.0f, maxY - y);
                    float textureU = 0.0f;
                    if (isNorthWallVisible || isSouthWallVisible) {
                        for (float x = minX; x < maxX; ) {
                            float segmentX = Math.min(1.0f, maxX - x);
                            float halfSegmentX = segmentX * 0.5f;
                            float halfSegmentY = segmentY * 0.5f;
                            if (isNorthWallVisible) {
                                renderLockedLandmarkBorderNorthWall(
                                    bufferBuilder,
                                    minZ,
                                    x, segmentX, halfSegmentX,
                                    y, segmentY, halfSegmentY,
                                    textureU,
                                    animationTime
                                );
                                quadCount++;
                            }
                            if (isSouthWallVisible) {
                                renderLockedLandmarkBorderSouthWall(
                                    bufferBuilder,
                                    maxZ,
                                    x, segmentX, halfSegmentX,
                                    y, segmentY, halfSegmentY,
                                    textureU,
                                    animationTime
                                );
                                quadCount++;
                            }
                            x += segmentX;
                            textureU += 0.5f;
                        }
                    }
                    if (isWestWallVisible || isEastWallVisible) {
                        for (float z = minZ; z < maxZ; ) {
                            float segmentZ = Math.min(1.0f, maxZ - z);
                            float halfSegmentZ = segmentZ * 0.5f;
                            float halfSegmentY = segmentY * 0.5f;
                            if (isWestWallVisible) {
                                renderLockedLandmarkBorderWestWall(
                                    bufferBuilder,
                                    minX,
                                    y, segmentY, halfSegmentY,
                                    z, segmentZ, halfSegmentZ,
                                    textureU,
                                    animationTime
                                );
                                quadCount++;
                            }
                            if (isEastWallVisible) {
                                renderLockedLandmarkBorderEastWall(
                                    bufferBuilder,
                                    maxX,
                                    y, segmentY, halfSegmentY,
                                    z, segmentZ, halfSegmentZ,
                                    textureU,
                                    animationTime
                                );
                                quadCount++;
                            }
                            z += segmentZ;
                            textureU += 0.5f;
                        }
                    }
                    y += segmentY;
                }
            }

            if (quadCount == 0) {
                return;
            }

            MeshData meshData = bufferBuilder.build();
            if (meshData == null) {
                return;
            }
            try (meshData) {
                ByteBuffer vertices = meshData.vertexBuffer();
                GpuDevice device = RenderSystem.getDevice();
                if (lockedLandmarkBorderVertexBuffer == null || lockedLandmarkBorderVertexBuffer.size() < vertices.remaining()) {
                    if (lockedLandmarkBorderVertexBuffer != null) {
                        lockedLandmarkBorderVertexBuffer.close();
                    }
                    lockedLandmarkBorderVertexBuffer = device.createBuffer(
                        () -> "Locked landmark border vertex buffer",
                        GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST,
                        vertices.remaining()
                    );
                }
                device.createCommandEncoder().writeToBuffer(lockedLandmarkBorderVertexBuffer.slice(), vertices);
                RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS).requestIndexCount(quadCount * 6);
                lockedLandmarkBorderQuadCount = quadCount;
                lockedLandmarkBorderAlpha = alpha;
            }
        }
    }

    @Unique
    private void drawLockedLandmarkBorder(RenderPass renderPass, RenderPipeline pipeline) {
        int quadCount = lockedLandmarkBorderQuadCount;
        GpuBuffer vertexBuffer = lockedLandmarkBorderVertexBuffer;
        if (quadCount == 0 || vertexBuffer == null) {
            return;
        }
        RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
        if (!indices.hasStorage(quadCount * 6)) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        Vector4f color = new Vector4f(
            ARGB.redFloat(DesignCodePalette.IN_WORLD_RGB),
            ARGB.greenFloat(DesignCodePalette.IN_WORLD_RGB),
            ARGB.blueFloat(DesignCodePalette.IN_WORLD_RGB),
            lockedLandmarkBorderAlpha
        );
        GpuBufferSlice transformBuffer = RenderSystem.getDynamicUniforms().writeTransform(
            RenderSystem.getModelViewMatrixCopy(),
            color,
            new Vector3f(0.0f, 0.0f, 0.0f),
            new Matrix4f()
        );

        renderPass.pushDebugGroup(() -> "AchieveToDo locked landmark border");
        renderPass.setPipeline(RenderSystem.getCompiledPipeline(pipeline));
        RenderSystem.bindDefaultUniforms(renderPass);
        renderPass.setUniform("DynamicTransforms", transformBuffer);
        var texture = client.getTextureManager().getTexture(LOCKED_LANDMARK_BORDER_TEXTURE);
        renderPass.setUniform("Sampler0", texture.getTextureView(), texture.getSampler());
        renderPass.setIndexBuffer(indices.getBuffer(), indices.type());
        renderPass.setVertexBuffer(0, vertexBuffer.slice());
        renderPass.drawIndexed(quadCount * 6, 1, 0, 0, 0);
        renderPass.popDebugGroup();
    }

    @Unique
    private void renderLockedLandmarkBorderFloor(
        @NotNull BufferBuilder bufferBuilder,
        float minY,
        float x, float segmentX, float halfSegmentX,
        float z, float segmentZ, float halfSegmentZ,
        float textureU,
        float animationTime
    ) {
        bufferBuilder.addVertex(x + segmentX, minY, z).setUv(animationTime - textureU, animationTime + halfSegmentZ);
        bufferBuilder.addVertex(x + segmentX, minY, z + segmentZ).setUv(animationTime - textureU, animationTime);
        bufferBuilder.addVertex(x, minY, z + segmentZ).setUv(animationTime - textureU + halfSegmentX, animationTime);
        bufferBuilder.addVertex(x, minY, z).setUv(animationTime - textureU + halfSegmentX, animationTime + halfSegmentZ);
    }

    @Unique
    private void renderLockedLandmarkBorderRoof(
        @NotNull BufferBuilder bufferBuilder,
        float maxY,
        float x, float segmentX, float halfSegmentX,
        float z, float segmentZ, float halfSegmentZ,
        float textureU,
        float animationTime
    ) {
        bufferBuilder.addVertex(x, maxY, z).setUv(animationTime - textureU + halfSegmentX, animationTime + halfSegmentZ);
        bufferBuilder.addVertex(x, maxY, z + segmentZ).setUv(animationTime - textureU + halfSegmentX, animationTime);
        bufferBuilder.addVertex(x + segmentX, maxY, z + segmentZ).setUv(animationTime - textureU, animationTime);
        bufferBuilder.addVertex(x + segmentX, maxY, z).setUv(animationTime - textureU, animationTime + halfSegmentZ);
    }

    @Unique
    private void renderLockedLandmarkBorderNorthWall(
        @NotNull BufferBuilder bufferBuilder,
        float minZ,
        float x, float segmentX, float halfSegmentX,
        float y, float segmentY, float halfSegmentY,
        float textureU,
        float animationTime
    ) {
        bufferBuilder.addVertex(x, y, minZ).setUv(animationTime - textureU + halfSegmentX, animationTime);
        bufferBuilder.addVertex(x, y + segmentY, minZ).setUv(animationTime - textureU + halfSegmentX, animationTime + halfSegmentY);
        bufferBuilder.addVertex(x + segmentX, y + segmentY, minZ).setUv(animationTime - textureU, animationTime + halfSegmentY);
        bufferBuilder.addVertex(x + segmentX, y, minZ).setUv(animationTime - textureU, animationTime);
    }

    @Unique
    private void renderLockedLandmarkBorderSouthWall(
        @NotNull BufferBuilder bufferBuilder,
        float maxZ,
        float x, float segmentX, float halfSegmentX,
        float y, float segmentY, float halfSegmentY,
        float textureU,
        float animationTime
    ) {
        bufferBuilder.addVertex(x, y, maxZ).setUv(animationTime - textureU + halfSegmentX, animationTime);
        bufferBuilder.addVertex(x + segmentX, y, maxZ).setUv(animationTime - textureU, animationTime);
        bufferBuilder.addVertex(x + segmentX, y + segmentY, maxZ).setUv(animationTime - textureU, animationTime + halfSegmentY);
        bufferBuilder.addVertex(x, y + segmentY, maxZ).setUv(animationTime - textureU + halfSegmentX, animationTime + halfSegmentY);
    }

    @Unique
    private void renderLockedLandmarkBorderWestWall(
        @NotNull BufferBuilder bufferBuilder,
        float minX,
        float y, float segmentY, float halfSegmentY,
        float z, float segmentZ, float halfSegmentZ,
        float textureU,
        float animationTime
    ) {
        bufferBuilder.addVertex(minX, y, z).setUv(animationTime - textureU + halfSegmentZ, animationTime);
        bufferBuilder.addVertex(minX, y, z + segmentZ).setUv(animationTime - textureU, animationTime);
        bufferBuilder.addVertex(minX, y + segmentY, z + segmentZ).setUv(animationTime - textureU, animationTime + halfSegmentY);
        bufferBuilder.addVertex(minX, y + segmentY, z).setUv(animationTime - textureU + halfSegmentZ, animationTime + halfSegmentY);
    }

    @Unique
    private void renderLockedLandmarkBorderEastWall(
        @NotNull BufferBuilder bufferBuilder,
        float maxX,
        float y, float segmentY, float halfSegmentY,
        float z, float segmentZ, float halfSegmentZ,
        float textureU,
        float animationTime
    ) {
        bufferBuilder.addVertex(maxX, y + segmentY, z).setUv(animationTime - textureU + halfSegmentZ, animationTime + halfSegmentY);
        bufferBuilder.addVertex(maxX, y + segmentY, z + segmentZ).setUv(animationTime - textureU + halfSegmentZ, animationTime);
        bufferBuilder.addVertex(maxX, y, z + segmentZ).setUv(animationTime - textureU, animationTime);
        bufferBuilder.addVertex(maxX, y, z).setUv(animationTime - textureU, animationTime + halfSegmentY);
    }
}
