package edu.isi.oba;

import io.swagger.v3.oas.models.media.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

public class MapperProperty {
    public HashMap<String, String> dataTypes;
    private final static Logger logger = Logger.getLogger("oba.MapperProperty");

    public void setDataTypes() {
        this.dataTypes.put("anyType", "string");
        this.dataTypes.put("anySimpleType", "string");
        this.dataTypes.put("string", "string");
        this.dataTypes.put("integer", "number");
        this.dataTypes.put("long", "number");
        this.dataTypes.put("int", "number");
        this.dataTypes.put("short", "number");
        this.dataTypes.put("byte", "string");
        this.dataTypes.put("decimal", "number");
        this.dataTypes.put("float", "number");
        this.dataTypes.put("boolean", "boolean");
        this.dataTypes.put("double", "number");
        this.dataTypes.put("nonPositiveInteger", "number");
        this.dataTypes.put("negativeInteger", "number");
        this.dataTypes.put("nonNegativeInteger", "number");
        this.dataTypes.put("unsignedLong", "number");
        this.dataTypes.put("unsignedInt", "number");
        this.dataTypes.put("positiveInteger", "number");
        this.dataTypes.put("base64Binary", "string");
        this.dataTypes.put("normalizedString", "string");
        this.dataTypes.put("hexBinary", "string");
        this.dataTypes.put("anyURI", "string");
        this.dataTypes.put("QName", "string");
        this.dataTypes.put("NOTATION", "string");
        this.dataTypes.put("token", "string");
        this.dataTypes.put("language", "string");
        this.dataTypes.put("Name", "string");
        this.dataTypes.put("NCName", "string");
        this.dataTypes.put("NMTOKEN", "string");
        this.dataTypes.put("NMTOKENS", "string");
        this.dataTypes.put("ID", "string");
        this.dataTypes.put("IDREF", "string");
        this.dataTypes.put("IDREFS", "string");
        this.dataTypes.put("ENTITY", "string");
        this.dataTypes.put("ENTITIES", "string");
        this.dataTypes.put("unsignedShort", "string");
        this.dataTypes.put("unsignedByte", "string");
        this.dataTypes.put("duration", "string");
        this.dataTypes.put("dateTime", "string");
        this.dataTypes.put("dateTimeStamp", "string");
        this.dataTypes.put("time", "string");
        this.dataTypes.put("date", "string");
        this.dataTypes.put("gYearMonth", "string");
        this.dataTypes.put("gYear", "string");
        this.dataTypes.put("gMonthYear", "string");
        this.dataTypes.put("gDay", "string");
        this.dataTypes.put("gMonth", "string");
        this.dataTypes.put("positiveInteger", "number");
    }

    public String getDataType(String key){
        return this.dataTypes.get(key);
    }

    public static final String STRING_TYPE = "string";
    public static final String NUMBER_TYPE = "number";
    String name;
    public List<String> type;
    public List<String> ref;
    Boolean array;
    Boolean nullable;
    Boolean object;


    public MapperProperty(String name, List<String> type, Boolean array, Boolean nullable, Boolean object) {
        this.dataTypes = new HashMap<>();
        this.setDataTypes();
        this.name = name;
        this.object = object;
        if (this.object)
            this.ref = type;
        else
            this.type = type;
        this.array = array;
        this.nullable = nullable;
    }
    /**
     * Set the property type
     * It can has multiple types
     * @param type
     */
    public void setType(List<String> type) {
        this.type = type;
    }

    public void setArray(Boolean array) {
        this.array = array;
    }

    public void setNullable(Boolean nullable) {
        this.nullable = nullable;
    }

    public Schema getSchemaByDataProperty(){
        //TODO: Assumption: only one type
        if (this.type.size() == 0) {
            return (array) ? arraySchema(new ObjectSchema(), nullable) : new ObjectSchema().nullable(nullable);
        }

        String schemaType = getDataType(this.type.get(0));
        switch (schemaType) {
            case STRING_TYPE:
                return (array) ? arraySchema(new StringSchema(), nullable) : new StringSchema().nullable(nullable);
            case NUMBER_TYPE:
                return (array) ? arraySchema(new NumberSchema(), nullable) : new IntegerSchema().nullable(nullable);
            default:
                System.out.println("datatype mapping failed " + this.type.get(0));
                return (array) ? arraySchema(new ObjectSchema(), nullable) : new ObjectSchema().nullable(nullable);
        }


    }

    public Schema getSchemaByObjectProperty(){
        if (this.ref.size() == 0){
            return new ObjectSchema().nullable(nullable);
        }

        if (this.ref.size() > 1){
            return getComposedSchemaObject(this.ref, array, nullable);
        }
        else {
            return getObjectPropertiesByRef(this.ref.get(0), array, nullable);
        }
    }

    public Schema getObjectPropertiesByRef(String ref, boolean array, boolean nullable){
        Schema object = new ObjectSchema();
        object.set$ref(ref);

        if (array) {
            ArraySchema objects = new ArraySchema();
            objects.setNullable(nullable);
            objects.setItems(object);
            return objects;
        }
        else {
            return object;
        }


    }
    private Schema getComposedSchemaObject(List<String> refs, boolean array, boolean nullable){
        ComposedSchema composedSchema = new ComposedSchema();
        List<Schema> items = new ArrayList<>();
        for (String ref : refs){
            Schema item = getObjectPropertiesByRef(ref, array, nullable);
            items.add(item);
        }
        composedSchema.setAnyOf(items);
        return composedSchema;
    }




    private ArraySchema arraySchema(Schema base, boolean nullable) {
        ArraySchema array = new ArraySchema();
        array.setNullable(nullable);
        array.setItems(base);
        return array;
    }

}
