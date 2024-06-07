package edu.isi.oba;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.*;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import org.openapitools.codegen.serializer.SerializerUtils;

class Serializer {
  String openapi_path;

  public  Serializer(Mapper mapper, java.nio.file.Path dir, OpenAPI openAPI, LinkedHashMap<String, PathItem> custom_paths, Boolean saveAsJSON) throws Exception {
    Map<String, Object> extensions = new HashMap<String, Object>();
    final String openapi_file = Optional.ofNullable(saveAsJSON).orElse(false) ? "openapi.json" : "openapi.yaml";

    //Generate security schema
    Map<String, SecurityScheme> securitySchemes = new HashMap<String, SecurityScheme>();
    SecurityScheme securityScheme = getSecurityScheme(extensions);
    securitySchemes.put("BearerAuth", securityScheme);

    Components components = new Components();
    Paths paths = new Paths();
    mapper.getPaths().forEach((k, v) -> paths.addPathItem(k, v));
    mapper.getSchemas().forEach((k, v) -> components.addSchemas(k, v));
    components.securitySchemes(securitySchemes);

    //add custom paths
    Map<String, Object> custom_extensions = new HashMap<String, Object>();
    custom_extensions.put("x-oba-custom", true);

    if (custom_paths != null)
      custom_paths.forEach((k, v) -> {
        System.out.println("inserting custom query " + k);
        v.setExtensions(custom_extensions);
        paths.addPathItem(k, v);
      });

    openAPI.setPaths(paths);
    openAPI.components(components);

    //write the filename
    final var content = Optional.ofNullable(saveAsJSON).orElse(false) ? SerializerUtils.toJsonString(openAPI) : SerializerUtils.toYamlString(openAPI);
    this.openapi_path = dir + File.separator + openapi_file;
    File file = new File(openapi_path);
    BufferedWriter writer = new BufferedWriter(new FileWriter(file.getAbsoluteFile()));
    writer.write(content);
    writer.close();
    this.validate();
  }

  private void validate() throws Exception {
    ParseOptions options = new ParseOptions();
    options.setResolve(true);
    SwaggerParseResult result = new OpenAPIV3Parser().readLocation(openapi_path, null, options);
    List<String> messageList = result.getMessages();
    Set<String> errors = new HashSet<>(messageList);
    Set<String> warnings = new HashSet<>();
    errors.forEach((errorMessage) -> {
      if (errorMessage.contains(".$ref target #/components/schemas/") 
          && errorMessage.contains("is not of expected type Examples")) {
        // Ignore.  The validator complains because the reference is to a Schemas item (which is valid) instead of an Examples item.
      } else {
        throw new RuntimeException(errorMessage);
      }
    });
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
