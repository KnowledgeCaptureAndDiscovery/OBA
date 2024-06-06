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
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.model.*;

class Mapper {
    private static final String DEFAULT_DIR_QUERY = "_default_";
    private final Map<IRI, String> schemaNames = new HashMap<>(); //URI-names of the schemas
    private final Map<String, Schema> schemas = new HashMap<>();
    private final Paths paths = new Paths();
    private final Set<OWLOntology> ontologies;
    private final Set<OWLClass> allowedClasses = new HashSet<>();
    private final YamlConfig configData;

    private final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

    /**
     * Constructor
     * 
     * @param configData the configuration data
     * @throws OWLOntologyCreationException
     * @throws IOException
     */
    public Mapper(YamlConfig configData) throws OWLOntologyCreationException, IOException {
        this.configData = configData;

        Set<String> configOntologies = this.configData.getOntologies();
        String destinationDir = this.configData.getOutput_dir() + File.separator + this.configData.getName();
        File outputDir = new File(destinationDir);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        
        // Load the ontology into the manager
        int i = 0;
        Set<String> ontologyPaths = new HashSet<>();
        this.downloadOntologies(configOntologies, destinationDir, i, ontologyPaths);

        //set ontology paths in YAML to the ones we have downloaded (for later reference by owl2jsonld)
        this.configData.setOntologies(ontologyPaths);
        this.ontologies = this.manager.ontologies().collect(Collectors.toSet());

        // Set the allowed classes for the OpenAPI based on configuration file.  If no restrictions set, all classes are added from each ontology.
        this.allowedClasses.addAll(this.getClassesAllowedByYamlConfig());
    }

    /**
     * Convenience method for unit testing.
     * 
     * @return a {@link Set} of {@link OWLOntology}
     */
    public Set<OWLOntology> getOntologies() {
        return this.ontologies;
    }

    /**
     * Convenience method for unit testing.
     * 
     * @return an {@link OWLOntologyManager}
     */
    public OWLOntologyManager getManager() {
        return this.manager;
    }

    private Schema getSchema(Query query, OWLClass cls) {
        logger.info("");
		logger.info("--->Beginning schema mapping for class \"" + cls + "\".");

        // Convert from OWL Class to OpenAPI Schema.
		final var objVisitor = new ObjectVisitor(cls, this.ontologies, this.configData);
		cls.accept(objVisitor);

        final var mappedSchema = objVisitor.getClassSchema();

        // Each time we generate a class's schema, there may be referenced classes that need to be added to the set of allowed classes.
        this.allowedClasses.addAll(objVisitor.getAllReferencedClasses());

        // Write queries
        query.writeReadme(mappedSchema.getName());

        // Create the OpenAPI schema
        logger.info("--->SAVING SCHEMA \"" + mappedSchema.getName() + "\".");
        this.schemas.put(mappedSchema.getName(), mappedSchema);

        return mappedSchema;
    }

    private void downloadOntologies(Set<String> configOntologies, String destinationDir, int i, Set<String> ontologyPaths) throws OWLOntologyCreationException, IOException {
        for (String ontologyPath: configOntologies) {
            // copy the ontologies used in the destination folder
            final var destinationPath = destinationDir + File.separator + "ontology" + i + ".owl";
            final var ontologyFile = new File (destinationPath);

            // content negotiation + download in case a URI is added
            if (ontologyPath.startsWith("http://") || ontologyPath.startsWith("https://")) {
                //download ontology to local path
                ObaUtils.downloadOntology(ontologyPath, destinationPath);
            } else {
                try {
                    // copy to the right folder
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
     * The schemas includes all (object and data) properties.
     *
     * @param destination_dir a {@link String> indicating the filesystem's directory to write the final results
     */
    public void createSchemas(String destinationDir) {
        final var query = new Query(destinationDir);
        final var pathGenerator = new PathGenerator(this.configData.getConfigFlags(), this.configData.getAuth() == null ? false : this.configData.getAuth().getEnable());

        try {
            query.getAll(DEFAULT_DIR_QUERY);
        } catch (Exception e) {
            logger.severe("Unable write the queries");
        }

        for (OWLOntology ontology: this.ontologies) {
            final var format = ontology.getFormat();
            if (format == null) {
                logger.severe("No ontology format found.  Unable to proceed.");
                System.exit(1);
            } else {
                String defaultOntologyPrefixIRI = format.asPrefixOWLDocumentFormat().getDefaultPrefix();
                if (defaultOntologyPrefixIRI == null) {
                    logger.severe("Unable to find the default prefix for the ontology.  Unable to proceed.");
                    System.exit(1);
                }

                // Make a copy of the original allowed classes.  Use it for comparison, until this working copy and the allowed classes are equal.
                var workingAllowedClasses = new HashSet<OWLClass>(this.allowedClasses);

                // Add allowed classes to OpenAPI (i.e. remove classes without default ontology
                ontology.classesInSignature().filter(owlClass -> owlClass.getIRI() != null && this.allowedClasses.contains(owlClass)).forEach((owlClass) -> {
                    this.addOwlclassToOpenAPI(query, pathGenerator, ontology, defaultOntologyPrefixIRI, owlClass, true);
                });

                // After allowed classes have been schema-fied, repeat for all the referenced classes.
                // If this is not done, the OpenAPI spec may contain references to schemas which do not exist (because they were not explicitly in the allow list).
                // Looping is done until no new references have been added from the schema-fication process.
                while (!this.allowedClasses.equals(workingAllowedClasses)) {
                    workingAllowedClasses.addAll(this.allowedClasses);

                    ontology.classesInSignature().filter(owlClass -> this.allowedClasses.contains(owlClass) && !this.schemas.keySet().contains(owlClass.getIRI().getShortForm())).forEach((owlClass) -> {
                        this.addOwlclassToOpenAPI(query, pathGenerator, ontology, defaultOntologyPrefixIRI, owlClass, true);
                    });
                }

                // Add all the allowed classes to the map of schema names/IRIs.
                this.setSchemaNames(this.allowedClasses);
            }
        }

        if (this.configData.getAuth().getEnable()) {
            this.addUserPath(pathGenerator);
        }
    }

    private void addUserPath(PathGenerator pathGenerator) {
        //User schema
        final var userProperties = new HashMap<String, Schema>();
        final var username = new StringSchema();
        final var password = new StringSchema();
        userProperties.put("username", username);
        userProperties.put("password", password);

        final var userSchema = new Schema();
        userSchema.setName("User");
        userSchema.setType("object");
        userSchema.setProperties(userProperties);

        this.schemas.put("User", userSchema);

        this.paths.addPathItem("/user/login", pathGenerator.user_login(userSchema.getName()));
    }

    private void addOwlclassToOpenAPI(Query query, PathGenerator pathGenerator, OWLOntology ontology, String defaultOntologyPrefixIRI, OWLClass cls, Boolean isTopLevel) {
        try{
            final var mappedSchema = this.getSchema(query, cls);
            
            //Add the OpenAPI paths
            if (isTopLevel && this.getClassesAllowedByYamlConfig().contains(cls)) {
                this.addPath(pathGenerator, mappedSchema, cls.getIRI());
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Could not parse class " + cls.getIRI().toString());
            logger.log(Level.SEVERE, "\n\tdetails:\n" + e);
        }
    }

    private void setSchemaNames(Set<OWLClass> classes) {
        for (OWLClass cls: classes) {
            this.schemaNames.put(cls.getIRI(), cls.getIRI().getShortForm());
        }
    }

    /**
     * Get a map of IRIs and their short form names for the schemas generated.
     * 
     * @return a {@link Map} of {@link IRI} keys and short form {@link String} values
     */
    public Map<IRI, String> getSchemaNames() {
        return this.schemaNames;
    }

    /**
     * Get a map of names and schemas for each class of the ontology/ies (that are allowed, according to the configuration file).
     * 
     * @return a {@link Map> of short form name {@link String} keys and their {@link Schema} values
     */
    public Map<String, Schema> getSchemas() {
        return this.schemas;
    }

    /**
     * Get all API paths from the OpenAPI spec.
     * 
     * @return A {@link Paths} object from Swagger's OAS model.
     */
    public Paths getPaths() {
        return this.paths;
    }

    private void addPath(PathGenerator pathGenerator, Schema mappedSchema, IRI classIRI) {
        // Pluralizing currently only works for English.  Non-English words will be treated as though they are English.
        // TODO: Java support for singularization/pluralization and locale/international support supoort for the process does not have many good options that we could find so far.
        // TODO: If such an option exists or becomes available, this should be updated to support pluralization in other languages.
        // TODO: The language/locale would need to be set as a configuration value and passed into this class somehow.
        final var nameTools = new DefaultNameConverter();

        // Pluralize the schema name.  Also convert to kebab-case if the configuration specifies it.
        String pluralName = "/";
        if (this.configData.getConfigFlagValue(CONFIG_FLAG.USE_KEBAB_CASE_PATHS)) { // "kebab-case" -> All lowercase and separate words with a dash/hyphen.
            pluralName += nameTools.pluralize(ObaUtils.pascalCaseToKebabCase(mappedSchema.getName()));
        } else { // "flatcase" -> This is the current/original version (all lower case, no spaces/dashes/underscores) of endpoint naming.
        pluralName += nameTools.pluralize(mappedSchema.getName().toLowerCase());
        }

        //Create the plural paths: for example: /models/
        this.paths.addPathItem(pluralName, pathGenerator.generate_plural(mappedSchema.getName(), classIRI.getIRIString()));

        //Create the plural paths: for example: /models/id
        this.paths.addPathItem(pluralName + "/{id}", pathGenerator.generate_singular(mappedSchema.getName(), classIRI.getIRIString()));
    }

    private Set<OWLClass> getClassesAllowedByYamlConfig() {
        final var allowedClassesByIRI = this.configData.getClasses();
        final var allowedClasses = new HashSet<OWLClass>();

        this.ontologies.forEach((ontology) -> {
            // If the configuration contains no allowed classes, then add all classes from the ontology.
            if (allowedClassesByIRI == null || allowedClassesByIRI.isEmpty()) {
                allowedClasses.addAll(ontology.getClassesInSignature());
            } else {
                ontology.classesInSignature().filter(owlClass -> allowedClassesByIRI.contains(owlClass.getIRI().toString())).forEach((allowedClass) -> {
                    allowedClasses.add(allowedClass);
                });
            }
        });

        return allowedClasses;
    }
}
