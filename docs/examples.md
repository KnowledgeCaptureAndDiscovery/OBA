## API generation examples

We have tested out OBA with different example ontologies to ensure its fucntionality. Check the [examples](https://github.com/KnowledgeCaptureAndDiscovery/OBA/tree/master/examples) directory on GitHub for sample configurations to test the system. We describe some of them briefly below:

1. The [DBPedia music configuration](https://github.com/KnowledgeCaptureAndDiscovery/OBA/blob/master/examples/dbpedia/config_music.yaml) shows how to use OBA to filter an ontology of significant size to include just a few classes and properties of interest (in this case, bands and genres in DBPedia).
2. The [P-Plan ontology configuration](https://github.com/KnowledgeCaptureAndDiscovery/OBA/tree/master/examples/pplan) shows how to load an ontology using its URI. OBA will do the content negotiation.
3. The [WINGS configuration](https://github.com/KnowledgeCaptureAndDiscovery/OBA/tree/master/examples/wings) shows how to load an ontology network in OBA.
4. The [Full Model Catalog configuration](https://github.com/KnowledgeCaptureAndDiscovery/OBA/tree/master/examples/modelcatalog_full) illustrates how to add custom queries in OBA, as well as to include full support for POST, PUT and DELETE methods in the specification.

## Server generation examples

If you are looking to test OBA with an end-to-end example, we recommend generating a server for the [Model Catalog configuration](https://github.com/KnowledgeCaptureAndDiscovery/OBA/tree/master/examples/modelcatalog), which shows how to create an API for a series of classes, properties and data properties with basic restrictions. The deployment and set up  of the API don't take more than a few minutes. 

The [DBPedia music configuration](https://github.com/KnowledgeCaptureAndDiscovery/OBA/blob/master/examples/dbpedia/config_music.yaml) also provides a nice end-to-end example, although the response times  (without proper optimizations) may be slower than the model catalog example.