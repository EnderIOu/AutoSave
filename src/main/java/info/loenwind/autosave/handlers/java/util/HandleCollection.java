package info.loenwind.autosave.handlers.java.util;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.nbt.NBTTagCompound;

import info.loenwind.autosave.Registry;
import info.loenwind.autosave.exceptions.NoHandlerFoundException;
import info.loenwind.autosave.handlers.IHandler;
import info.loenwind.autosave.handlers.util.HandleGenericType;
import info.loenwind.autosave.util.NBTAction;

@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class HandleCollection<T extends Collection> extends HandleGenericType<T> {

    public HandleCollection(Class<? extends T> clazz) {
        super(clazz);
    }

    protected HandleCollection(Class<? extends T> clazz, Registry registry,
                               Type... types) throws NoHandlerFoundException {
        super(clazz, registry, types);
    }

    @Override
    public boolean store(Registry registry, Set<NBTAction> phase, NBTTagCompound nbt, Type type, String name, T object)
                                                                                                                        throws IllegalArgumentException,
                                                                                                                        IllegalAccessException,
                                                                                                                        InstantiationException,
                                                                                                                        NoHandlerFoundException {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("size", object.size());
        int i = 0;
        for (Object elem : object) {
            if (elem != null) {
                for (IHandler handler : subHandlers[0]) {
                    handler.store(registry, phase, tag, type, String.valueOf(i), elem);
                }
            }
            i++;
        }
        nbt.setTag(name, tag);
        return true;
    }

    @Override
    public @Nullable T read(Registry registry, Set<NBTAction> phase, NBTTagCompound nbt, Type type, String name,
                            @Nullable T object) throws IllegalArgumentException, IllegalAccessException,
                                                InstantiationException, NoHandlerFoundException {
        if (nbt.hasKey(name)) {
            if (object == null) {
                object = makeCollection();
            } else {
                object.clear();
            }

            NBTTagCompound tag = nbt.getCompoundTag(name);
            int size = tag.getInteger("size");
            for (int i = 0; i < size; i++) {
                object.add(readRecursive(0, registry, phase, tag, String.valueOf(i), null));
            }
        }
        return object;
    }

    abstract protected T makeCollection();
}
