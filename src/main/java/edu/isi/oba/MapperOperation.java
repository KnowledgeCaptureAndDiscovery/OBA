package edu.isi.oba;

import io.swagger.annotations.Api;
import io.swagger.models.Method;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

import java.util.*;

enum Cardinality {
  SINGULAR, PLURAL
}

public class MapperOperation {
  String summary;
  String description;
  String schemaName;
  List<Parameter> parameters = new ArrayList<>();
  RequestBody requestBody = new RequestBody();
  ApiResponses apiResponses = new ApiResponses();
  Cardinality cardinality;
  Schema schema;
  String ref_text;

  public Operation getOperation() {
    return operation;
  }

  Operation operation;


  public MapperOperation(String schemaName, Method method, Cardinality cardinality) {
    this.cardinality = cardinality;
    this.schemaName = schemaName;
    String ref_text = "#/components/schemas/" + schemaName;
    schema = new Schema().$ref(ref_text);

    switch (method) {
      case GET:
        setOperationGet();
        break;
      case PUT:
        setOperationPut();
        break;
      case POST:
        setOperationPost();
        break;
      case DELETE:
        setOperationDelete();
        break;

    }

    if (cardinality == Cardinality.SINGULAR){
      parameters.add(new PathParameter()
              .description("The ID of the resource")
              .name("id")
              .required(true)
              .schema(new StringSchema()));
    }

    operation = new Operation()
            .description(description)
            .summary(summary)
            .addTagsItem(schemaName)
            .parameters(parameters)
            .responses(apiResponses);


    if (method == Method.PUT || method == Method.POST ){
      operation.setRequestBody(requestBody);
    }

  }

  private void setOperationGet() {
    String responseDescriptionOk;
    ApiResponse responseOk;
    //Set parameters
    parameters.add(new QueryParameter()
            .name("username")
            .description("Username to query")
            .required(false)
            .schema(new StringSchema()));

    switch (cardinality) {
      case PLURAL:
        summary = "List all " + this.schemaName + " entities";
        description = "Gets a list of all " + this.schemaName + " entities";
        ref_text = "#/components/schemas/" + this.schemaName;
        responseDescriptionOk = "Successful response - returns an array of " + schemaName + " entities.";

        //Set response
        ArraySchema schema = new ArraySchema();
        schema.setItems(this.schema);
        responseOk = new ApiResponse()
                .description(responseDescriptionOk)
                .content(new Content().addMediaType("application/json", new MediaType().schema(schema)));
        apiResponses.addApiResponse("200", responseOk);
        break;
      case SINGULAR:
        summary = "Get a " + this.schemaName;
        description = "Gets the details of a single instance of a " + this.schemaName;
        ref_text = "#/components/schemas/" + this.schemaName;
        responseDescriptionOk = "Gets the details of a single instance of  " + schemaName;

        //Set request
        responseOk = new ApiResponse()
                .description(responseDescriptionOk)
                .content(new Content().addMediaType("application/json", new MediaType().schema(this.schema)));
        apiResponses.addApiResponse("200", responseOk);
        break;

    }

  }

  private void setOperationPost() {
    String requestDescription = "A new " + this.schemaName + "to be created";

    //Edit global fields
    summary = "Create a " + this.schemaName;
    description = "Create a new instance of a " + this.schemaName;

    //Set request
    MediaType mediaType = new MediaType().schema(schema);
    Content content = new Content().addMediaType("application/json", mediaType);
    requestBody.setContent(content);
    requestBody.setDescription(requestDescription);


    //Set the response
    apiResponses.addApiResponse("201", new ApiResponse()
            .description("Created"));
  }


  private void setOperationPut() {
    String requestDescription = "An old " + this.schemaName + "to be updated";

    summary = "Update a " + this.schemaName;
    description = "Updates an existing " + this.schemaName;

    //Set request
    MediaType mediaType = new MediaType().schema(schema);
    Content content = new Content().addMediaType("application/json", mediaType);
    requestBody.setContent(content);
    requestBody.setDescription(requestDescription);

    //Set the response
    apiResponses
            .addApiResponse("200", new ApiResponse()
                    .description("Updated"))
            .addApiResponse("404", new ApiResponse()
                    .description("Not Found"));

  }


  private void setOperationDelete() {
    summary = "Delete a " + this.schemaName;
    description = "Delete an existing " + this.schemaName;

    //Set the response
    apiResponses
            .addApiResponse("202", new ApiResponse()
                    .description("Deleted"))
            .addApiResponse("404", new ApiResponse()
                    .description("Not Found"));

  }

}
