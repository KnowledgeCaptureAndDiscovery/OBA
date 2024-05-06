package edu.isi.oba;

import edu.isi.oba.config.*;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.json.JSONObject;

class Oba {
  public static final String SERVERS_ZIP = "/servers.zip";
  public static final String SERVERS_DIRECTORY = "servers";
  static Logger logger = null;
  public enum LANGUAGE {
    PYTHON_FLASK
  }

  public static void main(String[] args) throws Exception {
    /*
    TODO: we are supporting one language. Issue #42
    */
	
   InputStream stream = Oba.class.getClassLoader().getResourceAsStream("logging.properties");
    try {
      LogManager.getLogManager().readConfiguration(stream);
      logger = Logger.getLogger(Oba.class.getName());
      logger.setUseParentHandlers(false);//remove double logging

    } catch (IOException e) {
      e.printStackTrace();
    }

    LANGUAGE selected_language = LANGUAGE.PYTHON_FLASK;
    logger.setLevel(Level.FINE);
    logger.addHandler(new ConsoleHandler());
	  
    //parse command line
    String config_yaml = ObaUtils.get_config_yaml(args);
    //read the config yaml from command line
    YamlConfig config_data = new YamlConfig();

    try {
      config_data = ObaUtils.get_yaml_data(config_yaml);
    } catch (Exception e){
      logger.severe("Error parsing the configuration file. Please make sure it is valid \n " + e);
      System.exit(1);
    }

    String destination_dir = config_data.getOutput_dir() + File.separator + config_data.getName();
    FirebaseConfig firebase_data = config_data.getFirebase();
    AuthConfig authConfig = config_data.getAuth();
    if (authConfig != null) {

      Provider provider = authConfig.getProvider_obj();
      if (provider.equals(Provider.FIREBASE) && firebase_data.getKey() == null) {
        logger.severe("You must set up the firebase key");
        System.exit(1);
      }
    } else {
      config_data.setAuth(new AuthConfig());
    }

    try {
        Mapper mapper = new Mapper(config_data);
        mapper.createSchemas(destination_dir);

        LinkedHashMap<String, PathItem> custom_paths = config_data.getCustom_paths();
        OpenAPI openapi_base = config_data.getOpenapi();
        String custom_queries_dir = config_data.getCustom_queries_directory();

        //copy base project
        ObaUtils.unZipIt(Oba.SERVERS_ZIP, destination_dir);
        //get schema and paths
        generate_openapi_spec(openapi_base, mapper, destination_dir, custom_paths);
        generate_openapi_template(mapper, destination_dir, config_data, selected_language);
        generate_context(config_data, destination_dir);
        copy_custom_queries(custom_queries_dir, destination_dir);
        logger.info("OBA finished successfully. Output can be found at: " + destination_dir);
    } catch (Exception e) {
        logger.severe("Error while creating the API specification: " + e.getLocalizedMessage());
        e.printStackTrace();
        System.exit(1);
    }
  }
  
  private static void generate_context(YamlConfig config_data, String destination_dir) {
    List<String> ontologies = config_data.getOntologies();
    JSONObject context_json_object = null;
    JSONObject context_json_object_class = null;
    try {
      context_json_object = ObaUtils.generate_context_file(ontologies.toArray(new String[0]), false);
      context_json_object_class = ObaUtils.generate_context_file(ontologies.toArray(new String[0]), true);
    } catch (Exception e) {
      e.printStackTrace();
    }
    String file_path = destination_dir + File.separator + "servers" + File.separator + "context.json";
    String file_path_class = destination_dir + File.separator + "servers" + File.separator + "context_class.json";
    try {
        ObaUtils.write_file(file_path, context_json_object.toString(4));
        ObaUtils.write_file(file_path_class, context_json_object_class.toString(4));
    } catch(Exception e) {
        logger.severe("Could not generate the context files: "+e.getMessage());
    }
  }

  private static void copy_custom_queries(String source, String destination) {
    if (source != null) {
      try {
        ObaUtils.copyFolder(new File(source), new File(destination + File.separator + "queries" + File.separator + "custom"));
      } catch (IOException e) {
        logger.severe("The directory custom queries doesn't exists ");
      }
    }
  }

  private static void generate_openapi_template(Mapper mapper,
                                                String destination_directory,
                                                YamlConfig config,
                                                LANGUAGE language) throws Exception {
    switch (language) {
      case PYTHON_FLASK:
        new SerializerPython(mapper, destination_directory, config);
        break;
      default:
        logger.severe("The language selected is not supported");
    }

  }

  private static void generate_openapi_spec(OpenAPI openapi_base,
                                            Mapper mapper,
                                            String dir,
                                            LinkedHashMap<String, PathItem> custom_paths
                                            ) throws Exception {
    String destinationProjectDirectory = dir + File.separator + Oba.SERVERS_DIRECTORY;
    Path destinationProject = Paths.get(destinationProjectDirectory);
    new Serializer(mapper, destinationProject, openapi_base, custom_paths);
  }
}
