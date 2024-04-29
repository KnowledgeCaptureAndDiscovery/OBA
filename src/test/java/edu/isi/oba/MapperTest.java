package edu.isi.oba;

import static edu.isi.oba.ObaUtils.get_yaml_data;
import edu.isi.oba.config.AuthConfig;
import edu.isi.oba.config.YamlConfig;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import io.swagger.v3.oas.models.media.Schema;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.semanticweb.owlapi.model.OWLClass;

public class MapperTest {
    @Test
    public void testFilter() throws Exception{
        String config_test_file_path = "src/test/config/dbpedia.yaml";
        YamlConfig config_data = get_yaml_data(config_test_file_path);
        Mapper mapper = new Mapper(config_data);
        List<String> config = config_data.getClasses();
        List<OWLClass> classes = mapper.filter_classes();
        List<String> filter_classes = new ArrayList();
        for (OWLClass _class : classes){
            filter_classes.add(_class.getIRI().getIRIString());
        }
        Collections.sort(filter_classes);
        Collections.sort(config);
        Assertions.assertEquals(config, filter_classes);

    }
    
    /**
     * This test attempts to load a local ontology.
     * @throws java.lang.Exception
     */
    @Test
    public void testLocalFile() throws Exception{
        String local_ontology = "src/test/config/mcat_reduced.yaml";
        YamlConfig config_data = get_yaml_data(local_ontology);
        Mapper mapper = new Mapper(config_data);
        Assertions.assertEquals(false, mapper.ontologies.isEmpty());
    }

    /**
     * This test attempts to load a config in a folder with spaces.
     * @throws java.lang.Exception
     */
    @Test
    public void testSpacesInPath() throws Exception{
        String local_ontology = "examples/example with spaces/config.yaml";
        YamlConfig config_data = get_yaml_data(local_ontology);
        Mapper mapper = new Mapper(config_data);
        Assertions.assertEquals(false, mapper.ontologies.isEmpty());
    }
    
    /**
     * This test attempts to run OBA with an online ontology through a URI.
     * The ontology is hosted in GitHub, but there is a small risk of the test
     * not passing due to the unavailability of the ontology.
     * @throws java.lang.Exception
     */
    @Test
    public void testRemoteOntology() throws Exception{
        String example_remote = "src/test/config/pplan.yaml";
        YamlConfig config_data = get_yaml_data(example_remote);
        Mapper mapper = new Mapper(config_data);
        Assertions.assertEquals(false, mapper.ontologies.isEmpty());
        
    }

    /**
     * Test an ontology (very simple, two classes) with a missing import
     */
    @Test
    public void testMissingImportOntology() throws Exception{
        String example_remote = "src/test/resources/missing_import/config.yaml";
        YamlConfig config_data = get_yaml_data(example_remote);
        Mapper mapper = new Mapper(config_data);
        Assertions.assertEquals(false, mapper.ontologies.isEmpty());
    }

    /**
     * Test an ontology (very simple, two classes) with a missing import
     */
    @Test
    public void testComplexOntology() throws Exception{
        InputStream stream = Oba.class.getClassLoader().getResourceAsStream("logging.properties");
        try {
            LogManager.getLogManager().readConfiguration(stream);
            edu.isi.oba.Oba.logger = Logger.getLogger(Oba.class.getName());

        } catch (IOException e) {
            e.printStackTrace();
        }
        edu.isi.oba.Oba.logger.setLevel(Level.FINE);
        edu.isi.oba.Oba.logger.addHandler(new ConsoleHandler());
        String example_remote = "src/test/resources/complex_expr/config.yaml";
        YamlConfig config_data = get_yaml_data(example_remote);
        String destination_dir = config_data.getOutput_dir() + File.separator + config_data.getName();
        config_data.setAuth(new AuthConfig());
        Mapper mapper = new Mapper(config_data);
        OWLClass cls = mapper.manager.getOWLDataFactory().getOWLClass("https://businessontology.com/ontology/Person");
        String desc = ObaUtils.getDescription(cls, mapper.ontologies.get(0), true);
        MapperSchema mapperSchema = new MapperSchema(mapper.ontologies, cls, desc, mapper.schemaNames, mapper.ontologies.get(0), true, true, true);
        Schema schema = mapperSchema.getSchema();
        // The person schema must not be null.
        Assertions.assertNotNull(schema);
        Assertions.assertEquals(schema.getName(),"Person");
    }
}
