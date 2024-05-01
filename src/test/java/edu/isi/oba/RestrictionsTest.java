package edu.isi.oba;

import static edu.isi.oba.ObaUtils.get_yaml_data;
import edu.isi.oba.config.CONFIG_FLAG;
import edu.isi.oba.config.YamlConfig;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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
	private final Map<CONFIG_FLAG, Boolean> configFlags = Map.ofEntries(
		Map.entry(CONFIG_FLAG.DEFAULT_DESCRIPTIONS, true),
		Map.entry(CONFIG_FLAG.DEFAULT_PROPERTIES, true),
		Map.entry(CONFIG_FLAG.FOLLOW_REFERENCES, true));
	
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
	 * This test attempts to get the OAS representation of the exact cardinality of an ObjectProperty.
	 */
	@Test
	public void testObjectExactCardinality() throws OWLOntologyCreationException, Exception {
		try {
			this.initializeLogger();
			YamlConfig config_data = get_yaml_data("examples/restrictions/config.yaml");
			Mapper mapper = new Mapper(config_data);
			OWLClass cls = mapper.manager.getOWLDataFactory().getOWLClass("https://w3id.org/example#AmericanStudent");
			String desc = ObaUtils.getDescription(cls, mapper.ontologies.get(0), true);
			MapperSchema mapperSchema = new MapperSchema(mapper.ontologies, cls, desc, mapper.schemaNames, mapper.ontologies.get(0), this.configFlags);
			Schema schema = mapperSchema.getSchema();	  	       
			Object property= schema.getProperties().get("hasRecord");		        
			if (property instanceof ArraySchema) {					
				Integer maxItems = ((ArraySchema) property).getMaxItems();
				Integer minItems = ((ArraySchema) property).getMinItems();
				if (maxItems!=null && minItems!=null) {
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
	 */
	@Test
	public void testObjectHasValue() throws OWLOntologyCreationException, Exception {
		try {
			this.initializeLogger();
			String expectedResult = "<https://w3id.org/example/resource/Department/ArtificialIntelligenceDepartment>";
			YamlConfig config_data = get_yaml_data("examples/restrictions/config.yaml");
			Mapper mapper = new Mapper(config_data);
			OWLClass cls = mapper.manager.getOWLDataFactory().getOWLClass("https://w3id.org/example#ProfessorInArtificialIntelligence");
			String desc = ObaUtils.getDescription(cls, mapper.ontologies.get(0), true);
			MapperSchema mapperSchema = new MapperSchema(mapper.ontologies, cls, desc, mapper.schemaNames, mapper.ontologies.get(0), this.configFlags);
			Schema schema = mapperSchema.getSchema();	  	       
			Object property= schema.getProperties().get("belongsTo");	
			if (((ObjectSchema) property).getDefault()!=null)
				Assertions.assertEquals(((ObjectSchema) property).getDefault(),expectedResult);				
			else
				Assertions.fail("Wrong configuration of ObjectHasValue restriction.");
				
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
					itemsValue =((ComposedSchema) items).getAnyOf();
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
			
			if (((ArraySchema) property).getItems().getDefault()!=null)
				Assertions.assertEquals(((ArraySchema) property).getItems().getDefault(),expectedResult);				
			else
				Assertions.fail("Wrong configuration of DataHasValue restriction.");
				
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
				Integer maxItems = ((ArraySchema) property).getItems().getMaxItems();
				Integer minItems = ((ArraySchema) property).getItems().getMinItems();
				if (maxItems!=null && minItems!=null) {
					if (maxItems == minItems)
						// "Exact cardinality configured" -- does this really need to be output for the test?
						return;
					else
						Assertions.fail("Error in exact cardinality restriction.");
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
			Integer minItems = ((ArraySchema) property).getItems().getMinItems();
			if (minItems!=null) 
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
			Integer maxItems = ((ArraySchema) property).getItems().getMaxItems();
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
			if (((ArraySchema) property).getItems().getNot()!=null)
				Assertions.assertEquals(((ArraySchema) property).getItems().getNot().getType(),expectedResult);				
			else
				Assertions.fail("Wrong configuration of ComplementOf restriction.");
				
		} catch (OWLOntologyCreationException e) {			
			Assertions.fail("Error in ontology creation: ", e);
		}	    			
	}

}
