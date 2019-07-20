package edu.isi.oba;

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

  private Map<IRI, String> schemaNames = new HashMap<>();


  public Mapper(String ont_url, String ont_prefix) throws OWLOntologyCreationException, IOException {
    OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    OWLOntology ontology = manager.loadOntology(IRI.create(ont_url));
    OWLDocumentFormat format = manager.getOntologyFormat(ontology);
    OWLReasonerFactory reasonerFactory = new StructuralReasonerFactory();
    reasoner = reasonerFactory.createReasoner(ontology);

    setPrefixes(format);

    schemaNames = this.setSchemaNames(ontology);
    Map<String, Schema> schemas = this.createSchemas(ontology);
    Serializer serializer = new Serializer(schemas);
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
    this.pm.setPrefix("qudt", "http://qudt.org/schema/qudt/");
    this.pm.setPrefix("onto", "http://ontosoft.org/software#");
    this.pm.setPrefix("geo", "http://www.geoscienceontology.org/geo-upper#");
    this.pm.setPrefix("schema", "http://schema.org/");
    this.pm.setPrefix("skos", "http://www.w3.org/2004/02/skos/core#");
    this.pm.setPrefix("dcat", "http://www.w3.org/ns/dcat#");
    this.pm.setPrefix("dataCatalog", "https://w3id.org/mint/dataCatalog#");
    this.pm.setPrefix("mint", "https://w3id.org/mint/modelCatalog#");
  }

  /**
   * Extract the SchemaNames. It reads the ontology
   * @param ontology:  Represents an OWL 2 ontology
   * @return A hashmap key: class_uri and value: class name
   */
    private Map<IRI, String> setSchemaNames(OWLOntology ontology) {
    Set<OWLClass> classes;
    classes = ontology.getClassesInSignature();
    List<String> classesName = new ArrayList<>();
    List<String> dups = new ArrayList<>();
    //Create auxiliar list to detect duplicate name
    for (OWLClass cls : classes) {
      classesName.add(this.sfp.getShortForm(cls.getIRI()));
    }
    Set<String> uniqueNames = new HashSet<>(classesName);

    for (String uniqueName : uniqueNames) {
      if (Collections.frequency(classesName, uniqueName) > 1) {
        dups.add(uniqueName);
      }
    }

    for (OWLClass cls : classes) {
      String className = this.sfp.getShortForm(cls.getIRI());
      String schemaName;
      if (dups.contains(className)) {
        schemaName = this.pm.getPrefixIRI(cls.getIRI()).replace(":", "-");
      } else {
        schemaName = this.sfp.getShortForm(cls.getIRI());
      }
      schemaNames.put(cls.getIRI(), schemaName);
    }
    return schemaNames;
  }

  /**
   * Get the value (class_name)
   * @param cls class ontology
   * @return class name
   */
  private String getSchemaName(OWLClass cls) {
    return schemaNames.get(cls.getIRI());
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

    for (OWLClass cls : classes) {
      Map<String, Schema> dataProperties = this.getDataProperties(ontology, cls);
      Map<String, Schema> objectProperties = this.getObjectProperties(ontology, cls);
      Map<String, Schema> properties = new HashMap<>();
      properties.putAll(dataProperties);
      properties.putAll(objectProperties);
      MapperSchema mapperSchema = new MapperSchema();
      schemas.put(getSchemaName(cls), mapperSchema.getSchema(getSchemaName(cls), "object", properties));
    }

    return schemas;
  }

  /**
   * Check if the class cls is domain of the property dp
   *
   * @param cls class
   * @param dp PropertyDomain
   * @return true or false
   */
  private boolean checkDomainClass(OWLClass cls, OWLPropertyDomainAxiom dp) {
    Set<OWLClass> superDomainClasses = reasoner.getSuperClasses(cls, true).getFlattened();
    Set<OWLClass> domainClasses = dp.getDomain().getClassesInSignature();
    for (OWLClass domainClass : domainClasses) {
      if (domainClass.equals(cls))
        return true;

      //check super classes
      for (OWLClass superClass : superDomainClasses) {
        if (domainClass.equals(superClass)) {
          return true;
        }
      }
    }
    return false;
  }


  /**
   * Obtain a list of Codegenproperty of a OWLClass
   *
   * @param ontology Ontology
   * @param cls      Class
   * @return A HashMap key: property name, value: SchemaProperty
   */
  private Map<String, Schema> getDataProperties(OWLOntology ontology, OWLClass cls) {
    HashMap<String, String> propertyNameURI = new HashMap<>();
    Map<String, Schema> properties = new HashMap<>();

    for (OWLDataPropertyDomainAxiom dp : ontology.getAxioms(AxiomType.DATA_PROPERTY_DOMAIN)) {
      if (checkDomainClass(cls, dp)) {
        for (OWLDataProperty odp : dp.getDataPropertiesInSignature()) {
          Boolean array = true;
          Boolean nullable = true;

          Set<OWLDataPropertyRangeAxiom> ranges = ontology.getDataPropertyRangeAxioms(odp);
          String propertyName = this.sfp.getShortForm(odp.getIRI());
          String propertyURI = odp.getIRI().toString();
          propertyNameURI.put(propertyURI, propertyName);

          //obtain type using the range
          List<String> propertyRanges = getCodeGenTypesByRangeData(ranges, odp);
          MapperProperty mapperProperty = new MapperProperty(propertyName, propertyRanges, array, nullable, false);
          try {
            properties.put(mapperProperty.name, mapperProperty.getSchemaByDataProperty());
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
        //todo: set the parameters of property using ontologyProperty the information
      }
    }
    addDefaultProperties(properties);

    return properties;
  }

  /**
   * Add DefaultProperties
   * @param properties HashMap
   */
  private void addDefaultProperties(Map<String, Schema> properties) {
    List<String> defaultProperties = new ArrayList<String>(){
      {
        add("string");
      }
    };
    MapperProperty idProperty = new MapperProperty("id", defaultProperties, true, false, false);
    MapperProperty labelProperty = new MapperProperty("label", defaultProperties, true, true, false);
    MapperProperty typeProperty = new MapperProperty("type", defaultProperties, true, true, false);
    properties.put(idProperty.name, idProperty.getSchemaByDataProperty());
    properties.put(labelProperty.name, labelProperty.getSchemaByDataProperty());
    properties.put(typeProperty.name, typeProperty.getSchemaByDataProperty());
  }

  /**
   * Read the Ontology, obtain the ObjectProperties, obtain the range for each property and generate the SchemaProperty
   * @param ontology  Represents an OWL 2 ontology
   * @param cls   Represents a OWL class
   * @return A HashMap key: propertyName, value: SchemaProperty
   */
  private Map<String, Schema> getObjectProperties(OWLOntology ontology, OWLClass cls) {
    HashMap<String, String> propertyNameURI = new HashMap<>();
    Map<String, Schema> properties = new HashMap<>();
    for (OWLObjectPropertyDomainAxiom dp : ontology.getAxioms(AxiomType.OBJECT_PROPERTY_DOMAIN)) {
      if (checkDomainClass(cls, dp)) {
        for (OWLObjectProperty odp : dp.getObjectPropertiesInSignature()) {
          Boolean array = true;
          Boolean nullable = true;

          String propertyName = this.sfp.getShortForm(odp.getIRI());
          Set<OWLObjectPropertyRangeAxiom> ranges = ontology.getObjectPropertyRangeAxioms(odp);

          String propertyURI = odp.getIRI().toString();
          propertyNameURI.put(propertyURI, propertyName);

          List<String> propertyRanges = getCodeGenTypesByRangeObject(ranges, odp);
          MapperProperty mapperProperty = new MapperProperty(propertyName, propertyRanges, array, nullable, true);
          try {
            properties.put(mapperProperty.name, mapperProperty.getSchemaByObjectProperty());
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
    }
    return properties;
  }

  /**
   * Obtain SchemaPropertyType from the OWLRange of a OWLDataProperty
   * @param ranges Represents a DataPropertyRange
   * @param odp  Represents a OWLDataProperty
   * @return A list<String> with the properties
   */
  private List<String> getCodeGenTypesByRangeData(Set<OWLDataPropertyRangeAxiom> ranges, OWLDataProperty odp) {
    List<String> dataProperties = new ArrayList<>();
    for (OWLDataPropertyRangeAxiom propertyRangeAxiom : ranges) {
      for (OWLEntity rangeStr : propertyRangeAxiom.getSignature()) {
        if (!rangeStr.containsEntityInSignature(odp)) {
          String propertyName = this.sfp.getShortForm(rangeStr.getIRI());
          dataProperties.add(propertyName);
        }
      }
    }
    return dataProperties;
  }

  /**
   * Obtain SchemaPropertyType from the OWLRange of a OWLObjectProperty
   * @param ranges Represents a ObjectPropertyRange
   * @param odp  Represents a OWLObjectProperty
   * @return A list<String> with the properties
   */
  private List<String> getCodeGenTypesByRangeObject(Set<OWLObjectPropertyRangeAxiom> ranges, OWLObjectProperty odp) {
    List<String> objectProperty = new ArrayList<>();
    for (OWLObjectPropertyAxiom propertyRangeAxiom : ranges) {
      for (OWLEntity rangeClass : propertyRangeAxiom.getSignature()) {
        if (!rangeClass.containsEntityInSignature(odp)) {
          objectProperty.add(getSchemaName(rangeClass.asOWLClass()));
        }
      }

    }
    return objectProperty;
  }

}
