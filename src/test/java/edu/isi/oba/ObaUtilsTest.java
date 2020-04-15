package edu.isi.oba;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class ObaUtilsTest {

    @Test
    public void read_json_file() throws IOException {
        JSONObject actual = ObaUtils.read_json_file("json_one.json");
        Assert.assertEquals(actual.get("JSON1"), "Hello, World");
    }

    @Test
    public void mergeJSONObjects() throws IOException {
        JSONObject one = ObaUtils.read_json_file("json_one.json");
        JSONObject two = ObaUtils.read_json_file("json_two.json");
        JSONObject merge = ObaUtils.mergeJSONObjects(one, two);
        Assert.assertEquals(merge.get("JSON1"), "Hello, World");
        Assert.assertEquals(merge.get("JSON2"), "Bye, World");
    }

    @Test
    public void mergeJSONObjectsMultiple() throws IOException {
        JSONObject one = ObaUtils.read_json_file("json_one.json");
        JSONObject two = ObaUtils.read_json_file("json_two.json");
        JSONObject three = ObaUtils.read_json_file("json_three.json");
        JSONObject[] jsons = new JSONObject[]{ one, two, three};
        JSONObject merge = ObaUtils.concat_json(jsons);
        Assert.assertEquals(merge.get("JSON1"), "Hello, World");
        Assert.assertEquals(merge.get("JSON2"), "Bye, World");
        Assert.assertEquals(merge.get("JSON3"), "Hi again, World");
    }

    @Test
    public void run() {
        String ontology1 = "https://mintproject.github.io/Mint-ModelCatalog-Ontology/release/1.4.0/ontology.xml";
        String ontology2 = "https://knowledgecaptureanddiscovery.github.io/SoftwareDescriptionOntology/release/1.5.0/ontology.xml";
        String[] ontologies = new String[]{ontology1, ontology2};
        JSONObject context = null;
        try {
            context = ObaUtils.generate_context_file(ontologies);
        } catch (Exception e) {
            e.printStackTrace();
        }
        JSONObject o = (JSONObject) context.get("@context");
        assertEquals(o.get("id"), "@id");
        assertEquals(o.get("type"), "@type");
    }
}