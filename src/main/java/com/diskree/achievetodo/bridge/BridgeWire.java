package com.diskree.achievetodo.bridge;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.security.*;
import java.util.*;

/** Bounded1.3 authoritative numeric presentation. Product gameplay readiness remains false. */
public final class BridgeWire {
    public static final String CHANNEL="achievetodo:bridge";
    public static final String CATALOG="762de1ac54aa980415c9813d946a8c4836993072531c483b4cf310c01015c5d2";
    public static final int MAX_FRAME=24*1024, MAX_BODY=20*1024, MAJOR=1, MINOR=4;
    public static final long FEATURES=59;
    public static final long TRANSITION_BUDGET_NANOS=20_000_000_000L;
    public static final int HELLO=1, ACK=2, SNAPSHOT=3, READY=4, ADMIT=5, DELTA=6, RESYNC=7, SUSPEND=8, LANDMARKS=9;
    private static final int MAGIC=0x41544431;
    public record Frame(int kind,int major,int minor,UUID server,UUID session,UUID challenge,
                        long features,String catalog,String build,byte[] body) {
        public Frame { body=body.clone(); if (body.length>MAX_BODY) throw new IllegalArgumentException("body bound"); }
        public Frame(int kind,int major,int minor,UUID server,UUID session,UUID challenge,long features,String catalog,String build) {
            this(kind,major,minor,server,session,challenge,features,catalog,build,new byte[0]);
        }
        @Override public byte[] body() { return body.clone(); }
        public Frame with(int tag,byte[] data) { return new Frame(tag,major,minor,server,session,challenge,features,catalog,build,data); }
    }
    public record Ability(int threshold,boolean unlocked) {}
    public record Binding(UUID uuid,String key) {
        public Binding { nonzero(uuid);if (key==null || !key.matches("[a-z0-9_.-]+:[a-z0-9/._-]+")) throw new IllegalArgumentException("world key"); }
    }
    public record State(UUID world,String worldKey,UUID config,String group,long generation,String difficulty,String selection,
                        String objective,int count,Map<String,Binding> worlds,Map<String,Ability> abilities) {
        public State {
            nonzero(world);nonzero(config);
            if (group==null || !group.matches("[A-Za-z0-9_-]+") || generation<0 || selection==null || !selection.matches("[A-Za-z0-9_-]+") ||
                !Set.of("EASY","NORMAL","HARD","CHAOS","CUSTOM").contains(difficulty) ||
                !Set.of("bac_advancements","bac_advfirst","bac_advancements_team","bac_advfirst_team_sum","bac_advfirst_sum","bac_advfirst_team").contains(objective)) throw new IllegalArgumentException("state schema");
            worlds=Collections.unmodifiableMap(new TreeMap<>(worlds));abilities=Collections.unmodifiableMap(new TreeMap<>(abilities));
            if (worlds.size()>3 || !worlds.containsKey("overworld") || !Set.of("overworld","nether","end").containsAll(worlds.keySet()) ||
                new HashSet<>(worlds.values().stream().map(Binding::uuid).toList()).size()!=worlds.size() ||
                new HashSet<>(worlds.values().stream().map(Binding::key).toList()).size()!=worlds.size() ||
                !worlds.containsValue(new Binding(world,worldKey))) throw new IllegalArgumentException("world binding");
            if (abilities.size()!=153 || abilities.values().stream().anyMatch(Objects::isNull) ||
                !sha(String.join("\n",abilities.keySet()).concat("\n").getBytes(StandardCharsets.UTF_8)).equals(CATALOG) ||
                abilities.values().stream().anyMatch(v->v.threshold()>1152)) throw new IllegalArgumentException("ability catalog/threshold");
        }
        public State at(UUID id,String key) { return new State(id,key,config,group,generation,difficulty,selection,objective,count,worlds,abilities); }
    }
    public record Projection(UUID epoch,long revision,UUID snapshot,State state) {
        public Projection { nonzero(epoch);nonzero(snapshot);Objects.requireNonNull(state);if(revision<0)throw new IllegalArgumentException("revision"); }
        public Projection change(long next,State value) { return new Projection(epoch,next,snapshot,value); }
        public UUID world() { return state.world(); }
        public String worldKey() { return state.worldKey(); }
        public UUID config() { return state.config(); }
        public String group() { return state.group(); }
    }
    private static void nonzero(UUID id) { if(id==null || id.equals(new UUID(0,0)))throw new IllegalArgumentException("identity"); }
    public record Ticket(UUID request,Reference reference) {}
    public record ResyncSnapshot(UUID request,Projection projection) {}
    public record Reference(UUID epoch,long revision,UUID snapshot,String digest) {}
    public record Patch(UUID epoch,long base,long next,State value,String digest) {
        public Patch { if (base<0 || base==Long.MAX_VALUE || next!=base+1) throw new IllegalArgumentException("delta revision"); }
    }
    @FunctionalInterface private interface Writer { void write(DataOutputStream out) throws IOException; }
    @FunctionalInterface private interface Reader<T> { T read(DataInputStream in) throws IOException; }
    private static byte[] write(Writer writer) {
        try { var bytes=new ByteArrayOutputStream();writer.write(new DataOutputStream(bytes));return bytes.toByteArray(); }
        catch (IOException e) { throw new IllegalStateException(e); }
    }
    private static <T> T read(byte[] bytes,int bound,Reader<T> reader) {
        if (bytes.length>bound) throw new IllegalArgumentException("payload bound");
        try { var in=new DataInputStream(new ByteArrayInputStream(bytes));T value=reader.read(in);
            if (in.available()!=0) throw new IllegalArgumentException("trailing bytes");return value;
        } catch (IOException e) { throw new IllegalArgumentException("invalid/truncated payload",e); }
    }
    public static byte[] encode(Frame f) {
        byte[] result=write(out -> {
            out.writeInt(MAGIC);out.writeShort(f.major());out.writeShort(f.minor());out.writeByte(f.kind());
            uuid(out,f.server());uuid(out,f.session());uuid(out,f.challenge());out.writeLong(f.features());
            string(out,f.catalog());string(out,f.build());out.writeInt(f.body.length);out.write(f.body);
        });
        if (result.length>MAX_FRAME) throw new IllegalArgumentException("frame bound");return result;
    }
    public static Frame decode(byte[] data) {
        return read(data,MAX_FRAME,in -> {
            if (in.readInt()!=MAGIC) throw new IllegalArgumentException("magic");
            int major=in.readUnsignedShort(),minor=in.readUnsignedShort(),kind=in.readUnsignedByte();
            if (kind<HELLO || kind>LANDMARKS) throw new IllegalArgumentException("kind");
            UUID server=uuid(in),session=uuid(in),challenge=uuid(in);long features=in.readLong();
            String catalog=string(in),build=string(in);int size=in.readInt();
            if (size<0 || size>MAX_BODY || size>in.available()) throw new IllegalArgumentException("body bound");
            return new Frame(kind,major,minor,server,session,challenge,features,catalog,build,in.readNBytes(size));
        });
    }
    private static void state(DataOutputStream out,State p) throws IOException {
        uuid(out,p.world());string(out,p.worldKey());uuid(out,p.config());string(out,p.group());out.writeLong(p.generation());
        string(out,p.difficulty());string(out,p.selection());string(out,p.objective());out.writeInt(p.count());
        out.writeByte(p.worlds().size());for(var entry:p.worlds().entrySet()){string(out,entry.getKey());uuid(out,entry.getValue().uuid());string(out,entry.getValue().key());}
        out.writeShort(p.abilities().size());for(var entry:p.abilities().entrySet()){string(out,entry.getKey());out.writeInt(entry.getValue().threshold());out.writeBoolean(entry.getValue().unlocked());}
    }
    private static State state(DataInputStream in) throws IOException {
        UUID world=uuid(in);String key=string(in);UUID config=uuid(in);String group=string(in);long generation=in.readLong();
        String difficulty=string(in),selection=string(in),objective=string(in);int count=in.readInt();
        int size=in.readUnsignedByte();if(size<1 || size>3)throw new IllegalArgumentException("world count");
        Map<String,Binding> worlds=new TreeMap<>();for(int i=0;i<size;i++)if(worlds.putIfAbsent(string(in),new Binding(uuid(in),string(in)))!=null)throw new IllegalArgumentException("duplicate world role");
        size=in.readUnsignedShort();if(size!=153)throw new IllegalArgumentException("ability count");
        Map<String,Ability> abilities=new TreeMap<>();for(int i=0;i<size;i++)if(abilities.putIfAbsent(string(in),new Ability(in.readInt(),bool(in)))!=null)throw new IllegalArgumentException("duplicate ability");
        return new State(world,key,config,group,generation,difficulty,selection,objective,count,worlds,abilities);
    }
    public static byte[] projection(Projection p) {
        return write(out -> {out.writeByte(3);uuid(out,p.epoch());out.writeLong(p.revision());uuid(out,p.snapshot());state(out,p.state());});
    }
    public static Projection projection(byte[] bytes) {
        return read(bytes,MAX_BODY,in -> {if(in.readUnsignedByte()!=3)throw new IllegalArgumentException("numeric preview scope; product ready forbidden");return new Projection(uuid(in),in.readLong(),uuid(in),state(in));});
    }
    public static byte[] snapshot(Projection p) {
        return write(out -> { byte[] data=projection(p);out.writeShort(data.length);out.write(data);string(out,digest(p)); });
    }
    public static Projection snapshot(byte[] bytes) {
        return read(bytes,MAX_BODY,in -> {
            int length=in.readUnsignedShort();
            if (length>MAX_BODY || length>in.available()) throw new IllegalArgumentException("snapshot length");
            Projection result=projection(in.readNBytes(length));
            if (!digest(result).equals(string(in))) throw new IllegalArgumentException("snapshot digest");
            return result;
        });
    }
    public static String digest(Projection p) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(projection(p))); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
    public static Reference reference(Projection p) { return new Reference(p.epoch(),p.revision(),p.snapshot(),digest(p)); }
    public static byte[] reference(Reference r) {
        return write(out -> { uuid(out,r.epoch());out.writeLong(r.revision());uuid(out,r.snapshot());string(out,r.digest()); });
    }
    public static Reference reference(byte[] bytes) {
        return read(bytes,MAX_BODY,in -> new Reference(uuid(in),in.readLong(),uuid(in),string(in)));
    }
    public static byte[] ticket(Ticket t) {
        return write(out -> { uuid(out,t.request());out.write(reference(t.reference())); });
    }
    public static Ticket ticket(byte[] bytes) {
        return read(bytes,MAX_BODY,in -> new Ticket(uuid(in),reference(in.readAllBytes())));
    }
    public static byte[] resyncSnapshot(ResyncSnapshot s) {
        return write(out -> { uuid(out,s.request());out.write(snapshot(s.projection())); });
    }
    public static ResyncSnapshot resyncSnapshot(byte[] bytes) {
        return read(bytes,MAX_BODY,in -> new ResyncSnapshot(uuid(in),snapshot(in.readAllBytes())));
    }
    public record Suspension(Ticket control,ResyncSnapshot transition) {}
    public static byte[] suspend(Ticket control) {return write(out->{out.writeByte(0);out.write(ticket(control));});}
    public static byte[] suspend(ResyncSnapshot transition) {return write(out->{out.writeByte(1);out.write(resyncSnapshot(transition));});}
    public static Suspension suspension(byte[] bytes) {
        return read(bytes,MAX_BODY,in->{int tag=in.readUnsignedByte();if(tag==0)return new Suspension(ticket(in.readAllBytes()),null);if(tag==1)return new Suspension(null,resyncSnapshot(in.readAllBytes()));throw new IllegalArgumentException("suspension schema");});
    }
    public static boolean sameGroup(State a,State b) {
        return a.config().equals(b.config()) && a.group().equals(b.group()) && a.worlds().equals(b.worlds()) && b.generation()>=a.generation();
    }
    public static boolean sameScope(Projection a,Projection b) {
        return a.epoch().equals(b.epoch()) && a.world().equals(b.world()) && a.worldKey().equals(b.worldKey()) && sameGroup(a.state(),b.state());
    }
    public static byte[] patch(Patch p) {
        return write(out -> {uuid(out,p.epoch());out.writeLong(p.base());out.writeLong(p.next());state(out,p.value());string(out,p.digest());});
    }
    public static Patch patch(byte[] bytes) {
        return read(bytes,MAX_BODY,in -> new Patch(uuid(in),in.readLong(),in.readLong(),state(in),string(in)));
    }
    public static boolean sameContext(Frame a,Frame b) {
        return a.server().equals(b.server()) && a.session().equals(b.session()) && a.challenge().equals(b.challenge())
                && b.major()==MAJOR && b.minor()==MINOR && b.features()==FEATURES && b.catalog().equals(CATALOG);
    }
    private static boolean bool(DataInputStream in) throws IOException {
        int value=in.readUnsignedByte();if (value>1) throw new IllegalArgumentException("boolean");return value==1;
    }
    private static void uuid(DataOutputStream out,UUID id) throws IOException { out.writeLong(id.getMostSignificantBits());out.writeLong(id.getLeastSignificantBits()); }
    private static UUID uuid(DataInputStream in) throws IOException { return new UUID(in.readLong(),in.readLong()); }
    private static void string(DataOutputStream out,String value) throws IOException {
        byte[] b=value.getBytes(StandardCharsets.UTF_8);if (b.length>128) throw new IllegalArgumentException("string bound");out.writeShort(b.length);out.write(b);
    }
    private static String string(DataInputStream in) throws IOException {
        int n=in.readUnsignedShort();if (n>128 || n>in.available()) throw new IllegalArgumentException("string bound");
        return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(in.readNBytes(n))).toString();
    }
    private static String sha(byte[] bytes) { try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(NoSuchAlgorithmException e){throw new AssertionError(e);} }
    private BridgeWire() {}
}
