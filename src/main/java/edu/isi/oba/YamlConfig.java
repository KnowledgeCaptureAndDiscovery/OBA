package edu.isi.oba;


import java.util.List;
import java.util.Map;

class YamlConfig {
  public Map<String, OntologyConfig> ontologies;

  public List<String> getPaths() {
    return paths;
  }

  public void setPaths(List<String> paths) {
    this.paths = paths;
  }

  public List<String> paths;
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

