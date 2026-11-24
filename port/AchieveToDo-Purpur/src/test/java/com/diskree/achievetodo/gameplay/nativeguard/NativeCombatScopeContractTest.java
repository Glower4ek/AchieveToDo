package com.diskree.achievetodo.gameplay.nativeguard;
import java.lang.reflect.*;
import java.util.concurrent.atomic.AtomicInteger;
/** Pure invocation ownership/reporter contracts; not native installation proof. */
public final class NativeCombatScopeContractTest {
 private static int checks;
 private static void check(boolean yes){if(!yes)throw new AssertionError("scope "+checks);checks++;}
 private static NativeToolCombatInvocation frame(Object actor,boolean report)throws Exception {
  var c=NativeToolCombatInvocation.class.getDeclaredConstructor(Object.class,Object.class,Object.class,Object.class,boolean.class);c.setAccessible(true);
  return c.newInstance(actor,new Object(),new Object(),new Object(),report);
 }
 private static void rejects(Runnable r){try{r.run();}catch(IllegalStateException expected){checks++;return;}throw new AssertionError("expected scope rejection");}
 public static void main(String[] args)throws Exception {
  Object a=new Object(),b=new Object();var calls=new AtomicInteger();
  NativeToolCombatInvocation.record(a,calls::incrementAndGet,true);check(calls.get()==1);
  var outer=frame(a,true);var inner=frame(a,true);
  NativeToolCombatInvocation.record(a,calls::incrementAndGet,true);
  NativeToolCombatInvocation.record(a,()->calls.addAndGet(100),true);
  inner.reportDenial();check(calls.get()==1);rejects(outer::close);
  inner.close();outer.reportDenial();outer.reportDenial();check(calls.get()==2);outer.close();outer.close();
  var silent=frame(a,false);var child=frame(a,true);NativeToolCombatInvocation.record(a,calls::incrementAndGet,true);child.close();silent.reportDenial();check(calls.get()==2);silent.close();
  var actorA=frame(a,false);var actorB=frame(b,true);NativeToolCombatInvocation.record(b,calls::incrementAndGet,true);actorB.reportDenial();check(calls.get()==3);actorB.close();actorA.close();
  var threadOwned=frame(a,true);var failure=new java.util.concurrent.atomic.AtomicReference<Throwable>();
  Thread t=new Thread(()->{try{threadOwned.close();}catch(Throwable x){failure.set(x);}});t.start();t.join();check(failure.get() instanceof IllegalStateException);threadOwned.close();
  var failedOuter=frame(a,true);var failedInner=frame(a,true);Throwable original=new RuntimeException("original");
  NativeToolCombatInvocation.failed(failedOuter,original);check(original.getSuppressed().length==1);failedInner.close();failedOuter.close();
  var throwing=frame(a,true);NativeToolCombatInvocation.record(a,()->{throw new IllegalStateException("reporter");},true);
  try{throwing.reportDenial();throw new AssertionError();}catch(IllegalStateException expected){NativeToolCombatInvocation.failed(throwing,expected);check(expected.getSuppressed().length==0);}
  NativeToolCombatInvocation.record(a,calls::incrementAndGet,true);check(calls.get()==4);
  check(!NativeToolCombatInvocation.scopedTargetAllowed(false,a,b));check(NativeToolCombatInvocation.scopedTargetAllowed(true,a,b));
  NativeToolCombatInvocation.normal(null);NativeToolCombatInvocation.finish(null);
  System.out.println("COMBAT_SCOPE_CONTRACT_PASS checks="+checks+"; native/runtime=NOT_PROVEN");
 }
}
