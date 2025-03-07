package me.spaff.tradecenter.config;

import me.spaff.tradecenter.Main;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Config {
    private static HashMap<String, Object> configData = new HashMap<>();

    public static void load() {
        File cfg = new File(Main.getInstance().getDataFolder(), "config.yml");
        if (!cfg.exists())
            Main.getInstance().saveResource("config.yml", false);
        reload();
    }

    public static void reload() {
        FileManager fileManager = new FileManager("config");
        processSection("", fileManager.getConfiguration(), configData);
    }

    //

    public static HashMap<String, Object> getRawData() {
        return configData;
    }

    public static Object readValue(String section) {
        for (var entry : configData.entrySet()) {
            String sec = entry.getKey();
            Object value = entry.getValue();

            if (sec.equals(section))
                return value;
        }

        return null;
    }

    public static String readString(String section) {
        return (String) readValue(section);
    }

    public static int readInt(String section) {
        return (Integer) readValue(section);
    }

    public static boolean readBool(String section) {
        return (Boolean) readValue(section);
    }

    public static List<String> readList(String section) {
        if ((List<String>) readValue(section) == null || ((List<String>) readValue(section)).isEmpty())
            return new ArrayList<>();
        return (List<String>) readValue(section);
    }

    //

    private static void processSection(String prefix, ConfigurationSection section, Map<String, Object> dataMap) {
        for (String key : section.getKeys(false)) {
            String fullKey = prefix.isEmpty() ? key : prefix + "." + key;
            Object value = section.get(key);

            if (value instanceof ConfigurationSection)
                processSection(fullKey, (ConfigurationSection) value, dataMap);
            else
                dataMap.put(fullKey, value);
        }
    }
}
