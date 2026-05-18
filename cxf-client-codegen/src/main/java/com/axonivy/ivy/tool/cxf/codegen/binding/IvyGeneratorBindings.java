package com.axonivy.ivy.tool.cxf.codegen.binding;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.text.StringSubstitutor;
import org.apache.cxf.Bus;

import com.axonivy.ivy.tool.cxf.codegen.CxfClientGenerator.CodegenOpts;


public class IvyGeneratorBindings {

  private final Path tmpGenDir;

  public IvyGeneratorBindings(Path tmpGenDir) {
    this.tmpGenDir = tmpGenDir;
  }

  public String[] getBindings(CodegenOpts options) {
    var props = toProperties(options);
    var ivyBind = Stream.of("globalJaxbBindings.xml")
        .map(res -> interpolate(res, props));
    var statics = List.of(
        IvyGeneratorBindings.class.getResource("noWrappersJaxWsBinding.xml"),
        Bus.class.getResource("/schemas/wsdl/XMLSchema.xsd"));
    Stream<String> staticResources = statics.stream().map(URL::toExternalForm);
    return Stream.concat(ivyBind, staticResources).toArray(String[]::new);
  }

  private String interpolate(String resourceName, Map<String, String> props) {
    try (InputStream is = IvyGeneratorBindings.class.getResourceAsStream(resourceName)) {
      var template = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      var resolved = StringSubstitutor.replace(template, props, "${", "}");
      Path tmpResource = tmpGenDir.resolve(resourceName);
      Files.writeString(tmpResource, resolved, StandardOpenOption.CREATE_NEW);
      return tmpResource.toUri().toString();
    } catch (IOException ex) {
      throw new UncheckedIOException("Failed to interpolate codegen-options into " + resourceName, ex);
    }
  }

  private static Map<String, String> toProperties(CodegenOpts options) {
    // https://docs.oracle.com/javase/tutorial/jaxb/intro/custom.html
    return Map.of("underscoreBinding", options.underscoreNames() ? "asCharInWord" : "asWordSeparator");
  }

}
