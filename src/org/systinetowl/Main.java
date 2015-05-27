package org.systinetowl;

import com.hp.systinet.repository.remote.client.RepositoryClient;
import com.hp.systinet.repository.remote.client.impl.RepositoryClientFactory;
import com.hp.systinet.repository.sdm.ArtifactBase;
import com.hp.systinet.repository.sdm.desc.PropertyDescriptor;
import com.hp.systinet.repository.sdm.properties.PropertyValue;
import com.hp.systinet.repository.sdm.properties.Relation;
import com.hp.systinet.repository.sdm.properties.SinglePropertyValue;
import com.hp.systinet.repository.sdm.propertytypes.*;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.util.OWLEntityRemover;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import java.io.*;
import java.net.URISyntaxException;
import java.util.*;

/**
 * Created by dvasunin on 06.06.14.
 */
public class Main {

    //  Client stub to connect to the Systinet server
    private RepositoryClient repositoryClient;
    private Set<ArtifactBase> processedArtifacts = new HashSet<ArtifactBase>();
    private Map<String, OWLNamedIndividual> processedIndividuals = new HashMap<String, OWLNamedIndividual>();
    private Map<String, OWLClass> owlClasses = new HashMap<String, OWLClass>();
    private Map<String, OWLDataProperty> owlDataProperties = new HashMap<String, OWLDataProperty>();
    // OWLAPI specific fields:
    // Ontology namespace
    private static String ontologyNamespace = "http://www.semanticweb.org/frank/ontologies/2014/5/systinetOntology";
    private OWLOntologyManager manager;
    private OWLOntology ontology;
    private IRI ontologyIRI;
    private OWLDataFactory df;
    private PrefixManager pm;
    private OWLObjectProperty OBJECT_PROPERTY_OUTGOING;
    private OWLObjectProperty OBJECT_PROPERTY_INCOMING;
    private OWLObjectProperty OBJECT_PROPERTY_ADDRESS;
    private OWLObjectProperty OBJECT_PROPERTY_CATEGORY;
    // Parent class for all Systinet OWL-classes
    private OWLClass parentClass;
    // Parent class name;
    private static String parentClassName = "SystinetRootClass";
    // System console to print logging messages
    PrintStream console = System.out;
    private String systinetUri;

    /**
     * Constructs an empty instance of the OWL importer
     *
     * @param systinetURL the URL of the Systinet server
     * @param user username for the Systinet client to connect
     * @param password password for the Systinet client
     * @throws URISyntaxException
     * @throws OWLOntologyCreationException
     */
    public Main(String systinetURL, String user, String password) throws URISyntaxException, OWLOntologyCreationException {
        this.systinetUri = systinetURL;
        repositoryClient = RepositoryClientFactory.createRepositoryClient(
                systinetURL, user, password, false, null, 0);
        manager = OWLManager.createOWLOntologyManager();
        ontologyIRI = IRI.create(ontologyNamespace);
        ontology = manager.createOntology(ontologyIRI);
        df = manager.getOWLDataFactory();
        pm = new DefaultPrefixManager(ontologyIRI.toString() + "#");
        OBJECT_PROPERTY_OUTGOING = df.getOWLObjectProperty("Outgoing", pm);
        manager.applyChange(new AddAxiom(ontology, df.getOWLDeclarationAxiom(OBJECT_PROPERTY_OUTGOING)));
        OBJECT_PROPERTY_INCOMING = df.getOWLObjectProperty("Incoming", pm);
        manager.applyChange(new AddAxiom(ontology, df.getOWLDeclarationAxiom(OBJECT_PROPERTY_INCOMING)));
        OBJECT_PROPERTY_ADDRESS = df.getOWLObjectProperty("Address", pm);
        manager.applyChange(new AddAxiom(ontology, df.getOWLDeclarationAxiom(OBJECT_PROPERTY_ADDRESS)));
        OBJECT_PROPERTY_CATEGORY = df.getOWLObjectProperty("Category", pm);
        manager.applyChange(new AddAxiom(ontology, df.getOWLDeclarationAxiom(OBJECT_PROPERTY_CATEGORY)));
        parentClass = df.getOWLClass(parentClassName, pm);
        manager.applyChange(new AddAxiom(ontology, df.getOWLDeclarationAxiom(parentClass)));
    }

    /**
     * Create an instance of the OWL importer for updates:
     * the importer deletes all individuals from given ontology keeping all classes untouched
     *
     * @param systinetURL the URL of the Systinet server
     * @param user username for the Systinet client to connect
     * @param password password for the Systinet client
     * @param file file with the ontology to update
     * @throws URISyntaxException
     * @throws OWLOntologyCreationException
     */
    public Main(String systinetURL, String user, String password, File file) throws URISyntaxException, OWLOntologyCreationException {
        this.systinetUri = systinetURL;
        repositoryClient = RepositoryClientFactory.createRepositoryClient(
                systinetURL, user, password, false, null, 0);
        manager = OWLManager.createOWLOntologyManager();
        loadFromFile(file);
        //we set the IRI from the ontology file
        ontologyIRI = ontology.getOntologyID().getOntologyIRI();
        df = manager.getOWLDataFactory();
        pm = new DefaultPrefixManager(ontologyIRI.toString() + "#");
        OBJECT_PROPERTY_OUTGOING = df.getOWLObjectProperty("Outgoing", pm);
        manager.applyChange(new AddAxiom(ontology, df.getOWLDeclarationAxiom(OBJECT_PROPERTY_OUTGOING)));
        OBJECT_PROPERTY_INCOMING = df.getOWLObjectProperty("Incoming", pm);
        manager.applyChange(new AddAxiom(ontology, df.getOWLDeclarationAxiom(OBJECT_PROPERTY_INCOMING)));
        OBJECT_PROPERTY_ADDRESS = df.getOWLObjectProperty("Address", pm);
        manager.applyChange(new AddAxiom(ontology, df.getOWLDeclarationAxiom(OBJECT_PROPERTY_ADDRESS)));
        OBJECT_PROPERTY_CATEGORY = df.getOWLObjectProperty("Category", pm);
        manager.applyChange(new AddAxiom(ontology, df.getOWLDeclarationAxiom(OBJECT_PROPERTY_CATEGORY)));
        parentClass = df.getOWLClass(parentClassName, pm);
        manager.applyChange(new AddAxiom(ontology, df.getOWLDeclarationAxiom(parentClass)));

        // here we save all classes from our ontology into the owlClasses hashmap
        for(OWLClass owlClass : ontology.getClassesInSignature()) {
            owlClasses.put(owlClass.getIRI().getFragment(), owlClass);
        }
        // here we save all data properties from our ontology into the owlDataProperties hashmap
        for(OWLDataProperty owlDataProperty: ontology.getDataPropertiesInSignature()){
            owlDataProperties.put(owlDataProperty.getIRI().getFragment(), owlDataProperty);
        }

        deleteIndividuals();
    }

    /**
     * This method deletes all individuals from the ontology, including anonymous individuals
     */
    public void deleteIndividuals(){
        OWLEntityRemover remover = new OWLEntityRemover(manager, Collections.singleton(ontology));
        for (OWLNamedIndividual owlNamedIndividual : ontology.getIndividualsInSignature()) {
            owlNamedIndividual.accept(remover);
        }
        Set<OWLAxiom> owlAxioms = new HashSet<OWLAxiom>();
        for (OWLAnonymousIndividual owlAnonymousIndividual : ontology.getReferencedAnonymousIndividuals()) {
            for(OWLAxiom owlAxiom: ontology.getReferencingAxioms(owlAnonymousIndividual)){
                owlAxioms.add(owlAxiom);
            }
        }
        manager.removeAxioms(ontology, owlAxioms);
        manager.applyChanges(remover.getChanges());
    }


    /**
     * Main method for the class: imports all artifacts from the Systinet repository
     */
    public void importOWL() {
        List<ArtifactBase> artifacts = repositoryClient.search(null, null, null, 0,  10000);
        for (ArtifactBase au : artifacts) {
            addArtifact(au.get_uuid().toString());
        }
    }

    /**
     * Saves imported ontology into the specified file
     *
     * @param file file where the ontology is saved to
     * @throws FileNotFoundException
     * @throws OWLOntologyStorageException
     */
    public void saveToFile(File file) throws FileNotFoundException, OWLOntologyStorageException {
        manager.saveOntology(ontology, new BufferedOutputStream(new FileOutputStream(file)));
    }

    /**
     * Loads ontology from the specified file
     *
     * @param file file the ontology is loaded from
     * @throws OWLOntologyCreationException
     */
    public void loadFromFile(File file) throws OWLOntologyCreationException {
        ontology = manager.loadOntologyFromOntologyDocument(file);
    }

    /**
     * Process a single property from Systinet and imports it ito ontology
     *
     * @param pd Systinet property descriptor
     * @param pv Systinet property value
     * @param individual OWL individual to import
     */
    private void processPropertyValue(PropertyDescriptor pd, PropertyValue pv, OWLIndividual individual) {
        OWLAxiom owlAxiom = null;
        if (pv instanceof Address) {
            Address address = (Address) pv;
            OWLAnonymousIndividual addressIndividual = df.getOWLAnonymousIndividual();

            OWLDataProperty cityProperty = df.getOWLDataProperty("City", pm);
            OWLDataPropertyAssertionAxiom cityPropertyAssertion = df.getOWLDataPropertyAssertionAxiom(cityProperty, addressIndividual, address.getCity());

            OWLDataProperty countryProperty = df.getOWLDataProperty("Country", pm);
            OWLDataPropertyAssertionAxiom countryPropertyAssertion = df.getOWLDataPropertyAssertionAxiom(countryProperty, addressIndividual, address.getCountry());

            OWLDataProperty postalcodeProperty = df.getOWLDataProperty("Postalcode", pm);
            OWLDataPropertyAssertionAxiom postalcodePropertyAssertion = df.getOWLDataPropertyAssertionAxiom(postalcodeProperty, addressIndividual, address.getPostalCode());

            OWLDataProperty stateprovinceProperty = df.getOWLDataProperty("StateProvince", pm);
            OWLDataPropertyAssertionAxiom stateProvincePropertyAssertion = df.getOWLDataPropertyAssertionAxiom(stateprovinceProperty, addressIndividual, address.getStateProvince());

            owlAxiom = df.getOWLObjectPropertyAssertionAxiom(OBJECT_PROPERTY_ADDRESS, individual, addressIndividual);
            manager.addAxiom(ontology, cityPropertyAssertion);
            manager.addAxiom(ontology, countryPropertyAssertion);
            manager.addAxiom(ontology, postalcodePropertyAssertion);
            manager.addAxiom(ontology, stateProvincePropertyAssertion);
        } else if (pv instanceof Category) {
            Category category = (Category) pv;
            OWLAnonymousIndividual categoryIndividual = df.getOWLAnonymousIndividual();
            OWLDataProperty taxonomyURIDataProperty = df.getOWLDataProperty("taxonomyURI", pm);
            OWLDataPropertyAssertionAxiom  taxonomyURIPropertyAssertion = df.getOWLDataPropertyAssertionAxiom(taxonomyURIDataProperty, categoryIndividual, category.getTaxonomyURI());
            OWLDataProperty nameDataProperty = df.getOWLDataProperty("name", pm);
            OWLDataPropertyAssertionAxiom  namePropertyAssertion = df.getOWLDataPropertyAssertionAxiom(nameDataProperty, categoryIndividual, category.getName());
            OWLDataProperty valDataProperty = df.getOWLDataProperty("val", pm);
            OWLDataPropertyAssertionAxiom  valPropertyAssertion = df.getOWLDataPropertyAssertionAxiom(valDataProperty, categoryIndividual, category.getVal());
            OWLObjectProperty objProp =  df.getOWLObjectProperty(pd.getSdmName(), pm);
            OWLSubObjectPropertyOfAxiom subObjectPropertyOfAxiom = df.getOWLSubObjectPropertyOfAxiom(objProp, OBJECT_PROPERTY_CATEGORY);


            owlAxiom = df.getOWLObjectPropertyAssertionAxiom(objProp, individual, categoryIndividual);
            manager.addAxiom(ontology, taxonomyURIPropertyAssertion);
            manager.addAxiom(ontology, namePropertyAssertion);
            manager.addAxiom(ontology, valPropertyAssertion);
            manager.addAxiom(ontology, subObjectPropertyOfAxiom);
        } else if (pv instanceof CategoryBag) {
            CategoryBag categoryBag = (CategoryBag) pv;
            console.println(pd.getSdmName() + " = ");
            console.println("===========================");
            console.println("CATEGORIES:");
            for (Category category : categoryBag.getCategories()) {
                console.println("\t name = " + category.getName());
                console.println("\t val = " + category.getVal());
                console.println("\t TaxonomyURI = " + category.getTaxonomyURI());
                console.println("----------------------------");
            }
            console.println("===========================");
            console.println("CATEGORY GROUPS:");
            for (CategoryGroup categoryGroup : categoryBag.getCategoryGroups()) {
                console.println("Taxonomy URI = " + categoryGroup.getTaxonomyURI());
                console.println("CATEGORIES:");
                for (Category category : categoryGroup.getCategories()) {
                    console.println("\t name = " + category.getName());
                    console.println("\t val = " + category.getVal());
                    console.println("\t TaxonomyURI = " + category.getTaxonomyURI());
                    console.println("----------------------------");
                }
            }

        } else {
            OWLDataProperty dataProperty = owlDataProperties.get(pd.getSdmName());
            if (dataProperty == null) {
                dataProperty = df.getOWLDataProperty(pd.getSdmName(), pm);
                OWLDeclarationAxiom declarationAxiom = df.getOWLDeclarationAxiom(dataProperty);
                //OWLFunctionalDataPropertyAxiom functionalDataPropertyAxiom = df.getOWLFunctionalDataPropertyAxiom(dataProperty);
                manager.addAxiom(ontology, declarationAxiom);
                //manager.addAxiom(ontology, functionalDataPropertyAxiom);
                owlDataProperties.put(pd.getSdmName(), dataProperty);
            }

            if (pv instanceof StringProperty) {
                owlAxiom = df.getOWLDataPropertyAssertionAxiom(dataProperty, individual, ((StringProperty) pv).getStringValue());
            } else if (pv instanceof IntegerProperty) {
                owlAxiom = df.getOWLDataPropertyAssertionAxiom(dataProperty, individual, ((SinglePropertyValue) pv).getIntegerValue());
            } else if (pv instanceof BigIntegerProperty) {
                OWLLiteral bigIntegerLiteral = df.getOWLTypedLiteral(((BigIntegerProperty) pv).getValue().toString(), OWL2Datatype.XSD_INTEGER);
                owlAxiom = df.getOWLDataPropertyAssertionAxiom(dataProperty, individual, bigIntegerLiteral);
            } else if (pv instanceof DateProperty) {
                OWLLiteral dateLiteral = df.getOWLTypedLiteral(
                        XMLGregorianCalendarConverter.asXMLGregorianCalendar(((DateProperty) pv).getDateValue()).toString(),
                        OWL2Datatype.XSD_DATE_TIME);
                owlAxiom = df.getOWLDataPropertyAssertionAxiom(dataProperty, individual, dateLiteral);
            } else if (pv instanceof BooleanProperty) {
                owlAxiom = df.getOWLDataPropertyAssertionAxiom(dataProperty, individual, ((BooleanProperty) pv).getBooleanValue());
            } else if (pv instanceof DoubleProperty) {
                owlAxiom = df.getOWLDataPropertyAssertionAxiom(dataProperty, individual, ((DoubleProperty) pv).getDoubleValue());
            } else if (pv instanceof UuidProperty) {
                owlAxiom = df.getOWLDataPropertyAssertionAxiom(dataProperty, individual, ((UuidProperty) pv).getValue().toString());
            } else {
                console.println(pd.getSdmName() + "\t=\t" + pv);
            }
        }
        if (owlAxiom != null) {
            manager.addAxiom(ontology, owlAxiom);
        }
    }

    /**
     * Method which import an artifact from Systinet repository given by its identifier into the ontology
     * as an individual. The artifacts linked with that individual are imported too using recursion.
     *
     * @param au UUID of the Systinet Artifact to import
     * @return the OWL individual associated with given artifact
     */
    public OWLNamedIndividual addArtifact(String au) {
        ArtifactBase a = repositoryClient.getArtifact(au);
        // if an artifact was already processed then return it from hashmap
        OWLNamedIndividual individual = processedIndividuals.get(au);
        if(individual != null) {
            return individual;
        }

        console.println("INDIVIDUAL: " + au);
        console.println("CLASS: " + a.get_artifactSdmName());

        individual = df.getOWLNamedIndividual(au, pm);
        // try to get OWL class for the artifact from hashmap. If it is not there then create a new one
        OWLClass sdmClass = owlClasses.get(a.get_artifactSdmName());
        if(sdmClass == null) {
            sdmClass = df.getOWLClass(a.get_artifactSdmName(), pm);
            OWLDeclarationAxiom declarationAxiom = df.getOWLDeclarationAxiom(sdmClass);
            OWLAxiom subClassOfAxiom = df.getOWLSubClassOfAxiom(sdmClass, parentClass);
            // declare new class for the instance (can be skipped if declaration is not needed)
            manager.applyChange(new AddAxiom(ontology, declarationAxiom));
            // add an axiom to the ontology saying that our class is a subclass of parentClass
            manager.applyChange(new AddAxiom(ontology, subClassOfAxiom));
            //save class in a hashmap for further use
            owlClasses.put(a.get_artifactSdmName(), sdmClass);
        }
        // individual is an instance of sdmClass
        OWLClassAssertionAxiom classAssertion = df.getOWLClassAssertionAxiom(sdmClass, individual);
        manager.addAxiom(ontology, classAssertion);
        // let's annotate individual with a fancy name
        manager.applyChange(new AddAxiom(ontology,
                df.getOWLAnnotationAssertionAxiom(individual.asOWLNamedIndividual().getIRI(),
                        df.getOWLAnnotation(
                                df.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI()),
                                df.getOWLLiteral(((StringProperty) a.getProperty("name")).getStringValue()
                                        + " - " + au)))));
        manager.applyChange(new AddAxiom(ontology,
                df.getOWLAnnotationAssertionAxiom(individual.asOWLNamedIndividual().getIRI(),
                        df.getOWLAnnotation(
                                df.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_SEE_ALSO.getIRI()),
                                df.getOWLLiteral(systinetUri + "/web/service-catalog/artifact/" + au,
                                        OWL2Datatype.XSD_ANY_URI)))));

        // add individual to the hashmap
        processedIndividuals.put(au, individual);

        // iterate through all properties of a Systinet's artifact and add them as dataProperty of OWL individual
        for(PropertyDescriptor pd : a.getArtifactDescriptor().enumerateProperties()){
                if (!pd.isRelationship()) {
                    if(pd.getPropertyCardinality().isMultiple()){
                        for(SinglePropertyValue sp : a.getMultiProperty(pd.getSdmName())){
                            processPropertyValue(pd, sp, individual);
                        }
                    } else {
                        PropertyValue pv = a.getProperty(pd.getSdmName());
                        if(pv != null) {
                           processPropertyValue(pd, pv, individual);
                        }
                    }
                }
            }

        for (Relation r : a.getRelations()) {
            if (r.isOutgoing()) {
                OWLIndividual targetIndividual = addArtifact(r.getTargetId().toString());
                manager.applyChange(new AddAxiom(ontology,
                        df.getOWLObjectPropertyAssertionAxiom(OBJECT_PROPERTY_OUTGOING,
                                individual,
                                targetIndividual)));
            } else if (r.isIncoming()){
                OWLIndividual sourceIndividual = addArtifact(r.getSourceId().toString());
                manager.applyChange(new AddAxiom(ontology,
                        df.getOWLObjectPropertyAssertionAxiom(OBJECT_PROPERTY_INCOMING,
                                individual,
                                sourceIndividual)));
            }
        }
        return individual;
    }


/* Never used, just a snipplet
    public void walk(){
        OWLOntologyWalker walker =
                new OWLOntologyWalker(Collections.singleton(ontology));
// Now ask our walker to walk over the ontology

        OWLOntologyWalkerVisitor<Object> visitor =
                new OWLOntologyWalkerVisitor<Object>(walker) {
                    @Override
                    public Object visit(OWLClass individual) {
                        System.out.println(individual.getIRI());
                        System.out.println(" " + getCurrentAxiom());

                        return null;
                    }
                };
// Have the walker walk...
        walker.walkStructure(visitor);
    }
*/

    public static void main(String[] args) {
        Main m = null;
        try {
            File file = new File("SystinetOntology-updated.owl");
            m = new Main("http://systinet.local:8080/soa", "admin", "admin", file);
            //m.walk();
            m.importOWL();
            m.saveToFile(file);


        } catch (Exception e) {
            e.printStackTrace();
        };
    }


}