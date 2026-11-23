package com.diskree.achievetodo.gameplay.nativeguard;

import java.lang.classfile.*;
import java.lang.constant.*;
import java.lang.instrument.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiPredicate;

/** Reject unsupported source transactions before either staging or native resource/player commit.
 * This bootstrap class owns no advancement, reward, scoreboard or world state. */
public final class NativeSourceReloadAgent {
    static final String SERVER = "net/minecraft/server/MinecraftServer";
    static final String RELOAD = "(Ljava/util/Collection;Lio/papermc/paper/event/server/ServerResourcesReloadedEvent$Cause;)Ljava/util/concurrent/CompletableFuture;";
    static final String COMMIT = "(Ljava/util/Collection;Lio/papermc/paper/event/server/ServerResourcesReloadedEvent$Cause;Lnet/minecraft/server/MinecraftServer$ReloadableResources;)V";
    static final Map<String,String> INPUTS = Map.ofEntries(Map.entry("net/minecraft/server/MinecraftServer","f0d8df9f66b6d63cb18ff894e4b702f7640186001b992dab5d781a26fb5e8ba5"),
        Map.entry("net/minecraft/server/packs/FilePackResources","199dad7815afc1969e1db78984c016053cc8efed25309ce5aa9f5731349f83f9"),
        Map.entry("net/minecraft/server/packs/FilePackResources$SharedZipFileAccess","6db65be7cb5a2c462cb249cf0c96e516cc6b4f0ee3eed109e7c8f3aab69d9436"));
    private static final Set<String> verified = new HashSet<>();
    private static final Map<String,String> failures = new HashMap<>();
    private static final ClassDesc SELF=ClassDesc.of(NativeSourceReloadAgent.class.getName());
    private static final ClassDesc OBJECT=ConstantDescs.CD_Object;
    private static final ClassDesc COLLECTION=ClassDesc.of("java.util.Collection");
    private static final ClassDesc FUTURE=ClassDesc.of("java.util.concurrent.CompletableFuture");
    private static volatile BiPredicate<Object,Object> validator;
    private static Object owner;
    private static boolean started;
    private static int entryRefusals,commitRefusals;

    public static synchronized void initialize(Instrumentation instrumentation) {
        // Bootstrap class loader: only java.* and the nativeguard support classes are visible here, so the mode check
        // mirrors ReleaseLaunchDefaults.nativeMode instead of calling it.
        String mode=System.getProperty("achievetodo.gameplay.mode");
        if (!"release".equals(mode) && !"certification".equals(mode)) return;
        if(started)throw new IllegalStateException("ATD_DUPLICATE_SOURCE_RELOAD_AGENT");
        started=true;
        instrumentation.addTransformer(new ClassFileTransformer(){
            @Override public byte[] transform(ClassLoader loader,String name,Class<?> type,ProtectionDomain domain,byte[] bytes){
                if(!INPUTS.containsKey(name))return null;
                try {
                    verifyPin(name,bytes);
                    byte[] result=SERVER.equals(name)?transformNative(name,bytes,loader):null;
                    synchronized(NativeSourceReloadAgent.class){verified.add(name);}
                    // No early System.out: Purpur may redirect it before Bukkit.server exists.
                    return result;
                }catch(Throwable failure){synchronized(NativeSourceReloadAgent.class){failures.put(name,failure.toString());}return null;}
            }
        });
    }
    static void verifyPin(String name,byte[] bytes)throws Exception {
        String actual=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        if(!actual.equals(INPUTS.get(name)))throw new IllegalStateException("ATD_UNPINNED_SOURCE_RELOAD_INPUT: "+name+" "+actual);
    }
    public static synchronized void install(Object token,BiPredicate<Object,Object> callback,ClassLoader loader)throws Exception {
        if(!started || NativeSourceReloadAgent.class.getClassLoader()!=null
            || Class.forName(NativeSourceReloadAgent.class.getName(),false,loader)!=NativeSourceReloadAgent.class)
            throw new IllegalStateException("ATD_SOURCE_RELOAD_BOOTSTRAP_REQUIRED");
        if(owner!=null)throw new IllegalStateException("ATD_SOURCE_RELOAD_ALREADY_OWNED");
        for(String name:INPUTS.keySet())Class.forName(name.replace('/','.'),true,loader);
        if(!failures.isEmpty() || !verified.equals(INPUTS.keySet()))throw new IllegalStateException("ATD_SOURCE_RELOAD_PIN_OR_HOOK_FAILURE: "+failures+" verified="+verified);
        owner=Objects.requireNonNull(token);validator=Objects.requireNonNull(callback);
    }
    public static synchronized void retire(Object token){if(owner==token){validator=null;owner=null;}}
    public static synchronized boolean installed(){return started && owner!=null && validator!=null && failures.isEmpty() && verified.equals(INPUTS.keySet());}
    public static Collection<?> snapshot(Object packs){
        if(!(packs instanceof Collection<?> values))throw new IllegalArgumentException("ATD_INVALID_RELOAD_PACKS");
        return List.copyOf(values); // Native async work and commit share this immutable exact selection.
    }
    private static void validate(Object server,Object packs,Object candidate){
        var check=validator;
        if(check==null || !check.test(server,new Object[]{packs,candidate}))throw new IllegalStateException("ATD_SOURCE_RELOAD_VALIDATOR_UNAVAILABLE_OR_REJECTED");
    }
    public static CompletableFuture<?> rejection(Object server,Object packs){
        try{validate(server,packs,null);return null;}
        catch(RuntimeException | LinkageError failure){synchronized(NativeSourceReloadAgent.class){entryRefusals++;}
            return CompletableFuture.failedFuture(new IllegalStateException("ATD_SOURCE_RELOAD_REJECTED_BEFORE_STAGING",failure));}
    }
    public static void beforeCommit(Object server,Object packs,Object candidate){
        try{validate(server,packs,Objects.requireNonNull(candidate));}
        catch(RuntimeException | LinkageError failure){
            synchronized(NativeSourceReloadAgent.class){commitRefusals++;}
            // The candidate owns only its newly opened resources. Never close the active generation.
            if(candidate instanceof AutoCloseable staged)try{staged.close();}catch(Exception closeFailure){failure.addSuppressed(closeFailure);}
            throw new IllegalStateException("ATD_SOURCE_RELOAD_REJECTED_BEFORE_COMMIT",failure);
        }
    }
    public static synchronized Map<String,Integer> refusals(){return Map.of("entry",entryRefusals,"commit",commitRefusals);}
    static byte[] transformNative(String name,byte[] bytes,ClassLoader loader){
        if(!SERVER.equals(name))throw new IllegalArgumentException("Not the pinned source reload owner");
        int[] sites={0};
        var cf=ClassFile.of(ClassFile.ClassHierarchyResolverOption.of(ClassHierarchyResolver.ofResourceParsing(loader)));
        byte[] output=cf.transformClass(cf.parse(bytes),(cb,e)->{
            if(!(e instanceof MethodModel method)){cb.with(e);return;}
            boolean entry=method.methodName().equalsString("reloadResources") && method.methodType().equalsString(RELOAD);
            boolean commit=method.methodName().equalsString("lambda$reloadResources$4") && method.methodType().equalsString(COMMIT);
            if(!entry && !commit){cb.with(e);return;}
            cb.transformMethod(method,MethodTransform.transformingCode(new CodeTransform(){
                @Override public void atStart(CodeBuilder b){
                    sites[0]++;
                    if(entry){
                        b.aload(1).invokestatic(SELF,"snapshot",MethodTypeDesc.of(COLLECTION,OBJECT)).astore(1);
                        var allowed=b.newLabel();
                        b.aload(0).aload(1).invokestatic(SELF,"rejection",MethodTypeDesc.of(FUTURE,OBJECT,OBJECT))
                            .dup().ifnull(allowed).areturn().labelBinding(allowed).pop();
                    }else b.aload(0).aload(1).aload(3).invokestatic(SELF,"beforeCommit",MethodTypeDesc.of(ConstantDescs.CD_void,OBJECT,OBJECT,OBJECT));
                }
                @Override public void accept(CodeBuilder b,CodeElement element){b.with(element);}
            }));
        });
        if(sites[0]!=2)throw new IllegalStateException("ATD_SOURCE_RELOAD_EXACT_SITES="+sites[0]);
        return output;
    }
    private NativeSourceReloadAgent(){}
}
