set -xe

container_name="business"
java -jar target/oba-*-jar-with-dependencies.jar -c examples/business/config.yaml
pushd outputs/BusinessOntology/servers/python
bash generate-server.sh
pushd server
docker build -t openapi_server .
docker run --name ${container_name} -d -p 8081:8080 openapi_server
popd
sleep 10s
curl -X GET "http://0.0.0.0:8081/v1.3.0/bands/Pink_Floyd" -H  "accept: application/json"
docker logs ${container_name}
docker rm -f ${container_name}
