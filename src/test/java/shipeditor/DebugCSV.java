package shipeditor;

import shipeditor.parsing.loading.CsvLoader;
import shipeditor.utility.text.StringConstants;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class DebugCSV {
    public static void main(String[] args) {
        Path p = Paths.get("/media/lechibang/work/starsector/starsector-core/data/hullmods/hull_mods.csv");
        System.out.println("Path: " + p + " exists? " + p.toFile().exists());
        List<Map<String, String>> raw = CsvLoader.reparseCSVForPath(p);
        if (raw == null || raw.isEmpty()) {
            System.out.println("Raw is null or empty!");
            return;
        }
        System.out.println("First row keys: " + raw.get(0).keySet());
        System.out.println("First row values: " + raw.get(0).values());
    }
}
