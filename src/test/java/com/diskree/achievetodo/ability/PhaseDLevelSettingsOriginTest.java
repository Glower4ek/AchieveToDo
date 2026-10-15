package com.diskree.achievetodo.ability;

import com.diskree.achievetodo.injection.extension.main.LevelInfoExtension;
import com.diskree.achievetodo.injection.extension.main.LevelInfoExtension.ConfigurationIO;
import com.diskree.achievetodo.injection.extension.main.LevelInfoExtension.ConfigurationOrigin;
import com.diskree.achievetodo.injection.mixin.main.LevelInfoMixin;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.LevelSettings;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** Native descriptor assertions and production origin/IO tests; these alone do not certify applied Mixins. */
public final class PhaseDLevelSettingsOriginTest {
    @TempDir Path root;
    @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }

    @Test void ordinarySetterDoesNotInferFreshAndOriginIsTransient() {
        LevelInfoMixin settings=new LevelInfoMixin() {};
        assertEquals(ConfigurationOrigin.UNKNOWN,settings.achievetodo$getConfigurationOrigin());
        settings.achievetodo$setConfigName("chaos"); assertEquals(ConfigurationOrigin.UNKNOWN,settings.achievetodo$getConfigurationOrigin());
        settings.achievetodo$setConfigurationOrigin(ConfigurationOrigin.SAVED);settings.achievetodo$setConfigName("normal");
        assertEquals(ConfigurationOrigin.SAVED,settings.achievetodo$getConfigurationOrigin());
        settings.achievetodo$setConfigurationOrigin(ConfigurationOrigin.FRESH); assertEquals(ConfigurationOrigin.FRESH,settings.achievetodo$getConfigurationOrigin());
        assertThrows(NullPointerException.class,()->settings.achievetodo$setConfigurationOrigin(null));
    }

    @Test void freshSavedUnknownAndCustomMissingFileMatrix() throws Exception {
        for(ConfigurationOrigin origin:ConfigurationOrigin.values()) {
            for(String name:List.of("easy","normal","hard","chaos","custom")) {
                Path directory=root.resolve(origin+"_"+name); Files.createDirectory(directory);
                if(origin==ConfigurationOrigin.FRESH&&!name.equals("custom")) {
                    var map=ConfigurationIO.load(directory,name,Long.MIN_VALUE,origin);assertEquals(153,map.size());
                    Path target=directory.resolve(name+(name.equals("chaos")?"_"+Long.MIN_VALUE:"")+".toml");
                    assertTrue(Files.isRegularFile(target));byte[] published=Files.readAllBytes(target);
                    assertEquals(map,ConfigurationIO.load(directory,name,Long.MIN_VALUE,ConfigurationOrigin.SAVED));
                    assertArrayEquals(published,Files.readAllBytes(target));
                    if(name.equals("chaos")) assertNotEquals(Progressions.getNormalProgression(),map);
                } else {
                    assertThrows(RuntimeException.class,()->ConfigurationIO.load(directory,name,Long.MIN_VALUE,origin));
                    try(var entries=Files.list(directory)){assertEquals(0,entries.count());}
                }
                System.out.println("D-S01 ORIGIN missing "+origin+" "+name+" verified");
            }
        }
    }

    @Test void existingFileAuthoritativeForEveryOriginIncludingMalformed() throws Exception {
        Path target=root.resolve("chaos_5.toml");
        for(ConfigurationOrigin origin:ConfigurationOrigin.values()) {
            String text="version = 1\n[abilities]\nuse_copper_tools = 0\nequip_copper_armor = -1\njump = 117\n";
            Files.writeString(target,text); var map=ConfigurationIO.load(root,"chaos",5,origin);
            assertEquals(117,map.get(AbilityType.JUMP));assertEquals(0,map.get(AbilityType.USE_COPPER_TOOLS));assertEquals(-1,map.get(AbilityType.EQUIP_COPPER_ARMOR));assertEquals(text,Files.readString(target));
            Files.writeString(target,"malformed = [");assertThrows(RuntimeException.class,()->ConfigurationIO.load(root,"chaos",5,origin));assertEquals("malformed = [",Files.readString(target));
        }
    }

    private static ClassNode bytecode(String resource) throws Exception {
        try(var in=PhaseDLevelSettingsOriginTest.class.getResourceAsStream("/"+resource+".class")) {
            assertNotNull(in);ClassNode node=new ClassNode();new ClassReader(in).accept(node,0);return node;
        }
    }

    @Test void allSixNativeReconstructionDescriptorsAreExplicitlyInjected() throws Exception {
        ClassNode mixin=bytecode("com/diskree/achievetodo/injection/mixin/main/LevelInfoMixin");
        MethodNode handler=mixin.methods.stream().filter(m->m.name.equals("keepConfigNameOnRecreate")).findFirst().orElseThrow();
        AnnotationNode annotation=handler.visibleAnnotations.stream().filter(a->a.desc.endsWith("/ModifyReturnValue;")).findFirst().orElseThrow();
        @SuppressWarnings("unchecked") List<String> selectors=(List<String>)annotation.values.get(annotation.values.indexOf("method")+1);
        assertEquals(6,selectors.size());
        ClassNode nativeSettings=bytecode("net/minecraft/world/level/LevelSettings");
        for(String selector:selectors) {
            MethodNode nativeMethod=nativeSettings.methods.stream().filter(m->(m.name+m.desc).equals(selector)).findFirst().orElseThrow();
            long constructors=0,returns=0;
            for(AbstractInsnNode i:nativeMethod.instructions) {
                if(i.getOpcode()==Opcodes.ARETURN)returns++;
                if(i instanceof MethodInsnNode call && call.owner.equals("net/minecraft/world/level/LevelSettings")&&call.name.equals("<init>"))constructors++;
            }
            assertEquals(1,constructors,selector);assertEquals(1,returns,selector);
            System.out.println("D-S01 NATIVE_DESCRIPTOR "+selector+" one_constructor_one_return=PASS runtime=NOT_CLAIMED");
        }
        Set<String> setterCalls=new HashSet<>();
        for(AbstractInsnNode i:handler.instructions) if(i instanceof MethodInsnNode call)setterCalls.add(call.name);
        assertTrue(setterCalls.contains("achievetodo$setConfigName"));assertTrue(setterCalls.contains("achievetodo$setConfigurationOrigin"));
        MethodNode parse=mixin.methods.stream().filter(m->m.name.equals("readConfigName")).findFirst().orElseThrow();
        assertTrue(Arrays.stream(parse.instructions.toArray()).anyMatch(i->i instanceof FieldInsnNode f&&f.name.equals("SAVED")));
    }

    @Test void actualFreshConstructorOwnershipAndClientReturnOrdinalAreBounded() throws Exception {
        ClassNode server=bytecode("net/minecraft/server/Main");
        MethodNode creation=server.methods.stream().filter(m->m.name.equals("createNewWorldData")).findFirst().orElseThrow();
        assertEquals("(Lnet/minecraft/server/dedicated/DedicatedServerSettings;Lnet/minecraft/server/WorldLoader$DataLoadContext;Lnet/minecraft/core/Registry;ZZ)Lnet/minecraft/server/WorldLoader$DataLoadOutput;",creation.desc);
        assertEquals(1,Arrays.stream(creation.instructions.toArray()).filter(i->i instanceof MethodInsnNode m&&m.owner.equals("net/minecraft/world/level/LevelSettings")&&m.name.equals("<init>")).count());
        ClassNode client=bytecode("net/minecraft/client/gui/screens/worldselection/CreateWorldScreen");
        MethodNode clientCreation=client.methods.stream().filter(m->m.name.equals("createLevelSettings")).findFirst().orElseThrow();
        assertEquals("(Z)Lnet/minecraft/world/level/LevelSettings;",clientCreation.desc);
        assertEquals(2,Arrays.stream(clientCreation.instructions.toArray()).filter(i->i.getOpcode()==Opcodes.ARETURN).count());
        for(String resource:List.of("main/MainMixin","client/CreateWorldScreenMixin")) {
            ClassNode source=bytecode("com/diskree/achievetodo/injection/mixin/"+resource);
            long fresh=source.methods.stream().flatMap(m->Arrays.stream(m.instructions.toArray())).filter(i->i instanceof FieldInsnNode f&&f.name.equals("FRESH")).count();
            assertEquals(1,fresh,resource);
        }
    }

    /** Separate Fabric JVM probe: applies real Mixins, runs native parse and all six copy routes, launches no world/GUI. */
    public static void main(String[] args) throws Exception {
        // Gradle may launch through a Windows pathing JAR; Knot's inventory needs the resolved Gradle classpath.
        System.setProperty("java.class.path", Files.readString(Path.of(System.getProperty("phaseD.probe.classpath"))));
        Files.createDirectories(Path.of(args[1]));
        net.fabricmc.api.EnvType side=net.fabricmc.api.EnvType.valueOf(args[0]);
        net.fabricmc.loader.impl.launch.knot.Knot knot=new net.fabricmc.loader.impl.launch.knot.Knot(side);
        ClassLoader loader=knot.init(side==net.fabricmc.api.EnvType.CLIENT
            ?new String[]{"--gameDir",args[1],"--version","26.2","--accessToken","synthetic","--username","D_S01_probe"}
            :new String[]{"--universe",args[1],"nogui"});
        Class.forName("net.minecraft.SharedConstants",true,loader).getMethod("tryDetectVersion").invoke(null);
        Class.forName("net.minecraft.server.Bootstrap",true,loader).getMethod("bootStrap").invoke(null);
        Class<?> settings=Class.forName("net.minecraft.world.level.LevelSettings",true,loader);
        Class<?> extension=Class.forName("com.diskree.achievetodo.injection.extension.main.LevelInfoExtension",true,loader);
        Class<?> origin=Class.forName(extension.getName()+"$ConfigurationOrigin",true,loader);
        Class<?> gameType=Class.forName("net.minecraft.world.level.GameType",true,loader);
        Class<?> difficulty=Class.forName("net.minecraft.world.Difficulty",true,loader);
        Class<?> difficultySettings=Class.forName(settings.getName()+"$DifficultySettings",true,loader);
        Class<?> data=Class.forName("net.minecraft.world.level.WorldDataConfiguration",true,loader);
        Object config=data.getField("DEFAULT").get(null);
        Object normal=difficulty.getField("NORMAL").get(null);
        Object survival=gameType.getField("SURVIVAL").get(null);
        Object ds=difficultySettings.getConstructor(difficulty,boolean.class,boolean.class).newInstance(normal,false,false);
        Object original=settings.getConstructor(String.class,gameType,difficultySettings,boolean.class,data).newInstance("synthetic",survival,ds,false,config);
        require(extension.isInstance(original),"LevelSettings Mixin not applied");
        var getOrigin=extension.getMethod("achievetodo$getConfigurationOrigin");
        var getName=extension.getMethod("achievetodo$getConfigName");
        var setName=extension.getMethod("achievetodo$setConfigName",String.class);
        var setOrigin=extension.getMethod("achievetodo$setConfigurationOrigin",origin);
        require(getOrigin.invoke(original).toString().equals("UNKNOWN"),"Native constructor UNKNOWN");
        setName.invoke(original,"chaos");require(getOrigin.invoke(original).toString().equals("UNKNOWN"),"Ordinary setter origin-neutral");
        List<java.lang.reflect.Method> routes=List.of(settings.getMethod("withGameType",gameType),settings.getMethod("withAllowCommands",boolean.class),
            settings.getMethod("withDifficulty",difficulty),settings.getMethod("withDifficultyLock",boolean.class),settings.getMethod("withDataConfiguration",data),settings.getMethod("copy"));
        Object[][] arguments={{survival},{true},{normal},{true},{config},{}};
        for(String state:List.of("UNKNOWN","SAVED","FRESH")) {
            Object value=Arrays.stream(origin.getEnumConstants()).filter(o->o.toString().equals(state)).findFirst().orElseThrow();
            setOrigin.invoke(original,value);
            for(int i=0;i<routes.size();i++) {
                Object copy=routes.get(i).invoke(original,arguments[i]);
                require(copy!=original,"Native reconstructed object");require(getName.invoke(copy).equals("chaos"),"Copied selected name");
                require(getOrigin.invoke(copy).equals(value),"Copied origin "+routes.get(i).getName());
                System.out.println("D-S01 APPLIED_NATIVE_COPY "+side+" "+state+" "+routes.get(i).getName()+" PASS");
            }
        }
        Class<?> ops=Class.forName("com.mojang.serialization.DynamicOps",true,loader);
        Class<?> dynamic=Class.forName("com.mojang.serialization.Dynamic",true,loader);
        Object jsonOps=Class.forName("com.mojang.serialization.JsonOps",true,loader).getField("INSTANCE").get(null);
        Object json=Class.forName("com.google.gson.JsonParser",true,loader).getMethod("parseString",String.class).invoke(null,
            "{\"LevelName\":\"synthetic\",\"GameType\":0,\"Difficulty\":2,\"allowCommands\":false,\"achievetodo_configName\":\"chaos\"}");
        Object saved=settings.getMethod("parse",dynamic,data).invoke(null,dynamic.getConstructor(ops,Object.class).newInstance(jsonOps,json),config);
        require(getName.invoke(saved).equals("chaos"),"Native parse selected name");require(getOrigin.invoke(saved).toString().equals("SAVED"),"Native parse SAVED");
        Class.forName("net.minecraft.server.Main",false,loader);
        if(side==net.fabricmc.api.EnvType.CLIENT) Class.forName("net.minecraft.client.gui.screens.worldselection.CreateWorldScreen",false,loader);
        System.out.println("D-S01 FABRIC_ORIGIN_PROBE "+side+" parse_and_18_copies=PASS constructor_injections=APPLIED world_startup=NOT_RUN GUI=NOT_RUN");
    }

    private static void require(boolean condition,String message) { if(!condition)throw new IllegalStateException(message); }
}
