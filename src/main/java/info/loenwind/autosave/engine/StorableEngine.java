package info.loenwind.autosave.engine;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import info.loenwind.autosave.Reader;
import info.loenwind.autosave.Registry;
import info.loenwind.autosave.Writer;
import info.loenwind.autosave.annotations.Storable;
import info.loenwind.autosave.annotations.Store;
import info.loenwind.autosave.exceptions.NoHandlerFoundException;
import info.loenwind.autosave.handlers.IHandler;
import info.loenwind.autosave.handlers.internal.HandleStorable;
import info.loenwind.autosave.handlers.internal.NullHandler;
import info.loenwind.autosave.util.Log;
import info.loenwind.autosave.util.NBTAction;
import info.loenwind.autosave.util.NullHelper;
import net.minecraft.nbt.NBTTagCompound;

/**
 * The thread-safe engine that handles (re-)storing {@link Storable} objects by storing their fields. The fields to (re-)store must be annotated {@link Store}.
 * <p>
 * It will also process the annotated fields of superclasses, as long as there is an unbroken chain of {@link Storable} annotations (without special handlers).
 * Fields that have the same name as a field in a sub-/super-class will be processed independently.
 * <p>
 * If the final superclass has an {@link IHandler} registered in the {@link Registry}, it will also be processed. However, this will <i>not</i> work for
 * handlers that return a new object instead of changing the given one. A handler can check for this case by seeing if its "name" parameter is
 * {@link StorableEngine#SUPERCLASS_KEY}.
 * <p>
 * Note: If a {@link Storable} object is encountered in a {@link Store} field, it is handled by {@link HandleStorable}---which delegates here.
 * <p>
 * Note 2: There are public entrances to this class in {@link Writer} and {@link Reader}.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class StorableEngine {

  private static final ThreadLocal<StorableEngine> INSTANCE = new ThreadLocal<StorableEngine>() {
    @Override
    protected StorableEngine initialValue() {
      return new StorableEngine();
    }
  };

  public static final @Nonnull String NULL_POSTFIX = "-";
  public static final @Nonnull String EMPTY_POSTFIX = "+";
  public static final @Nonnull String SUPERCLASS_KEY = "__superclass";
  private final @Nonnull Map<Class<?>, List<Field>> fieldCache = new HashMap<Class<?>, List<Field>>();
  private final @Nonnull Map<Field, Set<NBTAction>> phaseCache = new HashMap<Field, Set<NBTAction>>();
  private final @Nonnull Map<Field, List<IHandler>> fieldHandlerCache = new HashMap<Field, List<IHandler>>();
  private final @Nonnull Map<Class<?>, Class<?>> superclassCache = new HashMap<Class<?>, Class<?>>();
  private final @Nonnull Map<Class<?>, List<IHandler>> superclassHandlerCache = new HashMap<Class<?>, List<IHandler>>();

  private StorableEngine() {
  }

  public static <T> void read(Registry registry, Set<NBTAction> phase, NBTTagCompound tag, T object)
      throws IllegalAccessException, InstantiationException, NoHandlerFoundException {
    INSTANCE.get().read_impl(registry, phase, tag, object);
  }

  public static <T> void store(Registry registry, Set<NBTAction> phase, NBTTagCompound tag, T object)
      throws IllegalAccessException, InstantiationException, NoHandlerFoundException {
    INSTANCE.get().store_impl(registry, phase, tag, object);
  }

  public <T> void read_impl(Registry registry, Set<NBTAction> phase, NBTTagCompound tag, T object)
      throws IllegalAccessException, InstantiationException, NoHandlerFoundException {
    Class<? extends Object> clazz = object.getClass();
    if (!fieldCache.containsKey(clazz)) {
      cacheHandlers(registry, clazz);
    }

    Log.livetraceNBT("Reading NBT data for object ", object, " of class ", clazz, " for phase(s) ", phase, " from NBT ", tag);
    for (Field field : fieldCache.get(clazz)) {
      if (!Collections.disjoint(phaseCache.get(field), phase)) {
        Object fieldData = field.get(object);
        String fieldName = field.getName();
        if (!tag.hasKey(fieldName + NULL_POSTFIX) && fieldName != null) {
          for (IHandler handler : fieldHandlerCache.get(field)) {
            Log.livetraceNBT("Trying to read data for field ", fieldName, " with handler ", handler);
            Object result = handler.read(registry, phase, tag, field, fieldName, fieldData);
            if (result != null) {
              Log.livetraceNBT("Read data for field ", fieldName, " with handler ", handler, " yielded data: ", result);
              field.set(object, result);
              break;
            }
          }
        } else {
          Log.livetraceNBT("Field ", fieldName, " is set to null. NULL_POSTFIX=", tag.hasKey(fieldName + NULL_POSTFIX));
          field.set(object, null);
        }
      } else {
        Log.livetraceNBT("Field ", field.getName(), " is not part of the current phase.");
      }
    }

    Class<?> superclazz = superclassCache.get(clazz);
    if (superclazz != null) {
      for (IHandler handler : superclassHandlerCache.get(superclazz)) {
        Log.livetraceNBT("Trying to read data for super class ", superclazz, " with handler ", handler);
        if (handler.read(registry, phase, tag, null, SUPERCLASS_KEY, object) != null) {
          Log.livetraceNBT("Read data for super class ", superclazz, " with handler ", handler);
          break;
        }
      }
    }

    Log.livetraceNBT("Read NBT data for object ", object, " of class ", clazz);
  }

  public <T> void store_impl(Registry registry, Set<NBTAction> phase, NBTTagCompound tag, T object)
      throws IllegalAccessException, InstantiationException, NoHandlerFoundException {
    Class<? extends Object> clazz = object.getClass();
    if (!fieldCache.containsKey(clazz)) {
      cacheHandlers(registry, clazz);
    }

    Log.livetraceNBT("Saving NBT data for object ", object, " of class ", clazz, " for phase(s) ", phase, " into NBT ", tag);
    for (Field field : fieldCache.get(clazz)) {
      if (!Collections.disjoint(phaseCache.get(field), phase)) {
        Object fieldData = field.get(object);
        String fieldName = field.getName();
        if (fieldData != null && fieldName != null) {
          for (IHandler handler : fieldHandlerCache.get(field)) {
            Log.livetraceNBT("Trying to save data for field ", fieldName, " with handler ", handler);
            if (handler.store(registry, phase, tag, fieldName, fieldData)) {
              Log.livetraceNBT("Saved data for field ", fieldName, " with handler ", handler, ". NBT now is ", tag);
              break;
            }
          }
        } else {
          Log.livetraceNBT("Field ", fieldName, " is null. Setting NULL_POSTFIX.");
          tag.setBoolean(fieldName + NULL_POSTFIX, true);
        }
      } else {
        Log.livetraceNBT("Field ", field.getName(), " is not part of the current phase.");
      }
    }

    Class<?> superclazz = superclassCache.get(clazz);
    if (superclazz != null) {
      for (IHandler handler : superclassHandlerCache.get(superclazz)) {
        Log.livetraceNBT("Trying to save data for super class ", superclazz, " with handler ", handler);
        if (handler.store(registry, phase, tag, SUPERCLASS_KEY, object)) {
          Log.livetraceNBT("Saved data for super class ", superclazz, " with handler ", handler);
          break;
        }
      }
    }

    Log.livetraceNBT("Saved NBT data for object ", object, " of class ", clazz);
  }

  public static @Nullable <T> T getSingleField(Registry registry, Set<NBTAction> phase, NBTTagCompound tag, String fieldName,
      Class<T> clazz, @Nullable T object) throws InstantiationException, IllegalAccessException, IllegalArgumentException, NoHandlerFoundException {
    if (!tag.hasKey(fieldName + NULL_POSTFIX)) {
      for (IHandler<T> handler : registry.findHandlers(clazz)) {
        T result = handler.read(registry, phase, tag, null, fieldName, object);
        if (result != null) {
          return result;
        }
      }
    }
    return null;
  }

  public static <T> void setSingleField(Registry registry, Set<NBTAction> phase, NBTTagCompound tag, String fieldName,
      Class<T> clazz, @Nullable T fieldData) throws InstantiationException, IllegalAccessException, IllegalArgumentException, NoHandlerFoundException {
    if (fieldData != null) {
      tag.removeTag(fieldName + NULL_POSTFIX);
      for (IHandler<T> handler : registry.findHandlers(clazz)) {
        if (handler.store(registry, phase, tag, fieldName, fieldData)) {
          return;
        }
      }
      throw new NoHandlerFoundException(clazz, fieldName);
    } else {
      tag.removeTag(fieldName);
      tag.setBoolean(fieldName + NULL_POSTFIX, true);
      return;
    }
  }

  private void cacheHandlers(Registry registry, Class<?> clazz) throws IllegalAccessException, InstantiationException, NoHandlerFoundException {
    final ArrayList<Field> fieldList = new ArrayList<Field>();
    for (Field field : clazz.getDeclaredFields()) {
      Store annotation = field.getAnnotation(Store.class);
      if (annotation != null) {
        ArrayList<IHandler> handlerList = new ArrayList<IHandler>();
        String fieldName = field.getName();
        if (fieldName != null) {
          Type fieldType = NullHelper.notnullJ(field.getGenericType(), "Field#getGenericType");
          Class<? extends IHandler> handlerClass = annotation.handler();
          if (handlerClass != NullHandler.class) {
            IHandler handler = handlerClass.newInstance().getHandler(registry, fieldType);
            if (handler != null) {
              handlerList.add(handler);
            } else {
              throw new NoHandlerFoundException("Handler specified in annotation on " + field + " does not apply to " + fieldType + ".");
            }
          }
          handlerList.addAll(registry.findHandlers(fieldType));
          if (handlerList.isEmpty()) {
            throw new NoHandlerFoundException(field, clazz);
          }
          EnumSet<NBTAction> enumSet = EnumSet.noneOf(NBTAction.class);
          enumSet.addAll(Arrays.asList(annotation.value()));
          phaseCache.put(field, enumSet);
          field.setAccessible(true);
          fieldList.add(field);
          fieldHandlerCache.put(field, handlerList);
        }
      }
    }

    Class<?> superclazz = clazz.getSuperclass();
    if (superclazz != null) {
      Storable annotation = superclazz.getAnnotation(Storable.class);
      if (annotation != null) {
        if (annotation.handler() == HandleStorable.class) {
          cacheHandlers(registry, superclazz);
          fieldList.addAll(fieldCache.get(superclazz));
        } else {
          superclassCache.put(clazz, superclazz);
          if (!superclassCache.containsKey(superclazz)) {
            superclassHandlerCache.put(superclazz, (List<IHandler>) Arrays.asList(annotation.handler().newInstance()));
          }
        }
      } else {
        List<IHandler> handlers = registry.findHandlers(superclazz);
        if (!handlers.isEmpty()) {
          superclassCache.put(clazz, superclazz);
          if (!superclassCache.containsKey(superclazz)) {
            superclassHandlerCache.put(superclazz, handlers);
          }
        }
      }
    }

    fieldCache.put(clazz, fieldList);
  }

}
