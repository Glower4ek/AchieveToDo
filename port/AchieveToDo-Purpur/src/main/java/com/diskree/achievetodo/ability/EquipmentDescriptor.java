package com.diskree.achievetodo.ability;

public record EquipmentDescriptor(String itemKey, String materialKey) {

    public static EquipmentDescriptor ofItem(String itemKey) {
        return new EquipmentDescriptor(itemKey, null);
    }

    public static EquipmentDescriptor ofMaterial(String materialKey) {
        return new EquipmentDescriptor(null, materialKey);
    }
}
