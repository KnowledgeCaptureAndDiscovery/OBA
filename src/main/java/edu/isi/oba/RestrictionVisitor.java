package edu.isi.oba;

import static edu.isi.oba.Oba.logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.*;
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
	private final Set<OWLClass> processedClasses;
	private final OWLClass cls;
	private final OWLOntology onto;
	String property_name;
	OWLClass owlThing;
	
	public Map<String, Map<String,String>> restrictionsValuesFromClass;
	
	public List<OWLObjectProperty> propertiesFromObjectRestrictions;
	public Map<String, List<String>> propertiesFromObjectRestrictions_ranges;
	public List<OWLDataProperty> propertiesFromDataRestrictions;
	public Map<String, List<String>> propertiesFromDataRestrictions_ranges;
	public List<String> valuesFromDataRestrictions_ranges;
	public Map<IRI, List<String>> enums;

	RestrictionVisitor(OWLClass visitedClass, OWLOntology onto, OWLClass owlThing, String propertyName ) {
		processedClasses = new HashSet<OWLClass>();
		this.cls = visitedClass;
		this.onto = onto;
		this.property_name = propertyName;           
		this.owlThing = owlThing;   
		this.propertiesFromObjectRestrictions_ranges= new HashMap<>();
		this.propertiesFromObjectRestrictions = new ArrayList<>();		
		this.restrictionsValuesFromClass = new HashMap<>();
		this.propertiesFromDataRestrictions = new ArrayList<>();
		this.propertiesFromDataRestrictions_ranges= new HashMap<>();
		this.valuesFromDataRestrictions_ranges = new ArrayList<>();
		this.enums = new HashMap<>();
	}

	@Override
	public void visit(OWLClass ce) {
		if (!this.processedClasses.contains(ce)) {
			// If we are processing inherited restrictions then we recursively visit named supers. 
			this.processedClasses.add(ce);
			for (OWLSubClassOfAxiom ax: this.onto.getSubClassAxiomsForSubClass(ce)) {
				ax.getSuperClass().accept(this);
			}               
		}
	}

	@Override
	public void visit( OWLEquivalentClassesAxiom ce ) {		 							    	
		logger.info("Analyzing restrictions of Class: " + this.cls+ " with axiom: "+ce);

		// If equivalent class axiom AND contains owl:oneOf, then we're looking at an ENUM class.
		ce.classExpressions().filter((e) -> e instanceof OWLObjectOneOf).forEach((oneOfObj) -> {
			var enumValues = new ArrayList<String>();
			((OWLObjectOneOf) oneOfObj).getOperandsAsList().forEach((indv) -> {
				enumValues.add(this.sfp.getShortForm(((OWLNamedIndividual) indv).getIRI()));
			});

			if (!enumValues.isEmpty()) {
				this.enums.put(this.cls.getIRI(), enumValues);
			}
		});

		// Loop through each expression in the equivalent classes axiom and accept visits from everything else.
		ce.classExpressions().filter((e) -> !(e instanceof OWLObjectOneOf)).forEach((e) -> {
			e.accept(this);
		});
	}

	/**
	 * This method gets called when a class expression is an existential
	 * (someValuesFrom) restriction and it asks us to visit it
	 */
	@Override
	public void visit(OWLObjectSomeValuesFrom ce) {
		Map<String, String> restrictionsValues = new HashMap<>();
		logger.info("Analyzing restrictions of Class: " + this.cls + " with axiom: " + ce);

		for(OWLObjectProperty property: ce.getObjectPropertiesInSignature()) {
			this.propertiesFromObjectRestrictions.add(property);
			this.property_name = this.sfp.getShortForm(property.getIRI());        		
		}

		OWLClassExpression ceFiller = ce.getFiller();
		if (ceFiller instanceof OWLObjectUnionOf || ceFiller instanceof OWLObjectIntersectionOf || ceFiller instanceof OWLObjectOneOf) {
			restrictionsValues.put("someValuesFrom", "");
			this.restrictionsValuesFromClass.put(this.property_name, restrictionsValues);
			ce.getFiller().accept(this);
		} else {
			if (ceFiller.asOWLClass().equals(owlThing)) {
				logger.info("Ignoring owl:Thing range" + this.property_name);
			} else {
				if (this.propertiesFromObjectRestrictions_ranges.containsKey(this.property_name)) {
					this.propertiesFromObjectRestrictions_ranges.get(this.property_name).add(this.sfp.getShortForm(ceFiller.asOWLClass().getIRI()));
				} else {
					List<String> ranges = new ArrayList<>();
					ranges.add(this.sfp.getShortForm(ceFiller.asOWLClass().getIRI()));
					this.propertiesFromObjectRestrictions_ranges.put(this.property_name, ranges);
				}
			}
			
			restrictionsValues.put("someValuesFrom", "");
			this.restrictionsValuesFromClass.put(this.property_name, restrictionsValues);
		}
	}
	
	/**
	 * This method gets called when a class expression is a universal 
	 * (allValuesFrom) restriction and it asks us to visit it
	 */
	@Override
	public void visit(OWLObjectAllValuesFrom ce) {
		logger.info("Analyzing restrictions of Class: " + this.cls+ " with axiom: "+ce);
		Map<String, String> restrictionsValues = new HashMap<>();
		for(OWLObjectProperty property:ce.getObjectPropertiesInSignature()) {        		   
			propertiesFromObjectRestrictions.add(property);
			property_name = sfp.getShortForm(property.getIRI()); 		
		}
		if (ce.getFiller() instanceof OWLObjectUnionOf || ce.getFiller() instanceof OWLObjectIntersectionOf) {
			ce.getFiller().accept(this);
		} else {       	   
			List<String> ranges = new ArrayList<>();
			ranges.add(sfp.getShortForm(ce.getFiller().asOWLClass().getIRI()));
			if (ce.getFiller().asOWLClass().equals(owlThing)) {
				logger.info("Ignoring owl:Thing range" + property_name);                       
			} else {
				propertiesFromObjectRestrictions_ranges.put(property_name, ranges);
			}

			restrictionsValues.put("allValuesFrom", "");			
			restrictionsValuesFromClass.put(property_name, restrictionsValues);
		}
	}
		
	@Override
	public void visit( OWLObjectUnionOf ce ) {		 							    	
		logger.info("Analyzing restrictions of Class: " + this.cls+ " with axiom: "+ce);
		
		// Loop through each item in the union and accept visits.
		for (OWLClassExpression e : ce.getOperands()) {
			e.accept(this);
		}
	}
	@Override
	public void visit( OWLObjectIntersectionOf ce ) {		 							    	
		logger.info("Analyzing restrictions of Class: " + this.cls+ " with axiom: "+ce);
		
		// Loop through each item in the intersection and accept visits.
		for (OWLClassExpression e : ce.getOperands()) {
			e.accept(this);
		}
	}

	@Override
	public void visit( OWLObjectMinCardinality ce ) {
		logger.info("Analyzing restrictions of Class: " + this.cls+ " with axiom: "+ce);
		setPropertiesWithCardinality(ce, "minCardinality");
		
	}
	@Override
	public void visit( OWLObjectMaxCardinality ce ) {
		logger.info("Analyzing restrictions of Class: " + this.cls+ " with axiom: "+ce);	 
		setPropertiesWithCardinality(ce,"maxCardinality")	;
	} 
	@Override
	public void visit( OWLObjectExactCardinality ce ) {
		logger.info("Analyzing restrictions of Class: " + this.cls+ " with axiom: "+ce);
		setPropertiesWithCardinality(ce,  "exactCardinality")	;
	}
	
	@Override
	public void visit( OWLObjectComplementOf ce ) {
		logger.info("Analyzing restrictions of Class: " + this.cls+ " with axiom: "+ce);
		Map<String, String> restrictionsValues = new HashMap<String, String>();
		String complementName = this.sfp.getShortForm(ce.getOperand().asOWLClass().getIRI());					
		restrictionsValues.put("complementOf", complementName );	
		restrictionsValuesFromClass.put("complementOf", restrictionsValues);
 	}
	
	@Override
	public void visit(OWLObjectHasValue ce) {
		logger.info("Analyzing restrictions of Class: " + this.cls + " with axiom: " + ce);
		
		for(OWLObjectProperty property: ce.getObjectPropertiesInSignature()) {
			List<String> ranges = new ArrayList<String>();
			Map<String, String> restrictionsValues = new HashMap<String, String>();

			this.property_name = this.sfp.getShortForm(property.getIRI());

			restrictionsValues.put("objectHasValue", this.sfp.getShortForm(((OWLNamedIndividual)ce.getFiller()).getIRI()));

			// If object property has object(s) in its range, we want to set references to the object class.
			var obRangeAxioms = this.onto.getObjectPropertyRangeAxioms(property);
			if (obRangeAxioms.isEmpty()) {
				logger.warning("\tObject has value (named individual) but there is no associated class/reference for the value.  Ontology may have errors.");
			} else {
				obRangeAxioms.forEach((obRangeAxiom) -> {
					obRangeAxiom.getRange().classesInSignature().forEach((owlClass) -> {
						restrictionsValues.put("objectHasReference", this.sfp.getShortForm(owlClass.getIRI()));
					});
				});
			}

			this.restrictionsValuesFromClass.put(this.property_name, restrictionsValues);
			this.propertiesFromObjectRestrictions.add(property);
			ranges.add("defaultValue");
			this.propertiesFromObjectRestrictions_ranges.put(this.property_name, ranges);
		}
	}
	
	@Override
	public void visit( OWLObjectOneOf ce ) {	
		logger.info("Analyzing restrictions of Class: " + this.cls + " with axiom: " + ce);
		List<String> ranges = new ArrayList<String>();
		Map<String, String> restrictionsValues = new HashMap<String, String>();

		if (!property_name.isEmpty()) {
			for (OWLIndividual individual: ce.getIndividuals()) {
				ranges.add(individual.toString());
			}

			restrictionsValues.put("oneOf", "someValuesFrom");
			this.restrictionsValuesFromClass.put(this.property_name, restrictionsValues);
			this.propertiesFromObjectRestrictions_ranges.put(this.property_name, ranges);
		}
	}
	
	/**
	 * This method gets called when a class expression is a universal
	 * (allValuesFrom) restriction and it asks us to visit it
	 */
	@Override
	public void visit( OWLDataAllValuesFrom ce ) {		
		logger.info( "\n Analyzed Class: " + this.cls+ " with axiom: "+ce);
		Map<String, String> restrictionsValues = new HashMap<String, String>();
		for(OWLDataProperty property:ce.getDataPropertiesInSignature()) {        		   
			propertiesFromDataRestrictions.add(property);
			property_name = sfp.getShortForm(property.getIRI());        		
		}
		if (ce.getFiller() instanceof OWLDataUnionOf || ce.getFiller() instanceof OWLDataIntersectionOf) {
			ce.getFiller().accept(this);
		} else {       	   
			List<String> ranges = new ArrayList<String>();
			ranges.add(sfp.getShortForm(ce.getFiller().asOWLDatatype().getIRI()));
			if (ce.getFiller().asOWLDatatype().equals(owlThing)) {
				logger.info("Ignoring owl:Thing range" + property_name);                       
			}
			else
				propertiesFromDataRestrictions_ranges.put(property_name,ranges);        		  

			restrictionsValues.put("allValuesFrom", "");			
			restrictionsValuesFromClass.put(property_name, restrictionsValues);		
		}
	}
	
	/**
	 * This method gets called when a class expression is a some
	 * (someValuesFrom) restriction and it asks us to visit it
	 */
	@Override
	public void visit( OWLDataSomeValuesFrom ce ) {
		logger.info( "\n Analyzed Class: " + this.cls + " with axiom: " + ce);
		Map<String, String> restrictionsValues = new HashMap<String, String>();
		for(OWLDataProperty property: ce.getDataPropertiesInSignature()) {        		   
			this.propertiesFromDataRestrictions.add(property);
			this.property_name = this.sfp.getShortForm(property.getIRI());
		}

		OWLDataRange ceFiller = ce.getFiller();
		if (ceFiller instanceof OWLDataUnionOf || ceFiller instanceof OWLDataIntersectionOf) {
			restrictionsValues.put("someValuesFrom", "");			
			this.restrictionsValuesFromClass.put(property_name, restrictionsValues);
			ce.getFiller().accept(this);
		} else {       	   
			List<String> ranges = new ArrayList<String>();
			ranges.add(this.sfp.getShortForm(ceFiller.asOWLDatatype().getIRI()));

			if (ceFiller.asOWLDatatype().equals(owlThing)) {
				logger.info("Ignoring owl:Thing range" + property_name);                       
			} else {
				propertiesFromDataRestrictions_ranges.put(property_name, ranges);  
			}      		  

			restrictionsValues.put("someValuesFrom", "");
			this.restrictionsValuesFromClass.put(property_name, restrictionsValues);
		}
	}

	@Override
	public void visit( OWLDataUnionOf ce ) {
		logger.info("Analyzing restrictions of Class: " + this.cls+ " with axiom: "+ce);
		
		// Loop through each item in the union and accept visits.
		for (OWLDataRange e : ce.getOperands()) {
			e.accept(this);
		}
	}
	
	@Override
	public void visit( OWLDataIntersectionOf ce ) {
		logger.info("Analyzing restrictions of Class: " + this.cls+ " with axiom: "+ce);

		// Loop through each item in the intersection and accept visits.
		for (OWLDataRange e : ce.getOperands()) {
			e.accept(this);
		}
	}
	
	@Override
	public void visit( OWLDataMinCardinality ce ) {
		logger.info("Analyzing restrictions of Class: " + this.cls+ " with axiom: "+ce);		
		setDataPropertiesWithCardinality(ce,"minCardinality");
	}
	
	@Override
	public void visit( OWLDataMaxCardinality ce ) {
		logger.info("Analyzing restrictions of Class: " + this.cls+ " with axiom: "+ce);	
		setDataPropertiesWithCardinality(ce,"maxCardinality");
	} 
	
	@Override
	public void visit( OWLDataExactCardinality ce ) {
		logger.info("Analyzing restrictions of Class: " + this.cls+ " with axiom: "+ce);		
		setDataPropertiesWithCardinality(ce,"exactCardinality");
	}
	
	@Override
	public void visit( OWLDataOneOf ce ) {	
		logger.info("Analyzing restrictions of Class: " + this.cls + " with axiom: " + ce);	
		List<String> ranges = new ArrayList<String>();
		Map<String, String> restrictionsValues = new HashMap<String, String>();

		for (OWLLiteral value: ce.getValues()) {
			ranges.add(this.sfp.getShortForm(value.getDatatype().getIRI()));
			this.valuesFromDataRestrictions_ranges.add(value.getLiteral());
			restrictionsValues.put("oneOf", "");
			this.restrictionsValuesFromClass.put(this.property_name, restrictionsValues);
			this.propertiesFromDataRestrictions_ranges.put(this.property_name, ranges);
		}
	}

	@Override
	public void visit( OWLDataComplementOf ce ) {
		logger.info("Analyzing restrictions of Class: " + this.cls + " with axiom: " + ce);
		List<String> ranges = new ArrayList<String>();
		Map<String, String> restrictionsValues = new HashMap<String, String>();					
		for (OWLDatatype value: ce.getDatatypesInSignature()) {
			ranges.add(this.sfp.getShortForm(value.getIRI()));
			restrictionsValues.put("complementOf", "");
			restrictionsValuesFromClass.put(this.property_name, restrictionsValues);
			propertiesFromDataRestrictions_ranges.put(this.property_name, ranges);
		}
	}
	
	@Override
	public void visit( OWLDataHasValue ce ) {
		logger.info("Analyzing restrictions of Class: " + this.cls + " with axiom: " + ce);
		List<String> ranges = new ArrayList<String>();
		Map<String, String> restrictionsValues = new HashMap<String, String>();
		for(OWLDataProperty property: ce.getDataPropertiesInSignature()) {
			property_name = this.sfp.getShortForm(property.getIRI());
			propertiesFromDataRestrictions.add(property);
			restrictionsValues.put("dataHasValue", ce.getFiller().getLiteral());
			restrictionsValuesFromClass.put(property_name, restrictionsValues);
			
			for (OWLDatatype value: ce.getDatatypesInSignature()) {
				ranges.add(this.sfp.getShortForm(value.getIRI()));
				propertiesFromDataRestrictions_ranges.put(this.property_name, ranges);
			}
		}
	}

	 /**
     * Method that given a class expression of any cardinality type will set it and its value.
     * @param ce object cardinality value e.g. OWLObjectExactCardinality
     * @param restriction string value of restriction e.g. "exactCardinality"
     */
	public void setPropertiesWithCardinality(OWLObjectCardinalityRestriction ce, String restriction) {
		String property_name="";
		List<String> ranges = new ArrayList<String>();
		Map<String, String> restrictionsValues = new HashMap<String, String>();
		for(OWLObjectProperty property:ce.getObjectPropertiesInSignature()) {
			propertiesFromObjectRestrictions.add(property);
			property_name = sfp.getShortForm(property.getIRI());    	
			ranges.add(sfp.getShortForm(ce.getFiller().asOWLClass().getIRI()));
			propertiesFromObjectRestrictions_ranges.put(property_name,ranges);
		}	    
		this.property_name =  property_name;
		
		restrictionsValues.put(restriction, Integer.toString(ce.getCardinality()));			
		restrictionsValuesFromClass.put(property_name, restrictionsValues);
	}
	
	 /**
     * Method that given a set of ranges and properties will set them according to the boolean restriction .
     * @param ranges a set of class ranges that compose the boolean restriction
     * @param properties object properties expression that have the boolean restriction 
     * @param restriction string value of restriction e.g. "unionOf"
     */
	public void setBooleanCombinationProperties( Set<OWLClassExpression> ranges, Set<OWLObjectProperty> properties, String restriction) {
		Map<String, String> restrictionsValues = new HashMap<String, String>();
			//if the expression ce is not part of a composed expression (existential or universal)
			if (property_name.isEmpty()) {
				for(OWLObjectProperty property:properties) {
					propertiesFromObjectRestrictions.add(property);
					property_name = sfp.getShortForm(property.getIRI());
					restrictionsValues.put(restriction, "");			
					restrictionsValuesFromClass.put(property_name, restrictionsValues);
				}
			} else { 
				List<String> rangeList = new ArrayList<String>();
				for (OWLClassExpression range:ranges) {  
					rangeList.add(sfp.getShortForm(range.asOWLClass().getIRI()));
					if (range.asOWLClass().equals(owlThing)) {
						logger.info("Ignoring owl:Thing range" + property_name);                       
					}
					else {						
						propertiesFromObjectRestrictions_ranges.put(property_name,rangeList);							
					} 					
				}
				
				if (!restrictionsValuesFromClass.isEmpty()) {
					for (String j :  restrictionsValuesFromClass.keySet()) { 
						if (j.equals(property_name)) {
							Map<String,String> value = restrictionsValuesFromClass.get(property_name);
							for (String i : value.keySet() ) {
								restrictionsValues.put(restriction, i);
							}
							restrictionsValuesFromClass.put(property_name, restrictionsValues);
						}
					}
				} else {
					restrictionsValues.put(restriction, "");			
					restrictionsValuesFromClass.put(property_name, restrictionsValues);
				}
			}
	}
	
	 /**
     * Method that given a set of ranges and properties will set them according to the boolean restriction .
     * @param ranges a set of class ranges that compose the boolean restriction
     * @param properties data properties expression that have the boolean restriction 
     * @param restriction string value of restriction e.g. "unionOf"
     */
	public void setBooleanCombinationDataProperties( Set<OWLDataRange> ranges, Set<OWLDataProperty> properties, String restriction) {
		Map<String, String> restrictionsValues = new HashMap<String, String>();
			//if the expression ce is not part of a composed expression (existential or universal)
			if (property_name.equals("")) {
				for(OWLDataProperty property:properties) {
					propertiesFromDataRestrictions.add(property);
					property_name = sfp.getShortForm(property.getIRI());
					restrictionsValues.put(restriction, "");			
					restrictionsValuesFromClass.put(property_name, restrictionsValues);
				}
			} else { 
				List<String> rangeList = new ArrayList<String>();
				for (OWLDataRange range:ranges) {  
					rangeList.add(sfp.getShortForm(range.asOWLDatatype().getIRI()));
					if (range.getClass().equals(owlThing)) {
						logger.info("Ignoring owl: Thing range" + property_name);
					}
					else {						
						propertiesFromDataRestrictions_ranges.put(property_name,rangeList);							
					} 					
				}				
				if (!restrictionsValuesFromClass.isEmpty()) {
					for (String j :  restrictionsValuesFromClass.keySet()) { 
						if (j.equals(property_name)) {
							Map<String,String> value = restrictionsValuesFromClass.get(property_name);
							for (String i : value.keySet()) {
								restrictionsValues.put(restriction, i);
							}
							restrictionsValuesFromClass.put(property_name, restrictionsValues);
						break;
						}
					}
				}
				else {
					restrictionsValues.put(restriction, "");			
					restrictionsValuesFromClass.put(property_name, restrictionsValues);
				}
			}
	}

	public void setDataPropertiesWithCardinality(OWLDataCardinalityRestriction ce, String restriction) {
		String property_name="";
		List<String> ranges = new ArrayList<>();
		Map<String, String> restrictionsValues = new HashMap<>();
		for(OWLDataProperty property:ce.getDataPropertiesInSignature()) {
			propertiesFromDataRestrictions.add(property);
			property_name = sfp.getShortForm(property.getIRI());    				
			for (OWLDatatype value:ce.getDatatypesInSignature()){
				ranges.add(sfp.getShortForm(value.getIRI()));
				propertiesFromDataRestrictions_ranges.put(property_name,ranges);
			}
		}	    
		this.property_name = property_name;
		restrictionsValues.put(restriction, Integer.toString(ce.getCardinality()));			
		restrictionsValuesFromClass.put(property_name, restrictionsValues);
	}
	
	public Map<String, Map<String,String>> getRestrictionsValuesFromClass() {
		return this.restrictionsValuesFromClass;
	}

	public List<OWLObjectProperty> getPropertiesFromObjectRestrictions() {
		return this.propertiesFromObjectRestrictions;
	}

	public Map<String, List<String>> getPropertiesFromObjectRestrictions_ranges() {
		return this.propertiesFromObjectRestrictions_ranges;
	}
		
	public List<OWLDataProperty> getPropertiesFromDataRestrictions() {
		return this.propertiesFromDataRestrictions;
	}
	
	public List<String> getValuesFromDataRestrictions_ranges() {
		return this.valuesFromDataRestrictions_ranges;
	}
	
	public Map<String, List<String>> getPropertiesFromDataRestrictions_ranges() {
		return this.propertiesFromDataRestrictions_ranges;
	}

	public List<String> getEnums(IRI classIRI) {
		return this.enums.get(classIRI);
	}

	public Map<IRI, List<String>> getAllEnums() {
		return this.enums;
    }
}
