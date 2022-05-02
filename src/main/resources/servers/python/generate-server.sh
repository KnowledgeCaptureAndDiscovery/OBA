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
cp ../openapi.yaml ${PWD}

SERVER_DIR=server


docker run --rm -v ${PWD}:/local \
     -u "$(id -u):$(id -u)" \
     openapitools/openapi-generator-cli:v5.4.0 \
     generate  \
     -i /local/openapi.yaml\
     -g python-flask  \
     -o /local/$SERVER_DIR/ \
     --template-dir /local/.openapi-generator/template \
     --ignore-file-override /local/.openapi-generator-ignore

cp -r ${PWD}/.openapi-generator/template/static_files/utils/ ${PWD}/$SERVER_DIR/openapi_server/utils/
cp -r ${PWD}/.openapi-generator/template/static_files/settings/ ${PWD}/$SERVER_DIR/openapi_server/settings/
cp -r ${PWD}/.openapi-generator/template/static_files/user_controller.py ${PWD}/$SERVER_DIR/openapi_server/controllers/
cp -r ${PWD}/.openapi-generator/template/static_files/cached.py ${PWD}/$SERVER_DIR/openapi_server/
mkdir -p ${PWD}/$SERVER_DIR/contexts/
echo "Copying query files"
cp -r ../../queries ${PWD}/$SERVER_DIR/queries
cp -r ../context.json ${PWD}/$SERVER_DIR/contexts/
cp -r ../context_class.json ${PWD}/$SERVER_DIR/contexts/
if [ "$?" == "0" ]; then
        echo -e "${GREEN}SUCCESS${RESET}"
fi
