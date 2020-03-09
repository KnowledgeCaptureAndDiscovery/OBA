package edu.isi.oba;

import edu.isi.oba.config.EndpointConfig;
import edu.isi.oba.config.FirebaseConfig;
import edu.isi.oba.config.YamlConfig;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.*;

class Oba {
  public static final String SERVERS_ZIP = "/servers.zip";
  public static final String SERVERS_DIRECTORY = "servers";
  static Logger logger = Logger.getLogger(Oba.class.getName());
  public enum LANGUAGE {
    PYTHON_FLASK
  }
  public static void main(String[] args) throws Exception {
    /*
    TODO: we are supporting one language. Issue #42
    */

    LANGUAGE selected_language = LANGUAGE.PYTHON_FLASK;
    logger.setLevel(Level.WARNING);
    logger.addHandler(new ConsoleHandler());

    //parse command line
    String config_yaml = get_config_yaml(args);
    //read the config yaml from command line
    YamlConfig config_data = new YamlConfig();

    try {
      config_data = get_yaml_data(config_yaml);
    } catch (Exception e){
      logger.severe("Error in the configuration file. Please review it \n " + e);
      System.exit(1);
    }

    String destination_dir = config_data.getOutput_dir() + File.separator + config_data.getName();
    EndpointConfig endpoint_data = config_data.getEndpoint();
    FirebaseConfig firebase_data = config_data.getFirebase();
    Mapper mapper = new Mapper(config_data);
    LinkedHashMap<String, PathItem> custom_paths = config_data.getCustom_paths();
    OpenAPI openapi_base = config_data.getOpenapi();

    //copy base project
    ObaUtils.unZipIt(SERVERS_ZIP, destination_dir);
    //get schema and paths
    generate_openapi_spec(openapi_base, mapper, destination_dir, custom_paths);
    generate_openapi_template(mapper, destination_dir, endpoint_data, firebase_data, selected_language);
  }

  private static void generate_openapi_template(Mapper mapper,
                                                String destination_directory,
                                                EndpointConfig endpoint_config,
                                                FirebaseConfig firebase_config,
                                                LANGUAGE language) throws IOException {
    switch (language) {
      case PYTHON_FLASK:
        new SerializerPython(mapper, destination_directory, endpoint_config, firebase_config);
        break;
      default:
        logger.severe("Language is not supported");
    }

  }

  private static void generate_openapi_spec(OpenAPI openapi_base,
                                            Mapper mapper,
                                            String dir,
                                            LinkedHashMap<String, PathItem> custom_paths
                                            ) throws IOException {
    String destinationProjectDirectory = dir + File.separator + SERVERS_DIRECTORY;
    Path destinationProject = Paths.get(destinationProjectDirectory);
    new Serializer(mapper, destinationProject, openapi_base, custom_paths);
  }


  private static YamlConfig get_yaml_data(String config_yaml) {
    Constructor constructor = new Constructor(YamlConfig.class);
    Yaml yaml = new Yaml(constructor);

    InputStream config_input = null;
    try {
      config_input = new FileInputStream(new File(config_yaml));
    } catch (FileNotFoundException e) {
      System.err.println("Configuration file not found: " + config_yaml);
      System.exit(1);
    }
    //Yaml config parse
    return yaml.loadAs(config_input, YamlConfig.class);
  }

  private static String get_config_yaml(String[] args) {
    //obtain the options to pass configuration
    Options options = new Options();
    Option input = new Option("c", "config", true, "configuration file path");
    input.setRequired(true);
    options.addOption(input);

    CommandLineParser parser = new DefaultParser();
    HelpFormatter formatter = new HelpFormatter();
    CommandLine cmd;
    String config_yaml = null;

    try {
      cmd = parser.parse(options, args);
      config_yaml = cmd.getOptionValue("config");
    } catch (ParseException e) {
      System.out.println(e.getMessage());
      formatter.printHelp("utiConfiguration filelity-name", options);
      System.exit(1);
    }
    return config_yaml;
  }
}
