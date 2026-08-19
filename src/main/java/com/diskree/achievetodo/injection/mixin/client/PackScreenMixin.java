package com.diskree.achievetodo.injection.mixin.client;

import com.diskree.achievetodo.client.ExternalPack;
import com.diskree.achievetodo.client.InternalPack;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.stream.Stream;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;

@Mixin(PackSelectionScreen.class)
public class PackScreenMixin {

    @ModifyArg(
        method = "populateLists",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/packs/TransferableSelectionList;updateList(Ljava/util/stream/Stream;Lnet/minecraft/client/gui/screens/packs/PackSelectionModel$EntryBase;)V"
        ),
        index = 0
    )
    private Stream<PackSelectionModel.Entry> hideExternalAndInternalPacksOnPopulate(
        @NotNull Stream<PackSelectionModel.Entry> packs
    ) {
        return filterVisiblePacks(packs);
    }

    @ModifyArg(
        method = "filterEntries",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/packs/TransferableSelectionList;updateList(Ljava/util/stream/Stream;Lnet/minecraft/client/gui/screens/packs/PackSelectionModel$EntryBase;)V"
        ),
        index = 0
    )
    private Stream<PackSelectionModel.Entry> hideExternalAndInternalPacksOnFilter(
        @NotNull Stream<PackSelectionModel.Entry> packs
    ) {
        return filterVisiblePacks(packs);
    }

    private Stream<PackSelectionModel.Entry> filterVisiblePacks(
        @NotNull Stream<PackSelectionModel.Entry> packs
    ) {
        return packs.filter(pack -> {
            String name = pack.getId();
            for (ExternalPack externalPack : ExternalPack.values()) {
                if (name.equals(externalPack.getDatapackName())) {
                    return false;
                }
            }
            for (InternalPack internalPack : InternalPack.values()) {
                if (name.equals(internalPack.getDatapackName())) {
                    return false;
                }
            }
            return true;
        });
    }
}
