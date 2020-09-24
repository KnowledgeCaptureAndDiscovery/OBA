set -xe

java -jar target/oba-*-jar-with-dependencies.jar -c examples/dbpedia/config_music.yaml
pushd outputs/dbpedia_music
bash generate-server.sh
pushd server
docker build -t openapi_server .
docker run -d -p 8080:8080 openapi_server
popd
