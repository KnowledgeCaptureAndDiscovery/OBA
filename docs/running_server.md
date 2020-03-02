

## Running with Docker

To run the server on a Docker container, please execute the following from the root directory:

```bash
# building the image
docker build -t openapi_server .

# starting up a container
docker run -p 8080:8080 openapi_server
```

and open your browser to here:

!!! warning
    The version (v1.3.0) depends of your configuration. If you have questions, go to the README in the server.

```
http://localhost:8080/v1.3.0/ui/
```


Your OpenAPI definition lives here:

```
http://localhost:8080/v1.3.0/openapi.json
```

To launch the integration tests, use tox:
```
sudo pip install tox
tox
```

