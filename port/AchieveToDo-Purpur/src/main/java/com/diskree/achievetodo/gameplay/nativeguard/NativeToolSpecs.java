package com.diskree.achievetodo.gameplay.nativeguard;
import java.util.*;
/** Exact descriptors and complete class pins after all retained transforms. */
final class NativeToolSpecs {
    record Spec(String id,String owner,String method,String descriptor,String anchor,int ordinal,String mode,int actor,int subject,String result,int sites) {}
    static final Map<String,List<String>> INPUTS=Map.ofEntries(
        Map.entry("net/minecraft/core/component/BlockTransformer",List.of("ae465e0a79ac3e9c575022c801ec38870d27d3ea4de56a1f0de7286d36d28a24", "cfe4f1babc714b775312937443626fcce981d766b871bc5bd8c640f355a8fe7b")),
        Map.entry("net/minecraft/world/entity/LivingEntity",List.of("a037737c666be3b354b8515d0a3f059f4c19ac8b08a9187658d910bcf5dbf03a", "a037737c666be3b354b8515d0a3f059f4c19ac8b08a9187658d910bcf5dbf03a")),
        Map.entry("net/minecraft/world/entity/animal/golem/CopperGolem",List.of("464ed8773279b4c3e54d60de1d2a71092a86210745e962577c69ef50b5195b1a", "464ed8773279b4c3e54d60de1d2a71092a86210745e962577c69ef50b5195b1a")),
        Map.entry("net/minecraft/world/entity/player/Player",List.of("9b5f00ce42828ae46e4ab409be27f4a4f1fb5553374c74e854d0eafdc13c657b", "1e08b5a14f2a558d0603dcb183b54fb9aca0aa5abfc3e18f4668e69384a522c8")),
        Map.entry("net/minecraft/world/item/Item",List.of("26a6062bf6cd3ec2993a6962f246d426e30a120971b6c633e0c56d43ac9f94b0", "26a6062bf6cd3ec2993a6962f246d426e30a120971b6c633e0c56d43ac9f94b0")),
        Map.entry("net/minecraft/world/item/component/KineticWeapon",List.of("6d78cc25b40cdcb776727fdad8124b1fb340162be46a8c2204152a4a9ebdfcd1", "6d78cc25b40cdcb776727fdad8124b1fb340162be46a8c2204152a4a9ebdfcd1")),
        Map.entry("net/minecraft/world/item/component/PiercingWeapon",List.of("d16ee9c15546ef5710955ea801203875d36d317dcc03f97846f3a5461b11e570", "d16ee9c15546ef5710955ea801203875d36d317dcc03f97846f3a5461b11e570")),
        Map.entry("net/minecraft/world/level/block/WeatheringCopperGolemStatueBlock",List.of("6593a6bb8cb5c3d6d64b6e4c2ac950f25793d46b83dbbb781541678bbff02939", "6593a6bb8cb5c3d6d64b6e4c2ac950f25793d46b83dbbb781541678bbff02939")));
    static final List<Spec> SPECS=List.of(
        new Spec("mining-start-and-completion","net/minecraft/world/entity/player/Player","blockActionRestricted","(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/GameType;)Z","RETURN",-1,"mine",0,-1,"false",4),
        new Spec("melee","net/minecraft/world/entity/player/Player","attack","(Lnet/minecraft/world/entity/Entity;)V","HEAD",-1,"attack",0,-1,"void",1),
        new Spec("stab","net/minecraft/world/entity/player/Player","stabAttack","(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/entity/Entity;FZZZ)Z","HEAD",-1,"slot",0,1,"false",1),
        new Spec("post-pierce","net/minecraft/world/entity/LivingEntity","postPiercingAttack","()V","HEAD",-1,"post",0,-1,"void",1),
        new Spec("pierce","net/minecraft/world/item/component/PiercingWeapon","attack","(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V","HEAD",-1,"slot",1,2,"void",1),
        new Spec("kinetic-contact","net/minecraft/world/item/component/KineticWeapon","damageEntities","(Lnet/minecraft/world/item/ItemStack;ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V","Lnet/minecraft/world/entity/LivingEntity;getLookAngle()Lnet/minecraft/world/phys/Vec3;",-1,"spear",3,1,"void",1),
        new Spec("kinetic-start","net/minecraft/world/item/Item","use","(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;","Lnet/minecraft/world/entity/player/Player;startUsingItem(Lnet/minecraft/world/InteractionHand;)V",1,"spear",2,4,"FAIL",2),
        // 26.3: axe/hoe/shovel block changes are data-driven BlockTransformers (AxeItem, HoeItem, ShovelItem are gone).
        // One boundary after the new state is known and before the Bukkit event, item use and block change.
        new Spec("block-transform","net/minecraft/core/component/BlockTransformer","transformBlock","(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;","Lnet/minecraft/world/item/context/UseOnContext;getPlayer()Lnet/minecraft/world/entity/player/Player;",0,"context",1,-1,"PASS",1),
        new Spec("golem-wax-off-and-deoxidize","net/minecraft/world/entity/animal/golem/CopperGolem","mobInteract","(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;","Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FF)V",-1,"identity",1,3,"FAIL",2),
        new Spec("statue-reanimation","net/minecraft/world/level/block/WeatheringCopperGolemStatueBlock","useItemOn","(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;","Lnet/minecraft/world/level/block/entity/CopperGolemStatueBlockEntity;removeStatue(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/entity/animal/golem/CopperGolem;",-1,"identity",5,1,"FAIL",1));

    public record CombatSite(String owner,String method,String descriptor,String kind,int nativeReturns) {}
    public static final List<CombatSite> COMBAT_SITES=List.of(
        new CombatSite("net/minecraft/world/entity/player/Player","stabAttack","(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/entity/Entity;FZZZ)Z","stab",4),
        new CombatSite("net/minecraft/world/item/component/PiercingWeapon","attack","(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V","piercing",1),
        new CombatSite("net/minecraft/world/item/component/KineticWeapon","damageEntities","(Lnet/minecraft/world/item/ItemStack;ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V","kinetic",1),
        new CombatSite("net/minecraft/world/item/component/PiercingWeapon","canHitEntity","(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;)Z","predicate",1));
    private NativeToolSpecs() {}

}
