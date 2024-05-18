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

import org.jibx.schema.codegen.extend.DefaultNameConverter;
import org.jibx.schema.codegen.extend.NameConverter;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.model.*;

class Mapper {
    public static final String DEFAULT_DIR_QUERY = "_default_";
    public final Map<IRI, String> schemaNames = new HashMap<>(); //URI-names of the schemas
    public final Map<IRI, String> schemaDescriptions = new HashMap<>(); //URI-description of the schemas
    public Map<String, Schema> schemas = new HashMap<>();
    final Paths paths = new Paths();
    Set<String> selected_paths = new HashSet<>();
    Set<OWLOntology> ontologies = new HashSet<>();
    List<OWLClass> selected_classes = new ArrayList<>();
    List<OWLClass> mappedClasses = new ArrayList<>();
    YamlConfig config_data;

    public OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

    public Mapper(YamlConfig config_data) throws OWLOntologyCreationException, IOException {
        this.config_data = config_data;
        this.selected_paths = config_data.getPaths();

        Set<String> config_ontologies = config_data.getOntologies();
        String destination_dir = config_data.getOutput_dir() + File.separator + config_data.getName();
        File outputDir = new File(destination_dir);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        
        //Load the ontology into the manager
        int i = 0;
        Set<String> ontologyPaths = new HashSet<>();
        this.download_ontologies(config_ontologies, destination_dir, i, ontologyPaths);
        //set ontology paths in YAML to the ones we have downloaded (for later reference by owl2jsonld)
        this.config_data.setOntologies(ontologyPaths);
        ontologies = this.manager.ontologies().collect(Collectors.toSet());

        //Create a temporal Map<IRI, String> schemaNames with the classes
        for (OWLOntology ontology : ontologies) {
            Set<OWLClass> classes = ontology.getClassesInSignature();
            this.setSchemaNames(classes);
            this.setSchemaDescriptions(classes, ontology);
        }

        if (config_data.getClasses() != null) {
            this.selected_classes.addAll(this.filter_classes());
        }
    }

    private void download_ontologies(Set<String> config_ontologies, String destination_dir, int i, Set<String> ontologyPaths) throws OWLOntologyCreationException, IOException {
        for (String ontologyPath : config_ontologies) {
            //copy the ontologies used in the destination folder
            String destinationPath = destination_dir + File.separator + "ontology" + i + ".owl";
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
                    Logger.getLogger(Mapper.class.getName()).log(Level.SEVERE, "ERROR while loading file: " + ontologyPath, ex);
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
            if (format == null) {
                logger.severe("No ontology format found.  Unable to proceed.");
                System.exit(1);
            } else {
                String defaultOntologyPrefixIRI = format.asPrefixOWLDocumentFormat().getDefaultPrefix();
                if (defaultOntologyPrefixIRI == null) {
                    logger.severe("Unable to find the default prefix for the ontology.  Unable to proceed.");
                    System.exit(1);
                }

                Set<OWLClass> classes = ontology.getClassesInSignature();
                for (OWLClass cls : classes) {
                    //filter if the class prefix does not have the default ontology prefix
                    if (cls.getIRI() != null) {
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

        try{
            MapperSchema mapperSchema = this.getMapperSchema(query, cls, this.schemaDescriptions.get(cls.getIRI()));

            // add references to schemas in class restrictions (check selected classes to avoid conflicts)
            for (String classToCheck : mapperSchema.getPropertiesFromObjectRestrictions_ranges()) {
                final var classIRI = IRI.create(classPrefixIRI + classToCheck);
                OWLClass clsToCheck = manager.getOWLDataFactory().getOWLClass(classIRI);
                if (this.mappedClasses.contains(clsToCheck) || this.selected_classes.contains(clsToCheck)){
                    logger.info("The class " + clsToCheck + " exists ");
                } else {
                    //rare cases have instances, so we filter them out and recheck that the target is a class.
                    if(ontology.containsClassInSignature(classIRI)) {
                        System.out.println("ADD "+ clsToCheck);
                        for (OWLOntology temp_ontology : this.ontologies) {
                            if (this.config_data.getConfigFlagValue(CONFIG_FLAG.FOLLOW_REFERENCES)) {
                                this.mappedClasses.add(clsToCheck);
                                this.getMapperSchema(query, clsToCheck, this.schemaDescriptions.get(classIRI));
                                this.selected_classes.addAll(this.add_owlclass_to_openapi(query, pathGenerator, temp_ontology, classPrefixIRI, clsToCheck, false));
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
                            this.getMapperSchema(query, ref_class, this.schemaDescriptions.get(ref_class.getIRI()));
                            this.selected_classes.addAll(this.add_owlclass_to_openapi(query, pathGenerator, temp_ontology, classPrefixIRI, ref_class, false));
                        }
                    }
                }
            }
            
            //Add the OpenAPI paths
            if (topLevel) {
                this.add_path(pathGenerator, mapperSchema);
            }
        }catch(Exception e){
            logger.log(Level.SEVERE,"Could not parse class " + cls.getIRI().toString());
            logger.log(Level.SEVERE,"\n\tdetails:\n" + e);
        }

        return ref;
    }

    private MapperSchema getMapperSchema(Query query, OWLClass cls, String cls_description) {
        //Convert from OWL Class to OpenAPI Schema.
        MapperSchema mapperSchema = new MapperSchema(this.ontologies, cls, cls_description, schemaNames, this.config_data.getConfigFlags());
        //Write queries
        query.write_readme(mapperSchema.name);
        //Create the OpenAPI schema
        Schema schema = mapperSchema.getSchema();
        schemas.put(schema.getName(), schema);
        return mapperSchema;
    }

    private void setSchemaNames(Set<OWLClass> classes) {
        for (OWLClass cls: classes) {
            this.schemaNames.put(cls.getIRI(), cls.getIRI().getShortForm());
        }
    }
    
    /**
     * Given a set of classes from an ontology, this method initializes
     * schemaDescriptions with the definitions used to describe an ontology (if provided)
     * @param classes the classes you want the description for
     * @param ontology the ontology from where we will extract the descriptions
     */
    private void setSchemaDescriptions(Set<OWLClass> classes, OWLOntology ontology) {
       for (OWLClass cls: classes) {
           System.out.println(cls);
           this.schemaDescriptions.put(cls.getIRI(), ObaUtils.getDescription(cls, ontology, this.config_data.getConfigFlagValue(CONFIG_FLAG.DEFAULT_DESCRIPTIONS)));
       }
    }

    private void add_path(PathGenerator pathGenerator, MapperSchema mapperSchema) {
        // Pluralizing currently only works for English.  Non-English words will be treated as though they are English.
        // TODO: Java support for singularization/pluralization and locale/international support supoort for the process does not have many good options that we could find so far.
        // TODO: If such an option exists or becomes available, this should be updated to support pluralization in other languages.
        // TODO: The language/locale would need to be set as a configuration value and passed into this class somehow.
        NameConverter nameTools = new DefaultNameConverter();
        String plural_name = "/" + nameTools.pluralize(mapperSchema.name.toLowerCase());

        //Create the plural paths: for example: /models/
        this.paths.addPathItem(plural_name, pathGenerator.generate_plural(mapperSchema.name,
                mapperSchema.getCls().getIRI().getIRIString()));
        //Create the plural paths: for example: /models/id
        this.paths.addPathItem(plural_name + "/{id}", pathGenerator.generate_singular(mapperSchema.name,
                mapperSchema.getCls().getIRI().getIRIString()));
    }

    public Set<OWLClass> filter_classes() {
        Set<String> selected_classes_iri = this.config_data.getClasses();
        Set<OWLClass> filtered_classes = new HashSet<>();
        for (OWLOntology ontology: this.ontologies) {
            for (OWLClass cls: ontology.getClassesInSignature()) {
                if (selected_classes_iri.contains(cls.getIRI().toString())) {
                    filtered_classes.add(cls);
                }
            }
        }

        return filtered_classes;
    }
}
