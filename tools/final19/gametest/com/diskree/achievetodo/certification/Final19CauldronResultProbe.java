package com.diskree.achievetodo.certification;
import java.util.*;
import net.minecraft.world.InteractionResult;
/** Observe only armed owned actors; no result replacement and no interaction replay. */
public final class Final19CauldronResultProbe {
    private static final Map<UUID,List<InteractionResult>> ACTIVE=new HashMap<>();
    public static void arm(UUID id){ACTIVE.put(id,new ArrayList<>());}
    public static void record(UUID id,InteractionResult result){var values=ACTIVE.get(id);if(values!=null)values.add(result);}
    public static InteractionResult take(UUID id){var values=ACTIVE.remove(id);Final19StaticContext.require(values!=null&&values.size()==1,"Expected exactly one native useItemOn return: "+values);return values.getFirst();}
    public static void clear(UUID id){ACTIVE.remove(id);}
}
