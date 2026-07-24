import shipeditor.parsing.JsonProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonParser;
import java.io.File;

public class TestParser {
    public static void main(String[] args) throws Exception {
        System.setProperty("log4j2.configurationFile", "file:/media/lechibang/work/projects/starsector-shipmaker/src/main/resources/log4j2.xml");
        File file = new File("/media/lechibang/work/starsector/mods/Sephira Conclave/data/weapons/proj/bbplus_overed_rail_cannon_shot.proj");
        String output = JsonProcessor.straightenMalformed(file);
        System.out.println("Output:");
        System.out.println(output);
        
        ObjectMapper objectMapper = shipeditor.parsing.FileUtilities.getConfigured();
        try (JsonParser parser = objectMapper.createParser(output)) {
            Object result = objectMapper.readValue(parser, Object.class);
            System.out.println("Parsed result: " + result);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
