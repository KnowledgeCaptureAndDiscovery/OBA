package edu.isi.oba;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * $NAME/get/all/query.sparql
 * $NAME/get/all/query-graph.sparql
 *
 * $NAME/get/one/query.sparql
 * $NAME/get/one/query-graph.sparql
 *
 * $NAME/get/related/query.sparql
 * $NAME/get/related/query-graph.sparql

 * $NAME/post/query.sparql
 * $NAME/post/query-graph.sparql
 *
 * $NAME/delete/query.sparql
 * $NAME/delete/query-graph.sparql
 */
class Query {
  private static String get_all_query_file = "query.sparql";
  private static String get_all_graph_query_file = "query-graph.sparql";
  private static String get_one_query_file = "query.sparql";
  private static String get_one_graph_query_file = "query-graph.sparql";
  private static String get_related_query_file = "query.sparql";
  private static String get_related_graph_query_file = "query-graph.sparql";

  private static String post_query_file = "query.sparql";
  private static String post_graph_query_file = "query-graph.sparql";

  private static String delete_query_file = "query.sparql";
  private static String delete_graph_query_file = "query-graph.sparql";

  private static String get_all_dir = "get/all";
  private static String get_one_dir = "get/one";
  private static String get_related_dir = "get/related";

  private static String post_dir = "post";

  private static String delete_dir = "delete";


  public Query() {
  }

  public void get_all(String schema_name){
    write_query(query_get_all_resources( true), get_all_dir, get_all_graph_query_file, schema_name);
    write_query(query_get_all_resources( false), get_all_dir, get_all_query_file, schema_name);
    write_query(query_resource( true), get_one_dir, get_one_graph_query_file, schema_name);
    write_query(query_resource(false), get_one_dir, get_one_query_file, schema_name);
    write_query(query_resource_related( true), get_related_dir, get_related_graph_query_file, schema_name);
    write_query(query_resource_related( false), get_related_dir, get_related_query_file, schema_name);
  }



  /**
   * Returns the query to get all resources by the type
   * @param graph: boolean
   * @return
   */
  public String query_get_all_resources(Boolean graph) {
    String query = "CONSTRUCT {" +
            "?item ?predicate_item ?prop ." +
            "?prop a ?type" +
            "}" +
            "WHERE {";
    query += graph ? "GRAPH ?_graph_iri {" : "";
    query += "?item a  ?_resource_type_uri . " +
            "{" +
            "?item ?predicate_item ?prop" +
            "}" +
            "OPTIONAL {" +
            "?prop a ?type" +
            "}" +
            "}" +
            "}" +
            "}";
    query += graph ? "}" : "";
    return query;
  }

  /**
   * Return the query to a resource by the resource_id
   * @param graph
   * @return
   */
  public String query_resource(Boolean graph) {
    String query = "CONSTRUCT {" +
            "?_resource_uri ?predicate_item ?prop " +
            "?prop a ?type" +
            "}" +
            "WHERE {";
    query += graph ? "GRAPH _?graph_iri {" : "";
    query += "?_resource_uri ?predicate_item ?prop " +
            "{" +
            "?item ?predicate_item ?prop" +
            "}" +
            "OPTIONAL {" +
            "?prop a ?type" +
            "}" +
            "}" +
            "}" +
            "}";
    query += graph ? "}" : "";
    return query;
  }


  /**
   * Return the query the related resources to resource id by a predicate
   * @param graph: enable user graph
   * @return
   */
  public String query_resource_related(Boolean graph) {
    String query = "CONSTRUCT {" +
            "?s ?p ?o" +
            "}" +
            "WHERE {";
    query += graph ? "GRAPH _?graph_iri {" : "";
    query += "?_resource_uri  ?_predicate  ?s" +
            "{" +
            "?s ?o ?p" +
            "}" +
            "}";
    query += graph ? "}" : "";
    return query;
  }



  private void write_query(String query, String method_dir, String file, String schema_name) {
    String dir_path = "queries" + File.separator + schema_name + File.separator + method_dir;
    String file_path = dir_path + File.separator + file;
    File directory = new File(dir_path);
    if (! directory.exists()){
      directory.mkdirs();
    }


    BufferedWriter writer = null;
    try {
      writer = new BufferedWriter(new FileWriter(file_path));
      writer.write(query);
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}