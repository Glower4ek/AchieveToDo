package com.diskree.achievetodo.certification.probe;
import com.diskree.achievetodo.certification.Final19CauldronResultProbe;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class Final19CauldronPacketResultMixin {
    @WrapOperation(method="handleUseItemOn",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerPlayerGameMode;useItemOn(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult observe(ServerPlayerGameMode mode,ServerPlayer player,Level level,ItemStack stack,InteractionHand hand,BlockHitResult hit,Operation<InteractionResult> original){
        var result=original.call(mode,player,level,stack,hand,hit);Final19CauldronResultProbe.record(player.getUUID(),result);return result;
    }
}
