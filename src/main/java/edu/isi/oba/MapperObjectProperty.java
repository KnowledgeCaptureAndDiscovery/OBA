package edu.isi.oba;

import io.swagger.v3.oas.models.media.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class MapperObjectProperty {
  final String name;
  private List<String> ref;
  private Boolean array;
  private Boolean nullable;

  public MapperObjectProperty(String name, List<String> ref) {
    this.name = name;
    this.ref = ref;
    this.array = true;
    this.nullable = true;
  }

  public MapperObjectProperty(String name, List<String> ref, Boolean array, Boolean nullable) {
    this.name = name;
    this.ref = ref;
    this.array = array;
    this.nullable = nullable;
  }

  public Schema getSchemaByObjectProperty(){
    if (this.ref.size() == 0){
      return new ObjectSchema().nullable(nullable);
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
    Schema object = new ObjectSchema();
    object.setType("object");

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

  private ArraySchema arraySchema(Schema base, boolean nullable) {
    ArraySchema array = new ArraySchema();
    array.setNullable(nullable);
    array.setItems(base);
    return array;
  }

}
