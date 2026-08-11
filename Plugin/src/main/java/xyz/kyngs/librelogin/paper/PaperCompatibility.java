/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.paper;

import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;

/**
 * Isolates Bukkit API names that changed after the oldest supported protocol
 * line. Reflection here is intentional: directly linking modern constants
 * would prevent the legacy plugin descriptor from loading on older servers.
 */
final class PaperCompatibility {

    private PaperCompatibility() {
    }

    static void setBooleanGameRule(World world, String modernName, String legacyName, boolean value) {
        try {
            world.setGameRule(resolveGameRule(modernName, legacyName), value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to resolve game rule " + legacyName, exception);
        }
    }

    static void keepSpawnChunkLoaded(World world, Plugin plugin) {
        var spawn = world.getSpawnLocation();
        try {
            World.class.getMethod("addPluginChunkTicket", int.class, int.class, Plugin.class)
                    .invoke(world, spawn.getBlockX() >> 4, spawn.getBlockZ() >> 4, plugin);
            return;
        } catch (NoSuchMethodException ignored) {
            // Bukkit 1.13 uses a world-level spawn retention toggle.
        } catch (IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException("Unable to retain the limbo spawn chunk", exception);
        }

        try {
            World.class.getMethod("setKeepSpawnInMemory", boolean.class).invoke(world, true);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to retain the legacy limbo spawn", exception);
        }
    }

    static double maximumHealth(Player player) {
        try {
            Attribute attribute;
            try {
                attribute = (Attribute) Attribute.class.getField("MAX_HEALTH").get(null);
            } catch (NoSuchFieldException ignored) {
                attribute = (Attribute) Attribute.class.getField("GENERIC_MAX_HEALTH").get(null);
            }
            var instance = player.getAttribute(attribute);
            return instance == null ? 20.0D : instance.getValue();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to resolve the maximum-health attribute", exception);
        }
    }

    static Location respawnLocation(Player player) {
        try {
            try {
                return (Location) Player.class.getMethod("getRespawnLocation").invoke(player);
            } catch (NoSuchMethodException ignored) {
                return (Location) Player.class.getMethod("getBedSpawnLocation").invoke(player);
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to resolve the player's respawn location", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private static GameRule<Boolean> resolveGameRule(String modernName, String legacyName)
            throws ReflectiveOperationException {
        try {
            var gameRules = Class.forName("org.bukkit.GameRules");
            return (GameRule<Boolean>) gameRules.getField(modernName).get(null);
        } catch (ClassNotFoundException | NoSuchFieldException ignored) {
            return (GameRule<Boolean>) GameRule.class.getField(legacyName).get(null);
        }
    }
}
