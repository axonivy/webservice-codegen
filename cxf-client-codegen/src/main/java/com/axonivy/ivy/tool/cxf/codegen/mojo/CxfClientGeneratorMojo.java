package com.axonivy.ivy.tool.cxf.codegen.mojo;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
 * -Divy.generate.webservice.client.wsdl=https://petstore3.swagger.io/api/v3/openapi.json
 * -Divy.generate.webservice.client.output=src_generated/soap/petstore
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
   * Defines the java package of the generated client. E.g. <code>com.acme.soap.service.client</code>
   */
  @Parameter(property = "ivy.generate.webservice.client.package")
  String packageName;

  /**
   * Defines the reflection of WSDL namespaces to Java packages. The format is <code>namespace@package</code>.
   * <pre>
   * &lt;nsMappings&gt;&lt;nsMapping&gt;http://service.soap.connectivity.axonivy.com/@com.axonivy.person.client&lt;/nsMapping&gt;
   * &lt;/nsMappings&gt;
   * </pre>
   * For command line usage, separate mappings with commas:
   * <code>-Divy.generate.webservice.client.nsMappings=http://soap.axonivy.com/person.wsdl/@com.axonivy.person.client,http://another.namespace/@com.another.package</code>
   *
   * @deprecated Use {@link #packageName} instead. This deprecated approach only remains to ease migration for existing clients migrating from Designer LTS12.
   */
  @Parameter(property = "ivy.generate.webservice.client.nsMappings")
  @Deprecated(since = "1.0.0")
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

  /**
   * Allow insecure SSL connections when fetching WSDL and XSD resources from HTTPS endpoints.
   * This is required for endpoints with self-signed certificates.
   */
  @Parameter(property = "ivy.generate.webservice.client.insecureSSL", defaultValue = "false")
  Boolean insecureSSL;

  @Override
  public void execute() throws MojoExecutionException {
    if (skipGenerate) {
      return;
    }

    try {
      var files = new CxfClientGeneratorFiles(outputDir);
      files.cleanup(getLog()::info);
      files.createDir();

      getLog().info("Generating CXF client sources for WSDL: " + wsdl);
      var cxfContext = new CxfClientGenerator(outputDir)
          .insecureSsl(Boolean.TRUE.equals(insecureSSL))
          .generate(wsdl, new CodegenOpts(packageName, mappings(), underscoreNames));
      getLog().info("Generated CXF client for service: " + cxfContext.getJavaModel().getServiceClasses().keySet());
    } catch (Exception ex) {
      throw new MojoExecutionException("Failed to generate CXF client sources", ex);
    }
  }

  private Map<String, String> mappings() {
    if (nsMappings == null) {
      return Map.of();
    }
    return nsMappings.stream()
        .map(s -> s.split("@"))
        .collect(Collectors.toMap(a -> a[0], a -> a[1]));
  }

}
