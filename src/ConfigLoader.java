import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.util.ArrayList;

class ConfigLoader {
    private static ConfigLoader configLoader = null;
    private static final String FILE_NAME = "config.json";
    private static ArrayList<String> regions_banned = new ArrayList<>();
    private static ArrayList<String> categories    = new ArrayList<>();
    private static int depth;

    private ConfigLoader() {
        String jsonString = extractJSON();
        JSONObject jsonObject = new JSONObject(jsonString);
        JSONArray regions_banned_array = jsonObject.getJSONArray("regions-banned");
        JSONArray categories_array = jsonObject.getJSONArray("categories");
        for (Object each : regions_banned_array)
            regions_banned.add((String) each);
        for (Object each : categories_array)
            categories.add((String) each);
        depth = Integer.parseInt(jsonObject.getString("depth"));
    }

    static synchronized ConfigLoader getInstance() {
        if (configLoader==null)
            configLoader = new ConfigLoader();
        return configLoader;
    }

    ArrayList<String> getRegions_banned() {
        return regions_banned;
    }

    ArrayList<String> getCategories() {
        return categories;
    }

    int getDepth() {
        return depth;
    }

    private String extractJSON() {
        File file = new File(FILE_NAME);
        String jsonString = "";
        String temp;
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            while ((temp = bufferedReader.readLine()) != null)
                jsonString+=temp;
        } catch (FileNotFoundException e) {
            System.out.println("\n[x] Configuration file not found");
            System.exit(0);
        } catch (IOException e) {
            System.out.println("\n[x] An error occurred while reading the configuration file");
            System.exit(0);
        }
        return jsonString;
    }
}
