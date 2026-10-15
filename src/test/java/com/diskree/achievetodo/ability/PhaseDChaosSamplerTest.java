package com.diskree.achievetodo.ability;

import com.diskree.achievetodo.injection.extension.main.LevelInfoExtension.ConfigurationIO;
import com.diskree.achievetodo.client.gui.AbilityUnlockedToastType;
import com.google.gson.*;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ToolMaterial;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

final class PhaseDChaosSamplerTest {
    // Hash-bound CP-CHAOS oracle; constants are populated from the accepted archive, never production output.
    private static final String VECTORS = """
[{"seed":0,"key":"use_copper_tools","trace":{"digest_hex":"ba8baf18b71f88dbe20f3248c7f00bbabc021656b7d8db66885efa686c1e72c6","derived_signed_long":-5004714040205801253,"random_initial_48":192540866473654,"pool_index":130,"candidate":684,"priority":70,"magnitude":11,"positive":true,"shifted":695,"ratio":0.1855685103857611,"interpolated":595,"initial_flag_draw_percent":15.296890472868363,"permanent_flag_draw_percent":15.413147823770856,"value":595}},{"seed":0,"key":"equip_copper_armor","trace":{"digest_hex":"bae69baf06be0cd8d19c145faf44db75f0904e769e9ddd505a5fd04c03eb7106","derived_signed_long":-4979121161976804136,"random_initial_48":171158076058293,"pool_index":33,"candidate":90,"priority":60,"magnitude":17,"positive":true,"shifted":107,"ratio":0.26565915400789447,"interpolated":105,"initial_flag_draw_percent":35.45037362072827,"permanent_flag_draw_percent":16.317738138108716,"value":105}},{"seed":1,"key":"use_copper_tools","trace":{"digest_hex":"a218100f0305906576aec1f7d6e5fc5db7d05cb757452f7f4f19fa2ba1a73041","derived_signed_long":-6766640783462920091,"random_initial_48":17638858782216,"pool_index":111,"candidate":532,"priority":70,"magnitude":12,"positive":false,"shifted":520,"ratio":0.595077219642589,"interpolated":305,"initial_flag_draw_percent":50.53247101874872,"permanent_flag_draw_percent":23.147434674825874,"value":305}},{"seed":1,"key":"equip_copper_armor","trace":{"digest_hex":"218d75ed6110bc591186de4cc5ad075301f65b380e5cd734eb79784e00b23766","derived_signed_long":2417718237364010073,"random_initial_48":129642513848884,"pool_index":39,"candidate":110,"priority":60,"magnitude":14,"positive":false,"shifted":96,"ratio":0.3842790798412135,"interpolated":97,"initial_flag_draw_percent":34.382161713513504,"permanent_flag_draw_percent":86.36487536094893,"value":97}},{"seed":-1,"key":"use_copper_tools","trace":{"digest_hex":"01148782c394d9910ba4bb9add41ec3fc25f5f743a5c0c1de70edb98a9baed20","derived_signed_long":77836089268951441,"random_initial_48":149014384754684,"pool_index":97,"candidate":420,"priority":70,"magnitude":6,"positive":true,"shifted":426,"ratio":0.16370116945517013,"interpolated":382,"initial_flag_draw_percent":95.19643689422142,"permanent_flag_draw_percent":99.00133862109847,"value":382}},{"seed":-1,"key":"equip_copper_armor","trace":{"digest_hex":"e8772c2d537ff630f12de9cf679d309259aabc9fb8a7ca98945ae0da3998852f","derived_signed_long":-1695838161495656912,"random_initial_48":48552685539421,"pool_index":81,"candidate":318,"priority":60,"magnitude":1,"positive":false,"shifted":317,"ratio":0.022746015177072133,"interpolated":312,"initial_flag_draw_percent":27.24110307776013,"permanent_flag_draw_percent":27.523780483977323,"value":312}},{"seed":9223372036854775807,"key":"use_copper_tools","trace":{"digest_hex":"ff7ad5cba710df35ed2e9de6c86fc8376119b29138ab9c802a4d2b518d44065d","derived_signed_long":-37482576221249739,"random_initial_48":235082786552152,"pool_index":86,"candidate":348,"priority":70,"magnitude":4,"positive":false,"shifted":344,"ratio":0.024576412726957174,"interpolated":339,"initial_flag_draw_percent":59.74103820350781,"permanent_flag_draw_percent":92.15941104717051,"value":339}},{"seed":9223372036854775807,"key":"equip_copper_armor","trace":{"digest_hex":"6d5b3378fe3fc92868d57a5784e336253efd0ccd27773f5ead328caedd4b3649","derived_signed_long":7879948567769499944,"random_initial_48":56612514639685,"pool_index":48,"candidate":146,"priority":60,"magnitude":10,"positive":true,"shifted":156,"ratio":0.4249713147308305,"interpolated":131,"initial_flag_draw_percent":82.74450502248936,"permanent_flag_draw_percent":58.02055675843166,"value":131}},{"seed":-9223372036854775808,"key":"use_copper_tools","trace":{"digest_hex":"41c903a6b6908c3872974b5acb5e6caf506241bbd2e91131fbd02b764879ba33","derived_signed_long":4740324097346538552,"random_initial_48":4000367536725,"pool_index":68,"candidate":240,"priority":70,"magnitude":3,"positive":false,"shifted":237,"ratio":0.042888544320979737,"interpolated":234,"initial_flag_draw_percent":96.55886310337324,"permanent_flag_draw_percent":57.6632259803328,"value":234}},{"seed":-9223372036854775808,"key":"equip_copper_armor","trace":{"digest_hex":"184819faba45b0ef9ab6d081f215e05f7c05010695a50e1c07e3ed43cbd87645","derived_signed_long":1749677019891085551,"random_initial_48":28584696174210,"pool_index":58,"candidate":182,"priority":60,"magnitude":13,"positive":true,"shifted":195,"ratio":0.2980982205092468,"interpolated":166,"initial_flag_draw_percent":75.98025029741983,"permanent_flag_draw_percent":92.943822870444,"value":166}},{"seed":17,"key":"use_copper_tools","trace":{"digest_hex":"fb2fcb703fa6e162cf1519a26df705c9a59963e1783a6502bb8db00231b51406","derived_signed_long":-346834963319561886,"random_initial_48":223707151337231,"pool_index":7,"candidate":16,"priority":70,"magnitude":7,"positive":false,"shifted":9,"ratio":0.6820243685006709,"interpolated":111,"initial_flag_draw_percent":0.4792777941805215,"permanent_flag_draw_percent":null,"value":0}},{"seed":82,"key":"use_copper_tools","trace":{"digest_hex":"2126e73d974aa707a494f0dd0470174cb1795173b31fa040543d8ca9737c8e58","derived_signed_long":2388850904083900167,"random_initial_48":254228939817322,"pool_index":25,"candidate":58,"priority":70,"magnitude":5,"positive":true,"shifted":63,"ratio":0.07813856990904876,"interpolated":70,"initial_flag_draw_percent":93.61718988001041,"permanent_flag_draw_percent":0.07677009398463497,"value":-1}},{"seed":90,"key":"equip_copper_armor","trace":{"digest_hex":"810592918ce9789e2ad9eb961498627b51da46f7753c3c70f2fe06c08f832fea","derived_signed_long":-9149745914101270370,"random_initial_48":161165728915187,"pool_index":40,"candidate":114,"priority":60,"magnitude":0,"positive":false,"shifted":114,"ratio":0.019750314280255043,"interpolated":114,"initial_flag_draw_percent":0.3851639090886594,"permanent_flag_draw_percent":null,"value":0}},{"seed":1603,"key":"equip_copper_armor","trace":{"digest_hex":"c9a59977c8b8d8ffceccb0c63b3036f7ae81628a7d9e3e85f89c46a555b4e697","derived_signed_long":-3916555561181062913,"random_initial_48":168715279941266,"pool_index":111,"candidate":532,"priority":60,"magnitude":18,"positive":false,"shifted":514,"ratio":0.3929793018779021,"interpolated":351,"initial_flag_draw_percent":13.280224026672316,"permanent_flag_draw_percent":0.15746021006871613,"value":-1}}]
        """;
    private static final String OLD_PRIORITIES = """
vision 100
eat_salmon 0
eat_cod 0
eat_tropical_fish 0
jump 100
swim 100
eat_rotten_flesh 40
eat_spider_eye 40
eat_sweet_berries 0
open_door 30
eat_glow_berries 0
interact_inside_village 0
sleep 100
eat_poisonous_potato 40
sneak 100
sprint 100
open_inventory 100
break_blocks 100
eat_suspicious_stew 40
use_golden_tools 70
open_chest 30
eat_chicken 0
eat_carrot 0
open_crafting_table 100
equip_golden_armor 30
open_barrel 30
eat_beetroot 0
eat_dried_kelp 0
eat_potato 0
open_stonecutter 80
get_into_boat 100
eat_apple 0
interact_inside_shipwreck 0
use_shield 70
eat_melon_slice 0
interact_inside_ruined_portal 0
open_furnace 30
eat_cookie 0
interact_inside_buried_treasure 0
eat_mushroom_stew 0
eat_beetroot_soup 0
put_in_bundle 100
eat_rabbit_stew 0
open_trapdoor 30
use_wooden_tools 70
eat_honey 0
interact_inside_igloo 0
use_water_bucket 70
eat_mutton 0
throw_snowball 30
interact_inside_ocean_ruin 0
equip_leather_armor 60
eat_pufferfish 40
open_fence_gate 30
eat_pumpkin_pie 0
eat_golden_apple 0
use_shears 30
interact_inside_desert_pyramid 0
break_blocks_in_negative_y 100
eat_enchanted_golden_apple 0
interact_inside_mineshaft 0
throw_egg 30
use_stone_tools 70
open_grindstone 30
shoot_crossbow 30
eat_rabbit 0
interact_inside_swamp_hut 0
equip_chainmail_armor 60
open_anvil 80
use_flint_and_steel 70
ignite_tnt 30
interact_inside_ancient_city 0
enter_nether 90
trade_with_wandering_trader 0
get_into_minecart 30
interact_inside_fortress 90
use_ominous_bottle 50
use_iron_tools 70
interact_inside_jungle_pyramid 0
attack_with_trident 30
equip_iron_armor 60
shoot_bow 30
interact_inside_bastion_remnant 0
use_jukebox 30
throw_ender_pearl 50
use_composter 30
charge_respawn_anchor 30
eat_beef 0
interact_inside_pillager_outpost 0
trade_with_mason 0
use_fishing_rod 30
eat_porkchop 0
trade_with_cartographer 0
interact_inside_monument 0
use_diamond_tools 30
use_cauldron 30
interact_inside_monster_room 0
eat_baked_potato 0
open_smoker 30
equip_turtle_helmet 30
interact_inside_trail_ruins 0
use_brush 30
equip_diamond_armor 30
unlock_vault 30
open_blast_furnace 30
interact_inside_stronghold 90
trade_with_leatherworker 0
use_spyglass 30
interact_inside_mansion 0
open_beacon 80
throw_wind_charge 30
enter_end 90
open_cartography_table 30
interact_inside_desert_well 0
eat_cooked_salmon 0
equip_elytra 60
trade_with_shepherd 0
trade_with_butcher 0
interact_inside_trial_chambers 0
open_ender_chest 80
attack_with_mace 30
interact_inside_end_city 0
use_ender_eye 90
teleport_outer_islands 30
use_netherite_tools 30
eat_cooked_cod 0
trade_with_farmer 0
glide_with_fireworks 30
trade_with_cleric 0
equip_netherite_armor 30
open_brewing_stand 30
eat_cooked_rabbit 0
place_end_crystal 50
trade_with_fisherman 0
open_smithing_table 80
eat_cooked_chicken 0
eat_chorus_fruit 0
trade_with_fletcher 0
eat_cooked_mutton 0
trade_with_armorer 0
eat_cooked_porkchop 0
eat_bread 0
trade_with_weaponsmith 0
eat_cooked_beef 0
open_loom 30
use_campfire 30
open_shulker_box 30
eat_golden_carrot 0
trade_with_toolsmith 0
trade_with_librarian 0
open_enchanting_table 30
        """;

    @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }

    static Object invoke(String method, Class<?>[] types, Object... args) throws Exception {
        Method m = ConfigurationIO.class.getDeclaredMethod(method, types); m.setAccessible(true);
        try { return m.invoke(null, args); }
        catch (InvocationTargetException e) {
            if (e.getCause() instanceof Exception exception) throw exception;
            throw (Error) e.getCause();
        }
    }

    static int sample(long seed, AbilityType ability) throws Exception {
        return (int) invoke("sample", new Class<?>[]{long.class, AbilityType.class}, seed, ability);
    }

    @Test void acceptedDigestDrawAndFlagVectors() throws Exception {
        int[] pool = pool();
        for (JsonElement element : JsonParser.parseString(VECTORS).getAsJsonArray()) {
            JsonObject vector = element.getAsJsonObject();
            long seed = vector.get("seed").getAsLong();
            AbilityType ability = AbilityType.findByName(vector.get("key").getAsString());
            JsonObject trace = vector.getAsJsonObject("trace");
            byte[] digest = (byte[]) invoke("digest", new Class<?>[]{long.class, AbilityType.class}, seed, ability);
            assertEquals(trace.get("digest_hex").getAsString(), HexFormat.of().formatHex(digest));
            long derived = ByteBuffer.wrap(digest).getLong();
            assertEquals(trace.get("derived_signed_long").getAsLong(), derived);
            Random r = new Random(derived);
            int index = r.nextInt(153); assertEquals(trace.get("pool_index").getAsInt(), index);
            int priority = trace.get("priority").getAsInt();
            int magnitude = r.nextInt(Math.round((100 - priority) * 0.5f));
            assertEquals(trace.get("magnitude").getAsInt(), magnitude);
            boolean positive = r.nextBoolean(); assertEquals(trace.get("positive").getAsBoolean(), positive);
            int shifted = Math.clamp(pool[index] + (positive ? magnitude : -magnitude), 2, 1000);
            assertEquals(trace.get("shifted").getAsInt(), shifted);
            double ratio = (priority / 100.0) * r.nextDouble(); assertEquals(trace.get("ratio").getAsDouble(), ratio);
            int value = (int) Math.round(shifted + ratio * ((priority == 70 ? 158 : 98) - shifted));
            assertEquals(trace.get("interpolated").getAsInt(), value);
            double initial = r.nextDouble() * 100.0; assertEquals(trace.get("initial_flag_draw_percent").getAsDouble(), initial);
            if (initial < 1.0) value = 0;
            else {
                double permanent = r.nextDouble() * 100.0; assertEquals(trace.get("permanent_flag_draw_percent").getAsDouble(), permanent);
                if (permanent < 0.2) value = -1;
            }
            assertEquals(trace.get("value").getAsInt(), value);
            assertEquals(value, sample(seed, ability));
            System.out.println("D-S01 JAVA_VECTOR " + seed + " " + ability.getName() + " " + HexFormat.of().formatHex(digest) + " " + value);
        }
    }

    private static int[] pool() throws Exception {
        Field field = ConfigurationIO.class.getDeclaredField("COPPER_POOL"); field.setAccessible(true);
        return ((int[]) field.get(null)).clone();
    }

    @Test void frozenMultisetDigestAndCopperIdentities() throws Exception {
        int[] pool = pool(); assertEquals(153, pool.length);
        String compact = new Gson().toJson(pool);
        assertEquals("603ad27cfb20ca0e3c15571e03be46be0da1ea1db0f39b975af45faa99072306",
            HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(compact.getBytes(StandardCharsets.UTF_8))));
        assertEquals(2, Arrays.stream(pool).filter(x -> x == 158).count());
        assertEquals(2, Arrays.stream(pool).filter(x -> x == 98).count());
        assertEquals(153, AbilityType.values().length);
        assertEquals(151, AbilityType.USE_COPPER_TOOLS.ordinal());
        assertEquals(152, AbilityType.EQUIP_COPPER_ARMOR.ordinal());
        assertEquals(70, AbilityType.USE_COPPER_TOOLS.getChaosPriority());
        assertEquals(60, AbilityType.EQUIP_COPPER_ARMOR.getChaosPriority());
        assertEquals(AbilityUnlockedToastType.TOOL, AbilityType.USE_COPPER_TOOLS.getUnlockToastType());
        assertEquals(AbilityUnlockedToastType.EQUIPMENT, AbilityType.EQUIP_COPPER_ARMOR.getUnlockToastType());
        assertEquals(AbilitiesHierarchyLayerType.UPGRADE, AbilityType.USE_COPPER_TOOLS.getHierarchyLayerType());
        assertEquals(AbilitiesHierarchyLayerType.UPGRADE, AbilityType.EQUIP_COPPER_ARMOR.getHierarchyLayerType());
        assertSame(Items.COPPER_PICKAXE, AbilityType.USE_COPPER_TOOLS.getIcon());
        assertSame(Items.COPPER_CHESTPLATE, AbilityType.EQUIP_COPPER_ARMOR.getIcon());
        // D-S02 activates native copper material and item selectors.
        assertSame(AbilityType.USE_COPPER_TOOLS,
            AbilityType.findToolMaterialUsageAbility(ToolMaterial.COPPER));
        assertSame(AbilityType.USE_COPPER_TOOLS,
            AbilityType.findToolMaterialUsageAbility(Items.COPPER_SWORD));
        for (String row : OLD_PRIORITIES.lines().filter(s -> !s.isBlank()).toList()) {
            String[] parts = row.trim().split(" "); assertEquals(Integer.parseInt(parts[1]), AbilityType.findByName(parts[0]).getChaosPriority());
        }
        List<Map<AbilityType, Integer>> presets = List.of(Progressions.getEasyProgression(), Progressions.getNormalProgression(), Progressions.getHardProgression());
        for (int i = 0; i < 3; i++) {
            assertEquals(153, presets.get(i).size());
            assertEquals(new int[]{98,128,158}[i], presets.get(i).get(AbilityType.USE_COPPER_TOOLS));
            assertEquals(new int[]{68,83,98}[i], presets.get(i).get(AbilityType.EQUIP_COPPER_ARMOR));
        }
        PhaseDAbilityMappingTest.verifyOldIdentities(); PhaseDProgressionCompatibilityTest.verifyOldPresets();
    }

    // Independent 48-bit LCG; inherited Random nextInt/nextDouble exercise Java's rejection/overflow behavior.
    private static final class Lcg extends Random {
        private long state;
        int draws;
        Lcg(long seed) { state = (seed ^ 0x5DEECE66DL) & ((1L << 48) - 1); }
        @Override protected int next(int bits) {
            draws++; state = (state * 0x5DEECE66DL + 11) & ((1L << 48) - 1); return (int) (state >>> (48 - bits));
        }
    }

    @Test void independentLcgMatchesJavaIncludingRejection() throws Exception {
        for (long seed : new long[]{0,1,-1,Long.MIN_VALUE,Long.MAX_VALUE,0x5DEECE66DL}) {
            Random java = new Random(seed); Lcg oracle = new Lcg(seed);
            for (int i = 0; i < 1000; i++) {
                assertEquals(java.nextInt(153), oracle.nextInt(153));
                assertEquals(java.nextInt(15), oracle.nextInt(15));
                assertEquals(java.nextBoolean(), oracle.nextBoolean());
                assertEquals(java.nextDouble(), oracle.nextDouble());
                assertEquals(java.nextInt(1073741825), oracle.nextInt(1073741825));
            }
            assertTrue(oracle.draws > 6000, "Large bound must consume rejection draws beyond six mandatory draws per iteration");
        }
        for (long seed = -250; seed <= 250; seed++) {
            for (AbilityType ability : List.of(AbilityType.USE_COPPER_TOOLS, AbilityType.EQUIP_COPPER_ARMOR)) {
                byte[] digest = (byte[]) invoke("digest",new Class<?>[]{long.class,AbilityType.class},seed,ability);
                Lcg r = new Lcg(ByteBuffer.wrap(digest).getLong());
                int p = ability == AbilityType.USE_COPPER_TOOLS ? 70 : 60;
                int candidate = pool()[r.nextInt(153)], magnitude = r.nextInt(Math.round((100-p)*0.5f));
                int shifted = Math.clamp(candidate + (r.nextBoolean() ? magnitude : -magnitude),2,1000);
                int value = (int)Math.round(shifted + (p/100.0)*r.nextDouble()*((p==70?158:98)-shifted));
                if (r.nextDouble()*100.0 < 1.0) value=0;
                else if (r.nextDouble()*100.0 < 0.2) value=-1;
                assertEquals(value,sample(seed,ability));
            }
        }
    }

    @Test void packagedResourceIdentitiesAndTranslations() throws Exception {
        for (String lang : List.of("en_us","ru_ru")) {
            try (var stream = getClass().getResourceAsStream("/assets/achievetodo/lang/"+lang+".json")) {
                assertNotNull(stream); JsonObject json=JsonParser.parseString(new String(stream.readAllBytes(),StandardCharsets.UTF_8)).getAsJsonObject();
                for (AbilityType a : List.of(AbilityType.USE_COPPER_TOOLS,AbilityType.EQUIP_COPPER_ARMOR))
                    for(String suffix:List.of("name","description","locked_message")) assertFalse(json.get("achievetodo.ability."+a.getName()+"."+suffix).getAsString().isBlank());
            }
        }
        for (String key : List.of("use_copper_tools","equip_copper_armor")) {
            try(var stream=getClass().getResourceAsStream("/data/achievetodo/advancement/abilities/"+key+".json")) {
                assertNotNull(stream); JsonObject json=JsonParser.parseString(new String(stream.readAllBytes(),StandardCharsets.UTF_8)).getAsJsonObject();
                assertEquals("achievetodo:abilities/root",json.get("parent").getAsString());
                assertEquals("achievetodo:abilities/"+key,json.getAsJsonObject("rewards").get("function").getAsString());
                assertEquals("minecraft:"+(key.startsWith("use")?"copper_pickaxe":"copper_chestplate"),json.getAsJsonObject("display").getAsJsonObject("icon").get("id").getAsString());
                assertEquals(Set.of("demystified","unlocked"),json.getAsJsonObject("criteria").keySet());
            }
        }
    }
}
