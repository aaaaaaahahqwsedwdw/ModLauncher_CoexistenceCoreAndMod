import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.module.ResolvedModule;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URLDecoder;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mx_wj.CommonSword.coremod.CommonSwordService;

import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.ModuleLayerHandler;
import cpw.mods.modlauncher.api.IModuleLayerManager.Layer;
import cpw.mods.modlauncher.api.NamedPath;
import net.minecraftforge.fml.loading.ModDirTransformerDiscoverer;
import sun.misc.Unsafe;

public class Helper {
    private static Unsafe UNSAFE;
    private static Lookup lookup;
    private static Object internalUNSAFE;

    static{
        UNSAFE = getUnsafe();
        lookup = getFieldValue(MethodHandles.Lookup.class, "IMPL_LOOKUP", Lookup.class);
        internalUNSAFE = getInternalUNSAFE();
        try {
            Class<?> internalUNSAFEClass = lookup.findClass("jdk.internal.misc.Unsafe");
            objectFieldOffsetInternal = lookup.findVirtual(internalUNSAFEClass, "objectFieldOffset", MethodType.methodType(long.class, Field.class)).bindTo(internalUNSAFE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Unsafe getUnsafe() {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            return (Unsafe) theUnsafe.get(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Object getInternalUNSAFE() {
        try {
            Class<?> clazz = lookup.findClass("jdk.internal.misc.Unsafe");
            return lookup.findStatic(clazz, "getUnsafe", MethodType.methodType(clazz)).invoke();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Field getField(Class<?> clazz, String name){
        try {
            return clazz.getDeclaredField(name);
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }

    @SuppressWarnings("all")
    public static <T> T getFieldValue(Field f, Object target, Class<T> clazz) {
        try {
            long offset = 0L;
            if(Modifier.isStatic(f.getModifiers())){
                target = UNSAFE.staticFieldBase(f);
                offset = UNSAFE.staticFieldOffset(f);
            } else{
                offset = objectFieldOffset(f);
            }
            return (T)UNSAFE.getObject(target, offset);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    private static MethodHandle objectFieldOffsetInternal;

    public static long objectFieldOffset(Field f){
        try {
            return UNSAFE.objectFieldOffset(f);
        } catch (Throwable e) {
            try {
                return (long)objectFieldOffsetInternal.invoke(f);
            } catch (Throwable t1) {
                t1.printStackTrace();
            }
        }
        return 0L;
    }

    public static <T> T getFieldValue(Object target, String fieldName, Class<T> clazz) {
        try {
            return getFieldValue(target.getClass().getDeclaredField(fieldName), target, clazz);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    public static <T> T getFieldValue(Class<?> target, String fieldName, Class<T> clazz) {
        try {
            return getFieldValue(target.getDeclaredField(fieldName), (Object)null, clazz);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void setFieldValue(Object target, String fieldName, Object value) {
        try {
            setFieldValue(target.getClass().getDeclaredField(fieldName), target, value);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void setFieldValue(Field f, Object target, Object value) {
        try {
            long offset = 0L;
            if(Modifier.isStatic(f.getModifiers())){
                target = UNSAFE.staticFieldBase(f);
                offset = UNSAFE.staticFieldOffset(f);
            } else{
                offset = objectFieldOffset(f);
            }
            UNSAFE.putObject(target, offset, value);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static String getJarPath(Class<?> clazz) throws Exception{
        String file = clazz.getProtectionDomain().getCodeSource().getLocation().getPath();
        if(!file.isEmpty()){
            if (file.startsWith("union:"))
                file = file.substring(6); 
            if (file.startsWith("/"))
                file = file.substring(1); 
            file = file.substring(0, file.lastIndexOf(".jar") + 4);
            file = file.replaceAll("/", "\\\\");
        }
        return URLDecoder.decode(file, "UTF-8");
    }

    //ModLauncher Start
    public static void coexistenceCoreAndMod() throws Throwable{
        String jar = Helper.getJarPath(Helper.class);
        String moduleName = Helper.class.getModule().getName();
        List<NamedPath> found = Helper.getFieldValue(ModDirTransformerDiscoverer.class, "found", List.class);
        Iterator<NamedPath> namedPathIterator = found.iterator();
        while (namedPathIterator.hasNext()) {
            NamedPath namedPath = namedPathIterator.next();
            if(jar.equals(namedPath.paths()[0].toString())){
                namedPathIterator.remove();
            }
        }
        ModuleLayerHandler moduleLayerHandler = Helper.getFieldValue(Launcher.INSTANCE, "moduleLayerHandler", ModuleLayerHandler.class);
        EnumMap<Layer, Object> completedLayers = Helper.getFieldValue(moduleLayerHandler, "completedLayers", EnumMap.class);
        Iterator<Object> completedLayersIterator = completedLayers.values().iterator();
        while(completedLayersIterator.hasNext()){
            Object layerInfo = completedLayersIterator.next();
            if(layerInfo != null){
                ModuleLayer layer = Helper.getFieldValue(layerInfo, "layer", ModuleLayer.class);
                Iterator<Module> modulesIterator = layer.modules().iterator();
                while(modulesIterator.hasNext()){
                    Module module = modulesIterator.next();
                    if(module.getName().equals(moduleName)){
                        Set<ResolvedModule> modules = new HashSet<>(Helper.getFieldValue(layer.configuration(), "modules", Set.class));
                        Map<String, ResolvedModule> nameToModule = new HashMap(Helper.getFieldValue(layer.configuration(), "nameToModule", Map.class));
                        modules.remove(nameToModule.remove(moduleName));
                        Helper.setFieldValue(layer.configuration(), "modules", modules);
                        Helper.setFieldValue(layer.configuration(), "nameToModule", nameToModule);
                    }
                }
            }
        }
    }
}
