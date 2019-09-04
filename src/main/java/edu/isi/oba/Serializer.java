package edu.isi.oba;


import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

import org.openapitools.codegen.serializer.SerializerUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;



class Serializer {
  //TODO: validate the yaml
    public Serializer(Map<String, Schema> schemas, Paths paths) throws IOException {
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

        Map<String,Object> extensions = new HashMap<String, Object>();
        Map<String, SecurityScheme> securitySchemes = new HashMap<String, SecurityScheme>();
        extensions.put("x-google-issuer", "https://securetoken.google.com/YOUR-PROJECT-ID");
        extensions.put("x-google-jwks_uri", "https://www.googleapis.com/service_accounts/v1/metadata/x509/securetoken@system.gserviceaccount.com");
        extensions.put("x-google-audiences", "YOUR-PROJECT-ID");
        SecurityScheme securityScheme = new SecurityScheme();
        securityScheme.setType(SecurityScheme.Type.OAUTH2);

        OAuthFlows flows = new OAuthFlows();
        flows.implicit(new OAuthFlow());
        securityScheme.setExtensions(extensions);
        securitySchemes.put("Google", securityScheme);
//        openAPI.setTags(Arrays.asList(
//                new Tag().name("tag1").description("some 1 description"),
//                new Tag().name("tag2").description("some 2 description"),
//                new Tag().name("tag3").description("some 3 description")
//        ));
        openAPI.setPaths(paths);

      Components components = new Components();
      components.schemas(schemas);
      //components.securitySchemes(securitySchemes);
      openAPI.components(components);
//        openAPI.setExtensions(new LinkedHashMap<String, Object>());
//        openAPI.addExtension("x-custom", "value1");
//        openAPI.addExtension("x-other", "value2");

        String content = SerializerUtils.toYamlString(openAPI);
        File file = new File("example.yaml");
        BufferedWriter writer = new BufferedWriter(new FileWriter(file.getAbsoluteFile()));
        writer.write(content);
        writer.close();
    }


}
