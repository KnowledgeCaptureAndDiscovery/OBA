package edu.isi.oba;

import static edu.isi.oba.Oba.logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.semanticweb.owlapi.search.Searcher;
import org.semanticweb.owlapi.util.IRIShortFormProvider;
import org.semanticweb.owlapi.util.SimpleIRIShortFormProvider;

import edu.isi.oba.config.CONFIG_FLAG;
import edu.isi.oba.config.YamlConfig;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;

/**
 * Visits existential restrictions and collects the properties which are restricted.
 */
public class ObjectVisitor implements OWLObjectVisitor {

	private OWLReasoner reasoner;
	private OWLReasonerFactory reasonerFactory;
	private OWLClass owlThing;

	private final Set<String> propertyNames = new HashSet<>();
	private final Set<String> requiredProperties = new HashSet<>();
	private final Set<String> functionalProperties = new HashSet<>();
	private final Set<OWLClass> referencedClasses = new HashSet<>();
	private final Set<OWLClass> processedClasses = new HashSet<>();
	private final Set<OWLOntology> ontologies;

	private OWLClass baseClass;
	private OWLOntology ontologyOfBaseClass;
	private Schema classSchema;

	private final IRIShortFormProvider sfp = new SimpleIRIShortFormProvider();

	private final YamlConfig configData;

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

	private void initializeBaseClass(OWLClass baseClass) {
		// If the base class is already set, ignore.
		if (this.baseClass == null) {
			this.baseClass = baseClass;
			final var visitedClassIRI = this.baseClass.getIRI();

			this.reasonerFactory = new StructuralReasonerFactory();

			// We can pragmatically determine the class's ontology based on the set of ontologies and the class itself.  Also set the owl:Thing for that ontology.
			this.ontologies.stream().takeWhile(ontology -> ontology.containsClassInSignature(visitedClassIRI)).forEach((ontology) -> {
				this.ontologyOfBaseClass = ontology;
				this.reasoner = reasonerFactory.createReasoner(ontology);
				this.owlThing = this.reasoner.getTopClassNode().getRepresentativeElement();
			});

			this.classSchema = this.getBaseClassBasicSchema();
		}
	}

	private Schema getBaseClassBasicSchema() {
		var basicClassSchema = new Schema();
		basicClassSchema.setName(this.getClassName());
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
	 * Although somewhat convoluted, this schema will not be generated fully until the {@link #visit(@Nonnull OWLClass ce)} method has been called by the base OWLClass to accept this visitor class.
	 * 
	 * @see {@link #visit(@Nonnull OWLClass ce)}
	 * @return a {@link Schema} for the entire class
	 */
	public Schema getClassSchema() {
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
				// Search the ontology for this OWLClass.
				// If it has subclass axioms, then loop through each to accept visits for all super classes.
				ontology.subClassAxiomsForSubClass(ce).forEach(ax -> {
					/** Only traverse super classes for inheriting properties.  Restrictions handled via {@link generatePropertySchemasWithRestrictions()} below. */
					if (ax.getSuperClass().isOWLClass()) {
						ax.getSuperClass().accept(this);
					}
				});
			}

			// Only include properties from the base class.  Inherited details should be determined via references.
			//if (this.baseClass.equals(ce)) {
				// Get all (including inherited) object and data properties.
				this.initializeObjectPropertySchemasForClass(ce);
				this.initializeDataPropertySchemasForClass(ce);
			// } else {
			// 	// If the class has a type (likely "object" - are there any other possibilities??), it needs to be removed and added to the "allOf" entries.
			// 	this.classSchema.setType(null);

			// 	// If adding for the first time, need to include a "type: object" entry.
			// 	if (this.classSchema.getAllOf() == null || this.classSchema.getAllOf().isEmpty()) {
			// 		final var objSchema = new ObjectSchema();
			// 		this.classSchema.addAllOfItem(objSchema);
			// 	}

			// 	// Only add the reference if it does not already exist.  (OpenAPI generation will create multiple entries, even if identical).
			// 	var canAddAllOfReference = false;

			// 	for (final var allOfItem: this.classSchema.getAllOf()) {
			// 		canAddAllOfReference = ((Schema) allOfItem).get$ref() == null;
			// 	}

			// 	if (canAddAllOfReference) {
			// 		final var refSchema = new ObjectSchema();
			// 		refSchema.set$ref(ce.getIRI().getShortForm());

			// 		this.classSchema.addAllOfItem(refSchema);
			// 	}
			// }
		}
		
		// Now generate all the restrictions.
		this.generatePropertySchemasWithRestrictions(ce);

		// Generate the required properties for the class, if applicable.
		if (this.configData.getConfigFlagValue(CONFIG_FLAG.REQUIRED_PROPERTIES_FROM_CARDINALITY)) {
			this.generateRequiredPropertiesForClassSchemas();
		}

		// Convert non-array property items, if applicable.
		if (!this.configData.getConfigFlagValue(CONFIG_FLAG.ALWAYS_GENERATE_ARRAYS)) {
			MapperProperty.convertArrayToNonArrayPropertySchemas(this.classSchema, this.functionalProperties);
		}
	}

	private void generatePropertySchemasWithRestrictions(OWLClass owlClass) {
		logger.info("  =============================Generating restrictions for:   " + this.baseClass);

		// Clear out the processed classes from the OWLClass visitor method, and re-check everything for restrictions.
		this.processedClasses.clear();

		// Avoid cycles and accept visits from super classes for the purpose of getting all properties.
		if (!this.processedClasses.contains(owlClass)) {
			// If we are processing inherited restrictions then we recursively visit named supers.
			this.processedClasses.add(owlClass);

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
							this.currentlyProcessedPropertyName = this.sfp.getShortForm(property.getIRI());
							
							// Add any classes referenced by the restriction.
							this.referencedClasses.addAll(ax.getSuperClass().getClassesInSignature());
						} else if (ax.getSuperClass() instanceof OWLBooleanClassExpression) {
							if (ax.getSuperClass() instanceof OWLObjectComplementOf) {
								// Add the object complement reference class.
								this.referencedClasses.addAll(ax.getSuperClass().getClassesInSignature());

								logger.info("\t" + this.getClassName() + " has an object complement of axiom.  This is not for a property, so do not set property name.  Axiom:   " + ax);
							} else {
								logger.severe("\t" + this.getClassName() + " has unknown restriction.  Axiom:   " + ax);
								shouldSkipVisits = true;
							}
						} else if (ax.getSuperClass() instanceof OWLObjectOneOf) {
							logger.info("\t" + this.getClassName() + " is an ObjectOneOf set containing one or more Individuals.  Not setting propety name, to treat it like an enum.  Axiom:  " + ax);
						} else {
							logger.info("\t" + this.getClassName() + " is a subclass of " + this.sfp.getShortForm(ax.getSuperClass().asOWLClass().getIRI()) + ".  No restrictions to process.  Axiom:  " + ax);
							shouldSkipVisits = true;
						}

						if (!shouldSkipVisits) {
							// Proceed with the visit.
							ax.getSuperClass().accept(this);
						}

						// Clear out the property name.
						this.currentlyProcessedPropertyName = null;
					} else {
						logger.severe("\t" + this.getClassName() + " has unknown restriction.  Axiom:   " + ax);
					}
				});

				// For equivalent (to) classes (e.g. Defined classes) we need to accept the visit to navigate it.
				ontology.equivalentClassesAxioms(owlClass).forEach((eqClsAx) -> {
					eqClsAx.accept(this);
				});
			}
		}
	}

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

	private String getClassName() {
		return this.sfp.getShortForm(this.baseClass.getIRI());
	}

	/**
     * Read the Ontology, obtain the ObjectProperties, obtain the range for each property and generate the SchemaProperty
     */
	private void initializeObjectPropertySchemasForClass(OWLClass owlClass) {
		final var objPropDomainAxioms = new HashSet<OWLObjectPropertyDomainAxiom>();
		for (OWLOntology ontology: this.ontologies) {
			objPropDomainAxioms.addAll(ontology.getAxioms(AxiomType.OBJECT_PROPERTY_DOMAIN));
		}

		// For the class's properties, check each axiom where the axiom's domain is a class AND the current class equals the domain.
		objPropDomainAxioms.stream().filter(objPropDomainAx -> objPropDomainAx.getDomain().isOWLClass() && owlClass.equals(objPropDomainAx.getDomain())).forEach((objPropDomainAx) -> {
			logger.info( "\tParsing object property domain axiom: " + objPropDomainAx.toString());

			// Loop through each object property and generate its schema
			objPropDomainAx.objectPropertiesInSignature().forEach((op) -> {
				final var propertyName = this.sfp.getShortForm(op.getIRI());
				this.propertyNames.add(propertyName);
				this.currentlyProcessedPropertyName = propertyName;

				logger.info( "\t\tClass: \"" + this.getClassName() + "\"  -  Property: \"" + propertyName + "\"");

				final var propertyRanges = new HashSet<String>();
				final var complexObjectRanges = new HashSet<OWLClassExpression>();
				this.ontologyOfBaseClass.objectPropertyRangeAxioms(op).forEach((objPropRangeAxiom) -> {
					if (objPropRangeAxiom.getRange() instanceof OWLClass) {
						if (this.configData.getConfigFlagValue(CONFIG_FLAG.FOLLOW_REFERENCES)) {
							propertyRanges.add(objPropRangeAxiom.getRange().asOWLClass().getIRI().getShortForm());

							// Add the range to the referenced class set.
							this.referencedClasses.add(objPropRangeAxiom.getRange().asOWLClass());
						} else {
							propertyRanges.add(null);
						}
					} else {
						complexObjectRanges.add(objPropRangeAxiom.getRange());
					}
				});

				if (propertyRanges.isEmpty()) {
					logger.warning("\t\tProperty \"" + op.getIRI() + "\" has range equals zero.");
				} else {
					logger.info( "\t\tProperty range(s): " + propertyRanges);
				}
				
				try {
					final var propertyDescription = ObaUtils.getDescription(op, this.ontologies, this.configData.getConfigFlagValue(CONFIG_FLAG.DEFAULT_DESCRIPTIONS));

					// Get data property schema from mapper.
					final var objPropertySchema = MapperObjectProperty.createObjectPropertySchema(propertyName, propertyDescription, propertyRanges);

					// If property is functional, set the schema accordingly.
					if (EntitySearcher.isFunctional(op, Collections.singleton(this.ontologyOfBaseClass).stream())) {
						this.functionalProperties.add(propertyName);
						MapperProperty.setFunctionalForPropertySchema(objPropertySchema);
					}

					// Save object property schema to class's schema.
					this.classSchema.addProperty(objPropertySchema.getName(), objPropertySchema);

					// For any complex property ranges, traverse.  This will grab restrictions also.  There is great way for this situation to grab only the types in this situation.
					if (!complexObjectRanges.isEmpty()) {
						complexObjectRanges.forEach((objectRange) -> {
							objectRange.accept(this);
						});
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

				this.currentlyProcessedPropertyName = null;
			});
		});
    }

	/**
     * Obtain a map of Codegen properties of a OWLClass
     */
	private void initializeDataPropertySchemasForClass(OWLClass owlClass) {
		final var dataPropDomainAxioms = new HashSet<OWLDataPropertyDomainAxiom>();
		for (OWLOntology ontology: this.ontologies) {
			dataPropDomainAxioms.addAll(ontology.getAxioms(AxiomType.DATA_PROPERTY_DOMAIN));
		}
		
		// For the class's properties, check each axiom where the axiom's domain is a class AND the current class equals the domain.
		dataPropDomainAxioms.stream().filter(dataPropDomainAx -> dataPropDomainAx.getDomain().isOWLClass() && owlClass.equals(dataPropDomainAx.getDomain())).forEach((dataPropDomainAx) -> {
			logger.info( "\tParsing data property domain axiom: " + dataPropDomainAx.toString());

			// Loop through each data property and generate its schema.
			dataPropDomainAx.dataPropertiesInSignature().forEach((dp) -> {
				final var propertyName = this.sfp.getShortForm(dp.getIRI());
				this.propertyNames.add(propertyName);
				this.currentlyProcessedPropertyName = propertyName;

				logger.info( "\t\tClass: \"" + this.getClassName() + "\"  -  Property: \"" + propertyName + "\"");

				final var propertyRanges = new HashSet<String>();
				final var complexDataRanges = new HashSet<OWLDataRange>();
				this.ontologyOfBaseClass.dataPropertyRangeAxioms(dp).forEach((dataPropRangeAxiom) -> {
					if (dataPropRangeAxiom.getRange() instanceof OWLDatatype) {
						propertyRanges.add(((OWLDatatype) dataPropRangeAxiom.getRange()).getIRI().getShortForm());
					} else {
						complexDataRanges.add(dataPropRangeAxiom.getRange());
					}
				});

				if (propertyRanges.isEmpty()) {
					logger.warning("\t\tProperty \"" + dp.getIRI() + "\" has range equals zero.");
				} else {
					logger.info( "\t\tProperty range(s): " + propertyRanges);
				}

				try {
					final var propertyDescription = ObaUtils.getDescription(dp, this.ontologies, this.configData.getConfigFlagValue(CONFIG_FLAG.DEFAULT_DESCRIPTIONS));

					// Get data property schema from mapper.
					final var dataPropertySchema = MapperDataProperty.createDataPropertySchema(propertyName, propertyDescription, propertyRanges);

					// If property is functional, set the schema accordingly.
					if (EntitySearcher.isFunctional(dp, Collections.singleton(this.ontologyOfBaseClass).stream())) {
						this.functionalProperties.add(propertyName);
						MapperProperty.setFunctionalForPropertySchema(dataPropertySchema);
					}

					// Save object property schema to class's schema.
					this.classSchema.addProperty(dataPropertySchema.getName(), dataPropertySchema);

					// For any complex property ranges, traverse.  This will grab restrictions also.  There is great way for this situation to grab only the types in this situation.
					if (!complexDataRanges.isEmpty()) {
						complexDataRanges.forEach((dataRange) -> {
							dataRange.accept(this);
						});
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

				this.currentlyProcessedPropertyName = null;
			});
		});
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
		MapperProperty.setNullableValueForPropertySchema(idPropertySchema, false);
        final var labelPropertySchema = MapperDataProperty.createDataPropertySchema("label", "short description of the resource", new HashSet<String>(){{add("string");}});
		MapperProperty.setNullableValueForPropertySchema(labelPropertySchema, true);
        final var typePropertySchema = MapperDataProperty.createDataPropertySchema("type", "type(s) of the resource", new HashSet<String>(){{add("string");}});
		MapperProperty.setNullableValueForPropertySchema(typePropertySchema, true);
		final var descriptionPropertySchema = MapperDataProperty.createDataPropertySchema("description", "small description", new HashSet<String>(){{add("string");}});
		MapperProperty.setNullableValueForPropertySchema(descriptionPropertySchema, true);
		
		// Also add some default property examples of different types (e.g. a date/time, a boolean, and a float)
		final var eventDateTimePropertySchema = MapperDataProperty.createDataPropertySchema("eventDateTime", "a date/time of the resource", new HashSet<String>(){{add("dateTime");}});
		MapperProperty.setNullableValueForPropertySchema(eventDateTimePropertySchema, true);
		final var isBoolPropertySchema = MapperDataProperty.createDataPropertySchema("isBool", "a boolean indicator of the resource", new HashSet<String>(){{add("boolean");}});
		MapperProperty.setNullableValueForPropertySchema(isBoolPropertySchema, true);
		final var quantityPropertySchema = MapperDataProperty.createDataPropertySchema("quantity", "a number quantity of the resource", new HashSet<String>(){{add("float");}});
		MapperProperty.setNullableValueForPropertySchema(quantityPropertySchema, true);

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

	private Schema getPropertySchema(String propertyName) {
		var currentPropertySchema = this.classSchema.getProperties() == null ? new Schema() : (Schema) this.classSchema.getProperties().get(propertyName);

		// In certain cases, a property was not set up with domains/ranges but has a restriction.
		// This property will not exist in the map of property names + schemas yet, so add it and set it up with basic info.
		if (currentPropertySchema == null) {
			currentPropertySchema = new ObjectSchema();
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
					MapperObjectProperty.addEnumValueToObjectSchema(this.classSchema, this.sfp.getShortForm(((OWLNamedIndividual) indv).getIRI()));
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
		final var currentPropertySchema = this.getPropertySchema(this.currentlyProcessedPropertyName);

		currentPropertySchema.setItems(MapperObjectProperty.getComplexObjectComposedSchema(ce));

		// Make sure to update the class's property schema.
		this.classSchema.addProperty(this.currentlyProcessedPropertyName, currentPropertySchema);
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
			final var objRestrictionRange = this.configData.getConfigFlagValue(CONFIG_FLAG.FOLLOW_REFERENCES) ? this.sfp.getShortForm(ce.asOWLClass().getIRI()) : null;

			// If no existing property schema, then create empty schema for it.
			final var currentPropertySchema = this.getPropertySchema(this.currentlyProcessedPropertyName);

			// Update current property schema with the appropriate restriction range/value.
			if (or instanceof OWLObjectSomeValuesFrom) {
				MapperObjectProperty.addAnyOfToObjectPropertySchema(currentPropertySchema, objRestrictionRange);
			} else if (or instanceof OWLObjectAllValuesFrom) {
				MapperObjectProperty.addAllOfToObjectPropertySchema(currentPropertySchema, objRestrictionRange);
			} else if (or instanceof OWLObjectMinCardinality) {
				MapperProperty.addMinCardinalityToPropertySchema(currentPropertySchema, restrictionValue);
			} else if (or instanceof OWLObjectMaxCardinality) {
				MapperProperty.addMaxCardinalityToPropertySchema(currentPropertySchema, restrictionValue);
			} else if (or instanceof OWLObjectExactCardinality) {
				MapperProperty.addExactCardinalityToPropertySchema(currentPropertySchema, restrictionValue);
			}

			// Make sure to update the class's property schema.
			this.classSchema.addProperty(this.currentlyProcessedPropertyName, currentPropertySchema);
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
			MapperObjectProperty.setComplementOfForObjectSchema(this.classSchema, this.sfp.getShortForm(ce.getOperand().asOWLClass().getIRI()));
		} else {
			// If no existing property schema, then create empty schema for it.
			final var currentPropertySchema = this.getPropertySchema(this.currentlyProcessedPropertyName);

			MapperObjectProperty.setComplementOfForObjectSchema(currentPropertySchema, this.sfp.getShortForm(ce.getOperand().asOWLClass().getIRI()));

			// Make sure to update the class's property schema.
			this.classSchema.addProperty(this.currentlyProcessedPropertyName, currentPropertySchema);
		}
 	}
	
	@Override
	public void visit(@Nonnull OWLObjectHasValue ce) {
		logger.info("Analyzing restrictions of Class: " + this.baseClass + " with axiom: " + ce);

		// If no existing property schema, then create empty schema for it.
		final var currentPropertySchema = this.getPropertySchema(this.currentlyProcessedPropertyName);
		
		MapperProperty.addHasValueOfPropertySchema(currentPropertySchema, this.sfp.getShortForm(((OWLNamedIndividual) ce.getFiller()).getIRI()));

		// Make sure to update the class's property schema.
		this.classSchema.addProperty(this.currentlyProcessedPropertyName, currentPropertySchema);
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
					MapperObjectProperty.addEnumValueToObjectSchema(this.classSchema, this.sfp.getShortForm(((OWLNamedIndividual) indv).getIRI()));
				});
			}
		} else {
			for (OWLIndividual individual: ce.getIndividuals()) {
				// If no existing property schema, then create empty schema for it.
				final var currentPropertySchema = this.getPropertySchema(this.currentlyProcessedPropertyName);
				
				MapperObjectProperty.addOneOfToObjectPropertySchema(currentPropertySchema, this.sfp.getShortForm(individual.asOWLNamedIndividual().getIRI()));

				// Make sure to update the class's property schema.
				this.classSchema.addProperty(this.currentlyProcessedPropertyName, currentPropertySchema);
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
		final var currentPropertySchema = this.getPropertySchema(this.currentlyProcessedPropertyName);

		currentPropertySchema.setItems(MapperDataProperty.getComplexDataComposedSchema(ce));

		// Make sure to update the class's property schema.
		this.classSchema.addProperty(this.currentlyProcessedPropertyName, currentPropertySchema);
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
		final var currentPropertySchema = this.getPropertySchema(this.currentlyProcessedPropertyName);

		Integer restrictionValue = (dr instanceof OWLDataCardinalityRestriction) ? ((OWLDataCardinalityRestriction) dr).getCardinality() : null;

		final var ce = dr.getFiller();
		if (ce instanceof OWLDataUnionOf || ce instanceof OWLDataIntersectionOf || ce instanceof OWLDataOneOf) {
			ce.accept(this);
		} else {
			final var dataRestrictionRange = this.sfp.getShortForm(ce.asOWLDatatype().getIRI());

			// Update current property schema with the appropriate restriction datatype/value.
			if (dr instanceof OWLDataSomeValuesFrom) {
				MapperDataProperty.addAnyOfDataPropertySchema(currentPropertySchema, dataRestrictionRange);
			} else if (dr instanceof OWLDataAllValuesFrom) {
				MapperDataProperty.addAllOfDataPropertySchema(currentPropertySchema, dataRestrictionRange);
			} else if (dr instanceof OWLDataMinCardinality) {
				MapperProperty.addMinCardinalityToPropertySchema(currentPropertySchema, restrictionValue);
			} else if (dr instanceof OWLDataMaxCardinality) {
				MapperProperty.addMaxCardinalityToPropertySchema(currentPropertySchema, restrictionValue);
			} else if (dr instanceof OWLDataExactCardinality) {
				MapperProperty.addExactCardinalityToPropertySchema(currentPropertySchema, restrictionValue);
			}

			// Make sure to update the class's property schema.
			this.classSchema.addProperty(this.currentlyProcessedPropertyName, currentPropertySchema);
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
			final var currentPropertySchema = this.getPropertySchema(this.currentlyProcessedPropertyName);

			MapperDataProperty.addOneOfDataPropertySchema(currentPropertySchema, oneOfValue);

			// Make sure to update the class's property schema.
			this.classSchema.addProperty(this.currentlyProcessedPropertyName, currentPropertySchema);
		});
	}

	@Override
	public void visit(@Nonnull OWLDataComplementOf ce) {
		logger.info("Analyzing restrictions of Class: " + this.baseClass + " with axiom: " + ce);

		ce.datatypesInSignature().forEach((complementOfDatatype) -> {
			// If no existing property schema, then create empty schema for it.
			final var currentPropertySchema = this.getPropertySchema(this.currentlyProcessedPropertyName);

			MapperDataProperty.setComplementOfForDataSchema(currentPropertySchema, complementOfDatatype);

			// Make sure to update the class's property schema.
			this.classSchema.addProperty(this.currentlyProcessedPropertyName, currentPropertySchema);
		});
	}
	
	@Override
	public void visit(@Nonnull OWLDataHasValue ce) {
		logger.info("Analyzing restrictions of Class: " + this.baseClass + " with axiom: " + ce);

		// If no existing property schema, then create empty schema for it.
		final var currentPropertySchema = this.getPropertySchema(this.currentlyProcessedPropertyName);
		
		MapperProperty.addHasValueOfPropertySchema(currentPropertySchema, ce.getFiller().getLiteral());

		// Make sure to update the class's property schema.
		this.classSchema.addProperty(this.currentlyProcessedPropertyName, currentPropertySchema);
	}
}
