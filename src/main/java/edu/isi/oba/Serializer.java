package edu.isi.oba;


import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

import org.openapitools.codegen.serializer.SerializerUtils;
import org.semanticweb.owlapi.model.IRI;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;



class Serializer {
  //TODO: validate the yaml
  public  Serializer(List<Mapper> mappers, java.nio.file.Path dir) throws IOException {
    String url = "https://w3id.org/mint/modelCatalog/";
    OpenAPI openAPI = new OpenAPI();
    final String title = "Model Catalog";
    final String version = "v1.0.0";
    String description = "This is MINT Model Catalog You can find out more about Model Catalog at [" + url + "](" + url + ")";
    openAPI.setInfo(new Info().title(title).description(description).version(version));
    openAPI.setExternalDocs(new ExternalDocumentation().url(url).description("Model Catalog"));

    ArrayList<String> server_address = new ArrayList<>();
    ArrayList<Server> servers = new ArrayList<>();

    server_address.add("https://api.models.mint.isi.edu/" + version);
    server_address.add("https://dev.api.models.mint.isi.edu/" + version);
    server_address.add("http://localhost:8080/" + version);


    for (Iterator i = server_address.iterator(); i.hasNext(); )
      servers.add(new Server().url(String.valueOf(i.next())));

    openAPI.setServers(servers);

    Map<String, Object> extensions = new HashMap<String, Object>();
    Map<String, SecurityScheme> securitySchemes = new HashMap<String, SecurityScheme>();
    extensions.put("x-bearerInfoFunc", "openapi_server.controllers.user_controller.decode_token");
    SecurityScheme securityScheme = new SecurityScheme();
    securityScheme.setType(SecurityScheme.Type.HTTP);
    securityScheme.bearerFormat("JWT");
    securityScheme.setScheme("bearer");
    securityScheme.setExtensions(extensions);
    securitySchemes.put("BearerAuth", securityScheme);

    Mapper mapper = null;
    Components components = new Components();
    Paths oepnapiPaths = new Paths();
    FileWriter myWriter = new FileWriter(dir + File.separator + ".openapi-generator/template/static_files/utils/vars.py");
    Iterator i = mappers.iterator();
    while (i.hasNext()) {

      mapper = (Mapper) i.next();
      String name;
      IRI clsIRI;

      Iterator it = mapper.schemaNames.entrySet().iterator();


      while (it.hasNext()) {
        Map.Entry pair = (Map.Entry)it.next();
        clsIRI = (IRI) pair.getKey();
        name = (String) pair.getValue();
        try {
          myWriter.write(name.toUpperCase() + "_TYPE_URI = \"" +  clsIRI + "\"\n");
          myWriter.write(name.toUpperCase() + "_TYPE_NAME = \"" +  name + "\"\n");
        } catch (IOException e) {
          System.out.println("An error occurred.");
          e.printStackTrace();
        }
      }

      System.out.println("inserting schemas and paths of " + mapper.ont_prefix);
      mapper.paths.forEach((k, v) -> oepnapiPaths.addPathItem(k, v));
      mapper.schemas.forEach((k, v) -> components.addSchemas(k, v));
      components.securitySchemes(securitySchemes);
    }
    myWriter.close();

    openAPI.setPaths(oepnapiPaths);
    openAPI.components(components);

    //write the filename
    String content = SerializerUtils.toYamlString(openAPI);
    File file = new File(dir + File.separator + "openapi.yaml");
    BufferedWriter writer = new BufferedWriter(new FileWriter(file.getAbsoluteFile()));
    writer.write(content);
    writer.close();
  }


}
