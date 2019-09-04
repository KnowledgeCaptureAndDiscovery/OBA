package edu.isi.oba;

import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Schema;
import org.semanticweb.owlapi.model.IRI;

import java.util.HashMap;
import java.util.Map;

public class Oba {
        public static void main(String[] args) throws Exception {
            String ont_url = "https://knowledgecaptureanddiscovery.github.io/SoftwareDescriptionOntology/release/1.1.0/ontology.xml";
            String ont_prefix = "sd";
            Map<String, String> prefixes = new HashMap<>();
            prefixes.put(ont_prefix, "https://w3id.org/okn/o/sd#");
            Mapper obaMapper = new Mapper(ont_url, ont_prefix, prefixes);

            Map<String, Schema> schemas = obaMapper.schemas;
            Paths paths = obaMapper.paths;
            Serializer serializer = new Serializer(schemas, paths);
        }
}

