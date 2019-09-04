package edu.isi.oba;

import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Schema;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.util.IRIShortFormProvider;
import org.semanticweb.owlapi.util.SimpleIRIShortFormProvider;

import java.io.IOException;
import java.util.*;

class Mapper {
  private PrefixManager pm;
  private OWLReasoner reasoner;
  private final IRIShortFormProvider sfp = new SimpleIRIShortFormProvider();

  public Map<IRI, String> schemaNames = new HashMap<>();
  public Map<String, Schema> schemas = new HashMap<>();
  Paths paths = new Paths();

  private String ont_prefix;

  public Mapper(String ont_url, String ont_prefix, Map<String, String> prefixes) throws OWLOntologyCreationException, IOException {
    OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    OWLOntology ontology = manager.loadOntology(IRI.create(ont_url));
    OWLDocumentFormat format = manager.getOntologyFormat(ontology);
    OWLReasonerFactory reasonerFactory = new StructuralReasonerFactory();
    this.ont_prefix = ont_prefix;
    reasoner = reasonerFactory.createReasoner(ontology);

    setPrefixes(format, prefixes);
    schemas = this.createSchemas(ontology);
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
   * @return schemas
   */
  private Map<String, Schema> createSchemas(OWLOntology ontology) {
    Set<OWLClass> classes;
    classes = ontology.getClassesInSignature();
    Map<String, Schema> schemas = new HashMap<>();

    Query query = new Query();
    Path pathGenerator = new Path();


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
          MapperSchema mapperSchema = new MapperSchema(ontology, cls, "object", pm, schemaNames);
          query.get_all(mapperSchema.name);
          Schema schema = mapperSchema.getSchema();
          schemas.put(schema.getName(), schema);

          String singular_name = "/" + mapperSchema.name.toLowerCase() + "s/{id}";
          //TODO: find better way to obtain the plural name
          String plural_name = "/" + mapperSchema.name.toLowerCase() + "s";

          //Create the plural paths: for example: /models/
          this.paths.addPathItem(plural_name, pathGenerator.generate_plural(mapperSchema.name));
          //Create the plural paths: for example: /models/id
          this.paths.addPathItem(singular_name, pathGenerator.generate_singular(mapperSchema.name));

        }
      }
    }

    return schemas;
  }

}
