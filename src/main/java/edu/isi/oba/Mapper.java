package edu.isi.oba;

import io.swagger.v3.oas.models.media.Schema;
import org.eclipse.rdf4j.model.vocabulary.OWL;
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

public class Mapper {
    public OWLOntologyManager manager;
    public PrefixManager pm;
    private IRIShortFormProvider sfp = new SimpleIRIShortFormProvider();
    public OWLReasoner reasoner;

    public Mapper(String ont_url, String ont_prefix) throws OWLOntologyCreationException, IOException {
        this.manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology = this.manager.loadOntology(IRI.create(ont_url));
        OWLDocumentFormat format = manager.getOntologyFormat(ontology);

        OWLReasonerFactory reasonerFactory = new StructuralReasonerFactory();
        reasoner = reasonerFactory.createReasoner(ontology);

        if (format.isPrefixOWLDocumentFormat()) {
            this.pm = format.asPrefixOWLDocumentFormat();
        } else {
            this.pm = new DefaultPrefixManager();

        }
        this.pm.setPrefix("qudt", "http://qudt.org/schema/qudt");
        this.pm.setPrefix("onto", "http://ontosoft.org/software");
        this.pm.setPrefix("geo", "http://www.geoscienceontology.org/geo-upper");
        Map<String, String> a = this.pm.getPrefixName2PrefixMap();

        Map<String, Schema> schemas = new HashMap<>();
        OWLDataFactory factory = this.manager.getOWLDataFactory();
        schemas = this.getClasses(ontology, factory);
        Serializer serializer = new Serializer(schemas);
    }


    /**
     * Obtain Schemas using the ontology classes
     *
     * @param ontology ontology
     * @param factory
     */
    public Map<String, Schema>  getClasses(OWLOntology ontology, OWLDataFactory factory) {
        Set<OWLClass> classes;
        classes = ontology.getClassesInSignature();
        Map<String, Schema> schemas = new HashMap<>();

        for (OWLClass cls : classes) {
            System.out.println(cls.getIRI());
            String className = cls.getIRI().getShortForm();

            //todo: FIX THIS HACK. THE HACK FIXS THE DUPLICATE NAME
            Map<String, Schema> dataProperties = this.getDataProperties(ontology, cls, factory);
            Map<String, Schema> objectProperties = this.getObjectProperties(ontology, cls, factory);
            Map<String, Schema> properties = new HashMap<>();
            properties.putAll(dataProperties);
            properties.putAll(objectProperties);
            MapperSchema mapperSchema = new MapperSchema();
            schemas.put(className, mapperSchema.getSchema(className, "object", properties));
        }

        return schemas;
    }
    /**
     * Obtain the classes that related with the property's domain. 
     * @param cls
     * @param dp
     * @return
     */
    private boolean checkDomainClass(OWLClass cls, OWLPropertyDomainAxiom dp, Set<OWLClass> superClasses) {
        IRI classIRI = cls.getIRI();
        IRI iri = IRI.create("https://w3id.org/mint/modelCatalog#CAG");
        IRI iridp = IRI.create("https://w3id.org/mint/modelCatalog#hasPresentation");
        if (classIRI.equals(iri)){
            System.out.println("ok");
        }

        OWLPropertyDomainAxiom dataProperty = dp;
        Set<OWLClass> domainClasses = dataProperty.getDomain().getClassesInSignature();

        for(OWLClass domainClass : domainClasses) {
            Set<OWLClass> superDomainClasses = reasoner.getSuperClasses(cls, true).getFlattened();
            for (OWLClass superClass : superDomainClasses) {
                if (domainClass.equals(superClass) || domainClass.equals(cls)) {
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
     * @param factory
     * @return
     */
    public Map<String, Schema> getDataProperties(OWLOntology ontology, OWLClass cls, OWLDataFactory factory) {
        HashMap<String, String> propertyNameURI = new HashMap<>();
        Map<String, Schema> properties = new HashMap<String, Schema>();
        Set<OWLClass> superClasses = reasoner.getSuperClasses(cls, true).getFlattened();


        for (OWLDataPropertyDomainAxiom dp : ontology.getAxioms(AxiomType.DATA_PROPERTY_DOMAIN)) {
            if (checkDomainClass(cls, dp, superClasses)) {
                for (OWLDataProperty odp : dp.getDataPropertiesInSignature()) {
                    Boolean array = true;
                    Boolean nullable = true;

                    Set<OWLDataPropertyRangeAxiom> ranges = ontology.getDataPropertyRangeAxioms(odp);
                    String propertyName = this.sfp.getShortForm(odp.getIRI());
                    String propertyURI = odp.getIRI().toString();
                    propertyNameURI.put(propertyURI, propertyName);

                    //obtain type using the range
                    //todo: Verify the short format
                    List<String> propertyRanges = getCodeGenTypesByRangeData(ranges, odp);
                    MapperProperty mapperProperty = new MapperProperty(propertyName, propertyRanges, array, nullable, false);
                    try {
                        properties.put(mapperProperty.name, mapperProperty.getSchemaByDataProperty());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    System.out.println(mapperProperty.name);
                }
                //todo: set the parameters of property using ontologyProperty the information

            }
        }
        return properties;
    }


    public Map<String, Schema> getObjectProperties(OWLOntology ontology, OWLClass cls, OWLDataFactory factory) {
        HashMap<String, String> propertyNameURI = new HashMap<>();
        Map<String, Schema> properties = new HashMap<String, Schema>();
        Set<OWLClass> superClasses = reasoner.getSuperClasses(cls, true).getFlattened();

        for (OWLObjectPropertyDomainAxiom dp : ontology.getAxioms(AxiomType.OBJECT_PROPERTY_DOMAIN)) {
            if (checkDomainClass(cls, dp, superClasses)) {
                IRI iri = IRI.create("https://w3id.org/mint/modelCatalog#CAG");
                if (cls.equals(iri)){
                    System.out.println("ok");
                }

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
                    System.out.println(mapperProperty.name);

                }
            }
        }
        return properties;
    }

    private List<String> getCodeGenTypesByRangeData(Set<OWLDataPropertyRangeAxiom> ranges, OWLDataProperty odp) {
        List<String> dataProperties = new ArrayList<>();
        DefaultPrefixManager pm = new DefaultPrefixManager(null, null, "http://owl.man.ac.uk/2005/07/sssw/people#");
        Iterator<OWLDataPropertyRangeAxiom> itr = ranges.iterator();
        while (itr.hasNext()) {
            OWLDataPropertyRangeAxiom propertyRangeAxiom = itr.next();
            for (OWLEntity rangeStr : propertyRangeAxiom.getSignature()) {
                if (!rangeStr.containsEntityInSignature(odp)) {
                    String dataProp = pm.getShortForm(rangeStr);
                    dataProperties.add(dataProp);
                }
            }

        }
        return dataProperties;
    }


    private List<String> getCodeGenTypesByRangeObject(Set<OWLObjectPropertyRangeAxiom> ranges, OWLObjectProperty odp) {
        List<String> objectProperty = new ArrayList<>();
        Iterator<OWLObjectPropertyRangeAxiom> itr = ranges.iterator();
        while (itr.hasNext()) {
            OWLObjectPropertyAxiom propertyRangeAxiom = itr.next();
            for (OWLEntity rangeStr : propertyRangeAxiom.getSignature()) {
                if (!rangeStr.containsEntityInSignature(odp)) {
                    String dataProp = this.sfp.getShortForm(rangeStr.getIRI());
                    objectProperty.add(dataProp);
                }
            }

        }
        return objectProperty;
    }

}
