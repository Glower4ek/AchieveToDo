package com.diskree.achievetodo.gameplay.nativeguard;

import java.util.Objects;

/** Invocation identity and one canonical reporter. No saved permission or progression allowance. */
public final class NativeToolCombatInvocation implements AutoCloseable {
    private static final ThreadLocal<NativeToolCombatInvocation> CURRENT=new ThreadLocal<>();
    private final Thread thread=Thread.currentThread();
    private final Object actor,slot,stack,item;
    private final NativeToolCombatInvocation previous,root;
    private final boolean report;
    private Runnable denial;
    private boolean reported,closed;

    public static NativeToolCombatInvocation open(Object actor,Object slot,Object stack,boolean report) {
        if(actor==null)return null;
        try {
            Class<?> player=Class.forName("net.minecraft.server.level.ServerPlayer",false,actor.getClass().getClassLoader());
            if(!player.isInstance(actor))return null;
            Object selected=NativeLandmarkCapture.call(actor,"getItemBySlot",slot);
            if(stack==null)stack=selected;
            Object item=NativeLandmarkCapture.call(stack,"getItem");
            if(selected!=stack || NativeToolAgent.identity(item,true)==null)return null;
            Object level=NativeLandmarkCapture.call(actor,"level"),server=NativeLandmarkCapture.call(level,"getServer");
            if(!(boolean)NativeLandmarkCapture.call(server,"isSameThread"))throw new IllegalStateException("Combat invocation must belong to server thread");
            return new NativeToolCombatInvocation(actor,slot,stack,item,report);
        } catch(ClassNotFoundException failure){throw new IllegalStateException("Pinned native combat identity unavailable",failure);}
        catch(ReflectiveOperationException failure){throw new IllegalStateException("Pinned native spear identity unavailable",failure);}
    }
    private NativeToolCombatInvocation(Object actor,Object slot,Object stack,Object item,boolean report) {
        this.actor=Objects.requireNonNull(actor);this.slot=Objects.requireNonNull(slot);this.stack=Objects.requireNonNull(stack);this.item=Objects.requireNonNull(item);
        previous=CURRENT.get();boolean sameActor=previous!=null && previous.actor==actor;
        root=sameActor?previous.root:this;this.report=report && (!sameActor || previous.report);CURRENT.set(this);
    }
    public static void record(Object actor,Runnable reporter,boolean report) {
        Objects.requireNonNull(reporter);var frame=CURRENT.get();
        if(frame!=null && frame.actor==actor){if(report && frame.report && frame.root.denial==null)frame.root.denial=reporter;}
        else if(report)reporter.run();
    }
    public static boolean scopedTargetAllowed(boolean original,Object actor,Object target) {
        if(!original)return false;var frame=CURRENT.get();
        if(frame==null || frame.actor!=actor || NativeLandmarkCapture.call(actor,"getItemBySlot",frame.slot)!=frame.stack
                || NativeLandmarkCapture.call(frame.stack,"getItem")!=frame.item)return true;
        return !NativeToolAgent.combatTargetDenied(actor,target);
    }
    public static void normal(NativeToolCombatInvocation frame){if(frame!=null)frame.reportDenial();}
    public static void finish(NativeToolCombatInvocation frame){if(frame!=null)frame.close();}
    /** Match try-with-resources: retain the identical original throwable and suppress close failure. */
    public static void failed(NativeToolCombatInvocation frame,Throwable original){
        if(frame!=null)try{frame.close();}catch(Throwable close){if(close!=original)original.addSuppressed(close);}
    }
    public void reportDenial(){
        if(Thread.currentThread()!=thread || closed)throw new IllegalStateException("Combat reporter outside invocation");
        if(root==this && report && !reported && denial!=null){reported=true;denial.run();}
    }
    @Override public void close(){
        if(Thread.currentThread()!=thread)throw new IllegalStateException("Combat invocation closed on another thread");
        if(closed)return;
        if(CURRENT.get()!=this)throw new IllegalStateException("Combat invocations must close in reverse order");
        if(previous==null)CURRENT.remove();else CURRENT.set(previous);closed=true;denial=null;
    }
}
