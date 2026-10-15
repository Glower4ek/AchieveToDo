package com.diskree.achievetodo.ability;

import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ToolMaterial;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** P4 pe05_copper_selector_matrix.json is the independent selector oracle.
 * Archive SHA256: 2aff0da3d85e01540b99dda4a0bfde42825ac524fb324b0ae2f8a5e1c7e50b18.
 * E128 binds native tool and spear identities; E084 binds player armor identities.
 * These unit tests do not certify successful equipping or any spear combat route.
 */
@Isolated("Temporarily binds genuine native item components, then restores registry state")
public final class PhaseDMaterialSelectorTest {
    private static final Map<Holder.Reference<?>, DataComponentMap> PREVIOUS_COMPONENTS = new IdentityHashMap<>();

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        // 26.2 registers initializers during item construction; bootstrap alone does not bind their maps.
        // Build genuine vanilla data with the full native lookup, binding only the item registry for this fixture.
        try {
            BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(VanillaRegistries.createLookup()).stream()
                .filter(pending -> pending.key().equals(Registries.ITEM))
                .forEach(pending -> pending.forEach((holder, components) -> {
                    PREVIOUS_COMPONENTS.put(holder, holder.areComponentsBound() ? holder.components() : null);
                    holder.bindComponents(components);
                }));
            assertEquals(BuiltInRegistries.ITEM.size(), PREVIOUS_COMPONENTS.size());
        } catch (RuntimeException | Error failure) {
            restoreComponents();
            throw failure;
        }
    }

    @AfterAll
    static void restoreComponents() {
        // Public native binding accepts null, which restores the original unbound state without reflection.
        PREVIOUS_COMPONENTS.forEach(Holder.Reference::bindComponents);
        PREVIOUS_COMPONENTS.forEach((holder, previous) -> {
            assertEquals(previous != null, holder.areComponentsBound());
            if (previous != null) {
                assertSame(previous, holder.components());
            }
        });
        PREVIOUS_COMPONENTS.clear();
    }

    static Stream<Arguments> materials() {
        return Stream.of(
            Arguments.of(ToolMaterial.WOOD, AbilityType.USE_WOODEN_TOOLS),
            Arguments.of(ToolMaterial.STONE, AbilityType.USE_STONE_TOOLS),
            Arguments.of(ToolMaterial.COPPER, AbilityType.USE_COPPER_TOOLS),
            Arguments.of(ToolMaterial.IRON, AbilityType.USE_IRON_TOOLS),
            Arguments.of(ToolMaterial.GOLD, AbilityType.USE_GOLDEN_TOOLS),
            Arguments.of(ToolMaterial.DIAMOND, AbilityType.USE_DIAMOND_TOOLS),
            Arguments.of(ToolMaterial.NETHERITE, AbilityType.USE_NETHERITE_TOOLS)
        );
    }

    static Stream<Arguments> tools() {
        return Stream.of(
            Arguments.of(Items.WOODEN_SWORD, AbilityType.USE_WOODEN_TOOLS, "minecraft:wooden_sword"),
            Arguments.of(Items.WOODEN_PICKAXE, AbilityType.USE_WOODEN_TOOLS, "minecraft:wooden_pickaxe"),
            Arguments.of(Items.WOODEN_AXE, AbilityType.USE_WOODEN_TOOLS, "minecraft:wooden_axe"),
            Arguments.of(Items.WOODEN_SHOVEL, AbilityType.USE_WOODEN_TOOLS, "minecraft:wooden_shovel"),
            Arguments.of(Items.WOODEN_HOE, AbilityType.USE_WOODEN_TOOLS, "minecraft:wooden_hoe"),
            Arguments.of(Items.STONE_SWORD, AbilityType.USE_STONE_TOOLS, "minecraft:stone_sword"),
            Arguments.of(Items.STONE_PICKAXE, AbilityType.USE_STONE_TOOLS, "minecraft:stone_pickaxe"),
            Arguments.of(Items.STONE_AXE, AbilityType.USE_STONE_TOOLS, "minecraft:stone_axe"),
            Arguments.of(Items.STONE_SHOVEL, AbilityType.USE_STONE_TOOLS, "minecraft:stone_shovel"),
            Arguments.of(Items.STONE_HOE, AbilityType.USE_STONE_TOOLS, "minecraft:stone_hoe"),
            Arguments.of(Items.COPPER_SWORD, AbilityType.USE_COPPER_TOOLS, "minecraft:copper_sword"),
            Arguments.of(Items.COPPER_PICKAXE, AbilityType.USE_COPPER_TOOLS, "minecraft:copper_pickaxe"),
            Arguments.of(Items.COPPER_AXE, AbilityType.USE_COPPER_TOOLS, "minecraft:copper_axe"),
            Arguments.of(Items.COPPER_SHOVEL, AbilityType.USE_COPPER_TOOLS, "minecraft:copper_shovel"),
            Arguments.of(Items.COPPER_HOE, AbilityType.USE_COPPER_TOOLS, "minecraft:copper_hoe"),
            Arguments.of(Items.GOLDEN_SWORD, AbilityType.USE_GOLDEN_TOOLS, "minecraft:golden_sword"),
            Arguments.of(Items.GOLDEN_PICKAXE, AbilityType.USE_GOLDEN_TOOLS, "minecraft:golden_pickaxe"),
            Arguments.of(Items.GOLDEN_AXE, AbilityType.USE_GOLDEN_TOOLS, "minecraft:golden_axe"),
            Arguments.of(Items.GOLDEN_SHOVEL, AbilityType.USE_GOLDEN_TOOLS, "minecraft:golden_shovel"),
            Arguments.of(Items.GOLDEN_HOE, AbilityType.USE_GOLDEN_TOOLS, "minecraft:golden_hoe"),
            Arguments.of(Items.IRON_SWORD, AbilityType.USE_IRON_TOOLS, "minecraft:iron_sword"),
            Arguments.of(Items.IRON_PICKAXE, AbilityType.USE_IRON_TOOLS, "minecraft:iron_pickaxe"),
            Arguments.of(Items.IRON_AXE, AbilityType.USE_IRON_TOOLS, "minecraft:iron_axe"),
            Arguments.of(Items.IRON_SHOVEL, AbilityType.USE_IRON_TOOLS, "minecraft:iron_shovel"),
            Arguments.of(Items.IRON_HOE, AbilityType.USE_IRON_TOOLS, "minecraft:iron_hoe"),
            Arguments.of(Items.DIAMOND_SWORD, AbilityType.USE_DIAMOND_TOOLS, "minecraft:diamond_sword"),
            Arguments.of(Items.DIAMOND_PICKAXE, AbilityType.USE_DIAMOND_TOOLS, "minecraft:diamond_pickaxe"),
            Arguments.of(Items.DIAMOND_AXE, AbilityType.USE_DIAMOND_TOOLS, "minecraft:diamond_axe"),
            Arguments.of(Items.DIAMOND_SHOVEL, AbilityType.USE_DIAMOND_TOOLS, "minecraft:diamond_shovel"),
            Arguments.of(Items.DIAMOND_HOE, AbilityType.USE_DIAMOND_TOOLS, "minecraft:diamond_hoe"),
            Arguments.of(Items.NETHERITE_SWORD, AbilityType.USE_NETHERITE_TOOLS, "minecraft:netherite_sword"),
            Arguments.of(Items.NETHERITE_PICKAXE, AbilityType.USE_NETHERITE_TOOLS, "minecraft:netherite_pickaxe"),
            Arguments.of(Items.NETHERITE_AXE, AbilityType.USE_NETHERITE_TOOLS, "minecraft:netherite_axe"),
            Arguments.of(Items.NETHERITE_SHOVEL, AbilityType.USE_NETHERITE_TOOLS, "minecraft:netherite_shovel"),
            Arguments.of(Items.NETHERITE_HOE, AbilityType.USE_NETHERITE_TOOLS, "minecraft:netherite_hoe"),
            Arguments.of(Items.DIAMOND_SPEAR, AbilityType.USE_DIAMOND_TOOLS, "minecraft:diamond_spear"),
            Arguments.of(Items.STONE_SPEAR, AbilityType.USE_STONE_TOOLS, "minecraft:stone_spear"),
            Arguments.of(Items.GOLDEN_SPEAR, AbilityType.USE_GOLDEN_TOOLS, "minecraft:golden_spear"),
            Arguments.of(Items.NETHERITE_SPEAR, AbilityType.USE_NETHERITE_TOOLS, "minecraft:netherite_spear"),
            Arguments.of(Items.WOODEN_SPEAR, AbilityType.USE_WOODEN_TOOLS, "minecraft:wooden_spear"),
            Arguments.of(Items.IRON_SPEAR, AbilityType.USE_IRON_TOOLS, "minecraft:iron_spear"),
            Arguments.of(Items.COPPER_SPEAR, AbilityType.USE_COPPER_TOOLS, "minecraft:copper_spear")
        );
    }

    static Stream<Arguments> armor() {
        return Stream.of(
            Arguments.of(Items.COPPER_HELMET, AbilityType.EQUIP_COPPER_ARMOR, "minecraft:copper_helmet"),
            Arguments.of(Items.COPPER_CHESTPLATE, AbilityType.EQUIP_COPPER_ARMOR, "minecraft:copper_chestplate"),
            Arguments.of(Items.COPPER_LEGGINGS, AbilityType.EQUIP_COPPER_ARMOR, "minecraft:copper_leggings"),
            Arguments.of(Items.COPPER_BOOTS, AbilityType.EQUIP_COPPER_ARMOR, "minecraft:copper_boots"),
            Arguments.of(Items.TURTLE_HELMET, AbilityType.EQUIP_TURTLE_HELMET, "minecraft:turtle_helmet"),
            Arguments.of(Items.ELYTRA, AbilityType.EQUIP_ELYTRA, "minecraft:elytra"),
            Arguments.of(Items.LEATHER_HELMET, AbilityType.EQUIP_LEATHER_ARMOR, "minecraft:leather_helmet"),
            Arguments.of(Items.LEATHER_CHESTPLATE, AbilityType.EQUIP_LEATHER_ARMOR, "minecraft:leather_chestplate"),
            Arguments.of(Items.LEATHER_LEGGINGS, AbilityType.EQUIP_LEATHER_ARMOR, "minecraft:leather_leggings"),
            Arguments.of(Items.LEATHER_BOOTS, AbilityType.EQUIP_LEATHER_ARMOR, "minecraft:leather_boots"),
            Arguments.of(Items.GOLDEN_HELMET, AbilityType.EQUIP_GOLDEN_ARMOR, "minecraft:golden_helmet"),
            Arguments.of(Items.GOLDEN_CHESTPLATE, AbilityType.EQUIP_GOLDEN_ARMOR, "minecraft:golden_chestplate"),
            Arguments.of(Items.GOLDEN_LEGGINGS, AbilityType.EQUIP_GOLDEN_ARMOR, "minecraft:golden_leggings"),
            Arguments.of(Items.GOLDEN_BOOTS, AbilityType.EQUIP_GOLDEN_ARMOR, "minecraft:golden_boots"),
            Arguments.of(Items.CHAINMAIL_HELMET, AbilityType.EQUIP_CHAINMAIL_ARMOR, "minecraft:chainmail_helmet"),
            Arguments.of(Items.CHAINMAIL_CHESTPLATE, AbilityType.EQUIP_CHAINMAIL_ARMOR, "minecraft:chainmail_chestplate"),
            Arguments.of(Items.CHAINMAIL_LEGGINGS, AbilityType.EQUIP_CHAINMAIL_ARMOR, "minecraft:chainmail_leggings"),
            Arguments.of(Items.CHAINMAIL_BOOTS, AbilityType.EQUIP_CHAINMAIL_ARMOR, "minecraft:chainmail_boots"),
            Arguments.of(Items.IRON_HELMET, AbilityType.EQUIP_IRON_ARMOR, "minecraft:iron_helmet"),
            Arguments.of(Items.IRON_CHESTPLATE, AbilityType.EQUIP_IRON_ARMOR, "minecraft:iron_chestplate"),
            Arguments.of(Items.IRON_LEGGINGS, AbilityType.EQUIP_IRON_ARMOR, "minecraft:iron_leggings"),
            Arguments.of(Items.IRON_BOOTS, AbilityType.EQUIP_IRON_ARMOR, "minecraft:iron_boots"),
            Arguments.of(Items.DIAMOND_HELMET, AbilityType.EQUIP_DIAMOND_ARMOR, "minecraft:diamond_helmet"),
            Arguments.of(Items.DIAMOND_CHESTPLATE, AbilityType.EQUIP_DIAMOND_ARMOR, "minecraft:diamond_chestplate"),
            Arguments.of(Items.DIAMOND_LEGGINGS, AbilityType.EQUIP_DIAMOND_ARMOR, "minecraft:diamond_leggings"),
            Arguments.of(Items.DIAMOND_BOOTS, AbilityType.EQUIP_DIAMOND_ARMOR, "minecraft:diamond_boots"),
            Arguments.of(Items.NETHERITE_HELMET, AbilityType.EQUIP_NETHERITE_ARMOR, "minecraft:netherite_helmet"),
            Arguments.of(Items.NETHERITE_CHESTPLATE, AbilityType.EQUIP_NETHERITE_ARMOR, "minecraft:netherite_chestplate"),
            Arguments.of(Items.NETHERITE_LEGGINGS, AbilityType.EQUIP_NETHERITE_ARMOR, "minecraft:netherite_leggings"),
            Arguments.of(Items.NETHERITE_BOOTS, AbilityType.EQUIP_NETHERITE_ARMOR, "minecraft:netherite_boots")
        );
    }

    @ParameterizedTest
    @MethodSource("materials")
    void nativeMaterialSingletonSelectsItsOwnAbility(ToolMaterial material, AbilityType expected) {
        assertSame(expected, AbilityType.findToolMaterialUsageAbility(material));
        ToolMaterial equivalent = new ToolMaterial(material.incorrectBlocksForDrops(), material.durability(),
            material.speed(), material.attackDamageBonus(), material.enchantmentValue(), material.repairItems());
        assertEquals(material, equivalent);
        assertNotSame(material, equivalent);
        assertNull(AbilityType.findToolMaterialUsageAbility(equivalent), "Value equality must not grant a material ability");
    }

    @ParameterizedTest
    @MethodSource("tools")
    void exactNativeToolOrSpearSelectsItsMaterial(Item item, AbilityType expected, String expectedId) {
        assertSame(expected, AbilityType.findToolMaterialUsageAbility(item));
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        assertEquals(expectedId, id.toString());
        assertSame(item, BuiltInRegistries.ITEM.getValue(id));
        assertEquals("minecraft", id.getNamespace());
        if (id.getPath().startsWith("copper_")) {
            assertNotSame(AbilityType.USE_STONE_TOOLS, AbilityType.findToolMaterialUsageAbility(item));
        }
    }

    @ParameterizedTest
    @MethodSource("armor")
    void onlyExactPlayerArmorKeepsItsEquipmentAbility(Item item, AbilityType expected, String expectedId) {
        assertEquals(expectedId, BuiltInRegistries.ITEM.getKey(item).toString());
        assertSame(expected, AbilityType.findEquipmentEquipAbility(item));
        assertNull(AbilityType.findToolMaterialUsageAbility(item));
        assertTrue(item.components().get(DataComponents.EQUIPPABLE).canBeEquippedBy(
            BuiltInRegistries.ENTITY_TYPE.getOrThrow(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.withDefaultNamespace("player")))));
    }

    static Stream<Item> animalEquipment() {
        return Stream.concat(Stream.of(Items.WOLF_ARMOR, Items.SADDLE, Items.COPPER_HORSE_ARMOR,
            Items.LEATHER_HORSE_ARMOR, Items.GOLDEN_HORSE_ARMOR, Items.IRON_HORSE_ARMOR,
            Items.DIAMOND_HORSE_ARMOR, Items.NETHERITE_HORSE_ARMOR),
            BuiltInRegistries.ITEM.stream().filter(item -> {
                String path = BuiltInRegistries.ITEM.getKey(item).getPath();
                return path.endsWith("_harness") || (path.endsWith("_carpet")
                    && item.components().get(DataComponents.EQUIPPABLE) != null);
            }));
    }

    @ParameterizedTest
    @MethodSource("animalEquipment")
    void nativeInvalidAnimalEquipmentDoesNotAcquirePlayerAbility(Item item) {
        assertNull(AbilityType.findEquipmentEquipAbility(item));
        assertNull(AbilityType.findToolMaterialUsageAbility(item));
        assertFalse(item.components().get(DataComponents.EQUIPPABLE).canBeEquippedBy(
            BuiltInRegistries.ENTITY_TYPE.getOrThrow(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.withDefaultNamespace("player")))));
    }

    @Test
    void nativeAnimalControlCoverageCannotDisappear() {
        List<Item> controls = animalEquipment().toList();
        assertEquals(40, controls.size()); // eight animal items, sixteen harnesses, sixteen llama carpets
        assertEquals(40, controls.stream().distinct().count());
    }

    @Test
    void nullAndUnrelatedInputsRemainUnmapped() {
        assertNull(AbilityType.findToolMaterialUsageAbility((ToolMaterial) null));
        assertNull(AbilityType.findToolMaterialUsageAbility((Item) null));
        assertNull(AbilityType.findEquipmentEquipAbility(null));
        for (Item item : List.of(Items.COPPER_INGOT,
                BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("copper_block")),
                Items.HONEYCOMB, Items.STICK,
                Items.TRIDENT, Items.MACE, Items.SHEARS, Items.SHIELD, Items.BOW)) {
            assertNull(AbilityType.findToolMaterialUsageAbility(item));
            assertNull(AbilityType.findEquipmentEquipAbility(item));
        }
    }

    @Test
    void sevenNativeSpearIdsRemainSevenDistinctMaterialPermissions() {
        Map<String, AbilityType> expected = Map.of("wooden_spear", AbilityType.USE_WOODEN_TOOLS,
            "stone_spear", AbilityType.USE_STONE_TOOLS, "copper_spear", AbilityType.USE_COPPER_TOOLS,
            "golden_spear", AbilityType.USE_GOLDEN_TOOLS, "iron_spear", AbilityType.USE_IRON_TOOLS,
            "diamond_spear", AbilityType.USE_DIAMOND_TOOLS, "netherite_spear", AbilityType.USE_NETHERITE_TOOLS);
        List<Item> registered = BuiltInRegistries.ITEM.stream()
            .filter(item -> BuiltInRegistries.ITEM.getKey(item).getPath().endsWith("_spear")).toList();
        assertEquals(7, registered.size());
        assertEquals(7, registered.stream().map(AbilityType::findToolMaterialUsageAbility).distinct().count());
        for (Item spear : registered) {
            String path = BuiltInRegistries.ITEM.getKey(spear).getPath();
            assertTrue(expected.containsKey(path));
            assertSame(expected.get(path),
                AbilityType.findToolMaterialUsageAbility(spear));
            assertNull(AbilityType.findEquipmentEquipAbility(spear));
        }
    }

    @Test
    void acceptedIdentityAndProgressionContractsRemainIntact() {
        PhaseDAbilityMappingTest.verifyOldIdentities();
        PhaseDProgressionCompatibilityTest.verifyOldPresets();
        assertEquals(153, AbilityType.values().length);
        assertEquals(151, AbilityType.USE_COPPER_TOOLS.ordinal());
        assertEquals(152, AbilityType.EQUIP_COPPER_ARMOR.ordinal());
        List<Map<AbilityType, Integer>> presets = List.of(Progressions.getEasyProgression(),
            Progressions.getNormalProgression(), Progressions.getHardProgression());
        int[] tools = {98, 128, 158}; int[] armor = {68, 83, 98};
        for (int i = 0; i < 3; i++) {
            assertEquals(tools[i], presets.get(i).get(AbilityType.USE_COPPER_TOOLS));
            assertEquals(armor[i], presets.get(i).get(AbilityType.EQUIP_COPPER_ARMOR));
        }
    }
}
