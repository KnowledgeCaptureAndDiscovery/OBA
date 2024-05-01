package edu.isi.oba.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class YamlConfig {
  private final Map<CONFIG_FLAG, Boolean> configFlags = new HashMap<>(){{
    put(CONFIG_FLAG.ALWAYS_GENERATE_ARRAYS, true);
    put(CONFIG_FLAG.DEFAULT_DESCRIPTIONS, true);
    put(CONFIG_FLAG.DEFAULT_PROPERTIES, true);
    put(CONFIG_FLAG.FOLLOW_REFERENCES, true);
    put(CONFIG_FLAG.PATH_DELETE, false);
    put(CONFIG_FLAG.PATH_GET, true);
    put(CONFIG_FLAG.PATH_PATCH, false);
    put(CONFIG_FLAG.PATH_POST, false);
    put(CONFIG_FLAG.PATH_PUT, false);
  }};

  String DEFAULT_OUTPUT_DIRECTORY = "outputs";
  String DEFAULT_PROJECT_NAME = "default_project";
  public OpenAPI openapi;
  public String output_dir = DEFAULT_OUTPUT_DIRECTORY;
  public String name = DEFAULT_PROJECT_NAME;
  public List<String> paths;
  public List<String> ontologies;
  private EndpointConfig endpoint;
  public AuthConfig auth;
  public FirebaseConfig firebase;
  public Map<String, List<RelationConfig>> relations;
  private LinkedHashMap<String, PathItem> custom_paths = null;
  public List<String> classes;
  public String custom_queries_directory;

  public Boolean getEnable_get_paths() {
    return this.configFlags.get(CONFIG_FLAG.PATH_GET);
  }

  public void setEnable_get_paths(Boolean enable_get_paths) {
    this.configFlags.put(CONFIG_FLAG.PATH_GET, enable_get_paths);
  }

  public Boolean getEnable_post_paths() {
    return this.configFlags.get(CONFIG_FLAG.PATH_POST);
  }

  public void setEnable_post_paths(Boolean enable_post_paths) {
    this.configFlags.put(CONFIG_FLAG.PATH_POST, enable_post_paths);
  }

  public Boolean getEnable_put_paths() {
    return this.configFlags.get(CONFIG_FLAG.PATH_PUT);
  }

  public void setEnable_put_paths(Boolean enable_put_paths) {
    this.configFlags.put(CONFIG_FLAG.PATH_PUT, enable_put_paths);
  }

  public Boolean getEnable_delete_paths() {
    return this.configFlags.get(CONFIG_FLAG.PATH_DELETE);
  }

  public void setEnable_delete_paths(Boolean enable_delete_paths) {
    this.configFlags.put(CONFIG_FLAG.PATH_DELETE, enable_delete_paths);
  }

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

  public Boolean getAlways_generate_arrays() {
    return this.configFlags.get(CONFIG_FLAG.ALWAYS_GENERATE_ARRAYS);
  }

  public void setAlways_generate_arrays(Boolean always_generate_arrays) {
    this.configFlags.put(CONFIG_FLAG.ALWAYS_GENERATE_ARRAYS, always_generate_arrays);
  }

  public Boolean getFollow_references() {
    return this.configFlags.get(CONFIG_FLAG.FOLLOW_REFERENCES);
  }

  public void setFollow_references(Boolean follow_references) {
    this.configFlags.put(CONFIG_FLAG.FOLLOW_REFERENCES, follow_references);
  }

  public Boolean getDefault_descriptions() {
    return this.configFlags.get(CONFIG_FLAG.DEFAULT_DESCRIPTIONS);
  }

  public void setDefault_descriptions(Boolean default_descriptions) {
    this.configFlags.put(CONFIG_FLAG.DEFAULT_DESCRIPTIONS, default_descriptions);
  }

  public Boolean getDefault_properties() {
    return this.configFlags.get(CONFIG_FLAG.DEFAULT_PROPERTIES);
  }

  public void setDefault_properties(Boolean default_properties) {
    this.configFlags.put(CONFIG_FLAG.DEFAULT_PROPERTIES, default_properties);
  }

  public AuthConfig getAuth() {
    return auth;
  }

  public void setAuth(AuthConfig auth) {
    this.auth = auth;
  }

  /**
   * Get the value of a particular configuration flag.
   * 
   * @param {flag} the configuration flag name
   * @return The flag's value (true/false/null).
   */
  public Boolean getConfigFlagValue(CONFIG_FLAG flag) {
    return this.configFlags.get(flag);
  }

  /**
   * Get map of all config flags and their values.
   * 
   * @return Map of CONFIG_FLAGs and their Boolean value (true/false/null).
   */
  public Map<CONFIG_FLAG, Boolean> getConfigFlags() {
    return this.configFlags;
  }
}