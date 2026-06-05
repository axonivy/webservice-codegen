package com.axonivy.ivy.tool.cxf.codegen.model;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static com.axonivy.ivy.tool.cxf.codegen.TestCxfClientCodegen.generateResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TestServiceModelWriter {

  @Test
  void storeNameAndPorts(@TempDir Path tmpDir) throws Exception {
    generateResource("IvyEchoService.WSDL", tmpDir);
    Path service = tmpDir.resolve("service.json");
    var model = Files.readString(service);
    assertThat(model)
      .as("Store name and ports for config/webservice-clients.yaml")
      .isEqualTo("""
        {
          "service" : "ch.ivyteam.test.ws.IvyEchoService",
          "ports" : {
            "IvyEchoServiceHttpsSoap11Endpoint" : "https://test-webservices.ivyteam.io:8443/axis2/services/IvyEchoService.IvyEchoServiceHttpsSoap11Endpoint/",
            "IvyEchoServiceHttpEndpoint" : "http://test-webservices.ivyteam.io:8080/axis2/services/IvyEchoService.IvyEchoServiceHttpEndpoint/",
            "IvyEchoServiceHttpsSoap12Endpoint" : "https://test-webservices.ivyteam.io:8443/axis2/services/IvyEchoService.IvyEchoServiceHttpsSoap12Endpoint/",
            "IvyEchoServiceHttpSoap12Endpoint" : "http://test-webservices.ivyteam.io:8080/axis2/services/IvyEchoService.IvyEchoServiceHttpSoap12Endpoint/",
            "IvyEchoServiceHttpsEndpoint" : "https://test-webservices.ivyteam.io:8443/axis2/services/IvyEchoService.IvyEchoServiceHttpsEndpoint/",
            "IvyEchoServiceHttpSoap11Endpoint" : "http://test-webservices.ivyteam.io:8080/axis2/services/IvyEchoService.IvyEchoServiceHttpSoap11Endpoint/"
          }
        }\
        """);
  }

}
