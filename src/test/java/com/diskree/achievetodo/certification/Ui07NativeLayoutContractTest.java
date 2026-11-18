package com.diskree.achievetodo.certification;

import net.minecraft.client.gui.screens.advancements.AdvancementTabType;
import com.diskree.achievetodo.client.gui.AdvancementsTabType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Native corner geometry regression: distinct slot identities can still intersect. */
public class Ui07NativeLayoutContractTest {
    private boolean overlaps(AdvancementTabType a,int ai,AdvancementTabType b,int bi) throws Exception {
        var type=Class.forName("com.diskree.achievetodo.injection.mixin.client.AdvancementsScreenMixin");
        var method=type.getDeclaredMethod("overlapsNativeTab",AdvancementTabType.class,int.class,AdvancementTabType.class,int.class);
        method.setAccessible(true);return (boolean)method.invoke(null,a,ai,b,bi);
    }
    @Test void nativeCornerIntersectionsCannotAliasDistinctSlots() throws Exception {
        assertTrue(overlaps(AdvancementTabType.LEFT,4,AdvancementTabType.BELOW,0));
        assertTrue(overlaps(AdvancementTabType.ABOVE,7,AdvancementTabType.RIGHT,0));
        assertTrue(overlaps(AdvancementTabType.BELOW,7,AdvancementTabType.RIGHT,4));
        assertFalse(overlaps(AdvancementTabType.LEFT,3,AdvancementTabType.BELOW,0));
    }
    @Test void exactNativeRectangleOracleAcrossAllPositions() throws Exception {
        int cases=0;
        for(var a:AdvancementTabType.values())for(int ai=0;ai<a.getMax();ai++)
            for(var b:AdvancementTabType.values())for(int bi=0;bi<b.getMax();bi++) {
                // Independent oracle: Java AWT rectangle intersection against native dimensions.
                var ra=new java.awt.Rectangle(a.getX(ai),a.getY(ai),a.getWidth(),a.getHeight());
                var rb=new java.awt.Rectangle(b.getX(bi),b.getY(bi),b.getWidth(),b.getHeight());
                assertEquals(ra.intersects(rb),overlaps(a,ai,b,bi),a+":"+ai+" / "+b+":"+bi);cases++;
            }
        assertEquals(676,cases);
    }
    @Test void sixGreedyForeignPositionsPreserveAllSeventeenReservedRoots() throws Exception {
        var allocated=new java.util.ArrayList<java.util.Map.Entry<AdvancementTabType,Integer>>();
        for(var a:AdvancementTabType.values())for(int ai=0;ai<a.getMax();ai++) {
            boolean blocked=false;
            for(var reserved:AdvancementsTabType.values())if(overlaps(a,ai,reserved.getPosition(),reserved.getOrder()))blocked=true;
            for(var prior:allocated)if(overlaps(a,ai,prior.getKey(),prior.getValue()))blocked=true;
            if(!blocked)allocated.add(java.util.Map.entry(a,ai));
        }
        assertEquals(java.util.List.of("ABOVE:7","BELOW:7","LEFT:3","RIGHT:1","RIGHT:2","RIGHT:3"),allocated.stream().map(e->e.getKey()+":"+e.getValue()).toList());
        assertEquals(17,AdvancementsTabType.values().length);
    }
}
