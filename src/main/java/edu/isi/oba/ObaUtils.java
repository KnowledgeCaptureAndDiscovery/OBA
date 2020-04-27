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

public class ObaUtils {

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

    public static JSONObject generate_context_file(String[] ontologies) throws Exception {
        JSONObject[] jsons = new JSONObject[ontologies.length];
        for (int i = 0; i < ontologies.length; i++) {
            try {
                jsons[i] = run_owl_jsonld(ontologies[i]);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return ObaUtils.concat_json_common_key(jsons, "@context");
    }

    private static JSONObject run_owl_jsonld(String ontology_url) throws Exception {
        String owl2jsonld = "/owl2jsonld-0.3.0-SNAPSHOT-standalone.jar";
        InputStream owl2_jar = ObaUtils.class.getResourceAsStream(owl2jsonld);
        File tempFile = File.createTempFile("oba", "jar");
        copy(owl2_jar,tempFile);
        Runtime rt = Runtime.getRuntime();
        Process proc = rt.exec(new String[]{"java", "-jar", tempFile.getPath(), ontology_url});

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

}