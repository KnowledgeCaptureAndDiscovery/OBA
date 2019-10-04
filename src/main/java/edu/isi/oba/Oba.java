package edu.isi.oba;

import org.apache.commons.io.FileUtils;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.cli.*;

class Oba {

  public static void main(String[] args) throws Exception {
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
          Constructor constructor = new Constructor(YamlConfig.class);
          Yaml yaml = new Yaml( constructor );

          InputStream config_input = null;
          try {
            config_input = new FileInputStream(new File(config_yaml));
          } catch (FileNotFoundException e) {
            System.err.println("Configuration file not found: " + config_yaml);
          }
          YamlConfig data = yaml.loadAs( config_input, YamlConfig.class );

          Map<String, OntologyConfig> ontologies = data.getOntologies();
          List<Mapper> mappers = new ArrayList<>();
          List<String> paths = data.getPaths();
          String dir = data.getName();

          for (Map.Entry<String, OntologyConfig> entry : ontologies.entrySet()) {
            OntologyConfig ontology = entry.getValue();
            Mapper mapper = extract_info(ontology.getXmlUrl(), ontology.getPrefix(), ontology.getPrefixUri(), paths);
            mappers.add(mapper);
          }
          String fileName = dir;
          Path destinationProject = Paths.get(fileName);
          Path baseProject = Paths.get("./tools/base_project/");
          if (Files.exists(destinationProject)){
            System.err.println("The destination project exists");
          }
          FileUtils.copyDirectory(baseProject.toFile(), destinationProject.toFile());
          Serializer serializer = new Serializer(mappers, destinationProject);

          Path destinationQueries = Paths.get(destinationProject + "/.openapi-generator/template/static_files/queries/");
          Path sourceQueries = Paths.get("queries");
          FileUtils.copyDirectory(sourceQueries.toFile(), destinationQueries.toFile());

          File source = new File("owl2jsonld-0.2.1-standalone.jar");
          File dest = new File(destinationProject + "/owl2jsonld-0.2.1-standalone.jar");

          FileUtils.copyFile(source, dest);

  }


        public static Mapper extract_info(String ont_serialization_url, String ont_prefix, String ont_uri, List<String> paths) throws OWLOntologyCreationException, IOException {
          Map<String, String> prefixes = new HashMap<>();
          prefixes.put(ont_prefix, ont_uri);
          return new Mapper(ont_serialization_url, ont_prefix, prefixes, paths);
        }
}
