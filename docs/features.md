# OBA features

This will serve as a list of all of the features that OBA currently has.

##  Generate an OpenAPI specification from multiple ontologies (OWL).

We now support converting OWL Classes to OpenAPI schemas and create paths (GET, POST, DELETE, PUT) for each schema.


## Generate SPARQL queries

For each classes, we now generate five queries

- Get all the resources of a type `/persons`
- Get one resource `/persons/{id}`
- Get all the resources related to another resource `/persons/{id}/pets`

 Read an OpenAPI base file with the description, documentation and servers of the API.