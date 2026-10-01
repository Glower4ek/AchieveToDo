package com.diskree.achievetodo.certification.probe;
import com.diskree.achievetodo.certification.Final19ShieldProbe;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import net.minecraft.advancements.triggers.EntityHurtPlayerTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(LivingEntity.class)
public abstract class Final19ShieldDamageMixin {
 @WrapOperation(method="hurtServer",at=@At(value="INVOKE",target="Lnet/minecraft/advancements/triggers/EntityHurtPlayerTrigger;trigger(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/damagesource/DamageSource;FFZ)V"))
 private void observed(EntityHurtPlayerTrigger trigger,ServerPlayer p,DamageSource source,float dealt,float taken,boolean blocked,Operation<Void> original){Final19ShieldProbe.observe(p,source,dealt,taken,blocked);original.call(trigger,p,source,dealt,taken,blocked);}
}
