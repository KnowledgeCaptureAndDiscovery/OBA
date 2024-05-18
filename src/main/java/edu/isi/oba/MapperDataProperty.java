package edu.isi.oba;

import static edu.isi.oba.Oba.logger;

import io.swagger.v3.oas.models.media.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

class MapperDataProperty {
  private final Map<String, String> dataTypes = Map.ofEntries(
    Map.entry("ENTITIES", "string"),
    Map.entry("ENTITY", "string"),
    Map.entry("ID", "string"),
    Map.entry("IDREF", "string"),
    Map.entry("IDREFS", "string"),
    Map.entry("NCName", "string"),
    Map.entry("NMTOKEN", "string"),
    Map.entry("NMTOKENS", "string"),
    Map.entry("NOTATION", "string"),
    Map.entry("Name", "string"),
    Map.entry("QName", "string"),
    Map.entry("anySimpleType", "string"),
    Map.entry("anyType", "string"),
    Map.entry("anyURI", "string"),
    Map.entry("base64Binary", "string"),
    Map.entry("boolean", "boolean"),
    Map.entry("byte", "integer"),
    Map.entry("date", "string"),
    Map.entry("dateTime", "dateTime"),
    Map.entry("dateTimeStamp", "dateTime"),
    Map.entry("decimal", "number"),
    Map.entry("double", "number"),
    Map.entry("duration", "string"),
    Map.entry("float", "number"),
    Map.entry("gDay", "string"),
    Map.entry("gMonth", "string"),
    Map.entry("gMonthYear", "string"),
    Map.entry("gYear", "string"),
    Map.entry("gYearMonth", "string"),
    Map.entry("hexBinary", "string"),
    Map.entry("int", "integer"),
    Map.entry("integer", "integer"),
    Map.entry("language", "string"),
    Map.entry("long", "integer"),
    Map.entry("negativeInteger", "integer"),
    Map.entry("nonNegativeInteger", "integer"),
    Map.entry("nonPositiveInteger", "integer"),
    Map.entry("normalizedString", "string"),
    Map.entry("positiveInteger", "integer"),
    Map.entry("short", "integer"),
    Map.entry("string", "string"),
    Map.entry("time", "string"),
    Map.entry("token", "string"),
    Map.entry("unsignedByte", "integer"),
    Map.entry("unsignedInt", "integer"),
    Map.entry("unsignedLong", "integer"),
    Map.entry("unsignedShort", "integer"),
    Map.entry("langString", "string"),
    Map.entry("Literal", "string")
  );

  private String getDataType(String key) {
    return this.dataTypes.get(key);
  }

  private static final String STRING_TYPE = "string";
  private static final String NUMBER_TYPE = "number";
  private static final String INTEGER_TYPE = "integer";
  private static final String BOOLEAN_TYPE = "boolean";
  private static final String DATETIME_TYPE = "dateTime";

  final String name;
  final String description;
  private Set<String> type;
  private Boolean array;
  private Boolean nullable;
  final Boolean isFunctional;
  private Map<String, String> restrictions;
  private Set<String> valuesFromDataRestrictions_ranges;

  public MapperDataProperty(String name, String description, Boolean isFunctional, Map<String, String> restrictions, Set<String> valuesFromDataRestrictions_ranges, Set<String> type, Boolean array, Boolean nullable) {
    this.name = name;
    this.description = description;
    this.type = type;
    this.array = array;
    this.nullable = nullable;
    this.isFunctional = isFunctional;
    this.restrictions = restrictions;
    this.valuesFromDataRestrictions_ranges = valuesFromDataRestrictions_ranges;
  }

  public Schema getSchemaByDataProperty() {
	  
    if (this.type == null || this.type.isEmpty()) {
      return (this.array) ? this.arraySchema(new StringSchema()) : this.nonArraySchema(new StringSchema());
    } else if (this.type.size() > 1) {
    	return (this.array) ? this.composedSchema() : this.nonArraySchema(new Schema());
    }

    final String dataType = this.type.iterator().next();
    String schemaType = this.getDataType(dataType);
    if (schemaType == null) {
      logger.severe("property " + this.name + " type " + this.type);
    }
    
    switch (schemaType) {
      case STRING_TYPE:
        return (this.array) ? this.arraySchema(new StringSchema()) : this.nonArraySchema(new StringSchema());
      case NUMBER_TYPE:
        return (this.array) ? this.arraySchema(new NumberSchema()) : this.nonArraySchema(new NumberSchema());
      case INTEGER_TYPE:
        return (this.array) ? this.arraySchema(new IntegerSchema()) : this.nonArraySchema(new IntegerSchema());
      case BOOLEAN_TYPE:
        return (this.array) ? this.arraySchema(new BooleanSchema()) : this.nonArraySchema(new BooleanSchema());
      case DATETIME_TYPE:
          return (this.array) ? this.arraySchema(new DateTimeSchema()) : this.nonArraySchema(new DateTimeSchema());
      default:
        logger.warning("datatype mapping failed " + dataType);
        return (this.array) ? this.arraySchema(new Schema()) : this.nonArraySchema(new Schema());
    }
  }

  /**
   * Generate a range of multiple values depending on the range axiom
   *
   * @return An ArraySchema: including a composedSchema with a list of items for anyOf, allOf restrictions.
   */ 
  private ArraySchema composedSchema() {
	  ArraySchema array = new ArraySchema();
	  Schema schema;
	  ComposedSchema composedSchema = new ComposedSchema();
	  array.setDescription(description);
	  array.setNullable(this.nullable);

	  // Operations for managing boolean combinations 
	  for (String restriction: this.restrictions.keySet()) { 
		  String value = this.restrictions.get(restriction); 	  
		  for (String item: this.type) {		  
			  switch (this.getDataType(item)) {
			  case STRING_TYPE:
				  schema = new StringSchema();

				  if ("anyURI".equals(item)) {
            schema.format("uri");
          } else if ("byte".equals(item)) {
            schema.format("byte");
          }

				  break;
			  case NUMBER_TYPE:
				  schema = new NumberSchema();

				  if ("float".equals(item)) {
            schema.format("double");
          } else if ("double".equals(item)) {
            schema.format("double");
          } else {
            schema.format("number");
          }
          
				  break;
			  case INTEGER_TYPE:
				  schema = new IntegerSchema();

          if ("nonPositiveInteger".equals(item)) {
            schema.setMaximum(BigDecimal.ZERO);
          }

          if ("nonNegativeInteger".equals(item)) {
            schema.setMinimum(BigDecimal.ZERO);
          }

				  if ("long".equals(item)) {
            schema.format("int64");
          }
					  
				  break;
			  case BOOLEAN_TYPE:
				  schema = new BooleanSchema();
				  break;
			  case DATETIME_TYPE:
				  schema = new DateTimeSchema();
				  break;	       
			  default:
				  logger.warning("datatype mapping failed " + item);
				  schema = new Schema();	  	
			  }
			  
			  switch (restriction) {
			  case "unionOf":
				  if ("someValuesFrom".equals(value)) {
            this.nullable = false;
          }

				  composedSchema.addAnyOfItem(schema); 
				  break;
			  case "intersectionOf":
				  if ("someValuesFrom".equals(value)) {
            this.nullable = false;	
          }

				  composedSchema.addAllOfItem(schema);
				  break;
			  default:
				  break;
			  }
		  }
	  }

	  array.setItems(composedSchema);
    array.setNullable(this.nullable);

	  if (isFunctional) {
      array.setMaxItems(1);
    }
	     
	  return array;
  }

  private Schema nonArraySchema(Schema base) {
    base.setDescription(this.description);
    base.setNullable(this.nullable);

    return this.getSchemaRestrictions(base);
  }

  private ArraySchema arraySchema(Schema base) {
	  ArraySchema array = new ArraySchema();
	  array.setDescription(this.description);

	  if (this.isFunctional) {
      array.setMaxItems(1);
    }

    if (this.restrictions.containsKey("complementOf")) {
      Schema schema = new Schema();
      Schema complementOf = new Schema();
      final String dataType = this.type.iterator().next();
      complementOf.setType(this.getDataType(dataType));
      complementOf.setFormat(dataType);
      schema.setNot(complementOf);
      array.setNullable(this.nullable);
      array.setItems(schema);
      return array;
    }

    base = this.getSchemaRestrictions(base);

    // Can this be done in a better way?
    Integer minItemsInteger = base.getMinItems();
    if (minItemsInteger != null) {
      base.setMinItems(null);
      array.setMinItems(minItemsInteger);
    }

    // Can this be done in a better way?
    Integer maxItemsInteger = base.getMaxItems();
    if (maxItemsInteger != null) {
      base.setMaxItems(null);
      array.setMaxItems(maxItemsInteger);
    }

	  array.setNullable(this.nullable);
	  array.setItems(base);

	  return array;
  }

  private Schema getSchemaRestrictions(Schema base) {
    for (String restriction: this.restrictions.keySet()) { 
      String value = restrictions.get(restriction);
      switch (restriction) {
        case "dataHasValue":
          base.setDefault(value);
          break;
        case "maxCardinality":
          base.setMaxItems(Integer.parseInt(value));
          break;
        case "minCardinality":
          base.setMinItems(Integer.parseInt(value));
          break;
        case "exactCardinality":
          base.setMaxItems(Integer.parseInt(value));
          base.setMinItems(Integer.parseInt(value));
          break;
        case "someValuesFrom":
          this.nullable = false;
          break;
        case "allValuesFrom":
          //nothing to do
          break;
        case "oneOf":
          if ("someValuesFrom".equals(value)) {
            this.nullable = false;
          }

          for (String rangeValue: this.valuesFromDataRestrictions_ranges) {
            base.addEnumItemObject(rangeValue);
          }

          break;
        default:
      }
    }

    return base;
  }
}
