package edu.isi.oba;

import io.swagger.v3.oas.models.media.*;

import java.util.HashMap;
import java.util.List;
import static edu.isi.oba.Oba.logger;

class MapperDataProperty {
  private final HashMap<String, String> dataTypes;

  private void setDataTypes() {
    this.dataTypes.put("ENTITIES", "string");
    this.dataTypes.put("ENTITY", "string");
    this.dataTypes.put("ID", "string");
    this.dataTypes.put("IDREF", "string");
    this.dataTypes.put("IDREFS", "string");
    this.dataTypes.put("NCName", "string");
    this.dataTypes.put("NMTOKEN", "string");
    this.dataTypes.put("NMTOKENS", "string");
    this.dataTypes.put("NOTATION", "string");
    this.dataTypes.put("Name", "string");
    this.dataTypes.put("QName", "string");
    this.dataTypes.put("anySimpleType", "string");
    this.dataTypes.put("anyType", "string");
    this.dataTypes.put("anyURI", "string");
    this.dataTypes.put("base64Binary", "string");
    this.dataTypes.put("boolean", "boolean");
    this.dataTypes.put("byte", "integer");
    this.dataTypes.put("date", "string");
    this.dataTypes.put("dateTime", "string");
    this.dataTypes.put("dateTimeStamp", "string");
    this.dataTypes.put("decimal", "number");
    this.dataTypes.put("double", "number");
    this.dataTypes.put("duration", "string");
    this.dataTypes.put("float", "number");
    this.dataTypes.put("gDay", "string");
    this.dataTypes.put("gMonth", "string");
    this.dataTypes.put("gMonthYear", "string");
    this.dataTypes.put("gYear", "string");
    this.dataTypes.put("gYearMonth", "string");
    this.dataTypes.put("hexBinary", "string");
    this.dataTypes.put("int", "integer");
    this.dataTypes.put("integer", "integer");
    this.dataTypes.put("language", "string");
    this.dataTypes.put("long", "integer");
    this.dataTypes.put("negativeInteger", "integer");
    this.dataTypes.put("nonNegativeInteger", "integer");
    this.dataTypes.put("nonPositiveInteger", "integer");
    this.dataTypes.put("normalizedString", "string");
    this.dataTypes.put("positiveInteger", "integer");
    this.dataTypes.put("short", "integer");
    this.dataTypes.put("string", "string");
    this.dataTypes.put("time", "string");
    this.dataTypes.put("token", "string");
    this.dataTypes.put("unsignedByte", "integer");
    this.dataTypes.put("unsignedInt", "integer");
    this.dataTypes.put("unsignedLong", "integer");
    this.dataTypes.put("unsignedShort", "integer");
    this.dataTypes.put("langString", "string");

  }

  private String getDataType(String key){
    return this.dataTypes.get(key);
  }

  private static final String STRING_TYPE = "string";
  private static final String NUMBER_TYPE = "number";
  private static final String INTEGER_TYPE = "integer";
  private static final String BOOLEAN_TYPE = "boolean";

  final String name;
  final String description;
  private List<String> type;
  private Boolean array;
  private Boolean nullable;
  final Boolean isFunctional;


  public MapperDataProperty(String name, String description, Boolean isFunctional, List<String> type, Boolean array, Boolean nullable) {
    this.dataTypes = new HashMap<>();
    this.setDataTypes();
    this.name = name;
    this.description = description;
    this.type = type;
    this.array = array;
    this.nullable = nullable;
    this.isFunctional=isFunctional;
  }

  public Schema getSchemaByDataProperty(){
    //TODO: Assumption: only one type
    //if (this.name.equalsIgnoreCase("hasMaximumAcceptedValue")){
    //  System.out.println("a");
    ////}

    if (this.type.size() == 0) {
      return (array) ? arraySchema(new StringSchema(), nullable) : new StringSchema().nullable(nullable).description(description);
    }
    else if (this.type.size() > 1){
      return (array) ? arraySchema(new Schema(), nullable) : new Schema().nullable(nullable).description(description);
    }

    String schemaType = getDataType(this.type.get(0));
    if (schemaType == null){
      logger.severe("property " + this.name + " type " + this.type);
    }
    switch (schemaType) {
      case STRING_TYPE:
        return (array) ? arraySchema(new StringSchema(), nullable) : new StringSchema().nullable(nullable).description(description);
      case NUMBER_TYPE:
        return (array) ? arraySchema(new NumberSchema(), nullable) : new NumberSchema().nullable(nullable).description(description);
      case INTEGER_TYPE:
        return (array) ? arraySchema(new IntegerSchema(), nullable) : new IntegerSchema().nullable(nullable).description(description);
      case BOOLEAN_TYPE:
        return (array) ? arraySchema(new BooleanSchema(), nullable) : new BooleanSchema().nullable(nullable).description(description);
     default:
        logger.warning("datatype mapping failed " + this.type.get(0));
        return (array) ? arraySchema(new Schema(), nullable) : new Schema().nullable(nullable).description(description);
    }
  }

  //Not used
//  private Schema getObjectPropertiesByRef(String ref, boolean array, boolean nullable){
//    Schema object = new ObjectSchema();
//    object.set$ref(ref);
//
//    if (array) {
//      ArraySchema objects = new ArraySchema();
//      objects.setNullable(nullable);
//      objects.setItems(object);
//      return objects;
//    }
//    else {
//      return object;
//    }
//
//
//  }
//  not used
//  private Schema getComposedSchemaObject(List<String> refs, boolean array, boolean nullable){
//    Schema object = new ObjectSchema();
//    object.setType("object");
//
//    if (array) {
//      ArraySchema objects = new ArraySchema();
//      objects.setNullable(nullable);
//      objects.setItems(object);
//      return objects;
//    }
//    else {
//      return object;
//    }
//
//        /*ComposedSchema composedSchema = new ComposedSchema();
//        List<Schema> items = new ArrayList<>();
//        for (String ref : refs){
//            Schema item = getObjectPropertiesByRef(ref, array, nullable);
//            items.add(item);
//        }
//        composedSchema.setAnyOf(items);
//        return composedSchema;
//        */
//
//  }




  private ArraySchema arraySchema(Schema base, boolean nullable) {
    ArraySchema array = new ArraySchema();
    array.setDescription(description);
    array.setNullable(nullable);
    array.setItems(base);
    if (isFunctional)
        array.setMaxItems(1);
    return array;
  }

}
