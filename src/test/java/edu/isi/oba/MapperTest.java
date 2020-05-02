package edu.isi.oba;

import edu.isi.oba.config.YamlConfig;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLClass;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static edu.isi.oba.ObaUtils.get_yaml_data;

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
        Assert.assertEquals(config, filter_classes);

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
        Assert.assertEquals(false, mapper.ontologies.isEmpty());
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
        Assert.assertEquals(false, mapper.ontologies.isEmpty());
        
    }
}
