package edu.isi.oba;

import edu.isi.oba.config.AuthConfig;
import static edu.isi.oba.ObaUtils.get_yaml_data;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.semanticweb.owlapi.model.OWLOntologyCreationException;

public class MapperTest {
    private Mapper setupMapper(String configFilePath, String destinationDirectory) throws Exception {
        Mapper mapper = null;

        try {
            final var configData = ObaUtils.get_yaml_data(configFilePath);
            configData.setAuth(new AuthConfig());
            mapper = new Mapper(configData);
            // Use temporary directory for unit testing
            mapper.createSchemas(destinationDirectory);

            // If no schemas are returned from the mapper, something is wrong.  Probably with the ontology(?).
            final var schemas = mapper.getSchemas();
            Assertions.assertNotNull(schemas);
        } catch (OWLOntologyCreationException e) {
            Assertions.fail("Error in ontology creation: ", e);
        }

        return mapper;
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
     * This test loads a local ontology and verifies the generated OpenAPI spec schema only
     * generates models/endpoints for classes allowed by the configuration file.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void testFilteredClasses() throws Exception {
        this.initializeLogger();

        // Expected values
        final var expectedResult1 = "Genre";
        final var expectedResult2 = "Band";
        final var expectedResultSize = 2;

        // The YAML config restricts allowed classes to:
        // - http://dbpedia.org/ontology/Genre
        // - http://dbpedia.org/ontology/Band
        final var configFilePath = "src/test/config/dbpedia.yaml";
        final var mapperTestTempDirectory = "src/test/config/dbpedia_MapperTest/";
        final var mapper = this.setupMapper(configFilePath, mapperTestTempDirectory);

        // The Genre and Band model schemas must exist.
        final var keys = mapper.getSchemas().keySet();
        Assertions.assertTrue(keys.contains(expectedResult1));
        Assertions.assertTrue(keys.contains(expectedResult2));
        Assertions.assertEquals(expectedResultSize, keys.size());

        // Delete temporary directory now
        Files.walk(Paths.get(mapperTestTempDirectory))
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
    }
    
    /**
     * This test attempts to load a local ontology.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void testLocalFile() throws Exception {
        final var local_ontology = "src/test/config/mcat_reduced.yaml";
        final var config_data = get_yaml_data(local_ontology);
        final var mapper = new Mapper(config_data);
        Assertions.assertEquals(false, mapper.getOntologies().isEmpty());
    }

    /**
     * This test attempts to load a config in a folder with spaces.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void testSpacesInPath() throws Exception {
        final var local_ontology = "examples/example with spaces/config.yaml";
        final var config_data = get_yaml_data(local_ontology);
        final var mapper = new Mapper(config_data);
        Assertions.assertEquals(false, mapper.getOntologies().isEmpty());
    }
    
    /**
     * This test attempts to run OBA with an online ontology through a URI.
     * The ontology is hosted in GitHub, but there is a small risk of the test
     * not passing due to the unavailability of the ontology.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void testRemoteOntology() throws Exception {
        final var example_remote = "src/test/config/pplan.yaml";
        final var config_data = get_yaml_data(example_remote);
        final var mapper = new Mapper(config_data);
        Assertions.assertEquals(false, mapper.getOntologies().isEmpty());
    }

    /**
     * Test an ontology (very simple, two classes) with a missing import
     */
    @Test
    public void testMissingImportOntology() throws Exception {
        this.initializeLogger();

        final var example_remote = "src/test/resources/missing_import/config.yaml";
        final var config_data = get_yaml_data(example_remote);
        final var mapper = new Mapper(config_data);
        Assertions.assertEquals(false, mapper.getOntologies().isEmpty());
    }

    /**
     * Test an ontology (very simple, two classes) with a missing import
     */
    @Test
    public void testComplexOntology() throws Exception {
        this.initializeLogger();

        final var configFilePath = "src/test/resources/complex_expr/config.yaml";
        final var mapperTestTempDirectory = "src/test/resources/complex_expr/MapperTest/";
        final var mapper = this.setupMapper(configFilePath, mapperTestTempDirectory);

        // The person model schema must exist.
        final var keys = mapper.getSchemas().keySet();
        Assertions.assertTrue(keys.contains("Person"));

        // Delete temporary directory now
        Files.walk(Paths.get(mapperTestTempDirectory))
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
    }
}
