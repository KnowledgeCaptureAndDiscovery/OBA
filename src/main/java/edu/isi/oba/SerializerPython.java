package edu.isi.oba;


import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

import org.openapitools.codegen.serializer.SerializerUtils;
import org.semanticweb.owlapi.model.IRI;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

class SerializerPython {
  public SerializerPython(Mapper mapper, java.nio.file.Path dir, OpenAPI openAPI) throws IOException {
    //Create directory utils
    final String utils_dir = ".openapi-generator/template/static_files/utils/";
    File utils_dir_path = new File(dir + File.separator + utils_dir);
    utils_dir_path.mkdir();

    //Create variable file
    final String var_file_python = ".openapi-generator/template/static_files/utils/vars.py";
    FileWriter var_file_writer = new FileWriter(dir + File.separator + var_file_python);
      Iterator it = mapper.schemaNames.entrySet().iterator();
      add_variable_python(var_file_writer, it);
    var_file_writer.close();


  }


  private void add_variable_python(FileWriter myWriter, Iterator it) {
    IRI clsIRI;
    String name;
    while (it.hasNext()) {
      Map.Entry pair = (Map.Entry) it.next();
      clsIRI = (IRI) pair.getKey();
      name = (String) pair.getValue();
      try {
        String variable_name = name.toUpperCase().replace("-", "");
        myWriter.write(variable_name + "_TYPE_URI = \"" + clsIRI + "\"\n");
        myWriter.write(variable_name + "_TYPE_NAME = \"" + name + "\"\n");
      } catch (IOException e) {
        System.out.println("An error occurred.");
        e.printStackTrace();
      }
    }
  }
}