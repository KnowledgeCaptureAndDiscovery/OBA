package edu.isi.oba;

import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Schema;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

class Oba {
        public static void main(String[] args) throws Exception {
          extract_info(
                  "https://mintproject.github.io/Mint-ModelCatalog-Ontology/release/1.0.0/ontology.xml",
                  "sdm",
                  "https://w3id.org/okn/o/sdm#");
          /*extract_info(
                  "https://knowledgecaptureanddiscovery.github.io/SoftwareDescriptionOntology/release/1.1.0/ontology.xml",
                  "sd",
                  "https://w3id.org/okn/o/sd#");*/
        }


        public static void extract_info(String ont_serialization_url, String ont_prefix, String ont_uri) throws OWLOntologyCreationException, IOException {
          Map<String, String> prefixes = new HashMap<>();
          prefixes.put(ont_prefix, ont_uri);
          Mapper obaMapper = new Mapper(ont_serialization_url, ont_prefix, prefixes);

          Map<String, Schema> schemas = obaMapper.schemas;
          Paths paths = obaMapper.paths;
          Serializer serializer = new Serializer(schemas, paths);
        }
}

