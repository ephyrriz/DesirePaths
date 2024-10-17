package me.kermx.desirepaths.schedulers;

import me.kermx.desirepaths.DesirePaths;
import me.kermx.desirepaths.files.Config;
import me.kermx.desirepaths.listeners.PlayerMoveEventListener;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PathScheduler {
    private final DesirePaths plugin;
    private final PlayerMoveEventListener playerMove;
    private final Config fileConfig;

    public PathScheduler(final DesirePaths plugin, final PlayerMoveEventListener playerMove, final Config fileConfig) {
        this.plugin = plugin;
        this.playerMove = playerMove;
        this.fileConfig = fileConfig;
    }

    public void startScheduler() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(
                plugin,
                this::processPlayers,
                0L,
                fileConfig.getAttemptFrequency()
        );
    }

    private void processPlayers() {
        for (final Player player : Bukkit.getOnlinePlayers()) {
            if (fileConfig.isMovementCheckEnabled()) {
                final UUID playerId = player.getUniqueId();

                if (playerMove.getMovedPlayers().containsKey(playerId)) {
                    // Do action
                    playerHandler(playerId);
                }
            }
        }
    }

    private void playerHandler(final UUID playerId) {
        playerMove.getMovedPlayers().remove(playerId);
    }
}
