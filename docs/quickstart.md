
## Requirements

Java 1.8 or higher (SDK 1.8 or JRE 8).

## Installation 

There are two options to run OBA:
 
1. Download the binary
2. Build the binary from the repository


### 1 Downloading binary

1. Go the [latest release page](https://github.com/KnowledgeCaptureAndDiscovery/OBA/releases/latest)
2. Download the file with extension .jar

### 2 Building binary

1. Clone the repository `git clone https://github.com/KnowledgeCaptureAndDiscovery/OBA.git`
2. Install it executing `mvn package`
3. The binary will be available in the `target` directory

## Running OBA

!!! info
    Documentation on the configuration file can be accessed [in the configuration page](configuration_file.md)


1. Create the OBA config file (config.yaml) from the [sample configuration we provide](config.yaml.sample)
2. Use the configuration to run the OBA JAR:

```bash
$ java -jar oba-*-jar-with-dependencies.jar -c config.yaml
```

