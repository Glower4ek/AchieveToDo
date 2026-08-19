package com.diskree.achievetodo.ability;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public enum DimensionType {

    OVERWORLD,
    NETHER,
    END;

    public static @Nullable DimensionType findByWorld(ResourceKey<Level> world) {
        if (world == Level.OVERWORLD) {
            return OVERWORLD;
        }
        if (world == Level.NETHER) {
            return NETHER;
        }
        if (world == Level.END) {
            return END;
        }
        return null;
    }
}
