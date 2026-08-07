package shipeditor.persistence.database;

import org.junit.jupiter.api.Test;
import shipeditor.utility.text.StringConstants;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DataReferenceQueriesTest {
    @Test
    public void testHullmodsExist() {
        DatabaseManager.initializeDatabase();
        Map<String, List<IndexedFile>> hullmods = DatabaseQueryService.getFilesByTypeGroupedByMod(StringConstants.HULLMOD_CSV_TYPE);
        System.out.println("HULLMOD MODS COUNT: " + hullmods.size());
        for(String m : hullmods.keySet()) {
             System.out.println("MOD: " + m + " FILES: " + hullmods.get(m).size());
        }
    }
}
