package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.injection.extension.main.ChunkExtension;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelChunk.class)
public class WorldChunkMixin {

    @Inject(
        method = "<init>(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ProtoChunk;Lnet/minecraft/world/level/chunk/LevelChunk$PostLoadProcessor;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/chunk/LevelChunk;setAllReferences(Ljava/util/Map;)V",
            shift = At.Shift.AFTER
        )
    )
    public void set(ServerLevel world, ProtoChunk protoChunk, LevelChunk.PostLoadProcessor entityLoader, CallbackInfo ci) {
        LevelChunk worldChunk = (LevelChunk) (Object) this;
        if (worldChunk instanceof ChunkExtension worldChunkExtension &&
            protoChunk instanceof ChunkExtension protoChunkExtension
        ) {
            worldChunkExtension.achievetodo$setFeatureLandmarks(
                world,
                protoChunkExtension.achievetodo$getFeatureLandmarks()
            );
        }
    }
}
