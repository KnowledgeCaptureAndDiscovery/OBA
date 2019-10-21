java -jar owl2jsonld-0.2.2-SNAPSHOT-standalone.jar  https://mintproject.github.io/Mint-ModelCatalog-Ontology/release/1.2.0/ontology.xml > a.json
java -jar owl2jsonld-0.2.2-SNAPSHOT-standalone.jar https://knowledgecaptureanddiscovery.github.io/SoftwareDescriptionOntology/release/1.2.0/ontology.xml > b.json
rm -rf server/contexts/
mkdir -p server/contexts
jq -s '.[0] * .[1]' a.json b.json  | jq -S > server/contexts/context.json
rm a.json b.json
