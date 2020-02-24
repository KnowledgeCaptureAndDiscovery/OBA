# Configuration File

OBA supports configuring your documentation builds with a YAML file.

Below is an example YAML file which may require some changes for your project's configuration:

```yaml
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

endpoint:
  url: https://endpoint.mint.isi.edu/modelCatalog-1.2.0
  prefix: https://w3id.org/okn/i/mint/
  graph_base: http://ontosoft.isi.edu:3030/modelCatalog-1.2.0/data/

firebase:
  key:
```


## Supported settings

### name

The name of OpenAPI

| Field | Value |
|---|---|
| **Required:** | ``true`` |

Example:

```yaml
name: modelcatalog
```


### output_dir

The output directory of the OpenApi specification files, relative to the root of the project.

| Field | Value |
|---|---|
| **Required:** | ``false`` |
| **Default:** | ``output`` |


Example:

```yaml
output_dir: outputs
```


### openapi

The path of the [OpenAPI Base file](https://swagger.io/docs/specification/basic-structure/)

| Field | Value |
|---|---|
| **Required:** | ``true`` |
| **Type:** | ``OpenAPI`` |


Example:

```yaml
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

### endpoint

Example

```yaml
endpoint:
  url: https://endpoint.mint.isi.edu/modelCatalog-1.2.0
  prefix: https://w3id.org/okn/i/mint/
  graph_base: http://ontosoft.isi.edu:3030/modelCatalog-1.2.0/data/
```

### endpoint.url

The url of the SPARQL Endpoint 

| Field | Value |
|---|---|
| **Required:** | ``true`` |
| **Type:** | ``url`` |


Example:

```yaml
  url: https://endpoint.mint.isi.edu/modelCatalog-1.2.0
```


### endpoint.prefix


The prefix of the SPARQL Endpoint 

| Field | Value |
|---|---|
| **Required:** | ``true`` |
| **Type:** | ``url`` |


Example:

```yaml
  prefix: https://w3id.org/okn/i/mint/
```


### endpoint.graph_base

OBA uses a graph to store the user contents on a personal namespace. 

| Field | Value |
|---|---|
| **Required:** | ``true`` |
| **Type:** | ``url`` |


Example:

```yaml
  graph_base: http://ontosoft.isi.edu:3030/modelCatalog-1.2.0/data/
```


## ontologies

Example:

```yaml
ontologies:
  - https://mintproject.github.io/Mint-ModelCatalog-Ontology/release/1.2.0/ontology.xml
  - https://knowledgecaptureanddiscovery.github.io/SoftwareDescriptionOntology/release/1.4.0/ontology.xml
```

| Field | Value |
|---|---|
| **Required:** | ``true`` |
| **Type:** | ``List[string]`` |

## firebase

You can use firebase to login

| Field | Value |
|---|---|
| **Required:** | ``false`` |
| **Type:** | ``dict`` |

```
firebase:
  key: key
```

### firebase.key

To authenticate a service account and authorize it to access Firebase services, you must generate a private key file.



| Field | Value |
|---|---|
| **Required:** | ``true`` |
| **Type:** | ``str`` |

```
firebase:
  key: key
```
