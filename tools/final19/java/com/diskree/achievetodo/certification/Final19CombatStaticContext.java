package com.diskree.achievetodo.certification;

import com.diskree.achievetodo.client.ExternalPackCompatibility;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.*;
import net.minecraft.tags.TagKey;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.ZipFile;

/** Bounded new-stage context. Phase A classifications and fallback allowlist are unchanged. */
public final class Final19CombatStaticContext implements AutoCloseable {
    public static final Path RESOURCES = Path.of("src/test/resources/final19_certification");
    private final Path root;
    private final ZipFile frozen, vanilla;
    private final HolderLookup.Provider provider;
    private final Map<String,JsonObject> tagSources = new TreeMap<>();
    public Final19CombatStaticContext(Path root, String family) throws Exception {
        this.root = root;
        PhaseACertification.bootstrapMinecraft();
        frozen = new ZipFile(root.resolve("reference/phase_a_preservation/files/final/bacap.zip").toFile());
        vanilla = new ZipFile(root.resolve(".gradle-user-home/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-deobf/26.2/minecraft-merged-deobf-26.2.jar").toFile());
        require(sha(Files.readAllBytes(Path.of(frozen.getName()))).equals("8c72314535c5df7b4416bf0f38310371ec8aec537fde1445bdc820a3c9aada70"), "Frozen BACAP drift");
        Set<String> dynamic = switch (family) {
            case "WORLDGEN_HOLDERSET_CONTEXT" -> Set.of("minecraft:worldgen/biome");
            case "ENTITY_VARIANT_COMPONENT_CONTEXT" -> Set.of("minecraft:cat_variant", "minecraft:frog_variant", "minecraft:wolf_variant");
            case "DUAL_ITEM_BLOCK_TAG_CONTEXT" -> Set.of();
            case "INVENTORY_ENCHANTMENT_ITEM_TAG_CONTEXT" -> Set.of("minecraft:enchantment");
            case "MIXED_WORLDGEN_PREDICATE_CONTEXT" -> Set.of("minecraft:worldgen/biome", "minecraft:worldgen/structure", "minecraft:enchantment");
            case "RAIDER_PREDICATE_KEY_MIGRATION", "SKELETON_PROJECTILE_BLOCK_RUNTIME_PROOF" -> Set.of("minecraft:banner_pattern", "minecraft:damage_type");
            default -> throw new IllegalArgumentException("Unimplemented FINAL19 context: " + family);
        };
        var builtins = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).freeze();
        var real = VanillaRegistries.createLookup();
        Map<String,HolderLookup.RegistryLookup<?>> registries = new LinkedHashMap<>();
        builtins.listRegistries().forEach(r -> registries.put(r.key().identifier().toString(), r));
        real.listRegistries().filter(r -> dynamic.contains(r.key().identifier().toString())).forEach(r -> registries.put(r.key().identifier().toString(),r));
        require(registries.keySet().containsAll(dynamic), "Missing typed dynamic registry");
        provider = HolderLookup.Provider.create(registries.values().stream().map(this::tagged));
    }
    public JsonObject catalog(String family) throws Exception {
        var result=JsonParser.parseString(Files.readString(root.resolve(RESOURCES).resolve(family.toLowerCase(Locale.ROOT)+"_catalog.json"))).getAsJsonObject();
        if(family.equals("RAIDER_PREDICATE_KEY_MIGRATION"))repairReviewedRaiderKeys(result);
        return result;
    }
    public JsonObject current(String path, String expectedSha) throws Exception {
        var entry = frozen.getEntry(path); require(entry != null, "Missing frozen source");
        byte[] raw; try(var in = frozen.getInputStream(entry)) { raw = in.readAllBytes(); }
        require(sha(raw).equals(expectedSha), "Frozen source drift");
        return convert(new String(raw,StandardCharsets.UTF_8));
    }
    public static JsonObject convert(String text) throws Exception {
        var m = ExternalPackCompatibility.class.getDeclaredMethod("convertJson", String.class); m.setAccessible(true);
        var result = m.invoke(null,text); var accessor = result.getClass().getDeclaredMethod("text"); accessor.setAccessible(true);
        return JsonParser.parseString((String)accessor.invoke(result)).getAsJsonObject();
    }
    public Advancement parse(JsonObject definition) {
        var decoded = Advancement.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE,provider),definition);
        require(decoded.error().isEmpty(), "Typed context rejected predicate: " + decoded.error());
        return decoded.result().orElseThrow();
    }
    /** A parse alone never certifies: exact audited/frozen predicates AND ordered groups must match. */
    public JsonObject validate(String family) throws Exception {
        JsonObject catalog = catalog(family), result = new JsonObject(); int criteria = 0, groups = 0;
        Map<String,JsonObject> definitions=new HashMap<>();
        for(var source : catalog.getAsJsonArray("sources")) {
            JsonObject s = source.getAsJsonObject();
            var current = current(s.get("sourcePath").getAsString(),s.get("frozenSha256").getAsString());
            validateExact(s,current);
            require(definitions.put(s.get("advancementId").getAsString(),s)==null,"Duplicate catalog source");
            var parsed = parse(current);
            require(parsed.criteria().size() == current.getAsJsonObject("criteria").size(), "Criterion loss");
            criteria += parsed.criteria().size(); groups += s.getAsJsonArray("requirements").size();
        }
        Set<String> covered=new HashSet<>();
        for(var value:catalog.getAsJsonArray("cases")){
            var row=value.getAsJsonObject();String id=row.get("advancementId").getAsString();var source=definitions.get(id);require(source!=null,"Unknown catalog ID");int group=row.get("requirementGroup").getAsInt();
            require(group>=0&&group<source.getAsJsonArray("requirements").size()&&covered.add(id+"#"+group),"Invalid/duplicate catalog group");
            require(row.get("alternatives").equals(source.getAsJsonArray("requirements").get(group)),"Catalog group alternatives changed");
            require(row.getAsJsonArray("alternatives").asList().contains(row.get("criterion")),"Selected criterion outside group");
            require(row.get("expectedCriterion").equals(source.getAsJsonObject("auditedConvertedDefinition").getAsJsonObject("criteria").get(row.get("criterion").getAsString())),"Expected predicate changed");
            require(row.get("retainedHistoricalProof").getAsBoolean()==id.equals("blazeandcave:biomes/the_mighty_jungle"),"Wrong retained proof attribution");
        }
        require(covered.size()==groups,"Catalog deleted requirement groups");
        result.addProperty("family",family); result.addProperty("staticSemantics","EXACT_FROZEN_CURRENT_TYPED_CONTEXT");
        result.addProperty("criteriaCount",criteria); result.addProperty("requirementGroupCount",groups);
        result.add("sourceBackedTags",new Gson().toJsonTree(tagSources)); return result;
    }
    public static void validateExact(JsonObject source, JsonObject candidate) {
        require(candidate.equals(source.getAsJsonObject("auditedConvertedDefinition")), "Definition differs from frozen audited conversion");
        JsonArray groups = candidate.getAsJsonArray("requirements");
        if(groups == null) {groups = new JsonArray(); for(var key : candidate.getAsJsonObject("criteria").keySet()) {var group=new JsonArray();group.add(key);groups.add(group);}}
        require(groups.equals(source.getAsJsonArray("requirements")), "Ordered requirement groups changed");
    }
    private JsonArray sources(String path) throws Exception {
        JsonArray sources = new JsonArray();
        for(var zip:List.of(vanilla,frozen)) {var entry=zip.getEntry(path);if(entry!=null){byte[] bytes;try(var in=zip.getInputStream(entry)){bytes=in.readAllBytes();}JsonObject source=new JsonObject();source.addProperty("source",zip.getName()+"!"+path);source.addProperty("sha256",sha(bytes));source.add("definition",convert(new String(bytes,StandardCharsets.UTF_8)));sources.add(source);}}
        var local=root.resolve("src/main/resources").resolve(path);if(Files.exists(local)){JsonObject source=new JsonObject();source.addProperty("source",root.relativize(local).toString());source.addProperty("sha256",sha(Files.readAllBytes(local)));source.add("definition",convert(Files.readString(local)));sources.add(source);}return sources;
    }
    private <T> List<Holder<T>> resolve(HolderLookup.RegistryLookup<T> parent,TagKey<T> tag,Set<String> visiting) throws Exception {
        String key=parent.key().identifier()+"|"+tag.location();require(visiting.add(key),"Tag cycle "+key);
        var definitions=sources("data/"+tag.location().getNamespace()+"/tags/"+parent.key().identifier().getPath()+"/"+tag.location().getPath()+".json");
        if(definitions.isEmpty()){visiting.remove(key);return null;}LinkedHashMap<String,Holder<T>> members=new LinkedHashMap<>();
        for(var source:definitions){var definition=source.getAsJsonObject().getAsJsonObject("definition");if(definition.has("replace")&&definition.get("replace").getAsBoolean())members.clear();for(var value:definition.getAsJsonArray("values")){boolean required=!value.isJsonObject()||!value.getAsJsonObject().has("required")||value.getAsJsonObject().get("required").getAsBoolean();String id=value.isJsonObject()?value.getAsJsonObject().get("id").getAsString():value.getAsString();if(id.startsWith("#")){var nested=resolve(parent,TagKey.create(tag.registry(),Identifier.parse(id.substring(1))),visiting);if(nested==null){require(!required,"Missing nested tag "+id);}else for(var holder:nested)members.put(holder.unwrapKey().orElseThrow().identifier().toString(),holder);}else{var holder=parent.get(ResourceKey.create(tag.registry(),Identifier.parse(id)));require(!required||holder.isPresent(),"Unknown tag member "+id);holder.ifPresent(h->members.put(id,h));}}}
        visiting.remove(key);JsonObject receipt=new JsonObject();receipt.add("sources",definitions);receipt.add("resolvedMembers",new Gson().toJsonTree(members.keySet()));tagSources.put(key,receipt);return List.copyOf(members.values());
    }
    private <T> HolderLookup.RegistryLookup<T> tagged(HolderLookup.RegistryLookup<T> parent) {
        return new HolderLookup.RegistryLookup.Delegate<>() {
            final Map<TagKey<T>,HolderSet.Named<T>> cache=new HashMap<>();
            public HolderLookup.RegistryLookup<T> parent(){return parent;}
            public Optional<HolderSet.Named<T>> get(TagKey<T> tag){try{if(cache.containsKey(tag))return Optional.of(cache.get(tag));var members=resolve(parent,tag,new HashSet<>());if(members==null)return Optional.empty();var named=HolderSet.emptyNamed(parent,tag);var bind=HolderSet.Named.class.getDeclaredMethod("bind",List.class);bind.setAccessible(true);bind.invoke(named,members);cache.put(tag,named);return Optional.of(named);}catch(Exception e){throw new IllegalStateException("Source-backed tag resolution "+tag,e);}}
        };
    }
    public static String sha(byte[] bytes) throws Exception {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
    public static String inputFingerprint(Path root,String family)throws Exception {
        StringBuilder value=new StringBuilder();
        for(String name:List.of("reference/phase_a_preservation/files/final/bacap.zip","src/main/java/com/diskree/achievetodo/client/ExternalPackCompatibility.java","tools/final19/java/com/diskree/achievetodo/certification/Final19CombatStaticContext.java","src/test/resources/final19_certification/"+family.toLowerCase(Locale.ROOT)+"_catalog.json"))value.append(name).append(':').append(sha(Files.readAllBytes(root.resolve(name)))).append('\n');
        return sha(value.toString().getBytes(StandardCharsets.UTF_8));
    }
    static void repairReviewedRaiderKeys(JsonElement value){if(value.isJsonObject()){var o=value.getAsJsonObject();if(o.has("type_specific/raider")){var r=o.getAsJsonObject("type_specific/raider");if(r.has("isCaptain"))r.add("is_captain",r.remove("isCaptain"));if(r.has("hasRaid"))r.add("has_raid",r.remove("hasRaid"));}for(var e:o.entrySet())repairReviewedRaiderKeys(e.getValue());}else if(value.isJsonArray())for(var v:value.getAsJsonArray())repairReviewedRaiderKeys(v);}
    public static void require(boolean condition,String reason){if(!condition)throw new IllegalStateException(reason);}
    @Override public void close() throws Exception {frozen.close();vanilla.close();}
}
