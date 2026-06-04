package id.naturalsmp.naturalSchool.api;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

public final class NaturalSchoolProvider {

    private static NaturalSchoolAPI instance;

    private NaturalSchoolProvider() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /**
     * Gets the registered NaturalSchoolAPI instance.
     * Searches Bukkit's ServicesManager if the cached static reference is not set.
     *
     * @return NaturalSchoolAPI instance
     * @throws IllegalStateException if the API has not been registered yet
     */
    @NotNull
    public static NaturalSchoolAPI get() {
        if (instance != null) {
            return instance;
        }
        NaturalSchoolAPI api = Bukkit.getServicesManager().load(NaturalSchoolAPI.class);
        if (api == null) {
            throw new IllegalStateException("NaturalSchoolAPI is not registered yet!");
        }
        instance = api;
        return api;
    }

    /**
     * Registers the API implementation statically.
     * This is intended for internal use during plugin enablement.
     *
     * @param api NaturalSchoolAPI implementation
     */
    @ApiStatus.Internal
    public static void register(NaturalSchoolAPI api) {
        instance = api;
    }

    /**
     * Unregisters the API implementation statically.
     * This is intended for internal use during plugin disablement.
     */
    @ApiStatus.Internal
    public static void unregister() {
        instance = null;
    }
}
