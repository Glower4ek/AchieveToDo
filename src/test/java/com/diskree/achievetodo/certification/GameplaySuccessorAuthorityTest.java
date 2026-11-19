package com.diskree.achievetodo.certification;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
/** Full standard-suite source controls; historical runtime evidence remains historical. */
public final class GameplaySuccessorAuthorityTest {
 @Test void currentSourceAndTamperControls() throws Exception {
  String authority=System.getenv("ACHIEVETODO_GAMEPLAY_SOURCE_AUTHORITY");
  if (authority==null) return;
  assertTrue(authority.equals("ATD_PURPUR_GAMEPLAY_SOURCE_V1") || authority.equals("ATD_PURPUR_GAMEPLAY_SOURCE_V2") || authority.equals("ATD_PURPUR_GAMEPLAY_SOURCE_V3") || authority.equals("ATD_PURPUR_GAMEPLAY_SOURCE_V4") || authority.equals("ATD_PURPUR_GAMEPLAY_SOURCE_V5"),"Unknown gameplay authority");
  UiUxMaintenanceSourceContract.verifyRoot(Path.of("").toAbsolutePath());
  // V2's exact-current control also runs all four unchanged V1 controls on authenticated originals.
  if (authority.equals("ATD_PURPUR_GAMEPLAY_SOURCE_V4") || authority.equals("ATD_PURPUR_GAMEPLAY_SOURCE_V5")) System.err.println("SOURCE_IDENTITY_ONLY standalone JUnit historical_controls=NOT_RUN; fresh Gradle node required");
  String controls=UiUxMaintenanceSourceContract.stage1Selected() ? "tools.build.tests.test_stage1_test_source_v2" : authority.equals("ATD_PURPUR_GAMEPLAY_SOURCE_V5") ? "tools.build.tests.test_gameplay_successor_v5" : authority.equals("ATD_PURPUR_GAMEPLAY_SOURCE_V4") ? "tools.build.tests.test_gameplay_successor_v4" : authority.equals("ATD_PURPUR_GAMEPLAY_SOURCE_V3") ? "tools.build.tests.test_gameplay_successor_v3" : (authority.equals("ATD_PURPUR_GAMEPLAY_SOURCE_V2") ? "tools.build.tests.test_gameplay_successor_v2" : "tools.build.tests.test_gameplay_successor");
  var builder=new ProcessBuilder(System.getenv("ACHIEVETODO_PYTHON"),"-B","-X","utf8","-m","unittest",controls,"-v").redirectErrorStream(true);
  var process=builder.start();var output=new String(process.getInputStream().readAllBytes(),StandardCharsets.UTF_8);
  assertEquals(0,process.waitFor(),output);
 }
}
