package com.winthier.toomanyentities;

import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.metadata.Metadatable;
import org.bukkit.plugin.java.JavaPlugin;

@RequiredArgsConstructor
final class Metadata {
    final JavaPlugin plugin;
    static final String SESSION = "tme:session";

    <T> T get(final Metadatable entity,
                        final String key,
                        final Class<T> theClass) {
        for (MetadataValue meta : entity.getMetadata(key)) {
            if (meta.getOwningPlugin() == plugin) {
                Object value = meta.value();
                if (!theClass.isInstance(value)) {
                    return null;
                }
                return theClass.cast(value);
            }
        }
        return null;
    }

    void set(final Metadatable entity,
             final String key,
             final Object value) {
        entity.setMetadata(key, new FixedMetadataValue(plugin, value));
    }

    void remove(final Metadatable entity, final String key) {
        entity.removeMetadata(key, plugin);
    }

    /**
     * {@link Metadatable::hasMetadata(String)} may be preferable.
     */
    boolean has(final Metadatable entity,
                final String key) {
        for (MetadataValue meta : entity.getMetadata(key)) {
            if (meta.getOwningPlugin() == plugin) {
                return true;
            }
        }
        return false;
    }

    Session sessionOf(Player player) {
        Session result = get(player, SESSION, Session.class);
        if (result != null) return result;
        result = new Session();
        set(player, SESSION, result);
        return result;
    }

    void clearSession(Player player) {
        remove(player, SESSION);
    }
}
