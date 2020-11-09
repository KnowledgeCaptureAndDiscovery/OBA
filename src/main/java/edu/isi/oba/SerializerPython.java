package edu.isi.oba;


import edu.isi.oba.config.*;

import org.semanticweb.owlapi.model.IRI;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static edu.isi.oba.Oba.SERVERS_DIRECTORY;
import static edu.isi.oba.Oba.logger;

class SerializerPython {
  static final String name = "python";
  static final String STATIC_DIRECTORY = SERVERS_DIRECTORY + File.separator + name + File.separator + ".openapi-generator/template/static_files/";
  static final String UTILS_DIR = STATIC_DIRECTORY + File.separator + "utils";
  static final String VARIABLES_FILE = UTILS_DIR + File.separator + "vars.py";
  static final String CONFIG_FILE = STATIC_DIRECTORY + File.separator + "settings" + File.separator + "config.ini";

  private String utils_dir;
  private String variables_file;
  private String config_file;

  String base;
  public SerializerPython(Mapper mapper,
                          String base,
                          YamlConfig config) throws Exception {

    this.base = base;
    this.utils_dir = base + File.separator + UTILS_DIR;
    this.variables_file = base + File.separator + VARIABLES_FILE;
    this.config_file = base + File.separator + CONFIG_FILE;

    //Create directory utils
    File utils_dir_path = new File(utils_dir);
    utils_dir_path.mkdir();

    //Create variable file
    FileWriter var_file_writer = new FileWriter(variables_file);
    Iterator it = mapper.schemaNames.entrySet().iterator();
    add_variable_python(var_file_writer, it);
    var_file_writer.close();


    //Create the config.ini
    try {
      EndpointConfig endpoint_config = config.getEndpoint();
      create_settings_file(endpoint_config.getUrl(), endpoint_config.getPrefix(), endpoint_config.getGraph_base(), config);
    }catch(Exception e){
      throw new Exception("Cannot create the endpoint configuration.\n" +
              "Please check that an endpoint is included in your config.yaml file\n" +
              "If you don't have an endpoint, you can use a placeholder");
    }

  }

  private void create_settings_file(String endpoint, String prefix, String graph_base, YamlConfig config){
    String content = "[defaults]\n" +
            "endpoint = " + endpoint + "\n" +
            "queries_dir = queries/\n" +
            "context_dir = contexts/\n" +
            "prefix = " + prefix + "\n" +
            "graph_base = " + graph_base + "\n";

    if (config.getAuth().getEnable() && config.getAuth().getProvider_obj() == Provider.FIREBASE)
      content.concat("firebase_key = " + config.getFirebase().getKey() + "\n");

    ObaUtils.write_file(this.config_file, content);
  }
  private void add_variable_python(FileWriter myWriter, Iterator it) {
    IRI clsIRI;
    String name;
    while (it.hasNext()) {
      Map.Entry pair = (Map.Entry) it.next();
      clsIRI = (IRI) pair.getKey();
      name = (String) pair.getValue();
      try {
        String variable_name = name.toUpperCase().replace("-", "").replace("_", "");
        //TODO: Catch class name empty
        String catch_temp = name.replace("<", "").replace(">", "");
        if (!catch_temp.equals(clsIRI.toString())) {
          myWriter.write(variable_name + "_TYPE_URI = \"" + clsIRI + "\"\n");
          myWriter.write(variable_name + "_TYPE_NAME = \"" + name + "\"\n");
        }
        else {
          logger.warning("Ignoring class " + clsIRI);
        }
      } catch (IOException e) {
        System.out.println("An error occurred.");
        e.printStackTrace();
      }
    }
  }
}