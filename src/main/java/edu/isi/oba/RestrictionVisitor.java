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
 * @param class Represents the OWLClass
 * @param onto  Represents the Ontology that contains the Class
 * @param owlThing
 * @param propertyName Represents the name of a property that will be analyzed when this property has a restriction
 * 		   (e.g. it may be invoked from the getObjectProperties method of the MapperSchema). 
 * It defines values for the following global variables: 
 * 
 * - propertiesFromObjectRestrictions -> contains all the restricted OWLObjectProperties  
 * - propertiesFromObjectRestrictions_ranges -> contains the Ranges of each Class Object Restriction
 * - restrictionsValuesFromClass -> contains the restriction names of each Class Object Restriction and its values 
 *   e.g. [exactCardinality 5].  However, for restrictions that does not have a value a blank string is added, 
 *   e.g. [someValuesFrom ""]. It is worth noting that when an existential or universal restriction contains other 
 *   restriction it will be from this map and the nested restriction will be added including as its value the 
 *   restriction that contain the nested restriction e.g. [unionOf someValuesFrom]. The restrictionsValuesFromClass 
 *   will be used to map the ObjectProperties its the corresponding Schema.
 *                                                       
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



	RestrictionVisitor(OWLClass clas, OWLOntology onto, OWLClass owlThing, String propertyName ) {
		processedClasses = new HashSet<OWLClass>();
		this.cls=clas;
		this.onto=onto;
		this.property_name=propertyName;           
		this.owlThing=owlThing;   
		this.propertiesFromObjectRestrictions_ranges= new HashMap<>();
		this.propertiesFromObjectRestrictions = new ArrayList<>();
		
		this.restrictionsValuesFromClass = new HashMap<>();

	}

	@Override
	public void visit(OWLClass ce) {
		if (!processedClasses.contains(ce)) {
			// If we are processing inherited restrictions then we recursively visit named supers. 
			processedClasses.add(ce);
			for (OWLSubClassOfAxiom ax : onto.getSubClassAxiomsForSubClass(ce)) {
				ax.getSuperClass().accept(this);                        
			}               
		}
	}

	@Override
	public void visit(OWLObjectSomeValuesFrom ce) {
		// This method gets called when a class expression is an existential
		// (someValuesFrom) restriction and it asks us to visit it
		Map<String, String> restrictionsValues = new HashMap<String, String>();
		logger.info("Analyzing restrictions of Class: " + this.cls+ " with axiom: "+ce);
		
		for(OWLObjectProperty property:ce.getObjectPropertiesInSignature()) {        		   
			propertiesFromObjectRestrictions.add(property);
			property_name = sfp.getShortForm(property.getIRI());        		
		}
		if (ce.getFiller() instanceof OWLObjectUnionOf || ce.getFiller() instanceof OWLObjectIntersectionOf) {
			
			restrictionsValues.put("someValuesFrom", "");			
			restrictionsValuesFromClass.put(property_name, restrictionsValues);
			ce.getFiller().accept(this);
		} else {       	   
			List<String> ranges = new ArrayList<String>();
			ranges.add(sfp.getShortForm(ce.getFiller().asOWLClass().getIRI()));
			if (ce.getFiller().asOWLClass().equals(owlThing)) {
				logger.info("Ignoring owl:Thing range" + property_name);                       
			}
			else
				propertiesFromObjectRestrictions_ranges.put(property_name,ranges);        		  
			
			restrictionsValues.put("someValuesFrom", "");			
			restrictionsValuesFromClass.put(property_name, restrictionsValues);
		}
	}
	@Override
	public void visit(OWLObjectAllValuesFrom ce) {
		// This method gets called when a class expression is a universal
		// (allValuesFrom) restriction and it asks us to visit it
		logger.info("Analyzing restrictions of Class: " + this.cls+ " with axiom: "+ce);
		Map<String, String> restrictionsValues = new HashMap<String, String>();
		for(OWLObjectProperty property:ce.getObjectPropertiesInSignature()) {        		   
			propertiesFromObjectRestrictions.add(property);
			property_name = sfp.getShortForm(property.getIRI()); 		
		}
		if (ce.getFiller() instanceof OWLObjectUnionOf || ce.getFiller() instanceof OWLObjectIntersectionOf) {
			ce.getFiller().accept(this);
		} else {       	   
			List<String> ranges = new ArrayList<String>();
			ranges.add(sfp.getShortForm(ce.getFiller().asOWLClass().getIRI()));
			if (ce.getFiller().asOWLClass().equals(owlThing)) {
				logger.info("Ignoring owl:Thing range" + property_name);                       
			}
			else
				propertiesFromObjectRestrictions_ranges.put(property_name,ranges);        		  

			restrictionsValues.put("allValuesFrom", "");			
			restrictionsValuesFromClass.put(property_name, restrictionsValues);
		}
	}
	
	
	@Override
	public void visit( OWLObjectUnionOf ce ) {		 							    	
		logger.info("Analyzing restrictions of Class: " + this.cls+ " with axiom: "+ce);
		Set<OWLClassExpression> ranges = ce.getOperands();
		Set<OWLObjectProperty> properties = ce.getObjectPropertiesInSignature();
		setBooleanCombinationProperties(ranges,  properties, "unionOf");
	}
	@Override
	public void visit( OWLObjectIntersectionOf ce ) {		 							    	
		logger.info("Analyzing restrictions of Class: " + this.cls+ " with axiom: "+ce);  
		Set<OWLClassExpression> ranges = ce.getOperands();
		Set<OWLObjectProperty> properties = ce.getObjectPropertiesInSignature();
		setBooleanCombinationProperties(ranges, properties, "intersectionOf");
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
		logger.info("Analyzing restrictions of Class: " + this.cls+ " with axiom: "+ce);
		Map<String, String> restrictionsValues = new HashMap<String, String>();
		for(OWLObjectProperty property:ce.getObjectPropertiesInSignature()) {
			property_name = sfp.getShortForm(property.getIRI());		
			restrictionsValues.put("objectHasValue",ce.getValue().toString());	
			restrictionsValuesFromClass.put(property_name, restrictionsValues);
		}
	}
	
	
	// TODO: add validation for all the dataProperties e.g. OWLDataAllValuesFrom
	@Override
	public void visit( OWLDataAllValuesFrom ce ) {


	}
	@Override
	public void visit( OWLDataSomeValuesFrom ce ) {

	}
	
	@Override
	public void visit( OWLDataUnionOf ce ) {

	}
	
	@Override
	public void visit( OWLDataIntersectionOf ce ) {

	}
	
	@Override
	public void visit( OWLDataMinCardinality ce ) {
		

	}
	
	@Override
	public void visit( OWLDataMaxCardinality ce ) {
		 			
	} 
	
	@Override
	public void visit( OWLDataExactCardinality ce ) {
		

	}
	
	@Override
	public void visit( OWLDataHasValue ce ) {
		logger.info("Analyzing restrictions of Class: " + this.cls+ " with axiom: "+ce);
		Map<String, String> restrictionsValues = new HashMap<String, String>();		
		for(OWLObjectProperty property:ce.getObjectPropertiesInSignature()) {
			property_name = sfp.getShortForm(property.getIRI());
			OWLLiteral value=ce.getValue();
			restrictionsValues.put("dataHasValue",value.getLiteral());	
			restrictionsValuesFromClass.put(property_name, restrictionsValues);
		}
	}

	 /**
     * Method that given a class expression of any cardinality type will set it and its value.
     * @param ce object cardinality value e.g. OWLObjectExactCardinality
     * @param retriction string value of restriction e.g. "exactCardinality"
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
		Boolean inspect=true;
		Map<String, String> restrictionsValues = new HashMap<String, String>();
		for (OWLClassExpression value :ranges) {
			//if operands from boolean restriction contain complex combinations the restriction on this property
			// will be ignored                   		 
			if (value instanceof OWLObjectIntersectionOf || value instanceof OWLObjectComplementOf || value instanceof OWLObjectSomeValuesFrom || value instanceof OWLObjectAllValuesFrom 
					|| value instanceof OWLObjectHasValue) {                    			                    		        				
				logger.warning("Ignoring complex range restriction of property "+ property_name);				
				if (property_name!="") {
					propertiesFromObjectRestrictions.remove(property_name);	
					restrictionsValuesFromClass.remove(property_name);
				}
				inspect=false;
			} 
		}
		// if there are not complex range restrictions inspect ce
		if (inspect) {
			//if the expression ce is not part of a composed expression (existencial or universal)
			if (property_name=="") { 
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
				
				if (restrictionsValuesFromClass.size()!=0) {
					for (String j :  restrictionsValuesFromClass.keySet()) { 
						if (j==property_name) {							
							Map<String,String> valor=restrictionsValuesFromClass.get(property_name);
							for (String i : valor.keySet() ) { 
								restrictionsValues.put(restriction, i);
								restrictionsValuesFromClass.remove(property_name);
								restrictionsValuesFromClass.put(property_name, restrictionsValues);
							}
						}
					}
				}
				else {
					restrictionsValues.put(restriction, "");			
					restrictionsValuesFromClass.put(property_name, restrictionsValues);
				}
			}       
		}
	}

	public Map<String, Map<String,String>> getRestrictionsValuesFromClass(){
		return restrictionsValuesFromClass;
	}


	public List<OWLObjectProperty> getPropertiesFromObjectRestrictions(){
		return propertiesFromObjectRestrictions;
	}

	public Map<String, List<String>> getPropertiesFromObjectRestrictions_ranges(){
		return  propertiesFromObjectRestrictions_ranges;
	}
}
