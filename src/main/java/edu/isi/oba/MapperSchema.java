package edu.isi.oba;


import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.semanticweb.owlapi.util.IRIShortFormProvider;
import org.semanticweb.owlapi.util.SimpleIRIShortFormProvider;

import java.util.*;

import static edu.isi.oba.Oba.logger;

class MapperSchema {

    private final OWLReasoner reasoner;
    private final IRIShortFormProvider sfp = new SimpleIRIShortFormProvider();
    private final String type;
    private final OWLClass cls;
    private final String cls_description;
    private final List<OWLOntology> ontologies;
    private Map<String, Schema> dataProperties;
    private Map<String, Schema> objectProperties;
    private Map<String, Schema> properties;
    final String name;
    private final Map<IRI, String> schemaNames;
    private final Schema schema;
    private OWLOntology ontology_cls;
    private OWLReasonerFactory reasonerFactory;
    public List<OWLClass> properties_range;
    private boolean follow_references;

    public List<OWLObjectProperty> propertiesFromObjectRestrictions;
    public Map<String, List<String>> propertiesFromObjectRestrictions_ranges;
    public String complementOf;

    public List<OWLClass> getProperties_range() {
        return properties_range;
    }

    public Schema getSchema() {
        return schema;
    }

    public MapperSchema(List<OWLOntology> ontologies, OWLClass cls, String clsDescription, Map<IRI, String> schemaNames, OWLOntology class_ontology, Boolean follow_references) {
        this.schemaNames = schemaNames;
        this.follow_references = follow_references;
        this.cls = cls;
        this.cls_description = clsDescription;
        this.type = "object";
        this.ontologies = ontologies;
        this.ontology_cls = class_ontology;
        reasonerFactory = new StructuralReasonerFactory();
        this.reasoner = reasonerFactory.createReasoner(this.ontology_cls);

        properties_range = new ArrayList<>();

        propertiesFromObjectRestrictions_ranges= new HashMap<>();
        propertiesFromObjectRestrictions = new ArrayList<>();
        properties = new HashMap<>();
        this.complementOf="";
        this.getClassRestrictions(cls);

        this.name = getSchemaName(cls);
       // this.properties = setProperties();
        this.schema = setSchema();
    }

    private Map<String, Schema> setProperties() {
        dataProperties = this.getDataProperties();
        objectProperties = this.getObjectProperties();
       // properties = new HashMap<>();
       // properties.putAll(dataProperties);
      //  properties.putAll(objectProperties);
        return properties;
    }

    private Schema setSchema() {
        Schema schema = new Schema();
        schema.setName(this.name);
        schema.setDescription(this.cls_description);
        schema.setType(this.type);
        // if the Schema is the complement of other Schema
        if (complementOf!="") {
        	Schema complement = new ObjectSchema();
            complement.set$ref(complementOf);
        	schema.not(complement);
        }

        schema.setProperties(this.getProperties());
        HashMap<String, String> exampleMap = new HashMap<>();
        exampleMap.put("id", "some_id");
        Example example = new Example();
        example.setValue(exampleMap);
        schema.setExample(example);
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
     * @return A HashMap key: property name, value: SchemaProperty
     */
    private Map<String, Schema> getDataProperties() {
        HashMap<String, String> propertyNameURI = new HashMap<>();
        Map<String, Schema> properties = new HashMap<>();
        Set<OWLDataPropertyDomainAxiom> properties_class = new HashSet<>();
        Set<OWLFunctionalDataPropertyAxiom> functional;
        for (OWLOntology ontology : ontologies)
            properties_class.addAll(ontology.getAxioms(AxiomType.DATA_PROPERTY_DOMAIN));

        for (OWLDataPropertyDomainAxiom dp : properties_class) {
            if (checkDomainClass(cls, dp)) {
                for (OWLDataProperty odp : dp.getDataPropertiesInSignature()) {
                    Boolean array = true;
                    Boolean nullable = true;
                    Set<OWLDataPropertyRangeAxiom> ranges = new HashSet<>();

                    Boolean isFunctional=false;
                    for (OWLOntology ontology : ontologies) {
                        ranges.addAll(ontology.getDataPropertyRangeAxioms(odp));

                        functional = ontology.getAxioms(AxiomType.FUNCTIONAL_DATA_PROPERTY);
                        for (OWLFunctionalDataPropertyAxiom functionalAxiom:functional) {
                        	if (functionalAxiom.getProperty().equals(odp))
                        		isFunctional = true;
                        }
                    }
                    if (ranges.size() == 0)
                        logger.warning("Property " + odp.getIRI() + " has range equals zero");

                    String propertyName = this.sfp.getShortForm(odp.getIRI());
                    String propertyURI = odp.getIRI().toString();
                    propertyNameURI.put(propertyURI, propertyName);

                    //obtain type using the range
                    List<String> propertyRanges = getCodeGenTypesByRangeData(ranges, odp);
                    String propertyDescription = ObaUtils.getDescription(odp, ontology_cls);
                    MapperDataProperty mapperProperty = new MapperDataProperty(propertyName, propertyDescription, isFunctional, propertyRanges, array, nullable);
                    try {
                    	this.properties.put(mapperProperty.name, mapperProperty.getSchemaByDataProperty());
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
        MapperDataProperty idProperty = new MapperDataProperty("id", "identifier", true, defaultProperties, false, false);
        MapperDataProperty labelProperty = new MapperDataProperty("label", "short description of the resource", false, defaultProperties, true, true);
        MapperDataProperty typeProperty = new MapperDataProperty("type", "type of the resource", false, defaultProperties, true, true);
        MapperDataProperty descriptionProperty = new MapperDataProperty("description", "small description", false, defaultProperties, true, true);

        properties.put(idProperty.name, idProperty.getSchemaByDataProperty());
        properties.put(labelProperty.name, labelProperty.getSchemaByDataProperty());
        properties.put(typeProperty.name, typeProperty.getSchemaByDataProperty());
        properties.put(descriptionProperty.name, descriptionProperty.getSchemaByDataProperty());

    }

    /**
     * Read the Ontology, obtain the ObjectProperties, obtain the range for each property and generate the SchemaProperty
     * @return A HashMap key: propertyName, value: SchemaProperty
     */
    private Map<String, Schema> getObjectProperties() {
        OWLOntologyManager m = OWLManager.createOWLOntologyManager();
        OWLDataFactory dataFactory = m.getOWLDataFactory();
        OWLClass owlThing = dataFactory.getOWLThing();

        Set<OWLObjectPropertyDomainAxiom> properties_class = new HashSet<>();
        Set<OWLFunctionalObjectPropertyAxiom> functional;
        for (OWLOntology ontology : ontologies)
            properties_class.addAll(ontology.getAxioms(AxiomType.OBJECT_PROPERTY_DOMAIN));

        HashMap<String, String> propertyNameURI = new HashMap<>();
        Map<String, Schema> properties = new HashMap<>();
        logger.info("Parsing class " + cls.toString());

        for (OWLObjectPropertyDomainAxiom dp : properties_class) {
        	if (checkDomainClass(cls, dp)) {
        		logger.info( "Parsing property " + dp.toString());
        		for (OWLObjectProperty odp : dp.getObjectPropertiesInSignature()) {
        			String propertyName = this.sfp.getShortForm(odp.getIRI());

        			Boolean inspect = true;
        			// If there are property restrictions from the Class we need to check if
        			// the object property has been previously analyzed on the getClassRestrictions method.
        			// If the property was analyzed, we will change the value of inspect to false, otherwise
        			// the property will be inspected.
        			if (propertiesFromObjectRestrictions.size() != 0) {
        				if (propertiesFromObjectRestrictions.contains(odp)) {
        					inspect=false;
        				}

        			}
        			if (inspect) {

        				Boolean isFunctional=false;
        				Set<OWLObjectPropertyRangeAxiom> ranges = new HashSet<>();
        				for (OWLOntology ontology : ontologies) {
        					ranges.addAll(ontology.getObjectPropertyRangeAxioms(odp));

        					functional = ontology.getAxioms(AxiomType.FUNCTIONAL_OBJECT_PROPERTY);
        					for (OWLFunctionalObjectPropertyAxiom functionalAxiom:functional) {
        						if (functionalAxiom.getProperty().equals(odp)) {
        							isFunctional = true;
        						}
        					}
        				}
        				if (ranges.size() == 0)
        					logger.warning("Property " + odp.getIRI() + " has range equals zero");

        				String propertyURI = odp.getIRI().toString();
        				propertyNameURI.put(propertyURI, propertyName);
        				List<String> propertyRanges = getCodeGenTypesByRangeObject(ranges, odp, owlThing, follow_references);

        				Map<String,String> restrictionValues = new HashMap<String, String>() ;
        				for (OWLOntology ontology : ontologies) {
        					RestrictionVisitor restrictionVisitor = new RestrictionVisitor(cls,ontology,owlThing,propertyName);
        					for (OWLObjectPropertyRangeAxiom propertyRangeAxiom : ranges) {
        						OWLClassExpression ce = propertyRangeAxiom.getRange();
        						ce.accept(restrictionVisitor);
        					}
        					Map<String, Map<String,String>> restrictionsValuesFromClass = restrictionVisitor.getRestrictionsValuesFromClass();
        					for (String j :  restrictionsValuesFromClass.keySet()) {
        						if (j==propertyName) {
        							restrictionValues=restrictionsValuesFromClass.get(j);
        						}
        					}
        					if (restrictionsValuesFromClass.isEmpty() && propertyRanges.size()>1)
                        		propertyRanges.clear();
        				}

        				String propertyDescription = ObaUtils.getDescription(odp, ontology_cls);
        				MapperObjectProperty mapperObjectProperty = new MapperObjectProperty(propertyName, propertyDescription, isFunctional, restrictionValues, propertyRanges);
        				try {
        					this.properties.put(mapperObjectProperty.name, mapperObjectProperty.getSchemaByObjectProperty());
        				} catch (Exception e) {
        					e.printStackTrace();
        				}
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
     * @param follow_references
     * @return A list<String> with the properties
     */
    private List<String> getCodeGenTypesByRangeObject(Set<OWLObjectPropertyRangeAxiom> ranges, OWLObjectProperty odp, OWLClass owlThing, boolean follow_references) {
        List<String> objectProperty = new ArrayList<>();

        for (OWLObjectPropertyRangeAxiom propertyRangeAxiom : ranges) {
    		for (OWLEntity rangeClass : propertyRangeAxiom.getSignature()) {
    			if (rangeClass instanceof OWLClassExpression) {
    				if (!rangeClass.containsEntityInSignature(odp)) {
    					if (rangeClass.asOWLClass().equals(owlThing)) {
    						logger.info("Ignoring owl:Thing" + odp);
    					}
    					else {
    						this.properties_range.add(rangeClass.asOWLClass());
    						if (follow_references)
        						objectProperty.add(getSchemaName(rangeClass.asOWLClass()));
    					}
    				}

    			}
    		}

    	}
    	return objectProperty;
    }

    /**
     * Read the Ontology and gets all the Class restrictions on object or data properties and
     * generate SchemaProperties.
     * @param clas Class that will be analyzed in order to get its restrictions
     */
    private void getClassRestrictions(OWLClass clas){
    	OWLOntologyManager m = OWLManager.createOWLOntologyManager();
    	OWLDataFactory dataFactory = m.getOWLDataFactory();
    	OWLClass owlThing = dataFactory.getOWLThing();
    	RestrictionVisitor restrictionVisitor;

    	for (OWLOntology ontology : ontologies) {
    		restrictionVisitor = new RestrictionVisitor(clas,ontology,owlThing, "");
    		for (OWLSubClassOfAxiom ax : ontology.getSubClassAxiomsForSubClass(clas)) {
    			OWLClassExpression superCls = ax.getSuperClass();
    			// Ask our superclass to accept a visit from the RestrictionVisitor
    			// - e.g. if it is an existential restiction then the restriction visitor
    			// will answer it - if not the visitor will ignore it
    			superCls.accept(restrictionVisitor);
    		}
    		propertiesFromObjectRestrictions = restrictionVisitor.getPropertiesFromObjectRestrictions();
			propertiesFromObjectRestrictions_ranges  = restrictionVisitor.getPropertiesFromObjectRestrictions_ranges();

			Map<String, Map<String,String>> restrictionsValuesFromClass = restrictionVisitor.getRestrictionsValuesFromClass();

			if (restrictionsValuesFromClass.size()!=0) {
    			// When the restriction is a ObjectComplementOf it doesn't have a object property,
				// thus we need to set its value at the setSchema function
    			if (restrictionsValuesFromClass.containsKey("complementOf") && restrictionsValuesFromClass.size()==1) {
    				for (String j :  restrictionsValuesFromClass.keySet()) {
						Map<String,String> restrictionValues=restrictionsValuesFromClass.get(j);
						for (String restriction:  restrictionValues.keySet()) {
					    	  complementOf = restrictionValues.get(restriction);
						}
					}

    			} else {

    			for (int i = 0; i < propertiesFromObjectRestrictions.size(); i++) {
    				MapperObjectProperty mapperObjectProperty;
    				OWLObjectProperty OP = propertiesFromObjectRestrictions.get(i);
    				String propertyDescription = ObaUtils.getDescription(OP, ontology_cls);
    				if (propertiesFromObjectRestrictions_ranges.size() != 0) {
    					List<String> rangesOP = propertiesFromObjectRestrictions_ranges.get(sfp.getShortForm(OP.getIRI()));
    					for (String j :  restrictionsValuesFromClass.keySet()) {
    						Map<String,String> restrictionValues=restrictionsValuesFromClass.get(j);
    						if (j==sfp.getShortForm(OP.getIRI())) {
    							if (rangesOP.get(0)=="defaultValue")
    								mapperObjectProperty = new MapperObjectProperty(sfp.getShortForm(OP.getIRI()), propertyDescription, false, restrictionValues, rangesOP, false, true);
    							else
    								mapperObjectProperty = new MapperObjectProperty(sfp.getShortForm(OP.getIRI()), propertyDescription, false, restrictionValues, rangesOP);
    							try {
    								this.properties.put(mapperObjectProperty.name, mapperObjectProperty.getSchemaByObjectProperty());
    							} catch (Exception e) {
    								e.printStackTrace();
    							}
    						}
    					}
    				}
    			}
    			}
    			this.properties = setProperties();
    		}
    		else {
    			this.properties = setProperties();
    		}
    	}
    	addDefaultProperties(this.properties);
    }




    private Map<String, Schema> getProperties() {
        return properties;
    }

    private String getSchemaName(OWLClass cls) {
        return schemaNames.get(cls.getIRI());
    }

    public OWLClass getCls() {
        return cls;
    }


}
