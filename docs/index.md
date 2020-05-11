# Ontology-Based APIs (OBA) [![Build Status](https://travis-ci.org/KnowledgeCaptureAndDiscovery/OBA.svg?branch=master)](https://travis-ci.org/KnowledgeCaptureAndDiscovery/OBA)

**Authors**: Maximiliano Osorio and Daniel Garijo.


The Ontology-Based API (OBA) framework takes as input an ontology or ontology network (specified in OWL) and generates an OpenAPI Specification (OAS). Using this definition, OBA creates a REST API server automatically that can validate the requests from users; deliver JSON objects following the structure described in the ontology; accept custom queries needed by users; and support clients for easing the interaction with the API. Figure 1 shows a snapshot of the different capabilities of OBA: 

![Diagram](figures/oba.svg)
**Figure 1**: Overview off the capabilities of OBA 

!!! info
    If you experience any issues when using OBA, or if you would like us to support additional exciting features, please open an issue on our [GitHub repository](https://github.com/KnowledgeCaptureAndDiscovery/OBA/issues).

To see how to install and use OBA, just follow this [link](quickstart.md).

## Why OBA?
We developed OBA to help bridge the gap between ontology engineers -who create and maintain knowledge graphs using RDF, OWL and SPARQL- and Web developers who need to consume data in knowledge graphs but are not familiar with Semantic Web standards. OBA helps ontology engineers sketch and customize APIs created directly from their ontologies, making the process easy to maintain. 

## Used Technologies and Standards

### OpenAPI

The OpenAPI Specification (OAS) defines a standard, language-agnostic interface for RESTful APIs which allows **both humans and computers to discover and understand** the capabilities of the service without having to inspect the source code or network traffic. When properly defined, a consumer can understand and interact with the remote service with a minimal amount of implementation logic.
More information about Open API can be found at the [OpenAPI Specification official page (Swagger)](https://swagger.io/specification/)

### OWL

The [W3C Web Ontology Language (OWL)](https://www.w3.org/TR/owl-semantics/) is a Semantic Web language designed to represent rich and complex knowledge about things, groups of things, and relations between things. OWL is a computational logic-based language such that knowledge expressed in **OWL can be exploited by computer programs**, e.g., to verify the consistency of that knowledge or to make implicit knowledge explicit. OWL documents, known as ontologies, can be published in the World Wide Web. 

### JSON
The JavaScript Object Notation (JSON) is a syntax for storing and exchanging data commonly used by Web developers.

### JSON-LD
JSON-LD is "a JSON-based format to serialize Linked Data. The syntax is designed to easily integrate into deployed systems that already use JSON, and provides a smooth upgrade path from JSON to JSON-LD". [[Source: Json-LD1.1 specification](https://www.w3.org/TR/json-ld11/)]

OBA uses JSON-LD to transform JSON-LD results to JSON.

### SPARQL
[W3C standard](https://www.w3.org/TR/sparql11-query/) describing a query language for RDF. OBA uses SPARQL queries to retrieve the contents of a given knowledge graph according the contents of an ontology.
