

## Running with Docker

To run the server on a Docker container, execute the following command from the root directory:

```bash
# building the image
docker build -t openapi_server .

# starting up a container
docker run -p 8080:8080 openapi_server
```

and open the following URL in your browser:

```
http://localhost:8080/v1.3.0/ui/
```

!!! warning
    The version (v1.3.0) depends of your configuration. If you have questions, see the README file in the server.



Your OpenAPI definition lives here:

```
http://localhost:8080/v1.3.0/openapi.json
```

To launch the integration tests, install and execute [tox](https://pypi.org/project/tox/):
```
sudo pip install tox
tox
```

