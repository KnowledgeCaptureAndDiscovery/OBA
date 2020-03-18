# OBA: Features

##  Generating an OpenAPI specification from multiple OWL ontologies

OBA converts OWL Classes to OpenAPI schemas and creates paths (GET, POST, DELETE, PUT) for each schema.

### Paths
For each class in the provided ontology (unless filtered) OBA generates the following paths:

- Get all the resources of a type `GET /persons`
    - Search by a free text `GET /persons?label=pattern`
- Get one resource `GET /persons/{id}`
- Post a new resource `POST /persons`
- Put a existing resource `PUT /persons/{id}`
- Delete a existing resource `DELETE /persons/{id}`


## Generating SPARQL query templates

For each of these paths, OBA generates the SPARQL queries that are necessary to retrieve them from a target SPARQL endpoint.


## Generating a Python Server

OBA generates a Python Server using [OpenAPITools/openapi-generator](https://github.com/OpenAPITools/openapi-generator) and [Connexion](https://github.com/zalando/connexion), integrating the SPARQL queries with the server.


### Authorization

OBA supports authorization using Firebase as backend. When using POST, PUT and DELETE methods with a Knowledge Graph, OBA requires log-in per user. OBA separates the contributions and editions of each user in a different named graph, the user id as the id named graph URI.

## Other features

- Filter the paths: OBA allows selecting a subset of the classes to expose, as large ontologies may lead to big specifications. 
- Read an OpenAPI base file with the description, documentation and servers of the API.