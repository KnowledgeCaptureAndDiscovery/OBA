Some ontologies contain numerous classes. However, you can be interested in a subgroup.
OBA can filter the classes.

The following example is selecting two classes:

  - http://dbpedia.org/ontology/Genre
  - http://dbpedia.org/ontology/Band

```yaml
### For more information about the section. Go to the official documentation
openapi:
  openapi: 3.0.1
  info:
    description: This is the API of the DBpedia Ontology
    title: DBpedia
    version: v1.3.0
  externalDocs:
    description: DBpedia
    url: https://w3id.org/okn/o/sdm
  servers:
    - url: https://dbpedia.oba.isi.edu/v1.3.0
    - url: http://localhost:8080/v1.3.0

## Ontologies
### List of ontologies
ontologies:
  - https://gist.githubusercontent.com/sirspock/cec147b24bd241295584dfcc21c21b93/raw/b6fa41ddf93212d967f35da20278f54d2ae2d40d/gistfile1.txt

## SPARQL information
endpoint:
  url: http://endpoint.mint.isi.edu/modelCatalog-1.2.0
  prefix: https://w3id.org/okn/i/mint
  graph_base: http://ontosoft.isi.edu:3030/modelCatalog-1.2.0/data/

## Filter the paths by methods
enable_get_paths: true
enable_post_paths: false
enable_delete_paths: false
enable_put_paths: false

## Select the classes to add in the API
classes:
  - http://dbpedia.org/ontology/Genre
  - http://dbpedia.org/ontology/Band
follow_references: false
```

The result is available at: [DBPedia Music](https://app.swaggerhub.com/apis/sirspock/dbpedia-music/v1.3.0)


### Following references

If you inspect the properties of a Band, you can see the a Band has one or more locationCity. However, a locationCity is a object then you don't have information about the object.

```yaml
components:
  schemas:
    Band:
      properties:
        locationCity:
          items:
            type: object
          nullable: true
          type: array
```

The option `follow_references` enables to follow the references.
Let's enable the option for the previous example.

Now, you have the whole information:
- A city has 423 properties. 
- One property is the leaderName and a leaderName is Person.
- A person has 285. 


!!! warning
    For large ontologies, we don't recommend use the option because the result can be too heavy.


```yaml
components:
  schemas:
    Band:
        locationCity:
          items:
            $ref: '#/components/schemas/City'
          nullable: true
          type: array
    City:
      properties:
        cityType:
          items:
            type: string
          nullable: true
          type: array
        irishName:
          items:
            type: string
          nullable: true
          type: array
        reffBourgmestre:
          items:
            $ref: '#/components/schemas/Person'
          nullable: true
          type: array
        communityIsoCode:
          items:
            type: string
          nullable: true
          type: array
        leaderName:
          items:
            $ref: '#/components/schemas/Person'
          nullable: true
          type: array
    Person:
      properties:
        parent:
          items:
            $ref: '#/components/schemas/Person'
          nullable: true
          type: array
        viafId:
          items:
            type: string
          nullable: true
          type: array
        competitionTitle:
          items:
            $ref: '#/components/schemas/SportsEvent'
          nullable: true
          type: array
        artPatron:
          items:
            $ref: '#/components/schemas/Artist'
          nullable: true
          type: array
        hairColour:
          items:
            type: string
          nullable: true
          type: array    
```