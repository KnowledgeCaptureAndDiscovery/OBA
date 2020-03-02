package edu.isi.oba;


import edu.isi.oba.config.EndpointConfig;
import edu.isi.oba.config.FirebaseConfig;
import io.swagger.v3.oas.models.*;

import org.semanticweb.owlapi.model.IRI;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static edu.isi.oba.Oba.SERVERS_DIRECTORY;

class SerializerPython {
  static final String name = "python";
  static final String STATIC_DIRECTORY = SERVERS_DIRECTORY + File.separator + name + File.separator + ".openapi-generator/template/static_files/";
  static final String UTILS_DIR = STATIC_DIRECTORY + File.separator + "utils";
  static final String VARIABLES_FILE = UTILS_DIR + File.separator + "vars.py";
  static final String CONFIG_FILE = STATIC_DIRECTORY + File.separator + "settings" + File.separator + "config.ini";

  private String utils_dir;
  private String variables_file;
  private String config_fie;

  String base;
  public SerializerPython(Mapper mapper,
                          String base,
                          EndpointConfig endpoint_config,
                          FirebaseConfig firebase_config) throws IOException {

    this.base = base;
    this.utils_dir = base + File.separator + UTILS_DIR;
    this.variables_file = base + File.separator + VARIABLES_FILE;
    this.config_fie = base + File.separator + CONFIG_FILE;

    //Create directory utils
    File utils_dir_path = new File(utils_dir);
    utils_dir_path.mkdir();

    //Create variable file
    FileWriter var_file_writer = new FileWriter(variables_file);
    Iterator it = mapper.schemaNames.entrySet().iterator();
    add_variable_python(var_file_writer, it);
    var_file_writer.close();

    //Create the config.ini
    create_settings_file(endpoint_config.url, endpoint_config.prefix, endpoint_config.graph_base, firebase_config.key);
  }

  private void create_settings_file(String endpoint, String prefix, String graph_base, String firebase_key ){
    String content = "[defaults]\n" +
            "endpoint = " + endpoint + "\n" +
            "queries_dir = queries/\n" +
            "context_dir = contexts/\n" +
            "prefix = " + prefix + "\n" +
            "graph_base = " + graph_base + "\n" +
            "firebase_key = " + firebase_key + "\n";
    ObaUtils.write_file(this.config_fie, content);
  }
  private void add_variable_python(FileWriter myWriter, Iterator it) {
    IRI clsIRI;
    String name;
    while (it.hasNext()) {
      Map.Entry pair = (Map.Entry) it.next();
      clsIRI = (IRI) pair.getKey();
      name = (String) pair.getValue();
      try {
        String variable_name = name.toUpperCase().replace("-", "");
        myWriter.write(variable_name + "_TYPE_URI = \"" + clsIRI + "\"\n");
        myWriter.write(variable_name + "_TYPE_NAME = \"" + name + "\"\n");
      } catch (IOException e) {
        System.out.println("An error occurred.");
        e.printStackTrace();
      }
    }
  }
}