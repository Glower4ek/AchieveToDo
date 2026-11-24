package com.diskree.achievetodo.gameplay.nativeguard;

import java.util.Map;

/** Canonical LandmarkType keys. Shared by capture and reader; never infer a type from blocks. */
public final class NativeLandmarkMappings {
    public static final Map<String, String> STRUCTURES = Map.ofEntries(
        Map.entry("minecraft:desert_pyramid", "desert_pyramid"),
        Map.entry("minecraft:jungle_pyramid", "jungle_pyramid"),
        Map.entry("minecraft:pillager_outpost", "pillager_outpost"),
        Map.entry("minecraft:igloo", "igloo"), Map.entry("minecraft:swamp_hut", "swamp_hut"),
        Map.entry("minecraft:mansion", "mansion"),
        Map.entry("minecraft:village_plains", "village"), Map.entry("minecraft:village_desert", "village"),
        Map.entry("minecraft:village_savanna", "village"), Map.entry("minecraft:village_snowy", "village"),
        Map.entry("minecraft:village_taiga", "village"),
        Map.entry("minecraft:ruined_portal", "ruined_portal"), Map.entry("minecraft:ruined_portal_desert", "ruined_portal"),
        Map.entry("minecraft:ruined_portal_jungle", "ruined_portal"), Map.entry("minecraft:ruined_portal_swamp", "ruined_portal"),
        Map.entry("minecraft:ruined_portal_mountain", "ruined_portal"), Map.entry("minecraft:ruined_portal_ocean", "ruined_portal"),
        Map.entry("minecraft:ruined_portal_nether", "ruined_portal"),
        Map.entry("minecraft:buried_treasure", "buried_treasure"),
        Map.entry("minecraft:shipwreck", "shipwreck"), Map.entry("minecraft:shipwreck_beached", "shipwreck"),
        Map.entry("minecraft:ocean_ruin_cold", "ocean_ruin"), Map.entry("minecraft:ocean_ruin_warm", "ocean_ruin"),
        Map.entry("minecraft:monument", "monument"),
        Map.entry("minecraft:mineshaft", "mineshaft"), Map.entry("minecraft:mineshaft_mesa", "mineshaft"),
        Map.entry("minecraft:trail_ruins", "trail_ruins"), Map.entry("minecraft:ancient_city", "ancient_city"),
        Map.entry("minecraft:trial_chambers", "trial_chambers"), Map.entry("minecraft:stronghold", "stronghold"),
        Map.entry("minecraft:fortress", "fortress"), Map.entry("minecraft:bastion_remnant", "bastion_remnant"),
        Map.entry("minecraft:end_city", "end_city"),
        // 0.4.0: the 26.3 abandoned camp, one structure per biome.
        Map.entry("minecraft:abandoned_camp_bamboo_jungle", "abandoned_camp"),
        Map.entry("minecraft:abandoned_camp_birch_forest", "abandoned_camp"),
        Map.entry("minecraft:abandoned_camp_cherry_grove", "abandoned_camp"),
        Map.entry("minecraft:abandoned_camp_dappled_forest", "abandoned_camp"),
        Map.entry("minecraft:abandoned_camp_flower_forest", "abandoned_camp"),
        Map.entry("minecraft:abandoned_camp_forest", "abandoned_camp"),
        Map.entry("minecraft:abandoned_camp_meadow", "abandoned_camp"),
        Map.entry("minecraft:abandoned_camp_old_growth_birch_forest", "abandoned_camp"),
        Map.entry("minecraft:abandoned_camp_old_growth_pine_taiga", "abandoned_camp"),
        Map.entry("minecraft:abandoned_camp_old_growth_spruce_taiga", "abandoned_camp"),
        Map.entry("minecraft:abandoned_camp_pale_garden", "abandoned_camp"),
        Map.entry("minecraft:abandoned_camp_savanna", "abandoned_camp"),
        Map.entry("minecraft:abandoned_camp_snowy_taiga", "abandoned_camp"),
        Map.entry("minecraft:abandoned_camp_sparse_jungle", "abandoned_camp"),
        Map.entry("minecraft:abandoned_camp_swamp", "abandoned_camp"),
        Map.entry("minecraft:abandoned_camp_taiga", "abandoned_camp"),
        Map.entry("minecraft:abandoned_camp_windswept_forest", "abandoned_camp"),
        Map.entry("minecraft:abandoned_camp_wooded_badlands", "abandoned_camp")
    );
    private NativeLandmarkMappings() {}
}
