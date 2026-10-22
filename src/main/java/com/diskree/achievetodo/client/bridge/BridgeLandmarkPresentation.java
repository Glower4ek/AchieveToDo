package com.diskree.achievetodo.client.bridge;

import com.diskree.achievetodo.ability.*;
import com.diskree.achievetodo.bridge.*;
import com.diskree.achievetodo.client.gui.LockedLandmarkBox;
import net.minecraft.world.phys.AABB;
import java.util.*;

/** Original client ordering and geometry, populated only by an admitted world projection. */
public final class BridgeLandmarkPresentation {
    private static BridgeLandmarks.Snapshot previous;
    private static BridgeWire.Reference reference;
    private static List<LockedLandmarkBox> boxes=List.of();
    public static void clear() { previous=null;reference=null;boxes=List.of(); }
    public static List<LockedLandmarkBox> boxes() {
        var geometry=BridgeClient.landmarks();var projection=BridgeClient.presentation();
        if(geometry==null || projection==null || !geometry.enabled()) { clear();return boxes; }
        var ref=BridgeWire.reference(projection);
        if(geometry==previous && ref.equals(reference))return boxes;
        var result=new ArrayList<LockedLandmarkBox>();
        for(var box:geometry.boxes()) {
            AbilityType ability=AbilityType.findByName(box.ability());
            if(ability==null || ability.getLandmarkType()==null)throw new IllegalArgumentException("landmark catalog");
            String role=projection.state().worlds().entrySet().stream().filter(e->e.getValue().uuid().equals(box.world())).findFirst().orElseThrow().getKey();
            DimensionType dimension=switch(role) {case "overworld"->DimensionType.OVERWORLD;case "nether"->DimensionType.NETHER;case "end"->DimensionType.END;default->throw new IllegalArgumentException("landmark world");};
            result.add(new LockedLandmarkBox(ability.getLandmarkType(),dimension,new AABB(box.minX(),box.minY(),box.minZ(),(double)box.maxX()+1,(double)box.maxY()+1,(double)box.maxZ()+1)));
        }
        // Same comparator as calculateLockedLandmarkBoxes in the canonical Fabric client.
        result.sort((a,b)->{
            int dimension=a.dimensionType().compareTo(b.dimensionType());if(dimension!=0)return dimension;
            int left=projection.state().abilities().get(AbilityType.findByLandmarkType(a.landmarkType()).getName()).threshold();
            int right=projection.state().abilities().get(AbilityType.findByLandmarkType(b.landmarkType()).getName()).threshold();
            if(left==-1)left=Integer.MAX_VALUE;if(right==-1)right=Integer.MAX_VALUE;
            int threshold=Integer.compare(right,left);return threshold!=0?threshold:Integer.compare(b.landmarkType().ordinal(),a.landmarkType().ordinal());
        });
        boxes=List.copyOf(result);previous=geometry;reference=ref;return boxes;
    }
    private BridgeLandmarkPresentation() {}
}
