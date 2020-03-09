
OBA can create paths using custom queries

## Defining the query

First, we must define the SPARQL query. In the Model Catalog, we need a special query: get all the Models related 
with a Variable.


!!!info
    The query must use **CONSTRUCT**


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

Then, we have two parameters:

- g (IRI): the IRI of the user graph.
- label (string): A string to filter by label.

Save the query in the custom directory. For example, *custom_models_variable.rq*. 

## Defining the parameters

We must to define the parameters on the OpenAPI specification.

!!! warning
    You must add a new parameter with the name: custom_query_name. 
    The default value of the parameter must be the filename of the custom query without the extension.
    In this example: **custom_models_variable**


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


## Defining the responses

The response is going to be a List of Model as a JSON Format.

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

In this case, the name is going to be `/custom/modelconfigurationsetups/variable` 
```yaml
  /custom/modelconfigurationsetups/variable:
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