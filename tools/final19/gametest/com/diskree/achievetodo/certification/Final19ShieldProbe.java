package com.diskree.achievetodo.certification;
import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import java.util.*;
/** Read-only probes armed for owned actors; native operations are always invoked unchanged. */
public final class Final19ShieldProbe {
 static final Map<UUID,JsonObject>hurt=new HashMap<>();static final Set<UUID>armed=new HashSet<>(),aiShots=new HashSet<>();
 public static void arm(ServerPlayer p){armed.add(p.getUUID());hurt.remove(p.getUUID());}
 public static void aiShot(Entity source){boolean nativeGoal=StackWalker.getInstance().walk(s->s.anyMatch(f->f.getClassName().contains("RangedBowAttackGoal")||f.getClassName().contains("RangedCrossbowAttackGoal")));if(nativeGoal)aiShots.add(source.getUUID());}
 public static void observe(ServerPlayer p,DamageSource source,float dealt,float taken,boolean blocked){if(!armed.contains(p.getUUID()))return;var r=new JsonObject();r.addProperty("nativeHurtObserved",true);r.addProperty("damageType",source.typeHolder().unwrapKey().orElseThrow().identifier().toString());r.addProperty("isProjectile",source.is(DamageTypeTags.IS_PROJECTILE));r.addProperty("blocked",blocked);r.addProperty("dealt",dealt);r.addProperty("taken",taken);Entity owner=source.getEntity(),direct=source.getDirectEntity();r.addProperty("skeletonSource",owner!=null&&owner.getType().builtInRegistryHolder().is(TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:skeletons"))));r.addProperty("sourceUuid",owner==null?"none":owner.getUUID().toString());r.addProperty("directUuid",direct==null?"none":direct.getUUID().toString());r.addProperty("liveProjectileOwnerObserved",direct instanceof Projectile projectile&&projectile.getOwner()==owner&&owner!=null);r.addProperty("nativeAiShotObserved",owner!=null&&aiShots.contains(owner.getUUID()));hurt.put(p.getUUID(),r);}
 public static JsonObject peek(ServerPlayer p){return hurt.get(p.getUUID());}
 public static void close(ServerPlayer p,Entity source){armed.remove(p.getUUID());hurt.remove(p.getUUID());if(source!=null)aiShots.remove(source.getUUID());}
}
