package edu.isi.oba;

import io.swagger.models.Method;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;

import java.math.BigDecimal;
import java.util.*;

enum Cardinality {
  SINGULAR, PLURAL
}

class MapperOperation {
  private boolean auth;
  private String summary;
  private String description;
  private final String schemaName;
  private final String schemaURI;
  private final List<Parameter> parameters = new ArrayList<>();
  private final RequestBody requestBody = new RequestBody();
  private final ApiResponses apiResponses = new ApiResponses();
  private final Cardinality cardinality;
  private final Schema schema;

  public Operation getOperation() {
    return operation;
  }

  private final Operation operation;


  public MapperOperation(String schemaName, String schemaURI, Method method, Cardinality cardinality, Boolean auth) {
    this.auth = auth;
    this.cardinality = cardinality;
    this.schemaName = schemaName;
    this.schemaURI = schemaURI;
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
              .description("The ID of the "+schemaName+" to be retrieved")
              .name("id")
              .required(true)
              .schema(new StringSchema()));
    }

    if (auth && (method == Method.PUT || method == Method.POST  || method == Method.DELETE )) {
      parameters.add(new QueryParameter()
              .description("Username")
              .name("user")
              .required(false)
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


    if (method == Method.PUT || method == Method.POST  || method == Method.DELETE ){
      SecurityRequirement securityRequirement = new SecurityRequirement();
      securityRequirement.addList("BearerAuth");
      operation.addSecurityItem(securityRequirement);
    }

  }

  private void setOperationGet() {
    String responseDescriptionOk;
    ApiResponse responseOk;
    //Set parameters
    if (this.auth)
      parameters.add(new QueryParameter()
              .name("username")
              .description("Name of the user graph to query")
              .required(false)
              .schema(new StringSchema()));

    switch (cardinality) {
      case PLURAL:
        summary = "List all instances of " + this.schemaName;
        description = "Gets a list of all instances of " + this.schemaName +
                " (more information in " +this.schemaURI+")";
        responseDescriptionOk = "Successful response - returns an array with the instances of " + schemaName + ".";

        //Set response
        ArraySchema schema = new ArraySchema();
        schema.setItems(this.schema);
        responseOk = new ApiResponse()
                .description(responseDescriptionOk)
                .content(new Content().addMediaType("application/json", new MediaType().schema(schema)));
        apiResponses.addApiResponse("200", responseOk);
        parameters.add(new QueryParameter()
                .name("label")
                .description("Filter by label")
                .required(false)
                .schema(new StringSchema()));
        parameters.add(new QueryParameter()
                .name("page")
                .description("Page number")
                .required(false)
                .schema(new IntegerSchema()._default(1)));
        parameters.add(new QueryParameter()
                .name("per_page")
                .description("Items per page")
                .required(false)
                .schema(new IntegerSchema()._default(100).maximum(BigDecimal.valueOf(200)).minimum(BigDecimal.valueOf(1))));
        break;
      case SINGULAR:
        summary = "Get a single " + this.schemaName +" by its id";
        description = "Gets the details of a given " + this.schemaName+
                " (more information in " +this.schemaURI+")";
        responseDescriptionOk = "Gets the details of a given " + schemaName;

        //Set request
        responseOk = new ApiResponse()
                .description(responseDescriptionOk)
                .content(new Content().addMediaType("application/json", new MediaType().schema(this.schema)));
        apiResponses.addApiResponse("200", responseOk);
        break;

    }

  }

  private void setOperationPost() {
    String requestDescription = "Information about the " + this.schemaName + "to be created";

    //Edit global fields
    summary = "Create one " + this.schemaName;
    description = "Create a new instance of " + this.schemaName+
                " (more information in " +this.schemaURI+")";

    //Set request
    MediaType mediaType = new MediaType().schema(schema);
    Content content = new Content().addMediaType("application/json", mediaType);
    requestBody.setContent(content);
    requestBody.setDescription(requestDescription);

    //Set the response
    apiResponses.addApiResponse("201", new ApiResponse()
            .content(content)
            .description("Created")
    );
  }


  private void setOperationPut() {
    String requestDescription = "An old " + this.schemaName + "to be updated";

    summary = "Update an existing " + this.schemaName;
    description = "Updates an existing " + this.schemaName+
                " (more information in " +this.schemaURI+")";

    //Set request
    MediaType mediaType = new MediaType().schema(schema);
    Content content = new Content().addMediaType("application/json", mediaType);
    requestBody.setContent(content);
    requestBody.setDescription(requestDescription);

    //Set the response
    apiResponses
            .addApiResponse("200", new ApiResponse()
                    .content(content)
                    .description("Updated")
            )
            .addApiResponse("404", new ApiResponse()
                    .description("Not Found"));

  }


  private void setOperationDelete() {
    summary = "Delete an existing " + this.schemaName;
    description = "Delete an existing " + this.schemaName+
                " (more information in " +this.schemaURI+")";

    //Set the response
    apiResponses
            .addApiResponse("202", new ApiResponse()
                    .description("Deleted"))
            .addApiResponse("404", new ApiResponse()
                    .description("Not Found"));

  }

}
