package info.loenwind.autosave.handlers.java;

import java.lang.reflect.Field;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import info.loenwind.autosave.Registry;
import info.loenwind.autosave.handlers.IHandler;
import info.loenwind.autosave.util.NBTAction;
import net.minecraft.nbt.NBTTagCompound;

public class HandleString implements IHandler<String> {

  public HandleString() {
  }

  @Override
  public Class<?> getRootType() {
    return String.class;
  }

  @Override
  public boolean store(@Nonnull Registry registry, @Nonnull Set<NBTAction> phase, @Nonnull NBTTagCompound nbt, @Nonnull String name, @Nonnull String object)
      throws IllegalArgumentException, IllegalAccessException {
    nbt.setString(name, object);
    return true;
  }

  @Override
  public String read(@Nonnull Registry registry, @Nonnull Set<NBTAction> phase, @Nonnull NBTTagCompound nbt, @Nullable Field field, @Nonnull String name,
      @Nullable String object) {
    return nbt.hasKey(name) ? nbt.getString(name) : object;
  }

}
