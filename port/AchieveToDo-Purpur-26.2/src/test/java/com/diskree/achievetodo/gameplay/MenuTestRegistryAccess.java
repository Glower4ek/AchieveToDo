package com.diskree.achievetodo.gameplay;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import org.bukkit.entity.Villager;
import org.bukkit.Keyed;
import org.bukkit.Registry;
import org.bukkit.inventory.MenuType;

/** Paper's InventoryType loads MenuType from a registry even in event-only tests.
 * This fixture supplies inert menu descriptors, never gameplay state or event outcomes. */
public final class MenuTestRegistryAccess implements RegistryAccess {
    @Override public <T extends Keyed> Registry<T> getRegistry(Class<T> type) { return registry(type == Villager.Profession.class ? type : null); }
    @Override public <T extends Keyed> Registry<T> getRegistry(RegistryKey<T> key) {
        return registry(key == RegistryKey.MENU ? MenuType.Typed.class
            : key == RegistryKey.VILLAGER_PROFESSION ? Villager.Profession.class : null);
    }
    @SuppressWarnings("unchecked")
    private static <T extends Keyed> Registry<T> registry(Class<?> descriptorType) {
        var values = new HashMap<Object,Object>();
        return (Registry<T>) Proxy.newProxyInstance(Registry.class.getClassLoader(), new Class<?>[]{Registry.class},
            (self, method, args) -> switch(method.getName()) {
                case "get", "getOrThrow" -> descriptorType == null ? null : values.computeIfAbsent(args[0], key ->
                    Proxy.newProxyInstance(descriptorType.getClassLoader(), new Class<?>[]{descriptorType},
                        (descriptor, operation, parameters) -> switch(operation.getName()) {
                            case "hashCode" -> key.hashCode();
                            case "equals" -> descriptor == parameters[0];
                            case "getKey" -> key;
                            case "toString", "name" -> key.toString();
                            default -> null;
                        }));
                case "iterator" -> Collections.emptyIterator();
                default -> null;
            });
    }
}
