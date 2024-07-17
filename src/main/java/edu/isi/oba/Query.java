package edu.isi.oba;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

class Query {

  private String queryDirectory;

  private static final String GET_ALL_QUERY_FILE = "/queries/get_all.rq";
  private static final String GET_ALL_GRAPH_QUERY_FILE = "/queries/get_all_user.rq";

  private static final String GET_ONE_QUERY_FILE = "/queries/get_one.rq";
  private static final String GET_ONE_GRAPH_QUERY_FILE = "/queries/get_one_user.rq";

  private static final String GET_ALL_SEARCH_QUERY_FILE = "/queries/get_all_search.rq";
  private static final String GET_ALL_SEARCH_GRAPH_QUERY_FILE = "/queries/get_all_search_user.rq";


  public Query(String queryDirectory) {
    this.queryDirectory = queryDirectory + File.separator + "queries";
  }

  public void getAll(String schema_name) throws Exception {
    writeQuery(GET_ALL_QUERY_FILE, schema_name);
    writeQuery(GET_ALL_GRAPH_QUERY_FILE, schema_name);
    writeQuery(GET_ONE_QUERY_FILE, schema_name);
    writeQuery(GET_ONE_GRAPH_QUERY_FILE, schema_name);
    writeQuery(GET_ALL_SEARCH_QUERY_FILE, schema_name);
    writeQuery(GET_ALL_SEARCH_GRAPH_QUERY_FILE, schema_name);
  }

  public void writeReadme(String schema_name) {
    final var dirPath = this.queryDirectory + File.separator + schema_name + File.separator;
    final var filePath = dirPath + File.separator + "README";
    final var directory = new File(dirPath);
    if (! directory.exists()){
      directory.mkdirs();
    }

    BufferedWriter writer = null;
    try {
      writer = new BufferedWriter(new FileWriter(filePath));
      writer.write("To modify the query from this class, edit this file");
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void writeQuery(String queryFileName, String schemaName) throws Exception {
    final var dirPath = this.queryDirectory + File.separator + schemaName + File.separator;
    final var directory = new File(dirPath);
    if (!directory.exists()){
      directory.mkdirs();
    }

    final var resource = new File(ObaUtils.class.getResource(queryFileName).getFile());
    final var queryFile = ObaUtils.class.getResourceAsStream(queryFileName);
    final var filePath = new File(dirPath + File.separator + resource.getName());
    ObaUtils.copy(queryFile, filePath);
  }
}
