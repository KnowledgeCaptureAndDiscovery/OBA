package edu.isi.oba;

import io.swagger.v3.oas.models.media.*;

import static edu.isi.oba.Oba.logger;

import java.util.List;
import java.util.Map;

class MapperObjectProperty {
  final String name;
  final String description;
  private List<String> ref;
  private Boolean array;
  private Boolean nullable;
  final Boolean isFunctional;
  
  final Map<String,String>  restrictions;

  public MapperObjectProperty(String name, String description, Boolean isFunctional, Map<String,String> restrictions, List<String> ref) {
    this.name = name;
    this.description = description;
    this.ref = ref;
    this.array = true;
    this.nullable = true;
    this.isFunctional=isFunctional;
    this.restrictions = restrictions;
  }

  public MapperObjectProperty(String name, String description,  Boolean isFunctional, Map<String,String> restrictions, List<String> ref, Boolean array, Boolean nullable) {
    this.name = name;
    this.description = description;
    this.ref = ref;
    this.array = array;
    this.nullable = nullable;
    this.isFunctional=isFunctional;
    this.restrictions = restrictions;
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
      if (isFunctional)
          objects.setMaxItems(1);
      
      for (String restriction:  restrictions.keySet()) { 
    	  String value = restrictions.get(restriction);
      switch (restriction) {
      case "maxCardinality":
    	objects.setMaxItems(Integer.parseInt(value));
      	break ;
      case "minCardinality":	
    	  objects.setMinItems(Integer.parseInt(value));
      	break ;
      case "exactCardinality":
    	  objects.setMaxItems(Integer.parseInt(value));
    	  objects.setMinItems(Integer.parseInt(value));
      	break ;
      case "someValuesFrom": 
    	  nullable=false;
      	break ;
      case "allValuesFrom":      	  
    	  //nothing to do in the Schema
      	break ;   
      case "objectHasValue":
    	  break ; 
      default:
    	  break ; 
    	} 
      }     
      
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
    ComposedSchema composedSchema = new ComposedSchema() ;
    
    object.setType("object");
    object.setDescription(description);

    if (array) {
    	ArraySchema objects = new ArraySchema();
    	objects.setDescription(description);
    	objects.setNullable(nullable);
    	if (isFunctional)
    		objects.setMaxItems(1);

    	for (String restriction:  restrictions.keySet()) { 
    		String value = restrictions.get(restriction); 

    		for (String item:refs) {
    			Schema objectRange = new ObjectSchema();
    			objectRange.setType("object");
    			objectRange.set$ref(item);
    			switch (restriction) {
    			case "unionOf":
    				if (value=="someValuesFrom")
    					nullable=false;	
    				composedSchema.addAnyOfItem(objectRange);
    				objects.setItems(composedSchema);
    				break ;
    			case "intersectionOf":
    				if (value=="someValuesFrom")
    					nullable=false;	
    				composedSchema.addAllOfItem(objectRange);
    				objects.setItems(composedSchema);
    				break ;
    			case "someValuesFrom": 
    				nullable=false;
    				break;
    			case "allValuesFrom":      	  
    				//nothing to do in the Schema
    				break ;  
    			default:
    				//if the property range is complex it will be omitted   
    				logger.warning("omitted complex restriction");	 
    				objects.setItems(object); 		          
    			}             
    		}
    	}     
    	if (refs.size() == 0)
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
    if (isFunctional)
        array.setMaxItems(1);
    return array;
  }

}
