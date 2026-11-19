package com.diskree.achievetodo.adapters;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.tests.TestSupport;

public final class BukkitRestrictionResolverTest {

    private BukkitRestrictionResolverTest() {
    }

    public static void run() {
        TestSupport.assertEquals(
            AbilityType.OPEN_DOOR,
            BukkitRestrictionResolver.resolveBlockInteractionAbility("oak_door", false).orElseThrow()
        );
        TestSupport.assertFalse(BukkitRestrictionResolver.resolveBlockInteractionAbility("oak_door", true).isPresent());
        TestSupport.assertEquals(
            AbilityType.OPEN_CHEST,
            BukkitRestrictionResolver.resolveBlockInteractionAbility("chest", false).orElseThrow()
        );
        TestSupport.assertEquals(
            AbilityType.THROW_ENDER_PEARL,
            BukkitRestrictionResolver.resolveItemUseAbility("ender_pearl").orElseThrow()
        );
        TestSupport.assertEquals(
            AbilityType.EAT_GOLDEN_CARROT,
            BukkitRestrictionResolver.resolveConsumableAbility("golden_carrot").orElseThrow()
        );
        TestSupport.assertEquals(
            AbilityType.USE_IRON_TOOLS,
            BukkitRestrictionResolver.resolveToolMaterialAbility("iron_axe").orElseThrow()
        );
        TestSupport.assertEquals(
            AbilityType.EQUIP_DIAMOND_ARMOR,
            BukkitRestrictionResolver.resolveEquipmentAbility("diamond_chestplate").orElseThrow()
        );
        TestSupport.assertEquals(
            AbilityType.TRADE_WITH_LIBRARIAN,
            BukkitRestrictionResolver.resolveTradeAbility("librarian").orElseThrow()
        );
        TestSupport.assertEquals(
            AbilityType.GET_INTO_BOAT,
            BukkitRestrictionResolver.resolveVehicleEnterAbility("oak_boat").orElseThrow()
        );
        TestSupport.assertEquals(
            AbilityType.GET_INTO_MINECART,
            BukkitRestrictionResolver.resolveVehicleEnterAbility("hopper_minecart").orElseThrow()
        );
        TestSupport.assertEquals(
            AbilityType.ENTER_NETHER,
            BukkitRestrictionResolver.resolvePortalAbility(org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.NETHER_PORTAL).orElseThrow()
        );
        TestSupport.assertEquals(
            AbilityType.TELEPORT_OUTER_ISLANDS,
            BukkitRestrictionResolver.resolvePortalAbility(org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.END_GATEWAY).orElseThrow()
        );
    }
}
