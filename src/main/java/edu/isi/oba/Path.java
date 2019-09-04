package edu.isi.oba;

import io.swagger.models.Method;
import io.swagger.v3.oas.models.PathItem;

class Path {

  public Path() {
  }

  public PathItem generate_singular(String schemaName){
    return new PathItem()
            .get(new MapperOperation(schemaName, Method.GET, Cardinality.SINGULAR).getOperation())
            .put(new MapperOperation(schemaName, Method.PUT, Cardinality.SINGULAR).getOperation())
            .delete(new MapperOperation(schemaName, Method.DELETE, Cardinality.SINGULAR).getOperation());
  }


  public PathItem generate_plural(String schemaName){
    return new PathItem()
            .get(new MapperOperation(schemaName, Method.GET, Cardinality.PLURAL).getOperation())
            .post(new MapperOperation(schemaName, Method.POST, Cardinality.PLURAL).getOperation());

  }
}