import java.io.File;
import java.util.*;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLXMLOntologyFormat;
import org.semanticweb.owlapi.io.StreamDocumentTarget;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLIndividualAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectExactCardinality;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.reasoner.FreshEntitiesException;
import org.semanticweb.owlapi.reasoner.InconsistentOntologyException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.ReasonerInterruptedException;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.semanticweb.owlapi.reasoner.TimeOutException;
import org.semanticweb.owlapi.vocab.PrefixOWLOntologyFormat;

import uk.ac.manchester.cs.factplusplus.owlapiv3.FaCTPlusPlusReasonerFactory;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectUnionOfImpl;


public class OntologyOperations {

	private static OWLOntologyManager manager = null;
	private static OWLOntology PoContology = null;
	private static PrefixOWLOntologyFormat pm = null;
	private static List<X3DObject> listOfX3DObjects = null;
	private static OWLReasoner reasoner = null;

	
	//Constructor
	public OntologyOperations(File ontologyFile, List<X3DObject> list) {
		initClassVariables(ontologyFile, list);
	    createIndividuals();
	    infer();

	}

	  
	private void initClassVariables(File file, List<X3DObject> list) {
		//An ontology manager for ontology operations
	    System.out.println("_________________ INITIALISATION DE L'ONTOLOGIE__________________");
	    manager = OWLManager.createOWLOntologyManager();
		System.out.println("Création du manager -> " + manager.toString());
		
	    //The list to be populated by individuals
		listOfX3DObjects = list;
		
	    //Load of the l'ontology
	    try {
			PoContology = manager.loadOntologyFromOntologyDocument(file);
		} catch (OWLOntologyCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
	    System.out.println("Chargement de l'ontologie -> " +  PoContology.getOntologyID());
	    System.out.println("Ontologie Up and Ready!");
	    System.out.println("_________________________________________________________");
	    
	    //Set the prefix used in querying classes
	    pm = (PrefixOWLOntologyFormat) manager.getOntologyFormat(PoContology);
	    pm.setDefaultPrefix(PoContology.getOntologyID().getOntologyIRI().toString()); 
	    
	    // We need a reasoner to query and classify the ontology
	    OWLReasonerFactory reasonerFactory = new FaCTPlusPlusReasonerFactory();    
	    reasoner = reasonerFactory.createReasoner(PoContology, new SimpleConfiguration());	
	}
	
	public static OWLOntologyManager getManager () {
		return manager;
	}
	
	public static PrefixOWLOntologyFormat getPrefixManager () {
		return pm;
	}
	public static OWLReasoner getReasoner () {
		return reasoner;
	}
	
	private static void infer() throws InconsistentOntologyException, FreshEntitiesException, ReasonerInterruptedException, TimeOutException {

        Set<OWLClass> theBigThing = reasoner.getTopClassNode().getEntities();
        System.out.println("Top class Node is: " + reasoner.getTopClassNode().getEntities().toString());
        OWLClassExpression rootClass = (OWLClassExpression) theBigThing.iterator().next(); //Premier de la liste est forcément ":Thing"

        OWLReasonerFactory reasonerFactory = new FaCTPlusPlusReasonerFactory();
        
        OWLReasoner reasoner = reasonerFactory.createReasoner(PoContology, new SimpleConfiguration());

        System.out.println("Top class Node is: " + reasoner.getTopClassNode().getEntities().toString());
 
        Set<OWLNamedIndividual> setOfIndividuals = reasoner.getInstances(rootClass, false).getFlattened();
        
        System.out.println("NOMBRE d'ind = " + setOfIndividuals.size());
        
        Iterator<OWLNamedIndividual> itMe = setOfIndividuals.iterator();
        
        while (itMe.hasNext()) {
        	OWLNamedIndividual currentIndividual = itMe.next();
        	X3DObject object = findX3DObject(currentIndividual.getIRI());
        	object.setInferedTypes(reasoner.getTypes(currentIndividual, true));
       	
            System.out.println("X3DObject ID is    ---> " + object.getID());
        	System.out.println("Individual is   ---> " + currentIndividual);
            System.out.println("Infered type is ---> " + object.getInferedTypes());

            System.out.println("");
            System.out.println("");
        }
        
        // Après avoir inférer l'ensemble des classes d'objets, on peut tenter d'identifier la scène 3D.
	    // On crée donc un individu 'global3DScene' qui va être défini par 
	    // autant de propriétés 'isDefinedBy' que de classes inférées.
	    // ceci afin de pouvoir ensuite classer cet individu dans les classes 'Sites' (Bathroom, Livingroom, etc.)
	    OWLDataFactory factory = manager.getOWLDataFactory();
	    OWLNamedIndividual global3DScene = factory.getOWLNamedIndividual(IRI.create(pm.getDefaultPrefix() + "global3DScene"));
	    System.out.println("LA SCENE S'appelle  " + global3DScene.getIRI());
	    OWLObjectProperty isDefinedBy = factory.getOWLObjectProperty(IRI.create(pm.getDefaultPrefix() + "isDefinedBy")); 
	    System.out.println("La property est -> " + isDefinedBy.getIRI());
	    //Le set de cardinalities (owlclassExpression) qui va définir la scene 3D
	    HashSet<OWLClassExpression> SetOfCardinalities = new HashSet<OWLClassExpression>();
	    
        Iterator<X3DObject> iterateX3DObjects = listOfX3DObjects.iterator();
        while(iterateX3DObjects.hasNext()) {
        	X3DObject currentObject = iterateX3DObjects.next();
        	
        	OWLClassExpression currentObjectType = currentObject.getInferedTypes().iterator().next().getEntities().iterator().next();

        	// Si le type n'est pas "Thing" alors on ajoute la classe trouvée au type de l'individu 'global3DScene'
        	if ( currentObjectType != reasoner.getTopClassNode().getEntities().iterator().next() )
        		{
	        	OWLObjectExactCardinality cardinality = factory.getOWLObjectExactCardinality(1, isDefinedBy, currentObjectType);     	
	        	System.out.println("Cardinality is -> " + cardinality.toString());
	        	SetOfCardinalities.add(cardinality);	
        		}
        	}
	    
    	OWLObjectIntersectionOf intersectionOfObjectTypes = factory.getOWLObjectIntersectionOf(SetOfCardinalities);
    	//OWLObjectAllValuesFrom allValuesFromIntersection = factory.getOWLObjectAllValuesFrom(isDefinedBy, intersectionOfObjectTypes);
    	//SetOfCardinalities.add(allValuesFromIntersection);
        OWLIndividualAxiom global3DSceneAxiom = factory.getOWLClassAssertionAxiom(intersectionOfObjectTypes, global3DScene);
        
        // Add the axiom to the ontology. May use AddAxioms(Set<OWLIndividualAxioms>) to do so if multiple axioms
        AddAxiom addglobal3DSceneAxiom = new AddAxiom(PoContology, global3DSceneAxiom);
        
        // Then, use the manager class to apply the change
        manager.applyChange(addglobal3DSceneAxiom);

        try {
			manager.saveOntology(PoContology);
		} catch (OWLOntologyStorageException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static X3DObject findX3DObject(IRI individualIri) {
		X3DObject correspondingObject = null;
		String iri = individualIri.toString();
		
		for (int i=0; i < listOfX3DObjects.size(); i++) {
			X3DObject currentObj = listOfX3DObjects.get(i);
			if ( iri.contains(currentObj.getID())) {
				correspondingObject = currentObj;
				break;
			}
		}
		
		return correspondingObject;
	}
	
	private static void createIndividuals() {
		 
	    System.out.println("_________________ MISE A JOUR DE L'ONTOLOGIE__________________");
	    //Instanciate a factory class to help instanciate OWL classes
	    OWLDataFactory factory = manager.getOWLDataFactory();
	    PrefixOWLOntologyFormat pm = (PrefixOWLOntologyFormat) manager.getOntologyFormat(PoContology);

	    	    
		//Référence sur la propriété 'isComposedOf'
        OWLObjectProperty isComposedOf = factory.getOWLObjectProperty(IRI.create(pm.getDefaultPrefix() + "isComposedOf")); 
        
        
		for(int i=0; i < listOfX3DObjects.size(); i++) {
			
			//RECUPERATION DE L'OBJET COURANT DANS LA LISTE DES X3DObjects
			X3DObject currentObject = listOfX3DObjects.get(i);
			
			//INSTANCIATION D'UN OWLINDIVIDUAL
			OWLNamedIndividual individual = factory.getOWLNamedIndividual(IRI.create(pm.getDefaultPrefix() + listOfX3DObjects.get(i).getID()));
			//System.out.println("ID de l'objet est -> " + listOfX3DObjects.get(i).getID());
			//System.out.println("Individual créé avec l'IRI is -> " + individual.getIRI());

	      	//
	        //TRAITEMENT DES TYPES
			//On construit pour chaque individu le type suivant
			//Exemple pour une table:
			//
			//ObjectIntersectionOf(
			//		ObjectAllValuesFrom ( #isComposedOf	ObjectUnionOf(#Plane #SolidTubular))
			//		ObjectExactCardinality(1 #isComposedOf #Plane)
			//		ObjectExactCardinality(4 #isComposedOf #SolidTubular)
	        //)
			
			//On va stocker chacune des OWLclass 'Abstractions' correspondantes aux abstractions de l'objet3D
			HashSet<OWLClassExpression> SetOfAbstractionClass = new HashSet<OWLClassExpression>();//as OWLClassExpression pour les utiliser dans une union
			List<OWLClassExpression> abstractionsClassList = new ArrayList<OWLClassExpression>();//Contournement pour éviter d'utiliser HashSet.Iterator.next
			HashSet<OWLClassExpression> SetOfCardinalityClass = new HashSet<OWLClassExpression>();
			
			//Création des références sur les classes des abstractions
			for ( int j=0; j < currentObject.getAbstractions().size(); j++) {
				OWLClass anAbsIRI = factory.getOWLClass(IRI.create(pm.getDefaultPrefix()+currentObject.getAbstraction(j).getAbstractionName()));
				SetOfAbstractionClass.add(anAbsIRI);
				abstractionsClassList.add(anAbsIRI);
				//System.out.println("Pour l'objet " + currentObject.getID() + " -> AbsIRI = " + anAbsIRI.getIRI());
			}
			
     
	        
	        //Création des cardinalités
	        for ( int j=0; j < SetOfAbstractionClass.size(); j++) {
	        	int cardinalityValue = currentObject.getAbstraction(j).getCardinality();
	        	OWLObjectExactCardinality cardinality = factory.getOWLObjectExactCardinality(cardinalityValue, isComposedOf, abstractionsClassList.get(j));
	        	SetOfCardinalityClass.add(cardinality);
	        	//System.out.println("La cardinalité de " + currentObject.getAbstraction(j).getAbstractionName() + " est " + cardinality.getCardinality());
	        }	        
			
		
	        OWLObjectUnionOfImpl union = new OWLObjectUnionOfImpl(SetOfAbstractionClass);
	
	        OWLObjectAllValuesFrom allValuesFromUnion = factory.getOWLObjectAllValuesFrom(isComposedOf, union);
	        
	        //Intersection entre les cardinality et le résultat de l'union 
	        //On peut ajouter allValuesFromUnion à listOfCardinality car les deux types sont "CLassExpression".
	        OWLObjectIntersectionOf intersection = null;
	        
	        SetOfCardinalityClass.add(allValuesFromUnion);
	        intersection = factory.getOWLObjectIntersectionOf(SetOfCardinalityClass );
	        
	        //Assert the axiom defining the type
	        OWLIndividualAxiom ax = factory.getOWLClassAssertionAxiom(intersection, individual);
	        
	        //Add the axiom to the ontology. May use AddAxioms(Set<OWLIndividualAxioms>) to do so if multiple axioms
	        AddAxiom addAxiom = new AddAxiom(PoContology, ax);
	        
	        //Then, use the manager class to apply the change
	        manager.applyChange(addAxiom);
	        
	      	//
	        //TRAITEMENT DES DATAPROPERTIES
	        //
	        
	        //Les dimensions de la BoundingBox sont triées
	        //puis affectées aux dataproperties:
	        //hasBoundingBoxMaxDimensionOf, hasBoundingBoxMiddleDimensionOf, hasBoundingBoxMinDimensionOf
	        //Un Array pour trier les longueurs
	        
	        Double[] lll = new Double[3];
	        Double swapMe = new Double(0.0);
	        Boolean change = true;
	        
	        //We store the 3 dimensions in the lll array to be sorted
	        lll[0] = currentObject.getObjectGeometry().getDimension().getX();
	        lll[1] = currentObject.getObjectGeometry().getDimension().getY();
	        lll[2] = currentObject.getObjectGeometry().getDimension().getZ();
	
	        //Tri bulle, ici deux if suffiraient
	        while (change == true) {
	        	for (int z=0; z < 2; z++) {	        		
	        		swapMe = lll[z];	        		
	        		if (swapMe < lll[z+1]) {
	        			//Let's swap        			
	        			lll[z] = lll[z+1];
	        			lll[z+1] = swapMe;	
	        			change = true;	        			
	        		} else {
	        			change = false;
	        		}
	        	}
	        }
	        
	        //Les trois dimensions de l'objet courant
	        OWLDataProperty hasBoundingBoxMaxValueOf = factory.getOWLDataProperty(":hasBoundingBoxMaxValueOf", pm); 
	        OWLDataProperty hasBoundingBoxMiddleValueOf = factory.getOWLDataProperty(":hasBoundingBoxMiddleValueOf", pm); 	        
	        OWLDataProperty hasBoundingBoxMinValueOf = factory.getOWLDataProperty(":hasBoundingBoxMinValueOf", pm); 
	        
	        double max = lll[0];
	        double middle = lll[1];
	        double min = lll[2];
	       
	        OWLLiteral literalMax = factory.getOWLLiteral(max);
	        OWLLiteral literalMiddle = factory.getOWLLiteral(middle);
	        OWLLiteral literalMin = factory.getOWLLiteral(min);
	        
	        OWLDataPropertyAssertionAxiom dataPropertyAssertionMax = factory
	                .getOWLDataPropertyAssertionAxiom(hasBoundingBoxMaxValueOf, individual, literalMax);
	        AddAxiom axiom = new AddAxiom(PoContology, dataPropertyAssertionMax);
	        manager.addAxiom(PoContology, dataPropertyAssertionMax);
	        manager.applyChange(axiom);
	
	        
	        OWLDataPropertyAssertionAxiom dataPropertyAssertionMiddle = factory
	                .getOWLDataPropertyAssertionAxiom(hasBoundingBoxMiddleValueOf, individual, literalMiddle);
	        axiom = new AddAxiom(PoContology, dataPropertyAssertionMiddle);
	        manager.addAxiom(PoContology, dataPropertyAssertionMiddle);
	        manager.applyChange(axiom);
	        
	        
	        OWLDataPropertyAssertionAxiom dataPropertyAssertionMin = factory
	                .getOWLDataPropertyAssertionAxiom(hasBoundingBoxMinValueOf, individual, literalMin);
	        axiom = new AddAxiom(PoContology, dataPropertyAssertionMin);
	        manager.addAxiom(PoContology, dataPropertyAssertionMin);
	        manager.applyChange(axiom);
	        
	        
	        //L'assertion de l'orientation Verticale, Horizontale ou Even
	        Orientation_CONST_NAME.Orientation orientation = listOfX3DObjects.get(i).getObjectGeometry().getOrientation();
	        if ( orientation == Orientation_CONST_NAME.Orientation.Vertical) {
	        	OWLDataProperty isVertical = factory.getOWLDataProperty(":isVertical", pm);
	        	OWLLiteral literalTrue = factory.getOWLLiteral(true);
	        	OWLDataPropertyAssertionAxiom dataPropertyAssertionVertical = factory
		                .getOWLDataPropertyAssertionAxiom(isVertical, individual, literalTrue);
		        AddAxiom axiom2 = new AddAxiom(PoContology, dataPropertyAssertionVertical);
		        manager.addAxiom(PoContology, dataPropertyAssertionVertical);
		        manager.applyChange(axiom2);
	        	} else if (orientation == Orientation_CONST_NAME.Orientation.Horizontal){
		        	OWLDataProperty isHorizontal = factory.getOWLDataProperty(":isHorizontal", pm);
		        	OWLLiteral literalTrue = factory.getOWLLiteral(true);
		        	OWLDataPropertyAssertionAxiom dataPropertyAssertionHorizontal = factory
			                .getOWLDataPropertyAssertionAxiom(isHorizontal, individual, literalTrue);
			        AddAxiom axiom3 = new AddAxiom(PoContology, dataPropertyAssertionHorizontal);
			        manager.addAxiom(PoContology, dataPropertyAssertionHorizontal);
			        manager.applyChange(axiom3);
	        		} else {
			        	OWLDataProperty isEven = factory.getOWLDataProperty(":isEven", pm);
			        	OWLLiteral literalTrue = factory.getOWLLiteral(true);
			        	OWLDataPropertyAssertionAxiom dataPropertyAssertionEven = factory
				                .getOWLDataPropertyAssertionAxiom(isEven, individual, literalTrue);
				        AddAxiom axiom4 = new AddAxiom(PoContology, dataPropertyAssertionEven);
				        manager.addAxiom(PoContology, dataPropertyAssertionEven);
				        manager.applyChange(axiom4);
	        			}
	       

			//On garde la référence de l'individu correspondant à cet X3DObject
			listOfX3DObjects.get(i).setNamedIndividual(individual);

			//System.out.println("ON A MIS A JOUR X3DOBject avec -> " + individual.getIRI());
		}
		
		System.out.println("Ontologie mise à jour avec " + listOfX3DObjects.size() + " instances d'objets 3D.");
		try {
	    	System.out.print("Saving Ontology............");
			manager.saveOntology(PoContology);
		    System.out.println("OK!");
		} catch (OWLOntologyStorageException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		printOntology(manager, PoContology);
	}
	
	private static void printOntology(OWLOntologyManager manager, OWLOntology ontology) {
	    // Print the ontology out on the console in OWL/XML format
	    try {
			manager.saveOntology(ontology, new OWLXMLOntologyFormat(),
			        new StreamDocumentTarget(System.out));
		} catch (OWLOntologyStorageException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	           
	    System.out.println("==================================================================================================");
	    System.out.println("\n");
	    System.out.println("\n");
		
	}


}



