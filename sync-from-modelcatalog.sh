dest=src/main/resources/servers/python/.openapi-generator/template/
rsync -av /Users/mosorio/repos/modelcatalog/server/.openapi-generator/template/ $dest
 rm -rf $dest/static_files/contexts
 rm -rf $dest/static_files/queries
 rm -rf $dest/static_files/utils/vars.py
