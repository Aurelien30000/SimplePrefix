package com.flabaliki.simpleprefix;

import net.milkbowl.vault.chat.Chat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerChatPreviewEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class SimplePrefix extends JavaPlugin implements Listener {
    static String pluginName;
    static File pluginFolder;
    static String pluginVersion;
    protected static final Logger log = Bukkit.getLogger();
    static String template;
    static String timeFormat;
    static Boolean multiPrefix;
    static String multiPrefixSeparator;
    static Boolean multiSuffix;
    static String multiSuffixSeparator;
    Config config = new Config(this);
    SprCommand commands = new SprCommand(this);
    BukkitScheduler scheduler = null;
    ConcurrentHashMap<String, UUID> uuids = new ConcurrentHashMap<>();
    public Chat chat = null;
    boolean useVault = false;
    static Boolean debug;
    static Boolean allowOps;
    static Boolean useUUID = true;

    public void onEnable() {
        pluginName = getDescription().getName();
        pluginFolder = getDataFolder();
        pluginVersion = getDescription().getVersion();
        config.firstRun();
        commands.initialise();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("spr").setExecutor(commands);
        scheduler = Bukkit.getScheduler();

        if (useUUID && Bukkit.getOnlinePlayers().size() > 0) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                uuids.put(p.getName(), p.getUniqueId());
            }
        }

        if (Config.config.getBoolean("Use-Vault"))
            setupChat();
    }

    private void setupChat() {
        try {
            RegisteredServiceProvider<Chat> rsp = getServer().getServicesManager().getRegistration(Chat.class);
            if (rsp == null) return;
            chat = rsp.getProvider();
            useVault = true;
            log.info("[" + pluginName + "] " + chat.getName() + " has been detected, through Vault. Simple Prefix will get prefixes and suffixes from it.");
        } finally {
            if (!useVault) {
                log.warning("[" + pluginName + "] Use-Vault was enabled in the Simple Prefix config, but there was a problem accessing it.");
                log.warning("[" + pluginName + "] Make sure you have Vault, and a Vault compatible permissions/chat plugin installed.");
            }

        }
    }

    @EventHandler
    public void onAsyncPlayerChatEvent(AsyncPlayerChatEvent event) {
        handleChat(event);
    }

    @EventHandler
    public void onAsyncPlayerChatEvent(AsyncPlayerChatPreviewEvent event) {
        handleChat(event);
    }

    private void handleChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String prefix = config.getPrefix(player);
        String suffix = config.getSuffix(player);
        String world = config.getWorld(player);
        String message = event.getMessage().replaceAll("%", "%%");
        if (template == null) template = "<[time] [world] [prefix][name][suffix]> ";
        if (timeFormat == null) timeFormat = "[h:mm aa]";
        String formattedName = template
                .replaceAll("\\[world]", world)
                .replaceAll("\\[prefix]", prefix)
                .replaceAll("\\[name]", player.getDisplayName())
                .replaceAll("\\[suffix]", suffix)
                .replaceAll("(&([A-Fa-f0-9L-Ol-okKrR]))", "§$2");
        if ((timeFormat != null) && (!timeFormat.equalsIgnoreCase("")) && (formattedName.contains("[time]"))) {
            DateFormat dateFormat = new SimpleDateFormat(timeFormat);
            Date date = new Date();
            formattedName = formattedName.replaceAll("\\[time]", dateFormat.format(date));
        }
        formattedName = formattedName.replaceAll("\\s+", " ");
        event.setFormat(formattedName + message);
    }

    @EventHandler
    public void onAsyncPlayerPreLoginEvent(AsyncPlayerPreLoginEvent event) {
        if (useUUID)
            uuids.put(event.getName(), event.getUniqueId());
    }
}