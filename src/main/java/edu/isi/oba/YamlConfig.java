package edu.isi.oba;


import java.util.Map;

class YamlConfig {
  public Map<String, OntologyConfig> ontologies;

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

