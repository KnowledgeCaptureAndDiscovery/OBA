package edu.isi.oba;

import io.swagger.models.Method;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Path {
  public Boolean enable_get_paths;
  public Boolean enable_post_paths;
  public Boolean enable_put_paths;
  public Boolean enable_delete_paths;
  private Boolean auth;

  public Path(Boolean enable_get_paths, Boolean enable_post_paths, Boolean enable_put_paths, Boolean enable_delete_paths, Boolean auth) {
    this.auth = auth;
    this.enable_get_paths = enable_get_paths;
    this.enable_post_paths = enable_post_paths;
    this.enable_put_paths = enable_put_paths;
    this.enable_delete_paths = enable_delete_paths;
  }

  public PathItem generate_singular(String schemaName){
    PathItem path_item = new PathItem();
    if (enable_get_paths)
      path_item.get(new MapperOperation(schemaName, Method.GET, Cardinality.SINGULAR, auth).getOperation());

    if (enable_delete_paths)
      path_item.delete(new MapperOperation(schemaName, Method.DELETE, Cardinality.SINGULAR, auth).getOperation());

    if (enable_put_paths)
      path_item.put(new MapperOperation(schemaName, Method.PUT, Cardinality.SINGULAR, auth).getOperation());

    return path_item;
  }


  public PathItem generate_plural(String schemaName){
    PathItem path_item = new PathItem();
    if (enable_get_paths)
      path_item.get(new MapperOperation(schemaName, Method.GET, Cardinality.PLURAL, auth).getOperation());
    if (enable_post_paths)
      path_item.post(new MapperOperation(schemaName, Method.POST, Cardinality.PLURAL, auth).getOperation());
    return path_item;
  }


  public PathItem user_login() {
    List<Parameter> parameters = new ArrayList<>();
    ApiResponses apiResponses = new ApiResponses();

    parameters.add(new PathParameter()
            .description("The user name for login")
            .name("username")
            .required(true)
            .in("query")
            .schema(new StringSchema()));

    parameters.add(new PathParameter()
            .description("The password for login in clear text")
            .name("password")
            .required(true)
            .in("query")
            .schema(new StringSchema()));

    Map<String, Header> headers = new HashMap<>();
    headers.put("X-Rate-Limit", new Header().description("calls per hour allowed by the user").
            schema(new IntegerSchema().format("int32")));
    headers.put("X-Expires-After", new Header().description("date in UTC when token expires").
            schema(new StringSchema().format("date-time")));
    apiResponses.addApiResponse("200", new ApiResponse()
            .description("successful operation")
            .headers(headers)
            .content(new Content().addMediaType("application/json", new MediaType().schema(new StringSchema()))));

    apiResponses.addApiResponse("400", new ApiResponse()
            .description("unsuccessful operation")
            .content(new Content().addMediaType("application/json", new MediaType().schema(new StringSchema()))));

    Map<String,Object> extensions = new HashMap<String, Object>();
    extensions.put("x-openapi-router-controller", "openapi_server.controllers.user_controller");
    Operation operation = new Operation()
            .description("Login the user")
            .extensions(extensions)
            .operationId("user_login_get")
            .parameters(parameters)
            .responses(apiResponses);
    return new PathItem().get(operation);
  }
}