# OBA

OBA project reads ontologies (OWL) and generates the OpenAPI Specification (OAS). Using this definition, it creates a REST API server automatically. Optionally, OBA can query an RDF endpoint using [OBA_sparql: A Python module based on GRLC to execute queries](https://github.com/knowledgeCaptureAndDiscovery/oba_sparql)

![Diagram](oba.svg) 

## Tools

### OpenAPI

The OpenAPI Specification (OAS) defines a standard, language-agnostic interface to RESTful APIs which allows **both humans and computers to discover and understand** the capabilities of the service without access to source code, documentation, or through network traffic inspection. When properly defined, a consumer can understand and interact with the remote service with a minimal amount of implementation logic.
More information at [OpenAPI Specification | Swagger](https://swagger.io/specification/)

### OWL

The W3C Web Ontology Language (OWL) is a Semantic Web language designed to represent rich and complex knowledge about things, groups of things, and relations between things. OWL is a computational logic-based language such that knowledge expressed in **OWL can be exploited by computer programs**, e.g., to verify the consistency of that knowledge or to make implicit knowledge explicit. OWL documents, known as ontologies, can be published in the World Wide Web 
