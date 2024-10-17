package me.kermx.desirepaths.listeners;

import me.kermx.desirepaths.files.Config;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerMoveEventListener implements Listener {
    private final Config fileConfig;
    private final boolean movementCheckEnabled;

    private final Map<UUID, Boolean> movedPlayers;

    public PlayerMoveEventListener(final Config fileConfig) {
        this.fileConfig = fileConfig;
        movementCheckEnabled = fileConfig.isMovementCheckEnabled();
        movedPlayers = new HashMap<>();
    }

    @EventHandler
    public void on(final PlayerMoveEvent event) {
        if (movementCheckEnabled) {
            final UUID playerId = event.getPlayer().getUniqueId();
            movedPlayers.putIfAbsent(playerId, true);
        }
    }

    public Map<UUID, Boolean> getMovedPlayers() {
        return movedPlayers;
    }
}
