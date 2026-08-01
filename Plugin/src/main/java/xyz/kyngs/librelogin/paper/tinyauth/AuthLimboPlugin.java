/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.paper.tinyauth;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.Random;

/**
 * Minimal backend-only lockdown for the Paper server used as LibreLogin's
 * Velocity limbo. LibreLogin itself must remain installed only on Velocity.
 */
public final class AuthLimboPlugin extends JavaPlugin implements Listener {

    // Keep the old `limbo` world untouched: it was generated as a normal
    // world before this plugin existed. This fresh world is always void.
    private static final String LIMBO_WORLD = "auth_void";
    private World limbo;

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        return "void".equalsIgnoreCase(id) ? new VoidGenerator() : null;
    }

    @Override
    public void onEnable() {
        // Register immediately so no player can move during deferred world
        // initialization. Paper forbids creating extra worlds from a STARTUP
        // plugin, so world setup itself runs one tick later.
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getScheduler().runTask(this, this::initializeLimbo);
    }

    private void initializeLimbo() {
        limbo = Bukkit.getWorld(LIMBO_WORLD);
        if (limbo == null) {
            getLogger().info("Creating missing fresh void world " + LIMBO_WORLD);
            limbo = Bukkit.createWorld(new WorldCreator(LIMBO_WORLD).generator(new VoidGenerator()));
        }

        if (limbo == null) {
            getLogger().severe("Could not load or create the limbo world; disabling AuthLimbo");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        limbo.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, false);
        limbo.setGameRule(org.bukkit.GameRule.DO_WEATHER_CYCLE, false);
        limbo.setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING, false);
        limbo.setGameRule(org.bukkit.GameRule.DO_INSOMNIA, false);
        limbo.setPVP(false);
        limbo.setSpawnLocation(0, 70, 0);

        getLogger().info("AuthLimbo enabled; players are locked in world " + LIMBO_WORLD);

        for (Player player : Bukkit.getOnlinePlayers()) {
            prepare(player);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        prepare(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedPosition()) return;
        event.setCancelled(true);
        event.setTo(event.getFrom());
        event.getPlayer().setVelocity(new Vector(0, 0, 0));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getTo() == null || !event.getTo().getWorld().equals(limbo)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBreak(BlockBreakEvent event) {
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlace(BlockPlaceEvent event) {
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrop(PlayerDropItemEvent event) {
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventory(InventoryClickEvent event) {
        event.setCancelled(true);
    }

    private void prepare(Player player) {
        if (limbo == null) return;
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(true);
        // Keep the player at the void spawn instead of letting gravity move
        // them before the movement correction arrives from the server.
        player.setFlying(true);
        player.setWalkSpeed(0.0f);
        player.setFlySpeed(0.0f);
        if (!player.getWorld().equals(limbo)) {
            player.teleportAsync(limbo.getSpawnLocation());
        }
    }

    private static final class VoidGenerator extends ChunkGenerator {
        @Override
        public void generateSurface(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkData chunkData) {
            // Intentionally empty: no terrain, structures, or floor.
        }

        @Override
        public void generateNoise(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkData chunkData) {
            // Prevent vanilla terrain from being generated around the spawn.
        }
    }
}
