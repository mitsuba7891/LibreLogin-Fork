/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.paper;

import net.byteflux.libby.BukkitLibraryManager;
import net.byteflux.libby.LibraryManager;
import net.byteflux.libby.PaperLibraryManager;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.kyngs.librelogin.api.provider.LibreLoginProvider;

public class PaperBootstrap extends JavaPlugin implements LibreLoginProvider<Player, World> {

    private PaperLibreLogin libreLogin;

    /**
     * Set when the server distribution cannot run LibreLogin at all. The plugin
     * is disabled in {@link #onEnable()} instead of terminating the server.
     */
    private boolean unusable;

    @Override
    public @Nullable ChunkGenerator getDefaultWorldGenerator(@NotNull String worldName, @Nullable String id) {
        return id == null ?
                null
                : id.equals("void") ? new VoidWorldGenerator() : null;
    }

    @Override
    public void onLoad() {
        getLogger().info("Analyzing server setup...");
        initialize();
    }

    @Override
    public void onEnable() {
        getLogger().info("Bootstrapping LibreLogin...");
        if (libreLogin == null) {
            getLogger().warning("LibreLogin was not initialized during onLoad; initializing during onEnable.");
        }
        initialize();

        if (libreLogin == null) {
            getLogger().severe("LibreLogin could not be initialised and stays disabled; see the errors above.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        libreLogin.enable();
    }

    private void initialize() {
        if (unusable || libreLogin != null) {
            return;
        }

        getLogger().info("Initializing LibreLogin...");

        try {
            var adventureClass = Class.forName("net.kyori.adventure.audience.Audience");

            if (!adventureClass.isAssignableFrom(Player.class)) {
                throw new ClassNotFoundException();
            }
        } catch (ClassNotFoundException e) {
            unsupportedSetup();
            return;
        }

        getLogger().info("Detected Adventure-compatible server distribution - " + getServer().getName() + " " + getServer().getVersion());

        LibraryManager libraryManager;

        try {
            Class.forName("io.papermc.paper.plugin.entrypoint.classloader.PaperPluginClassLoader");
            libraryManager = new PaperLibraryManager(this);
        } catch (ClassNotFoundException e) {
            libraryManager = new BukkitLibraryManager(this);
        }

        getSLF4JLogger().info("Loading libraries...");

        try {
            libraryManager.configureFromJSON();
        } catch (Exception e) {
            // Library loading needs outbound HTTPS. Refusing to start is correct,
            // but terminating the server over it is not: the operator may be
            // running behind a firewall that has nothing to do with LibreLogin.
            getSLF4JLogger().error("Failed to load libraries; LibreLogin will stay disabled", e);
            unusable = true;
            return;
        }

        libreLogin = new PaperLibreLogin(this);
    }

    private void unsupportedSetup() {
        getLogger().severe("***********************************************************");

        getLogger().severe("Detected an unsupported server distribution. Please use Paper or its forks. SPIGOT IS NOT SUPPORTED!");

        getLogger().severe("***********************************************************");

        getLogger().severe("LibreLogin is disabled and will not authenticate anyone.");

        unusable = true;
    }

    @Override
    public void onDisable() {
        if (libreLogin == null) {
            getLogger().warning("LibreLogin was not initialized, skipping disable.");
            return;
        }
        libreLogin.disable();
    }

    @Override
    public PaperLibreLogin getLibreLogin() {
        return libreLogin;
    }

    protected void disable() {
        setEnabled(false);
    }

}
