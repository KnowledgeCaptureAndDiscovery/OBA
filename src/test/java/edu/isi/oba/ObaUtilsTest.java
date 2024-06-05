package edu.isi.oba;

import static edu.isi.oba.ObaUtils.get_yaml_data;

import java.io.File;
import java.io.IOException;

import org.json.JSONObject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

public class ObaUtilsTest {

    @Test
    public void read_json_file() throws IOException {
        final var actual = ObaUtils.read_json_file("json_one.json");
        Assertions.assertNotNull(actual.get("@context"));
    }

    @Test
    public void mergeJSONObjects() throws IOException {
        final var one = ObaUtils.read_json_file("json_one.json");
        final var two = ObaUtils.read_json_file("json_two.json");
        final var merge = ObaUtils.mergeJSONObjects(one, two);
        Assertions.assertNotNull(merge.get("@context"));
        Assertions.assertNotNull(merge.get("@context"));
    }

    @Test
    public void concat_json_common_key() throws IOException {
        final var one = ObaUtils.read_json_file("json_one.json");
        final var two = ObaUtils.read_json_file("json_two.json");
        final var three = ObaUtils.read_json_file("json_three.json");
        final var jsons = new JSONObject[]{ one, two, three};
        final var merge = ObaUtils.concat_json_common_key(jsons, "@context");
        final var o = (JSONObject) merge.get("@context");
        Assertions.assertNotNull(o.get("Entity"));
        Assertions.assertNotNull(o.get("Model"));
        Assertions.assertNotNull(o.get("Setup"));
    }
    
    @Test
    public void getDescription () throws OWLOntologyCreationException{
        final var example_remote = "src/test/config/pplan.yaml";
        final var config_data = get_yaml_data(example_remote);
        try {
            final var mapper = new Mapper(config_data);
            OWLClass planClass = mapper.getManager().getOWLDataFactory().getOWLClass("http://purl.org/net/p-plan#Plan");
            String desc = ObaUtils.getDescription(planClass, mapper.getOntologies(), true);
            Assertions.assertNotEquals(desc, "");
        }catch(Exception e){
            Assertions.fail("Failed to get description.", e);
        }
    }

    /**
     * This test will try to load a file that does not exits. The exception is captured and reported.
     * This test will pass IF you see an error on the output terminal
     * @throws OWLOntologyCreationException
     */
    @Test
    public void missing_file () throws OWLOntologyCreationException{
        final var missing_file = "src/test/config/missing_file.yaml";
        final var config_data = get_yaml_data(missing_file);
        try {
            final var mapper = new Mapper(config_data);
            Assertions.fail("Missing file: If no exception is launched, fail test");
        }catch(Exception e){
            //pass test if there is an exception
        }
    }

    @Test
    public void run() {
        final var ontology1 = "https://mintproject.github.io/Mint-ModelCatalog-Ontology/release/1.8.0/ontology.owl";
        final var ontology2 = "https://knowledgecaptureanddiscovery.github.io/SoftwareDescriptionOntology/release/1.9.0/ontology.owl";
        final var ont1 = new File("ontology1");
        final var ont2 = new File("ontology2");
        ObaUtils.downloadOntology(ontology1, ont1.getPath());
        ObaUtils.downloadOntology(ontology2, ont2.getPath());
        final var ontologies = new String[]{"ontology1", "ontology2"};
        JSONObject context = null;
        try {
            context = ObaUtils.generate_context_file(ontologies, false);
        } catch (Exception e) {
            e.printStackTrace();
        }

        final var o = (JSONObject) context.get("@context");
        Assertions.assertEquals(o.get("id"), "@id");
        Assertions.assertEquals(o.get("type"), "@type");
        Assertions.assertNotNull(o.get("Entity"));
        Assertions.assertNotNull(o.get("Model"));
        try{
            java.nio.file.Files.delete(ont1.toPath());
            java.nio.file.Files.delete(ont2.toPath());
        }catch(Exception e){
        }
    }

    /**
     * This test will try to load a file that does not exits. The exception is captured and reported.
     * This test will pass IF you see an error on the output terminal
     * @throws OWLOntologyCreationException
     */
    @Test
    public void testKebabCaseConversion() {
        // Test #1
        var expectedStr = "this-is-a-string";
        var originalStr = "thisIsAString";
        var convertedStr = ObaUtils.pascalCaseToKebabCase(originalStr);

        Assertions.assertEquals(expectedStr, convertedStr);

        // Test #2
        expectedStr = "this-happy-string";
        originalStr = "thisHAPPYString";
        convertedStr = ObaUtils.pascalCaseToKebabCase(originalStr);

        Assertions.assertEquals(expectedStr, convertedStr);

        // Test #3
        expectedStr = "this-phd-string";
        originalStr = "thisPhDString";
        convertedStr = ObaUtils.pascalCaseToKebabCase(originalStr);

        Assertions.assertEquals(expectedStr, convertedStr);
    }
}