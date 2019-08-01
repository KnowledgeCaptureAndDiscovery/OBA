package edu.isi.oba;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

public class Path {


  public Path() {

  }

  private Parameter user_param(){
    return new QueryParameter()
            .description("Username to query")
            .required(false)
            .schema(new StringSchema());
  }

  private Operation plurar_get(String schema_name){
    String summary = "List All " + schema_name;
    String description = "Gets a list of all " + schema_name + " entities.";
    String description_response = "Successful response - returns an array of " + schema_name + " entities.";
    String ref_text = "#/components/schemas/" + schema_name;
    Schema ref = new Schema().$ref(ref_text);
    MediaType media_type = new MediaType().schema(ref);
    Content content = new Content().addMediaType("application/json", media_type);
    ApiResponse apiResponse = new ApiResponse()
            .description(description_response)
            .content(content);

    return new Operation()
            .description(description)
            .summary(summary)
            .addParametersItem(user_param())
            .responses(
                    new ApiResponses().addApiResponse("200", apiResponse)
            );

  }

  private Operation plurar_post(String schema_name){
    String summary = "Create a " + schema_name;
    String description = "Create a new instance of a " + schema_name;
    String description_response = "Successful response - returns an array of " + schema_name + "  entities.";
    String description_request = "A new " + schema_name + "to be created";

    String ref_text = "#/components/schemas/" + schema_name;
    Schema ref = new Schema().$ref(ref_text);
    MediaType media_type = new MediaType().schema(ref);
    Content content = new Content().addMediaType("application/json", media_type);
    RequestBody requestBody = new RequestBody();
    requestBody.setDescription(description_response);
    requestBody.setContent(content);

    ApiResponse apiResponse = new ApiResponse()
            .description("Successful response");

    return new Operation()
            .description(description)
            .summary(summary)
            .requestBody(requestBody)
            .responses(
                    new ApiResponses().addApiResponse("201", apiResponse)
            );

  }
  public PathItem generate_plurar(MapperSchema mapperSchema){
    String schema_name = mapperSchema.name;


    Operation get_operation = plurar_get(schema_name);
    Operation post_operation = plurar_post(schema_name);


    PathItem path = new PathItem()
                      .get(get_operation)
                      .post(post_operation);
    return path;
  }
}
