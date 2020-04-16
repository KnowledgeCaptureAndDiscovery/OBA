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
        Assert.assertNotNull(actual.get("@context"));
    }

    @Test
    public void mergeJSONObjects() throws IOException {
        JSONObject one = ObaUtils.read_json_file("json_one.json");
        JSONObject two = ObaUtils.read_json_file("json_two.json");
        JSONObject merge = ObaUtils.mergeJSONObjects(one, two);
        Assert.assertNotNull(merge.get("@context"));
        Assert.assertNotNull(merge.get("@context"));
    }

    @Test
    public void concat_json_common_key() throws IOException {
        JSONObject one = ObaUtils.read_json_file("json_one.json");
        JSONObject two = ObaUtils.read_json_file("json_two.json");
        JSONObject three = ObaUtils.read_json_file("json_three.json");
        JSONObject[] jsons = new JSONObject[]{ one, two, three};
        JSONObject merge = ObaUtils.concat_json_common_key(jsons, "@context");
        JSONObject o = (JSONObject) merge.get("@context");
        assertNotNull(o.get("Entity"));
        assertNotNull(o.get("Model"));
        assertNotNull(o.get("Setup"));
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
        assertNotNull(o.get("Entity"));
        assertNotNull(o.get("Model"));
    }

}