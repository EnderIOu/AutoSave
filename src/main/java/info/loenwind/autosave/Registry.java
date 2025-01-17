package info.loenwind.autosave;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import org.apache.commons.lang3.ArrayUtils;

import info.loenwind.autosave.annotations.Storable;
import info.loenwind.autosave.handlers.IHandler;
import info.loenwind.autosave.handlers.forge.HandleFluid;
import info.loenwind.autosave.handlers.forge.HandleFluidStack;
import info.loenwind.autosave.handlers.forge.HandleRegistryEntry;
import info.loenwind.autosave.handlers.internal.HandleStorable;
import info.loenwind.autosave.handlers.java.HandleArrayList;
import info.loenwind.autosave.handlers.java.HandleArrays;
import info.loenwind.autosave.handlers.java.HandleEnum;
import info.loenwind.autosave.handlers.java.HandleEnum2EnumMap;
import info.loenwind.autosave.handlers.java.HandleEnumMap;
import info.loenwind.autosave.handlers.java.HandleEnumSet;
import info.loenwind.autosave.handlers.java.HandleHashMap;
import info.loenwind.autosave.handlers.java.HandleHashSet;
import info.loenwind.autosave.handlers.java.HandlePrimitive;
import info.loenwind.autosave.handlers.java.HandleString;
import info.loenwind.autosave.handlers.java.util.HandleSimpleCollection;
import info.loenwind.autosave.handlers.minecraft.HandleBlockPos;
import info.loenwind.autosave.handlers.minecraft.HandleIBlockState;
import info.loenwind.autosave.handlers.minecraft.HandleItemStack;
import info.loenwind.autosave.handlers.util.DelegatingHandler;
import info.loenwind.autosave.util.BitUtil;
import info.loenwind.autosave.util.NullableType;
import info.loenwind.autosave.util.TypeUtil;

/**
 * A registry for {@link IHandler}s.
 * 
 * <p>
 * Registries use Java-like inheritance. That means any registry, except the base registry
 * {@link Registry#GLOBAL_REGISTRY}, has exactly one super-registry.
 * When looking for handlers, all handlers from this registry and all its super-registries will be returned in order.
 *
 */
@SuppressWarnings({ "rawtypes" })
public class Registry {

    /**
     * This is the super-registry of all registries. It contains handlers for Java primitives, Java classes, Minecraft
     * classes and Forge classes.
     * <p>
     * You can register new handlers here if you want other mods to be able to store your objects. Otherwise please use
     * your own registry.
     */

    public static final @Nonnull Registry GLOBAL_REGISTRY = new Registry(true);

    static {
        // Java primitives
        GLOBAL_REGISTRY.register(new HandlePrimitive<>(false, Boolean.class, boolean.class,
                NBTTagCompound::setBoolean, NBTTagCompound::getBoolean));
        GLOBAL_REGISTRY.register(new HandlePrimitive<>((char) 0, Character.class, char.class,
                (nbt, name, c) -> nbt.setInteger(name, (int) c), (nbt, name) -> (char) nbt.getInteger(name)));
        GLOBAL_REGISTRY.register(new HandlePrimitive<>((byte) 0, Byte.class, byte.class, NBTTagCompound::setByte,
                NBTTagCompound::getByte));
        GLOBAL_REGISTRY.register(new HandlePrimitive<>((short) 0, Short.class, short.class,
                NBTTagCompound::setShort, NBTTagCompound::getShort));
        GLOBAL_REGISTRY.register(new HandlePrimitive<>(0, Integer.class, int.class, NBTTagCompound::setInteger,
                NBTTagCompound::getInteger));
        GLOBAL_REGISTRY.register(new HandlePrimitive<>(0L, Long.class, long.class, NBTTagCompound::setLong,
                NBTTagCompound::getLong));
        GLOBAL_REGISTRY.register(new HandlePrimitive<>(0F, Float.class, float.class, NBTTagCompound::setFloat,
                NBTTagCompound::getFloat));
        GLOBAL_REGISTRY.register(new HandlePrimitive<>(0D, Double.class, double.class, NBTTagCompound::setDouble,
                NBTTagCompound::getDouble));
        GLOBAL_REGISTRY.register(new HandleEnum());
        GLOBAL_REGISTRY.register(new HandleString());

        // Primitive array handlers

        // byte/Byte
        IHandler<byte[]> byteArrayHandler = new HandlePrimitive<byte @NullableType []>(new byte[0], byte[].class, null,
                NBTTagCompound::setByteArray, NBTTagCompound::getByteArray);
        GLOBAL_REGISTRY.register(byteArrayHandler);
        GLOBAL_REGISTRY.register(
                new DelegatingHandler<>(Byte[].class, byteArrayHandler, ArrayUtils::toPrimitive, ArrayUtils::toObject));

        // int/Integer
        IHandler<int[]> intArrayHandler = new HandlePrimitive<int @NullableType []>(new int[0], int[].class, null,
                NBTTagCompound::setIntArray, NBTTagCompound::getIntArray);
        GLOBAL_REGISTRY.register(intArrayHandler);
        GLOBAL_REGISTRY.register(new DelegatingHandler<>(Integer[].class, intArrayHandler, ArrayUtils::toPrimitive,
                ArrayUtils::toObject));

        // The rest are packed into int[]

        // short/Short
        IHandler<short[]> shortArrayHandler = new HandlePrimitive<short @NullableType []>(new short[0], short[].class,
                null,
                (nbt, name, arr) -> {
                    int[] ret = new int[arr.length];
                    for (int i = 0; i < ret.length; i++) {
                        ret[i] = arr[i];
                    }
                    nbt.setIntArray(name, ret);
                },
                (nbt, name) -> {
                    int[] read = nbt.getIntArray(name);
                    short[] ret = new short[read.length];
                    for (int i = 0; i < ret.length; i++) {
                        ret[i] = (short) read[i];
                    }
                    return ret;
                });
        GLOBAL_REGISTRY.register(shortArrayHandler);
        GLOBAL_REGISTRY.register(new DelegatingHandler<>(Short[].class, shortArrayHandler, ArrayUtils::toPrimitive,
                ArrayUtils::toObject));

        // char/Character
        IHandler<char[]> charArrayHandler = new HandlePrimitive<char @NullableType []>(new char[0], char[].class, null,
                (nbt, name, arr) -> {
                    int[] ret = new int[arr.length];
                    for (int i = 0; i < ret.length; i++) {
                        ret[i] = arr[i];
                    }
                    nbt.setIntArray(name, ret);
                },
                (nbt, name) -> {
                    int[] read = nbt.getIntArray(name);
                    char[] ret = new char[read.length];
                    for (int i = 0; i < ret.length; i++) {
                        ret[i] = (char) read[i];
                    }
                    return ret;
                });
        GLOBAL_REGISTRY.register(charArrayHandler);
        GLOBAL_REGISTRY.register(new DelegatingHandler<>(Character[].class, charArrayHandler, ArrayUtils::toPrimitive,
                ArrayUtils::toObject));

        // float/Float
        IHandler<float[]> floatArrayHandler = new HandlePrimitive<float @NullableType []>(new float[0], float[].class,
                null,
                (nbt, name, arr) -> {
                    int[] ret = new int[arr.length];
                    for (int i = 0; i < ret.length; i++) {
                        ret[i] = Float.floatToIntBits(arr[i]);
                    }
                    nbt.setIntArray(name, ret);
                },
                (nbt, name) -> {
                    int[] read = nbt.getIntArray(name);
                    float[] ret = new float[read.length];
                    for (int i = 0; i < ret.length; i++) {
                        ret[i] = Float.intBitsToFloat(read[i]);
                    }
                    return ret;
                });
        GLOBAL_REGISTRY.register(floatArrayHandler);
        GLOBAL_REGISTRY.register(new DelegatingHandler<>(Float[].class, floatArrayHandler, ArrayUtils::toPrimitive,
                ArrayUtils::toObject));

        // long/Long
        IHandler<long[]> longArrayHandler = new HandlePrimitive<long @NullableType []>(new long[0], long[].class, null,
                (nbt, name, arr) -> {
                    int[] ret = new int[arr.length * 2];
                    for (int i = 0; i < arr.length; i++) {
                        ret[i * 2] = BitUtil.getLongMSB(arr[i]);
                        ret[i * 2 + 1] = BitUtil.getLongLSB(arr[i]);
                    }
                    nbt.setIntArray(name, ret);
                },
                (nbt, name) -> {
                    int[] read = nbt.getIntArray(name);
                    long[] ret = new long[read.length / 2];
                    for (int i = 0; i < ret.length; i++) {
                        ret[i] = BitUtil.longFromInts(read[i * 2], read[i * 2 + 1]);
                    }
                    return ret;
                });
        GLOBAL_REGISTRY.register(longArrayHandler);
        GLOBAL_REGISTRY.register(
                new DelegatingHandler<>(Long[].class, longArrayHandler, ArrayUtils::toPrimitive, ArrayUtils::toObject));

        // double/Double
        // Reuse the long[] handler since we can just stream convert
        IHandler<double[]> doubleArrayHandler = new DelegatingHandler<>(double[].class, longArrayHandler,
                (doubleArr) -> Arrays.stream(doubleArr).mapToLong(Double::doubleToLongBits).toArray(),
                (longArr) -> Arrays.stream(longArr).mapToDouble(Double::longBitsToDouble).toArray());
        GLOBAL_REGISTRY.register(doubleArrayHandler);
        GLOBAL_REGISTRY.register(new DelegatingHandler<>(Double[].class, doubleArrayHandler, ArrayUtils::toPrimitive,
                ArrayUtils::toObject));

        // Fallback array handler
        GLOBAL_REGISTRY.register(new HandleArrays());

        // Collections

        // List/ArrayList
        GLOBAL_REGISTRY.register(new HandleArrayList());
        // LinkedList
        GLOBAL_REGISTRY.register(new HandleSimpleCollection<>(LinkedList.class));

        // Set/HashSet
        GLOBAL_REGISTRY.register(new HandleHashSet());
        GLOBAL_REGISTRY.register(new HandleEnumSet());

        GLOBAL_REGISTRY.register(new HandleHashMap());
        GLOBAL_REGISTRY.register(new HandleEnum2EnumMap<>()); // This MUST be before HandleEnumMap, special case
        GLOBAL_REGISTRY.register(new HandleEnumMap<>());

        // Minecraft basic types
        GLOBAL_REGISTRY.register(new HandleRegistryEntry());
        GLOBAL_REGISTRY.register(new HandleItemStack());
        GLOBAL_REGISTRY.register(new HandleBlockPos());
        GLOBAL_REGISTRY.register(new HandleIBlockState());
        GLOBAL_REGISTRY.register(new DelegatingHandler<>(ResourceLocation.class, new HandleString(),
                ResourceLocation::toString, ResourceLocation::new));

        // Forge basic types
        GLOBAL_REGISTRY.register(new HandleFluidStack());
        GLOBAL_REGISTRY.register(new HandleFluid());

        // Annotated objects
        GLOBAL_REGISTRY.register(new HandleStorable<>());
    }

    private final List<IHandler> handlers = new ArrayList<>();
    @Nullable
    private final Registry parent;

    /**
     * Creates the {@link Registry#GLOBAL_REGISTRY}.
     * 
     * @param root
     *             A placeholder
     */
    private Registry(boolean root) {
        parent = null;
    }

    /**
     * Crates a new registry which extends {@link Registry#GLOBAL_REGISTRY}.
     */
    public Registry() {
        this(GLOBAL_REGISTRY);
    }

    /**
     * Creates a new registry which extends the given parent.
     * 
     * @param parent
     *               The parent to extend
     */
    public Registry(Registry parent) {
        this.parent = parent;
    }

    /**
     * Registers a new {@link IHandler}.
     * 
     * @param handler
     *                The {@link IHandler} to register
     */
    public void register(IHandler handler) {
        handlers.add(handler);
    }

    /**
     * Registers a new {@link IHandler} that has higher priority than all existing handlers.
     * 
     * @param handler
     *                The {@link IHandler} to register
     */
    public void registerPriority(IHandler handler) {
        handlers.add(0, handler);
    }

    /**
     * Finds all {@link IHandler}s from this registry and all its parents that can handle the given class.
     * 
     * <p>
     * Handlers will be returned in this order:
     * <ol>
     * <li>The annotated special handler of the given class
     * <li>The annotated special handler(s) of its superclass(es)
     * <li>The registered handlers from this registry
     * <li>The registered handlers from this registry's super-registries
     * <li>{@link HandleStorable} if the class is annotated {@link Storable} without a special handler
     * </ol>
     * 
     * Note: If a class is annotated {@link Storable}, then all subclasses must be annotated {@link Storable}, too.
     * <p>
     * Note 2: If a class is annotated {@link Storable} without a special handler, all subclasses must either also be
     * annotated {@link Storable} without a special
     * handler or their handlers must be able to handle the inheritance because {@link HandleStorable} will <i>not</i>
     * be added to this list in this case.
     * <p>
     * Note 3: If a handler can handle a class but not its subclasses, it will not be added to this list for the
     * subclasses.
     * 
     * @param type
     *             The class that should be handled
     * @return A list of all {@link IHandler}s that can handle the class. If none are found, an empty list is returned.
     *
     * @throws InstantiationException From reflection
     * @throws IllegalAccessException From reflection
     */

    public List<IHandler> findHandlers(Type type) throws InstantiationException, IllegalAccessException {
        List<IHandler> result = new ArrayList<>();

        @Nonnull
        Class<?> clazz = TypeUtil.toClass(type);
        Storable annotation = clazz.getAnnotation(Storable.class);
        while (annotation != null) {
            if (annotation.handler() != HandleStorable.class) {
                result.add(annotation.handler().newInstance());
            }
            Class<?> superclass = clazz.getSuperclass();
            if (superclass != null) {
                annotation = superclass.getAnnotation(Storable.class);
                clazz = superclass;
            } else {
                // Theoretically impossible as the hierarchy should always
                // reach Object before null, but handle the case anyway
                break;
            }
        }

        findRegisteredHandlers(this, type, result);

        return result;
    }

    /**
     * Helper method for {@link #findHandlers(Type)}. Looks up only registered handlers and adds them to the end of the
     * given list.
     */
    private void findRegisteredHandlers(Registry caller, Type type, List<IHandler> result) {
        for (IHandler handler : handlers) {
            handler = handler.getHandler(caller, type);
            if (handler != null) {
                result.add(handler);
            }
        }
        final Registry thisParent = parent;
        if (thisParent != null) {
            thisParent.findRegisteredHandlers(caller, type, result);
        }
    }
}
