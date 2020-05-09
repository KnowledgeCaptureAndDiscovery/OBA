package edu.isi.oba;

import java.io.*;

class Query {

  private String query_directory;
  private static final String get_all_query_file = "/queries/get_all.rq";
  private static final String get_all_graph_query_file = "/queries/get_all_user.rq";

  private static final String get_one_query_file = "/queries/get_one.rq";
  private static final String get_one_graph_query_file = "/queries/get_one_user.rq";

  private static final String get_all_search_query_file = "/queries/get_all_search.rq";
  private static final String get_all_search_graph_query_file = "/queries/get_all_search_user.rq";


  public Query(String query_directory) {
    this.query_directory = query_directory + File.separator + "queries";
  }

  public void get_all(String schema_name) throws Exception {
    String dir_path = query_directory + File.separator + schema_name + File.separator;
    write_query(get_all_query_file, schema_name);
    write_query(get_all_graph_query_file, schema_name);
    write_query(get_one_query_file, schema_name);
    write_query(get_one_graph_query_file, schema_name);
    write_query(get_all_search_query_file, schema_name);
    write_query(get_all_search_graph_query_file, schema_name);
  }

  public void write_readme(String schema_name) {
    String dir_path = query_directory + File.separator + schema_name + File.separator;
    String file_path = dir_path + File.separator + "README";
    File directory = new File(dir_path);
    if (! directory.exists()){
      directory.mkdirs();
    }


    BufferedWriter writer = null;
    try {
      writer = new BufferedWriter(new FileWriter(file_path));
      writer.write("To modify the query from this class, edit this file");
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void write_query(String query_file_name, String schema_name) throws Exception {
    String dir_path = query_directory + File.separator + schema_name + File.separator;
    File directory = new File(dir_path);
    if (! directory.exists()){
      directory.mkdirs();
    }
    File resource = new File(ObaUtils.class.getResource(query_file_name).getFile());
    InputStream query_file = ObaUtils.class.getResourceAsStream(query_file_name);
    File file_path = new File(dir_path + File.separator + resource.getName());
    ObaUtils.copy(query_file, file_path);
  }
}
