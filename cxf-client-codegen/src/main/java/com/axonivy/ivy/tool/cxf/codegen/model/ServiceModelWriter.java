package com.axonivy.ivy.tool.cxf.codegen.model;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;

import org.apache.cxf.tools.common.model.JavaModel;
import org.apache.cxf.tools.common.model.JavaServiceClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class ServiceModelWriter {
  
  private static final Logger LOGGER = LoggerFactory.getLogger(ServiceModelWriter.class);
  private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

  public void write(JavaModel javaModel, Path tmpDir) throws IOException {
    if (javaModel.getServiceClasses().isEmpty()) {
      throw new IllegalStateException("Expected at least one service class in the generated model, but found none.");
    }
    if (javaModel.getServiceClasses().size() > 1) {
      LOGGER.warn("Expected exactly one service class in the generated model, " +
      "but found: {}. Only the first one will be stored.", javaModel.getServiceClasses().keySet());
    }
    var webService = asMap(javaModel.getServiceClasses().values().iterator().next());
    toJson(webService, tmpDir.resolve("service.json"));
  }

  private static Object asMap(JavaServiceClass service) {
    var ports = new LinkedHashMap<String, String>();
    for (var port : service.getPorts()) {
      ports.put(port.getName(), port.getBindingAdress());
    }

    var webService = new LinkedHashMap<String, Object>();
    webService.put("service", service.getFullClassName());
    webService.put("ports", ports);

    return webService;
  }

  private void toJson(Object service, Path outputFile) throws IOException {
    MAPPER.writeValue(outputFile.toFile(), service);
  }

}
