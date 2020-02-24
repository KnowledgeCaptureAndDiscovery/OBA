## Installation 

### Downloading binary


1. Go the [latest release](https://github.com/KnowledgeCaptureAndDiscovery/OBA/releases/latest)
2. Download the file with extension .jar


### Building binary from the resource

1. Clone the repository `git clone https://github.com/KnowledgeCaptureAndDiscovery/OBA.git`
2. Install it using `mvn package`


## Running

- Create the OBA config file from the [sample configuration](config.yaml.sample)
```
ontologies:
  - https://mintproject.github.io/Mint-ModelCatalog-Ontology/release/1.2.0/ontology.xml
  - https://knowledgecaptureanddiscovery.github.io/SoftwareDescriptionOntology/release/1.4.0/ontology.xml
name: modelcatalog
output_dir: outputs

openapi:
  openapi: 3.0.1
  info:
    description: This is the API of the  Software Description Ontology
      at [https://mintproject.github.io/Mint-ModelCatalog-Ontology/release/1.3.0/index-en.html](https://w3id.org/okn/o/sdm)
    title: Model Catalog
    version: v1.3.0
  externalDocs:
    description: Model Catalog
    url: https://w3id.org/okn/o/sdm
  servers:
    - url: https://api.models.mint.isi.edu/v1.3.0
    - url: https://dev.api.models.mint.isi.edu/v1.3.0
    - url: http://localhost:8080/v1.3.0
```

- Pass the configuration and run OBA
```bash
$ java -jar oba-2.2.0-jar-with-dependencies.jar -c config.yaml
```
