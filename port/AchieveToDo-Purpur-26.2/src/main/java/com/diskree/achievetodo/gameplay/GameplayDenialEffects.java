package com.diskree.achievetodo.gameplay;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.progression.*;
import com.diskree.achievetodo.restriction.PlayerProgressionLookup;
import java.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

/** Canonical denial presentation/discovery only. Permission queries remain pure. */
public final class GameplayDenialEffects {
    private final PlayerProgressionLookup progression;
    private final DemystificationSink discovery;
    // Presentation coalescing only: bound retained disconnected actors until lifecycle cleanup.
    private final Map<UUID,EnumMap<AbilityType,Long>> reported=new LinkedHashMap<>(16,0.75f,true) {
        @Override protected boolean removeEldestEntry(Map.Entry<UUID,EnumMap<AbilityType,Long>> entry){return size()>1024;}
    };
    public GameplayDenialEffects(PlayerProgressionLookup progression) {
        this.progression=Objects.requireNonNull(progression);
        if(!(progression instanceof DemystificationSink sink))throw new IllegalArgumentException("Canonical discovery sink required");
        discovery=sink;
    }
    public static Component message(AbilityType ability,int required,int count) {
        var prefix=Component.translatable("achievetodo.ability."+ability.getName()+".locked_message").append(Component.text(".\n"));
        return required==-1 ? prefix.append(Component.translatable("achievetodo.ability.permanently_locked")).color(NamedTextColor.RED)
            : prefix.append(Component.translatable("achievetodo.ability.left_to_unlock",Component.text(required-count))).color(NamedTextColor.YELLOW);
    }
    public boolean allows(Player player,AbilityType ability) {
        var state=progression.getProgressionState(player.getUniqueId());
        boolean bypass=player.getGameMode()==GameMode.CREATIVE || player.getGameMode()==GameMode.SPECTATOR;
        boolean allowed=GameplayGate.allows(state,player.getUniqueId(),bypass,ability);
        if(!allowed)report(player,ability);
        return allowed;
    }
    /** Select the first canonical failed mining check; the numeric result is unchanged. */
    public boolean allowsBreak(Player player,int y) {
        return (y>=0 || allows(player,AbilityType.BREAK_BLOCKS_IN_NEGATIVE_Y)) && allows(player,AbilityType.BREAK_BLOCKS);
    }
    /** Call only where the matching Fabric call is effectful, never for a silent query. */
    public void report(Player player,AbilityType ability) {
        if(player.getGameMode()==GameMode.CREATIVE || player.getGameMode()==GameMode.SPECTATOR)return;
        afterDenial(player);
        var state=progression.getProgressionState(player.getUniqueId());
        if(state==null || !state.isReady() || !player.getUniqueId().equals(state.getPlayerId()) || state.getAbilityState(ability).isUnlocked())return;
        discovery.demystify(player.getUniqueId(),ability);
        long now=System.nanoTime();var times=reported.computeIfAbsent(player.getUniqueId(),ignored->new EnumMap<>(AbilityType.class));
        Long previous=times.get(ability);if(previous!=null && now-previous<250_000_000L)return;
        times.put(ability,now);
        player.sendActionBar(message(ability,state.getRequirements().getRequiredCount(ability),state.getObtainedAdvancementCount()));
    }
    /** W5-RED-1/2: suppress the post-result use criterion and undo client-side item prediction. */
    private static void afterDenial(Player player) {
        if(MARK_DENIED!=null){
            try{MARK_DENIED.invoke(null,player.getUniqueId());}
            catch(ReflectiveOperationException|RuntimeException ignored){}
        }
        // The client may already have predicted consumption; the server stack is unchanged, so its
        // remote-slot snapshot sees no delta. Resend the inventory like Paper does for cancelled uses.
        player.updateInventory();
    }
    private static final java.lang.reflect.Method MARK_DENIED=markDenied();
    private static java.lang.reflect.Method markDenied(){
        try{return Class.forName("com.diskree.achievetodo.gameplay.nativeguard.NativeCriterionAgent",false,null)
            .getMethod("markDenied",UUID.class);}
        catch(ReflectiveOperationException|LinkageError unavailable){return null;}
    }
    public void forget(UUID actor){reported.remove(actor);}
    public void clear(){reported.clear();}
}
