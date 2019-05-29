package edu.isi.oba;

import static org.junit.Assert.*;

import org.openapitools.codegen.CodegenModel;
import org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.util.SimpleIRIMapper;
import org.semanticweb.owlapi.apibinding.OWLManager;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import org.openapitools.codegen.CodegenProperty;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
/**
 * Hello world!
 */
public class App {

    public static void main(String[] args) throws Exception {

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        String ont_url = "https://mintproject.github.io/Mint-ModelCatalog-Ontology/release/0.4.0/ontology.json";
        OWLOntology ontology = manager.loadOntology(IRI.create(ont_url));
        OWLDataFactory factory = manager.getOWLDataFactory();
        DefaultPrefixManager pm = new DefaultPrefixManager(null, null, "<https://w3id.org/mint/modelCatalog#");


        Set<OWLClass> classes;
        Set<CodegenModel> models = null;

        classes = ontology.getClassesInSignature();

        System.out.println("Classes");
        System.out.println("--------------------------------");
        for (OWLClass cls : classes) {
            CodegenModel codegenModel = new CodegenModel();
            List<CodegenProperty> properties = getDataProperties(ontology, cls, factory);
            codegenModel.setAllVars(properties);
        }
    }

    private static void printDataProperties(OWLOntology ontology, OWLClass cls) {
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

    private static List<CodegenProperty> getDataProperties(OWLOntology ontology, OWLClass cls, OWLDataFactory factory) {
        List<CodegenProperty> properties = null;
        for (OWLDataPropertyDomainAxiom dp : ontology.getAxioms(AxiomType.DATA_PROPERTY_DOMAIN)) {
            if (dp.getDomain().equals(cls)) {
                CodegenProperty property = new CodegenProperty();
                for (OWLDataProperty odp : dp.getDataPropertiesInSignature()) {
                    OWLDataPropertyDomainAxiom domain = factory.getOWLDataPropertyDomainAxiom(odp, cls);
                    Set <OWLDataPropertyRangeAxiom> sgdp = ontology.getDataPropertyRangeAxioms(odp);
                    System.out.println(sgdp);
                }
                //todo: set the parameters of property using ontologyProperty the information
            }
        }
        return properties;
    }

    private static void printObjectProperties(OWLOntology ontology, OWLClass cls) {
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

/*

    ModelVersion:
      description: ""
      required:
      - id
      type: object
      properties:
        id:
          type: string
        label:
          type: string
          nullable: true
        type:
          type: array
          nullable: true
          items:
            type: string
        hasDocumentation:
          type: array
          nullable: true
          items:
            type: string
        hasVersionId:
          type: string
          nullable: true
        hasConfiguration:
          type: array
          nullable: true
          items:
            $ref: '#/components/schemas/ModelConfiguration'
        description:
          type: string
          nullable: true
      example:
        id: DSSAT_4.7
        label: DSSAT v4.7
        type:
        - http://ontosoft.org/software#SoftwareVersion
        hasVersionId: '4.7'
        hasConfiguration:
        - id: economic
 */