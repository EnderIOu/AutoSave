package info.loenwind.autosave.handlers.forge;

import java.lang.reflect.Type;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraftforge.registries.RegistryManager;

import info.loenwind.autosave.Registry;
import info.loenwind.autosave.exceptions.NoHandlerFoundException;
import info.loenwind.autosave.handlers.IHandler;
import info.loenwind.autosave.util.NBTAction;

@SuppressWarnings("rawtypes")
public class HandleRegistryEntry implements IHandler<IForgeRegistryEntry> {

    // Usually handler must save into the name, but we cheat and use a second nbt key here
    private static final String REGISTRY = "*R";

    @Override
    public Class<?> getRootType() {
        return IForgeRegistryEntry.class;
    }

    @Override
    public boolean store(Registry registry, Set<NBTAction> phase, NBTTagCompound nbt, Type type, String name,
                         IForgeRegistryEntry object)
                                                     throws IllegalArgumentException, IllegalAccessException,
                                                     InstantiationException, NoHandlerFoundException {
        ResourceLocation loc = object.getRegistryName();
        if (loc == null) {
            throw new IllegalArgumentException("Registry entry must be registered to be stored: " + object);
        }
        nbt.setString(name, loc.toString());
        @SuppressWarnings("unchecked")
        final IForgeRegistry<?> forgeRegistry = GameRegistry.findRegistry(object.getRegistryType());
        if (forgeRegistry == null) {
            throw new IllegalArgumentException("Registry entry's registry must be registered to be stored: " + object);
        }
        nbt.setString(name + REGISTRY, RegistryManager.ACTIVE.getName(forgeRegistry).toString());
        return true;
    }

    @Override
    @Nullable
    public IForgeRegistryEntry read(Registry registry, Set<NBTAction> phase, NBTTagCompound nbt, Type type, String name,
                                    @Nullable IForgeRegistryEntry object)
                                                                          throws IllegalArgumentException,
                                                                          IllegalAccessException,
                                                                          InstantiationException,
                                                                          NoHandlerFoundException {
        if (nbt.hasKey(name) && nbt.hasKey(name + REGISTRY)) {
            final ResourceLocation registryName = new ResourceLocation(nbt.getString(name + REGISTRY));
            IForgeRegistry forgeRegistry = RegistryManager.ACTIVE.getRegistry(registryName);
            if (forgeRegistry == null) {
                throw new IllegalArgumentException(
                        "Registry entry's registry must be registered to be read: " + registryName);
            }
            ResourceLocation rl = new ResourceLocation(nbt.getString(name));
            return forgeRegistry.getValue(rl);
        } else {
            return object;
        }
    }
}
