# Running API tests

The resultant server code contains the tests to evaluate the status of your API and Knowledge Graph.

## Usage

To use the tests, you must install `tox`:

```bash
$ pip install tox
```

And run it:

```bash
$ tox 
```

You can modify the test requirements at `src/test-requirements.txt`.

## Editing

The tests located at `server/openapi_server/test/`. You can read the following [docuentation](https://nose.readthedocs.io/en/latest/testing.html) to understand how to edit them

### Configure


There are two useful option to test your API and Knowledge Graph:

- validate_responses can be useful to detect invalid properties or types on your Knowledge Graph.
- strict_validation can be helpful to see an invalid request.

```
        :param validate_responses: True enables validation. 
        Validation errors generate HTTP 500 responses.
        :type validate_responses: bool
        :param strict_validation: 
        True enables validation on invalid request parameters
        :type strict_validation: bool
```



You can edit these option at `server/openapi_server/test/__init__.py`

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