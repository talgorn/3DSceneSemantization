
//JAVA
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.reasoner.FreshEntitiesException;
import org.semanticweb.owlapi.reasoner.InconsistentOntologyException;
import org.semanticweb.owlapi.reasoner.ReasonerInterruptedException;
import org.semanticweb.owlapi.reasoner.TimeOutException;



//TODO
//Refactorer le code pour
//1) Intégrer les méthodes liées à la géometry dans l'objet geometry
//2) Utiliser les transformations matricielles de Java plutôt que les classes présentes
//3) Tri bulle inutile pour classer les 3 mesures de bounding box, un double if suffirait

public class MainClass{

    private static final String X3D_FILE = "path_to/OneRoomAppartment.x3d";
    private static final String ONTOLOGY_FILE = "path_to/OntologyFile.owl";
 
    public static void main(String[] args) throws OWLOntologyStorageException, OWLException, InconsistentOntologyException, FreshEntitiesException, ReasonerInterruptedException, TimeOutException {
    	  	
    	//Retrieving the X3DObjects via X3DOperations class
        List<X3DObject> listOfx3DObjects = new ArrayList<X3DObject>();
        
        File X3DSceneFile = new File(X3D_FILE);
        File ontologyFile = new File(ONTOLOGY_FILE);
    	
        X3DOperations x3DOps = new X3DOperations(X3DSceneFile);
    	
        //Get the list of all X3DObjects
    	listOfx3DObjects = x3DOps.getListOfX3DObjects();
    	
    	//Pass it to the ontologyOperations Class
    	OntologyOperations OntologyOps = new OntologyOperations(ontologyFile, listOfx3DObjects);
    	
    	//Now that we have infered the classificiation of individuals
    	///We can apply it to X3DObject by colorizing them in the X3DFile  	
    	x3DOps.colorizeX3dFile();	
    	
    	System.out.println("MOTHER11 DIED AOK." + OntologyOps.toString());
 	}
}





	
	
	

