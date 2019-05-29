package edu.isi.oba;

import static org.junit.Assert.*;

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

        classes = ontology.getClassesInSignature();

        System.out.println("Classes");
        System.out.println("--------------------------------");
        for (OWLClass cls : classes) {
            System.out.println("+: " + cls.getIRI().getShortForm());

            printObjectProperties(ontology, cls);
            printDataProperties(ontology, cls);

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