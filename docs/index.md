# Ontology-Based APIs (OBA) [![Build Status](https://travis-ci.org/KnowledgeCaptureAndDiscovery/OBA.svg?branch=master)](https://travis-ci.org/KnowledgeCaptureAndDiscovery/OBA)


The Ontology-Based API (OBA) project reads ontologies (specified in OWL) and generates an OpenAPI Specification (OAS). Using this definition, it creates a REST API server automatically.

![Diagram](figures/oba.svg) 


## Tools

### OpenAPI

The OpenAPI Specification (OAS) defines a standard, language-agnostic interface to RESTful APIs which allows **both humans and computers to discover and understand** the capabilities of the service without access to source code, documentation, or through network traffic inspection. When properly defined, a consumer can understand and interact with the remote service with a minimal amount of implementation logic.
More information at [OpenAPI Specification | Swagger](https://swagger.io/specification/)

### OWL

The W3C Web Ontology Language (OWL) is a Semantic Web language designed to represent rich and complex knowledge about things, groups of things, and relations between things. OWL is a computational logic-based language such that knowledge expressed in **OWL can be exploited by computer programs**, e.g., to verify the consistency of that knowledge or to make implicit knowledge explicit. OWL documents, known as ontologies, can be published in the World Wide Web 