package com.shanebeestudios.vf.api;

import com.shanebeestudios.vf.api.machine.Furnace;
import com.shanebeestudios.vf.api.property.FurnaceProperties;
import com.shanebeestudios.vf.api.task.FurnaceTick;
import com.shanebeestudios.vf.api.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.Properties;

/**
 * Main API for VirtualFurnace
 */
@SuppressWarnings("unused")
public class VirtualFurnaceAPI {

    static {
        ConfigurationSerialization.registerClass(Furnace.class, "furnace");
        ConfigurationSerialization.registerClass(FurnaceProperties.class, "furnace_properties");
    }

    private static VirtualFurnaceAPI instance;
    private final String apiVersion;
    private final JavaPlugin plugin;
    private final RecipeManager recipeManager;
    private final FurnaceManager furnaceManager;
    private final FurnaceTick furnaceTick;

    /**
     * Create a new instance of the VirtualFurnaceAPI
     *
     * @param javaPlugin Your plugin
     */
    public VirtualFurnaceAPI(JavaPlugin javaPlugin) {
        instance = this;
        this.plugin = javaPlugin;
        this.apiVersion = getVersion();
        new Metrics(javaPlugin, 7021, this);
        this.recipeManager = new RecipeManager();
        this.furnaceManager = new FurnaceManager(this);
        this.furnaceTick = new FurnaceTick(this);
        Bukkit.getPluginManager().registerEvents(new FurnaceListener(this), javaPlugin);
        Util.log("Initialized VirtualFurnaceAPI version: &b" + getVersion());
    }

    /**
     * Disable the furnace tick
     * <p>This is good to use in your onDisable() method, to prevent tasks still running on server shutdown/reload</p>
     */
    public void disableFurnaceTick() {
        this.furnaceTick.cancel();
    }

    /**
     * Get a static instance of the VirtualFurnaceAPI
     * <p><b>NOTE:</b> You have to create once first or this will return null</p>
     *
     * @return Instance of the VirtualFurnaceAPI
     */
    public static VirtualFurnaceAPI getInstance() {
        return instance;
    }

    /**
     * Get the plugin that was passed into this instance
     *
     * @return Plugin in this instance
     */
    public JavaPlugin getJavaPlugin() {
        return plugin;
    }

    /**
     * Get an instance of the recipe manager
     *
     * @return Instance of recipe manager
     */
    public RecipeManager getRecipeManager() {
        return recipeManager;
    }

    /**
     * Get an instance of the furnace manager
     *
     * @return Instance of the furnace manager
     */
    public FurnaceManager getFurnaceManager() {
        return furnaceManager;
    }

    /**
     * Get an instance of the furnace tick class
     *
     * @return Instance of furnace tick
     */
    public FurnaceTick getFurnaceTick() {
        return furnaceTick;
    }

    /**
     * Get the version of this API
     *
     * @return Version of the API
     */
    public String getAPIVersion() {
        return this.apiVersion;
    }

    private String getVersion() {
        Properties prop = new Properties();
        try {
            prop.load(getJavaPlugin().getResource("VirtualFurnace.properties"));
            return prop.getProperty("api-version");
        } catch (IOException e) {
            return "unknown-version";
        }
    }

}
