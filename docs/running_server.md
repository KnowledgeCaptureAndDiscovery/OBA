

## Running with Docker

To run the server on a Docker container, execute the following command from the root directory:

```bash
# building the image
$ docker build -t openapi_server .

# starting up a container
$  docker run -v $PWD/openapi_server/openapi/:/usr/src/app/openapi_server/openapi/ <docker_image>
```

!!!info
    OBA uses a [cache system](cached.md)

and open the following URL in your browser:


```
http://localhost:8080/<API_VERSION>/ui/
```

!!! warning
    The version (v1.3.0) depends of your configuration. If you have questions, see the README file in the server.



Your OpenAPI definition lives here:

```
http://localhost:8080/<API_VERSION>/openapi.json
```

To launch the integration tests, install and execute [tox](https://pypi.org/project/tox/):
```
sudo pip install tox
tox
```

