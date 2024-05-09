package edu.isi.oba;

import edu.isi.oba.config.CONFIG_FLAG;
import edu.isi.oba.config.YamlConfig;
import static edu.isi.oba.Oba.logger;

import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.model.*;

class Mapper {
    public static final String DEFAULT_DIR_QUERY = "_default_";
    public final Map<IRI, String> schemaNames = new HashMap<>(); //URI-names of the schemas
    public final Map<IRI, String> schemaDescriptions = new HashMap<>(); //URI-description of the schemas
    public Map<String, Schema> schemas = new HashMap<>();
    final Paths paths = new Paths();
    List<String> selected_paths;
    List<OWLOntology> ontologies;
    List<OWLClass> selected_classes;
    List<OWLClass> mappedClasses;
    YamlConfig config_data;

    public OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

    public Mapper(YamlConfig config_data) throws OWLOntologyCreationException, IOException {
        this.config_data = config_data;
        this.selected_paths = config_data.getPaths();
        this.mappedClasses = new ArrayList<>();

        List<String> config_ontologies = config_data.getOntologies();
        String destination_dir = config_data.getOutput_dir() + File.separator + config_data.getName();
        File outputDir = new File(destination_dir);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        
        //Load the ontology into the manager
        int i = 0;
        List<String> ontologyPaths = new ArrayList<>();
        this.download_ontologies(config_ontologies, destination_dir, i, ontologyPaths);
        //set ontology paths in YAML to the ones we have downloaded (for later reference by owl2jsonld)
        this.config_data.setOntologies(ontologyPaths);
        ontologies = this.manager.ontologies().collect(Collectors.toList());

        //Create a temporal Map<IRI, String> schemaNames with the classes
        for (OWLOntology ontology : ontologies) {
            Set<OWLClass> classes = ontology.getClassesInSignature();
            this.setSchemaNames(classes);
            this.setSchemaDrescriptions(classes,ontology);
        }

        if (config_data.getClasses() != null) {
            this.selected_classes = this.filter_classes();
        }
    }

    private void download_ontologies(List<String> config_ontologies, String destination_dir, int i, List<String> ontologyPaths) throws OWLOntologyCreationException, IOException {
        for (String ontologyPath : config_ontologies) {
            //copy the ontologies used in the destination folder
            String destinationPath = destination_dir + File.separator +"ontology"+i+".owl";
            File ontologyFile = new File (destinationPath);
            //content negotiation + download in case a URI is added
            if(ontologyPath.startsWith("http://") || ontologyPath.startsWith("https://")){
                //download ontology to local path
                ObaUtils.downloadOntology(ontologyPath, destinationPath);
            }
            else{
                try {
                    //copy to the right folder
                    Files.copy(new File(ontologyPath).toPath(), ontologyFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ex) {
                    Logger.getLogger(Mapper.class.getName()).log(Level.SEVERE, "ERROR while loading file: "+ontologyPath, ex);
                    throw ex;
                }
            }
            System.out.println(destinationPath);
            ontologyPaths.add(destinationPath);
            // Set to silent so missing imports don't make the program fail.
            OWLOntologyLoaderConfiguration loadingConfig = new OWLOntologyLoaderConfiguration();
            loadingConfig = loadingConfig.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
            this.manager.loadOntologyFromOntologyDocument(new FileDocumentSource(new File(destinationPath)), loadingConfig);
            i++;
        }
    }

    /**
     * Obtain Schemas using the ontology classes
     * The schemas includes the properties
     *
     * @param destination_dir directory to write the final results
     */
    public void createSchemas(String destination_dir) {
        Query query = new Query(destination_dir);
        PathGenerator pathGenerator = new PathGenerator(this.config_data.getConfigFlags(),
            this.config_data.getAuth().getEnable()
        );

        try {
            query.get_all(DEFAULT_DIR_QUERY);
        } catch (Exception e) {
            logger.severe("Unable write the queries");
        }

        for (OWLOntology ontology : this.ontologies) {

            OWLDocumentFormat format = ontology.getFormat();
            String defaultOntologyPrefixIRI = format.asPrefixOWLDocumentFormat().getDefaultPrefix();
            if (defaultOntologyPrefixIRI == null) {
                logger.severe("Unable to find the default prefix for the ontology");
                System.exit(1);
            }

            Set<OWLClass> classes = ontology.getClassesInSignature();
            for (OWLClass cls : classes) {
                //filter if the class prefix does not have the default ontology prefix
                if (cls.getIRI() != null) {
                    if (this.selected_classes == null || this.selected_classes.contains(cls)) {
                        this.add_owlclass_to_openapi(query, pathGenerator, ontology, defaultOntologyPrefixIRI, cls, true);
                    }
                }
            }
        }

        if (this.config_data.getAuth().getEnable()) {
            this.add_user_path(pathGenerator);
        }
    }

    private void add_user_path(PathGenerator pathGenerator) {
        //User schema
        Map<String, Schema> userProperties = new HashMap<>();
        StringSchema username = new StringSchema();
        StringSchema password = new StringSchema();
        userProperties.put("username", username);
        userProperties.put("password", password);

        Schema userSchema = new Schema();
        userSchema.setName("User");
        userSchema.setType("object");
        userSchema.setProperties(userProperties);
        schemas.put("User", userSchema);

        this.paths.addPathItem("/user/login", pathGenerator.user_login(userSchema.getName()));
    }

    private List<OWLClass> add_owlclass_to_openapi(Query query, PathGenerator pathGenerator, OWLOntology ontology,
                                                   String defaultOntologyPrefixIRI, OWLClass cls, Boolean topLevel) {
        List<OWLClass> ref = new ArrayList<>();
        String classPrefixIRI = cls.getIRI().getNamespace();
        if (defaultOntologyPrefixIRI.equals(classPrefixIRI)) {
            try{
                MapperSchema mapperSchema = getMapperSchema(query, ontology, cls, this.schemaDescriptions.get(cls.getIRI()));

                // add references to schemas in class restrictions (check selected classes to avoid conflicts)
                for (String classToCheck : mapperSchema.getPropertiesFromObjectRestrictions_ranges()) {
                    OWLClass clsToCheck = manager.getOWLDataFactory().getOWLClass(IRI.create(classPrefixIRI + classToCheck));
                    if (this.mappedClasses.contains(clsToCheck) || (this.selected_classes != null && this.selected_classes.contains(clsToCheck))){
                        logger.info("The class " + clsToCheck + " exists ");
                    } else {
                        //rare cases have instances, so we filter them out and recheck that the target is a class.
                        if(ontology.containsClassInSignature(clsToCheck.getIRI())) {
                            System.out.println("ADD "+ clsToCheck);
                            for (OWLOntology temp_ontology : this.ontologies) {
                                if (this.config_data.getConfigFlagValue(CONFIG_FLAG.FOLLOW_REFERENCES)) {
                                    this.mappedClasses.add(clsToCheck);
                                    this.getMapperSchema(query, temp_ontology, clsToCheck, this.schemaDescriptions.get(clsToCheck.getIRI()));
                                    this.add_owlclass_to_openapi(query, pathGenerator, temp_ontology, classPrefixIRI, clsToCheck, false);
                                }
                            }
                        }
                    }
                }

                // add references to schemas in property ranges
                for (OWLClass ref_class : mapperSchema.getProperties_range()) {
                    if (this.mappedClasses.contains(ref_class)){
                        logger.info("The class " + ref_class + " exists ");
                    } else {
                        for (OWLOntology temp_ontology : this.ontologies) {
                            if (this.config_data.getConfigFlagValue(CONFIG_FLAG.FOLLOW_REFERENCES)) {
                                this.mappedClasses.add(ref_class);
                                this.getMapperSchema(query, temp_ontology, ref_class,this.schemaDescriptions.get(ref_class.getIRI()));
                                this.add_owlclass_to_openapi(query, pathGenerator, temp_ontology, classPrefixIRI, ref_class, false);
                            }
                        }
                    }
                }
                
                //Add the OpenAPI paths
                if (topLevel) {
                    addOpenAPIPaths(pathGenerator, mapperSchema, cls);
                }
            }catch(Exception e){
                logger.log(Level.SEVERE,"Could not parse class "+cls.getIRI().toString());
            }
        }
        return ref;
    }

    private MapperSchema getMapperSchema(Query query, OWLOntology ontology, OWLClass cls, String cls_description) {
        //Convert from OWL Class to OpenAPI Schema.
        MapperSchema mapperSchema = new MapperSchema(this.ontologies, cls, cls_description, schemaNames, ontology, this.config_data.getConfigFlags());
        //Write queries
        query.write_readme(mapperSchema.name);
        //Create the OpenAPI schema
        Schema schema = mapperSchema.getSchema();
        schemas.put(schema.getName(), schema);
        return mapperSchema;
    }

    private void addOpenAPIPaths(PathGenerator pathGenerator, MapperSchema mapperSchema, OWLClass cls) {
        if (this.selected_classes != null && !this.selected_classes.contains(cls)) {
            logger.info("Ignoring class " + cls.toString());
        } else {
            this.add_path(pathGenerator, mapperSchema);
        }
    }

    private void setSchemaNames(Set<OWLClass> classes) {
        for (OWLClass cls : classes) {
            this.schemaNames.put(cls.getIRI(), cls.getIRI().getShortForm());
        }
    }
    
    /**
     * Given a set of classes from an ontology, this method initializes
     * schemaDescriptions with the definitions used to describe an ontology (if provided)
     * @param classes the classes you want the description for
     * @param ontology the ontology from where we will extract the descriptions
     */
    private void setSchemaDrescriptions(Set<OWLClass> classes, OWLOntology ontology){
       for (OWLClass cls: classes) {
           System.out.println(cls);
           schemaDescriptions.put(cls.getIRI(), ObaUtils.getDescription(cls, ontology, this.config_data.getConfigFlagValue(CONFIG_FLAG.DEFAULT_DESCRIPTIONS)));
       }
    }

    private void add_path(PathGenerator pathGenerator, MapperSchema mapperSchema) {
        String singular_name = "/" + mapperSchema.name.toLowerCase() + "s/{id}";
        String plural_name = "/" + mapperSchema.name.toLowerCase() + "s";
        //Create the plural paths: for example: /models/
        this.paths.addPathItem(plural_name, pathGenerator.generate_plural(mapperSchema.name,
                mapperSchema.getCls().getIRI().getIRIString()));
        //Create the plural paths: for example: /models/id
        this.paths.addPathItem(singular_name, pathGenerator.generate_singular(mapperSchema.name,
                mapperSchema.getCls().getIRI().getIRIString()));
    }


    public List<OWLClass> filter_classes() {
        List<String> selected_classes_iri = this.config_data.getClasses();
        ArrayList<OWLClass> filtered_classes = new ArrayList();
        for (OWLOntology ontology : this.ontologies) {
            for (OWLClass cls : ontology.getClassesInSignature()) {
                if (selected_classes_iri.contains(cls.getIRI().toString())) {
                    filtered_classes.add(cls);
                }
            }
        }
        return filtered_classes;
    }
}
