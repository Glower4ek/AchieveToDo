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
    private static void clear() {
        generation++;cache.clear();hello=null;configurationListener=null;playListener=null;
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
        ClientConfigurationConnectionEvents.COMPLETE.register((listener,client) -> {
            synchronized (BridgeClient.class) {
                if (configurationListener==listener && hello!=null && !cache.admitted())
                    connection.disconnect(Component.literal("ATD_FINISH_BEFORE_ADMIT"));
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
            synchronized (BridgeClient.class) {
                if (configurationListener!=context.packetListener() || connection!=context.packetContext().get(PacketContext.CONNECTION)) return;
                try {
                    var frame=BridgeWire.decode(payload.bytes());
                    if (frame.kind()==BridgeWire.HELLO) {
                        if (hello!=null || fabricObserved || frame.body().length!=0 || frame.major()!=BridgeWire.MAJOR
                                || frame.minor()!=BridgeWire.MINOR || frame.features()!=BridgeWire.FEATURES || !frame.catalog().equals(BridgeWire.CATALOG))
                            throw new IllegalArgumentException("ATD_PROTOCOL_INCOMPATIBLE");
                        hello=frame;
                        var ack=new BridgeWire.Frame(BridgeWire.ACK,BridgeWire.MAJOR,BridgeWire.MINOR,frame.server(),frame.session(),frame.challenge(),BridgeWire.FEATURES,BridgeWire.CATALOG,"universal-0.2.0-dev.s2p-r2");
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
            synchronized (BridgeClient.class) {
                if (playListener!=context.player().connection || connection!=context.packetContext().get(PacketContext.CONNECTION)) return;
                try {
                    var frame=BridgeWire.decode(payload.bytes());
                    if (hello==null || !BridgeWire.sameContext(hello,frame)) return;
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
    }
    private BridgeClient() {}
}
