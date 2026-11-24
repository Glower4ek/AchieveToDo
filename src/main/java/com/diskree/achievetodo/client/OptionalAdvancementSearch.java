package com.diskree.achievetodo.client;

import net.minecraft.resources.Identifier;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Optional interop with the installed AdvancementsSearch screen, without linking its classes. */
public final class OptionalAdvancementSearch {
    public static final String MOD_ID = "advancements_search";

    private OptionalAdvancementSearch() {}

    /**
     * Asks AdvancementsSearch to center and flash one advancement. {@code widget} is that advancement's widget on
     * {@code screen}. Two layouts are known:
     * <ul>
     *   <li>1.3.1 and later (Minecraft 26.3): the screen mixin keeps its state in a patch object
     *   ({@code getOrInitPatch()}), whose {@code startFlashing(AdvancementWidget)} flashes the widget;</li>
     *   <li>1.3.0: the screen holds {@code flashingAdvancementId} and {@code advancements_search$stopFlashing()}.</li>
     * </ul>
     * Anything else returns false, and the caller reports that the integration is unavailable.
     */
    public static boolean requestHighlight(Object screen, Identifier advancementId, Object widget) {
        return widget != null && flashWidget(screen, widget) || flashIdentifier(screen, advancementId);
    }

    private static boolean flashWidget(Object screen, Object widget) {
        try {
            Method patchAccessor = declared(screen.getClass(), "getOrInitPatch");
            if (patchAccessor == null || patchAccessor.getParameterCount() != 0 || !patchAccessor.trySetAccessible()) return false;
            Object patch = patchAccessor.invoke(screen);
            if (patch == null) return false;
            for (Class<?> type = patch.getClass(); type != null; type = type.getSuperclass()) {
                for (Method method : type.getDeclaredMethods()) {
                    if (method.getName().equals("startFlashing") && method.getParameterCount() == 1
                        && method.getParameterTypes()[0].isInstance(widget) && method.trySetAccessible()) {
                        method.invoke(patch, widget);
                        return true;
                    }
                }
            }
            return false;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            return false;
        }
    }

    private static boolean flashIdentifier(Object screen, Identifier advancementId) {
        try {
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

    private static Method declared(Class<?> type, String name) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getName().equals(name)) return method;
            }
        }
        return null;
    }
}
