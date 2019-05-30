package edu.isi.oba;

import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenProperty;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

import java.util.*;

public class Mapper {
    public HashMap<String, String> dataTypes;
    public OWLOntologyManager manager;

    public Mapper(String ont_url, String ont_prefix) throws OWLOntologyCreationException {
        this.manager = OWLManager.createOWLOntologyManager();
        this.dataTypes = new HashMap<>();
        OWLOntology ontology = manager.loadOntology(IRI.create(ont_url));
        OWLDataFactory factory = manager.getOWLDataFactory();
        DefaultPrefixManager pm = new DefaultPrefixManager(null, null, ont_prefix);
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

    /**
     * Obtain a list of Codegenproperty using the Ontology DataProperties of a Class
     * @param ontology Ontology
     * @param cls Class
     * @param factory
     * @return
     */
    public List<CodegenProperty> getDataProperties(OWLOntology ontology, OWLClass cls, OWLDataFactory factory) {
        HashMap<String, String> dataTypes = new HashMap<String, String>();
        List<CodegenProperty> properties = new ArrayList<>();
        for (OWLDataPropertyDomainAxiom dp : ontology.getAxioms(AxiomType.DATA_PROPERTY_DOMAIN)) {
            if (dp.getDomain().equals(cls)) {
                CodegenProperty property = new CodegenProperty();
                for (OWLDataProperty odp : dp.getDataPropertiesInSignature()) {
                    OWLDataPropertyDomainAxiom domain = factory.getOWLDataPropertyDomainAxiom(odp, cls);
                    Set <OWLDataPropertyRangeAxiom> ranges = ontology.getDataPropertyRangeAxioms(odp);
                    CodegenProperty codegenProperty = new CodegenProperty();
                    getCodeGenTypesByRange(ranges, odp);
                    List<String> types = getCodeGenTypesByRange(ranges, odp);
                    String type = types.get(0);
                    //todo: obtain type using the range

                    codegenProperty.setName(name);
                    codegenProperty.setBaseName(type);
                }
                //todo: set the parameters of property using ontologyProperty the information


                properties.add(property);
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
