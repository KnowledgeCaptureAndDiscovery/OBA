package edu.isi.oba;

import static edu.isi.oba.Oba.logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.semanticweb.owlapi.util.IRIShortFormProvider;
import org.semanticweb.owlapi.util.SimpleIRIShortFormProvider;

/**
 * Visitor interface to inspect Class Restrictions
 * class Represents the OWLClass
 * onto  Represents the Ontology that contains the Class
 * owlThing Represents the visited class
 * propertyName Represents the name of a property that will be analyzed when this property has a restriction
 * 		   (e.g. it may be invoked from the getObjectProperties method of the MapperSchema). 
 * It defines values for the following global variables: 
 * 
 * - propertiesFromObjectRestrictions -> contains all the restricted OWLObjectProperties  
 * - propertiesFromObjectRestrictions_ranges -> contains the ranges of each object Restriction
 * - restrictionsValuesFromClass -> contains the restriction names of each Class Object and Data Restrictions and its values 
 *   e.g. [exactCardinality 5].  However, for restrictions that does not have a value a blank string is added, 
 *   e.g. [someValuesFrom ""]. It is worth noting that when an existential or universal restriction contains other 
 *   restriction it will be from this map and the nested restriction will be added including as its value the 
 *   restriction that contain the nested restriction e.g. [unionOf someValuesFrom]. The restrictionsValuesFromClass 
 *   will be used to map the ObjectProperties its the corresponding Schema.
 * - propertiesFromDataRestrictions -> contains all the restricted OWLDataProperties  
 * - propertiesFromDataRestrictions_ranges -> contains the ranges of each data property restriction    
 * - valuesFromDataRestrictions_ranges -> contains the range values of data restrictions. e.g. oneOf values.
 */

public class RestrictionVisitor implements OWLObjectVisitor {
	private final IRIShortFormProvider sfp = new SimpleIRIShortFormProvider();
	private final Set<OWLClass> processedClasses = new HashSet<OWLClass>();
	private final OWLClass cls;
	private final Set<OWLOntology> ontologies;
	String property_name;
	private OWLClass owlThing;
	
	private Map<String, Map<String,String>> restrictionsValuesFromClass = new HashMap<>();
	
	private Set<OWLObjectProperty> propertiesFromObjectRestrictions = new HashSet<>();
	private Map<String, Set<String>> propertiesFromObjectRestrictions_ranges = new HashMap<>();
	private Set<OWLDataProperty> propertiesFromDataRestrictions = new HashSet<>();
	private Map<String, Set<String>> propertiesFromDataRestrictions_ranges = new HashMap<>();
	private Set<String> valuesFromDataRestrictions_ranges = new HashSet<>();
	private Map<IRI, Set<String>> enums = new HashMap<>();

	/**
	 * Constructor for restriction visitor.
	 * 
	 * @param visitedClass the class being checked for restrictions
	 * @param ontologies the set of loaded ontologies (presumably the visited class is within one of these)
	 * @param propertyName
	 */
	RestrictionVisitor(OWLClass visitedClass, Set<OWLOntology> ontologies, String propertyName) {
		this.cls = visitedClass;
		this.ontologies = ontologies;
		this.property_name = propertyName;

		this.ontologies.forEach((ontology) -> {
			if (ontology.containsClassInSignature(this.cls.getIRI())) {
				this.owlThing = new StructuralReasonerFactory().createReasoner(ontology).getTopClassNode().getRepresentativeElement();
			}
		});
	}

	/**
	 * Convenience method for adding/updating restrictions to a property (i.e. in the {@link restrictionsValuesFromClass} map).
	 * 
	 * @param propertyName the property name to attach the restriction to
	 * @param restrictionKey the restriction's name/key
	 * @param restrictionValue the restriction's value
	 */
	private void addRestrictionValueToProperty(String propertyName, String restrictionKey, String restrictionValue) {
		Map<String, String> restrictions = new HashMap<>();

		if (!this.restrictionsValuesFromClass.containsKey(propertyName)) {
			logger.info("No restriction values for " + propertyName + " exist yet.  Creating now.");
		} else {
			restrictions = this.restrictionsValuesFromClass.get(propertyName);
		}

		if (restrictions.containsKey(restrictionKey)) {
			// What should happen here?  There is a chance they differ?
			logger.warning("Restriction value (= \"" + restrictionValue + "\") for " + propertyName + " already exists.  Ignoring...");
		} else {
			logger.info("Adding restriction <\"" + restrictionKey + "\", \"" + restrictionValue + "\"> to property \"" + propertyName + "\".\n");
			restrictions.put(restrictionKey, restrictionValue);
			this.restrictionsValuesFromClass.put(propertyName, restrictions);
		}
	}

	/**
	 * Convenience method for adding/updating object restriction ranges to a property (i.e. in the {@link propertiesFromObjectRestrictions_ranges} map).
	 * 
	 * @param propertyName the property name to attach the restriction range to
	 * @param range the object restriction range
	 */
	private void addObjectRestrictionRangeToProperty(String propertyName, String range) {
		Set<String> restrictionRanges = new HashSet<>();

		if (!this.propertiesFromObjectRestrictions_ranges.containsKey(propertyName)) {
			logger.info("No object restriction ranges for " + propertyName + " exist yet.  Creating now.");
		} else {
			restrictionRanges = this.propertiesFromObjectRestrictions_ranges.get(propertyName);
		}

		if (restrictionRanges.contains(range)) {
			// What should happen here?  There is a chance they differ?
			logger.warning("Restriction range (= \"" + range + "\") for " + propertyName + " already exists.  Ignoring...");
		} else {
			logger.info("Adding object restriction range \"" + range + "\" to property \"" + propertyName + "\".\n");
			restrictionRanges.add(range);
			this.propertiesFromObjectRestrictions_ranges.put(propertyName, restrictionRanges);
		}
	}

	/**
	 * Convenience method for adding/updating data restriction ranges to a property (i.e. in the {@link propertiesFromDataRestrictions_ranges} map).
	 * 
	 * @param propertyName the property name to attach the restriction range to
	 * @param range the data restriction range
	 */
	private void addDataRestrictionRangeToProperty(String propertyName, String range) {
		Set<String> restrictionRanges = new HashSet<>();

		if (!this.propertiesFromDataRestrictions_ranges.containsKey(propertyName)) {
			logger.info("No data restriction ranges for " + propertyName + " exist yet.  Creating now.");
		} else {
			restrictionRanges = this.propertiesFromDataRestrictions_ranges.get(propertyName);
		}

		if (restrictionRanges.contains(range)) {
			// What should happen here?  There is a chance they differ?
			logger.warning("Restriction range (= \"" + range + "\") for " + propertyName + " already exists.  Ignoring...");
		} else {
			logger.info("Adding object restriction range \"" + range + "\" to property \"" + propertyName + "\".\n");
			restrictionRanges.add(range);
			this.propertiesFromDataRestrictions_ranges.put(propertyName, restrictionRanges);
		}
	}

	@Override
        public void visit(@Nonnull OWLClass ce) {
            // avoid cycles
            if (!this.processedClasses.contains(ce)) {
                // If we are processing inherited restrictions then we recursively visit named supers.
                this.processedClasses.add(ce);

                for (OWLOntology ont: this.ontologies) {
                    ont.subClassAxiomsForSubClass(ce)
                        .forEach(ax -> ax.getSuperClass().accept(this));
                }
            }
        }

	@Override
	public void visit(@Nonnull OWLEquivalentClassesAxiom ce) {
		logger.info("Analyzing restrictions of Class: " + this.cls + " with axiom: " + ce);

		// If equivalent class axiom AND contains owl:oneOf, then we're looking at an ENUM class.
		ce.classExpressions().filter((e) -> e instanceof OWLObjectOneOf).forEach((oneOfObj) -> {
			var enumValues = ((OWLObjectOneOf) oneOfObj).getOperandsAsList();
			if (enumValues != null && !enumValues.isEmpty()) {
				// Add enum individuals to restriction range
				enumValues.forEach((indv) -> {
					this.addObjectRestrictionRangeToProperty(this.property_name, this.sfp.getShortForm(((OWLNamedIndividual) indv).getIRI()));
				});

				// For class enums, this is a misnomer.  There are no properties, in this case, and the property name will be an empty string.  Thi
				this.addRestrictionValueToProperty(this.property_name, "enum", "");
			}
		});

		// Loop through each expression in the equivalent classes axiom and accept visits from everything else.
		ce.classExpressions().filter((e) -> !(e instanceof OWLObjectOneOf)).forEach((e) -> {
			e.accept(this);
		});
	}

	/**
	 * Convenience method for adding restriction values and ranges from a visit to {@link OWLNaryBooleanClassExpression} (i.e. {@link OWLObjectUnionOf} or {@link OWLObjectIntersectionOf}).
	 * 
	 * @param ce the OWLNaryBooleanClassExpression object
	 */
	private void visitOWLNaryBooleanClassExpression(@Nonnull OWLNaryBooleanClassExpression ce) {
		logger.info("Analyzing restrictions of Class: " + this.cls + " with axiom: " + ce);

		String restrictionKey = (ce instanceof OWLObjectUnionOf) ? "unionOf" : "intersectionOf";

		this.addRestrictionValueToProperty(this.property_name, restrictionKey, "");
		
		// Loop through each item in the union/intersection and accept visits.
		for (OWLClassExpression e: ce.getOperands()) {
			if (e.isOWLClass()) {
				this.addObjectRestrictionRangeToProperty(this.property_name, this.sfp.getShortForm(e.asOWLClass().getIRI()));
			} else {
				e.accept(this);
			}
		}
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
		logger.info("Analyzing restrictions of Class: " + this.cls + " with axiom: " + or);

		this.property_name = this.sfp.getShortForm(or.getProperty().asOWLObjectProperty().getIRI());

		String restrictionKey = "";

		if (or instanceof OWLObjectAllValuesFrom) {
			restrictionKey = "allValuesFrom";
		} else if (or instanceof OWLObjectSomeValuesFrom) {
			restrictionKey = "someValuesFrom";
		} else if (or instanceof OWLObjectMinCardinality) {
			restrictionKey = "minCardinality";
		} else if (or instanceof OWLObjectMaxCardinality) {
			restrictionKey = "maxCardinality";
		} else if (or instanceof OWLObjectExactCardinality) {
			restrictionKey = "exactCardinality";
		}

		// If it is a cardinality type, set the restriction's value, otherwise an empty string.
		String restrictionValue = (or instanceof OWLObjectCardinalityRestriction) ? Integer.toString(((OWLObjectCardinalityRestriction) or).getCardinality()) : "";

		this.addRestrictionValueToProperty(this.property_name, restrictionKey, restrictionValue);

		final var ce = or.getFiller();
		if (ce instanceof OWLObjectUnionOf || ce instanceof OWLObjectIntersectionOf || ce instanceof OWLObjectOneOf) {
			ce.accept(this);
		} else {
			if (ce.asOWLClass().equals(this.owlThing)) {
				logger.info("Ignoring owl:Thing range" + this.property_name);
			} else {
				this.addObjectRestrictionRangeToProperty(this.property_name, this.sfp.getShortForm(ce.asOWLClass().getIRI()));
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
		logger.info("Analyzing restrictions of Class: " + this.cls + " with axiom: " + ce);

		String complementName = this.sfp.getShortForm(ce.getOperand().asOWLClass().getIRI());
		this.addRestrictionValueToProperty("complementOf", "complementOf", complementName);
 	}
	
	@Override
	public void visit(@Nonnull OWLObjectHasValue ce) {
		logger.info("Analyzing restrictions of Class: " + this.cls + " with axiom: " + ce);

		this.property_name = this.sfp.getShortForm(ce.getProperty().asOWLObjectProperty().getIRI());

		if (ce.getFiller() instanceof OWLObjectUnionOf || ce.getFiller() instanceof OWLObjectIntersectionOf) {
			ce.getFiller().accept(this);
		} else {
			for (OWLObjectProperty property: ce.getObjectPropertiesInSignature()) {
				this.ontologies.forEach((ontology) -> {
					// If object property has object(s) in its range, we want to set references to the object class.
					var obRangeAxioms = ontology.getObjectPropertyRangeAxioms(property);
					if (obRangeAxioms.isEmpty()) {
						logger.warning("\tObject has value (named individual) but there is no associated class/reference for the value.  Ontology may have errors.");
					} else {
						obRangeAxioms.forEach((obRangeAxiom) -> {
							obRangeAxiom.getRange().classesInSignature().forEach((owlClass) -> {
								this.addRestrictionValueToProperty(this.property_name, "objectHasReference", this.sfp.getShortForm(owlClass.getIRI()));
							});
						});
					}

					this.propertiesFromObjectRestrictions.add(property);
					this.addRestrictionValueToProperty(this.property_name, "objectHasValue", this.sfp.getShortForm(((OWLNamedIndividual) ce.getFiller()).getIRI()));
					this.addObjectRestrictionRangeToProperty(this.property_name, "defaultValue");
				});
			}
		}
	}
	
	@Override
	public void visit(@Nonnull OWLObjectOneOf ce) {
		logger.info("Analyzing restrictions of Class: " + this.cls + " with axiom: " + ce);

		if (!this.property_name.isBlank()) {
			for (OWLIndividual individual: ce.getIndividuals()) {
				this.addObjectRestrictionRangeToProperty(this.property_name, this.sfp.getShortForm(individual.asOWLNamedIndividual().getIRI()));
			}

			this.addRestrictionValueToProperty(this.property_name, "oneOf", "someValuesFrom");
		}
	}

	/**
	 * Convenience method for adding restriction values and ranges from a visit to {@link OWLNaryDataRange} (i.e. {@link OWLDataUnionOf} or {@link OWLDataIntersectionOf}).
	 * 
	 * @param ce the OWLNaryDataRange object
	 */
	private void visitOWLNaryDataRange(@Nonnull OWLNaryDataRange ce) {
		logger.info("Analyzing restrictions of Class: " + this.cls + " with axiom: " + ce);

		String restrictionKey = (ce instanceof OWLDataUnionOf) ? "unionOf" : "intersectionOf";

		this.addRestrictionValueToProperty(this.property_name, restrictionKey, "");
		
		// Loop through each item in the union/intersection and accept visits.
		for (OWLDataRange e: ce.getOperands()) {
			this.addDataRestrictionRangeToProperty(this.property_name, this.sfp.getShortForm(e.asOWLDatatype().getIRI()));
			e.accept(this);
		}
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
		logger.info("Analyzing restrictions of Class: " + this.cls + " with axiom: " + dr);

		this.property_name = this.sfp.getShortForm(dr.getProperty().asOWLDataProperty().getIRI());

		String restrictionKey = "";

		if (dr instanceof OWLDataAllValuesFrom) {
			restrictionKey = "allValuesFrom";
		} else if (dr instanceof OWLDataSomeValuesFrom) {
			restrictionKey = "someValuesFrom";
		} else if (dr instanceof OWLDataMinCardinality) {
			restrictionKey = "minCardinality";
		} else if (dr instanceof OWLDataMaxCardinality) {
			restrictionKey = "maxCardinality";
		} else if (dr instanceof OWLDataExactCardinality) {
			restrictionKey = "exactCardinality";
		}

		// If it is a cardinality type, set the restriction's value, otherwise an empty string.
		final String restrictionValue = (dr instanceof OWLDataCardinalityRestriction) ? Integer.toString(((OWLDataCardinalityRestriction) dr).getCardinality()) : "";

		this.addRestrictionValueToProperty(this.property_name, restrictionKey, restrictionValue);

		final var ce = dr.getFiller();
		if (ce instanceof OWLDataUnionOf || ce instanceof OWLDataIntersectionOf || ce instanceof OWLDataOneOf) {
			ce.accept(this);
		} else {
			this.addDataRestrictionRangeToProperty(this.property_name, this.sfp.getShortForm(ce.asOWLDatatype().getIRI()));
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
		logger.info("Analyzing restrictions of Class: " + this.cls + " with axiom: " + ce);

		for (OWLLiteral value: ce.getValues()) {
			this.valuesFromDataRestrictions_ranges.add(value.getLiteral());
			this.addRestrictionValueToProperty(this.property_name, "oneOf", "");
			this.addDataRestrictionRangeToProperty(this.property_name, this.sfp.getShortForm(value.getDatatype().getIRI()));
		}
	}

	@Override
	public void visit(@Nonnull OWLDataComplementOf ce) {
		logger.info("Analyzing restrictions of Class: " + this.cls + " with axiom: " + ce);

		for (OWLDatatype value: ce.getDatatypesInSignature()) {
			this.addRestrictionValueToProperty(this.property_name, "complementOf", "");
			this.addDataRestrictionRangeToProperty(this.property_name, this.sfp.getShortForm(value.getIRI()));
		}
	}
	
	@Override
	public void visit(@Nonnull OWLDataHasValue ce) {
		logger.info("Analyzing restrictions of Class: " + this.cls + " with axiom: " + ce);

		this.property_name = this.sfp.getShortForm(ce.getProperty().asOWLDataProperty().getIRI());
		
		this.addRestrictionValueToProperty(this.property_name, "dataHasValue", ce.getFiller().getLiteral());
			
		for (OWLDatatype value: ce.getDatatypesInSignature()) {
			this.addDataRestrictionRangeToProperty(this.property_name, this.sfp.getShortForm(value.getIRI()));
		}
	}
	
	/**
	 * Getter for {@link restrictionsValuesFromClass}.
	 * 
	 * @return a Map of property name keys with values that contain one or more restriction name keys and restriction values
	 */
	public Map<String, Map<String, String>> getRestrictionsValuesFromClass() {
		return this.restrictionsValuesFromClass;
	}

	/**
	 * Getter for {@link propertiesFromObjectRestrictions}.
	 * 
	 * @return a Set of OWL object properties for the class
	 */
	public Set<OWLObjectProperty> getPropertiesFromObjectRestrictions() {
		return this.propertiesFromObjectRestrictions;
	}

	/**
	 * Getter for {@link propertiesFromObjectRestrictions_ranges}.
	 * 
	 * @return a Map of OWL object property name keys with values that contain a Set of OWL object restrictions
	 */
	public Map<String, Set<String>> getPropertiesFromObjectRestrictions_ranges() {
		return this.propertiesFromObjectRestrictions_ranges;
	}
	
	/**
	 * Getter for {@link propertiesFromDataRestrictions}.
	 * 
	 * @return a Set of OWL data properties for the class
	 */
	public Set<OWLDataProperty> getPropertiesFromDataRestrictions() {
		return this.propertiesFromDataRestrictions;
	}
	
	/**
	 * Getter for {@link propertiesFromDataRestrictions_ranges}.
	 * 
	 * @return a Map of OWL object property name keys with values that contain a Set of OWL object restrictions
	 */
	public Map<String, Set<String>> getPropertiesFromDataRestrictions_ranges() {
		return this.propertiesFromDataRestrictions_ranges;
	}

	/**
	 * Getter for {@link valuesFromDataRestrictions_ranges}.
	 * 
	 * @return a Set of values from the OWL data restrictions
	 */
	public Set<String> getValuesFromDataRestrictions_ranges() {
		return this.valuesFromDataRestrictions_ranges;
	}

	/**
	 * Getter for a specific enumeration (enum).
	 * 
	 * @param classIRI an IRI of the enum name
	 * @return a Set of short names for enum individuals
	 */
	public Set<String> getEnums(IRI classIRI) {
		return this.enums.get(classIRI);
	}

	/**
	 * Getter for map of all enumerations (enums) (i.e. {@link enums}).
	 * 
	 * @return a Map of IRIs (enum names) keys with values which are a Set of short names for the enum individuals
	 */
	public Map<IRI, Set<String>> getAllEnums() {
		return this.enums;
	}
}
