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
        this.dataTypes.put("xsd:anyType", "string");
        this.dataTypes.put("xsd:anySimpleType", "string");
        this.dataTypes.put("xsd:string", "string");
        this.dataTypes.put("xsd:integer", "number");
        this.dataTypes.put("xsd:long", "number");
        this.dataTypes.put("xsd:int", "number");
        this.dataTypes.put("xsd:short", "number");
        this.dataTypes.put("xsd:byte", "string");
        this.dataTypes.put("xsd:decimal", "number");
        this.dataTypes.put("xsd:float", "number");
        this.dataTypes.put("xsd:boolean", "boolean");
        this.dataTypes.put("xsd:double", "number");
        this.dataTypes.put("xsd:nonPositiveInteger", "number");
        this.dataTypes.put("xsd:negativeInteger", "number");
        this.dataTypes.put("xsd:nonNegativeInteger", "number");
        this.dataTypes.put("xsd:unsignedLong", "number");
        this.dataTypes.put("xsd:unsignedInt", "number");
        this.dataTypes.put("xsd:positiveInteger", "number");
        this.dataTypes.put("xsd:base64Binary", "string");
        this.dataTypes.put("xsd:normalizedString", "string");
        this.dataTypes.put("xsd:hexBinary", "string");
        this.dataTypes.put("xsd:anyURI", "string");
        this.dataTypes.put("xsd:QName", "string");
        this.dataTypes.put("xsd:NOTATION", "string");
        this.dataTypes.put("xsd:token", "string");
        this.dataTypes.put("xsd:language", "string");
        this.dataTypes.put("xsd:Name", "string");
        this.dataTypes.put("xsd:NCName", "string");
        this.dataTypes.put("xsd:NMTOKEN", "string");
        this.dataTypes.put("xsd:NMTOKENS", "string");
        this.dataTypes.put("xsd:ID", "string");
        this.dataTypes.put("xsd:IDREF", "string");
        this.dataTypes.put("xsd:IDREFS", "string");
        this.dataTypes.put("xsd:ENTITY", "string");
        this.dataTypes.put("xsd:ENTITIES", "string");
        this.dataTypes.put("xsd:unsignedShort", "string");
        this.dataTypes.put("xsd:unsignedByte", "string");
        this.dataTypes.put("xsd:duration", "string");
        this.dataTypes.put("xsd:dateTime", "string");
        this.dataTypes.put("xsd:dateTimeStamp", "string");
        this.dataTypes.put("xsd:time", "string");
        this.dataTypes.put("xsd:date", "string");
        this.dataTypes.put("xsd:gYearMonth", "string");
        this.dataTypes.put("xsd:gYear", "string");
        this.dataTypes.put("xsd:gMonthYear", "string");
        this.dataTypes.put("xsd:gDay", "string");
        this.dataTypes.put("xsd:gMonth", "string");
        this.dataTypes.put("xsd:positiveInteger", "number");
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
