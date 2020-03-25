package com.winthier.toomanyentities;

import org.bukkit.plugin.java.JavaPlugin;

public final class TooManyEntitiesPlugin extends JavaPlugin {
    TMECommand command = new TMECommand(this);
    Metadata metadata = new Metadata(this);

    @Override
    public void onEnable() {
        getCommand("tme").setExecutor(command);
    }
}
