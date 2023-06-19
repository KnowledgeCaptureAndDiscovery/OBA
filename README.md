# Ontology-Based APIs (OBA) [![Test](https://github.com/KnowledgeCaptureAndDiscovery/OBA/actions/workflows/build.yaml/badge.svg)](https://github.com/KnowledgeCaptureAndDiscovery/OBA/actions/workflows/build.yaml) [![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.6639554.svg)](https://doi.org/10.5281/zenodo.6639554)

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

1. Create the OBA config file (config.yaml) from one of the [sample configuration files in the examples folder](examples/modelcatalog/config.yaml)
2. Use the configuration to run OBA with the following command:

```bash
$ java -jar oba-*-jar-with-dependencies.jar -c config.yaml
```

Congratulations! You have generated an Open Api Specification.

For instructions on using OBA to create your API server, go to the [documentation](https://oba.readthedocs.io/en/latest/)

## Citation
Please cite our work as follows:

```
@inproceedings{garijo2020OBA,
	title        = {{OBA}: An Ontology-Based Framework for Creating REST APIs for Knowledge Graphs},
	author       = {Garijo, Daniel and Osorio, Maximiliano},
	booktitle={International Semantic Web Conference},
	pages={48--64},
	year={2020},
    doi={https://doi.org/10.1007/978-3-030-62466-8_4},
	organization = {Springer, Cham},
    isbn={978-3-030-62466-8}
}
```
