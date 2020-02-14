# OBA features

OBA features has the following features

##  Generate an OpenAPI specification from multiple ontologies (OWL).

We support converting OWL Classes to OpenAPI schemas and create paths (GET, POST, DELETE, PUT) for each schema.

### Paths

- Get all the resources of a type `GET /persons`
    - Search by a free text `GET /persons?label=pattern`
- Get one resource `GET /persons/{id}`
- Post a new resource `POST /persons`
- Put a existing resource `PUT /persons/{id}`
- Delete a existing resource `DELETE /persons/{id}`



## Generate SPARQL queries

For each of the previous paths, OBA generates the related queries


## Generate a Python Server

OBA can generate a Python Server using [OpenAPITools/openapi-generator](https://github.com/OpenAPITools/openapi-generator) and [Connexion](https://github.com/zalando/connexion) and integrate the previous queries with the server.


### Authorization

OBA supports authorization using Firebase as backend. 
The methods POST, PUT, DELETE require log-in and the resources are stored in a namespace (graph) per user.

## Other features

- Filter the paths: Select the classes to expose.
- Read an OpenAPI base file with the description, documentation and servers of the API.