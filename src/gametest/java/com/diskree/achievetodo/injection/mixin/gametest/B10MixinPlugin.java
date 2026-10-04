package com.diskree.achievetodo.injection.mixin.gametest;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import java.util.List;
import java.util.Set;

/** Explicit B10 isolation; without the property every historical mixin is unchanged. */
public final class B10MixinPlugin implements IMixinConfigPlugin {
    public void onLoad(String mixinPackage) { }
    public String getRefMapperConfig() { return null; }
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        boolean b10 = System.getProperty("achievetodo.b10.mode") != null;
        boolean current = mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1).startsWith("B10");
        return current == b10;
    }
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) { }
    public List<String> getMixins() { return null; }
    public void preApply(String name, ClassNode target, String mixin, IMixinInfo info) { }
    public void postApply(String name, ClassNode target, String mixin, IMixinInfo info) { }
}
