package edu.isi.oba;

class Query {
  private final String prefix;

  public Query(String prefix) {
    this.prefix = prefix;
  }

  /**
   * Return the graph uri of the user
   * @param username the username of a user
   * @return
   */

  public String build_graph_uri(String username){
    return String.format("%s/%s_", username);
  }

  /**
   * Return the resource uri of a resource
   * @param resource_id
   * @return
   */
  private String build_resource_uri(String resource_id){
    return String.format("%s/%s",this.prefix, resource_id);
  }

  /**
   * Returns the query to get all resources by the type
   * @param graph: boolean
   * @param resource_type Model
   * @return
   */
  public String query_get_all_resources(String resource_type, Boolean graph) {
    String resource_type_uri = build_resource_uri(resource_type);
    String query = "CONSTRUCT {" +
            "?item ?predicate_item ?prop ." +
            "?prop a ?type" +
            "}" +
            "WHERE {";
    query += graph ? "GRAPH _?graph_iri {" : "";
    query += "?item a <{" + resource_type_uri + "}> . " +
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
   * @param resource_id:
   * @param graph
   * @return
   */
  public String query_resource(String resource_id, Boolean graph) {
    String resource_uri = build_resource_uri(resource_id);
    String query = "CONSTRUCT {" +
            "<" + resource_uri + "> ?predicate_item ?prop ." +
              "?prop a ?type" +
            "}" +
            "WHERE {";
    query += graph ? "GRAPH _?graph_iri {" : "";
    query += "<" + resource_uri + "> ?predicate_item ?prop " +
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
   * @param resource_id: the name of the subject
   * @param predicate: the name of the resource
   * @param graph: enable user graph
   * @return
   */
  public String query_resource_related(String resource_id, String predicate, Boolean graph) {
    String resource_uri = build_resource_uri(resource_id);
    String query = "CONSTRUCT {" +
            "?s ?p ?o" +
            "}" +
            "WHERE {";
    query += graph ? "GRAPH _?graph_iri {" : "";
    query += "<" + resource_uri + "> <" + predicate + "> ?s" +
            "{" +
            "?s ?o ?p" +
            "}" +
            "}";
    query += graph ? "}" : "";
    return query;
  }

}