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
  public  Serializer(List<Mapper> mappers, java.nio.file.Path dir, OpenAPI openAPI, LinkedHashMap<String, PathItem> custom_paths) throws IOException {
    Map<String, Object> extensions = new HashMap<String, Object>();
    final String openapi_file = "openapi.yaml";

    //Generate security schema
    Map<String, SecurityScheme> securitySchemes = new HashMap<String, SecurityScheme>();
    SecurityScheme securityScheme = getSecurityScheme(extensions);
    securitySchemes.put("BearerAuth", securityScheme);

    Components components = new Components();
    Paths paths = new Paths();
    Iterator i = mappers.iterator();
    while (i.hasNext()) {
      Mapper mapper = (Mapper) i.next();
      mapper.paths.forEach((k, v) -> paths.addPathItem(k, v));
      mapper.schemas.forEach((k, v) -> components.addSchemas(k, v));
      components.securitySchemes(securitySchemes);
    }

    //add custom paths
    Map<String, Object> custom_extensions = new HashMap<String, Object>();
    custom_extensions.put("x-oba-custom", true);

    custom_paths.forEach((k, v) -> {
      System.out.println("inserting custom query " + k);
      v.setExtensions(custom_extensions);
      paths.addPathItem(k, v);
    });

    openAPI.setPaths(paths);
    openAPI.components(components);

    //write the filename
    String content = SerializerUtils.toYamlString(openAPI);
    File file = new File(dir + File.separator + openapi_file );
    BufferedWriter writer = new BufferedWriter(new FileWriter(file.getAbsoluteFile()));
    writer.write(content);
    writer.close();
  }

  private SecurityScheme getSecurityScheme(Map<String, Object> extensions) {
    extensions.put("x-bearerInfoFunc", "openapi_server.controllers.user_controller.decode_token");
    SecurityScheme securityScheme = new SecurityScheme();
    securityScheme.setType(SecurityScheme.Type.HTTP);
    securityScheme.bearerFormat("JWT");
    securityScheme.setScheme("bearer");
    securityScheme.setExtensions(extensions);
    return securityScheme;
  }
}
