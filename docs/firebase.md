## Authentication using Firebase

1. Go to [https://console.firebase.google.com/u/0/](https://console.firebase.google.com/u/0/)
2. Create a new project
3. Enable the authentication and copy the Web API key
    <video width="640" height="480" controls>
    <source src="../figures/firebase.mp4" type="video/mp4">
    Your browser does not support the video tag.
    </video>
4. Edit the OBA configuration file as follows:
```yaml
auth:
  provider: firebase
firebase:
  key: YOUR_KEY
```
Where `YOUR_KEY` corresponds to the Web API key you obtained above. 

Finally, re-run OBA.

### Testing the authentication

Now you can see that OBA added a new path `/user/login`:


```
  /user/login:
    get:
      description: Login the user
      operationId: user_login_get
      parameters:
      - description: The user name for login
        in: query
        name: username
        required: true
        schema:
          type: string
      - description: The password for login in clear text
        in: query
        name: password
        required: true
        schema:
          type: string
      responses:
        200:
          content:
            application/json:
              schema:
                type: string
          description: successful operation
          headers:
            X-Rate-Limit:
              description: calls per hour allowed by the user
              schema:
                format: int32
                type: integer
            X-Expires-After:
              description: date in UTC when token expires
              schema:
                format: date-time
                type: string
        400:
          content:
            application/json:
              schema:
                type: string
          description: unsuccessful operation
      x-openapi-router-controller: openapi_server.controllers.user_controller
```


You can test the authentication with the following command:

```
$ curl -s -X GET 'https://$SERVER/$VERSION/user/login?username=$USERNAME&password=$PASSWORD' -H 'accept: application/json'
{
  "access_token": "$ACCESS_TOKEN",
  "expires_in": 60000,
  "refresh_token": "$REFREST_TOKEN",
  "scope": "create",
  "token_type": "bearer"
}
```

To use the token, add the token in the header

```
$ curl -v -X POST "$SERVER/$CLASS" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d "$payload"
```