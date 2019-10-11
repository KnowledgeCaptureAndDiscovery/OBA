#!/usr/bin/env bash
dir=${PWD}
server_dir=server-v1.0.0

docker run -ti --rm -v ${PWD}:/local openapitools/openapi-generator-cli:v4.1.2 \
     generate  \
     -i /local/openapi.yaml\
     -g python-flask  \
     -o /local/${server_dir}/ \
     --git-repo-id preo-api \
     --git-user-id mintproject \
     --template-dir /local/.openapi-generator/template \
     --ignore-file-override /local/.openapi-generator-ignore
rm -f ${server_dir}/openapi_server/controllers/default_controller.py
cp -rv ${PWD}/.openapi-generator/template/static_files/utils/ ${PWD}/${server_dir}/openapi_server/utils/
cp -rv ${PWD}/.openapi-generator/template/static_files/settings/ ${PWD}/${server_dir}/openapi_server/settings/
cp -rv ${PWD}/.openapi-generator/template/static_files/user_controller.py ${PWD}/${server_dir}/openapi_server/controllers/
cp -rv ${PWD}/.openapi-generator/template/static_files/contexts/ ${PWD}/${server_dir}/contexts/
cp -rv ${PWD}/.openapi-generator/template/static_files/queries/ ${PWD}/${server_dir}/queries/
