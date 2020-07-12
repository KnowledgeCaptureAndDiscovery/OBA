package edu.isi.oba;

import edu.isi.oba.config.YamlConfig;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import static edu.isi.oba.Oba.logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

class Mapper {
    public static final String DEFAULT_DIR_QUERY = "_default_";
    public final Map<IRI, String> schemaNames = new HashMap<>(); //URI-names of the schemas
    public final Map<IRI, String> schemaDescriptions = new HashMap<>(); //URI-description of the schemas
    public Map<String, Schema> schemas = new HashMap<>();
    final Paths paths = new Paths();
    List<String> selected_paths;
    List<OWLOntology> ontologies;
    List<OWLClass> selected_classes;
    List<OWLClass> mapped_classes;
    YamlConfig config_data;

    public OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    private Boolean follow_references;

    public Mapper(YamlConfig config_data) throws OWLOntologyCreationException {
        this.config_data = config_data;
        List<String> paths = config_data.getPaths();
        this.selected_paths = paths;
        this.mapped_classes = new ArrayList<>();
        this.follow_references = config_data.getFollow_references();

        List<String> config_ontologies = config_data.getOntologies();
        String destination_dir = config_data.getOutput_dir() + File.separator + config_data.getName();
        File outputDir = new File(destination_dir);
        if (!outputDir.exists()){
            outputDir.mkdirs();
        }
        //Load the ontology into the manager
        int i = 0;
        List<String> ontologyPaths = new ArrayList<>();
        download_ontologies(config_ontologies, destination_dir, i, ontologyPaths);
        //set ontology paths in YAML to the ones we have downloaded (for later reference by owl2jsonld)
        this.config_data.setOntologies(ontologyPaths);
        ontologies = this.manager.ontologies().collect(Collectors.toList());

        //Create a temporal Map<IRI, String> schemaNames with the classes
        for (OWLOntology ontology : ontologies) {
            Set<OWLClass> classes = ontology.getClassesInSignature();
            setSchemaNames(classes);
            setSchemaDrescriptions(classes,ontology);
        }
        if (config_data.getClasses() != null)
            this.selected_classes = filter_classes();
    }

    private void download_ontologies(List<String> config_ontologies, String destination_dir, int i, List<String> ontologyPaths) throws OWLOntologyCreationException {
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
                    //copy to right folder
                    Files.copy(new File(ontologyPath).toPath(), ontologyFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ex) {
                    Logger.getLogger(Mapper.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            System.out.println(destinationPath);
            ontologyPaths.add(destinationPath);
            //this.manager.loadOntology(IRI.create(ontologyURL));
            this.manager.loadOntologyFromOntologyDocument(new File(destinationPath));
            i++;
        }
    }

    /**
     * Obtain Schemas using the ontology classes
     * The schemas includes the properties
     *
     * @param config_data
     * @return schemas
     */
    public void createSchemas(String destination_dir, YamlConfig config_data) {
        Query query = new Query(destination_dir);
        Path pathGenerator = new Path(config_data.getEnable_get_paths(),
                config_data.getEnable_post_paths(),
                config_data.getEnable_put_paths(),
                config_data.getEnable_delete_paths(),
                config_data.getAuth().getEnable()
        );
        try {
            query.get_all(DEFAULT_DIR_QUERY);
        } catch (Exception e) {
            logger.severe("Unable write the queries");
        }
        for (OWLOntology ontology : this.ontologies) {

            OWLDocumentFormat format = ontology.getFormat();
            //String defaultOntologyPrefixIRI = ((RDFXMLDocumentFormat) format).getDefaultPrefix();
            String defaultOntologyPrefixIRI = format.asPrefixOWLDocumentFormat().getDefaultPrefix();
            Set<OWLClass> classes = ontology.getClassesInSignature();

            /**
             * Find the classes and return the related classes
             */
            for (OWLClass cls : classes) {
                //filter if the class prefix is not the default ontology's prefix
                if (cls.getIRI() != null) {
                    if (selected_classes != null && !selected_classes.contains(cls))
                        continue;
                    add_owlclass_to_openapi(query, pathGenerator, ontology, defaultOntologyPrefixIRI, cls, true);
                }
            }
        }
        if (this.config_data.getAuth().getEnable())
            add_user_path(pathGenerator);
    }

    private void add_user_path(Path pathGenerator) {
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

    private List<OWLClass> add_owlclass_to_openapi(Query query, Path pathGenerator, OWLOntology ontology,
                                                   String defaultOntologyPrefixIRI, OWLClass cls, Boolean topLevel) {
        List<OWLClass> ref = new ArrayList<>();
        String classPrefixIRI = cls.getIRI().getNamespace();
        if (defaultOntologyPrefixIRI.equals(classPrefixIRI)) {
            try{
                MapperSchema mapperSchema = getMapperSchema(query, ontology, cls, this.schemaDescriptions.get(cls.getIRI()));

                for (OWLClass ref_class : mapperSchema.getProperties_range()) {
                    if (this.mapped_classes.contains(ref_class)){
                        logger.info("The class " + ref_class + " exists ");
                    } else {
                        for (OWLOntology temp_ontology : this.ontologies) {
                            if ( follow_references ) {
                                this.mapped_classes.add(ref_class);
                                getMapperSchema(query, temp_ontology, ref_class,this.schemaDescriptions.get(ref_class.getIRI()));
                                OWLDocumentFormat format = ontology.getFormat();
                                //String temp_defaultOntologyPrefixIRI = ((RDFXMLDocumentFormat) format).getDefaultPrefix();
                                String temp_defaultOntologyPrefixIRI = format.asPrefixOWLDocumentFormat().getDefaultPrefix();
                                add_owlclass_to_openapi(query, pathGenerator, temp_ontology, temp_defaultOntologyPrefixIRI, ref_class, false);
                            }
                        }
                    }
                }

                //Add the OpenAPI paths
                if (topLevel)
                    addOpenAPIPaths(pathGenerator, mapperSchema, cls);
            }catch(Exception e){
                logger.log(Level.SEVERE,"Could not parse class "+cls.getIRI().toString());
            }
        }
        return ref;
    }

    private MapperSchema getMapperSchema(Query query, OWLOntology ontology, OWLClass cls, String cls_description) {
        //Convert from OWL Class to OpenAPI Schema.
        MapperSchema mapperSchema = new MapperSchema(this.ontologies, cls, cls_description, schemaNames, ontology, follow_references);
        //Write queries
        query.write_readme(mapperSchema.name);
        //Create the OpenAPI schema
        Schema schema = mapperSchema.getSchema();
        schemas.put(schema.getName(), schema);
        return mapperSchema;
    }

    private void addOpenAPIPaths(Path pathGenerator, MapperSchema mapperSchema, OWLClass cls) {
        if (selected_classes != null && !selected_classes.contains(cls))
            logger.info("Ignoring class " + cls.toString());
        else
            add_path(pathGenerator, mapperSchema);
    }

    private void setSchemaNames(Set<OWLClass> classes) {
        for (OWLClass cls : classes) {
            schemaNames.put(cls.getIRI(), cls.getIRI().getShortForm());
        }
    }
    
    /**
     * Given a set of classes from an ontology, this method initializes
     * schemaDescriptions with the definitions used to describe an ontology (if provided)
     * @param classes the classes you want the description for
     * @param ontology the ontology from where we will extract the descriptions
     */
    private void setSchemaDrescriptions(Set<OWLClass> classes,OWLOntology ontology){
       for (OWLClass cls : classes) {
           System.out.println(cls);
           schemaDescriptions.put(cls.getIRI(), ObaUtils.getDescription(cls, ontology));
       }
    }

    private void add_path(Path pathGenerator, MapperSchema mapperSchema) {
        String singular_name = "/" + mapperSchema.name.toLowerCase() + "s/{id}";
        String plural_name = "/" + mapperSchema.name.toLowerCase() + "s";
        //Create the plural paths: for example: /models/
        this.paths.addPathItem(plural_name, pathGenerator.generate_plural(mapperSchema.name,
                mapperSchema.getCls().getIRI().getIRIString()));
        //Create the plural paths: for example: /models/id
        this.paths.addPathItem(singular_name, pathGenerator.generate_singular(mapperSchema.name,
                mapperSchema.getCls().getIRI().getIRIString()));
    }

// Method not used
//    private void add_path_relation(Path pathGenerator, String schema_name, String predicate, String path) {
//        String relation = "/" + schema_name.toLowerCase() + "s/{id}/" + path;
//        this.paths.addPathItem(relation, pathGenerator.generate_plural(schema_name));
//
//    }

    public List<OWLClass> filter_classes() {
        List<String> selected_classes_iri = this.config_data.getClasses();
        List<OWLClass> filtered_classes = new ArrayList();
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
