package edu.isi.oba;

import edu.isi.oba.config.OntologyConfig;
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
import org.semanticweb.owlapi.util.DefaultPrefixManager;

import java.io.IOException;
import java.util.*;

class Mapper {
  public static final String DEFAULT_DIR_QUERY = "_default_";
  public final Map<IRI, String> schemaNames = new HashMap<>();
  public Map<String, Schema> schemas = new HashMap<>();
  final Paths paths = new Paths();
  List<String> selected_paths;
  public OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

  public Mapper(YamlConfig config_data) throws OWLOntologyCreationException {
    List<String> paths = config_data.getPaths();

    this.selected_paths = paths;
    List<String>  config_ontologies = config_data.getOntologies();
    Map<String, List<RelationConfig>> relations = config_data.getRelations();

    //Load the ontology into the manager
    for (String ontologyURL : config_ontologies) {
      this.manager.loadOntology(IRI.create(ontologyURL));
    }

    //Create a temporal Map<IRI, String> schemaNames with the classes
    for (OWLOntology ontology : this.manager.getOntologies()) {
      OWLDocumentFormat format = ontology.getFormat();
      OWLOntologyXMLNamespaceManager nsManager = new OWLOntologyXMLNamespaceManager(ontology, format);
      Set<OWLClass> classes = ontology.getClassesInSignature();
      setSchemaNames(classes);
    }

    //Add schema and paths
    for (OWLOntology ontology : this.manager.getOntologies()) {
      OWLDocumentFormat format = ontology.getFormat();
      this.createSchemas(ontology, relations, format);
    }
  }

  /**
   * Obtain Schemas using the ontology classes
   * The schemas includes the properties
   *
   * @param ontology  Represents an OWL 2 ontology
   * @param relations
   * @param format
   * @return schemas
   */
  private void createSchemas(OWLOntology ontology, Map<String, List<RelationConfig>> relations, OWLDocumentFormat format) {
    String defaultOntologyPrefixIRI = ((RDFXMLDocumentFormat) format).getDefaultPrefix();

    Set<OWLClass> classes = ontology.getClassesInSignature();

    Query query = new Query();
    Path pathGenerator = new Path();
    query.get_all(DEFAULT_DIR_QUERY);

    for (OWLClass cls : classes) {
      //filter if the class prefix is not the default ontology's prefix
      if (cls.getIRI() != null) {
        String classPrefixIRI = cls.getIRI().getNamespace();
        if (defaultOntologyPrefixIRI.equals(classPrefixIRI)) {
          MapperSchema mapperSchema = new MapperSchema(ontology, cls, "object", schemaNames);

          query.write_readme(mapperSchema.name);

          //Obtain and add OpenAPI schema
          Schema schema = mapperSchema.getSchema();
          schemas.put(schema.getName(), schema);


          //obtain the relations
//          List<RelationConfig> model_relations = relations.get(mapperSchema.name);
//          for (RelationConfig model_relation : model_relations){
//              add_path_relation(pathGenerator, model_relation.getSubject(), model_relation.getPredicate(), model_relation.getPath());
//          }


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
