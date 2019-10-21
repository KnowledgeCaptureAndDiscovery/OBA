#!/usr/bin/env bash
dir=${PWD}
SERVER_DIR=server
docker run -ti --rm -v ${PWD}:/local openapitools/openapi-generator-cli:v4.1.2 \
     generate  \
     -i /local/model-catalog.yaml\
     -g python-flask  \
     -o /local/$SERVER_DIR/ \
     --git-repo-id model-catalog-api \
     --git-user-id mintproject \
     --template-dir /local/.openapi-generator/template \
     --ignore-file-override /local/.openapi-generator-ignore
rm -f $SERVER_DIR/openapi_server/controllers/default_controller.py
cp -rv ${PWD}/.openapi-generator/template/static_files/utils/ ${PWD}/$SERVER_DIR/openapi_server/utils/
cp -rv ${PWD}/.openapi-generator/template/static_files/settings/ ${PWD}/$SERVER_DIR/openapi_server/settings/
cp -rv ${PWD}/.openapi-generator/template/static_files/user_controller.py ${PWD}/$SERVER_DIR/openapi_server/controllers/
cp -rv ${PWD}/.openapi-generator/template/static_files/contexts/ ${PWD}/$SERVER_DIR/contexts/
cp -rv ${PWD}/.openapi-generator/template/static_files/queries/ ${PWD}/$SERVER_DIR/queries/
