package info.loenwind.autosave.handlers.java.util;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import info.loenwind.autosave.Registry;
import info.loenwind.autosave.engine.StorableEngine;
import info.loenwind.autosave.exceptions.NoHandlerFoundException;
import info.loenwind.autosave.handlers.util.HandleGenericType;
import info.loenwind.autosave.util.NBTAction;

@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class HandleMap<T extends Map> extends HandleGenericType<T> {

    protected HandleMap(Class<? extends T> clazz) {
        super(clazz);
    }

    protected HandleMap(Class<? extends T> clazz, Registry registry, Type... types) throws NoHandlerFoundException {
        super(clazz, registry, types);
    }

    @Override
    public boolean store(Registry registry, Set<NBTAction> phase, NBTTagCompound nbt, Type type, String name, T object)
                                                                                                                        throws IllegalArgumentException,
                                                                                                                        IllegalAccessException,
                                                                                                                        InstantiationException,
                                                                                                                        NoHandlerFoundException {
        NBTTagList tag = new NBTTagList();
        for (Entry e : (Set<Entry>) object.entrySet()) {
            NBTTagCompound etag = new NBTTagCompound();
            Object key = e.getKey();
            if (key != null) {
                storeRecursive(0, registry, phase, etag, "key", key);
            } else {
                etag.setBoolean("key" + StorableEngine.NULL_POSTFIX, true);
            }
            Object val = e.getValue();
            if (val != null) {
                storeRecursive(1, registry, phase, etag, "val", val);
            } else {
                etag.setBoolean("val" + StorableEngine.NULL_POSTFIX, true);
            }
            tag.appendTag(etag);
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
                object = createMap();
            } else {
                object.clear();
            }

            NBTTagList tag = nbt.getTagList(name, Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < tag.tagCount(); i++) {
                NBTTagCompound etag = tag.getCompoundTagAt(i);
                Object key = etag.getBoolean("key" + StorableEngine.NULL_POSTFIX) ? null :
                        readRecursive(0, registry, phase, etag, "key", null);
                Object val = etag.getBoolean("val" + StorableEngine.NULL_POSTFIX) ? null :
                        readRecursive(1, registry, phase, etag, "val", null);
                object.put(key, val);
            }
        }
        return object;
    }

    abstract protected T createMap();
}
