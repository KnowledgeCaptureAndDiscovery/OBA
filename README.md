# Ontology-Based APIs (OBA) [![Build Status](https://travis-ci.org/KnowledgeCaptureAndDiscovery/OBA.svg?branch=master)](https://travis-ci.org/KnowledgeCaptureAndDiscovery/OBA)
[![DOI](https://zenodo.org/badge/184804693.svg)](https://zenodo.org/badge/latestdoi/184804693)


OBA reads ontologies (OWL) and generates an OpenAPI Specification (OAS). Using this definition, OBA creates a REST API server automatically.

![Diagram](docs/figures/oba.svg) 

## Quickstart

There are two option to run OBA:

1. Download the binary.
2. Build the binary from the repository.


### Downloading binary

1. Go the [latest release](https://github.com/KnowledgeCaptureAndDiscovery/OBA/releases/latest)
2. Download the file with extension .jar

### Building binary

1. Clone the repository `git clone https://github.com/KnowledgeCaptureAndDiscovery/OBA.git`
2. Install it using `mvn package`
3. The binary is available in the `target` directory

## Running

1. Create the OBA config file (config.yaml) from one of the [sample configuration files in the examples folder](examples/modelcatalog_reduced/config.yaml)
2. Use the configuration to run OBA with the following command:

```bash
$ java -jar oba-*-jar-with-dependencies.jar -c config.yaml
```

Congratulations! You have generated an Open Api Specification.

For instructions on using OBA to create your API server, go to the [documentation](https://oba.readthedocs.io/en/latest/)
