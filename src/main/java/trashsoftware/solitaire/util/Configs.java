package trashsoftware.solitaire.util;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class Configs {

    public static final String CONFIG_FILE = "config.cfg";

    public static void writeConfig(String key, String value) {
        Map<String, String> map = readMapFile(CONFIG_FILE);
        map.put(key, value);
        writeMapFile(CONFIG_FILE, map);
    }

    public static void writeConfig(String key, int value) {
        writeConfig(key, String.valueOf(value));
    }

    public static String getConfig(String key) {
        Map<String, String> map = readMapFile(CONFIG_FILE);
        return map.get(key);
    }

    public static int getInt(String key, int defaultValue) {
        String value = getConfig(key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * @param key the config key
     * @return the boolean value, or {@code false} by default
     */
    public static boolean getBoolean(String key) {
        return Boolean.parseBoolean(getConfig(key));
    }

    private static void writeMapFile(String fileName, Map<String, String> map) {
        createDirsIfNotExist();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName))) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                String line = entry.getKey() + "=" + entry.getValue() + '\n';
                bw.write(line);
            }
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Map<String, String> readMapFile(String fileName) {
        Map<String, String> map = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] keyValue = line.split("=");
                if (keyValue.length == 1) {
                    map.put(keyValue[0], "");
                } else if (keyValue.length == 2) {
                    map.put(keyValue[0], keyValue[1]);
                }
            }
        } catch (FileNotFoundException e) {
            writeMapFile(fileName, map);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }

    static void createDirsIfNotExist() {
//        File cache = new File(CONFIG_FILE);
//        if (!cache.exists()) {
//            if (!cache.mkdirs()) {
//                System.err.println("Cannot create directory 'config.cfg'");
//            }
//        }
    }
}
