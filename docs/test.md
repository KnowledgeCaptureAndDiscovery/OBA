# Running API tests

The resultant server code contains the tests to evaluate the status of your API against a knowledge graph.

## Installation

To use the tests, you must install `tox`:

```bash
$ pip install tox
```

## Before you run the tests
OBA creates tests for all the paths in your API, including specific instances. However, since the instances to test are unknown beforehand, OBA uses a placeholder `id_example` which has to be modified with the instance id you want to test. For example, for the `dbpedia_music` example, the files `servers/python/server/openapi_server/test/test_band_controller.py` and `python/server/openapi_server/test/test_genre_controller.py` will need to be modified with a band name and a music genre of your choice. You can choose an id by running your API, e.g., for bands:

```
curl -X GET "http://localhost:8080/v1.3.0/bands?page=1&per_page=10" -H  "accept: application/json"
```

And then selecting one to change in the test file. For example, in test_band_controller.py  we can ask for the Black_Sabbath, changing 
```
        response = self.client.open(
            '/v1.3.0/bands/{id}'.format(id='id_example'),
```
into
```
        response = self.client.open(
            '/v1.3.0/bands/{id}'.format(id='Black_Sabbath'),
```

You will need to provide a sample id to test for all the paths of the API that test individual instances, or the tests will fail.

## Running the tests

And run the tests with:

```bash
$ tox 
```

You can modify the test requirements in `src/test-requirements.txt`.

## Editing

The tests are located in `server/openapi_server/test/`. You can read the following [documentation](https://nose.readthedocs.io/en/latest/testing.html) to understand how to edit them.

### Configure


There are two useful options to test your API against a knowledge graph:

- validate_responses can be useful to detect invalid properties or types on your knowledge graph.
- strict_validation can be helpful to see an invalid request.

```
        :param validate_responses: True enables validation. 
        Validation errors generate HTTP 500 responses.
        :type validate_responses: bool
        :param strict_validation: 
        True enables validation on invalid request parameters
        :type strict_validation: bool
```



You can edit these option in `server/openapi_server/test/__init__.py`

```python
    def create_app(self):
        Specification.from_file = CachedSpecification.from_file
        app = connexion.App(__name__, specification_dir='../openapi/')
        app.app.json_encoder = JSONEncoder
        app.add_api('openapi.yaml',
                    pythonic_params=False,
                    validate_responses=True)
        return app.app
```

## Examples

The following OpenAPI Operation (`/bands`) is going to generate a test with the parameters and request formats required.

```yaml
  /bands:
    get:
      description: Gets a list of all instances of Band (more information in http://dbpedia.org/ontology/Band)
      operationId: bands_get
      parameters:
      - description: Filter by label
        explode: true
        in: query
        name: label
        required: false
        schema:
          type: string
        style: form
      - description: Page number
        explode: true
        in: query
        name: page
        required: false
        schema:
          default: 1
          format: int32
          type: integer
        style: form
      - description: Items per page
        explode: true
        in: query
        name: per_page
        required: false
        schema:
          default: 100
          format: int32
          maximum: 200
          minimum: 1
          type: integer
        style: form
      responses:
        200:
          content:
            application/json:
              schema:
                items:
                  $ref: '#/components/schemas/Band'
                type: array
          description: Successful response - returns an array with the instances of
            Band.
      summary: List all instances of Band
      tags:
      - Band
      x-openapi-router-controller: openapi_server.controllers.band_controller
```


```python
    def test_bands_get(self):
        """Test case for bands_get

        List all instances of Band
        """
        query_string = [('label', 'label_example'),
                        ('page', 1),
                        ('per_page', 100)]
        headers = { 
            'Accept': 'application/json',
        }
        response = self.client.open(
            '/v1.3.0/bands',
            method='GET',
            headers=headers,
            query_string=query_string)
        self.assert200(response,
                       'Response body is : ' + response.data.decode('utf-8'))
```