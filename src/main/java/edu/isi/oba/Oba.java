package edu.isi.oba;

import edu.isi.oba.config.OntologyConfig;
import edu.isi.oba.config.RelationConfig;
import edu.isi.oba.config.YamlConfig;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import org.apache.commons.io.FileUtils;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.*;

class Oba {

  public static void main(String[] args) throws Exception {
    //parse command line
    String config_yaml = get_config_yaml(args);
    //read the config yaml from command line
    YamlConfig data = get_yaml_data(config_yaml);
    //read ontologies and get schema and paths
    List<Mapper> mappers = get_mappers(data);
    //get base of openapi
    OpenAPI openapi_base = new OpenAPIV3Parser().read(data.getOpenapi_base());
    //obtain the output directory to write the openapi specification
    String dir = data.getOutput_dir() + File.separator + data.getName();
    //write the openapi specification
    generate_openapi_spec(openapi_base, mappers, dir);
  }

  private static List<Mapper> get_mappers(YamlConfig data) throws OWLOntologyCreationException, IOException {
    Map<String, OntologyConfig> ontologies = data.getOntologies();
    List<Mapper> mappers = new ArrayList<>();
    List<String> paths = data.getPaths();
    Map<String, List<RelationConfig>> relations = data.getRelations();
    for (Map.Entry<String, OntologyConfig> entry : ontologies.entrySet()) {
      OntologyConfig ontology = entry.getValue();
      Mapper mapper = extract_info(ontology.getXmlUrl(), ontology.getPrefix(), ontology.getPrefixUri(), paths, relations);
      mappers.add(mapper);
    }
    return mappers;
  }

  private static void generate_openapi_spec(OpenAPI openapi_base, List<Mapper> mappers, String dir) throws IOException {
    String fileName = dir;
    Path destinationProject = Paths.get(fileName);
    Path baseProject = Paths.get("./tools/base_project/");
    if (Files.exists(destinationProject)) {
      System.err.println("The destination project exists");
    }
    FileUtils.copyDirectory(baseProject.toFile(), destinationProject.toFile());
    Serializer serializer = new Serializer(mappers, destinationProject, openapi_base);

    Path destinationQueries = Paths.get(destinationProject + "/.openapi-generator/template/static_files/queries/");
    Path sourceQueries = Paths.get("queries");
    FileUtils.copyDirectory(sourceQueries.toFile(), destinationQueries.toFile());
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


  public static Mapper extract_info(String ont_serialization_url, String ont_prefix, String ont_uri, List<String> paths, Map<String, List<RelationConfig>> relations) throws OWLOntologyCreationException, IOException {
    Map<String, String> prefixes = new HashMap<>();
    prefixes.put(ont_prefix, ont_uri);
    return new Mapper(ont_serialization_url, ont_prefix, prefixes, paths, relations);
  }
}
