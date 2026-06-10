package shipeditor.parsing.deserialize;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TextureTypeDeserializer extends JsonDeserializer<List<String>> {

    private static final TypeReference<List<String>> LIST_TYPE_REF = new TypeReference<>() {};

    @Override
    public List<String> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.isExpectedStartArrayToken()) {
            // Deserialize array.
            return p.readValueAs(LIST_TYPE_REF);
        } else if (p.isExpectedStartObjectToken()) {
            p.readValueAs(Object.class);
            // This is not going to work if there are indeed curly braces used to enclose array.
            return new ArrayList<>();
        } else {
            // Deserialize single value as a list.
            String singleValue = p.getValueAsString();
            List<String> list = new ArrayList<>();
            list.add(singleValue);
            return list;
        }
    }

}