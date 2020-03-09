package edu.isi.oba;

import io.swagger.v3.oas.models.media.Schema;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.semanticweb.owlapi.util.IRIShortFormProvider;
import org.semanticweb.owlapi.util.OWLClassLiteralCollector;
import org.semanticweb.owlapi.util.SimpleIRIShortFormProvider;
import org.semanticweb.owlapi.model.OWLDataFactory;
import java.util.*;
import static edu.isi.oba.Oba.logger;

class MapperSchema {

    private final OWLReasoner reasoner;
    private final IRIShortFormProvider sfp = new SimpleIRIShortFormProvider();
    private Map<String, Schema> dataProperties;
    private Map<String, Schema> objectProperties;
    private Map<String, Schema> properties;
    final String name;
    private final String type;
    private final Map<IRI, String> schemaNames;
    private final Schema schema;


    public Schema getSchema() {
        return schema;
    }

    public MapperSchema(OWLOntology ontology, OWLClass cls, String type, Map<IRI, String> schemaNames) {
        OWLReasonerFactory reasonerFactory = new StructuralReasonerFactory();
        reasoner = reasonerFactory.createReasoner(ontology);

        this.type = type;
        this.schemaNames = schemaNames;
        this.name = getSchemaName(cls);
        this.properties = setProperties(ontology, cls);
        this.schema = setSchema();

    }

    private Map<String, Schema> setProperties(OWLOntology ontology, OWLClass cls) {
        dataProperties = this.getDataProperties(ontology, cls);
        objectProperties = this.getObjectProperties(ontology, cls);
        properties = new HashMap<>();
        properties.putAll(dataProperties);
        properties.putAll(objectProperties);
        return properties;
    }

    private Schema setSchema() {
        Schema schema = new Schema();
        schema.setName(this.name);
        schema.setType(this.type);
        schema.setProperties(this.getProperties());
        return schema;
    }

    private List<String> required() {
        return new ArrayList<String>() {{
            //add("id");
        }};
    }

    /**
     * Check if the class cls is domain of the property dp
     *
     * @param cls class
     * @param dp PropertyDomain
     * @return true or false
     */
    private boolean checkDomainClass(OWLClass cls, OWLPropertyDomainAxiom dp) {
        Set<OWLClass> superDomainClasses = reasoner.getSuperClasses(cls, false).getFlattened();
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
        Set<OWLDataPropertyDomainAxiom> properties_class = ontology.getAxioms(AxiomType.DATA_PROPERTY_DOMAIN);
        for (OWLDataPropertyDomainAxiom dp : properties_class) {
            if (checkDomainClass(cls, dp)) {
                for (OWLDataProperty odp : dp.getDataPropertiesInSignature()) {
                    Boolean array = true;
                    Boolean nullable = true;

                    Set<OWLDataPropertyRangeAxiom> ranges = ontology.getDataPropertyRangeAxioms(odp);
                    if (ranges.size() == 0)
                        logger.info(odp.getIRI() + " range 0");
                    String propertyName = this.sfp.getShortForm(odp.getIRI());
                    String propertyURI = odp.getIRI().toString();
                    propertyNameURI.put(propertyURI, propertyName);

                    //obtain type using the range
                    List<String> propertyRanges = getCodeGenTypesByRangeData(ranges, odp);
                    MapperDataProperty mapperProperty = new MapperDataProperty(propertyName, propertyRanges, array, nullable);
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
        MapperDataProperty idProperty = new MapperDataProperty("id", defaultProperties, false, false);
        MapperDataProperty labelProperty = new MapperDataProperty("label", defaultProperties, true, true);
        MapperDataProperty typeProperty = new MapperDataProperty("type", defaultProperties, true, true);
        MapperDataProperty descriptionProperty = new MapperDataProperty("description", defaultProperties, true, true);

        properties.put(idProperty.name, idProperty.getSchemaByDataProperty());
        properties.put(labelProperty.name, labelProperty.getSchemaByDataProperty());
        properties.put(typeProperty.name, typeProperty.getSchemaByDataProperty());
        properties.put(descriptionProperty.name, descriptionProperty.getSchemaByDataProperty());

    }

    /**
     * Read the Ontology, obtain the ObjectProperties, obtain the range for each property and generate the SchemaProperty
     * @param ontology  Represents an OWL 2 ontology
     * @param cls   Represents a OWL class
     * @return A HashMap key: propertyName, value: SchemaProperty
     */
    private Map<String, Schema> getObjectProperties(OWLOntology ontology, OWLClass cls) {
        OWLOntologyManager m = OWLManager.createOWLOntologyManager();
        OWLDataFactory dataFactory = m.getOWLDataFactory();
        OWLClass owlThing = dataFactory.getOWLThing();

        HashMap<String, String> propertyNameURI = new HashMap<>();
        Map<String, Schema> properties = new HashMap<>();
        for (OWLObjectPropertyDomainAxiom dp : ontology.getAxioms(AxiomType.OBJECT_PROPERTY_DOMAIN)) {
            if (checkDomainClass(cls, dp)) {
                for (OWLObjectProperty odp : dp.getObjectPropertiesInSignature()) {
                    String propertyName = this.sfp.getShortForm(odp.getIRI());
                    Set<OWLObjectPropertyRangeAxiom> ranges = ontology.getObjectPropertyRangeAxioms(odp);
                    if (ranges.size() == 0)
                        logger.info(odp.getIRI() + " range 0");

                    String propertyURI = odp.getIRI().toString();
                    propertyNameURI.put(propertyURI, propertyName);

                    List<String> propertyRanges = getCodeGenTypesByRangeObject(ranges, odp, owlThing);
                    MapperObjectProperty mapperObjectProperty = new MapperObjectProperty(propertyName, propertyRanges);
                    try {
                        properties.put(mapperObjectProperty.name, mapperObjectProperty.getSchemaByObjectProperty());
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
     * @param owlThing
     * @return A list<String> with the properties
     */
    private List<String> getCodeGenTypesByRangeObject(Set<OWLObjectPropertyRangeAxiom> ranges, OWLObjectProperty odp, OWLClass owlThing) {
        List<String> objectProperty = new ArrayList<>();


        for (OWLObjectPropertyAxiom propertyRangeAxiom : ranges) {
            for (OWLEntity rangeClass : propertyRangeAxiom.getSignature()) {
                 if (!rangeClass.containsEntityInSignature(odp)) {
                    if (rangeClass.asOWLClass().equals(owlThing)) {
                        logger.info("Ignoring owl:Thing" + odp);
                    }
                    else {
                        objectProperty.add(getSchemaName(rangeClass.asOWLClass()));
                    }
                }
            }

        }
        return objectProperty;
    }


    private Map<String, Schema> getProperties() {
        return properties;
    }

    private String getSchemaName(OWLClass cls) {
        return schemaNames.get(cls.getIRI());
    }
}
