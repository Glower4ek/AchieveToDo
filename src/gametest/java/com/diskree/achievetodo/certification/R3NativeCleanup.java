package com.diskree.achievetodo.certification;
import io.netty.channel.embedded.EmbeddedChannel;import io.netty.util.ReferenceCountUtil;import net.minecraft.network.Connection;import java.util.*;
final class R3NativeCleanup {
 private static final Map<Connection,EmbeddedChannel> channels=new IdentityHashMap<>();
 static void register(Connection c,EmbeddedChannel ch){if(channels.put(c,ch)!=null)throw new IllegalStateException("duplicate cleanup owner");}
 static void settle(Connection c){
  var ch=channels.remove(c);if(ch==null)throw new IllegalStateException("missing cleanup channel owner");
  if(!ch.isOpen())throw new IllegalStateException("channel closed before settlement");
  int released=0;
  for(int pass=0;pass<32;pass++){
   ch.runPendingTasks();ch.runScheduledPendingTasks();ch.flushOutbound();int count=0;Object packet;
   while((packet=ch.readOutbound())!=null){ReferenceCountUtil.release(packet);count++;if(++released>4096)throw new IllegalStateException("cleanup message bound");}
   if(count==0&&!ch.hasPendingTasks()){System.out.println("R3_CLEANUP_SETTLED messages="+released);return;}
  }
  throw new IllegalStateException("cleanup did not quiesce");
 }
}