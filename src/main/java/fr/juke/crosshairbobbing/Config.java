package fr.juke.crosshairbobbing;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Client config. Version 1.
 *
 * <p>Several booleans control which animations are enabled. Keep the key names stable:
 * users edit this file by hand, and the README documents every key.</p>
 */
public final class Config {
    public static final int CURRENT_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Config instance;

    public int configVersion = CURRENT_VERSION;
    public boolean enabled = true;
    public boolean followBobbing = true;
    public boolean sneakAnimation = true;
    public boolean itemSwitchAnimation = true;
    public boolean swingAnimation = true;
    public boolean attackAnimation = true;
    public boolean eatingAnimation = true;
    public boolean crossbowAnimation = true;
    public boolean bowAimAnimation = true;
    public boolean tridentSpinAnimation = true;

    public static Config get() {
        if (instance == null) {
            instance = new Config();
        }
        return instance;
    }

    public static void load(Logger logger) {
        Path path = file();
        Config loaded = null;
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                loaded = GSON.fromJson(reader, Config.class);
            } catch (Exception e) {
                logger.warn("Could not read {}, using defaults", path, e);
            }
        }
        if (loaded == null) {
            loaded = new Config();
        }
        loaded.sanitize();
        instance = loaded;
        save(logger);
    }

    public static void save(Logger logger) {
        Path path = file();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(get(), writer);
            }
        } catch (IOException e) {
            logger.warn("Could not write {}", path, e);
        }
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("crosshair-bobbing-continued.json");
    }

    private void sanitize() {
        if (configVersion < 1) {
            configVersion = 1;
        }
    }
}
