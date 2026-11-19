package com.diskree.achievetodo.gameplay;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.progression.*;
import java.util.*;

/** Authority coverage for the native transaction family; actual slot mutations are certified separately. */
public final class NativeTransactionGameplayTest {
    public static void main(String[] args) {
        int checks = 0;
        var abilities = NativeEquipmentAbilities.abilities();
        if (abilities.size() != 12 || NativeEquipmentAbilities.ITEMS.values().stream().mapToInt(List::size).sum() != 30)
            throw new AssertionError("Canonical equipment identity envelope");
        UUID actor = UUID.randomUUID();
        var files = new HashMap<String, String>();
        StringBuilder custom = new StringBuilder("version = 1\n[abilities]\n"); int index = 0;
        for (var ability : abilities.stream().sorted().toList())
            custom.append(ability.name().toLowerCase(Locale.ROOT)).append(" = ").append(new int[]{-2,-1,0,7}[index++ % 4]).append('\n');
        files.put("custom.toml", custom.toString());
        var repository = new ProgressionConfigRepository() {
            public Optional<String> read(String name) { return Optional.ofNullable(files.get(name)); }
            public void write(String name, String value) { files.put(name, value); }
        };
        for (String config : List.of("easy", "medium", "hard", "custom", "chaos")) {
            var requirements = new ProgressionConfigService().resolve(new WorldProgressionConfig(config),777,repository,true).definition().requirements();
            for (var ability : abilities) {
                int threshold = requirements.getRequiredCount(ability);
                for (int count : new int[]{-3,-2,-1,0,threshold-1,threshold,threshold+1,threshold-1}) {
                    var state = PlayerProgressionState.create(actor,count,requirements);
                    if (GameplayGate.allows(state,actor,false,ability) != (threshold==0 || threshold!=-1 && count>=threshold))
                        throw new AssertionError(config+":"+ability+":"+count);
                    checks++;
                }
                var state = PlayerProgressionState.create(actor,10000,requirements);
                if (GameplayGate.allows(null,actor,false,ability)
                    || GameplayGate.allows(state.withReady(false),actor,false,ability)
                    || GameplayGate.allows(state,UUID.randomUUID(),false,ability)
                    || !GameplayGate.allows(null,actor,true,ability)) throw new AssertionError("Authority isolation: "+ability);
                checks+=4;
            }
        }
        // Every addition independently exercises CUSTOM's signed/permanent/initial/positive meanings.
        var additions = EnumSet.copyOf(abilities);
        additions.addAll(List.of(AbilityType.EAT_SWEET_BERRIES, AbilityType.EAT_GLOW_BERRIES,
            AbilityType.EAT_CARROT, AbilityType.EAT_POTATO));
        for (var ability : additions) for (int threshold : new int[]{-2,-1,0,7}) {
            var requirements = AbilityRequirements.normalize(Map.of(ability,threshold));
            for (int count : new int[]{-3,-2,-1,0,threshold-1,threshold,threshold+1,threshold-1}) {
                var state = PlayerProgressionState.create(actor,count,requirements);
                if (GameplayGate.allows(state,actor,false,ability) != (threshold==0 || threshold!=-1 && count>=threshold))
                    throw new AssertionError("Signed custom: "+ability+":"+threshold+":"+count);
                checks++;
            }
        }
        System.out.println("S4F_NATIVE_TRANSACTION_AUTHORITY_PASS checks="+checks+" ids="+abilities.size()+" nativeRuntime=SEPARATE");
    }
    private NativeTransactionGameplayTest() {}
}
