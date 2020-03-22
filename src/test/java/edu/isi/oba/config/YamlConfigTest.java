package edu.isi.oba.config;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static edu.isi.oba.ObaUtils.get_yaml_data;
import static org.junit.Assert.assertThat;

public class YamlConfigTest {
    @Test
    public void getSelectedClasses(){
        String config_test_file_path = "examples/dbpedia/config.yaml";
        YamlConfig config_data = get_yaml_data(config_test_file_path);
        List<String> expected = Arrays.asList("http://dbpedia.org/ontology/Genre", "http://dbpedia.org/ontology/Author");
        List<String> config = config_data.getClasses();
        Assert.assertEquals(expected, config);
    }
}
