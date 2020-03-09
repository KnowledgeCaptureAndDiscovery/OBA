
## Requirements

You will need Java 1.8 or higher (SDK 1.8 or JRE 8).

## Installation 

There are two option to run OBA:
 
1. Download the binary.
2. Build the binary from the repository.


### Downloading binary

1. Go the [latest release](https://github.com/KnowledgeCaptureAndDiscovery/OBA/releases/latest)
2. Download the file with extension .jar

### Building binary

1. Clone the repository `git clone https://github.com/KnowledgeCaptureAndDiscovery/OBA.git`
2. Install it using `mvn package`
3. The binary is available in the `target` directory

## Running

!!! info
    For the configuration file documentation, go to [here](configuration_file.md)


1. Create the OBA config file (config.yaml) from the [sample configuration](config.yaml.sample)
2. Pass the configuration and run OBA

```bash
$ java -jar oba-*-jar-with-dependencies.jar -c config.yaml
```

