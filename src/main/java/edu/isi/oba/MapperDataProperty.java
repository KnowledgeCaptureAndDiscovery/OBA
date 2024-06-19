package edu.isi.oba;

import static edu.isi.oba.Oba.logger;

import io.swagger.v3.oas.models.media.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.semanticweb.owlapi.model.OWLDataIntersectionOf;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLDataUnionOf;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNaryDataRange;

/**
 * Class for generating an new data property {@link Schema} OR taking an existing one and updating it.
 */
class MapperDataProperty {
  private static final Map<String, String> DATA_TYPES = Map.ofEntries(
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

  public static final String STRING_TYPE = "string";
  public static final String NUMBER_TYPE = "number";
  public static final String INTEGER_TYPE = "integer";
  public static final String BOOLEAN_TYPE = "boolean";
  public static final String DATETIME_TYPE = "dateTime";

  /**
   * Create a data property {@link Schema}.
   * 
   * @param name the data property's name.
   * @param description the data property's description.
   * @param datatypes a {@link Set} of {@link String} indicating the possible data types.
   * @return a generated {@link Schema} for the data property.
   */
  public static Schema createDataPropertySchema(String name, String description, Set<String> datatypes) {
    Schema propertySchema = new Schema();
    propertySchema.setName(name);
	  propertySchema.setDescription(description);

    // Do not set items unless datatypes contain something
    if (datatypes != null && !datatypes.isEmpty()) {
      if (datatypes.size() == 1) {
        final var datatype = datatypes.iterator().next();
        final var itemsSchema = MapperDataProperty.getTypeSchema(datatype);
        itemsSchema.setFormat(MapperDataProperty.getFormatForDatatype(datatype).isBlank() ? null : MapperDataProperty.getFormatForDatatype(datatype));
        propertySchema.setItems(itemsSchema);
      } else {
        final var composedSchema = new ComposedSchema();
        datatypes.forEach((datatype) -> {
          final var datatypeSchema = MapperDataProperty.getTypeSchema(datatype);
          datatypeSchema.setFormat(MapperDataProperty.getFormatForDatatype(datatype).isBlank() ? null : MapperDataProperty.getFormatForDatatype(datatype));
          composedSchema.addOneOfItem(datatypeSchema);
        });
        propertySchema.setItems(composedSchema);
      }

      // All property schemas are array types by default, if they have any items.
      propertySchema.setType("array");
    } else {
      // If no items, then the property's schema type should be "object" and not "array".  This might occur if the property has a complementOf value, but none of its own values.
      propertySchema.setType("object");
    }

    // By default, set property to be nullable.
	  propertySchema.setNullable(true);

	  return propertySchema;
  }

  private static String getScrubbedDataType(OWLDatatype owlDatatype) {
    return owlDatatype.toString().replaceFirst("owl:", "").replaceFirst("rdf:", "").replaceFirst("rdfs:", "").replaceFirst("xsd:", "");
  }

  private static String getScrubbedDataType(String owlDatatype) {
    return owlDatatype.replaceFirst("owl:", "").replaceFirst("rdf:", "").replaceFirst("rdfs:", "").replaceFirst("xsd:", "");
  }

  /**
   * Get the general datatype (i.e. string, integer, number, boolean, or data/time) of the OWL datatype.
   * 
   * @param owlDatatype an {@link OWLDatatype} (e.g. "xsd:integer").
   * @return a {@link String} indicating the general datatype.
   */
  public static String getDataType(OWLDatatype owlDatatype) {
    return MapperDataProperty.DATA_TYPES.get(MapperDataProperty.getScrubbedDataType(owlDatatype));
  }

  /**
   * Get the general datatype (i.e. string, integer, number, boolean, or data/time) of the OWL datatype.
   * 
   * @param owlDatatype a {@link String} indicating the "raw" value of an OWL dataype (e.g. "xsd:integer").
   * @return a {@link String} indicating the general datatype.
   */
  public static String getDataType(String owlDatatype) {
    return MapperDataProperty.DATA_TYPES.get(MapperDataProperty.getScrubbedDataType(owlDatatype));
  }

  private static Schema getTypeSchema(String owlDatatype) {
    switch (MapperDataProperty.getDataType(owlDatatype)) {
      case STRING_TYPE:
        return new StringSchema();
      case NUMBER_TYPE:
        return new NumberSchema();
      case INTEGER_TYPE:
        return new IntegerSchema();
      case BOOLEAN_TYPE:
        return new BooleanSchema();
      case DATETIME_TYPE:
          return new DateTimeSchema();
      default:
        logger.warning("datatype mapping failed: " + owlDatatype);
        return new Schema();
    }
  }

  /**
   * Get the format (if applicable) for an OWL datatype.
   * 
   * @param datatype
   * @return a string indicating the specific format, if applicable.  Empty string otherwise.
   */
  private static String getFormatForDatatype(String owlDatatype) {
    final var scrubbedDatatype = MapperDataProperty.getScrubbedDataType(owlDatatype);
    switch (MapperDataProperty.getDataType(owlDatatype)) {
      case STRING_TYPE:
        if ("anyURI".equals(scrubbedDatatype)) {
          return "uri";
        } else if ("byte".equals(scrubbedDatatype)) {
          return "byte";
        }

        break;
      case NUMBER_TYPE:
        if ("float".equals(scrubbedDatatype)) {
          return "double";
        } else if ("double".equals(scrubbedDatatype)) {
          return"double";
        } else {
          return "number";
        }
      case INTEGER_TYPE:
        if ("long".equals(scrubbedDatatype)) {
          return "int64";
        }
          
        return "int32";
      case DATETIME_TYPE:
        return "date-time";
      case BOOLEAN_TYPE:
        break;     
      default:
        logger.warning("Unknown OWLDatatype: " + owlDatatype);
        break;
      }

      return "";
  }

  private static String getFormatForDatatype(OWLDatatype owlDatatype) {
    return MapperDataProperty.getFormatForDatatype(owlDatatype.toString());
  }

  /**
   * Set the data property {@link Schema}'s complement.
   * 
   * @param dataPropertySchema a data property {@link Schema}.
   * @param complementOfType an {@link OWLDatatype} value to set as the complement.
   * @return an data property {@link Schema}
   */
  public static Schema setComplementOfForDataSchema(Schema dataPropertySchema, OWLDatatype complementOfType) {
    final var complementDatatypeString = MapperDataProperty.getDataType(complementOfType.toString());
    Schema complement = MapperDataProperty.getTypeSchema(complementDatatypeString);
    complement.setFormat(MapperDataProperty.getFormatForDatatype(complementOfType));
    dataPropertySchema.not(complement);

    return dataPropertySchema;
  }

  /**
   * Add an anyOf value to an data property {@link Schema}.
   * TODO: determine whether the return value can be removed, because it updates the Schema by reference anyway
   * 
   * @param dataPropertySchema an data property {@link Schema}.
   * @param dataRangeType a {@link String} value indicating the data range type.
   * @return an data property {@link Schema}
   */
  public static Schema addAnyOfDataPropertySchema(Schema dataPropertySchema, String dataRangeType) {
    // Always set nullable to false for owl:someValuesFrom
    // @see https://owl-to-oas.readthedocs.io/en/latest/mapping/#someValuesFromExample
    MapperProperty.setNullableValueForPropertySchema(dataPropertySchema, false);

    Schema itemsSchema = null;

    if (dataPropertySchema.getItems() == null) {
      itemsSchema = new ComposedSchema();
    } else {
      itemsSchema = dataPropertySchema.getItems();
    }

    // Only add anyOf value if there are no enum values.
    if (itemsSchema.getEnum() == null || itemsSchema.getEnum().isEmpty()) {
      // Only add anyOf value if the value is not already included.
      if (itemsSchema.getAnyOf() == null || !itemsSchema.getAnyOf().contains(dataRangeType)) {
        final var dataTypeSchema = MapperDataProperty.getTypeSchema(dataRangeType);

        itemsSchema.addAnyOfItem(dataTypeSchema);

        dataPropertySchema.setItems(itemsSchema);
        dataPropertySchema.setType("array");
        dataPropertySchema.setNullable(false);
      }
    }

    return dataPropertySchema;
  }

  /**
   * Add an allOf value to an data property {@link Schema}.
   * TODO: determine whether the return value can be removed, because it updates the Schema by reference anyway
   * 
   * @param dataPropertySchema an data property {@link Schema}.
   * @param dataRangeType a {@link String} value indicating the data range type.
   * @return an data property {@link Schema}
   */
  public static Schema addAllOfDataPropertySchema(Schema dataPropertySchema, String dataRangeType) {
    // Always set nullable to true for owl:allValuesFrom
    // @see https://owl-to-oas.readthedocs.io/en/latest/mapping/#allValuesFromExample
    MapperProperty.setNullableValueForPropertySchema(dataPropertySchema, true);

    Schema itemsSchema = null;

    if (dataPropertySchema.getItems() == null) {
      itemsSchema = new ComposedSchema();
    } else {
      itemsSchema = dataPropertySchema.getItems();
    }

    // Only add allOf value if there are no enum values.
    if (itemsSchema.getEnum() == null || itemsSchema.getEnum().isEmpty()) {
      // Only add allOf value if the value is not already included.
      if (itemsSchema.getAllOf() == null || !itemsSchema.getAllOf().contains(dataRangeType)) {
        final var dataTypeSchema = MapperDataProperty.getTypeSchema(dataRangeType);
        
        itemsSchema.addAllOfItem(dataTypeSchema);

        dataPropertySchema.setItems(itemsSchema);
        dataPropertySchema.setType("array");
        dataPropertySchema.setNullable(false);
      }
    }

    return dataPropertySchema;
  }

  /**
   * Add an oneOf value to an data property {@link Schema}.
   * TODO: determine whether the return value can be removed, because it updates the Schema by reference anyway
   * 
   * @param dataPropertySchema an data property {@link Schema}.
   * @param oneOfLiteral an {@link OWLLiteral} value to add
   * @return an data property {@link Schema}
   */
  public static Schema addOneOfDataPropertySchema(Schema dataPropertySchema, OWLLiteral oneOfLiteral) {
    Schema itemsSchema = null;

    if (dataPropertySchema.getItems() == null) {
      itemsSchema = new ComposedSchema();
    } else {
      itemsSchema = dataPropertySchema.getItems();

      // oneOf/enum takes priority over (and cannot co-occur with) allOf/anyOf.
      itemsSchema.setAllOf(null);
      itemsSchema.setAnyOf(null);
    }

    // Only add oneOf/enum value if the value is not already included.
    if (itemsSchema.getEnum() == null || !((List<String>) itemsSchema.getEnum().stream().map(Object::toString).collect(Collectors.toList())).contains(oneOfLiteral.getLiteral())) {
      final var datatype = MapperDataProperty.getDataType(oneOfLiteral.toString().substring(oneOfLiteral.toString().lastIndexOf(":") + 1));

      switch (datatype) {
        case NUMBER_TYPE:
          itemsSchema.addEnumItemObject(Double.parseDouble(oneOfLiteral.getLiteral()));
          break;
        case INTEGER_TYPE:
          itemsSchema.addEnumItemObject(Integer.parseInt(oneOfLiteral.getLiteral()));
          break;
        case BOOLEAN_TYPE:
          itemsSchema.addEnumItemObject(Boolean.parseBoolean(oneOfLiteral.getLiteral()));
          break;
        case STRING_TYPE:
        case DATETIME_TYPE:
        default:
          itemsSchema.addEnumItemObject(oneOfLiteral.getLiteral());
          break;
      }

      itemsSchema.set$ref(null);
      itemsSchema.setType(null);
      
      dataPropertySchema.setItems(itemsSchema);
      dataPropertySchema.setType("array");
      dataPropertySchema.setNullable(false);
    }

    return dataPropertySchema;
  }

  /**
   * Add a minimum cardinality value to the property's {@link Schema}.
   * TODO: determine whether the return value can be removed, because it updates the Schema by reference anyway
   * 
   * @param propertySchema a (data / object) property {@link Schema}.
   * @param cardinalityInt an exact cardinality value.
   * @param dataRangeType a {@link String} value indicating the data range type.
   * @return the {@link Schema} with added cardinality value.
   */
  public static Schema addMinCardinalityToPropertySchema(Schema propertySchema, Integer cardinalityInt, String dataRangeType) {
    propertySchema.setMinItems(cardinalityInt);

    final var dataTypeSchema = MapperDataProperty.getTypeSchema(dataRangeType);
    propertySchema.setItems(dataTypeSchema);
    propertySchema.setType("array");

    return propertySchema;
  }

  /**
   * Add a maximum cardinality value to the property's {@link Schema}.
   * TODO: determine whether the return value can be removed, because it updates the Schema by reference anyway
   * 
   * @param propertySchema a (data / object) property {@link Schema}.
   * @param cardinalityInt a maximum cardinality value.
   * @param dataRangeType a {@link String} value indicating the data range type.
   * @return the {@link Schema} with added cardinality value.
   */
  public static Schema addMaxCardinalityToPropertySchema(Schema propertySchema, Integer cardinalityInt, String dataRangeType) {
    propertySchema.setMaxItems(cardinalityInt);

    final var dataTypeSchema = MapperDataProperty.getTypeSchema(dataRangeType);
    propertySchema.setItems(dataTypeSchema);
    propertySchema.setType("array");

    return propertySchema;
  }

  /**
   * Add an exact cardinality value to the property's {@link Schema}.
   * TODO: determine whether the return value can be removed, because it updates the Schema by reference anyway
   * 
   * @param propertySchema a (data / object) property {@link Schema}.
   * @param cardinalityInt an exact cardinality value.
   * @param dataRangeType a {@link String} value indicating the data range type.
   * @return the {@link Schema} with added cardinality value.
   */
   public static Schema addExactCardinalityToPropertySchema(Schema propertySchema, Integer cardinalityInt, String dataRangeType) {
    propertySchema.setMinItems(cardinalityInt);
    propertySchema.setMaxItems(cardinalityInt);

    final var dataTypeSchema = MapperDataProperty.getTypeSchema(dataRangeType);
    propertySchema.setItems(dataTypeSchema);
    propertySchema.setType("array");

    return propertySchema;
  }

  /**
   * Recursive method to get/generate a {@link ComposedSchema} that may/may not be complex (i.e. contains nested unions/intersections).
   * 
   * @param dr a {@link OWLNaryDataRange} data range (i.e. {@link OWLDataUnionOf} or {@link OWLDataIntersectionOf})
   * @return a {@link ComposedSchema} comprising an anyOf/allOf list of items.
   */
  public static ComposedSchema getComplexDataComposedSchema(OWLNaryDataRange dr) {
		final var schema = new ComposedSchema();

		final var isDataUnion = dr instanceof OWLDataUnionOf;
		
		// Loop through each item in the union/intersection and accept visits.
		for (OWLDataRange e: dr.getOperands()) {
			if (e.isOWLDatatype()) {
				Schema dataTypeSchema = null;

				final var owlDataType = e.asOWLDatatype().toString();
				final var dataType = MapperDataProperty.getDataType(owlDataType);
				switch (dataType) {
					case MapperDataProperty.STRING_TYPE:
						dataTypeSchema = new StringSchema();

						if ("xsd:anyURI".equals(owlDataType)) {
							dataTypeSchema.format("uri");
						} else if ("xsd:byte".equals(owlDataType)) {
							dataTypeSchema.format("byte");
						}

						break;
					case MapperDataProperty.NUMBER_TYPE:
						dataTypeSchema = new NumberSchema();

						if ("xsd:float".equals(owlDataType)) {
							dataTypeSchema.format("double");
						} else if ("xsd:double".equals(owlDataType)) {
							dataTypeSchema.format("double");
						} else {
							dataTypeSchema.format("number");
						}

						break;
					case MapperDataProperty.INTEGER_TYPE:
						dataTypeSchema = new IntegerSchema();

						if ("xsd:nonPositiveInteger".equals(owlDataType)) {
							dataTypeSchema.setMaximum(BigDecimal.ZERO);
						}
			
						if ("xsd:nonNegativeInteger".equals(owlDataType)) {
							dataTypeSchema.setMinimum(BigDecimal.ZERO);
						}

						if ("xsd:long".equals(owlDataType)) {
							dataTypeSchema.format("int64");
						}

						break;
					case MapperDataProperty.BOOLEAN_TYPE:
						dataTypeSchema = new BooleanSchema();
						break;
					case MapperDataProperty.DATETIME_TYPE:
						dataTypeSchema = new DateTimeSchema();
						break;	       
					default:
						logger.warning("datatype mapping failed for:  " + owlDataType);
						dataTypeSchema = new Schema();
				}

				if (isDataUnion) {
					schema.addAnyOfItem(dataTypeSchema);
				} else {
					schema.addAllOfItem(dataTypeSchema);
				}
			} else if (e instanceof OWLNaryDataRange) {
				if (isDataUnion) {
					schema.addAnyOfItem(MapperDataProperty.getComplexDataComposedSchema((OWLNaryDataRange) e));
				} else {
					schema.addAllOfItem(MapperDataProperty.getComplexDataComposedSchema((OWLNaryDataRange) e));
				}
			} else {
				logger.severe("Need to investigate how to handle this OWLClassExpression:  " + e);
			}
		}

		return schema;
	}
}
