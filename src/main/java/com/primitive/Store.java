package com.primitive;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Saves everything to  .minecraft/config/primitive/macros.json  and  settings.json
 * so macros survive leaving the world / closing the game.
 */
public final class Store {
    private Store() {}

    private static final Logger LOG = LoggerFactory.getLogger("Primitive");
    private static final Gson PRETTY = new GsonBuilder().setPrettyPrinting().create();
    private static final Gson COMPACT = new Gson();

    public static final List<Macro> MACROS = new ArrayList<>();

    public static class Settings {
        public boolean loop = false;
        public boolean relativeAim = false;
        public boolean hud = true;
        public String selected = "";
    }

    public static Settings settings = new Settings();

    private static Path dir() {
        return FabricLoader.getInstance().getConfigDir().resolve("primitive");
    }

    // ---------------------------------------------------------------- load / save

    public static void load() {
        try {
            Files.createDirectories(dir());

            Path macros = dir().resolve("macros.json");
            if (Files.exists(macros)) {
                try {
                    Macro[] arr = COMPACT.fromJson(Files.readString(macros), Macro[].class);
                    if (arr != null) {
                        for (Macro m : arr) {
                            if (m != null && m.valid()) MACROS.add(m);
                        }
                    }
                } catch (Exception e) {
                    LOG.error("macros.json could not be read - keeping a backup copy as macros.json.bak", e);
                    Files.copy(macros, dir().resolve("macros.json.bak"), StandardCopyOption.REPLACE_EXISTING);
                }
            }

            Path set = dir().resolve("settings.json");
            if (Files.exists(set)) {
                Settings loaded = PRETTY.fromJson(Files.readString(set), Settings.class);
                if (loaded != null) {
                    if (loaded.selected == null) loaded.selected = "";
                    settings = loaded;
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to load Primitive data", e);
        }
    }

    public static void saveMacros() {
        write(dir().resolve("macros.json"), COMPACT.toJson(MACROS.toArray(new Macro[0])));
    }

    public static void saveSettings() {
        write(dir().resolve("settings.json"), PRETTY.toJson(settings));
    }

    private static void write(Path target, String json) {
        try {
            Files.createDirectories(target.getParent());
            Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
            Files.writeString(tmp, json);
            try {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            LOG.error("Failed to write " + target, e);
        }
    }

    // ---------------------------------------------------------------- macro helpers

    public static Macro selected() {
        for (Macro m : MACROS) {
            if (m.id.equals(settings.selected)) return m;
        }
        return null;
    }

    public static void select(Macro m) {
        settings.selected = m == null ? "" : m.id;
        saveSettings();
    }

    public static void add(Macro m) {
        MACROS.add(m);
        saveMacros();
        select(m);
    }

    public static void rename(Macro m, String newName) {
        m.name = uniqueName(newName, m);
        saveMacros();
    }

    public static void delete(Macro m) {
        int i = MACROS.indexOf(m);
        MACROS.remove(m);
        if (m.id.equals(settings.selected)) {
            select(MACROS.isEmpty() ? null : MACROS.get(Math.min(Math.max(i, 0), MACROS.size() - 1)));
        }
        saveMacros();
    }

    public static String nextDefaultName() {
        int n = 1;
        while (nameTaken("Macro " + n, null)) n++;
        return "Macro " + n;
    }

    public static String uniqueName(String base, Macro self) {
        String b = base == null ? "" : base.trim();
        if (b.isEmpty()) b = "Macro";
        String name = b;
        int n = 2;
        while (nameTaken(name, self)) name = b + " (" + n++ + ")";
        return name;
    }

    private static boolean nameTaken(String name, Macro self) {
        for (Macro m : MACROS) {
            if (m != self && m.name.equalsIgnoreCase(name)) return true;
        }
        return false;
    }
}
