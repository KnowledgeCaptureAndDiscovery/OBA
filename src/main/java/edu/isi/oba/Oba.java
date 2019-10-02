package edu.isi.oba;

import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Oba {

  public static final String CONFIG_YAML = "config.yaml";

  public static void main(String[] args) throws Exception {
          Constructor constructor = new Constructor(YamlConfig.class);
          Yaml yaml = new Yaml( constructor );

          InputStream input = null;
          try {
            input = new FileInputStream(new File(CONFIG_YAML));
          } catch (FileNotFoundException e) {
            System.err.println("Configuration file not found: " + CONFIG_YAML);
          }
          YamlConfig data = yaml.loadAs( input, YamlConfig.class );

          Map<String, OntologyConfig> ontologies = data.getOntologies();
          List<Mapper> mappers = new ArrayList<>();
          List<String> paths = data.getPaths();

          for (Map.Entry<String, OntologyConfig> entry : ontologies.entrySet()) {
            OntologyConfig ontology = entry.getValue();
            Mapper mapper = extract_info(ontology.getXmlUrl(), ontology.getPrefix(), ontology.getPrefixUri(), paths);
            mappers.add(mapper);
          }
          Serializer serializer = new Serializer(mappers);
        }


        public static Mapper extract_info(String ont_serialization_url, String ont_prefix, String ont_uri, List<String> paths) throws OWLOntologyCreationException, IOException {
          Map<String, String> prefixes = new HashMap<>();
          prefixes.put(ont_prefix, ont_uri);
          return new Mapper(ont_serialization_url, ont_prefix, prefixes, paths);
        }
}
