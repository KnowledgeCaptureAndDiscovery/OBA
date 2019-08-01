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

  public Mapper(String ont_url, String ont_prefix) throws OWLOntologyCreationException, IOException {
    OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    OWLOntology ontology = manager.loadOntology(IRI.create(ont_url));
    OWLDocumentFormat format = manager.getOntologyFormat(ontology);
    OWLReasonerFactory reasonerFactory = new StructuralReasonerFactory();
    this.ont_prefix = ont_prefix;
    reasoner = reasonerFactory.createReasoner(ontology);

    setPrefixes(format);
    schemas = this.createSchemas(ontology);
  }

  /**
   * Manually set the prefixes
   * @param format: Represents the concrete representation format of an ontology
   */
  private void setPrefixes(OWLDocumentFormat format) {
    if (format.isPrefixOWLDocumentFormat()) {
      this.pm = format.asPrefixOWLDocumentFormat();
    } else {
      this.pm = new DefaultPrefixManager();

    }
    this.pm.setPrefix("sd", "https://w3id.org/okn/o/sd#");
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
        if (prefix.equals(this.ont_prefix)) {
          schemaNames.put(cls.getIRI(), prefixIRI);
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

          this.paths.addPathItem(mapperSchema.name, pathGenerator.generate_plurar(mapperSchema));

        }
      }
    }

    return schemas;
  }

}
