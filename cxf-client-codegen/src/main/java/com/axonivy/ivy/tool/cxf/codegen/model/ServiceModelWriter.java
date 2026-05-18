package com.axonivy.ivy.tool.cxf.codegen.model;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.cxf.tools.common.model.JavaModel;
import org.apache.cxf.tools.common.model.JavaServiceClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public class ServiceModelWriter {

  private static final Logger LOGGER = LoggerFactory.getLogger(ServiceModelWriter.class);

  public void write(JavaModel javaModel, Path tmpDir) throws IOException {
    if (javaModel.getServiceClasses().isEmpty()) {
      throw new IllegalStateException("Expected at least one service class in the generated model, but found none.");
    }
    if (javaModel.getServiceClasses().size() > 1) {
      LOGGER.warn("Expected exactly one service class in the generated model, " +
      "but found: {}. Only the first one will be stored.", javaModel.getServiceClasses().keySet());
    }
    var webService = asMap(javaModel.getServiceClasses().values().iterator().next());
    toYaml(webService, tmpDir.resolve("service.yaml"));
  }

  private static Object asMap(JavaServiceClass service) {
    var ports = new LinkedHashMap<String, String>();
    for (var port : service.getPorts()) {
      ports.put(port.getName(), port.getBindingAdress());
    }

    var webService = new LinkedHashMap<String, Object>();
    webService.put("name", service.getFullClassName());
    webService.put("ports", ports);

    return Map.of("webService", webService);
  }

  private void toYaml(Object service, Path outputFile) throws IOException {
    var options = new DumperOptions();
    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    var yaml = new Yaml(options);
    try (Writer writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
      yaml.dump(service, writer);
    }
  }

}
