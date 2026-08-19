package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MushroomCow.class)
public class MooshroomEntityMixin {

    @Inject(
        method = "mobInteract",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/entity/animal/cow/MushroomCow;stewEffects:Lnet/minecraft/world/item/component/SuspiciousStewEffects;",
            shift = At.Shift.BEFORE,
            ordinal = 0
        ),
        cancellable = true
    )
    public void lockInteract1(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        MushroomCow mooshroomEntity = (MushroomCow) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, mooshroomEntity)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(
        method = "mobInteract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/cow/MushroomCow;level()Lnet/minecraft/world/level/Level;",
            shift = At.Shift.BEFORE,
            ordinal = 0
        ),
        cancellable = true
    )
    public void lockInteract2(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        MushroomCow mooshroomEntity = (MushroomCow) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, mooshroomEntity) ||
            AchieveToDoMod.isAbilityLocked(player, AbilityType.USE_SHEARS)
        ) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }

    @Inject(
        method = "mobInteract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;consume(ILnet/minecraft/world/entity/LivingEntity;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract3(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        MushroomCow mooshroomEntity = (MushroomCow) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, mooshroomEntity)) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
