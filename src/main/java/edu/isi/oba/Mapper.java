package edu.isi.oba;

import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenProperty;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.PrefixDocumentFormat;
import org.semanticweb.owlapi.io.OWLXMLOntologyFormat;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.vocab.PrefixOWLOntologyFormat;

import java.util.*;

public class Mapper {
    public HashMap<String, String> dataTypes;
    public OWLOntologyManager manager;
    public  DefaultPrefixManager pm;

    public Mapper(String ont_url, String ont_prefix) throws OWLOntologyCreationException {
        this.pm = new DefaultPrefixManager();
        this.manager = OWLManager.createOWLOntologyManager();
        this.dataTypes = new HashMap<>();
        OWLOntology ontology = this.manager.loadOntology(IRI.create(ont_url));
        OWLDataFactory factory = this.manager.getOWLDataFactory();
        OWLDocumentFormat format = ontology.getOWLOntologyManager().getOntologyFormat(ontology);

        this.setDataTypes();
        this.getClass(ontology, factory);
    }
    /*
    Set the dataTypes
     */
    public void setDataTypes() {
        this.dataTypes.put("xsd:string", "string");
        this.dataTypes.put("xsd:boolean", "boolean");
        this.dataTypes.put("xsd:byte", "number");
        this.dataTypes.put("xsd:decimal", "number");
        this.dataTypes.put("xsd:int", "number");
        this.dataTypes.put("xsd:integer", "number");
        this.dataTypes.put("xsd:long", "number");
        this.dataTypes.put("xsd:negativeInteger", "number");
        this.dataTypes.put("xsd:nonNegativeInteger", "number");
        this.dataTypes.put("xsd:nonPositiveInteger", "number");
        this.dataTypes.put("xsd:short", "number");
        this.dataTypes.put("xsd:unsignedLong", "number");
        this.dataTypes.put("xsd:unsignedInt", "number");
        this.dataTypes.put("xsd:unsignedShort", "number");
    }

    /**
     * Obtain OWL classes
     * @param ontology ontology
     * @param factory
     */
    public void getClass(OWLOntology ontology, OWLDataFactory factory){
        Set<OWLClass> classes;
        classes = ontology.getClassesInSignature();

        for (OWLClass cls : classes) {
            CodegenModel codegenModel = new CodegenModel();
            List<CodegenProperty> properties = this.getDataProperties(ontology, cls, factory);
            codegenModel.setAllVars(properties);
        }

    }

    private void printDataProperties(OWLOntology ontology, OWLClass cls) {
        System.out.println(" \tData Property Domain");
        for (OWLDataPropertyDomainAxiom dp : ontology.getAxioms(AxiomType.DATA_PROPERTY_DOMAIN)) {
            if (dp.getDomain().equals(cls)) {
                for (OWLDataProperty odp : dp.getDataPropertiesInSignature()) {
                     System.out.println("\t\t +: " + odp.getIRI().getShortForm());
                }
                System.out.println("\t\t +:" + dp.getProperty());
            }
        }
    }

    public CodegenProperty createCodegenProperty (String name, String type){
        CodegenProperty codegenProperty = new CodegenProperty();
        codegenProperty.setName(name);
        codegenProperty.setDatatype(type);
        return codegenProperty;
    }
    /**
     * Obtain a list of Codegenproperty using the Ontology DataProperties of a Class
     * @param ontology Ontology
     * @param cls Class
     * @param factory
     * @return
     */
    public List<CodegenProperty> getDataProperties(OWLOntology ontology, OWLClass cls, OWLDataFactory factory) {
        HashMap<String, String> dataTypes = new HashMap<String, String>();
        HashMap<String, String> propertyNameURI = new HashMap<>();
        List<CodegenProperty> properties = new ArrayList<>();
        for (OWLDataPropertyDomainAxiom dp : ontology.getAxioms(AxiomType.DATA_PROPERTY_DOMAIN)) {
            if (dp.getDomain().equals(cls)) {
                for (OWLDataProperty odp : dp.getDataPropertiesInSignature()) {
                    //OWLDataPropertyDomainAxiom domain = factory.getOWLDataPropertyDomainAxiom(odp, cls);
                    Set <OWLDataPropertyRangeAxiom> ranges = ontology.getDataPropertyRangeAxioms(odp);
                    //todo: Verify the short format
                    String propertyName = this.pm.getShortForm(odp.getIRI());
                    String propertyURI = odp.getIRI().toString();
                    propertyNameURI.put(propertyURI,propertyName);

                    //obtain type using the range
                    //todo: Verify the short format
                    List<String> propertyRanges = getCodeGenTypesByRange(ranges, odp);
                    String propertyRange = propertyRanges.get(0);

                    CodegenProperty codeProperty = createCodegenProperty(propertyName, propertyRange);
                }
                //todo: set the parameters of property using ontologyProperty the information

            }
        }
        return properties;
    }

    private List<String> getCodeGenTypesByRange(Set<OWLDataPropertyRangeAxiom> ranges, OWLDataProperty odp) {
        List<String> dataProperties = new ArrayList<>();
        DefaultPrefixManager pm = new DefaultPrefixManager(null, null, "http://owl.man.ac.uk/2005/07/sssw/people#");
        Iterator<OWLDataPropertyRangeAxiom>  itr = ranges.iterator();
        while(itr.hasNext()){
            OWLDataPropertyRangeAxiom propertyRangeAxiom = itr.next();
            for(OWLEntity rangeStr : propertyRangeAxiom.getSignature()) {
                if (!rangeStr.containsEntityInSignature(odp)) {
                    String dataProp = pm.getShortForm(rangeStr);
                    dataProperties.add(dataProp);
                }
            }

        }
        return dataProperties;
    }

    private void printObjectProperties(OWLOntology ontology, OWLClass cls) {
        System.out.println(" \tObject Property Domain");
        for (OWLObjectPropertyDomainAxiom op : ontology.getAxioms(AxiomType.OBJECT_PROPERTY_DOMAIN)) {
            if (op.getDomain().equals(cls)) {
                for (OWLObjectProperty oop : op.getObjectPropertiesInSignature()) {
                    System.out.println("\t\t +: " + oop.getIRI());
                }
            }
            System.out.println("\t\t +:" + op.getProperty());

        }
    }
}
