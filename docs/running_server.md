

## Running with Docker

To run the server on a Docker container, execute the following command from the root directory:

```bash
# building the image
$ docker build -t <docker_image> .

# starting up a container
$  docker run -p 8080:8080 -v $PWD/openapi_server/openapi/:/usr/src/app/openapi_server/openapi/ <docker_image>
```

!!!info
    To improve the speed at which the queries are returned, you can configure OBA to use a [cache](cache.md) (recommended)

and open the following URL in your browser:


```
http://localhost:8080/<API_VERSION>/ui/
```

!!! warning
    The <API_VERSION> (e.g., v1.3.0) is defined in your configuration yaml file (the field `version`). For more information, see the README file generated in your server folder when running OBA.


Your OpenAPI definition is accessible here:

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


You can enable CORS in the Python server as follows:

```python
import connexion
from flask_cors import CORS

app = connexion.FlaskApp(__name__)
app.add_api('swagger.yaml')

# add CORS support
CORS(app.app)

app.run(port=8080)
```

You can see an example in the following [GitHub repository](https://github.com/sirspock/dbpedia_api/blob/master/server/openapi_server/__main__.py)

