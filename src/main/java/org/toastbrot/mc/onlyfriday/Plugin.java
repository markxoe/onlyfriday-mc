package org.toastbrot.mc.onlyfriday;

import java.util.Calendar;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Plugin extends JavaPlugin implements Listener {
  FileConfiguration config = getConfig();

  @Override
  public void onEnable() {
    Bukkit.getServer().getPluginManager().registerEvents(this, this);

    if (!getDataFolder().exists()) {
      getDataFolder().mkdirs();
    }
    config = getConfig();

    setDefault("i18n.kickmsg", "It's not Friday!");
    setDefault("i18n.you-only-friday", "You can now only join on Friday");
    setDefault("i18n.you-always", "You can now join anytime");
    setDefault("i18n.xy-only-friday", "Player {player} can now only join on Friday");
    setDefault("i18n.xy-always", "Player {player} can now join anytime");

    saveConfig();
  }

  private void setDefault(String path, Object value) {
    if (!config.isSet(path))
      config.set(path, value);
  }

  private void saveLastKnownUsername(Player player) {
    String id = player.getUniqueId().toString();
    if (!config.isSet("player." + id + ".enabled"))
      config.set("player." + id + ".enabled", false);
    config.set("player." + id + ".username", player.getName());
    saveConfig();
  }

  private boolean isFriday() {
    Calendar c = Calendar.getInstance();
    return c.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY;
  }

  private boolean isPlayerEnabled(String id) {
    String path = "player." + id + ".enabled";
    return config.isSet(path) && config.getBoolean(path);
  }

  private boolean isPlayerEnabled(Player player) {
    return isPlayerEnabled(player.getUniqueId().toString());
  }

  private void kickPlayer(Player player) {
    player.kickPlayer("It's not Friday!");
  }

  @SuppressWarnings("deprecation")
  private String findUserIDByName(String name) {
    Player onlinePlayer = this.getServer().getPlayer(name);
    if (onlinePlayer != null) {
      return onlinePlayer.getUniqueId().toString();
    }

    OfflinePlayer offlinePlayer = this.getServer().getOfflinePlayer(name);
    if (offlinePlayer != null) {
      return offlinePlayer.getUniqueId().toString();
    }

    if (!config.isSet("player")) {
      return null;
    }

    Set<String> playerIDs = config.getConfigurationSection("player").getKeys(false);
    for (String id : playerIDs) {
      if (config.getString("player." + id + ".username").equals(name)) {
        return id;
      }
    }

    return null;
  }

  private boolean toggleUser(String id) {
    boolean enabled = isPlayerEnabled(id);
    enabled = !enabled;
    config.set("player." + id + ".enabled", enabled);
    saveConfig();
    return enabled;
  }

  @EventHandler
  public void onPlayerLogin(PlayerLoginEvent e) {
    saveLastKnownUsername(e.getPlayer());
    if (isPlayerEnabled(e.getPlayer())) {
      if (!isFriday()) {
        kickPlayer(e.getPlayer());
        e.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, config.getString("i18n.kickmsg"));
      }
    }
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (command.getName().equalsIgnoreCase("toggleFriday")) {
      if (!(sender instanceof Player)) {
        sender.sendMessage("This command can only be run by a player.");
        return true;
      }
      Player player = (Player) sender;
      if (!command.testPermission(sender)) {
        sender.sendMessage("No Permission!");
        return false;
      }
      String id = player.getUniqueId().toString();

      boolean enabled = toggleUser(id);

      if (enabled)
        sender.sendMessage(config.getString("i18n.you-only-friday"));
      else
        sender.sendMessage(config.getString("i18n.you-always"));

      return true;
    } else if (command.getName().equalsIgnoreCase("toggleOtherFriday")) {

      if (args.length != 1) {
        return false;
      }

      String id;
      Player player = Bukkit.getPlayer(args[0]);

      if (player != null) {
        id = player.getUniqueId().toString();
      } else {
        id = findUserIDByName(args[0]);
      }

      if (id == null) {
        sender.sendMessage("Player does not exist");
        return true;
      }

      boolean enabled = toggleUser(id);

      if (enabled)
        sender.sendMessage(config.getString("i18n.xy-only-friday").replace("{player}", args[0]));
      else
        sender.sendMessage(config.getString("i18n.xy-always").replace("{player}", args[0]));

      return true;
    }
    return false;
  }
}
