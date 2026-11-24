package com.diskree.achievetodo.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

/**
 * Upgrades datapack JSON written for Minecraft 26.2 to the 26.3 format, for packs that only exist in older formats
 * (the legacy BACAP terrain variants go through {@link ExternalPackCompatibility}'s 1.18 converter, which targets 26.2).
 * <p>
 * 26.3 changes handled here, derived from the 26.3 codecs and from BACAP 1.21 vs 1.21.1:
 * <ul>
 *     <li>loot conditions dispatch on {@code type} instead of {@code condition};</li>
 *     <li>criterion entity fields ({@code player}, {@code entity}, ...) take one condition, not a list, and no longer
 *     accept a bare entity predicate (it becomes {@code entity_properties} on {@code this});</li>
 *     <li>{@code victims} stays a list of conditions, with the same per-element upgrade;</li>
 *     <li>damage-type tag predicates name the tag as a holder set ({@code #namespace:path});</li>
 *     <li>{@code block_state_property} became {@code match_block} with {@code blocks};</li>
 *     <li>criterion fields {@code loot_table} -> {@code loot_tables}, {@code recipe_id} -> {@code recipes};</li>
 *     <li>potion predicates ({@code brewed_potion.potion}, item {@code predicates.potion_contents}) are objects
 *     {@code {"potions": ...}};</li>
 *     <li>the {@code pot_decorations} component is an object of sides instead of a four-item list.</li>
 * </ul>
 * Content changes (for example filled maps marked by {@code map_color}, replaced by dedicated map items in 26.3)
 * cannot be derived from syntax and are not handled.
 * The upgrade is idempotent: 26.3 JSON passes through unchanged.
 */
public final class Bacap263Upgrade {
    /** Criterion fields typed as a single {@code Holder<LootItemCondition>} in 26.3. */
    private static final Set<String> SINGLE_CONDITION_KEYS = Set.of(
        "player", "entity", "villager", "zombie", "source", "projectile", "partner", "parent", "lightning",
        "child", "cause", "bystander"
    );
    /** Criterion fields that were already lists of conditions (no bare entity predicate shorthand). */
    private static final Set<String> CONDITION_ONLY_KEYS = Set.of("location");
    private static final String LIST_KEY = "victims";
    private static final Map<String, String> RENAMED_CRITERION_KEYS = Map.of("loot_table", "loot_tables", "recipe_id", "recipes");

    private Bacap263Upgrade() {
    }

    public static @NotNull JsonElement upgrade(@NotNull JsonElement root) {
        JsonElement result = renameConditionKeys(root);
        if (result.isJsonObject() && result.getAsJsonObject().has("criteria")
            && result.getAsJsonObject().get("criteria").isJsonObject()) {
            for (Map.Entry<String, JsonElement> criterion : result.getAsJsonObject().getAsJsonObject("criteria").entrySet()) {
                if (!criterion.getValue().isJsonObject()) continue;
                JsonObject object = criterion.getValue().getAsJsonObject();
                if (object.has("conditions") && object.get("conditions").isJsonObject()) {
                    upgradeCriterionConditions(object.getAsJsonObject("conditions"));
                }
            }
        }
        upgradeDamageTags(result);
        upgradePotionContents(result);
        upgradePotDecorations(result);
        return result;
    }

    private static final String[] POT_SIDES = {"back", "left", "right", "front"};

    /** {@code pot_decorations: [back, left, right, front]} (brick = plain side) -> {@code {"left": {"id": ...}, ...}}. */
    private static void upgradePotDecorations(@NotNull JsonElement element) {
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) upgradePotDecorations(child);
            return;
        }
        if (!element.isJsonObject()) return;
        JsonObject object = element.getAsJsonObject();
        for (String key : Set.copyOf(object.keySet())) {
            JsonElement value = object.get(key);
            if ((key.equals("minecraft:pot_decorations") || key.equals("pot_decorations")) && value.isJsonArray()
                && value.getAsJsonArray().size() <= POT_SIDES.length) {
                JsonObject sides = new JsonObject();
                JsonArray array = value.getAsJsonArray();
                for (int i = 0; i < array.size(); i++) {
                    String item = array.get(i).getAsString();
                    if (item.equals("minecraft:brick") || item.equals("brick")) continue;
                    JsonObject stack = new JsonObject();
                    stack.addProperty("id", item);
                    sides.add(POT_SIDES[i], stack);
                }
                object.add(key, sides);
            } else {
                upgradePotDecorations(value);
            }
        }
    }

    /** Same upgrade for a criterion {@code conditions} object on its own (tests parse trigger instances directly). */
    public static @NotNull JsonElement upgradeCriterionConditionsFragment(@NotNull JsonElement conditions) {
        JsonElement result = upgrade(conditions);
        if (result.isJsonObject()) upgradeCriterionConditions(result.getAsJsonObject());
        return result;
    }

    private static void upgradeCriterionConditions(@NotNull JsonObject conditions) {
        for (var renamed : RENAMED_CRITERION_KEYS.entrySet()) {
            if (conditions.has(renamed.getKey()) && !conditions.has(renamed.getValue())) {
                conditions.add(renamed.getValue(), conditions.remove(renamed.getKey()));
            }
        }
        if (conditions.has("potion") && conditions.get("potion").isJsonPrimitive()) {
            JsonObject potion = new JsonObject();
            potion.add("potions", conditions.get("potion"));
            conditions.add("potion", potion);
        }
        for (String key : Set.copyOf(conditions.keySet())) {
            JsonElement value = conditions.get(key);
            if (SINGLE_CONDITION_KEYS.contains(key)) {
                conditions.add(key, singleCondition(value, true));
            } else if (CONDITION_ONLY_KEYS.contains(key) && value.isJsonArray()) {
                conditions.add(key, singleCondition(value, false));
            } else if (LIST_KEY.equals(key) && value.isJsonArray()) {
                JsonArray upgraded = new JsonArray();
                for (JsonElement element : value.getAsJsonArray()) upgraded.add(singleCondition(element, true));
                conditions.add(key, upgraded);
            }
        }
    }

    /** One 26.3 condition from a 26.2 context-aware predicate value (list of conditions or bare entity predicate). */
    private static @NotNull JsonElement singleCondition(@NotNull JsonElement value, boolean entityShorthand) {
        if (value.isJsonArray()) {
            JsonArray array = value.getAsJsonArray();
            if (array.size() == 1) return singleCondition(array.get(0), entityShorthand);
            JsonObject allOf = new JsonObject();
            allOf.addProperty("type", "minecraft:all_of");
            JsonArray terms = new JsonArray();
            for (JsonElement element : array) terms.add(singleCondition(element, entityShorthand));
            allOf.add("terms", terms);
            return allOf;
        }
        if (entityShorthand && value.isJsonObject() && !value.getAsJsonObject().has("type")) {
            JsonObject wrapped = new JsonObject();
            wrapped.addProperty("type", "minecraft:entity_properties");
            wrapped.addProperty("entity", "this");
            wrapped.add("predicate", value);
            return wrapped;
        }
        return value;
    }

    /** Every loot condition object: {@code condition} -> {@code type}. Returns a deep copy. */
    private static @NotNull JsonElement renameConditionKeys(@NotNull JsonElement element) {
        if (element.isJsonArray()) {
            JsonArray copy = new JsonArray();
            for (JsonElement child : element.getAsJsonArray()) copy.add(renameConditionKeys(child));
            return copy;
        }
        if (!element.isJsonObject()) return element.deepCopy();
        JsonObject source = element.getAsJsonObject();
        JsonObject copy = new JsonObject();
        boolean condition = source.has("condition") && source.get("condition").isJsonPrimitive()
            && source.get("condition").getAsJsonPrimitive().isString() && !source.has("type");
        String type = condition ? source.get("condition").getAsString()
            : source.has("type") && source.get("type").isJsonPrimitive() ? source.get("type").getAsString() : null;
        boolean blockState = "minecraft:block_state_property".equals(type) || "block_state_property".equals(type);
        for (Map.Entry<String, JsonElement> entry : source.entrySet()) {
            String key = condition && entry.getKey().equals("condition") ? "type" : entry.getKey();
            if (blockState && key.equals("block")) key = "blocks";
            JsonElement value = renameConditionKeys(entry.getValue());
            if (blockState && key.equals("type")) value = new JsonPrimitive("minecraft:match_block");
            copy.add(key, value);
        }
        return copy;
    }

    /** Item sub-predicate {@code "potion_contents": "id"} -> {@code {"potions": "id"}} (inside {@code predicates}). */
    private static void upgradePotionContents(@NotNull JsonElement element) {
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) upgradePotionContents(child);
            return;
        }
        if (!element.isJsonObject()) return;
        JsonObject object = element.getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (entry.getKey().equals("predicates") && entry.getValue().isJsonObject()) {
                JsonObject predicates = entry.getValue().getAsJsonObject();
                JsonElement potion = predicates.get("potion_contents");
                if (potion != null && (potion.isJsonPrimitive() || potion.isJsonArray())) {
                    JsonObject wrapped = new JsonObject();
                    wrapped.add("potions", potion);
                    predicates.add("potion_contents", wrapped);
                }
            }
            upgradePotionContents(entry.getValue());
        }
    }

    /** Damage source tag predicates: {@code {"id":"minecraft:is_fire",...}} -> {@code "#minecraft:is_fire"}. */
    private static void upgradeDamageTags(@NotNull JsonElement element) {
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) upgradeDamageTags(child);
            return;
        }
        if (!element.isJsonObject()) return;
        JsonObject object = element.getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (entry.getKey().equals("tags") && entry.getValue().isJsonArray()) {
                for (JsonElement tag : entry.getValue().getAsJsonArray()) {
                    if (!tag.isJsonObject()) continue;
                    JsonObject tagObject = tag.getAsJsonObject();
                    if (tagObject.has("id") && tagObject.has("expected") && tagObject.get("id").isJsonPrimitive()) {
                        String id = tagObject.get("id").getAsString();
                        if (!id.startsWith("#")) tagObject.add("id", new JsonPrimitive("#" + id));
                    }
                }
            }
            upgradeDamageTags(entry.getValue());
        }
    }
}
