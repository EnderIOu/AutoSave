package info.loenwind.autosave.handlers.java;

import java.lang.reflect.Type;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.MathHelper;

import info.loenwind.autosave.Registry;
import info.loenwind.autosave.handlers.IHandler;
import info.loenwind.autosave.util.NBTAction;
import info.loenwind.autosave.util.TypeUtil;

public class HandleEnum implements IHandler<Enum<?>> {

    public HandleEnum() {}

    @Override
    public Class<?> getRootType() {
        return Enum.class;
    }

    @Override
    public boolean store(Registry registry, Set<NBTAction> phase, NBTTagCompound nbt, Type type, String name,
                         Enum<?> object) throws IllegalArgumentException, IllegalAccessException {
        nbt.setInteger(name, object.ordinal());
        return true;
    }

    @Override
    public @Nullable Enum<?> read(Registry registry, Set<NBTAction> phase, NBTTagCompound nbt, Type type, String name,
                                  @Nullable Enum<?> object) {
        if (nbt.hasKey(name)) {
            Enum<?>[] enumConstants = (Enum<?>[]) TypeUtil.toClass(type).getEnumConstants();
            if (enumConstants != null) { // This should be "impossible"
                return enumConstants[MathHelper.clamp(nbt.getInteger(name), 0, enumConstants.length - 1)];
            }
        }
        return object;
    }
}
