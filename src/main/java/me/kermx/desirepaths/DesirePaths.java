package me.kermx.desirepaths;

import me.kermx.desirepaths.commands.DesirePathsCommand;
import me.kermx.desirepaths.files.Config;
import me.kermx.desirepaths.integrations.*;
import me.kermx.desirepaths.listeners.PlayerMoveEventListener;
import me.kermx.desirepaths.managers.ToggleManager;
import me.kermx.desirepaths.schedulers.PathScheduler;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.PluginCommand;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.logging.Logger;

// new features:
// speed boost when walking on paths
// papi placeholders, %desirepaths_toggle_status%, %desirepaths_maintenance_status%
// weather chance modifiers
// different block switch based on weather
// different block switch and/or biome chance modifiers
// itemsadder blocks?

public final class DesirePaths extends JavaPlugin implements Listener {
    private Config fileConfig;
    private Logger logger;

    private PlayerMoveEventListener playerMove;
    private TownyIntegration townyIntegration;
    private WorldGuardIntegration worldGuardIntegration;
    private LandsPathIntegration landsPathIntegration;
    private GriefPreventionIntegration griefPreventionIntegration;
    private CoreProtectIntegration coreProtectIntegration;

    public boolean townyEnabled;
    public boolean worldGuardEnabled;
    public boolean landsEnabled;
    public boolean griefPreventionEnabled;
    public boolean coreProtectEnabled;

    private ToggleManager toggleManager;

    @Override
    public void onLoad() {
        loadLogger();
        loadConfig();
        loadCommand();
        loadToggleManager();
        startScheduler();
        checkForDependencies();
    }

    /**
     * Loads config.
     */
    private void loadConfig() {
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
        fileConfig = new Config(getConfig(), getLogger());
    }

    /**
     * Loads logger.
     */
    private void loadLogger() {
        logger = getLogger();
    }

    private void loadToggleManager() {
        new ToggleManager(this);
    }

    /**
     * Loads command.
     */
    private void loadCommand() {
        final PluginCommand command = getCommand("desirepaths");
        final DesirePathsCommand desirePathsCommand = new DesirePathsCommand(this);

        if (command != null) {
            command.setExecutor(desirePathsCommand);
            command.setTabCompleter(desirePathsCommand);
        }
    }

    private void startScheduler() {
        new PathScheduler(this, playerMove, fileConfig).startScheduler();
    }

    /**
     * Checks for dependencies and adds them if they present.
     */
    private void checkForDependencies() {
        final PluginManager pluginManager = getServer().getPluginManager();

        if (pluginManager.getPlugin("Towny") != null){
            try {
                townyIntegration = new TownyIntegration(this);
            } catch (NoClassDefFoundError ignored){}
        }

        if (pluginManager.getPlugin("WorldGuard") != null) {
            try {
                worldGuardIntegration = new WorldGuardIntegration();
                worldGuardIntegration.preloadWorldGuardIntegration();
            } catch (NoClassDefFoundError ignored) {}
        }

        if (pluginManager.getPlugin("Lands") != null){
            try {
                landsPathIntegration = new LandsPathIntegration(this);
                landsPathIntegration.loadLandsIntegration();
            } catch (NoClassDefFoundError ignored){}
        }

        if (pluginManager.getPlugin("GriefPrevention") != null){
            try {
                griefPreventionIntegration = new GriefPreventionIntegration(this);
            } catch (NoClassDefFoundError ignored){}
        }

        if (pluginManager.getPlugin("CoreProtect") != null) {
            try {
                coreProtectIntegration = new CoreProtectIntegration(this);
            } catch (NoClassDefFoundError ignored) {}
        }
    }

    @Override
    public void onEnable() {
         // Plugin startup logic
        Bukkit.getConsoleSender()
                .sendMessage(ChatColor.GOLD + ">>" + ChatColor.GREEN + " DesirePaths " + getDescription().getVersion() + " enabled successfully");

        townyEnabled = Bukkit.getPluginManager().isPluginEnabled("Towny");
        if (townyEnabled) {
            Bukkit.getConsoleSender()
                    .sendMessage(ChatColor.GOLD + ">>" + ChatColor.GREEN + " DesirePaths-Towny integration successful");
        }

        worldGuardEnabled = Bukkit.getPluginManager().isPluginEnabled("WorldGuard");
        if (worldGuardEnabled) {
            Bukkit.getConsoleSender().sendMessage(
                    ChatColor.GOLD + ">>" + ChatColor.GREEN + " DesirePaths-WorldGuard integration successful");
        }

        landsEnabled = Bukkit.getPluginManager().isPluginEnabled("Lands");
        if (landsEnabled) {
            Bukkit.getConsoleSender().sendMessage(
                    ChatColor.GOLD + ">>" + ChatColor.GREEN + " DesirePaths-Lands integration successful");
        }

        griefPreventionEnabled = Bukkit.getPluginManager().isPluginEnabled("GriefPrevention");
        if (griefPreventionEnabled){
            Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + ">>" + ChatColor.GREEN + " DesirePaths-GriefPrevention integration successful");
        }

        coreProtectEnabled = Bukkit.getPluginManager().isPluginEnabled("CoreProtect");
        if (coreProtectEnabled && coreProtectIntegration.getAPI() != null){
            Bukkit.getConsoleSender()
                    .sendMessage(ChatColor.GOLD + ">>" + ChatColor.GREEN + " DesirePaths-CoreProtect integration successful");
        }
    }

    private void playerHandler(Player player, int noBootsChance, int leatherBootsChance, int hasBootsChance,
                               int featherFallingChance, int ridingHorseChance, int ridingBoatChance, int ridingPigChance,
                               int sprintingBlockBelowChance, int sprintingBlockAtFeetChance, int crouchingBlockBelowChance,
                               int crouchingBlockAtFeetChance, List<String> blockAtFeetSwitcherConfig, List<String> blockBelowSwitcherConfig) {
        if (player.getGameMode() != GameMode.SURVIVAL && !enableInCreativeMode) {
            return;
        }
        boolean pathsToggledOff = !getToggleManager().getToggle(player.getUniqueId());
        if (pathsToggledOff) {
            return;
        }
        int chance = getChance(player, noBootsChance, leatherBootsChance, hasBootsChance, featherFallingChance,
                ridingHorseChance, ridingBoatChance, ridingPigChance);
        int randomNum = random.nextInt(100);
        Bukkit.getScheduler().runTask(this,
                () -> blockHandler(player.getLocation().getBlock().getRelative(BlockFace.DOWN), player, chance,
                        randomNum, sprintingBlockBelowChance, crouchingBlockBelowChance, blockBelowSwitcherConfig));
        Bukkit.getScheduler().runTask(this,
                () -> blockHandler(player.getLocation().getBlock(), player, chance,
                        randomNum, sprintingBlockAtFeetChance, crouchingBlockAtFeetChance, blockAtFeetSwitcherConfig));
    }

    public static int getChance(Player player, int noBootsChance, int leatherBootsChance, int hasBootsChance,
                                int featherFallingChance, int ridingHorseChance, int ridingBoatChance, int ridingPigChance) {
        return switch (getModifier(player)) {
            case RIDING_HORSE -> ridingHorseChance;
            case RIDING_BOAT -> ridingBoatChance;
            case RIDING_PIG -> ridingPigChance;
            case FEATHER_FALLING -> featherFallingChance;
            case HAS_BOOTS -> hasBootsChance;
            case LEATHER_BOOTS -> leatherBootsChance;
            case NO_BOOTS -> noBootsChance;
        };
    }

    // determine modifier to use for chance
    private static modifierType getModifier(Player player) {
        if (player.getVehicle() instanceof AbstractHorse)
            return modifierType.RIDING_HORSE;
        if (player.getVehicle() instanceof Boat)
            return modifierType.RIDING_BOAT;
        if (player.getVehicle() instanceof Pig)
            return modifierType.RIDING_PIG;
        ItemStack boots = player.getInventory().getBoots();
        if (boots == null)
            return modifierType.NO_BOOTS;
        Material bootMaterial = boots.getType();
        Set<Material> bootMaterials = EnumSet.of(Material.IRON_BOOTS, Material.GOLDEN_BOOTS, Material.DIAMOND_BOOTS,
                Material.NETHERITE_BOOTS, Material.LEATHER_BOOTS);
        Optional<Enchantment> featherFallingEnchantment = boots.getEnchantments().keySet().stream()
                .filter(Enchantment.PROTECTION_FALL::equals).findFirst();
        if (bootMaterials.contains(bootMaterial)) {
            if (featherFallingEnchantment.isPresent()) {
                return modifierType.FEATHER_FALLING;
            } else {
                return modifierType.HAS_BOOTS;
            }
        } else if (bootMaterial == Material.LEATHER_BOOTS) {
            return modifierType.LEATHER_BOOTS;
        } else {
            return modifierType.NO_BOOTS;
        }
    }

    // Handle block at the players feet
    private void blockHandler(Block block, Player player, int chance, int randomNum, int sprintingChance,
                              int crouchingChance, List<String> switcherConfig) {
        if (!canModifyBlock(player,block)){
            return;
        }

        if (!player.isSprinting() && !player.isSneaking() && randomNum < chance) {
            blockSwitcher(block, switcherConfig, player);
        }
        if (player.isSprinting() && randomNum < chance + sprintingChance) {
            blockSwitcher(block, switcherConfig, player);
        }
        if (player.isSneaking() && randomNum < chance + crouchingChance){
            blockSwitcher(block, switcherConfig, player);
        }
    }

    private boolean canModifyBlock(Player player, Block block) {
        if (toggleManager.getMaintenanceMode() ||
                disabledWorlds.contains(player.getWorld().getName()) ||
                player.getLocation().getY() % 1 != 0) {
            return false;
        }
        Block blockAbove = block.getRelative(BlockFace.UP);
        if (blockAbove.getType() == Material.RAIL || blockAbove.getType() == Material.POWERED_RAIL || blockAbove.getType() == Material.ACTIVATOR_RAIL || blockAbove.getType() == Material.DETECTOR_RAIL) {
            return false;
        }
        if (worldGuardEnabled && worldGuardIntegration.checkFlag(player)) {
            return false;
        }
        if (landsEnabled && landsPathIntegration.checkFlag(player)) {
            return false;
        }
        if (griefPreventionEnabled && !griefPreventionIntegration.checkLocation(player, block.getLocation())) {
            return false;
        }
        if (townyEnabled && !townyIntegration.checkLocation(player, block.getLocation())) {
            return false;
        }
        return true;
    }

    private void blockSwitcher(Block block, List<String> switcherConfig, Player player) {
        Material type = block.getType();
        Map<Material, Material> blockSwitcher = new HashMap<>();
        for (String switchCase : switcherConfig) {
            String[] parts = switchCase.split(":");
            Material sourceMaterial = Material.matchMaterial(parts[0]);
            Material targetMaterial = Material.matchMaterial(parts[1]);
            if (sourceMaterial != null && targetMaterial != null) {
                blockSwitcher.put(sourceMaterial, targetMaterial);
            } else {
                Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + ">>" + ChatColor.RED
                        + " Invalid block switch case in blockModifications: " + switchCase);
            }
        }
        Material targetMaterial = blockSwitcher.get(type);
        if (targetMaterial != null) {
            block.setType(targetMaterial);
            //coreprotect logging
            if (block.getType() == targetMaterial){
                if (coreProtectEnabled && logPathsToCoreProtect){
                    coreProtectIntegration.logPathChangesToCoreProtectRemoval(player, block.getLocation(), type, block.getBlockData());
                    coreProtectIntegration.logPathChangesToCoreProtectPlacement(player, block.getLocation(), targetMaterial, block.getBlockData());
                }
            }
        }
    }

    public ToggleManager getToggleManager() {
        return toggleManager;
    }

    @Override
    public void onDisable() {

        Bukkit.getScheduler().cancelTasks(this);
        Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + ">>" + ChatColor.RED + " DesirePaths Disabled");
    }
}