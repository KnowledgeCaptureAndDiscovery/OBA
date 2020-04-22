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
import io.swagger.v3.oas.models.security.SecurityScheme;

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
  private final List<Parameter> parameters = new ArrayList<>();
  private final RequestBody requestBody = new RequestBody();
  private final ApiResponses apiResponses = new ApiResponses();
  private final Cardinality cardinality;
  private final Schema schema;

  public Operation getOperation() {
    return operation;
  }

  private final Operation operation;


  public MapperOperation(String schemaName, Method method, Cardinality cardinality, Boolean auth) {
    this.auth = auth;
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

    if (auth && (method == Method.PUT || method == Method.POST  || method == Method.DELETE )) {
      parameters.add(new PathParameter()
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
              .description("Username to query")
              .required(false)
              .schema(new StringSchema()));

    switch (cardinality) {
      case PLURAL:
        summary = "List all " + this.schemaName + " entities";
        description = "Gets a list of all " + this.schemaName + " entities";
        responseDescriptionOk = "Successful response - returns an array of " + schemaName + " entities.";

        //Set response
        ArraySchema schema = new ArraySchema();
        schema.setItems(this.schema);
        Map<String, Header> headers =  new HashMap<>();
        headers.put("link", new Header().schema(new StringSchema()).description("Information about pagination"));
        responseOk = new ApiResponse()
                .description(responseDescriptionOk)
                .headers(headers)
                .content(new Content().addMediaType("application/json", new MediaType().schema(schema)));
        apiResponses.addApiResponse("200", responseOk);
        parameters.add(new QueryParameter()
                .name("label")
                .description("Filter by label")
                .required(false)
                .schema(new StringSchema()));
        parameters.add(new QueryParameter()
                .name("page")
                .description("The page number")
                .required(false)
                .schema(new IntegerSchema()._default(1)));
        parameters.add(new QueryParameter()
                .name("per_page")
                .description("Items per page")
                .required(false)
                .schema(new IntegerSchema()._default(100).maximum(BigDecimal.valueOf(200)).minimum(BigDecimal.valueOf(1))));
        break;
      case SINGULAR:
        summary = "Get a " + this.schemaName;
        description = "Gets the details of a single instance of a " + this.schemaName;
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
            .content(content)
            .description("Created")
    );
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
                    .content(content)
                    .description("Updated")
            )
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
