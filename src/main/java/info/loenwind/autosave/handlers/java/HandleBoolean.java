package info.loenwind.autosave.handlers.java;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import info.loenwind.autosave.Registry;
import info.loenwind.autosave.handlers.IHandler;
import info.loenwind.autosave.util.NBTAction;
import info.loenwind.autosave.util.TypeUtil;
import net.minecraft.nbt.NBTTagCompound;

public class HandleBoolean implements IHandler<Boolean> {

  public HandleBoolean() {
  }

  @Override
  public IHandler<? extends Boolean> getHandler(Type type) {
    return TypeUtil.isAssignable(Boolean.class, type) || TypeUtil.isAssignable(boolean.class, type) ? this : null;
  }

  @Override
  public boolean store(@Nonnull Registry registry, @Nonnull Set<NBTAction> phase, @Nonnull NBTTagCompound nbt, @Nonnull String name,
      @Nonnull Boolean object) throws IllegalArgumentException, IllegalAccessException {
    nbt.setBoolean(name, object);
    return true;
  }

  @Override
  public Boolean read(@Nonnull Registry registry, @Nonnull Set<NBTAction> phase, @Nonnull NBTTagCompound nbt, @Nullable Field field, @Nonnull String name,
      @Nullable Boolean object) {
    return nbt.hasKey(name) ? nbt.getBoolean(name) : object != null ? object : false;
  }

}
