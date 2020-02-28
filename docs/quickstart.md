## Installation 

### Downloading binary


1. Go the [latest release](https://github.com/KnowledgeCaptureAndDiscovery/OBA/releases/latest)
2. Download the file with extension .jar


### Building binary from the resource

1. Clone the repository `git clone https://github.com/KnowledgeCaptureAndDiscovery/OBA.git`
2. Install it using `mvn package`


## Running


1. Create the OBA config file from the [sample configuration](config.yaml.sample)
2. Pass the configuration and run OBA
```bash
$ java -jar oba-2.0.0-jar-with-dependencies.jar -c config.yaml
```
