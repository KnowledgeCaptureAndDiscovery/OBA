package edu.isi.oba;


import java.util.List;
import java.util.Map;

class YamlConfig {
  public Map<String, OntologyConfig> ontologies;
  public Map<String, List<RelationConfig>> relations;
  public String output_dir = "outputs";
  public String openapi_base;
  public String name = "server";
  public List<String> paths;

  public String getOutput_dir() {
    return output_dir;
  }

  public void setOutput_dir(String output_dir) {
    this.output_dir = output_dir;
  }


  public String getOpenapi_base() {
    return openapi_base;
  }

  public void setOpenapi_base(String openapi_base) {
    this.openapi_base = openapi_base;
  }

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

  public Map<String, List<RelationConfig>> getRelations() {
    return relations;
  }

  public void setRelations(Map<String, List<RelationConfig>> relations) {
    this.relations = relations;
  }

}

