package edu.isi.oba;

import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Schema;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Config {
  public Config(List<Ontology> ontologies) {
    this.ontologies = ontologies;
  }

  public List<Ontology> getOntologies() {
    return ontologies;
  }

  public void setOntologies(List<Ontology> ontologies) {
    this.ontologies = ontologies;
  }

  private List<Ontology> ontologies;
}


class Ontology {
  private String xml_url;

  public void setXml_url(String xml_url) {
    this.xml_url = xml_url;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  public void setPrefix_uri(String prefix_uri) {
    this.prefix_uri = prefix_uri;
  }

  public Ontology(String xml_url) {
    this.xml_url = xml_url;
  }

  private String prefix;
  private String prefix_uri;

}

class Oba {
        public static void main(String[] args) throws Exception {
//          Yaml yaml = new Yaml(new Constructor(Config.class));
//          InputStream inputStream = new FileInputStream("config.yaml");
//          Config obj = yaml.load(inputStream);
//          System.out.println(obj);

          Mapper sdm = extract_info(
                  "https://mintproject.github.io/Mint-ModelCatalog-Ontology/release/1.0.0/ontology.xml",
                  "sdm",
                  "https://w3id.org/okn/o/sdm#");
          Mapper sd = extract_info(
                  "https://knowledgecaptureanddiscovery.github.io/SoftwareDescriptionOntology/release/1.1.0/ontology.xml",
                  "sd",
                  "https://w3id.org/okn/o/sd#");

          List<Mapper> mappers = new ArrayList<>();
          mappers.add(sdm);
          mappers.add(sd);

          Serializer serializer = new Serializer(mappers);
        }


        public static Mapper extract_info(String ont_serialization_url, String ont_prefix, String ont_uri) throws OWLOntologyCreationException, IOException {
          Map<String, String> prefixes = new HashMap<>();
          prefixes.put(ont_prefix, ont_uri);
          return new Mapper(ont_serialization_url, ont_prefix, prefixes);
        }
}
