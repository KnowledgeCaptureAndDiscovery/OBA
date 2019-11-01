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
  public  Serializer(List<Mapper> mappers, java.nio.file.Path dir, OpenAPI openAPI) throws IOException {
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
          String variable_name = name.toUpperCase().replace("-", "") ;
          myWriter.write(variable_name + "_TYPE_URI = \"" +  clsIRI + "\"\n");
          myWriter.write(variable_name + "_TYPE_NAME = \"" +  name + "\"\n");
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
