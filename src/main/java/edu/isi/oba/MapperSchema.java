package edu.isi.oba;

import io.swagger.v3.oas.models.media.Schema;

import java.util.ArrayList;
import java.util.HashMap;
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

    public getProperties(){
        Map<String, Schema> dataProperties = this.getDataProperties(ontology, cls);
        Map<String, Schema> objectProperties = this.getObjectProperties(ontology, cls);
        Map<String, Schema> properties = new HashMap<>();
        properties.putAll(dataProperties);
        properties.putAll(objectProperties);
        MapperSchema mapperSchema = new MapperSchema();
        schemas.put(getSchemaName(cls), mapperSchema.getSchema(getSchemaName(cls), "object", properties));
    }



    private List<String> required() {
        return new ArrayList<String>() {{
            add("type");
        }};
    }


}
