package edu.isi.oba;

import edu.isi.oba.config.RelationConfig;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.media.XML;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

import java.io.IOException;
import java.util.*;

class Mapper {
  public static final String DEFAULT_DIR_QUERY = "_default_";
  private PrefixManager pm;

  public final Map<IRI, String> schemaNames = new HashMap<>();
  public Map<String, Schema> schemas;
  final Paths paths = new Paths();
  public String ont_prefix;
  List<String> selected_paths;

  public Mapper(String ont_url, String ont_prefix, Map<String, String> prefixes,
                List<String> paths, Map<String, List<RelationConfig>> relations)
          throws OWLOntologyCreationException, IOException {
    OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    OWLOntology ontology = manager.loadOntology(IRI.create(ont_url));
    OWLDocumentFormat format = manager.getOntologyFormat(ontology);
    this.selected_paths = paths;
    this.ont_prefix = ont_prefix;
    setPrefixes(format, prefixes);
    schemas = this.createSchemas(ontology, relations);


  }

  /**
   * Manually set the prefixes
   * @param format: Represents the concrete representation format of an ontology
   */
  private void setPrefixes(OWLDocumentFormat format, Map<String, String> prefixes) {
    if (format.isPrefixOWLDocumentFormat()) {
      this.pm = format.asPrefixOWLDocumentFormat();
    } else {
      this.pm = new DefaultPrefixManager();

    }
    for (Map.Entry prefix : prefixes.entrySet()) {
      this.pm.setPrefix(prefix.getKey().toString(), prefix.getValue().toString());
    }
  }


  /**
   * Obtain Schemas using the ontology classes
   * The schemas includes the properties
   *
   * @param ontology  Represents an OWL 2 ontology
   * @param relations
   * @return schemas
   */
  private Map<String, Schema> createSchemas(OWLOntology ontology, Map<String, List<RelationConfig>> relations) {
    Set<OWLClass> classes;
    classes = ontology.   getClassesInSignature();
    Map<String, Schema> schemas = new HashMap<>();

    Query query = new Query();
    Path pathGenerator = new Path();
    query.get_all(DEFAULT_DIR_QUERY);


    for (OWLClass cls : classes) {
      String prefixIRI = this.pm.getPrefixIRI(cls.getIRI());
      if (prefixIRI != null) {
        String prefix = prefixIRI.split(":")[0];
        String name =  prefixIRI.split(":")[1];
        if (prefix.equals(this.ont_prefix)) {
          schemaNames.put(cls.getIRI(), name);
        }
      }
    }

    for (OWLClass cls : classes) {

      String prefixIRI = this.pm.getPrefixIRI(cls.getIRI());
      if (prefixIRI != null) {
        String prefix = prefixIRI.split(":")[0];
        if (prefix.equals(this.ont_prefix)) {
          MapperSchema mapperSchema = new MapperSchema(ontology, cls, "object", schemaNames);

          query.write_readme(mapperSchema.name);

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

    return schemas;
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
