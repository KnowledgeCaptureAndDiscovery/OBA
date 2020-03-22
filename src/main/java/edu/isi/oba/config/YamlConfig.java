package edu.isi.oba.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class YamlConfig {
  String DEFAULT_OUTPUT_DIRECTORY = "outputs";
  String DEFAULT_PROJECT_NAME = "default_project";
  public OpenAPI openapi;
  public String output_dir = DEFAULT_OUTPUT_DIRECTORY;
  public String name = DEFAULT_PROJECT_NAME;
  public List<String> paths;
  public Boolean enable_get_paths = true;
  public Boolean enable_post_paths = false;
  public Boolean enable_put_paths = false;
  public Boolean enable_delete_paths = false;
  public List<String> ontologies;
  public EndpointConfig endpoint;
  public FirebaseConfig firebase;
  public Map<String, List<RelationConfig>> relations;
  private LinkedHashMap<String, PathItem> custom_paths = null;
  public List<String> classes;
  public Boolean follow_references = false;

  public Boolean getEnable_get_paths() {
    return enable_get_paths;
  }

  public void setEnable_get_paths(Boolean enable_get_paths) {
    this.enable_get_paths = enable_get_paths;
  }

  public Boolean getEnable_post_paths() {
    return enable_post_paths;
  }

  public void setEnable_post_paths(Boolean enable_post_paths) {
    this.enable_post_paths = enable_post_paths;
  }

  public Boolean getEnable_put_paths() {
    return enable_put_paths;
  }

  public void setEnable_put_paths(Boolean enable_put_paths) {
    this.enable_put_paths = enable_put_paths;
  }

  public Boolean getEnable_delete_paths() {
    return enable_delete_paths;
  }

  public void setEnable_delete_paths(Boolean enable_delete_paths) {
    this.enable_delete_paths = enable_delete_paths;
  }

  public String custom_queries_directory = "custom_queries";

  public String getCustom_queries_directory() {
    return custom_queries_directory;
  }

  public void setCustom_queries_directory(String custom_queries_directory) {
    this.custom_queries_directory = custom_queries_directory;
  }

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

  public List<String> getPaths() {
    return paths;
  }

  public void setPaths(List<String> paths) {
    this.paths = paths;
  }

  public List<String>  getOntologies() {
    return ontologies;
  }

  public void setOntologies(List<String> ontologies) {
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

  public OpenAPI getOpenapi() {
    return openapi;
  }

  public void setOpenapi(OpenAPI openapi) {
    this.openapi = openapi;
  }

    public List<String> getClasses() {
      return this.classes;
    }

  public void setClasses(List<String> classes) {
    this.classes = classes;
  }

  public Boolean getFollow_references() {
    return follow_references;
  }

  public void setFollow_references(Boolean follow_references) {
    this.follow_references = follow_references;
  }
}

