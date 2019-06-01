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

    public Serializer() throws IOException {
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


        Map<String, Schema> schemas = new HashMap<>();


        Map<String, Schema> properties = new HashMap<String, Schema>();


        //Data properties
        properties.put("id", getDataPropertiesBySchemaType(STRING_TYPE, false, false));
        properties.put("label", getDataPropertiesBySchemaType(STRING_TYPE, true, true));
        properties.put("type", getDataPropertiesBySchemaType(STRING_TYPE, true, true));
        properties.put("description", getDataPropertiesBySchemaType(STRING_TYPE, true, true));
        properties.put("hasDimensionality", getDataPropertiesBySchemaType(STRING_TYPE, true, true));
        //Object Properties
        properties.put("hasPresentation", getObjectPropertiesByRef("VariablePresentation", true,true));
        properties.put("hasOnePresentation", getObjectPropertiesByRef("VariablePresentation", false,true));

        //Create the schema using the properties
        schemas.put("ModelConfiguration", getSchema("Object", properties));

        openAPI.components(new Components().schemas(schemas));
        openAPI.setExtensions(new LinkedHashMap<String, Object>());
        openAPI.addExtension("x-custom", "value1");
        openAPI.addExtension("x-other", "value2");

        String content = SerializerUtils.toYamlString(openAPI);
        File file = new File("test.yaml");
        BufferedWriter writer = new BufferedWriter(new FileWriter(file.getAbsoluteFile()));
        writer.write(content);
        writer.close();
    }

    private Schema getDataPropertiesBySchemaType(String schemaType, Boolean array, Boolean nullable){
            switch (schemaType) {
                case STRING_TYPE:
                    return (array)? arraySchema(new StringSchema(), nullable) : new StringSchema().nullable(nullable);
                case INTEGER_TYPE:
                    return (array)? arraySchema(new IntegerSchema(), nullable) : new IntegerSchema().nullable(nullable);
                default:
                    return (array)? arraySchema(new ObjectSchema(), nullable) : new ObjectSchema().nullable(nullable);
            }
    }


    private Schema getSchema(String type, Map<String, Schema> properties) {
        Schema schema = new Schema();
        schema.setType(type);
        schema.setProperties(properties);
        schema.setRequired(required());
        return schema;
    }

    private List<String> required() {
        ArrayList<String> required = new ArrayList<String>() {{
            add("type");

        }};
        return required;
    }

    private Schema getObjectPropertiesByRef(String ref, boolean array , boolean nullable){
        if (array) {
            ArraySchema objects = new ArraySchema();
            Schema base = new  ObjectSchema();
            base.set$ref(ref);
            objects.setItems(base);
            objects.setNullable(nullable);
            return objects;
        }
        else {
            ObjectSchema object = new ObjectSchema();
            object.set$ref(ref);
            object.setNullable(nullable);
            return object;
        }
    }
    private ArraySchema arraySchema(Schema base, boolean nullable) {
        ArraySchema array = new ArraySchema();
        array.setNullable(nullable);
        array.setItems(base);
        return array;
    }

}
