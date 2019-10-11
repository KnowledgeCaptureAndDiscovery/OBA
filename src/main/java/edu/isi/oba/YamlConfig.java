package edu.isi.oba;


import java.util.List;
import java.util.Map;

class YamlConfig {
  public Map<String, OntologyConfig> ontologies;
  public List<String> paths;
  public String name = "server";

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
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

