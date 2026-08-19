package com.diskree.achievetodo.util;

import com.diskree.achievetodo.injection.extension.main.LevelInfoExtension;
import com.diskree.achievetodo.injection.extension.main.SerializedChunkExtension;
import com.diskree.achievetodo.injection.extension.main.StructureStartExtension;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.jetbrains.annotations.NotNull;

public final class MixinCasting {

    private MixinCasting() {
    }

    public static @NotNull LevelInfoExtension levelInfo(LevelSettings levelSettings) {
        return (LevelInfoExtension) (Object) levelSettings;
    }

    public static @NotNull SerializedChunkExtension serializedChunk(SerializableChunkData serializedChunk) {
        return (SerializedChunkExtension) (Object) serializedChunk;
    }

    public static @NotNull StructureStartExtension structureStart(StructureStart structureStart) {
        return (StructureStartExtension) (Object) structureStart;
    }
}
