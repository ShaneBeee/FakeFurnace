package com.shanebeestudios.vf.api;

import com.shanebeestudios.vf.api.machine.Furnace;
import com.shanebeestudios.vf.api.machine.Machine;
import com.shanebeestudios.vf.api.property.FurnaceProperties;
import com.shanebeestudios.vf.api.util.Util;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Manager for {@link Machine Machines}
 * <p>You can get an instance of this class from <b>{@link VirtualFurnaceAPI#getFurnaceManager()}</b></p>
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class FurnaceManager {

    private static final boolean HAS_GLINT = Util.methodExists(ItemMeta.class, "getEnchantmentGlintOverride");
    @SuppressWarnings({"deprecation", "DataFlowIssue"})
    private static final @NotNull Enchantment SHARPNESS = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("sharpness"));
    @SuppressWarnings({"deprecation", "DataFlowIssue"})
    private static final @NotNull Enchantment INFINITY = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("infinity"));

    private final VirtualFurnaceAPI virtualFurnaceAPI;
    private File machineFile;
    private FileConfiguration machineConfig;
    private final Map<UUID, Machine> machineMap;
    private final NamespacedKey key;

    FurnaceManager(VirtualFurnaceAPI virtualFurnaceAPI) {
        this.virtualFurnaceAPI = virtualFurnaceAPI;
        this.machineMap = new HashMap<>();
        this.key = new NamespacedKey(virtualFurnaceAPI.getJavaPlugin(), "furnaceID");
        loadFurnaceConfig();
    }

    /**
     * Get a collection of all {@link Furnace Furnaces}
     *
     * @return Collection of all furnaces
     * @deprecated Use {@link #getAllMachines()} instead
     */
    @Deprecated(since = "1.1.0")
    public Collection<Furnace> getAllFurnaces() {
        List<Furnace> furnaces = new ArrayList<>();
        for (Machine value : this.machineMap.values()) {
            if (value instanceof Furnace furnace) {
                furnaces.add(furnace);
            }
        }
        return furnaces;
    }

    /**
     * Get a collection of all {@link Machine Machines}
     *
     * @return Collection of all machines
     */
    public Collection<Machine> getAllMachines() {
        return Collections.unmodifiableCollection(this.machineMap.values());
    }

    /**
     * Get a {@link Machine} by ID
     *
     * @param uuid ID of machine to grab
     * @return Machine from ID (null if a machine with this ID does not exist)
     */
    public Machine getByID(@NotNull UUID uuid) {
        return this.machineMap.get(uuid);
    }

    /**
     * Create a new furnace
     * <p>This will create a new furnace, add it to the tick list, and save to file</p>
     * <p><b>NOTE:</b> The default <b>{@link FurnaceProperties}</b> associated with this furnace will be <b>{@link FurnaceProperties#FURNACE}</b></p>
     *
     * @param name Name of new furnace (This shows up in the inventory view)
     * @return Instance of this new furnace
     */
    public Furnace createFurnace(@NotNull String name) {
        return createFurnace(name, FurnaceProperties.FURNACE, null);
    }

    /**
     * Create a new furnace
     * <p>This will create a new furnace, add it to the tick list, and save to file</p>
     *
     * @param name              Name of new furnace (This shows up in the inventory view)
     * @param furnaceProperties Properties to apply to this furnace
     * @return Instance of this new furnace
     */
    public Furnace createFurnace(@NotNull String name, @NotNull FurnaceProperties furnaceProperties) {
        return createFurnace(name, furnaceProperties, null);
    }

    /**
     * Create a new furnace
     * <p>This will create a new furnace, add it to the tick list, and save to file</p>
     * <p><b>NOTE:</b> The default <b>{@link FurnaceProperties}</b> associated with this furnace will be <b>{@link FurnaceProperties#FURNACE}</b></p>
     *
     * @param name     Name of new furnace (This shows up in the inventory view)
     * @param function Function to run before furnace is created
     * @return Instance of this new furnace
     */
    public Furnace createFurnace(@NotNull String name, @NotNull Consumer<Furnace> function) {
        return createFurnace(name, FurnaceProperties.FURNACE, function);
    }

    /**
     * Create a new furnace
     * <p>This will create a new furnace, add it to the tick list, and save to file</p>
     *
     * @param name              Name of new furnace (This shows up in the inventory view)
     * @param furnaceProperties Properties to apply to this furnace
     * @param function          Function to run before furnace is created
     * @return Instance of this new furnace
     */
    public Furnace createFurnace(@NotNull String name, @NotNull FurnaceProperties furnaceProperties, @Nullable Consumer<Furnace> function) {
        Furnace furnace = new Furnace(name, furnaceProperties);
        if (function != null) {
            function.accept(furnace);
        }
        this.machineMap.put(furnace.getUniqueID(), furnace);
        saveFurnace(furnace, true);
        return furnace;
    }

    /**
     * Create a {@link Furnace} that is attached to an {@link ItemStack}
     * <p><b>NOTE:</b> The default <b>{@link FurnaceProperties}</b> associated with this furnace will be <b>{@link FurnaceProperties#FURNACE}</b></p>
     *
     * @param name     Name of furnace (this will show up in the furnace UI)
     * @param material Material of the new ItemStack
     * @param glowing  Whether the item should glow (enchanted)
     * @return New ItemStack with a furnace attached
     */
    public ItemStack createItemWithFurnace(@NotNull String name, @NotNull Material material, boolean glowing) {
        return createItemWithFurnace(name, new ItemStack(material), glowing);
    }

    /**
     * Create a {@link Furnace} that is attached to an {@link ItemStack}
     *
     * @param name              Name of furnace (this will show up in the furnace UI)
     * @param furnaceProperties Properties associated with this furnace item
     * @param material          Material of the new ItemStack
     * @param glowing           Whether the item should glow (enchanted)
     * @return New ItemStack with a furnace attached
     */
    public ItemStack createItemWithFurnace(@NotNull String name, @NotNull FurnaceProperties furnaceProperties, @NotNull Material material, boolean glowing) {
        return createItemWithFurnace(name, furnaceProperties, new ItemStack(material), glowing);
    }

    /**
     * Create a {@link Furnace} that is attached to an {@link ItemStack}
     * <p><b>NOTE:</b> The default <b>{@link FurnaceProperties}</b> associated with this furnace will be <b>{@link FurnaceProperties#FURNACE}</b></p>
     *
     * @param name     Name of furnace (this will show up in the furnace UI)
     * @param material Material of the new ItemStack
     * @param glowing  Whether the item should glow (enchanted)
     * @param function Function to run before furnace is created
     * @return New ItemStack with a furnace attached
     */
    public ItemStack createItemWithFurnace(@NotNull String name, @NotNull Material material, boolean glowing, @Nullable Consumer<Furnace> function) {
        return createItemWithFurnace(name, new ItemStack(material), glowing, function);
    }

    /**
     * Create a {@link Furnace} that is attached to an {@link ItemStack}
     *
     * @param name              Name of furnace (this will show up in the furnace UI)
     * @param furnaceProperties Properties associated with this furnace item
     * @param material          Material of the new ItemStack
     * @param glowing           Whether the item should glow (enchanted)
     * @param function          Function to run before furnace is created
     * @return New ItemStack with a furnace attached
     */
    public ItemStack createItemWithFurnace(@NotNull String name, @NotNull FurnaceProperties furnaceProperties, @NotNull Material material, boolean glowing, @Nullable Consumer<Furnace> function) {
        return createItemWithFurnace(name, furnaceProperties, new ItemStack(material), glowing, function);
    }

    /**
     * Create a {@link Furnace} that is attached to an {@link ItemStack}
     *
     * @param name      Name of furnace (this will show up in the furnace UI)
     * @param itemStack ItemStack to be copied and have a furnace attached
     * @param glowing   Whether the item should glow (enchanted)
     * @return Clone of the input ItemStack with a furnace attached
     */
    public ItemStack createItemWithFurnace(@NotNull String name, @NotNull ItemStack itemStack, boolean glowing) {
        return createItemWithFurnace(name, itemStack, glowing, null);
    }

    /**
     * Create a {@link Furnace} that is attached to an {@link ItemStack}
     *
     * @param name              Name of furnace (this will show up in the furnace UI)
     * @param furnaceProperties Properties associated with this furnace item
     * @param itemStack         ItemStack to be copied and have a furnace attached
     * @param glowing           Whether the item should glow (enchanted)
     * @return Clone of the input ItemStack with a furnace attached
     */
    public ItemStack createItemWithFurnace(@NotNull String name, @NotNull FurnaceProperties furnaceProperties, @NotNull ItemStack itemStack, boolean glowing) {
        return createItemWithFurnace(name, furnaceProperties, itemStack, glowing, null);
    }

    /**
     * Create a {@link Furnace} that is attached to an {@link ItemStack}
     * <p><b>NOTE:</b> The default <b>{@link FurnaceProperties}</b> associated with this furnace will be <b>{@link FurnaceProperties#FURNACE}</b></p>
     *
     * @param name      Name of furnace (this will show up in the furnace UI)
     * @param itemStack ItemStack to be copied and have a furnace attached
     * @param glowing   Whether the item should glow (enchanted)
     * @param function  Function to run before furnace is created
     * @return Clone of the input ItemStack with a furnace attached
     */
    public ItemStack createItemWithFurnace(@NotNull String name, @NotNull ItemStack itemStack, boolean glowing, @Nullable Consumer<Furnace> function) {
        return createItemWithFurnace(name, FurnaceProperties.FURNACE, itemStack, glowing, function);
    }

    /**
     * Create a {@link Furnace} that is attached to an {@link ItemStack}
     *
     * @param name              Name of furnace (this will show up in the furnace UI)
     * @param furnaceProperties Properties associated with this furnace item
     * @param itemStack         ItemStack to be copied and have a furnace attached
     * @param glowing           Whether the item should glow (enchanted)
     * @param function          Function to run before furnace is created
     * @return Clone of the input ItemStack with a furnace attached
     */
    public ItemStack createItemWithFurnace(@NotNull String name, @NotNull FurnaceProperties furnaceProperties, @NotNull ItemStack itemStack, boolean glowing, @Nullable Consumer<Furnace> function) {
        ItemStack item = itemStack.clone();
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        if (glowing) {
            if (HAS_GLINT) {
                meta.setEnchantmentGlintOverride(true);
            } else {
                if (item.getType() == Material.ARROW) {
                    meta.addEnchant(SHARPNESS, 1, true);
                } else {
                    meta.addEnchant(INFINITY, 1, true);
                }
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
        }
        Furnace furnace;
        if (function == null) {
            furnace = createFurnace(name, furnaceProperties);
        } else {
            furnace = createFurnace(name, furnaceProperties, function);
        }
        meta.getPersistentDataContainer().set(this.key, PersistentDataType.STRING, furnace.getUniqueID().toString());
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Get a {@link Furnace} from an {@link ItemStack}
     *
     * @param itemStack ItemStack to grab furnace from
     * @return Furnace if the ItemStack has one assigned to it else null
     * @deprecated Use {@link #getMachineFromItemStack(ItemStack)} instead
     */
    @Deprecated(since = "1.1.0")
    public Furnace getFurnaceFromItemStack(@NotNull ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null && meta.getPersistentDataContainer().has(this.key, PersistentDataType.STRING)) {
            String u = meta.getPersistentDataContainer().get(this.key, PersistentDataType.STRING);
            if (u == null) return null;
            Machine machine = getByID(UUID.fromString(u));
            if (machine instanceof Furnace furnace) return furnace;
        }
        return null;
    }

    /**
     * Get a {@link Machine} from an {@link ItemStack}
     *
     * @param itemStack ItemStack to grab machine from
     * @return Machine if the ItemStack has one assigned to it else null
     */
    public Machine getMachineFromItemStack(@NotNull ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null && meta.getPersistentDataContainer().has(this.key, PersistentDataType.STRING)) {
            String u = meta.getPersistentDataContainer().get(this.key, PersistentDataType.STRING);
            if (u == null) return null;
            return getByID(UUID.fromString(u));
        }
        return null;
    }

    private void loadFurnaceConfig() {
        if (this.machineFile == null) {
            this.machineFile = new File(this.virtualFurnaceAPI.getJavaPlugin().getDataFolder(), "furnaces.yml");
        }
        if (!machineFile.exists()) {
            this.virtualFurnaceAPI.getJavaPlugin().saveResource("furnaces.yml", false);
        }
        this.machineConfig = YamlConfiguration.loadConfiguration(this.machineFile);
        loadFurnaces();
    }

    void loadFurnaces() {
        ConfigurationSection section = this.machineConfig.getConfigurationSection("furnaces");
        if (section != null) {
            for (String string : section.getKeys(true)) {
                if (section.get(string) instanceof Furnace furnace) {
                    this.machineMap.put(UUID.fromString(string), furnace);
                }
            }
        }
        Util.log("Loaded: &b" + this.machineMap.size() + "&7 furnaces");
    }

    /**
     * Save a machine to YAML storage
     * <p><b>NOTE:</b> If choosing not to save to file, this change will not take effect
     * in the YAML file, this may be useful for saving a large batch and saving file at the
     * end of the batch change, use {@link #saveConfig()} to save all changes to file</p>
     *
     * @param machine    Machine to save
     * @param saveToFile Whether to save to file
     */
    public void saveFurnace(@NotNull Machine machine, boolean saveToFile) {
        this.machineConfig.set("furnaces." + machine.getUniqueID(), machine);
        if (saveToFile)
            saveConfig();
    }

    /**
     * Remove a machine from YAML storage
     * <p><b>NOTE:</b> If choosing not to save to file, this change will not take effect
     * in the YAML file, this may be useful if removing a large batch and saving file at the
     * end of the batch change, use {@link #saveConfig()} to save all changes to file</p>
     *
     * @param machine    Machine to remove
     * @param saveToFile Whether to save changes to file
     */
    public void removeFurnaceFromConfig(@NotNull Machine machine, boolean saveToFile) {
        this.machineConfig.set("furnaces." + machine.getUniqueID(), null);
        if (saveToFile)
            saveConfig();
    }

    /**
     * Remove a {@link Machine}
     * <p>This will remove from memory and from file (if saving is true)
     *
     * @param machine    Machine to remove
     * @param saveToFile Whether to save to file
     */
    public void removeMachine(@NotNull Machine machine, boolean saveToFile) {
        this.machineMap.remove(machine.getUniqueID());
        removeFurnaceFromConfig(machine, saveToFile);
    }

    /**
     * Save all furnaces to file
     */
    public void saveAll() {
        for (Machine machine : this.machineMap.values()) {
            saveFurnace(machine, false);
        }
        saveConfig();
    }

    /**
     * Save current furnace YAML from RAM to file
     */
    @SuppressWarnings("CallToPrintStackTrace")
    public void saveConfig() {
        try {
            machineConfig.save(machineFile);
        } catch (ConcurrentModificationException ignore) {
            // TODO figure out a proper way to handle this exception and figure out why its happening
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void shutdown() {
        saveAll();
        machineMap.clear();
    }

}
