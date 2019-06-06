package edu.isi.oba;


import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;

import org.openapitools.codegen.serializer.SerializerUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;



public class Serializer {
    public static final String STRING_TYPE = "string";
    public static final String INTEGER_TYPE = "integer";
    //TODO: validate the yaml
    public Serializer(Map<String, Schema> schemas) throws IOException {
        OpenAPI openAPI = new OpenAPI();
        openAPI.setInfo(new Info().title("Some title").description("Some description"));
        openAPI.setExternalDocs(new ExternalDocumentation().url("http://abcdef.com").description("a-description"));
        openAPI.setServers(Arrays.asList(
                new Server().url("http://www.server1.com").description("first server"),
                new Server().url("http://www.server2.com").description("second server")
        ));
        openAPI.setSecurity(Arrays.asList(
                new SecurityRequirement().addList("some_auth", Arrays.asList("write", "read"))
        ));
        openAPI.setTags(Arrays.asList(
                new Tag().name("tag1").description("some 1 description"),
                new Tag().name("tag2").description("some 2 description"),
                new Tag().name("tag3").description("some 3 description")
        ));
        openAPI.path("/ping/pong", new PathItem().get(new Operation()
                .description("Some description")
                .operationId("pingOp")
                .responses(new ApiResponses().addApiResponse("200", new ApiResponse().description("Ok")))));



        openAPI.components(new Components().schemas(schemas));
        openAPI.setExtensions(new LinkedHashMap<String, Object>());
        openAPI.addExtension("x-custom", "value1");
        openAPI.addExtension("x-other", "value2");

        String content = SerializerUtils.toYamlString(openAPI);
        File file = new File("example.yaml");
        BufferedWriter writer = new BufferedWriter(new FileWriter(file.getAbsoluteFile()));
        writer.write(content);
        writer.close();
    }


}
