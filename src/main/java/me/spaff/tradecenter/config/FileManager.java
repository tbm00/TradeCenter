package me.spaff.tradecenter.config;

import me.spaff.tradecenter.Main;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class FileManager {
    private File file;
    private String path;
    private FileConfiguration configuration;

    public FileManager(String fileName) {
        this(getMainDirectory(), fileName);
    }

    public FileManager(String path, String fileName) {
        this.path = path;
        this.file = new File(path + File.separator + fileName + ".yml");

        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        this.configuration = new YamlConfiguration();
        try {
            configuration.load(file);
        }
        catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    public void write(String path, Object object) {
        configuration.set(path, object);
    }

    public Object read(String path) {
        return configuration.get(path);
    }

    public void reload() {
        try {
            configuration.load(file);
        }
        catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    public void save() {
        try {
            configuration.save(file);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean delete() {
        this.configuration = new YamlConfiguration();
        return file.delete();
    }

    public File getFile() {
        return file;
    }

    public FileConfiguration getConfiguration() {
        return configuration;
    }

    // Other

    public static String getMainDirectory() {
        return Main.getInstance().getDataFolder().getPath();
    }

    public static boolean fileExistsInDirectory(String path, String fileName) {
        File directory = new File(path);
        File[] files = directory.listFiles((pathname) -> pathname.getName().endsWith(".yml"));
        if (files == null) return false;

        for (File f : files) {
            if (f == null) continue;
            String name = f.getName().replaceAll(".yml", "");

            if (name.equals(fileName))
                return true;
        }

        return false;
    }
}