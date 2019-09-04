package edu.isi.oba;

import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Schema;

import java.util.Map;

class Oba {
        public static void main(String[] args) throws Exception {
            String ont_url = "https://knowledgecaptureanddiscovery.github.io/SoftwareDescriptionOntology/release/1.1.0/ontology.xml";
            String ont_prefix = "sd";
            Mapper obaMapper = new Mapper(ont_url, ont_prefix);

            Map<String, Schema> schemas = obaMapper.schemas;
            Paths paths = obaMapper.paths;
            Serializer serializer = new Serializer(schemas, paths);
        }
}

