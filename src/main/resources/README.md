# Servers directory

Note servers.zip file is used to generate the python flask server, so any changes to files in the folder need to be replicated into a new servers.zip file. eg.

```
rm servers.zip
zip -r servers.zip servers
```

Also note the moustache template files in the hidden folder: OBA/src/main/resources/servers/python/.openapi-generator were copied from https://github.com/OpenAPITools/openapi-generator/tree/master/modules/openapi-generator/src/main/resources/python-flask on 17 Jul 2020, and have not followed changes to that project so that a full refactor would be required to get back in sync.