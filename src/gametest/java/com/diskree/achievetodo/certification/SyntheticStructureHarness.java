package com.diskree.achievetodo.certification;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.util.RandomSource;
import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SyntheticStructureHarness {
    private SyntheticStructureHarness() {
    }

    public static InjectedStructure inject(ServerLevel level, Holder<Structure> structureHolder, BlockPos playerPos) {
        LevelChunk chunk = level.getChunkAt(playerPos);
        Map<Structure, StructureStart> originalStarts = new HashMap<>(chunk.getAllStarts());
        Map<Structure, LongSet> originalReferences = copyReferences(chunk.getAllReferences());

        BoundingBox pieceBox = new BoundingBox(
            playerPos.getX() - 1,
            playerPos.getY(),
            playerPos.getZ() - 1,
            playerPos.getX() + 1,
            playerPos.getY() + 2,
            playerPos.getZ() + 1
        );
        StructurePiece piece = new SyntheticBoundingBoxPiece(pieceBox);
        StructureStart start = new StructureStart(
            structureHolder.value(),
            chunk.getPos(),
            0,
            new PiecesContainer(List.of(piece))
        );
        if (!start.isValid()) {
            throw new IllegalStateException("Injected StructureStart must be valid");
        }

        chunk.setStartForStructure(structureHolder.value(), start);
        chunk.addReferenceForStructure(structureHolder.value(), chunk.getPos().pack());
        return new InjectedStructure(chunk, originalStarts, originalReferences, start, pieceBox);
    }

    public record InjectedStructure(
        LevelChunk chunk,
        Map<Structure, StructureStart> originalStarts,
        Map<Structure, LongSet> originalReferences,
        StructureStart injectedStart,
        BoundingBox pieceBox
    ) implements AutoCloseable {
        @Override
        public void close() {
            chunk.setAllStarts(new HashMap<>(originalStarts));
            chunk.setAllReferences(copyReferences(originalReferences));
        }
    }

    private static Map<Structure, LongSet> copyReferences(Map<Structure, LongSet> references) {
        Map<Structure, LongSet> copy = new HashMap<>();
        for (Map.Entry<Structure, LongSet> entry : references.entrySet()) {
            copy.put(entry.getKey(), new LongOpenHashSet(entry.getValue()));
        }
        return copy;
    }

    private static final class SyntheticBoundingBoxPiece extends StructurePiece {
        private SyntheticBoundingBoxPiece(BoundingBox boundingBox) {
            super(StructurePieceType.STRONGHOLD_STRAIGHT, 0, boundingBox);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        }

        @Override
        public void postProcess(
            WorldGenLevel level,
            StructureManager structureManager,
            ChunkGenerator chunkGenerator,
            RandomSource random,
            BoundingBox chunkBox,
            ChunkPos chunkPos,
            BlockPos pivot
        ) {
        }
    }
}

