package com.diskree.achievetodo.client;

import com.diskree.achievetodo.injection.mixin.main.PlacedAdvancementMixin;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AdvancementChildrenRefreshTest {
    @Test void additionsAndSameSizeReplacementsInvalidateCachedChildren() throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        var mixin = new PlacedAdvancementMixin() { public AdvancementNode root() { return null; } };
        var holder = PlacedAdvancementMixin.class.getDeclaredField("holder"); holder.setAccessible(true);
        holder.set(mixin, new AdvancementHolder(Identifier.parse("blazeandcave:biomes/root"), null));
        mixin.children = new HashSet<>();
        var a = node("a"); var b = node("b"); var c = node("c"); var d = node("d");
        mixin.children.addAll(List.of(b, a)); assertEquals(List.of(a,b), sorted(mixin));
        mixin.children.add(c); assertEquals(List.of(a,b,c), sorted(mixin));
        mixin.children.remove(b); mixin.children.add(d); assertEquals(List.of(a,c,d), sorted(mixin));
        mixin.children.remove(c); assertEquals(List.of(a,d), sorted(mixin));
    }
    private static AdvancementNode node(String id) {
        return new AdvancementNode(new AdvancementHolder(Identifier.parse("blazeandcave:biomes/"+id), null), null);
    }
    private static List<AdvancementNode> sorted(PlacedAdvancementMixin mixin) {
        var result = new CallbackInfoReturnable<Iterable<AdvancementNode>>("children", true);
        mixin.sortChildren(result); var nodes = new ArrayList<AdvancementNode>();
        result.getReturnValue().forEach(nodes::add); return nodes;
    }
}
