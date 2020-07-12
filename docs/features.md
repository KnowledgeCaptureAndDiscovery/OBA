# OBA: Features
OBA's features are organized on its two main functionalities: the generation of an OpenAPI specification from an ontology and the generation of a server with a given specification:

  - [Generating an OpenAPI Specification from OWL ontologies](#generating-an-openapi-specification-from-owl-ontologies)
    - [Generating SPARQL query templates](#generating-sparql-query-templates)
    - [Generating JSON-LD contexts](#generating-json-ld-contexts)
    - [Class filtering](#class-filtering)
    - [Documentation](#documentation)
  - [Generating a Python Server from an OAS specification](#generating-a-python-server-from-an-oas-specification)
    - [Automatic query handling](#automatic-query-handling)
    - [Custom query support](#custom-query-support)
    - [Authorization](#authorization)


##  Generating an OpenAPI Specification from OWL ontologies

OBA converts one or multiple OWL ontologies to OpenAPI schemas and creates paths (GET, POST, DELETE, PUT) for each schema corresponding to an ontology class. OBA will document automatically the specification from the definitions provided in the ontology (using rdfs:comment, skos:definition or prov:definition annotations). Each ontology can be loaded from your local computer or from its URI.

**Paths:**
For each class in the provided ontology (unless filtered) OBA generates the following paths:

- Get all the resources of a type `GET /persons`
    - Search by a free text `GET /persons?label=pattern`
- Get one resource `GET /persons/{id}`
- Post a new resource `POST /persons`
- Put a existing resource `PUT /persons/{id}`
- Delete a existing resource `DELETE /persons/{id}`


### Generating SPARQL query templates

For each of these paths, OBA generates the SPARQL queries that are necessary to retrieve them from a target SPARQL endpoint.

### Generating JSON-LD contexts

OBA converts the format of the responses from JSON/LD to plain JSON, which is a widely format used by Web developers. In order to achieve this, OBA requires two files with the context of the ontologies used. The context is used to map simple terms to IRIs. OBA generates these context file automatically.

!!! note
    Since OBA 3.3.0, OBA uses two files `context.json` and `context_class.json`. The first file contains all the mappings (classes and properties) and the second file contains the classes mapping. OBAsparql uses the `context.json` for `POST` and `PUT` method and uses the `context_class.json` for `GET` method.

!!! info
    OBA reuses and extends [owl2jsonld](https://github.com/stain/owl2jsonld), developed by [Stain Soiland-Reyes](https://github.com/stain).

### Class filtering

OBA allows selecting a subset of the classes to expose, as large ontologies may lead to big specifications. 

### Documentation

OBA will document automatically your API using the annotations found in your ontologies.

## Generating a Python Server from an OAS specification

OBA generates a Python Server using [OpenAPITools/openapi-generator](https://github.com/OpenAPITools/openapi-generator) and [Connexion](https://github.com/zalando/connexion), integrating the SPARQL queries with the server.

### Automatic query handling

For each of the paths in the OpenAPI specification, OBA will automatically convert any requests to deliver a JSON file that follows the structure specified in the ontology.

### Custom query support

OBA will allow you to add custom paths in the API using your own SPARQL queries. 

### Authorization

OBA supports authorization using Firebase as backend. When using POST, PUT and DELETE methods with a Knowledge Graph, OBA requires log-in per user. OBA separates the contributions and editions of each user in a different named graph, the user id as the id named graph URI.
