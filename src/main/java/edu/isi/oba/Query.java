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
  private static final String get_all_query_file = "get_all.rq";
  private static final String get_all_graph_query_file = "get_all_user.rq";
  private static final String get_one_query_file = "get_one.rq";
  private static final String get_one_graph_query_file = "get_one_user.rq";
  private static final String get_related_query_file = "get_all_related.rq";
  private static final String get_related_graph_query_file = "get_all_related_user.rq";


  public Query() {
  }

  public void get_all(String schema_name){
    write_query(query_get_all_resources( true), get_all_graph_query_file, schema_name);
    write_query(query_get_all_resources( false), get_all_query_file, schema_name);
    write_query(query_resource( true), get_one_graph_query_file, schema_name);
    write_query(query_resource(false), get_one_query_file, schema_name);
    write_query(query_resource_related( true), get_related_graph_query_file, schema_name);
    write_query(query_resource_related( false), get_related_query_file, schema_name);
  }



  /**
   * Returns the query to get all resources by the type
   * @param graph: boolean
   * @return
   */
  private String query_get_all_resources(Boolean graph) {
    String query = "#+ summary: Given a rdf type, returns all the resources related to the type\n" +
            "CONSTRUCT {\n" +
            "?item ?predicate ?prop .\n" +
            "?prop a ?type\n" +
            "}\n" +
            "WHERE {\n";
    query += graph ? "GRAPH ?_g_iri {\n" : "\n";
    query += "?item a  ?_type_iri . \n" +
            "{\n" +
            "?item ?predicate ?prop\n" +
            "}\n" +
            "OPTIONAL {\n" +
            "?prop a ?type\n" +
            "}\n" +
            "}";
    query += graph ? "}" : "";
    return query;
  }

  /**
   * Return the query to a resource by the resource_id
   * @param graph
   * @return
   */
  private String query_resource(Boolean graph) {
    String query = "#+ summary: Return the query to a resource by the resource_iri\n" +
            "CONSTRUCT {\n" +
            "?_resource_iri ?predicate ?prop .\n" +
            "?prop a ?type\n" +
            "}\n" +
            "WHERE {\n";
    query += graph ? "GRAPH ?_g_iri {\n" : "\n";
    query += "?_resource_iri ?predicate ?prop \n" +
            "{\n" +
            "?item ?predicate ?prop\n" +
            "}\n" +
            "OPTIONAL {\n" +
            "?prop a ?type\n" +
            "}\n" +
            "}\n";
    query += graph ? "}" : "";
    return query;
  }


  /**
   * Return the query the related resources to resource id by a predicate
   * @param graph: enable user graph
   * @return
   */
  private String query_resource_related(Boolean graph) {
    String query = "#+ summary: Return the query the related resources to resource_iri by a predicate_ir\n" +
            "CONSTRUCT {\n" +
            "?s ?p ?o\n" +
            "}\n" +
            "WHERE {\n";
    query += graph ? "GRAPH ?_g_iri {\n" : "";
    query += "?_resource_iri  ?_predicate_iri  ?s .\n" +
            "?s ?o ?p\n" +
            "}\n";
    query += graph ? "}" : "";

    return query;
  }


  private void write_query(String query, String file, String schema_name) {
    String dir_path = "queries" + File.separator + schema_name + File.separator;
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