package edu.isi.oba;

import static edu.isi.oba.Oba.logger;
import edu.isi.oba.config.CONFIG_FLAG;
import edu.isi.oba.config.YamlConfig;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.InferenceDepth;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.semanticweb.owlapi.search.EntitySearcher;

/**
 * Visits existential restrictions and collects the properties which are restricted.
 */
public class ObjectVisitor implements OWLObjectVisitor {
	private final Set<OWLOntology> ontologies;
	private final YamlConfig configData;

	// Base class for this Object Visitor.  This _should_ be the class that .accept()s a visit from this Object Visitor.
	private OWLClass baseClass;
	private OWLOntology ontologyOfBaseClass;
	private Schema classSchema;

	private OWLReasoner reasoner;
	private OWLReasonerFactory reasonerFactory;
	private OWLClass owlThing; // TODO: is this needed anymore??

	private final Map<String, Schema> basePropertiesMap = new HashMap<>();
	private final Map<IRI, Map<String, Schema>> inheritedPropertiesMap = new HashMap<>();

	private final Set<String> propertyNames = new HashSet<>();
	private final Set<String> requiredProperties = new HashSet<>();
	private final Set<String> functionalProperties = new HashSet<>();
	private final Set<OWLClass> referencedClasses = new HashSet<>();
	private final Set<OWLClass> processedClasses = new HashSet<>();
	private final Set<OWLClass> processedRestrictionClasses = new HashSet<>();

	// Used to keep track of a property being visited.  Necessary for complex visits which can involve recursion, because the property name is not passable. 
	private String currentlyProcessedPropertyName = null;

	/**
	 * Constructor for object visitor.
	 * 
	 * @param ontologies the {@link Set} of {@link OWLOntology} loaded by the configuration file.
	 * @param configData a {@link YamlConfig} containing all details loaded from the configuration file.
	 */
	ObjectVisitor(Set<OWLOntology> ontologies, YamlConfig configData) {
		this.ontologies = ontologies;
		this.configData = configData;
	}

	/**
	 * Using the specified base OWLClass, determine its ontology, a reasoner for the ontology, 
	 * the owl:Thing from the ontology, and the basic schema (e.g. name, description, type, default properties).
	 * 
	 * @param baseClass an {@link OWLClass} which should be treated as the primary/base class for this visitor class.
	 */
	private void initializeBaseClass(OWLClass baseClass) {
		// If the base class is already set, ignore.
		if (this.baseClass == null) {
			this.baseClass = baseClass;
			final var visitedClassIRI = this.baseClass.getIRI();

			this.reasonerFactory = new StructuralReasonerFactory();

			// We can pragmatically determine the class's ontology based on the set of ontologies and the class itself.  Also set the owl:Thing for that ontology.
			this.ontologies.stream().takeWhile(ontology -> ontology.containsClassInSignature(visitedClassIRI)).forEach((ontology) -> {
				this.ontologyOfBaseClass = ontology;
				this.reasoner = this.reasonerFactory.createReasoner(ontology);
				this.owlThing = this.reasoner.getTopClassNode().getRepresentativeElement();
			});

			this.classSchema = this.getBaseClassBasicSchema();
		}
	}

	/**
	 * Create and return a basic {@link Schema} to be used when adding other details, such as properties.
	 * 
	 * @return a basic {@link Schema} for this visitor's base class.
	 */
	private Schema getBaseClassBasicSchema() {
		var basicClassSchema = new Schema();
		basicClassSchema.setName(this.getBaseClassName());
		basicClassSchema.setDescription(ObaUtils.getDescription(this.baseClass, this.ontologies, this.configData.getConfigFlagValue(CONFIG_FLAG.DEFAULT_DESCRIPTIONS)));
		basicClassSchema.setType("object");
		
		if (this.configData.getConfigFlagValue(CONFIG_FLAG.DEFAULT_PROPERTIES)) {
			// Not using setProperties(), because it creates immutability which breaks unit tests.
			this.getDefaultProperties().forEach((schemaName, schema) -> {
				basicClassSchema.addProperty(schemaName, schema);
			});
		}

		return basicClassSchema;
	}

	/**
	 * Get the OpenAPI schema for the base class specified in the constructor.
	 * Although somewhat convoluted, this schema will not be generated fully until the {@link #visit(OWLClass)} method has been called by the base OWLClass to accept this visitor class.
	 * 
	 * @see {@link #visit(OWLClass)}
	 * @return a {@link Schema} for the entire class
	 */
	public Schema getClassSchema() {
		// Generate the required properties for the class, if applicable.
		if (this.configData.getConfigFlagValue(CONFIG_FLAG.REQUIRED_PROPERTIES_FROM_CARDINALITY)) {
			this.generateRequiredPropertiesForClassSchemas();
		}

		// Convert non-array property items, if applicable.
		if (!this.configData.getConfigFlagValue(CONFIG_FLAG.ALWAYS_GENERATE_ARRAYS)) {
			MapperProperty.convertArrayToNonArrayPropertySchemas(this.classSchema, this.functionalProperties);
		}

		// If following references AND use inheritance references (for the class), we do not want to inherit/reference the same class multiple times accidentally.
		// (e.g. if we have Person > Student > ExchangeStudent, Student already inherits everything from Person.
		// 		For ExchangeStudent, we do not want to inherit from Person AND Student.
		//		We only need to inherit from Student [which automatically inherits everything from Person also].)
		if (this.configData.getConfigFlagValue(CONFIG_FLAG.FOLLOW_REFERENCES) 
			&& this.configData.getConfigFlagValue(CONFIG_FLAG.USE_INHERITANCE_REFERENCES)) {
			
			// If the class has a type (likely "object" - are there any other possibilities??), it needs to be removed and added to the "allOf" entries.
			this.classSchema.setType(null);

			// If adding for the first time, need to include a "type: object" entry.
			if (this.classSchema.getAllOf() == null || this.classSchema.getAllOf().isEmpty()) {
				final var objSchema = new ObjectSchema();
				this.classSchema.addAllOfItem(objSchema);
			}

			// All processed classes, minus the base class, are the super classes.
			final var superClasses = new HashSet<OWLClass>(this.processedClasses);
			superClasses.remove(this.baseClass);

			// Make a copy of the super classes.
			// Loop through all super classes and remove any super-super-classes that are being inherited by a nearer/more direct super class to the base class.
			final var directSuperClasses = new HashSet<OWLClass>(superClasses);
			for (OWLClass superClassA : superClasses) {
				for (OWLClass superClassB : superClasses) {
					if (!superClassA.equals(superClassB) && this.reasoner.getSuperClasses(superClassA, false).containsEntity(superClassB)) {
						directSuperClasses.remove(superClassB);
					}
				}
			}

			// Add all direct superclasses to allOf list.
			directSuperClasses.stream().forEach(superClass -> {
				final var refSchema = new ObjectSchema();
				refSchema.set$ref(superClass.getIRI().getShortForm());

				this.classSchema.addAllOfItem(refSchema);
			});

			// If there is only one item in the allOf list, then it is "type: object".  That means nothing is inherited.  Set it back to null.
			if (this.classSchema.getAllOf() != null && this.classSchema.getAllOf().size() == 1) {
				this.classSchema.setAllOf(null);
			}
		}

		return this.classSchema;
	}

	/**
	 * Get all the classes referenced directly or indirectly (potentially through inheritance) by the base class.
	 * 
	 * @return a {@link Set} of {@link OWLClass}
	 */
	public Set<OWLClass> getAllReferencedClasses() {
		return this.referencedClasses;
	}

	/**
	 * 
	 * @param ce an {@link OWLClass} to be visited by this visitor class.
	 */
	@Override
	public void visit(@Nonnull OWLClass ce) {
		// If the base class is null when this OWLClass is visited, then treat it as the base class and set up this Visitor class with its basic details.
		if (this.baseClass == null) {
			this.initializeBaseClass(ce);
		}

		// Avoid cycles and accept visits from super classes for the purpose of getting all properties.
		if (!this.processedClasses.contains(ce)) {
			// If we are processing inherited restrictions then we recursively visit named supers.
			this.processedClasses.add(ce);

			// Loop through the ontologies to use the one relevant for the current OWLClass.
			for (OWLOntology ontology: this.ontologies) {
				// Only traverse this OWLClass's super classes, if it is contained in the ontology.
				if (ontology.containsClassInSignature(ce.getIRI())) {
					// If it has subclass axioms, then loop through each to accept visits for all super classes.
					ontology.subClassAxiomsForSubClass(ce).forEach(ax -> {
						// Only traverse super classes for inheriting properties.  Restrictions handled via generatePropertySchemasWithRestrictions() below.
						if (ax.getSuperClass().isOWLClass()) {
							ax.getSuperClass().accept(this);
						}
					});
				}
			}
		}

		// Only include properties from the base class OR we are not following references (because all properties need to be copied to the base class in this case).
		// Inherited details should be determined via references.
		if (this.baseClass.equals(ce)) {
			// Get all non-inherited object and data properties.
			this.basePropertiesMap.putAll(this.getObjectPropertySchemasForClass(ce));
			this.basePropertiesMap.putAll(this.getDataPropertySchemasForClass(ce));

			// Not using setProperties(), because it creates immutability which breaks unit tests.
			this.basePropertiesMap.forEach((schemaName, schema) -> {
				this.classSchema.addProperty(schemaName, schema);
			});

			// Generate restrictions for all properties of this class, regardless of following references or not.
			this.generatePropertySchemasWithRestrictions(ce);
		} else {
			// If this is a superclass AND we are using inheritance/superclass references, still add the properties to the properties map.
			if (!this.configData.getConfigFlagValue(CONFIG_FLAG.USE_INHERITANCE_REFERENCES)) {
				this.basePropertiesMap.putAll(this.getObjectPropertySchemasForClass(ce));
				this.basePropertiesMap.putAll(this.getDataPropertySchemasForClass(ce));

				// Not using setProperties(), because it creates immutability which breaks unit tests.
				this.basePropertiesMap.forEach((schemaName, schema) -> {
					this.classSchema.addProperty(schemaName, schema);
				});

				this.generatePropertySchemasWithRestrictions(ce);
			}
		}
	}

	/**
	 * Update the base class's {@link Schema} properties with restrictions from the ontology.
	 * 
	 * @param owlClass an {@link OWLClass} which may either be the base class or one of its super classes.
	 */
	private void generatePropertySchemasWithRestrictions(OWLClass owlClass) {
		if (owlClass != null && owlClass.equals(this.baseClass)) {
			logger.info("  =============================Generating restrictions for:  " + this.baseClass);
		} else {
			logger.info("  =============================Generating restrictions for:  " + this.baseClass + "  where were inherited from:  " + owlClass);
		}

		// Avoid cycles and accept visits from super classes for the purpose of getting all properties.
		if (!this.processedRestrictionClasses.contains(owlClass)) {
			// If we are processing inherited restrictions then we recursively visit named supers.
			this.processedRestrictionClasses.add(owlClass);

			// Loop through the ontologies to use the one relevant for the current OWLClass.
			for (OWLOntology ontology: this.ontologies) {
				// Search the ontology for this OWLClass.
				// If it has subclass axioms, then loop through each to generate schema restrictions.
				ontology.subClassAxiomsForSubClass(owlClass).forEach(ax -> {
					// A flag to determine whether we should skip visiting the axiom's super class.
					boolean shouldSkipVisits = false;

					// Well-formed axioms should not be an OWLClass type.
					if (!(ax instanceof OWLClass)) {
						if (ax.getSuperClass() instanceof OWLRestriction) {
							final var property = ax.getSuperClass() instanceof OWLObjectRestriction ? ((OWLObjectRestriction) ax.getSuperClass()).getProperty().asOWLObjectProperty() : ((OWLDataRestriction) ax.getSuperClass()).getProperty().asOWLDataProperty();
							this.currentlyProcessedPropertyName = property.getIRI().getShortForm();
							
							// Add any classes referenced by the restriction.
							this.referencedClasses.addAll(ax.getSuperClass().getClassesInSignature());
						} else if (ax.getSuperClass() instanceof OWLBooleanClassExpression) {
							if (ax.getSuperClass() instanceof OWLObjectComplementOf) {
								// Add the object complement reference class.
								this.referencedClasses.addAll(ax.getSuperClass().getClassesInSignature());

								logger.info("\t" + this.getBaseClassName() + " has an object complement of axiom.  This is not for a property, so do not set property name.  Axiom:   " + ax);
							} else {
								logger.severe("\t" + this.getBaseClassName() + " has unknown restriction.  Axiom:   " + ax);
								shouldSkipVisits = true;
							}
						} else if (ax.getSuperClass() instanceof OWLObjectOneOf) {
							logger.info("\t" + this.getBaseClassName() + " is an ObjectOneOf set containing one or more Individuals.  Not setting propety name, to treat it like an enum.  Axiom:  " + ax);
						} else {
							logger.info("\t" + this.getBaseClassName() + " is a subclass of " + ax.getSuperClass().asOWLClass().getIRI().getShortForm() + ".  No restrictions to process.  Axiom:  " + ax);
							shouldSkipVisits = true;
						}

						if (!shouldSkipVisits) {
							// Proceed with the visit.
							ax.getSuperClass().accept(this);
						}

						// Clear out the property name.
						this.currentlyProcessedPropertyName = null;
					} else {
						logger.severe("\t" + this.getBaseClassName() + " has unknown restriction.  Axiom:   " + ax);
					}
				});

				// For equivalent (to) classes (e.g. Defined classes) we need to accept the visit to navigate it.
				ontology.equivalentClassesAxioms(owlClass).forEach((eqClsAx) -> {
					eqClsAx.accept(this);
				});
			}
		}
	}

	/**
	 * Check each of the base class's properties and add it to the list of required properties, if it meets the criteria.
	 */
	private void generateRequiredPropertiesForClassSchemas() {
		final Map<String, Schema> propertySchemas = this.classSchema.getProperties() == null ? new HashMap<>() : this.classSchema.getProperties();

		propertySchemas.forEach((propertyName, propertySchema) -> {
			propertySchema.getMinItems();

			if (propertySchema.getMinItems() != null && propertySchema.getMinItems() == 1
				&& propertySchema.getMaxItems() != null && propertySchema.getMaxItems() == 1) {
				this.requiredProperties.add(propertyName);
			}

			if (propertySchema.getMinItems() != null && propertySchema.getMinItems() > 0) {
				this.requiredProperties.add(propertyName);
			}
		});

		this.classSchema.setRequired(this.requiredProperties.stream().collect(Collectors.toList()));
	}

	/**
	 * Convenience method for getting the base class's short form name (i.e. only its name, not its full IRI).
	 * 
	 * @return a {@link String} which is the (short form) name of the base class.
	 */
	private String getBaseClassName() {
		return this.baseClass.getIRI().getShortForm();
	}

	/**
	 * Get a {@link Schema} for the object property (expression).  The ranges are also passed because it may be inheriting from a super-property (and therefore not obvious from the object property expression itself).
	 * 
	 * @param objPropExpr an {@link OWLObjectPropertyExpression}
	 * @param objPropRanges a {@link Set} of {@link OWLClass} representing the object property's range(s), which may include inherited ranges not obvious from the {@link OWLObjectPropertyExpression} itself.
	 * @return a {@link Schema} of the object property (expression).
	 */
	private Schema getObjectPropertySchema(OWLObjectPropertyExpression objPropExpr, @Nullable Set<OWLClass> objPropRanges) {
		final var op = objPropExpr.asOWLObjectProperty();
		final var propertyName = op.getIRI().getShortForm();
		this.propertyNames.add(propertyName);
		this.currentlyProcessedPropertyName = propertyName;

		logger.info( "\t\tClass: \"" + this.getBaseClassName() + "\"  -  Property: \"" + propertyName + "\"");

		final var propertyRanges = new HashSet<String>();
		final var complexObjectRanges = new HashSet<OWLClassExpression>();

		// Add object property OWLClass ranges to set of property ranges.
		if (objPropRanges != null) {
			objPropRanges.forEach(objPropRange -> {
				if (this.configData.getConfigFlagValue(CONFIG_FLAG.FOLLOW_REFERENCES)) {
					propertyRanges.add(objPropRange.asOWLClass().getIRI().getShortForm());
	
					// Add the range to the referenced class set.
					this.referencedClasses.add(objPropRange.asOWLClass());
				} else {
					propertyRanges.add(null);
				}
			});	
		}
		
		// Also loop through range axioms for object property expression and add ranges to map, complex map, or visit the range, if unionOf/intersectionOf/oneOf.
		this.ontologyOfBaseClass.objectPropertyRangeAxioms(objPropExpr).forEach((objPropRangeAxiom) -> {
			if (objPropRangeAxiom.getRange() instanceof OWLClass) {
				if (this.configData.getConfigFlagValue(CONFIG_FLAG.FOLLOW_REFERENCES)) {
					propertyRanges.add(objPropRangeAxiom.getRange().asOWLClass().getIRI().getShortForm());

					// Add the range to the referenced class set.
					this.referencedClasses.add(objPropRangeAxiom.getRange().asOWLClass());
				} else {
					propertyRanges.add(null);
				}
			} else if (objPropRangeAxiom.getRange() instanceof OWLObjectUnionOf
						|| objPropRangeAxiom.getRange() instanceof OWLObjectIntersectionOf
						|| objPropRangeAxiom.getRange() instanceof OWLObjectOneOf) {
				objPropRangeAxiom.getRange().accept(this);
			} else {
				complexObjectRanges.add(objPropRangeAxiom.getRange());
			}
		});

		// Check the ranges.  Output relevant info.  May not be necessary.
		if (propertyRanges.isEmpty()) {
			logger.warning("\t\tProperty \"" + op.getIRI() + "\" has range equals zero.");
		} else {
			logger.info( "\t\tProperty range(s): " + propertyRanges);
		}

		// In cases, such as unionOf/intersectionOf/oneOf , the property schema may already be set.  Get it, if so.
		var objPropertySchema = this.classSchema.getProperties() == null ? null : (Schema) this.classSchema.getProperties().get(propertyName);
		
		try {
			final var propertyDescription = ObaUtils.getDescription(op, this.ontologies, this.configData.getConfigFlagValue(CONFIG_FLAG.DEFAULT_DESCRIPTIONS));

			// Workaround for handling unionOf/intersectionOf/oneOf cases which may be set already above.
			if (objPropertySchema == null) {
				// Get object property schema from mapper.
				objPropertySchema = MapperObjectProperty.createObjectPropertySchema(propertyName, propertyDescription, propertyRanges);
			} else {
				// These do not get set properly because the unionOf/intersectionOf/oneOf property schema was not created via MapperDataProperty.createDataPropertySchema().
				MapperObjectProperty.setSchemaName(objPropertySchema, propertyName);
				MapperObjectProperty.setSchemaDescription(objPropertySchema, propertyDescription);
			}

			// If property is functional, set the schema accordingly.
			if (EntitySearcher.isFunctional(op, Collections.singleton(this.ontologyOfBaseClass).stream())) {
				this.functionalProperties.add(propertyName);
				MapperProperty.setFunctionalForPropertySchema(objPropertySchema);
			}

			// For any complex property ranges, traverse.  This will grab restrictions also.  There is no good way for this situation to grab only the types in this situation.
			if (!complexObjectRanges.isEmpty()) {
				complexObjectRanges.forEach((objectRange) -> {
					objectRange.accept(this);
				});
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		this.currentlyProcessedPropertyName = null;

		return objPropertySchema;
	}

	/**
	 * Read the Ontology, obtain the ObjectProperties, obtain the range for each property and generate the SchemaProperty.
	 * 
	 * @param owlClass the {@link OWLClass} to get all object property {@link Schema}s for.
	 * @return a {@link Map} of the object property name keys and their associated {@link Schema}s.
	 */
	private Map<String, Schema> getObjectPropertySchemasForClass(OWLClass owlClass) {
		// Object property map to return
		final var objPropertiesMap = new HashMap<String, Schema>();

		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		this.reasoner.subObjectProperties(this.reasoner.getTopObjectPropertyNode().getRepresentativeElement(), InferenceDepth.DIRECT)
						.filter(objPropExpr -> !objPropExpr.isBottomEntity() && objPropExpr.isOWLObjectProperty())
						.forEach((objPropExpr) -> {
			final var objPropDomains = new HashSet<OWLClass>();
			this.reasoner.objectPropertyDomains(objPropExpr, InferenceDepth.DIRECT).filter(objPropDomain -> owlClass.equals(objPropDomain)).forEach(objPropDomains::add);

			final var objPropRanges = new HashSet<OWLClass>();
			this.reasoner.objectPropertyRanges(objPropExpr, InferenceDepth.DIRECT).forEach(objPropRanges::add);

			// If this (sub-)property (under owl:topObjectProperty) has a domain of the current owlClass, then get its schema.
			if (!objPropDomains.isEmpty()) {
				// Save object property schema to class's schema.
				objPropertiesMap.put(objPropExpr.asOWLObjectProperty().getIRI().getShortForm(), this.getObjectPropertySchema(objPropExpr, objPropRanges));
			}
			
			// Loop through all subproperties of this property.
			this.reasoner.subObjectProperties(objPropExpr, InferenceDepth.ALL)
							.filter(subObjPropExpr -> !subObjPropExpr.isBottomEntity() && subObjPropExpr.isOWLObjectProperty())
							.forEach((subObjPropExpr) -> {
				// Check subproperty's domain(s) and inherit from its super-property.
				final var subObjPropDomains = new HashSet<OWLClass>();
				this.reasoner.objectPropertyDomains(subObjPropExpr, InferenceDepth.DIRECT).filter(objPropDomain -> owlClass.equals(objPropDomain)).forEach(subObjPropDomains::add);
				subObjPropDomains.addAll(objPropDomains);

				// Check subproperty's range(s) and inherit from its super-property.
				final var subObjPropRanges = new HashSet<OWLClass>();
				this.reasoner.objectPropertyRanges(subObjPropExpr, InferenceDepth.DIRECT).forEach(subObjPropRanges::add);
				subObjPropRanges.addAll(objPropRanges);

				// If this (sub-)property has a domain of the current owlClass, then get its schema.
				if (!subObjPropDomains.isEmpty()) {
					// Save object property schema to class's schema.
					objPropertiesMap.put(subObjPropExpr.asOWLObjectProperty().getIRI().getShortForm(), this.getObjectPropertySchema(subObjPropExpr, subObjPropRanges));
				}
			});
		});
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		return objPropertiesMap;
    }

	/**
	 * Read the Ontology, obtain the DataProperties, obtain the range for each property and generate the SchemaProperty.
	 * 
	 * @param owlClass the {@link OWLClass} to get all data property {@link Schema}s for.
	 * @return a {@link Map} of the data property name keys and their associated {@link Schema}s.
	 */
	private Map<String, Schema> getDataPropertySchemasForClass(OWLClass owlClass) {
		final var dataPropDomainAxioms = new HashSet<OWLDataPropertyDomainAxiom>();
		for (OWLOntology ontology: this.ontologies) {
			dataPropDomainAxioms.addAll(ontology.getAxioms(AxiomType.DATA_PROPERTY_DOMAIN));
		}

		// Data property map to return
		final var dataPropertiesMap = new HashMap<String, Schema>();
		
		// For the class's properties, check each axiom where the axiom's domain is a class AND the current class equals the domain.
		dataPropDomainAxioms.stream().filter(dataPropDomainAx -> dataPropDomainAx.getDomain().getClassesInSignature().contains(owlClass)).forEach((dataPropDomainAx) -> {
			logger.info( "\tParsing data property domain axiom: " + dataPropDomainAx.toString());

			// Get set of all data properties and subproperties.
			final var dataProperties = dataPropDomainAx.getDataPropertiesInSignature();
			for (final var topLevelDataProperty: dataProperties) {
				for (final var dataPropEx: this.reasoner.getSubDataProperties(topLevelDataProperty, false).getFlattened()) {
					//owl:bottomDataProperty
					if (!dataPropEx.isOWLBottomDataProperty()) {
						dataProperties.add(dataPropEx.asOWLDataProperty());
					}
				}
			}

			// Loop through each object (sub)property and generate its schema
			for (final var dp: dataProperties) {
				final var propertyName = dp.getIRI().getShortForm();
				this.propertyNames.add(propertyName);
				this.currentlyProcessedPropertyName = propertyName;

				logger.info( "\t\tClass: \"" + this.getBaseClassName() + "\"  -  Property: \"" + propertyName + "\"");

				final var propertyRanges = new HashSet<String>();
				final var complexDataRanges = new HashSet<OWLDataRange>();
				this.ontologyOfBaseClass.dataPropertyRangeAxioms(dp).forEach((dataPropRangeAxiom) -> {
					if (dataPropRangeAxiom.getRange() instanceof OWLDatatype) {
						propertyRanges.add(((OWLDatatype) dataPropRangeAxiom.getRange()).getIRI().getShortForm());
					} else if (dataPropRangeAxiom.getRange() instanceof OWLDataUnionOf
								|| dataPropRangeAxiom.getRange() instanceof OWLDataIntersectionOf
								|| dataPropRangeAxiom.getRange() instanceof OWLDataOneOf) {
						dataPropRangeAxiom.getRange().accept(this);
					} else {
						complexDataRanges.add(dataPropRangeAxiom.getRange());
					}
				});

				// Check the ranges.  Output relevant info.  May not be necessary.
				if (propertyRanges.isEmpty()) {
					logger.warning("\t\tProperty \"" + dp.getIRI() + "\" has range equals zero.");
				} else {
					logger.info( "\t\tProperty range(s): " + propertyRanges);

					try {
						final var propertyDescription = ObaUtils.getDescription(dp, this.ontologies, this.configData.getConfigFlagValue(CONFIG_FLAG.DEFAULT_DESCRIPTIONS));
	
						// In cases, such as unionOf/intersectionOf/oneOf , the property schema may already be set.  Get it, if so.
						var dataPropertySchema = this.classSchema.getProperties() == null ? null : (Schema) this.classSchema.getProperties().get(propertyName);
	
						// Workaround for handling unionOf/intersectionOf/oneOf cases which may be set already above.
						if (dataPropertySchema == null) {
							// Get data property schema from mapper.
							dataPropertySchema = MapperDataProperty.createDataPropertySchema(propertyName, propertyDescription, propertyRanges);
						} else {
							// These do not get set properly because the unionOf/intersectionOf/oneOf property schema was not created via MapperDataProperty.createDataPropertySchema().
							MapperDataProperty.setSchemaName(dataPropertySchema, propertyName);
							MapperDataProperty.setSchemaDescription(dataPropertySchema, propertyDescription);
						}
	
						// If property is functional, set the schema accordingly.
						if (EntitySearcher.isFunctional(dp, Collections.singleton(this.ontologyOfBaseClass).stream())) {
							this.functionalProperties.add(propertyName);
							MapperDataProperty.setFunctionalForPropertySchema(dataPropertySchema);
						}
	
						// Save object property schema to class's schema.
						dataPropertiesMap.put(dataPropertySchema.getName(), dataPropertySchema);
	
						// For any complex property ranges, traverse.  This will grab restrictions also.  There is no good way for this situation to grab only the types in this situation.
						if (!complexDataRanges.isEmpty()) {
							complexDataRanges.forEach((dataRange) -> {
								dataRange.accept(this);
							});
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				this.currentlyProcessedPropertyName = null;
			}
		});

		return dataPropertiesMap;
    }

	/**
     * Get default schema properties.
	 * 
	 * These can be disabled by setting `default_properties` to `false` in the `config.yaml` file.
	 * 
	 * @return A Map where key is property name and value is the property's Swagger/OpenAPI Schema
     */
    private Map<String, Schema> getDefaultProperties() {
		// Add some typical default properties (e.g. id, lable, type, and description)
        final var idPropertySchema = MapperDataProperty.createDataPropertySchema("id", "identifier", new HashSet<String>(){{add("integer");}});
		MapperDataProperty.setNullableValueForPropertySchema(idPropertySchema, false);
        final var labelPropertySchema = MapperDataProperty.createDataPropertySchema("label", "short description of the resource", new HashSet<String>(){{add("string");}});
		MapperDataProperty.setNullableValueForPropertySchema(labelPropertySchema, true);
        final var typePropertySchema = MapperDataProperty.createDataPropertySchema("type", "type(s) of the resource", new HashSet<String>(){{add("string");}});
		MapperDataProperty.setNullableValueForPropertySchema(typePropertySchema, true);
		final var descriptionPropertySchema = MapperDataProperty.createDataPropertySchema("description", "small description", new HashSet<String>(){{add("string");}});
		MapperDataProperty.setNullableValueForPropertySchema(descriptionPropertySchema, true);
		
		// Also add some default property examples of different types (e.g. a date/time, a boolean, and a float)
		final var eventDateTimePropertySchema = MapperDataProperty.createDataPropertySchema("eventDateTime", "a date/time of the resource", new HashSet<String>(){{add("dateTime");}});
		MapperDataProperty.setNullableValueForPropertySchema(eventDateTimePropertySchema, true);
		final var isBoolPropertySchema = MapperDataProperty.createDataPropertySchema("isBool", "a boolean indicator of the resource", new HashSet<String>(){{add("boolean");}});
		MapperDataProperty.setNullableValueForPropertySchema(isBoolPropertySchema, true);
		final var quantityPropertySchema = MapperDataProperty.createDataPropertySchema("quantity", "a number quantity of the resource", new HashSet<String>(){{add("float");}});
		MapperDataProperty.setNullableValueForPropertySchema(quantityPropertySchema, true);

		return Map.ofEntries(
			Map.entry(idPropertySchema.getName(), idPropertySchema),
			Map.entry(labelPropertySchema.getName(), labelPropertySchema),
			Map.entry(typePropertySchema.getName(), typePropertySchema),
			Map.entry(descriptionPropertySchema.getName(), descriptionPropertySchema),
			Map.entry(eventDateTimePropertySchema.getName(), eventDateTimePropertySchema),
			Map.entry(isBoolPropertySchema.getName(), isBoolPropertySchema),
			Map.entry(quantityPropertySchema.getName(), quantityPropertySchema)
		);
    }

	/**
	 * Gets a new (if one does not already exist) or existing property schema for use in updating during restriction visits.
	 * 
	 * @param propertyName the name of the property to get a {@link Schema} for.
	 * @return a {@link Schema} for the property.
	 */
	private Schema getPropertySchemaForRestrictionVisit(String propertyName) {
		Schema currentPropertySchema = this.classSchema.getProperties() == null ? null : (Schema) this.classSchema.getProperties().get(propertyName);

		// In certain cases, a property was not set up with domains/ranges but has a restriction.
		// This property will not exist in the map of property names + schemas yet, so add it and set it up with basic info.
		if (currentPropertySchema == null) {
			currentPropertySchema = new ArraySchema();
			currentPropertySchema.setName(propertyName);

			final var propertyDescription = this.configData.getConfigFlagValue(CONFIG_FLAG.DEFAULT_DESCRIPTIONS) ? ObaUtils.DEFAULT_DESCRIPTION : null;
			currentPropertySchema.setDescription(propertyDescription);

			// If this was a new property schema, need to make sure it's added.
			this.classSchema.addProperty(propertyName, currentPropertySchema);
		}

		return currentPropertySchema;
	}

	/**
	 * ================== Restrictions traversals ==================
	 */

	@Override
	public void visit(@Nonnull OWLEquivalentClassesAxiom ax) {
		logger.info("Analyzing restrictions of Class: " + this.baseClass + " with axiom: " + ax);

		// If equivalent class axiom AND contains owl:oneOf, then we're looking at an ENUM class.
		ax.classExpressions().filter((e) -> e instanceof OWLObjectOneOf).forEach((oneOfObj) -> {
			var enumValues = ((OWLObjectOneOf) oneOfObj).getOperandsAsList();
			if (enumValues != null && !enumValues.isEmpty()) {
				// Add enum individuals to restriction range
				enumValues.forEach((indv) -> {
					MapperObjectProperty.addEnumValueToObjectSchema(this.classSchema, ((OWLNamedIndividual) indv).getIRI().getShortForm());
				});
			}
		});

		// Loop through each expression in the equivalent classes axiom and accept visits from everything else.
		ax.classExpressions().filter((e) -> !this.baseClass.equals(e) && !(e instanceof OWLObjectOneOf)).forEach((e) -> {
			e.accept(this);
		});
	}

	/**
	 * Convenience method for adding restriction values and ranges from a visit to {@link OWLNaryBooleanClassExpression} (i.e. {@link OWLObjectUnionOf} or {@link OWLObjectIntersectionOf}).
	 * 
	 * @param ce the OWLNaryBooleanClassExpression object
	 */
	private void visitOWLNaryBooleanClassExpression(@Nonnull OWLNaryBooleanClassExpression ce) {
		logger.info("Analyzing restrictions of Class: " + this.baseClass + " with axiom: " + ce);

		// If no existing property schema, then create empty schema for it.
		final var currentPropertySchema = this.getPropertySchemaForRestrictionVisit(this.currentlyProcessedPropertyName);

		currentPropertySchema.setItems(MapperObjectProperty.getComplexObjectComposedSchema(ce, this.configData.getConfigFlagValue(CONFIG_FLAG.FOLLOW_REFERENCES)));
		MapperObjectProperty.setSchemaType(currentPropertySchema, "array");
	}
		
	@Override
	public void visit(@Nonnull OWLObjectUnionOf ce) {
		this.visitOWLNaryBooleanClassExpression(ce);
	}

	@Override
	public void visit(@Nonnull OWLObjectIntersectionOf ce) {
		this.visitOWLNaryBooleanClassExpression(ce);
	}

	/**
	 * Convenience method for adding restriction values and ranges from a visit to {@link OWLQuantifiedObjectRestriction} 
	 * (i.e. {@link OWLObjectAllValuesFrom}, {@link OWLObjectSomeValuesFrom}, or
	 * {@link OWLObjectCardinalityRestriction [subinterfaces: {@link OWLObjectExactCardinality}, {@link OWLObjectMaxCardinality}, or {@link OWLObjectMinCardinality}]).
	 * 
	 * @param ce the {@link OWLQuantifiedObjectRestriction} object
	 */
	private void visitOWLQuantifiedObjectRestriction(@Nonnull OWLQuantifiedObjectRestriction or) {
		logger.info("Analyzing restrictions of Class: " + this.baseClass + " with axiom: " + or);

		final var ce = or.getFiller();
		if (ce instanceof OWLObjectUnionOf || ce instanceof OWLObjectIntersectionOf || ce instanceof OWLObjectOneOf) {
			ce.accept(this);
		} else {
			final Integer restrictionValue = (or instanceof OWLObjectCardinalityRestriction) ? ((OWLObjectCardinalityRestriction) or).getCardinality() : null;
			final var objRestrictionRange = this.configData.getConfigFlagValue(CONFIG_FLAG.FOLLOW_REFERENCES) ? ce.asOWLClass().getIRI().getShortForm() : null;

			// If no existing property schema, then create empty schema for it.
			final var currentPropertySchema = this.getPropertySchemaForRestrictionVisit(this.currentlyProcessedPropertyName);

			// Update current property schema with the appropriate restriction range/value.
			if (or instanceof OWLObjectSomeValuesFrom) {
				MapperObjectProperty.addAnyOfToObjectPropertySchema(currentPropertySchema, objRestrictionRange);
			} else if (or instanceof OWLObjectAllValuesFrom) {
				MapperObjectProperty.addAllOfToObjectPropertySchema(currentPropertySchema, objRestrictionRange);
			} else if (or instanceof OWLObjectMinCardinality) {
				MapperObjectProperty.addMinCardinalityToPropertySchema(currentPropertySchema, restrictionValue, objRestrictionRange);
			} else if (or instanceof OWLObjectMaxCardinality) {
				MapperObjectProperty.addMaxCardinalityToPropertySchema(currentPropertySchema, restrictionValue, objRestrictionRange);
			} else if (or instanceof OWLObjectExactCardinality) {
				MapperObjectProperty.addExactCardinalityToPropertySchema(currentPropertySchema, restrictionValue, objRestrictionRange);
			}
		}
	}

	/**
	 * This method gets called when a class expression is an existential
	 * (someValuesFrom) restriction and it asks us to visit it
	 */
	@Override
	public void visit(@Nonnull OWLObjectSomeValuesFrom ce) {
		this.visitOWLQuantifiedObjectRestriction(ce);
	}
	
	/**
	 * This method gets called when a class expression is a universal 
	 * (allValuesFrom) restriction and it asks us to visit it
	 */
	@Override
	public void visit(@Nonnull OWLObjectAllValuesFrom ce) {
		this.visitOWLQuantifiedObjectRestriction(ce);
	}

	@Override
	public void visit(@Nonnull OWLObjectMinCardinality ce) {
		this.visitOWLQuantifiedObjectRestriction(ce);
	}

	@Override
	public void visit(@Nonnull OWLObjectMaxCardinality ce) {
		this.visitOWLQuantifiedObjectRestriction(ce);
	}

	@Override
	public void visit(@Nonnull OWLObjectExactCardinality ce) {
		this.visitOWLQuantifiedObjectRestriction(ce);
	}
	
	@Override
	public void visit(@Nonnull OWLObjectComplementOf ce) {
		logger.info("Analyzing restrictions of Class: " + this.baseClass + " with axiom: " + ce);

		// ComplementOf can occur either for OWLClass or for one of its object properties.  If the property name is null, assume it is a class's complement (and not a property's complement).
		if (this.currentlyProcessedPropertyName == null) {
			MapperObjectProperty.setComplementOfForObjectSchema(this.classSchema, ce.getOperand().asOWLClass().getIRI().getShortForm());
		} else {
			// If no existing property schema, then create empty schema for it.
			final var currentPropertySchema = this.getPropertySchemaForRestrictionVisit(this.currentlyProcessedPropertyName);

			MapperObjectProperty.setComplementOfForObjectSchema(currentPropertySchema, ce.getOperand().asOWLClass().getIRI().getShortForm());
		}
 	}
	
	@Override
	public void visit(@Nonnull OWLObjectHasValue ce) {
		logger.info("Analyzing restrictions of Class: " + this.baseClass + " with axiom: " + ce);

		// If no existing property schema, then create empty schema for it.
		final var currentPropertySchema = this.getPropertySchemaForRestrictionVisit(this.currentlyProcessedPropertyName);
		
		MapperObjectProperty.addHasValueOfPropertySchema(currentPropertySchema, ((OWLNamedIndividual) ce.getFiller()).getIRI().getShortForm());
	}
	
	@Override
	public void visit(@Nonnull OWLObjectOneOf ce) {
		logger.info("Analyzing restrictions of Class: " + this.baseClass + " with axiom: " + ce);

		// ObjectOneOf can occur either for OWLClass or for one of its object properties.  If the property name is null, assume it the class is actually an enum.
		if (this.currentlyProcessedPropertyName == null) {
			var enumValues = ce.getOperandsAsList();
			if (enumValues != null && !enumValues.isEmpty()) {
				// Add enum individuals to restriction range
				enumValues.forEach((indv) -> {
					MapperObjectProperty.addEnumValueToObjectSchema(this.classSchema, ((OWLNamedIndividual) indv).getIRI().getShortForm());
				});
			}
		} else {
			for (OWLIndividual individual: ce.getIndividuals()) {
				// If no existing property schema, then create empty schema for it.
				final var currentPropertySchema = this.getPropertySchemaForRestrictionVisit(this.currentlyProcessedPropertyName);
				
				MapperObjectProperty.addOneOfToObjectPropertySchema(currentPropertySchema, individual.asOWLNamedIndividual().getIRI().getShortForm());
			}
		}
	}

	/**
	 * Convenience method for adding restriction values and ranges from a visit to {@link OWLNaryDataRange} (i.e. {@link OWLDataUnionOf} or {@link OWLDataIntersectionOf}).
	 * 
	 * @param ce the OWLNaryDataRange object
	 */
	private void visitOWLNaryDataRange(@Nonnull OWLNaryDataRange ce) {
		logger.info("Analyzing OWLNaryDataRange restrictions of Class: " + this.baseClass + " with axiom: " + ce);

		// If no existing property schema, then create empty schema for it.
		final var currentPropertySchema = this.getPropertySchemaForRestrictionVisit(this.currentlyProcessedPropertyName);

		currentPropertySchema.setItems(MapperDataProperty.getComplexDataComposedSchema(ce));
		MapperDataProperty.setSchemaType(currentPropertySchema, "array");
	}

	@Override
	public void visit(@Nonnull OWLDataUnionOf ce) {
		this.visitOWLNaryDataRange(ce);
	}

	@Override
	public void visit(@Nonnull OWLDataIntersectionOf ce) {
		this.visitOWLNaryDataRange(ce);
	}

	 /**
	 * Convenience method for adding restriction values and ranges from a visit to {@link OWLQuantifiedDataRestriction} 
	 * (i.e. {@link OWLDataAllValuesFrom}, {@link OWLDataSomeValuesFrom}, or
	 * {@link OWLDataCardinalityRestriction} [subinterfaces: {@link OWLDataMinCardinality}, {@link OWLDataMaxCardinality}, or {@link OWLDataExactCardinality}]).
	 * 
	 * @param ce the {@link OWLQuantifiedDataRestriction} object
	 */
	private void visitOWLQuantifiedDataRestriction(@Nonnull OWLQuantifiedDataRestriction dr) {
		logger.info("Analyzing restrictions of Class: " + this.baseClass + " with axiom: " + dr);

		// If no existing property schema, then create empty schema for it.
		final var currentPropertySchema = this.getPropertySchemaForRestrictionVisit(this.currentlyProcessedPropertyName);

		Integer restrictionValue = (dr instanceof OWLDataCardinalityRestriction) ? ((OWLDataCardinalityRestriction) dr).getCardinality() : null;

		final var ce = dr.getFiller();
		if (ce instanceof OWLDataUnionOf || ce instanceof OWLDataIntersectionOf || ce instanceof OWLDataOneOf) {
			ce.accept(this);
		} else {
			final var dataRestrictionRange = ce.asOWLDatatype().getIRI().getShortForm();

			// Update current property schema with the appropriate restriction datatype/value.
			if (dr instanceof OWLDataSomeValuesFrom) {
				MapperDataProperty.addAnyOfDataPropertySchema(currentPropertySchema, dataRestrictionRange);
			} else if (dr instanceof OWLDataAllValuesFrom) {
				MapperDataProperty.addAllOfDataPropertySchema(currentPropertySchema, dataRestrictionRange);
			} else if (dr instanceof OWLDataMinCardinality) {
				MapperDataProperty.addMinCardinalityToPropertySchema(currentPropertySchema, restrictionValue, dataRestrictionRange);
			} else if (dr instanceof OWLDataMaxCardinality) {
				MapperDataProperty.addMaxCardinalityToPropertySchema(currentPropertySchema, restrictionValue, dataRestrictionRange);
			} else if (dr instanceof OWLDataExactCardinality) {
				MapperDataProperty.addExactCardinalityToPropertySchema(currentPropertySchema, restrictionValue, dataRestrictionRange);
			}
		}
	}

	/**
	 * This method gets called when a class expression is a universal
	 * (allValuesFrom) restriction and it asks us to visit it
	 */
	@Override
	public void visit(@Nonnull OWLDataAllValuesFrom ce) {
		this.visitOWLQuantifiedDataRestriction(ce);
	}
	
	/**
	 * This method gets called when a class expression is a some
	 * (someValuesFrom) restriction and it asks us to visit it
	 */
	@Override
	public void visit(@Nonnull OWLDataSomeValuesFrom ce) {
		this.visitOWLQuantifiedDataRestriction(ce);
	}
	
	@Override
	public void visit(@Nonnull OWLDataMinCardinality ce) {
		this.visitOWLQuantifiedDataRestriction(ce);
	}
	
	@Override
	public void visit(@Nonnull OWLDataMaxCardinality ce) {
		this.visitOWLQuantifiedDataRestriction(ce);
	}
	
	@Override
	public void visit(@Nonnull OWLDataExactCardinality ce) {
		this.visitOWLQuantifiedDataRestriction(ce);
	}
	
	@Override
	public void visit(@Nonnull OWLDataOneOf ce) {
		logger.info("Analyzing restrictions of Class: " + this.baseClass + " with axiom: " + ce);

		ce.values().forEach((oneOfValue) -> {
			// If no existing property schema, then create empty schema for it.
			final var currentPropertySchema = this.getPropertySchemaForRestrictionVisit(this.currentlyProcessedPropertyName);

			MapperDataProperty.addOneOfDataPropertySchema(currentPropertySchema, oneOfValue);
		});
	}

	@Override
	public void visit(@Nonnull OWLDataComplementOf ce) {
		logger.info("Analyzing restrictions of Class: " + this.baseClass + " with axiom: " + ce);

		ce.datatypesInSignature().forEach((complementOfDatatype) -> {
			// If no existing property schema, then create empty schema for it.
			final var currentPropertySchema = this.getPropertySchemaForRestrictionVisit(this.currentlyProcessedPropertyName);

			MapperDataProperty.setComplementOfForDataSchema(currentPropertySchema, complementOfDatatype);
		});
	}
	
	@Override
	public void visit(@Nonnull OWLDataHasValue ce) {
		logger.info("Analyzing restrictions of Class: " + this.baseClass + " with axiom: " + ce);

		// If no existing property schema, then create empty schema for it.
		final var currentPropertySchema = this.getPropertySchemaForRestrictionVisit(this.currentlyProcessedPropertyName);
		
		MapperDataProperty.addHasValueOfPropertySchema(currentPropertySchema, ce.getFiller().getLiteral());
	}
}
