package edu.isi.oba;


import java.util.List;
import java.util.Map;

class YamlConfig {
  public Map<String, OntologyConfig> ontologies;
  public List<String> paths;
  public String filename = "openapi.yaml";

  public String getFilename() {
    return filename;
  }

  public void setFilename(String filename) {
    this.filename = filename;
  }

  public List<String> getPaths() {
    return paths;
  }

  public void setPaths(List<String> paths) {
    this.paths = paths;
  }

  public Map<String, OntologyConfig> getOntologies() {
    return ontologies;
  }

  public void setOntologies(Map<String, OntologyConfig> ontologies) {
    this.ontologies = ontologies;
  }

  @Override
  public String toString() {
    return "YamlConfig{" +
            "ontologies=" + ontologies +
            '}';
  }
}

