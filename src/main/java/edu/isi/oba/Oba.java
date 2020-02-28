package edu.isi.oba;

import edu.isi.oba.config.YamlConfig;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.*;

class Oba {
  public static final String SERVERS_ZIP = "/servers.zip";
  static Logger logger = Logger.getLogger(Oba.class.getName());

  public static void main(String[] args) throws Exception {

    logger.setLevel(Level.FINE);
    logger.addHandler(new ConsoleHandler());


    //parse command line
    String config_yaml = get_config_yaml(args);
    //read the config yaml from command line
    YamlConfig config_data = get_yaml_data(config_yaml);
    //copy base project
    String destination_dir = config_data.getOutput_dir() + File.separator + config_data.getName();
    //read ontologies and get schema and paths
    Mapper mapper = new Mapper(config_data);
    LinkedHashMap<String, PathItem> custom_paths = config_data.getCustom_paths();
    OpenAPI openapi_base = config_data.getOpenapi();
    ObaUtils.unZipIt(SERVERS_ZIP, destination_dir);
    generate_openapi_spec(openapi_base, mapper, destination_dir, custom_paths);
  }


  private static void generate_openapi_spec(OpenAPI openapi_base, Mapper mapper, String dir, LinkedHashMap<String, PathItem> custom_paths) throws IOException {
    String destinationProjectDirectory = dir + File.separator + "servers" + File.separator + "python";
    Path destinationProject = Paths.get(destinationProjectDirectory);
    Serializer serializer = new Serializer(mapper, destinationProject, openapi_base, custom_paths);
    SerializerPython serializer_python = new SerializerPython(mapper, destinationProject, openapi_base);
  }


  private static YamlConfig get_yaml_data(String config_yaml) {
    Constructor constructor = new Constructor(YamlConfig.class);
    Yaml yaml = new Yaml(constructor);

    InputStream config_input = null;
    try {
      config_input = new FileInputStream(new File(config_yaml));
    } catch (FileNotFoundException e) {
      System.err.println("Configuration file not found: " + config_yaml);
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
      formatter.printHelp("utility-name", options);
      System.exit(1);
    }
    return config_yaml;
  }
}
