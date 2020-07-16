

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

### Enabling CORS

!!! info
We recommend to enable CORS in the WebServer and not in the application. [https://enable-cors.org/server.html](https://enable-cors.org/server.html)


We can enable CORS in the Python server.

```python
import connexion
from flask_cors import CORS

app = connexion.FlaskApp(__name__)
app.add_api('swagger.yaml')

# add CORS support
CORS(app.app)

app.run(port=8080)
```

You can see a [example](https://github.com/sirspock/dbpedia_api/blob/master/server/openapi_server/__main__.py)

