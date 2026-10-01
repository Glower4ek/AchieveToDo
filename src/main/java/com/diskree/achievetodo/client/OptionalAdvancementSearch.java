package com.diskree.achievetodo.client;

import net.minecraft.resources.Identifier;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Optional interop with the installed Search 1.3 screen, without linking its classes. */
public final class OptionalAdvancementSearch {
    public static final String MOD_ID = "advancements_search";

    private OptionalAdvancementSearch() {}

    public static boolean requestHighlight(Object screen, Identifier advancementId) {
        try {
            // Search 1.3's native startFlashing centers and flashes this identifier on render.
            Method stop = screen.getClass().getMethod("advancements_search$stopFlashing");
            Field target = screen.getClass().getDeclaredField("flashingAdvancementId");
            if (target.getType() != Identifier.class || !target.trySetAccessible()) return false;
            stop.invoke(screen);
            target.set(screen, advancementId);
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            return false;
        }
    }
}
