package com.axonivy.ivy.tool.cxf.codegen.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.axonivy.ivy.tool.cxf.codegen.CxfClientGenerator;
import com.axonivy.ivy.tool.cxf.codegen.TestCxfClientCodegen;

class TestServiceModelWriter {

  @Test
  void storeNameAndPorts(@TempDir Path tmpDir) throws Exception {
    generate(tmpDir, true);
    Path service = tmpDir.resolve("service.json");
    var model = Files.readString(service);
    assertThat(model)
        .as("Store name and ports for config/webservice-clients.yaml")
        .isEqualTo("""
          {
            "service" : "com.acme.echo.IvyEchoService",
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

  @Test
  void optIn(@TempDir Path tmpDir, @TempDir Path tmpDir2) throws Exception {
    boolean serviceInfo = false;
    generate(tmpDir, serviceInfo);
    assertThat(tmpDir.resolve("service.json")).doesNotExist();

    serviceInfo = true;
    generate(tmpDir2, serviceInfo);
    assertThat(tmpDir2.resolve("service.json")).exists();
  }

  private void generate(Path tmpDir, boolean serviceInfo) throws Exception {
    var opts = new CxfClientGenerator.CodegenOpts(
        "com.acme.echo", Map.of(), false,
        serviceInfo);
    new CxfClientGenerator(tmpDir).generate(
        TestCxfClientCodegen.class.getResource("IvyEchoService.WSDL").toString(), opts);
  }

}
