OBA supports the mapping defined in [https://owl-to-oas.readthedocs.io/en/latest/mapping/](https://owl-to-oas.readthedocs.io/en/latest/mapping/), with a few small modifications:

 - The mapping suggests using the `allOf` property from OAS to capture subclass relationships. However, this was not supported by any existing generators until very recently (it is still on test), and therefore OBA will iterate through all superclasses to add the appropriate properties for a given schema.
 - Path naming conventions: The mapping suggest using the labels of the terms of the ontology for creating the paths in the target API. However, at the moment OBA will use the local namespace of the classes.

Additional materials and examples of the mapping are available in [this GitHub repository](https://github.com/oeg-upm/OWL-To-OAS-Specification)
