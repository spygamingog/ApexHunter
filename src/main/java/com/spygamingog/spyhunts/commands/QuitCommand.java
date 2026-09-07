package com.spygamingog.spyhunts.commands;

import com.spygamingog.spyhunts.SpyHuntsPlugin;
import com.spygamingog.spyhunts.data.PlayerDataManager;
import com.spygamingog.spyhunts.lobby.LobbyManager;
import com.spygamingog.spyhunts.slots.SlotManager;
import com.spygamingog.spyhunts.slots.WinnerType;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class QuitCommand implements CommandExecutor {
    private final SpyHuntsPlugin plugin;
    public QuitCommand(SpyHuntsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        PlayerDataManager pd = plugin.getPlayerDataManager();
        SlotManager sm = plugin.getSlotManager();
        String modeId = pd.getActiveModeId(p.getUniqueId());
        String slotId = pd.getActiveSlotId(p.getUniqueId());
        com.spygamingog.spyhunts.slots.GameType type = pd.getActiveType(p.getUniqueId());
        if (modeId != null && slotId != null && type != null) {
            sm.endGameComplete(modeId, slotId, type, WinnerType.QUIT);
            return true;
        }
        LobbyManager lm = plugin.getLobbyManager();
        if (lm.teleportToMainLobby(p)) {
            p.sendMessage("You quit manhunt. Cooldown applied.");
        } else {
            if (!plugin.getConfig().getBoolean("lobby_enabled", false)) {
                p.sendMessage("You quit manhunt. Lobby features are disabled.");
            } else {
                p.sendMessage("Main manhunt lobby is not set.");
            }
        }
        return true;
    }
}
