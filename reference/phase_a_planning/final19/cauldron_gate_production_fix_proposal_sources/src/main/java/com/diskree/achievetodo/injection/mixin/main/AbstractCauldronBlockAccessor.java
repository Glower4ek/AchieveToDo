package com.diskree.achievetodo.injection.mixin.main;

import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractCauldronBlock.class)
public interface AbstractCauldronBlockAccessor {

    @Accessor("interactions")
    CauldronInteraction.Dispatcher achievetodo$getInteractions();
}
