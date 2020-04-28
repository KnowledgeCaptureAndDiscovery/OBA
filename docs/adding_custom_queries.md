
Sometimes, building the REST API from an ontology does not cover all the target queries that need to be supported. In order to address this issue, OBA can create paths in the API using custom SPARQL queries specified by users. 

## Defining Custom Queries

First, we must define the SPARQL queries we would like our API to support. For example, let's consider a [sample ontology](https://w3id.org/okn/o/sdm#)which we have already used with OBA. The ontology describes software metadata of complex physical models and, among other classes, it has a Model class and a Variable class. We need to support a special query: get all Models associated with a particular Variable label, which is described in the query below:


!!!info
    The query must be a **CONSTRUCT**, not SELECT

```
PREFIX sd: <https://w3id.org/okn/o/sd#>
PREFIX sdm: <https://w3id.org/okn/o/sdm#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>


CONSTRUCT {
    ?model ?predicate ?prop .
    ?prop a ?type
}
WHERE {
    GRAPH ?_g_iri {
        {
      		SELECT DISTINCT ?model {
                ?model sdm:usefulForCalculatingIndex ?sv .
                ?sv rdfs:label ?variableLabel
                FILTER REGEX(?variableLabel, ?_label, "i")
      		}
    	}
        ?model ?predicate ?prop
        OPTIONAL {
         ?prop a ?type
        }
    }
}
```

The query has two parameters:

- ?_g_iri (IRI): IRI of the user graph.
- ?_label (string): String of the label belonging to the variable we want to filter by.

We are using [BASIL's convention](https://github.com/the-open-university/basil/wiki/SPARQL-variable-name-convention-for-WEB-API-parameters-mapping).

Next, you have to save the query in the **custom** directory. For example, as *custom_models_variable.rq*. 

## Defining Custom Query Parameters

We have to extend the OpenAPI specification with the custom query:

!!! info
    You must be familiar with OpenAPI specitification. Please, read  [OpenAPI Docs - Describing parameters](https://swagger.io/docs/specification/describing-parameters/)


!!! warning
    You must add a new parameter with the name *custom_query_name*. 
    The default value of the parameter must be the filename of the custom query without the extension.
    In our example, this name is: **custom_models_variable**


```yaml
      parameters:
      - description: Username to query
        in: query
        name: username
        required: false
        schema:
          type: string
      - description: variable to search
        in: query
        name: label
        required: true
        schema:
            type: string
      - description: Name of the custom query
        in: query
        name: custom_query_name
        required: false
        schema:
          default: custom_models_variable
          type: string
```


## Defining the Custom Query Responses

Following the OpenAPI specification, we must select the type of response the query is returning. In this case, the response is a list of *Model* in a JSON Format.

```yaml
      responses:
        '200':
          content:
            application/json:
              schema:
                items:
                  $ref: '#/components/schemas/Model'
          description: Gets the details of a single instance of Model
      summary: Get a Model
```

## Defining the path name and method

In this case, the name is going to be `/custom/models/variable`; and we want it to be a GET method:
```yaml
  /custom/models/variable:
    get:
```

## Final result

The custom_paths must be a List of paths

```yaml
custom_paths:
  /custom/models/variable:
    get:
      description: Get models by variable name
      parameters:
      - description: Name of the custom query
        in: query
        name: custom_query_name
        required: false
        schema:
          default: custom_models_variable
          type: string
      - description: Username to query
        in: query
        name: username
        required: false
        schema:
          type: string
      - description: variable to search
        in: query
        name: label
        required: true
        schema:
            type: string
      responses:
        200:
          content:
            application/json:
              schema:
                items:
                  $ref: '#/components/schemas/Model'
          description: Gets a list of instance of Model
      summary: Get a list of Model
      tags:
      - Model
```

Finally, you must re run OBA.
