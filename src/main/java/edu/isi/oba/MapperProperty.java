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
   * Set the nullable value for a property's {@link Schema}.
   * TODO: determine whether the return value can be removed, because it updates the Schema by reference anyway
   * 
   * @param propertySchema a (data / object) property {@link Schema}.
   * @param isNullable a boolean value indicating nullable or not.
   * @return the {@link Schema} with new/updated nullable value set.
   */
  public static Schema setNullableValueForPropertySchema(Schema propertySchema, Boolean isNullable) {
    propertySchema.setNullable(isNullable);

    return propertySchema;
  }

  /**
   * Convert the class {@link Schema} so that any properties that can be converted from arrays to non-arrays will be converted.
   * Some properties cannot be converted (e.g. if they require multiple values) -> these properties are no converted.
   * 
   * @param classSchemaToConvert a {@link Schema} to perform the conversion on.
   * @param functionalProperties a {@link Set} of {@link String} indicating the (short form) names of properties which are functional.
   * @return a {@link Schema} with all possible non-array properties converted.
   */
  public static Schema convertArrayToNonArrayPropertySchemas(Schema classSchemaToConvert, Set<String> functionalProperties) {
		final Map<String, Schema> propertySchemas = classSchemaToConvert.getProperties() == null ? new HashMap<>() : classSchemaToConvert.getProperties();

		// Loop through all of the properties and convert as necessary.
		propertySchemas.forEach((propertyName, propertySchema) -> {
			// Only need to convert if the propertySchema is of type "array".
			if ("array".equals(propertySchema.getType())) {
				// Unsure if this should be done, but if the property items are sufficiently complex (e.g. oneOf, allOf, anyOf), do no convert it(??).
				final var itemsSchema = propertySchema.getItems();

				if (itemsSchema != null) {
					boolean shouldBeArray = !(functionalProperties != null && functionalProperties.contains(propertyName))
                            && (Objects.requireNonNullElse(propertySchema.getMinItems(), -1) > 1
                              || Objects.requireNonNullElse(propertySchema.getMaxItems(), -1) > 1);
					
					// Keep as array (even if only one item exists), if there is a single reference or allOf/anyOf/oneOf/enum composed schemas are contained within the property's item.
					shouldBeArray |= itemsSchema != null && (itemsSchema.get$ref() != null
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
              }
            }

						// Now clear out the original items.
						propertySchema.setItems(null);
					}
				}
			}
		});

    return classSchemaToConvert;
	}

  /**
   * Add a minimum cardinality value to the property's {@link Schema}.
   * TODO: determine whether the return value can be removed, because it updates the Schema by reference anyway
   * 
   * @param propertySchema a (data / object) property {@link Schema}.
   * @param cardinalityInt a minimum cardinality value.
   * @return the {@link Schema} with added cardinality value.
   */
  public static Schema addMinCardinalityToPropertySchema(Schema propertySchema, Integer cardinalityInt) {
    propertySchema.setMinItems(cardinalityInt);

    return propertySchema;
  }

  /**
   * Add a maximum cardinality value to the property's {@link Schema}.
   * TODO: determine whether the return value can be removed, because it updates the Schema by reference anyway
   * 
   * @param propertySchema a (data / object) property {@link Schema}.
   * @param cardinalityInt a maximum cardinality value.
   * @return the {@link Schema} with added cardinality value.
   */
  public static Schema addMaxCardinalityToPropertySchema(Schema propertySchema, Integer cardinalityInt) {
    propertySchema.setMaxItems(cardinalityInt);

    return propertySchema;
  }

  /**
   * Add an exact cardinality value to the property's {@link Schema}.
   * TODO: determine whether the return value can be removed, because it updates the Schema by reference anyway
   * 
   * @param propertySchema a (data / object) property {@link Schema}.
   * @param cardinalityInt an exact cardinality value.
   * @return the {@link Schema} with added cardinality value.
   */
  public static Schema addExactCardinalityToPropertySchema(Schema propertySchema, Integer cardinalityInt) {
    propertySchema.setMinItems(cardinalityInt);
    propertySchema.setMaxItems(cardinalityInt);

    return propertySchema;
  }

  /**
   * Add a "hasValue" value to the property's {@link Schema}.
   * TODO: determine whether the return value can be removed, because it updates the Schema by reference anyway
   * 
   * @param propertySchema a (data / object) property {@link Schema}.
   * @param cardinalityInt a minimum cardinality value.
   * @return the {@link Schema} with added cardinality value.
   */
  public static Schema addHasValueOfPropertySchema(Schema propertySchema, String hasValue) {
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

    return propertySchema;
  }

  /**
   * Set the property's {@link Schema} to indicate that it is functional.
   * NOTE: This is basically a convenience method for calling {@link #addMaxCardinalityToPropertySchema(Schema, Integer)} with {@link Integer} value of 1.
   * TODO: determine whether the return value can be removed, because it updates the Schema by reference anyway
   * 
   * @param propertySchema a (data / object) property {@link Schema}.
   * @param cardinalityInt a minimum cardinality value.
   * @return the {@link Schema} with added cardinality value.
   */
  public static Schema setFunctionalForPropertySchema(Schema propertySchema) {
    MapperProperty.setNullableValueForPropertySchema(propertySchema, false);
    return MapperProperty.addMaxCardinalityToPropertySchema(propertySchema, Integer.valueOf(1));
  }
}
