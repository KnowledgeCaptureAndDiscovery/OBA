package edu.isi.oba.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class YamlConfig {
  String DEFAULT_OUTPUT_DIRECTORY = "outputs";
  String DEFAULT_PROJECT_NAME = "default_project";

  public OpenAPI getOpenapi() {
    return openapi;
  }

  public void setOpenapi(OpenAPI openapi) {
    this.openapi = openapi;
  }

  public OpenAPI openapi;

  public String output_dir = DEFAULT_OUTPUT_DIRECTORY;
  public String name = DEFAULT_PROJECT_NAME;
  public String openapi_base;
  public List<String> paths;
  public Map<String, OntologyConfig> ontologies;
  public EndpointConfig endpoint;
  public FirebaseConfig firebase;
  public Map<String, List<RelationConfig>> relations;
  private LinkedHashMap<String, PathItem> custom_paths = null;


  public String getOutput_dir() {
    return output_dir;
  }

  public void setOutput_dir(String output_dir) {
    this.output_dir = output_dir;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getOpenapi_base() {
    return openapi_base;
  }

  public void setOpenapi_base(String openapi_base) {
    this.openapi_base = openapi_base;
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

  public EndpointConfig getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(EndpointConfig endpoint) {
    this.endpoint = endpoint;
  }

  public FirebaseConfig getFirebase() {
    return firebase;
  }

  public void setFirebase(FirebaseConfig firebase) {
    this.firebase = firebase;
  }

  public Map<String, List<RelationConfig>> getRelations() {
    return relations;
  }

  public void setRelations(Map<String, List<RelationConfig>> relations) {
    this.relations = relations;
  }


  public LinkedHashMap<String, PathItem> getCustom_paths() {
    return custom_paths;
  }

  public void setCustom_paths(LinkedHashMap<String, PathItem> custom_paths) {
    this.custom_paths = custom_paths;
  }
}

