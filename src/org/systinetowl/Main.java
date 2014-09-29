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
import org.semanticweb.owlapi.util.OWLOntologyWalker;
import org.semanticweb.owlapi.util.OWLOntologyWalkerVisitor;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import java.io.*;
import java.net.URISyntaxException;
import java.util.*;

/**
 * Created by dvasunin on 06.06.14.
 */
public class Main {

    //  Client stub to connect to the
    private RepositoryClient repositoryClient;
    private Set<ArtifactBase> processedArtifacts = new HashSet<>();
    private Map<String, OWLNamedIndividual> processedIndividuals = new HashMap<>();
    private Map<String, OWLClass> owlClasses = new HashMap<>();
    private Map<String, OWLDataProperty> owlDataProperties = new HashMap<>();
    private OWLOntologyManager manager;
    private OWLOntology ontology;
    private IRI ontologyIRI;
    private OWLDataFactory df;
    private PrefixManager pm;
    private OWLObjectProperty OBJECT_PROPERTY_OUTGOING;
    private OWLObjectProperty OBJECT_PROPERTY_INCOMING;
    private OWLObjectProperty OBJECT_PROPERTY_ADDRESS;
    private OWLObjectProperty OBJECT_PROPERTY_CATEGORY;
    private OWLClass parentClass;
    PrintStream console = System.out;


    /**
     *
     * @param systinetURL
     * @param user
     * @param password
     * @throws URISyntaxException
     * @throws OWLOntologyCreationException
     */
    public Main(String systinetURL, String user, String password) throws URISyntaxException, OWLOntologyCreationException {
        repositoryClient = RepositoryClientFactory.createRepositoryClient(
                systinetURL, user, password, false, null, 0);
        manager = OWLManager.createOWLOntologyManager();
        ontologyIRI = IRI.create("http://www.semanticweb.org/frank/ontologies/2014/5/systinetOntology");
        ontology = manager.createOntology(ontologyIRI);
        df = manager.getOWLDataFactory();
        pm = new DefaultPrefixManager(ontologyIRI.toString() + "#");
        OBJECT_PROPERTY_OUTGOING = df.getOWLObjectProperty("Outgoing", pm);
        manager.addAxiom(ontology, df.getOWLDeclarationAxiom(OBJECT_PROPERTY_OUTGOING));
        OBJECT_PROPERTY_INCOMING = df.getOWLObjectProperty("Incoming", pm);
        manager.addAxiom(ontology, df.getOWLDeclarationAxiom(OBJECT_PROPERTY_INCOMING));
        OBJECT_PROPERTY_ADDRESS = df.getOWLObjectProperty("Address", pm);
        manager.addAxiom(ontology, df.getOWLDeclarationAxiom(OBJECT_PROPERTY_ADDRESS));
        OBJECT_PROPERTY_CATEGORY = df.getOWLObjectProperty("Category", pm);
        manager.addAxiom(ontology, df.getOWLDeclarationAxiom(OBJECT_PROPERTY_CATEGORY));
        parentClass = df.getOWLClass("SystinetRootClass", pm);
        OWLDeclarationAxiom declarationAxiom = df.getOWLDeclarationAxiom(parentClass);
        manager.addAxiom(ontology, declarationAxiom);
    }

    public Main(String systinetURL, String user, String password, File file) throws URISyntaxException, OWLOntologyCreationException {
        repositoryClient = RepositoryClientFactory.createRepositoryClient(
                systinetURL, user, password, false, null, 0);
        manager = OWLManager.createOWLOntologyManager();
        loadFromFile(file);
        ontologyIRI = ontology.getOntologyID().getOntologyIRI();
        df = manager.getOWLDataFactory();
        pm = new DefaultPrefixManager(ontologyIRI.toString() + "#");

        OBJECT_PROPERTY_OUTGOING = df.getOWLObjectProperty("Outgoing", pm);
        manager.addAxiom(ontology, df.getOWLDeclarationAxiom(OBJECT_PROPERTY_OUTGOING));
        OBJECT_PROPERTY_INCOMING = df.getOWLObjectProperty("Incoming", pm);
        manager.addAxiom(ontology, df.getOWLDeclarationAxiom(OBJECT_PROPERTY_INCOMING));
        OBJECT_PROPERTY_ADDRESS = df.getOWLObjectProperty("Address", pm);
        manager.addAxiom(ontology, df.getOWLDeclarationAxiom(OBJECT_PROPERTY_ADDRESS));
        OBJECT_PROPERTY_CATEGORY = df.getOWLObjectProperty("Category", pm);
        manager.addAxiom(ontology, df.getOWLDeclarationAxiom(OBJECT_PROPERTY_CATEGORY));
        parentClass = df.getOWLClass("SystinetRootClass", pm);
        OWLDeclarationAxiom declarationAxiom = df.getOWLDeclarationAxiom(parentClass);
        manager.addAxiom(ontology, declarationAxiom);


        for(OWLClass owlClass : ontology.getClassesInSignature()) {
            owlClasses.put(owlClass.getIRI().getFragment(), owlClass);
        }
        for(OWLDataProperty owlDataProperty: ontology.getDataPropertiesInSignature()){
            owlDataProperties.put(owlDataProperty.getIRI().getFragment(), owlDataProperty);
        }

        deleteIndividuals();
    }

    public void deleteIndividuals(){
        // delete individuals
        OWLEntityRemover remover = new OWLEntityRemover(manager, Collections.singleton(ontology));
        for (OWLNamedIndividual ind : ontology.getIndividualsInSignature()) {
            ind.accept(remover);
        }

        Set<OWLAxiom> owlAxioms = new HashSet<>();
        for (OWLAnonymousIndividual anonIndind : ontology.getReferencedAnonymousIndividuals()) {
            for(OWLAxiom owlAxiom: ontology.getReferencingAxioms(anonIndind)){
                owlAxioms.add(owlAxiom);
            }
        }
        manager.removeAxioms(ontology, owlAxioms);

        manager.applyChanges(remover.getChanges());

    }

    public void addArtifacts() {
        List<ArtifactBase> artifacts = repositoryClient.search(null, null, null, 0,  10000);
        for (ArtifactBase au : artifacts) {
            addArtifact(au.get_uuid().toString());
        }
    }

    public void saveToFile(File file) throws FileNotFoundException, OWLOntologyStorageException {
        manager.saveOntology(ontology, new BufferedOutputStream(new FileOutputStream(file)));
    }

    public void loadFromFile(File file) throws OWLOntologyCreationException {
        ontology = manager.loadOntologyFromOntologyDocument(file);
    }

    public OWLNamedIndividual addArtifact(String au) {
        ArtifactBase a = repositoryClient.getArtifact(au);
        OWLNamedIndividual individual = processedIndividuals.get(au);
        if(individual != null) {
            return individual;
        }

        //System.out.println("SDM name: " + a.get_artifactSdmName());
        individual = df.getOWLNamedIndividual(au, pm);

        OWLClass sdmClass = owlClasses.get(a.get_artifactSdmName());
        if(sdmClass == null) {
            sdmClass = df.getOWLClass(a.get_artifactSdmName(), pm);
            OWLDeclarationAxiom declarationAxiom = df.getOWLDeclarationAxiom(sdmClass);
            OWLAxiom axiom = df.getOWLSubClassOfAxiom(sdmClass, parentClass);
            manager.addAxiom(ontology, declarationAxiom);
            manager.addAxiom(ontology, axiom);
            owlClasses.put(a.get_artifactSdmName(), sdmClass);
        }

        OWLClassAssertionAxiom classAssertion = df.getOWLClassAssertionAxiom(sdmClass, individual);
        manager.addAxiom(ontology, classAssertion);

        OWLLiteral lbl = df.getOWLLiteral(au + " - " + a.get_artifactSdmName());
        OWLAnnotation label =
                df.getOWLAnnotation(
                        df.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI()), lbl);
        OWLAxiom axiom = df.getOWLAnnotationAssertionAxiom(individual.asOWLNamedIndividual().getIRI(), label);
        manager.applyChange(new AddAxiom(ontology, axiom));
        processedIndividuals.put(au, individual);
            //IRI ontologyIRI = getFactory().getOwlOntology().getOntologyID().getOntologyIRI();
            //PrefixManager pm = new DefaultPrefixManager(ontologyIRI.toString());
            //OWLDataProperty hasName = getFactory().getOwlOntology().getOWLOntologyManager().getOWLDataFactory().getOWLDataProperty("#hasName", pm);
        //OWLDataPropertyAssertionAxiom dataPropertyAssertion = getFactory().getOwlOntology().getOWLOntologyManager().getOWLDataFactory()
        //            .getOWLDataPropertyAssertionAxiom(Vocabulary.DATA_PROPERTY_HASNAME, individual, a.getName());
        //System.out.println("Adding axiom: " + individual + " #hasName " + a.getName());
        //getFactory().getOwlOntology().getOWLOntologyManager().addAxiom(getFactory().getOwlOntology(), dataPropertyAssertion);


        for(PropertyDescriptor pd : a.getArtifactDescriptor().enumerateProperties()){
                if (!pd.isRelationship()) {
                    // + pd.getPropertyTypeDescriptor().getPropertyTypeClass().getSimpleName());
                    if(pd.getPropertyCardinality().isMultiple()){
                        //System.out.print(pd.getSdmName() + " == ");
                        for(SinglePropertyValue sp : a.getMultiProperty(pd.getSdmName())){
                            //System.out.print("\t" + sp.toString());
                        }
                        //System.out.println();
                    } else {
                        PropertyValue pv = a.getProperty(pd.getSdmName());
                        if(pv != null) {
                            OWLAxiom owlAxiom = null;
                            if (pv instanceof Address) {
                                Address address = (Address) pv;
                                OWLAnonymousIndividual addressIndividual = df.getOWLAnonymousIndividual();
                                //OWLNamedIndividual addressIndividual =  df.getOWLNamedIndividual(IRI.create("http://www.semanticweb.org/frank/ontologies/2014/5/systinetOntology/address#" + au));

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
                                    OWLFunctionalDataPropertyAxiom functionalDataPropertyAxiom = df.getOWLFunctionalDataPropertyAxiom(dataProperty);
                                    manager.addAxiom(ontology, declarationAxiom);
                                    manager.addAxiom(ontology, functionalDataPropertyAxiom);
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
                    }
                }
            }

        for (Relation r : a.getRelations()) {
            if (r.isOutgoing()) {
                OWLIndividual targetIndividual = addArtifact(r.getTargetId().toString());
                OWLObjectPropertyAssertionAxiom assertion = df.getOWLObjectPropertyAssertionAxiom(OBJECT_PROPERTY_OUTGOING, individual, targetIndividual);
                manager.addAxiom(ontology, assertion);
            } else if (r.isIncoming()){
                OWLIndividual sourceIndividual = addArtifact(r.getSourceId().toString());
                OWLObjectPropertyAssertionAxiom assertion = df.getOWLObjectPropertyAssertionAxiom(OBJECT_PROPERTY_INCOMING, individual, sourceIndividual);
                manager.addAxiom(ontology, assertion);
            }
        }
        return individual;
    }

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


    public static void main(String[] args) {
        Main m = null;
        try { /*
            JFileChooser chooser = new JFileChooser();
            FileNameExtensionFilter filter = new FileNameExtensionFilter(
                    "OWL files", "owl");
            chooser.setFileFilter(filter);
            int returnVal = chooser.showSaveDialog(null);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                //OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
                //File f = new File("SystinetOntology.owl");
                //OWLOntology ontology = manager.loadOntologyFromOntologyDocument(f);
                m = new Main("http://systinet.local:8080/soa", "admin", "admin");
                m.addArtifacts();
                File file = chooser.getSelectedFile();
                m.saveToFile(file);
            }
             */
            File file = new File("SystinetOntology-updated.owl");
            m = new Main("http://systinet.local:8080/soa", "admin", "admin", file);
            //m.walk();
            m.addArtifacts();
            m.saveToFile(file);


        } catch (Exception e) {
            e.printStackTrace();
        };
    }


}