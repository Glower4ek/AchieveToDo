package com.diskree.achievetodo.gameplay;
import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.progression.*;
import com.diskree.achievetodo.restriction.PlayerProgressionLookup;
import java.util.*;
import java.lang.reflect.Proxy;
import net.kyori.adventure.text.*;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
/** Criterion-only canonical effects contracts; no real-client/runtime certification. */
public final class GameplayDenialEffectsTest {
 private static int checks;
 private static void check(boolean value){if(!value)throw new AssertionError("effects check "+checks);checks++;}
 private static final class Source implements PlayerProgressionLookup,DemystificationSink {
  PlayerProgressionState state;final List<String> discovery=new ArrayList<>();
  public PlayerProgressionState getProgressionState(UUID actor){return state;}
  public void demystify(UUID actor,AbilityType ability){discovery.add(actor+":"+ability);}
 }
 public static void main(String[] args){
  UUID actor=UUID.randomUUID();var messages=new ArrayList<Component>();GameMode[] mode={GameMode.SURVIVAL};
  Player player=(Player)Proxy.newProxyInstance(Player.class.getClassLoader(),new Class<?>[]{Player.class},(proxy,method,values)->switch(method.getName()){
   case "getUniqueId" -> actor;case "getGameMode" -> mode[0];
   case "sendActionBar" -> {messages.add((Component)values[0]);yield null;}
   case "hashCode" -> System.identityHashCode(proxy);case "equals" -> proxy==values[0];case "toString" -> "CanonicalDenialTestPlayer";
   default -> throw new AssertionError("Unexpected player method "+method);
  });
  var counts=new EnumMap<AbilityType,Integer>(AbilityType.class);for(var ability:AbilityType.values())counts.put(ability,4);
  var requirements=AbilityRequirements.from(counts);var source=new Source();source.state=PlayerProgressionState.create(actor,3,requirements);var effects=new GameplayDenialEffects(source);
  check(!effects.allows(player,AbilityType.JUMP));check(messages.size()==1 && source.discovery.size()==1);
  var message=(TranslatableComponent)messages.getFirst();check(message.key().equals("achievetodo.ability.jump.locked_message"));check(message.color().equals(NamedTextColor.YELLOW));
  var suffix=(TranslatableComponent)message.children().get(1);check(suffix.key().equals("achievetodo.ability.left_to_unlock"));check(suffix.arguments().size()==1);
  effects.report(player,AbilityType.JUMP);check(messages.size()==1);check(source.discovery.size()==2); // Coalesce overlays only; criterion sink remains idempotent authority.
  effects.forget(actor);effects.report(player,AbilityType.JUMP);check(messages.size()==2 && source.discovery.size()==3);
  source.state=PlayerProgressionState.create(actor,4,requirements);check(effects.allows(player,AbilityType.JUMP));check(source.discovery.size()==3);
  source.state=PlayerProgressionState.unavailable(actor,requirements);check(!effects.allows(player,AbilityType.JUMP));check(source.discovery.size()==3);
  source.state=PlayerProgressionState.create(UUID.randomUUID(),3,requirements);effects.report(player,AbilityType.JUMP);check(source.discovery.size()==3);
  source.state=PlayerProgressionState.create(actor,3,requirements);mode[0]=GameMode.CREATIVE;check(effects.allows(player,AbilityType.JUMP));effects.report(player,AbilityType.JUMP);check(source.discovery.size()==3);
  mode[0]=GameMode.SPECTATOR;check(effects.allows(player,AbilityType.JUMP));check(source.discovery.size()==3);
  mode[0]=GameMode.SURVIVAL;counts.put(AbilityType.JUMP,-1);source.state=PlayerProgressionState.create(actor,500,AbilityRequirements.from(counts));effects.forget(actor);effects.report(player,AbilityType.JUMP);
  var permanent=(TranslatableComponent)messages.getLast();check(permanent.color().equals(NamedTextColor.RED));check(((TranslatableComponent)permanent.children().get(1)).key().equals("achievetodo.ability.permanently_locked"));
  effects.clear();source.state=PlayerProgressionState.create(actor,0,requirements);int start=source.discovery.size();check(!effects.allowsBreak(player,-1));check(source.discovery.get(start).endsWith(":"+AbilityType.BREAK_BLOCKS_IN_NEGATIVE_Y));
  check(source.discovery.size()==start+1);effects.clear();
  System.out.println("DENIAL_EFFECTS_PASS checks="+checks+"; runtime=NOT_PROVEN");
 }
}
