#!/usr/bin/env bash
set -e

GREEN='\033[0;32m'
RED='\033[0;31m'
WHITE='\033[0;37m'
RESET='\033[0m'

if [ -x "$(command -v docker)" ]; then
    echo "Docker is installed"
else
    echo "Docker is not installed"
    exit 1
fi


dir=${PWD}
SERVER_DIR=server
docker run -ti --rm -v ${PWD}:/local openapitools/openapi-generator-cli:v4.1.2 \
     generate  \
     -i /local/openapi.yaml\
     -g python-flask  \
     -o /local/$SERVER_DIR/ \
     --template-dir /local/.openapi-generator/template \
     --ignore-file-override /local/.openapi-generator-ignore

cp -r ${PWD}/.openapi-generator/template/static_files/utils/ ${PWD}/$SERVER_DIR/openapi_server/utils/
cp -r ${PWD}/.openapi-generator/template/static_files/settings/ ${PWD}/$SERVER_DIR/openapi_server/settings/
cp -r ${PWD}/.openapi-generator/template/static_files/user_controller.py ${PWD}/$SERVER_DIR/openapi_server/controllers/
mkdir -p ${PWD}/$SERVER_DIR/contexts/
echo "Copying query files"
cp -r ../../queries ${PWD}/$SERVER_DIR/queries
if [ "$?" == "0" ]; then
        echo -e "${GREEN}SUCCESS${RESET}"
fi