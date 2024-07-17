package edu.isi.oba.config;

import static edu.isi.oba.ObaUtils.get_yaml_data;

import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class YamlConfigTest {
    @Test
    public void getSelectedClasses(){
        String config_test_file_path = "examples/dbpedia/config_music.yaml";
        YamlConfig config_data = get_yaml_data(config_test_file_path);
        Set<String> expected = Set.of("http://dbpedia.org/ontology/Genre", "http://dbpedia.org/ontology/Band");
        Set<String> config = config_data.getClasses();
        Assertions.assertEquals(expected, config);
    }
}
