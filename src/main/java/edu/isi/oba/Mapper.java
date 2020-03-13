package edu.isi.oba;

import edu.isi.oba.config.RelationConfig;
import edu.isi.oba.config.YamlConfig;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.media.XML;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.rdf.rdfxml.renderer.OWLOntologyXMLNamespaceManager;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

class Mapper {
  public static final String DEFAULT_DIR_QUERY = "_default_";
  public final Map<IRI, String> schemaNames = new HashMap<>();
  public Map<String, Schema> schemas = new HashMap<>();
  final Paths paths = new Paths();
  List<String> selected_paths;
  List<OWLOntology> ontologies;

  public OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

  public Mapper(YamlConfig config_data) throws OWLOntologyCreationException {
    List<String> paths = config_data.getPaths();

    this.selected_paths = paths;
    List<String> config_ontologies = config_data.getOntologies();
    String destination_dir = config_data.getOutput_dir() + File.separator + config_data.getName();

    //Load the ontology into the manager
    for (String ontologyURL : config_ontologies) {
      this.manager.loadOntology(IRI.create(ontologyURL));
    }
    ontologies = this.manager.ontologies().collect(Collectors.toList());

    //Create a temporal Map<IRI, String> schemaNames with the classes
    for (OWLOntology ontology : ontologies) {
      Set<OWLClass> classes = ontology.getClassesInSignature();
      setSchemaNames(classes);
    }
    //Create OpenAPI spec
    this.createSchemas(destination_dir, config_data);
  }

  /**
   * Obtain Schemas using the ontology classes
   * The schemas includes the properties
   *
   * @param config_data
   * @return schemas
   */
  private void createSchemas(String destination_dir, YamlConfig config_data) {
    Query query = new Query(destination_dir);
    Path pathGenerator = new Path(config_data.getEnable_get_paths(),
                                  config_data.getEnable_post_paths(),
                                  config_data.getEnable_put_paths(),
                                  config_data.getEnable_delete_paths()
                                 );
    query.get_all(DEFAULT_DIR_QUERY);

    for (OWLOntology ontology  : this.ontologies){
      OWLDocumentFormat format = ontology.getFormat();
      String defaultOntologyPrefixIRI = ((RDFXMLDocumentFormat) format).getDefaultPrefix();
      Set<OWLClass> classes = ontology.getClassesInSignature();
      for (OWLClass cls : classes) {
        //filter if the class prefix is not the default ontology's prefix
        if (cls.getIRI() != null) {
          String classPrefixIRI = cls.getIRI().getNamespace();
          if (defaultOntologyPrefixIRI.equals(classPrefixIRI)) {
            //Convert from OWL Class to OpenAPI Schema.
            MapperSchema mapperSchema = new MapperSchema(this.ontologies, cls, schemaNames, ontology);
            //Write the query
            query.write_readme(mapperSchema.name);

            //Create the schema
            Schema schema = mapperSchema.getSchema();
            schemas.put(schema.getName(), schema);


            //Add the paths
            if (this.selected_paths == null){
              add_path(pathGenerator, mapperSchema);
            } else {
              for (String str : this.selected_paths) {
                String search = str.trim().toLowerCase();
                String schemaName = mapperSchema.name.toLowerCase();
                if (search.trim().toLowerCase().equals(schemaName)) {
                  add_path(pathGenerator, mapperSchema);
                }
              }
            }
          }
        }

    }





      //User schema
      Map<String, Schema> userProperties = new HashMap<>();
      StringSchema username = new StringSchema();
      StringSchema password = new StringSchema();
      userProperties.put("username", username);
      userProperties.put("password", password);

      Schema userSchema = new Schema();
      userSchema.setName("User");
      userSchema.setType("object");
      userSchema.setProperties(userProperties);
      userSchema.setXml(new XML().name("User"));
      schemas.put("User", userSchema);

      this.paths.addPathItem("/user/login", pathGenerator.user_login());
    }

  }

  private void setSchemaNames(Set<OWLClass> classes) {
    for (OWLClass cls : classes) {
        schemaNames.put(cls.getIRI(), cls.getIRI().getShortForm());
    }
  }

  private void add_path(Path pathGenerator, MapperSchema mapperSchema) {
    String singular_name = "/" + mapperSchema.name.toLowerCase() + "s/{id}";
    String plural_name = "/" + mapperSchema.name.toLowerCase() + "s";
    //Create the plural paths: for example: /models/
    this.paths.addPathItem(plural_name, pathGenerator.generate_plural(mapperSchema.name));
    //Create the plural paths: for example: /models/id
    this.paths.addPathItem(singular_name, pathGenerator.generate_singular(mapperSchema.name));
  }

  private void add_path_relation(Path pathGenerator, String schema_name, String predicate, String path) {
    String relation = "/" + schema_name.toLowerCase() + "s/{id}/" + path;
    this.paths.addPathItem(relation, pathGenerator.generate_plural(schema_name));

  }

}
