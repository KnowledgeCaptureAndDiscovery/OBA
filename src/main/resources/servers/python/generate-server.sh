#!/usr/bin/env bash
set -e
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
     -i /local/model-catalog.yaml\
     -g python-flask  \
     -o /local/$SERVER_DIR/ \
     --template-dir /local/.openapi-generator/template \
     --ignore-file-override /local/.openapi-generator-ignore
cp -rv ${PWD}/.openapi-generator/template/static_files/utils/ ${PWD}/$SERVER_DIR/openapi_server/utils/
cp -rv ${PWD}/.openapi-generator/template/static_files/settings/ ${PWD}/$SERVER_DIR/openapi_server/settings/
cp -rv ${PWD}/.openapi-generator/template/static_files/user_controller.py ${PWD}/$SERVER_DIR/openapi_server/controllers/
mkdir -p ${PWD}/.openapi-generator/template/static_files/contexts/
mkdir -p ${PWD}/.openapi-generator/template/static_files/queries/
