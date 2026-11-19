package com.diskree.achievetodo.certification;
import io.netty.channel.embedded.EmbeddedChannel;import net.minecraft.network.Connection;import net.minecraft.network.protocol.PacketFlow;import net.minecraft.SharedConstants;import net.minecraft.server.Bootstrap;import java.nio.file.*;
public final class R3CleanupControls {
 static int rejected=0;
 static void reject(Runnable f){try{f.run();throw new AssertionError("negative accepted");}catch(IllegalStateException expected){rejected++;System.out.println("R3_CLEANUP_NEGATIVE "+expected.getMessage());}}
 public static void main(String[] args)throws Exception{
  SharedConstants.tryDetectVersion();Bootstrap.bootStrap();
  var good=new Connection(PacketFlow.SERVERBOUND);var open=new EmbeddedChannel(good);R3NativeCleanup.register(good,open);open.writeOutbound(new Object());R3NativeCleanup.settle(good);open.finishAndReleaseAll();
  var missing=new Connection(PacketFlow.SERVERBOUND);reject(()->R3NativeCleanup.settle(missing));
  var closed=new Connection(PacketFlow.SERVERBOUND);var shut=new EmbeddedChannel(closed);R3NativeCleanup.register(closed,shut);shut.close();reject(()->R3NativeCleanup.settle(closed));shut.finishAndReleaseAll();
  var excessive=new Connection(PacketFlow.SERVERBOUND);var flood=new EmbeddedChannel(excessive);R3NativeCleanup.register(excessive,flood);for(int i=0;i<4097;i++)flood.writeOutbound(new Object());reject(()->R3NativeCleanup.settle(excessive));flood.finishAndReleaseAll();
  if(rejected!=3)throw new AssertionError("control count");
  Files.writeString(Path.of(args[0]),"{\"positive\":1,\"negativeControls\":3,\"allRejected\":true}");
 }
}