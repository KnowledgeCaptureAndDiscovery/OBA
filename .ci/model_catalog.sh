set -xe

java -jar target/oba-*-jar-with-dependencies.jar -c examples/modelcatalog/config.yaml
pushd outputs/modelcatalog/servers/python
bash generate-server.sh
pushd server
docker build -t modelcatalog .
docker run --name modelcatalog -d -p 8080:8080 modelcatalog
popd
sleep 10s
#curl -X GET "http://0.0.0.0:8080/v1.3.0/bands/Pink_Floyd" -H  "accept: application/json"
docker logs modelcatalog
