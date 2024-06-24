package edu.isi.oba;

import static edu.isi.oba.Oba.logger;

import io.swagger.v3.oas.models.media.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNaryBooleanClassExpression;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;

/**
 * Class for generating an new object property {@link Schema} OR taking an existing one and updating it.
 */
public class MapperObjectProperty extends MapperProperty {

  /**
   * Create an object property {@link Schema}.
   * 
   * @param name the object property's name.
   * @param description the object property's description.
   * @param references a {@link Set} of {@link String} references to other objects (i.e. the object property range(s)).
   * @return a generated {@link Schema} for the object property.
   */
  public static Schema createObjectPropertySchema(String name, String description, Set<String> references) {
    Schema propertySchema = new Schema();
    MapperProperty.setSchemaName(propertySchema, name);
    MapperProperty.setSchemaDescription(propertySchema, description);

    // For one item, set items.  For multiple items, set items with an anyOf array with the items array.
    if (references != null && references.size() == 1) {
      final var reference = references.iterator().next();
      final var itemsSchema = new ObjectSchema();
      itemsSchema.set$ref(reference);
      propertySchema.setItems(itemsSchema);
    } else {
      final var composedSchema = new ComposedSchema();
      references.forEach((reference) -> {
        final var referenceSchema = new ObjectSchema();
        referenceSchema.set$ref(reference);
        composedSchema.addAnyOfItem(referenceSchema);
      });
      propertySchema.setItems(composedSchema);
    }

    // All property schemas are array types by default, if they have any items.
    MapperProperty.setSchemaType(propertySchema, "array");

    // By default, set property to be nullable.
    MapperProperty.setNullableValueForPropertySchema(propertySchema, true);

	  return propertySchema;
  }

  /**
   * This method is intented to add an enum value to a class's schema (cf. property's schema).
   * 
   * @param objectSchema an object {@link Schema}.
   * @param enumItem a {@link String} value to add to the enum list.
   */
  public static void addEnumValueToObjectSchema(Schema objectSchema, String enumItem) {
    // Not using setProperties(), because it creates immutability which breaks unit tests.
    objectSchema.properties(null);

    MapperProperty.setSchemaType(objectSchema, "string");
    objectSchema.setItems(null);
    MapperProperty.setNullableValueForPropertySchema(objectSchema, true);

    objectSchema.addEnumItemObject(enumItem);
  }

  /**
   * Set the object {@link Schema}'s complement.
   * 
   * @param objectSchema an object {@link Schema}.
   * @param complementOfReference a {@link String} value (i.e. OWLClass' short form name) to set as the complement.
   */
  public static void setComplementOfForObjectSchema(Schema objectSchema, String complementOfReference) {
    Schema complement = new ObjectSchema();
    complement.set$ref(complementOfReference);
    objectSchema.not(complement);
  }

  /**
   * Add an anyOf value to an object property {@link Schema}.
   * 
   * @param objectPropertySchema an object property {@link Schema}.
   * @param anyOfItem a {@link String} value (i.e. OWLClass' short form name) to add
   */
  public static void addAnyOfToObjectPropertySchema(Schema objectPropertySchema, String anyOfItem) {
    // Always set nullable to false for owl:someValuesFrom
    // @see https://owl-to-oas.readthedocs.io/en/latest/mapping/#someValuesFromExample
    MapperProperty.setNullableValueForPropertySchema(objectPropertySchema, false);

    Schema itemsSchema = null;

    if (objectPropertySchema.getItems() == null) {
      itemsSchema = new ComposedSchema();
    } else {
      itemsSchema = objectPropertySchema.getItems();

      // oneOf takes priority over (and cannot co-occur with) allOf/anyOf.
      itemsSchema.setAllOf(null);
      itemsSchema.setAnyOf(null);
    }

    // Only add anyOf value if there are no enum values.
    if (itemsSchema.getEnum() == null || itemsSchema.getEnum().isEmpty()) {
      // Only add anyOf value if the value is not already included.
      if (itemsSchema.getAnyOf() == null || !itemsSchema.getAnyOf().contains(anyOfItem)) {
        // There are cases where the property has a range (i.e. the items schema has a ref), but class restrictions have been added which further restrict it with an anyOf.
        // So, we need to unset the items reference first.
        if (itemsSchema.get$ref() != null) {
          itemsSchema.set$ref(null);
        }

        final var objSchema = new ObjectSchema();
        objSchema.set$ref(anyOfItem);

        itemsSchema.addAnyOfItem(objSchema);
        MapperProperty.setSchemaType(itemsSchema, null);

        objectPropertySchema.setItems(itemsSchema);
        MapperProperty.setSchemaType(objectPropertySchema, "array");
      }
    }
  }

  /**
   * Add an allOf value to an object property {@link Schema}.
   * 
   * @param objectPropertySchema an object property {@link Schema}.
   * @param allOfItem a {@link String} value (i.e. OWLClass' short form name) to add
   */
  public static void addAllOfToObjectPropertySchema(Schema objectPropertySchema, String allOfItem) {
    // Always set nullable to true for owl:allValuesFrom
    // @see https://owl-to-oas.readthedocs.io/en/latest/mapping/#allValuesFromExample
    MapperProperty.setNullableValueForPropertySchema(objectPropertySchema, true);

    Schema itemsSchema = null;

    if (objectPropertySchema.getItems() == null) {
      itemsSchema = new ComposedSchema();
    } else {
      itemsSchema = objectPropertySchema.getItems();
    }

    // Only add allOf value if there are no enum values.
    if (itemsSchema.getEnum() == null || itemsSchema.getEnum().isEmpty()) {
      // Only add allOf value if the value is not already included.
      if (itemsSchema.getAllOf() == null || !itemsSchema.getAllOf().contains(allOfItem)) {
        // There are cases where the property has a range (i.e. the items schema has a ref), but class restrictions have been added which further restrict it with an allOf.
        // So, we need to unset the items reference first.
        if (itemsSchema.get$ref() != null) {
          itemsSchema.set$ref(null);
        }

        final var objSchema = new ObjectSchema();
        objSchema.set$ref(allOfItem);
        
        itemsSchema.addAllOfItem(objSchema);
        MapperProperty.setSchemaType(itemsSchema, null);

        objectPropertySchema.setItems(itemsSchema);
        MapperProperty.setSchemaType(objectPropertySchema, "array");
      }
    }
  }

  /**
   * Add an oneOf value to an object property {@link Schema}.
   * 
   * @param objectPropertySchema an object property {@link Schema}.
   * @param oneOfItem a {@link String} value (i.e. OWLClass' short form name) to add
   */
  public static void addOneOfToObjectPropertySchema(Schema objectPropertySchema, String oneOfItem) {
    Schema itemsSchema = null;

    if (objectPropertySchema.getItems() == null) {
      itemsSchema = new ComposedSchema();
    } else {
      itemsSchema = objectPropertySchema.getItems();

      // This may be the first restriction added and a reference may exist.  Clear it, to make sure.
      itemsSchema.set$ref(null);

      // oneOf takes priority over (and cannot co-occur with) allOf/anyOf.
      itemsSchema.setAllOf(null);
      itemsSchema.setAnyOf(null);
    }

    // Only add if no enums already OR it's not contained with the enums yet.
    if (itemsSchema.getEnum() == null || !((List<String>) itemsSchema.getEnum().stream().map(Object::toString).collect(Collectors.toList())).contains(oneOfItem)) {
      itemsSchema.addEnumItemObject(oneOfItem);
      MapperProperty.setSchemaType(itemsSchema, "string");

      objectPropertySchema.setItems(itemsSchema);
      MapperProperty.setSchemaType(objectPropertySchema, "array");
      MapperProperty.setNullableValueForPropertySchema(objectPropertySchema, false);
    }
  }

  /**
   * Add a minimum cardinality value to the property's {@link Schema}.
   * 
   * @param propertySchema a (data / object) property {@link Schema}.
   * @param cardinalityInt a minimum cardinality value.
   * @param objectRange a {@link String} value (i.e. OWLClass' short form name) to add
   */
  public static void addMinCardinalityToPropertySchema(Schema propertySchema, Integer cardinalityInt, String objectRange) {
    MapperProperty.addMinCardinalityToPropertySchema(propertySchema, cardinalityInt);

    final var objSchema = new ObjectSchema();
    MapperProperty.setSchemaType(objSchema, "object");
    objSchema.set$ref(objectRange);
    propertySchema.setItems(objSchema);
  }

  /**
   * Add a maximum cardinality value to the property's {@link Schema}.
   * 
   * @param propertySchema a (data / object) property {@link Schema}.
   * @param cardinalityInt a maximum cardinality value.
   * @param objectRange a {@link String} value (i.e. OWLClass' short form name) to add
   * @return the {@link Schema} with added cardinality value.
   */
  public static void addMaxCardinalityToPropertySchema(Schema propertySchema, Integer cardinalityInt, String objectRange) {
    MapperProperty.addMaxCardinalityToPropertySchema(propertySchema, cardinalityInt);

    final var objSchema = new ObjectSchema();
    MapperProperty.setSchemaType(objSchema, "object");
    objSchema.set$ref(objectRange);
    propertySchema.setItems(objSchema);
  }

  /**
   * Add an exact cardinality value to the property's {@link Schema}.
   * 
   * @param propertySchema a (data / object) property {@link Schema}.
   * @param cardinalityInt an exact cardinality value.
   * @param objectRange a {@link String} value (i.e. OWLClass' short form name) to add
   */
  public static void addExactCardinalityToPropertySchema(Schema propertySchema, Integer cardinalityInt, String objectRange) {
    propertySchema.setMinItems(cardinalityInt);
    propertySchema.setMaxItems(cardinalityInt);

    final var objSchema = new ObjectSchema();
    MapperProperty.setSchemaType(objSchema, "object");
    objSchema.set$ref(objectRange);
    propertySchema.setItems(objSchema);
  }

  /**
   * Recursive method to get/generate a {@link ComposedSchema} that may/may not be complex (i.e. contains nested unions/intersections).
   * 
   * @param ce a {@link OWLNaryBooleanClassExpression} class expression (i.e. {@link OWLObjectUnionOf} or {@link OWLObjectIntersectionOf})
   * @param allowReferences a boolean flag indicating whether to use references (true) or not (false).
   * @return a {@link ComposedSchema} comprising an anyOf/allOf list of items.
   */
  public static ComposedSchema getComplexObjectComposedSchema(OWLNaryBooleanClassExpression ce, boolean allowReferences) {
		final var schema = new ComposedSchema();

		final var isObjectUnion = ce instanceof OWLObjectUnionOf;
		
		// Loop through each item in the union/intersection and accept visits.
		for (OWLClassExpression e: ce.getOperands()) {
			if (e.isOWLClass()) {
				final var objSchema = new ObjectSchema();
        MapperProperty.setSchemaType(objSchema, "object");

        if (allowReferences) {
          objSchema.set$ref(e.asOWLClass().getIRI().getShortForm());
        }

				if (isObjectUnion) {
					schema.addAnyOfItem(objSchema);
				} else {
					schema.addAllOfItem(objSchema);
				}
			} else if (e instanceof OWLNaryBooleanClassExpression) {
				if (isObjectUnion) {
					schema.addAnyOfItem(MapperObjectProperty.getComplexObjectComposedSchema((OWLNaryBooleanClassExpression) e, allowReferences));
				} else {
					schema.addAllOfItem(MapperObjectProperty.getComplexObjectComposedSchema((OWLNaryBooleanClassExpression) e, allowReferences));
				}
			} else {
				logger.severe("Need to investigate how to handle this OWLClassExpression:  " + e);
			}
		}

		return schema;
	}
}
