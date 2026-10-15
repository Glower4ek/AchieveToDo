package com.diskree.achievetodo.ability;

import com.diskree.achievetodo.injection.extension.main.LevelInfoExtension.ConfigurationIO;
import com.diskree.achievetodo.injection.extension.main.LevelInfoExtension.ConfigurationOrigin;
import com.moandjiezana.toml.Toml;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import static org.junit.jupiter.api.Assertions.*;

/** Executes production IO with actual toml4j and disposable files; injected checkpoints are not physical power-loss tests. */
public final class PhaseDChaosConfigPersistenceTest {
    @TempDir Path root;
    @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }
    private Path chaos() { return root.resolve("chaos_0.toml"); }
    private void write(String text) throws IOException { Files.writeString(chaos(), text); }
    private Map<AbilityType,Integer> load() { return ConfigurationIO.load(root,"chaos",0,ConfigurationOrigin.SAVED); }
    private static final BiConsumer<Path,Path> ATOMIC = (from,to) -> {
        try { Files.move(from,to,StandardCopyOption.ATOMIC_MOVE); }
        catch(IOException e) { throw new UncheckedIOException(e); }
    };
    @SuppressWarnings("unchecked")
    private Map<AbilityType,Integer> fault(Consumer<String> checkpoint, BiConsumer<Path,Path> publisher) throws Exception {
        try { return (Map<AbilityType,Integer>) PhaseDChaosSamplerTest.invoke("load",
            new Class<?>[]{Path.class,String.class,long.class,ConfigurationOrigin.class,Consumer.class,BiConsumer.class},
            root,"chaos",0L,ConfigurationOrigin.SAVED,checkpoint,publisher); }
        catch (UncheckedIOException e) { throw e.getCause(); }
    }
    private void noTemps() throws IOException {
        try(var paths=Files.list(root)) { assertTrue(paths.noneMatch(p -> p.getFileName().toString().endsWith(".tmp"))); }
    }

    @Test void noOneBothCopperAndRepeatedStartupPreserveAllOldRawAndEffectiveValues() throws Exception {
        for(int mask=0;mask<4;mask++) {
            StringBuilder original=new StringBuilder("# preserved α\nversion = 7\nseed = -5\n[abilities]\n");
            Map<AbilityType,Integer> expected=new EnumMap<>(AbilityType.class);
            for(AbilityType a:AbilityType.values()) if(a.ordinal()<151) {
                int value=switch(a.ordinal()%5) { case 0 -> 0; case 1 -> -1; case 2 -> Integer.MAX_VALUE; case 3 -> Integer.MIN_VALUE; default -> 73; };
                original.append(a.getName()).append(" = ").append(value).append(" # old\n");
                expected.put(a,Math.min(value,1152));
            }
            if((mask&1)!=0) original.append("USE_COPPER_TOOLS = 0 # explicit\n");
            if((mask&2)!=0) original.append("equip_copper_armor = -1\n");
            original.append("unknown = 'literal # retained'\n[metadata.foo]\nvalue = \"escaped \\\"# string\"\nflag = true\n");
            write(original.toString()); byte[] before=Files.readAllBytes(chaos()); Toml old=new Toml().read(chaos().toFile());
            var map=load(); assertEquals(153,map.size()); expected.forEach((a,v)->assertEquals(v,map.get(a)));
            assertEquals((mask&1)!=0?0:595,map.get(AbilityType.USE_COPPER_TOOLS));
            assertEquals((mask&2)!=0?-1:105,map.get(AbilityType.EQUIP_COPPER_ARMOR));
            var after=new Toml().read(chaos().toFile()).toMap(); var afterAbilities=new HashMap<>(new Toml().read(chaos().toFile()).getTable("abilities").toMap());
            if((mask&1)==0) afterAbilities.remove("use_copper_tools");
            if((mask&2)==0) afterAbilities.remove("equip_copper_armor");
            after.put("abilities",afterAbilities); assertEquals(old.toMap(),after);
            byte[] published=Files.readAllBytes(chaos()); assertEquals(map,load()); assertArrayEquals(published,Files.readAllBytes(chaos()));
            if(mask==3) assertArrayEquals(before,published);
            else {
                String added=((mask&1)==0?"use_copper_tools = 595\n":"")+((mask&2)==0?"equip_copper_armor = 105\n":"");
                assertArrayEquals(before,new String(published,java.nio.charset.StandardCharsets.UTF_8).replace(added,"").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            noTemps(); System.out.println("D-S01 TOML old151_raw_effective mask="+mask+" repeated=PASS");
        }
    }

    @Test void emptyPartialUniformNewlinesAndNoFinalNewline() throws Exception {
        for(String newline:List.of("\n","\r\n")) for(boolean finalNewline:List.of(false,true)) {
            String text="version = 1"+newline+"[abilities]"+newline+"jump = -1 # retained"+(finalNewline?newline:"");
            write(text); var map=load(); assertEquals(-1,map.get(AbilityType.JUMP)); assertEquals(0,map.get(AbilityType.VISION));
            String actual=Files.readString(chaos()); assertTrue(actual.startsWith(text));
            assertEquals(text+(finalNewline?"":newline)+"use_copper_tools = 595"+newline+"equip_copper_armor = 105"+newline,actual);
        }
        write("version = 1\n[abilities]\n"); assertEquals(595,load().get(AbilityType.USE_COPPER_TOOLS));
    }

    @Test void oneKeyFirstEqualsBothWithoutRelocatingExplicitLines() throws Exception {
        write("version = 1\n[abilities]\n"); var both=load();
        write("version = 1\n[abilities]\nequip_copper_armor = 105 # first\n");
        assertEquals(both,load()); assertTrue(Files.readString(chaos()).contains("equip_copper_armor = 105 # first\nuse_copper_tools = 595\n"));
        write("version = 1\n[abilities]\nuse_copper_tools = 595 # first\n"); assertEquals(both,load());
    }

    @Test void recognizedInvalidTypesOverflowVersionAndAbsentTableRefuseWithoutMutation() throws Exception {
        List<String> invalid=new ArrayList<>(List.of("", "version = 1\n", "[abilities]\n", "version = 0\n[abilities]\n", "version = '1'\n[abilities]\n", "version = 1\n[abilities]\njump = [1,2]\n", "version = 1\n[abilities]\njump = {value=1}\n"));
        for(String key:List.of("jump","use_copper_tools","equip_copper_armor"))
            for(String value:List.of("2147483648","-2147483649","9223372036854775808","'5'","1.0","true","[1]")) invalid.add("version = 1\n[abilities]\n"+key+" = "+value+"\n");
        for(String text:invalid) {
            write(text); byte[] bytes=Files.readAllBytes(chaos()); assertThrows(RuntimeException.class,this::load,text); assertArrayEquals(bytes,Files.readAllBytes(chaos())); noTemps();
        }
        System.out.println("D-S01 TOML invalid_value_version_table cases="+invalid.size()+" PASS");
    }

    @Test void unsupportedInsertionLayoutsAndAliasesRefuse() throws Exception {
        List<String> invalid=List.of(
            "version = 1\n[abilities]\n\"use_copper_tools\" = 0\n",
            "version = 1\n[abilities]\n'use_copper_tools' = -1\n",
            "version = 1\n[abilities]\njump = 0\nJUMP = 1\n",
            "version = 1\n[abilities]\nuse_copper_tools = 0\nUSE_COPPER_TOOLS = 1\n",
            "version = 1\n[abilities]\nunknown = [1,2]\n",
            "version = 1\n[abilities]\nunknown = {x=1}\n",
            "version = 1\n[abilities]\n[abilities.other]\nx = 1\n",
            "version = 1\n[abilities]\n[\"other\"]\nx = 1\n",
            "version = 1\n[abilities]\nunknown.key = 1\n",
            "version = 1\n[abilities]\nunknown = \"\"\"multiline\ntext\"\"\"\n",
            "\uFEFFversion = 1\n[abilities]\n",
            "version = 1\r\n[abilities]\nx = 1\r\n");
        for(String text:invalid) {
            write(text); byte[] bytes=Files.readAllBytes(chaos()); assertThrows(RuntimeException.class,this::load,text); assertArrayEquals(bytes,Files.readAllBytes(chaos())); noTemps();
        }
        System.out.println("D-S01 TOML unsupported_or_alias cases="+invalid.size()+" PASS");
    }

    @Test void noInsertionRetainsParserSupportedComplexSyntaxAndNoSidecar() throws Exception {
        write("version = 9\n[abilities]\nuse_copper_tools = 0\nequip_copper_armor = -1\nunknown = [1,2]\n");
        byte[] bytes=Files.readAllBytes(chaos()); load(); assertArrayEquals(bytes,Files.readAllBytes(chaos()));
        assertFalse(Files.exists(root.resolve(".achievetodo-configuration.lock")));
    }

    @Test void customIndividuallyMissingCopperIsMinusOneAndDiskReadOnly() throws Exception {
        Path custom=root.resolve("custom.toml");
        for(String copper:List.of("", "use_copper_tools = 0\n", "equip_copper_armor = -1\n", "\"use_copper_tools\" = 0\n")) {
            Files.writeString(custom,"[abilities]\njump = 17\n"+copper); byte[] bytes=Files.readAllBytes(custom);
            var map=ConfigurationIO.load(root,"custom",99,ConfigurationOrigin.FRESH);
            assertEquals(copper.startsWith("use_copper_tools")?0:-1,map.get(AbilityType.USE_COPPER_TOOLS));
            assertEquals(-1,map.get(AbilityType.EQUIP_COPPER_ARMOR)); assertEquals(17,map.get(AbilityType.JUMP)); assertEquals(0,map.get(AbilityType.VISION));
            assertArrayEquals(bytes,Files.readAllBytes(custom));
        }
        assertFalse(Files.exists(root.resolve(".achievetodo-configuration.lock")));
        Files.writeString(custom,"[abilities]\nuse_copper_tools = false\n"); assertThrows(RuntimeException.class,()->ConfigurationIO.load(root,"custom",0,ConfigurationOrigin.FRESH));
    }

    @Test void fixedBuiltinsOnlyBackfillMissingCopperWithoutChangingOverrides() throws Exception {
        int index=0;
        for(String name:List.of("easy","normal","hard")) {
            Path target=root.resolve(name+".toml"); String text="version = 1\n[abilities]\njump = -1\nuse_copper_tools = 0\n"; Files.writeString(target,text);
            var map=ConfigurationIO.load(root,name,0,ConfigurationOrigin.UNKNOWN);
            assertEquals(0,map.get(AbilityType.USE_COPPER_TOOLS)); assertEquals(new int[]{68,83,98}[index++],map.get(AbilityType.EQUIP_COPPER_ARMOR)); assertEquals(-1,map.get(AbilityType.JUMP));
            assertTrue(Files.readString(target).startsWith(text));
        }
    }

    @Test void interruptedStagesDoNotPublishAndOnlyOwnedTempsAreCleaned() throws Exception {
        for(String stage:List.of("TEMP_CREATED","WRITTEN","FORCED","TEMP_VALIDATED","BEFORE_MOVE")) {
            write("version = 1\n[abilities]\njump = -1\n"); byte[] before=Files.readAllBytes(chaos());
            Path unrelated=root.resolve("unrelated.tmp"); Files.writeString(unrelated,"keep");
            assertThrows(IOException.class,()->fault(s->{if(s.equals(stage)) throw new UncheckedIOException(new IOException("injected "+stage));},ATOMIC));
            assertArrayEquals(before,Files.readAllBytes(chaos())); assertEquals("keep",Files.readString(unrelated)); Files.delete(unrelated); noTemps();
            assertTrue(Files.exists(root.resolve(".achievetodo-configuration.lock"))); System.out.println("D-S01 INJECTED_CHECKPOINT "+stage+" original_unchanged=PASS");
        }
    }

    @Test void unsupportedAtomicReplacementFailsWithNoFallback() throws Exception {
        write("version = 1\n[abilities]\n"); byte[] before=Files.readAllBytes(chaos());
        assertThrows(IOException.class,()->fault(s->{},(from,to)->{throw new UncheckedIOException(new AtomicMoveNotSupportedException(from.toString(),to.toString(),"injected provider refusal"));}));
        assertArrayEquals(before,Files.readAllBytes(chaos())); noTemps();
    }

    @Test void observedInterveningEditIsNotOverwritten() throws Exception {
        write("version = 1\n[abilities]\n"); String changed="version = 1\n[abilities]\njump = 47\n";
        assertThrows(IOException.class,()->fault(s->{if(s.equals("TEMP_VALIDATED")) try{write(changed);}catch(IOException e){throw new UncheckedIOException(e);}},ATOMIC));
        assertEquals(changed,Files.readString(chaos())); noTemps();
    }

    @Test void postPublicationFailureReturnsNoMapAndRestartReadsCompleteCandidate() throws Exception {
        write("version = 1\n[abilities]\njump = -1\n");
        assertThrows(IOException.class,()->fault(s->{if(s.equals("AFTER_MOVE")) throw new UncheckedIOException(new IOException("injected post-publication stop"));},ATOMIC));
        assertEquals(595,new Toml().read(chaos().toFile()).getLong("abilities.use_copper_tools"));
        assertEquals(-1,load().get(AbilityType.JUMP)); assertEquals(105,load().get(AbilityType.EQUIP_COPPER_ARMOR)); noTemps();
    }

    @Test void actualThreadContentionUsesStableMutexAndSidecar() throws Exception {
        write("version = 1\n[abilities]\n"); CountDownLatch locked=new CountDownLatch(1), release=new CountDownLatch(1);
        ExecutorService worker=Executors.newSingleThreadExecutor();
        try {
            Future<?> first=worker.submit(()->{try{fault(s->{if(s.equals("LOCKED")){locked.countDown();try{if(!release.await(15,TimeUnit.SECONDS))throw new IllegalStateException("test timeout");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}}},ATOMIC);}catch(Exception e){throw new RuntimeException(e);}});
            assertTrue(locked.await(15,TimeUnit.SECONDS)); assertThrows(RuntimeException.class,this::load); release.countDown(); first.get(15,TimeUnit.SECONDS);
            byte[] published=Files.readAllBytes(chaos()); load(); assertArrayEquals(published,Files.readAllBytes(chaos()));
        } finally {release.countDown();worker.shutdownNow();}
    }

    // Child JVM has only JDK dependencies on its main path; executes an actual cross-process sidecar lock.
    public static void main(String[] args) throws Exception {
        try(FileChannel c=FileChannel.open(Path.of(args[0]),StandardOpenOption.CREATE,StandardOpenOption.WRITE);FileLock lock=c.lock()) {
            Files.writeString(Path.of(args[1]),"locked"); System.in.read();
        }
    }
    @Test void actualCooperatingProcessLockContention() throws Exception {
        write("version = 1\n[abilities]\n"); Path ready=root.resolve("child.ready");
        String java=Path.of(System.getProperty("java.home"),"bin","java.exe").toString();
        String classes=Path.of(getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        Process child=new ProcessBuilder(java,"-Xmx32m","-XX:+UseSerialGC","-XX:ActiveProcessorCount=1","-cp",classes,getClass().getName(),root.resolve(".achievetodo-configuration.lock").toString(),ready.toString()).redirectErrorStream(true).redirectOutput(root.resolve("child.log").toFile()).start();
        try {
            long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(15);
            while(!Files.exists(ready)&&child.isAlive()&&System.nanoTime()<deadline) Thread.sleep(20);
            assertTrue(Files.exists(ready),()->{try{return Files.readString(root.resolve("child.log"));}catch(IOException e){return e.toString();}});
            byte[] before=Files.readAllBytes(chaos()); assertThrows(RuntimeException.class,this::load); assertArrayEquals(before,Files.readAllBytes(chaos()));
        } finally { child.getOutputStream().write(10);child.getOutputStream().flush();if(!child.waitFor(15,TimeUnit.SECONDS))child.destroyForcibly(); }
        assertEquals(0,child.exitValue()); assertEquals(595,load().get(AbilityType.USE_COPPER_TOOLS)); noTemps();
    }

    @Test void unsafeTargetAndSelectedNameRefuseWithoutMutation() throws Exception {
        Files.createDirectory(chaos()); assertThrows(RuntimeException.class,this::load); Files.delete(chaos());
        for(String name:List.of("","../normal","foo/bar","foo\\bar","normal.","normal ")) assertThrows(RuntimeException.class,()->ConfigurationIO.load(root,name,0,ConfigurationOrigin.FRESH));
        noTemps();
    }

    @Test void actualHardlinkAliasRefusesWithoutChangingEitherName() throws Exception {
        write("version = 1\n[abilities]\n"); Path alias=root.resolve("alias.toml");Files.createLink(alias,chaos());
        byte[] original=Files.readAllBytes(chaos());assertTrue(Files.isSameFile(alias,chaos()));
        assertThrows(RuntimeException.class,this::load);assertArrayEquals(original,Files.readAllBytes(chaos()));assertArrayEquals(original,Files.readAllBytes(alias));noTemps();
    }

    @Test void actualThreadInterruptBeforePublicationPreservesOriginal() throws Exception {
        write("version = 1\n[abilities]\n");byte[] original=Files.readAllBytes(chaos());
        try {
            assertThrows(IOException.class,()->fault(s->{if(s.equals("WRITTEN"))Thread.currentThread().interrupt();},ATOMIC));
        } finally {Thread.interrupted();}
        assertArrayEquals(original,Files.readAllBytes(chaos()));noTemps();
    }

    @Test void actualWindowsExclusiveHandleCannotBecomeFreshRegeneration() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(System.getProperty("os.name").startsWith("Windows"));
        write("version = 1\n[abilities]\n");byte[] original=Files.readAllBytes(chaos());
        com.sun.jna.platform.win32.WinNT.HANDLE handle=com.sun.jna.platform.win32.Kernel32.INSTANCE.CreateFile(chaos().toString(),
            com.sun.jna.platform.win32.WinNT.FILE_READ_ATTRIBUTES,0,null,com.sun.jna.platform.win32.WinNT.OPEN_EXISTING,0,null);
        assertNotEquals(com.sun.jna.platform.win32.WinBase.INVALID_HANDLE_VALUE,handle);
        try {assertThrows(RuntimeException.class,()->ConfigurationIO.load(root,"chaos",0,ConfigurationOrigin.FRESH));}
        finally {assertTrue(com.sun.jna.platform.win32.Kernel32.INSTANCE.CloseHandle(handle));}
        assertArrayEquals(original,Files.readAllBytes(chaos()));noTemps();
    }
}
