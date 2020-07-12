package edu.isi.oba;


import static edu.isi.oba.ObaUtils.get_yaml_data;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;


import edu.isi.oba.config.YamlConfig;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;

public class RestrictionsTest {
	static Logger logger = null;
	
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
			String desc = ObaUtils.getDescription(cls, mapper.ontologies.get(0));
			MapperSchema mapperSchema = new MapperSchema(mapper.ontologies, cls, desc, mapper.schemaNames, mapper.ontologies.get(0), true);
			Schema schema = mapperSchema.getSchema();	  	       
			Object property= schema.getProperties().get("hasRector");	       	        
			if (property instanceof io.swagger.v3.oas.models.media.ArraySchema) {	        	
				Integer maxItems = ((ArraySchema) property).getMaxItems();
				assertEquals(expectedResult,maxItems);
			}			
		} catch (OWLOntologyCreationException e) {			
			assertTrue("error in ontology creation", false);
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
			String desc = ObaUtils.getDescription(cls, mapper.ontologies.get(0));
			MapperSchema mapperSchema = new MapperSchema(mapper.ontologies, cls, desc, mapper.schemaNames, mapper.ontologies.get(0), true);
			Schema schema = mapperSchema.getSchema();	  	       
			Object property= schema.getProperties().get("author");		        
			if (property instanceof ArraySchema) {	
				Schema items = ((ArraySchema) property).getItems();
				List<Schema> itemsValue;
				if (items instanceof ComposedSchema) {
					itemsValue =((ComposedSchema) items).getAnyOf();
					for (int i=0; i<itemsValue.size(); i++ ) {
						String ref = itemsValue.get(i).get$ref();
						assertEquals(ref,expectedResult.get(i));
					}	        	
				}	
			} 
		} catch (OWLOntologyCreationException e) {			
			assertTrue("error in ontology creation", false);
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
			String desc = ObaUtils.getDescription(cls, mapper.ontologies.get(0));
			MapperSchema mapperSchema = new MapperSchema(mapper.ontologies, cls, desc, mapper.schemaNames, mapper.ontologies.get(0), true);
			Schema schema = mapperSchema.getSchema();	  	       
			Object property= schema.getProperties().get("hasEvaluationMethod");		        
			if (property instanceof ArraySchema) {	
				Schema items = ((ArraySchema) property).getItems();
				List<Schema> itemsValue;
				if (items instanceof ComposedSchema) {
					itemsValue =((ComposedSchema) items).getAllOf();
					for (int i=0; i<itemsValue.size(); i++ ) {
						String ref = itemsValue.get(i).get$ref();
						assertEquals(ref,expectedResult.get(i));
					}	        	
				}	
			} 
		} catch (OWLOntologyCreationException e) {			
			assertTrue("error in ontology creation", false);
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
			String desc = ObaUtils.getDescription(cls, mapper.ontologies.get(0));
			MapperSchema mapperSchema = new MapperSchema(mapper.ontologies, cls, desc, mapper.schemaNames, mapper.ontologies.get(0), true);
			Schema schema = mapperSchema.getSchema();	  	       
			Object property= schema.getProperties().get("hasDepartment");		        
			if (property instanceof ArraySchema) {	
				Boolean nullable = ((ArraySchema) property).getNullable();
				assertEquals(nullable,expectedResult);					
			} 
		} catch (OWLOntologyCreationException e) {			
			assertTrue("error in ontology creation", false);
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
			String desc = ObaUtils.getDescription(cls, mapper.ontologies.get(0));
			MapperSchema mapperSchema = new MapperSchema(mapper.ontologies, cls, desc, mapper.schemaNames, mapper.ontologies.get(0), true);
			Schema schema = mapperSchema.getSchema();	  	       
			Object property= schema.getProperties().get("enrolledIn");		        
			if (property instanceof ArraySchema) {	
				Boolean nullable = ((ArraySchema) property).getNullable();
				Schema items = ((ArraySchema) property).getItems();
				List<Schema> itemsValue;
				if (items instanceof ComposedSchema) {
					itemsValue =((ComposedSchema) items).getAnyOf();
					for (int i=0; i<itemsValue.size(); i++ ) {			
						assertEquals(itemsValue.get(i).get$ref(),expectedResult.get(i));
					}	        	
				}	
				assertEquals(nullable,false);					
			} 			
		} catch (OWLOntologyCreationException e) {			
			assertTrue("error in ontology creation", false);
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
			String desc = ObaUtils.getDescription(cls, mapper.ontologies.get(0));
			MapperSchema mapperSchema = new MapperSchema(mapper.ontologies, cls, desc, mapper.schemaNames, mapper.ontologies.get(0), true);
			Schema schema = mapperSchema.getSchema();	  	       
			Object property= schema.getProperties().get("hasRecord");		        
			if (property instanceof ArraySchema) {					
				Integer maxItems = ((ArraySchema) property).getMaxItems();
				Integer minItems = ((ArraySchema) property).getMinItems();
				if (maxItems!=null && minItems!=null) {
					if (maxItems == minItems)
						assertTrue("Exact cardinality configured", true);
					else
						assertTrue("Error in exact cardinality restriction", false);
				} else
					assertTrue("Null values in exact cardinality restriction.", false);								
			} 			
		} catch (OWLOntologyCreationException e) {			
			assertTrue("Error in ontology creation", false);
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
			String desc = ObaUtils.getDescription(cls, mapper.ontologies.get(0));
			MapperSchema mapperSchema = new MapperSchema(mapper.ontologies, cls, desc, mapper.schemaNames, mapper.ontologies.get(0), true);
			Schema schema = mapperSchema.getSchema();	  	       
			Object property= schema.getProperties().get("takesCourse");		        
			if (property instanceof ArraySchema) {									
				Integer minItems = ((ArraySchema) property).getMinItems();
				if (minItems!=null) {
					Schema items = ((ArraySchema) property).getItems();
					assertEquals(items.get$ref(),"#/components/schemas/Course");
					assertEquals(minItems,expectedResult);
				} else
					assertTrue("Wrong values in minimum cardinality restriction.", false);								
			} 			
		} catch (OWLOntologyCreationException e) {			
			assertTrue("Error in ontology creation", false);
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
			String desc = ObaUtils.getDescription(cls, mapper.ontologies.get(0));
			MapperSchema mapperSchema = new MapperSchema(mapper.ontologies, cls, desc, mapper.schemaNames, mapper.ontologies.get(0), true);
			Schema schema = mapperSchema.getSchema();	  	       
			Object property= schema.getProperties().get("hasStudentEnrolled");		        
			if (property instanceof ArraySchema) {									
				Integer maxItems = ((ArraySchema) property).getMaxItems();
				if (maxItems!=null) {
					Schema items = ((ArraySchema) property).getItems();
					assertEquals(items.get$ref(),"#/components/schemas/Student");
					assertEquals(maxItems,expectedResult);
				} else
					assertTrue("Wrong values in maximum cardinality restriction.", false);								
			} 			
		} catch (OWLOntologyCreationException e) {			
			assertTrue("Error in ontology creation", false);
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
			String desc = ObaUtils.getDescription(cls, mapper.ontologies.get(0));
			MapperSchema mapperSchema = new MapperSchema(mapper.ontologies, cls, desc, mapper.schemaNames, mapper.ontologies.get(0), true);
			Schema schema = mapperSchema.getSchema();	
			if (schema.getNot()!=null)
				assertEquals(schema.getNot().get$ref(),expectedResult);				
			else
				assertTrue("Wrong configuration of ComplementOf restriction", false);
				
		} catch (OWLOntologyCreationException e) {			
			assertTrue("Error in ontology creation", false);
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
			String desc = ObaUtils.getDescription(cls, mapper.ontologies.get(0));
			MapperSchema mapperSchema = new MapperSchema(mapper.ontologies, cls, desc, mapper.schemaNames, mapper.ontologies.get(0), true);
			Schema schema = mapperSchema.getSchema();	  	       
			Object property= schema.getProperties().get("belongsTo");	
			if (((ObjectSchema) property).getDefault()!=null)
				assertEquals(((ObjectSchema) property).getDefault(),expectedResult);				
			else
				assertTrue("Wrong configuration of ObjectHasValue restriction", false);
				
		} catch (OWLOntologyCreationException e) {			
			assertTrue("Error in ontology creation", false);
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
			String desc = ObaUtils.getDescription(cls, mapper.ontologies.get(0));
			MapperSchema mapperSchema = new MapperSchema(mapper.ontologies, cls, desc, mapper.schemaNames, mapper.ontologies.get(0), true);
			Schema schema = mapperSchema.getSchema();	  	       
			Object property= schema.getProperties().get("hasDegree");		        

			if (property instanceof ArraySchema) {	
				Schema items = ((ArraySchema) property).getItems();
				List<Object> itemsValue;
				if (items instanceof ComposedSchema) {
					itemsValue =((ComposedSchema) items).getEnum();
					for (int i=0; i<itemsValue.size(); i++ ) {
						Object ref = itemsValue.get(i);
						assertEquals(ref.toString(),expectedResult.get(i));
					}	        	
				}	
			} 

		} catch (OWLOntologyCreationException e) {			
			assertTrue("Error in ontology creation", false);
		}	
	}

}
