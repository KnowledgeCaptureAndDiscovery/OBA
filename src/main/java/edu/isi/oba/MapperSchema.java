package edu.isi.oba;

import io.swagger.v3.oas.models.media.Schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MapperSchema {

    public Schema getSchema(String name, String type, Map<String, Schema> properties) {
        Schema schema = new Schema();
        schema.setName(name);
        schema.setType(type);
        schema.setProperties(properties);
        schema.setRequired(required());
        return schema;
    }



    private List<String> required() {
        return new ArrayList<String>() {{
            add("type");
        }};
    }


}
