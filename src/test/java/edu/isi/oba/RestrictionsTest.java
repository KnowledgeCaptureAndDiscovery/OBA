package edu.isi.oba;

import static edu.isi.oba.ObaUtils.get_yaml_data;
import edu.isi.oba.config.CONFIG_FLAG;
import edu.isi.oba.config.YamlConfig;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;

public class RestrictionsTest {
	static Logger logger = null;

	// Convenience variable so we don't need to retype this for each MapperSchema constructor.
	private Map<CONFIG_FLAG, Boolean> configFlags = new HashMap<>(){{
		put(CONFIG_FLAG.ALWAYS_GENERATE_ARRAYS, true);
		put(CONFIG_FLAG.DEFAULT_DESCRIPTIONS, true);
		put(CONFIG_FLAG.DEFAULT_PROPERTIES, true);
		put(CONFIG_FLAG.FOLLOW_REFERENCES, true);
		put(CONFIG_FLAG.REQUIRED_PROPERTIES_FROM_CARDINALITY, true);
	}};
	
	/**
	 * This method allows you to configure the logger variable that is required to print several 
	 * messages during the OBA execution.
	 */
	public void initializeLogger() throws Exception {
		InputStream stream = Oba.class.getClassLoader().getResourceAsStream("logging.properties");
		try {
			LogManager.getLogManager().readConfiguration(stream);
			edu.isi.oba.Oba.logger = Logger.getLogger(Oba.class.getName());

		} catch (IOException e) {
			e.printStackTrace();
		}	    
		edu.isi.oba.Oba.logger.setLevel(Level.FINE);
		edu.isi.oba.Oba.logger.addHandler(new ConsoleHandler());
	}
	
	/**
	 * This test attempts to get the OAS representation of a FunctionalObjectProperty.
	 */
	@Test
	public void testFunctionalObjectProperty() throws OWLOntologyCreationException, Exception {
		try {
			this.initializeLogger();
			Integer expectedResult = 1;
			YamlConfig config_data = get_yaml_data("examples/restrictions/config.yaml");
			Mapper mapper = new Mapper(config_data);
			OWLClass cls = mapper.manager.getOWLDataFactory().getOWLClass("https://w3id.org/example#University");
			String desc = ObaUtils.getDescription(cls, mapper.ontologies.get(0), true);
			MapperSchema mapperSchema = new MapperSchema(mapper.ontologies, cls, desc, mapper.schemaNames, mapper.ontologies.get(0), this.configFlags);
			Schema schema = mapperSchema.getSchema();	  	       
			Object property= schema.getProperties().get("hasRector");	       	        
			if (property instanceof io.swagger.v3.oas.models.media.ArraySchema) {	        	
				Integer maxItems = ((ArraySchema) property).getMaxItems();
				Assertions.assertEquals(expectedResult, maxItems);
			}			
		} catch (OWLOntologyCreationException e) {			
			Assertions.fail("Error in ontology creation: ", e);
		}
	}
	
	/**
	 * This test attempts to get the OAS representation of a ObjectUnionOf restriction.
	 */
	@Test
	public void testObjectUnionOf() throws OWLOntologyCreationException, Exception {		
		List<String> expectedResult = new ArrayList<String>();
		expectedResult.add("#/components/schemas/Organization");
		expectedResult.add("#/components/schemas/Person");
		try {	
			this.initializeLogger();
			YamlConfig config_data = get_yaml_data("examples/restrictions/config.yaml");
			Mapper mapper = new Mapper(config_data);
			OWLClass cls = mapper.manager.getOWLDataFactory().getOWLClass("https://w3id.org/example#StudyMaterial");
			String desc = ObaUtils.getDescription(cls, mapper.ontologies.get(0), true);
			MapperSchema mapperSchema = new MapperSchema(mapper.ontologies, cls, desc, mapper.schemaNames, mapper.ontologies.get(0), this.configFlags);
			Schema schema = mapperSchema.getSchema();	  	       
			Object property= schema.getProperties().get("author");		        
			if (property instanceof ArraySchema) {	
				Schema items = ((ArraySchema) property).getItems();
				List<Schema> itemsValue;
				if (items instanceof ComposedSchema) {
					itemsValue =((ComposedSchema) items).getAnyOf();
					for (int i=0; i<itemsValue.size(); i++ ) {
						String ref = itemsValue.get(i).get$ref();
						Assertions.assertEquals(ref,expectedResult.get(i));
					}	        	
				}	
			} 
		} catch (OWLOntologyCreationException e) {			
			Assertions.fail("Error in ontology creation: ", e);
		}
	}
	
	/**
	 * This test attempts to get the OAS representation of a ObjectIntersectionOf restriction.
	 */
	@Test
	public void testObjectIntersectionOf() throws OWLOntologyCreationException, Exception {
		List<String> expectedResult = new ArrayList<String>();			
		expectedResult.add("#/components/schemas/Assignment");
		expectedResult.add("#/components/schemas/Exam");
		
		try {
			this.initializeLogger();
			YamlConfig config_data = get_yaml_data("examples/restrictions/config.yaml");
			Mapper mapper = new Mapper(config_data);
			OWLClass cls = mapper.manager.getOWLDataFactory().getOWLClass("https://w3id.org/example#Course");
			String desc = ObaUtils.getDescription(cls, mapper.ontologies.get(0), true);
			MapperSchema mapperSchema = new MapperSchema(mapper.ontologies, cls, desc, mapper.schemaNames, mapper.ontologies.get(0), this.configFlags);
			Schema schema = mapperSchema.getSchema();	  	       
			Object property= schema.getProperties().get("hasEvaluationMethod");		        
			if (property instanceof ArraySchema) {	
				Schema items = ((ArraySchema) property).getItems();
				List<Schema> itemsValue;
				if (items instanceof ComposedSchema) {
					itemsValue =((ComposedSchema) items).getAllOf();
					for (int i=0; i<itemsValue.size(); i++ ) {
						String ref = itemsValue.get(i).get$ref();
						Assertions.assertEquals(ref,expectedResult.get(i));
					}	        	
				}	
			} 
		} catch (OWLOntologyCreationException e) {			
			Assertions.fail("Error in ontology creation: ", e);
		}
	}
	
	/**
	 * This test attempts to get the OAS representation of a ObjectSomeValuesFrom without a complex restriction
	 * e.g. it doesn't include a union.
	 */
	@Test
	public void testSimpleObjectSomeValuesFrom() throws OWLOntologyCreationException, Exception {
		try {
			this.initializeLogger();
			Boolean expectedResult=false;
			YamlConfig config_data = get_yaml_data("examples/restrictions/config.yaml");
			Mapper mapper = new Mapper(config_data);
			OWLClass cls = mapper.manager.getOWLDataFactory().getOWLClass("https://w3id.org/example#University");
			String desc = ObaUtils.getDescription(cls, mapper.ontologies.get(0), true);
			MapperSchema mapperSchema = new MapperSchema(mapper.ontologies, cls, desc, mapper.schemaNames, mapper.ontologies.get(0), this.configFlags);
			Schema schema = mapperSchema.getSchema();	  	       
			Object property= schema.getProperties().get("hasDepartment");		        
			if (property instanceof ArraySchema) {	
				Boolean nullable = ((ArraySchema) property).getNullable();
				Assertions.assertEquals(nullable,expectedResult);					
			} 
		} catch (OWLOntologyCreationException e) {			
			Assertions.fail("Error in ontology creation: ", e);
		}
	}

	/**
	 * This test attempts to get the OAS representation of a ObjectSomeValuesFrom which includes another 
	 * restriction e.g. UnionOf.
	 */
	@Test
	public void testObjectSomeValuesFrom_ComposedByRestriction() throws OWLOntologyCreationException, Exception {
		List<String> expectedResult = new ArrayList<String>();			
		expectedResult.add("#/components/schemas/BachelorProgram");
		expectedResult.add("#/components/schemas/MasterProgram");
		expectedResult.add("#/components/schemas/PhDProgram");
		try {
			this.initializeLogger();
			YamlConfig config_data = get_yaml_data("examples/restrictions/config.yaml");
			Mapper mapper = new Mapper(config_data);
			OWLClass cls = mapper.manager.getOWLDataFactory().getOWLClass("https://w3id.org/example#Student");
			String desc = ObaUtils.getDescription(cls, mapper.ontologies.get(0), true);
			MapperSchema mapperSchema = new MapperSchema(mapper.ontologies, cls, desc, mapper.schemaNames, mapper.ontologies.get(0), this.configFlags);
			Schema schema = mapperSchema.getSchema();	  	       
			Object property= schema.getProperties().get("enrolledIn");		        
			if (property instanceof ArraySchema) {	
				Boolean nullable = ((ArraySchema) property).getNullable();
				Schema items = ((ArraySchema) property).getItems();
				List<Schema> itemsValue;
				if (items instanceof ComposedSchema) {
					itemsValue =((ComposedSchema) items).getAnyOf();
					for (int i=0; i<itemsValue.size(); i++ ) {			
						Assertions.assertEquals(itemsValue.get(i).get$ref(),expectedResult.get(i));
					}	        	
				}	
				//Assertions.assertEquals(nullable, false);					
			} 			
		} catch (OWLOntologyCreationException e) {			
			Assertions.fail("Error in ontology creation: ", e);
		}	

	}

	/**
	 * This test attempts to get the OAS representation of the exact cardinality of an ObjectProperty,
	 * when arrays are set to always be generated for properties.
	 */
	@Test
	public void testObjectExactCardinalityWithArraysGenerated() throws OWLOntologyCreationException, Exception {
		try {
			this.initializeLogger();
			YamlConfig config_data = get_yaml_data("examples/restrictions/config.yaml");
			Mapper mapper = new Mapper(config_data);
			OWLClass cls = mapper.manager.getOWLDataFactory().getOWLClass("https://w3id.org/example#AmericanStudent");
			String desc = ObaUtils.getDescription(cls, mapper.ontologies.get(0), true);

			this.configFlags.put(CONFIG_FLAG.ALWAYS_GENERATE_ARRAYS, true);
			MapperSchema mapperSchema = new MapperSchema(mapper.ontologies, cls, desc, mapper.schemaNames, mapper.ontologies.get(0), this.configFlags);
			Schema schema = mapperSchema.getSchema();	  	       
			Object property= schema.getProperties().get("hasRecord");		        
			if (property instanceof ArraySchema) {					
				Integer maxItems = ((ArraySchema) property).getMaxItems();
				Integer minItems = ((ArraySchema) property).getMinItems();
				if (maxItems != null && minItems != null) {
					if (maxItems == minItems)
						// "Exact cardinality configured" -- does this really need to be output for the test?
						return;
					else
						Assertions.fail("Error in exact cardinality restriction.");
				} else
					Assertions.fail("Null values in exact cardinality restriction.");								
			} 			
		} catch (OWLOntologyCreationException e) {			
			Assertions.fail("Error in ontology creation: ", e);
		}	  
	}
	
	/**
	 * This test attempts to get the OAS representation of the exact cardinality of an ObjectProperty,
	 * when properties may or may not be arrays, depending on cardinality.
	 */
	@Test
	public void testObjectExactCardinalityWithoutArraysGenerated() throws OWLOntologyCreationException, Exception {
		try {
			this.initializeLogger();
			YamlConfig config_data = get_yaml_data("examples/restrictions/config.yaml");
			Mapper mapper = new Mapper(config_data);
			OWLClass cls = mapper.manager.getOWLDataFactory().getOWLClass("https://w3id.org/example#AmericanStudent");
			String desc = ObaUtils.getDescription(cls, mapper.ontologies.get(0), true);

			this.configFlags.put(CONFIG_FLAG.ALWAYS_GENERATE_ARRAYS, false);
			MapperSchema mapperSchema = new MapperSchema(mapper.ontologies, cls, desc, mapper.schemaNames, mapper.ontologies.get(0), this.configFlags);
			Schema schema = mapperSchema.getSchema();

			boolean isRequired = schema.getRequired() != null && schema.getRequired().contains("hasRecord");

			// For exact cardinality, the class schema should have it marked as required.
			if (isRequired) {
				Object property= schema.getProperties().get("hasRecord");
				Integer maxItems = null;
				Integer minItems = null;
				
				// If the property is an array, it is an object property (i.e. has a "$ref" value).  Otherwise, it is a data property.
				if (property instanceof ArraySchema) {
					maxItems = ((ArraySchema) property).getMaxItems();
					minItems = ((ArraySchema) property).getMinItems();
					//return;
				} else if (property instanceof ObjectSchema) {
					maxItems = ((ObjectSchema) property).getMaxItems();
					minItems = ((ObjectSchema) property).getMinItems();
				}

				// Property is known to be required.  And cardinality restrictions should have also been removed, so min/max items should be null.
				if (minItems == null && maxItems == null) {
					return;
				} else {
					Assertions.fail("Error in exact cardinality restriction.");
				}
			} else {
				Assertions.fail("Error in exact cardinality restriction.");
			}
		} catch (OWLOntologyCreationException e) {
			Assertions.fail("Error in ontology creation: ", e);
		}
	}
	
	/**
	 * This test attempts to get the OAS representation of the minimum cardinality of an ObjectProperty.
	 */
	@Test
	public void testObjectMinCardinality() throws OWLOntologyCreationException, Exception {
		try {
			this.initializeLogger();
			Integer expectedResult = 2;
			YamlConfig config_data = get_yaml_data("examples/restrictions/config.yaml");
			Mapper mapper = new Mapper(config_data);
			OWLClass cls = mapper.manager.getOWLDataFactory().getOWLClass("https://w3id.org/example#AmericanStudent");
			String desc = ObaUtils.getDescription(cls, mapper.ontologies.get(0), true);
			MapperSchema mapperSchema = new MapperSchema(mapper.ontologies, cls, desc, mapper.schemaNames, mapper.ontologies.get(0), this.configFlags);
			Schema schema = mapperSchema.getSchema();	  	       
			Object property= schema.getProperties().get("takesCourse");		        
			if (property instanceof ArraySchema) {									
				Integer minItems = ((ArraySchema) property).getMinItems();
				if (minItems!=null) {
					Schema items = ((ArraySchema) property).getItems();
					Assertions.assertEquals(items.get$ref(),"#/components/schemas/Course");
					Assertions.assertEquals(minItems,expectedResult);
				} else
					Assertions.fail("Wrong values in minimum cardinality restriction.");								
			} 			
		} catch (OWLOntologyCreationException e) {			
			Assertions.fail("Error in ontology creation: ", e);
		}	  
	}
	
	/**
	 * This test attempts to get the OAS representation of the maximum cardinality of an ObjectProperty.
	 */
	@Test
	public void testObjectMaxCardinality() throws OWLOntologyCreationException, Exception {
		try {
			this.initializeLogger();
			Integer expectedResult = 20;
			YamlConfig config_data = get_yaml_data("examples/restrictions/config.yaml");
			Mapper mapper = new Mapper(config_data);
			OWLClass cls = mapper.manager.getOWLDataFactory().getOWLClass("https://w3id.org/example#Course");
			String desc = ObaUtils.getDescription(cls, mapper.ontologies.get(0), true);
			MapperSchema mapperSchema = new MapperSchema(mapper.ontologies, cls, desc, mapper.schemaNames, mapper.ontologies.get(0), this.configFlags);
			Schema schema = mapperSchema.getSchema();	  	       
			Object property= schema.getProperties().get("hasStudentEnrolled");		        
			if (property instanceof ArraySchema) {									
				Integer maxItems = ((ArraySchema) property).getMaxItems();
				if (maxItems!=null) {
					Schema items = ((ArraySchema) property).getItems();
					Assertions.assertEquals(items.get$ref(),"#/components/schemas/Student");
					Assertions.assertEquals(maxItems,expectedResult);
				} else
					Assertions.fail("Wrong values in maximum cardinality restriction.");								
			} 			
		} catch (OWLOntologyCreationException e) {			
			Assertions.fail("Error in ontology creation: ", e);
		}	

	}
	

	/**
	 * This test attempts to get the OAS representation of the complementOf of an ObjectProperty.
	 */
	@Test
	public void testObjectComplementOf() throws OWLOntologyCreationException, Exception {
		try {
			this.initializeLogger();
			String expectedResult = "#/components/schemas/ProfessorInArtificialIntelligence";
			YamlConfig config_data = get_yaml_data("examples/restrictions/config.yaml");
			Mapper mapper = new Mapper(config_data);
			OWLClass cls = mapper.manager.getOWLDataFactory().getOWLClass("https://w3id.org/example#ProfessorInOtherDepartment");
			String desc = ObaUtils.getDescription(cls, mapper.ontologies.get(0), true);
			MapperSchema mapperSchema = new MapperSchema(mapper.ontologies, cls, desc, mapper.schemaNames, mapper.ontologies.get(0), this.configFlags);
			Schema schema = mapperSchema.getSchema();	
			if (schema.getNot()!=null)
				Assertions.assertEquals(schema.getNot().get$ref(),expectedResult);				
			else
				Assertions.fail("Wrong configuration of ComplementOf restriction.");
				
		} catch (OWLOntologyCreationException e) {			
			Assertions.fail("Error in ontology creation: ", e);
		}	
	}
	
	/**
	 * This test attempts to get the OAS representation of the hasValue of an ObjectProperty.
	 * 
	 * This corresponds to a named individual (of a particular class).  Before version 3.1.0, OpenAPI does not support
	 * the "$ref" at the same level of a default value.  For this reason, they are treated as separate items
	 * under the "allOf" key (which is an array type).  The "$ref" value is the named individual's class and the "default" value is
	 * the named individual's name.
	 */
	@Test
	public void testObjectHasValue() throws OWLOntologyCreationException, Exception {
		try {
			this.initializeLogger();
			// The short form of: "<https://w3id.org/example/resource/Department/ArtificialIntelligenceDepartment>"
			// resolves to the default value of: "ArtificialIntelligenceDepartment".
			String expectedResult = "ArtificialIntelligenceDepartment";
			YamlConfig config_data = get_yaml_data("examples/restrictions/config.yaml");
			Mapper mapper = new Mapper(config_data);
			OWLClass cls = mapper.manager.getOWLDataFactory().getOWLClass("https://w3id.org/example#ProfessorInArtificialIntelligence");
			String desc = ObaUtils.getDescription(cls, mapper.ontologies.get(0), true);
			MapperSchema mapperSchema = new MapperSchema(mapper.ontologies, cls, desc, mapper.schemaNames, mapper.ontologies.get(0), this.configFlags);
			Schema schema = mapperSchema.getSchema();	  	       
			Object property = schema.getProperties().get("belongsTo");	

			if (property instanceof ArraySchema) {
				Schema items = ((ArraySchema) property).getItems();
				if (items != null && (items.getAllOf() != null && !items.getAllOf().isEmpty())) {
					for (var item: items.getAllOf()) {
						if (((ObjectSchema) item).getDefault() != null) {
							Assertions.assertEquals(((ObjectSchema) item).getDefault(), expectedResult);
							return;
						}
					}

					// If we reach here, then none of the items had a default value, so fail.
					Assertions.fail("Wrong configuration of ObjectHasValue restriction.");
				} else {
					Assertions.fail("Wrong configuration of ObjectHasValue restriction.");
				}				
			} else {
				Assertions.fail("Wrong configuration of ObjectHasValue restriction.");
			}
		} catch (OWLOntologyCreationException e) {			
			Assertions.fail("Error in ontology creation: ", e);
		}	    
	}
	
	/**
	 * This test attempts to get the OAS representation of the oneOf restriction of an ObjectProperty.
	 */
	@Test
	public void testObjectOneOf() throws OWLOntologyCreationException, Exception {
		try {
			this.initializeLogger();
			List<String> expectedResult = new ArrayList<String>();
			expectedResult.add("<https://w3id.org/example/resource/Degree/MS>");
			expectedResult.add("<https://w3id.org/example/resource/Degree/PhD>");
			YamlConfig config_data = get_yaml_data("examples/restrictions/config.yaml");
			Mapper mapper = new Mapper(config_data);
			OWLClass cls = mapper.manager.getOWLDataFactory().getOWLClass("https://w3id.org/example#Professor");
			String desc = ObaUtils.getDescription(cls, mapper.ontologies.get(0), true);
			MapperSchema mapperSchema = new MapperSchema(mapper.ontologies, cls, desc, mapper.schemaNames, mapper.ontologies.get(0), this.configFlags);
			Schema schema = mapperSchema.getSchema();	  	       
			Object property= schema.getProperties().get("hasDegree");		        

			if (property instanceof ArraySchema) {	
				Schema items = ((ArraySchema) property).getItems();
				List<Object> itemsValue;
				if (items instanceof ComposedSchema) {
					itemsValue =((ComposedSchema) items).getEnum();
					for (int i=0; i<itemsValue.size(); i++ ) {
						Object ref = itemsValue.get(i);
						Assertions.assertEquals(ref.toString(),expectedResult.get(i));
					}	        	
				}	
			} 

		} catch (OWLOntologyCreationException e) {			
			Assertions.fail("Error in ontology creation: ", e);
		}	
	}

	
	/**
	 * This test attempts to get the OAS representation of a FunctionalDataProperty.
	 */
	@Test
	public void testFunctionalDataProperty() throws OWLOntologyCreationException, Exception {
		try {
			this.initializeLogger();
			Integer expectedResult = 1;
			YamlConfig config_data = get_yaml_data("examples/restrictions/config.yaml");
			Mapper mapper = new Mapper(config_data);
			OWLClass cls = mapper.manager.getOWLDataFactory().getOWLClass("https://w3id.org/example#AmericanStudent");
			String desc = ObaUtils.getDescription(cls, mapper.ontologies.get(0), true);
			MapperSchema mapperSchema = new MapperSchema(mapper.ontologies, cls, desc, mapper.schemaNames, mapper.ontologies.get(0), this.configFlags);
			Schema schema = mapperSchema.getSchema();	  	       
			Object property= schema.getProperties().get("birthDate");	       	        
			if (property instanceof io.swagger.v3.oas.models.media.ArraySchema) {	        	
				Integer maxItems = ((ArraySchema) property).getMaxItems();
				Assertions.assertEquals(expectedResult,maxItems);
			}			
		} catch (OWLOntologyCreationException e) {			
			Assertions.fail("error in ontology creation: ", e);
		}
	}
	
	/**
	 * This test attempts to get the OAS representation of the UnionOf restriction of a DataProperty.
	 */
	@Test
	public void testDataUnionOf() throws OWLOntologyCreationException, Exception {
		try {
			this.initializeLogger();
			List<String> expectedResult = new ArrayList<String>();
			expectedResult.add("number");
			expectedResult.add("integer");

			YamlConfig config_data = get_yaml_data("examples/restrictions/config.yaml");
			Mapper mapper = new Mapper(config_data);
			OWLClass cls = mapper.manager.getOWLDataFactory().getOWLClass("https://w3id.org/example#Course");
			String desc = ObaUtils.getDescription(cls, mapper.ontologies.get(0), true);
			MapperSchema mapperSchema = new MapperSchema(mapper.ontologies, cls, desc, mapper.schemaNames, mapper.ontologies.get(0), this.configFlags);
			Schema schema = mapperSchema.getSchema();	  	       
			Object property= schema.getProperties().get("ects");		        
			if (property instanceof ArraySchema) {	
				Schema items = ((ArraySchema) property).getItems();
				List<Schema> itemsValue;
				if (items instanceof ComposedSchema) {
					itemsValue = ((ComposedSchema) items).getAnyOf();
					if (itemsValue != null) {
						for (int i = 0; i < itemsValue.size(); i++) {
							String ref = itemsValue.get(i).getType();
							Assertions.assertEquals(ref,expectedResult.get(i));
						}
					}
				}	
			} 
		} catch (OWLOntologyCreationException e) {			
			Assertions.fail("error in ontology creation: ", e);
		}
	}

	/**
	 * This test attempts to get the OAS representation of the UnionOf restriction of a DataProperty.
	 */
	@Test
	public void testDataIntersectionOf() throws OWLOntologyCreationException, Exception {
		try {
			this.initializeLogger();
			List<String> expectedResult = new ArrayList<String>();
			expectedResult.add("integer");
			expectedResult.add("integer");

			YamlConfig config_data = get_yaml_data("examples/restrictions/config.yaml");
			Mapper mapper = new Mapper(config_data);
			OWLClass cls = mapper.manager.getOWLDataFactory().getOWLClass("https://w3id.org/example#ProfessorInArtificialIntelligence");
			String desc = ObaUtils.getDescription(cls, mapper.ontologies.get(0), true);
			MapperSchema mapperSchema = new MapperSchema(mapper.ontologies, cls, desc, mapper.schemaNames, mapper.ontologies.get(0), this.configFlags);
			Schema schema = mapperSchema.getSchema();	  	       
			Object property= schema.getProperties().get("memberOfOtherDepartments");		        
			if (property instanceof ArraySchema) {	
				Schema items = ((ArraySchema) property).getItems();
				List<Schema> itemsValue;
				if (items instanceof ComposedSchema) {
					itemsValue =((ComposedSchema) items).getAllOf();
					for (int i=0; i<itemsValue.size(); i++ ) {
						String ref = itemsValue.get(i).getType();
						Assertions.assertEquals(ref,expectedResult.get(i));
					}	        	
				}	
			} 
		} catch (OWLOntologyCreationException e) {			
			Assertions.fail("error in ontology creation: ", e);
		}
	}
	
	/**
	 * This test attempts to get the OAS representation of the SomeValuesFrom restriction of a DataProperty.
	 */
	@Disabled
	@Test
	public void testDataSomeValuesFrom() throws OWLOntologyCreationException, Exception {
		try {
			this.initializeLogger();
			String expectedResult = "string";
			YamlConfig config_data = get_yaml_data("examples/restrictions/config.yaml");
			Mapper mapper = new Mapper(config_data);
			OWLClass cls = mapper.manager.getOWLDataFactory().getOWLClass("https://w3id.org/example#StudyProgram");
			String desc = ObaUtils.getDescription(cls, mapper.ontologies.get(0), true);
			MapperSchema mapperSchema = new MapperSchema(mapper.ontologies, cls, desc, mapper.schemaNames, mapper.ontologies.get(0), this.configFlags);
			Schema schema = mapperSchema.getSchema();	  	       
			Object property= schema.getProperties().get("studyProgramName");		        
			if (property instanceof ArraySchema) {	
				Boolean nullable = ((ArraySchema) property).getNullable();
				Assertions.assertEquals(((ArraySchema) property).getItems().getType(), expectedResult);
				Assertions.assertEquals(nullable, false);	
			} 
		} catch (OWLOntologyCreationException e) {			
			Assertions.fail("error in ontology creation: ", e);
		}
	}

	/**
	 * This test attempts to get the OAS representation of a DataSomeValuesFrom which includes another restriction.
	 * e.g. IntersectionOf
	 */
	@Test
	public void testDataSomeValuesFrom_ComposedByRestriction() throws OWLOntologyCreationException, Exception {
		try {
			this.initializeLogger();
			List<String> expectedResult = new ArrayList<String>();			
			expectedResult.add("integer");
			expectedResult.add("integer");
			YamlConfig config_data = get_yaml_data("examples/restrictions/config.yaml");
			Mapper mapper = new Mapper(config_data);
			OWLClass cls = mapper.manager.getOWLDataFactory().getOWLClass("https://w3id.org/example#ProfessorInArtificialIntelligence");
			String desc = ObaUtils.getDescription(cls, mapper.ontologies.get(0), true);
			MapperSchema mapperSchema = new MapperSchema(mapper.ontologies, cls, desc, mapper.schemaNames, mapper.ontologies.get(0), this.configFlags);
			Schema schema = mapperSchema.getSchema();	  	       
			Object property= schema.getProperties().get("memberOfOtherDepartments");		        
			if (property instanceof ArraySchema) {	
				Boolean nullable = ((ArraySchema) property).getNullable();
				Schema items = ((ArraySchema) property).getItems();
				List<Schema> itemsValue;
				if (items instanceof ComposedSchema) {
					itemsValue =((ComposedSchema) items).getAllOf();
					for (int i=0; i<itemsValue.size(); i++ ) {			
						Assertions.assertEquals(itemsValue.get(i).getType(),expectedResult.get(i));
					}	        	
				}	
				Assertions.assertEquals(nullable, false);					
			} 			
		} catch (OWLOntologyCreationException e) {			
			Assertions.fail("error in ontology creation: ", e);
		}	    		
	}
	
	/**
	 * This test attempts to get the OAS representation of the AllValuesFrom restriction of a DataProperty.
	 */
	@Test
	public void testDataAllValuesFrom() throws OWLOntologyCreationException, Exception {
		try {
			this.initializeLogger();
			String expectedResult = "string";
			YamlConfig config_data = get_yaml_data("examples/restrictions/config.yaml");
			Mapper mapper = new Mapper(config_data);
			OWLClass cls = mapper.manager.getOWLDataFactory().getOWLClass("https://w3id.org/example#StudyProgram");
			String desc = ObaUtils.getDescription(cls, mapper.ontologies.get(0), true);
			MapperSchema mapperSchema = new MapperSchema(mapper.ontologies, cls, desc, mapper.schemaNames, mapper.ontologies.get(0), this.configFlags);
			Schema schema = mapperSchema.getSchema();	  	       
			Object property= schema.getProperties().get("studyProgramName");		        
			if (property instanceof ArraySchema) {	
				Assertions.assertEquals(((ArraySchema) property).getItems().getType(), expectedResult);
			} 
		} catch (OWLOntologyCreationException e) {			
			Assertions.fail("Error in ontology creation: ", e);
		}
	}
	
	/**
	 * This test attempts to get the OAS representation of the OneOf restriction of a DataProperty.
	 */
	@Test
	public void testDataOneOf() throws OWLOntologyCreationException, Exception {
		try {
			this.initializeLogger();
			List<String> expectedResult = new ArrayList<String>();			
			expectedResult.add("female");
			expectedResult.add("male");
			YamlConfig config_data = get_yaml_data("examples/restrictions/config.yaml");
			Mapper mapper = new Mapper(config_data);
			OWLClass cls = mapper.manager.getOWLDataFactory().getOWLClass("https://w3id.org/example#Person");
			String desc = ObaUtils.getDescription(cls, mapper.ontologies.get(0), true);
			MapperSchema mapperSchema = new MapperSchema(mapper.ontologies, cls, desc, mapper.schemaNames, mapper.ontologies.get(0), this.configFlags);
			Schema schema = mapperSchema.getSchema();	  	       
			Object property= schema.getProperties().get("gender");	
			if (property instanceof ArraySchema) {	
				Schema items = ((ArraySchema) property).getItems();
				List<Object> itemsValue;
				if (items instanceof ComposedSchema) {
					itemsValue =((ComposedSchema) items).getEnum();
					for (int i=0; i<itemsValue.size(); i++ ) {			
						Assertions.assertEquals(itemsValue.get(i),expectedResult.get(i));
					}	        	
				}						
			} 			
			else
				Assertions.fail("Wrong configuration of DataOneOf restriction.");
				
		} catch (OWLOntologyCreationException e) {			
			Assertions.fail("Error in ontology creation: ", e);
		}	    			
	}
	
	/**
	 * This test attempts to get the OAS representation of the hasValue restriction of a DataProperty.
	 */
	@Test
	public void testDataHasValue() throws OWLOntologyCreationException, Exception {
		try {
			this.initializeLogger();
			String expectedResult = "American";
			YamlConfig config_data = get_yaml_data("examples/restrictions/config.yaml");
			Mapper mapper = new Mapper(config_data);
			OWLClass cls = mapper.manager.getOWLDataFactory().getOWLClass("https://w3id.org/example#AmericanStudent");
			String desc = ObaUtils.getDescription(cls, mapper.ontologies.get(0), true);
			MapperSchema mapperSchema = new MapperSchema(mapper.ontologies, cls, desc, mapper.schemaNames, mapper.ontologies.get(0), this.configFlags);
			Schema schema = mapperSchema.getSchema();	  	       
			Object property= schema.getProperties().get("nationality");	
			
			if (property instanceof ArraySchema && ((ArraySchema) property).getItems().getDefault() != null) {
				Assertions.assertEquals(((ArraySchema) property).getItems().getDefault(), expectedResult);
			} else if (((Schema) property).getDefault() != null) {
				Assertions.assertEquals(((Schema) property).getDefault(), expectedResult);
			} else {
				Assertions.fail("Wrong configuration of DataHasValue restriction.");
			}
		} catch (OWLOntologyCreationException e) {			
			Assertions.fail("Error in ontology creation: ", e);
		}	    			
	}
	
	/**
	 * This test attempts to get the OAS representation of the exact cardinality of a DataProperty.
	 */
	@Test
	public void testDataExactCardinality() throws OWLOntologyCreationException,Exception {
		try {
			this.initializeLogger();
			YamlConfig config_data = get_yaml_data("examples/restrictions/config.yaml");
			Mapper mapper = new Mapper(config_data);
			OWLClass cls = mapper.manager.getOWLDataFactory().getOWLClass("https://w3id.org/example#University");
			String desc = ObaUtils.getDescription(cls, mapper.ontologies.get(0), true);
			MapperSchema mapperSchema = new MapperSchema(mapper.ontologies, cls, desc, mapper.schemaNames, mapper.ontologies.get(0), this.configFlags);
			Schema schema = mapperSchema.getSchema();	  	       
			Object property= schema.getProperties().get("universityName");

			if (property instanceof ArraySchema) {
				Integer maxItems = ((ArraySchema) property).getItems().getMaxItems();
				Integer minItems = ((ArraySchema) property).getItems().getMinItems();
				if (maxItems!=null && minItems!=null) {
					if (maxItems == minItems) {
						// "Exact cardinality configured" -- does this really need to be output for the test?
						return;
					} else {
						Assertions.fail("Error in exact cardinality restriction.");
					}
				} 
			} else if (property instanceof Schema) {
				Integer maxItems = ((Schema) property).getMaxItems();
				Integer minItems = ((Schema) property).getMinItems();
				if (maxItems!=null && minItems!=null) {
					if (maxItems == minItems) {
						// "Exact cardinality configured" -- does this really need to be output for the test?
						return;
					} else {
						Assertions.fail("Error in exact cardinality restriction.");
					}
				} 
			} else {
				Assertions.fail("Wrong configuration of cardinality restriction.");
			}
		} catch (OWLOntologyCreationException e) {			
			Assertions.fail("Error in ontology creation: ", e);
		}	    			
	}
	
	/**
	 * This test attempts to get the OAS representation of the minimum cardinality of a DataProperty.
	 */
	@Test
	public void testDataMinCardinality() throws OWLOntologyCreationException, Exception {
		try {
			this.initializeLogger();
			Integer expectedResult = 1;
			YamlConfig config_data = get_yaml_data("examples/restrictions/config.yaml");
			Mapper mapper = new Mapper(config_data);
			OWLClass cls = mapper.manager.getOWLDataFactory().getOWLClass("https://w3id.org/example#Professor");
			String desc = ObaUtils.getDescription(cls, mapper.ontologies.get(0), true);
			MapperSchema mapperSchema = new MapperSchema(mapper.ontologies, cls, desc, mapper.schemaNames, mapper.ontologies.get(0), this.configFlags);
			Schema schema = mapperSchema.getSchema();	  	       
			Object property= schema.getProperties().get("researchField");		        					
			Integer minItems = null;

			if (property instanceof ArraySchema) {
				minItems = ((ArraySchema) property).getItems().getMinItems();
			} else if (property instanceof Schema) {
				minItems = ((Schema) property).getMinItems();
			} else {
				Assertions.fail("Wrong configuration of minimum cardinality restriction.");
			}

			if (minItems != null) 
				Assertions.assertEquals(minItems,expectedResult);
			else
				Assertions.fail("Error in minimum cardinality restriction.");
						
		} catch (OWLOntologyCreationException e) {			
			Assertions.fail("Error in ontology creation: ", e);
		}	    			
	}
	
	/**
	 * This test attempts to get the OAS representation of the maximum cardinality of a DataProperty.
	 */
	@Test
	public void testDataMaxCardinality() throws OWLOntologyCreationException, Exception {
		try {
			this.initializeLogger();
			Integer expectedResult = 2;
			YamlConfig config_data = get_yaml_data("examples/restrictions/config.yaml");
			Mapper mapper = new Mapper(config_data);
			OWLClass cls = mapper.manager.getOWLDataFactory().getOWLClass("https://w3id.org/example#Person");
			String desc = ObaUtils.getDescription(cls, mapper.ontologies.get(0), true);
			MapperSchema mapperSchema = new MapperSchema(mapper.ontologies, cls, desc, mapper.schemaNames, mapper.ontologies.get(0), this.configFlags);
			Schema schema = mapperSchema.getSchema();	  	       
			Object property= schema.getProperties().get("address");		        					
			Integer maxItems = null;

			if (property instanceof ArraySchema) {
				maxItems = ((ArraySchema) property).getItems().getMaxItems();
			} else if (property instanceof Schema) {
				maxItems = ((Schema) property).getMaxItems();
			} else {
				Assertions.fail("Wrong configuration of minimum cardinality restriction.");
			}

			if (maxItems!=null) 
				Assertions.assertEquals(maxItems,expectedResult);
			else
				Assertions.fail("Error in maximum cardinality restriction.");

		} catch (OWLOntologyCreationException e) {			
			Assertions.fail("Error in ontology creation: ", e);
		}	    			
	}
	
	/**
	 * This test attempts to get the OAS representation of the complementOf of a DataProperty.
	 */
	@Test
	public void testDataComplementOf() throws OWLOntologyCreationException,Exception {
		try {
			this.initializeLogger();
			String expectedResult = "integer";
			YamlConfig config_data = get_yaml_data("examples/restrictions/config.yaml");
			Mapper mapper = new Mapper(config_data);
			OWLClass cls = mapper.manager.getOWLDataFactory().getOWLClass("https://w3id.org/example#Department");
			String desc = ObaUtils.getDescription(cls, mapper.ontologies.get(0), true);
			MapperSchema mapperSchema = new MapperSchema(mapper.ontologies, cls, desc, mapper.schemaNames, mapper.ontologies.get(0), this.configFlags);
			Schema schema = mapperSchema.getSchema();	
			Object property= schema.getProperties().get("numberOfProfessors");

			if (property instanceof ArraySchema && ((ArraySchema) property).getItems().getNot().getType() != null) {
				Assertions.assertEquals(((ArraySchema) property).getItems().getNot().getType(), expectedResult);
			} else if (((Schema) property).getNot().getType() != null) {
				Assertions.assertEquals(((Schema) property).getNot().getType(), expectedResult);
			} else {
				Assertions.fail("Wrong configuration of ComplementOf restriction.");
			}
		} catch (OWLOntologyCreationException e) {			
			Assertions.fail("Error in ontology creation: ", e);
		}	    			
	}

}
