set -xe

java -jar target/oba-*-jar-with-dependencies.jar -c examples/dbpedia/config_music.yaml
pushd outputs/dbpedia_music/servers/python
bash generate-server.sh
pushd server
docker build -t openapi_server .
docker run --name dbpedia_music -d -p 8080:8080 openapi_server
popd
sleep 10s
curl -X GET "http://0.0.0.0:8080/v1.3.0/bands/Pink_Floyd" -H  "accept: application/json"
docker logs dbpedia_music
