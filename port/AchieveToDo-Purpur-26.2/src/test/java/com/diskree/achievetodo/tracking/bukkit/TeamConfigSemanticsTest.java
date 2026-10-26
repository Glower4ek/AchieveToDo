package com.diskree.achievetodo.tracking.bukkit;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.progression.*;
import com.diskree.achievetodo.tracking.*;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;
import java.lang.reflect.Proxy;
import java.nio.file.*;
import java.util.*;
import java.util.function.BiFunction;
import java.util.logging.Logger;

public final class TeamConfigSemanticsTest {
    private static int checks;
    private static void check(boolean value) { checks++; if (!value) throw new AssertionError("S3B check " + checks); }
    private static void fails(Runnable action) { boolean failed=false; try { action.run(); } catch (RuntimeException e) { failed=true; } check(failed); }
    @SuppressWarnings("unchecked") private static <T> T proxy(Class<T> type, BiFunction<String,Object[],Object> action) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(self,m,a) -> switch(m.getName()) {
            case "hashCode" -> System.identityHashCode(self); case "equals" -> self==a[0]; case "toString" -> type.getSimpleName(); default -> action.apply(m.getName(),a);
        });
    }
    public static void main(String[] args) throws Exception {
        ProgressionsTest.run(); ProgressionConfigServiceTest.run(); ChaosProgressionGeneratorTest.run(); AbilityStateResolverTest.run();
        configuration(); teams(); groups(); signedCriteria();
        System.out.println("S3B automated contracts PASS checks="+checks+" plus focused existing progression tests");
    }
    private static void configuration() throws Exception {
        Path root=Files.createTempDirectory("s3b-config");
        var repo=new BukkitProgressionConfigRepository(root);
        var service=new ProgressionConfigService();
        for (String mode : List.of("easy","medium","hard","chaos")) {
            var selection=new WorldProgressionConfig(mode);
            var first=service.resolve(selection,42,repo,true);
            check(first.definition().requirements().asMap().size()==153);
            byte[] bytes=Files.readAllBytes(root.resolve(first.fileName()));
            var reloaded=service.resolve(selection,42,repo,false);
            check(first.definition().requirements().asMap().equals(reloaded.definition().requirements().asMap()));
            check(Arrays.equals(bytes,Files.readAllBytes(root.resolve(first.fileName()))));
        }
        check(service.resolve(new WorldProgressionConfig("medium"),42,repo,false).fileName().equals("normal.toml"));
        var chaos=new WorldProgressionConfig("chaos");
        repo.write("chaos_42.toml",repo.read("chaos_42.toml").orElseThrow().replaceFirst("jump = [-0-9]+","jump = -123"));
        check(service.resolve(chaos,42,repo,false).definition().requirements().getRequiredCount(AbilityType.JUMP)==-123);
        fails(()->service.resolve(chaos,43,repo,false));
        for (String malformed : List.of("version = 0\n[abilities]\njump = 3\n","version = 1\n[other]\njump = 3\n","version = 1\n[abilities]\njump = 1\njump = 2\n","version = 1\n[abilities]\njump = 2\nJUMP = 3\n","version = 1\n[abilities]\njump = true\n")) {
            repo.write("chaos_42.toml",malformed);
            fails(()->service.resolve(chaos,42,repo,true));
            check(repo.read("chaos_42.toml").orElseThrow().equals(malformed));
        }
        repo.write("custom.toml","version = 7\nnote = \"a#b\" # comment\n[abilities]\n\"jump\" = -2\nopen_chest = 999999\nvision = -1\nunknown = 'text#ok'\n");
        var custom=new WorldProgressionConfig("custom");
        String customBytes=repo.read("custom.toml").orElseThrow();
        var values=service.resolve(custom,0,repo,false).definition().requirements();
        check(values.getRequiredCount(AbilityType.JUMP)==-2);
        check(values.getRequiredCount(AbilityType.OPEN_CHEST)==1152);
        check(values.getRequiredCount(AbilityType.SWIM)==0);
        check(values.getRequiredCount(AbilityType.USE_COPPER_TOOLS)==-1 && values.getRequiredCount(AbilityType.EQUIP_COPPER_ARMOR)==-1);
        check(repo.read("custom.toml").orElseThrow().equals(customBytes));
        check(PlayerProgressionState.create(UUID.randomUUID(),-2,values).getAbilityState(AbilityType.JUMP).isUnlocked());
        check(!PlayerProgressionState.create(UUID.randomUUID(),-3,values).getAbilityState(AbilityType.JUMP).isUnlocked());
        for (String malformed : List.of("[abilities]\njump = 2147483648","[abilities]\njump = \"4\"","[abilities]\njump = 1\n'jump' = 2","[abilities]\nbroken","[abilities]\njump = 1\n[abilities]","[abilities]\njump = 1.0","[abilities]\nJUMP = 2\njump = 3")) {
            repo.write("invalid.toml",malformed);
            fails(()->service.resolve(new WorldProgressionConfig("invalid"),0,repo,false));
            check(repo.read("invalid.toml").orElseThrow().equals(malformed));
        }
        fails(()->service.resolve(new WorldProgressionConfig("missing"),0,repo,true));
        fails(()->new WorldProgressionConfig("../normal")); fails(()->repo.read("../normal.toml"));
        fails(()->repo.compareAndWrite("custom.toml",Optional.of("stale"),"oops"));
        check(repo.read("custom.toml").orElseThrow().equals(customBytes));
        // A blocked root is a real filesystem write failure, not a simulated success.
        Path blocked=root.resolve("blocked"); Files.writeString(blocked,"untouched");
        fails(()->service.resolve(new WorldProgressionConfig("chaos"),1,new BukkitProgressionConfigRepository(blocked),true));
        check(Files.readString(blocked).equals("untouched"));
        var failing=new ProgressionConfigRepository() {
            public Optional<String> read(String name) { return Optional.empty(); }
            public void write(String name,String text) { throw new IllegalStateException("Injected publish failure"); }
        };
        fails(()->service.resolve(chaos,0,failing,true));
        // Canonical sampler fixtures independently frozen in Fabric PhaseDChaosSamplerTest.
        for (long seed : new long[]{17,82,90,1603}) {
            String file="chaos_"+seed+".toml";
            String original="version = 1\n# preserve me\n[abilities]\njump = 987\n\n[other]\nnote = 'kept'\n";
            repo.write(file,original);
            var table=service.resolve(chaos,seed,repo,false).definition().requirements();
            check(table.getRequiredCount(AbilityType.JUMP)==987);
            var patched=repo.read(file).orElseThrow();
            check(patched.replaceAll("(?m)^(use_copper_tools|equip_copper_armor) = [-0-9]+\\n", "").equals(original));
            check(table.getRequiredCount(seed==17||seed==82 ? AbilityType.USE_COPPER_TOOLS : AbilityType.EQUIP_COPPER_ARMOR)==(seed==17||seed==90?0:-1));
            service.resolve(chaos,seed,repo,false); check(repo.read(file).orElseThrow().equals(patched));
        }
        String ambiguous="version = 1\n[abilities]\njump = 3\n[other]\n'note' = 'ambiguous for canonical insertion'\n";
        repo.write("chaos_999.toml",ambiguous);
        fails(()->service.resolve(chaos,999,repo,false));check(repo.read("chaos_999.toml").orElseThrow().equals(ambiguous));
        for (int seed=0;seed<32;seed++) {
            var generated=ChaosProgressionGenerator.generateChaosProgression(seed);
            check(generated.size()==153 && generated.get(AbilityType.VISION)==0);
            check(generated.get(AbilityType.OPEN_CRAFTING_TABLE).equals(Progressions.getHardProgression().get(AbilityType.OPEN_CRAFTING_TABLE)) || generated.get(AbilityType.OPEN_CRAFTING_TABLE)==0);
            check(generated.values().stream().allMatch(v->v>=-1 && v<=1000));
        }
        String oracle=System.getenv("ACHIEVETODO_CANONICAL_PRESETS");
        if (oracle==null || oracle.isBlank()) throw new IllegalStateException("Canonical preset source required");
        String source=Files.readString(Path.of(oracle));
        source=source.replace("package com.diskree.achievetodo.ability;","package oracle;\nimport com.diskree.achievetodo.ability.AbilityType;")
            .replace("import org.jetbrains.annotations.NotNull;","").replace("@NotNull ","").replace("class Progressions", "class CanonicalProgressions");
        Path compiled=Files.createTempDirectory("s3b-canonical-oracle");
        Path java=compiled.resolve("CanonicalProgressions.java"); Files.writeString(java,source);
        String chaosSource=Files.readString(Path.of(oracle).resolveSibling("ChaosProgressionGenerator.java"))
            .replace("package com.diskree.achievetodo.ability;","package oracle;\nimport com.diskree.achievetodo.ability.AbilityType;\nimport com.diskree.achievetodo.progression.ProgressionConstants;")
            .replace("import com.diskree.achievetodo.server.Constants;","").replace("import org.jetbrains.annotations.NotNull;","").replace("@NotNull ","")
            .replace("Constants.Progression.","ProgressionConstants.").replace("Progressions.getHardProgression()","CanonicalProgressions.getHardProgression()")
            .replace("ChaosProgressionGenerator","CanonicalChaosProgressionGenerator");
        Path chaosJava=compiled.resolve("CanonicalChaosProgressionGenerator.java");Files.writeString(chaosJava,chaosSource);
        int code=javax.tools.ToolProvider.getSystemJavaCompiler().run(null,null,null,"-classpath",System.getProperty("java.class.path"),"-d",compiled.toString(),java.toString(),chaosJava.toString());
        check(code==0);
        try (var loader=new java.net.URLClassLoader(new java.net.URL[]{compiled.toUri().toURL()},TeamConfigSemanticsTest.class.getClassLoader())) {
        Class<?> canonical=loader.loadClass("oracle.CanonicalProgressions");
        Class<?> canonicalChaos=loader.loadClass("oracle.CanonicalChaosProgressionGenerator");
        for (long seed=0;seed<32;seed++) check(ChaosProgressionGenerator.generateChaosProgression(seed).equals(canonicalChaos.getMethod("generateChaosProgression",long.class).invoke(null,seed)));
        for (var mode : List.of(ProgressionModeType.EASY,ProgressionModeType.NORMAL,ProgressionModeType.HARD)) {
            String method=switch(mode){case EASY->"getEasyProgression";case NORMAL->"getNormalProgression";default->"getHardProgression";};
            var expected=(Map<?,?>)canonical.getMethod(method).invoke(null);
            var actual=service.resolve(new WorldProgressionConfig(mode.getName()),0,repo,false).definition().requirements();
            for (var ability : AbilityType.values()) check(Objects.equals(actual.getRequiredCount(ability),expected.get(ability)));
            check(expected.size()==153);
        }
        }
    }
    private static void teams() {
        Map<String,Map<String,Integer>> scores=new HashMap<>();
        Map<String,Set<String>> members=new HashMap<>(); members.put("red",new HashSet<>(Set.of("Alice","Bob","Offline","Fake","Missing"))); members.put("blue",new HashSet<>());
        String[] objective={"bac_advancements_team"};
        Map<String,Team> teams=new HashMap<>();
        for (String id : members.keySet()) teams.put(id,proxy(Team.class,(n,a)->switch(n){case "getName"->id;case "getEntries"->Set.copyOf(members.get(id));default->null;}));
        Scoreboard board=proxy(Scoreboard.class,(n,a)->switch(n) {
            case "getEntryTeam" -> teams.entrySet().stream().filter(t->members.get(t.getKey()).contains(a[0])).map(Map.Entry::getValue).findFirst().orElse(null);
            case "getObjective" -> {
                String id=a[0] instanceof DisplaySlot ? (a[0]==DisplaySlot.SIDEBAR?objective[0]:null):(String)a[0];
                yield id==null?null:proxy(Objective.class,(on,oa)->switch(on){case "getName"->id;case "getScore"->proxy(Score.class,(sn,sa)->switch(sn){case "isScoreSet"->scores.getOrDefault(id,Map.of()).containsKey(oa[0]);case "getScore"->scores.getOrDefault(id,Map.of()).getOrDefault(oa[0],0);default->null;});default->null;});
            }
            default -> null;
        });
        UUID aid=UUID.randomUUID(),bid=UUID.randomUUID();
        Player alice=proxy(Player.class,(n,a)->n.equals("getName")?"Alice":n.equals("getUniqueId")?aid:null);
        Player bob=proxy(Player.class,(n,a)->n.equals("getName")?"Bob":n.equals("getUniqueId")?bid:null);
        for (String mode : List.of("bac_advancements_team","bac_advfirst_team_sum","bac_advfirst_sum","bac_advfirst_team")) {
            objective[0]=mode;
            var values=new HashMap<>(Map.of("Alice",3,"Bob",5,"Offline",7,"Fake",11,"Nonmember",9999)); scores.put(mode,values);
            members.get("red").addAll(Set.of("Alice","Bob","Offline","Fake","Missing"));members.get("blue").clear();
            var source=new BukkitBacapProgressionSource(()->board,Logger.getAnonymousLogger(),(id,type)->0);
            source.refreshDetectedObjective(); source.refreshCount(alice); source.refreshCount(bob);
            check(source.getObtainedAdvancementCount(aid)==26 && source.getObtainedAdvancementCount(bid)==26);
            // S4M consumes this same team/offline resolver, including count decreases and signed values.
            var originalScores = new HashMap<>(values);
            var clientRequirements = com.diskree.achievetodo.progression.AbilityRequirements.normalize(Map.of(
                AbilityType.VISION, 2, AbilityType.OPEN_INVENTORY, 3, AbilityType.USE_SPYGLASS, 4));
            for (int contribution : new int[]{1, 2, 3, 4, 2, -3}) {
                values.clear(); values.put("Alice", 0); values.put("Offline", contribution);
                source.refreshCount(alice);
                int count = source.getObtainedAdvancementCount(aid); check(count == contribution);
                var state = com.diskree.achievetodo.progression.PlayerProgressionState.create(aid, count, clientRequirements);
                for (var ability : com.diskree.achievetodo.gameplay.GameplayGate.CLIENT_DEPENDENT)
                    check(com.diskree.achievetodo.gameplay.GameplayGate.allows(state, aid, false, ability)
                        == (contribution >= clientRequirements.getRequiredCount(ability)));
            }
            values.clear(); values.putAll(originalScores); source.refreshCount(alice); source.refreshCount(bob);
            members.get("red").remove("Fake"); source.refreshCount(alice); check(source.getObtainedAdvancementCount(aid)==15);
            values.remove("Offline");source.refreshCount(alice);check(source.getObtainedAdvancementCount(aid)==8);
            members.get("red").remove("Bob");members.get("blue").add("Bob");source.refreshCount(alice);source.refreshCount(bob);
            check(source.getObtainedAdvancementCount(aid)==3 && source.getObtainedAdvancementCount(bid)==5);
            values.put("Bob",-4);source.refreshCount(bob);check(source.getObtainedAdvancementCount(bid)==-4);
            members.get("blue").clear();source.refreshCount(bob);check(source.getObtainedAdvancementCount(bid)==0);
            source.removePlayer(aid);source.refreshCount(alice);check(source.getObtainedAdvancementCount(aid)==3);
            source.clear();source.refreshDetectedObjective();source.refreshCount(alice);check(source.getObtainedAdvancementCount(aid)==3);
        }
    }
    private static void groups() throws Exception {
        UUID main=UUID.randomUUID(),nether=UUID.randomUUID(),end=UUID.randomUUID(),config=UUID.randomUUID();
        var group=new ProgressionWorldGroup("game",config,Map.of("overworld",new ProgressionWorldIdentity(main,"minecraft:overworld"),"nether",new ProgressionWorldIdentity(nether,"minecraft:the_nether"),"end",new ProgressionWorldIdentity(end,"minecraft:the_end")));
        check(group.contains(main,"minecraft:overworld") && group.contains(nether,"minecraft:the_nether") && group.contains(end,"minecraft:the_end"));
        check(!group.contains(main,"minecraft:the_nether") && !group.contains(UUID.randomUUID(),"minecraft:overworld"));
        fails(()->new ProgressionWorldGroup("game",config,Map.of("overworld",new ProgressionWorldIdentity(main,"minecraft:overworld"),"nether",new ProgressionWorldIdentity(main,"minecraft:the_nether"))));
        fails(()->new ProgressionWorldGroup("game",config,Map.of("nether",new ProgressionWorldIdentity(nether,"minecraft:the_nether"))));
        Path path=Files.createTempDirectory("s3b-group").resolve("group.properties");
        String text="formatVersion=1\ngroupId=game\nconfigId="+config+"\noverworld.uuid="+main+"\noverworld.key=minecraft:overworld\nnether.uuid="+nether+"\nnether.key=minecraft:the_nether\nend.uuid="+end+"\nend.key=minecraft:the_end\n";
        Files.writeString(path,text);check(ProgressionWorldGroup.load(path).equals(group));
        Files.writeString(path,text+"groupId=other\n");fails(()->ProgressionWorldGroup.load(path));
        Files.writeString(path,text+"secondGroup=unsupported\n");fails(()->ProgressionWorldGroup.load(path));
        org.bukkit.World world=proxy(org.bukkit.World.class,(n,a)->switch(n){case "getUID"->main;case "getKey"->org.bukkit.NamespacedKey.minecraft("overworld");case "getName"->"same-display-name";default->null;});
        Path selection=path.resolveSibling("selection.properties");
        var store=new BukkitWorldProgressionConfigStore(selection,world,group);
        store.save(new WorldProgressionConfig("normal"));
        byte[] saved=Files.readAllBytes(selection);
        check(store.load().orElseThrow().configName().equals("normal"));
        var foreign=new ProgressionWorldGroup("other",UUID.randomUUID(),group.worlds());
        fails(()->new BukkitWorldProgressionConfigStore(selection,world,foreign).load());
        check(Arrays.equals(saved,Files.readAllBytes(selection)));
        Files.writeString(selection,new String(saved,java.nio.charset.StandardCharsets.ISO_8859_1)+"configName=hard\n",java.nio.charset.StandardCharsets.ISO_8859_1);
        fails(store::load);
    }
    private static void signedCriteria() {
        Set<String> awarded=new HashSet<>();int[] completions={0},writes={0};
        var key=new org.bukkit.NamespacedKey("achievetodo","abilities/jump");
        var advancement=proxy(org.bukkit.advancement.Advancement.class,(n,a)->n.equals("getKey")?key:null);
        var progress=proxy(org.bukkit.advancement.AdvancementProgress.class,(n,a)->switch(n) {
            case "getAwardedCriteria"->Set.copyOf(awarded);
            case "getRemainingCriteria"->{var r=new HashSet<>(Set.of("demystified","unlocked"));r.removeAll(awarded);yield r;}
            case "awardCriteria"->{boolean changed=awarded.add((String)a[0]);if(changed){writes[0]++;if(awarded.size()==2)completions[0]++;}yield changed;}
            case "revokeCriteria"->{boolean changed=awarded.remove(a[0]);if(changed)writes[0]++;yield changed;}
            default->null;
        });
        Player player=proxy(Player.class,(n,a)->n.equals("getAdvancementProgress")?progress:null);
        var sync=new BukkitSyntheticAdvancementSynchronizer(k->k.equals(key)?advancement:null);
        UUID id=UUID.randomUUID();var requirements=AbilityRequirements.normalize(Map.of(AbilityType.JUMP,-2));
        var locked=PlayerProgressionState.create(id,-3,requirements);
        var unlocked=locked.withObtainedAdvancementCount(-2);
        sync.fullSync(player,locked);check(awarded.isEmpty());
        sync.applyUpdate(player,ProgressionStateSynchronizer.diff(locked,unlocked));check(completions[0]==1 && awarded.size()==2);
        int oldWrites=writes[0];sync.applyUpdate(player,ProgressionStateSynchronizer.diff(unlocked,unlocked));check(writes[0]==oldWrites);
        sync.applyUpdate(player,ProgressionStateSynchronizer.diff(unlocked,locked));check(!awarded.contains("unlocked"));
        sync.applyUpdate(player,ProgressionStateSynchronizer.diff(locked,unlocked));check(completions[0]==2);
        var minimum=PlayerProgressionState.create(id,0,AbilityRequirements.normalize(Map.of(AbilityType.JUMP,Integer.MIN_VALUE)));
        check(minimum.getAbilityState(AbilityType.JUMP).isUnlocked());sync.fullSync(player,minimum);check(!awarded.contains("unlocked") && completions[0]==2);
    }
}
