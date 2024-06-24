package edu.isi.oba;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import io.swagger.v3.oas.models.media.*;

/**
 * Class for taking an existing {@link Schema} and updating in ways that are generic/shared between object and data properties.
 */
public class MapperProperty {

  /**
   * Sets the {@link Schema}'s name.
   * 
   * @param schema a {@link Schema}
   * @param name a {@link String} indicating the {@link Schema}'s name.
   */
  public static void setSchemaName(Schema schema, String name) {
    schema.setName(name);
  }

  /**
   * Sets the {@link Schema}'s description.
   * 
   * @param schema a {@link Schema}
   * @param description a {@link String} indicating the {@link Schema}'s name.
   */
  public static void setSchemaDescription(Schema schema, String description) {
    schema.setDescription(description);
  }

  /**
   * Sets the {@link Schema}'s type.
   * 
   * @param schema a {@link Schema}
   * @param type a {@link String} indicating {@link Schema}'s type.
   */
  public static void setSchemaType(Schema schema, String type) {
    schema.setType(type);
  }

  /**
   * Set the nullable value for a property's {@link Schema}.
   * 
   * @param propertySchema a (data / object) property {@link Schema}.
   * @param isNullable a boolean value indicating nullable or not.
   */
  public static void setNullableValueForPropertySchema(Schema propertySchema, Boolean isNullable) {
    propertySchema.setNullable(isNullable);
  }

  /**
   * Convert the class {@link Schema} so that any properties that can be converted from arrays to non-arrays will be converted.
   * Some properties cannot be converted (e.g. if they require multiple values) -> these properties are no converted.
   * 
   * @param classSchemaToConvert a {@link Schema} to perform the conversion on.
   * @param functionalProperties a {@link Set} of {@link String} indicating the (short form) names of properties which are functional.
   * @return a {@link Schema} with all possible non-array properties converted.
   */
  public static void convertArrayToNonArrayPropertySchemas(Schema classSchemaToConvert, Set<String> functionalProperties) {
		final Map<String, Schema> propertySchemas = classSchemaToConvert.getProperties() == null ? new HashMap<>() : classSchemaToConvert.getProperties();

		// Loop through all of the properties and convert as necessary.
		propertySchemas.forEach((propertyName, propertySchema) -> {
			// Only need to convert if the propertySchema is of type "array".
			if ("array".equals(propertySchema.getType())) {
				// Unsure if this should be done, but if the property items are sufficiently complex (e.g. oneOf, allOf, anyOf), do no convert it(??).
				final var itemsSchema = propertySchema.getItems();

				if (itemsSchema != null) {
					boolean shouldBeArray = !(functionalProperties != null && functionalProperties.contains(propertyName))
                            && (Objects.requireNonNullElse(propertySchema.getMinItems(), -1) > 0
                              || Objects.requireNonNullElse(propertySchema.getMaxItems(), -1) > 1)
                            && !(Objects.requireNonNullElse(propertySchema.getMinItems(), -1) == 1
                              && Objects.requireNonNullElse(propertySchema.getMaxItems(), -1) == 1);
					
					// Keep as array (even if only one item exists), if there is a single reference or allOf/anyOf/oneOf/enum composed schemas are contained within the property's item.
					shouldBeArray |= Objects.requireNonNullElse(propertySchema.getMinItems(), -1) < 1 // Weird edge case that someone may define minimum items as zero (or negative?), and should remain as array
                            || itemsSchema != null && (itemsSchema.get$ref() != null
														  || (itemsSchema.getAllOf() != null && !itemsSchema.getAllOf().isEmpty())
														  || (itemsSchema.getAnyOf() != null && !itemsSchema.getAnyOf().isEmpty())
														  || (itemsSchema.getOneOf() != null && !itemsSchema.getOneOf().isEmpty())
														  || (itemsSchema.getEnum() != null && !itemsSchema.getEnum().isEmpty()));
					
					// By default, everything is an array.  If this property is not, then convert it from an array to a single item.
					if (!shouldBeArray) {
						propertySchema.setType(itemsSchema.getType());
						propertySchema.setFormat(itemsSchema.getFormat());
						// Anything else?

            // Because non-arrays are allowed by the configuration, we do not need min/max items for an exact configuration of one.
            // NOTE: These values should only be removed if the property is marked as required (via the configuration file).
            //        The property *should* be marked required (if applicable) before calling this method!
            if (classSchemaToConvert.getRequired() != null && classSchemaToConvert.getRequired().contains(propertyName)) {
              if (Objects.requireNonNullElse(propertySchema.getMinItems(), -1) == 1
                  && Objects.requireNonNullElse(propertySchema.getMaxItems(), -1) == 1) {
                propertySchema.setMaxItems(null);
                propertySchema.setMinItems(null);
              } else if (Objects.requireNonNullElse(propertySchema.getMaxItems(), -1) == 1) {
                propertySchema.setMaxItems(null);
                MapperProperty.setNullableValueForPropertySchema(propertySchema, true);
              }
            }

						// Now clear out the original items.
						propertySchema.setItems(null);
					}
				}
			}
		});

    // Not using setProperties(), because it creates immutability which breaks unit tests.
    propertySchemas.forEach((schemaName, schema) -> {
      classSchemaToConvert.addProperty(schemaName, schema);
    });
	}

    /**
   * Add a minimum cardinality value to the property's {@link Schema}.
   * 
   * @param propertySchema a (data / object) property {@link Schema}.
   * @param cardinalityInt a minimum cardinality value.
   */
  protected static void addMinCardinalityToPropertySchema(Schema propertySchema, Integer cardinalityInt) {
    propertySchema.setMinItems(cardinalityInt);

    if (cardinalityInt > 0) {
      MapperProperty.setNullableValueForPropertySchema(propertySchema, false);
    }
  }

  /**
   * Add a maximum cardinality value to the property's {@link Schema}.
   * 
   * @param propertySchema a (data / object) property {@link Schema}.
   * @param cardinalityInt a maximum cardinality value.
   */
  protected static void addMaxCardinalityToPropertySchema(Schema propertySchema, Integer cardinalityInt) {
    propertySchema.setMaxItems(cardinalityInt);

    if (cardinalityInt.intValue() < 2) {
      MapperProperty.setNullableValueForPropertySchema(propertySchema, true);
    }
  }

    /**
   * Add an exact cardinality value to the property's {@link Schema}.
   * 
   * @param propertySchema a (data / object) property {@link Schema}.
   * @param cardinalityInt an exact cardinality value.
   */
  protected static void addExactCardinalityToPropertySchema(Schema propertySchema, Integer cardinalityInt) {
    propertySchema.setMinItems(cardinalityInt);
    propertySchema.setMaxItems(cardinalityInt);
  }

  /**
   * Add a "hasValue" value to the property's {@link Schema}.
   * 
   * @param propertySchema a (data / object) property {@link Schema}.
   * @param cardinalityInt a minimum cardinality value.
   */
  public static void addHasValueOfPropertySchema(Schema propertySchema, String hasValue) {
    Schema itemsSchema = null;

    if (propertySchema.getItems() == null) {
      itemsSchema = new ComposedSchema();
    } else {
      itemsSchema = propertySchema.getItems();

      // default value and "has value" (i.e. specific enum(s)) takes priority over (and cannot co-occur with) allOf/anyOf/oneOf.
      itemsSchema.setAllOf(null);
      itemsSchema.setAnyOf(null);
      itemsSchema.setOneOf(null);
    }

    // Only set the first value as default, in case there are multiple ones.
    if (itemsSchema.getDefault() == null) {
      itemsSchema.setDefault(hasValue);
    }
    
    // Only add if no enums already OR it's not contained with the enums yet.
    if (itemsSchema.getEnum() == null || !((List<String>) itemsSchema.getEnum().stream().map(Object::toString).collect(Collectors.toList())).contains(hasValue)) {
      itemsSchema.addEnumItemObject(hasValue);
      itemsSchema.setType(null);

      propertySchema.setItems(itemsSchema);
    }

    // Need to make sure the property's type is "array" because it has items.
    propertySchema.setType("array");
  }

  /**
   * Set the property's {@link Schema} to indicate that it is functional.
   * NOTE: This is basically a convenience method for calling {@link #addMaxCardinalityToPropertySchema(Schema, Integer)} with {@link Integer} value of 1.
   * 
   * @param propertySchema a (data / object) property {@link Schema}.
   * @param cardinalityInt a minimum cardinality value.
   */
  public static void setFunctionalForPropertySchema(Schema propertySchema) {
    MapperProperty.setNullableValueForPropertySchema(propertySchema, false);
    MapperProperty.addMaxCardinalityToPropertySchema(propertySchema, Integer.valueOf(1));
  }
}
