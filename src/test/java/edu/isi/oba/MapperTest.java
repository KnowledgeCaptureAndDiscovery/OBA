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
        String example_dbpedia = "src/test/config/dbpedia.yaml";
        String config_test_file_path = example_dbpedia;
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
}
