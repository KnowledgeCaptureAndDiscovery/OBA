package edu.isi.oba;

import io.swagger.v3.oas.models.media.*;

import java.util.List;

class MapperObjectProperty {
  final String name;
  final String description;
  private List<String> ref;
  private Boolean array;
  private Boolean nullable;
  final Boolean isFunctional;

  public MapperObjectProperty(String name, String description, Boolean isFunctional, List<String> ref) {
    this.name = name;
    this.description = description;
    this.ref = ref;
    this.array = true;
    this.nullable = true;
    this.isFunctional=isFunctional;
  }

  public MapperObjectProperty(String name, String description,  Boolean isFunctional, List<String> ref, Boolean array, Boolean nullable) {
    this.name = name;
    this.description = description;
    this.ref = ref;
    this.array = array;
    this.nullable = nullable;
    this.isFunctional=isFunctional;
  }

  public Schema getSchemaByObjectProperty(){
    if (this.ref.size() == 0){
      return getComposedSchemaObject(this.ref, array, nullable);
    }

    if (this.ref.size() > 1){
      return getComposedSchemaObject(this.ref, array, nullable);
    }
    else {
      return getObjectPropertiesByRef(this.ref.get(0), array, nullable);
    }
  }

  private Schema getObjectPropertiesByRef(String ref, boolean array, boolean nullable){
    Schema object = new ObjectSchema();
    object.set$ref(ref);
    object.setDescription(description);

    if (array) {
      ArraySchema objects = new ArraySchema();
      objects.setDescription(description);
      objects.setNullable(nullable);
      objects.setItems(object);
      if (isFunctional)
          objects.setMaxItems(1);
      return objects;
    }
    else {
      return object;
    }


  }
  private Schema getComposedSchemaObject(List<String> refs, boolean array, boolean nullable){
    Schema object = new ObjectSchema();
    object.setType("object");
    object.setDescription(description);

    if (array) {
      ArraySchema objects = new ArraySchema();
      objects.setDescription(description);
      objects.setNullable(nullable);
      objects.setItems(object);
      if (isFunctional)
          objects.setMaxItems(1);
      return objects;
    }
    else {
      return object;
    }
  }

  private ArraySchema arraySchema(Schema base, boolean nullable) {
    ArraySchema array = new ArraySchema();
    array.setNullable(nullable);
    array.setItems(base);
    if (isFunctional)
        array.setMaxItems(1);
    return array;
  }

}
