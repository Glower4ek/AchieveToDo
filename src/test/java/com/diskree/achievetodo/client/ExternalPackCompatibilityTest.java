package com.diskree.achievetodo.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.SharedConstants;
import net.minecraft.advancements.predicates.DamageSourcePredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.triggers.EffectsChangedTrigger;
import net.minecraft.advancements.triggers.KilledTrigger;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalPackCompatibilityTest {

    private static RegistryAccess.Frozen registryAccess;

    @BeforeAll
    static void bootstrapMinecraftCodecs() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        registryAccess = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).freeze();
    }

    @TempDir
    Path tempDir;

    @Test
    void preservesEggingDudeProjectileConstraintInsideDamageType() throws Exception {
        JsonObject converted = convert("""
            {
              "criteria": {
                "egging_dude": {
                  "trigger": "minecraft:player_hurt_entity",
                  "conditions": {
                    "damage": {
                      "type": {
                        "tags": [{"id":"minecraft:is_projectile","expected":true}],
                        "direct_entity": {"type":"minecraft:egg"}
                      }
                    }
                  }
                }
              }
            }
            """);

        JsonObject damage = converted
            .getAsJsonObject("criteria")
            .getAsJsonObject("egging_dude")
            .getAsJsonObject("conditions")
            .getAsJsonObject("damage");
        assertNotNull(damage.getAsJsonObject("type"));
        assertEquals(
            "minecraft:egg",
            damage.getAsJsonObject("type")
                .getAsJsonObject("direct_entity")
                .get("entity_type")
                .getAsString()
        );
        parseCodec(
            DamageSourcePredicate.CODEC,
            damage.getAsJsonObject("type")
        );
    }

    @Test
    void preservesGlowsInTheDarkSpectralArrowConstraint() throws Exception {
        JsonObject converted = convert("""
            {
              "criteria": {
                "glows_in_the_dark": {
                  "trigger": "minecraft:player_hurt_entity",
                  "conditions": {
                    "damage": {
                      "type": {
                        "tags": [{"id":"minecraft:is_projectile","expected":true}],
                        "direct_entity": {"type":"minecraft:spectral_arrow"}
                      }
                    }
                  }
                }
              }
            }
            """);

        assertDamageDirectEntityType(converted, "glows_in_the_dark", "minecraft:spectral_arrow");
    }

    @Test
    void preservesTeleMorphEnderPearlConstraint() throws Exception {
        JsonObject converted = convert("""
            {
              "criteria": {
                "tele_morph": {
                  "trigger": "minecraft:player_hurt_entity",
                  "conditions": {
                    "damage": {
                      "type": {
                        "tags": [{"id":"minecraft:is_projectile","expected":true}],
                        "direct_entity": {"type":"minecraft:ender_pearl"}
                      }
                    }
                  }
                }
              }
            }
            """);

        assertDamageDirectEntityType(converted, "tele_morph", "minecraft:ender_pearl");
    }

    @Test
    void preservesTasteOfYourOwnMedicineKillingBlowStructure() throws Exception {
        JsonObject converted = convert("""
            {
              "criteria": {
                "taste_of_your_own_medicine": {
                  "trigger": "minecraft:player_killed_entity",
                  "conditions": {
                    "entity": {"type":"witch"},
                    "killing_blow": {
                      "direct_entity": {"type":"minecraft:potion"}
                    }
                  }
                }
              }
            }
            """);

        assertKillingBlowDirectEntityType(converted, "taste_of_your_own_medicine", "minecraft:splash_potion");
    }

    @Test
    void preservesSiblingDistanceConstraintAlongsideDamagePredicate() throws Exception {
        JsonObject converted = convert("""
            {
              "criteria": {
                "point_blank": {
                  "trigger": "minecraft:player_hurt_entity",
                  "conditions": {
                    "damage": {
                      "type": {
                        "tags": [{"id":"minecraft:is_projectile","expected":true}],
                        "direct_entity": {"type":"minecraft:arrow"}
                      }
                    },
                    "entity": {
                      "distance": {
                        "absolute": {
                          "max": 2.0
                        }
                      }
                    }
                  }
                }
              }
            }
            """);

        JsonObject conditions = converted
            .getAsJsonObject("criteria")
            .getAsJsonObject("point_blank")
            .getAsJsonObject("conditions");
        assertEquals(
            "minecraft:arrow",
            conditions.getAsJsonObject("damage")
                .getAsJsonObject("type")
                .getAsJsonObject("direct_entity")
                .get("entity_type")
                .getAsString()
        );
        assertEquals(
            2.0,
            conditions.getAsJsonObject("entity")
                .getAsJsonObject("distance")
                .getAsJsonObject("absolute")
                .get("max")
                .getAsDouble()
        );
    }

    @Test
    void preservesInventoryChangedItemPredicatesForEnchantments() throws Exception {
        JsonObject converted = convert("""
            {
              "criteria": {
                "sharpness": {
                  "trigger": "minecraft:inventory_changed",
                  "conditions": {
                    "items": [
                      {
                        "predicates": {
                          "enchantments": [
                            {
                              "enchantments": "minecraft:sharpness",
                              "levels": {
                                "min": 5
                              }
                            }
                          ]
                        }
                      }
                    ]
                  }
                }
              }
            }
            """);

        JsonObject item = converted
            .getAsJsonObject("criteria")
            .getAsJsonObject("sharpness")
            .getAsJsonObject("conditions")
            .getAsJsonArray("items")
            .get(0)
            .getAsJsonObject();
        assertTrue(item.has("predicates"));
        assertFalse(item.has("minecraft:predicates"));
        assertEquals(
            "minecraft:sharpness",
            item.getAsJsonObject("predicates")
                .getAsJsonArray("enchantments")
                .get(0)
                .getAsJsonObject()
                .get("enchantments")
                .getAsString()
        );
    }

    @Test
    void preservesNeedleSharpStoredAndDirectEnchantmentPredicatesFromLiveRegression() throws Exception {
        JsonObject converted = convert("""
            {
              "criteria": {
                "sharpness": {
                  "trigger": "minecraft:inventory_changed",
                  "conditions": {
                    "items": [
                      {
                        "predicates": {
                          "enchantments": [
                            {
                              "enchantments": "minecraft:sharpness",
                              "levels": {
                                "min": 5
                              }
                            }
                          ]
                        }
                      }
                    ]
                  }
                },
                "sharpness_book": {
                  "trigger": "minecraft:inventory_changed",
                  "conditions": {
                    "items": [
                      {
                        "predicates": {
                          "stored_enchantments": [
                            {
                              "enchantments": "minecraft:sharpness",
                              "levels": {
                                "min": 5
                              }
                            }
                          ]
                        }
                      }
                    ]
                  }
                }
              },
              "requirements": [
                [
                  "sharpness",
                  "sharpness_book"
                ]
              ]
            }
            """);

        JsonObject criteria = converted.getAsJsonObject("criteria");
        assertInventoryChangedPredicateKey(criteria, "sharpness");
        assertInventoryChangedPredicateKey(criteria, "sharpness_book");
        assertEquals(
            "minecraft:sharpness",
            criteria.getAsJsonObject("sharpness")
                .getAsJsonObject("conditions")
                .getAsJsonArray("items")
                .get(0)
                .getAsJsonObject()
                .getAsJsonObject("predicates")
                .getAsJsonArray("enchantments")
                .get(0)
                .getAsJsonObject()
                .get("enchantments")
                .getAsString()
        );
        assertEquals(
            "minecraft:sharpness",
            criteria.getAsJsonObject("sharpness_book")
                .getAsJsonObject("conditions")
                .getAsJsonArray("items")
                .get(0)
                .getAsJsonObject()
                .getAsJsonObject("predicates")
                .getAsJsonArray("stored_enchantments")
                .get(0)
                .getAsJsonObject()
                .get("enchantments")
                .getAsString()
        );
    }

    @Test
    void preservesConsumeItemPredicatesForPotionContents() throws Exception {
        JsonObject converted = convert("""
            {
              "criteria": {
                "slow_falling": {
                  "trigger": "minecraft:consume_item",
                  "conditions": {
                    "item": {
                      "items": ["minecraft:potion"],
                      "predicates": {
                        "potion_contents": "minecraft:slow_falling"
                      }
                    }
                  }
                }
              }
            }
            """);

        JsonObject item = converted
            .getAsJsonObject("criteria")
            .getAsJsonObject("slow_falling")
            .getAsJsonObject("conditions")
            .getAsJsonObject("item");
        assertTrue(item.has("predicates"));
        assertFalse(item.has("minecraft:predicates"));
        assertEquals(
            "minecraft:slow_falling",
            item.getAsJsonObject("predicates")
                .get("potion_contents")
                .getAsString()
        );
    }

    @Test
    void preservesAnAmazingStoryPlainItemConstraintFromLiveRegression() throws Exception {
        JsonObject converted = convert("""
            {
              "criteria": {
                "written_book": {
                  "trigger": "minecraft:inventory_changed",
                  "conditions": {
                    "items": [
                      {
                        "items": [
                          "minecraft:written_book"
                        ]
                      }
                    ]
                  }
                }
              }
            }
            """);

        JsonObject item = converted
            .getAsJsonObject("criteria")
            .getAsJsonObject("written_book")
            .getAsJsonObject("conditions")
            .getAsJsonArray("items")
            .get(0)
            .getAsJsonObject();
        assertEquals("minecraft:written_book", item.getAsJsonArray("items").get(0).getAsString());
        assertFalse(item.has("minecraft:items"));
    }

    @Test
    void stillConvertsEntityPredicateArrays() throws Exception {
        JsonObject converted = convert("""
            {
              "criteria": {
                "spruce_sapling": {
                  "trigger": "minecraft:thrown_item_picked_up_by_player",
                  "conditions": {
                    "entity": [
                      {
                        "condition": "minecraft:entity_properties",
                        "entity": "this",
                        "predicate": {
                          "type": "minecraft:allay"
                        }
                      }
                    ],
                    "item": {
                      "predicates": {
                        "custom_data": {
                          "Trophy": 1
                        }
                      }
                    }
                  }
                }
              }
            }
            """);

        JsonObject entityPredicate = converted
            .getAsJsonObject("criteria")
            .getAsJsonObject("spruce_sapling")
            .getAsJsonObject("conditions")
            .getAsJsonArray("entity")
            .get(0)
            .getAsJsonObject()
            .getAsJsonObject("predicate");
        assertEquals("minecraft:allay", entityPredicate.get("entity_type").getAsString());

        JsonObject item = converted
            .getAsJsonObject("criteria")
            .getAsJsonObject("spruce_sapling")
            .getAsJsonObject("conditions")
            .getAsJsonObject("item");
        assertTrue(item.has("predicates"));
        assertFalse(item.has("minecraft:predicates"));
        parseCodec(
            EffectsChangedTrigger.TriggerInstance.CODEC,
            converted.getAsJsonObject("criteria")
                .getAsJsonObject("spruce_sapling")
                .getAsJsonObject("conditions")
        );
    }

    @Test
    void preservesKillRootTriggersWithoutIntroducingExtraConditions() throws Exception {
        JsonObject converted = convert("""
            {
              "criteria": {
                "killed_something": {
                  "trigger": "minecraft:player_killed_entity"
                },
                "killed_by_something": {
                  "trigger": "minecraft:entity_killed_player"
                }
              },
              "requirements": [
                [
                  "killed_something",
                  "killed_by_something"
                ]
              ]
            }
            """);

        JsonObject criteria = converted.getAsJsonObject("criteria");
        assertEquals("minecraft:player_killed_entity", criteria.getAsJsonObject("killed_something").get("trigger").getAsString());
        assertEquals("minecraft:entity_killed_player", criteria.getAsJsonObject("killed_by_something").get("trigger").getAsString());
        assertFalse(criteria.getAsJsonObject("killed_something").has("conditions"));
        assertFalse(criteria.getAsJsonObject("killed_by_something").has("conditions"));
    }

    @Test
    void disablesVanillaAnnouncementsForBacapRewardFunctions() throws Exception {
        JsonObject converted = convert("""
            {
              "display": {
                "icon": {"id":"minecraft:white_wool"},
                "title": {"translate":"Wooly!"},
                "description": {"translate":"Obtain wool from a sheep by killing it"},
                "announce_to_chat": true
              },
              "rewards": {
                "function": "bacap_rewards:animal/wooly"
              },
              "criteria": {
                "sheep": {
                  "trigger": "minecraft:player_killed_entity"
                }
              }
            }
            """);

        assertFalse(
            converted.getAsJsonObject("display")
                .get("announce_to_chat")
                .getAsBoolean()
        );
    }

    @Test
    void preservesTasteOfYourOwnMedicineSiblingEntityConstraintFromKillFamily() throws Exception {
        JsonObject converted = convert("""
            {
              "criteria": {
                "taste_of_your_own_medicine": {
                  "trigger": "minecraft:player_killed_entity",
                  "conditions": {
                    "entity": {
                      "type": "minecraft:witch"
                    },
                    "killing_blow": {
                      "direct_entity": {
                        "type": "minecraft:potion"
                      }
                    }
                  }
                }
              }
            }
            """);

        JsonObject conditions = converted
            .getAsJsonObject("criteria")
            .getAsJsonObject("taste_of_your_own_medicine")
            .getAsJsonObject("conditions");
        assertEquals(
            "minecraft:witch",
            conditions.getAsJsonObject("entity").get("entity_type").getAsString()
        );
        assertEquals(
            "minecraft:splash_potion",
            conditions.getAsJsonObject("killing_blow")
                .getAsJsonObject("direct_entity")
                .get("entity_type")
                .getAsString()
        );
        parseCodec(
            KilledTrigger.TriggerInstance.CODEC,
            conditions
        );
    }

    @Test
    void convertsVictimArraysInsideKilledByArrowAndChanneledLightningTriggers() throws Exception {
        JsonObject converted = convert("""
            {
              "criteria": {
                "arbalistic": {
                  "trigger": "minecraft:killed_by_arrow",
                  "conditions": {
                    "victims": [
                      {"type":"minecraft:pillager"},
                      {"type":"phantom"}
                    ]
                  }
                },
                "struck_villager": {
                  "trigger": "minecraft:channeled_lightning",
                  "conditions": {
                    "victims": [
                      {"type":"villager"}
                    ]
                  }
                }
              }
            }
            """);

        JsonObject arbalisticConditions = converted
            .getAsJsonObject("criteria")
            .getAsJsonObject("arbalistic")
            .getAsJsonObject("conditions");
        assertEquals(
            "minecraft:pillager",
            arbalisticConditions.getAsJsonArray("victims")
                .get(0)
                .getAsJsonObject()
                .get("entity_type")
                .getAsString()
        );
        assertEquals(
            "minecraft:phantom",
            arbalisticConditions.getAsJsonArray("victims")
                .get(1)
                .getAsJsonObject()
                .get("entity_type")
                .getAsString()
        );

        JsonObject lightningConditions = converted
            .getAsJsonObject("criteria")
            .getAsJsonObject("struck_villager")
            .getAsJsonObject("conditions");
        assertEquals(
            "minecraft:villager",
            lightningConditions.getAsJsonArray("victims")
                .get(0)
                .getAsJsonObject()
                .get("entity_type")
                .getAsString()
        );
    }

    @Test
    void admitsOnlyRawOrCurrentCompatiblePackCopies() throws Exception {
        ExternalPack externalPack = ExternalPack.BACAP;
        Path rawPack = Path.of("reference", "phase_a_preservation", "files", "final", externalPack.getFileName());
        assertTrue(Files.exists(rawPack));
        assertTrue(isAdmittedPack(rawPack, externalPack));

        Path currentCompatiblePack = tempDir.resolve("bacap-current.zip");
        writeMarkerOnlyPack(
            currentCompatiblePack,
            "achievetodo_compatibility/compat_26_2.properties",
            "compat_26_2_r15",
            externalPack.getFileName(),
            externalPack.getSha1(),
            ExternalPackCompatibility.currentRootOverrideSha1()
        );
        assertTrue(isAdmittedPack(currentCompatiblePack, externalPack));

        Path staleCompatiblePack = tempDir.resolve("bacap-stale.zip");
        writeMarkerOnlyPack(
            staleCompatiblePack,
            "achievetodo_compatibility/phase_b_26_2.properties",
            "phase_b_26_2_r6",
            externalPack.getFileName(),
            externalPack.getSha1(),
            ExternalPackCompatibility.currentRootOverrideSha1()
        );
        assertFalse(isAdmittedPack(staleCompatiblePack, externalPack));

        Path wrongSourceCompatiblePack = tempDir.resolve("bacap-wrong-source.zip");
        writeMarkerOnlyPack(
            wrongSourceCompatiblePack,
            "achievetodo_compatibility/compat_26_2.properties",
            "compat_26_2_r15",
            externalPack.getFileName(),
            "deadbeef",
            ExternalPackCompatibility.currentRootOverrideSha1()
        );
        assertFalse(isAdmittedPack(wrongSourceCompatiblePack, externalPack));

        Path wrongRootsCompatiblePack = tempDir.resolve("bacap-wrong-roots.zip");
        writeMarkerOnlyPack(
            wrongRootsCompatiblePack,
            "achievetodo_compatibility/compat_26_2.properties",
            "compat_26_2_r13",
            externalPack.getFileName(),
            externalPack.getSha1(),
            "deadbeef"
        );
        assertFalse(isAdmittedPack(wrongRootsCompatiblePack, externalPack));
    }

    @Test
    void categoryRootOverridesDoNotGrantAdvancementOutsideCoopMode() throws Exception {
        Path rootFunctionsDir = Path.of(
            "src",
            "main",
            "resources",
            "resourcepacks",
            "bacap_override",
            "data",
            "bacap_rewards",
            "function"
        );
        for (String category : new String[]{
            "adventure",
            "animal",
            "biomes",
            "building",
            "challenges",
            "enchanting",
            "end",
            "farming",
            "mining",
            "monsters",
            "nether",
            "potion",
            "redstone",
            "statistics",
            "weaponry"
        }) {
            String function = Files.readString(rootFunctionsDir.resolve(category).resolve("root.mcfunction"));
            assertFalse(
                function.contains("execute unless score coop bac_settings matches 1..2 run advancement grant @s only "),
                category
            );
        }
    }

    @Test
    void originalRootCriteriaRemainCategorySpecific() throws Exception {
        Path bacapPack = Path.of("reference", "phase_a_preservation", "files", "final", "bacap.zip");
        try (ZipFile zipFile = new ZipFile(bacapPack.toFile())) {
            JsonObject animalsRoot = readJson(zipFile, "data/minecraft/advancement/husbandry/root.json");
            assertTrue(animalsRoot.getAsJsonObject("criteria").has("consumed_item"));
            assertEquals(
                "minecraft:consume_item",
                animalsRoot.getAsJsonObject("criteria")
                    .getAsJsonObject("consumed_item")
                    .get("trigger")
                    .getAsString()
            );

            JsonObject adventureRoot = readJson(zipFile, "data/minecraft/advancement/adventure/root.json");
            assertEquals(
                "minecraft:player_killed_entity",
                adventureRoot.getAsJsonObject("criteria")
                    .getAsJsonObject("killed_something")
                    .get("trigger")
                    .getAsString()
            );

            JsonObject biomesRoot = readJson(zipFile, "data/blazeandcave/advancement/biomes/root.json");
            assertEquals(
                "minecraft:player_killed_entity",
                biomesRoot.getAsJsonObject("criteria")
                    .getAsJsonObject("killed_something")
                    .get("trigger")
                    .getAsString()
            );

            JsonObject bacon = readJson(zipFile, "data/blazeandcave/advancement/animal/bacon.json");
            assertEquals("minecraft:husbandry/root", bacon.get("parent").getAsString());

            JsonObject tarzan = readJson(zipFile, "data/blazeandcave/advancement/biomes/tarzan.json");
            assertEquals("blazeandcave:biomes/the_mighty_jungle", tarzan.get("parent").getAsString());
        }
    }

    @Test
    void rewritesTenSecondTimerDaytimeQueryToOverworldDayTimeline() throws Exception {
        String converted = convertFunction("execute store result score time bac_current_time run time query daytime");
        assertEquals(
            "execute store result score time bac_current_time run time of minecraft:overworld query minecraft:day",
            converted
        );
    }

    private static void assertInventoryChangedPredicateKey(JsonObject criteria, String criterion) {
        JsonObject item = criteria
            .getAsJsonObject(criterion)
            .getAsJsonObject("conditions")
            .getAsJsonArray("items")
            .get(0)
            .getAsJsonObject();
        assertTrue(item.has("predicates"));
        assertFalse(item.has("minecraft:predicates"));
    }

    private static void assertDamageDirectEntityType(JsonObject converted, String criterion, String expectedEntityType) {
        JsonObject damage = converted
            .getAsJsonObject("criteria")
            .getAsJsonObject(criterion)
            .getAsJsonObject("conditions")
            .getAsJsonObject("damage");
        assertNotNull(damage.getAsJsonObject("type"));
        assertEquals(
            expectedEntityType,
            damage.getAsJsonObject("type")
                .getAsJsonObject("direct_entity")
                .get("entity_type")
                .getAsString()
        );
        parseCodec(
            DamageSourcePredicate.CODEC,
            damage.getAsJsonObject("type")
        );
    }

    private static void assertKillingBlowDirectEntityType(JsonObject converted, String criterion, String expectedEntityType) {
        JsonObject killingBlow = converted
            .getAsJsonObject("criteria")
            .getAsJsonObject(criterion)
            .getAsJsonObject("conditions")
            .getAsJsonObject("killing_blow");
        assertEquals(
            expectedEntityType,
            killingBlow.getAsJsonObject("direct_entity")
                .get("entity_type")
                .getAsString()
        );
        parseCodec(
            DamageSourcePredicate.CODEC,
            killingBlow
        );
    }

    @Test
    void keepsEffectsChangedSourceArrayAndSiblingPlayerConstraintCodecCompatible() throws Exception {
        JsonObject converted = convert("""
            {
              "criteria": {
                "kill_axolotl_target": {
                  "trigger": "minecraft:effects_changed",
                  "conditions": {
                    "source": [
                      {
                        "condition": "minecraft:entity_properties",
                        "predicate": {
                          "type": "minecraft:axolotl",
                          "flags": {
                            "is_on_ground": true
                          }
                        },
                        "entity": "this"
                      }
                    ],
                    "player": {
                      "condition": "minecraft:entity_properties",
                      "entity": "this",
                      "predicate": {
                        "flags": {
                          "is_swimming": false
                        }
                      }
                    }
                  }
                }
              }
            }
            """);

        JsonObject conditions = converted
            .getAsJsonObject("criteria")
            .getAsJsonObject("kill_axolotl_target")
            .getAsJsonObject("conditions");
        JsonObject sourceCondition = conditions.getAsJsonArray("source")
            .get(0)
            .getAsJsonObject();
        assertEquals("minecraft:entity_properties", sourceCondition.get("condition").getAsString());
        assertEquals("this", sourceCondition.get("entity").getAsString());
        JsonObject sourcePredicate = sourceCondition.getAsJsonObject("predicate");
        assertEquals("minecraft:axolotl", sourcePredicate.get("entity_type").getAsString());
        assertTrue(sourcePredicate.has("flags"));
        assertTrue(conditions.get("player").isJsonArray());
        JsonObject playerCondition = conditions.getAsJsonArray("player")
            .get(0)
            .getAsJsonObject();
        assertEquals("minecraft:entity_properties", playerCondition.get("condition").getAsString());
        assertEquals("this", playerCondition.get("entity").getAsString());
        assertTrue(playerCondition.getAsJsonObject("predicate").has("flags"));
        parseCodec(EffectsChangedTrigger.TriggerInstance.CODEC, conditions);
    }

    @Test
    void keepsPotionProjectileEntityPredicateCodecCompatible() throws Exception {
        JsonObject converted = convert("""
            {
              "criteria": {
                "taste_of_your_own_medicine": {
                  "trigger": "minecraft:player_killed_entity",
                  "conditions": {
                    "entity": {
                      "type": "minecraft:witch"
                    },
                    "killing_blow": {
                      "direct_entity": {
                        "type": "minecraft:potion"
                      }
                    }
                  }
                }
              }
            }
            """);

        JsonObject directEntity = converted
            .getAsJsonObject("criteria")
            .getAsJsonObject("taste_of_your_own_medicine")
            .getAsJsonObject("conditions")
            .getAsJsonObject("killing_blow")
            .getAsJsonObject("direct_entity");
        assertEquals("minecraft:splash_potion", directEntity.get("entity_type").getAsString());
        parseCodec(EntityPredicate.CODEC, directEntity);
    }

    @Test
    void keepsTargetHitPotionProjectilePredicateCodecCompatible() throws Exception {
        JsonObject converted = convert("""
            {
              "criteria": {
                "potion": {
                  "trigger": "minecraft:target_hit",
                  "conditions": {
                    "projectile": {
                      "type": "minecraft:potion"
                    }
                  }
                }
              }
            }
            """);

        JsonObject projectile = converted
            .getAsJsonObject("criteria")
            .getAsJsonObject("potion")
            .getAsJsonObject("conditions")
            .getAsJsonObject("projectile");
        assertEquals("minecraft:splash_potion", projectile.get("entity_type").getAsString());
        parseCodec(EntityPredicate.CODEC, projectile);
    }

    @Test
    void rewritesLegacyVariantTypeSpecificPredicatesToComponentChecks() throws Exception {
        JsonObject converted = convert("""
            {
              "criteria": {
                "cat": {
                  "trigger": "minecraft:tame_animal",
                  "conditions": {
                    "entity": [
                      {
                        "condition": "minecraft:entity_properties",
                        "entity": "this",
                        "predicate": {
                          "type_specific": {
                            "type": "cat",
                            "variant": "minecraft:jellie"
                          }
                        }
                      }
                    ]
                  }
                },
                "frog": {
                  "trigger": "minecraft:player_interacted_with_entity",
                  "conditions": {
                    "entity": [
                      {
                        "condition": "minecraft:entity_properties",
                        "entity": "this",
                        "predicate": {
                          "type": "minecraft:frog",
                          "type_specific": {
                            "type": "frog",
                            "variant": "minecraft:warm"
                          }
                        }
                      }
                    ]
                  }
                },
                "wolf": {
                  "trigger": "minecraft:tame_animal",
                  "conditions": {
                    "entity": [
                      {
                        "condition": "minecraft:entity_properties",
                        "entity": "this",
                        "predicate": {
                          "type_specific": {
                            "type": "minecraft:wolf",
                            "variant": "minecraft:ashen"
                          }
                        }
                      }
                    ]
                  }
                }
              }
            }
            """);

        JsonObject criteria = converted.getAsJsonObject("criteria");
        JsonObject catPredicate = criteria.getAsJsonObject("cat")
            .getAsJsonObject("conditions")
            .getAsJsonArray("entity")
            .get(0)
            .getAsJsonObject()
            .getAsJsonObject("predicate");
        assertEquals(
            "minecraft:jellie",
            catPredicate.getAsJsonObject("components").get("minecraft:cat/variant").getAsString()
        );
        assertFalse(catPredicate.has("type_specific/cat"));

        JsonObject frogPredicate = criteria.getAsJsonObject("frog")
            .getAsJsonObject("conditions")
            .getAsJsonArray("entity")
            .get(0)
            .getAsJsonObject()
            .getAsJsonObject("predicate");
        assertEquals("minecraft:frog", frogPredicate.get("entity_type").getAsString());
        assertEquals(
            "minecraft:warm",
            frogPredicate.getAsJsonObject("components").get("minecraft:frog/variant").getAsString()
        );

        JsonObject wolfPredicate = criteria.getAsJsonObject("wolf")
            .getAsJsonObject("conditions")
            .getAsJsonArray("entity")
            .get(0)
            .getAsJsonObject()
            .getAsJsonObject("predicate");
        assertEquals(
            "minecraft:ashen",
            wolfPredicate.getAsJsonObject("components").get("minecraft:wolf/variant").getAsString()
        );
    }

    @Test
    void rewritesLegacyEntityTagPredicateWithoutWidening() throws Exception {
        JsonObject converted = convert("""
            {
              "criteria": {
                "zombie": {
                  "trigger": "minecraft:player_killed_entity",
                  "conditions": {
                    "entity": {
                      "tag": "blazeandcave:overworld_zombies",
                      "flags": {
                        "is_baby": true
                      },
                      "vehicle": {
                        "type": "minecraft:chicken"
                      }
                    }
                  }
                }
              }
            }
            """);

        JsonObject entityPredicate = converted.getAsJsonObject("criteria")
            .getAsJsonObject("zombie")
            .getAsJsonObject("conditions")
            .getAsJsonObject("entity");
        assertEquals(
            "blazeandcave:overworld_zombies",
            entityPredicate.getAsJsonObject("entity_tags")
                .getAsJsonArray("any_of")
                .get(0)
                .getAsString()
        );
        assertTrue(entityPredicate.getAsJsonObject("flags").get("is_baby").getAsBoolean());
        assertEquals(
            "minecraft:chicken",
            entityPredicate.getAsJsonObject("vehicle").get("entity_type").getAsString()
        );
        parseCodec(EntityPredicate.CODEC, entityPredicate);
    }

    @Test
    void collapsesLegacyLightningEntityStruckConditionArrayToLightningSubPredicate() throws Exception {
        JsonObject converted = convert("""
            {
              "criteria": {
                "called_shot": {
                  "trigger": "minecraft:lightning_strike",
                  "conditions": {
                    "lightning": [
                      {
                        "condition": "minecraft:entity_properties",
                        "entity": "this",
                        "predicate": {
                          "distance": {
                            "absolute": {
                              "max": 2.0
                            }
                          },
                          "entity_struck": [
                            {
                              "condition": "minecraft:entity_properties",
                              "predicate": {
                                "type": "minecraft:player"
                              }
                            }
                          ]
                        }
                      }
                    ]
                  }
                }
              }
            }
            """);

        JsonObject lightningPredicate = converted.getAsJsonObject("criteria")
            .getAsJsonObject("called_shot")
            .getAsJsonObject("conditions")
            .getAsJsonArray("lightning")
            .get(0)
            .getAsJsonObject()
            .getAsJsonObject("predicate");
        assertEquals(
            "minecraft:player",
            lightningPredicate.getAsJsonObject("type_specific/lightning")
                .getAsJsonObject("entity_struck")
                .get("entity_type")
                .getAsString()
        );
        assertEquals(
            2.0,
            lightningPredicate.getAsJsonObject("distance")
                .getAsJsonObject("absolute")
                .get("max")
                .getAsDouble()
        );
    }

    @Test
    void preservesInvertedContextAwarePredicateArraysWithoutFlattening() throws Exception {
        JsonObject converted = convert("""
            {
              "criteria": {
                "distract_piglin": {
                  "trigger": "minecraft:thrown_item_picked_up_by_entity",
                  "conditions": {
                    "player": [
                      {
                        "condition": "minecraft:inverted",
                        "term": {
                          "condition": "minecraft:entity_properties",
                          "predicate": {
                            "equipment": {
                              "head": {
                                "items": [
                                  "minecraft:golden_helmet"
                                ]
                              }
                            }
                          },
                          "entity": "this"
                        }
                      }
                    ],
                    "entity": [
                      {
                        "condition": "minecraft:entity_properties",
                        "predicate": {
                          "type": "minecraft:piglin",
                          "flags": {
                            "is_baby": false
                          }
                        },
                        "entity": "this"
                      }
                    ]
                  }
                }
              }
            }
            """);

        JsonObject conditions = converted
            .getAsJsonObject("criteria")
            .getAsJsonObject("distract_piglin")
            .getAsJsonObject("conditions");
        JsonObject playerCondition = conditions.getAsJsonArray("player")
            .get(0)
            .getAsJsonObject();
        assertEquals("minecraft:inverted", playerCondition.get("condition").getAsString());
        assertEquals(
            "minecraft:golden_helmet",
            playerCondition.getAsJsonObject("term")
                .getAsJsonObject("predicate")
                .getAsJsonObject("equipment")
                .getAsJsonObject("head")
                .getAsJsonArray("items")
                .get(0)
                .getAsString()
        );
        JsonObject entityCondition = conditions.getAsJsonArray("entity")
            .get(0)
            .getAsJsonObject();
        assertEquals("minecraft:entity_properties", entityCondition.get("condition").getAsString());
        assertEquals(
            "minecraft:piglin",
            entityCondition.getAsJsonObject("predicate").get("entity_type").getAsString()
        );
    }

    private static JsonObject convert(String json) throws Exception {
        Method convertJson = ExternalPackCompatibility.class.getDeclaredMethod("convertJson", String.class);
        convertJson.setAccessible(true);
        Object conversionResult = convertJson.invoke(null, json);
        Method text = conversionResult.getClass().getDeclaredMethod("text");
        text.setAccessible(true);
        return JsonParser.parseString((String) text.invoke(conversionResult)).getAsJsonObject();
    }

    private static String convertFunction(String function) throws Exception {
        Method convertFunction = ExternalPackCompatibility.class.getDeclaredMethod("convertFunction", String.class);
        convertFunction.setAccessible(true);
        Object conversionResult = convertFunction.invoke(null, function);
        Method text = conversionResult.getClass().getDeclaredMethod("text");
        text.setAccessible(true);
        return (String) text.invoke(conversionResult);
    }

    private static <T> T parseCodec(Codec<T> codec, com.google.gson.JsonElement jsonElement) {
        return codec.parse(RegistryOps.create(JsonOps.INSTANCE, registryAccess), jsonElement)
            .getOrThrow(message -> new IllegalStateException(message));
    }

    private static JsonObject readJson(ZipFile zipFile, String entryName) throws Exception {
        ZipEntry entry = zipFile.getEntry(entryName);
        assertNotNull(entry, entryName);
        try (InputStream input = zipFile.getInputStream(entry)) {
            return JsonParser.parseString(new String(input.readAllBytes())).getAsJsonObject();
        }
    }

    private static boolean isAdmittedPack(Path pack, ExternalPack externalPack) {
        return externalPack.getSha1().equalsIgnoreCase(Utils.calculateSHA1(pack))
            || ExternalPackCompatibility.isCompatibleWorldCopy(pack, externalPack);
    }

    private static void writeMarkerOnlyPack(
        Path pack,
        String markerEntry,
        String version,
        String fileName,
        String sourceSha1,
        String rootOverrideSha1
    ) throws Exception {
        Properties properties = new Properties();
        properties.setProperty("version", version);
        properties.setProperty("fileName", fileName);
        properties.setProperty("sourceSha1", sourceSha1);
        properties.setProperty("rootOverrideSha1", rootOverrideSha1);

        ByteArrayOutputStream markerBytes = new ByteArrayOutputStream();
        properties.store(markerBytes, null);

        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(pack))) {
            output.putNextEntry(new ZipEntry("pack.mcmeta"));
            output.write("{}".getBytes());
            output.closeEntry();

            output.putNextEntry(new ZipEntry(markerEntry));
            output.write(markerBytes.toByteArray());
            output.closeEntry();
        }
    }
}
