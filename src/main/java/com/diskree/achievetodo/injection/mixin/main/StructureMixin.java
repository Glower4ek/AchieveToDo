package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.ability.LandmarkType;
import com.diskree.achievetodo.injection.extension.main.StructureStartExtension;
import com.diskree.achievetodo.util.MixinCasting;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.StructureType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Structure.class)
public abstract class StructureMixin {

    @Shadow
    public abstract StructureType<?> type();

    @ModifyReturnValue(
        method = "generate",
        at = @At(
            value = "RETURN",
            ordinal = 0
        )
    )
    private StructureStart setStructureLandmarkType(
        StructureStart structureStart,
        @Local(argsOnly = true) Holder<Structure> structure
    ) {
        LandmarkType landmarkType = LandmarkType.findByStructureRegistryKey(structure.unwrapKey().orElse(null));
        if (landmarkType != null) {
            StructureStartExtension structureStartExtension = MixinCasting.structureStart(structureStart);
            structureStartExtension.achievetodo$setLandmarkType(landmarkType);
        }
        return structureStart;
    }
}
