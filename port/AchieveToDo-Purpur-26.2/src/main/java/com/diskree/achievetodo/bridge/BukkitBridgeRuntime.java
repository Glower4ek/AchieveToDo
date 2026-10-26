package com.diskree.achievetodo.bridge;

import io.papermc.paper.connection.*;
import io.papermc.paper.event.connection.configuration.*;
import io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent;
import java.util.*;
import java.util.concurrent.*;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitTask;

/** Explicit disposable progression preview; never claims production gameplay readiness. */
public final class BukkitBridgeRuntime implements Listener,PluginMessageListener,AutoCloseable {
    private static final long HELLO_BUDGET=TimeUnit.SECONDS.toNanos(5),TOTAL_BUDGET=TimeUnit.SECONDS.toNanos(20);
    private final JavaPlugin plugin;
    private final UUID serverId;
    private final boolean groupScope;
    private final Map<PlayerConnection,Session> connections=new IdentityHashMap<>();
    private final Map<UUID,Session> players=new HashMap<>();
    private volatile boolean open;
    private BukkitTask ticker;
    private final java.util.function.Supplier<com.diskree.achievetodo.tracking.bukkit.BukkitProgressionRuntime> progression;
    private java.util.function.Function<UUID,BridgeLandmarks.Snapshot> landmarks=id->BridgeLandmarks.Snapshot.disabled();
    public void setLandmarks(java.util.function.Function<UUID,BridgeLandmarks.Snapshot> source) { landmarks=Objects.requireNonNull(source); }
    private static final class Session {
        final UUID playerId;
        PlayerCommonConnection connection;
        BridgeHandshake handshake;
        BridgeWire.Projection projection;
        UUID epoch;
        String profileName;
        UUID boundWorld;
        String boundKey;
        CompletableFuture<Boolean> helloGate,admitGate;
        long deadline,lastResync;
        boolean sent,admitted,active,reloadSuspended;
        final BridgeResyncGate resync=new BridgeResyncGate();
        BridgeWire.Patch lastPatch,previousPatch;
        BridgeWire.Reference geometryReference;
        BridgeLandmarks.Snapshot geometry;
        BridgeLandmarks.Snapshot geometrySource;
        long geometrySequence;
        int geometryTick;
        Session(UUID player,PlayerConfigurationConnection c) { playerId=player;connection=c; }
    }
    private static UUID optionalId(String name) { String v=System.getProperty(name);return v==null?null:UUID.fromString(v); }
    public BukkitBridgeRuntime(JavaPlugin plugin,java.util.function.Supplier<com.diskree.achievetodo.tracking.bukkit.BukkitProgressionRuntime> progression) {
        this.plugin=plugin;this.progression=progression;
        serverId=optionalId("achievetodo.bridge.server");groupScope=Boolean.getBoolean("achievetodo.bridge.groupScope");
    }
    public void start() {
        open=true;
        var messenger=plugin.getServer().getMessenger();
        messenger.registerOutgoingPluginChannel(plugin,BridgeWire.CHANNEL);
        messenger.registerIncomingPluginChannel(plugin,BridgeWire.CHANNEL,this);
        plugin.getServer().getPluginManager().registerEvents(this,plugin);
        ticker=plugin.getServer().getScheduler().runTaskTimer(plugin,this::tick,1,1);
        plugin.registerCommand("atdbridge","Console-only disposable protocol demonstration",new io.papermc.paper.command.brigadier.BasicCommand() {
            @Override public void execute(io.papermc.paper.command.brigadier.CommandSourceStack source,String[] args) {
                CommandSender sender=source.getSender();
                if (!(sender instanceof ConsoleCommandSender) || !enabled() || args.length!=1) return;
                switch (args[0]) {
                    case "resync" -> inject("gap");
                    case "stale", "gap" -> inject(args[0]);
                    case "status" -> plugin.getLogger().info("ATD_BRIDGE STATUS sessions="+players.size()+" productGameplayReady=false");
                    default -> { return; }
                }
            }
        });
        plugin.getLogger().info("ATD_BRIDGE v1.3; testOnly="+enabled()+"; productGameplayReady=false; default admission CLOSED");
    }
    private boolean enabled() {
        return Boolean.getBoolean("achievetodo.bridge.spike") && Boolean.getBoolean("achievetodo.bridge.real") && progression.get()!=null && progression.get().isReady() && serverId!=null && !serverId.equals(new UUID(0,0));
    }
    private void dispatch(Runnable action) {
        if (Bukkit.isPrimaryThread()) action.run();
        else plugin.getServer().getScheduler().runTask(plugin,action);
    }
    @EventHandler
    public void configure(AsyncPlayerConnectionConfigureEvent event) {
        if (!event.isAsynchronous()) throw new IllegalStateException("no main-thread wait");
        var connection=event.getConnection();var gate=new CompletableFuture<Boolean>();
        if (!open) { connection.disconnect(Component.text("ATD_SERVER_CLOSED"));return; }
        dispatch(() -> {
            if (!open || gate.isDone() || !connection.isConnected()) { gate.complete(false);return; }
            if (!enabled() || players.size()>=16) { connection.disconnect(Component.text("ATD_SPIKE_SCOPE_CLOSED"));gate.complete(false);return; }
            UUID id=connection.getProfile().getId();
            if (id==null || players.containsKey(id)) { connection.disconnect(Component.text("ATD_DUPLICATE_CONNECTION"));gate.complete(false);return; }
            Session s=new Session(id,connection);s.profileName=connection.getProfile().getName();players.put(id,s);connections.put(connection,s);
            bootstrap(s,connection,gate,null);log("HOLD_CONFIGURATION",s);
        });
        await(connection,gate,7);
    }
    private void bootstrap(Session s,PlayerConfigurationConnection c,CompletableFuture<Boolean> gate,UUID existingSession) {
        s.connection=c;s.helloGate=gate;s.admitGate=null;s.admitted=false;s.active=false;s.sent=false;
        s.deadline=System.nanoTime()+TOTAL_BUDGET;
        s.epoch=UUID.randomUUID();
        s.handshake=existingSession==null?new BridgeHandshake(serverId,System.nanoTime(),HELLO_BUDGET)
                :new BridgeHandshake(serverId,existingSession,System.nanoTime(),HELLO_BUDGET);
    }
    private void await(PlayerConfigurationConnection connection,CompletableFuture<Boolean> gate,int seconds) {
        boolean success=false;
        try { success=gate.get(seconds,TimeUnit.SECONDS); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        catch (ExecutionException | TimeoutException ignored) { }
        if (!success) {
            gate.complete(false);
            connection.disconnect(Component.text("ATD_SYNC_TIMEOUT: universal AchieveToDo1.3 required"));
            if (open) dispatch(() -> { Session s=connections.get(connection);if (s!=null) close(s,"ATD_WAIT_CLOSED"); });
        }
    }
    @EventHandler(priority=EventPriority.MONITOR)
    public void spawn(AsyncPlayerSpawnLocationEvent event) {
        var connection=event.getConnection();var location=event.getSpawnLocation();var gate=new CompletableFuture<Boolean>();
        if (!open) { connection.disconnect(Component.text("ATD_SERVER_CLOSED"));return; }
        dispatch(() -> {
            Session s=connections.get(connection);
            if (gate.isDone() || s==null || s.handshake.state()!=BridgeHandshake.State.ACKNOWLEDGED || s.admitGate!=null) {
                connection.disconnect(Component.text("ATD_SPAWN_WITHOUT_HANDSHAKE"));gate.complete(false);return;
            }
            s.admitGate=gate;
            World world=location.getWorld();
            log("OBSERVED_SPAWN world="+(world==null?"null":world.getUID()+" key="+world.getKey()),s);
            if (!accepts(world)) {
                close(s,"ATD_WORLD_UNBOUND");return;
            }
            log("VERIFIED_SPAWN world="+world.getUID()+" key="+world.getKey(),s);
            s.boundWorld=world.getUID();s.boundKey=world.getKey().toString();
            try { snapshot(s); } catch(RuntimeException failure) {close(s,"ATD_PROGRESSION_UNAVAILABLE");}
        });
        await(connection,gate,12);
    }
    private BridgeWire.State capture(Session s) {
        var runtime=progression.get();if(runtime==null)throw new IllegalStateException("ATD_PROGRESSION_UNAVAILABLE");
        return BridgeProgressionAdapter.project(runtime.capturePresentation(s.playerId,s.profileName,s.boundWorld,s.boundKey));
    }
    private boolean refresh(Session s) {
        var state=capture(s);
        if(s.projection!=null && !BridgeWire.sameGroup(s.projection.state(),state))throw new IllegalStateException("ATD_SCOPE_CHANGED_RECONNECT_REQUIRED");
        if(s.projection!=null && s.projection.state().equals(state))return false;
        long next=s.projection==null?0:Math.addExact(s.projection.revision(),1);
        s.projection=new BridgeWire.Projection(s.epoch,next,s.projection==null?UUID.randomUUID():s.projection.snapshot(),state);
        return true;
    }
    private void snapshot(Session s) {
        refresh(s);
        s.projection=new BridgeWire.Projection(s.epoch,s.projection.revision(),UUID.randomUUID(),s.projection.state());
        send(s,s.handshake.hello().with(BridgeWire.SNAPSHOT,BridgeWire.snapshot(s.projection)));
        log("SNAPSHOT_SENT revision="+s.projection.revision()+" count="+s.projection.state().count()+" rows="+s.projection.state().abilities().size()+" digest="+BridgeWire.digest(s.projection),s);
    }
    private void tick() {
        requireMain();
        for (Session s:List.copyOf(players.values())) {
            if (!s.connection.isConnected()) {
                close(s,"ATD_DISCONNECTED");continue;
            }
            if (s.resync.expired(System.nanoTime())) { close(s,"ATD_RESYNC_TIMEOUT");continue; }
            if (!s.active && System.nanoTime()-s.deadline>=0) { close(s,"ATD_SYNC_TIMEOUT");continue; }
            if (s.handshake.state()==BridgeHandshake.State.NEGOTIATING && s.handshake.expired(System.nanoTime())) { close(s,"ATD_CLIENT_REQUIRED: universal AchieveToDo1.3 handshake timeout");continue; }
            if (s.active) {
                try {
                    World actual=((PlayerGameConnection)s.connection).getPlayer().getWorld();
                    if(!s.boundWorld.equals(actual.getUID()) || !s.boundKey.equals(actual.getKey().toString())) {
                        transition(s,actual);continue;
                    }
                    if(s.resync.pending()) {
                        if(s.reloadSuspended) {
                            if(progression.get()==null || !progression.get().isReady())continue;
                            // Readiness changed even when config rejection retained identical effective data.
                            if(!refresh(s))s.projection=s.projection.change(Math.addExact(s.projection.revision(),1),s.projection.state());
                            playSnapshot(s);s.reloadSuspended=false;
                        } else capture(s);
                        continue;
                    }
                    var previous=s.projection;
                    if(refresh(s)) {
                        if(s.resync.pending())playSnapshot(s);
                        else {
                            s.previousPatch=s.lastPatch;s.lastPatch=new BridgeWire.Patch(s.epoch,previous.revision(),s.projection.revision(),s.projection.state(),BridgeWire.digest(s.projection));
                            send(s,s.handshake.hello().with(BridgeWire.DELTA,BridgeWire.patch(s.lastPatch)));
                            log("DELTA_SENT base="+previous.revision()+" next="+s.projection.revision()+" count="+s.projection.state().count()+" configGeneration="+s.projection.state().generation(),s);
                        }
                    }
                    if(++s.geometryTick>=5) {
                        s.geometryTick=0;
                        var supplied=landmarks.apply(s.playerId);
                        var reference=BridgeWire.reference(s.projection);
                        // The runtime returns an immutable cached snapshot. Do not scan/copy
                        // every loaded hull for every idle player on each polling tick.
                        if(!reference.equals(s.geometryReference) || supplied!=s.geometrySource) {
                            var bound=s.projection.state().worlds().values().stream().map(BridgeWire.Binding::uuid).collect(java.util.stream.Collectors.toSet());
                            var geometry=new BridgeLandmarks.Snapshot(supplied.enabled(),supplied.boxes().stream().filter(b->bound.contains(b.world())).toList());
                            if(!reference.equals(s.geometryReference) || !geometry.equals(s.geometry)) {
                                for(var page:BridgeLandmarks.pages(reference,++s.geometrySequence,geometry))
                                    send(s,s.handshake.hello().with(BridgeWire.LANDMARKS,BridgeLandmarks.encode(page)));
                                s.geometryReference=reference;s.geometry=geometry;
                            }
                            s.geometrySource=supplied;
                        }
                    }
                } catch(RuntimeException failure) {close(s,"ATD_PROGRESSION_UNAVAILABLE");continue;}
            }
            if (s.connection instanceof PlayerConfigurationConnection c && !s.sent) {
                if (s.handshake.expired(System.nanoTime())) { close(s,"ATD_CLIENT_REQUIRED: universal AchieveToDo bridge1.3 handshake timeout");continue; }
                if (c.getListeningPluginChannels().contains(BridgeWire.CHANNEL)) {
                    s.sent=true;send(s,s.handshake.hello());log("HELLO_SENT",s);
                }
            }
        }
    }
    @Override public void onPluginMessageReceived(String channel,Player ignored,byte[] bytes) {}
    @Override public void onPluginMessageReceived(String channel,PlayerConnection connection,byte[] bytes) {
        requireMain();Session s=connections.get(connection);
        if (!open || s==null) return;
        try {
            BridgeWire.Frame frame=BridgeWire.decode(bytes);
            if (frame.kind()==BridgeWire.ACK && connection instanceof PlayerConfigurationConnection c) {
                if (!s.sent) throw new IllegalArgumentException("ATD_UNSOLICITED_ACK");
                s.handshake.accept(frame,System.nanoTime());log("HELLO_ACK_VALID",s);
                s.helloGate.complete(true);

            } else {
                if (!BridgeWire.sameContext(s.handshake.hello(),frame)) throw new IllegalArgumentException("ATD_SESSION_MISMATCH");
                if (frame.kind()==BridgeWire.READY && connection instanceof PlayerConfigurationConnection) {
                    if (s.admitGate==null || s.admitGate.isDone() || s.admitted || s.projection==null
                            || !BridgeWire.reference(s.projection).equals(BridgeWire.reference(frame.body())))
                        throw new IllegalArgumentException("ATD_INVALID_READY");
                    if (refresh(s)) { snapshot(s);return; }
                    s.admitted=true;
                    send(s,s.handshake.hello().with(BridgeWire.ADMIT,BridgeWire.reference(BridgeWire.reference(s.projection))));
                    log("PROTOCOL_SNAPSHOT_READY ADMITTED revision="+s.projection.revision()+" productGameplayReady=false",s);
                    s.admitGate.complete(true);
                } else if (frame.kind()==BridgeWire.RESYNC && s.active && connection instanceof PlayerGameConnection) {
                    var ticket=BridgeWire.ticket(frame.body());
                    if (!ticket.reference().epoch().equals(s.epoch))return; // retired world epoch
                    if (ticket.reference().revision()<0
                            || ticket.reference().revision()>s.projection.revision() || !ticket.reference().snapshot().equals(s.projection.snapshot()))
                        throw new IllegalArgumentException("ATD_INVALID_RESYNC_REQUEST");
                    if (System.nanoTime()-s.lastResync<TimeUnit.SECONDS.toNanos(1)) throw new IllegalArgumentException("ATD_RESYNC_RATE");
                    s.lastResync=System.nanoTime();
                    s.resync.begin(ticket.request(),System.nanoTime(),TimeUnit.SECONDS.toNanos(5));
                    playSnapshot(s);
                } else if (frame.kind()==BridgeWire.READY && s.active && connection instanceof PlayerGameConnection) {
                    var ticket=BridgeWire.ticket(frame.body());if(s.reloadSuspended)return;s.resync.validate(ticket,System.nanoTime());
                    if (refresh(s)) { playSnapshot(s);return; }
                    send(s,s.handshake.hello().with(BridgeWire.ADMIT,BridgeWire.ticket(ticket)));
                    s.resync.clear();log("PLAY_RESYNC_COMPLETE revision="+s.projection.revision(),s);
                } else throw new IllegalArgumentException("ATD_INVALID_MESSAGE_PHASE");
            }
        } catch (RuntimeException invalid) { close(s,"ATD_INVALID_MESSAGE_OR_PROGRESSION: "+invalid.getMessage()); }
    }
    private void playSnapshot(Session s) {
        Player player=((PlayerGameConnection)s.connection).getPlayer();
        if (!accepts(player.getWorld()) || !s.boundWorld.equals(player.getWorld().getUID()) || !s.boundKey.equals(player.getWorld().getKey().toString())) {
            close(s,"ATD_PLAY_SCOPE_MISMATCH");return;
        }
        refresh(s);
        s.projection=new BridgeWire.Projection(s.epoch,s.projection.revision(),UUID.randomUUID(),s.projection.state());
        s.resync.snapshot(BridgeWire.reference(s.projection));
        send(s,s.handshake.hello().with(BridgeWire.SNAPSHOT,BridgeWire.resyncSnapshot(new BridgeWire.ResyncSnapshot(s.resync.request(),s.projection))));
        log("PLAY_SNAPSHOT_SENT request="+s.resync.request()+" revision="+s.projection.revision()+" digest="+BridgeWire.digest(s.projection),s);
    }
    @EventHandler(priority=EventPriority.LOWEST)
    public void join(PlayerJoinEvent event) {
        Player player=event.getPlayer();Session s=players.get(player.getUniqueId());
        if (s==null || !s.admitted || !accepts(player.getWorld()) || !s.projection.world().equals(player.getWorld().getUID()) || !s.projection.worldKey().equals(player.getWorld().getKey().toString())) {
            player.kick(Component.text("ATD_UNEXPECTED_PLAY_SCOPE"));return;
        }
        connections.remove(s.connection);s.connection=player.getConnection();connections.put(s.connection,s);
        s.active=true;log("PLAY_ENTER revision="+s.projection.revision()+" world="+player.getWorld().getUID(),s);
    }
    @EventHandler public void quit(PlayerQuitEvent event) {
        Session s=players.get(event.getPlayer().getUniqueId());
        if (s!=null && s.connection instanceof PlayerGameConnection game && game.getPlayer()==event.getPlayer()) close(s,"ATD_QUIT");
    }
    @EventHandler(priority=EventPriority.LOWEST)
    public void resourcesReloaded(io.papermc.paper.event.server.ServerResourcesReloadedEvent event) {
        for(Session s:List.copyOf(players.values()))if(s.active) {
            s.reloadSuspended=true;
            if(!s.resync.pending()) {
                s.resync.begin(UUID.randomUUID(),System.nanoTime(),BridgeWire.TRANSITION_BUDGET_NANOS);
                send(s,s.handshake.hello().with(BridgeWire.SUSPEND,BridgeWire.suspend(new BridgeWire.Ticket(s.resync.request(),BridgeWire.reference(s.projection)))));
                log("SOURCE_RELOAD_SUSPENDED",s);
            }
        }
    }
    private boolean accepts(World world) {
        var runtime=progression.get();
        if(world==null || runtime==null || !runtime.isReady() || Bukkit.getWorld(world.getUID())!=world)return false;
        var group=runtime.getWorldGroup();
        return group.contains(world.getUID(),world.getKey().toString()) &&
            (groupScope || group.worlds().get("overworld").matches(world.getUID(),world.getKey().toString()));
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void travel(PlayerTeleportEvent event) {
        Session s=players.get(event.getPlayer().getUniqueId());
        if(s==null)return;
        World destination=event.getTo()==null?null:event.getTo().getWorld();
        if(destination!=null && destination==event.getPlayer().getWorld())return;
        if(!accepts(destination) || s.resync.pending()) {event.setCancelled(true);close(s,"ATD_WORLD_UNBOUND_OR_TRANSITION_BUSY");return;}
        try {
            var candidate=BridgeProgressionAdapter.project(progression.get().capturePresentation(s.playerId,s.profileName,destination.getUID(),destination.getKey().toString()));
            if(!BridgeWire.sameGroup(s.projection.state(),candidate))throw new IllegalArgumentException("group/config changed");
        } catch(RuntimeException invalid) {event.setCancelled(true);close(s,"ATD_WORLD_UNBOUND");}
    }
    @EventHandler(priority=EventPriority.MONITOR)
    public void changedWorld(PlayerChangedWorldEvent event) {
        Session s=players.get(event.getPlayer().getUniqueId());if(s==null || !s.active)return;
        transition(s,event.getPlayer().getWorld());
    }
    private void transition(Session s,World world) {
        if(!accepts(world) || s.resync.pending()){close(s,"ATD_WORLD_UNBOUND_OR_TRANSITION_BUSY");return;}
        try {
            s.boundWorld=world.getUID();s.boundKey=world.getKey().toString();var candidate=capture(s);
            if(!BridgeWire.sameGroup(s.projection.state(),candidate))throw new IllegalArgumentException("group/config changed");
            s.epoch=UUID.randomUUID();s.projection=new BridgeWire.Projection(s.epoch,0,UUID.randomUUID(),candidate);s.previousPatch=null;s.lastPatch=null;
            s.resync.begin(UUID.randomUUID(),System.nanoTime(),BridgeWire.TRANSITION_BUDGET_NANOS);s.resync.snapshot(BridgeWire.reference(s.projection));
            send(s,s.handshake.hello().with(BridgeWire.SUSPEND,BridgeWire.suspend(new BridgeWire.ResyncSnapshot(s.resync.request(),s.projection))));
            log("WORLD_TRANSITION_SNAPSHOT epoch="+s.epoch+" world="+s.boundWorld+" key="+s.boundKey,s);
        } catch(RuntimeException failure){close(s,"ATD_WORLD_TRANSITION_FAILED");}
    }
    private void inject(String kind) {
        for (Session s:List.copyOf(players.values())) if (s.active && !s.resync.pending()) {
            BridgeWire.Patch patch="stale".equals(kind)?s.previousPatch:new BridgeWire.Patch(s.projection.epoch(),s.projection.revision()+1,s.projection.revision()+2,s.projection.state(),"0".repeat(64));
            if (patch!=null) { send(s,s.handshake.hello().with(BridgeWire.DELTA,BridgeWire.patch(patch)));log("TEST_INJECT_"+kind,s); }
        }
    }
    private void send(Session s,BridgeWire.Frame frame) { s.connection.sendPluginMessage(plugin,BridgeWire.CHANNEL,BridgeWire.encode(frame)); }
    private void close(Session s,String reason) {
        if (players.get(s.playerId)!=s) return;
        players.remove(s.playerId);connections.remove(s.connection);s.active=false;s.admitted=false;s.handshake.close();s.resync.clear();
        s.connection.disconnect(Component.text(reason));s.helloGate.complete(false);
        if (s.admitGate!=null) s.admitGate.complete(false);log(reason,s);s.projection=null;
    }
    private void log(String action,Session s) { plugin.getLogger().info("ATD_BRIDGE "+action+" session="+s.handshake.hello().session()+" thread="+Thread.currentThread().getName()); }
    private static void requireMain() { if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Bridge owner thread"); }
    @Override public void close() {
        open=false;if (ticker!=null)ticker.cancel();
        for (Session s:List.copyOf(players.values())) close(s,"ATD_SERVER_CLOSED");
        HandlerList.unregisterAll(this);
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin,BridgeWire.CHANNEL,this);
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin,BridgeWire.CHANNEL);
        plugin.getLogger().info("ATD_BRIDGE CLEANUP sessions="+players.size()+" connections="+connections.size());
    }
}
