package info.loenwind.autosave.handlers.minecraft;

import java.lang.reflect.Type;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

import info.loenwind.autosave.Registry;
import info.loenwind.autosave.exceptions.NoHandlerFoundException;
import info.loenwind.autosave.handlers.IHandler;
import info.loenwind.autosave.util.NBTAction;

public class HandleBlockPos implements IHandler<BlockPos> {

    public HandleBlockPos() {}

    @Override
    public Class<?> getRootType() {
        return BlockPos.class;
    }

    @Override
    public boolean store(Registry registry, Set<NBTAction> phase, NBTTagCompound nbt, Type type, String name,
                         BlockPos object)
                                          throws IllegalArgumentException, IllegalAccessException,
                                          InstantiationException, NoHandlerFoundException {
        nbt.setLong(name, object.toLong());
        return true;
    }

    @Override
    public @Nullable BlockPos read(Registry registry, Set<NBTAction> phase, NBTTagCompound nbt, Type type, String name,
                                   @Nullable BlockPos object) throws IllegalArgumentException, IllegalAccessException,
                                                              InstantiationException, NoHandlerFoundException {
        if (nbt.hasKey(name)) {
            return BlockPos.fromLong(nbt.getLong(name));
        }
        return object;
    }
}
