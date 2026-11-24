package com.diskree.achievetodo.bridge;

import java.util.UUID;

/** Immutable staging; the transport caller also checks connection generation/session. */
public final class BridgeProjectionCache {
    public enum Result { APPLIED, DUPLICATE, STALE, RESYNC }
    private BridgeWire.Projection current;
    private boolean admitted;
    private UUID request;
    private long deadline;
    private BridgeWire.Projection staged;
    private UUID transitionEpoch;
    public BridgeWire.Projection current() { return current; }
    public boolean admitted() { return admitted; }
    public void replace(BridgeWire.Projection snapshot) {
        if (current!=null && (!snapshot.epoch().equals(current.epoch()) || snapshot.revision()<current.revision()))
            throw new IllegalArgumentException("unsolicited/retired epoch or stale snapshot");
        current=snapshot;admitted=false;
    }
    public void admit(BridgeWire.Reference reference) {
        if (admitted || current==null || !BridgeWire.reference(current).equals(reference)) throw new IllegalArgumentException("ATD_INVALID_ADMIT");
        admitted=true;
    }
    public Result apply(BridgeWire.Patch patch) {
        if (current==null) { admitted=false;return Result.RESYNC; }
        if (!patch.epoch().equals(current.epoch()) || patch.next()<current.revision()) return Result.STALE;
        if (patch.next()==current.revision()) {
            if (!patch.digest().equals(BridgeWire.digest(current))) throw new IllegalArgumentException("revision conflict");
            return Result.DUPLICATE;
        }
        if (!admitted || patch.base()!=current.revision()) { admitted=false;return Result.RESYNC; }
        var next=current.change(patch.next(),patch.value());
        if (!patch.digest().equals(BridgeWire.digest(next))) throw new IllegalArgumentException("delta digest");
        if (!BridgeWire.sameScope(current,next)) throw new IllegalArgumentException("delta scope");
        current=next;return Result.APPLIED;
    }
    public UUID request() { return request; }
    public boolean resyncPending() { return request!=null; }
    public void beginResync(UUID token,long now,long budget) {
        if (current==null || request!=null || budget<=0) throw new IllegalArgumentException("resync lifecycle");
        request=token;deadline=Math.addExact(now,budget);staged=null;admitted=false;transitionEpoch=null;
    }
    public boolean expired(long now) { return request!=null && now-deadline>=0; }
    public Result stageResync(BridgeWire.ResyncSnapshot message,long now) {
        if (request==null || !request.equals(message.request())) return Result.STALE;
        if (expired(now)) throw new IllegalArgumentException("ATD_RESYNC_TIMEOUT");
        var p=message.projection();
        if (transitionEpoch==null) {
            if (!BridgeWire.sameScope(current,p)) throw new IllegalArgumentException("ATD_RESYNC_SCOPE");
            if (p.revision()<current.revision() || p.snapshot().equals(current.snapshot())) return Result.STALE;
            if (p.revision()==current.revision() && !p.state().equals(current.state())) throw new IllegalArgumentException("revision conflict");
        } else if (!p.epoch().equals(transitionEpoch) || !BridgeWire.sameGroup(current.state(),p.state()) ||
                   staged!=null && (!p.world().equals(staged.world()) || !p.worldKey().equals(staged.worldKey()))) throw new IllegalArgumentException("transition scope");
        if (staged!=null) {
            if (p.equals(staged)) return Result.DUPLICATE;
            if (p.revision()<=staged.revision()) return Result.STALE;
        }
        staged=p;return Result.APPLIED;
    }
    public BridgeWire.Reference stagedReference() {
        if (staged==null) throw new IllegalArgumentException("snapshot not staged");
        return BridgeWire.reference(staged);
    }
    public Result commitResync(BridgeWire.Ticket ready,long now) {
        if (request==null || !request.equals(ready.request())) return Result.STALE;
        if (expired(now)) throw new IllegalArgumentException("ATD_RESYNC_TIMEOUT");
        if (staged==null || !BridgeWire.reference(staged).equals(ready.reference())) throw new IllegalArgumentException("ATD_INVALID_RESYNC_ADMIT");
        current=staged;staged=null;request=null;transitionEpoch=null;admitted=true;return Result.APPLIED;
    }
    public Result stageTransition(BridgeWire.ResyncSnapshot message,long now,long budget) {
        if (current==null || !admitted || request!=null || current.epoch().equals(message.projection().epoch()) ||
            !BridgeWire.sameGroup(current.state(),message.projection().state()) || current.world().equals(message.projection().world())) throw new IllegalArgumentException("ATD_TRANSITION_SCOPE");
        beginResync(message.request(),now,budget);transitionEpoch=message.projection().epoch();
        return stageResync(message,now);
    }
    public void clear() { current=null;admitted=false;request=null;staged=null;deadline=0;transitionEpoch=null; }
}
