package info.loenwind.autosave.handlers.internal;

import java.lang.reflect.Type;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.nbt.NBTTagCompound;

import info.loenwind.autosave.Registry;
import info.loenwind.autosave.annotations.Storable;
import info.loenwind.autosave.annotations.Store;
import info.loenwind.autosave.engine.StorableEngine;
import info.loenwind.autosave.exceptions.NoHandlerFoundException;
import info.loenwind.autosave.handlers.IHandler;
import info.loenwind.autosave.util.NBTAction;
import info.loenwind.autosave.util.NullHelper;
import info.loenwind.autosave.util.TypeUtil;

/**
 * An {@link IHandler} that can (re-)store objects by storing their fields. The
 * fields to (re-)store must be annotated {@link Store}.
 * <p>
 * It will also process the annotated fields of superclasses, as long as there
 * is an unbroken chain of {@link Storable} annotations (without special
 * handlers). Fields that have the same name as a field in a sub-/super-class
 * will be processed independently.
 * <p>
 * If the final superclass has an {@link IHandler} registered in the
 * {@link Registry}, it will also be processed. However, this will <i>not</i>
 * work for handlers that return a new object instead of changing the given one.
 * A handler can check for this case by seeing if its "name" parameter is
 * {@link StorableEngine#SUPERCLASS_KEY}.
 *
 * @param <T>
 */
public class HandleStorable<T> implements IHandler<T> {

    public HandleStorable() {}

    @Override
    public @Nullable IHandler<T> getHandler(Registry registry, Type type) {
        Class<?> clazz = TypeUtil.toClass(type);
        Storable annotation = clazz.getAnnotation(Storable.class);
        return annotation != null && annotation.handler() == this.getClass() ? this : null;
    }

    @Override
    public boolean store(Registry registry, Set<NBTAction> phase, NBTTagCompound nbt, Type type, String name, T object)
                                                                                                                        throws IllegalArgumentException,
                                                                                                                        IllegalAccessException,
                                                                                                                        InstantiationException,
                                                                                                                        NoHandlerFoundException {
        NBTTagCompound tag = new NBTTagCompound();
        StorableEngine.store(registry, phase, tag, object);
        nbt.setTag(name, tag);
        return true;
    }

    @Override
    public @Nullable T read(Registry registry, Set<NBTAction> phase, NBTTagCompound nbt, Type type, String name,
                            @Nullable T object)
                                                throws IllegalArgumentException, IllegalAccessException,
                                                InstantiationException, NoHandlerFoundException {
        if (nbt.hasKey(name)) {
            if (object == null) {
                object = StorableEngine.instantiate(registry, type);
            }
            NBTTagCompound tag = NullHelper.notnullM(nbt.getCompoundTag(name), "NBTTagCompound.getCompoundTag()");
            StorableEngine.read(registry, phase, tag, object);
        }
        return object;
    }
}
