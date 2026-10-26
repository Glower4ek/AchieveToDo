package com.diskree.achievetodo.certification;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
public final class R4CurrentRunGuard {
 private R4CurrentRunGuard() {}
 public static boolean claim(String family) {
  try {
  if (!"S5_R4_CURRENT_REQUALIFICATION_V1".equals(System.getProperty("achievetodo.r4.profile"))) throw new IllegalStateException("Missing explicit R4 current authority");
  Path runtime=Path.of(System.getProperty("achievetodo.r4.runtime")).toAbsolutePath().normalize();
  if (!Files.isRegularFile(runtime.resolve("r3-launch-binding.json"))) throw new IllegalStateException("Missing native classpath receipt");
  Path marker=runtime.resolve("r4-once-"+family+".json");
  try { Files.writeString(marker,"{\"profile\":\"S5_R4_CURRENT_REQUALIFICATION_V1\",\"family\":\""+family+"\"}",StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW); }
  catch (FileAlreadyExistsException duplicate) { return true; }
  return false;
  } catch (java.io.IOException failure) { throw new IllegalStateException("Cannot claim R4 runtime", failure); }
 }
}
