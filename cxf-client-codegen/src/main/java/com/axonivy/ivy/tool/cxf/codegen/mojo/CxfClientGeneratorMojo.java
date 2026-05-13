package com.axonivy.ivy.tool.cxf.codegen.mojo;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.cxf.tools.common.ToolContext;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.axonivy.ivy.tool.cxf.codegen.CxfClientGenerator;
import com.axonivy.ivy.tool.cxf.codegen.CxfClientGenerator.CodegenOpts;

/**
 * <p>
 * Generates a CXF client
 * </p>
 * <p>
 * Command line invocation is supported.
 * </p>
 * <code>
 * mvn com.axonivy.ivy.tool.soap:cxf-client-codegen:generate-cxf-client
 * -Divy.generate.webservice.client.spec=https://petstore3.swagger.io/api/v3/openapi.json
 * -Divy.generate.webservice.client.output=src_generated/soap/petstore
 * -Divy.generate.webservice.client.package=com.swagger.petstore.client
 * </code>
 *
 * @since 1.0.0
 */
@Mojo(name = CxfClientGeneratorMojo.GOAL, requiresProject = false)
public class CxfClientGeneratorMojo extends AbstractMojo {
  public static final String GOAL = "generate-cxf-client";

  @Parameter(property = "ivy.generate.webservice.client.skip", defaultValue = "false")
  boolean skipGenerate;

  /** URI or Path to a WSDL file */
  @Parameter(property = "ivy.generate.webservice.client.wsdl", required = true)
  String wsdl;

  @Parameter(property = "ivy.generate.webservice.client.output", required = true)
  Path outputDir;

  /**
   * Defines the reflection of WSDL namespaces to Java packages. The format is <code>namespace=package</code>.
   * <pre>
   * &lt;nsMappings&gt;&lt;nsMapping&gt;http://service.soap.connectivity.axonivy.com/=com.axonivy.person.client&lt;/nsMapping&gt;
   * &lt;/nsMappings&gt;
   * </pre>
   */
  @Parameter(property = "ivy.generate.webservice.client.nsMappings")
  List<String> nsMappings;

  /**
   * If the WSDL service definitions contains similar attributes only differing by
   * their ‘underscore’ name, one of the attributes is gone in the generated CXF
   * client. 
   * Enable this property to enforce the manifestation of the underscore in the generated client.
   * <pre>
   * &lt;xsd:element name="PRICEDATE" type="tns:char1"/&gt;
   * &lt;xsd:element name="PRICE_DATE" type="tns:date10"/&gt;
   * </pre>
   */
  @Parameter(property = "ivy.generate.webservice.client.underscoreNames", defaultValue = "false")
  Boolean underscoreNames;

  // TODO: enable insecure SSL
  @Parameter(property = "ivy.generate.webservice.client.insecureSSL", defaultValue = "false")
  Boolean insecureSSL;

  @Override
  public void execute() throws MojoExecutionException {
    if (skipGenerate) {
      return;
    }

    new CxfClientGeneratorFiles(outputDir).cleanup(getLog()::info);
    getLog().info("Generating CXF client sources for WSDL: " + wsdl);

    ToolContext cxfContext;
    try {
      cxfContext = CxfClientGenerator.generate(wsdl, outputDir,
          new CodegenOpts(mappings(), underscoreNames));
      getLog().info("Generated CXF client context: " + cxfContext);
    } catch (Exception ex) {
      getLog().error("Failed to generate CXF client sources", ex);
    }
    // TODO: serialize parsed; Service, Ports, URIs.
  }
  
  private Map<String, String> mappings() {
    if (nsMappings == null) {
      return Map.of();
    }
    return nsMappings.stream()
        .map(s -> s.split("="))
        .collect(Collectors.toMap(a -> a[0], a -> a[1]));
  }

}
