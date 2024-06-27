package edu.isi.oba;

import edu.isi.oba.config.YamlConfig;

import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.semanticweb.owlapi.model.OWLOntologyCreationException;

public class RestrictionsTest {
	static Logger logger = null;
	private YamlConfig configData = ObaUtils.get_yaml_data("examples/restrictions/config.yaml");
	private Mapper mapper;

	private void setupMapper() throws Exception {
		try {
			this.mapper = new Mapper(this.configData);
			// Use temporary directory for unit testing
			mapper.createSchemas("examples/restrictions/ObjectVisitorTest/");

			// If no schemas are returned from the mapper, something is wrong.  Probably with the ontology(?).
			final var schemas = this.mapper.getSchemas();
			Assertions.assertNotNull(schemas);
		} catch (OWLOntologyCreationException e) {
			Assertions.fail("Error in ontology creation: ", e);
		}
	}

	@BeforeEach
	public void setupLoggerAndMapper() throws Exception {
		this.initializeLogger();

		this.configData.setAlways_generate_arrays(true);
		this.configData.setDefault_descriptions(true);
		this.configData.setDefault_properties(true);
		this.configData.setFollow_references(true);
		this.configData.setRequired_properties_from_cardinality(false);

		this.setupMapper();
	}

	@AfterAll
	public static void removeUnitTestFiles() throws Exception {
		// Delete temporary directory now
		Files.walk(Paths.get("examples/restrictions/ObjectVisitorTest/"))
			.sorted(Comparator.reverseOrder())
			.map(Path::toFile)
			.forEach(File::delete);
	}
	
	/**
	 * This method allows you to configure the logger variable that is required to print several 
	 * messages during the OBA execution.
	 */
	public void initializeLogger() throws Exception {
		final var stream = Oba.class.getClassLoader().getResourceAsStream("logging.properties");

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
		// Expected value
		final var expectedResult = 1;

		// Get the class schema and make sure it has properties.
		final var schema = this.mapper.getSchemas().get("University");
		Assertions.assertNotNull(schema);
		Assertions.assertNotNull(schema.getProperties());

		// Get the property to check object property is functional.
		final var property = (Schema) schema.getProperties().get("hasRector");
		Assertions.assertNotNull(property);
		Assertions.assertEquals(expectedResult, property.getMaxItems());
	}
	
	/**
	 * This test attempts to get the OAS representation of a ObjectUnionOf restriction.
	 */
	@Test
	public void testObjectUnionOf() throws OWLOntologyCreationException, Exception {
		// Expected value
		final var expectedResult = new ArrayList<String>();
		expectedResult.add("#/components/schemas/Organization");
		expectedResult.add("#/components/schemas/Person");

		// Get the class schema and make sure it has properties.
		final var schema = this.mapper.getSchemas().get("StudyMaterial");
		Assertions.assertNotNull(schema);
		Assertions.assertNotNull(schema.getProperties());

		// Get the property to check object property unionOf.
		final var property = (Schema) schema.getProperties().get("author");
		Assertions.assertNotNull(property);

		final var items = property.getItems();
		Assertions.assertNotNull(property.getItems());

		if (items instanceof ComposedSchema) {
			final var itemsValue = ((ComposedSchema) items).getAnyOf();
			for (int i = 0; i < itemsValue.size(); i++) {
				final var ref = itemsValue.get(i).get$ref();
				Assertions.assertEquals(expectedResult.get(i), ref);
			}
		}
	}
	
	/**
	 * This test attempts to get the OAS representation of a ObjectIntersectionOf restriction.
	 */
	@Test
	public void testObjectIntersectionOf() throws OWLOntologyCreationException, Exception {
		// Expected value
		final var expectedResult = new ArrayList<String>();
		expectedResult.add("#/components/schemas/Assignment");
		expectedResult.add("#/components/schemas/Exam");

		// Get the class schema and make sure it has properties.
		final var schema = this.mapper.getSchemas().get("Course");
		Assertions.assertNotNull(schema);
		Assertions.assertNotNull(schema.getProperties());

		// Get the property to check object property intersectionOf.
		final var property = (Schema) schema.getProperties().get("hasEvaluationMethod");
		Assertions.assertNotNull(property);

		final var items = property.getItems();
		Assertions.assertNotNull(property.getItems());

		if (items instanceof ComposedSchema) {
			final var itemsValue = ((ComposedSchema) items).getAllOf();
			for (int i = 0; i < itemsValue.size(); i++) {
				final var ref = itemsValue.get(i).get$ref();
				Assertions.assertEquals(expectedResult.get(i), ref);
			}
		}
	}
	
	/**
	 * This test attempts to get the OAS representation of a ObjectSomeValuesFrom without a complex restriction
	 * e.g. it doesn't include a union.
	 */
	@Test
	public void testSimpleObjectSomeValuesFrom() throws OWLOntologyCreationException, Exception {
		// Expected value
		final var expectedResult = false;

		// Get the class schema and make sure it has properties.
		final var schema = this.mapper.getSchemas().get("University");
		Assertions.assertNotNull(schema);
		Assertions.assertNotNull(schema.getProperties());

		// Get the property to check object property someValuesFrom.
		final var property = (Schema) schema.getProperties().get("hasDepartment");
		Assertions.assertNotNull(property);
		Assertions.assertEquals(expectedResult, property.getNullable());
	}

	/**
	 * This test attempts to get the OAS representation of a ObjectSomeValuesFrom which includes another 
	 * restriction e.g. UnionOf.
	 */
	@Test
	public void testObjectSomeValuesFrom_ComposedByRestriction() throws OWLOntologyCreationException, Exception {
		// Expected value
		final var expectedResult = new ArrayList<String>();
		expectedResult.add("#/components/schemas/BachelorProgram");
		expectedResult.add("#/components/schemas/MasterProgram");
		expectedResult.add("#/components/schemas/PhDProgram");

		// Get the class schema and make sure it has properties.
		final var schema = this.mapper.getSchemas().get("Student");
		Assertions.assertNotNull(schema);
		Assertions.assertNotNull(schema.getProperties());

		// Get the property to check object property someValuesFrom.
		final var property = (Schema) schema.getProperties().get("enrolledIn");
		Assertions.assertNotNull(property);

		final var items = property.getItems();
		Assertions.assertNotNull(property.getItems());
		
		if (items instanceof ComposedSchema) {
			final var itemsValue = ((ComposedSchema) items).getAnyOf();

			// If the anyOf list is null, something is wrong.
			Assertions.assertNotNull(itemsValue);

			// Verify the expectedResult list size and anyOf list size are equal.
			Assertions.assertEquals(expectedResult.size(), itemsValue.size());

			// The anyOf ordering may differ than the expectedResult list.
			for (int i = 0; i < itemsValue.size(); i++) {
				expectedResult.remove(itemsValue.get(i).get$ref());
			}

			// If the expectedResult list is now empty, then both lists contained the same reference values (even if in a different order).
			Assertions.assertTrue(expectedResult.isEmpty());
		}
	}

	/**
	 * This test attempts to get the OAS representation of the exact cardinality of an ObjectProperty,
	 * when arrays are set to always be generated for properties.
	 */
	@Test
	public void testObjectExactCardinalityWithArraysGenerated() throws OWLOntologyCreationException, Exception {
		// Set up the mapper with non-default values.
		this.configData.setAlways_generate_arrays(true);
		this.setupMapper();

		// Expected value
		final var expectedValue = 1;

		// Get the class schema and make sure it has properties.
		final var schema = this.mapper.getSchemas().get("AmericanStudent");
		Assertions.assertNotNull(schema);
		Assertions.assertNotNull(schema.getProperties());

		// Get the property to check object property exact cardinality.
		final var property = (Schema) schema.getProperties().get("hasRecord");
		Assertions.assertNotNull(property);
		Assertions.assertEquals(expectedValue, property.getMinItems());
		Assertions.assertEquals(property.getMinItems(), property.getMaxItems());
	}
	
	/**
	 * This test attempts to get the OAS representation of the exact cardinality of an ObjectProperty,
	 * when properties may or may not be arrays, depending on cardinality.  Plus, list of required properties are set to be generated for schemas.
	 */
	@Test
	public void testObjectExactCardinalityWithRequiredPropertiesAndWithoutArraysGenerated() throws OWLOntologyCreationException, Exception {
		// Set up the mapper with non-default values.
		this.configData.setAlways_generate_arrays(false);
		this.configData.setRequired_properties_from_cardinality(true);
		this.setupMapper();

		// Expected value
		final var expectedMinMaxResult = 1;

		// Get the class schema and make sure it has properties.
		final var schema = this.mapper.getSchemas().get("AmericanStudent");
		Assertions.assertNotNull(schema);
		Assertions.assertNotNull(schema.getProperties());

		final var isRequired = schema.getRequired() != null && schema.getRequired().contains("hasRecord");
		Assertions.assertTrue(isRequired);

		// Get the property to check object property exact cardinality.
		// For exact cardinality, the class schema should have it marked as required when required properties are generated.
		final var property = (Schema) schema.getProperties().get("hasRecord");
		Assertions.assertNotNull(property);

		final var items = property.getItems();
		Assertions.assertNotNull(items);
		Assertions.assertNotNull(items.get$ref());
		Assertions.assertEquals(property.getMaxItems(), property.getMinItems());
		Assertions.assertEquals(expectedMinMaxResult, property.getMinItems());
	}
	
	/**
	 * This test attempts to get the OAS representation of the minimum cardinality of an ObjectProperty.
	 */
	@Test
	public void testObjectMinCardinality() throws OWLOntologyCreationException, Exception {
		// Expected value
		final var expectedResult = 2;

		// Get the class schema and make sure it has properties.
		final var schema = this.mapper.getSchemas().get("AmericanStudent");
		Assertions.assertNotNull(schema);
		Assertions.assertNotNull(schema.getProperties());

		// Get the property to check object property min cardinality.
		final var property = (Schema) schema.getProperties().get("takesCourse");
		Assertions.assertNotNull(property);

		final var items = property.getItems();
		Assertions.assertNotNull(property.getItems());
		Assertions.assertEquals("#/components/schemas/Course", items.get$ref());
		Assertions.assertEquals(expectedResult, property.getMinItems());
	}
	
	/**
	 * This test attempts to get the OAS representation of the maximum cardinality of an ObjectProperty.
	 */
	@Test
	public void testObjectMaxCardinality() throws OWLOntologyCreationException, Exception {
		// Expected value
		final var expectedResult = 20;

		// Get the class schema and make sure it has properties.
		final var schema = this.mapper.getSchemas().get("Course");
		Assertions.assertNotNull(schema);
		Assertions.assertNotNull(schema.getProperties());

		// Get the property to check object property max cardinality.
		final var property = (Schema) schema.getProperties().get("hasStudentEnrolled");
		Assertions.assertNotNull(property);

		final var items = property.getItems();
		Assertions.assertNotNull(items);
		Assertions.assertEquals("#/components/schemas/Student", items.get$ref());
		Assertions.assertEquals(expectedResult, property.getMaxItems());
	}

	/**
	 * This test attempts to get the OAS representation of the complementOf of an ObjectProperty.
	 */
	@Test
	public void testObjectComplementOf() throws OWLOntologyCreationException, Exception {
		// Expected value
		final var expectedResult = "#/components/schemas/ProfessorInArtificialIntelligence";

		// Get the class schema and make sure it has properties.
		final var schema = this.mapper.getSchemas().get("ProfessorInOtherDepartment");
		Assertions.assertNotNull(schema);
		Assertions.assertNotNull(schema.getNot());
		Assertions.assertEquals(expectedResult, schema.getNot().get$ref());
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
		// Expected value
		// The short form of: "<https://w3id.org/example/resource/Department/ArtificialIntelligenceDepartment>"
		// resolves to the default value of: "ArtificialIntelligenceDepartment".
		final var  expectedResult = "ArtificialIntelligenceDepartment";

		// Get the class schema and make sure it has properties.
		final var  schema = this.mapper.getSchemas().get("ProfessorInArtificialIntelligence");
		Assertions.assertNotNull(schema);
		Assertions.assertNotNull(schema.getProperties());

		// Get the property to check object property hasValue.
		final var  property = (Schema) schema.getProperties().get("belongsTo");
		Assertions.assertNotNull(property);

		final var items = property.getItems();
		Assertions.assertNotNull(items);
		Assertions.assertNotNull(items.getDefault());
		Assertions.assertNotNull(items.getEnum());
		Assertions.assertFalse(items.getEnum().isEmpty());
		Assertions.assertEquals(expectedResult, items.getDefault());
		Assertions.assertTrue(items.getEnum().contains(expectedResult));
	}
	
	/**
	 * This test attempts to get the OAS representation of the oneOf restriction of an ObjectProperty.
	 */
	@Test
	public void testObjectOneOf() throws OWLOntologyCreationException, Exception {
		// Expected value
		final var expectedResult = new ArrayList<String>();
		// Original full IRIs were:
		// expectedResult.add("<https://w3id.org/example/resource/Degree/MS>");
		// expectedResult.add("<https://w3id.org/example/resource/Degree/PhD>");
		// Because these are individuals which may up the (sub)set of the Degree enum, we only need their short form name now:
		expectedResult.add("MS");
		expectedResult.add("PhD");

		// Get the class schema and make sure it has properties.
		final var schema = this.mapper.getSchemas().get("Professor");
		Assertions.assertNotNull(schema);
		Assertions.assertNotNull(schema.getProperties());

		// Get the property to check object property oneOf.
		final var property = (Schema) schema.getProperties().get("hasDegree");
		Assertions.assertNotNull(property);

		final var items = property.getItems();
		Assertions.assertNotNull(items);

		final var enumValues = items.getEnum();
		Assertions.assertNotNull(enumValues);
		Assertions.assertFalse(enumValues.isEmpty());

		enumValues.forEach((enumValue) -> {
			Assertions.assertTrue(expectedResult.contains(enumValue));
			expectedResult.remove(enumValue);
		});

		// Each value found should have been removed.  Now the expected result list should be empty.
		Assertions.assertTrue(expectedResult.isEmpty());
	}

	/**
	 * This test attempts to get the OAS representation of a FunctionalDataProperty.
	 */
	@Test
	public void testFunctionalDataProperty() throws OWLOntologyCreationException, Exception {
		// Expected value
		final var expectedResult = 1;

		// Get the class schema and make sure it has properties.
		final var schema = this.mapper.getSchemas().get("AmericanStudent");
		Assertions.assertNotNull(schema);
		Assertions.assertNotNull(schema.getProperties());

		// Get the property to check data property is functional.
		final var property = (Schema) schema.getProperties().get("birthDate");
		Assertions.assertNotNull(property);
		Assertions.assertEquals(expectedResult, property.getMaxItems());
	}
	
	/**
	 * This test attempts to get the OAS representation of the UnionOf restriction of a DataProperty.
	 */
	@Test
	public void testDataUnionOf() throws OWLOntologyCreationException, Exception {
		// Expected value
		final var expectedResult = new ArrayList<String>();
		expectedResult.add("number");
		expectedResult.add("integer");

		// Get the class schema and make sure it has properties.
		final var schema = this.mapper.getSchemas().get("Course");
		Assertions.assertNotNull(schema);
		Assertions.assertNotNull(schema.getProperties());

		// Get the property to check data property unionOf.
		final var property = (Schema) schema.getProperties().get("ects");
		Assertions.assertNotNull(property);
		
		final var items = property.getItems();
		Assertions.assertNotNull(items);

		if (items instanceof ComposedSchema) {
			final var itemsValue = ((ComposedSchema) items).getAnyOf();
			if (itemsValue != null) {
				for (int i = 0; i < itemsValue.size(); i++) {
					final var ref = itemsValue.get(i).getType();
					Assertions.assertEquals(expectedResult.get(i), ref);
				}
			}
		}
	}

	/**
	 * This test attempts to get the OAS representation of the UnionOf restriction of a DataProperty.
	 */
	@Test
	public void testDataIntersectionOf() throws OWLOntologyCreationException, Exception {
		// Expected value
		final var expectedResult = new ArrayList<String>();
		expectedResult.add("integer");
		expectedResult.add("integer");

		// Get the class schema and make sure it has properties.
		final var schema = this.mapper.getSchemas().get("ProfessorInArtificialIntelligence");
		Assertions.assertNotNull(schema);
		Assertions.assertNotNull(schema.getProperties());

		// Get the property to check data property intersectionOf.
		final var property = (Schema) schema.getProperties().get("memberOfOtherDepartments");
		Assertions.assertNotNull(property);

		final var items = property.getItems();
		Assertions.assertNotNull(items);

		if (items instanceof ComposedSchema) {
			final var itemsValue = ((ComposedSchema) items).getAllOf();
			for (int i = 0; i < itemsValue.size(); i++) {
				final var ref = itemsValue.get(i).getType();
				Assertions.assertEquals(expectedResult.get(i), ref);
			}
		}
	}
	
	/**
	 * This test attempts to get the OAS representation of the SomeValuesFrom restriction of a DataProperty.
	 */
	@Test
	public void testDataSomeValuesFrom() throws OWLOntologyCreationException, Exception {
		// Expected value
		final var expectedTypeResult = "integer";
		final var expectedFormatResult = "int32";

		// Get the class schema and make sure it has properties.
		final var schema = this.mapper.getSchemas().get("ProfessorInArtificialIntelligence");
		Assertions.assertNotNull(schema);
		Assertions.assertNotNull(schema.getProperties());

		// Get the property to check data property someValuesFrom.
		// The "memberOfOtherDepartments" property of "ProfessorInArtificialIntelligence" has some values from an intersection of "xsd:nonNegativeInteger" and "xsd:nonPositiveInteger"
		// (That means it has an anyOf > allOf hierarchy)
		// Both are integers (format: int32), differing only by their "minimum" and "maximum" values.
		final var property = (Schema) schema.getProperties().get("memberOfOtherDepartments");
		Assertions.assertNotNull(property);

		final var items = property.getItems();
		Assertions.assertNotNull(items);

		// Always set nullable to false for owl:someValuesFrom
    	// @see https://owl-to-oas.readthedocs.io/en/latest/mapping/#someValuesFromExample
		Assertions.assertEquals(false, property.getNullable());

		// Because the property has an intersection within someValuesFrom restriction, it occurs under the items > anyOf > allOf hierarchy.
		Assertions.assertNotNull(items.getAnyOf());
		Assertions.assertEquals(1, items.getAnyOf().size());

		// There should be exactly one anyOf item (which is a ComposedSchema containing two allOf items).
		final var allOfComposedSchema = (Schema) items.getAnyOf().get(0);
		Assertions.assertNotNull(allOfComposedSchema);

		final var allOfSchemas = allOfComposedSchema.getAllOf();
		Assertions.assertNotNull(allOfSchemas);
		Assertions.assertNotNull(allOfSchemas.get(0));
		Assertions.assertNotNull(allOfSchemas.get(1));
		Assertions.assertEquals(expectedTypeResult, ((Schema) allOfSchemas.get(0)).getType());
		Assertions.assertEquals(expectedTypeResult, ((Schema) allOfSchemas.get(1)).getType());
		Assertions.assertEquals(expectedFormatResult, ((Schema) allOfSchemas.get(0)).getFormat());
		Assertions.assertEquals(expectedFormatResult, ((Schema) allOfSchemas.get(1)).getFormat());
	}

	/**
	 * This test attempts to get the OAS representation of a DataSomeValuesFrom which includes another restriction.
	 * e.g. IntersectionOf
	 */
	@Test
	public void testDataSomeValuesFrom_ComposedByRestriction() throws OWLOntologyCreationException, Exception {
		// Expected values
		final var expectedResult = new ArrayList<String>();
		expectedResult.add("integer");
		expectedResult.add("integer");

		// Get the class schema and make sure it has properties.
		final var schema = this.mapper.getSchemas().get("ProfessorInArtificialIntelligence");
		Assertions.assertNotNull(schema);
		Assertions.assertNotNull(schema.getProperties());

		// Get the property to check data property someValuesFrom.
		final var property = (Schema) schema.getProperties().get("memberOfOtherDepartments");
		Assertions.assertNotNull(property);

		final var items = property.getItems();
		Assertions.assertNotNull(items);

		if (items instanceof ComposedSchema) {
			final var itemsValue = ((ComposedSchema) items).getAllOf();
			for (int i = 0; i < itemsValue.size(); i++) {
				Assertions.assertEquals(expectedResult.get(i), itemsValue.get(i).getType());
			}
		}

		// Always set nullable to false for owl:someValuesFrom
    	// @see https://owl-to-oas.readthedocs.io/en/latest/mapping/#someValuesFromExample
		Assertions.assertEquals(false, property.getNullable());
	}
	
	/**
	 * This test attempts to get the OAS representation of the AllValuesFrom restriction of a DataProperty.
	 */
	@Test
	public void testDataAllValuesFrom() throws OWLOntologyCreationException, Exception {
		// Expected value
		final var expectedResult = "string";

		// Get the class schema and make sure it has properties.
		final var schema = this.mapper.getSchemas().get("StudyProgram");
		Assertions.assertNotNull(schema);
		Assertions.assertNotNull(schema.getProperties());

		// Get the property to check data property allValuesFrom.
		final var property = (Schema) schema.getProperties().get("studyProgramName");
		Assertions.assertNotNull(property);
		Assertions.assertNotNull(property.getItems());

		// Because the property has an allValuesFrom restriction, it occurs under the items>allOf hierarchy.
		Assertions.assertNotNull(property.getItems().getAllOf());
		Assertions.assertEquals(1, property.getItems().getAllOf().size());
		Assertions.assertEquals(expectedResult, ((Schema) property.getItems().getAllOf().get(0)).getType());
	}
	
	/**
	 * This test attempts to get the OAS representation of the OneOf restriction of a DataProperty.
	 */
	@Test
	public void testDataOneOf() throws OWLOntologyCreationException, Exception {
		// Expected values
		final var expectedResult = new ArrayList<String>();
		expectedResult.add("female");
		expectedResult.add("male");

		// Get the class schema and make sure it has properties.
		final var schema = this.mapper.getSchemas().get("Person");
		Assertions.assertNotNull(schema);
		Assertions.assertNotNull(schema.getProperties());

		// Get the property to check data property oneOf.
		final var property = (Schema) schema.getProperties().get("gender");
		Assertions.assertNotNull(property);

		final var items = property.getItems();
		Assertions.assertNotNull(items);
		
		if (items instanceof ComposedSchema) {
			final var itemsValue = ((ComposedSchema) items).getEnum();
			for (int i = 0; i < itemsValue.size(); i++) {
				Assertions.assertEquals(expectedResult.get(i), itemsValue.get(i));
			}
		}
	}
	
	/**
	 * This test attempts to get the OAS representation of the hasValue restriction of a DataProperty.
	 */
	@Test
	public void testDataHasValue() throws OWLOntologyCreationException, Exception {
		// Expected value
		final var expectedResult = "American";

		// Get the class schema and make sure it has properties.
		final var schema = this.mapper.getSchemas().get("AmericanStudent");
		Assertions.assertNotNull(schema);
		Assertions.assertNotNull(schema.getProperties());

		// Get the property to check data property hasValue.
		final var property = (Schema) schema.getProperties().get("nationality");
		Assertions.assertNotNull(property);
		
		final var items = property.getItems();
		Assertions.assertNotNull(items);
		Assertions.assertEquals(expectedResult, items.getDefault());
	}
	
	/**
	 * This test attempts to get the OAS representation of the exact cardinality of a DataProperty.
	 */
	@Test
	public void testDataExactCardinality() throws OWLOntologyCreationException,Exception {
		// Get the class schema and make sure it has properties.
		final var schema = this.mapper.getSchemas().get("University");
		Assertions.assertNotNull(schema);
		Assertions.assertNotNull(schema.getProperties());

		// Get the property to check data property exact cardinality.
		final var property = (Schema) schema.getProperties().get("universityName");
		Assertions.assertNotNull(property);
		Assertions.assertNotNull(property.getItems());
		Assertions.assertNotNull(property.getMaxItems());
		Assertions.assertNotNull(property.getMinItems());
		Assertions.assertEquals(property.getMaxItems(), property.getMinItems());
	}
	
	/**
	 * This test attempts to get the OAS representation of the minimum cardinality of a DataProperty.
	 */
	@Test
	public void testDataMinCardinality() throws OWLOntologyCreationException, Exception {
		// Expected value
		final var expectedResult = 1;

		// Get the class schema and make sure it has properties.
		final var schema = this.mapper.getSchemas().get("Professor");
		Assertions.assertNotNull(schema);
		Assertions.assertNotNull(schema.getProperties());

		// Get the property to check data property min cardinality.
		final var property = (Schema) schema.getProperties().get("researchField");
		Assertions.assertNotNull(property);
		Assertions.assertEquals(expectedResult, property.getMinItems());
	}
	
	/**
	 * This test attempts to get the OAS representation of the maximum cardinality of a DataProperty.
	 */
	@Test
	public void testDataMaxCardinality() throws OWLOntologyCreationException, Exception {
		// Expected value
		final var expectedResult = 2;

		// Get the class schema and make sure it has properties.
		final var schema = this.mapper.getSchemas().get("Person");
		Assertions.assertNotNull(schema);
		Assertions.assertNotNull(schema.getProperties());

		// Get the property to check data property max cardinality.
		final var property = (Schema) schema.getProperties().get("address");
		Assertions.assertNotNull(property);
		Assertions.assertEquals(expectedResult, property.getMaxItems());
	}
	
	/**
	 * This test attempts to get the OAS representation of the complementOf of a DataProperty.
	 */
	@Test
	public void testDataComplementOf() throws OWLOntologyCreationException,Exception {
		// Expected values
		final var expectedComplementTypeResult = "integer";
		final var expectedComplementMinimumResult = BigDecimal.ZERO;
		final var expectedComplementFormatResult = "int32";

		// Get the class schema and make sure it has properties.
		final var schema = this.mapper.getSchemas().get("Department");
		Assertions.assertNotNull(schema);
		Assertions.assertNotNull(schema.getProperties());

		// Get the property to check data property ComplementOf.
		final var property = (Schema) schema.getProperties().get("numberOfProfessors");
		Assertions.assertNotNull(property);

		// The "numberOfProfessors" property has range "not xsd:nonNegativeInteger" and "Department" has no additional restrictions for the property.
		// Therefore, we expect the property to *only* have a complement and its items should be null. 
		Assertions.assertNull(property.getItems());
		Assertions.assertNotNull(property.getNot());
		Assertions.assertEquals(expectedComplementTypeResult, property.getNot().getType());
		Assertions.assertEquals(expectedComplementMinimumResult, property.getNot().getMinimum());
		Assertions.assertEquals(expectedComplementFormatResult, property.getNot().getFormat());
	}
}
