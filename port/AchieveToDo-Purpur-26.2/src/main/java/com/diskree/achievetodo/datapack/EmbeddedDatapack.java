package com.diskree.achievetodo.datapack;

import com.diskree.achievetodo.ability.AbilityBinding;
import com.diskree.achievetodo.ability.AbilityBindingType;
import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.ability.LandmarkType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class EmbeddedDatapack {

    public static final String MOD_ID = "achievetodo";
    public static final String MOD_NAME = "AchieveToDo";
    public static final String TEXT_COLOR_NAME = "yellow";
    public static final String ABILITY_ADVANCEMENT_PREFIX = "abilities/";
    public static final String ROOT_ADVANCEMENT_ID = MOD_ID + ":" + ABILITY_ADVANCEMENT_PREFIX + "root";
    public static final int MIN_FORMAT_MAJOR = 107;
    public static final int MIN_FORMAT_MINOR = 1;
    public static final int MAX_FORMAT = 107;
    public static final String DATAPACK_DESCRIPTION = "AchieveToDo embedded synthetic ability datapack";
    public static final String ROOT_BACKGROUND = "minecraft:textures/block/pale_moss_block.png";
    public static final String DEMYSTIFIED_CRITERION = "demystified";
    public static final String UNLOCKED_CRITERION = "unlocked";

    private EmbeddedDatapack() {
    }

    public static Map<String, String> renderResources() {
        LinkedHashMap<String, String> resources = new LinkedHashMap<>();
        resources.put("pack.mcmeta", renderPackMcmeta());
        resources.put("data/minecraft/tags/function/load.json", renderLoadTag());
        resources.put("data/achievetodo/function/load.mcfunction", renderLoadFunction());
        resources.put("data/achievetodo/advancement/abilities/root.json", renderRootAdvancement());
        for (AbilityType abilityType : AbilityType.values()) {
            resources.put(relativeAdvancementPath(abilityType), renderAbilityAdvancement(abilityType));
            resources.put(relativeFunctionPath(abilityType), renderUnlockFunction(abilityType));
        }
        return resources;
    }

    public static void writeToDirectory(Path datapackRoot) throws IOException {
        Objects.requireNonNull(datapackRoot, "datapackRoot");
        for (Map.Entry<String, String> entry : renderResources().entrySet()) {
            Path target = datapackRoot.resolve(entry.getKey().replace('/', java.io.File.separatorChar));
            Files.createDirectories(target.getParent());
            Files.writeString(target, entry.getValue(), StandardCharsets.UTF_8);
        }
    }

    public static String relativeAdvancementPath(AbilityType abilityType) {
        return "data/achievetodo/advancement/abilities/" + abilityType.getName() + ".json";
    }

    public static String relativeFunctionPath(AbilityType abilityType) {
        return "data/achievetodo/function/abilities/" + abilityType.getName() + ".mcfunction";
    }

    public static String renderPackMcmeta() {
        return """
            {
              "pack": {
                "description": "%s",
                "max_format": %d,
                "min_format": [
                  %d,
                  %d
                ]
              }
            }
            """.formatted(
            escapeJson(DATAPACK_DESCRIPTION),
            MAX_FORMAT,
            MIN_FORMAT_MAJOR,
            MIN_FORMAT_MINOR
        );
    }

    public static String renderLoadTag() {
        return """
            {
              "values": [
                "achievetodo:load"
              ]
            }
            """;
    }

    public static String renderLoadFunction() {
        return """
            # Required by the embedded Purpur datapack bootstrap.
            # Intentionally left empty until runtime server logic is added in a later stage.
            """;
    }

    public static String renderRootAdvancement() {
        return """
            {
              "display": {
                "icon": {
                  "id": "minecraft:barrier",
                  "count": 1
                },
                "title": {
                  "text": "%s"
                },
                "description": {
                  "translate": "%s.description"
                },
                "background": "%s",
                "frame": "task",
                "show_toast": false,
                "announce_to_chat": false,
                "hidden": false
              },
              "criteria": {
                "tick": {
                  "trigger": "minecraft:tick"
                }
              },
              "requirements": [
                [
                  "tick"
                ]
              ]
            }
            """.formatted(
            escapeJson(MOD_NAME),
            MOD_ID,
            ROOT_BACKGROUND
        );
    }

    public static String renderAbilityAdvancement(AbilityType abilityType) {
        String name = abilityType.getName();
        String advancementId = abilityType.getAdvancementId();
        String iconId = "minecraft:" + resolveIconKey(abilityType);
        return """
            {
              "parent": "%s",
              "display": {
                "icon": {
                  "id": "%s",
                  "count": 1
                },
                "title": {
                  "translate": "%s.ability.%s.name"
                },
                "description": {
                  "translate": "%s.ability.%s.description"
                },
                "frame": "task",
                "show_toast": true,
                "announce_to_chat": false,
                "hidden": false
              },
              "rewards": {
                "function": "%s"
              },
              "criteria": {
                "%s": {
                  "trigger": "minecraft:impossible"
                },
                "%s": {
                  "trigger": "minecraft:impossible"
                }
              },
              "requirements": [
                [
                  "%s"
                ],
                [
                  "%s"
                ]
              ]
            }
            """.formatted(
            ROOT_ADVANCEMENT_ID,
            iconId,
            MOD_ID,
            name,
            MOD_ID,
            name,
            advancementId,
            DEMYSTIFIED_CRITERION,
            UNLOCKED_CRITERION,
            DEMYSTIFIED_CRITERION,
            UNLOCKED_CRITERION
        );
    }

    public static String renderUnlockFunction(AbilityType abilityType) {
        String name = abilityType.getName();
        String advancementId = abilityType.getAdvancementId();
        return """
            tellraw @a {"translate":"%s.ability_unlocked_chat_message","with":[{"selector":"@s"},{"color":"%s","text":"["},{"color":"%s","translate":"%s.ability.%s.name","clickEvent":{"action":"run_command","value":"/advancementssearch highlight %s obtained_status"},"hoverEvent":{"action":"show_text","contents":{"color":"%s","translate":"%s.ability.%s.name","extra":[{"text":"\\n"},{"color":"%s","translate":"%s.ability.%s.description"},{"text":"\\n\\n"},{"color":"gray","italic":true,"translate":"%%1$s tab","with":[{"text":"%s"}]}]}}},{"color":"%s","text":"]"}]}
            """.formatted(
            MOD_ID,
            TEXT_COLOR_NAME,
            TEXT_COLOR_NAME,
            MOD_ID,
            name,
            advancementId,
            TEXT_COLOR_NAME,
            MOD_ID,
            name,
            TEXT_COLOR_NAME,
            MOD_ID,
            name,
            MOD_NAME,
            TEXT_COLOR_NAME
        );
    }

    private static String resolveIconKey(AbilityType abilityType) {
        AbilityBinding binding = abilityType.getBinding();
        return switch (binding.type()) {
            case ITEM, BLOCK, FOOD, EQUIPMENT_ITEM -> binding.key();
            case TOOL_MATERIAL -> switch (binding.key()) {
                case "copper" -> "copper_pickaxe";
                case "wood" -> "wooden_pickaxe";
                case "stone" -> "stone_pickaxe";
                case "gold" -> "golden_pickaxe";
                case "iron" -> "iron_pickaxe";
                case "diamond" -> "diamond_pickaxe";
                case "netherite" -> "netherite_pickaxe";
                default -> throw unsupportedBinding(abilityType, binding);
            };
            case EQUIPMENT_MATERIAL -> switch (binding.key()) {
                case "copper" -> "copper_chestplate";
                case "leather" -> "leather_chestplate";
                case "gold" -> "golden_chestplate";
                case "chain" -> "chainmail_chestplate";
                case "iron" -> "iron_chestplate";
                case "diamond" -> "diamond_chestplate";
                case "netherite" -> "netherite_chestplate";
                default -> throw unsupportedBinding(abilityType, binding);
            };
            case PORTAL -> switch (binding.key()) {
                case "netherportalblock" -> "obsidian";
                case "endportalblock" -> "end_portal_frame";
                case "endgatewayblock" -> "chorus_flower";
                default -> throw unsupportedBinding(abilityType, binding);
            };
            case TRADE_TARGET -> switch (binding.key()) {
                case "wandering_trader" -> "wandering_trader_spawn_egg";
                case "mason" -> "stonecutter";
                case "cartographer" -> "cartography_table";
                case "leatherworker" -> "cauldron";
                case "shepherd" -> "loom";
                case "butcher" -> "smoker";
                case "farmer" -> "composter";
                case "cleric" -> "brewing_stand";
                case "fisherman" -> "barrel";
                case "fletcher" -> "fletching_table";
                case "armorer" -> "blast_furnace";
                case "weaponsmith" -> "grindstone";
                case "toolsmith" -> "smithing_table";
                case "librarian" -> "lectern";
                default -> throw unsupportedBinding(abilityType, binding);
            };
            case LANDMARK -> resolveLandmarkIconKey(abilityType.getLandmarkType());
        };
    }

    private static String resolveLandmarkIconKey(LandmarkType landmarkType) {
        return switch (Objects.requireNonNull(landmarkType, "landmarkType")) {
            case DESERT_PYRAMID -> "chiseled_sandstone";
            case DESERT_WELL -> "sandstone";
            case JUNGLE_PYRAMID -> "mossy_cobblestone";
            case PILLAGER_OUTPOST -> "dark_oak_planks";
            case IGLOO -> "snow_block";
            case SWAMP_HUT -> "cauldron";
            case MANSION -> "dark_oak_log";
            case VILLAGE -> "bell";
            case RUINED_PORTAL -> "crying_obsidian";
            case BURIED_TREASURE -> "sand";
            case SHIPWRECK -> "oak_planks";
            case OCEAN_RUIN -> "sea_lantern";
            case MONUMENT -> "prismarine";
            case MONSTER_ROOM -> "spawner";
            case MINESHAFT -> "rail";
            case TRAIL_RUINS -> "suspicious_gravel";
            case ANCIENT_CITY -> "reinforced_deepslate";
            case TRIAL_CHAMBERS -> "trial_spawner";
            case STRONGHOLD -> "infested_cracked_stone_bricks";
            case FORTRESS -> "chiseled_nether_bricks";
            case BASTION_REMNANT -> "gilded_blackstone";
            case END_CITY -> "purpur_block";
        };
    }

    private static IllegalStateException unsupportedBinding(AbilityType abilityType, AbilityBinding binding) {
        return new IllegalStateException("Unsupported datapack icon mapping for " + abilityType + ": " + binding.type() + "/" + binding.key());
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
