package com.axonivy.ivy.tool.cxf.codegen.mojo;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.axonivy.ivy.tool.cxf.codegen.TestCxfClientCodegen;

@MojoTest
class TestCxfClientGeneratorMojo {

  private CxfClientGeneratorMojo mojo;

  @BeforeEach
  @InjectMojo(goal = CxfClientGeneratorMojo.GOAL)
  void setUp(CxfClientGeneratorMojo cxfClient) {
    this.mojo = cxfClient;
  }

  @Test
  void generate(@TempDir Path out) throws Exception {
    var echoOut = out.resolve("my-workspace").resolve("my-project").resolve("src_generated").resolve("soap").resolve("echoService");
    mojo.wsdl = TestCxfClientCodegen.class.getResource("IvyEchoService.WSDL").toURI().toString();
    mojo.nsMappings = List.of("urn:ws.test.ivyteam.ch@com.acme.ivy.echo");
    mojo.outputDir = echoOut;
    mojo.execute();

    var echo = echoOut.resolve("com/acme/ivy/echo");
    try (var sources = Files.list(echo)) {
      assertThat(sources)
          .extracting(p -> p.getFileName().toString())
          .contains("IvyEchoService.java", "IvyEchoServicePortType.java");
    }

    assertThat(echo.resolve("IvyEchoService.wsdl"))
        .as("Used WSDL is copied to service; and read at runtime by JAX-WS impl")
        .exists();
  }

  @Test
  void regenerate_cleanup(@TempDir Path out) throws Exception {
    mojo.wsdl = TestCxfClientCodegen.class.getResource("IvyEchoService.WSDL").toURI().toString();
    mojo.nsMappings = List.of("urn:ws.test.ivyteam.ch@com.acme.ivy.echo");
    mojo.outputDir = out;

    var legacy = out.resolve("com/acme/ivy/echo").resolve("MyClient.java");
    Files.createDirectories(legacy.getParent());
    Files.writeString(legacy, "package legacy;", StandardOpenOption.CREATE_NEW);

    mojo.execute();

    assertThat(legacy)
        .as("existing client is removed before re-generation")
        .doesNotExist();
  }

}
