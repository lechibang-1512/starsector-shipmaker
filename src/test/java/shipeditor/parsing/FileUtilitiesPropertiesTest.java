package shipeditor.parsing;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import static org.junit.jupiter.api.Assertions.*;

class FileUtilitiesPropertiesTest {

    @Property
    void testGetExtension(@ForAll @AlphaChars @StringLength(min = 1, max = 10) String name,
                          @ForAll @AlphaChars @StringLength(min = 1, max = 5) String ext) {
        String filename = name + "." + ext;
        String extracted = FileUtilities.getExtension(filename);
        assertEquals(ext, extracted);
    }

    @Property
    void testGetExtensionNoDot(@ForAll @AlphaChars @StringLength(min = 1, max = 10) String name) {
        String extracted = FileUtilities.getExtension(name);
        assertEquals("", extracted);
    }

    @Property
    void testExtractFolderName(@ForAll @AlphaChars @StringLength(min = 1, max = 10) String dir,
                               @ForAll @AlphaChars @StringLength(min = 1, max = 10) String file) {
        String path = dir + "/" + file;
        String folderName = FileUtilities.extractFolderName(path);
        assertEquals(file, folderName);
    }

    @Property
    void testExtractFolderNameTrailingSlash(@ForAll @AlphaChars @StringLength(min = 1, max = 10) String dir) {
        // According to Path.getFileName() on a directory with trailing slash, it should still return the directory name
        String path = dir + "/";
        String folderName = FileUtilities.extractFolderName(path);
        assertEquals(dir, folderName);
    }

}
