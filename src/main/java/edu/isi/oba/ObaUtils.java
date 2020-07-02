package edu.isi.oba;

import edu.isi.oba.config.YamlConfig;
import org.apache.commons.cli.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static edu.isi.oba.Oba.logger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.search.EntitySearcher;
import uk.ac.manchester.cs.owl.owlapi.OWLAnnotationPropertyImpl;

public class ObaUtils {
    public static final String[] POSSIBLE_VOCAB_SERIALIZATIONS = { "application/rdf+xml", "text/turtle", "text/n3",
			"application/ld+json" };
    private static final String RDFS_NS = "http://www.w3.org/2000/01/rdf-schema#";
    private static final String SKOS_NS = "http://www.w3.org/2004/02/skos/core#";
    private static final String PROV_NS = "http://www.w3.org/ns/prov#";
    public static final String RDFS_COMMENT = RDFS_NS+"comment";
    public static final String SKOS_DEFINITION = SKOS_NS+"definition";
    public static final String PROV_DEFINITION = PROV_NS+"definition";
    public static final List<String> DESCRIPTION_PROPERTIES = new ArrayList<>(Arrays.asList(
      RDFS_COMMENT,
      SKOS_DEFINITION,
      PROV_DEFINITION
      ));

    public static void write_file(String file_path, String content) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(file_path));
            writer.write(content);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Code to unzip a file. Inspired from
     * http://www.mkyong.com/java/how-to-decompress-files-from-a-zip-file/ Taken
     * from
     *
     * @param resourceName
     * @param outputFolder
     */
    public static void unZipIt(String resourceName, String outputFolder) {

        byte[] buffer = new byte[1024];
        try {
            ZipInputStream zis = new ZipInputStream(ObaUtils.class.getResourceAsStream(resourceName));
            ZipEntry ze = zis.getNextEntry();

            while (ze != null) {
                String fileName = ze.getName();
                File newFile = new File(outputFolder + File.separator + fileName);
                // System.out.println("file unzip : "+ newFile.getAbsoluteFile());
                if (ze.isDirectory()) {
                    String temp = newFile.getAbsolutePath();
                    new File(temp).mkdirs();
                } else {
                    String directory = newFile.getParent();
                    if (directory != null) {
                        File d = new File(directory);
                        if (!d.exists()) {
                            d.mkdirs();
                        }
                    }
                    FileOutputStream fos = new FileOutputStream(newFile);
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                }
                ze = zis.getNextEntry();
            }

            zis.closeEntry();
            zis.close();

        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Exception while copying resource. " + ex.getMessage());
        }

    }

    public static void copy(InputStream is, File dest) throws Exception {
        OutputStream os = null;
        try {
            os = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error while extracting the reosurces: " + e.getMessage());
            throw e;
        } finally {
            if (is != null)
                is.close();
            if (os != null)
                os.close();
        }
    }

    /**
     * This function recursively copy all the sub folder and files from sourceFolder to destinationFolder
     */
    public static void copyFolder(File sourceFolder, File destinationFolder) throws IOException {
        //Check if sourceFolder is a directory or file
        //If sourceFolder is file; then copy the file directly to new location
        if (sourceFolder.isDirectory()) {
            //Verify if destinationFolder is already present; If not then create it
            if (!destinationFolder.exists()) {
                destinationFolder.mkdir();
                System.out.println("Directory created :: " + destinationFolder);
            }

            //Get all files from source directory
            String files[] = sourceFolder.list();

            //Iterate over all files and copy them to destinationFolder one by one
            for (String file : files) {
                File srcFile = new File(sourceFolder, file);
                File destFile = new File(destinationFolder, file);

                //Recursive function call
                copyFolder(srcFile, destFile);
            }
        } else {
            //Copy the file content from one place to another
            Files.copy(sourceFolder.toPath(), destinationFolder.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("File copied :: " + destinationFolder);
        }
    }

    public static String get_config_yaml(String[] args) {
        //obtain the options to pass configuration
        Options options = new Options();
        Option input = new Option("c", "config", true, "configuration file path");
        input.setRequired(true);
        options.addOption(input);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;
        String config_yaml = null;

        try {
            cmd = parser.parse(options, args);
            config_yaml = cmd.getOptionValue("config");
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utiConfiguration filelity-name", options);
            System.exit(1);
        }
        return config_yaml;
    }

    public static YamlConfig get_yaml_data(String config_yaml) {
        Constructor constructor = new Constructor(YamlConfig.class);
        Yaml yaml = new Yaml(constructor);

        InputStream config_input = null;
        try {
            config_input = new FileInputStream(new File(config_yaml));
        } catch (FileNotFoundException e) {
            System.err.println("Configuration file not found: " + config_yaml);
            System.exit(1);
        }
        //Yaml config parse
        return yaml.loadAs(config_input, YamlConfig.class);
    }

    public static JSONObject concat_json_common_key(JSONObject[] objects, String common_key) {
        JSONObject mergeJSON = (JSONObject) objects[0].get(common_key);
        for (int i = 1; i < objects.length; i++) {
            mergeJSON = mergeJSONObjects(mergeJSON, (JSONObject) objects[i].get(common_key));
        }

        return new JSONObject().put(common_key, mergeJSON);
    }

    public static JSONObject concat_json(JSONObject[] objects) {
        JSONObject mergeJSON = objects[0];
        for (int i = 1; i < objects.length; i++) {
            mergeJSON = mergeJSONObjects(mergeJSON, objects[i]);
        }
        return mergeJSON;
    }

    public static JSONObject mergeJSONObjects(JSONObject json1, JSONObject json2) {

        JSONObject mergedJSON = new JSONObject();
        try {
            mergedJSON = new JSONObject(json1, JSONObject.getNames(json1));
            for (String crunchifyKey : JSONObject.getNames(json2)) {
                mergedJSON.put(crunchifyKey, json2.get(crunchifyKey));
            }

        } catch (JSONException e) {
            throw new RuntimeException("JSON Exception" + e);
        }
        return mergedJSON;
    }

    public static JSONObject generate_context_file(String[] ontologies, Boolean only_classes) throws Exception {
        JSONObject[] jsons = new JSONObject[ontologies.length];
        for (int i = 0; i < ontologies.length; i++) {
            try {
                jsons[i] = run_owl_jsonld(ontologies[i], only_classes);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return ObaUtils.concat_json_common_key(jsons, "@context");
    }

    /**
     * Method to execute OWL2JSONLD
     * @param ontology_file file path of the ontology to load
     * @param only_classes
     * @return JSON object with the context of the ontology
     * @throws Exception 
     */
    private static JSONObject run_owl_jsonld(String ontology_file, Boolean only_classes) throws Exception {
        ontology_file = new File(ontology_file).toURI().toString();
        String owl2jsonld = "/owl2jsonld-0.3.0-SNAPSHOT-standalone.jar";
        InputStream owl2_jar = ObaUtils.class.getResourceAsStream(owl2jsonld);
        File tempFile = File.createTempFile("oba", "jar");
        copy(owl2_jar,tempFile);
        Runtime rt = Runtime.getRuntime();
        String[] cmdarray = new String[0];

        if (only_classes) {
            cmdarray = new String[]{"java", "-jar", tempFile.getPath(), "-c", ontology_file};
        }
        else {
            cmdarray = new String[]{"java", "-jar", tempFile.getPath(), ontology_file};

        }
        Process proc = rt.exec(cmdarray);

        BufferedReader stdInput = new BufferedReader(new
                InputStreamReader(proc.getInputStream()));

        BufferedReader stdError = new BufferedReader(new
                InputStreamReader(proc.getErrorStream()));

        String s = null;
        StringBuilder result = new StringBuilder();
        while ((s = stdInput.readLine()) != null) {
            result.append(s);
        }
        if (result != null){
            JSONObject json = new JSONObject(result.toString());
            //TODO: This is a hack to remove
            //     "": {"@id": "http://dbpedia.org/ontology/"}
            //temp_json is a reference to json
            JSONObject temp_json = (JSONObject) json.get("@context");

            if (!temp_json.isNull("")) {
                logger.warning("Generating of context.json - Ignoring the class " + temp_json.get("") + ": Name is empty.");
                temp_json.remove("");
            }
            return json;
        }
        while ((s = stdError.readLine()) != null) {
            logger.severe(s);
        }

        throw new IOException("no data");

    }

    /**
     * @param file_name
     * @return
     * @throws IOException
     */
    public static JSONObject read_json_file(String file_name) throws IOException {
        InputStream stream = Oba.class.getClassLoader().getResourceAsStream(file_name);
        byte b[] = new byte[stream.available()];
        JSONObject jsonObject = null;
        if (stream.read(b) == b.length) {
            jsonObject = new JSONObject(new String(b));
        }
        return jsonObject;
    }

    public static String check_trailing_slash(String string) {
        return string.endsWith("/") ? string : string + "/";
    }
    
    /**
    * Method that will download an ontology given its URI, doing content
    * negotiation The ontology will be downloaded in the first serialization
    * available (see Constants.POSSIBLE_VOCAB_SERIALIZATIONS)
    * @param uri the URI of the ontology
    * @param downloadPath path where the ontology will be saved locally.
    */
    public static void downloadOntology(String uri, String downloadPath) {
        for (String serialization : POSSIBLE_VOCAB_SERIALIZATIONS) {
            System.out.println("Attempting to download vocabulary in " + serialization);
            //logger is not initialized correctly and fails in tests (to fix)
            //logger.info("Attempting to download vocabulary in " + serialization);
            try {
                URL url = new URL(uri);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setInstanceFollowRedirects(true);
                connection.setRequestProperty("Accept", serialization);
                int status = connection.getResponseCode();
                boolean redirect = false;
                if (status != HttpURLConnection.HTTP_OK) {
                    if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM
                            || status == HttpURLConnection.HTTP_SEE_OTHER)
                        redirect = true;
                }
                // there are some vocabularies with multiple redirections:
                // 301 -> 303 -> owl
                while (redirect) {
                    String newUrl = connection.getHeaderField("Location");
                    connection = (HttpURLConnection) new URL(newUrl).openConnection();
                    connection.setRequestProperty("Accept", serialization);
                    status = connection.getResponseCode();
                    if (status != HttpURLConnection.HTTP_MOVED_TEMP && status != HttpURLConnection.HTTP_MOVED_PERM
                                    && status != HttpURLConnection.HTTP_SEE_OTHER)
                        redirect = false;
                }
                InputStream in = (InputStream) connection.getInputStream();
                Files.copy(in, Paths.get(downloadPath), StandardCopyOption.REPLACE_EXISTING);
                in.close();
                break; // if the vocabulary is downloaded, then we don't download it for the other
                                // serializations
            } catch (Exception e) {
                final String message = "Failed to download vocabulary in RDF format [" + serialization +"]: ";
                logger.severe(message + e.toString());
                throw new RuntimeException(message, e);
            }
        }
    }
    
    /**
     * Method that given a class, property or data property, searches for the best description.
     * @param entity entity to search.
     * @param ontology ontology to be used to search descriptions.
     * @return Description String (prioritizes English language)
     */
    public static String getDescription(OWLEntity entity, OWLOntology ontology){
        String descriptionValue = "Description not available";
        for(String description:ObaUtils.DESCRIPTION_PROPERTIES){
               Object[] annotationsObjects = EntitySearcher.getAnnotationObjects(entity, ontology, new OWLAnnotationPropertyImpl(new IRI(description) {
               })).toArray();
               if(annotationsObjects.length!=0){
                   Optional<OWLLiteral> descriptionLiteral;
                   for(Object annotation: annotationsObjects){
                       descriptionLiteral = ((OWLAnnotation) annotation).getValue().asLiteral();
                       if(descriptionLiteral.isPresent()){
                           if(annotationsObjects.length == 1 || descriptionLiteral.get().getLang().equals("en")){
                               descriptionValue = descriptionLiteral.get().getLiteral();
                           }
                       }
                   }
                   break;
               }
           }
        return descriptionValue;
    }
    
}

