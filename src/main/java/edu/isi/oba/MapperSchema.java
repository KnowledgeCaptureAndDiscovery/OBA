package edu.isi.oba;

import edu.isi.oba.config.CONFIG_FLAG;
import static edu.isi.oba.Oba.logger;

import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;

import java.util.*;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.semanticweb.owlapi.util.IRIShortFormProvider;
import org.semanticweb.owlapi.util.SimpleIRIShortFormProvider;

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
	private List<String> required_properties;
	private Map<IRI, List<String>> enums;
    final String name;
    private final Map<IRI, String> schemaNames;
    private final Schema schema;
    private OWLOntology ontology_cls;
    private OWLReasonerFactory reasonerFactory;
    public List<OWLClass> properties_range;

	private final Map<CONFIG_FLAG, Boolean> configFlags = new HashMap<>();

    public List<OWLObjectProperty> propertiesFromObjectRestrictions;
    public Map<String, List<String>> propertiesFromObjectRestrictions_ranges;
    public String complementOf;
    public List<OWLDataProperty> propertiesFromDataRestrictions;
    public Map<String, List<String>> propertiesFromDataRestrictions_ranges;

    public List<OWLClass> getProperties_range() {
        return this.properties_range;
    }

    public List<String> getPropertiesFromObjectRestrictions_ranges() {
    	List<String> aggregatedClasses = new ArrayList<>();
    	for (List<String> l: this.propertiesFromObjectRestrictions_ranges.values()) {
    		aggregatedClasses.addAll(l);
		}

    	return aggregatedClasses;
	}

    public Schema getSchema() {
        return this.schema;
    }

    public MapperSchema(List<OWLOntology> ontologies, OWLClass cls, String clsDescription, Map<IRI, String> schemaNames, OWLOntology class_ontology, Map<CONFIG_FLAG, Boolean> configFlags) {
        this.schemaNames = schemaNames;
		this.configFlags.putAll(configFlags);
        this.cls = cls;
        this.cls_description = clsDescription;
        this.type = "object";
        this.ontologies = ontologies;
        this.ontology_cls = class_ontology;
        this.reasonerFactory = new StructuralReasonerFactory();
        this.reasoner = reasonerFactory.createReasoner(this.ontology_cls);
        this.properties_range = new ArrayList<>();
        this.propertiesFromObjectRestrictions_ranges = new HashMap<>();
        this.propertiesFromObjectRestrictions = new ArrayList<>();
        this.propertiesFromDataRestrictions_ranges = new HashMap<>();
        this.propertiesFromDataRestrictions = new ArrayList<>();
        this.properties = new HashMap<>();
		this.required_properties = new ArrayList<>();
        this.complementOf = "";
        this.getClassRestrictions(cls);
        this.name = getSchemaName(cls);
        this.schema = setSchema();
    }

    private Map<String, Schema> setProperties() {
        this.dataProperties = this.getDataProperties();
        this.objectProperties = this.getObjectProperties();
        return this.properties;
    }

    private Schema setSchema() {
        Schema schema = new Schema();
        schema.setName(this.name);
        schema.setDescription(this.cls_description);

		if (this.enums.containsKey(this.cls.getIRI())) {
			// Only string enums allowed in RDF/OWL ??
			schema.setType("string");
			schema.setEnum(this.enums.get(this.cls.getIRI()));
		} else {
			// if the Schema is the complement of other Schema
			if (complementOf!="") {
				Schema complement = new ObjectSchema();
				complement.set$ref(complementOf);
				schema.not(complement);
			}
			schema.setType(this.type);
			schema.setProperties(this.getProperties());

			if (this.configFlags.containsKey(CONFIG_FLAG.REQUIRED_PROPERTIES_FROM_CARDINALITY) 
				&& this.configFlags.get(CONFIG_FLAG.REQUIRED_PROPERTIES_FROM_CARDINALITY)) {
				schema.setRequired(this.required_properties);
			}

			HashMap<String, String> exampleMap = new HashMap<>();
			exampleMap.put("id", "some_id");
			Example example = new Example();
			example.setValue(exampleMap);
			schema.setExample(example);
			ArrayList<Example> examples = new ArrayList<Example>();
			examples.add(example);
			schema.setExamples(examples);
		}

        return schema;
    }

    /**
     * Check if the class cls is domain of the property dp
     *
     * @param cls class
     * @param dp PropertyDomain
     * @return true or false
     */
    private boolean checkDomainClass(OWLClass cls, OWLPropertyDomainAxiom dp) {
        Set<OWLClass> superDomainClasses = this.reasoner.getSuperClasses(cls, false).getFlattened();
        Set<OWLClass> domainClasses = dp.getDomain().getClassesInSignature();
        for (OWLClass domainClass : domainClasses) {
            if (domainClass.equals(cls)) {
				return true;
			}

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
    	OWLOntologyManager m = OWLManager.createOWLOntologyManager();
    	OWLDataFactory dataFactory = m.getOWLDataFactory();
    	OWLClass owlThing = dataFactory.getOWLThing();
    	HashMap<String, String> propertyNameURI = new HashMap<>();
    	Map<String, Schema> properties = new HashMap<>();
    	Set<OWLDataPropertyDomainAxiom> properties_class = new HashSet<>();
    	Set<OWLFunctionalDataPropertyAxiom> functional;

    	for (OWLOntology ontology: this.ontologies) {
    		properties_class.addAll(ontology.getAxioms(AxiomType.DATA_PROPERTY_DOMAIN));
		}

    	for (OWLDataPropertyDomainAxiom dp : properties_class) {
    		if (checkDomainClass(this.cls, dp)) {
    			for (OWLDataProperty odp : dp.getDataPropertiesInSignature()) {
    				Boolean array = true;
    				Boolean nullable = true;
    				Set<OWLDataPropertyRangeAxiom> ranges = new HashSet<>();

    				Boolean inspect = true;
    				// If there are property restrictions from the Class we need to check if 
    				// the data property has been previously analyzed on the getClassRestrictions function.
    				// If the property was analyzed, we will change the value of inspect to false, otherwise 
    				// the property will be inspected.
    				if (!propertiesFromDataRestrictions.isEmpty()) {
    					if (propertiesFromDataRestrictions.contains(odp)) {           				
    						inspect = false;
    					}    
    				}

    				if (inspect) {
						boolean isFunctional = EntitySearcher.isFunctional(odp, this.ontologies.stream());

    					for (OWLOntology ontology: this.ontologies) {
    						ranges.addAll(ontology.getDataPropertyRangeAxioms(odp));
    					}

    					if (ranges.isEmpty()) {
    						logger.warning("Property " + odp.getIRI() + " has range equals zero");
						}

    					String propertyName = this.sfp.getShortForm(odp.getIRI());
    					String propertyURI = odp.getIRI().toString();
    					propertyNameURI.put(propertyURI, propertyName);

    					//obtain type using the range
    					List<String> valuesFromDataRestrictions_ranges = new ArrayList<String>();
    					Map<String,String> restrictionValues = new HashMap<String, String>();
    					for (OWLOntology ontology: this.ontologies) {
    						RestrictionVisitor restrictionVisitor = new RestrictionVisitor(this.cls, ontology, owlThing, propertyName);
    						for (OWLDataPropertyRangeAxiom propertyRangeAxiom : ranges) {
    							OWLDataRange ce = propertyRangeAxiom.getRange();
    							ce.accept(restrictionVisitor);
    							if (ce instanceof OWLDataOneOf) {   							
    								valuesFromDataRestrictions_ranges  = restrictionVisitor.getValuesFromDataRestrictions_ranges();
    							}    							
    						}
    						Map<String, Map<String,String>> restrictionsValuesFromClass = restrictionVisitor.getRestrictionsValuesFromClass();
    						for (String j :  restrictionsValuesFromClass.keySet()) {     						
    							if (j.equals(propertyName)) {
    								restrictionValues = restrictionsValuesFromClass.get(j);
    							}
    						}          		    						
    					}

						List<String> propertyRanges = getCodeGenTypesByRangeData(ranges, odp);
    					String propertyDescription = ObaUtils.getDescription(odp, this.ontology_cls, this.configFlags.get(CONFIG_FLAG.DEFAULT_DESCRIPTIONS));
    					MapperDataProperty mapperProperty = new MapperDataProperty(propertyName, propertyDescription, isFunctional, restrictionValues, valuesFromDataRestrictions_ranges, propertyRanges, array, nullable);
    					try {
    						this.properties.put(mapperProperty.name, mapperProperty.getSchemaByDataProperty());
    					} catch (Exception e) {
    						e.printStackTrace();
    					}
    				}                
    			}
    		}
    	}

		if (this.configFlags.get(CONFIG_FLAG.DEFAULT_DESCRIPTIONS)) {
			properties.putAll(this.getDefaultProperties());
		}
    	
    	return properties;
    }

    /**
     * Get default schema properties.
	 * 
	 * These can be disabled by setting `default_properties` to `false` in the `config.yaml` file.
	 * 
	 * @return A HashMap key: propertyName, value: Schema of data property
     */
    private Map<String, Schema> getDefaultProperties() {
        Map<String,String> defaultRestrictionValues = new HashMap<String, String>();
        List<String> valuesFromDataRestrictions_ranges = new ArrayList<String>();
        
		// Add some typical default properties (e.g. id, lable, type, and description)
        MapperDataProperty idProperty = new MapperDataProperty("id", "identifier", true, defaultRestrictionValues, valuesFromDataRestrictions_ranges, new ArrayList<String>(){{add("integer");}}, false, false);
        MapperDataProperty labelProperty = new MapperDataProperty("label", "short description of the resource", false, defaultRestrictionValues, valuesFromDataRestrictions_ranges, new ArrayList<String>(){{add("string");}}, false, true);
        MapperDataProperty typeProperty = new MapperDataProperty("type", "type(s) of the resource", false, defaultRestrictionValues, valuesFromDataRestrictions_ranges, new ArrayList<String>(){{add("string");}}, true, true);
		MapperDataProperty descriptionProperty = new MapperDataProperty("description", "small description", false, defaultRestrictionValues, valuesFromDataRestrictions_ranges, new ArrayList<String>(){{add("string");}}, false, true);
		
		// Also add some default property examples of different types (e.g. a date/time, a boolean, and a float)
		MapperDataProperty eventDateTimeProperty = new MapperDataProperty("eventDateTime", "a date/time of the resource", false, defaultRestrictionValues, valuesFromDataRestrictions_ranges, new ArrayList<String>(){{add("dateTime");}}, false, true);
		MapperDataProperty isBoolProperty = new MapperDataProperty("isBool", "a boolean indicator of the resource", false, defaultRestrictionValues, valuesFromDataRestrictions_ranges, new ArrayList<String>(){{add("boolean");}}, false, true);
		MapperDataProperty quantityProperty = new MapperDataProperty("quantity", "a number quantity of the resource", false, defaultRestrictionValues, valuesFromDataRestrictions_ranges, new ArrayList<String>(){{add("float");}}, false, true);

		return Map.ofEntries(
			Map.entry(idProperty.name, idProperty.getSchemaByDataProperty()),
			Map.entry(labelProperty.name, labelProperty.getSchemaByDataProperty()),
			Map.entry(typeProperty.name, typeProperty.getSchemaByDataProperty()),
			Map.entry(descriptionProperty.name, descriptionProperty.getSchemaByDataProperty()),
			Map.entry(eventDateTimeProperty.name, eventDateTimeProperty.getSchemaByDataProperty()),
			Map.entry(isBoolProperty.name, isBoolProperty.getSchemaByDataProperty()),
			Map.entry(quantityProperty.name, quantityProperty.getSchemaByDataProperty())
		);
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
        for (OWLOntology ontology: this.ontologies) {
            properties_class.addAll(ontology.getAxioms(AxiomType.OBJECT_PROPERTY_DOMAIN));
		}

        HashMap<String, String> propertyNameURI = new HashMap<>();
        Map<String, Schema> properties = new HashMap<>();
        logger.info("Parsing class " + this.cls.toString());

        for (OWLObjectPropertyDomainAxiom dp : properties_class) {
        	if (checkDomainClass(this.cls, dp)) {
        		logger.info( "Parsing property " + dp.toString());
        		for (OWLObjectProperty odp : dp.getObjectPropertiesInSignature()) {
        			String propertyName = this.sfp.getShortForm(odp.getIRI());

        			Boolean inspect = true;
        			// If there are property restrictions from the Class we need to check if
        			// the object property has been previously analyzed on the getClassRestrictions method.
        			// If the property was analyzed, we will change the value of inspect to false, otherwise
        			// the property will be inspected.
        			if (!propertiesFromObjectRestrictions.isEmpty()) {
        				if (propertiesFromObjectRestrictions.contains(odp)) {
        					inspect = false;
        				}
        			}

        			if (inspect) {
						boolean isFunctional = EntitySearcher.isFunctional(odp, this.ontologies.stream());

        				Set<OWLObjectPropertyRangeAxiom> ranges = new HashSet<>();
        				for (OWLOntology ontology: this.ontologies) {
        					ranges.addAll(ontology.getObjectPropertyRangeAxioms(odp));
        				}

        				if (ranges.isEmpty()) {
        					logger.warning("Property " + odp.getIRI() + " has range equals zero");
						}

        				String propertyURI = odp.getIRI().toString();
        				propertyNameURI.put(propertyURI, propertyName);
        				List<String> propertyRanges = getCodeGenTypesByRangeObject(ranges, odp, owlThing);

        				Map<String,String> restrictionValues = new HashMap<String, String>() ;
        				for (OWLOntology ontology: this.ontologies) {
        					RestrictionVisitor restrictionVisitor = new RestrictionVisitor(this.cls, ontology, owlThing, propertyName);
        					for (OWLObjectPropertyRangeAxiom propertyRangeAxiom : ranges) {
        						OWLClassExpression ce = propertyRangeAxiom.getRange();
        						ce.accept(restrictionVisitor);
        					}
        					Map<String, Map<String,String>> restrictionsValuesFromClass = restrictionVisitor.getRestrictionsValuesFromClass();
        					for (String j :  restrictionsValuesFromClass.keySet()) {
        						if (j.equals(propertyName)) {
        							restrictionValues=restrictionsValuesFromClass.get(j);
        						}
        					}
        					if (restrictionsValuesFromClass.isEmpty() && propertyRanges.size() > 1) {
								propertyRanges.clear();
							}
        				}

        				String propertyDescription = ObaUtils.getDescription(odp, this.ontology_cls, this.configFlags.get(CONFIG_FLAG.DEFAULT_DESCRIPTIONS));

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
     * @return A list<String> with the properties
     */
    private List<String> getCodeGenTypesByRangeObject(Set<OWLObjectPropertyRangeAxiom> ranges, OWLObjectProperty odp, OWLClass owlThing) {
        List<String> objectProperty = new ArrayList<>();

        for (OWLObjectPropertyRangeAxiom propertyRangeAxiom : ranges) {
    		for (OWLEntity rangeClass : propertyRangeAxiom.getSignature()) {
    			if (rangeClass instanceof OWLClassExpression) {
    				if (!rangeClass.containsEntityInSignature(odp)) {
    					if (rangeClass.asOWLClass().equals(owlThing)) {
    						logger.info("Ignoring owl:Thing" + odp);
    					} else {
    						this.properties_range.add(rangeClass.asOWLClass());
    						if (this.configFlags.get(CONFIG_FLAG.FOLLOW_REFERENCES))
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
     * @param analyzedClass Class that will be analyzed in order to get its restrictions
     */
    private void getClassRestrictions(OWLClass analyzedClass) {
    	OWLOntologyManager m = OWLManager.createOWLOntologyManager();
    	OWLDataFactory dataFactory = m.getOWLDataFactory();
    	OWLClass owlThing = dataFactory.getOWLThing();

    	for (OWLOntology ontology: this.ontologies) {
    		final RestrictionVisitor restrictionVisitor = new RestrictionVisitor(analyzedClass, ontology, owlThing, "");
    		for (OWLSubClassOfAxiom ax: ontology.getSubClassAxiomsForSubClass(analyzedClass)) {
    			OWLClassExpression superCls = ax.getSuperClass();
    			// Ask our superclass to accept a visit from the RestrictionVisitor
    			// - e.g. if it is an existential restriction then the restriction visitor
    			// will answer it - if not the visitor will ignore it
    			superCls.accept(restrictionVisitor);
    		}

			// For equivalent (to) classes (e.g. Defined classes) we need to accept the visit to navigate it.
			ontology.equivalentClassesAxioms(analyzedClass).forEach((eqClsAx) -> {
				eqClsAx.accept(restrictionVisitor);
			});

			this.enums = restrictionVisitor.getAllEnums();
			this.propertiesFromObjectRestrictions = restrictionVisitor.getPropertiesFromObjectRestrictions();
			this.propertiesFromObjectRestrictions_ranges  = restrictionVisitor.getPropertiesFromObjectRestrictions_ranges();
			this.propertiesFromDataRestrictions = restrictionVisitor.getPropertiesFromDataRestrictions();
			this.propertiesFromDataRestrictions_ranges  = restrictionVisitor.getPropertiesFromDataRestrictions_ranges();
			Map<String, Map<String,String>> restrictionsValuesFromClass = restrictionVisitor.getRestrictionsValuesFromClass();

			if (!restrictionsValuesFromClass.isEmpty()) {
				// When the restriction is a ObjectComplementOf it doesn't have a object property,
				// thus we need to set its value at the setSchema function
				if (restrictionsValuesFromClass.containsKey("complementOf") && restrictionsValuesFromClass.size() == 1) {
					for (String j: restrictionsValuesFromClass.keySet()) {
						Map<String,String> restrictionValues = restrictionsValuesFromClass.get(j);
						for (String restriction:  restrictionValues.keySet()) {
							this.complementOf = restrictionValues.get(restriction);
						}
					}
				} else {
					for (OWLObjectProperty op: this.propertiesFromObjectRestrictions) {
						boolean isFunctional = EntitySearcher.isFunctional(op, this.ontologies.stream());

						MapperObjectProperty mapperObjectProperty;
						String propertyDescription = ObaUtils.getDescription(op, this.ontology_cls, this.configFlags.get(CONFIG_FLAG.DEFAULT_DESCRIPTIONS));
						if (!this.propertiesFromObjectRestrictions_ranges.isEmpty()) {
							List<String> rangesOP = this.propertiesFromObjectRestrictions_ranges.get(this.sfp.getShortForm(op.getIRI()));
							for (String j : restrictionsValuesFromClass.keySet()) {
								Map<String, String> restrictionValues = restrictionsValuesFromClass.get(j);
								if (j.equals(this.sfp.getShortForm(op.getIRI()))) {
									int exactCardinality = -1;
									int minCardinality = -1;
									int maxCardinality = -1;

									if (rangesOP != null && rangesOP.get(0).equals("defaultValue")) {
										mapperObjectProperty = new MapperObjectProperty(this.sfp.getShortForm(op.getIRI()), propertyDescription, isFunctional, restrictionValues, rangesOP, false, true);
									} else {
										String exactCardinalityStr = restrictionValues.get("exactCardinality");
										exactCardinalityStr = ((exactCardinalityStr == null || exactCardinalityStr.isBlank()) ? "-1" : exactCardinalityStr);
										exactCardinality = Integer.parseInt(exactCardinalityStr);

										String minCardinalityStr = restrictionValues.get("minCardinality");
										minCardinalityStr = ((minCardinalityStr == null || minCardinalityStr.isBlank()) ? "-1" : minCardinalityStr);
										minCardinality = Integer.parseInt(minCardinalityStr);

										String maxCardinalityStr = restrictionValues.get("maxCardinality");
										maxCardinalityStr = ((maxCardinalityStr == null || maxCardinalityStr.isBlank()) ? "-1" : maxCardinalityStr);
										maxCardinality = Integer.parseInt(maxCardinalityStr);

										// If cardinality is present and allows for multiple values and it is not functional,
										// then this is an array.
										boolean isArray = !isFunctional
															&& (exactCardinality > 1
															|| minCardinality > 1
															|| maxCardinality > 1);
										
										// If config flag to generate arrays is set, use it to override current setting.
										isArray |= (this.configFlags.containsKey(CONFIG_FLAG.ALWAYS_GENERATE_ARRAYS) && this.configFlags.get(CONFIG_FLAG.ALWAYS_GENERATE_ARRAYS));

										// If cardinality is exactly 1 OR a minimum of 1, then not nullable.
										boolean isNullable = (exactCardinality == -1 && minCardinality == -1) ? true : exactCardinality != 1 && minCardinality < 1;

										mapperObjectProperty = new MapperObjectProperty(this.sfp.getShortForm(op.getIRI()), propertyDescription, isFunctional, restrictionValues, rangesOP, isArray, isNullable);
									}
									
									try {
										Schema opSchema = mapperObjectProperty.getSchemaByObjectProperty();

										boolean is_required = false;

										// If cardinality is exactly 1, then we can remove the min/max property constraints
										// and set the property to be required for the class.
										if (exactCardinality == 1 || (minCardinality == 1 && maxCardinality == 1)) {
											if (this.configFlags.containsKey(CONFIG_FLAG.ALWAYS_GENERATE_ARRAYS) && !this.configFlags.get(CONFIG_FLAG.ALWAYS_GENERATE_ARRAYS)) {
												opSchema.setMinItems(null);
												opSchema.setMaxItems(null);
											}
											
											is_required = true;
										}

										// If cardinality minimum is 1, keep the min/max property constraints
										// and set the property to be required for the class.
										if (minCardinality > 0) {
											is_required = true;
										}

										if (is_required) {
											this.required_properties.add(mapperObjectProperty.name);
										}

										this.properties.put(mapperObjectProperty.name, opSchema);
									} catch (Exception e) {
										logger.warning("Error when parsing object property "+mapperObjectProperty.name);
									}
								}
							}
						}
					}

					for (OWLDataProperty dp: this.propertiesFromDataRestrictions) {
						boolean isFunctional = EntitySearcher.isFunctional(dp, this.ontologies.stream());

						List<String> valuesFromDataRestrictions_ranges = new ArrayList<>();
						String propertyDescription = ObaUtils.getDescription(dp, this.ontology_cls, this.configFlags.get(CONFIG_FLAG.DEFAULT_DESCRIPTIONS));
						if (!this.propertiesFromDataRestrictions_ranges.isEmpty()) {
							List<String> rangesDP = this.propertiesFromDataRestrictions_ranges.get(this.sfp.getShortForm(dp.getIRI()));
							for (String j: restrictionsValuesFromClass.keySet()) {
								Map<String, String> restrictionValues = restrictionsValuesFromClass.get(j);
								if (j.equals(this.sfp.getShortForm(dp.getIRI()))) {
									String exactCardinalityStr = restrictionValues.get("exactCardinality");
									exactCardinalityStr = ((exactCardinalityStr == null || exactCardinalityStr.isBlank()) ? "-1" : exactCardinalityStr);
									int exactCardinality = Integer.parseInt(exactCardinalityStr);

									String minCardinalityStr = restrictionValues.get("minCardinality");
									minCardinalityStr = ((minCardinalityStr == null || minCardinalityStr.isBlank()) ? "-1" : minCardinalityStr);
									int minCardinality = Integer.parseInt(minCardinalityStr);

									String maxCardinalityStr = restrictionValues.get("maxCardinality");
									maxCardinalityStr = ((maxCardinalityStr == null || maxCardinalityStr.isBlank()) ? "-1" : maxCardinalityStr);
									int maxCardinality = Integer.parseInt(maxCardinalityStr);

									// If cardinality is present and allows for multiple values and it is not functional,
									// then this is an array.
									boolean isArray = !isFunctional
														&& (exactCardinality > 1
														|| minCardinality > 1
														|| maxCardinality > 1);
									
									// If config flag to generate arrays is set, use it to override current setting.
									isArray |= (this.configFlags.containsKey(CONFIG_FLAG.ALWAYS_GENERATE_ARRAYS) && this.configFlags.get(CONFIG_FLAG.ALWAYS_GENERATE_ARRAYS));
									
									// If cardinality is exactly 1 OR a minimum of 1, then not nullable.
									boolean isNullable = (exactCardinality == -1 && minCardinality == -1) ? true : exactCardinality != 1 && minCardinality < 1;

									MapperDataProperty mapperDataProperty = new MapperDataProperty(this.sfp.getShortForm(dp.getIRI()), propertyDescription, isFunctional, restrictionValues, valuesFromDataRestrictions_ranges, rangesDP, isArray, isNullable);
									try {
										Schema dpSchema = mapperDataProperty.getSchemaByDataProperty();

										boolean is_required = false;

										// If cardinality is exactly 1, then we can remove the min/max property constraints
										// and set the property to be required for the class.
										if (exactCardinality == 1 || (minCardinality == 1 && maxCardinality == 1)) {
											if (this.configFlags.containsKey(CONFIG_FLAG.ALWAYS_GENERATE_ARRAYS) && !this.configFlags.get(CONFIG_FLAG.ALWAYS_GENERATE_ARRAYS)) {
												dpSchema.setMinItems(null);
												dpSchema.setMaxItems(null);
											}

											is_required = true;
										}

										// If cardinality minimum is 1, keep the min/max property constraints
										// and set the property to be required for the class.
										if (minCardinality > 0) {
											is_required = true;
										}

										if (is_required) {
											this.required_properties.add(mapperDataProperty.name);
										}

										this.properties.put(mapperDataProperty.name, dpSchema);
									} catch (Exception e) {
										logger.warning("Error when processing data property " + mapperDataProperty.name);
									}
								}
							}
						}
					}
				}
			}

			this.properties = setProperties();
    	}
    	
		if (this.configFlags.get(CONFIG_FLAG.DEFAULT_PROPERTIES)) {
			this.properties.putAll(this.getDefaultProperties());
		}
    }

    private Map<String, Schema> getProperties() {
        return this.properties;
    }

    private String getSchemaName(OWLClass cls) {
        return this.schemaNames.get(cls.getIRI());
    }

    public OWLClass getCls() {
        return this.cls;
    }
}
