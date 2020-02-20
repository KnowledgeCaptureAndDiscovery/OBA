package edu.isi.oba;

import edu.isi.oba.config.YamlConfig;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

import org.apache.commons.cli.*;

class Oba {
  public static void main(String[] args) throws Exception {
    String resourcesFolder = "oba_python";

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
    generate_openapi_spec(openapi_base, mapper, destination_dir, custom_paths);

    //python_copy_base_project(resourcesFolder, destination_dir);
  }


  /**
   * Copy the base project dir for a python project
   * @param base_project_dir
   * @param destination_dir
   * @throws IOException
   */
  private static void python_copy_base_project(String base_project_dir, String destination_dir) throws IOException, URISyntaxException {
    DirectoryCopy d = new DirectoryCopy();
    InputStream originFolder = Oba.class.getResourceAsStream(base_project_dir);

    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    URL url = loader.getResource("oba_python");
    String path = url.getPath();
    File[] a = new File(path).listFiles();
    System.out.println(path);
    //System.out.println(originFolder.getPath());
    System.out.println(destination_dir);

    d = new DirectoryCopy();
    d.copyFolder(new File(path), new File(destination_dir));

    System.out.println(destination_dir);




  }

  private static void generate_openapi_spec(OpenAPI openapi_base, Mapper mapper, String dir, LinkedHashMap<String, PathItem> custom_paths) throws IOException {
    String destinationProjectDirectory = dir;
    Path destinationProject = Paths.get(destinationProjectDirectory);
    Serializer serializer = new Serializer(mapper, destinationProject, openapi_base, custom_paths);
    SerializerPython serializer_python = new SerializerPython(mapper, destinationProject, openapi_base);
  }

  private static File[] getResourceFolderFiles (String folder) {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    URL url = loader.getResource(folder);
    String path = url.getPath();
    return new File(path).listFiles();
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
