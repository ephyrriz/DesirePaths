package me.kermx.desirepaths.files;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;
import java.util.logging.Logger;

public class Config {
    private final FileConfiguration fileConfig;
    private final Logger logger;

    // General settings
    private List<World> disabledWorlds;
    private boolean creativeModeAllowed;
    private boolean movementCheckEnabled;
    private int attemptFrequency;

    // Modifiers settings
    private Map<String, Integer> chanceModifiers;
    private Map<String, Integer> additiveModifiers;
    private Map<String, Map<Material, Material>> blockModificationsMappings;

    // Integrations settings
    private Map<String, Map<String, Object>> integrationMappings;

    /**
     * Constructs the
     *
     * @param fileConfig The file configuration
     * @param logger     The logger
     */
    public Config(final FileConfiguration fileConfig, final Logger logger) {
        this.fileConfig = fileConfig;
        this.logger = logger;
        loadValues();
    }

    private void loadValues() {
        loadGeneralValues();
        loadModifiers();
        loadIntegrationSettings();
    }

    private void loadGeneralValues() {
        disabledWorlds = turnToWorlds();
        creativeModeAllowed = fileConfig.getBoolean("enableInCreativeMode", true);
        movementCheckEnabled = fileConfig.getBoolean("movementCheckEnabled", false);
        attemptFrequency = fileConfig.getInt("attemptFrequency");
    }

    private List<World> turnToWorlds() {
        final List<World> worlds = new ArrayList<>();
        final List<String> worldNames = fileConfig.getStringList("disabledWorlds");

        for (final String worldName : worldNames) {
            final World world = Bukkit.getWorld(worldName);

            if (world != null) {
                worlds.add(world);
            } else {
                logger.warning(String.format("The world %s is null", worldName));
            }
        }

        return worlds;
    }

    private void loadModifiers() {
        loadChanceModifiers();
        loadAdditiveModifiers();

        blockModificationsMappings = Map.of(
                "blockModifications.blockAtFeetModifications", new EnumMap<>(Material.class),
                "blockModifications.blockBelowModifications", new EnumMap<>(Material.class)
        );

        for (final Map.Entry<String, Map<Material, Material>> blockModificationsEntry : blockModificationsMappings.entrySet()) {
            final String path = blockModificationsEntry.getKey();
            final Map<Material, Material> map = blockModificationsEntry.getValue();
            loadBlockModifications(path, map);
        }
    }

    private void loadChanceModifiers() {
        chanceModifiers = new HashMap<>();
        final ConfigurationSection section = fileConfig.getConfigurationSection("chanceModifiers");

        if (section != null) {
            for (final String key : section.getKeys(false)) {
                chanceModifiers.put(key, section.getInt(key));
            }
        } else {
            logger.warning("The chanceModifiers section is null.");
        }
    }

    private void loadAdditiveModifiers() {
        additiveModifiers = new HashMap<>();
        final ConfigurationSection section = fileConfig.getConfigurationSection("additiveModifiers");

        if (section != null) {
            for (final String key : section.getKeys(false)) {
                additiveModifiers.put(key, section.getInt(key));
            }
        } else {
            logger.warning("The additiveModifiers is null.");
        }
    }

    private void loadBlockModifications(final String path, final Map<Material, Material> blockModificationMap) {
        final List<String> modifications = fileConfig.getStringList(path);

        if (!modifications.isEmpty()) {
            for (final String modification : modifications) {
                final String[] parts = modification.split(":");

                if (parts.length == 2) {
                    try {
                        final Material fromBlock = Material.valueOf(parts[0].toUpperCase());
                        final Material toBlock = Material.valueOf(parts[1].toUpperCase());

                        blockModificationMap.put(fromBlock, toBlock);
                    } catch (final IllegalArgumentException e) {
                        logger.warning("Invalid block type in " + path + ": " + modification);
                    }
                } else {
                    logger.warning("Invalid format in " + path + ": " + modification);
                }
            }
        } else {
            logger.warning("The " + path + " section is empty.");
        }
    }

    private void loadIntegrationSettings() {
         integrationMappings = Map.of(
                "townyModifiers", new HashMap<>(),
                "landsIntegrations", new HashMap<>(),
                "griefPreventionIntegration", new HashMap<>(),
                "coreProtectIntegrations", new HashMap<>()
        );

        for (final Map.Entry<String, Map<String, Object>> integrationSettingEntry : integrationMappings.entrySet()) {
            final String path = integrationSettingEntry.getKey();
            final Map<String, Object> map = integrationSettingEntry.getValue();
            loadIntegrationSettings(path, map);
        }
    }

    private void loadIntegrationSettings(final String path, final Map<String, Object> settingsMap) {
        final ConfigurationSection section = fileConfig.getConfigurationSection(path);

        if (section != null) {
            for (final String key : section.getKeys(false)) {
                settingsMap.put(key, section.get(key));
            }
        } else {
            logger.warning(String.format("The %s section is null.", path));
        }
    }

    /**
     * GETTERS SECTION
     */

    public List<World> getDisabledWorlds() {
        return disabledWorlds;
    }

    public boolean isCreativeModeAllowed() {
        return creativeModeAllowed;
    }

    public boolean isMovementCheckEnabled() {
        return movementCheckEnabled;
    }

    public int getAttemptFrequency() {
        return attemptFrequency;
    }

    public Map<String, Integer> getChanceModifiers() {
        return chanceModifiers;
    }

    public Map<String, Integer> getAdditiveModifiers() {
        return additiveModifiers;
    }

    public Map<String, Map<Material, Material>> getBlockModificationsMappings() {
        return blockModificationsMappings;
    }

    public Map<String, Map<String, Object>> getIntegrationMappings() {
        return integrationMappings;
    }
}
