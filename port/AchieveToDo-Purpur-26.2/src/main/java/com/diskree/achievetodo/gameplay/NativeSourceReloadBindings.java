package com.diskree.achievetodo.gameplay;

import com.diskree.achievetodo.datapack.BacapPacks;
import com.diskree.achievetodo.tracking.bukkit.BukkitBacapRegistry;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Stream;
import java.util.zip.ZipFile;
import org.bukkit.plugin.java.JavaPlugin;

/** Transaction source validation. No native progress reconstruction or reward suppression. */
final class NativeSourceReloadBindings implements AutoCloseable {
    private final Object token=new Object();
    private final Method retire;
    NativeSourceReloadBindings(JavaPlugin plugin){
        try{
            Class<?> nativeServer=Class.forName("net.minecraft.server.MinecraftServer");
            Class<?> agent=Class.forName("com.diskree.achievetodo.gameplay.nativeguard.NativeSourceReloadAgent",false,null);
            retire=agent.getMethod("retire",Object.class);
            BiPredicate<Object,Object> callback=(server,context)->{
                try{
                    Object[] parts=(Object[])context;
                    List<String> names=names(parts[0]);BacapPacks.validateEnabledPackSelection(names);
                    // Preflight validates the operator's current complete profile; this is not the
                    // staged identity check. The commit path below reads the actual already-open ZIPs.
                    var source=BacapPacks.validate(BacapPacks.directory());
                    if(parts[1]!=null){
                        Object candidate=parts[1];Object resources=access(candidate,"resourceManager");
                        @SuppressWarnings("unchecked") Stream<Object> stream=(Stream<Object>)resources.getClass().getMethod("listPacks").invoke(resources);
                        List<Object> packs;try(stream){packs=stream.toList();}
                        var opened=new LinkedHashMap<String,ZipFile>();var actualNames=new ArrayList<String>();
                        for(Object pack:packs){
                            String id=(String)pack.getClass().getMethod("packId").invoke(pack);actualNames.add(id);
                            if(id.equals("file/bacap.zip") || id.equals("file/bacap_override.zip")){
                                if(pack.getClass()!=Class.forName("net.minecraft.server.packs.FilePackResources",false,nativeServer.getClassLoader()))throw new IllegalStateException("Unknown staged BACAP pack class");
                                Object prefix=field(pack,"prefix");if(prefix!=null && !prefix.equals(""))throw new IllegalStateException("Unexpected staged BACAP overlay prefix");
                                Object shared=field(pack,"zipFileAccess");
                                if(shared.getClass()!=Class.forName("net.minecraft.server.packs.FilePackResources$SharedZipFileAccess",false,nativeServer.getClassLoader()))throw new IllegalStateException("Unknown staged ZIP access class");
                                Object zip=field(shared,"zipFile");if(!(zip instanceof ZipFile openedZip) || (boolean)field(shared,"failedToLoad"))throw new IllegalStateException("Staged BACAP ZIP not opened/available");
                                String key=id.substring("file/".length(),id.length()-".zip".length());
                                if(opened.put(key,openedZip)!=null)throw new IllegalStateException("Duplicate staged BACAP source");
                            }
                        }
                        BacapPacks.validateEnabledPackSelection(actualNames);
                        if(!actualNames.equals(names))throw new IllegalStateException("Staged resource order differs from immutable requested source");
                        source=BacapPacks.validateOpenedArchives(opened); // Borrowed handles; never reopen paths or close them.
                        Object managers=access(candidate,"managers");Object advancements=managers.getClass().getMethod("getAdvancements").invoke(managers);
                        BukkitBacapRegistry.validateCandidateAdvancements(advancements,source);
                    }
                    return true;
                }catch(Exception | LinkageError failure){throw new IllegalStateException("ATD_CANONICAL_RELOAD_SOURCE_REJECTED",failure);}
            };
            agent.getMethod("install",Object.class,BiPredicate.class,ClassLoader.class).invoke(null,token,callback,nativeServer.getClassLoader());
            plugin.getLogger().info("ATD_SOURCE_RELOAD_GUARD_INSTALLED twoArgEntry=true stagedOpenedZipValidation=true beforeNativeCommit=true");
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("ATD_SOURCE_RELOAD_GUARD_UNAVAILABLE",failure);}
    }
    private static List<String> names(Object value){
        if(!(value instanceof Collection<?> values))throw new IllegalArgumentException("Invalid reload selection");
        var result=new ArrayList<String>();for(Object id:values){if(!(id instanceof String name))throw new IllegalArgumentException("Non-string pack ID");result.add(name);}return List.copyOf(result);
    }
    private static Object access(Object value,String name)throws ReflectiveOperationException {
        Method method=value.getClass().getDeclaredMethod(name);method.setAccessible(true);return method.invoke(value);
    }
    private static Object field(Object value,String name)throws ReflectiveOperationException {
        Field field=value.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(value);
    }
    @Override public void close(){try{retire.invoke(null,token);}catch(ReflectiveOperationException failure){throw new IllegalStateException("Source reload guard retirement failed",failure);}}
}
