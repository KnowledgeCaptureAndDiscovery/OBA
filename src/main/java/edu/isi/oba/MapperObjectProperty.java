package edu.isi.oba;

import static edu.isi.oba.Oba.logger;

import io.swagger.v3.oas.models.media.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class MapperObjectProperty {
  final String name;
  final String description;
  final private Set<String> ref;
  private Boolean array;
  private Boolean nullable;
  final Boolean isFunctional;
  
  final Map<String,String> restrictions;

  public MapperObjectProperty(String name, String description, Boolean isFunctional, Map<String,String> restrictions, Set<String> ref) {
    this.name = name;
    this.description = description;
    this.ref = (ref != null) ? ref : new HashSet<>();
    this.array = true;
    this.nullable = true;
    this.isFunctional = isFunctional;
    this.restrictions = (restrictions != null) ? restrictions : new HashMap<>();
  }

  public MapperObjectProperty(String name, String description,  Boolean isFunctional, Map<String, String> restrictions, Set<String> ref, Boolean array, Boolean nullable) {
    this.name = name;
    this.description = description;
    this.ref = (ref != null) ? ref : new HashSet<>();
    this.array = array;
    this.nullable = nullable;
    this.isFunctional = isFunctional;
    this.restrictions = (restrictions != null) ? restrictions : new HashMap<>();
  }

  public Schema getSchemaByObjectProperty() {
    if (this.ref == null || this.ref.isEmpty()){
      return getComposedSchemaObject(this.ref, this.array, this.nullable);
    }

    if (this.ref.size() > 1){
      return getComposedSchemaObject(this.ref, this.array, this.nullable);
    } else {
      return getObjectPropertiesByRef(this.ref.iterator().next(), this.array, this.nullable);
    }
  }

  private Schema getObjectPropertiesByRef(String ref, boolean isArray, boolean isNullable) {
    if (this.restrictions.isEmpty()) {
      Schema object = new ObjectSchema();
      object.set$ref(ref);

      if (isArray) {
        ArraySchema objects = new ArraySchema();
        objects.setDescription(this.description);
        objects.setNullable(isNullable);
        objects.setItems(object);

        if (this.isFunctional) {
          objects.setMaxItems(1);
        }

        return objects;
      } else {
        object.setDescription(this.description);
        object.setNullable(isNullable);
        object.setType("object");

        if (this.isFunctional) {
          object.setMaxItems(1);
        }

        return object;
      }
    }

    ArraySchema objects = new ArraySchema();
    objects.setType("array");

    // For OpenAPI v3.0, "$ref" cannot be used as a sibling with "default".
    // see: https://swagger.io/docs/specification/using-ref/
    // see: https://stackoverflow.com/a/77189463
    // A workaround is to include both within "items" and as separate entries under "allOf".
    // TODO: version check here (and elsewhere?) to differentiate the schema structure.  For v3.1+, it can support "$ref" and "default" as siblings.
    ArraySchema allOfItems = new ArraySchema();
    allOfItems.setType(null);

    for (String restriction:  this.restrictions.keySet()) {
      String value = this.restrictions.get(restriction);

      switch (restriction) {
        case "maxCardinality":
          objects.setMaxItems(Integer.parseInt(value));
          break;
        case "minCardinality":
          objects.setMinItems(Integer.parseInt(value));
          break;
        case "exactCardinality":
          objects.setMaxItems(Integer.parseInt(value));
          objects.setMinItems(Integer.parseInt(value));
          break;
        case "someValuesFrom":
          isNullable = false;
          break;
        case "allValuesFrom":
          //nothing to do in the Schema
          break;
        case "objectHasReference": {
          Schema object = new ObjectSchema();
          object.set$ref(value);
          object.setType(null);
          allOfItems.addAllOfItem(object);
          break;
        }
        case "objectHasValue": {
          Schema object = new ObjectSchema();
          object.setDefault(value);
          object.setType("string");
          allOfItems.addAllOfItem(object);
          break;
        }
        default:
          break;
      }
    }

    if (allOfItems.getAllOf() == null || allOfItems.getAllOf().isEmpty()) {
      Schema object = new ObjectSchema();
      object.set$ref(ref);
      objects.items(object);
    } else {
      objects.items(allOfItems);
    }

    if (this.isFunctional) {
      objects.setMaxItems(1);
    }

    objects.setNullable(isNullable);

    return objects;
  }

  private Schema getComposedSchemaObject(Set<String> refs, boolean array, boolean nullable) {
    Schema object = new ObjectSchema();
    ComposedSchema composedSchema = new ComposedSchema();
    
    object.setType("object");
    object.setDescription(this.description);

    if (array) {
    	ArraySchema objects = new ArraySchema();
    	objects.setDescription(this.description);
    	
    	if (this.isFunctional) {
    		objects.setMaxItems(1);
      }

    	for (String restriction:  this.restrictions.keySet()) { 
        String value = this.restrictions.get(restriction);

        for (String item: refs) {
    			Schema objectRange = new ObjectSchema();
    			objectRange.setType("object");
    			objectRange.set$ref(item);

    			switch (restriction) {
    			case "unionOf":
    				if ("someValuesFrom".equals(value)) {
    					nullable = false;
            }

            if (composedSchema.getAnyOf() == null || !composedSchema.getAnyOf().contains(objectRange)) {
              composedSchema.addAnyOfItem(objectRange);
              objects.setItems(composedSchema);
            }
    				
    				break;
    			case "intersectionOf":
    				if ("someValuesFrom".equals(value)) {
    					nullable = false;
            }

            if (composedSchema.getAllOf() == null || !composedSchema.getAllOf().contains(objectRange)) {
              composedSchema.addAllOfItem(objectRange);
              objects.setItems(composedSchema);
            }

    				break;
    			case "someValuesFrom":
    				nullable = false;

            if (composedSchema.getAnyOf() == null || !composedSchema.getAnyOf().contains(objectRange)) {
              composedSchema.addAnyOfItem(objectRange);
              objects.setItems(composedSchema);
            }

    				break;
    			case "allValuesFrom":
    				//nothing to do in the Schema
    				break;
    	    case "oneOf":
            if ("someValuesFrom".equals(value)) {
              nullable = false;
            }

            Schema enumSchema = new Schema();

            if (composedSchema.getOneOf() == null || composedSchema.getOneOf().isEmpty()) {
              enumSchema.addEnumItemObject(item);
            } else {
              enumSchema = composedSchema.getOneOf().iterator().next();
              enumSchema.addEnumItemObject(item);
              composedSchema = new ComposedSchema();
            }

            composedSchema.addOneOfItem(enumSchema);

            objects.setItems(composedSchema);
            break;
          case "maxCardinality":
            objects.setMaxItems(Integer.parseInt(value));
            break;
          case "minCardinality":
            objects.setMinItems(Integer.parseInt(value));
            break;
          case "exactCardinality":
            objects.setMaxItems(Integer.parseInt(value));
            objects.setMinItems(Integer.parseInt(value));
            break;
    			default:
    				//if the property range is complex it will be omitted   
    				logger.warning("omitted complex restriction");
    				objects.setItems(object);
    			}
    		}
    	}

      objects.setNullable(nullable);

    	if (refs.isEmpty()) {
        if (objects.getItems() != null && objects.getEnum() != null) {
          objects.setItems(object);
        } else {
          objects.setType(null);
          objects.setDescription(null);
          objects.setItems(null);
          objects.setNullable(null);
        }
      }

    	return objects;
    } else {
      object.setNullable(nullable);
      return object;
    }
  }
}
