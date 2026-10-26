package com.diskree.achievetodo.bridge;

import java.io.*;
import java.util.*;

/** World metadata only. Numeric permission always comes from the admitted S3C projection. */
public final class BridgeLandmarks {
    public static final int PAGE_SIZE=128, MAX_PAGES=8192;
    public record Box(String ability, UUID world, int minX,int minY,int minZ,int maxX,int maxY,int maxZ) {
        public Box {
            if(ability==null || !ability.matches("interact_inside_[a-z_]+") || world==null ||
                minX>maxX || minY>maxY || minZ>maxZ) throw new IllegalArgumentException("landmark box");
        }
    }
    public record Snapshot(boolean enabled,List<Box> boxes) {
        public Snapshot { boxes=List.copyOf(boxes);if(!enabled && !boxes.isEmpty())throw new IllegalArgumentException("disabled geometry"); }
        public static Snapshot disabled() { return new Snapshot(false,List.of()); }
    }
    public record Page(BridgeWire.Reference reference,long sequence,boolean enabled,int index,int total,List<Box> boxes) {
        public Page {
            Objects.requireNonNull(reference);boxes=List.copyOf(boxes);
            if(sequence<0 || total<1 || total>MAX_PAGES || index<0 || index>=total || boxes.size()>PAGE_SIZE ||
                (!enabled && (total!=1 || !boxes.isEmpty()))) throw new IllegalArgumentException("landmark page");
        }
    }
    public static List<Page> pages(BridgeWire.Reference ref,long sequence,Snapshot snapshot) {
        int total=Math.max(1,(snapshot.boxes().size()+PAGE_SIZE-1)/PAGE_SIZE);
        if(total>MAX_PAGES)throw new IllegalArgumentException("landmark snapshot bound");
        var result=new ArrayList<Page>();
        for(int n=0;n<total;n++)result.add(new Page(ref,sequence,snapshot.enabled(),n,total,
            snapshot.boxes().subList(n*PAGE_SIZE,Math.min((n+1)*PAGE_SIZE,snapshot.boxes().size()))));
        return List.copyOf(result);
    }
    public static byte[] encode(Page page) {
        try {
            var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);
            byte[] ref=BridgeWire.reference(page.reference());out.writeShort(ref.length);out.write(ref);
            out.writeLong(page.sequence());out.writeBoolean(page.enabled());out.writeShort(page.index());out.writeShort(page.total());out.writeShort(page.boxes().size());
            for(Box b:page.boxes()) {
                out.writeUTF(b.ability());out.writeLong(b.world().getMostSignificantBits());out.writeLong(b.world().getLeastSignificantBits());
                out.writeInt(b.minX());out.writeInt(b.minY());out.writeInt(b.minZ());out.writeInt(b.maxX());out.writeInt(b.maxY());out.writeInt(b.maxZ());
            }
            if(bytes.size()>BridgeWire.MAX_BODY)throw new IllegalArgumentException("landmark body bound");
            return bytes.toByteArray();
        } catch(IOException failure) { throw new IllegalStateException(failure); }
    }
    public static Page decode(byte[] bytes) {
        if(bytes.length>BridgeWire.MAX_BODY)throw new IllegalArgumentException("landmark body bound");
        try {
            var in=new DataInputStream(new ByteArrayInputStream(bytes));int length=in.readUnsignedShort();
            if(length>256 || length>in.available())throw new IllegalArgumentException("landmark reference");
            var ref=BridgeWire.reference(in.readNBytes(length));long sequence=in.readLong();int flag=in.readUnsignedByte();
            if(flag>1)throw new IllegalArgumentException("landmark enabled");
            int index=in.readUnsignedShort(),total=in.readUnsignedShort(),size=in.readUnsignedShort();
            if(size>PAGE_SIZE)throw new IllegalArgumentException("landmark page size");
            var boxes=new ArrayList<Box>();
            for(int n=0;n<size;n++) {
                // Read the bounded ability bytes before allocating/decoding a modified UTF string.
                int count=in.readUnsignedShort();if(count>64 || count>in.available())throw new IllegalArgumentException("landmark ability bound");
                byte[] text=in.readNBytes(count);for(byte c:text)if(c<0 || c>127)throw new IllegalArgumentException("landmark ability encoding");
                String ability=new String(text,java.nio.charset.StandardCharsets.US_ASCII);
                boxes.add(new Box(ability,new UUID(in.readLong(),in.readLong()),in.readInt(),in.readInt(),in.readInt(),in.readInt(),in.readInt(),in.readInt()));
            }
            if(in.available()!=0)throw new IllegalArgumentException("landmark trailing bytes");
            return new Page(ref,sequence,flag==1,index,total,boxes);
        } catch(IOException failure) { throw new IllegalArgumentException("invalid landmark page",failure); }
    }
    /** One atomic replacement, scoped to an exact admitted numeric reference and physical session. */
    public static final class Cache {
        private BridgeWire.Reference reference;
        private long sequence=-1;
        private int next,total;
        private boolean enabled;
        private ArrayList<Box> staging;
        private Snapshot current;
        public void clear() { reference=null;sequence=-1;next=total=0;staging=null;current=null; }
        public Snapshot current(BridgeWire.Projection projection) {
            return projection!=null && BridgeWire.reference(projection).equals(reference) ? current : null;
        }
        public boolean accept(Page page,BridgeWire.Projection admitted) {
            if(admitted==null || !BridgeWire.reference(admitted).equals(page.reference()))return false;
            if(!page.reference().equals(reference))clear();
            if(page.sequence()<sequence)return false;
            if(page.sequence()>sequence) {
                if(page.index()!=0)throw new IllegalArgumentException("landmark initial page");
                reference=page.reference();sequence=page.sequence();next=0;total=page.total();enabled=page.enabled();staging=new ArrayList<>();current=null;
            }
            if(page.index()<next)return false; // Idempotent replay cannot publish twice.
            if(staging==null || page.index()!=next || total!=page.total() || enabled!=page.enabled())throw new IllegalArgumentException("landmark page order");
            for(Box box:page.boxes()) {
                if(!admitted.state().abilities().containsKey(box.ability()) || admitted.state().worlds().values().stream().noneMatch(w->w.uuid().equals(box.world())))
                    throw new IllegalArgumentException("landmark scope");
                staging.add(box);
            }
            next++;
            if(next!=total)return false;
            current=new Snapshot(enabled,staging);staging=null;return true;
        }
    }
    private BridgeLandmarks() {}
}
