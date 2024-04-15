package edu.isi.oba;

import io.swagger.v3.oas.models.media.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    this.dataTypes.put("dateTime", "dateTime");
    this.dataTypes.put("dateTimeStamp", "dateTime");
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
    this.dataTypes.put("Literal", "string");

  }

  private String getDataType(String key){
    return this.dataTypes.get(key);
  }

  private static final String STRING_TYPE = "string";
  private static final String NUMBER_TYPE = "number";
  private static final String INTEGER_TYPE = "integer";
  private static final String BOOLEAN_TYPE = "boolean";
  private static final String DATETIME_TYPE = "dateTime";

  final String name;
  final String description;
  private List<String> type;
  private Boolean array;
  private Boolean nullable;
  final Boolean isFunctional;
  private Map<String,String> restrictions;
  private List<String> valuesFromDataRestrictions_ranges;


  public MapperDataProperty(String name, String description, Boolean isFunctional,Map<String,String> restrictions,List<String> valuesFromDataRestrictions_ranges, List<String> type, Boolean array, Boolean nullable) {
    this.dataTypes = new HashMap<>();
    this.setDataTypes();
    this.name = name;
    this.description = description;
    this.type = type;
    this.array = array;
    this.nullable = nullable;
    this.isFunctional=isFunctional;
    this.restrictions=restrictions;
    this.valuesFromDataRestrictions_ranges=valuesFromDataRestrictions_ranges;
  }

  public Schema getSchemaByDataProperty(){
	  
    if (this.type.isEmpty()) {
      return (array) ? arraySchema(new StringSchema(), nullable) : new StringSchema().nullable(nullable).description(description);
    }
    else if (this.type.size() > 1) {
    	return (array) ? composedSchema(this.type, nullable) : new Schema().nullable(nullable).description(description);
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
      case DATETIME_TYPE:
          return (array) ? arraySchema(new DateTimeSchema() , nullable) : new DateTimeSchema().nullable(nullable).description(description);
      default:
        logger.warning("datatype mapping failed " + this.type.get(0));
        return (array) ? arraySchema(new Schema(), nullable) : new Schema().nullable(nullable).description(description);
    }
  }

  /**
   * Generate a range of multiple values depending on the range axiom
   *
   * @return An ArraySchema: including a composedSchema with a list of items for anyOf, allOf restrictions.
   */ 
  private ArraySchema composedSchema(List<String> base, boolean nullable){
	  ArraySchema array = new ArraySchema();
	  Schema schema ;
	  ComposedSchema composedSchema = new ComposedSchema() ;
	  array.setDescription(description);
	  array.setNullable(nullable);  
	  // Operations for managing boolean combinations 
	  for (String restriction:  restrictions.keySet()) { 
		  String value = restrictions.get(restriction); 	  
		  for (String item:base) {		  
			  switch (getDataType(item)) {
			  case STRING_TYPE:
				  schema = new StringSchema();
				  if (item.equals("anyURI"))
					  schema.format("uri");
				  else if (item.equals("byte"))
					  schema.format("byte");
				  break ;
			  case NUMBER_TYPE:
				  schema = new NumberSchema();
				  if (item.equals("float"))
					  schema.format("float");
				  else if (item.equals("double"))
					  schema.format("double");
				  break ;
			  case INTEGER_TYPE:
				  schema = new IntegerSchema();
				  if (item.equals("long"))
					  schema.format("int64");
				  break ;
			  case BOOLEAN_TYPE:
				  schema = new BooleanSchema();
				  break ;
			  case DATETIME_TYPE:
				  schema = new DateTimeSchema();
				  break ;	       
			  default:
				  logger.warning("datatype mapping failed " + this.type.get(0));
				  schema = new Schema();	  	
			  }
			  
			  switch (restriction) {
			  case "unionOf":
				  if (value=="someValuesFrom")
		        		nullable=false;	
				  composedSchema.addAnyOfItem(schema); 
				  break;
			  case "intersectionOf":
				  if (value=="someValuesFrom")
		        		nullable=false;	
				  composedSchema.addAllOfItem(schema);
				  break;
			  default:
				  break;
			  }
		  }
	  }
	  array.setItems(composedSchema);  	 

	  if (isFunctional)
		  array.setMaxItems(1);
	  array.setNullable(nullable);	     
	  return array ;
  }

  private ArraySchema arraySchema(Schema base, boolean nullable) {
	  ArraySchema array = new ArraySchema();
	  array.setDescription(description);

	  if (isFunctional)
		  array.setMaxItems(1);

	  for (String restriction:  restrictions.keySet()) { 
		  String value = restrictions.get(restriction);
		  switch (restriction) {
		  case "dataHasValue":      		
			  base.setDefault(value);
			  break;
		  case "maxCardinality":
			  base.setMaxItems(Integer.parseInt(value));
			  break ;
		  case "minCardinality":	
			  base.setMinItems(Integer.parseInt(value));
			  break ;
		  case "exactCardinality":
			  base.setMaxItems(Integer.parseInt(value));
			  base.setMinItems(Integer.parseInt(value));
			  break ;
		  case "someValuesFrom": 
			  nullable=false;
			  break ;
		  case "allValuesFrom":      	 
			  //nothing to do
			  break ; 		   
		  case "oneOf":   
			  if (value=="someValuesFrom")
				  nullable=false;	
			  for (String rangeValue:valuesFromDataRestrictions_ranges)
				  base.addEnumItemObject(rangeValue);
			  break;			 
		  case "complementOf":  
			  Schema schema = new Schema();
			  Schema complementOf = new Schema();
			  complementOf.setType(getDataType(type.get(0)));
			  complementOf.setFormat(type.get(0));
			  schema.setNot(complementOf);
			  array.setNullable(nullable);
			  array.setItems(schema);
			  return array;
		  default:
		  } 
	  } 

	  array.setNullable(nullable);
	  array.setItems(base);

	  return array;
  }

}
