package com.diskree.achievetodo.client.bridge;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.bridge.*;
import com.diskree.achievetodo.networking.c2s.DemystifyAbilityPayload;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.networking.v1.context.*;
import net.fabricmc.fabric.impl.networking.RegistrationPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;

/** Session cache is staged without a Minecraft world; only JOIN publishes diagnostic readiness. */
public final class BridgeClient {
    private static final BridgeProjectionCache cache=new BridgeProjectionCache();
    private static final BridgeLandmarks.Cache landmarks=new BridgeLandmarks.Cache();
    public static synchronized BridgeLandmarks.Snapshot landmarks() { return presentationReady()?landmarks.current(cache.current()):null; }
    private static Connection connection;
    private static Object configurationListener,playListener;
    private static BridgeWire.Frame hello;
    private static long generation;
    private static boolean configuring,uiActive,resyncRequested,fabricObserved;
    private static void log(String message) { AchieveToDoMod.logger.info("ATD_BRIDGE {}",message); }
    private static Connection physical(Object listener) {
        return ((PacketContextProvider)listener).getPacketContext().orElseThrow(PacketContext.CONNECTION);
    }
    public static synchronized boolean isFabricBackend() {
        return Minecraft.getInstance().getSingleplayerServer()!=null || (hello==null && (fabricObserved || ClientPlayNetworking.canSend(DemystifyAbilityPayload.ID)));
    }
    public static synchronized void observeFabric() {
        if (hello!=null) { connection.disconnect(Component.literal("ATD_BACKEND_CONFLICT"));return; }
        fabricObserved=true;
    }
    public static synchronized boolean isPurpurBackend() { return hello!=null; }
    public static synchronized BridgeWire.Projection presentation() { return hello==null?null:cache.current(); }
    public static synchronized boolean presentationReady() {
        var client=Minecraft.getInstance();
        return uiActive && connection!=null && connection.isConnected() && cache.admitted() && cache.current()!=null && client.level!=null &&
            client.level.dimension().identifier().toString().equals(cache.current().worldKey());
    }
    public static synchronized String presentationStatus() {
        var p=presentation();
        return p==null?"Progression unavailable":(presentationReady()?"Progression preview":"Progression syncing")+" | "+p.state().difficulty()+" | "+p.state().count()+" | gameplay enforcement incomplete";
    }
    private static void clear() {
        Minecraft.getInstance().execute(()->{BridgePresentation.clear();BridgeLandmarkPresentation.clear();});
        generation++;cache.clear();landmarks.clear();hello=null;configurationListener=null;playListener=null;
        uiActive=false;resyncRequested=false;fabricObserved=false;
    }
    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            synchronized (BridgeClient.class) {
                if (cache.expired(System.nanoTime()) && connection!=null) {
                    log("ATD_RESYNC_TIMEOUT");cache.clear();uiActive=false;
                    connection.disconnect(Component.literal("ATD_RESYNC_TIMEOUT"));
                }
            }
        });
        ClientConfigurationConnectionEvents.INIT.register((listener,client) -> {
            synchronized (BridgeClient.class) {
                clear();connection=physical(listener);configurationListener=listener;configuring=true;
                log("CONFIGURATION_INIT generation="+generation);
            }
        });
        ClientConfigurationConnectionEvents.START.register((listener,client) ->
            // Pinned Fabric6.3.4 native channel advertisement, not proof of compatibility.
            ClientConfigurationNetworking.getSender().sendPacket(
                new RegistrationPayload(RegistrationPayload.REGISTER,List.of(BridgePayload.ID.id()))));
        // Configuration authority is applied synchronously on the ordered receive path.
        // COMPLETE must verify it before native play initialization, never enqueue a check
        // which a reentrant Minecraft task loop could run after admission to play.
        ClientConfigurationConnectionEvents.COMPLETE.register((listener,client) -> {
            synchronized (BridgeClient.class) {
                if (configurationListener==listener && hello!=null) {
                    if(!cache.admitted())connection.disconnect(Component.literal("ATD_FINISH_BEFORE_ADMIT"));
                    else log("CONFIGURATION_FINISH_VERIFIED session="+hello.session());
                }
            }
        });
        ClientConfigurationConnectionEvents.DISCONNECT.register((listener,client) -> {
            synchronized (BridgeClient.class) {
                if (configurationListener==listener) { clear();connection=null;configuring=false;log("CLEANUP configuration"); }
            }
        });
        ClientPlayConnectionEvents.INIT.register((listener,client) -> {
            synchronized (BridgeClient.class) {
                if (connection!=listener.getConnection()) { clear();connection=listener.getConnection(); }
                playListener=listener;configuring=false;
            }
        });
        ClientPlayConnectionEvents.JOIN.register((listener,sender,client) -> {
            synchronized (BridgeClient.class) {
                if (playListener!=listener) return;
                if (hello==null) {
                    log("REMOTE_CLASSIFIED fabric="+isFabricBackend()+" neutral="+!isFabricBackend());return;
                }
                if (!cache.admitted() || client.level==null
                        || !client.level.dimension().identifier().toString().equals(cache.current().worldKey())) {
                    connection.disconnect(Component.literal("ATD_PLAY_SCOPE_MISMATCH"));return;
                }
                uiActive=true;
                log("PLAY_DIAGNOSTIC_ACTIVE session="+hello.session()+" epoch="+cache.current().epoch()
                        +" revision="+cache.current().revision()+" world="+cache.current().world()
                        +" productGameplayReady=false");
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((listener,client) -> {
            synchronized (BridgeClient.class) {
                if (!configuring && connection==listener.getConnection()) { clear();connection=null;log("CLEANUP play"); }
            }
        });
        ClientConfigurationNetworking.registerGlobalReceiver(BridgePayload.ID,(payload,context) -> {
            // Fabric 6.3.4 invokes configuration receivers on the connection's network
            // thread. Only immutable wire data and the synchronized session cache are
            // touched here; no world, presentation or render state is accessed. Applying
            // ADMIT before returning establishes a wire-order barrier before FINISH.
            synchronized (BridgeClient.class) {
                if(connection==null || !connection.isConnected())return;
                if (configurationListener!=context.packetListener() || connection!=context.packetContext().get(PacketContext.CONNECTION)) return;
                try {
                    var frame=BridgeWire.decode(payload.bytes());
                    if (frame.kind()==BridgeWire.HELLO) {
                        if (hello!=null || fabricObserved || frame.body().length!=0 || frame.major()!=BridgeWire.MAJOR
                                || frame.minor()!=BridgeWire.MINOR || frame.features()!=BridgeWire.FEATURES || !frame.catalog().equals(BridgeWire.CATALOG))
                            throw new IllegalArgumentException("ATD_PROTOCOL_INCOMPATIBLE");
                        hello=frame;
                        var ack=new BridgeWire.Frame(BridgeWire.ACK,BridgeWire.MAJOR,BridgeWire.MINOR,frame.server(),frame.session(),frame.challenge(),BridgeWire.FEATURES,BridgeWire.CATALOG,"universal-"+com.diskree.achievetodo.BuildConfig.MOD_VERSION);
                        context.responseSender().sendPacket(new BridgePayload(BridgeWire.encode(ack)));
                        log("HELLO_ACK_SENT phase=configuration session="+hello.session());
                    } else {
                        if (hello==null || !BridgeWire.sameContext(hello,frame)) return; // retired session/generation
                        if (frame.kind()==BridgeWire.SNAPSHOT) {
                            if (cache.admitted()) throw new IllegalArgumentException("unsolicited snapshot");
                            var projection=BridgeWire.snapshot(frame.body());cache.replace(projection);
                            context.responseSender().sendPacket(new BridgePayload(BridgeWire.encode(hello.with(BridgeWire.READY,BridgeWire.reference(BridgeWire.reference(projection))))));
                            log("SNAPSHOT_STAGED_READY session="+hello.session()+" epoch="+projection.epoch()+" revision="+projection.revision()+" digest="+BridgeWire.digest(projection)+" worldObjectRequired=false");
                        } else if (frame.kind()==BridgeWire.ADMIT) {
                            cache.admit(BridgeWire.reference(frame.body()));log("ADMIT_RECEIVED configuration session="+hello.session());
                        } else throw new IllegalArgumentException("ATD_INVALID_CONFIGURATION_MESSAGE");
                    }
                } catch (IllegalArgumentException failure) { context.responseSender().disconnect(Component.literal(failure.getMessage())); }
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(BridgePayload.ID,(payload,context) -> {
            long receivedGeneration; synchronized(BridgeClient.class){receivedGeneration=generation;}
            Minecraft.getInstance().execute(() -> {
            synchronized (BridgeClient.class) {
                if(receivedGeneration!=generation)return;
                if(connection==null || !connection.isConnected())return;
                if (playListener!=context.player().connection || connection!=context.packetContext().get(PacketContext.CONNECTION)) return;
                try {
                    var frame=BridgeWire.decode(payload.bytes());
                    if (hello==null || !BridgeWire.sameContext(hello,frame)) return;
                    if(frame.kind()==BridgeWire.LANDMARKS) {
                        if(!presentationReady() || cache.resyncPending())return;
                        if(landmarks.accept(BridgeLandmarks.decode(frame.body()),cache.current()))
                            log("LANDMARKS_APPLIED revision="+cache.current().revision()+" enabled="+landmarks.current(cache.current()).enabled()+" boxes="+landmarks.current(cache.current()).boxes().size());
                        return;
                    }
                    if (frame.kind()==BridgeWire.SUSPEND) {
                        landmarks.clear();
                        var suspension=BridgeWire.suspension(frame.body());
                        if(suspension.control()!=null) {
                            var control=suspension.control();
                            if(cache.current()==null || !BridgeWire.reference(cache.current()).equals(control.reference()))throw new IllegalArgumentException("ATD_SUSPEND_REFERENCE");
                            cache.beginResync(control.request(),System.nanoTime(),BridgeWire.TRANSITION_BUDGET_NANOS);uiActive=false;resyncRequested=true;
                            log("SOURCE_RELOAD_SUSPENDED");return;
                        }
                        var message=suspension.transition();
                        cache.stageTransition(message,System.nanoTime(),BridgeWire.TRANSITION_BUDGET_NANOS);
                        uiActive=false;resyncRequested=true;
                        context.responseSender().sendPacket(new BridgePayload(BridgeWire.encode(hello.with(BridgeWire.READY,BridgeWire.ticket(new BridgeWire.Ticket(message.request(),cache.stagedReference()))))));
                        log("WORLD_SNAPSHOT_STAGED epoch="+message.projection().epoch()+" world="+message.projection().worldKey());return;
                    }
                    if (frame.kind()==BridgeWire.SNAPSHOT) {
                        var message=BridgeWire.resyncSnapshot(frame.body());
                        var result=cache.stageResync(message,System.nanoTime());
                        log("PLAY_SNAPSHOT_"+result+" request="+message.request());
                        if (result==BridgeProjectionCache.Result.APPLIED) {
                            var ticket=new BridgeWire.Ticket(message.request(),cache.stagedReference());
                            context.responseSender().sendPacket(new BridgePayload(BridgeWire.encode(hello.with(BridgeWire.READY,BridgeWire.ticket(ticket)))));
                        }
                        return;
                    }
                    if (frame.kind()==BridgeWire.ADMIT) {
                        var result=cache.commitResync(BridgeWire.ticket(frame.body()),System.nanoTime());
                        if (result==BridgeProjectionCache.Result.APPLIED) {
                            uiActive=true;resyncRequested=false;
                            log("PLAY_RESYNC_COMPLETE session="+hello.session()+" epoch="+cache.current().epoch()+" revision="+cache.current().revision()+" productGameplayReady=false");
                        } else log("PLAY_RESYNC_ADMIT_"+result);
                        return;
                    }
                    if (frame.kind()!=BridgeWire.DELTA) throw new IllegalArgumentException("ATD_INVALID_PLAY_MESSAGE");
                    if (cache.resyncPending()) { log("DELTA_IGNORED_RESYNC_PENDING");return; }
                    var result=cache.apply(BridgeWire.patch(frame.body()));
                    log("DELTA_"+result+" session="+hello.session()+" revision="+(cache.current()==null?-1:cache.current().revision()));
                    if (result==BridgeProjectionCache.Result.RESYNC && !resyncRequested) {
                        resyncRequested=true;uiActive=false;
                        UUID request=UUID.randomUUID();
                        cache.beginResync(request,System.nanoTime(),TimeUnit.SECONDS.toNanos(5));
                        context.responseSender().sendPacket(new BridgePayload(BridgeWire.encode(hello.with(BridgeWire.RESYNC,BridgeWire.ticket(new BridgeWire.Ticket(request,BridgeWire.reference(cache.current())))))));
                        log("RESYNC_REQUESTED");
                    }
                } catch (IllegalArgumentException invalid) { context.responseSender().disconnect(Component.literal(invalid.getMessage())); }
            }
            });
        });
    }
    private BridgeClient() {}
}
