package shipeditor.parsing;

import net.jqwik.api.*;
import org.junit.jupiter.api.Assertions;

public class JsonProcessorPropertiesTest {

    @Property(tries = 1000)
    public void testStraightenMalformedDoesNotCrash(@ForAll String input) {
        // We just want to ensure that no arbitrary string crashes the processor
        // through regex catastrophic backtracking, StringIndexOutOfBounds, etc.
        String result = JsonProcessor.straightenMalformedText(input);
        Assertions.assertNotNull(result);
    }
}
