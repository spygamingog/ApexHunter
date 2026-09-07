package com.spygamingog.spyhunts.commands;

import com.spygamingog.spyhunts.SpyHuntsPlugin;
import com.spygamingog.spyhunts.lobby.LobbyManager;
import com.spygamingog.spyhunts.slots.SlotManager;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LeaveCommand implements CommandExecutor {
    private final SpyHuntsPlugin plugin;
    public LeaveCommand(SpyHuntsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        SlotManager sm = plugin.getSlotManager();
        
        // If in an active game, treat as quit
        String modeId = plugin.getPlayerDataManager().getActiveModeId(p.getUniqueId());
        String slotId = plugin.getPlayerDataManager().getActiveSlotId(p.getUniqueId());
        com.spygamingog.spyhunts.slots.GameType type = plugin.getPlayerDataManager().getActiveType(p.getUniqueId());
        if (modeId != null && slotId != null && type != null) {
            sm.endGameComplete(modeId, slotId, type, com.spygamingog.spyhunts.slots.WinnerType.QUIT);
            return true;
        }

        sm.leaveAnyQueue(p);
        LobbyManager lm = plugin.getLobbyManager();
        if (lm.teleportToMainLobby(p)) {
            p.sendMessage("Returned to manhunt lobby.");
        } else {
            if (!plugin.getConfig().getBoolean("lobby_enabled", false)) {
                p.sendMessage("Lobby features are disabled.");
            } else {
                p.sendMessage("Main manhunt lobby is not set.");
            }
        }
        return true;
    }
}
