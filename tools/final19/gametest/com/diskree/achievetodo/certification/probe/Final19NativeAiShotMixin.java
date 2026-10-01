package com.diskree.achievetodo.certification.probe;
import com.diskree.achievetodo.certification.Final19ShieldProbe;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.illager.Pillager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin({AbstractSkeleton.class,Pillager.class})
public abstract class Final19NativeAiShotMixin {
 @Inject(method="performRangedAttack",at=@At("HEAD"))
 private void observe(LivingEntity target,float power,CallbackInfo ci){Final19ShieldProbe.aiShot((Entity)(Object)this);}
}
